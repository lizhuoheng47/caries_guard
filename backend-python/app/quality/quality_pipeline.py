from __future__ import annotations

from typing import Any

from app.schemas.callback import QualityCheckResult
from app.schemas.request import ImageInput


def to_quality_check_result(image: ImageInput, result: dict[str, Any]) -> QualityCheckResult:
    status = str(
        result.get("qualityStatusCode")
        or result.get("qualityStatus")
        or "FAIL"
    ).upper()
    quality_score = _score(result.get("qualityScore"), 0.0)
    quality_issues = _issues(result.get("qualityIssues") or result.get("issues"))
    retake_suggested = bool(result.get("retakeSuggested")) or status == "FAIL"
    blur_score = _score(result.get("blurScore"), 0.0)
    exposure_score = _score(result.get("exposureScore"), 0.0)
    integrity_score = _score(result.get("integrityScore"), 0.0)
    occlusion_score = _score(result.get("occlusionScore"), 0.0)
    impl_type = str(result.get("implType") or "UNKNOWN")
    model_version = str(result.get("modelVersion") or "") or None
    inference_millis = _int_or_none(result.get("inferenceMillis"))
    suggestion = _suggestion_text(status, quality_issues, retake_suggested)

    return QualityCheckResult(
        image_id=image.image_id if image else None,
        check_result_code=status,
        quality_score=int(round(quality_score * 100)),
        quality_score_float=quality_score,
        quality_status=status,
        blur_score=int(round(blur_score * 100)),
        exposure_score=int(round(exposure_score * 100)),
        integrity_score=int(round(integrity_score * 100)),
        occlusion_score=int(round(occlusion_score * 100)),
        issue_codes=quality_issues,
        quality_issues=quality_issues,
        retake_suggested=retake_suggested,
        impl_type=impl_type,
        model_version=model_version,
        inference_millis=inference_millis,
        raw_result=result.get("rawResult") if isinstance(result.get("rawResult"), dict) else {},
        suggestion_text=suggestion,
    )


def _score(value: Any, default: float) -> float:
    try:
        score = float(value)
    except (TypeError, ValueError):
        score = default
    return round(max(0.0, min(1.0, score)), 4)


def _issues(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    normalized: list[str] = []
    for item in value:
        text = str(item or "").strip().lower()
        if text:
            normalized.append(text)
    return list(dict.fromkeys(normalized))


def _int_or_none(value: Any) -> int | None:
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def _suggestion_text(status: str, issues: list[str], retake_suggested: bool) -> str:
    if status == "PASS":
        return "quality passed"
    if status == "WARN":
        base = f"quality warning: {', '.join(issues)}" if issues else "quality warning"
        return base + ("; consider retake" if retake_suggested else "")
    base = f"quality failed: {', '.join(issues)}" if issues else "quality failed"
    return base + "; retake required"
