from dataclasses import replace

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.services.analysis_asset_service import AnalysisAssetService


class LoadedRegistry:
    def is_module_loaded(self, _module_name: str) -> bool:
        return True

    def get_module_error(self, _module_name: str) -> None:
        return None


def test_heuristic_modules_do_not_require_ml_checkpoints() -> None:
    settings = replace(
        Settings(),
        ai_runtime_mode="hybrid",
        model_tooth_detect_enabled=True,
        model_tooth_detect_impl_type="HEURISTIC",
        model_grading_enabled=True,
        model_grading_impl_type="HEURISTIC",
    )
    service = AnalysisAssetService(settings, ModelAssets(settings))

    snapshot = service.runtime_snapshot(LoadedRegistry())

    assert snapshot.modules["tooth_detect"].ready is True
    assert snapshot.modules["tooth_detect"].missing_items == []
    assert snapshot.modules["grading"].ready is True
    assert snapshot.modules["grading"].missing_items == []


def test_ml_grading_still_requires_runnable_manifest_and_checkpoint() -> None:
    settings = replace(
        Settings(),
        ai_runtime_mode="hybrid",
        model_grading_enabled=True,
        model_grading_impl_type="ML_MODEL",
    )
    service = AnalysisAssetService(settings, ModelAssets(settings))

    status = service.runtime_snapshot(LoadedRegistry()).modules["grading"]
    requirements = {item.requirement for item in status.missing_items}

    assert status.ready is False
    assert "checkpoint" in requirements
    assert "manifestStatus" in requirements
