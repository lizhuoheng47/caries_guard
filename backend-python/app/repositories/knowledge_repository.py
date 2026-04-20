from __future__ import annotations

import uuid
from typing import Any

import sqlalchemy as sa
from sqlalchemy import delete, desc, func, select, update

from app.core.db import session_scope
from app.core.time_utils import local_naive_now
from app.models.rag import (
    KnowledgeBase,
    KnowledgeDocument,
    KnowledgeDocumentChunk,
    KnowledgeDocumentVersion,
    KnowledgeGraphSyncLog,
    KnowledgeIngestJob,
    KnowledgeIngestJobStep,
    KnowledgePublishRecord,
    KnowledgeRelation,
    KnowledgeReviewRecord,
    KnowledgeSourceFile,
    KnowledgeSourceParseResult,
    KnowledgeEntity,
    KnowledgeRebuildJob,
    RagRequestLog,
    RagEvalRun,
)


def _row_to_dict(obj: Any) -> dict[str, Any]:
    if obj is None:
        return {}
    return {column.name: getattr(obj, column.name) for column in obj.__table__.columns}


class KnowledgeRepository:
    def ensure_knowledge_base(
        self,
        kb_code: str,
        kb_name: str,
        kb_type_code: str,
        knowledge_version: str,
        embedding_model: str,
        vector_store_type_code: str,
        vector_store_path: str | None,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            existing = session.execute(
                select(KnowledgeBase).where(
                    KnowledgeBase.kb_code == kb_code,
                    KnowledgeBase.deleted_flag == "0",
                )
            ).scalar_one_or_none()
            if existing is not None:
                existing.kb_name = kb_name
                existing.kb_type_code = kb_type_code
                existing.knowledge_version = knowledge_version
                existing.embedding_model = embedding_model
                existing.vector_store_type_code = vector_store_type_code
                existing.vector_store_path = vector_store_path
                existing.updated_at = now
                session.flush()
                return _row_to_dict(existing)

            row = KnowledgeBase(
                kb_code=kb_code,
                kb_name=kb_name,
                kb_type_code=kb_type_code,
                knowledge_version=knowledge_version,
                embedding_model=embedding_model,
                vector_store_type_code=vector_store_type_code,
                vector_store_path=vector_store_path,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def get_knowledge_base(self, kb_code: str | None = None, kb_id: int | None = None) -> dict[str, Any] | None:
        with session_scope() as session:
            stmt = select(KnowledgeBase).where(KnowledgeBase.deleted_flag == "0")
            if kb_code:
                stmt = stmt.where(KnowledgeBase.kb_code == kb_code)
            if kb_id:
                stmt = stmt.where(KnowledgeBase.id == kb_id)
            row = session.execute(stmt.order_by(KnowledgeBase.id)).scalar_one_or_none()
            return _row_to_dict(row) if row else None

    def create_document(
        self,
        kb_id: int,
        doc_title: str,
        doc_no: str | None,
        doc_source_code: str,
        source_uri: str | None,
        content_text: str | None,
        doc_version: str,
        org_id: int | None,
        source_file_id: int | None = None,
        created_by: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = KnowledgeDocument(
                kb_id=kb_id,
                doc_no=doc_no or f"DOC-{uuid.uuid4().hex[:16].upper()}",
                doc_title=doc_title,
                doc_source_code=doc_source_code,
                source_uri=source_uri,
                content_text=content_text,
                doc_version=doc_version,
                current_version_no=doc_version,
                source_file_id=source_file_id,
                org_id=org_id,
                created_by=created_by,
                updated_by=created_by,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def update_document_metadata(
        self,
        doc_id: int,
        *,
        doc_title: str | None = None,
        doc_source_code: str | None = None,
        source_uri: str | None = None,
        updated_by: int | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = session.execute(select(KnowledgeDocument).where(KnowledgeDocument.id == doc_id)).scalar_one()
            if doc_title is not None:
                row.doc_title = doc_title
            if doc_source_code is not None:
                row.doc_source_code = doc_source_code
            if source_uri is not None:
                row.source_uri = source_uri
            row.updated_by = updated_by
            row.updated_at = now
            session.flush()
            return _row_to_dict(row)

    def get_document(self, doc_id: int) -> dict[str, Any] | None:
        with session_scope() as session:
            row = session.execute(select(KnowledgeDocument).where(KnowledgeDocument.id == doc_id)).scalar_one_or_none()
            return _row_to_dict(row) if row else None

    def list_documents(self, kb_id: int | None = None, org_id: int | None = None, keyword: str | None = None) -> list[dict[str, Any]]:
        with session_scope() as session:
            stmt = select(KnowledgeDocument).where(KnowledgeDocument.deleted_flag == "0")
            if kb_id is not None:
                stmt = stmt.where(KnowledgeDocument.kb_id == kb_id)
            if org_id is not None:
                stmt = stmt.where(KnowledgeDocument.org_id == org_id)
            if keyword:
                like = f"%{keyword.strip()}%"
                stmt = stmt.where(
                    KnowledgeDocument.doc_title.like(like) | KnowledgeDocument.doc_no.like(like)
                )
            rows = session.execute(stmt.order_by(desc(KnowledgeDocument.updated_at))).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def create_source_file(
        self,
        kb_id: int | None,
        doc_id: int | None,
        bucket_name: str,
        object_key: str,
        file_name: str,
        mime_type: str | None,
        file_size_bytes: int | None,
        md5: str | None,
        source_type_code: str,
        org_id: int | None,
        uploaded_by: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = KnowledgeSourceFile(
                source_file_no=f"SRC-{uuid.uuid4().hex[:16].upper()}",
                kb_id=kb_id,
                doc_id=doc_id,
                bucket_name=bucket_name,
                object_key=object_key,
                file_name=file_name,
                mime_type=mime_type,
                file_size_bytes=file_size_bytes,
                md5=md5,
                source_type_code=source_type_code,
                uploaded_by=uploaded_by,
                uploaded_at=now,
                org_id=org_id,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def mark_source_file(
        self,
        source_file_id: int,
        parse_status_code: str,
        doc_id: int | None = None,
        kb_id: int | None = None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            values: dict[str, Any] = {"parse_status_code": parse_status_code, "updated_at": now}
            if doc_id is not None:
                values["doc_id"] = doc_id
            if kb_id is not None:
                values["kb_id"] = kb_id
            session.execute(
                update(KnowledgeSourceFile).where(KnowledgeSourceFile.id == source_file_id).values(**values)
            )

    def save_parse_result(
        self,
        source_file_id: int,
        parse_status_code: str,
        normalized_markdown: str | None,
        structured_json: dict[str, Any] | list[Any] | None,
        section_tree: dict[str, Any] | list[Any] | None,
        table_json: dict[str, Any] | list[Any] | None,
        metadata_json: dict[str, Any] | list[Any] | None,
        artifact_bucket_name: str | None,
        artifact_object_key: str | None,
        error_message: str | None = None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = KnowledgeSourceParseResult(
                source_file_id=source_file_id,
                parse_status_code=parse_status_code,
                normalized_markdown=normalized_markdown,
                structured_json=structured_json,
                section_tree=section_tree,
                table_json=table_json,
                metadata_json=metadata_json,
                artifact_bucket_name=artifact_bucket_name,
                artifact_object_key=artifact_object_key,
                error_message=error_message,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def create_ingest_job(
        self,
        kb_id: int | None,
        doc_id: int | None,
        source_file_id: int | None,
        org_id: int | None,
        created_by: int | None,
        trace_id: str | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = KnowledgeIngestJob(
                ingest_job_no=f"INGEST-{uuid.uuid4().hex[:16].upper()}",
                kb_id=kb_id,
                doc_id=doc_id,
                source_file_id=source_file_id,
                ingest_status_code="RUNNING",
                current_step_code="UPLOADED",
                trace_id=trace_id,
                started_at=now,
                org_id=org_id,
                created_by=created_by,
                updated_by=created_by,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def record_ingest_step(
        self,
        ingest_job_id: int,
        step_code: str,
        step_order: int,
        step_status_code: str,
        message_text: str | None = None,
        payload_json: dict[str, Any] | list[Any] | None = None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.add(
                KnowledgeIngestJobStep(
                    ingest_job_id=ingest_job_id,
                    step_code=step_code,
                    step_order=step_order,
                    step_status_code=step_status_code,
                    message_text=message_text,
                    payload_json=payload_json,
                    started_at=now,
                    finished_at=now,
                    created_at=now,
                )
            )
            session.execute(
                update(KnowledgeIngestJob)
                .where(KnowledgeIngestJob.id == ingest_job_id)
                .values(current_step_code=step_code, updated_at=now)
            )

    def finish_ingest_job(
        self,
        ingest_job_id: int,
        ingest_status_code: str,
        current_step_code: str,
        error_code: str | None = None,
        error_message: str | None = None,
        doc_id: int | None = None,
    ) -> None:
        now = local_naive_now()
        values: dict[str, Any] = {
            "ingest_status_code": ingest_status_code,
            "current_step_code": current_step_code,
            "error_code": error_code,
            "error_message": error_message,
            "finished_at": now,
            "updated_at": now,
        }
        if doc_id is not None:
            values["doc_id"] = doc_id
        with session_scope() as session:
            session.execute(update(KnowledgeIngestJob).where(KnowledgeIngestJob.id == ingest_job_id).values(**values))

    def create_document_version(
        self,
        doc_id: int,
        version_no: str,
        parent_version_no: str | None,
        normalized_content: str | None,
        structured_json: dict[str, Any] | list[Any] | None,
        section_tree: dict[str, Any] | list[Any] | None,
        table_json: dict[str, Any] | list[Any] | None,
        metadata_json: dict[str, Any] | list[Any] | None,
        change_summary: str | None,
        source_file_id: int | None,
        org_id: int | None,
        created_by: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = KnowledgeDocumentVersion(
                doc_id=doc_id,
                version_no=version_no,
                parent_version_no=parent_version_no,
                normalized_content=normalized_content,
                structured_json=structured_json,
                section_tree=section_tree,
                table_json=table_json,
                metadata_json=metadata_json,
                change_summary=change_summary,
                source_file_id=source_file_id,
                org_id=org_id,
                created_by=created_by,
                updated_by=created_by,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.execute(
                update(KnowledgeDocument)
                .where(KnowledgeDocument.id == doc_id)
                .values(
                    current_version_no=version_no,
                    doc_version=version_no,
                    content_text=normalized_content,
                    review_status_code="PARSED",
                    publish_status_code="DRAFT",
                    updated_by=created_by,
                    updated_at=now,
                )
            )
            session.flush()
            return _row_to_dict(row)

    def get_document_version(self, doc_id: int, version_no: str | None = None) -> dict[str, Any] | None:
        with session_scope() as session:
            stmt = select(KnowledgeDocumentVersion).where(KnowledgeDocumentVersion.doc_id == doc_id)
            if version_no:
                stmt = stmt.where(KnowledgeDocumentVersion.version_no == version_no)
            else:
                stmt = stmt.order_by(desc(KnowledgeDocumentVersion.created_at))
            row = session.execute(stmt).scalars().first()
            return _row_to_dict(row) if row else None

    def ensure_document_version_row(
        self,
        doc_id: int,
        version_no: str,
        *,
        parent_version_no: str | None,
        normalized_content: str | None,
        source_file_id: int | None,
        review_status_code: str | None,
        publish_status_code: str | None,
        org_id: int | None,
        created_by: int | None,
        change_summary: str | None = "Auto backfill missing version row",
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            existing = session.execute(
                select(KnowledgeDocumentVersion).where(
                    KnowledgeDocumentVersion.doc_id == doc_id,
                    KnowledgeDocumentVersion.version_no == version_no,
                )
            ).scalar_one_or_none()
            if existing is not None:
                return _row_to_dict(existing)

            row = KnowledgeDocumentVersion(
                doc_id=doc_id,
                version_no=version_no,
                parent_version_no=parent_version_no,
                normalized_content=normalized_content,
                structured_json=None,
                section_tree=None,
                table_json=None,
                metadata_json=None,
                change_summary=change_summary,
                source_file_id=source_file_id,
                review_status_code=review_status_code or "PENDING",
                publish_status_code=publish_status_code or "DRAFT",
                org_id=org_id,
                created_by=created_by,
                updated_by=created_by,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def list_document_versions(self, doc_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(KnowledgeDocumentVersion)
                .where(KnowledgeDocumentVersion.doc_id == doc_id)
                .order_by(desc(KnowledgeDocumentVersion.created_at))
            ).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def list_review_records(self, doc_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(KnowledgeReviewRecord)
                .where(KnowledgeReviewRecord.doc_id == doc_id)
                .order_by(desc(KnowledgeReviewRecord.reviewed_at))
            ).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def list_publish_records(self, doc_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(KnowledgePublishRecord)
                .where(KnowledgePublishRecord.doc_id == doc_id)
                .order_by(desc(KnowledgePublishRecord.published_at))
            ).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def replace_chunks(
        self,
        kb_id: int,
        doc_id: int,
        version_no: str,
        chunks: list[dict[str, Any]],
        embedding_model: str,
        vector_store_path: str | None,
        publish_status: str = "INDEXED",
    ) -> list[dict[str, Any]]:
        now = local_naive_now()
        stored: list[dict[str, Any]] = []
        with session_scope() as session:
            session.execute(
                delete(KnowledgeDocumentChunk).where(
                    KnowledgeDocumentChunk.doc_id == doc_id,
                    KnowledgeDocumentChunk.version_no == version_no,
                )
            )
            for item in chunks:
                row = KnowledgeDocumentChunk(
                    kb_id=kb_id,
                    doc_id=doc_id,
                    version_no=version_no,
                    chunk_no=item["chunk_no"],
                    section_path=item.get("section_path"),
                    chunk_text=item["chunk_text"],
                    chunk_type=item.get("chunk_type", "PARAGRAPH"),
                    token_count=item.get("token_count"),
                    embedding_model=embedding_model,
                    vector_store_path=vector_store_path,
                    doc_title_snapshot=item.get("doc_title_snapshot"),
                    doc_source_code=item.get("doc_source_code"),
                    source_uri=item.get("source_uri"),
                    medical_tags=item.get("medical_tags"),
                    graph_entity_refs=item.get("graph_entity_refs"),
                    publish_status=publish_status,
                    org_id=item.get("org_id"),
                    created_at=now,
                )
                session.add(row)
                session.flush()
                row.vector_id = f"chunk-{row.id}"
                session.flush()
                stored.append(_row_to_dict(row))
        return stored

    def list_published_chunks(self, kb_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(KnowledgeDocumentChunk, KnowledgeDocument)
                .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeDocumentChunk.doc_id)
                .where(
                    KnowledgeDocumentChunk.kb_id == kb_id,
                    KnowledgeDocumentChunk.publish_status == "PUBLISHED",
                    KnowledgeDocumentChunk.deleted_flag == "0",
                    KnowledgeDocument.publish_status_code == "PUBLISHED",
                )
                .order_by(KnowledgeDocumentChunk.id)
            ).all()
            result: list[dict[str, Any]] = []
            for chunk, doc in rows:
                payload = _row_to_dict(chunk)
                payload["doc_title"] = doc.doc_title
                payload["doc_no"] = doc.doc_no
                payload["source_uri"] = chunk.source_uri or doc.source_uri
                result.append(payload)
            return result

    def list_chunks_for_version(self, doc_id: int, version_no: str) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(KnowledgeDocumentChunk)
                .where(
                    KnowledgeDocumentChunk.doc_id == doc_id,
                    KnowledgeDocumentChunk.version_no == version_no,
                    KnowledgeDocumentChunk.deleted_flag == "0",
                )
                .order_by(KnowledgeDocumentChunk.chunk_no)
            ).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def update_chunk_graph_refs(self, chunk_refs: list[dict[str, Any]]) -> None:
        with session_scope() as session:
            for item in chunk_refs:
                graph_refs = item.get("concept_ids") or item.get("entity_names") or []
                session.execute(
                    update(KnowledgeDocumentChunk)
                    .where(KnowledgeDocumentChunk.id == item["chunk_id"])
                    .values(graph_entity_refs=graph_refs)
                )

    def submit_review(self, doc_id: int, version_no: str, reviewer_id: int | None) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(KnowledgeDocumentVersion)
                .where(
                    KnowledgeDocumentVersion.doc_id == doc_id,
                    KnowledgeDocumentVersion.version_no == version_no,
                )
                .values(review_status_code="REVIEW_PENDING", updated_at=now)
            )
            session.execute(
                update(KnowledgeDocument)
                .where(KnowledgeDocument.id == doc_id)
                .values(review_status_code="REVIEW_PENDING", reviewer_id=reviewer_id, updated_at=now)
            )

    def record_review(
        self,
        doc_id: int,
        version_no: str,
        decision_code: str,
        review_comment: str | None,
        reviewer_id: int | None,
        org_id: int | None,
    ) -> None:
        now = local_naive_now()
        final_status = "APPROVED" if decision_code == "APPROVE" else "REJECTED"
        with session_scope() as session:
            session.add(
                KnowledgeReviewRecord(
                    doc_id=doc_id,
                    version_no=version_no,
                    decision_code=decision_code,
                    review_comment=review_comment,
                    reviewed_at=now,
                    org_id=org_id,
                    created_by=reviewer_id,
                    created_at=now,
                )
            )
            session.execute(
                update(KnowledgeDocumentVersion)
                .where(
                    KnowledgeDocumentVersion.doc_id == doc_id,
                    KnowledgeDocumentVersion.version_no == version_no,
                )
                .values(review_status_code=final_status, updated_at=now)
            )
            session.execute(
                update(KnowledgeDocument)
                .where(KnowledgeDocument.id == doc_id)
                .values(
                    review_status_code=final_status,
                    reviewer_id=reviewer_id,
                    reviewed_at=now,
                    updated_at=now,
                )
            )

    def publish_version(
        self,
        doc_id: int,
        version_no: str,
        operator_id: int | None,
        org_id: int | None,
        action_code: str = "PUBLISH",
        comment_text: str | None = None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            doc = session.execute(select(KnowledgeDocument).where(KnowledgeDocument.id == doc_id)).scalar_one()
            previous_version_no = doc.published_version_no
            session.execute(
                update(KnowledgeDocumentVersion)
                .where(KnowledgeDocumentVersion.doc_id == doc_id)
                .values(
                    publish_status_code=sa.case(
                        (KnowledgeDocumentVersion.version_no == version_no, "PUBLISHED"),
                        else_="DISABLED",
                    ),
                    updated_at=now,
                )
            )
            session.execute(
                update(KnowledgeDocumentChunk)
                .where(KnowledgeDocumentChunk.doc_id == doc_id)
                .values(
                    publish_status=sa.case(
                        (KnowledgeDocumentChunk.version_no == version_no, "PUBLISHED"),
                        else_="DISABLED",
                    )
                )
            )
            session.execute(
                update(KnowledgeDocument)
                .where(KnowledgeDocument.id == doc_id)
                .values(
                    publish_status_code="PUBLISHED",
                    published_version_no=version_no,
                    doc_version=version_no,
                    updated_by=operator_id,
                    updated_at=now,
                )
            )
            session.add(
                KnowledgePublishRecord(
                    doc_id=doc_id,
                    version_no=version_no,
                    previous_version_no=previous_version_no,
                    action_code=action_code,
                    comment_text=comment_text,
                    published_at=now,
                    org_id=org_id,
                    created_by=operator_id,
                    created_at=now,
                )
            )

    def disable_document(self, doc_id: int, operator_id: int | None) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(KnowledgeDocument)
                .where(KnowledgeDocument.id == doc_id)
                .values(enabled_flag="0", publish_status_code="DISABLED", updated_by=operator_id, updated_at=now)
            )

    def list_ingest_jobs(self, org_id: int | None = None) -> list[dict[str, Any]]:
        with session_scope() as session:
            stmt = select(KnowledgeIngestJob)
            if org_id is not None:
                stmt = stmt.where(KnowledgeIngestJob.org_id == org_id)
            rows = session.execute(stmt.order_by(desc(KnowledgeIngestJob.created_at))).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def list_rebuild_jobs(self, kb_id: int | None = None, org_id: int | None = None) -> list[dict[str, Any]]:
        with session_scope() as session:
            stmt = select(KnowledgeRebuildJob)
            if kb_id is not None:
                stmt = stmt.where(KnowledgeRebuildJob.kb_id == kb_id)
            if org_id is not None:
                stmt = stmt.where(KnowledgeRebuildJob.org_id == org_id)
            rows = session.execute(stmt.order_by(desc(KnowledgeRebuildJob.created_at))).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def overview(self, kb_id: int | None = None, org_id: int | None = None) -> dict[str, Any]:
        with session_scope() as session:
            kb_stmt = select(func.count(KnowledgeBase.id))
            doc_stmt = select(func.count(KnowledgeDocument.id))
            chunk_stmt = select(func.count(KnowledgeDocumentChunk.id))
            entity_stmt = select(func.count(KnowledgeEntity.id))
            relation_stmt = select(func.count(KnowledgeRelation.id))
            request_stmt = select(func.count(RagRequestLog.id))
            eval_stmt = select(func.count(RagEvalRun.id))
            if kb_id is not None:
                doc_stmt = doc_stmt.where(KnowledgeDocument.kb_id == kb_id)
                chunk_stmt = chunk_stmt.where(KnowledgeDocumentChunk.kb_id == kb_id)
            if org_id is not None:
                kb_stmt = kb_stmt.where(KnowledgeBase.org_id == org_id)
                doc_stmt = doc_stmt.where(KnowledgeDocument.org_id == org_id)
                chunk_stmt = chunk_stmt.where(KnowledgeDocumentChunk.org_id == org_id)
                entity_stmt = entity_stmt.where(KnowledgeEntity.org_id == org_id)
                relation_stmt = relation_stmt.where(KnowledgeRelation.org_id == org_id)
                request_stmt = request_stmt.where(RagRequestLog.org_id == org_id)
                eval_stmt = eval_stmt.where(RagEvalRun.org_id == org_id)
            latest_rebuild = session.execute(
                select(KnowledgeRebuildJob).order_by(desc(KnowledgeRebuildJob.created_at)).limit(1)
            ).scalar_one_or_none()
            latest_eval = session.execute(
                select(RagEvalRun).order_by(desc(RagEvalRun.created_at)).limit(1)
            ).scalar_one_or_none()
            return {
                "knowledgeBaseCount": session.execute(kb_stmt).scalar_one(),
                "documentCount": session.execute(doc_stmt).scalar_one(),
                "chunkCount": session.execute(chunk_stmt).scalar_one(),
                "entityCount": session.execute(entity_stmt).scalar_one(),
                "relationCount": session.execute(relation_stmt).scalar_one(),
                "requestCount": session.execute(request_stmt).scalar_one(),
                "evalRunCount": session.execute(eval_stmt).scalar_one(),
                "latestRebuildJob": _row_to_dict(latest_rebuild) if latest_rebuild else None,
                "latestEvalRun": _row_to_dict(latest_eval) if latest_eval else None,
            }

    def create_graph_sync_log(
        self,
        doc_id: int,
        version_no: str,
        sync_status_code: str,
        entity_count: int,
        relation_count: int,
        trace_id: str | None,
        error_message: str | None,
        org_id: int | None,
        created_by: int | None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.add(
                KnowledgeGraphSyncLog(
                    doc_id=doc_id,
                    version_no=version_no,
                    sync_status_code=sync_status_code,
                    entity_count=entity_count,
                    relation_count=relation_count,
                    trace_id=trace_id,
                    error_message=error_message,
                    org_id=org_id,
                    created_by=created_by,
                    created_at=now,
                )
            )
