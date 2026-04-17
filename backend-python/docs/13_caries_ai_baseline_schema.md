# caries_ai 基线 Schema(Phase 4 Step 1 产物)

> 本文档记录 `caries_ai` 数据库在 Phase 4 启动前的完整 schema 基线,作为 Alembic `0001_baseline_caries_ai.py` 迁移与 ORM 模型对齐的**唯一权威来源**。

## 生成方式

当前 `caries_ai` 的所有表由 `backend-python/app/repositories/metadata_repository.py` 的 `ensure_schema()` 方法于应用启动时执行 DDL 创建(`_table_statements()` 第 409-836 行,`_index_statements()` 第 838-863 行)。由于代码即建表源,本基线直接从代码抽取,并与库结构保持一致。所有表均使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。

## 表清单总览

| # | 表名 | 域 | 说明 |
|---|---|---|---|
| 1 | ai_infer_job | AI 运行 | AI 推理任务主表 |
| 2 | ai_infer_job_image | AI 运行 | 推理任务输入图像 |
| 3 | ai_infer_artifact | AI 运行 | 推理产物(分割图/热力图等) |
| 4 | ai_callback_log | AI 运行 | Java 回调日志 |
| 5 | kb_knowledge_base | RAG/知识库 | 知识库元数据 |
| 6 | kb_document | RAG/知识库 | 知识库文档 |
| 7 | kb_document_chunk | RAG/知识库 | 文档切块(向量检索单位) |
| 8 | kb_rebuild_job | RAG/知识库 | 索引重建任务 |
| 9 | rag_session | RAG/知识库 | RAG 会话 |
| 10 | rag_request_log | RAG/知识库 | RAG 请求日志 |
| 11 | rag_retrieval_log | RAG/知识库 | 检索结果日志 |
| 12 | llm_call_log | RAG/知识库 | LLM 调用日志 |
| 13 | mdl_model_version | 模型治理 | 模型版本 |
| 14 | mdl_model_eval_record | 模型治理 | 模型评估记录 |
| 15 | mdl_model_approval_record | 模型治理 | 模型审批记录 |
| 16 | trn_dataset_snapshot | 模型治理 | 数据集快照 |
| 17 | trn_dataset_sample | 模型治理 | 数据集样本 |
| 18 | trn_training_run | 模型治理 | 训练运行 |
| 19 | ann_annotation_record | 模型治理 | 标注记录 |
| 20 | ann_gold_set_item | 模型治理 | 黄金集条目 |

**统计**: 20 张业务表 + 22 个二级索引 + 6 个表内 `UNIQUE` 列约束 + 1 个命名 `UNIQUE KEY`。代码与库一致(代码即权威源),无分歧项。

## 公共列约定

所有业务主表(非纯日志表)共享以下列集合:

| 列名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| org_id | BIGINT | NULL | 组织 ID |
| status | VARCHAR(32) | NOT NULL DEFAULT 'ACTIVE' | 行状态 |
| deleted_flag | CHAR(1) | NOT NULL DEFAULT '0' | 软删除标记 |
| remark | VARCHAR(500) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**轻量日志表**(`kb_document_chunk` / `rag_retrieval_log` / `llm_call_log` / `trn_dataset_sample`)仅保留 `id` / `org_id` / `created_at`,部分含 `status` / `deleted_flag`,详见各表定义。

---

## AI 运行域

### 1. ai_infer_job

| 列名 | 类型 | NULL | 默认值 | 说明 |
|---|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT | PK |
| job_no | VARCHAR(64) | NO | — | 任务号,`UNIQUE` |
| java_task_no | VARCHAR(64) | NO | — | Java 端任务号 |
| case_no | VARCHAR(64) | YES | — | 病例号 |
| patient_uuid | VARCHAR(128) | YES | — | 患者 UUID |
| infer_type_code | VARCHAR(32) | NO | 'ANALYZE' | 推理类型码 |
| model_version | VARCHAR(64) | NO | — | 模型版本 |
| status_code | VARCHAR(32) | NO | 'QUEUEING' | 状态码 |
| request_json | JSON | YES | — | 请求载荷 |
| result_json | JSON | YES | — | 结果载荷 |
| error_message | VARCHAR(1000) | YES | — | 错误信息 |
| started_at | DATETIME | YES | — | 开始时间 |
| finished_at | DATETIME | YES | — | 完成时间 |
| callback_required_flag | CHAR(1) | NO | '1' | 是否需要回调 |
| callback_status_code | VARCHAR(32) | NO | 'PENDING' | 回调状态 |
| + 公共列 | | | | org_id / status / deleted_flag / remark / created_by / created_at / updated_by / updated_at |

**索引**:
- `idx_ai_infer_job_java_task_no (java_task_no)`
- `idx_ai_infer_job_case_status (case_no, status_code)`

### 2. ai_infer_job_image

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| job_id | BIGINT | NO | — |
| image_id | BIGINT | YES | — |
| attachment_id | BIGINT | YES | — |
| image_type_code | VARCHAR(32) | YES | — |
| bucket_name | VARCHAR(128) | YES | — |
| object_key | VARCHAR(500) | YES | — |
| access_url | TEXT | YES | — |
| url_expire_at | DATETIME | YES | — |
| download_status_code | VARCHAR(32) | NO | 'PENDING' |
| local_cache_path | VARCHAR(500) | YES | — |
| quality_status_code | VARCHAR(32) | YES | — |
| grading_label | VARCHAR(32) | YES | — |
| uncertainty_score | DECIMAL(8,4) | YES | — |
| result_json | JSON | YES | — |
| + 公共列 | | | |

**索引**: `idx_ai_infer_job_image_job (job_id, image_id)`

### 3. ai_infer_artifact

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| job_id | BIGINT | NO | — |
| related_image_id | BIGINT | YES | — |
| artifact_type_code | VARCHAR(32) | NO | — |
| bucket_name | VARCHAR(128) | NO | — |
| object_key | VARCHAR(500) | NO | — |
| content_type | VARCHAR(128) | YES | — |
| file_size_bytes | BIGINT | YES | — |
| md5 | VARCHAR(64) | YES | — |
| model_version | VARCHAR(64) | YES | — |
| attachment_id | BIGINT | YES | — |
| ext_json | JSON | YES | — |
| + 公共列 | | | |

**索引**: `idx_ai_infer_artifact_job (job_id, artifact_type_code)`

### 4. ai_callback_log

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| job_id | BIGINT | NO | — |
| callback_url | VARCHAR(500) | NO | — |
| request_json | JSON | YES | — |
| response_code | INT | YES | — |
| response_body | TEXT | YES | — |
| callback_status_code | VARCHAR(32) | NO | 'PENDING' |
| retry_count | INT | NO | 0 |
| next_retry_at | DATETIME | YES | — |
| error_message | VARCHAR(1000) | YES | — |
| trace_id | VARCHAR(128) | YES | — |
| + 公共列 | | | |

**索引**: `idx_ai_callback_log_job (job_id, callback_status_code)`

---

## RAG / 知识库域

### 5. kb_knowledge_base

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| kb_code | VARCHAR(64) | NO | — (`UNIQUE`) |
| kb_name | VARCHAR(128) | NO | — |
| kb_type_code | VARCHAR(32) | NO | 'PATIENT_GUIDE' |
| knowledge_version | VARCHAR(64) | NO | 'v1.0' |
| embedding_model | VARCHAR(64) | YES | — |
| vector_store_type_code | VARCHAR(32) | NO | 'LOCAL_JSON' |
| vector_store_path | VARCHAR(500) | YES | — |
| enabled_flag | CHAR(1) | NO | '1' |
| status_code | VARCHAR(32) | NO | 'ACTIVE' |
| + 公共列 | | | |

**索引**: 无额外二级索引(仅 `kb_code` 列 UNIQUE)

### 6. kb_document

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| kb_id | BIGINT | NO | — |
| doc_no | VARCHAR(64) | NO | — (`UNIQUE`) |
| doc_title | VARCHAR(255) | NO | — |
| doc_source_code | VARCHAR(32) | NO | 'INTERNAL' |
| source_uri | VARCHAR(500) | YES | — |
| doc_version | VARCHAR(64) | NO | 'v1.0' |
| content_text | LONGTEXT | YES | — |
| content_attachment_key | VARCHAR(500) | YES | — |
| review_status_code | VARCHAR(32) | NO | 'PENDING' |
| reviewer_id | BIGINT | YES | — |
| reviewed_at | DATETIME | YES | — |
| enabled_flag | CHAR(1) | NO | '1' |
| + 公共列 | | | |

**索引**: `idx_kb_document_kb_review (kb_id, review_status_code)`

### 7. kb_document_chunk

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| kb_id | BIGINT | NO | — |
| doc_id | BIGINT | NO | — |
| chunk_no | INT | NO | — |
| chunk_text | TEXT | NO | — |
| token_count | INT | YES | — |
| embedding_model | VARCHAR(64) | YES | — |
| vector_store_path | VARCHAR(500) | YES | — |
| vector_id | VARCHAR(128) | YES | — |
| enabled_flag | CHAR(1) | NO | '1' |
| org_id | BIGINT | YES | — |
| status | VARCHAR(32) | NO | 'ACTIVE' |
| deleted_flag | CHAR(1) | NO | '0' |
| created_at | DATETIME | NO | — |

**索引**: `idx_kb_chunk_kb_doc (kb_id, doc_id)`

> **注**: 轻量表,**无** `remark / created_by / updated_by / updated_at`。

### 8. kb_rebuild_job

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| rebuild_job_no | VARCHAR(64) | NO | — (`UNIQUE`) |
| kb_id | BIGINT | NO | — |
| knowledge_version | VARCHAR(64) | NO | — |
| rebuild_status_code | VARCHAR(32) | NO | 'RUNNING' |
| chunk_count | INT | NO | 0 |
| vector_store_path | VARCHAR(500) | YES | — |
| started_at | DATETIME | YES | — |
| finished_at | DATETIME | YES | — |
| error_message | VARCHAR(1000) | YES | — |
| + 公共列 | | | |

**索引**: `idx_kb_rebuild_job_kb (kb_id, rebuild_status_code)`

### 9. rag_session

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| session_no | VARCHAR(64) | NO | — (`UNIQUE`) |
| session_type_code | VARCHAR(32) | NO | — |
| related_biz_no | VARCHAR(64) | YES | — |
| patient_uuid | VARCHAR(128) | YES | — |
| java_user_id | BIGINT | YES | — |
| knowledge_version | VARCHAR(64) | YES | — |
| model_name | VARCHAR(64) | YES | — |
| + 公共列 | | | |

**索引**: `idx_rag_session_type (session_type_code)`

### 10. rag_request_log

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| session_id | BIGINT | NO | — |
| request_no | VARCHAR(64) | NO | — (`UNIQUE`) |
| request_type_code | VARCHAR(32) | NO | — |
| user_query | TEXT | NO | — |
| rewritten_query | TEXT | YES | — |
| top_k | INT | NO | 5 |
| answer_text | LONGTEXT | YES | — |
| request_status_code | VARCHAR(32) | NO | 'SUCCESS' |
| safety_flag | CHAR(1) | NO | '0' |
| latency_ms | INT | YES | — |
| + 公共列 | | | |

**索引**: `idx_rag_request_session (session_id, request_type_code)`

### 11. rag_retrieval_log

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| request_id | BIGINT | NO | — |
| chunk_id | BIGINT | NO | — |
| rank_no | INT | NO | — |
| retrieval_score | DECIMAL(10,6) | YES | — |
| doc_id | BIGINT | NO | — |
| chunk_text_snapshot | TEXT | YES | — |
| cited_flag | CHAR(1) | NO | '0' |
| org_id | BIGINT | YES | — |
| created_at | DATETIME | NO | — |

**索引**: `idx_rag_retrieval_request (request_id, rank_no)`

> **注**: 纯日志表,**无** status/deleted_flag/remark/updated_*。

### 12. llm_call_log

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| request_id | BIGINT | NO | — |
| model_name | VARCHAR(128) | NO | — |
| provider_code | VARCHAR(64) | YES | — |
| prompt_text | LONGTEXT | YES | — |
| completion_text | LONGTEXT | YES | — |
| prompt_tokens | INT | YES | — |
| completion_tokens | INT | YES | — |
| total_tokens | INT | YES | — |
| latency_ms | INT | YES | — |
| call_status_code | VARCHAR(32) | NO | 'SUCCESS' |
| error_message | VARCHAR(1000) | YES | — |
| org_id | BIGINT | YES | — |
| created_at | DATETIME | NO | — |

**索引**: `idx_llm_call_request (request_id, model_name)`

> **注**: 纯日志表,无 status/deleted_flag/remark/updated_*。

---

## 模型治理域

### 13. mdl_model_version

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| model_code | VARCHAR(64) | NO | — |
| model_name | VARCHAR(128) | NO | — |
| model_type_code | VARCHAR(32) | NO | 'SEGMENTATION' |
| version_no | VARCHAR(64) | NO | — |
| artifact_path | VARCHAR(500) | YES | — |
| dataset_version | VARCHAR(64) | YES | — |
| metrics_json | JSON | YES | — |
| status_code | VARCHAR(32) | NO | 'CANDIDATE' |
| active_flag | CHAR(1) | NO | '0' |
| published_at | DATETIME | YES | — |
| + 公共列 | | | |

**唯一约束**: `UNIQUE KEY uk_mdl_model_version_code_version (model_code, version_no)`  
**索引**: `idx_mdl_model_version_type_status (model_type_code, status_code)`

### 14. mdl_model_eval_record

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| model_version_id | BIGINT | NO | — |
| dataset_snapshot_id | BIGINT | YES | — |
| eval_type_code | VARCHAR(32) | NO | 'OFFLINE' |
| metric_json | JSON | YES | — |
| error_case_json | JSON | YES | — |
| evidence_attachment_key | VARCHAR(500) | YES | — |
| evaluated_at | DATETIME | NO | — |
| evaluator_name | VARCHAR(128) | YES | — |
| + 公共列 | | | |

**索引**: `idx_mdl_model_eval_model_dataset (model_version_id, dataset_snapshot_id)`

### 15. mdl_model_approval_record

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| model_version_id | BIGINT | NO | — |
| decision_code | VARCHAR(32) | NO | 'PENDING' |
| approver_name | VARCHAR(128) | YES | — |
| decision_note | VARCHAR(1000) | YES | — |
| approved_at | DATETIME | YES | — |
| + 公共列 | | | |

**索引**: `idx_mdl_model_approval_model_decision (model_version_id, decision_code)`

### 16. trn_dataset_snapshot

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| dataset_version | VARCHAR(64) | NO | — (`UNIQUE`) |
| snapshot_type_code | VARCHAR(32) | NO | 'TRAIN' |
| source_summary | VARCHAR(500) | YES | — |
| sample_count | INT | YES | — |
| metadata_json | JSON | YES | — |
| dataset_card_path | VARCHAR(500) | YES | — |
| released_at | DATETIME | YES | — |
| + 公共列 | | | |

**索引**: `idx_trn_dataset_snapshot_type (snapshot_type_code)`

### 17. trn_dataset_sample

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| snapshot_id | BIGINT | NO | — |
| sample_ref_no | VARCHAR(128) | NO | — |
| patient_uuid | VARCHAR(128) | YES | — |
| image_ref_no | VARCHAR(128) | YES | — |
| source_type_code | VARCHAR(32) | NO | 'CORRECTION' |
| split_type_code | VARCHAR(32) | NO | 'TRAIN' |
| label_version | VARCHAR(64) | YES | — |
| org_id | BIGINT | YES | — |
| created_at | DATETIME | NO | — |

**索引**: `idx_trn_dataset_sample_snapshot_ref (snapshot_id, sample_ref_no)`

> **注**: 轻量表,无 status/deleted_flag/remark/updated_*/created_by。

### 18. trn_training_run

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| training_run_no | VARCHAR(64) | NO | — (`UNIQUE`) |
| dataset_snapshot_id | BIGINT | YES | — |
| target_model_code | VARCHAR(64) | NO | — |
| base_model_version | VARCHAR(64) | YES | — |
| run_type_code | VARCHAR(32) | NO | 'TRAIN' |
| parameters_json | JSON | YES | — |
| output_artifact_path | VARCHAR(500) | YES | — |
| metric_json | JSON | YES | — |
| run_status_code | VARCHAR(32) | NO | 'PENDING' |
| started_at | DATETIME | YES | — |
| finished_at | DATETIME | YES | — |
| error_message | VARCHAR(1000) | YES | — |
| + 公共列 | | | |

**索引**:
- `idx_trn_training_run_dataset_status (dataset_snapshot_id, run_status_code)`
- `idx_trn_training_run_model (target_model_code)`

### 19. ann_annotation_record

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| sample_ref_no | VARCHAR(128) | NO | — |
| patient_uuid | VARCHAR(128) | YES | — |
| annotation_version | VARCHAR(64) | NO | — |
| annotation_result_json | JSON | YES | — |
| annotation_object_key | VARCHAR(500) | YES | — |
| annotator_l1 | VARCHAR(128) | YES | — |
| reviewer_l2 | VARCHAR(128) | YES | — |
| qc_status_code | VARCHAR(32) | NO | 'PENDING' |
| difficulty_code | VARCHAR(32) | YES | — |
| + 公共列 | | | |

**索引**:
- `idx_ann_annotation_sample_version (sample_ref_no, annotation_version)`
- `idx_ann_annotation_qc (qc_status_code)`

### 20. ann_gold_set_item

| 列名 | 类型 | NULL | 默认值 |
|---|---|---|---|
| id | BIGINT | NO | AUTO_INCREMENT |
| sample_ref_no | VARCHAR(128) | NO | — (`UNIQUE`) |
| annotation_record_id | BIGINT | YES | — |
| difficulty_code | VARCHAR(32) | YES | — |
| active_flag | CHAR(1) | NO | '1' |
| + 公共列 | | | |

**索引**: `idx_ann_gold_set_active (active_flag)`

---

## 索引完整清单(22 个)

| 序 | 索引名 | 表 | 列 |
|---|---|---|---|
| 1 | idx_ai_infer_job_java_task_no | ai_infer_job | java_task_no |
| 2 | idx_ai_infer_job_case_status | ai_infer_job | case_no, status_code |
| 3 | idx_ai_infer_job_image_job | ai_infer_job_image | job_id, image_id |
| 4 | idx_ai_infer_artifact_job | ai_infer_artifact | job_id, artifact_type_code |
| 5 | idx_ai_callback_log_job | ai_callback_log | job_id, callback_status_code |
| 6 | idx_kb_document_kb_review | kb_document | kb_id, review_status_code |
| 7 | idx_kb_chunk_kb_doc | kb_document_chunk | kb_id, doc_id |
| 8 | idx_kb_rebuild_job_kb | kb_rebuild_job | kb_id, rebuild_status_code |
| 9 | idx_rag_session_type | rag_session | session_type_code |
| 10 | idx_rag_request_session | rag_request_log | session_id, request_type_code |
| 11 | idx_rag_retrieval_request | rag_retrieval_log | request_id, rank_no |
| 12 | idx_llm_call_request | llm_call_log | request_id, model_name |
| 13 | idx_mdl_model_version_type_status | mdl_model_version | model_type_code, status_code |
| 14 | idx_mdl_model_eval_model_dataset | mdl_model_eval_record | model_version_id, dataset_snapshot_id |
| 15 | idx_mdl_model_approval_model_decision | mdl_model_approval_record | model_version_id, decision_code |
| 16 | idx_trn_dataset_snapshot_type | trn_dataset_snapshot | snapshot_type_code |
| 17 | idx_trn_dataset_sample_snapshot_ref | trn_dataset_sample | snapshot_id, sample_ref_no |
| 18 | idx_ann_annotation_sample_version | ann_annotation_record | sample_ref_no, annotation_version |
| 19 | idx_ann_annotation_qc | ann_annotation_record | qc_status_code |
| 20 | idx_ann_gold_set_active | ann_gold_set_item | active_flag |
| 21 | idx_trn_training_run_dataset_status | trn_training_run | dataset_snapshot_id, run_status_code |
| 22 | idx_trn_training_run_model | trn_training_run | target_model_code |

---

## 代码 vs 库一致性结论

代码是当前基线的唯一建表源(`ensure_schema()` 每次启动执行),数据库被动跟随。所有 20 张表、22 个索引、6 个列级 UNIQUE、1 个命名 UNIQUE KEY 均**代码与库一致**,无分歧项。

本基线将作为 Phase 4 Step 4 ORM 模型编写、Step 6 Alembic `0001_baseline_caries_ai.py` 手写迁移的对照依据。任何偏离本文档的结构变更必须先更新本文档再生成迁移。
