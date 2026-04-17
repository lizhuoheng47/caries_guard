"""Tests for ModelRegistry — mode routing and lifecycle."""

from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry


def _settings(**overrides) -> Settings:
    values = {
        "ai_runtime_mode": "mock",
        "model_quality_enabled": False,
        "model_tooth_detect_enabled": False,
        "model_segmentation_enabled": False,
        "model_grading_enabled": False,
        "model_risk_enabled": False,
        "model_confidence_threshold": 0.5,
    }
    mapping = {
        "CG_AI_RUNTIME_MODE": "ai_runtime_mode",
        "CG_MODEL_QUALITY_ENABLED": "model_quality_enabled",
        "CG_MODEL_TOOTH_DETECT_ENABLED": "model_tooth_detect_enabled",
        "CG_MODEL_SEGMENTATION_ENABLED": "model_segmentation_enabled",
        "CG_MODEL_GRADING_ENABLED": "model_grading_enabled",
        "CG_MODEL_RISK_ENABLED": "model_risk_enabled",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "model_confidence_threshold",
    }
    for key, value in overrides.items():
        target = mapping.get(key, key)
        if target.startswith("model_") and target.endswith("_enabled"):
            values[target] = str(value).lower() == "true"
        elif target == "model_confidence_threshold":
            values[target] = float(value)
        else:
            values[target] = value
    return Settings(**values)


class TestMockMode:
    def test_all_modules_return_none(self):
        registry = ModelRegistry(_settings(CG_AI_RUNTIME_MODE="mock"))
        registry.startup()
        assert registry.get_quality_model() is None
        assert registry.get_tooth_detector() is None
        assert registry.get_segmenter() is None
        assert registry.get_grading_model() is None
        assert registry.get_risk_model() is None
        assert registry.get_runtime_mode() == "mock"

    def test_is_module_real_always_false(self):
        registry = ModelRegistry(_settings(CG_AI_RUNTIME_MODE="mock"))
        assert not registry.is_module_real("quality")
        assert not registry.is_module_real("tooth_detect")


class TestHybridMode:
    def test_quality_enabled_loads_adapter(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        ))
        registry.startup()
        assert registry.get_quality_model() is not None
        assert registry.get_quality_model().is_loaded()
        assert registry.get_tooth_detector() is None

    def test_tooth_detect_enabled_loads_adapter(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        ))
        registry.startup()
        assert registry.get_tooth_detector() is not None
        assert registry.get_tooth_detector().is_loaded()
        assert registry.get_quality_model() is None

    def test_segmentation_enabled_loads_adapter(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_SEGMENTATION_ENABLED="true",
        ))
        registry.startup()
        assert registry.get_segmenter() is not None
        assert registry.get_segmenter().is_loaded()

    def test_grading_enabled_loads_adapter(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_GRADING_ENABLED="true",
        ))
        registry.startup()
        assert registry.get_grading_model() is not None
        assert registry.get_grading_model().is_loaded()

    def test_risk_enabled_loads_adapter(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_RISK_ENABLED="true",
        ))
        registry.startup()
        assert registry.get_risk_model() is not None
        assert registry.get_risk_model().is_loaded()

    def test_both_enabled(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        ))
        registry.startup()
        assert registry.get_quality_model() is not None
        assert registry.get_tooth_detector() is not None

    def test_disabled_module_is_not_real(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
            CG_MODEL_TOOTH_DETECT_ENABLED="false",
        ))
        assert registry.is_module_real("quality")
        assert not registry.is_module_real("tooth_detect")


class TestRealMode:
    def test_is_module_real_always_true(self):
        registry = ModelRegistry(_settings(CG_AI_RUNTIME_MODE="real"))
        assert registry.is_module_real("quality")
        assert registry.is_module_real("tooth_detect")
        assert registry.is_module_real("segmentation")
        assert registry.is_module_real("grading")
        assert registry.is_module_real("risk")

    def test_all_adapters_loaded(self):
        registry = ModelRegistry(_settings(CG_AI_RUNTIME_MODE="real"))
        registry.startup()
        assert registry.get_quality_model() is not None
        assert registry.get_tooth_detector() is not None
        assert registry.get_segmenter() is not None
        assert registry.get_grading_model() is not None
        assert registry.get_risk_model() is not None


class TestLifecycle:
    def test_shutdown_unloads_all(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        ))
        registry.startup()
        registry.shutdown()
        assert not registry.get_quality_model().is_loaded()
        assert not registry.get_tooth_detector().is_loaded()

    def test_status_dict(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        ))
        registry.startup()
        status = registry.status()
        assert status["aiRuntimeMode"] == "hybrid"
        assert "QUALITY" in status["adapters"]
        assert status["adapters"]["QUALITY"]["implType"] == "HEURISTIC"

    def test_status_includes_segmentation(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_SEGMENTATION_ENABLED="true",
        ))
        registry.startup()
        status = registry.status()
        assert "SEGMENTATION" in status["adapters"]
        assert status["adapters"]["SEGMENTATION"]["implType"] == "HEURISTIC"

    def test_status_includes_grading(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_GRADING_ENABLED="true",
        ))
        registry.startup()
        status = registry.status()
        assert "GRADING" in status["adapters"]
        assert status["adapters"]["GRADING"]["implType"] == "HEURISTIC"

    def test_status_includes_risk(self):
        registry = ModelRegistry(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_RISK_ENABLED="true",
        ))
        registry.startup()
        status = registry.status()
        assert "RISK" in status["adapters"]
        assert status["adapters"]["RISK"]["implType"] == "HEURISTIC"
