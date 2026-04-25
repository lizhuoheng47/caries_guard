from __future__ import annotations

import random
from pathlib import Path
from typing import Any

import numpy as np
import torch
import torch.nn.functional as functional
from PIL import Image
from torch import nn
from torch.utils.data import DataLoader, Dataset

from training.common import DatasetRecord


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
    requested = device_name.strip().lower()
    if requested == "auto":
        return torch.device("cuda" if torch.cuda.is_available() else "cpu")
    if requested.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("CUDA device requested but torch.cuda.is_available() is false")
    return torch.device(device_name)


def _pil_to_normalized_array(
    image: Image.Image,
    image_size: tuple[int, int],
    mean: float,
    std: float,
) -> np.ndarray:
    resized = image.resize(image_size, Image.BILINEAR)
    array = np.asarray(resized, dtype=np.float32)
    array = np.clip(array, 0.0, 255.0) / 255.0
    denom = std if abs(std) > 1e-6 else 1.0
    array = (array - mean) / denom
    return array[None, ...]


def _mask_arrays(
    mask: Image.Image,
    image_size: tuple[int, int],
    ignore_index: int,
) -> tuple[np.ndarray, np.ndarray]:
    resized = mask.resize(image_size, Image.NEAREST)
    array = np.asarray(resized, dtype=np.uint8)
    valid = (array != ignore_index).astype(np.float32)
    binary = ((array > 0) & (array != ignore_index)).astype(np.float32)
    return binary[None, ...], valid[None, ...]


def _bbox_from_mask(mask_array: np.ndarray, padding: int) -> tuple[int, int, int, int] | None:
    ys, xs = np.where(mask_array > 0)
    if xs.size == 0 or ys.size == 0:
        return None
    x1 = max(0, int(xs.min()) - padding)
    y1 = max(0, int(ys.min()) - padding)
    x2 = int(xs.max()) + 1 + padding
    y2 = int(ys.max()) + 1 + padding
    return x1, y1, x2, y2


class SegmentationDataset(Dataset):
    def __init__(
        self,
        records: list[DatasetRecord],
        image_size: tuple[int, int],
        mean: float,
        std: float,
        ignore_index: int,
    ) -> None:
        self._records = records
        self._image_size = image_size
        self._mean = mean
        self._std = std
        self._ignore_index = ignore_index

    def __len__(self) -> int:
        return len(self._records)

    def __getitem__(self, index: int) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        record = self._records[index]
        image = Image.open(record.image_path).convert("L")
        mask = Image.open(record.mask_path).convert("L")

        image_tensor = torch.from_numpy(
            _pil_to_normalized_array(image, self._image_size, self._mean, self._std)
        ).float()
        mask_array, valid_array = _mask_arrays(mask, self._image_size, self._ignore_index)
        mask_tensor = torch.from_numpy(mask_array).float()
        valid_tensor = torch.from_numpy(valid_array).float()
        return image_tensor, mask_tensor, valid_tensor


class GradingDataset(Dataset):
    def __init__(
        self,
        records: list[DatasetRecord],
        image_size: tuple[int, int],
        mean: float,
        std: float,
        label_map: dict[str, int],
        crop_from_mask: bool,
        crop_padding_pixels: int,
        allow_whole_image_fallback: bool,
        negative_label_code: str = "C0",
    ) -> None:
        self._records = records
        self._image_size = image_size
        self._mean = mean
        self._std = std
        self._label_map = label_map
        self._crop_from_mask = crop_from_mask
        self._crop_padding_pixels = crop_padding_pixels
        self._allow_whole_image_fallback = allow_whole_image_fallback
        self._negative_label_code = negative_label_code

    def __len__(self) -> int:
        return len(self._records)

    def __getitem__(self, index: int) -> tuple[torch.Tensor, torch.Tensor]:
        record = self._records[index]
        image = Image.open(record.image_path).convert("L")

        if self._crop_from_mask:
            mask_array = np.asarray(Image.open(record.mask_path).convert("L"), dtype=np.uint8)
            crop_box = _bbox_from_mask(mask_array, self._crop_padding_pixels)
            if crop_box is not None:
                image = image.crop(crop_box)
            elif not (self._allow_whole_image_fallback or record.grade_label == self._negative_label_code):
                raise RuntimeError(
                    "grading sample has empty mask and fallback is disabled: "
                    f"{record.image_id} ({record.mask_path})"
                )

        image_tensor = torch.from_numpy(
            _pil_to_normalized_array(image, self._image_size, self._mean, self._std)
        ).float()
        label_tensor = torch.tensor(self._label_map[record.grade_label], dtype=torch.long)
        return image_tensor, label_tensor


class ConvBlock(nn.Module):
    def __init__(self, in_channels: int, out_channels: int) -> None:
        super().__init__()
        self.layers = nn.Sequential(
            nn.Conv2d(in_channels, out_channels, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(out_channels),
            nn.ReLU(inplace=True),
            nn.Conv2d(out_channels, out_channels, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(out_channels),
            nn.ReLU(inplace=True),
        )

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        return self.layers(inputs)


class DownBlock(nn.Module):
    def __init__(self, in_channels: int, out_channels: int) -> None:
        super().__init__()
        self.pool = nn.MaxPool2d(kernel_size=2, stride=2)
        self.block = ConvBlock(in_channels, out_channels)

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        return self.block(self.pool(inputs))


class UpBlock(nn.Module):
    def __init__(self, in_channels: int, skip_channels: int, out_channels: int) -> None:
        super().__init__()
        self.up = nn.ConvTranspose2d(in_channels, out_channels, kernel_size=2, stride=2)
        self.block = ConvBlock(out_channels + skip_channels, out_channels)

    def forward(self, inputs: torch.Tensor, skip: torch.Tensor) -> torch.Tensor:
        upsampled = self.up(inputs)
        diff_y = skip.size(2) - upsampled.size(2)
        diff_x = skip.size(3) - upsampled.size(3)
        if diff_x != 0 or diff_y != 0:
            upsampled = functional.pad(
                upsampled,
                [diff_x // 2, diff_x - diff_x // 2, diff_y // 2, diff_y - diff_y // 2],
            )
        merged = torch.cat([skip, upsampled], dim=1)
        return self.block(merged)


class LightUNet(nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.enc1 = ConvBlock(1, 16)
        self.enc2 = DownBlock(16, 32)
        self.enc3 = DownBlock(32, 64)
        self.bottleneck = DownBlock(64, 128)
        self.dec3 = UpBlock(128, 64, 64)
        self.dec2 = UpBlock(64, 32, 32)
        self.dec1 = UpBlock(32, 16, 16)
        self.head = nn.Conv2d(16, 1, kernel_size=1)

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        enc1 = self.enc1(inputs)
        enc2 = self.enc2(enc1)
        enc3 = self.enc3(enc2)
        bottleneck = self.bottleneck(enc3)
        dec3 = self.dec3(bottleneck, enc3)
        dec2 = self.dec2(dec3, enc2)
        dec1 = self.dec1(dec2, enc1)
        return self.head(dec1)


class GradingClassifier(nn.Module):
    def __init__(self, num_classes: int) -> None:
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv2d(1, 16, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(16),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            nn.Conv2d(16, 32, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(32),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(kernel_size=2, stride=2),
            nn.Conv2d(32, 64, kernel_size=3, padding=1, bias=False),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.AdaptiveAvgPool2d((1, 1)),
        )
        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Dropout(p=0.2),
            nn.Linear(64, num_classes),
        )

    def forward(self, inputs: torch.Tensor) -> torch.Tensor:
        features = self.features(inputs)
        return self.classifier(features)


def _segmentation_loss(
    logits: torch.Tensor,
    targets: torch.Tensor,
    valid_mask: torch.Tensor,
) -> torch.Tensor:
    loss_map = functional.binary_cross_entropy_with_logits(logits, targets, reduction="none")
    valid_sum = valid_mask.sum().clamp_min(1.0)
    bce = (loss_map * valid_mask).sum() / valid_sum

    probs = torch.sigmoid(logits) * valid_mask
    masked_targets = targets * valid_mask
    intersection = (probs * masked_targets).sum(dim=(1, 2, 3))
    union = probs.sum(dim=(1, 2, 3)) + masked_targets.sum(dim=(1, 2, 3))
    dice = (2.0 * intersection + 1e-6) / (union + 1e-6)
    return bce + (1.0 - dice.mean())


def _binary_confusion(
    logits: torch.Tensor,
    targets: torch.Tensor,
    valid_mask: torch.Tensor,
) -> np.ndarray:
    probs = torch.sigmoid(logits)
    preds = (probs >= 0.5).to(torch.int64)
    targets_int = targets.to(torch.int64)
    valid = valid_mask > 0.5

    true_positive = int(((preds == 1) & (targets_int == 1) & valid).sum().item())
    true_negative = int(((preds == 0) & (targets_int == 0) & valid).sum().item())
    false_positive = int(((preds == 1) & (targets_int == 0) & valid).sum().item())
    false_negative = int(((preds == 0) & (targets_int == 1) & valid).sum().item())
    return np.asarray(
        [
            [true_negative, false_positive],
            [false_negative, true_positive],
        ],
        dtype=np.int64,
    )


def _classification_confusion(
    truths: list[int],
    preds: list[int],
    num_classes: int,
) -> np.ndarray:
    matrix = np.zeros((num_classes, num_classes), dtype=np.int64)
    for truth, pred in zip(truths, preds):
        matrix[int(truth), int(pred)] += 1
    return matrix


def _segmentation_metrics(confusion: np.ndarray, loss_value: float) -> dict[str, float]:
    tn, fp = confusion[0]
    fn, tp = confusion[1]
    total = max(1, int(confusion.sum()))
    dice = (2.0 * tp) / max(1.0, (2.0 * tp + fp + fn))
    iou = tp / max(1.0, (tp + fp + fn))
    accuracy = (tp + tn) / total
    return {
        "loss": float(loss_value),
        "dice": float(dice),
        "iou": float(iou),
        "pixelAccuracy": float(accuracy),
    }


def _classification_metrics(confusion: np.ndarray, loss_value: float) -> dict[str, Any]:
    total = int(confusion.sum())
    accuracy = float(np.trace(confusion) / max(1, total))
    per_class: dict[str, dict[str, float]] = {}
    f1_scores: list[float] = []
    for index in range(confusion.shape[0]):
        tp = int(confusion[index, index])
        fp = int(confusion[:, index].sum() - tp)
        fn = int(confusion[index, :].sum() - tp)
        precision = tp / max(1.0, tp + fp)
        recall = tp / max(1.0, tp + fn)
        f1 = (2.0 * precision * recall) / max(1e-6, precision + recall)
        per_class[str(index)] = {
            "precision": float(precision),
            "recall": float(recall),
            "f1": float(f1),
        }
        f1_scores.append(float(f1))
    macro_f1 = float(sum(f1_scores) / max(1, len(f1_scores)))
    return {
        "loss": float(loss_value),
        "accuracy": accuracy,
        "macroF1": macro_f1,
        "perClass": per_class,
    }


def _save_checkpoint(
    checkpoint_path: Path,
    task_name: str,
    model: nn.Module,
    optimizer: torch.optim.Optimizer | None,
    epoch: int,
    input_size: tuple[int, int],
    metrics: dict[str, Any],
    extra: dict[str, Any] | None = None,
) -> None:
    payload = {
        "taskName": task_name,
        "epoch": epoch,
        "inputSize": list(input_size),
        "modelStateDict": model.state_dict(),
        "optimizerStateDict": optimizer.state_dict() if optimizer is not None else None,
        "metrics": metrics,
    }
    if extra:
        payload.update(extra)
    torch.save(payload, checkpoint_path)


def create_segmentation_model() -> LightUNet:
    return LightUNet()


def create_grading_model(num_classes: int) -> GradingClassifier:
    return GradingClassifier(num_classes=num_classes)


def make_segmentation_loader(
    records: list[DatasetRecord],
    image_size: tuple[int, int],
    batch_size: int,
    num_workers: int,
    mean: float,
    std: float,
    ignore_index: int,
    shuffle: bool,
) -> DataLoader:
    dataset = SegmentationDataset(
        records=records,
        image_size=image_size,
        mean=mean,
        std=std,
        ignore_index=ignore_index,
    )
    return DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=shuffle,
        num_workers=num_workers,
        pin_memory=False,
    )


def make_grading_loader(
    records: list[DatasetRecord],
    image_size: tuple[int, int],
    batch_size: int,
    num_workers: int,
    mean: float,
    std: float,
    label_map: dict[str, int],
    crop_from_mask: bool,
    crop_padding_pixels: int,
    allow_whole_image_fallback: bool,
    shuffle: bool,
) -> DataLoader:
    dataset = GradingDataset(
        records=records,
        image_size=image_size,
        mean=mean,
        std=std,
        label_map=label_map,
        crop_from_mask=crop_from_mask,
        crop_padding_pixels=crop_padding_pixels,
        allow_whole_image_fallback=allow_whole_image_fallback,
    )
    return DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=shuffle,
        num_workers=num_workers,
        pin_memory=False,
    )


def train_segmentation(
    train_records: list[DatasetRecord],
    val_records: list[DatasetRecord],
    image_size: tuple[int, int],
    epochs: int,
    batch_size: int,
    lr: float,
    device_name: str,
    checkpoint_dir: Path,
    mean: float,
    std: float,
    ignore_index: int,
    num_workers: int,
    seed: int,
) -> dict[str, Any]:
    seed_everything(seed)
    device = resolve_device(device_name)
    model = create_segmentation_model().to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr)

    train_loader = make_segmentation_loader(
        records=train_records,
        image_size=image_size,
        batch_size=batch_size,
        num_workers=num_workers,
        mean=mean,
        std=std,
        ignore_index=ignore_index,
        shuffle=True,
    )
    val_loader = make_segmentation_loader(
        records=val_records,
        image_size=image_size,
        batch_size=batch_size,
        num_workers=num_workers,
        mean=mean,
        std=std,
        ignore_index=ignore_index,
        shuffle=False,
    )

    history: list[dict[str, Any]] = []
    best_epoch = 0
    best_metrics: dict[str, Any] | None = None
    best_confusion = np.zeros((2, 2), dtype=np.int64)
    best_score = -1.0

    for epoch in range(1, epochs + 1):
        model.train()
        running_loss = 0.0
        sample_count = 0

        for images, masks, valid_masks in train_loader:
            images = images.to(device)
            masks = masks.to(device)
            valid_masks = valid_masks.to(device)

            optimizer.zero_grad(set_to_none=True)
            logits = model(images)
            loss = _segmentation_loss(logits, masks, valid_masks)
            loss.backward()
            optimizer.step()

            batch_size_value = int(images.shape[0])
            running_loss += float(loss.item()) * batch_size_value
            sample_count += batch_size_value

        train_loss = running_loss / max(1, sample_count)
        val_metrics, val_confusion = evaluate_segmentation_model(
            model=model,
            data_loader=val_loader,
            device=device,
        )

        epoch_summary = {
            "epoch": epoch,
            "train": {"loss": float(train_loss)},
            "val": val_metrics,
        }
        history.append(epoch_summary)

        latest_path = checkpoint_dir / "latest.pt"
        _save_checkpoint(
            checkpoint_path=latest_path,
            task_name="segmentation",
            model=model,
            optimizer=optimizer,
            epoch=epoch,
            input_size=image_size,
            metrics=epoch_summary,
        )

        current_score = float(val_metrics["dice"])
        if current_score > best_score:
            best_score = current_score
            best_epoch = epoch
            best_metrics = val_metrics
            best_confusion = val_confusion.copy()
            best_path = checkpoint_dir / "best.pt"
            _save_checkpoint(
                checkpoint_path=best_path,
                task_name="segmentation",
                model=model,
                optimizer=optimizer,
                epoch=epoch,
                input_size=image_size,
                metrics=epoch_summary,
            )

    return {
        "history": history,
        "bestEpoch": best_epoch,
        "bestMetrics": best_metrics or {},
        "bestConfusionMatrix": best_confusion.tolist(),
        "latestCheckpoint": str(checkpoint_dir / "latest.pt"),
        "bestCheckpoint": str(checkpoint_dir / "best.pt"),
    }


@torch.no_grad()
def evaluate_segmentation_model(
    model: nn.Module,
    data_loader: DataLoader,
    device: torch.device,
) -> tuple[dict[str, Any], np.ndarray]:
    model.eval()
    running_loss = 0.0
    sample_count = 0
    confusion = np.zeros((2, 2), dtype=np.int64)

    for images, masks, valid_masks in data_loader:
        images = images.to(device)
        masks = masks.to(device)
        valid_masks = valid_masks.to(device)

        logits = model(images)
        loss = _segmentation_loss(logits, masks, valid_masks)
        batch_size_value = int(images.shape[0])
        running_loss += float(loss.item()) * batch_size_value
        sample_count += batch_size_value
        confusion += _binary_confusion(logits, masks, valid_masks)

    metrics = _segmentation_metrics(confusion, running_loss / max(1, sample_count))
    return metrics, confusion


def load_segmentation_checkpoint(
    checkpoint_path: str | Path,
    device_name: str,
) -> tuple[nn.Module, torch.device, dict[str, Any]]:
    device = resolve_device(device_name)
    payload = torch.load(Path(checkpoint_path), map_location=device)
    if payload.get("taskName") != "segmentation":
        raise RuntimeError(f"checkpoint is not a segmentation checkpoint: {checkpoint_path}")
    model = create_segmentation_model().to(device)
    model.load_state_dict(payload["modelStateDict"])
    return model, device, payload


def train_grading(
    train_records: list[DatasetRecord],
    val_records: list[DatasetRecord],
    image_size: tuple[int, int],
    epochs: int,
    batch_size: int,
    lr: float,
    device_name: str,
    checkpoint_dir: Path,
    mean: float,
    std: float,
    label_map: dict[str, int],
    crop_from_mask: bool,
    crop_padding_pixels: int,
    allow_whole_image_fallback: bool,
    num_workers: int,
    seed: int,
) -> dict[str, Any]:
    seed_everything(seed)
    device = resolve_device(device_name)
    model = create_grading_model(num_classes=len(label_map)).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr)
    criterion = nn.CrossEntropyLoss()

    train_loader = make_grading_loader(
        records=train_records,
        image_size=image_size,
        batch_size=batch_size,
        num_workers=num_workers,
        mean=mean,
        std=std,
        label_map=label_map,
        crop_from_mask=crop_from_mask,
        crop_padding_pixels=crop_padding_pixels,
        allow_whole_image_fallback=allow_whole_image_fallback,
        shuffle=True,
    )
    val_loader = make_grading_loader(
        records=val_records,
        image_size=image_size,
        batch_size=batch_size,
        num_workers=num_workers,
        mean=mean,
        std=std,
        label_map=label_map,
        crop_from_mask=crop_from_mask,
        crop_padding_pixels=crop_padding_pixels,
        allow_whole_image_fallback=allow_whole_image_fallback,
        shuffle=False,
    )

    history: list[dict[str, Any]] = []
    best_epoch = 0
    best_metrics: dict[str, Any] | None = None
    best_confusion = np.zeros((len(label_map), len(label_map)), dtype=np.int64)
    best_score = -1.0
    label_order = [label for label, _ in sorted(label_map.items(), key=lambda item: item[1])]

    for epoch in range(1, epochs + 1):
        model.train()
        running_loss = 0.0
        sample_count = 0

        for images, labels in train_loader:
            images = images.to(device)
            labels = labels.to(device)

            optimizer.zero_grad(set_to_none=True)
            logits = model(images)
            loss = criterion(logits, labels)
            loss.backward()
            optimizer.step()

            batch_size_value = int(images.shape[0])
            running_loss += float(loss.item()) * batch_size_value
            sample_count += batch_size_value

        train_loss = running_loss / max(1, sample_count)
        val_metrics, val_confusion = evaluate_grading_model(
            model=model,
            data_loader=val_loader,
            device=device,
            criterion=criterion,
        )

        epoch_summary = {
            "epoch": epoch,
            "train": {"loss": float(train_loss)},
            "val": val_metrics,
        }
        history.append(epoch_summary)

        latest_path = checkpoint_dir / "latest.pt"
        _save_checkpoint(
            checkpoint_path=latest_path,
            task_name="grading",
            model=model,
            optimizer=optimizer,
            epoch=epoch,
            input_size=image_size,
            metrics=epoch_summary,
            extra={"labelOrder": label_order, "numClasses": len(label_map)},
        )

        current_score = float(val_metrics["macroF1"])
        if current_score > best_score:
            best_score = current_score
            best_epoch = epoch
            best_metrics = val_metrics
            best_confusion = val_confusion.copy()
            best_path = checkpoint_dir / "best.pt"
            _save_checkpoint(
                checkpoint_path=best_path,
                task_name="grading",
                model=model,
                optimizer=optimizer,
                epoch=epoch,
                input_size=image_size,
                metrics=epoch_summary,
                extra={"labelOrder": label_order, "numClasses": len(label_map)},
            )

    return {
        "history": history,
        "bestEpoch": best_epoch,
        "bestMetrics": best_metrics or {},
        "bestConfusionMatrix": best_confusion.tolist(),
        "labelOrder": label_order,
        "latestCheckpoint": str(checkpoint_dir / "latest.pt"),
        "bestCheckpoint": str(checkpoint_dir / "best.pt"),
    }


@torch.no_grad()
def evaluate_grading_model(
    model: nn.Module,
    data_loader: DataLoader,
    device: torch.device,
    criterion: nn.Module,
) -> tuple[dict[str, Any], np.ndarray]:
    model.eval()
    truths: list[int] = []
    preds: list[int] = []
    running_loss = 0.0
    sample_count = 0

    for images, labels in data_loader:
        images = images.to(device)
        labels = labels.to(device)
        logits = model(images)
        loss = criterion(logits, labels)
        pred_labels = torch.argmax(logits, dim=1)

        batch_size_value = int(images.shape[0])
        running_loss += float(loss.item()) * batch_size_value
        sample_count += batch_size_value
        truths.extend(int(value) for value in labels.detach().cpu().tolist())
        preds.extend(int(value) for value in pred_labels.detach().cpu().tolist())

    confusion = _classification_confusion(truths, preds, num_classes=model.classifier[-1].out_features)
    metrics = _classification_metrics(confusion, running_loss / max(1, sample_count))
    return metrics, confusion


def load_grading_checkpoint(
    checkpoint_path: str | Path,
    device_name: str,
) -> tuple[nn.Module, torch.device, dict[str, Any]]:
    device = resolve_device(device_name)
    payload = torch.load(Path(checkpoint_path), map_location=device)
    if payload.get("taskName") != "grading":
        raise RuntimeError(f"checkpoint is not a grading checkpoint: {checkpoint_path}")
    num_classes = int(payload.get("numClasses") or len(payload.get("labelOrder") or []))
    if num_classes <= 0:
        raise RuntimeError(f"grading checkpoint missing numClasses metadata: {checkpoint_path}")
    model = create_grading_model(num_classes=num_classes).to(device)
    model.load_state_dict(payload["modelStateDict"])
    return model, device, payload
