from __future__ import annotations

from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response
from app.schemas.rag import RagEvalRunRequest

router = APIRouter(tags=["eval"])


@router.get("/eval/runs")
def list_runs() -> dict:
    container = get_container()
    return success_response(data=container.eval_service.list_runs())


@router.get("/eval/datasets")
def list_datasets() -> dict:
    container = get_container()
    return success_response(data=container.eval_service.list_datasets())


@router.get("/eval/datasets/{dataset_id}")
def dataset_detail(dataset_id: int) -> dict:
    container = get_container()
    return success_response(data=container.eval_service.dataset_detail(dataset_id))


@router.get("/eval/runs/{run_no}")
def run_detail(run_no: str) -> dict:
    container = get_container()
    return success_response(data=container.eval_service.run_detail(run_no))


@router.get("/eval/runs/{run_no}/results")
def run_results(run_no: str) -> dict:
    container = get_container()
    return success_response(data=container.eval_service.run_results(run_no))


@router.post("/eval/run")
def run_eval(payload: RagEvalRunRequest) -> dict:
    container = get_container()
    result = container.eval_service.run_dataset(
        dataset_id=payload.dataset_id,
        org_id=payload.org_id,
        created_by=payload.operator_id,
    )
    return success_response(data=result, trace_id=payload.trace_id)
