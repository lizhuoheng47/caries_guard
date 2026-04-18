# backend-python

`backend-python/` 是 CariesGuard 的 AI / RAG 能力提供方，不是业务状态权威。

它负责消费分析任务、执行推理流水线、管理知识库与 RAG、记录运行日志，并通过 callback 与 Java 主链协作。

## 职责

- MQ Worker 消费分析任务
- 质量检查、检测、分割、分级、不确定性、风险流程
- Visual asset 生成与上传
- Analysis callback 组装与发送
- RAG 问答、患者解释、知识文档导入与重建
- 推理日志、检索日志、LLM 调用日志
- 模型版本、评估、审批、训练数据治理

## 运行模式

`app/core/config.py` 中定义三种模式：

- `mock`：默认、最稳定、适合演示
- `hybrid`：部分模块启用真实适配器
- `real`：所有启用模块必须真实执行，失败显式报错

相关开关：

- `CG_AI_RUNTIME_MODE`
- `CG_MODEL_QUALITY_ENABLED`
- `CG_MODEL_TOOTH_DETECT_ENABLED`
- `CG_MODEL_SEGMENTATION_ENABLED`
- `CG_MODEL_GRADING_ENABLED`
- `CG_MODEL_RISK_ENABLED`
- `CG_UNCERTAINTY_REVIEW_THRESHOLD`

## HTTP API

默认挂载在 `/ai/v1`：

- `POST /ai/v1/analyze`
- `POST /ai/v1/quality-check`
- `POST /ai/v1/assess-risk`
- `GET /ai/v1/model-version`
- `GET /ai/v1/health`
- `POST /ai/v1/rag/doctor-qa`
- `POST /ai/v1/rag/patient-explanation`
- `POST /ai/v1/rag/ask`
- `POST /ai/v1/knowledge/documents`
- `POST /ai/v1/knowledge/rebuild`

## 与 Java 的边界

- Python 不推进 `caries_biz` 业务状态。
- Python 不直接裁定病例、复核、报告最终结论。
- Python 向 Java 提供结构化 AI 结果、引用证据和运行留痕。
- Java 负责状态机、附件主数据和业务可见性。

## 启动

容器入口位于 [scripts/entrypoint.sh](./scripts/entrypoint.sh)。

主要行为：

1. 根据 `CG_DB_MIGRATION_ENABLED` 判断是否执行 Alembic。
2. 必要时先 `stamp` 再 `upgrade head`。
3. 启动 FastAPI HTTP 服务和 MQ Worker。

本地启动：

```powershell
python -m app.main
```

健康检查：

```text
http://127.0.0.1:8001/ai/v1/health
```

## 测试

示例：

```powershell
.\.venv\Scripts\python -m pytest tests\unit\test_rag_service.py tests\unit\test_inference_pipeline_phase5b.py tests\unit\test_schema_alias.py
```

`tests/unit/` 已覆盖回调、模型适配器、推理流水线、RAG、风险、存储和工作区清理逻辑。

## 相关文档

- [项目概览](../Documents/01_项目概览.md)
- [功能说明](../Documents/02_功能说明.md)
- [部署与运行](../Documents/03_部署与运行.md)
- [接口与集成说明](../Documents/04_接口与集成说明.md)
- [数据字典](../Documents/05_数据字典.md)
