from __future__ import annotations

import uuid
from typing import Any

from sqlalchemy import or_, select, update

from app.core.db import session_scope
from app.core.time_utils import local_naive_now
from app.models.ai_runtime import AiCallbackLog, AiInferArtifact, AiInferJob, AiInferJobImage


def _row_to_dict(obj: Any) -> dict[str, Any]:
    if obj is None:
        return {}
    return {column.name: getattr(obj, column.name) for column in obj.__table__.columns}


class AiRuntimeRepository:
    """Repository for ai_infer_job, ai_infer_job_image, and ai_callback_log."""

    def create_infer_job(
        self,
        java_task_no: str,
        model_version: str,
        *,
        trace_id: str | None = None,
        case_no: str | None = None,
        patient_uuid: str | None = None,
        infer_type_code: str = "ANALYZE",
        request_json: dict | list | None = None,
        callback_required_flag: str = "1",
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        job_no = f"AIJOB-{uuid.uuid4().hex[:16].upper()}"
        with session_scope() as session:
            row = AiInferJob(
                job_no=job_no,
                java_task_no=java_task_no,
                trace_id=trace_id,
                case_no=case_no,
                patient_uuid=patient_uuid,
                infer_type_code=infer_type_code,
                model_version=model_version,
                status_code="RUNNING",
                request_json=request_json,
                started_at=now,
                callback_required_flag=callback_required_flag,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def mark_infer_job_running(self, job_id: int) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(AiInferJob)
                .where(AiInferJob.id == job_id)
                .values(
                    status_code="RUNNING",
                    started_at=now,
                    updated_at=now,
                )
            )
            row = session.execute(select(AiInferJob).where(AiInferJob.id == job_id)).scalar_one_or_none()
            return _row_to_dict(row) if row else {}

    def finish_infer_job(
        self,
        job_id: int,
        status_code: str,
        *,
        result_json: dict | list | None = None,
        error_message: str | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(AiInferJob)
                .where(AiInferJob.id == job_id)
                .values(
                    status_code=status_code,
                    result_json=result_json,
                    error_message=error_message,
                    finished_at=now,
                    updated_at=now,
                )
            )
            row = session.execute(select(AiInferJob).where(AiInferJob.id == job_id)).scalar_one_or_none()
            return _row_to_dict(row) if row else {}

    def get_latest_infer_job(
        self,
        java_task_no: str | None = None,
        *,
        trace_id: str | None = None,
        open_only: bool = False,
    ) -> dict[str, Any] | None:
        if not java_task_no and not trace_id:
            raise ValueError("java_task_no or trace_id is required")
        with session_scope() as session:
            filters = [AiInferJob.deleted_flag == "0"]
            if java_task_no and trace_id:
                filters.append(or_(AiInferJob.java_task_no == java_task_no, AiInferJob.trace_id == trace_id))
            elif java_task_no:
                filters.append(AiInferJob.java_task_no == java_task_no)
            else:
                filters.append(AiInferJob.trace_id == trace_id)
            stmt = select(AiInferJob).where(*filters).order_by(AiInferJob.id.desc())
            if open_only:
                stmt = stmt.where(AiInferJob.status_code.notin_(["SUCCESS", "FAILED", "CANCELLED"]))
            row = session.execute(stmt).scalars().first()
            return _row_to_dict(row) if row else None

    def update_callback_status(self, job_id: int, callback_status_code: str) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(AiInferJob)
                .where(AiInferJob.id == job_id)
                .values(
                    callback_status_code=callback_status_code,
                    updated_at=now,
                )
            )
            row = session.execute(select(AiInferJob).where(AiInferJob.id == job_id)).scalar_one_or_none()
            return _row_to_dict(row) if row else {}

    def upsert_job_image(
        self,
        job_id: int,
        image_id: int | None,
        **fields: Any,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = session.execute(
                select(AiInferJobImage)
                .where(
                    AiInferJobImage.job_id == job_id,
                    AiInferJobImage.image_id == image_id,
                    AiInferJobImage.deleted_flag == "0",
                )
            ).scalar_one_or_none()
            if row is None:
                row = AiInferJobImage(
                    job_id=job_id,
                    image_id=image_id,
                    created_at=now,
                    updated_at=now,
                    **fields,
                )
                session.add(row)
                session.flush()
                return _row_to_dict(row)
            for key, value in fields.items():
                setattr(row, key, value)
            row.updated_at = now
            session.flush()
            return _row_to_dict(row)

    def add_job_image(self, job_id: int, **fields: Any) -> dict[str, Any]:
        payload = dict(fields)
        image_id = payload.pop("image_id", None)
        return self.upsert_job_image(job_id, image_id, **payload)

    def list_job_images(self, job_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(AiInferJobImage)
                .where(
                    AiInferJobImage.job_id == job_id,
                    AiInferJobImage.deleted_flag == "0",
                )
                .order_by(AiInferJobImage.id.asc())
            ).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def add_artifact(self, job_id: int, **fields: Any) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = AiInferArtifact(
                job_id=job_id,
                created_at=now,
                updated_at=now,
                **fields,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def record_callback(
        self,
        job_id: int,
        callback_url: str,
        *,
        request_json: dict | list | None = None,
        response_code: int | None = None,
        response_body: str | None = None,
        callback_status_code: str = "PENDING",
        retry_count: int = 0,
        error_message: str | None = None,
        trace_id: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = AiCallbackLog(
                job_id=job_id,
                callback_url=callback_url,
                request_json=request_json,
                response_code=response_code,
                response_body=response_body,
                callback_status_code=callback_status_code,
                retry_count=retry_count,
                error_message=error_message,
                trace_id=trace_id,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)
