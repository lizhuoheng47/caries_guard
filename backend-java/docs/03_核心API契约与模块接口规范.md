# 核心 API 契约与模块接口规范

## 1. 统一契约

### 1.1 URL 规则

统一前缀：

```text
/api/v1/**
```

### 1.2 认证规则

除公开接口外，统一使用：

```http
Authorization: Bearer <token>
```

### 1.3 统一响应体

当前所有控制器统一返回 `ApiResponse<T>`：

```json
{
  "code": "00000",
  "message": "Success",
  "data": {},
  "traceId": "..."
}
```

### 1.4 当前公开接口

- `POST /api/v1/auth/login`
- `GET /api/v1/system/ping`
- `GET /api/v1/files/{attachmentId}/content`（签名校验，不走 JWT）
- `POST /api/v1/internal/ai/callbacks/analysis-result`
- `/actuator/health`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

## 2. system 模块

### 2.1 auth

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/auth/permissions`

### 2.2 support / metadata

- `GET /api/v1/system/ping`
- `GET /api/v1/system/dicts`
- `GET /api/v1/system/dicts/{dictType}`
- `GET /api/v1/system/configs/{configKey}`

### 2.3 admin

- `GET /api/v1/system/users`
- `GET /api/v1/system/users/{userId}`
- `POST /api/v1/system/users`
- `PUT /api/v1/system/users/{userId}`
- `GET /api/v1/system/roles`
- `GET /api/v1/system/roles/{roleId}`
- `POST /api/v1/system/roles`
- `PUT /api/v1/system/roles/{roleId}`
- `GET /api/v1/system/menus`
- `GET /api/v1/system/menus/{menuId}`
- `POST /api/v1/system/menus`
- `PUT /api/v1/system/menus/{menuId}`
- `GET /api/v1/system/data-permission-rules`
- `POST /api/v1/system/data-permission-rules`
- `PUT /api/v1/system/data-permission-rules/{ruleId}`

## 3. patient / visit / case 模块

### 3.1 patient

- `POST /api/v1/patients`
- `PUT /api/v1/patients/{patientId}`
- `GET /api/v1/patients/{patientId}`
- `GET /api/v1/patients`

### 3.2 visit

- `POST /api/v1/visits`
- `GET /api/v1/visits/{visitId}`
- `GET /api/v1/visits`

### 3.3 case

- `POST /api/v1/cases`
- `GET /api/v1/cases/{caseId}`
- `GET /api/v1/cases`
- `POST /api/v1/cases/{caseId}/diagnoses`
- `POST /api/v1/cases/{caseId}/tooth-records`
- `POST /api/v1/cases/{caseId}/status-transition`

## 4. image 模块

- `POST /api/v1/files/upload`
- `GET /api/v1/files/{attachmentId}/access-url`
- `GET /api/v1/files/{attachmentId}/content`
- `POST /api/v1/cases/{caseId}/images`
- `GET /api/v1/cases/{caseId}/images`
- `GET /api/v1/cases/{caseId}/images/detail/{imageId}`
- `POST /api/v1/images/{imageId}/quality-checks`
- `GET /api/v1/images/{imageId}/quality-checks/current`

## 5. analysis 模块

- `POST /api/v1/analysis/tasks`
- `POST /api/v1/analysis/tasks/retry`
- `GET /api/v1/analysis/tasks/{taskId}`
- `GET /api/v1/analysis/tasks`
- `POST /api/v1/internal/ai/callbacks/analysis-result`
- `POST /api/v1/analysis/corrections`
- `POST /api/v1/cases/{caseId}/analysis`
- `POST /api/v1/cases/{caseId}/corrections`

## 6. report 模块

- `POST /api/v1/cases/{caseId}/reports`
- `GET /api/v1/cases/{caseId}/reports`
- `GET /api/v1/reports/{reportId}`
- `POST /api/v1/reports/{reportId}/export`
- `POST /api/v1/report-templates`
- `PUT /api/v1/report-templates/{templateId}`
- `GET /api/v1/report-templates`
- `GET /api/v1/report-templates/{templateId}`

## 7. followup 模块

- `POST /api/v1/cases/{caseId}/followup/plans`
- `GET /api/v1/cases/{caseId}/followup/plans`
- `GET /api/v1/followup/plans/{planId}`
- `POST /api/v1/followup/plans/{planId}/cancel`
- `POST /api/v1/followup/plans/{planId}/close`
- `POST /api/v1/cases/{caseId}/followup/tasks`
- `GET /api/v1/cases/{caseId}/followup/tasks`
- `GET /api/v1/followup/tasks/{taskId}`
- `POST /api/v1/followup/tasks/{taskId}/status`
- `POST /api/v1/followup/tasks/{taskId}/assign`
- `POST /api/v1/followup/records`
- `GET /api/v1/cases/{caseId}/followup/records`
- `GET /api/v1/followup/tasks/{taskId}/records`

## 8. dashboard 模块

- `GET /api/v1/dashboard/overview`
- `GET /api/v1/dashboard/case-status-distribution`
- `GET /api/v1/dashboard/risk-level-distribution`
- `GET /api/v1/dashboard/followup-task-summary`
- `GET /api/v1/dashboard/backlog-summary`
- `GET /api/v1/dashboard/trend`
- `GET /api/v1/dashboard/model-runtime`

## 9. 契约中的当前注意点

- 文件下载不通过 report/export 接口直接返回流，而是通过附件签名访问链路实现
- AI 回调 body 当前由服务端读取原始 JSON 字符串做验签与解析
- dashboard 接口统一按当前登录用户 `org_id` 聚合
- 普通角色权限依赖 `sys_menu` 权限码；当前本地库默认只有 `SYS_ADMIN` 可直接跑通所有受保护接口
