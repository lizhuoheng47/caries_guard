from __future__ import annotations

import argparse
import json
import shutil
import sys
from dataclasses import replace
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from app.core.config import Settings  # noqa: E402
from app.infra.model.model_assets import ModelAssets  # noqa: E402
from app.infra.model.model_registry import ModelRegistry  # noqa: E402
from app.pipelines.detection_pipeline import DetectionPipeline  # noqa: E402
from app.pipelines.grading_pipeline import GradingPipeline  # noqa: E402
from app.pipelines.inference_pipeline import InferencePipeline  # noqa: E402
from app.pipelines.quality_pipeline import QualityPipeline  # noqa: E402
from app.pipelines.segmentation_pipeline import SegmentationPipeline  # noqa: E402
from app.services.analysis_asset_service import AnalysisAssetService  # noqa: E402
from app.services.image_fetch_service import ImageFetchService  # noqa: E402
from app.services.risk_service import RiskService  # noqa: E402
from app.schemas.callback import VisualAsset  # noqa: E402
from app.core.hash_utils import file_md5  # noqa: E402


class LocalVisualAssetService:
    """Persist generated visuals locally for dependency-free pipeline checks."""

    def __init__(self, output_dir: Path) -> None:
        self.output_dir = output_dir

    def upload_visual(
        self,
        asset_type_code: str,
        org_id: int | None,
        case_no: str,
        task_no: str,
        model_version: str,
        image_id: int | None,
        local_path: str | Path,
        tooth_code: str | None = None,
    ) -> VisualAsset:
        del org_id, case_no, task_no, model_version
        source = Path(local_path)
        image_dir = self.output_dir / f"image_{image_id if image_id is not None else 'unknown'}"
        image_dir.mkdir(parents=True, exist_ok=True)
        destination = image_dir / source.name
        shutil.copy2(source, destination)
        return VisualAsset(
            asset_type_code=asset_type_code.strip().upper(),
            bucket_name="local-validation",
            object_key=str(destination.resolve()),
            content_type="image/png",
            related_image_id=image_id,
            tooth_code=tooth_code,
            file_size_bytes=destination.stat().st_size,
            md5=file_md5(destination),
            file_name=destination.name,
        )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Validate the Python analysis pipeline against a local image file.")
    parser.add_argument("--image", required=True, help="Path to a local JPG/PNG/DICOM image")
    parser.add_argument("--task-no", default="TASK-LOCAL-VALIDATION-001", help="Task number for the local run")
    parser.add_argument("--case-no", default="CASE-LOCAL-VALIDATION-001", help="Case number for the local run")
    parser.add_argument("--org-id", type=int, default=100001, help="Org id written into visual object keys")
    parser.add_argument("--patient-id", type=int, default=100001, help="Patient id used for the request envelope")
    parser.add_argument("--image-id", type=int, default=900001, help="Image id used for the request envelope")
    parser.add_argument("--image-type", default="BITEWING", help="Image type code")
    parser.add_argument("--model-version", default="caries-v1", help="Model version stamp")
    parser.add_argument("--dump-path", help="Optional file path to save the callback payload JSON")
    parser.add_argument(
        "--real-segmentation",
        action="store_true",
        help="Run the exported DC1000 TorchScript segmenter; other unfinished modules remain explicit heuristics",
    )
    parser.add_argument("--device", default="cuda:0", help="Torch device used with --real-segmentation")
    parser.add_argument(
        "--output-dir",
        help="Directory for locally persisted mask, overlay, heatmap, and result JSON",
    )
    parser.add_argument("--full-json", action="store_true", help="Also print the complete callback payload")
    return parser


def main() -> int:
    args = build_parser().parse_args()
    image_path = Path(args.image).resolve()
    if not image_path.is_file():
        raise SystemExit(f"image does not exist: {image_path}")

    settings = Settings()
    if args.real_segmentation:
        settings = replace(
            settings,
            mq_worker_enabled=False,
            model_quality_enabled=True,
            model_quality_impl_type="HEURISTIC",
            model_tooth_detect_enabled=True,
            model_tooth_detect_impl_type="HEURISTIC",
            model_segmentation_enabled=True,
            model_segmentation_impl_type="ML_MODEL",
            model_grading_enabled=True,
            model_grading_impl_type="HEURISTIC",
            model_risk_enabled=True,
            model_risk_impl_type="HEURISTIC",
            model_device=args.device,
            strict_model_startup_validation=True,
        )
    output_dir = (
        Path(args.output_dir).resolve()
        if args.output_dir
        else (PROJECT_ROOT.parent / "artifacts" / "pipeline_validation" / args.task_no).resolve()
    )
    output_dir.mkdir(parents=True, exist_ok=True)
    settings = replace(settings, temp_dir=str(output_dir / "_workspace"))
    model_assets = ModelAssets(settings)
    model_registry = ModelRegistry(settings, model_assets)
    model_registry.startup()
    pipeline = InferencePipeline(
        settings=settings,
        image_fetch_service=ImageFetchService(settings, None),
        visual_asset_service=LocalVisualAssetService(output_dir),
        model_registry=model_registry,
        model_assets=model_assets,
        quality_pipeline=QualityPipeline(model_registry, settings),
        detection_pipeline=DetectionPipeline(model_registry, settings),
        segmentation_pipeline=SegmentationPipeline(model_registry, settings, model_assets),
        grading_pipeline=GradingPipeline(model_registry, settings, model_assets),
        risk_service=RiskService(settings),
        ai_runtime_repository=None,
        analysis_asset_service=AnalysisAssetService(settings, model_assets),
    )
    raw_task = {
        "taskNo": args.task_no,
        "caseNo": args.case_no,
        "orgId": args.org_id,
        "patientId": args.patient_id,
        "modelVersion": args.model_version,
        "images": [
            {
                "imageId": args.image_id,
                "imageTypeCode": args.image_type,
                "localStoragePath": str(image_path),
            }
        ],
    }

    exit_code = 0
    try:
        payload = pipeline.run(raw_task)
    except Exception as exc:
        payload = pipeline.build_failure_payload(raw_task, exc)
        exit_code = 2

    rendered = json.dumps(payload, ensure_ascii=True, indent=2)
    if args.full_json:
        print(rendered)
    dump_path = Path(args.dump_path).resolve() if args.dump_path else output_dir / "result.json"
    dump_path.parent.mkdir(parents=True, exist_ok=True)
    dump_path.write_text(rendered, encoding="utf-8")
    raw_result = payload.get("rawResultJson") if isinstance(payload.get("rawResultJson"), dict) else {}
    modules = raw_result.get("moduleStatus") if isinstance(raw_result.get("moduleStatus"), dict) else {}
    print(json.dumps(
        {
            "taskStatusCode": payload.get("taskStatusCode"),
            "inferenceMillis": payload.get("inferenceMillis"),
            "segmentation": {
                "implType": raw_result.get("segmentationImplType"),
                "modelCode": (modules.get("segmentation") or {}).get("modelCode"),
                "ready": (modules.get("segmentation") or {}).get("ready"),
            },
            "supportingModules": {
                "quality": raw_result.get("qualityImplType"),
                "toothDetection": raw_result.get("toothDetectionImplType"),
                "grading": raw_result.get("gradingImplType"),
            },
            "lesionRegionCount": len(raw_result.get("lesionResults") or []),
            "needsReview": payload.get("needsReview"),
            "validationOutputDir": str(output_dir),
            "resultJson": str(dump_path),
            "visualAssets": [
                item.get("objectKey")
                for item in (payload.get("visualAssets") or [])
                if isinstance(item, dict)
            ],
            "errorCode": payload.get("errorCode"),
            "errorMessage": payload.get("errorMessage"),
        },
        ensure_ascii=False,
        indent=2,
    ))
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
