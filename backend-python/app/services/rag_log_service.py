from __future__ import annotations

from app.repositories.rag_repository import RagRepository


class RagLogService:
    def __init__(self, repository: RagRepository) -> None:
        self.repository = repository

    def list_requests(self, org_id: int | None = None) -> list[dict]:
        return self.repository.list_requests(org_id)

    def request_detail(self, request_no: str) -> dict:
        request = self.repository.get_request_by_no(request_no)
        if request is None:
            raise ValueError(f"request {request_no} not found")
        request_id = request["id"]
        request["retrievalLogs"] = self.repository.list_retrieval_logs(request_id)
        request["fusionLogs"] = self.repository.list_fusion_logs(request_id)
        request["rerankLogs"] = self.repository.list_rerank_logs(request_id)
        request["graphLogs"] = self.repository.list_graph_logs(request_id)
        request["llmCallLogs"] = self.repository.list_llm_call_logs(request_id)
        return request

    def retrieval_logs(self, request_no: str) -> list[dict]:
        request = self._request_or_raise(request_no)
        return self.repository.list_retrieval_logs(request["id"])

    def fusion_logs(self, request_no: str) -> list[dict]:
        request = self._request_or_raise(request_no)
        return self.repository.list_fusion_logs(request["id"])

    def rerank_logs(self, request_no: str) -> list[dict]:
        request = self._request_or_raise(request_no)
        return self.repository.list_rerank_logs(request["id"])

    def graph_logs(self, request_no: str) -> list[dict]:
        request = self._request_or_raise(request_no)
        return self.repository.list_graph_logs(request["id"])

    def llm_call_logs(self, request_no: str) -> list[dict]:
        request = self._request_or_raise(request_no)
        return self.repository.list_llm_call_logs(request["id"])

    def _request_or_raise(self, request_no: str) -> dict:
        request = self.repository.get_request_by_no(request_no)
        if request is None:
            raise ValueError(f"request {request_no} not found")
        return request
