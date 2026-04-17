# CariesGuard

CariesGuard is a dental caries screening, AI-assisted analysis, reporting, review, and follow-up platform.

## Architecture

```text
Java Backend (caries_biz)
  -> RabbitMQ -> Python AI/RAG (caries_ai)
  -> MinIO / Redis / Vector Index / General LLM Provider
```

Java owns the business workflow. Python provides AI and RAG capabilities.

- `caries_biz`: Java business domain (Flyway).
- `caries_ai`: Python AI/RAG runtime domain (Alembic).
- MinIO stores source images, visual assets, reports, and exports.

## AI Strategy

The current AI strategy is general LLM + knowledge-base RAG, not domain-specific fine-tuning.

- Image analysis produces structured outputs through replaceable adapters.
- RAG uses curated knowledge, citations, and structured case context.
- General LLM output is advisory and cannot directly change clinical business state.
- High uncertainty triggers review semantics.

## Docker

```powershell
docker compose up -d --build
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

## Docs

All long-term design documents live under [Documents/](Documents/):

- [01_架构设计.md](Documents/01_架构设计.md) — architecture, UML, flows
- [02_数据库设计.md](Documents/02_数据库设计.md) — schema, ownership, migrations
- [03_接口契约.md](Documents/03_接口契约.md) — API, callback, RAG, error codes
- [04_AI与RAG规范.md](Documents/04_AI与RAG规范.md) — AI architecture, runtime modes, RAG
- [05_开发规范.md](Documents/05_开发规范.md) — naming, logging, data annotation
