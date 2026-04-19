from app.services.eval_service import EvalService
from app.services.rag_log_service import RagLogService


class FakeRagRepository:
    def list_requests(self, org_id=None):
        return [{"id": 1, "request_no": "REQ-1", "org_id": org_id}]

    def get_request_by_no(self, request_no: str):
        if request_no != "REQ-1":
            return None
        return {"id": 1, "request_no": request_no}

    def list_retrieval_logs(self, request_id: int):
        return [{"request_id": request_id, "rank_no": 1, "retrieval_channel_code": "LEXICAL"}]

    def list_fusion_logs(self, request_id: int):
        return [{"request_id": request_id, "final_rank": 1, "candidate_id": "chunk:1"}]

    def list_rerank_logs(self, request_id: int):
        return [{"request_id": request_id, "final_rank": 1, "candidate_id": "chunk:1"}]

    def list_graph_logs(self, request_id: int):
        return [{"request_id": request_id, "cypher_template_code": "RISK_TO_RECOMMEND"}]

    def list_llm_call_logs(self, request_id: int):
        return [{"request_id": request_id, "model_name": "gpt-4o-mini"}]


class FakeEvalRepository:
    def list_runs(self):
        return [{"id": 10, "run_no": "RUN-1", "dataset_name": "Default Dataset"}]

    def list_datasets(self):
        return [{"id": 1, "dataset_code": "DATASET-1", "dataset_name": "Default Dataset"}]

    def get_dataset(self, dataset_id: int):
        if dataset_id != 1:
            return None
        return {"id": 1, "dataset_code": "DATASET-1", "dataset_name": "Default Dataset"}

    def list_questions(self, dataset_id: int):
        return [{"id": 101, "question_no": "Q-1", "question_text": "How often to review?"}]

    def get_run(self, run_no: str):
        if run_no != "RUN-1":
            return None
        return {"id": 10, "run_no": "RUN-1", "dataset_code": "DATASET-1", "dataset_name": "Default Dataset"}

    def list_results(self, run_id: int):
        return [{"run_id": run_id, "question_no": "Q-1", "citation_hit_flag": "1"}]


def test_rag_log_service_exposes_split_detail_views() -> None:
    service = RagLogService(FakeRagRepository())

    detail = service.request_detail("REQ-1")

    assert detail["request_no"] == "REQ-1"
    assert detail["retrievalLogs"][0]["retrieval_channel_code"] == "LEXICAL"
    assert service.fusion_logs("REQ-1")[0]["candidate_id"] == "chunk:1"
    assert service.rerank_logs("REQ-1")[0]["candidate_id"] == "chunk:1"
    assert service.graph_logs("REQ-1")[0]["cypher_template_code"] == "RISK_TO_RECOMMEND"
    assert service.llm_call_logs("REQ-1")[0]["model_name"] == "gpt-4o-mini"


def test_eval_service_exposes_dataset_and_run_details() -> None:
    service = EvalService(FakeEvalRepository(), None)  # type: ignore[arg-type]

    dataset = service.dataset_detail(1)
    run = service.run_detail("RUN-1")

    assert dataset["dataset_code"] == "DATASET-1"
    assert dataset["questions"][0]["question_no"] == "Q-1"
    assert run["run_no"] == "RUN-1"
    assert run["results"][0]["citation_hit_flag"] == "1"
    assert service.run_results("RUN-1")[0]["question_no"] == "Q-1"
