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
from app.pipelines.uncertainty_pipeline import UncertaintyPipeline
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
        self._uncertainty_pipeline = UncertaintyPipeline(settings)

    def grade(
        self,
        image: ImageInput | None,
        image_path: Path | None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detections: list[ToothDetection] | None = None,
        quality_results: list[Any] | None = None,
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
            model_adapter: Any = adapter
            result = model_adapter.infer(image_path, segmentation_regions or [], tooth_detections or [])
            return self._real_result(
                result,
                segmentation_regions or [],
                tooth_detections or [],
                quality_results or [],
            )
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

    def _real_result(
        self,
        result: dict[str, Any],
        segmentation_regions: list[dict[str, Any]],
        tooth_detections: list[ToothDetection],
        quality_results: list[Any],
    ) -> GradingResult:
        base_uncertainty = self._score(result.get("uncertaintyScore"), 0.5)
        confidence_score = self._score(result.get("confidenceScore"), 1.0 - base_uncertainty)
        raw = dict(result.get("rawResult") or {})
        lesion_grades = self._build_lesion_grades(segmentation_regions, raw.get("candidates"))
        uncertainty_result = self._uncertainty_pipeline.evaluate(
            quality_results=quality_results,
            tooth_detections=tooth_detections,
            lesion_regions=lesion_grades or segmentation_regions,
            grading_confidence=confidence_score,
            grading_raw=raw,
            base_uncertainty=base_uncertainty,
        )
        uncertainty_score = uncertainty_result.uncertainty_score
        needs_review = uncertainty_result.needs_review
        raw.update(
            {
                "reviewThreshold": self._settings.uncertainty_review_threshold,
                "needsReview": needs_review,
                "uncertaintyMode": "real",
                "uncertaintyImplType": "COMPOSITE_HEURISTIC",
                "uncertaintyReasons": uncertainty_result.uncertainty_reasons,
                "uncertaintyComponents": uncertainty_result.components,
                "lesionGrades": lesion_grades,
                "toothAggregation": self._tooth_aggregation(lesion_grades),
                "imageAggregation": {
                    "highestSeverity": str(result.get("gradingLabel") or "C1"),
                    "lesionCount": len(lesion_grades),
                    "confidenceScore": confidence_score,
                    "uncertaintyScore": uncertainty_score,
                    "needsReview": needs_review,
                },
            }
        )
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
            "uncertaintyMode": "mock",
            "uncertaintyImplType": "MOCK",
            "uncertaintyReasons": ["MOCK_BASELINE"],
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

    @staticmethod
    def _build_lesion_grades(
        segmentation_regions: list[dict[str, Any]],
        candidates: Any,
    ) -> list[dict[str, Any]]:
        if not isinstance(candidates, list) or not candidates:
            return []
        by_region: dict[int, dict[str, Any]] = {}
        for item in candidates:
            if not isinstance(item, dict):
                continue
            try:
                index = int(item.get("regionIndex") or item.get("region_index") or 0)
            except (TypeError, ValueError):
                index = 0
            by_region[index] = item

        lesion_grades: list[dict[str, Any]] = []
        for index, region in enumerate(segmentation_regions):
            candidate = by_region.get(index)
            if candidate is None:
                continue
            lesion_grades.append(
                {
                    "regionIndex": index,
                    "toothCode": str(region.get("toothCode") or region.get("tooth_code") or candidate.get("toothCode") or "16"),
                    "severityCode": str(candidate.get("severityLabel") or "C1"),
                    "severityScore": float(candidate.get("severityScore") or 0.0),
                    "confidenceScore": round(max(0.0, min(1.0, 1.0 - float(candidate.get("boundaryDistance") or 0.5))), 4),
                    "boundaryDistance": float(candidate.get("boundaryDistance") or 0.0),
                    "bbox": region.get("bbox") if isinstance(region.get("bbox"), list) else candidate.get("bbox"),
                    "score": candidate.get("segmentationScore") or region.get("score"),
                }
            )
        return lesion_grades

    @staticmethod
    def _tooth_aggregation(lesion_grades: list[dict[str, Any]]) -> list[dict[str, Any]]:
        order = {"C0": 0, "C1": 1, "C2": 2, "C3": 3}
        grouped: dict[str, dict[str, Any]] = {}
        for lesion in lesion_grades:
            tooth = str(lesion.get("toothCode") or "UNKNOWN")
            severity = str(lesion.get("severityCode") or "C0")
            current = grouped.get(tooth)
            if current is None:
                grouped[tooth] = {
                    "toothCode": tooth,
                    "highestSeverity": severity,
                    "lesionCount": 1,
                    "maxSeverityScore": float(lesion.get("severityScore") or 0.0),
                }
                continue
            current["lesionCount"] += 1
            if order.get(severity, 0) > order.get(str(current["highestSeverity"]), 0):
                current["highestSeverity"] = severity
            current["maxSeverityScore"] = max(float(current["maxSeverityScore"]), float(lesion.get("severityScore") or 0.0))
        return list(grouped.values())

