-- Drop stale virtual column `highest_severity_vc` on `ana_result_summary`.
-- The expression `raw_result_json->>'$.overall_highest_severity'` expects snake_case,
-- but the AI callback writes camelCase (`overallHighestSeverity` nested under `summary`),
-- so the virtual column is always NULL and its index never matches.
-- The real column `overall_highest_severity` is populated directly by the Java callback path
-- and is the correct read source.

DROP INDEX `idx_highest_severity_vc` ON `ana_result_summary`;
ALTER TABLE `ana_result_summary` DROP COLUMN `highest_severity_vc`;
