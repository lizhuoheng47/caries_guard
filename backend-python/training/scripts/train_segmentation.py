from __future__ import annotations

import argparse
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from training.common import (  # noqa: E402
    build_confusion_matrix_payload,
    configure_logger,
    load_dataset_records,
    load_task_defaults,
    load_yaml,
    parse_input_size,
    prepare_output_dirs,
    resolve_project_path,
    save_training_args,
    write_json,
)

DEFAULTS = load_task_defaults("segmentation")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Train caries segmentation model")
    parser.add_argument(
        "--dataset-manifest",
        default=str(DEFAULTS["train_manifest_path"]),
        help="Path to the training JSONL manifest",
    )
    parser.add_argument(
        "--val-manifest",
        default=str(DEFAULTS["val_manifest_path"]),
        help="Path to the validation JSONL manifest",
    )
    parser.add_argument(
        "--class-map",
        default=str(DEFAULTS["class_map_path"]),
        help="Path to class_map.json",
    )
    parser.add_argument(
        "--preprocess-config",
        default=str(DEFAULTS["preprocess_path"]),
        help="Path to shared preprocess.yaml",
    )
    parser.add_argument(
        "--output-dir",
        default=str(PROJECT_ROOT / "training" / "outputs" / "runs" / "seg_v1"),
        help="Directory for checkpoints, metrics, and logs",
    )
    parser.add_argument("--epochs", type=int, default=50, help="Number of training epochs")
    parser.add_argument("--batch-size", type=int, default=4, help="Mini-batch size")
    parser.add_argument("--lr", type=float, default=1e-3, help="Learning rate")
    parser.add_argument(
        "--input-size",
        default=f"{DEFAULTS['expected_input_size'][0]}x{DEFAULTS['expected_input_size'][1]}",
        help="Input size as N or WIDTHxHEIGHT",
    )
    parser.add_argument("--device", default="auto", help="Training device, e.g. auto/cpu/cuda")
    parser.add_argument("--num-workers", type=int, default=0, help="DataLoader worker count")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    return parser


def load_runtime():
    try:
        from training import torch_runtime
    except ImportError as exc:  # pragma: no cover - depends on local environment
        raise RuntimeError(
            "PyTorch is required for training. Install torch in backend-python/.venv before running."
        ) from exc
    return torch_runtime


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    args.input_size = parse_input_size(args.input_size)

    output_dirs = prepare_output_dirs(args.output_dir)
    logger = configure_logger("training.segmentation.train", output_dirs["logs"] / "train.log")
    save_training_args(output_dirs["metrics"], args)

    train_records, _, label_maps = load_dataset_records(
        manifest_path=args.dataset_manifest,
        class_map_path=args.class_map,
        require_mask=True,
    )
    val_records, _, _ = load_dataset_records(
        manifest_path=args.val_manifest,
        class_map_path=args.class_map,
        require_mask=True,
    )

    preprocess_config = load_yaml(args.preprocess_config)
    shared_config = preprocess_config.get("shared", {})
    normalize = shared_config.get("normalize", {})
    mean = float((normalize.get("mean") or [0.5])[0])
    std = float((normalize.get("std") or [0.5])[0])

    logger.info(
        "starting segmentation training train_samples=%s val_samples=%s input_size=%s device=%s output_dir=%s",
        len(train_records),
        len(val_records),
        args.input_size,
        args.device,
        resolve_project_path(args.output_dir),
    )

    runtime = load_runtime()
    result = runtime.train_segmentation(
        train_records=train_records,
        val_records=val_records,
        image_size=args.input_size,
        epochs=args.epochs,
        batch_size=args.batch_size,
        lr=args.lr,
        device_name=args.device,
        checkpoint_dir=output_dirs["checkpoints"],
        mean=mean,
        std=std,
        ignore_index=label_maps.ignore_index,
        num_workers=args.num_workers,
        seed=args.seed,
    )

    segmentation_labels = [
        label for label, _ in sorted(label_maps.segmentation.items(), key=lambda item: item[1])
    ]
    metrics_payload = {
        "task": "segmentation",
        "trainSamples": len(train_records),
        "valSamples": len(val_records),
        "bestEpoch": result["bestEpoch"],
        "bestCheckpoint": result["bestCheckpoint"],
        "latestCheckpoint": result["latestCheckpoint"],
        "bestVal": result["bestMetrics"],
        "history": result["history"],
    }
    write_json(output_dirs["metrics"] / "metrics.json", metrics_payload)
    write_json(
        output_dirs["metrics"] / "confusion_matrix.json",
        build_confusion_matrix_payload(result["bestConfusionMatrix"], segmentation_labels),
    )
    logger.info(
        "segmentation training finished best_epoch=%s best_checkpoint=%s",
        result["bestEpoch"],
        result["bestCheckpoint"],
    )


if __name__ == "__main__":
    main()
