from __future__ import annotations


class FusionService:
    def fuse(self, lexical_hits: list[dict], dense_hits: list[dict], graph_hits: list[dict], top_k: int) -> list[dict]:
        scored: dict[str, dict] = {}
        for channel_hits in (lexical_hits, dense_hits, graph_hits):
            for rank, hit in enumerate(channel_hits, start=1):
                evidence_id = hit["evidence_id"]
                if evidence_id not in scored:
                    scored[evidence_id] = {**hit, "origin_channels": [], "fusion_score": 0.0}
                scored[evidence_id]["origin_channels"].append(hit["channel"])
                scored[evidence_id]["fusion_score"] += 1.0 / (60 + rank)
        results = list(scored.values())
        results.sort(key=lambda item: item["fusion_score"], reverse=True)
        for idx, item in enumerate(results, start=1):
            item["fusion_rank"] = idx
        return results[:top_k]
