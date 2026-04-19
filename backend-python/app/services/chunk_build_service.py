from __future__ import annotations

import re
from typing import Any


class ChunkBuildService:
    def build(
        self,
        normalized_markdown: str,
        *,
        doc_title: str,
        doc_source_code: str,
        source_uri: str | None,
        org_id: int | None,
        max_length: int = 700,
    ) -> list[dict[str, Any]]:
        sections = self._split_sections(normalized_markdown)
        chunks: list[dict[str, Any]] = []
        chunk_no = 1
        for section_path, text in sections:
            if len(text) <= max_length:
                chunks.append(self._chunk_payload(chunk_no, section_path, text, doc_title, doc_source_code, source_uri, org_id))
                chunk_no += 1
                continue
            start = 0
            while start < len(text):
                end = min(len(text), start + max_length)
                segment = text[start:end].strip()
                if segment:
                    chunks.append(self._chunk_payload(chunk_no, section_path, segment, doc_title, doc_source_code, source_uri, org_id))
                    chunk_no += 1
                if end >= len(text):
                    break
                start = max(0, end - 120)
        return chunks

    @staticmethod
    def _split_sections(text: str) -> list[tuple[str, str]]:
        current_heading = "Document"
        buffer: list[str] = []
        result: list[tuple[str, str]] = []
        for line in text.splitlines():
            stripped = line.strip()
            if stripped.startswith("#"):
                if buffer:
                    result.append((current_heading, "\n".join(buffer).strip()))
                    buffer = []
                current_heading = stripped.lstrip("#").strip() or current_heading
                continue
            if stripped:
                buffer.append(stripped)
        if buffer:
            result.append((current_heading, "\n".join(buffer).strip()))
        if not result:
            normalized = re.sub(r"\s+", " ", text).strip()
            return [("Document", normalized)] if normalized else []
        return result

    @staticmethod
    def _chunk_payload(
        chunk_no: int,
        section_path: str,
        text: str,
        doc_title: str,
        doc_source_code: str,
        source_uri: str | None,
        org_id: int | None,
    ) -> dict[str, Any]:
        chunk_type = "LIST" if text.startswith("-") else "PARAGRAPH"
        return {
            "chunk_no": chunk_no,
            "section_path": section_path,
            "chunk_text": text,
            "chunk_type": chunk_type,
            "token_count": len(text),
            "doc_title_snapshot": doc_title,
            "doc_source_code": doc_source_code,
            "source_uri": source_uri,
            "org_id": org_id,
            "medical_tags": [],
            "graph_entity_refs": [],
        }
