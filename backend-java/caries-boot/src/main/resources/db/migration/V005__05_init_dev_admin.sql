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
