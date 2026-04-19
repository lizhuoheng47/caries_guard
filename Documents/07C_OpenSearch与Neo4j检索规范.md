# 07C OpenSearch 与 Neo4j 检索规范

## 1. OpenSearch 索引

### 1.1 `kb_chunk_index`

固定字段：

- `chunkId: keyword`
- `docId: keyword`
- `docNo: keyword`
- `kbCode: keyword`
- `docTitle: text`
- `sourceUri: keyword`
- `chunkText: text`
- `medicalTags: keyword`
- `graphEntityRefs: keyword`
- `publishStatus: keyword`
- `versionNo: keyword`
- `lexicalText: text`
- `denseVector: knn_vector`
- `embeddingProvider: keyword`
- `embeddingModel: keyword`
- `embeddingVersion: keyword`
- `sourceType: keyword`
- `sourceAuthorityScore: float`
- `freshnessScore: float`
- `createdAt: date`
- `updatedAt: date`

固定约束：

1. `_id` 使用 `chunk.id`。
2. 检索只允许命中 `publishStatus=PUBLISHED`。
3. 删除文档时必须同步执行 `delete_document_chunks(doc_id)`。

### 1.2 `kb_doc_index`

固定字段：

- `docId: keyword`
- `docNo: keyword`
- `docTitle: text`
- `versionNo: keyword`
- `reviewStatus: keyword`
- `publishStatus: keyword`
- `sourceType: keyword`
- `sourceAuthorityScore: float`
- `embeddingProvider: keyword`
- `embeddingModel: keyword`
- `embeddingVersion: keyword`
- `tags: keyword`
- `createdAt: date`
- `updatedAt: date`

## 2. OpenSearch 查询模板

### 2.1 lexical retrieval

```json
{
  "size": 20,
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "{{query}}",
            "fields": [
              "lexicalText^2",
              "docTitle^1.4",
              "medicalTags^1.1",
              "graphEntityRefs^1.2"
            ]
          }
        }
      ],
      "filter": [
        { "term": { "kbCode": "{{kbCode}}" } },
        { "term": { "publishStatus": "PUBLISHED" } }
      ]
    }
  }
}
```

### 2.2 dense retrieval

```json
{
  "size": 20,
  "query": {
    "bool": {
      "must": [
        {
          "knn": {
            "denseVector": {
              "vector": "{{queryVector}}",
              "k": 20
            }
          }
        }
      ],
      "filter": [
        { "term": { "kbCode": "{{kbCode}}" } },
        { "term": { "publishStatus": "PUBLISHED" } }
      ]
    }
  }
}
```

## 3. Neo4j schema

当前实现固定节点：

- `Concept { conceptId, name, entityTypeCode, normalizedName, aliases }`
- `AliasTerm { name, normalizedName }`
- `EvidenceDocument { docId, title, versionNo }`
- `EvidenceChunk { chunkId, docId }`

当前实现固定关系：

- `(:AliasTerm)-[:ALIAS_OF]->(:Concept)`
- `(:EvidenceChunk)-[:PART_OF]->(:EvidenceDocument)`
- `(:EvidenceChunk)-[:MENTIONS]->(:Concept)`
- `(:EvidenceChunk)-[:SUPPORTED_BY]->(:Concept)`
- `(:Concept)-[:SUGGESTS|REQUIRES_FOLLOWUP|RECOMMENDED_FOR|APPLIES_TO|CONTRAINDICATED_FOR|HAS_RISK_FACTOR|INDICATES|RELATED_TO]->(:Concept)`

## 4. Upsert 规则

1. 同一文档同步图谱前，先删除该文档已有 `EvidenceChunk`、`EvidenceDocument` 与带 `docId` 的关系。
2. `Concept` 通过 `conceptId` 幂等 `MERGE`。
3. `AliasTerm` 通过 `name` 幂等 `MERGE`。
4. 文档级关系在 `MERGE (s)-[r:REL]->(t)` 后写入 `relationCode`、`docId`、`evidenceChunkId`。

示例：

```cypher
MERGE (n:Concept {conceptId: $concept_id})
SET n.name = $concept_name,
    n.entityTypeCode = $entity_type_code,
    n.normalizedName = $normalized_name,
    n.aliases = $aliases
```

## 5. 图谱查询模板

系统已实现的查询模板围绕以下场景：

- `RiskFactor -> Recommendation`
- `Severity -> FollowUpInterval`
- `ImagingFinding -> Disease`
- `Disease -> RiskFactor`
- `Population -> Recommendation`
- `Recommendation -> EvidenceDocument`
- `ToothPosition -> EvidenceDocument`

固定要求：

1. 图谱结果必须回写 `rag_graph_retrieval_log`。
2. 图谱证据必须能映射为 `graphEvidence` 输出。

## 6. 三路融合与阈值

### 6.1 通道权重

默认配置：

- `LEXICAL = 1.0`
- `DENSE = 1.15`
- `GRAPH = 1.2`

### 6.2 加分项

- 来源权威分：`source_authority_score * 0.05`
- 新鲜度分：`freshness_score * 0.03`
- 图谱置信分：`graph_confidence_score * 0.2`

### 6.3 拒答阈值

以下任一成立立即拒答：

1. 证据数小于 `CG_RAG_EVIDENCE_MIN_COUNT`。
2. 命中文档数为 `0`。
3. 问题包含 prompt injection 关键词。
4. 问题要求诊断或处方。

## 7. 删除与回滚

1. 发布新版本前，文档旧 chunk 必须从 OpenSearch 删除。
2. rebuild 时若 `cleanup_stale=true`，先删文档旧图再重建。
3. rollback 不恢复“旧索引快照”，而是对目标版本重新执行 publish/index/sync。
