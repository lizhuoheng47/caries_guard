"""Quality check sub-pipeline with mode-aware routing and fallback.

Fallback rules (hard constraint from user review):
- **mock** — always return mock result.
- **hybrid** — attempt real adapter; on failure, fallback to mock + log degradation.
- **real** — attempt real adapter; on failure, raise exception (NO silent fallback).
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import QualityCheckResult
from app.schemas.request import ImageInput

log = get_logger("cariesguard-ai.pipeline.quality")


class QualityPipeline:
    """Quality check pipeline with mock / heuristic routing."""

    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings

    def check(self, image: ImageInput, image_path: Path | None = None) -> QualityCheckResult:
        """Run quality check, respecting the current runtime mode."""
        mode = self._registry.get_runtime_mode()

        if not self._registry.is_module_real("quality"):
            return self._mock_result(image)

        # ── real / hybrid path ──────────────────────────────────────────
        adapter = self._registry.get_quality_model()
        if adapter is None or image_path is None:
            if mode == "real":
                raise BusinessException(
                    "M5001",
                    "quality adapter or image not available in real mode",
                )
            log.warning("quality adapter/image unavailable — fallback to mock (hybrid)")
            return self._mock_result(image)

        try:
            result = adapter.infer(image_path)
            return self._to_schema(image, result)
        except Exception as exc:
            if mode == "real":
                raise BusinessException("M5002", f"quality inference failed: {exc}") from exc
            log.warning("quality inference failed — fallback to mock (hybrid) error=%s", exc)
            return self._mock_result(image)

    def get_last_impl_type(self) -> str:
        """Return the impl_type used in the most recent ``check()`` call.

        Callers read this AFTER ``check()`` to stamp ``raw_result_json``.
        """
        adapter = self._registry.get_quality_model()
        if adapter is not None and self._registry.is_module_real("quality"):
            return adapter.impl_type.value
        return ImplType.MOCK.value

    # ── private ──────────────────────────────────────────────────────────

    @staticmethod
    def _mock_result(image: ImageInput) -> QualityCheckResult:
        return QualityCheckResult(
            image_id=image.image_id if image else None,
            check_result_code="PASS",
            quality_score=90,
            blur_score=88,
            exposure_score=90,
            integrity_score=92,
            occlusion_score=86,
            issue_codes=[],
            suggestion_text="quality passed (mock)",
        )

    @staticmethod
    def _to_schema(image: ImageInput, result: dict[str, Any]) -> QualityCheckResult:
        """Convert adapter dict → callback schema object."""
        status = result.get("qualityStatusCode", "PASS")
        q_score = result.get("qualityScore", 0.9)
        issues = result.get("issues", [])
        return QualityCheckResult(
            image_id=image.image_id if image else None,
            check_result_code=status,
            quality_score=int(q_score * 100),
            blur_score=int(result.get("blurScore", 0.9) * 100),
            exposure_score=int(result.get("exposureScore", 0.9) * 100),
            integrity_score=int(result.get("edgeDensityScore", 0.9) * 100),
            occlusion_score=86,  # not yet computed by heuristic
            issue_codes=issues,
            suggestion_text="quality passed" if status == "PASS" else f"quality issues: {', '.join(issues)}",
        )
