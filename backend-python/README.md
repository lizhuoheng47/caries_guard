# CariesGuard Python AI/RAG Service

## 1. 定位

Python 服务提供 AI 推理、RAG 检索、通用大模型调用、visual asset 生成和 AI/RAG 运行日志。

当前路线：

- 通用大模型 + 知识图库；
- 不以业务专用微调大模型作为主路线；
- 影像链路使用可替换 adapter；
- 所有结果进入可追溯 JSON。

## 2. 运行

```powershell
cd backend-python
.\.venv\Scripts\python.exe -m pytest tests\unit
```

Docker:

```powershell
docker compose up -d --build backend-python
```

Health:

```powershell
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

## 3. 运行模式

- `mock`
- `hybrid`
- `real`

模块开关：

- `CG_MODEL_QUALITY_ENABLED`
- `CG_MODEL_TOOTH_DETECT_ENABLED`
- `CG_MODEL_SEGMENTATION_ENABLED`
- `CG_MODEL_GRADING_ENABLED`

## 4. Phase 5C

`rawResultJson` 必须包含：

- `gradingMode`
- `gradingImplType`
- `gradingLabel`
- `confidenceScore`
- `uncertaintyMode`
- `uncertaintyImplType`
- `uncertaintyScore`
- `needsReview`

## 5. 文档

- `docs/08_Python AI与RAG开发说明书.md`
- `docs/09_Python AI与RAG数据库设计与数据字典.md`
- `docs/A_Java_Python接口契约对照表.md`
- `docs/B_CariesGuard数据现状清单与采集计划.md`
