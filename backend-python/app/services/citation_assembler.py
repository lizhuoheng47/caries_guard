from __future__ import annotations

from app.schemas.rag import RagCitation, RagRetrievedChunk


class CitationAssembler:
    def citations(self, kb: dict, hits: list[dict]) -> list[RagCitation]:
        return [
            RagCitation(
                rank_no=index,
                knowledge_base_code=kb.get("kb_code"),
                document_code=hit.get("doc_no"),
                document_version=hit.get("doc_version"),
                doc_id=hit["doc_id"],
                doc_title=hit.get("doc_title"),
                chunk_id=hit["chunk_id"],
                score=hit["score"],
                retrieval_score=hit["score"],
                source_uri=hit.get("source_uri"),
                chunk_text=hit["chunk_text"],
            )
            for index, hit in enumerate(hits, start=1)
        ]

    def retrieved_chunks(self, hits: list[dict]) -> list[RagRetrievedChunk]:
        return [
            RagRetrievedChunk(
                chunk_id=hit["chunk_id"],
                document_code=hit.get("doc_no"),
                score=hit["score"],
            )
            for hit in hits
        ]
