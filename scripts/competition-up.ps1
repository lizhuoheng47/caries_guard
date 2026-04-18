$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

Write-Host "### Starting CariesGuard Competition/Demo Environment ###" -ForegroundColor Cyan
docker compose --env-file env/competition.env up -d --build

Write-Host "Waiting for services to start up..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host "### Waiting for Java Backend ###" -ForegroundColor Cyan
while ($true) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -ErrorAction Stop
        if ($response.status -eq "UP") {
            Write-Host " Java Backend is UP!" -ForegroundColor Green
            break
        }
    } catch {
        Write-Host -NoNewline "."
        Start-Sleep -Seconds 2
    }
}

Write-Host "### Waiting for Python Backend ###" -ForegroundColor Cyan
while ($true) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8001/ai/v1/health" -Method Get -ErrorAction Stop
        if ($response.code -eq 200) {
            Write-Host " Python Backend is UP!" -ForegroundColor Green
            break
        }
    } catch {
        Write-Host -NoNewline "."
        Start-Sleep -Seconds 2
    }
}

Write-Host "### Setting up Demo Data (SQL) ###" -ForegroundColor Cyan
$ContainerName = docker compose ps -q mysql
if ($ContainerName) {
    Write-Host "Injecting demo seed data into MySQL..."
    docker exec -i $ContainerName mysql -u root -p123456 smbms < scripts/seed_demo_data.sql
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Demo data injected successfully." -ForegroundColor Green
    } else {
        Write-Host "Failed to inject demo data via mysql CLI." -ForegroundColor Red
    }
} else {
    Write-Host "Could not locate running MySQL container." -ForegroundColor Yellow
}

Write-Host "### Seeding Demo Assets ###" -ForegroundColor Cyan
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts/seed_demo_assets.ps1"

Write-Host "### Importing Knowledge Base ###" -ForegroundColor Cyan
python scripts/seed_demo_knowledge.py

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "    Competition Environment Successfully Started  " -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host "Access URL          : http://localhost:8080"
Write-Host "API Docs (Java)     : http://localhost:8080/swagger-ui.html"
Write-Host "API Docs (Python)   : http://localhost:8001/docs"
Write-Host "RabbitMQ Management : http://localhost:15672 (guest/guest)"
Write-Host "MinIO Console       : http://localhost:9001 (minioadmin/minioadmin)"
Write-Host ""
Write-Host "Demo Summary:"
Write-Host " - Competition Mode: ENABLED (skips complex real-world checks)"
Write-Host " - AI Runtime Mode: MOCK (fast, offline-friendly predictable outputs)"
Write-Host " - Knowledge Base: v1.0 (local loaded)"
Write-Host "==================================================" -ForegroundColor Green
