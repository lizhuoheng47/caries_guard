-- V011: Add aggregate columns to ana_result_summary for direct query consumption
-- These fields are extracted from raw_result_json during AI callback write-back
-- to support list views, reports, dashboard, and competition defense without JSON parsing.

ALTER TABLE ana_result_summary
  ADD COLUMN overall_highest_severity VARCHAR(32) DEFAULT NULL COMMENT '最高严重程度 (e.g. C1, C2, C3)',
  ADD COLUMN uncertainty_score DECIMAL(5,4) DEFAULT NULL COMMENT '不确定性评分 0~1',
  ADD COLUMN review_suggested_flag CHAR(1) DEFAULT '0' COMMENT '是否建议复核 0-否 1-是';
