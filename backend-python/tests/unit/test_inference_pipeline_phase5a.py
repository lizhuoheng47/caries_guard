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


class _FakeRepo:
    def __init__(self) -> None:
        self.job = {"id": 7, "job_no": "AIJOB-TEST"}
        self.finished: list[dict] = []

    def get_latest_infer_job(self, *_args, **_kwargs):
        return None

    def create_infer_job(self, *_args, **_kwargs):
        return dict(self.job)

    def finish_infer_job(self, job_id: int, status_code: str, **kwargs):
        self.finished.append({"jobId": job_id, "statusCode": status_code, **kwargs})
        return {"id": job_id, "status_code": status_code}


def _settings() -> Settings:
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
        strict_model_startup_validation=False,
    )


def test_failure_payload_uses_current_analysis_v2_contract() -> None:
    settings = _settings()
    model_assets = ModelAssets(settings)
    repo = _FakeRepo()
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
        ai_runtime_repository=repo,
        analysis_asset_service=AnalysisAssetService(settings, model_assets),
    )

    payload = pipeline.build_failure_payload(
        {"taskNo": "TASK-FAIL-001", "modelVersion": "caries-v1"},
        AnalysisRuntimeException(
            "M5101",
            "real inference assets are not ready",
            missing_items=[
                {
                    "moduleName": "segmentation",
                    "requirement": "checkpoint",
                    "message": "segmentation checkpoint is missing",
                }
            ],
        ),
    )

    assert payload["taskStatusCode"] == "FAILED"
    assert payload["rawResultJson"]["pipelineVersion"] == "analysis-v2"
    assert payload["rawResultJson"]["errorCode"] == "M5101"
    assert payload["rawResultJson"]["missingItems"][0]["moduleName"] == "segmentation"
    assert repo.finished[-1]["statusCode"] == "FAILED"
