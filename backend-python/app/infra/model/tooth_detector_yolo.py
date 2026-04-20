from __future__ import annotations

from pathlib import Path
from typing import Any

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType
# We'll import the FDI helper from the heuristic file after refactoring it
# For now, we'll use a placeholder or define it.

log = get_logger("cariesguard-ai.model.tooth-detector-yolo")

class ToothDetectorYoloAdapter(BaseModelAdapter):
    """ML-model tooth detector placeholder.

    The repository currently does not include runnable YOLO inference code.
    This adapter fails explicitly instead of returning fabricated boxes.
    """

    model_code = "tooth-detect-yolo-v8"
    model_type_code = "DETECTION"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        log.info("loading YOLOv8 weights model_code=%s", self.model_code)
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(self, image_path: Path) -> dict[str, Any]:
        raise RuntimeError(
            "ToothDetectorYoloAdapter ML inference is not implemented in this runtime. "
            "Use CG_MODEL_TOOTH_DETECT_IMPL_TYPE=HEURISTIC or integrate a real ML backend."
        )
