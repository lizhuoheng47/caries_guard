from __future__ import annotations

import time
import uuid
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw

from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.core.logging import get_logger
from app.core.time_utils import local_naive_iso_now
from app.schemas.base import dump_camel
from app.schemas.callback import (
    AnalysisCallbackPayload,
    AssetRef,
    FailureCallbackPayload,
    LesionResult,
    Summary,
    ToothDetection,
    VisualAsset,
)
from app.schemas.request import AnalyzeRequest, ImageInput
from app.services.image_fetch_service import FetchedImage, ImageFetchService, TaskWorkspace
from app.services.quality_service import QualityService
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService

log = get_logger("cariesguard-ai.pipeline")


class InferencePipeline:
    def __init__(
        self,
        settings: Settings,
        image_fetch_service: ImageFetchService,
        visual_asset_service: VisualAssetService | None,
        quality_service: QualityService,
        risk_service: RiskService,
    ) -> None:
        self.settings = settings
        self.image_fetch_service = image_fetch_service
        self.visual_asset_service = visual_asset_service
        self.quality_service = quality_service
        self.risk_service = risk_service

    def run(self, raw_task: dict[str, Any]) -> dict[str, Any]:
        task = self._parse_task(raw_task)
        started_at = local_naive_iso_now()
        started = time.perf_counter()
        trace_id = task.trace_id or raw_task.get("traceId") or f"py-{uuid.uuid4().hex[:12]}"
        model_version = task.model_version or self.settings.model_version
        log.info("pipeline started taskNo=%s traceId=%s images=%s", task.task_no, trace_id, len(task.images))

        with TaskWorkspace(self.settings, task.task_no) as workspace:
            fetched_images = self._fetch_images(task, workspace)
            visual_assets = self._create_visual_assets(task, fetched_images, workspace)
            quality_results = [self.quality_service.check(image) for image in task.images]
            risk_assessment = self.risk_service.assess(task.patient_profile)

        completed_at = local_naive_iso_now()
        inference_millis = int((time.perf_counter() - started) * 1000)
        first_image_id = task.images[0].image_id if task.images else None
        summary = Summary(
            overall_highest_severity="C1",
            uncertainty_score=0.1,
            review_suggested_flag="0",
            teeth_count=2,
        )
        raw_result_json = {
            "pipelineVersion": "mock-1",
            "mode": self.settings.app_mode,
            "qualityCheckResults": [dump_camel(item) for item in quality_results],
            "toothDetections": [
                dump_camel(ToothDetection(image_id=first_image_id, tooth_code="16")),
                dump_camel(ToothDetection(image_id=first_image_id, tooth_code="26", bbox=[220, 64, 340, 180], detection_score=0.93)),
            ],
            "lesionResults": [
                dump_camel(
                    LesionResult(
                        image_id=first_image_id,
                        tooth_code="16",
                        severity_code="C1",
                        uncertainty_score=0.1,
                        lesion_area_px=512,
                        lesion_area_ratio=0.01,
                        mask_asset=self._asset_ref(visual_assets, "MASK"),
                        overlay_asset=self._asset_ref(visual_assets, "OVERLAY"),
                    )
                )
            ],
            "riskAssessment": dump_camel(risk_assessment),
            "visualAssets": [dump_camel(item) for item in visual_assets],
            "note": "mock visual assets are for integration verification only",
        }
        payload = AnalysisCallbackPayload(
            task_no=task.task_no,
            task_status_code="SUCCESS",
            started_at=started_at,
            completed_at=completed_at,
            model_version=model_version,
            summary=summary,
            raw_result_json=raw_result_json,
            visual_assets=visual_assets,
            risk_assessment=risk_assessment,
            error_message=None,
            trace_id=trace_id,
            inference_millis=inference_millis,
            uncertainty_score=0.1,
        )
        log.info("pipeline completed taskNo=%s traceId=%s millis=%s", task.task_no, trace_id, inference_millis)
        return dump_camel(payload)

    def build_failure_payload(self, raw_task: dict[str, Any], exc: Exception) -> dict[str, Any]:
        now = local_naive_iso_now()
        task_no = str(raw_task.get("taskNo") or raw_task.get("task_no") or "UNKNOWN")
        trace_id = str(raw_task.get("traceId") or raw_task.get("trace_id") or f"py-{uuid.uuid4().hex[:12]}")
        code = exc.code if isinstance(exc, BusinessException) else "C9999"
        payload = FailureCallbackPayload(
            task_no=task_no,
            task_status_code="FAILED",
            started_at=now,
            completed_at=now,
            model_version=str(raw_task.get("modelVersion") or self.settings.model_version),
            raw_result_json={"errorCode": code, "errorType": exc.__class__.__name__},
            error_message=str(exc),
            trace_id=trace_id,
            inference_millis=0,
        )
        return dump_camel(payload)

    def _parse_task(self, raw_task: dict[str, Any]) -> AnalyzeRequest:
        payload = raw_task.get("payload") if isinstance(raw_task.get("payload"), dict) else raw_task
        if not payload.get("traceId") and raw_task.get("traceId"):
            payload = {**payload, "traceId": raw_task.get("traceId")}
        task = AnalyzeRequest.model_validate(payload)
        task.raw_payload = raw_task
        return task

    def _fetch_images(self, task: AnalyzeRequest, workspace: Path) -> list[FetchedImage]:
        if not self.settings.download_images:
            return []
        fetched: list[FetchedImage] = []
        for image in task.images:
            fetched.append(self.image_fetch_service.download(image, workspace))
        return fetched

    def _create_visual_assets(
        self,
        task: AnalyzeRequest,
        fetched_images: list[FetchedImage],
        workspace: Path,
    ) -> list[VisualAsset]:
        if not fetched_images or self.visual_asset_service is None:
            return []
        first_image = fetched_images[0]
        case_no = task.case_no or f"CASE{task.case_id or task.task_no}"
        image_id = first_image.image_id
        generated_dir = workspace / "visual"
        generated_dir.mkdir(parents=True, exist_ok=True)
        mask_path = generated_dir / f"mask_{image_id or 'unknown'}_16.png"
        overlay_path = generated_dir / f"overlay_{image_id or 'unknown'}_16.png"
        heatmap_path = generated_dir / f"heatmap_{image_id or 'unknown'}.png"
        self._draw_mock_assets(first_image.path, mask_path, overlay_path, heatmap_path)
        return [
            self.visual_asset_service.upload_visual("MASK", case_no, image_id, mask_path, tooth_code="16"),
            self.visual_asset_service.upload_visual("OVERLAY", case_no, image_id, overlay_path, tooth_code="16"),
            self.visual_asset_service.upload_visual("HEATMAP", case_no, image_id, heatmap_path),
        ]

    def _draw_mock_assets(self, image_path: Path, mask_path: Path, overlay_path: Path, heatmap_path: Path) -> None:
        try:
            base = Image.open(image_path).convert("RGB")
        except Exception:
            base = Image.new("RGB", (512, 256), color=(245, 245, 245))
        width, height = base.size
        box = self._stable_box(width, height)

        mask = Image.new("RGBA", base.size, color=(0, 0, 0, 0))
        draw_mask = ImageDraw.Draw(mask)
        draw_mask.ellipse(box, fill=(255, 255, 255, 220))
        mask.save(mask_path)

        overlay = base.copy().convert("RGBA")
        draw_overlay = ImageDraw.Draw(overlay)
        draw_overlay.ellipse(box, outline=(255, 0, 0, 255), width=max(2, width // 300))
        draw_overlay.rectangle([box[0], max(0, box[1] - 24), box[0] + 90, box[1]], fill=(255, 0, 0, 180))
        draw_overlay.text((box[0] + 6, max(0, box[1] - 21)), "C1 16", fill=(255, 255, 255, 255))
        overlay.convert("RGB").save(overlay_path)

        heatmap = Image.new("RGBA", base.size, color=(0, 0, 0, 0))
        draw_heatmap = ImageDraw.Draw(heatmap)
        draw_heatmap.ellipse(box, fill=(255, 80, 0, 150))
        Image.alpha_composite(base.convert("RGBA"), heatmap).convert("RGB").save(heatmap_path)

    @staticmethod
    def _stable_box(width: int, height: int) -> list[int]:
        x1 = max(0, int(width * 0.35))
        y1 = max(0, int(height * 0.35))
        x2 = min(width - 1, int(width * 0.55))
        y2 = min(height - 1, int(height * 0.65))
        return [x1, y1, x2, y2]

    @staticmethod
    def _asset_ref(assets: list[VisualAsset], asset_type_code: str) -> AssetRef | None:
        for asset in assets:
            if asset.asset_type_code == asset_type_code:
                return AssetRef(bucket_name=asset.bucket_name, object_key=asset.object_key)
        return None

