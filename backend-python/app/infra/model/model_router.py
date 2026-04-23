from __future__ import annotations

from typing import Type

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.router")


class ModelRouter:
    """Unified entry point to resolve enabled model implementations."""

    @staticmethod
    def resolve_impl_type(settings: Settings, module: str) -> ImplType:
        if settings.ai_runtime_mode == "mock":
            return ImplType.MOCK

        impl_type_overrides = {
            "quality": getattr(settings, "model_quality_impl_type", "HEURISTIC"),
            "tooth_detect": getattr(settings, "model_tooth_detect_impl_type", "HEURISTIC"),
            "segmentation": getattr(settings, "model_segmentation_impl_type", "HEURISTIC"),
            "grading": getattr(settings, "model_grading_impl_type", "HEURISTIC"),
            "risk": getattr(settings, "model_risk_impl_type", "HEURISTIC"),
        }
        target_impl = str(impl_type_overrides.get(module, "HEURISTIC")).strip().upper()
        return ImplType.ML_MODEL if target_impl == "ML_MODEL" else ImplType.HEURISTIC

    @staticmethod
    def get_adapter_class(module: str, impl_type: ImplType) -> Type[BaseModelAdapter] | None:
        from app.infra.model.grading_model_adapter import GradingModelAdapter
        from app.infra.model.grading_model import GradingHeuristicAdapter
        from app.infra.model.lesion_segmenter import LesionSegmenterAdapter
        from app.infra.model.segmentation_model_adapter import SegmentationModelAdapter
        from app.infra.model.mock_adapters import (
            GradingMockAdapter,
            QualityMockAdapter,
            RiskMockAdapter,
            SegmentationMockAdapter,
            ToothDetectionMockAdapter,
        )
        from app.infra.model.quality_cnn_model import QualityCnnAdapter
        from app.quality.quality_adapter import QualityAssessmentAdapter
        from app.infra.model.risk_ml_fusion_model import RiskMlFusionAdapter
        from app.infra.model.risk_model import RiskHeuristicFusionAdapter
        from app.infra.model.tooth_detector import ToothDetectorHeuristicAdapter
        from app.infra.model.tooth_detector_yolo import ToothDetectorYoloAdapter

        mapping = {
            ("quality", ImplType.MOCK): QualityMockAdapter,
            ("quality", ImplType.HEURISTIC): QualityAssessmentAdapter,
            ("quality", ImplType.ML_MODEL): QualityCnnAdapter,
            ("tooth_detect", ImplType.MOCK): ToothDetectionMockAdapter,
            ("tooth_detect", ImplType.HEURISTIC): ToothDetectorHeuristicAdapter,
            ("tooth_detect", ImplType.ML_MODEL): ToothDetectorYoloAdapter,
            ("segmentation", ImplType.MOCK): SegmentationMockAdapter,
            ("segmentation", ImplType.HEURISTIC): LesionSegmenterAdapter,
            ("segmentation", ImplType.ML_MODEL): SegmentationModelAdapter,
            ("grading", ImplType.MOCK): GradingMockAdapter,
            ("grading", ImplType.HEURISTIC): GradingHeuristicAdapter,
            ("grading", ImplType.ML_MODEL): GradingModelAdapter,
            ("risk", ImplType.MOCK): RiskMockAdapter,
            ("risk", ImplType.HEURISTIC): RiskHeuristicFusionAdapter,
            ("risk", ImplType.ML_MODEL): RiskMlFusionAdapter,
        }
        return mapping.get((module, impl_type))
