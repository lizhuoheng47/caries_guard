from __future__ import annotations


class RefusalPolicyService:
    def evaluate(self, query: str, evidence_count: int) -> str | None:
        lowered = (query or "").lower()
        if any(token in lowered for token in ("ignore previous", "system prompt", "developer message")):
            return "PROMPT_INJECTION"
        if any(token in lowered for token in ("身份证", "手机号", "家庭住址")):
            return "PRIVACY_CONCERN"
        if evidence_count == 0:
            return "INSUFFICIENT_EVIDENCE"
        if any(token in lowered for token in ("处方", "开药", "确诊")):
            return "HUMAN_REVIEW_REQUIRED"
        return None
