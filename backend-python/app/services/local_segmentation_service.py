from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.segmentation_pipeline import SegmentationPipeline


@dataclass(frozen=True)
class LocalSegmentationRuntime:
    settings: Settings
    model_registry: ModelRegistry
    segmentation_pipeline: SegmentationPipeline


@lru_cache(maxsize=1)
def get_local_segmentation_runtime() -> LocalSegmentationRuntime:
    """Load only dependencies required by the standalone segmentation API."""
    settings = Settings()
    model_assets = ModelAssets(settings)
    registry = ModelRegistry(settings, model_assets)
    registry.startup()
    return LocalSegmentationRuntime(
        settings=settings,
        model_registry=registry,
        segmentation_pipeline=SegmentationPipeline(registry, settings, model_assets),
    )
