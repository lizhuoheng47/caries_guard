from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.v1.router import router as v1_router
from app.core.exceptions import BusinessException
from app.schemas.common import error_response


def create_app() -> FastAPI:
    app = FastAPI(title="CariesGuard Python AI", version="0.1.0")
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

