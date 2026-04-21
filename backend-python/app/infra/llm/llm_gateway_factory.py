from __future__ import annotations

from app.core.config import Settings
from app.infra.llm.base_llm_client import BaseLlmClient
from app.infra.llm.openai_compatible_llm_client import OpenAiCompatibleLlmClient
from app.infra.llm.template_llm_client import TemplateLlmClient


class FallbackLlmClient:
    def __init__(self, primary: BaseLlmClient, fallback: BaseLlmClient) -> None:
        self.primary = primary
        self.fallback = fallback

    def generate(self, scene: str, query: str, evidence: list[dict], context_text: str | None = None):
        try:
            return self.primary.generate(scene=scene, query=query, evidence=evidence, context_text=context_text)
        except Exception:
            return self.fallback.generate(scene=scene, query=query, evidence=evidence, context_text=context_text)


def create_llm_client(settings: Settings) -> BaseLlmClient:
    provider = (settings.llm_provider_code or "MOCK").strip().upper()
    if provider == "MOCK":
        return TemplateLlmClient(settings)
    if provider in {"OPENAI", "OPENAI_COMPATIBLE", "DASHSCOPE", "DEEPSEEK", "QWEN"}:
        primary = OpenAiCompatibleLlmClient(settings)
        if settings.llm_enable_fallback_mock and settings.ai_runtime_mode != "real":
            return FallbackLlmClient(primary, TemplateLlmClient(settings))
        return primary
    raise ValueError(f"Unsupported CG_LLM_PROVIDER_CODE={settings.llm_provider_code}")
