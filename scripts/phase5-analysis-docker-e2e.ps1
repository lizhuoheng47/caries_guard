param(
    [string]$JavaBaseUrl = "http://127.0.0.1:8080",
    [string]$PythonBaseUrl = "http://127.0.0.1:8001",
    [string]$MysqlContainer = "caries-mysql",
    [string]$MysqlPassword = $(if ($env:CARIES_MYSQL_PASSWORD) { $env:CARIES_MYSQL_PASSWORD } else { "1234" }),
    [string]$BizDatabase = $(if ($env:CARIES_MYSQL_DATABASE_BIZ) { $env:CARIES_MYSQL_DATABASE_BIZ } else { "caries_biz" }),
    [string]$MinioAccessKey = $(if ($env:CARIES_MINIO_ACCESS_KEY) { $env:CARIES_MINIO_ACCESS_KEY } else { "minioadmin" }),
    [string]$MinioSecretKey = $(if ($env:CARIES_MINIO_SECRET_KEY) { $env:CARIES_MINIO_SECRET_KEY } else { "minioadmin" }),
    [string]$EvidenceDir = "",
    [int]$WaitSeconds = 120,
    [switch]$Phase5COnly,
    [switch]$SkipComposeUp
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-EvidenceDir {
    param([string]$RequestedDir)
    if ([string]::IsNullOrWhiteSpace($RequestedDir)) {
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $RequestedDir = Join-Path "e2e-artifacts" "phase5-analysis-$stamp"
    }
    New-Item -ItemType Directory -Force -Path $RequestedDir | Out-Null
    return (Resolve-Path $RequestedDir).Path
}

function Save-Json {
    param([string]$Name, [object]$Value)
    $path = Join-Path $script:EvidenceRoot $Name
    $Value | ConvertTo-Json -Depth 40 | Out-File -FilePath $path -Encoding UTF8
}

function Save-Text {
    param([string]$Name, [string]$Value)
    $path = Join-Path $script:EvidenceRoot $Name
    $Value | Out-File -FilePath $path -Encoding UTF8
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [string]$Token = ""
    )
    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }
    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers -TimeoutSec 30
    }
    $json = $Body | ConvertTo-Json -Depth 40
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers -ContentType "application/json" -Body $json -TimeoutSec 60
}

function Assert-ApiSuccess {
    param([object]$Response, [string]$StepName)
    if ($null -eq $Response.code -or $Response.code -ne "00000") {
        throw "$StepName failed: code=$($Response.code), message=$($Response.message)"
    }
}

function Wait-Url {
    param([string]$Url, [int]$Attempts = 60)
    for ($i = 1; $i -le $Attempts; $i++) {
        try {
            Invoke-RestMethod -Method GET -Uri $Url -TimeoutSec 5 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    throw "Timed out waiting for $Url"
}

function Invoke-MysqlQuery {
    param([string]$Database, [string]$Sql)
    $output = $Sql | & docker exec -i $MysqlContainer mysql -uroot "-p$MysqlPassword" --batch --raw --default-character-set=utf8mb4 $Database
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed on database $Database"
    }
    return ($output -join [Environment]::NewLine)
}

function Invoke-MysqlScalar {
    param([string]$Database, [string]$Sql)
    $output = $Sql | & docker exec -i $MysqlContainer mysql -uroot "-p$MysqlPassword" --batch --raw --skip-column-names --default-character-set=utf8mb4 $Database
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL scalar query failed on database $Database"
    }
    return (($output | Select-Object -First 1) -as [string]).Trim()
}

function Set-PhaseRuntime {
    param([hashtable]$Scenario)
    $gradingEnabled = if ($Scenario.ContainsKey("gradingEnabled")) { $Scenario.gradingEnabled } else { "false" }
    $gradingForceFail = if ($Scenario.ContainsKey("gradingForceFail")) { $Scenario.gradingForceFail } else { "false" }
    $uncertaintyThreshold = if ($Scenario.ContainsKey("uncertaintyThreshold")) { $Scenario.uncertaintyThreshold } else { "0.35" }
    $env:CG_AI_RUNTIME_MODE = $Scenario.runtimeMode
    $env:CG_MODEL_QUALITY_ENABLED = $Scenario.qualityEnabled
    $env:CG_MODEL_TOOTH_DETECT_ENABLED = $Scenario.toothEnabled
    $env:CG_MODEL_SEGMENTATION_ENABLED = $Scenario.segmentationEnabled
    $env:CG_MODEL_GRADING_ENABLED = $gradingEnabled
    $env:CG_SEGMENTATION_FORCE_FAIL = $Scenario.segmentationForceFail
    $env:CG_GRADING_FORCE_FAIL = $gradingForceFail
    $env:CG_UNCERTAINTY_REVIEW_THRESHOLD = $uncertaintyThreshold
    $env:CG_CALLBACK_VISUAL_ASSET_MODE = "metadata"
    $env:CG_AI_DOWNLOAD_IMAGES = "true"

    $command = "docker compose up -d --build backend-python"
    Save-Text "$($Scenario.name)-docker-command.txt" @"
$command
CG_AI_RUNTIME_MODE=$env:CG_AI_RUNTIME_MODE
CG_MODEL_QUALITY_ENABLED=$env:CG_MODEL_QUALITY_ENABLED
CG_MODEL_TOOTH_DETECT_ENABLED=$env:CG_MODEL_TOOTH_DETECT_ENABLED
CG_MODEL_SEGMENTATION_ENABLED=$env:CG_MODEL_SEGMENTATION_ENABLED
CG_MODEL_GRADING_ENABLED=$env:CG_MODEL_GRADING_ENABLED
CG_SEGMENTATION_FORCE_FAIL=$env:CG_SEGMENTATION_FORCE_FAIL
CG_GRADING_FORCE_FAIL=$env:CG_GRADING_FORCE_FAIL
CG_UNCERTAINTY_REVIEW_THRESHOLD=$env:CG_UNCERTAINTY_REVIEW_THRESHOLD
"@
    & docker compose up -d --build backend-python | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed for scenario $($Scenario.name)"
    }
    Wait-Url "$JavaBaseUrl/actuator/health"
    Wait-Url "$PythonBaseUrl/ai/v1/health"
    Start-Sleep -Seconds 2
}

function New-TestImageObject {
    param([string]$ObjectKey, [string]$ImageKind = "default")
    $code = @"
from io import BytesIO
from minio import Minio
import numpy as np
from PIL import Image

kind = "$ImageKind"
if kind == "phase5c-low-uncertainty":
    rng = np.random.default_rng(501)
    arr = rng.integers(70, 210, (256, 512), dtype=np.uint8)
    arr[40:90, 35:95] = 10
elif kind == "phase5c-high-uncertainty":
    rng = np.random.default_rng(502)
    arr = rng.integers(70, 210, (256, 512), dtype=np.uint8)
    arr[46:80, 43:83] = 105
else:
    rng = np.random.default_rng(500)
    arr = rng.integers(70, 210, (256, 512), dtype=np.uint8)
    arr[96:132, 170:215] = 30
img = Image.fromarray(arr, mode="L").convert("RGB")
buf = BytesIO()
img.save(buf, format="PNG")
buf.seek(0)
client = Minio("minio:9000", access_key="$MinioAccessKey", secret_key="$MinioSecretKey", secure=False)
client.put_object("caries-image", "$ObjectKey", buf, length=len(buf.getvalue()), content_type="image/png")
"@
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($code))
    & docker compose run --rm backend-python python -c "import base64; exec(base64.b64decode('$encoded').decode('utf-8'))" | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "failed to upload test image to MinIO"
    }
}

function New-AnalysisFixture {
    param([string]$ScenarioName, [string]$ImageKind = "default")
    $seed = ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 1000) + (Get-Random -Minimum 10 -Maximum 999)
    $patientId = $seed + 1
    $visitId = $seed + 2
    $caseId = $seed + 3
    $imageAttachmentId = $seed + 4
    $imageId = $seed + 5
    $caseNo = "P5$seed"
    $objectKey = "phase5/e2e/$ScenarioName/$seed/source.png"

    New-TestImageObject $objectKey $ImageKind

    $sql = @"
INSERT INTO pat_patient
(id, patient_no, patient_name_enc, patient_name_hash, patient_name_masked, gender_code, age, source_code, privacy_level_code, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($patientId, 'PAT$seed', 'enc_$seed', 'hash_$seed', 'P*$seed', 'UNKNOWN', 12, 'MANUAL', 'L4', 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_visit
(id, visit_no, patient_id, doctor_user_id, visit_type_code, visit_date, triage_level_code, source_channel_code, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($visitId, 'VIS$seed', $patientId, 100001, 'OUTPATIENT', NOW(), 'NORMAL', 'MANUAL', 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_case
(id, case_no, visit_id, patient_id, case_title, case_type_code, case_status_code, priority_code, report_ready_flag, followup_required_flag, org_id, version_no, status, deleted_flag, created_by, updated_by)
VALUES
($caseId, '$caseNo', $visitId, $patientId, 'Phase5 analysis E2E $ScenarioName', 'CARIES_SCREENING', 'QC_PENDING', 'NORMAL', '0', '0', 100001, 1, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_attachment
(id, biz_module_code, biz_id, file_category_code, file_name, original_name, bucket_name, object_key, content_type, file_ext, file_size_bytes, md5, storage_provider_code, visibility_code, upload_user_id, org_id, status, deleted_flag, remark, created_by, updated_by)
VALUES
($imageAttachmentId, 'CASE', $caseId, 'RAW_IMAGE', 'phase5-source-$seed.png', 'phase5-source-$seed.png', 'caries-image', '$objectKey', 'image/png', 'png', 131072, 'md5-phase5-$seed', 'MINIO', 'PRIVATE', 100001, 100001, 'ACTIVE', 0, 'phase5 e2e source image', 100001, 100001);

INSERT INTO med_image_file
(id, case_id, visit_id, patient_id, attachment_id, image_type_code, image_source_code, shooting_time, image_index_no, quality_status_code, is_primary, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($imageId, $caseId, $visitId, $patientId, $imageAttachmentId, 'PANORAMIC', 'UPLOAD', NOW(), 0, 'PASS', '1', 100001, 'ACTIVE', 0, 100001, 100001);
"@
    Invoke-MysqlQuery $BizDatabase $sql | Out-Null
    return [ordered]@{
        caseId = $caseId
        caseNo = $caseNo
        patientId = $patientId
        imageId = $imageId
        imageAttachmentId = $imageAttachmentId
        objectKey = $objectKey
        imageKind = $ImageKind
    }
}

function Wait-TaskTerminal {
    param([string]$TaskNo)
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = Invoke-MysqlScalar $BizDatabase "SELECT task_status_code FROM ana_task_record WHERE task_no = '$TaskNo' AND deleted_flag = 0 ORDER BY id DESC LIMIT 1;"
        if ($status -in @("SUCCESS", "FAILED")) {
            return $status
        }
        Start-Sleep -Seconds 3
    }
    throw "Timed out waiting for task $TaskNo to reach terminal status"
}

function Save-ScenarioEvidence {
    param([hashtable]$Scenario, [object]$Fixture, [string]$TaskNo)
    $prefix = $Scenario.name
    Save-Text "$prefix-sql-java.txt" (Invoke-MysqlQuery $BizDatabase @"
SELECT id, task_no, task_status_code, model_version, error_code, error_message, trace_id, inference_millis, created_at, started_at, completed_at
FROM ana_task_record
WHERE task_no = '$TaskNo'
ORDER BY id DESC
LIMIT 1;

SELECT task_id, overall_highest_severity, uncertainty_score, review_suggested_flag,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.mode')) AS mode,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.qualityMode')) AS qualityMode,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.qualityImplType')) AS qualityImplType,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.toothDetectionMode')) AS toothDetectionMode,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.toothDetectionImplType')) AS toothDetectionImplType,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.segmentationMode')) AS segmentationMode,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.segmentationImplType')) AS segmentationImplType,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.gradingMode')) AS gradingMode,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.gradingImplType')) AS gradingImplType,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.gradingLabel')) AS gradingLabel,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.uncertaintyMode')) AS uncertaintyMode,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.uncertaintyImplType')) AS uncertaintyImplType,
       JSON_EXTRACT(raw_result_json, '$.uncertaintyScore') AS rawUncertaintyScore,
       JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.needsReview')) AS needsReview
FROM ana_result_summary
WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1)
ORDER BY id DESC
LIMIT 1;

SELECT ava.task_id, ava.asset_type_code, ava.related_image_id, ava.source_attachment_id, ava.attachment_id,
       ma.bucket_name, ma.object_key, ma.content_type, ma.file_size_bytes, ma.md5
FROM ana_visual_asset ava
JOIN med_attachment ma ON ma.id = ava.attachment_id
WHERE ava.task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1)
  AND ava.deleted_flag = 0
ORDER BY ava.sort_order, ava.asset_type_code;
"@)
    Save-Text "$prefix-callback-payload.json.txt" (Invoke-MysqlQuery $BizDatabase "SELECT callback_payload_json FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1;")
    Save-Text "$prefix-python-logs.txt" (& docker compose logs --tail 220 backend-python | Out-String)
    Save-Text "$prefix-java-logs.txt" (& docker compose logs --tail 220 backend-java | Out-String)
    $minioRows = & docker run --rm --network caries-net --entrypoint /bin/sh minio/mc -c "mc alias set caries http://minio:9000 $MinioAccessKey $MinioSecretKey >/dev/null && mc ls --recursive caries/caries-visual"
    $minioMatches = $minioRows | Select-String -SimpleMatch $TaskNo | ForEach-Object { $_.Line }
    Save-Text "$prefix-minio-visual.txt" ($minioMatches -join [Environment]::NewLine)
    if ($Scenario.expectedStatus -eq "SUCCESS" -and ($null -eq $minioMatches -or @($minioMatches).Count -lt 3)) {
        throw "$prefix expected MinIO visual objects for task $TaskNo"
    }
}

function Assert-ScenarioResult {
    param([hashtable]$Scenario, [string]$TaskNo, [string]$Status)
    if ($Scenario.expectedStatus -ne $Status) {
        throw "$($Scenario.name) expected task status $($Scenario.expectedStatus) but got $Status"
    }
    if ($Status -eq "SUCCESS") {
        $mode = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.mode')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $qualityMode = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.qualityMode')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $qualityImpl = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.qualityImplType')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $toothMode = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.toothDetectionMode')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $toothImpl = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.toothDetectionImplType')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $segMode = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.segmentationMode')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $segImpl = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.segmentationImplType')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $gradingMode = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.gradingMode')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $gradingImpl = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.gradingImplType')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $uncertaintyMode = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.uncertaintyMode')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $uncertaintyImpl = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.uncertaintyImplType')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $uncertaintyScore = [double](Invoke-MysqlScalar $BizDatabase "SELECT JSON_EXTRACT(raw_result_json, '$.uncertaintyScore') FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;")
        $needsReview = Invoke-MysqlScalar $BizDatabase "SELECT JSON_UNQUOTE(JSON_EXTRACT(raw_result_json, '$.needsReview')) FROM ana_result_summary WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) ORDER BY id DESC LIMIT 1;"
        $visualCount = [int](Invoke-MysqlScalar $BizDatabase "SELECT COUNT(1) FROM ana_visual_asset WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) AND deleted_flag = 0;")
        $attachmentCount = [int](Invoke-MysqlScalar $BizDatabase "SELECT COUNT(1) FROM med_attachment WHERE biz_module_code = 'ANALYSIS' AND biz_id = (SELECT id FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1) AND deleted_flag = 0;")
        if ($mode -ne $Scenario.expectedMode) { throw "$($Scenario.name) mode expected $($Scenario.expectedMode), got $mode" }
        if ($qualityMode -ne $Scenario.expectedQualityMode) { throw "$($Scenario.name) qualityMode expected $($Scenario.expectedQualityMode), got $qualityMode" }
        if ($qualityImpl -ne $Scenario.expectedQualityImpl) { throw "$($Scenario.name) qualityImplType expected $($Scenario.expectedQualityImpl), got $qualityImpl" }
        if ($toothMode -ne $Scenario.expectedToothMode) { throw "$($Scenario.name) toothDetectionMode expected $($Scenario.expectedToothMode), got $toothMode" }
        if ($toothImpl -ne $Scenario.expectedToothImpl) { throw "$($Scenario.name) toothDetectionImplType expected $($Scenario.expectedToothImpl), got $toothImpl" }
        if ($segMode -ne $Scenario.expectedSegMode) { throw "$($Scenario.name) segmentationMode expected $($Scenario.expectedSegMode), got $segMode" }
        if ($segImpl -ne $Scenario.expectedSegImpl) { throw "$($Scenario.name) segmentationImplType expected $($Scenario.expectedSegImpl), got $segImpl" }
        if ($Scenario.ContainsKey("expectedGradingMode") -and $gradingMode -ne $Scenario.expectedGradingMode) { throw "$($Scenario.name) gradingMode expected $($Scenario.expectedGradingMode), got $gradingMode" }
        if ($Scenario.ContainsKey("expectedGradingImpl") -and $gradingImpl -ne $Scenario.expectedGradingImpl) { throw "$($Scenario.name) gradingImplType expected $($Scenario.expectedGradingImpl), got $gradingImpl" }
        if ($Scenario.ContainsKey("expectedUncertaintyMode") -and $uncertaintyMode -ne $Scenario.expectedUncertaintyMode) { throw "$($Scenario.name) uncertaintyMode expected $($Scenario.expectedUncertaintyMode), got $uncertaintyMode" }
        if ($Scenario.ContainsKey("expectedUncertaintyImpl") -and $uncertaintyImpl -ne $Scenario.expectedUncertaintyImpl) { throw "$($Scenario.name) uncertaintyImplType expected $($Scenario.expectedUncertaintyImpl), got $uncertaintyImpl" }
        if ($Scenario.ContainsKey("expectedNeedsReview") -and $needsReview.ToLowerInvariant() -ne $Scenario.expectedNeedsReview) { throw "$($Scenario.name) needsReview expected $($Scenario.expectedNeedsReview), got $needsReview" }
        if ($Scenario.ContainsKey("uncertaintyLessThan") -and $uncertaintyScore -ge [double]$Scenario.uncertaintyLessThan) { throw "$($Scenario.name) uncertaintyScore expected < $($Scenario.uncertaintyLessThan), got $uncertaintyScore" }
        if ($Scenario.ContainsKey("uncertaintyAtLeast") -and $uncertaintyScore -lt [double]$Scenario.uncertaintyAtLeast) { throw "$($Scenario.name) uncertaintyScore expected >= $($Scenario.uncertaintyAtLeast), got $uncertaintyScore" }
        if ($visualCount -ne 3) { throw "$($Scenario.name) expected 3 ana_visual_asset rows, got $visualCount" }
        if ($attachmentCount -lt 3) { throw "$($Scenario.name) expected >=3 ANALYSIS attachments, got $attachmentCount" }
    } else {
        $errorCode = Invoke-MysqlScalar $BizDatabase "SELECT error_code FROM ana_task_record WHERE task_no = '$TaskNo' ORDER BY id DESC LIMIT 1;"
        if ($Scenario.expectedErrorCode -and $errorCode -ne $Scenario.expectedErrorCode) {
            throw "$($Scenario.name) expected errorCode $($Scenario.expectedErrorCode), got $errorCode"
        }
    }
}

function Invoke-AnalysisScenario {
    param([hashtable]$Scenario, [string]$Token)
    Write-Host "Running scenario $($Scenario.name)..."
    Set-PhaseRuntime $Scenario
    $imageKind = if ($Scenario.ContainsKey("imageKind")) { $Scenario.imageKind } else { "default" }
    $fixture = New-AnalysisFixture $Scenario.name $imageKind
    Save-Json "$($Scenario.name)-fixture.json" $fixture

    $request = @{
        caseId = $fixture.caseId
        patientId = $fixture.patientId
        forceRetryFlag = $true
        taskTypeCode = "INFERENCE"
        remark = "phase5 docker e2e $($Scenario.name)"
    }
    Save-Json "$($Scenario.name)-trigger-request.json" $request
    $response = Invoke-JsonRequest -Method POST -Uri "$JavaBaseUrl/api/v1/analysis/tasks" -Token $Token -Body $request
    Save-Json "$($Scenario.name)-trigger-response.json" $response
    Assert-ApiSuccess $response "$($Scenario.name) trigger"
    $taskNo = $response.data.taskNo
    if ([string]::IsNullOrWhiteSpace($taskNo)) {
        throw "$($Scenario.name) trigger did not return taskNo"
    }

    $status = Wait-TaskTerminal $taskNo
    Save-ScenarioEvidence $Scenario $fixture $taskNo
    Assert-ScenarioResult $Scenario $taskNo $status
    return [ordered]@{
        name = $Scenario.name
        taskNo = $taskNo
        taskStatus = $status
        caseId = $fixture.caseId
        caseNo = $fixture.caseNo
        patientId = $fixture.patientId
        objectKey = $fixture.objectKey
    }
}

$script:EvidenceRoot = New-EvidenceDir $EvidenceDir
Write-Host "Evidence directory: $script:EvidenceRoot"

if (-not $SkipComposeUp) {
    & docker compose up -d --build | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed"
    }
}

Wait-Url "$JavaBaseUrl/actuator/health"
Wait-Url "$PythonBaseUrl/ai/v1/health"
Save-Json "python-health-initial.json" (Invoke-JsonRequest -Method GET -Uri "$PythonBaseUrl/ai/v1/health")

$loginResponse = Invoke-JsonRequest -Method POST -Uri "$JavaBaseUrl/api/v1/auth/login" -Body @{
    username = "admin"
    password = "123456"
}
Assert-ApiSuccess $loginResponse "java login"
$token = $loginResponse.data.token
Save-Json "java-login-response.json" $loginResponse

$scenarios = @(
    @{
        name = "5a-a-mock"
        runtimeMode = "mock"; qualityEnabled = "false"; toothEnabled = "false"; segmentationEnabled = "false"; segmentationForceFail = "false"
        expectedStatus = "SUCCESS"; expectedMode = "mock"
        expectedQualityMode = "mock"; expectedQualityImpl = "MOCK"
        expectedToothMode = "mock"; expectedToothImpl = "MOCK"
        expectedSegMode = "mock"; expectedSegImpl = "MOCK"
    },
    @{
        name = "5a-b-hybrid-quality"
        runtimeMode = "hybrid"; qualityEnabled = "true"; toothEnabled = "false"; segmentationEnabled = "false"; segmentationForceFail = "false"
        expectedStatus = "SUCCESS"; expectedMode = "hybrid"
        expectedQualityMode = "real"; expectedQualityImpl = "HEURISTIC"
        expectedToothMode = "mock"; expectedToothImpl = "MOCK"
        expectedSegMode = "mock"; expectedSegImpl = "MOCK"
    },
    @{
        name = "5a-c-hybrid-quality-tooth"
        runtimeMode = "hybrid"; qualityEnabled = "true"; toothEnabled = "true"; segmentationEnabled = "false"; segmentationForceFail = "false"
        expectedStatus = "SUCCESS"; expectedMode = "hybrid"
        expectedQualityMode = "real"; expectedQualityImpl = "HEURISTIC"
        expectedToothMode = "real"; expectedToothImpl = "HEURISTIC"
        expectedSegMode = "mock"; expectedSegImpl = "MOCK"
    },
    @{
        name = "5b-a-mock-segmentation"
        runtimeMode = "mock"; qualityEnabled = "false"; toothEnabled = "false"; segmentationEnabled = "false"; segmentationForceFail = "false"
        expectedStatus = "SUCCESS"; expectedMode = "mock"
        expectedQualityMode = "mock"; expectedQualityImpl = "MOCK"
        expectedToothMode = "mock"; expectedToothImpl = "MOCK"
        expectedSegMode = "mock"; expectedSegImpl = "MOCK"
    },
    @{
        name = "5b-b-hybrid-segmentation"
        runtimeMode = "hybrid"; qualityEnabled = "true"; toothEnabled = "true"; segmentationEnabled = "true"; segmentationForceFail = "false"
        expectedStatus = "SUCCESS"; expectedMode = "hybrid"
        expectedQualityMode = "real"; expectedQualityImpl = "HEURISTIC"
        expectedToothMode = "real"; expectedToothImpl = "HEURISTIC"
        expectedSegMode = "real"; expectedSegImpl = "HEURISTIC"
    },
    @{
        name = "5b-c-real-segmentation-failure"
        runtimeMode = "real"; qualityEnabled = "true"; toothEnabled = "true"; segmentationEnabled = "true"; segmentationForceFail = "true"
        expectedStatus = "FAILED"; expectedErrorCode = "M5006"
    },
    @{
        name = "5c-a-mock-grading"
        runtimeMode = "mock"; qualityEnabled = "false"; toothEnabled = "false"; segmentationEnabled = "false"; gradingEnabled = "false"; segmentationForceFail = "false"; gradingForceFail = "false"; uncertaintyThreshold = "0.35"; imageKind = "default"
        expectedStatus = "SUCCESS"; expectedMode = "mock"
        expectedQualityMode = "mock"; expectedQualityImpl = "MOCK"
        expectedToothMode = "mock"; expectedToothImpl = "MOCK"
        expectedSegMode = "mock"; expectedSegImpl = "MOCK"
        expectedGradingMode = "mock"; expectedGradingImpl = "MOCK"
        expectedUncertaintyMode = "mock"; expectedUncertaintyImpl = "MOCK"
        expectedNeedsReview = "false"; uncertaintyLessThan = "0.35"
    },
    @{
        name = "5c-b-hybrid-grading-low-uncertainty"
        runtimeMode = "hybrid"; qualityEnabled = "true"; toothEnabled = "true"; segmentationEnabled = "true"; gradingEnabled = "true"; segmentationForceFail = "false"; gradingForceFail = "false"; uncertaintyThreshold = "0.35"; imageKind = "phase5c-low-uncertainty"
        expectedStatus = "SUCCESS"; expectedMode = "hybrid"
        expectedQualityMode = "real"; expectedQualityImpl = "HEURISTIC"
        expectedToothMode = "real"; expectedToothImpl = "HEURISTIC"
        expectedSegMode = "real"; expectedSegImpl = "HEURISTIC"
        expectedGradingMode = "real"; expectedGradingImpl = "HEURISTIC"
        expectedUncertaintyMode = "real"; expectedUncertaintyImpl = "COMPOSITE_HEURISTIC"
        expectedNeedsReview = "false"; uncertaintyLessThan = "0.35"
    },
    @{
        name = "5c-c-hybrid-grading-high-uncertainty"
        runtimeMode = "hybrid"; qualityEnabled = "true"; toothEnabled = "true"; segmentationEnabled = "true"; gradingEnabled = "true"; segmentationForceFail = "false"; gradingForceFail = "false"; uncertaintyThreshold = "0.35"; imageKind = "phase5c-high-uncertainty"
        expectedStatus = "SUCCESS"; expectedMode = "hybrid"
        expectedQualityMode = "real"; expectedQualityImpl = "HEURISTIC"
        expectedToothMode = "real"; expectedToothImpl = "HEURISTIC"
        expectedSegMode = "real"; expectedSegImpl = "HEURISTIC"
        expectedGradingMode = "real"; expectedGradingImpl = "HEURISTIC"
        expectedUncertaintyMode = "real"; expectedUncertaintyImpl = "COMPOSITE_HEURISTIC"
        expectedNeedsReview = "true"; uncertaintyAtLeast = "0.35"
    },
    @{
        name = "5c-d-real-grading-failure"
        runtimeMode = "real"; qualityEnabled = "true"; toothEnabled = "true"; segmentationEnabled = "true"; gradingEnabled = "true"; segmentationForceFail = "false"; gradingForceFail = "true"; uncertaintyThreshold = "0.35"; imageKind = "phase5c-low-uncertainty"
        expectedStatus = "FAILED"; expectedErrorCode = "M5008"
    }
)

if ($Phase5COnly) {
    $scenarios = @($scenarios | Where-Object { $_.name -like "5c-*" })
}

$results = @()
foreach ($scenario in $scenarios) {
    $results += Invoke-AnalysisScenario $scenario $token
}

Save-Text "docker-ps-final.txt" (& docker compose ps | Out-String)
Save-Json "phase5-analysis-e2e-summary.json" ([ordered]@{
    evidenceDir = $script:EvidenceRoot
    scenarioCount = $results.Count
    results = $results
})

Write-Host "Phase 5 analysis Docker E2E passed."
Write-Host ($results | ConvertTo-Json -Depth 20)
