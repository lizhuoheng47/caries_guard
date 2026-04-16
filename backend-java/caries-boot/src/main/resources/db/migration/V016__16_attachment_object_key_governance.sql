ALTER TABLE `med_attachment`
    ADD COLUMN `asset_type_code` VARCHAR(32) NULL COMMENT 'Asset type code, for example PANORAMIC/MASK/DOCTOR_REPORT' AFTER `file_category_code`,
    ADD COLUMN `source_attachment_id` BIGINT NULL COMMENT 'Source attachment id when this object is derived from another object' AFTER `asset_type_code`,
    ADD COLUMN `retention_policy_code` VARCHAR(32) NOT NULL DEFAULT 'LONG_TERM' COMMENT 'Object retention policy code' AFTER `visibility_code`,
    ADD COLUMN `expired_at` DATETIME NULL COMMENT 'Object expiration time for temporary assets' AFTER `retention_policy_code`;

CREATE INDEX `idx_attachment_source` ON `med_attachment` (`source_attachment_id`);
CREATE INDEX `idx_attachment_retention` ON `med_attachment` (`retention_policy_code`, `expired_at`);

UPDATE `med_attachment`
SET `file_category_code` = 'RAW_IMAGE'
WHERE `biz_module_code` = 'CASE'
  AND `file_category_code` = 'IMAGE';

UPDATE `med_attachment`
SET `file_category_code` = 'VISUAL',
    `retention_policy_code` = 'VISUAL_180D',
    `expired_at` = COALESCE(`expired_at`, DATE_ADD(`upload_time`, INTERVAL 180 DAY))
WHERE `biz_module_code` = 'ANALYSIS'
  AND `file_category_code` = 'IMAGE';

UPDATE `med_attachment`
SET `asset_type_code` = COALESCE(`asset_type_code`, 'PANORAMIC'),
    `retention_policy_code` = 'LONG_TERM',
    `expired_at` = NULL
WHERE `file_category_code` = 'RAW_IMAGE';

UPDATE `med_attachment`
SET `asset_type_code` = COALESCE(`asset_type_code`, 'DOCTOR_REPORT'),
    `retention_policy_code` = 'LONG_TERM',
    `expired_at` = NULL
WHERE `file_category_code` = 'REPORT';

UPDATE `med_attachment`
SET `asset_type_code` = COALESCE(`asset_type_code`, 'EXPORT_FILE'),
    `retention_policy_code` = 'EXPORT_7D',
    `expired_at` = COALESCE(`expired_at`, DATE_ADD(`upload_time`, INTERVAL 7 DAY))
WHERE `file_category_code` = 'EXPORT';
