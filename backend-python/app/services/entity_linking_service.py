from __future__ import annotations

import re

from app.repositories.graph_repository import GraphRepository


class EntityLinkingService:
    def __init__(self, graph_repository: GraphRepository) -> None:
        self.graph_repository = graph_repository

    def link(self, query: str) -> list[dict]:
        candidates = re.findall(r"[A-Za-z0-9\u4e00-\u9fff]{2,}", query or "")
        return self.graph_repository.list_entities_by_names(candidates)
