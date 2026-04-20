import pytest
from unittest.mock import MagicMock, patch
from app.core.config import Settings
from app.infra.vector.openai_compatible_embedding_provider import OpenAiCompatibleEmbeddingProvider

@pytest.fixture
def settings():
    return Settings(
        rag_embedding_base_url="http://fake-embedding-api.com/v1",
        rag_embedding_api_key="fake-key",
        rag_embedding_model="text-embedding-3-small",
        rag_embedding_dimension=256,
        llm_api_key="fake-llm-key",
        llm_base_url="http://fake-llm",
        rag_embedding_version="v1"
    )

@pytest.fixture
def provider(settings):
    return OpenAiCompatibleEmbeddingProvider(settings)

def test_embed_single(provider):
    with patch('requests.post') as mock_post:
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "data": [
                {"index": 0, "embedding": [0.1] * 256}
            ]
        }
        mock_post.return_value = mock_response
        
        result = provider.embed("hello")
        assert len(result) == 256
        assert result[0] == 0.1

def test_embed_many(provider):
    with patch('requests.post') as mock_post:
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "data": [
                {"index": 0, "embedding": [0.1] * 256},
                {"index": 1, "embedding": [0.2] * 256}
            ]
        }
        mock_post.return_value = mock_response
        
        results = provider.embed_many(["hello", "world"])
        assert len(results) == 2
        assert results[0][0] == 0.1
        assert results[1][0] == 0.2

def test_dimension_mismatch(provider):
    with patch('requests.post') as mock_post:
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "data": [
                {"index": 0, "embedding": [0.1] * 128} # Mismatch
            ]
        }
        mock_post.return_value = mock_response
        
        with pytest.raises(ValueError, match="Embedding dimension mismatch"):
            provider.embed("hello")

def test_empty_data(provider):
    with patch('requests.post') as mock_post:
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"data": []}
        mock_post.return_value = mock_response
        
        with pytest.raises(RuntimeError, match="returned empty data"):
            provider.embed("hello")
