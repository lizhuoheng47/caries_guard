ALTER TABLE `med_image_file`
    ADD COLUMN `primary_slot` VARCHAR(128)
        GENERATED ALWAYS AS (
            CASE
                WHEN `is_primary` = '1' AND `deleted_flag` = 0
                    THEN CONCAT(`case_id`, ':', `image_type_code`)
                ELSE NULL
            END
        ) STORED COMMENT '主图唯一约束槽位' AFTER `is_primary`,
    ADD UNIQUE KEY `uk_primary_slot` (`primary_slot`);

ALTER TABLE `med_image_file`
    ADD KEY `idx_patient_shooting_time` (`patient_id`, `shooting_time`),
    ADD KEY `idx_org_quality_status` (`org_id`, `quality_status_code`);
