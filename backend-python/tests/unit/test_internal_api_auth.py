from fastapi.testclient import TestClient

from app.api.app import create_app
from app.api.v1 import health


def test_non_health_http_routes_require_internal_key() -> None:
    response = TestClient(create_app()).get("/ai/v1/segment/health")

    assert response.status_code == 401
    assert response.json()["code"] == "A0401"


def test_public_health_route_remains_available(monkeypatch) -> None:
    class FakeModelSwitchService:
        @staticmethod
        def get_runtime_status() -> dict:
            return {"runtimePipeline": "full_chain"}

    class FakeSettings:
        app_mode = "test"
        model_version = "test-model"
        minio_endpoint = "http://minio"
        rabbit_host = "rabbitmq"
        mysql_host = "mysql"
        mysql_port = 3306
        mysql_database = "caries_ai"

    class FakeContainer:
        settings = FakeSettings()
        model_switch_service = FakeModelSwitchService()

    monkeypatch.setattr(health, "get_container", lambda: FakeContainer())
    response = TestClient(create_app()).get("/ai/v1/health")

    assert response.status_code == 200
