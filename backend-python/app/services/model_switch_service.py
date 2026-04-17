"""Runtime model status query service for API and health-check consumers."""

from __future__ import annotations

from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry


class ModelSwitchService:
    """Exposes the current runtime mode and per-module adapter status."""

    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings

    def get_runtime_status(self) -> dict:
        """Return a dict describing the current model runtime state.

        Suitable for the ``/ai/v1/model-version`` or a dedicated
        ``/ai/v1/model-status`` endpoint.
        """
        base = self._registry.status()
        base["modules"] = {
            "quality": {
                "mode": "real" if self._registry.is_module_real("quality") else "mock",
                "enabled": self._settings.model_quality_enabled,
            },
            "toothDetect": {
                "mode": "real" if self._registry.is_module_real("tooth_detect") else "mock",
                "enabled": self._settings.model_tooth_detect_enabled,
            },
            "segmentation": {
                "mode": "real" if self._registry.is_module_real("segmentation") else "mock",
                "enabled": self._settings.model_segmentation_enabled,
            },
            "grading": {
                "mode": "real" if self._registry.is_module_real("grading") else "mock",
                "enabled": self._settings.model_grading_enabled,
            },
            "risk": {
                "mode": "real" if self._registry.is_module_real("risk") else "mock",
                "enabled": self._settings.model_risk_enabled,
            },
        }
        return base
