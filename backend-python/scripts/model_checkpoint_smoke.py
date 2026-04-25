from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path
from typing import Any


BACKEND_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = BACKEND_ROOT.parent
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from app.core.config import Settings
from app.infra.model.checkpoint_validator import CheckpointValidator
from app.infra.model.model_assets import ModelAssets
from app.infra.model.segmentation_model_adapter import SegmentationModelAdapter
from app.infra.model.grading_model_adapter import GradingModelAdapter


def _settings(module: str) -> Settings:
    values = {
        "ai_runtime_mode": "real",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "model_quality_enabled": False,
        "model_quality_impl_type": "HEURISTIC",
        "model_tooth_detect_enabled": False,
        "model_tooth_detect_impl_type": "HEURISTIC",
        "model_segmentation_enabled": module == "segmentation",
        "model_segmentation_impl_type": "ML_MODEL" if module == "segmentation" else "HEURISTIC",
        "model_grading_enabled": module == "grading",
        "model_grading_impl_type": "ML_MODEL" if module == "grading" else "HEURISTIC",
        "model_risk_enabled": False,
        "model_risk_impl_type": "HEURISTIC",
        "model_weights_dir": str(REPO_ROOT / "model-weights"),
        "quality_model_param_path": str(REPO_ROOT / "model-weights" / "quality" / "quality_model_params.json"),
        "quality_model_weights_path": str(REPO_ROOT / "model-weights" / "quality" / "quality_model_params.json"),
        "model_confidence_threshold": 0.3,
        "strict_model_startup_validation": False,
    }
    return Settings(**values)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _validate_module(module: str, assets: ModelAssets) -> dict[str, Any]:
    manifest = assets.segmentation_manifest if module == "segmentation" else assets.grading_manifest
    validator = CheckpointValidator(module)
    validator.validate_manifest_assets(
        manifest,
        class_map=assets.class_map(),
        preprocess=assets.preprocess_config(),
        postprocess=assets.postprocess_config(),
    )
    validator.validate_checkpoint_ready(manifest)
    summary = {
        "module": module,
        "manifestPath": str(manifest.manifest_path),
        "checkpointPath": str(manifest.checkpoint_path),
        "checkpointFormat": manifest.checkpoint_format,
        "checkpointSha256Actual": manifest.checkpoint_actual_sha256 or _sha256(manifest.checkpoint_path),
        "checkpointSha256Declared": manifest.checkpoint_declared_sha256,
        "status": manifest.status,
        "exportedAt": manifest.exported_at,
        "datasetVersion": manifest.dataset_version,
        "classMapPath": str(manifest.class_map_path),
        "preprocessPath": str(manifest.preprocess_path),
        "postprocessPath": str(manifest.postprocess_path),
    }
    print(json.dumps({"sha256": summary}, ensure_ascii=False, indent=2))
    return summary


def _load_regions(args: argparse.Namespace) -> list[dict[str, Any]]:
    if args.regions_json:
        loaded = json.loads(Path(args.regions_json).read_text(encoding="utf-8"))
        if not isinstance(loaded, list):
            raise ValueError("--regions-json must point to a JSON list")
        return loaded
    if args.region_bbox:
        x1, y1, x2, y2 = args.region_bbox
        return [
            {
                "toothCode": args.tooth_code,
                "bbox": [x1, y1, x2, y2],
                "score": args.region_score,
                "regionIndex": 0,
            }
        ]
    raise ValueError("grading smoke requires --regions-json or --region-bbox")


def _run_segmentation(image_path: Path, settings: Settings, assets: ModelAssets) -> dict[str, Any]:
    adapter = SegmentationModelAdapter(model_assets=assets, settings=settings)
    adapter.load()
    result = adapter.infer(image_path, [])
    print(
        json.dumps(
            {
                "singleImageInference": {
                    "module": "segmentation",
                    "implType": result["implType"],
                    "regionCount": len(result["regions"]),
                    "maskPixels": result["rawResult"]["maskPixels"],
                    "modelCode": result["rawResult"]["modelCode"],
                    "datasetVersion": result["rawResult"]["datasetVersion"],
                }
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return result


def _run_grading(image_path: Path, settings: Settings, assets: ModelAssets, regions: list[dict[str, Any]]) -> dict[str, Any]:
    adapter = GradingModelAdapter(model_assets=assets, settings=settings)
    adapter.load()
    result = adapter.infer(image_path, regions, [])
    print(
        json.dumps(
            {
                "singleImageInference": {
                    "module": "grading",
                    "implType": result["implType"],
                    "gradingLabel": result["gradingLabel"],
                    "confidenceScore": result["confidenceScore"],
                    "uncertaintyScore": result["uncertaintyScore"],
                    "modelCode": result["rawResult"]["modelCode"],
                    "datasetVersion": assets.grading_manifest.dataset_version,
                }
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return result


def _pytest_targets(module: str) -> list[str]:
    targets = ["tests/unit/test_model_runtime_loading.py"]
    if module == "segmentation":
        targets.extend(
            [
                "tests/unit/test_segmenter_adapter.py",
                "tests/unit/test_segmentation_pipeline.py",
            ]
        )
    else:
        targets.extend(
            [
                "tests/unit/test_grading_model_adapter.py",
                "tests/unit/test_grading_pipeline.py",
            ]
        )
    return targets


def _run_pytest(module: str) -> None:
    command = [sys.executable, "-m", "pytest", *_pytest_targets(module), "-q"]
    print(json.dumps({"pytest": {"command": command}}, ensure_ascii=False, indent=2))
    completed = subprocess.run(command, cwd=str(BACKEND_ROOT), check=False)
    if completed.returncode != 0:
        raise SystemExit(completed.returncode)


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Checkpoint smoke validation for segmentation/grading manifests.")
    parser.add_argument("--module", choices=["segmentation", "grading"], required=True)
    parser.add_argument("--image", required=True, help="Single test image path.")
    parser.add_argument("--regions-json", help="JSON file with grading regions.")
    parser.add_argument(
        "--region-bbox",
        nargs=4,
        type=int,
        metavar=("X1", "Y1", "X2", "Y2"),
        help="Single grading region bbox.",
    )
    parser.add_argument("--tooth-code", default="UNKNOWN")
    parser.add_argument("--region-score", type=float, default=0.9)
    parser.add_argument("--skip-pytest", action="store_true")
    return parser


def main() -> int:
    args = _parser().parse_args()
    image_path = Path(args.image).resolve()
    if not image_path.is_file():
        raise FileNotFoundError(f"image does not exist: {image_path}")

    settings = _settings(args.module)
    assets = ModelAssets(settings)
    _validate_module(args.module, assets)
    if args.module == "segmentation":
        _run_segmentation(image_path, settings, assets)
    else:
        regions = _load_regions(args)
        _run_grading(image_path, settings, assets, regions)

    if not args.skip_pytest:
        _run_pytest(args.module)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
