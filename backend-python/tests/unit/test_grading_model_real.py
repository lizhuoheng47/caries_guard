import pytest
from pathlib import Path
from app.infra.model.grading_classifier_model import GradingClassifierAdapter
from app.infra.model.base_model import ImplType

def test_grading_classifier_inference():
    adapter = GradingClassifierAdapter(confidence_threshold=0.5)
    adapter.load()
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    
    result = adapter.infer(Path("test.png"))
    
    assert "gradingLabel" in result
    assert result["gradingLabel"] in ("C0", "C1", "C2", "C3")
    assert "confidenceScore" in result
    assert "uncertaintyScore" in result
    assert result["implType"] == "ML_MODEL"
    assert "temperature" in result["rawResult"]

def test_grading_classifier_unload():
    adapter = GradingClassifierAdapter()
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
