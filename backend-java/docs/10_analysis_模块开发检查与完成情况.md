# Analysis 模块开发检查与完成情况

## 1. 当前结论

`analysis` 模块已经具备完整的任务创建、重试、回调写回、幂等控制和纠偏反馈能力，是当前主链路中最完整的业务协同模块之一。

## 2. 已实现接口

- `POST /api/v1/analysis/tasks`
- `POST /api/v1/analysis/tasks/retry`
- `GET /api/v1/analysis/tasks/{taskId}`
- `GET /api/v1/analysis/tasks`
- `POST /api/v1/internal/ai/callbacks/analysis-result`
- `POST /api/v1/analysis/corrections`
- `POST /api/v1/cases/{caseId}/analysis`
- `POST /api/v1/cases/{caseId}/corrections`

## 3. 任务创建规则

当前创建分析任务时会校验：

- 病例存在且机构可访问
- 病例状态允许分析
- `patientId` 必须与病例匹配
- 同病例不存在运行中的任务，除非显式允许强制重试
- 病例至少存在一张 `PASS` 影像

任务创建成功后：

- 写入 `ana_task_record`
- 生成 `taskNo`
- 把病例从 `QC_PENDING` 推进到 `ANALYZING`
- 发布 `analysis.requested`

## 4. 回调处理规则

### 4.1 安全

- HMAC 签名校验
- 时间戳窗口校验
- 原始 JSON 串解析为 DTO

### 4.2 PROCESSING

- 仅更新任务状态，不写业务结果

### 4.3 SUCCESS

- 更新任务为 `SUCCESS`
- 写入 `ana_result_summary`
- 写入 `ana_visual_asset`
- 视情况写入 `med_risk_assessment_record`
- 将病例推进到 `REVIEW_PENDING`
- 发布 `analysis.completed`

### 4.4 FAILED

- 更新任务为 `FAILED`
- 保留错误信息
- 将病例退回 `QC_PENDING`
- 发布 `analysis.failed`

## 5. 幂等与重试

当前已实现的高价值规则：

- 终态重复回调直接幂等返回
- 非法回调顺序拦截
- 失败任务重试必须新建任务
- 新任务用 `retry_from_task_id` 关联旧任务
- 旧任务已被重试后，迟到回调不会再次写回摘要/风险/资产

## 6. 医生纠偏

当前修正反馈写入：

- `ana_correction_feedback`

用途：

- 记录医生对 AI 结果的再判断
- 保留 `original_inference_json` 与 `corrected_truth_json`
- 后续可供离线训练清洗使用

## 7. 当前数据库表

- `ana_task_record`
- `ana_result_summary`
- `ana_visual_asset`
- `ana_correction_feedback`
- `med_risk_assessment_record`

## 8. 当前限制

- 仓库内没有 MQ 消费端或 Python 推理服务实现
- 当前模型治理、版本灰度、批量任务调度并未实现
- 风险评估完全依赖回调 payload，不在 Java 侧二次计算

## 9. 测试情况

analysis 模块已有：

- app 层单测
- 幂等/回调测试
- boot 真库 E2E
- 跨 report/followup 的链路测试
