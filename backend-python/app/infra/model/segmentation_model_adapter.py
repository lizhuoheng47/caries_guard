from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np

from app.core.image_utils import ensure_channel_dim, load_image, normalize_image, resize_image
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.infra.model.checkpoint_validator import CheckpointValidator
from app.infra.model.manifest_loader import ModuleManifest
from app.infra.model.model_assets import ModelAssets

log = get_logger("cariesguard-ai.model.segmentation")


@dataclass(frozen=True)
class _PreprocessedImage:
    tensor: np.ndarray
    original_size: tuple[int, int]
    model_size: tuple[int, int]
    channel_count: int
    layout: str
    normalize_mode: str


class SegmentationModelAdapter(BaseModelAdapter):
    model_code = "caries-segmentation-v1"
    model_type_code = "SEGMENTATION"
    impl_type = ImplType.ML_MODEL

    def __init__(
        self,
        confidence_threshold: float = 0.5,
        model_assets: ModelAssets | None = None,
        settings: Any | None = None,
    ) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold
        self._settings = settings
        self._model_assets = model_assets
        self._validator = CheckpointValidator("segmentation")
        self._session: Any | None = None
        self._model: Any | None = None
        self._runtime_kind: str | None = None
        self._device: str = "cpu"
        self._input_name: str | None = None
        self._output_name: str | None = None
        self._input_shape: list[Any] = []
        self._output_shape: list[Any] = []
        self._input_layout = "NCHW"

    def load(self) -> None:
        assets = self._require_assets()
        manifest = assets.segmentation_manifest
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
        if arch in {"onnx", "onnx_segmentation", "segmentation_onnx"} or checkpoint_format == "onnx":
            self._load_onnx(manifest)
        elif arch in {"torchscript", "torchscript_segmentation", "segmentation_torchscript"} or checkpoint_format in {"pt", "pth", "torchscript"}:
            self._load_torch(manifest)
        else:
            self._validator._raise("load", f"unsupported segmentation arch={manifest.arch!r} format={checkpoint_format!r}")

        self._validator.validate_model_shapes(
            manifest,
            class_map=assets.class_map(),
            input_shape=self._input_shape,
            output_shape=self._output_shape,
        )
        self._loaded = True
        log.info(
            "loaded segmentation model checkpoint=%s runtime=%s arch=%s",
            manifest.checkpoint_path,
            self._runtime_kind,
            manifest.arch,
        )

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._session = None
        self._model = None
        self._input_name = None
        self._output_name = None
        self._loaded = False

    def infer(self, image_path: Path, tooth_detections: list[Any] | None = None) -> dict[str, Any]:
        if not self._loaded:
            raise RuntimeError("segmentation model adapter is not loaded")

        assets = self._require_assets()
        preprocessed = self._preprocess_image(image_path, assets)
        raw_output = self._run_model(preprocessed.tensor)
        probability = self._output_to_probability(raw_output, assets.segmentation_foreground_class_id())
        probability = self._resize_probability(probability, preprocessed.original_size)
        regions, mask_array = self._postprocess_probability(
            probability,
            tooth_detections or [],
            assets,
        )
        if not regions:
            raise RuntimeError("segmentation model produced no lesion regions above postprocess thresholds")

        manifest = assets.segmentation_manifest
        score = round(float(np.mean([item["score"] for item in regions])), 4)
        return {
            "regions": regions,
            "maskArray": mask_array,
            "segmentationScore": score,
            "implType": ImplType.ML_MODEL.value,
            "rawResult": {
                "modelCode": self.model_code,
                "datasetVersion": manifest.dataset_version,
                "manifestStatus": manifest.status,
                "manifestPath": str(manifest.manifest_path),
                "checkpointPath": str(manifest.checkpoint_path),
                "checkpointSha256": manifest.checkpoint_actual_sha256 or manifest.checkpoint_sha256,
                "checkpointDeclaredSha256": manifest.checkpoint_declared_sha256,
                "exportedAt": manifest.exported_at,
                "classMapPath": str(assets.class_map_path),
                "preprocessPath": str(assets.preprocess_path),
                "postprocessPath": str(assets.postprocess_path),
                "arch": manifest.arch,
                "runtime": self._runtime_kind,
                "device": self._device,
                "inputName": self._input_name,
                "outputName": self._output_name,
                "inputShape": self._input_shape,
                "outputShape": self._output_shape,
                "inputLayout": preprocessed.layout,
                "imageSize": [preprocessed.original_size[0], preprocessed.original_size[1]],
                "modelInputSize": [preprocessed.model_size[0], preprocessed.model_size[1]],
                "imageChannels": preprocessed.channel_count,
                "normalizeMode": preprocessed.normalize_mode,
                "maskThreshold": assets.segmentation_mask_threshold(),
                "minRegionAreaPx": assets.segmentation_min_region_area(0),
                "regionCount": len(regions),
                "maskPixels": int(np.sum(mask_array > 0)),
                "confidenceThreshold": self._confidence_threshold,
            },
        }

    def _load_onnx(self, manifest: ModuleManifest) -> None:
        try:
            import onnxruntime as ort
        except ImportError as exc:  # pragma: no cover - depends on runtime image
            raise RuntimeError("onnxruntime is required for segmentation ONNX inference") from exc

        providers = self._providers(ort)
        session = ort.InferenceSession(str(manifest.checkpoint_path), providers=providers)
        inputs = session.get_inputs()
        outputs = session.get_outputs()
        if not inputs:
            raise RuntimeError("segmentation ONNX model has no inputs")
        if not outputs:
            raise RuntimeError("segmentation ONNX model has no outputs")

        self._session = session
        self._model = None
        self._runtime_kind = "onnx"
        self._device = "cpu"
        self._input_name = inputs[0].name
        self._output_name = outputs[0].name
        self._input_shape = list(getattr(inputs[0], "shape", []) or [])
        self._output_shape = list(getattr(outputs[0], "shape", []) or [])
        self._input_layout = self._resolve_layout(self._input_shape)

    def _load_torch(self, manifest: ModuleManifest) -> None:
        try:
            import torch
        except ImportError as exc:  # pragma: no cover - depends on runtime image
            raise RuntimeError("torch is required for segmentation TorchScript inference") from exc

        requested = str(getattr(self._settings, "model_device", "cpu") or "cpu").strip().lower()
        if requested.startswith("cuda") and not torch.cuda.is_available():
            raise RuntimeError(f"requested CUDA device is unavailable: {requested}")
        device = torch.device(requested if requested else "cpu")
        try:
            model = torch.jit.load(str(manifest.checkpoint_path), map_location=device)
        except Exception as exc:  # pragma: no cover - depends on external checkpoint
            raise RuntimeError("only TorchScript segmentation checkpoints are supported in this runtime") from exc

        model.eval()
        width, height = manifest.expected_input_size or (512, 512)
        channels = self._input_channels(manifest.raw, self._require_assets().preprocess_config().get("shared", {}))
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
        self._output_name = "output"
        self._input_shape = [1, channels, height, width]
        self._output_shape = list(getattr(output, "shape", []) or [])
        self._input_layout = "NCHW"

    def _run_model(self, tensor: np.ndarray) -> Any:
        if self._runtime_kind == "onnx":
            outputs = self._session.run(None, {self._input_name: tensor})
            if not outputs:
                raise RuntimeError("segmentation ONNX model returned no outputs")
            return outputs[0]

        try:
            import torch
        except ImportError as exc:  # pragma: no cover - depends on runtime image
            raise RuntimeError("torch is required for segmentation TorchScript inference") from exc

        with torch.inference_mode():
            output = self._model(torch.from_numpy(tensor).to(self._device))
        if isinstance(output, (tuple, list)):
            output = output[0]
        if hasattr(output, "detach"):
            output = output.detach().cpu().numpy()
        return output

    def _require_assets(self) -> ModelAssets:
        if self._model_assets is not None:
            return self._model_assets

        from app.core.config import Settings

        self._model_assets = ModelAssets(self._settings or Settings())
        return self._model_assets

    def _preprocess_image(self, image_path: Path, assets: ModelAssets) -> _PreprocessedImage:
        manifest = assets.segmentation_manifest
        preprocess = assets.preprocess_config()
        shared = preprocess.get("shared", {})
        normalize = shared.get("normalize", {})
        expected_size = manifest.expected_input_size or self._parse_size(shared.get("imageSize")) or (512, 512)
        channel_count = self._input_channels(manifest.raw, shared)

        loaded = load_image(image_path)
        original_size = (loaded.width, loaded.height)
        pixels = loaded.pixels
        if channel_count == 3 and pixels.ndim == 2:
            pixels = np.repeat(pixels[:, :, None], 3, axis=2)
        elif channel_count == 1 and pixels.ndim == 3:
            pixels = pixels[:, :, 0]
        arr = resize_image(pixels, expected_size, interpolation=str(shared.get("interpolation") or "bilinear"))
        arr = ensure_channel_dim(arr, channel_count)

        clip = shared.get("valueClip", {})
        min_value = float(clip.get("min", 0.0))
        max_value = float(clip.get("max", 255.0))
        if max_value <= min_value:
            raise RuntimeError(f"invalid preprocess valueClip range: min={min_value} max={max_value}")

        normalize_mode = str(normalize.get("mode") or "minmax_0_1")
        arr = normalize_image(
            arr,
            min_value=min_value,
            max_value=max_value,
            normalize_mode=normalize_mode,
            mean=normalize.get("mean"),
            std=normalize.get("std"),
            channels=channel_count,
        )

        layout = self._input_layout
        if layout == "NHWC":
            tensor = arr[None, :, :, :].astype(np.float32)
        else:
            tensor = np.transpose(arr, (2, 0, 1))[None, :, :, :].astype(np.float32)
        return _PreprocessedImage(
            tensor=tensor,
            original_size=original_size,
            model_size=expected_size,
            channel_count=channel_count,
            layout=layout,
            normalize_mode=normalize_mode,
        )

    def _output_to_probability(self, raw_output: Any, foreground_class_id: int) -> np.ndarray:
        output = np.asarray(raw_output, dtype=np.float32)
        output = np.squeeze(output, axis=0) if output.ndim >= 3 and output.shape[0] == 1 else output

        if output.ndim == 3:
            if output.shape[0] <= 8:
                channel_count = output.shape[0]
                if channel_count == 1:
                    logits_or_prob = output[0]
                    return self._binary_probability(logits_or_prob)
                class_id = min(max(0, int(foreground_class_id)), channel_count - 1)
                return self._softmax(output, axis=0)[class_id]
            if output.shape[-1] <= 8:
                channel_count = output.shape[-1]
                if channel_count == 1:
                    return self._binary_probability(output[:, :, 0])
                class_id = min(max(0, int(foreground_class_id)), channel_count - 1)
                return self._softmax(output, axis=-1)[:, :, class_id]
        if output.ndim == 2:
            return self._binary_probability(output)
        raise RuntimeError(f"unsupported segmentation output shape: {list(output.shape)}")

    def _postprocess_probability(
        self,
        probability: np.ndarray,
        tooth_detections: list[Any],
        assets: ModelAssets,
    ) -> tuple[list[dict[str, Any]], np.ndarray]:
        threshold = assets.segmentation_mask_threshold(0.5)
        mask = probability >= threshold
        components = (
            self._connected_components(mask)
            if assets.segmentation_connected_components_enabled(True)
            else [np.argwhere(mask)]
        )
        min_area = assets.segmentation_min_region_area(0)
        height, width = mask.shape
        regions: list[dict[str, Any]] = []
        region_mask = np.zeros(mask.shape, dtype=np.uint8)

        for component in components:
            if component.size == 0 or len(component) < max(1, min_area):
                continue
            y_values = component[:, 0]
            x_values = component[:, 1]
            x1 = int(np.min(x_values))
            y1 = int(np.min(y_values))
            x2 = int(np.max(x_values))
            y2 = int(np.max(y_values))
            bbox = self._clamp_box([x1, y1, x2, y2], width, height)
            component_score = float(np.mean(probability[y_values, x_values]))
            if component_score < self._confidence_threshold:
                continue
            region_mask[y_values, x_values] = 255
            regions.append(
                {
                    "toothCode": self._match_tooth_code(bbox, tooth_detections),
                    "polygon": [[bbox[0], bbox[1]], [bbox[2], bbox[1]], [bbox[2], bbox[3]], [bbox[0], bbox[3]]],
                    "bbox": bbox,
                    "score": round(max(0.0, min(0.9999, component_score)), 4),
                    "regionIndex": len(regions),
                }
            )
        return regions, region_mask

    @staticmethod
    def _providers(ort: Any) -> list[str]:
        available = set(ort.get_available_providers())
        if "CPUExecutionProvider" in available:
            return ["CPUExecutionProvider"]
        return list(available)

    @staticmethod
    def _resolve_layout(shape: list[Any]) -> str:
        if len(shape) == 4:
            last = shape[-1]
            second = shape[1]
            if isinstance(last, int) and last in {1, 3}:
                return "NHWC"
            if isinstance(second, int) and second in {1, 3}:
                return "NCHW"
        return "NCHW"

    @staticmethod
    def _input_channels(manifest: dict[str, Any], shared_preprocess: dict[str, Any]) -> int:
        input_spec = manifest.get("inputSpec", {})
        try:
            return max(1, int(input_spec.get("imageChannels")))
        except (TypeError, ValueError):
            color_mode = str(shared_preprocess.get("imageColorMode") or "grayscale").lower()
            return 3 if color_mode in {"rgb", "color"} else 1

    @staticmethod
    def _parse_size(raw: Any) -> tuple[int, int] | None:
        if isinstance(raw, (list, tuple)) and len(raw) == 2:
            try:
                return int(raw[0]), int(raw[1])
            except (TypeError, ValueError):
                return None
        return None

    @staticmethod
    def _binary_probability(values: np.ndarray) -> np.ndarray:
        if float(np.nanmin(values)) < 0.0 or float(np.nanmax(values)) > 1.0:
            return 1.0 / (1.0 + np.exp(-values))
        return np.clip(values, 0.0, 1.0)

    @staticmethod
    def _softmax(values: np.ndarray, axis: int) -> np.ndarray:
        shifted = values - np.max(values, axis=axis, keepdims=True)
        exp = np.exp(shifted)
        return exp / np.maximum(np.sum(exp, axis=axis, keepdims=True), 1e-12)

    @staticmethod
    def _resize_probability(probability: np.ndarray, target_size: tuple[int, int]) -> np.ndarray:
        probability = np.clip(probability, 0.0, 1.0)
        resized = resize_image((probability * 255.0).astype(np.float32), target_size, interpolation="bilinear")
        return np.asarray(resized, dtype=np.float32) / 255.0

    @staticmethod
    def _connected_components(mask: np.ndarray) -> list[np.ndarray]:
        height, width = mask.shape
        visited = np.zeros(mask.shape, dtype=bool)
        components: list[np.ndarray] = []

        for y in range(height):
            for x in range(width):
                if visited[y, x] or not mask[y, x]:
                    continue
                stack = [(y, x)]
                visited[y, x] = True
                pixels: list[tuple[int, int]] = []
                while stack:
                    cy, cx = stack.pop()
                    pixels.append((cy, cx))
                    for ny in range(max(0, cy - 1), min(height, cy + 2)):
                        for nx in range(max(0, cx - 1), min(width, cx + 2)):
                            if visited[ny, nx] or not mask[ny, nx]:
                                continue
                            visited[ny, nx] = True
                            stack.append((ny, nx))
                components.append(np.asarray(pixels, dtype=np.int32))
        return components

    @staticmethod
    def _match_tooth_code(bbox: list[int], tooth_detections: list[Any]) -> str:
        best_code = "UNKNOWN"
        best_overlap = 0
        for detection in tooth_detections:
            det_bbox = getattr(detection, "bbox", None)
            tooth_code = getattr(detection, "tooth_code", None) or getattr(detection, "toothCode", None)
            if not det_bbox and isinstance(detection, dict):
                det_bbox = detection.get("bbox")
                tooth_code = detection.get("toothCode") or detection.get("tooth_code")
            if not det_bbox or len(det_bbox) != 4:
                continue
            overlap = SegmentationModelAdapter._overlap_area(bbox, [int(value) for value in det_bbox])
            if overlap > best_overlap:
                best_overlap = overlap
                best_code = str(tooth_code or "UNKNOWN")
        return best_code

    @staticmethod
    def _overlap_area(left: list[int], right: list[int]) -> int:
        x1 = max(left[0], right[0])
        y1 = max(left[1], right[1])
        x2 = min(left[2], right[2])
        y2 = min(left[3], right[3])
        return max(0, x2 - x1) * max(0, y2 - y1)

    @staticmethod
    def _clamp_box(box: list[int], width: int, height: int) -> list[int]:
        x1, y1, x2, y2 = box
        return [
            max(0, min(width - 1, x1)),
            max(0, min(height - 1, y1)),
            max(0, min(width - 1, x2)),
            max(0, min(height - 1, y2)),
        ]
