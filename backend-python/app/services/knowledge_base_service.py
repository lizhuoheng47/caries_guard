from __future__ import annotations

import json
import math
import re
import threading
from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.logging import get_logger

log = get_logger("cariesguard-ai.knowledge")

_ASCII_TOKEN = re.compile(r"[a-z0-9]+(?:[-_.][a-z0-9]+)*", re.IGNORECASE)
_CJK = re.compile(r"[\u3400-\u9fff]")


@dataclass(frozen=True)
class KnowledgeChunk:
    doc_no: str
    doc_title: str
    chunk_text: str
    source_uri: str | None = None
    source_file: str | None = None
    source_pages: tuple[int, ...] = ()
    tags: tuple[str, ...] = ()
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class KnowledgeHit:
    chunk: KnowledgeChunk
    score: float

    def to_citation(self, rank_no: int) -> dict[str, Any]:
        return {
            "rankNo": rank_no,
            "docNo": self.chunk.doc_no,
            "docTitle": self.chunk.doc_title,
            "chunkText": self.chunk.chunk_text,
            "score": round(self.score, 6),
            "sourceUri": self.chunk.source_uri,
            "sourceFile": self.chunk.source_file,
            "sourcePages": list(self.chunk.source_pages),
            "metadata": self.chunk.metadata,
        }


@dataclass(frozen=True)
class KnowledgeSnapshot:
    version: str
    chunks: tuple[KnowledgeChunk, ...]
    document_count: int
    loaded_at: str
    document_frequency: dict[str, int]


class KnowledgeBaseService:
    """Versioned, thread-safe local retriever with deterministic TF-IDF scoring."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._lock = threading.RLock()
        self._snapshot: KnowledgeSnapshot | None = None
        self._error: str | None = None
        if settings.rag_enabled:
            self.reload()

    def reload(self) -> dict[str, Any]:
        path = self._resolve_path(self._settings.rag_knowledge_path)
        try:
            snapshot = self._load(path)
        except Exception as exc:
            with self._lock:
                self._error = str(exc)
            log.exception("knowledge reload failed path=%s", path)
            raise
        with self._lock:
            self._snapshot = snapshot
            self._error = None
        log.info(
            "knowledge loaded version=%s documents=%s chunks=%s path=%s",
            snapshot.version,
            snapshot.document_count,
            len(snapshot.chunks),
            path,
        )
        return self.status()

    def status(self) -> dict[str, Any]:
        with self._lock:
            snapshot = self._snapshot
            error = self._error
        return {
            "enabled": self._settings.rag_enabled,
            "ready": snapshot is not None,
            "knowledgeVersion": snapshot.version if snapshot else None,
            "sourcePath": str(self._resolve_path(self._settings.rag_knowledge_path)),
            "documentCount": snapshot.document_count if snapshot else 0,
            "chunkCount": len(snapshot.chunks) if snapshot else 0,
            "loadedAt": snapshot.loaded_at if snapshot else None,
            "error": error,
        }

    @property
    def version(self) -> str | None:
        with self._lock:
            return self._snapshot.version if self._snapshot else None

    def search(self, query: str, top_k: int | None = None) -> list[KnowledgeHit]:
        normalized_query = (query or "").strip()
        if not normalized_query:
            return []
        with self._lock:
            snapshot = self._snapshot
        if snapshot is None:
            return []
        limit = max(1, min(top_k or self._settings.rag_top_k, 10))
        query_tokens = self._tokenize(normalized_query)
        if not query_tokens:
            return []

        query_counts = Counter(query_tokens)
        scored: list[KnowledgeHit] = []
        corpus_size = max(1, len(snapshot.chunks))
        for chunk in snapshot.chunks:
            tokens = self._tokenize(" ".join((chunk.doc_title, chunk.chunk_text, *chunk.tags)))
            counts = Counter(tokens)
            score = 0.0
            for token, query_tf in query_counts.items():
                term_tf = counts.get(token, 0)
                if not term_tf:
                    continue
                doc_freq = snapshot.document_frequency.get(token, 0)
                inverse_doc_freq = math.log(1.0 + (corpus_size + 1.0) / (doc_freq + 1.0))
                score += (1.0 + math.log(term_tf)) * inverse_doc_freq * (1.0 + math.log(query_tf))
            if score > 0:
                length_normalizer = 1.0 + math.log(max(1, len(tokens)))
                scored.append(KnowledgeHit(chunk=chunk, score=score / length_normalizer))
        scored.sort(key=lambda item: (-item.score, item.chunk.doc_no, item.chunk.chunk_text))
        return scored[:limit]

    def _load(self, path: Path) -> KnowledgeSnapshot:
        if not path.is_file():
            raise FileNotFoundError(f"knowledge file does not exist: {path}")
        payload = json.loads(path.read_text(encoding="utf-8"))
        version = str(payload.get("knowledgeVersion") or payload.get("version") or "").strip()
        if not version:
            raise ValueError("knowledgeVersion is required")
        documents = payload.get("documents")
        if not isinstance(documents, list) or not documents:
            raise ValueError("knowledge documents must be a non-empty list")

        chunks: list[KnowledgeChunk] = []
        document_ids: set[str] = set()
        for document in documents:
            if not isinstance(document, dict):
                raise ValueError("each knowledge document must be an object")
            doc_no = str(document.get("docNo") or "").strip()
            title = str(document.get("title") or document.get("docTitle") or "").strip()
            if not doc_no or not title:
                raise ValueError("each knowledge document requires docNo and title")
            if doc_no in document_ids:
                raise ValueError(f"duplicate knowledge docNo: {doc_no}")
            document_ids.add(doc_no)
            raw_chunks = document.get("chunks")
            if not isinstance(raw_chunks, list) or not raw_chunks:
                raise ValueError(f"knowledge document {doc_no} requires at least one chunk")
            base_tags = self._string_tuple(document.get("tags"))
            for raw_chunk in raw_chunks:
                if isinstance(raw_chunk, str):
                    raw_chunk = {"text": raw_chunk}
                if not isinstance(raw_chunk, dict):
                    raise ValueError(f"invalid chunk in document {doc_no}")
                text = str(raw_chunk.get("text") or raw_chunk.get("chunkText") or "").strip()
                if not text:
                    raise ValueError(f"empty chunk in document {doc_no}")
                raw_pages = raw_chunk.get("sourcePages", document.get("sourcePages", []))
                pages = tuple(int(page) for page in raw_pages) if isinstance(raw_pages, list) else ()
                chunks.append(
                    KnowledgeChunk(
                        doc_no=doc_no,
                        doc_title=title,
                        chunk_text=text,
                        source_uri=self._optional_text(document.get("sourceUri")),
                        source_file=self._optional_text(document.get("sourceFile")),
                        source_pages=pages,
                        tags=tuple(dict.fromkeys((*base_tags, *self._string_tuple(raw_chunk.get("tags"))))),
                        metadata=dict(raw_chunk.get("metadata") or {}),
                    )
                )
        document_frequency: Counter[str] = Counter()
        for chunk in chunks:
            terms = set(self._tokenize(" ".join((chunk.doc_title, chunk.chunk_text, *chunk.tags))))
            document_frequency.update(terms)
        loaded_at = datetime.now(timezone.utc).isoformat()
        return KnowledgeSnapshot(
            version=version,
            chunks=tuple(chunks),
            document_count=len(document_ids),
            loaded_at=loaded_at,
            document_frequency=dict(document_frequency),
        )

    @staticmethod
    def _resolve_path(value: str) -> Path:
        path = Path(value)
        project_root = Path(__file__).resolve().parents[2]
        normalized = path.as_posix()
        if normalized.startswith("/app/"):
            return (project_root / normalized.removeprefix("/app/")).resolve()
        return path.resolve() if path.is_absolute() else (project_root / path).resolve()

    @staticmethod
    def _tokenize(value: str) -> list[str]:
        lowered = value.lower()
        ascii_terms = _ASCII_TOKEN.findall(lowered)
        cjk_chars = _CJK.findall(lowered)
        cjk_terms = cjk_chars + ["".join(cjk_chars[index:index + 2]) for index in range(len(cjk_chars) - 1)]
        return ascii_terms + cjk_terms

    @staticmethod
    def _string_tuple(value: Any) -> tuple[str, ...]:
        if not isinstance(value, list):
            return ()
        return tuple(str(item).strip() for item in value if str(item).strip())

    @staticmethod
    def _optional_text(value: Any) -> str | None:
        text = str(value or "").strip()
        return text or None
