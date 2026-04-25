from __future__ import annotations

import argparse
import json
import sys
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
    return parser


def main() -> int:
    args = build_parser().parse_args()
    image_path = Path(args.image).resolve()
    if not image_path.is_file():
        raise SystemExit(f"image does not exist: {image_path}")

    settings = Settings()
    model_assets = ModelAssets(settings)
    model_registry = ModelRegistry(settings, model_assets)
    model_registry.startup()
    pipeline = InferencePipeline(
        settings=settings,
        image_fetch_service=ImageFetchService(settings, None),
        visual_asset_service=None,
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
    print(rendered)
    if args.dump_path:
        dump_path = Path(args.dump_path).resolve()
        dump_path.parent.mkdir(parents=True, exist_ok=True)
        dump_path.write_text(rendered, encoding="utf-8")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
