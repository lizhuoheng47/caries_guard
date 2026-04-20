from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageFilter


_ISSUE_CODES = (
    "blur",
    "under_exposure",
    "over_exposure",
    "occlusion",
    "field_cutoff",
    "artifact",
)


@dataclass(frozen=True)
class QualityInferOutput:
    quality_status: str
    quality_score: float
    quality_issues: list[str]
    retake_suggested: bool
    issue_confidences: dict[str, float]
    sub_scores: dict[str, float]
    feature_vector: dict[str, float]
    model_version: str


def _sigmoid(value: float) -> float:
    return float(1.0 / (1.0 + np.exp(-value)))


def _clamp01(value: float) -> float:
    return float(max(0.0, min(1.0, value)))


def _safe_float(value: Any, default: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return float(default)


class QualityInferModel:
    """Real quality inference model based on multi-signal CV features."""

    def __init__(self, params: dict[str, Any]) -> None:
        self._params = params
        self._model_version = str(params.get("modelVersion") or "quality-assessment-cv-v2")
        self._fail_threshold = _safe_float(params.get("failThreshold"), 0.78)
        self._warn_threshold = _safe_float(params.get("warnThreshold"), 0.42)
        self._issue_threshold = _safe_float(params.get("issueThreshold"), 0.45)
        self._weights = {
            issue: _safe_float((params.get("issueWeights") or {}).get(issue), default)
            for issue, default in {
                "blur": 0.24,
                "under_exposure": 0.14,
                "over_exposure": 0.14,
                "occlusion": 0.19,
                "field_cutoff": 0.16,
                "artifact": 0.13,
            }.items()
        }

    @property
    def model_version(self) -> str:
        return self._model_version

    def infer(self, image_path: Path) -> QualityInferOutput:
        with Image.open(image_path) as image:
            gray = image.convert("L")
            arr = np.asarray(gray, dtype=np.float32)

        feature_vector = self._extract_features(gray, arr)
        issue_confidences = self._issue_confidences(feature_vector)
        quality_issues = self._select_quality_issues(issue_confidences)
        weighted_defect = sum(
            self._weights.get(issue, 0.0) * issue_confidences.get(issue, 0.0)
            for issue in _ISSUE_CODES
        )
        quality_score = round(_clamp01(1.0 - weighted_defect), 4)
        status, retake_suggested = self._status_and_retake(
            issue_confidences=issue_confidences,
            weighted_defect=weighted_defect,
        )

        sub_scores = {
            "blur": round(1.0 - issue_confidences["blur"], 4),
            "exposure": round(
                1.0 - max(issue_confidences["under_exposure"], issue_confidences["over_exposure"]),
                4,
            ),
            "integrity": round(1.0 - issue_confidences["field_cutoff"], 4),
            "occlusion": round(1.0 - issue_confidences["occlusion"], 4),
            "artifact": round(1.0 - issue_confidences["artifact"], 4),
        }
        return QualityInferOutput(
            quality_status=status,
            quality_score=quality_score,
            quality_issues=quality_issues,
            retake_suggested=retake_suggested,
            issue_confidences={k: round(v, 4) for k, v in issue_confidences.items()},
            sub_scores=sub_scores,
            feature_vector={k: round(v, 6) for k, v in feature_vector.items()},
            model_version=self._model_version,
        )

    def _extract_features(self, gray: Image.Image, arr: np.ndarray) -> dict[str, float]:
        height, width = arr.shape
        total_pixels = float(max(1, width * height))
        normalized = arr / 255.0

        laplacian_var = self._laplacian_variance(arr)
        tenengrad = self._tenengrad_focus(arr)
        entropy = self._entropy(arr)

        mean_intensity = float(np.mean(arr))
        std_intensity = float(np.std(arr))
        low_ratio = float(np.mean(arr <= 20))
        high_ratio = float(np.mean(arr >= 235))
        mid_ratio = float(np.mean((arr >= 40) & (arr <= 215)))

        center_mask = np.zeros_like(arr, dtype=bool)
        y1, y2 = int(height * 0.2), int(height * 0.8)
        x1, x2 = int(width * 0.2), int(width * 0.8)
        center_mask[y1:y2, x1:x2] = True
        center_dark_ratio = float(np.mean(arr[center_mask] <= 24)) if center_mask.any() else 0.0

        border_px = max(4, int(min(height, width) * 0.08))
        border_mask = np.zeros_like(arr, dtype=bool)
        border_mask[:border_px, :] = True
        border_mask[-border_px:, :] = True
        border_mask[:, :border_px] = True
        border_mask[:, -border_px:] = True
        border_dark_ratio = float(np.mean(arr[border_mask] <= 22))
        border_bright_ratio = float(np.mean(arr[border_mask] >= 232))

        edges = np.asarray(gray.filter(ImageFilter.FIND_EDGES), dtype=np.float32)
        edge_content_ratio = float(np.mean(edges >= 28))

        blurred = np.asarray(gray.filter(ImageFilter.MedianFilter(size=3)), dtype=np.float32)
        impulse_noise_ratio = float(np.mean(np.abs(arr - blurred) >= 48))

        cols = np.mean(normalized, axis=0)
        rows = np.mean(normalized, axis=1)
        col_diff = np.abs(np.diff(cols)).mean() if len(cols) > 1 else 0.0
        row_diff = np.abs(np.diff(rows)).mean() if len(rows) > 1 else 0.0
        stripe_anomaly = float(max(col_diff, row_diff))

        return {
            "laplacian_var_norm": _clamp01(np.log1p(laplacian_var) / 7.2),
            "tenengrad_norm": _clamp01(np.log1p(tenengrad) / 8.0),
            "entropy_norm": _clamp01(entropy / 8.0),
            "mean_norm": _clamp01(mean_intensity / 255.0),
            "contrast_norm": _clamp01(std_intensity / 70.0),
            "low_ratio": _clamp01(low_ratio),
            "high_ratio": _clamp01(high_ratio),
            "mid_ratio": _clamp01(mid_ratio),
            "center_dark_ratio": _clamp01(center_dark_ratio),
            "border_dark_ratio": _clamp01(border_dark_ratio),
            "border_bright_ratio": _clamp01(border_bright_ratio),
            "edge_content_ratio": _clamp01(edge_content_ratio),
            "impulse_noise_ratio": _clamp01(impulse_noise_ratio),
            "stripe_anomaly": _clamp01(stripe_anomaly * 7.0),
            "image_size_norm": _clamp01(total_pixels / (1024.0 * 1024.0)),
        }

    def _issue_confidences(self, fv: dict[str, float]) -> dict[str, float]:
        blur = _sigmoid(
            3.2
            - 3.0 * fv["laplacian_var_norm"]
            - 2.2 * fv["tenengrad_norm"]
            - 1.4 * fv["edge_content_ratio"]
            - 1.1 * fv["entropy_norm"]
        )
        under_exposure = _sigmoid(
            -1.1
            + 3.1 * fv["low_ratio"]
            + 1.4 * (1.0 - fv["mid_ratio"])
            + 1.1 * (1.0 - fv["mean_norm"])
            + 0.7 * (1.0 - fv["contrast_norm"])
        )
        over_exposure = _sigmoid(
            -1.2
            + 3.3 * fv["high_ratio"]
            + 1.5 * (1.0 - fv["mid_ratio"])
            + 1.0 * fv["mean_norm"]
            + 0.6 * (1.0 - fv["contrast_norm"])
        )
        occlusion = _sigmoid(
            -1.0
            + 3.0 * fv["center_dark_ratio"]
            + 1.3 * fv["border_dark_ratio"]
            + 0.7 * (1.0 - fv["edge_content_ratio"])
        )
        field_cutoff = _sigmoid(
            -0.9
            + 2.5 * max(fv["border_dark_ratio"], fv["border_bright_ratio"])
            + 1.4 * (1.0 - fv["mid_ratio"])
            + 0.9 * (1.0 - fv["edge_content_ratio"])
        )
        artifact = _sigmoid(
            -1.4
            + 2.8 * fv["impulse_noise_ratio"]
            + 1.6 * fv["stripe_anomaly"]
            + 0.7 * (fv["low_ratio"] + fv["high_ratio"])
        )
        return {
            "blur": _clamp01(blur),
            "under_exposure": _clamp01(under_exposure),
            "over_exposure": _clamp01(over_exposure),
            "occlusion": _clamp01(occlusion),
            "field_cutoff": _clamp01(field_cutoff),
            "artifact": _clamp01(artifact),
        }

    def _select_quality_issues(self, confidences: dict[str, float]) -> list[str]:
        selected = [
            issue for issue in _ISSUE_CODES if confidences.get(issue, 0.0) >= self._issue_threshold
        ]
        if not selected:
            return []
        return sorted(selected, key=lambda issue: confidences.get(issue, 0.0), reverse=True)

    def _status_and_retake(
        self,
        *,
        issue_confidences: dict[str, float],
        weighted_defect: float,
    ) -> tuple[str, bool]:
        max_issue = max(issue_confidences.values()) if issue_confidences else 0.0
        fail = max_issue >= self._fail_threshold or weighted_defect >= 0.56
        warn = max_issue >= self._warn_threshold or weighted_defect >= 0.34
        if fail:
            return "FAIL", True
        if warn:
            retake = any(
                issue_confidences.get(key, 0.0) >= 0.65
                for key in ("occlusion", "field_cutoff", "blur")
            )
            return "WARN", retake
        return "PASS", False

    @staticmethod
    def _laplacian_variance(arr: np.ndarray) -> float:
        kernel = np.array([[0.0, 1.0, 0.0], [1.0, -4.0, 1.0], [0.0, 1.0, 0.0]], dtype=np.float32)
        h, w = arr.shape
        if h < 3 or w < 3:
            return 0.0
        laplacian = np.zeros((h - 2, w - 2), dtype=np.float32)
        for di in range(3):
            for dj in range(3):
                laplacian += kernel[di, dj] * arr[di : h - 2 + di, dj : w - 2 + dj]
        return float(np.var(laplacian))

    @staticmethod
    def _tenengrad_focus(arr: np.ndarray) -> float:
        gx = np.zeros_like(arr, dtype=np.float32)
        gy = np.zeros_like(arr, dtype=np.float32)
        gx[:, 1:-1] = arr[:, 2:] - arr[:, :-2]
        gy[1:-1, :] = arr[2:, :] - arr[:-2, :]
        grad = np.sqrt(gx * gx + gy * gy)
        return float(np.mean(grad))

    @staticmethod
    def _entropy(arr: np.ndarray) -> float:
        hist, _ = np.histogram(arr, bins=256, range=(0, 255), density=True)
        hist = hist[hist > 0]
        if hist.size == 0:
            return 0.0
        return float(-np.sum(hist * np.log2(hist)))
