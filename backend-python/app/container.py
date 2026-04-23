from functools import lru_cache

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.infra.storage.minio_client import MinioStorageClient
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.risk_pipeline import RiskPipeline
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.repositories.ai_runtime_repository import AiRuntimeRepository
from app.repositories.governance_repository import GovernanceRepository
from app.repositories.metadata_repository import MetadataRepository
from app.services.analysis_service import AnalysisService
from app.services.analysis_asset_service import AnalysisAssetService
from app.services.callback_service import CallbackService
from app.services.governance_bootstrap_service import GovernanceBootstrapService
from app.services.image_fetch_service import ImageFetchService
from app.services.model_switch_service import ModelSwitchService
from app.services.qwen_vision_service import QwenVisionService
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService


class AppContainer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.storage = MinioStorageClient(settings)
        self.image_fetch_service = ImageFetchService(settings, self.storage)
        self.visual_asset_service = VisualAssetService(settings, self.storage)
        self.risk_service = RiskService(settings)
        self.model_assets = ModelAssets(settings)
        self.metadata_repository = MetadataRepository(settings)
        self.ai_runtime_repository = AiRuntimeRepository()
        self.governance_repository = GovernanceRepository()
        self.callback_service = CallbackService(settings, self.ai_runtime_repository)
        self.qwen_vision_service = QwenVisionService(settings)
        self.analysis_asset_service = AnalysisAssetService(settings, self.model_assets)

        self.model_registry = ModelRegistry(settings, self.model_assets)
        self.model_registry.startup()
        self.governance_bootstrap_service = GovernanceBootstrapService(
            settings,
            self.model_registry,
            self.governance_repository,
        )
        self.governance_bootstrap_service.bootstrap()

        self.quality_pipeline = QualityPipeline(self.model_registry, settings)
        self.detection_pipeline = DetectionPipeline(self.model_registry, settings)
        self.segmentation_pipeline = SegmentationPipeline(self.model_registry, settings, self.model_assets)
        self.grading_pipeline = GradingPipeline(self.model_registry, settings, self.model_assets)
        self.risk_pipeline = RiskPipeline(self.model_registry, settings)
        self.model_switch_service = ModelSwitchService(
            self.model_registry,
            settings,
            self.model_assets,
            self.analysis_asset_service,
        )

        self.pipeline = InferencePipeline(
            settings=settings,
            image_fetch_service=self.image_fetch_service,
            visual_asset_service=self.visual_asset_service,
            model_registry=self.model_registry,
            model_assets=self.model_assets,
            quality_pipeline=self.quality_pipeline,
            detection_pipeline=self.detection_pipeline,
            segmentation_pipeline=self.segmentation_pipeline,
            grading_pipeline=self.grading_pipeline,
            risk_service=self.risk_service,
            ai_runtime_repository=self.ai_runtime_repository,
            analysis_asset_service=self.analysis_asset_service,
        )
        self.analysis_service = AnalysisService(self.pipeline, self.callback_service)


@lru_cache(maxsize=1)
def get_container() -> AppContainer:
    return AppContainer(Settings())
