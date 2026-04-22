from pathlib import Path

import pytest
from PIL import Image

from app.core.config import Settings
from app.services.qwen_vision_service import QwenVisionService, VisionAnalysisResult, VisionFinding


def _settings(tmp_path: Path, **overrides) -> Settings:
    values = {
        "ai_runtime_mode": "real",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "qwen_vision_enabled": True,
        "qwen_vision_model": "qwen3-vl-plus",
        "qwen_vision_base_url": "http://qwen.local/v1",
        "qwen_vision_api_key": "qwen-key",
        "temp_dir": str(tmp_path / "work"),
    }
    values.update(overrides)
    return Settings(**values)


def test_qwen_vision_rejects_missing_geometry_in_real_mode(tmp_path: Path):
    service = QwenVisionService(_settings(tmp_path))

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


def test_qwen_vision_renders_visual_assets(tmp_path: Path):
    service = QwenVisionService(_settings(tmp_path))
    image_path = tmp_path / "image.png"
    Image.new("L", (512, 256), color=128).save(image_path)
    result = VisionAnalysisResult(
        image_id=100,
        image_width=512,
        image_height=256,
        overall_severity_code="C2",
        overall_confidence_score=0.88,
        overall_uncertainty_score=0.12,
        clinical_summary="Visible lesion on tooth 16.",
        treatment_plan=[],
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
                summary="Approximate dentin-level lesion.",
                treatment_suggestion="Composite restoration after confirmation.",
            )
        ],
        raw_result={},
    )

    rendered = service.render_visual_assets(image_path, result, tmp_path / "visual")

    assert rendered.mask_path.exists()
    assert rendered.overlay_path.exists()
    assert rendered.heatmap_path.exists()
