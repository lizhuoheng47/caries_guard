# 04 API 接口清单

## 1. Analysis

| 接口 | 说明 |
| --- | --- |
| `POST /api/v1/analysis/tasks` | 创建分析任务 |
| `GET /api/v1/analysis/tasks/{taskNo}` | 查询任务 |
| `POST /api/v1/internal/ai/callbacks/analysis-result` | Python callback |

## 2. Report

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/reports/{id}` | 报告详情 |
| `POST /api/v1/reports` | 创建或生成报告 |
| `POST /api/v1/reports/{id}/confirm` | 确认报告 |

## 3. Review

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/reviews/pending` | 待复核 |
| `POST /api/v1/reviews/{id}/submit` | 提交复核 |

## 4. RAG

| 接口 | 说明 |
| --- | --- |
| `POST /api/v1/rag/ask` | 业务侧问答入口 |

Java 将用户、角色、病例上下文传给 Python RAG。Python 返回 answer、citations、retrievedChunks、safetyFlags。

## 5. Internal Callback 要求

Callback 必须：

- 校验签名；
- 校验 `taskNo`；
- 幂等处理；
- 兼容 `rawResultJson` 新字段；
- 保存失败状态。

## 6. AI 字段

API 展示层可读取：

- `gradingLabel`
- `uncertaintyScore`
- `needsReview`
- `visualAssets`
- `riskAssessment`
- `citations`

不应直接暴露内部 prompt 或敏感日志。
