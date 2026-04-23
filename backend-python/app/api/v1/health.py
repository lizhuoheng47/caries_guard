from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["health"])


@router.get("/health")
@router.head("/health")
def health_check() -> dict:
    container = get_container()
    runtime_status = container.model_switch_service.get_runtime_status()
    data = {
        "status": "UP",
        "mode": container.settings.app_mode,
        "modelRegistry": {
            "modelVersion": container.settings.model_version,
            "status": runtime_status.get("aiRuntimeMode"),
            "runtimeStatus": runtime_status,
        },
        "dependencies": {
            "minioEndpoint": container.settings.minio_endpoint,
            "rabbitHost": container.settings.rabbit_host,
            "mysqlHost": container.settings.mysql_host,
            "mysqlPort": container.settings.mysql_port,
            "mysqlDatabase": container.settings.mysql_database,
        },
    }
    return success_response(data=data, trace_id="health")
