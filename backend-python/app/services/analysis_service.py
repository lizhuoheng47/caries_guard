from __future__ import annotations

from typing import Any

from app.core.logging import get_logger
from app.services.callback_service import CallbackService

log = get_logger("cariesguard-ai.analysis-service")


class AnalysisService:
    def __init__(self, pipeline: Any, callback_service: CallbackService) -> None:
        self._pipeline = pipeline
        self._callback_service = callback_service

    def execute(self, raw_task: dict[str, Any]) -> dict[str, Any]:
        payload = raw_task.get("payload") if isinstance(raw_task.get("payload"), dict) else raw_task
        callback_url = payload.get("callbackUrl") if isinstance(payload, dict) else None
        try:
            callback_payload = self._pipeline.run(raw_task)
        except Exception as exc:
            log.exception(
                "analysis execution failed taskNo=%s traceId=%s",
                payload.get("taskNo") if isinstance(payload, dict) else raw_task.get("taskNo"),
                payload.get("traceId") if isinstance(payload, dict) else raw_task.get("traceId"),
            )
            callback_payload = self._pipeline.build_failure_payload(raw_task, exc)
        self._callback_service.post_callback(callback_payload, callback_url)
        return callback_payload
