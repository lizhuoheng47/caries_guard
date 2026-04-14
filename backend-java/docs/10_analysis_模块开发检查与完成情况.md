# Analysis 模块开发检查与完成情况

## 1. 文档对齐范围

本次检查按《一、模块定位.md》重点核对以下约束：

- 模块边界是否纯业务编排（不做训练治理、不做 Java 推理）
- 是否严格服从病例状态机：`QC_PENDING -> ANALYZING -> REVIEW_PENDING`
- 回调是否具备验签与幂等能力
- 结果落库职责是否分表清晰
- 分层是否遵循 `Controller -> App -> Domain -> Infrastructure`

结论：`analysis` 模块主链路已具备可执行能力，且本轮已修正关键不一致项。

## 2. 已完成能力（与模块定位一致）

- 任务创建与重试：`ana_task_record` 主线落库、状态跟踪、重试血缘（`retry_from_task_id`）
- 回调处理：`POST /api/v1/internal/ai/callbacks/analysis-result`
- 回调验签：HMAC + 时间戳窗口
- 回调幂等：终态重复回调直接幂等返回，终态冲突回调拦截
- 成功回写：`ana_result_summary` + `ana_visual_asset` + `med_risk_assessment_record`
- 修正反馈：`ana_correction_feedback`
- 病例状态流转：统一通过 `CaseCommandAppService`，未绕开状态机

## 3. 本轮修正项（2026-04-13）

1. 修正事件发布时机，先过状态机再发布任务事件  
`AnalysisTaskAppService#createTask/retryTask` 从“先发布后流转”改为“先流转后发布”，避免状态机失败时对外发布无效任务事件。

2. 加强回调基础字段校验与状态规范化  
`AnalysisCallbackDomainService` 新增：
- `normalizeAndValidateTaskNo`
- `normalizeAndValidateTaskStatus`  
`AnalysisCallbackAppService` 接入后，避免空值/NPE，并统一处理大小写状态码（如 `success`）。

3. 摘要查询优先使用聚合列  
`AnalysisQueryAppService#toSummaryVO` 改为优先读聚合字段：
- `overall_highest_severity`
- `uncertainty_score`
- `review_suggested_flag`  
当 `raw_result_json` 非法时，若聚合列可用则仍可稳定返回。

4. 收紧 `QC_PENDING -> ANALYZING` 前置校验  
`VisitCaseCommandRepositoryImpl#hasActiveImage` 增加 `quality_status_code=PASS` 条件，确保“可分析影像”语义一致。

## 4. 验收测试覆盖（当前）

- 任务创建成功/失败路径
- 重试任务规则（仅 FAILED 可重试）
- 回调成功/失败写回
- 回调幂等、终态冲突、迟到回调处理
- 修正反馈提交与 sourceImage 归属校验
- 回调状态大小写规范化与空状态校验
- 摘要聚合列回退读取能力

## 5. 当前剩余项

- Python AI 消费端联调：对接真实 `analysis.requested` 队列消费
- 补充 MQ 消费确认、失败重投、死信等运行治理策略
- 与 Python AI 服务进行真实签名联调验证

## 6. 当前结论

`analysis` 模块当前实现已满足“业务编排中枢”定位，不是简单回调接收器。  
本轮修正后，边界、状态机、幂等、分层与落库职责均与《一、模块定位.md》保持一致。

补充说明（2026-04-14）：

- `AnalysisTaskEventPublisher` 已接入真实 RabbitMQ Publisher
- `analysis.requested` 发布原始 AI 请求 JSON，供 Python AI 消费
- `analysis.completed` / `analysis.failed` 事件同步发布到 RabbitMQ，供后续集成侧订阅
- `local` profile 默认走真实 RabbitMQ，`e2e` profile 仍保持 logging publisher，避免测试对 MQ 产生硬依赖
