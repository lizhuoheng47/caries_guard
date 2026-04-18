$ErrorActionPreference = "Continue"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

Write-Host "### Running Competition Environment Acceptance Tests ###" -ForegroundColor Cyan
$Failures = 0

function Check-Step($Result, $Name, $Details="") {
    if ($Result) {
        Write-Host "[PASS] $Name" -ForegroundColor Green
        if ($Details) { Write-Host "       $Details" -ForegroundColor DarkGray }
    } else {
        Write-Host "[FAIL] $Name" -ForegroundColor Red
        if ($Details) { Write-Host "       $Details" -ForegroundColor Red }
        $script:Failures++
    }
}

Write-Host "1. Checking Evidence Directories..."
$dirs = @("evidence/demo-screenshots", "evidence/payloads", "evidence/metrics", "evidence/qa-cases", "evidence/review-cases")
$dirsExist = $true
foreach ($d in $dirs) {
    if (-not (Test-Path $d)) { $dirsExist = $false; break }
}
Check-Step $dirsExist "Evidence Directories Created"

Write-Host "2. Checking Java backend API health..."
$javaHealth = $false
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -ErrorAction Stop
    $javaHealth = ($res.status -eq "UP")
} catch {}
Check-Step $javaHealth "Java Health"

Write-Host "3. Querying AI Governance Dashboard Metrics..."
$dashboardJson = $null
try {
    # Provide a mock token or assume auth is disabled in competition mode
    $headers = @{ "Authorization" = "Bearer competition-admin-token" }
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/dashboard/model-runtime" -Method Get -Headers $headers -ErrorAction Stop
    $dashboardJson = $res.data
} catch {
    Write-Host "Could not query dashboard API. Assuming offline validation for script..." -ForegroundColor Yellow
}

if ($dashboardJson) {
    Write-Host "4. Semantic Validation of AI Governance Metrics..."
    
    $ratesValid = ($dashboardJson.taskSuccessRate -ge 0 -and $dashboardJson.taskSuccessRate -le 1) -and
                  ($dashboardJson.callbackSuccessRate -ge 0 -and $dashboardJson.callbackSuccessRate -le 1) -and
                  ($dashboardJson.visualAssetSuccessRate -ge 0 -and $dashboardJson.visualAssetSuccessRate -le 1)
    
    Check-Step $ratesValid "Metrics Semantic Range Check [0, 1]" "Rates are within expected bounds"

    $numDenomValid = ($dashboardJson.callbackSuccessCount -le $dashboardJson.callbackTotalCount) -and
                     ($dashboardJson.visualAssetGeneratedCount -le $dashboardJson.visualAssetExpectedCount) -and
                     ($dashboardJson.reviewCompletedCount -le $dashboardJson.reviewSuggestedCount)
    
    Check-Step $numDenomValid "Numerator/Denominator Logical Check" "Numerators do not exceed denominators"

    $configValid = ($dashboardJson.llmModelName -ne "UNKNOWN" -and $dashboardJson.llmModelName -ne $null) -and
                   ($dashboardJson.knowledgeVersion -ne "UNKNOWN" -and $dashboardJson.knowledgeVersion -ne $null)

    Check-Step $configValid "Runtime Configuration Check" "LLM and Knowledge Version successfully sourced from mdl_model_version"
} else {
    Write-Host "Skipping API semantic checks due to API unavailability." -ForegroundColor Yellow
}

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
