"""add updated_by column to kb_graph_sync_log

Revision ID: 0003
Revises: 0002
Create Date: 2026-04-19
"""
from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    columns = {col["name"] for col in inspector.get_columns("kb_graph_sync_log")}
    if "updated_by" not in columns:
        op.add_column("kb_graph_sync_log", sa.Column("updated_by", sa.BigInteger(), nullable=True))


def downgrade() -> None:
    bind = op.get_bind()
    inspector = sa.inspect(bind)
    columns = {col["name"] for col in inspector.get_columns("kb_graph_sync_log")}
    if "updated_by" in columns:
        op.drop_column("kb_graph_sync_log", "updated_by")
