# P7 Dashboard 数据口径冻结表与首批接口清单

## 1. 文档用途

本文件用于冻结 P7 dashboard 的统计口径、接口范围和验收断言。

当前口径已经进入代码实现与真库测试阶段，后续开发和答辩均以本文件为准，不再允许前后端各自解释指标。

## 2. 当前执行结论

- P7 已进入可执行并已落地状态。
- 首批 7 个 dashboard 接口已完成，其中 `trend` 已作为第二批接口补齐。
- 所有统计均按当前登录用户 `org_id` 做机构隔离，不开放跨机构全局聚合。
- 当前实现直接基于业务表聚合，不新增 dashboard 快照表。

## 3. 统一基础口径

### 3.1 时间口径

- `overview`、`case-status-distribution`、`risk-level-distribution`、`followup-task-summary`、`backlog-summary` 为当前快照或全量累计。
- `model-runtime` 的近期窗口固定为最近 30 天。
- `trend` 支持 `TODAY`、`LAST_7_DAYS`、`LAST_30_DAYS`、`CUSTOM`。
- `trend` 默认范围为 `LAST_7_DAYS`。

### 3.2 组织口径

- 所有 dashboard 查询默认按 `SecurityContextUtils.currentUser().getOrgId()` 过滤。
- 普通用户与管理员当前都只看本机构数据。
- 当前阶段不支持跨机构总览。

### 3.3 去重口径

- 患者数按 `pat_patient.id` 统计。
- 病例数按 `med_case.id` 统计。
- 已分析病例数按 `ana_task_record` 成功任务关联的 `case_id` 去重。
- 已生成报告病例数按 `rpt_record` 关联的 `case_id` 去重。
- 风险分布按病例维度统计，每个病例仅取最新一条有效风险记录。

### 3.4 状态口径

病例主状态统一使用：

- `CREATED`
- `QC_PENDING`
- `ANALYZING`
- `REVIEW_PENDING`
- `REPORT_READY`
- `FOLLOWUP_REQUIRED`
- `CLOSED`
- `CANCELLED`

随访任务状态统一使用：

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`
- `CANCELLED`

AI 任务状态统一使用：

- `QUEUEING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`

### 3.5 风险口径

- 风险来源表为 `med_risk_assessment_record`。
- 同一病例存在多条风险记录时，仅取 `MAX(id)` 对应的最新有效记录。
- 风险分布统计的是当前病例风险结构，不统计历史风险次数。

### 3.6 Followup 口径

- 待办任务数 = `TODO + IN_PROGRESS`
- 逾期任务数 = `OVERDUE`
- 完成任务数 = `DONE`
- `completionRate = DONE / (TODO + IN_PROGRESS + DONE + OVERDUE)`
- `overdueRate = OVERDUE / (TODO + IN_PROGRESS + DONE + OVERDUE)`
- `CANCELLED` 不进入完成率和逾期率分母

## 4. 接口冻结清单

### 4.1 已完成接口

1. `GET /api/v1/dashboard/overview`
2. `GET /api/v1/dashboard/case-status-distribution`
3. `GET /api/v1/dashboard/risk-level-distribution`
4. `GET /api/v1/dashboard/followup-task-summary`
5. `GET /api/v1/dashboard/backlog-summary`
6. `GET /api/v1/dashboard/model-runtime`
7. `GET /api/v1/dashboard/trend`

### 4.2 权限口径

- `overview`、`case-status-distribution`、`risk-level-distribution`、`followup-task-summary`、`backlog-summary`、`trend` 需要 `dashboard:view`
- `model-runtime` 需要 `dashboard:ops:view`

## 5. 各接口口径冻结

### 5.1 `GET /api/v1/dashboard/overview`

返回字段：

- `patientCount`
- `caseCount`
- `analyzedCaseCount`
- `generatedReportCount`
- `followupRequiredCaseCount`
- `closedCaseCount`

数据来源：

- `pat_patient`
- `med_case`
- `ana_task_record`
- `rpt_record`

冻结规则：

- `analyzedCaseCount` 统计成功 AI 任务关联病例去重数
- `generatedReportCount` 统计已生成报告病例去重数
- `followupRequiredCaseCount` 统计 `case_status_code = FOLLOWUP_REQUIRED` 或 `followup_required_flag = 1`

### 5.2 `GET /api/v1/dashboard/case-status-distribution`

返回字段：

- `createdCount`
- `qcPendingCount`
- `analyzingCount`
- `reviewPendingCount`
- `reportReadyCount`
- `followupRequiredCount`
- `closedCount`
- `cancelledCount`

数据来源：

- `med_case`

冻结规则：

- 每个病例仅按当前 `case_status_code` 归类一次
- 不从状态日志回推当前状态

### 5.3 `GET /api/v1/dashboard/risk-level-distribution`

返回字段：

- `highRiskCount`
- `mediumRiskCount`
- `lowRiskCount`

数据来源：

- `med_risk_assessment_record`

冻结规则：

- 每个病例仅取最新有效风险记录
- 统计对象是病例数，不是风险记录条数

### 5.4 `GET /api/v1/dashboard/followup-task-summary`

返回字段：

- `todoCount`
- `inProgressCount`
- `doneCount`
- `overdueCount`
- `completionRate`
- `overdueRate`

数据来源：

- `fup_task`

冻结规则：

- `CANCELLED` 不进入比率分母

### 5.5 `GET /api/v1/dashboard/backlog-summary`

返回字段：

- `reviewPendingCaseCount`
- `todoFollowupTaskCount`
- `overdueFollowupTaskCount`
- `highRiskPendingCaseCount`

数据来源：

- `med_case`
- `fup_task`
- `med_risk_assessment_record`

冻结规则：

- `todoFollowupTaskCount = TODO + IN_PROGRESS`
- `highRiskPendingCaseCount` 统计最新风险为 `HIGH` 且病例状态不在 `CLOSED`、`CANCELLED` 的病例

### 5.6 `GET /api/v1/dashboard/model-runtime`

返回字段：

- `currentModelVersion`
- `recentTaskCount`
- `successTaskCount`
- `failedTaskCount`
- `successRate`

数据来源：

- `ana_task_record`

冻结规则：

- 近期窗口固定最近 30 天
- `currentModelVersion` 取最近成功任务的 `model_version`
- `successRate = successTaskCount / recentTaskCount`

### 5.7 `GET /api/v1/dashboard/trend`

返回字段：

- `date`
- `newCaseCount`
- `analysisCompletedCount`
- `reportGeneratedCount`
- `followupTriggeredCount`

数据来源：

- `med_case.created_at`
- `ana_task_record.completed_at` 且 `task_status_code = SUCCESS`
- `rpt_record.created_at`
- `fup_plan.created_at`

冻结规则：

- 按天聚合
- 缺失日期补零
- 当前统计口径按记录数聚合，不做病例去重

## 6. 当前实现映射

已落地类：

- `DashboardController`
- `DashboardOpsController`
- `DashboardOverviewAppService`
- `DashboardCaseStatsAppService`
- `DashboardRiskStatsAppService`
- `DashboardFollowupStatsAppService`
- `DashboardBacklogAppService`
- `DashboardOpsMetricsAppService`
- `DashboardTrendAppService`
- `DashboardStatsRepository`

已落地输出对象：

- `DashboardOverviewVO`
- `CaseStatusDistributionVO`
- `RiskLevelDistributionVO`
- `FollowupTaskSummaryVO`
- `BacklogSummaryVO`
- `ModelRuntimeVO`
- `DashboardTrendPointVO`
- `DashboardRangeQuery`

## 7. 验收测试证据

boot 真库测试已通过：

- `DashboardOverviewIntegrationTest`
- `DashboardCaseStatusDistributionIntegrationTest`
- `DashboardRiskLevelDistributionIntegrationTest`
- `DashboardFollowupTaskSummaryIntegrationTest`
- `DashboardBacklogIntegrationTest`
- `DashboardModelRuntimeIntegrationTest`
- `DashboardTrendIntegrationTest`

核心断言点：

- `overview` 能正确统计患者、病例、分析、报告、随访、关闭病例
- `case-status-distribution` 能覆盖 8 个冻结状态且总和正确
- `risk-level-distribution` 对同一病例按最新风险记录统计
- `followup-task-summary` 对 `CANCELLED` 排除分母，完成率与逾期率正确
- `backlog-summary` 不将已关闭病例计入高风险待处理
- `model-runtime` 能正确识别最近成功模型版本与最近 30 天成功率
- `trend` 能按自定义日期范围补零并过滤范围外数据

## 8. 当前未覆盖边界声明

- 当前不支持跨机构总览、按部门下钻、按医生下钻
- 当前不引入 Redis 缓存和离线统计宽表
- 当前不提供实时推送式大屏，仅提供查询型聚合接口
- 当前 `trend` 按记录数聚合，不做复杂多版本去重

## 9. 下一步

P7 首批统计接口已完成，下一阶段进入：

1. 前端 dashboard 联调
2. 文档答辩化收口
3. P8 测试与交付阶段
