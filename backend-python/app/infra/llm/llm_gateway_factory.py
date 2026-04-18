from __future__ import annotations

from app.core.config import Settings
from app.infra.llm.base_llm_client import BaseLlmClient
from app.infra.llm.openai_compatible_llm_client import OpenAiCompatibleLlmClient
from app.infra.llm.template_llm_client import TemplateLlmClient


def create_llm_client(settings: Settings) -> BaseLlmClient:
    provider = (settings.llm_provider_code or "MOCK").strip().upper()
    if provider == "MOCK":
        return TemplateLlmClient()
    if provider in {"OPENAI", "OPENAI_COMPATIBLE", "DASHSCOPE", "DEEPSEEK", "QWEN"}:
        return OpenAiCompatibleLlmClient(settings)
    raise ValueError(f"Unsupported CG_LLM_PROVIDER_CODE={settings.llm_provider_code}")
