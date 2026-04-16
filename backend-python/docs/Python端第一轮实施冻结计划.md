# Python 端第一轮实施冻结计划（MinIO + 契约 + Mock Pipeline 版）

本文档为 Python 端第一轮实施冻结计划。除明确标注为“兼容策略”或“后续阶段”的内容外，其余内容默认按本计划执行，不再反复讨论。

本轮开发优先级：工程闭环 > 契约稳定 > 可联调 > 可测试 > 可扩展；暂不追求真实模型效果。

## 本轮范围

- Python 工程骨架规范化
- MinIO 接入
- Java 接口契约兼容
- Mock 推理闭环
- 必要的 FastAPI 补齐
- 测试与 Docker 联调
- 阶段文档编写与更新

## 本轮明确排除项

- 真实模型训练
- 数据集构建与 Dataset Card 落地
- 模型注册表与审批平台完整实现
- GPU 训练优化
- 多模型切换与灰度发布
- Java 原有环境、源码、Profile、依赖改造

真实模型接入的前置条件：脱敏数据快照完成、标注规范稳定、模型权重可用、GPU 环境就绪、离线评估指标可复现。

## 禁止事项

- 禁止直接连接业务 MySQL。
- 禁止使用共享目录作为正式影像读取路径。
- 禁止把 MinIO 凭证硬编码进源码。
- 禁止在 FastAPI 路由层编排核心业务逻辑。
- 禁止未通过 schema 定义的自由字段回调。
- 禁止在当前推理镜像中一次性引入完整训练栈依赖。
- 禁止删除当前 HMAC 回调兼容。
- 禁止把 mock 输出描述成真实 AI 诊断结果。

## 阶段 1：Python 工程骨架规范化

### 目标

把当前单文件 RabbitMQ worker 整理成可维护的 Python 服务结构，并保持现有 Docker 运行链路不破坏。

### 工作内容

- 建立一级目录结构：`app/core`、`app/infra`、`app/schemas`、`app/services`、`app/pipelines`、`app/api`。
- `main.py` 仅作为应用入口，负责启动 MQ worker、FastAPI 应用或统一生命周期初始化，不允许在 `main.py` 中堆积业务逻辑。
- 当前 RabbitMQ 消费、回调签名、配置读取、mock 推理逻辑拆分到对应模块。
- 本轮至少保留 `requirements.txt` 单入口；如 AI 工具自动拆分 `requirements/base.txt` 与 `requirements/service.txt`，必须保证 `requirements.txt` 仍可直接安装全部运行依赖。
- 推理服务 Docker 镜像调整为 Python 3.11。
- 统一 logger，禁止 `print`；每条任务日志必须带 `taskNo` 或 `traceId`。
- 阶段 1 完成后，`app/core`、`app/infra`、`app/schemas`、`app/services`、`app/pipelines`、`app/api` 的一级结构冻结，后续新增代码只能在既有结构内扩展，不再随意调整包路径。

### 文档更新

- 更新 `backend-python/README.md`：目录结构、启动方式、环境变量、Docker 运行说明。
- 新增或更新 `backend-python/docs/Python端第一轮实施记录.md`，记录阶段 1 改动模块、影响范围、测试结果、已知问题、回滚方式。

### 验收标准

- `docker compose build backend-python` 通过。
- `docker compose up backend-python` 后服务能正常启动。
- 当前 RabbitMQ 消费与 Java 回调链路不因拆分失效。
- 代码中无新增 `print`。
- 核心日志包含 `taskNo` 或 `traceId`。

## 阶段 2：MinIO 接入与影像读取改造

### 目标

实现文档要求的 Python MinIO 能力。MinIO 为 Python 端唯一正式对象存储后端，共享目录、本地文件系统路径仅允许用于临时测试，不得作为正式运行口径。

### 工作内容

- 新增 `app/infra/storage/minio_client.py`，实现 `download_bytes`、`download_to_file`、`upload_file`、`upload_bytes`、`ensure_bucket`。
- `ensure_bucket` 仅允许在 dev/test 环境使用；prod 环境下 bucket 必须由部署脚本或运维预创建，Python 服务启动时不得自动创建生产 bucket。
- 新增 `ImageFetchService`：
  - 优先使用 `bucketName + objectKey` 从 `caries-image` 下载原图。
  - `accessUrl` 仅作为过渡兼容路径。本轮所有新逻辑必须优先使用 `bucketName + objectKey`，不允许新增功能继续依赖 `accessUrl`。
  - 原图只读，不由 Python 改写。
- 新增 `VisualAssetService`：
  - 统一上传到 `caries-visual`。
  - 返回完整 metadata：`bucketName`、`objectKey`、`fileName`、`contentType`、`fileSizeBytes`。
- visual objectKey 固定规则：
  - `org/{orgId}/case/{caseNo}/analysis/{taskNo}/{modelVersion}/{assetTypeCode}/{relatedImageId}/{toothCode}/{attachmentId}.{ext}`
  - `assetTypeCode` 使用 `MASK`、`OVERLAY`、`HEATMAP` 等枚举
  - `caries-visual` 默认 30 天自动清理
- contentType 固定规则：
  - PNG：`image/png`
  - JPG/JPEG：`image/jpeg`
  - JSON：`application/json`
- 所有下载文件和中间可视化文件必须写入临时目录，任务完成后必须清理；不得在容器内保留永久影像副本。
- 环境变量一次冻结：
  - `CG_MINIO_ENDPOINT`
  - `CG_MINIO_ACCESS_KEY`
  - `CG_MINIO_SECRET_KEY`
  - `CG_MINIO_SECURE`
  - `CG_BUCKET_IMAGE`
  - `CG_BUCKET_VISUAL`
  - `CG_BUCKET_REPORT`
  - `CG_BUCKET_EXPORT`
  - `CG_MINIO_REGION`
  - `CG_MINIO_CONNECT_TIMEOUT_SECONDS`
  - `CG_MINIO_READ_TIMEOUT_SECONDS`
  - `CG_TEMP_DIR`

### 文档更新

- 更新 `backend-python/README.md` 的 MinIO 配置、bucket 规则、objectKey 规则。
- 更新实施记录，写明 `accessUrl` 兼容路径的退出条件。
- 如 `DOCKER.md` 与 Python 实际运行不一致，仅更新 Python 运行说明，不改 Java 配置。

### 验收标准

- Docker 网络内 Python 能读 `caries-image`。
- Python 能写 `caries-visual`。
- 集成测试优先使用本地 MinIO；如测试环境受限，可使用 mock 作为补充，不得以 mock 代替 MinIO 主链路验证。
- 任务结束后临时文件被清理。
- 上传 visual asset 后 metadata 完整。

## 阶段 3：接口契约与回调 Payload 对齐

### 目标

让 Python 输入、输出、错误码、字段命名与 Java ↔ Python 契约对齐，同时保留当前 Java HMAC 回调兼容。

### 本轮最小必需请求字段

- `traceId`
- `taskNo`
- `caseId`
- `caseNo`
- `patientId`，如当前 Java 已提供则接收
- `orgId`，如当前 Java 已提供则接收
- `images[].imageId`
- `images[].imageTypeCode`
- `images[].bucketName`
- `images[].objectKey`
- `callbackUrl`
- `modelVersion`
- `requestedAt`，如无则允许透传空

### 本轮最小必需回调字段

- `taskNo`
- `taskStatusCode`
- `modelVersion`
- `visualAssets`
- `riskAssessment`
- `completedAt`
- `traceId`

### 工作内容

- 新增 Pydantic schema：`AnalyzeRequest`、`ImageInput`、`PatientProfile`、`AnalysisCallbackPayload`、`FailureCallbackPayload`、`QualityCheckResult`、`RiskAssessment`、`VisualAsset`。
- 对外字段命名转换必须通过 Pydantic schema alias 实现，不允许手写 dict 键名拼装作为主要实现方式。
- 对外 JSON 一律 camelCase，对内 Python 代码可用 snake_case。
- 当前正式兼容：`X-AI-Timestamp + X-AI-Signature`。
- 目标态预留：`X-Callback-Token`。
- 本轮不得移除 HMAC 兼容。
- 如 Java 未升级 token 校验，Python 只保留接口扩展位，不主动切换生产签名方式。
- 错误码按契约实现：`00000`、`A0400`、`A0404`、`B0003`、`C3001`、`C3002`、`C3003`、`C9999`。
- HTTP 状态码除框架崩溃外统一返回 200，业务成功失败通过 `code` 字段区分。

### 文档更新

- 更新实施记录，说明当前 HMAC 兼容与目标态 callback token 的差异。
- 更新 README，列出当前支持的请求字段、回调字段、错误码。
- 固化成功/失败回调 JSON 示例到 `docs/Python端联调样例.md`。

### 验收标准

- 契约测试通过。
- 成功回调、失败回调、质量拒收回调都有样例和测试。
- 任一对外 JSON 样例中不得出现 snake_case 字段名。
- 当前 Java callback 不因 schema 改造失效。

## 阶段 4：Mock 推理 Pipeline 与可视化产物

### 目标

在没有真实模型和正式数据集前，先跑通完整工程闭环：取图、mock 推理、生成可视化、上传 MinIO、回调 Java。

### 工作内容

- 实现 `InferencePipeline`，包含质量检查、影像预处理、mock tooth detection、mock lesion result、mock risk fusion、mock explain/overlay。
- 本轮 mock pipeline 统一输出以下对象结构：
  - `qualityCheckResults[]`
  - `toothDetections[]`
  - `lesionResults[]`
  - `riskAssessment`
  - `visualAssets[]`
  - `rawResultJson`
- 不允许单独返回 `overlayUrl`。
- 不允许用自由字段替代 `visualAssets[]`。
- 高级字段可置空，但字段名必须稳定。
- mock 数据规则：
  - `toothCode` 使用固定样例值，如 `16`、`26`。
  - `severityCode` 仅使用 `C0`、`C1`、`C2`、`C3`。
  - `riskLevelCode` 仅使用 `LOW`、`MEDIUM`、`HIGH`。
  - `reviewRecommendedFlag`、`highRiskFlag` 使用 `"0"` / `"1"`，不用 boolean。
- overlay/mask 使用 Pillow 或 OpenCV 生成稳定 PNG。
- 同一输入任务在 mock 模式下应尽量输出可重复结果，避免完全随机，便于联调与测试断言。
- 当前可视化产物仅用于联调、演示和对象存储链路验证，不代表真实临床诊断结果。

### 文档更新

- 更新 README，明确当前是 mock inference。
- 更新实施记录，说明 mock 输出字段、真实模型接入点、产物命名规则。
- 更新 `docs/Python端联调样例.md`，加入 RabbitMQ message 示例、成功回调示例、失败回调示例。

### 验收标准

- 一条分析任务能完成：消费消息 -> 取图 -> 生成 overlay/mask -> 上传 MinIO -> 回调 Java。
- MinIO `caries-visual` 中能看到产物。
- 回调体字段稳定，不出现自由字段替代契约字段。
- mock 输出可被测试断言。

## 阶段 5：FastAPI 服务补齐

### 目标

阶段 5 作为本轮补齐项，保留在 MQ worker 稳定后执行；本轮主链路仍以 RabbitMQ worker 为主。

### 工作内容

- 补齐 HTTP 接口：
  - `GET /ai/v1/health`
  - `HEAD /ai/v1/health`
  - `POST /ai/v1/quality-check`
  - `POST /ai/v1/analyze`
  - `POST /ai/v1/assess-risk`
  - `GET /ai/v1/model-version`
- FastAPI 路由层只负责参数接收、响应包装和调用 pipeline/service，不允许在路由函数中直接编排复杂业务逻辑。
- `POST /ai/v1/analyze` 仅返回“已受理”，实际分析通过 background task 或内部异步执行，不在请求链路中等待完整推理完成。
- MQ 消费入口与 HTTP analyze 入口必须共用同一套 pipeline/service，不允许维护两套独立业务实现。
- health 接口返回：
  - 服务状态
  - 当前模式，`mock` / `real`
  - model registry 简要状态
  - MinIO / MQ 可用性摘要，可选

### 文档更新

- README 增加 HTTP API 说明。
- 实施记录写明 FastAPI 与 MQ 双入口关系。
- 本轮不修改 Java 配置，仅更新 Python 运行说明。

### 验收标准

- Docker 内访问 `/ai/v1/health` 返回标准响应。
- `quality-check`、`assess-risk` 可直接请求测试。
- MQ 和 HTTP 两种入口共用同一 pipeline。
- 路由函数中不出现复杂业务编排。

## 阶段 6：测试、容器联调、回归记录

### 目标

锁住阶段成果，形成可重复验证的 Docker 联调与回归记录。

### 工作内容

- 单元测试覆盖：
  - config
  - schema alias
  - 错误码包装
  - 临时文件清理
  - visual metadata 结构
  - HMAC 签名生成
  - MinIO objectKey 生成
  - callback payload 构造
- 集成测试覆盖：
  - MinIO 上传/下载主链路
  - mock Java callback
  - pipeline happy path
- 契约测试覆盖：
  - 成功回调字段
  - 失败回调字段
  - 无 snake_case 外泄
- Docker 验证顺序：
  - `docker compose build backend-python`
  - `docker compose up -d rabbitmq minio backend-python`
  - 发送 mock 消息
  - 检查 MinIO `caries-visual`
  - 检查 Java callback 日志

### 文档更新

- 每轮完成后，在实施记录中登记：改动模块、影响范围、测试结果、已知问题、回滚方式。
- 至少固化 1 条成功回调 JSON 和 1 条失败回调 JSON 到 `docs/Python端联调样例.md`。
- README 增加常用验证命令。

### 验收标准

- Python 单元测试通过。
- 契约测试通过。
- Docker build 通过。
- 至少一条 mock 分析链路跑通。
- 文档记录包含测试结果、已知问题、回滚方式。

## 本轮执行顺序

第一轮交付：阶段 1 -> 阶段 2 -> 阶段 3 -> 阶段 4。

第二轮补齐：阶段 5 -> 阶段 6。

第一轮完成后，Python 端应达到：目录结构冻结、MinIO 主链路可用、契约字段稳定、mock pipeline 可闭环、Docker 内可联调、阶段文档完整。
