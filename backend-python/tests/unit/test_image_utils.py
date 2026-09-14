from pathlib import Path

import numpy as np

import app.core.image_utils as image_utils


class _SingleChannelCv2:
    IMREAD_UNCHANGED = -1

    @staticmethod
    def imread(_path: str, _mode: int) -> np.ndarray:
        return np.arange(6, dtype=np.uint8).reshape(2, 3, 1)


def test_load_raster_accepts_explicit_single_channel_array(monkeypatch) -> None:
    monkeypatch.setattr(image_utils, "_cv2", lambda: _SingleChannelCv2())

    loaded = image_utils.load_image(Path("single-channel.png"))

    assert loaded.pixels.shape == (2, 3)
    assert loaded.channels == 1
    assert loaded.width == 3
    assert loaded.height == 2
