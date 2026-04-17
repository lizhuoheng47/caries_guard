# Python AI 与 RAG 数据库设计与数据字典

> 项目：多模态龋齿智能识别与分级预警平台（CariesGuard）
> 
> 文档性质：Python AI 服务、RAG 知识库、模型治理与训练治理数据库设计与数据字典
> 
> 适用对象：Python 开发工程师、算法工程师、数据库设计人员、测试工程师、项目负责人、AI 编码工具（Codex / Gemini / Claude Code）
> 
> 当前版本：v1.0
> 
> 设计边界：**本文仅覆盖 Python AI 运行层、RAG 文本智能层、模型治理层与训练治理层，不覆盖 Java 业务平台数据库。**

---

## 目录

1. [文档目标与设计边界](#1-文档目标与设计边界)
2. [数据库设计总原则](#2-数据库设计总原则)
3. [数据域划分](#3-数据域划分)
4. [全表清单总览](#4-全表清单总览)
5. [统一字段设计约定](#5-统一字段设计约定)
6. [AI 运行域数据字典](#6-ai-运行域数据字典)
7. [RAG 与知识库域数据字典](#7-rag-与知识库域数据字典)
8. [模型与训练治理域数据字典](#8-模型与训练治理域数据字典)
9. [数据生命周期与留存策略](#9-数据生命周期与留存策略)
10. [数据库部署与实现建议](#10-数据库部署与实现建议)
11. [实施建议与落地顺序](#11-实施建议与落地顺序)

---

# 1. 文档目标与设计边界

## 1.1 本文档的目标

本文档用于为 Python 端提供**可直接实施的数据库设计依据**，解决以下问题：

1. 明确 Python 端到底该存什么，不该存什么；
2. 把 AI 运行、RAG 知识库、模型版本、训练快照等元数据统一纳入一套结构；
3. 避免 Python 端反向侵入 Java 业务数据库；
4. 为 Alembic 迁移、ORM 设计、接口联调和答辩演示提供统一依据；
5. 将原本分散的 AI / RAG / 训练治理表结构收敛成一套独立文档。

## 1.2 本文档覆盖范围

本文档覆盖以下内容：

- Python AI 运行态任务与回调元数据；
- visual asset 与中间产物元数据；
- 知识库、文档、分片、索引重建任务；
- RAG 请求、检索、生成日志；
- 模型版本、评估记录、审批记录；
- 数据快照、训练样本、训练运行记录；
- Gold Set 与标注结果的治理侧最小元数据。

## 1.3 本文档明确不覆盖的内容

以下内容不在本文档范围内：

- 患者主档、监护人、就诊、病例主表；
- Java 业务侧报告、随访、消息通知主表；
- Java 业务侧 analysis 快照表（如 `ana_task_record`、`ana_result_summary` 等）；
- 前端页面状态缓存；
- 原始影像文件本体与大体量二进制对象存储。

## 1.4 与 Java 数据库文档的关系

Java 与 Python 的数据库文档应严格分册：

- **Java 业务平台数据库设计与数据字典**：负责业务运行主线；
- **Python AI 与 RAG 数据库设计与数据字典**：负责 AI 运行元数据、知识库元数据与训练治理元数据。

二者通过以下方式衔接：

1. Java 通过 MQ / HTTP 把 `taskNo / caseNo / image accessUrl / risk factors` 下发给 Python；
2. Python 把运行结果通过 callback 回给 Java；
3. Java 存业务消费快照，Python 存运行元数据与治理元数据；
4. 训练数据从业务侧脱敏导出后，进入 Python 治理域，而不是直接使用业务表。

---

# 2. 数据库设计总原则

## 2.1 核心原则

1. **运行元数据与业务结果分离**：Python 只存运行日志与治理数据，不替代 Java 的业务结果承载；
2. **最小必要原则**：不在 Python 库里存患者姓名、电话、证件号等高敏信息；
3. **可追溯原则**：每次推理、检索、生成都能追溯到任务号、模型版本、知识版本和时间；
4. **知识受控原则**：进入知识库的文档必须有来源、审核状态和版本；
5. **训练治理可审计原则**：候选模型不能“口头上线”，必须有评估、审批和状态记录；
6. **轻量优先原则**：比赛阶段优先采用 MySQL 元数据 + FAISS 索引文件，不强引入额外复杂中间件；
7. **边界清晰原则**：Python 库中不复制 Java 业务主线，只保存足以支撑 AI 与 RAG 的元数据。

## 2.2 数据建模层次

本文档同样分三层：

| 层次 | 目标 | 本次是否覆盖 |
|---|---|---|
| 概念数据模型 | 定义 Python 端有哪些核心实体及其关系 | 是 |
| 逻辑数据模型 | 定义表、字段、主外键、代码集、约束 | 是 |
| 物理数据模型 | Alembic / MySQL 建表、索引、字符集、参数 | 部分覆盖，暂不写 SQL |

---

# 3. 数据域划分

建议按以下 3 个核心数据域组织 Python 侧数据库：

| 数据域 | 说明 | 代表对象 |
|---|---|---|
| AI 运行域 | 推理任务、任务影像、产物、回调日志 | ai_infer_job、ai_infer_job_image |
| RAG 与知识库域 | 知识库、文档、分片、索引重建、问答与检索日志 | kb_document、rag_request_log |
| 模型与训练治理域 | 模型版本、评估、审批、数据快照、训练运行 | mdl_model_version、trn_dataset_snapshot |

---

# 4. 全表清单总览

| 序号 | 表名 | 中文名 | 数据域 | 作用 |
|---|---|---|---|---|
| 1 | `ai_infer_job` | AI 推理任务表 | AI 运行域 | 记录一次 Python 端实际接收与执行的推理任务主信息。 |
| 2 | `ai_infer_job_image` | 推理任务影像表 | AI 运行域 | 记录任务对应的影像列表、下载信息与影像级处理结果。 |
| 3 | `ai_infer_artifact` | 推理产物表 | AI 运行域 | 记录 mask、overlay、heatmap 等中间/最终产物元数据。 |
| 4 | `ai_callback_log` | Java 回调日志表 | AI 运行域 | 记录 Python 回调 Java 的请求体、结果、重试与失败原因。 |
| 5 | `kb_knowledge_base` | 知识库表 | RAG 与知识库域 | 记录一个知识库集合的定义、用途、状态与版本信息。 |
| 6 | `kb_document` | 知识文档表 | RAG 与知识库域 | 记录单篇知识文档的来源、标题、状态、审核与版本。 |
| 7 | `kb_document_chunk` | 知识分片表 | RAG 与知识库域 | 记录知识文档切分后的片段与其索引定位信息。 |
| 8 | `kb_rebuild_job` | 知识索引重建任务表 | RAG 与知识库域 | 记录知识切分、embedding 与索引重建任务。 |
| 9 | `rag_session` | RAG 会话表 | RAG 与知识库域 | 记录一次患者解释或医生问答会话的上下文信息。 |
| 10 | `rag_request_log` | RAG 请求日志表 | RAG 与知识库域 | 记录一次完整的检索增强生成请求。 |
| 11 | `rag_retrieval_log` | RAG 检索命中表 | RAG 与知识库域 | 记录某次请求命中的知识分片明细与排序得分。 |
| 12 | `llm_call_log` | 通用大模型调用日志表 | RAG 与知识库域 | 记录外部 LLM 调用参数、耗时、token 与结果摘要。 |
| 13 | `mdl_model_version` | 模型版本表 | 模型与训练治理域 | 记录视觉模型、风险模型、embedding 模型等版本信息。 |
| 14 | `mdl_model_eval_record` | 模型评估记录表 | 模型与训练治理域 | 记录候选模型的离线评估指标和证据材料。 |
| 15 | `mdl_model_approval_record` | 模型审批记录表 | 模型与训练治理域 | 记录候选模型的人工审批与上线决策。 |
| 16 | `trn_dataset_snapshot` | 数据快照表 | 模型与训练治理域 | 记录一次训练/评估使用的数据快照版本。 |
| 17 | `trn_dataset_sample` | 数据快照样本明细表 | 模型与训练治理域 | 记录某个快照包含的样本清单与来源。 |
| 18 | `ann_annotation_record` | 标注记录表 | 模型与训练治理域 | 记录标注版本、标注者、复核者与质控状态。 |
| 19 | `ann_gold_set_item` | Gold Set 样本表 | 模型与训练治理域 | 记录高置信度金标准样本及其状态。 |
| 20 | `trn_training_run` | 训练运行记录表 | 模型与训练治理域 | 记录一次训练或评估任务的执行过程与结果。 |

---

# 5. 统一字段设计约定

## 5.1 公共字段约定

建议除纯关联表外，统一包含以下公共字段：

| 字段 | 类型建议 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `org_id` | BIGINT | 所属机构，便于隔离与追踪 |
| `status` | VARCHAR(32) | 记录状态 |
| `deleted_flag` | CHAR(1) | 逻辑删除标记 |
| `remark` | VARCHAR(500) | 备注 |
| `created_by` | BIGINT | 创建人 |
| `created_at` | DATETIME | 创建时间 |
| `updated_by` | BIGINT | 更新人 |
| `updated_at` | DATETIME | 更新时间 |

## 5.2 字段命名约定

| 类型 | 设计规范 |
|---|---|
| 任务编号 | `*_no` 或 `*_job_no` |
| 外部业务编号 | `java_task_no`、`case_no`、`patient_uuid` |
| 类型/状态 | `*_code` 或 `status` |
| 布尔值 | `*_flag` |
| 时间字段 | `*_at`、`*_date` |
| JSON 字段 | `*_json` |
| 模型版本 | `model_version` |
| 知识版本 | `knowledge_version` |

## 5.3 隐私约定

Python 库中应尽量只出现以下业务关联锚点：

- `task_no`
- `case_no`
- `patient_uuid`
- `image_id`
- `attachment_id`

不应在 Python 库中保存：

- 患者真实姓名；
- 手机号；
- 身份证号；
- 监护人联系方式；
- 未脱敏的病例全文。

---

# 6. AI 运行域数据字典

## ai_infer_job（AI 推理任务表）
**所属数据域**：AI 运行域  
**表说明**：记录一次 Python 端实际接收与执行的推理任务主信息。  

**建模规则**：
- `job_no` 全局唯一；
- `java_task_no` 与 Java analysis 任务保持一一对应或可追溯对应；
- `request_json`、`result_json` 用于保存运行态快照，而不是业务最终结论。

**索引建议**：
- UK(`job_no`)
- IDX(`java_task_no`)
- IDX(`case_no`,`status_code`)
- IDX(`infer_type_code`,`status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| job_no | VARCHAR(64) | 是 | UK | - | Python 任务编号 |
| java_task_no | VARCHAR(64) | 是 | IDX | - | Java analysis 任务号 |
| case_no | VARCHAR(64) | 否 | IDX | NULL | 病例编号 |
| patient_uuid | VARCHAR(128) | 否 | IDX | NULL | 脱敏患者标识 |
| infer_type_code | VARCHAR(32) | 是 | IDX | ANALYZE | 任务类型：QUALITY/ANALYZE/RISK |
| model_version | VARCHAR(64) | 是 | IDX | - | 当前模型版本 |
| status_code | VARCHAR(32) | 是 | IDX | QUEUEING | QUEUEING/RUNNING/SUCCESS/FAILED/CANCELLED |
| request_json | JSON | 否 | - | NULL | 下发请求快照 |
| result_json | JSON | 否 | - | NULL | 推理结果快照 |
| error_message | VARCHAR(1000) | 否 | - | NULL | 错误信息 |
| started_at | DATETIME | 否 | IDX | NULL | 开始时间 |
| finished_at | DATETIME | 否 | IDX | NULL | 结束时间 |
| callback_required_flag | CHAR(1) | 是 | - | 1 | 是否需要回调 Java |
| callback_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/SUCCESS/FAILED |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## ai_infer_job_image（推理任务影像表）
**所属数据域**：AI 运行域  
**表说明**：记录任务对应影像的下载与影像级处理结果。  

**建模规则**：
- 一个任务可对应多张影像；
- Python 只记录影像级运行状态，不替代 Java 的 `med_image_file`；
- visual asset 最终通过 `ai_infer_artifact` 关联。

**索引建议**：
- IDX(`job_id`,`image_id`)
- IDX(`download_status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| job_id | BIGINT | 是 | IDX | - | ai_infer_job.id |
| image_id | BIGINT | 否 | IDX | NULL | Java 影像ID |
| attachment_id | BIGINT | 否 | IDX | NULL | Java 附件ID |
| image_type_code | VARCHAR(32) | 否 | IDX | NULL | 影像类型 |
| bucket_name | VARCHAR(128) | 否 | - | NULL | MinIO 桶名 |
| object_key | VARCHAR(500) | 否 | - | NULL | 对象键 |
| access_url | TEXT | 否 | - | NULL | 预签名访问地址 |
| url_expire_at | DATETIME | 否 | - | NULL | URL 失效时间 |
| download_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/SUCCESS/FAILED |
| local_cache_path | VARCHAR(500) | 否 | - | NULL | 本地缓存路径 |
| quality_status_code | VARCHAR(32) | 否 | IDX | NULL | 质量状态 |
| grading_label | VARCHAR(32) | 否 | - | NULL | 影像级分级结果 |
| uncertainty_score | DECIMAL(8,4) | 否 | - | NULL | 不确定性分数 |
| result_json | JSON | 否 | - | NULL | 影像级结果 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## ai_infer_artifact（推理产物表）
**所属数据域**：AI 运行域  
**表说明**：记录推理产生的 mask、overlay、heatmap 等产物元数据。  

**建模规则**：
- 只保存元数据，不直接保存二进制文件；
- 产物应落在既有 MinIO 体系中；
- 回调 Java 后，可保存 Java 侧 attachment 关联信息。

**索引建议**：
- IDX(`job_id`,`artifact_type_code`)
- IDX(`related_image_id`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| job_id | BIGINT | 是 | IDX | - | ai_infer_job.id |
| related_image_id | BIGINT | 否 | IDX | NULL | 关联 Java 影像ID |
| artifact_type_code | VARCHAR(32) | 是 | IDX | OVERLAY | MASK/OVERLAY/HEATMAP |
| bucket_name | VARCHAR(128) | 是 | - | - | MinIO 桶名 |
| object_key | VARCHAR(500) | 是 | - | - | 对象键 |
| content_type | VARCHAR(128) | 否 | - | NULL | MIME 类型 |
| file_size_bytes | BIGINT | 否 | - | NULL | 文件大小 |
| md5 | VARCHAR(64) | 否 | IDX | NULL | 文件摘要 |
| model_version | VARCHAR(64) | 否 | IDX | NULL | 生成此产物的模型版本 |
| attachment_id | BIGINT | 否 | IDX | NULL | Java 回写后的 attachment_id |
| ext_json | JSON | 否 | - | NULL | 扩展信息 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## ai_callback_log（Java 回调日志表）
**所属数据域**：AI 运行域  
**表说明**：记录 Python 回调 Java 的请求体、响应、失败原因与重试过程。  

**建模规则**：
- 一次任务可有多次回调尝试；
- 必须留存失败原因与 HTTP 响应摘要；
- 所有重试必须可审计。

**索引建议**：
- IDX(`job_id`,`callback_status_code`)
- IDX(`next_retry_at`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| job_id | BIGINT | 是 | IDX | - | ai_infer_job.id |
| callback_url | VARCHAR(500) | 是 | - | - | Java 回调地址 |
| request_json | JSON | 否 | - | NULL | 回调请求体 |
| response_code | INT | 否 | - | NULL | HTTP 响应码 |
| response_body | TEXT | 否 | - | NULL | 响应摘要 |
| callback_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/SUCCESS/FAILED |
| retry_count | INT | 是 | - | 0 | 重试次数 |
| next_retry_at | DATETIME | 否 | IDX | NULL | 下次重试时间 |
| error_message | VARCHAR(1000) | 否 | - | NULL | 错误信息 |
| trace_id | VARCHAR(128) | 否 | IDX | NULL | 链路追踪ID |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

---

# 7. RAG 与知识库域数据字典

## kb_knowledge_base（知识库表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录一个知识库集合的定义、用途、状态与版本。  

**建模规则**：
- 同一知识库可对应多版知识文档；
- 明确知识库用途，避免“一库通吃”；
- 知识库本身必须可启停。

**索引建议**：
- UK(`kb_code`)
- IDX(`kb_type_code`,`status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| kb_code | VARCHAR(64) | 是 | UK | - | 知识库编码 |
| kb_name | VARCHAR(128) | 是 | - | - | 知识库名称 |
| kb_type_code | VARCHAR(32) | 是 | IDX | PATIENT_GUIDE | PATIENT_GUIDE/DOCTOR_QA/FAQ/RULE |
| knowledge_version | VARCHAR(64) | 是 | IDX | v1.0 | 知识版本 |
| embedding_model | VARCHAR(64) | 否 | - | NULL | embedding 模型 |
| vector_store_type_code | VARCHAR(32) | 是 | - | FAISS | FAISS/QDRANT |
| vector_store_path | VARCHAR(500) | 否 | - | NULL | 向量索引路径 |
| enabled_flag | CHAR(1) | 是 | - | 1 | 是否启用 |
| status_code | VARCHAR(32) | 是 | IDX | ACTIVE | ACTIVE/ARCHIVED |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## kb_document（知识文档表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录知识文档的来源、审核与版本信息。  

**建模规则**：
- 一篇文档必须有来源与审核状态；
- 未审核文档不得进入生产知识库；
- 文档正文可存储文本或对象存储引用。

**索引建议**：
- IDX(`kb_id`,`review_status_code`)
- IDX(`doc_source_code`,`doc_version`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| kb_id | BIGINT | 是 | IDX | - | kb_knowledge_base.id |
| doc_no | VARCHAR(64) | 是 | UK | - | 文档编号 |
| doc_title | VARCHAR(255) | 是 | IDX | - | 文档标题 |
| doc_source_code | VARCHAR(32) | 是 | IDX | INTERNAL | INTERNAL/PROJECT_DOC/FAQ/MANUAL |
| source_uri | VARCHAR(500) | 否 | - | NULL | 来源地址或文件路径 |
| doc_version | VARCHAR(64) | 是 | IDX | v1.0 | 文档版本 |
| content_text | LONGTEXT | 否 | - | NULL | 文本内容 |
| content_attachment_key | VARCHAR(500) | 否 | - | NULL | 原文件对象键 |
| review_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/APPROVED/REJECTED |
| reviewer_id | BIGINT | 否 | - | NULL | 审核人 |
| reviewed_at | DATETIME | 否 | - | NULL | 审核时间 |
| enabled_flag | CHAR(1) | 是 | - | 1 | 是否启用 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## kb_document_chunk（知识分片表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录切分后的知识片段及其向量索引定位。  

**建模规则**：
- 分片必须可追溯到原始文档；
- 向量本体不强制存 MySQL，可存索引文件；
- `chunk_text` 保留以便审计与命中展示。

**索引建议**：
- IDX(`doc_id`,`chunk_no`)
- IDX(`kb_id`,`enabled_flag`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| kb_id | BIGINT | 是 | IDX | - | kb_knowledge_base.id |
| doc_id | BIGINT | 是 | IDX | - | kb_document.id |
| chunk_no | INT | 是 | IDX | - | 分片序号 |
| chunk_text | TEXT | 是 | - | - | 分片文本 |
| token_count | INT | 否 | - | NULL | token 数 |
| embedding_model | VARCHAR(64) | 否 | - | NULL | embedding 模型 |
| vector_ref | VARCHAR(255) | 否 | - | NULL | 向量索引定位引用 |
| section_title | VARCHAR(255) | 否 | - | NULL | 原章节标题 |
| source_offset_start | INT | 否 | - | NULL | 原文起始偏移 |
| source_offset_end | INT | 否 | - | NULL | 原文结束偏移 |
| enabled_flag | CHAR(1) | 是 | - | 1 | 是否可检索 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## kb_rebuild_job（知识索引重建任务表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录知识切分、embedding 与向量索引重建任务。  

**建模规则**：
- 每次重建都形成独立记录；
- 必须可回溯重建使用的文档范围与结果。

**索引建议**：
- IDX(`kb_id`,`job_status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| kb_id | BIGINT | 是 | IDX | - | kb_knowledge_base.id |
| rebuild_job_no | VARCHAR(64) | 是 | UK | - | 重建任务号 |
| job_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/RUNNING/SUCCESS/FAILED |
| doc_count | INT | 否 | - | NULL | 文档数量 |
| chunk_count | INT | 否 | - | NULL | 分片数量 |
| embedding_model | VARCHAR(64) | 否 | - | NULL | embedding 模型 |
| output_path | VARCHAR(500) | 否 | - | NULL | 索引输出路径 |
| error_message | VARCHAR(1000) | 否 | - | NULL | 错误信息 |
| started_at | DATETIME | 否 | - | NULL | 开始时间 |
| finished_at | DATETIME | 否 | - | NULL | 结束时间 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## rag_session（RAG 会话表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录一次患者解释或医生问答会话。  

**建模规则**：
- 会话用于承载一轮或多轮交互；
- 会话类型必须清晰，避免患者问答与医生问答混用。

**索引建议**：
- IDX(`session_type_code`,`biz_ref_no`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| session_no | VARCHAR(64) | 是 | UK | - | 会话号 |
| session_type_code | VARCHAR(32) | 是 | IDX | PATIENT_EXPLAIN | PATIENT_EXPLAIN/DOCTOR_QA/FAQ |
| biz_ref_no | VARCHAR(64) | 否 | IDX | NULL | 关联业务编号，如 caseNo/reportNo |
| patient_uuid | VARCHAR(128) | 否 | IDX | NULL | 脱敏患者标识 |
| java_user_id | BIGINT | 否 | IDX | NULL | Java 用户ID |
| knowledge_version | VARCHAR(64) | 否 | IDX | NULL | 使用的知识版本 |
| model_name | VARCHAR(64) | 否 | - | NULL | 通用大模型名称 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## rag_request_log（RAG 请求日志表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录一次完整的 RAG 请求与生成结果。  

**建模规则**：
- 每次调用必须留痕；
- 应保留用户问题、检索查询、最终回答摘要。

**索引建议**：
- IDX(`session_id`,`request_type_code`)
- IDX(`request_status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| session_id | BIGINT | 是 | IDX | - | rag_session.id |
| request_no | VARCHAR(64) | 是 | UK | - | 请求号 |
| request_type_code | VARCHAR(32) | 是 | IDX | PATIENT_EXPLAIN | 请求类型 |
| user_query | TEXT | 是 | - | - | 用户问题或生成指令 |
| rewritten_query | TEXT | 否 | - | NULL | 检索改写结果 |
| top_k | INT | 是 | - | 5 | 检索 TopK |
| answer_text | LONGTEXT | 否 | - | NULL | 最终回答 |
| request_status_code | VARCHAR(32) | 是 | IDX | SUCCESS | SUCCESS/FAILED/BLOCKED |
| safety_flag | CHAR(1) | 是 | - | 0 | 是否触发安全规则 |
| latency_ms | INT | 否 | - | NULL | 总耗时 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## rag_retrieval_log（RAG 检索命中表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录某次请求命中的知识分片明细。  

**建模规则**：
- 一次请求对应多条命中；
- 必须保存命中排序和分数，支撑答辩证据链。

**索引建议**：
- IDX(`request_id`,`rank_no`)
- IDX(`chunk_id`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| request_id | BIGINT | 是 | IDX | - | rag_request_log.id |
| chunk_id | BIGINT | 是 | IDX | - | kb_document_chunk.id |
| rank_no | INT | 是 | IDX | - | 命中排序 |
| retrieval_score | DECIMAL(10,6) | 否 | - | NULL | 检索得分 |
| doc_id | BIGINT | 是 | IDX | - | kb_document.id |
| chunk_text_snapshot | TEXT | 否 | - | NULL | 命中文本快照 |
| cited_flag | CHAR(1) | 是 | - | 0 | 是否被最终引用 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |

## llm_call_log（通用大模型调用日志表）
**所属数据域**：RAG 与知识库域  
**表说明**：记录外部通用大模型调用细节与耗时。  

**建模规则**：
- 一次 RAG 请求可对应一次或多次 LLM 调用；
- 应保留模型名、token、状态与错误信息。

**索引建议**：
- IDX(`request_id`,`model_name`)
- IDX(`call_status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| request_id | BIGINT | 是 | IDX | - | rag_request_log.id |
| model_name | VARCHAR(128) | 是 | IDX | - | 通用大模型名称 |
| provider_code | VARCHAR(64) | 否 | - | NULL | 提供方 |
| prompt_text | LONGTEXT | 否 | - | NULL | 最终 prompt |
| completion_text | LONGTEXT | 否 | - | NULL | 模型原始输出 |
| prompt_tokens | INT | 否 | - | NULL | 输入 token |
| completion_tokens | INT | 否 | - | NULL | 输出 token |
| total_tokens | INT | 否 | - | NULL | 总 token |
| latency_ms | INT | 否 | - | NULL | 耗时 |
| call_status_code | VARCHAR(32) | 是 | IDX | SUCCESS | SUCCESS/FAILED/TIMEOUT |
| error_message | VARCHAR(1000) | 否 | - | NULL | 错误信息 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |

---

# 8. 模型与训练治理域数据字典

## mdl_model_version（模型版本表）
**所属数据域**：模型与训练治理域  
**表说明**：记录视觉模型、风险模型、embedding 模型等版本信息。  

**建模规则**：
- 一个模型类型可有多个版本；
- 状态必须区分 CANDIDATE / ACTIVE / ARCHIVED；
- Python 和 RAG 都通过此表记录版本。

**索引建议**：
- UK(`model_code`,`version_no`)
- IDX(`model_type_code`,`status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| model_code | VARCHAR(64) | 是 | IDX | - | 模型编码 |
| model_name | VARCHAR(128) | 是 | - | - | 模型名称 |
| model_type_code | VARCHAR(32) | 是 | IDX | SEGMENTATION | SEGMENTATION/GRADING/RISK/EMBEDDING/LLM_PROMPT |
| version_no | VARCHAR(64) | 是 | IDX | - | 版本号 |
| artifact_path | VARCHAR(500) | 否 | - | NULL | 模型文件路径 |
| dataset_version | VARCHAR(64) | 否 | IDX | NULL | 关联数据快照 |
| metrics_json | JSON | 否 | - | NULL | 指标摘要 |
| status_code | VARCHAR(32) | 是 | IDX | CANDIDATE | CANDIDATE/UNDER_REVIEW/ACTIVE/REJECTED/ARCHIVED |
| active_flag | CHAR(1) | 是 | - | 0 | 是否当前启用 |
| published_at | DATETIME | 否 | - | NULL | 发布时间 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## mdl_model_eval_record（模型评估记录表）
**所属数据域**：模型与训练治理域  
**表说明**：记录候选模型的离线评估结果与证据材料。  

**建模规则**：
- 评估必须绑定模型版本与数据快照；
- 支持保存指标、错误分析和附件引用。

**索引建议**：
- IDX(`model_version_id`,`dataset_snapshot_id`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| model_version_id | BIGINT | 是 | IDX | - | mdl_model_version.id |
| dataset_snapshot_id | BIGINT | 否 | IDX | NULL | trn_dataset_snapshot.id |
| eval_type_code | VARCHAR(32) | 是 | IDX | OFFLINE | OFFLINE/AB_TEST/REGRESSION |
| metric_json | JSON | 否 | - | NULL | 指标摘要 |
| error_case_json | JSON | 否 | - | NULL | 错误样例 |
| evidence_attachment_key | VARCHAR(500) | 否 | - | NULL | 证据附件 |
| evaluated_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 评估时间 |
| evaluator_name | VARCHAR(128) | 否 | - | NULL | 评估人 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## mdl_model_approval_record（模型审批记录表）
**所属数据域**：模型与训练治理域  
**表说明**：记录候选模型人工审批与上线决策。  

**建模规则**：
- 不允许“口头上线”；
- 每次审批都必须保存决策人与结论。

**索引建议**：
- IDX(`model_version_id`,`decision_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| model_version_id | BIGINT | 是 | IDX | - | mdl_model_version.id |
| decision_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/APPROVED/REJECTED |
| approver_name | VARCHAR(128) | 否 | - | NULL | 审批人 |
| decision_note | VARCHAR(1000) | 否 | - | NULL | 决策说明 |
| approved_at | DATETIME | 否 | IDX | NULL | 审批时间 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## trn_dataset_snapshot（数据快照表）
**所属数据域**：模型与训练治理域  
**表说明**：记录一次训练或评估使用的数据快照版本。  

**建模规则**：
- 每次训练必须绑定快照版本；
- 快照一旦发布，不得被静默篡改。

**索引建议**：
- UK(`dataset_version`)
- IDX(`snapshot_type_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| dataset_version | VARCHAR(64) | 是 | UK | - | 快照版本 |
| snapshot_type_code | VARCHAR(32) | 是 | IDX | TRAIN | TRAIN/VAL/TEST/GOLDSET |
| source_summary | VARCHAR(500) | 否 | - | NULL | 来源摘要 |
| sample_count | INT | 否 | - | NULL | 样本数量 |
| metadata_json | JSON | 否 | - | NULL | 元数据摘要 |
| dataset_card_path | VARCHAR(500) | 否 | - | NULL | Dataset Card 路径 |
| released_at | DATETIME | 否 | IDX | NULL | 发布时间 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## trn_dataset_sample（数据快照样本明细表）
**所属数据域**：模型与训练治理域  
**表说明**：记录某个快照包含的样本清单与来源。  

**建模规则**：
- 用于追溯某个样本属于哪个快照；
- 只保存脱敏标识，不保存敏感原始数据。

**索引建议**：
- IDX(`snapshot_id`,`sample_ref_no`)
- IDX(`patient_uuid`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| snapshot_id | BIGINT | 是 | IDX | - | trn_dataset_snapshot.id |
| sample_ref_no | VARCHAR(128) | 是 | IDX | - | 样本引用编号 |
| patient_uuid | VARCHAR(128) | 否 | IDX | NULL | 脱敏患者标识 |
| image_ref_no | VARCHAR(128) | 否 | IDX | NULL | 影像引用编号 |
| source_type_code | VARCHAR(32) | 是 | IDX | CORRECTION | CORRECTION/PUBLIC/DESENSITIZED |
| split_type_code | VARCHAR(32) | 是 | IDX | TRAIN | TRAIN/VAL/TEST |
| label_version | VARCHAR(64) | 否 | - | NULL | 标注版本 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |

## ann_annotation_record（标注记录表）
**所属数据域**：模型与训练治理域  
**表说明**：记录样本标注版本、标注者、复核者与质控状态。  

**建模规则**：
- 不直接保存大面积像素标注本体，可保存 JSON 路径或对象存储引用；
- 必须记录 L1/L2 角色与质控状态。

**索引建议**：
- IDX(`sample_ref_no`,`annotation_version`)
- IDX(`qc_status_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| sample_ref_no | VARCHAR(128) | 是 | IDX | - | 样本引用编号 |
| patient_uuid | VARCHAR(128) | 否 | IDX | NULL | 脱敏患者标识 |
| annotation_version | VARCHAR(64) | 是 | IDX | - | 标注版本 |
| annotation_result_json | JSON | 否 | - | NULL | 标注结果摘要 |
| annotation_object_key | VARCHAR(500) | 否 | - | NULL | 标注文件对象键 |
| annotator_l1 | VARCHAR(128) | 否 | - | NULL | 一级标注员 |
| reviewer_l2 | VARCHAR(128) | 否 | - | NULL | 二级复核医生 |
| qc_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/PASSED/ARBITRATION |
| difficulty_code | VARCHAR(32) | 否 | - | NULL | 样本难度 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## ann_gold_set_item（Gold Set 样本表）
**所属数据域**：模型与训练治理域  
**表说明**：记录高置信度金标准样本及其启用状态。  

**建模规则**：
- 进入 Gold Set 的样本必须经过复核或仲裁；
- Gold Set 可被训练评估、标注员考核和答辩展示复用。

**索引建议**：
- UK(`sample_ref_no`)
- IDX(`active_flag`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| sample_ref_no | VARCHAR(128) | 是 | UK | - | 样本引用编号 |
| annotation_record_id | BIGINT | 否 | IDX | NULL | ann_annotation_record.id |
| difficulty_code | VARCHAR(32) | 否 | - | NULL | 难度等级 |
| active_flag | CHAR(1) | 是 | IDX | 1 | 是否启用 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

## trn_training_run（训练运行记录表）
**所属数据域**：模型与训练治理域  
**表说明**：记录一次训练或评估任务的执行过程与结果。  

**建模规则**：
- 训练必须绑定模型与快照；
- 应保留运行参数、产物路径与状态。

**索引建议**：
- IDX(`dataset_snapshot_id`,`run_status_code`)
- IDX(`target_model_code`)

| 字段 | 类型 | 必填 | 键/索引 | 默认值 | 说明 |
|---|---|---|---|---|---|
| id | BIGINT | 是 | PK | - | 主键 |
| training_run_no | VARCHAR(64) | 是 | UK | - | 训练运行号 |
| dataset_snapshot_id | BIGINT | 否 | IDX | NULL | trn_dataset_snapshot.id |
| target_model_code | VARCHAR(64) | 是 | IDX | - | 目标模型编码 |
| base_model_version | VARCHAR(64) | 否 | - | NULL | 基础模型版本 |
| run_type_code | VARCHAR(32) | 是 | IDX | TRAIN | TRAIN/EVAL/REGRESSION |
| parameters_json | JSON | 否 | - | NULL | 训练参数 |
| output_artifact_path | VARCHAR(500) | 否 | - | NULL | 输出产物路径 |
| metric_json | JSON | 否 | - | NULL | 结果指标 |
| run_status_code | VARCHAR(32) | 是 | IDX | PENDING | PENDING/RUNNING/SUCCESS/FAILED |
| started_at | DATETIME | 否 | - | NULL | 开始时间 |
| finished_at | DATETIME | 否 | - | NULL | 结束时间 |
| error_message | VARCHAR(1000) | 否 | - | NULL | 错误信息 |
| org_id | BIGINT | 否 | IDX | NULL | 所属机构 |
| status | VARCHAR(32) | 是 | - | ACTIVE | 记录状态 |
| deleted_flag | CHAR(1) | 是 | - | 0 | 逻辑删除 |
| remark | VARCHAR(500) | 否 | - | NULL | 备注 |
| created_by | BIGINT | 否 | - | NULL | 创建人 |
| created_at | DATETIME | 是 | IDX | CURRENT_TIMESTAMP | 创建时间 |
| updated_by | BIGINT | 否 | - | NULL | 更新人 |
| updated_at | DATETIME | 是 | - | CURRENT_TIMESTAMP | 更新时间 |

---

# 9. 数据生命周期与留存策略

## 9.1 AI 运行域

- `ai_infer_job`：建议至少保留 180 天；
- `ai_infer_job_image`：建议与主任务同步保留；
- `ai_infer_artifact`：可与 MinIO visual 产物生命周期配套，默认 30 天；
- `ai_callback_log`：建议保留 180 天以上，用于排障与审计。

## 9.2 RAG 与知识库域

- `kb_document`、`kb_document_chunk`：按知识版本长期保留；
- `kb_rebuild_job`：至少保留全部成功/失败记录；
- `rag_request_log`、`rag_retrieval_log`、`llm_call_log`：建议保留 90~180 天；
- 若面向隐私敏感场景，应保留脱敏日志摘要而非原始全文。

## 9.3 模型与训练治理域

- `mdl_model_version`、`mdl_model_eval_record`、`mdl_model_approval_record`：长期保留；
- `trn_dataset_snapshot`、`trn_dataset_sample`：长期保留；
- `trn_training_run`：长期保留；
- `ann_gold_set_item`：长期保留。

---

# 10. 数据库部署与实现建议

## 10.1 当前推荐实现

### 元数据数据库

- 使用 **MySQL 8.x**；
- 与 Java 业务库逻辑分开，建议独立 schema 或独立库；
- 比赛阶段可与现有 MySQL 共实例，但不建议与 Java 共表。

### 向量索引

- 一期采用 **FAISS 本地索引文件**；
- 索引路径通过 `kb_knowledge_base.vector_store_path` 管理；
- 向量本体不强制写入 MySQL。

## 10.2 与 MinIO 的关系

以下内容建议存 MinIO，只在数据库中保存元数据：

- mask / overlay / heatmap；
- 大体量标注文件；
- 数据集卡片附件；
- 评估证据附件；
- 文档原始文件对象。

## 10.3 与 Java 的衔接建议

- Java 与 Python 共用 `taskNo / caseNo / attachmentId / imageId / orgId` 作为对账锚点；
- Python 不依赖 Java 业务表 join 查询；
- 所有跨服务协作都通过消息、接口和快照完成。

---

# 11. 实施建议与落地顺序

## 第一阶段：必须先建的表

建议先实现以下 12 张表：

1. `ai_infer_job`
2. `ai_infer_job_image`
3. `ai_infer_artifact`
4. `ai_callback_log`
5. `kb_knowledge_base`
6. `kb_document`
7. `kb_document_chunk`
8. `kb_rebuild_job`
9. `rag_session`
10. `rag_request_log`
11. `rag_retrieval_log`
12. `llm_call_log`

这些表足以支撑：

- analysis 运行闭环；
- visual asset 管理；
- RAG 知识入库与检索生成；
- 检索与 LLM 调用留痕。

## 第二阶段：治理补齐

建议再实现：

13. `mdl_model_version`
14. `mdl_model_eval_record`
15. `mdl_model_approval_record`
16. `trn_dataset_snapshot`
17. `trn_dataset_sample`
18. `trn_training_run`

## 第三阶段：标注与 Gold Set 管理补齐

建议最后实现：

19. `ann_annotation_record`
20. `ann_gold_set_item`

## 收口原则

- 先让 Python AI 运行域与 RAG 闭环可用；
- 再逐步补模型治理；
- 最后补训练治理与标注治理；
- 不要一开始就把治理体系堆得过于庞大，导致运行闭环迟迟不可演示。

---

# 12. 一句话收口

**Python 侧数据库的正确定位不是“复制 Java 业务库”，而是承载 AI 运行元数据、RAG 知识元数据和模型训练治理元数据；这样既能保证业务边界清晰，也能让“取消 LoRA、采用 RAG + 通用大模型”的路线有真正可落地的数据库底座。**
