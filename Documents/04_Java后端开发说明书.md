# Java后端开发说明书

更新日期：2026-04-15

本文档用于 Java 后端维护开发和 Python 服务联调。内容以当前代码、Flyway V017 和已通过编译测试的实现为准。

## 1. 开发基线

| 项 | 当前值 |
| --- | --- |
| 工作目录 | `backend-java` |
| 启动模块 | `caries-boot` |
| Spring profile | `local`、`e2e` |
| 数据库迁移 | Flyway V001-V017 |
| 默认对象存储 | `MINIO` |
| 默认对象存储实现 | `MinioObjectStorageClient` |
| 本地兼容对象存储 | 不作为当前运行口径 |
| AI 回调 DTO | `AiAnalysisResultCallbackCommand` |
| 报告 PDF 生成 | `ReportPdfService` + PDFBox |
| 报告导出结果 | 审计日志 + `attachmentId` + `downloadUrl` + `expireAt` |

## 2. 常用命令

```powershell
cd E:\caries_guard\backend-java
mvn -q -DskipTests compile
mvn -q test
mvn -q -pl caries-analysis -am test
```

`application-local.yml` 默认连接 MinIO：

```yaml
caries:
  storage:
    provider: ${CARIES_STORAGE_PROVIDER:MINIO}
    endpoint: ${CARIES_MINIO_ENDPOINT:http://127.0.0.1:9000}
    access-key: ${CARIES_MINIO_ACCESS_KEY:minioadmin}
    secret-key: ${CARIES_MINIO_SECRET_KEY:minioadmin}
    secure: ${CARIES_MINIO_SECURE:false}
    default-presign-expire-seconds: ${CARIES_STORAGE_PRESIGN_EXPIRE_SECONDS:900}
    auto-create-buckets: ${CARIES_STORAGE_AUTO_CREATE_BUCKETS:true}
    proxy-access-secret: ${CARIES_IMAGE_ACCESS_SECRET:change-me-to-a-strong-image-access-secret}
    buckets:
      image: ${CARIES_BUCKET_IMAGE:caries-image}
      visual: ${CARIES_BUCKET_VISUAL:caries-visual}
      report: ${CARIES_BUCKET_REPORT:caries-report}
      export: ${CARIES_BUCKET_EXPORT:caries-export}
```

`application-e2e.yml` 使用 `LOCAL_FS`，避免测试必须启动外部 MinIO。

## 3. 代码分层约定

当前主要分层：

| 层 | 典型包名 | 说明 |
| --- | --- | --- |
| Controller | `controller` | REST API 入口，处理权限注解和请求响应 |
| AppService | `app` | 应用服务，组织事务、调用领域服务和仓储 |
| Domain Service | `domain.service` | 领域规则和业务处理 |
| Domain Model | `domain.model` | 领域模型/查询模型/命令模型 |
| Repository Interface | `domain.repository` | 仓储接口 |
| Repository Impl | `infrastructure.repository` | MyBatis-Plus / SQL 实现 |
| Storage/Service Impl | `infrastructure.storage`、`infrastructure.service` | MinIO、本地存储、PDF 生成等技术实现 |
| Command/DTO/VO | `interfaces.command`、`interfaces.dto`、`interfaces.vo` | 接口入参、跨服务 DTO、出参 |

## 4. 关键模块开发说明

### 4.1 `caries-image`

关键类：

| 类名 | 方法 | 说明 |
| --- | --- | --- |
| `FileController` | `upload` | 上传附件 |
| `FileController` | `accessUrl` | 生成短时访问 URL |
| `FileController` | `content` | 受控代理兜底入口，校验签名后返回文件内容 |
| `AttachmentAppService` | `upload` | 计算 MD5、调用对象存储、写 `med_attachment` |
| `AttachmentAppService` | `createAccessUrl` | 面向前端生成 URL |
| `AttachmentAppService` | `createInternalAccessUrl` | 面向 AI 内部服务生成 URL |
| `AttachmentAppService` | `resolveLocalStoragePath` | LOCAL_FS 场景解析路径 |
| `ObjectStorageService` | `store`、`load`、`delete` | 对象存储统一接口 |
| `MinioObjectStorageClient` | `store`、`load`、`delete` | MinIO 实现，默认启用 |
| `ImageObjectStorageServiceAdapter` | `store`、`load`、`delete` | 本地文件兼容实现 |

开发注意：

1. 新增存储 provider 时必须实现 `ObjectStorageService`。
2. `med_attachment.storage_provider_code` 必须写入真实 provider：`MINIO` 或 `LOCAL_FS`。
3. Python 不应依赖 `bucketName + objectKey` 自行猜测文件路径，优先使用 `accessUrl`。
4. `localStoragePath` 只允许 LOCAL_FS 受控环境使用。

### 4.2 `caries-analysis`

关键类：

| 类名 | 方法 | 说明 |
| --- | --- | --- |
| `AnalysisTaskController` | `createAnalysisTask`、`retryAnalysisTask`、`getAnalysisTaskDetail`、`pageAnalysisTasks` | 分析任务 API |
| `AnalysisTaskAppService` | `createTask`、`retryTask` | 创建任务、构造 `AiAnalysisRequestDTO`、发布事件 |
| `RabbitAnalysisTaskEventPublisher` | `publish` | 投递分析任务消息 |
| `AnalysisCallbackController` | `receiveAnalysisResultCallback` | Python 回调入口 |
| `AnalysisCallbackAppService` | `handleCallback` | 回调幂等、状态更新、结果落库 |
| `AnalysisCallbackDomainService` | `applyCallback` | 处理 summary、visualAssets、riskAssessment |
| `CorrectionFeedbackAppService` | `submit` | 医生修正反馈，写训练准入字段 |

`AiAnalysisRequestDTO.ImageItem` 字段：

| 字段 | 说明 |
| --- | --- |
| `imageId` | 影像记录 ID |
| `attachmentId` | 附件 ID |
| `imageTypeCode` | 影像类型 |
| `bucketName` | 对象 bucket |
| `objectKey` | 对象 key |
| `storageProviderCode` | `MINIO` 或 `LOCAL_FS` |
| `attachmentMd5` | 附件 MD5 |
| `accessUrl` | 短时 HTTP 访问 URL |
| `accessExpireAt` | URL 过期时间 |
| `localStoragePath` | LOCAL_FS 受控环境路径 |

`AiAnalysisResultCallbackCommand` 字段：

| 字段 | 说明 |
| --- | --- |
| `taskNo` | 必填，任务编号 |
| `taskStatusCode` | 必填，任务状态 |
| `startedAt` | 推理开始时间 |
| `completedAt` | 推理完成时间 |
| `modelVersion` | 实际执行模型版本 |
| `summary` | 摘要聚合 |
| `rawResultJson` | 原始结果 |
| `visualAssets` | 视觉资产列表 |
| `riskAssessment` | 风险评估 |
| `errorMessage` | 错误信息 |
| `traceId` | Python 链路 ID |
| `inferenceMillis` | 推理耗时 |
| `uncertaintyScore` | 顶层不确定性分 |

幂等要求：

1. 同一终态回调重复到达时只 ACK，不重复写 summary、risk、visual assets。
2. 旧任务重试后的晚到回调不能覆盖新任务链路。
3. `PROCESSING -> SUCCESS` 要保留时间链路。
4. `FAILED` 时病例应回退到 `QC_PENDING`。

当前相关测试已经覆盖 analysis 模块主链路，后续新增字段时必须同步测试。

### 4.3 `caries-report`

关键类：

| 类名 | 方法 | 说明 |
| --- | --- | --- |
| `ReportController` | `generateReport` | 生成报告 |
| `ReportController` | `listCaseReports` | 查询病例报告列表 |
| `ReportController` | `getReport` | 查询报告详情 |
| `ReportController` | `exportReport` | 导出审计 + 返回下载 URL |
| `ReportAppService` | `generateReport` | 聚合数据、渲染模板、生成 PDF 附件 |
| `ReportAppService` | `exportReport` | 写 `rpt_export_log`，返回 `ReportExportResultVO` |
| `ReportPdfService` | `generatePdf` | PDFBox 生成 PDF |
| `ReportTemplateAppService` | `createTemplate`、`updateTemplate`、`listTemplates`、`getTemplate` | 模板管理 |

`ReportExportResultVO` 字段：`reportId`、`exported`、`exportLogId`、`attachmentId`、`downloadUrl`、`expireAt`。

PDF 注意：

1. 当前 PDF 生成器不再是 ASCII-only 的极简生成器。
2. 运行环境存在中文字体时可正常编码中文。
3. 若 Windows 或 Linux 环境没有候选中文字体，会 fallback 到 Helvetica，不可编码字符会变成 `?`。
4. 赛前正式环境应安装 `NotoSansSC`、`SimHei` 或等价中文字体。

### 4.4 `caries-system`

V014 已补：

1. 角色：`ORG_ADMIN`、`DOCTOR`、`SCREENER`。
2. 菜单：患者、就诊、病例、影像、分析任务、报告、随访、看板、AI 运行看板。
3. `sys_role_menu` 默认授权。
4. `sys_data_permission_rule` 默认 ORG/SELF 范围与列脱敏策略。

代码侧仍需要注意：数据权限框架已经有表和管理入口，但业务查询仍以 `org_id` 防线为主。后续若继续增强，应在 `QueryAppService` 或 Repository 查询层进一步下推 `ALL/ORG/DEPT/SELF/CUSTOM`。

### 4.5 `caries-dashboard`

`DashboardOpsController.getModelRuntime` 返回 AI 运行指标：

| 字段 | 说明 |
| --- | --- |
| `currentModelVersion` | 最近模型版本 |
| `recentTaskCount` | 近期任务数 |
| `successTaskCount` | 成功任务数 |
| `failedTaskCount` | 失败任务数 |
| `successRate` | 成功率 |
| `averageInferenceMillis` | 平均推理时长 |
| `highUncertaintyRate` | 高不确定性占比 |
| `reviewSuggestedRate` | 建议复核占比 |
| `correctionFeedbackCount` | 修正反馈数量 |
| `modelVersions` | 按模型版本聚合的任务数、成功率、平均耗时 |

## 5. 数据库开发说明

新增结构在 Flyway 中完成，不直接手工改库。

当前最新迁移：`V017__17_freeze_analysis_data_dictionary_v2.sql`

V014 内容：

1. `ana_task_record` 增加 `trace_id`、`inference_millis`。
2. `ana_correction_feedback` 增加 `training_candidate_flag`、`desensitized_export_flag`、`dataset_snapshot_no`、`review_status_code`、`reviewed_by`、`reviewed_at`。
3. 新增 `ana_model_version_registry`。
4. 初始化模型版本 `caries-detector / caries-v1`。
5. 初始化业务角色、菜单、角色菜单关联和数据权限规则。

V015-V017 内容：

1. V015 为 `ana_visual_asset` 增加 `related_image_id` 和 `tooth_code`。
2. V016 固化 attachment object key 治理。
3. V017 冻结 analysis 数据字典 v2。

## 6. 与 Python 服务联调要求

Python 消费任务消息时应：

1. 优先使用 `images[].accessUrl` 拉取影像。
2. 读取 `images[].accessExpireAt`，过期后不得继续请求。
3. 记录并回传 `modelVersion`、`traceId`、`inferenceMillis`。
4. 回调时严格使用 `AiAnalysisResultCallbackCommand` 对应 JSON 结构。
5. 失败时回传 `taskStatusCode=FAILED` 和 `errorMessage`。
6. 不要把 `bucketName + objectKey` 当成一定可直接访问的本地路径。

## 7. 当前不建议改动的边界

1. 不建议为了图好看拆出 `caries-risk`，风险能力已有表和流程承载。
2. 不建议声称已有独立 ModelAdmin 平台，当前只是最小模型治理落点。
3. 不建议绕过 `AttachmentAppService` 直接拼下载地址。
4. 不建议在业务代码中硬编码 MinIO endpoint，应通过 `StorageProperties` 读取配置。
