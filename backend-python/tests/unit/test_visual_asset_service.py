from pathlib import Path

from app.core.config import Settings
from app.services.visual_asset_service import VisualAssetService


class FakeStorage:
    def upload_file(self, bucket_name: str, object_key: str, local_path: Path, content_type: str):
        class Uploaded:
            etag = None
            version_id = None
            size = Path(local_path).stat().st_size
            file_name = Path(local_path).name

        uploaded = Uploaded()
        uploaded.bucket_name = bucket_name
        uploaded.object_key = object_key
        uploaded.content_type = content_type
        return uploaded


def test_visual_asset_metadata_and_key(tmp_path) -> None:
    path = tmp_path / "mask.png"
    path.write_bytes(b"png-data")
    settings = Settings()
    service = VisualAssetService(settings, FakeStorage())

    asset = service.upload_visual(
        "MASK",
        1001,
        "CASE202604150001",
        "ANA202604150001",
        "caries-v1",
        90001,
        path,
        tooth_code="16",
    )
    body = asset.model_dump(by_alias=True)

    assert body["assetTypeCode"] == "MASK"
    assert body["bucketName"] == "caries-visual"
    assert body["objectKey"].startswith(
        "org/1001/case/CASE202604150001/analysis/ANA202604150001/caries-v1/MASK/90001/16/tmp-"
    )
    assert body["objectKey"].endswith(".png")
    assert body["contentType"] == "image/png"
    assert body["fileSizeBytes"] == 8
    assert body["md5"]
