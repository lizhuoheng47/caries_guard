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
