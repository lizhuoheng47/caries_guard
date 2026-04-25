from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from training.monai_segmentation_poc import DEFAULT_INPUT_SIZE, RunConfig, train_poc  # noqa: E402


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Run the CariesGuard segmentation POC training/export pipeline.")
    parser.add_argument("--dataset-dir", default=str(REPO_ROOT / "data" / "traindata"))
    parser.add_argument("--artifact-dir", default=str(REPO_ROOT / "artifacts" / "segmentation_v1"))
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--batch-size", type=int, default=4)
    parser.add_argument("--lr", type=float, default=1e-4)
    parser.add_argument("--early-stopping-patience", type=int, default=5)
    parser.add_argument("--input-size", default=f"{DEFAULT_INPUT_SIZE[0]}x{DEFAULT_INPUT_SIZE[1]}")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--num-workers", type=int, default=0)
    parser.add_argument("--mask-threshold", type=float, default=0.5)
    parser.add_argument("--min-region-area-px", type=int, default=16)
    return parser


def parse_input_size(raw_value: str) -> tuple[int, int]:
    value = str(raw_value).strip().lower()
    if "x" in value:
        width, height = value.split("x", maxsplit=1)
        return int(width), int(height)
    size = int(value)
    return size, size


def main() -> int:
    args = build_parser().parse_args()
    config = RunConfig(
        dataset_dir=Path(args.dataset_dir).resolve(),
        artifact_dir=Path(args.artifact_dir).resolve(),
        epochs=args.epochs,
        batch_size=args.batch_size,
        lr=args.lr,
        early_stopping_patience=args.early_stopping_patience,
        input_size=parse_input_size(args.input_size),
        seed=args.seed,
        device=args.device,
        num_workers=args.num_workers,
        mask_threshold=args.mask_threshold,
        min_region_area_px=args.min_region_area_px,
    )
    summary = train_poc(config)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
