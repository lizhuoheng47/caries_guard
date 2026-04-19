from __future__ import annotations

import re
import uuid
from typing import Any

from sqlalchemy import delete, func, select

from app.core.db import session_scope
from app.core.time_utils import local_naive_now
from app.models.rag import KnowledgeEntity, KnowledgeEntityAlias, KnowledgeRelation


def _row_to_dict(obj: Any) -> dict[str, Any]:
    return {column.name: getattr(obj, column.name) for column in obj.__table__.columns}


class GraphRepository:
    def replace_entities_and_relations(
        self,
        doc_id: int,
        chunk_entities: list[dict[str, Any]],
        relations: list[dict[str, Any]],
        org_id: int | None,
        created_by: int | None,
    ) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        now = local_naive_now()
        with session_scope() as session:
            chunk_ids = {item.get("source_chunk_id") for item in chunk_entities if item.get("source_chunk_id")}
            if chunk_ids:
                session.execute(
                    delete(KnowledgeRelation).where(KnowledgeRelation.evidence_chunk_id.in_(chunk_ids))
                )
                session.execute(
                    delete(KnowledgeEntity).where(KnowledgeEntity.source_chunk_id.in_(chunk_ids))
                )

            stored_entities: list[dict[str, Any]] = []
            entity_id_by_key: dict[str, int] = {}
            for item in chunk_entities:
                row = KnowledgeEntity(
                    entity_code=f"ENT-{uuid.uuid4().hex[:16].upper()}",
                    entity_name=item["entity_name"],
                    entity_type_code=item["entity_type_code"],
                    normalized_name=self.normalize(item["entity_name"]),
                    source_doc_id=doc_id,
                    source_chunk_id=item.get("source_chunk_id"),
                    confidence_score=item.get("confidence_score"),
                    review_status_code=item.get("review_status_code", "APPROVED"),
                    org_id=org_id,
                    created_by=created_by,
                    created_at=now,
                )
                session.add(row)
                session.flush()
                entity_id_by_key[item["entity_key"]] = row.id
                stored_entities.append(_row_to_dict(row))
                for alias in item.get("aliases", []):
                    session.add(
                        KnowledgeEntityAlias(
                            entity_id=row.id,
                            alias_name=alias,
                            normalized_alias_name=self.normalize(alias),
                            created_at=now,
                        )
                    )

            stored_relations: list[dict[str, Any]] = []
            for item in relations:
                source_entity_id = entity_id_by_key.get(item["source_entity_key"])
                target_entity_id = entity_id_by_key.get(item["target_entity_key"])
                if source_entity_id is None or target_entity_id is None:
                    continue
                row = KnowledgeRelation(
                    relation_code=f"REL-{uuid.uuid4().hex[:16].upper()}",
                    source_entity_id=source_entity_id,
                    target_entity_id=target_entity_id,
                    relation_type_code=item["relation_type_code"],
                    evidence_doc_id=doc_id,
                    evidence_chunk_id=item.get("evidence_chunk_id"),
                    confidence_score=item.get("confidence_score"),
                    review_status_code=item.get("review_status_code", "APPROVED"),
                    org_id=org_id,
                    created_by=created_by,
                    created_at=now,
                )
                session.add(row)
                session.flush()
                stored_relations.append(_row_to_dict(row))
            return stored_entities, stored_relations

    def list_entities_by_names(self, names: list[str], only_approved: bool = True) -> list[dict[str, Any]]:
        normalized_names = [self.normalize(name) for name in names if name]
        if not normalized_names:
            return []
        with session_scope() as session:
            rows = session.execute(
                select(KnowledgeEntity)
                .where(
                    KnowledgeEntity.normalized_name.in_(normalized_names),
                    KnowledgeEntity.enabled_flag == "1",
                )
                .order_by(KnowledgeEntity.id)
            ).scalars().all()
            results = [_row_to_dict(row) for row in rows]
            if results:
                return results
            alias_rows = session.execute(
                select(KnowledgeEntityAlias, KnowledgeEntity)
                .join(KnowledgeEntity, KnowledgeEntity.id == KnowledgeEntityAlias.entity_id)
                .where(
                    KnowledgeEntityAlias.normalized_alias_name.in_(normalized_names),
                    KnowledgeEntity.enabled_flag == "1",
                )
            ).all()
            dedup: dict[int, dict[str, Any]] = {}
            for _, entity in alias_rows:
                dedup[entity.id] = _row_to_dict(entity)
            return list(dedup.values())

    def entity_counts(self) -> dict[str, int]:
        with session_scope() as session:
            return {
                "entityCount": session.execute(select(func.count(KnowledgeEntity.id))).scalar_one(),
                "relationCount": session.execute(select(func.count(KnowledgeRelation.id))).scalar_one(),
            }

    @staticmethod
    def normalize(value: str) -> str:
        return re.sub(r"\s+", "", value or "").strip().lower()
