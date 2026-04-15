# Python联调与AI协作说明

更新日期：2026-04-15

本文档定义 Java 后端与 Python AI 服务当前真实协作契约。

## 1. 联调结论

1. Java 通过 RabbitMQ 发布 `AiAnalysisRequestDTO`。
2. Python 通过 `images[].accessUrl` 拉取影像，`accessUrl` 当前为 MinIO 预签名 GET URL。
3. Java 当前长期对象存储口径为 MinIO，统一配置为 `caries.storage`。
4. Python 回调 Java 使用 `AiAnalysisResultCallbackCommand`。
5. Python 必须回传 `modelVersion`、`traceId`、`inferenceMillis`，用于审计和看板统计。
6. Python 如生成热力图、mask、overlay，推荐先上传到 `caries-visual`，再在回调的 `visualAssets` 中返回 `bucketName + objectKey`。
7. 回调接口具备幂等语义，重复终态回调不会重复写结果。

## 2. Java -> Python 请求

类名：`AiAnalysisRequestDTO`

```json
{
  "taskNo": "TASK202604151000000001",
  "taskTypeCode": "INFERENCE",
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
      "objectKey": "case-image/2026/04/15/40001/image.jpg",
      "storageProviderCode": "MINIO",
      "attachmentMd5": "f1d2d2f924e986ac86fdf7b36c94bcdf",
      "accessUrl": "http://127.0.0.1:9000/caries-image/case-image/2026/04/15/40001/image.jpg?X-Amz-Algorithm=...",
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
| `images[].bucketName` | 原图所在 bucket，当前通常为 `caries-image` |
| `images[].objectKey` | 原图对象 key |
| `images[].storageProviderCode` | 当前为 `MINIO` |
| `images[].attachmentMd5` | 文件 MD5，用于校验 |
| `images[].accessUrl` | 推荐取图入口，MinIO 预签名 GET URL |
| `images[].accessExpireAt` | URL 过期时间，秒级时间戳 |
| `images[].localStoragePath` | 当前 MinIO 模式固定为空，不要依赖本地路径 |

## 3. Python 拉图规则

1. 优先请求 `accessUrl`。
2. 若 `accessExpireAt` 已过期，不要继续推理，应返回失败或重新请求任务。
3. 不要把 `bucketName + objectKey` 拼成本地路径。
4. 拉图失败时回调 `taskStatusCode=FAILED` 和 `errorMessage`。
5. 如 Python 自己使用 MinIO SDK，凭据由 Docker/部署环境提供，不从 Java 消息中获取。

## 4. Python -> Java 回调

接口：`POST /api/v1/internal/ai/callbacks/analysis-result`

请求体：`AiAnalysisResultCallbackCommand`

```json
{
  "taskNo": "TASK202604151000000001",
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
  "visualAssets": [
    {
      "assetTypeCode": "HEATMAP",
      "bucketName": "caries-visual",
      "objectKey": "visual/2026/04/15/TASK202604151000000001/heatmap.png",
      "contentType": "image/png",
      "relatedImageId": 30001,
      "toothCode": "16",
      "fileSizeBytes": 20480,
      "md5": "8c7dd922ad47494fc02c388e12c00eac"
    }
  ],
  "riskAssessment": {
    "overallRiskLevelCode": "MEDIUM",
    "assessmentReportJson": {
      "score": 62
    },
    "recommendedCycleDays": 90
  },
  "errorMessage": null,
  "traceId": "py-94f1c9",
  "inferenceMillis": 3021,
  "uncertaintyScore": 0.18
}
```

`visualAssets` 支持两种写法：

| 写法 | 适用场景 | Java 行为 |
| --- | --- | --- |
| `attachmentId` | 可视化附件已经由 Java 侧登记 | 直接写 `ana_visual_asset.attachment_id` |
| `bucketName + objectKey` | Python 已把可视化文件上传到 MinIO | Java 自动写 `med_attachment`，再写 `ana_visual_asset` |

字段要求：

| 字段 | 要求 |
| --- | --- |
| `taskNo` | 必填 |
| `taskStatusCode` | 必填，支持 `PROCESSING`、`SUCCESS`、`FAILED` |
| `modelVersion` | 成功时必须回传实际模型版本 |
| `summary` | 成功时建议提供 |
| `rawResultJson` | 成功时建议完整保留 |
| `visualAssets[].assetTypeCode` | 可视化资产存在时必填，例如 `HEATMAP`、`MASK`、`OVERLAY` |
| `visualAssets[].attachmentId` | 与 `bucketName/objectKey` 二选一 |
| `visualAssets[].bucketName`、`visualAssets[].objectKey` | 与 `attachmentId` 二选一 |
| `visualAssets[].relatedImageId` | 建议填写，便于原图追溯 |
| `visualAssets[].toothCode` | 有牙位定位时填写 |
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
| `visualAssets[].bucketName/objectKey` | `med_attachment.bucket_name/object_key` |
| `visualAssets[].attachmentId` | `ana_visual_asset.attachment_id` |
| `visualAssets[].relatedImageId` | `ana_visual_asset.related_image_id` |
| `visualAssets[].toothCode` | `ana_visual_asset.tooth_code` |
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
  storage:
    provider: MINIO
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
    default-presign-expire-seconds: 900
    buckets:
      image: caries-image
      visual: caries-visual
      report: caries-report
      export: caries-export
```

Python 容器推荐环境变量：

```env
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_VISUAL=caries-visual
```

Python 不需要知道 MinIO 密钥也可以通过 `accessUrl` 拉原图。若 Python 要上传 visual asset，则必须使用 MinIO SDK 或调用后续内部上传接口，将结果写入 `caries-visual`。