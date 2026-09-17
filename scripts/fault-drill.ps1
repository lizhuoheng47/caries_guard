[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param(
    [string]$ComposeFile = (Join-Path $PSScriptRoot '..\docker-compose.yml'),
    [string]$JavaHealthUrl = 'http://127.0.0.1:8080/actuator/health',
    [string]$PythonHealthUrl = 'http://127.0.0.1:8001/ai/v1/health',
    [int]$RecoveryTimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
$resolvedCompose = (Resolve-Path -LiteralPath $ComposeFile).Path

if (-not $PSCmdlet.ShouldProcess('backend-python container', 'Pause, verify Java isolation, then unpause and verify recovery')) {
    return
}

$running = & docker compose -f $resolvedCompose ps --services --status running
if ($LASTEXITCODE -ne 0 -or $running -notcontains 'backend-python' -or $running -notcontains 'backend-java') {
    throw 'backend-java and backend-python must both be running before the drill.'
}

$paused = $false
try {
    & docker compose -f $resolvedCompose pause backend-python
    if ($LASTEXITCODE -ne 0) { throw 'Failed to pause backend-python.' }
    $paused = $true

    $javaHealth = Invoke-RestMethod -Uri $JavaHealthUrl -TimeoutSec 10
    if ($javaHealth.status -ne 'UP') {
        throw "Java service did not remain healthy while the AI worker was paused: $($javaHealth.status)"
    }
    Write-Host 'Isolation check passed: Java remained healthy while the AI worker was paused.'
}
finally {
    if ($paused) {
        & docker compose -f $resolvedCompose unpause backend-python
    }
}

$deadline = (Get-Date).AddSeconds($RecoveryTimeoutSeconds)
do {
    try {
        $pythonHealth = Invoke-RestMethod -Uri $PythonHealthUrl -TimeoutSec 10
        if ($pythonHealth.code -eq '00000' -and $pythonHealth.data.status -eq 'UP') {
            Write-Host 'Recovery check passed: Python service returned to UP.'
            exit 0
        }
    }
    catch {
        Start-Sleep -Seconds 2
    }
} while ((Get-Date) -lt $deadline)

throw "Python service did not recover within $RecoveryTimeoutSeconds seconds."
