# UML图集

本文档只保留与当前代码一致的 UML 表达，同时把来源设计中的“治理原则”和“未落地能力”单独标注，避免把设计口径写成已实现事实。

## 1. 绘图规则

1. `当前实现` 与 `建议扩展` 分开描述。
2. `Risk` 作为能力存在，不画成当前独立模块。
3. `ModelAdmin` 只保留为治理设计原则，不画成当前独立模块。
4. 部署图优先反映当前真实可确认形态：Java 后端 + MySQL + Redis + RabbitMQ + 本地文件系统。

## 2. 当前用例图

```mermaid
flowchart LR
    Doctor[医生]
    Screener[筛查员]
    Admin[系统管理员]
    AISvc[外部AI服务]

    subgraph CG[当前 Java 业务平台]
        UC1[登录与权限]
        UC2[患者与就诊管理]
        UC3[病例管理]
        UC4[图像上传与质检]
        UC5[分析任务创建]
        UC6[AI 回调写回]
        UC7[报告生成]
        UC8[随访管理]
        UC9[看板统计]
    end

    Doctor --> UC1
    Doctor --> UC2
    Doctor --> UC3
    Doctor --> UC4
    Doctor --> UC5
    Doctor --> UC7
    Doctor --> UC8

    Screener --> UC1
    Screener --> UC2
    Screener --> UC3
    Screener --> UC4
    Screener --> UC5

    Admin --> UC1
    Admin --> UC9

    AISvc --> UC6
```

## 3. 当前组件图

```mermaid
flowchart TB
    Client[API Client / Swagger / 前端] --> Boot[caries-boot]

    subgraph Backend[Spring Boot Modules]
        Framework[caries-framework\nJWT / Security / Sensitive Data]
        System[caries-system\nAuth / User / Role / Menu / Dict]
        Patient[caries-patient\nPatient / Visit / Case / StatusMachine]
        Image[caries-image\nAttachment / Image / QualityCheck]
        Analysis[caries-analysis\nTask / Callback / Feedback / Risk Writeback]
        Report[caries-report\nTemplate / Generate / Export Audit]
        Followup[caries-followup\nPlan / Task / Record / Trigger]
        Dashboard[caries-dashboard\nOverview / Trend / ModelRuntime]
    end

    Boot --> Framework
    Boot --> System
    Boot --> Patient
    Boot --> Image
    Boot --> Analysis
    Boot --> Report
    Boot --> Followup
    Boot --> Dashboard

    Analysis --> Rabbit[(RabbitMQ)]
    Image --> LocalFS[(Local File Storage)]
    Report --> LocalFS
    Boot --> MySQL[(MySQL cg)]
    Boot --> Redis[(Redis)]
    AISvc[External AI Service] -->|callback| Analysis
    Analysis -->|publish requested| Rabbit
```

说明：
- MinIO 已作为当前对象存储 provider 落地，同时保留 LOCAL_FS 作为本地/E2E provider。
- 来源稿中的独立 Risk / ModelAdmin 不再画成当前实现组件。

## 4. 当前主链路时序图

### 4.1 病例创建到分析回调

```mermaid
sequenceDiagram
    participant User as Doctor/Screener
    participant Case as caries-patient
    participant Image as caries-image
    participant Analysis as caries-analysis
    participant MQ as RabbitMQ
    participant AI as External AI Service

    User->>Case: 创建患者 / 就诊 / 病例
    User->>Image: 上传附件并创建病例图像
    User->>Image: 保存图像质检(PASS)
    User->>Analysis: 创建分析任务
    Analysis->>Case: 病例状态 -> ANALYZING
    Analysis->>MQ: publish analysis.requested
    AI-->>MQ: 消费任务
    AI->>Analysis: POST /api/v1/internal/ai/callbacks/analysis-result
    Analysis->>Analysis: 验签 / 幂等校验 / 写回摘要与风险
    Analysis->>Case: 病例状态 -> REVIEW_PENDING 或 QC_PENDING
```

### 4.2 报告与随访链路

```mermaid
sequenceDiagram
    participant User as Doctor
    participant Report as caries-report
    participant Case as caries-patient
    participant Followup as caries-followup
    participant Store as LocalObjectStorageService

    User->>Report: 生成报告
    Report->>Report: 读取摘要 / 风险 / 图像 / 纠偏
    Report->>Store: 存储 PDF 附件
    Report->>Case: 状态 -> REPORT_READY
    Report->>Followup: triggerFromReport(...)
    Followup->>Followup: 满足条件时创建 plan/task/notify
    Followup->>Case: 尝试状态 -> FOLLOWUP_REQUIRED
```

## 5. 病例状态机图

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> QC_PENDING
    CREATED --> CANCELLED

    QC_PENDING --> ANALYZING
    QC_PENDING --> CREATED
    QC_PENDING --> CANCELLED

    ANALYZING --> REVIEW_PENDING
    ANALYZING --> QC_PENDING
    ANALYZING --> CANCELLED

    REVIEW_PENDING --> REPORT_READY
    REVIEW_PENDING --> FOLLOWUP_REQUIRED
    REVIEW_PENDING --> CANCELLED

    REPORT_READY --> FOLLOWUP_REQUIRED
    REPORT_READY --> CLOSED

    FOLLOWUP_REQUIRED --> CLOSED
    FOLLOWUP_REQUIRED --> CANCELLED

    CLOSED --> [*]
    CANCELLED --> [*]
```

## 6. 当前部署图

```mermaid
flowchart LR
    User[Browser / Swagger / API Client]
    User --> App[Spring Boot App\ncaries-boot]

    App --> DB[(MySQL cg)]
    App --> Cache[(Redis)]
    App --> MQ[(RabbitMQ)]
    App --> FS[(Local File Storage)]

    AI[External AI Service\nNot in this repo] -->|callback| App
    MQ -->|analysis.requested| AI
```

当前部署结论：
- 可以确认 Java 后端本地 profile 的依赖和运行方式。
- 不能把完整 Frontend / Python AI Service / Docker Compose 写成当前已交付事实；MinIO 对象存储 provider 已在 Java 后端落地。

## 7. 来源设计中保留但不画成当前实现的能力

### 7.1 模型治理能力

来源中的正确原则：
- 候选模型
- 离线评估
- 专家复核
- 人工审批上线
- 模型资产分层治理

当前 UML 处理方式：
- 不画成 `ModelAdmin` 独立模块
- 在文档说明里保留为“建议扩展治理能力”

### 7.2 风险评估能力

当前 UML 处理方式：
- 风险评估作为 `caries-analysis` 写回能力和 `caries-dashboard` 统计能力存在
- 不单独拆成 `Risk` 模块

## 8. 建议扩展图（不代表当前已实现）

```mermaid
flowchart TB
    subgraph Governance[建议扩展治理层]
        ModelGov[Model Governance\n未实现独立模块]
        DatasetGov[Dataset Snapshot / Review\n未实现独立模块]
    end

    Analysis[caries-analysis] --> ModelGov
    Analysis --> DatasetGov
    Dashboard[caries-dashboard] --> ModelGov
    Patient[caries-patient] --> DatasetGov
```

说明：
- 这张图只表达“应保留的治理方向”。
- 不可在答辩或现状描述中说它已经完成开发。

