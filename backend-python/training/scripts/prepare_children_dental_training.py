from __future__ import annotations

import argparse
import json
import math
import random
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from prepare_children_dental_external import add_image, load_label, write_text, yolo_line


CLASS_NAMES = ["Caries", "Periapical_Lesion"]
LABEL_MAPPING = {"龋病": 0, "根尖周炎": 1}


def parse_args() -> argparse.Namespace:
    repo_root = Path(__file__).resolve().parents[3]
    parser = argparse.ArgumentParser(
        description="Build a leakage-safe pediatric two-class YOLO train/val/test dataset."
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        default=repo_root / "data" / "raw" / "children_dental" / "disease_detection",
    )
    parser.add_argument(
        "--output-root",
        type=Path,
        default=repo_root / "data" / "processed" / "children_dental" / "yolo_pediatric_2class",
    )
    parser.add_argument("--validation-ratio", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--image-mode", choices=("hardlink", "copy"), default="hardlink")
    return parser.parse_args()


def mapped_presence(document: dict[str, Any]) -> tuple[bool, bool]:
    labels = {str(shape.get("label") or "").strip() for shape in document.get("shapes") or []}
    return "龋病" in labels, "根尖周炎" in labels


def stratified_validation_stems(
    documents: dict[str, dict[str, Any]], ratio: float, seed: int
) -> set[str]:
    if not 0.05 <= ratio <= 0.5:
        raise ValueError("validation ratio must be between 0.05 and 0.5")
    groups: dict[tuple[bool, bool], list[str]] = defaultdict(list)
    for stem, document in documents.items():
        groups[mapped_presence(document)].append(stem)

    target = max(1, round(len(documents) * ratio))
    allocation: dict[tuple[bool, bool], int] = {}
    remainders: list[tuple[float, tuple[bool, bool]]] = []
    for key in sorted(groups):
        ideal = len(groups[key]) * ratio
        count = min(len(groups[key]), math.floor(ideal))
        if groups[key] and count == 0:
            count = 1
        allocation[key] = count
        remainders.append((ideal - math.floor(ideal), key))

    while sum(allocation.values()) < target:
        candidates = [item for item in remainders if allocation[item[1]] < len(groups[item[1]])]
        _, selected = max(candidates, key=lambda item: (item[0], item[1]))
        allocation[selected] += 1
        remainders = [
            (remainder if key != selected else -1.0, key) for remainder, key in remainders
        ]
    while sum(allocation.values()) > target:
        candidates = [
            item
            for item in remainders
            if allocation[item[1]] > 1
        ]
        _, selected = min(candidates, key=lambda item: (item[0], item[1]))
        allocation[selected] -= 1

    rng = random.Random(seed)
    selected_stems: set[str] = set()
    for key in sorted(groups):
        stems = sorted(groups[key])
        rng.shuffle(stems)
        selected_stems.update(stems[: allocation[key]])
    if len(selected_stems) != target:
        raise RuntimeError(f"Expected {target} validation images, selected {len(selected_stems)}")
    return selected_stems


def main() -> None:
    args = parse_args()
    source_root = args.source_root.resolve()
    output_root = args.output_root.resolve()
    if output_root.exists() and any(path.is_file() for path in output_root.rglob("*")):
        raise RuntimeError(f"Output directory is not empty; refusing to mix data: {output_root}")

    source_documents: dict[str, dict[str, Any]] = {}
    train_label_dir = source_root / "Train" / "label"
    for path in train_label_dir.glob("*.json"):
        source_documents[path.stem] = load_label(path)
    if len(source_documents) != 70:
        raise RuntimeError(f"Expected 70 official Train labels, found {len(source_documents)}")
    validation_stems = stratified_validation_stems(
        source_documents, args.validation_ratio, args.seed
    )

    split_counts: Counter[str] = Counter()
    mapped_counts: Counter[tuple[str, str]] = Counter()
    ignored_counts: Counter[tuple[str, str]] = Counter()
    positive_image_counts: Counter[tuple[str, str]] = Counter()
    manifest_rows: list[dict[str, Any]] = []

    for official_split in ("Train", "Test"):
        image_dir = source_root / official_split / "images"
        label_dir = source_root / official_split / "label"
        images = {path.stem: path for path in image_dir.glob("*.png")}
        labels = {path.stem: path for path in label_dir.glob("*.json")}
        if not images or images.keys() != labels.keys():
            raise RuntimeError(f"Image/label pairing failed in {official_split}")

        for stem in sorted(images):
            split = "test" if official_split == "Test" else (
                "val" if stem in validation_stems else "train"
            )
            document = source_documents[stem] if official_split == "Train" else load_label(labels[stem])
            width = int(document.get("imageWidth") or 0)
            height = int(document.get("imageHeight") or 0)
            if width <= 0 or height <= 0:
                raise ValueError(f"Invalid dimensions in {labels[stem]}")

            lines: list[str] = []
            present_labels: set[str] = set()
            ignored_for_image: Counter[str] = Counter()
            for index, shape in enumerate(document.get("shapes") or []):
                label = str(shape.get("label") or "").strip()
                if label not in LABEL_MAPPING:
                    ignored_counts[(split, label)] += 1
                    ignored_for_image[label] += 1
                    continue
                if str(shape.get("shape_type") or "rectangle").lower() != "rectangle":
                    raise ValueError(f"Unsupported shape in {labels[stem]} at index {index}")
                lines.append(
                    yolo_line(LABEL_MAPPING[label], shape.get("points") or [], width, height)
                )
                mapped_counts[(split, label)] += 1
                present_labels.add(label)

            target_image = output_root / "images" / split / images[stem].name
            target_label = output_root / "labels" / split / f"{stem}.txt"
            target_image.parent.mkdir(parents=True, exist_ok=True)
            target_label.parent.mkdir(parents=True, exist_ok=True)
            add_image(images[stem], target_image, args.image_mode)
            write_text(target_label, "\n".join(lines) + ("\n" if lines else ""))
            split_counts[split] += 1
            for label in present_labels:
                positive_image_counts[(split, label)] += 1
            manifest_rows.append(
                {
                    "fileName": images[stem].name,
                    "officialSplit": official_split,
                    "split": split,
                    "mappedAnnotationCount": len(lines),
                    "ignoredAnnotations": dict(sorted(ignored_for_image.items())),
                    "sourceImage": str(images[stem]),
                    "sourceLabel": str(labels[stem]),
                }
            )

    yaml_lines = [
        f"path: {output_root.as_posix()}",
        "train: images/train",
        "val: images/val",
        "test: images/test",
        "names:",
        "  0: Caries",
        "  1: Periapical_Lesion",
    ]
    write_text(output_root / "dataset.yaml", "\n".join(yaml_lines) + "\n")
    write_text(
        output_root / "manifest.jsonl",
        "\n".join(json.dumps(row, ensure_ascii=False) for row in manifest_rows) + "\n",
    )

    def nested_counts(counter: Counter[tuple[str, str]]) -> dict[str, dict[str, int]]:
        result: dict[str, dict[str, int]] = {}
        for (split, label), count in sorted(counter.items()):
            result.setdefault(split, {})[label] = count
        return result

    summary = {
        "dataset": "Pediatric dental disease detection two-class subset",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "seed": args.seed,
        "validationRatioWithinOfficialTrain": args.validation_ratio,
        "images": dict(sorted(split_counts.items())),
        "classNames": CLASS_NAMES,
        "mappedAnnotations": nested_counts(mapped_counts),
        "positiveImages": nested_counts(positive_image_counts),
        "ignoredAnnotations": nested_counts(ignored_counts),
        "splitPolicy": {
            "trainAndVal": "deterministic multilabel-presence stratification of official Train",
            "test": "official Test; never used for gradient updates or early stopping",
        },
        "limitations": [
            "Only Caries and Periapical_Lesion are mapped.",
            "The dataset is small and pediatric-only.",
            "Unmapped disease categories are treated as non-target findings.",
            "The official Test split was previously used once for baseline external evaluation.",
        ],
    }
    write_text(output_root / "dataset_summary.json", json.dumps(summary, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
