# CariesGuard Backend

## Overview

`backend-java` is the backend monorepo for the CariesGuard platform. It is currently implemented as a modular Spring Boot 3 monolith on JDK 17, with MySQL + Flyway for schema management and JWT for stateless authentication.

The repository already contains:

- a working Maven multi-module project
- a Spring Boot bootstrap module
- common response / exception / trace infrastructure
- Spring Security + JWT baseline
- a minimal `system` module with login and current-user APIs
- Flyway migration scripts covering system, patient, case, image, analysis, report, and follow-up domains

The repository does not yet contain a completed business implementation for all modeled domains. Most non-system modules are still placeholders at the Java code level.

## Modules

- `caries-common`: common response models, error codes, exceptions, trace utilities
- `caries-framework`: security, JWT, OpenAPI, web infrastructure
- `caries-system`: current business implementation for auth and basic system APIs
- `caries-patient`: reserved patient domain module
- `caries-image`: reserved image domain module
- `caries-analysis`: reserved analysis domain module
- `caries-report`: reserved report domain module
- `caries-followup`: reserved follow-up domain module
- `caries-dashboard`: reserved dashboard domain module
- `caries-integration`: reserved external integration module
- `caries-boot`: application bootstrap module

## Stack

- Java 17
- Spring Boot 3.2.12
- Spring Web / Validation / Security / Actuator
- MyBatis-Plus 3.5.7
- Flyway
- MySQL
- Redis
- RabbitMQ
- springdoc-openapi
- JJWT

## Current Runtime Baseline

Application config lives in:

- `caries-boot/src/main/resources/application.yml`
- `caries-boot/src/main/resources/application-local.yml`

Default local behavior:

- app name: `caries-guard-backend`
- active profile: `local`
- server port: `8080`
- OpenAPI docs: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`
- Flyway enabled

Environment variables currently supported:

- `CARIES_DB_URL`
- `CARIES_DB_USERNAME`
- `CARIES_DB_PASSWORD`
- `CARIES_REDIS_HOST`
- `CARIES_REDIS_PORT`
- `CARIES_RABBIT_HOST`
- `CARIES_RABBIT_PORT`
- `CARIES_RABBIT_USERNAME`
- `CARIES_RABBIT_PASSWORD`
- `CARIES_JWT_SECRET`
- `CARIES_JWT_EXPIRE_SECONDS`

## Current APIs

Implemented endpoints:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/system/ping`

Current auth model:

- username/password login
- JWT access token
- stateless authentication

## Database

Flyway migrations are under `caries-boot/src/main/resources/db/migration`.

Current migrations:

- `V001__01_sys_management_schema.sql`
- `V002__02_init_sys_dict.sql`
- `V003__03_medical_business_schema.sql`
- `V004__04_image_report_followup.sql`
- `V005__05_init_dev_admin.sql`

These scripts already model a broad domain scope:

- system and RBAC
- patient profile and guardian
- visit and case
- case diagnosis and tooth records
- attachments and medical images
- AI task / summary / correction
- report records
- follow-up plans
- notification records

## Build

Run from repo root:

```bash
mvn test
```

This has been verified successfully on the current codebase. At the moment, test coverage is minimal and mostly validates baseline project compilation.

## Development Status

Implemented:

- engineering skeleton
- boot integration
- JWT auth baseline
- unified API response
- global exception handling
- trace ID propagation
- OpenAPI baseline
- Flyway bootstrap

Not yet fully implemented:

- RBAC management APIs
- patient lifecycle APIs
- case lifecycle and state machine
- image upload and quality check flow
- MQ-driven AI integration
- report generation
- follow-up workflow
- dashboard aggregation
- test coverage and deployment packaging

## Documentation

Review documents for the next development phase:

- `docs/architecture.md`
- `docs/development-plan.md`
- `docs/schema-gap-analysis.md`

## Recommended Next Step

Proceed in this order:

1. baseline alignment
2. `system` module completion
3. `patient` + `case` core workflow
4. `image` workflow
5. `analysis`
6. `report`
7. `followup`
8. `dashboard`
