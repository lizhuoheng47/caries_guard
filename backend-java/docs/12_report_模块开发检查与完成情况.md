# Report 模块开发检查与完成情况

## 1. 当前结论

`report` 模块已经实现模板管理、报告生成、报告查询和导出审计，但当前“导出”仍以审计留痕为主，PDF 输出也仍是极简实现。

## 2. 已实现接口

- `POST /api/v1/cases/{caseId}/reports`
- `GET /api/v1/cases/{caseId}/reports`
- `GET /api/v1/reports/{reportId}`
- `POST /api/v1/reports/{reportId}/export`
- `POST /api/v1/report-templates`
- `PUT /api/v1/report-templates/{templateId}`
- `GET /api/v1/report-templates`
- `GET /api/v1/report-templates/{templateId}`

## 3. 生成报告的真实规则

当前生成报告时会：

1. 校验病例存在且状态为 `REVIEW_PENDING` 或 `REPORT_READY`
2. 读取最近一次分析摘要
3. 读取病例影像、风险评估、最新医生纠偏
4. 解析模板内容（无模板时走默认模板）
5. 渲染占位符
6. 生成 PDF 字节流
7. 存入对象存储并在 `med_attachment` 建档
8. 在 `rpt_record` 中先创建 `DRAFT`，再更新为 `FINAL`
9. 若病例原状态为 `REVIEW_PENDING`，推进到 `REPORT_READY`
10. 调用 `FollowupTriggerService` 判断是否自动触发随访

## 4. 模板能力

当前模板支持两类：

- `DOCTOR`
- `PATIENT`

当前解析方式：

- 模板内容中使用 `{{placeholder}}`
- `ReportRenderService` 直接字符串替换

注意：

- 模板更新当前不会自动递增 `version_no`
- 若数据库中没有模板，会使用代码内置默认模板

## 5. 导出能力的当前真实含义

`POST /api/v1/reports/{reportId}/export` 当前做的是：

- 校验报告与附件存在
- 写入 `rpt_export_log`
- 返回 `exported=true` 和 `exportLogId`

当前不会：

- 单独生成新的导出文件
- 直接返回文件流
- 直接返回下载地址

## 6. PDF 生成实现

当前 `ReportPdfService` 是一个极简 PDF 生成器：

- 只支持基本 ASCII 文本
- 最多约 35 行
- 非 ASCII 字符会被替换为 `?`

这说明它适合测试和演示闭环，不适合作为正式生产版病历报告输出。

## 7. 当前数据库表

- `rpt_template`
- `rpt_record`
- `rpt_export_log`
- `med_attachment`（报告归档文件）

## 8. 当前限制

- 报告没有电子签章流程，`signed_at` 当前未形成完整业务链路
- 导出仅审计留痕
- PDF 质量需要后续替换成熟渲染组件

## 9. 测试证据

当前 report 能力在以下测试中被覆盖：

- `AnalysisToReportE2ETest`
- `AnalysisReportFollowupE2ETest`
- `ReportExportAuditIntegrationTest`
- `MainlineWorkflowE2ETest`
