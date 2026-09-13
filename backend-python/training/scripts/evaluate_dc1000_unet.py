from __future__ import annotations

import argparse
import csv
import json
import sys
import time
from pathlib import Path
from typing import Any

import matplotlib
import numpy as np
import torch
from PIL import Image
from torch import nn

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402


BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from training.common import DatasetRecord, load_dataset_records  # noqa: E402


DEFAULT_DATASET_DIR = REPO_ROOT / "data" / "processed" / "dc1000" / "segmentation_v1"
DEFAULT_RUN_DIR = (
    REPO_ROOT / "artifacts" / "training" / "dc1000_segmentation" / "dc1000_unet_v1"
)
DEFAULT_CLASS_MAP = BACKEND_ROOT / "assets" / "datasets" / "caries_v1" / "meta" / "class_map.json"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Choose a segmentation threshold on DC1000 validation data, then evaluate "
            "the locked model and threshold once on the official test set."
        )
    )
    parser.add_argument("--checkpoint", default=str(DEFAULT_RUN_DIR / "weights" / "best.pt"))
    parser.add_argument(
        "--val-manifest", default=str(DEFAULT_DATASET_DIR / "manifests" / "val.jsonl")
    )
    parser.add_argument(
        "--test-manifest", default=str(DEFAULT_DATASET_DIR / "manifests" / "test.jsonl")
    )
    parser.add_argument("--class-map", default=str(DEFAULT_CLASS_MAP))
    parser.add_argument("--output-dir", default=str(DEFAULT_RUN_DIR / "official_evaluation"))
    parser.add_argument("--threshold-min", type=float, default=0.10)
    parser.add_argument("--threshold-max", type=float, default=0.90)
    parser.add_argument("--threshold-step", type=float, default=0.05)
    parser.add_argument("--overlap", type=float, default=0.25)
    parser.add_argument("--sliding-window-batch-size", type=int, default=4)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--amp", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite an existing official evaluation. Avoid this during model development.",
    )
    return parser


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def resolve_device(requested: str) -> torch.device:
    name = requested.strip().lower()
    if name == "auto":
        return torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    if name.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("CUDA was requested but torch.cuda.is_available() is false")
    return torch.device(requested)


def normalize_image(array: np.ndarray) -> np.ndarray:
    image = array.astype(np.float32) / 255.0
    lower, upper = np.percentile(image, (1.0, 99.0))
    if upper > lower + 1e-6:
        image = np.clip((image - lower) / (upper - lower), 0.0, 1.0)
    return (image - 0.5) / 0.5


def create_model() -> nn.Module:
    try:
        from monai.networks.nets import UNet
    except ImportError as exc:
        raise RuntimeError("MONAI is required. Use the existing backend-python/.venv environment.") from exc
    return UNet(
        spatial_dims=2,
        in_channels=1,
        out_channels=1,
        channels=(16, 32, 64, 128, 256),
        strides=(2, 2, 2, 2),
        num_res_units=2,
        norm="BATCH",
    )


def thresholds_from_args(args: argparse.Namespace) -> list[float]:
    if not 0.0 < args.threshold_min <= args.threshold_max < 1.0:
        raise ValueError("threshold range must be inside (0, 1)")
    if args.threshold_step <= 0:
        raise ValueError("--threshold-step must be positive")
    count = int(round((args.threshold_max - args.threshold_min) / args.threshold_step))
    values = [round(args.threshold_min + index * args.threshold_step, 6) for index in range(count + 1)]
    if values[-1] < args.threshold_max - 1e-8:
        values.append(round(args.threshold_max, 6))
    return values


def empty_counts() -> dict[str, int]:
    return {"tp": 0, "fp": 0, "fn": 0, "tn": 0}


def update_counts(counts: dict[str, int], prediction: np.ndarray, target: np.ndarray) -> None:
    counts["tp"] += int(np.logical_and(prediction, target).sum())
    counts["fp"] += int(np.logical_and(prediction, np.logical_not(target)).sum())
    counts["fn"] += int(np.logical_and(np.logical_not(prediction), target).sum())
    counts["tn"] += int(np.logical_and(np.logical_not(prediction), np.logical_not(target)).sum())


def metric_payload(counts: dict[str, int]) -> dict[str, float | int]:
    tp, fp, fn, tn = (counts[key] for key in ("tp", "fp", "fn", "tn"))

    def divide(numerator: float, denominator: float) -> float:
        return numerator / denominator if denominator else 0.0

    return {
        "dice": divide(2 * tp, 2 * tp + fp + fn),
        "iou": divide(tp, tp + fp + fn),
        "precision": divide(tp, tp + fp),
        "recall": divide(tp, tp + fn),
        "specificity": divide(tn, tn + fp),
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "tn": tn,
    }


@torch.inference_mode()
def predict_probability(
    *,
    model: nn.Module,
    image_array: np.ndarray,
    device: torch.device,
    patch_size: int,
    overlap: float,
    sliding_window_batch_size: int,
    amp_enabled: bool,
) -> np.ndarray:
    from monai.inferers import sliding_window_inference

    tensor = torch.from_numpy(normalize_image(image_array)[None, None, ...]).float().to(device)
    with torch.autocast(device_type=device.type, enabled=amp_enabled):
        logits = sliding_window_inference(
            tensor,
            roi_size=(patch_size, patch_size),
            sw_batch_size=sliding_window_batch_size,
            predictor=model,
            overlap=overlap,
            mode="gaussian",
        )
    return torch.sigmoid(logits)[0, 0].float().cpu().numpy()


def calibrate_thresholds(
    *,
    model: nn.Module,
    records: list[DatasetRecord],
    thresholds: list[float],
    device: torch.device,
    patch_size: int,
    overlap: float,
    sliding_window_batch_size: int,
    amp_enabled: bool,
) -> list[dict[str, Any]]:
    counts = {threshold: empty_counts() for threshold in thresholds}
    model.eval()
    for index, record in enumerate(records, start=1):
        image = np.asarray(Image.open(record.image_path).convert("L"), dtype=np.uint8)
        target = np.asarray(Image.open(record.mask_path).convert("L"), dtype=np.uint8) > 0
        probability = predict_probability(
            model=model,
            image_array=image,
            device=device,
            patch_size=patch_size,
            overlap=overlap,
            sliding_window_batch_size=sliding_window_batch_size,
            amp_enabled=amp_enabled,
        )
        for threshold in thresholds:
            update_counts(counts[threshold], probability >= threshold, target)
        if index == 1 or index % 10 == 0 or index == len(records):
            print(f"validation threshold calibration: {index}/{len(records)}")

    return [
        {"threshold": threshold, **metric_payload(counts[threshold])}
        for threshold in thresholds
    ]


def save_threshold_csv(path: Path, rows: list[dict[str, Any]]) -> None:
    with path.open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def selected_preview_indices(count: int, preview_count: int = 6) -> set[int]:
    if count <= preview_count:
        return set(range(count))
    return {round(index * (count - 1) / (preview_count - 1)) for index in range(preview_count)}


def color_overlay(image: np.ndarray, mask: np.ndarray, color: tuple[int, int, int]) -> np.ndarray:
    base = np.repeat(image[..., None], 3, axis=2).astype(np.float32)
    color_array = np.asarray(color, dtype=np.float32)
    base[mask] = base[mask] * 0.35 + color_array * 0.65
    return np.clip(base, 0, 255).astype(np.uint8)


def error_overlay(image: np.ndarray, prediction: np.ndarray, target: np.ndarray) -> np.ndarray:
    base = np.repeat(image[..., None], 3, axis=2).astype(np.float32)
    states = [
        (np.logical_and(prediction, target), np.asarray((38, 137, 12))),
        (np.logical_and(prediction, np.logical_not(target)), np.asarray((232, 117, 0))),
        (np.logical_and(np.logical_not(prediction), target), np.asarray((193, 21, 116))),
    ]
    for mask, color in states:
        base[mask] = base[mask] * 0.25 + color * 0.75
    return np.clip(base, 0, 255).astype(np.uint8)


def save_previews(path: Path, rows: list[dict[str, Any]]) -> None:
    figure, axes = plt.subplots(len(rows), 4, figsize=(16, 2.7 * len(rows)), constrained_layout=True)
    if len(rows) == 1:
        axes = np.asarray([axes])
    titles = ["Panoramic image", "Ground truth (blue)", "Prediction (orange)", "Errors"]
    for column, title in enumerate(titles):
        axes[0, column].set_title(title, fontsize=12, fontweight="bold")
    for row_index, row in enumerate(rows):
        image = row["image"]
        target = row["target"]
        prediction = row["prediction"]
        panels = [
            image,
            color_overlay(image, target, (23, 92, 211)),
            color_overlay(image, prediction, (232, 117, 0)),
            error_overlay(image, prediction, target),
        ]
        for column, panel in enumerate(panels):
            axes[row_index, column].imshow(panel, cmap="gray" if column == 0 else None)
            axes[row_index, column].axis("off")
        axes[row_index, 0].set_ylabel(
            f"{row['imageId']}\nDice {row['dice']:.3f}", rotation=0, labelpad=70, va="center"
        )
    figure.text(
        0.5,
        0.002,
        "Error colors: green = correct lesion, orange = false positive, pink = missed lesion",
        ha="center",
        fontsize=10,
        color="#344054",
    )
    figure.savefig(path, dpi=160, facecolor="white")
    plt.close(figure)


def evaluate_test(
    *,
    model: nn.Module,
    records: list[DatasetRecord],
    threshold: float,
    device: torch.device,
    patch_size: int,
    overlap: float,
    sliding_window_batch_size: int,
    amp_enabled: bool,
) -> tuple[dict[str, Any], list[dict[str, Any]], list[dict[str, Any]]]:
    counts = empty_counts()
    per_image: list[dict[str, Any]] = []
    preview_indices = selected_preview_indices(len(records))
    previews: list[dict[str, Any]] = []
    model.eval()
    for zero_index, record in enumerate(records):
        image = np.asarray(Image.open(record.image_path).convert("L"), dtype=np.uint8)
        target = np.asarray(Image.open(record.mask_path).convert("L"), dtype=np.uint8) > 0
        probability = predict_probability(
            model=model,
            image_array=image,
            device=device,
            patch_size=patch_size,
            overlap=overlap,
            sliding_window_batch_size=sliding_window_batch_size,
            amp_enabled=amp_enabled,
        )
        prediction = probability >= threshold
        image_counts = empty_counts()
        update_counts(image_counts, prediction, target)
        update_counts(counts, prediction, target)
        metrics = metric_payload(image_counts)
        per_image.append({"imageId": record.image_id, **metrics})
        if zero_index in preview_indices:
            previews.append(
                {
                    "imageId": record.image_id,
                    "dice": float(metrics["dice"]),
                    "image": image,
                    "target": target,
                    "prediction": prediction,
                }
            )
        completed = zero_index + 1
        if completed == 1 or completed % 10 == 0 or completed == len(records):
            print(f"official test evaluation: {completed}/{len(records)}")
    metrics = metric_payload(counts)
    metrics["positiveImageDiceMean"] = float(np.mean([row["dice"] for row in per_image]))
    return metrics, per_image, previews


def main() -> int:
    args = build_parser().parse_args()
    output_dir = Path(args.output_dir).resolve()
    result_path = output_dir / "result.json"
    if result_path.exists() and not args.force:
        raise RuntimeError(
            f"official evaluation already exists: {result_path}. "
            "Do not repeatedly tune against the test set."
        )
    output_dir.mkdir(parents=True, exist_ok=True)

    device = resolve_device(args.device)
    amp_enabled = bool(args.amp and device.type == "cuda")
    checkpoint_path = Path(args.checkpoint).resolve()
    checkpoint = torch.load(checkpoint_path, map_location=device, weights_only=False)
    if checkpoint.get("smokeRun"):
        raise RuntimeError("refusing to evaluate a smoke-run checkpoint")
    model = create_model().to(device)
    model.load_state_dict(checkpoint["modelStateDict"])
    patch_size = int(checkpoint["patchSize"])

    val_records, _, _ = load_dataset_records(args.val_manifest, args.class_map, require_mask=True)
    test_records, _, _ = load_dataset_records(args.test_manifest, args.class_map, require_mask=True)
    thresholds = thresholds_from_args(args)
    started = time.time()
    threshold_rows = calibrate_thresholds(
        model=model,
        records=val_records,
        thresholds=thresholds,
        device=device,
        patch_size=patch_size,
        overlap=args.overlap,
        sliding_window_batch_size=args.sliding_window_batch_size,
        amp_enabled=amp_enabled,
    )
    selected = max(
        threshold_rows,
        key=lambda row: (float(row["dice"]), float(row["recall"]), float(row["precision"])),
    )
    selected_threshold = float(selected["threshold"])
    write_json(output_dir / "validation_thresholds.json", threshold_rows)
    save_threshold_csv(output_dir / "validation_thresholds.csv", threshold_rows)
    write_json(output_dir / "selected_validation_operating_point.json", selected)
    print(
        "locked validation operating point: "
        f"threshold={selected_threshold:.2f} dice={float(selected['dice']):.4f} "
        f"precision={float(selected['precision']):.4f} recall={float(selected['recall']):.4f}"
    )

    test_metrics, per_image, previews = evaluate_test(
        model=model,
        records=test_records,
        threshold=selected_threshold,
        device=device,
        patch_size=patch_size,
        overlap=args.overlap,
        sliding_window_batch_size=args.sliding_window_batch_size,
        amp_enabled=amp_enabled,
    )
    save_threshold_csv(output_dir / "official_test_per_image.csv", per_image)
    save_previews(output_dir / "official_test_examples.png", previews)

    result = {
        "modelCode": checkpoint.get("modelCode", "dc1000-unet-v1"),
        "checkpoint": str(checkpoint_path),
        "checkpointEpoch": int(checkpoint["epoch"]),
        "selectionProtocol": "threshold selected only on validation Dice; official test evaluated afterward",
        "validationImageCount": len(val_records),
        "selectedThreshold": selected_threshold,
        "selectedValidationMetrics": selected,
        "officialTestImageCount": len(test_records),
        "officialTestMetrics": test_metrics,
        "device": str(device),
        "gpu": torch.cuda.get_device_name(device) if device.type == "cuda" else None,
        "elapsedSeconds": time.time() - started,
        "artifacts": {
            "thresholdTable": str((output_dir / "validation_thresholds.csv").resolve()),
            "perImageMetrics": str((output_dir / "official_test_per_image.csv").resolve()),
            "qualitativeExamples": str((output_dir / "official_test_examples.png").resolve()),
        },
        "limitations": [
            "DC1000 is research data and does not establish clinical safety.",
            "The official test set contains no normal/negative panoramics in this extracted release.",
            "Metrics assess binary lesion pixels, not caries depth or tooth-level diagnosis.",
        ],
    }
    write_json(result_path, result)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
