# API接口清单

本文档只列出当前源码中实际存在的 HTTP API。

统一说明：
- 除特别注明外，接口均要求 JWT 认证
- 统一响应包装为 `ApiResponse<T>`
- Swagger/OpenAPI 已开启，可通过 `/swagger-ui/index.html` 查看

## 1. 公开接口

| 方法 | 路径 | 控制器方法 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | `AuthController.login` | 用户登录，返回 token |
| `GET` | `/api/v1/system/ping` | `SystemSupportController.ping` | 系统连通性检查 |
| `GET` | `/api/v1/files/{attachmentId}/content` | `FileController.content` | 按签名下载附件内容 |
| `POST` | `/api/v1/internal/ai/callbacks/analysis-result` | `AnalysisCallbackController.receiveAnalysisResultCallback` | AI 回调入口 |
| `GET` | `/actuator/health` | Spring Actuator | 健康检查 |
| `GET` | `/v3/api-docs/**` | Springdoc | OpenAPI 文档 |
| `GET` | `/swagger-ui/**` | Springdoc | Swagger UI 静态资源 |
| `GET` | `/swagger-ui.html` | Springdoc | Swagger UI 跳转 |

## 2. 认证与系统元数据

### 2.1 认证接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | `login` | `username` `password` | 登录 |
| `GET` | `/api/v1/auth/me` | `currentUser` | 无 | 当前用户信息 |
| `GET` | `/api/v1/auth/permissions` | `currentPermissions` | 无 | 当前用户权限集合 |

### 2.2 系统元数据接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/system/ping` | `ping` | 无 | 健康探针 |
| `GET` | `/api/v1/system/dicts` | `listDictTypes` | 无 | 字典类型列表 |
| `GET` | `/api/v1/system/dicts/{dictType}` | `listDictItems` | `dictType` | 指定字典项 |
| `GET` | `/api/v1/system/configs/{configKey}` | `getConfig` | `configKey` | 配置查询 |

## 3. 系统管理接口

### 3.1 用户管理

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/system/users` | `pageUsers` | `pageNo` `pageSize` 等分页查询参数 | 用户分页 |
| `GET` | `/api/v1/system/users/{userId}` | `getUser` | `userId` | 用户详情 |
| `POST` | `/api/v1/system/users` | `createUser` | `deptId` `userNo` `username` `password` `realName` `nickName` `userTypeCode` `genderCode` `phone` `email` `avatarUrl` `certificateTypeCode` `certificateNo` `status` `remark` `roleIds` | 创建用户 |
| `PUT` | `/api/v1/system/users/{userId}` | `updateUser` | 同创建，但 `password` 可选，`status` 必填 | 更新用户 |

### 3.2 角色管理

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/system/roles` | `listRoles` | `status` | 角色列表 |
| `GET` | `/api/v1/system/roles/{roleId}` | `getRole` | `roleId` | 角色详情 |
| `POST` | `/api/v1/system/roles` | `createRole` | `roleCode` `roleName` `roleSort` `dataScopeCode` `status` `remark` `menuIds` | 创建角色 |
| `PUT` | `/api/v1/system/roles/{roleId}` | `updateRole` | 同创建，且 `status` 必填 | 更新角色 |

### 3.3 菜单管理

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/system/menus` | `listMenus` | `status` | 菜单列表 |
| `GET` | `/api/v1/system/menus/{menuId}` | `getMenu` | `menuId` | 菜单详情 |
| `POST` | `/api/v1/system/menus` | `createMenu` | `parentId` `menuName` `menuTypeCode` `routePath` `componentPath` `permissionCode` `icon` `visibleFlag` `cacheFlag` `orderNum` `status` `remark` | 创建菜单 |
| `PUT` | `/api/v1/system/menus/{menuId}` | `updateMenu` | 同创建，且 `status` 必填 | 更新菜单 |

### 3.4 数据权限规则管理

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/system/data-permission-rules` | `listDataPermissionRules` | `roleId` `moduleCode` | 查询规则 |
| `POST` | `/api/v1/system/data-permission-rules` | `createDataPermissionRule` | `roleId` `moduleCode` `scopeTypeCode` `customDeptIds` `columnMaskPolicy` `status` `remark` | 创建规则 |
| `PUT` | `/api/v1/system/data-permission-rules/{ruleId}` | `updateDataPermissionRule` | 同创建，且 `status` 必填 | 更新规则 |

## 4. 患者、就诊、病例接口

### 4.1 患者接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/patients` | `createPatient` | `patientName` `genderCode` `birthDate` `phone` `idCardNo` `sourceCode` `firstVisitDate` `privacyLevelCode` `status` `remark` `guardian` | 创建患者 |
| `PUT` | `/api/v1/patients/{patientId}` | `updatePatient` | 与创建类似 | 更新患者 |
| `GET` | `/api/v1/patients/{patientId}` | `getPatient` | `patientId` | 患者详情 |
| `GET` | `/api/v1/patients` | `pagePatients` | 分页查询参数 | 患者分页 |

`guardian` 子对象字段：
- `guardianName`
- `relationCode`
- `phone`
- `certificateTypeCode`
- `certificateNo`
- `primaryFlag`
- `status`
- `remark`

### 4.2 就诊接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/visits` | `createVisit` | `patientId` `departmentId` `doctorUserId` `visitTypeCode` `visitDate` `complaint` `triageLevelCode` `sourceChannelCode` `status` `remark` | 创建就诊 |
| `GET` | `/api/v1/visits/{visitId}` | `getVisit` | `visitId` | 就诊详情 |
| `GET` | `/api/v1/visits` | `pageVisits` | 分页查询参数 | 就诊分页 |

### 4.3 病例接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/cases` | `createCase` | `visitId` `patientId` `caseTypeCode` `caseTitle` `chiefComplaint` `priorityCode` `clinicalNotes` `onsetDate` `status` `remark` | 创建病例 |
| `GET` | `/api/v1/cases/{caseId}` | `getCase` | `caseId` | 病例详情 |
| `GET` | `/api/v1/cases` | `pageCases` | 分页与筛选参数 | 病例分页 |
| `POST` | `/api/v1/cases/{caseId}/diagnoses` | `saveDiagnoses` | `diagnoses[]` | 保存诊断 |
| `POST` | `/api/v1/cases/{caseId}/tooth-records` | `saveToothRecords` | `toothRecords[]` | 保存牙位记录 |
| `POST` | `/api/v1/cases/{caseId}/status-transition` | `transitionStatus` | `targetStatusCode` `reasonCode` `reasonRemark` | 状态流转 |

`diagnoses[]` 元素字段：
- `diagnosisTypeCode`
- `diagnosisName`
- `severityCode`
- `diagnosisBasis`
- `diagnosisDesc`
- `treatmentAdvice`
- `finalFlag`
- `remark`

`toothRecords[]` 元素字段：
- `sourceImageId`
- `toothCode`
- `toothSurfaceCode`
- `issueTypeCode`
- `severityCode`
- `findingDesc`
- `suggestion`
- `sortOrder`
- `remark`

## 5. 文件与图像接口

### 5.1 文件接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/files/upload` | `upload` | multipart `file` | 上传附件 |
| `GET` | `/api/v1/files/{attachmentId}/access-url` | `accessUrl` | `attachmentId` | 生成签名访问 URL |
| `GET` | `/api/v1/files/{attachmentId}/content` | `content` | `attachmentId` `expireAt` `signature` | 按签名读取附件 |

说明：
- `/content` 是公开接口，但必须带合法签名参数
- Java 内部也会使用附件元数据绑定图像和报告

### 5.2 病例图像接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/cases/{caseId}/images` | `create` | `attachmentId` `visitId` `patientId` `imageTypeCode` `imageSourceCode` `shootingTime` `bodyPositionCode` `primaryFlag` `remark` | 创建病例图像 |
| `GET` | `/api/v1/cases/{caseId}/images` | `list` | `caseId` | 病例图像列表 |
| `GET` | `/api/v1/cases/{caseId}/images/detail/{imageId}` | `detail` | `caseId` `imageId` | 图像详情 |

### 5.3 图像质检接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/images/{imageId}/quality-checks` | `save` | `checkTypeCode` `checkResultCode` `qualityScore` `blurScore` `exposureScore` `integrityScore` `occlusionScore` `issueCodes` `suggestionText` `remark` | 保存质检结果 |
| `GET` | `/api/v1/images/{imageId}/quality-checks/current` | `getCurrent` | `imageId` | 当前有效质检结果 |

## 6. 分析接口

### 6.1 分析任务接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/analysis/tasks` | `createAnalysisTask` | `caseId` `patientId` `forceRetryFlag` `taskTypeCode` `remark` | 创建分析任务 |
| `POST` | `/api/v1/analysis/tasks/retry` | `retryAnalysisTask` | `taskId` `reasonCode` `reasonRemark` | 重试分析任务 |
| `GET` | `/api/v1/analysis/tasks/{taskId}` | `getAnalysisTaskDetail` | `taskId` | 任务详情 |
| `GET` | `/api/v1/analysis/tasks` | `pageAnalysisTasks` | 分页与筛选参数 | 任务分页 |

### 6.2 病例别名接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/cases/{caseId}/analysis` | `createAnalysisFromCase` | 与创建分析任务类似 | 用病例路径触发分析 |
| `POST` | `/api/v1/cases/{caseId}/corrections` | `submitCorrectionFromCase` | 与纠偏接口类似 | 用病例路径提交纠偏 |

### 6.3 纠偏接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/analysis/corrections` | `submitCorrectionFeedback` | `caseId` `diagnosisId` `sourceImageId` `feedbackTypeCode` `originalInferenceJson` `correctedTruthJson` | 提交纠偏反馈 |

### 6.4 AI 回调接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/internal/ai/callbacks/analysis-result` | `receiveAnalysisResultCallback` | Header: `timestamp` `signature`; Body: 回调 DTO | AI 结果回调 |

回调 Body 顶层字段：
- `taskNo`
- `taskStatusCode`
- `startedAt`
- `completedAt`
- `modelVersion`
- `summary`
- `rawResultJson`
- `visualAssets`
- `riskAssessment`
- `errorMessage`

`summary` 字段：
- `overallHighestSeverity`
- `uncertaintyScore`
- `reviewSuggestedFlag`
- `teethCount`

`visualAssets[]` 字段：
- `assetTypeCode`
- `attachmentId`

`riskAssessment` 字段：
- `overallRiskLevelCode`
- `assessmentReportJson`
- `recommendedCycleDays`

## 7. 报告接口

### 7.1 报告主接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/cases/{caseId}/reports` | `generateReport` | `reportTypeCode` `doctorConclusion` `remark` | 生成报告 |
| `GET` | `/api/v1/cases/{caseId}/reports` | `listCaseReports` | `caseId` | 按病例列报告 |
| `GET` | `/api/v1/reports/{reportId}` | `getReport` | `reportId` | 报告详情 |
| `POST` | `/api/v1/reports/{reportId}/export` | `exportReport` | `exportTypeCode` `exportChannelCode` | 导出审计 |

说明：
- `exportReport` 当前不会直接返回 PDF 文件流

### 7.2 报告模板接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/report-templates` | `createTemplate` | 创建模板命令 | 新建模板 |
| `PUT` | `/api/v1/report-templates/{templateId}` | `updateTemplate` | 更新模板命令 | 更新模板 |
| `GET` | `/api/v1/report-templates` | `listTemplates` | `reportTypeCode` | 模板列表 |
| `GET` | `/api/v1/report-templates/{templateId}` | `getTemplate` | `templateId` | 模板详情 |

## 8. 随访接口

### 8.1 随访计划接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/cases/{caseId}/followup/plans` | `createPlan` | `planTypeCode` `intervalDays` `ownerUserId` `remark` | 创建随访计划 |
| `GET` | `/api/v1/cases/{caseId}/followup/plans` | `listCasePlans` | `caseId` | 按病例查计划 |
| `GET` | `/api/v1/followup/plans/{planId}` | `getPlan` | `planId` | 计划详情 |
| `POST` | `/api/v1/followup/plans/{planId}/cancel` | `cancelPlan` | `planId` | 取消计划 |
| `POST` | `/api/v1/followup/plans/{planId}/close` | `closePlan` | `planId` | 关闭计划 |

### 8.2 随访任务接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/cases/{caseId}/followup/tasks` | `createTask` | `planId` `taskTypeCode` `assignedToUserId` `dueDate` `remark` | 创建任务 |
| `GET` | `/api/v1/cases/{caseId}/followup/tasks` | `listCaseTasks` | `caseId` | 按病例查任务 |
| `GET` | `/api/v1/followup/tasks/{taskId}` | `getTask` | `taskId` | 任务详情 |
| `POST` | `/api/v1/followup/tasks/{taskId}/status` | `updateTaskStatus` | `targetStatusCode` `remark` | 更新任务状态 |
| `POST` | `/api/v1/followup/tasks/{taskId}/assign` | `assignTask` | 指派用户 ID | 指派任务 |

### 8.3 随访记录接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/followup/records` | `addRecord` | `taskId` `followupMethodCode` `contactResultCode` `followNext` `nextIntervalDays` `outcomeSummary` `doctorNotes` `remark` | 新增随访记录 |
| `GET` | `/api/v1/cases/{caseId}/followup/records` | `listCaseRecords` | `caseId` | 按病例查记录 |
| `GET` | `/api/v1/followup/tasks/{taskId}/records` | `listTaskRecords` | `taskId` | 按任务查记录 |

## 9. 看板接口

| 方法 | 路径 | 控制器方法 | 主要入参 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/dashboard/overview` | `getOverview` | 无 | 总览统计 |
| `GET` | `/api/v1/dashboard/case-status-distribution` | `getCaseStatusDistribution` | 无 | 病例状态分布 |
| `GET` | `/api/v1/dashboard/risk-level-distribution` | `getRiskLevelDistribution` | 无 | 风险等级分布 |
| `GET` | `/api/v1/dashboard/followup-task-summary` | `getFollowupTaskSummary` | 无 | 随访任务汇总 |
| `GET` | `/api/v1/dashboard/backlog-summary` | `getBacklogSummary` | 无 | 积压摘要 |
| `GET` | `/api/v1/dashboard/trend` | `getTrend` | `rangeType` `startDate` `endDate` | 趋势数据 |
| `GET` | `/api/v1/dashboard/model-runtime` | `getModelRuntime` | 无 | 模型运行指标 |

## 10. Python 联调最相关的接口顺序

如果 Python 侧只关心 AI 联调，最常用顺序通常是：

1. `POST /api/v1/auth/login`
2. `POST /api/v1/files/upload`
3. `POST /api/v1/cases/{caseId}/images`
4. `POST /api/v1/images/{imageId}/quality-checks`
5. `POST /api/v1/analysis/tasks`
6. Rabbit 消费 `analysis.requested`
7. `POST /api/v1/internal/ai/callbacks/analysis-result`
8. `POST /api/v1/cases/{caseId}/reports`
9. `GET /api/v1/reports/{reportId}`
10. `GET /api/v1/dashboard/*`

