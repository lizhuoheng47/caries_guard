## 一、模块定位

`caries-report` 的职责已经冻结得很清楚：

- 报告模板管理
- 医生版报告生成
- 患者版报告生成
- PDF 生成、版本归档
- 报告导出审计

核心对象就是：

- `rpt_template`
- `rpt_record`
- `rpt_export_log`。

P5 的验收标准也已经固定：

- 同一病例可生成医生版与患者版报告
- 报告版本不可覆盖
- 导出可审计
- 报告列表与详情可查。

------

## 二、启动前 Gate

report 不是从零起步，它依赖 analysis 的产物。你现在 analysis 已经具备：

- AI 摘要
- visual assets
- risk assessment
- 医生修正入口
- 病例状态流转统一走 case 模块

这意味着 report 已经可以启动。

启动前只再确认这 4 件事：

- `analysis` 已能稳定提供 `ana_result_summary`、`ana_visual_asset`、`med_risk_assessment_record`
- case 状态机接口可用，report 不得直接改 `med_case`
- MinIO/attachment 基础能力可用，因为 PDF 最终要归档到附件体系
- system 的 org 隔离查询模式已经生效。 

------

## 三、已拍板的 report 模块决策

### D1：先做“报告记录 + 版本归档”，后做“PDF 美化”

先把主链路跑通，不要一开始就在模板样式上花太多时间。
 第一阶段先做到：

- 能生成 `rpt_record`
- 能区分医生版 / 患者版
- 能递增 `version_no`
- 能落 `med_attachment`
- 能记 `rpt_export_log`

### D2：报告生成必须走病例子资源路径

冻结接口已经给了：

- `POST /api/v1/cases/{caseId}/reports`
- `GET /api/v1/cases/{caseId}/reports`
- `POST /api/v1/reports/{reportId}/export`。

### D3：report 模块禁止绕开 case 状态机

report 生成完成后若需要推动病例进入 `REPORT_READY`，必须统一通过 case 模块公开接口流转，不能在 report 里直接 update `med_case.case_status_code`。这和你刚在 analysis 模块里守住的硬边界是一套原则。

------

## 四、开发顺序

## Step 1：冻结 report 模块输入与输出

先明确 report 生成时读取哪些数据，避免边写边漂。

### 医生版报告最小输入

- 病例基础信息
- 影像列表
- AI 摘要结果
- 不确定性提示
- 风险评估结果
- 医生修正/确认结果
- 医生结论。医生版报告应包含病例摘要、影像列表、病灶定位、分级结果、不确定性提示、风险评估、复核建议和医生结论。

### 患者版报告最小输入

- 通俗结果解释
- 可能异常牙位
- 风险等级卡片
- 复查建议
- 日常护理建议。患者版报告应包含通俗解释、异常牙位、风险等级、复查建议和护理建议。

### 输出

- `rpt_record`
- 归档后的 PDF attachment
- 导出日志 `rpt_export_log`。

------

## Step 2：先做数据模型和分层骨架

按你们固定分层来建 `caries-report`：

```
caries-report/
├── controller/
├── app/
├── domain/
│   ├── model/
│   ├── service/
│   ├── event/
│   └── repository/
├── infrastructure/
│   ├── mapper/
│   ├── repository/
│   ├── convert/
│   └── client/
└── interfaces/
    ├── dto/
    ├── vo/
    ├── query/
    └── command/
```

这是统一分层规范，不要改。

建议第一批类就建这些：

### domain/model

- `ReportTemplateModel`
- `ReportRecordModel`
- `ReportExportLogModel`
- `ReportGenerateModel`

### domain/service

- `ReportDomainService`
- `ReportTemplateDomainService`

### domain/repository

- `ReportTemplateRepository`
- `ReportRecordRepository`
- `ReportExportLogRepository`

### app

- `ReportAppService`
- `ReportQueryAppService`

### controller

- `ReportController`

### interfaces/command

- `GenerateReportCommand`
- `ExportReportCommand`

### interfaces/vo

- `ReportDetailVO`
- `ReportListItemVO`
- `ReportExportResultVO`

------

## Step 3：先实现 P5-1：报告记录

你们任务清单里 P5-1 已经很明确：

- 生成报告记录
- `version_no` 递增
- 医生版 / 患者版分型。

### 必做规则

1. 同一病例可有多份报告
2. 同一病例同一类型报告生成新版本时，只能新增，不可覆盖旧记录
3. `version_no` 必须按 `(case_id, report_type_code)` 递增
4. 报告记录必须带 `org_id`
5. 查询必须 org-aware。

### 这里最关键的方法

```
ReportAppService.generateReport(caseId, GenerateReportCommand cmd)
```

建议内部步骤：

1. 查 case 是否存在且当前用户有权限
2. 查病例当前状态是否允许生成报告
3. 聚合病例、影像、analysis 摘要、风险、修正结果
4. 计算当前 `reportTypeCode` 下的新 `versionNo`
5. 先落一条 `rpt_record`，状态先记 `DRAFT`
6. 调模板渲染
7. 生成 PDF 并归档 attachment
8. 回写 `rpt_record` 的附件引用和最终状态
9. 通过 case 模块把病例推进到 `REPORT_READY`。P5 的目标本身就是完成医生版/患者版报告生成、版本归档和导出审计。

------

## Step 4：实现 P5-2：报告生成

你们任务清单写的是：

- 模板渲染
- PDF 生成
- attachment 归档
- 状态更新。

### 推荐做法

先不要追求复杂模板引擎，第一版用最稳的方式：

- HTML 模板
- 组装 `ReportRenderDTO`
- 渲染 HTML
- 再转 PDF

### 你要先做的 3 个内部组件

- `ReportTemplateResolver`
- `ReportRenderService`
- `ReportPdfService`

### 模板策略

建议至少准备两套模板：

- `DOCTOR`
- `PATIENT`

而且一开始模板内容放在 `rpt_template.template_content` 或项目资源文件都行，但要保证后续能切换成库表管理。`rpt_template` 本来就是报告模板表。

------

## Step 5：实现 P5-3：导出审计

任务清单已经写明：

- 导出日志记录
- 下载操作审计
- 权限校验。

导出接口也已经冻结：

- `POST /api/v1/reports/{reportId}/export`。

### 导出接口该做什么

1. 校验当前用户是否有该报告访问权限
2. 校验报告是否存在且 attachment 已归档
3. 记录一条 `rpt_export_log`
4. 返回导出成功结果，或者返回预签名下载地址

### 注意

导出不是“直接给文件”这么简单。
 你们文档已经把对象存储安全要求冻结了：

- 默认 `PRIVATE`
- 对外访问走短时预签名
- 不允许前端直拼公网地址。

------

## Step 6：Controller 接口一次性定好

直接按冻结 API 来，不要自己再改风格：

### 1. 生成报告

```
POST /api/v1/cases/{caseId}/reports
```

请求：

```
{
  "reportTypeCode": "DOCTOR"
}
```

返回：

```
{
  "reportId": 8001,
  "reportNo": "RPT202604110001",
  "reportTypeCode": "DOCTOR",
  "versionNo": 1,
  "reportStatusCode": "DRAFT"
}
```

这个契约已经冻结。

### 2. 病例报告列表

`GET /api/v1/cases/{caseId}/reports`。

### 3. 导出报告

`POST /api/v1/reports/{reportId}/export`。

### 4. 建议补一个详情接口

虽然片段里没直接写明，但为了“列表与详情可查”的 P5 验收，建议补：
 `GET /api/v1/reports/{reportId}`。P5 验收明确要求“报告列表与详情可查”。

------

## Step 7：必须写的领域规则

这块最容易被忽视，但实际最重要。

### R1：只有满足条件的病例才能生成报告

建议至少要求：

- 病例存在
- org 权限合法
- analysis 已有可消费结果
- 医生已确认，或流程允许直接出报告

### R2：版本号不能靠前端传

必须后端按数据库记录自行递增。

### R3：同一份报告不可被覆盖

新内容 = 新版本，不是 update 原版。

### R4：患者版和医生版是两类报告

不能用一个字段文本硬拼混过去。你们文档已经明确医生版和患者版内容职责不同。

### R5：report 模块只消费 analysis 结果，不改 analysis 结果

不要在 report 里反向修 analysis 表。