"""Tests for GradingPipeline routing, fallback, and review threshold rules."""

from pathlib import Path
from typing import Any, cast

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.grading_pipeline import GradingPipeline
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput


def _settings(**overrides) -> Settings:
    values = {
        "ai_runtime_mode": "mock",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "model_grading_enabled": False,
        "model_confidence_threshold": 0.3,
        "grading_force_fail": False,
        "uncertainty_review_threshold": 0.35,
    }
    mapping = {
        "CG_AI_RUNTIME_MODE": "ai_runtime_mode",
        "CG_MODEL_GRADING_ENABLED": "model_grading_enabled",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "model_confidence_threshold",
        "CG_GRADING_FORCE_FAIL": "grading_force_fail",
        "CG_UNCERTAINTY_REVIEW_THRESHOLD": "uncertainty_review_threshold",
    }
    for key, value in overrides.items():
        target = mapping.get(key, key)
        if target in {"model_grading_enabled", "grading_force_fail"}:
            values[target] = str(value).lower() == "true"
        elif target in {"model_confidence_threshold", "uncertainty_review_threshold"}:
            values[target] = float(value)
        else:
            values[target] = value
    return Settings(**values)


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(85, 190, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 35
    img = Image.fromarray(cast(Any, arr), mode="L")
    path = tmp_path / "image.png"
    img.save(path)
    return path


def _image() -> ImageInput:
    return ImageInput(image_id=100)


def _regions() -> list[dict]:
    return [{"toothCode": "16", "bbox": [150, 80, 230, 155], "score": 0.88, "regionIndex": 0}]


def _detections() -> list[ToothDetection]:
    return [ToothDetection(image_id=100, tooth_code="16", bbox=[120, 70, 240, 170], detection_score=0.9)]


class TestMockMode:
    def test_returns_mock_grading(self):
        settings = _settings(CG_AI_RUNTIME_MODE="mock")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = GradingPipeline(registry, settings)

        result = pipeline.grade(_image(), None, [], [])

        assert result.grading_mode == "mock"
        assert result.grading_impl_type == "MOCK"
        assert result.grading_label == "C1"
        assert result.needs_review is False
        assert result.raw_result["source"] == "mock"


class TestHybridMode:
    def test_real_path_when_enabled(self, sample_image: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_GRADING_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = GradingPipeline(registry, settings)

        result = pipeline.grade(_image(), sample_image, _regions(), _detections())

        assert result.grading_mode == "real"
        assert result.grading_impl_type == "HEURISTIC"
        assert result.grading_label in {"C0", "C1", "C2", "C3"}
        assert "reviewThreshold" in result.raw_result
        assert isinstance(result.raw_result.get("lesionGrades"), list)
        assert isinstance(result.raw_result.get("uncertaintyReasons"), list)

    def test_fallback_to_mock_on_error(self, sample_image: Path, monkeypatch):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_GRADING_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        adapter = registry.get_grading_model()

        def fail(*args, **kwargs):
            raise RuntimeError("forced failure")

        monkeypatch.setattr(adapter, "infer", fail)
        pipeline = GradingPipeline(registry, settings)

        result = pipeline.grade(_image(), sample_image, _regions(), _detections())

        assert result.grading_mode == "mock"
        assert result.grading_impl_type == "MOCK"
        assert result.raw_result["fallbackReason"] == "forced failure"

    def test_uncertainty_threshold_sets_needs_review(self, sample_image: Path, monkeypatch):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_GRADING_ENABLED="true",
            CG_UNCERTAINTY_REVIEW_THRESHOLD=0.35,
        )
        registry = ModelRegistry(settings)
        registry.startup()
        adapter = registry.get_grading_model()

        def high_uncertainty(*args, **kwargs):
            return {
                "gradingLabel": "C2",
                "confidenceScore": 0.51,
                "uncertaintyScore": 0.48,
                "implType": "HEURISTIC",
                "rawResult": {"source": "test"},
            }

        monkeypatch.setattr(adapter, "infer", high_uncertainty)
        pipeline = GradingPipeline(registry, settings)

        result = pipeline.grade(_image(), sample_image, _regions(), _detections())

        assert result.needs_review is True
        assert result.raw_result["needsReview"] is True
        assert result.raw_result["reviewThreshold"] == 0.35
        assert "HIGH_UNCERTAINTY" in result.raw_result["uncertaintyReasons"]


class TestRealMode:
    def test_raises_when_image_missing(self):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = GradingPipeline(registry, settings)

        with pytest.raises(BusinessException) as exc_info:
            pipeline.grade(_image(), None, _regions(), _detections())
        assert exc_info.value.code == "M5007"

    def test_raises_on_inference_error(self, sample_image: Path, monkeypatch):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        adapter = registry.get_grading_model()

        def fail(*args, **kwargs):
            raise RuntimeError("forced failure")

        monkeypatch.setattr(adapter, "infer", fail)
        pipeline = GradingPipeline(registry, settings)

        with pytest.raises(BusinessException) as exc_info:
            pipeline.grade(_image(), sample_image, _regions(), _detections())
        assert exc_info.value.code == "M5008"

    def test_force_fail_raises_in_real_mode(self, sample_image: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="real",
            CG_GRADING_FORCE_FAIL="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = GradingPipeline(registry, settings)

        with pytest.raises(BusinessException) as exc_info:
            pipeline.grade(_image(), sample_image, _regions(), _detections())
        assert exc_info.value.code == "M5008"

    def test_invalid_real_grading_label_raises_explicitly(self, sample_image: Path, monkeypatch):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        adapter = registry.get_grading_model()

        def invalid_label(*args, **kwargs):
            return {
                "gradingLabel": "UNKNOWN",
                "confidenceScore": 0.72,
                "uncertaintyScore": 0.18,
                "implType": "HEURISTIC",
                "rawResult": {"classMargin": 0.08},
            }

        monkeypatch.setattr(adapter, "infer", invalid_label)
        pipeline = GradingPipeline(registry, settings)

        with pytest.raises(BusinessException) as exc_info:
            pipeline.grade(_image(), sample_image, _regions(), _detections())
        assert exc_info.value.code in {"M5008", "M5024"}
