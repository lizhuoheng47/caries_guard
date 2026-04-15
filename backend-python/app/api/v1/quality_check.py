from fastapi import APIRouter

from app.container import get_container
from app.schemas.base import dump_camel
from app.schemas.common import success_response
from app.schemas.request import ImageInput, QualityCheckRequest

router = APIRouter(tags=["quality-check"])


@router.post("/quality-check")
def quality_check(request: QualityCheckRequest) -> dict:
    container = get_container()
    image = ImageInput(
        image_id=request.image_id,
        image_type_code=request.image_type_code,
        bucket_name=request.bucket_name,
        object_key=request.object_key,
        access_url=request.access_url,
    )
    result = container.quality_service.check(image)
    return success_response(data=dump_camel(result), trace_id=request.trace_id)

