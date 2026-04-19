param(
    [string[]]$Urls = @(
        "http://127.0.0.1:8080/actuator/health",
        "http://127.0.0.1:8001/ai/v1/health"
    ),
    [int]$TimeoutSec = 180,
    [int]$IntervalSec = 3
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$deadline = (Get-Date).AddSeconds($TimeoutSec)

foreach ($url in $Urls) {
    Write-Host "Waiting for $url" -ForegroundColor Cyan
    $healthy = $false

    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Method Get -Uri $url -TimeoutSec 5 | Out-Null
            Write-Host "  UP: $url" -ForegroundColor Green
            $healthy = $true
            break
        } catch {
            Start-Sleep -Seconds $IntervalSec
        }
    }

    if (-not $healthy) {
        throw "Timed out waiting for $url after $TimeoutSec seconds."
    }
}
