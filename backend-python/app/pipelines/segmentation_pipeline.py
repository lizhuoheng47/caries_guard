"""Lesion segmentation sub-pipeline with mode-aware routing and fallback."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.infra.model.base_model import ImplType
from app.infra.model.model_registry import ModelRegistry
from app.schemas.callback import ToothDetection
from app.schemas.request import ImageInput

log = get_logger("cariesguard-ai.pipeline.segmentation")


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
    """Segmentation pipeline with mock / heuristic routing."""

    def __init__(self, registry: ModelRegistry, settings: Settings) -> None:
        self._registry = registry
        self._settings = settings

    def segment(
        self,
        image: ImageInput,
        image_path: Path | None,
        tooth_detections: list[ToothDetection],
        output_dir: Path,
    ) -> SegmentationResult:
        """Run segmentation and write mask/overlay/heatmap images."""
        output_dir.mkdir(parents=True, exist_ok=True)
        mode = self._registry.get_runtime_mode()
        image_detections = self._filter_detections(image, tooth_detections)

        if not self._registry.is_module_real("segmentation"):
            return self._mock_result(image, image_path, output_dir)

        adapter = self._registry.get_segmenter()
        if adapter is None or image_path is None:
            if mode == "real":
                raise BusinessException(
                    "M5005",
                    "segmentation adapter or image not available in real mode",
                )
            log.warning("segmentation adapter/image unavailable - fallback to mock (hybrid)")
            return self._mock_result(image, image_path, output_dir)

        try:
            result = adapter.infer(image_path, image_detections)
            return self._real_result(image, image_path, output_dir, result)
        except Exception as exc:
            if mode == "real":
                raise BusinessException("M5006", f"segmentation failed: {exc}") from exc
            log.warning("segmentation failed - fallback to mock (hybrid) error=%s", exc)
            return self._mock_result(image, image_path, output_dir)

    def get_last_impl_type(self) -> str:
        adapter = self._registry.get_segmenter()
        if adapter is not None and self._registry.is_module_real("segmentation"):
            return adapter.impl_type.value
        return ImplType.MOCK.value

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
        regions = list(result.get("regions") or [])

        self._render_assets(
            image_path=image_path,
            mask_array=mask_array,
            regions=regions,
            mask_path=paths["mask"],
            overlay_path=paths["overlay"],
            heatmap_path=paths["heatmap"],
        )
        raw = dict(result.get("rawResult") or {})
        raw["segmentationScore"] = result.get("segmentationScore")
        return SegmentationResult(
            segmentation_mode="real",
            segmentation_impl_type=str(result.get("implType") or ImplType.HEURISTIC.value),
            regions=regions,
            mask_path=paths["mask"],
            overlay_path=paths["overlay"],
            heatmap_path=paths["heatmap"],
            raw_result=raw,
        )

    def _mock_result(
        self,
        image: ImageInput,
        image_path: Path | None,
        output_dir: Path,
    ) -> SegmentationResult:
        paths = self._paths(image, output_dir)
        self._draw_mock_assets(image_path, paths["mask"], paths["overlay"], paths["heatmap"])
        width, height = self._image_size(image_path)
        box = self._stable_box(width, height)
        region = {
            "toothCode": "16",
            "polygon": [[box[0], box[1]], [box[2], box[1]], [box[2], box[3]], [box[0], box[3]]],
            "bbox": box,
            "score": 0.95,
            "regionIndex": 0,
        }
        return SegmentationResult(
            segmentation_mode="mock",
            segmentation_impl_type=ImplType.MOCK.value,
            regions=[region],
            mask_path=paths["mask"],
            overlay_path=paths["overlay"],
            heatmap_path=paths["heatmap"],
            raw_result={"imageSize": [width, height], "regionCount": 1, "fallback": "mock"},
        )

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
            "mask": output_dir / f"mask_{image_id}_16.png",
            "overlay": output_dir / f"overlay_{image_id}_16.png",
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
            base = Image.open(image_path).convert("RGB")
        except Exception:
            base = Image.new("RGB", (512, 256), color=(245, 245, 245))

        mask = Image.fromarray(mask_array, mode="L").resize(base.size)
        mask.save(mask_path)

        overlay = base.convert("RGBA")
        draw = ImageDraw.Draw(overlay)
        line_width = max(2, base.size[0] // 300)
        for region in regions:
            bbox = region.get("bbox")
            if bbox and len(bbox) == 4:
                draw.ellipse([int(v) for v in bbox], outline=(255, 0, 0, 255), width=line_width)
                label = f"C1 {region.get('toothCode') or '16'}"
                x1, y1 = int(bbox[0]), int(bbox[1])
                draw.rectangle([x1, max(0, y1 - 24), x1 + 90, y1], fill=(255, 0, 0, 180))
                draw.text((x1 + 6, max(0, y1 - 21)), label, fill=(255, 255, 255, 255))
        overlay.convert("RGB").save(overlay_path)

        blurred_mask = mask.filter(ImageFilter.GaussianBlur(radius=max(2, base.size[0] // 160)))
        alpha = np.asarray(blurred_mask, dtype=np.float64)
        if alpha.max() > 0:
            alpha = alpha / alpha.max() * 170.0
        heat = Image.new("RGBA", base.size, color=(255, 80, 0, 0))
        heat.putalpha(Image.fromarray(alpha.astype(np.uint8), mode="L"))
        Image.alpha_composite(base.convert("RGBA"), heat).convert("RGB").save(heatmap_path)

    @classmethod
    def _draw_mock_assets(
        cls,
        image_path: Path | None,
        mask_path: Path,
        overlay_path: Path,
        heatmap_path: Path,
    ) -> None:
        try:
            base = Image.open(image_path).convert("RGB") if image_path else Image.new("RGB", (512, 256))
        except Exception:
            base = Image.new("RGB", (512, 256), color=(245, 245, 245))
        width, height = base.size
        box = cls._stable_box(width, height)

        mask = Image.new("RGBA", base.size, color=(0, 0, 0, 0))
        draw_mask = ImageDraw.Draw(mask)
        draw_mask.ellipse(box, fill=(255, 255, 255, 220))
        mask.save(mask_path)

        overlay = base.copy().convert("RGBA")
        draw_overlay = ImageDraw.Draw(overlay)
        draw_overlay.ellipse(box, outline=(255, 0, 0, 255), width=max(2, width // 300))
        draw_overlay.rectangle([box[0], max(0, box[1] - 24), box[0] + 90, box[1]], fill=(255, 0, 0, 180))
        draw_overlay.text((box[0] + 6, max(0, box[1] - 21)), "C1 16", fill=(255, 255, 255, 255))
        overlay.convert("RGB").save(overlay_path)

        heatmap = Image.new("RGBA", base.size, color=(0, 0, 0, 0))
        draw_heatmap = ImageDraw.Draw(heatmap)
        draw_heatmap.ellipse(box, fill=(255, 80, 0, 150))
        Image.alpha_composite(base.convert("RGBA"), heatmap).convert("RGB").save(heatmap_path)

    @staticmethod
    def _image_size(image_path: Path | None) -> tuple[int, int]:
        try:
            return Image.open(image_path).size if image_path else (512, 256)
        except Exception:
            return (512, 256)

    @staticmethod
    def _stable_box(width: int, height: int) -> list[int]:
        x1 = max(0, int(width * 0.35))
        y1 = max(0, int(height * 0.35))
        x2 = min(width - 1, int(width * 0.55))
        y2 = min(height - 1, int(height * 0.65))
        return [x1, y1, x2, y2]
