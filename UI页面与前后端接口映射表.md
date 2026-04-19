# UI 页面与前后端接口映射表

## 1. 文档目的

本文档用于把 Claude 提供的《CariesGuard UI Design Specification》与当前 GitHub 仓库的实际工程现状对齐，形成一份可直接交给前端、Java 后端、Python AI/RAG 与 Codex 执行的页面—接口—服务映射表。

本文档只回答四类问题：

1. 每个页面应该调用哪些前端接口。
2. 这些前端接口应由 Java BFF 还是 Python 直接提供。
3. 当前仓库中哪些能力已经存在，哪些仍属于待补齐缺口。
4. 每个页面下一步该如何推进，避免前端设计与接口现状脱节。

## 2. 统一接口边界

### 2.1 固定边界

- Frontend 只调用 Java 对外接口。
- Java 负责鉴权、机构隔离、页面视图聚合、错误收口、统一响应结构。
- Python 负责知识导入、解析、检索、图谱、RAG、评估与日志能力。
- 前端不得直接调用 Python `/ai/v1/**`。

### 2.2 统一前端 API 分组建议

前端建议固定为以下 API 模块：

- `auth.ts`
- `analysis.ts`
- `review.ts`
- `rag.ts`
- `knowledge.ts`
- `dashboard.ts`
- `case.ts`
- `system.ts`

### 2.3 统一响应结构

前端统一只消费 Java BFF 的外层响应：

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "traceId": "trace-20260419-001",
  "timestamp": "2026-04-19T12:00:00"
}
```

## 3. 页面总体映射总览

| 页面 | 路由 | 前端 API 模块 | Java BFF 是否应存在 | Python 现有承接能力 | 当前匹配度 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| 登录页 | `/login` | `auth.ts` | 是 | 无需 Python | 高 | 可直接做 |
| 影像分析详情页 | `/analysis/:taskId` | `analysis.ts` + `rag.ts` | 是 | 有 RAG 与分析结果承接 | 高 | 第一批落地 |
| AI 评估看板 | `/dashboard/ai` | `dashboard.ts` + `rag.ts` | 是，且必须聚合 | 有 eval 与日志，但缺 Java 聚合视图 | 中 | 先补 BFF 再做 |
| 分析任务队列 | `/analysis` | `analysis.ts` | 是 | 无需直接调 Python | 高 | 第一批落地 |
| 医生复核工作台 | `/review/:taskId` | `review.ts` + `analysis.ts` | 是，且必须工作台化 | 业务主链存在，但 UI 工作台接口不足 | 中 | 补工作台接口后落地 |
| 智能解释 / RAG Console | `/rag` | `rag.ts` | 是 | RAG、日志、评估能力存在 | 高 | 第一批落地 |
| 知识图库管理 | `/knowledge` | `knowledge.ts` | 是 | 知识治理链已进入代码层 | 高 | 第一批落地 |
| 病例与影像管理 | `/cases` | `case.ts` + `analysis.ts` | 是 | 详情链可确认，列表/创建链待补证 | 中 | 先核 Java 再做 |

## 4. 逐页映射明细

---

## Page 01 登录页 / Neural Console Entry

### 4.1 页面目标

完成账号登录、权限初始化、菜单装载、当前用户态初始化。

### 4.2 前端调用接口

#### `auth.ts`

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/auth/permissions`

#### `system.ts`（可选）

- `GET /api/v1/system/menus`
- `GET /api/v1/system/configs/{configKey}`

### 4.3 Java BFF 责任

Java 直接对外提供，无需 Python 参与。

Java 需要完成：

- 登录鉴权
- token / session 管理
- 当前用户信息返回
- 权限点与菜单树返回
- 比赛模式菜单裁剪

### 4.4 Python 侧依赖

无。

### 4.5 页面数据结构建议

```ts
interface LoginResponseVO {
  token: string
  refreshToken?: string
  expiresAt?: string
}

interface CurrentUserVO {
  userId: number
  username: string
  nickname: string
  roleCodes: string[]
  orgId: number
}

interface PermissionVO {
  permissionCodes: string[]
  menus: MenuNodeVO[]
}
```

### 4.6 当前匹配度判断

高。

### 4.7 下一步工作

- 前端可直接开做。
- 登录页视觉不受接口限制。
- 需要在登录成功后根据权限与比赛模式裁剪侧边栏菜单。

---

## Page 02 影像分析详情页 / Neural Diagnostic Console

### 5.1 页面目标

展示病例、影像、AI 分析结果、uncertainty、风险等级、可视化产物、RAG 解释、时间轴。

### 5.2 前端调用接口

#### `analysis.ts`

- `GET /api/v1/analysis/tasks/{taskId}`
- `GET /api/v1/cases/{caseId}`
- `GET /api/v1/cases/{caseId}/images/detail/{imageId}`
- `GET /api/v1/images/{imageId}/quality-checks/current`

#### `rag.ts`

- `POST /api/v1/rag/patient-explanation`
- `POST /api/v1/rag/doctor-qa`
- `POST /api/v1/rag/ask`

### 5.3 建议新增 Java 聚合接口

建议新增：

- `GET /api/v1/analysis/tasks/{taskId}/view`

理由：
当前 UI 页面是一个工作台视图，不适合前端分别调用多个接口后再组装。

### 5.4 建议返回的视图对象

```ts
interface AnalysisDetailViewVO {
  task: AnalysisTaskVO
  patient: PatientBriefVO
  caseInfo: CaseBriefVO
  image: ImageDetailVO
  analysisSummary: {
    gradingLabel: string
    confidenceScore?: number
    uncertaintyScore?: number
    needsReview: boolean
    riskLevel?: string
    riskFactors?: string[]
    visualAssets?: VisualAssetVO[]
  }
  timeline: TimelineNodeVO[]
  ragHint?: {
    enabled: boolean
    latestAnswer?: string
    latestCitations?: CitationVO[]
  }
}
```

### 5.5 Java BFF 责任

- 读取业务病例与影像主数据
- 读取 AI task 结果摘要
- 生成时间轴 VO
- 补充图像访问 URL
- 将 RAG 调试入口收敛成统一调用

### 5.6 Python 侧来源

- 分析 callback 结果已经进入业务主链
- RAG 输出已支持：`answer`、`answerText`、`citations`、`retrievedChunks`、`graphEvidence`、`safetyFlags`、`confidence`、`traceId`

### 5.7 当前匹配度判断

高。

### 5.8 下一步工作

- 第一批优先实现。
- 前端影像区建议第一阶段使用普通图片 + SVG/CSS overlay，不强依赖 Cornerstone/OHIF。
- Java 新增 `/view` 聚合接口后，前端实现会更稳。

---

## Page 03 AI 评估看板 / Intelligence Overview

### 6.1 页面目标

展示模型运行质量、RAG 检索效果、评估指标与系统事件。

### 6.2 前端调用接口

#### `dashboard.ts`

- `GET /api/v1/dashboard/overview`
- `GET /api/v1/dashboard/model-runtime`

#### `rag.ts`

- `GET /api/v1/rag/eval/runs`
- `POST /api/v1/rag/eval/run`
- `GET /api/v1/rag/logs/requests`

### 6.3 建议新增 Java 聚合接口

必须新增：

- `GET /api/v1/dashboard/ai-neural`

### 6.4 建议返回的聚合对象

```ts
interface AINeuralDashboardVO {
  kpis: {
    totalTasks: number
    reviewRate: number
    avgUncertainty: number
    ragRequestCount: number
    latestKnowledgeVersion?: string
  }
  uncertaintyDistribution: Array<{ bucket: string; count: number }>
  confusionMatrix: number[][]
  modelCapability: Array<{ dimension: string; score: number; baseline?: number }>
  gradingDistribution: Array<{ grade: string; count: number; ratio: number }>
  activityHeatmap: Array<{ day: string; hour: number; value: number }>
  systemEvents: Array<SystemEventVO>
  latestEvalSummary?: {
    datasetName: string
    citationAccuracy: number
    graphPathHitRate: number
    refusalPrecision: number
    groundednessRate: number
    avgLatencyMs: number
  }
}
```

### 6.5 Java BFF 责任

- 聚合业务看板指标
- 聚合 Python eval run 结果
- 聚合 RAG 日志摘要
- 将复杂图表数据一次性成型返回

### 6.6 Python 侧来源

- `/ai/v1/eval/runs`
- `/ai/v1/eval/run`
- `/ai/v1/logs/requests`
- `rag_eval_*` 表
- `rag_request_log` / `rag_retrieval_log` / `rag_fusion_log` / `rag_rerank_log` / `llm_call_log`

### 6.7 当前匹配度判断

中。

原因：

- Python 评估和日志基础存在。
- Java 当前接口概览中已有 dashboard 入口，但未证明有专门面向 UI 看板的聚合视图。

### 6.8 下一步工作

- 第二批做。
- 先定义好 `AINeuralDashboardVO`。
- 再补 Java 聚合实现。

---

## Page 04 分析任务队列 / Analysis Pipeline

### 7.1 页面目标

展示分析任务分页列表、筛选、排序、状态标签、跳转详情、失败重试。

### 7.2 前端调用接口

#### `analysis.ts`

- `GET /api/v1/analysis/tasks`
- `GET /api/v1/analysis/tasks/{taskId}`
- `POST /api/v1/analysis/tasks/retry`

### 7.3 建议分页查询参数

```ts
interface AnalysisTaskQuery {
  pageNum: number
  pageSize: number
  keyword?: string
  statusCode?: string
  grade?: string
  needsReview?: boolean
  createdFrom?: string
  createdTo?: string
}
```

### 7.4 建议列表项对象

```ts
interface AnalysisTaskListItemVO {
  taskId: number
  taskNo: string
  patientNameMasked?: string
  patientIdMasked?: string
  caseNo: string
  gradingLabel?: string
  uncertaintyScore?: number
  statusCode: 'DONE' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'QUEUED'
  createdAt: string
  durationMs?: number
  needsReview: boolean
}
```

### 7.5 Java BFF 责任

- 业务分页
- 条件筛选
- 状态文本和枚举值映射
- 重试动作封装

### 7.6 Python 侧依赖

无直接依赖。

### 7.7 当前匹配度判断

高。

### 7.8 下一步工作

- 第一批直接做。
- 这页是最稳的“后台风格 + HUD 风格”结合页。

---

## Page 05 医生复核工作台 / Review Workbench

### 8.1 页面目标

支持医生查看 AI 原始结果、对照影像、修正分级、填写原因、提交复核与导出训练候选样本。

### 8.2 前端调用接口

#### `review.ts`

现有主流程接口：

- `POST /api/v1/cases/{caseId}/corrections`
- `POST /api/v1/analysis/corrections/review`
- `POST /api/v1/analysis/corrections/training-candidates/export`

#### `analysis.ts`

- `GET /api/v1/analysis/tasks/{taskId}`
- `GET /api/v1/cases/{caseId}/images/detail/{imageId}`

### 8.3 建议新增 Java 工作台接口

建议新增：

- `GET /api/v1/review/queue`
- `GET /api/v1/review/tasks/{taskId}`
- `POST /api/v1/review/tasks/{taskId}/draft`
- `POST /api/v1/review/tasks/{taskId}/submit`
- `POST /api/v1/review/tasks/{taskId}/second-opinion`

### 8.4 建议返回的工作台对象

```ts
interface ReviewWorkbenchVO {
  task: AnalysisTaskVO
  caseInfo: CaseBriefVO
  image: ImageDetailVO
  aiResult: {
    gradingLabel?: string
    uncertaintyScore?: number
    detections?: DetectionBoxVO[]
    visualAssets?: VisualAssetVO[]
  }
  doctorDraft?: {
    draftId?: number
    revisedGrade?: string
    revisedDetections?: DetectionBoxVO[]
    reasonTags?: string[]
    note?: string
  }
  reviewOptions: {
    gradeOptions: string[]
    reasonTags: string[]
  }
}
```

### 8.5 Java BFF 责任

- 聚合 AI 原始结果与病例主数据
- 读取医生草稿
- 提供理由标签选项
- 复核提交/导出动作封装

### 8.6 Python 侧依赖

无直接页面调用依赖。

### 8.7 当前匹配度判断

中。

原因：

- 复核主流程存在。
- 但“医生工作台视图接口”当前没有足够证据表明已完整落地。

### 8.8 下一步工作

- 第二批实现。
- 先补工作台型 View API，再做 UI。

---

## Page 06 智能解释 / RAG Knowledge Console

### 9.1 页面目标

完成患者解释、医生问答、引用回显、知识版本展示、safety flags 展示、RAG 日志追踪。

### 9.2 前端调用接口

#### `rag.ts`

- `POST /api/v1/rag/doctor-qa`
- `POST /api/v1/rag/patient-explanation`
- `POST /api/v1/rag/ask`
- `GET /api/v1/rag/logs/requests`
- `GET /api/v1/rag/logs/requests/{requestNo}`
- `GET /api/v1/rag/logs/retrievals/{requestNo}`
- `GET /api/v1/rag/logs/graph/{requestNo}`

### 9.3 建议前端直接采用的响应对象

```ts
interface RagAnswerVO {
  sessionNo: string
  requestNo: string
  answer: string
  answerText: string
  citations: CitationVO[]
  retrievedChunks: RetrievedChunkVO[]
  graphEvidence: GraphEvidenceVO[]
  knowledgeBaseCode: string
  knowledgeVersion: string
  modelName: string
  safetyFlag: '0' | '1'
  safetyFlags: string[]
  refusalReason?: string | null
  confidence?: number | null
  traceId?: string | null
  latencyMs: number
}
```

### 9.4 Java BFF 责任

- 注入 `traceId`、`orgId`、`operatorId`
- 对患者场景和医生场景做请求 DTO 映射
- 封装日志查询接口

### 9.5 Python 侧来源

- RAG orchestrator 三路检索已存在
- 日志接口已存在
- eval 接口已存在

### 9.6 当前匹配度判断

高。

### 9.7 下一步工作

- 第一批直接做。
- 该页可和知识图库页并行推进。

---

## Page 07 知识图库管理 / Knowledge Repository

### 10.1 页面目标

支持知识文档列表、上传、文本导入、详情、编辑、提交审核、审核通过/驳回、发布、回滚、rebuild、ingest/rebuild job 查看。

### 10.2 前端调用接口

#### `knowledge.ts`

- `GET /api/v1/kb/overview`
- `GET /api/v1/kb/documents`
- `GET /api/v1/kb/documents/{id}`
- `POST /api/v1/kb/documents/import-text`
- `POST /api/v1/kb/documents/upload`
- `PUT /api/v1/kb/documents/{id}`
- `POST /api/v1/kb/documents/{id}/submit-review`
- `POST /api/v1/kb/documents/{id}/approve`
- `POST /api/v1/kb/documents/{id}/reject`
- `POST /api/v1/kb/documents/{id}/publish`
- `POST /api/v1/kb/documents/{id}/rollback`
- `POST /api/v1/kb/rebuild`
- `GET /api/v1/kb/rebuild-jobs`
- `GET /api/v1/kb/ingest-jobs`

### 10.3 页面建议拆分子视图

- 知识库总览 Tab
- 文档列表 Tab
- 文档详情抽屉/页
- 上传弹窗
- 文本导入弹窗
- 审核动作弹窗
- Rebuild Job 列表
- Ingest Job 列表

### 10.4 建议列表对象

```ts
interface KnowledgeDocumentListItemVO {
  docId: number
  docNo: string
  docTitle: string
  docSourceCode: string
  reviewStatusCode: string
  publishStatusCode: string
  currentVersionNo?: string
  publishedVersionNo?: string
  chunkCount?: number
  entityCount?: number
  relationCount?: number
  updatedAt: string
}
```

### 10.5 Java BFF 责任

- 与 Python 知识治理接口一一映射
- 处理上传文件与文本导入两种模式
- 统一动作类响应

### 10.6 Python 侧来源

- `/ai/v1/knowledge/**` 现有能力已较完整
- `KnowledgeService` 已覆盖上传、解析、chunk、索引、图谱、审核、发布、回滚、rebuild

### 10.7 当前匹配度判断

高。

### 10.8 下一步工作

- 第一批直接做。
- 这是最适合和 RAG Console 一起先完成的页面。

---

## Page 08 病例与影像管理 / Case & Image Portal

### 11.1 页面目标

支持病例分页列表、影像上传入口、病例详情跳转、最近分析状态展示。

### 11.2 前端调用接口

#### `case.ts`

可确认的详情类接口：

- `GET /api/v1/patients/{patientId}`
- `PUT /api/v1/patients/{patientId}`
- `GET /api/v1/visits/{visitId}`
- `GET /api/v1/cases/{caseId}`

#### `analysis.ts`

- `POST /api/v1/cases/{caseId}/analysis`
- `POST /api/v1/files/upload`
- `GET /api/v1/files/{attachmentId}/access-url`

### 11.3 明确缺口

以下能力在当前可核接口概览中证据不足：

- 病例分页列表接口
- 病例新建接口
- 卡片筛选接口
- 影像多文件批量上传后的病例绑定接口

### 11.4 建议新增 Java BFF 接口

- `GET /api/v1/cases`
- `POST /api/v1/cases`
- `GET /api/v1/cases/card-view`
- `POST /api/v1/cases/{caseId}/images`

### 11.5 建议列表对象

```ts
interface CaseCardVO {
  caseId: number
  caseNo: string
  patientIdMasked?: string
  patientNameMasked?: string
  age?: number
  gender?: string
  visitTime?: string
  latestImageThumbUrl?: string
  latestTaskStatus?: string
  latestGrade?: string
  latestUncertainty?: number
}
```

### 11.6 当前匹配度判断

中。

### 11.7 下一步工作

- 先确认 Java 是否已有 list/create 接口。
- 如无，先补病例门户 BFF，再做页面。

---

## 5. 插件与技术栈映射建议

### 12.1 保留项

- Vue 3 + TypeScript + Vite
- Pinia
- Vue Router
- Axios
- Tailwind CSS + 自定义 HUD CSS
- ECharts
- Lucide Icons

### 12.2 建议收敛项

#### Cornerstone.js / OHIF Viewer

建议定位为：

- 第二阶段增强功能
- 非第一阶段硬依赖

原因：

- 当前可核对的是影像详情与附件访问能力
- 但不足以证明已完整进入 DICOM 工作站级深度集成

第一阶段建议：

- 普通影像查看 + CSS/SVG overlay
- 实现扫描线、检测框、HUD 叠加层

### 12.3 组件层建议

建议前端先落一套基础组件，不要一页一页硬写：

- `ChromeBar`
- `AppSidebar`
- `Panel`
- `KpiCard`
- `StatusChip`
- `GradeBadge`
- `UncertaintyBar`
- `Timeline`
- `HudChip`
- `NeuralButton`
- `DetectionReticle`
- `CitationTag`

## 6. 页面实施优先级

### 13.1 第一批直接做

这些页面与现有接口匹配度高：

1. 登录页
2. 分析任务队列
3. 影像分析详情页
4. 智能解释 / RAG Console
5. 知识图库管理

### 13.2 第二批补 BFF 后做

这些页面需要先补 Java View API：

1. AI 评估看板
2. 医生复核工作台
3. 病例与影像管理页

## 7. 给 Codex 的直接执行要求

1. 独立创建前端工程并入仓。
2. 前端只实现对 Java `/api/v1/**` 的调用，不允许写 Python 直连代码。
3. 先完成第一批五个页面。
4. 对第二批页面，先补 Java BFF 再写页面。
5. 所有页面的前端 DTO 必须以 Java ViewObject 为准，不直接依赖 Python 内部返回结构。
6. 影像查看器第一阶段不得强绑 DICOM viewer，先完成 overlay 能力。

## 8. 最终判断

这份 UI 设计规范在视觉和产品层面与当前项目目标高度一致，尤其适合比赛答辩展示；但它对应的是目标态前端，不是当前所有接口都已经完整具备的现状态前端。

因此，正确的落地方式不是修改视觉设计，而是：

- 保留 UI 设计规范；
- 用本映射表把页面和现有接口、缺口接口逐一对齐；
- 按“先可落地页面、后补聚合接口页面”的顺序推进。
