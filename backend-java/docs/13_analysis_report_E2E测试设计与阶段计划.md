# analysis -> report E2E 测试设计与阶段计划

## 1. 目标主用例（冻结）

主链路：

1. 创建 AI 任务  
2. AI 成功回调  
3. analysis 写回摘要/可视化资产/风险结果  
4. 病例状态 `ANALYZING -> REVIEW_PENDING`  
5. 医生生成报告  
6. 病例状态 `REVIEW_PENDING -> REPORT_READY`  
7. 导出报告  
8. `rpt_export_log` 留痕

## 2. 当前阶段（Stage-1）

本阶段采用“设计先行 + 用例先落”方式，先用跨模块 in-memory 集成夹具固化业务编排约束，不宣称真实基础设施全链路已经完成。

已新增测试：

- `AnalysisToReportE2ETest`
- `AnalysisCallbackIdempotencyE2ETest`
- `ReportExportAuditIntegrationTest`

共用夹具：

- `com.cariesguard.integration.support.AnalysisReportE2EFixture`

## 3. Stage-1 断言覆盖

### analysis 段

- 任务状态从 `QUEUEING` 进入 `SUCCESS`
- `ana_result_summary` 写回（夹具内等价仓储）
- `ana_visual_asset` 写回（夹具内等价仓储）
- `med_risk_assessment_record` 写回（夹具内等价仓储）
- 病例状态 `ANALYZING -> REVIEW_PENDING`
- 状态日志存在上述跳转，`change_reason_code = AI_CALLBACK_SUCCESS`

### report 段

- 生成 `rpt_record`
- `report_type_code = DOCTOR`
- 首次 `version_no = 1`
- 归档后存在 `attachment_id`
- 病例状态 `REVIEW_PENDING -> REPORT_READY`
- 状态日志存在上述跳转，`change_reason_code = DOCTOR_CONFIRMED`

### 导出审计段

- 导出返回 `exported = true`
- `rpt_export_log` 新增记录（夹具内等价仓储）
- `report_id / export_type_code / export_channel_code / exported_by / exported_at / org_id` 完整

## 4. 下一阶段（Stage-2）

将当前用例迁移为真实基础设施集成测试：

- `@SpringBootTest + MockMvc` 或 HTTP 调用
- 真实 MySQL（Testcontainers 或独立测试库）
- Flyway 迁移
- 真实 Repository/Service/Controller
- 外部依赖保持 mock：MQ 真消费、AI 真推理、对象存储真实公网链路

## 5. 验证命令

- `mvn test -pl caries-integration -am`

## 6. Stage-2 落地结果（已完成）

Stage-2 已在 `caries-boot` 完成真实集成联测，覆盖 controller -> app service -> repository -> MySQL(Flyway)。

新增测试类：

- `caries-boot/src/test/java/com/cariesguard/boot/e2e/AnalysisToReportE2ETest.java`
- `caries-boot/src/test/java/com/cariesguard/boot/e2e/AnalysisCallbackIdempotencyE2ETest.java`
- `caries-boot/src/test/java/com/cariesguard/boot/e2e/ReportExportAuditIntegrationTest.java`

测试基座：

- `caries-boot/src/test/java/com/cariesguard/boot/e2e/AnalysisReportE2EBaseTest.java`
- `caries-boot/src/test/resources/application-e2e.yml`

关键执行说明：

- 测试数据库独立为 `cg_e2e`，避免影响开发库 `cg`。
- Flyway 在测试库上全量迁移，保证断言基于最新迁移结构。
- 外部对象存储在测试中使用 `@MockBean`，不切真实存储网络链路。

验证命令：

- `mvn -pl caries-boot -am "-Dtest=AnalysisToReportE2ETest,AnalysisCallbackIdempotencyE2ETest,ReportExportAuditIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 7. 下一步开发建议（2026-04-13）

analysis -> report 的 E2E 已完成，下一步不切真实 MQ，直接进入 `P6 followup`。

1. 实现 followup 主链路：`fup_plan`、`fup_task`、`fup_record` 的应用服务与 API。
2. 建立报告/风险结果触发随访的规则入口（高风险、医生复核建议）。
3. 新增 `analysis -> report -> followup` 跨模块 E2E 用例，形成更完整答辩证据链。

## 8. P6 接入后的口径更新（2026-04-13）

`ReportAppService` 已接入 `FollowupTriggerService`。因此在“高风险 + 建议复核”的报告生成路径中：

1. 病例会先发生 `REVIEW_PENDING -> REPORT_READY`（`DOCTOR_CONFIRMED`）。
2. 随后触发 `REPORT_READY -> FOLLOWUP_REQUIRED`（`FOLLOWUP_TRIGGERED`）。
3. 旧的 analysis->report 用例若使用高风险回调，最终病例状态将不再停留在 `REPORT_READY`，而是 `FOLLOWUP_REQUIRED`。

后续测试分层原则：

- analysis/report 基础链路：可用低风险输入维持终态 `REPORT_READY`。
- analysis->report->followup 主链路：使用高风险输入，断言 followup 计划/任务/状态日志/通知留痕。
