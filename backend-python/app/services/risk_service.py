from typing import Any

from app.core.config import Settings
from app.schemas.callback import ExplanationFactor, RiskAssessment
from app.schemas.request import PatientProfile


class RiskService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def assess(
        self,
        patient_profile: PatientProfile | None = None,
        *,
        grading_label: str = "C1",
        uncertainty_score: float = 0.1,
        needs_review: bool = False,
        segmentation_regions: list[dict[str, Any]] | None = None,
    ) -> RiskAssessment:
        previous_caries = patient_profile.previous_caries_count if patient_profile else None
        risk_level = "MEDIUM" if previous_caries and previous_caries > 0 else "LOW"
        if grading_label in {"C2", "C3"} or uncertainty_score >= self.settings.uncertainty_review_threshold:
            risk_level = "MEDIUM" if risk_level == "LOW" else risk_level
        if grading_label == "C3" and previous_caries and previous_caries > 0:
            risk_level = "HIGH"
        risk_score = 62 if risk_level == "MEDIUM" else 25
        if risk_level == "HIGH":
            risk_score = 78
        return RiskAssessment(
            overall_risk_level_code=risk_level,
            assessment_report_json={
                "source": "mock",
                "riskLevelCode": risk_level,
                "riskScore": risk_score,
                "gradingLabel": grading_label,
                "uncertaintyScore": uncertainty_score,
                "needsReview": needs_review,
                "lesionRegionCount": len(segmentation_regions or []),
            },
            recommended_cycle_days=90 if risk_level == "HIGH" or needs_review else 180,
            risk_level_code=risk_level,
            risk_score=risk_score,
            explanation_factors=[
                ExplanationFactor(feature_code="previous_caries_count", contribution=0.31, direction="POSITIVE"),
                ExplanationFactor(feature_code="grading_label", contribution=0.27, direction="POSITIVE"),
                ExplanationFactor(feature_code="uncertainty_score", contribution=0.14, direction="POSITIVE"),
            ],
            model_version=self.settings.model_version,
        )
