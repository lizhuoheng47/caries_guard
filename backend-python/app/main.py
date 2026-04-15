from __future__ import annotations

import threading

import uvicorn

from app.api.app import create_app
from app.container import get_container
from app.core.logging import configure_logging, get_logger
from app.infra.mq.consumer import AnalysisRequestConsumer

configure_logging()
log = get_logger("cariesguard-ai")
fastapi_app = create_app()


def start_worker_thread() -> threading.Thread:
    container = get_container()
    worker = AnalysisRequestConsumer(container.settings, container)
    thread = threading.Thread(target=worker.start, name="analysis-mq-worker", daemon=True)
    thread.start()
    return thread


def main() -> None:
    container = get_container()
    settings = container.settings
    log.info("starting CariesGuard Python AI service mode=%s modelVersion=%s", settings.app_mode, settings.model_version)
    log.info("callback_url=%s", settings.callback_url)
    if settings.mq_worker_enabled:
        start_worker_thread()
    if settings.http_enabled:
        uvicorn.run(fastapi_app, host=settings.http_host, port=settings.http_port)
    elif settings.mq_worker_enabled:
        threading.Event().wait()


if __name__ == "__main__":
    main()

