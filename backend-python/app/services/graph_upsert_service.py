from __future__ import annotations

from collections import defaultdict
from typing import Any

from neo4j import Driver

from app.core.config import Settings
from app.core.logging import get_logger
from app.repositories.graph_repository import GraphRepository

log = get_logger("cariesguard-ai.graph-upsert")


class GraphUpsertService:
    def __init__(self, settings: Settings, driver: Driver, graph_repository: GraphRepository) -> None:
        self.settings = settings
        self.driver = driver
        self.graph_repository = graph_repository

    def ensure_schema(self) -> None:
        statements = [
            "CREATE CONSTRAINT concept_id_unique IF NOT EXISTS FOR (n:Concept) REQUIRE n.conceptId IS UNIQUE",
            "CREATE INDEX concept_name_idx IF NOT EXISTS FOR (n:Concept) ON (n.name)",
            "CREATE INDEX alias_name_idx IF NOT EXISTS FOR (a:AliasTerm) ON (a.name)",
            "CREATE INDEX evidence_chunk_id_idx IF NOT EXISTS FOR (c:EvidenceChunk) ON (c.chunkId)",
            "CREATE INDEX evidence_chunk_doc_idx IF NOT EXISTS FOR (c:EvidenceChunk) ON (c.docId)",
            "CREATE INDEX evidence_doc_id_idx IF NOT EXISTS FOR (d:EvidenceDocument) ON (d.docId)",
        ]
        with self.driver.session(database=self.settings.neo4j_database) as session:
            for statement in statements:
                session.run(statement)
        log.info("neo4j schema ensured statements=%s", len(statements))

    def sync_document_graph(
        self,
        doc_id: int,
        doc_title: str,
        version_no: str,
        chunk_entities: list[dict],
        relations: list[dict],
        org_id: int | None,
        created_by: int | None,
    ) -> dict[str, Any]:
        entities, stored_relations = self.graph_repository.replace_entities_and_relations(
            doc_id=doc_id,
            chunk_entities=chunk_entities,
            relations=relations,
            org_id=org_id,
            created_by=created_by,
        )
        entity_lookup = {
            item["entity_key"]: item
            for item in chunk_entities
        }
        entities_by_id = {entity["id"]: entity for entity in entities}
        with self.driver.session(database=self.settings.neo4j_database) as session:
            self.cleanup_document_graph(doc_id, session=session)
            session.run(
                "MERGE (d:EvidenceDocument {docId: $doc_id}) "
                "SET d.title = $title, d.versionNo = $version_no, d.docId = $doc_id",
                doc_id=str(doc_id),
                title=doc_title,
                version_no=version_no,
            )
            concept_payloads = {}
            for entity in chunk_entities:
                concept_id = entity["concept_id"]
                concept_payloads.setdefault(
                    concept_id,
                    {
                        "concept_id": concept_id,
                        "concept_name": entity["canonical_name"],
                        "entity_type_code": entity["entity_type_code"],
                        "normalized_name": entity["normalized_name"],
                        "aliases": entity.get("aliases") or [],
                    },
                )
            for payload in concept_payloads.values():
                session.run(
                    "MERGE (n:Concept {conceptId: $concept_id}) "
                    "SET n.name = $concept_name, "
                    "    n.entityTypeCode = $entity_type_code, "
                    "    n.normalizedName = $normalized_name, "
                    "    n.aliases = $aliases",
                    **payload,
                )
                for alias in payload["aliases"]:
                    session.run(
                        "MERGE (a:AliasTerm {name: $alias_name}) "
                        "SET a.normalizedName = $normalized_alias_name "
                        "WITH a "
                        "MATCH (n:Concept {conceptId: $concept_id}) "
                        "MERGE (a)-[:ALIAS_OF]->(n)",
                        alias_name=alias,
                        normalized_alias_name=self.graph_repository.normalize(alias),
                        concept_id=payload["concept_id"],
                    )
            chunk_mentions: dict[int, list[str]] = defaultdict(list)
            for entity in chunk_entities:
                if entity.get("source_chunk_id"):
                    chunk_mentions[int(entity["source_chunk_id"])].append(entity["concept_id"])
            for chunk_id, concept_ids in chunk_mentions.items():
                session.run(
                    "MERGE (c:EvidenceChunk {chunkId: $chunk_id}) "
                    "SET c.docId = $doc_id "
                    "MERGE (c)-[:PART_OF]->(d:EvidenceDocument {docId: $doc_id})",
                    chunk_id=str(chunk_id),
                    doc_id=str(doc_id),
                )
                for concept_id in sorted(set(concept_ids)):
                    session.run(
                        "MATCH (c:EvidenceChunk {chunkId: $chunk_id}), (n:Concept {conceptId: $concept_id}) "
                        "MERGE (c)-[:MENTIONS]->(n)",
                        chunk_id=str(chunk_id),
                        concept_id=concept_id,
                    )
            for relation in stored_relations:
                source = entities_by_id.get(relation["source_entity_id"])
                target = entities_by_id.get(relation["target_entity_id"])
                source_payload = entity_lookup.get(source["entity_key"]) if source else None
                target_payload = entity_lookup.get(target["entity_key"]) if target else None
                if source_payload is None or target_payload is None:
                    continue
                session.run(
                    f"MATCH (s:Concept {{conceptId: $source_concept_id}}), (t:Concept {{conceptId: $target_concept_id}}) "
                    f"MERGE (s)-[r:{relation['relation_type_code']}]->(t) "
                    "SET r.relationCode = $relation_code, "
                    "    r.docId = $doc_id, "
                    "    r.versionNo = $version_no, "
                    "    r.evidenceChunkId = $evidence_chunk_id, "
                    "    r.confidenceScore = $confidence_score",
                    source_concept_id=source_payload["concept_id"],
                    target_concept_id=target_payload["concept_id"],
                    relation_code=relation["relation_code"],
                    doc_id=str(doc_id),
                    version_no=version_no,
                    evidence_chunk_id=str(relation.get("evidence_chunk_id") or ""),
                    confidence_score=float(relation.get("confidence_score") or 0.0),
                )
                if relation.get("evidence_chunk_id"):
                    session.run(
                        "MATCH (c:EvidenceChunk {chunkId: $chunk_id}), "
                        "      (s:Concept {conceptId: $source_concept_id}), "
                        "      (t:Concept {conceptId: $target_concept_id}) "
                        "MERGE (c)-[:SUPPORTED_BY]->(s) "
                        "MERGE (c)-[:SUPPORTED_BY]->(t)",
                        chunk_id=str(relation["evidence_chunk_id"]),
                        source_concept_id=source_payload["concept_id"],
                        target_concept_id=target_payload["concept_id"],
                    )
        return {
            "docId": doc_id,
            "conceptCount": len(concept_payloads),
            "relationCount": len(stored_relations),
            "chunkMentionCount": len(chunk_mentions),
            "status": "SUCCESS"
        }

    def detect_document_graph_conflicts(self, doc_id: int) -> list[dict]:
        """Detect conflicts such as alias collisions or version mismatches."""
        conflicts = []
        with self.driver.session(database=self.settings.neo4j_database) as session:
            res = session.run(
                "MATCH (a:AliasTerm)-[:ALIAS_OF]->(n:Concept) "
                "WITH a, collect(n) AS concepts "
                "WHERE size(concepts) > 1 "
                "RETURN a.name AS alias, [c in concepts | c.name] AS conceptNames"
            ).data()
            for row in res:
                conflicts.append({
                    "type": "ALIAS_COLLISION",
                    "alias": row["alias"],
                    "conflictingConcepts": row["conceptNames"]
                })
        return conflicts

    def cleanup_document_graph(self, doc_id: int, session=None) -> None:
        managed_session = session or self.driver.session(database=self.settings.neo4j_database)
        close_after = session is None
        try:
            managed_session.run(
                "MATCH (c:EvidenceChunk {docId: $doc_id}) DETACH DELETE c",
                doc_id=str(doc_id),
            )
            managed_session.run(
                "MATCH (d:EvidenceDocument {docId: $doc_id}) DETACH DELETE d",
                doc_id=str(doc_id),
            )
            managed_session.run(
                "MATCH ()-[r]->() WHERE r.docId = $doc_id DELETE r",
                doc_id=str(doc_id),
            )
        finally:
            if close_after:
                managed_session.close()
