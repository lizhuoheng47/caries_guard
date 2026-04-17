from fastapi import APIRouter

from app.container import get_container
from app.schemas.common import success_response
from app.schemas.rag import KnowledgeDocumentRequest, KnowledgeRebuildRequest

router = APIRouter(tags=["knowledge"])


@router.post("/knowledge/documents")
def import_document(request: KnowledgeDocumentRequest) -> dict:
    container = get_container()
    result = container.knowledge_service.import_document(request)
    return success_response(data=result)


@router.post("/knowledge/rebuild")
def rebuild_knowledge(request: KnowledgeRebuildRequest) -> dict:
    container = get_container()
    result = container.knowledge_service.rebuild(request)
    return success_response(data=result)
