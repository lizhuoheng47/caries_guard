from app.core.config import Settings
from app.services.callback_service import CallbackService


class FakeResponse:
    status_code = 200
    text = '{"code":"00000"}'

    def raise_for_status(self):
        return None

    def json(self):
        return {"code": "00000"}


class FakeRuntimeRepository:
    def __init__(self):
        self.callback_logs = []
        self.callback_statuses = []

    def record_callback(self, job_id: int, callback_url: str, **fields):
        self.callback_logs.append((job_id, callback_url, fields))
        return {"id": len(self.callback_logs), "job_id": job_id, **fields}

    def update_callback_status(self, job_id: int, callback_status_code: str):
        self.callback_statuses.append((job_id, callback_status_code))
        return {"id": job_id, "callback_status_code": callback_status_code}


def test_post_callback_records_runtime_callback_log(monkeypatch):
    repo = FakeRuntimeRepository()

    def fake_post(*args, **kwargs):
        return FakeResponse()

    monkeypatch.setattr("app.services.callback_service.requests.post", fake_post)
    service = CallbackService(
        Settings(callback_url="http://java/callback", callback_secret="secret"),
        repo,
    )

    service.post_callback({
        "taskNo": "TASK-001",
        "traceId": "trace-001",
        "taskStatusCode": "SUCCESS",
        "rawResultJson": {"aiRuntimeJobId": 77},
    })

    assert repo.callback_logs[0][0] == 77
    assert repo.callback_logs[0][1] == "http://java/callback"
    assert repo.callback_logs[0][2]["callback_status_code"] == "SUCCESS"
    assert repo.callback_statuses == [(77, "SUCCESS")]
