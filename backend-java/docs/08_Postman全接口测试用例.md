# Postman 全接口测试用例

更新日期：2026-04-15

本文档用于通过 Postman 验证 Java 后端当前所有 REST 接口是否可用。接口基于当前 `backend-java` 代码、Flyway V017、Docker profile 和 `ApiResponse<T>` 响应结构整理。

## 1. 测试基准

### 1.1 启动地址

Docker 全容器运行时，Postman 在宿主机访问：

```text
{{baseUrl}} = http://127.0.0.1:8080
```

Java 本地运行时同样使用：

```text
{{baseUrl}} = http://127.0.0.1:8080
```

### 1.2 默认账号

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 1.3 通用 Header

除登录、ping、文件签名下载、AI 内部回调外，其他接口都需要：

```text
Authorization: Bearer {{token}}
Content-Type: application/json
```

文件上传接口使用：

```text
Authorization: Bearer {{token}}
Content-Type: multipart/form-data
```

### 1.4 统一成功响应结构

所有 `ApiResponse<T>` 成功响应都应满足：

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "traceId": "...",
  "timestamp": "..."
}
```

Postman 通用 Tests 脚本：

```javascript
pm.test("HTTP 2xx", function () {
  pm.expect(pm.response.code).to.be.within(200, 299);
});
const json = pm.response.json();
pm.test("business success", function () {
  pm.expect(json.code).to.eql("00000");
});
```

## 2. Postman 环境变量

建议新建环境 `caries-guard-docker`，预设以下变量：

| 变量 | 初始值 | 说明 |
| --- | --- | --- |
| `baseUrl` | `http://127.0.0.1:8080` | Java 后端宿主机地址 |
| `token` | 空 | 登录后自动写入 |
| `patientId` | 空 | 创建患者后写入 |
| `visitId` | 空 | 创建就诊后写入 |
| `caseId` | 空 | 创建病例后写入 |
| `attachmentId` | 空 | 上传原图后写入 |
| `visualAssetAttachmentId` | 空 | 上传 AI 可视化资产后写入 |
| `imageId` | 空 | 创建病例影像后写入 |
| `taskId` | 空 | 创建分析任务后写入 |
| `taskNo` | 空 | 创建分析任务后写入 |
| `feedbackId` | 空 | 提交修正后写入 |
| `templateId` | 空 | 创建报告模板后写入 |
| `reportId` | 空 | 生成报告后写入 |
| `downloadUrl` | 空 | 报告导出后写入 |
| `planId` | 空 | 创建/查询随访计划后写入 |
| `followupTaskId` | 空 | 创建/查询随访任务后写入 |
| `recordId` | 空 | 新增随访记录后写入 |
| `roleId` | `100101` | V014 种子 ORG_ADMIN |
| `menuId` | `200001` | V014 种子患者管理菜单 |
| `ruleId` | 空 | 创建数据权限规则后写入 |
| `callbackSecret` | `docker-change-me-to-a-strong-analysis-callback-secret` | Docker profile 默认回调密钥 |

## 3. 建议执行顺序

1. 系统健康检查。
2. 登录并保存 `token`。
3. 查询当前用户和权限。
4. 创建报告模板。
5. 创建患者。
6. 创建就诊。
7. 创建病例。
8. 上传原始影像文件。
9. 创建病例影像。
10. 保存影像质检。
11. 创建 AI 分析任务。
12. 上传视觉资产文件。
13. 模拟 AI 回调成功。
14. 提交医生修正。
15. 生成报告。
16. 导出报告并下载。
17. 查询随访任务，新增随访记录。
18. 查询 Dashboard。
19. 测试系统管理接口。

## 4. 认证与公共接口

### 4.1 健康检查

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/ping` |
| Auth | 无 |
| Body | 无 |

预期输出：`code=00000`，`data.pong=true`，`data.app=caries-guard-backend`。

### 4.2 Actuator 健康检查

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/actuator/health` |
| Auth | 无 |
| Body | 无 |

预期输出：HTTP 200，`status` 为 `UP` 或包含组件健康状态。

### 4.3 登录

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/auth/login` |
| Auth | 无 |
| Body | JSON |

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

预期输出：`code=00000`，`data.token` 有值，`data.user.username=admin`，`data.user.orgId=100001`。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.expect(json.code).to.eql("00000");
pm.environment.set("token", json.data.token);
```

### 4.4 当前用户

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/auth/me` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.userId=100001`，`data.username=admin`，`data.roles` 包含 `SYS_ADMIN`。

### 4.5 当前权限

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/auth/permissions` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.roles` 包含 `SYS_ADMIN`，`data.permissions` 为权限列表。

## 5. 报告模板接口

### 5.1 创建报告模板

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/report-templates` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "templateCode": "POSTMAN_DOCTOR_{{$timestamp}}",
  "templateName": "Postman医生报告模板",
  "reportTypeCode": "DOCTOR",
  "templateContent": "Report No: {{reportNo}}\nCase No: {{caseNo}}\nHighest Severity: {{highestSeverity}}\nRisk Level: {{riskLevelCode}}\nDoctor Conclusion: {{doctorConclusion}}",
  "status": "ACTIVE",
  "remark": "postman test"
}
```

预期输出：`data.templateId` 有值，`data.templateCode` 为请求中的模板编码。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("templateId", json.data.templateId);
```

### 5.2 查询报告模板列表

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/report-templates?reportTypeCode=DOCTOR` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组，包含刚创建的模板。

### 5.3 查询报告模板详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/report-templates/{{templateId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.templateId={{templateId}}`。

### 5.4 更新报告模板

| 项 | 内容 |
| --- | --- |
| Method | `PUT` |
| URL | `{{baseUrl}}/api/v1/report-templates/{{templateId}}` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "templateName": "Postman医生报告模板-已更新",
  "templateContent": "Report No: {{reportNo}}\nCase No: {{caseNo}}\nDoctor Conclusion: {{doctorConclusion}}\nUpdated by Postman",
  "status": "ACTIVE",
  "remark": "postman update"
}
```

预期输出：`data.templateId={{templateId}}`，`data.versionNo` 为有效版本号。

## 6. 患者接口

### 6.1 创建患者

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/patients` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "patientName": "测试患者{{$timestamp}}",
  "genderCode": "FEMALE",
  "birthDate": "2016-05-20",
  "phone": "13900001234",
  "idCardNo": "330101201605201234",
  "sourceCode": "OUTPATIENT",
  "firstVisitDate": "2026-04-15",
  "privacyLevelCode": "L4",
  "status": "ACTIVE",
  "remark": "postman patient",
  "guardian": {
    "guardianName": "测试家长{{$timestamp}}",
    "relationCode": "PARENT",
    "phone": "13800001234",
    "certificateTypeCode": "ID_CARD",
    "certificateNo": "330101198812121234",
    "primaryFlag": "1",
    "status": "ACTIVE",
    "remark": "postman guardian"
  }
}
```

预期输出：`data.patientId`、`data.patientNo` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("patientId", json.data.patientId);
```

### 6.2 更新患者

| 项 | 内容 |
| --- | --- |
| Method | `PUT` |
| URL | `{{baseUrl}}/api/v1/patients/{{patientId}}` |
| Auth | Bearer Token |
| Body | JSON |

请求体与创建患者一致，可修改 `patientName`、`phone`、`guardian`。预期输出：`data.patientId={{patientId}}`。

### 6.3 查询患者详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/patients/{{patientId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.patientId={{patientId}}`，姓名和手机号为脱敏字段。

### 6.4 分页查询患者

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/patients?pageNo=1&pageSize=10&status=ACTIVE` |
| Auth | Bearer Token |
| Body | 无 |

可选参数：`keyword`、`sourceCode`、`status`。预期输出：`data.records` 为数组，`data.total>=1`。

## 7. 就诊接口

### 7.1 创建就诊

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/visits` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "patientId": {{patientId}},
  "departmentId": 100001,
  "doctorUserId": 100001,
  "visitTypeCode": "OUTPATIENT",
  "visitDate": "2026-04-15T10:00:00",
  "complaint": "牙痛，龋齿筛查",
  "triageLevelCode": "NORMAL",
  "sourceChannelCode": "MANUAL",
  "status": "ACTIVE",
  "remark": "postman visit"
}
```

预期输出：`data.visitId`、`data.visitNo` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("visitId", json.data.visitId);
```

### 7.2 查询就诊详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/visits/{{visitId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.visitId={{visitId}}`。

### 7.3 分页查询就诊

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/visits?pageNo=1&pageSize=10&patientId={{patientId}}` |
| Auth | Bearer Token |
| Body | 无 |

可选参数：`patientId`、`doctorUserId`、`visitTypeCode`。预期输出：`data.records` 为数组。

## 8. 病例接口

### 8.1 创建病例

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "visitId": {{visitId}},
  "patientId": {{patientId}},
  "caseTypeCode": "CARIES_SCREENING",
  "caseTitle": "Postman龋齿筛查病例",
  "chiefComplaint": "龋齿筛查",
  "priorityCode": "NORMAL",
  "clinicalNotes": "口腔检查",
  "onsetDate": "2026-04-15",
  "status": "CREATED",
  "remark": "postman case"
}
```

预期输出：`data.caseId` 有值，`data.caseStatusCode=CREATED`。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("caseId", json.data.caseId);
```

### 8.2 查询病例详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.caseId={{caseId}}`。

### 8.3 分页查询病例

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases?pageNo=1&pageSize=10&patientId={{patientId}}` |
| Auth | Bearer Token |
| Body | 无 |

可选参数：`patientId`、`caseStatusCode`、`attendingDoctorId`。预期输出：`data.records` 为数组。

### 8.4 保存诊断

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/diagnoses` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "diagnoses": [
    {
      "diagnosisTypeCode": "PRIMARY",
      "diagnosisName": "龋齿",
      "severityCode": "C2",
      "diagnosisBasis": "影像提示疑似龋坏",
      "diagnosisDesc": "后牙邻面龋坏",
      "treatmentAdvice": "建议复查并制定治疗计划",
      "finalFlag": "1",
      "remark": "postman diagnosis"
    }
  ]
}
```

预期输出：`data.caseId={{caseId}}`。

### 8.5 保存牙位记录

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/tooth-records` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "toothRecords": [
    {
      "sourceImageId": {{imageId}},
      "toothCode": "16",
      "toothSurfaceCode": "OCCLUSAL",
      "issueTypeCode": "CARIES",
      "severityCode": "C2",
      "findingDesc": "咬合面疑似龋坏",
      "suggestion": "建议医生复核",
      "sortOrder": 1,
      "remark": "postman tooth record"
    }
  ]
}
```

如果尚未创建 `imageId`，可先跳过，等影像接口执行后再测。预期输出：`data.caseId={{caseId}}`。

### 8.6 病例状态流转

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/status-transition` |
| Auth | Bearer Token |
| Body | JSON |

请求体示例：

```json
{
  "targetStatusCode": "CLOSED",
  "reasonCode": "MANUAL_CLOSE",
  "reasonRemark": "postman manual close"
}
```

预期输出：`data.caseId={{caseId}}`，`data.toStatusCode=CLOSED`。注意：状态机有合法流转限制，如果当前状态不允许关闭，预期会返回业务失败。

## 9. 文件、影像、质检接口

### 9.1 上传附件

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/files/upload` |
| Auth | Bearer Token |
| Body | `form-data` |

Body：

| key | type | value |
| --- | --- | --- |
| `file` | File | 选择一张 jpg/png 图片 |

预期输出：`data.attachmentId`、`data.bucketName`、`data.objectKey`、`data.md5` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("attachmentId", json.data.attachmentId);
```

### 9.2 获取附件访问 URL

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/files/{{attachmentId}}/access-url` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.accessUrl`、`data.expireAt` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("downloadUrl", json.data.accessUrl);
```

### 9.3 下载附件内容

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{downloadUrl}}` |
| Auth | 无 |
| Body | 无 |

预期输出：HTTP 200，响应体为文件二进制，`Content-Type` 与上传文件一致或为 `application/octet-stream`。

### 9.4 创建病例影像

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/images` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "attachmentId": {{attachmentId}},
  "visitId": {{visitId}},
  "patientId": {{patientId}},
  "imageTypeCode": "PANORAMIC",
  "imageSourceCode": "UPLOAD",
  "shootingTime": "2026-04-15T10:10:00",
  "bodyPositionCode": "ORAL",
  "primaryFlag": "1",
  "remark": "postman image"
}
```

预期输出：`data.imageId` 有值，`data.qualityStatusCode=PENDING`。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("imageId", json.data.imageId);
```

### 9.5 查询病例影像列表

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/images` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组，包含 `imageId={{imageId}}`。

### 9.6 查询病例影像详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/images/detail/{{imageId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.imageId={{imageId}}`。

### 9.7 保存影像质检

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/images/{{imageId}}/quality-checks` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "checkTypeCode": "AUTO",
  "checkResultCode": "PASS",
  "qualityScore": 97,
  "blurScore": 95,
  "exposureScore": 96,
  "integrityScore": 98,
  "occlusionScore": 97,
  "issueCodes": ["NONE"],
  "suggestionText": "影像质量合格",
  "remark": "postman quality check"
}
```

预期输出：`data.imageId={{imageId}}`，`data.checkResultCode=PASS`。

### 9.8 查询当前影像质检

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/images/{{imageId}}/quality-checks/current` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.checkResultCode=PASS`，`data.qualityScore=97`。

## 10. AI 分析接口

### 10.1 创建分析任务

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/analysis/tasks` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "caseId": {{caseId}},
  "patientId": {{patientId}},
  "forceRetryFlag": false,
  "taskTypeCode": "INFERENCE",
  "remark": "postman analysis task"
}
```

预期输出：`data.taskId`、`data.taskNo` 有值，`data.taskStatusCode` 通常为 `QUEUEING` 或当前任务状态。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("taskId", json.data.taskId);
pm.environment.set("taskNo", json.data.taskNo);
```

### 10.2 创建分析任务（病例入口）

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/analysis` |
| Auth | Bearer Token |
| Body | JSON |

请求体同 10.1，路径里的 `caseId` 优先。预期输出：`data.taskId`、`data.taskNo` 有值。

### 10.3 查询分析任务详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/analysis/tasks/{{taskId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.taskId={{taskId}}`，包含 `summary`、`visualAssets`、`traceId`、`inferenceMillis` 等字段。

### 10.4 分页查询分析任务

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/analysis/tasks?pageNo=1&pageSize=10&caseId={{caseId}}` |
| Auth | Bearer Token |
| Body | 无 |

可选参数：`pageNo`、`pageSize`、`caseId`、`taskStatusCode`。预期输出：`data.records` 为数组。

### 10.5 重试分析任务

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/analysis/tasks/retry` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "taskId": {{taskId}},
  "reasonCode": "MANUAL_RETRY",
  "reasonRemark": "postman retry"
}
```

预期输出：如果原任务可重试，返回新的 `data.taskId` 和 `data.taskNo`；如果原任务不是失败态，预期返回业务失败。

### 10.6 上传 AI 可视化资产附件

复用 9.1 上传接口，选择一张 png。上传成功后把返回值保存为 `visualAssetAttachmentId`：

```javascript
const json = pm.response.json();
pm.environment.set("visualAssetAttachmentId", json.data.attachmentId);
```

### 10.7 模拟 AI 成功回调

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/internal/ai/callbacks/analysis-result` |
| Auth | 无，使用 HMAC Header |
| Body | JSON raw |

Headers：

```text
Content-Type: application/json
X-AI-Timestamp: {{aiTimestamp}}
X-AI-Signature: {{aiSignature}}
```

Pre-request Script：

```javascript
const timestamp = Math.floor(Date.now() / 1000).toString();
pm.environment.set("aiTimestamp", timestamp);
const rawBody = pm.request.body.raw;
const secret = pm.environment.get("callbackSecret");
const signatureWordArray = CryptoJS.HmacSHA256(timestamp + "." + rawBody, secret);
let signature = CryptoJS.enc.Base64.stringify(signatureWordArray);
signature = signature.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
pm.environment.set("aiSignature", signature);
```

请求体：

```json
{
  "taskNo": "{{taskNo}}",
  "taskStatusCode": "SUCCESS",
  "startedAt": "2026-04-15T10:20:00",
  "completedAt": "2026-04-15T10:20:03",
  "modelVersion": "caries-v1",
  "summary": {
    "overallHighestSeverity": "C2",
    "uncertaintyScore": 0.27,
    "reviewSuggestedFlag": "1",
    "teethCount": 8
  },
  "rawResultJson": {
    "overallHighestSeverity": "C2",
    "uncertaintyScore": 0.27,
    "reviewSuggestedFlag": "1"
  },
  "visualAssets": [
    {
      "assetTypeCode": "HEATMAP",
      "bucketName": "caries-visual",
      "objectKey": "org/1001/case/CASE202604150001/analysis/{{taskNo}}/caries-v1/HEATMAP/{{imageId}}/16/91001.png",
      "contentType": "image/png",
      "relatedImageId": {{imageId}},
      "toothCode": "16",
      "fileSizeBytes": 2048,
      "md5": "postman-visual-md5"
    }
  ],
  "riskAssessment": {
    "overallRiskLevelCode": "HIGH",
    "assessmentReportJson": {
      "factor": "SUGAR"
    },
    "recommendedCycleDays": 30
  },
  "errorMessage": null,
  "traceId": "postman-ai-trace-{{$timestamp}}",
  "inferenceMillis": 3000,
  "uncertaintyScore": 0.27
}
```

预期输出：`code=00000`，`data.taskNo={{taskNo}}`，`data.taskStatusCode=SUCCESS`，首次回调通常 `data.idempotent=false`。重复发送同一回调时，仍成功，但 `data.idempotent` 可能为 `true`。

### 10.8 模拟 AI 失败回调

请求体：

```json
{
  "taskNo": "{{taskNo}}",
  "taskStatusCode": "FAILED",
  "startedAt": "2026-04-15T10:20:00",
  "completedAt": "2026-04-15T10:20:03",
  "modelVersion": "caries-v1",
  "summary": null,
  "rawResultJson": {"errorType": "MODEL_ERROR"},
  "visualAssets": [],
  "riskAssessment": null,
  "errorMessage": "postman simulated failure",
  "traceId": "postman-ai-failed-{{$timestamp}}",
  "inferenceMillis": 3000,
  "uncertaintyScore": null
}
```

使用同 10.7 的 HMAC Pre-request Script。预期输出：`data.taskStatusCode=FAILED`。

## 11. 医生修正接口

### 11.1 提交修正反馈

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/analysis/corrections` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "caseId": {{caseId}},
  "diagnosisId": null,
  "sourceImageId": {{imageId}},
  "feedbackTypeCode": "RE_GRADE",
  "originalInferenceJson": {"severity": "C2"},
  "correctedTruthJson": {
    "severity": "C1",
    "doctorConclusion": "manual downgrade"
  }
}
```

预期输出：`data.feedbackId` 有值，`data.trainingCandidateFlag=1`，`data.reviewStatusCode=PENDING`。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("feedbackId", json.data.feedbackId);
```

### 11.2 提交修正反馈（病例入口）

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/corrections` |
| Auth | Bearer Token |
| Body | JSON |

请求体同 11.1，`caseId` 会以路径变量为准。预期输出同 11.1。

## 12. 报告接口

### 12.1 生成报告

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/reports` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "reportTypeCode": "DOCTOR",
  "doctorConclusion": "医生确认，建议随访",
  "remark": "postman report"
}
```

前置条件：病例已有成功 AI 回调结果。否则预期可能返回业务失败。

预期输出：`data.reportId`、`data.reportNo` 有值，`data.reportStatusCode` 为生成完成状态。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("reportId", json.data.reportId);
```

### 12.2 查询病例报告列表

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/reports` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组，包含 `reportId={{reportId}}`。

### 12.3 查询报告详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/reports/{{reportId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.reportId={{reportId}}`，包含 `attachmentId`。

### 12.4 导出报告

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/reports/{{reportId}}/export` |
| Auth | Bearer Token |
| Body | JSON，可为空 |

请求体：

```json
{
  "exportTypeCode": "PDF",
  "exportChannelCode": "DOWNLOAD"
}
```

预期输出：`data.exported=true`，`data.exportLogId`、`data.attachmentId`、`data.downloadUrl`、`data.expireAt` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("downloadUrl", json.data.downloadUrl);
```

### 12.5 下载导出报告 PDF

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{downloadUrl}}` |
| Auth | 无 |
| Body | 无 |

预期输出：HTTP 200，响应体为 PDF 二进制，`Content-Type` 通常为 `application/pdf`。

## 13. 随访接口

### 13.1 创建随访计划

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/followup/plans` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "planTypeCode": "RISK_HIGH",
  "intervalDays": 30,
  "ownerUserId": 100001,
  "remark": "postman followup plan"
}
```

预期输出：`data.planId`、`data.planNo` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("planId", json.data.planId);
```

### 13.2 查询病例随访计划

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/followup/plans` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组。

### 13.3 查询随访计划详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/followup/plans/{{planId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.planId={{planId}}`。

### 13.4 创建随访任务

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/followup/tasks` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "planId": {{planId}},
  "taskTypeCode": "FOLLOW_CONTACT",
  "assignedToUserId": 100001,
  "dueDate": "2026-05-15",
  "remark": "postman followup task"
}
```

预期输出：`data.taskId`、`data.taskNo` 有值，`data.taskStatusCode=TODO` 或有效任务状态。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("followupTaskId", json.data.taskId);
```

### 13.5 查询病例随访任务

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/followup/tasks` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组，包含 `taskId`。

### 13.6 查询随访任务详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/followup/tasks/{{followupTaskId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.taskId={{followupTaskId}}`。

### 13.7 更新随访任务状态

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/followup/tasks/{{followupTaskId}}/status` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "targetStatusCode": "IN_PROGRESS",
  "remark": "postman start followup"
}
```

预期输出：`data.taskStatusCode=IN_PROGRESS`。状态机不允许的流转会返回业务失败。

### 13.8 分派随访任务

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/followup/tasks/{{followupTaskId}}/assign?assigneeUserId=100001` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`code=00000`，`data=null`。

### 13.9 新增随访记录

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/followup/records` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "taskId": {{followupTaskId}},
  "followupMethodCode": "PHONE",
  "contactResultCode": "REACHED",
  "followNext": false,
  "nextIntervalDays": null,
  "outcomeSummary": "随访完成，暂无进一步处理",
  "doctorNotes": "postman followup record",
  "remark": "postman record"
}
```

预期输出：`data.recordId` 有值，`data.taskId={{followupTaskId}}`。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("recordId", json.data.recordId);
```

### 13.10 查询病例随访记录

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/cases/{{caseId}}/followup/records` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组。

### 13.11 查询任务随访记录

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/followup/tasks/{{followupTaskId}}/records` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data` 为数组，包含 `recordId={{recordId}}`。

### 13.12 取消随访计划

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/followup/plans/{{planId}}/cancel` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`code=00000`，`data=null`。若计划已完成，可能返回业务失败。

### 13.13 关闭随访计划

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/followup/plans/{{planId}}/close` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`code=00000`，`data=null`。若计划已完成或不允许关闭，可能返回业务失败。

## 14. Dashboard 接口

### 14.1 概览

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/overview` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.patientCount`、`data.caseCount`、`data.analyzedCaseCount` 等数字字段存在。

### 14.2 病例状态分布

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/case-status-distribution` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `createdCount`、`qcPendingCount`、`analyzingCount`、`reviewPendingCount` 等字段。

### 14.3 风险等级分布

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/risk-level-distribution` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `highRiskCount`、`mediumRiskCount`、`lowRiskCount`。

### 14.4 随访任务摘要

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/followup-task-summary` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `todoCount`、`inProgressCount`、`doneCount`、`overdueCount`。

### 14.5 待办摘要

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/backlog-summary` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `reviewPendingCaseCount`、`todoFollowupTaskCount`、`overdueFollowupTaskCount`。

### 14.6 趋势

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/trend?rangeType=LAST_7_DAYS` |
| Auth | Bearer Token |
| Body | 无 |

自定义日期示例：

```text
{{baseUrl}}/api/v1/dashboard/trend?rangeType=CUSTOM&startDate=2026-04-01&endDate=2026-04-15
```

预期输出：`data` 为数组，包含每天的 `newCaseCount`、`analysisCompletedCount` 等。

### 14.7 模型运行质量

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/dashboard/model-runtime` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `currentModelVersion`、`recentTaskCount`、`successRate`、`averageInferenceMillis`、`highUncertaintyRate`、`reviewSuggestedRate`、`modelVersions`。

## 15. 系统管理接口

### 15.1 分页查询用户

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/users?pageNo=1&pageSize=10&status=ACTIVE` |
| Auth | Bearer Token |
| Body | 无 |

可选参数：`keyword`、`deptId`、`userTypeCode`、`status`。预期输出：`data.records` 为数组，包含 `admin` 用户。

### 15.2 查询用户详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/users/100001` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.userId=100001`，`data.username=admin`。

### 15.3 创建用户

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/system/users` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "deptId": 100001,
  "userNo": "U{{$timestamp}}",
  "username": "doctor{{$timestamp}}",
  "password": "123456",
  "realName": "Postman医生",
  "nickName": "Postman医生",
  "userTypeCode": "DOCTOR",
  "genderCode": "UNKNOWN",
  "phone": "13700001234",
  "email": "doctor{{$timestamp}}@example.com",
  "avatarUrl": null,
  "certificateTypeCode": "ID_CARD",
  "certificateNo": "330101198812121234",
  "status": "ACTIVE",
  "remark": "postman user",
  "roleIds": [100102]
}
```

预期输出：`data.userId` 有值，`data.username` 为请求用户名。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("createdUserId", json.data.userId);
```

### 15.4 更新用户

| 项 | 内容 |
| --- | --- |
| Method | `PUT` |
| URL | `{{baseUrl}}/api/v1/system/users/{{createdUserId}}` |
| Auth | Bearer Token |
| Body | JSON |

请求体同 15.3，可修改 `realName`、`nickName`、`status`、`roleIds`。预期输出：`data.userId={{createdUserId}}`。

### 15.5 查询角色列表

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/roles?status=ACTIVE` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `SYS_ADMIN`、`ORG_ADMIN`、`DOCTOR`、`SCREENER`。

### 15.6 查询角色详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/roles/{{roleId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.roleId={{roleId}}`。

### 15.7 创建角色

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/system/roles` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "roleCode": "POSTMAN_ROLE_{{$timestamp}}",
  "roleName": "Postman测试角色",
  "roleSort": 99,
  "dataScopeCode": "ORG",
  "status": "ACTIVE",
  "remark": "postman role",
  "menuIds": [200001, 200002, 200003]
}
```

预期输出：`data.roleId` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("createdRoleId", json.data.roleId);
```

### 15.8 更新角色

| 项 | 内容 |
| --- | --- |
| Method | `PUT` |
| URL | `{{baseUrl}}/api/v1/system/roles/{{createdRoleId}}` |
| Auth | Bearer Token |
| Body | JSON |

请求体同 15.7，可修改 `roleName`、`roleSort`、`menuIds`。预期输出：`data.roleId={{createdRoleId}}`。

### 15.9 查询菜单列表

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/menus?status=ACTIVE` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 V014 初始化菜单，例如 `patient:view`、`analysis:view`、`dashboard:view`。

### 15.10 查询菜单详情

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/menus/{{menuId}}` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：`data.menuId={{menuId}}`。

### 15.11 创建菜单

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/system/menus` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "parentId": 0,
  "menuName": "Postman菜单{{$timestamp}}",
  "menuTypeCode": "MENU",
  "routePath": "/postman-test-{{$timestamp}}",
  "componentPath": "postman/index",
  "permissionCode": "postman:test:{{$timestamp}}",
  "icon": "test",
  "visibleFlag": "1",
  "cacheFlag": "0",
  "orderNum": 999,
  "status": "ACTIVE",
  "remark": "postman menu"
}
```

预期输出：`data.menuId` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("createdMenuId", json.data.menuId);
```

### 15.12 更新菜单

| 项 | 内容 |
| --- | --- |
| Method | `PUT` |
| URL | `{{baseUrl}}/api/v1/system/menus/{{createdMenuId}}` |
| Auth | Bearer Token |
| Body | JSON |

请求体同 15.11，可修改 `menuName`、`routePath`、`permissionCode`。预期输出：`data.menuId={{createdMenuId}}`。

### 15.13 查询数据权限规则

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/data-permission-rules?roleId={{roleId}}` |
| Auth | Bearer Token |
| Body | 无 |

可选参数：`moduleCode`。预期输出：`data` 为数组，V014 种子角色应有规则。

### 15.14 创建数据权限规则

建议使用新创建的 `createdRoleId`，避免和 V014 种子规则唯一键冲突。

| 项 | 内容 |
| --- | --- |
| Method | `POST` |
| URL | `{{baseUrl}}/api/v1/system/data-permission-rules` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "roleId": {{createdRoleId}},
  "moduleCode": "POSTMAN",
  "scopeTypeCode": "ORG",
  "customDeptIds": [],
  "columnMaskPolicy": {
    "patientName": "MASKED",
    "phone": "MASKED"
  },
  "status": "ACTIVE",
  "remark": "postman data permission"
}
```

预期输出：`data.ruleId` 有值。

Tests 脚本：

```javascript
const json = pm.response.json();
pm.environment.set("ruleId", json.data.ruleId);
```

### 15.15 更新数据权限规则

| 项 | 内容 |
| --- | --- |
| Method | `PUT` |
| URL | `{{baseUrl}}/api/v1/system/data-permission-rules/{{ruleId}}` |
| Auth | Bearer Token |
| Body | JSON |

请求体：

```json
{
  "roleId": {{createdRoleId}},
  "moduleCode": "POSTMAN",
  "scopeTypeCode": "ORG",
  "customDeptIds": [],
  "columnMaskPolicy": {
    "patientName": "MASKED",
    "phone": "MASKED",
    "idCard": "MASKED"
  },
  "status": "ACTIVE",
  "remark": "postman data permission update"
}
```

预期输出：`data.ruleId={{ruleId}}`。

### 15.16 查询字典类型

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/dicts` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `sys_gender`、`sys_yes_no`、`med_case_status`、`sys_user_type`。

### 15.17 查询字典项

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/dicts/med_case_status` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：包含 `CREATED`、`QC_PENDING`、`ANALYZING`、`REVIEW_PENDING` 等病例状态。

### 15.18 查询系统配置

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/api/v1/system/configs/demo.config` |
| Auth | Bearer Token |
| Body | 无 |

预期输出：如果配置不存在，预期返回业务失败；如果库中已有该 `configKey`，返回 `SystemConfigVO`。

## 16. Swagger / OpenAPI 辅助接口

### 16.1 OpenAPI JSON

| 项 | 内容 |
| --- | --- |
| Method | `GET` |
| URL | `{{baseUrl}}/v3/api-docs` |
| Auth | 无 |
| Body | 无 |

预期输出：OpenAPI JSON 文档。

### 16.2 Swagger UI

浏览器打开：

```text
{{baseUrl}}/swagger-ui.html
```

预期输出：Swagger UI 页面。

## 17. 常见失败预期

| 场景 | 预期 |
| --- | --- |
| 未带 `Authorization` 调 protected API | HTTP 401 或业务鉴权失败 |
| token 错误或过期 | HTTP 401 |
| 权限不足 | HTTP 403 |
| 必填字段缺失 | HTTP 400，`code` 非 `00000` |
| 状态机非法流转 | HTTP 400，`code` 非 `00000` |
| 查询不存在 ID | HTTP 400 或业务失败 |
| AI 回调签名错误 | HTTP 403 |
| 签名下载 URL 过期 | HTTP 403 |

## 18. 一次完整冒烟链路的通过标准

按本文档顺序执行后，至少应满足：

1. 登录成功并拿到 `token`。
2. 患者、就诊、病例都能创建并查询。
3. 文件能上传到 MinIO，并能通过 MinIO 预签名 URL 下载。
4. 病例影像和质检能创建。
5. AI 分析任务能创建，模拟回调成功后任务状态变为 `SUCCESS`。
6. 医生修正能写入，返回训练候选字段。
7. 报告能生成，导出返回 `downloadUrl`。
8. 随访计划、任务、记录能创建或查询。
9. Dashboard 接口能返回统计值。
10. 系统管理接口能查询 V014 角色、菜单、数据权限种子。
