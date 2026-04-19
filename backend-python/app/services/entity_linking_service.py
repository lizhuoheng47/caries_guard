from __future__ import annotations

import re

from app.repositories.graph_repository import GraphRepository
from app.services.concept_normalization_service import ConceptNormalizationService
from app.services.entity_extraction_service import EntityExtractionService


class EntityLinkingService:
    def __init__(
        self,
        graph_repository: GraphRepository,
        concept_normalization_service: ConceptNormalizationService,
        entity_extraction_service: EntityExtractionService,
    ) -> None:
        self.graph_repository = graph_repository
        self.concept_normalization_service = concept_normalization_service
        self.entity_extraction_service = entity_extraction_service

    def link(self, query: str) -> list[dict]:
        extractor_hits = self.entity_extraction_service.extract_query_entities(query)
        candidates = re.findall(r"[A-Za-z0-9\u4e00-\u9fff]{2,}", query or "")
        repo_hits = self.graph_repository.list_entities_by_names(candidates)
        linked: dict[str, dict] = {}
        for item in repo_hits:
            concept = self.concept_normalization_service.canonicalize(
                item["entity_type_code"],
                item["entity_name"],
                confidence_score=float(item.get("confidence_score") or 0.8),
            )
            linked[concept.concept_id] = {
                **item,
                "concept_id": concept.concept_id,
                "canonical_name": concept.canonical_name,
                "aliases": concept.aliases,
            }
        for item in extractor_hits:
            linked.setdefault(item["concept_id"], item)
        return list(linked.values())
