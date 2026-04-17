import os
from dataclasses import dataclass, field


_VALID_RUNTIME_MODES = {"mock", "hybrid", "real"}


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


def _validate_runtime_mode(raw: str) -> str:
    mode = raw.strip().lower()
    if mode not in _VALID_RUNTIME_MODES:
        raise ValueError(
            f"CG_AI_RUNTIME_MODE={raw!r} is invalid; "
            f"allowed values: {sorted(_VALID_RUNTIME_MODES)}"
        )
    return mode


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
    rag_knowledge_version: str = os.getenv("CG_RAG_KNOWLEDGE_VERSION", "v1.0")
    rag_embedding_model: str = os.getenv("CG_RAG_EMBEDDING_MODEL", "hashing-embedding-v1")
    rag_vector_store_type: str = os.getenv("CG_RAG_VECTOR_STORE_TYPE", "LOCAL_JSON")
    rag_top_k: int = int_env("CG_RAG_TOP_K", 5)
    llm_provider_code: str = os.getenv("CG_LLM_PROVIDER_CODE", "MOCK")
    llm_model_name: str = os.getenv("CG_LLM_MODEL_NAME", "template-llm-v1")

    # ── Phase 5: Model Runtime ──────────────────────────────────────────
    ai_runtime_mode: str = _validate_runtime_mode(os.getenv("CG_AI_RUNTIME_MODE", "mock"))
    model_quality_enabled: bool = bool_env("CG_MODEL_QUALITY_ENABLED", False)
    model_tooth_detect_enabled: bool = bool_env("CG_MODEL_TOOTH_DETECT_ENABLED", False)
    model_segmentation_enabled: bool = bool_env("CG_MODEL_SEGMENTATION_ENABLED", False)
    model_grading_enabled: bool = bool_env("CG_MODEL_GRADING_ENABLED", False)
    model_risk_enabled: bool = bool_env("CG_MODEL_RISK_ENABLED", False)
    model_device: str = os.getenv("CG_MODEL_DEVICE", "cpu")
    model_weights_dir: str = os.getenv("CG_MODEL_WEIGHTS_DIR", "/app/model-weights")
    model_confidence_threshold: float = float_env("CG_MODEL_CONFIDENCE_THRESHOLD", 0.5)
    segmentation_force_fail: bool = bool_env("CG_SEGMENTATION_FORCE_FAIL", False)
    uncertainty_review_threshold: float = float_env("CG_UNCERTAINTY_REVIEW_THRESHOLD", 0.35)

    def __post_init__(self) -> None:
        object.__setattr__(self, "ai_runtime_mode", _validate_runtime_mode(self.ai_runtime_mode))

    def build_mysql_url(self) -> str:
        return (
            f"mysql+pymysql://{self.mysql_username}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}?charset=utf8mb4"
        )
