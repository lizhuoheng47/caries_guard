-- 21. 附件对象表 med_attachment
CREATE TABLE `med_attachment` (
  `id` BIGINT NOT NULL COMMENT '附件主键',
  `biz_module_code` VARCHAR(32) NOT NULL COMMENT '业务模块(CASE/REPORT/FOLLOWUP)',
  `biz_id` BIGINT NULL COMMENT '业务主键ID(对应模块)',
  `file_category_code` VARCHAR(32) NOT NULL DEFAULT 'FILE' COMMENT '文件类别(IMAGE/REPORT/EXPORT)',
  `file_name` VARCHAR(255) NOT NULL COMMENT '服务器持久化存储文件名',
  `original_name` VARCHAR(255) NULL COMMENT '客户上传原始品名',
  `bucket_name` VARCHAR(128) NOT NULL COMMENT '对象存储桶名',
  `object_key` VARCHAR(500) NOT NULL COMMENT '对象存储地址键',
  `content_type` VARCHAR(128) NULL COMMENT '文件MIME',
  `file_ext` VARCHAR(32) NULL COMMENT '扩展名',
  `file_size_bytes` BIGINT NULL COMMENT '内容大小',
  `md5` VARCHAR(64) NULL COMMENT '防重复碰撞签名',
  `storage_provider_code` VARCHAR(32) NOT NULL DEFAULT 'MINIO' COMMENT '落地支持方(MINIO/OSS/S3)',
  `visibility_code` VARCHAR(32) NOT NULL DEFAULT 'PRIVATE' COMMENT '跨边界控制保护',
  `upload_user_id` BIGINT NULL,
  `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  /* 规避同一桶内物理文件重复冲突但允许软删重发 */
  UNIQUE KEY `uk_bucket_key_del` (`bucket_name`, `object_key`, `deleted_flag`),
  KEY `idx_biz` (`biz_module_code`, `biz_id`),
  KEY `idx_md5` (`md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨媒介底座附件对象表';


-- 22. 病例影像表 med_image_file
CREATE TABLE `med_image_file` (
  `id` BIGINT NOT NULL COMMENT '影像主键',
  `case_id` BIGINT NOT NULL COMMENT '病例ID',
  `visit_id` BIGINT NULL COMMENT '就诊ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `attachment_id` BIGINT NOT NULL COMMENT '强力挂靠的底层文件ID',
  `image_type_code` VARCHAR(32) NOT NULL DEFAULT 'PANORAMIC' COMMENT '影像模型(PANORAMIC/PERIAPICAL...)',
  `image_source_code` VARCHAR(32) NULL DEFAULT 'UPLOAD' COMMENT '传入通道',
  `shooting_time` DATETIME NULL COMMENT '仪器物理生成日期',
  `body_position_code` VARCHAR(32) NULL COMMENT '口内位点或拍片方向',
  `image_index_no` INT NULL COMMENT '病历集同类自增序号排序',
  `quality_status_code` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '前置质检状态拦截用',
  `is_primary` CHAR(1) NOT NULL DEFAULT '0' COMMENT '首图优先推荐标识',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_imagetype` (`case_id`, `image_type_code`),
  KEY `idx_attachment` (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病例结构化影像表';


-- ----------------------------------------------------
-- ★★★ 绝密不并入训练大体系的：四个最基础 AI 业务协同表 ★★★
-- ----------------------------------------------------

-- A1. AI 分析任务基础承载表 ana_task_record
CREATE TABLE `ana_task_record` (
  `id` BIGINT NOT NULL COMMENT '调度排盘主键',
  `task_no` VARCHAR(64) NOT NULL COMMENT '业务流水跟踪器',
  `case_id` BIGINT NOT NULL COMMENT '目标诊断病例',
  `patient_id` BIGINT NOT NULL COMMENT '涉事用户',
  `model_version` VARCHAR(64) NOT NULL COMMENT 'AI服务当时使用的版本依据(如 v2.5.1-caries)',
  `task_type_code` VARCHAR(32) NOT NULL DEFAULT 'INFERENCE' COMMENT '识别任务类型',
  `task_status_code` VARCHAR(32) NOT NULL DEFAULT 'QUEUEING' COMMENT '内部状态机(QUEUEING/PROCESSING/SUCCESS/FAILED)',
  `request_payload_json` JSON NULL COMMENT '发盘原始指令',
  `started_at` DATETIME NULL COMMENT '真正调起事件戳',
  `completed_at` DATETIME NULL COMMENT '处理交还事件戳',
  `error_message` VARCHAR(1000) NULL COMMENT '宕机容错异常文',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_taskno_del` (`task_no`, `deleted_flag`),
  KEY `idx_caseid` (`case_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立域-AI推演流程留底';

-- A2. AI 分析摘要提点表 ana_result_summary (使用 P1高优特征虚拟列提速)
CREATE TABLE `ana_result_summary` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `task_id` BIGINT NOT NULL COMMENT '对应上述调度的源血统',
  `case_id` BIGINT NOT NULL COMMENT '直出挂靠的目标病历',
  `raw_result_json` JSON NOT NULL COMMENT '最庞大复杂的元结果输出串',
  
  -- P1 级 "少而精" 聚焦提速虚拟列，主要用于医生页面极速看板加载和等值 Where :
  `highest_severity_vc` VARCHAR(32) GENERATED ALWAYS AS (`raw_result_json`->>'$.overall_highest_severity') VIRTUAL COMMENT '虚拟列：提取大模型总体认定的最恶性风险等级以直接下推查询',
  
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_case` (`case_id`),
  /* 为虚拟列提升的高规格检索索引 */
  KEY `idx_highest_severity_vc` (`highest_severity_vc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立域-大屏及医患分析摘要体载体表';

-- A3. 独白修正纠偏数据接警表 ana_correction_feedback
CREATE TABLE `ana_correction_feedback` (
  `id` BIGINT NOT NULL,
  `case_id` BIGINT NOT NULL,
  `diagnosis_id` BIGINT NULL COMMENT '所推翻或干预的那一行具体的医生复验定级结论指针',
  `source_image_id` BIGINT NULL COMMENT '如果有错位原图，必须指定指向',
  `doctor_user_id` BIGINT NOT NULL COMMENT '背书责任修改的专家身份',
  `original_inference_json` JSON NULL COMMENT '当时机器犯错的第一反应快照',
  `corrected_truth_json` JSON NULL COMMENT '最终人类裁定的标准答案方向',
  `feedback_type_code` VARCHAR(32) NOT NULL DEFAULT 'RE_GRADE' COMMENT '修补类别(OVERKILL/MISS/RE_GRADE)',
  `is_exported_for_train` CHAR(1) NOT NULL DEFAULT '0' COMMENT '用于未来和离线清洗训练体系桥接脱钩使用的隔离标记(默认0)',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case` (`case_id`),
  KEY `idx_doctor` (`doctor_user_id`),
  KEY `idx_export_status` (`is_exported_for_train`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立域-人类干涉纠偏池表';

-- A5. AI分析可视化资产表 ana_visual_asset
CREATE TABLE `ana_visual_asset` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `task_id` BIGINT NOT NULL COMMENT '分析任务ID',
  `case_id` BIGINT NOT NULL COMMENT '病例ID',
  `model_version` VARCHAR(64) NOT NULL COMMENT '生成此资产的模型版本',
  `asset_type_code` VARCHAR(32) NOT NULL COMMENT '资产类型(HEATMAP/MASK/OVERLAY)',
  `attachment_id` BIGINT NOT NULL COMMENT '指向物理对象存储的文件ID',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_asset_del` (`task_id`, `asset_type_code`, `deleted_flag`),
  KEY `idx_case` (`case_id`),
  KEY `idx_attachment` (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立域-AI分析可解释性可视化资产表';

-- A4.多模态风险研判表 med_risk_assessment_record
CREATE TABLE `med_risk_assessment_record` (
  `id` BIGINT NOT NULL,
  `case_id` BIGINT NOT NULL COMMENT '归口的定调大单',
  `patient_id` BIGINT NOT NULL COMMENT '所属人口标的',
  `overall_risk_level_code` VARCHAR(32) NOT NULL COMMENT '经过多项化合后出具的发展险恶指数评级(LOW/MEDIUM/HIGH)',
  `assessment_report_json` JSON NULL COMMENT '对内聚合后的多元因子计算结构展示明细',
  `recommended_cycle_days` INT NULL COMMENT '强制介入复验周期的冷酷计算推荐值(例如:15天内必须召回)',
  `assessed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '得出最后宣判的时间点',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case` (`case_id`),
  KEY `idx_patient_risk` (`patient_id`, `overall_risk_level_code`),
  KEY `idx_org_time` (`org_id`, `assessed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='特别新增核载点-整案量化与未来风险预案输出底座';

-- 26. 报告记录表 rpt_record
CREATE TABLE `rpt_record` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `report_no` VARCHAR(32) NOT NULL COMMENT '脱敏流转防猜序列号',
  `case_id` BIGINT NOT NULL COMMENT '关联事发病历',
  `patient_id` BIGINT NOT NULL,
  `attachment_id` BIGINT NULL COMMENT '指向最终成型并推走至 OSS 的那个不变得 PDF 脱敏或原件',
  `report_type_code` VARCHAR(32) NOT NULL DEFAULT 'DOCTOR' COMMENT '模式分类保护(DOCTOR/PATIENT)',
  `report_status_code` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '(DRAFT/FINAL/ARCHIVED)',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '重生成多版本时叠加',
  `summary_text` TEXT NULL COMMENT '核心精妙提取不涉及高敏的数据呈现预览段',
  `generated_at` DATETIME NULL COMMENT '机器拟定完稿时',
  `signed_at` DATETIME NULL COMMENT '主任/医师点击画押通过时',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reportno_del` (`report_no`, `deleted_flag`),
  KEY `idx_case_type` (`case_id`, `report_type_code`),
  KEY `idx_patient_generated` (`patient_id`, `generated_at`),
  KEY `idx_org_reportstatus` (`org_id`, `report_status_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者报告流转存案记事表';

-- 29. 随访计划表 fup_plan
CREATE TABLE `fup_plan` (
  `id` BIGINT NOT NULL,
  `plan_no` VARCHAR(32) NOT NULL COMMENT '全周期检索索引ID',
  `case_id` BIGINT NOT NULL,
  `patient_id` BIGINT NOT NULL,
  `plan_type_code` VARCHAR(32) NOT NULL DEFAULT 'ROUTINE' COMMENT '(ROUTINE/HIGH_RISK/RECHECK)',
  `plan_status_code` VARCHAR(32) NOT NULL DEFAULT 'PLANNED' COMMENT '启停用保护(PLANNED/ACTIVE/DONE/CANCELLED)',
  `next_followup_date` DATE NULL COMMENT '下回预判日',
  `interval_days` INT NULL COMMENT '步进流转间隔天数',
  `owner_user_id` BIGINT NULL COMMENT '包干责任挂号人ID',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_planno_del` (`plan_no`, `deleted_flag`),
  KEY `idx_case_status` (`case_id`, `plan_status_code`),
  KEY `idx_org_owner` (`org_id`, `owner_user_id`),
  KEY `idx_patient_date` (`patient_id`, `next_followup_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全局随访战略部署计划表';

-- 32. 消息短信站内防丢推送网关日志 msg_notify_record 
CREATE TABLE `msg_notify_record` (
  `id` BIGINT NOT NULL,
  `biz_module_code` VARCHAR(32) NOT NULL COMMENT '反向找寻追踪主体(如 FOLLOWUP/REPORT/CASE)',
  `biz_id` BIGINT NULL,
  `receiver_user_id` BIGINT NULL COMMENT '指向该站内人的系统数字身份(即使是病人也有数字体系身份ID)',
  `notify_type_code` VARCHAR(32) NOT NULL DEFAULT 'REMINDER' COMMENT '紧急分档(REMINDER/NOTICE/ALERT)',
  `channel_code` VARCHAR(32) NOT NULL DEFAULT 'IN_APP' COMMENT '(IN_APP/SMS/EMAIL/WECHAT)',
  `title` VARCHAR(255) NULL COMMENT '摘要文(禁止放真名真地)',
  `content_summary` VARCHAR(1000) NULL COMMENT '仅存局部或首身脱敏，长文由下游外包负责自留底保护',
  `send_status_code` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '重传/弃掷校验(PENDING/SENT/FAILED)',
  `sent_at` DATETIME NULL COMMENT '网关出界时间戳',
  `failure_reason` VARCHAR(500) NULL COMMENT '异常回调码分析补全',
  `org_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_module_code`, `biz_id`),
  KEY `idx_receiver_sentat` (`receiver_user_id`, `sent_at`),
  KEY `idx_sendstatus_sentat` (`send_status_code`, `sent_at`),
  KEY `idx_org_notifytype` (`org_id`, `notify_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知投递信使轨迹记录集';
