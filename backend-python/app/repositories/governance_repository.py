from __future__ import annotations

import uuid
from typing import Any

from sqlalchemy import select

from app.core.db import session_scope
from app.core.time_utils import local_naive_now
from app.models.governance import (
    AnnotationRecord,
    DatasetSample,
    DatasetSnapshot,
    GoldSetItem,
    ModelApprovalRecord,
    ModelEvalRecord,
    ModelVersion,
    TrainingRun,
)


def _row_to_dict(obj: Any) -> dict[str, Any]:
    if obj is None:
        return {}
    return {column.name: getattr(obj, column.name) for column in obj.__table__.columns}


class GovernanceRepository:
    """Model-governance domain repository."""

    def register_model_version(
        self,
        model_code: str,
        model_name: str,
        version_no: str,
        *,
        model_type_code: str = "SEGMENTATION",
        artifact_path: str | None = None,
        dataset_version: str | None = None,
        metrics_json: dict | list | None = None,
        status_code: str = "CANDIDATE",
        active_flag: str = "0",
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = ModelVersion(
                model_code=model_code,
                model_name=model_name,
                model_type_code=model_type_code,
                version_no=version_no,
                artifact_path=artifact_path,
                dataset_version=dataset_version,
                metrics_json=metrics_json,
                status_code=status_code,
                active_flag=active_flag,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def ensure_model_version(
        self,
        model_code: str,
        model_name: str,
        version_no: str,
        *,
        model_type_code: str = "SEGMENTATION",
        artifact_path: str | None = None,
        dataset_version: str | None = None,
        metrics_json: dict | list | None = None,
        status_code: str = "APPROVED",
        active_flag: str = "1",
        org_id: int | None = None,
    ) -> dict[str, Any]:
        """Create or refresh a model-version row without duplicating it."""
        now = local_naive_now()
        with session_scope() as session:
            row = session.execute(
                select(ModelVersion).where(
                    ModelVersion.model_code == model_code,
                    ModelVersion.version_no == version_no,
                    ModelVersion.deleted_flag == "0",
                )
            ).scalar_one_or_none()
            if row is None:
                row = ModelVersion(
                    model_code=model_code,
                    model_name=model_name,
                    model_type_code=model_type_code,
                    version_no=version_no,
                    artifact_path=artifact_path,
                    dataset_version=dataset_version,
                    metrics_json=metrics_json,
                    status_code=status_code,
                    active_flag=active_flag,
                    org_id=org_id,
                    created_at=now,
                    updated_at=now,
                )
                session.add(row)
            else:
                row.model_name = model_name
                row.model_type_code = model_type_code
                row.artifact_path = artifact_path
                row.dataset_version = dataset_version
                row.metrics_json = metrics_json
                row.status_code = status_code
                row.active_flag = active_flag
                row.org_id = org_id
                row.updated_at = now
            session.flush()
            return _row_to_dict(row)

    def get_active_model_version(self, model_code: str) -> dict[str, Any] | None:
        with session_scope() as session:
            stmt = (
                select(ModelVersion)
                .where(
                    ModelVersion.model_code == model_code,
                    ModelVersion.active_flag == "1",
                    ModelVersion.deleted_flag == "0",
                )
                .order_by(ModelVersion.id.desc())
            )
            row = session.execute(stmt).scalar_one_or_none()
            return _row_to_dict(row) if row else None

    def record_model_eval(
        self,
        model_version_id: int,
        *,
        dataset_snapshot_id: int | None = None,
        eval_type_code: str = "OFFLINE",
        metric_json: dict | list | None = None,
        error_case_json: dict | list | None = None,
        evidence_attachment_key: str | None = None,
        evaluator_name: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = ModelEvalRecord(
                model_version_id=model_version_id,
                dataset_snapshot_id=dataset_snapshot_id,
                eval_type_code=eval_type_code,
                metric_json=metric_json,
                error_case_json=error_case_json,
                evidence_attachment_key=evidence_attachment_key,
                evaluated_at=now,
                evaluator_name=evaluator_name,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def record_model_approval(
        self,
        model_version_id: int,
        decision_code: str,
        *,
        approver_name: str | None = None,
        decision_note: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = ModelApprovalRecord(
                model_version_id=model_version_id,
                decision_code=decision_code,
                approver_name=approver_name,
                decision_note=decision_note,
                approved_at=now if decision_code != "PENDING" else None,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def create_dataset_snapshot(
        self,
        dataset_version: str,
        *,
        snapshot_type_code: str = "TRAIN",
        source_summary: str | None = None,
        sample_count: int | None = None,
        metadata_json: dict | list | None = None,
        dataset_card_path: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = DatasetSnapshot(
                dataset_version=dataset_version,
                snapshot_type_code=snapshot_type_code,
                source_summary=source_summary,
                sample_count=sample_count,
                metadata_json=metadata_json,
                dataset_card_path=dataset_card_path,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def add_dataset_sample(
        self,
        snapshot_id: int,
        sample_ref_no: str,
        *,
        patient_uuid: str | None = None,
        image_ref_no: str | None = None,
        source_type_code: str = "CORRECTION",
        split_type_code: str = "TRAIN",
        label_version: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = DatasetSample(
                snapshot_id=snapshot_id,
                sample_ref_no=sample_ref_no,
                patient_uuid=patient_uuid,
                image_ref_no=image_ref_no,
                source_type_code=source_type_code,
                split_type_code=split_type_code,
                label_version=label_version,
                org_id=org_id,
                created_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def create_training_run(
        self,
        target_model_code: str,
        *,
        dataset_snapshot_id: int | None = None,
        base_model_version: str | None = None,
        run_type_code: str = "TRAIN",
        parameters_json: dict | list | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        training_run_no = f"TRNRUN-{uuid.uuid4().hex[:16].upper()}"
        with session_scope() as session:
            row = TrainingRun(
                training_run_no=training_run_no,
                dataset_snapshot_id=dataset_snapshot_id,
                target_model_code=target_model_code,
                base_model_version=base_model_version,
                run_type_code=run_type_code,
                parameters_json=parameters_json,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def record_annotation(
        self,
        sample_ref_no: str,
        annotation_version: str,
        *,
        patient_uuid: str | None = None,
        annotation_result_json: dict | list | None = None,
        annotation_object_key: str | None = None,
        annotator_l1: str | None = None,
        reviewer_l2: str | None = None,
        qc_status_code: str = "PENDING",
        difficulty_code: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = AnnotationRecord(
                sample_ref_no=sample_ref_no,
                patient_uuid=patient_uuid,
                annotation_version=annotation_version,
                annotation_result_json=annotation_result_json,
                annotation_object_key=annotation_object_key,
                annotator_l1=annotator_l1,
                reviewer_l2=reviewer_l2,
                qc_status_code=qc_status_code,
                difficulty_code=difficulty_code,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def upsert_gold_set_item(
        self,
        sample_ref_no: str,
        *,
        annotation_record_id: int | None = None,
        difficulty_code: str | None = None,
        active_flag: str = "1",
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            existing = session.execute(
                select(GoldSetItem).where(GoldSetItem.sample_ref_no == sample_ref_no)
            ).scalar_one_or_none()
            if existing is not None:
                existing.annotation_record_id = annotation_record_id
                existing.difficulty_code = difficulty_code
                existing.active_flag = active_flag
                existing.updated_at = now
                session.flush()
                return _row_to_dict(existing)
            row = GoldSetItem(
                sample_ref_no=sample_ref_no,
                annotation_record_id=annotation_record_id,
                difficulty_code=difficulty_code,
                active_flag=active_flag,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)
