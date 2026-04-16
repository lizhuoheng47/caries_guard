from __future__ import annotations

import re
import uuid
from pathlib import Path

from app.core.config import Settings
from app.core.hash_utils import file_md5
from app.infra.storage.minio_client import MinioStorageClient
from app.schemas.callback import VisualAsset


CONTENT_TYPES = {
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".json": "application/json",
}

VISUAL_ASSET_TYPES = {"HEATMAP", "MASK", "OVERLAY"}


class VisualAssetService:
    def __init__(self, settings: Settings, storage: MinioStorageClient) -> None:
        self.settings = settings
        self.storage = storage

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
        path = Path(local_path)
        object_key = self.build_visual_object_key(
            asset_type_code=asset_type_code,
            org_id=org_id,
            case_no=case_no,
            task_no=task_no,
            model_version=model_version,
            image_id=image_id,
            file_name=path.name,
            tooth_code=tooth_code,
        )
        content_type = self.content_type(path)
        uploaded = self.storage.upload_file(
            bucket_name=self.settings.bucket_visual,
            object_key=object_key,
            local_path=path,
            content_type=content_type,
        )
        return VisualAsset(
            asset_type_code=self._asset_type(asset_type_code),
            bucket_name=uploaded.bucket_name,
            object_key=uploaded.object_key,
            content_type=uploaded.content_type,
            related_image_id=image_id,
            tooth_code=tooth_code,
            file_size_bytes=uploaded.size,
            md5=file_md5(path),
            file_name=uploaded.file_name,
        )

    def build_visual_object_key(
        self,
        asset_type_code: str,
        org_id: int | None,
        case_no: str,
        task_no: str,
        model_version: str,
        image_id: int | None,
        file_name: str,
        tooth_code: str | None,
    ) -> str:
        ext = self._extension(file_name)
        asset_type = self._asset_type(asset_type_code)
        related_image = str(image_id) if image_id is not None else "NA"
        tooth = self._segment(tooth_code) if tooth_code else "NA"
        temp_name = f"tmp-{uuid.uuid4().hex}.{ext}"
        return (
            f"org/{org_id or 0}"
            f"/case/{self._segment(case_no)}"
            f"/analysis/{self._segment(task_no)}"
            f"/{self._segment(model_version)}"
            f"/{asset_type}"
            f"/{related_image}"
            f"/{tooth}"
            f"/{temp_name}"
        )

    @staticmethod
    def content_type(path: Path) -> str:
        return CONTENT_TYPES.get(path.suffix.lower(), "application/octet-stream")

    @staticmethod
    def _asset_type(value: str) -> str:
        asset_type = (value or "").strip().upper()
        if asset_type not in VISUAL_ASSET_TYPES:
            raise ValueError(f"unsupported visual asset type: {value}")
        return asset_type

    @staticmethod
    def _extension(file_name: str) -> str:
        suffix = Path(file_name).suffix.lower().lstrip(".")
        return re.sub(r"[^a-z0-9]+", "", suffix) or "png"

    @staticmethod
    def _segment(value: str | None) -> str:
        cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", (value or "").strip().replace("\\", "/").replace("/", "-"))
        cleaned = re.sub(r"-+", "-", cleaned).strip("-")
        return cleaned or "NA"
