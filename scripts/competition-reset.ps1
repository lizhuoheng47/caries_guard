$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

Write-Host "### Resetting CariesGuard Competition Environment ###" -ForegroundColor Cyan
Write-Host "This will stop the containers and remove all associated data volumes." -ForegroundColor Yellow

docker compose --env-file env/competition.env down -v --remove-orphans

Write-Host "### Pruning demo state ###" -ForegroundColor Cyan
if (Test-Path "evidence/generated/demo_assets") {
    Remove-Item -Recurse -Force "evidence/generated/demo_assets"
    Write-Host "Cleared generated demo assets." -ForegroundColor Green
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "    Competition Environment Successfully Reset    " -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
