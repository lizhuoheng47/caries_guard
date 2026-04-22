from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput
from app.services.image_fetch_service import FetchedImage

log = get_logger("cariesguard-ai.pipeline.detection")


class DetectionPipeline:
    """Tooth detection pipeline without fabricated fallback results."""

    def __init__(self, registry: ModelRegistry, _settings: Any) -> None:
        self._registry = registry

    def detect_all(
        self,
        images: list[ImageInput],
        fetched_images: list[FetchedImage],
    ) -> list[ToothDetection]:
        all_detections: list[ToothDetection] = []
        for image in images:
            image_path = self._find_path(image, fetched_images)
            all_detections.extend(self.detect(image, image_path))
        return all_detections

    def detect(self, image: ImageInput, image_path: Path | None = None) -> list[ToothDetection]:
        if not self._registry.is_module_real("tooth_detect"):
            raise BusinessException("M5003", "tooth detection module is disabled")

        adapter = self._registry.get_tooth_detector()
        if adapter is None:
            raise BusinessException("M5004", "tooth detector adapter is unavailable")
        if image_path is None:
            raise BusinessException("M5005", "tooth detection image is unavailable")

        try:
            result = adapter.infer(image_path)
        except Exception as exc:
            raise BusinessException("M5006", f"tooth detection failed: {exc}") from exc
        return self._to_schema(image, result)

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_tooth_detector()
        return adapter.impl_type.value if adapter is not None else "DISABLED"

    @staticmethod
    def _to_schema(image: ImageInput, result: dict[str, Any]) -> list[ToothDetection]:
        image_id = image.image_id if image else None
        detections: list[ToothDetection] = []
        for det in result.get("detections", []):
            detections.append(
                ToothDetection(
                    image_id=image_id,
                    tooth_code=det.get("toothCode", "UNKNOWN"),
                    bbox=det.get("bbox", [0, 0, 0, 0]),
                    detection_score=det.get("score", 0.0),
                )
            )
        return detections

    @staticmethod
    def _find_path(image: ImageInput, fetched: list[FetchedImage]) -> Path | None:
        for item in fetched:
            if item.image_id == image.image_id:
                return item.path
        return fetched[0].path if fetched else None
