from types import SimpleNamespace

from app.core.config import Settings
from app.core.exceptions import AnalysisRuntimeException
from app.infra.model.model_assets import ModelAssets
from app.pipelines.inference_pipeline import InferencePipeline
from app.services.analysis_asset_service import AnalysisAssetService


class _FakeRegistry:
    def is_module_loaded(self, _module: str) -> bool:
        return False

    def get_module_error(self, module: str) -> str | None:
        if module == "segmentation":
            return "M5006: segmentation checkpoint does not exist"
        return None


class _FakeStage:
    def __init__(self, impl_type: str) -> None:
        self._impl_type = impl_type

    def get_last_impl_type(self) -> str:
        return self._impl_type


def test_failure_payload_exposes_segmentation_manifest_paths() -> None:
    settings = Settings(
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
        strict_model_startup_validation=False,
    )
    model_assets = ModelAssets(settings)
    pipeline = InferencePipeline(
        settings=settings,
        image_fetch_service=SimpleNamespace(),
        visual_asset_service=None,
        model_registry=_FakeRegistry(),
        model_assets=model_assets,
        quality_pipeline=_FakeStage("HEURISTIC"),
        detection_pipeline=_FakeStage("HEURISTIC"),
        segmentation_pipeline=_FakeStage("HEURISTIC"),
        grading_pipeline=_FakeStage("HEURISTIC"),
        risk_service=SimpleNamespace(),
        analysis_asset_service=AnalysisAssetService(settings, model_assets),
    )

    payload = pipeline.build_failure_payload(
        {"taskNo": "TASK-FAIL-SEG"},
        AnalysisRuntimeException("M5101", "real inference assets are not ready"),
    )

    module_status = payload["rawResultJson"]["moduleStatus"]["segmentation"]
    assert module_status["manifestPath"] == str(model_assets.segmentation_manifest.manifest_path)
    assert module_status["checkpointPath"] == str(model_assets.segmentation_manifest.checkpoint_path)
    assert payload["rawResultJson"]["segmentationImplType"] == "HEURISTIC"
