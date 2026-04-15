# backend-python

CariesGuard Python AI 服务骨架，用于 Docker 容器联调。

当前职责：

1. 消费 RabbitMQ 队列 `caries.analysis.requested.queue`。
2. 读取 Java 下发的 `AiAnalysisRequestDTO`。
3. 使用 `images[].accessUrl` 下载影像。
4. 执行占位推理逻辑。
5. 按 `AiAnalysisResultCallbackCommand` 结构回调 Java。
6. 使用 `X-AI-Timestamp` 和 `X-AI-Signature` 完成 HMAC 签名。

后续接入真实模型时，优先替换 `app/main.py` 中的 `run_inference()`。

## 本地 Docker 运行

在项目根目录执行：

```powershell
docker compose up --build
```

Python 容器默认环境变量：

| 变量 | 默认值 |
| --- | --- |
| `CG_RABBIT_HOST` | `rabbitmq` |
| `CG_RABBIT_PORT` | `5672` |
| `CG_ANALYSIS_EXCHANGE` | `caries.analysis.exchange` |
| `CG_ANALYSIS_REQUESTED_QUEUE` | `caries.analysis.requested.queue` |
| `CG_ANALYSIS_REQUESTED_ROUTING_KEY` | `analysis.requested` |
| `CG_JAVA_CALLBACK_URL` | `http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result` |
| `CG_ANALYSIS_CALLBACK_SECRET` | 必须与 Java `CARIES_ANALYSIS_CALLBACK_SECRET` 一致 |
| `CG_MODEL_VERSION` | `caries-v1` |

## 重要约定

Python 不需要读取 Java 本地目录。原图优先使用 Java 消息里的 `accessUrl` 获取；如果后续生成 heatmap/mask/overlay，应上传到 `CG_MINIO_BUCKET_VISUAL`，再在回调 `visualAssets` 中返回 `bucketName + objectKey`。