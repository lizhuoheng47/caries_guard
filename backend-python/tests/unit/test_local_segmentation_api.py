from pathlib import Path
from types import SimpleNamespace

import numpy as np
from fastapi import FastAPI
from fastapi.testclient import TestClient
from PIL import Image

import app.api.v1.segment as segment_api
from app.pipelines.segmentation_pipeline import SegmentationResult


class FakeRegistry:
    def is_module_real(self, _module_name: str) -> bool:
        return True


class FakePipeline:
    def segment(self, _image, _image_path: Path, _detections, output_dir: Path) -> SegmentationResult:
        paths = {
            name: output_dir / f"{name}_unknown.png"
            for name in ("mask", "overlay", "heatmap")
        }
        for path in paths.values():
            Image.fromarray(np.zeros((8, 8), dtype=np.uint8), mode="L").save(path)
        return SegmentationResult(
            segmentation_mode="real",
            segmentation_impl_type="ML_MODEL",
            regions=[],
            mask_path=paths["mask"],
            overlay_path=paths["overlay"],
            heatmap_path=paths["heatmap"],
            raw_result={
                "modelCode": "test-unet",
                "device": "cpu",
                "inferenceMode": "sliding_window",
                "maskThreshold": 0.7,
                "segmentationScore": 0.0,
            },
        )


class FakeStorage:
    def ensure_bucket(self, _bucket_name: str) -> None:
        return None

    def upload_file(self, _bucket_name: str, _object_key: str, path: Path, _content_type: str) -> None:
        assert path.is_file()

    def presigned_get_url(self, bucket_name: str, object_key: str, _expires_seconds: int) -> str:
        return f"http://127.0.0.1:9000/{bucket_name}/{object_key}?signed=test"


def test_raw_image_endpoint_returns_segmentation_only(monkeypatch, tmp_path: Path) -> None:
    settings = SimpleNamespace(
        local_segmentation_api_enabled=True,
        local_segmentation_api_max_bytes=1024,
        local_segmentation_api_output_dir=str(tmp_path),
        local_segmentation_asset_ttl_seconds=3600,
        bucket_visual="caries-visual",
        segmentation_asset_url_expiry_seconds=900,
    )
    runtime = SimpleNamespace(
        settings=settings,
        model_registry=FakeRegistry(),
        segmentation_pipeline=FakePipeline(),
        storage=FakeStorage(),
    )
    monkeypatch.setattr(segment_api, "get_local_segmentation_runtime", lambda: runtime)
    app = FastAPI()
    app.include_router(segment_api.router, prefix="/ai/v1")

    response = TestClient(app).post(
        "/ai/v1/segment",
        content=b"not-decoded-by-the-fake-pipeline",
        headers={"Content-Type": "image/png"},
    )

    assert response.status_code == 200
    data = response.json()["data"]
    assert data["implementationType"] == "ML_MODEL"
    assert data["regionCount"] == 0
    assert "gradingLabel" not in data
    assert "riskLevel" not in data
    request_dir = tmp_path / data["requestId"]
    assert not request_dir.exists()
    assert data["assetUrlExpiresInSeconds"] == 900
    assert data["assets"]["overlayUrl"].startswith("http://127.0.0.1:9000/caries-visual/")
