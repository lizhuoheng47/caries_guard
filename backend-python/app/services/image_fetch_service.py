from __future__ import annotations

import shutil
import uuid
from dataclasses import dataclass
from pathlib import Path

import requests

from app.core.config import Settings
from app.core.exceptions import ResourceNotFoundException
from app.infra.storage.minio_client import MinioStorageClient, ObjectRef
from app.schemas.request import ImageInput


@dataclass(frozen=True)
class FetchedImage:
    image_id: int | None
    image_type_code: str | None
    path: Path
    size_bytes: int
    source: str
    bucket_name: str | None = None
    object_key: str | None = None


class TaskWorkspace:
    def __init__(self, settings: Settings, task_no: str) -> None:
        safe_task_no = "".join(ch if ch.isalnum() or ch in {"-", "_"} else "_" for ch in task_no)
        self.path = Path(settings.temp_dir) / safe_task_no / uuid.uuid4().hex

    def __enter__(self) -> Path:
        self.path.mkdir(parents=True, exist_ok=True)
        return self.path

    def __exit__(self, exc_type, exc, tb) -> None:
        shutil.rmtree(self.path, ignore_errors=True)


class ImageFetchService:
    def __init__(self, settings: Settings, storage: MinioStorageClient | None) -> None:
        self.settings = settings
        self.storage = storage

    def download(self, image: ImageInput, workspace: Path) -> FetchedImage:
        target_path = workspace / "images" / self._target_name(image)
        target_path.parent.mkdir(parents=True, exist_ok=True)

        if image.bucket_name and image.object_key:
            if self.storage is None:
                raise ResourceNotFoundException("MinIO storage client is not configured")
            self.storage.download_to_file(ObjectRef(image.bucket_name, image.object_key), target_path)
            return FetchedImage(
                image_id=image.image_id,
                image_type_code=image.image_type_code,
                path=target_path,
                size_bytes=target_path.stat().st_size,
                source="minio",
                bucket_name=image.bucket_name,
                object_key=image.object_key,
            )

        if image.access_url:
            response = requests.get(image.access_url, timeout=self.settings.request_timeout_seconds)
            response.raise_for_status()
            target_path.write_bytes(response.content)
            return FetchedImage(
                image_id=image.image_id,
                image_type_code=image.image_type_code,
                path=target_path,
                size_bytes=target_path.stat().st_size,
                source="accessUrl",
            )

        raise ResourceNotFoundException(f"image {image.image_id} has no bucketName/objectKey or accessUrl")

    def _target_name(self, image: ImageInput) -> str:
        suffix = ".jpg"
        source_name = image.original_filename or image.object_key or ""
        if "." in source_name:
            suffix = "." + source_name.rsplit(".", 1)[-1].lower()
        image_id = image.image_id if image.image_id is not None else "unknown"
        return f"image_{image_id}{suffix}"

