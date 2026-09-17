# AI inference

Existing deployable service in `backend-python`. It consumes `analysis-requested.v1`, retrieves protected image objects, runs quality/inference/RAG stages, and sends `analysis-result-callback.v1` to analysis orchestration.

It owns AI runtime/audit data and model assets. It does not update clinical, reporting, or follow-up tables directly.
