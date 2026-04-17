from __future__ import annotations

import uuid
from typing import Any

from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session

from app.core.db import session_scope
from app.core.time_utils import local_naive_now
from app.models.rag import (
    KnowledgeBase,
    KnowledgeDocument,
    KnowledgeDocumentChunk,
    KnowledgeRebuildJob,
    LlmCallLog,
    RagRequestLog,
    RagRetrievalLog,
    RagSession,
)


def _row_to_dict(obj: Any) -> dict[str, Any]:
    if obj is None:
        return {}
    return {
        column.name: getattr(obj, column.name)
        for column in obj.__table__.columns
    }


class RagRepository:
    """RAG / knowledge base domain repository."""

    # ----- knowledge base ------------------------------------------------

    def ensure_knowledge_base(
        self,
        kb_code: str,
        kb_name: str,
        kb_type_code: str,
        knowledge_version: str,
        embedding_model: str,
        vector_store_type_code: str,
        vector_store_path: str,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            stmt = select(KnowledgeBase).where(
                KnowledgeBase.kb_code == kb_code,
                KnowledgeBase.deleted_flag == "0",
            )
            existing = session.execute(stmt).scalar_one_or_none()
            if existing is not None:
                existing.kb_name = kb_name
                existing.kb_type_code = kb_type_code
                existing.knowledge_version = knowledge_version
                existing.embedding_model = embedding_model
                existing.vector_store_type_code = vector_store_type_code
                existing.vector_store_path = vector_store_path
                existing.updated_at = now
                session.flush()
                return _row_to_dict(existing)
            kb = KnowledgeBase(
                kb_code=kb_code,
                kb_name=kb_name,
                kb_type_code=kb_type_code,
                knowledge_version=knowledge_version,
                embedding_model=embedding_model,
                vector_store_type_code=vector_store_type_code,
                vector_store_path=vector_store_path,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(kb)
            session.flush()
            return _row_to_dict(kb)

    def get_knowledge_base(self, kb_code: str) -> dict[str, Any] | None:
        with session_scope() as session:
            stmt = select(KnowledgeBase).where(
                KnowledgeBase.kb_code == kb_code,
                KnowledgeBase.deleted_flag == "0",
            )
            kb = session.execute(stmt).scalar_one_or_none()
            return _row_to_dict(kb) if kb else None

    # ----- documents -----------------------------------------------------

    def create_document(
        self,
        kb_id: int,
        doc_title: str,
        content_text: str,
        doc_no: str | None,
        doc_source_code: str,
        source_uri: str | None,
        doc_version: str,
        review_status_code: str,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        reviewed_at = now if review_status_code == "APPROVED" else None
        doc_no = doc_no or f"DOC-{uuid.uuid4().hex[:16].upper()}"
        with session_scope() as session:
            doc = KnowledgeDocument(
                kb_id=kb_id,
                doc_no=doc_no,
                doc_title=doc_title,
                doc_source_code=doc_source_code,
                source_uri=source_uri,
                doc_version=doc_version,
                content_text=content_text,
                review_status_code=review_status_code,
                reviewed_at=reviewed_at,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(doc)
            session.flush()
            return _row_to_dict(doc)

    def list_approved_documents(self, kb_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            stmt = (
                select(KnowledgeDocument)
                .where(
                    KnowledgeDocument.kb_id == kb_id,
                    KnowledgeDocument.review_status_code == "APPROVED",
                    KnowledgeDocument.enabled_flag == "1",
                    KnowledgeDocument.deleted_flag == "0",
                )
                .order_by(KnowledgeDocument.id)
            )
            docs = session.execute(stmt).scalars().all()
            return [_row_to_dict(doc) for doc in docs]

    # ----- chunks --------------------------------------------------------

    def replace_chunks(
        self,
        kb_id: int,
        chunks: list[dict[str, Any]],
        embedding_model: str,
        vector_store_path: str,
    ) -> list[dict[str, Any]]:
        now = local_naive_now()
        stored: list[dict[str, Any]] = []
        with session_scope() as session:
            session.execute(
                delete(KnowledgeDocumentChunk).where(KnowledgeDocumentChunk.kb_id == kb_id)
            )
            for item in chunks:
                chunk = KnowledgeDocumentChunk(
                    kb_id=kb_id,
                    doc_id=item["doc_id"],
                    chunk_no=item["chunk_no"],
                    chunk_text=item["chunk_text"],
                    token_count=item.get("token_count"),
                    embedding_model=embedding_model,
                    vector_store_path=vector_store_path,
                    org_id=item.get("org_id"),
                    created_at=now,
                )
                session.add(chunk)
                session.flush()
                chunk.vector_id = f"chunk-{chunk.id}"
                session.flush()
                stored.append(self._chunk_joined_dict(session, chunk.id))
        return stored

    def list_chunks(self, kb_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            stmt = (
                select(KnowledgeDocumentChunk, KnowledgeDocument)
                .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeDocumentChunk.doc_id)
                .where(
                    KnowledgeDocumentChunk.kb_id == kb_id,
                    KnowledgeDocumentChunk.enabled_flag == "1",
                    KnowledgeDocumentChunk.deleted_flag == "0",
                )
                .order_by(KnowledgeDocumentChunk.id)
            )
            return [self._join_row_to_dict(chunk, doc) for chunk, doc in session.execute(stmt).all()]

    @staticmethod
    def _chunk_joined_dict(session: Session, chunk_id: int) -> dict[str, Any]:
        stmt = (
            select(KnowledgeDocumentChunk, KnowledgeDocument)
            .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeDocumentChunk.doc_id)
            .where(KnowledgeDocumentChunk.id == chunk_id)
        )
        row = session.execute(stmt).one_or_none()
        if row is None:
            return {}
        chunk, doc = row
        return RagRepository._join_row_to_dict(chunk, doc)

    @staticmethod
    def _join_row_to_dict(chunk: KnowledgeDocumentChunk, doc: KnowledgeDocument) -> dict[str, Any]:
        result = _row_to_dict(chunk)
        result.update(
            {
                "doc_title": doc.doc_title,
                "doc_no": doc.doc_no,
                "source_uri": doc.source_uri,
                "doc_source_code": doc.doc_source_code,
            }
        )
        return result

    # ----- rebuild jobs --------------------------------------------------

    def create_rebuild_job(
        self,
        kb_id: int,
        knowledge_version: str,
        vector_store_path: str,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        job_no = f"KBREBUILD-{uuid.uuid4().hex[:16].upper()}"
        with session_scope() as session:
            job = KnowledgeRebuildJob(
                rebuild_job_no=job_no,
                kb_id=kb_id,
                knowledge_version=knowledge_version,
                rebuild_status_code="RUNNING",
                vector_store_path=vector_store_path,
                started_at=now,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(job)
            session.flush()
            return _row_to_dict(job)

    def finish_rebuild_job(
        self,
        job_id: int,
        status_code: str,
        chunk_count: int,
        error_message: str | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(KnowledgeRebuildJob)
                .where(KnowledgeRebuildJob.id == job_id)
                .values(
                    rebuild_status_code=status_code,
                    chunk_count=chunk_count,
                    error_message=error_message,
                    finished_at=now,
                    updated_at=now,
                )
            )
            job = session.execute(
                select(KnowledgeRebuildJob).where(KnowledgeRebuildJob.id == job_id)
            ).scalar_one_or_none()
            return _row_to_dict(job) if job else {}

    # ----- rag sessions / requests --------------------------------------

    def create_rag_session(
        self,
        session_type_code: str,
        knowledge_version: str,
        model_name: str,
        related_biz_no: str | None,
        patient_uuid: str | None,
        java_user_id: int | None,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        session_no = f"RAG-{uuid.uuid4().hex[:16].upper()}"
        with session_scope() as session:
            row = RagSession(
                session_no=session_no,
                session_type_code=session_type_code,
                related_biz_no=related_biz_no,
                patient_uuid=patient_uuid,
                java_user_id=java_user_id,
                knowledge_version=knowledge_version,
                model_name=model_name,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def create_rag_request(
        self,
        session_id: int,
        request_type_code: str,
        user_query: str,
        rewritten_query: str,
        top_k: int,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        request_no = f"RAGREQ-{uuid.uuid4().hex[:16].upper()}"
        with session_scope() as session:
            row = RagRequestLog(
                session_id=session_id,
                request_no=request_no,
                request_type_code=request_type_code,
                user_query=user_query,
                rewritten_query=rewritten_query,
                top_k=top_k,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def finish_rag_request(
        self,
        request_id: int,
        answer_text: str,
        status_code: str,
        latency_ms: int,
        safety_flag: str = "0",
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(RagRequestLog)
                .where(RagRequestLog.id == request_id)
                .values(
                    answer_text=answer_text,
                    request_status_code=status_code,
                    latency_ms=latency_ms,
                    safety_flag=safety_flag,
                    updated_at=now,
                )
            )

    def create_retrieval_logs(
        self,
        request_id: int,
        hits: list[dict[str, Any]],
        org_id: int | None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            for rank, hit in enumerate(hits, start=1):
                session.add(
                    RagRetrievalLog(
                        request_id=request_id,
                        chunk_id=hit["chunk_id"],
                        rank_no=rank,
                        retrieval_score=hit.get("score"),
                        doc_id=hit["doc_id"],
                        chunk_text_snapshot=hit.get("chunk_text"),
                        cited_flag="1",
                        org_id=org_id,
                        created_at=now,
                    )
                )

    def create_llm_call_log(
        self,
        request_id: int,
        model_name: str,
        provider_code: str,
        prompt_text: str,
        completion_text: str,
        latency_ms: int,
        status_code: str,
        org_id: int | None,
        error_message: str | None = None,
    ) -> None:
        now = local_naive_now()
        prompt_tokens = len(prompt_text.split())
        completion_tokens = len(completion_text.split())
        with session_scope() as session:
            session.add(
                LlmCallLog(
                    request_id=request_id,
                    model_name=model_name,
                    provider_code=provider_code,
                    prompt_text=prompt_text,
                    completion_text=completion_text,
                    prompt_tokens=prompt_tokens,
                    completion_tokens=completion_tokens,
                    total_tokens=prompt_tokens + completion_tokens,
                    latency_ms=latency_ms,
                    call_status_code=status_code,
                    error_message=error_message,
                    org_id=org_id,
                    created_at=now,
                )
            )
