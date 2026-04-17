"""Tests for ModelRegistry — mode routing and lifecycle."""

import os
from unittest.mock import patch

import pytest

from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry


def _settings(**overrides) -> Settings:
    """Create a Settings with environment-based defaults overridden."""
    env = {
        "CG_AI_RUNTIME_MODE": "mock",
        "CG_MODEL_QUALITY_ENABLED": "false",
        "CG_MODEL_TOOTH_DETECT_ENABLED": "false",
        "CG_MODEL_SEGMENTATION_ENABLED": "false",
        "CG_MODEL_GRADING_ENABLED": "false",
        "CG_MODEL_RISK_ENABLED": "false",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "0.5",
    }
    env.update(overrides)
    with patch.dict(os.environ, env, clear=False):
        return Settings()


class TestMockMode:
    def test_all_modules_return_none(self):
        registry = ModelRegistry(_settings(CG_AI_RUNTIME_MODE="mock"))
        registry.startup()
        assert registry.get_quality_model() is None
        assert registry.get_tooth_detector() is None
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

    def test_all_adapters_loaded(self):
        registry = ModelRegistry(_settings(CG_AI_RUNTIME_MODE="real"))
        registry.startup()
        assert registry.get_quality_model() is not None
        assert registry.get_tooth_detector() is not None


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
