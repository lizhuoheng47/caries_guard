from pathlib import Path
from unittest.mock import MagicMock

import numpy as np
import pytest
from PIL import Image

from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.services.image_fetch_service import FetchedImage, ImageFetchService
from app.services.analysis_knowledge_service import AnalysisKnowledgeService
from app.services.qwen_vision_service import (
    QwenVisionService,
    VisionAnalysisResult,
    VisionFinding,
    VisionRenderResult,
)
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


class FakeQwenVisionService(QwenVisionService):
    def __init__(self, settings: Settings) -> None:
        super().__init__(settings)

    def is_enabled(self) -> bool:
        return True

    def analyze(
        self,
        image_path: Path,
        image_id: int | None,
        tooth_detections,
    ) -> VisionAnalysisResult:
        return VisionAnalysisResult(
            image_id=image_id,
            image_width=512,
            image_height=256,
            overall_severity_code="C2",
            overall_confidence_score=0.88,
            overall_uncertainty_score=0.12,
            clinical_summary="Occlusal enamel and dentin lesion is visible on tooth 16.",
            treatment_plan=[
                {
                    "priority": "HIGH",
                    "title": "Restorative treatment",
                    "details": "Schedule prompt in-clinic restoration after dentist confirmation.",
                }
            ],
            findings=[
                VisionFinding(
                    tooth_code="16",
                    severity_code="C2",
                    confidence_score=0.88,
                    uncertainty_score=0.12,
                    lesion_area_ratio=0.023,
                    lesion_area_px=3015,
                    bbox=[150, 80, 235, 150],
                    polygon=[[150, 80], [235, 80], [235, 150], [150, 150]],
                    summary="Approximate dentin-level lesion on tooth 16.",
                    treatment_suggestion="Composite restoration after clinical confirmation.",
                )
            ],
            raw_result={
                "provider": "QWEN",
                "model": "qwen3-vl-plus",
                "imageWidth": 512,
                "imageHeight": 256,
                "finishReason": "stop",
            },
        )

    def render_visual_assets(
        self,
        image_path: Path,
        result: VisionAnalysisResult,
        output_dir: Path,
    ) -> VisionRenderResult:
        output_dir.mkdir(parents=True, exist_ok=True)
        mask_path = output_dir / "mask_qwen.png"
        overlay_path = output_dir / "overlay_qwen.png"
        heatmap_path = output_dir / "heatmap_qwen.png"
        Image.new("L", (512, 256), color=128).save(mask_path)
        Image.new("RGB", (512, 256), color=(240, 240, 240)).save(overlay_path)
        Image.new("RGB", (512, 256), color=(255, 210, 180)).save(heatmap_path)
        return VisionRenderResult(mask_path=mask_path, overlay_path=overlay_path, heatmap_path=heatmap_path)


class FakeAnalysisKnowledgeService(AnalysisKnowledgeService):
    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def is_enabled(self) -> bool:
        return True

    def generate_guidance(self, task, vision_result, risk_assessment):
        return {
            "question": "Provide grounded follow-up advice.",
            "answer": "Recommend restorative consultation and 3-month follow-up after dentist confirmation.",
            "knowledgeVersion": "v1.0",
            "confidence": 0.81,
            "safetyFlags": [],
            "refusalReason": None,
            "citations": [
                {
                    "documentCode": "GUIDE-001",
                    "docTitle": "Caries treatment guide",
                    "chunkText": "Moderate lesions should be clinically confirmed and treated promptly.",
                    "sourceUri": "kb://guide-001",
                }
            ],
        }


def _settings(tmp_path: Path, **overrides) -> Settings:
    values = {
        "ai_runtime_mode": "hybrid",
        "llm_provider_code": "MOCK",
        "model_quality_enabled": False,
        "model_tooth_detect_enabled": False,
        "model_segmentation_enabled": False,
        "model_grading_enabled": False,
        "qwen_vision_enabled": True,
        "analysis_kb_enhancement_enabled": True,
        "qwen_vision_model": "qwen3-vl-plus",
        "temp_dir": str(tmp_path / "work"),
        "download_images": True,
    }
    values.update(overrides)
    return Settings(**values)


def _task_payload() -> dict:
    return {
        "taskNo": "TASK-QWEN-001",
        "traceId": "trace-qwen",
        "caseNo": "CASE-QWEN-001",
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
    path = tmp_path / "image.png"
    Image.fromarray(arr, mode="L").save(path)
    return path


def test_pipeline_uses_qwen_vision_result_for_annotations(tmp_path: Path):
    settings = _settings(tmp_path)
    registry = ModelRegistry(settings)
    registry.startup()

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
        quality_pipeline=QualityPipeline(registry, settings),
        detection_pipeline=DetectionPipeline(registry, settings),
        segmentation_pipeline=None,
        grading_pipeline=GradingPipeline(registry, settings),
        qwen_vision_service=FakeQwenVisionService(settings),
        analysis_knowledge_service=FakeAnalysisKnowledgeService(settings),
    )

    result = pipeline.run(_task_payload())
    raw = result["rawResultJson"]

    assert result["summary"]["overallHighestSeverity"] == "C2"
    assert raw["annotationProvider"] == "QWEN"
    assert raw["annotationModel"] == "qwen3-vl-plus"
    assert raw["annotationImageWidth"] == 512
    assert raw["annotationImageHeight"] == 256
    assert raw["segmentationImplType"] == "VLM_API"
    assert raw["gradingImplType"] == "VLM_API"
    assert raw["clinicalSummary"] == "Occlusal enamel and dentin lesion is visible on tooth 16."
    assert raw["treatmentPlan"][0]["priority"] == "HIGH"
    assert raw["followUpRecommendation"] == "Recommend restorative consultation and 3-month follow-up after dentist confirmation."
    assert raw["knowledgeAdvice"]["knowledgeVersion"] == "v1.0"
    assert raw["citations"][0]["documentCode"] == "GUIDE-001"
    assert raw["lesionCount"] == 1
    assert raw["abnormalToothCount"] == 1
    assert raw["lesionResults"][0]["severityCode"] == "C2"
    assert raw["lesionResults"][0]["bbox"] == [150, 80, 235, 150]
    assert raw["lesionResults"][0]["polygon"][0] == [150, 80]
    assert raw["lesionResults"][0]["lesionAreaRatio"] == 0.023
    assert raw["qwenVisionRawResult"]["provider"] == "QWEN"
    assert raw["qwenVisionRawResult"]["imageWidth"] == 512
    assert [item["assetTypeCode"] for item in result["visualAssets"]] == ["MASK", "OVERLAY", "HEATMAP"]


def test_qwen_vision_rejects_missing_geometry_in_real_mode(tmp_path: Path):
    service = QwenVisionService(
        _settings(
            tmp_path,
            ai_runtime_mode="real",
            llm_provider_code="OPENAI_COMPATIBLE",
            llm_base_url="http://llm.local/v1",
            llm_api_key="test-key",
            rag_embedding_provider="OPENAI_COMPATIBLE",
            rag_embedding_base_url="http://embedding.local/v1",
            rag_embedding_api_key="embed-key",
            rag_vector_store_type="LOCAL_JSON",
            qwen_vision_base_url="http://qwen.local/v1",
            qwen_vision_api_key="qwen-key",
        )
    )

    with pytest.raises(RuntimeError, match="missing a valid bbox or polygon"):
        service._normalize_findings(
            {
                "findings": [
                    {
                        "toothCode": "16",
                        "severityCode": "C2",
                    }
                ]
            },
            512,
            256,
        )
