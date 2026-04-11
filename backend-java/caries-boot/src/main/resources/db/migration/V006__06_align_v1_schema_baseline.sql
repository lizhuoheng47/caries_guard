ALTER TABLE `med_case_status_log`
    ADD COLUMN `change_reason_code` VARCHAR(64) NULL COMMENT 'standard reason code' AFTER `changed_by`;

CREATE TABLE `sys_login_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `trace_id` VARCHAR(64) NULL COMMENT '请求链路ID',
  `username` VARCHAR(64) NULL COMMENT '登录账号',
  `user_id` BIGINT NULL COMMENT '登录用户ID',
  `org_id` BIGINT NULL COMMENT '所属机构ID',
  `login_status_code` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '登录状态(SUCCESS/FAILED)',
  `client_ip` VARCHAR(64) NULL COMMENT '客户端IP',
  `user_agent` VARCHAR(500) NULL COMMENT '客户端标识',
  `failure_reason` VARCHAR(500) NULL COMMENT '失败原因',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `login_time`),
  KEY `idx_org_time` (`org_id`, `login_time`),
  KEY `idx_status_time` (`login_status_code`, `login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统登录日志表';

CREATE TABLE `sys_oper_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `trace_id` VARCHAR(64) NULL COMMENT '请求链路ID',
  `module_code` VARCHAR(64) NOT NULL COMMENT '模块编码',
  `operation_type_code` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `operation_name` VARCHAR(128) NULL COMMENT '操作名称',
  `request_path` VARCHAR(255) NULL COMMENT '请求路径',
  `request_method` VARCHAR(16) NULL COMMENT '请求方法',
  `target_id` BIGINT NULL COMMENT '目标业务主键',
  `operator_user_id` BIGINT NULL COMMENT '操作人用户ID',
  `org_id` BIGINT NULL COMMENT '所属机构ID',
  `success_flag` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否成功(0/1)',
  `result_code` VARCHAR(32) NULL COMMENT '结果码',
  `error_message` VARCHAR(1000) NULL COMMENT '错误信息',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_module_time` (`module_code`, `operation_time`),
  KEY `idx_operator_time` (`operator_user_id`, `operation_time`),
  KEY `idx_org_time` (`org_id`, `operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作日志表';

CREATE TABLE `rpt_template` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码',
  `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
  `report_type_code` VARCHAR(32) NOT NULL DEFAULT 'DOCTOR' COMMENT '报告类型(DOCTOR/PATIENT)',
  `template_content` MEDIUMTEXT NULL COMMENT '模板内容',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code_del` (`template_code`, `deleted_flag`),
  KEY `idx_org_type` (`org_id`, `report_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报告模板表';

CREATE TABLE `rpt_export_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `report_id` BIGINT NOT NULL COMMENT '报告ID',
  `attachment_id` BIGINT NULL COMMENT '导出附件ID',
  `export_type_code` VARCHAR(32) NOT NULL DEFAULT 'PDF' COMMENT '导出类型',
  `export_channel_code` VARCHAR(32) NOT NULL DEFAULT 'DOWNLOAD' COMMENT '导出渠道',
  `exported_by` BIGINT NULL COMMENT '导出人用户ID',
  `exported_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导出时间',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  PRIMARY KEY (`id`),
  KEY `idx_report_time` (`report_id`, `exported_at`),
  KEY `idx_org_time` (`org_id`, `exported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报告导出日志表';

CREATE TABLE `fup_task` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `task_no` VARCHAR(32) NOT NULL COMMENT '随访任务编号',
  `plan_id` BIGINT NOT NULL COMMENT '随访计划ID',
  `case_id` BIGINT NOT NULL COMMENT '病例ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `task_status_code` VARCHAR(32) NOT NULL DEFAULT 'TODO' COMMENT '任务状态(TODO/IN_PROGRESS/DONE/OVERDUE/CANCELLED)',
  `due_date` DATE NULL COMMENT '应完成日期',
  `assigned_user_id` BIGINT NULL COMMENT '指派用户ID',
  `completed_by` BIGINT NULL COMMENT '完成人用户ID',
  `completed_at` DATETIME NULL COMMENT '完成时间',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_taskno_del` (`task_no`, `deleted_flag`),
  KEY `idx_plan_status` (`plan_id`, `task_status_code`),
  KEY `idx_patient_due_date` (`patient_id`, `due_date`),
  KEY `idx_org_assigned` (`org_id`, `assigned_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='随访任务表';

CREATE TABLE `fup_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `task_id` BIGINT NOT NULL COMMENT '随访任务ID',
  `plan_id` BIGINT NOT NULL COMMENT '随访计划ID',
  `case_id` BIGINT NOT NULL COMMENT '病例ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `result_code` VARCHAR(32) NOT NULL DEFAULT 'DONE' COMMENT '记录结果',
  `record_content` TEXT NULL COMMENT '记录内容',
  `next_followup_date` DATE NULL COMMENT '下次随访日期',
  `recorder_user_id` BIGINT NULL COMMENT '记录人用户ID',
  `recorded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_time` (`task_id`, `recorded_at`),
  KEY `idx_plan_time` (`plan_id`, `recorded_at`),
  KEY `idx_org_time` (`org_id`, `recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='随访记录表';
