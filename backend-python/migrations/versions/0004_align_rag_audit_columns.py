"""align missing audit columns for rag tables

Revision ID: 0004
Revises: 0003
Create Date: 2026-04-19
"""
from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "0004"
down_revision: Union[str, None] = "0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def _table_columns(table_name: str) -> set[str]:
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    return {col["name"] for col in inspector.get_columns(table_name)}


def _add_column_if_missing(table_name: str, column: sa.Column) -> None:
    if column.name not in _table_columns(table_name):
        op.add_column(table_name, column)


def _drop_column_if_exists(table_name: str, column_name: str) -> None:
    if column_name in _table_columns(table_name):
        op.drop_column(table_name, column_name)


def upgrade() -> None:
    _add_column_if_missing("kb_source_file", sa.Column("created_by", sa.BigInteger(), nullable=True))
    _add_column_if_missing("kb_source_file", sa.Column("updated_by", sa.BigInteger(), nullable=True))
    _add_column_if_missing("kb_review_record", sa.Column("updated_by", sa.BigInteger(), nullable=True))
    _add_column_if_missing("kb_publish_record", sa.Column("updated_by", sa.BigInteger(), nullable=True))
    _add_column_if_missing("kb_entity", sa.Column("updated_by", sa.BigInteger(), nullable=True))
    _add_column_if_missing("kb_relation", sa.Column("updated_by", sa.BigInteger(), nullable=True))
    _add_column_if_missing("rag_eval_run", sa.Column("updated_by", sa.BigInteger(), nullable=True))


def downgrade() -> None:
    _drop_column_if_exists("rag_eval_run", "updated_by")
    _drop_column_if_exists("kb_relation", "updated_by")
    _drop_column_if_exists("kb_entity", "updated_by")
    _drop_column_if_exists("kb_publish_record", "updated_by")
    _drop_column_if_exists("kb_review_record", "updated_by")
    _drop_column_if_exists("kb_source_file", "updated_by")
    _drop_column_if_exists("kb_source_file", "created_by")
