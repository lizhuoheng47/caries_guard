# P7 Dashboard 开发文档与说明文档

## 1. 模块结论

P7 dashboard 已从规划态进入实现完成态。

当前已经完成：

- dashboard 首批 7 个聚合接口
- 按机构隔离的统一统计口径
- boot 真库集成测试
- 文档口径冻结

当前未做：

- 跨机构总览
- 部门或医生维度下钻
- 实时推送大屏
- 离线宽表和缓存优化

## 2. 模块定位

P7 的目标不是新增业务流程，而是把现有 `patient`、`case`、`analysis`、`report`、`followup` 数据转成可展示、可解释、可答辩的管理指标。

P7 只负责：

- 聚合查询
- 统计结果编排
- 管理端与答辩端输出

P7 不负责：

- 改写病例、报告、随访主业务状态
- 新增业务核心表
- 承担 BI 平台或训练平台职责

## 3. 当前代码落地

### 3.1 Controller

- `DashboardController`
- `DashboardOpsController`

### 3.2 AppService

- `DashboardOverviewAppService`
- `DashboardCaseStatsAppService`
- `DashboardRiskStatsAppService`
- `DashboardFollowupStatsAppService`
- `DashboardBacklogAppService`
- `DashboardOpsMetricsAppService`
- `DashboardTrendAppService`

### 3.3 Repository

- `DashboardStatsRepository`

### 3.4 Query / VO

- `DashboardRangeQuery`
- `DashboardOverviewVO`
- `CaseStatusDistributionVO`
- `RiskLevelDistributionVO`
- `FollowupTaskSummaryVO`
- `BacklogSummaryVO`
- `ModelRuntimeVO`
- `DashboardTrendPointVO`

## 4. 接口实现清单

### 4.1 已完成接口

1. `GET /api/v1/dashboard/overview`
2. `GET /api/v1/dashboard/case-status-distribution`
3. `GET /api/v1/dashboard/risk-level-distribution`
4. `GET /api/v1/dashboard/followup-task-summary`
5. `GET /api/v1/dashboard/backlog-summary`
6. `GET /api/v1/dashboard/model-runtime`
7. `GET /api/v1/dashboard/trend`

### 4.2 权限要求

- 普通 dashboard 接口要求 `dashboard:view`
- 运营接口 `model-runtime` 要求 `dashboard:ops:view`

## 5. 实现原则

### 5.1 直接基于业务表聚合

当前所有统计直接读取：

- `pat_patient`
- `med_case`
- `ana_task_record`
- `med_risk_assessment_record`
- `rpt_record`
- `fup_plan`
- `fup_task`

### 5.2 不新增统计快照表

当前阶段先保证口径正确，不引入额外快照表、宽表、离线任务。

### 5.3 先保证可解释，再考虑性能优化

当前实现优先保证：

- 统计定义清晰
- 结果可复现
- 答辩可解释

后续如首页压力增大，再考虑：

- Redis 短缓存
- 预聚合
- 补充索引

## 6. 已冻结的关键统计规则

### 6.1 风险分布

- 每个病例仅取最新一条有效风险记录
- 使用 `med_risk_assessment_record` 的 `MAX(id)` 作为当前记录

### 6.2 Followup 完成率与逾期率

- 分母固定为 `TODO + IN_PROGRESS + DONE + OVERDUE`
- `CANCELLED` 不进入分母

### 6.3 高风险待处理病例

- 仅统计最新风险为 `HIGH`
- 且病例状态不在 `CLOSED`、`CANCELLED`

### 6.4 模型运行指标

- 最近窗口固定 30 天
- 当前模型版本取最近成功任务的 `model_version`

### 6.5 趋势图

- 支持 `TODAY`、`LAST_7_DAYS`、`LAST_30_DAYS`、`CUSTOM`
- 缺失日期按 0 补齐
- 当前按记录数聚合，不做病例去重

## 7. 测试情况

已通过的 boot 真库测试：

- `DashboardOverviewIntegrationTest`
- `DashboardCaseStatusDistributionIntegrationTest`
- `DashboardRiskLevelDistributionIntegrationTest`
- `DashboardFollowupTaskSummaryIntegrationTest`
- `DashboardBacklogIntegrationTest`
- `DashboardModelRuntimeIntegrationTest`
- `DashboardTrendIntegrationTest`

这些测试已经覆盖：

- 冻结状态统计
- 最新风险记录口径
- Followup 比率公式
- 高风险未关闭病例口径
- 最近 30 天模型运行统计
- 趋势图按天补零

## 8. 未覆盖边界声明

- 当前未接入 dashboard 前端页面联调截图
- 当前未补 dashboard 的跨机构管理视角
- 当前未实现缓存和离线统计宽表
- 当前未实现实时消息推送或大屏刷新

这些都属于增强项，不阻塞 P7 交付口径。

## 9. 下一步建议

P7 后续最合理的顺序是：

1. 前端联调 dashboard 页面
2. 将测试断言点回写答辩文档
3. 进入 P8 测试与交付收口
