# 05 Java 类与方法参考

## 1. 分层约定

| 层 | 职责 |
| --- | --- |
| Controller | HTTP 入参、鉴权、响应 |
| AppService | 用例编排 |
| DomainService | 业务规则 |
| Repository/Mapper | 数据访问 |
| IntegrationClient | 外部服务调用 |

## 2. Analysis 关键类

建议职责：

- `AnalysisTaskController`：创建和查询任务；
- `AnalysisTaskAppService`：任务编排；
- `AnalysisCallbackController`：接收 Python callback；
- `AnalysisCallbackAppService`：落库和状态推进；
- `VisualAssetService`：保存 visual asset 附件；
- `ReviewRoutingService`：根据 `needsReview` 推进复核。

## 3. RAG 关键类

建议职责：

- `RagController`：问答入口；
- `RagAppService`：鉴权和上下文组装；
- `PythonRagClient`：调用 Python RAG；
- `RagResponseAssembler`：引用和安全标记展示。

## 4. 禁止逻辑

Java 不应：

- 直接实现影像模型；
- 直接调用通用大模型 provider；
- 管理微调训练任务；
- 修改 `caries_ai`；
- 将 RAG 文本直接写成临床最终结论。

## 5. rawResultJson 解析

解析时应容忍未知字段。强依赖字段仅限：

- `mode`
- `gradingLabel`
- `uncertaintyScore`
- `needsReview`
