from pathlib import Path

import pytest

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.grading_pipeline import GradingResult
from app.pipelines.risk_pipeline import RiskPipeline
from app.schemas.request import PatientProfile


def _settings(**overrides) -> Settings:
    repo_root = Path(__file__).resolve().parents[3]
    values = {
        "ai_runtime_mode": "hybrid",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "model_quality_enabled": False,
        "model_tooth_detect_enabled": False,
        "model_segmentation_enabled": False,
        "model_grading_enabled": False,
        "model_risk_enabled": False,
        "model_quality_impl_type": "HEURISTIC",
        "model_tooth_detect_impl_type": "HEURISTIC",
        "model_segmentation_impl_type": "HEURISTIC",
        "model_grading_impl_type": "HEURISTIC",
        "model_risk_impl_type": "HEURISTIC",
        "model_weights_dir": str(repo_root / "model-weights"),
        "quality_model_param_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "quality_model_weights_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "model_confidence_threshold": 0.5,
        "uncertainty_review_threshold": 0.35,
    }
    values.update(overrides)
    return Settings(**values)


def _pipeline(settings: Settings) -> RiskPipeline:
    assets = ModelAssets(settings)
    registry = ModelRegistry(settings, assets)
    registry.startup()
    return RiskPipeline(registry, settings)


def _grading(label: str = "C2", uncertainty: float = 0.42) -> GradingResult:
    return GradingResult(
        grading_mode="real",
        grading_impl_type="HEURISTIC",
        grading_label=label,
        confidence_score=0.58,
        uncertainty_score=uncertainty,
        needs_review=uncertainty >= 0.35,
        raw_result={},
    )


def test_disabled_risk_module_fails_explicitly() -> None:
    pipeline = _pipeline(_settings(ai_runtime_mode="mock"))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.assess(PatientProfile(previous_caries_count=1), _grading(), [{"toothCode": "16"}], tooth_detection_count=2)

    assert exc_info.value.code == "M5009"
    assert pipeline.get_last_impl_type() == "DISABLED"


def test_enabled_risk_module_returns_real_heuristic_assessment() -> None:
    pipeline = _pipeline(_settings(model_risk_enabled=True))

    result = pipeline.assess(
        PatientProfile(previous_caries_count=3, sugar_diet_level_code="HIGH", brushing_frequency_code="LOW"),
        _grading("C3", 0.5),
        [{"toothCode": "16"}, {"toothCode": "17"}],
        tooth_detection_count=2,
    )

    assert result.risk_mode == "real"
    assert result.risk_impl_type == "HEURISTIC"
    assert result.assessment.risk_level_code in {"LOW", "MEDIUM", "HIGH"}
    assert result.raw_result["source"] == "risk_pipeline"
