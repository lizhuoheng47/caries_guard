from __future__ import annotations

import re
from typing import Any


class EntityExtractionService:
    ENTITY_DICTIONARY: dict[str, list[str]] = {
        "Disease": ["龋病", "龋齿", "早期龋", "中龋", "深龋"],
        "Severity": ["低风险", "中风险", "高风险", "轻度", "中度", "重度"],
        "RiskFactor": ["高糖饮食", "夜奶", "口腔卫生差", "牙菌斑", "窝沟深", "低氟", "频繁进食"],
        "Recommendation": ["定期复查", "窝沟封闭", "局部涂氟", "控制糖摄入", "规范刷牙", "及时复诊"],
        "Population": ["儿童", "学龄前儿童", "青少年", "成人", "孕妇"],
    }

    def extract(self, chunks: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
        chunk_entities: list[dict[str, Any]] = []
        relations: list[dict[str, Any]] = []
        refs_by_chunk: dict[int, list[str]] = {}
        for chunk in chunks:
            entities = self._extract_entities_for_chunk(chunk)
            refs_by_chunk[chunk["id"]] = [entity["entity_name"] for entity in entities]
            chunk_entities.extend(entities)
            relations.extend(self._extract_relations(chunk["id"], entities, chunk["chunk_text"]))
        return chunk_entities, relations, [
            {"chunk_id": chunk_id, "entity_names": entity_names} for chunk_id, entity_names in refs_by_chunk.items()
        ]

    def _extract_entities_for_chunk(self, chunk: dict[str, Any]) -> list[dict[str, Any]]:
        text = chunk["chunk_text"]
        result: list[dict[str, Any]] = []
        seen: set[tuple[str, str]] = set()
        for entity_type, candidates in self.ENTITY_DICTIONARY.items():
            for candidate in candidates:
                if candidate in text and (entity_type, candidate) not in seen:
                    seen.add((entity_type, candidate))
                    result.append(
                        {
                            "entity_key": f"{chunk['id']}::{entity_type}::{candidate}",
                            "entity_name": candidate,
                            "entity_type_code": entity_type,
                            "source_chunk_id": chunk["id"],
                            "confidence_score": 0.82,
                            "aliases": [candidate.lower()],
                        }
                    )
        for match in re.findall(r"(\d+\s*(?:天|周|月))", text):
            result.append(
                {
                    "entity_key": f"{chunk['id']}::FollowUpInterval::{match}",
                    "entity_name": match,
                    "entity_type_code": "FollowUpInterval",
                    "source_chunk_id": chunk["id"],
                    "confidence_score": 0.88,
                    "aliases": [],
                }
            )
        for match in re.findall(r"\b[1-4][1-8]\b", text):
            result.append(
                {
                    "entity_key": f"{chunk['id']}::ToothPosition::{match}",
                    "entity_name": match,
                    "entity_type_code": "ToothPosition",
                    "source_chunk_id": chunk["id"],
                    "confidence_score": 0.8,
                    "aliases": [],
                }
            )
        return result

    @staticmethod
    def _extract_relations(chunk_id: int, entities: list[dict[str, Any]], text: str) -> list[dict[str, Any]]:
        by_type: dict[str, list[dict[str, Any]]] = {}
        for entity in entities:
            by_type.setdefault(entity["entity_type_code"], []).append(entity)
        relations: list[dict[str, Any]] = []
        if "RiskFactor" in by_type and "Recommendation" in by_type:
            for left in by_type["RiskFactor"]:
                for right in by_type["Recommendation"]:
                    relations.append(
                        {
                            "source_entity_key": left["entity_key"],
                            "target_entity_key": right["entity_key"],
                            "relation_type_code": "SUGGESTS",
                            "evidence_chunk_id": chunk_id,
                            "confidence_score": 0.76,
                        }
                    )
        if "Severity" in by_type and "FollowUpInterval" in by_type:
            for left in by_type["Severity"]:
                for right in by_type["FollowUpInterval"]:
                    relations.append(
                        {
                            "source_entity_key": left["entity_key"],
                            "target_entity_key": right["entity_key"],
                            "relation_type_code": "REQUIRES_FOLLOWUP",
                            "evidence_chunk_id": chunk_id,
                            "confidence_score": 0.84,
                        }
                    )
        if "Population" in by_type and "Recommendation" in by_type:
            for left in by_type["Population"]:
                for right in by_type["Recommendation"]:
                    relations.append(
                        {
                            "source_entity_key": left["entity_key"],
                            "target_entity_key": right["entity_key"],
                            "relation_type_code": "APPLIES_TO",
                            "evidence_chunk_id": chunk_id,
                            "confidence_score": 0.72,
                        }
                    )
        if "Disease" in by_type and "RiskFactor" in by_type and "风险" in text:
            for left in by_type["Disease"]:
                for right in by_type["RiskFactor"]:
                    relations.append(
                        {
                            "source_entity_key": left["entity_key"],
                            "target_entity_key": right["entity_key"],
                            "relation_type_code": "HAS_RISK_FACTOR",
                            "evidence_chunk_id": chunk_id,
                            "confidence_score": 0.71,
                        }
                    )
        return relations
