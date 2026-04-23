"""Tests for LesionSegmenterAdapter output structure."""

from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from app.core.exceptions import ModelRuntimeException
from app.infra.model.base_model import ImplType
from app.infra.model.lesion_segmenter import LesionSegmenterAdapter
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_router import ModelRouter
from app.infra.model.segmentation_model_adapter import SegmentationModelAdapter
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
    assert result["regions"][0]["toothCode"] == "UNKNOWN"
    assert int(np.sum(result["maskArray"] > 0)) > 0


class _AssetSettings:
    model_segmentation_manifest_path = "assets/models/manifests/segmentation_v1.yaml"
    model_grading_manifest_path = "assets/models/manifests/grading_v1.yaml"


class _RouterSettings:
    ai_runtime_mode = "real"
    model_segmentation_impl_type = "ML_MODEL"


def test_router_maps_segmentation_ml_model_adapter():
    impl_type = ModelRouter.resolve_impl_type(_RouterSettings(), "segmentation")
    assert impl_type == ImplType.ML_MODEL
    assert ModelRouter.get_adapter_class("segmentation", impl_type) is SegmentationModelAdapter


def test_onnx_adapter_fails_explicitly_when_checkpoint_missing():
    assets = ModelAssets(_AssetSettings())
    adapter = SegmentationModelAdapter(model_assets=assets)

    with pytest.raises(ModelRuntimeException, match="segmentation checkpoint does not exist") as exc_info:
        adapter.load()
    assert exc_info.value.code == "M5006"
