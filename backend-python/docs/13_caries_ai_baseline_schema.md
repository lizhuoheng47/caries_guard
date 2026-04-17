# 13 caries_ai Baseline Schema

## 1. 范围

本文档描述 Python AI/RAG 运行域数据库 `caries_ai` 的基线 schema。该库只保存 AI 服务运行数据、RAG 数据、模型治理数据和审计日志。

## 2. Schema 原则

- 与 Java 业务库 `caries_biz` 物理隔离；
- 只通过 Java task_no、image_id、case_id 等业务标识建立弱引用；
- 不保存明文患者隐私；
- 所有 AI 输出保留原始 JSON；
- RAG 和通用大模型调用必须保留可审计日志；
- 不以微调训练数据为核心，而以知识图库和检索日志为核心。

## 3. 表分组

### 3.1 推理运行表

- `ai_infer_job`
- `ai_infer_job_image`
- `ai_callback_log`

### 3.2 知识库/RAG 表

- `rag_knowledge_base`
- `rag_document`
- `rag_chunk`
- `rag_request_log`
- `rag_retrieval_log`
- `llm_call_log`

### 3.3 模型治理表

- `mdl_model_version`
- `mdl_model_eval_record`
- `mdl_model_approval_record`

## 4. 推理 JSON 基线

推理结果 JSON 必须支持 Phase 5A/5B/5C 的模式留痕：

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
  "segmentationRegions": [],
  "segmentationRawResult": {},
  "gradingMode": "real",
  "gradingImplType": "HEURISTIC",
  "gradingLabel": "C2",
  "confidenceScore": 0.63,
  "uncertaintyMode": "real",
  "uncertaintyImplType": "HEURISTIC",
  "uncertaintyScore": 0.37,
  "needsReview": true,
  "gradingRawResult": {},
  "visualAssets": []
}
```

## 5. RAG 基线

RAG 不依赖微调模型表作为主数据来源。知识数据必须能描述：

- 知识库编码和版本；
- 文档来源、标题、类型、发布时间；
- chunk 文本、hash、标签、引用路径；
- 检索 score、rank、过滤条件；
- 通用大模型 provider、model、prompt 摘要、响应摘要；
- 安全过滤、拒答、人工复核结果。

## 6. 模型版本基线

`mdl_model_version` 同时登记：

- 传统 ML/ONNX/PyTorch 模型；
- HEURISTIC 算法适配器；
- RAG embedding 模型；
- 通用大模型 provider/model 组合；
- 知识库版本。

示例：

| model_type_code | model_code | impl_type |
| --- | --- | --- |
| `GRADING` | `caries-grading-heuristic-v1` | `HEURISTIC` |
| `RAG` | `caries-rag-kb-v1` | `RAG` |
| `LLM` | `general-llm-provider-model` | `GENERAL_LLM` |

## 7. 审计要求

- callback payload 必须可回放；
- RAG 回答必须能追踪到检索 chunk；
- 失败结果必须保留错误码和错误消息；
- 高 uncertainty 必须保留 `needsReview=true`；
- 所有时间字段采用数据库统一时间策略。
