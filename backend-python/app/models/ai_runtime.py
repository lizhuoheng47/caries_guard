from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from sqlalchemy import CHAR, JSON, BigInteger, DateTime, Index, Integer, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import AuditMixin, Base, RemarkMixin, SoftDeleteMixin, StatusMixin, TimestampMixin


class AiInferJob(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "ai_infer_job"
    __table_args__ = (
        Index("idx_ai_infer_job_java_task_no", "java_task_no"),
        Index("idx_ai_infer_job_case_status", "case_no", "status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    job_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    java_task_no: Mapped[str] = mapped_column(String(64), nullable=False)
    case_no: Mapped[str | None] = mapped_column(String(64), nullable=True)
    patient_uuid: Mapped[str | None] = mapped_column(String(128), nullable=True)
    infer_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="ANALYZE", server_default="ANALYZE"
    )
    model_version: Mapped[str] = mapped_column(String(64), nullable=False)
    status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="QUEUEING", server_default="QUEUEING"
    )
    request_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    result_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    callback_required_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="1", server_default="1"
    )
    callback_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )


class AiInferJobImage(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "ai_infer_job_image"
    __table_args__ = (
        Index("idx_ai_infer_job_image_job", "job_id", "image_id"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    job_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    image_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    attachment_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    image_type_code: Mapped[str | None] = mapped_column(String(32), nullable=True)
    bucket_name: Mapped[str | None] = mapped_column(String(128), nullable=True)
    object_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    access_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    url_expire_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    download_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    local_cache_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    quality_status_code: Mapped[str | None] = mapped_column(String(32), nullable=True)
    grading_label: Mapped[str | None] = mapped_column(String(32), nullable=True)
    uncertainty_score: Mapped[Decimal | None] = mapped_column(Numeric(8, 4), nullable=True)
    result_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)


class AiInferArtifact(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "ai_infer_artifact"
    __table_args__ = (
        Index("idx_ai_infer_artifact_job", "job_id", "artifact_type_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    job_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    related_image_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    artifact_type_code: Mapped[str] = mapped_column(String(32), nullable=False)
    bucket_name: Mapped[str] = mapped_column(String(128), nullable=False)
    object_key: Mapped[str] = mapped_column(String(500), nullable=False)
    content_type: Mapped[str | None] = mapped_column(String(128), nullable=True)
    file_size_bytes: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    md5: Mapped[str | None] = mapped_column(String(64), nullable=True)
    model_version: Mapped[str | None] = mapped_column(String(64), nullable=True)
    attachment_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    ext_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)


class AiCallbackLog(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "ai_callback_log"
    __table_args__ = (
        Index("idx_ai_callback_log_job", "job_id", "callback_status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    job_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    callback_url: Mapped[str] = mapped_column(String(500), nullable=False)
    request_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    response_code: Mapped[int | None] = mapped_column(Integer, nullable=True)
    response_body: Mapped[str | None] = mapped_column(Text, nullable=True)
    callback_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    retry_count: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0, server_default="0"
    )
    next_retry_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    trace_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
