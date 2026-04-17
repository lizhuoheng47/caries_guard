# CariesGuard

CariesGuard 是一个龋病筛查、AI 辅助分析、报告、复核和随访管理平台。

## 架构

```text
Java Backend
  -> caries_biz
  -> RabbitMQ
  -> MinIO
  -> Redis
  -> Python AI/RAG
      -> caries_ai
      -> Vector Index
      -> General LLM Provider
```

Java 负责业务主链，Python 负责 AI/RAG 能力。

## AI 路线

当前路线已经统一为：

- 通用大模型 + 知识图库检索增强；
- 不采用业务专用大模型微调作为当前主路线；
- 影像质量、检测、分割、分级由可替换算法适配器输出结构化结果；
- 高 uncertainty 触发复核；
- 所有 AI/RAG 结果必须留痕。

## 已完成能力

- Java/Python analysis callback 主链；
- MinIO 原始影像和 visual asset；
- Phase 5A quality/detection；
- Phase 5B segmentation；
- Phase 5C grading + uncertainty；
- RAG 基础服务和知识库结构。

## 文档入口

- `Documents/1. 系统架构概述.md`
- `Documents/03_项目总体设计文档.md`
- `Documents/06_数据库总体设计与数据治理文档.md`
- `backend-python/docs/08_Python AI与RAG开发说明书.md`
- `backend-python/docs/A_Java_Python接口契约对照表.md`

阶段实施记录、临时联调样例和旧改造方案已删除，长期设计以以上文档为准。

## Docker

```powershell
docker compose up -d --build
docker compose ps
```

Python health:

```powershell
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

Java health:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```
