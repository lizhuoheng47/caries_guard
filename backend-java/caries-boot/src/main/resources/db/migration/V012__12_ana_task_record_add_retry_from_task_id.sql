-- V012: Add retry_from_task_id to ana_task_record for retry audit trail
-- When a FAILED task is retried, the new task records this field pointing to the original.
-- First-time tasks have NULL. This enables retry lineage tracking for auditing and defense.

ALTER TABLE ana_task_record
  ADD COLUMN retry_from_task_id BIGINT DEFAULT NULL COMMENT '重试来源任务ID，首次任务为NULL';
