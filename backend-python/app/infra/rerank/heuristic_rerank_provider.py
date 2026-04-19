from __future__ import annotations

import re

from app.infra.rerank.base_rerank_provider import BaseRerankProvider, RerankMetadata


class HeuristicRerankProvider(BaseRerankProvider):
    def __init__(self, model_name: str = "token-overlap-reranker", version: str = "v1") -> None:
        self._metadata = RerankMetadata(provider="HEURISTIC", model=model_name, version=version)

    @property
    def metadata(self) -> RerankMetadata:
        return self._metadata

    def score(self, query: str, candidates: list[dict]) -> list[float]:
        query_tokens = self._tokens(query)
        scores: list[float] = []
        for candidate in candidates:
            text_tokens = self._tokens(candidate.get("chunk_text") or candidate.get("evidence_text") or "")
            overlap = len(query_tokens & text_tokens)
            denominator = max(1, len(query_tokens | text_tokens))
            scores.append(round(overlap / denominator, 6))
        return scores

    @staticmethod
    def _tokens(text: str) -> set[str]:
        return {token for token in re.findall(r"[A-Za-z0-9\u4e00-\u9fff]+", (text or "").lower()) if token}
