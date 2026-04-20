from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.grading-classifier")

class GradingClassifierAdapter(BaseModelAdapter):
    """ML-model grading adapter placeholder.

    This adapter intentionally fails in infer() until a real classifier runtime
    is integrated, preventing any hard-coded grading output in real mode.
    """

    model_code = "caries-grading-classifier-v1"
    model_type_code = "GRADING"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        log.info("loading real grading classifier weights model_code=%s", self.model_code)
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(
        self,
        image_path: Path,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detections: list[Any] | None = None,
    ) -> dict[str, Any]:
        raise RuntimeError(
            "GradingClassifierAdapter ML inference is not implemented in this runtime. "
            "Use CG_MODEL_GRADING_IMPL_TYPE=HEURISTIC or integrate a real ML backend."
        )
