"""Centralised model registry — manages adapter lifecycle and routing.

The registry decides, based on ``Settings.ai_runtime_mode`` and per-module
enable flags, which adapters to instantiate and load at startup.
"""

from __future__ import annotations

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.infra.model.grading_model import GradingModelAdapter
from app.infra.model.lesion_segmenter import LesionSegmenterAdapter
from app.infra.model.quality_model import QualityModelAdapter
from app.infra.model.tooth_detector import ToothDetectorAdapter

log = get_logger("cariesguard-ai.model.registry")


class ModelRegistry:
    """Owns the full set of model adapters and their lifecycle."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._quality: QualityModelAdapter | None = None
        self._tooth_detector: ToothDetectorAdapter | None = None
        self._segmenter: LesionSegmenterAdapter | None = None
        self._grading_model: GradingModelAdapter | None = None

    # ── accessors ────────────────────────────────────────────────────────

    def get_quality_model(self) -> QualityModelAdapter | None:
        return self._quality

    def get_tooth_detector(self) -> ToothDetectorAdapter | None:
        return self._tooth_detector

    def get_segmenter(self) -> LesionSegmenterAdapter | None:
        return self._segmenter

    def get_grading_model(self) -> GradingModelAdapter | None:
        return self._grading_model

    def get_runtime_mode(self) -> str:
        return self._settings.ai_runtime_mode

    def is_module_real(self, module: str) -> bool:
        """Return *True* if *module* should run a real (non-mock) path."""
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

    # ── lifecycle ────────────────────────────────────────────────────────

    def startup(self) -> None:
        """Instantiate and load adapters that are enabled."""
        mode = self._settings.ai_runtime_mode
        log.info("model registry startup ai_runtime_mode=%s", mode)

        if self.is_module_real("quality"):
            self._quality = QualityModelAdapter(
                confidence_threshold=self._settings.model_confidence_threshold,
            )
            self._quality.load()
            log.info(
                "quality adapter loaded model_code=%s impl_type=%s",
                self._quality.model_code,
                self._quality.impl_type.value,
            )

        if self.is_module_real("tooth_detect"):
            self._tooth_detector = ToothDetectorAdapter(
                confidence_threshold=self._settings.model_confidence_threshold,
            )
            self._tooth_detector.load()
            log.info(
                "tooth detector loaded model_code=%s impl_type=%s",
                self._tooth_detector.model_code,
                self._tooth_detector.impl_type.value,
            )

        if self.is_module_real("segmentation"):
            self._segmenter = LesionSegmenterAdapter(
                confidence_threshold=self._settings.model_confidence_threshold,
            )
            self._segmenter.load()
            log.info(
                "segmentation adapter loaded model_code=%s impl_type=%s",
                self._segmenter.model_code,
                self._segmenter.impl_type.value,
            )

        if self.is_module_real("grading"):
            self._grading_model = GradingModelAdapter(
                confidence_threshold=self._settings.model_confidence_threshold,
            )
            self._grading_model.load()
            log.info(
                "grading adapter loaded model_code=%s impl_type=%s",
                self._grading_model.model_code,
                self._grading_model.impl_type.value,
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
            )
            if a is not None
        ]
