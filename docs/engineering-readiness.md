# Engineering readiness and verification

This document separates implemented reliability mechanisms from claims that require measurements in a real environment.

## Quality gates

Run every deterministic repository gate with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-engineering-gates.ps1
```

GitHub Actions runs the same frontend tests/build, Python tests, Java tests, and Compose contract on pushes and pull requests.

## Load SLO

The repository ships a dependency-free bounded load runner. The default gate sends 500 requests with concurrency 25 and requires error rate <= 1%, p95 <= 500 ms, and throughput >= 1 request/s:

```powershell
python .\scripts\load-test.py `
  --url http://127.0.0.1:8080/actuator/health `
  --requests 500 `
  --concurrency 25 `
  --max-error-rate 0.01 `
  --max-p95-ms 500 `
  --min-rps 20 `
  --output .\evidence\generated\health-load.json
```

For a business API, pass an `Authorization` header and choose a read-only endpoint. Never load-test a shared or production environment without approval. A result file is evidence for one environment and time; it is not a permanent high-concurrency claim.

The checked-in local baseline at `docs/evidence/health-load-local-2026-09-17.json` records 300/300 successful health requests at concurrency 30, p95 76.612 ms, and 579.493 requests/s. Redis/RabbitMQ health checks and MinIO bucket creation were disabled for that developer-machine run, so it must not be treated as business-path or production-capacity evidence.

## Failure drill

With the full Compose stack running, this guarded drill pauses the Python worker, verifies the Java service remains healthy, resumes the worker in a `finally` block, and verifies recovery:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fault-drill.ps1
```

The script requests confirmation because it deliberately pauses a service. Record queue depth, recovery time, and failed-task state before and after the drill when producing release evidence.

## Availability architecture

`deploy/k8s` provides multi-replica Deployments, readiness/liveness probes, disruption budgets, resource requests, and horizontal pod autoscaling for the stateless application tier. MySQL, Redis, RabbitMQ, MinIO, model assets, ingress TLS, backups, and monitoring must be supplied by managed or clustered platform services.

The manifests therefore demonstrate a scale-out application design, not database or storage high availability by themselves. A production acceptance record should additionally include:

- backup restoration and recovery-point/recovery-time measurements;
- RabbitMQ quorum queue or managed-broker failover evidence;
- object-storage replication and lifecycle verification;
- rolling update and node-drain results;
- load results at expected and peak traffic;
- alert delivery and incident runbook exercises.

## Initial service-level objectives

| Signal | Initial target | Verification |
| --- | ---: | --- |
| Authenticated read API availability | 99.9% monthly | External probe and server metrics |
| Read API p95 | <= 500 ms at agreed load | `scripts/load-test.py` |
| API 5xx rate | < 1% during load gate | Load result and metrics |
| AI callback eventual completion | >= 99% excluding rejected input | Callback audit table |
| Worker recovery after one pod loss | <= 120 seconds | Fault drill / Kubernetes pod deletion |

Targets must be revised after a representative dataset and traffic profile are available.
