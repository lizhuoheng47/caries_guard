from __future__ import annotations

import argparse
import json
import math
import random
import sys
import time
from pathlib import Path
from typing import Any

import numpy as np
import torch
from PIL import Image
from torch import nn
from torch.utils.data import DataLoader, Dataset


BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from training.common import DatasetRecord, load_dataset_records  # noqa: E402


DEFAULT_DATASET_DIR = REPO_ROOT / "data" / "processed" / "dc1000" / "segmentation_v1"
DEFAULT_CLASS_MAP = BACKEND_ROOT / "assets" / "datasets" / "caries_v1" / "meta" / "class_map.json"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "artifacts" / "training" / "dc1000_segmentation" / "dc1000_unet_v1"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Train a low-cost MONAI U-Net baseline on DC1000 panoramic lesion masks."
    )
    parser.add_argument(
        "--train-manifest",
        default=str(DEFAULT_DATASET_DIR / "manifests" / "train.jsonl"),
    )
    parser.add_argument(
        "--val-manifest",
        default=str(DEFAULT_DATASET_DIR / "manifests" / "val.jsonl"),
    )
    parser.add_argument("--class-map", default=str(DEFAULT_CLASS_MAP))
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--epochs", type=int, default=40)
    parser.add_argument("--batch-size", type=int, default=2)
    parser.add_argument("--patch-size", type=int, default=512)
    parser.add_argument("--patches-per-image", type=int, default=4)
    parser.add_argument("--positive-patch-probability", type=float, default=0.75)
    parser.add_argument("--negative-crop-attempts", type=int, default=12)
    parser.add_argument("--learning-rate", type=float, default=2e-4)
    parser.add_argument("--weight-decay", type=float, default=1e-4)
    parser.add_argument("--bce-positive-weight", type=float, default=6.0)
    parser.add_argument("--bce-loss-weight", type=float, default=0.5)
    parser.add_argument("--threshold", type=float, default=0.5)
    parser.add_argument("--sliding-window-overlap", type=float, default=0.25)
    parser.add_argument("--sliding-window-batch-size", type=int, default=4)
    parser.add_argument("--patience", type=int, default=8)
    parser.add_argument("--num-workers", type=int, default=0)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--amp", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument(
        "--max-train-samples",
        type=int,
        default=None,
        help="Limit source panoramics for a smoke run only.",
    )
    parser.add_argument(
        "--max-val-samples",
        type=int,
        default=None,
        help="Limit validation panoramics for a smoke run only.",
    )
    parser.add_argument(
        "--smoke-run",
        action="store_true",
        help="Mark outputs as a pipeline check, never as a model candidate.",
    )
    return parser


def seed_everything(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def resolve_device(requested: str) -> torch.device:
    name = requested.strip().lower()
    if name == "auto":
        return torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    if name.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("CUDA was requested but torch.cuda.is_available() is false")
    return torch.device(requested)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def normalize_image(array: np.ndarray) -> np.ndarray:
    image = array.astype(np.float32) / 255.0
    lower, upper = np.percentile(image, (1.0, 99.0))
    if upper > lower + 1e-6:
        image = np.clip((image - lower) / (upper - lower), 0.0, 1.0)
    return (image - 0.5) / 0.5


def pad_to_patch(
    image: np.ndarray, mask: np.ndarray, patch_size: int
) -> tuple[np.ndarray, np.ndarray]:
    height, width = image.shape
    pad_height = max(0, patch_size - height)
    pad_width = max(0, patch_size - width)
    if not pad_height and not pad_width:
        return image, mask
    image = np.pad(image, ((0, pad_height), (0, pad_width)), mode="edge")
    mask = np.pad(mask, ((0, pad_height), (0, pad_width)), mode="constant")
    return image, mask


def random_crop_origin(
    rng: random.Random, width: int, height: int, patch_size: int
) -> tuple[int, int]:
    return (
        rng.randint(0, max(0, width - patch_size)),
        rng.randint(0, max(0, height - patch_size)),
    )


class PanoramicPatchDataset(Dataset[tuple[torch.Tensor, torch.Tensor]]):
    def __init__(
        self,
        records: list[DatasetRecord],
        patch_size: int,
        patches_per_image: int,
        positive_probability: float,
        negative_crop_attempts: int,
        seed: int,
    ) -> None:
        self.records = records
        self.patch_size = patch_size
        self.patches_per_image = patches_per_image
        self.positive_probability = positive_probability
        self.negative_crop_attempts = negative_crop_attempts
        self.seed = seed
        self.epoch = 0

    def set_epoch(self, epoch: int) -> None:
        self.epoch = epoch

    def __len__(self) -> int:
        return len(self.records) * self.patches_per_image

    def __getitem__(self, index: int) -> tuple[torch.Tensor, torch.Tensor]:
        record = self.records[index % len(self.records)]
        rng = random.Random(self.seed + self.epoch * len(self) + index)
        image = np.asarray(Image.open(record.image_path).convert("L"), dtype=np.uint8)
        mask = np.asarray(Image.open(record.mask_path).convert("L"), dtype=np.uint8) > 0
        image = normalize_image(image)
        image, mask = pad_to_patch(image, mask, self.patch_size)
        height, width = image.shape

        positive_points = np.argwhere(mask)
        use_positive = positive_points.size > 0 and rng.random() < self.positive_probability
        if use_positive:
            point_y, point_x = positive_points[rng.randrange(len(positive_points))]
            jitter = self.patch_size // 4
            center_x = int(point_x) + rng.randint(-jitter, jitter)
            center_y = int(point_y) + rng.randint(-jitter, jitter)
            x0 = min(max(0, center_x - self.patch_size // 2), width - self.patch_size)
            y0 = min(max(0, center_y - self.patch_size // 2), height - self.patch_size)
        else:
            candidates: list[tuple[int, int, int]] = []
            for _ in range(max(1, self.negative_crop_attempts)):
                candidate_x, candidate_y = random_crop_origin(rng, width, height, self.patch_size)
                foreground = int(
                    mask[
                        candidate_y : candidate_y + self.patch_size,
                        candidate_x : candidate_x + self.patch_size,
                    ].sum()
                )
                candidates.append((foreground, candidate_x, candidate_y))
                if foreground == 0:
                    break
            _, x0, y0 = min(candidates)

        image_patch = image[y0 : y0 + self.patch_size, x0 : x0 + self.patch_size]
        mask_patch = mask[y0 : y0 + self.patch_size, x0 : x0 + self.patch_size]

        if rng.random() < 0.5:
            image_patch = np.fliplr(image_patch)
            mask_patch = np.fliplr(mask_patch)
        image_patch = np.ascontiguousarray(image_patch, dtype=np.float32)
        mask_patch = np.ascontiguousarray(mask_patch.astype(np.float32))
        return (
            torch.from_numpy(image_patch[None, ...]).float(),
            torch.from_numpy(mask_patch[None, ...]).float(),
        )


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


def soft_dice_loss(logits: torch.Tensor, targets: torch.Tensor) -> torch.Tensor:
    probabilities = torch.sigmoid(logits)
    dims = tuple(range(1, probabilities.ndim))
    intersection = (probabilities * targets).sum(dim=dims)
    denominator = probabilities.sum(dim=dims) + targets.sum(dim=dims)
    return (1.0 - (2.0 * intersection + 1.0) / (denominator + 1.0)).mean()


def combined_loss(
    logits: torch.Tensor,
    targets: torch.Tensor,
    bce_loss: nn.Module,
    bce_weight: float,
) -> torch.Tensor:
    return soft_dice_loss(logits, targets) + bce_weight * bce_loss(logits, targets)


def metric_payload(tp: int, fp: int, fn: int, tn: int) -> dict[str, float | int]:
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
def validate_full_images(
    *,
    model: nn.Module,
    records: list[DatasetRecord],
    device: torch.device,
    patch_size: int,
    overlap: float,
    sw_batch_size: int,
    threshold: float,
    amp_enabled: bool,
) -> dict[str, Any]:
    try:
        from monai.inferers import sliding_window_inference
    except ImportError as exc:
        raise RuntimeError("MONAI is required for sliding-window validation.") from exc

    model.eval()
    tp = fp = fn = tn = 0
    positive_image_dice: list[float] = []
    negative_image_false_positive_ratios: list[float] = []
    for record in records:
        image_array = np.asarray(Image.open(record.image_path).convert("L"), dtype=np.uint8)
        target_array = np.asarray(Image.open(record.mask_path).convert("L"), dtype=np.uint8) > 0
        image_tensor = torch.from_numpy(normalize_image(image_array)[None, None, ...]).float().to(device)
        with torch.autocast(device_type=device.type, enabled=amp_enabled):
            logits = sliding_window_inference(
                image_tensor,
                roi_size=(patch_size, patch_size),
                sw_batch_size=sw_batch_size,
                predictor=model,
                overlap=overlap,
                mode="gaussian",
            )
        prediction = torch.sigmoid(logits)[0, 0].float().cpu().numpy() >= threshold
        target = target_array
        image_tp = int(np.logical_and(prediction, target).sum())
        image_fp = int(np.logical_and(prediction, np.logical_not(target)).sum())
        image_fn = int(np.logical_and(np.logical_not(prediction), target).sum())
        image_tn = int(np.logical_and(np.logical_not(prediction), np.logical_not(target)).sum())
        tp += image_tp
        fp += image_fp
        fn += image_fn
        tn += image_tn
        if target.any():
            positive_image_dice.append(
                (2.0 * image_tp) / max(1, 2 * image_tp + image_fp + image_fn)
            )
        else:
            negative_image_false_positive_ratios.append(float(prediction.mean()))

    metrics = metric_payload(tp, fp, fn, tn)
    metrics.update(
        {
            "positiveImageDiceMean": (
                float(np.mean(positive_image_dice)) if positive_image_dice else 0.0
            ),
            "positiveImageCount": len(positive_image_dice),
            "negativeImageCount": len(negative_image_false_positive_ratios),
            "negativeImageFalsePositiveRatioMean": (
                float(np.mean(negative_image_false_positive_ratios))
                if negative_image_false_positive_ratios
                else 0.0
            ),
        }
    )
    return metrics


def checkpoint_payload(
    *,
    model: nn.Module,
    optimizer: torch.optim.Optimizer,
    epoch: int,
    args: argparse.Namespace,
    metrics: dict[str, Any],
) -> dict[str, Any]:
    return {
        "modelCode": "dc1000-unet-v1",
        "modelType": "BINARY_SEGMENTATION",
        "architecture": "MONAI_UNet",
        "epoch": epoch,
        "modelStateDict": model.state_dict(),
        "optimizerStateDict": optimizer.state_dict(),
        "validationMetrics": metrics,
        "patchSize": args.patch_size,
        "threshold": args.threshold,
        "smokeRun": bool(args.smoke_run),
        "trainingArgs": vars(args),
    }


def main() -> int:
    args = build_parser().parse_args()
    if args.patch_size <= 0 or args.patch_size % 16:
        raise ValueError("--patch-size must be positive and divisible by 16")
    if not 0.0 <= args.positive_patch_probability <= 1.0:
        raise ValueError("--positive-patch-probability must be between 0 and 1")
    if not 0.0 <= args.sliding_window_overlap < 1.0:
        raise ValueError("--sliding-window-overlap must be in [0, 1)")

    seed_everything(args.seed)
    device = resolve_device(args.device)
    amp_enabled = bool(args.amp and device.type == "cuda")
    output_dir = Path(args.output_dir).resolve()
    weights_dir = output_dir / "weights"
    weights_dir.mkdir(parents=True, exist_ok=True)

    train_records, _, _ = load_dataset_records(args.train_manifest, args.class_map, require_mask=True)
    val_records, _, _ = load_dataset_records(args.val_manifest, args.class_map, require_mask=True)
    if args.max_train_samples is not None:
        train_records = train_records[: args.max_train_samples]
    if args.max_val_samples is not None:
        val_records = val_records[: args.max_val_samples]
    if not train_records or not val_records:
        raise RuntimeError("training and validation records must both be non-empty")

    dataset = PanoramicPatchDataset(
        records=train_records,
        patch_size=args.patch_size,
        patches_per_image=args.patches_per_image,
        positive_probability=args.positive_patch_probability,
        negative_crop_attempts=args.negative_crop_attempts,
        seed=args.seed,
    )
    loader_generator = torch.Generator().manual_seed(args.seed)
    loader = DataLoader(
        dataset,
        batch_size=args.batch_size,
        shuffle=True,
        num_workers=args.num_workers,
        pin_memory=device.type == "cuda",
        generator=loader_generator,
        persistent_workers=args.num_workers > 0,
    )

    model = create_model().to(device)
    optimizer = torch.optim.AdamW(
        model.parameters(), lr=args.learning_rate, weight_decay=args.weight_decay
    )
    bce_loss = nn.BCEWithLogitsLoss(
        pos_weight=torch.tensor([args.bce_positive_weight], device=device)
    )
    scaler = torch.amp.GradScaler(device.type, enabled=amp_enabled)

    configuration = {
        **vars(args),
        "deviceResolved": str(device),
        "gpu": torch.cuda.get_device_name(device) if device.type == "cuda" else None,
        "torchVersion": torch.__version__,
        "trainPanoramics": len(train_records),
        "valPanoramics": len(val_records),
        "patchesPerEpoch": len(dataset),
        "modelParameters": sum(parameter.numel() for parameter in model.parameters()),
    }
    write_json(output_dir / "training_config.json", configuration)
    print(json.dumps(configuration, ensure_ascii=False, indent=2))

    history: list[dict[str, Any]] = []
    best_dice = -math.inf
    best_epoch = 0
    epochs_without_improvement = 0
    started_at = time.time()
    for epoch in range(1, args.epochs + 1):
        epoch_started = time.time()
        dataset.set_epoch(epoch)
        model.train()
        running_loss = 0.0
        sample_count = 0
        for images, targets in loader:
            images = images.to(device, non_blocking=True)
            targets = targets.to(device, non_blocking=True)
            optimizer.zero_grad(set_to_none=True)
            with torch.autocast(device_type=device.type, enabled=amp_enabled):
                logits = model(images)
                loss = combined_loss(logits, targets, bce_loss, args.bce_loss_weight)
            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()
            batch_count = int(images.shape[0])
            running_loss += float(loss.item()) * batch_count
            sample_count += batch_count

        validation = validate_full_images(
            model=model,
            records=val_records,
            device=device,
            patch_size=args.patch_size,
            overlap=args.sliding_window_overlap,
            sw_batch_size=args.sliding_window_batch_size,
            threshold=args.threshold,
            amp_enabled=amp_enabled,
        )
        epoch_result = {
            "epoch": epoch,
            "trainLoss": running_loss / max(1, sample_count),
            "validation": validation,
            "epochSeconds": time.time() - epoch_started,
        }
        history.append(epoch_result)
        print(json.dumps(epoch_result, ensure_ascii=False))

        torch.save(
            checkpoint_payload(
                model=model,
                optimizer=optimizer,
                epoch=epoch,
                args=args,
                metrics=validation,
            ),
            weights_dir / "last.pt",
        )
        current_dice = float(validation["dice"])
        if current_dice > best_dice + 1e-6:
            best_dice = current_dice
            best_epoch = epoch
            epochs_without_improvement = 0
            torch.save(
                checkpoint_payload(
                    model=model,
                    optimizer=optimizer,
                    epoch=epoch,
                    args=args,
                    metrics=validation,
                ),
                weights_dir / "best.pt",
            )
        else:
            epochs_without_improvement += 1
        write_json(output_dir / "history.json", history)
        if epochs_without_improvement >= args.patience:
            print(f"Early stopping at epoch {epoch}; best epoch was {best_epoch}.")
            break

    result = {
        "modelCode": "dc1000-unet-v1",
        "modelType": "BINARY_SEGMENTATION",
        "smokeRun": bool(args.smoke_run),
        "bestEpoch": best_epoch,
        "bestValidationDice": best_dice,
        "bestCheckpoint": str((weights_dir / "best.pt").resolve()),
        "lastCheckpoint": str((weights_dir / "last.pt").resolve()),
        "epochsCompleted": len(history),
        "totalSeconds": time.time() - started_at,
        "limitations": [
            "DC1000 is research data and is not clinical validation.",
            "The model predicts a binary caries-lesion mask, not lesion depth.",
            "The official test set is intentionally not evaluated by this training script.",
        ],
    }
    write_json(output_dir / "result.json", result)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
