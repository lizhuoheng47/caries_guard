# 多模态龋齿智能识别平台 —— UML 图集

> **文档性质**：软件设计 UML 图集  
> **说明**：本版图集仅保留当前真实落地架构，统一与《项目总体设计文档》的口径，不再以过重的未来分布式方案作为主展示内容。

---

## 目录

1. [设计约束说明](#1-设计约束说明)
2. [用例图](#2-用例图)
3. [组件图](#3-组件图)
4. [核心领域类图](#4-核心领域类图)
5. [时序图](#5-时序图)
6. [状态机图](#6-状态机图)
7. [部署图](#7-部署图)
8. [ER 图](#8-er-图)
9. [活动图](#9-活动图)
10. [图集维护要求](#10-图集维护要求)

---

## 1. 设计约束说明

本图集遵循以下约束：

1. 当前系统架构固定为“**模块化单体 + 独立 AI 服务 + RabbitMQ 解耦**”；
2. 业务运行数据与训练数据分层治理，图中必须体现边界；
3. 候选模型上线需要“**离线评估 + 人工审批**”，不使用“自动切换主模型”的口径；
4. 图中重点体现真实业务闭环，而非未来集群化想象。

---

## 2. 用例图

### 2.1 系统总体用例图

```mermaid
graph TB
    subgraph 多模态龋齿智能识别平台
        UC1[登录与权限控制]
        UC2[创建患者与病例]
        UC3[上传口腔影像]
        UC4[查看AI分析结果]
        UC5[查看风险评估与复核建议]
        UC6[医生修正结果]
        UC7[生成医生版报告]
        UC8[生成患者版报告]
        UC9[管理随访计划]
        UC10[查看统计看板]
        UC11[管理模型版本]
        UC12[查看审计日志]
        UC13[导出数据与报告]
    end

    Doctor((口腔医生))
    Screener((筛查人员))
    Admin((系统管理员))
    Researcher((科研管理员))
    Patient((患者/家属))

    Doctor --- UC1
    Doctor --- UC2
    Doctor --- UC3
    Doctor --- UC4
    Doctor --- UC5
    Doctor --- UC6
    Doctor --- UC7
    Doctor --- UC8
    Doctor --- UC9

    Screener --- UC1
    Screener --- UC2
    Screener --- UC3
    Screener --- UC4

    Admin --- UC1
    Admin --- UC10
    Admin --- UC11
    Admin --- UC12

    Researcher --- UC1
    Researcher --- UC10
    Researcher --- UC11
    Researcher --- UC13

    Patient --- UC8
```

### 2.2 医生端核心闭环用例图

```mermaid
graph LR
    Doctor((口腔医生))

    subgraph 医生闭环
        A[创建病例]
        B[上传影像]
        C[查看AI结果]
        D[查看不确定性与复核提示]
        E[修正牙位/病灶/分级]
        F[生成报告]
        G[创建随访计划]
    end

    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G

    Doctor --> A
    Doctor --> C
    Doctor --> E
    Doctor --> F
    Doctor --> G
```

---

## 3. 组件图

### 3.1 平台总体组件图

```mermaid
graph TB
    subgraph Client[客户端]
        FE1[医生端]
        FE2[筛查端]
        FE3[管理端]
        FE4[患者报告页]
    end

    subgraph Access[接入层]
        Nginx[Nginx]
    end

    subgraph Backend[Spring Boot 模块化单体]
        M1[Auth模块]
        M2[Patient/Case模块]
        M3[Image模块]
        M4[Analysis模块]
        M5[Risk模块]
        M6[Report模块]
        M7[Followup模块]
        M8[Dashboard模块]
        M9[ModelAdmin模块]
    end

    subgraph AI[Python FastAPI AI 服务]
        A1[Quality Check]
        A2[Tooth Detect]
        A3[TPC-Net Segmentation]
        A4[EDL Grading]
        A5[Risk Fusion]
        A6[Explainability]
    end

    subgraph Infra[基础设施]
        DB[(MySQL)]
        Redis[(Redis)]
        MQ[(RabbitMQ)]
        MinIO[(MinIO)]
    end

    FE1 --> Nginx
    FE2 --> Nginx
    FE3 --> Nginx
    FE4 --> Nginx

    Nginx --> M1
    Nginx --> M2
    Nginx --> M3
    Nginx --> M4
    Nginx --> M5
    Nginx --> M6
    Nginx --> M7
    Nginx --> M8
    Nginx --> M9

    M1 --> DB
    M2 --> DB
    M3 --> DB
    M4 --> DB
    M5 --> DB
    M6 --> DB
    M7 --> DB
    M8 --> DB
    M9 --> DB

    M1 --> Redis
    M4 --> MQ
    M4 --> AI
    M5 --> AI
    M3 --> MinIO
    M6 --> MinIO

    AI --> MQ
    AI --> MinIO
```

### 3.2 AI 服务组件图

```mermaid
graph TB
    subgraph AIService[Python AI Service]
        API[FastAPI Router]

        subgraph Endpoints
            E1["/quality-check"]
            E2["/analyze"]
            E3["/assess-risk"]
            E4["/model-version"]
        end

        subgraph Pipelines
            P1[Preprocessing Pipeline]
            P2[Tooth Detection Pipeline]
            P3[TPC-Net Inference Pipeline]
            P4[EDL Grading Pipeline]
            P5[Risk Fusion Pipeline]
            P6[Explainability Pipeline]
        end

        subgraph Models
            MD1[YOLOv8 Tooth Detector]
            MD2[SegFormer Backbone + TPE]
            MD3[EDL Grading Head]
            MD4[Tabular Risk Model]
        end

        subgraph Mechanisms
            M1[Cross-View Consistency Constraint]
        end

        API --> E1
        API --> E2
        API --> E3
        API --> E4

        E1 --> P1
        E2 --> P1
        P1 --> P2
        P2 --> MD1
        P2 --> P3
        P3 --> MD2
        P3 --> M1
        P3 --> P4
        P4 --> MD3
        E3 --> P5
        P5 --> MD4
        P5 --> P6
    end
```

---

## 4. 核心领域类图

### 4.1 业务运行域类图

```mermaid
classDiagram
    class Patient {
        +Long id
        +String patientNo
        +String patientUuid
        +String name
        +Gender gender
        +Integer age
        +Long orgId
        +createCase() CaseRecord
    }

    class RiskFactorProfile {
        +Long id
        +Long patientId
        +Integer brushingFreqPerDay
        +Integer sugarDietScore
        +Boolean fluorideUse
        +Integer lastCheckMonths
        +Boolean orthodonticHistory
        +Integer previousCariesCount
    }

    class CaseRecord {
        +Long id
        +String caseNo
        +CaseStatus status
        +Long patientId
        +addImage() void
        +transitionTo(status) void
    }

    class ImageFile {
        +Long id
        +Long caseId
        +ImageType imageType
        +String storageKey
        +String md5
        +QualityStatus qualityStatus
    }

    class AiTask {
        +Long id
        +Long caseId
        +Long imageId
        +TaskStatus status
        +String modelVersion
        +markRunning() void
        +markSuccess() void
        +markFailed() void
    }

    class AiResult {
        +Long id
        +Long taskId
        +String lesionMaskKey
        +String gradingLabel
        +Double uncertaintyScore
        +needsReview() boolean
    }

    class RiskAssessment {
        +Long id
        +Long caseId
        +String riskLevel
        +Double riskScore
        +String explanationJson
    }

    class ReportRecord {
        +Long id
        +Long caseId
        +String reportType
        +Integer version
        +String storageKey
    }

    class CorrectionFeedback {
        +Long id
        +Long caseId
        +Long aiResultId
        +String originalValueJson
        +String correctedValueJson
        +Boolean approvedForTraining
    }

    class FollowupPlan {
        +Long id
        +Long caseId
        +Date nextDate
        +String priority
        +String status
    }

    Patient --> RiskFactorProfile
    Patient --> CaseRecord
    CaseRecord --> ImageFile
    CaseRecord --> AiTask
    AiTask --> AiResult
    CaseRecord --> RiskAssessment
    CaseRecord --> ReportRecord
    CaseRecord --> CorrectionFeedback
    CaseRecord --> FollowupPlan
```

### 4.2 训练数据治理类图

```mermaid
classDiagram
    class DatasetSnapshot {
        +Long id
        +String datasetVersion
        +String sourceSummary
        +String splitSummary
        +String datasetCardPath
        +Date releasedAt
    }

    class AnnotationRecord {
        +Long id
        +String imageId
        +String patientUuid
        +String annotationVersion
        +String annotatorL1
        +String annotatorL2
        +String qcStatus
    }

    class GoldSetItem {
        +Long id
        +String imageId
        +String difficulty
        +Boolean active
    }

    class ModelVersion {
        +Long id
        +String modelName
        +String version
        +String datasetVersion
        +String metricsJson
        +String status
    }

    class ApprovalRecord {
        +Long id
        +Long modelVersionId
        +String approver
        +String decision
        +String notes
        +Date approvedAt
    }

    DatasetSnapshot --> AnnotationRecord
    DatasetSnapshot --> GoldSetItem
    DatasetSnapshot --> ModelVersion
    ModelVersion --> ApprovalRecord
```

---

## 5. 时序图

### 5.1 影像上传到报告生成时序图

```mermaid
sequenceDiagram
    actor Doctor as 医生
    participant FE as 前端
    participant API as SpringBoot后端
    participant MinIO as MinIO
    participant MQ as RabbitMQ
    participant AI as Python AI服务
    participant DB as MySQL
    participant WS as WebSocket

    Doctor->>FE: 上传影像并提交病例
    FE->>API: POST /cases/{id}/images
    API->>MinIO: 存储原始影像
    API->>DB: 写入 image_file、ai_task
    API->>MQ: 发布 ImageUploadedEvent
    API-->>FE: 返回 imageId 和 taskId

    MQ->>AI: 消费任务
    AI->>AI: 质量检查
    alt 质量不合格
        AI->>DB: 更新任务状态=REJECTED
        AI->>WS: 推送质检失败
    else 质量合格
        AI->>AI: 牙位检测
        AI->>AI: TPC-Net 分割
        AI->>AI: EDL 分级与不确定性计算
        AI->>DB: 回写 ai_result
        AI->>MQ: 发布 AnalysisCompletedEvent
    end

    MQ->>API: 触发风险评估与报告生成
    API->>AI: 请求风险融合与解释信息
    AI-->>API: 返回风险等级与解释
    API->>DB: 保存 risk_assessment、report_record
    API->>WS: 推送分析完成
    WS-->>FE: 前端刷新展示结果
```

### 5.2 医生修正与回流治理时序图

```mermaid
sequenceDiagram
    actor Doctor as 医生
    participant FE as 前端
    participant API as SpringBoot后端
    participant DB as MySQL
    participant Admin as 数据管理员
    participant Train as 训练流程
    participant Approver as 审批人

    Doctor->>FE: 修正分割或分级结果
    FE->>API: POST /cases/{id}/corrections
    API->>DB: 写入 correction_feedback
    API->>DB: 记录审计日志
    API-->>FE: 返回修正后的报告

    Note over Admin,Train: 周期性执行
    Admin->>DB: 导出待审核修正样本
    Admin->>Admin: 脱敏并检查完整性
    Admin->>Train: 生成数据快照并发起训练
    Train->>Train: 离线训练与评估
    Train->>Approver: 提交候选模型与评估结果
    Approver->>Approver: 人工审核
    alt 审批通过
        Approver->>DB: 模型状态=ACTIVE
    else 审批拒绝
        Approver->>DB: 模型状态=ARCHIVED
    end
```

### 5.3 数据边界治理时序图

```mermaid
sequenceDiagram
    participant Biz as 业务运行库
    participant Desensitize as 脱敏流程
    participant Annot as 标注平台
    participant Snapshot as 数据快照服务
    participant Train as 模型训练服务

    Biz->>Desensitize: 导出候选业务数据
    Desensitize->>Desensitize: 删除身份信息并生成 patient_uuid
    Desensitize->>Annot: 提供脱敏副本
    Annot->>Annot: 标注、复核、仲裁
    Annot->>Snapshot: 提交可训练样本
    Snapshot->>Snapshot: 固化版本号与 Dataset Card
    Snapshot->>Train: 提供训练快照
```

---

## 6. 状态机图

### 6.1 病例状态机

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> IMAGE_UPLOADED: 上传影像
    IMAGE_UPLOADED --> QC_CHECKING: 触发质检
    QC_CHECKING --> QC_REJECTED: 质检失败
    QC_CHECKING --> AI_ANALYZING: 质检通过
    QC_REJECTED --> IMAGE_UPLOADED: 重新上传
    AI_ANALYZING --> AI_FAILED: 推理异常
    AI_ANALYZING --> AI_DONE: 推理完成
    AI_FAILED --> AI_ANALYZING: 重试
    AI_DONE --> RISK_DONE: 风险评估完成
    RISK_DONE --> REPORT_READY: 报告生成
    REPORT_READY --> REVIEW_PENDING: 等待医生复核
    REVIEW_PENDING --> CORRECTED: 医生修正
    REVIEW_PENDING --> REVIEWED: 医生确认
    CORRECTED --> REPORT_READY: 重新生成报告
    REVIEWED --> FOLLOWUP_SCHEDULED: 需要随访
    REVIEWED --> CLOSED: 无需随访
    FOLLOWUP_SCHEDULED --> CLOSED: 完成随访
    CLOSED --> [*]
```

### 6.2 模型版本状态机

```mermaid
stateDiagram-v2
    [*] --> CANDIDATE
    CANDIDATE --> UNDER_REVIEW: 提交评估结果
    UNDER_REVIEW --> ACTIVE: 审批通过
    UNDER_REVIEW --> REJECTED: 审批拒绝
    ACTIVE --> ARCHIVED: 版本替换
    REJECTED --> ARCHIVED
    ARCHIVED --> [*]
```

---

## 7. 部署图

### 7.1 当前落地部署图（演示环境）

```mermaid
graph TB
    Browser[浏览器]

    subgraph Host[单机演示服务器]
        subgraph DockerCompose[Docker Compose]
            Nginx[Nginx]
            Frontend[Frontend]
            Backend[Spring Boot]
            AI[FastAPI AI Service]
            MySQL[(MySQL)]
            Redis[(Redis)]
            MinIO[(MinIO)]
            MQ[(RabbitMQ)]
        end
        GPU[GPU]
        Disk[SSD Storage]
    end

    Browser --> Nginx
    Nginx --> Frontend
    Nginx --> Backend
    Backend --> MySQL
    Backend --> Redis
    Backend --> MinIO
    Backend --> MQ
    Backend --> AI
    MQ --> AI
    AI --> GPU
    MySQL --> Disk
    MinIO --> Disk
```

### 7.2 数据边界部署示意图

```mermaid
graph LR
    subgraph Biz[业务运行区]
        B1[业务数据库]
        B2[原始影像存储]
    end

    subgraph TrainZone[训练准备区]
        T1[脱敏副本]
        T2[标注平台]
        T3[数据快照仓库]
        T4[模型资产仓库]
    end

    B1 --> T1
    B2 --> T1
    T1 --> T2
    T2 --> T3
    T3 --> T4
```

---

## 8. ER 图

### 8.1 业务运行核心 ER 图

```mermaid
erDiagram
    SYS_USER ||--o{ SYS_USER_ROLE : has
    SYS_ROLE ||--o{ SYS_USER_ROLE : assigned_to

    PATIENT ||--|| RISK_FACTOR_PROFILE : owns
    PATIENT ||--o{ CASE_RECORD : has
    CASE_RECORD ||--o{ IMAGE_FILE : contains
    CASE_RECORD ||--o{ AI_TASK : triggers
    AI_TASK ||--|| AI_RESULT : produces
    CASE_RECORD ||--|| RISK_ASSESSMENT : has
    CASE_RECORD ||--o{ REPORT_RECORD : generates
    CASE_RECORD ||--o{ CORRECTION_FEEDBACK : records
    CASE_RECORD ||--o{ FOLLOWUP_PLAN : creates

    PATIENT {
        bigint id PK
        varchar patient_no
        varchar patient_uuid
        varchar name
        tinyint gender
        int age
        bigint org_id
    }

    RISK_FACTOR_PROFILE {
        bigint id PK
        bigint patient_id FK
        tinyint brushing_freq
        tinyint sugar_diet_score
        boolean fluoride_use
        int last_check_months
        boolean orthodontic_history
        int previous_caries_count
    }

    CASE_RECORD {
        bigint id PK
        varchar case_no
        bigint patient_id FK
        varchar status
        bigint created_by
        datetime created_at
    }

    IMAGE_FILE {
        bigint id PK
        bigint case_id FK
        varchar image_type
        varchar storage_key
        varchar md5
        varchar quality_status
    }

    AI_TASK {
        bigint id PK
        bigint case_id FK
        bigint image_id FK
        varchar status
        varchar model_version
    }

    AI_RESULT {
        bigint id PK
        bigint task_id FK
        varchar lesion_mask_key
        varchar grading_label
        double uncertainty_score
        json raw_output
    }

    RISK_ASSESSMENT {
        bigint id PK
        bigint case_id FK
        varchar risk_level
        double risk_score
        json explanation_json
    }

    REPORT_RECORD {
        bigint id PK
        bigint case_id FK
        varchar report_type
        int version
        varchar storage_key
    }

    CORRECTION_FEEDBACK {
        bigint id PK
        bigint case_id FK
        bigint ai_result_id FK
        json original_value
        json corrected_value
        boolean approved_for_training
    }

    FOLLOWUP_PLAN {
        bigint id PK
        bigint case_id FK
        date next_date
        varchar priority
        varchar status
    }
```

### 8.2 训练治理 ER 图

```mermaid
erDiagram
    DATASET_SNAPSHOT ||--o{ ANNOTATION_RECORD : contains
    DATASET_SNAPSHOT ||--o{ GOLD_SET_ITEM : contains
    DATASET_SNAPSHOT ||--o{ MODEL_VERSION : trains
    MODEL_VERSION ||--o{ APPROVAL_RECORD : reviewed_by

    DATASET_SNAPSHOT {
        bigint id PK
        varchar dataset_version
        varchar dataset_card_path
        json source_summary
        json split_summary
        datetime released_at
    }

    ANNOTATION_RECORD {
        bigint id PK
        varchar image_id
        varchar patient_uuid
        varchar qc_status
        varchar annotation_version
    }

    GOLD_SET_ITEM {
        bigint id PK
        varchar image_id
        varchar difficulty
        boolean active
    }

    MODEL_VERSION {
        bigint id PK
        varchar model_name
        varchar version
        varchar dataset_version
        json metrics
        varchar status
    }

    APPROVAL_RECORD {
        bigint id PK
        bigint model_version_id FK
        varchar approver
        varchar decision
        text notes
        datetime approved_at
    }
```

---

## 9. 活动图

### 9.1 基础筛查闭环活动图

```mermaid
flowchart TD
    A([开始]) --> B[登录系统]
    B --> C[创建或选择患者]
    C --> D[创建病例并录入风险因子]
    D --> E[上传影像]
    E --> F{质量检查是否通过}
    F -->|否| G[提示重传或隔离]
    G --> E
    F -->|是| H[提交AI分析]
    H --> I[牙位检测 + TPC-Net分割]
    I --> J[EDL分级与不确定性计算]
    J --> K[多模态风险评估]
    K --> L[生成报告]
    L --> M{是否高不确定性或高风险}
    M -->|是| N[进入复核队列]
    M -->|否| O[医生确认]
    N --> P[医生修正]
    P --> Q[记录回流日志]
    Q --> R[重新生成报告]
    R --> S[创建随访计划]
    O --> S
    S --> T([结束])
```

### 9.2 数据回流治理活动图

```mermaid
flowchart TD
    A([周期开始]) --> B[导出待审核修正样本]
    B --> C[执行脱敏]
    C --> D[临床顾问确认样本有效性]
    D --> E{是否准入训练池}
    E -->|否| F[归档但不训练]
    E -->|是| G[生成数据快照版本]
    G --> H[离线训练候选模型]
    H --> I[评估指标与错误分析]
    I --> J[提交审批]
    J --> K{审批是否通过}
    K -->|否| L[归档候选模型]
    K -->|是| M[激活新版本]
    M --> N([结束])
    F --> N
    L --> N
```

---

## 10. 图集维护要求

1. 本图集必须与《项目总体设计文档》保持一致；
2. 每次状态流、模块边界或数据边界发生变化，都要同步更新；
3. 答辩 PPT 中优先使用：
   - 平台总体组件图；
   - 影像上传到报告生成时序图；
   - 基础筛查闭环活动图；
   - 当前落地部署图；
4. 未来扩展架构可单独附录说明，但不作为当前主图展示；
5. 重要评审节点建议将 Mermaid 图导出为 PNG/SVG 固化版本。
