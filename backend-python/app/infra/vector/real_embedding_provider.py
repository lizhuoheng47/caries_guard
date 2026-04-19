from __future__ import annotations

import requests

from app.core.config import Settings
from app.infra.vector.base_embedding_provider import BaseEmbeddingProvider, EmbeddingMetadata


class OpenAiCompatibleEmbeddingProvider(BaseEmbeddingProvider):
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._metadata = EmbeddingMetadata(
            provider=settings.rag_embedding_provider,
            model=settings.rag_embedding_model,
            dimension=settings.rag_embedding_dimension,
            version=settings.rag_embedding_version,
        )

    @property
    def metadata(self) -> EmbeddingMetadata:
        return self._metadata

    def embed(self, text: str) -> list[float]:
        return self.embed_many([text])[0]

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        if not self.settings.rag_embedding_base_url:
            raise RuntimeError("CG_RAG_EMBEDDING_BASE_URL is required for real embedding provider")
        url = self.settings.rag_embedding_base_url.rstrip("/") + "/embeddings"
        headers = {"Content-Type": "application/json"}
        if self.settings.rag_embedding_api_key:
            headers["Authorization"] = f"Bearer {self.settings.rag_embedding_api_key}"
        response = requests.post(
            url,
            json={"model": self.settings.rag_embedding_model, "input": texts},
            headers=headers,
            timeout=self.settings.rag_embedding_timeout_seconds,
        )
        response.raise_for_status()
        payload = response.json()
        items = payload.get("data") or []
        vectors = [item.get("embedding") for item in items]
        if len(vectors) != len(texts):
            raise RuntimeError("embedding provider returned unexpected vector count")
        return [self._normalize(vector) for vector in vectors]

    def _normalize(self, vector: list[float]) -> list[float]:
        values = [float(item) for item in vector[: self.settings.rag_embedding_dimension]]
        if len(values) < self.settings.rag_embedding_dimension:
            values.extend([0.0] * (self.settings.rag_embedding_dimension - len(values)))
        return values
