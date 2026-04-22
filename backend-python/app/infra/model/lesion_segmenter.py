from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw

from app.core.logging import get_logger
from app.infra.model.base_model import BaseModelAdapter, ImplType

log = get_logger("cariesguard-ai.model.lesion-segmenter")


class LesionSegmenterAdapter(BaseModelAdapter):
    """Heuristic lesion-segmentation adapter without fixed fake boxes."""

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
        img = Image.open(image_path).convert("L")
        width, height = img.size
        arr = np.asarray(img, dtype=np.float64)

        candidate_boxes = self._candidate_boxes(arr, width, height, tooth_detections or [])
        if not candidate_boxes:
            raise RuntimeError("no segmentation candidates available")

        mask = Image.new("L", (width, height), color=0)
        draw = ImageDraw.Draw(mask)
        regions: list[dict[str, Any]] = []

        for index, candidate in enumerate(candidate_boxes):
            region = self._region_from_candidate(arr, candidate, index)
            draw.ellipse(region["bbox"], fill=255)
            regions.append(region)

        mask_array = np.asarray(mask, dtype=np.uint8)
        score = round(float(np.mean([item["score"] for item in regions])) if regions else 0.0, 4)
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
        arr: np.ndarray,
        width: int,
        height: int,
        tooth_detections: list[Any],
    ) -> list[dict[str, Any]]:
        boxes: list[dict[str, Any]] = []
        for detection in tooth_detections:
            bbox = getattr(detection, "bbox", None)
            tooth_code = getattr(detection, "tooth_code", None) or getattr(detection, "toothCode", None)
            score = getattr(detection, "detection_score", None) or getattr(detection, "score", None)
            if not bbox and isinstance(detection, dict):
                bbox = detection.get("bbox")
                tooth_code = detection.get("toothCode") or detection.get("tooth_code")
                score = detection.get("score") or detection.get("detectionScore")
            if not bbox or len(bbox) != 4:
                continue
            x1, y1, x2, y2 = self._clamp_box([int(value) for value in bbox], width, height)
            if x2 <= x1 or y2 <= y1:
                continue
            boxes.append(
                {
                    "bbox": [x1, y1, x2, y2],
                    "toothCode": tooth_code or "UNKNOWN",
                    "priorScore": float(score) if score is not None else 0.5,
                }
            )

        if boxes:
            return boxes
        return self._image_driven_candidates(arr, width, height)

    def _image_driven_candidates(self, arr: np.ndarray, width: int, height: int) -> list[dict[str, Any]]:
        if arr.size == 0:
            return []
        threshold = float(np.percentile(arr, 12))
        dark_y, dark_x = np.where(arr <= threshold)
        if dark_x.size == 0 or dark_y.size == 0:
            return []
        center_x = int(np.mean(dark_x))
        center_y = int(np.mean(dark_y))
        span_x = max(12, int(np.std(dark_x) * 1.5))
        span_y = max(12, int(np.std(dark_y) * 1.5))
        bbox = self._clamp_box(
            [center_x - span_x, center_y - span_y, center_x + span_x, center_y + span_y],
            width,
            height,
        )
        if bbox[2] <= bbox[0] or bbox[3] <= bbox[1]:
            return []
        return [{"bbox": bbox, "toothCode": "UNKNOWN", "priorScore": 0.4}]

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
            raise RuntimeError("empty segmentation ROI")

        threshold = float(np.percentile(roi, 35))
        dark_y, dark_x = np.where(roi <= threshold)
        if dark_x.size:
            center_x = int(x1 + np.mean(dark_x))
            center_y = int(y1 + np.mean(dark_y))
        else:
            center_x = (x1 + x2) // 2
            center_y = (y1 + y2) // 2
        lesion_w = max(8, int((x2 - x1) * 0.32))
        lesion_h = max(8, int((y2 - y1) * 0.28))
        lesion_box = self._clamp_box(
            [
                center_x - lesion_w // 2,
                center_y - lesion_h // 2,
                center_x + lesion_w // 2,
                center_y + lesion_h // 2,
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
            "toothCode": candidate.get("toothCode") or "UNKNOWN",
            "polygon": [[bx1, by1], [bx2, by1], [bx2, by2], [bx1, by2]],
            "bbox": lesion_box,
            "score": min(0.99, score),
            "regionIndex": index,
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
