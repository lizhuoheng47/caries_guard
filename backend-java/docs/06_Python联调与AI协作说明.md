# Python联调与AI协作说明

更新日期：2026-04-15

本文档定义 Java 后端与 Python AI 服务当前真实协作契约。

## 1. 联调结论

1. Java 通过 RabbitMQ 发布 `AiAnalysisRequestDTO`。
2. Python 通过 `images[].accessUrl` 拉取影像，MinIO 场景不需要共享本地卷。
3. Java 默认对象存储是 MinIO，`LOCAL_FS` 只用于 e2e 或受控本地兼容场景。
4. Python 回调 Java 使用 `AiAnalysisResultCallbackCommand`。
5. Python 必须回传 `modelVersion`、`traceId`、`inferenceMillis`，用于审计和看板统计。
6. 回调接口具备幂等语义，重复终态回调不会重复写结果。

## 2. Java -> Python 请求

类名：`AiAnalysisRequestDTO`

```json
{
  "taskNo": "ANA202604150001",
  "taskTypeCode": "CARIES_DETECTION",
  "caseId": 10001,
  "patientId": 20001,
  "orgId": 100001,
  "modelVersion": "caries-v1",
  "images": [
    {
      "imageId": 30001,
      "attachmentId": 40001,
      "imageTypeCode": "INTRAORAL",
      "bucketName": "caries-image",
      "objectKey": "attachments/2026/04/15/xxxx.jpg",
      "storageProviderCode": "MINIO",
      "attachmentMd5": "f1d2d2f924e986ac86fdf7b36c94bcdf",
      "accessUrl": "http://127.0.0.1:8080/api/v1/files/40001/content?expireAt=...&signature=...",
      "accessExpireAt": 1776220000,
      "localStoragePath": null
    }
  ],
  "patientProfile": {
    "age": 12,
    "genderCode": "FEMALE"
  }
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `taskNo` | 任务编号，回调时必须原样返回 |
| `modelVersion` | Java 请求模型版本 |
| `images[].storageProviderCode` | `MINIO` 或 `LOCAL_FS` |
| `images[].attachmentMd5` | 文件 MD5，用于校验 |
| `images[].accessUrl` | 推荐取图入口 |
| `images[].accessExpireAt` | URL 过期时间，秒级时间戳 |
| `images[].localStoragePath` | 只有 LOCAL_FS 受控环境才可能有值 |

## 3. Python 拉图规则

1. 优先请求 `accessUrl`。
2. 若 `accessExpireAt` 已过期，不要继续推理，应返回失败或重新请求任务。
3. MinIO 场景不要把 `bucketName + objectKey` 拼成本地路径。
4. LOCAL_FS 场景如 Java 下发 `localStoragePath`，只能在共享卷/本机受控环境使用。
5. 拉图失败时回调 `taskStatusCode=FAILED` 和 `errorMessage`。

## 4. Python -> Java 回调

接口：`POST /api/v1/internal/ai/callbacks/analysis-result`

请求体：`AiAnalysisResultCallbackCommand`

```json
{
  "taskNo": "ANA202604150001",
  "taskStatusCode": "SUCCESS",
  "startedAt": "2026-04-15T10:00:00",
  "completedAt": "2026-04-15T10:00:03",
  "modelVersion": "caries-v1",
  "summary": {
    "overallHighestSeverity": "C2",
    "uncertaintyScore": 0.18,
    "reviewSuggestedFlag": "0",
    "teethCount": 24
  },
  "rawResultJson": {
    "lesions": []
  },
  "visualAssets": [],
  "riskAssessment": {
    "riskLevelCode": "MEDIUM",
    "riskScore": 0.62,
    "riskFactorsJson": "{}",
    "suggestion": "建议复查",
    "nextFollowupDays": 90
  },
  "errorMessage": null,
  "traceId": "py-94f1c9",
  "inferenceMillis": 3021,
  "uncertaintyScore": 0.18
}
```

字段要求：

| 字段 | 要求 |
| --- | --- |
| `taskNo` | 必填 |
| `taskStatusCode` | 必填，建议 `PROCESSING`、`SUCCESS`、`FAILED` |
| `modelVersion` | 成功时必须回传实际模型版本 |
| `summary` | 成功时建议提供 |
| `rawResultJson` | 成功时建议完整保留 |
| `visualAssets` | 有可视化结果时提供 |
| `riskAssessment` | 有风险评估时提供 |
| `traceId` | 建议必填，便于排查 |
| `inferenceMillis` | 建议必填，dashboard 统计使用 |
| `errorMessage` | 失败时必填 |

## 5. Java 落库结果

| 回调字段 | 落库位置 |
| --- | --- |
| `taskStatusCode` | `ana_task_record.task_status_code` |
| `modelVersion` | `ana_task_record.model_version` |
| `traceId` | `ana_task_record.trace_id` |
| `inferenceMillis` | `ana_task_record.inference_millis` |
| `summary` | `ana_result_summary.summary_json` 和聚合字段 |
| `rawResultJson` | `ana_result_summary.raw_result_json` |
| `visualAssets` | `ana_visual_asset` |
| `riskAssessment` | `med_risk_assessment_record` |

## 6. 幂等和重试

1. 同一个 `SUCCESS` 回调重复发送，Java 不重复写 summary、risk、visualAssets。
2. 失败任务重试后，旧任务的晚到成功回调不能覆盖新任务。
3. `FAILED` 回调会保留错误原因。
4. 需要 Python 侧保证 `taskNo` 不被改写。

## 7. MinIO 联调配置

Java local 默认：

```yaml
caries:
  image:
    storage:
      provider-code: MINIO
      public-base-url: http://127.0.0.1:8080
      minio:
        endpoint: http://127.0.0.1:9000
        access-key: minioadmin
        secret-key: minioadmin
```

Python 不需要知道 MinIO 密钥也可以通过 `accessUrl` 拉图。若 Python 选择使用 MinIO SDK，应由部署环境单独提供 MinIO 连接信息。