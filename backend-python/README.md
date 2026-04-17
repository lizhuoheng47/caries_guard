# backend-python

CariesGuard Python AI 服务第一轮实现，按 `docs/Python端第一轮实施冻结计划.md` 执行。

当前版本定位：MinIO + Java 契约兼容 + Mock Pipeline。可视化产物仅用于联调、演示和对象存储链路验证，不代表真实临床诊断结果。

## 当前职责

1. 消费 RabbitMQ 队列 `caries.analysis.requested.queue`。
2. 接收 Java 下发的分析任务。
3. 优先使用 `images[].bucketName + images[].objectKey` 从 MinIO 读取原图。
4. `images[].accessUrl` 仅作为过渡兼容路径。
5. 执行 mock pipeline，生成 mask / overlay / heatmap。
6. 上传 visual assets 到 MinIO `caries-visual`。
7. 使用当前 Java 兼容的 `X-AI-Timestamp` 和 `X-AI-Signature` 完成 HMAC 回调。
8. 提供 `/ai/v1/*` FastAPI 补齐接口，主链路仍以 RabbitMQ worker 为主。
9. 提供 RAG 最小闭环：知识文档入库、索引重建、患者解释、医生问答和检索/生成日志留痕。

## 目录结构

```text
app/
  api/          # FastAPI route，只负责参数接收和响应包装
  core/         # 配置、日志、异常、时间、hash 等基础能力
  infra/        # MQ、MinIO、外部基础设施适配
  pipelines/    # 推理编排
  repositories/ # Python AI/RAG 运行元数据访问
  schemas/      # Pydantic 契约模型
  services/     # image fetch、visual upload、callback、risk、quality
  main.py       # 应用入口
```

`main.py` 仅作为入口，不承载业务逻辑。

## Docker 运行

在项目根目录执行：

```powershell
docker compose up --build backend-python
```

完整联调：

```powershell
docker compose up -d --build
```

查看 Python 日志：

```powershell
docker compose logs -f backend-python
```

## HTTP 接口

容器内地址：

```text
http://backend-python:8001
```

接口：

- `GET /ai/v1/health`
- `HEAD /ai/v1/health`
- `POST /ai/v1/quality-check`
- `POST /ai/v1/analyze`
- `POST /ai/v1/assess-risk`
- `GET /ai/v1/model-version`
- `POST /ai/v1/knowledge/documents`
- `POST /ai/v1/knowledge/rebuild`
- `POST /ai/v1/rag/patient-explanation`
- `POST /ai/v1/rag/doctor-qa`

`POST /ai/v1/analyze` 只返回已受理，实际分析通过后台任务执行并回调 Java。

## RAG 最小闭环

当前 RAG 实现用于联调和演示：

1. `POST /ai/v1/knowledge/documents` 写入知识文档元数据，只有 `reviewStatusCode=APPROVED` 的文档会进入索引。
2. `POST /ai/v1/knowledge/rebuild` 将文档切分为 chunk，并生成本地向量索引文件。
3. `POST /ai/v1/rag/patient-explanation` 基于检索结果生成患者版解释。
4. `POST /ai/v1/rag/doctor-qa` 基于检索结果生成医生端问答。
5. 每次 RAG 请求都会记录 `rag_session`、`rag_request_log`、`rag_retrieval_log`、`llm_call_log`。

本轮不引入外部大模型强依赖，`TemplateLlmClient` 用受控模板模拟通用大模型网关；后续可在 `app/infra/llm` 下替换为真实 HTTP LLM 适配器。本地向量索引位于 `CG_RAG_INDEX_DIR`，AI/RAG 元数据直接写入 MySQL。

## 关键环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `CG_APP_MODE` | `mock` | 当前运行模式 |
| `CG_HTTP_ENABLED` | `true` | 是否启动 FastAPI |
| `CG_HTTP_PORT` | `8001` | HTTP 端口 |
| `CG_MQ_WORKER_ENABLED` | `true` | 是否启动 RabbitMQ worker |
| `CG_RABBIT_HOST` | `rabbitmq` | RabbitMQ 地址 |
| `CG_ANALYSIS_EXCHANGE` | `caries.analysis.exchange` | 分析 exchange |
| `CG_ANALYSIS_REQUESTED_QUEUE` | `caries.analysis.requested.queue` | 分析请求队列 |
| `CG_JAVA_CALLBACK_URL` | `http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result` | Java 回调地址 |
| `CG_ANALYSIS_CALLBACK_SECRET` | `docker-change-me-to-a-strong-analysis-callback-secret` | HMAC 回调密钥 |
| `CG_CALLBACK_VISUAL_ASSET_MODE` | `metadata` | visual 回调模式。`metadata` 按冻结契约回传顶层 `visualAssets`；`legacy-empty` 仅作为历史兼容模式保留，不是当前 Docker 默认口径 |
| `CG_MODEL_VERSION` | `caries-v1` | mock 模型版本 |
| `CG_MINIO_ENDPOINT` | `http://minio:9000` | MinIO S3 API endpoint |
| `CG_MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `CG_MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `CG_MINIO_SECURE` | `false` | 是否使用 HTTPS |
| `CG_BUCKET_IMAGE` | `caries-image` | 原图 bucket，只读 |
| `CG_BUCKET_VISUAL` | `caries-visual` | visual assets bucket，读写 |
| `CG_BUCKET_REPORT` | `caries-report` | 报告 bucket，Python 本轮不写 |
| `CG_BUCKET_EXPORT` | `caries-export` | 导出 bucket，Python 本轮不写 |
| `CG_TEMP_DIR` | `/tmp/cariesguard` | 临时目录 |
| `CG_MYSQL_HOST` | `mysql` | Python AI/RAG 元数据 MySQL 主机 |
| `CG_MYSQL_PORT` | `3306` | Python AI/RAG 元数据 MySQL 端口 |
| `CG_MYSQL_DATABASE` | `caries_ai` | Python AI/RAG 元数据 MySQL 数据库（与 Java `caries_biz` 严格分离） |
| `CG_MYSQL_USERNAME` | `root` | Python AI/RAG 元数据 MySQL 用户名 |
| `CG_MYSQL_PASSWORD` | `1234` | Python AI/RAG 元数据 MySQL 密码 |
| `CG_MYSQL_CONNECT_TIMEOUT_SECONDS` | `5` | MySQL 连接超时 |
| `CG_RAG_INDEX_DIR` | `/tmp/cariesguard/vector-index` | 本地向量索引文件目录 |
| `CG_RAG_DEFAULT_KB_CODE` | `caries-default` | 默认知识库编码 |
| `CG_RAG_KNOWLEDGE_VERSION` | `v1.0` | 默认知识版本 |
| `CG_RAG_EMBEDDING_MODEL` | `hashing-embedding-v1` | 当前轻量 embedding 标识 |
| `CG_RAG_VECTOR_STORE_TYPE` | `LOCAL_JSON` | 当前向量索引类型 |
| `CG_RAG_TOP_K` | `5` | 默认检索 TopK |
| `CG_LLM_PROVIDER_CODE` | `MOCK` | 通用大模型网关提供方标识 |
| `CG_LLM_MODEL_NAME` | `template-llm-v1` | 当前文本生成模型标识 |

## Visual ObjectKey

固定规则：

```text
org/{orgId}/case/{caseNo}/analysis/{taskNo}/{modelVersion}/{assetTypeCode}/{relatedImageId}/{toothCode}/{attachmentId}.{ext}
```

`assetTypeCode` 使用 `MASK`、`OVERLAY`、`HEATMAP` 等枚举。visual assets 写入 `caries-visual`，默认 30 天自动清理。

contentType 固定规则：

- PNG：`image/png`
- JPG/JPEG：`image/jpeg`
- JSON：`application/json`

## 测试

容器构建：

```powershell
docker compose build backend-python
```

Python 单元测试：

```powershell
docker compose run --rm backend-python pytest tests/ -q
```

本轮禁止在推理镜像中一次性引入完整训练栈依赖。
