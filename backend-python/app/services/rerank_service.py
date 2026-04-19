from __future__ import annotations

from app.infra.rerank.base_rerank_provider import BaseRerankProvider


class RerankService:
    def __init__(self, provider: BaseRerankProvider) -> None:
        self.provider = provider

    def rerank(self, query: str, candidates: list[dict], top_k: int) -> list[dict]:
        scores = self.provider.score(query, candidates)
        for candidate, score in zip(candidates, scores):
            candidate["rerank_score"] = score
            candidate["rerank_provider"] = self.provider.metadata.provider
            candidate["rerank_model"] = self.provider.metadata.model
            candidate["rerank_version"] = self.provider.metadata.version
        candidates.sort(key=lambda item: item["rerank_score"], reverse=True)
        for idx, item in enumerate(candidates, start=1):
            item["final_rank"] = idx
        return candidates[:top_k]
