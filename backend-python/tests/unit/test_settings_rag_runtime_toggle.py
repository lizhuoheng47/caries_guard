import pytest

from app.core.config import Settings


def test_settings_allows_image_chain_without_llm_embedding_when_rag_runtime_disabled():
    settings = Settings(
        ai_runtime_mode="real",
        rag_runtime_enabled=False,
        analysis_kb_enhancement_enabled=False,
        llm_provider_code="OPENAI_COMPATIBLE",
        llm_api_key="",
        rag_embedding_provider="OPENAI_COMPATIBLE",
        rag_embedding_api_key="",
        rag_vector_store_type="LOCAL_JSON",
        qwen_vision_enabled=False,
    )
    assert settings.rag_runtime_enabled is False


def test_analysis_kb_requires_rag_runtime_enabled():
    with pytest.raises(ValueError) as exc_info:
        Settings(
            rag_runtime_enabled=False,
            analysis_kb_enhancement_enabled=True,
            rag_vector_store_type="LOCAL_JSON",
        )
    assert "CG_ANALYSIS_KB_ENHANCEMENT_ENABLED=true requires CG_RAG_RUNTIME_ENABLED=true" in str(exc_info.value)


def test_real_image_chain_does_not_require_llm_embedding_credentials_when_kb_enhancement_disabled():
    settings = Settings(
        ai_runtime_mode="real",
        rag_runtime_enabled=True,
        analysis_kb_enhancement_enabled=False,
        llm_provider_code="OPENAI_COMPATIBLE",
        llm_base_url="",
        llm_api_key="",
        llm_enable_fallback_mock=False,
        rag_embedding_provider="OPENAI_COMPATIBLE",
        rag_embedding_base_url="",
        rag_embedding_api_key="",
        rag_vector_store_type="LOCAL_JSON",
        qwen_vision_enabled=False,
    )
    assert settings.analysis_kb_enhancement_enabled is False


def test_real_mode_with_rag_runtime_forbids_llm_mock_fallback():
    with pytest.raises(ValueError) as exc_info:
        Settings(
            ai_runtime_mode="real",
            rag_runtime_enabled=True,
            analysis_kb_enhancement_enabled=False,
            llm_enable_fallback_mock=True,
            qwen_vision_enabled=False,
        )
    assert "CG_LLM_ENABLE_FALLBACK_MOCK=true is forbidden" in str(exc_info.value)

