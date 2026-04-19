from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from sqlalchemy import CHAR, JSON, BigInteger, DateTime, Index, Integer, Numeric, String, Text, UniqueConstraint
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
        String(32), nullable=False, default="OPENSEARCH", server_default="OPENSEARCH"
    )
    vector_store_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    enabled_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="1", server_default="1")
    status_code: Mapped[str] = mapped_column(String(32), nullable=False, default="ACTIVE", server_default="ACTIVE")
    published_version_no: Mapped[str | None] = mapped_column(String(64), nullable=True)


class KnowledgeDocument(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "kb_document"
    __table_args__ = (
        Index("idx_kb_document_kb_review", "kb_id", "review_status_code"),
        Index("idx_kb_document_kb_publish", "kb_id", "publish_status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    kb_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    doc_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    doc_title: Mapped[str] = mapped_column(String(255), nullable=False)
    doc_source_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="INTERNAL", server_default="INTERNAL"
    )
    source_uri: Mapped[str | None] = mapped_column(String(500), nullable=True)
    doc_version: Mapped[str] = mapped_column(String(64), nullable=False, default="v1.0", server_default="v1.0")
    content_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    content_attachment_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    review_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="DRAFT", server_default="DRAFT"
    )
    publish_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="DRAFT", server_default="DRAFT"
    )
    reviewer_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    current_version_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    published_version_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    source_file_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    enabled_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="1", server_default="1")


class KnowledgeDocumentChunk(Base):
    __tablename__ = "kb_document_chunk"
    __table_args__ = (
        Index("idx_kb_chunk_kb_doc", "kb_id", "doc_id"),
        Index("idx_kb_chunk_doc_version_publish", "doc_id", "version_no", "publish_status"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    kb_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    version_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    chunk_no: Mapped[int] = mapped_column(Integer, nullable=False)
    section_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    chunk_text: Mapped[str] = mapped_column(Text, nullable=False)
    chunk_type: Mapped[str] = mapped_column(String(32), nullable=False, default="PARAGRAPH", server_default="PARAGRAPH")
    token_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    embedding_model: Mapped[str | None] = mapped_column(String(64), nullable=True)
    vector_store_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    vector_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    doc_title_snapshot: Mapped[str | None] = mapped_column(String(255), nullable=True)
    doc_source_code: Mapped[str | None] = mapped_column(String(32), nullable=True)
    source_uri: Mapped[str | None] = mapped_column(String(500), nullable=True)
    medical_tags: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    graph_entity_refs: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    publish_status: Mapped[str] = mapped_column(String(32), nullable=False, default="DRAFT", server_default="DRAFT")
    enabled_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="1", server_default="1")
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    status: Mapped[str] = mapped_column(String(32), nullable=False, default="ACTIVE", server_default="ACTIVE")
    deleted_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeRebuildJob(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "kb_rebuild_job"
    __table_args__ = (Index("idx_kb_rebuild_job_kb", "kb_id", "rebuild_status_code"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    rebuild_job_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    kb_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    knowledge_version: Mapped[str] = mapped_column(String(64), nullable=False)
    rebuild_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="RUNNING", server_default="RUNNING"
    )
    chunk_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0, server_default="0")
    vector_store_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)


class KnowledgeSourceFile(Base, AuditMixin, SoftDeleteMixin, TimestampMixin):
    __tablename__ = "kb_source_file"
    __table_args__ = (
        Index("idx_kb_source_file_doc", "doc_id", "parse_status_code"),
        UniqueConstraint("source_file_no", name="uk_kb_source_file_no"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    source_file_no: Mapped[str] = mapped_column(String(64), nullable=False)
    kb_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    doc_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    bucket_name: Mapped[str] = mapped_column(String(128), nullable=False)
    object_key: Mapped[str] = mapped_column(String(500), nullable=False)
    file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    mime_type: Mapped[str | None] = mapped_column(String(128), nullable=True)
    file_size_bytes: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    md5: Mapped[str | None] = mapped_column(String(64), nullable=True)
    source_type_code: Mapped[str] = mapped_column(String(32), nullable=False, default="UPLOAD", server_default="UPLOAD")
    uploaded_by: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    uploaded_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    parse_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )


class KnowledgeSourceParseResult(Base, TimestampMixin):
    __tablename__ = "kb_source_parse_result"
    __table_args__ = (
        Index("idx_kb_parse_result_source", "source_file_id", "parse_status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    source_file_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    parse_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="SUCCESS", server_default="SUCCESS"
    )
    normalized_markdown: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    structured_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    section_tree: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    table_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    metadata_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    artifact_bucket_name: Mapped[str | None] = mapped_column(String(128), nullable=True)
    artifact_object_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)


class KnowledgeIngestJob(Base, AuditMixin, TimestampMixin):
    __tablename__ = "kb_ingest_job"
    __table_args__ = (
        Index("idx_kb_ingest_job_doc", "doc_id", "ingest_status_code"),
        UniqueConstraint("ingest_job_no", name="uk_kb_ingest_job_no"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    ingest_job_no: Mapped[str] = mapped_column(String(64), nullable=False)
    kb_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    doc_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    source_file_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    ingest_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    current_step_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    error_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    trace_id: Mapped[str | None] = mapped_column(String(128), nullable=True)


class KnowledgeIngestJobStep(Base):
    __tablename__ = "kb_ingest_job_step"
    __table_args__ = (
        Index("idx_kb_ingest_job_step_job", "ingest_job_id", "step_order"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    ingest_job_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    step_code: Mapped[str] = mapped_column(String(64), nullable=False)
    step_order: Mapped[int] = mapped_column(Integer, nullable=False)
    step_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    message_text: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    payload_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeDocumentVersion(Base, AuditMixin, TimestampMixin):
    __tablename__ = "kb_document_version"
    __table_args__ = (
        Index("idx_kb_document_version_doc", "doc_id", "version_no"),
        UniqueConstraint("doc_id", "version_no", name="uk_kb_document_version_doc_no"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    version_no: Mapped[str] = mapped_column(String(64), nullable=False)
    parent_version_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    normalized_content: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    structured_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    section_tree: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    table_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    metadata_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    change_summary: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    review_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="DRAFT", server_default="DRAFT"
    )
    publish_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="DRAFT", server_default="DRAFT"
    )
    source_file_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)


class KnowledgePublishRecord(Base, AuditMixin):
    __tablename__ = "kb_publish_record"
    __table_args__ = (
        Index("idx_kb_publish_record_doc", "doc_id", "published_at"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    version_no: Mapped[str] = mapped_column(String(64), nullable=False)
    previous_version_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    action_code: Mapped[str] = mapped_column(String(32), nullable=False, default="PUBLISH", server_default="PUBLISH")
    comment_text: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    published_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeReviewRecord(Base, AuditMixin):
    __tablename__ = "kb_review_record"
    __table_args__ = (
        Index("idx_kb_review_record_doc", "doc_id", "reviewed_at"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    version_no: Mapped[str] = mapped_column(String(64), nullable=False)
    decision_code: Mapped[str] = mapped_column(String(32), nullable=False)
    review_comment: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    reviewed_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeEntity(Base, AuditMixin):
    __tablename__ = "kb_entity"
    __table_args__ = (
        Index("idx_kb_entity_doc", "source_doc_id", "review_status_code"),
        UniqueConstraint("entity_code", name="uk_kb_entity_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    entity_code: Mapped[str] = mapped_column(String(64), nullable=False)
    entity_name: Mapped[str] = mapped_column(String(255), nullable=False)
    entity_type_code: Mapped[str] = mapped_column(String(64), nullable=False)
    normalized_name: Mapped[str] = mapped_column(String(255), nullable=False)
    source_doc_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    source_chunk_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    confidence_score: Mapped[Decimal | None] = mapped_column(Numeric(8, 4), nullable=True)
    review_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="REVIEW_PENDING", server_default="REVIEW_PENDING"
    )
    enabled_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="1", server_default="1")
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeRelation(Base, AuditMixin):
    __tablename__ = "kb_relation"
    __table_args__ = (
        Index("idx_kb_relation_source_target", "source_entity_id", "target_entity_id"),
        UniqueConstraint("relation_code", name="uk_kb_relation_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    relation_code: Mapped[str] = mapped_column(String(64), nullable=False)
    source_entity_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    target_entity_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    relation_type_code: Mapped[str] = mapped_column(String(64), nullable=False)
    evidence_doc_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    evidence_chunk_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    confidence_score: Mapped[Decimal | None] = mapped_column(Numeric(8, 4), nullable=True)
    review_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="REVIEW_PENDING", server_default="REVIEW_PENDING"
    )
    enabled_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="1", server_default="1")
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeEntityAlias(Base):
    __tablename__ = "kb_entity_alias"
    __table_args__ = (
        Index("idx_kb_entity_alias_entity", "entity_id"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    entity_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    alias_name: Mapped[str] = mapped_column(String(255), nullable=False)
    normalized_alias_name: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class KnowledgeGraphSyncLog(Base, AuditMixin):
    __tablename__ = "kb_graph_sync_log"
    __table_args__ = (
        Index("idx_kb_graph_sync_doc", "doc_id", "version_no"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    version_no: Mapped[str] = mapped_column(String(64), nullable=False)
    sync_status_code: Mapped[str] = mapped_column(String(32), nullable=False)
    entity_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0, server_default="0")
    relation_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0, server_default="0")
    trace_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagSession(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "rag_session"
    __table_args__ = (Index("idx_rag_session_type", "session_type_code"),)

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
    __table_args__ = (Index("idx_rag_request_session", "session_id", "request_type_code"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    session_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    request_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    request_type_code: Mapped[str] = mapped_column(String(32), nullable=False)
    user_query: Mapped[str] = mapped_column(Text, nullable=False)
    rewritten_query: Mapped[str | None] = mapped_column(Text, nullable=True)
    intent_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    entity_link_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    top_k: Mapped[int] = mapped_column(Integer, nullable=False, default=5, server_default="5")
    answer_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    request_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="SUCCESS", server_default="SUCCESS"
    )
    safety_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    refusal_reason: Mapped[str | None] = mapped_column(String(64), nullable=True)
    confidence_score: Mapped[Decimal | None] = mapped_column(Numeric(8, 4), nullable=True)
    latency_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    trace_id: Mapped[str | None] = mapped_column(String(128), nullable=True)


class RagRetrievalLog(Base):
    __tablename__ = "rag_retrieval_log"
    __table_args__ = (Index("idx_rag_retrieval_request", "request_id", "rank_no"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    request_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    chunk_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    rank_no: Mapped[int] = mapped_column(Integer, nullable=False)
    retrieval_channel_code: Mapped[str | None] = mapped_column(String(32), nullable=True)
    retrieval_score: Mapped[Decimal | None] = mapped_column(Numeric(10, 6), nullable=True)
    doc_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    chunk_text_snapshot: Mapped[str | None] = mapped_column(Text, nullable=True)
    cited_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagFusionLog(Base):
    __tablename__ = "rag_fusion_log"
    __table_args__ = (
        Index("idx_rag_fusion_request", "request_id", "final_rank"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    request_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    candidate_id: Mapped[str] = mapped_column(String(128), nullable=False)
    candidate_type: Mapped[str] = mapped_column(String(32), nullable=False)
    origin_channels_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    fusion_score: Mapped[Decimal | None] = mapped_column(Numeric(10, 6), nullable=True)
    final_rank: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagRerankLog(Base):
    __tablename__ = "rag_rerank_log"
    __table_args__ = (
        Index("idx_rag_rerank_request", "request_id", "final_rank"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    request_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    candidate_id: Mapped[str] = mapped_column(String(128), nullable=False)
    candidate_type: Mapped[str] = mapped_column(String(32), nullable=False)
    origin_channel: Mapped[str | None] = mapped_column(String(32), nullable=True)
    pre_score: Mapped[Decimal | None] = mapped_column(Numeric(10, 6), nullable=True)
    rerank_score: Mapped[Decimal | None] = mapped_column(Numeric(10, 6), nullable=True)
    final_rank: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagGraphRetrievalLog(Base):
    __tablename__ = "rag_graph_retrieval_log"
    __table_args__ = (
        Index("idx_rag_graph_request", "request_id", "cypher_template_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    request_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    cypher_template_code: Mapped[str] = mapped_column(String(64), nullable=False)
    query_entity_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    result_path_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    score: Mapped[Decimal | None] = mapped_column(Numeric(10, 6), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class LlmCallLog(Base):
    __tablename__ = "llm_call_log"
    __table_args__ = (Index("idx_llm_call_request", "request_id", "model_name"),)

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
    response_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagEvalDataset(Base, AuditMixin, TimestampMixin):
    __tablename__ = "rag_eval_dataset"
    __table_args__ = (
        UniqueConstraint("dataset_code", name="uk_rag_eval_dataset_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    dataset_code: Mapped[str] = mapped_column(String(64), nullable=False)
    dataset_name: Mapped[str] = mapped_column(String(255), nullable=False)
    description_text: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    active_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="1", server_default="1")


class RagEvalQuestion(Base, AuditMixin, TimestampMixin):
    __tablename__ = "rag_eval_question"
    __table_args__ = (
        Index("idx_rag_eval_question_dataset", "dataset_id", "scene_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    dataset_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    question_no: Mapped[str] = mapped_column(String(64), nullable=False)
    scene_code: Mapped[str] = mapped_column(String(32), nullable=False)
    question_text: Mapped[str] = mapped_column(Text, nullable=False)
    case_context_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    expected_refusal_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    expected_risk_level: Mapped[str | None] = mapped_column(String(32), nullable=True)


class RagEvalExpectedCitation(Base):
    __tablename__ = "rag_eval_expected_citation"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    question_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    expected_doc_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    expected_chunk_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagEvalExpectedGraphPath(Base):
    __tablename__ = "rag_eval_expected_graph_path"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    question_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    path_signature: Mapped[str] = mapped_column(String(500), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagEvalRun(Base, AuditMixin):
    __tablename__ = "rag_eval_run"
    __table_args__ = (
        Index("idx_rag_eval_run_dataset", "dataset_id", "started_at"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    run_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    dataset_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    run_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="RUNNING", server_default="RUNNING"
    )
    metric_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    started_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class RagEvalResult(Base):
    __tablename__ = "rag_eval_result"
    __table_args__ = (
        Index("idx_rag_eval_result_run", "run_id", "question_id"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    run_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    question_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    request_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    answer_text: Mapped[str | None] = mapped_column(LONGTEXT, nullable=True)
    citation_hit_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    graph_hit_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    refusal_hit_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    hallucination_flag: Mapped[str] = mapped_column(CHAR(1), nullable=False, default="0", server_default="0")
    latency_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    metric_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
