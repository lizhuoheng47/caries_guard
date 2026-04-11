# 核心 API 契约与模块接口规范

> 文档定位：后端核心接口冻结文档  
> 目标：在功能开发前，先冻结主链路 request/response 契约  
> 适用范围：system / patient / case / image / analysis / report / followup

---

# 1. 统一接口原则

## 1.1 URL 规则
统一使用：

```text
/api/v1/**
```

规则：
- 资源名使用复数；
- URL 不写中文；
- 普通查询用资源路径；
- 动作用子资源或动作端点。

## 1.2 统一响应结构
统一返回：

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "traceId": "xxx",
  "timestamp": "2026-04-11T12:00:00"
}
```

## 1.3 Command / Query / VO 规范
- 写接口：`Command`
- 查接口：`Query`
- 返回对象：`VO`
- 外部集成对象：`Request / Response`

禁止：
- Controller 直接收 DO；
- Controller 返回数据库实体；
- 一个接口塞 20 个散乱参数。

---

# 2. system 模块

## 2.1 登录
### POST /api/v1/auth/login

请求：
```json
{
  "username": "admin",
  "password": "123456"
}
```

返回：
```json
{
  "token": "jwt-token",
  "expireIn": 7200,
  "user": {
    "userId": 1,
    "username": "admin",
    "nickName": "System Defender",
    "userTypeCode": "ADMIN",
    "orgId": 0
  }
}
```

## 2.2 当前用户
### GET /api/v1/auth/me

返回：
```json
{
  "userId": 1,
  "username": "admin",
  "nickName": "System Defender",
  "userTypeCode": "ADMIN",
  "orgId": 0,
  "roles": ["ADMIN"],
  "permissions": ["system:user:list", "patient:create"]
}
```

## 2.3 系统探活
### GET /api/v1/system/ping

返回：
```json
{
  "pong": true,
  "app": "caries-guard-backend"
}
```

## 2.4 用户列表
### GET /api/v1/system/users

查询参数：
- pageNo
- pageSize
- keyword
- deptId
- userTypeCode
- status

## 2.5 角色管理
- `GET /api/v1/system/roles`
- `POST /api/v1/system/roles`
- `PUT /api/v1/system/roles/{roleId}`

## 2.6 字典查询
- `GET /api/v1/system/dicts/{dictType}`
- `GET /api/v1/system/dicts`

---

# 3. patient 模块

## 3.1 创建患者
### POST /api/v1/patients

请求：
```json
{
  "patientName": "张三",
  "genderCode": "MALE",
  "birthDate": "2012-06-01",
  "phone": "13800000000",
  "idCardNo": "4401xxxxxxxxxxxx",
  "sourceCode": "CAMPUS_SCREENING",
  "guardian": {
    "guardianName": "张父",
    "relationCode": "PARENT",
    "phone": "13900000000",
    "primaryFlag": "1"
  }
}
```

返回：
```json
{
  "patientId": 1001,
  "patientNo": "PAT202604110001"
}
```

## 3.2 更新患者
### PUT /api/v1/patients/{patientId}

## 3.3 患者详情
### GET /api/v1/patients/{patientId}

返回：
```json
{
  "patientId": 1001,
  "patientNo": "PAT202604110001",
  "patientNameMasked": "张*",
  "genderCode": "MALE",
  "age": 13,
  "sourceCode": "CAMPUS_SCREENING",
  "guardianList": [],
  "currentProfile": {}
}
```

## 3.4 患者分页
### GET /api/v1/patients

参数：
- keyword
- sourceCode
- pageNo
- pageSize

---

# 4. visit / case 模块

## 4.1 创建就诊
### POST /api/v1/visits

请求：
```json
{
  "patientId": 1001,
  "departmentId": 10,
  "doctorUserId": 21,
  "visitTypeCode": "SCREENING",
  "visitDate": "2026-04-11T10:00:00",
  "complaint": "学校筛查"
}
```

返回：
```json
{
  "visitId": 2001,
  "visitNo": "VIS202604110001"
}
```

## 4.2 创建病例
### POST /api/v1/cases

请求：
```json
{
  "visitId": 2001,
  "patientId": 1001,
  "caseTypeCode": "CARIES_SCREENING",
  "caseTitle": "四年级春季口腔筛查病例",
  "chiefComplaint": "学校筛查初检",
  "priorityCode": "NORMAL"
}
```

返回：
```json
{
  "caseId": 3001,
  "caseNo": "CASE202604110001",
  "caseStatusCode": "CREATED"
}
```

## 4.3 病例详情
### GET /api/v1/cases/{caseId}

返回：
```json
{
  "caseId": 3001,
  "caseNo": "CASE202604110001",
  "patientId": 1001,
  "visitId": 2001,
  "caseStatusCode": "REVIEW_PENDING",
  "reportReadyFlag": "0",
  "followupRequiredFlag": "0",
  "images": [],
  "diagnoses": [],
  "latestAiSummary": null
}
```

## 4.4 病例分页
### GET /api/v1/cases

参数：
- patientId
- caseStatusCode
- attendingDoctorId
- pageNo
- pageSize

## 4.5 病例状态迁移
### POST /api/v1/cases/{caseId}/status-transition

请求：
```json
{
  "targetStatusCode": "REPORT_READY",
  "reasonCode": "DOCTOR_CONFIRMED",
  "reasonRemark": "医生复核通过"
}
```

返回：
```json
{
  "caseId": 3001,
  "fromStatusCode": "REVIEW_PENDING",
  "toStatusCode": "REPORT_READY"
}
```

---

# 5. image 模块

## 5.1 上传附件
### POST /api/v1/files/upload

说明：
- 使用 multipart/form-data；
- 返回 attachment 元数据。

返回：
```json
{
  "attachmentId": 4001,
  "fileName": "20260411_xxx.jpg",
  "bucketName": "caries-image",
  "objectKey": "case-image/2026/04/11/CASE202604110001/original_01.jpg",
  "md5": "xxxx"
}
```

## 5.2 关联病例影像
### POST /api/v1/cases/{caseId}/images

请求：
```json
{
  "attachmentId": 4001,
  "visitId": 2001,
  "patientId": 1001,
  "imageTypeCode": "PANORAMIC",
  "imageSourceCode": "UPLOAD",
  "shootingTime": "2026-04-11T10:03:00",
  "bodyPositionCode": "FULL_MOUTH",
  "primaryFlag": "1"
}
```

返回：
```json
{
  "imageId": 5001,
  "qualityStatusCode": "PENDING"
}
```

## 5.3 写入质检记录
### POST /api/v1/images/{imageId}/quality-checks

请求：
```json
{
  "checkTypeCode": "AUTO",
  "checkResultCode": "PASS",
  "qualityScore": 88,
  "blurScore": 85,
  "exposureScore": 90,
  "integrityScore": 92,
  "occlusionScore": 80,
  "issueCodes": [],
  "suggestionText": "可进入分析"
}
```

## 5.4 查询影像列表
### GET /api/v1/cases/{caseId}/images

---

# 6. analysis 模块

## 6.1 发起分析任务
### POST /api/v1/cases/{caseId}/analysis

请求：
```json
{
  "imageIds": [5001],
  "taskTypeCode": "INFERENCE"
}
```

返回：
```json
{
  "taskId": 6001,
  "taskNo": "TASK202604110001",
  "taskStatusCode": "QUEUEING"
}
```

## 6.2 查询分析任务
### GET /api/v1/analysis/tasks/{taskId}

## 6.3 AI 结果回调
### POST /api/v1/internal/ai/callbacks/analysis-result

说明：
- 仅供 AI 服务调用；
- 必须校验签名或网关白名单。

请求示例：
```json
{
  "taskNo": "TASK202604110001",
  "taskStatusCode": "SUCCESS",
  "resultSummary": {
    "overallHighestSeverity": "C2",
    "uncertaintyScore": 0.18
  },
  "visualAssets": [
    {
      "assetTypeCode": "OVERLAY",
      "attachmentId": 4101
    }
  ]
}
```

## 6.4 医生修正提交
### POST /api/v1/cases/{caseId}/corrections

请求：
```json
{
  "diagnosisId": 7001,
  "sourceImageId": 5001,
  "feedbackTypeCode": "RE_GRADE",
  "originalInferenceJson": {},
  "correctedTruthJson": {}
}
```

---

# 7. report 模块

## 7.1 生成报告
### POST /api/v1/cases/{caseId}/reports

请求：
```json
{
  "reportTypeCode": "DOCTOR"
}
```

返回：
```json
{
  "reportId": 8001,
  "reportNo": "RPT202604110001",
  "reportTypeCode": "DOCTOR",
  "versionNo": 1,
  "reportStatusCode": "DRAFT"
}
```

## 7.2 病例报告列表
### GET /api/v1/cases/{caseId}/reports

## 7.3 导出报告
### POST /api/v1/reports/{reportId}/export

返回：
```json
{
  "reportId": 8001,
  "exported": true
}
```

---

# 8. followup 模块

## 8.1 创建随访计划
### POST /api/v1/cases/{caseId}/followups

请求：
```json
{
  "nextFollowupDate": "2026-04-26",
  "priorityCode": "HIGH",
  "reason": "高风险病例复查"
}
```

## 8.2 随访任务列表
### GET /api/v1/followup/tasks

## 8.3 完成随访任务
### POST /api/v1/followup/tasks/{taskId}/complete

请求：
```json
{
  "resultCode": "DONE",
  "recordContent": "已电话通知复查"
}
```

---

# 9. 模块间接口规范

## 9.1 system -> 其他模块
提供：
- 当前用户身份
- 角色集
- 权限集
- orgId
- 数据范围上下文

## 9.2 image -> analysis
输入：
- caseId
- imageIds
- 附件地址
- imageTypeCode
- patient 基础非敏感特征

## 9.3 analysis -> case/report
输出：
- 最高严重程度
- 不确定性
- 病灶结果摘要
- 可视化资产引用
- 风险建议输入

## 9.4 report -> followup
触发条件：
- 风险高；
- 医生明确要求；
- AI 高不确定性且经医生确认需复查。

---

# 10. 结论

本文件冻结的是“开发前契约”，不是“开发后补文档”。

后续所有核心接口设计必须遵循：

1. 先冻结 request / response；
2. 再写 controller / app / domain；
3. 再由 OpenAPI 自动生成联调文档。
