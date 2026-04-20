import pytest
from pathlib import Path
from app.infra.model.tooth_detector_yolo import ToothDetectorYoloAdapter
from app.infra.model.base_model import ImplType

def test_tooth_detector_yolo_inference():
    adapter = ToothDetectorYoloAdapter(confidence_threshold=0.5)
    adapter.load()
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    with pytest.raises(RuntimeError) as exc_info:
        adapter.infer(Path("test.png"))
    assert "not implemented" in str(exc_info.value)

def test_tooth_detector_yolo_unload():
    adapter = ToothDetectorYoloAdapter()
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
