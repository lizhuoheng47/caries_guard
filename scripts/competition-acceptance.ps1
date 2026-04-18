$ErrorActionPreference = "Continue"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

Write-Host "### Running Competition Environment Acceptance Tests ###" -ForegroundColor Cyan
$Failures = 0

function Check-Step($Result, $Name) {
    if ($Result) {
        Write-Host "[PASS] $Name" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $Name" -ForegroundColor Red
        $script:Failures++
    }
}

Write-Host "1. Checking Java backend health..."
$javaHealth = $false
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -ErrorAction Stop
    $javaHealth = ($res.status -eq "UP")
} catch {}
Check-Step $javaHealth "Java Health"

Write-Host "2. Checking Python backend health..."
$pythonHealth = $false
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8001/ai/v1/health" -Method Get -ErrorAction Stop
    $pythonHealth = ($res.code -eq 200)
} catch {}
Check-Step $pythonHealth "Python Health"

Write-Host "3. Checking if competition mode is active..."
$competitionMode = $false
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8001/ai/v1/health" -Method Get -ErrorAction Stop
    $json = $res | ConvertTo-Json -Depth 10
    $competitionMode = ($json -match "MOCK")
} catch {}
Check-Step $competitionMode "Competition Mode Active"

Write-Host "4. Checking if demo case exists (TODO)..."
# TODO: Call patient/case list API to ensure demo case 'demo-001' or similar exists.
Write-Host "[TODO] Demo case check is pending implementation." -ForegroundColor Yellow

Write-Host "5. Checking knowledge version exists (TODO)..."
# TODO: Call RAG knowledge version API to verify 'v1.0' vector index is loaded.
Write-Host "[TODO] Knowledge version check is pending implementation." -ForegroundColor Yellow

Write-Host "6. Running analysis task pipeline (TODO)..."
# TODO: Trigger a mock analysis task and wait for completion.
Write-Host "[TODO] Analysis task evaluation is pending implementation." -ForegroundColor Yellow

Write-Host ""
if ($Failures -eq 0) {
    Write-Host "==================================================" -ForegroundColor Green
    Write-Host "    Acceptance Tests Passed Successfully          " -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host "==================================================" -ForegroundColor Red
    Write-Host "    Acceptance Tests Finished with $Failures Failures " -ForegroundColor Red
    Write-Host "==================================================" -ForegroundColor Red
    exit 1
}
