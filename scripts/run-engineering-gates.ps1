[CmdletBinding()]
param(
    [switch]$IncludeLoadTest,
    [string]$LoadTestUrl = 'http://127.0.0.1:8080/actuator/health'
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Invoke-Gate([string]$Name, [scriptblock]$Action) {
    Write-Host "==> $Name"
    & $Action
    if ($LASTEXITCODE -ne 0) { throw "$Name failed with exit code $LASTEXITCODE" }
}

Push-Location (Join-Path $projectRoot 'frontend')
try {
    Invoke-Gate 'Frontend tests' { & npm.cmd test }
    Invoke-Gate 'Frontend production build' { & npm.cmd run build }
}
finally { Pop-Location }

Push-Location (Join-Path $projectRoot 'backend-python')
try {
    $python = if (Test-Path -LiteralPath '.venv\Scripts\python.exe') { '.venv\Scripts\python.exe' } else { 'python' }
    Invoke-Gate 'Python compile check' { & $python -m compileall -q app }
    Invoke-Gate 'Python tests' { & $python -m pytest -q }
}
finally { Pop-Location }

Push-Location (Join-Path $projectRoot 'backend-java')
try { Invoke-Gate 'Java tests' { & mvn test } }
finally { Pop-Location }

Push-Location $projectRoot
try {
    Invoke-Gate 'Compose contract' { & docker compose config --quiet }
    if ($IncludeLoadTest) {
        Invoke-Gate 'HTTP load SLO' { & python scripts/load-test.py --url $LoadTestUrl }
    }
}
finally { Pop-Location }
