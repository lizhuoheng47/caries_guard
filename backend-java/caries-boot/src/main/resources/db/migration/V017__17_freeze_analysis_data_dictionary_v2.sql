-- V017: Freeze AI analysis related fields according to data dictionary v2.

ALTER TABLE `med_attachment`
    MODIFY COLUMN `retention_policy_code` VARCHAR(32) NULL COMMENT 'Object retention policy code',
    ADD COLUMN `integrity_status_code` VARCHAR(32) NULL DEFAULT 'NORMAL' COMMENT 'Attachment integrity status: NORMAL/MISSING/ORPHANED' AFTER `expired_at`,
    ADD COLUMN `metadata_json` JSON NULL COMMENT 'Attachment extension metadata' AFTER `integrity_status_code`;

CREATE INDEX `idx_attachment_org_category_asset` ON `med_attachment` (`org_id`, `file_category_code`, `asset_type_code`);
CREATE INDEX `idx_attachment_expired_at` ON `med_attachment` (`expired_at`);

UPDATE `med_attachment`
SET `retention_policy_code` = 'TEMP_30D',
    `expired_at` = DATE_ADD(`upload_time`, INTERVAL 30 DAY)
WHERE `file_category_code` = 'VISUAL'
  AND `deleted_flag` = 0;

UPDATE `med_attachment`
SET `retention_policy_code` = 'TEMP_7D',
    `expired_at` = DATE_ADD(`upload_time`, INTERVAL 7 DAY)
WHERE `file_category_code` = 'EXPORT'
  AND `deleted_flag` = 0;

UPDATE `med_attachment`
SET `integrity_status_code` = 'NORMAL'
WHERE `integrity_status_code` IS NULL;

ALTER TABLE `med_image_file`
    ADD COLUMN `source_device_code` VARCHAR(64) NULL COMMENT 'Source device code' AFTER `quality_status_code`,
    ADD COLUMN `capture_batch_no` VARCHAR(64) NULL COMMENT 'Capture batch number' AFTER `source_device_code`;

ALTER TABLE `ana_task_record`
    ADD COLUMN `request_batch_no` VARCHAR(64) NULL COMMENT 'Batch scheduling number' AFTER `patient_id`,
    ADD COLUMN `callback_payload_json` JSON NULL COMMENT 'Raw callback payload' AFTER `request_payload_json`,
    ADD COLUMN `error_code` VARCHAR(64) NULL COMMENT 'AI callback error code' AFTER `retry_from_task_id`,
    MODIFY COLUMN `inference_millis` INT NULL COMMENT 'AI inference duration in milliseconds';

CREATE INDEX `idx_task_case_status` ON `ana_task_record` (`case_id`, `task_status_code`);
CREATE INDEX `idx_task_model_created` ON `ana_task_record` (`model_version`, `created_at`);

ALTER TABLE `ana_result_summary`
    ADD COLUMN `lesion_count` INT NULL COMMENT 'Lesion count extracted from AI result' AFTER `review_suggested_flag`,
    ADD COLUMN `abnormal_tooth_count` INT NULL COMMENT 'Abnormal tooth count extracted from AI result' AFTER `lesion_count`,
    ADD COLUMN `summary_version_no` INT NULL DEFAULT 1 COMMENT 'Summary version number' AFTER `abnormal_tooth_count`;

CREATE INDEX `idx_summary_case_created` ON `ana_result_summary` (`case_id`, `created_at`);
CREATE INDEX `idx_summary_overall_severity` ON `ana_result_summary` (`overall_highest_severity`);
CREATE INDEX `idx_summary_review_uncertainty` ON `ana_result_summary` (`review_suggested_flag`, `uncertainty_score`);

ALTER TABLE `ana_visual_asset`
    MODIFY COLUMN `tooth_code` VARCHAR(8) NULL COMMENT 'Tooth position code related to the visual asset',
    ADD COLUMN `source_attachment_id` BIGINT NULL COMMENT 'Source raw image attachment id' AFTER `related_image_id`,
    ADD COLUMN `sort_order` INT NULL DEFAULT 0 COMMENT 'Display order' AFTER `tooth_code`;

DROP INDEX `uk_task_asset_del` ON `ana_visual_asset`;
CREATE UNIQUE INDEX `uk_visual_task_asset_scope`
    ON `ana_visual_asset` (`task_id`, `asset_type_code`, `related_image_id`, `tooth_code`, `deleted_flag`);
CREATE INDEX `idx_visual_case_asset` ON `ana_visual_asset` (`case_id`, `asset_type_code`);
CREATE INDEX `idx_visual_source_attachment` ON `ana_visual_asset` (`source_attachment_id`);

ALTER TABLE `ana_correction_feedback`
    ADD COLUMN `source_attachment_id` BIGINT NULL COMMENT 'Source raw image attachment id' AFTER `source_image_id`,
    ADD COLUMN `export_candidate_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Whether this feedback is a training export candidate' AFTER `feedback_type_code`,
    ADD COLUMN `exported_snapshot_no` VARCHAR(64) NULL COMMENT 'Training snapshot number after export' AFTER `export_candidate_flag`;

UPDATE `ana_correction_feedback`
SET `export_candidate_flag` = `is_exported_for_train`,
    `exported_snapshot_no` = `dataset_snapshot_no`
WHERE `deleted_flag` = 0;

CREATE INDEX `idx_correction_source_attachment` ON `ana_correction_feedback` (`source_attachment_id`);

ALTER TABLE `med_risk_assessment_record`
    ADD COLUMN `task_id` BIGINT NULL COMMENT 'Source analysis task id' AFTER `patient_id`,
    ADD COLUMN `risk_score` DECIMAL(6,4) NULL COMMENT 'Risk score' AFTER `overall_risk_level_code`,
    ADD COLUMN `version_no` INT NULL DEFAULT 1 COMMENT 'Risk assessment version number' AFTER `assessment_report_json`;

CREATE INDEX `idx_risk_task` ON `med_risk_assessment_record` (`task_id`);
CREATE INDEX `idx_risk_case_version` ON `med_risk_assessment_record` (`case_id`, `version_no`);

ALTER TABLE `rpt_record`
    ADD COLUMN `source_summary_id` BIGINT NULL COMMENT 'Source analysis summary id' AFTER `attachment_id`,
    ADD COLUMN `source_risk_assessment_id` BIGINT NULL COMMENT 'Source risk assessment id' AFTER `source_summary_id`,
    ADD COLUMN `source_correction_id` BIGINT NULL COMMENT 'Source correction feedback id' AFTER `source_risk_assessment_id`;

CREATE INDEX `idx_report_sources`
    ON `rpt_record` (`source_summary_id`, `source_risk_assessment_id`, `source_correction_id`);
