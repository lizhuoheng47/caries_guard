from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image


BACKEND_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

DEFAULT_IMAGE = (
    REPO_ROOT
    / "data"
    / "raw"
    / "dc1000"
    / "full"
    / "DC1000_dataset"
    / "org_test_dataset"
    / "images"
    / "1008.png"
)
DEFAULT_MASK = DEFAULT_IMAGE.parents[1] / "labels" / DEFAULT_IMAGE.name
DEFAULT_OUTPUT = (
    REPO_ROOT / "artifacts" / "runtime_smoke" / "dc1000_unet_v1" / "sample_1008"
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Run the production segmentation registry and pipeline on one panoramic image."
    )
    parser.add_argument("--image", default=str(DEFAULT_IMAGE))
    parser.add_argument("--ground-truth", default=str(DEFAULT_MASK))
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--image-id", type=int, default=1008)
    parser.add_argument("--device", default="cuda:0")
    return parser


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def binary_metrics(prediction: np.ndarray, target: np.ndarray) -> dict[str, float | int]:
    tp = int(np.logical_and(prediction, target).sum())
    fp = int(np.logical_and(prediction, np.logical_not(target)).sum())
    fn = int(np.logical_and(np.logical_not(prediction), target).sum())

    def divide(numerator: float, denominator: float) -> float:
        return numerator / denominator if denominator else 0.0

    return {
        "dice": divide(2 * tp, 2 * tp + fp + fn),
        "iou": divide(tp, tp + fp + fn),
        "precision": divide(tp, tp + fp),
        "recall": divide(tp, tp + fn),
        "tp": tp,
        "fp": fp,
        "fn": fn,
    }


def main() -> int:
    args = build_parser().parse_args()
    image_path = Path(args.image).resolve()
    ground_truth_path = Path(args.ground_truth).resolve() if args.ground_truth else None
    output_dir = Path(args.output_dir).resolve()
    if not image_path.is_file():
        raise FileNotFoundError(f"input image does not exist: {image_path}")

    os.environ["CG_AI_RUNTIME_MODE"] = "hybrid"
    os.environ["CG_MODEL_SEGMENTATION_ENABLED"] = "true"
    os.environ["CG_MODEL_SEGMENTATION_IMPL_TYPE"] = "ML_MODEL"
    os.environ["CG_MODEL_DEVICE"] = args.device
    os.environ["CG_STRICT_MODEL_STARTUP_VALIDATION"] = "true"
    os.environ["CG_MODEL_QUALITY_ENABLED"] = "false"
    os.environ["CG_MODEL_TOOTH_DETECT_ENABLED"] = "false"
    os.environ["CG_MODEL_GRADING_ENABLED"] = "false"
    os.environ["CG_MODEL_RISK_ENABLED"] = "false"

    from app.core.config import Settings
    from app.infra.model.model_assets import ModelAssets
    from app.infra.model.model_registry import ModelRegistry
    from app.pipelines.segmentation_pipeline import SegmentationPipeline
    from app.schemas.request import ImageInput

    settings = Settings()
    assets = ModelAssets(settings)
    registry = ModelRegistry(settings, assets)
    started = time.time()
    registry.startup()
    try:
        pipeline = SegmentationPipeline(registry, settings, assets)
        result = pipeline.segment(
            image=ImageInput(
                image_id=args.image_id,
                image_type_code="PANORAMIC",
                local_storage_path=str(image_path),
                original_filename=image_path.name,
            ),
            image_path=image_path,
            tooth_detections=[],
            output_dir=output_dir,
        )
    finally:
        registry.shutdown()

    mask = np.asarray(Image.open(result.mask_path).convert("L"), dtype=np.uint8) > 0
    report: dict[str, Any] = {
        "modelCode": assets.segmentation_manifest.model_code,
        "runtimeMode": settings.ai_runtime_mode,
        "implementationType": result.segmentation_impl_type,
        "device": result.raw_result.get("device"),
        "inputImage": str(image_path),
        "inputSize": result.raw_result.get("imageSize"),
        "modelTileSize": result.raw_result.get("modelInputSize"),
        "inferenceMode": result.raw_result.get("inferenceMode"),
        "slidingWindowCount": result.raw_result.get("slidingWindowCount"),
        "slidingWindowBatchSize": result.raw_result.get("slidingWindowBatchSize"),
        "maskThreshold": result.raw_result.get("maskThreshold"),
        "maskShape": [int(mask.shape[1]), int(mask.shape[0])],
        "maskPixels": int(mask.sum()),
        "regionCount": len(result.regions),
        "segmentationScore": result.raw_result.get("segmentationScore"),
        "elapsedSeconds": time.time() - started,
        "assets": {
            "mask": str(result.mask_path.resolve()),
            "overlay": str(result.overlay_path.resolve()),
            "heatmap": str(result.heatmap_path.resolve()),
        },
    }
    if ground_truth_path is not None and ground_truth_path.is_file():
        target = np.asarray(Image.open(ground_truth_path).convert("L"), dtype=np.uint8) > 0
        if target.shape != mask.shape:
            raise RuntimeError(
                f"ground-truth shape {list(target.shape)} does not match mask {list(mask.shape)}"
            )
        report["groundTruth"] = str(ground_truth_path)
        report["metrics"] = binary_metrics(mask, target)

    report_path = output_dir / "runtime_report.json"
    write_json(report_path, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
