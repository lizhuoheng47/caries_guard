from __future__ import annotations

import time
from typing import Any

import requests

from app.callback_signature import sign_callback
from app.core.config import Settings
from app.core.exceptions import DownstreamException
from app.core.json_utils import compact_json
from app.core.logging import get_logger

log = get_logger("cariesguard-ai.callback")


class CallbackService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

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
                log.info(
                    "callback accepted taskNo=%s traceId=%s status=%s",
                    payload.get("taskNo"),
                    payload.get("traceId"),
                    payload.get("taskStatusCode"),
                )
                return
            except requests.RequestException as exc:
                last_error = exc
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
                log.warning(
                    "callback rejected attempt=%s taskNo=%s traceId=%s error=%s",
                    attempt,
                    payload.get("taskNo"),
                    payload.get("traceId"),
                    exc,
                )
                time.sleep(min(2 * attempt, 10))
        raise DownstreamException(f"callback failed after retries: {last_error}")

    @staticmethod
    def _raise_on_business_error(response: requests.Response) -> None:
        try:
            body = response.json()
        except ValueError:
            return
        code = body.get("code")
        if code and code != "00000":
            raise DownstreamException(f"Java callback returned code={code} message={body.get('message')}")

