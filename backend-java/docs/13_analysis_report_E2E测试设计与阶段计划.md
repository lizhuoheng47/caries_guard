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
