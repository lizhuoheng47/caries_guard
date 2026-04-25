from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

BACKEND_ROOT = Path(__file__).resolve().parents[2]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))


IMAGE_SUFFIX = ".png"
MASK_SUFFIX = "-mask.png"
NAME_PATTERN = re.compile(r"^\d{4}-c\d-t(?:-mask)?\.png$", re.IGNORECASE)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Audit segmentation dataset quality under data/traindata.")
    parser.add_argument("--dataset-dir", default=str(REPO_ROOT / "data" / "traindata"))
    parser.add_argument("--output-dir", default=str(REPO_ROOT / "artifacts" / "segmentation_data_audit"))
    return parser


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def repo_relative(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(REPO_ROOT.resolve()))
    except ValueError:
        return str(path.resolve())


def has_valid_name(path: Path) -> bool:
    return bool(NAME_PATTERN.match(path.name))


def image_stem_for_mask(mask_path: Path) -> str:
    if not mask_path.name.lower().endswith(MASK_SUFFIX):
        return mask_path.stem
    return mask_path.name[: -len(MASK_SUFFIX)]


def image_name_for_mask(mask_path: Path) -> str:
    return f"{image_stem_for_mask(mask_path)}{IMAGE_SUFFIX}"


def mask_name_for_image(image_path: Path) -> str:
    return f"{image_path.stem}-mask{image_path.suffix}"


def inspect_mask(mask_path: Path) -> tuple[bool, list[int], tuple[int, int]]:
    mask = Image.open(mask_path).convert("L")
    array = np.asarray(mask, dtype=np.uint8)
    unique_values = sorted(int(value) for value in np.unique(array))
    is_binary = set(unique_values).issubset({0, 255})
    return is_binary, unique_values, mask.size


def inspect_image(image_path: Path) -> tuple[tuple[int, int], str]:
    image = Image.open(image_path)
    return image.size, image.mode


def audit_dataset(dataset_dir: Path) -> dict[str, Any]:
    if not dataset_dir.is_dir():
        raise FileNotFoundError(f"dataset directory does not exist: {dataset_dir}")

    files = sorted(path for path in dataset_dir.iterdir() if path.is_file())
    images = sorted(path for path in files if path.suffix.lower() == IMAGE_SUFFIX and not path.name.lower().endswith(MASK_SUFFIX))
    masks = sorted(path for path in files if path.name.lower().endswith(MASK_SUFFIX))

    image_lookup = {path.name.lower(): path for path in images}
    mask_lookup = {path.name.lower(): path for path in masks}

    naming_issues: list[dict[str, Any]] = []
    missing_images: list[dict[str, Any]] = []
    missing_masks: list[dict[str, Any]] = []
    non_binary_masks: list[dict[str, Any]] = []
    size_mismatches: list[dict[str, Any]] = []
    valid_samples: list[dict[str, Any]] = []

    for path in files:
        if not has_valid_name(path):
            naming_issues.append(
                {
                    "file": path.name,
                    "path": repo_relative(path),
                    "reason": "name_does_not_match_expected_pattern",
                }
            )

    for image_path in images:
        expected_mask = mask_name_for_image(image_path)
        mask_path = mask_lookup.get(expected_mask.lower())
        if mask_path is None:
            missing_masks.append(
                {
                    "image": image_path.name,
                    "imagePath": repo_relative(image_path),
                    "expectedMask": expected_mask,
                }
            )
            continue

        image_size, image_mode = inspect_image(image_path)
        is_binary, unique_values, mask_size = inspect_mask(mask_path)

        if not is_binary:
            non_binary_masks.append(
                {
                    "image": image_path.name,
                    "mask": mask_path.name,
                    "imagePath": repo_relative(image_path),
                    "maskPath": repo_relative(mask_path),
                    "uniqueMaskValues": unique_values,
                }
            )
            continue

        if image_size != mask_size:
            size_mismatches.append(
                {
                    "image": image_path.name,
                    "mask": mask_path.name,
                    "imagePath": repo_relative(image_path),
                    "maskPath": repo_relative(mask_path),
                    "imageSize": [image_size[0], image_size[1]],
                    "maskSize": [mask_size[0], mask_size[1]],
                }
            )
            continue

        valid_samples.append(
            {
                "imageId": image_path.stem,
                "image": image_path.name,
                "mask": mask_path.name,
                "imagePath": repo_relative(image_path),
                "maskPath": repo_relative(mask_path),
                "imageSize": [image_size[0], image_size[1]],
                "imageMode": image_mode,
            }
        )

    for mask_path in masks:
        expected_image = image_name_for_mask(mask_path)
        if expected_image.lower() not in image_lookup:
            missing_images.append(
                {
                    "mask": mask_path.name,
                    "maskPath": repo_relative(mask_path),
                    "expectedImage": expected_image,
                }
            )

    problem_samples = sorted(
        {
            item["image"] if "image" in item else item.get("expectedImage") or item["mask"]
            for item in missing_images + missing_masks + non_binary_masks + size_mismatches
        }
    )

    return {
        "datasetPath": str(dataset_dir.resolve()),
        "summary": {
            "fileCount": len(files),
            "imageCount": len(images),
            "maskCount": len(masks),
            "validSampleCount": len(valid_samples),
            "problemSampleCount": len(problem_samples),
            "missingImageCount": len(missing_images),
            "missingMaskCount": len(missing_masks),
            "nonBinaryMaskCount": len(non_binary_masks),
            "sizeMismatchCount": len(size_mismatches),
            "namingIssueCount": len(naming_issues),
        },
        "issues": {
            "missingImages": missing_images,
            "missingMasks": missing_masks,
            "nonBinaryMasks": non_binary_masks,
            "sizeMismatches": size_mismatches,
            "namingIssues": naming_issues,
        },
        "validSamples": valid_samples,
    }


def main() -> int:
    args = build_parser().parse_args()
    dataset_dir = Path(args.dataset_dir).resolve()
    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    report = audit_dataset(dataset_dir)
    write_json(output_dir / "dataset_audit_report.json", report)
    write_json(output_dir / "valid_samples.json", report["validSamples"])
    write_json(output_dir / "invalid_issues.json", report["issues"])
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
