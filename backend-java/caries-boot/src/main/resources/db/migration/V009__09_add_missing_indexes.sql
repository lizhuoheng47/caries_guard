ALTER TABLE `ana_task_record`
    ADD KEY `idx_task_status_created_at` (`task_status_code`, `created_at`),
    ADD KEY `idx_org_created_at` (`org_id`, `created_at`);
