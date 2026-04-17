from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["health"])


@router.get("/health")
@router.head("/health")
def health_check() -> dict:
    container = get_container()
    data = {
        "status": "UP",
        "mode": container.settings.app_mode,
        "modelRegistry": {
            "modelVersion": container.settings.model_version,
            "status": "MOCK",
        },
        "dependencies": {
            "minioEndpoint": container.settings.minio_endpoint,
            "rabbitHost": container.settings.rabbit_host,
            "metadataDbPath": container.settings.metadata_db_path,
            "ragIndexDir": container.settings.rag_index_dir,
        },
    }
    return success_response(data=data, trace_id="health")
