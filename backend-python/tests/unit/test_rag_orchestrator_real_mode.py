import sys
from unittest.mock import MagicMock

# Mock missing dependencies to allow import
sys.modules["opensearchpy"] = MagicMock()
sys.modules["opensearchpy.helpers"] = MagicMock()
sys.modules["neo4j"] = MagicMock()

import pytest
from unittest.mock import MagicMock
from app.core.config import Settings
from app.services.rag_orchestrator import RagOrchestrator

@pytest.fixture
def mock_deps():
    return {
        "rag_repository": MagicMock(),
        "knowledge_repository": MagicMock(),
        "llm_client": MagicMock(),
        "query_rewrite_service": MagicMock(),
        "intent_classifier_service": MagicMock(),
        "entity_linking_service": MagicMock(),
        "lexical_retriever": MagicMock(),
        "dense_retriever": MagicMock(),
        "graph_retriever": MagicMock(),
        "fusion_service": MagicMock(),
        "rerank_service": MagicMock(),
        "citation_assembler": MagicMock(),
        "refusal_policy_service": MagicMock(),
        "answer_validator_service": MagicMock(),
    }

def test_real_mode_fail_fast_llm_mock(mock_deps):
    # This should fail at Settings level first
    with pytest.raises(ValueError, match="forbids CG_LLM_PROVIDER_CODE='MOCK'"):
        Settings(
            ai_runtime_mode="real",
            llm_provider_code="MOCK",
            rag_embedding_provider="OPENAI_COMPATIBLE",
            llm_base_url="http://fake",
            llm_api_key="key",
            rag_embedding_base_url="http://fake",
            rag_embedding_api_key="key"
        )

def test_real_mode_fail_fast_embedding_mock(mock_deps):
    # This should fail at Settings level first
    with pytest.raises(ValueError, match="forbids CG_RAG_EMBEDDING_PROVIDER='HASHING'"):
        Settings(
            ai_runtime_mode="real",
            llm_provider_code="OPENAI_COMPATIBLE",
            rag_embedding_provider="HASHING",
            llm_base_url="http://fake",
            llm_api_key="key",
            rag_embedding_base_url="http://fake",
            rag_embedding_api_key="key"
        )

def test_orchestrator_gate_fail_fast_llm_mock(mock_deps):
    # Manually bypass Settings validation by mocking ai_runtime_mode after creation
    settings = Settings(
        ai_runtime_mode="hybrid", # pass validation
        llm_provider_code="MOCK",
        rag_embedding_provider="OPENAI_COMPATIBLE",
        llm_base_url="http://fake",
        llm_api_key="key",
        rag_embedding_base_url="http://fake",
        rag_embedding_api_key="key"
    )
    # Force real mode on the object to test Orchestrator gate
    object.__setattr__(settings, "ai_runtime_mode", "real")
    
    orchestrator = RagOrchestrator(settings, **mock_deps)
    
    with pytest.raises(RuntimeError, match="requires a real LLM provider"):
        orchestrator.answer(scene="QA", question="test", kb_code="test", related_biz_no=None, patient_uuid=None, java_user_id=None, org_id=None, trace_id=None, context_text=None)

def test_hybrid_mode_allows_mock(mock_deps):
    # Setup mock to return something
    mock_deps["knowledge_repository"].get_knowledge_base.return_value = {"kb_code": "test", "knowledge_version": "v1"}
    mock_deps["rag_repository"].create_rag_session.return_value = {"id": 1, "session_no": "S1"}
    mock_deps["rag_repository"].create_rag_request.return_value = {"id": 1, "request_no": "R1"}
    mock_deps["lexical_retriever"].retrieve.return_value = []
    mock_deps["dense_retriever"].retrieve.return_value = []
    mock_deps["graph_retriever"].retrieve.return_value = []
    mock_deps["fusion_service"].fuse.return_value = []
    mock_deps["rerank_service"].rerank.return_value = []
    mock_deps["citation_assembler"].evidence.return_value = []
    mock_deps["citation_assembler"].citations.return_value = []
    mock_deps["citation_assembler"].retrieved_chunks.return_value = []
    mock_deps["citation_assembler"].graph_evidence.return_value = []
    mock_deps["refusal_policy_service"].evaluate.return_value = "INSUFFICIENT_EVIDENCE"
    mock_deps["answer_validator_service"].validate.return_value = []

    settings = Settings(
        ai_runtime_mode="hybrid",
        llm_provider_code="MOCK",
        rag_embedding_provider="HASHING",
        # Provide these even if hybrid to avoid general _require_non_empty errors if I added them to hybrid (usually not)
    )
    orchestrator = RagOrchestrator(settings, **mock_deps)
    
    result = orchestrator.answer(scene="QA", question="test", kb_code="test", related_biz_no=None, patient_uuid=None, java_user_id=None, org_id=None, trace_id=None, context_text=None)
    assert result["answer"] is not None
