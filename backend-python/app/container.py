from functools import lru_cache

from app.core.config import Settings
from app.infra.graph.neo4j_client import create_neo4j_driver
from app.infra.llm.llm_gateway_factory import create_llm_client
from app.infra.model.model_registry import ModelRegistry
from app.infra.search.opensearch_client import create_opensearch_client
from app.infra.storage.minio_client import MinioStorageClient
from app.infra.vector.hashing_embedder import HashingEmbedder
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline
from app.pipelines.inference_pipeline import InferencePipeline
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.risk_pipeline import RiskPipeline
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.repositories.ai_runtime_repository import AiRuntimeRepository
from app.repositories.eval_repository import EvalRepository
from app.repositories.graph_repository import GraphRepository
from app.repositories.governance_repository import GovernanceRepository
from app.repositories.knowledge_repository import KnowledgeRepository
from app.repositories.metadata_repository import MetadataRepository
from app.repositories.rag_repository import RagRepository
from app.services.answer_validator_service import AnswerValidatorService
from app.services.callback_service import CallbackService
from app.services.case_context_builder import CaseContextBuilder
from app.services.citation_assembler import CitationAssembler
from app.services.chunk_build_service import ChunkBuildService
from app.services.cypher_template_registry import CypherTemplateRegistry
from app.services.dense_retriever import DenseRetriever
from app.services.document_parse_service import DocumentParseService
from app.services.entity_extraction_service import EntityExtractionService
from app.services.entity_linking_service import EntityLinkingService
from app.services.fusion_service import FusionService
from app.services.governance_bootstrap_service import GovernanceBootstrapService
from app.services.graph_retriever import GraphRetriever
from app.services.graph_upsert_service import GraphUpsertService
from app.services.eval_service import EvalService
from app.services.eval_bootstrap_service import EvalBootstrapService
from app.services.image_fetch_service import ImageFetchService
from app.services.intent_classifier_service import IntentClassifierService
from app.services.knowledge_service import KnowledgeService
from app.services.lexical_retriever import LexicalRetriever
from app.services.model_switch_service import ModelSwitchService
from app.services.open_search_index_service import OpenSearchIndexService
from app.services.query_rewrite_service import QueryRewriteService
from app.services.rag_orchestrator import RagOrchestrator
from app.services.rag_log_service import RagLogService
from app.services.refusal_policy_service import RefusalPolicyService
from app.services.rag_safety_guard_service import RagSafetyGuardService
from app.services.rag_service import RagService
from app.services.rerank_service import RerankService
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService


class AppContainer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.storage = MinioStorageClient(settings)
        self.image_fetch_service = ImageFetchService(settings, self.storage)
        self.visual_asset_service = VisualAssetService(settings, self.storage)
        self.risk_service = RiskService(settings)
        self.metadata_repository = MetadataRepository(settings)
        self.rag_repository = RagRepository()
        self.knowledge_repository = KnowledgeRepository()
        self.graph_repository = GraphRepository()
        self.eval_repository = EvalRepository()
        self.ai_runtime_repository = AiRuntimeRepository()
        self.governance_repository = GovernanceRepository()
        self.callback_service = CallbackService(settings, self.ai_runtime_repository)
        self.vector_store = SimpleVectorStore()
        self.embedder = HashingEmbedder(settings.rag_embedding_dimension)
        self.opensearch_client = create_opensearch_client(settings)
        self.neo4j_driver = create_neo4j_driver(settings)
        self.document_parse_service = DocumentParseService()
        self.chunk_build_service = ChunkBuildService()
        self.entity_extraction_service = EntityExtractionService()
        self.open_search_index_service = OpenSearchIndexService(settings, self.opensearch_client, self.embedder)
        self.cypher_template_registry = CypherTemplateRegistry()
        self.graph_upsert_service = GraphUpsertService(settings, self.neo4j_driver, self.graph_repository)
        self.lexical_retriever = LexicalRetriever(self.open_search_index_service)
        self.dense_retriever = DenseRetriever(self.open_search_index_service)
        self.graph_retriever = GraphRetriever(settings, self.neo4j_driver, self.cypher_template_registry)
        self.llm_client = create_llm_client(settings)
        self.knowledge_service = KnowledgeService(
            settings=settings,
            repository=self.knowledge_repository,
            storage=self.storage,
            parser_service=self.document_parse_service,
            chunk_build_service=self.chunk_build_service,
            entity_extraction_service=self.entity_extraction_service,
            open_search_index_service=self.open_search_index_service,
            graph_upsert_service=self.graph_upsert_service,
            graph_repository=self.graph_repository,
        )
        self.rag_orchestrator = RagOrchestrator(
            settings=settings,
            rag_repository=self.rag_repository,
            knowledge_repository=self.knowledge_repository,
            llm_client=self.llm_client,
            query_rewrite_service=QueryRewriteService(),
            intent_classifier_service=IntentClassifierService(),
            entity_linking_service=EntityLinkingService(self.graph_repository),
            lexical_retriever=self.lexical_retriever,
            dense_retriever=self.dense_retriever,
            graph_retriever=self.graph_retriever,
            fusion_service=FusionService(),
            rerank_service=RerankService(),
            citation_assembler=CitationAssembler(),
            refusal_policy_service=RefusalPolicyService(),
            answer_validator_service=AnswerValidatorService(),
        )
        self.rag_service = RagService(
            rag_orchestrator=self.rag_orchestrator,
            case_context_builder=CaseContextBuilder(),
        )
        self.rag_log_service = RagLogService(self.rag_repository)
        self.eval_service = EvalService(self.eval_repository, self.rag_service)
        self.eval_bootstrap_service = EvalBootstrapService(self.eval_repository)
        self.eval_bootstrap_service.bootstrap()

        # ── Phase 5A: model runtime ─────────────────────────────────────
        self.model_registry = ModelRegistry(settings)
        self.model_registry.startup()
        self.governance_bootstrap_service = GovernanceBootstrapService(
            settings,
            self.model_registry,
            self.governance_repository,
        )
        self.governance_bootstrap_service.bootstrap()

        self.quality_pipeline = QualityPipeline(self.model_registry, settings)
        self.detection_pipeline = DetectionPipeline(self.model_registry, settings)
        self.segmentation_pipeline = SegmentationPipeline(self.model_registry, settings)
        self.grading_pipeline = GradingPipeline(self.model_registry, settings)
        self.risk_pipeline = RiskPipeline(self.model_registry, settings)
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
            grading_pipeline=self.grading_pipeline,
            risk_pipeline=self.risk_pipeline,
            ai_runtime_repository=self.ai_runtime_repository,
        )


@lru_cache(maxsize=1)
def get_container() -> AppContainer:
    return AppContainer(Settings())
