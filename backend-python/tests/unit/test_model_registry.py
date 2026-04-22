"""Tests for ModelRegistry mode routing and lifecycle."""

from pathlib import Path

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry


def _settings(**overrides) -> Settings:
    repo_root = Path(__file__).resolve().parents[3]
    values = {
        "ai_runtime_mode": "mock",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "model_quality_enabled": False,
        "model_quality_impl_type": "HEURISTIC",
        "model_tooth_detect_enabled": False,
        "model_tooth_detect_impl_type": "HEURISTIC",
        "model_segmentation_enabled": False,
        "model_segmentation_impl_type": "HEURISTIC",
        "model_grading_enabled": False,
        "model_grading_impl_type": "HEURISTIC",
        "model_risk_enabled": False,
        "model_risk_impl_type": "HEURISTIC",
        "model_weights_dir": str(repo_root / "model-weights"),
        "quality_model_param_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "quality_model_weights_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "model_confidence_threshold": 0.5,
    }
    values.update(overrides)
    return Settings(**values)


def _registry(**overrides) -> ModelRegistry:
    settings = _settings(**overrides)
    return ModelRegistry(settings, ModelAssets(settings))


class TestMockMode:
    def test_disabled_modules_keep_registry_empty(self):
        registry = _registry(ai_runtime_mode="mock")
        registry.startup()

        assert registry.get_quality_model() is None
        assert registry.get_tooth_detector() is None
        assert registry.get_segmenter() is None
        assert registry.get_grading_model() is None
        assert registry.get_risk_model() is None
        assert registry.get_runtime_mode() == "mock"

    def test_enabled_module_loads_mock_adapter_but_is_not_real(self):
        registry = _registry(ai_runtime_mode="mock", model_quality_enabled=True)
        registry.startup()

        assert registry.get_quality_model() is not None
        assert registry.get_quality_model().info()["implType"] == "MOCK"
        assert registry.is_module_loaded("quality") is True
        assert registry.is_module_real("quality") is False


class TestHybridMode:
    def test_enabled_modules_load_heuristic_adapters(self):
        registry = _registry(
            ai_runtime_mode="hybrid",
            model_quality_enabled=True,
            model_tooth_detect_enabled=True,
            model_segmentation_enabled=True,
            model_grading_enabled=True,
            model_risk_enabled=True,
        )
        registry.startup()

        assert registry.get_quality_model().info()["implType"] == "HEURISTIC"
        assert registry.get_tooth_detector().info()["implType"] == "HEURISTIC"
        assert registry.get_segmenter().info()["implType"] == "HEURISTIC"
        assert registry.get_grading_model().info()["implType"] == "HEURISTIC"
        assert registry.get_risk_model().info()["implType"] == "HEURISTIC"
        assert registry.is_module_real("quality") is True
        assert registry.is_module_real("tooth_detect") is True

    def test_disabled_module_is_not_real(self):
        registry = _registry(
            ai_runtime_mode="hybrid",
            model_quality_enabled=True,
            model_tooth_detect_enabled=False,
        )
        registry.startup()

        assert registry.is_module_real("quality") is True
        assert registry.is_module_real("tooth_detect") is False


class TestRealMode:
    def test_only_enabled_modules_become_real(self):
        registry = _registry(
            ai_runtime_mode="real",
            model_quality_enabled=True,
            model_tooth_detect_enabled=True,
        )
        registry.startup()

        assert registry.is_module_real("quality") is True
        assert registry.is_module_real("tooth_detect") is True
        assert registry.is_module_real("segmentation") is False
        assert registry.get_quality_model() is not None
        assert registry.get_tooth_detector() is not None
        assert registry.get_segmenter() is None


class TestLifecycle:
    def test_shutdown_unloads_all_loaded_adapters(self):
        registry = _registry(
            ai_runtime_mode="hybrid",
            model_quality_enabled=True,
            model_tooth_detect_enabled=True,
        )
        registry.startup()

        registry.shutdown()

        assert registry.get_quality_model().is_loaded() is False
        assert registry.get_tooth_detector().is_loaded() is False

    def test_status_includes_adapter_and_manifest_assets(self):
        registry = _registry(
            ai_runtime_mode="hybrid",
            model_segmentation_enabled=True,
            model_grading_enabled=True,
        )
        registry.startup()

        status = registry.status()

        assert status["aiRuntimeMode"] == "hybrid"
        assert status["adapters"]["SEGMENTATION"]["implType"] == "HEURISTIC"
        assert status["adapters"]["GRADING"]["implType"] == "HEURISTIC"
        assert status["assets"]["segmentation"]["modelCode"] == "caries-segmentation-v1"
        assert status["assets"]["segmentation"]["datasetVersion"] == "caries-annot-v1.0"
        assert status["assets"]["grading"]["modelCode"] == "caries-grading-v1"
        assert status["assets"]["grading"]["labelOrder"] == ["C0", "C1", "C2", "C3"]
