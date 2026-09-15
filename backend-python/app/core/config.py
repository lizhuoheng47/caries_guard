import json
import logging
import os
from dataclasses import dataclass
from pathlib import Path

import yaml

log = logging.getLogger("cariesguard-ai.config")

_VALID_MODEL_IMPL_TYPES = {"HEURISTIC", "ML_MODEL"}
_VALID_QUALITY_FAIL_STRATEGIES = {"CONTINUE", "FAIL_FAST"}


def first_non_empty(*values: str | None, default: str = "") -> str:
    for raw in values:
        value = (raw or "").strip()
        if value and value != "...":
            return value
    return default


def bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    return default if value is None else value.strip().lower() in {"1", "true", "yes", "y", "on"}


def int_env(name: str, default: int) -> int:
    value = os.getenv(name)
    return default if value is None or not value.strip() else int(value)


def float_env(name: str, default: float) -> float:
    value = os.getenv(name)
    return default if value is None or not value.strip() else float(value)


def _require_non_empty(name: str, value: str) -> None:
    if not value or value.strip() in {"", "..."}:
        raise ValueError(f"Configuration {name} is required but missing or placeholder.")


def _validate_model_impl_type(name: str, raw: str) -> str:
    value = (raw or "").strip().upper()
    if value not in _VALID_MODEL_IMPL_TYPES:
        raise ValueError(f"{name}={raw!r} is invalid; allowed values: {sorted(_VALID_MODEL_IMPL_TYPES)}")
    return value


def _validate_quality_fail_strategy(raw: str) -> str:
    value = (raw or "").strip().upper()
    if value not in _VALID_QUALITY_FAIL_STRATEGIES:
        raise ValueError(
            f"CG_QUALITY_FAIL_STRATEGY={raw!r} is invalid; allowed values: {sorted(_VALID_QUALITY_FAIL_STRATEGIES)}"
        )
    return value


def _project_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _resolve_project_path(path_value: str) -> Path:
    path = Path(path_value)
    normalized = path.as_posix()
    if normalized.startswith("/app/"):
        return (_project_root() / normalized.removeprefix("/app/")).resolve()
    return path if path.is_absolute() else (_project_root() / path).resolve()


def _require_manifest_backed_model_assets(
    module_name: str,
    enabled: bool,
    impl_type: str,
    manifest_path_value: str,
) -> None:
    if not enabled or impl_type != "ML_MODEL":
        return
    from app.core.exceptions import ModelRuntimeException
    from app.infra.model.checkpoint_validator import CheckpointValidator
    from app.infra.model.manifest_loader import ManifestLoader

    try:
        manifest = ManifestLoader(_project_root()).load(module_name, manifest_path_value)
        class_map: dict = {}
        preprocess: dict = {}
        postprocess: dict = {}
        if manifest.class_map_path is not None and manifest.class_map_path.is_file():
            with manifest.class_map_path.open("r", encoding="utf-8") as stream:
                class_map = json.load(stream)
        if manifest.preprocess_path is not None and manifest.preprocess_path.is_file():
            with manifest.preprocess_path.open("r", encoding="utf-8") as stream:
                preprocess = yaml.safe_load(stream) or {}
        if manifest.postprocess_path is not None and manifest.postprocess_path.is_file():
            with manifest.postprocess_path.open("r", encoding="utf-8") as stream:
                postprocess = yaml.safe_load(stream) or {}
        validator = CheckpointValidator(module_name)
        validator.validate_manifest_assets(manifest, class_map=class_map, preprocess=preprocess, postprocess=postprocess)
        validator.validate_checkpoint_ready(manifest)
    except ModelRuntimeException as exc:
        raise ValueError(f"{exc.code} {exc.message}") from exc


@dataclass(frozen=True)
class Settings:
    app_env: str = os.getenv("CG_APP_ENV", "dev")
    app_mode: str = "full_chain"
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
    failed_routing_key: str = os.getenv("CG_ANALYSIS_FAILED_ROUTING_KEY", "analysis.failed")
    rabbit_retry_seconds: int = int_env("CG_RABBIT_RETRY_SECONDS", 5)

    callback_url: str = os.getenv(
        "CG_JAVA_CALLBACK_URL", "http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result"
    )
    callback_secret: str = os.getenv(
        "CG_ANALYSIS_CALLBACK_SECRET", "docker-change-me-to-a-strong-analysis-callback-secret"
    )
    callback_retry_count: int = int_env("CG_CALLBACK_RETRY_COUNT", 3)
    callback_visual_asset_mode: str = os.getenv("CG_CALLBACK_VISUAL_ASSET_MODE", "metadata")
    request_timeout_seconds: int = int_env("CG_REQUEST_TIMEOUT_SECONDS", 30)
    internal_api_key: str = os.getenv("CG_INTERNAL_API_KEY", "change-me-to-a-strong-internal-api-key")

    model_version: str = os.getenv("CG_MODEL_VERSION", "caries-v1")
    minio_endpoint: str = os.getenv("CG_MINIO_ENDPOINT", "http://minio:9000")
    # Signed links must use an address that the user's browser can reach.
    minio_public_endpoint: str = os.getenv(
        "CG_MINIO_PUBLIC_ENDPOINT",
        os.getenv("CG_MINIO_ENDPOINT", "http://127.0.0.1:9000"),
    )
    minio_access_key: str = os.getenv("CG_MINIO_ACCESS_KEY", "minioadmin")
    minio_secret_key: str = os.getenv("CG_MINIO_SECRET_KEY", "minioadmin")
    minio_secure: bool = bool_env("CG_MINIO_SECURE", False)
    minio_region: str = os.getenv("CG_MINIO_REGION", "")
    bucket_visual: str = os.getenv("CG_BUCKET_VISUAL", os.getenv("CG_MINIO_BUCKET_VISUAL", "caries-visual"))
    temp_dir: str = os.getenv("CG_TEMP_DIR", "/tmp/cariesguard")

    local_segmentation_api_enabled: bool = bool_env("CG_LOCAL_SEGMENTATION_API_ENABLED", False)
    local_segmentation_api_output_dir: str = os.getenv(
        "CG_LOCAL_SEGMENTATION_API_OUTPUT_DIR", str(_project_root() / "runtime-assets" / "segmentation")
    )
    local_segmentation_api_max_bytes: int = int_env("CG_LOCAL_SEGMENTATION_API_MAX_BYTES", 25 * 1024 * 1024)
    local_segmentation_asset_ttl_seconds: int = int_env("CG_LOCAL_SEGMENTATION_ASSET_TTL_SECONDS", 3600)
    segmentation_asset_url_expiry_seconds: int = int_env("CG_SEGMENTATION_ASSET_URL_EXPIRY_SECONDS", 900)

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

    qwen_vision_enabled: bool = bool_env("CG_QWEN_VISION_ENABLED", False)
    qwen_vision_model: str = os.getenv("CG_QWEN_VISION_MODEL", "qwen3-vl-plus")
    qwen_vision_base_url: str = first_non_empty(
        os.getenv("CG_QWEN_VISION_BASE_URL"), os.getenv("DASHSCOPE_BASE_URL"), default=""
    )
    qwen_vision_api_key: str = first_non_empty(
        os.getenv("CG_QWEN_VISION_API_KEY"), os.getenv("DASHSCOPE_API_KEY"), default=""
    )
    qwen_vision_timeout_seconds: int = int_env("CG_QWEN_VISION_TIMEOUT_SECONDS", 60)
    qwen_vision_temperature: float = float_env("CG_QWEN_VISION_TEMPERATURE", 0.1)

    # Minimal knowledge-grounded diagnostic support. The local retriever is usable
    # without an external LLM; enabling the LLM upgrades template generation to
    # grounded Qwen generation while retaining the same result contract.
    rag_enabled: bool = bool_env("CG_RAG_ENABLED", True)
    rag_knowledge_path: str = os.getenv(
        "CG_RAG_KNOWLEDGE_PATH", "knowledge-base/caries_guidance_v1.json"
    ).strip()
    rag_top_k: int = int_env("CG_RAG_TOP_K", 3)
    rag_llm_enabled: bool = bool_env("CG_RAG_LLM_ENABLED", False)
    rag_llm_model: str = os.getenv("CG_RAG_LLM_MODEL", os.getenv("CG_QWEN_VISION_MODEL", "qwen3-vl-plus"))
    rag_llm_base_url: str = first_non_empty(
        os.getenv("CG_RAG_LLM_BASE_URL"),
        os.getenv("CG_QWEN_VISION_BASE_URL"),
        os.getenv("DASHSCOPE_BASE_URL"),
        default="",
    )
    rag_llm_api_key: str = first_non_empty(
        os.getenv("CG_RAG_LLM_API_KEY"),
        os.getenv("CG_QWEN_VISION_API_KEY"),
        os.getenv("DASHSCOPE_API_KEY"),
        default="",
    )
    rag_llm_timeout_seconds: int = int_env("CG_RAG_LLM_TIMEOUT_SECONDS", 45)
    rag_llm_temperature: float = float_env("CG_RAG_LLM_TEMPERATURE", 0.1)

    model_quality_enabled: bool = bool_env("CG_MODEL_QUALITY_ENABLED", True)
    model_quality_impl_type: str = os.getenv("CG_MODEL_QUALITY_IMPL_TYPE", "HEURISTIC").upper()
    model_tooth_detect_enabled: bool = bool_env("CG_MODEL_TOOTH_DETECT_ENABLED", True)
    model_tooth_detect_impl_type: str = os.getenv("CG_MODEL_TOOTH_DETECT_IMPL_TYPE", "HEURISTIC").upper()
    model_tooth_detect_checkpoint_path: str = os.getenv("CG_MODEL_TOOTH_DETECT_CHECKPOINT_PATH", "").strip()
    model_tooth_detect_config_path: str = os.getenv("CG_MODEL_TOOTH_DETECT_CONFIG_PATH", "").strip()
    model_disease_detect_enabled: bool = bool_env("CG_MODEL_DISEASE_DETECT_ENABLED", False)
    model_disease_detect_checkpoint_path: str = os.getenv("CG_MODEL_DISEASE_DETECT_CHECKPOINT_PATH", "").strip()
    model_disease_detect_metadata_path: str = os.getenv("CG_MODEL_DISEASE_DETECT_METADATA_PATH", "").strip()
    model_disease_detect_image_size: int = int_env("CG_MODEL_DISEASE_DETECT_IMAGE_SIZE", 960)
    model_disease_detect_confidence_threshold: float = float_env("CG_MODEL_DISEASE_DETECT_CONFIDENCE_THRESHOLD", 0.25)
    model_segmentation_enabled: bool = bool_env("CG_MODEL_SEGMENTATION_ENABLED", True)
    model_segmentation_impl_type: str = os.getenv("CG_MODEL_SEGMENTATION_IMPL_TYPE", "ML_MODEL").upper()
    model_grading_enabled: bool = bool_env("CG_MODEL_GRADING_ENABLED", True)
    model_grading_impl_type: str = os.getenv("CG_MODEL_GRADING_IMPL_TYPE", "HEURISTIC").upper()
    model_risk_enabled: bool = bool_env("CG_MODEL_RISK_ENABLED", True)
    model_risk_impl_type: str = os.getenv("CG_MODEL_RISK_IMPL_TYPE", "HEURISTIC").upper()
    model_device: str = os.getenv("CG_MODEL_DEVICE", "cpu")
    model_weights_dir: str = os.getenv("CG_MODEL_WEIGHTS_DIR", "/app/model-weights")
    model_confidence_threshold: float = float_env("CG_MODEL_CONFIDENCE_THRESHOLD", 0.5)
    model_segmentation_manifest_path: str = os.getenv(
        "CG_MODEL_SEGMENTATION_MANIFEST_PATH", "assets/models/manifests/segmentation_v1.yaml"
    ).strip()
    model_grading_manifest_path: str = os.getenv(
        "CG_MODEL_GRADING_MANIFEST_PATH", "assets/models/manifests/grading_v1.yaml"
    ).strip()
    quality_model_param_path: str = os.getenv("CG_QUALITY_MODEL_PARAM_PATH", "").strip()
    quality_model_weights_path: str = os.getenv("CG_QUALITY_MODEL_WEIGHTS_PATH", "").strip()
    quality_fail_strategy: str = os.getenv("CG_QUALITY_FAIL_STRATEGY", "CONTINUE").upper()
    segmentation_force_fail: bool = bool_env("CG_SEGMENTATION_FORCE_FAIL", False)
    grading_force_fail: bool = bool_env("CG_GRADING_FORCE_FAIL", False)
    uncertainty_review_threshold: float = float_env("CG_UNCERTAINTY_REVIEW_THRESHOLD", 0.35)
    strict_model_startup_validation: bool = bool_env("CG_STRICT_MODEL_STARTUP_VALIDATION", False)

    def __post_init__(self) -> None:
        for attribute, env_name in (
            ("model_quality_impl_type", "CG_MODEL_QUALITY_IMPL_TYPE"),
            ("model_tooth_detect_impl_type", "CG_MODEL_TOOTH_DETECT_IMPL_TYPE"),
            ("model_segmentation_impl_type", "CG_MODEL_SEGMENTATION_IMPL_TYPE"),
            ("model_grading_impl_type", "CG_MODEL_GRADING_IMPL_TYPE"),
            ("model_risk_impl_type", "CG_MODEL_RISK_IMPL_TYPE"),
        ):
            object.__setattr__(self, attribute, _validate_model_impl_type(env_name, getattr(self, attribute)))
        object.__setattr__(self, "quality_fail_strategy", _validate_quality_fail_strategy(self.quality_fail_strategy))
        if self.qwen_vision_enabled:
            _require_non_empty("CG_QWEN_VISION_BASE_URL", self.qwen_vision_base_url)
            _require_non_empty("CG_QWEN_VISION_API_KEY", self.qwen_vision_api_key)
        if self.rag_enabled:
            _require_non_empty("CG_RAG_KNOWLEDGE_PATH", self.rag_knowledge_path)
            if self.rag_top_k < 1 or self.rag_top_k > 10:
                raise ValueError("CG_RAG_TOP_K must be between 1 and 10")
        if self.rag_llm_enabled:
            _require_non_empty("CG_RAG_LLM_BASE_URL", self.rag_llm_base_url)
            _require_non_empty("CG_RAG_LLM_API_KEY", self.rag_llm_api_key)
        if self.model_disease_detect_enabled:
            for name, value in (
                ("CG_MODEL_DISEASE_DETECT_CHECKPOINT_PATH", self.model_disease_detect_checkpoint_path),
                ("CG_MODEL_DISEASE_DETECT_METADATA_PATH", self.model_disease_detect_metadata_path),
            ):
                _require_non_empty(name, value)
                if self.strict_model_startup_validation and not _resolve_project_path(value).is_file():
                    raise ValueError(f"Configuration {name} does not point to a readable file: {value}")
        if self.strict_model_startup_validation:
            _require_manifest_backed_model_assets(
                "segmentation", self.model_segmentation_enabled, self.model_segmentation_impl_type, self.model_segmentation_manifest_path
            )
            _require_manifest_backed_model_assets(
                "grading", self.model_grading_enabled, self.model_grading_impl_type, self.model_grading_manifest_path
            )
        log.info("CariesGuard runtime pipeline=FULL_CHAIN")

    def build_mysql_url(self) -> str:
        return (
            f"mysql+pymysql://{self.mysql_username}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}?charset=utf8mb4"
        )
