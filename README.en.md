# CariesGuard

CariesGuard is an AI-assisted decision support system for dental caries screening.

Its current mainline is:

`image upload -> AI analysis -> uncertainty evaluation -> doctor review -> RAG explanation -> risk assessment -> report / follow-up`

AI output is advisory and traceable. It does not replace a doctor's diagnosis.

> 中文版: [README.md](README.md)

## Current Status

The repository currently supports an end-to-end flow across the main UI:

- create a case
- upload an X-ray image
- create an analysis task
- view analysis details
- enter the review workbench
- submit review results

Main frontend routes currently wired:

- `dashboard/ai`
- `analysis`
- `analysis/:taskId`
- `review/:taskId`
- `cases`
- `rag`
- `knowledge`

Additional notes:

- The review workbench already supports queue switching, image linkage, and side-by-side display of original vs current review content.
- The case portal already supports click-to-select, drag-and-drop upload, and the full client-side chain from patient creation to analysis submission.
- Critical IDs in the case creation flow are now passed as strings to avoid JavaScript precision loss on Java `Long` values.

## Architecture

```text
Browser / Script
  -> frontend/ (Vue 3 + Vite)
  -> backend-java/ (Spring Boot, business workflow)
      -> caries_biz (MySQL)
      -> Redis
      -> RabbitMQ
      -> MinIO
      -> backend-python/ (FastAPI, AI / RAG)
          -> caries_ai (MySQL)
          -> OpenSearch
          -> Neo4j
          -> OpenAI-compatible LLM Provider
```

Responsibility split:

- Java owns the business workflow, state transitions, permissions, reports, review, follow-up, and public APIs.
- Python provides AI inference, RAG, knowledge processing, runtime logs, and model governance.
- RabbitMQ carries async analysis tasks.
- MinIO stores source images, visual assets, reports, and exported files.

## Repository Layout

- `frontend/`: Vue 3 + Vite + TypeScript frontend
- `backend-java/`: Java business backend, multi-module Maven project
- `backend-python/`: Python AI / RAG service, FastAPI + MQ worker
- `Documents/`: long-term project documents
- `scripts/`: startup, acceptance, seeding, and helper scripts
- `infra/`: infrastructure bootstrap resources
- `env/`: environment presets

## Tooling

Verified locally in this workspace:

- Node.js `v24.13.0`
- Python `3.10.11`
- Java `17.0.12`
- Maven `3.9.12`

Recommended minimums:

- Node.js 20+
- Python 3.10+
- JDK 17+
- Maven 3.9+
- Docker / Docker Compose optional, but recommended

## Quick Start

### Option A: Docker for backend + infra, frontend locally

The current `docker-compose.yml` starts:

- MySQL
- Redis
- RabbitMQ
- MinIO
- OpenSearch
- Neo4j
- Java backend
- Python backend

It does not start the frontend, so the frontend still needs to run locally.

1. Start containers:

```powershell
docker compose up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait-for-health.ps1
```

2. Check health:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

3. Start the frontend:

```powershell
cd frontend
npm install
npm run dev
```

4. Open:

- Frontend dev server: `http://127.0.0.1:5173`
- Java health: `http://127.0.0.1:8080/actuator/health`
- Python health: `http://127.0.0.1:8001/ai/v1/health`
- RabbitMQ management: `http://127.0.0.1:15672`
- MinIO Console: `http://127.0.0.1:9001`
- Neo4j Browser: `http://127.0.0.1:7474`

Additional default Docker host ports:

- MySQL: `13306`
- Redis: `16379`
- OpenSearch: `9200`

Competition demo preset:

```powershell
docker compose --env-file env/competition.env up -d --build
```

### Option B: Fully local, no Docker

Full local integration requires these services first:

- MySQL 8.x
- Redis
- RabbitMQ
- MinIO
- OpenSearch
- Neo4j

Reference configuration:

- [application-local.yml](backend-java/caries-boot/src/main/resources/application-local.yml)
- [config.py](backend-python/app/core/config.py)
- [vite.config.ts](frontend/vite.config.ts)

Recommended local defaults:

- MySQL: `127.0.0.1:3306`, databases `caries_biz` / `caries_ai`
- Redis: `127.0.0.1:6379`
- RabbitMQ: `127.0.0.1:5672`
- MinIO: `http://127.0.0.1:9000`
- OpenSearch: `http://127.0.0.1:9200`
- Neo4j: `bolt://127.0.0.1:7687`

Java backend:

```powershell
cd backend-java
$env:SPRING_PROFILES_ACTIVE="local"
mvn -pl caries-boot -am spring-boot:run
```

Python backend:

```powershell
cd backend-python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m app.main
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

## Default Account

- Username: `admin`
- Password: `123456`

## Development Commands

Frontend build:

```powershell
cd frontend
npm run build
```

Java compile:

```powershell
cd backend-java
mvn -pl caries-boot -am -DskipTests compile
```

Python unit test example:

```powershell
cd backend-python
.\.venv\Scripts\python -m pytest tests\unit\test_rag_service.py
```

## Frontend Integration Notes

- The frontend reads [frontend/.env](frontend/.env) by default:
  - `VITE_API_BASE_URL=/api/v1`
  - `VITE_USE_MOCK=false`
- Vite proxies `/api` to `http://localhost:8080`.
- For local development, the Java backend should stay on port `8080`.

If you only want to view the UI quickly, you can temporarily switch `frontend/.env` to:

```env
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCK=true
```

But note:

- not every page is fully mocked
- `cases`, `analysis`, and `review` are better exercised against the real backend

## Current Integration Caveats

- Docker Compose does not start the frontend. This is the easiest thing to miss.
- The case creation flow depends on MinIO, RabbitMQ, and MySQL. If any of them is down, "Create and Analyze" can fail.
- The review workbench frontend is usable now, but if the backend does not return enough review queue data, the frontend falls back to analysis tasks as review entry points.
- Python `mock` mode remains the safest default for development and demos. Use `real` only when models, callbacks, storage, and knowledge dependencies are all ready.

## Documentation

- [Project Overview](Documents/01_项目概览.md)
- [Feature Overview](Documents/02_功能说明.md)
- [Deployment and Runbook](Documents/03_部署与运行.md)
- [API and Integration](Documents/04_接口与集成说明.md)
- [Data Dictionary](Documents/05_数据字典.md)
- [UI to API Mapping](UI页面与前后端接口映射表.md)
- [Java Backend README](backend-java/README.md)
- [Python Backend README](backend-python/README.md)

## Recommendations

- Use Python `mock` mode first for development and demos.
- The intended production RAG path is `OpenSearch + Neo4j + OpenAI-compatible provider`.
- Use `real` mode only after model weights, indexes, graph data, callbacks, and object storage are fully validated.
