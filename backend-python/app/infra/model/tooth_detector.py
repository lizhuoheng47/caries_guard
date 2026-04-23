"""Heuristic tooth detection adapter."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageFilter

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.tooth-detector")

_FDI_MAP: dict[tuple[str, str], list[str]] = {
    ("upper", "right"): ["18", "17", "16", "15", "14", "13", "12", "11"],
    ("upper", "left"):  ["21", "22", "23", "24", "25", "26", "27", "28"],
    ("lower", "left"):  ["31", "32", "33", "34", "35", "36", "37", "38"],
    ("lower", "right"): ["48", "47", "46", "45", "44", "43", "42", "41"],
}


def _fdi_code(arch: str, side: str, order_index: int) -> str:
    """Map region metadata to an FDI tooth code."""
    codes = _FDI_MAP.get((arch, side), [])
    if 0 <= order_index < len(codes):
        return codes[order_index]
    return "XX"


class ToothDetectorHeuristicAdapter(BaseModelAdapter):
    """Heuristic tooth-detection adapter."""

    model_code = "tooth-detect-heuristic-v1"
    model_type_code = "DETECTION"
    impl_type = ImplType.HEURISTIC

    _GRID_COLS = 4
    _GRID_ROWS = 2

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        log.info("loading tooth detector heuristic adapter model_code=%s", self.model_code)
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(self, image_path: Path) -> dict[str, Any]:
        """Analyse image_path and return tooth detection results."""
        img = Image.open(image_path).convert("L")
        width, height = img.size
        arr = np.asarray(img, dtype=np.float64)

        cell_w = width // self._GRID_COLS
        cell_h = height // self._GRID_ROWS

        detections: list[dict[str, Any]] = []
        grid_scores: list[list[float]] = []

        for row in range(self._GRID_ROWS):
            row_scores: list[float] = []
            arch = "upper" if row == 0 else "lower"
            for col in range(self._GRID_COLS):
                x1 = col * cell_w
                y1 = row * cell_h
                x2 = min(x1 + cell_w, width)
                y2 = min(y1 + cell_h, height)

                cell = arr[y1:y2, x1:x2]
                score = self._cell_score(cell)
                row_scores.append(round(score, 4))

                if score >= self._confidence_threshold:
                    mid_col = self._GRID_COLS // 2
                    if col < mid_col:
                        side = "right" if row == 0 else "left"
                        order_index = mid_col - 1 - col
                    else:
                        side = "left" if row == 0 else "right"
                        order_index = col - mid_col

                    tooth_code = _fdi_code(arch, side, order_index)
                    detections.append({
                        "arch": arch,
                        "side": side,
                        "orderIndex": order_index,
                        "toothCode": tooth_code,
                        "bbox": [x1, y1, x2, y2],
                        "score": round(score, 4),
                    })

            grid_scores.append(row_scores)

        return {
            "detections": detections,
            "implType": ImplType.HEURISTIC.value,
            "rawResult": {
                "gridSize": [self._GRID_COLS, self._GRID_ROWS],
                "gridScores": grid_scores,
                "imageSize": [width, height],
            },
        }

    @staticmethod
    def _cell_score(cell: np.ndarray) -> float:
        """Score a grid cell based on texture energy and contrast.

        A cell that contains a tooth typically has higher local contrast
        and more edge energy than background / soft tissue.
        """
        if cell.size == 0:
            return 0.0
        std = float(np.std(cell))
        mean = float(np.mean(cell))
        contrast_score = min(1.0, std / 60.0)
        brightness_penalty = 1.0 - abs(mean - 130.0) / 130.0
        brightness_penalty = max(0.0, min(1.0, brightness_penalty))
        return (contrast_score * 0.7 + brightness_penalty * 0.3)


class ToothDetectorAdapter(ToothDetectorHeuristicAdapter):
    """Backward-compatible alias used by legacy tests/imports."""
