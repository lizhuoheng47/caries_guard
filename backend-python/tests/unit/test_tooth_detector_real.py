import pytest
from pathlib import Path
from app.infra.model.tooth_detector_yolo import ToothDetectorYoloAdapter
from app.infra.model.base_model import ImplType

def test_tooth_detector_yolo_inference():
    adapter = ToothDetectorYoloAdapter(confidence_threshold=0.5)
    adapter.load()
    
    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL
    
    result = adapter.infer(Path("test.png"))
    
    assert "detections" in result
    assert len(result["detections"]) > 0
    assert result["implType"] == "ML_MODEL"
    
    det = result["detections"][0]
    assert "toothCode" in det
    assert len(det["bbox"]) == 4

def test_tooth_detector_yolo_unload():
    adapter = ToothDetectorYoloAdapter()
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
