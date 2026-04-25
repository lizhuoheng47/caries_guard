from __future__ import annotations

import argparse
import json
import logging
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError:  # pragma: no cover - handled at runtime with a clear error
    yaml = None

PROJECT_ROOT = Path(__file__).resolve().parents[1]
REQUIRED_MANIFEST_FIELDS = {
    "imageId",
    "imagePath",
    "maskPath",
    "gradeLabel",
    "qualityLabel",
    "imageType",
    "annotationVersion",
    "desensitized",
}


@dataclass(frozen=True)
class DatasetRecord:
    image_id: str
    image_path: Path
    mask_path: Path
    grade_label: str
    quality_label: str
    image_type: str
    annotation_version: str
    desensitized: bool


@dataclass(frozen=True)
class LabelMaps:
    segmentation: dict[str, int]
    grading: dict[str, int]
    quality: dict[str, int]
    image_types: set[str]
    annotation_version: str
    ignore_index: int


def ensure_yaml_available() -> None:
    if yaml is None:
        raise RuntimeError(
            "PyYAML is required to read shared training assets. "
            "Install it in the backend-python environment before running training scripts."
        )


def resolve_project_path(path_value: str | Path, project_root: Path = PROJECT_ROOT) -> Path:
    path = Path(path_value)
    if path.is_absolute():
        return path
    return (project_root / path).resolve()


def load_json(path_value: str | Path) -> Any:
    path = resolve_project_path(path_value)
    return json.loads(path.read_text(encoding="utf-8"))


def load_yaml(path_value: str | Path) -> dict[str, Any]:
    ensure_yaml_available()
    path = resolve_project_path(path_value)
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def parse_input_size(raw_value: str | int | list[int] | tuple[int, int]) -> tuple[int, int]:
    if isinstance(raw_value, (list, tuple)):
        if len(raw_value) != 2:
            raise ValueError(f"input size must contain 2 integers, got: {raw_value}")
        width, height = int(raw_value[0]), int(raw_value[1])
        return width, height
    if isinstance(raw_value, int):
        return raw_value, raw_value

    raw_text = str(raw_value).strip().lower()
    if "x" in raw_text:
        width_text, height_text = raw_text.split("x", maxsplit=1)
        return int(width_text), int(height_text)
    size = int(raw_text)
    return size, size


def read_jsonl(path_value: str | Path) -> list[dict[str, Any]]:
    path = resolve_project_path(path_value)
    rows: list[dict[str, Any]] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        content = line.strip()
        if not content:
            continue
        try:
            rows.append(json.loads(content))
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"invalid JSONL at {path}:{line_number}: {exc}") from exc
    if not rows:
        raise RuntimeError(f"manifest is empty: {path}")
    return rows


def build_label_maps(class_map: dict[str, Any]) -> LabelMaps:
    segmentation = {
        item["classCode"]: int(item["classId"])
        for item in class_map.get("segmentationClasses", [])
    }
    grading = {
        item["labelCode"]: int(item["labelId"])
        for item in class_map.get("gradingLabels", [])
    }
    quality = {
        item["labelCode"]: int(item["labelId"])
        for item in class_map.get("qualityLabels", [])
    }
    image_types = {item["typeCode"] for item in class_map.get("imageTypes", [])}
    annotation_version = str(class_map.get("annotationVersion") or "")
    ignore_index = int(class_map.get("maskEncoding", {}).get("ignoreIndex", 255))

    if not segmentation or not grading or not quality:
        raise RuntimeError("class_map.json is missing segmentation, grading, or quality labels")

    return LabelMaps(
        segmentation=segmentation,
        grading=grading,
        quality=quality,
        image_types=image_types,
        annotation_version=annotation_version,
        ignore_index=ignore_index,
    )


def load_task_defaults(task_name: str) -> dict[str, Any]:
    manifest_name = {
        "segmentation": "segmentation_v1.yaml",
        "grading": "grading_v1.yaml",
    }.get(task_name)
    if manifest_name is None:
        raise ValueError(f"unsupported task name: {task_name}")

    model_manifest_path = PROJECT_ROOT / "assets" / "models" / "manifests" / manifest_name
    model_manifest = load_yaml(model_manifest_path)
    preprocess_path = resolve_project_path(model_manifest["inputSpec"]["preprocessConfigPath"])
    preprocess_config = load_yaml(preprocess_path)
    expected_size = (
        model_manifest.get("inputSpec", {}).get("expectedImageSize")
        or preprocess_config.get("shared", {}).get("imageSize")
        or [512, 512]
    )
    return {
        "project_root": PROJECT_ROOT,
        "model_manifest_path": model_manifest_path,
        "model_manifest": model_manifest,
        "preprocess_path": preprocess_path,
        "preprocess_config": preprocess_config,
        "class_map_path": resolve_project_path(model_manifest["dataset"]["classMapPath"]),
        "train_manifest_path": resolve_project_path(model_manifest["dataset"]["trainManifestPath"]),
        "val_manifest_path": resolve_project_path(model_manifest["dataset"]["valManifestPath"]),
        "test_manifest_path": resolve_project_path(model_manifest["dataset"]["testManifestPath"]),
        "expected_input_size": parse_input_size(expected_size),
    }


def load_dataset_records(
    manifest_path: str | Path,
    class_map_path: str | Path,
    require_mask: bool = True,
) -> tuple[list[DatasetRecord], dict[str, Any], LabelMaps]:
    class_map = load_json(class_map_path)
    label_maps = build_label_maps(class_map)
    rows = read_jsonl(manifest_path)

    manifest_file = resolve_project_path(manifest_path)
    records: list[DatasetRecord] = []
    for index, row in enumerate(rows, start=1):
        missing_fields = REQUIRED_MANIFEST_FIELDS - row.keys()
        if missing_fields:
            raise RuntimeError(
                f"manifest record {index} in {manifest_file} is missing fields: {sorted(missing_fields)}"
            )

        image_path = resolve_project_path(row["imagePath"])
        mask_path = resolve_project_path(row["maskPath"])
        if not image_path.is_file():
            raise RuntimeError(f"image file does not exist for record {index}: {image_path}")
        if require_mask and not mask_path.is_file():
            raise RuntimeError(f"mask file does not exist for record {index}: {mask_path}")

        grade_label = str(row["gradeLabel"])
        quality_label = str(row["qualityLabel"])
        image_type = str(row["imageType"])
        annotation_version = str(row["annotationVersion"])
        desensitized = row["desensitized"]

        if grade_label not in label_maps.grading:
            raise RuntimeError(f"unknown gradeLabel={grade_label} in record {index}")
        if quality_label not in label_maps.quality:
            raise RuntimeError(f"unknown qualityLabel={quality_label} in record {index}")
        if image_type not in label_maps.image_types:
            raise RuntimeError(f"unknown imageType={image_type} in record {index}")
        if annotation_version != label_maps.annotation_version:
            raise RuntimeError(
                "annotationVersion mismatch in record "
                f"{index}: expected {label_maps.annotation_version}, got {annotation_version}"
            )
        if not isinstance(desensitized, bool):
            raise RuntimeError(f"desensitized must be boolean in record {index}")

        records.append(
            DatasetRecord(
                image_id=str(row["imageId"]),
                image_path=image_path,
                mask_path=mask_path,
                grade_label=grade_label,
                quality_label=quality_label,
                image_type=image_type,
                annotation_version=annotation_version,
                desensitized=desensitized,
            )
        )

    return records, class_map, label_maps


def prepare_output_dirs(output_dir: str | Path) -> dict[str, Path]:
    root = resolve_project_path(output_dir)
    checkpoints = root / "checkpoints"
    metrics = root / "metrics"
    logs = root / "logs"
    for path in (root, checkpoints, metrics, logs):
        path.mkdir(parents=True, exist_ok=True)
    return {
        "root": root,
        "checkpoints": checkpoints,
        "metrics": metrics,
        "logs": logs,
    }


def configure_logger(logger_name: str, log_file: Path) -> logging.Logger:
    logger = logging.getLogger(logger_name)
    logger.setLevel(logging.INFO)
    logger.propagate = False
    for handler in list(logger.handlers):
        logger.removeHandler(handler)

    formatter = logging.Formatter("%(asctime)s | %(levelname)s | %(message)s")

    stream_handler = logging.StreamHandler(sys.stdout)
    stream_handler.setFormatter(formatter)
    logger.addHandler(stream_handler)

    file_handler = logging.FileHandler(log_file, encoding="utf-8")
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    return logger


def _to_jsonable(value: Any) -> Any:
    if isinstance(value, Path):
        return str(value)
    if isinstance(value, tuple):
        return [_to_jsonable(item) for item in value]
    if isinstance(value, list):
        return [_to_jsonable(item) for item in value]
    if isinstance(value, dict):
        return {key: _to_jsonable(item) for key, item in value.items()}
    return value


def write_json(path: str | Path, payload: Any) -> None:
    output_path = resolve_project_path(path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(_to_jsonable(payload), ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def save_training_args(metrics_dir: Path, args: argparse.Namespace) -> None:
    write_json(metrics_dir / "training_args.json", vars(args))


def build_confusion_matrix_payload(matrix: list[list[int]] | Any, labels: list[str]) -> dict[str, Any]:
    rows = [[int(value) for value in row] for row in matrix]
    total = int(sum(sum(row) for row in rows))
    return {
        "labels": labels,
        "matrix": rows,
        "total": total,
    }
