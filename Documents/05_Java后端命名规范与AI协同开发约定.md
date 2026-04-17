# 05 Java 后端命名规范与 AI 协同开发约定

## 1. 命名原则

- 业务实体使用清晰英文；
- 数据库字段使用 snake_case；
- Java 类使用 PascalCase；
- Java 方法和变量使用 camelCase；
- JSON 对外字段使用 camelCase；
- AI 原始输出统一放入 `raw_result_json`。

## 2. 状态码约定

任务状态：

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`

复核状态：

- `REVIEW_PENDING`
- `REVIEWED`
- `REJECTED`

AI 实现类型：

- `MOCK`
- `HEURISTIC`
- `ML_MODEL`
- `RAG`
- `GENERAL_LLM`

## 3. AI 协同原则

当前 AI 方案为通用大模型 + 知识图库，不采用业务专用微调大模型作为主路线。

协同边界：

- Java 管业务；
- Python 管 AI/RAG；
- 通用大模型只生成解释和问答；
- 规则和阈值决定业务状态；
- 医生复核决定最终医疗意见。

## 4. Callback 兼容约定

新增字段优先进入：

- `raw_result_json`
- `assessment_report_json`
- RAG response 扩展字段

避免随意修改：

- callback 顶层 DTO；
- visualAssets 顶层结构；
- Java 业务表主键和状态字段。

## 5. 日志约定

必须记录：

- `taskNo`
- `traceId`
- runtime mode；
- implType；
- errorCode；
- inferenceMillis。

不得记录：

- 明文身份证；
- 明文手机号；
- 不必要的患者姓名；
- 大段 prompt 敏感内容。

## 6. RAG 命名

建议字段：

- `knowledgeBaseCode`
- `documentCode`
- `chunkId`
- `retrievalScore`
- `citations`
- `safetyFlags`
- `llmProviderCode`
- `llmModelName`

RAG 回答要保留引用，不能只返回纯文本。

## 7. Phase 5C 命名

统一字段：

- `gradingMode`
- `gradingImplType`
- `gradingLabel`
- `confidenceScore`
- `uncertaintyMode`
- `uncertaintyImplType`
- `uncertaintyScore`
- `needsReview`

这些字段大小写不得漂移。
