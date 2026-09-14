from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from app.core.logging import get_logger

log = get_logger("cariesguard-ai.model.disease-detector-yolo")


class DiseaseDetectorYoloAdapter:
    """Ultralytics runtime for the trained DENTEX disease detector.

    The model predicts disease classes, not FDI tooth identities. Keeping this
    adapter separate prevents disease labels from being consumed as tooth codes.
    """

    def __init__(self, settings: Any) -> None:
        self._settings = settings
        self._model: Any | None = None
        self._metadata: dict[str, Any] = {}
        self._checkpoint: Path | None = None

    def load(self) -> None:
        checkpoint = self._resolve_path(self._settings.model_disease_detect_checkpoint_path)
        metadata_path = self._resolve_path(self._settings.model_disease_detect_metadata_path)
        if not checkpoint.is_file():
            raise RuntimeError(f"DENTEX disease checkpoint is missing: {checkpoint}")
        if not metadata_path.is_file():
            raise RuntimeError(f"DENTEX disease metadata is missing: {metadata_path}")
        with metadata_path.open("r", encoding="utf-8") as stream:
            metadata = json.load(stream)
        labels = metadata.get("labelOrder")
        if not isinstance(labels, list) or not labels:
            raise RuntimeError("DENTEX disease metadata has no labelOrder")
        try:
            from ultralytics import YOLO
        except ImportError as exc:
            raise RuntimeError("ultralytics is required for DENTEX disease detection") from exc
        self._model = YOLO(str(checkpoint))
        self._metadata = metadata
        self._checkpoint = checkpoint
        log.info("loaded DENTEX disease detector checkpoint=%s", checkpoint)

    def is_loaded(self) -> bool:
        return self._model is not None

    def infer(self, image_path: Path) -> list[dict[str, Any]]:
        if self._model is None:
            raise RuntimeError("DENTEX disease detector is not loaded")
        results = self._model.predict(
            source=str(image_path),
            imgsz=int(self._settings.model_disease_detect_image_size),
            conf=float(self._settings.model_disease_detect_confidence_threshold),
            device=self._settings.model_device,
            verbose=False,
        )
        labels = [str(item) for item in self._metadata["labelOrder"]]
        detections: list[dict[str, Any]] = []
        for result in results:
            boxes = getattr(result, "boxes", None)
            if boxes is None:
                continue
            for xyxy, confidence, class_id in zip(boxes.xyxy.tolist(), boxes.conf.tolist(), boxes.cls.tolist()):
                # 类别顺序以随权重发布的元数据为准，不能依赖训练框架的隐式默认名称。
                index = int(class_id)
                detections.append(
                    {
                        "diseaseCode": labels[index] if 0 <= index < len(labels) else f"CLASS_{index}",
                        "bbox": [int(round(value)) for value in xyxy],
                        "confidenceScore": round(float(confidence), 4),
                    }
                )
        return detections

    @staticmethod
    def _resolve_path(raw_path: str) -> Path:
        path = Path(raw_path)
        if path.is_absolute():
            return path
        return (Path(__file__).resolve().parents[4] / path).resolve()

    @property
    def model_code(self) -> str:
        return str(self._metadata.get("modelCode") or "dentex-disease-detect-yolov8n-v1")

    @property
    def checkpoint_path(self) -> str | None:
        return str(self._checkpoint) if self._checkpoint else None
