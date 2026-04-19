from __future__ import annotations

from app.core.config import Settings
from app.infra.rerank.base_rerank_provider import BaseRerankProvider
from app.infra.rerank.embedding_rerank_provider import EmbeddingSimilarityRerankProvider
from app.infra.rerank.heuristic_rerank_provider import HeuristicRerankProvider
from app.infra.vector.base_embedding_provider import BaseEmbeddingProvider


def create_rerank_provider(settings: Settings, embedding_provider: BaseEmbeddingProvider) -> BaseRerankProvider:
    provider = settings.rerank_provider.upper()
    if provider in {"EMBEDDING", "SEMANTIC"}:
        return EmbeddingSimilarityRerankProvider(settings, embedding_provider)
    if provider in {"HEURISTIC", "TOKEN_OVERLAP"}:
        return HeuristicRerankProvider(model_name=settings.rerank_model_name, version="v1")
    raise ValueError(f"Unsupported rerank provider: {settings.rerank_provider}")
