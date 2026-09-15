from fastapi import FastAPI, Request
import hmac
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.v1.router import router as v1_router
from app.core.config import Settings
from app.core.exceptions import BusinessException
from app.schemas.common import error_response


def create_app() -> FastAPI:
    settings = Settings()
    app = FastAPI(title="CariesGuard Python AI", version="0.1.0")

    @app.middleware("http")
    async def require_internal_api_key(request: Request, call_next):
        if request.url.path == "/ai/v1/health":
            return await call_next(request)
        supplied = request.headers.get("X-Internal-Api-Key", "")
        if not settings.internal_api_key or not hmac.compare_digest(supplied, settings.internal_api_key):
            return JSONResponse(status_code=401, content=error_response("A0401", "internal API key is required"))
        return await call_next(request)

    app.include_router(v1_router, prefix="/ai/v1")
    @app.exception_handler(BusinessException)
    async def business_exception_handler(request: Request, exc: BusinessException) -> JSONResponse:
        return JSONResponse(status_code=200, content=error_response(exc.code, exc.message, request.headers.get("X-Trace-Id")))

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(status_code=200, content=error_response("A0400", "request validation failed", request.headers.get("X-Trace-Id")))

    @app.exception_handler(Exception)
    async def unknown_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        return JSONResponse(status_code=200, content=error_response("C9999", str(exc), request.headers.get("X-Trace-Id")))

    return app

