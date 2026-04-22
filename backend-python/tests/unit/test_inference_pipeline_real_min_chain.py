from pathlib import Path
import importlib.util
from typing import Any, cast
from unittest.mock import MagicMock

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import AnalysisRuntimeException
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.services.image_fetch_service import FetchedImage, ImageFetchService
from app.services.risk_service import RiskService


def _settings(tmp_path: Path) -> Settings:
    repo_root = Path(__file__).resolve().parents[3]
    return Settings(
        ai_runtime_mode="real",
        rag_runtime_enabled=False,
        analysis_kb_enhancement_enabled=False,
        model_quality_enabled=True,
        model_tooth_detect_enabled=True,
        model_segmentation_enabled=True,
        model_grading_enabled=True,
        model_risk_enabled=False,
        model_quality_impl_type="HEURISTIC",
        model_tooth_detect_impl_type="HEURISTIC",
        model_segmentation_impl_type="HEURISTIC",
        model_grading_impl_type="HEURISTIC",
        model_risk_impl_type="HEURISTIC",
        model_weights_dir=str(repo_root / "model-weights"),
        quality_model_param_path=str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        quality_model_weights_path=str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        download_images=True,
        temp_dir=str(tmp_path / "work"),
        strict_model_startup_validation=False,
    )


def _sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
    path = tmp_path / "image.png"
    Image.fromarray(cast(Any, arr), mode="L").save(path)
    return path


def test_real_pipeline_fails_fast_when_manifest_backed_assets_are_not_ready(tmp_path: Path) -> None:
    if importlib.util.find_spec("cv2") is None:
        pytest.skip("opencv-python-headless is not installed in this environment")
    settings = _settings(tmp_path)
    model_assets = ModelAssets(settings)
    registry = ModelRegistry(settings, model_assets)
    registry.startup()

    image_path = _sample_image(tmp_path)
    mock_fetch = MagicMock(spec=ImageFetchService)
    mock_fetch.download.return_value = FetchedImage(
        image_id=100,
        image_type_code="BITEWING",
        path=image_path,
        size_bytes=image_path.stat().st_size,
        source="test",
        bucket_name="caries-image",
        object_key="test/img.jpg",
    )

    pipeline = InferencePipeline(
        settings=settings,
        image_fetch_service=mock_fetch,
        visual_asset_service=None,
        model_registry=registry,
        model_assets=model_assets,
        quality_pipeline=QualityPipeline(registry, settings),
        detection_pipeline=DetectionPipeline(registry, settings),
        segmentation_pipeline=SegmentationPipeline(registry, settings, model_assets),
        grading_pipeline=GradingPipeline(registry, settings, model_assets),
        risk_service=RiskService(settings),
    )

    with pytest.raises(AnalysisRuntimeException) as exc_info:
        pipeline.run(
            {
                "taskNo": "TASK-REAL-001",
                "traceId": "trace-real-001",
                "caseNo": "CASE-REAL-001",
                "orgId": 1,
                "images": [
                    {
                        "imageId": 100,
                        "imageTypeCode": "BITEWING",
                        "bucketName": "caries-image",
                        "objectKey": "test/img.jpg",
                    }
                ],
            }
        )

    assert exc_info.value.code == "M5101"
