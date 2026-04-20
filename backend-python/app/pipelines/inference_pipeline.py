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
from app.infra.model.model_registry import ModelRegistry
from app.pipelines.detection_pipeline import DetectionPipeline
from app.pipelines.grading_pipeline import GradingPipeline, GradingResult
from app.pipelines.quality_pipeline import QualityPipeline
from app.pipelines.risk_pipeline import RiskPipeline, RiskPipelineResult
from app.pipelines.segmentation_pipeline import SegmentationPipeline, SegmentationResult
from app.repositories.ai_runtime_repository import AiRuntimeRepository
from app.schemas.base import dump_camel
from app.schemas.callback import (
    AnalysisCallbackPayload,
    AssetRef,
    EvidenceRef,
    FailureCallbackPayload,
    LesionResult,
    Summary,
    ToothDetection,
    VisualAsset,
)
from app.schemas.request import AnalyzeRequest, ImageInput
from app.services.image_fetch_service import FetchedImage, ImageFetchService, TaskWorkspace
from app.services.qwen_vision_service import QwenVisionService, VisionAnalysisResult
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService

log = get_logger("cariesguard-ai.pipeline")


class InferencePipeline:
    def __init__(
        self,
        settings: Settings,
        image_fetch_service: ImageFetchService,
        visual_asset_service: VisualAssetService | None,
        risk_service: RiskService,
        model_registry: ModelRegistry,
        quality_pipeline: QualityPipeline,
        detection_pipeline: DetectionPipeline,
        segmentation_pipeline: SegmentationPipeline | None = None,
        grading_pipeline: GradingPipeline | None = None,
        risk_pipeline: RiskPipeline | None = None,
        ai_runtime_repository: AiRuntimeRepository | None = None,
        qwen_vision_service: QwenVisionService | None = None,
    ) -> None:
        self.settings = settings
        self.image_fetch_service = image_fetch_service
        self.visual_asset_service = visual_asset_service
        self.risk_service = risk_service
        self.model_registry = model_registry
        self.quality_pipeline = quality_pipeline
        self.detection_pipeline = detection_pipeline
        self.segmentation_pipeline = segmentation_pipeline
        self.grading_pipeline = grading_pipeline
        self.risk_pipeline = risk_pipeline
        self.ai_runtime_repository = ai_runtime_repository
        self.qwen_vision_service = qwen_vision_service

    def run(self, raw_task: dict[str, Any]) -> dict[str, Any]:
        task = self._parse_task(raw_task)
        started_at = local_naive_iso_now()
        started = time.perf_counter()
        trace_id = task.trace_id or raw_task.get("traceId") or f"py-{uuid.uuid4().hex[:12]}"
        model_version = task.model_version or self.settings.model_version
        runtime_mode = self.model_registry.get_runtime_mode()
        log.info(
            "pipeline started taskNo=%s traceId=%s images=%s runtimeMode=%s",
            task.task_no, trace_id, len(task.images), runtime_mode,
        )
        runtime_job = self._safe_create_runtime_job(task, raw_task, model_version)

        with TaskWorkspace(self.settings, task.task_no) as workspace:
            fetched_images = self._fetch_images(task, workspace)

            # ── Phase 5A: mode-aware quality check ──────────────────────
            quality_results = []
            for image in task.images:
                img_path = self._get_image_path(fetched_images, image)
                quality_results.append(self.quality_pipeline.check(image, img_path))

            # ── Phase 5A: mode-aware tooth detection ────────────────────
            tooth_detections = self.detection_pipeline.detect_all(task.images, fetched_images)
            qwen_vision_result = self._analyze_with_qwen(task, fetched_images, tooth_detections)

            visual_assets, segmentation_result = self._create_visual_assets(
                task,
                fetched_images,
                workspace,
                model_version,
                tooth_detections,
                qwen_vision_result,
            )
            grading_result = self._create_grading_result(
                task,
                fetched_images,
                segmentation_result,
                tooth_detections,
                qwen_vision_result,
            )
            risk_result = self._create_risk_result(
                task,
                grading_result,
                segmentation_result,
                tooth_detections,
            )
            risk_assessment = risk_result.assessment
            review_reason = self._review_reason(
                grading_result.uncertainty_score,
                grading_result.needs_review,
                quality_results,
                risk_assessment.review_suggested,
            )
            evidence_refs = self._evidence_refs(risk_assessment, visual_assets)

        completed_at = local_naive_iso_now()
        inference_millis = int((time.perf_counter() - started) * 1000)
        first_image_id = task.images[0].image_id if task.images else None
        summary = Summary(
            overall_highest_severity=grading_result.grading_label,
            uncertainty_score=grading_result.uncertainty_score,
            review_suggested_flag="1" if grading_result.needs_review else "0",
            teeth_count=len(tooth_detections),
        )

        # ── Phase 5A: mode / implType stamps ────────────────────────────
        quality_mode = "real" if self.model_registry.is_module_real("quality") else "mock"
        quality_impl_type = self.quality_pipeline.get_last_impl_type()
        tooth_detection_mode = "real" if self.model_registry.is_module_real("tooth_detect") else "mock"
        tooth_detection_impl_type = self.detection_pipeline.get_last_impl_type()
        segmentation_mode = (
            segmentation_result.segmentation_mode
            if segmentation_result is not None
            else ("real" if self.model_registry.is_module_real("segmentation") else "mock")
        )
        segmentation_impl_type = (
            segmentation_result.segmentation_impl_type
            if segmentation_result is not None
            else (
                self.segmentation_pipeline.get_last_impl_type()
                if self.segmentation_pipeline is not None
                else "MOCK"
            )
        )

        raw_result_json: dict[str, Any] = {
            "pipelineVersion": "phase5d-1",
            "mode": runtime_mode,
            "aiRuntimeJobId": runtime_job.get("id") if runtime_job else None,
            "aiRuntimeJobNo": runtime_job.get("job_no") if runtime_job else None,
            "qualityMode": quality_mode,
            "qualityImplType": quality_impl_type,
            "toothDetectionMode": tooth_detection_mode,
            "toothDetectionImplType": tooth_detection_impl_type,
            "segmentationMode": segmentation_mode,
            "segmentationImplType": segmentation_impl_type,
            "segmentationRegions": segmentation_result.regions if segmentation_result is not None else [],
            "segmentationRawResult": segmentation_result.raw_result if segmentation_result is not None else {},
            "annotationProvider": "QWEN" if qwen_vision_result is not None else None,
            "annotationModel": self.settings.qwen_vision_model if qwen_vision_result is not None else None,
            "gradingMode": grading_result.grading_mode,
            "gradingImplType": grading_result.grading_impl_type,
            "gradingLabel": grading_result.grading_label,
            "confidenceScore": grading_result.confidence_score,
            "uncertaintyMode": grading_result.grading_mode,
            "uncertaintyImplType": grading_result.grading_impl_type,
            "uncertaintyScore": grading_result.uncertainty_score,
            "needsReview": grading_result.needs_review,
            "gradingRawResult": grading_result.raw_result,
            "riskMode": risk_result.risk_mode,
            "riskImplType": risk_result.risk_impl_type,
            "riskRawResult": risk_result.raw_result,
            "riskLevel": risk_assessment.risk_level_code or risk_assessment.overall_risk_level_code,
            "riskFactors": [dump_camel(item) for item in (risk_assessment.risk_factors or [])],
            "reviewReason": review_reason,
            "knowledgeVersion": self.settings.rag_knowledge_version,
            "evidenceRefs": [dump_camel(item) for item in evidence_refs],
            "doctorReviewRequiredReason": review_reason if grading_result.needs_review else None,
            "qualityCheckResults": [dump_camel(item) for item in quality_results],
            "toothDetections": [dump_camel(td) for td in tooth_detections],
            "lesionResults": self._build_lesion_results(
                first_image_id,
                tooth_detections,
                grading_result,
                visual_assets,
                qwen_vision_result,
            ),
            "lesionCount": len(qwen_vision_result.findings) if qwen_vision_result is not None else 1,
            "abnormalToothCount": self._abnormal_tooth_count(tooth_detections, qwen_vision_result),
            "clinicalSummary": qwen_vision_result.clinical_summary if qwen_vision_result is not None else None,
            "treatmentPlan": qwen_vision_result.treatment_plan if qwen_vision_result is not None else [],
            "qwenVisionRawResult": qwen_vision_result.raw_result if qwen_vision_result is not None else None,
            "riskAssessment": dump_camel(risk_assessment),
            "visualAssets": [dump_camel(item) for item in visual_assets],
        }
        self._safe_persist_runtime_success(
            runtime_job,
            task,
            fetched_images,
            quality_results,
            visual_assets,
            raw_result_json,
            grading_result,
        )
        payload = AnalysisCallbackPayload(
            task_no=task.task_no,
            task_status_code="SUCCESS",
            started_at=started_at,
            completed_at=completed_at,
            model_version=model_version,
            summary=summary,
            raw_result_json=raw_result_json,
            visual_assets=self._callback_visual_assets(visual_assets, task.task_no, trace_id),
            risk_assessment=risk_assessment,
            error_message=None,
            trace_id=trace_id,
            inference_millis=inference_millis,
            uncertainty_score=grading_result.uncertainty_score,
            risk_level=risk_assessment.risk_level_code or risk_assessment.overall_risk_level_code,
            risk_factors=risk_assessment.risk_factors or [],
            review_reason=review_reason,
            knowledge_version=self.settings.rag_knowledge_version,
            evidence_refs=evidence_refs,
            doctor_review_required_reason=review_reason if grading_result.needs_review else None,
        )
        log.info(
            "pipeline completed taskNo=%s traceId=%s millis=%s qualityMode=%s toothMode=%s segmentationMode=%s gradingMode=%s riskMode=%s",
            task.task_no, trace_id, inference_millis, quality_mode, tooth_detection_mode, segmentation_mode, grading_result.grading_mode, risk_result.risk_mode,
        )
        return dump_camel(payload)

    def build_failure_payload(self, raw_task: dict[str, Any], exc: Exception) -> dict[str, Any]:
        now = local_naive_iso_now()
        task_no = str(raw_task.get("taskNo") or raw_task.get("task_no") or "UNKNOWN")
        trace_id = str(raw_task.get("traceId") or raw_task.get("trace_id") or f"py-{uuid.uuid4().hex[:12]}")
        code = exc.code if isinstance(exc, BusinessException) else "C9999"
        model_version = str(raw_task.get("modelVersion") or self.settings.model_version)
        raw_result_json = {"errorCode": code, "errorType": exc.__class__.__name__}
        runtime_job = self._safe_persist_runtime_failure(
            raw_task,
            task_no,
            model_version,
            raw_result_json,
            exc,
        )
        if runtime_job:
            raw_result_json["aiRuntimeJobId"] = runtime_job.get("id")
            raw_result_json["aiRuntimeJobNo"] = runtime_job.get("job_no")
        payload = FailureCallbackPayload(
            task_no=task_no,
            task_status_code="FAILED",
            started_at=now,
            completed_at=now,
            model_version=model_version,
            raw_result_json=raw_result_json,
            error_code=code,
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

    @staticmethod
    def _get_image_path(fetched_images: list[FetchedImage], image: ImageInput) -> Path | None:
        """Find the local file path for the given ImageInput."""
        for f in fetched_images:
            if f.image_id == image.image_id:
                return f.path
        return fetched_images[0].path if fetched_images else None

    def _create_visual_assets(
        self,
        task: AnalyzeRequest,
        fetched_images: list[FetchedImage],
        workspace: Path,
        model_version: str,
        tooth_detections: list[ToothDetection],
        qwen_vision_result: VisionAnalysisResult | None = None,
    ) -> tuple[list[VisualAsset], SegmentationResult | None]:
        if not fetched_images or self.visual_asset_service is None:
            return [], None
        first_image = fetched_images[0]
        image_input = task.images[0] if task.images else ImageInput(image_id=first_image.image_id)
        case_no = task.case_no or f"CASE{task.case_id or task.task_no}"
        image_id = first_image.image_id
        generated_dir = workspace / "visual"
        generated_dir.mkdir(parents=True, exist_ok=True)

        if qwen_vision_result is not None and self.qwen_vision_service is not None:
            rendered = self.qwen_vision_service.render_visual_assets(
                first_image.path,
                qwen_vision_result,
                generated_dir,
            )
            tooth_code = self._vision_tooth_code(qwen_vision_result)
            assets = [
                self.visual_asset_service.upload_visual(
                    "MASK",
                    task.org_id,
                    case_no,
                    task.task_no,
                    model_version,
                    image_id,
                    rendered.mask_path,
                    tooth_code=tooth_code,
                ),
                self.visual_asset_service.upload_visual(
                    "OVERLAY",
                    task.org_id,
                    case_no,
                    task.task_no,
                    model_version,
                    image_id,
                    rendered.overlay_path,
                    tooth_code=tooth_code,
                ),
                self.visual_asset_service.upload_visual(
                    "HEATMAP",
                    task.org_id,
                    case_no,
                    task.task_no,
                    model_version,
                    image_id,
                    rendered.heatmap_path,
                ),
            ]
            return assets, SegmentationResult(
                segmentation_mode="real",
                segmentation_impl_type="VLM_API",
                regions=qwen_vision_result.to_regions(),
                mask_path=rendered.mask_path,
                overlay_path=rendered.overlay_path,
                heatmap_path=rendered.heatmap_path,
                raw_result=qwen_vision_result.raw_result,
            )

        if self.segmentation_pipeline is not None:
            segmentation_result = self.segmentation_pipeline.segment(
                image_input,
                first_image.path,
                tooth_detections,
                generated_dir,
            )
            tooth_code = self._segmentation_tooth_code(segmentation_result)
            assets = [
                self.visual_asset_service.upload_visual(
                    "MASK",
                    task.org_id,
                    case_no,
                    task.task_no,
                    model_version,
                    image_id,
                    segmentation_result.mask_path,
                    tooth_code=tooth_code,
                ),
                self.visual_asset_service.upload_visual(
                    "OVERLAY",
                    task.org_id,
                    case_no,
                    task.task_no,
                    model_version,
                    image_id,
                    segmentation_result.overlay_path,
                    tooth_code=tooth_code,
                ),
                self.visual_asset_service.upload_visual(
                    "HEATMAP",
                    task.org_id,
                    case_no,
                    task.task_no,
                    model_version,
                    image_id,
                    segmentation_result.heatmap_path,
                ),
            ]
            return assets, segmentation_result

        mask_path = generated_dir / f"mask_{image_id or 'unknown'}_16.png"
        overlay_path = generated_dir / f"overlay_{image_id or 'unknown'}_16.png"
        heatmap_path = generated_dir / f"heatmap_{image_id or 'unknown'}.png"
        self._draw_mock_assets(first_image.path, mask_path, overlay_path, heatmap_path)
        return [
            self.visual_asset_service.upload_visual("MASK", task.org_id, case_no, task.task_no, model_version, image_id, mask_path, tooth_code="16"),
            self.visual_asset_service.upload_visual("OVERLAY", task.org_id, case_no, task.task_no, model_version, image_id, overlay_path, tooth_code="16"),
            self.visual_asset_service.upload_visual("HEATMAP", task.org_id, case_no, task.task_no, model_version, image_id, heatmap_path),
        ], None

    def _create_grading_result(
        self,
        task: AnalyzeRequest,
        fetched_images: list[FetchedImage],
        segmentation_result: SegmentationResult | None,
        tooth_detections: list[ToothDetection],
        qwen_vision_result: VisionAnalysisResult | None = None,
    ) -> GradingResult:
        if qwen_vision_result is not None:
            uncertainty_score = qwen_vision_result.overall_uncertainty_score
            needs_review = uncertainty_score >= self.settings.uncertainty_review_threshold
            raw_result = dict(qwen_vision_result.raw_result)
            raw_result.update(
                {
                    "reviewThreshold": self.settings.uncertainty_review_threshold,
                    "needsReview": needs_review,
                    "clinicalSummary": qwen_vision_result.clinical_summary,
                    "treatmentPlan": qwen_vision_result.treatment_plan,
                }
            )
            return GradingResult(
                grading_mode="real",
                grading_impl_type="VLM_API",
                grading_label=qwen_vision_result.overall_severity_code,
                confidence_score=qwen_vision_result.overall_confidence_score,
                uncertainty_score=uncertainty_score,
                needs_review=needs_review,
                raw_result=raw_result,
            )

        if self.grading_pipeline is None:
            uncertainty_score = 0.1
            return GradingResult(
                grading_mode="mock",
                grading_impl_type="MOCK",
                grading_label="C1",
                confidence_score=0.9,
                uncertainty_score=uncertainty_score,
                needs_review=uncertainty_score >= self.settings.uncertainty_review_threshold,
                raw_result={
                    "source": "legacy-default",
                    "reviewThreshold": self.settings.uncertainty_review_threshold,
                },
            )

        image_input = task.images[0] if task.images else None
        image_path = fetched_images[0].path if fetched_images else None
        regions = segmentation_result.regions if segmentation_result is not None else []
        return self.grading_pipeline.grade(
            image_input,
            image_path,
            regions,
            tooth_detections,
        )

    def _analyze_with_qwen(
        self,
        task: AnalyzeRequest,
        fetched_images: list[FetchedImage],
        tooth_detections: list[ToothDetection],
    ) -> VisionAnalysisResult | None:
        if self.qwen_vision_service is None or not self.qwen_vision_service.is_enabled():
            return None
        if self.model_registry.get_runtime_mode() == "mock" or not fetched_images:
            return None

        first_image = fetched_images[0]
        relevant_detections = [
            item
            for item in tooth_detections
            if item.image_id in {None, first_image.image_id}
        ]
        try:
            result = self.qwen_vision_service.analyze(
                first_image.path,
                first_image.image_id,
                relevant_detections,
            )
            log.info(
                "qwen vision completed taskNo=%s imageId=%s findings=%s severity=%s",
                task.task_no,
                first_image.image_id,
                len(result.findings),
                result.overall_severity_code,
            )
            return result
        except Exception as exc:
            if self.model_registry.get_runtime_mode() == "real":
                raise BusinessException("M5010", f"qwen vision analysis failed: {exc}") from exc
            log.warning("qwen vision failed - fallback to existing pipeline (hybrid) error=%s", exc)
            return None

    def _create_risk_result(
        self,
        task: AnalyzeRequest,
        grading_result: GradingResult,
        segmentation_result: SegmentationResult | None,
        tooth_detections: list[ToothDetection],
    ) -> RiskPipelineResult:
        regions = segmentation_result.regions if segmentation_result is not None else []
        if self.risk_pipeline is not None:
            return self.risk_pipeline.assess(
                task.patient_profile,
                grading_result,
                regions,
                len(tooth_detections),
            )

        assessment = self.risk_service.assess(
            task.patient_profile,
            grading_label=grading_result.grading_label,
            uncertainty_score=grading_result.uncertainty_score,
            needs_review=grading_result.needs_review,
            segmentation_regions=regions,
        )
        return RiskPipelineResult(
            risk_mode="mock",
            risk_impl_type="MOCK",
            assessment=assessment,
            raw_result=assessment.assessment_report_json,
        )

    def _safe_create_runtime_job(
        self,
        task: AnalyzeRequest,
        raw_task: dict[str, Any],
        model_version: str,
    ) -> dict[str, Any]:
        if self.ai_runtime_repository is None:
            return {}
        try:
            return self.ai_runtime_repository.create_infer_job(
                task.task_no,
                model_version,
                case_no=task.case_no,
                patient_uuid=str(task.patient_id) if task.patient_id is not None else None,
                request_json=raw_task,
                org_id=task.org_id,
            )
        except Exception as exc:
            log.warning("failed to create ai runtime job taskNo=%s error=%s", task.task_no, exc)
            return {}

    def _safe_persist_runtime_success(
        self,
        runtime_job: dict[str, Any],
        task: AnalyzeRequest,
        fetched_images: list[FetchedImage],
        quality_results: list[Any],
        visual_assets: list[VisualAsset],
        raw_result_json: dict[str, Any],
        grading_result: GradingResult,
    ) -> None:
        if self.ai_runtime_repository is None or not runtime_job.get("id"):
            return
        job_id = int(runtime_job["id"])
        try:
            quality_by_image = {item.image_id: item for item in quality_results}
            fetched_by_image = {item.image_id: item for item in fetched_images}
            for image in task.images:
                quality = quality_by_image.get(image.image_id)
                fetched = fetched_by_image.get(image.image_id)
                image_result = {
                    "qualityCheckResult": dump_camel(quality) if quality is not None else None,
                    "gradingMode": grading_result.grading_mode,
                    "gradingImplType": grading_result.grading_impl_type,
                    "gradingLabel": grading_result.grading_label,
                    "confidenceScore": grading_result.confidence_score,
                    "uncertaintyScore": grading_result.uncertainty_score,
                    "needsReview": grading_result.needs_review,
                    "rawResult": raw_result_json,
                }
                self.ai_runtime_repository.add_job_image(
                    job_id,
                    image_id=image.image_id,
                    attachment_id=image.attachment_id,
                    image_type_code=image.image_type_code,
                    bucket_name=image.bucket_name,
                    object_key=image.object_key,
                    access_url=image.access_url,
                    download_status_code="SUCCESS" if fetched is not None else "SKIPPED",
                    local_cache_path=str(fetched.path) if fetched is not None else None,
                    quality_status_code=quality.check_result_code if quality is not None else None,
                    grading_label=grading_result.grading_label,
                    uncertainty_score=grading_result.uncertainty_score,
                    result_json=image_result,
                )

            for asset in visual_assets:
                self.ai_runtime_repository.add_artifact(
                    job_id,
                    related_image_id=asset.related_image_id,
                    artifact_type_code=asset.asset_type_code,
                    bucket_name=asset.bucket_name,
                    object_key=asset.object_key,
                    content_type=asset.content_type,
                    file_size_bytes=asset.file_size_bytes,
                    md5=asset.md5,
                    model_version=self.settings.model_version,
                    attachment_id=asset.attachment_id,
                    ext_json=dump_camel(asset),
                )

            self.ai_runtime_repository.finish_infer_job(
                job_id,
                "SUCCESS",
                result_json=raw_result_json,
            )
        except Exception as exc:
            log.warning("failed to persist ai runtime success taskNo=%s error=%s", task.task_no, exc)

    def _safe_persist_runtime_failure(
        self,
        raw_task: dict[str, Any],
        task_no: str,
        model_version: str,
        raw_result_json: dict[str, Any],
        exc: Exception,
    ) -> dict[str, Any]:
        if self.ai_runtime_repository is None:
            return {}
        try:
            runtime_job = self.ai_runtime_repository.get_latest_infer_job(task_no, open_only=True)
            if runtime_job is None:
                runtime_job = self.ai_runtime_repository.create_infer_job(
                    task_no,
                    model_version,
                    request_json=raw_task,
                )
            return self.ai_runtime_repository.finish_infer_job(
                int(runtime_job["id"]),
                "FAILED",
                result_json=raw_result_json,
                error_message=str(exc)[:1000],
            )
        except Exception as persist_exc:
            log.warning("failed to persist ai runtime failure taskNo=%s error=%s", task_no, persist_exc)
            return {}

    def _callback_visual_assets(self, visual_assets: list[VisualAsset], task_no: str, trace_id: str) -> list[VisualAsset]:
        mode = (self.settings.callback_visual_asset_mode or "metadata").strip().lower()
        if mode == "metadata":
            return visual_assets
        if mode in {"legacy-empty", "empty"}:
            if visual_assets:
                log.warning(
                    "top-level visualAssets suppressed for legacy Java compatibility taskNo=%s traceId=%s",
                    task_no,
                    trace_id,
                )
            return []
        raise BusinessException("C4001", f"unsupported CG_CALLBACK_VISUAL_ASSET_MODE={self.settings.callback_visual_asset_mode}")

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

    def _build_lesion_results(
        self,
        image_id: int | None,
        tooth_detections: list[ToothDetection],
        grading_result: GradingResult,
        visual_assets: list[VisualAsset],
        qwen_vision_result: VisionAnalysisResult | None,
    ) -> list[dict[str, Any]]:
        if qwen_vision_result is not None:
            results: list[dict[str, Any]] = []
            for finding in qwen_vision_result.findings:
                results.append(
                    dump_camel(
                        LesionResult(
                            image_id=image_id,
                            tooth_code=finding.tooth_code or (tooth_detections[0].tooth_code if tooth_detections else "16"),
                            severity_code=finding.severity_code,
                            uncertainty_score=finding.uncertainty_score,
                            lesion_area_px=finding.lesion_area_px,
                            lesion_area_ratio=finding.lesion_area_ratio,
                            mask_asset=self._asset_ref(visual_assets, "MASK"),
                            overlay_asset=self._asset_ref(visual_assets, "OVERLAY"),
                        )
                    )
                )
            return results

        return [
            dump_camel(
                LesionResult(
                    image_id=image_id,
                    tooth_code=tooth_detections[0].tooth_code if tooth_detections else "16",
                    severity_code=grading_result.grading_label,
                    uncertainty_score=grading_result.uncertainty_score,
                    lesion_area_px=512,
                    lesion_area_ratio=0.01,
                    mask_asset=self._asset_ref(visual_assets, "MASK"),
                    overlay_asset=self._asset_ref(visual_assets, "OVERLAY"),
                )
            )
        ]

    @staticmethod
    def _abnormal_tooth_count(
        tooth_detections: list[ToothDetection],
        qwen_vision_result: VisionAnalysisResult | None,
    ) -> int:
        if qwen_vision_result is None:
            return 1 if tooth_detections else 0
        return len({item.tooth_code for item in qwen_vision_result.findings if item.tooth_code})

    @staticmethod
    def _segmentation_tooth_code(segmentation_result: SegmentationResult) -> str:
        for region in segmentation_result.regions:
            tooth_code = region.get("toothCode") or region.get("tooth_code")
            if tooth_code:
                return str(tooth_code)
        return "16"

    @staticmethod
    def _vision_tooth_code(result: VisionAnalysisResult) -> str | None:
        for finding in result.findings:
            if finding.tooth_code:
                return finding.tooth_code
        return None

    def _review_reason(
        self,
        uncertainty_score: float,
        needs_review: bool,
        quality_results: list[Any],
        risk_review_suggested: bool | None,
    ) -> str | None:
        if not needs_review and not risk_review_suggested:
            return None
        reasons: list[str] = []
        if uncertainty_score >= self.settings.uncertainty_review_threshold:
            reasons.append("HIGH_UNCERTAINTY")
        if any(getattr(item, "check_result_code", "PASS") != "PASS" for item in quality_results):
            reasons.append("QUALITY_ALERT")
        if risk_review_suggested:
            reasons.append("RISK_RULE_REVIEW")
        return ",".join(reasons) if reasons else "MANUAL_REVIEW_REQUIRED"

    @staticmethod
    def _evidence_refs(
        risk_assessment: Any,
        visual_assets: list[VisualAsset],
    ) -> list[EvidenceRef]:
        refs: list[EvidenceRef] = []
        for item in (risk_assessment.risk_factors or [])[:3]:
            refs.append(
                EvidenceRef(
                    ref_type="RISK_FACTOR",
                    ref_code=item.code,
                    summary=item.evidence,
                    source=item.source,
                )
            )
        for asset in visual_assets:
            refs.append(
                EvidenceRef(
                    ref_type="VISUAL_ASSET",
                    ref_code=asset.asset_type_code,
                    summary=asset.file_name or asset.object_key,
                    source=f"{asset.bucket_name}/{asset.object_key}",
                )
            )
        return refs
