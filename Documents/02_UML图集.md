# UML图集

更新日期：2026-04-15

本文档用 UML / Mermaid 图描述当前代码结构。图中不会把未落地的 `Risk`、`ModelAdmin` 画成独立模块；它们只作为能力或治理边界出现。

## 1. 系统组件图

```mermaid
flowchart TB
  Client[前端/调用方] --> Auth[caries-system]
  Auth --> Patient[caries-patient]
  Patient --> Image[caries-image]
  Image --> Storage{ObjectStorageService}
  Storage --> MinIO[MinIO provider\nMinioObjectStorageClient]
  Storage --> LocalFS[LOCAL_FS provider\nImageObjectStorageServiceAdapter]
  Image --> Analysis[caries-analysis]
  Analysis --> MQ[RabbitMQ]
  MQ --> Python[Python AI Service\n仓库外]
  Python --> Callback[AnalysisCallbackController]
  Callback --> Result[(analysis result tables)]
  Result --> Report[caries-report]
  Result --> Followup[caries-followup]
  Result --> Dashboard[caries-dashboard]
  Auth --> Dashboard
```

## 2. 模块依赖图

```mermaid
flowchart LR
  boot[caries-boot] --> common[caries-common]
  boot --> framework[caries-framework]
  boot --> system[caries-system]
  boot --> patient[caries-patient]
  boot --> image[caries-image]
  boot --> analysis[caries-analysis]
  boot --> report[caries-report]
  boot --> followup[caries-followup]
  boot --> dashboard[caries-dashboard]
  analysis --> image
  report --> image
  dashboard --> analysis
  dashboard --> followup
  dashboard --> patient
```

## 3. 对象存储类图

```mermaid
classDiagram
  class ObjectStorageService {
    <<interface>>
    +store(originalFileName, contentType, inputStream, fileSizeBytes, md5) StoredObject
    +load(bucketName, objectKey, originalFileName, contentType) StoredObjectResource
    +delete(bucketName, objectKey) void
  }
  class MinioObjectStorageClient {
    -StorageProperties properties
    -MinioClient minioClient
    +store(...)
    +load(...)
    +delete(...)
  }
  class ImageObjectStorageServiceAdapter {
    -StorageProperties properties
    +store(...)
    +load(...)
    +delete(...)
  }
  class AttachmentAppService {
    +upload(file) AttachmentUploadVO
    +createAccessUrl(attachmentId, request) AttachmentAccessVO
    +createAccessUrl(attachmentId, baseUrl) AttachmentAccessVO
    +createInternalAccessUrl(attachmentId) AttachmentAccessVO
    +resolveLocalStoragePath(bucketName, objectKey) String
    +loadContent(attachmentId, expireAt, signature) StoredObjectResource
  }
  ObjectStorageService <|.. MinioObjectStorageClient
  ObjectStorageService <|.. ImageObjectStorageServiceAdapter
  AttachmentAppService --> ObjectStorageService
```

## 4. AI 分析任务时序图

```mermaid
sequenceDiagram
  participant Doctor as 医生/筛查员
  participant Case as CaseController
  participant Analysis as AnalysisTaskAppService
  participant Image as AttachmentAppService
  participant MQ as RabbitAnalysisTaskEventPublisher
  participant Python as Python AI Service
  participant Callback as AnalysisCallbackController
  participant Domain as AnalysisCallbackDomainService
  participant DB as MySQL

  Doctor->>Case: POST /api/v1/cases/{caseId}/analysis
  Case->>Analysis: createTask(caseId, command)
  Analysis->>Image: createInternalAccessUrl(attachmentId)
  Image-->>Analysis: accessUrl + expireAt
  Analysis->>DB: insert ana_task_record
  Analysis->>MQ: publish AiAnalysisRequestDTO
  MQ-->>Python: taskNo + images[].accessUrl
  Python->>Python: 拉图、推理、生成结果
  Python->>Callback: POST /api/v1/internal/ai/callbacks/analysis-result
  Callback->>Domain: applyCallback(AiAnalysisResultCallbackCommand)
  Domain->>DB: update ana_task_record
  Domain->>DB: insert summary / visualAssets / riskAssessment
  Domain->>DB: update med_case status
```

## 5. AI 请求载荷结构图

```mermaid
classDiagram
  class AiAnalysisRequestDTO {
    +String taskNo
    +String taskTypeCode
    +Long caseId
    +Long patientId
    +Long orgId
    +String modelVersion
    +List~ImageItem~ images
    +PatientProfile patientProfile
  }
  class ImageItem {
    +Long imageId
    +Long attachmentId
    +String imageTypeCode
    +String bucketName
    +String objectKey
    +String storageProviderCode
    +String attachmentMd5
    +String accessUrl
    +Long accessExpireAt
    +String localStoragePath
  }
  class PatientProfile {
    +Integer age
    +String genderCode
  }
  AiAnalysisRequestDTO --> ImageItem
  AiAnalysisRequestDTO --> PatientProfile
```

## 6. AI 回调载荷结构图

```mermaid
classDiagram
  class AiAnalysisResultCallbackCommand {
    +String taskNo
    +String taskStatusCode
    +LocalDateTime startedAt
    +LocalDateTime completedAt
    +String modelVersion
    +Summary summary
    +JsonNode rawResultJson
    +List~AiVisualAssetDTO~ visualAssets
    +RiskAssessmentDTO riskAssessment
    +String errorMessage
    +String traceId
    +Long inferenceMillis
    +Double uncertaintyScore
  }
  class Summary {
    +String overallHighestSeverity
    +Double uncertaintyScore
    +String reviewSuggestedFlag
    +Integer teethCount
  }
  class AiVisualAssetDTO
  class RiskAssessmentDTO
  AiAnalysisResultCallbackCommand --> Summary
  AiAnalysisResultCallbackCommand --> AiVisualAssetDTO
  AiAnalysisResultCallbackCommand --> RiskAssessmentDTO
```

## 7. 报告导出时序图

```mermaid
sequenceDiagram
  participant User as 用户
  participant Report as ReportController
  participant App as ReportAppService
  participant Export as ReportExportLogRepository
  participant Image as AttachmentAppService
  participant File as FileController
  participant Store as ObjectStorageService

  User->>Report: POST /api/v1/reports/{reportId}/export
  Report->>App: exportReport(reportId, command)
  App->>Export: insert rpt_export_log
  App->>Image: createAccessUrl(attachmentId, baseUrl)
  Image-->>App: downloadUrl + expireAt
  App-->>Report: ReportExportResultVO
  Report-->>User: attachmentId + downloadUrl + expireAt
  User->>File: GET /api/v1/files/{attachmentId}/access-url
  File-->>User: MinIO presigned URL + expireAt
  User->>Store: GET presigned URL
  Store-->>User: PDF resource
```

## 8. 看板模型运行统计类图

```mermaid
classDiagram
  class DashboardOpsController {
    +getModelRuntime() ApiResponse~ModelRuntimeVO~
  }
  class DashboardOpsMetricsAppService {
    +getModelRuntime() ModelRuntimeVO
  }
  class DashboardStatsRepository {
    +queryModelRuntime(orgId) ModelRuntimeVO
  }
  class ModelRuntimeVO {
    +String currentModelVersion
    +long recentTaskCount
    +long successTaskCount
    +long failedTaskCount
    +BigDecimal successRate
    +BigDecimal averageInferenceMillis
    +BigDecimal highUncertaintyRate
    +BigDecimal reviewSuggestedRate
    +long correctionFeedbackCount
    +List~ModelVersionRuntimeVO~ modelVersions
  }
  class ModelVersionRuntimeVO {
    +String modelVersion
    +long taskCount
    +long successTaskCount
    +long failedTaskCount
    +BigDecimal successRate
    +BigDecimal averageInferenceMillis
  }
  DashboardOpsController --> DashboardOpsMetricsAppService
  DashboardOpsMetricsAppService --> DashboardStatsRepository
  DashboardStatsRepository --> ModelRuntimeVO
  ModelRuntimeVO --> ModelVersionRuntimeVO
```

## 9. 数据库领域图

```mermaid
erDiagram
  pat_patient ||--o{ med_visit : has
  med_visit ||--o{ med_case : creates
  med_case ||--o{ med_image_file : has
  med_attachment ||--o{ med_image_file : stores
  med_image_file ||--o{ med_image_quality_check : checked_by
  med_case ||--o{ ana_task_record : analyzed_by
  ana_task_record ||--o{ ana_result_summary : produces
  ana_task_record ||--o{ ana_visual_asset : produces
  ana_task_record ||--o{ med_risk_assessment_record : produces
  med_case ||--o{ ana_correction_feedback : corrected_by
  med_case ||--o{ rpt_record : reports
  rpt_record ||--o{ rpt_export_log : exports
  med_case ||--o{ fup_plan : follows
  fup_plan ||--o{ fup_task : schedules
  fup_task ||--o{ fup_record : records
  ana_model_version_registry ||--o{ ana_task_record : governs
```

## 10. 权限模型图

```mermaid
erDiagram
  sys_user ||--o{ sys_user_role : has
  sys_role ||--o{ sys_user_role : assigned
  sys_role ||--o{ sys_role_menu : grants
  sys_menu ||--o{ sys_role_menu : bound
  sys_role ||--o{ sys_data_permission_rule : owns
```

当前 V014 已初始化：

1. `ORG_ADMIN`
2. `DOCTOR`
3. `SCREENER`
4. 患者、就诊、病例、影像、分析、报告、随访、看板菜单
5. `dashboard:ops:view` AI 运行看板权限
6. 数据权限规则和列脱敏策略种子
