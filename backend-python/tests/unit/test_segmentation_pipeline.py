from pathlib import Path
import importlib.util

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput


def _settings(**overrides) -> Settings:
    repo_root = Path(__file__).resolve().parents[3]
    values = {
        "ai_runtime_mode": "hybrid",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "model_quality_enabled": False,
        "model_tooth_detect_enabled": False,
        "model_segmentation_enabled": False,
        "model_grading_enabled": False,
        "model_risk_enabled": False,
        "model_quality_impl_type": "HEURISTIC",
        "model_tooth_detect_impl_type": "HEURISTIC",
        "model_segmentation_impl_type": "HEURISTIC",
        "model_grading_impl_type": "HEURISTIC",
        "model_risk_impl_type": "HEURISTIC",
        "model_weights_dir": str(repo_root / "model-weights"),
        "quality_model_param_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "quality_model_weights_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "model_confidence_threshold": 0.3,
        "segmentation_force_fail": False,
    }
    values.update(overrides)
    return Settings(**values)


def _pipeline(settings: Settings) -> tuple[ModelAssets, SegmentationPipeline]:
    assets = ModelAssets(settings)
    registry = ModelRegistry(settings, assets)
    registry.startup()
    return assets, SegmentationPipeline(registry, settings, assets)


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
    path = tmp_path / "image.png"
    Image.fromarray(arr, mode="L").save(path)
    return path


def _image() -> ImageInput:
    return ImageInput(image_id=100)


def _detections() -> list[ToothDetection]:
    return [ToothDetection(image_id=100, tooth_code="16", bbox=[120, 70, 240, 170], detection_score=0.9)]


def test_disabled_segmentation_module_fails_explicitly(tmp_path: Path) -> None:
    _, pipeline = _pipeline(_settings(ai_runtime_mode="mock"))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.segment(_image(), None, _detections(), tmp_path / "visual")

    assert exc_info.value.code == "M5005"
    assert pipeline.get_last_impl_type() == "DISABLED"


def test_enabled_segmentation_module_renders_real_assets(sample_image: Path, tmp_path: Path) -> None:
    if importlib.util.find_spec("cv2") is None:
        pytest.skip("opencv-python-headless is not installed in this environment")
    assets, pipeline = _pipeline(_settings(model_segmentation_enabled=True))

    result = pipeline.segment(_image(), sample_image, _detections(), tmp_path / "visual")

    assert result.segmentation_mode == "real"
    assert result.segmentation_impl_type == "HEURISTIC"
    assert result.regions
    assert result.mask_path.exists()
    assert result.overlay_path.exists()
    assert result.heatmap_path.exists()
    assert result.raw_result["manifestPath"] == str(assets.segmentation_manifest.manifest_path)
    assert result.raw_result["classMapPath"] == str(assets.class_map_path)


def test_force_fail_raises_segmentation_business_error(sample_image: Path, tmp_path: Path) -> None:
    _, pipeline = _pipeline(_settings(ai_runtime_mode="real", model_segmentation_enabled=True, segmentation_force_fail=True))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.segment(_image(), sample_image, _detections(), tmp_path / "visual")

    assert exc_info.value.code == "M5008"
