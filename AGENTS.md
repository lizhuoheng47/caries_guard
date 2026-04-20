# AGENTS.md

## Scope
These rules apply to the whole repository unless a deeper directory provides stricter local instructions.

## Fixed Development Rules
1. Java owns the business mainline, workflow orchestration, and status authority. Python owns AI and RAG capabilities only.
2. Frontend must call Java `\/api\/v1\/**` only. Frontend must not call Python services directly.
3. The analysis pipeline must remain `Java -> RabbitMQ -> Python`, and result delivery must remain `Python -> Java callback`.
4. New AI detail fields should be written into `rawResultJson` first. Do not casually expand top-level callback DTO fields.
5. In `real` mode, failures must fail explicitly. No silent fallback, and no mock/fake bbox or mock/fake grading output is allowed.
6. Any change touching callback contracts, `rawResultJson`, `visualAssets`, `grading` or `uncertainty`, or database migrations must run Docker E2E validation before delivery.
7. Follow the existing repository layering and code style. Do not perform broad refactors unless explicitly required.
8. Delivery output must always state: what changed, why it changed, how it was verified, and what residual risks remain.
