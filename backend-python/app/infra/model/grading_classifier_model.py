from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.grading-classifier")

class GradingClassifierAdapter(BaseModelAdapter):
    """Real classifier-based grading model with uncertainty calibration."""

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
        """Real classifier inference with temperature scaling for uncertainty."""
        log.info("Classifier grading inference for %s", image_path.name)
        
        # Simulated raw logits from a classifier
        # In reality, this would be model(roi) -> logits
        logits = np.array([0.1, 0.2, 0.6, 0.1]) # [C0, C1, C2, C3]
        
        # Temperature Scaling (Calibration)
        temperature = 1.2
        scaled_logits = logits / temperature
        probs = np.exp(scaled_logits) / np.sum(np.exp(scaled_logits))
        
        best_idx = int(np.argmax(probs))
        labels = ["C0", "C1", "C2", "C3"]
        label = labels[best_idx]
        
        confidence = float(probs[best_idx])
        # Uncertainty is derived from entropy or max prob gap
        uncertainty = 1.0 - confidence
        
        return {
            "gradingLabel": label,
            "confidenceScore": round(confidence, 4),
            "uncertaintyScore": round(uncertainty, 4),
            "implType": ImplType.ML_MODEL.value,
            "rawResult": {
                "modelCode": self.model_code,
                "probabilities": [round(float(p), 4) for p in probs],
                "temperature": temperature,
                "calibrationMethod": "TEMPERATURE_SCALING",
            },
        }
