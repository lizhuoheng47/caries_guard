from __future__ import annotations

import re
from typing import Any

from app.services.concept_normalization_service import CanonicalConcept, ConceptNormalizationService
from app.services.entity_dictionary import DEFAULT_ENTITY_DICTIONARY


class EntityExtractionService:
    def __init__(
        self,
        normalizer: ConceptNormalizationService | None = None,
        entity_dictionary: dict[str, list[str]] | None = None,
    ) -> None:
        self.normalizer = normalizer or ConceptNormalizationService()
        self.entity_dictionary = entity_dictionary or DEFAULT_ENTITY_DICTIONARY

    def extract(
        self,
        chunks: list[dict[str, Any]],
    ) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
        chunk_entities: list[dict[str, Any]] = []
        relations: list[dict[str, Any]] = []
        refs_by_chunk: dict[int, dict[str, list[str]]] = {}
        for chunk in chunks:
            entities = self._extract_entities_for_chunk(chunk)
            refs_by_chunk[chunk["id"]] = {
                "entity_names": [entity["canonical_name"] for entity in entities],
                "concept_ids": [entity["concept_id"] for entity in entities],
            }
            chunk_entities.extend(entities)
            relations.extend(self._extract_relations(chunk["id"], entities, chunk["chunk_text"]))
        return (
            chunk_entities,
            relations,
            [
                {
                    "chunk_id": chunk_id,
                    "entity_names": payload["entity_names"],
                    "concept_ids": payload["concept_ids"],
                }
                for chunk_id, payload in refs_by_chunk.items()
            ],
        )

    def extract_query_entities(self, query: str) -> list[dict[str, Any]]:
        linked = []
        for concept in self.normalizer.match_terms(query):
            linked.append(self._concept_payload(source_key="query", chunk_id=None, concept=concept))
        return linked

    def _extract_entities_for_chunk(self, chunk: dict[str, Any]) -> list[dict[str, Any]]:
        text = chunk["chunk_text"]
        result: list[dict[str, Any]] = []
        seen: set[str] = set()
        for entity_type, candidates in self.entity_dictionary.items():
            for candidate in candidates:
                if candidate and candidate in text:
                    concept = self.normalizer.canonicalize(entity_type, candidate, confidence_score=0.82)
                    if concept.concept_id in seen:
                        continue
                    seen.add(concept.concept_id)
                    result.append(self._concept_payload(source_key=str(chunk["id"]), chunk_id=chunk["id"], concept=concept))
        for match in re.findall(r"(\d+\s*(?:天|周|月))", text):
            concept = self.normalizer.canonicalize("FollowUpInterval", match, confidence_score=0.88)
            if concept.concept_id not in seen:
                seen.add(concept.concept_id)
                result.append(self._concept_payload(source_key=str(chunk["id"]), chunk_id=chunk["id"], concept=concept))
        for match in re.findall(r"\b[1-4][1-8]\b", text):
            concept = self.normalizer.canonicalize("ToothPosition", match, confidence_score=0.8)
            if concept.concept_id not in seen:
                seen.add(concept.concept_id)
                result.append(self._concept_payload(source_key=str(chunk["id"]), chunk_id=chunk["id"], concept=concept))
        return result

    def _concept_payload(self, source_key: str, chunk_id: int | None, concept: CanonicalConcept) -> dict[str, Any]:
        return {
            "entity_key": f"{source_key}::{concept.entity_type_code}::{concept.concept_id}",
            "entity_name": concept.canonical_name,
            "canonical_name": concept.canonical_name,
            "entity_type_code": concept.entity_type_code,
            "source_chunk_id": chunk_id,
            "confidence_score": concept.confidence_score,
            "aliases": concept.aliases,
            "concept_id": concept.concept_id,
            "normalized_name": concept.normalized_name,
            "provenance": {"canonicalName": concept.canonical_name, "aliases": concept.aliases},
        }

    @staticmethod
    def _extract_relations(chunk_id: int, entities: list[dict[str, Any]], text: str) -> list[dict[str, Any]]:
        by_type: dict[str, list[dict[str, Any]]] = {}
        for entity in entities:
            by_type.setdefault(entity["entity_type_code"], []).append(entity)
        relations: list[dict[str, Any]] = []
        relation_rules = [
            ("RiskFactor", "Recommendation", "SUGGESTS", 0.76),
            ("Severity", "FollowUpInterval", "REQUIRES_FOLLOWUP", 0.84),
            ("Severity", "Recommendation", "RECOMMENDED_FOR", 0.8),
            ("Population", "Recommendation", "APPLIES_TO", 0.72),
            ("Population", "Contraindication", "CONTRAINDICATED_FOR", 0.7),
            ("Disease", "RiskFactor", "HAS_RISK_FACTOR", 0.71),
            ("Disease", "Recommendation", "RECOMMENDED_FOR", 0.78),
            ("ImagingFinding", "Disease", "INDICATES", 0.8),
            ("Disease", "ImagingFinding", "RELATED_TO", 0.75),
            ("Recommendation", "Guideline", "SUPPORTED_BY", 0.66),
        ]
        for left_type, right_type, relation_code, confidence in relation_rules:
            if left_type not in by_type or right_type not in by_type:
                continue
            if relation_code == "HAS_RISK_FACTOR" and "风险" not in text and "危险" not in text:
                continue
            for left in by_type[left_type]:
                for right in by_type[right_type]:
                    relations.append(
                        {
                            "source_entity_key": left["entity_key"],
                            "target_entity_key": right["entity_key"],
                            "relation_type_code": relation_code,
                            "evidence_chunk_id": chunk_id,
                            "confidence_score": confidence,
                            "source_concept_id": left["concept_id"],
                            "target_concept_id": right["concept_id"],
                        }
                    )
        return relations
