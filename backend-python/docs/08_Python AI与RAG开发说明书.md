# 08 Python AI 与 RAG 开发说明书

## 1. 定位

Python 服务是 CariesGuard 的 AI 与知识增强服务层，面向 Java 业务平台提供影像分析、RAG 问答、知识检索、风险辅助说明和模型运行治理能力。

当前 AI 路线已统一调整为：

- 不以业务专用微调模型作为主路线；
- 使用通用大模型作为自然语言理解、医学解释、报告草稿和问答生成能力；
- 使用知识图库、结构化病例数据、指南文档和业务规则进行检索增强；
- 影像质量、牙位检测、病灶分割、分级和 uncertainty 由可替换的算法适配器提供结构化结果；
- 所有可影响业务结论的结果必须进入可追溯 JSON 和数据库留痕。

## 2. 模块边界

Python 负责：

- 接收 Java 通过 MQ 发送的 analysis task；
- 下载或读取原始影像；
- 执行 quality、detection、segmentation、grading、risk 等子流水线；
- 生成 visual assets 并上传 MinIO；
- 构造 callback payload 回传 Java；
- 提供 RAG 知识检索和通用大模型问答接口；
- 管理 `caries_ai` 运行域数据库。

Java 负责：

- 用户、患者、病例、影像、任务、报告、复核、随访等业务闭环；
- MinIO 附件主数据；
- analysis task 的发起、状态推进和 callback 落库；
- 报告和复核的业务展示；
- 权限、组织、审计和对外 API。

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
      -> Risk/summary composition
      -> VisualAssetService
  -> Java callback
  -> ana_task_record / ana_result_summary / ana_visual_asset
```

### 3.2 通用大模型 + 知识图库链路

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

知识图库来源包括：

- 口腔医学指南、科普材料、诊疗路径；
- 项目内部数据字典和业务规则；
- 标注规范、影像质量规范；
- 经脱敏的病例摘要和报告模板；
- 风险因素定义、复查周期规则。

## 4. 运行模式

`CG_AI_RUNTIME_MODE` 支持三种模式：

| 模式 | 行为 |
| --- | --- |
| `mock` | 全部模块走 mock，适合本地联调和契约验证 |
| `hybrid` | 启用的模块走真实/算法适配器，失败后可按模块规则回退 |
| `real` | 所有模块必须走真实/算法适配器，失败必须显式失败 |

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

Phase 5C 后，分级结果和不确定性不再是展示字段，而是业务语义：

- `gradingLabel`：`C0/C1/C2/C3`
- `confidenceScore`：分级置信度
- `uncertaintyScore`：不确定性分数
- `needsReview`：是否触发复核
- `gradingMode`：`mock/real`
- `gradingImplType`：`MOCK/HEURISTIC/ML_MODEL`
- `uncertaintyMode`：与 uncertainty 来源一致
- `uncertaintyImplType`：与 uncertainty 实现类型一致

规则：

```python
needsReview = uncertaintyScore >= CG_UNCERTAINTY_REVIEW_THRESHOLD
```

这些字段必须写入 `raw_result_json`，Java 侧无需扩展顶层 callback DTO 即可消费。

## 6. RAG 与通用大模型策略

### 6.1 不做微调的原因

当前阶段数据量、标注一致性、合规治理、模型评估样本和上线审批尚不足以支撑可靠微调。微调还会带来模型版本治理、幻觉评估、回滚成本和医疗责任边界问题。

因此，当前采用通用大模型 + 知识图库检索增强：

- 通用大模型负责语言生成和推理表达；
- 知识图库负责事实约束；
- 结构化病例数据负责个体上下文；
- 规则引擎负责必须确定的业务判断；
- 输出保留引用、检索证据和安全提示。

### 6.2 输出约束

RAG 输出必须：

- 标明信息来源或知识条目；
- 不直接替代医生诊断；
- 对低置信度或证据不足场景给出保守表述；
- 对影像 AI 的高 uncertainty 结论提示复核；
- 保留 request log、retrieval log、llm call log。

## 7. 目录约定

```text
backend-python/app
  api/v1/                 HTTP API
  core/                   配置、日志、异常、DB
  infra/model/            AI 适配器
  infra/vector/           向量检索
  pipelines/              子流水线编排
  repositories/           caries_ai 数据访问
  schemas/                请求/响应/回调模型
  services/               回调、MinIO、知识、RAG、风险服务
```

## 8. 验收状态

已完成：

- Phase 5A quality/detection runtime routing；
- Phase 5B segmentation visual assets；
- Phase 5C grading + uncertainty 真实化；
- Docker E2E 样例 A/B/C/D；
- Java callback 契约保持稳定；
- `raw_result_json` 留痕字段完整。

Phase 5D 建议目标：

- 风险融合真实化；
- RAG 与病例结构化上下文深度整合；
- 知识图库管理、版本、引用和评估闭环完善。
