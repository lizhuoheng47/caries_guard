from __future__ import annotations

from pathlib import Path

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.infra.model.model_registry import ModelRegistry
from app.quality.quality_pipeline import to_quality_check_result
from app.schemas.callback import QualityCheckResult
from app.schemas.request import ImageInput


class QualityPipeline:
    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings
        self._last_impl_type = "DISABLED"

    def check(self, image: ImageInput, image_path: Path | None = None) -> QualityCheckResult:
        if not self._registry.is_module_real("quality"):
            raise BusinessException("M5001", "quality module is disabled")

        adapter = self._registry.get_quality_model()
        if adapter is None:
            raise BusinessException("M5002", "quality adapter is unavailable")
        if image_path is None:
            raise BusinessException("M5003", "quality image is unavailable")

        try:
            result = adapter.infer(image_path)
            self._last_impl_type = str(result.get("implType") or adapter.impl_type.value)
            parsed = to_quality_check_result(image, result)
            if parsed.check_result_code == "FAIL" and self._settings.quality_fail_strategy == "FAIL_FAST":
                raise BusinessException(
                    "M5004",
                    "quality assessment failed and CG_QUALITY_FAIL_STRATEGY=FAIL_FAST",
                )
            return parsed
        except BusinessException:
            raise
        except Exception as exc:
            raise BusinessException("M5005", f"quality inference failed: {exc}") from exc

    def get_last_impl_type(self) -> str:
        return self._last_impl_type
