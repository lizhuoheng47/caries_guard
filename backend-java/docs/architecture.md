# Architecture

## Positioning

The current backend should be treated as a modular monolith, not a microservice system.

This is the correct choice for the current phase because:

- domain boundaries are already visible in module structure
- core business workflow is not yet fully implemented
- patient, case, image, analysis, report, and follow-up flows are strongly coupled
- a single deployable unit keeps delivery risk lower while the model is still stabilizing

Future service extraction can be evaluated after the main workflow is stable and integration pressure becomes real.

## Architectural Goals

- keep domain boundaries explicit
- keep deployment simple
- align code, schema, and API contracts
- preserve traceability and auditability
- support later AI workflow integration without rewriting the base system

## Module Layout

### Foundation modules

- `caries-common`
- `caries-framework`
- `caries-boot`

### Business modules

- `caries-system`
- `caries-patient`
- `caries-image`
- `caries-analysis`
- `caries-report`
- `caries-followup`
- `caries-dashboard`
- `caries-integration`

## Layering Rule

Business modules should follow a consistent internal structure:

- `controller`: REST API layer
- `app`: application services, orchestration, transaction boundary
- `domain`: domain models, domain services, repository interfaces
- `infrastructure`: DOs, mappers, repository implementations, external gateways
- `interfaces`: commands, queries, VOs, assemblers

`caries-system` already partially follows this direction and should be used as the normalization baseline.

## Current Foundation Capabilities

Already implemented in code:

- unified response envelope
- business exception abstraction
- common error codes
- request trace ID propagation
- global exception handling
- Spring Security filter chain
- JWT generation and parsing
- OpenAPI base configuration

## Current Security Model

Current security chain:

1. request enters `TraceIdFilter`
2. JWT token is resolved by `JwtAuthenticationFilter`
3. `SystemUserDetailsService` loads user details
4. security context is populated
5. controller accesses current user via `SecurityContextUtils`

Current public endpoints:

- auth login
- system ping
- actuator health
- OpenAPI / Swagger

Current limitation:

- the repository contains login authentication, but not a complete RBAC management implementation
- menu permission and data-scope enforcement are modeled in schema, not completed in Java code

## Data Architecture

Current schema direction is domain-complete but code-incomplete.

Main data domains already modeled in Flyway:

- system and organization
- patient and guardian
- patient profile
- visit
- case and case status log
- diagnosis and tooth findings
- attachment and image
- AI task / result / correction
- risk assessment
- report
- follow-up plan
- notification

Design characteristics already visible in schema:

- `org_id` is the row-level isolation anchor
- `deleted_flag` is the logical-delete strategy
- many sensitive fields are prepared for encrypted/hash/masked storage
- status flow is intended to be auditable

## Integration Architecture

Planned integration direction inferred from stack and schema:

- MySQL for transactional persistence
- Redis for cache / token / hot data scenarios
- RabbitMQ for async AI workflow decoupling
- object storage for image and report payloads
- external AI service as an independent service boundary

Current reality:

- MySQL + Flyway integration is present in config
- Redis and RabbitMQ are configured but not yet functionally integrated in domain code
- object storage integration is not yet implemented

## Target Core Workflow

The intended MVP business chain should be:

1. user logs in
2. patient is registered or selected
3. visit is created
4. case is created
5. images are uploaded and linked
6. AI analysis task is submitted
7. AI result is written back
8. doctor reviews and corrects result
9. report is generated
10. follow-up is created when required

Current code only covers step 1 and a minimal system health check.

## Engineering Constraints

The following constraints should be kept during implementation:

- schema changes must be managed by Flyway
- module boundaries should be respected
- do not mix future design assumptions into current-state documentation
- expose APIs only after DTO/VO contract is stable
- every status transition must be auditable
- business data isolation must be org-aware
- sensitive data handling must not be bypassed by convenience shortcuts

## Immediate Architectural Priorities

1. align schema, documentation, and code baseline
2. finish `system` as the permission and organization foundation
3. implement patient/case/image chain before dashboard and operations features
4. formalize AI collaboration tables into V1 scope to avoid repeated rework
