import pytest
from pathlib import Path
from app.infra.model.quality_cnn_model import QualityCnnAdapter
from app.infra.model.base_model import ImplType

def test_quality_cnn_adapter_inference():
    adapter = QualityCnnAdapter(confidence_threshold=0.5)
    adapter.load()
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    
    result = adapter.infer(Path("test.png"))
    
    assert "qualityStatusCode" in result
    assert "qualityScore" in result
    assert result["implType"] == "ML_MODEL"
    assert "softmaxOutput" in result["rawResult"]

def test_quality_cnn_adapter_unload():
    adapter = QualityCnnAdapter()
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
