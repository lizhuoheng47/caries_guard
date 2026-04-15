from app.infra.storage.minio_client import normalize_endpoint


def test_normalize_endpoint_strips_scheme() -> None:
    endpoint, secure = normalize_endpoint("http://minio:9000", False)
    assert endpoint == "minio:9000"
    assert secure is False

    endpoint, secure = normalize_endpoint("https://minio.example.com", False)
    assert endpoint == "minio.example.com"
    assert secure is True

