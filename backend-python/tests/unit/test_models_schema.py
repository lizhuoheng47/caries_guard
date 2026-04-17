"""Reflection parity test: Base.metadata must match the live DB schema.

This is the correctness gate between Step 4 (ORM) and Step 6 (Alembic baseline).
It only runs when CG_DB_TEST_ENABLED=true so unit CI stays fast.
"""
from __future__ import annotations

import os

import pytest
from sqlalchemy import inspect

import app.models  # noqa: F401  registers all models on Base.metadata
from app.core.db import get_engine
from app.models.base import Base


pytestmark = pytest.mark.skipif(
    os.getenv("CG_DB_TEST_ENABLED", "false").lower() not in {"1", "true", "yes"},
    reason="requires live MySQL; set CG_DB_TEST_ENABLED=true to enable",
)


EXPECTED_TABLES = {
    "ai_infer_job",
    "ai_infer_job_image",
    "ai_infer_artifact",
    "ai_callback_log",
    "kb_knowledge_base",
    "kb_document",
    "kb_document_chunk",
    "kb_rebuild_job",
    "rag_session",
    "rag_request_log",
    "rag_retrieval_log",
    "llm_call_log",
    "mdl_model_version",
    "mdl_model_eval_record",
    "mdl_model_approval_record",
    "trn_dataset_snapshot",
    "trn_dataset_sample",
    "trn_training_run",
    "ann_annotation_record",
    "ann_gold_set_item",
}


def test_orm_registers_all_expected_tables():
    orm_tables = set(Base.metadata.tables.keys())
    missing = EXPECTED_TABLES - orm_tables
    extra = orm_tables - EXPECTED_TABLES
    assert not missing, f"ORM missing tables: {missing}"
    assert not extra, f"ORM has unexpected tables: {extra}"


def test_orm_columns_match_live_db():
    engine = get_engine()
    inspector = inspect(engine)
    live_tables = set(inspector.get_table_names())

    diffs: list[str] = []
    for table_name in EXPECTED_TABLES:
        if table_name not in live_tables:
            diffs.append(f"{table_name}: missing in DB")
            continue
        orm_cols = {c.name for c in Base.metadata.tables[table_name].columns}
        db_cols = {c["name"] for c in inspector.get_columns(table_name)}
        only_orm = orm_cols - db_cols
        only_db = db_cols - orm_cols
        if only_orm or only_db:
            diffs.append(
                f"{table_name}: only_in_orm={sorted(only_orm)} only_in_db={sorted(only_db)}"
            )
    assert not diffs, "\n".join(diffs)
