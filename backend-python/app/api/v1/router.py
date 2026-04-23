from fastapi import APIRouter

from app.api.v1 import analyze, assess_risk, health, model_version, quality_check

router = APIRouter()
router.include_router(health.router)
router.include_router(quality_check.router)
router.include_router(analyze.router)
router.include_router(assess_risk.router)
router.include_router(model_version.router)
