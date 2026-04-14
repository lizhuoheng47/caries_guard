# API 契约与当前实现差异清单

## 1. 文档目标

本文件用于对齐：

- `docs/03_核心API契约与模块接口规范.md`
- 当前仓库真实实现

目标不是推翻契约，而是明确哪些已经一致，哪些仍需在文档或代码层收口。

## 2. 已基本对齐的部分

### 2.1 统一响应结构

当前已实现：

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "traceId": "xxx",
  "timestamp": "2026-04-14T00:00:00Z"
}
```

与 `03` 文档口径一致。

### 2.2 system 基础接口

以下接口当前与契约已基本一致：

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/system/ping`

当前真实情况：

- `login` 返回 `token / expireIn / user`
- `me` 返回 `userId / username / nickName / userTypeCode / orgId / roles / permissions`
- `ping` 返回 `pong / app`

## 3. 当前主要差异

### 3.1 analysis 创建任务入参与契约不一致

契约文档写法：

- `POST /api/v1/cases/{caseId}/analysis`
- request 示例使用 `imageIds`

当前真实实现：

- 真实 command 为 `CreateAnalysisTaskCommand`
- 当前接口实际接收：
  - `caseId`
  - `patientId`
  - `forceRetryFlag`
  - `taskTypeCode`
  - `remark`

当前业务逻辑：

- 可分析影像不是前端显式传 `imageIds`
- 服务端会按病例自动选择 `quality_status_code = PASS` 的有效影像

结论：

- `03` 文档中的 `imageIds` 示例已经落后
- 应更新为服务端按病例自动解析影像

### 3.2 case 别名接口的 body 校验要求需要写清楚

当前别名接口：

- `POST /api/v1/cases/{caseId}/analysis`
- `POST /api/v1/cases/{caseId}/corrections`

虽然控制器内部会使用 path 中的 `caseId` 覆盖 command，但由于 `@Valid` 会先校验 request body：

- `analysis` 的 body 当前仍需要带 `caseId`
- `corrections` 的 body 当前仍需要带 `caseId`

结论：

- 这是当前实现细节
- 若不改代码，则文档应明确 body 仍需包含 `caseId`

### 3.3 report 生成返回状态与契约样例不一致

契约文档示例：

- `reportStatusCode = DRAFT`

当前真实实现：

- 报告生成后会完成附件归档和状态更新
- 返回的 `reportStatusCode` 为 `FINAL`

结论：

- 契约样例应调整为当前真实口径

### 3.4 followup 路径设计已经演进

契约文档写法：

- `POST /api/v1/cases/{caseId}/followups`
- `GET /api/v1/followup/tasks`
- `POST /api/v1/followup/tasks/{taskId}/complete`

当前真实实现：

- `POST /api/v1/cases/{caseId}/followup/plans`
- `GET /api/v1/cases/{caseId}/followup/plans`
- `POST /api/v1/cases/{caseId}/followup/tasks`
- `GET /api/v1/cases/{caseId}/followup/tasks`
- `POST /api/v1/followup/tasks/{taskId}/status`
- `POST /api/v1/followup/records`

当前完成随访的真实方式：

- 通过 `POST /api/v1/followup/records` 写记录
- 同时自动将任务改为 `DONE`

结论：

- `03` 文档中的 followup 路径已经明显落后
- 应按 plan / task / record 三层结构更新

### 3.5 dashboard 不在 03 文档中

当前真实实现已存在：

- `/api/v1/dashboard/overview`
- `/api/v1/dashboard/case-status-distribution`
- `/api/v1/dashboard/risk-level-distribution`
- `/api/v1/dashboard/followup-task-summary`
- `/api/v1/dashboard/backlog-summary`
- `/api/v1/dashboard/model-runtime`
- `/api/v1/dashboard/trend`

这些接口当前已冻结在：

- `P7_Dashboard_数据口径冻结表与首批接口清单.md`
- `P7_Dashboard开发文档与说明文档.md`

结论：

- `03` 不再覆盖完整 API 现状
- dashboard 应继续以 P7 专项文档为准

## 4. 建议收口动作

### 4.1 文档层

优先修改 `03_核心API契约与模块接口规范.md`：

- 修正 analysis request 示例
- 修正 report 生成返回状态
- 重写 followup 部分路径和交互
- 增加 dashboard 章节或明确引用 P7 文档

### 4.2 代码层

当前最值得后续优化的一点：

- 让 case 别名接口真正只依赖 path variable，不再要求 body 内重复传 `caseId`

这属于体验优化，不影响当前主链路可用。

## 5. 当前统一口径

如果当前要联调或答辩，应以以下文档优先级为准：

1. `03_核心API契约与模块接口规范.md` 中已对齐部分
2. 本文档中的差异修正
3. P7 / P8 专项文档中的新增接口与测试口径
