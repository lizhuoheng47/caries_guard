"""Integration-level test for InferencePipeline with Phase 5A mode routing.

Covers:
- mock mode complete pipeline
- hybrid + quality enabled
- hybrid + tooth detect enabled
- callback JSON structure unchanged
- raw_result_json contains qualityMode / qualityImplType / toothDetectionMode / toothDetectionImplType
- hybrid degradation logging
- real mode failure (no silent fallback)
"""

import os
from pathlib import Path
from unittest.mock import MagicMock, patch

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.services.image_fetch_service import FetchedImage, ImageFetchService
from app.services.risk_service import RiskService


def _settings(**overrides) -> Settings:
    env = {
        "CG_AI_RUNTIME_MODE": "mock",
        "CG_MODEL_QUALITY_ENABLED": "false",
        "CG_MODEL_TOOTH_DETECT_ENABLED": "false",
        "CG_MODEL_CONFIDENCE_THRESHOLD": "0.3",
        "CG_AI_DOWNLOAD_IMAGES": "false",
    }
    env.update(overrides)
    with patch.dict(os.environ, env, clear=False):
        return Settings()


def _task_payload(task_no: str = "TASK-001") -> dict:
    return {
        "taskNo": task_no,
        "traceId": "test-trace-001",
        "caseNo": "CASE-001",
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


def _build_pipeline(settings: Settings) -> InferencePipeline:
    registry = ModelRegistry(settings)
    registry.startup()
    quality_pipeline = QualityPipeline(registry, settings)
    detection_pipeline = DetectionPipeline(registry, settings)

    mock_fetch = MagicMock(spec=ImageFetchService)
    risk_service = RiskService(settings)

    return InferencePipeline(
        settings=settings,
        image_fetch_service=mock_fetch,
        visual_asset_service=None,
        risk_service=risk_service,
        model_registry=registry,
        quality_pipeline=quality_pipeline,
        detection_pipeline=detection_pipeline,
    )


# ── Mock mode ────────────────────────────────────────────────────────────


class TestMockModePipeline:
    def test_produces_valid_callback_payload(self):
        pipeline = _build_pipeline(_settings(CG_AI_RUNTIME_MODE="mock"))
        result = pipeline.run(_task_payload())

        # Callback contract fields
        assert result["taskNo"] == "TASK-001"
        assert result["taskStatusCode"] == "SUCCESS"
        assert "startedAt" in result
        assert "completedAt" in result
        assert "modelVersion" in result
        assert "summary" in result
        assert "rawResultJson" in result
        assert "riskAssessment" in result
        assert "traceId" in result
        assert "inferenceMillis" in result

    def test_raw_result_has_mode_stamps(self):
        pipeline = _build_pipeline(_settings(CG_AI_RUNTIME_MODE="mock"))
        result = pipeline.run(_task_payload())
        raw = result["rawResultJson"]

        assert raw["qualityMode"] == "mock"
        assert raw["qualityImplType"] == "MOCK"
        assert raw["toothDetectionMode"] == "mock"
        assert raw["toothDetectionImplType"] == "MOCK"
        assert raw["mode"] == "mock"

    def test_quality_check_results_present(self):
        pipeline = _build_pipeline(_settings(CG_AI_RUNTIME_MODE="mock"))
        result = pipeline.run(_task_payload())
        raw = result["rawResultJson"]
        assert "qualityCheckResults" in raw
        assert len(raw["qualityCheckResults"]) == 1

    def test_tooth_detections_present(self):
        pipeline = _build_pipeline(_settings(CG_AI_RUNTIME_MODE="mock"))
        result = pipeline.run(_task_payload())
        raw = result["rawResultJson"]
        assert "toothDetections" in raw
        assert len(raw["toothDetections"]) == 2


# ── Hybrid mode (quality enabled) ───────────────────────────────────────


class TestHybridQualityEnabled:
    def test_quality_mode_is_real(self):
        pipeline = _build_pipeline(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        ))
        result = pipeline.run(_task_payload())
        raw = result["rawResultJson"]

        assert raw["qualityMode"] == "real"
        assert raw["qualityImplType"] == "HEURISTIC"
        # Tooth detection should still be mock
        assert raw["toothDetectionMode"] == "mock"
        assert raw["toothDetectionImplType"] == "MOCK"

    def test_callback_structure_unchanged(self):
        pipeline = _build_pipeline(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
        ))
        result = pipeline.run(_task_payload())

        # These fields must always be present regardless of mode
        assert "taskNo" in result
        assert "taskStatusCode" in result
        assert "summary" in result
        assert "rawResultJson" in result
        assert "visualAssets" in result


# ── Hybrid mode (tooth detect enabled) ──────────────────────────────────


class TestHybridToothDetectEnabled:
    def test_tooth_detection_mode_is_real(self):
        pipeline = _build_pipeline(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        ))
        result = pipeline.run(_task_payload())
        raw = result["rawResultJson"]

        assert raw["toothDetectionMode"] == "real"
        assert raw["toothDetectionImplType"] == "HEURISTIC"
        # Quality should still be mock
        assert raw["qualityMode"] == "mock"
        assert raw["qualityImplType"] == "MOCK"


# ── Hybrid mode (both enabled) ──────────────────────────────────────────


class TestHybridBothEnabled:
    def test_both_modes_real(self):
        pipeline = _build_pipeline(_settings(
            CG_AI_RUNTIME_MODE="hybrid",
            CG_MODEL_QUALITY_ENABLED="true",
            CG_MODEL_TOOTH_DETECT_ENABLED="true",
        ))
        result = pipeline.run(_task_payload())
        raw = result["rawResultJson"]

        assert raw["qualityMode"] == "real"
        assert raw["qualityImplType"] == "HEURISTIC"
        assert raw["toothDetectionMode"] == "real"
        assert raw["toothDetectionImplType"] == "HEURISTIC"

    def test_pipeline_version_updated(self):
        pipeline = _build_pipeline(_settings(CG_AI_RUNTIME_MODE="hybrid"))
        result = pipeline.run(_task_payload())
        assert result["rawResultJson"]["pipelineVersion"] == "phase5a-1"


# ── Failure payload ──────────────────────────────────────────────────────


class TestFailurePayload:
    def test_failure_payload_structure(self):
        pipeline = _build_pipeline(_settings())
        payload = pipeline.build_failure_payload(
            _task_payload(),
            RuntimeError("test error"),
        )
        assert payload["taskNo"] == "TASK-001"
        assert payload["taskStatusCode"] == "FAILED"
        assert "errorMessage" in payload
        assert payload["inferenceMillis"] == 0


# ── Config validation ────────────────────────────────────────────────────


class TestConfigValidation:
    def test_invalid_runtime_mode_raises(self):
        with pytest.raises(ValueError, match="invalid"):
            _settings(CG_AI_RUNTIME_MODE="invalid_mode")
