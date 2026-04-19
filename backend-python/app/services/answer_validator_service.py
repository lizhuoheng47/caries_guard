from __future__ import annotations


class AnswerValidatorService:
    def validate(self, answer_text: str, citations: list[dict]) -> list[str]:
        flags = ["DENTAL_SCOPE", "NO_DIAGNOSIS"]
        lowered = (answer_text or "").lower()
        if not citations:
            flags.append("NO_CITATION")
        if any(token in lowered for token in ("确诊", "处方", "药物剂量")):
            flags.append("HUMAN_REVIEW_REQUIRED")
        return flags
