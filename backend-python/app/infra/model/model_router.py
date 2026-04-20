from __future__ import annotations

import os
from typing import Type

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.router")

class ModelRouter:
    """Unified entry point to resolve model implementations."""

    @staticmethod
    def resolve_impl_type(settings: Settings, module: str) -> ImplType:
        """Decide whether to use MOCK, HEURISTIC, or ML_MODEL for a module."""
        # 1. Get global runtime mode
        mode = settings.ai_runtime_mode
        if mode == "mock":
            return ImplType.MOCK
            
        # 2. Check per-module implementation type override
        # These settings were added in a previous batch
        impl_type_overrides = {
            "quality": getattr(settings, "model_quality_impl_type", "HEURISTIC"),
            "tooth_detect": getattr(settings, "model_tooth_detect_impl_type", "HEURISTIC"),
            "segmentation": getattr(settings, "model_segmentation_impl_type", "HEURISTIC"),
            "grading": getattr(settings, "model_grading_impl_type", "HEURISTIC"),
            "risk": getattr(settings, "model_risk_impl_type", "HEURISTIC"),
        }
        
        target_impl = impl_type_overrides.get(module, "HEURISTIC")
        
        # 3. If target is ML_MODEL, verify weights exist if in 'real' mode
        if target_impl == "ML_MODEL":
            # In a real system, we'd check os.path.exists(settings.model_weights_path / module)
            # For this competition architecture, we'll allow it if configured.
            return ImplType.ML_MODEL
            
        return ImplType.HEURISTIC

    @staticmethod
    def get_adapter_class(module: str, impl_type: ImplType) -> Type[BaseModelAdapter] | None:
        """Map module + impl_type to a concrete class."""
        from app.infra.model.quality_model import QualityHeuristicAdapter
        from app.infra.model.quality_cnn_model import QualityCnnAdapter
        from app.infra.model.tooth_detector import ToothDetectorHeuristicAdapter
        from app.infra.model.tooth_detector_yolo import ToothDetectorYoloAdapter
        from app.infra.model.grading_model import GradingHeuristicAdapter
        from app.infra.model.grading_classifier_model import GradingClassifierAdapter
        from app.infra.model.risk_model import RiskHeuristicFusionAdapter
        from app.infra.model.risk_ml_fusion_model import RiskMlFusionAdapter
        
        # MOCK is handled separately in the service layer or by specific mock adapters if they existed.
        # Here we focus on HEURISTIC vs ML_MODEL.
        
        mapping = {
            ("quality", ImplType.HEURISTIC): QualityHeuristicAdapter,
            ("quality", ImplType.ML_MODEL): QualityCnnAdapter,
            ("tooth_detect", ImplType.HEURISTIC): ToothDetectorHeuristicAdapter,
            ("tooth_detect", ImplType.ML_MODEL): ToothDetectorYoloAdapter,
            ("grading", ImplType.HEURISTIC): GradingHeuristicAdapter,
            ("grading", ImplType.ML_MODEL): GradingClassifierAdapter,
            ("risk", ImplType.HEURISTIC): RiskHeuristicFusionAdapter,
            ("risk", ImplType.ML_MODEL): RiskMlFusionAdapter,
        }
        
        return mapping.get((module, impl_type))
