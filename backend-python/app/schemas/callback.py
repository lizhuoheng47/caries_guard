from typing import Any

from app.schemas.base import CamelModel


class Summary(CamelModel):
    overall_highest_severity: str = "C1"
    uncertainty_score: float = 0.1
    review_suggested_flag: str = "0"
    teeth_count: int | None = None


class ContractSummary(CamelModel):
    overall_highest_severity: str = "C1"
    suspicious_tooth_count: int = 1
    overall_uncertainty_score: float = 0.1
    lesion_area_ratio: float = 0.0
    review_recommended_flag: str = "0"
    high_risk_flag: str = "0"


class QualityCheckResult(CamelModel):
    image_id: int | None = None
    check_result_code: str = "PASS"
    quality_score: int = 90
    blur_score: int | None = None
    exposure_score: int | None = None
    integrity_score: int | None = None
    occlusion_score: int | None = None
    issue_codes: list[str] = []
    suggestion_text: str = "quality passed"


class ToothDetection(CamelModel):
    image_id: int | None = None
    tooth_code: str = "16"
    bbox: list[int] = [64, 64, 180, 180]
    detection_score: float = 0.95


class AssetRef(CamelModel):
    bucket_name: str
    object_key: str
    width_px: int | None = None
    height_px: int | None = None


class VisualAsset(CamelModel):
    asset_type_code: str
    attachment_id: int | None = None
    bucket_name: str
    object_key: str
    content_type: str = "image/png"
    related_image_id: int | None = None
    tooth_code: str | None = None
    file_size_bytes: int | None = None
    md5: str | None = None
    file_name: str | None = None


class LesionResult(CamelModel):
    image_id: int | None = None
    tooth_code: str = "16"
    severity_code: str = "C1"
    uncertainty_score: float = 0.1
    lesion_area_px: int | None = None
    lesion_area_ratio: float | None = None
    mask_asset: AssetRef | None = None
    overlay_asset: AssetRef | None = None


class ExplanationFactor(CamelModel):
    feature_code: str
    contribution: float
    direction: str


class RiskAssessment(CamelModel):
    overall_risk_level_code: str = "LOW"
    assessment_report_json: dict[str, Any] = {}
    recommended_cycle_days: int = 180
    risk_level_code: str | None = None
    risk_score: int | None = None
    explanation_factors: list[ExplanationFactor] | None = None
    model_version: str | None = None


class AnalysisCallbackPayload(CamelModel):
    task_no: str
    task_status_code: str
    started_at: str | None = None
    completed_at: str | None = None
    model_version: str | None = None
    summary: Summary | None = None
    raw_result_json: dict[str, Any] | None = None
    visual_assets: list[VisualAsset] = []
    risk_assessment: RiskAssessment | None = None
    error_code: str | None = None
    error_message: str | None = None
    trace_id: str | None = None
    inference_millis: int | None = None
    uncertainty_score: float | None = None


class FailureCallbackPayload(CamelModel):
    task_no: str
    task_status_code: str = "FAILED"
    started_at: str | None = None
    completed_at: str | None = None
    model_version: str | None = None
    summary: None = None
    raw_result_json: dict[str, Any] | None = None
    visual_assets: list[VisualAsset] = []
    risk_assessment: None = None
    error_code: str | None = None
    error_message: str
    trace_id: str | None = None
    inference_millis: int = 0
    uncertainty_score: None = None
