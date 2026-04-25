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
    write_json,
)

DEFAULTS = load_task_defaults("grading")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Evaluate caries grading model")
    parser.add_argument(
        "--dataset-manifest",
        default=str(DEFAULTS["test_manifest_path"]),
        help="Path to the evaluation JSONL manifest",
    )
    parser.add_argument(
        "--class-map",
        default=str(DEFAULTS["class_map_path"]),
        help="Path to class_map.json",
    )
    parser.add_argument(
        "--output-dir",
        default=str(PROJECT_ROOT / "training" / "outputs" / "runs" / "grading_v1_eval"),
        help="Directory for evaluation metrics and logs",
    )
    parser.add_argument(
        "--preprocess-config",
        default=str(DEFAULTS["preprocess_path"]),
        help="Path to shared preprocess.yaml",
    )
    parser.add_argument(
        "--checkpoint",
        default=str(PROJECT_ROOT / "training" / "outputs" / "runs" / "grading_v1" / "checkpoints" / "best.pt"),
        help="Path to model checkpoint",
    )
    parser.add_argument("--batch-size", type=int, default=8, help="Mini-batch size")
    parser.add_argument(
        "--input-size",
        default=f"{DEFAULTS['expected_input_size'][0]}x{DEFAULTS['expected_input_size'][1]}",
        help="Input size as N or WIDTHxHEIGHT",
    )
    parser.add_argument("--device", default="auto", help="Evaluation device, e.g. auto/cpu/cuda")
    parser.add_argument("--num-workers", type=int, default=0, help="DataLoader worker count")
    return parser


def load_runtime():
    try:
        from training import torch_runtime
    except ImportError as exc:  # pragma: no cover - depends on local environment
        raise RuntimeError(
            "PyTorch is required for evaluation. Install torch in backend-python/.venv before running."
        ) from exc
    return torch_runtime


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    args.input_size = parse_input_size(args.input_size)
    args.checkpoint = str(resolve_project_path(args.checkpoint))

    output_dirs = prepare_output_dirs(args.output_dir)
    logger = configure_logger("training.grading.eval", output_dirs["logs"] / "evaluate.log")

    if not Path(args.checkpoint).is_file():
        raise RuntimeError(f"checkpoint does not exist: {args.checkpoint}")

    records, _, label_maps = load_dataset_records(
        manifest_path=args.dataset_manifest,
        class_map_path=args.class_map,
        require_mask=True,
    )

    preprocess_config = load_yaml(args.preprocess_config)
    shared_config = preprocess_config.get("shared", {})
    grading_config = preprocess_config.get("grading", {})
    normalize = shared_config.get("normalize", {})
    mean = float((normalize.get("mean") or [0.5])[0])
    std = float((normalize.get("std") or [0.5])[0])
    crop_from_mask = bool(grading_config.get("cropFromMask", True))
    crop_padding_pixels = int(grading_config.get("cropPaddingPixels", 8))
    allow_whole_image_fallback = bool(grading_config.get("allowWholeImageFallback", True))

    logger.info(
        "starting grading evaluation samples=%s checkpoint=%s input_size=%s device=%s",
        len(records),
        args.checkpoint,
        args.input_size,
        args.device,
    )

    runtime = load_runtime()
    model, device, checkpoint_payload = runtime.load_grading_checkpoint(args.checkpoint, args.device)
    data_loader = runtime.make_grading_loader(
        records=records,
        image_size=args.input_size,
        batch_size=args.batch_size,
        num_workers=args.num_workers,
        mean=mean,
        std=std,
        label_map=label_maps.grading,
        crop_from_mask=crop_from_mask,
        crop_padding_pixels=crop_padding_pixels,
        allow_whole_image_fallback=allow_whole_image_fallback,
        shuffle=False,
    )
    criterion = runtime.nn.CrossEntropyLoss()
    metrics, confusion = runtime.evaluate_grading_model(
        model=model,
        data_loader=data_loader,
        device=device,
        criterion=criterion,
    )

    grade_labels = [label for label, _ in sorted(label_maps.grading.items(), key=lambda item: item[1])]
    write_json(
        output_dirs["metrics"] / "metrics.json",
        {
            "task": "grading",
            "datasetManifest": str(args.dataset_manifest),
            "checkpoint": str(args.checkpoint),
            "checkpointEpoch": checkpoint_payload.get("epoch"),
            "samples": len(records),
            "metrics": metrics,
        },
    )
    write_json(
        output_dirs["metrics"] / "confusion_matrix.json",
        build_confusion_matrix_payload(confusion.tolist(), grade_labels),
    )
    write_json(output_dirs["metrics"] / "evaluation_args.json", vars(args))
    logger.info("grading evaluation finished checkpoint=%s", args.checkpoint)


if __name__ == "__main__":
    main()
