from __future__ import annotations

import time
from typing import Any

import requests

from app.callback_signature import sign_callback
from app.core.config import Settings
from app.core.exceptions import DownstreamException
from app.core.json_utils import compact_json
from app.core.logging import get_logger
from app.repositories.ai_runtime_repository import AiRuntimeRepository

log = get_logger("cariesguard-ai.callback")


class CallbackService:
    def __init__(self, settings: Settings, ai_runtime_repository: AiRuntimeRepository | None = None) -> None:
        self.settings = settings
        self.ai_runtime_repository = ai_runtime_repository

    def post_callback(self, payload: dict[str, Any], callback_url: str | None = None) -> None:
        target_url = callback_url or self.settings.callback_url
        raw_body = compact_json(payload)
        timestamp = str(int(time.time()))
        signature = sign_callback(raw_body, timestamp, self.settings.callback_secret)
        headers = {
            "Content-Type": "application/json",
            "X-AI-Timestamp": timestamp,
            "X-AI-Signature": signature,
            "X-Trace-Id": str(payload.get("traceId") or ""),
        }
        last_error: Exception | None = None
        for attempt in range(1, self.settings.callback_retry_count + 1):
            try:
                response = requests.post(
                    target_url,
                    data=raw_body.encode("utf-8"),
                    headers=headers,
                    timeout=self.settings.request_timeout_seconds,
                )
                response.raise_for_status()
                self._raise_on_business_error(response)
                self._safe_record_callback(
                    payload,
                    target_url,
                    response_code=response.status_code,
                    response_body=response.text,
                    callback_status_code="SUCCESS",
                    retry_count=attempt - 1,
                )
                log.info(
                    "callback accepted taskNo=%s traceId=%s status=%s",
                    payload.get("taskNo"),
                    payload.get("traceId"),
                    payload.get("taskStatusCode"),
                )
                return
            except requests.RequestException as exc:
                last_error = exc
                response = getattr(exc, "response", None)
                self._safe_record_callback(
                    payload,
                    target_url,
                    response_code=response.status_code if response is not None else None,
                    response_body=response.text if response is not None else None,
                    callback_status_code="FAILED",
                    retry_count=attempt,
                    error_message=str(exc),
                )
                log.warning(
                    "callback failed attempt=%s taskNo=%s traceId=%s error=%s",
                    attempt,
                    payload.get("taskNo"),
                    payload.get("traceId"),
                    exc,
                )
                time.sleep(min(2 * attempt, 10))
            except DownstreamException as exc:
                last_error = exc
                self._safe_record_callback(
                    payload,
                    target_url,
                    callback_status_code="FAILED",
                    retry_count=attempt,
                    error_message=str(exc),
                )
                log.warning(
                    "callback rejected attempt=%s taskNo=%s traceId=%s error=%s",
                    attempt,
                    payload.get("taskNo"),
                    payload.get("traceId"),
                    exc,
                )
                time.sleep(min(2 * attempt, 10))
        raise DownstreamException(f"callback failed after retries: {last_error}")

    def _safe_record_callback(
        self,
        payload: dict[str, Any],
        callback_url: str,
        *,
        response_code: int | None = None,
        response_body: str | None = None,
        callback_status_code: str,
        retry_count: int,
        error_message: str | None = None,
    ) -> None:
        if self.ai_runtime_repository is None:
            return
        job_id = self._extract_runtime_job_id(payload)
        if job_id is None:
            return
        try:
            self.ai_runtime_repository.record_callback(
                job_id,
                callback_url,
                request_json=payload,
                response_code=response_code,
                response_body=(response_body[:4000] if response_body else None),
                callback_status_code=callback_status_code,
                retry_count=retry_count,
                error_message=(error_message[:1000] if error_message else None),
                trace_id=str(payload.get("traceId") or ""),
            )
            self.ai_runtime_repository.update_callback_status(job_id, callback_status_code)
        except Exception as exc:
            log.warning(
                "failed to persist callback log taskNo=%s traceId=%s error=%s",
                payload.get("taskNo"),
                payload.get("traceId"),
                exc,
            )

    @staticmethod
    def _extract_runtime_job_id(payload: dict[str, Any]) -> int | None:
        raw_result = payload.get("rawResultJson") or payload.get("raw_result_json") or {}
        if not isinstance(raw_result, dict):
            return None
        raw_job_id = raw_result.get("aiRuntimeJobId") or raw_result.get("ai_runtime_job_id")
        if raw_job_id is None:
            return None
        try:
            return int(raw_job_id)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _raise_on_business_error(response: requests.Response) -> None:
        try:
            body = response.json()
        except ValueError:
            return
        code = body.get("code")
        if code and code != "00000":
            raise DownstreamException(f"Java callback returned code={code} message={body.get('message')}")
