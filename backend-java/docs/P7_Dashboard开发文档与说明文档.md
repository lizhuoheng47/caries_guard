# P7 Dashboard 开发文档与说明文档

## 1. 当前模块结论

dashboard 模块已经实现首批统计能力，并且已经有 boot 真库集成测试支撑。

## 2. 技术实现

- 控制器：`DashboardController`、`DashboardOpsController`
- 应用服务：overview / case stats / risk stats / followup stats / backlog / trend / ops metrics
- 基础设施：`DashboardStatsRepository`
- 实现方式：`JdbcTemplate` 直接查询业务表

## 3. 已实现统计项

- 总览统计
- 病例状态分布
- 风险等级分布
- 随访任务汇总
- 积压工作汇总
- 模型运行情况
- 趋势统计

## 4. 当前优点

- 口径清晰，SQL 可直接核查
- 与业务表强一致，不依赖异步同步表
- 对答辩和演示很友好

## 5. 当前缺点

- 性能与业务表耦合
- 无缓存层
- 无历史快照
- 无多维钻取

## 6. 当前测试

boot 中已有对应集成测试：

- `DashboardOverviewIntegrationTest`
- `DashboardCaseStatusDistributionIntegrationTest`
- `DashboardRiskLevelDistributionIntegrationTest`
- `DashboardFollowupTaskSummaryIntegrationTest`
- `DashboardBacklogIntegrationTest`
- `DashboardModelRuntimeIntegrationTest`
- `DashboardTrendIntegrationTest`
