import pytest
from pathlib import Path
from app.infra.model.grading_model_adapter import GradingModelAdapter
from app.infra.model.base_model import ImplType


def _load_or_skip(adapter: GradingModelAdapter) -> None:
    try:
        adapter.load()
    except Exception as exc:
        if "checkpoint does not exist" in str(exc):
            pytest.skip(f"grading checkpoint not available: {exc}")
        raise


def test_grading_model_inference():
    adapter = GradingModelAdapter(confidence_threshold=0.5)
    _load_or_skip(adapter)
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    with pytest.raises(RuntimeError) as exc_info:
        adapter.infer(Path("test.png"))
    assert "failed to read image" in str(exc_info.value)

def test_grading_model_unload():
    adapter = GradingModelAdapter()
    _load_or_skip(adapter)
    adapter.unload()
    assert not adapter.is_loaded()
