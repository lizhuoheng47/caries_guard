from __future__ import annotations

from opensearchpy import OpenSearch

from app.core.config import Settings


def create_opensearch_client(settings: Settings) -> OpenSearch:
    http_auth = None
    if settings.opensearch_username:
        http_auth = (settings.opensearch_username, settings.opensearch_password)
    return OpenSearch(
        hosts=settings.opensearch_hosts,
        http_auth=http_auth,
        verify_certs=settings.opensearch_verify_certs,
        use_ssl=any(host.startswith("https://") for host in settings.opensearch_hosts),
        ssl_assert_hostname=False,
        ssl_show_warn=False,
        timeout=settings.request_timeout_seconds,
    )
