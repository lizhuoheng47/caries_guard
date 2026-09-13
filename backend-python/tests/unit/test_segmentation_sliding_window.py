from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.infra.model.segmentation_model_adapter import (
    SegmentationModelAdapter,
    _PreprocessedImage,
)
from app.pipelines.segmentation_pipeline import SegmentationPipeline
from app.schemas.request import ImageInput


def test_window_positions_cover_the_final_edge() -> None:
    positions = SegmentationModelAdapter._window_positions(900, 512, 0.25)

    assert positions == [0, 384, 388]
    assert positions[-1] + 512 == 900


def test_gaussian_window_is_positive_and_center_weighted() -> None:
    weight = SegmentationModelAdapter._gaussian_window(512, 512)

    assert weight.shape == (512, 512)
    assert float(weight.min()) > 0.0
    assert float(weight[256, 256]) > float(weight[0, 0])


def test_sliding_window_fusion_preserves_identity_model_probability() -> None:
    rng = np.random.default_rng(42)
    tensor = rng.normal(size=(1, 1, 700, 900)).astype(np.float32)
    adapter = SegmentationModelAdapter()
    adapter._run_model = lambda batch: batch  # type: ignore[method-assign]
    preprocessed = _PreprocessedImage(
        tensor=tensor,
        original_size=(900, 700),
        model_size=(512, 512),
        channel_count=1,
        layout="NCHW",
        normalize_mode="test",
        inference_mode="sliding_window",
        sliding_window_overlap=0.25,
        sliding_window_batch_size=2,
    )

    probability, window_count = adapter._sliding_window_probability(preprocessed, 1)
    expected = 1.0 / (1.0 + np.exp(-tensor[0, 0]))

    assert window_count == 6
    np.testing.assert_allclose(probability, expected, rtol=1e-6, atol=1e-6)


def test_preprocess_keeps_original_size_and_uses_percentiles(tmp_path: Path) -> None:
    pixels = np.tile(np.arange(64, dtype=np.uint8), (32, 1)) * 4
    image_path = tmp_path / "panoramic.png"
    Image.fromarray(pixels, mode="L").save(image_path)
    assets = ModelAssets(Settings())
    adapter = SegmentationModelAdapter(model_assets=assets, settings=Settings())

    preprocessed = adapter._preprocess_image(image_path, assets)

    assert preprocessed.inference_mode == "sliding_window"
    assert preprocessed.original_size == (64, 32)
    assert preprocessed.model_size == (512, 512)
    assert preprocessed.tensor.shape == (1, 1, 32, 64)
    assert preprocessed.normalize_mode == "percentile_1_99+minmax_0_1"
    assert float(preprocessed.tensor.min()) >= -1.0
    assert float(preprocessed.tensor.max()) <= 1.0


def test_connected_components_returns_each_region_once() -> None:
    mask = np.zeros((20, 30), dtype=bool)
    mask[2:5, 3:7] = True
    mask[10:15, 20:29] = True

    components = SegmentationModelAdapter._connected_components(mask)

    assert sorted(len(component) for component in components) == [12, 45]


def test_segmentation_pipeline_accepts_a_valid_empty_mask(tmp_path: Path) -> None:
    image_path = tmp_path / "healthy.png"
    Image.fromarray(np.full((32, 64), 128, dtype=np.uint8), mode="L").save(image_path)
    settings = Settings()
    assets = ModelAssets(settings)
    pipeline = SegmentationPipeline(None, settings, assets)  # type: ignore[arg-type]

    result = pipeline._real_result(
        ImageInput(image_id=1),
        image_path,
        tmp_path / "outputs",
        {
            "maskArray": np.zeros((32, 64), dtype=np.uint8),
            "regions": [],
            "segmentationScore": 0.0,
            "implType": "ML_MODEL",
            "rawResult": {"modelCode": "empty-mask-test"},
        },
    )

    assert result.regions == []
    assert result.mask_path.is_file()
    assert result.overlay_path.is_file()
    assert result.heatmap_path.is_file()
