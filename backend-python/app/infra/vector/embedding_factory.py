from __future__ import annotations

from app.core.config import Settings
from app.infra.vector.base_embedding_provider import BaseEmbeddingProvider
from app.infra.vector.hashing_embedder import HashingEmbedder
from app.infra.vector.real_embedding_provider import OpenAiCompatibleEmbeddingProvider


def create_embedding_provider(settings: Settings) -> BaseEmbeddingProvider:
    provider = settings.rag_embedding_provider.upper()
    if provider in {"HASHING", "LOCAL_HASH"}:
        return HashingEmbedder(settings.rag_embedding_dimension, model_name=settings.rag_embedding_model, version=settings.rag_embedding_version)
    if provider in {"OPENAI", "OPENAI_COMPATIBLE", "DASHSCOPE", "DEEPSEEK", "QWEN"}:
        return OpenAiCompatibleEmbeddingProvider(settings)
    raise ValueError(f"Unsupported embedding provider: {settings.rag_embedding_provider}")
