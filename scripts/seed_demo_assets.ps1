$ErrorActionPreference = "Stop"

Write-Host "### Seeding MinIO Demo Assets ###" -ForegroundColor Cyan

$ContainerName = "caries-minio-init"
$Buckets = @("caries/caries-image")
$Files = @(
    "demo_assets/pano_CA-2026-LOW.jpg",
    "demo_assets/peri1_CA-2026-LOW.jpg",
    "demo_assets/pano_CA-2026-HIGH.jpg",
    "demo_assets/peri1_CA-2026-HIGH.jpg",
    "demo_assets/peri2_CA-2026-HIGH.jpg"
)

# We use a 1x1 base64 JPEG block as the dummy payload to prevent purely empty files that fail visual inspection
$B64_1x1_JPG = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="

foreach ($File in $Files) {
    Write-Host "Uploading mock image to $File..."
    $Cmd = "echo $B64_1x1_JPG | base64 -d > /tmp/dummy.jpg && mc cp /tmp/dummy.jpg caries/caries-image/$File"
    
    # Execute inside the minio-init container which already has 'mc' configured
    docker exec -i $ContainerName sh -c $Cmd | Out-Null
}

Write-Host "Demo assets seeded successfully!" -ForegroundColor Green