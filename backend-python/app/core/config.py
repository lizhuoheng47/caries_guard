import os
import json
from dataclasses import dataclass, field


_VALID_RUNTIME_MODES = {"mock", "hybrid", "real"}
_VALID_VECTOR_STORE_TYPES = {"LOCAL_JSON", "OPENSEARCH"}
_VALID_MODEL_IMPL_TYPES = {"MOCK", "HEURISTIC", "ML_MODEL"}


def bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


def int_env(name: str, default: int) -> int:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return default
    return int(value)


def float_env(name: str, default: float) -> float:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return default
    return float(value)


def csv_env(name: str, default: list[str]) -> list[str]:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return list(default)
    return [item.strip() for item in value.split(",") if item.strip()]


def json_env(name: str, default: dict[str, float]) -> dict[str, float]:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return dict(default)
    loaded = json.loads(value)
    return {str(key): float(item) for key, item in loaded.items()}


def _validate_runtime_mode(raw: str) -> str:
    mode = raw.strip().lower()
    if mode not in _VALID_RUNTIME_MODES:
        raise ValueError(
            f"CG_AI_RUNTIME_MODE={raw!r} is invalid; "
            f"allowed values: {sorted(_VALID_RUNTIME_MODES)}"
        )
    return mode


def _validate_vector_store_type(raw: str) -> str:
    value = raw.strip().upper()
    if value not in _VALID_VECTOR_STORE_TYPES:
        raise ValueError(
            f"CG_RAG_VECTOR_STORE_TYPE={raw!r} is invalid; "
            f"allowed values: {sorted(_VALID_VECTOR_STORE_TYPES)}"
        )
    return value


def _require_non_empty(name: str, value: str) -> None:
    if not value or value.strip() == "" or value.strip() == "...":
        raise ValueError(f"Configuration {name} is required but missing or placeholder.")


def _require_model_weights_if_enabled(weights_dir: str, model_name: str, enabled: bool, impl_type: str) -> None:
    if not enabled or impl_type != "ML_MODEL":
        return
    path = os.path.join(weights_dir, model_name)
    if not os.path.exists(path):
        raise ValueError(f"Model {model_name} is enabled with ML_MODEL type, but weights are missing at {path}")


def _validate_model_impl_type(name: str, raw: str) -> str:
    value = (raw or "").strip().upper()
    if value not in _VALID_MODEL_IMPL_TYPES:
        raise ValueError(
            f"{name}={raw!r} is invalid; allowed values: {sorted(_VALID_MODEL_IMPL_TYPES)}"
        )
    return value


@dataclass(frozen=True)
class Settings:
    app_env: str = os.getenv("CG_APP_ENV", "dev")
    app_mode: str = os.getenv("CG_APP_MODE", "mock")
    http_enabled: bool = bool_env("CG_HTTP_ENABLED", True)
    http_host: str = os.getenv("CG_HTTP_HOST", "0.0.0.0")
    http_port: int = int_env("CG_HTTP_PORT", 8001)
    mq_worker_enabled: bool = bool_env("CG_MQ_WORKER_ENABLED", True)

    rabbit_host: str = os.getenv("CG_RABBIT_HOST", "rabbitmq")
    rabbit_port: int = int_env("CG_RABBIT_PORT", 5672)
    rabbit_username: str = os.getenv("CG_RABBIT_USERNAME", "guest")
    rabbit_password: str = os.getenv("CG_RABBIT_PASSWORD", "guest")
    analysis_exchange: str = os.getenv("CG_ANALYSIS_EXCHANGE", "caries.analysis.exchange")
    requested_queue: str = os.getenv("CG_ANALYSIS_REQUESTED_QUEUE", "caries.analysis.requested.queue")
    requested_routing_key: str = os.getenv("CG_ANALYSIS_REQUESTED_ROUTING_KEY", "analysis.requested")
    rabbit_retry_seconds: int = int_env("CG_RABBIT_RETRY_SECONDS", 5)

    callback_url: str = os.getenv(
        "CG_JAVA_CALLBACK_URL",
        "http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result",
    )
    callback_secret: str = os.getenv(
        "CG_ANALYSIS_CALLBACK_SECRET",
        "docker-change-me-to-a-strong-analysis-callback-secret",
    )
    callback_retry_count: int = int_env("CG_CALLBACK_RETRY_COUNT", 3)
    callback_visual_asset_mode: str = os.getenv("CG_CALLBACK_VISUAL_ASSET_MODE", "metadata")
    request_timeout_seconds: int = int_env("CG_REQUEST_TIMEOUT_SECONDS", 30)

    model_version: str = os.getenv("CG_MODEL_VERSION", "caries-v1")
    download_images: bool = bool_env("CG_AI_DOWNLOAD_IMAGES", True)

    minio_endpoint: str = os.getenv("CG_MINIO_ENDPOINT", "http://minio:9000")
    minio_access_key: str = os.getenv("CG_MINIO_ACCESS_KEY", "minioadmin")
    minio_secret_key: str = os.getenv("CG_MINIO_SECRET_KEY", "minioadmin")
    minio_secure: bool = bool_env("CG_MINIO_SECURE", False)
    minio_region: str = os.getenv("CG_MINIO_REGION", "")
    minio_connect_timeout_seconds: int = int_env("CG_MINIO_CONNECT_TIMEOUT_SECONDS", 5)
    minio_read_timeout_seconds: int = int_env("CG_MINIO_READ_TIMEOUT_SECONDS", 30)
    bucket_image: str = os.getenv("CG_BUCKET_IMAGE", os.getenv("CG_MINIO_BUCKET_IMAGE", "caries-image"))
    bucket_visual: str = os.getenv("CG_BUCKET_VISUAL", os.getenv("CG_MINIO_BUCKET_VISUAL", "caries-visual"))
    bucket_report: str = os.getenv("CG_BUCKET_REPORT", "caries-report")
    bucket_export: str = os.getenv("CG_BUCKET_EXPORT", "caries-export")
    bucket_knowledge: str = os.getenv("CG_BUCKET_KNOWLEDGE", "caries-knowledge")
    temp_dir: str = os.getenv("CG_TEMP_DIR", "/tmp/cariesguard")

    allow_bucket_create: bool = bool_env("CG_MINIO_ALLOW_BUCKET_CREATE", False)

    mysql_host: str = os.getenv("CG_MYSQL_HOST", os.getenv("CARIES_MYSQL_HOST", "mysql"))
    mysql_port: int = int_env("CG_MYSQL_PORT", int_env("CARIES_MYSQL_PORT", 3306))
    mysql_database: str = os.getenv("CG_MYSQL_DATABASE", "caries_ai")
    mysql_username: str = os.getenv("CG_MYSQL_USERNAME", os.getenv("CARIES_MYSQL_USERNAME", "root"))
    mysql_password: str = os.getenv("CG_MYSQL_PASSWORD", os.getenv("CARIES_MYSQL_PASSWORD", "1234"))
    mysql_connect_timeout_seconds: int = int_env("CG_MYSQL_CONNECT_TIMEOUT_SECONDS", 5)
    db_pool_size: int = int_env("CG_DB_POOL_SIZE", 5)
    db_max_overflow: int = int_env("CG_DB_MAX_OVERFLOW", 10)
    db_pool_recycle_seconds: int = int_env("CG_DB_POOL_RECYCLE_SECONDS", 1800)
    db_echo: bool = bool_env("CG_DB_ECHO", False)
    db_schema_bootstrap_enabled: bool = bool_env("CG_DB_SCHEMA_BOOTSTRAP_ENABLED", True)
    db_migration_enabled: bool = bool_env("CG_DB_MIGRATION_ENABLED", False)
    rag_index_dir: str = os.getenv("CG_RAG_INDEX_DIR", "/tmp/cariesguard/vector-index")
    rag_default_kb_code: str = os.getenv("CG_RAG_DEFAULT_KB_CODE", "caries-default")
    rag_default_kb_name: str = os.getenv("CG_RAG_DEFAULT_KB_NAME", "CariesGuard Default Knowledge Base")
    rag_runtime_enabled: bool = bool_env("CG_RAG_RUNTIME_ENABLED", True)
    rag_knowledge_version: str = os.getenv("CG_RAG_KNOWLEDGE_VERSION", "v1.0")
    rag_embedding_model: str = os.getenv("CG_RAG_EMBEDDING_MODEL", "text-embedding-3-small")
    rag_embedding_provider: str = os.getenv("CG_RAG_EMBEDDING_PROVIDER", "OPENAI_COMPATIBLE").strip().upper()
    rag_embedding_dimension: int = int_env("CG_RAG_EMBEDDING_DIMENSION", 256)
    rag_embedding_version: str = os.getenv("CG_RAG_EMBEDDING_VERSION", "2026-04")
    rag_embedding_base_url: str = os.getenv("CG_RAG_EMBEDDING_BASE_URL", os.getenv("CG_LLM_BASE_URL", ""))
    rag_embedding_api_key: str = os.getenv("CG_RAG_EMBEDDING_API_KEY", os.getenv("CG_LLM_API_KEY", ""))
    rag_embedding_batch_size: int = int_env("CG_RAG_EMBEDDING_BATCH_SIZE", 16)
    rag_embedding_timeout_seconds: int = int_env("CG_RAG_EMBEDDING_TIMEOUT_SECONDS", 30)
    rag_vector_store_type: str = _validate_vector_store_type(os.getenv("CG_RAG_VECTOR_STORE_TYPE", "OPENSEARCH"))
    rag_top_k: int = int_env("CG_RAG_TOP_K", 5)
    lexical_top_k: int = int_env("CG_RAG_LEXICAL_TOP_K", 20)
    dense_top_k: int = int_env("CG_RAG_DENSE_TOP_K", 20)
    graph_top_k: int = int_env("CG_RAG_GRAPH_TOP_K", 10)
    fusion_top_k: int = int_env("CG_RAG_FUSION_TOP_K", 12)
    rerank_top_k: int = int_env("CG_RAG_RERANK_TOP_K", 8)
    answer_evidence_top_k: int = int_env("CG_RAG_ANSWER_EVIDENCE_TOP_K", 6)
    rag_evidence_min_count: int = int_env("CG_RAG_EVIDENCE_MIN_COUNT", 2)
    rag_evidence_min_distinct_docs: int = int_env("CG_RAG_EVIDENCE_MIN_DISTINCT_DOCS", 1)
    rag_rebuild_graph_enabled: bool = bool_env("CG_RAG_REBUILD_GRAPH_ENABLED", True)
    rag_rebuild_parse_enabled: bool = bool_env("CG_RAG_REBUILD_PARSE_ENABLED", True)
    rag_rebuild_cleanup_enabled: bool = bool_env("CG_RAG_REBUILD_CLEANUP_ENABLED", True)
    rag_rebuild_lexical_enabled: bool = bool_env("CG_RAG_REBUILD_LEXICAL_ENABLED", True)
    rag_rebuild_dense_enabled: bool = bool_env("CG_RAG_REBUILD_DENSE_ENABLED", True)
    rag_eval_enabled: bool = bool_env("CG_RAG_EVAL_ENABLED", True)
    rag_channel_weights: dict[str, float] = field(
        default_factory=lambda: json_env(
            "CG_RAG_CHANNEL_WEIGHTS",
            {
                "LEXICAL": 1.0,
                "DENSE": 1.15,
                "GRAPH": 1.2,
            },
        )
    )
    rag_source_authority_weights: dict[str, float] = field(
        default_factory=lambda: json_env(
            "CG_RAG_SOURCE_AUTHORITY_WEIGHTS",
            {
                "GUIDELINE": 1.0,
                "MANUAL": 0.9,
                "INTERNAL": 0.75,
                "UPLOAD": 0.7,
            },
        )
    )
    rag_freshness_decay_days: int = int_env("CG_RAG_FRESHNESS_DECAY_DAYS", 365)
    rag_graph_confidence_weight: float = float_env("CG_RAG_GRAPH_CONFIDENCE_WEIGHT", 0.2)
    rerank_provider: str = os.getenv("CG_RAG_RERANK_PROVIDER", "EMBEDDING").strip().upper()
    rerank_model_name: str = os.getenv("CG_RAG_RERANK_MODEL_NAME", "embedding-similarity-reranker")
    rerank_base_url: str = os.getenv("CG_RAG_RERANK_BASE_URL", os.getenv("CG_LLM_BASE_URL", ""))
    rerank_api_key: str = os.getenv("CG_RAG_RERANK_API_KEY", os.getenv("CG_LLM_API_KEY", ""))
    rerank_timeout_seconds: int = int_env("CG_RAG_RERANK_TIMEOUT_SECONDS", 20)
    rerank_semantic_weight: float = float_env("CG_RAG_RERANK_SEMANTIC_WEIGHT", 0.7)
    rerank_lexical_weight: float = float_env("CG_RAG_RERANK_LEXICAL_WEIGHT", 0.2)
    rerank_prior_weight: float = float_env("CG_RAG_RERANK_PRIOR_WEIGHT", 0.1)
    opensearch_hosts: list[str] = field(default_factory=lambda: csv_env("CG_OPENSEARCH_HOSTS", ["http://127.0.0.1:9200"]))
    opensearch_username: str = os.getenv("CG_OPENSEARCH_USERNAME", "")
    opensearch_password: str = os.getenv("CG_OPENSEARCH_PASSWORD", "")
    opensearch_verify_certs: bool = bool_env("CG_OPENSEARCH_VERIFY_CERTS", False)
    opensearch_chunk_index: str = os.getenv("CG_OPENSEARCH_CHUNK_INDEX", "kb_chunk_index")
    opensearch_doc_index: str = os.getenv("CG_OPENSEARCH_DOC_INDEX", "kb_doc_index")
    neo4j_uri: str = os.getenv("CG_NEO4J_URI", "bolt://127.0.0.1:7687")
    neo4j_username: str = os.getenv("CG_NEO4J_USERNAME", "neo4j")
    neo4j_password: str = os.getenv("CG_NEO4J_PASSWORD", "cariesguard")
    neo4j_database: str = os.getenv("CG_NEO4J_DATABASE", "neo4j")
    llm_provider_code: str = os.getenv("CG_LLM_PROVIDER_CODE", "OPENAI_COMPATIBLE")
    llm_model_name: str = os.getenv("CG_LLM_MODEL_NAME", "gpt-4o-mini")
    llm_base_url: str = os.getenv("CG_LLM_BASE_URL", "https://api.openai.com/v1")
    llm_api_key: str = os.getenv("CG_LLM_API_KEY", "")
    llm_timeout_seconds: int = int_env("CG_LLM_TIMEOUT_SECONDS", 30)
    llm_retry_count: int = int_env("CG_LLM_RETRY_COUNT", 1)
    llm_temperature: float = float_env("CG_LLM_TEMPERATURE", 0.2)
    llm_enable_fallback_mock: bool = bool_env("CG_LLM_ENABLE_FALLBACK_MOCK", True)
    qwen_vision_enabled: bool = bool_env("CG_QWEN_VISION_ENABLED", False)
    qwen_vision_model: str = os.getenv("CG_QWEN_VISION_MODEL", "qwen3-vl-plus")
    qwen_vision_base_url: str = os.getenv("CG_QWEN_VISION_BASE_URL", os.getenv("CG_LLM_BASE_URL", ""))
    qwen_vision_api_key: str = os.getenv("CG_QWEN_VISION_API_KEY", os.getenv("CG_LLM_API_KEY", ""))
    qwen_vision_timeout_seconds: int = int_env("CG_QWEN_VISION_TIMEOUT_SECONDS", int_env("CG_LLM_TIMEOUT_SECONDS", 60))
    qwen_vision_temperature: float = float_env("CG_QWEN_VISION_TEMPERATURE", 0.1)
    analysis_kb_enhancement_enabled: bool = bool_env("CG_ANALYSIS_KB_ENHANCEMENT_ENABLED", False)
    analysis_kb_code: str = os.getenv("CG_ANALYSIS_KB_CODE", os.getenv("CG_RAG_DEFAULT_KB_CODE", "caries-default"))

    # ── Phase 5: Model Runtime ──────────────────────────────────────────
    ai_runtime_mode: str = _validate_runtime_mode(os.getenv("CG_AI_RUNTIME_MODE", "mock"))
    model_quality_enabled: bool = bool_env("CG_MODEL_QUALITY_ENABLED", False)
    model_quality_impl_type: str = os.getenv("CG_MODEL_QUALITY_IMPL_TYPE", "HEURISTIC").upper()
    model_tooth_detect_enabled: bool = bool_env("CG_MODEL_TOOTH_DETECT_ENABLED", False)
    model_tooth_detect_impl_type: str = os.getenv("CG_MODEL_TOOTH_DETECT_IMPL_TYPE", "HEURISTIC").upper()
    model_segmentation_enabled: bool = bool_env("CG_MODEL_SEGMENTATION_ENABLED", False)
    model_segmentation_impl_type: str = os.getenv("CG_MODEL_SEGMENTATION_IMPL_TYPE", "HEURISTIC").upper()
    model_grading_enabled: bool = bool_env("CG_MODEL_GRADING_ENABLED", False)
    model_grading_impl_type: str = os.getenv("CG_MODEL_GRADING_IMPL_TYPE", "HEURISTIC").upper()
    model_risk_enabled: bool = bool_env("CG_MODEL_RISK_ENABLED", False)
    model_risk_impl_type: str = os.getenv("CG_MODEL_RISK_IMPL_TYPE", "HEURISTIC").upper()
    model_device: str = os.getenv("CG_MODEL_DEVICE", "cpu")
    model_weights_dir: str = os.getenv("CG_MODEL_WEIGHTS_DIR", "/app/model-weights")
    model_confidence_threshold: float = float_env("CG_MODEL_CONFIDENCE_THRESHOLD", 0.5)
    segmentation_force_fail: bool = bool_env("CG_SEGMENTATION_FORCE_FAIL", False)
    grading_force_fail: bool = bool_env("CG_GRADING_FORCE_FAIL", False)
    uncertainty_review_threshold: float = float_env("CG_UNCERTAINTY_REVIEW_THRESHOLD", 0.35)

    def __post_init__(self) -> None:
        mode = _validate_runtime_mode(self.ai_runtime_mode)
        object.__setattr__(self, "ai_runtime_mode", mode)
        object.__setattr__(self, "rag_vector_store_type", _validate_vector_store_type(self.rag_vector_store_type))
        object.__setattr__(self, "model_quality_impl_type", _validate_model_impl_type("CG_MODEL_QUALITY_IMPL_TYPE", self.model_quality_impl_type))
        object.__setattr__(self, "model_tooth_detect_impl_type", _validate_model_impl_type("CG_MODEL_TOOTH_DETECT_IMPL_TYPE", self.model_tooth_detect_impl_type))
        object.__setattr__(self, "model_segmentation_impl_type", _validate_model_impl_type("CG_MODEL_SEGMENTATION_IMPL_TYPE", self.model_segmentation_impl_type))
        object.__setattr__(self, "model_grading_impl_type", _validate_model_impl_type("CG_MODEL_GRADING_IMPL_TYPE", self.model_grading_impl_type))
        object.__setattr__(self, "model_risk_impl_type", _validate_model_impl_type("CG_MODEL_RISK_IMPL_TYPE", self.model_risk_impl_type))

        # ── Fail-fast Validation ──
        if mode == "real":
            if self.llm_provider_code == "MOCK":
                raise ValueError("CG_AI_RUNTIME_MODE='real' forbids CG_LLM_PROVIDER_CODE='MOCK'")
            if self.rag_embedding_provider == "HASHING":
                raise ValueError("CG_AI_RUNTIME_MODE='real' forbids CG_RAG_EMBEDDING_PROVIDER='HASHING'")
            if self.qwen_vision_enabled:
                _require_non_empty("CG_QWEN_VISION_BASE_URL", self.qwen_vision_base_url)
                _require_non_empty("CG_QWEN_VISION_API_KEY", self.qwen_vision_api_key)
                _require_non_empty("CG_QWEN_VISION_MODEL", self.qwen_vision_model)

        rag_dependencies_required = self.rag_runtime_enabled or self.analysis_kb_enhancement_enabled
        if self.analysis_kb_enhancement_enabled and not self.rag_runtime_enabled:
            raise ValueError("CG_ANALYSIS_KB_ENHANCEMENT_ENABLED=true requires CG_RAG_RUNTIME_ENABLED=true")

        if rag_dependencies_required and self.llm_provider_code != "MOCK":
            _require_non_empty("CG_LLM_BASE_URL", self.llm_base_url)
            _require_non_empty("CG_LLM_API_KEY", self.llm_api_key)

        if rag_dependencies_required and self.rag_embedding_provider != "HASHING":
            _require_non_empty("CG_RAG_EMBEDDING_BASE_URL", self.rag_embedding_base_url)
            _require_non_empty("CG_RAG_EMBEDDING_API_KEY", self.rag_embedding_api_key)

        if rag_dependencies_required and self.rag_vector_store_type == "OPENSEARCH":
            if not self.opensearch_hosts:
                raise ValueError("CG_RAG_VECTOR_STORE_TYPE='OPENSEARCH' requires CG_OPENSEARCH_HOSTS")

        modules = [
            ("quality", self.model_quality_enabled, self.model_quality_impl_type, "CG_MODEL_QUALITY_IMPL_TYPE"),
            ("tooth_detect", self.model_tooth_detect_enabled, self.model_tooth_detect_impl_type, "CG_MODEL_TOOTH_DETECT_IMPL_TYPE"),
            ("segmentation", self.model_segmentation_enabled, self.model_segmentation_impl_type, "CG_MODEL_SEGMENTATION_IMPL_TYPE"),
            ("grading", self.model_grading_enabled, self.model_grading_impl_type, "CG_MODEL_GRADING_IMPL_TYPE"),
            ("risk", self.model_risk_enabled, self.model_risk_impl_type, "CG_MODEL_RISK_IMPL_TYPE"),
        ]
        for module_name, enabled, impl_type, env_name in modules:
            if mode == "real" and impl_type == "MOCK":
                raise ValueError(f"{env_name}='MOCK' is forbidden when CG_AI_RUNTIME_MODE='real'")
            _require_model_weights_if_enabled(
                self.model_weights_dir,
                module_name,
                mode == "real" or enabled,
                impl_type,
            )

        # Logging summary
        print(f"[*] CariesGuard Runtime Mode: {mode.upper()}")
        print(f"[*] LLM Provider: {self.llm_provider_code} (Model: {self.llm_model_name})")
        if self.qwen_vision_enabled:
            print(f"[*] Qwen Vision: ENABLED (Model: {self.qwen_vision_model})")
        if self.analysis_kb_enhancement_enabled:
            print(f"[*] Analysis KB Enhancement: ENABLED (KB: {self.analysis_kb_code})")
        if self.rag_runtime_enabled:
            print(f"[*] Embedding: {self.rag_embedding_provider} (Store: {self.rag_vector_store_type})")
        else:
            print("[*] RAG Runtime: DISABLED")
        enabled_mods = [m for m, e, t in [
            ("Quality", self.model_quality_enabled, self.model_quality_impl_type),
            ("Detect", self.model_tooth_detect_enabled, self.model_tooth_detect_impl_type),
            ("Segment", self.model_segmentation_enabled, self.model_segmentation_impl_type),
            ("Grading", self.model_grading_enabled, self.model_grading_impl_type),
            ("Risk", self.model_risk_enabled, self.model_risk_impl_type)
        ] if e]
        print(f"[*] Enabled Modules: {', '.join([f'{m}({self._get_impl_type(m)})' for m in enabled_mods]) if enabled_mods else 'None'}")

    def _get_impl_type(self, module_name: str) -> str:
        mapping = {
            "Quality": self.model_quality_impl_type,
            "Detect": self.model_tooth_detect_impl_type,
            "Segment": self.model_segmentation_impl_type,
            "Grading": self.model_grading_impl_type,
            "Risk": self.model_risk_impl_type
        }
        return mapping.get(module_name, "UNKNOWN")

    def build_mysql_url(self) -> str:
        return (
            f"mysql+pymysql://{self.mysql_username}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}?charset=utf8mb4"
        )
