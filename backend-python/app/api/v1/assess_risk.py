from fastapi import APIRouter

from app.container import get_container
from app.schemas.base import dump_camel
from app.schemas.common import success_response
from app.schemas.request import RiskAssessmentRequest

router = APIRouter(tags=["assess-risk"])


@router.post("/assess-risk")
def assess_risk(request: RiskAssessmentRequest) -> dict:
    container = get_container()
    result = container.risk_service.assess(request.patient_profile)
    return success_response(data=dump_camel(result), trace_id=request.trace_id)

