from app.schemas.callback import QualityCheckResult
from app.schemas.request import ImageInput


class QualityService:
    def check(self, image: ImageInput | None = None) -> QualityCheckResult:
        return QualityCheckResult(
            image_id=image.image_id if image else None,
            check_result_code="PASS",
            quality_score=90,
            blur_score=88,
            exposure_score=90,
            integrity_score=92,
            occlusion_score=86,
            issue_codes=[],
            suggestion_text="quality passed",
        )

