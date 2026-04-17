# 04 Java 后端开发说明书

## 1. Java 定位

Java 后端是 CariesGuard 的业务主系统，负责业务状态、权限、病例、影像、报告、复核和随访。Python AI/RAG 是外部智能能力服务，Java 不把 AI 结果视为最终诊断。

## 2. 主要职责

- 用户、角色、组织和权限；
- 患者、就诊、病例；
- 附件和 MinIO 对象主数据；
- analysis task 创建和状态推进；
- MQ 任务发送；
- callback 签名校验和落库；
- `ana_result_summary`、`ana_visual_asset` 写入；
- review/report/followup 主链；
- RAG 问答入口和展示。

## 3. AI Analysis 集成

Java 发起任务：

```text
POST /api/v1/analysis/tasks
  -> ana_task_record
  -> MQ analysis.requested
```

Python 回调：

```text
POST /api/v1/internal/ai/callbacks/analysis-result
```

Java 处理：

- 校验签名；
- 更新 task 状态；
- 保存 summary；
- 保存 raw_result_json；
- 保存 visual assets；
- 根据 `needsReview` 推进复核语义；
- 失败时记录 error_code/error_message。

## 4. RAG 集成

Java 负责：

- 鉴权；
- 组装用户角色和病例上下文；
- 调用 Python RAG API；
- 展示 answer/citations/safetyFlags；
- 必要时记录问答历史。

Java 不直接调用通用大模型 provider。

## 5. MinIO 规则

Java 是附件主数据权威：

- 原始影像写 `med_attachment` 和 `med_image_file`；
- Python 生成 visual asset 后回传 bucket/object；
- Java 为 visual asset 补写 `med_attachment` 和 `ana_visual_asset`；
- bucket 不开放匿名访问。

## 6. Phase 5C 字段消费

Java 需要兼容 `raw_result_json` 中：

- `gradingMode`
- `gradingImplType`
- `gradingLabel`
- `confidenceScore`
- `uncertaintyMode`
- `uncertaintyImplType`
- `uncertaintyScore`
- `needsReview`

顶层 callback DTO 不需要为每个字段都新增强类型字段。

## 7. 复核语义

建议规则：

```text
needsReview=true -> review pending
uncertaintyScore high -> 报告采用保守提示
```

Java 应允许医生复核后覆盖或确认 AI 建议，并保留原始 AI 证据。

## 8. 开发约束

- 不跨库修改 `caries_ai`；
- 不把 RAG 回答当业务状态；
- 不在 Java 内部硬编码 Python 算法细节；
- 不破坏 callback 兼容性；
- 不在日志输出敏感明文。
