from __future__ import annotations

import time
from typing import Any

from app.core.config import Settings
from app.infra.llm.base_llm_client import BaseLlmClient
from app.repositories.knowledge_repository import KnowledgeRepository
from app.repositories.rag_repository import RagRepository
from app.schemas.base import dump_camel
from app.schemas.rag import RagAnswer, RagGraphEvidence
from app.services.answer_validator_service import AnswerValidatorService
from app.services.citation_assembler import CitationAssembler
from app.services.dense_retriever import DenseRetriever
from app.services.entity_linking_service import EntityLinkingService
from app.services.fusion_service import FusionService
from app.services.graph_retriever import GraphRetriever
from app.services.intent_classifier_service import IntentClassifierService
from app.services.lexical_retriever import LexicalRetriever
from app.services.query_rewrite_service import QueryRewriteService
from app.services.refusal_policy_service import RefusalPolicyService
from app.services.rerank_service import RerankService


class RagOrchestrator:
    def __init__(
        self,
        settings: Settings,
        rag_repository: RagRepository,
        knowledge_repository: KnowledgeRepository,
        llm_client: BaseLlmClient,
        query_rewrite_service: QueryRewriteService,
        intent_classifier_service: IntentClassifierService,
        entity_linking_service: EntityLinkingService,
        lexical_retriever: LexicalRetriever,
        dense_retriever: DenseRetriever,
        graph_retriever: GraphRetriever,
        fusion_service: FusionService,
        rerank_service: RerankService,
        citation_assembler: CitationAssembler,
        refusal_policy_service: RefusalPolicyService,
        answer_validator_service: AnswerValidatorService,
    ) -> None:
        self.settings = settings
        self.rag_repository = rag_repository
        self.knowledge_repository = knowledge_repository
        self.llm_client = llm_client
        self.query_rewrite_service = query_rewrite_service
        self.intent_classifier_service = intent_classifier_service
        self.entity_linking_service = entity_linking_service
        self.lexical_retriever = lexical_retriever
        self.dense_retriever = dense_retriever
        self.graph_retriever = graph_retriever
        self.fusion_service = fusion_service
        self.rerank_service = rerank_service
        self.citation_assembler = citation_assembler
        self.refusal_policy_service = refusal_policy_service
        self.answer_validator_service = answer_validator_service

    def answer(
        self,
        *,
        scene: str,
        question: str,
        kb_code: str | None,
        related_biz_no: str | None,
        patient_uuid: str | None,
        java_user_id: int | None,
        org_id: int | None,
        trace_id: str | None,
        context_text: str | None,
    ) -> dict[str, Any]:
        started = time.perf_counter()
        kb = self.knowledge_repository.get_knowledge_base(kb_code=kb_code or self.settings.rag_default_kb_code)
        if kb is None:
            raise ValueError(f"knowledge base {kb_code or self.settings.rag_default_kb_code} not found")
        rewritten_query = self.query_rewrite_service.rewrite(question)
        intent_code = self.intent_classifier_service.classify(rewritten_query)
        linked_entities = self.entity_linking_service.link(rewritten_query)
        session = self.rag_repository.create_rag_session(
            session_type_code=scene,
            knowledge_version=kb["knowledge_version"],
            model_name=self.settings.llm_model_name,
            related_biz_no=related_biz_no,
            patient_uuid=patient_uuid,
            java_user_id=java_user_id,
            org_id=org_id,
        )
        request_log = self.rag_repository.create_rag_request(
            session_id=session["id"],
            request_type_code=scene,
            user_query=question,
            rewritten_query=rewritten_query,
            top_k=self.settings.answer_evidence_top_k,
            org_id=org_id,
        )
        lexical_hits = self.lexical_retriever.retrieve(kb["kb_code"], rewritten_query, self.settings.lexical_top_k)
        dense_hits = self.dense_retriever.retrieve(kb["kb_code"], rewritten_query, self.settings.dense_top_k)
        graph_hits = self.graph_retriever.retrieve(linked_entities, rewritten_query, self.settings.graph_top_k)
        self.rag_repository.create_retrieval_logs(request_log["id"], lexical_hits + dense_hits, org_id)
        self.rag_repository.create_graph_logs(request_log["id"], graph_hits)
        fused = self.fusion_service.fuse(lexical_hits, dense_hits, graph_hits, self.settings.fusion_top_k)
        self.rag_repository.create_fusion_logs(request_log["id"], fused)
        reranked = self.rerank_service.rerank(rewritten_query, fused, self.settings.rerank_top_k)
        self.rag_repository.create_rerank_logs(request_log["id"], reranked)

        evidence_for_answer = []
        for item in reranked[: self.settings.answer_evidence_top_k]:
            if item["channel"] == "GRAPH":
                evidence_for_answer.append({"chunk_text": item.get("evidence_text") or "", "doc_title": item.get("cypher_template_code")})
            else:
                evidence_for_answer.append(item)
        refusal_reason = self.refusal_policy_service.evaluate(rewritten_query, len(evidence_for_answer))
        if refusal_reason is not None:
            answer_text = self._refusal_text(refusal_reason)
            llm_latency_ms = 0
            llm_status = "SKIPPED"
            self.rag_repository.create_llm_call_log(
                request_id=request_log["id"],
                model_name=self.settings.llm_model_name,
                provider_code=self.settings.llm_provider_code,
                prompt_text=rewritten_query,
                completion_text=answer_text,
                latency_ms=llm_latency_ms,
                status_code=llm_status,
                org_id=org_id,
                error_message=refusal_reason,
            )
        else:
            llm_started = time.perf_counter()
            llm_result = self.llm_client.generate(
                scene=scene,
                query=rewritten_query,
                evidence=evidence_for_answer,
                context_text=context_text,
            )
            llm_latency_ms = int((time.perf_counter() - llm_started) * 1000)
            answer_text = llm_result.answer_text
            llm_status = "SUCCESS"
            self.rag_repository.create_llm_call_log(
                request_id=request_log["id"],
                model_name=self.settings.llm_model_name,
                provider_code=self.settings.llm_provider_code,
                prompt_text=llm_result.prompt_text,
                completion_text=answer_text,
                latency_ms=llm_latency_ms,
                status_code=llm_status,
                org_id=org_id,
            )
        citations = self.citation_assembler.citations(kb, [item for item in reranked if item["channel"] != "GRAPH"])
        retrieved_chunks = self.citation_assembler.retrieved_chunks([item for item in reranked if item["channel"] != "GRAPH"])
        graph_evidence = [
            RagGraphEvidence(
                graph_path_id=item["graph_path_id"],
                cypher_template_code=item.get("cypher_template_code"),
                score=item.get("score", 0.0),
                evidence_text=item.get("evidence_text"),
                result_path_json=item.get("result_path_json"),
                chunk_id=item.get("chunk_id"),
                doc_id=item.get("doc_id"),
            )
            for item in reranked
            if item["channel"] == "GRAPH"
        ]
        safety_flags = self.answer_validator_service.validate(answer_text, dump_camel(citations))
        total_latency_ms = int((time.perf_counter() - started) * 1000)
        confidence = self._confidence(reranked, refusal_reason)
        self.rag_repository.finish_rag_request(
            request_id=request_log["id"],
            answer_text=answer_text,
            status_code="SUCCESS",
            latency_ms=total_latency_ms,
            safety_flag="1" if refusal_reason or "NO_CITATION" in safety_flags else "0",
            refusal_reason=refusal_reason,
            confidence_score=confidence,
            trace_id=trace_id,
        )
        answer = RagAnswer(
            session_no=session["session_no"],
            request_no=request_log["request_no"],
            answer_text=answer_text,
            citations=citations,
            retrieved_chunks=retrieved_chunks,
            graph_evidence=graph_evidence,
            knowledge_base_code=kb["kb_code"],
            knowledge_version=kb["knowledge_version"],
            model_name=self.settings.llm_model_name,
            safety_flag="1" if refusal_reason or "NO_CITATION" in safety_flags else "0",
            safety_flags=safety_flags,
            refusal_reason=refusal_reason,
            confidence=confidence,
            case_context_summary=context_text,
            trace_id=trace_id,
            latency_ms=total_latency_ms,
        )
        payload = dump_camel(answer)
        payload["answer"] = payload["answerText"]
        return payload

    @staticmethod
    def _confidence(reranked: list[dict], refusal_reason: str | None) -> float:
        if refusal_reason is not None or not reranked:
            return 0.0
        total = sum(float(item.get("rerank_score") or 0.0) for item in reranked[:3])
        return round(min(1.0, total / 3), 4)

    @staticmethod
    def _refusal_text(reason: str) -> str:
        mapping = {
            "PROMPT_INJECTION": "The request contains unsafe prompt content and cannot be answered.",
            "PRIVACY_CONCERN": "The request involves sensitive personal information and cannot be answered.",
            "INSUFFICIENT_EVIDENCE": "Published knowledge does not contain enough evidence to answer this question.",
            "HUMAN_REVIEW_REQUIRED": "This question requires clinician review instead of automated advice.",
        }
        return mapping.get(reason, "The request cannot be answered safely.")
