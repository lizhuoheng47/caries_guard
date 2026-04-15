from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["model-version"])


@router.get("/model-version")
def model_version() -> dict:
    settings = get_container().settings
    data = {
        "toothDetect": {
            "modelVersion": settings.model_version,
            "modelArtifactMd5": "mock",
            "releasedAt": None,
        },
        "tpcNet": {
            "modelVersion": settings.model_version,
            "modelArtifactMd5": "mock",
            "releasedAt": None,
        },
        "edlGrade": {
            "modelVersion": settings.model_version,
            "modelArtifactMd5": "mock",
            "releasedAt": None,
        },
        "riskFusion": {
            "modelVersion": settings.model_version,
            "modelArtifactMd5": "mock",
            "releasedAt": None,
        },
    }
    return success_response(data=data, trace_id="model-version")

