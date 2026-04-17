# CariesGuard Java Backend

## 1. 定位

Java 后端是业务主链，负责用户、患者、病例、影像、analysis task、报告、复核、随访和 RAG 展示入口。

Python 服务提供 AI/RAG 能力，Java 不直接运行模型或通用大模型。

## 2. AI 协作

Analysis：

```text
Java task -> RabbitMQ -> Python InferencePipeline -> Java callback
```

RAG：

```text
Java API -> Python RAG -> Knowledge Base + General LLM -> Java response
```

当前 AI 路线不采用业务专用大模型微调。Java 只消费结构化结果、引用和安全标记。

## 3. 数据库

Java 只管理 `caries_biz`。Python 管理 `caries_ai`。

## 4. 关键表

- `ana_task_record`
- `ana_result_summary`
- `ana_visual_asset`
- `med_attachment`
- `med_image_file`
- `rpt_report`
- `rev_review_record`
- `fup_followup_plan`

## 5. Phase 5C 消费字段

Java 通过 `ana_result_summary.raw_result_json` 消费：

- `gradingMode`
- `gradingImplType`
- `gradingLabel`
- `uncertaintyMode`
- `uncertaintyImplType`
- `uncertaintyScore`
- `needsReview`

## 6. 文档

长期设计文档位于：

- `../Documents`
- `docs/00_文档说明与项目总览.md`
- `docs/06_Python联调与AI协作说明.md`
