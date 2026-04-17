# RAG 业务接入说明（Phase 3）

## 目标

将 Python 内部可运行的 RAG 能力接入 Java 业务表面，形成医生端问答和患者版报告解释能力。

## 边界约束

- Java 不直接读取 `caries_ai` 的 RAG 表。
- Java 只通过 Python HTTP API 调用 RAG。
- Python 继续负责 `rag_session`、`rag_request_log`、`rag_retrieval_log`、`llm_call_log` 留痕。
- Java 调用失败时返回兜底文案，不阻断报告生成主链路。

## Java 对外接口

### 医生端问答

```text
POST /api/v1/rag/doctor-qa
```

请求核心字段：

- `question`：医生问题，必填
- `kbCode`：知识库编码，可选
- `topK`：检索条数，1 到 20
- `relatedBizNo`：关联业务编号，可用于病历或报告追踪
- `patientUuid`：患者标识，可选
- `clinicalContext`：结构化临床上下文，可选

返回核心字段：

- `answerText`
- `citations`
- `knowledgeVersion`
- `modelName`
- `safetyFlag`
- `latencyMs`
- `fallback`

### 患者解释

```text
POST /api/v1/rag/patient-explanation
```

请求核心字段：

- `question`：问题，可选；为空时 Python 可按病例摘要生成默认解释
- `caseSummary`：结构化病例摘要
- `riskLevelCode`：风险等级
- `relatedBizNo`：关联报告号或病例号

## 报告集成

患者版报告生成时会调用 `RagAppService.generatePatientReportExplanation(...)`，将病例摘要、牙位发现、风险等级、复查建议等结构化字段传给 Python：

```text
Java ReportAppService
  -> RagAppService
  -> RagClient
  -> Python /ai/v1/rag/patient-explanation
```

RAG 返回文本会写入渲染占位符：

```text
{{patientExplanation}}
```

如果 Python RAG 不可用，该占位符回落到 Java 本地护理建议，不影响 PDF 生成、归档和导出链路。

## Python 接口

Java 适配层调用：

```text
POST {caries.rag.base-url}/rag/doctor-qa
POST {caries.rag.base-url}/rag/patient-explanation
```

默认配置：

```yaml
caries:
  rag:
    base-url: http://backend-python:8001/ai/v1
    connect-timeout-millis: 3000
    request-timeout-millis: 10000
```

本地 profile 默认使用：

```text
http://127.0.0.1:8001/ai/v1
```

Docker profile 默认使用：

```text
http://backend-python:8001/ai/v1
```

## 验收点

- 医生端问答接口可返回答案和引用。
- 患者版报告包含 `patientExplanation`。
- RAG 服务失败时 Java 返回 `fallback=true` 或报告内使用兜底文案。
- Java 业务库不新增跨库读取 `caries_ai` 的逻辑。
- Python 侧继续完成 RAG 请求、检索、LLM 调用日志落库。
