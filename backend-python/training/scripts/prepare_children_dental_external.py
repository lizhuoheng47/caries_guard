from __future__ import annotations

import argparse
import json
import os
import shutil
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


CLASS_NAMES = [
    "Impacted",
    "Caries",
    "Periapical_Lesion",
    "Deep_Caries",
]

# Only clinically equivalent labels are mapped. In particular, 深窝沟 (deep
# pits/fissures) is not the same diagnosis as DENTEX Deep_Caries.
LABEL_MAPPING = {
    "龋病": 1,
    "根尖周炎": 2,
}


def parse_args() -> argparse.Namespace:
    repo_root = Path(__file__).resolve().parents[3]
    parser = argparse.ArgumentParser(
        description=(
            "Convert the untouched pediatric disease-detection Test split into "
            "a DENTEX-compatible, two-class external YOLO evaluation dataset."
        )
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        default=repo_root / "data" / "raw" / "children_dental" / "disease_detection",
    )
    parser.add_argument(
        "--output-root",
        type=Path,
        default=repo_root / "data" / "processed" / "children_dental" / "yolo_external",
    )
    parser.add_argument("--split", default="Test", choices=("Test", "Train"))
    parser.add_argument("--image-mode", default="hardlink", choices=("hardlink", "copy"))
    return parser.parse_args()


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def add_image(source: Path, target: Path, mode: str) -> None:
    if mode == "copy":
        shutil.copy2(source, target)
        return
    try:
        os.link(source, target)
    except OSError as exc:
        raise RuntimeError(
            f"Could not create hard link {target}. Re-run with --image-mode copy."
        ) from exc


def yolo_line(class_id: int, points: list[list[float]], width: int, height: int) -> str:
    if len(points) != 2 or any(len(point) < 2 for point in points):
        raise ValueError("rectangle must contain exactly two [x, y] points")
    x1, x2 = sorted((float(points[0][0]), float(points[1][0])))
    y1, y2 = sorted((float(points[0][1]), float(points[1][1])))
    if x1 < 0 or y1 < 0 or x2 > width or y2 > height or x2 <= x1 or y2 <= y1:
        raise ValueError(
            f"rectangle is outside image bounds: {(x1, y1, x2, y2)} vs {(width, height)}"
        )
    center_x = ((x1 + x2) / 2.0) / width
    center_y = ((y1 + y2) / 2.0) / height
    box_width = (x2 - x1) / width
    box_height = (y2 - y1) / height
    values = (center_x, center_y, box_width, box_height)
    return f"{class_id} " + " ".join(f"{value:.8f}".rstrip("0").rstrip(".") for value in values)


def load_label(path: Path) -> dict[str, Any]:
    loaded = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(loaded, dict):
        raise ValueError(f"LabelMe document must be an object: {path}")
    return loaded


def main() -> None:
    args = parse_args()
    source_root = args.source_root.resolve()
    output_root = args.output_root.resolve()
    source_image_dir = source_root / args.split / "images"
    source_label_dir = source_root / args.split / "label"
    output_image_dir = output_root / "images" / "test"
    output_label_dir = output_root / "labels" / "test"

    for required in (source_image_dir, source_label_dir):
        if not required.is_dir():
            raise FileNotFoundError(f"Required directory does not exist: {required}")

    existing = []
    if output_root.exists():
        existing = [path for path in output_root.rglob("*") if path.is_file()]
    if existing:
        raise RuntimeError(f"Output directory is not empty; refusing to mix data: {output_root}")

    output_image_dir.mkdir(parents=True, exist_ok=True)
    output_label_dir.mkdir(parents=True, exist_ok=True)

    image_paths = {path.stem: path for path in source_image_dir.glob("*.png")}
    label_paths = {path.stem: path for path in source_label_dir.glob("*.json")}
    if not image_paths or image_paths.keys() != label_paths.keys():
        missing_labels = sorted(image_paths.keys() - label_paths.keys())
        missing_images = sorted(label_paths.keys() - image_paths.keys())
        raise RuntimeError(
            f"Image/label pairing failed; missing_labels={missing_labels}, missing_images={missing_images}"
        )

    mapped_counts: Counter[str] = Counter()
    ignored_counts: Counter[str] = Counter()
    manifest_rows: list[dict[str, Any]] = []

    for stem in sorted(image_paths):
        image_path = image_paths[stem]
        label_path = label_paths[stem]
        document = load_label(label_path)
        width = int(document.get("imageWidth") or 0)
        height = int(document.get("imageHeight") or 0)
        if width <= 0 or height <= 0:
            raise ValueError(f"Invalid image dimensions in {label_path}: {(width, height)}")

        lines: list[str] = []
        ignored_for_image: Counter[str] = Counter()
        for index, shape in enumerate(document.get("shapes") or []):
            label = str(shape.get("label") or "").strip()
            if label not in LABEL_MAPPING:
                ignored_counts[label] += 1
                ignored_for_image[label] += 1
                continue
            shape_type = str(shape.get("shape_type") or "rectangle").strip().lower()
            if shape_type != "rectangle":
                raise ValueError(f"Unsupported shape type {shape_type!r} in {label_path} shape {index}")
            try:
                lines.append(yolo_line(LABEL_MAPPING[label], shape.get("points") or [], width, height))
            except ValueError as exc:
                raise ValueError(f"Invalid shape {index} in {label_path}: {exc}") from exc
            mapped_counts[label] += 1

        target_image = output_image_dir / image_path.name
        target_label = output_label_dir / f"{stem}.txt"
        add_image(image_path, target_image, args.image_mode)
        write_text(target_label, "\n".join(lines) + ("\n" if lines else ""))
        manifest_rows.append(
            {
                "fileName": image_path.name,
                "width": width,
                "height": height,
                "mappedAnnotationCount": len(lines),
                "ignoredAnnotations": dict(sorted(ignored_for_image.items())),
                "sourceImage": str(image_path),
                "sourceLabel": str(label_path),
            }
        )

    yaml_lines = [
        f"path: {output_root.as_posix()}",
        "train: images/test",
        "val: images/test",
        "test: images/test",
        "names:",
        *[f"  {index}: {name}" for index, name in enumerate(CLASS_NAMES)],
    ]
    write_text(output_root / "dataset.yaml", "\n".join(yaml_lines) + "\n")
    write_text(
        output_root / "manifest.jsonl",
        "\n".join(json.dumps(row, ensure_ascii=False) for row in manifest_rows) + "\n",
    )

    summary = {
        "dataset": "Children's Dental Panoramic Radiographs - pediatric disease detection",
        "purpose": "external evaluation only",
        "sourceSplit": args.split,
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "sourceRoot": str(source_root),
        "outputRoot": str(output_root),
        "imageMode": args.image_mode,
        "images": len(image_paths),
        "modelLabelOrder": CLASS_NAMES,
        "mappedLabels": {
            label: {"classId": class_id, "count": mapped_counts[label]}
            for label, class_id in LABEL_MAPPING.items()
        },
        "ignoredLabels": dict(sorted(ignored_counts.items())),
        "limitations": [
            "Only Caries and Periapical_Lesion have clinically compatible public labels.",
            "Deep pits/fissures are not mapped to DENTEX Deep_Caries.",
            "Dental developmental abnormalities are not mapped to DENTEX Impacted.",
            "This pediatric external set differs from the adult-dominant DENTEX training domain.",
        ],
    }
    write_text(output_root / "dataset_summary.json", json.dumps(summary, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
