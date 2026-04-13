# P7 Dashboard 国一版开发文档与说明文档

## 一、结论先行

当前阶段 **可以进入 P7 dashboard**，但口径应当明确为：

- **P6 已完成主链路、边界测试、boot 真库 E2E 与验收证据化，已达到“可收口”状态**
- **P7 现在可以进入实施**
- **P6 不再是阻塞项，但仍建议保留一条“文档与迁移口径最终一致性复核”作为收尾动作，而不是继续扩展功能**

这一路径与当前开发计划一致：现状已经写明 `P6 followup` 已进入实施并完成主链路、边界测试与 boot 真实联测，剩余工作是 **P6 文档收口与 P7 dashboard 进入实施**；同时“Next Implementation Step”也明确要求 **完成 P6 验收后进入 P7 dashboard**。因此，从工程节奏和答辩节奏看，当前切入 P7 是正确选择。  
同时，P7 在总计划中的职责是：提供 **statistics aggregation、backlog monitoring、health and operations metrics、model version query**，目标是支撑管理演示与运维观察。  
而 P6 验收更新文档也已经证明，followup 的 in-memory 与 boot 真库双轨证据、状态机链路、审计与通知留痕已经成型，P7 不需要再等一个新的业务模块稳定后才能开始。  

---

## 二、P6 是否还有需要改进的地方

### 2.1 结论

**有，但已经不属于“阻塞 P7 开发”的改进，而属于“P6 冻结前的最后清理项”。**

### 2.2 P6 当前状态判断

从当前文档看，P6 已具备以下完成度：

1. `fup_plan / fup_task / fup_record` 的 domain + app + controller 已存在。
2. `FollowupTriggerService` 已统一承接触发入口。
3. report 生成路径已能根据高风险或复核建议自动触发 followup。
4. in-memory 边界测试已补齐。
5. boot 真库跨模块 E2E 与边界测试已补齐。
6. 状态机与通知留痕证据已经写回阶段文档。

因此，P6 已经不是“未完成模块”，而是“已完成主链路并进入收尾状态的模块”。

### 2.3 P6 还建议保留的最后改进项

#### （1）Flyway 与字段口径做一次最终复核
虽然文档中已写“finish schema migration alignment for evolved followup fields”属于剩余项，但这更像一致性复核，而不是新增开发。  
建议检查：

- `fup_plan / fup_task / fup_record` 是否与当前最新字段定义完全一致
- 触发来源字段、计划状态字段、任务状态字段是否与测试断言一致
- `msg_notify_record` 相关枚举是否与 dashboard 后续统计口径一致

#### （2）P6 对外展示字段再做一次“答辩可视化复核”
后续 P7 看板会直接消费 P6 数据，所以要提前确认：

- 哪些字段能直接用于统计
- 哪些字段仍需转义或枚举映射
- 哪些状态适合在前端直接展示
- 哪些字段只应作为内部审计字段使用

#### （3）为 P7 预留统计友好索引与查询口径
P7 上来就会做：

- 随访任务待办统计
- 逾期任务统计
- 病例状态分布
- 高风险病例分布
- 医生/机构维度统计

所以建议提前确认 P6 相关表是否已具备 dashboard 友好的筛选字段与索引组合。

### 2.4 结论重申

**P6 还可以精修，但无需继续扩展功能。现在最合理的策略是：P6 收口、P7 开工。**

---

## 三、为什么现在进入 P7 是正确动作

### 3.1 从工程顺序看，P7 正好承接 P6

当前总计划的推荐顺序仍然是：

1. P0 baseline
2. P1 system
3. P2 patient + case
4. P3 image
5. P4 analysis
6. P5 report
7. P6 follow-up
8. P7 dashboard
9. P8 testing and delivery

而最新计划状态已经明确：  
`P6 followup` 的主要剩余工作是文档收口，`P7 dashboard` 是下一阶段的主要缺口。  
因此，当前进入 P7 完全符合整体研发路线。

### 3.2 从答辩叙事看，P7 是把“闭环”变成“可展示平台”
P6 解决的是：  
**系统不只会分析和出报告，还会跟进高风险病例。**

P7 解决的是：  
**系统不只是有业务闭环，还能让管理者、评委、演示现场直接看到闭环运行效果。**

也就是说：

- P6 补“闭环能力”
- P7 补“平台可观测与管理展示能力”

这两个模块天然连在一起。

### 3.3 从比赛视角看，P7 是管理端展示的核心舞台
答辩时，评委最容易被打动的不是某张表做了多少字段，而是：

- 当前有多少待复核病例
- 高风险病例占比多少
- 随访完成率如何
- 逾期任务有多少
- 模型版本是什么
- 最近分析成功率如何

这些都属于 dashboard 的直接展示价值。  
所以 P7 不只是“加一个看板模块”，而是把整个项目从“后端能力存在”变成“前台能展示、管理能解释”。

---

## 四、P7 的模块定位

## 4.1 模块目标

P7 Dashboard 的目标应冻结为：

> 为管理端、演示端和运维观察提供统一统计聚合与监控接口，支撑病例业务闭环展示、待办压测、风险分布分析和模型运行状态查询。

### 4.2 模块边界

P7 不做以下事情：

- 不新增核心业务主表
- 不重复存储大批统计快照表（除非后续性能逼迫）
- 不篡改 analysis / report / followup 的业务状态
- 不承担真正的 BI 平台职责
- 不直接承担模型训练治理

P7 只做：

- 聚合查询
- 统计结果编排
- 管理端和演示端接口输出
- 必要缓存与分页

### 4.3 模块核心职责

建议冻结四类职责：

1. **业务统计聚合**
2. **待办与积压监控**
3. **运维健康指标**
4. **模型版本与运行信息展示**

这与当前开发计划对 P7 的定义完全一致。

---

## 五、P7 的数据口径必须先冻结

你提到“先做 dashboard 的数据口径冻结和首批统计接口”，这个判断是正确的。  
而且我认为：**P7 第一优先级不是写接口，而是先冻结统计口径。**

因为 dashboard 最容易翻车的地方不是代码，而是“同一个指标大家解释不一致”。

所以必须先冻结以下内容。

### 5.1 统计时间口径

建议统一支持以下时间维度：

- 今日
- 近 7 天
- 近 30 天
- 自定义时间范围

并明确所有统计默认使用：

- `org_id` 进行机构隔离
- `created_at` 或业务主时间字段作为时间筛选基础
- 统计时间采用服务端统一时区

### 5.2 组织口径

所有 dashboard 统计必须明确：

- 是否默认按当前用户所属机构过滤
- 是否允许管理员查看全机构汇总
- 是否支持按部门 / 医生 / 角色维度下钻

### 5.3 状态口径

必须冻结以下状态如何参与统计：

#### 病例状态
- `CREATED`
- `QC_PENDING`
- `ANALYZING`
- `REVIEW_PENDING`
- `REPORT_READY`
- `FOLLOWUP_REQUIRED`
- `CLOSED`
- `CANCELLED`

#### 随访任务状态
- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`
- `CANCELLED`

#### 报告统计口径
需明确：
- 是统计 `rpt_record` 数量
- 还是统计“已生成报告的病例数”
- 是否区分 DOCTOR / PATIENT 报告类型

### 5.4 去重口径

必须提前定义：

- 统计病例数时按 `case_id` 去重
- 统计任务数时按 `task_id` 统计
- 统计患者数时按 `patient_id` 去重
- 统计高风险病例时，是按最新风险记录还是所有历史记录统计

### 5.5 风险口径

高风险、中风险、低风险必须明确来自哪个字段：

- 建议统一从 `med_risk_assessment_record` 的最新有效记录统计
- 若同一病例多次评估，dashboard 默认按最新一条统计
- 如需趋势图，再做历史序列统计

### 5.6 Followup 口径

必须明确：

- 随访计划数统计 `fup_plan`
- 待办数统计 `fup_task` 中 `TODO + IN_PROGRESS`
- 逾期数统计 `fup_task.OVERDUE`
- 完成率 = `DONE / (DONE + OVERDUE + TODO + IN_PROGRESS)` 还是其他公式

建议在文档中固定一版，避免前后端、答辩口径不一致。

---

## 六、P7 首批接口建议范围

P7 不宜一开始铺得太大。  
建议先做 **“国一答辩最有价值的首批接口”**。

## 6.1 第一批必须做的统计接口

### 1. 首页总览统计接口
建议接口：

`GET /api/v1/dashboard/overview`

返回示例：

- 总患者数
- 总病例数
- 已分析病例数
- 已生成报告数
- 需要随访病例数
- 已关闭病例数

### 2. 病例状态分布接口
建议接口：

`GET /api/v1/dashboard/case-status-distribution`

返回：

- CREATED 数
- ANALYZING 数
- REVIEW_PENDING 数
- REPORT_READY 数
- FOLLOWUP_REQUIRED 数
- CLOSED 数

### 3. 风险等级分布接口
建议接口：

`GET /api/v1/dashboard/risk-level-distribution`

返回：

- HIGH 数
- MEDIUM 数
- LOW 数

### 4. 随访任务看板接口
建议接口：

`GET /api/v1/dashboard/followup-task-summary`

返回：

- TODO 数
- IN_PROGRESS 数
- DONE 数
- OVERDUE 数
- 完成率
- 逾期率

### 5. 近期趋势接口
建议接口：

`GET /api/v1/dashboard/trend`

可支持：

- 最近 7/30 天新增病例数
- 最近 7/30 天分析完成数
- 最近 7/30 天报告生成数
- 最近 7/30 天随访触发数

### 6. 待办/积压接口
建议接口：

`GET /api/v1/dashboard/backlog-summary`

返回：

- 待复核病例数
- 待随访任务数
- 逾期任务数
- 最近待处理高风险病例数

### 7. 模型版本与运行信息接口
建议接口：

`GET /api/v1/dashboard/model-runtime`

返回：

- 当前模型版本
- 最近分析任务数
- 成功任务数
- 失败任务数
- 成功率

---

## 七、P7 技术实现策略

## 7.1 分层建议

模块内建议仍按统一分层：

- `controller`
- `app`
- `domain`
- `infrastructure`
- `interfaces`

但 P7 的重点不是复杂领域模型，而是 **查询编排与聚合输出**。

### 建议核心类

#### AppService
- `DashboardOverviewAppService`
- `DashboardCaseStatsAppService`
- `DashboardFollowupStatsAppService`
- `DashboardOpsMetricsAppService`

#### QueryService / Repository
- `DashboardStatsRepository`
- `DashboardTrendRepository`
- `DashboardOpsRepository`

#### Controller
- `DashboardController`
- `DashboardOpsController`

#### Query / VO
- `DashboardRangeQuery`
- `DashboardOverviewVO`
- `CaseStatusDistributionVO`
- `RiskLevelDistributionVO`
- `FollowupTaskSummaryVO`
- `DashboardTrendPointVO`
- `ModelRuntimeVO`

## 7.2 查询实现原则

### 原则 1：优先聚合查询，不新增统计表
一期建议直接基于业务表做 SQL 聚合：

- `med_case`
- `ana_task_record`
- `rpt_record`
- `fup_plan`
- `fup_task`
- `med_risk_assessment_record`
- `msg_notify_record`

### 原则 2：必要时加缓存，不先上离线汇总表
若首页概览查询过重，可考虑：

- Redis 短缓存
- 只缓存机构级概览
- 缓存时长 30 秒 ~ 5 分钟

### 原则 3：先保证口径正确，再追求炫酷图表
dashboard 最重要的是“解释得清楚”，不是接口数量多。

## 7.3 索引与性能建议

建议重点关注以下筛选列：

- `org_id`
- `created_at`
- `case_status_code`
- `task_status_code`
- `overall_risk_level_code`
- `report_type_code`
- `task_status_code + due_date`

如果某些 dashboard 查询明显走全表扫描，再补组合索引。

---

## 八、P7 详细开发顺序

## 阶段 1：冻结数据口径与指标定义

交付物：

- 指标定义表
- 状态口径表
- 时间口径表
- 去重规则表
- dashboard API 草案

必须先写清楚：

1. 每个指标来自哪张表
2. 每个指标按什么字段过滤
3. 每个指标按什么字段去重
4. 每个指标默认展示哪个时间范围
5. 哪些指标支持按机构/部门/医生筛选

## 阶段 2：完成首批聚合 Repository 与 Query 对象

交付物：

- `DashboardStatsRepository`
- `DashboardTrendRepository`
- `DashboardRangeQuery`
- 首批 SQL / Mapper XML

目标：

- 不先写前端页面
- 先把聚合结果查准

## 阶段 3：完成首批 AppService 与 Controller

交付物：

- overview
- case-status-distribution
- risk-level-distribution
- followup-task-summary
- backlog-summary
- model-runtime

目标：

- 形成可供管理端直接联调的接口集合

## 阶段 4：补齐 P7 集成测试

至少补：

- `DashboardOverviewIntegrationTest`
- `DashboardFollowupSummaryIntegrationTest`
- `DashboardRiskDistributionIntegrationTest`
- `DashboardOpsMetricsIntegrationTest`

建议先 in-memory 固化口径，再进 boot 真库。

## 阶段 5：补 P7 文档与答辩说明

必须写回：

- 每个接口统计的来源表
- 每个指标的口径解释
- 已覆盖与未覆盖边界
- 管理演示如何使用这些接口讲故事

---

## 九、P7 推荐接口清单（可直接下发给队员）

## 9.1 管理总览

### `GET /api/v1/dashboard/overview`
说明：首页总览卡片

字段建议：
- patientCount
- caseCount
- analyzedCaseCount
- generatedReportCount
- followupRequiredCaseCount
- closedCaseCount

---

## 9.2 病例状态分布

### `GET /api/v1/dashboard/case-status-distribution`
说明：病例全流程分布

字段建议：
- createdCount
- analyzingCount
- reviewPendingCount
- reportReadyCount
- followupRequiredCount
- closedCount
- cancelledCount

---

## 9.3 风险等级分布

### `GET /api/v1/dashboard/risk-level-distribution`
说明：高/中/低风险分布

字段建议：
- highRiskCount
- mediumRiskCount
- lowRiskCount

---

## 9.4 随访任务统计

### `GET /api/v1/dashboard/followup-task-summary`
说明：随访待办与完成统计

字段建议：
- todoCount
- inProgressCount
- doneCount
- overdueCount
- completionRate
- overdueRate

---

## 9.5 近期趋势

### `GET /api/v1/dashboard/trend`
说明：按天返回近期业务趋势

字段建议：
- date
- newCaseCount
- analysisCompletedCount
- reportGeneratedCount
- followupTriggeredCount

---

## 9.6 待办看板

### `GET /api/v1/dashboard/backlog-summary`
说明：待复核、待随访、逾期等积压项

字段建议：
- reviewPendingCaseCount
- todoFollowupTaskCount
- overdueFollowupTaskCount
- highRiskPendingCaseCount

---

## 9.7 模型运行信息

### `GET /api/v1/dashboard/model-runtime`
说明：模型版本与近期运行状态

字段建议：
- currentModelVersion
- recentTaskCount
- successTaskCount
- failedTaskCount
- successRate

---

## 十、P7 测试建议

## 10.1 必测场景

### 场景 1：总览统计正确
断言：
- 患者数、病例数、报告数按预期聚合
- 机构隔离条件生效

### 场景 2：病例状态分布正确
断言：
- `FOLLOWUP_REQUIRED` 能被正确统计
- `CANCELLED` 不误并入其他状态

### 场景 3：风险分布取最新记录
断言：
- 同一病例多条风险记录时，默认取最新有效记录

### 场景 4：随访任务完成率计算正确
断言：
- 分母分子口径固定
- `OVERDUE` 不被误算成完成

### 场景 5：模型运行指标正确
断言：
- success / failed / total 数量一致
- successRate 公式正确

## 10.2 建议测试类

- `DashboardOverviewIntegrationTest`
- `DashboardCaseStatusDistributionIntegrationTest`
- `DashboardRiskLevelDistributionIntegrationTest`
- `DashboardFollowupTaskSummaryIntegrationTest`
- `DashboardBacklogIntegrationTest`
- `DashboardModelRuntimeIntegrationTest`

boot 真库阶段再补：

- `com.cariesguard.boot.dashboard.DashboardOverviewIntegrationTest`
- `com.cariesguard.boot.dashboard.DashboardFollowupTaskSummaryIntegrationTest`
- `com.cariesguard.boot.dashboard.DashboardModelRuntimeIntegrationTest`

---

## 十一、P7 文档说明写法建议

后续说明文档不要写成“我们做了几个统计接口”。  
建议写成下面这个口径：

> P7 dashboard 的目标是把平台已有的 patient、case、analysis、report、followup、audit 数据统一汇总为管理可视化指标，使系统不仅具备业务闭环能力，还具备过程可观测、结果可解释、积压可发现的管理展示能力。

这句话有三个好处：

1. 能把 P7 和前面所有模块串起来
2. 能解释为什么 dashboard 不是附属页面
3. 能突出平台级价值

---

## 十二、给你的最终建议

### 12.1 是否可以直接进入 P7
**可以。**

### 12.2 P6 是否还有改进空间
**有，但属于收口优化，不再阻塞 P7。**

### 12.3 P7 现在最正确的起手动作
不是先画页面，而是：

1. 冻结数据口径
2. 冻结首批统计指标
3. 先做聚合接口
4. 再做管理端联调
5. 最后补测试与答辩文档

---

## 十三、可直接执行的最终版开发建议

### P7 Dashboard 实施顺序

1. 先冻结 dashboard 数据口径  
   明确时间维度、组织维度、状态口径、去重规则、风险统计口径与 followup 完成率口径。

2. 实现首批统计聚合 Repository 与 Query 对象  
   优先完成 overview、病例状态分布、风险等级分布、随访任务统计、待办统计、模型运行信息的聚合查询。

3. 实现首批 AppService 与 Controller  
   形成管理端可直接联调的统计接口集，不先追求复杂页面，先确保接口口径稳定。

4. 补齐 dashboard 集成测试  
   先验证总览、状态分布、风险统计、随访统计、模型运行指标的准确性，再进入 boot 真库验证。

5. 完成 P7 文档与答辩说明  
   将每个指标的来源表、去重规则、状态解释、边界声明写回文档，形成可答辩的口径资产。

---

## 十四、结语

P7 不是“最后补个看板”，而是把你们已经做出的 patient、case、analysis、report、followup 全链路能力，统一转译成评委和管理端一眼就能看懂的统计证据。

从比赛视角看，P6 让你们变成“闭环平台”，  
P7 则让这个闭环平台“可展示、可观察、可解释”。

这一步现在进入，时机是对的。
