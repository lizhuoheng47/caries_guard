# Development Plan

## Plan Position

This plan reflects the actual repository state as of `2026-04-14`.

The project is no longer in bootstrap phase. Core modules have already moved into implementation and verification.

## Current Snapshot

- `P3 image` core workflow is completed.
- `P4 analysis` core workflow is completed, and local profile now uses a real RabbitMQ publisher.
- `P5 report` core workflow is completed.
- analysis -> report real integration tests are completed in `caries-boot`.
- `P6 followup` is completed for current V1 scope:
  - trigger, audit, overdue, idempotency, and cross-module E2E are all covered
  - P6 acceptance evidence has been written back to docs
- `P7 dashboard` first implementation scope is completed:
  - overview, status distribution, risk distribution, followup summary, backlog summary, model runtime, and trend endpoints are implemented
  - boot real DB integration tests are in place for all 7 endpoints

## Phase P0: Baseline Alignment

Completed.

Baseline engineering conventions, Flyway alignment strategy, and module boundaries have been frozen.

## Phase P1: System Module Completion

Core baseline is available and can support later business modules.

Current status:

- auth and org-isolation baseline are available
- remaining work belongs to management enhancement, not mainline blocker

## Phase P2: Patient and Case Core Workflow

Core workflow is in place.

Covered chain:

- patient
- visit
- case
- case status log

## Phase P3: Image Module

Completed.

Covered scope:

- attachment persistence
- image metadata persistence
- upload flow
- primary-image rule
- image quality check

## Phase P4: Analysis Module

Completed for current stage.

Covered scope:

- task creation
- callback write-back
- correction feedback
- case status synchronization
- callback idempotency

Deferred:

- Python AI consumer integration and MQ runtime governance

## Phase P5: Report Module

Completed for current V1 scope.

Covered scope:

- template management
- report generation
- version increment
- export audit
- PDF archive persistence

## Phase P6: Follow-up Module

Completed for current V1 scope.

Covered scope:

- follow-up plan
- follow-up task
- follow-up record
- notification trace
- trigger idempotency
- overdue handling
- audit evidence
- analysis -> report -> followup E2E

Acceptance evidence:

- `AnalysisReportFollowupE2ETest`
- `FollowupTriggerIdempotencyE2ETest`
- `FollowupAuditIntegrationTest`
- `FollowupOverdueIntegrationTest`

## Phase P7: Dashboard and Operations

Current implementation scope is completed.

Covered endpoints:

1. `/api/v1/dashboard/overview`
2. `/api/v1/dashboard/case-status-distribution`
3. `/api/v1/dashboard/risk-level-distribution`
4. `/api/v1/dashboard/followup-task-summary`
5. `/api/v1/dashboard/backlog-summary`
6. `/api/v1/dashboard/model-runtime`
7. `/api/v1/dashboard/trend`

Acceptance evidence:

- `DashboardOverviewIntegrationTest`
- `DashboardCaseStatusDistributionIntegrationTest`
- `DashboardRiskLevelDistributionIntegrationTest`
- `DashboardFollowupTaskSummaryIntegrationTest`
- `DashboardBacklogIntegrationTest`
- `DashboardModelRuntimeIntegrationTest`
- `DashboardTrendIntegrationTest`

Deferred:

- dashboard frontend page integration
- cache and offline aggregation optimization
- cross-org drill-down

## Phase P8: Testing and Delivery

This phase is now in progress.

### Goal

Close the release loop with:

- module acceptance evidence
- deployment baseline
- API/doc cleanup
- final demo and答辩材料收口

### Mainline Coverage

At minimum, keep the following chains verifiable:

1. login
2. patient registration
3. visit creation
4. case creation
5. image upload
6. AI analysis
7. report generation
8. correction feedback
9. follow-up creation
10. dashboard aggregation query

Current evidence:

- `MainlineWorkflowE2ETest` now covers:
  - login
  - current-user query
  - patient registration
  - visit creation
  - case creation
  - file upload
  - image creation
  - image quality check
  - AI analysis callback
  - doctor correction feedback
  - report generation
  - report export audit
  - follow-up completion
  - dashboard overview and followup aggregation

## Fixed Delivery Template Per Module

Each module should ship with:

- migration validation
- repository + app service + controller
- command / query / VO
- integration tests
- module note and acceptance evidence

## Recommended Sequence

Completed sequence:

1. P0 baseline alignment
2. P1 system baseline
3. P2 patient + case
4. P3 image
5. P4 analysis
6. P5 report
7. P6 follow-up
8. P7 dashboard

Next sequence:

9. P8 testing and delivery

## Next Implementation Step

Next development should enter `P8`.

Recommended execution order:

1. consolidate module acceptance docs and答辩证据
2. clean API/document inconsistencies
3. prepare deployment and environment documentation
4. produce final test evidence summary
5. perform frontend/dashboard joint verification if needed
