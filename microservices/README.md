# CariesGuard microservices evolution

This directory is the migration boundary for the future `caries-guard-microservices` repository. The current application remains runnable while capabilities are extracted with the strangler pattern.

## Current deployment units

| Unit | Current source | State | Next extraction step |
| --- | --- | --- | --- |
| Clinical core | `backend-java` | Modular monolith | Introduce service APIs around patient, case, and visit ownership |
| Analysis orchestrator | `backend-java/caries-analysis` | In-process module | Remove direct access to clinical tables and give it an owned schema |
| AI inference | `backend-python` | Independently deployable | Treat the versioned request/callback contracts as its public boundary |
| Web application | `frontend` | Independently deployable | Route through a gateway instead of directly targeting one backend |

The service catalog is machine-readable in `service-catalog.json`. Contracts under `contracts/` are additive and versioned; CI rejects malformed or duplicate contract identifiers.

## Migration sequence

1. Freeze the current modular-monolith behavior with tests and versioned contracts.
2. Extract analysis orchestration behind `/api/v1/analysis/**` while keeping the existing AI worker.
3. Give analysis orchestration its own database/schema and use clinical APIs or events instead of cross-module table access.
4. Extract image metadata/access, then reports, then follow-up. Identity and dashboards remain shared until independent scaling or ownership justifies extraction.
5. Move this directory and selected service sources to the private `caries-guard-microservices` repository once the first extracted service passes compatibility tests.

## Rules

- A service owns its data and does not query another service's tables.
- HTTP and event contracts are versioned; compatible changes are additive.
- Consumers are idempotent and messages carry stable task identifiers and trace context.
- Database writes and published events use an outbox/inbox pattern before dual-write traffic is enabled.
- No distributed transaction spans service boundaries.
- Every service exposes health, metrics, structured logs, and graceful shutdown behavior.

Validate the bootstrap metadata and contracts with:

```powershell
python .\scripts\validate-service-contracts.py
python .\scripts\check-service-boundaries.py
```
