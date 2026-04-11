# Schema Gap Analysis

## Purpose

This document records the current gaps between:

- the existing Java codebase
- the Flyway migration scripts in this repository
- the broader business/table specification you provided

It is intended to freeze what is already true, what is missing, and what must be corrected before or during implementation.

## Summary

Current state is:

- schema modeling is ahead of Java business implementation
- the repository already includes AI collaboration tables in Flyway
- some business requirements described outside the repository are not yet fully reflected in migration scripts
- some constraints are expressed in text requirements but not yet enforced structurally

## Confirmed Present in Current Flyway

The current migrations already include:

- `sys_role`
- `sys_dept`
- `sys_user`
- `sys_post`
- `sys_menu`
- `sys_user_role`
- `sys_user_post`
- `sys_role_menu`
- `sys_config`
- `sys_data_permission_rule`
- `sys_dict_type`
- `sys_dict_item`
- `pat_patient`
- `pat_guardian`
- `pat_profile`
- `med_visit`
- `med_case`
- `med_case_status_log`
- `med_case_diagnosis`
- `med_case_tooth_record`
- `med_attachment`
- `med_image_file`
- `ana_task_record`
- `ana_result_summary`
- `ana_correction_feedback`
- `ana_visual_asset`
- `med_risk_assessment_record`
- `rpt_record`
- `fup_plan`
- `msg_notify_record`

## Gap A: Code Behind Schema

Status:

- only `system` has meaningful Java business code
- `patient`, `image`, `analysis`, `report`, `followup`, `dashboard`, and `integration` are placeholders

Impact:

- schema cannot yet be exercised through stable APIs
- early schema errors are harder to detect
- business invariants are not yet enforced at service layer

Required action:

- implement modules in the planned order
- keep service-layer rules explicit rather than relying on table comments

## Gap B: AI Collaboration Tables Need Formal V1 Freeze

Observed fact:

- `ana_task_record`
- `ana_result_summary`
- `ana_correction_feedback`

are already present in `V004__04_image_report_followup.sql`.

Issue:

- if external documentation still treats them as provisional, docs and codebase will diverge

Decision recommended:

- formally include these three tables in V1 scope

Reason:

- they already exist in migration baseline
- they are necessary for the `image -> analysis -> review` chain
- postponing the formal freeze only creates future migration churn

## Gap C: Image Quality Check Table Not Yet Seen in Current Flyway

Requirement from your specification:

- `med_image_quality_check` should exist
- one image can have multiple quality-check records
- latest effective record should be query baseline
- scoring fields should follow a 0-100 rule

Current repository status:

- this table was not found in existing Flyway scripts reviewed so far

Impact:

- image module cannot fully support manual/rule-engine quality workflows
- image quality status may end up being overloaded into `med_image_file`

Required action:

- add `med_image_quality_check` by migration during image module implementation
- define latest-record query strategy explicitly

## Gap D: Primary Image Rule Is Not Yet Strongly Enforced in Schema

Requirement from your specification:

- for the same case and same image type, only one record should have `is_primary = 1`

Current repository status:

- `med_image_file` contains `is_primary`
- current Flyway does not show a direct structural constraint that enforces uniqueness for this rule

Impact:

- concurrent writes can produce multiple primary images of the same type
- service-only enforcement is fragile

Required action:

- define an enforceable uniqueness strategy
- if MySQL expression/index strategy is not adopted, implement transactional guard plus compensating validation

## Gap E: Some Suggested Indexes Are Missing or Need Recheck

Your specification explicitly emphasizes indexes such as:

- `IDX(case_id, changed_at)` for case status log
- `IDX(org_id, changed_at)` for case status log
- `IDX(case_id, image_type_code)` for image
- `IDX(patient_id, shooting_time)` for image
- `IDX(org_id, quality_status_code)` for image
- `IDX(attachment_id)` for image

Current repository status:

- some are present in existing scripts
- some need recheck against the final frozen dictionary
- `patient_id + shooting_time` and `org_id + quality_status_code` are not clearly present in the reviewed `med_image_file` DDL

Required action:

- perform a strict field-by-field diff before next migration revision
- keep the final dictionary as the single approval source

## Gap F: SQL File Comment Encoding

Observed fact:

- Chinese comments in several migration files display as garbled text

Impact:

- poor maintainability
- increased risk when reviewing DDL later
- unnecessary confusion during onboarding

Required action:

- normalize migration source encoding
- ensure editor and repository encoding are fixed to UTF-8
- avoid editing historical migrations unless absolutely necessary; if required, document the reason carefully

## Gap G: Audit Rules Are Modeled but Not Yet Fully Enforced

The specification emphasizes:

- every case status change must be persisted
- reason should be standardized plus optional remark
- status machine must be auditable

Current repository status:

- `med_case_status_log` exists in schema
- no Java implementation currently writes or validates these transitions

Required action:

- define allowed transition matrix in code
- require reason code on state change API
- persist operator, reason, and timestamp on every transition

## Gap H: Sensitive Data Strategy Is Prepared but Not Implemented

Observed fact:

- multiple tables reserve `*_enc`, `*_hash`, and `*_masked` fields

Current repository status:

- repository has not yet implemented encryption/hash/masking services

Impact:

- sensitive fields can easily be mishandled in early business code
- developers may accidentally bypass the intended privacy model

Required action:

- freeze whether V1 uses real encryption or placeholder strategy
- if real encryption is required, implement it before patient-domain CRUD expands

## Gap I: Data Permission Is Designed but Not Operational

Observed fact:

- `sys_data_permission_rule` exists
- `org_id` exists on major business tables

Current repository status:

- login is implemented
- data-scope filtering is not yet operational in repositories/services

Required action:

- complete `system` module first
- define repository-level and service-level org/data-scope enforcement pattern

## Recommended Baseline Decisions

The following should be approved before coding continues:

1. `ana_task_record`, `ana_result_summary`, and `ana_correction_feedback` are V1 formal business tables.
2. `med_image_quality_check` is a required follow-up migration item for the image module.
3. primary-image uniqueness rule must be enforced, not left as a comment-only requirement.
4. the external field dictionary is the frozen approval source, but Flyway in repo is the actual current baseline.
5. future development should use additive migrations; do not silently rewrite historical intent.

## Implementation Priority Triggered by This Analysis

Based on current gaps, the next engineering order should be:

1. baseline alignment
2. system completion
3. patient and case workflow
4. image and quality-check workflow
5. analysis workflow
6. report and follow-up
