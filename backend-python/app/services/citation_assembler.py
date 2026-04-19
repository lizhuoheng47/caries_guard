from __future__ import annotations

from app.schemas.rag import RagCitation, RagEvidence, RagGraphEvidence, RagRetrievedChunk


class CitationAssembler:
    def citations(self, kb: dict, hits: list[dict]) -> list[RagCitation]:
        citations: list[RagCitation] = []
        for index, hit in enumerate(hits, start=1):
            citations.append(
                RagCitation(
                    rank_no=index,
                    evidence_type=self._evidence_type(hit),
                    knowledge_base_code=kb.get("kb_code"),
                    document_code=hit.get("doc_no"),
                    document_version=hit.get("doc_version"),
                    doc_id=hit.get("doc_id"),
                    doc_title=hit.get("doc_title"),
                    chunk_id=hit.get("chunk_id"),
                    score=float(hit.get("rerank_score") or hit.get("fusion_score") or hit.get("score") or 0.0),
                    retrieval_score=hit.get("score"),
                    source_uri=hit.get("source_uri"),
                    chunk_text=hit.get("chunk_text") or hit.get("evidence_text") or "",
                    cypher_template_code=hit.get("cypher_template_code"),
                    graph_path_id=hit.get("graph_path_id"),
                    provenance_json=hit.get("provenance_path") or hit.get("result_path_json"),
                )
            )
        return citations

    def retrieved_chunks(self, hits: list[dict]) -> list[RagRetrievedChunk]:
        return [
            RagRetrievedChunk(
                chunk_id=hit["chunk_id"],
                document_code=hit.get("doc_no"),
                score=hit.get("score") or 0.0,
                chunk_text=hit.get("chunk_text"),
                doc_title=hit.get("doc_title"),
            )
            for hit in hits
            if hit.get("chunk_id") is not None
        ]

    def graph_evidence(self, hits: list[dict]) -> list[RagGraphEvidence]:
        return [
            RagGraphEvidence(
                graph_path_id=hit["graph_path_id"],
                cypher_template_code=hit.get("cypher_template_code"),
                score=hit.get("score", 0.0),
                evidence_text=hit.get("evidence_text"),
                result_path_json=hit.get("result_path_json"),
                chunk_id=hit.get("chunk_id"),
                doc_id=hit.get("doc_id"),
                doc_title=hit.get("doc_title"),
                doc_version=hit.get("doc_version"),
            )
            for hit in hits
            if hit.get("channel") == "GRAPH"
        ]

    def evidence(self, hits: list[dict]) -> list[RagEvidence]:
        return [
            RagEvidence(
                evidence_id=str(hit.get("evidence_id") or hit.get("graph_path_id")),
                evidence_type=self._evidence_type(hit),
                channel=hit.get("channel", "UNKNOWN"),
                score=float(hit.get("score") or 0.0),
                fusion_score=hit.get("fusion_score"),
                rerank_score=hit.get("rerank_score"),
                doc_id=hit.get("doc_id"),
                chunk_id=hit.get("chunk_id"),
                document_code=hit.get("doc_no"),
                document_version=hit.get("doc_version"),
                doc_title=hit.get("doc_title"),
                source_uri=hit.get("source_uri"),
                chunk_text=hit.get("chunk_text"),
                evidence_text=hit.get("evidence_text"),
                cypher_template_code=hit.get("cypher_template_code"),
                graph_path_id=hit.get("graph_path_id"),
                provenance_json=hit.get("provenance_path") or hit.get("result_path_json"),
                source_authority_score=hit.get("source_authority_score"),
                freshness_score=hit.get("freshness_score"),
            )
            for hit in hits
        ]

    @staticmethod
    def _evidence_type(hit: dict) -> str:
        channels = set(hit.get("origin_channels") or [])
        if hit.get("channel") == "GRAPH" or "GRAPH" in channels:
            return "GRAPH"
        return hit.get("evidence_kind", "TEXT")
