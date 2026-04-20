from __future__ import annotations

import time
import requests
from typing import Any

from app.core.config import Settings
from app.infra.vector.base_embedding_provider import BaseEmbeddingProvider, EmbeddingMetadata


class OpenAiCompatibleEmbeddingProvider(BaseEmbeddingProvider):
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._metadata = EmbeddingMetadata(
            provider="OPENAI_COMPATIBLE",
            model=settings.rag_embedding_model,
            dimension=settings.rag_embedding_dimension,
            version=settings.rag_embedding_version,
        )

    @property
    def metadata(self) -> EmbeddingMetadata:
        return self._metadata

    def embed(self, text: str) -> list[float]:
        results = self.embed_many([text])
        return results[0]

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []

        payload = {
            "model": self.settings.rag_embedding_model,
            "input": texts,
        }
        
        response_data = self._post_with_retry(payload)
        
        data = response_data.get("data", [])
        if not data:
            raise RuntimeError("Embedding provider returned empty data")
            
        # OpenAI returns data sorted by index, but it's safer to sort it explicitly if we want to be robust
        # However, usually we just extract them.
        results = [None] * len(texts)
        for item in data:
            index = item.get("index")
            embedding = item.get("embedding")
            if index is not None and embedding is not None:
                if len(embedding) != self.metadata.dimension:
                    raise ValueError(
                        f"Embedding dimension mismatch: expected {self.metadata.dimension}, "
                        f"got {len(embedding)}"
                    )
                results[index] = embedding
        
        if any(r is None for r in results):
            raise RuntimeError("Embedding provider returned incomplete data")
            
        return results # type: ignore

    def _post_with_retry(self, payload: dict[str, Any]) -> dict[str, Any]:
        if not self.settings.rag_embedding_base_url:
            raise RuntimeError("CG_RAG_EMBEDDING_BASE_URL is required for non-HASHING embedding provider")
            
        url = self.settings.rag_embedding_base_url.rstrip("/") + "/embeddings"
        headers = {"Content-Type": "application/json"}
        if self.settings.rag_embedding_api_key:
            headers["Authorization"] = f"Bearer {self.settings.rag_embedding_api_key}"
            
        last_error: Exception | None = None
        # Use same retry logic as LLM for consistency
        retry_count = 2 
        for attempt in range(retry_count + 1):
            try:
                response = requests.post(
                    url,
                    json=payload,
                    headers=headers,
                    timeout=self.settings.rag_embedding_timeout_seconds,
                )
                response.raise_for_status()
                return response.json()
            except Exception as exc:
                last_error = exc
                if attempt < retry_count:
                    # Exponential backoff
                    time.sleep(min(0.5 * (2 ** attempt), 2.0))
                    
        raise RuntimeError(f"Embedding provider call failed: {last_error}")
