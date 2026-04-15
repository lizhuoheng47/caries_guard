# API接口清单

更新日期：2026-04-15

所有接口默认返回 `ApiResponse<T>`。

## 1. 认证与系统

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | `AuthController.login` | 登录开放 |
| GET | `/api/v1/auth/me` | `AuthController.currentUser` | 登录用户 |
| GET | `/api/v1/auth/permissions` | `AuthController.currentPermissions` | 登录用户 |
| GET | `/api/v1/system/users` | `SystemAdminController.pageUsers` | `system:user:list` |
| GET | `/api/v1/system/users/{userId}` | `getUser` | `system:user:view` |
| POST | `/api/v1/system/users` | `createUser` | `system:user:create` |
| PUT | `/api/v1/system/users/{userId}` | `updateUser` | `system:user:update` |
| GET | `/api/v1/system/roles` | `listRoles` | `system:role:list` |
| POST | `/api/v1/system/roles` | `createRole` | `system:role:create` |
| PUT | `/api/v1/system/roles/{roleId}` | `updateRole` | `system:role:update` |
| GET | `/api/v1/system/menus` | `listMenus` | `system:menu:list` |
| POST | `/api/v1/system/menus` | `createMenu` | `system:menu:create` |
| PUT | `/api/v1/system/menus/{menuId}` | `updateMenu` | `system:menu:update` |
| GET | `/api/v1/system/data-permission-rules` | `listDataPermissionRules` | `system:data-permission-rule:list` |
| POST | `/api/v1/system/data-permission-rules` | `createDataPermissionRule` | `system:data-permission-rule:create` |
| PUT | `/api/v1/system/data-permission-rules/{ruleId}` | `updateDataPermissionRule` | `system:data-permission-rule:update` |
| GET | `/api/v1/system/dicts` | `listDictTypes` | `system:dict:list` |
| GET | `/api/v1/system/dicts/{dictType}` | `listDictItems` | `system:dict:list` |
| GET | `/api/v1/system/configs/{configKey}` | `getConfig` | `system:config:view` |
| GET | `/api/v1/system/ping` | `ping` | 开放 |

## 2. 患者、就诊、病例

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/patients` | `createPatient` | `patient:create` |
| PUT | `/api/v1/patients/{patientId}` | `updatePatient` | `patient:update` |
| GET | `/api/v1/patients/{patientId}` | `getPatient` | `patient:view` |
| GET | `/api/v1/patients` | `pagePatients` | `patient:list` |
| POST | `/api/v1/visits` | `createVisit` | `visit:create` |
| GET | `/api/v1/visits/{visitId}` | `getVisit` | `visit:view` |
| GET | `/api/v1/visits` | `pageVisits` | `visit:list` |
| POST | `/api/v1/cases` | `createCase` | `case:create` |
| GET | `/api/v1/cases/{caseId}` | `getCase` | `case:view` |
| GET | `/api/v1/cases` | `pageCases` | `case:list` |
| POST | `/api/v1/cases/{caseId}/diagnoses` | `saveDiagnoses` | `case:update` |
| POST | `/api/v1/cases/{caseId}/tooth-records` | `saveToothRecords` | `case:update` |
| POST | `/api/v1/cases/{caseId}/status-transition` | `transitionStatus` | `case:transition` |

## 3. 文件、影像、质检

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/files/upload` | `FileController.upload` | `image:upload` |
| GET | `/api/v1/files/{attachmentId}/access-url` | `accessUrl` | `image:read` |
| GET | `/api/v1/files/{attachmentId}/content` | `content` | 签名校验 |
| POST | `/api/v1/cases/{caseId}/images` | `CaseImageController.create` | `image:create` |
| GET | `/api/v1/cases/{caseId}/images` | `list` | `image:list` |
| GET | `/api/v1/cases/{caseId}/images/detail/{imageId}` | `detail` | `image:view` |
| POST | `/api/v1/images/{imageId}/quality-checks` | `save` | `image:quality-check` |
| GET | `/api/v1/images/{imageId}/quality-checks/current` | `getCurrent` | `image:view` |

## 4. AI 分析

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/analysis/tasks` | `createAnalysisTask` | `analysis:create` |
| POST | `/api/v1/analysis/tasks/retry` | `retryAnalysisTask` | `analysis:create` |
| GET | `/api/v1/analysis/tasks/{taskId}` | `getAnalysisTaskDetail` | `analysis:view` |
| GET | `/api/v1/analysis/tasks` | `pageAnalysisTasks` | `analysis:view` |
| POST | `/api/v1/cases/{caseId}/analysis` | `createAnalysisFromCase` | `analysis:create` |
| POST | `/api/v1/analysis/corrections` | `submitCorrectionFeedback` | `analysis:correct` |
| POST | `/api/v1/cases/{caseId}/corrections` | `submitCorrectionFromCase` | `analysis:correct` |
| POST | `/api/v1/internal/ai/callbacks/analysis-result` | `receiveAnalysisResultCallback` | HMAC/内部鉴权口径 |

AI 回调请求体为 `AiAnalysisResultCallbackCommand`。

## 5. 报告

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/cases/{caseId}/reports` | `generateReport` | `report:generate` |
| GET | `/api/v1/cases/{caseId}/reports` | `listCaseReports` | `report:view` |
| GET | `/api/v1/reports/{reportId}` | `getReport` | `report:view` |
| POST | `/api/v1/reports/{reportId}/export` | `exportReport` | `report:export` |
| POST | `/api/v1/report-templates` | `createTemplate` | `report:template:manage` |
| PUT | `/api/v1/report-templates/{templateId}` | `updateTemplate` | `report:template:manage` |
| GET | `/api/v1/report-templates` | `listTemplates` | `report:template:view` |
| GET | `/api/v1/report-templates/{templateId}` | `getTemplate` | `report:template:view` |

`exportReport` 返回 `ReportExportResultVO`：`reportId`、`exported`、`exportLogId`、`attachmentId`、`downloadUrl`、`expireAt`。

## 6. 随访

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/v1/cases/{caseId}/followup/plans` | `createPlan` | `followup:plan:create` |
| GET | `/api/v1/cases/{caseId}/followup/plans` | `listCasePlans` | `followup:plan:view` |
| GET | `/api/v1/followup/plans/{planId}` | `getPlan` | `followup:plan:view` |
| POST | `/api/v1/followup/plans/{planId}/cancel` | `cancelPlan` | `followup:plan:manage` |
| POST | `/api/v1/followup/plans/{planId}/close` | `closePlan` | `followup:plan:manage` |
| POST | `/api/v1/cases/{caseId}/followup/tasks` | `createTask` | `followup:task:create` |
| GET | `/api/v1/cases/{caseId}/followup/tasks` | `listCaseTasks` | `followup:task:view` |
| GET | `/api/v1/followup/tasks/{taskId}` | `getTask` | `followup:task:view` |
| POST | `/api/v1/followup/tasks/{taskId}/status` | `updateTaskStatus` | `followup:task:manage` |
| POST | `/api/v1/followup/tasks/{taskId}/assign` | `assignTask` | `followup:task:manage` |
| POST | `/api/v1/followup/records` | `addRecord` | `followup:record:create` |
| GET | `/api/v1/cases/{caseId}/followup/records` | `listCaseRecords` | `followup:record:view` |
| GET | `/api/v1/followup/tasks/{taskId}/records` | `listTaskRecords` | `followup:record:view` |

## 7. Dashboard

| Method | Path | 方法 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/v1/dashboard/overview` | `getOverview` | `dashboard:view` |
| GET | `/api/v1/dashboard/case-status-distribution` | `getCaseStatusDistribution` | `dashboard:view` |
| GET | `/api/v1/dashboard/risk-level-distribution` | `getRiskLevelDistribution` | `dashboard:view` |
| GET | `/api/v1/dashboard/followup-task-summary` | `getFollowupTaskSummary` | `dashboard:view` |
| GET | `/api/v1/dashboard/backlog-summary` | `getBacklogSummary` | `dashboard:view` |
| GET | `/api/v1/dashboard/trend` | `getTrend` | `dashboard:view` |
| GET | `/api/v1/dashboard/model-runtime` | `getModelRuntime` | `dashboard:ops:view` |

`model-runtime` 返回模型版本、任务数、成功率、失败率、平均推理耗时、高不确定性占比、建议复核占比、修正反馈数和按版本聚合列表。