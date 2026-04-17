"""Grading model adapter - HEURISTIC implementation.

The adapter produces a traceable lesion grade from image statistics plus
segmentation regions. It is a real algorithmic placeholder, not a hard-coded
label. A trained classifier can replace the infer body while keeping the
public output contract stable.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.grading")


class GradingModelAdapter(BaseModelAdapter):
    """Heuristic caries grading adapter for Phase 5C."""

    model_code = "caries-grading-heuristic-v1"
    model_type_code = "GRADING"
    impl_type = ImplType.HEURISTIC

    _GRADE_THRESHOLDS = (0.25, 0.45, 0.70)

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        log.info("loading grading heuristic adapter model_code=%s", self.model_code)
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(
        self,
        image_path: Path,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detections: list[Any] | None = None,
    ) -> dict[str, Any]:
        """Grade lesion severity for *image_path*.

        Returns a dict with gradingLabel, confidenceScore, uncertaintyScore,
        implType, and rawResult. The caller applies business review thresholds.
        """
        img = Image.open(image_path).convert("L")
        arr = np.asarray(img, dtype=np.float64)
        height, width = arr.shape
        regions = self._normalise_regions(segmentation_regions, width, height)
        if not regions:
            regions = [self._fallback_region(width, height, tooth_detections)]

        candidates = [self._score_region(arr, region) for region in regions]
        best = max(candidates, key=lambda item: item["severityScore"])
        label = self._label_for_score(best["severityScore"])
        uncertainty = self._uncertainty(best)
        confidence = self._confidence(best, uncertainty)

        return {
            "gradingLabel": label,
            "confidenceScore": confidence,
            "uncertaintyScore": uncertainty,
            "implType": ImplType.HEURISTIC.value,
            "rawResult": {
                "modelCode": self.model_code,
                "imageSize": [width, height],
                "regionCount": len(regions),
                "selectedRegionIndex": best["regionIndex"],
                "selectedToothCode": best["toothCode"],
                "severityScore": best["severityScore"],
                "contrastScore": best["contrastScore"],
                "areaRatio": best["areaRatio"],
                "segmentationScore": best["segmentationScore"],
                "boundaryDistance": best["boundaryDistance"],
                "confidenceThreshold": self._confidence_threshold,
                "candidates": candidates,
            },
        }

    def _score_region(self, arr: np.ndarray, region: dict[str, Any]) -> dict[str, Any]:
        height, width = arr.shape
        bbox = self._clamp_box([int(v) for v in region["bbox"]], width, height)
        x1, y1, x2, y2 = bbox
        roi = arr[y1:y2, x1:x2]
        if roi.size == 0:
            roi = arr

        image_mean = float(np.mean(arr)) if arr.size else 0.0
        roi_mean = float(np.mean(roi)) if roi.size else image_mean
        roi_min = float(np.min(roi)) if roi.size else roi_mean
        dark_contrast = max(0.0, min(1.0, (image_mean - roi_min) / 120.0))
        local_contrast = max(0.0, min(1.0, (roi_mean - roi_min) / 90.0))
        contrast_score = round(max(dark_contrast, local_contrast), 4)

        area = max(1, (x2 - x1) * (y2 - y1))
        area_ratio = min(1.0, area / max(1, width * height))
        area_score = min(1.0, area_ratio / 0.08)
        segmentation_score = float(region.get("score") or region.get("segmentationScore") or 0.5)
        segmentation_score = max(0.0, min(1.0, segmentation_score))

        severity_score = (
            contrast_score * 0.55
            + area_score * 0.25
            + segmentation_score * 0.20
        )
        severity_score = round(max(0.0, min(1.0, severity_score)), 4)
        boundary_distance = min(abs(severity_score - item) for item in self._GRADE_THRESHOLDS)

        return {
            "regionIndex": int(region.get("regionIndex") or region.get("region_index") or 0),
            "toothCode": str(region.get("toothCode") or region.get("tooth_code") or "16"),
            "bbox": bbox,
            "severityScore": severity_score,
            "contrastScore": contrast_score,
            "areaRatio": round(area_ratio, 6),
            "segmentationScore": round(segmentation_score, 4),
            "boundaryDistance": round(boundary_distance, 4),
            "roiMean": round(roi_mean, 4),
            "roiMin": round(roi_min, 4),
            "imageMean": round(image_mean, 4),
        }

    def _uncertainty(self, scored: dict[str, Any]) -> float:
        boundary_margin = min(1.0, float(scored["boundaryDistance"]) / 0.15)
        segmentation_score = float(scored["segmentationScore"])
        ambiguity = 1.0 - boundary_margin
        weak_segmentation = 1.0 - segmentation_score
        uncertainty = 0.08 + ambiguity * 0.45 + weak_segmentation * 0.25
        return round(max(0.02, min(0.95, uncertainty)), 4)

    def _confidence(self, scored: dict[str, Any], uncertainty: float) -> float:
        segmentation_score = float(scored["segmentationScore"])
        base = 1.0 - uncertainty
        confidence = base * 0.85 + segmentation_score * 0.15
        return round(max(0.0, min(0.99, confidence)), 4)

    @classmethod
    def _label_for_score(cls, severity_score: float) -> str:
        if severity_score < cls._GRADE_THRESHOLDS[0]:
            return "C0"
        if severity_score < cls._GRADE_THRESHOLDS[1]:
            return "C1"
        if severity_score < cls._GRADE_THRESHOLDS[2]:
            return "C2"
        return "C3"

    @classmethod
    def _normalise_regions(
        cls,
        regions: list[dict[str, Any]] | None,
        width: int,
        height: int,
    ) -> list[dict[str, Any]]:
        normalised: list[dict[str, Any]] = []
        for index, region in enumerate(regions or []):
            bbox = region.get("bbox")
            if not bbox or len(bbox) != 4:
                continue
            box = cls._clamp_box([int(v) for v in bbox], width, height)
            if box[2] <= box[0] or box[3] <= box[1]:
                continue
            item = dict(region)
            item["bbox"] = box
            item.setdefault("regionIndex", index)
            normalised.append(item)
        return normalised

    @staticmethod
    def _fallback_region(
        width: int,
        height: int,
        tooth_detections: list[Any] | None,
    ) -> dict[str, Any]:
        for det in tooth_detections or []:
            bbox = getattr(det, "bbox", None)
            tooth_code = getattr(det, "tooth_code", None) or getattr(det, "toothCode", None)
            score = getattr(det, "detection_score", None) or getattr(det, "score", None)
            if isinstance(det, dict):
                bbox = det.get("bbox")
                tooth_code = det.get("toothCode") or det.get("tooth_code")
                score = det.get("score") or det.get("detectionScore")
            if bbox and len(bbox) == 4:
                return {
                    "bbox": GradingModelAdapter._clamp_box([int(v) for v in bbox], width, height),
                    "toothCode": tooth_code or "16",
                    "score": score or 0.5,
                    "regionIndex": 0,
                }
        return {
            "bbox": [
                max(0, int(width * 0.35)),
                max(0, int(height * 0.35)),
                min(width - 1, int(width * 0.55)),
                min(height - 1, int(height * 0.65)),
            ],
            "toothCode": "16",
            "score": 0.5,
            "regionIndex": 0,
        }

    @staticmethod
    def _clamp_box(box: list[int], width: int, height: int) -> list[int]:
        x1, y1, x2, y2 = box
        return [
            max(0, min(width - 1, x1)),
            max(0, min(height - 1, y1)),
            max(0, min(width - 1, x2)),
            max(0, min(height - 1, y2)),
        ]
