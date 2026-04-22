from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np

from app.core.config import Settings
from app.core.image_utils import load_image, resize_image
from app.core.exceptions import BusinessException
from app.infra.model.base_model import ImplType
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput


@dataclass(frozen=True)
class SegmentationResult:
    segmentation_mode: str
    segmentation_impl_type: str
    regions: list[dict[str, Any]]
    mask_path: Path
    overlay_path: Path
    heatmap_path: Path
    raw_result: dict[str, Any]


class SegmentationPipeline:
    """Segmentation pipeline without fabricated mask fallback."""

    def __init__(self, registry: ModelRegistry, settings: Settings, model_assets: ModelAssets) -> None:
        self._registry = registry
        self._settings = settings
        self._model_assets = model_assets

    def segment(
        self,
        image: ImageInput,
        image_path: Path | None,
        tooth_detections: list[ToothDetection],
        output_dir: Path,
    ) -> SegmentationResult:
        output_dir.mkdir(parents=True, exist_ok=True)
        if not self._registry.is_module_real("segmentation"):
            raise BusinessException("M5005", "segmentation module is disabled")

        adapter = self._registry.get_segmenter()
        if adapter is None:
            raise BusinessException("M5006", "segmentation adapter is unavailable")
        if image_path is None:
            raise BusinessException("M5007", "segmentation image is unavailable")

        try:
            if self._settings.segmentation_force_fail:
                raise RuntimeError("forced segmentation failure")
            result = adapter.infer(image_path, self._filter_detections(image, tooth_detections))
        except Exception as exc:
            raise BusinessException("M5008", f"segmentation failed: {exc}") from exc
        return self._real_result(image, image_path, output_dir, result)

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_segmenter()
        return adapter.impl_type.value if adapter is not None else "DISABLED"

    def _real_result(
        self,
        image: ImageInput,
        image_path: Path,
        output_dir: Path,
        result: dict[str, Any],
    ) -> SegmentationResult:
        paths = self._paths(image, output_dir)
        mask_array = result.get("maskArray")
        if mask_array is None:
            raise ValueError("segmentation result missing maskArray")

        mask_array = np.asarray(mask_array, dtype=np.uint8)
        regions = self._filter_regions(list(result.get("regions") or []))
        if not regions:
            raise ValueError("segmentation result has no usable regions")

        self._render_assets(
            image_path=image_path,
            mask_array=mask_array,
            regions=regions,
            mask_path=paths["mask"],
            overlay_path=paths["overlay"],
            heatmap_path=paths["heatmap"],
        )
        raw = dict(result.get("rawResult") or {})
        raw.update(
            {
                "segmentationScore": result.get("segmentationScore"),
                "classMapPath": str(self._model_assets.class_map_path),
                "preprocessPath": str(self._model_assets.preprocess_path),
                "manifestPath": str(self._model_assets.segmentation_manifest.manifest_path),
                "postprocessPath": str(self._model_assets.postprocess_path),
            }
        )
        return SegmentationResult(
            segmentation_mode="real",
            segmentation_impl_type=str(result.get("implType") or ImplType.HEURISTIC.value),
            regions=regions,
            mask_path=paths["mask"],
            overlay_path=paths["overlay"],
            heatmap_path=paths["heatmap"],
            raw_result=raw,
        )

    def _filter_regions(self, regions: list[dict[str, Any]]) -> list[dict[str, Any]]:
        min_area = self._model_assets.segmentation_min_region_area(0)
        if min_area <= 0:
            return regions

        filtered: list[dict[str, Any]] = []
        for region in regions:
            bbox = region.get("bbox")
            if not isinstance(bbox, list) or len(bbox) != 4:
                continue
            try:
                x1, y1, x2, y2 = [int(value) for value in bbox]
            except (TypeError, ValueError):
                continue
            area = max(0, x2 - x1) * max(0, y2 - y1)
            if area >= min_area:
                filtered.append(region)
        return filtered

    @staticmethod
    def _filter_detections(
        image: ImageInput,
        tooth_detections: list[ToothDetection],
    ) -> list[ToothDetection]:
        if image.image_id is None:
            return tooth_detections
        return [item for item in tooth_detections if item.image_id in {None, image.image_id}]

    @staticmethod
    def _paths(image: ImageInput, output_dir: Path) -> dict[str, Path]:
        image_id = image.image_id if image and image.image_id is not None else "unknown"
        return {
            "mask": output_dir / f"mask_{image_id}.png",
            "overlay": output_dir / f"overlay_{image_id}.png",
            "heatmap": output_dir / f"heatmap_{image_id}.png",
        }

    @staticmethod
    def _render_assets(
        image_path: Path,
        mask_array: np.ndarray,
        regions: list[dict[str, Any]],
        mask_path: Path,
        overlay_path: Path,
        heatmap_path: Path,
    ) -> None:
        try:
            import cv2
        except ImportError as exc:
            raise RuntimeError("opencv-python-headless is required for segmentation asset rendering") from exc

        loaded = load_image(image_path)
        base_gray = loaded.pixels
        base = cv2.cvtColor(base_gray, cv2.COLOR_GRAY2BGR)
        mask = resize_image(mask_array.astype(np.uint8), (loaded.width, loaded.height), interpolation="nearest")
        cv2.imwrite(str(mask_path), mask)

        overlay = base.copy()
        line_width = max(2, loaded.width // 300)
        for region in regions:
            bbox = region.get("bbox")
            if not bbox or len(bbox) != 4:
                continue
            x1, y1, x2, y2 = [int(value) for value in bbox]
            cv2.rectangle(overlay, (x1, y1), (x2, y2), (0, 0, 255), line_width)
            polygon = region.get("polygon")
            if isinstance(polygon, list) and len(polygon) >= 3:
                points = np.asarray(polygon, dtype=np.int32)
                cv2.polylines(overlay, [points], isClosed=True, color=(0, 255, 255), thickness=line_width)
            tooth_code = str(region.get("toothCode") or region.get("tooth_code") or "").strip()
            if tooth_code:
                cv2.putText(
                    overlay,
                    tooth_code,
                    (x1, max(18, y1 - 8)),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    (255, 255, 255),
                    1,
                    cv2.LINE_AA,
                )
        blended = cv2.addWeighted(base, 0.72, overlay, 0.28, 0.0)
        cv2.imwrite(str(overlay_path), blended)

        blurred = cv2.GaussianBlur(mask, (0, 0), sigmaX=max(2, loaded.width // 160))
        heat = cv2.applyColorMap(blurred, cv2.COLORMAP_JET)
        heatmap = cv2.addWeighted(base, 0.6, heat, 0.4, 0.0)
        cv2.imwrite(str(heatmap_path), heatmap)
