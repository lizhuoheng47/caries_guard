# CariesGuard

CariesGuard is an AI-assisted dental imaging research system. The repository now has one application runtime: the complete Docker Compose chain.

## Start

```powershell
Copy-Item .env.docker.example .env
docker compose up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait-for-health.ps1
```

Open http://127.0.0.1:5173.

The stack contains the Vue frontend, Java business service, Python inference service, MySQL, Redis, RabbitMQ, and MinIO. Competition, demo, staging-preset, and standalone segmentation startup paths have been removed.

The released DC1000 UNet TorchScript checkpoint performs lesion segmentation. Quality checking, tooth candidate localization, grading, and risk assessment are still explicitly identified rule-based stages; they are not clinically validated models.

Stop while preserving persistent data:

```powershell
docker compose down
```

AI output is for research assistance and does not replace a dentist's diagnosis.
