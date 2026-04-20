from __future__ import annotations

from datetime import datetime
from typing import Any

from opensearchpy import OpenSearch
from opensearchpy.helpers import bulk

from app.core.config import Settings
from app.infra.vector.base_embedding_provider import BaseEmbeddingProvider


class OpenSearchIndexService:
    def __init__(self, settings: Settings, client: OpenSearch, embedding_provider: BaseEmbeddingProvider) -> None:
        self.settings = settings
        self.client = client
        self.embedding_provider = embedding_provider

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
                            "docNo": {"type": "keyword"},
                            "kbCode": {"type": "keyword"},
                            "docTitle": {"type": "text"},
                            "sourceUri": {"type": "keyword"},
                            "chunkText": {"type": "text"},
                            "medicalTags": {"type": "keyword"},
                            "graphEntityRefs": {"type": "keyword"},
                            "publishStatus": {"type": "keyword"},
                            "versionNo": {"type": "keyword"},
                            "lexicalText": {"type": "text"},
                            "denseVector": {
                                "type": "knn_vector",
                                "dimension": self.embedding_provider.metadata.dimension,
                                "method": {"engine": "lucene", "space_type": "cosinesimil", "name": "hnsw"},
                            },
                            "embeddingProvider": {"type": "keyword"},
                            "embeddingModel": {"type": "keyword"},
                            "embeddingVersion": {"type": "keyword"},
                            "sourceType": {"type": "keyword"},
                            "sourceAuthorityScore": {"type": "float"},
                            "freshnessScore": {"type": "float"},
                            "createdAt": {"type": "date"},
                            "updatedAt": {"type": "date"},
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
                            "sourceAuthorityScore": {"type": "float"},
                            "embeddingProvider": {"type": "keyword"},
                            "embeddingModel": {"type": "keyword"},
                            "embeddingVersion": {"type": "keyword"},
                            "tags": {"type": "keyword"},
                            "createdAt": {"type": "date"},
                            "updatedAt": {"type": "date"},
                        }
                    },
                },
            )

    def index_document_version(
        self,
        kb_code: str,
        document: dict[str, Any],
        version: dict[str, Any],
        chunks: list[dict[str, Any]],
    ) -> dict[str, Any]:
        self._validate_embedding_metadata_consistency()
        self.ensure_indices()
        vectors = self._embed_chunk_texts([chunk["chunk_text"] for chunk in chunks])
        bulk_actions = []
        for chunk, vector in zip(chunks, vectors):
            bulk_actions.append(
                {
                    "_index": self.settings.opensearch_chunk_index,
                    "_id": str(chunk["id"]),
                    "_source": {
                        "chunkId": str(chunk["id"]),
                        "docId": str(document["id"]),
                        "docNo": document["doc_no"],
                        "kbCode": kb_code,
                        "docTitle": document["doc_title"],
                        "sourceUri": chunk.get("source_uri") or document.get("source_uri"),
                        "chunkText": chunk["chunk_text"],
                        "medicalTags": chunk.get("medical_tags") or [],
                        "graphEntityRefs": chunk.get("graph_entity_refs") or [],
                        "publishStatus": chunk.get("publish_status", "INDEXED"),
                        "versionNo": version["version_no"],
                        "lexicalText": chunk["chunk_text"],
                        "denseVector": vector,
                        "embeddingProvider": self.embedding_provider.metadata.provider,
                        "embeddingModel": self.embedding_provider.metadata.model,
                        "embeddingVersion": self.embedding_provider.metadata.version,
                        "sourceType": document.get("doc_source_code"),
                        "sourceAuthorityScore": self._authority_score(document.get("doc_source_code")),
                        "freshnessScore": self._freshness_score(document.get("updated_at") or version.get("updated_at")),
                        "createdAt": self._to_iso(chunk.get("created_at") or document.get("created_at")),
                        "updatedAt": self._to_iso(document.get("updated_at") or version.get("updated_at")),
                    },
                }
            )
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
                "sourceAuthorityScore": self._authority_score(document["doc_source_code"]),
                "embeddingProvider": self.embedding_provider.metadata.provider,
                "embeddingModel": self.embedding_provider.metadata.model,
                "embeddingVersion": self.embedding_provider.metadata.version,
                "tags": [],
                "createdAt": self._to_iso(document.get("created_at")),
                "updatedAt": self._to_iso(document.get("updated_at")),
            },
            refresh=True,
        )
        return {
            "indexedChunkCount": len(chunks),
            "embeddingProvider": self.embedding_provider.metadata.provider,
            "embeddingModel": self.embedding_provider.metadata.model,
            "embeddingVersion": self.embedding_provider.metadata.version,
        }

    def lexical_search(self, kb_code: str, query: str, top_k: int) -> list[dict[str, Any]]:
        self.ensure_indices()
        result = self.client.search(
            index=self.settings.opensearch_chunk_index,
            body={
                "size": top_k,
                "query": {
                    "bool": {
                        "must": [
                            {
                                "multi_match": {
                                    "query": query,
                                    "fields": ["lexicalText^2", "docTitle^1.4", "medicalTags^1.1", "graphEntityRefs^1.2"],
                                }
                            }
                        ],
                        "filter": [{"term": {"kbCode": kb_code}}, {"term": {"publishStatus": "PUBLISHED"}}],
                    }
                },
                "sort": [{"_score": {"order": "desc"}}],
            },
        )
        return self._hits_to_payload(result, channel="LEXICAL")

    def dense_search(self, kb_code: str, query: str, top_k: int) -> list[dict[str, Any]]:
        self.ensure_indices()
        self._validate_index_dimension()
        query_vector = self.embedding_provider.embed(query)
        result = self.client.search(
            index=self.settings.opensearch_chunk_index,
            body={
                "size": top_k,
                "query": {
                    "bool": {
                        "must": [{"knn": {"denseVector": {"vector": query_vector, "k": top_k}}}],
                        "filter": [{"term": {"kbCode": kb_code}}, {"term": {"publishStatus": "PUBLISHED"}}],
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
        if self.client.indices.exists(index=self.settings.opensearch_doc_index):
            self.client.delete(index=self.settings.opensearch_doc_index, id=str(doc_id), ignore=[404], refresh=True)

    def rebuild_documents(self, kb_code: str, versioned_documents: list[tuple[dict[str, Any], dict[str, Any], list[dict[str, Any]]]]) -> dict[str, Any]:
        processed = []
        total_chunks = 0
        for document, version, chunks in versioned_documents:
            self.delete_document_chunks(int(document["id"]))
            res = self.index_document_version(kb_code, document, version, chunks)
            processed.append(
                {
                    "docId": document["id"],
                    "docNo": document["doc_no"],
                    **res,
                }
            )
            total_chunks += res.get("indexedChunkCount", 0)
        return {
            "kbCode": kb_code,
            "processedDocuments": processed,
            "totalDocs": len(processed),
            "totalChunks": total_chunks,
            "embeddingProvider": self.embedding_provider.metadata.provider,
            "embeddingModel": self.embedding_provider.metadata.model,
            "embeddingVersion": self.embedding_provider.metadata.version,
        }

    def _validate_embedding_metadata_consistency(self) -> None:
        """Check if existing index has different embedding metadata to prevent contamination."""
        index_name = self.settings.opensearch_chunk_index
        if not self.client.indices.exists(index=index_name):
            return

        # Sample one document to check metadata
        query = {"query": {"match_all": {}}, "size": 1}
        res = self.client.search(index=index_name, body=query)
        hits = res.get("hits", {}).get("hits", [])
        if not hits:
            return

        source = hits[0].get("_source", {})
        old_provider = source.get("embeddingProvider")
        old_model = source.get("embeddingModel")
        
        current = self.embedding_provider.metadata
        if old_provider and old_provider != current.provider:
            raise RuntimeError(
                f"Index metadata mismatch: Index uses '{old_provider}', "
                f"but current provider is '{current.provider}'. Rebuild required."
            )
        if old_model and old_model != current.model:
             raise RuntimeError(
                f"Index metadata mismatch: Index uses model '{old_model}', "
                f"but current model is '{current.model}'. Rebuild required."
            )

    def _validate_index_dimension(self) -> None:
        """Ensure the search vector dimension matches the index mapping."""
        index_name = self.settings.opensearch_chunk_index
        if not self.client.indices.exists(index=index_name):
            return

        mapping = self.client.indices.get_mapping(index=index_name)
        props = mapping.get(index_name, {}).get("mappings", {}).get("properties", {})
        dense_vector = props.get("denseVector", {})
        index_dim = dense_vector.get("dimension")
        
        if index_dim is not None and index_dim != self.embedding_provider.metadata.dimension:
            raise ValueError(
                f"Dimension mismatch: Index '{index_name}' expects {index_dim}, "
                f"but provider generates {self.embedding_provider.metadata.dimension}."
            )

    def _embed_chunk_texts(self, texts: list[str]) -> list[list[float]]:
        vectors: list[list[float]] = []
        batch_size = max(1, self.settings.rag_embedding_batch_size)
        for offset in range(0, len(texts), batch_size):
            batch = texts[offset : offset + batch_size]
            vectors.extend(self.embedding_provider.embed_many(batch))
        return vectors

    def _authority_score(self, source_type: str | None) -> float:
        if not source_type:
            return 0.6
        return float(self.settings.rag_source_authority_weights.get(source_type.upper(), 0.6))

    def _freshness_score(self, dt: Any) -> float:
        if not isinstance(dt, datetime):
            return 0.5
        age_days = max(0, (datetime.now() - dt).days)
        decay = max(1, self.settings.rag_freshness_decay_days)
        score = 1.0 - min(age_days / decay, 1.0)
        return round(score, 4)

    @staticmethod
    def _to_iso(value: Any) -> str | None:
        return value.isoformat() if isinstance(value, datetime) else None

    @staticmethod
    def _hits_to_payload(result: dict[str, Any], channel: str) -> list[dict[str, Any]]:
        hits = []
        for rank, hit in enumerate(result.get("hits", {}).get("hits", []), start=1):
            source = hit.get("_source") or {}
            chunk_id = source.get("chunkId")
            doc_id = source.get("docId")
            if chunk_id is None or doc_id is None:
                continue
            hits.append(
                {
                    "evidence_id": str(chunk_id),
                    "chunk_id": int(chunk_id),
                    "doc_id": int(doc_id),
                    "doc_no": source.get("docNo"),
                    "doc_title": source.get("docTitle"),
                    "doc_version": source.get("versionNo"),
                    "source_uri": source.get("sourceUri"),
                    "chunk_text": source.get("chunkText"),
                    "score": float(hit.get("_score") or 0.0),
                    "rank": rank,
                    "channel": channel,
                    "embedding_provider": source.get("embeddingProvider"),
                    "embedding_model": source.get("embeddingModel"),
                    "embedding_version": source.get("embeddingVersion"),
                    "source_type": source.get("sourceType"),
                    "source_authority_score": float(source.get("sourceAuthorityScore") or 0.0),
                    "freshness_score": float(source.get("freshnessScore") or 0.0),
                    "graph_entity_refs": source.get("graphEntityRefs") or [],
                    "evidence_kind": "TEXT",
                }
            )
        return hits
