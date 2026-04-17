from app.core.config import Settings
from app.pipelines.inference_pipeline import InferencePipeline
from app.schemas.callback import VisualAsset


def _pipeline(mode: str) -> InferencePipeline:
    return InferencePipeline(
        settings=Settings(callback_visual_asset_mode=mode),
        image_fetch_service=None,
        visual_asset_service=None,
        risk_service=None,
        model_registry=None,
        quality_pipeline=None,
        detection_pipeline=None,
    )


def _asset() -> VisualAsset:
    return VisualAsset(
        asset_type_code="MASK",
        bucket_name="caries-visual",
        object_key="visual/2026/04/15/CASE/mask_90001_16.png",
        content_type="image/png",
        related_image_id=90001,
        tooth_code="16",
        file_size_bytes=8,
        md5="abc",
    )


def test_metadata_mode_keeps_top_level_visual_assets() -> None:
    assets = [_asset()]

    assert _pipeline("metadata")._callback_visual_assets(assets, "TASK1", "trace-1") == assets


def test_legacy_empty_mode_suppresses_top_level_visual_assets() -> None:
    assets = [_asset()]

    assert _pipeline("legacy-empty")._callback_visual_assets(assets, "TASK1", "trace-1") == []
