from __future__ import annotations

from datetime import datetime

from sqlalchemy import CHAR, BigInteger, DateTime, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class SoftDeleteMixin:
    deleted_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="0", server_default="0"
    )


class StatusMixin:
    status: Mapped[str] = mapped_column(
        String(32), nullable=False, default="ACTIVE", server_default="ACTIVE"
    )


class RemarkMixin:
    remark: Mapped[str | None] = mapped_column(String(500), nullable=True)


class AuditMixin:
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_by: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    updated_by: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
