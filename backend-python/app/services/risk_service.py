from app.core.config import Settings
from app.schemas.callback import ExplanationFactor, RiskAssessment
from app.schemas.request import PatientProfile


class RiskService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def assess(self, patient_profile: PatientProfile | None = None) -> RiskAssessment:
        previous_caries = patient_profile.previous_caries_count if patient_profile else None
        risk_level = "MEDIUM" if previous_caries and previous_caries > 0 else "LOW"
        risk_score = 62 if risk_level == "MEDIUM" else 25
        return RiskAssessment(
            overall_risk_level_code=risk_level,
            assessment_report_json={
                "source": "mock",
                "riskLevelCode": risk_level,
                "riskScore": risk_score,
            },
            recommended_cycle_days=180,
            risk_level_code=risk_level,
            risk_score=risk_score,
            explanation_factors=[
                ExplanationFactor(feature_code="previous_caries_count", contribution=0.31, direction="POSITIVE"),
                ExplanationFactor(feature_code="overall_highest_severity", contribution=0.27, direction="POSITIVE"),
            ],
            model_version=self.settings.model_version,
        )

