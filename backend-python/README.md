# CariesGuard Python AI service

该模块消费 RabbitMQ 分析任务，读取 MinIO 影像，执行质量检查、牙位候选、病灶分割、分级、风险计算和知识增强建议，并把结构化结果回调给 Java。

应用只通过仓库根目录的完整 Docker Compose 链路启动：

```powershell
docker compose up -d --build
```

运行链路固定启用全部阶段。其中 DC1000 UNet TorchScript 是真实分割模型；质量、牙位候选、分级和风险仍为规则实现。不存在面向用户的 mock、比赛或独立分割启动入口。

训练、导出和评估脚本位于 `training/scripts/`，它们不是应用启动模式。

健康检查：`http://127.0.0.1:8001/ai/v1/health`。

## 最小 RAG

- `knowledge-base/caries_guidance_v1.json`：从项目内 ICDAS、ICCMS 和国家卫生健康委 PDF 整理的版本化知识、C0-C3 项目映射及分级建议模板。
- `GET /ai/v1/knowledge/status`：查看加载状态。
- `POST /ai/v1/knowledge/search`：检查召回证据。
- `POST /ai/v1/knowledge/reload`：热加载知识文件。
- 默认使用本地 BM25 风格检索与模板生成；设置 `CG_RAG_LLM_ENABLED=true` 后使用兼容 OpenAI Chat Completions 协议的 Qwen 服务进行受约束生成。

RAG 结果通过 `knowledgeVersion`、`citations`、`evidenceRefs`、`clinicalSummary` 和 `treatmentPlan` 写入 `rawResultJson`；citation 额外包含 `sourceFile` 和 `sourcePages`，用于在前端和报告中显示原始 PDF 与页码。知识版本和证据引用同时写入 Java 回调顶层字段。外部大模型异常时自动降级到检索模板。
