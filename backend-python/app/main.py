import json
import logging
import time
import uuid
from datetime import datetime, timezone
from typing import Any

import pika
import requests

from app.callback_signature import sign_callback
from app.config import Settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("cariesguard-ai")
settings = Settings()


def utc_now_local_iso() -> str:
    return datetime.now(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()


def compact_json(payload: dict[str, Any]) -> str:
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":"))


def connect_rabbit() -> pika.BlockingConnection:
    credentials = pika.PlainCredentials(settings.rabbit_username, settings.rabbit_password)
    params = pika.ConnectionParameters(
        host=settings.rabbit_host,
        port=settings.rabbit_port,
        credentials=credentials,
        heartbeat=30,
        blocked_connection_timeout=60,
    )
    while True:
        try:
            return pika.BlockingConnection(params)
        except pika.exceptions.AMQPConnectionError as exc:
            log.warning("RabbitMQ is not ready: %s; retrying in %ss", exc, settings.rabbit_retry_seconds)
            time.sleep(settings.rabbit_retry_seconds)


def declare_topology(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    channel.exchange_declare(exchange=settings.analysis_exchange, exchange_type="topic", durable=True)
    channel.queue_declare(queue=settings.requested_queue, durable=True)
    channel.queue_bind(
        queue=settings.requested_queue,
        exchange=settings.analysis_exchange,
        routing_key=settings.requested_routing_key,
    )
    channel.basic_qos(prefetch_count=1)


def download_images(task: dict[str, Any]) -> list[int]:
    sizes: list[int] = []
    if not settings.download_images:
        return sizes
    for image in task.get("images") or []:
        access_url = image.get("accessUrl")
        if not access_url:
            raise RuntimeError(f"image {image.get('imageId')} has no accessUrl")
        response = requests.get(access_url, timeout=settings.request_timeout_seconds)
        response.raise_for_status()
        sizes.append(len(response.content))
    return sizes


def run_inference(task: dict[str, Any]) -> dict[str, Any]:
    started_at = utc_now_local_iso()
    start = time.perf_counter()
    image_sizes = download_images(task)
    inference_millis = int((time.perf_counter() - start) * 1000)
    completed_at = utc_now_local_iso()
    model_version = task.get("modelVersion") or settings.model_version
    trace_id = f"py-{uuid.uuid4().hex[:12]}"

    return {
        "taskNo": task["taskNo"],
        "taskStatusCode": "SUCCESS",
        "startedAt": started_at,
        "completedAt": completed_at,
        "modelVersion": model_version,
        "summary": {
            "overallHighestSeverity": "C1",
            "uncertaintyScore": 0.1,
            "reviewSuggestedFlag": "0",
            "teethCount": None,
        },
        "rawResultJson": {
            "engine": "backend-python-skeleton",
            "imageCount": len(task.get("images") or []),
            "imageSizes": image_sizes,
            "note": "Replace run_inference() with the real model pipeline.",
        },
        "visualAssets": [],
        "riskAssessment": {
            "overallRiskLevelCode": "LOW",
            "assessmentReportJson": {
                "source": "backend-python-skeleton",
                "reason": "mock low-risk result",
            },
            "recommendedCycleDays": 180,
        },
        "errorMessage": None,
        "traceId": trace_id,
        "inferenceMillis": inference_millis,
        "uncertaintyScore": 0.1,
    }


def failure_callback(task: dict[str, Any], exc: Exception) -> dict[str, Any]:
    now = utc_now_local_iso()
    return {
        "taskNo": task.get("taskNo", "UNKNOWN"),
        "taskStatusCode": "FAILED",
        "startedAt": now,
        "completedAt": now,
        "modelVersion": task.get("modelVersion") or settings.model_version,
        "summary": None,
        "rawResultJson": {"errorType": exc.__class__.__name__},
        "visualAssets": [],
        "riskAssessment": None,
        "errorMessage": str(exc),
        "traceId": f"py-{uuid.uuid4().hex[:12]}",
        "inferenceMillis": 0,
        "uncertaintyScore": None,
    }


def post_callback(payload: dict[str, Any]) -> None:
    raw_body = compact_json(payload)
    timestamp = str(int(time.time()))
    signature = sign_callback(raw_body, timestamp, settings.callback_secret)
    headers = {
        "Content-Type": "application/json",
        "X-AI-Timestamp": timestamp,
        "X-AI-Signature": signature,
    }
    last_error: Exception | None = None
    for attempt in range(1, settings.callback_retry_count + 1):
        try:
            response = requests.post(
                settings.callback_url,
                data=raw_body.encode("utf-8"),
                headers=headers,
                timeout=settings.request_timeout_seconds,
            )
            response.raise_for_status()
            log.info("callback accepted taskNo=%s status=%s", payload.get("taskNo"), payload.get("taskStatusCode"))
            return
        except requests.RequestException as exc:
            last_error = exc
            log.warning("callback failed attempt=%s taskNo=%s error=%s", attempt, payload.get("taskNo"), exc)
            time.sleep(min(2 * attempt, 10))
    raise RuntimeError(f"callback failed after retries: {last_error}")


def handle_message(channel: pika.adapters.blocking_connection.BlockingChannel, method, properties, body: bytes) -> None:
    task: dict[str, Any] = {}
    try:
        task = json.loads(body.decode("utf-8"))
        log.info("received analysis task taskNo=%s images=%s", task.get("taskNo"), len(task.get("images") or []))
        payload = run_inference(task)
        post_callback(payload)
        channel.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exc:
        log.exception("analysis task failed taskNo=%s", task.get("taskNo"))
        try:
            post_callback(failure_callback(task, exc))
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception:
            log.exception("failed to callback failure result; message will be rejected")
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def main() -> None:
    log.info("starting CariesGuard Python AI skeleton")
    log.info("rabbit=%s:%s queue=%s", settings.rabbit_host, settings.rabbit_port, settings.requested_queue)
    log.info("callback_url=%s", settings.callback_url)
    connection = connect_rabbit()
    channel = connection.channel()
    declare_topology(channel)
    channel.basic_consume(queue=settings.requested_queue, on_message_callback=handle_message)
    log.info("waiting for analysis tasks")
    channel.start_consuming()


if __name__ == "__main__":
    main()