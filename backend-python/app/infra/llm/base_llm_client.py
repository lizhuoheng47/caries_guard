from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class LlmResult:
    answer_text: str
    prompt_text: str
    provider: str | None = None
    model: str | None = None
    latency_ms: int | None = None
    usage: dict[str, int] | None = None
    finish_reason: str | None = None


class BaseLlmClient(Protocol):
    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None) -> LlmResult:
        ...
