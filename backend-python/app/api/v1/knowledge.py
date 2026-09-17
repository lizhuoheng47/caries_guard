from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response
from app.schemas.rag import KnowledgeSearchRequest

router = APIRouter(prefix="/knowledge", tags=["knowledge"])


@router.get("/status")
def knowledge_status() -> dict:
    return success_response(get_container().knowledge_base_service.status())


@router.post("/search")
def search_knowledge(request: KnowledgeSearchRequest) -> dict:
    container = get_container()
    hits = container.knowledge_base_service.search(request.query, request.top_k)
    return success_response(
        {
            "knowledgeVersion": container.knowledge_base_service.version,
            "citations": [hit.to_citation(index) for index, hit in enumerate(hits, start=1)],
        }
    )


@router.post("/reload")
def reload_knowledge() -> dict:
    return success_response(get_container().knowledge_base_service.reload())
