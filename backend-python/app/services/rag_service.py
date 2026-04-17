from __future__ import annotations

import json
import time
from typing import Any

from app.core.config import Settings
from app.infra.llm.template_llm_client import TemplateLlmClient
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.repositories.metadata_repository import MetadataRepository
from app.schemas.base import dump_camel
from app.schemas.rag import DoctorQaRequest, PatientExplanationRequest, RagAnswer, RagCitation
from app.services.knowledge_service import KnowledgeService


class RagService:
    def __init__(
        self,
        settings: Settings,
        repository: MetadataRepository,
        vector_store: SimpleVectorStore,
        llm_client: TemplateLlmClient,
        knowledge_service: KnowledgeService,
    ) -> None:
        self.settings = settings
        self.repository = repository
        self.vector_store = vector_store
        self.llm_client = llm_client
        self.knowledge_service = knowledge_service

    def patient_explanation(self, request: PatientExplanationRequest) -> dict[str, Any]:
        query = request.question or self._patient_query(request)
        context_text = json.dumps(request.case_summary or {}, ensure_ascii=False)
        return self._answer(
            scene="PATIENT_EXPLAIN",
            request_type_code="PATIENT_EXPLAIN",
            session_type_code="PATIENT_EXPLAIN",
            query=query,
            context_text=context_text,
            kb_code=request.kb_code,
            top_k=request.top_k,
            related_biz_no=request.related_biz_no,
            patient_uuid=request.patient_uuid,
            java_user_id=request.java_user_id,
            org_id=request.org_id,
        )

    def doctor_qa(self, request: DoctorQaRequest) -> dict[str, Any]:
        context_text = json.dumps(request.clinical_context or {}, ensure_ascii=False)
        return self._answer(
            scene="DOCTOR_QA",
            request_type_code="DOCTOR_QA",
            session_type_code="DOCTOR_QA",
            query=request.question,
            context_text=context_text,
            kb_code=request.kb_code,
            top_k=request.top_k,
            related_biz_no=request.related_biz_no,
            patient_uuid=request.patient_uuid,
            java_user_id=request.java_user_id,
            org_id=request.org_id,
        )

    def _answer(
        self,
        scene: str,
        request_type_code: str,
        session_type_code: str,
        query: str,
        context_text: str | None,
        kb_code: str | None,
        top_k: int | None,
        related_biz_no: str | None,
        patient_uuid: str | None,
        java_user_id: int | None,
        org_id: int | None,
    ) -> dict[str, Any]:
        started = time.perf_counter()
        kb = self.knowledge_service.ensure_knowledge_base(kb_code=kb_code, org_id=org_id)
        limit = top_k or self.settings.rag_top_k
        hits = self.vector_store.search(kb["vector_store_path"], query, limit)
        session = self.repository.create_rag_session(
            session_type_code=session_type_code,
            knowledge_version=kb["knowledge_version"],
            model_name=self.settings.llm_model_name,
            related_biz_no=related_biz_no,
            patient_uuid=patient_uuid,
            java_user_id=java_user_id,
            org_id=org_id,
        )
        request_log = self.repository.create_rag_request(
            session_id=session["id"],
            request_type_code=request_type_code,
            user_query=query,
            rewritten_query=query,
            top_k=limit,
            org_id=org_id,
        )
        llm_started = time.perf_counter()
        llm_result = self.llm_client.generate(scene=scene, query=query, evidence=hits, context_text=context_text)
        llm_latency_ms = int((time.perf_counter() - llm_started) * 1000)
        total_latency_ms = int((time.perf_counter() - started) * 1000)
        safety_flag = "1" if not hits else "0"
        self.repository.create_retrieval_logs(request_log["id"], hits, org_id)
        self.repository.create_llm_call_log(
            request_id=request_log["id"],
            model_name=self.settings.llm_model_name,
            provider_code=self.settings.llm_provider_code,
            prompt_text=llm_result.prompt_text,
            completion_text=llm_result.answer_text,
            latency_ms=llm_latency_ms,
            status_code="SUCCESS",
            org_id=org_id,
        )
        self.repository.finish_rag_request(
            request_id=request_log["id"],
            answer_text=llm_result.answer_text,
            status_code="SUCCESS",
            latency_ms=total_latency_ms,
            safety_flag=safety_flag,
        )
        answer = RagAnswer(
            session_no=session["session_no"],
            request_no=request_log["request_no"],
            answer_text=llm_result.answer_text,
            citations=[
                RagCitation(
                    rank_no=index,
                    doc_id=hit["doc_id"],
                    doc_title=hit.get("doc_title"),
                    chunk_id=hit["chunk_id"],
                    score=hit["score"],
                    source_uri=hit.get("source_uri"),
                    chunk_text=hit["chunk_text"],
                )
                for index, hit in enumerate(hits, start=1)
            ],
            knowledge_version=kb["knowledge_version"],
            model_name=self.settings.llm_model_name,
            safety_flag=safety_flag,
            latency_ms=total_latency_ms,
        )
        return dump_camel(answer)

    @staticmethod
    def _patient_query(request: PatientExplanationRequest) -> str:
        risk = request.risk_level_code or "UNKNOWN"
        return f"请生成患者可理解的龋齿分析解释、护理建议和复查建议，风险等级为 {risk}。"
