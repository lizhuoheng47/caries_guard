from __future__ import annotations

from typing import Any

from opensearchpy import OpenSearch
from opensearchpy.helpers import bulk

from app.core.config import Settings
from app.infra.vector.hashing_embedder import HashingEmbedder


class OpenSearchIndexService:
    def __init__(self, settings: Settings, client: OpenSearch, embedder: HashingEmbedder) -> None:
        self.settings = settings
        self.client = client
        self.embedder = embedder

    def ensure_indices(self) -> None:
        chunk_index = self.settings.opensearch_chunk_index
        if not self.client.indices.exists(index=chunk_index):
            self.client.indices.create(
                index=chunk_index,
                body={
                    "settings": {"index": {"knn": True}},
                    "mappings": {
                        "properties": {
                            "chunkId": {"type": "keyword"},
                            "docId": {"type": "keyword"},
                            "kbCode": {"type": "keyword"},
                            "docTitle": {"type": "text"},
                            "sourceUri": {"type": "keyword"},
                            "chunkText": {"type": "text"},
                            "medicalTags": {"type": "keyword"},
                            "publishStatus": {"type": "keyword"},
                            "versionNo": {"type": "keyword"},
                            "lexicalText": {"type": "text"},
                            "denseVector": {
                                "type": "knn_vector",
                                "dimension": self.settings.rag_embedding_dimension,
                                "method": {
                                    "engine": "lucene",
                                    "space_type": "cosinesimil",
                                    "name": "hnsw",
                                },
                            },
                        }
                    },
                },
            )
        doc_index = self.settings.opensearch_doc_index
        if not self.client.indices.exists(index=doc_index):
            self.client.indices.create(
                index=doc_index,
                body={
                    "mappings": {
                        "properties": {
                            "docId": {"type": "keyword"},
                            "docNo": {"type": "keyword"},
                            "docTitle": {"type": "text"},
                            "versionNo": {"type": "keyword"},
                            "reviewStatus": {"type": "keyword"},
                            "publishStatus": {"type": "keyword"},
                            "sourceType": {"type": "keyword"},
                            "tags": {"type": "keyword"},
                            "createdAt": {"type": "date"},
                            "updatedAt": {"type": "date"},
                        }
                    }
                },
            )

    def index_document_version(
        self,
        kb_code: str,
        document: dict[str, Any],
        version: dict[str, Any],
        chunks: list[dict[str, Any]],
    ) -> None:
        self.ensure_indices()
        bulk_actions = [
            {
                "_index": self.settings.opensearch_chunk_index,
                "_id": str(chunk["id"]),
                "_source": {
                    "chunkId": str(chunk["id"]),
                    "docId": str(document["id"]),
                    "kbCode": kb_code,
                    "docTitle": document["doc_title"],
                    "sourceUri": chunk.get("source_uri") or document.get("source_uri"),
                    "chunkText": chunk["chunk_text"],
                    "medicalTags": chunk.get("medical_tags") or [],
                    "publishStatus": chunk.get("publish_status", "INDEXED"),
                    "versionNo": version["version_no"],
                    "lexicalText": chunk["chunk_text"],
                    "denseVector": self.embedder.embed(chunk["chunk_text"]),
                },
            }
            for chunk in chunks
        ]
        if bulk_actions:
            bulk(self.client, bulk_actions, refresh=True)
        self.client.index(
            index=self.settings.opensearch_doc_index,
            id=str(document["id"]),
            body={
                "docId": str(document["id"]),
                "docNo": document["doc_no"],
                "docTitle": document["doc_title"],
                "versionNo": version["version_no"],
                "reviewStatus": version["review_status_code"],
                "publishStatus": version["publish_status_code"],
                "sourceType": document["doc_source_code"],
                "tags": [],
                "createdAt": document["created_at"].isoformat() if document.get("created_at") else None,
                "updatedAt": document["updated_at"].isoformat() if document.get("updated_at") else None,
            },
            refresh=True,
        )

    def lexical_search(self, kb_code: str, query: str, top_k: int) -> list[dict[str, Any]]:
        self.ensure_indices()
        result = self.client.search(
            index=self.settings.opensearch_chunk_index,
            body={
                "size": top_k,
                "query": {
                    "bool": {
                        "must": [{"match": {"lexicalText": {"query": query}}}],
                        "filter": [
                            {"term": {"kbCode": kb_code}},
                            {"term": {"publishStatus": "PUBLISHED"}},
                        ],
                    }
                },
            },
        )
        return self._hits_to_payload(result, channel="LEXICAL")

    def dense_search(self, kb_code: str, query: str, top_k: int) -> list[dict[str, Any]]:
        self.ensure_indices()
        result = self.client.search(
            index=self.settings.opensearch_chunk_index,
            body={
                "size": top_k,
                "query": {
                    "bool": {
                        "must": [
                            {
                                "knn": {
                                    "denseVector": {
                                        "vector": self.embedder.embed(query),
                                        "k": top_k,
                                    }
                                }
                            }
                        ],
                        "filter": [
                            {"term": {"kbCode": kb_code}},
                            {"term": {"publishStatus": "PUBLISHED"}},
                        ],
                    }
                },
            },
        )
        return self._hits_to_payload(result, channel="DENSE")

    def delete_document_chunks(self, doc_id: int) -> None:
        if self.client.indices.exists(index=self.settings.opensearch_chunk_index):
            self.client.delete_by_query(
                index=self.settings.opensearch_chunk_index,
                body={"query": {"term": {"docId": str(doc_id)}}},
                refresh=True,
                conflicts="proceed",
            )

    @staticmethod
    def _hits_to_payload(result: dict[str, Any], channel: str) -> list[dict[str, Any]]:
        hits = []
        for rank, hit in enumerate(result.get("hits", {}).get("hits", []), start=1):
            source = hit.get("_source") or {}
            hits.append(
                {
                    "evidence_id": source.get("chunkId"),
                    "chunk_id": int(source.get("chunkId")),
                    "doc_id": int(source.get("docId")),
                    "doc_title": source.get("docTitle"),
                    "source_uri": source.get("sourceUri"),
                    "chunk_text": source.get("chunkText"),
                    "score": float(hit.get("_score") or 0.0),
                    "rank": rank,
                    "channel": channel,
                }
            )
        return hits
