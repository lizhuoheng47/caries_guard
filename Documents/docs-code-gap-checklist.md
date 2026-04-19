# docs-code-gap-checklist

基线文档：`CariesGuard_Codex任务分解与缺口清单.md`

## 1. 总览

| 文档项 | 仓库路径 | 当前状态 | 负责模块 | 下一步动作 |
| --- | --- | --- | --- | --- |
| Java `/api/v1/kb/**` BFF | `backend-java/caries-report/controller` | 已实现 | Java report | 保持与文档同步 |
| Java `/api/v1/rag/**` 日志/评估 BFF | `backend-java/caries-report/controller` | 已实现 | Java report | 保持与文档同步 |
| Python 知识治理主链 | `backend-python/app/api/v1/knowledge.py`, `services/knowledge_service.py` | 已实现 | Python knowledge | 保持与文档同步 |
| Python 细粒度日志接口 | `backend-python/app/api/v1/logs.py` | 已实现 | Python rag | 后续只做增强型筛选 |
| Python 评估详情接口 | `backend-python/app/api/v1/eval.py` | 已实现 | Python eval | 后续只做增强型导出 |
| 前端后台工程 | `frontend-web/` | 已实现 | frontend-web | 后续只做交互增强 |
| `Documents/04_接口与集成说明.md` | `Documents/04_接口与集成说明.md` | 已同步 | Documents | 继续作为接口总览 |
| `Documents/07B_API契约与DTO规范.md` | `Documents/07B_API契约与DTO规范.md` | 已同步 | Documents | 继续作为契约基线 |
| README 架构与运行建议 | `README.md` | 已同步 | root docs | 继续作为仓库入口 |

## 2. 逐项对照

### 2.1 `Documents/04_接口与集成说明.md`

| 项目 | 代码位置 | 状态 | 备注 |
| --- | --- | --- | --- |
| `/api/v1/kb/overview` | `RagKbController` | 已实现 | 已联通 Python `/knowledge/overview` |
| `/api/v1/kb/documents*` | `RagKbController` | 已实现 | 上传、导入、编辑、审核、发布、回滚均已联通 |
| `/api/v1/kb/graph-stats` | `RagKbController` | 已实现 | 本轮新增 |
| `/api/v1/rag/doctor-qa` | `RagController` | 已实现 | 已联通 Python |
| `/api/v1/rag/patient-explanation` | `RagController` | 已实现 | 已联通 Python |
| `/api/v1/rag/ask` | `RagController` | 已实现 | 已联通 Python |
| `/api/v1/rag/logs/requests*` | `RagLogController` | 已实现 | 已联通 Python |
| `/api/v1/rag/logs/fusion|rerank|llm` | `RagLogController` | 已实现 | 本轮新增 |
| `/api/v1/rag/eval/datasets*` | `RagLogController` | 已实现 | 本轮新增 |
| `/api/v1/rag/eval/runs*` | `RagLogController` | 已实现 | 已补 run detail/results |

### 2.2 `Documents/07_知识图库与AI全量实施方案.md` 及附录

| 文档项 | 代码位置 | 状态 | 备注 |
| --- | --- | --- | --- |
| 知识治理任务链 | `KnowledgeService` | 已实现 | 上传、解析、chunk、索引、图谱、审核、发布、回滚 |
| OpenSearch / Neo4j 检索 | `OpenSearchIndexService`, `GraphUpsertService` | 已实现 | 主链可运行 |
| 三路检索编排 | `RagOrchestrator` | 已实现 | lexical / dense / graph / fusion / rerank / refusal |
| 日志细分矩阵 | `RagRepository`, `RagLogService` | 已实现 | request / retrieval / graph / fusion / rerank / llm 可查 |
| 评估闭环 | `EvalService`, `EvalRepository` | 已实现 | dataset、run、aggregate、single result 可查 |

### 2.3 前端后台工程

| 页面要求 | 路径 | 状态 | 备注 |
| --- | --- | --- | --- |
| 知识库总览页 | `frontend-web/src/views/rag-kb/KbOverviewView.vue` | 已实现 |  |
| 文档列表页 | `DocumentListView.vue` | 已实现 |  |
| 文档详情/编辑页 | `DocumentDetailView.vue` | 已实现 | 详情页内可编辑 |
| 文档导入页 | `DocumentUploadView.vue` | 已实现 |  |
| 审核页 | `ReviewPublishView.vue` | 已实现 |  |
| 发布/回滚记录页 | `PublishHistoryView.vue` | 已实现 | 本轮新增 |
| rebuild job 页 | `RebuildJobsView.vue` | 已实现 | 本轮新增 |
| ingest job 页 | `IngestJobsView.vue` | 已实现 | 本轮新增 |
| graph stats 页 | `GraphStatsView.vue` | 已实现 | 本轮新增 |
| doctor QA 调试页 | `DoctorQaDebugView.vue` | 已实现 | 展示 requestNo / traceId / citations / refusal / confidence |
| patient explanation 调试页 | `PatientExplanationDebugView.vue` | 已实现 | 展示 requestNo / traceId / citations / refusal / confidence |
| request log 页 | `RagLogsView.vue` | 已实现 |  |
| retrieval detail 页 | `RetrievalDetailView.vue` | 已实现 | 本轮新增 |
| graph evidence 页 | `GraphEvidenceView.vue` | 已实现 | 支持路径摘要与原始 JSON |
| eval run 页 | `RagEvalView.vue` | 已实现 |  |
| eval result 页 | `EvalResultsView.vue` | 已实现 | 本轮新增 |

## 3. 当前未完成项

1. 没有 P0 未完成项。当前剩余项仅属于增强项，不影响文档要求的 5 条工作流闭环。
2. Java 侧目前补的是 `app service` 级回归测试，还没有额外新增 controller/integration 级用例。
3. Python 日志与评估接口暂未补分页、筛选、导出，这些不属于原任务文档的 P0 必做闭环。
4. 前端构建已通过，存在一个 Vite chunk size 警告，属于性能优化项，不是功能缺口。
