from typing import Any

from app.core.config import Settings
from app.schemas.base import dump_camel
from app.schemas.callback import ExplanationFactor, RiskAssessment
from app.schemas.request import ImageSummary, PatientProfile
from app.schemas.risk_assessment import StructuredRiskAssessment
from app.services.risk_fusion_service import RiskFusionService

RISK_LEVEL_LABELS: dict[str, str] = {
    "HIGH": "高风险",
    "MEDIUM": "中风险",
    "LOW": "低风险",
    "VERY_HIGH": "极高风险",
    "MINIMAL": "极低风险",
}

RISK_FACTOR_LABELS: dict[str, str] = {
    "SEVERITY_FACTOR": "龋齿严重程度",
    "UNCERTAINTY_FACTOR": "模型不确定性",
    "MULTI_LESION_FACTOR": "多发病灶",
    "TOOTH_COUNT_FACTOR": "异常牙齿数量",
    "AGE_FACTOR": "年龄风险因子",
    "DIET_FACTOR": "饮食风险因子",
    "HYGIENE_FACTOR": "口腔卫生因子",
    "FLUORIDE_FACTOR": "氟化物使用因子",
    "HISTORY_FACTOR": "既往龋齿史",
    "QUALITY_FACTOR": "影像质量因子",
    "REVIEW_NEEDED_FACTOR": "需要复核",
}

FOLLOWUP_SUGGESTION_LABELS: dict[str, str] = {
    "3_MONTH_RECHECK": "建议3个月后复查",
    "6_MONTH_RECHECK": "建议6个月后复查",
    "IMMEDIATE_TREATMENT": "建议立即治疗",
    "12_MONTH_RECHECK": "建议12个月后复查",
    "ANNUAL_RECHECK": "建议每年复查",
}


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

        risk_level = payload.get("riskLevelCode") or payload.get("overallRiskLevelCode") or ""
        payload["riskLevelLabel"] = RISK_LEVEL_LABELS.get(risk_level, risk_level)

        followup = payload.get("followupSuggestion") or ""
        payload["followupSuggestionLabel"] = FOLLOWUP_SUGGESTION_LABELS.get(followup, followup)

        risk_factors = payload.get("riskFactors") or []
        for rf in risk_factors:
            if isinstance(rf, dict):
                code = rf.get("code") or ""
                rf["label"] = RISK_FACTOR_LABELS.get(code, code)

        explanation = payload.get("explanation") or ""
        if explanation:
            payload["explanationNote"] = "此风险解释由 AI 风险融合引擎自动生成，仅供临床参考，不构成诊断建议。"

        payload["rawResultJsonCollapsed"] = True

        return payload

    @staticmethod
    def _cycle_days(followup_suggestion: str) -> int:
        if followup_suggestion == "3_MONTH_RECHECK":
            return 90
        if followup_suggestion == "6_MONTH_RECHECK":
            return 180
        return 365
