"""Composite uncertainty pipeline for analysis callbacks.

This module merges multiple signal groups into a single uncertainty score:
- image quality penalties
- tooth detection confidence/instability
- lesion geometry stability
- grading confidence / class-margin stability
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.core.config import Settings
from app.core.logging import get_logger
from app.pipelines.grading_pipeline import GradingResult
from app.schemas.callback import ToothDetection

log = get_logger("cariesguard-ai.pipeline.uncertainty")


@dataclass(frozen=True)
class UncertaintyResult:
    uncertainty_mode: str
    uncertainty_impl_type: str
    uncertainty_score: float
    uncertainty_reasons: list[str]
    needs_review: bool
    component_penalties: dict[str, float]


class UncertaintyPipeline:
    """Compose uncertainty from quality/detection/geometry/grading signals."""

    def __init__(self, settings: Settings, review_threshold: float | None = None) -> None:
        self._settings = settings
        self._review_threshold = (
            float(review_threshold)
            if review_threshold is not None
            else float(settings.uncertainty_review_threshold)
        )

    def assess(
        self,
        *,
        grading_result: GradingResult,
        quality_results: list[Any],
        tooth_detections: list[ToothDetection],
        segmentation_regions: list[dict[str, Any]] | None = None,
        lesion_results: list[dict[str, Any]] | None = None,
    ) -> UncertaintyResult:
        if grading_result.grading_mode != "real":
            score = self._clamp(grading_result.uncertainty_score)
            needs_review = score >= self._review_threshold
            reasons = ["UNCERTAINTY_THRESHOLD_EXCEEDED"] if needs_review else []
            return UncertaintyResult(
                uncertainty_mode=grading_result.grading_mode or "disabled",
                uncertainty_impl_type=grading_result.grading_impl_type or "DISABLED",
                uncertainty_score=score,
                uncertainty_reasons=reasons,
                needs_review=needs_review,
                component_penalties={
                    "qualityPenalty": 0.0,
                    "detectionPenalty": 0.0,
                    "geometryPenalty": 0.0,
                    "gradingPenalty": self._clamp(1.0 - grading_result.confidence_score),
                    "gradingPrior": score,
                },
            )

        quality_penalty = self._quality_penalty(quality_results)
        detection_penalty = self._detection_penalty(tooth_detections)
        geometry_penalty = self._geometry_penalty(segmentation_regions or [], lesion_results or [])
        grading_penalty, class_margin = self._grading_penalty(grading_result)
        grading_prior = self._clamp(grading_result.uncertainty_score)

        blended = self._clamp(
            0.68 * grading_prior
            + 0.22 * grading_penalty
            + 0.06 * detection_penalty
            + 0.02 * quality_penalty
            + 0.02 * geometry_penalty
        )
        # Composite uncertainty should not be lower than grading prior.
        # Other signals can raise uncertainty, but should not suppress the
        # primary grading instability signal in review gating.
        composite = max(grading_prior, blended)
        reasons = self._build_reasons(
            quality_penalty=quality_penalty,
            detection_penalty=detection_penalty,
            geometry_penalty=geometry_penalty,
            grading_penalty=grading_penalty,
            grading_prior=grading_prior,
            class_margin=class_margin,
            score=composite,
        )
        needs_review = composite >= self._review_threshold
        return UncertaintyResult(
            uncertainty_mode=grading_result.grading_mode,
            uncertainty_impl_type="COMPOSITE_HEURISTIC",
            uncertainty_score=composite,
            uncertainty_reasons=reasons,
            needs_review=needs_review,
            component_penalties={
                "qualityPenalty": round(quality_penalty, 4),
                "detectionPenalty": round(detection_penalty, 4),
                "geometryPenalty": round(geometry_penalty, 4),
                "gradingPenalty": round(grading_penalty, 4),
                "gradingPrior": round(grading_prior, 4),
                "classMargin": round(class_margin, 4),
            },
        )

    @staticmethod
    def _clamp(value: Any, default: float = 0.0) -> float:
        try:
            number = float(value)
        except (TypeError, ValueError):
            number = default
        return max(0.0, min(1.0, number))

    def _quality_penalty(self, quality_results: list[Any]) -> float:
        if not quality_results:
            return 0.65
        total = len(quality_results)
        status_penalties = {"PASS": 0.0, "WARN": 0.58, "FAIL": 1.0}
        weighted_status = 0.0
        score_sum = 0.0
        issue_count = 0

        for item in quality_results:
            status = str(
                getattr(item, "quality_status", None)
                or getattr(item, "check_result_code", "FAIL")
            ).upper()
            weighted_status += status_penalties.get(status, 1.0)
            score = getattr(item, "quality_score_float", None)
            if score is None:
                try:
                    score = float(getattr(item, "quality_score", 0) or 0) / 100.0
                except (TypeError, ValueError):
                    score = 0.0
            score_sum += self._clamp(score, 0.0)
            issue_count += len(
                getattr(item, "quality_issues", None)
                or getattr(item, "issue_codes", None)
                or []
            )

        mean_status_penalty = weighted_status / total
        mean_score = score_sum / total
        issue_ratio = issue_count / max(total * 4, 1)
        penalty = (
            0.50 * mean_status_penalty
            + 0.35 * (1.0 - mean_score)
            + 0.15 * self._clamp(issue_ratio, 0.0)
        )
        return self._clamp(penalty, 0.6)

    def _detection_penalty(self, tooth_detections: list[ToothDetection]) -> float:
        if not tooth_detections:
            return 0.85

        scores = [self._clamp(item.detection_score, 0.0) for item in tooth_detections]
        mean_conf = sum(scores) / len(scores)
        variance = sum((score - mean_conf) ** 2 for score in scores) / len(scores)
        std_conf = variance ** 0.5
        low_conf_ratio = sum(1 for score in scores if score < 0.5) / len(scores)
        overlap_ratio = self._overlap_ratio(tooth_detections)
        count_penalty = 0.30 if len(tooth_detections) <= 1 else 0.0

        penalty = (
            0.45 * (1.0 - mean_conf)
            + 0.22 * self._clamp(std_conf / 0.35)
            + 0.18 * low_conf_ratio
            + 0.10 * overlap_ratio
            + 0.05 * count_penalty
        )
        return self._clamp(penalty, 0.5)

    def _geometry_penalty(
        self,
        segmentation_regions: list[dict[str, Any]],
        lesion_results: list[dict[str, Any]],
    ) -> float:
        regions = lesion_results if lesion_results else segmentation_regions
        if not regions:
            return 0.75

        scores: list[float] = []
        size_outlier_count = 0
        missing_shape_count = 0
        for item in regions:
            if not isinstance(item, dict):
                continue
            score = self._clamp(
                item.get("confidenceScore")
                if "confidenceScore" in item
                else item.get("score"),
                0.5,
            )
            scores.append(score)
            area_ratio = item.get("lesionAreaRatio")
            if area_ratio is None:
                area_ratio = self._bbox_area_ratio(item.get("bbox"))
            ratio = self._clamp(area_ratio, 0.0)
            if ratio < 0.0015 or ratio > 0.30:
                size_outlier_count += 1
            polygon = item.get("polygon")
            bbox = item.get("bbox")
            if (not isinstance(polygon, list) or len(polygon) < 3) and (
                not isinstance(bbox, list) or len(bbox) != 4
            ):
                missing_shape_count += 1

        if not scores:
            return 0.75
        mean_score = sum(scores) / len(scores)
        size_outlier_ratio = size_outlier_count / len(scores)
        missing_shape_ratio = missing_shape_count / len(scores)
        penalty = (
            0.62 * (1.0 - mean_score)
            + 0.23 * size_outlier_ratio
            + 0.15 * missing_shape_ratio
        )
        return self._clamp(penalty, 0.5)

    def _grading_penalty(self, grading_result: GradingResult) -> tuple[float, float]:
        confidence = self._clamp(grading_result.confidence_score, 0.5)
        class_margin = self._extract_class_margin(grading_result.raw_result)
        margin_stability = self._clamp(class_margin / 0.18)
        penalty = 0.72 * (1.0 - confidence) + 0.28 * (1.0 - margin_stability)
        return self._clamp(penalty, 0.5), class_margin

    def _build_reasons(
        self,
        *,
        quality_penalty: float,
        detection_penalty: float,
        geometry_penalty: float,
        grading_penalty: float,
        grading_prior: float,
        class_margin: float,
        score: float,
    ) -> list[str]:
        reasons: list[str] = []
        if quality_penalty >= 0.35:
            reasons.append("QUALITY_ALERT")
        if detection_penalty >= 0.40:
            reasons.append("DETECTION_INSTABILITY")
        if geometry_penalty >= 0.40:
            reasons.append("LESION_GEOMETRY_INSTABILITY")
        if grading_penalty >= 0.38:
            reasons.append("GRADING_CONFIDENCE_LOW")
        if class_margin < 0.06:
            reasons.append("GRADING_MARGIN_LOW")
        if grading_prior >= 0.45:
            reasons.append("GRADING_PRIOR_UNCERTAIN")
        if score >= self._review_threshold:
            reasons.append("UNCERTAINTY_THRESHOLD_EXCEEDED")
        # keep order and dedupe
        return list(dict.fromkeys(reasons))

    def _extract_class_margin(self, raw_result: dict[str, Any] | None) -> float:
        if not isinstance(raw_result, dict):
            return 0.0
        for key in ("classMargin", "class_margin", "boundaryDistance", "boundary_distance"):
            if key in raw_result:
                return self._clamp(raw_result.get(key), 0.0)

        candidates = raw_result.get("candidates")
        if not isinstance(candidates, list) or not candidates:
            return 0.0
        selected_idx = raw_result.get("selectedRegionIndex")
        if isinstance(selected_idx, int):
            for item in candidates:
                if not isinstance(item, dict):
                    continue
                if int(item.get("regionIndex", -1)) == selected_idx:
                    return self._clamp(item.get("boundaryDistance"), 0.0)
        first = candidates[0]
        if isinstance(first, dict):
            return self._clamp(first.get("boundaryDistance"), 0.0)
        return 0.0

    @staticmethod
    def _bbox_area_ratio(bbox: Any) -> float:
        if not isinstance(bbox, list) or len(bbox) != 4:
            return 0.0
        try:
            x1, y1, x2, y2 = [float(value) for value in bbox]
        except (TypeError, ValueError):
            return 0.0
        width = max(0.0, x2 - x1)
        height = max(0.0, y2 - y1)
        # Heuristic normalisation without explicit image size:
        # area around 512px is treated as 0.01 in uncertainty scoring.
        return min(1.0, (width * height) / 51200.0)

    def _overlap_ratio(self, detections: list[ToothDetection]) -> float:
        if len(detections) < 2:
            return 0.0
        boxes = [item.bbox for item in detections if item.bbox and len(item.bbox) == 4]
        if len(boxes) < 2:
            return 0.0

        overlaps = 0
        total = 0
        for i in range(len(boxes)):
            for j in range(i + 1, len(boxes)):
                total += 1
                if self._iou(boxes[i], boxes[j]) >= 0.65:
                    overlaps += 1
        return overlaps / total if total else 0.0

    @staticmethod
    def _iou(box_a: list[int], box_b: list[int]) -> float:
        ax1, ay1, ax2, ay2 = box_a
        bx1, by1, bx2, by2 = box_b
        inter_x1 = max(ax1, bx1)
        inter_y1 = max(ay1, by1)
        inter_x2 = min(ax2, bx2)
        inter_y2 = min(ay2, by2)
        inter_w = max(0, inter_x2 - inter_x1)
        inter_h = max(0, inter_y2 - inter_y1)
        inter_area = inter_w * inter_h
        area_a = max(0, ax2 - ax1) * max(0, ay2 - ay1)
        area_b = max(0, bx2 - bx1) * max(0, by2 - by1)
        union_area = max(1, area_a + area_b - inter_area)
        return inter_area / union_area
