[CmdletBinding()]
param(
    [string]$DatasetRoot = "",
    [string]$OfficialValidationDir = "",
    [string]$OutputRoot = "",
    [ValidateRange(0.05, 0.5)]
    [double]$ValidationRatio = 0.2,
    [int]$Seed = 42,
    [ValidateSet("HardLink", "Copy")]
    [string]$ImageMode = "HardLink"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
if (-not $DatasetRoot) {
    $DatasetRoot = Join-Path $repoRoot "data\raw\dentex\training_data\training_data\quadrant-enumeration-disease"
}
if (-not $OfficialValidationDir) {
    $OfficialValidationDir = Join-Path $repoRoot "data\raw\dentex\official_validation"
}
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $repoRoot "data\processed\dentex\yolo"
}

$DatasetRoot = [IO.Path]::GetFullPath($DatasetRoot)
$OfficialValidationDir = [IO.Path]::GetFullPath($OfficialValidationDir)
$OutputRoot = [IO.Path]::GetFullPath($OutputRoot)
$annotationPath = Join-Path $DatasetRoot "train_quadrant_enumeration_disease.json"
$sourceImageDir = Join-Path $DatasetRoot "xrays"

foreach ($requiredPath in @($annotationPath, $sourceImageDir, $OfficialValidationDir)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required DENTEX path does not exist: $requiredPath"
    }
}

$imageDirs = @{
    train = Join-Path $OutputRoot "images\train"
    val = Join-Path $OutputRoot "images\val"
}
$labelDirs = @{
    train = Join-Path $OutputRoot "labels\train"
    val = Join-Path $OutputRoot "labels\val"
}
$manifestDir = Join-Path ([IO.Path]::GetDirectoryName($OutputRoot)) "manifests"

foreach ($directory in @($imageDirs.train, $imageDirs.val, $labelDirs.train, $labelDirs.val, $manifestDir)) {
    [void](New-Item -ItemType Directory -Force -Path $directory)
}

$existingGeneratedFiles = @(
    Get-ChildItem -LiteralPath $imageDirs.train, $imageDirs.val, $labelDirs.train, $labelDirs.val -File -ErrorAction SilentlyContinue
)
if ($existingGeneratedFiles.Count -gt 0) {
    throw "Output directories are not empty. Refusing to mix datasets: $OutputRoot"
}

$dataset = Get-Content -LiteralPath $annotationPath -Raw -Encoding UTF8 | ConvertFrom-Json
$images = @($dataset.images)
$annotations = @($dataset.annotations)
$categories = @($dataset.categories_3 | Sort-Object { [int]$_.id })

if ($images.Count -eq 0 -or $annotations.Count -eq 0 -or $categories.Count -eq 0) {
    throw "DENTEX disease annotation JSON is empty or has an unexpected schema: $annotationPath"
}

$categoryNames = @{}
foreach ($category in $categories) {
    $categoryNames[[int]$category.id] = [string]$category.name
}

$imageById = @{}
foreach ($image in $images) {
    $imageId = [int]$image.id
    if ($imageById.ContainsKey($imageId)) {
        throw "Duplicate image id in annotation JSON: $imageId"
    }
    $sourcePath = Join-Path $sourceImageDir ([string]$image.file_name)
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Annotated image is missing from disk: $sourcePath"
    }
    $imageById[$imageId] = $image
}

$annotationsByImage = @{}
foreach ($annotation in $annotations) {
    $imageId = [int]$annotation.image_id
    if (-not $imageById.ContainsKey($imageId)) {
        throw "Annotation references an unknown image id: $imageId"
    }
    $categoryId = [int]$annotation.category_id_3
    if (-not $categoryNames.ContainsKey($categoryId)) {
        throw "Annotation references an unknown disease category id: $categoryId"
    }
    $bbox = @($annotation.bbox)
    if ($bbox.Count -ne 4 -or [double]$bbox[2] -le 0 -or [double]$bbox[3] -le 0) {
        throw "Invalid COCO bbox in annotation id $($annotation.id)"
    }
    if (-not $annotationsByImage.ContainsKey($imageId)) {
        $annotationsByImage[$imageId] = [Collections.Generic.List[object]]::new()
    }
    $annotationsByImage[$imageId].Add($annotation)
}

function Shuffle-Items {
    param([object[]]$Items, [int]$RandomSeed)
    $result = @($Items)
    $random = [Random]::new($RandomSeed)
    for ($index = $result.Count - 1; $index -gt 0; $index--) {
        $swapIndex = $random.Next($index + 1)
        $temporary = $result[$index]
        $result[$index] = $result[$swapIndex]
        $result[$swapIndex] = $temporary
    }
    return $result
}

function Format-YoloNumber {
    param([double]$Value)
    return $Value.ToString("0.########", [Globalization.CultureInfo]::InvariantCulture)
}

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    [IO.File]::WriteAllText($Path, $Content, [Text.UTF8Encoding]::new($false))
}

function Add-ImageFile {
    param([string]$Source, [string]$Target, [string]$Mode)
    if ($Mode -eq "Copy") {
        Copy-Item -LiteralPath $Source -Destination $Target
        return
    }
    try {
        [void](New-Item -ItemType HardLink -Path $Target -Target $Source)
    }
    catch {
        throw "Unable to create hard link '$Target'. Re-run with -ImageMode Copy if source and output are on different drives. $($_.Exception.Message)"
    }
}

$shuffledImages = @(Shuffle-Items -Items $images -RandomSeed $Seed)
$validationCount = [Math]::Max(1, [Math]::Round($images.Count * $ValidationRatio))
$validationIds = [Collections.Generic.HashSet[int]]::new()
for ($index = 0; $index -lt $validationCount; $index++) {
    [void]$validationIds.Add([int]$shuffledImages[$index].id)
}

$splitRows = [Collections.Generic.List[object]]::new()
$splitAnnotationCounts = @{
    train = @{}
    val = @{}
}
foreach ($splitName in @("train", "val")) {
    foreach ($categoryId in $categoryNames.Keys) {
        $splitAnnotationCounts[$splitName][$categoryId] = 0
    }
}

foreach ($image in ($images | Sort-Object { [int]$_.id })) {
    $imageId = [int]$image.id
    $splitName = if ($validationIds.Contains($imageId)) { "val" } else { "train" }
    $fileName = [string]$image.file_name
    $sourcePath = Join-Path $sourceImageDir $fileName
    $targetImagePath = Join-Path $imageDirs[$splitName] $fileName
    $targetLabelPath = Join-Path $labelDirs[$splitName] (([IO.Path]::GetFileNameWithoutExtension($fileName)) + ".txt")
    $width = [double]$image.width
    $height = [double]$image.height
    if ($width -le 0 -or $height -le 0) {
        throw "Invalid image dimensions for image id $imageId"
    }

    Add-ImageFile -Source $sourcePath -Target $targetImagePath -Mode $ImageMode

    $labelLines = [Collections.Generic.List[string]]::new()
    $imageCategoryIds = [Collections.Generic.HashSet[int]]::new()
    $imageAnnotations = @()
    if ($annotationsByImage.ContainsKey($imageId)) {
        $imageAnnotations = @($annotationsByImage[$imageId])
    }
    foreach ($annotation in $imageAnnotations) {
        $categoryId = [int]$annotation.category_id_3
        $bbox = @($annotation.bbox)
        $x1 = [Math]::Max(0.0, [double]$bbox[0])
        $y1 = [Math]::Max(0.0, [double]$bbox[1])
        $x2 = [Math]::Min($width, [double]$bbox[0] + [double]$bbox[2])
        $y2 = [Math]::Min($height, [double]$bbox[1] + [double]$bbox[3])
        if ($x2 -le $x1 -or $y2 -le $y1) {
            throw "COCO bbox is outside image bounds for annotation id $($annotation.id)"
        }
        $centerX = (($x1 + $x2) / 2.0) / $width
        $centerY = (($y1 + $y2) / 2.0) / $height
        $boxWidth = ($x2 - $x1) / $width
        $boxHeight = ($y2 - $y1) / $height
        $labelLines.Add(
            "$categoryId $(Format-YoloNumber $centerX) $(Format-YoloNumber $centerY) $(Format-YoloNumber $boxWidth) $(Format-YoloNumber $boxHeight)"
        )
        $splitAnnotationCounts[$splitName][$categoryId]++
        [void]$imageCategoryIds.Add($categoryId)
    }
    Write-Utf8NoBom -Path $targetLabelPath -Content (($labelLines -join "`n") + "`n")

    $splitRows.Add([ordered]@{
        imageId = $imageId
        fileName = $fileName
        split = $splitName
        width = [int]$width
        height = [int]$height
        annotationCount = $labelLines.Count
        diseaseCategoryIds = @($imageCategoryIds | Sort-Object)
        sourceImagePath = $sourcePath
        imagePath = $targetImagePath
        labelPath = $targetLabelPath
    })
}

$officialImages = @(
    Get-ChildItem -LiteralPath $OfficialValidationDir -File -Filter "val_*.png" |
        Where-Object { $_.Name -notlike "*checkpoint*" } |
        Sort-Object {
            if ($_.BaseName -match "^val_(\d+)$") { [int]$Matches[1] } else { [int]::MaxValue }
        }
)

$datasetYamlLines = [Collections.Generic.List[string]]::new()
$datasetYamlLines.Add("path: $($OutputRoot.Replace('\', '/'))")
$datasetYamlLines.Add("train: images/train")
$datasetYamlLines.Add("val: images/val")
$datasetYamlLines.Add("names:")
foreach ($categoryId in ($categoryNames.Keys | Sort-Object)) {
    $datasetYamlLines.Add("  $categoryId`: $($categoryNames[$categoryId].Replace(' ', '_'))")
}
Write-Utf8NoBom -Path (Join-Path $OutputRoot "dataset.yaml") -Content (($datasetYamlLines -join "`n") + "`n")

$trainImagePaths = @($splitRows | Where-Object split -eq "train" | ForEach-Object { ([string]$_.imagePath).Replace('\', '/') })
$valImagePaths = @($splitRows | Where-Object split -eq "val" | ForEach-Object { ([string]$_.imagePath).Replace('\', '/') })
Write-Utf8NoBom -Path (Join-Path $manifestDir "train.txt") -Content (($trainImagePaths -join "`n") + "`n")
Write-Utf8NoBom -Path (Join-Path $manifestDir "val.txt") -Content ((@($valImagePaths) -join "`n") + "`n")
$officialImagePaths = @($officialImages | ForEach-Object { $_.FullName.Replace('\', '/') })
Write-Utf8NoBom -Path (Join-Path $manifestDir "official_validation.txt") -Content (($officialImagePaths -join "`n") + "`n")

$jsonLines = @($splitRows | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 6 })
Write-Utf8NoBom -Path (Join-Path $manifestDir "split.jsonl") -Content (($jsonLines -join "`n") + "`n")

$summary = [ordered]@{
    dataset = "DENTEX quadrant-enumeration-disease"
    sourceAnnotation = $annotationPath
    sourceImageDirectory = $sourceImageDir
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    seed = $Seed
    validationRatio = $ValidationRatio
    imageMode = $ImageMode
    images = [ordered]@{
        total = $images.Count
        train = $trainImagePaths.Count
        val = $valImagePaths.Count
        officialValidation = $officialImages.Count
    }
    annotations = [ordered]@{
        total = $annotations.Count
        train = ($splitAnnotationCounts.train.Values | Measure-Object -Sum).Sum
        val = ($splitAnnotationCounts.val.Values | Measure-Object -Sum).Sum
    }
    classes = @(
        foreach ($categoryId in ($categoryNames.Keys | Sort-Object)) {
            [ordered]@{
                id = $categoryId
                name = $categoryNames[$categoryId]
                trainAnnotations = $splitAnnotationCounts.train[$categoryId]
                valAnnotations = $splitAnnotationCounts.val[$categoryId]
            }
        }
    )
    limitations = @(
        "The official validation images have no public ground-truth labels and are excluded from training.",
        "DENTEX does not expose patient identifiers, so this split is deterministic at image level rather than patient level.",
        "The disease annotations localize abnormal teeth; their polygons must not be presented as pixel-level caries lesion masks."
    )
}
$summaryJson = $summary | ConvertTo-Json -Depth 8
Write-Utf8NoBom -Path (Join-Path $manifestDir "dataset_summary.json") -Content ($summaryJson + "`n")

Write-Output "DENTEX YOLO preparation completed."
Write-Output "Dataset YAML: $(Join-Path $OutputRoot 'dataset.yaml')"
Write-Output "Train images: $($trainImagePaths.Count)"
Write-Output "Validation images: $($valImagePaths.Count)"
Write-Output "Official validation images: $($officialImages.Count)"
foreach ($categoryId in ($categoryNames.Keys | Sort-Object)) {
    Write-Output ("Class {0} {1}: train={2}, val={3}" -f $categoryId, $categoryNames[$categoryId], $splitAnnotationCounts.train[$categoryId], $splitAnnotationCounts.val[$categoryId])
}
