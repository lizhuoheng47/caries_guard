# Java类与方法参考

本文档面向需要直接对照 Java 实现的开发者，重点列出当前项目中最关键的类名、方法名和职责。

## 1. 安全与基础设施

### 1.1 `SecurityConfig`

职责：Spring Security 入口。

关键方法：
- `securityFilterChain(HttpSecurity http)`

说明：
- 配置白名单接口
- 指定其他请求都需要认证
- 挂载 `JwtAuthenticationFilter`

### 1.2 `JwtAuthenticationFilter`

职责：从请求头中解析 JWT 并建立认证上下文。

关键方法：
- 构造方法 `JwtAuthenticationFilter(JwtTokenProvider, UserDetailsService)`
- 继承自 `OncePerRequestFilter`，实际过滤逻辑在父类约定的方法中

### 1.3 `RequirePermissionAspect`

职责：处理方法级权限注解。

关键方法：
- `around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission)`

说明：
- 管理员角色可绕过细粒度权限校验
- 其他用户依赖权限服务判断

### 1.4 `SensitiveDataFacade`

职责：统一调用加密、哈希、脱敏。

关键方法：
- `protectName`
- `protectPhone`
- `protectIdCard`
- `protectBirthDate`
- `protectGeneric`

### 1.5 `DefaultCryptoService`

职责：敏感明文加密与解密。

关键方法：
- `init`
- `encrypt`
- `decrypt`

### 1.6 `DefaultHashService`

职责：敏感字段归一化后做 HMAC-SHA256 哈希。

关键方法：
- `hmacSha256`
- `normalizeThenHash`

### 1.7 `DefaultMaskingService`

职责：脱敏显示。

关键方法：
- `maskName`
- `maskPhone`
- `maskIdCard`
- `maskBirthDate`
- `maskGeneric`

## 2. 系统模块

### 2.1 `AuthController`

关键方法：
- `login`
- `currentUser`
- `currentPermissions`

### 2.2 `AuthAppService`

职责：登录和当前登录态查询。

关键方法：
- `login(LoginCommand command, HttpServletRequest request)`
- `currentUser()`
- `currentPermissions()`

### 2.3 `SystemUserAuthRepositoryImpl`

职责：从数据库读取认证用户和角色。

关键方法：
- `findByUsername(String username)`
- `findByUserId(Long userId)`
- `markLoginSuccess(Long userId, LocalDateTime loginTime)`

### 2.4 `SystemSupportController`

关键方法：
- `ping()`

### 2.5 `SystemMetadataController`

关键方法：
- `listDictTypes()`
- `listDictItems(String dictType)`
- `getConfig(String configKey)`

### 2.6 `SystemAdminController`

关键方法：
- `pageUsers`
- `getUser`
- `createUser`
- `updateUser`
- `listRoles`
- `getRole`
- `createRole`
- `updateRole`
- `listMenus`
- `getMenu`
- `createMenu`
- `updateMenu`
- `listDataPermissionRules`
- `createDataPermissionRule`
- `updateDataPermissionRule`

### 2.7 系统模块 AppService

| 类名 | 关键方法 |
| --- | --- |
| `SystemQueryAppService` | `listDictTypes` `listDictItems` `getConfig` |
| `SystemAdminQueryAppService` | `pageUsers` `listRoles` `listMenus` `getUser` `getRole` `getMenu` |
| `SystemUserCommandAppService` | `createUser` `updateUser` |
| `SystemRoleCommandAppService` | `createRole` `updateRole` |
| `SystemMenuCommandAppService` | `createMenu` `updateMenu` |
| `SystemDataPermissionRuleAppService` | `listRules` `createRule` `updateRule` |

## 3. 患者与病例模块

### 3.1 `PatientController`

关键方法：
- `createPatient`
- `updatePatient`
- `getPatient`
- `pagePatients`

### 3.2 `VisitController`

关键方法：
- `createVisit`
- `getVisit`
- `pageVisits`

### 3.3 `CaseController`

关键方法：
- `createCase`
- `getCase`
- `pageCases`
- `saveDiagnoses`
- `saveToothRecords`
- `transitionStatus`

### 3.4 `PatientCommandAppService`

职责：创建与更新患者。

关键方法：
- `createPatient(CreatePatientCommand command)`
- `updatePatient(Long patientId, UpdatePatientCommand command)`

实现关注点：
- 生成患者编号
- 敏感数据保护
- 证件号去重
- 同步维护监护人

### 3.5 `PatientQueryAppService`

关键方法：
- `getPatient(Long patientId)`
- `pagePatients(int pageNo, ...)`

### 3.6 `VisitCommandAppService`

关键方法：
- `createVisit(CreateVisitCommand command)`

### 3.7 `VisitQueryAppService`

关键方法：
- `getVisit(Long visitId)`
- `pageVisits(int pageNo, ...)`

### 3.8 `CaseCommandAppService`

职责：病例创建与状态流转的唯一主入口。

关键方法：
- `createCase(CreateCaseCommand command)`
- `transitionStatus(Long caseId, CaseStatusTransitionCommand command)`
- `transitionStatusAsSystem(Long caseId, Long orgId, CaseStatusTransitionCommand command)`

### 3.9 `CaseQueryAppService`

关键方法：
- `getCase(Long caseId)`
- `pageCases(int pageNo, ...)`

### 3.10 `CaseClinicalRecordAppService`

关键方法：
- `saveDiagnoses(Long caseId, SaveCaseDiagnosesCommand command)`
- `saveToothRecords(Long caseId, SaveCaseToothRecordsCommand command)`

### 3.11 `CaseStatusMachine`

职责：病例状态机校验。

关键方法：
- `ensureTransitionAllowed(String currentStatus, String targetStatus)`

## 4. 图像与附件模块

### 4.1 `FileController`

关键方法：
- `upload`
- `accessUrl`
- `content`

### 4.2 `CaseImageController`

关键方法：
- `create`
- `list`
- `detail`

### 4.3 `ImageQualityCheckController`

关键方法：
- `save`
- `getCurrent`

### 4.4 `AttachmentAppService`

职责：附件上传与签名访问。

关键方法：
- `upload(MultipartFile file)`
- `createAccessUrl(Long attachmentId, HttpServletRequest request)`
- `loadContent(Long attachmentId, Long expireAt, String signature)`

### 4.5 `CaseImageAppService`

职责：病例图像与质检。

关键方法：
- `createCaseImage(Long caseId, CreateCaseImageCommand command)`
- `listCaseImages(Long caseId)`
- `getImage(Long imageId)`
- `saveQualityCheck(Long imageId, SaveImageQualityCheckCommand command)`
- `getCurrentQualityCheck(Long imageId)`

### 4.6 `LocalObjectStorageService`

职责：当前真实对象存储实现。

关键方法：
- `store`
- `load`
- `delete`

说明：
- 把对象保存到 `${localRoot}/${bucketName}/attachments/yyyy/MM/dd/...`

## 5. 分析模块

### 5.1 `AnalysisTaskController`

关键方法：
- `createAnalysisTask`
- `retryAnalysisTask`
- `getAnalysisTaskDetail`
- `pageAnalysisTasks`

### 5.2 `CaseAnalysisAliasController`

关键方法：
- `createAnalysisFromCase`
- `submitCorrectionFromCase`

### 5.3 `CorrectionFeedbackController`

关键方法：
- `submitCorrectionFeedback`

### 5.4 `AnalysisCallbackController`

关键方法：
- `receiveAnalysisResultCallback`

### 5.5 `AnalysisTaskAppService`

职责：创建和重试分析任务。

关键方法：
- `createTask(CreateAnalysisTaskCommand command)`
- `retryTask(RetryAnalysisTaskCommand command)`

内部重要逻辑：
- `ensureOrgAccess`
- `buildRequestDto`
- `toJson`
- `defaultRemark`

### 5.6 `AnalysisQueryAppService`

关键方法：
- `getTaskDetail(Long taskId)`
- `pageTasks(AnalysisTaskPageQuery query)`

### 5.7 `AnalysisCallbackAppService`

职责：处理 AI 回调。

关键方法：
- `handleResultCallback(String rawBody, String timestamp, String signature)`

内部重要逻辑：
- `processProcessing`
- `processSuccess`
- `processFailure`
- `readCallback`
- `resolveRawResultJson`
- `buildVisualAssets`
- `toJson`
- `defaultStartedAt`
- `defaultCompletedAt`
- `defaultRemark`
- `trimToNull`

### 5.8 `CorrectionFeedbackAppService`

关键方法：
- `submit(SubmitCorrectionFeedbackCommand command)`

### 5.9 `AnalysisTaskDomainService`

职责：分析任务领域规则。

关键方法：
- `ensureCaseReadyForAnalysis`
- `ensureNoRunningTask`
- `ensureAnalyzableImagesExist`
- `ensurePatientMatchesCase`
- `generateTaskNo`
- `resolveTaskTypeCode`
- `resolveRetryReasonCode`

### 5.10 `AnalysisIdempotencyDomainService`

职责：回调幂等和重试保护。

关键方法：
- `isDuplicateTerminalCallback`
- `ensureCallbackAllowed`
- `ensureRetryAllowed`
- `hasBeenRetried`
- `shouldSkipWriteBack`

### 5.11 `AnalysisCallbackDomainService`

职责：回调内容解析与状态决策。

关键方法：
- `normalizeAndValidateTaskNo`
- `normalizeAndValidateTaskStatus`
- `validateSuccessCallbackCompleteness`
- `resolveTargetCaseStatus`
- `resolveChangeReasonCode`
- `extractSummaryAggregates`

### 5.12 分析消息相关类

| 类名 | 关键方法 |
| --- | --- |
| `LoggingAnalysisTaskEventPublisher` | `publishRequested` `publishCompleted` `publishFailed` |
| `RabbitAnalysisTaskEventPublisher` | `publishRequested` `publishCompleted` `publishFailed` |
| `AiCallbackSignatureVerifier` | `verify` |
| `AnalysisMessagingProperties` | `getMode` 及 Rabbit 配置 getter/setter |
| `AiAnalysisRequestDTO` | 请求 DTO 定义 |
| `AiAnalysisCallbackDTO` | 回调 DTO 定义 |

## 6. 报告模块

### 6.1 `ReportController`

关键方法：
- `generateReport`
- `listCaseReports`
- `getReport`
- `exportReport`

### 6.2 `ReportTemplateController`

关键方法：
- `createTemplate`
- `updateTemplate`
- `listTemplates`
- `getTemplate`

### 6.3 `ReportAppService`

职责：报告生成与导出审计。

关键方法：
- `generateReport(Long caseId, GenerateReportCommand command)`
- `exportReport(Long reportId, ExportReportCommand command)`

内部重要逻辑：
- `storeReportPdf`
- `deleteStoredObjectQuietly`
- `triggerFollowupIfNeeded`
- `transitionCaseToReportReady`
- `md5`
- `trimToNull`
- `ensureOrgAccess`

### 6.4 `ReportQueryAppService`

关键方法：
- `listCaseReports(Long caseId)`
- `getReport(Long reportId)`

### 6.5 `ReportTemplateAppService`

关键方法：
- `createTemplate(CreateReportTemplateCommand command)`
- `updateTemplate(Long templateId, UpdateReportTemplateCommand command)`
- `listTemplates(ReportTemplateListQuery query)`
- `getTemplate(Long templateId)`

### 6.6 `ReportDomainService`

关键方法：
- `normalizeReportType`
- `ensureCaseStatusAllowed`
- `buildReportNo`
- `draftStatus`
- `finalStatus`
- `normalizeExportType`
- `normalizeExportChannel`
- `buildSummaryText`

### 6.7 `ReportTemplateDomainService`

关键方法：
- `resolveTemplateContent`
- `validateTemplateContent`

### 6.8 `ReportPdfService`

职责：当前极简 PDF 生成器。

关键方法：
- `generatePdf(String renderedContent)`
- `buildContentStream`
- `splitLines`
- `toAscii`
- `escapePdfText`
- `write`

## 7. 随访模块

### 7.1 `FollowupPlanController`

关键方法：
- `createPlan`
- `listCasePlans`
- `getPlan`
- `cancelPlan`
- `closePlan`

### 7.2 `FollowupTaskController`

关键方法：
- `createTask`
- `listCaseTasks`
- `getTask`
- `updateTaskStatus`
- `assignTask`

### 7.3 `FollowupRecordController`

关键方法：
- `addRecord`
- `listCaseRecords`
- `listTaskRecords`

### 7.4 `FollowupPlanAppService`

关键方法：
- `createPlan`
- `cancelPlan`
- `closePlan`
- `getPlan`
- `listCasePlans`

### 7.5 `FollowupTaskAppService`

关键方法：
- `createTask`
- `updateTaskStatus`
- `assignTask`
- `listByPlan`
- `listByCase`
- `getTask`

### 7.6 `FollowupRecordAppService`

关键方法：
- `addRecord`
- `listByTask`
- `listByCase`

### 7.7 `FollowupTriggerService`

职责：从报告结果触发随访。

关键方法：
- `triggerFromReport(Long caseId, Long patientId, Long orgId, Long reportId, String riskLevelCode, String reviewSuggestedFlag, Integer recommendedCycleDays, Long operatorUserId)`

内部重要逻辑：
- `transitionCaseToFollowupRequired`
- `recordNotify`

### 7.8 `FollowupDomainService`

关键方法：
- `shouldTriggerFollowup`
- `resolveTriggerSource`
- `resolvePlanType`
- `resolveIntervalDays`
- `resolveFirstTaskDueDate`
- `buildPlanNo`
- `buildTaskNo`
- `buildRecordNo`
- `normalizeFollowupMethod`
- `normalizeContactResult`

## 8. 看板模块

### 8.1 `DashboardController`

关键方法：
- `getOverview`
- `getCaseStatusDistribution`
- `getRiskLevelDistribution`
- `getFollowupTaskSummary`
- `getBacklogSummary`
- `getTrend`

### 8.2 `DashboardOpsController`

关键方法：
- `getModelRuntime`

### 8.3 看板 AppService

| 类名 | 关键方法 |
| --- | --- |
| `DashboardOverviewAppService` | `getOverview` |
| `DashboardCaseStatsAppService` | `getCaseStatusDistribution` |
| `DashboardRiskStatsAppService` | `getRiskLevelDistribution` |
| `DashboardFollowupStatsAppService` | `getFollowupTaskSummary` |
| `DashboardBacklogAppService` | `getBacklogSummary` |
| `DashboardTrendAppService` | `getTrend` |
| `DashboardOpsMetricsAppService` | `getModelRuntime` |

### 8.4 `DashboardStatsRepository`

职责：直接用 SQL 查询统计结果。

关键方法：
- `queryOverview(Long orgId)`
- `queryCaseStatusDistribution(Long orgId)`
- `queryRiskLevelDistribution(Long orgId)`
- `queryFollowupTaskSummary(Long orgId)`
- `queryBacklogSummary(Long orgId)`
- `queryModelRuntime(Long orgId)`
- `queryTrend(Long orgId, DashboardRangeQuery rangeQuery)`

内部辅助方法：
- `longValue`
- `stringValue`
- `rate`
- `queryCountByDate`

## 9. Python 开发建议优先阅读顺序

如果 Python 开发要快速掌握 Java 侧行为，建议按下面顺序阅读：

1. `AnalysisTaskAppService`
2. `AiAnalysisRequestDTO`
3. `RabbitAnalysisTaskEventPublisher`
4. `AiCallbackSignatureVerifier`
5. `AiAnalysisCallbackDTO`
6. `AnalysisCallbackAppService`
7. `CaseCommandAppService`
8. `ReportAppService`
9. `FollowupTriggerService`
10. `DashboardStatsRepository`

