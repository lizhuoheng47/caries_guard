# Python 端第一轮实施记录

## 阶段 1：Python 工程骨架规范化

### 改动模块

- `app/main.py`：改为应用入口，只负责启动 MQ worker 与 FastAPI。
- `app/core/`：新增配置、日志、异常、JSON、时间、hash 工具。
- `app/infra/`：新增 MQ consumer 与 MinIO client 目录。
- `app/schemas/`：新增 Pydantic schema 基础结构。
- `app/services/`：新增 image fetch、visual asset、callback、quality、risk 服务。
- `app/pipelines/`：新增 mock inference pipeline。
- `app/api/`：新增 FastAPI 路由结构。
- `Dockerfile`：推理镜像切换到 Python 3.11。
- `requirements.txt`：保留单入口并增加运行与测试依赖。

### 影响范围

- 只修改 `backend-python`。
- 未修改 `backend-java` 源码、配置、Profile、依赖。
- Docker 启动命令仍为 `python -m app.main`。

### 测试结果

- `docker compose build backend-python` 已通过。
- `docker compose run --rm --no-deps backend-python pytest tests/ -q` 已通过，5 passed。

### 已知问题

- 当前 Java 实际 DTO 仍使用 HMAC 回调和旧版 summary/risk 字段；Python 已保留兼容。
- `accessUrl` 仍作为过渡路径保留，但新逻辑优先 `bucketName + objectKey`。

### 回滚方式

- 回滚 `backend-python/app`、`backend-python/Dockerfile`、`backend-python/requirements.txt`、`backend-python/README.md` 到本阶段前版本。

## 阶段 2：MinIO 接入与影像读取改造

### 改动模块

- `app/infra/storage/minio_client.py`
- `app/services/image_fetch_service.py`
- `app/services/visual_asset_service.py`

### 影响范围

- Python 原图读取主路径改为 MinIO。
- `accessUrl` 仅保留为兼容路径。
- visual assets 统一上传到 `caries-visual`。

### 测试结果

- `tests/unit/test_minio_client.py` 覆盖 endpoint 规范化。
- `tests/unit/test_visual_asset_service.py` 覆盖 visual metadata、contentType、objectKey 后缀、MD5。
- `tests/unit/test_workspace_cleanup.py` 覆盖临时目录清理。

### 已知问题

- dev/test 可通过 `CG_MINIO_ALLOW_BUCKET_CREATE=true` 开启建桶；默认不自动建桶。

### 回滚方式

- 将 image fetch 调用退回旧 `accessUrl` 下载实现；保留 MinIO 文件不影响 Java。

## 阶段 3：接口契约与回调 Payload 对齐

### 改动模块

- `app/schemas/request.py`
- `app/schemas/callback.py`
- `app/services/callback_service.py`

### 影响范围

- 对外 JSON 通过 Pydantic alias 输出 camelCase。
- 当前正式回调签名仍为 `X-AI-Timestamp + X-AI-Signature`。
- `X-Callback-Token` 仅作为后续目标态预留。

### 测试结果

- `tests/unit/test_schema_alias.py` 覆盖 camelCase 输出与 snake_case 外泄检查。
- `tests/unit/test_callback_signature.py` 覆盖 HMAC 签名稳定性。

### 已知问题

- 当前 Java callback command 不接收完整新契约中的 `lesionResults` 顶层字段，因此 Python 将完整 mock 结果放入 `rawResultJson`，同时保留 Java 当前落库需要的 `visualAssets`。

### 回滚方式

- 回滚 schema 和 callback service；Java 侧无改动。

## 阶段 4：Mock 推理 Pipeline 与可视化产物

### 改动模块

- `app/pipelines/inference_pipeline.py`
- `app/services/quality_service.py`
- `app/services/risk_service.py`

### 影响范围

- mock pipeline 生成稳定 mask / overlay / heatmap。
- 当前可视化产物仅用于联调、演示和对象存储链路验证，不代表真实临床诊断结果。

### 测试结果

- 当前 mock pipeline 已完成代码实现；完整 MinIO 主链路需在 RabbitMQ、MinIO、Java callback 均启动后做端到端联调。

### 已知问题

- 无真实模型推理。

### 回滚方式

- 将 pipeline 回退为空 mock 回调，不影响 Java。

## 阶段 5：FastAPI 服务补齐

### 改动模块

- `app/api/app.py`
- `app/api/v1/*`

### 影响范围

- 新增 `/ai/v1/*` HTTP 接口。
- 主链路仍以 RabbitMQ worker 为主。
- HTTP analyze 与 MQ worker 共用同一 pipeline。

### 测试结果

- 容器内 TestClient 调用 `GET /ai/v1/health` 返回 HTTP 200、业务码 `00000`、状态 `UP`。

### 已知问题

- 根 `docker-compose.yml` 暂未映射 Python HTTP 端口到宿主机；Docker 网络内可访问。

### 回滚方式

- 设置 `CG_HTTP_ENABLED=false` 可关闭 HTTP 入口，保留 MQ worker。

## 阶段 6：测试、容器联调、回归记录

### 改动模块

- `tests/unit/*`
- `docs/Python端联调样例.md`

### 测试结果

- `python -m compileall app tests` 已通过。
- 本机 Python 为 3.10 且未安装 pytest，未作为正式测试环境。
- Docker 构建：`docker compose build backend-python` 已通过。
- Docker 单元测试：`docker compose run --rm --no-deps backend-python pytest tests/ -q` 已通过，5 passed。
- Docker HTTP 验证：容器内 TestClient 调用 `/ai/v1/health` 通过。

### 已知问题

- 尚未执行真实 RabbitMQ + MinIO + Java callback 的完整端到端任务。
- 根 `docker-compose.yml` 暂未把 Python 8001 端口映射到宿主机；当前 HTTP 接口面向 Docker 网络内访问。

### 回滚方式

- 测试和样例文档可独立回滚，不影响运行逻辑。
