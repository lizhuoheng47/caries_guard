from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.quality.quality_adapter import QualityAssessmentAdapter

log = get_logger("cariesguard-ai.model.quality-cnn")


class QualityCnnAdapter(QualityAssessmentAdapter):
    """ML-model quality adapter backed by external model parameters."""

    model_code = "quality-assessment-ml-v1"
    model_type_code = "QUALITY"
    impl_type = ImplType.ML_MODEL

    def __init__(self, confidence_threshold: float = 0.5, settings: Any | None = None) -> None:
        super().__init__(confidence_threshold=confidence_threshold, settings=settings)

    def _load_model_params(self) -> dict[str, Any]:
        candidates: list[Path] = []

        if self._settings is not None:
            configured = str(getattr(self._settings, "quality_model_weights_path", "") or "").strip()
            if configured:
                candidates.append(Path(configured))

            model_weights_dir = str(getattr(self._settings, "model_weights_dir", "") or "").strip()
            if model_weights_dir:
                candidates.append(Path(model_weights_dir) / "quality" / "quality_model_params.json")

        repo_root = Path(__file__).resolve().parents[4]
        candidates.append(repo_root / "model-weights" / "quality" / "quality_model_params.json")

        for candidate in candidates:
            if not candidate.exists():
                continue
            params = self._read_params(candidate)
            issue_models = params.get("issueModels")
            if not isinstance(issue_models, dict) or not issue_models:
                raise RuntimeError(
                    f"invalid quality ML model params at {candidate}: missing issueModels"
                )
            log.info("loaded quality ML params path=%s", candidate)
            return params

        raise RuntimeError(
            "quality ML model params not found. "
            "Set CG_QUALITY_MODEL_WEIGHTS_PATH or provide "
            "model-weights/quality/quality_model_params.json"
        )

    @staticmethod
    def _read_params(path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as fp:
            params = json.load(fp)
        if not isinstance(params, dict):
            raise RuntimeError(f"quality ML model params at {path} must be a JSON object")
        return params
