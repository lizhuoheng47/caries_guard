# A Java / Python 接口契约对照表

## 1. 契约原则

- Java 是业务入口和状态权威；
- Python 是 AI/RAG 服务提供方；
- Java -> Python analysis 请求通过 MQ；
- Python -> Java analysis 结果通过 HTTP callback；
- RAG/知识问答通过 HTTP API；
- 新增 AI 字段优先进入 `raw_result_json`，不随意破坏顶层 DTO；
- 所有接口都必须携带 trace id 或可追踪任务号。

## 2. Analysis 请求

Java 发送 analysis task，核心字段：

| 字段 | 说明 |
| --- | --- |
| `taskNo` | Java 任务号 |
| `traceId` | 链路追踪 |
| `caseId/caseNo` | 病例标识 |
| `patientId` | 患者标识 |
| `orgId` | 机构 |
| `images[]` | 影像输入 |
| `patientProfile` | 风险评估上下文 |
| `modelVersion` | 对外模型版本 |

影像可通过 MinIO bucket/object 或 Java 文件代理链接获取。

## 3. Analysis Callback

Python 回传 Java：

| 字段 | 说明 |
| --- | --- |
| `taskNo` | 任务号 |
| `taskStatusCode` | `SUCCESS/FAILED` |
| `startedAt/completedAt` | 执行时间 |
| `modelVersion` | 模型版本 |
| `summary` | 摘要 |
| `rawResultJson` | 完整证据链 |
| `visualAssets` | MASK/OVERLAY/HEATMAP 元数据 |
| `riskAssessment` | 风险辅助结果 |
| `errorCode/errorMessage` | 失败信息 |
| `traceId` | 链路追踪 |
| `inferenceMillis` | 耗时 |
| `uncertaintyScore` | 总体不确定性 |

## 4. Phase 5C rawResultJson 必备字段

```json
{
  "pipelineVersion": "phase5c-1",
  "mode": "hybrid",
  "qualityMode": "real",
  "qualityImplType": "HEURISTIC",
  "toothDetectionMode": "real",
  "toothDetectionImplType": "HEURISTIC",
  "segmentationMode": "real",
  "segmentationImplType": "HEURISTIC",
  "gradingMode": "real",
  "gradingImplType": "HEURISTIC",
  "gradingLabel": "C2",
  "confidenceScore": 0.63,
  "uncertaintyMode": "real",
  "uncertaintyImplType": "HEURISTIC",
  "uncertaintyScore": 0.37,
  "needsReview": true
}
```

## 5. RAG HTTP 契约

Java 调用 Python RAG API 时，必须传入：

- 用户问题；
- 当前用户角色；
- 可用病例上下文；
- 可访问知识库范围；
- trace id。

Python 返回：

- answer；
- citations；
- retrievedChunks；
- safetyFlags；
- confidence；
- refusalReason；
- trace id。

RAG 使用通用大模型 + 知识图库，不要求 Java 感知微调模型。

## 6. 错误码约定

| 错误码 | 含义 |
| --- | --- |
| `M5001/M5002` | quality adapter 不可用或推理失败 |
| `M5003/M5004` | tooth detection 不可用或失败 |
| `M5005/M5006` | segmentation 不可用或失败 |
| `M5007/M5008` | grading 不可用或失败 |
| `C9999` | 未分类系统错误 |

`real` 模式不允许静默回退。

## 7. 兼容性要求

- Java callback DTO 允许未知 JSON 字段；
- visualAssets 顶层结构保持稳定；
- 高 uncertainty 通过 `needsReview` 驱动 review pending 语义；
- Java 报告和复核模块读取 `raw_result_json` 时不得因新增字段失败。
