# Analysis 跨模块 E2E 与 Python 签名联调记录

## 1. 当前结论

当前仓库没有 Python AI 服务代码，但 Java 后端已经把“与 Python 服务协同所需的契约、验签和跨模块写回链路”固化完成。

## 2. 当前联调口径

### 2.1 请求侧

Java 会把分析任务发布为带任务号、病例号、患者号、模型版本、影像列表的消息/日志事件。

### 2.2 回调侧

Python 或其他 AI 服务必须调用：

- `POST /api/v1/internal/ai/callbacks/analysis-result`

并附带：

- `X-AI-Timestamp`
- `X-AI-Signature`

签名算法：

```text
Base64Url(HMAC_SHA256(secret, timestamp + "." + rawBody))
```

## 3. 当前联调可保证的行为

- 签名错误直接拒绝
- 时间戳过期直接拒绝
- 重复终态回调幂等 ACK
- 成功回调会写摘要、风险、可视化资产并推进病例状态
- 失败回调会把病例退回 `QC_PENDING`

## 4. 已有测试证据

当前仓库中可视为联调证据的测试包括：

- `AnalysisCallbackIdempotencyE2ETest`
- `AnalysisToReportE2ETest`
- `AnalysisReportFollowupE2ETest`
- `MainlineWorkflowE2ETest`

这些测试虽然不是调用真实 Python 服务，但已经覆盖：

- 回调签名
- 回调状态流转
- 写回下游 report/followup 的效果

## 5. 当前未纳入仓库的部分

- Python 侧任务消费逻辑
- Python 侧模型推理逻辑
- Python 侧可视化资产生成逻辑
- 真 MQ 两端联调脚本

## 6. 对外说明建议

如果需要对外介绍“Java 和 Python 是否已联通”，准确说法应为：

- Java 侧协同契约、验签规则、写回规则已固定
- 仓库内已有对该契约的自动化测试
- Python 服务本身不在当前仓库内
