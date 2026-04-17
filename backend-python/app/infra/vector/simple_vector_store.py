from __future__ import annotations

import json
import math
import re
import hashlib
from pathlib import Path
from typing import Any


class SimpleVectorStore:
    def __init__(self, dimension: int = 128) -> None:
        self.dimension = dimension

    def build(self, index_path: str | Path, kb: dict[str, Any], chunks: list[dict[str, Any]]) -> None:
        path = Path(index_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "kbCode": kb["kb_code"],
            "knowledgeVersion": kb["knowledge_version"],
            "embeddingModel": kb.get("embedding_model"),
            "dimension": self.dimension,
            "chunks": [
                {
                    "chunkId": item["id"],
                    "docId": item["doc_id"],
                    "docNo": item.get("doc_no"),
                    "docTitle": item.get("doc_title"),
                    "sourceUri": item.get("source_uri"),
                    "docSourceCode": item.get("doc_source_code"),
                    "chunkText": item["chunk_text"],
                    "embedding": self.embed(item["chunk_text"]),
                }
                for item in chunks
            ],
        }
        path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")

    def search(self, index_path: str | Path, query: str, top_k: int) -> list[dict[str, Any]]:
        path = Path(index_path)
        if not path.exists():
            return []
        payload = json.loads(path.read_text(encoding="utf-8"))
        query_vector = self.embed(query)
        hits: list[dict[str, Any]] = []
        for item in payload.get("chunks", []):
            score = self._cosine(query_vector, item.get("embedding") or [])
            hits.append(
                {
                    "chunk_id": item["chunkId"],
                    "doc_id": item["docId"],
                    "doc_no": item.get("docNo"),
                    "doc_title": item.get("docTitle"),
                    "source_uri": item.get("sourceUri"),
                    "doc_source_code": item.get("docSourceCode"),
                    "chunk_text": item["chunkText"],
                    "score": round(float(score), 6),
                }
            )
        hits.sort(key=lambda item: item["score"], reverse=True)
        return hits[:top_k]

    def embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimension
        for token in self._tokens(text):
            digest = hashlib.md5(token.encode("utf-8")).hexdigest()
            vector[int(digest[:8], 16) % self.dimension] += 1.0
        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            return vector
        return [round(value / norm, 8) for value in vector]

    @staticmethod
    def _tokens(text: str) -> list[str]:
        lower = text.lower()
        latin = re.findall(r"[a-z0-9]+", lower)
        cjk = re.findall(r"[\u4e00-\u9fff]", text)
        cjk_bigrams = [f"{cjk[i]}{cjk[i + 1]}" for i in range(len(cjk) - 1)]
        return latin + cjk + cjk_bigrams

    @staticmethod
    def _cosine(left: list[float], right: list[float]) -> float:
        if not left or not right:
            return 0.0
        return sum(a * b for a, b in zip(left, right))
