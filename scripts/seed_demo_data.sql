-- ---------------------------------------------------------
-- CariesGuard Competition Mode - Master Seed Data
-- ---------------------------------------------------------
-- Description: Unconditionally resets and provisions the 
-- fixed demo entities: 1 Org, 2 Doctors, 2 Patients, 
-- 2 Cases (1 Low Uncertainty, 1 High Uncertainty), 
-- plus simulated imagery & AI results.
-- Idempotency: Executes top-down deletions to clear targets, 
-- then re-inserts. Safe to run multiple times.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. [TEARDOWN] FK-Safe Delete Order
DELETE FROM `med_risk_assessment_record` WHERE `case_id` IN (3001, 3002);
DELETE FROM `ana_visual_asset` WHERE `case_id` IN (3001, 3002);
DELETE FROM `ana_result_summary` WHERE `task_id` IN (4001, 4002);
DELETE FROM `ana_task_record` WHERE `id` IN (4001, 4002);
DELETE FROM `med_case_tooth_record` WHERE `case_id` IN (3001, 3002);
DELETE FROM `med_image_file` WHERE `attachment_id` IN (5001, 5002, 5003, 5004, 5005);
DELETE FROM `med_attachment` WHERE `id` IN (5001, 5002, 5003, 5004, 5005);
DELETE FROM `med_case_status_log` WHERE `case_id` IN (3001, 3002);
DELETE FROM `med_case` WHERE `id` IN (3001, 3002);
DELETE FROM `med_visit` WHERE `id` IN (3001, 3002);
DELETE FROM `pat_patient` WHERE `id` IN (2001, 2002);
DELETE FROM `sys_user` WHERE `id` IN (1001, 1002);
DELETE FROM `sys_dept` WHERE `id` = 100001;

-- 2. [PROVISION] Organization
INSERT INTO `sys_dept` (
  `id`, `parent_id`, `dept_code`, `dept_name`, `dept_category_code`, `org_id`, `status`, `deleted_flag`
) VALUES 
(100001, 0, 'ORG-001', 'Demo Clinic', 'ORG', 100001, 'ACTIVE', 0);

-- 3. [PROVISION] System Users (Doctors)
-- Note: 'password_hash' relies on Bcrypt representation of '123456'.
-- 'real_name_enc' uses a placeholder. The system will either gracefully fall back or you must update this string.
INSERT INTO `sys_user` (
  `id`, `user_no`, `username`, `password_hash`, 
  `real_name_enc`, `real_name_hash`, `real_name_masked`,
  `nick_name`, `user_type_code`, `org_id`, `status`, `deleted_flag`
) VALUES 
(1001, 'DOC-001', 'demo_doctor_01', '$2a$10$yS.8v.20r1tGz2z4uJ84M.LOKL2c3a3T/4Gv9xS7a8L.D8bF8LwU.',
 'U2FsdGVkX19DEMO_CIPHER_A', 'HASH_A', 'Dr. Smith (Demo)', 
 'Dr. Smith (Demo)', 'DOCTOR', 100001, 'ACTIVE', 0),
(1002, 'DOC-002', 'demo_doctor_02', '$2a$10$yS.8v.20r1tGz2z4uJ84M.LOKL2c3a3T/4Gv9xS7a8L.D8bF8LwU.',
 'U2FsdGVkX19DEMO_CIPHER_B', 'HASH_B', 'Dr. Lee (Demo Control)', 
 'Dr. Lee (Demo Control)', 'DOCTOR', 100001, 'ACTIVE', 0);

-- 4. [PROVISION] Patients
INSERT INTO `pat_patient` (
  `id`, `patient_no`, `patient_name_enc`, `patient_name_hash`, `patient_name_masked`,
  `gender_code`, `age`, `source_code`, `privacy_level_code`, `org_id`, `status`, `deleted_flag`
) VALUES
(2001, 'P-2026-001', 'enc_A', 'hash_A', 'John D.', 'MALE', 35, 'MANUAL', 'L4', 100001, 'ACTIVE', 0),
(2002, 'P-2026-002', 'enc_B', 'hash_B', 'Jane R.', 'FEMALE', 28, 'MANUAL', 'L4', 100001, 'ACTIVE', 0);

-- 5. [PROVISION] Visits
INSERT INTO `med_visit` (
  `id`, `visit_no`, `patient_id`, `doctor_user_id`, `visit_type_code`, `visit_date`, 
  `triage_level_code`, `source_channel_code`, `org_id`, `status`, `deleted_flag`
) VALUES
(3001, 'V-2026-001', 2001, 1001, 'OUTPATIENT', NOW(), 'NORMAL', 'MANUAL', 100001, 'ACTIVE', 0),
(3002, 'V-2026-002', 2002, 1002, 'OUTPATIENT', NOW(), 'NORMAL', 'MANUAL', 100001, 'ACTIVE', 0);

-- 6. [PROVISION] Cases (One Normal closed-loop, One High-Risk Review)
INSERT INTO `med_case` (
  `id`, `case_no`, `visit_id`, `patient_id`, `case_title`, `case_type_code`, 
  `case_status_code`, `priority_code`, `report_ready_flag`, `followup_required_flag`, 
  `org_id`, `version_no`, `status`, `deleted_flag`
) VALUES
(3001, 'CA-2026-LOW', 3001, 2001, 'Demo Low Risk', 'CARIES_SCREENING', 'REPORT_READY', 'NORMAL', '1', '0', 100001, 1, 'ACTIVE', 0),
(3002, 'CA-2026-HIGH', 3002, 2002, 'Demo High Uncertainty', 'CARIES_SCREENING', 'REVIEW_PENDING', 'URGENT', '0', '1', 100001, 1, 'ACTIVE', 0);

INSERT INTO `med_case_status_log` (
  `id`, `case_id`, `from_status_code`, `to_status_code`, `changed_by`, `change_reason_code`, `change_reason`, `changed_at`, `org_id`
) VALUES 
(3001, 3001, 'ANALYZING', 'REPORT_READY', 1001, 'AI_AUTO', 'Low uncertainty AI closed loop', NOW(), 100001),
(3002, 3002, 'ANALYZING', 'REVIEW_PENDING', 1001, 'AI_ALERT', 'High uncertainty threshold exceeded', NOW(), 100001);

-- 7. [PROVISION] AI Tasks and AI Results
INSERT INTO `ana_task_record` (
  `id`, `task_no`, `case_id`, `patient_id`, `request_batch_no`, `model_version`, 
  `task_type_code`, `task_status_code`, `started_at`, `completed_at`, `org_id`, `status`, `deleted_flag`
) VALUES
(4001, 'T-LOW-2026', 3001, 2001, 'DEMO1', 'caries-v1', 'INFERENCE', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), 100001, 'ACTIVE', 0),
(4002, 'T-HIGH-2026', 3002, 2002, 'DEMO2', 'caries-v1', 'INFERENCE', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), 100001, 'ACTIVE', 0);

INSERT INTO `ana_result_summary` (
  `id`, `task_id`, `case_id`, `raw_result_json`, `overall_highest_severity`, `uncertainty_score`, `review_suggested_flag`, `lesion_count`, `abnormal_tooth_count`, `summary_version_no`, `org_id`, `status`, `deleted_flag`
) VALUES
(4001, 4001, 3001, '{"overallHighestSeverity": "C1", "uncertaintyScore": 0.15, "reviewSuggestedFlag": "0", "reviewReason": null, "evidenceRefs": ["DOC-002"]}', 'C1', 0.1500, '0', 1, 1, 1, 100001, 'ACTIVE', 0),
(4002, 4002, 3002, '{"overallHighestSeverity": "C3", "uncertaintyScore": 0.85, "reviewSuggestedFlag": "1", "reviewReason": "HIGH_UNCERTAINTY", "evidenceRefs": ["DOC-001", "DOC-005"]}', 'C3', 0.8500, '1', 2, 2, 1, 100001, 'ACTIVE', 0);

INSERT INTO `med_risk_assessment_record` (
  `id`, `case_id`, `patient_id`, `task_id`, `overall_risk_level_code`, `risk_score`, `assessment_report_json`, `recommended_cycle_days`, `version_no`, `assessed_at`, `org_id`, `status`, `deleted_flag`
) VALUES
(4001, 3001, 2001, 4001, 'LOW', 0.20, '{"riskFactors": ["Routine"]}', 180, 1, NOW(), 100001, 'ACTIVE', 0),
(4002, 3002, 2002, 4002, 'HIGH', 0.90, '{"riskFactors": ["High Sugar Diet", "Low Fluoride"]}', 15, 1, NOW(), 100001, 'ACTIVE', 0);

-- 8. [PROVISION] File Attachments (5 dummy assets) & Images
INSERT INTO `med_attachment` (
  `id`, `biz_module_code`, `biz_id`, `file_category_code`, `asset_type_code`, `file_name`, `original_name`, `bucket_name`, `object_key`, `content_type`, `file_ext`, `storage_provider_code`, `visibility_code`, `org_id`, `status`, `deleted_flag`
) VALUES
(5001, 'CASE', 3001, 'RAW_IMAGE', 'PANORAMIC', 'pano_1.jpg', 'pano_1.jpg', 'caries-image', 'demo_assets/pano_CA-2026-LOW.jpg', 'image/jpeg', 'jpg', 'MINIO', 'PRIVATE', 100001, 'ACTIVE', 0),
(5002, 'CASE', 3001, 'RAW_IMAGE', 'PERIAPICAL', 'peri_1.jpg', 'peri_1.jpg', 'caries-image', 'demo_assets/peri1_CA-2026-LOW.jpg', 'image/jpeg', 'jpg', 'MINIO', 'PRIVATE', 100001, 'ACTIVE', 0),
(5003, 'CASE', 3002, 'RAW_IMAGE', 'PANORAMIC', 'pano_2.jpg', 'pano_2.jpg', 'caries-image', 'demo_assets/pano_CA-2026-HIGH.jpg', 'image/jpeg', 'jpg', 'MINIO', 'PRIVATE', 100001, 'ACTIVE', 0),
(5004, 'CASE', 3002, 'RAW_IMAGE', 'PERIAPICAL', 'peri_2.jpg', 'peri_2.jpg', 'caries-image', 'demo_assets/peri1_CA-2026-HIGH.jpg', 'image/jpeg', 'jpg', 'MINIO', 'PRIVATE', 100001, 'ACTIVE', 0),
(5005, 'CASE', 3002, 'RAW_IMAGE', 'PERIAPICAL', 'peri_3.jpg', 'peri_3.jpg', 'caries-image', 'demo_assets/peri2_CA-2026-HIGH.jpg', 'image/jpeg', 'jpg', 'MINIO', 'PRIVATE', 100001, 'ACTIVE', 0);

INSERT INTO `med_image_file` (
  `id`, `case_id`, `visit_id`, `patient_id`, `attachment_id`, `image_type_code`, `image_source_code`, `quality_status_code`, `is_primary`, `org_id`, `status`, `deleted_flag`
) VALUES
(5001, 3001, 3001, 2001, 5001, 'PANORAMIC', 'UPLOAD', 'PASS', '1', 100001, 'ACTIVE', 0),
(5002, 3001, 3001, 2001, 5002, 'PERIAPICAL', 'UPLOAD', 'PASS', '0', 100001, 'ACTIVE', 0),
(5003, 3002, 3002, 2002, 5003, 'PANORAMIC', 'UPLOAD', 'PASS', '1', 100001, 'ACTIVE', 0),
(5004, 3002, 3002, 2002, 5004, 'PERIAPICAL', 'UPLOAD', 'PASS', '0', 100001, 'ACTIVE', 0),
(5005, 3002, 3002, 2002, 5005, 'PERIAPICAL', 'UPLOAD', 'PASS', '0', 100001, 'ACTIVE', 0);

INSERT INTO `med_case_tooth_record` (
  `id`, `case_id`, `source_image_id`, `tooth_code`, `tooth_surface_code`, `issue_type_code`, `severity_code`, `finding_desc`, `suggestion`, `reviewed_by`, `org_id`, `status`, `deleted_flag`
) VALUES
(6001, 3001, 5001, '16', 'OCCLUSAL', 'CARIES', 'C1', 'Slight demineralization.', 'Routine care', 1001, 100001, 'ACTIVE', 0),
(6002, 3002, 5003, '46', 'PROXIMAL', 'CARIES', 'C3', 'Deep lesion reaching dentin. High uncertainty due to artifact.', 'Requires review and potential restorative treatment.', NULL, 100001, 'ACTIVE', 0);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Competition Mode Seed Data successfully (re)inserted!' as Result;
