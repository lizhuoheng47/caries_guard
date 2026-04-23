from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
import time
from typing import Any

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.llm.base_llm_client import BaseLlmClient
from app.repositories.knowledge_repository import KnowledgeRepository
from app.repositories.rag_repository import RagRepository
from app.schemas.base import dump_camel
from app.schemas.rag import RagAnswer, RagDebugMeta
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

log = get_logger("cariesguard-ai.rag.orchestrator")


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
        top_k: int | None,
        kb_code: str | None,
        related_biz_no: str | None,
        patient_uuid: str | None,
        java_user_id: int | None,
        org_id: int | None,
        trace_id: str | None,
        context_text: str | None,
        include_debug: bool = False,
    ) -> dict[str, Any]:
        if self.settings.ai_runtime_mode == "real":
            if self.settings.llm_provider_code == "MOCK":
                raise RuntimeError("CG_AI_RUNTIME_MODE='real' requires a real LLM provider")
            if self.settings.rag_embedding_provider == "HASHING":
                raise RuntimeError("CG_AI_RUNTIME_MODE='real' requires a real Embedding provider")

        started = time.perf_counter()
        answer_top_k = self._resolve_answer_top_k(top_k)
        retrieval_top_k = max(answer_top_k, answer_top_k * 2)
        lexical_top_k = min(self.settings.lexical_top_k, retrieval_top_k)
        dense_top_k = min(self.settings.dense_top_k, retrieval_top_k)
        graph_top_k = min(self.settings.graph_top_k, retrieval_top_k)
        fusion_top_k = min(self.settings.fusion_top_k, retrieval_top_k)
        rerank_top_k = min(self.settings.rerank_top_k, retrieval_top_k)
        kb = self.knowledge_repository.get_knowledge_base(kb_code=kb_code or self.settings.rag_default_kb_code)
        if kb is None:
            raise ValueError(f"knowledge base {kb_code or self.settings.rag_default_kb_code} not found")
        rewritten_query = self.query_rewrite_service.rewrite(question)
        intent_code = self.intent_classifier_service.classify(rewritten_query)
        linked_entities = self.entity_linking_service.link(rewritten_query)
        llm_profile = self._resolve_llm_profile(scene)
        llm_provider_code = llm_profile["providerCode"]
        llm_model_name = llm_profile["modelName"]
        session = self.rag_repository.create_rag_session(
            session_type_code=scene,
            knowledge_version=kb["knowledge_version"],
            model_name=llm_model_name,
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
            top_k=answer_top_k,
            org_id=org_id,
        )
        lexical_hits, dense_hits, graph_hits, retrieval_meta = self._retrieve_channels(
            kb_code=kb["kb_code"],
            rewritten_query=rewritten_query,
            linked_entities=linked_entities,
            lexical_top_k=lexical_top_k,
            dense_top_k=dense_top_k,
            graph_top_k=graph_top_k,
        )
        channel_hits = retrieval_meta["channelHits"]

        self.rag_repository.create_retrieval_logs(request_log["id"], lexical_hits + dense_hits, org_id)
        self.rag_repository.create_graph_logs(request_log["id"], graph_hits)
        fused = self.fusion_service.fuse(lexical_hits, dense_hits, graph_hits, fusion_top_k)
        self.rag_repository.create_fusion_logs(request_log["id"], fused)
        reranked = self.rerank_service.rerank(rewritten_query, fused, rerank_top_k)
        self.rag_repository.create_rerank_logs(request_log["id"], reranked)

        evidence_bundle = self.citation_assembler.evidence(reranked)
        evidence_for_answer = [
            {
                "chunk_text": item.chunk_text or item.evidence_text or "",
                "doc_title": item.doc_title,
                "channel": item.channel,
                "evidence_type": item.evidence_type,
            }
            for item in evidence_bundle[: answer_top_k]
        ]
        distinct_doc_count = len({item.doc_id for item in evidence_bundle if item.doc_id is not None})
        evidence_sufficient = self._evidence_sufficient(evidence_bundle)
        refusal_reason = self.refusal_policy_service.evaluate(
            rewritten_query,
            len(evidence_bundle),
            distinct_doc_count=distinct_doc_count,
            evidence_sufficient=evidence_sufficient,
        )
        
        llm_metadata = {}
        if refusal_reason is not None:
            answer_text = self._refusal_text(refusal_reason)
            llm_latency_ms = 0
            llm_status = "SKIPPED"
            llm_metadata = {
                "provider": llm_provider_code,
                "model": llm_model_name,
                "usage": None,
                "finishReason": "SKIPPED",
            }
            self.rag_repository.create_llm_call_log(
                request_id=request_log["id"],
                model_name=llm_model_name,
                provider_code=llm_provider_code,
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
            llm_provider_code = str(llm_result.provider or llm_provider_code)
            llm_model_name = str(llm_result.model or llm_model_name)
            llm_metadata = {
                "provider": llm_provider_code,
                "model": llm_model_name,
                "usage": llm_result.usage,
                "finishReason": llm_result.finish_reason
            }
            self.rag_repository.create_llm_call_log(
                request_id=request_log["id"],
                model_name=llm_model_name,
                provider_code=llm_provider_code,
                prompt_text=llm_result.prompt_text,
                completion_text=answer_text,
                latency_ms=llm_latency_ms,
                status_code=llm_status,
                org_id=org_id,
            )
        citations = self.citation_assembler.citations(kb, reranked[: answer_top_k])
        retrieved_chunks = self.citation_assembler.retrieved_chunks(reranked[: answer_top_k])
        graph_evidence = self.citation_assembler.graph_evidence(reranked[: answer_top_k])
        safety_flags = self.answer_validator_service.validate(
            answer_text,
            [dump_camel(item) for item in citations],
            [dump_camel(item) for item in evidence_bundle],
        )
        if refusal_reason and refusal_reason not in safety_flags:
            safety_flags.append(refusal_reason)
        if context_text and "[REDACTED_" in context_text and "SENSITIVE_INPUT_REDACTED" not in safety_flags:
            safety_flags.append("SENSITIVE_INPUT_REDACTED")
        total_latency_ms = int((time.perf_counter() - started) * 1000)
        confidence = self._confidence(reranked, evidence_bundle, refusal_reason, safety_flags)
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
        
        debug = None
        if include_debug:
            debug = RagDebugMeta(
                rewritten_query=rewritten_query,
                intent_code=intent_code,
                linked_entities=linked_entities,
                lexical_hit_count=len(lexical_hits),
                dense_hit_count=len(dense_hits),
                graph_hit_count=len(graph_hits),
                evidence_sufficient=evidence_sufficient,
                rerank_provider=self.settings.rerank_provider,
                rerank_model=self.settings.rerank_model_name,
                channel_hit_summary=channel_hits,
                provider_summary={
                    "llm": llm_provider_code,
                    "llmModel": llm_model_name,
                    "embedding": self.settings.rag_embedding_provider,
                    "vectorStore": self.settings.rag_vector_store_type
                },
                index_summary=self.settings.opensearch_chunk_index,
                graph_used=len(graph_hits) > 0,
                graph_fallback_used=intent_code == "GRAPH_PREFERRED" and len(graph_hits) == 0
            )
        answer = RagAnswer(
            session_no=session["session_no"],
            request_no=request_log["request_no"],
            answer_text=answer_text,
            citations=citations,
            evidence=evidence_bundle[: answer_top_k],
            retrieved_chunks=retrieved_chunks,
            graph_evidence=graph_evidence,
            knowledge_base_code=kb["kb_code"],
            knowledge_version=kb["knowledge_version"],
            model_name=llm_model_name,
            safety_flag="1" if refusal_reason or "NO_CITATION" in safety_flags else "0",
            safety_flags=safety_flags,
            refusal_reason=refusal_reason,
            confidence=confidence,
            case_context_summary=context_text,
            trace_id=trace_id,
            latency_ms=total_latency_ms,
            retrieval_channel_summary=channel_hits,
            evidence_sufficient=evidence_sufficient,
            distinct_document_count=distinct_doc_count,
            debug=debug,
        )
        payload = dump_camel(answer)
        payload["answer"] = payload["answerText"]
        if llm_metadata:
            payload["llmTelemetry"] = llm_metadata
        payload["retrievalTelemetry"] = retrieval_meta
        return payload

    def _resolve_llm_profile(self, scene: str) -> dict[str, str]:
        resolver = getattr(self.llm_client, "resolve_profile", None)
        if callable(resolver):
            resolved = resolver(scene)
            if isinstance(resolved, dict):
                provider = str(resolved.get("providerCode") or self.settings.llm_provider_code)
                model = str(resolved.get("modelName") or self.settings.llm_model_name)
                base_url = str(resolved.get("baseUrl") or self.settings.llm_base_url)
                api_key = str(resolved.get("apiKey") or self.settings.llm_api_key)
                return {
                    "providerCode": provider,
                    "modelName": model,
                    "baseUrl": base_url,
                    "apiKey": api_key,
                }
        return {
            "providerCode": self.settings.llm_provider_code,
            "modelName": self.settings.llm_model_name,
            "baseUrl": self.settings.llm_base_url,
            "apiKey": self.settings.llm_api_key,
        }

    def _resolve_answer_top_k(self, top_k: int | None) -> int:
        default_top_k = max(1, int(self.settings.answer_evidence_top_k))
        if top_k is None:
            return default_top_k
        requested = max(1, int(top_k))
        return min(requested, default_top_k, 20)

    def _retrieve_channels(
        self,
        *,
        kb_code: str,
        rewritten_query: str,
        linked_entities: list[dict[str, Any]],
        lexical_top_k: int,
        dense_top_k: int,
        graph_top_k: int,
    ) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]], dict[str, Any]]:
        channel_hits: dict[str, int] = {"LEXICAL": 0, "DENSE": 0, "GRAPH": 0}
        channel_latency_ms: dict[str, int] = {"LEXICAL": 0, "DENSE": 0, "GRAPH": 0}
        channel_errors: dict[str, str] = {}
        retrieval_started = time.perf_counter()
        futures: dict[str, Any] = {}
        with ThreadPoolExecutor(max_workers=3) as executor:
            futures["LEXICAL"] = executor.submit(
                self._timed_retrieve_call,
                self.lexical_retriever.retrieve,
                kb_code,
                rewritten_query,
                lexical_top_k,
            )
            futures["DENSE"] = executor.submit(
                self._timed_retrieve_call,
                self.dense_retriever.retrieve,
                kb_code,
                rewritten_query,
                dense_top_k,
            )
            futures["GRAPH"] = executor.submit(
                self._timed_retrieve_call,
                self.graph_retriever.retrieve,
                linked_entities,
                rewritten_query,
                graph_top_k,
            )

            lexical_hits: list[dict[str, Any]] = []
            dense_hits: list[dict[str, Any]] = []
            graph_hits: list[dict[str, Any]] = []

            for channel, future in futures.items():
                try:
                    hits, latency_ms = future.result()
                    channel_latency_ms[channel] = latency_ms
                    channel_hits[channel] = len(hits)
                    if channel == "LEXICAL":
                        lexical_hits = hits
                    elif channel == "DENSE":
                        dense_hits = hits
                    else:
                        graph_hits = hits
                except Exception as exc:
                    channel_errors[channel] = str(exc)
                    if self.settings.ai_runtime_mode == "real":
                        raise RuntimeError(f"{channel} retrieval failed: {exc}") from exc
                    log.warning("rag retrieval channel failed channel=%s error=%s", channel, exc)

        total_retrieval_ms = int((time.perf_counter() - retrieval_started) * 1000)
        retrieval_meta = {
            "channelHits": channel_hits,
            "channelLatencyMs": channel_latency_ms,
            "channelErrors": channel_errors,
            "totalRetrievalMs": total_retrieval_ms,
            "dataSources": self._data_source_summary(),
        }
        return lexical_hits, dense_hits, graph_hits, retrieval_meta

    @staticmethod
    def _timed_retrieve_call(retrieve_fn: Any, *args: Any) -> tuple[list[dict[str, Any]], int]:
        started = time.perf_counter()
        hits = retrieve_fn(*args)
        latency_ms = int((time.perf_counter() - started) * 1000)
        return hits, latency_ms

    def _data_source_summary(self) -> dict[str, str]:
        return {
            "knowledgeBase": "MYSQL(caries_ai)",
            "lexicalDenseStore": self.settings.rag_vector_store_type,
            "graphStore": "NEO4J",
            "llmProvider": self.settings.llm_provider_code,
        }

    def _evidence_sufficient(self, evidence_bundle: list) -> bool:
        if len(evidence_bundle) < self.settings.rag_evidence_min_count:
            return False
        distinct_doc_count = len({item.doc_id for item in evidence_bundle if item.doc_id is not None})
        return distinct_doc_count >= self.settings.rag_evidence_min_distinct_docs

    @staticmethod
    def _confidence(reranked: list[dict], evidence_bundle: list, refusal_reason: str | None, safety_flags: list[str]) -> float:
        if refusal_reason is not None or not reranked:
            return 0.0
        top_scores = [float(item.get("rerank_score") or 0.0) for item in reranked[:3]]
        evidence_diversity = len({item.doc_id for item in evidence_bundle[:3] if item.doc_id is not None}) / 3
        penalty = 0.1 if "NO_PROVENANCE" in safety_flags else 0.0
        return round(max(0.0, min(1.0, (sum(top_scores) / max(1, len(top_scores))) + evidence_diversity * 0.15 - penalty)), 4)

    @staticmethod
    def _refusal_text(reason: str) -> str:
        mapping = {
            "PROMPT_INJECTION": "The request contains unsafe prompt content and cannot be answered.",
            "PRIVACY_CONCERN": "The request involves sensitive personal information and cannot be answered.",
            "INSUFFICIENT_EVIDENCE": "Published knowledge does not contain enough evidence to answer this question.",
            "HUMAN_REVIEW_REQUIRED": "This question requires clinician review instead of automated advice.",
            "OUT_OF_SCOPE": "The request is outside the supported dental knowledge scope.",
        }
        return mapping.get(reason, "The request cannot be answered safely.")
