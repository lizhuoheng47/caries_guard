from __future__ import annotations

import threading
import uuid
import shutil
import time
from datetime import datetime, timezone
from pathlib import Path

from fastapi import APIRouter, Request
from starlette.concurrency import run_in_threadpool

from app.core.exceptions import BusinessException
from app.core.runtime_paths import local_segmentation_output_dir
from app.schemas.common import success_response
from app.schemas.request import ImageInput
from app.services.local_segmentation_service import get_local_segmentation_runtime

router = APIRouter(tags=["segmentation"])
_INFERENCE_LOCK = threading.Lock()
_CLEANUP_LOCK = threading.Lock()
_last_cleanup_at = 0.0
_CONTENT_TYPE_EXTENSIONS = {
    "image/png": ".png",
    "image/jpeg": ".jpg",
    "application/dicom": ".dcm",
    "application/dicom+json": ".dcm",
}


def _run_segmentation(runtime: object, image: ImageInput, image_path: Path, output_dir: Path):
    # Serializing GPU work prevents concurrent requests from exhausting an 8 GB card.
    with _INFERENCE_LOCK:
        return runtime.segmentation_pipeline.segment(image, image_path, [], output_dir)


def _cleanup_expired_assets(root: Path, ttl_seconds: int) -> None:
    global _last_cleanup_at
    now = time.time()
    interval = min(max(ttl_seconds // 4, 30), 300)
    if now - _last_cleanup_at < interval:
        return
    with _CLEANUP_LOCK:
        if now - _last_cleanup_at < interval:
            return
        cutoff = now - max(ttl_seconds, 60)
        for child in root.iterdir() if root.exists() else []:
            try:
                if child.is_dir() and child.stat().st_mtime < cutoff:
                    shutil.rmtree(child)
            except OSError:
                continue
        _last_cleanup_at = now


def _publish_assets(runtime: object, request_id: str, result: object) -> dict[str, str]:
    """Persist generated images and expose only time-limited, read-only URLs."""
    bucket = runtime.settings.bucket_visual
    date_prefix = datetime.now(timezone.utc).strftime("%Y/%m/%d")
    object_prefix = f"segmentation/{date_prefix}/{request_id}"
    runtime.storage.ensure_bucket(bucket)

    urls: dict[str, str] = {}
    for response_key, path in (
        ("maskUrl", result.mask_path),
        ("overlayUrl", result.overlay_path),
        ("heatmapUrl", result.heatmap_path),
    ):
        object_key = f"{object_prefix}/{path.name}"
        runtime.storage.upload_file(bucket, object_key, path, "image/png")
        urls[response_key] = runtime.storage.presigned_get_url(
            bucket,
            object_key,
            runtime.settings.segmentation_asset_url_expiry_seconds,
        )
    return urls


@router.post("/segment")
async def segment_image(request: Request) -> dict:
    """Run the real lesion segmenter against raw PNG/JPEG/DICOM request bytes."""
    runtime = get_local_segmentation_runtime()
    settings = runtime.settings
    if not settings.local_segmentation_api_enabled:
        raise BusinessException("A0404", "local segmentation API is disabled")
    if not runtime.model_registry.is_module_real("segmentation"):
        raise BusinessException("M5005", "real segmentation model is not ready")

    content_type = request.headers.get("content-type", "").split(";", 1)[0].strip().lower()
    suffix = _CONTENT_TYPE_EXTENSIONS.get(content_type)
    if suffix is None:
        raise BusinessException(
            "A0400",
            "unsupported Content-Type; use image/png, image/jpeg, or application/dicom",
        )

    declared_length = request.headers.get("content-length")
    if declared_length:
        try:
            if int(declared_length) > settings.local_segmentation_api_max_bytes:
                raise BusinessException("A0413", "image exceeds configured upload size limit")
        except ValueError as exc:
            raise BusinessException("A0400", "invalid Content-Length header") from exc

    image_bytes = await request.body()
    if not image_bytes:
        raise BusinessException("A0400", "request body is empty")
    if len(image_bytes) > settings.local_segmentation_api_max_bytes:
        raise BusinessException("A0413", "image exceeds configured upload size limit")

    request_id = uuid.uuid4().hex
    output_root = local_segmentation_output_dir(settings)
    _cleanup_expired_assets(output_root, getattr(settings, "local_segmentation_asset_ttl_seconds", 3600))
    output_dir = output_root / request_id
    output_dir.mkdir(parents=True, exist_ok=False)
    input_path = output_dir / f"input{suffix}"
    input_path.write_bytes(image_bytes)
    image = ImageInput(image_id=None, image_type_code="DENTAL_XRAY")
    try:
        result = await run_in_threadpool(_run_segmentation, runtime, image, input_path, output_dir)
        assets = await run_in_threadpool(_publish_assets, runtime, request_id, result)
    finally:
        # MinIO is the durable source; local files are only inference scratch space.
        shutil.rmtree(output_dir, ignore_errors=True)

    regions = []
    for item in result.regions:
        region = dict(item)
        if str(region.get("toothCode") or "").upper() == "UNKNOWN":
            region.pop("toothCode", None)
        regions.append(region)

    raw = result.raw_result if isinstance(result.raw_result, dict) else {}
    data = {
        "requestId": request_id,
        "modelCode": raw.get("modelCode"),
        "implementationType": result.segmentation_impl_type,
        "device": raw.get("device"),
        "inferenceMode": raw.get("inferenceMode"),
        "maskThreshold": raw.get("maskThreshold"),
        "segmentationScore": raw.get("segmentationScore"),
        "regionCount": len(regions),
        "regions": regions,
        "assets": assets,
        "assetUrlExpiresInSeconds": settings.segmentation_asset_url_expiry_seconds,
        "needsReview": True,
        "limitations": [
            "Research-use binary caries segmentation; not a clinical diagnosis.",
            "This endpoint does not infer tooth number, lesion depth, severity grade, or treatment.",
        ],
    }
    return success_response(data=data, trace_id=request_id)


@router.get("/segment/health")
def segment_health() -> dict:
    runtime = get_local_segmentation_runtime()
    ready = runtime.model_registry.is_module_real("segmentation")
    adapter = runtime.model_registry.get_segmenter()
    data = {
        "status": "UP" if ready else "DOWN",
        "ready": ready,
        "runtimePipeline": "full_chain",
        "implementationType": adapter.impl_type.value if adapter is not None else "DISABLED",
        "modelCode": adapter.model_code if adapter is not None else None,
        "device": runtime.settings.model_device,
    }
    return success_response(data=data, trace_id="segment-health")
