from __future__ import annotations

from typing import Any

from app.schemas.base import CamelModel


class KnowledgeDocumentRequest(CamelModel):
    trace_id: str | None = None
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
    trace_id: str | None = None
    kb_code: str | None = None
    kb_name: str | None = None
    kb_type_code: str = "PATIENT_GUIDE"
    knowledge_version: str | None = None
    org_id: int | None = None
    rebuild_parse: bool = True
    rebuild_lexical: bool = True
    rebuild_dense: bool = True
    rebuild_graph: bool = True
    cleanup_stale: bool = True


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


class RagAskRequest(RagBaseRequest):
    question: str
    scene: str = "DOCTOR_QA"
    case_context: dict[str, Any] | None = None
    include_debug: bool = False


class RagCitation(CamelModel):
    rank_no: int
    evidence_type: str = "TEXT"
    knowledge_base_code: str | None = None
    document_code: str | None = None
    document_version: str | None = None
    doc_id: int | None = None
    doc_title: str | None = None
    chunk_id: int | None = None
    score: float
    retrieval_score: float | None = None
    source_uri: str | None = None
    chunk_text: str = ""
    cypher_template_code: str | None = None
    graph_path_id: str | None = None
    provenance_json: dict[str, Any] | list[Any] | None = None


class RagRetrievedChunk(CamelModel):
    chunk_id: int
    document_code: str | None = None
    score: float
    chunk_text: str | None = None
    doc_title: str | None = None


class RagGraphEvidence(CamelModel):
    graph_path_id: str
    cypher_template_code: str | None = None
    score: float
    evidence_text: str | None = None
    result_path_json: dict[str, Any] | None = None
    chunk_id: int | None = None
    doc_id: int | None = None
    doc_title: str | None = None
    doc_version: str | None = None


class RagEvidence(CamelModel):
    evidence_id: str
    evidence_type: str
    channel: str
    score: float
    fusion_score: float | None = None
    rerank_score: float | None = None
    doc_id: int | None = None
    chunk_id: int | None = None
    document_code: str | None = None
    document_version: str | None = None
    doc_title: str | None = None
    source_uri: str | None = None
    chunk_text: str | None = None
    evidence_text: str | None = None
    cypher_template_code: str | None = None
    graph_path_id: str | None = None
    provenance_json: dict[str, Any] | list[Any] | None = None
    source_authority_score: float | None = None
    freshness_score: float | None = None


class RagDebugMeta(CamelModel):
    rewritten_query: str
    intent_code: str
    linked_entities: list[dict[str, Any]]
    lexical_hit_count: int
    dense_hit_count: int
    graph_hit_count: int
    evidence_sufficient: bool
    rerank_provider: str | None = None
    rerank_model: str | None = None


class RagAnswer(CamelModel):
    session_no: str
    request_no: str
    answer_text: str
    citations: list[RagCitation]
    evidence: list[RagEvidence] = []
    retrieved_chunks: list[RagRetrievedChunk] = []
    graph_evidence: list[RagGraphEvidence] = []
    knowledge_base_code: str | None = None
    knowledge_version: str
    model_name: str
    safety_flag: str = "0"
    safety_flags: list[str] = []
    refusal_reason: str | None = None
    confidence: float | None = None
    case_context_summary: str | None = None
    trace_id: str | None = None
    latency_ms: int
    debug: RagDebugMeta | None = None
