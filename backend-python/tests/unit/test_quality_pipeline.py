"""Tests for QualityPipeline — mock/heuristic routing and fallback rules."""

from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.quality_pipeline import QualityPipeline
from app.schemas.request import ImageInput


def _settings(**overrides) -> Settings:
    values = {
        "ai_runtime_mode": "mock",
        "model_quality_enabled": False,
        "model_confidence_threshold": 0.5,
    }
    mapping = {
        "CG_AI_RUNTIME_MODE": "ai_runtime_mode",
        "CG_MODEL_QUALITY_ENABLED": "model_quality_enabled",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "model_confidence_threshold",
    }
    for key, value in overrides.items():
        target = mapping.get(key, key)
        if target == "model_quality_enabled":
            values[target] = str(value).lower() == "true"
        elif target == "model_confidence_threshold":
            values[target] = float(value)
        else:
            values[target] = value
    return Settings(**values)


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(60, 200, (256, 512), dtype=np.uint8)
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "test.png"
    img.save(path)
    return path


def _image_input(image_id: int = 1) -> ImageInput:
    return ImageInput(image_id=image_id)


class TestMockMode:
    def test_returns_mock_result(self):
        settings = _settings(CG_AI_RUNTIME_MODE="mock")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        result = pipeline.check(_image_input())
        assert result.check_result_code == "PASS"
        assert result.quality_score == 90
        assert "mock" in result.suggestion_text

    def test_impl_type_is_mock(self):
        settings = _settings(CG_AI_RUNTIME_MODE="mock")
        registry = ModelRegistry(settings)
        pipeline = QualityPipeline(registry, settings)
        assert pipeline.get_last_impl_type() == "MOCK"


class TestHybridMode:
    def test_real_path_when_enabled(self, sample_image: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        result = pipeline.check(_image_input(), sample_image)
        assert result.check_result_code in {"PASS", "WARN", "FAIL"}
        assert result.quality_status in {"PASS", "WARN", "FAIL"}
        assert result.quality_score_float is not None
        assert result.quality_score >= 0
        # Should NOT contain "mock" since it went through heuristic
        assert "mock" not in (result.suggestion_text or "")

    def test_fallback_to_mock_on_error(self, tmp_path: Path):
        """Hybrid mode falls back to mock when inference fails."""
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        # Pass a non-existent image to trigger an error
        bad_path = tmp_path / "nonexistent.png"
        result = pipeline.check(_image_input(), bad_path)
        assert result.check_result_code == "PASS"
        assert "mock" in result.suggestion_text

    def test_fallback_when_no_image_path(self):
        """Hybrid mode falls back when image_path is None."""
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        result = pipeline.check(_image_input(), None)
        assert result.check_result_code == "PASS"


class TestRealMode:
    def test_raises_on_missing_adapter(self):
        """Real mode raises BusinessException when adapter unavailable."""
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        # Don't call startup() → adapter is None but is_module_real returns True
        # Need to manually set no adapter scenario
        registry._quality = None
        pipeline = QualityPipeline(registry, settings)

        with pytest.raises(BusinessException) as exc_info:
            pipeline.check(_image_input(), None)
        assert exc_info.value.code == "M5001"

    def test_raises_on_inference_failure(self, tmp_path: Path):
        """Real mode raises BusinessException when inference fails — NO silent fallback."""
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        bad_path = tmp_path / "nonexistent.png"
        with pytest.raises(BusinessException) as exc_info:
            pipeline.check(_image_input(), bad_path)
        assert exc_info.value.code == "M5002"

    def test_real_success(self, sample_image: Path):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        result = pipeline.check(_image_input(), sample_image)
        assert result.check_result_code in {"PASS", "WARN", "FAIL"}
        assert result.impl_type == "HEURISTIC"

    def test_real_warn_continues(self, sample_image: Path, monkeypatch):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)
        adapter = registry.get_quality_model()

        monkeypatch.setattr(
            adapter,
            "infer",
            lambda _path: {
                "qualityStatusCode": "WARN",
                "qualityScore": 0.61,
                "qualityIssues": ["blur"],
                "retakeSuggested": False,
                "blurScore": 0.51,
                "exposureScore": 0.84,
                "integrityScore": 0.7,
                "occlusionScore": 0.82,
                "implType": "HEURISTIC",
                "modelVersion": "test-v1",
                "inferenceMillis": 11,
                "rawResult": {},
            },
        )

        result = pipeline.check(_image_input(), sample_image)
        assert result.check_result_code == "WARN"
        assert result.quality_issues == ["blur"]
        assert result.retake_suggested is False

    def test_real_fail_continue_branch(self, sample_image: Path, monkeypatch):
        settings = _settings(CG_AI_RUNTIME_MODE="real", quality_fail_strategy="CONTINUE")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)
        adapter = registry.get_quality_model()

        monkeypatch.setattr(
            adapter,
            "infer",
            lambda _path: {
                "qualityStatusCode": "FAIL",
                "qualityScore": 0.28,
                "qualityIssues": ["occlusion", "field_cutoff"],
                "retakeSuggested": True,
                "blurScore": 0.2,
                "exposureScore": 0.41,
                "integrityScore": 0.16,
                "occlusionScore": 0.11,
                "implType": "HEURISTIC",
                "modelVersion": "test-v1",
                "inferenceMillis": 12,
                "rawResult": {},
            },
        )

        result = pipeline.check(_image_input(), sample_image)
        assert result.check_result_code == "FAIL"
        assert result.retake_suggested is True

    def test_real_mode_fail_fast_raises_on_fail(self, tmp_path: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="real",
            quality_fail_strategy="FAIL_FAST",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = QualityPipeline(registry, settings)

        # Pure black image should be classified as FAIL.
        bad = tmp_path / "black.png"
        Image.fromarray(np.zeros((256, 512), dtype=np.uint8), mode="L").save(bad)
        with pytest.raises(BusinessException) as exc_info:
            pipeline.check(_image_input(), bad)
        assert exc_info.value.code == "M5003"
