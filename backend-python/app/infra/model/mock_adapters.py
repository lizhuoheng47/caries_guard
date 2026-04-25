from __future__ import annotations

from pathlib import Path
from typing import Any

from app.infra.model.base_model import BaseModelAdapter, ImplType


class _MockAdapter(BaseModelAdapter):
    impl_type = ImplType.MOCK
    model_code = "mock"
    model_type_code = "MOCK"

    def __init__(self, confidence_threshold: float = 0.5, **_: Any) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(self, *_args: Any, **_kwargs: Any) -> dict[str, Any]:
        raise RuntimeError(
            f"{self.__class__.__name__} is a runtime placeholder only. "
            "Use the mock pipeline path instead of model-backed inference."
        )


class QualityMockAdapter(_MockAdapter):
    model_code = "quality-mock-v1"
    model_type_code = "QUALITY"


class ToothDetectionMockAdapter(_MockAdapter):
    model_code = "tooth-detect-mock-v1"
    model_type_code = "DETECTION"


class SegmentationMockAdapter(_MockAdapter):
    model_code = "segmentation-mock-v1"
    model_type_code = "SEGMENTATION"


class GradingMockAdapter(_MockAdapter):
    model_code = "grading-mock-v1"
    model_type_code = "GRADING"


class RiskMockAdapter(_MockAdapter):
    model_code = "risk-mock-v1"
    model_type_code = "RISK"
