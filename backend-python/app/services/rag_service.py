from __future__ import annotations

import time
from typing import Any

from app.core.config import Settings
from app.infra.llm.base_llm_client import BaseLlmClient
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.repositories.rag_repository import RagRepository
from app.schemas.base import dump_camel
from app.schemas.rag import DoctorQaRequest, PatientExplanationRequest, RagAnswer, RagAskRequest
from app.services.case_context_builder import CaseContextBuilder
from app.services.citation_assembler import CitationAssembler
from app.services.knowledge_service import KnowledgeService
from app.services.query_rewrite_service import QueryRewriteService
from app.services.rag_safety_guard_service import RagSafetyGuardService


class RagService:
    def __init__(
        self,
        settings: Settings,
        repository: RagRepository,
        vector_store: SimpleVectorStore,
        llm_client: BaseLlmClient,
        knowledge_service: KnowledgeService,
        query_rewrite_service: QueryRewriteService | None = None,
        case_context_builder: CaseContextBuilder | None = None,
        citation_assembler: CitationAssembler | None = None,
        safety_guard_service: RagSafetyGuardService | None = None,
    ) -> None:
        self.settings = settings
        self.repository = repository
        self.vector_store = vector_store
        self.llm_client = llm_client
        self.knowledge_service = knowledge_service
        self.query_rewrite_service = query_rewrite_service or QueryRewriteService()
        self.case_context_builder = case_context_builder or CaseContextBuilder()
        self.citation_assembler = citation_assembler or CitationAssembler()
        self.safety_guard_service = safety_guard_service or RagSafetyGuardService()

    def patient_explanation(self, request: PatientExplanationRequest) -> dict[str, Any]:
        query = request.question or self._patient_query(request)
        context_text = self.case_context_builder.build(request.case_summary)
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
            trace_id=request.trace_id,
        )

    def doctor_qa(self, request: DoctorQaRequest) -> dict[str, Any]:
        context_text = self.case_context_builder.build(request.clinical_context)
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
            trace_id=request.trace_id,
        )

    def ask(self, request: RagAskRequest) -> dict[str, Any]:
        scene = (request.scene or "DOCTOR_QA").strip().upper()
        request_type_code = "PATIENT_EXPLAIN" if scene == "PATIENT_EXPLAIN" else "DOCTOR_QA"
        context_text = self.case_context_builder.build(request.case_context)
        return self._answer(
            scene=request_type_code,
            request_type_code=request_type_code,
            session_type_code=request_type_code,
            query=request.question,
            context_text=context_text,
            kb_code=request.kb_code,
            top_k=request.top_k,
            related_biz_no=request.related_biz_no,
            patient_uuid=request.patient_uuid,
            java_user_id=request.java_user_id,
            org_id=request.org_id,
            trace_id=request.trace_id,
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
        trace_id: str | None,
    ) -> dict[str, Any]:
        started = time.perf_counter()
        rewritten_query = self.query_rewrite_service.rewrite(query)
        kb = self.knowledge_service.ensure_knowledge_base(kb_code=kb_code, org_id=org_id)
        limit = top_k or self.settings.rag_top_k
        hits = self.vector_store.search(kb["vector_store_path"], rewritten_query, limit)
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
            rewritten_query=rewritten_query,
            top_k=limit,
            org_id=org_id,
        )

        safety_decision = self.safety_guard_service.evaluate(scene, rewritten_query, hits, context_text)
        prompt_summary = self.safety_guard_service.prompt_summary(scene, rewritten_query, hits, context_text)
        if safety_decision.refusal_reason:
            llm_latency_ms = 0
            answer_text = safety_decision.answer_text or "The request cannot be answered safely."
            llm_status = "SKIPPED"
        else:
            llm_started = time.perf_counter()
            llm_result = self.llm_client.generate(
                scene=scene,
                query=rewritten_query,
                evidence=hits,
                context_text=context_text,
            )
            llm_latency_ms = int((time.perf_counter() - llm_started) * 1000)
            answer_text = llm_result.answer_text
            llm_status = "SUCCESS"

        total_latency_ms = int((time.perf_counter() - started) * 1000)
        safety_flag = "1" if safety_decision.refusal_reason or len(safety_decision.safety_flags) > 1 else "0"
        self.repository.create_retrieval_logs(request_log["id"], hits, org_id)
        self.repository.create_llm_call_log(
            request_id=request_log["id"],
            model_name=self.settings.llm_model_name,
            provider_code=self.settings.llm_provider_code,
            prompt_text=prompt_summary,
            completion_text=answer_text,
            latency_ms=llm_latency_ms,
            status_code=llm_status,
            org_id=org_id,
        )
        self.repository.finish_rag_request(
            request_id=request_log["id"],
            answer_text=answer_text,
            status_code="SUCCESS",
            latency_ms=total_latency_ms,
            safety_flag=safety_flag,
        )

        answer = RagAnswer(
            session_no=session["session_no"],
            request_no=request_log["request_no"],
            answer_text=answer_text,
            citations=self.citation_assembler.citations(kb, hits),
            retrieved_chunks=self.citation_assembler.retrieved_chunks(hits),
            knowledge_version=kb["knowledge_version"],
            model_name=self.settings.llm_model_name,
            safety_flag=safety_flag,
            safety_flags=safety_decision.safety_flags,
            refusal_reason=safety_decision.refusal_reason,
            confidence=self.safety_guard_service.confidence(hits, safety_decision.refusal_reason),
            trace_id=trace_id,
            latency_ms=total_latency_ms,
        )
        return dump_camel(answer)

    @staticmethod
    def _patient_query(request: PatientExplanationRequest) -> str:
        risk = request.risk_level_code or "UNKNOWN"
        return (
            "Generate a patient-friendly caries analysis explanation, home care advice, "
            f"and follow-up recommendation. Risk level: {risk}."
        )
