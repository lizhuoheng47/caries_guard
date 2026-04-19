from __future__ import annotations

import re


class RerankService:
    def rerank(self, query: str, candidates: list[dict], top_k: int) -> list[dict]:
        query_tokens = self._tokens(query)
        for candidate in candidates:
            text_tokens = self._tokens(candidate.get("chunk_text") or candidate.get("evidence_text") or "")
            overlap = len(query_tokens & text_tokens)
            candidate["rerank_score"] = round(candidate.get("fusion_score", 0.0) + overlap * 0.1, 6)
        candidates.sort(key=lambda item: item["rerank_score"], reverse=True)
        for idx, item in enumerate(candidates, start=1):
            item["final_rank"] = idx
        return candidates[:top_k]

    @staticmethod
    def _tokens(text: str) -> set[str]:
        return {token for token in re.findall(r"[A-Za-z0-9\u4e00-\u9fff]+", text.lower()) if token}
