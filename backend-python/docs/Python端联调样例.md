# Python 端联调样例

## RabbitMQ 分析请求示例

```json
{
  "traceId": "trace-202604150001",
  "taskNo": "TASK202604150001",
  "caseId": 10001,
  "caseNo": "CASE202604150001",
  "patientId": 70001,
  "orgId": 1001,
  "modelVersion": "caries-v1",
  "images": [
    {
      "imageId": 90001,
      "imageTypeCode": "PANORAMIC",
      "bucketName": "caries-image",
      "objectKey": "org/1001/case/CASE202604150001/image/PANORAMIC/2026/04/15/90001/pan_01.jpg",
      "originalFilename": "pan_01.jpg"
    }
  ],
  "patientProfile": {
    "age": 28,
    "genderCode": "M",
    "previousCariesCount": 1
  },
  "requestedAt": "2026-04-15T10:00:00"
}
```

## 成功回调示例

```json
{
  "taskNo": "TASK202604150001",
  "taskStatusCode": "SUCCESS",
  "startedAt": "2026-04-15T10:00:01",
  "completedAt": "2026-04-15T10:00:03",
  "modelVersion": "caries-v1",
  "summary": {
    "overallHighestSeverity": "C1",
    "uncertaintyScore": 0.1,
    "reviewSuggestedFlag": "0",
    "teethCount": 2
  },
  "rawResultJson": {
    "pipelineVersion": "mock-1",
    "mode": "mock",
    "note": "mock visual assets are for integration verification only"
  },
  "visualAssets": [
    {
      "assetTypeCode": "MASK",
      "bucketName": "caries-visual",
      "objectKey": "org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/MASK/90001/16/91001.png",
      "contentType": "image/png",
      "relatedImageId": 90001,
      "toothCode": "16",
      "fileSizeBytes": 18342,
      "md5": "mock-md5"
    }
  ],
  "riskAssessment": {
    "overallRiskLevelCode": "LOW",
    "assessmentReportJson": {
      "source": "mock"
    },
    "recommendedCycleDays": 180
  },
  "errorMessage": null,
  "traceId": "trace-202604150001",
  "inferenceMillis": 1200,
  "uncertaintyScore": 0.1
}
```

## 失败回调示例

```json
{
  "taskNo": "TASK202604150001",
  "taskStatusCode": "FAILED",
  "startedAt": "2026-04-15T10:00:01",
  "completedAt": "2026-04-15T10:00:01",
  "modelVersion": "caries-v1",
  "summary": null,
  "rawResultJson": {
    "errorCode": "A0404",
    "errorType": "ResourceNotFoundException"
  },
  "visualAssets": [],
  "riskAssessment": null,
  "errorMessage": "image 90001 has no bucketName/objectKey or accessUrl",
  "traceId": "trace-202604150001",
  "inferenceMillis": 0,
  "uncertaintyScore": null
}
```
