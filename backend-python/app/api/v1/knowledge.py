from __future__ import annotations

from fastapi import APIRouter, File, Form, UploadFile

from app.container import get_container
from app.schemas.common import success_response
from app.schemas.rag import (
    KnowledgeDocumentRequest,
    KnowledgeDocumentUpdateRequest,
    KnowledgeRebuildRequest,
    KnowledgeVersionActionRequest,
)

router = APIRouter(tags=["knowledge"])


@router.get("/knowledge/overview")
def knowledge_overview(kb_code: str | None = None, org_id: int | None = None) -> dict:
    container = get_container()
    return success_response(data=container.knowledge_service.overview(kb_code=kb_code, org_id=org_id))


@router.get("/knowledge/documents")
def list_documents(kb_code: str | None = None, org_id: int | None = None, keyword: str | None = None) -> dict:
    container = get_container()
    return success_response(data=container.knowledge_service.list_documents(kb_code=kb_code, org_id=org_id, keyword=keyword))


@router.get("/knowledge/documents/{doc_id}")
def document_detail(doc_id: int) -> dict:
    container = get_container()
    return success_response(data=container.knowledge_service.get_document_detail(doc_id))


@router.post("/knowledge/documents")
def import_document(request: KnowledgeDocumentRequest) -> dict:
    container = get_container()
    result = container.knowledge_service.import_document(request)
    return success_response(data=result, trace_id=request.trace_id)


@router.post("/knowledge/documents/upload")
async def upload_document(
    file: UploadFile = File(...),
    kb_code: str | None = Form(default=None),
    kb_name: str | None = Form(default=None),
    kb_type_code: str = Form(default="PATIENT_GUIDE"),
    doc_title: str | None = Form(default=None),
    doc_source_code: str = Form(default="UPLOAD"),
    source_uri: str | None = Form(default=None),
    doc_no: str | None = Form(default=None),
    doc_version: str | None = Form(default=None),
    change_summary: str | None = Form(default=None),
    org_id: int | None = Form(default=None),
    operator_id: int | None = Form(default=None),
    trace_id: str | None = Form(default=None),
) -> dict:
    container = get_container()
    content = await file.read()
    result = container.knowledge_service.upload_document(
        file_name=file.filename,
        content_type=file.content_type,
        data=content,
        kb_code=kb_code,
        kb_name=kb_name,
        kb_type_code=kb_type_code,
        doc_title=doc_title,
        doc_source_code=doc_source_code,
        source_uri=source_uri,
        doc_no=doc_no,
        doc_version=doc_version,
        change_summary=change_summary,
        org_id=org_id,
        operator_id=operator_id,
        trace_id=trace_id,
    )
    return success_response(data=result, trace_id=trace_id)


@router.put("/knowledge/documents/{doc_id}")
def update_document(doc_id: int, payload: KnowledgeDocumentUpdateRequest) -> dict:
    container = get_container()
    result = container.knowledge_service.update_document(
        doc_id=doc_id,
        doc_title=payload.doc_title,
        doc_source_code=payload.doc_source_code,
        source_uri=payload.source_uri,
        content_text=payload.content_text,
        change_summary=payload.change_summary,
        operator_id=payload.operator_id,
        trace_id=payload.trace_id,
    )
    return success_response(data=result, trace_id=payload.trace_id)


@router.delete("/knowledge/documents/{doc_id}")
def delete_document(
    doc_id: int,
    operator_id: int | None = None,
    org_id: int | None = None,
    trace_id: str | None = None,
) -> dict:
    container = get_container()
    result = container.knowledge_service.delete_document(
        doc_id=doc_id,
        operator_id=operator_id,
        org_id=org_id,
        trace_id=trace_id,
    )
    return success_response(data=result, trace_id=trace_id)


@router.post("/knowledge/documents/{doc_id}/submit-review")
def submit_review(doc_id: int, payload: KnowledgeVersionActionRequest) -> dict:
    container = get_container()
    container.knowledge_service.submit_review(doc_id, payload.version_no, payload.reviewer_id)
    return success_response(data={"docId": doc_id, "versionNo": payload.version_no, "status": "REVIEW_PENDING"}, trace_id=payload.trace_id)


@router.post("/knowledge/documents/{doc_id}/approve")
def approve_document(doc_id: int, payload: KnowledgeVersionActionRequest) -> dict:
    container = get_container()
    container.knowledge_service.approve(doc_id, payload.version_no, payload.reviewer_id, payload.org_id, payload.comment)
    return success_response(data={"docId": doc_id, "versionNo": payload.version_no, "status": "APPROVED"}, trace_id=payload.trace_id)


@router.post("/knowledge/documents/{doc_id}/reject")
def reject_document(doc_id: int, payload: KnowledgeVersionActionRequest) -> dict:
    container = get_container()
    container.knowledge_service.reject(doc_id, payload.version_no, payload.reviewer_id, payload.org_id, payload.comment)
    return success_response(data={"docId": doc_id, "versionNo": payload.version_no, "status": "REJECTED"}, trace_id=payload.trace_id)


@router.post("/knowledge/documents/{doc_id}/publish")
def publish_document(doc_id: int, payload: KnowledgeVersionActionRequest) -> dict:
    container = get_container()
    container.knowledge_service.publish(doc_id, payload.version_no, payload.operator_id, payload.org_id, payload.comment)
    return success_response(data={"docId": doc_id, "versionNo": payload.version_no, "status": "PUBLISHED"}, trace_id=payload.trace_id)


@router.post("/knowledge/documents/{doc_id}/rollback")
def rollback_document(doc_id: int, payload: KnowledgeVersionActionRequest) -> dict:
    container = get_container()
    container.knowledge_service.rollback(doc_id, payload.version_no, payload.operator_id, payload.org_id, payload.comment)
    return success_response(data={"docId": doc_id, "versionNo": payload.version_no, "status": "ROLLED_BACK"}, trace_id=payload.trace_id)


@router.get("/knowledge/ingest-jobs")
def ingest_jobs(org_id: int | None = None) -> dict:
    container = get_container()
    return success_response(data=container.knowledge_service.list_ingest_jobs(org_id))


@router.get("/knowledge/rebuild-jobs")
def rebuild_jobs(kb_code: str | None = None, org_id: int | None = None) -> dict:
    container = get_container()
    return success_response(data=container.knowledge_service.list_rebuild_jobs(kb_code, org_id))


@router.get("/knowledge/graph-stats")
def graph_stats(kb_code: str | None = None, org_id: int | None = None) -> dict:
    container = get_container()
    return success_response(data=container.knowledge_service.graph_statistics(kb_code, org_id))


@router.post("/knowledge/rebuild")
def rebuild_knowledge(request: KnowledgeRebuildRequest) -> dict:
    container = get_container()
    result = container.knowledge_service.rebuild(request)
    return success_response(data=result, trace_id=request.trace_id)
