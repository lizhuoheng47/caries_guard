-- ----------------------------
-- 数据域：系统管理域
-- 数据库版本建表规约脚本 V001
-- 说明：含三列保护策略、复合 UK 逻辑删除策略和虚拟列。
-- ----------------------------

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

-- 3. 系统用户表 sys_user (三列保护落地)
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL COMMENT '用户主键',
  `dept_id` BIGINT NULL COMMENT '所属部门ID',
  `user_no` VARCHAR(32) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  
  -- "三列保护: 姓名"
  `real_name_enc` VARCHAR(255) NOT NULL COMMENT '真实姓名 AES 密文存储',
  `real_name_hash` VARCHAR(128) NOT NULL COMMENT '真实姓名 Hash 摘要(查询用)',
  `real_name_masked` VARCHAR(64) NOT NULL COMMENT '真实姓名 脱敏展示',
  
  `nick_name` VARCHAR(64) NULL COMMENT '昵称/展示名',
  `user_type_code` VARCHAR(32) NOT NULL DEFAULT 'DOCTOR' COMMENT '用户类型',
  `gender_code` VARCHAR(16) NULL DEFAULT 'UNKNOWN' COMMENT '性别代码',
  
  -- "三列保护: 手机号"
  `phone_enc` VARCHAR(255) NULL COMMENT '手机号 AES 密文存储',
  `phone_hash` VARCHAR(128) NULL COMMENT '手机号 Hash 摘要(唯一精准定位)',
  `phone_masked` VARCHAR(32) NULL COMMENT '手机号 前端脱敏展示',
  
  -- "三列保护: 邮箱"
  `email_enc` VARCHAR(255) NULL COMMENT '邮箱 AES 密文存储',
  `email_hash` VARCHAR(128) NULL COMMENT '邮箱 Hash 摘要',
  `email_masked` VARCHAR(128) NULL COMMENT '邮箱 前端脱敏展示',
  
  `avatar_url` VARCHAR(255) NULL COMMENT '头像地址',
  `certificate_type_code` VARCHAR(32) NULL COMMENT '证件类型',
  
  -- "三列保护: 证件号"
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
  /* 由于菜单可能是树形结构，对于同一个权限码即使不在同级也唯一 */
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
  
  -- P2 虚拟列：抽取列级限制中指定的常用掩码规则，便于被快速命中（假如经常针对掩码检索）
  -- `policy_mode_vc` VARCHAR(64) GENERATED ALWAYS AS (`column_mask_policy_json`->>'$.mode') VIRTUAL COMMENT '虚拟列：提取核心拦截模型名称',
  
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_mod_scope_del` (`role_id`, `module_code`, `scope_type_code`, `deleted_flag`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据权限规则表';

-- 11. 字典类型表 sys_dict_type
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

-- 12. 字典项表 sys_dict_item
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

