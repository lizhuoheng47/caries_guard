from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
DEFAULT_DATASET = REPO_ROOT / "data" / "processed" / "dentex" / "yolo" / "dataset.yaml"
DEFAULT_PROJECT = REPO_ROOT / "artifacts" / "training" / "dentex_detection"
EXPECTED_CLASSES = {
    0: "Impacted",
    1: "Caries",
    2: "Periapical_Lesion",
    3: "Deep_Caries",
}


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Train a DENTEX abnormal-tooth disease detector with Ultralytics YOLO."
    )
    parser.add_argument("--data", default=str(DEFAULT_DATASET), help="YOLO dataset YAML")
    parser.add_argument("--model", default="yolov8n.pt", help="Pretrained Ultralytics checkpoint")
    parser.add_argument("--project", default=str(DEFAULT_PROJECT), help="Training output directory")
    parser.add_argument("--name", default="dentex_yolov8n_v1", help="Ultralytics run name")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--imgsz", type=int, default=960)
    parser.add_argument("--batch", type=int, default=4)
    parser.add_argument("--workers", type=int, default=2)
    parser.add_argument("--device", default="0")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--patience", type=int, default=30)
    parser.add_argument(
        "--smoke",
        action="store_true",
        help="Run a short 2-epoch, 640px validation of the complete training path.",
    )
    parser.add_argument(
        "--skip-export",
        action="store_true",
        help="Do not export the best checkpoint to ONNX after validation.",
    )
    return parser


def require_runtime() -> tuple[Any, Any]:
    try:
        import torch
        from ultralytics import YOLO
    except ImportError as exc:
        raise RuntimeError(
            "PyTorch and Ultralytics are required. Use the project's Python 3.11 virtual environment."
        ) from exc
    return torch, YOLO


def ensure_environment(torch: Any, dataset_path: Path, device: str) -> None:
    if not dataset_path.is_file():
        raise FileNotFoundError(f"DENTEX dataset YAML does not exist: {dataset_path}")
    if str(device).lower() not in {"cpu", "mps"} and not torch.cuda.is_available():
        raise RuntimeError("CUDA training was requested, but torch.cuda.is_available() is False")


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def metric_value(metrics: Any, attribute: str) -> float | None:
    value = getattr(metrics, attribute, None)
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def main() -> int:
    args = build_parser().parse_args()
    torch, yolo_class = require_runtime()

    dataset_path = Path(args.data).resolve()
    project_path = Path(args.project).resolve()
    ensure_environment(torch, dataset_path, str(args.device))

    epochs = 2 if args.smoke else args.epochs
    image_size = 640 if args.smoke else args.imgsz
    batch_size = 2 if args.smoke else args.batch
    run_name = f"{args.name}_smoke" if args.smoke else args.name

    model = yolo_class(args.model)
    model.train(
        data=str(dataset_path),
        project=str(project_path),
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
        close_mosaic=min(10, max(0, epochs - 1)),
        cache=False,
        plots=True,
        exist_ok=False,
    )

    save_dir = Path(model.trainer.save_dir).resolve()
    best_checkpoint = save_dir / "weights" / "best.pt"
    if not best_checkpoint.is_file():
        raise RuntimeError(f"Ultralytics did not create the expected best checkpoint: {best_checkpoint}")

    best_model = yolo_class(str(best_checkpoint))
    metrics = best_model.val(
        data=str(dataset_path),
        imgsz=image_size,
        batch=batch_size,
        workers=args.workers,
        device=args.device,
        plots=True,
        project=str(project_path),
        name=f"{run_name}_validation",
        exist_ok=True,
    )

    exported_onnx: str | None = None
    if not args.skip_export:
        exported_onnx = str(
            Path(
                best_model.export(
                    format="onnx",
                    imgsz=image_size,
                    batch=1,
                    dynamic=False,
                    simplify=True,
                    nms=True,
                    device=args.device,
                )
            ).resolve()
        )

    metadata = {
        "modelCode": "dentex-disease-detect-yolov8n-v1",
        "modelType": "DETECTION",
        "task": "abnormal-tooth-disease-detection",
        "dataset": "DENTEX quadrant-enumeration-disease",
        "datasetYaml": str(dataset_path),
        "createdAtUtc": datetime.now(timezone.utc).isoformat(),
        "smokeRun": bool(args.smoke),
        "seed": args.seed,
        "epochs": epochs,
        "imageSize": image_size,
        "batchSize": batch_size,
        "device": str(args.device),
        "gpu": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
        "torchVersion": str(torch.__version__),
        "labelOrder": [EXPECTED_CLASSES[index] for index in sorted(EXPECTED_CLASSES)],
        "bestCheckpoint": str(best_checkpoint),
        "exportedOnnx": exported_onnx,
        "metrics": {
            "map50": metric_value(metrics.box, "map50"),
            "map50To95": metric_value(metrics.box, "map"),
            "precisionMean": metric_value(metrics.box, "mp"),
            "recallMean": metric_value(metrics.box, "mr"),
        },
        "limitations": [
            "This model detects and classifies abnormal teeth; it does not produce pixel-level caries lesion masks.",
            "The generated image split is image-level because DENTEX does not expose patient identifiers.",
            "The official validation set has no public ground-truth labels and is not used for metric calculation.",
        ],
    }
    write_json(save_dir / "cariesguard_model_metadata.json", metadata)
    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
