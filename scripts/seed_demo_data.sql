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
DELETE FROM `ana_result_summary` WHERE `task_id` IN (4001, 4002);
DELETE FROM `ana_task_record` WHERE `id` IN (4001, 4002);
DELETE FROM `med_image_file` WHERE `attachment_id` IN (5001, 5002, 5003, 5004, 5005);
DELETE FROM `med_attachment` WHERE `id` IN (5001, 5002, 5003, 5004, 5005);
DELETE FROM `med_case_status_log` WHERE `case_id` IN (3001, 3002);
DELETE FROM `med_case_diagnosis` WHERE `case_id` IN (3001, 3002);
DELETE FROM `med_case_tooth_record` WHERE `case_id` IN (3001, 3002);
DELETE FROM `med_case` WHERE `id` IN (3001, 3002);
DELETE FROM `med_visit` WHERE `id` IN (3001, 3002);
DELETE FROM `pat_patient` WHERE `id` IN (2001, 2002);
DELETE FROM `sys_user` WHERE `id` IN (1001, 1002);
DELETE FROM `sys_dept` WHERE `id` = 100;

-- 2. [PROVISION] Organization
INSERT INTO `sys_dept` (
  `id`, `parent_id`, `dept_code`, `dept_name`, `dept_category_code`, `org_id`, `status`
) VALUES 
(100, 0, 'ORG-001', 'Demo Clinic', 'ORG', 100, 'ACTIVE');

-- 3. [PROVISION] System Users (Doctors)
-- Note: 'password_hash' relies on Bcrypt representation of '123456'.
-- 'real_name_enc' uses a placeholder. The system will either gracefully fall back or you must update this string.
INSERT INTO `sys_user` (
  `id`, `user_no`, `username`, `password_hash`, 
  `real_name_enc`, `real_name_hash`, `real_name_masked`,
  `nick_name`, `user_type_code`, `org_id`, `status`
) VALUES 
(1001, 'DOC-001', 'demo_doctor_01', '$2a$10$yS.8v.20r1tGz2z4uJ84M.LOKL2c3a3T/4Gv9xS7a8L.D8bF8LwU.',
 'U2FsdGVkX19DEMO_CIPHER_A', 'HASH_A', 'Dr. Smith', 
 'Dr. Smith (Demo)', 'DOCTOR', 100, 'ACTIVE'),
(1002, 'DOC-002', 'demo_doctor_02', '$2a$10$yS.8v.20r1tGz2z4uJ84M.LOKL2c3a3T/4Gv9xS7a8L.D8bF8LwU.',
 'U2FsdGVkX19DEMO_CIPHER_B', 'HASH_B', 'Dr. Lee', 
 'Dr. Lee (Demo Control)', 'DOCTOR', 100, 'ACTIVE');

-- 4. [PROVISION] Patients
INSERT INTO `pat_patient` (
  `id`, `patient_no`, 
  `patient_name_enc`, `patient_name_hash`, `patient_name_masked`,
  `gender_code`, `birth_date_masked`, `org_id`, `status`
) VALUES
(2001, 'P-2026-001', 'U2FsdGVkX19DEMO_PATIENT_A', 'HASH_PA', 'John D.', 'MALE', '1990-01-**', 100, 'ACTIVE'),
(2002, 'P-2026-002', 'U2FsdGVkX19DEMO_PATIENT_B', 'HASH_PB', 'Jane R.', 'FEMALE', '1985-06-**', 100, 'ACTIVE');

-- 5. [PROVISION] Visits
INSERT INTO `med_visit` (
  `id`, `visit_no`, `patient_id`, `doctor_user_id`, `visit_date`, `org_id`, `status`
) VALUES
(3001, 'V-2026-001', 2001, 1001, NOW(), 100, 'ACTIVE'),
(3002, 'V-2026-002', 2002, 1002, NOW(), 100, 'ACTIVE');

-- 6. [PROVISION] Cases (One Normal closed-loop, One High-Risk Review)
INSERT INTO `med_case` (
  `id`, `case_no`, `visit_id`, `patient_id`, `case_type_code`, 
  `case_status_code`, `org_id`, `status`
) VALUES
(3001, 'CA-2026-LOW', 3001, 2001, 'CARIES_SCREENING', 'REPORT_READY', 100, 'ACTIVE'),
(3002, 'CA-2026-HIGH', 3002, 2002, 'CARIES_SCREENING', 'REVIEW_PENDING', 100, 'ACTIVE');

-- 7. [PROVISION] AI Tasks and AI Results
-- Case 3001 -> Low Uncertainty
INSERT INTO `ana_task_record` (
  `id`, `task_no`, `case_id`, `patient_id`, `model_version`, 
  `task_type_code`, `task_status_code`, `org_id`
) VALUES
(4001, 'T-LOW-2026', 3001, 2001, 'v1.0-demo', 'INFERENCE', 'SUCCESS', 100);

INSERT INTO `ana_result_summary` (
  `id`, `task_id`, `case_id`, `raw_result_json`, `org_id`
) VALUES
(4001, 4001, 3001, '{"overall_highest_severity": "LOW_RISK", "uncertainty_score": 0.15, "boxes": [{"x": 10, "y": 20, "w": 30, "h": 40}]}', 100);

-- Case 3002 -> High Uncertainty
INSERT INTO `ana_task_record` (
  `id`, `task_no`, `case_id`, `patient_id`, `model_version`, 
  `task_type_code`, `task_status_code`, `org_id`
) VALUES
(4002, 'T-HIGH-2026', 3002, 2002, 'v1.0-demo', 'INFERENCE', 'SUCCESS', 100);

INSERT INTO `ana_result_summary` (
  `id`, `task_id`, `case_id`, `raw_result_json`, `org_id`
) VALUES
(4002, 4002, 3002, '{"overall_highest_severity": "HIGH_RISK", "uncertainty_score": 0.85, "boxes": [{"x": 100, "y": 200, "w": 50, "h": 60, "trigger_alarm": true}]}', 100);

-- 8. [PROVISION] File Attachments (5 dummy assets) & Images
INSERT INTO `med_attachment` (
  `id`, `biz_module_code`, `biz_id`, `file_name`, `bucket_name`, `object_key`, `org_id`
) VALUES
(5001, 'CASE', 3001, 'pano_1.jpg', 'caries-demo', 'demo_assets/pano_CA-2026-LOW.jpg', 100),
(5002, 'CASE', 3001, 'peri_1.jpg', 'caries-demo', 'demo_assets/peri1_CA-2026-LOW.jpg', 100),
(5003, 'CASE', 3002, 'pano_2.jpg', 'caries-demo', 'demo_assets/pano_CA-2026-HIGH.jpg', 100),
(5004, 'CASE', 3002, 'peri_2.jpg', 'caries-demo', 'demo_assets/peri1_CA-2026-HIGH.jpg', 100),
(5005, 'CASE', 3002, 'peri_3.jpg', 'caries-demo', 'demo_assets/peri2_CA-2026-HIGH.jpg', 100);

INSERT INTO `med_image_file` (
  `id`, `case_id`, `visit_id`, `patient_id`, `attachment_id`, `image_type_code`, `org_id`
) VALUES
(5001, 3001, 3001, 2001, 5001, 'PANORAMIC', 100),
(5002, 3001, 3001, 2001, 5002, 'PERIAPICAL', 100),
(5003, 3002, 3002, 2002, 5003, 'PANORAMIC', 100),
(5004, 3002, 3002, 2002, 5004, 'PERIAPICAL', 100),
(5005, 3002, 3002, 2002, 5005, 'PERIAPICAL', 100);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Competition Mode Seed Data successfully (re)inserted!' as Result;
