import pytest
from pathlib import Path
from app.infra.model.grading_classifier_model import GradingClassifierAdapter
from app.infra.model.base_model import ImplType


def _load_or_skip(adapter: GradingClassifierAdapter) -> None:
    try:
        adapter.load()
    except Exception as exc:
        if "checkpoint does not exist" in str(exc):
            pytest.skip(f"grading checkpoint not available: {exc}")
        raise


def test_grading_classifier_inference():
    adapter = GradingClassifierAdapter(confidence_threshold=0.5)
    _load_or_skip(adapter)
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    with pytest.raises(RuntimeError) as exc_info:
        adapter.infer(Path("test.png"))
    assert "failed to read image" in str(exc_info.value)

def test_grading_classifier_unload():
    adapter = GradingClassifierAdapter()
    _load_or_skip(adapter)
    adapter.unload()
    assert not adapter.is_loaded()
