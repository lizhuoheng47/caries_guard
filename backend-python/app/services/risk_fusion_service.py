from __future__ import annotations

from typing import Any

from app.core.config import Settings
from app.schemas.base import dump_camel
from app.schemas.request import ImageSummary, PatientProfile
from app.schemas.risk_assessment import StructuredRiskAssessment
from app.services.risk_explanation_service import RiskExplanationService
from app.services.risk_rule_engine import RiskRuleEngine


class RiskFusionService:
    fusion_version = "risk-fusion-v1"

    def __init__(
        self,
        settings: Settings,
        rule_engine: RiskRuleEngine | None = None,
        explanation_service: RiskExplanationService | None = None,
    ) -> None:
        self.settings = settings
        self.rule_engine = rule_engine or RiskRuleEngine(settings)
        self.explanation_service = explanation_service or RiskExplanationService()

    def assess(self, signals: dict[str, Any], patient_profile: PatientProfile | None) -> StructuredRiskAssessment:
        normalized = self.normalize_signals(signals)
        factors = self.rule_engine.factors(normalized, patient_profile)
        score = round(max(0.0, min(1.0, sum(item.weight for item in factors))), 4)
        risk_level = self.rule_engine.level(score)
        review_suggested = self.rule_engine.review_suggested(risk_level, normalized, factors)
        return StructuredRiskAssessment(
            risk_level=risk_level,
            risk_score=score,
            risk_factors=factors,
            followup_suggestion=self.rule_engine.followup_suggestion(risk_level),
            review_suggested=review_suggested,
            explanation=self.explanation_service.explain(risk_level, factors, review_suggested),
            fusion_version=self.fusion_version,
            evidence_quality=self.rule_engine.evidence_quality(factors),
            raw_signals=normalized,
        )

    def to_legacy_callback_report(self, assessment: StructuredRiskAssessment) -> dict[str, Any]:
        return {
            "riskAssessment": dump_camel(assessment),
            "riskLevelCode": assessment.risk_level,
            "riskScore": assessment.risk_score,
            "gradingLabel": assessment.raw_signals.get("grading_label"),
            "uncertaintyScore": assessment.raw_signals.get("uncertainty_score"),
            "needsReview": assessment.review_suggested,
            "lesionRegionCount": assessment.raw_signals.get("lesion_region_count"),
            "suspiciousToothCount": assessment.raw_signals.get("suspicious_tooth_count"),
            "followupSuggestion": assessment.followup_suggestion,
            "reviewSuggested": assessment.review_suggested,
            "fusionVersion": assessment.fusion_version,
            "evidenceQuality": assessment.evidence_quality,
        }

    @staticmethod
    def from_image_summary(image_summary: ImageSummary | None) -> dict[str, Any]:
        if image_summary is None:
            return {}
        return {
            "grading_label": image_summary.overall_highest_severity or "C1",
            "suspicious_tooth_count": image_summary.suspicious_tooth_count or 0,
            "uncertainty_score": image_summary.overall_uncertainty_score or 0.1,
            "lesion_area_ratio": image_summary.lesion_area_ratio or 0.0,
            "quality_status_code": image_summary.quality_status_code,
        }

    @staticmethod
    def normalize_signals(signals: dict[str, Any]) -> dict[str, Any]:
        return {
            "grading_label": signals.get("grading_label") or signals.get("gradingLabel") or "C1",
            "uncertainty_score": signals.get("uncertainty_score", signals.get("uncertaintyScore", 0.1)),
            "needs_review": signals.get("needs_review", signals.get("needsReview", False)),
            "lesion_region_count": signals.get("lesion_region_count", signals.get("lesionRegionCount", 0)),
            "suspicious_tooth_count": signals.get("suspicious_tooth_count", signals.get("suspiciousToothCount", 0)),
            "tooth_detection_count": signals.get("tooth_detection_count", signals.get("toothDetectionCount", 0)),
            "quality_status_code": signals.get("quality_status_code", signals.get("qualityStatusCode", "PASS")),
        }
