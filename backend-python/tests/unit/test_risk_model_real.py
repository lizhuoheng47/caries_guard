import sys
from unittest.mock import MagicMock

# Mock missing dependencies
sys.modules["pydantic_settings"] = MagicMock()

import pytest
from app.core.config import Settings
from app.infra.model.risk_ml_fusion_model import RiskMlFusionAdapter
from app.infra.model.base_model import ImplType

@pytest.fixture
def dummy_settings():
    return Settings(
        llm_api_key="key",
        llm_base_url="http://fake",
        rag_embedding_api_key="key",
        rag_embedding_base_url="http://fake"
    )

@pytest.fixture
def mock_grading_result():
    res = MagicMock()
    res.grading_label = "C3"
    res.uncertainty_score = 0.5
    res.needs_review = True
    return res

def test_risk_ml_fusion_guardrail(dummy_settings, mock_grading_result):
    adapter = RiskMlFusionAdapter(confidence_threshold=0.5, settings=dummy_settings)
    adapter.load()
    
    # C3 with high uncertainty should trigger guardrails
    result = adapter.infer(
        patient_profile=None,
        grading_result=mock_grading_result,
        segmentation_regions=[{"bbox": [0,0,10,10]}],
        tooth_detection_count=1
    )
    
    assert result["implType"] == "ML_MODEL"
    assert result["riskLevelCode"] == "HIGH"
    assert result["reviewSuggested"] is True
    assert "GUARDRAIL" in result["rawResult"]["guardrailReason"]

def test_risk_ml_fusion_unload(dummy_settings):
    adapter = RiskMlFusionAdapter(settings=dummy_settings)
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
