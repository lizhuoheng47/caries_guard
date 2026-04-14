# 数据库与 Flyway 整改方案

## 1. 当前结论

数据库整改工作已基本完成，当前文档改为记录“现行 schema 基线”和“剩余数据库风险”。

截至 `2026-04-14`：

- `flyway_schema_history` 已执行到 `V013`
- `cg` 与 `cg_e2e` 均为 `38` 张表（含 Flyway 历史表）
- 当前 schema 已覆盖系统、病例、影像、分析、报告、随访、通知、看板统计所需主表

## 2. 当前迁移版本

- `V001`：系统管理基础表
- `V002`：字典种子数据
- `V003`：患者/就诊/病例/诊断/牙位等医疗主表
- `V004`：附件、影像、AI、报告、随访、通知主表
- `V005`：开发管理员与基础组织/角色种子
- `V006`：补齐登录日志、操作日志、报告模板、随访表基线字段
- `V007`：新增 `med_image_quality_check`
- `V008`：补强影像约束
- `V009`：补充缺失索引
- `V010`：修正开发管理员密码
- `V011`：`ana_result_summary` 增加聚合列
- `V012`：`ana_task_record` 增加 `retry_from_task_id`
- `V013`：对齐随访计划/任务/记录字段

## 3. 当前表清单

### 3.1 system 域

- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_menu`
- `sys_role_menu`
- `sys_config`
- `sys_dict_type`
- `sys_dict_item`
- `sys_dept`
- `sys_post`
- `sys_user_post`
- `sys_data_permission_rule`
- `sys_login_log`
- `sys_oper_log`

### 3.2 patient/case 域

- `pat_patient`
- `pat_guardian`
- `pat_profile`
- `med_visit`
- `med_case`
- `med_case_status_log`
- `med_case_diagnosis`
- `med_case_tooth_record`

### 3.3 image 域

- `med_attachment`
- `med_image_file`
- `med_image_quality_check`

### 3.4 analysis 域

- `ana_task_record`
- `ana_result_summary`
- `ana_correction_feedback`
- `ana_visual_asset`
- `med_risk_assessment_record`

### 3.5 report 域

- `rpt_template`
- `rpt_record`
- `rpt_export_log`

### 3.6 followup 域

- `fup_plan`
- `fup_task`
- `fup_record`
- `msg_notify_record`

## 4. 当前库真实内容

本地 `cg` 当前不是样例演示库，而是“系统种子库”。

已有数据：

- `sys_dept = 1`
- `sys_role = 1`
- `sys_user = 1`
- `sys_user_role = 1`
- `sys_dict_type = 4`
- `sys_dict_item = 18`

当前为空或基本为空：

- `sys_menu`
- `sys_role_menu`
- `sys_data_permission_rule`
- 所有 patient/image/analysis/report/followup 业务表

## 5. schema 共同规则

当前表设计统一遵循：

- 主键均为 `BIGINT`
- 机构隔离字段统一为 `org_id`
- 逻辑删除字段统一为 `deleted_flag`
- 大部分业务表保留 `created_by/created_at/updated_by/updated_at`
- 状态字段通常分为业务状态码与记录状态 `status`

## 6. 代码已实际使用的关键字段

- `med_case.case_status_code`
- `med_case.report_ready_flag`
- `med_case.followup_required_flag`
- `med_case_status_log.change_reason_code`
- `med_image_file.quality_status_code`
- `ana_task_record.task_status_code`
- `ana_task_record.retry_from_task_id`
- `ana_result_summary.overall_highest_severity`
- `ana_result_summary.uncertainty_score`
- `ana_result_summary.review_suggested_flag`
- `fup_plan.trigger_source_code`
- `fup_plan.trigger_ref_id`
- `fup_task.task_type_code`
- `fup_task.assigned_to_user_id`
- `fup_record.record_no`
- `fup_record.followup_method_code`
- `fup_record.contact_result_code`
- `fup_record.follow_next_flag`

## 7. 当前剩余数据库风险

- 表间仍主要依赖应用层保证一致性，数据库未大量声明外键约束
- `sys_menu` 当前无种子数据，导致权限矩阵无法通过本地库直接演示普通角色场景
- dashboard 当前直接查业务表，不存在快照表或宽表
- `rpt_export_log` 当前记录审计行为，不产出额外导出文件

## 8. 文档建议

后续凡涉及数据库说明，统一以：

- `SHOW TABLES` / `SHOW CREATE TABLE`
- `flyway_schema_history`
- 当前 Java DO/Mapper/Repository

三者共同确认，不再沿用历史规划文档中的“应有表结构”表述。
