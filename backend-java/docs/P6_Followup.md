# P6 Followup 国一版开发规划终稿

## 一、文档定位

本文档用于指导 `P6 Followup` 模块的实施落地、联调测试与答辩支撑，目标不是单纯补齐一组 CRUD，而是把系统能力从 `analysis -> report` 推进到 `analysis -> report -> followup` 的业务闭环，形成更完整的可信辅助决策链路。

该规划基于当前项目真实进度制定：`P4 analysis` 核心流程已完成，`P5 report` 核心流程已完成，且 `analysis -> report` Stage-2 真实集成联测已落地。因此，当前进入 `P6 followup` 是正确且必要的阶段选择。

---

## 二、P6 的核心目标

P6 的目标不是“增加一个随访模块页面”，而是完成以下三件事：

1. 让高风险病例在报告生成后能够自动进入后续干预流程；
2. 让随访计划、任务执行、结果记录形成完整业务链路；
3. 让系统具备“识别—报告—干预追踪”的平台闭环能力。

从国一标准看，`followup` 的价值不在于表数量，而在于它是否把平台从“出结果”升级为“可持续跟踪管理”。

## 当前落地进度（2026-04-13）

已完成：

1. `FollowupPlanAppService / FollowupTaskAppService / FollowupRecordAppService / FollowupTriggerService / FollowupQueryService` 已实现。
2. `FollowupPlanController / FollowupTaskController / FollowupRecordController` 已实现。
3. report 生成流程已接入 followup 触发入口：高风险或复核建议会自动触发随访。
4. `caries-integration` 已新增：
   - `AnalysisReportFollowupE2ETest`
   - `FollowupTriggerIdempotencyE2ETest`
   - `FollowupNonTriggerIntegrationTest`

进行中：

1. Flyway 迁移与 followup 字段模型最终对齐。
2. 真实数据库链路下的 followup E2E 与边界用例补齐（审计、逾期）。

---

## 三、P6 在全平台中的作用

在整体链路中，P6 应处于以下位置：

`image -> analysis -> risk/report -> followup -> audit`

它承担的是“高风险病例后续追踪”的职责，是平台闭环中的最后一个关键业务环节。

如果没有 P6，系统更像一个“分析 + 报告系统”；
如果 P6 做完整，系统才更像一个“可信辅助决策平台”。

---

## 四、当前规划的总体评价

当前提出的开发顺序总体方向正确，能够自然衔接现有工程进度，尤其是把 `analysis -> report -> followup` 作为下一条跨模块证据链，这是正确判断。

但如果对标顶级国一水准，原始规划仍偏“开发任务清单”，还不够“答辩级实施方案”。

主要差距有六点：

1. 业务目标尚未充分冻结；
2. 触发规则未形成统一规则入口；
3. 病例主状态与随访子状态关系表达不够清晰；
4. 幂等、逾期、关闭规则没有显式提出；
5. 通知留痕未纳入主开发计划；
6. E2E 测试范围偏窄，仍需补边界场景。

因此，建议将原规划升级为国一版实施方案。

---

## 五、P6 必须冻结的业务口径

在正式编码前，必须先冻结以下业务定义。

### 5.1 三层实体职责

#### `fup_plan`
表示病例级随访计划，是随访阶段的总控容器。

职责：
- 记录为什么需要随访；
- 记录计划周期、优先级、开始/结束状态；
- 作为多个随访任务的上位实体。

#### `fup_task`
表示一次具体待执行的随访任务。

职责：
- 表示某次应执行的联系、复查、回访动作；
- 支持分配责任人、截止日期、任务状态流转；
- 支持逾期和完成管理。

#### `fup_record`
表示一次实际随访执行后的事实记录。

职责：
- 记录本次随访结果；
- 记录患者反馈、处理建议、是否继续观察；
- 为后续是否派生下一次任务提供依据。

---

### 5.2 三层关系必须清晰

必须坚持以下关系：

- 病例是否需要随访：看 `med_case`
- 随访计划是否建立：看 `fup_plan`
- 当前任务执行到哪里：看 `fup_task`
- 实际执行结果是什么：看 `fup_record`

不允许出现以下混淆：

- 用任务状态代替病例主状态；
- 用计划是否存在代替任务是否完成；
- 用记录是否存在代替计划是否关闭。

---

### 5.3 状态机必须分层

#### 病例主状态
病例主状态继续使用现有口径：

- `CREATED`
- `QC_PENDING`
- `ANALYZING`
- `REVIEW_PENDING`
- `REPORT_READY`
- `FOLLOWUP_REQUIRED`
- `CLOSED`
- `CANCELLED`

其中，病例进入随访阶段时，主状态可以进入 `FOLLOWUP_REQUIRED`。

#### 随访任务状态
建议冻结为：

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`
- `CANCELLED`

注意：
病例主状态和随访任务状态不是一回事，绝对不能混用。

---

## 六、P6 国一版开发顺序

### 阶段 1：先冻结 Followup 业务口径与状态机

先完成以下设计冻结，而不是直接写 CRUD：

1. 明确 `fup_plan / fup_task / fup_record` 的职责边界；
2. 定义计划创建、任务流转、记录填写、计划关闭规则；
3. 定义病例何时进入 `FOLLOWUP_REQUIRED`；
4. 定义哪些动作必须写 `med_case_status_log`；
5. 定义哪些动作必须写 `sys_oper_log`；
6. 明确通知记录是否进入一期主链路。

交付物：
- P6 业务规则说明；
- 状态机说明；
- 触发规则矩阵；
- 测试口径清单。

---

### 阶段 2：实现 Followup 主链路应用服务与 API

这一阶段不应只做三张表的基础增删改查，而应形成完整应用服务。

建议至少实现以下服务：

#### `FollowupPlanAppService`
职责：
- 创建计划
- 取消计划
- 关闭计划
- 根据规则派生首个任务

#### `FollowupTaskAppService`
职责：
- 创建任务
- 指派任务
- 认领任务
- 更新任务状态
- 处理逾期
- 完成任务

#### `FollowupRecordAppService`
职责：
- 新增随访记录
- 回填任务完成信息
- 记录是否建议继续观察
- 必要时派生下一次任务

#### `FollowupTriggerService`
职责：
- 统一承接 report / risk 的触发逻辑
- 统一幂等保护
- 避免触发逻辑散落在多个模块

#### `FollowupQueryService`
职责：
- 查询病例随访历史
- 查询患者随访轨迹
- 查询任务列表与待办视图

同时完成以下层级代码：

- DO
- Mapper
- Repository
- AppService
- Controller
- Command / Query / VO

---

### 阶段 3：抽象统一触发入口，接入 report / risk 规则

原始规划中“从 report/risk 结果触发随访计划”这个方向是正确的，但必须进一步制度化。

建议正式建立触发规则矩阵：

| 触发来源 | 条件 | 结果 |
|---|---|---|
| 风险评估 | `overall_risk_level_code = HIGH` | 自动创建随访计划 |
| 报告结论 | 医生明确勾选复查/随访建议 | 自动创建随访计划 |
| 医生复核 | 修正后仍属于高风险 | 自动或半自动创建计划 |
| 随访记录 | 建议继续观察 | 在原计划下派生下一任务 |

要求：

1. 所有触发逻辑统一走 `FollowupTriggerService`；
2. 不允许 report 模块和 analysis 模块各自偷偷建计划；
3. 同一触发来源必须具备幂等保护；
4. 必须能解释“为什么被触发”。

---

### 阶段 4：补齐幂等、关闭与逾期机制

这是国一工程感最强的一层，必须明确写进开发计划。

#### 4.1 幂等保护
必须保证：
- 同一病例
- 同一触发来源
- 同一触发版本

不会重复生成多个有效计划或重复首任务。

否则在以下场景会出问题：
- 报告重复生成；
- 风险结果重复写回；
- callback 重放；
- 医生重复确认。

#### 4.2 计划关闭
必须明确计划何时关闭，例如：
- 所有关联任务完成；
- 医生确认无需继续随访；
- 病例已进入关闭态；
- 人工取消计划。

#### 4.3 任务派生
必须明确：
- 若本次随访记录结果为“继续观察”，应派生下一次任务；
- 若本次随访已完成且无需继续干预，计划可关闭；
- 派生下一任务优先在原计划下进行，不建议无规则地新建计划。

#### 4.4 逾期处理
必须明确：
- 任务超过 `due_date` 后如何变更为 `OVERDUE`；
- 逾期后是否自动提醒；
- 逾期是否影响病例状态或看板统计。

---

### 阶段 5：打通病例联动、审计留痕与通知记录

这一阶段是很多队伍容易忽视，但评委很看重的部分。

必须完成以下联动：

#### 病例联动
- 创建计划后，必要时同步更新 `med_case.followup_required_flag`
- 视业务规则决定病例主状态是否转为 `FOLLOWUP_REQUIRED`
- 所有主状态变化必须写 `med_case_status_log`

#### 操作审计
关键动作必须落 `sys_oper_log`，包括：
- 创建计划
- 取消计划
- 指派任务
- 完成任务
- 填写随访记录
- 手工关闭计划

#### 消息留痕
即使一期不接真实短信/邮件，也建议至少落 `msg_notify_record`，包括：
- 任务创建提醒
- 任务即将到期提醒
- 任务逾期提醒
- 通知发送成功/失败结果

这样 followup 才像真实业务闭环，而不是纯内部台账。

---

### 阶段 6：新增跨模块 E2E 与边界测试

这一阶段必须把 followup 纳入跨模块证据链，而不是只做单模块接口自测。

建议新增：

#### 主链路 E2E
`analysis -> report -> followup`

断言至少覆盖：
1. analysis 成功写回风险结果；
2. report 成功生成；
3. 高风险或复查建议触发随访计划；
4. 自动生成首个随访任务；
5. 病例状态或标记同步更新；
6. 操作审计与通知记录存在。

---

## 七、P6 必测的关键测试场景

除了 happy path，至少要覆盖以下四类场景。

### 7.1 正向主链路
场景：
- AI 分析成功
- 风险等级高
- 医生生成报告
- 自动触发 followup

断言：
- `fup_plan` 创建成功
- `fup_task` 创建成功
- 病例进入随访要求态或打上随访标记
- 有状态日志和操作日志

---

### 7.2 幂等场景
场景：
- 同一风险结果重复回放
- 或同一报告重复触发

断言：
- 不重复创建有效计划
- 不重复创建首任务
- 幂等条件可被解释

---

### 7.3 非触发场景
场景：
- 风险等级低
- 报告无复查建议

断言：
- 不生成 followup plan
- 不生成 followup task
- 不错误修改病例状态

---

### 7.4 审计场景
场景：
- 创建计划
- 任务完成
- 填写随访记录

断言：
- `med_case_status_log` 正确落表
- `sys_oper_log` 存在关键操作
- `msg_notify_record` 有提醒留痕

---

### 7.5 逾期场景
场景：
- 任务超期未完成

断言：
- `task_status_code` 变为 `OVERDUE`
- 产生提醒留痕
- 看板统计可识别逾期任务

---

## 八、建议新增的代码与测试对象

### 8.1 建议新增核心类

#### 应用服务
- `FollowupPlanAppService`
- `FollowupTaskAppService`
- `FollowupRecordAppService`
- `FollowupTriggerService`
- `FollowupQueryService`

#### 控制器
- `FollowupPlanController`
- `FollowupTaskController`
- `FollowupRecordController`

#### 查询与命令对象
- `CreateFollowupPlanCommand`
- `CreateFollowupTaskCommand`
- `CompleteFollowupTaskCommand`
- `CreateFollowupRecordCommand`
- `FollowupTaskPageQuery`
- `FollowupPlanDetailVO`
- `FollowupTaskListVO`
- `FollowupRecordDetailVO`

---

### 8.2 建议新增测试类

至少建议新增以下测试：

- `AnalysisReportFollowupE2ETest`
- `FollowupTriggerIdempotencyE2ETest`
- `FollowupNonTriggerIntegrationTest`
- `FollowupAuditIntegrationTest`
- `FollowupOverdueIntegrationTest`

如果资源有限，最少也要先落前三个。

---

## 九、建议的实际开发顺序

为了兼顾交付效率与答辩质量，建议按下面顺序推进：

### 第一步
冻结 `fup_plan / fup_task / fup_record` 职责、状态机、触发规则矩阵。

### 第二步
完成三张表对应的 DO、Mapper、Repository、AppService、Controller。

### 第三步
实现 `FollowupTriggerService`，统一承接 risk/report 触发。

### 第四步
补齐病例联动、状态日志、操作日志、消息记录。

### 第五步
完成 `analysis -> report -> followup` 跨模块 E2E。

### 第六步
补充幂等、非触发、逾期、审计边界用例。

这个顺序兼顾开发现实性和国一展示效果，是当前最稳的实施路线。

---

## 十、P6 答辩价值如何表述

答辩时不要说：

“我们又新增了一个随访模块。”

建议改成：

“我们把平台从分析和报告，进一步推进到高风险病例的后续干预与追踪管理，让系统从一次性出结果，升级为可持续闭环管理的平台。”

这句话的价值在于：

1. 强调平台闭环，而非页面堆砌；
2. 强调业务价值，而非单点功能；
3. 强调可信辅助决策，而非识别 Demo。

---

## 十一、最终评价

从当前工程进度看，下一步开发 P6 是正确决策。

但若要达到顶级国一水准，P6 不能只理解为三张表和几个接口，而必须升级为：

- 有规则入口；
- 有状态机分层；
- 有幂等控制；
- 有逾期与关闭机制；
- 有跨模块 E2E；
- 有审计与通知留痕；
- 有明确答辩价值表达。

只有这样，P6 才不仅是“功能补齐”，而是“闭环能力补齐”。

---

## 十二、可直接执行的最终版开发建议

### P6 Followup 国一版实施顺序

1. 先冻结 followup 业务口径与状态机  
   明确 `fup_plan / fup_task / fup_record` 三层职责，冻结计划状态、任务状态、记录字段，以及病例何时进入 `FOLLOWUP_REQUIRED`。

2. 实现 followup 主链路应用服务与 API  
   完成 `fup_plan / fup_task / fup_record` 的 DO、Mapper、Repository、AppService、Controller，并补齐计划创建、任务流转、记录回填、计划关闭等核心用例。

3. 抽象统一触发入口，接入 report / risk 规则  
   由统一的 `FollowupTriggerService` 承接高风险、医生复核建议等触发条件，避免触发逻辑散落在 analysis/report 模块，并加入幂等保护。

4. 打通病例联动、审计留痕与通知记录  
   计划创建后同步维护病例随访标记与必要的病例状态流转日志；关键操作落 `sys_oper_log`；提醒行为落 `msg_notify_record`。

5. 新增跨模块 E2E 与边界测试  
   新增 `analysis -> report -> followup` 主链路 E2E，同时覆盖幂等、非触发、逾期、审计留痕等关键场景，形成完整答辩证据链。

---

## 十三、结语

这版规划的核心，不是让队员“把模块写完”，而是让整个项目在现有基础上，真正补上“后续干预与追踪”的闭环能力。

从竞赛视角看，这一步非常关键。  
它会直接决定评委如何定义你们的作品：  
是“会分析、会出报告的系统”，还是“真正面向真实筛查流程的可信辅助决策平台”。
