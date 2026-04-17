from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from sqlalchemy import CHAR, JSON, BigInteger, DateTime, Index, Integer, Numeric, String, Text
from sqlalchemy.dialects.mysql import LONGTEXT
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import AuditMixin, Base, RemarkMixin, SoftDeleteMixin, StatusMixin, TimestampMixin


class KnowledgeBase(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "kb_knowledge_base"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    kb_code: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    kb_name: Mapped[str] = mapped_column(String(128), nullable=False)
    kb_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PATIENT_GUIDE", server_default="PATIENT_GUIDE"
    )
    knowledge_version: Mapped[str] = mapped_column(
        String(64), nullable=False, default="v1.0", server_default="v1.0"
    )
    embedding_model: Mapped[str | None] = mapped_column(String(64), nullable=True)
    vector_store_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="LOCAL_JSON", server_default="LOCAL_JSON"
    )
    vector_store_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    enabled_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="1", server_default="1"
    )
    status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="ACTIVE", server_default="ACTIVE"
    )


class KnowledgeDocument(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "kb_document"
    __table_args__ = (
        Index("idx_kb_document_kb_review", "kb_id", "review_status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    kb_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    doc_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    doc_title: Mapped[str] = mapped_column(String(255), nullable=False)
    doc_source_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="INTERNAL", server_default="INTERNAL"
    )
    source_uri: Mapped[str | None] = mapped_column(String(500), nullable=True)
    doc_version: Mapped[str] = mapped_column(
        String(64), nullable=False, default="v1.0", server_default="v1.0"
    )
    content_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    content_attachment_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    review_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    reviewer_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    enabled_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="1", server_default="1"
    )


class KnowledgeDocumentChunk(Base):
    """Lightweight chunk table: no remark/created_by/updated_*."""

    __tablename__ = "kb_document_chunk"
    __table_args__ = (
        Index("idx_kb_chunk_kb_doc", "kb_id", "doc_id"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    kb_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    chunk_no: Mapped[int] = mapped_column(Integer, nullable=False)
    chunk_text: Mapped[str] = mapped_column(Text, nullable=False)
    token_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    embedding_model: Mapped[str | None] = mapped_column(String(64), nullable=True)
    vector_store_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    vector_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    enabled_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="1", server_default="1"
    )
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    status: Mapped[str] = mapped_column(
        String(32), nullable=False, default="ACTIVE", server_default="ACTIVE"
    )
    deleted_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="0", server_default="0"
    )
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeRebuildJob(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "kb_rebuild_job"
    __table_args__ = (
        Index("idx_kb_rebuild_job_kb", "kb_id", "rebuild_status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    rebuild_job_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    kb_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    knowledge_version: Mapped[str] = mapped_column(String(64), nullable=False)
    rebuild_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="RUNNING", server_default="RUNNING"
    )
    chunk_count: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0, server_default="0"
    )
    vector_store_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)


class RagSession(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "rag_session"
    __table_args__ = (
        Index("idx_rag_session_type", "session_type_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    session_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    session_type_code: Mapped[str] = mapped_column(String(32), nullable=False)
    related_biz_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    patient_uuid: Mapped[str | None] = mapped_column(String(128), nullable=True)
    java_user_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    knowledge_version: Mapped[str | None] = mapped_column(String(64), nullable=True)
    model_name: Mapped[str | None] = mapped_column(String(64), nullable=True)


class RagRequestLog(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "rag_request_log"
    __table_args__ = (
        Index("idx_rag_request_session", "session_id", "request_type_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    session_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    request_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    request_type_code: Mapped[str] = mapped_column(String(32), nullable=False)
    user_query: Mapped[str] = mapped_column(Text, nullable=False)
    rewritten_query: Mapped[str | None] = mapped_column(Text, nullable=True)
    top_k: Mapped[int] = mapped_column(
        Integer, nullable=False, default=5, server_default="5"
    )
    answer_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    request_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="SUCCESS", server_default="SUCCESS"
    )
    safety_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="0", server_default="0"
    )
    latency_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)


class RagRetrievalLog(Base):
    """Pure log table: no status/deleted_flag/remark/updated_*."""

    __tablename__ = "rag_retrieval_log"
    __table_args__ = (
        Index("idx_rag_retrieval_request", "request_id", "rank_no"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    request_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    chunk_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    rank_no: Mapped[int] = mapped_column(Integer, nullable=False)
    retrieval_score: Mapped[Decimal | None] = mapped_column(Numeric(10, 6), nullable=True)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    chunk_text_snapshot: Mapped[str | None] = mapped_column(Text, nullable=True)
    cited_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="0", server_default="0"
    )
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class LlmCallLog(Base):
    """Pure log table: no status/deleted_flag/remark/updated_*."""

    __tablename__ = "llm_call_log"
    __table_args__ = (
        Index("idx_llm_call_request", "request_id", "model_name"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    request_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    model_name: Mapped[str] = mapped_column(String(128), nullable=False)
    provider_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    prompt_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    completion_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    prompt_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)
    completion_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)
    total_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)
    latency_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    call_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="SUCCESS", server_default="SUCCESS"
    )
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
