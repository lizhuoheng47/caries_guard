from __future__ import annotations

from typing import Any

from pydantic import Field

from app.schemas.base import CamelModel


class KnowledgeSearchRequest(CamelModel):
    query: str = Field(min_length=2, max_length=1000)
    top_k: int | None = Field(default=None, ge=1, le=10)


class KnowledgeCitation(CamelModel):
    rank_no: int
    doc_no: str
    doc_title: str
    chunk_text: str
    score: float
    source_uri: str | None = None
    source_file: str | None = None
    source_pages: list[int] = Field(default_factory=list)
    metadata: dict[str, Any] = Field(default_factory=dict)


class KnowledgeStatus(CamelModel):
    enabled: bool
    ready: bool
    knowledge_version: str | None = None
    source_path: str | None = None
    document_count: int = 0
    chunk_count: int = 0
    loaded_at: str | None = None
    error: str | None = None
