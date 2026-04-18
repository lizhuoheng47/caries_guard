from typing import Any

from app.schemas.base import CamelModel


class PatientProfile(CamelModel):
    age: int | None = None
    gender_code: str | None = None
    brushing_frequency_code: str | None = None
    sugar_diet_level_code: str | None = None
    fluoride_use_flag: str | None = None
    previous_caries_count: int | None = None
    last_dental_check_months: int | None = None


class ImageInput(CamelModel):
    image_id: int | None = None
    attachment_id: int | None = None
    image_type_code: str | None = None
    bucket_name: str | None = None
    object_key: str | None = None
    storage_provider_code: str | None = None
    attachment_md5: str | None = None
    access_url: str | None = None
    access_expire_at: int | None = None
    local_storage_path: str | None = None
    original_filename: str | None = None
    content_type: str | None = None
    width_px: int | None = None
    height_px: int | None = None


class AnalyzeRequest(CamelModel):
    trace_id: str | None = None
    task_no: str
    task_type_code: str | None = None
    case_id: int | None = None
    case_no: str | None = None
    patient_id: int | None = None
    org_id: int | None = None
    image_ids: list[int] | None = None
    images: list[ImageInput] = []
    patient_profile: PatientProfile | None = None
    model_version: str | None = None
    callback_url: str | None = None
    callback_token: str | None = None
    requested_at: str | None = None
    raw_payload: dict[str, Any] | None = None


class QualityCheckRequest(CamelModel):
    trace_id: str | None = None
    task_no: str | None = None
    case_id: int | None = None
    image_id: int | None = None
    patient_id: int | None = None
    org_id: int | None = None
    image_type_code: str | None = None
    bucket_name: str | None = None
    object_key: str | None = None
    access_url: str | None = None


class ImageSummary(CamelModel):
    overall_highest_severity: str | None = None
    suspicious_tooth_count: int | None = None
    overall_uncertainty_score: float | None = None
    lesion_area_ratio: float | None = None
    quality_status_code: str | None = None


class RiskAssessmentRequest(CamelModel):
    trace_id: str | None = None
    case_id: int | None = None
    patient_id: int | None = None
    image_summary: ImageSummary | None = None
    patient_profile: PatientProfile | None = None
    model_version: str | None = None
