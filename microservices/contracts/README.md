# Contract policy

Contract filenames end in `.v<major>.schema.json`. The `$id` is permanent after publication. Within one major version, producers may add optional fields but must not rename fields, narrow accepted values, or make optional fields required.

`analysis-requested.v1` describes the message body consumed by the Python AI worker. `analysis-result-callback.v1` describes the signed HTTP callback body sent back to analysis orchestration. RabbitMQ headers such as `eventType`, `taskNo`, and `traceId` are transport metadata and do not replace fields required in the body.

Before changing either producer, run the repository tests and `scripts/validate-service-contracts.py`. Consumer-driven compatibility tests will be added before analysis orchestration is deployed as a separate process.
