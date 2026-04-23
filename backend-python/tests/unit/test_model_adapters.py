"""Tests for model adapters — output structure validation."""

from pathlib import Path
import numpy as np
import pytest
from PIL import Image

from app.infra.model.base_model import ImplType
from app.infra.model.tooth_detector import ToothDetectorAdapter, _fdi_code
from app.quality.quality_adapter import QualityAssessmentAdapter


@pytest.fixture()
def sample_image(tmp_path: Path) -> Path:
    """Create a realistic-ish test image (gradient with noise)."""
    arr = np.random.randint(60, 200, (256, 512), dtype=np.uint8)
    img = Image.fromarray(arr, mode="L")
    path = tmp_path / "test_dental.png"
    img.save(path)
    return path


# ── QualityModelAdapter ─────────────────────────────────────────────────


class TestQualityAssessmentAdapter:
    def test_impl_type_is_heuristic(self):
        adapter = QualityAssessmentAdapter()
        assert adapter.impl_type == ImplType.HEURISTIC
        assert adapter.model_type_code == "QUALITY"

    def test_lifecycle(self):
        adapter = QualityAssessmentAdapter()
        assert not adapter.is_loaded()
        adapter.load()
        assert adapter.is_loaded()
        adapter.unload()
        assert not adapter.is_loaded()

    def test_infer_returns_required_keys(self, sample_image: Path):
        adapter = QualityAssessmentAdapter()
        adapter.load()
        result = adapter.infer(sample_image)

        assert "qualityStatusCode" in result
        assert result["qualityStatusCode"] in {"PASS", "WARN", "FAIL"}
        assert "qualityScore" in result
        assert 0.0 <= result["qualityScore"] <= 1.0
        assert "blurScore" in result
        assert "exposureScore" in result
        assert "integrityScore" in result
        assert "qualityIssues" in result
        assert isinstance(result["qualityIssues"], list)
        assert "retakeSuggested" in result
        assert "modelVersion" in result
        assert "inferenceMillis" in result
        assert result["implType"] == "HEURISTIC"
        assert "rawResult" in result
        assert "issueConfidences" in result["rawResult"]

    def test_infer_blurry_image_detects_blur(self, tmp_path: Path):
        """A uniform gray image should be flagged as blurry."""
        arr = np.full((256, 512), 128, dtype=np.uint8)
        img = Image.fromarray(arr, mode="L")
        path = tmp_path / "uniform.png"
        img.save(path)

        adapter = QualityAssessmentAdapter(confidence_threshold=0.3)
        adapter.load()
        result = adapter.infer(path)
        assert "blur" in result["qualityIssues"]

    def test_info_dict(self):
        adapter = QualityAssessmentAdapter()
        adapter.load()
        info = adapter.info()
        assert info["modelCode"] == "quality-assessment-cv-v2"
        assert info["implType"] == "HEURISTIC"
        assert info["loaded"] is True


# ── ToothDetectorAdapter ────────────────────────────────────────────────


class TestToothDetectorAdapter:
    def test_impl_type_is_heuristic(self):
        adapter = ToothDetectorAdapter()
        assert adapter.impl_type == ImplType.HEURISTIC
        assert adapter.model_type_code == "DETECTION"

    def test_lifecycle(self):
        adapter = ToothDetectorAdapter()
        assert not adapter.is_loaded()
        adapter.load()
        assert adapter.is_loaded()
        adapter.unload()
        assert not adapter.is_loaded()

    def test_infer_returns_required_keys(self, sample_image: Path):
        adapter = ToothDetectorAdapter(confidence_threshold=0.1)
        adapter.load()
        result = adapter.infer(sample_image)

        assert "detections" in result
        assert isinstance(result["detections"], list)
        assert result["implType"] == "HEURISTIC"
        assert "rawResult" in result
        assert "gridSize" in result["rawResult"]

    def test_detections_have_fdi_mapping_fields(self, sample_image: Path):
        """Every detection must include arch/side/orderIndex/toothCode."""
        adapter = ToothDetectorAdapter(confidence_threshold=0.1)
        adapter.load()
        result = adapter.infer(sample_image)

        for det in result["detections"]:
            assert "arch" in det
            assert det["arch"] in {"upper", "lower"}
            assert "side" in det
            assert det["side"] in {"left", "right"}
            assert "orderIndex" in det
            assert isinstance(det["orderIndex"], int)
            assert "toothCode" in det
            assert len(det["toothCode"]) == 2
            assert "bbox" in det
            assert len(det["bbox"]) == 4
            assert "score" in det


# ── FDI mapping ─────────────────────────────────────────────────────────


class TestFdiMapping:
    def test_upper_right_mapping(self):
        assert _fdi_code("upper", "right", 0) == "18"
        assert _fdi_code("upper", "right", 7) == "11"

    def test_upper_left_mapping(self):
        assert _fdi_code("upper", "left", 0) == "21"
        assert _fdi_code("upper", "left", 7) == "28"

    def test_lower_left_mapping(self):
        assert _fdi_code("lower", "left", 0) == "31"

    def test_lower_right_mapping(self):
        assert _fdi_code("lower", "right", 0) == "48"

    def test_out_of_range_returns_xx(self):
        assert _fdi_code("upper", "right", 99) == "XX"

    def test_unknown_quadrant_returns_xx(self):
        assert _fdi_code("middle", "center", 0) == "XX"
