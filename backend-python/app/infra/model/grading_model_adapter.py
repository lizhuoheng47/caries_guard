from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np

from app.core.image_utils import ensure_channel_dim, load_image, normalize_image, resize_image
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.infra.model.checkpoint_validator import CheckpointValidator
from app.infra.model.manifest_loader import ModuleManifest
from app.infra.model.model_assets import ModelAssets

log = get_logger("cariesguard-ai.model.grading")


class GradingModelAdapter(BaseModelAdapter):
    model_code = "caries-grading-v1"
    model_type_code = "GRADING"
    impl_type = ImplType.ML_MODEL

    def __init__(
        self,
        confidence_threshold: float = 0.5,
        model_assets: ModelAssets | None = None,
        settings: Any | None = None,
    ) -> None:
        self._confidence_threshold = confidence_threshold
        self._model_assets = model_assets
        self._settings = settings
        self._validator = CheckpointValidator("grading")
        self._loaded = False
        self._session: Any | None = None
        self._model: Any | None = None
        self._runtime_kind: str | None = None
        self._input_name: str | None = None
        self._input_shape: list[Any] = []
        self._output_shape: list[Any] = []
        self._input_layout: str = "NCHW"
        self._device: str = "cpu"

    def load(self) -> None:
        assets = self._require_assets()
        manifest = assets.grading_manifest
        self.bind_manifest(manifest)

        self._validator.validate_manifest_assets(
            manifest,
            class_map=assets.class_map(),
            preprocess=assets.preprocess_config(),
            postprocess=assets.postprocess_config(),
        )
        self._validator.validate_checkpoint_ready(manifest)

        arch = str(manifest.arch or "").strip().lower()
        checkpoint_format = str(manifest.checkpoint_format or manifest.checkpoint_path.suffix.lstrip(".")).strip().lower() if manifest.checkpoint_path is not None else ""
        if arch in {"onnx", "onnx_classifier", "classification_onnx"} or checkpoint_format == "onnx":
            self._load_onnx(manifest)
        elif arch in {"torchscript", "torchscript_classifier", "classification_torchscript"} or checkpoint_format in {"pt", "pth", "torchscript"}:
            self._load_torch(manifest)
        else:
            self._validator._raise("load", f"unsupported grading arch={manifest.arch!r} format={checkpoint_format!r}")

        self._validator.validate_model_shapes(
            manifest,
            class_map=assets.class_map(),
            input_shape=self._input_shape,
            output_shape=self._output_shape,
        )
        self._loaded = True
        log.info(
            "loaded grading model checkpoint=%s runtime=%s arch=%s",
            manifest.checkpoint_path,
            self._runtime_kind,
            manifest.arch,
        )

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._session = None
        self._model = None
        self._loaded = False

    def infer(
        self,
        image_path: Path,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detections: list[Any] | None = None,
    ) -> dict[str, Any]:
        if not self._loaded:
            raise RuntimeError("grading model adapter is not loaded")

        assets = self._require_assets()
        loaded = load_image(image_path)
        regions = list(segmentation_regions or [])
        if not regions:
            raise RuntimeError("grading requires non-empty segmentation regions")
        crops = self._build_crops(loaded.pixels, regions)
        if not crops:
            raise RuntimeError("grading failed to build lesion crops")

        candidates: list[dict[str, Any]] = []
        for index, crop in enumerate(crops):
            logits = self._run_model(self._preprocess_crop(crop, assets))
            probabilities = self._to_probabilities(logits)
            if probabilities.size == 0:
                raise RuntimeError("grading model returned empty logits")
            candidates.append(self._candidate_from_probabilities(index, regions[index], probabilities, assets))

        selected = self._select_candidate(candidates, assets)
        probabilities = np.asarray(selected["probabilities"], dtype=np.float32)
        confidence = float(np.max(probabilities))
        uncertainty = float(1.0 - confidence)
        class_margin = self._class_margin(probabilities)
        manifest = assets.grading_manifest
        return {
            "gradingLabel": selected["severityLabel"],
            "confidenceScore": round(max(0.0, min(0.9999, confidence)), 4),
            "uncertaintyScore": round(max(0.0, min(0.9999, uncertainty)), 4),
            "implType": ImplType.ML_MODEL.value,
            "rawResult": {
                "modelCode": self.model_code,
                "datasetVersion": manifest.dataset_version,
                "runtime": self._runtime_kind,
                "device": self._device,
                "arch": manifest.arch,
                "inputShape": self._input_shape,
                "outputShape": self._output_shape,
                "classMargin": class_margin,
                "decisionMode": assets.postprocess_config().get("grading", {}).get("decisionMode"),
                "candidates": [
                    {
                        "regionIndex": item["regionIndex"],
                        "toothCode": item["toothCode"],
                        "severityLabel": item["severityLabel"],
                        "severityScore": item["severityScore"],
                        "boundaryDistance": item["boundaryDistance"],
                        "probabilities": item["probabilities"],
                    }
                    for item in candidates
                ],
            },
        }

    def _load_onnx(self, manifest: ModuleManifest) -> None:
        try:
            import onnxruntime as ort
        except ImportError as exc:  # pragma: no cover - depends on runtime image
            raise RuntimeError("onnxruntime is required for grading ONNX inference") from exc
        providers = ["CPUExecutionProvider"]
        self._session = ort.InferenceSession(str(manifest.checkpoint_path), providers=providers)
        inputs = self._session.get_inputs()
        outputs = self._session.get_outputs()
        if not inputs:
            raise RuntimeError("grading ONNX model has no inputs")
        if not outputs:
            raise RuntimeError("grading ONNX model has no outputs")
        self._input_name = inputs[0].name
        self._input_shape = list(getattr(inputs[0], "shape", []) or [])
        self._output_shape = list(getattr(outputs[0], "shape", []) or [])
        self._input_layout = self._resolve_layout(self._input_shape)
        self._runtime_kind = "onnx"
        self._device = "cpu"

    def _load_torch(self, manifest: ModuleManifest) -> None:
        try:
            import torch
        except ImportError as exc:  # pragma: no cover - depends on runtime image
            raise RuntimeError("torch is required for grading TorchScript inference") from exc
        requested = str(getattr(self._settings, "model_device", "cpu") or "cpu").strip().lower()
        if requested.startswith("cuda") and not torch.cuda.is_available():
            raise RuntimeError(f"requested CUDA device is unavailable: {requested}")
        device = torch.device(requested if requested else "cpu")
        try:
            model = torch.jit.load(str(manifest.checkpoint_path), map_location=device)
        except Exception as exc:  # pragma: no cover - depends on external checkpoint
            raise RuntimeError("only TorchScript grading checkpoints are supported in this runtime") from exc
        model.eval()

        width, height = manifest.expected_input_size or (512, 512)
        channels = self._channels()
        dummy = torch.zeros((1, channels, height, width), dtype=torch.float32, device=device)
        with torch.inference_mode():
            output = model(dummy)
        if isinstance(output, (tuple, list)):
            output = output[0]

        self._model = model
        self._session = None
        self._runtime_kind = "torchscript"
        self._device = str(device)
        self._input_name = "input"
        self._input_shape = [1, channels, height, width]
        self._output_shape = list(getattr(output, "shape", []) or [])
        self._input_layout = "NCHW"

    def _build_crops(self, pixels: np.ndarray, regions: list[dict[str, Any]]) -> list[np.ndarray]:
        preprocess = self._require_assets().preprocess_config()
        grading = preprocess.get("grading", {}) if isinstance(preprocess.get("grading"), dict) else {}
        padding = int(grading.get("cropPaddingPixels", 0) or 0)
        height, width = pixels.shape[:2]
        crops: list[np.ndarray] = []
        for region in regions:
            bbox = region.get("bbox")
            if not isinstance(bbox, list) or len(bbox) != 4:
                continue
            x1, y1, x2, y2 = [int(value) for value in bbox]
            x1 = max(0, x1 - padding)
            y1 = max(0, y1 - padding)
            x2 = min(width, x2 + padding)
            y2 = min(height, y2 + padding)
            if x2 <= x1 or y2 <= y1:
                continue
            crops.append(np.asarray(pixels[y1:y2, x1:x2]))
        return crops

    def _preprocess_crop(self, crop: np.ndarray, assets: ModelAssets) -> np.ndarray:
        preprocess = assets.preprocess_config()
        shared = preprocess.get("shared", {})
        normalize = shared.get("normalize", {}) if isinstance(shared.get("normalize"), dict) else {}
        clip = shared.get("valueClip", {}) if isinstance(shared.get("valueClip"), dict) else {}
        target_size = assets.grading_manifest.expected_input_size or (512, 512)
        resized = resize_image(crop, target_size, interpolation=str(shared.get("interpolation") or "bilinear"))
        arranged = ensure_channel_dim(resized, self._channels())
        normalized = normalize_image(
            arranged,
            min_value=float(clip.get("min", 0.0) or 0.0),
            max_value=float(clip.get("max", 255.0) or 255.0),
            normalize_mode=str(normalize.get("mode") or "minmax_0_1"),
            mean=normalize.get("mean"),
            std=normalize.get("std"),
            channels=self._channels(),
        )
        if self._input_layout == "NHWC":
            return normalized[None, :, :, :].astype(np.float32)
        return np.transpose(normalized, (2, 0, 1))[None, :, :, :].astype(np.float32)

    def _run_model(self, tensor: np.ndarray) -> np.ndarray:
        if self._runtime_kind == "onnx":
            outputs = self._session.run(None, {self._input_name: tensor})
            if not outputs:
                raise RuntimeError("grading ONNX model returned no outputs")
            return np.asarray(outputs[0])

        try:
            import torch
        except ImportError as exc:  # pragma: no cover - depends on runtime image
            raise RuntimeError("torch is required for grading TorchScript inference") from exc
        with torch.inference_mode():
            output = self._model(torch.from_numpy(tensor).to(self._device))
        if isinstance(output, (tuple, list)):
            output = output[0]
        if hasattr(output, "detach"):
            output = output.detach().cpu().numpy()
        return np.asarray(output)

    @staticmethod
    def _to_probabilities(logits: np.ndarray) -> np.ndarray:
        arr = np.asarray(logits, dtype=np.float32)
        arr = np.squeeze(arr)
        if arr.ndim == 0:
            arr = arr[None]
        if arr.ndim != 1:
            raise RuntimeError(f"unsupported grading output shape: {list(np.asarray(logits).shape)}")
        shifted = arr - np.max(arr)
        exp = np.exp(shifted)
        denom = np.maximum(np.sum(exp), 1e-12)
        return exp / denom

    def _candidate_from_probabilities(
        self,
        index: int,
        region: dict[str, Any],
        probabilities: np.ndarray,
        assets: ModelAssets,
    ) -> dict[str, Any]:
        label_order = assets.grading_labels()
        if probabilities.size != len(label_order):
            raise RuntimeError(
                f"grading checkpoint logits size mismatch: checkpoint={probabilities.size} labelMap={len(label_order)}"
            )
        class_id = int(np.argmax(probabilities))
        label = label_order[class_id]
        top_prob = float(probabilities[class_id])
        return {
            "regionIndex": index,
            "toothCode": str(region.get("toothCode") or region.get("tooth_code") or "UNKNOWN"),
            "severityLabel": label,
            "severityScore": round(top_prob, 4),
            "boundaryDistance": self._class_margin(probabilities),
            "probabilities": [round(float(item), 6) for item in probabilities.tolist()],
        }

    def _select_candidate(self, candidates: list[dict[str, Any]], assets: ModelAssets) -> dict[str, Any]:
        return max(
            candidates,
            key=lambda item: (
                assets.severity_rank(item.get("severityLabel")),
                float(item.get("severityScore") or 0.0),
            ),
        )

    @staticmethod
    def _class_margin(probabilities: np.ndarray) -> float:
        if probabilities.size < 2:
            return 1.0
        ordered = np.sort(probabilities)
        return round(float(ordered[-1] - ordered[-2]), 4)

    def _require_assets(self) -> ModelAssets:
        if self._model_assets is not None:
            return self._model_assets
        from app.core.config import Settings

        self._model_assets = ModelAssets(self._settings or Settings())
        return self._model_assets

    def _channels(self) -> int:
        manifest = self._require_assets().grading_manifest
        input_spec = manifest.raw.get("inputSpec", {})
        try:
            return max(1, int(input_spec.get("imageChannels") or 1))
        except (TypeError, ValueError):
            return 1

    @staticmethod
    def _resolve_layout(shape: list[Any]) -> str:
        if len(shape) == 4:
            if isinstance(shape[-1], int) and shape[-1] in {1, 3}:
                return "NHWC"
            if isinstance(shape[1], int) and shape[1] in {1, 3}:
                return "NCHW"
        return "NCHW"
