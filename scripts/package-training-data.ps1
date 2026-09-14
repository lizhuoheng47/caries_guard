[CmdletBinding()]
param(
    [string]$OutputRoot = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $repoRoot ("artifacts\cloud-upload\training-data-by-model_{0}" -f (Get-Date -Format "yyyyMMdd-HHmmss"))
}
$OutputRoot = [IO.Path]::GetFullPath($OutputRoot)

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Content
    )

    $parent = Split-Path -Parent $Path
    if ($parent) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    [IO.File]::WriteAllText($Path, $Content, [Text.UTF8Encoding]::new($false))
}

function Write-JsonFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value,
        [int]$Depth = 20
    )

    Write-Utf8NoBom -Path $Path -Content (($Value | ConvertTo-Json -Depth $Depth) + "`n")
}

function Assert-ExistingPath {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Required path is missing: $Path"
    }
}

function Copy-FilteredTree {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination,
        [string[]]$ExcludedExtensions = @(".cache")
    )

    Assert-ExistingPath -Path $Source
    $sourceFull = [IO.Path]::GetFullPath($Source).TrimEnd('\')
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null

    foreach ($file in Get-ChildItem -LiteralPath $sourceFull -Recurse -File -Force) {
        if ($ExcludedExtensions -contains $file.Extension.ToLowerInvariant()) {
            continue
        }
        $relative = $file.FullName.Substring($sourceFull.Length).TrimStart('\')
        $target = Join-Path $Destination $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $target -Force
    }
}

function Copy-OneFile {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    Assert-ExistingPath -Path $Source
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}

function Get-PackageInventory {
    param([Parameter(Mandatory = $true)][string]$PackageRoot)

    $packageFull = [IO.Path]::GetFullPath($PackageRoot).TrimEnd('\')
    $rows = foreach ($file in Get-ChildItem -LiteralPath $packageFull -Recurse -File -Force | Sort-Object FullName) {
        if ($file.Name -eq "FILE_MANIFEST.csv") {
            continue
        }
        [pscustomobject]@{
            RelativePath = $file.FullName.Substring($packageFull.Length).TrimStart('\').Replace('\', '/')
            Bytes        = $file.Length
            Sha256       = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    }
    return @($rows)
}

function Complete-PackageMetadata {
    param([Parameter(Mandatory = $true)][string]$PackageRoot)

    $inventory = Get-PackageInventory -PackageRoot $PackageRoot
    $csv = ($inventory | ConvertTo-Csv -NoTypeInformation) -join "`n"
    Write-Utf8NoBom -Path (Join-Path $PackageRoot "FILE_MANIFEST.csv") -Content ($csv + "`n")

    return [pscustomobject]@{
        FileCount = (Get-ChildItem -LiteralPath $PackageRoot -Recurse -File -Force).Count
        Bytes     = (Get-ChildItem -LiteralPath $PackageRoot -Recurse -File -Force | Measure-Object Length -Sum).Sum
    }
}

function New-ZipArchive {
    param(
        [Parameter(Mandatory = $true)][string]$StagingRoot,
        [Parameter(Mandatory = $true)][string]$PackageName,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    Write-Host "Creating archive: $Destination"
    & tar.exe -a -c -f $Destination -C $StagingRoot $PackageName
    if ($LASTEXITCODE -ne 0) {
        throw "tar failed while creating $Destination (exit $LASTEXITCODE)"
    }

    $entries = @(& tar.exe -t -f $Destination)
    if ($LASTEXITCODE -ne 0) {
        throw "tar failed while validating $Destination (exit $LASTEXITCODE)"
    }
    $archiveFileCount = @($entries | Where-Object { $_ -and -not $_.EndsWith('/') }).Count
    $expectedFileCount = (Get-ChildItem -LiteralPath (Join-Path $StagingRoot $PackageName) -Recurse -File -Force).Count
    if ($archiveFileCount -ne $expectedFileCount) {
        throw "Archive entry mismatch for $($PackageName): expected $expectedFileCount files, found $archiveFileCount"
    }

    $archive = Get-Item -LiteralPath $Destination
    return [pscustomobject]@{
        PackageName  = $PackageName
        ArchiveName  = $archive.Name
        ArchiveBytes = $archive.Length
        ArchiveSha256 = (Get-FileHash -LiteralPath $archive.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        EntryCount   = $archiveFileCount
    }
}

function Resolve-BackendManifestPath {
    param([Parameter(Mandatory = $true)][string]$ManifestPath)

    return [IO.Path]::GetFullPath((Join-Path (Join-Path $repoRoot "backend-python") $ManifestPath.Replace('/', '\')))
}

function New-DentexPackage {
    param([Parameter(Mandatory = $true)][string]$PackageRoot)

    $source = Join-Path $repoRoot "data\processed\dentex\yolo"
    $dataset = Join-Path $PackageRoot "dataset"
    Copy-FilteredTree -Source $source -Destination $dataset

    Write-Utf8NoBom -Path (Join-Path $dataset "dataset.yaml") -Content @'
train: images/train
val: images/val
names:
  0: Impacted
  1: Caries
  2: Periapical_Lesion
  3: Deep_Caries

'@

    $sourceManifestDir = Join-Path $repoRoot "data\processed\dentex\manifests"
    $manifestDir = Join-Path $PackageRoot "manifests"
    New-Item -ItemType Directory -Force -Path $manifestDir | Out-Null

    foreach ($split in @("train", "val")) {
        $lines = foreach ($file in Get-ChildItem -LiteralPath (Join-Path $dataset "images\$split") -File | Sort-Object Name) {
            "dataset/images/$split/$($file.Name)"
        }
        Write-Utf8NoBom -Path (Join-Path $manifestDir "$split.txt") -Content (($lines -join "`n") + "`n")
    }

    $portableRows = foreach ($line in Get-Content -LiteralPath (Join-Path $sourceManifestDir "split.jsonl")) {
        if (-not $line.Trim()) { continue }
        $row = $line | ConvertFrom-Json
        $row.sourceImagePath = "source://DENTEX/quadrant-enumeration-disease/xrays/$($row.fileName)"
        $row.imagePath = "dataset/images/$($row.split)/$($row.fileName)"
        $row.labelPath = "dataset/labels/$($row.split)/$([IO.Path]::GetFileNameWithoutExtension($row.fileName)).txt"
        $row | ConvertTo-Json -Compress -Depth 10
    }
    Write-Utf8NoBom -Path (Join-Path $manifestDir "split.jsonl") -Content (($portableRows -join "`n") + "`n")

    $summary = Get-Content -Raw -LiteralPath (Join-Path $sourceManifestDir "dataset_summary.json") | ConvertFrom-Json
    $summary.sourceAnnotation = "source://DENTEX/quadrant-enumeration-disease/train_quadrant_enumeration_disease.json"
    $summary.sourceImageDirectory = "source://DENTEX/quadrant-enumeration-disease/xrays"
    $summary.imageMode = "PackagedCopy"
    Write-JsonFile -Path (Join-Path $manifestDir "dataset_summary.json") -Value $summary

    Copy-OneFile -Source (Join-Path $repoRoot "backend-python\training\scripts\train_dentex_detection.py") -Destination (Join-Path $PackageRoot "metadata\train_dentex_detection.py")
    Copy-OneFile -Source (Join-Path $repoRoot "artifacts\training\dentex_detection\dentex_yolov8n_v1\cariesguard_model_metadata.json") -Destination (Join-Path $PackageRoot "metadata\trained_model_metadata.json")

    $info = [ordered]@{
        schemaVersion = "1.0"
        packageType = "training-data"
        completeness = "COMPLETE"
        modelCode = "dentex-disease-detect-yolov8n-v1"
        task = "object-detection"
        framework = "Ultralytics YOLO"
        dataset = "DENTEX quadrant-enumeration-disease"
        splits = [ordered]@{ train = 564; val = 141 }
        classes = @("Impacted", "Caries", "Periapical_Lesion", "Deep_Caries")
        trainingEntryPoint = "metadata/train_dentex_detection.py"
        datasetConfig = "dataset/dataset.yaml"
        notes = @(
            "Contains the exact prepared train/validation images and YOLO labels used by the model.",
            "Ultralytics cache files, raw source archives, unrelated DENTEX tasks, and evaluation-only images are excluded.",
            "Model weights and training run artifacts are not training data and are not included."
        )
    }
    Write-JsonFile -Path (Join-Path $PackageRoot "PACKAGE_INFO.json") -Value $info
    Write-Utf8NoBom -Path (Join-Path $PackageRoot "README.md") -Content @'
# DENTEX YOLOv8n 训练数据包

- 模型：`dentex-disease-detect-yolov8n-v1`
- 任务：4 类牙病目标检测
- 数据：564 张训练图、141 张验证图，以及对应 YOLO 标签
- 入口：`dataset/dataset.yaml`

这是训练就绪副本。绝对本机路径已移除；原始压缩包、YOLO 缓存、无关 DENTEX 子任务、仅评估数据、模型权重和训练产物未纳入。

'@
}

function New-PediatricPackage {
    param([Parameter(Mandatory = $true)][string]$PackageRoot)

    $source = Join-Path $repoRoot "data\processed\children_dental\yolo_pediatric_2class"
    $dataset = Join-Path $PackageRoot "dataset"
    Copy-FilteredTree -Source $source -Destination $dataset

    Write-Utf8NoBom -Path (Join-Path $dataset "dataset.yaml") -Content @'
train: images/train
val: images/val
test: images/test
names:
  0: Caries
  1: Periapical_Lesion

'@

    $portableRows = foreach ($line in Get-Content -LiteralPath (Join-Path $source "manifest.jsonl")) {
        if (-not $line.Trim()) { continue }
        $row = $line | ConvertFrom-Json
        $row.sourceImage = "source://ChildrensDental/$($row.officialSplit)/images/$($row.fileName)"
        $row.sourceLabel = "source://ChildrensDental/$($row.officialSplit)/label/$([IO.Path]::GetFileNameWithoutExtension($row.fileName)).json"
        $row | Add-Member -NotePropertyName imagePath -NotePropertyValue "dataset/images/$($row.split)/$($row.fileName)" -Force
        $row | Add-Member -NotePropertyName labelPath -NotePropertyValue "dataset/labels/$($row.split)/$([IO.Path]::GetFileNameWithoutExtension($row.fileName)).txt" -Force
        $row | ConvertTo-Json -Compress -Depth 10
    }
    Write-Utf8NoBom -Path (Join-Path $dataset "manifest.jsonl") -Content (($portableRows -join "`n") + "`n")

    $summary = Get-Content -Raw -LiteralPath (Join-Path $source "dataset_summary.json") | ConvertFrom-Json
    Write-JsonFile -Path (Join-Path $dataset "dataset_summary.json") -Value $summary

    Copy-OneFile -Source (Join-Path $repoRoot "backend-python\training\scripts\train_pediatric_detection.py") -Destination (Join-Path $PackageRoot "metadata\train_pediatric_detection.py")
    Copy-OneFile -Source (Join-Path $repoRoot "artifacts\training\pediatric_detection\pediatric_yolov8n_2class_v1\cariesguard_model_metadata.json") -Destination (Join-Path $PackageRoot "metadata\trained_model_metadata.json")

    $info = [ordered]@{
        schemaVersion = "1.0"
        packageType = "training-data"
        completeness = "COMPLETE"
        modelCode = "pediatric-disease-detect-yolov8n-2class-v1"
        task = "object-detection"
        framework = "Ultralytics YOLO"
        dataset = "Pediatric dental disease detection two-class subset"
        splits = [ordered]@{ train = 56; val = 14; test = 30 }
        classes = @("Caries", "Periapical_Lesion")
        trainingEntryPoint = "metadata/train_pediatric_detection.py"
        datasetConfig = "dataset/dataset.yaml"
        notes = @(
            "Contains the prepared train/validation data and the held-out official test split.",
            "Ultralytics cache files, raw source archives, model weights, and training run artifacts are excluded."
        )
    }
    Write-JsonFile -Path (Join-Path $PackageRoot "PACKAGE_INFO.json") -Value $info
    Write-Utf8NoBom -Path (Join-Path $PackageRoot "README.md") -Content @'
# 儿童牙科 YOLOv8n 两类检测训练数据包

- 模型：`pediatric-disease-detect-yolov8n-2class-v1`
- 任务：龋病 / 根尖周病变目标检测
- 数据：56 张训练图、14 张验证图、30 张官方测试图，以及对应 YOLO 标签
- 入口：`dataset/dataset.yaml`

这是训练与复评就绪副本。绝对本机路径已移除；原始压缩包、YOLO 缓存、模型权重和训练产物未纳入。

'@
}

function New-Dc1000Package {
    param([Parameter(Mandatory = $true)][string]$PackageRoot)

    $source = Join-Path $repoRoot "data\processed\dc1000\segmentation_v1"
    $dataset = Join-Path $PackageRoot "dataset"
    $manifestOutput = Join-Path $dataset "manifests"
    New-Item -ItemType Directory -Force -Path $manifestOutput | Out-Null

    foreach ($split in @("train", "val", "test")) {
        $sourceManifest = Join-Path $source "manifests\$split.jsonl"
        $portableRows = [Collections.Generic.List[string]]::new()
        foreach ($line in Get-Content -LiteralPath $sourceManifest) {
            if (-not $line.Trim()) { continue }
            $row = $line | ConvertFrom-Json
            $sourceImage = Resolve-BackendManifestPath -ManifestPath $row.imagePath
            $sourceMask = Resolve-BackendManifestPath -ManifestPath $row.maskPath
            Assert-ExistingPath -Path $sourceImage
            Assert-ExistingPath -Path $sourceMask

            $fileName = [IO.Path]::GetFileName($sourceImage)
            $imageDestination = Join-Path $dataset "images\$split\$fileName"
            $maskDestination = Join-Path $dataset "masks\$split\$fileName"
            Copy-OneFile -Source $sourceImage -Destination $imageDestination
            Copy-OneFile -Source $sourceMask -Destination $maskDestination

            $row.imagePath = "dataset/images/$split/$fileName"
            $row.maskPath = "dataset/masks/$split/$fileName"
            if ($row.PSObject.Properties.Name -contains "sourceMaskPath") {
                $row.sourceMaskPath = "source://DC1000/$($row.sourceSplit)/masks/$fileName"
            }
            $portableRows.Add(($row | ConvertTo-Json -Compress -Depth 10))
        }
        Write-Utf8NoBom -Path (Join-Path $manifestOutput "$split.jsonl") -Content (($portableRows -join "`n") + "`n")
    }

    Copy-OneFile -Source (Join-Path $source "dataset_card.md") -Destination (Join-Path $dataset "meta\dataset_card.md")
    Copy-OneFile -Source (Join-Path $source "audit.json") -Destination (Join-Path $dataset "meta\audit.json")
    Copy-OneFile -Source (Join-Path $repoRoot "backend-python\assets\datasets\caries_v1\meta\class_map.json") -Destination (Join-Path $dataset "meta\class_map.json")

    $summary = Get-Content -Raw -LiteralPath (Join-Path $source "summary.json") | ConvertFrom-Json
    $summary.sourceDirectory = "source://DC1000/DC1000_dataset"
    $summary.outputDirectory = "dataset"
    $summary.manifests.train = "dataset/manifests/train.jsonl"
    $summary.manifests.val = "dataset/manifests/val.jsonl"
    $summary.manifests.test = "dataset/manifests/test.jsonl"
    Write-JsonFile -Path (Join-Path $dataset "meta\summary.json") -Value $summary

    Copy-OneFile -Source (Join-Path $repoRoot "backend-python\training\scripts\train_dc1000_unet.py") -Destination (Join-Path $PackageRoot "metadata\train_dc1000_unet.py")
    Copy-OneFile -Source (Join-Path $repoRoot "backend-python\assets\models\manifests\segmentation_v1.yaml") -Destination (Join-Path $PackageRoot "metadata\model_manifest.yaml")

    $info = [ordered]@{
        schemaVersion = "1.0"
        packageType = "training-data"
        completeness = "COMPLETE"
        modelCode = "dc1000-unet-v1"
        task = "binary-segmentation"
        framework = "PyTorch + MONAI UNet"
        dataset = "DC1000 binary caries segmentation v1"
        splits = [ordered]@{ train = 394; val = 99; test = 100 }
        classes = @("background", "caries_lesion")
        trainingEntryPoint = "metadata/train_dc1000_unet.py"
        datasetManifests = [ordered]@{
            train = "dataset/manifests/train.jsonl"
            val = "dataset/manifests/val.jsonl"
            test = "dataset/manifests/test.jsonl"
        }
        notes = @(
            "The original panoramics referenced by the training manifests and the converted binary masks are copied into this self-contained package.",
            "Publisher-provided correlated slice/augmentation data was not used by this model and is excluded to preserve the leakage-resistant split.",
            "Raw source archives, source-format masks, model weights, and training run artifacts are excluded."
        )
    }
    Write-JsonFile -Path (Join-Path $PackageRoot "PACKAGE_INFO.json") -Value $info
    Write-Utf8NoBom -Path (Join-Path $PackageRoot "README.md") -Content @'
# DC1000 MONAI U-Net 分割训练数据包

- 模型：`dc1000-unet-v1`
- 任务：二值龋损分割
- 数据：394 组训练图/掩膜、99 组验证图/掩膜、100 组官方测试图/掩膜
- 清单：`dataset/manifests/*.jsonl`

原项目清单引用了 raw 目录中的原图；本包已复制实际引用的原图，并把清单改为包内相对路径，因此可以独立保存和复训。未参与该模型训练的发布方切片/增强数据、原始压缩包、源格式掩膜、模型权重和训练产物未纳入。

'@
}

if (Test-Path -LiteralPath $OutputRoot) {
    throw "Output directory already exists; refusing to overwrite: $OutputRoot"
}

New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null
$stagingRoot = Join-Path $OutputRoot "_staging"
New-Item -ItemType Directory -Path $stagingRoot -Force | Out-Null

$packageDefinitions = @(
    [pscustomobject]@{ Name = "dentex-disease-detect-yolov8n-v1"; Builder = "New-DentexPackage" },
    [pscustomobject]@{ Name = "pediatric-disease-detect-yolov8n-2class-v1"; Builder = "New-PediatricPackage" },
    [pscustomobject]@{ Name = "dc1000-unet-v1"; Builder = "New-Dc1000Package" }
)

$archiveResults = [Collections.Generic.List[object]]::new()
foreach ($definition in $packageDefinitions) {
    Write-Output "Staging package: $($definition.Name)"
    $packageRoot = Join-Path $stagingRoot $definition.Name
    New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null
    & $definition.Builder -PackageRoot $packageRoot
    $packageStats = Complete-PackageMetadata -PackageRoot $packageRoot
    $archivePath = Join-Path $OutputRoot ($definition.Name + ".zip")
    $archiveResult = New-ZipArchive -StagingRoot $stagingRoot -PackageName $definition.Name -Destination $archivePath
    $archiveResult | Add-Member -NotePropertyName SourceFileCount -NotePropertyValue $packageStats.FileCount
    $archiveResult | Add-Member -NotePropertyName SourceBytes -NotePropertyValue $packageStats.Bytes
    $archiveResults.Add($archiveResult)
    Write-Output "Finished package: $($definition.Name)"
}

$missingData = [ordered]@{
    status = "SOURCE_DATA_MISSING"
    affectedModels = @(
        "caries-segmentation-techval-monai-unet-poc-v1",
        "traindata_seg_20260503_mask01_fg40",
        "traindata_grading_20260503_mask01"
    )
    expectedPaths = @(
        "data/traindata",
        "backend-python/training/outputs/datasets/traindata/masks_class_id"
    )
    availableMetadata = @(
        "backend-python/training/outputs/datasets/traindata/train.jsonl",
        "backend-python/training/outputs/datasets/traindata/val.jsonl",
        "backend-python/training/outputs/datasets/traindata/summary.json"
    )
    explanation = "The manifests describe 22 image-mask pairs, but the referenced source images and converted masks are absent from the current workspace, so no complete upload package can be produced for these models."
}
Write-JsonFile -Path (Join-Path $OutputRoot "MISSING_DATA_REPORT.json") -Value $missingData
Write-Utf8NoBom -Path (Join-Path $OutputRoot "MISSING_DATA_REPORT.md") -Content @'
# 未能打包的数据

以下模型共用的 `data/traindata` 当前不在工作区，因此没有生成冒充完整数据集的压缩包：

- `caries-segmentation-techval-monai-unet-poc-v1`
- `traindata_seg_20260503_mask01_fg40`
- `traindata_grading_20260503_mask01`

现有清单描述 22 组图像/掩膜，但缺少 `data/traindata` 原图与 `backend-python/training/outputs/datasets/traindata/masks_class_id` 转换掩膜。恢复这些目录后可重新运行 `scripts/package-training-data.ps1`。

`caries-grading-v1` 的正式模型清单状态为 `SPEC_ONLY`，并不存在一套完整的正式训练数据，未单独打包。

'@

$uploadManifest = [ordered]@{
    schemaVersion = "1.0"
    generatedAt = (Get-Date).ToString("o")
    sourceWorkspace = $repoRoot
    packagingPolicy = [ordered]@{
        grouping = "one archive per trained model"
        includes = @("training-ready images", "labels or masks", "train/val/test manifests", "portable dataset configuration", "file-level SHA-256 manifest")
        excludes = @("duplicate raw archives", "cache files", "unrelated or unused datasets", "model weights", "training run artifacts")
    }
    archives = @($archiveResults)
    missingDataReport = "MISSING_DATA_REPORT.json"
}
Write-JsonFile -Path (Join-Path $OutputRoot "UPLOAD_MANIFEST.json") -Value $uploadManifest

$checksumLines = foreach ($archive in $archiveResults) {
    "$($archive.ArchiveSha256)  $($archive.ArchiveName)"
}
Write-Utf8NoBom -Path (Join-Path $OutputRoot "SHA256SUMS.txt") -Content (($checksumLines -join "`n") + "`n")

Write-Utf8NoBom -Path (Join-Path $OutputRoot "README.md") -Content @'
# 按模型分组的训练数据上传包

本目录包含 3 个完整、可独立保存的训练数据 ZIP：

1. `dentex-disease-detect-yolov8n-v1.zip`
2. `pediatric-disease-detect-yolov8n-2class-v1.zip`
3. `dc1000-unet-v1.zip`

上传时请把 3 个 ZIP、`UPLOAD_MANIFEST.json` 和 `SHA256SUMS.txt` 一并上传。上传后可用 SHA-256 核对文件完整性。

原始下载压缩包、缓存、重复副本、未参与对应模型训练的数据、模型权重与训练运行产物均未重复装入。`MISSING_DATA_REPORT.md` 记录了当前工作区缺失、无法完整打包的 `traindata` 数据。

'@

$outputPrefix = $OutputRoot.TrimEnd('\') + '\'
$stagingFull = [IO.Path]::GetFullPath($stagingRoot)
if (-not $stagingFull.StartsWith($outputPrefix, [StringComparison]::OrdinalIgnoreCase) -or (Split-Path -Leaf $stagingFull) -ne "_staging") {
    throw "Refusing to remove unexpected staging path: $stagingFull"
}
Remove-Item -LiteralPath $stagingFull -Recurse -Force

Write-Output "Packaging complete: $OutputRoot"
$archiveResults | Format-Table PackageName, ArchiveName, ArchiveBytes, EntryCount, ArchiveSha256 -AutoSize
