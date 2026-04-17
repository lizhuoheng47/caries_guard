from __future__ import annotations

import sqlite3
import uuid
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.core.time_utils import local_naive_iso_now


class MetadataRepository:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.db_path = settings.metadata_db_path
        if self.db_path != ":memory:":
            Path(self.db_path).parent.mkdir(parents=True, exist_ok=True)
        self.ensure_schema()

    def ensure_schema(self) -> None:
        with self._connect() as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS ai_infer_job (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_no TEXT NOT NULL UNIQUE,
                    java_task_no TEXT NOT NULL,
                    case_no TEXT,
                    patient_uuid TEXT,
                    infer_type_code TEXT NOT NULL DEFAULT 'ANALYZE',
                    model_version TEXT NOT NULL,
                    status_code TEXT NOT NULL DEFAULT 'QUEUEING',
                    request_json TEXT,
                    result_json TEXT,
                    error_message TEXT,
                    started_at TEXT,
                    finished_at TEXT,
                    callback_required_flag TEXT NOT NULL DEFAULT '1',
                    callback_status_code TEXT NOT NULL DEFAULT 'PENDING',
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS ai_infer_job_image (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id INTEGER NOT NULL,
                    image_id INTEGER,
                    attachment_id INTEGER,
                    image_type_code TEXT,
                    bucket_name TEXT,
                    object_key TEXT,
                    access_url TEXT,
                    url_expire_at TEXT,
                    download_status_code TEXT NOT NULL DEFAULT 'PENDING',
                    local_cache_path TEXT,
                    quality_status_code TEXT,
                    grading_label TEXT,
                    uncertainty_score REAL,
                    result_json TEXT,
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS ai_infer_artifact (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id INTEGER NOT NULL,
                    related_image_id INTEGER,
                    artifact_type_code TEXT NOT NULL,
                    bucket_name TEXT NOT NULL,
                    object_key TEXT NOT NULL,
                    content_type TEXT,
                    file_size_bytes INTEGER,
                    md5 TEXT,
                    model_version TEXT,
                    attachment_id INTEGER,
                    ext_json TEXT,
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS ai_callback_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id INTEGER NOT NULL,
                    callback_url TEXT NOT NULL,
                    request_json TEXT,
                    response_code INTEGER,
                    response_body TEXT,
                    callback_status_code TEXT NOT NULL DEFAULT 'PENDING',
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    next_retry_at TEXT,
                    error_message TEXT,
                    trace_id TEXT,
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS kb_knowledge_base (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kb_code TEXT NOT NULL UNIQUE,
                    kb_name TEXT NOT NULL,
                    kb_type_code TEXT NOT NULL DEFAULT 'PATIENT_GUIDE',
                    knowledge_version TEXT NOT NULL DEFAULT 'v1.0',
                    embedding_model TEXT,
                    vector_store_type_code TEXT NOT NULL DEFAULT 'LOCAL_JSON',
                    vector_store_path TEXT,
                    enabled_flag TEXT NOT NULL DEFAULT '1',
                    status_code TEXT NOT NULL DEFAULT 'ACTIVE',
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS kb_document (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kb_id INTEGER NOT NULL,
                    doc_no TEXT NOT NULL UNIQUE,
                    doc_title TEXT NOT NULL,
                    doc_source_code TEXT NOT NULL DEFAULT 'INTERNAL',
                    source_uri TEXT,
                    doc_version TEXT NOT NULL DEFAULT 'v1.0',
                    content_text TEXT,
                    content_attachment_key TEXT,
                    review_status_code TEXT NOT NULL DEFAULT 'PENDING',
                    reviewer_id INTEGER,
                    reviewed_at TEXT,
                    enabled_flag TEXT NOT NULL DEFAULT '1',
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS kb_document_chunk (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kb_id INTEGER NOT NULL,
                    doc_id INTEGER NOT NULL,
                    chunk_no INTEGER NOT NULL,
                    chunk_text TEXT NOT NULL,
                    token_count INTEGER,
                    embedding_model TEXT,
                    vector_store_path TEXT,
                    vector_id TEXT,
                    enabled_flag TEXT NOT NULL DEFAULT '1',
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS kb_rebuild_job (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rebuild_job_no TEXT NOT NULL UNIQUE,
                    kb_id INTEGER NOT NULL,
                    knowledge_version TEXT NOT NULL,
                    rebuild_status_code TEXT NOT NULL DEFAULT 'RUNNING',
                    chunk_count INTEGER NOT NULL DEFAULT 0,
                    vector_store_path TEXT,
                    started_at TEXT,
                    finished_at TEXT,
                    error_message TEXT,
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS rag_session (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_no TEXT NOT NULL UNIQUE,
                    session_type_code TEXT NOT NULL,
                    related_biz_no TEXT,
                    patient_uuid TEXT,
                    java_user_id INTEGER,
                    knowledge_version TEXT,
                    model_name TEXT,
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS rag_request_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL,
                    request_no TEXT NOT NULL UNIQUE,
                    request_type_code TEXT NOT NULL,
                    user_query TEXT NOT NULL,
                    rewritten_query TEXT,
                    top_k INTEGER NOT NULL DEFAULT 5,
                    answer_text TEXT,
                    request_status_code TEXT NOT NULL DEFAULT 'SUCCESS',
                    safety_flag TEXT NOT NULL DEFAULT '0',
                    latency_ms INTEGER,
                    org_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    deleted_flag TEXT NOT NULL DEFAULT '0',
                    remark TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_by INTEGER,
                    updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS rag_retrieval_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    request_id INTEGER NOT NULL,
                    chunk_id INTEGER NOT NULL,
                    rank_no INTEGER NOT NULL,
                    retrieval_score REAL,
                    doc_id INTEGER NOT NULL,
                    chunk_text_snapshot TEXT,
                    cited_flag TEXT NOT NULL DEFAULT '0',
                    org_id INTEGER,
                    created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS llm_call_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    request_id INTEGER NOT NULL,
                    model_name TEXT NOT NULL,
                    provider_code TEXT,
                    prompt_text TEXT,
                    completion_text TEXT,
                    prompt_tokens INTEGER,
                    completion_tokens INTEGER,
                    total_tokens INTEGER,
                    latency_ms INTEGER,
                    call_status_code TEXT NOT NULL DEFAULT 'SUCCESS',
                    error_message TEXT,
                    org_id INTEGER,
                    created_at TEXT NOT NULL
                );
                """
            )
            self._create_indexes(conn)

    def ensure_knowledge_base(
        self,
        kb_code: str,
        kb_name: str,
        kb_type_code: str,
        knowledge_version: str,
        embedding_model: str,
        vector_store_type_code: str,
        vector_store_path: str,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        existing = self.get_knowledge_base(kb_code)
        now = local_naive_iso_now()
        if existing:
            with self._connect() as conn:
                conn.execute(
                    """
                    UPDATE kb_knowledge_base
                    SET kb_name = ?, kb_type_code = ?, knowledge_version = ?, embedding_model = ?,
                        vector_store_type_code = ?, vector_store_path = ?, updated_at = ?
                    WHERE kb_code = ?
                    """,
                    (kb_name, kb_type_code, knowledge_version, embedding_model, vector_store_type_code, vector_store_path, now, kb_code),
                )
            return self.get_knowledge_base(kb_code) or existing
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO kb_knowledge_base (
                    kb_code, kb_name, kb_type_code, knowledge_version, embedding_model,
                    vector_store_type_code, vector_store_path, org_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (kb_code, kb_name, kb_type_code, knowledge_version, embedding_model, vector_store_type_code, vector_store_path, org_id, now, now),
            )
        return self.get_knowledge_base(kb_code) or {}

    def get_knowledge_base(self, kb_code: str) -> dict[str, Any] | None:
        with self._connect() as conn:
            row = conn.execute(
                "SELECT * FROM kb_knowledge_base WHERE kb_code = ? AND deleted_flag = '0'",
                (kb_code,),
            ).fetchone()
            return self._row(row)

    def create_document(
        self,
        kb_id: int,
        doc_title: str,
        content_text: str,
        doc_no: str | None,
        doc_source_code: str,
        source_uri: str | None,
        doc_version: str,
        review_status_code: str,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_iso_now()
        reviewed_at = now if review_status_code == "APPROVED" else None
        doc_no = doc_no or f"DOC-{uuid.uuid4().hex[:16].upper()}"
        with self._connect() as conn:
            cursor = conn.execute(
                """
                INSERT INTO kb_document (
                    kb_id, doc_no, doc_title, doc_source_code, source_uri, doc_version,
                    content_text, review_status_code, reviewed_at, org_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    kb_id,
                    doc_no,
                    doc_title,
                    doc_source_code,
                    source_uri,
                    doc_version,
                    content_text,
                    review_status_code,
                    reviewed_at,
                    org_id,
                    now,
                    now,
                ),
            )
            row_id = cursor.lastrowid
            row = conn.execute("SELECT * FROM kb_document WHERE id = ?", (row_id,)).fetchone()
            return self._row(row) or {}

    def list_approved_documents(self, kb_id: int) -> list[dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT * FROM kb_document
                WHERE kb_id = ?
                  AND review_status_code = 'APPROVED'
                  AND enabled_flag = '1'
                  AND deleted_flag = '0'
                ORDER BY id
                """,
                (kb_id,),
            ).fetchall()
            return [self._row(row) for row in rows if row is not None]

    def replace_chunks(self, kb_id: int, chunks: list[dict[str, Any]], embedding_model: str, vector_store_path: str) -> list[dict[str, Any]]:
        now = local_naive_iso_now()
        with self._connect() as conn:
            conn.execute("DELETE FROM kb_document_chunk WHERE kb_id = ?", (kb_id,))
            stored: list[dict[str, Any]] = []
            for item in chunks:
                cursor = conn.execute(
                    """
                    INSERT INTO kb_document_chunk (
                        kb_id, doc_id, chunk_no, chunk_text, token_count, embedding_model,
                        vector_store_path, org_id, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        kb_id,
                        item["doc_id"],
                        item["chunk_no"],
                        item["chunk_text"],
                        item.get("token_count"),
                        embedding_model,
                        vector_store_path,
                        item.get("org_id"),
                        now,
                    ),
                )
                chunk_id = cursor.lastrowid
                vector_id = f"chunk-{chunk_id}"
                conn.execute("UPDATE kb_document_chunk SET vector_id = ? WHERE id = ?", (vector_id, chunk_id))
                row = conn.execute(
                    """
                    SELECT c.*, d.doc_title, d.doc_no, d.source_uri, d.doc_source_code
                    FROM kb_document_chunk c
                    JOIN kb_document d ON d.id = c.doc_id
                    WHERE c.id = ?
                    """,
                    (chunk_id,),
                ).fetchone()
                if row is not None:
                    stored.append(self._row(row))
            return stored

    def create_rebuild_job(self, kb_id: int, knowledge_version: str, vector_store_path: str, org_id: int | None) -> dict[str, Any]:
        now = local_naive_iso_now()
        job_no = f"KBREBUILD-{uuid.uuid4().hex[:16].upper()}"
        with self._connect() as conn:
            cursor = conn.execute(
                """
                INSERT INTO kb_rebuild_job (
                    rebuild_job_no, kb_id, knowledge_version, rebuild_status_code, vector_store_path,
                    started_at, org_id, created_at, updated_at
                )
                VALUES (?, ?, ?, 'RUNNING', ?, ?, ?, ?, ?)
                """,
                (job_no, kb_id, knowledge_version, vector_store_path, now, org_id, now, now),
            )
            row = conn.execute("SELECT * FROM kb_rebuild_job WHERE id = ?", (cursor.lastrowid,)).fetchone()
            return self._row(row) or {}

    def finish_rebuild_job(self, job_id: int, status_code: str, chunk_count: int, error_message: str | None = None) -> dict[str, Any]:
        now = local_naive_iso_now()
        with self._connect() as conn:
            conn.execute(
                """
                UPDATE kb_rebuild_job
                SET rebuild_status_code = ?, chunk_count = ?, error_message = ?, finished_at = ?, updated_at = ?
                WHERE id = ?
                """,
                (status_code, chunk_count, error_message, now, now, job_id),
            )
            row = conn.execute("SELECT * FROM kb_rebuild_job WHERE id = ?", (job_id,)).fetchone()
            return self._row(row) or {}

    def list_chunks(self, kb_id: int) -> list[dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute(
                """
                SELECT c.*, d.doc_title, d.doc_no, d.source_uri, d.doc_source_code
                FROM kb_document_chunk c
                JOIN kb_document d ON d.id = c.doc_id
                WHERE c.kb_id = ?
                  AND c.enabled_flag = '1'
                  AND c.deleted_flag = '0'
                ORDER BY c.id
                """,
                (kb_id,),
            ).fetchall()
            return [self._row(row) for row in rows if row is not None]

    def create_rag_session(
        self,
        session_type_code: str,
        knowledge_version: str,
        model_name: str,
        related_biz_no: str | None,
        patient_uuid: str | None,
        java_user_id: int | None,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_iso_now()
        session_no = f"RAG-{uuid.uuid4().hex[:16].upper()}"
        with self._connect() as conn:
            cursor = conn.execute(
                """
                INSERT INTO rag_session (
                    session_no, session_type_code, related_biz_no, patient_uuid, java_user_id,
                    knowledge_version, model_name, org_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (session_no, session_type_code, related_biz_no, patient_uuid, java_user_id, knowledge_version, model_name, org_id, now, now),
            )
            row = conn.execute("SELECT * FROM rag_session WHERE id = ?", (cursor.lastrowid,)).fetchone()
            return self._row(row) or {}

    def create_rag_request(
        self,
        session_id: int,
        request_type_code: str,
        user_query: str,
        rewritten_query: str,
        top_k: int,
        org_id: int | None,
    ) -> dict[str, Any]:
        now = local_naive_iso_now()
        request_no = f"RAGREQ-{uuid.uuid4().hex[:16].upper()}"
        with self._connect() as conn:
            cursor = conn.execute(
                """
                INSERT INTO rag_request_log (
                    session_id, request_no, request_type_code, user_query, rewritten_query,
                    top_k, org_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (session_id, request_no, request_type_code, user_query, rewritten_query, top_k, org_id, now, now),
            )
            row = conn.execute("SELECT * FROM rag_request_log WHERE id = ?", (cursor.lastrowid,)).fetchone()
            return self._row(row) or {}

    def finish_rag_request(self, request_id: int, answer_text: str, status_code: str, latency_ms: int, safety_flag: str = "0") -> None:
        now = local_naive_iso_now()
        with self._connect() as conn:
            conn.execute(
                """
                UPDATE rag_request_log
                SET answer_text = ?, request_status_code = ?, latency_ms = ?, safety_flag = ?, updated_at = ?
                WHERE id = ?
                """,
                (answer_text, status_code, latency_ms, safety_flag, now, request_id),
            )

    def create_retrieval_logs(self, request_id: int, hits: list[dict[str, Any]], org_id: int | None) -> None:
        now = local_naive_iso_now()
        with self._connect() as conn:
            for rank, hit in enumerate(hits, start=1):
                conn.execute(
                    """
                    INSERT INTO rag_retrieval_log (
                        request_id, chunk_id, rank_no, retrieval_score, doc_id,
                        chunk_text_snapshot, cited_flag, org_id, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, '1', ?, ?)
                    """,
                    (
                        request_id,
                        hit["chunk_id"],
                        rank,
                        hit.get("score"),
                        hit["doc_id"],
                        hit.get("chunk_text"),
                        org_id,
                        now,
                    ),
                )

    def create_llm_call_log(
        self,
        request_id: int,
        model_name: str,
        provider_code: str,
        prompt_text: str,
        completion_text: str,
        latency_ms: int,
        status_code: str,
        org_id: int | None,
        error_message: str | None = None,
    ) -> None:
        now = local_naive_iso_now()
        prompt_tokens = len(prompt_text.split())
        completion_tokens = len(completion_text.split())
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO llm_call_log (
                    request_id, model_name, provider_code, prompt_text, completion_text,
                    prompt_tokens, completion_tokens, total_tokens, latency_ms,
                    call_status_code, error_message, org_id, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    request_id,
                    model_name,
                    provider_code,
                    prompt_text,
                    completion_text,
                    prompt_tokens,
                    completion_tokens,
                    prompt_tokens + completion_tokens,
                    latency_ms,
                    status_code,
                    error_message,
                    org_id,
                    now,
                ),
            )

    def _connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    @staticmethod
    def _row(row: sqlite3.Row | None) -> dict[str, Any] | None:
        if row is None:
            return None
        return {key: row[key] for key in row.keys()}

    @staticmethod
    def _create_indexes(conn: sqlite3.Connection) -> None:
        indexes = [
            "CREATE INDEX IF NOT EXISTS idx_ai_infer_job_java_task_no ON ai_infer_job(java_task_no)",
            "CREATE INDEX IF NOT EXISTS idx_ai_infer_job_case_status ON ai_infer_job(case_no, status_code)",
            "CREATE INDEX IF NOT EXISTS idx_ai_infer_job_image_job ON ai_infer_job_image(job_id, image_id)",
            "CREATE INDEX IF NOT EXISTS idx_ai_infer_artifact_job ON ai_infer_artifact(job_id, artifact_type_code)",
            "CREATE INDEX IF NOT EXISTS idx_ai_callback_log_job ON ai_callback_log(job_id, callback_status_code)",
            "CREATE INDEX IF NOT EXISTS idx_kb_document_kb_review ON kb_document(kb_id, review_status_code)",
            "CREATE INDEX IF NOT EXISTS idx_kb_chunk_kb_doc ON kb_document_chunk(kb_id, doc_id)",
            "CREATE INDEX IF NOT EXISTS idx_kb_rebuild_job_kb ON kb_rebuild_job(kb_id, rebuild_status_code)",
            "CREATE INDEX IF NOT EXISTS idx_rag_session_type ON rag_session(session_type_code)",
            "CREATE INDEX IF NOT EXISTS idx_rag_request_session ON rag_request_log(session_id, request_type_code)",
            "CREATE INDEX IF NOT EXISTS idx_rag_retrieval_request ON rag_retrieval_log(request_id, rank_no)",
            "CREATE INDEX IF NOT EXISTS idx_llm_call_request ON llm_call_log(request_id, model_name)",
        ]
        for statement in indexes:
            conn.execute(statement)
