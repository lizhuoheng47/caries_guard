# Phase 3 RAG Docker E2E 执行说明

## 目标

验证 Phase 3 不是“代码接好”，而是在 Docker 环境中真实闭环：

- Java 容器可访问 Python RAG 服务。
- 知识文档可导入并重建索引。
- Java 医生问答接口可返回 Python RAG 结果和引用。
- 患者版报告生成会触发 RAG 患者解释。
- Python `rag_request_log`、`rag_retrieval_log`、`llm_call_log` 有真实落库记录。
- Java 不直接读取 `caries_ai` 表。

## 自动化脚本

仓库提供 PowerShell 验收脚本：

```powershell
.\scripts\phase3-rag-docker-e2e.ps1
```

默认行为：

1. 执行 `docker compose up -d --build`
2. 等待 Java `/actuator/health`
3. 等待 Python `/ai/v1/health`
4. 导入 3 篇 APPROVED 知识文档
5. 调用 `/ai/v1/knowledge/rebuild`
6. 登录 Java `admin / 123456`
7. 调用 Java `/api/v1/rag/doctor-qa`
8. 在 `caries_biz` 种入一组最小“已分析病例”数据
9. 调用 Java 患者版报告生成接口
10. 查询 Python RAG 日志表
11. 归档响应、SQL 和容器日志

如果容器已经启动，可跳过 compose up：

```powershell
.\scripts\phase3-rag-docker-e2e.ps1 -SkipComposeUp
```

如端口被占用，可覆盖地址：

```powershell
.\scripts\phase3-rag-docker-e2e.ps1 `
  -JavaBaseUrl "http://127.0.0.1:18080" `
  -PythonBaseUrl "http://127.0.0.1:18001"
```

## 关键配置

Docker profile 中 Java 默认调用：

```text
http://backend-python:8001/ai/v1
```

宿主机默认可访问 Python：

```text
http://127.0.0.1:8001/ai/v1
```

RAG 超时变量兼容以下命名：

```text
CARIES_RAG_CONNECT_TIMEOUT_MS
CARIES_RAG_READ_TIMEOUT_MS
CARIES_RAG_CONNECT_TIMEOUT_MILLIS
CARIES_RAG_REQUEST_TIMEOUT_MILLIS
```

## 验收证据

脚本输出目录默认位于：

```text
e2e-artifacts/phase3-rag-YYYYMMDD-HHmmss
```

核心文件：

- `knowledge-import-responses.json`
- `knowledge-rebuild-response.json`
- `java-doctor-qa-response.json`
- `java-patient-report-response.json`
- `java-patient-report-detail.json`
- `rag-request-log.sql.txt`
- `rag-retrieval-log.sql.txt`
- `llm-call-log.sql.txt`
- `docker-logs-tail.txt`
- `phase3-rag-e2e-summary.json`

## 判定标准

脚本成功结束并输出：

```text
Phase 3 RAG Docker E2E passed.
```

才可判定 Phase 3 Docker E2E 通过。

任一项失败都会抛出错误，包括：

- 医生问答返回 fallback
- 医生问答无引用
- 患者报告未包含 `patientExplanation` 证据
- 患者报告使用 Java fallback 文案
- Python RAG 请求日志不足
- Python 检索日志不足
- Python LLM 调用日志不足
- Java 源码直接引用 `caries_ai` 或 RAG 内部日志表
