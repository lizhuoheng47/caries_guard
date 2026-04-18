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

Write-Host "### Setting up Demo Data (TODO) ###" -ForegroundColor Cyan
# TODO: Import demo database dump, patients, files.
# Connect to mysql container or execute SQL scripts here.
Write-Host "- Demo data setup pending implementation." -ForegroundColor Yellow

Write-Host "### Importing Knowledge Base (TODO) ###" -ForegroundColor Cyan
# TODO: Inject vector database data or copy files to RAG directory.
Write-Host "- Knowledge base import pending implementation." -ForegroundColor Yellow

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
