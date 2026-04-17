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
        risk_score = int(result.get("riskScore") or 0)
        raw = dict(result.get("rawResult") or {})
        raw["source"] = "risk_pipeline"
        assessment = RiskAssessment(
            overall_risk_level_code=risk_level,
            risk_level_code=risk_level,
            risk_score=risk_score,
            recommended_cycle_days=int(result.get("recommendedCycleDays") or 180),
            explanation_factors=factors,
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
        risk_level = "MEDIUM" if previous_caries and previous_caries > 0 else "LOW"
        if grade in {"C2", "C3"} or uncertainty >= self._settings.uncertainty_review_threshold:
            risk_level = "MEDIUM" if risk_level == "LOW" else risk_level
        risk_score = 62 if risk_level == "MEDIUM" else 25
        raw = {
            "source": "mock",
            "riskLevelCode": risk_level,
            "riskScore": risk_score,
            "gradingLabel": grade,
            "uncertaintyScore": uncertainty,
            "lesionRegionCount": len(segmentation_regions or []),
        }
        if fallback_reason:
            raw["fallbackReason"] = fallback_reason
        assessment = RiskAssessment(
            overall_risk_level_code=risk_level,
            assessment_report_json=raw,
            recommended_cycle_days=180,
            risk_level_code=risk_level,
            risk_score=risk_score,
            explanation_factors=[
                ExplanationFactor(feature_code="previous_caries_count", contribution=0.31, direction="POSITIVE"),
                ExplanationFactor(feature_code="grading_label", contribution=0.27, direction="POSITIVE"),
            ],
            model_version=self._settings.model_version,
        )
        return RiskPipelineResult(
            risk_mode="mock",
            risk_impl_type=ImplType.MOCK.value,
            assessment=assessment,
            raw_result=raw,
        )
