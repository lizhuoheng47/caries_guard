import pytest
from pathlib import Path
from app.infra.model.grading_classifier_model import GradingClassifierAdapter
from app.infra.model.base_model import ImplType

def test_grading_classifier_inference():
    adapter = GradingClassifierAdapter(confidence_threshold=0.5)
    adapter.load()
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    with pytest.raises(RuntimeError) as exc_info:
        adapter.infer(Path("test.png"))
    assert "not implemented" in str(exc_info.value)

def test_grading_classifier_unload():
    adapter = GradingClassifierAdapter()
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
