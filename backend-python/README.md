# backend-python

`backend-python/` 是 CariesGuard 的 AI / RAG 能力提供方，不是业务状态权威。

它负责围绕龋病筛查主线提供受控的模型推理、RAG 解释、风险融合和运行治理能力，并通过 MQ 消费与 callback 回传和 Java 主链协作。

## 当前能力范围

Python 侧当前实现的是一条可追踪的 AI 能力链：

- quality
- detection
- segmentation
- grading
- uncertainty
- risk
- rag
- llm gateway
- governance logging

重点不是“模型多强”，而是输出是否结构化、可引用、可复核、可留痕。

## 当前主线

### 影像分析

- 消费 Java 投递的 analysis task
- 下载脱敏影像
- 执行质量检查、检测、分割、分级、不确定性和风险融合
- 生成 visual assets 元数据
- 回传 `rawResultJson`、`riskLevel`、`riskFactors`、`reviewReason`、`knowledgeVersion` 等字段

### RAG

- 知识文档入库和版本管理
- chunk 切分与索引构建
- 向量检索
- 结构化病例上下文组装
- 受控 LLM 生成
- 返回 `answer`、`citations`、`retrievedChunks`、`knowledgeVersion`、`safetyFlags`

### 治理

- `rag_request_log`
- `rag_retrieval_log`
- `llm_call_log`
- AI 模型/知识版本信息
- callback 与推理过程日志

## 运行模式

`app/core/config.py` 中定义了三种运行模式：

- `mock`：全部模块使用 mock，最稳定，适合默认演示和开发
- `hybrid`：开启的模块使用真实适配器，其余仍用 mock
- `real`：所有模块都必须走真实适配器，失败时显式报错，不静默回退

对应环境变量：

```env
CG_AI_RUNTIME_MODE=mock
CG_MODEL_QUALITY_ENABLED=false
CG_MODEL_TOOTH_DETECT_ENABLED=false
CG_MODEL_SEGMENTATION_ENABLED=false
CG_MODEL_GRADING_ENABLED=false
CG_MODEL_RISK_ENABLED=false
```

## 知识与 LLM 默认值

当前代码默认值来自 [config.py](/E:/caries_guard/backend-python/app/core/config.py)：

- `CG_RAG_KNOWLEDGE_VERSION=v1.0`
- `CG_LLM_PROVIDER_CODE=MOCK`
- `CG_LLM_MODEL_NAME=template-llm-v1`

这些默认值的作用是保证仓库在没有外部大模型依赖时也能稳定复现解释链路。

## 与 Java 的边界

- Python 不负责业务主表状态推进
- Python 不直接代替 Java 做病例、复核、报告的最终业务裁定
- Python 向 Java 提供结构化 AI 结果和解释证据
- Java 决定任务状态、复核状态和对外业务可见性

## Docker 启动行为

容器入口是 [scripts/entrypoint.sh](/E:/caries_guard/backend-python/scripts/entrypoint.sh)：

1. 根据 `CG_DB_MIGRATION_ENABLED` 决定是否执行 Alembic
2. 在已有业务表但无 `alembic_version` 时先 `stamp`
3. 之后执行 `alembic upgrade head`
4. 最终启动 `python -m app.main`

当前 Python 健康检查地址：

```text
http://127.0.0.1:8001/ai/v1/health
```

## 常用命令

单元测试：

```powershell
.\.venv\Scripts\python -m pytest tests\unit\test_rag_service.py tests\unit\test_inference_pipeline_phase5b.py tests\unit\test_schema_alias.py
```

本地直接启动应用时，建议先准备 MySQL、RabbitMQ、MinIO 和必要环境变量，再运行：

```powershell
python -m app.main
```

完整联调仍推荐使用仓库根目录的 `docker compose up -d --build`。

## 对外叙事限制

本目录相关文档和说明统一遵守以下约束：

- 不把 Python 服务宣传成“高精度医学大模型”
- 不把 RAG 描述成自由聊天机器人
- 不把 AI 输出描述成最终诊断
- 强调受控解释、证据引用、uncertainty 复核和日志治理
