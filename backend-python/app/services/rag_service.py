from __future__ import annotations

from typing import Any

from app.schemas.rag import DoctorQaRequest, PatientExplanationRequest, RagAskRequest
from app.services.case_context_builder import CaseContextBuilder
from app.services.rag_orchestrator import RagOrchestrator


class RagService:
    def __init__(
        self,
        rag_orchestrator: RagOrchestrator,
        case_context_builder: CaseContextBuilder | None = None,
    ) -> None:
        self.rag_orchestrator = rag_orchestrator
        self.case_context_builder = case_context_builder or CaseContextBuilder()

    def patient_explanation(self, request: PatientExplanationRequest) -> dict[str, Any]:
        query = request.question or self._patient_query(request.risk_level_code)
        return self.rag_orchestrator.answer(
            scene="PATIENT_EXPLAIN",
            question=query,
            top_k=request.top_k,
            kb_code=request.kb_code,
            related_biz_no=request.related_biz_no,
            patient_uuid=request.patient_uuid,
            java_user_id=request.java_user_id,
            org_id=request.org_id,
            trace_id=request.trace_id,
            context_text=self.case_context_builder.build(request.case_summary),
            include_debug=False,
        )

    def doctor_qa(self, request: DoctorQaRequest) -> dict[str, Any]:
        return self.rag_orchestrator.answer(
            scene="DOCTOR_QA",
            question=request.question,
            top_k=request.top_k,
            kb_code=request.kb_code,
            related_biz_no=request.related_biz_no,
            patient_uuid=request.patient_uuid,
            java_user_id=request.java_user_id,
            org_id=request.org_id,
            trace_id=request.trace_id,
            context_text=self.case_context_builder.build(request.clinical_context),
            include_debug=False,
        )

    def ask(self, request: RagAskRequest) -> dict[str, Any]:
        scene = (request.scene or "DOCTOR_QA").strip().upper()
        scene = "PATIENT_EXPLAIN" if scene == "PATIENT_EXPLAIN" else "DOCTOR_QA"
        return self.rag_orchestrator.answer(
            scene=scene,
            question=request.question,
            top_k=request.top_k,
            kb_code=request.kb_code,
            related_biz_no=request.related_biz_no,
            patient_uuid=request.patient_uuid,
            java_user_id=request.java_user_id,
            org_id=request.org_id,
            trace_id=request.trace_id,
            context_text=self.case_context_builder.build(request.case_context),
            include_debug=request.include_debug,
        )

    @staticmethod
    def _patient_query(risk_level_code: str | None) -> str:
        risk = risk_level_code or "UNKNOWN"
        return f"请结合已发布知识，以患者可理解方式解释当前龋病风险等级，并给出家庭护理与复查建议。风险等级：{risk}"
