from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.grading_pipeline import GradingResult
from app.pipelines.risk_pipeline import RiskPipeline
from app.schemas.request import PatientProfile


def _settings(**overrides) -> Settings:
    values = {
        "ai_runtime_mode": "mock",
        "model_risk_enabled": False,
        "model_confidence_threshold": 0.5,
        "uncertainty_review_threshold": 0.35,
    }
    mapping = {
        "CG_AI_RUNTIME_MODE": "ai_runtime_mode",
        "CG_MODEL_RISK_ENABLED": "model_risk_enabled",
    }
    for key, value in overrides.items():
        target = mapping.get(key, key)
        if target == "model_risk_enabled":
            values[target] = str(value).lower() == "true"
        else:
            values[target] = value
    return Settings(**values)


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


def test_mock_mode_returns_mock_risk_result():
    settings = _settings(CG_AI_RUNTIME_MODE="mock")
    registry = ModelRegistry(settings)
    registry.startup()

    result = RiskPipeline(registry, settings).assess(
        PatientProfile(previous_caries_count=1),
        _grading(),
        [{"toothCode": "16"}],
        tooth_detection_count=2,
    )

    assert result.risk_mode == "mock"
    assert result.risk_impl_type == "MOCK"
    assert result.assessment.risk_level_code == "MEDIUM"
    assert result.raw_result["gradingLabel"] == "C2"
    assert result.raw_result["lesionRegionCount"] == 1


def test_hybrid_risk_enabled_uses_heuristic_adapter():
    settings = _settings(
        CG_AI_RUNTIME_MODE="hybrid",
        CG_MODEL_RISK_ENABLED="true",
    )
    registry = ModelRegistry(settings)
    registry.startup()

    result = RiskPipeline(registry, settings).assess(
        PatientProfile(
            previous_caries_count=3,
            sugar_diet_level_code="HIGH",
            brushing_frequency_code="LOW",
        ),
        _grading("C3", 0.5),
        [{"toothCode": "16"}, {"toothCode": "17"}],
        tooth_detection_count=2,
    )

    assert result.risk_mode == "real"
    assert result.risk_impl_type == "HEURISTIC"
    assert result.assessment.risk_level_code == "HIGH"
    assert result.assessment.recommended_cycle_days == 90
    assert result.raw_result["source"] == "risk_pipeline"
    assert result.raw_result["modelCode"] == "risk-fusion-heuristic-v1"
