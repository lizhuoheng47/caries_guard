"""Quality check model adapter — HEURISTIC implementation.

Uses Pillow + numpy to compute image quality metrics:
- **Blur detection**: Laplacian variance via convolution kernel.
- **Exposure analysis**: Mean brightness and histogram spread.
- **Edge density**: Sobel-like gradient magnitude as a proxy for content integrity.

These are genuine image-statistics algorithms, NOT hard-coded mock values.
The ``impl_type`` is ``HEURISTIC`` to clearly distinguish from a trained ML model.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageFilter

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.quality")


class QualityHeuristicAdapter(BaseModelAdapter):
    """Heuristic quality-check adapter for Phase 5A."""

    model_code = "quality-check-heuristic-v1"
    model_type_code = "QUALITY"
    impl_type = ImplType.HEURISTIC

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    # ── lifecycle ────────────────────────────────────────────────────────

    def load(self) -> None:
        log.info("loading quality heuristic adapter model_code=%s", self.model_code)
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    # ── inference ────────────────────────────────────────────────────────

    def infer(self, image_path: Path) -> dict[str, Any]:
        """Analyse *image_path* and return a quality assessment dict.

        Returns
        -------
        dict with keys:
            qualityStatusCode   — ``"PASS"`` or ``"FAIL"``
            qualityScore        — float 0‒1
            blurScore           — float 0‒1
            exposureScore       — float 0‒1
            edgeDensityScore    — float 0‒1
            issues              — list[str]
            implType            — ``"HEURISTIC"``
            rawResult           — dict with intermediate numeric values
        """
        img = Image.open(image_path).convert("L")  # grayscale
        arr = np.asarray(img, dtype=np.float64)

        blur_score, laplacian_var = self._compute_blur(arr)
        exposure_score, mean_brightness, std_brightness = self._compute_exposure(arr)
        edge_score, edge_density = self._compute_edge_density(img, arr)

        issues: list[str] = []
        if blur_score < self._confidence_threshold:
            issues.append("HIGH_BLUR")
        if exposure_score < self._confidence_threshold:
            if mean_brightness < 80:
                issues.append("LOW_EXPOSURE")
            else:
                issues.append("OVER_EXPOSURE")
        if edge_score < self._confidence_threshold:
            issues.append("LOW_CONTENT_INTEGRITY")

        quality_score = round(float(np.mean([blur_score, exposure_score, edge_score])), 4)
        status = "FAIL" if issues else "PASS"

        return {
            "qualityStatusCode": status,
            "qualityScore": quality_score,
            "blurScore": round(blur_score, 4),
            "exposureScore": round(exposure_score, 4),
            "edgeDensityScore": round(edge_score, 4),
            "issues": issues,
            "implType": ImplType.HEURISTIC.value,
            "rawResult": {
                "laplacianVar": round(float(laplacian_var), 4),
                "meanBrightness": round(float(mean_brightness), 4),
                "stdBrightness": round(float(std_brightness), 4),
                "edgeDensity": round(float(edge_density), 6),
            },
        }

    # ── private helpers ──────────────────────────────────────────────────

    @staticmethod
    def _compute_blur(arr: np.ndarray) -> tuple[float, float]:
        """Laplacian variance — higher means sharper."""
        kernel = np.array([[0, 1, 0], [1, -4, 1], [0, 1, 0]], dtype=np.float64)
        # Manual 2-D convolution on the interior (avoids scipy dependency).
        h, w = arr.shape
        if h < 3 or w < 3:
            return 0.5, 0.0
        laplacian = np.zeros((h - 2, w - 2), dtype=np.float64)
        for di in range(3):
            for dj in range(3):
                laplacian += kernel[di, dj] * arr[di : h - 2 + di, dj : w - 2 + dj]
        variance = float(np.var(laplacian))
        # Map variance to 0‒1 score via logistic-like curve.
        # Empirically, dental X-rays with var > 500 are sharp.
        score = min(1.0, variance / 1000.0)
        return score, variance

    @staticmethod
    def _compute_exposure(arr: np.ndarray) -> tuple[float, float, float]:
        """Mean brightness & spread — penalise extremes."""
        mean_b = float(np.mean(arr))
        std_b = float(np.std(arr))
        # Ideal mean ~120‒140 for dental radiographs.
        mean_penalty = 1.0 - abs(mean_b - 130.0) / 130.0
        mean_penalty = max(0.0, min(1.0, mean_penalty))
        # Low std → flat / washed-out image.
        std_penalty = min(1.0, std_b / 60.0)
        score = round((mean_penalty + std_penalty) / 2.0, 4)
        return score, mean_b, std_b

    @staticmethod
    def _compute_edge_density(img: Image.Image, arr: np.ndarray) -> tuple[float, float]:
        """Sobel-like edge density as proxy for content integrity."""
        edges = img.filter(ImageFilter.FIND_EDGES)
        edge_arr = np.asarray(edges, dtype=np.float64)
        total_pixels = max(1, edge_arr.size)
        strong = int(np.sum(edge_arr > 30))
        density = strong / total_pixels
        # Dental images typically have density 0.05‒0.25.
        score = min(1.0, density / 0.15)
        return round(score, 4), density
