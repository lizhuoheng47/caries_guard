from __future__ import annotations

import argparse
import hashlib
import json
import random
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image


BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
DEFAULT_SOURCE = REPO_ROOT / "data" / "raw" / "dc1000" / "full" / "DC1000_dataset"
DEFAULT_OUTPUT = REPO_ROOT / "data" / "processed" / "dc1000" / "segmentation_v1"
ALLOWED_TRAIN_VALUES = {0, 255}
ALLOWED_TEST_VALUES = {0, 102, 153, 255}
ANNOTATION_VERSION = "caries-annot-v1.0"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Validate DC1000, preserve the official test set, convert masks to 0/1, "
            "and build leakage-resistant train/val/test manifests."
        )
    )
    parser.add_argument("--source-dir", default=str(DEFAULT_SOURCE))
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--val-ratio", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=42)
    return parser


def natural_key(path_or_name: Path | str) -> tuple[int, str]:
    name = Path(path_or_name).stem
    return (int(name), name) if name.isdigit() else (2**31 - 1, name.lower())


def repo_relative(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(REPO_ROOT.resolve())).replace("\\", "/")
    except ValueError:
        return str(path.resolve())


def manifest_path(path: Path) -> str:
    try:
        relative = path.resolve().relative_to(BACKEND_ROOT.resolve())
        return str(relative).replace("\\", "/")
    except ValueError:
        try:
            relative = path.resolve().relative_to(REPO_ROOT.resolve())
            return str(Path("..") / relative).replace("\\", "/")
        except ValueError:
            return str(path.resolve())


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "\n".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in rows)
        + "\n",
        encoding="utf-8",
    )


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def severity_label(unique_values: set[int]) -> str:
    if 255 in unique_values:
        return "C3"
    if 153 in unique_values:
        return "C2"
    if 102 in unique_values:
        return "C1"
    return "C0"


def scan_split(
    *,
    split_name: str,
    image_dir: Path,
    mask_dir: Path,
    converted_mask_dir: Path,
    allowed_mask_values: set[int],
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    image_files = {path.name: path for path in image_dir.glob("*.png")}
    mask_files = {path.name: path for path in mask_dir.glob("*.png")}
    paired_names = sorted(image_files.keys() & mask_files.keys(), key=natural_key)

    rows: list[dict[str, Any]] = []
    issues: list[dict[str, Any]] = []
    dimensions: Counter[str] = Counter()
    mask_value_pixels: Counter[int] = Counter()
    total_pixels = 0
    total_foreground_pixels = 0

    for name in sorted(image_files.keys() - mask_files.keys(), key=natural_key):
        issues.append({"type": "missing_mask", "file": name})
    for name in sorted(mask_files.keys() - image_files.keys(), key=natural_key):
        issues.append({"type": "missing_image", "file": name})

    converted_mask_dir.mkdir(parents=True, exist_ok=True)
    for name in paired_names:
        image_path = image_files[name]
        source_mask_path = mask_files[name]
        try:
            with Image.open(image_path) as image:
                image_size = image.size
                image_mode = image.mode
            with Image.open(source_mask_path) as source_mask:
                mask_array = np.asarray(source_mask.convert("L"), dtype=np.uint8)
        except Exception as exc:
            issues.append({"type": "unreadable_pair", "file": name, "detail": str(exc)})
            continue

        mask_size = (int(mask_array.shape[1]), int(mask_array.shape[0]))
        if image_size != mask_size:
            issues.append(
                {
                    "type": "size_mismatch",
                    "file": name,
                    "imageSize": list(image_size),
                    "maskSize": list(mask_size),
                }
            )
            continue

        unique, counts = np.unique(mask_array, return_counts=True)
        unique_values = {int(value) for value in unique}
        unknown_values = sorted(unique_values - allowed_mask_values)
        if unknown_values:
            issues.append(
                {
                    "type": "unexpected_mask_values",
                    "file": name,
                    "values": sorted(unique_values),
                    "unexpected": unknown_values,
                }
            )
            continue

        for value, count in zip(unique, counts):
            mask_value_pixels[int(value)] += int(count)

        binary_mask = (mask_array > 0).astype(np.uint8)
        foreground_pixels = int(binary_mask.sum())
        pixel_count = int(binary_mask.size)
        total_foreground_pixels += foreground_pixels
        total_pixels += pixel_count

        converted_mask_path = converted_mask_dir / name
        Image.fromarray(binary_mask, mode="L").save(converted_mask_path, optimize=True)
        image_hash = sha256_file(image_path)

        rows.append(
            {
                "imageId": f"dc1000-{split_name}-{image_path.stem}",
                "imagePath": manifest_path(image_path),
                "maskPath": manifest_path(converted_mask_path),
                "gradeLabel": severity_label(unique_values),
                "qualityLabel": "PASS",
                "imageType": "PANORAMIC",
                "annotationVersion": ANNOTATION_VERSION,
                "desensitized": True,
                "sourceDataset": "DC1000",
                "sourceSplit": split_name,
                "sourceImageId": image_path.stem,
                "sourceMaskPath": manifest_path(source_mask_path),
                "imageSha256": image_hash,
                "imageSize": [int(image_size[0]), int(image_size[1])],
                "imageMode": image_mode,
                "maskSourceValues": sorted(unique_values),
                "maskPositivePixels": foreground_pixels,
                "maskPositiveRatio": foreground_pixels / pixel_count,
            }
        )
        dimensions[f"{image_size[0]}x{image_size[1]}"] += 1

    report = {
        "split": split_name,
        "imageDirectory": repo_relative(image_dir),
        "maskDirectory": repo_relative(mask_dir),
        "imageCount": len(image_files),
        "maskCount": len(mask_files),
        "pairedNameCount": len(paired_names),
        "validPairCount": len(rows),
        "issueCount": len(issues),
        "positiveImageCount": sum(int(row["maskPositivePixels"] > 0) for row in rows),
        "emptyMaskCount": sum(int(row["maskPositivePixels"] == 0) for row in rows),
        "foregroundPixelRatio": total_foreground_pixels / total_pixels if total_pixels else 0.0,
        "maskValuePixelCounts": {str(key): value for key, value in sorted(mask_value_pixels.items())},
        "dimensionCounts": dict(sorted(dimensions.items())),
        "issues": issues,
    }
    return rows, report


def split_train_val(
    rows: list[dict[str, Any]], val_ratio: float, seed: int
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if not 0.0 < val_ratio < 1.0:
        raise ValueError("--val-ratio must be between 0 and 1")

    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        groups[str(row["gradeLabel"])].append(row)

    rng = random.Random(seed)
    train_rows: list[dict[str, Any]] = []
    val_rows: list[dict[str, Any]] = []
    for label in sorted(groups):
        group = list(groups[label])
        rng.shuffle(group)
        if len(group) == 1:
            train_rows.extend(group)
            continue
        val_count = max(1, round(len(group) * val_ratio))
        val_count = min(val_count, len(group) - 1)
        val_rows.extend(group[:val_count])
        train_rows.extend(group[val_count:])

    train_rows.sort(key=lambda row: natural_key(str(row["sourceImageId"])))
    val_rows.sort(key=lambda row: natural_key(str(row["sourceImageId"])))
    return train_rows, val_rows


def label_counts(rows: list[dict[str, Any]]) -> dict[str, int]:
    return dict(sorted(Counter(str(row["gradeLabel"]) for row in rows).items()))


def find_cross_split_duplicates(splits: dict[str, list[dict[str, Any]]]) -> list[dict[str, Any]]:
    occurrences: dict[str, list[dict[str, str]]] = defaultdict(list)
    for split_name, rows in splits.items():
        for row in rows:
            occurrences[str(row["imageSha256"])].append(
                {"split": split_name, "imageId": str(row["imageId"]), "path": str(row["imagePath"])}
            )
    return [
        {"sha256": digest, "occurrences": items}
        for digest, items in sorted(occurrences.items())
        if len({item["split"] for item in items}) > 1
    ]


def write_dataset_card(output_dir: Path, summary: dict[str, Any]) -> None:
    train = summary["splits"]["train"]
    val = summary["splits"]["val"]
    test = summary["splits"]["test"]
    text = f"""# DC1000 binary caries segmentation dataset

This prepared dataset uses the original panoramic images and pixel masks from DC1000.

- Train: {train['count']} original panoramics
- Validation: {val['count']} original panoramics
- Official test: {test['count']} panoramics, never used for fitting or early stopping
- Mask encoding: 0 background, 1 any annotated caries lesion
- Split seed: {summary['seed']}

The publisher-provided `train/images` and `train/labels` slice/augmentation directories are deliberately excluded from the first benchmark because their source-panorama grouping is not supplied. Randomly splitting those correlated slices could leak one patient/image into both training and validation.

DC1000 is research data, not clinical validation. Its labels and acquisition domain must be reviewed before any clinical claim.
"""
    (output_dir / "dataset_card.md").write_text(text, encoding="utf-8")


def main() -> int:
    args = build_parser().parse_args()
    source_dir = Path(args.source_dir).resolve()
    output_dir = Path(args.output_dir).resolve()
    if not source_dir.is_dir():
        raise FileNotFoundError(f"DC1000 source directory does not exist: {source_dir}")

    original_train = source_dir / "org_train_dataset"
    original_test = source_dir / "org_test_dataset"
    train_pool_rows, train_audit = scan_split(
        split_name="original-train",
        image_dir=original_train / "images",
        mask_dir=original_train / "labels_clean",
        converted_mask_dir=output_dir / "masks" / "trainval",
        allowed_mask_values=ALLOWED_TRAIN_VALUES,
    )
    test_rows, test_audit = scan_split(
        split_name="official-test",
        image_dir=original_test / "images",
        mask_dir=original_test / "labels",
        converted_mask_dir=output_dir / "masks" / "test",
        allowed_mask_values=ALLOWED_TEST_VALUES,
    )

    train_blocking_issues = [
        issue for issue in train_audit["issues"] if issue["type"] != "missing_mask"
    ]
    test_blocking_issues = list(test_audit["issues"])
    if train_blocking_issues or test_blocking_issues:
        write_json(output_dir / "audit.json", {"trainPool": train_audit, "officialTest": test_audit})
        raise RuntimeError(
            "DC1000 audit found invalid image-mask pairs; inspect "
            f"{output_dir / 'audit.json'} before creating manifests"
        )
    if not train_pool_rows or not test_rows:
        raise RuntimeError("DC1000 train or official test split is empty")

    train_rows, val_rows = split_train_val(train_pool_rows, args.val_ratio, args.seed)
    duplicates = find_cross_split_duplicates(
        {"train": train_rows, "val": val_rows, "official-test": test_rows}
    )
    if duplicates:
        write_json(output_dir / "cross_split_duplicates.json", duplicates)
        raise RuntimeError(
            "Identical image bytes occur across splits; inspect "
            f"{output_dir / 'cross_split_duplicates.json'}"
        )

    manifests_dir = output_dir / "manifests"
    write_jsonl(manifests_dir / "train.jsonl", train_rows)
    write_jsonl(manifests_dir / "val.jsonl", val_rows)
    write_jsonl(manifests_dir / "test.jsonl", test_rows)
    write_json(output_dir / "audit.json", {"trainPool": train_audit, "officialTest": test_audit})

    summary = {
        "dataset": "DC1000",
        "task": "binary-caries-lesion-segmentation",
        "sourceDirectory": repo_relative(source_dir),
        "outputDirectory": repo_relative(output_dir),
        "seed": args.seed,
        "validationRatio": args.val_ratio,
        "maskEncoding": {
            "backgroundValue": 0,
            "foregroundValue": 1,
            "sourceTrainValues": sorted(ALLOWED_TRAIN_VALUES),
            "sourceTestValues": sorted(ALLOWED_TEST_VALUES),
            "conversion": "source_value > 0",
        },
        "splits": {
            "train": {"count": len(train_rows), "gradeCounts": label_counts(train_rows)},
            "val": {"count": len(val_rows), "gradeCounts": label_counts(val_rows)},
            "test": {
                "count": len(test_rows),
                "gradeCounts": label_counts(test_rows),
                "official": True,
                "excludedFromTrainingAndEarlyStopping": True,
            },
        },
        "crossSplitDuplicateImageCount": 0,
        "excludedOriginalTrainImagesWithoutCleanMask": [
            issue["file"]
            for issue in train_audit["issues"]
            if issue["type"] == "missing_mask"
        ],
        "excludedPublisherAugmentedSlices": {
            "imageDirectory": repo_relative(source_dir / "train" / "images"),
            "maskDirectory": repo_relative(source_dir / "train" / "labels"),
            "reason": "source-panorama grouping is unavailable; patch-level random split risks leakage",
        },
        "manifests": {
            "train": repo_relative(manifests_dir / "train.jsonl"),
            "val": repo_relative(manifests_dir / "val.jsonl"),
            "test": repo_relative(manifests_dir / "test.jsonl"),
        },
    }
    write_json(output_dir / "summary.json", summary)
    write_dataset_card(output_dir, summary)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
