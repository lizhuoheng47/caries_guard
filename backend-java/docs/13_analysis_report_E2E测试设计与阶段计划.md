# analysis -> report E2E 测试设计与阶段计划

## 1. 当前文档定位

原文档是测试设计计划，当前改为记录“已经落地的 analysis -> report 测试链路”。

## 2. 当前已落地链路

analysis -> report 方向当前已经具备以下自动化验证：

1. 创建 AI 任务
2. AI 成功回调
3. 写入分析摘要 / 资产 / 风险评估
4. 病例状态 `ANALYZING -> REVIEW_PENDING`
5. 生成报告
6. 病例状态 `REVIEW_PENDING -> REPORT_READY`
7. 导出报告时写入 `rpt_export_log`

## 3. 当前代表性测试

- `AnalysisToReportE2ETest`
- `MainlineWorkflowE2ETest`

## 4. 已验证的关键断言

- analysis 成功回调后 `ana_result_summary` 有数据
- 风险评估可被 report 读取
- 报告版本号可自动递增
- 报告归档文件会写 `med_attachment`
- 报告生成不会直接跨模块更新数据库，而是通过 case 服务做状态迁移
- 导出时会有 `rpt_export_log`

## 5. 当前仍未覆盖的点

- 真正的报告文件下载流接口
- 中文模板渲染质量
- 复杂多模板版本切换场景
- 报告签章与撤回

## 6. 当前阶段判断

从测试视角看，analysis -> report 已不是计划态，而是已落地、可回归的稳定主链路。
