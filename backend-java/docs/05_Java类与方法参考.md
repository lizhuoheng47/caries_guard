# Java类与方法参考

更新日期：2026-04-15

## 1. 系统模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `AuthController` | `login`、`currentUser`、`currentPermissions` | 认证与当前用户 |
| `AuthAppService` | `login`、`currentUser`、`currentPermissions` | 认证逻辑 |
| `SystemAdminController` | 用户、角色、菜单、数据权限规则管理方法 | 后台系统管理 API |
| `SystemDataPermissionRuleAppService` | `listRules`、`createRule`、`updateRule` | 数据权限规则 |
| `SystemMenuCommandAppService` | 菜单创建/修改 | 菜单命令服务 |
| `SystemRoleCommandAppService` | 角色创建/修改 | 角色命令服务 |
| `SystemUserCommandAppService` | 用户创建/修改 | 用户命令服务 |

## 2. 患者病例模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `PatientController` | `createPatient`、`updatePatient`、`getPatient`、`pagePatients` | 患者 API |
| `VisitController` | `createVisit`、`getVisit`、`pageVisits` | 就诊 API |
| `CaseController` | `createCase`、`getCase`、`pageCases`、`saveDiagnoses`、`saveToothRecords`、`transitionStatus` | 病例 API |
| `PatientCommandAppService` | 患者写操作 | 写 `pat_patient` 等 |
| `VisitCommandAppService` | 就诊写操作 | 写 `med_visit` |
| `CaseCommandAppService` | 病例写操作 | 写 `med_case`、诊断、牙位、状态日志 |
| `CaseQueryAppService` | 病例查询 | 聚合病例详情 |

## 3. 影像模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `FileController` | `upload` | 上传附件 |
| `FileController` | `accessUrl` | 返回短时访问 URL |
| `FileController` | `content` | 校验签名并返回文件内容 |
| `CaseImageController` | `create`、`list`、`detail` | 病例影像 API |
| `ImageQualityCheckController` | `save`、`getCurrent` | 影像质检 API |
| `AttachmentAppService` | `upload` | MD5 去重、对象存储、附件落库 |
| `AttachmentAppService` | `createAccessUrl(Long, HttpServletRequest)` | 生成 MinIO 预签名 GET URL |
| `AttachmentAppService` | `createAccessUrl(Long, String)` | 兼容旧签名，当前返回 MinIO 预签名 GET URL |
| `AttachmentAppService` | `createInternalAccessUrl` | 内部 AI 服务访问 URL |
| `AttachmentAppService` | `registerExternalObject` | 登记 Python 已写入 MinIO 的对象元数据 |
| `AttachmentAppService` | `loadContent` | 签名下载 |
| `ObjectStorageService` | `store`、`load`、`delete` | 存储抽象 |
| `StorageProperties` | `provider`、`endpoint`、`buckets`、`defaultPresignExpireSeconds` | `caries.storage` 配置 |
| `MinioObjectStorageClient` | `upload`、`download`、`delete`、`presignGetObject`、`presignPutObject` | MinIO 基础设施实现 |
| `ImageObjectStorageServiceAdapter` | `store`、`load`、`presignGetObject` | 业务端口到 MinIO 客户端的适配器 | 存储配置 |

## 4. AI 分析模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `AnalysisTaskController` | `createAnalysisTask`、`retryAnalysisTask`、`getAnalysisTaskDetail`、`pageAnalysisTasks` | 分析任务 API |
| `CaseAnalysisAliasController` | `createAnalysisFromCase`、`submitCorrectionFromCase` | 病例别名入口 |
| `AnalysisCallbackController` | `receiveAnalysisResultCallback` | Python 回调入口 |
| `CorrectionFeedbackController` | `submitCorrectionFeedback` | 修正反馈 API |
| `AnalysisTaskAppService` | `createTask`、`retryTask` | 创建/重试分析任务、构造 `AiAnalysisRequestDTO` |
| `AnalysisCallbackAppService` | `handleCallback` | 幂等处理和事务编排 |
| `AnalysisCallbackDomainService` | `applyCallback` | 写 summary、visualAssets、riskAssessment |
| `CorrectionFeedbackAppService` | `submit` | 写修正反馈和训练准入字段 |
| `RabbitAnalysisTaskEventPublisher` | `publish` | 发布分析任务消息 |

关键 DTO/Command：

| 类 | 字段重点 |
| --- | --- |
| `AiAnalysisRequestDTO` | `taskNo`、`modelVersion`、`images`、`patientProfile` |
| `AiAnalysisRequestDTO.ImageItem` | `attachmentId`、`storageProviderCode`、`attachmentMd5`、`accessUrl`、`accessExpireAt`、`localStoragePath` |
| `AiAnalysisResultCallbackCommand` | `taskNo`、`taskStatusCode`、`modelVersion`、`summary`、`rawResultJson`、`visualAssets`、`riskAssessment`、`traceId`、`inferenceMillis` |
| `AiVisualAssetDTO` | `assetTypeCode`、`attachmentId` 或 `bucketName/objectKey`、`relatedImageId`、`toothCode` |
| `RiskAssessmentDTO` | 风险等级和建议 |
| `SubmitCorrectionFeedbackCommand` | 医生修正反馈 |

## 5. 报告模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `ReportController` | `generateReport`、`listCaseReports`、`getReport`、`exportReport` | 报告 API |
| `ReportTemplateController` | `createTemplate`、`updateTemplate`、`listTemplates`、`getTemplate` | 模板 API |
| `ReportAppService` | `generateReport` | 聚合病例、分析、风险、模板并生成 PDF 附件 |
| `ReportAppService` | `exportReport` | 复制到 `caries-export`、写导出审计并返回下载 URL |
| `ReportPdfService` | `generatePdf` | PDFBox 生成 PDF，支持中文字体候选 |
| `ReportExportResultVO` | `reportId`、`exported`、`exportLogId`、`attachmentId`、`downloadUrl`、`expireAt` | 导出返回对象 |

## 6. 随访模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `FollowupPlanController` | `createPlan`、`listCasePlans`、`getPlan`、`cancelPlan`、`closePlan` | 随访计划 API |
| `FollowupTaskController` | `createTask`、`listCaseTasks`、`getTask`、`updateTaskStatus`、`assignTask` | 随访任务 API |
| `FollowupRecordController` | `addRecord`、`listCaseRecords`、`listTaskRecords` | 随访记录 API |
| `FollowupPlanAppService` | 计划写入/关闭/取消 | 写 `fup_plan` |
| `FollowupTaskAppService` | 任务写入/状态/分派 | 写 `fup_task` |
| `FollowupRecordAppService` | 记录写入/查询 | 写 `fup_record` |

## 7. 看板模块

| 类 | 方法 | 说明 |
| --- | --- | --- |
| `DashboardController` | `getOverview`、`getCaseStatusDistribution`、`getRiskLevelDistribution`、`getFollowupTaskSummary`、`getBacklogSummary`、`getTrend` | 业务看板 |
| `DashboardOpsController` | `getModelRuntime` | AI 运行质量看板 |
| `DashboardStatsRepository` | `queryModelRuntime` | 查询模型版本、成功率、耗时、不确定性、复核建议、修正反馈 |
| `ModelRuntimeVO` | 多个运行质量字段 | 模型运行汇总 |
| `ModelVersionRuntimeVO` | 按版本统计字段 | 模型版本聚合 |

## 8. 当前边界类名说明

1. 当前无 `RiskController` 或独立 `caries-risk`。
2. 当前无独立 `ModelAdminController` 或 `caries-model-admin`。
3. 模型治理最小落点是 `ana_model_version_registry` 和 dashboard `model-runtime`。
4. 风险评估落点是 `med_risk_assessment_record`。