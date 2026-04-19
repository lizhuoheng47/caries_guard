from __future__ import annotations

import uuid
from typing import Any

from sqlalchemy import desc, select, update

from app.core.db import session_scope
from app.core.time_utils import local_naive_now
from app.models.rag import (
    RagEvalDataset,
    RagEvalExpectedCitation,
    RagEvalExpectedGraphPath,
    RagEvalQuestion,
    RagEvalResult,
    RagEvalRun,
)


def _row_to_dict(obj: Any) -> dict[str, Any]:
    return {column.name: getattr(obj, column.name) for column in obj.__table__.columns}


class EvalRepository:
    def ensure_dataset(
        self,
        dataset_code: str,
        dataset_name: str,
        description_text: str | None,
        org_id: int | None,
        created_by: int | None,
    ) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            existing = session.execute(
                select(RagEvalDataset).where(RagEvalDataset.dataset_code == dataset_code)
            ).scalar_one_or_none()
            if existing is not None:
                existing.dataset_name = dataset_name
                existing.description_text = description_text
                existing.updated_at = now
                session.flush()
                return _row_to_dict(existing)
            row = RagEvalDataset(
                dataset_code=dataset_code,
                dataset_name=dataset_name,
                description_text=description_text,
                org_id=org_id,
                created_by=created_by,
                updated_by=created_by,
                created_at=now,
                updated_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def replace_questions(
        self,
        dataset_id: int,
        questions: list[dict[str, Any]],
        org_id: int | None,
        created_by: int | None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            existing = session.execute(
                select(RagEvalQuestion.id).where(RagEvalQuestion.dataset_id == dataset_id)
            ).scalars().all()
            if existing:
                session.query(RagEvalExpectedCitation).filter(RagEvalExpectedCitation.question_id.in_(existing)).delete(
                    synchronize_session=False
                )
                session.query(RagEvalExpectedGraphPath).filter(RagEvalExpectedGraphPath.question_id.in_(existing)).delete(
                    synchronize_session=False
                )
                session.query(RagEvalQuestion).filter(RagEvalQuestion.dataset_id == dataset_id).delete(
                    synchronize_session=False
                )
            for item in questions:
                row = RagEvalQuestion(
                    dataset_id=dataset_id,
                    question_no=item["question_no"],
                    scene_code=item["scene_code"],
                    question_text=item["question_text"],
                    case_context_json=item.get("case_context_json"),
                    expected_refusal_flag=item.get("expected_refusal_flag", "0"),
                    expected_risk_level=item.get("expected_risk_level"),
                    org_id=org_id,
                    created_by=created_by,
                    updated_by=created_by,
                    created_at=now,
                    updated_at=now,
                )
                session.add(row)
                session.flush()
                for citation in item.get("expected_citations", []):
                    session.add(
                        RagEvalExpectedCitation(
                            question_id=row.id,
                            expected_doc_no=citation.get("expected_doc_no"),
                            expected_chunk_id=citation.get("expected_chunk_id"),
                            created_at=now,
                        )
                    )
                for graph_path in item.get("expected_graph_paths", []):
                    session.add(
                        RagEvalExpectedGraphPath(
                            question_id=row.id,
                            path_signature=graph_path["path_signature"],
                            created_at=now,
                        )
                    )

    def list_questions(self, dataset_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            questions = session.execute(
                select(RagEvalQuestion)
                .where(RagEvalQuestion.dataset_id == dataset_id)
                .order_by(RagEvalQuestion.id)
            ).scalars().all()
            question_ids = [question.id for question in questions]
            citation_rows = session.execute(
                select(RagEvalExpectedCitation).where(RagEvalExpectedCitation.question_id.in_(question_ids))
            ).scalars().all() if question_ids else []
            graph_rows = session.execute(
                select(RagEvalExpectedGraphPath).where(RagEvalExpectedGraphPath.question_id.in_(question_ids))
            ).scalars().all() if question_ids else []
            citations_by_question: dict[int, list[dict[str, Any]]] = {}
            graphs_by_question: dict[int, list[dict[str, Any]]] = {}
            for row in citation_rows:
                citations_by_question.setdefault(row.question_id, []).append(_row_to_dict(row))
            for row in graph_rows:
                graphs_by_question.setdefault(row.question_id, []).append(_row_to_dict(row))
            result = []
            for question in questions:
                payload = _row_to_dict(question)
                payload["expected_citations"] = citations_by_question.get(question.id, [])
                payload["expected_graph_paths"] = graphs_by_question.get(question.id, [])
                result.append(payload)
            return result

    def list_datasets(self) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(select(RagEvalDataset).order_by(RagEvalDataset.id)).scalars().all()
            return [_row_to_dict(row) for row in rows]

    def get_dataset(self, dataset_id: int) -> dict[str, Any] | None:
        with session_scope() as session:
            row = session.execute(
                select(RagEvalDataset).where(RagEvalDataset.id == dataset_id)
            ).scalar_one_or_none()
            return _row_to_dict(row) if row else None

    def create_run(self, dataset_id: int, org_id: int | None, created_by: int | None) -> dict[str, Any]:
        now = local_naive_now()
        with session_scope() as session:
            row = RagEvalRun(
                run_no=f"EVAL-{uuid.uuid4().hex[:16].upper()}",
                dataset_id=dataset_id,
                org_id=org_id,
                created_by=created_by,
                started_at=now,
                created_at=now,
            )
            session.add(row)
            session.flush()
            return _row_to_dict(row)

    def save_result(
        self,
        run_id: int,
        question_id: int,
        request_no: str | None,
        answer_text: str | None,
        citation_hit_flag: str,
        graph_hit_flag: str,
        refusal_hit_flag: str,
        hallucination_flag: str,
        latency_ms: int | None,
        metric_json: dict[str, Any] | None,
    ) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.add(
                RagEvalResult(
                    run_id=run_id,
                    question_id=question_id,
                    request_no=request_no,
                    answer_text=answer_text,
                    citation_hit_flag=citation_hit_flag,
                    graph_hit_flag=graph_hit_flag,
                    refusal_hit_flag=refusal_hit_flag,
                    hallucination_flag=hallucination_flag,
                    latency_ms=latency_ms,
                    metric_json=metric_json,
                    created_at=now,
                )
            )

    def finish_run(self, run_id: int, status_code: str, metric_json: dict[str, Any]) -> None:
        now = local_naive_now()
        with session_scope() as session:
            session.execute(
                update(RagEvalRun)
                .where(RagEvalRun.id == run_id)
                .values(run_status_code=status_code, metric_json=metric_json, finished_at=now)
            )

    def list_runs(self) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(RagEvalRun, RagEvalDataset)
                .join(RagEvalDataset, RagEvalDataset.id == RagEvalRun.dataset_id)
                .order_by(desc(RagEvalRun.created_at))
            ).all()
            result: list[dict[str, Any]] = []
            for run, dataset in rows:
                payload = _row_to_dict(run)
                payload["dataset_code"] = dataset.dataset_code
                payload["dataset_name"] = dataset.dataset_name
                result.append(payload)
            return result

    def get_run(self, run_no: str) -> dict[str, Any] | None:
        with session_scope() as session:
            row = session.execute(
                select(RagEvalRun, RagEvalDataset)
                .join(RagEvalDataset, RagEvalDataset.id == RagEvalRun.dataset_id)
                .where(RagEvalRun.run_no == run_no)
            ).one_or_none()
            if row is None:
                return None
            run, dataset = row
            payload = _row_to_dict(run)
            payload["dataset_code"] = dataset.dataset_code
            payload["dataset_name"] = dataset.dataset_name
            return payload

    def list_results(self, run_id: int) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.execute(
                select(RagEvalResult, RagEvalQuestion)
                .join(RagEvalQuestion, RagEvalQuestion.id == RagEvalResult.question_id)
                .where(RagEvalResult.run_id == run_id)
                .order_by(RagEvalResult.question_id)
            ).all()
            result: list[dict[str, Any]] = []
            for eval_result, question in rows:
                payload = _row_to_dict(eval_result)
                payload["question_no"] = question.question_no
                payload["scene_code"] = question.scene_code
                payload["question_text"] = question.question_text
                payload["case_context_json"] = question.case_context_json
                payload["expected_refusal_flag"] = question.expected_refusal_flag
                payload["expected_risk_level"] = question.expected_risk_level
                result.append(payload)
            return result
