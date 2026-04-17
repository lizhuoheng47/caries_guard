"""Tooth detection sub-pipeline with mode-aware routing and fallback.

Fallback rules (hard constraint from user review):
- **mock** — always return mock detections.
- **hybrid** — attempt real adapter; on failure, fallback to mock + log degradation.
- **real** — attempt real adapter; on failure, raise exception (NO silent fallback).
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput
from app.services.image_fetch_service import FetchedImage

log = get_logger("cariesguard-ai.pipeline.detection")


class DetectionPipeline:
    """Tooth detection pipeline with mock / heuristic routing."""

    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings

    def detect_all(
        self,
        images: list[ImageInput],
        fetched_images: list[FetchedImage],
    ) -> list[ToothDetection]:
        """Run detection on all images, combining results."""
        all_detections: list[ToothDetection] = []
        for image in images:
            image_path = self._find_path(image, fetched_images)
            all_detections.extend(self.detect(image, image_path))
        return all_detections

    def detect(self, image: ImageInput, image_path: Path | None = None) -> list[ToothDetection]:
        """Run detection on a single image, respecting the current runtime mode."""
        mode = self._registry.get_runtime_mode()

        if not self._registry.is_module_real("tooth_detect"):
            return self._mock_detections(image)

        # ── real / hybrid path ──────────────────────────────────────────
        adapter = self._registry.get_tooth_detector()
        if adapter is None or image_path is None:
            if mode == "real":
                raise BusinessException(
                    "M5003",
                    "tooth detector adapter or image not available in real mode",
                )
            log.warning("tooth detector/image unavailable — fallback to mock (hybrid)")
            return self._mock_detections(image)

        try:
            result = adapter.infer(image_path)
            return self._to_schema(image, result)
        except Exception as exc:
            if mode == "real":
                raise BusinessException("M5004", f"tooth detection failed: {exc}") from exc
            log.warning("tooth detection failed — fallback to mock (hybrid) error=%s", exc)
            return self._mock_detections(image)

    def get_last_impl_type(self) -> str:
        """Return the impl_type used in the most recent detection."""
        adapter = self._registry.get_tooth_detector()
        if adapter is not None and self._registry.is_module_real("tooth_detect"):
            return adapter.impl_type.value
        return ImplType.MOCK.value

    # ── private ──────────────────────────────────────────────────────────

    @staticmethod
    def _mock_detections(image: ImageInput) -> list[ToothDetection]:
        image_id = image.image_id if image else None
        return [
            ToothDetection(image_id=image_id, tooth_code="16", bbox=[64, 64, 180, 180], detection_score=0.95),
            ToothDetection(image_id=image_id, tooth_code="26", bbox=[220, 64, 340, 180], detection_score=0.93),
        ]

    @staticmethod
    def _to_schema(image: ImageInput, result: dict[str, Any]) -> list[ToothDetection]:
        """Convert adapter dict → list of callback schema objects."""
        image_id = image.image_id if image else None
        detections: list[ToothDetection] = []
        for det in result.get("detections", []):
            detections.append(
                ToothDetection(
                    image_id=image_id,
                    tooth_code=det.get("toothCode", "XX"),
                    bbox=det.get("bbox", [0, 0, 0, 0]),
                    detection_score=det.get("score", 0.0),
                )
            )
        return detections

    @staticmethod
    def _find_path(image: ImageInput, fetched: list[FetchedImage]) -> Path | None:
        """Find the local file path for the given ImageInput."""
        for f in fetched:
            if f.image_id == image.image_id:
                return f.path
        return fetched[0].path if fetched else None
