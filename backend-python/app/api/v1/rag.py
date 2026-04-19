from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response
from app.schemas.rag import DoctorQaRequest, PatientExplanationRequest, RagAskRequest

router = APIRouter(tags=["rag"])


@router.post("/rag/patient-explanation")
def patient_explanation(request: PatientExplanationRequest) -> dict:
    container = get_container()
    result = container.rag_service.patient_explanation(request)
    return success_response(data=result, trace_id=request.trace_id)


@router.post("/rag/doctor-qa")
def doctor_qa(request: DoctorQaRequest) -> dict:
    container = get_container()
    result = container.rag_service.doctor_qa(request)
    return success_response(data=result, trace_id=request.trace_id)


@router.post("/rag/ask")
def ask(request: RagAskRequest) -> dict:
    container = get_container()
    result = container.rag_service.ask(request)
    return success_response(data=result, trace_id=request.trace_id)


@router.post("/rag/retrieval-debug")
def retrieval_debug(request: RagAskRequest) -> dict:
    container = get_container()
    debug_request = request.model_copy(update={"include_debug": True})
    result = container.rag_service.ask(debug_request)
    return success_response(data=result, trace_id=request.trace_id)


@router.post("/rag/evidence-inspect")
def evidence_inspect(request: RagAskRequest) -> dict:
    container = get_container()
    debug_request = request.model_copy(update={"include_debug": True})
    result = container.rag_service.ask(debug_request)
    return success_response(data={"evidence": result.get("evidence", []), "debug": result.get("debug")}, trace_id=request.trace_id)
