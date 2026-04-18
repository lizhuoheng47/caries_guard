"""Integration-level tests for Phase 5B segmentation visual assets."""

from pathlib import Path
from unittest.mock import MagicMock

import numpy as np
from PIL import Image

from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.services.image_fetch_service import FetchedImage, ImageFetchService
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService


class FakeStorage:
    def upload_file(self, bucket_name: str, object_key: str, local_path: Path, content_type: str):
        class Uploaded:
            size = Path(local_path).stat().st_size
            file_name = Path(local_path).name

        uploaded = Uploaded()
        uploaded.bucket_name = bucket_name
        uploaded.object_key = object_key
        uploaded.content_type = content_type
        return uploaded


def _settings(tmp_path: Path, **overrides) -> Settings:
    values = {
        "ai_runtime_mode": "hybrid",
        "model_quality_enabled": False,
        "model_tooth_detect_enabled": True,
        "model_segmentation_enabled": True,
        "model_grading_enabled": True,
        "model_confidence_threshold": 0.3,
        "download_images": True,
        "temp_dir": str(tmp_path / "work"),
    }
    mapping = {
        "CG_AI_RUNTIME_MODE": "ai_runtime_mode",
        "CG_MODEL_QUALITY_ENABLED": "model_quality_enabled",
        "CG_MODEL_TOOTH_DETECT_ENABLED": "model_tooth_detect_enabled",
        "CG_MODEL_SEGMENTATION_ENABLED": "model_segmentation_enabled",
        "CG_MODEL_GRADING_ENABLED": "model_grading_enabled",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "model_confidence_threshold",
        "CG_AI_DOWNLOAD_IMAGES": "download_images",
        "CG_TEMP_DIR": "temp_dir",
    }
    for key, value in overrides.items():
        target = mapping.get(key, key)
        if target in {
            "model_quality_enabled",
            "model_tooth_detect_enabled",
            "model_segmentation_enabled",
            "model_grading_enabled",
            "download_images",
        }:
            values[target] = str(value).lower() == "true"
        elif target == "model_confidence_threshold":
            values[target] = float(value)
        else:
            values[target] = value
    return Settings(**values)


def _task_payload() -> dict:
    return {
        "taskNo": "TASK-P5B-001",
        "traceId": "trace-p5b",
        "caseNo": "CASE-P5B-001",
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


def _sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "image.png"
    img.save(path)
    return path


def test_phase5b_pipeline_uploads_segmentation_visual_assets(tmp_path: Path):
    settings = _settings(tmp_path)
    registry = ModelRegistry(settings)
    registry.startup()
    quality_pipeline = QualityPipeline(registry, settings)
    detection_pipeline = DetectionPipeline(registry, settings)
    segmentation_pipeline = SegmentationPipeline(registry, settings)
    grading_pipeline = GradingPipeline(registry, settings)

    image_path = _sample_image(tmp_path)
    mock_fetch = MagicMock(spec=ImageFetchService)
    mock_fetch.download.return_value = FetchedImage(
        image_id=100,
        image_type_code="BITEWING",
        path=image_path,
        size_bytes=image_path.stat().st_size,
        source="test",
    )

    pipeline = InferencePipeline(
        settings=settings,
        image_fetch_service=mock_fetch,
        visual_asset_service=VisualAssetService(settings, FakeStorage()),
        risk_service=RiskService(settings),
        model_registry=registry,
        quality_pipeline=quality_pipeline,
        detection_pipeline=detection_pipeline,
        segmentation_pipeline=segmentation_pipeline,
        grading_pipeline=grading_pipeline,
    )

    result = pipeline.run(_task_payload())
    raw = result["rawResultJson"]
    visual_assets = result["visualAssets"]

    assert raw["pipelineVersion"] == "phase5d-1"
    assert raw["segmentationMode"] == "real"
    assert raw["segmentationImplType"] == "HEURISTIC"
    assert raw["gradingMode"] == "real"
    assert raw["gradingImplType"] == "HEURISTIC"
    assert raw["uncertaintyMode"] == "real"
    assert raw["uncertaintyImplType"] == "HEURISTIC"
    assert raw["gradingLabel"] in {"C0", "C1", "C2", "C3"}
    assert raw["uncertaintyScore"] == result["uncertaintyScore"]
    assert raw["needsReview"] == (raw["uncertaintyScore"] >= settings.uncertainty_review_threshold)
    assert raw["riskMode"] == "mock"
    assert raw["riskImplType"] == "MOCK"
    assert raw["riskRawResult"]["gradingLabel"] == raw["gradingLabel"]
    assert raw["riskLevel"] in {"LOW", "MEDIUM", "HIGH"}
    assert raw["knowledgeVersion"] == settings.rag_knowledge_version
    assert isinstance(raw["evidenceRefs"], list)
    assert len(raw["segmentationRegions"]) >= 1
    assert [item["assetTypeCode"] for item in visual_assets] == ["MASK", "OVERLAY", "HEATMAP"]
    for item in visual_assets:
        assert item["bucketName"] == "caries-visual"
        assert item["objectKey"].startswith("org/1/case/CASE-P5B-001/analysis/TASK-P5B-001/")
        assert item["contentType"] == "image/png"
        assert item["fileSizeBytes"] > 0
        assert item["md5"]
