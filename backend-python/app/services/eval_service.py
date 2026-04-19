from __future__ import annotations

from app.repositories.eval_repository import EvalRepository
from app.schemas.rag import RagAskRequest
from app.services.rag_service import RagService


class EvalService:
    def __init__(self, eval_repository: EvalRepository, rag_service: RagService) -> None:
        self.eval_repository = eval_repository
        self.rag_service = rag_service

    def list_runs(self) -> list[dict]:
        return self.eval_repository.list_runs()

    def run_dataset(self, dataset_id: int, org_id: int | None, created_by: int | None) -> dict:
        run = self.eval_repository.create_run(dataset_id, org_id, created_by)
        questions = self.eval_repository.list_questions(dataset_id)
        citation_hits = 0
        citation_coverage_total = 0.0
        graph_hits = 0
        grounded_hits = 0
        refusal_hits = 0
        latency_total = 0
        total = len(questions)
        for question in questions:
            answer = self.rag_service.ask(
                RagAskRequest(
                    question=question["question_text"],
                    scene=question["scene_code"],
                    case_context=question.get("case_context_json"),
                    org_id=org_id,
                    include_debug=True,
                )
            )
            actual_citations = answer.get("citations", [])
            actual_doc_codes = {item.get("documentCode") for item in actual_citations if item.get("documentCode")}
            expected_citations = {
                item.get("expected_doc_no")
                for item in question.get("expected_citations", [])
                if item.get("expected_doc_no")
            }
            citation_hit = "1" if not expected_citations or bool(actual_doc_codes & expected_citations) else "0"
            citation_hits += 1 if citation_hit == "1" else 0
            if expected_citations:
                citation_coverage_total += len(actual_doc_codes & expected_citations) / len(expected_citations)
            else:
                citation_coverage_total += 1.0
            actual_graph_codes = {item.get("cypherTemplateCode") for item in answer.get("graphEvidence", [])}
            expected_graph_codes = {item.get("path_signature") for item in question.get("expected_graph_paths", [])}
            graph_hit = "1" if not expected_graph_codes or bool(actual_graph_codes & expected_graph_codes) else "0"
            graph_hits += 1 if graph_hit == "1" else 0
            refusal_hit = "1" if (question.get("expected_refusal_flag") == "1") == bool(answer.get("refusalReason")) else "0"
            refusal_hits += 1 if refusal_hit == "1" else 0
            grounded = "1" if not any(flag in (answer.get("safetyFlags") or []) for flag in ("NO_CITATION", "NO_PROVENANCE")) else "0"
            grounded_hits += 1 if grounded == "1" else 0
            latency_ms = int(answer.get("latencyMs") or 0)
            latency_total += latency_ms
            self.eval_repository.save_result(
                run_id=run["id"],
                question_id=question["id"],
                request_no=answer.get("requestNo"),
                answer_text=answer.get("answer"),
                citation_hit_flag=citation_hit,
                graph_hit_flag=graph_hit,
                refusal_hit_flag=refusal_hit,
                hallucination_flag="0" if grounded == "1" else "1",
                latency_ms=latency_ms,
                metric_json={
                    "citationHit": citation_hit == "1",
                    "citationCoverage": 1.0 if not expected_citations else len(actual_doc_codes & expected_citations) / len(expected_citations),
                    "graphHit": graph_hit == "1",
                    "refusalHit": refusal_hit == "1",
                    "grounded": grounded == "1",
                    "evidenceCount": len(answer.get("evidence", [])),
                },
            )
        metrics = {
            "questionCount": total,
            "citationAccuracy": round(citation_hits / total, 4) if total else 0.0,
            "citationCoverage": round(citation_coverage_total / total, 4) if total else 0.0,
            "graphPathHitRate": round(graph_hits / total, 4) if total else 0.0,
            "refusalPrecision": round(refusal_hits / total, 4) if total else 0.0,
            "groundednessRate": round(grounded_hits / total, 4) if total else 0.0,
            "avgLatencyMs": round(latency_total / total, 2) if total else 0.0,
        }
        self.eval_repository.finish_run(run["id"], "SUCCESS", metrics)
        return {"runNo": run["run_no"], "metrics": metrics}
