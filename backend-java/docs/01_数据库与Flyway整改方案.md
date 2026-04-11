# 数据库与 Flyway 整改方案

> 文档定位：数据库补口径、补约束、补迁移的执行方案  
> 目标：解决“文档正确但数据库未完全落地”的问题  
> 原则：只做增量迁移，不静默篡改历史迁移意图

---

# 1. 整改目标

本方案解决以下数据库层核心问题：

1. AI 协同表虽已存在，但口径未正式冻结；
2. `med_image_quality_check` 缺失或未统一进入当前迁移基线；
3. 部分索引与字段字典不完全一致；
4. 主图唯一规则没有强工程约束；
5. 注释编码与 DDL 可维护性存在问题；
6. 后续模块若直接开发，可能因为 schema 漂移导致返工。

---

# 2. 迁移管理总原则

## 2.1 迁移原则
- 统一使用 Flyway 增量迁移；
- 不轻易修改已经执行过的历史迁移脚本；
- 所有结构变化必须通过新版本号脚本追加；
- 迁移脚本命名必须表达目的，不允许 `fix.sql`、`new.sql`、`final.sql`。

## 2.2 推荐后续命名
- `V006__06_align_v1_schema_baseline.sql`
- `V007__07_add_med_image_quality_check.sql`
- `V008__08_strengthen_image_constraints.sql`
- `V009__09_add_missing_indexes.sql`
- `V010__10_fix_comment_encoding_notes.sql`

说明：
如果你希望减少脚本数量，也可以合并为 1~2 个迁移，但建议逻辑上分明。

---

# 3. V1 数据库正式冻结口径

## 3.1 AI 协同表正式纳入业务库
以下表正式作为业务平台库 V1 范围：

- `ana_task_record`
- `ana_result_summary`
- `ana_visual_asset`
- `ana_correction_feedback`
- `med_risk_assessment_record`

说明：
这些表只承载**业务协同、结果快照、可视化引用、医生修正、风险评估结果**，不承载训练主数据治理。

## 3.2 明确业务库与训练库边界
业务库中可以保留：
- AI 任务状态
- AI 摘要结果
- 可视化资产引用
- 医生修正回流日志
- 风险评估结果

业务库中不得直接扩展为：
- 标注平台主数据
- 训练数据快照治理
- 模型审批主工作流
- Gold Set 主表

---

# 4. 必补结构：med_image_quality_check

## 4.1 建表目的
`med_image_quality_check` 用于承载影像质检记录，支持：

- 规则引擎自动质检；
- 人工复检；
- 多次质检留痕；
- 以最新有效记录为准；
- 分数与原因可追溯。

## 4.2 推荐字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| image_id | BIGINT | 关联 `med_image_file.id` |
| case_id | BIGINT | 冗余病例ID便于查询 |
| patient_id | BIGINT | 冗余患者ID便于查询 |
| check_type_code | VARCHAR(32) | AUTO / MANUAL |
| check_result_code | VARCHAR(32) | PASS / REJECT / REVIEW |
| quality_score | INT | 0-100 |
| blur_score | INT | 0-100 |
| exposure_score | INT | 0-100 |
| integrity_score | INT | 0-100 |
| occlusion_score | INT | 0-100 |
| issue_codes_json | JSON | 问题列表 |
| suggestion_text | VARCHAR(500) | 建议 |
| current_flag | CHAR(1) | 是否当前有效记录 |
| checked_by | BIGINT | 人工质检人 |
| checked_at | DATETIME | 质检时间 |
| org_id | BIGINT | 机构ID |
| status | VARCHAR(32) | 记录状态 |
| deleted_flag | BIGINT | 逻辑删除 |
| created_by | BIGINT | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_by | BIGINT | 更新人 |
| updated_at | DATETIME | 更新时间 |

## 4.3 索引建议
- `IDX(image_id, checked_at)`
- `IDX(case_id, checked_at)`
- `IDX(org_id, check_result_code)`
- `IDX(image_id, current_flag)`

## 4.4 查询策略
正式规定：

> 一张影像可以有多次质检记录，但系统默认读取 `current_flag = 1` 的最新有效记录。

更新策略：
- 新增一条有效质检记录前，将该 image_id 下现有 `current_flag=1` 更新为 0；
- 新插入记录标记为 `current_flag=1`；
- 此操作必须在事务中完成。

---

# 5. 必补约束：主图唯一规则

## 5.1 业务规则
对于同一病例（`case_id`）和同一影像类型（`image_type_code`）：

> 同时最多只允许一条记录为主图，即 `is_primary = 1`。

## 5.2 风险
若只在代码层约束，可能在并发上传或重复提交时出现多主图。

## 5.3 落地策略
推荐采用“双层保护”：

### 第一层：应用服务事务约束
保存主图时：
1. 查询当前病例、当前类型下是否已有主图；
2. 若本次新图要设主图，则先批量清空旧主图；
3. 再写入当前记录为主图；
4. 全流程一个事务。

### 第二层：数据库校验辅助
如果当前 MySQL 版本和索引策略允许，可以考虑以下方式之一：
- 生成列 + 唯一索引；
- 或通过触发器 / 约束型补偿；
- 若环境不适合，保留事务约束 + 定时巡检校验任务。

## 5.4 巡检 SQL 建议
定期巡检是否存在异常多主图：

```sql
SELECT case_id, image_type_code, COUNT(*) AS primary_count
FROM med_image_file
WHERE is_primary = '1' AND deleted_flag = 0
GROUP BY case_id, image_type_code
HAVING COUNT(*) > 1;
```

---

# 6. 索引复核整改

## 6.1 med_case_status_log
必须存在：
- `IDX(case_id, changed_at)`
- `IDX(org_id, changed_at)`

建议补充：
- `IDX(to_status_code, changed_at)`

## 6.2 med_image_file
建议最终冻结以下索引：
- `IDX(case_id, image_type_code)`
- `IDX(patient_id, shooting_time)`
- `IDX(org_id, quality_status_code)`
- `IDX(attachment_id)`

## 6.3 ana_task_record
建议确认以下索引：
- `UK(task_no, deleted_flag)`
- `IDX(case_id)`
- `IDX(task_status_code, created_at)`
- `IDX(org_id, created_at)`

## 6.4 ana_result_summary
建议确认以下索引：
- `IDX(task_id)`
- `IDX(case_id)`
- `IDX(highest_severity_vc)` 或等效虚拟列索引

## 6.5 rpt_record
建议确认：
- `UK(report_no, deleted_flag)`
- `IDX(case_id, report_type_code)`
- `IDX(patient_id, generated_at)`
- `IDX(report_status_code, generated_at)`

---

# 7. 注释编码整改

## 7.1 问题
当前部分 SQL 文件中中文注释存在乱码风险，影响后续维护和评审展示。

## 7.2 方案
- 仓库统一 UTF-8 编码；
- IDE 全员统一 UTF-8；
- 新迁移脚本全部使用 UTF-8；
- 对历史迁移不做无意义重写；
- 若必须修复历史乱码，应单独留说明。

## 7.3 团队规范
- 提交前检查 SQL 文件编码；
- Git 仓库增加 `.editorconfig`；
- SQL 文件统一 `utf-8` + `lf`。

---

# 8. 历史迁移与新增迁移边界

## 8.1 禁止事项
- 禁止因为“看起来不整齐”重写已执行的历史迁移；
- 禁止直接在旧迁移文件里偷偷加字段；
- 禁止代码先依赖新字段、却不补迁移。

## 8.2 正确做法
- 任何新增字段/表/索引/约束，均写新增迁移；
- 在 README 和架构文档中同步更新；
- 对“历史口径不一致”的内容，用新增迁移完成纠偏。

---

# 9. 推荐迁移脚本拆分方案

## V006：对齐 V1 正式范围
内容：
- 文档层对应的必要注释修正；
- 明确 AI 协同表为 V1 范围；
- 补缺失索引。

## V007：新增 med_image_quality_check
内容：
- 建表；
- 索引；
- 默认状态约定。

## V008：强化 image 约束
内容：
- 主图唯一支持；
- 巡检视图或巡检脚本；
- quality_status_code 索引补强。

## V009：补通用索引与局部字段修正
内容：
- case/image/report/analysis 相关关键索引对齐；
- 统一版本号字段命名兼容说明。

---

# 10. 交付验收标准

数据库整改完成后，必须满足：

1. V1 表范围无歧义；
2. `med_image_quality_check` 正式存在；
3. 主图规则可通过代码与测试保证；
4. 关键索引齐备；
5. Flyway 可从空库完整执行；
6. 本地环境与 CI 环境迁移一致；
7. 文档与迁移口径一致。

---

# 11. 输出结论

数据库整改不是“优化建议”，而是当前继续开发前的**强前置动作**。

若不先做这一步，后续 patient/image/analysis/report 模块将面临：

- 字段漂移；
- 迁移返工；
- 业务规则分散；
- 测试失真；
- 答辩口径冲突。

因此本文正式建议：

> 在进入 patient/image 模块实做前，先完成一次 V1 数据库与 Flyway 基线整改。
