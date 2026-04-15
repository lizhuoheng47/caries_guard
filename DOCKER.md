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

## 2. 服务地址

| 服务 | 容器内地址 | 宿主机访问 |
| --- | --- | --- |
| Java 后端 | `http://backend-java:8080` | `http://127.0.0.1:8080` |
| Python AI | `backend-python` | 无 HTTP 端口，消费 RabbitMQ |
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

Python 不需要读取 Java 本地目录，也不需要直接访问 MinIO。Python 只消费 Java 消息中的：

```json
{
  "images": [
    {
      "accessUrl": "http://backend-java:8080/api/v1/files/.../content?...",
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
4. Python 通过 `accessUrl` 下载影像。
5. Python 回调 Java：
   - `POST http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result`
6. 回调使用 HMAC header：
   - `X-AI-Timestamp`
   - `X-AI-Signature`

## 5. Python 开发入口

Python 服务目录：`backend-python`

当前主要文件：

| 文件 | 作用 |
| --- | --- |
| `backend-python/app/main.py` | RabbitMQ 消费、取图、模拟推理、回调 Java |
| `backend-python/app/config.py` | 环境变量配置 |
| `backend-python/app/callback_signature.py` | HMAC 签名 |
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