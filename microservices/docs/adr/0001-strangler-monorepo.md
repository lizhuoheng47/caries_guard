# ADR-0001: Evolve through a strangler monorepo

- Status: Accepted
- Date: 2026-09-17

## Context

The Java backend has useful domain modules but is deployed as one Spring Boot process and shares one business database. The Python inference worker is already separately deployable and communicates through RabbitMQ and a signed callback. A big-bang rewrite would remove the working integration and make behavioral comparison difficult.

## Decision

Use a monorepo and the strangler pattern. The modular monolith remains the reference implementation while one bounded context at a time receives an independent runtime and owned data. Analysis orchestration is first because it already has an asynchronous boundary, stable task identity, retry rules, and callback idempotency.

Kubernetes service discovery is sufficient for runtime discovery; Spring Cloud components are not introduced unless a demonstrated platform requirement appears. RabbitMQ carries domain/integration events. Synchronous calls use versioned HTTP APIs.

## Consequences

- The transition temporarily contains both legacy modules and extracted services.
- Contract compatibility and observability become release gates.
- Cross-service joins are replaced by APIs, events, or purpose-built read models.
- Database separation happens before independent production ownership is claimed.
- The future GitHub repository can retain one atomic change across service and contract updates.
