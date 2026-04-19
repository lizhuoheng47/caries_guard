from __future__ import annotations

import io
import json
from pathlib import Path
from typing import Any

import pandas as pd
from bs4 import BeautifulSoup
from docx import Document as DocxDocument
from pypdf import PdfReader


class DocumentParseService:
    def parse_bytes(self, file_name: str, data: bytes) -> dict[str, Any]:
        suffix = Path(file_name).suffix.lower()
        if suffix in {".md", ".txt"}:
            text = self._decode_text(data)
            return self._text_payload(text, file_name)
        if suffix == ".json":
            payload = json.loads(self._decode_text(data))
            return self._json_payload(payload, file_name)
        if suffix == ".jsonl":
            lines = [json.loads(line) for line in self._decode_text(data).splitlines() if line.strip()]
            return self._json_payload(lines, file_name)
        if suffix == ".docx":
            document = DocxDocument(io.BytesIO(data))
            parts = [paragraph.text.strip() for paragraph in document.paragraphs if paragraph.text.strip()]
            return self._text_payload("\n\n".join(parts), file_name)
        if suffix == ".pdf":
            reader = PdfReader(io.BytesIO(data))
            text = "\n".join((page.extract_text() or "") for page in reader.pages)
            return self._text_payload(text, file_name)
        if suffix == ".html":
            soup = BeautifulSoup(data, "html.parser")
            text = soup.get_text("\n")
            return self._text_payload(text, file_name)
        if suffix == ".csv":
            df = pd.read_csv(io.BytesIO(data))
            return self._table_payload(df, file_name)
        if suffix == ".xlsx":
            sheets = pd.read_excel(io.BytesIO(data), sheet_name=None)
            markdown = []
            structured: dict[str, Any] = {}
            for sheet_name, df in sheets.items():
                structured[sheet_name] = df.fillna("").to_dict(orient="records")
                markdown.append(f"# {sheet_name}\n")
                markdown.append(df.fillna("").to_markdown(index=False))
            return {
                "normalized_markdown": "\n\n".join(markdown),
                "structured_json": structured,
                "section_tree": [{"title": sheet_name, "level": 1} for sheet_name in sheets.keys()],
                "table_json": structured,
                "metadata_json": {"fileName": file_name, "sheetCount": len(sheets)},
            }
        raise ValueError(f"unsupported file suffix: {suffix}")

    def _table_payload(self, df: pd.DataFrame, file_name: str) -> dict[str, Any]:
        clean_df = df.fillna("")
        return {
            "normalized_markdown": clean_df.to_markdown(index=False),
            "structured_json": clean_df.to_dict(orient="records"),
            "section_tree": [{"title": file_name, "level": 1}],
            "table_json": clean_df.to_dict(orient="records"),
            "metadata_json": {"fileName": file_name, "rowCount": int(clean_df.shape[0]), "columnCount": int(clean_df.shape[1])},
        }

    def _json_payload(self, payload: Any, file_name: str) -> dict[str, Any]:
        formatted = json.dumps(payload, ensure_ascii=False, indent=2)
        return {
            "normalized_markdown": f"```json\n{formatted}\n```",
            "structured_json": payload,
            "section_tree": [{"title": file_name, "level": 1}],
            "table_json": payload if isinstance(payload, list) else None,
            "metadata_json": {"fileName": file_name, "recordCount": len(payload) if isinstance(payload, list) else 1},
        }

    def _text_payload(self, text: str, file_name: str) -> dict[str, Any]:
        normalized = "\n".join(line.rstrip() for line in text.replace("\r\n", "\n").split("\n")).strip()
        section_tree = self._section_tree(normalized)
        return {
            "normalized_markdown": normalized,
            "structured_json": {"text": normalized},
            "section_tree": section_tree,
            "table_json": [],
            "metadata_json": {"fileName": file_name, "charCount": len(normalized)},
        }

    @staticmethod
    def _section_tree(text: str) -> list[dict[str, Any]]:
        sections: list[dict[str, Any]] = []
        for line in text.splitlines():
            stripped = line.strip()
            if stripped.startswith("#"):
                level = len(stripped) - len(stripped.lstrip("#"))
                sections.append({"title": stripped.lstrip("#").strip(), "level": level})
        if not sections:
            sections.append({"title": "Document", "level": 1})
        return sections

    @staticmethod
    def _decode_text(data: bytes) -> str:
        for encoding in ("utf-8", "utf-8-sig", "gbk", "gb18030"):
            try:
                return data.decode(encoding)
            except UnicodeDecodeError:
                continue
        return data.decode("utf-8", errors="ignore")
