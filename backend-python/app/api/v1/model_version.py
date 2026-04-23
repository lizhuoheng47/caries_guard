from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["model-version"])


@router.get("/model-version")
def model_version() -> dict:
    container = get_container()
    settings = container.settings
    runtime_status = container.model_switch_service.get_runtime_status()
    segmentation_assets = container.model_assets.module_descriptor("segmentation")
    grading_assets = container.model_assets.module_descriptor("grading")

    def _module_payload(name: str, assets: dict | None = None) -> dict:
        module = runtime_status.get("modules", {}).get(name, {})
        artifact = assets or {}
        return {
            "modelVersion": settings.model_version,
            "mode": module.get("mode"),
            "enabled": module.get("enabled"),
            "ready": module.get("ready"),
            "implType": module.get("implType"),
            "modelCode": artifact.get("modelCode"),
            "manifestPath": artifact.get("manifestPath"),
            "manifestStatus": artifact.get("manifestStatus"),
            "checkpointPath": artifact.get("checkpointPath"),
            "checkpointExists": artifact.get("checkpointExists"),
            "checkpointSha256": artifact.get("checkpointSha256"),
            "checkpointFormat": artifact.get("checkpointFormat"),
            "releasedAt": artifact.get("releasedAt"),
            "classMapPath": artifact.get("classMapPath"),
            "preprocessPath": artifact.get("preprocessPath"),
            "postprocessPath": artifact.get("postprocessPath"),
            "labelOrder": artifact.get("labelOrder"),
            "expectedInputSize": module.get("expectedInputSize") or artifact.get("expectedInputSize"),
            "normalization": module.get("normalization"),
            "postprocess": module.get("postprocess"),
            "missingItems": module.get("missingItems") or [],
            "loadError": module.get("loadError"),
        }

    data = {
        "aiRuntimeMode": settings.ai_runtime_mode,
        "runtimeStatus": runtime_status,
        "toothDetect": _module_payload("toothDetect"),
        "tpcNet": _module_payload("segmentation", segmentation_assets),
        "edlGrade": _module_payload("grading", grading_assets),
        "riskFusion": _module_payload("risk"),
    }
    return success_response(data=data, trace_id="model-version")
