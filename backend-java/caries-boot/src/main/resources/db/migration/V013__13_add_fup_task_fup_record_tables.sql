-- V013: Align followup schema with P6 implementation.
-- Baseline tables were introduced in V006; this migration upgrades them for:
-- 1) trigger-source idempotency
-- 2) richer task fields
-- 3) richer record fields

-- ============================================================
-- 1. fup_plan: trigger source + trigger ref for idempotent triggering
-- ============================================================
ALTER TABLE `fup_plan`
    ADD COLUMN `trigger_source_code` VARCHAR(32) NULL
        COMMENT 'trigger source(RISK_HIGH/REPORT_REVIEW/DOCTOR_MANUAL)' AFTER `remark`,
    ADD COLUMN `trigger_ref_id` BIGINT NULL
        COMMENT 'trigger ref id(report id / risk id)' AFTER `trigger_source_code`;

ALTER TABLE `fup_plan`
    ADD KEY `idx_case_trigger_ref` (`case_id`, `trigger_source_code`, `trigger_ref_id`, `plan_status_code`);

-- ============================================================
-- 2. fup_task: align with application model
-- ============================================================
ALTER TABLE `fup_task`
    ADD COLUMN `task_type_code` VARCHAR(32) NOT NULL DEFAULT 'FOLLOW_CONTACT'
        COMMENT 'task type(FOLLOW_CONTACT/RECHECK)' AFTER `patient_id`,
    ADD COLUMN `assigned_to_user_id` BIGINT NULL
        COMMENT 'assigned operator user id' AFTER `task_status_code`,
    ADD COLUMN `started_at` DATETIME NULL
        COMMENT 'task started time' AFTER `due_date`;

UPDATE `fup_task`
SET `assigned_to_user_id` = `assigned_user_id`
WHERE `assigned_to_user_id` IS NULL
  AND `assigned_user_id` IS NOT NULL;

ALTER TABLE `fup_task`
    DROP COLUMN `assigned_user_id`;

ALTER TABLE `fup_task`
    ADD KEY `idx_case_status` (`case_id`, `task_status_code`),
    ADD KEY `idx_assigned_due` (`assigned_to_user_id`, `due_date`),
    ADD KEY `idx_org_duedate` (`org_id`, `due_date`);

-- ============================================================
-- 3. fup_record: align with application model
-- ============================================================
ALTER TABLE `fup_record`
    ADD COLUMN `record_no` VARCHAR(32) NULL COMMENT 'record no' AFTER `id`,
    ADD COLUMN `followup_method_code` VARCHAR(32) NULL COMMENT 'method(PHONE/OUTPATIENT/SMS/ONLINE)' AFTER `patient_id`,
    ADD COLUMN `contact_result_code` VARCHAR(32) NULL COMMENT 'contact result(REACHED/NO_ANSWER/REFUSED)' AFTER `followup_method_code`,
    ADD COLUMN `follow_next_flag` CHAR(1) NULL COMMENT 'continue followup flag(0/1)' AFTER `contact_result_code`,
    ADD COLUMN `next_interval_days` INT NULL COMMENT 'next interval days' AFTER `follow_next_flag`,
    ADD COLUMN `outcome_summary` VARCHAR(1000) NULL COMMENT 'outcome summary' AFTER `next_interval_days`,
    ADD COLUMN `doctor_notes` VARCHAR(2000) NULL COMMENT 'doctor notes' AFTER `outcome_summary`;

UPDATE `fup_record`
SET `record_no` = CONCAT('REC', LPAD(`id`, 19, '0'))
WHERE `record_no` IS NULL;

UPDATE `fup_record`
SET `followup_method_code` = 'PHONE'
WHERE `followup_method_code` IS NULL
   OR `followup_method_code` = '';

UPDATE `fup_record`
SET `contact_result_code` = 'REACHED'
WHERE `contact_result_code` IS NULL
   OR `contact_result_code` = '';

UPDATE `fup_record`
SET `follow_next_flag` = CASE WHEN `next_followup_date` IS NULL THEN '0' ELSE '1' END
WHERE `follow_next_flag` IS NULL
   OR `follow_next_flag` = '';

UPDATE `fup_record`
SET `outcome_summary` = `record_content`
WHERE (`outcome_summary` IS NULL OR `outcome_summary` = '')
  AND `record_content` IS NOT NULL;

UPDATE `fup_record`
SET `next_interval_days` = DATEDIFF(`next_followup_date`, DATE(`recorded_at`))
WHERE `next_interval_days` IS NULL
  AND `next_followup_date` IS NOT NULL;

ALTER TABLE `fup_record`
    MODIFY COLUMN `record_no` VARCHAR(32) NOT NULL COMMENT 'record no',
    MODIFY COLUMN `followup_method_code` VARCHAR(32) NOT NULL DEFAULT 'PHONE'
        COMMENT 'method(PHONE/OUTPATIENT/SMS/ONLINE)',
    MODIFY COLUMN `contact_result_code` VARCHAR(32) NOT NULL DEFAULT 'REACHED'
        COMMENT 'contact result(REACHED/NO_ANSWER/REFUSED)',
    MODIFY COLUMN `follow_next_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT 'continue followup flag(0/1)';

ALTER TABLE `fup_record`
    ADD UNIQUE KEY `uk_recordno_del` (`record_no`, `deleted_flag`),
    ADD KEY `idx_case_recorded` (`case_id`, `recorded_at`);
