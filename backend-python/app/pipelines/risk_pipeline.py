from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.grading_pipeline import GradingResult
from app.schemas.callback import ExplanationFactor, RiskAssessment
from app.schemas.request import PatientProfile


@dataclass(frozen=True)
class RiskPipelineResult:
    risk_mode: str
    risk_impl_type: str
    assessment: RiskAssessment
    raw_result: dict[str, Any]


class RiskPipeline:
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
        if not self._registry.is_module_real("risk"):
            raise BusinessException("M5009", "risk module is disabled")

        adapter = self._registry.get_risk_model()
        if adapter is None:
            raise BusinessException("M5010", "risk adapter is unavailable")

        try:
            result = adapter.infer(
                patient_profile=patient_profile,
                grading_result=grading_result,
                segmentation_regions=segmentation_regions or [],
                tooth_detection_count=tooth_detection_count,
            )
        except Exception as exc:
            raise BusinessException("M5011", f"risk fusion failed: {exc}") from exc
        return self._real_result(result)

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_risk_model()
        return adapter.impl_type.value if adapter is not None else "DISABLED"

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
        assessment = RiskAssessment(
            overall_risk_level_code=risk_level,
            risk_level_code=risk_level,
            risk_score=risk_score,
            recommended_cycle_days=int(result.get("recommendedCycleDays") or 180),
            explanation_factors=factors,
            risk_factors=result.get("riskFactors") or [],
            followup_suggestion=result.get("followupSuggestion"),
            review_suggested=result.get("reviewSuggested"),
            explanation=result.get("explanation"),
            fusion_version=result.get("fusionVersion"),
            assessment_report_json=raw,
            model_version=self._settings.model_version,
        )
        return RiskPipelineResult(
            risk_mode="real",
            risk_impl_type=str(result.get("implType") or "HEURISTIC"),
            assessment=assessment,
            raw_result=raw,
        )
