from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.exceptions import BusinessException
from app.infra.model.disease_detector_yolo import DiseaseDetectorYoloAdapter
from app.schemas.request import ImageInput
from app.services.image_fetch_service import FetchedImage


class DiseaseDetectionPipeline:
    def __init__(self, settings: Any) -> None:
        self._settings = settings
        self._adapter: DiseaseDetectorYoloAdapter | None = None
        if settings.model_disease_detect_enabled:
            self._adapter = DiseaseDetectorYoloAdapter(settings)
            self._adapter.load()

    def detect_all(
        self,
        images: list[ImageInput],
        fetched_images: list[FetchedImage],
    ) -> list[dict[str, Any]]:
        if self._adapter is None:
            return []
        detections: list[dict[str, Any]] = []
        for image in images:
            image_path = self._find_path(image, fetched_images)
            if image_path is None:
                raise BusinessException("M5010", "disease detection image is unavailable")
            try:
                image_detections = self._adapter.infer(image_path)
            except Exception as exc:
                raise BusinessException("M5011", f"DENTEX disease detection failed: {exc}") from exc
            for item in image_detections:
                detections.append({"imageId": image.image_id, **item})
        return detections

    def runtime_info(self) -> dict[str, Any]:
        return {
            "enabled": self._adapter is not None,
            "implType": "ML_MODEL" if self._adapter is not None else "DISABLED",
            "modelCode": self._adapter.model_code if self._adapter is not None else None,
            "checkpointPath": self._adapter.checkpoint_path if self._adapter is not None else None,
            "task": "abnormal-tooth-disease-detection",
        }

    @staticmethod
    def _find_path(image: ImageInput, fetched: list[FetchedImage]) -> Path | None:
        for item in fetched:
            if item.image_id == image.image_id:
                return item.path
        return fetched[0].path if fetched else None
