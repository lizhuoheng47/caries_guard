from __future__ import annotations

from neo4j import Driver

from app.core.config import Settings
from app.services.cypher_template_registry import CypherTemplateRegistry


class GraphRetriever:
    def __init__(self, settings: Settings, driver: Driver, template_registry: CypherTemplateRegistry) -> None:
        self.settings = settings
        self.driver = driver
        self.template_registry = template_registry

    def retrieve(self, linked_entities: list[dict], query: str, top_k: int) -> list[dict]:
        evidence: list[dict] = []
        with self.driver.session(database=self.settings.neo4j_database) as session:
            for entity in linked_entities:
                entity_type = entity.get("entity_type_code")
                if entity_type == "RiskFactor":
                    code = "RISK_TO_RECOMMENDATION"
                    records = session.run(self.template_registry.get(code), risk=entity["entity_name"]).data()
                elif entity_type == "Severity":
                    code = "SEVERITY_TO_FOLLOWUP"
                    records = session.run(self.template_registry.get(code), severity=entity["entity_name"]).data()
                elif entity_type == "Population":
                    code = "POPULATION_RULES"
                    records = session.run(self.template_registry.get(code), population=entity["entity_name"]).data()
                else:
                    continue
                for idx, record in enumerate(records[:top_k], start=1):
                    evidence.append(
                        {
                            "evidence_id": f"graph-{code}-{entity['entity_name']}-{idx}",
                            "graph_path_id": f"{code}-{idx}",
                            "score": 1.0 / idx,
                            "channel": "GRAPH",
                            "cypher_template_code": code,
                            "query_entity_json": entity,
                            "result_path_json": self._serialize_record(record),
                            "evidence_text": self._record_to_text(record),
                            "doc_id": int(record.get("d", {}).get("docId")) if isinstance(record.get("d"), dict) and record.get("d", {}).get("docId") else None,
                            "chunk_id": int(record.get("c", {}).get("chunkId")) if isinstance(record.get("c"), dict) and record.get("c", {}).get("chunkId") else None,
                        }
                    )
        return evidence[:top_k]

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
            if hasattr(value, "get"):
                name = value.get("name") or value.get("title") or value.get("docId") or value.get("chunkId")
                parts.append(f"{key}: {name}")
        return " | ".join(parts)
