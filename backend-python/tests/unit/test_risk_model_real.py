from unittest.mock import MagicMock

import pytest

from app.core.config import Settings
from app.infra.model.base_model import ImplType
from app.infra.model.risk_ml_fusion_model import RiskMlFusionAdapter


@pytest.fixture
def dummy_settings():
    return Settings(
        llm_api_key="key",
        llm_base_url="http://fake",
        rag_embedding_api_key="key",
        rag_embedding_base_url="http://fake",
    )


def test_risk_ml_fusion_inference_fails_explicitly(dummy_settings):
    adapter = RiskMlFusionAdapter(confidence_threshold=0.5, settings=dummy_settings)
    adapter.load()
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL

    with pytest.raises(RuntimeError) as exc_info:
        adapter.infer(
            patient_profile=None,
            grading_result=MagicMock(),
            segmentation_regions=[{"bbox": [0, 0, 10, 10]}],
            tooth_detection_count=1,
        )
    assert "not implemented" in str(exc_info.value)


def test_risk_ml_fusion_unload(dummy_settings):
    adapter = RiskMlFusionAdapter(settings=dummy_settings)
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
