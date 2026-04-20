from __future__ import annotations

import json
import time
from typing import Any

import pika

from app.container import AppContainer
from app.core.config import Settings
from app.core.logging import get_logger

log = get_logger("cariesguard-ai.mq")


class AnalysisRequestConsumer:
    def __init__(self, settings: Settings, container: AppContainer) -> None:
        self.settings = settings
        self.container = container

    def start(self) -> None:
        connection = self._connect()
        channel = connection.channel()
        self._declare_topology(channel)
        channel.basic_consume(queue=self.settings.requested_queue, on_message_callback=self._handle_message)
        log.info(
            "waiting for analysis tasks rabbit=%s:%s queue=%s",
            self.settings.rabbit_host,
            self.settings.rabbit_port,
            self.settings.requested_queue,
        )
        channel.start_consuming()

    def _connect(self) -> pika.BlockingConnection:
        credentials = pika.PlainCredentials(self.settings.rabbit_username, self.settings.rabbit_password)
        params = pika.ConnectionParameters(
            host=self.settings.rabbit_host,
            port=self.settings.rabbit_port,
            credentials=credentials,
            heartbeat=30,
            blocked_connection_timeout=60,
        )
        while True:
            try:
                return pika.BlockingConnection(params)
            except pika.exceptions.AMQPConnectionError as exc:
                log.warning("RabbitMQ is not ready: %s; retrying in %ss", exc, self.settings.rabbit_retry_seconds)
                time.sleep(self.settings.rabbit_retry_seconds)

    def _declare_topology(self, channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
        channel.exchange_declare(exchange=self.settings.analysis_exchange, exchange_type="topic", durable=True)
        channel.queue_declare(queue=self.settings.requested_queue, durable=True)
        channel.queue_bind(
            queue=self.settings.requested_queue,
            exchange=self.settings.analysis_exchange,
            routing_key=self.settings.requested_routing_key,
        )
        channel.basic_qos(prefetch_count=1)

    def _handle_message(self, channel: pika.adapters.blocking_connection.BlockingChannel, method, properties, body: bytes) -> None:
        task: dict[str, Any] = {}
        try:
            task = json.loads(body.decode("utf-8"))
            payload = task.get("payload") if isinstance(task.get("payload"), dict) else task
            task_no = payload.get("taskNo") or task.get("taskNo")
            trace_id = payload.get("traceId") or task.get("traceId")
            log.info("received analysis task taskNo=%s traceId=%s images=%s", task_no, trace_id, len(payload.get("images") or []))
            callback_payload = self.container.pipeline.run(task)
            self.container.callback_service.post_callback(callback_payload, payload.get("callbackUrl"))
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as exc:
            payload = task.get("payload") if isinstance(task.get("payload"), dict) else task
            task_no = payload.get("taskNo") if isinstance(payload, dict) else task.get("taskNo")
            trace_id = payload.get("traceId") if isinstance(payload, dict) else task.get("traceId")
            callback_url = payload.get("callbackUrl") if isinstance(payload, dict) else None
            log.exception("analysis task failed taskNo=%s traceId=%s", task_no, trace_id)
            try:
                failure_payload = self.container.pipeline.build_failure_payload(task, exc)
                self.container.callback_service.post_callback(failure_payload, callback_url)
                channel.basic_ack(delivery_tag=method.delivery_tag)
            except Exception:
                log.exception("failed to callback failure result; message will be rejected")
                channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
