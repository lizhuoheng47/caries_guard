# 07B API 契约与 DTO 规范

## 1. 边界冻结

| 调用方 | 基础路径 | 说明 |
| --- | --- | --- |
| Frontend -> Java | `/api/v1/kb/**` | 知识治理 |
| Frontend -> Java | `/api/v1/rag/**` | 问答、日志、评估 |
| Java -> Python | `/ai/v1/knowledge/**` | 内部知识治理 |
| Java -> Python | `/ai/v1/rag/**` | 内部 RAG |
| Java -> Python | `/ai/v1/logs/**` | 内部日志查询 |
| Java -> Python | `/ai/v1/eval/**` | 内部评估 |

## 2. Java 对前端

### 2.1 知识治理接口

| 接口 | 方法 | 请求 DTO |
| --- | --- | --- |
| `/api/v1/kb/overview` | `GET` | `kbCode?` |
| `/api/v1/kb/documents` | `GET` | `kbCode?`, `keyword?` |
| `/api/v1/kb/documents/{id}` | `GET` | path `id` |
| `/api/v1/kb/documents/import-text` | `POST` | `RagKbImportTextCommand` |
| `/api/v1/kb/documents/upload` | `POST multipart` | file + form fields |
| `/api/v1/kb/documents/{id}` | `PUT` | `RagKbUpdateCommand` |
| `/api/v1/kb/documents/{id}/submit-review` | `POST` | `RagVersionActionCommand` |
| `/api/v1/kb/documents/{id}/approve` | `POST` | `RagVersionActionCommand` |
| `/api/v1/kb/documents/{id}/reject` | `POST` | `RagVersionActionCommand` |
| `/api/v1/kb/documents/{id}/publish` | `POST` | `RagVersionActionCommand` |
| `/api/v1/kb/documents/{id}/rollback` | `POST` | `RagVersionActionCommand` |
| `/api/v1/kb/rebuild` | `POST` | `RagKbRebuildCommand` |
| `/api/v1/kb/rebuild-jobs` | `GET` | `kbCode?` |
| `/api/v1/kb/ingest-jobs` | `GET` | 无 |

### 2.2 RAG 与日志接口

| 接口 | 方法 | 请求 DTO |
| --- | --- | --- |
| `/api/v1/rag/doctor-qa` | `POST` | `DoctorQaCommand` |
| `/api/v1/rag/patient-explanation` | `POST` | `PatientExplanationCommand` |
| `/api/v1/rag/ask` | `POST` | `RagAskCommand` |
| `/api/v1/rag/logs/requests` | `GET` | 无 |
| `/api/v1/rag/logs/requests/{requestNo}` | `GET` | path `requestNo` |
| `/api/v1/rag/logs/retrievals/{requestNo}` | `GET` | path `requestNo` |
| `/api/v1/rag/logs/graph/{requestNo}` | `GET` | path `requestNo` |
| `/api/v1/rag/eval/runs` | `GET` | 无 |
| `/api/v1/rag/eval/run` | `POST` | `RagEvalRunCommand` |

## 3. Java -> Python DTO 对应

| Java DTO | Python DTO |
| --- | --- |
| `RagKbImportTextCommand` | `KnowledgeDocumentRequest` |
| `RagKbUpdateCommand` | `KnowledgeDocumentUpdateRequest` |
| `RagVersionActionCommand` | `KnowledgeVersionActionRequest` |
| `RagKbRebuildCommand` | `KnowledgeRebuildRequest` |
| `DoctorQaCommand` | `DoctorQaRequest` |
| `PatientExplanationCommand` | `PatientExplanationRequest` |
| `RagAskCommand` | `RagAskRequest` |
| `RagEvalRunCommand` | `RagEvalRunRequest` |

固定规则：

1. Java 负责补充 `traceId`。
2. Java 负责补充 `orgId`、`operatorId`、`reviewerId`。
3. Python 只接收 camelCase 别名，不再使用裸 `dict` 驱动状态变化。

## 4. 核心请求示例

### 4.1 导入文本

```json
{
  "kbCode": "caries-default",
  "kbName": "CariesGuard Default Knowledge Base",
  "kbTypeCode": "PATIENT_GUIDE",
  "docTitle": "儿童龋病复查指南",
  "docSourceCode": "GUIDELINE",
  "sourceUri": "internal://guideline/caries-followup",
  "docVersion": "v1.0",
  "contentText": "高风险儿童应在 3 个月内复查。",
  "reviewStatusCode": "APPROVED"
}
```

### 4.2 版本动作

```json
{
  "versionNo": "v1.1",
  "comment": "review passed"
}
```

### 4.3 统一问答

```json
{
  "question": "高风险儿童多久复查一次？",
  "scene": "DOCTOR_QA",
  "kbCode": "caries-default",
  "relatedBizNo": "CASE-20260419-001",
  "patientUuid": "masked-uuid",
  "caseContext": {
    "caseNo": "CASE-20260419-001",
    "riskLevelCode": "HIGH"
  }
}
```

### 4.4 评估运行

```json
{
  "datasetId": 1
}
```

## 5. 统一响应 DTO

所有接口统一使用：

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "traceId": "trace-20260419-001",
  "timestamp": "2026-04-19T12:00:00"
}
```

## 6. RAG 回答响应

```json
{
  "sessionNo": "RAG-20260419-001",
  "requestNo": "RAGREQ-20260419-001",
  "answer": "已发布知识显示高风险儿童通常需要更密切复查。",
  "answerText": "已发布知识显示高风险儿童通常需要更密切复查。",
  "citations": [],
  "retrievedChunks": [],
  "graphEvidence": [],
  "knowledgeBaseCode": "caries-default",
  "knowledgeVersion": "v1.0",
  "modelName": "gpt-4o-mini",
  "safetyFlag": "0",
  "safetyFlags": [
    "MEDICAL_CAUTION"
  ],
  "refusalReason": null,
  "confidence": 0.81,
  "traceId": "trace-20260419-001",
  "latencyMs": 732
}
```

## 7. 错误处理

1. Java 外层错误统一包装，不向前端直接透出 Python 内部堆栈。
2. Python 返回非 `00000` 时，Java 抛 `EXTERNAL_SERVICE_ERROR`。
3. DTO 新字段优先加在 JSON 扩展结构，不破坏必填字段。
