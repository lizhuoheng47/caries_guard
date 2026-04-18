from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class RagSafetyDecision:
    safety_flags: list[str]
    refusal_reason: str | None
    answer_text: str | None


class RagSafetyGuardService:
    _danger_phrases = (
        "final diagnosis",
        "definitive diagnosis",
        "prescribe",
        "prescription",
        "replace the doctor",
        "treatment plan",
    )
    _out_of_scope_phrases = (
        "stock price",
        "weather",
        "lottery",
        "write code",
        "programming",
        "politics",
    )
    _prompt_injection_phrases = (
        "ignore previous instructions",
        "ignore the previous instructions",
        "ignore all previous instructions",
        "system prompt",
        "developer message",
        "reveal your prompt",
        "show your prompt",
        "jailbreak",
    )

    def evaluate(self, scene: str, query: str, hits: list[dict], context_text: str | None) -> RagSafetyDecision:
        flags = ["MEDICAL_CAUTION"]
        lowered = (query or "").lower()
        refusal_reason: str | None = None
        answer_text: str | None = None

        if any(phrase in lowered for phrase in self._prompt_injection_phrases):
            flags.append("PROMPT_INJECTION")
            refusal_reason = "PROMPT_INJECTION"
            answer_text = "I cannot follow instructions that attempt to override safety rules or reveal internal prompts."
        elif any(phrase in lowered for phrase in self._out_of_scope_phrases):
            flags.append("OUT_OF_SCOPE")
            refusal_reason = "OUT_OF_SCOPE"
            answer_text = "This question is outside the approved dental knowledge scope."
        elif any(phrase in lowered for phrase in self._danger_phrases):
            flags.append("HUMAN_REVIEW_REQUIRED")
            refusal_reason = "HUMAN_REVIEW_REQUIRED"
            answer_text = (
                "I cannot replace a dentist or provide a final diagnosis or prescription. "
                "Please use the structured findings and clinical review."
            )
        elif not hits:
            flags.append("INSUFFICIENT_EVIDENCE")
            refusal_reason = "INSUFFICIENT_EVIDENCE"
            answer_text = (
                "No approved knowledge evidence was retrieved, so I cannot produce a reliable answer. "
                "Please add reviewed knowledge documents or ask a dentist to review the case."
            )

        if context_text and "[REDACTED_" in context_text:
            flags.append("SENSITIVE_INPUT_REDACTED")

        return RagSafetyDecision(
            safety_flags=self._dedupe(flags),
            refusal_reason=refusal_reason,
            answer_text=answer_text,
        )

    def confidence(self, hits: list[dict], refusal_reason: str | None) -> float:
        if refusal_reason:
            return 0.0
        if not hits:
            return 0.0
        top_scores = [float(item.get("score") or 0.0) for item in hits[:3]]
        score = sum(top_scores) / len(top_scores)
        return round(max(0.0, min(score, 0.95)), 2)

    @staticmethod
    def prompt_summary(scene: str, query: str, hits: list[dict], context_text: str | None) -> str:
        context_state = "redacted" if context_text and "[REDACTED_" in context_text else "minimal"
        return (
            f"scene={scene}; queryChars={len(query or '')}; evidenceCount={len(hits)}; "
            f"context={context_state if context_text else 'none'}"
        )

    @staticmethod
    def _dedupe(values: list[str]) -> list[str]:
        result: list[str] = []
        for value in values:
            if value not in result:
                result.append(value)
        return result
