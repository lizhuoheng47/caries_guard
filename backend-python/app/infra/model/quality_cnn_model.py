from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.quality-cnn")

class QualityCnnAdapter(BaseModelAdapter):
    """ML-model quality adapter placeholder.

    Current repository does not ship executable CNN runtime code/weights loader.
    In real mode this adapter MUST fail explicitly instead of returning fake
    values, so callers can detect unsupported ML deployments immediately.
    """

    model_code = "quality-check-cnn-v1"
    model_type_code = "QUALITY"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        log.info("loading real CNN quality model weights model_code=%s", self.model_code)
        # In a real impl, this would be torch.load() or onnx.InferenceSession()
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(self, image_path: Path) -> dict[str, Any]:
        raise RuntimeError(
            "QualityCnnAdapter ML inference is not implemented in this runtime. "
            "Use CG_MODEL_QUALITY_IMPL_TYPE=HEURISTIC or integrate a real ML backend."
        )
