-- CariesGuard database baseline schema.
-- Squashed from historical Flyway migrations into a single baseline migration.
-- Existing databases with old flyway_schema_history must be recreated or cleaned before using this baseline.

-- 1. 系统角色表 sys_role
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL COMMENT '角色主键',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `role_sort` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `data_scope_code` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围(ALL/ORG/DEPT/SELF/CUSTOM)',
  `is_builtin` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否内置角色(0/1)',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '记录状态(ACTIVE/DISABLED)',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除, id-已删除)',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人用户ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rolecode_del` (`role_code`, `deleted_flag`),
  KEY `idx_org_status` (`org_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';

-- 2. 组织/部门表 sys_dept
CREATE TABLE `sys_dept` (
  `id` BIGINT NOT NULL COMMENT '部门主键',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父级ID',
  `ancestor_path` VARCHAR(500) NULL COMMENT '祖级路径',
  `dept_code` VARCHAR(64) NOT NULL COMMENT '部门/机构编码',
  `dept_name` VARCHAR(128) NOT NULL COMMENT '部门/机构名称',
  `dept_category_code` VARCHAR(32) NOT NULL DEFAULT 'ORG' COMMENT '节点类别(ORG/CAMPUS/CLINIC/DEPT/TEAM)',
  `org_type_code` VARCHAR(32) NULL COMMENT '机构类型',
  `leader_user_id` BIGINT NULL COMMENT '负责人用户ID',
  `phone` VARCHAR(32) NULL COMMENT '联系电话',
  `order_num` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '记录状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除, id-已删除)',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人用户ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deptcode_del` (`dept_code`, `deleted_flag`),
  KEY `idx_parent_order` (`parent_id`, `order_num`),
  KEY `idx_ancestor` (`ancestor_path`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织/部门表';

-- 3. 系统用户表 sys_user
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL COMMENT '用户主键',
  `dept_id` BIGINT NULL COMMENT '所属部门ID',
  `user_no` VARCHAR(32) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  
  -- "姓名"
  `real_name_enc` VARCHAR(255) NOT NULL COMMENT '真实姓名 AES 密文存储',
  `real_name_hash` VARCHAR(128) NOT NULL COMMENT '真实姓名 Hash 摘要(查询用)',
  `real_name_masked` VARCHAR(64) NOT NULL COMMENT '真实姓名 脱敏展示',
  
  `nick_name` VARCHAR(64) NULL COMMENT '昵称/展示名',
  `user_type_code` VARCHAR(32) NOT NULL DEFAULT 'DOCTOR' COMMENT '用户类型',
  `gender_code` VARCHAR(16) NULL DEFAULT 'UNKNOWN' COMMENT '性别代码',
  
  -- "手机号"
  `phone_enc` VARCHAR(255) NULL COMMENT '手机号 AES 密文存储',
  `phone_hash` VARCHAR(128) NULL COMMENT '手机号 Hash 摘要(唯一精准定位)',
  `phone_masked` VARCHAR(32) NULL COMMENT '手机号 前端脱敏展示',
  
  -- "邮箱"
  `email_enc` VARCHAR(255) NULL COMMENT '邮箱 AES 密文存储',
  `email_hash` VARCHAR(128) NULL COMMENT '邮箱 Hash 摘要',
  `email_masked` VARCHAR(128) NULL COMMENT '邮箱 前端脱敏展示',
  
  `avatar_url` VARCHAR(255) NULL COMMENT '头像地址',
  `certificate_type_code` VARCHAR(32) NULL COMMENT '证件类型',

  `certificate_no_enc` VARCHAR(255) NULL COMMENT '证件号 AES 密文存储',
  `certificate_no_hash` VARCHAR(128) NULL COMMENT '证件号 Hash 摘要',
  `certificate_no_masked` VARCHAR(64) NULL COMMENT '证件号前端脱敏展示',
  
  `last_login_at` DATETIME NULL COMMENT '最后登录时间',
  `pwd_updated_at` DATETIME NULL COMMENT '密码更新时间',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '记录状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删除, id-已删除)',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username_del` (`username`, `deleted_flag`),
  UNIQUE KEY `uk_userno_del` (`user_no`, `deleted_flag`),
  KEY `idx_dept_status` (`dept_id`, `status`),
  KEY `idx_org_usertype` (`org_id`, `user_type_code`),
  KEY `idx_phone_hash` (`phone_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

-- 4. 岗位表 sys_post
CREATE TABLE `sys_post` (
  `id` BIGINT NOT NULL COMMENT '岗位主键',
  `post_code` VARCHAR(64) NOT NULL COMMENT '岗位编码',
  `post_name` VARCHAR(64) NOT NULL COMMENT '岗位名称',
  `post_sort` INT NOT NULL DEFAULT 0 COMMENT '岗位排序',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-未删除, id-删除)',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_postcode_del` (`post_code`, `deleted_flag`),
  KEY `idx_org_status` (`org_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';

-- 5. 系统菜单权限表 sys_menu
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL COMMENT '菜单主键',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父级菜单ID',
  `menu_name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `menu_type_code` VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT '菜单类型(DIR/MENU/BUTTON/API)',
  `route_path` VARCHAR(255) NULL COMMENT '前端路由路径',
  `component_path` VARCHAR(255) NULL COMMENT '前端组件路径',
  `permission_code` VARCHAR(128) NULL COMMENT '权限标识',
  `icon` VARCHAR(64) NULL COMMENT '图标',
  `visible_flag` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否显示(0/1)',
  `cache_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否缓存',
  `order_num` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_del` (`permission_code`, `deleted_flag`),
  KEY `idx_parent_order` (`parent_id`, `order_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单权限表';

-- 6. 用户角色关联表 sys_user_role
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除(0-正常, id-删除)',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_del` (`user_id`, `role_id`, `deleted_flag`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

-- 7. 用户岗位关联表 sys_user_post
CREATE TABLE `sys_user_post` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '岗位ID',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post_del` (`user_id`, `post_id`, `deleted_flag`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户岗位关联表';

-- 8. 角色菜单关联表 sys_role_menu
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu_del` (`role_id`, `menu_id`, `deleted_flag`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';

-- 9. 自动配置/参数配置 sys_config
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `config_key` VARCHAR(128) NOT NULL COMMENT '参数键(系统唯一)',
  `config_name` VARCHAR(128) NOT NULL COMMENT '参数名称',
  `config_value` TEXT NOT NULL COMMENT '参数值(可为JSON)',
  `value_type_code` VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '值类型(STRING/NUMBER/BOOL/JSON)',
  `sensitive_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否敏感(0/1)',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key_del` (`config_key`, `deleted_flag`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统参数配置表';

-- 10. 数据权限规则表 sys_data_permission_rule (JSON 虚拟列优化)
CREATE TABLE `sys_data_permission_rule` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `module_code` VARCHAR(64) NOT NULL COMMENT '模块编码(如 CASE/REPORT)',
  `scope_type_code` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '范围类型(ALL/ORG/DEPT/SELF/CUSTOM)',
  `dept_ids_json` JSON NULL COMMENT '自定义部门集合JSON配置',
  `self_only_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否仅本人数据',
  `column_mask_policy_json` JSON NULL COMMENT '列脱敏策略配置',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_mod_scope_del` (`role_id`, `module_code`, `scope_type_code`, `deleted_flag`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据权限规则表';

-- 11.表 sys_dict_type
CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `dict_type` VARCHAR(64) NOT NULL COMMENT '字典类型编码',
  `dict_name` VARCHAR(128) NOT NULL COMMENT '字典名称',
  `system_flag` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否系统内置(0/1)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_del` (`dict_type`, `deleted_flag`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';

-- 12.表 sys_dict_item
CREATE TABLE `sys_dict_item` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `dict_type_id` BIGINT NOT NULL COMMENT '字典类型ID',
  `item_label` VARCHAR(128) NOT NULL COMMENT '显示标签(中文展示)',
  `item_value` VARCHAR(64) NOT NULL COMMENT '持久化取值(固定英文)',
  `item_code` VARCHAR(64) NULL COMMENT '扩展编码',
  `item_sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `css_class` VARCHAR(64) NULL COMMENT '样式类',
  `tag_type` VARCHAR(32) NULL COMMENT '标签类型',
  `is_default` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否默认项',
  `ext_json` JSON NULL COMMENT '扩展结构',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dicttype_itemval_del` (`dict_type_id`, `item_value`, `deleted_flag`),
  KEY `idx_dicttype_sort` (`dict_type_id`, `item_sort`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项表';

-- Seed built-in dictionary types.
INSERT INTO `sys_dict_type` (
    `id`,
    `dict_type`,
    `dict_name`,
    `system_flag`,
    `sort_order`,
    `org_id`,
    `status`,
    `deleted_flag`,
    `created_by`,
    `created_at`
) VALUES
    (1001, 'sys_gender', '性别', '1', 1, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
    (1002, 'sys_yes_no', '是/否', '1', 2, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
    (1003, 'med_case_status', '病例状态', '1', 11, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
    (1004, 'sys_user_type', '用户类型', '1', 6, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP);

-- Seed built-in dictionary items.
INSERT INTO `sys_dict_item` (
    `id`,
    `dict_type_id`,
    `item_label`,
    `item_value`,
    `item_sort`,
    `is_default`,
    `org_id`,
    `status`,
    `deleted_flag`
) VALUES
    (2001, 1001, '男', 'MALE', 1, '0', 0, 'ACTIVE', 0),
    (2002, 1001, '女', 'FEMALE', 2, '0', 0, 'ACTIVE', 0),
    (2003, 1001, '未知', 'UNKNOWN', 3, '1', 0, 'ACTIVE', 0),
    (2004, 1002, '是', '1', 1, '0', 0, 'ACTIVE', 0),
    (2005, 1002, '否', '0', 2, '1', 0, 'ACTIVE', 0),
    (2006, 1004, '系统超级管理员', 'ADMIN', 1, '0', 0, 'ACTIVE', 0),
    (2007, 1004, '机构管理员', 'ORG_ADMIN', 2, '0', 0, 'ACTIVE', 0),
    (2008, 1004, '医生', 'DOCTOR', 3, '1', 0, 'ACTIVE', 0),
    (2009, 1004, '筛查员', 'SCREENER', 4, '0', 0, 'ACTIVE', 0),
    (2010, 1004, '患者', 'PATIENT', 5, '0', 0, 'ACTIVE', 0),
    (3001, 1003, '已创建', 'CREATED', 1, '1', 0, 'ACTIVE', 0),
    (3002, 1003, '待影像质检', 'QC_PENDING', 2, '0', 0, 'ACTIVE', 0),
    (3003, 1003, 'AI分析中', 'ANALYZING', 3, '0', 0, 'ACTIVE', 0),
    (3004, 1003, '待医生复核', 'REVIEW_PENDING', 4, '0', 0, 'ACTIVE', 0),
    (3005, 1003, '报告已就绪', 'REPORT_READY', 5, '0', 0, 'ACTIVE', 0),
    (3006, 1003, '需要随访', 'FOLLOWUP_REQUIRED', 6, '0', 0, 'ACTIVE', 0),
    (3007, 1003, '已关闭', 'CLOSED', 7, '0', 0, 'ACTIVE', 0),
    (3008, 1003, '已取消', 'CANCELLED', 8, '0', 0, 'ACTIVE', 0);

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

INSERT INTO sys_dept (
    id,
    parent_id,
    ancestor_path,
    dept_code,
    dept_name,
    dept_category_code,
    org_type_code,
    leader_user_id,
    phone,
    order_num,
    org_id,
    status,
    deleted_flag,
    remark,
    created_by,
    created_at,
    updated_by,
    updated_at
)
SELECT
    100001,
    0,
    '0',
    'ROOT_ORG',
    'Default Organization',
    'ORG',
    'HOSPITAL',
    NULL,
    NULL,
    0,
    100001,
    'ACTIVE',
    0,
    'Development seed data',
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dept
    WHERE dept_code = 'ROOT_ORG'
      AND deleted_flag = 0
);

INSERT INTO sys_role (
    id,
    role_code,
    role_name,
    role_sort,
    data_scope_code,
    is_builtin,
    org_id,
    status,
    deleted_flag,
    remark,
    created_by,
    created_at,
    updated_by,
    updated_at
)
SELECT
    100001,
    'SYS_ADMIN',
    'System Administrator',
    1,
    'ALL',
    '1',
    100001,
    'ACTIVE',
    0,
    'Development seed data',
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role
    WHERE role_code = 'SYS_ADMIN'
      AND deleted_flag = 0
);

INSERT INTO sys_user (
    id,
    dept_id,
    user_no,
    username,
    password_hash,
    real_name_enc,
    real_name_hash,
    real_name_masked,
    nick_name,
    user_type_code,
    gender_code,
    phone_enc,
    phone_hash,
    phone_masked,
    email_enc,
    email_hash,
    email_masked,
    avatar_url,
    certificate_type_code,
    certificate_no_enc,
    certificate_no_hash,
    certificate_no_masked,
    last_login_at,
    pwd_updated_at,
    org_id,
    status,
    deleted_flag,
    remark,
    created_by,
    created_at,
    updated_by,
    updated_at
)
SELECT
    100001,
    100001,
    'U100001',
    'admin',
    '$2a$10$lp.QpmcFtDn2RyRVkcgX7.w/vx.AgcQmi6zTpAztJ/duGHb5ZMK7q',
    'DEV_ADMIN_ENC',
    'DEV_ADMIN_HASH',
    'Admin',
    'Admin',
    'ADMIN',
    'UNKNOWN',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    100001,
    'ACTIVE',
    0,
    'Default development administrator',
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'admin'
      AND deleted_flag = 0
);

INSERT INTO sys_user_role (
    id,
    user_id,
    role_id,
    org_id,
    deleted_flag,
    created_by,
    created_at
)
SELECT
    100001,
    100001,
    100001,
    100001,
    0,
    100001,
    CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role
    WHERE user_id = 100001
      AND role_id = 100001
      AND deleted_flag = 0
);

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

CREATE TABLE `med_image_quality_check` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `image_id` BIGINT NOT NULL COMMENT '影像ID',
  `case_id` BIGINT NOT NULL COMMENT '病例ID',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `check_type_code` VARCHAR(32) NOT NULL DEFAULT 'AUTO' COMMENT '质检类型(AUTO/MANUAL)',
  `check_result_code` VARCHAR(32) NOT NULL DEFAULT 'REVIEW' COMMENT '质检结果(PASS/REJECT/REVIEW)',
  `quality_score` INT NULL COMMENT '综合评分(0-100)',
  `blur_score` INT NULL COMMENT '模糊评分(0-100)',
  `exposure_score` INT NULL COMMENT '曝光评分(0-100)',
  `integrity_score` INT NULL COMMENT '完整度评分(0-100)',
  `occlusion_score` INT NULL COMMENT '遮挡评分(0-100)',
  `issue_codes_json` JSON NULL COMMENT '问题码列表',
  `suggestion_text` VARCHAR(500) NULL COMMENT '建议说明',
  `current_flag` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否当前有效记录(0/1)',
  `checked_by` BIGINT NULL COMMENT '质检人用户ID',
  `checked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '质检时间',
  `org_id` BIGINT NOT NULL COMMENT '所属机构ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `deleted_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_image_checked_at` (`image_id`, `checked_at`),
  KEY `idx_case_checked_at` (`case_id`, `checked_at`),
  KEY `idx_org_result` (`org_id`, `check_result_code`),
  KEY `idx_image_current` (`image_id`, `current_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='影像质量检查表';

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

ALTER TABLE `ana_task_record`
    ADD KEY `idx_task_status_created_at` (`task_status_code`, `created_at`),
    ADD KEY `idx_org_created_at` (`org_id`, `created_at`);

UPDATE sys_user
SET password_hash = '$2a$10$dgo85fF5uEjzlO8USrYMzec7DQ2woxBy6qLXX2U5w6MUq6WKS06EK',
    pwd_updated_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin'
  AND deleted_flag = 0;

-- These fields are extracted from raw_result_json during AI callback write-back
-- to support list views, reports, dashboard, and competition defense without JSON parsing.

ALTER TABLE ana_result_summary
  ADD COLUMN overall_highest_severity VARCHAR(32) DEFAULT NULL COMMENT '最高严重程度 (e.g. C1, C2, C3)',
  ADD COLUMN uncertainty_score DECIMAL(5,4) DEFAULT NULL COMMENT '不确定性评分 0~1',
  ADD COLUMN review_suggested_flag CHAR(1) DEFAULT '0' COMMENT '是否建议复核 0-否 1-是';

-- When a FAILED task is retried, the new task records this field pointing to the original.
-- First-time tasks have NULL. This enables retry lineage tracking for auditing and defense.

ALTER TABLE ana_task_record
  ADD COLUMN retry_from_task_id BIGINT DEFAULT NULL COMMENT '重试来源任务ID，首次任务为NULL';

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

ALTER TABLE ana_task_record
    ADD COLUMN trace_id VARCHAR(64) NULL COMMENT 'AI service trace id' AFTER error_message,
    ADD COLUMN inference_millis BIGINT NULL COMMENT 'AI inference duration in milliseconds' AFTER trace_id;

ALTER TABLE ana_correction_feedback
    ADD COLUMN training_candidate_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Whether this feedback can enter training candidate pool' AFTER is_exported_for_train,
    ADD COLUMN desensitized_export_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Whether this feedback has passed desensitized export' AFTER training_candidate_flag,
    ADD COLUMN dataset_snapshot_no VARCHAR(64) NULL COMMENT 'Dataset snapshot number after approved export' AFTER desensitized_export_flag,
    ADD COLUMN review_status_code VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Training admission review status' AFTER dataset_snapshot_no,
    ADD COLUMN reviewed_by BIGINT NULL COMMENT 'Training admission reviewer user id' AFTER review_status_code,
    ADD COLUMN reviewed_at DATETIME NULL COMMENT 'Training admission reviewed time' AFTER reviewed_by;

CREATE TABLE ana_model_version_registry (
    id BIGINT NOT NULL COMMENT 'Primary key',
    model_code VARCHAR(64) NOT NULL COMMENT 'Model code, for example caries-detector',
    model_version VARCHAR(64) NOT NULL COMMENT 'Published or candidate model version',
    model_type_code VARCHAR(32) NOT NULL COMMENT 'Model type, for example DETECTION/SEGMENTATION/RISK',
    approved_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Whether this model version is approved for production use',
    approved_by BIGINT NULL COMMENT 'Approver user id',
    approved_at DATETIME NULL COMMENT 'Approval time',
    status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE' COMMENT 'CANDIDATE/EVALUATING/APPROVED/RETIRED',
    remark VARCHAR(500) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Creator user id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_by BIGINT NULL COMMENT 'Updater user id',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_version (model_code, model_version),
    KEY idx_model_status (model_code, status),
    KEY idx_approved (approved_flag, approved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI model version governance registry';

INSERT IGNORE INTO ana_model_version_registry (
    id, model_code, model_version, model_type_code, approved_flag, approved_by, approved_at,
    status, remark, created_by, created_at, updated_by, updated_at
) VALUES (
    300001, 'caries-detector', 'caries-v1', 'DETECTION', '1', 100001, CURRENT_TIMESTAMP,
    'APPROVED', 'Default local Java/Python integration model version', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP
);

INSERT IGNORE INTO sys_role (
    id, role_code, role_name, role_sort, data_scope_code, is_builtin, org_id, status,
    deleted_flag, remark, created_by, created_at, updated_by, updated_at
) VALUES
    (100101, 'ORG_ADMIN', 'Organization Administrator', 2, 'ORG', '1', 100001, 'ACTIVE', 0, 'Default business role seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (100102, 'DOCTOR', 'Doctor', 3, 'SELF', '1', 100001, 'ACTIVE', 0, 'Default business role seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (100103, 'SCREENER', 'Screener', 4, 'SELF', '1', 100001, 'ACTIVE', 0, 'Default business role seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP);

INSERT IGNORE INTO sys_menu (
    id, parent_id, menu_name, menu_type_code, route_path, component_path, permission_code,
    icon, visible_flag, cache_flag, order_num, org_id, status, deleted_flag, remark,
    created_by, created_at, updated_by, updated_at
) VALUES
    (200001, 0, 'Patient Management', 'MENU', '/patients', 'patient/index', 'patient:view', 'patient', '1', '0', 10, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200002, 0, 'Visit Management', 'MENU', '/visits', 'visit/index', 'visit:view', 'visit', '1', '0', 20, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200003, 0, 'Case Management', 'MENU', '/cases', 'case/index', 'case:view', 'case', '1', '0', 30, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200004, 0, 'Image Management', 'MENU', '/images', 'image/index', 'image:view', 'image', '1', '0', 40, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200005, 0, 'Analysis Tasks', 'MENU', '/analysis/tasks', 'analysis/task-index', 'analysis:view', 'analysis', '1', '0', 50, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200006, 0, 'Report Management', 'MENU', '/reports', 'report/index', 'report:view', 'report', '1', '0', 60, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200007, 0, 'Follow-up Management', 'MENU', '/followups', 'followup/index', 'followup:view', 'followup', '1', '0', 70, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200008, 0, 'Dashboard Statistics', 'MENU', '/dashboard', 'dashboard/index', 'dashboard:view', 'dashboard', '1', '0', 80, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (200009, 0, 'AI Ops Dashboard', 'MENU', '/dashboard/model-runtime', 'dashboard/model-runtime', 'dashboard:ops:view', 'dashboard', '1', '0', 90, 100001, 'ACTIVE', 0, 'Default menu seed', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP);

INSERT IGNORE INTO sys_role_menu (id, role_id, menu_id, org_id, deleted_flag, created_by, created_at) VALUES
    (210001, 100001, 200001, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210002, 100001, 200002, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210003, 100001, 200003, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210004, 100001, 200004, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210005, 100001, 200005, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210006, 100001, 200006, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210007, 100001, 200007, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210008, 100001, 200008, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210009, 100001, 200009, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210101, 100101, 200001, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210102, 100101, 200002, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210103, 100101, 200003, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210104, 100101, 200004, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210105, 100101, 200005, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210106, 100101, 200006, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210107, 100101, 200007, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210108, 100101, 200008, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210109, 100101, 200009, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210201, 100102, 200001, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210202, 100102, 200002, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210203, 100102, 200003, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210204, 100102, 200004, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210205, 100102, 200005, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210206, 100102, 200006, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210207, 100102, 200007, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210208, 100102, 200008, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210209, 100102, 200009, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210301, 100103, 200001, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210302, 100103, 200002, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210303, 100103, 200003, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210304, 100103, 200004, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210305, 100103, 200005, 100001, 0, 100001, CURRENT_TIMESTAMP),
    (210306, 100103, 200008, 100001, 0, 100001, CURRENT_TIMESTAMP);

INSERT IGNORE INTO sys_data_permission_rule (
    id, role_id, module_code, scope_type_code, dept_ids_json, self_only_flag, column_mask_policy_json,
    org_id, status, deleted_flag, remark, created_by, created_at, updated_by, updated_at
) VALUES
    (220001, 100101, 'PATIENT', 'ORG', NULL, '0', JSON_OBJECT('patientName', 'MASKED', 'phone', 'MASKED', 'idCard', 'MASKED'), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220002, 100101, 'VISIT', 'ORG', NULL, '0', JSON_OBJECT('patientName', 'MASKED'), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220003, 100101, 'CASE', 'ORG', NULL, '0', JSON_OBJECT('patientName', 'MASKED'), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220004, 100101, 'IMAGE', 'ORG', NULL, '0', JSON_OBJECT('originalName', 'MASKED'), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220005, 100101, 'ANALYSIS', 'ORG', NULL, '0', JSON_OBJECT(), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220006, 100101, 'REPORT', 'ORG', NULL, '0', JSON_OBJECT('patientName', 'MASKED'), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220007, 100101, 'FOLLOWUP', 'ORG', NULL, '0', JSON_OBJECT('patientName', 'MASKED', 'phone', 'MASKED'), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220008, 100101, 'DASHBOARD', 'ORG', NULL, '0', JSON_OBJECT(), 100001, 'ACTIVE', 0, 'ORG_ADMIN org scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220101, 100102, 'PATIENT', 'SELF', NULL, '1', JSON_OBJECT('phone', 'MASKED', 'idCard', 'MASKED'), 100001, 'ACTIVE', 0, 'DOCTOR self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220102, 100102, 'CASE', 'SELF', NULL, '1', JSON_OBJECT('patientName', 'MASKED'), 100001, 'ACTIVE', 0, 'DOCTOR self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220103, 100102, 'IMAGE', 'SELF', NULL, '1', JSON_OBJECT('originalName', 'MASKED'), 100001, 'ACTIVE', 0, 'DOCTOR self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220104, 100102, 'ANALYSIS', 'SELF', NULL, '1', JSON_OBJECT(), 100001, 'ACTIVE', 0, 'DOCTOR self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220105, 100102, 'REPORT', 'SELF', NULL, '1', JSON_OBJECT('patientName', 'MASKED'), 100001, 'ACTIVE', 0, 'DOCTOR self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220106, 100102, 'FOLLOWUP', 'SELF', NULL, '1', JSON_OBJECT('phone', 'MASKED'), 100001, 'ACTIVE', 0, 'DOCTOR self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220107, 100102, 'DASHBOARD', 'ORG', NULL, '0', JSON_OBJECT(), 100001, 'ACTIVE', 0, 'DOCTOR dashboard org aggregate', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220201, 100103, 'PATIENT', 'SELF', NULL, '1', JSON_OBJECT('phone', 'MASKED', 'idCard', 'MASKED'), 100001, 'ACTIVE', 0, 'SCREENER self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220202, 100103, 'CASE', 'SELF', NULL, '1', JSON_OBJECT('patientName', 'MASKED'), 100001, 'ACTIVE', 0, 'SCREENER self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220203, 100103, 'IMAGE', 'SELF', NULL, '1', JSON_OBJECT('originalName', 'MASKED'), 100001, 'ACTIVE', 0, 'SCREENER self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220204, 100103, 'ANALYSIS', 'SELF', NULL, '1', JSON_OBJECT(), 100001, 'ACTIVE', 0, 'SCREENER self scope', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP),
    (220205, 100103, 'DASHBOARD', 'ORG', NULL, '0', JSON_OBJECT(), 100001, 'ACTIVE', 0, 'SCREENER dashboard org aggregate', 100001, CURRENT_TIMESTAMP, 100001, CURRENT_TIMESTAMP);

ALTER TABLE ana_visual_asset
    ADD COLUMN related_image_id BIGINT NULL COMMENT 'Source image id related to the generated visual asset' AFTER attachment_id,
    ADD COLUMN tooth_code VARCHAR(32) NULL COMMENT 'Tooth position code related to the visual asset' AFTER related_image_id;

CREATE INDEX idx_visual_related_image ON ana_visual_asset (related_image_id);

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

