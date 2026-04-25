"""add trace_id to ai_infer_job

Revision ID: 0005
Revises: 0004
Create Date: 2026-04-21
"""
from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "0005"
down_revision: Union[str, None] = "0004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def _table_columns(table_name: str) -> set[str]:
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    return {col["name"] for col in inspector.get_columns(table_name)}


def _table_indexes(table_name: str) -> set[str]:
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    return {index["name"] for index in inspector.get_indexes(table_name)}


def upgrade() -> None:
    if "trace_id" not in _table_columns("ai_infer_job"):
        op.add_column("ai_infer_job", sa.Column("trace_id", sa.String(length=128), nullable=True))
    if "idx_ai_infer_job_trace_id" not in _table_indexes("ai_infer_job"):
        op.create_index("idx_ai_infer_job_trace_id", "ai_infer_job", ["trace_id"])


def downgrade() -> None:
    if "idx_ai_infer_job_trace_id" in _table_indexes("ai_infer_job"):
        op.drop_index("idx_ai_infer_job_trace_id", table_name="ai_infer_job")
    if "trace_id" in _table_columns("ai_infer_job"):
        op.drop_column("ai_infer_job", "trace_id")
