"""Centralised model registry — manages adapter lifecycle and routing.

The registry decides, based on ``Settings.ai_runtime_mode`` and per-module
enable flags, which adapters to instantiate and load at startup.
"""

from __future__ import annotations

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.infra.model.model_router import ModelRouter

log = get_logger("cariesguard-ai.model.registry")


class ModelRegistry:
    """Owns the full set of model adapters and their lifecycle."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._quality: BaseModelAdapter | None = None
        self._tooth_detector: BaseModelAdapter | None = None
        self._segmenter: BaseModelAdapter | None = None
        self._grading_model: BaseModelAdapter | None = None
        self._risk_model: BaseModelAdapter | None = None

    # ── accessors ────────────────────────────────────────────────────────

    def get_quality_model(self) -> BaseModelAdapter | None:
        return self._quality

    def get_tooth_detector(self) -> BaseModelAdapter | None:
        return self._tooth_detector

    def get_segmenter(self) -> BaseModelAdapter | None:
        return self._segmenter

    def get_grading_model(self) -> BaseModelAdapter | None:
        return self._grading_model

    def get_risk_model(self) -> BaseModelAdapter | None:
        return self._risk_model

    def get_runtime_mode(self) -> str:
        return self._settings.ai_runtime_mode

    def is_module_enabled(self, module: str) -> bool:
        """Return *True* if *module* should be active (either heuristic or real)."""
        mode = self._settings.ai_runtime_mode
        if mode == "mock":
            return False
        if mode == "real":
            return True
        # hybrid — check per-module flag
        flag_map: dict[str, bool] = {
            "quality": self._settings.model_quality_enabled,
            "tooth_detect": self._settings.model_tooth_detect_enabled,
            "segmentation": self._settings.model_segmentation_enabled,
            "grading": self._settings.model_grading_enabled,
            "risk": self._settings.model_risk_enabled,
        }
        return flag_map.get(module, False)

    def is_module_real(self, module: str) -> bool:
        """Return *True* when a non-mock implementation should execute for *module*."""
        if self._settings.ai_runtime_mode == "mock":
            return False
        if self._settings.ai_runtime_mode == "real":
            return True
        return self.is_module_enabled(module)

    # ── lifecycle ────────────────────────────────────────────────────────

    def startup(self) -> None:
        """Instantiate and load adapters that are enabled."""
        mode = self._settings.ai_runtime_mode
        log.info("model registry startup ai_runtime_mode=%s", mode)

        modules = ["quality", "tooth_detect", "segmentation", "grading", "risk"]
        
        for module in modules:
            if not self.is_module_enabled(module):
                continue
                
            impl_type = ModelRouter.resolve_impl_type(self._settings, module)
            cls = ModelRouter.get_adapter_class(module, impl_type)
            
            if not cls:
                # Handle segmenter specifically as it wasn't refactored yet in the router mapping
                if module == "segmentation":
                    from app.infra.model.lesion_segmenter import LesionSegmenterAdapter
                    cls = LesionSegmenterAdapter
                else:
                    log.warning("No adapter class found for module=%s impl_type=%s", module, impl_type)
                    continue
            
            adapter = None
            if module == "risk":
                adapter = cls(
                    confidence_threshold=self._settings.model_confidence_threshold,
                    settings=self._settings
                )
            else:
                adapter = cls(
                    confidence_threshold=self._settings.model_confidence_threshold
                )
                
            adapter.load()
            
            # Assign to internal attribute
            if module == "quality": self._quality = adapter
            elif module == "tooth_detect": self._tooth_detector = adapter
            elif module == "segmentation": self._segmenter = adapter
            elif module == "grading": self._grading_model = adapter
            elif module == "risk": self._risk_model = adapter
            
            log.info(
                "%s adapter loaded model_code=%s impl_type=%s",
                module,
                adapter.model_code,
                adapter.impl_type.value,
            )

        loaded = [a for a in self._all_adapters() if a.is_loaded()]
        log.info("model registry ready — %d adapter(s) loaded", len(loaded))

    def shutdown(self) -> None:
        """Unload all adapters."""
        for adapter in self._all_adapters():
            if adapter.is_loaded():
                adapter.unload()
                log.info("unloaded adapter model_code=%s", adapter.model_code)

    # ── diagnostics ──────────────────────────────────────────────────────

    def status(self) -> dict:
        """Return a summary suitable for health-check or API response."""
        adapters = {a.model_type_code: a.info() for a in self._all_adapters()}
        return {
            "aiRuntimeMode": self._settings.ai_runtime_mode,
            "adapters": adapters,
        }

    # ── internal ─────────────────────────────────────────────────────────

    def _all_adapters(self) -> list[BaseModelAdapter]:
        return [
            a
            for a in (
                self._quality,
                self._tooth_detector,
                self._segmenter,
                self._grading_model,
                self._risk_model,
            )
            if a is not None
        ]
