from functools import lru_cache

from app.core.config import Settings
from app.infra.storage.minio_client import MinioStorageClient
from app.pipelines.inference_pipeline import InferencePipeline
from app.services.callback_service import CallbackService
from app.services.image_fetch_service import ImageFetchService
from app.services.quality_service import QualityService
from app.services.risk_service import RiskService
from app.services.visual_asset_service import VisualAssetService


class AppContainer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.storage = MinioStorageClient(settings)
        self.image_fetch_service = ImageFetchService(settings, self.storage)
        self.visual_asset_service = VisualAssetService(settings, self.storage)
        self.quality_service = QualityService()
        self.risk_service = RiskService(settings)
        self.callback_service = CallbackService(settings)
        self.pipeline = InferencePipeline(
            settings=settings,
            image_fetch_service=self.image_fetch_service,
            visual_asset_service=self.visual_asset_service,
            quality_service=self.quality_service,
            risk_service=self.risk_service,
        )


@lru_cache(maxsize=1)
def get_container() -> AppContainer:
    return AppContainer(Settings())

