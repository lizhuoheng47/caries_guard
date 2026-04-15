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

## 目录结构

```text
app/
  api/          # FastAPI route，只负责参数接收和响应包装
  core/         # 配置、日志、异常、时间、hash 等基础能力
  infra/        # MQ、MinIO、外部基础设施适配
  pipelines/    # 推理编排
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

`POST /ai/v1/analyze` 只返回已受理，实际分析通过后台任务执行并回调 Java。

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

## Visual ObjectKey

固定规则：

```text
visual/{yyyy}/{MM}/{dd}/{caseNo}/mask_{imageId}_{toothCode}.png
visual/{yyyy}/{MM}/{dd}/{caseNo}/overlay_{imageId}_{toothCode}.png
visual/{yyyy}/{MM}/{dd}/{caseNo}/heatmap_{imageId}.png
```

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
