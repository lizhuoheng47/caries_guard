# 04 AI 与 RAG 规范

## 1. 定位

Python 服务是 CariesGuard 的 AI / 知识增强服务层，面向 Java 业务平台提供影像分析、RAG 问答、知识检索、风险辅助说明和模型运行治理能力。

AI 路线：

- 不以业务专用微调模型作为主路线；
- 通用大模型负责语言生成、医学解释、报告草稿、问答；
- 知识图库 + 结构化病例数据 + 指南文档 + 业务规则负责事实约束；
- 影像 quality / detection / segmentation / grading / uncertainty 由可替换算法适配器输出结构化结果；
- 所有影响业务结论的结果必须进入可追溯 JSON 和数据库留痕。

## 2. 模块边界

Python 负责：

- 消费 MQ analysis task；
- 下载 / 读取原始影像；
- 执行 quality、detection、segmentation、grading、risk 子流水线；
- 生成 visual assets 并上传 MinIO；
- 构造 callback payload；
- 提供 RAG 知识检索和通用大模型问答 API；
- 管理 `caries_ai` 运行域数据库。

Java 负责：用户 / 患者 / 病例 / 影像 / 任务 / 报告 / 复核 / 随访业务闭环、MinIO 附件主数据、analysis task 发起和 callback 落库、权限与审计、对外 API。

## 3. AI 架构

### 3.1 影像分析链路

```text
Java analysis task
  -> MQ
  -> Python InferencePipeline
      -> QualityPipeline
      -> DetectionPipeline
      -> SegmentationPipeline
      -> GradingPipeline
      -> Risk / summary composition
      -> VisualAssetService
  -> Java callback
  -> ana_task_record / ana_result_summary / ana_visual_asset
```

### 3.2 RAG 链路

```text
用户问题 / 报告解释请求
  -> Java API
  -> Python RAG API
      -> 查询改写
      -> 知识图库检索
      -> 结构化病例上下文组装
      -> 通用大模型生成
      -> 引用与安全约束检查
  -> Java 返回或落库
```

知识图库来源：

- 口腔医学指南、科普材料、诊疗路径；
- 项目内部数据字典和业务规则；
- 标注规范、影像质量规范；
- 脱敏后的病例摘要和报告模板；
- 风险因素定义、复查周期规则。

## 4. 运行模式

`CG_AI_RUNTIME_MODE`：

| 模式 | 行为 |
| --- | --- |
| `mock` | 全部模块走 mock，适合契约验证 |
| `hybrid` | 启用模块走真实 / 算法适配器，失败可按模块规则回退 |
| `real` | 所有模块必须走真实 / 算法适配器，失败必须显式失败 |

模块开关：

- `CG_MODEL_QUALITY_ENABLED`
- `CG_MODEL_TOOTH_DETECT_ENABLED`
- `CG_MODEL_SEGMENTATION_ENABLED`
- `CG_MODEL_GRADING_ENABLED`
- `CG_MODEL_RISK_ENABLED`

关键阈值：

- `CG_MODEL_CONFIDENCE_THRESHOLD`
- `CG_UNCERTAINTY_REVIEW_THRESHOLD`

测试故障开关：

- `CG_SEGMENTATION_FORCE_FAIL`
- `CG_GRADING_FORCE_FAIL`

## 5. Grading 与 Uncertainty

Phase 5C 后，分级与不确定性是业务语义而非展示字段：

| 字段 | 含义 |
| --- | --- |
| `gradingLabel` | `C0` / `C1` / `C2` / `C3` |
| `confidenceScore` | 分级置信度 |
| `uncertaintyScore` | 不确定性分数 |
| `needsReview` | 是否触发复核 |
| `gradingMode` | `mock` / `real` |
| `gradingImplType` | `MOCK` / `HEURISTIC` / `ML_MODEL` |
| `uncertaintyMode` | 与 uncertainty 来源一致 |
| `uncertaintyImplType` | 与 uncertainty 实现类型一致 |

规则：

```python
needsReview = uncertaintyScore >= CG_UNCERTAINTY_REVIEW_THRESHOLD
```

### 分级标签定义

| 标签 | 含义 |
| --- | --- |
| `C0` | 未见明确龋损 |
| `C1` | 早期或轻度可疑 |
| `C2` | 中度龋损或需重点关注 |
| `C3` | 重度龋损或高度可疑 |

分级标签用于辅助决策，不替代医生诊断。

## 6. RAG 与通用大模型策略

### 6.1 不做微调的原因

当前数据量、标注一致性、合规治理、模型评估样本和上线审批尚不足以支撑可靠微调。微调还会带来版本治理、幻觉评估、回滚成本和医疗责任边界问题。

因此采用通用大模型 + 知识图库检索增强：通用大模型做语言生成，知识图库做事实约束，结构化病例做个体上下文，规则引擎做确定性业务判断，输出保留引用 / 检索证据 / 安全提示。

### 6.2 输出约束

RAG 输出必须：

- 标明信息来源或知识条目；
- 不直接替代医生诊断；
- 对低置信度或证据不足场景给出保守表述；
- 对影像 AI 的高 uncertainty 结论提示复核；
- 保留 `rag_request_log` / `rag_retrieval_log` / `llm_call_log`。

### 6.3 RAG 评估维度

- 检索命中；
- 引用准确；
- 医学表达保守；
- 无幻觉；
- 不越权诊断；
- 可读性。

## 7. 目录约定

```text
backend-python/app
  api/v1/                 HTTP API
  core/                   配置、日志、异常、DB
  infra/model/            AI 适配器
  infra/vector/           向量检索
  pipelines/              子流水线编排
  repositories/           caries_ai 数据访问
  schemas/                请求 / 响应 / 回调模型
  services/               回调、MinIO、知识、RAG、风险服务
```

## 8. 数据采集方向

当前不以通用大模型微调为目标。采集分三类：

1. **影像算法适配器评估数据**：影像类型、拍摄设备 / 质量状态、牙位、病灶区域、分级标签、标注 / 复核医生、标注版本、脱敏状态；
2. **RAG 知识图库数据**：权威指南、机构诊疗规范、科普材料、分级说明、影像质量规范、报告模板、风险因素、复查周期规则。每条知识必须记录来源、版本、适用范围、更新时间、审核状态；
3. **业务反馈数据**：医生采纳率、high uncertainty 是否触发复核、复核结论与 AI 分级差异、报告修改、随访结果、RAG 回答人工评分。

标注流程：

```text
数据入库 -> 脱敏检查 -> 初标 -> 复核 -> 仲裁 -> 版本冻结 -> 评估集发布
```

## 9. 质量指标

影像算法：分级一致率 / uncertainty 校准 / high uncertainty 召回率 / visual asset 生成成功率 / real 模式失败显式性。

RAG：检索命中率 / 引用准确率 / 回答可读性 / 幻觉率 / 拒答合理性 / 医生采纳率。

## 10. 验收状态

已完成：

- Phase 5A quality / detection runtime routing；
- Phase 5B segmentation visual assets；
- Phase 5C grading + uncertainty 真实化；
- Docker E2E 样例 A / B / C / D；
- Java callback 契约保持稳定；
- `raw_result_json` 留痕字段完整。

Phase 5D 目标：

- 风险融合真实化；
- RAG 与结构化病例上下文深度整合；
- 知识图库管理 / 版本 / 引用 / 评估闭环完善。
