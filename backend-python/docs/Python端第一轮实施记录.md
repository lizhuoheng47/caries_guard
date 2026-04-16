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

## Docker 全链路联调记录（2026-04-15）

### 改动模块

- 未修改 Java 源码、Java 配置、Java profile。
- 未新增 Python 业务代码；本轮仅执行 Docker 联调与结果记录。

### 影响范围

- 启动容器：`mysql`、`redis`、`rabbitmq`、`minio`、`backend-java`、`backend-python`。
- Python worker 使用 RabbitMQ 消费 `caries.analysis.requested.queue`。
- Python 从 MinIO `caries-image` 读取测试原图，并向 `caries-visual` 写入 mock visual assets。
- Python 使用 `X-AI-Timestamp + X-AI-Signature` 向 Java callback endpoint 回调。

### 测试结果

- MinIO 测试原图已上传：`caries-image/case-image/2026/04/15/CASEE2E0001/pan_01.jpg`。
- MySQL 已写入限定测试数据：`taskNo=TASKE2E0001`、`caseNo=CASEE2E0001`。
- RabbitMQ 消息已发送到 `caries.analysis.exchange`，routing key 为 `analysis.requested`。
- Python worker 已消费任务，并完成 mock pipeline。
- Python 已生成并上传 3 个 visual assets：
  - `visual/2026/04/15/CASEE2E0001/mask_990001_16.png`
  - `visual/2026/04/15/CASEE2E0001/overlay_990001_16.png`
  - `visual/2026/04/15/CASEE2E0001/heatmap_990001.png`
- Java success callback 返回 HTTP 500；Python 重试 3 次后发送 FAILED callback，FAILED callback 被 Java 接收。

### 已知问题

- 当前运行的 Java Docker 镜像保存 `ana_visual_asset` 时仍要求 `visualAssets[].attachmentId` 非空，未使用当前源码中已存在的 `bucketName + objectKey` 自动登记 attachment 逻辑。
- Java 日志中的失败 SQL 为：`INSERT INTO ana_visual_asset (...)` 未包含 `attachment_id`，触发 MySQL `Field 'attachment_id' doesn't have a default value`。
- 这与本轮 Python 冻结计划不一致：Python 新逻辑按 MinIO 契约回传 `bucketName + objectKey + contentType + fileSizeBytes + md5`，不应直接连接业务 MySQL 生成或写入 Java attachmentId。
- 因此，本次联调结论为：Python MQ -> MinIO -> mock pipeline -> visual upload 已通过；Java success callback 入库链路被当前运行 Java 镜像的旧 visual asset 契约阻塞。

### 回滚方式

- 删除本次测试数据时，仅按固定测试范围清理：`taskNo=TASKE2E0001`、`caseNo=CASEE2E0001`、`visual/2026/04/15/CASEE2E0001/`。
- MinIO 测试对象可删除，不影响 Python 运行逻辑。
- Python 代码无需回滚。

### 下一步

- 不修改 Python MinIO 主契约。
- 待 Java Docker 镜像升级到支持 `visualAssets[].bucketName + objectKey` 自动登记 attachment 后，重新执行同一条 Docker 全链路用例。

## Docker 全链路兼容复测记录（2026-04-15）

### 改动模块

- `app/core/config.py`：新增 `CG_CALLBACK_VISUAL_ASSET_MODE`。
- `app/pipelines/inference_pipeline.py`：新增顶层 `visualAssets` 兼容输出策略。
- `tests/unit/test_callback_visual_asset_mode.py`：新增 callback visual asset 模式测试。
- `docker-compose.yml`：仅调整 Python 容器环境变量，默认设置 `CG_CALLBACK_VISUAL_ASSET_MODE=legacy-empty`，未修改 Java 环境。
- `README.md`、`DOCKER.md`：补充兼容模式说明。

### 影响范围

- 默认代码契约仍为 `metadata`：顶层 `visualAssets` 按 MinIO metadata 回传。
- 当前 Docker Compose 使用 `legacy-empty` 兼容旧 Java 镜像：顶层 `visualAssets` 置空，完整 visual metadata 保留在 `rawResultJson.visualAssets`。
- Python 不直接连接业务 MySQL，不生成 Java `attachmentId`，不改 Java 镜像、源码、配置。

### 测试结果

- 本地测试：`.venv` 执行 `python -m pytest tests/ -q` 通过，结果为 7 passed。
- Docker 测试：`docker compose run --rm --no-deps backend-python pytest tests/ -q` 通过，结果为 7 passed。
- Docker 全链路复测任务：`taskNo=TASKE2E0002`、`caseNo=CASEE2E0002`。
- Python worker 已消费 RabbitMQ 消息并完成 mock pipeline。
- Python 已上传 3 个 visual assets：
  - `visual/2026/04/15/CASEE2E0002/mask_990002_16.png`
  - `visual/2026/04/15/CASEE2E0002/overlay_990002_16.png`
  - `visual/2026/04/15/CASEE2E0002/heatmap_990002.png`
- Java success callback 已接收，`ana_task_record.task_status_code=SUCCESS`。
- 病例状态已流转为 `REVIEW_PENDING`。
- `ana_result_summary` 已落库 1 条，`med_risk_assessment_record` 已落库 1 条。
- 因旧 Java 镜像兼容模式顶层 `visualAssets` 置空，`ana_visual_asset` 本轮复测为 0 条；visual metadata 可在 `ana_result_summary.raw_result_json.visualAssets` 查询。

### 已知问题

- `legacy-empty` 仅用于当前旧 Java Docker 镜像联调，不是目标态。
- 目标态仍要求 Java 支持 `visualAssets[].bucketName + objectKey` 自动登记 attachment，并写入 `ana_visual_asset`。
- Java 镜像升级后需要将 `CG_CALLBACK_VISUAL_ASSET_MODE` 切回 `metadata` 并复测。

### 回滚方式

- 删除 `CG_CALLBACK_VISUAL_ASSET_MODE` compose 覆盖后，Python 代码默认回到 `metadata`。
- 删除本次测试数据时，仅按固定测试范围清理：`taskNo=TASKE2E0002`、`caseNo=CASEE2E0002`、`visual/2026/04/15/CASEE2E0002/`。
