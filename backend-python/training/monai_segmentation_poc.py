from __future__ import annotations

import hashlib
import json
import random
import shutil
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    yaml = None
    YAML_IMPORT_ERROR = exc
else:
    YAML_IMPORT_ERROR = None

try:
    import torch
    from torch import nn
    from torch.utils.data import DataLoader, Dataset
except ImportError as exc:  # pragma: no cover
    torch = None
    nn = None
    DataLoader = None
    Dataset = object
    TORCH_IMPORT_ERROR = exc
else:
    TORCH_IMPORT_ERROR = None

try:
    from monai.losses import DiceLoss
    from monai.networks.nets import UNet
except ImportError as exc:  # pragma: no cover
    DiceLoss = None
    UNet = None
    MONAI_IMPORT_ERROR = exc
else:
    MONAI_IMPORT_ERROR = None

try:
    import onnx
except ImportError as exc:  # pragma: no cover
    onnx = None
    ONNX_IMPORT_ERROR = exc
else:
    ONNX_IMPORT_ERROR = None

try:
    import onnxruntime as ort
except ImportError as exc:  # pragma: no cover
    ort = None
    ONNXRUNTIME_IMPORT_ERROR = exc
else:
    ONNXRUNTIME_IMPORT_ERROR = None


BACKEND_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = BACKEND_ROOT.parent
DEFAULT_INPUT_SIZE = (256, 256)
DEFAULT_MODEL_CODE = "caries-segmentation-v1"
DEFAULT_INPUT_NAME = "input"
DEFAULT_OUTPUT_NAME = "output"
DEFAULT_CLASS_MAP = {"background": 0, "caries_lesion": 1}
DEFAULT_RUNTIME_CLASS_MAP = {
    "datasetCode": "caries_guard_segmentation_poc",
    "manifestVersion": "1.0",
    "annotationVersion": "caries-segmentation-poc-v1",
    "maskEncoding": {"format": "png", "dtype": "uint8", "backgroundValue": 0, "ignoreIndex": 255},
    "segmentationClasses": [
        {"classId": 0, "classCode": "background", "displayName": "Background"},
        {"classId": 1, "classCode": "caries_lesion", "displayName": "Caries Lesion"},
    ],
}


@dataclass(frozen=True)
class PairRecord:
    image_id: str
    image_path: Path
    mask_path: Path
    image_size: tuple[int, int]
    mask_positive_pixels: int


@dataclass(frozen=True)
class RunConfig:
    dataset_dir: Path
    artifact_dir: Path
    epochs: int = 20
    batch_size: int = 4
    lr: float = 1e-4
    early_stopping_patience: int = 5
    input_size: tuple[int, int] = DEFAULT_INPUT_SIZE
    seed: int = 42
    val_ratio: float = 0.15
    device: str = "auto"
    num_workers: int = 0
    mask_threshold: float = 0.5
    min_region_area_px: int = 16


class PairingError(RuntimeError):
    pass


class SegmentationPairDataset(Dataset):
    def __init__(self, records: list[PairRecord], input_size: tuple[int, int]) -> None:
        self._records = records
        self._input_size = input_size

    def __len__(self) -> int:
        return len(self._records)

    def __getitem__(self, index: int) -> tuple[torch.Tensor, torch.Tensor]:
        record = self._records[index]
        image = Image.open(record.image_path).convert("L").resize(self._input_size, Image.Resampling.BILINEAR)
        mask = Image.open(record.mask_path).convert("L").resize(self._input_size, Image.Resampling.NEAREST)
        image_array = np.asarray(image, dtype=np.float32) / 255.0
        image_array = (image_array - 0.5) / 0.5
        mask_array = (np.asarray(mask, dtype=np.uint8) > 127).astype(np.float32)
        return torch.from_numpy(image_array[None, ...]).float(), torch.from_numpy(mask_array[None, ...]).float()


def ensure_dependencies() -> None:
    missing: list[str] = []
    if YAML_IMPORT_ERROR is not None:
        missing.append(f"PyYAML ({YAML_IMPORT_ERROR})")
    if TORCH_IMPORT_ERROR is not None:
        missing.append(f"torch ({TORCH_IMPORT_ERROR})")
    if MONAI_IMPORT_ERROR is not None:
        missing.append(f"monai ({MONAI_IMPORT_ERROR})")
    if ONNX_IMPORT_ERROR is not None:
        missing.append(f"onnx ({ONNX_IMPORT_ERROR})")
    if ONNXRUNTIME_IMPORT_ERROR is not None:
        missing.append(f"onnxruntime ({ONNXRUNTIME_IMPORT_ERROR})")
    if missing:
        raise RuntimeError(
            "training dependencies are missing; install backend-python/requirements-training.txt first: "
            + ", ".join(missing)
        )


def seed_everything(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)
    if hasattr(torch.backends, "cudnn"):
        torch.backends.cudnn.deterministic = True
        torch.backends.cudnn.benchmark = False


def resolve_device(device_name: str) -> torch.device:
    requested = str(device_name or "auto").strip().lower()
    if requested == "auto":
        return torch.device("cuda" if torch.cuda.is_available() else "cpu")
    if requested.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("CUDA device requested but torch.cuda.is_available() is false")
    return torch.device(requested)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def repo_relative(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(REPO_ROOT.resolve()))
    except ValueError:
        return str(path.resolve())


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2, default=_json_default),
        encoding="utf-8",
    )


def write_yaml(path: Path, payload: dict[str, Any]) -> None:
    if yaml is None:
        raise RuntimeError("PyYAML is required to write artifact yaml files")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(yaml.safe_dump(payload, sort_keys=False), encoding="utf-8")


def _json_default(value: Any) -> Any:
    if isinstance(value, Path):
        return str(value)
    if isinstance(value, tuple):
        return list(value)
    raise TypeError(f"unsupported JSON value: {type(value)!r}")


def load_binary_mask(mask_path: Path) -> np.ndarray:
    mask_array = np.asarray(Image.open(mask_path).convert("L"), dtype=np.uint8)
    unique_values = {int(value) for value in np.unique(mask_array)}
    if not unique_values.issubset({0, 255}):
        raise PairingError(f"mask contains non-binary pixel values {sorted(unique_values)}: {mask_path.name}")
    return (mask_array > 127).astype(np.uint8)


def scan_dataset(dataset_dir: Path) -> dict[str, Any]:
    if not dataset_dir.is_dir():
        raise FileNotFoundError(f"dataset directory does not exist: {dataset_dir}")

    image_extensions = {".png", ".jpg", ".jpeg"}
    raw_images = sorted(
        path
        for path in dataset_dir.iterdir()
        if path.is_file()
        and path.suffix.lower() in image_extensions
        and not path.name.lower().endswith("-mask.png")
    )
    mask_files = sorted(dataset_dir.glob("*-mask.png"))
    mask_lookup = {path.name.lower(): path for path in mask_files}
    raw_stems = {path.stem.lower() for path in raw_images}
    valid_pairs: list[PairRecord] = []
    skipped: list[dict[str, Any]] = []

    for image_path in raw_images:
        expected_mask_name = f"{image_path.stem}-mask.png".lower()
        mask_path = mask_lookup.get(expected_mask_name)
        if mask_path is None:
            skipped.append({"entryType": "image", "file": image_path.name, "reason": "missing_mask"})
            continue
        try:
            image_size = Image.open(image_path).size
            mask_binary = load_binary_mask(mask_path)
        except Exception as exc:
            skipped.append(
                {"entryType": "image", "file": image_path.name, "reason": "invalid_pair", "detail": str(exc)}
            )
            continue
        mask_size = (mask_binary.shape[1], mask_binary.shape[0])
        if image_size != mask_size:
            skipped.append(
                {
                    "entryType": "image",
                    "file": image_path.name,
                    "reason": "size_mismatch",
                    "detail": {"imageSize": list(image_size), "maskSize": list(mask_size), "maskFile": mask_path.name},
                }
            )
            continue
        valid_pairs.append(
            PairRecord(
                image_id=image_path.stem,
                image_path=image_path.resolve(),
                mask_path=mask_path.resolve(),
                image_size=image_size,
                mask_positive_pixels=int(mask_binary.sum()),
            )
        )

    for mask_path in mask_files:
        image_stem = mask_path.name[: -len("-mask.png")].lower()
        if image_stem not in raw_stems:
            skipped.append({"entryType": "mask", "file": mask_path.name, "reason": "missing_image"})

    if len(valid_pairs) < 2:
        raise RuntimeError(f"valid paired samples are insufficient for train/val split: {len(valid_pairs)}")

    return {
        "datasetPath": str(dataset_dir.resolve()),
        "rawImageCount": len(raw_images),
        "maskFileCount": len(mask_files),
        "pairedCount": len(valid_pairs),
        "skippedCount": len(skipped),
        "paired": valid_pairs,
        "skipped": skipped,
    }


def split_pairs(records: list[PairRecord], val_ratio: float, seed: int) -> tuple[list[PairRecord], list[PairRecord]]:
    shuffled = list(records)
    random.Random(seed).shuffle(shuffled)
    val_count = max(1, int(round(len(shuffled) * val_ratio)))
    val_count = min(len(shuffled) - 1, val_count)
    val_records = shuffled[:val_count]
    train_records = shuffled[val_count:]
    if not train_records or not val_records:
        raise RuntimeError(f"train/val split failed: train={len(train_records)} val={len(val_records)}")
    return train_records, val_records


def build_model() -> nn.Module:
    return UNet(
        spatial_dims=2,
        in_channels=1,
        out_channels=1,
        channels=(16, 32, 64, 128),
        strides=(2, 2, 2),
        num_res_units=1,
    )


def combined_loss(logits: torch.Tensor, targets: torch.Tensor, dice_loss: DiceLoss, bce_loss: nn.Module) -> torch.Tensor:
    return dice_loss(logits, targets) + bce_loss(logits, targets)


def compute_binary_metrics(logits: torch.Tensor, targets: torch.Tensor) -> dict[str, float]:
    probabilities = torch.sigmoid(logits)
    predictions = (probabilities >= 0.5).to(torch.int64)
    truths = targets.to(torch.int64)
    tp = int(((predictions == 1) & (truths == 1)).sum().item())
    tn = int(((predictions == 0) & (truths == 0)).sum().item())
    fp = int(((predictions == 1) & (truths == 0)).sum().item())
    fn = int(((predictions == 0) & (truths == 1)).sum().item())
    return {
        "dice": float((2.0 * tp) / max(1.0, (2.0 * tp + fp + fn))),
        "iou": float(tp / max(1.0, (tp + fp + fn))),
        "pixelAccuracy": float((tp + tn) / max(1.0, (tp + tn + fp + fn))),
    }


def evaluate(model: nn.Module, loader: DataLoader, device: torch.device, dice_loss: DiceLoss, bce_loss: nn.Module) -> dict[str, float]:
    model.eval()
    running_loss = 0.0
    batch_count = 0
    logits_list: list[torch.Tensor] = []
    mask_list: list[torch.Tensor] = []
    with torch.no_grad():
        for images, masks in loader:
            images = images.to(device)
            masks = masks.to(device)
            logits = model(images)
            running_loss += float(combined_loss(logits, masks, dice_loss, bce_loss).item())
            batch_count += 1
            logits_list.append(logits.detach().cpu())
            mask_list.append(masks.detach().cpu())
    all_logits = torch.cat(logits_list, dim=0)
    all_masks = torch.cat(mask_list, dim=0)
    metrics = compute_binary_metrics(all_logits, all_masks)
    metrics["loss"] = float(running_loss / max(1, batch_count))
    return metrics


def save_best_checkpoint(model: nn.Module, path: Path, config: RunConfig, history: list[dict[str, Any]], best_epoch: int, best_metrics: dict[str, float]) -> None:
    payload = {
        "modelCode": DEFAULT_MODEL_CODE,
        "taskName": "segmentation",
        "framework": "torch+monai",
        "architecture": "MONAI_UNet_2D",
        "inputSize": list(config.input_size),
        "numClasses": 2,
        "epochsRequested": config.epochs,
        "bestEpoch": best_epoch,
        "bestMetrics": best_metrics,
        "history": history,
        "classMap": DEFAULT_CLASS_MAP,
        "modelStateDict": model.state_dict(),
    }
    torch.save(payload, path)


def export_onnx(model: nn.Module, config: RunConfig, path: Path) -> None:
    dummy = torch.zeros((1, 1, config.input_size[1], config.input_size[0]), dtype=torch.float32)
    try:
        torch.onnx.export(
            model.cpu(),
            dummy,
            str(path),
            export_params=True,
            opset_version=17,
            do_constant_folding=True,
            input_names=[DEFAULT_INPUT_NAME],
            output_names=[DEFAULT_OUTPUT_NAME],
            dynamic_axes={DEFAULT_INPUT_NAME: {0: "batch"}, DEFAULT_OUTPUT_NAME: {0: "batch"}},
        )
    except Exception as exc:
        raise RuntimeError(f"ONNX export failed: {exc}") from exc
    try:
        onnx_model = onnx.load(str(path))
        onnx.checker.check_model(onnx_model)
    except Exception as exc:
        raise RuntimeError(f"exported ONNX validation failed: {exc}") from exc


def preprocess_for_inference(image_path: Path, input_size: tuple[int, int]) -> tuple[np.ndarray, tuple[int, int]]:
    image = Image.open(image_path).convert("L")
    original_size = image.size
    resized = image.resize(input_size, Image.Resampling.BILINEAR)
    image_array = np.asarray(resized, dtype=np.float32) / 255.0
    image_array = (image_array - 0.5) / 0.5
    return image_array[None, None, ...].astype(np.float32), original_size


def postprocess_probability(raw_output: np.ndarray, original_size: tuple[int, int], threshold: float) -> np.ndarray:
    output = np.asarray(raw_output, dtype=np.float32)
    if output.ndim == 4:
        output = output[0, 0]
    elif output.ndim == 3:
        output = output[0]
    probability = 1.0 / (1.0 + np.exp(-output))
    mask = (probability >= threshold).astype(np.uint8) * 255
    resized = Image.fromarray(mask, mode="L").resize(original_size, Image.Resampling.NEAREST)
    return np.asarray(resized, dtype=np.uint8)


def run_onnx_validation(artifact_dir: Path, sample: PairRecord, config: RunConfig) -> dict[str, Any]:
    session = ort.InferenceSession(str((artifact_dir / "model.onnx").resolve()), providers=["CPUExecutionProvider"])
    input_tensor, original_size = preprocess_for_inference(sample.image_path, config.input_size)
    outputs = session.run([DEFAULT_OUTPUT_NAME], {DEFAULT_INPUT_NAME: input_tensor})
    predicted_mask = postprocess_probability(outputs[0], original_size, config.mask_threshold)
    ground_truth = load_binary_mask(sample.mask_path).astype(np.uint8) * 255

    predicted_binary = (predicted_mask > 127).astype(np.uint8)
    truth_binary = (ground_truth > 127).astype(np.uint8)
    tp = int(((predicted_binary == 1) & (truth_binary == 1)).sum())
    fp = int(((predicted_binary == 1) & (truth_binary == 0)).sum())
    fn = int(((predicted_binary == 0) & (truth_binary == 1)).sum())
    dice = (2.0 * tp) / max(1.0, (2.0 * tp + fp + fn))
    iou = tp / max(1.0, (tp + fp + fn))

    predicted_mask_path = artifact_dir / "sample_infer_mask.png"
    Image.fromarray(predicted_mask, mode="L").save(predicted_mask_path)
    result = {
        "image": repo_relative(sample.image_path),
        "mask": repo_relative(sample.mask_path),
        "outputMask": repo_relative(predicted_mask_path),
        "predictedPositivePixels": int(predicted_binary.sum()),
        "groundTruthPositivePixels": int(truth_binary.sum()),
        "diceAgainstGroundTruth": float(dice),
        "iouAgainstGroundTruth": float(iou),
        "onnxProvider": "CPUExecutionProvider",
    }
    write_json(artifact_dir / "single_infer_result.json", result)
    return result


def write_supporting_artifacts(
    artifact_dir: Path,
    config: RunConfig,
    scan_report: dict[str, Any],
    train_records: list[PairRecord],
    val_records: list[PairRecord],
    history: list[dict[str, Any]],
    best_epoch: int,
    best_metrics: dict[str, float],
    sample_inference: dict[str, Any],
) -> None:
    preprocess_payload = {
        "version": "1.0",
        "shared": {
            "imageColorMode": "grayscale",
            "imageSize": [config.input_size[0], config.input_size[1]],
            "keepAspectRatio": False,
            "interpolation": "bilinear",
            "normalize": {"mode": "minmax_0_1", "mean": [0.5], "std": [0.5]},
            "valueClip": {"min": 0, "max": 255},
        },
        "segmentation": {
            "maskColorMode": "grayscale",
            "maskInterpolation": "nearest",
            "foregroundClassCode": "caries_lesion",
        },
    }
    postprocess_payload = {
        "version": "1.0",
        "segmentation": {
            "maskThreshold": config.mask_threshold,
            "minRegionAreaPx": config.min_region_area_px,
            "connectedComponents": True,
            "bboxClampToImage": True,
            "polygonSimplifyEpsilonPx": 2.0,
            "exportMaskPng": True,
            "exportOverlayPng": False,
            "exportHeatmapPng": False,
        },
    }
    write_json(artifact_dir / "class_map.json", DEFAULT_CLASS_MAP)
    write_json(artifact_dir / "runtime_class_map.json", DEFAULT_RUNTIME_CLASS_MAP)
    write_yaml(artifact_dir / "preprocess.yaml", preprocess_payload)
    write_yaml(artifact_dir / "postprocess.yaml", postprocess_payload)

    metrics_payload = {
        "train_count": len(train_records),
        "val_count": len(val_records),
        "input_size": [config.input_size[0], config.input_size[1]],
        "num_classes": 2,
        "best_val_dice": float(best_metrics["dice"]),
        "best_val_iou": float(best_metrics["iou"]),
        "export_success": True,
        "dataset_path": str(config.dataset_dir.resolve()),
        "raw_image_count": int(scan_report["rawImageCount"]),
        "mask_file_count": int(scan_report["maskFileCount"]),
        "paired_count": int(scan_report["pairedCount"]),
        "skipped_count": int(scan_report["skippedCount"]),
        "best_epoch": int(best_epoch),
        "epochs_requested": int(config.epochs),
        "epochs_completed": len(history),
        "history": history,
        "scan_skipped": scan_report["skipped"],
        "sample_inference": sample_inference,
    }
    write_json(artifact_dir / "metrics.json", metrics_payload)

    export_info_payload = {
        "modelCode": DEFAULT_MODEL_CODE,
        "checkpointFormat": "onnx",
        "trainingCheckpointFormat": "pth",
        "exportedAt": datetime.now(timezone.utc).isoformat(),
        "inputName": DEFAULT_INPUT_NAME,
        "outputName": DEFAULT_OUTPUT_NAME,
        "inputSize": [config.input_size[0], config.input_size[1]],
        "numClasses": 2,
        "bestModelPath": repo_relative(artifact_dir / "best_model.pth"),
        "onnxPath": repo_relative(artifact_dir / "model.onnx"),
        "bestModelSha256": sha256_file(artifact_dir / "best_model.pth"),
        "onnxSha256": sha256_file(artifact_dir / "model.onnx"),
    }
    write_json(artifact_dir / "export_info.json", export_info_payload)

    split_payload = {
        "seed": config.seed,
        "trainRatio": 1.0 - config.val_ratio,
        "valRatio": config.val_ratio,
        "train": [{"imageId": item.image_id, "imagePath": repo_relative(item.image_path), "maskPath": repo_relative(item.mask_path)} for item in train_records],
        "val": [{"imageId": item.image_id, "imagePath": repo_relative(item.image_path), "maskPath": repo_relative(item.mask_path)} for item in val_records],
        "skipped": scan_report["skipped"],
    }
    write_json(artifact_dir / "dataset_split.json", split_payload)

    template_path = BACKEND_ROOT / "training" / "scripts" / "sample_infer_segmentation_poc.py"
    shutil.copyfile(template_path, artifact_dir / "sample_infer.py")

    readme = "\n".join(
        [
            "# segmentation_v1 POC artifacts",
            "",
            f"- Model code: `{DEFAULT_MODEL_CODE}`",
            f"- Dataset source: `{repo_relative(config.dataset_dir)}`",
            f"- Input size: `{config.input_size[0]}x{config.input_size[1]}` grayscale",
            f"- Best validation dice: `{best_metrics['dice']:.4f}`",
            f"- Best validation IoU: `{best_metrics['iou']:.4f}`",
            "",
            "- `best_model.pth`: best PyTorch checkpoint payload",
            "- `model.onnx`: deployable ONNX segmentation model",
            "- `class_map.json`: minimal binary class map requested for this POC",
            "- `runtime_class_map.json`: optional richer class map for later runtime wiring",
            "- `preprocess.yaml` / `postprocess.yaml`: runtime-facing config",
            "- `metrics.json`: scan stats, split stats, and epoch history",
            "- `export_info.json`: export metadata and sha256 values",
            "- `sample_infer.py`: single-image ONNX inference script",
            "- `dataset_split.json`: reproducible train/val split manifest",
            "- `single_infer_result.json`: post-export validation result",
            "",
            "- Manifest update hint: use `model.onnx` and the sha256 values from `export_info.json`.",
            "- Limitation: this is a tiny proof-of-concept checkpoint, not a production release.",
        ]
    )
    (artifact_dir / "README.md").write_text(readme + "\n", encoding="utf-8")


def train_poc(config: RunConfig) -> dict[str, Any]:
    ensure_dependencies()
    artifact_dir = config.artifact_dir.resolve()
    artifact_dir.mkdir(parents=True, exist_ok=True)
    seed_everything(config.seed)

    scan_report = scan_dataset(config.dataset_dir.resolve())
    records: list[PairRecord] = scan_report["paired"]
    train_records, val_records = split_pairs(records, config.val_ratio, config.seed)
    device = resolve_device(config.device)

    train_loader = DataLoader(SegmentationPairDataset(train_records, config.input_size), batch_size=config.batch_size, shuffle=True, num_workers=config.num_workers, pin_memory=False)
    val_loader = DataLoader(SegmentationPairDataset(val_records, config.input_size), batch_size=config.batch_size, shuffle=False, num_workers=config.num_workers, pin_memory=False)

    model = build_model().to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=config.lr)
    dice_loss = DiceLoss(sigmoid=True)
    bce_loss = nn.BCEWithLogitsLoss()

    history: list[dict[str, Any]] = []
    best_epoch = 0
    best_metrics: dict[str, float] | None = None
    best_score = -1.0
    stale_epochs = 0

    for epoch in range(1, config.epochs + 1):
        model.train()
        running_loss = 0.0
        batch_count = 0
        for images, masks in train_loader:
            images = images.to(device)
            masks = masks.to(device)
            optimizer.zero_grad(set_to_none=True)
            logits = model(images)
            loss = combined_loss(logits, masks, dice_loss, bce_loss)
            loss.backward()
            optimizer.step()
            running_loss += float(loss.item())
            batch_count += 1

        train_loss = float(running_loss / max(1, batch_count))
        val_metrics = evaluate(model, val_loader, device, dice_loss, bce_loss)
        epoch_summary = {"epoch": epoch, "train": {"loss": train_loss}, "val": val_metrics}
        history.append(epoch_summary)

        if float(val_metrics["dice"]) > best_score:
            best_score = float(val_metrics["dice"])
            best_epoch = epoch
            best_metrics = val_metrics
            stale_epochs = 0
            save_best_checkpoint(model, artifact_dir / "best_model.pth", config, history, best_epoch, best_metrics)
        else:
            stale_epochs += 1
        if stale_epochs >= config.early_stopping_patience:
            break

    if best_metrics is None or best_epoch <= 0:
        raise RuntimeError("training did not produce a best checkpoint")

    checkpoint_payload = torch.load(artifact_dir / "best_model.pth", map_location="cpu")
    model.load_state_dict(checkpoint_payload["modelStateDict"])
    model.eval()
    export_onnx(model, config, artifact_dir / "model.onnx")
    sample_inference = run_onnx_validation(artifact_dir, val_records[0], config)
    write_supporting_artifacts(artifact_dir, config, scan_report, train_records, val_records, history, best_epoch, best_metrics, sample_inference)

    summary = {
        "dataset": {
            "datasetPath": scan_report["datasetPath"],
            "rawImageCount": scan_report["rawImageCount"],
            "maskFileCount": scan_report["maskFileCount"],
            "pairedCount": scan_report["pairedCount"],
            "skippedCount": scan_report["skippedCount"],
            "skipped": scan_report["skipped"],
        },
        "split": {
            "seed": config.seed,
            "trainCount": len(train_records),
            "valCount": len(val_records),
            "trainIds": [item.image_id for item in train_records],
            "valIds": [item.image_id for item in val_records],
        },
        "training": {
            "epochsRequested": config.epochs,
            "epochsCompleted": len(history),
            "bestEpoch": best_epoch,
            "bestValDice": float(best_metrics["dice"]),
            "bestValIou": float(best_metrics["iou"]),
            "history": history,
        },
        "export": {
            "success": True,
            "bestModelPath": str((artifact_dir / "best_model.pth").resolve()),
            "onnxPath": str((artifact_dir / "model.onnx").resolve()),
            "bestModelSha256": sha256_file(artifact_dir / "best_model.pth"),
            "onnxSha256": sha256_file(artifact_dir / "model.onnx"),
        },
        "sampleInference": sample_inference,
    }
    write_json(artifact_dir / "run_summary.json", summary)
    return summary
