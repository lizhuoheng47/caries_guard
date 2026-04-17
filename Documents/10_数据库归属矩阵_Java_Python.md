# 10 数据库归属矩阵 Java / Python

## 1. 归属原则

Java 和 Python 共享业务标识，但不共享 schema 归属。

| 原则 | 说明 |
| --- | --- |
| Java 是业务权威 | 患者、病例、报告、复核、随访归 Java |
| Python 是运行权威 | AI/RAG 推理、检索、模型治理归 Python |
| 不跨库迁移 | Java Flyway 不改 `caries_ai`，Python Alembic 不改 `caries_biz` |
| callback 解耦 | Python 通过 callback 交付结果 |
| JSON 扩展 | 新 AI 字段优先进入 raw_result_json |

## 2. 表归属

| 表/数据 | Java | Python |
| --- | --- | --- |
| 用户、角色、组织 | Owner | Read by API only |
| 患者、就诊、病例 | Owner | Weak reference |
| 原始影像附件 | Owner | Read/download |
| visual asset 文件 | Owner for metadata | Generate/upload |
| analysis task | Owner | Consume task_no |
| summary/raw_result_json | Owner | Produce payload |
| 推理运行日志 | No | Owner |
| RAG 知识库 | Optional display | Owner |
| 通用大模型调用日志 | No | Owner |
| 模型/算法版本 | Optional display | Owner |

## 3. 关键弱引用

- `task_no`
- `case_id`
- `case_no`
- `image_id`
- `attachment_id`
- `trace_id`

弱引用不建立跨库外键。

## 4. Phase 5C 归属

Python 负责生成：

- `gradingMode`
- `gradingImplType`
- `gradingLabel`
- `confidenceScore`
- `uncertaintyMode`
- `uncertaintyImplType`
- `uncertaintyScore`
- `needsReview`

Java 负责消费：

- 写入 `ana_result_summary.raw_result_json`；
- 更新 summary；
- 根据 `needsReview` 推进 review 语义；
- 保持 report/followup 主链稳定。

## 5. RAG 归属

Python 负责：

- 知识库；
- chunk；
- retrieval；
- 通用大模型调用；
- citations；
- safety flags。

Java 负责：

- 用户鉴权；
- 业务上下文；
- 问答入口；
- 页面展示；
- 需要时保存问答业务记录。

## 6. 禁止事项

- Python 直接写 `ana_task_record`；
- Python 直接写 `ana_result_summary`；
- Java 直接写 `rag_chunk`；
- Java 直接改 `ai_infer_job`；
- 用通用大模型回答直接改变病例状态；
- 以微调数据表替代知识库治理。
