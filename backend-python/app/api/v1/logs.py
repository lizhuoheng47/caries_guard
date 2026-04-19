from __future__ import annotations

from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["rag-logs"])


@router.get("/logs/requests")
def list_requests(org_id: int | None = None) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.list_requests(org_id))


@router.get("/logs/requests/{request_no}")
def request_detail(request_no: str) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.request_detail(request_no))


@router.get("/logs/retrievals/{request_no}")
def retrieval_logs(request_no: str) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.retrieval_logs(request_no))


@router.get("/logs/graph/{request_no}")
def graph_logs(request_no: str) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.graph_logs(request_no))


@router.get("/logs/fusion/{request_no}")
def fusion_logs(request_no: str) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.fusion_logs(request_no))


@router.get("/logs/rerank/{request_no}")
def rerank_logs(request_no: str) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.rerank_logs(request_no))


@router.get("/logs/llm/{request_no}")
def llm_logs(request_no: str) -> dict:
    container = get_container()
    return success_response(data=container.rag_log_service.llm_call_logs(request_no))
