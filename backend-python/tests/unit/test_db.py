from __future__ import annotations

import os

import pytest
from sqlalchemy import text

from app.core.db import get_engine, session_scope


@pytest.fixture(autouse=True)
def _clear_engine_cache():
    get_engine.cache_clear()
    yield
    get_engine.cache_clear()


@pytest.mark.skipif(
    os.getenv("CG_DB_TEST_ENABLED", "false").lower() not in {"1", "true", "yes"},
    reason="requires live MySQL; set CG_DB_TEST_ENABLED=true to enable",
)
def test_session_scope_executes_select_one():
    with session_scope() as session:
        result = session.execute(text("SELECT 1")).scalar_one()
    assert result == 1


def test_build_mysql_url_shape():
    from app.core.config import Settings

    url = Settings().build_mysql_url()
    assert url.startswith("mysql+pymysql://")
    assert "charset=utf8mb4" in url
