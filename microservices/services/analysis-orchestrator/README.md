# Analysis orchestrator

Extraction candidate sourced from `backend-java/caries-analysis`. It owns analysis task lifecycle, callback verification, idempotency, review state, model governance, and the `ana_*` tables. It does not run models and must not own patient or image records.

The first extraction milestone is a standalone Spring Boot application consuming clinical/image identifiers through versioned APIs while preserving the existing `analysis.requested` contract.
