"""Lesion segmentation model adapter - HEURISTIC implementation.

This Phase 5B adapter produces deterministic lesion candidate masks from
image statistics and optional tooth-detection priors. It is an algorithmic
placeholder, not a trained segmentation model.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.lesion-segmenter")


class LesionSegmenterAdapter(BaseModelAdapter):
    """Heuristic lesion-segmentation adapter for Phase 5B."""

    model_code = "lesion-segmentation-heuristic-v1"
    model_type_code = "SEGMENTATION"
    impl_type = ImplType.HEURISTIC

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        log.info("loading lesion segmentation heuristic adapter model_code=%s", self.model_code)
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(self, image_path: Path, tooth_detections: list[Any] | None = None) -> dict[str, Any]:
        """Segment lesion candidates for *image_path*.

        Returns a dict containing a uint8 ``maskArray`` plus serialisable region
        metadata. The mask uses 255 for candidate lesion pixels and 0 elsewhere.
        """
        img = Image.open(image_path).convert("L")
        width, height = img.size
        arr = np.asarray(img, dtype=np.float64)

        candidate_boxes = self._candidate_boxes(width, height, tooth_detections or [])
        mask = Image.new("L", (width, height), color=0)
        draw = ImageDraw.Draw(mask)
        regions: list[dict[str, Any]] = []

        for index, candidate in enumerate(candidate_boxes):
            region = self._region_from_candidate(arr, candidate, index)
            draw.ellipse(region["bbox"], fill=255)
            regions.append(region)

        mask_array = np.asarray(mask, dtype=np.uint8)
        score = round(float(np.mean([r["score"] for r in regions])) if regions else 0.0, 4)
        return {
            "regions": regions,
            "maskArray": mask_array,
            "segmentationScore": score,
            "implType": ImplType.HEURISTIC.value,
            "rawResult": {
                "imageSize": [width, height],
                "candidateCount": len(candidate_boxes),
                "regionCount": len(regions),
                "maskPixels": int(np.sum(mask_array > 0)),
                "confidenceThreshold": self._confidence_threshold,
            },
        }

    def _candidate_boxes(
        self,
        width: int,
        height: int,
        tooth_detections: list[Any],
    ) -> list[dict[str, Any]]:
        boxes: list[dict[str, Any]] = []
        for det in tooth_detections:
            bbox = getattr(det, "bbox", None)
            tooth_code = getattr(det, "tooth_code", None) or getattr(det, "toothCode", None)
            score = getattr(det, "detection_score", None) or getattr(det, "score", None)
            if not bbox and isinstance(det, dict):
                bbox = det.get("bbox")
                tooth_code = det.get("toothCode") or det.get("tooth_code")
                score = det.get("score") or det.get("detectionScore")
            if not bbox or len(bbox) != 4:
                continue
            x1, y1, x2, y2 = self._clamp_box([int(v) for v in bbox], width, height)
            if x2 <= x1 or y2 <= y1:
                continue
            boxes.append({
                "bbox": [x1, y1, x2, y2],
                "toothCode": tooth_code or "16",
                "priorScore": float(score) if score is not None else 0.5,
            })

        if boxes:
            return boxes

        fallback = self._stable_box(width, height)
        return [{"bbox": fallback, "toothCode": "16", "priorScore": 0.5}]

    def _region_from_candidate(
        self,
        arr: np.ndarray,
        candidate: dict[str, Any],
        index: int,
    ) -> dict[str, Any]:
        height, width = arr.shape
        x1, y1, x2, y2 = candidate["bbox"]
        roi = arr[y1:y2, x1:x2]
        if roi.size == 0:
            lesion_box = self._stable_box(width, height)
            roi_mean = float(np.mean(arr)) if arr.size else 0.0
            roi_min = float(np.min(arr)) if arr.size else 0.0
        else:
            threshold = float(np.percentile(roi, 35))
            dark_y, dark_x = np.where(roi <= threshold)
            if dark_x.size:
                cx = int(x1 + np.mean(dark_x))
                cy = int(y1 + np.mean(dark_y))
            else:
                cx = (x1 + x2) // 2
                cy = (y1 + y2) // 2
            lesion_w = max(8, int((x2 - x1) * 0.32))
            lesion_h = max(8, int((y2 - y1) * 0.28))
            lesion_box = self._clamp_box(
                [
                    cx - lesion_w // 2,
                    cy - lesion_h // 2,
                    cx + lesion_w // 2,
                    cy + lesion_h // 2,
                ],
                width,
                height,
            )
            roi_mean = float(np.mean(roi))
            roi_min = float(np.min(roi))

        contrast = max(0.0, min(1.0, (roi_mean - roi_min) / 90.0))
        prior_score = float(candidate.get("priorScore") or 0.5)
        score = round(max(self._confidence_threshold, 0.45 + 0.35 * contrast + 0.2 * prior_score), 4)
        bx1, by1, bx2, by2 = lesion_box
        return {
            "toothCode": candidate.get("toothCode") or "16",
            "polygon": [[bx1, by1], [bx2, by1], [bx2, by2], [bx1, by2]],
            "bbox": lesion_box,
            "score": min(0.99, score),
            "regionIndex": index,
        }

    @staticmethod
    def _stable_box(width: int, height: int) -> list[int]:
        x1 = max(0, int(width * 0.35))
        y1 = max(0, int(height * 0.35))
        x2 = min(width - 1, int(width * 0.55))
        y2 = min(height - 1, int(height * 0.65))
        return [x1, y1, x2, y2]

    @staticmethod
    def _clamp_box(box: list[int], width: int, height: int) -> list[int]:
        x1, y1, x2, y2 = box
        return [
            max(0, min(width - 1, x1)),
            max(0, min(height - 1, y1)),
            max(0, min(width - 1, x2)),
            max(0, min(height - 1, y2)),
        ]
