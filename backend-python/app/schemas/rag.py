from __future__ import annotations

from typing import Any

from app.schemas.base import CamelModel


class KnowledgeDocumentRequest(CamelModel):
    kb_code: str | None = None
    kb_name: str | None = None
    kb_type_code: str = "PATIENT_GUIDE"
    doc_no: str | None = None
    doc_title: str
    doc_source_code: str = "INTERNAL"
    source_uri: str | None = None
    doc_version: str = "v1.0"
    content_text: str
    review_status_code: str = "PENDING"
    org_id: int | None = None


class KnowledgeRebuildRequest(CamelModel):
    kb_code: str | None = None
    kb_name: str | None = None
    kb_type_code: str = "PATIENT_GUIDE"
    knowledge_version: str | None = None
    org_id: int | None = None


class RagBaseRequest(CamelModel):
    trace_id: str | None = None
    kb_code: str | None = None
    top_k: int | None = None
    related_biz_no: str | None = None
    patient_uuid: str | None = None
    java_user_id: int | None = None
    org_id: int | None = None


class PatientExplanationRequest(RagBaseRequest):
    question: str | None = None
    case_summary: dict[str, Any] | None = None
    risk_level_code: str | None = None


class DoctorQaRequest(RagBaseRequest):
    question: str
    clinical_context: dict[str, Any] | None = None


class RagCitation(CamelModel):
    rank_no: int
    doc_id: int
    doc_title: str | None = None
    chunk_id: int
    score: float
    source_uri: str | None = None
    chunk_text: str


class RagAnswer(CamelModel):
    session_no: str
    request_no: str
    answer_text: str
    citations: list[RagCitation]
    knowledge_version: str
    model_name: str
    safety_flag: str = "0"
    latency_ms: int
