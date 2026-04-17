"""baseline caries_ai schema (20 tables, 22 indexes)

Revision ID: 0001
Revises:
Create Date: 2026-04-17

Baseline migration capturing the full caries_ai schema as documented in
backend-python/docs/13_caries_ai_baseline_schema.md. Hand-written per Phase 4
doc section 6.3 (do not autogenerate baselines).

Any environment that already has the tables should be stamped rather than
upgraded: `alembic stamp 0001`. Fresh databases should run `alembic upgrade
head`. The Docker entrypoint script makes this decision automatically based on
the presence of the `alembic_version` table.
"""
from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import mysql

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


TABLE_KWARGS = {
    "mysql_engine": "InnoDB",
    "mysql_charset": "utf8mb4",
    "mysql_collate": "utf8mb4_unicode_ci",
}


def _full_audit_columns() -> list[sa.Column]:
    return [
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="ACTIVE"),
        sa.Column("deleted_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("remark", sa.String(length=500), nullable=True),
        sa.Column("created_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.Column("updated_by", sa.BigInteger(), nullable=True),
        sa.Column("updated_at", sa.DateTime(), nullable=False),
    ]


def upgrade() -> None:
    # ----- AI runtime domain --------------------------------------------
    op.create_table(
        "ai_infer_job",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("job_no", sa.String(length=64), nullable=False, unique=True),
        sa.Column("java_task_no", sa.String(length=64), nullable=False),
        sa.Column("case_no", sa.String(length=64), nullable=True),
        sa.Column("patient_uuid", sa.String(length=128), nullable=True),
        sa.Column("infer_type_code", sa.String(length=32), nullable=False, server_default="ANALYZE"),
        sa.Column("model_version", sa.String(length=64), nullable=False),
        sa.Column("status_code", sa.String(length=32), nullable=False, server_default="QUEUEING"),
        sa.Column("request_json", mysql.JSON(), nullable=True),
        sa.Column("result_json", mysql.JSON(), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        sa.Column("started_at", sa.DateTime(), nullable=True),
        sa.Column("finished_at", sa.DateTime(), nullable=True),
        sa.Column("callback_required_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        sa.Column("callback_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_ai_infer_job_java_task_no", "ai_infer_job", ["java_task_no"])
    op.create_index("idx_ai_infer_job_case_status", "ai_infer_job", ["case_no", "status_code"])

    op.create_table(
        "ai_infer_job_image",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("job_id", sa.BigInteger(), nullable=False),
        sa.Column("image_id", sa.BigInteger(), nullable=True),
        sa.Column("attachment_id", sa.BigInteger(), nullable=True),
        sa.Column("image_type_code", sa.String(length=32), nullable=True),
        sa.Column("bucket_name", sa.String(length=128), nullable=True),
        sa.Column("object_key", sa.String(length=500), nullable=True),
        sa.Column("access_url", sa.Text(), nullable=True),
        sa.Column("url_expire_at", sa.DateTime(), nullable=True),
        sa.Column("download_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("local_cache_path", sa.String(length=500), nullable=True),
        sa.Column("quality_status_code", sa.String(length=32), nullable=True),
        sa.Column("grading_label", sa.String(length=32), nullable=True),
        sa.Column("uncertainty_score", sa.Numeric(precision=8, scale=4), nullable=True),
        sa.Column("result_json", mysql.JSON(), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_ai_infer_job_image_job", "ai_infer_job_image", ["job_id", "image_id"])

    op.create_table(
        "ai_infer_artifact",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("job_id", sa.BigInteger(), nullable=False),
        sa.Column("related_image_id", sa.BigInteger(), nullable=True),
        sa.Column("artifact_type_code", sa.String(length=32), nullable=False),
        sa.Column("bucket_name", sa.String(length=128), nullable=False),
        sa.Column("object_key", sa.String(length=500), nullable=False),
        sa.Column("content_type", sa.String(length=128), nullable=True),
        sa.Column("file_size_bytes", sa.BigInteger(), nullable=True),
        sa.Column("md5", sa.String(length=64), nullable=True),
        sa.Column("model_version", sa.String(length=64), nullable=True),
        sa.Column("attachment_id", sa.BigInteger(), nullable=True),
        sa.Column("ext_json", mysql.JSON(), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_ai_infer_artifact_job", "ai_infer_artifact", ["job_id", "artifact_type_code"])

    op.create_table(
        "ai_callback_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("job_id", sa.BigInteger(), nullable=False),
        sa.Column("callback_url", sa.String(length=500), nullable=False),
        sa.Column("request_json", mysql.JSON(), nullable=True),
        sa.Column("response_code", sa.Integer(), nullable=True),
        sa.Column("response_body", sa.Text(), nullable=True),
        sa.Column("callback_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("retry_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("next_retry_at", sa.DateTime(), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        sa.Column("trace_id", sa.String(length=128), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_ai_callback_log_job", "ai_callback_log", ["job_id", "callback_status_code"])

    # ----- RAG / knowledge-base domain ----------------------------------
    op.create_table(
        "kb_knowledge_base",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("kb_code", sa.String(length=64), nullable=False, unique=True),
        sa.Column("kb_name", sa.String(length=128), nullable=False),
        sa.Column("kb_type_code", sa.String(length=32), nullable=False, server_default="PATIENT_GUIDE"),
        sa.Column("knowledge_version", sa.String(length=64), nullable=False, server_default="v1.0"),
        sa.Column("embedding_model", sa.String(length=64), nullable=True),
        sa.Column("vector_store_type_code", sa.String(length=32), nullable=False, server_default="LOCAL_JSON"),
        sa.Column("vector_store_path", sa.String(length=500), nullable=True),
        sa.Column("enabled_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        sa.Column("status_code", sa.String(length=32), nullable=False, server_default="ACTIVE"),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )

    op.create_table(
        "kb_document",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("kb_id", sa.BigInteger(), nullable=False),
        sa.Column("doc_no", sa.String(length=64), nullable=False, unique=True),
        sa.Column("doc_title", sa.String(length=255), nullable=False),
        sa.Column("doc_source_code", sa.String(length=32), nullable=False, server_default="INTERNAL"),
        sa.Column("source_uri", sa.String(length=500), nullable=True),
        sa.Column("doc_version", sa.String(length=64), nullable=False, server_default="v1.0"),
        sa.Column("content_text", mysql.LONGTEXT(), nullable=True),
        sa.Column("content_attachment_key", sa.String(length=500), nullable=True),
        sa.Column("review_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("reviewer_id", sa.BigInteger(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(), nullable=True),
        sa.Column("enabled_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_document_kb_review", "kb_document", ["kb_id", "review_status_code"])

    op.create_table(
        "kb_document_chunk",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("kb_id", sa.BigInteger(), nullable=False),
        sa.Column("doc_id", sa.BigInteger(), nullable=False),
        sa.Column("chunk_no", sa.Integer(), nullable=False),
        sa.Column("chunk_text", sa.Text(), nullable=False),
        sa.Column("token_count", sa.Integer(), nullable=True),
        sa.Column("embedding_model", sa.String(length=64), nullable=True),
        sa.Column("vector_store_path", sa.String(length=500), nullable=True),
        sa.Column("vector_id", sa.String(length=128), nullable=True),
        sa.Column("enabled_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="ACTIVE"),
        sa.Column("deleted_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_chunk_kb_doc", "kb_document_chunk", ["kb_id", "doc_id"])

    op.create_table(
        "kb_rebuild_job",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("rebuild_job_no", sa.String(length=64), nullable=False, unique=True),
        sa.Column("kb_id", sa.BigInteger(), nullable=False),
        sa.Column("knowledge_version", sa.String(length=64), nullable=False),
        sa.Column("rebuild_status_code", sa.String(length=32), nullable=False, server_default="RUNNING"),
        sa.Column("chunk_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("vector_store_path", sa.String(length=500), nullable=True),
        sa.Column("started_at", sa.DateTime(), nullable=True),
        sa.Column("finished_at", sa.DateTime(), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_kb_rebuild_job_kb", "kb_rebuild_job", ["kb_id", "rebuild_status_code"])

    op.create_table(
        "rag_session",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("session_no", sa.String(length=64), nullable=False, unique=True),
        sa.Column("session_type_code", sa.String(length=32), nullable=False),
        sa.Column("related_biz_no", sa.String(length=64), nullable=True),
        sa.Column("patient_uuid", sa.String(length=128), nullable=True),
        sa.Column("java_user_id", sa.BigInteger(), nullable=True),
        sa.Column("knowledge_version", sa.String(length=64), nullable=True),
        sa.Column("model_name", sa.String(length=64), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_session_type", "rag_session", ["session_type_code"])

    op.create_table(
        "rag_request_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("session_id", sa.BigInteger(), nullable=False),
        sa.Column("request_no", sa.String(length=64), nullable=False, unique=True),
        sa.Column("request_type_code", sa.String(length=32), nullable=False),
        sa.Column("user_query", sa.Text(), nullable=False),
        sa.Column("rewritten_query", sa.Text(), nullable=True),
        sa.Column("top_k", sa.Integer(), nullable=False, server_default="5"),
        sa.Column("answer_text", mysql.LONGTEXT(), nullable=True),
        sa.Column("request_status_code", sa.String(length=32), nullable=False, server_default="SUCCESS"),
        sa.Column("safety_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("latency_ms", sa.Integer(), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_request_session", "rag_request_log", ["session_id", "request_type_code"])

    op.create_table(
        "rag_retrieval_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("request_id", sa.BigInteger(), nullable=False),
        sa.Column("chunk_id", sa.BigInteger(), nullable=False),
        sa.Column("rank_no", sa.Integer(), nullable=False),
        sa.Column("retrieval_score", sa.Numeric(precision=10, scale=6), nullable=True),
        sa.Column("doc_id", sa.BigInteger(), nullable=False),
        sa.Column("chunk_text_snapshot", sa.Text(), nullable=True),
        sa.Column("cited_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_rag_retrieval_request", "rag_retrieval_log", ["request_id", "rank_no"])

    op.create_table(
        "llm_call_log",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("request_id", sa.BigInteger(), nullable=False),
        sa.Column("model_name", sa.String(length=128), nullable=False),
        sa.Column("provider_code", sa.String(length=64), nullable=True),
        sa.Column("prompt_text", mysql.LONGTEXT(), nullable=True),
        sa.Column("completion_text", mysql.LONGTEXT(), nullable=True),
        sa.Column("prompt_tokens", sa.Integer(), nullable=True),
        sa.Column("completion_tokens", sa.Integer(), nullable=True),
        sa.Column("total_tokens", sa.Integer(), nullable=True),
        sa.Column("latency_ms", sa.Integer(), nullable=True),
        sa.Column("call_status_code", sa.String(length=32), nullable=False, server_default="SUCCESS"),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index("idx_llm_call_request", "llm_call_log", ["request_id", "model_name"])

    # ----- governance domain --------------------------------------------
    op.create_table(
        "mdl_model_version",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("model_code", sa.String(length=64), nullable=False),
        sa.Column("model_name", sa.String(length=128), nullable=False),
        sa.Column("model_type_code", sa.String(length=32), nullable=False, server_default="SEGMENTATION"),
        sa.Column("version_no", sa.String(length=64), nullable=False),
        sa.Column("artifact_path", sa.String(length=500), nullable=True),
        sa.Column("dataset_version", sa.String(length=64), nullable=True),
        sa.Column("metrics_json", mysql.JSON(), nullable=True),
        sa.Column("status_code", sa.String(length=32), nullable=False, server_default="CANDIDATE"),
        sa.Column("active_flag", sa.CHAR(length=1), nullable=False, server_default="0"),
        sa.Column("published_at", sa.DateTime(), nullable=True),
        *_full_audit_columns(),
        sa.UniqueConstraint("model_code", "version_no", name="uk_mdl_model_version_code_version"),
        **TABLE_KWARGS,
    )
    op.create_index(
        "idx_mdl_model_version_type_status",
        "mdl_model_version",
        ["model_type_code", "status_code"],
    )

    op.create_table(
        "mdl_model_eval_record",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("model_version_id", sa.BigInteger(), nullable=False),
        sa.Column("dataset_snapshot_id", sa.BigInteger(), nullable=True),
        sa.Column("eval_type_code", sa.String(length=32), nullable=False, server_default="OFFLINE"),
        sa.Column("metric_json", mysql.JSON(), nullable=True),
        sa.Column("error_case_json", mysql.JSON(), nullable=True),
        sa.Column("evidence_attachment_key", sa.String(length=500), nullable=True),
        sa.Column("evaluated_at", sa.DateTime(), nullable=False),
        sa.Column("evaluator_name", sa.String(length=128), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index(
        "idx_mdl_model_eval_model_dataset",
        "mdl_model_eval_record",
        ["model_version_id", "dataset_snapshot_id"],
    )

    op.create_table(
        "mdl_model_approval_record",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("model_version_id", sa.BigInteger(), nullable=False),
        sa.Column("decision_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("approver_name", sa.String(length=128), nullable=True),
        sa.Column("decision_note", sa.String(length=1000), nullable=True),
        sa.Column("approved_at", sa.DateTime(), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index(
        "idx_mdl_model_approval_model_decision",
        "mdl_model_approval_record",
        ["model_version_id", "decision_code"],
    )

    op.create_table(
        "trn_dataset_snapshot",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("dataset_version", sa.String(length=64), nullable=False, unique=True),
        sa.Column("snapshot_type_code", sa.String(length=32), nullable=False, server_default="TRAIN"),
        sa.Column("source_summary", sa.String(length=500), nullable=True),
        sa.Column("sample_count", sa.Integer(), nullable=True),
        sa.Column("metadata_json", mysql.JSON(), nullable=True),
        sa.Column("dataset_card_path", sa.String(length=500), nullable=True),
        sa.Column("released_at", sa.DateTime(), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_trn_dataset_snapshot_type", "trn_dataset_snapshot", ["snapshot_type_code"])

    op.create_table(
        "trn_dataset_sample",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("snapshot_id", sa.BigInteger(), nullable=False),
        sa.Column("sample_ref_no", sa.String(length=128), nullable=False),
        sa.Column("patient_uuid", sa.String(length=128), nullable=True),
        sa.Column("image_ref_no", sa.String(length=128), nullable=True),
        sa.Column("source_type_code", sa.String(length=32), nullable=False, server_default="CORRECTION"),
        sa.Column("split_type_code", sa.String(length=32), nullable=False, server_default="TRAIN"),
        sa.Column("label_version", sa.String(length=64), nullable=True),
        sa.Column("org_id", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        **TABLE_KWARGS,
    )
    op.create_index(
        "idx_trn_dataset_sample_snapshot_ref",
        "trn_dataset_sample",
        ["snapshot_id", "sample_ref_no"],
    )

    op.create_table(
        "trn_training_run",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("training_run_no", sa.String(length=64), nullable=False, unique=True),
        sa.Column("dataset_snapshot_id", sa.BigInteger(), nullable=True),
        sa.Column("target_model_code", sa.String(length=64), nullable=False),
        sa.Column("base_model_version", sa.String(length=64), nullable=True),
        sa.Column("run_type_code", sa.String(length=32), nullable=False, server_default="TRAIN"),
        sa.Column("parameters_json", mysql.JSON(), nullable=True),
        sa.Column("output_artifact_path", sa.String(length=500), nullable=True),
        sa.Column("metric_json", mysql.JSON(), nullable=True),
        sa.Column("run_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("started_at", sa.DateTime(), nullable=True),
        sa.Column("finished_at", sa.DateTime(), nullable=True),
        sa.Column("error_message", sa.String(length=1000), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index(
        "idx_trn_training_run_dataset_status",
        "trn_training_run",
        ["dataset_snapshot_id", "run_status_code"],
    )
    op.create_index("idx_trn_training_run_model", "trn_training_run", ["target_model_code"])

    op.create_table(
        "ann_annotation_record",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("sample_ref_no", sa.String(length=128), nullable=False),
        sa.Column("patient_uuid", sa.String(length=128), nullable=True),
        sa.Column("annotation_version", sa.String(length=64), nullable=False),
        sa.Column("annotation_result_json", mysql.JSON(), nullable=True),
        sa.Column("annotation_object_key", sa.String(length=500), nullable=True),
        sa.Column("annotator_l1", sa.String(length=128), nullable=True),
        sa.Column("reviewer_l2", sa.String(length=128), nullable=True),
        sa.Column("qc_status_code", sa.String(length=32), nullable=False, server_default="PENDING"),
        sa.Column("difficulty_code", sa.String(length=32), nullable=True),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index(
        "idx_ann_annotation_sample_version",
        "ann_annotation_record",
        ["sample_ref_no", "annotation_version"],
    )
    op.create_index("idx_ann_annotation_qc", "ann_annotation_record", ["qc_status_code"])

    op.create_table(
        "ann_gold_set_item",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("sample_ref_no", sa.String(length=128), nullable=False, unique=True),
        sa.Column("annotation_record_id", sa.BigInteger(), nullable=True),
        sa.Column("difficulty_code", sa.String(length=32), nullable=True),
        sa.Column("active_flag", sa.CHAR(length=1), nullable=False, server_default="1"),
        *_full_audit_columns(),
        **TABLE_KWARGS,
    )
    op.create_index("idx_ann_gold_set_active", "ann_gold_set_item", ["active_flag"])


def downgrade() -> None:
    for name in (
        "ann_gold_set_item",
        "ann_annotation_record",
        "trn_training_run",
        "trn_dataset_sample",
        "trn_dataset_snapshot",
        "mdl_model_approval_record",
        "mdl_model_eval_record",
        "mdl_model_version",
        "llm_call_log",
        "rag_retrieval_log",
        "rag_request_log",
        "rag_session",
        "kb_rebuild_job",
        "kb_document_chunk",
        "kb_document",
        "kb_knowledge_base",
        "ai_callback_log",
        "ai_infer_artifact",
        "ai_infer_job_image",
        "ai_infer_job",
    ):
        op.drop_table(name)
