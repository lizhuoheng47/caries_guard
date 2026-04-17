from __future__ import annotations

import re
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.repositories.metadata_repository import MetadataRepository
from app.schemas.rag import KnowledgeDocumentRequest, KnowledgeRebuildRequest


class KnowledgeService:
    def __init__(self, settings: Settings, repository: MetadataRepository, vector_store: SimpleVectorStore) -> None:
        self.settings = settings
        self.repository = repository
        self.vector_store = vector_store

    def import_document(self, request: KnowledgeDocumentRequest) -> dict[str, Any]:
        kb = self.ensure_knowledge_base(
            kb_code=request.kb_code,
            kb_name=request.kb_name,
            kb_type_code=request.kb_type_code,
            knowledge_version=request.doc_version,
            org_id=request.org_id,
        )
        document = self.repository.create_document(
            kb_id=kb["id"],
            doc_title=request.doc_title,
            content_text=request.content_text,
            doc_no=request.doc_no,
            doc_source_code=request.doc_source_code,
            source_uri=request.source_uri,
            doc_version=request.doc_version,
            review_status_code=request.review_status_code,
            org_id=request.org_id,
        )
        return {
            "kbCode": kb["kb_code"],
            "kbId": kb["id"],
            "docId": document["id"],
            "docNo": document["doc_no"],
            "docTitle": document["doc_title"],
            "reviewStatusCode": document["review_status_code"],
        }

    def rebuild(self, request: KnowledgeRebuildRequest) -> dict[str, Any]:
        kb = self.ensure_knowledge_base(
            kb_code=request.kb_code,
            kb_name=request.kb_name,
            kb_type_code=request.kb_type_code,
            knowledge_version=request.knowledge_version,
            org_id=request.org_id,
        )
        job = self.repository.create_rebuild_job(
            kb_id=kb["id"],
            knowledge_version=kb["knowledge_version"],
            vector_store_path=kb["vector_store_path"],
            org_id=request.org_id,
        )
        try:
            documents = self.repository.list_approved_documents(kb["id"])
            chunks = self._chunk_documents(documents)
            stored_chunks = self.repository.replace_chunks(
                kb_id=kb["id"],
                chunks=chunks,
                embedding_model=self.settings.rag_embedding_model,
                vector_store_path=kb["vector_store_path"],
            )
            self.vector_store.build(kb["vector_store_path"], kb, stored_chunks)
            finished = self.repository.finish_rebuild_job(job["id"], "SUCCESS", len(stored_chunks))
            return {
                "rebuildJobNo": finished["rebuild_job_no"],
                "kbCode": kb["kb_code"],
                "knowledgeVersion": kb["knowledge_version"],
                "rebuildStatusCode": finished["rebuild_status_code"],
                "chunkCount": finished["chunk_count"],
                "vectorStorePath": kb["vector_store_path"],
            }
        except Exception as exc:
            self.repository.finish_rebuild_job(job["id"], "FAILED", 0, str(exc))
            raise

    def ensure_knowledge_base(
        self,
        kb_code: str | None = None,
        kb_name: str | None = None,
        kb_type_code: str = "PATIENT_GUIDE",
        knowledge_version: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        code = kb_code or self.settings.rag_default_kb_code
        version = knowledge_version or self.settings.rag_knowledge_version
        vector_store_path = str(Path(self.settings.rag_index_dir) / f"{self._safe_name(code)}.json")
        return self.repository.ensure_knowledge_base(
            kb_code=code,
            kb_name=kb_name or self.settings.rag_default_kb_name,
            kb_type_code=kb_type_code,
            knowledge_version=version,
            embedding_model=self.settings.rag_embedding_model,
            vector_store_type_code=self.settings.rag_vector_store_type,
            vector_store_path=vector_store_path,
            org_id=org_id,
        )

    def _chunk_documents(self, documents: list[dict[str, Any]]) -> list[dict[str, Any]]:
        chunks: list[dict[str, Any]] = []
        for document in documents:
            for chunk_no, text in enumerate(self._split_text(document.get("content_text") or ""), start=1):
                chunks.append(
                    {
                        "doc_id": document["id"],
                        "chunk_no": chunk_no,
                        "chunk_text": text,
                        "token_count": len(text),
                        "org_id": document.get("org_id"),
                    }
                )
        return chunks

    @staticmethod
    def _split_text(text: str, chunk_size: int = 500, overlap: int = 80) -> list[str]:
        normalized = re.sub(r"\s+", " ", text).strip()
        if not normalized:
            return []
        chunks: list[str] = []
        start = 0
        while start < len(normalized):
            end = min(len(normalized), start + chunk_size)
            chunks.append(normalized[start:end])
            if end == len(normalized):
                break
            start = max(0, end - overlap)
        return chunks

    @staticmethod
    def _safe_name(value: str) -> str:
        cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", value.strip())
        return cleaned.strip("-") or "default"
