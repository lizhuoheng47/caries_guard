# Report 模块开发检查与完成情况

## 1. 对齐范围

本次实现对齐以下约束：

- `report.md` 的 P5 顺序与领域规则
- `03_核心API契约与模块接口规范.md` 的 report 接口契约
- `V004/V006` 中 `rpt_record`、`rpt_template`、`rpt_export_log` 与 `med_attachment` 表结构
- 报告完成后只能通过 case 模块做状态流转，禁止 report 直接改 `med_case`

## 2. 已完成能力

- 报告主链路
  - `POST /api/v1/cases/{caseId}/reports`
  - `ReportAppService.generateReport(caseId, reportTypeCode)` 已实现
  - 按 `(case_id, report_type_code)` 后端自动递增 `version_no`
  - 先写 `DRAFT`，归档完成后更新为 `FINAL`
  - 同步写入 `med_attachment`，`biz_module_code=REPORT`、`file_category_code=REPORT`
  - 病例状态通过 `CaseCommandAppService` 从 `REVIEW_PENDING -> REPORT_READY`

- 报告查询
  - `GET /api/v1/cases/{caseId}/reports`
  - `GET /api/v1/reports/{reportId}`
  - 查询全量 org-aware，非 `ADMIN/SYS_ADMIN` 仅可访问本机构数据

- 导出审计
  - `POST /api/v1/reports/{reportId}/export`
  - 校验报告与归档附件后写入 `rpt_export_log`

- 模板管理
  - `POST /api/v1/report-templates`
  - `PUT /api/v1/report-templates/{templateId}`
  - `GET /api/v1/report-templates`
  - `GET /api/v1/report-templates/{templateId}`
  - 模板按 `report_type_code` 管理，支持 `DOCTOR/PATIENT`

## 3. 分层与结构

`caries-report` 已补齐：

- `controller`
- `app`
- `domain/model|repository|service`
- `infrastructure/dataobject|mapper|repository|service`
- `interfaces/command|query|vo`

并已在 `caries-boot` 注册 mapper 扫描包：

- `com.cariesguard.report.infrastructure.mapper`

## 4. 验证结果

已执行并通过：

- `mvn test -pl caries-report -am`
- `mvn -DskipTests package -pl caries-boot -am`

覆盖点包括：

- 生成报告后记录落库调用、附件归档调用、病例状态流转调用
- 导出审计写入调用
- 报告类型与病例状态核心规则

## 5. 当前边界说明

- 当前 PDF 生成采用最小可执行实现（结构化文本转 PDF），先保证链路可运行与可归档。
- 模板美化、复杂版式、图文混排可在下一阶段迭代，不影响当前 P5 主验收项。
