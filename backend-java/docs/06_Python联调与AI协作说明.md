# 06 Python 联调与 AI 协作说明

## 1. 协作模式

Analysis 使用 MQ + callback。RAG 使用 HTTP。

## 2. Analysis 联调

Java：

1. 创建 task；
2. 发送 MQ；
3. 等待 Python callback；
4. 落库；
5. 推进 report/review。

Python：

1. 消费 task；
2. 下载影像；
3. 执行 AI pipeline；
4. 上传 visual assets；
5. callback Java。

## 3. RAG 联调

Java 提供：

- 用户角色；
- 问题；
- 病例上下文；
- 可访问知识库范围；
- trace id。

Python 返回：

- answer；
- citations；
- retrievedChunks；
- confidence；
- safetyFlags；
- trace id。

## 4. 通用大模型方案

当前不做业务专用大模型微调。Python 通过知识图库、结构化上下文和通用大模型 provider 生成答案。

## 5. Phase 5C 验收字段

Java 必须能消费：

- `gradingMode`
- `gradingImplType`
- `uncertaintyMode`
- `uncertaintyImplType`
- `uncertaintyScore`
- `needsReview`

## 6. Docker E2E

运行 Phase 5C：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\phase5-analysis-docker-e2e.ps1 -SkipComposeUp -Phase5COnly -WaitSeconds 180
```
