"""Tests for SegmentationPipeline routing and fallback rules."""

from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput


def _settings(**overrides) -> Settings:
    values = {
        "ai_runtime_mode": "mock",
        "model_segmentation_enabled": False,
        "model_confidence_threshold": 0.3,
    }
    mapping = {
        "CG_AI_RUNTIME_MODE": "ai_runtime_mode",
        "CG_MODEL_SEGMENTATION_ENABLED": "model_segmentation_enabled",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "model_confidence_threshold",
    }
    for key, value in overrides.items():
        target = mapping.get(key, key)
        if target == "model_segmentation_enabled":
            values[target] = str(value).lower() == "true"
        elif target == "model_confidence_threshold":
            values[target] = float(value)
        else:
            values[target] = value
    return Settings(**values)


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "image.png"
    img.save(path)
    return path


def _image() -> ImageInput:
    return ImageInput(image_id=100)


def _detections() -> list[ToothDetection]:
    return [ToothDetection(image_id=100, tooth_code="16", bbox=[120, 70, 240, 170], detection_score=0.9)]


class TestMockMode:
    def test_generates_mock_assets(self, sample_image: Path, tmp_path: Path):
        settings = _settings(CG_AI_RUNTIME_MODE="mock")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = SegmentationPipeline(registry, settings)

        result = pipeline.segment(_image(), sample_image, _detections(), tmp_path / "visual")

        assert result.segmentation_mode == "mock"
        assert result.segmentation_impl_type == "MOCK"
        assert result.mask_path.exists()
        assert result.overlay_path.exists()
        assert result.heatmap_path.exists()
        assert result.mask_path.stat().st_size > 0


class TestHybridMode:
    def test_real_path_when_enabled(self, sample_image: Path, tmp_path: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_SEGMENTATION_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = SegmentationPipeline(registry, settings)

        result = pipeline.segment(_image(), sample_image, _detections(), tmp_path / "visual")

        assert result.segmentation_mode == "real"
        assert result.segmentation_impl_type == "HEURISTIC"
        assert result.regions[0]["toothCode"] == "16"
        assert result.mask_path.exists()
        assert result.overlay_path.exists()
        assert result.heatmap_path.exists()

    def test_fallback_to_mock_on_error(self, tmp_path: Path):
        settings = _settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_SEGMENTATION_ENABLED="true",
        )
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = SegmentationPipeline(registry, settings)

        result = pipeline.segment(_image(), tmp_path / "missing.png", _detections(), tmp_path / "visual")

        assert result.segmentation_mode == "mock"
        assert result.segmentation_impl_type == "MOCK"
        assert result.mask_path.exists()


class TestRealMode:
    def test_raises_on_missing_image(self, tmp_path: Path):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = SegmentationPipeline(registry, settings)

        with pytest.raises(BusinessException) as exc_info:
            pipeline.segment(_image(), tmp_path / "missing.png", _detections(), tmp_path / "visual")
        assert exc_info.value.code == "M5006"

    def test_real_success(self, sample_image: Path, tmp_path: Path):
        settings = _settings(CG_AI_RUNTIME_MODE="real")
        registry = ModelRegistry(settings)
        registry.startup()
        pipeline = SegmentationPipeline(registry, settings)

        result = pipeline.segment(_image(), sample_image, _detections(), tmp_path / "visual")

        assert result.segmentation_mode == "real"
        assert result.segmentation_impl_type == "HEURISTIC"
