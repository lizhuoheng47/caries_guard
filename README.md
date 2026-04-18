# CariesGuard

CariesGuard 是一个面向龋病筛查场景的多模态医疗 AI 辅助决策系统。项目保留 Java 业务主链与 Python AI/RAG 双后端协作架构，把影像分析、uncertainty 复核、RAG 解释、风险评估与医生反馈回流组织成可追踪的 AI 闭环。

> English version: [README.en.md](README.en.md)

## 当前定位

- 场景：口腔龋病筛查、解释与随访建议
- 方法：影像分析 + 检索增强生成 + 风险融合 + 医生复核反馈
- 边界：AI 用于辅助决策，不替代医生最终诊断

## 核心链路

```text
脱敏口腔影像 / 病例上下文 / 医学知识文档 / 医生反馈
  -> Java Backend (业务主链、状态权威、任务调度)
  -> RabbitMQ
  -> Python AI/RAG (影像推理、风险融合、RAG、治理日志)
  -> callback 回传结构化结果
  -> 医生复核 / 报告 / 随访 / 反馈回流
```

## 已落地能力

- 影像分析流水线：quality / detection / segmentation / grading / uncertainty
- 高 uncertainty 自动触发复核语义
- 风险融合输出：`riskLevel` / `riskFactors` / `followUpRecommendation`
- RAG 输出：回答、引用、命中 chunk、安全标志、上下文摘要
- 医生修正反馈闭环与训练候选导出
- AI 运行日志、知识库日志、模型版本治理
- MinIO visual assets 留痕与 callback 契约落库

## 工程原则

- Java 仍是业务主链和状态权威
- Python 仍是 AI / RAG 能力提供方
- MQ + callback 主链保持不变
- `rawResultJson` 作为 AI 扩展字段主入口
- `real` 模式失败必须显式失败，不允许静默回退

## 目录

- `backend-java/`: 业务主链、报告、复核、仪表盘与集成层
- `backend-python/`: AI 推理、RAG、知识治理、运行日志
- `Documents/`: 架构、接口、AI 规范与比赛导向文档
- `scripts/`: E2E 与工程脚本
- `infra/`: 部署与基础设施配置

## 文档

长期设计与比赛导向文档位于 `Documents/`：

- `01_架构设计.md`
- `02_数据库设计.md`
- `03_接口契约.md`
- `04_AI与RAG规范.md`
- `05_开发规范.md`
- `06_人工智能实践赛改造说明.md`
- `07_AI闭环演示脚本.md`
- `08_AI评估指标与实验设计.md`
- `09_知识图库建设与治理规范.md`
- `10_答辩问答与作品话术.md`

## Docker

启动：

```powershell
docker compose up -d --build
docker compose ps
```

健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

Phase 5 分析链路 E2E：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\phase5-analysis-docker-e2e.ps1 -SkipComposeUp -Phase5COnly -WaitSeconds 180
```

## 关键环境变量

```env
CG_AI_RUNTIME_MODE=mock
CG_MODEL_QUALITY_ENABLED=false
CG_MODEL_TOOTH_DETECT_ENABLED=false
CG_MODEL_SEGMENTATION_ENABLED=false
CG_MODEL_GRADING_ENABLED=false
CG_MODEL_RISK_ENABLED=false
CG_UNCERTAINTY_REVIEW_THRESHOLD=0.35
CG_RAG_KNOWLEDGE_VERSION=v1.0
```

## 项目目标

这个仓库当前的目标不是继续堆后台页面，而是把现有系统稳定收敛成一个：

- 可解释
- 可追踪
- 可评估
- 可治理
- 可演示

的医疗 AI 实践闭环。
