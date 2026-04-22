from __future__ import annotations

from app.core.config import Settings
from app.core.exceptions import AnalysisRuntimeException, ModelRuntimeException
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_router import ModelRouter

log = get_logger("cariesguard-ai.model.registry")


class ModelRegistry:
    """Owns the full set of model adapters and their lifecycle."""

    def __init__(self, settings: Settings, model_assets: ModelAssets) -> None:
        self._settings = settings
        self._model_assets = model_assets
        self._quality: BaseModelAdapter | None = None
        self._tooth_detector: BaseModelAdapter | None = None
        self._segmenter: BaseModelAdapter | None = None
        self._grading_model: BaseModelAdapter | None = None
        self._risk_model: BaseModelAdapter | None = None
        self._module_errors: dict[str, str | None] = {
            "quality": None,
            "tooth_detect": None,
            "segmentation": None,
            "grading": None,
            "risk": None,
        }

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
        flag_map: dict[str, bool] = {
            "quality": self._settings.model_quality_enabled,
            "tooth_detect": self._settings.model_tooth_detect_enabled,
            "segmentation": self._settings.model_segmentation_enabled,
            "grading": self._settings.model_grading_enabled,
            "risk": self._settings.model_risk_enabled,
        }
        return flag_map.get(module, False)

    def is_module_real(self, module: str) -> bool:
        if self._settings.ai_runtime_mode == "mock":
            return False
        return self.is_module_enabled(module) and self.is_module_loaded(module)

    def is_module_loaded(self, module: str) -> bool:
        adapter = self._adapter_for(module)
        return bool(adapter is not None and adapter.is_loaded())

    def get_module_error(self, module: str) -> str | None:
        return self._module_errors.get(module)

    def startup(self) -> None:
        mode = self._settings.ai_runtime_mode
        log.info("model registry startup ai_runtime_mode=%s", mode)

        for module in ["quality", "tooth_detect", "segmentation", "grading", "risk"]:
            if not self.is_module_enabled(module):
                continue

            impl_type = ModelRouter.resolve_impl_type(self._settings, module)
            adapter_cls = ModelRouter.get_adapter_class(module, impl_type)
            if adapter_cls is None:
                raise RuntimeError(f"No adapter class found for module={module} impl_type={impl_type.value}")

            if module == "risk":
                adapter = adapter_cls(
                    confidence_threshold=self._settings.model_confidence_threshold,
                    settings=self._settings,
                )
            elif module == "segmentation" and impl_type.value == "ML_MODEL":
                adapter = adapter_cls(
                    confidence_threshold=self._settings.model_confidence_threshold,
                    model_assets=self._model_assets,
                    settings=self._settings,
                )
            elif module == "grading" and impl_type.value == "ML_MODEL":
                adapter = adapter_cls(
                    confidence_threshold=self._settings.model_confidence_threshold,
                    model_assets=self._model_assets,
                    settings=self._settings,
                )
            elif module == "tooth_detect" and impl_type.value == "ML_MODEL":
                adapter = adapter_cls(
                    confidence_threshold=self._settings.model_confidence_threshold,
                    settings=self._settings,
                )
            elif module == "quality":
                try:
                    adapter = adapter_cls(
                        confidence_threshold=self._settings.model_confidence_threshold,
                        settings=self._settings,
                    )
                except TypeError:
                    adapter = adapter_cls(confidence_threshold=self._settings.model_confidence_threshold)
            else:
                adapter = adapter_cls(confidence_threshold=self._settings.model_confidence_threshold)

            self._set_adapter(module, adapter)
            try:
                adapter.load()
                self._module_errors[module] = None
                log.info(
                    "%s adapter loaded model_code=%s impl_type=%s",
                    module,
                    adapter.model_code,
                    adapter.impl_type.value,
                )
            except Exception as exc:
                normalized = self._normalize_startup_error(module, exc)
                self._module_errors[module] = f"{normalized.code}: {normalized.message}"
                log.error(
                    "failed to load %s adapter model_code=%s impl_type=%s error=%s",
                    module,
                    adapter.model_code,
                    adapter.impl_type.value,
                    normalized.message,
                )
                if mode == "real":
                    raise normalized

        loaded = [adapter for adapter in self._all_adapters() if adapter.is_loaded()]
        log.info("model registry ready %d adapter(s) loaded", len(loaded))

    def shutdown(self) -> None:
        for adapter in self._all_adapters():
            if adapter.is_loaded():
                adapter.unload()
                log.info("unloaded adapter model_code=%s", adapter.model_code)

    def status(self) -> dict:
        adapters = {adapter.model_type_code: adapter.info() for adapter in self._all_adapters()}
        return {
            "aiRuntimeMode": self._settings.ai_runtime_mode,
            "adapters": adapters,
            "moduleErrors": dict(self._module_errors),
            "assets": {
                "segmentation": self._model_assets.module_descriptor("segmentation"),
                "grading": self._model_assets.module_descriptor("grading"),
            },
        }

    def _normalize_startup_error(self, module: str, exc: Exception) -> AnalysisRuntimeException:
        if isinstance(exc, AnalysisRuntimeException):
            return exc
        return ModelRuntimeException(
            module,
            "load",
            f"{module} adapter startup failed: {exc}",
            details={"exceptionType": exc.__class__.__name__},
        )

    def _set_adapter(self, module: str, adapter: BaseModelAdapter) -> None:
        if module == "quality":
            self._quality = adapter
        elif module == "tooth_detect":
            self._tooth_detector = adapter
        elif module == "segmentation":
            self._segmenter = adapter
        elif module == "grading":
            self._grading_model = adapter
        elif module == "risk":
            self._risk_model = adapter

    def _adapter_for(self, module: str) -> BaseModelAdapter | None:
        mapping = {
            "quality": self._quality,
            "tooth_detect": self._tooth_detector,
            "segmentation": self._segmenter,
            "grading": self._grading_model,
            "risk": self._risk_model,
        }
        return mapping.get(module)

    def _all_adapters(self) -> list[BaseModelAdapter]:
        return [
            adapter
            for adapter in (
                self._quality,
                self._tooth_detector,
                self._segmenter,
                self._grading_model,
                self._risk_model,
            )
            if adapter is not None
        ]
