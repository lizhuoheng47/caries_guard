from __future__ import annotations

from datetime import datetime

from sqlalchemy import CHAR, JSON, BigInteger, DateTime, Index, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import AuditMixin, Base, RemarkMixin, SoftDeleteMixin, StatusMixin, TimestampMixin


class ModelVersion(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "mdl_model_version"
    __table_args__ = (
        UniqueConstraint("model_code", "version_no", name="uk_mdl_model_version_code_version"),
        Index("idx_mdl_model_version_type_status", "model_type_code", "status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    model_code: Mapped[str] = mapped_column(String(64), nullable=False)
    model_name: Mapped[str] = mapped_column(String(128), nullable=False)
    model_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="SEGMENTATION", server_default="SEGMENTATION"
    )
    version_no: Mapped[str] = mapped_column(String(64), nullable=False)
    artifact_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    dataset_version: Mapped[str | None] = mapped_column(String(64), nullable=True)
    metrics_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="CANDIDATE", server_default="CANDIDATE"
    )
    active_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="0", server_default="0"
    )
    published_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class ModelEvalRecord(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "mdl_model_eval_record"
    __table_args__ = (
        Index(
            "idx_mdl_model_eval_model_dataset",
            "model_version_id",
            "dataset_snapshot_id",
        ),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    model_version_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    dataset_snapshot_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    eval_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="OFFLINE", server_default="OFFLINE"
    )
    metric_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    error_case_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    evidence_attachment_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    evaluated_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    evaluator_name: Mapped[str | None] = mapped_column(String(128), nullable=True)


class ModelApprovalRecord(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "mdl_model_approval_record"
    __table_args__ = (
        Index("idx_mdl_model_approval_model_decision", "model_version_id", "decision_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    model_version_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    decision_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    approver_name: Mapped[str | None] = mapped_column(String(128), nullable=True)
    decision_note: Mapped[str | None] = mapped_column(String(1000), nullable=True)
    approved_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class DatasetSnapshot(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "trn_dataset_snapshot"
    __table_args__ = (
        Index("idx_trn_dataset_snapshot_type", "snapshot_type_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    dataset_version: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    snapshot_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="TRAIN", server_default="TRAIN"
    )
    source_summary: Mapped[str | None] = mapped_column(String(500), nullable=True)
    sample_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    metadata_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    dataset_card_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    released_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class DatasetSample(Base):
    """Lightweight table: no status/deleted_flag/remark/updated_*/created_by."""

    __tablename__ = "trn_dataset_sample"
    __table_args__ = (
        Index("idx_trn_dataset_sample_snapshot_ref", "snapshot_id", "sample_ref_no"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    snapshot_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    sample_ref_no: Mapped[str] = mapped_column(String(128), nullable=False)
    patient_uuid: Mapped[str | None] = mapped_column(String(128), nullable=True)
    image_ref_no: Mapped[str | None] = mapped_column(String(128), nullable=True)
    source_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="CORRECTION", server_default="CORRECTION"
    )
    split_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="TRAIN", server_default="TRAIN"
    )
    label_version: Mapped[str | None] = mapped_column(String(64), nullable=True)
    org_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class TrainingRun(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "trn_training_run"
    __table_args__ = (
        Index(
            "idx_trn_training_run_dataset_status",
            "dataset_snapshot_id",
            "run_status_code",
        ),
        Index("idx_trn_training_run_model", "target_model_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    training_run_no: Mapped[str] = mapped_column(String(64), nullable=False, unique=True)
    dataset_snapshot_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    target_model_code: Mapped[str] = mapped_column(String(64), nullable=False)
    base_model_version: Mapped[str | None] = mapped_column(String(64), nullable=True)
    run_type_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="TRAIN", server_default="TRAIN"
    )
    parameters_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    output_artifact_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    metric_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    run_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    started_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    error_message: Mapped[str | None] = mapped_column(String(1000), nullable=True)


class AnnotationRecord(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "ann_annotation_record"
    __table_args__ = (
        Index("idx_ann_annotation_sample_version", "sample_ref_no", "annotation_version"),
        Index("idx_ann_annotation_qc", "qc_status_code"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    sample_ref_no: Mapped[str] = mapped_column(String(128), nullable=False)
    patient_uuid: Mapped[str | None] = mapped_column(String(128), nullable=True)
    annotation_version: Mapped[str] = mapped_column(String(64), nullable=False)
    annotation_result_json: Mapped[dict | list | None] = mapped_column(JSON, nullable=True)
    annotation_object_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    annotator_l1: Mapped[str | None] = mapped_column(String(128), nullable=True)
    reviewer_l2: Mapped[str | None] = mapped_column(String(128), nullable=True)
    qc_status_code: Mapped[str] = mapped_column(
        String(32), nullable=False, default="PENDING", server_default="PENDING"
    )
    difficulty_code: Mapped[str | None] = mapped_column(String(32), nullable=True)


class GoldSetItem(Base, AuditMixin, StatusMixin, SoftDeleteMixin, RemarkMixin, TimestampMixin):
    __tablename__ = "ann_gold_set_item"
    __table_args__ = (
        Index("idx_ann_gold_set_active", "active_flag"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    sample_ref_no: Mapped[str] = mapped_column(String(128), nullable=False, unique=True)
    annotation_record_id: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    difficulty_code: Mapped[str | None] = mapped_column(String(32), nullable=True)
    active_flag: Mapped[str] = mapped_column(
        CHAR(1), nullable=False, default="1", server_default="1"
    )
