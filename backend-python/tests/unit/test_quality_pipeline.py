from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.quality_pipeline import QualityPipeline
from app.schemas.request import ImageInput


def _settings(**overrides) -> Settings:
    repo_root = Path(__file__).resolve().parents[3]
    values = {
        "ai_runtime_mode": "hybrid",
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
        "quality_fail_strategy": "CONTINUE",
    }
    values.update(overrides)
    return Settings(**values)


def _pipeline(settings: Settings) -> QualityPipeline:
    assets = ModelAssets(settings)
    registry = ModelRegistry(settings, assets)
    registry.startup()
    return QualityPipeline(registry, settings)


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(60, 200, (256, 512), dtype=np.uint8)
    path = tmp_path / "test.png"
    Image.fromarray(arr, mode="L").save(path)
    return path


def test_disabled_quality_module_fails_explicitly() -> None:
    pipeline = _pipeline(_settings(ai_runtime_mode="mock"))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.check(ImageInput(image_id=1), None)

    assert exc_info.value.code == "M5001"
    assert pipeline.get_last_impl_type() == "DISABLED"


def test_enabled_quality_module_returns_real_result(sample_image: Path) -> None:
    pipeline = _pipeline(_settings(model_quality_enabled=True))

    result = pipeline.check(ImageInput(image_id=1), sample_image)

    assert result.check_result_code in {"PASS", "WARN", "FAIL"}
    assert result.impl_type == "HEURISTIC"
    assert pipeline.get_last_impl_type() == "HEURISTIC"
    assert result.model_version is not None


def test_enabled_quality_module_requires_image_path() -> None:
    pipeline = _pipeline(_settings(ai_runtime_mode="real", model_quality_enabled=True))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.check(ImageInput(image_id=1), None)

    assert exc_info.value.code == "M5003"


def test_fail_fast_raises_when_quality_result_is_fail(tmp_path: Path) -> None:
    pipeline = _pipeline(_settings(ai_runtime_mode="real", model_quality_enabled=True, quality_fail_strategy="FAIL_FAST"))
    black = tmp_path / "black.png"
    Image.fromarray(np.zeros((256, 512), dtype=np.uint8), mode="L").save(black)

    with pytest.raises(BusinessException) as exc_info:
        pipeline.check(ImageInput(image_id=1), black)

    assert exc_info.value.code == "M5004"
