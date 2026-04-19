from __future__ import annotations

import math
import re

from app.core.config import Settings
from app.infra.rerank.base_rerank_provider import BaseRerankProvider, RerankMetadata
from app.infra.vector.base_embedding_provider import BaseEmbeddingProvider


class EmbeddingSimilarityRerankProvider(BaseRerankProvider):
    def __init__(self, settings: Settings, embedding_provider: BaseEmbeddingProvider) -> None:
        self.settings = settings
        self.embedding_provider = embedding_provider
        self._metadata = RerankMetadata(
            provider="EMBEDDING",
            model=settings.rerank_model_name,
            version=embedding_provider.metadata.version,
        )

    @property
    def metadata(self) -> RerankMetadata:
        return self._metadata

    def score(self, query: str, candidates: list[dict]) -> list[float]:
        if not candidates:
            return []
        query_vector = self.embedding_provider.embed(query)
        candidate_vectors = self.embedding_provider.embed_many(
            [candidate.get("chunk_text") or candidate.get("evidence_text") or "" for candidate in candidates]
        )
        query_tokens = self._tokens(query)
        scores: list[float] = []
        for candidate, vector in zip(candidates, candidate_vectors):
            evidence_text = candidate.get("chunk_text") or candidate.get("evidence_text") or ""
            lexical_overlap = self._lexical_overlap(query_tokens, self._tokens(evidence_text))
            semantic_score = self._cosine_similarity(query_vector, vector)
            prior_score = float(candidate.get("fusion_score") or candidate.get("score") or 0.0)
            final_score = (
                semantic_score * self.settings.rerank_semantic_weight
                + lexical_overlap * self.settings.rerank_lexical_weight
                + prior_score * self.settings.rerank_prior_weight
            )
            scores.append(round(final_score, 6))
        return scores

    @staticmethod
    def _cosine_similarity(left: list[float], right: list[float]) -> float:
        numerator = sum(a * b for a, b in zip(left, right))
        left_norm = math.sqrt(sum(a * a for a in left))
        right_norm = math.sqrt(sum(b * b for b in right))
        if left_norm == 0 or right_norm == 0:
            return 0.0
        return numerator / (left_norm * right_norm)

    @staticmethod
    def _lexical_overlap(left: set[str], right: set[str]) -> float:
        if not left or not right:
            return 0.0
        return len(left & right) / max(1, len(left | right))

    @staticmethod
    def _tokens(text: str) -> set[str]:
        return {token for token in re.findall(r"[A-Za-z0-9\u4e00-\u9fff]+", (text or "").lower()) if token}
