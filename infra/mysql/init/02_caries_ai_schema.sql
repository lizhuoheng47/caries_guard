USE `caries_ai`;

CREATE TABLE IF NOT EXISTS `ai_infer_job` (
    `id`                     BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `job_no`                 VARCHAR(64)   NOT NULL UNIQUE              COMMENT '推理任务编号',
    `java_task_no`           VARCHAR(64)   NOT NULL                     COMMENT 'Java端任务编号',
    `case_no`                VARCHAR(64)                                COMMENT '病例编号',
    `patient_uuid`           VARCHAR(128)                               COMMENT '患者UUID',
    `infer_type_code`        VARCHAR(32)   NOT NULL DEFAULT 'ANALYZE'   COMMENT '推理类型',
    `model_version`          VARCHAR(64)   NOT NULL                     COMMENT '模型版本',
    `status_code`            VARCHAR(32)   NOT NULL DEFAULT 'QUEUEING'  COMMENT '任务状态',
    `request_json`           JSON                                       COMMENT '请求JSON',
    `result_json`            JSON                                       COMMENT '结果JSON',
    `error_message`          VARCHAR(1000)                              COMMENT '错误信息',
    `started_at`             DATETIME                                   COMMENT '开始时间',
    `finished_at`            DATETIME                                   COMMENT '完成时间',
    `callback_required_flag` CHAR(1)       NOT NULL DEFAULT '1'         COMMENT '是否需要回调',
    `callback_status_code`   VARCHAR(32)   NOT NULL DEFAULT 'PENDING'   COMMENT '回调状态',
    `org_id`                 BIGINT                                     COMMENT '组织ID',
    `status`                 VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'    COMMENT '记录状态',
    `deleted_flag`           CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '逻辑删除',
    `remark`                 VARCHAR(500)                               COMMENT '备注',
    `created_by`             BIGINT                                     COMMENT '创建人',
    `created_at`             DATETIME      NOT NULL                     COMMENT '创建时间',
    `updated_by`             BIGINT                                     COMMENT '更新人',
    `updated_at`             DATETIME      NOT NULL                     COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI推理任务主表';

CREATE INDEX idx_ai_infer_job_java_task_no   ON `ai_infer_job`(`java_task_no`);
CREATE INDEX idx_ai_infer_job_case_status    ON `ai_infer_job`(`case_no`, `status_code`);


CREATE TABLE IF NOT EXISTS `ai_infer_job_image` (
    `id`                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `job_id`                BIGINT        NOT NULL                      COMMENT '关联推理任务ID',
    `image_id`              BIGINT                                     COMMENT '影像ID',
    `attachment_id`         BIGINT                                     COMMENT '附件ID',
    `image_type_code`       VARCHAR(32)                                COMMENT '影像类型',
    `bucket_name`           VARCHAR(128)                               COMMENT '存储桶名',
    `object_key`            VARCHAR(500)                               COMMENT '对象键',
    `access_url`            TEXT                                       COMMENT '访问URL',
    `url_expire_at`         DATETIME                                   COMMENT 'URL过期时间',
    `download_status_code`  VARCHAR(32)   NOT NULL DEFAULT 'PENDING'   COMMENT '下载状态',
    `local_cache_path`      VARCHAR(500)                               COMMENT '本地缓存路径',
    `quality_status_code`   VARCHAR(32)                                COMMENT '质量状态',
    `grading_label`         VARCHAR(32)                                COMMENT '分级标签',
    `uncertainty_score`     DECIMAL(8,4)                               COMMENT '不确定性分数',
    `result_json`           JSON                                       COMMENT '单图结果JSON',
    `org_id`                BIGINT                                     COMMENT '组织ID',
    `status`                VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'    COMMENT '记录状态',
    `deleted_flag`          CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '逻辑删除',
    `remark`                VARCHAR(500)                               COMMENT '备注',
    `created_by`            BIGINT                                     COMMENT '创建人',
    `created_at`            DATETIME      NOT NULL                     COMMENT '创建时间',
    `updated_by`            BIGINT                                     COMMENT '更新人',
    `updated_at`            DATETIME      NOT NULL                     COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI推理任务影像明细';

CREATE INDEX idx_ai_infer_job_image_job ON `ai_infer_job_image`(`job_id`, `image_id`);


CREATE TABLE IF NOT EXISTS `ai_infer_artifact` (
    `id`                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `job_id`             BIGINT        NOT NULL                     COMMENT '关联推理任务ID',
    `related_image_id`   BIGINT                                    COMMENT '关联影像ID',
    `artifact_type_code` VARCHAR(32)   NOT NULL                    COMMENT '产物类型',
    `bucket_name`        VARCHAR(128)  NOT NULL                    COMMENT '存储桶名',
    `object_key`         VARCHAR(500)  NOT NULL                    COMMENT '对象键',
    `content_type`       VARCHAR(128)                              COMMENT '内容类型',
    `file_size_bytes`    BIGINT                                    COMMENT '文件大小',
    `md5`                VARCHAR(64)                               COMMENT 'MD5校验',
    `model_version`      VARCHAR(64)                               COMMENT '模型版本',
    `attachment_id`      BIGINT                                    COMMENT '附件ID',
    `ext_json`           JSON                                      COMMENT '扩展JSON',
    `org_id`             BIGINT                                    COMMENT '组织ID',
    `status`             VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`       CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`             VARCHAR(500)                              COMMENT '备注',
    `created_by`         BIGINT                                    COMMENT '创建人',
    `created_at`         DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`         BIGINT                                    COMMENT '更新人',
    `updated_at`         DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI推理产物表';

CREATE INDEX idx_ai_infer_artifact_job ON `ai_infer_artifact`(`job_id`, `artifact_type_code`);


CREATE TABLE IF NOT EXISTS `ai_callback_log` (
    `id`                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `job_id`               BIGINT        NOT NULL                     COMMENT '关联推理任务ID',
    `callback_url`         VARCHAR(500)  NOT NULL                     COMMENT '回调URL',
    `request_json`         JSON                                       COMMENT '请求JSON',
    `response_code`        INT                                        COMMENT 'HTTP响应码',
    `response_body`        TEXT                                       COMMENT '响应体',
    `callback_status_code` VARCHAR(32)   NOT NULL DEFAULT 'PENDING'   COMMENT '回调状态',
    `retry_count`          INT           NOT NULL DEFAULT 0           COMMENT '重试次数',
    `next_retry_at`        DATETIME                                   COMMENT '下次重试时间',
    `error_message`        VARCHAR(1000)                              COMMENT '错误信息',
    `trace_id`             VARCHAR(128)                               COMMENT '追踪ID',
    `org_id`               BIGINT                                     COMMENT '组织ID',
    `status`               VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'    COMMENT '记录状态',
    `deleted_flag`         CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '逻辑删除',
    `remark`               VARCHAR(500)                               COMMENT '备注',
    `created_by`           BIGINT                                     COMMENT '创建人',
    `created_at`           DATETIME      NOT NULL                     COMMENT '创建时间',
    `updated_by`           BIGINT                                     COMMENT '更新人',
    `updated_at`           DATETIME      NOT NULL                     COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI回调日志表';

CREATE INDEX idx_ai_callback_log_job ON `ai_callback_log`(`job_id`, `callback_status_code`);

CREATE TABLE IF NOT EXISTS `kb_knowledge_base` (
    `id`                       BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `kb_code`                  VARCHAR(64)   NOT NULL UNIQUE              COMMENT '知识库编码',
    `kb_name`                  VARCHAR(128)  NOT NULL                     COMMENT '知识库名称',
    `kb_type_code`             VARCHAR(32)   NOT NULL DEFAULT 'PATIENT_GUIDE' COMMENT '知识库类型',
    `knowledge_version`        VARCHAR(64)   NOT NULL DEFAULT 'v1.0'     COMMENT '知识库版本',
    `embedding_model`          VARCHAR(64)                               COMMENT '向量化模型',
    `vector_store_type_code`   VARCHAR(32)   NOT NULL DEFAULT 'LOCAL_JSON' COMMENT '向量存储类型',
    `vector_store_path`        VARCHAR(500)                              COMMENT '向量存储路径',
    `enabled_flag`             CHAR(1)       NOT NULL DEFAULT '1'        COMMENT '启用标志',
    `status_code`              VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '业务状态',
    `org_id`                   BIGINT                                    COMMENT '组织ID',
    `status`                   VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`             CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`                   VARCHAR(500)                              COMMENT '备注',
    `created_by`               BIGINT                                    COMMENT '创建人',
    `created_at`               DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`               BIGINT                                    COMMENT '更新人',
    `updated_at`               DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库主表';


CREATE TABLE IF NOT EXISTS `kb_document` (
    `id`                       BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `kb_id`                    BIGINT        NOT NULL                     COMMENT '关联知识库ID',
    `doc_no`                   VARCHAR(64)   NOT NULL UNIQUE              COMMENT '文档编号',
    `doc_title`                VARCHAR(255)  NOT NULL                     COMMENT '文档标题',
    `doc_source_code`          VARCHAR(32)   NOT NULL DEFAULT 'INTERNAL'  COMMENT '文档来源',
    `source_uri`               VARCHAR(500)                              COMMENT '来源URI',
    `doc_version`              VARCHAR(64)   NOT NULL DEFAULT 'v1.0'     COMMENT '文档版本',
    `content_text`             LONGTEXT                                  COMMENT '文档内容',
    `content_attachment_key`   VARCHAR(500)                              COMMENT '附件对象键',
    `review_status_code`       VARCHAR(32)   NOT NULL DEFAULT 'PENDING'  COMMENT '审核状态',
    `reviewer_id`              BIGINT                                    COMMENT '审核人ID',
    `reviewed_at`              DATETIME                                  COMMENT '审核时间',
    `enabled_flag`             CHAR(1)       NOT NULL DEFAULT '1'        COMMENT '启用标志',
    `org_id`                   BIGINT                                    COMMENT '组织ID',
    `status`                   VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`             CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`                   VARCHAR(500)                              COMMENT '备注',
    `created_by`               BIGINT                                    COMMENT '创建人',
    `created_at`               DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`               BIGINT                                    COMMENT '更新人',
    `updated_at`               DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

CREATE INDEX idx_kb_document_kb_review ON `kb_document`(`kb_id`, `review_status_code`);


CREATE TABLE IF NOT EXISTS `kb_document_chunk` (
    `id`                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `kb_id`              BIGINT        NOT NULL                     COMMENT '关联知识库ID',
    `doc_id`             BIGINT        NOT NULL                     COMMENT '关联文档ID',
    `chunk_no`           INT           NOT NULL                     COMMENT '分块序号',
    `chunk_text`         TEXT          NOT NULL                     COMMENT '分块文本',
    `token_count`        INT                                        COMMENT 'Token数量',
    `embedding_model`    VARCHAR(64)                                COMMENT '向量化模型',
    `vector_store_path`  VARCHAR(500)                               COMMENT '向量存储路径',
    `vector_id`          VARCHAR(128)                               COMMENT '向量ID',
    `enabled_flag`       CHAR(1)       NOT NULL DEFAULT '1'         COMMENT '启用标志',
    `org_id`             BIGINT                                     COMMENT '组织ID',
    `status`             VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'    COMMENT '记录状态',
    `deleted_flag`       CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '逻辑删除',
    `created_at`         DATETIME      NOT NULL                     COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档分块表';

CREATE INDEX idx_kb_chunk_kb_doc ON `kb_document_chunk`(`kb_id`, `doc_id`);


CREATE TABLE IF NOT EXISTS `kb_rebuild_job` (
    `id`                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `rebuild_job_no`       VARCHAR(64)   NOT NULL UNIQUE              COMMENT '重建任务编号',
    `kb_id`                BIGINT        NOT NULL                     COMMENT '关联知识库ID',
    `knowledge_version`    VARCHAR(64)   NOT NULL                     COMMENT '知识库版本',
    `rebuild_status_code`  VARCHAR(32)   NOT NULL DEFAULT 'RUNNING'   COMMENT '重建状态',
    `chunk_count`          INT           NOT NULL DEFAULT 0           COMMENT '分块总数',
    `vector_store_path`    VARCHAR(500)                               COMMENT '向量存储路径',
    `started_at`           DATETIME                                   COMMENT '开始时间',
    `finished_at`          DATETIME                                   COMMENT '完成时间',
    `error_message`        VARCHAR(1000)                              COMMENT '错误信息',
    `org_id`               BIGINT                                     COMMENT '组织ID',
    `status`               VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'    COMMENT '记录状态',
    `deleted_flag`         CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '逻辑删除',
    `remark`               VARCHAR(500)                               COMMENT '备注',
    `created_by`           BIGINT                                     COMMENT '创建人',
    `created_at`           DATETIME      NOT NULL                     COMMENT '创建时间',
    `updated_by`           BIGINT                                     COMMENT '更新人',
    `updated_at`           DATETIME      NOT NULL                     COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库重建任务表';

CREATE INDEX idx_kb_rebuild_job_kb ON `kb_rebuild_job`(`kb_id`, `rebuild_status_code`);


CREATE TABLE IF NOT EXISTS `rag_session` (
    `id`                  BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `session_no`          VARCHAR(64)   NOT NULL UNIQUE              COMMENT '会话编号',
    `session_type_code`   VARCHAR(32)   NOT NULL                    COMMENT '会话类型',
    `related_biz_no`      VARCHAR(64)                               COMMENT '关联业务编号',
    `patient_uuid`        VARCHAR(128)                              COMMENT '患者UUID',
    `java_user_id`        BIGINT                                    COMMENT 'Java端用户ID',
    `knowledge_version`   VARCHAR(64)                               COMMENT '知识库版本',
    `model_name`          VARCHAR(64)                               COMMENT '模型名称',
    `org_id`              BIGINT                                    COMMENT '组织ID',
    `status`              VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`        CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`              VARCHAR(500)                              COMMENT '备注',
    `created_by`          BIGINT                                    COMMENT '创建人',
    `created_at`          DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`          BIGINT                                    COMMENT '更新人',
    `updated_at`          DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG会话表';

CREATE INDEX idx_rag_session_type ON `rag_session`(`session_type_code`);


CREATE TABLE IF NOT EXISTS `rag_request_log` (
    `id`                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `session_id`           BIGINT        NOT NULL                     COMMENT '关联会话ID',
    `request_no`           VARCHAR(64)   NOT NULL UNIQUE              COMMENT '请求编号',
    `request_type_code`    VARCHAR(32)   NOT NULL                     COMMENT '请求类型',
    `user_query`           TEXT          NOT NULL                     COMMENT '用户问题',
    `rewritten_query`      TEXT                                       COMMENT '改写后问题',
    `top_k`                INT           NOT NULL DEFAULT 5           COMMENT '召回数量',
    `answer_text`          LONGTEXT                                   COMMENT '回答文本',
    `request_status_code`  VARCHAR(32)   NOT NULL DEFAULT 'SUCCESS'   COMMENT '请求状态',
    `safety_flag`          CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '安全标记',
    `latency_ms`           INT                                        COMMENT '延迟毫秒',
    `org_id`               BIGINT                                     COMMENT '组织ID',
    `status`               VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'    COMMENT '记录状态',
    `deleted_flag`         CHAR(1)       NOT NULL DEFAULT '0'         COMMENT '逻辑删除',
    `remark`               VARCHAR(500)                               COMMENT '备注',
    `created_by`           BIGINT                                     COMMENT '创建人',
    `created_at`           DATETIME      NOT NULL                     COMMENT '创建时间',
    `updated_by`           BIGINT                                     COMMENT '更新人',
    `updated_at`           DATETIME      NOT NULL                     COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG请求日志表';

CREATE INDEX idx_rag_request_session ON `rag_request_log`(`session_id`, `request_type_code`);


CREATE TABLE IF NOT EXISTS `rag_retrieval_log` (
    `id`                  BIGINT         PRIMARY KEY AUTO_INCREMENT,
    `request_id`          BIGINT         NOT NULL                    COMMENT '关联请求ID',
    `chunk_id`            BIGINT         NOT NULL                    COMMENT '分块ID',
    `rank_no`             INT            NOT NULL                    COMMENT '排名序号',
    `retrieval_score`     DECIMAL(10,6)                              COMMENT '检索分数',
    `doc_id`              BIGINT         NOT NULL                    COMMENT '文档ID',
    `chunk_text_snapshot` TEXT                                       COMMENT '分块文本快照',
    `cited_flag`          CHAR(1)        NOT NULL DEFAULT '0'        COMMENT '是否被引用',
    `org_id`              BIGINT                                     COMMENT '组织ID',
    `created_at`          DATETIME       NOT NULL                    COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG检索日志表';

CREATE INDEX idx_rag_retrieval_request ON `rag_retrieval_log`(`request_id`, `rank_no`);


CREATE TABLE IF NOT EXISTS `llm_call_log` (
    `id`                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `request_id`         BIGINT        NOT NULL                     COMMENT '关联请求ID',
    `model_name`         VARCHAR(128)  NOT NULL                     COMMENT '模型名称',
    `provider_code`      VARCHAR(64)                                COMMENT '服务商编码',
    `prompt_text`        LONGTEXT                                   COMMENT '提示词文本',
    `completion_text`    LONGTEXT                                   COMMENT '生成文本',
    `prompt_tokens`      INT                                        COMMENT '提示词Token数',
    `completion_tokens`  INT                                        COMMENT '生成Token数',
    `total_tokens`       INT                                        COMMENT '总Token数',
    `latency_ms`         INT                                        COMMENT '延迟毫秒',
    `call_status_code`   VARCHAR(32)   NOT NULL DEFAULT 'SUCCESS'   COMMENT '调用状态',
    `error_message`      VARCHAR(1000)                              COMMENT '错误信息',
    `org_id`             BIGINT                                     COMMENT '组织ID',
    `created_at`         DATETIME      NOT NULL                     COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM调用日志表';

CREATE INDEX idx_llm_call_request ON `llm_call_log`(`request_id`, `model_name`);

CREATE TABLE IF NOT EXISTS `mdl_model_version` (
    `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `model_code`       VARCHAR(64)   NOT NULL                         COMMENT '模型编码',
    `model_name`       VARCHAR(128)  NOT NULL                         COMMENT '模型名称',
    `model_type_code`  VARCHAR(32)   NOT NULL DEFAULT 'SEGMENTATION'  COMMENT '模型类型',
    `version_no`       VARCHAR(64)   NOT NULL                         COMMENT '版本号',
    `artifact_path`    VARCHAR(500)                                   COMMENT '产物路径',
    `dataset_version`  VARCHAR(64)                                    COMMENT '数据集版本',
    `metrics_json`     JSON                                           COMMENT '评估指标JSON',
    `status_code`      VARCHAR(32)   NOT NULL DEFAULT 'CANDIDATE'     COMMENT '版本状态',
    `active_flag`      CHAR(1)       NOT NULL DEFAULT '0'             COMMENT '激活标志',
    `published_at`     DATETIME                                       COMMENT '发布时间',
    `org_id`           BIGINT                                         COMMENT '组织ID',
    `status`           VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'        COMMENT '记录状态',
    `deleted_flag`     CHAR(1)       NOT NULL DEFAULT '0'             COMMENT '逻辑删除',
    `remark`           VARCHAR(500)                                   COMMENT '备注',
    `created_by`       BIGINT                                         COMMENT '创建人',
    `created_at`       DATETIME      NOT NULL                         COMMENT '创建时间',
    `updated_by`       BIGINT                                         COMMENT '更新人',
    `updated_at`       DATETIME      NOT NULL                         COMMENT '更新时间',
    UNIQUE KEY `uk_mdl_model_version_code_version` (`model_code`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型版本表';

CREATE INDEX idx_mdl_model_version_type_status ON `mdl_model_version`(`model_type_code`, `status_code`);


CREATE TABLE IF NOT EXISTS `mdl_model_eval_record` (
    `id`                        BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `model_version_id`          BIGINT        NOT NULL                    COMMENT '关联模型版本ID',
    `dataset_snapshot_id`       BIGINT                                   COMMENT '关联数据集快照ID',
    `eval_type_code`            VARCHAR(32)   NOT NULL DEFAULT 'OFFLINE' COMMENT '评估类型',
    `metric_json`               JSON                                     COMMENT '评估指标JSON',
    `error_case_json`           JSON                                     COMMENT '错误案例JSON',
    `evidence_attachment_key`   VARCHAR(500)                             COMMENT '证据附件键',
    `evaluated_at`              DATETIME      NOT NULL                   COMMENT '评估时间',
    `evaluator_name`            VARCHAR(128)                             COMMENT '评估人',
    `org_id`                    BIGINT                                   COMMENT '组织ID',
    `status`                    VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'  COMMENT '记录状态',
    `deleted_flag`              CHAR(1)       NOT NULL DEFAULT '0'       COMMENT '逻辑删除',
    `remark`                    VARCHAR(500)                             COMMENT '备注',
    `created_by`                BIGINT                                   COMMENT '创建人',
    `created_at`                DATETIME      NOT NULL                   COMMENT '创建时间',
    `updated_by`                BIGINT                                   COMMENT '更新人',
    `updated_at`                DATETIME      NOT NULL                   COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型评估记录表';

CREATE INDEX idx_mdl_model_eval_model_dataset ON `mdl_model_eval_record`(`model_version_id`, `dataset_snapshot_id`);


CREATE TABLE IF NOT EXISTS `mdl_model_approval_record` (
    `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `model_version_id` BIGINT        NOT NULL                      COMMENT '关联模型版本ID',
    `decision_code`    VARCHAR(32)   NOT NULL DEFAULT 'PENDING'    COMMENT '审批决策',
    `approver_name`    VARCHAR(128)                                COMMENT '审批人',
    `decision_note`    VARCHAR(1000)                               COMMENT '审批说明',
    `approved_at`      DATETIME                                    COMMENT '审批时间',
    `org_id`           BIGINT                                      COMMENT '组织ID',
    `status`           VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'     COMMENT '记录状态',
    `deleted_flag`     CHAR(1)       NOT NULL DEFAULT '0'          COMMENT '逻辑删除',
    `remark`           VARCHAR(500)                                COMMENT '备注',
    `created_by`       BIGINT                                      COMMENT '创建人',
    `created_at`       DATETIME      NOT NULL                      COMMENT '创建时间',
    `updated_by`       BIGINT                                      COMMENT '更新人',
    `updated_at`       DATETIME      NOT NULL                      COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型审批记录表';

CREATE INDEX idx_mdl_model_approval_model_decision ON `mdl_model_approval_record`(`model_version_id`, `decision_code`);


CREATE TABLE IF NOT EXISTS `trn_dataset_snapshot` (
    `id`                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `dataset_version`    VARCHAR(64)   NOT NULL UNIQUE              COMMENT '数据集版本',
    `snapshot_type_code` VARCHAR(32)   NOT NULL DEFAULT 'TRAIN'    COMMENT '快照类型',
    `source_summary`     VARCHAR(500)                              COMMENT '来源摘要',
    `sample_count`       INT                                       COMMENT '样本数量',
    `metadata_json`      JSON                                      COMMENT '元数据JSON',
    `dataset_card_path`  VARCHAR(500)                              COMMENT '数据卡路径',
    `released_at`        DATETIME                                  COMMENT '发布时间',
    `org_id`             BIGINT                                    COMMENT '组织ID',
    `status`             VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`       CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`             VARCHAR(500)                              COMMENT '备注',
    `created_by`         BIGINT                                    COMMENT '创建人',
    `created_at`         DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`         BIGINT                                    COMMENT '更新人',
    `updated_at`         DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练数据集快照表';

CREATE INDEX idx_trn_dataset_snapshot_type ON `trn_dataset_snapshot`(`snapshot_type_code`);


CREATE TABLE IF NOT EXISTS `trn_dataset_sample` (
    `id`                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `snapshot_id`       BIGINT        NOT NULL                       COMMENT '关联快照ID',
    `sample_ref_no`     VARCHAR(128)  NOT NULL                       COMMENT '样本引用编号',
    `patient_uuid`      VARCHAR(128)                                 COMMENT '患者UUID',
    `image_ref_no`      VARCHAR(128)                                 COMMENT '影像引用编号',
    `source_type_code`  VARCHAR(32)   NOT NULL DEFAULT 'CORRECTION'  COMMENT '来源类型',
    `split_type_code`   VARCHAR(32)   NOT NULL DEFAULT 'TRAIN'       COMMENT '数据集划分',
    `label_version`     VARCHAR(64)                                  COMMENT '标签版本',
    `org_id`            BIGINT                                       COMMENT '组织ID',
    `created_at`        DATETIME      NOT NULL                       COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练数据集样本表';

CREATE INDEX idx_trn_dataset_sample_snapshot_ref ON `trn_dataset_sample`(`snapshot_id`, `sample_ref_no`);


CREATE TABLE IF NOT EXISTS `ann_annotation_record` (
    `id`                       BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `sample_ref_no`            VARCHAR(128)  NOT NULL                    COMMENT '样本引用编号',
    `patient_uuid`             VARCHAR(128)                              COMMENT '患者UUID',
    `annotation_version`       VARCHAR(64)   NOT NULL                    COMMENT '标注版本',
    `annotation_result_json`   JSON                                      COMMENT '标注结果JSON',
    `annotation_object_key`    VARCHAR(500)                              COMMENT '标注对象键',
    `annotator_l1`             VARCHAR(128)                              COMMENT '一级标注员',
    `reviewer_l2`              VARCHAR(128)                              COMMENT '二级审核员',
    `qc_status_code`           VARCHAR(32)   NOT NULL DEFAULT 'PENDING'  COMMENT 'QC状态',
    `difficulty_code`          VARCHAR(32)                               COMMENT '难度编码',
    `org_id`                   BIGINT                                    COMMENT '组织ID',
    `status`                   VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`             CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`                   VARCHAR(500)                              COMMENT '备注',
    `created_by`               BIGINT                                    COMMENT '创建人',
    `created_at`               DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`               BIGINT                                    COMMENT '更新人',
    `updated_at`               DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标注记录表';

CREATE INDEX idx_ann_annotation_sample_version ON `ann_annotation_record`(`sample_ref_no`, `annotation_version`);
CREATE INDEX idx_ann_annotation_qc             ON `ann_annotation_record`(`qc_status_code`);


CREATE TABLE IF NOT EXISTS `ann_gold_set_item` (
    `id`                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `sample_ref_no`         VARCHAR(128)  NOT NULL UNIQUE              COMMENT '样本引用编号',
    `annotation_record_id`  BIGINT                                    COMMENT '关联标注记录ID',
    `difficulty_code`       VARCHAR(32)                               COMMENT '难度编码',
    `active_flag`           CHAR(1)       NOT NULL DEFAULT '1'        COMMENT '激活标志',
    `org_id`                BIGINT                                    COMMENT '组织ID',
    `status`                VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`          CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`                VARCHAR(500)                              COMMENT '备注',
    `created_by`            BIGINT                                    COMMENT '创建人',
    `created_at`            DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`            BIGINT                                    COMMENT '更新人',
    `updated_at`            DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标注金标准集表';

CREATE INDEX idx_ann_gold_set_active ON `ann_gold_set_item`(`active_flag`);


CREATE TABLE IF NOT EXISTS `trn_training_run` (
    `id`                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `training_run_no`       VARCHAR(64)   NOT NULL UNIQUE              COMMENT '训练运行编号',
    `dataset_snapshot_id`   BIGINT                                    COMMENT '关联数据集快照ID',
    `target_model_code`     VARCHAR(64)   NOT NULL                    COMMENT '目标模型编码',
    `base_model_version`    VARCHAR(64)                               COMMENT '基线模型版本',
    `run_type_code`         VARCHAR(32)   NOT NULL DEFAULT 'TRAIN'   COMMENT '运行类型',
    `parameters_json`       JSON                                      COMMENT '训练参数JSON',
    `output_artifact_path`  VARCHAR(500)                              COMMENT '输出产物路径',
    `metric_json`           JSON                                      COMMENT '训练指标JSON',
    `run_status_code`       VARCHAR(32)   NOT NULL DEFAULT 'PENDING'  COMMENT '运行状态',
    `started_at`            DATETIME                                  COMMENT '开始时间',
    `finished_at`           DATETIME                                  COMMENT '完成时间',
    `error_message`         VARCHAR(1000)                             COMMENT '错误信息',
    `org_id`                BIGINT                                    COMMENT '组织ID',
    `status`                VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE'   COMMENT '记录状态',
    `deleted_flag`          CHAR(1)       NOT NULL DEFAULT '0'        COMMENT '逻辑删除',
    `remark`                VARCHAR(500)                              COMMENT '备注',
    `created_by`            BIGINT                                    COMMENT '创建人',
    `created_at`            DATETIME      NOT NULL                    COMMENT '创建时间',
    `updated_by`            BIGINT                                    COMMENT '更新人',
    `updated_at`            DATETIME      NOT NULL                    COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练运行表';

CREATE INDEX idx_trn_training_run_dataset_status ON `trn_training_run`(`dataset_snapshot_id`, `run_status_code`);
CREATE INDEX idx_trn_training_run_model          ON `trn_training_run`(`target_model_code`);

-- 默认 AI 模型版本 (caries-v1)
INSERT INTO `mdl_model_version`
    (`model_code`, `model_name`, `model_type_code`, `version_no`,
     `status_code`, `active_flag`, `org_id`, `created_at`, `updated_at`)
VALUES
    ('caries-seg', 'CariesGuard Segmentation Model', 'SEGMENTATION', 'v1.0',
     'PUBLISHED', '1', NULL, NOW(), NOW());

-- 默认知识库 (caries-default)
INSERT INTO `kb_knowledge_base`
    (`kb_code`, `kb_name`, `kb_type_code`, `knowledge_version`,
     `embedding_model`, `vector_store_type_code`, `vector_store_path`,
     `enabled_flag`, `status_code`, `org_id`, `created_at`, `updated_at`)
VALUES
    ('caries-default', 'CariesGuard Default Knowledge Base', 'PATIENT_GUIDE', 'v1.0',
     'hashing-embedding-v1', 'LOCAL_JSON', '/tmp/cariesguard/vector-index/caries-default.json',
     '1', 'ACTIVE', NULL, NOW(), NOW());

-- 默认知识库示例文档 (龋齿基础知识)
INSERT INTO `kb_document`
    (`kb_id`, `doc_no`, `doc_title`, `doc_source_code`, `doc_version`,
     `content_text`, `review_status_code`, `enabled_flag`, `created_at`, `updated_at`)
VALUES
    (1, 'DOC-CARIES-BASICS-001', '龋齿基础知识', 'INTERNAL', 'v1.0',
     '龋齿（dental caries）是由口腔中的细菌在牙齿表面产酸，导致牙齿硬组织脱矿和破坏的慢性感染性疾病。'
     '主要致病菌为变异链球菌（Streptococcus mutans）。龋齿根据病变深度分为浅龋、中龋和深龋。'
     '浅龋仅累及牙釉质，患者通常无自觉症状；中龋累及牙本质浅层，可出现冷热酸甜刺激痛；'
     '深龋累及牙本质深层，接近牙髓，可有明显的刺激痛和自发痛。'
     '预防措施包括：每日两次含氟牙膏刷牙、使用牙线清洁邻面、定期口腔检查、合理饮食控制糖摄入、窝沟封闭等。'
     '治疗方法根据龋坏程度不同，包括再矿化治疗、充填治疗、嵌体修复和冠修复等。',
     'APPROVED', '1', NOW(), NOW()),
    (1, 'DOC-CHILD-CARIES-002', '儿童龋齿防治指南', 'INTERNAL', 'v1.0',
     '儿童龋齿（乳牙龋）是学龄前儿童最常见的慢性口腔疾病，患病率在部分地区高达70%以上。'
     '乳牙龋如不及时治疗，可影响恒牙发育、咀嚼功能和颌面部正常发育。'
     '早期龋齿表现为牙面白垩色斑点，进展后出现褐色或黑色龋洞。'
     '推荐6月龄起进行口腔健康检查，建立口腔健康档案。'
     '3岁以下儿童使用米粒大小含氟牙膏，3-6岁使用豌豆大小含氟牙膏。'
     '乳磨牙窝沟封闭推荐在3-4岁完成，第一恒磨牙窝沟封闭推荐在6-7岁完成。'
     '家长应协助刷牙至8岁左右，确保后牙面充分清洁。',
     'APPROVED', '1', NOW(), NOW()),
    (1, 'DOC-PATIENT-FAQ-003', '患者常见问题解答', 'INTERNAL', 'v1.0',
     'Q: 什么是龋齿？A: 龋齿俗称"蛀牙"或"虫牙"，是牙齿被口腔细菌侵蚀后形成的孔洞。它不是虫子咬的，而是细菌分解食物残渣产生酸性物质腐蚀牙齿造成的。'
     'Q: 龋齿一定要治疗吗？A: 是的。龋齿不会自愈，只会越来越严重。如不治疗，最终会侵犯牙髓引起剧烈疼痛，甚至导致牙齿丧失。早期治疗简单且费用低。'
     'Q: 补牙疼吗？A: 现代补牙技术在局部麻醉下进行，过程基本无痛。浅龋充填甚至不需要麻醉。'
     'Q: 多久做一次口腔检查？A: 建议每6个月进行一次口腔检查和专业清洁。有龋齿高风险的人群建议每3个月检查一次。'
     'Q: 如何降低龋齿风险？A: 每天刷牙两次、使用含氟牙膏、减少含糖食物、定期看牙医、必要时进行窝沟封闭和氟化物涂布。',
     'APPROVED', '1', NOW(), NOW());
