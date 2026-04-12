# Analysis 跨模块 E2E 与 Python 签名联调记录

## 1. 目标

按当前阶段策略执行：

- 不切换真实 MQ（保留 logging publisher）
- 先完成跨模块主链路联测
- 先完成 Python AI 回调真实签名联调

## 2. 联测范围

本次联测覆盖 `patient + analysis` 关键链路：

1. 病例处于 `QC_PENDING`
2. 创建分析任务
3. 病例流转到 `ANALYZING`
4. AI 成功回调写回摘要/可视化/风险
5. 病例流转到 `REVIEW_PENDING`

验证点：

- 状态机路径为 `QC_PENDING -> ANALYZING -> REVIEW_PENDING`
- `ana_task_record` 状态可追踪
- `ana_result_summary`、`ana_visual_asset`、`med_risk_assessment_record` 正确写回
- 事件发布器仍为 logging 实现，不引入 MQ 基础设施变更

## 3. Python 签名联调范围

目标：验证 Java 验签与 Python 真实签名实现一致。

实现方式：

- Java 侧使用 `AiCallbackSignatureVerifier`
- 测试中直接调用本机 Python 解释器计算 HMAC-SHA256（base64url 无 padding）
- 将 Python 输出签名回传给 Java 验签
- 增加反例：错误 secret 生成的签名必须被拒绝

关键处理：

- 回调 rawBody 通过 stdin 传给 Python，避免 Windows 命令行 JSON 引号丢失问题

## 4. 新增测试

- `caries-integration/src/test/java/com/cariesguard/integration/AnalysisWorkflowE2ETests.java`
- `caries-integration/src/test/java/com/cariesguard/integration/PythonSignatureCompatibilityTests.java`

## 5. 构建验证

已通过：

- `mvn test -pl caries-integration -am`

结果：

- `AnalysisWorkflowE2ETests` 通过
- `PythonSignatureCompatibilityTests` 通过

## 6. 当前结论

P4 当前阶段目标已对齐：在不引入真实 MQ 的前提下，主链路联测与 Python 真实签名联调均可执行并通过验证。
