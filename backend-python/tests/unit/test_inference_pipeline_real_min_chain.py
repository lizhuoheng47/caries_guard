from pathlib import Path
from typing import Any, cast
from unittest.mock import MagicMock

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.risk_pipeline import RiskPipeline
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


class FakeAiRuntimeRepository:
    def __init__(self):
        self.jobs = []
        self.images = []
        self.artifacts = []
        self.finished = []

    def create_infer_job(self, java_task_no: str, model_version: str, **kwargs):
        self.jobs.append((java_task_no, model_version, kwargs))
        return {"id": 901, "job_no": "AIJOB-REAL-001"}

    def add_job_image(self, job_id: int, **fields):
        self.images.append((job_id, fields))
        return {"id": len(self.images), "job_id": job_id}

    def add_artifact(self, job_id: int, **fields):
        self.artifacts.append((job_id, fields))
        return {"id": len(self.artifacts), "job_id": job_id}

    def finish_infer_job(self, job_id: int, status_code: str, **kwargs):
        self.finished.append((job_id, status_code, kwargs))
        return {"id": job_id, "job_no": "AIJOB-REAL-001", "status_code": status_code}


def _settings(tmp_path: Path, **overrides) -> Settings:
    values = {
        "ai_runtime_mode": "real",
        "rag_runtime_enabled": False,
        "qwen_vision_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "download_images": True,
        "temp_dir": str(tmp_path / "work"),
    }
    values.update(overrides)
    return Settings(**values)


def _sample_image(tmp_path: Path) -> Path:
    arr = np.random.randint(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
    path = tmp_path / "image.png"
    Image.fromarray(cast(Any, arr), mode="L").save(path)
    return path


def _task_payload() -> dict:
    return {
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


def _build_pipeline(settings: Settings, image_path: Path, with_segmentation: bool = True) -> InferencePipeline:
    registry = ModelRegistry(settings)
    registry.startup()
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
    segmentation_pipeline = SegmentationPipeline(registry, settings) if with_segmentation else None
    return InferencePipeline(
        settings=settings,
        image_fetch_service=mock_fetch,
        visual_asset_service=VisualAssetService(settings, cast(Any, FakeStorage())),
        risk_service=RiskService(settings),
        model_registry=registry,
        quality_pipeline=QualityPipeline(registry, settings),
        detection_pipeline=DetectionPipeline(registry, settings),
        segmentation_pipeline=segmentation_pipeline,
        grading_pipeline=GradingPipeline(registry, settings),
        risk_pipeline=RiskPipeline(registry, settings),
        ai_runtime_repository=cast(Any, FakeAiRuntimeRepository()),
    )


def test_real_mode_pipeline_has_minimum_real_chain_fields(tmp_path: Path):
    settings = _settings(tmp_path)
    pipeline = _build_pipeline(settings, _sample_image(tmp_path), with_segmentation=True)

    result = pipeline.run(_task_payload())
    raw = result["rawResultJson"]
    repo = cast(FakeAiRuntimeRepository, pipeline.ai_runtime_repository)

    assert result["taskStatusCode"] == "SUCCESS"
    assert raw["mode"] == "real"
    assert raw["pipelineVersion"] == "phase5d-1"
    assert raw["imageType"] == "BITEWING"
    assert raw["imageTypeRoute"] == "INTRAORAL_XRAY"
    assert isinstance(raw["quality"], dict)
    assert raw["quality"]["qualityStatus"] in {"PASS", "WARN", "FAIL"}
    assert isinstance(raw["quality"]["qualityScore"], float)
    assert isinstance(raw["quality"]["qualityIssues"], list)
    assert "retakeSuggested" in raw["quality"]
    assert "implType" in raw["quality"]
    assert "modelVersion" in raw["quality"]
    assert "inferenceMillis" in raw["quality"]
    assert isinstance(raw["teeth"], dict)
    assert "gradingLabel" in raw
    assert "confidenceScore" in raw
    assert "uncertaintyScore" in raw
    assert isinstance(raw["uncertaintyReasons"], list)
    assert "needsReview" in raw
    assert isinstance(raw["visualAssets"], list)
    assert len(raw["visualAssets"]) >= 1
    assert raw["qualityMode"] == "real"
    assert raw["toothDetectionMode"] == "real"
    assert len(raw["lesionResults"]) >= 1
    assert len(raw["toothResults"]) >= 1
    assert len(raw["imageResults"]) >= 1
    assert result["gradingLabel"] == raw["gradingLabel"]
    assert result["confidenceScore"] == raw["confidenceScore"]
    assert result["needsReview"] == raw["needsReview"]
    assert repo.finished[0][2]["result_json"]["gradingLabel"] == raw["gradingLabel"]

    image_result = repo.images[0][1]["result_json"]
    assert image_result["gradingMode"] == raw["gradingMode"]
    assert image_result["gradingImplType"] == raw["gradingImplType"]
    assert image_result["gradingLabel"] == raw["gradingLabel"]
    assert image_result["confidenceScore"] == raw["confidenceScore"]
    assert image_result["uncertaintyMode"] == raw["uncertaintyMode"]
    assert image_result["uncertaintyImplType"] == raw["uncertaintyImplType"]
    assert image_result["uncertaintyScore"] == raw["uncertaintyScore"]
    assert image_result["needsReview"] == raw["needsReview"]


def test_real_mode_high_uncertainty_triggers_needs_review(tmp_path: Path, monkeypatch):
    settings = _settings(tmp_path, uncertainty_review_threshold=0.35)
    pipeline = _build_pipeline(settings, _sample_image(tmp_path), with_segmentation=True)
    adapter = pipeline.model_registry.get_grading_model()

    def unstable_infer(*_args, **_kwargs):
        return {
            "gradingLabel": "C3",
            "confidenceScore": 0.41,
            "uncertaintyScore": 0.66,
            "implType": "HEURISTIC",
            "rawResult": {"classMargin": 0.01},
        }

    monkeypatch.setattr(adapter, "infer", unstable_infer)
    result = pipeline.run(_task_payload())
    raw = result["rawResultJson"]

    assert raw["gradingLabel"] == "C3"
    assert raw["needsReview"] is True
    assert result["needsReview"] is True
    assert raw["uncertaintyScore"] >= settings.uncertainty_review_threshold
    assert "UNCERTAINTY_THRESHOLD_EXCEEDED" in raw["uncertaintyReasons"]
    assert raw["imageResults"][0]["needsReview"] is True
    assert raw["imageResults"][0]["uncertaintyScore"] == raw["uncertaintyScore"]
    assert raw["imageResults"][0]["gradingLabel"] == raw["gradingLabel"]


def test_real_mode_requires_segmentation_pipeline(tmp_path: Path):
    settings = _settings(tmp_path)
    pipeline = _build_pipeline(settings, _sample_image(tmp_path), with_segmentation=False)

    with pytest.raises(BusinessException) as exc_info:
        pipeline.run(_task_payload())
    assert exc_info.value.code == "M5016"
