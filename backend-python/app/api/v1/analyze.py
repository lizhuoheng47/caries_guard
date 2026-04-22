from concurrent.futures import ThreadPoolExecutor

from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response
from app.schemas.request import AnalyzeRequest

router = APIRouter(tags=["analyze"])
executor = ThreadPoolExecutor(max_workers=2)


def _run_background(raw_payload: dict) -> None:
    container = get_container()
    container.analysis_service.execute(raw_payload)


@router.post("/analyze")
def analyze(request: AnalyzeRequest) -> dict:
    raw_payload = request.model_dump(by_alias=True, exclude_none=True)
    executor.submit(_run_background, raw_payload)
    data = {
        "taskNo": request.task_no,
        "taskStatusCode": "QUEUEING",
        "estimatedSeconds": 12,
    }
    return success_response(data=data, trace_id=request.trace_id, message="accepted")
