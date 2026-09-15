from __future__ import annotations

import io
from dataclasses import dataclass
from datetime import timedelta
from pathlib import Path
from urllib.parse import urlparse

from minio import Minio

from app.core.config import Settings


@dataclass(frozen=True)
class ObjectRef:
    bucket_name: str
    object_key: str


@dataclass(frozen=True)
class UploadedObject:
    bucket_name: str
    object_key: str
    etag: str | None
    version_id: str | None
    size: int
    content_type: str
    file_name: str


def normalize_endpoint(endpoint: str, secure: bool) -> tuple[str, bool]:
    parsed = urlparse(endpoint)
    if parsed.scheme:
        return parsed.netloc, parsed.scheme == "https"
    return endpoint, secure


class MinioStorageClient:
    def __init__(self, settings: Settings) -> None:
        endpoint, secure = normalize_endpoint(settings.minio_endpoint, settings.minio_secure)
        self.client = Minio(
            endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=secure,
            region=settings.minio_region or None,
        )
        public_endpoint, public_secure = normalize_endpoint(settings.minio_public_endpoint, settings.minio_secure)
        # This client only signs browser-facing URLs.  Supplying the region is
        # important: otherwise the MinIO SDK tries to discover it by connecting
        # to the public endpoint, which is commonly 127.0.0.1 and therefore
        # unreachable from inside the application container.
        self.public_client = Minio(
            public_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=public_secure,
            region=settings.minio_region or "us-east-1",
        )

    def ensure_bucket(self, bucket_name: str) -> None:
        if not self.client.bucket_exists(bucket_name):
            self.client.make_bucket(bucket_name)

    def download_to_file(self, ref: ObjectRef, target_path: str | Path) -> Path:
        path = Path(target_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        self.client.fget_object(ref.bucket_name, ref.object_key, str(path))
        return path

    def download_bytes(self, ref: ObjectRef) -> bytes:
        response = self.client.get_object(ref.bucket_name, ref.object_key)
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()

    def upload_file(
        self,
        bucket_name: str,
        object_key: str,
        local_path: str | Path,
        content_type: str,
    ) -> UploadedObject:
        path = Path(local_path)
        result = self.client.fput_object(
            bucket_name,
            object_key,
            str(path),
            content_type=content_type,
        )
        return UploadedObject(
            bucket_name=bucket_name,
            object_key=object_key,
            etag=getattr(result, "etag", None),
            version_id=getattr(result, "version_id", None),
            size=path.stat().st_size,
            content_type=content_type,
            file_name=path.name,
        )

    def upload_bytes(
        self,
        bucket_name: str,
        object_key: str,
        data: bytes,
        content_type: str,
        file_name: str,
    ) -> UploadedObject:
        result = self.client.put_object(
            bucket_name,
            object_key,
            io.BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        return UploadedObject(
            bucket_name=bucket_name,
            object_key=object_key,
            etag=getattr(result, "etag", None),
            version_id=getattr(result, "version_id", None),
            size=len(data),
            content_type=content_type,
            file_name=file_name,
        )

    def presigned_get_url(self, bucket_name: str, object_key: str, expires_seconds: int) -> str:
        expires = timedelta(seconds=min(max(expires_seconds, 60), 7 * 24 * 60 * 60))
        return self.public_client.presigned_get_object(bucket_name, object_key, expires=expires)

