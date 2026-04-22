from pathlib import Path
from typing import Any, cast

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.grading_pipeline import GradingPipeline
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
        "grading_force_fail": False,
        "uncertainty_review_threshold": 0.35,
    }
    values.update(overrides)
    return Settings(**values)


def _runtime(settings: Settings) -> tuple[ModelAssets, ModelRegistry, GradingPipeline]:
    assets = ModelAssets(settings)
    registry = ModelRegistry(settings, assets)
    registry.startup()
    return assets, registry, GradingPipeline(registry, settings, assets)


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(85, 190, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 35
    path = tmp_path / "image.png"
    Image.fromarray(cast(Any, arr), mode="L").save(path)
    return path


def _image() -> ImageInput:
    return ImageInput(image_id=100)


def _regions() -> list[dict[str, Any]]:
    return [{"toothCode": "16", "bbox": [150, 80, 230, 155], "score": 0.88, "regionIndex": 0}]


def _detections() -> list[ToothDetection]:
    return [ToothDetection(image_id=100, tooth_code="16", bbox=[120, 70, 240, 170], detection_score=0.9)]


def test_disabled_grading_module_fails_explicitly() -> None:
    _, _, pipeline = _runtime(_settings(ai_runtime_mode="mock"))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.grade(_image(), None, _regions(), _detections())

    assert exc_info.value.code == "M5007"
    assert pipeline.get_last_impl_type() == "DISABLED"


def test_enabled_grading_module_uses_external_assets(sample_image: Path) -> None:
    assets, _, pipeline = _runtime(_settings(model_grading_enabled=True))

    result = pipeline.grade(_image(), sample_image, _regions(), _detections())

    assert result.grading_mode == "real"
    assert result.grading_impl_type == "HEURISTIC"
    assert result.grading_label in {"C0", "C1", "C2", "C3"}
    assert result.raw_result["manifestPath"] == str(assets.grading_manifest.manifest_path)
    assert result.raw_result["classMapPath"] == str(assets.class_map_path)
    assert isinstance(result.raw_result["lesionGrades"], list)


def test_invalid_real_grading_label_raises_explicitly(sample_image: Path, monkeypatch) -> None:
    _, registry, pipeline = _runtime(_settings(ai_runtime_mode="real", model_grading_enabled=True))
    adapter = registry.get_grading_model()

    def invalid_label(*_args, **_kwargs):
        return {
            "gradingLabel": "UNKNOWN",
            "confidenceScore": 0.72,
            "uncertaintyScore": 0.18,
            "implType": "HEURISTIC",
            "rawResult": {"classMargin": 0.08},
        }

    monkeypatch.setattr(adapter, "infer", invalid_label)

    with pytest.raises(BusinessException) as exc_info:
        pipeline.grade(_image(), sample_image, _regions(), _detections())

    assert exc_info.value.code == "M5024"


def test_force_fail_raises_grading_business_error(sample_image: Path) -> None:
    _, _, pipeline = _runtime(_settings(ai_runtime_mode="real", model_grading_enabled=True, grading_force_fail=True))

    with pytest.raises(BusinessException) as exc_info:
        pipeline.grade(_image(), sample_image, _regions(), _detections())

    assert exc_info.value.code == "M5010"
