import sys
from unittest.mock import MagicMock

# Mock missing dependencies
sys.modules["neo4j"] = MagicMock()

import pytest
from unittest.mock import MagicMock
from app.core.config import Settings
from app.services.graph_retriever import GraphRetriever
from app.services.cypher_template_registry import CypherTemplate

@pytest.fixture
def mock_registry():
    registry = MagicMock()
    template = CypherTemplate(
        code="TEST_CODE",
        purpose="Testing",
        required_entity_types=("TestType",),
        return_schema=("n", "chunk", "doc"),
        parameter_name="name",
        cypher="MATCH (n) RETURN n"
    )
    registry.matching_templates.return_value = [template]
    registry.get.return_value = template # for fallback
    return registry

@pytest.fixture
def mock_driver():
    driver = MagicMock()
    session = MagicMock()
    driver.session.return_value.__enter__.return_value = session
    return driver, session

@pytest.fixture
def settings():
    return Settings(
        neo4j_database="neo4j",
        llm_api_key="key",
        llm_base_url="http://fake",
        rag_embedding_api_key="key",
        rag_embedding_base_url="http://fake"
    )

def test_retrieve_success(settings, mock_driver, mock_registry):
    driver, session = mock_driver
    # Return 1 record for template, and empty for fallback
    session.run.return_value.data.side_effect = [
        [
            {
                "n": {"name": "Test Concept"},
                "chunk": {"chunkId": "101", "confidenceScore": 0.8},
                "doc": {"docId": "201", "title": "Test Doc"}
            }
        ],
        [] # Fallback call
    ]
    
    retriever = GraphRetriever(settings, driver, mock_registry)
    entities = [{"entity_name": "Test", "entity_type_code": "TestType"}]
    results = retriever.retrieve(entities, "query", 5)
    
    assert len(results) == 1
    assert results[0]["doc_id"] == 201
    assert results[0]["retrieval_quality"] == "HIGH"
    assert "NO_PROVENANCE" not in results[0]["quality_tags"]

def test_retrieve_fallback(settings, mock_driver, mock_registry):
    driver, session = mock_driver
    # First call (template) returns empty, second (fallback) returns result
    session.run.return_value.data.side_effect = [
        [], # Template hit
        [   # Fallback hit
            {
                "n": {"name": "Fallback Concept"},
                "chunk": {"chunkId": "102"},
                "doc": {"docId": "202"}
            }
        ]
    ]
    
    retriever = GraphRetriever(settings, driver, mock_registry)
    entities = [{"entity_name": "Test", "entity_type_code": "TestType"}]
    results = retriever.retrieve(entities, "query", 5)
    
    assert len(results) == 1
    assert results[0]["graph_fallback"] is True
    assert results[0]["retrieval_quality"] == "DEGRADED"

def test_missing_provenance_penalty(settings, mock_driver, mock_registry):
    driver, session = mock_driver
    session.run.return_value.data.return_value = [
        {
            "n": {"name": "No Doc Concept"},
            "chunk": {}, # Missing chunkId
            "doc": {}    # Missing docId
        }
    ]
    
    retriever = GraphRetriever(settings, driver, mock_registry)
    entities = [{"entity_name": "Test", "entity_type_code": "TestType"}]
    results = retriever.retrieve(entities, "query", 5)
    
    assert results[0]["retrieval_quality"] == "UNVERIFIED"
    assert "NO_PROVENANCE" in results[0]["quality_tags"]
    # Score should be penalized (1.0 - 0.2 = 0.8 roughly)
    assert results[0]["score"] < 1.0
