# 项目当前实现报告

更新日期：2026-04-15

本文档替换旧的过程性报告，只保留当前实现结论、模块能力、关键类、接口和边界。

## 1. 当前实现结论

1. Java 后端已实现龋齿筛查业务平台主链路。
2. MinIO 已保留并开发完成，默认 provider 为 `MINIO`。
3. 本地文件系统 provider `LOCAL_FS` 已保留为 e2e 和本地兼容方案。
4. AI 分析请求已经携带 `accessUrl`、`accessExpireAt`、`storageProviderCode`、`attachmentMd5`、`localStoragePath`。
5. AI 回调已经冻结为 `AiAnalysisResultCallbackCommand`。
6. 模型版本治理已有最小数据库落点 `ana_model_version_registry`。
7. 看板已有模型运行质量指标。
8. 报告导出返回审计记录和可下载 URL。
9. 数据库基线已补角色、菜单、权限和数据权限规则种子。

## 2. 已实现模块

| 模块 | 当前能力 |
| --- | --- |
| `caries-system` | 登录、当前用户、权限、用户、角色、菜单、字典、配置、数据权限规则 |
| `caries-patient` | 患者、就诊、病例、诊断、牙位记录、病例状态机 |
| `caries-image` | 文件上传、MinIO/LOCAL_FS 对象存储、短时访问 URL、影像、质检 |
| `caries-analysis` | 分析任务、重试、MQ 投递、Python 回调、摘要、视觉资产、风险、修正反馈 |
| `caries-report` | 模板、报告生成、PDF、导出审计、下载 URL |
| `caries-followup` | 随访计划、任务、记录 |
| `caries-dashboard` | 概览、病例分布、风险分布、随访摘要、待办、趋势、模型运行质量 |

## 3. 关键改动结论

| 项 | 当前状态 |
| --- | --- |
| MinIO | 已实现 `MinioObjectStorageClient`，local 默认启用 |
| LocalFS | 已实现 `ImageObjectStorageServiceAdapter`，e2e 默认使用 |
| AI 请求 | `AiAnalysisRequestDTO.ImageItem` 已补取图字段 |
| AI 回调 | `AiAnalysisResultCallbackCommand` 已替代旧回调 DTO |
| 模型版本 | `ana_model_version_registry` 已新增，dashboard 可统计 |
| 推理质量 | `trace_id`、`inference_millis` 已入 `ana_task_record` |
| 修正反馈治理 | `training_candidate_flag` 等字段已入 `ana_correction_feedback` |
| 报告 PDF | `ReportPdfService` 已改为 PDFBox，支持中文字体候选 |
| 报告导出 | `ReportExportResultVO` 已返回 `downloadUrl` |
| 权限种子 | 数据库基线已补 ORG_ADMIN、DOCTOR、SCREENER、菜单、数据权限 |

## 4. 当前仍不是独立模块的内容

1. `Risk` 不是独立模块，风险由 `med_risk_assessment_record` 和 analysis/dashboard/report/followup 承载。
2. `ModelAdmin` 不是独立模块，当前只有模型版本登记表和统计字段。
3. 标注平台未在当前仓库实现。
4. 训练数据快照平台未在当前仓库实现。
5. 完整 Python AI 服务源码未在当前仓库实现。
6. 完整前端源码未在当前仓库实现。

## 5. 验证口径

开发验证应优先运行：

```powershell
cd E:\caries_guard\backend-java
mvn -q -DskipTests compile
mvn -q test
```

MinIO 联调需要单独启动 MinIO 服务，并保证以下配置可用：

```powershell
$env:CARIES_STORAGE_PROVIDER='MINIO'
$env:CARIES_MINIO_ENDPOINT='http://127.0.0.1:9000'
$env:CARIES_MINIO_ACCESS_KEY='minioadmin'
$env:CARIES_MINIO_SECRET_KEY='minioadmin'
```

当前长期运行口径为 MinIO；如果不启动 MinIO，只能运行不触发真实对象写入的单元测试或使用测试桩。
