# Java后端命名规范与AI协同开发约定

更新日期：2026-04-15

本文档只保留当前项目真实使用的命名和 Java/Python 协作契约。

## 1. 模块命名

| 模块 | 命名含义 |
| --- | --- |
| `caries-system` | 系统、认证、用户、角色、菜单、权限 |
| `caries-patient` | 患者、就诊、病例、诊断、牙位 |
| `caries-image` | 附件、对象存储、病例影像、影像质检 |
| `caries-analysis` | AI 分析任务、回调、结果、风险、修正反馈 |
| `caries-report` | 模板、报告、PDF、导出 |
| `caries-followup` | 随访计划、任务、记录 |
| `caries-dashboard` | 业务看板、模型运行看板 |
| `caries-framework` | 权限注解、安全上下文、日志、Trace |
| `caries-common` | 通用响应、异常、分页、工具 |

禁止把未实现的独立模块写成当前模块：

1. 不写 `caries-risk` 为已实现模块。
2. 不写 `caries-model-admin` 为已实现模块。
3. 不写独立标注平台或训练平台为当前 Java 代码模块。

## 2. 包命名

| 包名 | 用途 |
| --- | --- |
| `controller` | REST Controller |
| `app` | 应用服务 |
| `domain.model` | 领域模型、查询模型、状态更新模型 |
| `domain.repository` | 仓储接口 |
| `domain.service` | 领域服务 |
| `infrastructure.repository` | 仓储实现 |
| `infrastructure.storage` | 对象存储实现 |
| `infrastructure.service` | PDF 等技术服务实现 |
| `interfaces.command` | 请求命令对象 |
| `interfaces.dto` | 跨服务 DTO 和嵌套 DTO |
| `interfaces.vo` | API 响应 VO |

## 3. 类命名

| 后缀 | 例子 | 说明 |
| --- | --- | --- |
| `Controller` | `AnalysisTaskController` | REST API 入口 |
| `AppService` | `AnalysisTaskAppService` | 应用服务，处理事务和流程编排 |
| `DomainService` | `AnalysisCallbackDomainService` | 领域规则处理 |
| `Repository` | `AnaTaskRecordRepository` | 仓储接口 |
| `RepositoryImpl` | `AnaTaskRecordRepositoryImpl` | 仓储实现 |
| `Command` | `AiAnalysisResultCallbackCommand` | 请求入参或回调命令 |
| `DTO` | `AiAnalysisRequestDTO` | 跨服务数据载荷 |
| `VO` | `ModelRuntimeVO` | API 出参 |
| `DO` | `AnaTaskRecordDO` | 数据库映射对象 |
| `Properties` | `StorageProperties` | 配置属性 |
| `Service` | `ReportPdfService` | 技术服务或领域服务 |

当前重要命名：

| 类名 | 状态 | 说明 |
| --- | --- | --- |
| `AiAnalysisRequestDTO` | 已实现 | Java 发给 Python 的任务载荷 |
| `AiAnalysisResultCallbackCommand` | 已实现 | Python 回调 Java 的冻结契约 |
| `ObjectStorageService` | 已实现 | 对象存储抽象 |
| `MinioObjectStorageClient` | 已实现 | 默认 MinIO provider |
| `ImageObjectStorageServiceAdapter` | 已实现 | 本地文件兼容 provider |
| `ReportExportResultVO` | 已实现 | 报告导出返回下载信息 |
| `ModelRuntimeVO` | 已实现 | AI 运行质量看板响应 |
| `ModelVersionRuntimeVO` | 已实现 | 按模型版本聚合响应 |

## 4. 对象存储命名

Provider code 必须使用明确枚举式字符串：

| provider code | 实现类 | 使用场景 |
| --- | --- | --- |
| `MINIO` | `MinioObjectStorageClient` | local 默认、正式联调、需要对象存储服务的环境 |
| `LOCAL_FS` | 不作为当前运行口径 | 历史/测试口径，不建议新增依赖 |
| `OSS` | 未实现 | 仅作为未来扩展命名，不写成当前能力 |

命名约定：

1. 数据库字段写 `storage_provider_code`。
2. Java 字段写 `storageProviderCode`。
3. DTO 中仍保留 `bucketName`、`objectKey`，但 Python 优先使用 `accessUrl`。
4. `localStoragePath` 只允许在 `LOCAL_FS` 受控环境下有值。

## 5. AI 协同命名

Java -> Python：`AiAnalysisRequestDTO`

| 字段 | 命名约定 |
| --- | --- |
| `taskNo` | 任务编号，跨系统幂等主键 |
| `taskTypeCode` | 任务类型代码 |
| `modelVersion` | 请求模型版本 |
| `images[].storageProviderCode` | 存储 provider |
| `images[].attachmentMd5` | 文件校验值 |
| `images[].accessUrl` | 推荐拉图入口 |
| `images[].accessExpireAt` | URL 过期时间 |
| `images[].localStoragePath` | LOCAL_FS 兼容路径 |

Python -> Java：`AiAnalysisResultCallbackCommand`

| 字段 | 命名约定 |
| --- | --- |
| `taskNo` | 必须原样回传 |
| `taskStatusCode` | 使用大写状态码 |
| `startedAt`、`completedAt` | ISO 时间 |
| `modelVersion` | 实际推理模型版本 |
| `summary.overallHighestSeverity` | 最高病变严重程度 |
| `summary.uncertaintyScore` | 摘要不确定性 |
| `summary.reviewSuggestedFlag` | `0/1` |
| `summary.teethCount` | 识别牙齿数量 |
| `rawResultJson` | 原始结果，不做业务字段散落 |
| `visualAssets` | 可视化资产列表 |
| `riskAssessment` | 风险评估 |
| `traceId` | Python 链路追踪 ID |
| `inferenceMillis` | 推理耗时 |

## 6. 数据库命名

| 领域 | 前缀 | 例子 |
| --- | --- | --- |
| 系统 | `sys_` | `sys_user`、`sys_role`、`sys_menu` |
| 患者 | `pat_` | `pat_patient`、`pat_guardian` |
| 医疗病例 | `med_` | `med_case`、`med_image_file` |
| AI 分析 | `ana_` | `ana_task_record`、`ana_result_summary` |
| 报告 | `rpt_` | `rpt_record`、`rpt_export_log` |
| 随访 | `fup_` | `fup_plan`、`fup_task`、`fup_record` |
| 消息通知 | `msg_` | `msg_notify_record` |

数据库基线包含的新增/增强命名：

| 名称 | 说明 |
| --- | --- |
| `ana_model_version_registry` | 模型版本登记表 |
| `ana_task_record.trace_id` | AI 服务 trace ID |
| `ana_task_record.inference_millis` | 推理耗时 |
| `ana_correction_feedback.training_candidate_flag` | 是否进入训练候选 |
| `ana_correction_feedback.desensitized_export_flag` | 是否脱敏导出 |
| `ana_correction_feedback.dataset_snapshot_no` | 数据集快照编号 |
| `ana_correction_feedback.review_status_code` | 训练准入审核状态 |
| `ana_correction_feedback.reviewed_by` | 审核人 |
| `ana_correction_feedback.reviewed_at` | 审核时间 |

## 7. API 命名

REST 约定：

1. 业务 API 使用 `/api/v1/...`。
2. 内部 AI 回调使用 `/api/v1/internal/ai/callbacks/...`。
3. 文件访问主路径使用 `/api/v1/files/{attachmentId}/access-url` 获取 MinIO presigned URL；`/api/v1/files/{attachmentId}/content` 仅作为受控代理兜底入口保留，必须带 `expireAt` 和 `signature`。
4. 模型运行看板使用 `/api/v1/dashboard/model-runtime`。
5. 操作型接口使用 POST，查询型接口使用 GET。
6. 批量分页查询返回 `PageResultVO`。

## 8. 文档用语约定

正确表述：

1. “当前默认对象存储为 MinIO，`LOCAL_FS` 为兼容实现。”
2. “ModelAdmin 治理原则保留，但当前未落地为独立业务模块。”
3. “风险评估是能力，不是独立模块。”
4. “报告导出包含审计和下载 URL。”
5. “数据库基线已补业务角色、菜单和数据权限规则种子。”

禁止表述：

1. 不把本地文件兼容实现写成唯一对象存储实现。
2. 不把历史回调对象写成当前回调契约。
3. “报告导出只写日志。”
4. “sys_menu 为空，无法演示非管理员角色。”
5. “已经完成完整 ModelAdmin 平台。”
