import pytest
from pathlib import Path
from app.infra.model.tooth_detector_yolo import ToothDetectorYoloAdapter
from app.infra.model.base_model import ImplType


def _load_or_skip(adapter: ToothDetectorYoloAdapter) -> None:
    try:
        adapter.load()
    except RuntimeError as exc:
        if "checkpoint is missing" in str(exc):
            pytest.skip(f"tooth detection checkpoint not available: {exc}")
        raise


def test_tooth_detector_yolo_inference():
    adapter = ToothDetectorYoloAdapter(confidence_threshold=0.5)
    _load_or_skip(adapter)
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    with pytest.raises(RuntimeError) as exc_info:
        adapter.infer(Path("test.png"))
    assert "failed to read image" in str(exc_info.value)

def test_tooth_detector_yolo_unload():
    adapter = ToothDetectorYoloAdapter()
    _load_or_skip(adapter)
    adapter.unload()
    assert not adapter.is_loaded()
