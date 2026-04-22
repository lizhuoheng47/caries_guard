from __future__ import annotations

from contextlib import contextmanager
from typing import Any, Iterator

import pymysql
from pymysql.connections import Connection
from pymysql.cursors import DictCursor

from app.core.config import Settings


class MetadataRepository:
    """Legacy schema-bootstrap helper retained for dev/local convenience.

    Phase 4 (Python persistence engineering) moved all CRUD onto domain
    repositories backed by SQLAlchemy ORM, and DDL lifecycle onto Alembic.
    This class now only owns the ``ensure_schema()`` fallback used when a
    developer runs ``python -m app.main`` against a fresh MySQL without
    first executing ``alembic upgrade head``. The fallback is gated by
    ``CG_DB_SCHEMA_BOOTSTRAP_ENABLED`` and is OFF in Docker/CI/prod.

    Scheduled for removal in Phase 4 stage B once Alembic is load-bearing
    in every environment.
    """

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.ensure_schema()

    def ensure_schema(self) -> None:
        if not self.settings.db_schema_bootstrap_enabled:
            return
        with self._transaction() as conn:
            for statement in self._table_statements():
                self._execute(conn, statement)
            for statement in self._index_statements():
                try:
                    self._execute(conn, statement)
                except pymysql.err.OperationalError as exc:
                    if exc.args and exc.args[0] == 1061:
                        continue
                    raise

    @contextmanager
    def _transaction(self, readonly: bool = False) -> Iterator[Connection]:
        conn = pymysql.connect(
            host=self.settings.mysql_host,
            port=self.settings.mysql_port,
            user=self.settings.mysql_username,
            password=self.settings.mysql_password,
            database=self.settings.mysql_database,
            charset="utf8mb4",
            cursorclass=DictCursor,
            autocommit=False,
            connect_timeout=self.settings.mysql_connect_timeout_seconds,
            read_timeout=self.settings.request_timeout_seconds,
            write_timeout=self.settings.request_timeout_seconds,
        )
        try:
            yield conn
            if not readonly:
                conn.commit()
        except Exception:
            if not readonly:
                conn.rollback()
            raise
        finally:
            conn.close()

    @staticmethod
    def _execute(conn: Connection, sql: str, params: tuple[Any, ...] = ()) -> int:
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            return int(cursor.lastrowid or 0)

    @staticmethod
    def _table_statements() -> list[str]:
        suffix = "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        return [
            f"""
            CREATE TABLE IF NOT EXISTS ai_infer_job (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                job_no VARCHAR(64) NOT NULL UNIQUE,
                java_task_no VARCHAR(64) NOT NULL,
                trace_id VARCHAR(128),
                case_no VARCHAR(64),
                patient_uuid VARCHAR(128),
                infer_type_code VARCHAR(32) NOT NULL DEFAULT 'ANALYZE',
                model_version VARCHAR(64) NOT NULL,
                status_code VARCHAR(32) NOT NULL DEFAULT 'QUEUEING',
                request_json JSON,
                result_json JSON,
                error_message VARCHAR(1000),
                started_at DATETIME,
                finished_at DATETIME,
                callback_required_flag CHAR(1) NOT NULL DEFAULT '1',
                callback_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS ai_infer_job_image (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                job_id BIGINT NOT NULL,
                image_id BIGINT,
                attachment_id BIGINT,
                image_type_code VARCHAR(32),
                bucket_name VARCHAR(128),
                object_key VARCHAR(500),
                access_url TEXT,
                url_expire_at DATETIME,
                download_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                local_cache_path VARCHAR(500),
                quality_status_code VARCHAR(32),
                grading_label VARCHAR(32),
                uncertainty_score DECIMAL(8,4),
                result_json JSON,
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS ai_infer_artifact (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                job_id BIGINT NOT NULL,
                related_image_id BIGINT,
                artifact_type_code VARCHAR(32) NOT NULL,
                bucket_name VARCHAR(128) NOT NULL,
                object_key VARCHAR(500) NOT NULL,
                content_type VARCHAR(128),
                file_size_bytes BIGINT,
                md5 VARCHAR(64),
                model_version VARCHAR(64),
                attachment_id BIGINT,
                ext_json JSON,
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS ai_callback_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                job_id BIGINT NOT NULL,
                callback_url VARCHAR(500) NOT NULL,
                request_json JSON,
                response_code INT,
                response_body TEXT,
                callback_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                retry_count INT NOT NULL DEFAULT 0,
                next_retry_at DATETIME,
                error_message VARCHAR(1000),
                trace_id VARCHAR(128),
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS kb_knowledge_base (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                kb_code VARCHAR(64) NOT NULL UNIQUE,
                kb_name VARCHAR(128) NOT NULL,
                kb_type_code VARCHAR(32) NOT NULL DEFAULT 'PATIENT_GUIDE',
                knowledge_version VARCHAR(64) NOT NULL DEFAULT 'v1.0',
                embedding_model VARCHAR(64),
                vector_store_type_code VARCHAR(32) NOT NULL DEFAULT 'LOCAL_JSON',
                vector_store_path VARCHAR(500),
                enabled_flag CHAR(1) NOT NULL DEFAULT '1',
                status_code VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS kb_document (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                kb_id BIGINT NOT NULL,
                doc_no VARCHAR(64) NOT NULL UNIQUE,
                doc_title VARCHAR(255) NOT NULL,
                doc_source_code VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
                source_uri VARCHAR(500),
                doc_version VARCHAR(64) NOT NULL DEFAULT 'v1.0',
                content_text LONGTEXT,
                content_attachment_key VARCHAR(500),
                review_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                reviewer_id BIGINT,
                reviewed_at DATETIME,
                enabled_flag CHAR(1) NOT NULL DEFAULT '1',
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS kb_document_chunk (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                kb_id BIGINT NOT NULL,
                doc_id BIGINT NOT NULL,
                chunk_no INT NOT NULL,
                chunk_text TEXT NOT NULL,
                token_count INT,
                embedding_model VARCHAR(64),
                vector_store_path VARCHAR(500),
                vector_id VARCHAR(128),
                enabled_flag CHAR(1) NOT NULL DEFAULT '1',
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                created_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS kb_rebuild_job (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                rebuild_job_no VARCHAR(64) NOT NULL UNIQUE,
                kb_id BIGINT NOT NULL,
                knowledge_version VARCHAR(64) NOT NULL,
                rebuild_status_code VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
                chunk_count INT NOT NULL DEFAULT 0,
                vector_store_path VARCHAR(500),
                started_at DATETIME,
                finished_at DATETIME,
                error_message VARCHAR(1000),
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS rag_session (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                session_no VARCHAR(64) NOT NULL UNIQUE,
                session_type_code VARCHAR(32) NOT NULL,
                related_biz_no VARCHAR(64),
                patient_uuid VARCHAR(128),
                java_user_id BIGINT,
                knowledge_version VARCHAR(64),
                model_name VARCHAR(64),
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS rag_request_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                session_id BIGINT NOT NULL,
                request_no VARCHAR(64) NOT NULL UNIQUE,
                request_type_code VARCHAR(32) NOT NULL,
                user_query TEXT NOT NULL,
                rewritten_query TEXT,
                top_k INT NOT NULL DEFAULT 5,
                answer_text LONGTEXT,
                request_status_code VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
                safety_flag CHAR(1) NOT NULL DEFAULT '0',
                latency_ms INT,
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS rag_retrieval_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                request_id BIGINT NOT NULL,
                chunk_id BIGINT NOT NULL,
                rank_no INT NOT NULL,
                retrieval_score DECIMAL(10,6),
                doc_id BIGINT NOT NULL,
                chunk_text_snapshot TEXT,
                cited_flag CHAR(1) NOT NULL DEFAULT '0',
                org_id BIGINT,
                created_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS llm_call_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                request_id BIGINT NOT NULL,
                model_name VARCHAR(128) NOT NULL,
                provider_code VARCHAR(64),
                prompt_text LONGTEXT,
                completion_text LONGTEXT,
                prompt_tokens INT,
                completion_tokens INT,
                total_tokens INT,
                latency_ms INT,
                call_status_code VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
                error_message VARCHAR(1000),
                org_id BIGINT,
                created_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS mdl_model_version (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                model_code VARCHAR(64) NOT NULL,
                model_name VARCHAR(128) NOT NULL,
                model_type_code VARCHAR(32) NOT NULL DEFAULT 'SEGMENTATION',
                version_no VARCHAR(64) NOT NULL,
                artifact_path VARCHAR(500),
                dataset_version VARCHAR(64),
                metrics_json JSON,
                status_code VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE',
                active_flag CHAR(1) NOT NULL DEFAULT '0',
                published_at DATETIME,
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL,
                UNIQUE KEY uk_mdl_model_version_code_version (model_code, version_no)
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS mdl_model_eval_record (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                model_version_id BIGINT NOT NULL,
                dataset_snapshot_id BIGINT,
                eval_type_code VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
                metric_json JSON,
                error_case_json JSON,
                evidence_attachment_key VARCHAR(500),
                evaluated_at DATETIME NOT NULL,
                evaluator_name VARCHAR(128),
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS mdl_model_approval_record (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                model_version_id BIGINT NOT NULL,
                decision_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                approver_name VARCHAR(128),
                decision_note VARCHAR(1000),
                approved_at DATETIME,
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS trn_dataset_snapshot (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                dataset_version VARCHAR(64) NOT NULL UNIQUE,
                snapshot_type_code VARCHAR(32) NOT NULL DEFAULT 'TRAIN',
                source_summary VARCHAR(500),
                sample_count INT,
                metadata_json JSON,
                dataset_card_path VARCHAR(500),
                released_at DATETIME,
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS trn_dataset_sample (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                snapshot_id BIGINT NOT NULL,
                sample_ref_no VARCHAR(128) NOT NULL,
                patient_uuid VARCHAR(128),
                image_ref_no VARCHAR(128),
                source_type_code VARCHAR(32) NOT NULL DEFAULT 'CORRECTION',
                split_type_code VARCHAR(32) NOT NULL DEFAULT 'TRAIN',
                label_version VARCHAR(64),
                org_id BIGINT,
                created_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS ann_annotation_record (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                sample_ref_no VARCHAR(128) NOT NULL,
                patient_uuid VARCHAR(128),
                annotation_version VARCHAR(64) NOT NULL,
                annotation_result_json JSON,
                annotation_object_key VARCHAR(500),
                annotator_l1 VARCHAR(128),
                reviewer_l2 VARCHAR(128),
                qc_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                difficulty_code VARCHAR(32),
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS ann_gold_set_item (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                sample_ref_no VARCHAR(128) NOT NULL UNIQUE,
                annotation_record_id BIGINT,
                difficulty_code VARCHAR(32),
                active_flag CHAR(1) NOT NULL DEFAULT '1',
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
            f"""
            CREATE TABLE IF NOT EXISTS trn_training_run (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                training_run_no VARCHAR(64) NOT NULL UNIQUE,
                dataset_snapshot_id BIGINT,
                target_model_code VARCHAR(64) NOT NULL,
                base_model_version VARCHAR(64),
                run_type_code VARCHAR(32) NOT NULL DEFAULT 'TRAIN',
                parameters_json JSON,
                output_artifact_path VARCHAR(500),
                metric_json JSON,
                run_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                started_at DATETIME,
                finished_at DATETIME,
                error_message VARCHAR(1000),
                org_id BIGINT,
                status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                deleted_flag CHAR(1) NOT NULL DEFAULT '0',
                remark VARCHAR(500),
                created_by BIGINT,
                created_at DATETIME NOT NULL,
                updated_by BIGINT,
                updated_at DATETIME NOT NULL
            ) {suffix}
            """,
        ]

    @staticmethod
    def _index_statements() -> list[str]:
        return [
            "CREATE INDEX idx_ai_infer_job_java_task_no ON ai_infer_job(java_task_no)",
            "CREATE INDEX idx_ai_infer_job_trace_id ON ai_infer_job(trace_id)",
            "CREATE INDEX idx_ai_infer_job_case_status ON ai_infer_job(case_no, status_code)",
            "CREATE INDEX idx_ai_infer_job_image_job ON ai_infer_job_image(job_id, image_id)",
            "CREATE INDEX idx_ai_infer_artifact_job ON ai_infer_artifact(job_id, artifact_type_code)",
            "CREATE INDEX idx_ai_callback_log_job ON ai_callback_log(job_id, callback_status_code)",
            "CREATE INDEX idx_kb_document_kb_review ON kb_document(kb_id, review_status_code)",
            "CREATE INDEX idx_kb_chunk_kb_doc ON kb_document_chunk(kb_id, doc_id)",
            "CREATE INDEX idx_kb_rebuild_job_kb ON kb_rebuild_job(kb_id, rebuild_status_code)",
            "CREATE INDEX idx_rag_session_type ON rag_session(session_type_code)",
            "CREATE INDEX idx_rag_request_session ON rag_request_log(session_id, request_type_code)",
            "CREATE INDEX idx_rag_retrieval_request ON rag_retrieval_log(request_id, rank_no)",
            "CREATE INDEX idx_llm_call_request ON llm_call_log(request_id, model_name)",
            "CREATE INDEX idx_mdl_model_version_type_status ON mdl_model_version(model_type_code, status_code)",
            "CREATE INDEX idx_mdl_model_eval_model_dataset ON mdl_model_eval_record(model_version_id, dataset_snapshot_id)",
            "CREATE INDEX idx_mdl_model_approval_model_decision ON mdl_model_approval_record(model_version_id, decision_code)",
            "CREATE INDEX idx_trn_dataset_snapshot_type ON trn_dataset_snapshot(snapshot_type_code)",
            "CREATE INDEX idx_trn_dataset_sample_snapshot_ref ON trn_dataset_sample(snapshot_id, sample_ref_no)",
            "CREATE INDEX idx_ann_annotation_sample_version ON ann_annotation_record(sample_ref_no, annotation_version)",
            "CREATE INDEX idx_ann_annotation_qc ON ann_annotation_record(qc_status_code)",
            "CREATE INDEX idx_ann_gold_set_active ON ann_gold_set_item(active_flag)",
            "CREATE INDEX idx_trn_training_run_dataset_status ON trn_training_run(dataset_snapshot_id, run_status_code)",
            "CREATE INDEX idx_trn_training_run_model ON trn_training_run(target_model_code)",
        ]
