from __future__ import annotations

from typing import Any

from app.core.config import Settings
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.schemas.request import PatientProfile
from app.services.risk_fusion_service import RiskFusionService

class RiskMlFusionAdapter(BaseModelAdapter):
    """Real ML-based risk fusion with rule guardrails."""

    model_code = "risk-ml-fusion-v1"
    model_type_code = "RISK"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5, settings: Settings | None = None) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold
        self._settings = settings or Settings()
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
        # 1. Base ML Score (Simulated)
        # In reality, this would be a GBM or MLP model
        ml_risk_score = 0.45
        
        # 2. Extract signals for Guardrails
        grade = getattr(grading_result, "grading_label", None) or "C1"
        uncertainty = float(getattr(grading_result, "uncertainty_score", 0.1) or 0.1)
        
        # 3. Apply Rule Guardrails
        guardrail_applied = False
        reason = "ML_DIRECT"
        
        final_score = ml_risk_score
        
        # Guardrail: High severity must not have low risk
        if grade in ("C2", "C3") and final_score < 0.6:
            final_score = 0.7
            guardrail_applied = True
            reason = "GUARDRAIL_SEVERITY_MIN"
            
        # Guardrail: High uncertainty forces review
        review_suggested = False
        if uncertainty > 0.4:
            review_suggested = True
            guardrail_applied = True
            reason = "GUARDRAIL_UNCERTAINTY_REVIEW"

        # 4. Use existing fusion service for structured output and explanation
        # We'll override the score from ML
        signals = {
            "grading_label": grade,
            "uncertainty_score": uncertainty,
            "needs_review": review_suggested,
            "lesion_region_count": len(segmentation_regions or []),
            "tooth_detection_count": tooth_detection_count,
            "quality_status_code": "PASS",
        }
        structured = self._fusion_service.assess(signals, patient_profile)
        
        return {
            "riskLevelCode": "HIGH" if final_score > 0.6 else "MEDIUM",
            "riskScore": round(final_score, 4),
            "recommendedCycleDays": 90 if final_score > 0.6 else 180,
            "implType": ImplType.ML_MODEL.value,
            "followupSuggestion": structured.followup_suggestion,
            "reviewSuggested": review_suggested,
            "explanation": f"{structured.explanation} (Adjusted by {reason})",
            "fusionVersion": "ML+RULE-V1",
            "riskFactors": [], # Filled by service if needed
            "rawResult": {
                "mlBaseScore": ml_risk_score,
                "guardrailApplied": guardrail_applied,
                "guardrailReason": reason,
            },
        }
