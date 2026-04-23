"""Risk fusion adapter backed by deterministic rules."""

from __future__ import annotations

from typing import Any

from app.core.config import Settings
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.schemas.base import dump_camel
from app.schemas.request import PatientProfile
from app.services.risk_fusion_service import RiskFusionService


class RiskHeuristicFusionAdapter(BaseModelAdapter):
    """Heuristic risk-fusion adapter."""

    model_code = "risk-fusion-heuristic-v1"
    model_type_code = "RISK"
    impl_type = ImplType.HEURISTIC

    def __init__(self, confidence_threshold: float = 0.5, settings: Settings | None = None) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold
        self._settings = settings or Settings(model_confidence_threshold=confidence_threshold)
        self._fusion_service = RiskFusionService(self._settings)

    def load(self) -> None:
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(
        self,
        *,
        patient_profile: PatientProfile | None,
        grading_result: Any | None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detection_count: int = 0,
    ) -> dict[str, Any]:
        grade = getattr(grading_result, "grading_label", None) or "C1"
        uncertainty = float(getattr(grading_result, "uncertainty_score", 0.1) or 0.1)
        needs_review = bool(getattr(grading_result, "needs_review", False))
        regions = segmentation_regions or []
        signals = {
            "grading_label": grade,
            "uncertainty_score": uncertainty,
            "needs_review": needs_review,
            "lesion_region_count": len(regions),
            "tooth_detection_count": tooth_detection_count,
            "quality_status_code": "PASS",
        }
        structured = self._fusion_service.assess(signals, patient_profile)
        raw_result = self._fusion_service.to_legacy_callback_report(structured)
        raw_result.update(
            {
                "modelCode": self.model_code,
                "implType": ImplType.HEURISTIC.value,
                "confidenceThreshold": self._confidence_threshold,
                "riskFactors": [dump_camel(item) for item in structured.risk_factors],
            }
        )
        return {
            "riskLevelCode": structured.risk_level,
            "riskScore": structured.risk_score,
            "recommendedCycleDays": self._cycle_days(structured.followup_suggestion),
            "implType": ImplType.HEURISTIC.value,
            "followupSuggestion": structured.followup_suggestion,
            "reviewSuggested": structured.review_suggested,
            "explanation": structured.explanation,
            "fusionVersion": structured.fusion_version,
            "riskFactors": [dump_camel(item) for item in structured.risk_factors],
            "explanationFactors": [
                {
                    "featureCode": item.code,
                    "contribution": item.weight,
                    "direction": "POSITIVE" if item.weight >= 0 else "NEGATIVE",
                }
                for item in structured.risk_factors
            ],
            "rawResult": raw_result,
        }

    @staticmethod
    def _cycle_days(followup_suggestion: str) -> int:
        if followup_suggestion == "3_MONTH_RECHECK":
            return 90
        if followup_suggestion == "6_MONTH_RECHECK":
            return 180
        return 365
