from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Any

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.quality.quality_infer import QualityInferModel

log = get_logger("cariesguard-ai.quality.adapter")


class QualityAssessmentAdapter(BaseModelAdapter):
    """Model-driven quality adapter (real CV inference)."""

    model_code = "quality-assessment-cv-v2"
    model_type_code = "QUALITY"
    impl_type = ImplType.HEURISTIC

    def __init__(
        self,
        confidence_threshold: float = 0.5,
        settings: Any | None = None,
    ) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold
        self._settings = settings
        self._infer_model: QualityInferModel | None = None
        self._params: dict[str, Any] = {}

    def load(self) -> None:
        params = self._load_model_params()
        self._params = params
        self._infer_model = QualityInferModel(params)
        self._loaded = True
        log.info(
            "loaded quality assessment adapter modelCode=%s modelVersion=%s",
            self.model_code,
            self._infer_model.model_version,
        )

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False
        self._infer_model = None
        self._params = {}

    def infer(self, image_path: Path) -> dict[str, Any]:
        if not self._loaded or self._infer_model is None:
            raise RuntimeError("quality adapter not loaded")
        started = time.perf_counter()
        output = self._infer_model.infer(image_path)
        inference_millis = int((time.perf_counter() - started) * 1000)
        return {
            "qualityStatus": output.quality_status,
            "qualityStatusCode": output.quality_status,
            "qualityScore": output.quality_score,
            "qualityIssues": output.quality_issues,
            "retakeSuggested": output.retake_suggested,
            "blurScore": output.sub_scores.get("blur"),
            "exposureScore": output.sub_scores.get("exposure"),
            "integrityScore": output.sub_scores.get("integrity"),
            "occlusionScore": output.sub_scores.get("occlusion"),
            "artifactScore": output.sub_scores.get("artifact"),
            "implType": self.impl_type.value,
            "modelVersion": output.model_version,
            "inferenceMillis": inference_millis,
            "rawResult": {
                "issueConfidences": output.issue_confidences,
                "subScores": output.sub_scores,
                "featureVector": output.feature_vector,
                "confidenceThreshold": self._confidence_threshold,
                "modelCode": self.model_code,
            },
        }

    def _load_model_params(self) -> dict[str, Any]:
        candidate = None
        if self._settings is not None:
            candidate = getattr(self._settings, "quality_model_param_path", None)
            if not candidate:
                candidate = getattr(self._settings, "quality_model_weights_path", None)
        if not candidate:
            candidate = str(Path(__file__).with_name("quality_model_params.json"))
        param_path = Path(candidate)
        if not param_path.exists():
            raise RuntimeError(f"quality model params not found: {param_path}")
        with param_path.open("r", encoding="utf-8") as fp:
            params = json.load(fp)
        if not isinstance(params, dict):
            raise RuntimeError("quality model params must be a JSON object")
        return params
