import os
from dataclasses import dataclass


def _bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


@dataclass(frozen=True)
class Settings:
    rabbit_host: str = os.getenv("CG_RABBIT_HOST", "rabbitmq")
    rabbit_port: int = int(os.getenv("CG_RABBIT_PORT", "5672"))
    rabbit_username: str = os.getenv("CG_RABBIT_USERNAME", "guest")
    rabbit_password: str = os.getenv("CG_RABBIT_PASSWORD", "guest")
    analysis_exchange: str = os.getenv("CG_ANALYSIS_EXCHANGE", "caries.analysis.exchange")
    requested_queue: str = os.getenv("CG_ANALYSIS_REQUESTED_QUEUE", "caries.analysis.requested.queue")
    requested_routing_key: str = os.getenv("CG_ANALYSIS_REQUESTED_ROUTING_KEY", "analysis.requested")
    callback_url: str = os.getenv(
        "CG_JAVA_CALLBACK_URL",
        "http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result",
    )
    callback_secret: str = os.getenv(
        "CG_ANALYSIS_CALLBACK_SECRET",
        "docker-change-me-to-a-strong-analysis-callback-secret",
    )
    model_version: str = os.getenv("CG_MODEL_VERSION", "caries-v1")
    download_images: bool = _bool_env("CG_AI_DOWNLOAD_IMAGES", True)
    request_timeout_seconds: int = int(os.getenv("CG_REQUEST_TIMEOUT_SECONDS", "30"))
    rabbit_retry_seconds: int = int(os.getenv("CG_RABBIT_RETRY_SECONDS", "5"))
    callback_retry_count: int = int(os.getenv("CG_CALLBACK_RETRY_COUNT", "3"))
    minio_endpoint: str = os.getenv("CG_MINIO_ENDPOINT", "http://minio:9000")
    minio_access_key: str = os.getenv("CG_MINIO_ACCESS_KEY", "minioadmin")
    minio_secret_key: str = os.getenv("CG_MINIO_SECRET_KEY", "minioadmin")
    minio_bucket_visual: str = os.getenv("CG_MINIO_BUCKET_VISUAL", "caries-visual")