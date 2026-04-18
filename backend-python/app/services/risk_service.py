from typing import Any

from app.core.config import Settings
from app.schemas.base import dump_camel
from app.schemas.callback import ExplanationFactor, RiskAssessment
from app.schemas.request import ImageSummary, PatientProfile
from app.schemas.risk_assessment import StructuredRiskAssessment
from app.services.risk_fusion_service import RiskFusionService


class RiskService:
    def __init__(self, settings: Settings, fusion_service: RiskFusionService | None = None) -> None:
        self.settings = settings
        self.fusion_service = fusion_service or RiskFusionService(settings)

    def assess(
        self,
        patient_profile: PatientProfile | None = None,
        *,
        image_summary: ImageSummary | None = None,
        grading_label: str = "C1",
        uncertainty_score: float = 0.1,
        needs_review: bool = False,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detection_count: int = 0,
        quality_status_code: str | None = None,
    ) -> RiskAssessment:
        signals = self.fusion_service.from_image_summary(image_summary)
        signals.update(
            {
                "grading_label": signals.get("grading_label") or grading_label,
                "uncertainty_score": signals.get("uncertainty_score", uncertainty_score),
                "needs_review": needs_review,
                "lesion_region_count": len(segmentation_regions or []),
                "tooth_detection_count": tooth_detection_count,
                "quality_status_code": quality_status_code or signals.get("quality_status_code") or "PASS",
            }
        )
        structured = self.fusion_service.assess(signals, patient_profile)
        return self.to_callback_assessment(structured)

    def to_callback_assessment(self, structured: StructuredRiskAssessment) -> RiskAssessment:
        cycle_days = self._cycle_days(structured.followup_suggestion)
        report = self.fusion_service.to_legacy_callback_report(structured)
        return RiskAssessment(
            overall_risk_level_code=structured.risk_level,
            assessment_report_json=report,
            recommended_cycle_days=cycle_days,
            risk_level_code=structured.risk_level,
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
            model_version=self.settings.model_version,
        )

    @staticmethod
    def as_api_payload(assessment: RiskAssessment) -> dict[str, Any]:
        payload = dump_camel(assessment)
        report = payload.get("assessmentReportJson") or {}
        structured = report.get("riskAssessment") or {}
        payload.update(structured)
        return payload

    @staticmethod
    def _cycle_days(followup_suggestion: str) -> int:
        if followup_suggestion == "3_MONTH_RECHECK":
            return 90
        if followup_suggestion == "6_MONTH_RECHECK":
            return 180
        return 365
