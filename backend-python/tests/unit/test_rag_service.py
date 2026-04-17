from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.infra.llm.template_llm_client import TemplateLlmClient
from app.infra.vector.simple_vector_store import SimpleVectorStore
from app.schemas.rag import DoctorQaRequest, KnowledgeDocumentRequest, KnowledgeRebuildRequest
from app.services.knowledge_service import KnowledgeService
from app.services.rag_service import RagService


class FakeMetadataRepository:
    def __init__(self) -> None:
        self._next_id = 1
        self.kbs: dict[str, dict[str, Any]] = {}
        self.documents: list[dict[str, Any]] = []
        self.chunks: list[dict[str, Any]] = []
        self.rebuild_jobs: list[dict[str, Any]] = []
        self.sessions: list[dict[str, Any]] = []
        self.requests: list[dict[str, Any]] = []
        self.retrieval_logs: list[dict[str, Any]] = []
        self.llm_logs: list[dict[str, Any]] = []

    def ensure_knowledge_base(self, **kwargs) -> dict[str, Any]:
        kb_code = kwargs["kb_code"]
        existing = self.kbs.get(kb_code)
        if existing:
            existing.update(kwargs)
            return existing
        kb = {"id": self._id(), **kwargs}
        self.kbs[kb_code] = kb
        return kb

    def get_knowledge_base(self, kb_code: str) -> dict[str, Any] | None:
        return self.kbs.get(kb_code)

    def create_document(self, **kwargs) -> dict[str, Any]:
        document = {
            "id": self._id(),
            **kwargs,
            "doc_no": kwargs.get("doc_no") or f"DOC-{self._next_id}",
            "doc_title": kwargs["doc_title"],
            "review_status_code": kwargs["review_status_code"],
        }
        self.documents.append(document)
        return document

    def list_approved_documents(self, kb_id: int) -> list[dict[str, Any]]:
        return [
            item
            for item in self.documents
            if item["kb_id"] == kb_id and item["review_status_code"] == "APPROVED"
        ]

    def replace_chunks(self, kb_id: int, chunks: list[dict[str, Any]], embedding_model: str, vector_store_path: str) -> list[dict[str, Any]]:
        self.chunks = [item for item in self.chunks if item["kb_id"] != kb_id]
        stored: list[dict[str, Any]] = []
        documents = {item["id"]: item for item in self.documents}
        for item in chunks:
            document = documents[item["doc_id"]]
            chunk = {
                "id": self._id(),
                "kb_id": kb_id,
                "embedding_model": embedding_model,
                "vector_store_path": vector_store_path,
                "doc_title": document["doc_title"],
                "doc_no": document["doc_no"],
                "source_uri": document.get("source_uri"),
                "doc_source_code": document.get("doc_source_code"),
                **item,
            }
            self.chunks.append(chunk)
            stored.append(chunk)
        return stored

    def create_rebuild_job(self, kb_id: int, knowledge_version: str, vector_store_path: str, org_id: int | None) -> dict[str, Any]:
        job = {
            "id": self._id(),
            "rebuild_job_no": f"KBREBUILD-{self._next_id}",
            "kb_id": kb_id,
            "knowledge_version": knowledge_version,
            "vector_store_path": vector_store_path,
            "org_id": org_id,
        }
        self.rebuild_jobs.append(job)
        return job

    def finish_rebuild_job(self, job_id: int, status_code: str, chunk_count: int, error_message: str | None = None) -> dict[str, Any]:
        job = next(item for item in self.rebuild_jobs if item["id"] == job_id)
        job.update({"rebuild_status_code": status_code, "chunk_count": chunk_count, "error_message": error_message})
        return job

    def create_rag_session(self, **kwargs) -> dict[str, Any]:
        session = {"id": self._id(), "session_no": f"RAG-{self._next_id}", **kwargs}
        self.sessions.append(session)
        return session

    def create_rag_request(self, **kwargs) -> dict[str, Any]:
        request = {"id": self._id(), "request_no": f"RAGREQ-{self._next_id}", **kwargs}
        self.requests.append(request)
        return request

    def finish_rag_request(self, request_id: int, answer_text: str, status_code: str, latency_ms: int, safety_flag: str = "0") -> None:
        request = next(item for item in self.requests if item["id"] == request_id)
        request.update({"answer_text": answer_text, "request_status_code": status_code, "latency_ms": latency_ms, "safety_flag": safety_flag})

    def create_retrieval_logs(self, request_id: int, hits: list[dict[str, Any]], org_id: int | None) -> None:
        for rank, hit in enumerate(hits, start=1):
            self.retrieval_logs.append({"request_id": request_id, "rank_no": rank, **hit})

    def create_llm_call_log(self, **kwargs) -> None:
        self.llm_logs.append(kwargs)

    def _id(self) -> int:
        value = self._next_id
        self._next_id += 1
        return value


def _services(tmp_path: Path) -> tuple[KnowledgeService, RagService, FakeMetadataRepository]:
    settings = Settings(
        rag_index_dir=str(tmp_path / "index"),
        rag_default_kb_code="test-kb",
        rag_knowledge_version="v-test",
        rag_top_k=3,
    )
    repository = FakeMetadataRepository()
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

    assert len(repository.requests) == 1
    assert len(repository.retrieval_logs) == 1
    assert len(repository.llm_logs) == 1


def test_rag_returns_safe_answer_when_index_has_no_hits(tmp_path: Path) -> None:
    knowledge_service, rag_service, _repository = _services(tmp_path)
    knowledge_service.ensure_knowledge_base(kb_code="empty-kb")

    answer = rag_service.doctor_qa(DoctorQaRequest(question="没有资料时怎么回答？", kb_code="empty-kb"))

    assert answer["citations"] == []
    assert answer["safetyFlag"] == "1"
    assert "没有检索到足够依据" in answer["answerText"]
