from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import numpy as np
import yaml

from app.core.image_utils import ensure_channel_dim, load_image, normalize_image, resize_image
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.tooth-detector-yolo")


class ToothDetectorYoloAdapter(BaseModelAdapter):
    model_code = "tooth-detect-yolo-v8"
    model_type_code = "DETECTION"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5, settings: Any | None = None) -> None:
        self._settings = settings
        self._confidence_threshold = confidence_threshold
        self._loaded = False
        self._checkpoint_path: Path | None = None
        self._config_path: Path | None = None
        self._runtime_kind: str | None = None
        self._session: Any | None = None
        self._model: Any | None = None
        self._input_name: str | None = None
        self._input_shape: list[Any] = []
        self._input_layout: str = "NCHW"
        self._metadata: dict[str, Any] = {}
        self._device: str = "cpu"

    def load(self) -> None:
        checkpoint = self._discover_checkpoint()
        config = self._discover_config(checkpoint)
        metadata = self._load_metadata(config)
        label_order = self._label_order(metadata)
        input_size = self._parse_size(
            metadata.get("expectedImageSize")
            or metadata.get("inputSize")
            or metadata.get("imageSize")
            or metadata.get("imgsz")
            or self._nested(metadata, "inputSpec", "expectedImageSize")
        )
        if checkpoint is None:
            raise RuntimeError("tooth detection checkpoint is missing")
        if config is None:
            raise RuntimeError("tooth detection metadata/config is missing")
        if not label_order:
            raise RuntimeError("tooth detection metadata is missing label mapping / FDI codes")
        if input_size is None:
            raise RuntimeError("tooth detection metadata is missing expected input size")

        self._checkpoint_path = checkpoint
        self._config_path = config
        self._metadata = metadata
        self._metadata["labelOrder"] = label_order
        self._metadata["inputSize"] = input_size
        self.model_code = str(metadata.get("modelCode") or self.model_code)

        suffix = checkpoint.suffix.lower()
        if suffix == ".onnx":
            self._load_onnx(checkpoint)
        elif suffix in {".pt", ".pth", ".torchscript"}:
            self._load_torch(checkpoint)
        else:
            raise RuntimeError(f"unsupported tooth detection checkpoint format: {checkpoint.suffix}")
        self._loaded = True
        log.info(
            "loaded tooth detection model checkpoint=%s runtime=%s config=%s",
            checkpoint,
            self._runtime_kind,
            config,
        )

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._session = None
        self._model = None
        self._loaded = False

    def infer(self, image_path: Path) -> dict[str, Any]:
        if not self._loaded:
            raise RuntimeError("tooth detector adapter is not loaded")
        loaded = load_image(image_path)
        tensor = self._preprocess(loaded)
        raw = self._run_model(tensor)
        detections = self._decode_detections(raw, loaded.width, loaded.height)
        if not detections:
            raise RuntimeError("tooth detector produced no detections above threshold")
        return {
            "detections": detections,
            "implType": ImplType.ML_MODEL.value,
            "rawResult": {
                "modelCode": self.model_code,
                "checkpointPath": str(self._checkpoint_path),
                "configPath": str(self._config_path),
                "runtime": self._runtime_kind,
                "inputShape": self._input_shape,
                "inputLayout": self._input_layout,
                "device": self._device,
                "confidenceThreshold": self._score_threshold(),
                "nmsThreshold": self._nms_threshold(),
                "labelOrder": list(self._metadata.get("labelOrder") or []),
                "expectedImageSize": list(self._metadata.get("inputSize") or []),
            },
        }

    def _load_onnx(self, checkpoint: Path) -> None:
        try:
            import onnxruntime as ort
        except ImportError as exc:
            raise RuntimeError("onnxruntime is required for tooth detection ONNX inference") from exc
        providers = ["CPUExecutionProvider"]
        self._session = ort.InferenceSession(str(checkpoint), providers=providers)
        inputs = self._session.get_inputs()
        if not inputs:
            raise RuntimeError("tooth detection ONNX model has no inputs")
        self._input_name = inputs[0].name
        self._input_shape = list(getattr(inputs[0], "shape", []) or [])
        self._input_layout = self._resolve_layout(self._input_shape)
        self._runtime_kind = "onnx"
        self._device = "cpu"

    def _load_torch(self, checkpoint: Path) -> None:
        try:
            import torch
        except ImportError as exc:
            raise RuntimeError("torch is required for tooth detection .pt/.pth inference") from exc
        requested = str(getattr(self._settings, "model_device", "cpu") or "cpu").strip().lower()
        if requested.startswith("cuda") and not torch.cuda.is_available():
            raise RuntimeError(f"requested CUDA device is unavailable: {requested}")
        device = torch.device(requested if requested else "cpu")
        try:
            model = torch.jit.load(str(checkpoint), map_location=device)
        except Exception as exc:
            raise RuntimeError(
                "only TorchScript tooth detection checkpoints are supported in this runtime"
            ) from exc
        model.eval()
        self._model = model
        self._runtime_kind = "torchscript"
        self._device = str(device)
        self._input_layout = str(self._metadata.get("inputLayout") or "NCHW").upper()
        self._input_shape = [1, self._channels(), self._metadata["inputSize"][1], self._metadata["inputSize"][0]]

    def _preprocess(self, loaded: Any) -> np.ndarray:
        pixels = loaded.pixels
        channels = self._channels()
        target_size = tuple(self._metadata["inputSize"])
        if channels == 3 and pixels.ndim == 2:
            pixels = np.repeat(pixels[:, :, None], 3, axis=2)
        elif channels == 1 and pixels.ndim == 3:
            pixels = pixels[:, :, 0]
        resized = resize_image(pixels, target_size, interpolation=self._interpolation())
        arranged = ensure_channel_dim(resized, channels)
        normalized = normalize_image(
            arranged,
            min_value=self._clip_min(),
            max_value=self._clip_max(),
            normalize_mode=self._normalize_mode(),
            mean=self._mean(),
            std=self._std(),
            channels=channels,
        )
        if self._input_layout == "NHWC":
            return normalized[None, :, :, :].astype(np.float32)
        return np.transpose(normalized, (2, 0, 1))[None, :, :, :].astype(np.float32)

    def _run_model(self, tensor: np.ndarray) -> np.ndarray:
        if self._runtime_kind == "onnx":
            outputs = self._session.run(None, {self._input_name: tensor})
            if not outputs:
                raise RuntimeError("tooth detection ONNX model returned no outputs")
            return np.asarray(outputs[0])
        try:
            import torch
        except ImportError as exc:
            raise RuntimeError("torch is required for tooth detection torchscript inference") from exc
        with torch.inference_mode():
            output = self._model(torch.from_numpy(tensor).to(self._device))
        if isinstance(output, (tuple, list)):
            output = output[0]
        if hasattr(output, "detach"):
            output = output.detach().cpu().numpy()
        return np.asarray(output)

    def _decode_detections(self, raw: np.ndarray, width: int, height: int) -> list[dict[str, Any]]:
        arr = np.asarray(raw, dtype=np.float32)
        if arr.ndim == 3 and arr.shape[0] == 1:
            arr = arr[0]
        if arr.ndim != 2 or arr.shape[1] < 6:
            raise RuntimeError(f"unsupported tooth detection output shape: {list(arr.shape)}")

        bbox_format = str(self._metadata.get("bboxFormat") or "xyxy").strip().lower()
        labels = list(self._metadata.get("labelOrder") or [])
        score_threshold = self._score_threshold()
        candidates: list[dict[str, Any]] = []
        for row in arr:
            if row.shape[0] >= 6 and row.shape[0] <= 8:
                class_id = int(row[5])
                score = float(row[4])
                bbox_values = row[:4]
            else:
                class_scores = row[4:]
                class_id = int(np.argmax(class_scores))
                score = float(class_scores[class_id])
                bbox_values = row[:4]
            if score < score_threshold:
                continue
            bbox = self._decode_box(bbox_values.tolist(), bbox_format, width, height)
            tooth_code = labels[class_id] if 0 <= class_id < len(labels) else f"CLASS_{class_id}"
            candidates.append(
                {
                    "toothCode": tooth_code,
                    "bbox": bbox,
                    "score": round(max(0.0, min(0.9999, score)), 4),
                    "classId": class_id,
                }
            )
        return self._nms(candidates, self._nms_threshold())

    def _decode_box(self, values: list[float], bbox_format: str, width: int, height: int) -> list[int]:
        if bbox_format == "cxcywh":
            cx, cy, box_w, box_h = values
            x1 = cx - box_w / 2.0
            y1 = cy - box_h / 2.0
            x2 = cx + box_w / 2.0
            y2 = cy + box_h / 2.0
        else:
            x1, y1, x2, y2 = values
        if max(abs(x1), abs(y1), abs(x2), abs(y2)) <= 1.5:
            x1 *= width
            x2 *= width
            y1 *= height
            y2 *= height
        return [
            max(0, min(width - 1, int(round(x1)))),
            max(0, min(height - 1, int(round(y1)))),
            max(0, min(width - 1, int(round(x2)))),
            max(0, min(height - 1, int(round(y2)))),
        ]

    @staticmethod
    def _nms(items: list[dict[str, Any]], threshold: float) -> list[dict[str, Any]]:
        ordered = sorted(items, key=lambda item: float(item.get("score") or 0.0), reverse=True)
        selected: list[dict[str, Any]] = []
        for candidate in ordered:
            if any(ToothDetectorYoloAdapter._iou(candidate["bbox"], item["bbox"]) > threshold for item in selected):
                continue
            selected.append(candidate)
        return selected

    @staticmethod
    def _iou(left: list[int], right: list[int]) -> float:
        x1 = max(left[0], right[0])
        y1 = max(left[1], right[1])
        x2 = min(left[2], right[2])
        y2 = min(left[3], right[3])
        inter = max(0, x2 - x1) * max(0, y2 - y1)
        left_area = max(0, left[2] - left[0]) * max(0, left[3] - left[1])
        right_area = max(0, right[2] - right[0]) * max(0, right[3] - right[1])
        denom = max(left_area + right_area - inter, 1)
        return float(inter) / float(denom)

    def _discover_checkpoint(self) -> Path | None:
        configured = str(getattr(self._settings, "model_tooth_detect_checkpoint_path", "") or "").strip()
        if configured:
            path = self._resolve_path(Path(configured))
            if path.is_file():
                return path
        roots = [
            Path(__file__).resolve().parents[4] / "model-weights",
            Path(__file__).resolve().parents[3] / "assets",
            Path(__file__).resolve().parents[3] / "training" / "outputs",
        ]
        patterns = ("*tooth*.onnx", "*tooth*.pt", "*detect*.onnx", "*detect*.pt", "*yolo*.onnx", "*yolo*.pt")
        for root in roots:
            if not root.exists():
                continue
            for pattern in patterns:
                hit = next(root.rglob(pattern), None)
                if hit is not None and hit.is_file():
                    return hit
        return None

    def _discover_config(self, checkpoint: Path | None) -> Path | None:
        configured = str(getattr(self._settings, "model_tooth_detect_config_path", "") or "").strip()
        if configured:
            path = self._resolve_path(Path(configured))
            if path.is_file():
                return path
        if checkpoint is None:
            return None
        for pattern in ("*.json", "*.yaml", "*.yml"):
            hit = next(checkpoint.parent.glob(pattern), None)
            if hit is not None and hit.is_file():
                return hit
        return None

    def _load_metadata(self, config: Path | None) -> dict[str, Any]:
        if config is None:
            return {}
        with config.open("r", encoding="utf-8") as fp:
            if config.suffix.lower() == ".json":
                loaded = json.load(fp)
            else:
                loaded = yaml.safe_load(fp)
        if not isinstance(loaded, dict):
            raise RuntimeError(f"tooth detection config must be a mapping: {config}")
        return loaded

    @staticmethod
    def _resolve_layout(shape: list[Any]) -> str:
        if len(shape) == 4:
            if isinstance(shape[-1], int) and shape[-1] in {1, 3}:
                return "NHWC"
            if isinstance(shape[1], int) and shape[1] in {1, 3}:
                return "NCHW"
        return "NCHW"

    @staticmethod
    def _parse_size(value: Any) -> list[int] | None:
        if isinstance(value, (list, tuple)) and len(value) == 2:
            try:
                return [int(value[0]), int(value[1])]
            except (TypeError, ValueError):
                return None
        return None

    @staticmethod
    def _nested(mapping: dict[str, Any], *keys: str) -> Any:
        current: Any = mapping
        for key in keys:
            if not isinstance(current, dict):
                return None
            current = current.get(key)
        return current

    @staticmethod
    def _label_order(mapping: dict[str, Any]) -> list[str]:
        for key in ("labelOrder", "classNames", "classLabels", "toothCodes", "fdiCodes"):
            value = mapping.get(key)
            if isinstance(value, list):
                return [str(item).strip() for item in value if str(item).strip()]
        return []

    @staticmethod
    def _resolve_path(path: Path) -> Path:
        normalized = path.as_posix()
        repo_root = Path(__file__).resolve().parents[4]
        project_root = Path(__file__).resolve().parents[3]
        if normalized.startswith("/app/"):
            return (project_root / normalized.removeprefix("/app/")).resolve()
        if path.is_absolute():
            return path
        return (repo_root / path).resolve()

    def _channels(self) -> int:
        raw = self._metadata.get("imageChannels") or self._nested(self._metadata, "inputSpec", "imageChannels")
        try:
            return max(1, int(raw))
        except (TypeError, ValueError):
            color_mode = str(self._metadata.get("imageColorMode") or "grayscale").strip().lower()
            return 3 if color_mode in {"rgb", "color"} else 1

    def _interpolation(self) -> str:
        return str(self._metadata.get("interpolation") or "bilinear")

    def _normalize_mode(self) -> str:
        normalize = self._metadata.get("normalize")
        if isinstance(normalize, dict):
            return str(normalize.get("mode") or "minmax_0_1")
        return "minmax_0_1"

    def _mean(self) -> list[float] | None:
        normalize = self._metadata.get("normalize")
        return normalize.get("mean") if isinstance(normalize, dict) else None

    def _std(self) -> list[float] | None:
        normalize = self._metadata.get("normalize")
        return normalize.get("std") if isinstance(normalize, dict) else None

    def _clip_min(self) -> float:
        value_clip = self._metadata.get("valueClip")
        if isinstance(value_clip, dict):
            try:
                return float(value_clip.get("min", 0.0))
            except (TypeError, ValueError):
                return 0.0
        return 0.0

    def _clip_max(self) -> float:
        value_clip = self._metadata.get("valueClip")
        if isinstance(value_clip, dict):
            try:
                return float(value_clip.get("max", 255.0))
            except (TypeError, ValueError):
                return 255.0
        return 255.0

    def _score_threshold(self) -> float:
        for key in ("confidenceThreshold", "scoreThreshold"):
            if key in self._metadata:
                try:
                    return float(self._metadata[key])
                except (TypeError, ValueError):
                    continue
        return self._confidence_threshold

    def _nms_threshold(self) -> float:
        for key in ("nmsThreshold", "iouThreshold"):
            if key in self._metadata:
                try:
                    return float(self._metadata[key])
                except (TypeError, ValueError):
                    continue
        return 0.45
