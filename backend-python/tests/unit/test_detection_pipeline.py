"""Tests for DetectionPipeline — mock/heuristic routing and fallback rules."""

import os
from pathlib import Path
from unittest.mock import patch

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.schemas.request import ImageInput
from app.services.image_fetch_service import FetchedImage


def _settings(**overrides) -> Settings:
    env = {
        "CG_AI_RUNTIME_MODE": "mock",
        "CG_MODEL_TOOTH_DETECT_ENABLED": "false",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "0.3",
    }
    env.update(overrides)
    with patch.dict(os.environ, env, clear=False):
        return Settings()


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(60, 200, (256, 512), dtype=np.uint8)
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "test.png"
    img.save(path)
    return path


def _image_input(image_id: int = 1) -> ImageInput:
    return ImageInput(image_id=image_id)


def _fetched(image_id: int, path: Path) -> FetchedImage:
    return FetchedImage(
        image_id=image_id,
        image_type_code="DENTAL",
        path=path,
        size_bytes=path.stat().st_size,
        source="test",
    )


class TestMockMode:
    def test_returns_two_mock_detections(self):
        settings = _settings(CG_AI_RUNTIME_MODE="mock")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = DetectionPipeline(registry, settings)

        detections = pipeline.detect(_image_input())
        assert len(detections) == 2
        assert detections[0].tooth_code == "16"
        assert detections[1].tooth_code == "26"

    def test_impl_type_is_mock(self):
        settings = _settings(CG_AI_RUNTIME_MODE="mock")
        registry = ModelRegistry(settings)
        pipeline = DetectionPipeline(registry, settings)
        assert pipeline.get_last_impl_type() == "MOCK"


class TestHybridMode:
    def test_real_path_returns_detections(self, sample_image: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = DetectionPipeline(registry, settings)

        detections = pipeline.detect(_image_input(), sample_image)
        assert isinstance(detections, list)
        # With a random image and low threshold, should detect some regions
        for d in detections:
            assert hasattr(d, "tooth_code")
            assert hasattr(d, "bbox")
            assert hasattr(d, "detection_score")

    def test_detect_all_aggregates(self, sample_image: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = DetectionPipeline(registry, settings)

        images = [_image_input(1), _image_input(2)]
        fetched = [_fetched(1, sample_image), _fetched(2, sample_image)]
        detections = pipeline.detect_all(images, fetched)
        assert isinstance(detections, list)

    def test_fallback_on_error(self, tmp_path: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = DetectionPipeline(registry, settings)

        bad_path = tmp_path / "nonexistent.png"
        detections = pipeline.detect(_image_input(), bad_path)
        # Should fallback to mock
        assert len(detections) == 2
        assert detections[0].tooth_code == "16"


class TestRealMode:
    def test_raises_on_failure(self, tmp_path: Path):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = DetectionPipeline(registry, settings)

        bad_path = tmp_path / "nonexistent.png"
        with pytest.raises(BusinessException) as exc_info:
            pipeline.detect(_image_input(), bad_path)
        assert exc_info.value.code == "M5004"

    def test_real_success(self, sample_image: Path):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = DetectionPipeline(registry, settings)

        detections = pipeline.detect(_image_input(), sample_image)
        assert isinstance(detections, list)
