from typing import Any

from pydantic import Field

from app.core.time_utils import local_naive_iso_now
from app.schemas.base import CamelModel


class ApiResponse(CamelModel):
    code: str = "00000"
    message: str = "success"
    data: Any | None = None
    trace_id: str | None = None
    timestamp: str = Field(default_factory=local_naive_iso_now)


def success_response(data: Any | None = None, trace_id: str | None = None, message: str = "success") -> dict:
    return ApiResponse(message=message, data=data, trace_id=trace_id).model_dump(by_alias=True)


def error_response(code: str, message: str, trace_id: str | None = None) -> dict:
    return ApiResponse(code=code, message=message, data=None, trace_id=trace_id).model_dump(by_alias=True)
