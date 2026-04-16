# Docker 容器联调说明

当前采用第三种联调方式：Java 后端、Python AI 服务、MySQL、Redis、RabbitMQ、MinIO 全部运行在 Docker Compose 网络中。

## 1. 启动

在项目根目录执行：

```powershell
docker compose up --build
```

后台启动：

```powershell
docker compose up -d --build
```

停止：

```powershell
docker compose down
```

如果要清空数据库和 MinIO 数据：

```powershell
docker compose down -v
```

当前数据库迁移已合并为单基线 `V001__baseline_schema.sql`。如果本机 Docker volume 里已经存在旧的 `flyway_schema_history`，需要先执行 `docker compose down -v` 后再启动；不要在有业务数据的库上直接清理。

## 2. 服务地址

| 服务 | 容器内地址 | 宿主机访问 |
| --- | --- | --- |
| Java 后端 | `http://backend-java:8080` | `http://127.0.0.1:8080` |
| Python AI | `http://backend-python:8001` | 默认未映射宿主机端口，主链路消费 RabbitMQ |
| MySQL | `mysql:3306` | `127.0.0.1:13306` |
| Redis | `redis:6379` | `127.0.0.1:16379` |
| RabbitMQ | `rabbitmq:5672` | `127.0.0.1:5672` |
| RabbitMQ 管理台 | `http://rabbitmq:15672` | `http://127.0.0.1:15672` |
| MinIO API | `http://minio:9000` | `http://127.0.0.1:9000` |
| MinIO Console | `http://minio:9001` | `http://127.0.0.1:9001` |

默认账号：

| 服务 | 用户名 | 密码 |
| --- | --- | --- |
| MySQL | `root` | `1234` |
| RabbitMQ | `guest` | `guest` |
| MinIO | `minioadmin` | `minioadmin` |

## 3. 固定配置原则

Java Docker profile 固定为：

```yaml
spring.profiles.active: docker
caries.storage.provider: MINIO
caries.storage.endpoint: http://minio:9000
```

Python 不读取 Java 本地目录。Python 当前优先消费 Java 消息中的 `bucketName + objectKey` 并直接访问 MinIO；`accessUrl` 为 Java 生成的 MinIO presigned GET URL，作为受控兼容路径。

```json
{
  "images": [
    {
      "bucketName": "caries-image",
      "objectKey": "org/1001/case/CASE202604150001/image/PANORAMIC/2026/04/15/40001/pan_01.jpg",
      "accessUrl": "http://minio:9000/caries-image/org/1001/case/CASE202604150001/image/PANORAMIC/2026/04/15/40001/pan_01.jpg?X-Amz-Algorithm=...",
      "storageProviderCode": "MINIO"
    }
  ]
}
```

## 4. Java/Python 消息链路

1. Java 创建 AI 分析任务。
2. Java 发布消息到 RabbitMQ：
   - exchange：`caries.analysis.exchange`
   - queue：`caries.analysis.requested.queue`
   - routing key：`analysis.requested`
3. Python 消费该队列。
4. Python 优先通过 MinIO `bucketName + objectKey` 下载影像；缺失时才走 `accessUrl` 兼容路径。
5. Python 生成 mock mask / overlay / heatmap 并上传到 `caries-visual`。
6. Python 回调 Java：
   - `POST http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result`
7. 回调使用 HMAC header：
   - `X-AI-Timestamp`
   - `X-AI-Signature`

当前 Docker Compose 默认给 Python 设置 `CG_CALLBACK_VISUAL_ASSET_MODE=metadata`。Python 生成并上传 visual assets 到 `caries-visual` 后，在顶层 `visualAssets` 回调 `bucketName + objectKey`，Java 会登记为 `med_attachment` 并写入 `ana_visual_asset`。

## 5. Python 开发入口

Python 服务目录：`backend-python`

当前主要文件：

| 文件 | 作用 |
| --- | --- |
| `backend-python/app/main.py` | RabbitMQ 消费、取图、模拟推理、回调 Java |
| `backend-python/app/config.py` | 环境变量配置 |
| `backend-python/app/callback_signature.py` | HMAC 签名 |
| `backend-python/app/infra/storage/minio_client.py` | MinIO 读写 |
| `backend-python/app/pipelines/inference_pipeline.py` | mock pipeline |
| `backend-python/requirements.txt` | Python 依赖 |
| `backend-python/Dockerfile` | Python 服务镜像 |

接入真实模型时，优先替换：

```python
run_inference(task)
```

不要改 Java 配置。

## 6. 常用排查命令

查看 Java 日志：

```powershell
docker compose logs -f backend-java
```

查看 Python 日志：

```powershell
docker compose logs -f backend-python
```

查看 RabbitMQ 队列：

```powershell
# 浏览器打开
http://127.0.0.1:15672
```

查看 MinIO：

```powershell
# 浏览器打开
http://127.0.0.1:9001
```
