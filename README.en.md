# CariesGuard

CariesGuard is an AI-assisted decision support system for dental caries screening.

Its mainline is:

`image analysis -> uncertainty evaluation -> doctor review -> RAG explanation -> risk assessment -> report/follow-up`

AI output is advisory and traceable. It does not replace a doctor's diagnosis.

## Architecture

```text
Client / Script
  -> Java Backend
      -> caries_biz (MySQL)
      -> Redis
      -> RabbitMQ
      -> MinIO
      -> Python Backend
          -> caries_ai (MySQL)
          -> Vector Index
          -> General LLM Provider
```

Java owns the business workflow and state. Python provides AI and RAG capabilities.

## Quick Start

```powershell
docker compose up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait-for-health.ps1
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

Competition preset:

```powershell
docker compose --env-file env/competition.env up -d --build
```

## Documentation

Long-term project documents are kept under [Documents/](Documents/):

- [Project Overview](Documents/01_项目概览.md)
- [Feature Overview](Documents/02_功能说明.md)
- [Deployment and Runbook](Documents/03_部署与运行.md)
- [API and Integration](Documents/04_接口与集成说明.md)
- [Data Dictionary](Documents/05_数据字典.md)
- [Java Backend README](backend-java/README.md)
- [Python Backend README](backend-python/README.md)

## Notes

- `mock` is the default and most reproducible demo mode.
- `hybrid` is used when partial real adapters need to be demonstrated.
- `real` should only be used when all model/runtime dependencies are ready.
