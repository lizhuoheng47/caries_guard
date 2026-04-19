# CariesGuard

CariesGuard 是一个面向龋病筛查场景的医疗 AI 辅助决策系统，主线固定为：

`影像分析 -> 不确定性评估 -> 医生复核 -> RAG 解释 -> 风险评估 -> 报告/随访`

AI 用于辅助分析、解释和风险提示，不替代医生最终诊断。

> English version: [README.en.md](README.en.md)

## 架构概览

```text
Client / Script
  -> frontend-web
  -> Java Backend
      -> caries_biz (MySQL)
      -> Redis
      -> RabbitMQ
      -> MinIO
      -> Python Backend
          -> caries_ai (MySQL)
          -> OpenSearch
          -> Neo4j
          -> OpenAI-compatible LLM Provider
```

职责边界：

- Java 负责业务主链、状态机、权限、报告、复核、随访和对外 API。
- Python 负责 AI 推理、RAG、知识库、运行日志和模型治理。
- RabbitMQ 负责分析任务异步投递。
- MinIO 负责原始影像、可视化产物、报告和导出文件存储。

## 核心能力

- 患者、就诊、病例、影像和附件管理
- AI 分析任务创建、异步执行和结果回流
- 医生复核、纠偏反馈和训练候选样本导出
- 报告生成、RAG 问答、患者解释
- 随访计划、随访任务、随访记录
- 业务看板、AI 运行时看板、模型治理

## 快速启动

```powershell
docker compose up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait-for-health.ps1
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

比赛演示模式：

```powershell
docker compose --env-file env/competition.env up -d --build
```

默认管理员账号：

- 用户名：`admin`
- 密码：`123456`

## 文档入口

- [项目概览](Documents/01_项目概览.md)
- [功能说明](Documents/02_功能说明.md)
- [部署与运行](Documents/03_部署与运行.md)
- [接口与集成说明](Documents/04_接口与集成说明.md)
- [数据字典](Documents/05_数据字典.md)
- [Java 后端说明](backend-java/README.md)
- [Python 后端说明](backend-python/README.md)

## 仓库结构

- `backend-java/`：Java 业务后端，多模块 Maven 工程
- `backend-python/`：Python AI/RAG 服务，FastAPI + MQ Worker
- `frontend-web/`：Vue 3 + Vite + TypeScript 后台前端，仅调用 Java BFF
- `Documents/`：保留后的项目正式文档
- `scripts/`：启动、验收、演示和灌数脚本
- `infra/`：基础设施初始化资源
- `env/`：环境预设

## 运行建议

- 正式知识治理与 RAG 主路径为 `OpenSearch + Neo4j + OpenAI-compatible provider`。
- `mock`、`LOCAL_JSON` 仅用于开发兜底或局部离线联调，不作为正式默认路径。
- `real` 模式应在模型、依赖、回调链路、索引与图谱都已确认时使用。
