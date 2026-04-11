-- ----------------------------
-- 数据域：系统管理域 (字典基础数据)
-- 数据库版本建表规约脚本 V002
-- 说明：严格写入被冻结的状态流转数据，不携带任何敏感测试值。
-- ----------------------------

-- 先初始字典类型 sys_dict_type
INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `system_flag`, `sort_order`, `org_id`, `status`, `deleted_flag`, `created_by`, `created_at`) VALUES 
(1001, 'sys_gender', '性别', '1', 1, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
(1002, 'sys_yes_no', '是/否', '1', 2, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
(1003, 'med_case_status', '病例状态', '1', 11, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
(1004, 'sys_user_type', '用户类型', '1', 6, 0, 'ACTIVE', 0, 0, CURRENT_TIMESTAMP);

-- 插入字典具体项 sys_dict_item
-- Gender
INSERT INTO `sys_dict_item` (`id`, `dict_type_id`, `item_label`, `item_value`, `item_sort`, `is_default`, `org_id`, `status`, `deleted_flag`) VALUES 
(2001, 1001, '男', 'MALE', 1, '0', 0, 'ACTIVE', 0),
(2002, 1001, '女', 'FEMALE', 2, '0', 0, 'ACTIVE', 0),
(2003, 1001, '未知', 'UNKNOWN', 3, '1', 0, 'ACTIVE', 0);

-- Yes/No
INSERT INTO `sys_dict_item` (`id`, `dict_type_id`, `item_label`, `item_value`, `item_sort`, `is_default`, `org_id`, `status`, `deleted_flag`) VALUES 
(2004, 1002, '是', '1', 1, '0', 0, 'ACTIVE', 0),
(2005, 1002, '否', '0', 2, '1', 0, 'ACTIVE', 0);

-- User Types
INSERT INTO `sys_dict_item` (`id`, `dict_type_id`, `item_label`, `item_value`, `item_sort`, `is_default`, `org_id`, `status`, `deleted_flag`) VALUES 
(2006, 1004, '系统超级防线', 'ADMIN', 1, '0', 0, 'ACTIVE', 0),
(2007, 1004, '机构管理', 'ORG_ADMIN', 2, '0', 0, 'ACTIVE', 0),
(2008, 1004, '医生', 'DOCTOR', 3, '1', 0, 'ACTIVE', 0),
(2009, 1004, '筛查员', 'SCREENER', 4, '0', 0, 'ACTIVE', 0),
(2010, 1004, '患者', 'PATIENT', 5, '0', 0, 'ACTIVE', 0);

-- **严苛锁定的八大级联病例主状态**
INSERT INTO `sys_dict_item` (`id`, `dict_type_id`, `item_label`, `item_value`, `item_sort`, `is_default`, `org_id`, `status`, `deleted_flag`) VALUES 
(3001, 1003, '已创建', 'CREATED', 1, '1', 0, 'ACTIVE', 0),
(3002, 1003, '等带影像质检', 'QC_PENDING', 2, '0', 0, 'ACTIVE', 0),
(3003, 1003, 'AI运算分析中', 'ANALYZING', 3, '0', 0, 'ACTIVE', 0),
(3004, 1003, '等待医生复核', 'REVIEW_PENDING', 4, '0', 0, 'ACTIVE', 0),
(3005, 1003, '报告已就绪', 'REPORT_READY', 5, '0', 0, 'ACTIVE', 0),
(3006, 1003, '必须启动随访', 'FOLLOWUP_REQUIRED', 6, '0', 0, 'ACTIVE', 0),
(3007, 1003, '已完全流转关闭', 'CLOSED', 7, '0', 0, 'ACTIVE', 0),
(3008, 1003, '被撤销或驳回', 'CANCELLED', 8, '0', 0, 'ACTIVE', 0);

-- 初始化超级管理员（不允许带证明明文数据）
INSERT INTO `sys_user` (
  `id`, `user_no`, `username`, `password_hash`, 
  `real_name_enc`, `real_name_hash`, `real_name_masked`, 
  `nick_name`, `user_type_code`, `org_id`, `status`, `deleted_flag`
) VALUES (
  1, 'SYS0001', 'admin', '$2a$10$D/T9z/Jm6E0iT/J2d9Z...', -- BCrypt Hash
  'PRE_ENC_EMPTY', 'PRE_HASH_EMPTY', 'Ad***',
  'System Defender', 'ADMIN', 0, 'ACTIVE', 0
);
