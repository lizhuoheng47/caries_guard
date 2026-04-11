-- 13. 患者主档表 pat_patient
CREATE TABLE `pat_patient` (
  `id` BIGINT NOT NULL COMMENT '患者主键',
  `patient_no` VARCHAR(32) NOT NULL COMMENT '患者编号(平台唯一)',
  
  -- "姓名"
  `patient_name_enc` VARCHAR(255) NOT NULL COMMENT '患者姓名 AES',
  `patient_name_hash` VARCHAR(128) NOT NULL COMMENT '患者姓名 Hash(去重)',
  `patient_name_masked` VARCHAR(64) NOT NULL COMMENT '患者姓名 脱敏展示',

  `gender_code` VARCHAR(16) NULL DEFAULT 'UNKNOWN' COMMENT '性别代码',
  
  -- 出生日期
  `birth_date_enc` VARCHAR(255) NULL COMMENT '出生日期密文',
  `birth_date_hash` VARCHAR(128) NULL COMMENT '出生日期 Hash',
  `birth_date_masked` VARCHAR(64) NULL COMMENT '出生日期(YYYY-MM-**)',
  
  `age` INT NULL COMMENT '年龄',
  
  -- "手机号"
  `phone_enc` VARCHAR(255) NULL COMMENT '手机号 AES',
  `phone_hash` VARCHAR(128) NULL COMMENT '手机号 Hash',
  `phone_masked` VARCHAR(32) NULL COMMENT '手机号 脱敏展示',
  
  -- "证件号"
  `id_card_enc` VARCHAR(255) NULL COMMENT '证件号 AES',
  `id_card_hash` VARCHAR(128) NULL COMMENT '证件号 Hash(严格去重核心)',
  `id_card_masked` VARCHAR(64) NULL COMMENT '证件号 脱敏展示',
  
  `source_code` VARCHAR(32) NOT NULL DEFAULT 'OUTPATIENT' COMMENT '来源(OUTPATIENT/CAMPUS_SCREENING...)',
  `first_visit_date` DATE NULL COMMENT '首次就诊日期',
  `privacy_level_code` VARCHAR(16) NOT NULL DEFAULT 'L4' COMMENT '隐私级别(L1-L4)',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-正常, id-删除)',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patientno_del` (`patient_no`, `deleted_flag`),
  KEY `idx_org_phone_hash` (`org_id`, `phone_hash`),
  KEY `idx_org_idcard_hash` (`org_id`, `id_card_hash`),
  KEY `idx_source_time` (`source_code`, `first_visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者主档表';

-- 14. 监护人信息表 pat_guardian
CREATE TABLE `pat_guardian` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  
  -- "监护人姓名"
  `guardian_name_enc` VARCHAR(255) NOT NULL,
  `guardian_name_hash` VARCHAR(128) NOT NULL,
  `guardian_name_masked` VARCHAR(64) NOT NULL,
  
  `relation_code` VARCHAR(32) NOT NULL DEFAULT 'PARENT' COMMENT '关系(PARENT/GRANDPARENT/SPOUSE/OTHER)',
  
  -- "监护人手机号"
  `phone_enc` VARCHAR(255) NULL,
  `phone_hash` VARCHAR(128) NULL,
  `phone_masked` VARCHAR(32) NULL,
  
  `certificate_type_code` VARCHAR(32) NULL COMMENT '证件类型',
  
  -- "监护人证件号"
  `certificate_no_enc` VARCHAR(255) NULL,
  `certificate_no_hash` VARCHAR(128) NULL,
  `certificate_no_masked` VARCHAR(64) NULL,
  
  `is_primary` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否主要联系人',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guardian_phone_del` (`patient_id`, `phone_hash`, `deleted_flag`),
  KEY `idx_patient_primary` (`patient_id`, `is_primary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监护人信息表';

-- 15. 患者扩展画像表 pat_profile
CREATE TABLE `pat_profile` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `patient_id` BIGINT NOT NULL COMMENT '关联患者的ID',
  `brushing_freq_per_day` TINYINT NULL COMMENT '每日刷牙频次',
  `sugar_diet_level_code` VARCHAR(32) NULL COMMENT '含糖饮食等级(LOW/MEDIUM/HIGH)',
  `fluoride_use_flag` CHAR(1) NULL COMMENT '含氟牙膏使用史(0/1)',
  `family_caries_history_flag` CHAR(1) NULL COMMENT '家族史(0/1)',
  `orthodontic_history_flag` CHAR(1) NULL COMMENT '正畸史(0/1)',
  `previous_caries_count` INT NULL COMMENT '既往病次数',
  `last_dental_check_months` INT NULL COMMENT '距离上次看牙过去月数',
  `smoking_flag` CHAR(1) NULL COMMENT '吸烟(0/1)',
  `drinking_flag` CHAR(1) NULL COMMENT '饮酒(0/1)',
  `oral_hygiene_level_code` VARCHAR(32) NULL COMMENT '腔生等级(GOOD/NORMAL/POOR)',
  `allergy_info` VARCHAR(255) NULL COMMENT '过敏信息(脱敏存储)',
  `chronic_disease_desc` VARCHAR(255) NULL COMMENT '慢病史描述',
  `profile_source_code` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '画像收集方式',
  `effective_date` DATE NOT NULL COMMENT '生效校验日期',
  `ext_json` JSON NULL COMMENT '结构化背景扩展',
  `org_id` BIGINT NOT NULL COMMENT '数据归属机构',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '防冲突基准隔离字段',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '录入人员ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  `updated_by` BIGINT NULL COMMENT '更新人员',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_effdate_del` (`patient_id`, `effective_date`, `deleted_flag`),
  KEY `idx_org_time` (`org_id`, `effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者扩展画像表';

-- 16. 就诊记录表 med_visit
CREATE TABLE `med_visit` (
  `id` BIGINT NOT NULL COMMENT '就诊的主键',
  `visit_no` VARCHAR(32) NOT NULL COMMENT '就诊层面的编号(如HIS系统流水号)',
  `patient_id` BIGINT NOT NULL COMMENT '关联患者',
  `department_id` BIGINT NULL COMMENT '科室挂靠',
  `doctor_user_id` BIGINT NULL COMMENT '接诊医生标识',
  `visit_type_code` VARCHAR(32) NOT NULL DEFAULT 'OUTPATIENT' COMMENT '形态(OUTPATIENT/SCREENING/RECHECK/FOLLOWUP)',
  `visit_date` DATETIME NOT NULL COMMENT '到院时间',
  `complaint` VARCHAR(500) NULL COMMENT '到院时初步主诉',
  `triage_level_code` VARCHAR(32) NULL DEFAULT 'NORMAL' COMMENT '分诊紧急度',
  `source_channel_code` VARCHAR(32) NULL DEFAULT 'MANUAL' COMMENT '系统来源渠道',
  `org_id` BIGINT NOT NULL COMMENT '机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '当前记录使用状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '删除基准(逻辑标识)',
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitno_del` (`visit_no`, `deleted_flag`),
  KEY `idx_patient_visitdate` (`patient_id`, `visit_date`),
  KEY `idx_dept_visitdate` (`org_id`, `department_id`, `visit_date`),
  KEY `idx_doc_visitdate` (`doctor_user_id`, `visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='就诊记录表';


-- 17. 病例主表 med_case
CREATE TABLE `med_case` (
  `id` BIGINT NOT NULL COMMENT '病例核心主键',
  `case_no` VARCHAR(32) NOT NULL COMMENT '全平台流转病例标号',
  `visit_id` BIGINT NOT NULL COMMENT '上游包裹其在内的就诊事件流',
  `patient_id` BIGINT NOT NULL COMMENT '主语患者身份',
  `case_title` VARCHAR(128) NULL COMMENT '自定义标题(如：三年级A班11号学生秋季普查)',
  `case_type_code` VARCHAR(32) NOT NULL DEFAULT 'CARIES_SCREENING' COMMENT '病例模型',
  `case_status_code` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '状态码基石',
  `priority_code` VARCHAR(32) NULL DEFAULT 'NORMAL' COMMENT '工作台插队优先级等',
  `chief_complaint` VARCHAR(500) NULL COMMENT '当前病例范畴内的主诉',
  `clinical_notes` TEXT NULL COMMENT '核心临床判断密文域或者脱敏域(因可能极长，由业务解密处理)',
  `onset_date` DATE NULL COMMENT '疑似发病起始',
  `first_diagnosis_at` DATETIME NULL COMMENT '系统首次判决时间',
  `attending_doctor_id` BIGINT NULL COMMENT '确诊干预责任人',
  `screener_user_id` BIGINT NULL COMMENT '现场上传拍摄责任人',
  `report_ready_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '用于报告域判定入口开关',
  `followup_required_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '用于随访域触发开关',
  `closed_at` DATETIME NULL COMMENT '结束业务封口时间戳',
  `org_id` BIGINT NOT NULL COMMENT '隔离环境',

  `version_no` INT NOT NULL DEFAULT 1 COMMENT '乐观并发版本号',
  
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '当前结构可读写态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑抗阻保护',
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_caseno_del` (`case_no`, `deleted_flag`),
  KEY `idx_patient_casestatus` (`patient_id`, `case_status_code`),
  KEY `idx_attending_casestatus` (`attending_doctor_id`, `case_status_code`),
  KEY `idx_org_casestatus` (`org_id`, `case_status_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病例主表';


-- 18. 病例状态流转日志表 med_case_status_log
CREATE TABLE `med_case_status_log` (
  `id` BIGINT NOT NULL COMMENT '留底日志标识',
  `case_id` BIGINT NOT NULL COMMENT '受限病例标识',
  `from_status_code` VARCHAR(32) NULL COMMENT '拨表前源枚举',
  `to_status_code` VARCHAR(32) NOT NULL COMMENT '转入目的枚举',
  `changed_by` BIGINT NULL COMMENT '推子操作人',
  `change_reason` VARCHAR(255) NULL COMMENT '标准留底批注',
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事发时刻',
  `org_id` BIGINT NOT NULL COMMENT '组织约束环境',
  PRIMARY KEY (`id`),
  KEY `idx_case_changedat` (`case_id`, `changed_at`),
  KEY `idx_org_changedat` (`org_id`, `changed_at`),
  KEY `idx_to_status_time` (`to_status_code`, `changed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病例状态流转日志表';


-- 19. 病例诊断结论表 med_case_diagnosis
CREATE TABLE `med_case_diagnosis` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `case_id` BIGINT NOT NULL COMMENT '目标病例主体ID',
  `diagnosis_type_code` VARCHAR(32) NOT NULL DEFAULT 'CARIES' COMMENT '类型',
  `diagnosis_name` VARCHAR(128) NOT NULL COMMENT '名称',
  `severity_code` VARCHAR(32) NULL COMMENT '等级',
  `diagnosis_basis` VARCHAR(255) NULL COMMENT '辅助判定源',
  `diagnosis_desc` TEXT NULL COMMENT '核心密文详情块',
  `treatment_advice` TEXT NULL COMMENT '临床反馈建议(或交由大模型合成展示)',
  `review_doctor_id` BIGINT NULL COMMENT '审核责任人',
  `review_time` DATETIME NULL COMMENT '判决时间',
  `is_final` CHAR(1) NOT NULL DEFAULT '1' COMMENT '定稿版本标致',
  `org_id` BIGINT NOT NULL COMMENT '环境',
  
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本控制数字(多医同修互斥防护)',

  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '当前激活状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '冲突隔离位',
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_final` (`case_id`, `is_final`),
  KEY `idx_review_doctor` (`review_doctor_id`, `review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病例诊断结论表';


-- 20. 病例牙位记录表 med_case_tooth_record
CREATE TABLE `med_case_tooth_record` (
  `id` BIGINT NOT NULL COMMENT '单牙位报告项的标示',
  `case_id` BIGINT NOT NULL COMMENT '所依附的病例容器',
  
  `source_image_id` BIGINT NULL COMMENT '指向 med_image_file.id 的实锤存留影像位点',

  `tooth_code` VARCHAR(8) NOT NULL COMMENT 'FDI 两数国际码',
  `tooth_surface_code` VARCHAR(32) NULL COMMENT '面位标记',
  `issue_type_code` VARCHAR(32) NOT NULL DEFAULT 'CARIES' COMMENT '发现归属大病灶类别',
  `severity_code` VARCHAR(32) NULL COMMENT '深度表征',
  `finding_desc` VARCHAR(500) NULL COMMENT '文字补充',
  `suggestion` VARCHAR(500) NULL COMMENT '处方意图',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '陈设展示规则排序',
  `reviewed_by` BIGINT NULL COMMENT '核查负责人(在人机博弈中极有帮助)',
  `reviewed_at` DATETIME NULL COMMENT '核查确认的留底',
  `org_id` BIGINT NOT NULL COMMENT '组约束',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '隔离位',
  `remark` VARCHAR(500) NULL,
  `created_by` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tooth_record_del` (`case_id`, `tooth_code`, `tooth_surface_code`, `issue_type_code`, `deleted_flag`),
  KEY `idx_case_tooth` (`case_id`, `tooth_code`),
  KEY `idx_org_severity` (`org_id`, `severity_code`),
  KEY `idx_issue` (`issue_type_code`),
  KEY `idx_source_image` (`source_image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病例结构化牙位记录溯源表';

