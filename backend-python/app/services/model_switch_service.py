"""Runtime model status query service for API and health-check consumers."""

from __future__ import annotations

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.schemas.base import dump_camel
from app.services.analysis_asset_service import AnalysisAssetService


class ModelSwitchService:
    """Exposes the current runtime mode and per-module adapter status."""

    def __init__(
        self,
        registry: ModelRegistry,
        settings: Settings,
        model_assets: ModelAssets,
        analysis_asset_service: AnalysisAssetService,
    ) -> None:
        self._registry = registry
        self._settings = settings
        self._model_assets = model_assets
        self._analysis_asset_service = analysis_asset_service

    def get_runtime_status(self) -> dict:
        """Return a dict describing the current model runtime state.

        Suitable for the ``/ai/v1/model-version`` or a dedicated
        ``/ai/v1/model-status`` endpoint.
        """
        base = self._registry.status()
        snapshot = self._analysis_asset_service.runtime_snapshot(self._registry)

        def _module_payload(module_name: str, asset_name: str | None = None) -> dict:
            status = snapshot.modules.get(module_name)
            assets = self._model_assets.module_descriptor(asset_name or module_name)
            if status is None:
                return {
                    "mode": "disabled",
                    "enabled": False,
                    "ready": False,
                    "implType": "DISABLED",
                    "assets": assets,
                    "missingItems": [],
                    "loadError": None,
                }
            return {
                "mode": "real" if status.ready else ("failed" if status.enabled else "disabled"),
                "enabled": status.enabled,
                "ready": status.ready,
                "implType": status.impl_type,
                "assets": assets,
                "missingItems": [dump_camel(item) for item in status.missing_items],
                "loadError": status.load_error,
                "expectedInputSize": status.expected_input_size,
                "normalization": status.normalization,
                "postprocess": status.postprocess,
                "labelOrder": status.label_order,
            }

        base["modules"] = {
            "quality": _module_payload("quality"),
            "toothDetect": _module_payload("tooth_detect"),
            "segmentation": _module_payload("segmentation"),
            "grading": _module_payload("grading"),
            "risk": {
                "mode": "real" if self._registry.is_module_real("risk") else "disabled",
                "enabled": self._settings.model_risk_enabled,
                "implType": self._settings.model_risk_impl_type,
            },
        }
        base["analysisRuntime"] = dump_camel(snapshot)
        return base
