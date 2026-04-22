from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.base_model import ImplType
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput


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
    """Grading pipeline with class labels sourced from the external class map."""

    def __init__(self, registry: ModelRegistry, settings: Settings, model_assets: ModelAssets) -> None:
        self._registry = registry
        self._settings = settings
        self._model_assets = model_assets
        self._review_threshold = model_assets.uncertainty_review_threshold(settings.uncertainty_review_threshold)

    def grade(
        self,
        image: ImageInput | None,
        image_path: Path | None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detections: list[ToothDetection] | None = None,
        quality_results: list[Any] | None = None,
    ) -> GradingResult:
        if not self._registry.is_module_real("grading"):
            raise BusinessException("M5007", "grading module is disabled")

        adapter = self._registry.get_grading_model()
        if adapter is None:
            raise BusinessException("M5008", "grading adapter is unavailable")
        if image_path is None:
            raise BusinessException("M5009", "grading image is unavailable")

        try:
            if self._settings.grading_force_fail:
                raise RuntimeError("forced grading failure")
            result = adapter.infer(image_path, segmentation_regions or [], tooth_detections or [])
        except Exception as exc:
            raise BusinessException("M5010", f"grading failed: {exc}") from exc
        return self._real_result(result, segmentation_regions or [], tooth_detections or [], quality_results or [])

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_grading_model()
        return adapter.impl_type.value if adapter is not None else "DISABLED"

    def _real_result(
        self,
        result: dict[str, Any],
        segmentation_regions: list[dict[str, Any]],
        tooth_detections: list[ToothDetection],
        quality_results: list[Any],
    ) -> GradingResult:
        base_uncertainty = self._score(result.get("uncertaintyScore"), 0.5)
        grading_label = self._normalize_grading_label(result.get("gradingLabel"))
        confidence_score = self._score(result.get("confidenceScore"), 1.0 - base_uncertainty)
        raw = dict(result.get("rawResult") or {})
        lesion_grades = self._build_lesion_grades(
            segmentation_regions,
            raw.get("candidates"),
            default_severity=grading_label,
            default_confidence=confidence_score,
        )
        class_margin = self._score(
            raw.get("classMargin") if "classMargin" in raw else raw.get("boundaryDistance"),
            0.0,
        )
        uncertainty_reasons = self._uncertainty_reasons(
            uncertainty_score=base_uncertainty,
            confidence_score=confidence_score,
            class_margin=class_margin,
            quality_results=quality_results,
            tooth_detections=tooth_detections,
            lesion_grades=lesion_grades,
        )
        needs_review = self._needs_review(base_uncertainty)
        raw.update(
            {
                "reviewThreshold": self._review_threshold,
                "needsReview": needs_review,
                "uncertaintyMode": "real",
                "uncertaintyImplType": str(result.get("implType") or ImplType.HEURISTIC.value),
                "uncertaintyReasons": uncertainty_reasons,
                "classMargin": class_margin,
                "lesionGrades": lesion_grades,
                "toothAggregation": self._tooth_aggregation(lesion_grades),
                "imageAggregation": {
                    "highestSeverity": grading_label,
                    "lesionCount": len(lesion_grades),
                    "confidenceScore": confidence_score,
                    "uncertaintyScore": base_uncertainty,
                    "needsReview": needs_review,
                },
                "classMapPath": str(self._model_assets.class_map_path),
                "preprocessPath": str(self._model_assets.preprocess_path),
                "manifestPath": str(self._model_assets.grading_manifest.manifest_path),
                "postprocessPath": str(self._model_assets.postprocess_path),
            }
        )
        return GradingResult(
            grading_mode="real",
            grading_impl_type=str(result.get("implType") or ImplType.HEURISTIC.value),
            grading_label=grading_label,
            confidence_score=confidence_score,
            uncertainty_score=base_uncertainty,
            needs_review=needs_review,
            raw_result=raw,
        )

    def _needs_review(self, uncertainty_score: float) -> bool:
        return uncertainty_score >= self._review_threshold

    @staticmethod
    def _score(value: Any, default: float) -> float:
        try:
            score = float(value)
        except (TypeError, ValueError):
            score = default
        return round(max(0.0, min(1.0, score)), 4)

    def _normalize_grading_label(self, value: Any) -> str:
        try:
            return self._model_assets.normalize_grading_label(value)
        except ValueError as exc:
            raise BusinessException("M5024", str(exc)) from exc

    def _normalize_severity_code(self, value: Any, default: str) -> str:
        if self._model_assets.is_valid_grading_label(value):
            return self._model_assets.normalize_grading_label(value)
        return default

    @staticmethod
    def _safe_float(value: Any, default: float) -> float:
        try:
            return float(value)
        except (TypeError, ValueError):
            return float(default)

    def _build_lesion_grades(
        self,
        segmentation_regions: list[dict[str, Any]],
        candidates: Any,
        *,
        default_severity: str,
        default_confidence: float,
    ) -> list[dict[str, Any]]:
        if not segmentation_regions:
            return []
        by_region: dict[int, dict[str, Any]] = {}
        if isinstance(candidates, list):
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
            boundary_distance = self._safe_float(
                candidate.get("boundaryDistance") if isinstance(candidate, dict) else None,
                max(0.0, min(1.0, 1.0 - default_confidence)),
            )
            severity_code = self._normalize_severity_code(
                candidate.get("severityLabel") if isinstance(candidate, dict) else None,
                default_severity,
            )
            severity_score = self._safe_float(
                candidate.get("severityScore") if isinstance(candidate, dict) else None,
                self._safe_float(region.get("score"), 0.0),
            )
            confidence_score = round(max(0.0, min(1.0, 1.0 - boundary_distance)), 4)
            if candidate is None:
                confidence_score = round(max(0.0, min(1.0, default_confidence)), 4)
            lesion_grades.append(
                {
                    "regionIndex": index,
                    "toothCode": str(
                        region.get("toothCode")
                        or region.get("tooth_code")
                        or (candidate.get("toothCode") if isinstance(candidate, dict) else None)
                        or "UNKNOWN"
                    ),
                    "severityCode": severity_code,
                    "severityScore": severity_score,
                    "confidenceScore": confidence_score,
                    "boundaryDistance": boundary_distance,
                    "bbox": region.get("bbox")
                    if isinstance(region.get("bbox"), list)
                    else (candidate.get("bbox") if isinstance(candidate, dict) else None),
                    "score": (candidate.get("segmentationScore") if isinstance(candidate, dict) else None)
                    or region.get("score"),
                }
            )
        return lesion_grades

    def _tooth_aggregation(self, lesion_grades: list[dict[str, Any]]) -> list[dict[str, Any]]:
        grouped: dict[str, dict[str, Any]] = {}
        for lesion in lesion_grades:
            tooth = str(lesion.get("toothCode") or "UNKNOWN")
            severity = str(lesion.get("severityCode") or self._model_assets.grading_labels()[0])
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
            if self._model_assets.severity_rank(severity) > self._model_assets.severity_rank(current["highestSeverity"]):
                current["highestSeverity"] = severity
            current["maxSeverityScore"] = max(float(current["maxSeverityScore"]), float(lesion.get("severityScore") or 0.0))
        return list(grouped.values())

    def _uncertainty_reasons(
        self,
        *,
        uncertainty_score: float,
        confidence_score: float,
        class_margin: float,
        quality_results: list[Any],
        tooth_detections: list[ToothDetection],
        lesion_grades: list[dict[str, Any]],
    ) -> list[str]:
        reasons: list[str] = []
        if uncertainty_score >= self._review_threshold:
            reasons.append("HIGH_UNCERTAINTY")
        if confidence_score < 0.6:
            reasons.append("LOW_CONFIDENCE")
        if class_margin < 0.06:
            reasons.append("BOUNDARY_CASE")
        if lesion_grades and len(lesion_grades) > 1:
            reasons.append("MULTI_LESION")
        if tooth_detections and min((item.detection_score for item in tooth_detections), default=1.0) < 0.5:
            reasons.append("LOW_DETECTION_CONFIDENCE")
        if any(getattr(item, "check_result_code", "PASS") != "PASS" for item in quality_results):
            reasons.append("QUALITY_ISSUE")
        if not reasons and uncertainty_score > 0.3:
            reasons.append("UNCERTAINTY_PRESENT")
        return list(dict.fromkeys(reasons))
