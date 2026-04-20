from app.core.config import Settings
from app.pipelines.grading_pipeline import GradingResult
from app.pipelines.uncertainty_pipeline import UncertaintyPipeline
from app.schemas.callback import QualityCheckResult, ToothDetection


def test_composite_uncertainty_has_grading_prior_floor():
    settings = Settings(uncertainty_review_threshold=0.35)
    pipeline = UncertaintyPipeline(settings)
    grading = GradingResult(
        grading_mode="real",
        grading_impl_type="HEURISTIC",
        grading_label="C2",
        confidence_score=0.62,
        uncertainty_score=0.42,
        needs_review=True,
        raw_result={"classMargin": 0.05},
    )
    result = pipeline.assess(
        grading_result=grading,
        quality_results=[QualityCheckResult(image_id=1, check_result_code="PASS", quality_score=95)],
        tooth_detections=[ToothDetection(image_id=1, tooth_code="16", bbox=[10, 10, 80, 80], detection_score=0.92)],
        segmentation_regions=[{"bbox": [10, 10, 70, 70], "score": 0.91}],
        lesion_results=[{"bbox": [10, 10, 70, 70], "confidenceScore": 0.91, "lesionAreaRatio": 0.02}],
    )
    assert result.uncertainty_score >= 0.42
    assert result.needs_review is True


def test_uncertainty_threshold_controls_needs_review():
    settings = Settings(uncertainty_review_threshold=0.35)
    pipeline = UncertaintyPipeline(settings)
    grading = GradingResult(
        grading_mode="real",
        grading_impl_type="HEURISTIC",
        grading_label="C1",
        confidence_score=0.89,
        uncertainty_score=0.18,
        needs_review=False,
        raw_result={"classMargin": 0.11},
    )
    result = pipeline.assess(
        grading_result=grading,
        quality_results=[QualityCheckResult(image_id=1, check_result_code="PASS", quality_score=96)],
        tooth_detections=[ToothDetection(image_id=1, tooth_code="16", bbox=[20, 20, 90, 90], detection_score=0.95)],
        segmentation_regions=[{"bbox": [20, 20, 90, 90], "score": 0.93}],
        lesion_results=[{"bbox": [20, 20, 90, 90], "confidenceScore": 0.93, "lesionAreaRatio": 0.018}],
    )
    assert result.uncertainty_score < 0.35
    assert result.needs_review is False
