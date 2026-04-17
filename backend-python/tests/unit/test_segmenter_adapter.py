"""Tests for LesionSegmenterAdapter output structure."""

from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from app.infra.model.base_model import ImplType
from app.infra.model.lesion_segmenter import LesionSegmenterAdapter
from app.schemas.callback import ToothDetection


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(70, 210, (256, 512), dtype=np.uint8)
    arr[90:130, 160:210] = 35
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "dental.png"
    img.save(path)
    return path


def test_impl_type_and_lifecycle():
    adapter = LesionSegmenterAdapter()
    assert adapter.impl_type == ImplType.HEURISTIC
    assert adapter.model_type_code == "SEGMENTATION"
    assert not adapter.is_loaded()
    adapter.load()
    assert adapter.is_loaded()
    adapter.unload()
    assert not adapter.is_loaded()


def test_infer_returns_mask_and_regions(sample_image: Path):
    adapter = LesionSegmenterAdapter(confidence_threshold=0.3)
    adapter.load()
    result = adapter.infer(
        sample_image,
        [ToothDetection(image_id=1, tooth_code="16", bbox=[120, 70, 240, 170], detection_score=0.9)],
    )

    assert result["implType"] == "HEURISTIC"
    assert result["segmentationScore"] >= 0.3
    assert "maskArray" in result
    assert result["maskArray"].shape == (256, 512)
    assert int(np.sum(result["maskArray"] > 0)) > 0
    assert len(result["regions"]) >= 1
    region = result["regions"][0]
    assert region["toothCode"] == "16"
    assert len(region["bbox"]) == 4
    assert len(region["polygon"]) == 4
    assert "rawResult" in result


def test_infer_without_detections_uses_fallback_region(sample_image: Path):
    adapter = LesionSegmenterAdapter(confidence_threshold=0.3)
    adapter.load()
    result = adapter.infer(sample_image, [])

    assert len(result["regions"]) == 1
    assert result["regions"][0]["toothCode"] == "16"
    assert int(np.sum(result["maskArray"] > 0)) > 0
