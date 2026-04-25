from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np


@dataclass(frozen=True)
class LoadedImage:
    path: Path
    source_format: str
    pixels: np.ndarray
    width: int
    height: int
    channels: int


def load_image(path: str | Path) -> LoadedImage:
    target = Path(path)
    suffix = target.suffix.lower()
    if suffix in {".dcm", ".dicom"}:
        return _load_dicom(target)
    return _load_raster(target)


def resize_image(image: np.ndarray, size: tuple[int, int], interpolation: str = "bilinear") -> np.ndarray:
    cv2 = _cv2()
    flag = {
        "nearest": cv2.INTER_NEAREST,
        "bicubic": cv2.INTER_CUBIC,
        "bilinear": cv2.INTER_LINEAR,
        "area": cv2.INTER_AREA,
    }.get(str(interpolation or "bilinear").strip().lower(), cv2.INTER_LINEAR)
    width, height = int(size[0]), int(size[1])
    return cv2.resize(image, (width, height), interpolation=flag)


def normalize_image(
    image: np.ndarray,
    *,
    min_value: float = 0.0,
    max_value: float = 255.0,
    normalize_mode: str = "minmax_0_1",
    mean: list[float] | None = None,
    std: list[float] | None = None,
    channels: int = 1,
) -> np.ndarray:
    arr = np.asarray(image, dtype=np.float32)
    arr = np.clip(arr, min_value, max_value)
    mode = str(normalize_mode or "minmax_0_1").strip().lower()
    if mode == "minmax_0_1":
        scale = max(max_value - min_value, 1e-6)
        arr = (arr - min_value) / scale
    elif mode not in {"none", ""}:
        raise RuntimeError(f"unsupported normalize mode: {normalize_mode}")

    channel_mean = _channel_values(mean, channels, 0.0)
    channel_std = np.maximum(_channel_values(std, channels, 1.0), 1e-6)
    if channels == 1:
        return (arr - channel_mean[0]) / channel_std[0]
    return (arr - channel_mean.reshape(1, 1, channels)) / channel_std.reshape(1, 1, channels)


def ensure_channel_dim(image: np.ndarray, channels: int) -> np.ndarray:
    arr = np.asarray(image)
    if channels == 1:
        if arr.ndim == 3:
            arr = arr[:, :, 0]
        return arr[:, :, None]
    if arr.ndim == 2:
        return np.repeat(arr[:, :, None], channels, axis=2)
    return arr


def png_bytes(image: np.ndarray) -> bytes:
    cv2 = _cv2()
    ok, encoded = cv2.imencode(".png", image)
    if not ok:
        raise RuntimeError("failed to encode png bytes")
    return encoded.tobytes()


def _load_raster(path: Path) -> LoadedImage:
    cv2 = _cv2()
    pixels = cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
    if pixels is None:
        raise RuntimeError(f"failed to read image: {path}")
    if pixels.ndim == 3:
        if pixels.shape[2] == 4:
            pixels = cv2.cvtColor(pixels, cv2.COLOR_BGRA2GRAY)
        else:
            pixels = cv2.cvtColor(pixels, cv2.COLOR_BGR2GRAY)
    pixels = _to_uint8(pixels)
    height, width = pixels.shape[:2]
    channels = 1 if pixels.ndim == 2 else pixels.shape[2]
    return LoadedImage(
        path=path,
        source_format=path.suffix.lower().lstrip(".") or "image",
        pixels=pixels,
        width=width,
        height=height,
        channels=channels,
    )


def _load_dicom(path: Path) -> LoadedImage:
    try:
        import pydicom
    except ImportError as exc:
        raise RuntimeError(
            "pydicom is required for DICOM ingestion. Add it to the backend-python environment first."
        ) from exc

    dataset = pydicom.dcmread(str(path))
    pixels = dataset.pixel_array.astype(np.float32)
    if pixels.ndim > 2:
        pixels = np.squeeze(pixels)
    pixels = _dicom_to_uint8(dataset, pixels)
    height, width = pixels.shape[:2]
    return LoadedImage(
        path=path,
        source_format="dicom",
        pixels=pixels,
        width=width,
        height=height,
        channels=1,
    )


def _dicom_to_uint8(dataset: Any, pixels: np.ndarray) -> np.ndarray:
    center = _first_number(getattr(dataset, "WindowCenter", None))
    width = _first_number(getattr(dataset, "WindowWidth", None))
    arr = pixels.astype(np.float32)
    if width is not None and width > 0 and center is not None:
        lower = center - width / 2.0
        upper = center + width / 2.0
        arr = np.clip(arr, lower, upper)
    arr = arr - float(np.min(arr))
    scale = float(np.max(arr))
    if scale > 0:
        arr = arr / scale
    arr = np.clip(arr * 255.0, 0.0, 255.0).astype(np.uint8)
    photometric = str(getattr(dataset, "PhotometricInterpretation", "") or "").upper()
    if photometric == "MONOCHROME1":
        arr = 255 - arr
    return arr


def _to_uint8(image: np.ndarray) -> np.ndarray:
    arr = np.asarray(image)
    if arr.dtype == np.uint8:
        return arr
    arr = arr.astype(np.float32)
    arr = arr - float(np.min(arr))
    scale = float(np.max(arr))
    if scale > 0:
        arr = arr / scale
    return np.clip(arr * 255.0, 0.0, 255.0).astype(np.uint8)


def _channel_values(raw: list[float] | None, channels: int, default: float) -> np.ndarray:
    values = [float(default)]
    if isinstance(raw, list) and raw:
        values = [float(item) for item in raw]
    while len(values) < channels:
        values.append(values[-1])
    return np.asarray(values[:channels], dtype=np.float32)


def _first_number(value: Any) -> float | None:
    if isinstance(value, (list, tuple)):
        if not value:
            return None
        value = value[0]
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _cv2():
    try:
        import cv2
    except ImportError as exc:
        raise RuntimeError(
            "opencv-python-headless is required for image preprocessing. "
            "Install it in the backend-python environment first."
        ) from exc
    return cv2
