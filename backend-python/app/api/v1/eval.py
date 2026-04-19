from __future__ import annotations

from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["eval"])


@router.get("/eval/runs")
def list_runs() -> dict:
    container = get_container()
    return success_response(data=container.eval_service.list_runs())


@router.post("/eval/run")
def run_eval(payload: dict) -> dict:
    container = get_container()
    result = container.eval_service.run_dataset(
        dataset_id=payload["datasetId"],
        org_id=payload.get("orgId"),
        created_by=payload.get("operatorId"),
    )
    return success_response(data=result, trace_id=payload.get("traceId"))
