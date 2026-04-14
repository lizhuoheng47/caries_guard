# Python联调与AI协作说明

本文档只描述当前 Java 后端已经实现并可供 Python 侧对接的部分。

## 1. Python 侧在当前架构中的位置

当前 Java 后端已经承担：
- 患者/病例/图像业务管理
- 分析任务入库
- 分析任务事件发布
- AI 回调验签与写库
- 报告生成
- 随访触发
- 看板统计

Python / AI 服务当前应承担：
- 消费分析请求事件
- 读取图像对象
- 执行分析模型推理
- 生成摘要、风险评估、可视化产物
- 回调 Java 后端写回结果

当前仓库中没有 Python 消费端实现，也没有 Python 回调客户端实现。

## 2. 分析请求从哪里来

Java 侧创建任务入口：
- `AnalysisTaskAppService.createTask`
- `AnalysisTaskAppService.retryTask`

创建任务后，Java 会发布 `AnalysisRequestedEvent`。

事件结构定义：
- 类：`AnalysisRequestedEvent`
- 字段：`taskId` `taskNo` `taskStatusCode` `payloadJson`

## 3. 当前消息模式

### 3.1 本地 local profile

当前真实配置：
- `caries.analysis.messaging.mode: rabbit`

也就是说：
- 本地默认不是 logging 模式
- 任务创建后会真正投到 RabbitMQ

### 3.2 E2E profile

`application-e2e.yml` 中会切到：
- `caries.analysis.messaging.mode: logging`

用途：
- 测试场景下不依赖真实 Rabbit 消费端

## 4. RabbitMQ 配置

当前配置来源：
- `AnalysisMessagingProperties`
- `application-local.yml`

关键配置：
- exchange: `caries.analysis.exchange`
- requested queue: `caries.analysis.requested.queue`
- completed queue: `caries.analysis.completed.queue`
- failed queue: `caries.analysis.failed.queue`
- requested routing key: `analysis.requested`
- completed routing key: `analysis.completed`
- failed routing key: `analysis.failed`

当前 Python 消费端至少要订阅：
- exchange `caries.analysis.exchange`
- routing key `analysis.requested`

## 5. `analysis.requested` 消息内容

Java 侧发送逻辑：
- `RabbitAnalysisTaskEventPublisher.publishRequested`

消息头：
- `eventType = analysis.requested`
- `taskId`
- `taskNo`
- `taskStatusCode`

消息体：
- `payloadJson`
- 真实内容来自 `AiAnalysisRequestDTO`

`AiAnalysisRequestDTO` 结构：

```json
{
  "taskNo": "ANA...",
  "taskTypeCode": "INFERENCE",
  "caseId": 1,
  "patientId": 1,
  "orgId": 100001,
  "modelVersion": "caries-v1",
  "images": [
    {
      "imageId": 1,
      "attachmentId": 1,
      "imageTypeCode": "PANORAMIC",
      "bucketName": "caries-image",
      "objectKey": "attachments/2026/04/15/xxxxxxxx.jpg",
      "storageProviderCode": "MINIO",
      "attachmentMd5": "...",
      "accessUrl": "http://127.0.0.1:8080/api/v1/files/1/content?expireAt=...&signature=...",
      "accessExpireAt": 1770000000,
      "localStoragePath": null
    }
  ],
  "patientProfile": {
    "age": 8,
    "genderCode": "MALE"
  }
}
```

字段说明：
- `taskNo` 是 Python 回调时最关键的关联键。
- `modelVersion` 默认来自 `caries.analysis.default-model-version`，默认为 `caries-v1`。
- `images` 只包含质检通过的图像。
- `accessUrl` / `accessExpireAt` 由 Java 通过 `AttachmentAppService.createInternalAccessUrl` 生成，Python 推荐直接 HTTP 拉图。
- `storageProviderCode` 当前支持 `MINIO` 和 `LOCAL_FS`；`OSS` 只是预留口径，未实现。
- `attachmentMd5` 用于 Python 校验输入文件。
- `localStoragePath` 只在 `LOCAL_FS` provider 下生成，用于同机/共享卷调试。

## 6. Python 如何取图

推荐方式：
1. Python 优先使用 `accessUrl` 拉取图片。
2. 在 `LOCAL_FS` 本地共享卷联调时，可使用 `localStoragePath` 直接读取。
3. 不建议 Python 自己拼 `bucketName + objectKey` 访问 MinIO，因为 Java 已经冻结了签名 URL 契约。

当前对象存储实现：
- `provider-code=MINIO`：`MinioObjectStorageService`，local profile 默认值。
- `provider-code=LOCAL_FS`：`LocalObjectStorageService`，E2E profile 使用。
- `application-local.yml` 已包含 MinIO endpoint、access key、secret key、bucket、public base URL 等配置项。

注意：
- MinIO Java provider 已经实现，但 MinIO 服务实例不在本仓库内，需要运行环境提供。
- Python 如果通过 `accessUrl` 拉图，需要能访问 `caries.image.storage.public-base-url` 指向的 Java 后端地址。

## 7. Python 回调接口

回调地址：
- `POST /api/v1/internal/ai/callbacks/analysis-result`

Java 处理入口：
- `AnalysisCallbackController.receiveAnalysisResultCallback`
- `AnalysisCallbackAppService.handleResultCallback`

请求头要求：
- `timestamp`
- `signature`

请求体结构：

```json
{
  "taskNo": "ANA...",
  "taskStatusCode": "SUCCESS",
  "startedAt": "2026-04-15T10:00:00",
  "completedAt": "2026-04-15T10:00:10",
  "modelVersion": "caries-v1",
  "summary": {
    "overallHighestSeverity": "HIGH",
    "uncertaintyScore": 0.12,
    "reviewSuggestedFlag": "1",
    "teethCount": 20
  },
  "rawResultJson": {},
  "visualAssets": [
    {
      "assetTypeCode": "HEATMAP",
      "attachmentId": 123
    }
  ],
  "riskAssessment": {
    "overallRiskLevelCode": "HIGH",
    "assessmentReportJson": {},
    "recommendedCycleDays": 30
  },
  "errorMessage": null
}
```

当前支持的 `taskStatusCode`：
- `PROCESSING`
- `SUCCESS`
- `FAILED`

## 8. 回调签名算法

Java 实现类：
- `AiCallbackSignatureVerifier`

算法规则：
1. 取原始请求体字符串 `rawBody`
2. 取请求头 `timestamp`
3. 拼接字符串：`timestamp + "." + rawBody`
4. 使用共享密钥做 `HmacSHA256`
5. 对摘要做 `Base64 URL Safe` 编码，且不带 padding
6. 结果放到请求头 `signature`

伪代码：

```python
import base64
import hashlib
import hmac

payload = f"{timestamp}.{raw_body}".encode("utf-8")
digest = hmac.new(secret.encode("utf-8"), payload, hashlib.sha256).digest()
signature = base64.urlsafe_b64encode(digest).decode("utf-8").rstrip("=")
```

额外校验：
- `timestamp` 必须是 Unix epoch seconds
- 与 Java 服务器当前时间偏差不能超过 `callbackAllowedClockSkewSeconds`
- 当前默认允许偏差 `300` 秒

## 9. 不同回调状态的效果

### 9.1 `PROCESSING`

Java 侧行为：
- 仅更新任务状态为 `PROCESSING`
- 不写摘要、不写风险、不推动报告链路

### 9.2 `SUCCESS`

Java 侧行为：
- 更新任务状态为 `SUCCESS`
- 写 `ana_result_summary`
- 写 `ana_visual_asset`
- 写 `med_risk_assessment_record`
- 推动病例到 `REVIEW_PENDING`
- 发布 `analysis.completed`

### 9.3 `FAILED`

Java 侧行为：
- 更新任务状态为 `FAILED`
- 记录错误信息
- 推动病例回到 `QC_PENDING`
- 发布 `analysis.failed`

## 10. 幂等与晚到回调

当前 Java 已实现这两类保护：

1. 重复终态回调
- 如果同一任务已经是终态，再收到相同终态回调，会直接 ACK

2. 已重试任务的晚到回调
- 如果原任务已经被重试，旧任务的晚到回调只 ACK，不再写库

这意味着 Python 侧可以安全地做有限次重试，但必须：
- 始终使用同一个 `taskNo` 回调对应任务
- 不要把新旧任务的 `taskNo` 混用

## 11. Python 生成可视化资产的要求

如果 Python 想让 Java 记录 `visualAssets`：
- 必须先确保资产文件已经作为附件存在于 Java 侧
- 回调里填写的是 `attachmentId`
- Java 不负责替 Python 上传可视化结果文件

现实落地方式通常有两种：
1. Python 先调 Java 文件上传接口，把标注图上传成附件，再回调 attachmentId
2. 后续单独扩展内部上传通道

## 12. Python 侧最小可用联调方案

最小闭环建议：

1. Java 使用本地 profile 启动
2. Python 监听 Rabbit `analysis.requested`
3. Python 通过共享文件目录读取图片
4. Python 做假推理也可以，只要能生成固定结果
5. Python 回调 `SUCCESS`
6. Java 生成报告
7. 风险为 `HIGH` 或 `reviewSuggestedFlag=1` 时观察随访触发

## 13. 当前联调限制总结

1. 当前仓库没有 Python 示例代码。
2. 当前请求消息不包含签名下载 URL，只包含 bucket/objectKey。
3. 当前存储是本地文件系统，不是对象存储服务。
4. 当前报告 PDF 生成是极简实现，只适合闭环验证。
5. 当前导出接口只写日志，不直接下载文件。

