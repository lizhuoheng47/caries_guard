"""Risk fusion sub-pipeline with mock / heuristic routing."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.grading_pipeline import GradingResult
from app.schemas.callback import ExplanationFactor, RiskAssessment
from app.schemas.request import PatientProfile
from app.schemas.base import dump_camel
from app.services.risk_fusion_service import RiskFusionService

log = get_logger("cariesguard-ai.pipeline.risk")


@dataclass(frozen=True)
class RiskPipelineResult:
    risk_mode: str
    risk_impl_type: str
    assessment: RiskAssessment
    raw_result: dict[str, Any]


class RiskPipeline:
    """Risk pipeline for Phase 5D."""

    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings
        self._fusion_service = RiskFusionService(settings)

    def assess(
        self,
        patient_profile: PatientProfile | None,
        grading_result: GradingResult | None = None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detection_count: int = 0,
    ) -> RiskPipelineResult:
        mode = self._registry.get_runtime_mode()
        if not self._registry.is_module_real("risk"):
            return self._mock_result(patient_profile, grading_result, segmentation_regions)

        adapter = self._registry.get_risk_model()
        if adapter is None:
            if mode == "real":
                raise BusinessException("M5009", "risk adapter not available in real mode")
            log.warning("risk adapter unavailable - fallback to mock (hybrid)")
            return self._mock_result(patient_profile, grading_result, segmentation_regions, "adapter_unavailable")

        try:
            result = adapter.infer(
                patient_profile=patient_profile,
                grading_result=grading_result,
                segmentation_regions=segmentation_regions or [],
                tooth_detection_count=tooth_detection_count,
            )
            return self._real_result(result)
        except Exception as exc:
            if mode == "real":
                raise BusinessException("M5010", f"risk fusion failed: {exc}") from exc
            log.warning("risk fusion failed - fallback to mock (hybrid) error=%s", exc)
            return self._mock_result(patient_profile, grading_result, segmentation_regions, str(exc))

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_risk_model()
        if adapter is not None and self._registry.is_module_real("risk"):
            return adapter.impl_type.value
        return ImplType.MOCK.value

    def _real_result(self, result: dict[str, Any]) -> RiskPipelineResult:
        factors = [
            ExplanationFactor(
                feature_code=item.get("featureCode", "unknown"),
                contribution=float(item.get("contribution", 0.0)),
                direction=item.get("direction", "POSITIVE"),
            )
            for item in result.get("explanationFactors", [])
        ]
        risk_level = str(result.get("riskLevelCode") or "LOW")
        risk_score = float(result.get("riskScore") or 0.0)
        raw = dict(result.get("rawResult") or {})
        raw["source"] = "risk_pipeline"
        risk_factors = result.get("riskFactors") or []
        assessment = RiskAssessment(
            overall_risk_level_code=risk_level,
            risk_level_code=risk_level,
            risk_score=risk_score,
            recommended_cycle_days=int(result.get("recommendedCycleDays") or 180),
            explanation_factors=factors,
            risk_factors=risk_factors,
            followup_suggestion=result.get("followupSuggestion"),
            review_suggested=result.get("reviewSuggested"),
            explanation=result.get("explanation"),
            fusion_version=result.get("fusionVersion"),
            assessment_report_json=raw,
            model_version=self._settings.model_version,
        )
        return RiskPipelineResult(
            risk_mode="real",
            risk_impl_type=str(result.get("implType") or ImplType.HEURISTIC.value),
            assessment=assessment,
            raw_result=raw,
        )

    def _mock_result(
        self,
        patient_profile: PatientProfile | None,
        grading_result: GradingResult | None = None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        fallback_reason: str | None = None,
    ) -> RiskPipelineResult:
        previous_caries = patient_profile.previous_caries_count if patient_profile else None
        grade = grading_result.grading_label if grading_result else "C1"
        uncertainty = grading_result.uncertainty_score if grading_result else 0.1
        structured = self._fusion_service.assess(
            {
                "grading_label": grade,
                "uncertainty_score": uncertainty,
                "needs_review": grading_result.needs_review if grading_result else False,
                "lesion_region_count": len(segmentation_regions or []),
                "quality_status_code": "PASS",
            },
            patient_profile,
        )
        risk_level = structured.risk_level
        raw = {
            "source": "mock",
            "riskLevelCode": risk_level,
            "riskScore": structured.risk_score,
            "gradingLabel": grade,
            "uncertaintyScore": uncertainty,
            "lesionRegionCount": len(segmentation_regions or []),
            "riskAssessment": dump_camel(structured),
            "followupSuggestion": structured.followup_suggestion,
            "reviewSuggested": structured.review_suggested,
            "fusionVersion": structured.fusion_version,
        }
        if fallback_reason:
            raw["fallbackReason"] = fallback_reason
        assessment = RiskAssessment(
            overall_risk_level_code=risk_level,
            assessment_report_json=raw,
            recommended_cycle_days=self._cycle_days(structured.followup_suggestion),
            risk_level_code=risk_level,
            risk_score=structured.risk_score,
            explanation_factors=[
                ExplanationFactor(
                    feature_code=item.code,
                    contribution=item.weight,
                    direction="POSITIVE" if item.weight >= 0 else "NEGATIVE",
                )
                for item in structured.risk_factors
            ],
            risk_factors=structured.risk_factors,
            followup_suggestion=structured.followup_suggestion,
            review_suggested=structured.review_suggested,
            explanation=structured.explanation,
            fusion_version=structured.fusion_version,
            model_version=self._settings.model_version,
        )
        return RiskPipelineResult(
            risk_mode="mock",
            risk_impl_type=ImplType.MOCK.value,
            assessment=assessment,
            raw_result=raw,
        )

    @staticmethod
    def _cycle_days(followup_suggestion: str) -> int:
        if followup_suggestion == "3_MONTH_RECHECK":
            return 90
        if followup_suggestion == "6_MONTH_RECHECK":
            return 180
        return 365
