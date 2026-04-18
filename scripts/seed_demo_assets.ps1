$ErrorActionPreference = "Continue"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

Write-Host "### Seeding Demo Assets for Competition Mode ###" -ForegroundColor Cyan
Write-Host "This script provisions baseline visual assets for the predefined demo cases." -ForegroundColor Yellow

# Assuming standard compose volume maps 'evidence/generated' directly to MinIO or local processing dir.
# The DB has these precise keys in 'med_attachment':
# demo_assets/pano_CA-2026-LOW.jpg
# demo_assets/peri1_CA-2026-LOW.jpg
# demo_assets/pano_CA-2026-HIGH.jpg
# demo_assets/peri1_CA-2026-HIGH.jpg
# demo_assets/peri2_CA-2026-HIGH.jpg

$DemoAssetDir = Join-Path "evidence" (Join-Path "generated" "demo_assets")
if (-not (Test-Path -Path $DemoAssetDir)) {
    New-Item -ItemType Directory -Force -Path $DemoAssetDir | Out-Null
}

$TargetAssets = @(
    "pano_CA-2026-LOW.jpg",
    "peri1_CA-2026-LOW.jpg",
    "pano_CA-2026-HIGH.jpg",
    "peri1_CA-2026-HIGH.jpg",
    "peri2_CA-2026-HIGH.jpg"
)

foreach ($Target in $TargetAssets) {
    $FilePath = Join-Path $DemoAssetDir $Target
    Write-Host "Generating placeholder asset at $FilePath" -ForegroundColor Gray
    
    # In a real environment, we would copy real images or use MinIO CLI (mc).
    # Here we just write a dummy file payload to signify presence.
    "DEMO_ASSET_PAYLOAD_FOR_$Target" | Out-File -FilePath $FilePath -Encoding utf8
}

Write-Host "Assets seeded locally." -ForegroundColor Green
Write-Host "If running inside Docker, ensure volume mappings cover evidence/generated." -ForegroundColor Yellow
Write-Host "--- Asset Seeding Complete ---" -ForegroundColor Cyan
