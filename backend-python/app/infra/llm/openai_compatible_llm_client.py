from __future__ import annotations

import time
from typing import Any

import requests

from app.core.config import Settings
from app.infra.llm.base_llm_client import LlmResult


class OpenAiCompatibleLlmClient:
    """Minimal OpenAI-compatible chat completions adapter."""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None) -> LlmResult:
        prompt = self._build_prompt(scene, query, evidence, context_text)
        payload = {
            "model": self.settings.llm_model_name,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are a controlled dental health assistant. Use only the supplied evidence. "
                        "Do not provide a final diagnosis or treatment prescription. Be conservative."
                    ),
                },
                {"role": "user", "content": prompt},
            ],
            "temperature": self.settings.llm_temperature,
        }
        response = self._post_with_retry(payload)
        answer = response.get("choices", [{}])[0].get("message", {}).get("content")
        if not answer:
            raise RuntimeError("LLM provider returned empty completion")
        return LlmResult(answer_text=str(answer).strip(), prompt_text=prompt)

    def _post_with_retry(self, payload: dict[str, Any]) -> dict[str, Any]:
        if not self.settings.llm_base_url:
            raise RuntimeError("CG_LLM_BASE_URL is required for non-MOCK LLM provider")
        url = self.settings.llm_base_url.rstrip("/") + "/chat/completions"
        headers = {"Content-Type": "application/json"}
        if self.settings.llm_api_key:
            headers["Authorization"] = f"Bearer {self.settings.llm_api_key}"
        last_error: Exception | None = None
        for attempt in range(self.settings.llm_retry_count + 1):
            try:
                response = requests.post(
                    url,
                    json=payload,
                    headers=headers,
                    timeout=self.settings.llm_timeout_seconds,
                )
                response.raise_for_status()
                return response.json()
            except Exception as exc:  # pragma: no cover - network path
                last_error = exc
                if attempt < self.settings.llm_retry_count:
                    time.sleep(min(0.25 * (attempt + 1), 1.0))
        raise RuntimeError(f"LLM provider call failed: {last_error}")

    @staticmethod
    def _build_prompt(scene: str, query: str, evidence: list[dict], context_text: str | None) -> str:
        evidence_text = "\n".join(
            f"[{index}] {item.get('doc_title') or item.get('doc_no') or 'document'}: {item['chunk_text']}"
            for index, item in enumerate(evidence, start=1)
        )
        return (
            f"Scene: {scene}\n"
            f"Question: {query}\n"
            f"Case context: {context_text or '{}'}\n"
            f"Evidence:\n{evidence_text or 'NONE'}"
        )
