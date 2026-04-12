# Development Plan

## Plan Position

This plan is based on the actual current repository state.

It is not a greenfield bootstrap plan. The project already has:

- a working Java multi-module structure
- security and auth baseline
- Flyway migrations
- a partial `system` module

So the next phase is baseline alignment plus incremental implementation.

## Phase P0: Baseline Alignment

### Goal

Freeze the engineering and schema baseline before feature expansion.

### Scope

- align codebase status with written documentation
- reconcile external field dictionary with Flyway scripts
- freeze naming, layering, and module conventions
- identify missing tables, missing indexes, and missing constraints
- resolve SQL comment encoding problems where needed
- define V1 formal table scope for AI collaboration tables

### Deliverables

- README and architecture docs
- schema gap report
- confirmed V1 table list
- implementation conventions for modules

### Exit Criteria

- docs are approved
- P1 implementation target is frozen
- no unresolved baseline contradiction remains between docs and migration scripts

## Phase P1: System Module Completion

### Goal

Turn `caries-system` into the actual access-control and org-isolation foundation.

### Scope

- user management
- role management
- menu / permission management
- dictionary query
- config query
- login audit
- operation audit
- initial data-scope enforcement

### Deliverables

- complete auth flow
- RBAC query and maintenance APIs
- current-user permission set API
- org-aware access checks

### Exit Criteria

- login and permission checks are complete
- role-based permission effect is verifiable
- org isolation is testable

## Phase P2: Patient and Case Core Workflow

### Goal

Build the first medical business chain.

### Scope

- patient master data
- guardian
- patient profile
- visit registration
- case creation
- case status transition log
- diagnosis
- tooth record

### Deliverables

- patient create/update/query
- visit create/query
- case create/detail/query
- explicit case state machine enforcement
- state transition audit persistence

### Exit Criteria

- `patient -> visit -> case` is fully runnable
- invalid case status transitions are blocked
- every valid case transition is logged

## Phase P3: Image Module

### Goal

Complete image and file persistence workflow.

### Current Status

Completed. Detailed completion note is tracked in `docs/09_image_模块开发检查与完成情况.md`.

### Scope

- attachment persistence
- medical image persistence
- upload flow
- MD5 dedup strategy
- primary-image rule
- image quality check table and APIs
- object storage integration

### Deliverables

- file upload API
- image metadata API
- quality check record API
- storage integration abstraction

### Exit Criteria

- image upload succeeds end to end
- metadata and attachment records are consistent
- primary-image rule is enforced

## Phase P4: Analysis Module

### Goal

Connect medical images with AI processing workflow.

### Current Status

Core workflow implemented. Task create/query, callback idempotency, result write-back,
correction feedback, and case status synchronization are online in code.
MQ consumer-side finalization remains for later phase.
Module-positioning alignment fixes (callback validation normalization, event publish timing,
state-machine precondition tightening, summary aggregate fallback) are completed.

### Scope

- AI task creation
- MQ event publishing
- task status tracking
- result summary callback
- correction feedback entry
- case status synchronization

### Deliverables

- `ana_task_record`
- `ana_result_summary`
- `ana_correction_feedback`
- analysis event chain
- AI callback integration contract

### Exit Criteria

- `image upload -> AI request -> result writeback -> case status update` runs successfully

## Phase P5: Report Module

### Goal

Generate doctor and patient reports with version control.

### Scope

- report record management
- report generation
- PDF archive persistence
- version retention
- export audit

### Exit Criteria

- same case can produce doctor and patient reports
- previous versions remain preserved

## Phase P6: Follow-up Module

### Goal

Generate and track follow-up plans from risk or diagnosis outcome.

### Scope

- follow-up plan
- follow-up task
- follow-up record
- notification trace

### Exit Criteria

- report or review outcome can trigger follow-up plan creation

## Phase P7: Dashboard and Operations

### Goal

Expose management and demo-facing aggregation capabilities.

### Scope

- statistics aggregation
- backlog monitoring
- health and operations metrics
- model version query

### Exit Criteria

- supports management demo and operational inspection

## Phase P8: Testing and Delivery

### Goal

Close the release loop with test coverage and deployment baseline.

### Scope

- unit tests
- integration tests
- end-to-end workflow tests
- deployment scripts
- API doc cleanup

### Required Mainline Coverage

At minimum, cover:

1. login
2. patient registration
3. visit creation
4. case creation
5. image upload
6. AI analysis
7. report generation
8. correction feedback
9. follow-up creation

## Fixed Delivery Template Per Module

Each module should ship with:

- migration validation
- DO + Mapper + Repository + AppService + Controller
- Command / Query / VO
- OpenAPI annotations
- minimal unit and integration tests
- module note and integration points

## Recommended Sequence

Implementation order should remain:

1. P0 baseline alignment
2. P1 system
3. P2 patient + case
4. P3 image
5. P4 analysis
6. P5 report
7. P6 follow-up
8. P7 dashboard
9. P8 testing and delivery

Reason:

- `system` is the access and isolation foundation
- `patient + case + image + analysis + report` forms the MVP chain
- `follow-up + dashboard` is enhancement, not bootstrap
