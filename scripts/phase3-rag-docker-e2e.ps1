param(
    [string]$JavaBaseUrl = "http://127.0.0.1:8080",
    [string]$PythonBaseUrl = "http://127.0.0.1:8001",
    [string]$MysqlContainer = "caries-mysql",
    [string]$MysqlPassword = $(if ($env:CARIES_MYSQL_PASSWORD) { $env:CARIES_MYSQL_PASSWORD } else { "1234" }),
    [string]$BizDatabase = $(if ($env:CARIES_MYSQL_DATABASE_BIZ) { $env:CARIES_MYSQL_DATABASE_BIZ } else { "caries_biz" }),
    [string]$AiDatabase = $(if ($env:CARIES_MYSQL_DATABASE_AI) { $env:CARIES_MYSQL_DATABASE_AI } else { "caries_ai" }),
    [string]$EvidenceDir = "",
    [switch]$SkipComposeUp
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-EvidenceDir {
    param([string]$RequestedDir)
    if ([string]::IsNullOrWhiteSpace($RequestedDir)) {
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $RequestedDir = Join-Path "e2e-artifacts" "phase3-rag-$stamp"
    }
    New-Item -ItemType Directory -Force -Path $RequestedDir | Out-Null
    return (Resolve-Path $RequestedDir).Path
}

function Save-Json {
    param([string]$Name, [object]$Value)
    $path = Join-Path $script:EvidenceRoot $Name
    $Value | ConvertTo-Json -Depth 30 | Out-File -FilePath $path -Encoding UTF8
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
    $json = $Body | ConvertTo-Json -Depth 30
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

function Require-CountAtLeast {
    param([string]$Name, [string]$Value, [int]$Expected)
    $number = [int]$Value
    if ($number -lt $Expected) {
        throw "$Name expected >= $Expected but was $number"
    }
}

$script:EvidenceRoot = New-EvidenceDir $EvidenceDir
Write-Host "Evidence directory: $script:EvidenceRoot"

if (-not $SkipComposeUp) {
    Write-Host "Starting Docker services..."
    & docker compose up -d --build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed"
    }
}

Write-Host "Waiting for Java and Python health endpoints..."
Wait-Url "$JavaBaseUrl/actuator/health"
Wait-Url "$PythonBaseUrl/ai/v1/health"
Save-Json "python-health.json" (Invoke-JsonRequest -Method GET -Uri "$PythonBaseUrl/ai/v1/health")

$requestBaseline = Invoke-MysqlScalar $AiDatabase "SELECT COALESCE(MAX(id), 0) FROM rag_request_log;"
$retrievalBaseline = Invoke-MysqlScalar $AiDatabase "SELECT COALESCE(MAX(id), 0) FROM rag_retrieval_log;"
$llmBaseline = Invoke-MysqlScalar $AiDatabase "SELECT COALESCE(MAX(id), 0) FROM llm_call_log;"
Save-Text "rag-log-baseline.txt" "request=$requestBaseline`nretrieval=$retrievalBaseline`nllm=$llmBaseline"

Write-Host "Importing approved knowledge documents..."
$documents = @(
    @{
        kbCode = "caries-default"
        kbName = "CariesGuard Phase3 E2E Knowledge"
        kbTypeCode = "PATIENT_GUIDE"
        docTitle = "Phase3 E2E - Patient care advice"
        docSourceCode = "E2E"
        sourceUri = "e2e://phase3/patient-care"
        docVersion = "v1.0"
        reviewStatusCode = "APPROVED"
        orgId = 100001
        contentText = "For caries prevention, patients should brush twice daily with fluoride toothpaste, reduce frequent sugar intake, and return for follow-up based on risk level. Pain, swelling, or tooth structure loss requires prompt dental review."
    },
    @{
        kbCode = "caries-default"
        kbName = "CariesGuard Phase3 E2E Knowledge"
        kbTypeCode = "DOCTOR_GUIDE"
        docTitle = "Phase3 E2E - Doctor uncertainty workflow"
        docSourceCode = "E2E"
        sourceUri = "e2e://phase3/doctor-uncertainty"
        docVersion = "v1.0"
        reviewStatusCode = "APPROVED"
        orgId = 100001
        contentText = "High uncertainty cases should be reviewed by a dentist using original images, tooth-level findings, visual overlays, and patient history. AI output is an assistant signal and must not replace clinical judgment."
    },
    @{
        kbCode = "caries-default"
        kbName = "CariesGuard Phase3 E2E Knowledge"
        kbTypeCode = "RISK_GUIDE"
        docTitle = "Phase3 E2E - Caries risk explanation"
        docSourceCode = "E2E"
        sourceUri = "e2e://phase3/risk-explanation"
        docVersion = "v1.0"
        reviewStatusCode = "APPROVED"
        orgId = 100001
        contentText = "Medium or high caries risk means the patient may need shorter recall intervals, diet counseling, fluoride use, and confirmation of suspicious lesions. Follow-up is recommended to prevent progression."
    }
)

$importResponses = @()
foreach ($document in $documents) {
    $response = Invoke-JsonRequest -Method POST -Uri "$PythonBaseUrl/ai/v1/knowledge/documents" -Body $document
    Assert-ApiSuccess $response "knowledge import"
    $importResponses += $response
}
Save-Json "knowledge-import-responses.json" $importResponses

Write-Host "Rebuilding knowledge index..."
$rebuildResponse = Invoke-JsonRequest -Method POST -Uri "$PythonBaseUrl/ai/v1/knowledge/rebuild" -Body @{
    kbCode = "caries-default"
    kbName = "CariesGuard Phase3 E2E Knowledge"
    kbTypeCode = "PATIENT_GUIDE"
    knowledgeVersion = "v1.0"
    orgId = 100001
}
Assert-ApiSuccess $rebuildResponse "knowledge rebuild"
Save-Json "knowledge-rebuild-response.json" $rebuildResponse
if ([int]$rebuildResponse.data.chunkCount -lt 1) {
    throw "knowledge rebuild produced no chunks"
}

$vectorPath = $null
$rebuildData = $rebuildResponse.data
$hasVectorStorePath = $rebuildData.PSObject.Properties.Name -contains "vectorStorePath"
if ($hasVectorStorePath) {
    $candidatePath = [string]$rebuildData.vectorStorePath
    if (-not [string]::IsNullOrWhiteSpace($candidatePath)) {
        $vectorPath = $candidatePath
        & docker exec caries-backend-python test -f $vectorPath
        if ($LASTEXITCODE -ne 0) {
            throw "Vector index file was not found in backend-python: $vectorPath"
        }
    }
}

if ([string]::IsNullOrWhiteSpace($vectorPath)) {
    Write-Host "knowledge rebuild response has no vectorStorePath; skipping local file assertion."
    $hasDocuments = $rebuildData.PSObject.Properties.Name -contains "documents"
    if ((-not $hasDocuments) -or $null -eq $rebuildData.documents -or [int]$rebuildData.documents.Count -lt 1) {
        throw "knowledge rebuild response missing documents metadata"
    }
}

Save-Text "knowledge-sql.txt" (Invoke-MysqlQuery $AiDatabase @"
SELECT id, doc_no, doc_title, review_status_code, created_at
FROM kb_document
WHERE doc_title LIKE 'Phase3 E2E%'
ORDER BY id DESC
LIMIT 20;

SELECT id, rebuild_job_no, rebuild_status_code, chunk_count, vector_store_path, started_at, finished_at
FROM kb_rebuild_job
ORDER BY id DESC
LIMIT 10;

SELECT id, kb_id, doc_id, chunk_no, LEFT(chunk_text, 120) AS chunk_preview
FROM kb_document_chunk
ORDER BY id DESC
LIMIT 20;
"@)

Write-Host "Logging in to Java..."
$loginResponse = Invoke-JsonRequest -Method POST -Uri "$JavaBaseUrl/api/v1/auth/login" -Body @{
    username = "admin"
    password = "123456"
}
Assert-ApiSuccess $loginResponse "java login"
$token = $loginResponse.data.token
Save-Json "java-login-response.json" $loginResponse

Write-Host "Calling Java doctor QA endpoint..."
$doctorQaResponse = Invoke-JsonRequest -Method POST -Uri "$JavaBaseUrl/api/v1/rag/doctor-qa" -Token $token -Body @{
    question = "How should high uncertainty caries cases be handled?"
    kbCode = "caries-default"
    topK = 3
    relatedBizNo = "PHASE3-E2E-DOCTOR-QA"
    clinicalContext = @{
        riskLevelCode = "HIGH"
        uncertaintyScore = 0.72
        toothCode = "16"
    }
}
Assert-ApiSuccess $doctorQaResponse "java doctor qa"
Save-Json "java-doctor-qa-response.json" $doctorQaResponse
if ([string]::IsNullOrWhiteSpace($doctorQaResponse.data.answerText)) {
    throw "Doctor QA returned empty answerText"
}
if ($doctorQaResponse.data.fallback -eq $true) {
    throw "Doctor QA returned fallback=true"
}
if ($doctorQaResponse.data.citations.Count -lt 1) {
    throw "Doctor QA returned no citations"
}

Write-Host "Seeding a minimal analyzed case in caries_biz..."
$seed = ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 1000) + (Get-Random -Minimum 10 -Maximum 999)
$patientId = $seed + 1
$visitId = $seed + 2
$caseId = $seed + 3
$imageAttachmentId = $seed + 4
$imageId = $seed + 5
$visualAttachmentId = $seed + 6
$visualAssetId = $seed + 7
$taskId = $seed + 8
$summaryId = $seed + 9
$riskId = $seed + 10
$toothRecordId = $seed + 11
$statusLogId = $seed + 12
$tag = "phase3e2e$seed"
$caseNo = "P3E2E$seed"
$taskNo = "TASK-P3E2E-$seed"

$seedSql = @"
INSERT INTO pat_patient
(id, patient_no, patient_name_enc, patient_name_hash, patient_name_masked, gender_code, age, source_code, privacy_level_code, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($patientId, 'PAT$seed', 'enc_$tag', 'hash_$tag', 'P*$seed', 'UNKNOWN', 12, 'MANUAL', 'L4', 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_visit
(id, visit_no, patient_id, doctor_user_id, visit_type_code, visit_date, triage_level_code, source_channel_code, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($visitId, 'VIS$seed', $patientId, 100001, 'OUTPATIENT', NOW(), 'NORMAL', 'MANUAL', 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_case
(id, case_no, visit_id, patient_id, case_title, case_type_code, case_status_code, priority_code, report_ready_flag, followup_required_flag, org_id, version_no, status, deleted_flag, created_by, updated_by)
VALUES
($caseId, '$caseNo', $visitId, $patientId, 'Phase3 RAG E2E case', 'CARIES_SCREENING', 'REVIEW_PENDING', 'NORMAL', '0', '0', 100001, 1, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_case_status_log
(id, case_id, from_status_code, to_status_code, changed_by, change_reason_code, change_reason, changed_at, org_id)
VALUES
($statusLogId, $caseId, 'ANALYZING', 'REVIEW_PENDING', 100001, 'PHASE3_E2E_SEED', 'seeded analyzed case for phase3 rag e2e', NOW(), 100001);

INSERT INTO med_attachment
(id, biz_module_code, biz_id, file_category_code, asset_type_code, source_attachment_id, file_name, original_name, bucket_name, object_key, content_type, file_ext, file_size_bytes, md5, storage_provider_code, visibility_code, retention_policy_code, expired_at, integrity_status_code, metadata_json, upload_user_id, org_id, status, deleted_flag, remark, created_by, updated_by)
VALUES
($imageAttachmentId, 'CASE', $caseId, 'RAW_IMAGE', 'PANORAMIC', NULL, 'phase3-e2e-image.jpg', 'phase3-e2e-image.jpg', 'caries-image', 'phase3/e2e/$tag/image.jpg', 'image/jpeg', 'jpg', 1024, 'md5-$tag-image', 'MINIO', 'PRIVATE', 'LONG_TERM', NULL, 'NORMAL', JSON_OBJECT('phase', 'phase3-e2e'), 100001, 100001, 'ACTIVE', 0, 'phase3 e2e source image', 100001, 100001),
($visualAttachmentId, 'ANALYSIS', $caseId, 'VISUAL', 'HEATMAP', $imageAttachmentId, 'phase3-e2e-heatmap.png', 'phase3-e2e-heatmap.png', 'caries-visual', 'phase3/e2e/$tag/heatmap.png', 'image/png', 'png', 2048, 'md5-$tag-visual', 'MINIO', 'PRIVATE', 'TEMP_30D', DATE_ADD(NOW(), INTERVAL 30 DAY), 'NORMAL', JSON_OBJECT('phase', 'phase3-e2e'), 100001, 100001, 'ACTIVE', 0, 'phase3 e2e visual asset', 100001, 100001);

INSERT INTO med_image_file
(id, case_id, visit_id, patient_id, attachment_id, image_type_code, image_source_code, shooting_time, quality_status_code, source_device_code, capture_batch_no, is_primary, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($imageId, $caseId, $visitId, $patientId, $imageAttachmentId, 'PANORAMIC', 'UPLOAD', NOW(), 'PASS', 'E2E_DEVICE', 'P3E2E', '1', 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_case_tooth_record
(id, case_id, source_image_id, tooth_code, tooth_surface_code, issue_type_code, severity_code, finding_desc, suggestion, sort_order, reviewed_by, reviewed_at, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($toothRecordId, $caseId, $imageId, '16', 'OCCLUSAL', 'CARIES', 'C2', 'Suspicious occlusal lesion with moderate risk.', 'Review image and reinforce fluoride care.', 0, 100001, NOW(), 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO ana_task_record
(id, task_no, case_id, patient_id, request_batch_no, model_version, task_type_code, task_status_code, request_payload_json, callback_payload_json, started_at, completed_at, error_message, trace_id, inference_millis, retry_from_task_id, error_code, org_id, status, deleted_flag, created_by)
VALUES
($taskId, '$taskNo', $caseId, $patientId, 'P3E2E', 'caries-v1', 'INFERENCE', 'SUCCESS', JSON_OBJECT('phase', 'phase3-e2e'), JSON_OBJECT('taskStatusCode', 'SUCCESS'), DATE_SUB(NOW(), INTERVAL 1 MINUTE), NOW(), NULL, 'phase3-e2e', 1200, NULL, NULL, 100001, 'ACTIVE', 0, 100001);

INSERT INTO ana_result_summary
(id, task_id, case_id, raw_result_json, overall_highest_severity, uncertainty_score, review_suggested_flag, lesion_count, abnormal_tooth_count, summary_version_no, org_id, status, deleted_flag, created_by)
VALUES
($summaryId, $taskId, $caseId, JSON_OBJECT('overallHighestSeverity', 'C2', 'uncertaintyScore', 0.24, 'reviewSuggestedFlag', '1'), 'C2', 0.2400, '1', 2, 1, 1, 100001, 'ACTIVE', 0, 100001);

INSERT INTO ana_visual_asset
(id, task_id, case_id, model_version, asset_type_code, source_attachment_id, attachment_id, related_image_id, tooth_code, sort_order, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($visualAssetId, $taskId, $caseId, 'caries-v1', 'HEATMAP', $imageAttachmentId, $visualAttachmentId, $imageId, '16', 0, 100001, 'ACTIVE', 0, 100001, 100001);

INSERT INTO med_risk_assessment_record
(id, case_id, patient_id, task_id, overall_risk_level_code, risk_score, assessment_report_json, recommended_cycle_days, version_no, assessed_at, org_id, status, deleted_flag, created_by, updated_by)
VALUES
($riskId, $caseId, $patientId, $taskId, 'HIGH', 0.8200, JSON_OBJECT('phase', 'phase3-e2e', 'riskFactor', 'frequent sugar intake'), 30, 1, NOW(), 100001, 'ACTIVE', 0, 100001, 100001);
"@
Invoke-MysqlQuery $BizDatabase $seedSql | Out-Null
Save-Text "seeded-case.txt" "caseId=$caseId`ncaseNo=$caseNo`npatientId=$patientId`ntaskId=$taskId`nsummaryId=$summaryId`nriskId=$riskId"

Write-Host "Generating patient report through Java..."
$patientReportResponse = Invoke-JsonRequest -Method POST -Uri "$JavaBaseUrl/api/v1/cases/$caseId/reports" -Token $token -Body @{
    reportTypeCode = "PATIENT"
    remark = "phase3 docker e2e patient report"
}
Assert-ApiSuccess $patientReportResponse "patient report generation"
Save-Json "java-patient-report-response.json" $patientReportResponse
$reportId = $patientReportResponse.data.reportId

$reportDetail = Invoke-JsonRequest -Method GET -Uri "$JavaBaseUrl/api/v1/reports/$reportId" -Token $token
Assert-ApiSuccess $reportDetail "patient report detail"
Save-Json "java-patient-report-detail.json" $reportDetail
$summaryText = [string]$reportDetail.data.summaryText
if ($summaryText -notlike "*patientExplanation=*") {
    throw "Report summaryText does not include patientExplanation evidence"
}
if ($summaryText -like "*temporarily unavailable*") {
    throw "Patient report used Java fallback explanation"
}

Write-Host "Collecting RAG SQL evidence..."
$requestSql = @"
SELECT id, request_no, request_type_code, request_status_code, top_k, latency_ms, safety_flag, created_at
FROM rag_request_log
WHERE id > $requestBaseline
ORDER BY id;
"@
$retrievalSql = @"
SELECT id, request_id, chunk_id, rank_no, retrieval_score, doc_id, cited_flag, created_at
FROM rag_retrieval_log
WHERE id > $retrievalBaseline
ORDER BY id;
"@
$llmSql = @"
SELECT id, request_id, model_name, provider_code, prompt_tokens, completion_tokens, total_tokens, latency_ms, call_status_code, created_at
FROM llm_call_log
WHERE id > $llmBaseline
ORDER BY id;
"@
Save-Text "rag-request-log.sql.txt" (Invoke-MysqlQuery $AiDatabase $requestSql)
Save-Text "rag-retrieval-log.sql.txt" (Invoke-MysqlQuery $AiDatabase $retrievalSql)
Save-Text "llm-call-log.sql.txt" (Invoke-MysqlQuery $AiDatabase $llmSql)

$doctorRequestCount = Invoke-MysqlScalar $AiDatabase "SELECT COUNT(1) FROM rag_request_log WHERE id > $requestBaseline AND request_type_code = 'DOCTOR_QA';"
$patientRequestCount = Invoke-MysqlScalar $AiDatabase "SELECT COUNT(1) FROM rag_request_log WHERE id > $requestBaseline AND request_type_code = 'PATIENT_EXPLAIN';"
$retrievalCount = Invoke-MysqlScalar $AiDatabase "SELECT COUNT(1) FROM rag_retrieval_log WHERE id > $retrievalBaseline AND request_id IN (SELECT id FROM rag_request_log WHERE id > $requestBaseline);"
$llmCount = Invoke-MysqlScalar $AiDatabase "SELECT COUNT(1) FROM llm_call_log WHERE id > $llmBaseline AND request_id IN (SELECT id FROM rag_request_log WHERE id > $requestBaseline);"
Require-CountAtLeast "doctor RAG request count" $doctorRequestCount 1
Require-CountAtLeast "patient RAG request count" $patientRequestCount 1
Require-CountAtLeast "RAG retrieval count" $retrievalCount 2
Require-CountAtLeast "LLM call count" $llmCount 2

$boundaryMatches = Get-ChildItem -Path backend-java -Recurse -File |
    Where-Object { $_.Extension -eq ".java" } |
    Select-String -Pattern "caries_ai|rag_request_log|rag_retrieval_log|llm_call_log" |
    ForEach-Object { "$($_.Path):$($_.LineNumber):$($_.Line.Trim())" }
$boundaryText = $boundaryMatches | Out-String
Save-Text "java-rag-boundary-check.txt" $boundaryText
if (-not [string]::IsNullOrWhiteSpace($boundaryText)) {
    throw "Java source contains direct AI/RAG table references. See java-rag-boundary-check.txt"
}

& docker compose logs --tail 200 backend-java backend-python | Out-File -FilePath (Join-Path $script:EvidenceRoot "docker-logs-tail.txt") -Encoding UTF8

$summary = [ordered]@{
    evidenceDir = $script:EvidenceRoot
    caseId = $caseId
    reportId = $reportId
    doctorRequestCount = [int]$doctorRequestCount
    patientRequestCount = [int]$patientRequestCount
    retrievalCount = [int]$retrievalCount
    llmCount = [int]$llmCount
    vectorStorePath = $vectorPath
}
Save-Json "phase3-rag-e2e-summary.json" $summary

Write-Host "Phase 3 RAG Docker E2E passed."
Write-Host ($summary | ConvertTo-Json -Depth 10)
