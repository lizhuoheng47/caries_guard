import sys
from unittest.mock import MagicMock

# Mock missing dependencies
sys.modules["neo4j"] = MagicMock()

import pytest
from unittest.mock import MagicMock
from app.core.config import Settings
from app.services.graph_upsert_service import GraphUpsertService

@pytest.fixture
def mock_repo():
    repo = MagicMock()
    repo.replace_entities_and_relations.return_value = ([], [])
    repo.normalize.side_effect = lambda x: x.lower()
    return repo

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

def test_sync_document_graph_stats(settings, mock_driver, mock_repo):
    driver, session = mock_driver
    service = GraphUpsertService(settings, driver, mock_repo)
    
    entities = [
        {"concept_id": "C1", "canonical_name": "Caries", "entity_type_code": "Disease", "normalized_name": "caries", "entity_key": "K1"}
    ]
    relations = []
    
    stats = service.sync_document_graph(
        doc_id=1,
        doc_title="Doc",
        version_no="v1",
        chunk_entities=entities,
        relations=relations,
        org_id=None,
        created_by=None
    )
    
    assert stats["docId"] == 1
    assert stats["conceptCount"] == 1
    assert stats["status"] == "SUCCESS"
    assert session.run.called

def test_detect_conflicts(settings, mock_driver, mock_repo):
    driver, session = mock_driver
    service = GraphUpsertService(settings, driver, mock_repo)
    
    session.run.return_value.data.return_value = [
        {"alias": "tooth decay", "conceptNames": ["Caries", "Cavity"]}
    ]
    
    conflicts = service.detect_document_graph_conflicts(doc_id=1)
    assert len(conflicts) == 1
    assert conflicts[0]["type"] == "ALIAS_COLLISION"
    assert conflicts[0]["alias"] == "tooth decay"
