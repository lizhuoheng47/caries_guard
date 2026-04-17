"""Tests for GradingModelAdapter output structure."""

from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from app.infra.model.base_model import ImplType
from app.infra.model.grading_model import GradingModelAdapter


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(85, 190, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 35
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "dental.png"
    img.save(path)
    return path


def test_impl_type_and_lifecycle():
    adapter = GradingModelAdapter()
    assert adapter.impl_type == ImplType.HEURISTIC
    assert adapter.model_type_code == "GRADING"
    assert not adapter.is_loaded()
    adapter.load()
    assert adapter.is_loaded()
    adapter.unload()
    assert not adapter.is_loaded()


def test_infer_returns_required_keys(sample_image: Path):
    adapter = GradingModelAdapter(confidence_threshold=0.3)
    adapter.load()
    result = adapter.infer(
        sample_image,
        [
            {
                "toothCode": "16",
                "bbox": [150, 80, 230, 155],
                "score": 0.88,
                "regionIndex": 0,
            }
        ],
        [],
    )

    assert result["implType"] == "HEURISTIC"
    assert result["gradingLabel"] in {"C0", "C1", "C2", "C3"}
    assert 0.0 <= result["confidenceScore"] <= 1.0
    assert 0.0 <= result["uncertaintyScore"] <= 1.0
    assert "rawResult" in result
    assert result["rawResult"]["selectedToothCode"] == "16"
    assert result["rawResult"]["regionCount"] == 1
    assert result["rawResult"]["severityScore"] >= 0.0


def test_infer_without_regions_uses_detection_fallback(sample_image: Path):
    adapter = GradingModelAdapter(confidence_threshold=0.3)
    adapter.load()
    result = adapter.infer(
        sample_image,
        [],
        [{"toothCode": "26", "bbox": [220, 70, 340, 180], "score": 0.8}],
    )

    assert result["rawResult"]["selectedToothCode"] == "26"
    assert result["rawResult"]["regionCount"] == 1
