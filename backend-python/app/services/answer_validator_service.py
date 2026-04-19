from __future__ import annotations

from typing import Any


class AnswerValidatorService:
    def validate(self, answer_text: str, citations: list[dict[str, Any]], evidence: list[dict[str, Any]] | None = None) -> list[str]:
        flags = ["DENTAL_SCOPE", "NO_DIAGNOSIS"]
        lowered = (answer_text or "").lower()
        if not citations:
            flags.append("NO_CITATION")
        if evidence is not None and not any(item.get("doc_id") or item.get("chunk_id") for item in evidence):
            flags.append("NO_PROVENANCE")
        if any(token in lowered for token in ("确诊", "处方", "药物剂量", "diagnosis", "prescription")):
            flags.append("HUMAN_REVIEW_REQUIRED")
        if evidence is not None and len({item.get("doc_id") for item in evidence if item.get("doc_id")}) < 1:
            flags.append("LOW_EVIDENCE_DIVERSITY")
        return flags
