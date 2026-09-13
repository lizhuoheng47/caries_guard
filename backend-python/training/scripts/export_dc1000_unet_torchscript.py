from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import torch
from torch import nn


REPO_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_CHECKPOINT = (
    REPO_ROOT
    / "artifacts"
    / "training"
    / "dc1000_segmentation"
    / "dc1000_unet_v1"
    / "weights"
    / "best.pt"
)
DEFAULT_OUTPUT = (
    REPO_ROOT
    / "backend-python"
    / "assets"
    / "models"
    / "checkpoints"
    / "segmentation_v1"
    / "model.pt"
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Export the validated DC1000 MONAI U-Net tile model to TorchScript."
    )
    parser.add_argument("--checkpoint", default=str(DEFAULT_CHECKPOINT))
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--batch-size", type=int, default=1)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--max-absolute-error", type=float, default=1e-4)
    return parser


def create_model() -> nn.Module:
    try:
        from monai.networks.nets import UNet
    except ImportError as exc:
        raise RuntimeError("MONAI is required. Use backend-python/.venv.") from exc
    return UNet(
        spatial_dims=2,
        in_channels=1,
        out_channels=1,
        channels=(16, 32, 64, 128, 256),
        strides=(2, 2, 2, 2),
        num_res_units=2,
        norm="BATCH",
    )


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    args = build_parser().parse_args()
    checkpoint_path = Path(args.checkpoint).resolve()
    output_path = Path(args.output).resolve()
    if not checkpoint_path.is_file():
        raise FileNotFoundError(f"checkpoint does not exist: {checkpoint_path}")
    if args.batch_size <= 0:
        raise ValueError("--batch-size must be positive")

    payload = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
    if payload.get("smokeRun"):
        raise RuntimeError("refusing to export a smoke-run checkpoint")
    if payload.get("modelType") != "BINARY_SEGMENTATION":
        raise RuntimeError(f"unexpected modelType: {payload.get('modelType')}")

    patch_size = int(payload.get("patchSize") or 512)
    model = create_model().cpu().eval()
    model.load_state_dict(payload["modelStateDict"])

    torch.manual_seed(args.seed)
    example = torch.randn(args.batch_size, 1, patch_size, patch_size, dtype=torch.float32)
    with torch.inference_mode():
        expected = model(example)
        scripted = torch.jit.trace(model, example, strict=True)
        scripted = scripted.eval()
        actual = scripted(example)

    maximum_error = float(torch.max(torch.abs(expected - actual)).item())
    mean_error = float(torch.mean(torch.abs(expected - actual)).item())
    probability_error = float(
        torch.max(torch.abs(torch.sigmoid(expected) - torch.sigmoid(actual))).item()
    )
    mask_difference_pixels = int(
        ((torch.sigmoid(expected) >= 0.70) != (torch.sigmoid(actual) >= 0.70)).sum().item()
    )
    if maximum_error > args.max_absolute_error:
        raise RuntimeError(
            f"TorchScript parity failed: max absolute error {maximum_error} "
            f"> allowed {args.max_absolute_error}"
        )

    output_path.parent.mkdir(parents=True, exist_ok=True)
    torch.jit.save(scripted, str(output_path))
    loaded = torch.jit.load(str(output_path), map_location="cpu").eval()
    with torch.inference_mode():
        reloaded = loaded(example)
    reload_maximum_error = float(torch.max(torch.abs(expected - reloaded)).item())
    reload_probability_error = float(
        torch.max(torch.abs(torch.sigmoid(expected) - torch.sigmoid(reloaded))).item()
    )
    reload_mask_difference_pixels = int(
        ((torch.sigmoid(expected) >= 0.70) != (torch.sigmoid(reloaded) >= 0.70)).sum().item()
    )
    if reload_maximum_error > args.max_absolute_error:
        output_path.unlink(missing_ok=True)
        raise RuntimeError(
            f"saved TorchScript parity failed: max absolute error {reload_maximum_error} "
            f"> allowed {args.max_absolute_error}"
        )

    report = {
        "modelCode": "dc1000-unet-v1",
        "modelType": "BINARY_SEGMENTATION",
        "format": "torchscript",
        "sourceCheckpoint": str(checkpoint_path),
        "sourceCheckpointEpoch": int(payload["epoch"]),
        "outputPath": str(output_path),
        "outputSha256": sha256_file(output_path),
        "outputBytes": output_path.stat().st_size,
        "inputShape": [args.batch_size, 1, patch_size, patch_size],
        "outputShape": list(reloaded.shape),
        "outputType": "logits",
        "parity": {
            "maxAbsoluteErrorBeforeSave": maximum_error,
            "meanAbsoluteErrorBeforeSave": mean_error,
            "maxProbabilityErrorBeforeSave": probability_error,
            "maskDifferencePixelsBeforeSaveAtThreshold0.70": mask_difference_pixels,
            "maxAbsoluteErrorAfterReload": reload_maximum_error,
            "maxProbabilityErrorAfterReload": reload_probability_error,
            "maskDifferencePixelsAfterReloadAtThreshold0.70": reload_mask_difference_pixels,
            "allowedMaxAbsoluteError": args.max_absolute_error,
            "passed": True,
        },
        "selectedMaskThreshold": 0.70,
        "exportedAtUtc": datetime.now(timezone.utc).isoformat(),
    }
    report_path = output_path.parent / "export_report.json"
    write_json(report_path, report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
