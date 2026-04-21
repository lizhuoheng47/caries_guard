from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response

router = APIRouter(tags=["model-version"])


@router.get("/model-version")
def model_version() -> dict:
    container = get_container()
    settings = container.settings

    # ── Phase 5A: include runtime status ────────────────────────────────
    runtime_status = container.model_switch_service.get_runtime_status()

    data = {
        "aiRuntimeMode": settings.ai_runtime_mode,
        "runtimeStatus": runtime_status,
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
        "rag": {
            "knowledgeBaseCode": settings.rag_default_kb_code,
            "knowledgeVersion": settings.rag_knowledge_version,
            "embeddingModel": settings.rag_embedding_model,
            "vectorStoreType": settings.rag_vector_store_type,
            "llmProviderCode": settings.llm_provider_code,
            "llmModelName": settings.llm_model_name,
            "llmSceneRoutingEnabled": settings.llm_scene_routing_enabled,
            "llmSceneProfiles": {
                "DOCTOR_QA": {
                    "providerCode": settings.llm_doctor_provider_code,
                    "modelName": settings.llm_doctor_model_name,
                },
                "PATIENT_EXPLAIN": {
                    "providerCode": settings.llm_patient_provider_code,
                    "modelName": settings.llm_patient_model_name,
                },
            },
        },
    }
    return success_response(data=data, trace_id="model-version")
