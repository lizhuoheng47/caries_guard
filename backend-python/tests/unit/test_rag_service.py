from pathlib import Path

from app.core.config import Settings
from app.infra.llm.template_llm_client import TemplateLlmClient
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.repositories.metadata_repository import MetadataRepository
from app.schemas.rag import DoctorQaRequest, KnowledgeDocumentRequest, KnowledgeRebuildRequest
from app.services.knowledge_service import KnowledgeService
from app.services.rag_service import RagService


def _services(tmp_path: Path) -> tuple[KnowledgeService, RagService, MetadataRepository]:
    settings = Settings(
        metadata_db_path=str(tmp_path / "metadata.sqlite3"),
        rag_index_dir=str(tmp_path / "index"),
        rag_default_kb_code="test-kb",
        rag_knowledge_version="v-test",
        rag_top_k=3,
    )
    repository = MetadataRepository(settings)
    vector_store = SimpleVectorStore()
    knowledge_service = KnowledgeService(settings, repository, vector_store)
    rag_service = RagService(settings, repository, vector_store, TemplateLlmClient(), knowledge_service)
    return knowledge_service, rag_service, repository


def test_rag_document_rebuild_and_doctor_qa_logs_sources(tmp_path: Path) -> None:
    knowledge_service, rag_service, repository = _services(tmp_path)
    imported = knowledge_service.import_document(
        KnowledgeDocumentRequest(
            doc_title="儿童龋齿护理建议",
            doc_source_code="MANUAL",
            source_uri="internal://guide/caries-care",
            content_text="儿童龋齿风险较高时，应控制含糖饮食，坚持刷牙并使用含氟牙膏，按医生建议复查。",
            review_status_code="APPROVED",
        )
    )

    rebuilt = knowledge_service.rebuild(KnowledgeRebuildRequest(kb_code="test-kb"))
    answer = rag_service.doctor_qa(DoctorQaRequest(question="儿童龋齿高风险如何护理？", kb_code="test-kb"))

    assert imported["docNo"].startswith("DOC-")
    assert rebuilt["chunkCount"] == 1
    assert rebuilt["rebuildStatusCode"] == "SUCCESS"
    assert answer["citations"]
    assert answer["citations"][0]["docTitle"] == "儿童龋齿护理建议"
    assert "依据" in answer["answerText"]

    with repository._connect() as conn:
        request_count = conn.execute("SELECT COUNT(*) FROM rag_request_log").fetchone()[0]
        retrieval_count = conn.execute("SELECT COUNT(*) FROM rag_retrieval_log").fetchone()[0]
        llm_count = conn.execute("SELECT COUNT(*) FROM llm_call_log").fetchone()[0]

    assert request_count == 1
    assert retrieval_count == 1
    assert llm_count == 1


def test_rag_returns_safe_answer_when_index_has_no_hits(tmp_path: Path) -> None:
    knowledge_service, rag_service, _repository = _services(tmp_path)
    knowledge_service.ensure_knowledge_base(kb_code="empty-kb")

    answer = rag_service.doctor_qa(DoctorQaRequest(question="没有资料时怎么回答？", kb_code="empty-kb"))

    assert answer["citations"] == []
    assert answer["safetyFlag"] == "1"
    assert "没有检索到足够依据" in answer["answerText"]
