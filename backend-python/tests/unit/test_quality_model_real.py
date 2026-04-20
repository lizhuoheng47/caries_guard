from pathlib import Path

import numpy as np
from PIL import Image

from app.infra.model.base_model import ImplType
from app.infra.model.quality_cnn_model import QualityCnnAdapter


def _sample_image(tmp_path: Path) -> Path:
    arr = np.random.default_rng(20260420).integers(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
    path = tmp_path / "quality-real.png"
    Image.fromarray(arr, mode="L").save(path)
    return path


def test_quality_cnn_adapter_inference_returns_real_result(tmp_path: Path):
    adapter = QualityCnnAdapter(confidence_threshold=0.5)
    adapter.load()

    assert adapter.is_loaded()
    assert adapter.impl_type == ImplType.ML_MODEL

    result = adapter.infer(_sample_image(tmp_path))
    assert result["implType"] == "ML_MODEL"
    assert result["qualityStatus"] in {"PASS", "WARN", "FAIL"}
    assert result["qualityStatusCode"] in {"PASS", "WARN", "FAIL"}
    assert 0.0 <= result["qualityScore"] <= 1.0
    assert isinstance(result["qualityIssues"], list)
    assert isinstance(result["retakeSuggested"], bool)
    assert isinstance(result["inferenceMillis"], int)
    assert result["inferenceMillis"] >= 0
    assert isinstance(result["modelVersion"], str)
    assert result["modelVersion"]
    assert "issueConfidences" in result["rawResult"]


def test_quality_cnn_adapter_unload():
    adapter = QualityCnnAdapter()
    adapter.load()
    adapter.unload()
    assert not adapter.is_loaded()
