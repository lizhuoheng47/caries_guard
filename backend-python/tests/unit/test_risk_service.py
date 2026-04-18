from app.core.config import Settings
from app.schemas.request import ImageSummary, PatientProfile
from app.services.risk_service import RiskService


def _service() -> RiskService:
    return RiskService(Settings(uncertainty_review_threshold=0.35))


def test_low_risk_profile_returns_structured_assessment() -> None:
    result = _service().assess(
        PatientProfile(
            brushing_frequency_code="TWICE_DAILY",
            sugar_diet_level_code="LOW",
            fluoride_use_flag="YES",
            previous_caries_count=0,
        ),
        image_summary=ImageSummary(
            overall_highest_severity="C1",
            suspicious_tooth_count=1,
            overall_uncertainty_score=0.12,
            quality_status_code="PASS",
        ),
    )

    assert result.overall_risk_level_code == "LOW"
    assert result.followup_suggestion == "12_MONTH_RECHECK"
    assert result.review_suggested is False
    assert result.fusion_version == "risk-fusion-v1"
    assert result.risk_factors


def test_high_risk_profile_suggests_review_and_short_followup() -> None:
    result = _service().assess(
        PatientProfile(
            brushing_frequency_code="LOW",
            sugar_diet_level_code="HIGH",
            previous_caries_count=4,
            last_dental_check_months=18,
        ),
        image_summary=ImageSummary(
            overall_highest_severity="C3",
            suspicious_tooth_count=3,
            overall_uncertainty_score=0.45,
            quality_status_code="PASS",
        ),
    )

    assert result.overall_risk_level_code == "HIGH"
    assert result.risk_score and result.risk_score >= 0.7
    assert result.followup_suggestion == "3_MONTH_RECHECK"
    assert result.recommended_cycle_days == 90
    assert result.review_suggested is True
    assert any(item.code == "HIGH_UNCERTAINTY" for item in result.risk_factors or [])
    assert result.assessment_report_json["riskAssessment"]["riskLevel"] == "HIGH"


def test_poor_quality_marks_evidence_insufficient_without_dropping_risk_trace() -> None:
    result = _service().assess(
        None,
        image_summary=ImageSummary(
            overall_highest_severity="C2",
            suspicious_tooth_count=1,
            overall_uncertainty_score=0.2,
            quality_status_code="FAIL",
        ),
    )

    assert result.review_suggested is True
    assert result.assessment_report_json["evidenceQuality"] == "INSUFFICIENT"
    assert any(item.code == "EVIDENCE_INSUFFICIENT" for item in result.risk_factors or [])


def test_api_payload_flattens_new_contract_fields() -> None:
    result = _service().assess(
        PatientProfile(previous_caries_count=1),
        image_summary=ImageSummary(overall_highest_severity="C2", overall_uncertainty_score=0.2),
    )
    payload = RiskService.as_api_payload(result)

    assert payload["riskLevel"] == result.overall_risk_level_code
    assert payload["fusionVersion"] == "risk-fusion-v1"
    assert payload["riskFactors"]
