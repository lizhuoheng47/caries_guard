"""Quality assessment pipeline with strict runtime mode handling."""

from __future__ import annotations

from pathlib import Path

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.quality.quality_pipeline import to_quality_check_result
from app.schemas.callback import QualityCheckResult
from app.schemas.request import ImageInput

log = get_logger("cariesguard-ai.pipeline.quality")


class QualityPipeline:
    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings
        self._last_impl_type = ImplType.MOCK.value

    def check(self, image: ImageInput, image_path: Path | None = None) -> QualityCheckResult:
        mode = self._registry.get_runtime_mode()

        if not self._registry.is_module_real("quality"):
            self._last_impl_type = ImplType.MOCK.value
            return self._mock_result(image)

        adapter = self._registry.get_quality_model()
        if adapter is None or image_path is None:
            if mode == "real":
                raise BusinessException("M5001", "quality adapter or image is unavailable in real mode")
            log.warning("quality adapter/image unavailable, fallback to mock in hybrid mode")
            self._last_impl_type = ImplType.MOCK.value
            return self._mock_result(image)

        try:
            result = adapter.infer(image_path)
            self._last_impl_type = str(result.get("implType") or adapter.impl_type.value)
            parsed = to_quality_check_result(image, result)

            if parsed.check_result_code == "FAIL":
                if self._settings.quality_fail_strategy == "FAIL_FAST":
                    raise BusinessException(
                        "M5003",
                        "quality assessment failed and CG_QUALITY_FAIL_STRATEGY=FAIL_FAST",
                    )
                log.warning(
                    "quality failed but pipeline continues via explicit failure branch imageId=%s issues=%s",
                    image.image_id if image else None,
                    parsed.issue_codes,
                )
            return parsed
        except BusinessException:
            raise
        except Exception as exc:
            if mode == "real":
                raise BusinessException("M5002", f"quality inference failed: {exc}") from exc
            log.warning("quality inference failed, fallback to mock in hybrid mode error=%s", exc)
            self._last_impl_type = ImplType.MOCK.value
            return self._mock_result(image)

    def get_last_impl_type(self) -> str:
        return self._last_impl_type

    @staticmethod
    def _mock_result(image: ImageInput) -> QualityCheckResult:
        return QualityCheckResult(
            image_id=image.image_id if image else None,
            check_result_code="PASS",
            quality_score=90,
            quality_score_float=0.9,
            quality_status="PASS",
            blur_score=90,
            exposure_score=90,
            integrity_score=90,
            occlusion_score=90,
            issue_codes=[],
            quality_issues=[],
            retake_suggested=False,
            impl_type=ImplType.MOCK.value,
            model_version="mock-quality-v1",
            inference_millis=0,
            raw_result={"source": "mock"},
            suggestion_text="quality passed (mock)",
        )

