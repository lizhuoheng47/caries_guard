from functools import lru_cache

from app.core.config import Settings
from app.infra.llm.template_llm_client import TemplateLlmClient
from app.infra.model.model_registry import ModelRegistry
from app.infra.storage.minio_client import MinioStorageClient
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.repositories.ai_runtime_repository import AiRuntimeRepository
from app.repositories.governance_repository import GovernanceRepository
from app.repositories.metadata_repository import MetadataRepository
from app.repositories.rag_repository import RagRepository
from app.services.callback_service import CallbackService
from app.services.image_fetch_service import ImageFetchService
from app.services.knowledge_service import KnowledgeService
from app.services.model_switch_service import ModelSwitchService
from app.services.rag_service import RagService
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService


class AppContainer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.storage = MinioStorageClient(settings)
        self.image_fetch_service = ImageFetchService(settings, self.storage)
        self.visual_asset_service = VisualAssetService(settings, self.storage)
        self.risk_service = RiskService(settings)
        self.callback_service = CallbackService(settings)
        self.metadata_repository = MetadataRepository(settings)
        self.rag_repository = RagRepository()
        self.ai_runtime_repository = AiRuntimeRepository()
        self.governance_repository = GovernanceRepository()
        self.vector_store = SimpleVectorStore()
        self.llm_client = TemplateLlmClient()
        self.knowledge_service = KnowledgeService(settings, self.rag_repository, self.vector_store)
        self.rag_service = RagService(
            settings=settings,
            repository=self.rag_repository,
            vector_store=self.vector_store,
            llm_client=self.llm_client,
            knowledge_service=self.knowledge_service,
        )

        # ── Phase 5A: model runtime ─────────────────────────────────────
        self.model_registry = ModelRegistry(settings)
        self.model_registry.startup()

        self.quality_pipeline = QualityPipeline(self.model_registry, settings)
        self.detection_pipeline = DetectionPipeline(self.model_registry, settings)
        self.segmentation_pipeline = SegmentationPipeline(self.model_registry, settings)
        self.model_switch_service = ModelSwitchService(self.model_registry, settings)

        self.pipeline = InferencePipeline(
            settings=settings,
            image_fetch_service=self.image_fetch_service,
            visual_asset_service=self.visual_asset_service,
            risk_service=self.risk_service,
            model_registry=self.model_registry,
            quality_pipeline=self.quality_pipeline,
            detection_pipeline=self.detection_pipeline,
            segmentation_pipeline=self.segmentation_pipeline,
        )


@lru_cache(maxsize=1)
def get_container() -> AppContainer:
    return AppContainer(Settings())
