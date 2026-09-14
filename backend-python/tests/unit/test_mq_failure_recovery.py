import json
from types import SimpleNamespace

from app.infra.mq.consumer import AnalysisRequestConsumer


class FailingAnalysisService:
    def execute(self, _task):
        raise RuntimeError("callback unavailable")


class RecordingChannel:
    def __init__(self) -> None:
        self.published = None
        self.acked = None
        self.nacked = None

    def basic_publish(self, **kwargs):
        self.published = kwargs

    def basic_ack(self, delivery_tag):
        self.acked = delivery_tag

    def basic_nack(self, delivery_tag, requeue):
        self.nacked = (delivery_tag, requeue)


def test_failed_delivery_is_persisted_to_recovery_route_then_acked() -> None:
    settings = SimpleNamespace(analysis_exchange="caries.analysis.exchange", failed_routing_key="analysis.failed")
    container = SimpleNamespace(analysis_service=FailingAnalysisService())
    channel = RecordingChannel()
    consumer = AnalysisRequestConsumer(settings, container)
    body = json.dumps({"payload": {"taskNo": "TASK-1", "traceId": "trace-1", "images": []}}).encode()

    consumer._handle_message(channel, SimpleNamespace(delivery_tag=42), None, body)

    assert channel.acked == 42
    assert channel.nacked is None
    assert channel.published["routing_key"] == "analysis.failed"
    failure = json.loads(channel.published["body"])
    assert failure["taskNo"] == "TASK-1"
    assert failure["originalMessage"]["payload"]["traceId"] == "trace-1"
