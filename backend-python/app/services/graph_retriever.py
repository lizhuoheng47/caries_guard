from __future__ import annotations

from typing import Any

from neo4j import Driver

from app.core.config import Settings
from app.core.logging import get_logger
from app.services.cypher_template_registry import CypherTemplate, CypherTemplateRegistry

log = get_logger("cariesguard-ai.graph-retriever")


class GraphRetriever:
    def __init__(self, settings: Settings, driver: Driver, template_registry: CypherTemplateRegistry) -> None:
        self.settings = settings
        self.driver = driver
        self.template_registry = template_registry

    def retrieve(self, linked_entities: list[dict[str, Any]], query: str, top_k: int) -> list[dict[str, Any]]:
        evidence: list[dict] = []
        seen: set[tuple[str, str]] = set()

        with self.driver.session(database=self.settings.neo4j_database) as session:
            for entity in linked_entities:
                entity_name = entity.get("canonical_name") or entity.get("entity_name")
                entity_type = entity.get("entity_type_code")
                for template in self.template_registry.matching_templates(entity_type):
                    dedup_key = (template.code, str(entity_name))
                    if dedup_key in seen:
                        continue
                    seen.add(dedup_key)

                    try:
                        records = session.run(
                            template.cypher,
                            **{template.parameter_name: entity_name},
                        ).data()

                        if records:
                            evidence.extend(self._records_to_evidence(records, template, entity, top_k))
                    except Exception as exc:
                        log.warning("graph template failed code=%s error=%s", template.code, exc)

            if len(evidence) < top_k:
                try:
                    fallback = self.template_registry.get("KEYWORD_EVIDENCE_TRACE")
                    records = session.run(fallback.cypher, keyword=query).data()

                    if records:
                        evidence.extend(
                            self._records_to_evidence(
                                records,
                                fallback,
                                {"entity_name": query, "entity_type_code": "KEYWORD"},
                                top_k,
                                is_fallback=True,
                            )
                        )
                except Exception as exc:
                    log.warning("graph fallback failed error=%s", exc)

        evidence.sort(key=lambda item: item["score"], reverse=True)
        results = evidence[:top_k]
        for index, item in enumerate(results, start=1):
            item["graph_rank"] = index

        return results

    def _records_to_evidence(
        self,
        records: list[dict[str, Any]],
        template: CypherTemplate,
        entity: dict[str, Any],
        top_k: int,
        is_fallback: bool = False,
    ) -> list[dict[str, Any]]:
        evidence: list[dict] = []
        for idx, record in enumerate(records[:top_k], start=1):
            serialized = self._serialize_record(record)
            doc = serialized.get("doc", {})
            chunk = serialized.get("chunk", {})

            has_doc = bool(doc.get("docId"))
            has_chunk = bool(chunk.get("chunkId"))
            has_provenance = has_doc and has_chunk

            authority = float(entity.get("source_authority_score") or 0.0)
            base_score = (
                (1.0 / idx)
                + float(chunk.get("confidenceScore") or 0.0) * self.settings.rag_graph_confidence_weight
                + authority * 0.05
            )

            score_penalty = 0.0
            tags = []
            if not has_provenance:
                score_penalty = 0.2
                tags.append("NO_PROVENANCE")

            score = max(0.0, base_score - score_penalty)

            quality = "HIGH"
            if is_fallback:
                quality = "DEGRADED"
            elif not has_provenance:
                quality = "UNVERIFIED"

            evidence.append(
                {
                    "evidence_id": f"graph-{template.code}-{entity.get('entity_name') or entity.get('canonical_name')}-{idx}",
                    "graph_path_id": f"{template.code}-{idx}",
                    "score": round(score, 6),
                    "channel": "GRAPH",
                    "cypher_template_code": template.code,
                    "query_entity_json": entity,
                    "result_path_json": serialized,
                    "evidence_text": self._record_to_text(serialized),
                    "doc_id": self._safe_int(doc.get("docId")),
                    "chunk_id": self._safe_int(chunk.get("chunkId")),
                    "doc_title": doc.get("title"),
                    "doc_version": doc.get("versionNo"),
                    "graph_confidence_score": round(float(chunk.get("confidenceScore") or 0.0), 6),
                    "provenance_path": self._provenance_path(serialized),
                    "graph_fallback": is_fallback,
                    "retrieval_quality": quality,
                    "quality_tags": tags,
                }
            )
        return evidence

    @staticmethod
    def _serialize_record(record: dict) -> dict:
        payload: dict[str, dict] = {}
        for key, value in record.items():
            if hasattr(value, "items"):
                payload[key] = dict(value.items())
            else:
                payload[key] = {"value": str(value)}
        return payload

    @staticmethod
    def _record_to_text(record: dict) -> str:
        parts = []
        for key, value in record.items():
            if not isinstance(value, dict):
                continue
            name = value.get("name") or value.get("title") or value.get("docId") or value.get("chunkId")
            if name:
                parts.append(f"{key}: {name}")
        return " | ".join(parts)

    @staticmethod
    def _provenance_path(record: dict) -> list[dict[str, Any]]:
        path = []
        for key in (
            "concept",
            "finding",
            "disease",
            "risk",
            "recommendation",
            "severity",
            "followUp",
            "population",
            "contraindication",
            "position",
            "chunk",
            "doc",
        ):
            value = record.get(key)
            if isinstance(value, dict) and value:
                path.append({"nodeType": key, "properties": value})
        return path

    @staticmethod
    def _safe_int(value: Any) -> int | None:
        try:
            return int(value) if value not in {None, ""} else None
        except (TypeError, ValueError):
            return None
