# P7 Dashboard 数据口径冻结表与首批接口清单（执行版）

## 一、文档用途

本文档不是答辩总述，而是 **P7 dashboard 直接执行版**。  
目标是给后端、前端、测试三类成员一份统一口径，避免出现：

- 同一指标不同人理解不一致
- 接口先写了，统计口径后补，导致返工
- 前端图表已经画了，后端字段又改
- 答辩时被问“这个数字怎么算的”说不清楚

本文件重点解决三件事：

1. 冻结 P7 首批指标口径  
2. 冻结 P7 首批接口清单  
3. 给出可以直接执行的开发顺序和测试清单  

---

## 二、执行结论

P7 当前可以直接进入实施。  
实施优先级不是页面，而是：

1. **先冻结数据口径**
2. **再写聚合查询**
3. **再暴露接口**
4. **再联调页面**
5. **最后补测试和文档回写**

---

## 三、P7 首批统计范围（冻结）

一期只做以下 7 类统计，不扩散：

1. 总览卡片统计
2. 病例状态分布
3. 风险等级分布
4. 随访任务统计
5. 近期趋势统计
6. 待办/积压统计
7. 模型运行信息

这 7 类已经足够支撑：

- 管理端首页展示
- 答辩演示主屏
- 平台闭环证明
- 运维观察基础能力

---

## 四、统一基础口径（必须冻结）

## 4.1 时间口径

默认支持四种时间维度：

- 今日
- 近 7 天
- 近 30 天
- 自定义时间范围

默认规则：

- 所有统计默认按服务端统一时区处理
- 没有指定时间范围时，overview 类接口默认取“全量累计”
- trend 类接口默认取“近 7 天”
- backlog 类接口默认取“当前时点快照”

---

## 4.2 组织口径

默认规则：

- 普通用户只看自己 `org_id` 范围内的数据
- 管理员可看本机构全量汇总
- 一期默认不开放跨机构全局汇总接口
- 是否按部门 / 医生维度下钻作为二期增强项预留

---

## 4.3 去重口径

必须统一：

- 患者数：按 `patient_id` 去重
- 病例数：按 `case_id` 去重
- 报告数：按 `report_id` 统计，若展示“已出报告病例数”则按 `case_id` 去重
- 随访计划数：按 `plan_id` 统计
- 随访任务数：按 `task_id` 统计
- 高风险病例数：按“病例最新有效风险记录”对应的 `case_id` 去重

---

## 4.4 状态口径

### 病例主状态

统一使用：

- `CREATED`
- `QC_PENDING`
- `ANALYZING`
- `REVIEW_PENDING`
- `REPORT_READY`
- `FOLLOWUP_REQUIRED`
- `CLOSED`
- `CANCELLED`

### 随访任务状态

统一使用：

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`
- `CANCELLED`

### AI 任务状态

统一使用：

- `QUEUEING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`

---

## 4.5 风险口径

高/中/低风险统计统一来自：

- `med_risk_assessment_record`

默认规则：

1. 同一病例若存在多条风险记录，dashboard 默认只取**最新一条有效记录**
2. 风险分布默认统计“当前病例风险状态”，不统计历史累计次数
3. 趋势统计若需要历史变化，后续单独扩展

---

## 4.6 Followup 口径

统一规则：

- 随访计划数：统计 `fup_plan`
- 随访待办数：统计 `fup_task` 中 `TODO + IN_PROGRESS`
- 随访逾期数：统计 `fup_task` 中 `OVERDUE`
- 随访完成数：统计 `fup_task` 中 `DONE`

### 完成率公式（冻结）
`completionRate = DONE / (TODO + IN_PROGRESS + DONE + OVERDUE)`

### 逾期率公式（冻结）
`overdueRate = OVERDUE / (TODO + IN_PROGRESS + DONE + OVERDUE)`

说明：
- `CANCELLED` 不纳入完成率和逾期率分母
- 一期只统计任务完成率，不统计计划完成率

---

## 五、首批接口口径冻结表

## 5.1 总览接口

### 接口
`GET /api/v1/dashboard/overview`

### 用途
首页顶部卡片，总体反映平台运行规模。

### 返回字段（冻结）

- `patientCount`：患者总数
- `caseCount`：病例总数
- `analyzedCaseCount`：已完成分析的病例数
- `generatedReportCount`：已生成报告的病例数
- `followupRequiredCaseCount`：需要随访的病例数
- `closedCaseCount`：已关闭病例数

### 数据来源建议

- 患者总数：`pat_patient`
- 病例总数：`med_case`
- 已完成分析病例数：`ana_task_record` 中成功任务关联病例去重
- 已生成报告病例数：`rpt_record` 关联病例去重
- 需要随访病例数：`med_case.case_status_code = FOLLOWUP_REQUIRED` 或 `followup_required_flag = 1`
- 已关闭病例数：`med_case.case_status_code = CLOSED`

---

## 5.2 病例状态分布接口

### 接口
`GET /api/v1/dashboard/case-status-distribution`

### 用途
展示病例主流程当前处于什么阶段。

### 返回字段（冻结）

- `createdCount`
- `qcPendingCount`
- `analyzingCount`
- `reviewPendingCount`
- `reportReadyCount`
- `followupRequiredCount`
- `closedCount`
- `cancelledCount`

### 数据来源
- `med_case`

### 统计规则
- 每个病例只归入当前一个主状态
- 使用 `med_case.case_status_code`
- 不根据状态日志回推

---

## 5.3 风险等级分布接口

### 接口
`GET /api/v1/dashboard/risk-level-distribution`

### 用途
展示当前病例风险结构。

### 返回字段（冻结）

- `highRiskCount`
- `mediumRiskCount`
- `lowRiskCount`

### 数据来源
- `med_risk_assessment_record`

### 统计规则
- 每个病例取最新一条有效风险记录
- 统计的是病例数量，不是风险记录数量

---

## 5.4 随访任务统计接口

### 接口
`GET /api/v1/dashboard/followup-task-summary`

### 用途
展示 followup 执行情况。

### 返回字段（冻结）

- `todoCount`
- `inProgressCount`
- `doneCount`
- `overdueCount`
- `completionRate`
- `overdueRate`

### 数据来源
- `fup_task`

### 统计规则
- 仅统计有效任务
- `CANCELLED` 不纳入完成率和逾期率分母

---

## 5.5 近期趋势接口

### 接口
`GET /api/v1/dashboard/trend`

### 用途
展示最近一段时间平台业务活跃度。

### 返回字段（冻结）

每个点返回：

- `date`
- `newCaseCount`
- `analysisCompletedCount`
- `reportGeneratedCount`
- `followupTriggeredCount`

### 数据来源建议

- 新增病例：`med_case.created_at`
- 分析完成：`ana_task_record.completed_at` 且状态为 `SUCCESS`
- 报告生成：`rpt_record.created_at`
- 随访触发：`fup_plan.created_at`

### 统计规则
- 按天聚合
- 默认近 7 天
- 时间轴缺失日期要补零

---

## 5.6 待办/积压接口

### 接口
`GET /api/v1/dashboard/backlog-summary`

### 用途
展示当前平台最需要处理的积压项。

### 返回字段（冻结）

- `reviewPendingCaseCount`
- `todoFollowupTaskCount`
- `overdueFollowupTaskCount`
- `highRiskPendingCaseCount`

### 数据来源建议

- 待复核病例：`med_case.case_status_code = REVIEW_PENDING`
- 待随访任务：`fup_task.task_status_code in (TODO, IN_PROGRESS)`
- 逾期任务：`fup_task.task_status_code = OVERDUE`
- 高风险待处理病例：高风险病例中仍未关闭的病例

---

## 5.7 模型运行信息接口

### 接口
`GET /api/v1/dashboard/model-runtime`

### 用途
展示 AI 运行状态与当前模型版本。

### 返回字段（冻结）

- `currentModelVersion`
- `recentTaskCount`
- `successTaskCount`
- `failedTaskCount`
- `successRate`

### 数据来源建议

- 模型版本：`ana_task_record.model_version` 中最近成功任务对应版本，或后续独立配置
- 近期任务总数：`ana_task_record`
- 成功任务数：`task_status_code = SUCCESS`
- 失败任务数：`task_status_code = FAILED`

### 公式（冻结）
`successRate = successTaskCount / recentTaskCount`

---

## 六、首批接口清单（可直接排期）

## 6.1 第一批必须开发

1. `/api/v1/dashboard/overview`
2. `/api/v1/dashboard/case-status-distribution`
3. `/api/v1/dashboard/risk-level-distribution`
4. `/api/v1/dashboard/followup-task-summary`
5. `/api/v1/dashboard/backlog-summary`
6. `/api/v1/dashboard/model-runtime`

## 6.2 第二批再开发

7. `/api/v1/dashboard/trend`

原因：
- trend 需要按天聚合、补零、前端展示适配，复杂度略高
- 第一批先把静态统计和总览跑通，最稳

---

## 七、建议类清单（后端执行版）

## 7.1 Controller

- `DashboardController`
- `DashboardOpsController`

## 7.2 AppService

- `DashboardOverviewAppService`
- `DashboardCaseStatsAppService`
- `DashboardRiskStatsAppService`
- `DashboardFollowupStatsAppService`
- `DashboardBacklogAppService`
- `DashboardOpsMetricsAppService`

## 7.3 Repository / Query

- `DashboardStatsRepository`
- `DashboardTrendRepository`
- `DashboardOpsRepository`

## 7.4 Query / VO

- `DashboardRangeQuery`
- `DashboardOverviewVO`
- `CaseStatusDistributionVO`
- `RiskLevelDistributionVO`
- `FollowupTaskSummaryVO`
- `BacklogSummaryVO`
- `ModelRuntimeVO`
- `DashboardTrendPointVO`

---

## 八、开发顺序（直接执行版）

## 第一步：冻结口径
负责人：后端 + 队长  
产物：

- 本文档确认版
- 指标字段清单
- 公式清单
- 数据来源表清单

目标：
- 不允许接口开发过程中再临时改口径

---

## 第二步：先做 Repository 聚合查询
负责人：后端  
产物：

- 首批 Mapper / SQL
- 能直接在测试里断言的聚合结果

目标：
- 先查准，再封装接口

---

## 第三步：完成第一批接口
负责人：后端  
产物：

- overview
- case-status-distribution
- risk-level-distribution
- followup-task-summary
- backlog-summary
- model-runtime

目标：
- 管理端可以先联调静态看板

---

## 第四步：完成 trend 接口
负责人：后端  
产物：

- trend 接口
- 按天补零逻辑
- 时间范围参数支持

---

## 第五步：补齐测试
负责人：后端 + 测试  
产物：

- `DashboardOverviewIntegrationTest`
- `DashboardCaseStatusDistributionIntegrationTest`
- `DashboardRiskLevelDistributionIntegrationTest`
- `DashboardFollowupTaskSummaryIntegrationTest`
- `DashboardBacklogIntegrationTest`
- `DashboardModelRuntimeIntegrationTest`

建议：
- 先 in-memory 固化口径
- 再进 boot 真库验证

---

## 第六步：前端联调
负责人：前端  
产物：

- 管理端首页卡片
- 状态分布图
- 风险分布图
- 随访统计图
- 积压提示区域

目标：
- 不追求炫技，先把数字准确展示出来

---

## 九、测试断言清单（冻结）

## 9.1 overview 必测
- 患者数统计正确
- 病例数统计正确
- 报告病例数按病例去重正确
- 随访病例数能识别 `FOLLOWUP_REQUIRED`

## 9.2 case-status-distribution 必测
- 每个状态数准确
- `CANCELLED` 不误计入其他状态
- 总和应等于病例总数

## 9.3 risk-level-distribution 必测
- 同一病例多条风险记录时取最新一条
- 高/中/低风险统计互斥

## 9.4 followup-task-summary 必测
- TODO / IN_PROGRESS / DONE / OVERDUE 数准确
- `CANCELLED` 不进入完成率分母
- 完成率、逾期率公式正确

## 9.5 backlog-summary 必测
- 待复核病例数准确
- 待随访任务数准确
- 逾期任务数准确
- 高风险待处理病例不应误计已关闭病例

## 9.6 model-runtime 必测
- recentTaskCount = successTaskCount + failedTaskCount + 其他近期状态数
- successRate 公式准确
- 当前模型版本字段有稳定来源

---

## 十、前端联调注意事项

1. 前端不要自行计算完成率和逾期率，直接以后端返回为准  
2. 前端不要自己做状态中文映射以外的业务判断  
3. 趋势图默认展示近 7 天  
4. overview 卡片优先展示累计值，不要默认展示今日值  
5. followup 相关图表颜色要能明显区分 `TODO / DONE / OVERDUE`

---

## 十一、当前阶段不做的内容（冻结）

一期不做：

- 医生维度排行榜
- 部门维度下钻
- 跨机构汇总
- 大屏实时推送
- 离线统计宽表
- 复杂运维图谱
- 模型训练治理看板

这些全部放后续增强，不在 P7 首批范围内。

---

## 十二、可直接发给队员的执行口令

### 后端
先冻结 dashboard 数据口径，优先实现 overview、病例状态分布、风险等级分布、随访任务统计、待办积压、模型运行信息六个接口，trend 放第二批。先做聚合查询，再做 controller，不要先写前端。

### 前端
先按六个固定接口预留页面区域，不自己推导业务公式，所有比率和统计数字以后端返回为准。第一版先做“准确展示”，不追求复杂交互。

### 测试
围绕 overview、状态分布、风险分布、followup 统计、backlog、模型运行六类接口写断言，重点盯“最新风险记录”“完成率公式”“FOLLOWUP_REQUIRED 统计”“CANCELLED 是否误入分母”。

---

## 十三、最终说明

P7 第一阶段不是做一个好看的页面，而是把系统现有 patient、case、analysis、report、followup 这些真实业务数据，转成一套 **可展示、可解释、可答辩** 的管理指标。

因此，P7 的核心不是“图表”，而是“口径”。
口径一旦冻结，后续开发和答辩都会稳定很多。
