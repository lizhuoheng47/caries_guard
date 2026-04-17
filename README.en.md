# CariesGuard

CariesGuard is a dental caries screening, AI-assisted analysis, reporting, review, and follow-up platform.

## Architecture

Java owns the business workflow. Python provides AI and RAG capabilities.

```text
Java Backend -> RabbitMQ -> Python AI/RAG -> Java Callback
```

Databases:

- `caries_biz`: Java business domain.
- `caries_ai`: Python AI/RAG runtime domain.

Storage:

- MinIO stores source images, visual assets, reports, and exports.

## AI Strategy

The current AI strategy is general LLM + knowledge-base RAG, not domain-specific LLM fine-tuning.

- Image analysis produces structured outputs through replaceable adapters.
- RAG uses curated knowledge, citations, and structured case context.
- General LLM output is advisory and cannot directly change clinical business state.
- High uncertainty triggers review semantics.

## Key Docs

- `Documents/1. 系统架构概述.md`
- `Documents/03_项目总体设计文档.md`
- `backend-python/docs/08_Python AI与RAG开发说明书.md`
- `backend-python/docs/A_Java_Python接口契约对照表.md`

Temporary implementation notes and phase process documents have been removed.
