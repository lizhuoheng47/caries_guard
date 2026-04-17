# Docker 运行说明

## 1. 启动

```powershell
docker compose up -d --build
docker compose ps
```

核心服务：

- `mysql`
- `redis`
- `rabbitmq`
- `minio`
- `backend-java`
- `backend-python`

## 2. 环境变量

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

## 3. 数据库

- Java：`caries_biz`
- Python：`caries_ai`

Docker 默认执行 Java Flyway 和 Python Alembic 迁移。

## 4. 健康检查

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

## 5. Phase 5C E2E

只跑 Phase 5C：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\phase5-analysis-docker-e2e.ps1 -SkipComposeUp -Phase5COnly -WaitSeconds 180
```

验证内容：

- mock grading；
- hybrid grading 低 uncertainty；
- hybrid grading 高 uncertainty；
- real grading failure；
- callback 契约；
- Java 落库；
- MinIO visual assets。

## 6. AI 路线说明

Docker 环境中的 AI 服务采用通用大模型 + 知识图库 RAG 方案。当前不要求部署业务专用微调大模型。影像分析模块通过算法适配器运行，RAG 模块通过知识库和通用大模型 provider 运行。
