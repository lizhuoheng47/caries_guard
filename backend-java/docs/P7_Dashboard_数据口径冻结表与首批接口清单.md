# P7 Dashboard 数据口径冻结表与首批接口清单

## 1. 当前结论

dashboard 首批接口已经全部实现，统计口径应以 `DashboardStatsRepository` 中的 SQL 为唯一准绳。

## 2. 当前接口清单

- `GET /api/v1/dashboard/overview`
- `GET /api/v1/dashboard/case-status-distribution`
- `GET /api/v1/dashboard/risk-level-distribution`
- `GET /api/v1/dashboard/followup-task-summary`
- `GET /api/v1/dashboard/backlog-summary`
- `GET /api/v1/dashboard/trend`
- `GET /api/v1/dashboard/model-runtime`

## 3. 当前统一口径

### 3.1 机构口径

所有统计都按当前登录用户的 `org_id` 过滤。

### 3.2 overview

- `patientCount`：`pat_patient`
- `caseCount`：`med_case`
- `analyzedCaseCount`：`ana_task_record` 中成功任务覆盖到的不同病例数
- `generatedReportCount`：`rpt_record` 的病例数
- `followupRequiredCaseCount`：病例状态为 `FOLLOWUP_REQUIRED` 或标记位为 `1`
- `closedCaseCount`：病例状态为 `CLOSED`

### 3.3 case-status-distribution

直接统计 `med_case.case_status_code` 八种状态。

### 3.4 risk-level-distribution

按每个病例最新一条 `med_risk_assessment_record` 聚合。

### 3.5 followup-task-summary

基于 `fup_task.task_status_code` 统计：

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`

并计算完成率与逾期率。

### 3.6 backlog-summary

当前定义为：

- 待医生复核病例数
- 待处理随访任务数
- 逾期随访任务数
- 未关闭高风险病例数

### 3.7 trend

按日期序列统计：

- 新建病例数
- 分析完成数
- 报告生成数
- 随访触发数

### 3.8 model-runtime

最近 30 天：

- 当前模型版本
- 任务总数
- 成功数
- 失败数
- 成功率

## 4. 当前实现特点

- 不建 dashboard 快照表
- 不做跨机构聚合
- 直接用 `JdbcTemplate` 写 SQL
- 面向当前数据量是可接受的

## 5. 当前限制

- 没有部门/医生下钻
- 没有缓存
- 没有实时推送
- 大数据量下需考虑宽表或物化策略
