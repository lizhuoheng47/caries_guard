from __future__ import annotations

from neo4j import Driver

from app.core.config import Settings
from app.repositories.graph_repository import GraphRepository


class GraphUpsertService:
    def __init__(self, settings: Settings, driver: Driver, graph_repository: GraphRepository) -> None:
        self.settings = settings
        self.driver = driver
        self.graph_repository = graph_repository

    def sync_document_graph(
        self,
        doc_id: int,
        doc_title: str,
        version_no: str,
        chunk_entities: list[dict],
        relations: list[dict],
        org_id: int | None,
        created_by: int | None,
    ) -> tuple[list[dict], list[dict]]:
        entities, stored_relations = self.graph_repository.replace_entities_and_relations(
            doc_id=doc_id,
            chunk_entities=chunk_entities,
            relations=relations,
            org_id=org_id,
            created_by=created_by,
        )
        with self.driver.session(database=self.settings.neo4j_database) as session:
            session.run("MERGE (d:EvidenceDocument {docId: $doc_id}) SET d.title = $title, d.versionNo = $version_no",
                        doc_id=str(doc_id), title=doc_title, version_no=version_no)
            for entity in entities:
                label = entity["entity_type_code"]
                session.run(
                    f"MERGE (n:{label} {{entityId: $entity_id}}) "
                    "SET n.name = $name, n.normalizedName = $normalized_name, n.docId = $doc_id",
                    entity_id=str(entity["id"]),
                    name=entity["entity_name"],
                    normalized_name=entity["normalized_name"],
                    doc_id=str(doc_id),
                )
                if entity.get("source_chunk_id"):
                    session.run(
                        "MERGE (c:EvidenceChunk {chunkId: $chunk_id}) "
                        "SET c.docId = $doc_id "
                        "MERGE (c)-[:MENTIONS]->(n) "
                        "MERGE (c)-[:PART_OF]->(d:EvidenceDocument {docId: $doc_id})",
                        chunk_id=str(entity["source_chunk_id"]),
                        doc_id=str(doc_id),
                    )
            for relation in stored_relations:
                source = next((item for item in entities if item["id"] == relation["source_entity_id"]), None)
                target = next((item for item in entities if item["id"] == relation["target_entity_id"]), None)
                if source is None or target is None:
                    continue
                session.run(
                    f"MATCH (s {{entityId: $source_id}}), (t {{entityId: $target_id}}) "
                    f"MERGE (s)-[r:{relation['relation_type_code']}]->(t) "
                    "SET r.relationCode = $relation_code, r.docId = $doc_id",
                    source_id=str(source["id"]),
                    target_id=str(target["id"]),
                    relation_code=relation["relation_code"],
                    doc_id=str(doc_id),
                )
        return entities, stored_relations
