from __future__ import annotations

import argparse
import json
import random
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from training.common import load_json, resolve_project_path  # noqa: E402


IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg"}
MASK_SUFFIX = "-mask.png"
NAME_PATTERN = re.compile(r"^(?P<sample_id>\d{4})-(?P<grade>c\d+)-t$", re.IGNORECASE)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Build training JSONL manifests from data/traindata image-mask pairs."
    )
    parser.add_argument("--dataset-dir", default=str(REPO_ROOT / "data" / "traindata"))
    parser.add_argument(
        "--output-dir",
        default=str(BACKEND_ROOT / "training" / "outputs" / "datasets" / "traindata"),
    )
    parser.add_argument(
        "--class-map",
        default=str(BACKEND_ROOT / "assets" / "datasets" / "caries_v1" / "meta" / "class_map.json"),
    )
    parser.add_argument("--val-ratio", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--quality-label", default="PASS")
    parser.add_argument("--image-type", default="BITEWING")
    parser.add_argument("--desensitized", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument(
        "--converted-mask-dir-name",
        default="masks_class_id",
        help="Subdirectory under output-dir for generated 0/1 class-id masks.",
    )
    return parser


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "\n".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in rows) + "\n",
        encoding="utf-8",
    )


def backend_relative(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(BACKEND_ROOT.resolve())).replace("\\", "/")
    except ValueError:
        try:
            return str(path.resolve().relative_to(REPO_ROOT.resolve())).replace("\\", "/")
        except ValueError:
            return str(path.resolve())


def project_relative_for_manifest(path: Path) -> str:
    try:
        relative = path.resolve().relative_to(BACKEND_ROOT.resolve())
        return str(relative).replace("\\", "/")
    except ValueError:
        try:
            relative_to_repo = path.resolve().relative_to(REPO_ROOT.resolve())
            return str(Path("..") / relative_to_repo).replace("\\", "/")
        except ValueError:
            return str(path.resolve())


def load_mask(mask_path: Path) -> np.ndarray:
    array = np.asarray(Image.open(mask_path).convert("L"), dtype=np.uint8)
    unique_values = {int(value) for value in np.unique(array)}
    if not unique_values.issubset({0, 255}):
        raise ValueError(f"mask must be binary 0/255, got {sorted(unique_values)}")
    return array


def write_class_id_mask(source_mask_path: Path, target_mask_path: Path) -> int:
    source = load_mask(source_mask_path)
    class_id_mask = (source > 0).astype(np.uint8)
    target_mask_path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(class_id_mask, mode="L").save(target_mask_path)
    return int(class_id_mask.sum())


def scan_pairs(
    dataset_dir: Path,
    converted_mask_dir: Path,
    annotation_version: str,
    quality_label: str,
    image_type: str,
    desensitized: bool,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    image_paths = sorted(
        path
        for path in dataset_dir.iterdir()
        if path.is_file()
        and path.suffix.lower() in IMAGE_EXTENSIONS
        and not path.name.lower().endswith(MASK_SUFFIX)
    )
    rows: list[dict[str, Any]] = []
    skipped: list[dict[str, Any]] = []

    for image_path in image_paths:
        match = NAME_PATTERN.match(image_path.stem)
        if not match:
            skipped.append(
                {
                    "file": image_path.name,
                    "reason": "invalid_image_name",
                    "expectedPattern": "0000-c1-t.png or 0000-c1-t.jpg",
                }
            )
            continue

        mask_path = dataset_dir / f"{image_path.stem}{MASK_SUFFIX}"
        if not mask_path.is_file():
            skipped.append({"file": image_path.name, "reason": "missing_mask", "expectedMask": mask_path.name})
            continue

        try:
            image_size = Image.open(image_path).size
            mask_array = load_mask(mask_path)
        except Exception as exc:
            skipped.append({"file": image_path.name, "reason": "invalid_pair", "detail": str(exc)})
            continue

        mask_size = (int(mask_array.shape[1]), int(mask_array.shape[0]))
        if image_size != mask_size:
            skipped.append(
                {
                    "file": image_path.name,
                    "reason": "size_mismatch",
                    "imageSize": list(image_size),
                    "maskSize": list(mask_size),
                    "maskFile": mask_path.name,
                }
            )
            continue

        converted_mask_path = converted_mask_dir / f"{image_path.stem}-mask.png"
        positive_pixels = write_class_id_mask(mask_path, converted_mask_path)

        rows.append(
            {
                "imageId": image_path.stem,
                "imagePath": project_relative_for_manifest(image_path),
                "maskPath": project_relative_for_manifest(converted_mask_path),
                "gradeLabel": match.group("grade").upper(),
                "qualityLabel": quality_label,
                "imageType": image_type,
                "annotationVersion": annotation_version,
                "desensitized": desensitized,
                "sourceMaskPath": project_relative_for_manifest(mask_path),
                "maskPositivePixels": positive_pixels,
            }
        )

    return rows, skipped


def stratified_train_val_split(
    rows: list[dict[str, Any]],
    val_ratio: float,
    seed: int,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        grouped[str(row["gradeLabel"])].append(row)

    rng = random.Random(seed)
    train_rows: list[dict[str, Any]] = []
    val_rows: list[dict[str, Any]] = []
    for grade_rows in grouped.values():
        shuffled = list(grade_rows)
        rng.shuffle(shuffled)
        if len(shuffled) <= 1:
            train_rows.extend(shuffled)
            continue
        val_count = max(1, int(round(len(shuffled) * val_ratio)))
        val_count = min(len(shuffled) - 1, val_count)
        val_rows.extend(shuffled[:val_count])
        train_rows.extend(shuffled[val_count:])

    if not train_rows or not val_rows:
        raise RuntimeError(f"train/val split failed: train={len(train_rows)} val={len(val_rows)}")

    train_rows.sort(key=lambda row: str(row["imageId"]))
    val_rows.sort(key=lambda row: str(row["imageId"]))
    return train_rows, val_rows


def validate_labels(
    rows: list[dict[str, Any]],
    class_map: dict[str, Any],
    quality_label: str,
    image_type: str,
) -> str:
    grades = {item["labelCode"] for item in class_map.get("gradingLabels", [])}
    quality_labels = {item["labelCode"] for item in class_map.get("qualityLabels", [])}
    image_types = {item["typeCode"] for item in class_map.get("imageTypes", [])}
    annotation_version = str(class_map.get("annotationVersion") or "")

    unknown_grades = sorted({str(row["gradeLabel"]) for row in rows} - grades)
    if unknown_grades:
        raise RuntimeError(f"unknown grade labels in filenames: {unknown_grades}")
    if quality_label not in quality_labels:
        raise RuntimeError(f"unknown quality label: {quality_label}")
    if image_type not in image_types:
        raise RuntimeError(f"unknown image type: {image_type}")
    if not annotation_version:
        raise RuntimeError("class_map.json is missing annotationVersion")
    return annotation_version


def count_by_label(rows: list[dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = defaultdict(int)
    for row in rows:
        counts[str(row["gradeLabel"])] += 1
    return dict(sorted(counts.items()))


def main() -> int:
    args = build_parser().parse_args()
    dataset_dir = Path(args.dataset_dir).resolve()
    output_dir = Path(args.output_dir).resolve()
    class_map_path = resolve_project_path(args.class_map)
    converted_mask_dir = output_dir / str(args.converted_mask_dir_name)

    if not dataset_dir.is_dir():
        raise FileNotFoundError(f"dataset directory does not exist: {dataset_dir}")

    class_map = load_json(class_map_path)
    annotation_version = str(class_map.get("annotationVersion") or "")
    rows, skipped = scan_pairs(
        dataset_dir=dataset_dir,
        converted_mask_dir=converted_mask_dir,
        annotation_version=annotation_version,
        quality_label=args.quality_label,
        image_type=args.image_type,
        desensitized=bool(args.desensitized),
    )
    validate_labels(rows, class_map, args.quality_label, args.image_type)
    train_rows, val_rows = stratified_train_val_split(rows, args.val_ratio, args.seed)

    write_jsonl(output_dir / "train.jsonl", train_rows)
    write_jsonl(output_dir / "val.jsonl", val_rows)
    write_json(
        output_dir / "summary.json",
        {
            "datasetDir": str(dataset_dir),
            "classMap": backend_relative(class_map_path),
            "seed": args.seed,
            "valRatio": args.val_ratio,
            "totalPairs": len(rows),
            "trainCount": len(train_rows),
            "valCount": len(val_rows),
            "skippedCount": len(skipped),
            "gradeCounts": count_by_label(rows),
            "trainGradeCounts": count_by_label(train_rows),
            "valGradeCounts": count_by_label(val_rows),
            "convertedMaskDir": backend_relative(converted_mask_dir),
            "maskEncoding": {
                "backgroundValue": 0,
                "foregroundValue": 1,
                "sourceForegroundValue": 255,
                "ignoreIndex": class_map.get("maskEncoding", {}).get("ignoreIndex"),
            },
            "skipped": skipped,
            "trainManifest": backend_relative(output_dir / "train.jsonl"),
            "valManifest": backend_relative(output_dir / "val.jsonl"),
        },
    )

    print(
        json.dumps(
            {
                "totalPairs": len(rows),
                "trainCount": len(train_rows),
                "valCount": len(val_rows),
                "skippedCount": len(skipped),
                "gradeCounts": count_by_label(rows),
                "trainManifest": str((output_dir / "train.jsonl").resolve()),
                "valManifest": str((output_dir / "val.jsonl").resolve()),
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
