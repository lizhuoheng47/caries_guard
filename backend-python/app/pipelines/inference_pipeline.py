from __future__ import annotations

import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.exceptions import AnalysisRuntimeException, BusinessException
from app.core.image_utils import LoadedImage, load_image
from app.core.logging import get_logger
from app.core.time_utils import local_naive_iso_now
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline, GradingResult
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.segmentation_pipeline import SegmentationPipeline, SegmentationResult
from app.repositories.ai_runtime_repository import AiRuntimeRepository
from app.schemas.analysis import ImageRuntimeRecord, ModuleAssetStatus
from app.schemas.base import dump_camel
from app.schemas.callback import (
    AnalysisCallbackPayload,
    FailureCallbackPayload,
    Summary,
    VisualAsset,
)
from app.schemas.request import AnalyzeRequest, ImageInput
from app.services.analysis_asset_service import AnalysisAssetService
from app.services.image_fetch_service import FetchedImage, ImageFetchService, TaskWorkspace
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService

log = get_logger("cariesguard-ai.pipeline")


@dataclass(frozen=True)
class RuntimeImage:
    request: ImageInput
    fetched: FetchedImage
    loaded: LoadedImage


class InferencePipeline:
    def __init__(
        self,
        settings: Settings,
        image_fetch_service: ImageFetchService,
        visual_asset_service: VisualAssetService | None,
        model_registry: ModelRegistry,
        model_assets: ModelAssets,
        quality_pipeline: QualityPipeline,
        detection_pipeline: DetectionPipeline,
        segmentation_pipeline: SegmentationPipeline,
        grading_pipeline: GradingPipeline,
        risk_service: RiskService,
        ai_runtime_repository: AiRuntimeRepository | None = None,
        analysis_asset_service: AnalysisAssetService | None = None,
    ) -> None:
        self.settings = settings
        self.image_fetch_service = image_fetch_service
        self.visual_asset_service = visual_asset_service
        self.model_registry = model_registry
        self.model_assets = model_assets
        self.quality_pipeline = quality_pipeline
        self.detection_pipeline = detection_pipeline
        self.segmentation_pipeline = segmentation_pipeline
        self.grading_pipeline = grading_pipeline
        self.risk_service = risk_service
        self.ai_runtime_repository = ai_runtime_repository
        self.analysis_asset_service = analysis_asset_service or AnalysisAssetService(settings, model_assets)
        self.pipeline_version = "analysis-v2"

    def run(self, raw_task: dict[str, Any]) -> dict[str, Any]:
        task = self._parse_task(raw_task)
        trace_id = self._trace_id(raw_task, task)
        started_at = local_naive_iso_now()
        started = time.perf_counter()
        runtime_job = self._ensure_runtime_job(task, trace_id)
        log.info("analysis pipeline started taskNo=%s traceId=%s images=%s", task.task_no, trace_id, len(task.images))

        with TaskWorkspace(self.settings, task.task_no) as workspace:
            runtime_images = self._prepare_images(task, workspace, runtime_job.get("id"))
            snapshot = self.analysis_asset_service.require_ready_modules(
                ["quality", "tooth_detect", "segmentation", "grading"],
                self.model_registry,
            )
            image_results, visual_assets, lesion_results, tooth_detections = self._process_images(
                task=task,
                runtime_images=runtime_images,
                workspace=workspace,
                runtime_job_id=runtime_job.get("id"),
            )
            aggregate = self._aggregate_results(image_results)
            risk_assessment = self._build_risk_assessment(
                task=task,
                aggregate=aggregate,
                lesion_results=lesion_results,
                tooth_detections=tooth_detections,
                image_results=image_results,
            )
            raw_result_json = self._build_success_raw_result(
                snapshot=snapshot.modules,
                image_results=image_results,
                lesion_results=lesion_results,
                tooth_detections=tooth_detections,
                visual_assets=visual_assets,
                risk_assessment=risk_assessment,
                runtime_job=runtime_job,
                aggregate=aggregate,
            )

        completed_at = local_naive_iso_now()
        inference_millis = int((time.perf_counter() - started) * 1000)
        summary = Summary(
            overall_highest_severity=aggregate["gradingLabel"],
            uncertainty_score=aggregate["uncertaintyScore"],
            review_suggested_flag="1" if aggregate["needsReview"] else "0",
            teeth_count=len(tooth_detections),
        )
        self._finish_job_success(runtime_job.get("id"), raw_result_json)
        payload = AnalysisCallbackPayload(
            task_no=task.task_no,
            task_status_code="SUCCESS",
            started_at=started_at,
            completed_at=completed_at,
            model_version=task.model_version or self.settings.model_version,
            summary=summary,
            raw_result_json=raw_result_json,
            visual_assets=self._callback_visual_assets(visual_assets, task.task_no, trace_id),
            risk_assessment=risk_assessment,
            trace_id=trace_id,
            inference_millis=inference_millis,
            grading_label=aggregate["gradingLabel"],
            confidence_score=aggregate["confidenceScore"],
            needs_review=aggregate["needsReview"],
            uncertainty_score=aggregate["uncertaintyScore"],
            risk_level=risk_assessment.risk_level_code or risk_assessment.overall_risk_level_code,
            risk_factors=risk_assessment.risk_factors or [],
            review_reason=aggregate["reviewReason"],
            knowledge_version=None,
            evidence_refs=[],
            doctor_review_required_reason=aggregate["reviewReason"] if aggregate["needsReview"] else None,
        )
        log.info("analysis pipeline completed taskNo=%s traceId=%s millis=%s", task.task_no, trace_id, inference_millis)
        return dump_camel(payload)

    def build_failure_payload(self, raw_task: dict[str, Any], exc: Exception) -> dict[str, Any]:
        task = self._parse_task_soft(raw_task)
        trace_id = self._trace_id(raw_task, task)
        now = local_naive_iso_now()
        snapshot = self.analysis_asset_service.runtime_snapshot(self.model_registry)
        runtime_job = self._ensure_runtime_job(task, trace_id)
        missing_items = []
        details = {}
        code = exc.code if isinstance(exc, BusinessException) else "C9999"
        if isinstance(exc, AnalysisRuntimeException):
            missing_items = list(exc.missing_items)
            details = dict(exc.details)
        if not missing_items:
            missing_items = [dump_camel(item) for item in snapshot.missing_items]
        missing_items.extend(self._runtime_dependency_items(exc))
        raw_result_json = self._build_failure_raw_result(
            snapshot.modules,
            runtime_job=runtime_job,
            error_code=code,
            error_message=str(exc),
            error_type=exc.__class__.__name__,
            missing_items=missing_items,
            details=details,
        )
        self._finish_job_failure(runtime_job.get("id"), raw_result_json, str(exc))
        payload = FailureCallbackPayload(
            task_no=task.task_no,
            task_status_code="FAILED",
            started_at=now,
            completed_at=now,
            model_version=task.model_version or self.settings.model_version,
            raw_result_json=raw_result_json,
            error_code=code,
            error_message=str(exc),
            trace_id=trace_id,
            inference_millis=0,
        )
        return dump_camel(payload)

    def _prepare_images(self, task: AnalyzeRequest, workspace: Path, job_id: int | None) -> list[RuntimeImage]:
        runtime_images: list[RuntimeImage] = []
        for image in task.images:
            fetched = self.image_fetch_service.download(image, workspace)
            loaded = load_image(fetched.path)
            runtime_images.append(RuntimeImage(request=image, fetched=fetched, loaded=loaded))
            if job_id is not None and self.ai_runtime_repository is not None:
                self.ai_runtime_repository.upsert_job_image(
                    job_id,
                    image.image_id,
                    attachment_id=image.attachment_id,
                    image_type_code=image.image_type_code,
                    bucket_name=image.bucket_name,
                    object_key=image.object_key,
                    access_url=image.access_url,
                    download_status_code="DOWNLOADED",
                    local_cache_path=str(fetched.path),
                    result_json=dump_camel(
                        ImageRuntimeRecord(
                            image_id=image.image_id,
                            image_type_code=image.image_type_code,
                            source_format=loaded.source_format,
                            width_px=loaded.width,
                            height_px=loaded.height,
                            channels=loaded.channels,
                            local_path=str(fetched.path),
                            bucket_name=image.bucket_name,
                            object_key=image.object_key,
                        )
                    ),
                )
        return runtime_images

    def _process_images(
        self,
        *,
        task: AnalyzeRequest,
        runtime_images: list[RuntimeImage],
        workspace: Path,
        runtime_job_id: int | None,
    ) -> tuple[list[dict[str, Any]], list[VisualAsset], list[dict[str, Any]], list[dict[str, Any]]]:
        visual_assets: list[VisualAsset] = []
        lesion_results: list[dict[str, Any]] = []
        image_results: list[dict[str, Any]] = []
        tooth_detections_all: list[dict[str, Any]] = []

        image_inputs = [item.request for item in runtime_images]
        fetched_images = [item.fetched for item in runtime_images]
        quality_by_image: dict[int | None, Any] = {}
        for runtime_image in runtime_images:
            result = self.quality_pipeline.check(runtime_image.request, runtime_image.fetched.path)
            quality_by_image[runtime_image.request.image_id] = result

        tooth_detections_schema = self.detection_pipeline.detect_all(image_inputs, fetched_images)
        for item in tooth_detections_schema:
            tooth_detections_all.append(dump_camel(item))

        for runtime_image in runtime_images:
            image_id = runtime_image.request.image_id
            detections = [item for item in tooth_detections_schema if item.image_id in {None, image_id}]
            output_dir = workspace / "outputs" / str(image_id or "unknown")
            segmentation = self.segmentation_pipeline.segment(
                runtime_image.request,
                runtime_image.fetched.path,
                detections,
                output_dir,
            )
            grading = self.grading_pipeline.grade(
                runtime_image.request,
                runtime_image.fetched.path,
                segmentation.regions,
                detections,
                [quality_by_image.get(image_id)] if quality_by_image.get(image_id) is not None else [],
            )
            uploaded = self._upload_visuals(task, runtime_image.request, segmentation)
            visual_assets.extend(uploaded)
            lesion_results.extend(self._lesions_for_image(runtime_image, segmentation, grading, uploaded))
            image_result = self._image_result(
                runtime_image=runtime_image,
                segmentation=segmentation,
                grading=grading,
                quality_result=quality_by_image.get(image_id),
                detections=[dump_camel(item) for item in detections],
                visual_assets=uploaded,
            )
            image_results.append(image_result)
            if runtime_job_id is not None and self.ai_runtime_repository is not None:
                self.ai_runtime_repository.upsert_job_image(
                    runtime_job_id,
                    image_id,
                    quality_status_code=getattr(quality_by_image.get(image_id), "check_result_code", None),
                    grading_label=grading.grading_label,
                    uncertainty_score=grading.uncertainty_score,
                    result_json=image_result,
                )
        return image_results, visual_assets, lesion_results, tooth_detections_all

    def _upload_visuals(self, task: AnalyzeRequest, image: ImageInput, segmentation: SegmentationResult) -> list[VisualAsset]:
        if self.visual_asset_service is None:
            raise AnalysisRuntimeException("M5102", "visual asset service is unavailable")
        assets: list[VisualAsset] = []
        tooth_code = self._first_region_tooth(segmentation.regions)
        for asset_type, path in (
            ("MASK", segmentation.mask_path),
            ("OVERLAY", segmentation.overlay_path),
            ("HEATMAP", segmentation.heatmap_path),
        ):
            assets.append(
                self.visual_asset_service.upload_visual(
                    asset_type_code=asset_type,
                    org_id=task.org_id,
                    case_no=task.case_no or task.task_no,
                    task_no=task.task_no,
                    model_version=task.model_version or self.settings.model_version,
                    image_id=image.image_id,
                    local_path=path,
                    tooth_code=tooth_code if asset_type != "HEATMAP" else None,
                )
            )
        return assets

    def _aggregate_results(self, image_results: list[dict[str, Any]]) -> dict[str, Any]:
        if not image_results:
            raise AnalysisRuntimeException("M5103", "no image results were produced")
        selected = max(
            image_results,
            key=lambda item: (
                self.model_assets.severity_rank(item.get("gradingLabel")),
                float(item.get("confidenceScore") or 0.0),
            ),
        )
        review_reasons = [str(item.get("reviewReason")) for item in image_results if item.get("reviewReason")]
        return {
            "gradingLabel": str(selected.get("gradingLabel") or self.model_assets.grading_labels()[0]),
            "confidenceScore": float(selected.get("confidenceScore") or 0.0),
            "uncertaintyScore": max(float(item.get("uncertaintyScore") or 0.0) for item in image_results),
            "needsReview": any(bool(item.get("needsReview")) for item in image_results),
            "reviewReason": ",".join(dict.fromkeys(review_reasons)) if review_reasons else None,
        }

    def _build_risk_assessment(
        self,
        *,
        task: AnalyzeRequest,
        aggregate: dict[str, Any],
        lesion_results: list[dict[str, Any]],
        tooth_detections: list[dict[str, Any]],
        image_results: list[dict[str, Any]],
    ):
        quality_statuses = [str(item.get("qualityStatusCode") or "PASS") for item in image_results]
        overall_quality = "FAIL" if "FAIL" in quality_statuses else ("WARN" if "WARN" in quality_statuses else "PASS")
        return self.risk_service.assess(
            task.patient_profile,
            grading_label=aggregate["gradingLabel"],
            uncertainty_score=float(aggregate["uncertaintyScore"]),
            needs_review=bool(aggregate["needsReview"]),
            segmentation_regions=lesion_results,
            tooth_detection_count=len(tooth_detections),
            quality_status_code=overall_quality,
        )

    def _build_success_raw_result(
        self,
        *,
        snapshot: dict[str, ModuleAssetStatus],
        image_results: list[dict[str, Any]],
        lesion_results: list[dict[str, Any]],
        tooth_detections: list[dict[str, Any]],
        visual_assets: list[VisualAsset],
        risk_assessment: Any,
        runtime_job: dict[str, Any],
        aggregate: dict[str, Any],
    ) -> dict[str, Any]:
        return {
            "pipelineVersion": self.pipeline_version,
            "mode": self.settings.ai_runtime_mode,
            "qualityMode": self._module_mode(snapshot.get("quality")),
            "qualityImplType": self.quality_pipeline.get_last_impl_type(),
            "toothDetectionMode": self._module_mode(snapshot.get("tooth_detect")),
            "toothDetectionImplType": self.detection_pipeline.get_last_impl_type(),
            "segmentationMode": self._module_mode(snapshot.get("segmentation")),
            "segmentationImplType": self.segmentation_pipeline.get_last_impl_type(),
            "gradingMode": self._module_mode(snapshot.get("grading")),
            "gradingImplType": self.grading_pipeline.get_last_impl_type(),
            "gradingLabel": aggregate["gradingLabel"],
            "confidenceScore": aggregate["confidenceScore"],
            "uncertaintyMode": "real",
            "uncertaintyImplType": "SOFTMAX_MARGIN",
            "uncertaintyScore": aggregate["uncertaintyScore"],
            "needsReview": aggregate["needsReview"],
            "reviewReason": aggregate["reviewReason"],
            "visualAssets": [dump_camel(item) for item in visual_assets],
            "moduleStatus": {name: dump_camel(status) for name, status in snapshot.items()},
            "aiRuntimeJobId": runtime_job.get("id"),
            "aiRuntimeJobNo": runtime_job.get("job_no"),
            "toothDetections": tooth_detections,
            "lesionResults": lesion_results,
            "imageResults": image_results,
            "riskAssessment": dump_camel(risk_assessment),
        }

    def _build_failure_raw_result(
        self,
        snapshot: dict[str, ModuleAssetStatus],
        *,
        runtime_job: dict[str, Any],
        error_code: str,
        error_message: str,
        error_type: str,
        missing_items: list[dict[str, Any]],
        details: dict[str, Any],
    ) -> dict[str, Any]:
        return {
            "pipelineVersion": self.pipeline_version,
            "mode": self.settings.ai_runtime_mode,
            "qualityMode": self._module_mode(snapshot.get("quality")),
            "qualityImplType": self._module_impl(snapshot.get("quality"), self.quality_pipeline.get_last_impl_type()),
            "toothDetectionMode": self._module_mode(snapshot.get("tooth_detect")),
            "toothDetectionImplType": self._module_impl(snapshot.get("tooth_detect"), self.detection_pipeline.get_last_impl_type()),
            "segmentationMode": self._module_mode(snapshot.get("segmentation")),
            "segmentationImplType": self._module_impl(snapshot.get("segmentation"), self.segmentation_pipeline.get_last_impl_type()),
            "gradingMode": self._module_mode(snapshot.get("grading")),
            "gradingImplType": self._module_impl(snapshot.get("grading"), self.grading_pipeline.get_last_impl_type()),
            "gradingLabel": None,
            "confidenceScore": None,
            "uncertaintyMode": "failed",
            "uncertaintyImplType": error_type,
            "uncertaintyScore": None,
            "needsReview": True,
            "visualAssets": [],
            "aiRuntimeJobId": runtime_job.get("id"),
            "aiRuntimeJobNo": runtime_job.get("job_no"),
            "errorCode": error_code,
            "errorMessage": error_message,
            "missingItems": missing_items,
            "moduleStatus": {name: dump_camel(status) for name, status in snapshot.items()},
            "details": details,
        }

    def _image_result(
        self,
        *,
        runtime_image: RuntimeImage,
        segmentation: SegmentationResult,
        grading: GradingResult,
        quality_result: Any,
        detections: list[dict[str, Any]],
        visual_assets: list[VisualAsset],
    ) -> dict[str, Any]:
        review_reason = None
        if grading.needs_review:
            review_reason = "HIGH_UNCERTAINTY"
        if quality_result is not None and getattr(quality_result, "check_result_code", "PASS") != "PASS":
            review_reason = "QUALITY_ALERT" if review_reason is None else f"{review_reason},QUALITY_ALERT"
        return {
            "imageId": runtime_image.request.image_id,
            "sourceFormat": runtime_image.loaded.source_format,
            "widthPx": runtime_image.loaded.width,
            "heightPx": runtime_image.loaded.height,
            "qualityStatusCode": getattr(quality_result, "check_result_code", None),
            "qualityScore": getattr(quality_result, "quality_score_float", None),
            "gradingLabel": grading.grading_label,
            "confidenceScore": grading.confidence_score,
            "uncertaintyScore": grading.uncertainty_score,
            "needsReview": grading.needs_review,
            "reviewReason": review_reason,
            "detections": detections,
            "segmentationRegions": segmentation.regions,
            "visualAssets": [dump_camel(item) for item in visual_assets],
            "gradingRawResult": grading.raw_result,
        }

    def _lesions_for_image(
        self,
        runtime_image: RuntimeImage,
        segmentation: SegmentationResult,
        grading: GradingResult,
        visual_assets: list[VisualAsset],
    ) -> list[dict[str, Any]]:
        candidates = grading.raw_result.get("candidates") if isinstance(grading.raw_result, dict) else []
        by_index: dict[int, dict[str, Any]] = {}
        if isinstance(candidates, list):
            for item in candidates:
                if not isinstance(item, dict):
                    continue
                try:
                    by_index[int(item.get("regionIndex") or 0)] = item
                except (TypeError, ValueError):
                    continue
        lesions: list[dict[str, Any]] = []
        image_area = max(runtime_image.loaded.width * runtime_image.loaded.height, 1)
        for index, region in enumerate(segmentation.regions):
            bbox = region.get("bbox")
            polygon = region.get("polygon")
            if not isinstance(bbox, list) or len(bbox) != 4 or not isinstance(polygon, list) or len(polygon) < 3:
                raise AnalysisRuntimeException(
                    "M5104",
                    "segmentation output missing bbox/polygon geometry",
                )
            x1, y1, x2, y2 = [int(value) for value in bbox]
            lesion_area = max(0, x2 - x1) * max(0, y2 - y1)
            candidate = by_index.get(index, {})
            lesions.append(
                {
                    "imageId": runtime_image.request.image_id,
                    "toothCode": region.get("toothCode") or region.get("tooth_code"),
                    "severityCode": candidate.get("severityLabel") or grading.grading_label,
                    "confidenceScore": candidate.get("severityScore") or grading.confidence_score,
                    "uncertaintyScore": grading.uncertainty_score,
                    "bbox": bbox,
                    "polygon": polygon,
                    "lesionArea": lesion_area,
                    "lesionAreaPx": lesion_area,
                    "lesionAreaRatio": round(float(lesion_area) / float(image_area), 6),
                    "gradingLabel": candidate.get("severityLabel") or grading.grading_label,
                    "maskAsset": self._asset_ref(visual_assets, "MASK"),
                    "overlayAsset": self._asset_ref(visual_assets, "OVERLAY"),
                    "heatmapAsset": self._asset_ref(visual_assets, "HEATMAP"),
                }
            )
        return lesions

    @staticmethod
    def _asset_ref(assets: list[VisualAsset], asset_type_code: str) -> dict[str, Any] | None:
        for asset in assets:
            if asset.asset_type_code == asset_type_code:
                return dump_camel(asset)
        return None

    def _callback_visual_assets(
        self,
        assets: list[VisualAsset],
        task_no: str,
        trace_id: str,
    ) -> list[VisualAsset]:
        mode = (self.settings.callback_visual_asset_mode or "metadata").strip().lower()
        if mode in {"legacy-empty", "legacy_empty", "empty", "none"}:
            log.info(
                "suppressing top-level callback visual assets taskNo=%s traceId=%s mode=%s count=%s",
                task_no,
                trace_id,
                mode,
                len(assets),
            )
            return []
        return assets

    @staticmethod
    def _first_region_tooth(regions: list[dict[str, Any]]) -> str | None:
        for region in regions:
            tooth = region.get("toothCode") or region.get("tooth_code")
            if tooth:
                return str(tooth)
        return None

    def _ensure_runtime_job(self, task: AnalyzeRequest, trace_id: str) -> dict[str, Any]:
        if self.ai_runtime_repository is None:
            return {}
        existing = self.ai_runtime_repository.get_latest_infer_job(
            task.task_no,
            trace_id=trace_id,
            open_only=True,
        )
        if existing:
            return existing
        return self.ai_runtime_repository.create_infer_job(
            java_task_no=task.task_no,
            model_version=task.model_version or self.settings.model_version,
            trace_id=trace_id,
            case_no=task.case_no,
            request_json=task.model_dump(by_alias=True, exclude_none=True),
            org_id=task.org_id,
        )

    def _finish_job_success(self, job_id: int | None, result_json: dict[str, Any]) -> None:
        if job_id is None or self.ai_runtime_repository is None:
            return
        self.ai_runtime_repository.finish_infer_job(job_id, "SUCCESS", result_json=result_json)

    def _finish_job_failure(self, job_id: int | None, result_json: dict[str, Any], error_message: str) -> None:
        if job_id is None or self.ai_runtime_repository is None:
            return
        self.ai_runtime_repository.finish_infer_job(
            job_id,
            "FAILED",
            result_json=result_json,
            error_message=error_message[:1000],
        )

    def _parse_task(self, raw_task: dict[str, Any]) -> AnalyzeRequest:
        payload = raw_task.get("payload") if isinstance(raw_task.get("payload"), dict) else raw_task
        parsed = AnalyzeRequest.model_validate(payload)
        if not parsed.images:
            raise AnalysisRuntimeException("M5105", "analysis request images are required")
        return parsed

    def _parse_task_soft(self, raw_task: dict[str, Any]) -> AnalyzeRequest:
        payload = raw_task.get("payload") if isinstance(raw_task.get("payload"), dict) else raw_task
        task_no = str(payload.get("taskNo") or payload.get("task_no") or raw_task.get("taskNo") or "UNKNOWN")
        model_version = str(payload.get("modelVersion") or payload.get("model_version") or self.settings.model_version)
        return AnalyzeRequest(task_no=task_no, model_version=model_version, images=[])

    @staticmethod
    def _trace_id(raw_task: dict[str, Any], task: AnalyzeRequest) -> str:
        payload = raw_task.get("payload") if isinstance(raw_task.get("payload"), dict) else raw_task
        return str(task.trace_id or payload.get("traceId") or raw_task.get("traceId") or f"py-{uuid.uuid4().hex[:12]}")

    @staticmethod
    def _module_mode(status: ModuleAssetStatus | None) -> str:
        if status is None:
            return "disabled"
        if status.enabled and status.ready:
            return "real"
        if status.enabled:
            return "failed"
        return "disabled"

    @staticmethod
    def _module_impl(status: ModuleAssetStatus | None, fallback: str) -> str:
        if status is None:
            return fallback
        return status.impl_type or fallback

    @staticmethod
    def _runtime_dependency_items(exc: Exception) -> list[dict[str, Any]]:
        message = str(exc)
        items: list[dict[str, Any]] = []
        if "opencv-python-headless" in message:
            items.append(
                {
                    "moduleName": "runtime",
                    "requirement": "opencv-python-headless",
                    "message": "opencv-python-headless is required for image preprocessing",
                }
            )
        if "pydicom is required" in message:
            items.append(
                {
                    "moduleName": "runtime",
                    "requirement": "pydicom",
                    "message": "pydicom is required for DICOM ingestion",
                }
            )
        return items
