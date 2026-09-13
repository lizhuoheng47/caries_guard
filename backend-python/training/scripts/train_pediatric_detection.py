from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
DEFAULT_DATASET = (
    REPO_ROOT / "data" / "processed" / "children_dental" / "yolo_pediatric_2class" / "dataset.yaml"
)
DEFAULT_MODEL = (
    REPO_ROOT
    / "artifacts"
    / "training"
    / "dentex_detection"
    / "dentex_yolov8n_v1"
    / "weights"
    / "best.pt"
)
DEFAULT_PROJECT = REPO_ROOT / "artifacts" / "training" / "pediatric_detection"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Fine-tune a pediatric Caries/Periapical YOLO detector without test leakage."
    )
    parser.add_argument("--data", default=str(DEFAULT_DATASET))
    parser.add_argument("--model", default=str(DEFAULT_MODEL))
    parser.add_argument("--project", default=str(DEFAULT_PROJECT))
    parser.add_argument("--name", default="pediatric_yolov8n_2class_v1")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--imgsz", type=int, default=960)
    parser.add_argument("--batch", type=int, default=4)
    parser.add_argument("--workers", type=int, default=2)
    parser.add_argument("--device", default="0")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--patience", type=int, default=20)
    parser.add_argument("--freeze", type=int, default=5)
    parser.add_argument("--smoke", action="store_true")
    return parser


def metric_value(metrics: Any, attribute: str) -> float | None:
    try:
        value = getattr(metrics, attribute)
        return float(value)
    except (AttributeError, TypeError, ValueError):
        return None


def metric_summary(result: Any) -> dict[str, float | None]:
    return {
        "map50": metric_value(result.box, "map50"),
        "map50To95": metric_value(result.box, "map"),
        "precisionMean": metric_value(result.box, "mp"),
        "recallMean": metric_value(result.box, "mr"),
    }


def main() -> int:
    args = build_parser().parse_args()
    try:
        import torch
        from ultralytics import YOLO
    except ImportError as exc:
        raise RuntimeError("Run this script with the existing Python 3.11 training environment") from exc

    dataset = Path(args.data).resolve()
    initial_model = Path(args.model).resolve()
    project = Path(args.project).resolve()
    if not dataset.is_file():
        raise FileNotFoundError(f"Dataset YAML is missing: {dataset}")
    if not initial_model.is_file():
        raise FileNotFoundError(f"Initial DENTEX checkpoint is missing: {initial_model}")
    if str(args.device).lower() not in {"cpu", "mps"} and not torch.cuda.is_available():
        raise RuntimeError("CUDA training was requested but is unavailable")

    epochs = 2 if args.smoke else args.epochs
    image_size = 640 if args.smoke else args.imgsz
    batch_size = 2 if args.smoke else args.batch
    run_name = f"{args.name}_smoke" if args.smoke else args.name

    model = YOLO(str(initial_model))
    model.train(
        data=str(dataset),
        project=str(project),
        name=run_name,
        epochs=epochs,
        imgsz=image_size,
        batch=batch_size,
        workers=args.workers,
        device=args.device,
        seed=args.seed,
        deterministic=True,
        amp=True,
        patience=args.patience,
        freeze=args.freeze,
        optimizer="AdamW",
        lr0=0.001,
        lrf=0.01,
        close_mosaic=min(10, max(0, epochs - 1)),
        cache=False,
        plots=True,
        exist_ok=False,
    )

    save_dir = Path(model.trainer.save_dir).resolve()
    best_checkpoint = save_dir / "weights" / "best.pt"
    if not best_checkpoint.is_file():
        raise RuntimeError(f"Best checkpoint was not produced: {best_checkpoint}")
    best_model = YOLO(str(best_checkpoint))
    validation = best_model.val(
        data=str(dataset),
        split="val",
        imgsz=image_size,
        batch=batch_size,
        workers=args.workers,
        device=args.device,
        plots=True,
        project=str(project),
        name=f"{run_name}_validation",
        exist_ok=True,
    )
    external_test = best_model.val(
        data=str(dataset),
        split="test",
        imgsz=image_size,
        batch=batch_size,
        workers=args.workers,
        device=args.device,
        plots=True,
        project=str(project),
        name=f"{run_name}_test",
        exist_ok=True,
    )

    metadata = {
        "modelCode": "pediatric-disease-detect-yolov8n-2class-v1",
        "modelType": "DETECTION",
        "task": "pediatric-caries-periapical-detection",
        "createdAtUtc": datetime.now(timezone.utc).isoformat(),
        "datasetYaml": str(dataset),
        "initialCheckpoint": str(initial_model),
        "bestCheckpoint": str(best_checkpoint),
        "labelOrder": ["Caries", "Periapical_Lesion"],
        "epochsRequested": epochs,
        "imageSize": image_size,
        "batchSize": batch_size,
        "freezeLayers": args.freeze,
        "seed": args.seed,
        "gpu": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
        "validationMetrics": metric_summary(validation),
        "officialTestMetrics": metric_summary(external_test),
        "limitations": [
            "The model supports only pediatric Caries and Periapical_Lesion detection.",
            "The public pediatric dataset is small and must not be treated as clinical validation.",
            "The official Test split is excluded from training and early stopping.",
        ],
    }
    metadata_path = save_dir / "cariesguard_model_metadata.json"
    metadata_path.write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
