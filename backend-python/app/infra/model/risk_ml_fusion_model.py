from __future__ import annotations

from typing import Any

from app.core.config import Settings
from app.infra.model.base_model import BaseModelAdapter, ImplType

class RiskMlFusionAdapter(BaseModelAdapter):
    """ML-model risk adapter placeholder.

    This adapter fails explicitly to avoid fabricated risk outcomes when no
    real ML risk model is integrated.
    """

    model_code = "risk-ml-fusion-v1"
    model_type_code = "RISK"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5, settings: Settings | None = None) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold
        self._settings = settings or Settings()

    def load(self) -> None:
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(
        self,
        *,
        patient_profile: Any | None,
        grading_result: Any | None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detection_count: int = 0,
    ) -> dict[str, Any]:
        raise RuntimeError(
            "RiskMlFusionAdapter ML inference is not implemented in this runtime. "
            "Use CG_MODEL_RISK_IMPL_TYPE=HEURISTIC or integrate a real ML backend."
        )
