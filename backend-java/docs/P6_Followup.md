# P6 Followup 当前实现说明

## 1. 当前结论

`followup` 模块已经从规划态进入实现态，并且已经接入 report 主链路。

## 2. 已实现接口

### 2.1 计划

- `POST /api/v1/cases/{caseId}/followup/plans`
- `GET /api/v1/cases/{caseId}/followup/plans`
- `GET /api/v1/followup/plans/{planId}`
- `POST /api/v1/followup/plans/{planId}/cancel`
- `POST /api/v1/followup/plans/{planId}/close`

### 2.2 任务

- `POST /api/v1/cases/{caseId}/followup/tasks`
- `GET /api/v1/cases/{caseId}/followup/tasks`
- `GET /api/v1/followup/tasks/{taskId}`
- `POST /api/v1/followup/tasks/{taskId}/status`
- `POST /api/v1/followup/tasks/{taskId}/assign`

### 2.3 记录

- `POST /api/v1/followup/records`
- `GET /api/v1/cases/{caseId}/followup/records`
- `GET /api/v1/followup/tasks/{taskId}/records`

## 3. 自动触发规则

当前统一由 `FollowupTriggerService` 承接自动触发。

触发来源：

- `RISK_HIGH`
- `REPORT_REVIEW`
- `DOCTOR_MANUAL`

触发条件：

- 风险等级为 `HIGH`
- 或 `reviewSuggestedFlag = 1`

触发结果：

- 创建 `fup_plan`
- 创建首个 `fup_task`
- 留消息通知痕迹 `msg_notify_record`
- 尝试把病例推进到 `FOLLOWUP_REQUIRED`

## 4. 幂等保护

自动触发随访时，会按：

- `case_id`
- `trigger_source_code`
- `trigger_ref_id`

检查是否已有有效计划，防止同一报告重复触发多份计划。

## 5. 计划、任务、记录的当前行为

### 5.1 计划

- 手工创建计划时自动派生首个任务
- 计划可取消、可关闭
- 所有任务都完成或取消后，计划会自动关闭

### 5.2 任务

当前任务状态：

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`
- `CANCELLED`

当任务被标记为 `OVERDUE` 时，会尝试写一条站内通知日志。

### 5.3 记录

新增随访记录时会：

- 写入 `fup_record`
- 自动把关联任务标记为 `DONE`
- 若 `followNext = true`，自动派生下一个任务
- 若不继续随访且计划下无未完成任务，则自动关闭计划

## 6. 当前数据库表

- `fup_plan`
- `fup_task`
- `fup_record`
- `msg_notify_record`

## 7. 当前限制

- 通知只是写 `msg_notify_record`，没有真正发送短信/邮件/微信
- 没有定时扫描任务自动改 `OVERDUE` 的调度器，当前主要依赖接口/测试触发
- 没有患者自助确认或外部回访渠道集成

## 8. 测试证据

- `AnalysisReportFollowupE2ETest`
- `FollowupTriggerIdempotencyE2ETest`
- `FollowupAuditIntegrationTest`
- `FollowupOverdueIntegrationTest`
- `MainlineWorkflowE2ETest`
