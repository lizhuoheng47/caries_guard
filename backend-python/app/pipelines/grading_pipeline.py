"""Caries grading sub-pipeline with mode-aware routing and fallback.

Fallback rules:
- mock: always return mock grading.
- hybrid: attempt real adapter; on failure, fallback to mock and record fallback.
- real: attempt real adapter; on failure, raise BusinessException.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput

log = get_logger("cariesguard-ai.pipeline.grading")


@dataclass(frozen=True)
class GradingResult:
    grading_mode: str
    grading_impl_type: str
    grading_label: str
    confidence_score: float
    uncertainty_score: float
    needs_review: bool
    raw_result: dict[str, Any]


class GradingPipeline:
    """Grading pipeline with mock / heuristic routing."""

    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings

    def grade(
        self,
        image: ImageInput | None,
        image_path: Path | None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detections: list[ToothDetection] | None = None,
    ) -> GradingResult:
        """Run grading and apply the uncertainty review business rule."""
        mode = self._registry.get_runtime_mode()

        if not self._registry.is_module_real("grading"):
            return self._mock_result(image)

        adapter = self._registry.get_grading_model()
        if adapter is None or image_path is None:
            if mode == "real":
                raise BusinessException(
                    "M5007",
                    "grading adapter or image not available in real mode",
                )
            log.warning("grading adapter/image unavailable - fallback to mock (hybrid)")
            return self._mock_result(image, fallback_reason="adapter_or_image_unavailable")

        try:
            if self._settings.grading_force_fail:
                raise RuntimeError("forced grading failure")
            result = adapter.infer(image_path, segmentation_regions or [], tooth_detections or [])
            return self._real_result(result)
        except Exception as exc:
            if mode == "real":
                raise BusinessException("M5008", f"grading failed: {exc}") from exc
            log.warning("grading failed - fallback to mock (hybrid) error=%s", exc)
            return self._mock_result(image, fallback_reason=str(exc))

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_grading_model()
        if adapter is not None and self._registry.is_module_real("grading"):
            return adapter.impl_type.value
        return ImplType.MOCK.value

    def _real_result(self, result: dict[str, Any]) -> GradingResult:
        uncertainty_score = self._score(result.get("uncertaintyScore"), 0.5)
        confidence_score = self._score(result.get("confidenceScore"), 1.0 - uncertainty_score)
        needs_review = self._needs_review(uncertainty_score)
        raw = dict(result.get("rawResult") or {})
        raw.update({
            "reviewThreshold": self._settings.uncertainty_review_threshold,
            "needsReview": needs_review,
        })
        log.info(
            "grading completed mode=real implType=%s label=%s uncertainty=%s threshold=%s needsReview=%s",
            result.get("implType") or ImplType.HEURISTIC.value,
            result.get("gradingLabel") or "C1",
            uncertainty_score,
            self._settings.uncertainty_review_threshold,
            needs_review,
        )
        return GradingResult(
            grading_mode="real",
            grading_impl_type=str(result.get("implType") or ImplType.HEURISTIC.value),
            grading_label=str(result.get("gradingLabel") or "C1"),
            confidence_score=confidence_score,
            uncertainty_score=uncertainty_score,
            needs_review=needs_review,
            raw_result=raw,
        )

    def _mock_result(
        self,
        image: ImageInput | None,
        fallback_reason: str | None = None,
    ) -> GradingResult:
        uncertainty_score = 0.1
        needs_review = self._needs_review(uncertainty_score)
        raw_result: dict[str, Any] = {
            "source": "mock",
            "imageId": image.image_id if image else None,
            "reviewThreshold": self._settings.uncertainty_review_threshold,
            "needsReview": needs_review,
        }
        if fallback_reason:
            raw_result["fallbackReason"] = fallback_reason
        log.info(
            "grading completed mode=mock implType=MOCK label=C1 uncertainty=%s threshold=%s needsReview=%s",
            uncertainty_score,
            self._settings.uncertainty_review_threshold,
            needs_review,
        )
        return GradingResult(
            grading_mode="mock",
            grading_impl_type=ImplType.MOCK.value,
            grading_label="C1",
            confidence_score=0.9,
            uncertainty_score=uncertainty_score,
            needs_review=needs_review,
            raw_result=raw_result,
        )

    def _needs_review(self, uncertainty_score: float) -> bool:
        return uncertainty_score >= self._settings.uncertainty_review_threshold

    @staticmethod
    def _score(value: Any, default: float) -> float:
        try:
            score = float(value)
        except (TypeError, ValueError):
            score = default
        return round(max(0.0, min(1.0, score)), 4)
