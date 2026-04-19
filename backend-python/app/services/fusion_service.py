from __future__ import annotations

from app.core.config import Settings


class FusionService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def fuse(self, lexical_hits: list[dict], dense_hits: list[dict], graph_hits: list[dict], top_k: int) -> list[dict]:
        scored: dict[str, dict] = {}
        for channel_hits in (lexical_hits, dense_hits, graph_hits):
            for rank, hit in enumerate(channel_hits, start=1):
                evidence_key = self._evidence_key(hit)
                candidate = scored.setdefault(
                    evidence_key,
                    {
                        **hit,
                        "origin_channels": [],
                        "fusion_score": 0.0,
                        "source_authority_score": float(hit.get("source_authority_score") or 0.0),
                        "freshness_score": float(hit.get("freshness_score") or 0.0),
                    },
                )
                channel = hit.get("channel", "UNKNOWN")
                if channel not in candidate["origin_channels"]:
                    candidate["origin_channels"].append(channel)
                candidate["fusion_score"] += self._weighted_rrf(rank, channel)
                candidate["fusion_score"] += float(hit.get("source_authority_score") or 0.0) * 0.05
                candidate["fusion_score"] += float(hit.get("freshness_score") or 0.0) * 0.03
                if channel == "GRAPH":
                    candidate["fusion_score"] += float(hit.get("graph_confidence_score") or 0.0) * self.settings.rag_graph_confidence_weight
                    candidate["cypher_template_code"] = hit.get("cypher_template_code") or candidate.get("cypher_template_code")
                    candidate["graph_path_id"] = hit.get("graph_path_id") or candidate.get("graph_path_id")
                    candidate["provenance_path"] = hit.get("provenance_path") or candidate.get("provenance_path")
                    candidate["result_path_json"] = hit.get("result_path_json") or candidate.get("result_path_json")
                    candidate["evidence_text"] = hit.get("evidence_text") or candidate.get("evidence_text")
                candidate["score"] = max(float(candidate.get("score") or 0.0), float(hit.get("score") or 0.0))
        results = list(scored.values())
        results.sort(key=lambda item: item["fusion_score"], reverse=True)
        for idx, item in enumerate(results, start=1):
            item["fusion_rank"] = idx
        return results[:top_k]

    def _weighted_rrf(self, rank: int, channel: str) -> float:
        channel_weight = float(self.settings.rag_channel_weights.get(channel.upper(), 1.0))
        return channel_weight / (60 + rank)

    @staticmethod
    def _evidence_key(hit: dict) -> str:
        if hit.get("chunk_id"):
            return f"chunk:{hit['chunk_id']}"
        if hit.get("doc_id") and hit.get("cypher_template_code"):
            return f"graph:{hit['doc_id']}:{hit['cypher_template_code']}"
        return str(hit.get("evidence_id") or hit.get("graph_path_id"))
