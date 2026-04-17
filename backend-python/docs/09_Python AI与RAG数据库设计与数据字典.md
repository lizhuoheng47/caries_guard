# 09 Python AI 与 RAG 数据库设计与数据字典

## 1. 数据库定位

Python 服务使用独立数据库 `caries_ai`，只保存 AI/RAG 运行域数据，不承载 Java 业务主数据。

`caries_ai` 的目标是：

- 记录推理任务、图像级推理结果和 callback 日志；
- 记录知识库、知识文档、切片、向量索引元数据；
- 记录 RAG 请求、检索结果、通用大模型调用日志；
- 记录模型/算法适配器版本、评估和审批状态；
- 为审计、回放、排错和离线评估提供证据链。

## 2. 与 Java 数据库边界

| 数据 | 归属 |
| --- | --- |
| 患者、病例、影像、附件、报告、复核、随访 | `caries_biz` |
| analysis task 业务状态 | `caries_biz` |
| AI 推理运行日志 | `caries_ai` |
| RAG 知识库和检索日志 | `caries_ai` |
| 模型/算法版本治理 | `caries_ai` |
| MinIO bucket/object 主索引 | Java 业务表为准，Python 可记录运行引用 |

Python 不直接修改 Java 业务主表，只通过 callback 让 Java 落库。

## 3. 核心表

### 3.1 `ai_infer_job`

AI 推理任务运行记录。

| 字段 | 含义 |
| --- | --- |
| `job_no` | Python 运行域任务号 |
| `task_no` | Java analysis task_no |
| `model_version` | 对外模型版本 |
| `job_status_code` | `PENDING/RUNNING/SUCCESS/FAILED` |
| `request_json` | 输入请求快照 |
| `result_json` | 总体结果快照 |
| `error_code/error_message` | 失败留痕 |

### 3.2 `ai_infer_job_image`

图像级结果。

| 字段 | 含义 |
| --- | --- |
| `job_id` | 关联 `ai_infer_job` |
| `image_id` | Java image id |
| `quality_status_code` | 质量检查结果 |
| `grading_label` | `C0/C1/C2/C3` |
| `uncertainty_score` | 不确定性 |
| `result_json` | 包含 mode、implType、needsReview、rawResult |

### 3.3 `ai_callback_log`

Python -> Java callback 调用日志。

| 字段 | 含义 |
| --- | --- |
| `task_no` | 业务任务号 |
| `callback_url` | 回调地址 |
| `request_json` | callback payload |
| `response_status_code` | HTTP 状态 |
| `response_body` | 响应体 |
| `success_flag` | 是否成功 |

## 4. RAG 表

### 4.1 `rag_knowledge_base`

知识库定义。

### 4.2 `rag_document`

知识文档元数据，来源可以是指南、规范、科普内容、报告模板、业务规则说明。

### 4.3 `rag_chunk`

文档切片，保存检索文本、hash、token 数和结构化标签。

### 4.4 `rag_retrieval_log`

每次检索命中的 chunk、score、rank 和过滤条件。

### 4.5 `rag_request_log`

用户问题、上下文摘要、响应摘要、耗时和状态。

### 4.6 `llm_call_log`

通用大模型调用日志，包括 provider、model、prompt 摘要、token 估算、响应摘要和错误信息。

## 5. 模型治理表

### 5.1 `mdl_model_version`

记录模型或算法适配器版本。当前不仅用于 ML 权重，也用于 HEURISTIC 算法版本登记。

推荐类型：

- `QUALITY`
- `DETECTION`
- `SEGMENTATION`
- `GRADING`
- `RISK`
- `RAG`

### 5.2 `mdl_model_eval_record`

离线评估记录。对通用大模型 + RAG 方案，应记录检索命中率、引用准确率、人工评估结果、幻觉率和拒答率。

### 5.3 `mdl_model_approval_record`

上线审批记录。算法适配器、RAG 知识版本、大模型 provider/model 切换均应登记。

## 6. Phase 5C 必备 JSON 字段

`ai_infer_job_image.result_json` 和 Java `ana_result_summary.raw_result_json` 应至少包含：

```json
{
  "mode": "hybrid",
  "qualityMode": "real",
  "qualityImplType": "HEURISTIC",
  "toothDetectionMode": "real",
  "toothDetectionImplType": "HEURISTIC",
  "segmentationMode": "real",
  "segmentationImplType": "HEURISTIC",
  "gradingMode": "real",
  "gradingImplType": "HEURISTIC",
  "gradingLabel": "C2",
  "confidenceScore": 0.63,
  "uncertaintyMode": "real",
  "uncertaintyImplType": "HEURISTIC",
  "uncertaintyScore": 0.37,
  "needsReview": true
}
```

## 7. 迁移策略

- 所有 schema 变更通过 Alembic 管理；
- Java 业务库通过 Flyway 管理；
- 不允许 Python migration 修改 `caries_biz`；
- 不允许 Java migration 修改 `caries_ai`；
- JSON 扩展优先于破坏 callback DTO；
- 表结构变更必须先补数据字典，再写 migration。
