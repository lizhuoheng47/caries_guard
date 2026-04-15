from __future__ import annotations

from datetime import datetime
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


class VisualAssetService:
    def __init__(self, settings: Settings, storage: MinioStorageClient) -> None:
        self.settings = settings
        self.storage = storage

    def upload_visual(
        self,
        asset_type_code: str,
        case_no: str,
        image_id: int | None,
        local_path: str | Path,
        tooth_code: str | None = None,
    ) -> VisualAsset:
        path = Path(local_path)
        object_key = self.build_visual_object_key(asset_type_code, case_no, image_id, path.name, tooth_code)
        content_type = self.content_type(path)
        uploaded = self.storage.upload_file(
            bucket_name=self.settings.bucket_visual,
            object_key=object_key,
            local_path=path,
            content_type=content_type,
        )
        return VisualAsset(
            asset_type_code=asset_type_code,
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
        case_no: str,
        image_id: int | None,
        file_name: str,
        tooth_code: str | None,
    ) -> str:
        now = datetime.now()
        safe_case_no = case_no or "UNKNOWN_CASE"
        image_part = image_id if image_id is not None else "unknown"
        asset_type = asset_type_code.upper()
        if asset_type == "MASK":
            name = f"mask_{image_part}_{tooth_code or 'NA'}.png"
        elif asset_type == "OVERLAY":
            name = f"overlay_{image_part}_{tooth_code or 'NA'}.png"
        elif asset_type == "HEATMAP":
            name = f"heatmap_{image_part}.png"
        else:
            name = file_name
        return f"visual/{now:%Y/%m/%d}/{safe_case_no}/{name}"

    @staticmethod
    def content_type(path: Path) -> str:
        return CONTENT_TYPES.get(path.suffix.lower(), "application/octet-stream")

