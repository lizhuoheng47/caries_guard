# CariesGuard

CariesGuard 是一个龋病筛查、AI 辅助分析、报告生成、复核和随访管理平台。

> English version: [README.en.md](README.en.md)

## 架构

```text
Java Backend (caries_biz)
  -> RabbitMQ -> Python AI/RAG (caries_ai)
  -> MinIO / Redis / Vector Index / General LLM Provider
```

Java 负责业务主链，Python 负责 AI / RAG 能力。

## AI 路线

- 通用大模型 + 知识图库检索增强；
- 不采用业务专用大模型微调作为当前主路线；
- 影像质量、检测、分割、分级由可替换算法适配器输出结构化结果；
- 高 uncertainty 触发复核；
- 所有 AI / RAG 结果留痕可审计。

## 已完成能力

- Java / Python analysis callback 主链；
- MinIO 原始影像和 visual asset；
- Phase 5A quality / detection；
- Phase 5B segmentation；
- Phase 5C grading + uncertainty；
- RAG 基础服务和知识库结构。

## 文档

所有长期设计文档位于 [Documents/](Documents/)：

- [01_架构设计.md](Documents/01_架构设计.md) — 系统架构、UML、核心链路、状态机
- [02_数据库设计.md](Documents/02_数据库设计.md) — 双库设计、数据字典、归属矩阵、迁移规范
- [03_接口契约.md](Documents/03_接口契约.md) — API、callback、RAG、错误码、Phase 5C 字段
- [04_AI与RAG规范.md](Documents/04_AI与RAG规范.md) — AI 架构、运行模式、grading、知识图库
- [05_开发规范.md](Documents/05_开发规范.md) — 命名、分层、日志、数据标注、合规

## Docker

启动：

```powershell
docker compose up -d --build
docker compose ps
```

核心服务：`mysql` / `redis` / `rabbitmq` / `minio` / `backend-java` / `backend-python`。

Docker 默认执行 Java Flyway 和 Python Alembic 迁移。

### 关键环境变量

AI 运行模式：

```env
CG_AI_RUNTIME_MODE=mock
CG_MODEL_QUALITY_ENABLED=false
CG_MODEL_TOOTH_DETECT_ENABLED=false
CG_MODEL_SEGMENTATION_ENABLED=false
CG_MODEL_GRADING_ENABLED=false
CG_UNCERTAINTY_REVIEW_THRESHOLD=0.35
```

故障验证：

```env
CG_SEGMENTATION_FORCE_FAIL=false
CG_GRADING_FORCE_FAIL=false
```

### 健康检查

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

### Phase 5C E2E

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\phase5-analysis-docker-e2e.ps1 -SkipComposeUp -Phase5COnly -WaitSeconds 180
```

验证内容：mock grading / hybrid 低 uncertainty / hybrid 高 uncertainty / real grading failure / callback 契约 / Java 落库 / MinIO visual assets。

## 数据库

- Java：`caries_biz`（Flyway 迁移）
- Python：`caries_ai`（Alembic 迁移）

两库物理隔离，通过 `task_no` / `case_id` / `image_id` / `trace_id` 建立弱引用。
