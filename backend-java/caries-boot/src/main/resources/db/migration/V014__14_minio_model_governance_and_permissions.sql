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
