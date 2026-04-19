"""real rag graph upgrade

Revision ID: 0002
Revises: 0001
Create Date: 2026-04-19
"""
from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import mysql

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

TABLE_KWARGS = {
    "mysql_engine": "InnoDB",
    "mysql_charset": "utf8mb4",
    "mysql_collate": "utf8mb4_unicode_ci",
}


def upgrade() -> None:
    op.add_column("kb_knowledge_base", sa.Column("published_version_no", sa.String(length=64), nullable=True))
    op.alter_column("kb_knowledge_base", "vector_store_type_code", server_default="OPENSEARCH")

    op.add_column("kb_document", sa.Column("publish_status_code", sa.String(length=32), nullable=False, server_default="DRAFT"))
    op.add_column("kb_document", sa.Column("current_version_no", sa.String(length=64), nullable=True))
    op.add_column("kb_document", sa.Column("published_version_no", sa.String(length=64), nullable=True))
    op.add_column("kb_document", sa.Column("source_file_id", sa.BigInteger(), nullable=True))
    op.create_index("idx_kb_document_kb_publish", "kb_document", ["kb_id", "publish_status_code"])

    op.add_column("kb_document_chunk", sa.Column("version_no", sa.String(length=64), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("section_path", sa.String(length=500), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("chunk_type", sa.String(length=32), nullable=False, server_default="PARAGRAPH"))
    op.add_column("kb_document_chunk", sa.Column("doc_title_snapshot", sa.String(length=255), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("doc_source_code", sa.String(length=32), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("source_uri", sa.String(length=500), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("medical_tags", mysql.JSON(), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("graph_entity_refs", mysql.JSON(), nullable=True))
    op.add_column("kb_document_chunk", sa.Column("publish_status", sa.String(length=32), nullable=False, server_default="DRAFT"))
    op.create_index("idx_kb_chunk_doc_version_publish", "kb_document_chunk", ["doc_id", "version_no", "publish_status"])

    op.add_column("rag_request_log", sa.Column("intent_code", sa.String(length=64), nullable=True))
    op.add_column("rag_request_log", sa.Column("entity_link_json", mysql.JSON(), nullable=True))
    op.add_column("rag_request_log", sa.Column("refusal_reason", sa.String(length=64), nullable=True))
    op.add_column("rag_request_log", sa.Column("confidence_score", sa.Numeric(precision=8, scale=4), nullable=True))
    op.add_column("rag_request_log", sa.Column("trace_id", sa.String(length=128), nullable=True))
    op.add_column("rag_retrieval_log", sa.Column("retrieval_channel_code", sa.String(length=32), nullable=True))
    op.add_column("llm_call_log", sa.Column("response_json", mysql.JSON(), nullable=True))

    op.create_table(
        "kb_source_file",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("source_file_no", sa.String(length=64), nullable=False),
        sa.Column("kb_id", sa.BigInteger(), nullable=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=True),
        sa.Column("bucket_name", sa.String(length=128), nullable=False),
        sa.Column("object_key", sa.String(length=500), nullable=False),
        sa.Column("file_name", sa.String(length=255), nullable=False),
        sa.Column("mime_type", sa.String(length=128), nullable=True),
        sa.Column("file_size_bytes", sa.BigInteger(), nullable=True),
        sa.Column("md5", sa.String(length=64), nullable=True),
        sa.Column("source_type_code", sa.String(length=32), nullable=False, server_default="UPLOAD"),
        sa.Column("uploaded_by", sa.BigInteger(), nullable=True),
        sa.Column("uploaded_at", sa.DateTime(), nullable=False),
        sa.Column("parse_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("deleted_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("source_file_no", name="uk_kb_source_file_no"),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_source_file_doc", "kb_source_file", ["doc_id", "parse_status_code"])

    op.create_table(
        "kb_source_parse_result",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("source_file_id", sa.BigInteger(), nullable=False),
        sa.Column("parse_status_code", sa.String(length=32), nullable=False, server_default="SUCCESS"),
        sa.Column("normalized_markdown", mysql.LONGTEXT(), nullable=True),
        sa.Column("structured_json", mysql.JSON(), nullable=True),
        sa.Column("section_tree", mysql.JSON(), nullable=True),
        sa.Column("table_json", mysql.JSON(), nullable=True),
        sa.Column("metadata_json", mysql.JSON(), nullable=True),
        sa.Column("artifact_bucket_name", sa.String(length=128), nullable=True),
        sa.Column("artifact_object_key", sa.String(length=500), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_parse_result_source", "kb_source_parse_result", ["source_file_id", "parse_status_code"])

    op.create_table(
        "kb_ingest_job",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("ingest_job_no", sa.String(length=64), nullable=False),
        sa.Column("kb_id", sa.BigInteger(), nullable=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=True),
        sa.Column("source_file_id", sa.BigInteger(), nullable=True),
        sa.Column("ingest_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("current_step_code", sa.String(length=64), nullable=True),
        sa.Column("error_code", sa.String(length=64), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        sa.Column("started_at", sa.DateTime(), nullable=True),
        sa.Column("finished_at", sa.DateTime(), nullable=True),
        sa.Column("trace_id", sa.String(length=128), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("updated_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("ingest_job_no", name="uk_kb_ingest_job_no"),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_ingest_job_doc", "kb_ingest_job", ["doc_id", "ingest_status_code"])

    op.create_table(
        "kb_ingest_job_step",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("ingest_job_id", sa.BigInteger(), nullable=False),
        sa.Column("step_code", sa.String(length=64), nullable=False),
        sa.Column("step_order", sa.Integer(), nullable=False),
        sa.Column("step_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("message_text", sa.String(length=1000), nullable=True),
        sa.Column("payload_json", mysql.JSON(), nullable=True),
        sa.Column("started_at", sa.DateTime(), nullable=True),
        sa.Column("finished_at", sa.DateTime(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_ingest_job_step_job", "kb_ingest_job_step", ["ingest_job_id", "step_order"])

    op.create_table(
        "kb_document_version",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=False),
        sa.Column("version_no", sa.String(length=64), nullable=False),
        sa.Column("parent_version_no", sa.String(length=64), nullable=True),
        sa.Column("normalized_content", mysql.LONGTEXT(), nullable=True),
        sa.Column("structured_json", mysql.JSON(), nullable=True),
        sa.Column("section_tree", mysql.JSON(), nullable=True),
        sa.Column("table_json", mysql.JSON(), nullable=True),
        sa.Column("metadata_json", mysql.JSON(), nullable=True),
        sa.Column("change_summary", sa.String(length=1000), nullable=True),
        sa.Column("review_status_code", sa.String(length=32), nullable=False, server_default="DRAFT"),
        sa.Column("publish_status_code", sa.String(length=32), nullable=False, server_default="DRAFT"),
        sa.Column("source_file_id", sa.BigInteger(), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("updated_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("doc_id", "version_no", name="uk_kb_document_version_doc_no"),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_document_version_doc", "kb_document_version", ["doc_id", "version_no"])

    op.create_table(
        "kb_publish_record",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=False),
        sa.Column("version_no", sa.String(length=64), nullable=False),
        sa.Column("previous_version_no", sa.String(length=64), nullable=True),
        sa.Column("action_code", sa.String(length=32), nullable=False, server_default="PUBLISH"),
        sa.Column("comment_text", sa.String(length=1000), nullable=True),
        sa.Column("published_at", sa.DateTime(), nullable=False),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_publish_record_doc", "kb_publish_record", ["doc_id", "published_at"])

    op.create_table(
        "kb_review_record",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=False),
        sa.Column("version_no", sa.String(length=64), nullable=False),
        sa.Column("decision_code", sa.String(length=32), nullable=False),
        sa.Column("review_comment", sa.String(length=1000), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(), nullable=False),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_review_record_doc", "kb_review_record", ["doc_id", "reviewed_at"])

    op.create_table(
        "kb_entity",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("entity_code", sa.String(length=64), nullable=False),
        sa.Column("entity_name", sa.String(length=255), nullable=False),
        sa.Column("entity_type_code", sa.String(length=64), nullable=False),
        sa.Column("normalized_name", sa.String(length=255), nullable=False),
        sa.Column("source_doc_id", sa.BigInteger(), nullable=True),
        sa.Column("source_chunk_id", sa.BigInteger(), nullable=True),
        sa.Column("confidence_score", sa.Numeric(precision=8, scale=4), nullable=True),
        sa.Column("review_status_code", sa.String(length=32), nullable=False, server_default="REVIEW_PENDING"),
        sa.Column("enabled_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("entity_code", name="uk_kb_entity_code"),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_entity_doc", "kb_entity", ["source_doc_id", "review_status_code"])

    op.create_table(
        "kb_relation",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("relation_code", sa.String(length=64), nullable=False),
        sa.Column("source_entity_id", sa.BigInteger(), nullable=False),
        sa.Column("target_entity_id", sa.BigInteger(), nullable=False),
        sa.Column("relation_type_code", sa.String(length=64), nullable=False),
        sa.Column("evidence_doc_id", sa.BigInteger(), nullable=True),
        sa.Column("evidence_chunk_id", sa.BigInteger(), nullable=True),
        sa.Column("confidence_score", sa.Numeric(precision=8, scale=4), nullable=True),
        sa.Column("review_status_code", sa.String(length=32), nullable=False, server_default="REVIEW_PENDING"),
        sa.Column("enabled_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("relation_code", name="uk_kb_relation_code"),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_relation_source_target", "kb_relation", ["source_entity_id", "target_entity_id"])

    op.create_table(
        "kb_entity_alias",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("entity_id", sa.BigInteger(), nullable=False),
        sa.Column("alias_name", sa.String(length=255), nullable=False),
        sa.Column("normalized_alias_name", sa.String(length=255), nullable=False),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_entity_alias_entity", "kb_entity_alias", ["entity_id"])

    op.create_table(
        "kb_graph_sync_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=False),
        sa.Column("version_no", sa.String(length=64), nullable=False),
        sa.Column("sync_status_code", sa.String(length=32), nullable=False),
        sa.Column("entity_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("relation_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("trace_id", sa.String(length=128), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_graph_sync_doc", "kb_graph_sync_log", ["doc_id", "version_no"])

    op.create_table(
        "rag_fusion_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("request_id", sa.BigInteger(), nullable=False),
        sa.Column("candidate_id", sa.String(length=128), nullable=False),
        sa.Column("candidate_type", sa.String(length=32), nullable=False),
        sa.Column("origin_channels_json", mysql.JSON(), nullable=True),
        sa.Column("fusion_score", sa.Numeric(precision=10, scale=6), nullable=True),
        sa.Column("final_rank", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_fusion_request", "rag_fusion_log", ["request_id", "final_rank"])

    op.create_table(
        "rag_rerank_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("request_id", sa.BigInteger(), nullable=False),
        sa.Column("candidate_id", sa.String(length=128), nullable=False),
        sa.Column("candidate_type", sa.String(length=32), nullable=False),
        sa.Column("origin_channel", sa.String(length=32), nullable=True),
        sa.Column("pre_score", sa.Numeric(precision=10, scale=6), nullable=True),
        sa.Column("rerank_score", sa.Numeric(precision=10, scale=6), nullable=True),
        sa.Column("final_rank", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_rerank_request", "rag_rerank_log", ["request_id", "final_rank"])

    op.create_table(
        "rag_graph_retrieval_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("request_id", sa.BigInteger(), nullable=False),
        sa.Column("cypher_template_code", sa.String(length=64), nullable=False),
        sa.Column("query_entity_json", mysql.JSON(), nullable=True),
        sa.Column("result_path_json", mysql.JSON(), nullable=True),
        sa.Column("score", sa.Numeric(precision=10, scale=6), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_graph_request", "rag_graph_retrieval_log", ["request_id", "cypher_template_code"])

    op.create_table(
        "rag_eval_dataset",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("dataset_code", sa.String(length=64), nullable=False),
        sa.Column("dataset_name", sa.String(length=255), nullable=False),
        sa.Column("description_text", sa.String(length=1000), nullable=True),
        sa.Column("active_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("updated_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("dataset_code", name="uk_rag_eval_dataset_code"),
        **TABLE_KWARGS,
    )

    op.create_table(
        "rag_eval_question",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("dataset_id", sa.BigInteger(), nullable=False),
        sa.Column("question_no", sa.String(length=64), nullable=False),
        sa.Column("scene_code", sa.String(length=32), nullable=False),
        sa.Column("question_text", sa.Text(), nullable=False),
        sa.Column("case_context_json", mysql.JSON(), nullable=True),
        sa.Column("expected_refusal_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("expected_risk_level", sa.String(length=32), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("updated_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_eval_question_dataset", "rag_eval_question", ["dataset_id", "scene_code"])

    op.create_table(
        "rag_eval_expected_citation",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("question_id", sa.BigInteger(), nullable=False),
        sa.Column("expected_doc_no", sa.String(length=64), nullable=True),
        sa.Column("expected_chunk_id", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )

    op.create_table(
        "rag_eval_expected_graph_path",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("question_id", sa.BigInteger(), nullable=False),
        sa.Column("path_signature", sa.String(length=500), nullable=False),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )

    op.create_table(
        "rag_eval_run",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("run_no", sa.String(length=64), nullable=False),
        sa.Column("dataset_id", sa.BigInteger(), nullable=False),
        sa.Column("run_status_code", sa.String(length=32), nullable=False, server_default="RUNNING"),
        sa.Column("metric_json", mysql.JSON(), nullable=True),
        sa.Column("started_at", sa.DateTime(), nullable=False),
        sa.Column("finished_at", sa.DateTime(), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.UniqueConstraint("run_no", name="uk_rag_eval_run_no"),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_eval_run_dataset", "rag_eval_run", ["dataset_id", "started_at"])

    op.create_table(
        "rag_eval_result",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("run_id", sa.BigInteger(), nullable=False),
        sa.Column("question_id", sa.BigInteger(), nullable=False),
        sa.Column("request_no", sa.String(length=64), nullable=True),
        sa.Column("answer_text", mysql.LONGTEXT(), nullable=True),
        sa.Column("citation_hit_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("graph_hit_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("refusal_hit_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("hallucination_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("latency_ms", sa.Integer(), nullable=True),
        sa.Column("metric_json", mysql.JSON(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_eval_result_run", "rag_eval_result", ["run_id", "question_id"])


def downgrade() -> None:
    for table_name in (
        "rag_eval_result",
        "rag_eval_run",
        "rag_eval_expected_graph_path",
        "rag_eval_expected_citation",
        "rag_eval_question",
        "rag_eval_dataset",
        "rag_graph_retrieval_log",
        "rag_rerank_log",
        "rag_fusion_log",
        "kb_graph_sync_log",
        "kb_entity_alias",
        "kb_relation",
        "kb_entity",
        "kb_review_record",
        "kb_publish_record",
        "kb_document_version",
        "kb_ingest_job_step",
        "kb_ingest_job",
        "kb_source_parse_result",
        "kb_source_file",
    ):
        op.drop_table(table_name)
