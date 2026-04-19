from __future__ import annotations

from app.services.open_search_index_service import OpenSearchIndexService


class DenseRetriever:
    def __init__(self, index_service: OpenSearchIndexService) -> None:
        self.index_service = index_service

    def retrieve(self, kb_code: str, query: str, top_k: int) -> list[dict]:
        return self.index_service.dense_search(kb_code, query, top_k)
