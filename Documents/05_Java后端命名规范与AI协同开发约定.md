# Java后端命名规范与AI协同开发约定

本文档统一当前数据库、Java 代码、接口、事件、AI 协同对象的命名口径，并补充与当前实现一致的边界说明。

## 1. 总体原则

1. 同一业务语义在数据库、Java、接口和联调文档中保持一致。
2. 命名优先可读性，避免生造缩写。
3. 当前已落地的命名必须跟代码一致，不能按理想设计重写历史实现。
4. 治理类设计名词可以保留，但必须标注“当前未落地”。

## 2. 数据库命名规范

### 2.1 表名前缀

当前已使用的域前缀：
- `sys_`：系统管理
- `pat_`：患者主档
- `med_`：病例与医疗业务
- `ana_`：分析任务与结果
- `rpt_`：报告
- `fup_`：随访
- `msg_`：消息通知

### 2.2 字段命名

当前统一约定：
- 主键：`id`
- 编号：`*_no`
- 外键：`*_id`
- 类型码：`*_code`
- 标记位：`*_flag`
- JSON：`*_json`
- 时间点：`*_at`
- 日期：`*_date`

### 2.3 当前应特别强调的字段

| 字段 | 说明 |
| --- | --- |
| `org_id` | 机构隔离基础字段 |
| `deleted_flag` | 逻辑删除 |
| `case_status_code` | 病例主状态机字段 |
| `quality_status_code` | 图像质检结果核心状态 |
| `model_version` | 当前模型版本留痕字段 |
| `retry_from_task_id` | 分析任务重试链路字段 |
| `trigger_source_code` / `trigger_ref_id` | 随访触发幂等字段 |

## 3. Java 包和类命名规范

### 3.1 模块内包结构

当前建议固定：
- `controller`
- `app`
- `domain.model`
- `domain.service`
- `domain.repository`
- `infrastructure.*`
- `interfaces.command`
- `interfaces.dto`
- `interfaces.vo`
- `interfaces.query`

### 3.2 类命名

当前应遵守：
- Controller：`XxxController`
- 应用服务：`XxxAppService`
- 领域服务：`XxxDomainService`
- Repository 接口：`XxxRepository`
- Repository 实现：`XxxRepositoryImpl`
- Command：`CreateXxxCommand` / `UpdateXxxCommand`
- DTO：`XxxDTO`
- VO：`XxxVO`

### 3.3 当前真实示例

| 类型 | 示例 |
| --- | --- |
| Controller | `AnalysisTaskController` |
| AppService | `AnalysisTaskAppService` |
| DomainService | `AnalysisCallbackDomainService` |
| DTO | `AiAnalysisRequestDTO` `AiAnalysisCallbackDTO` |
| VO | `ReportGenerateResultVO` |
| Command | `CreateAnalysisTaskCommand` |

## 4. API 命名规范

### 4.1 REST 路径

当前统一风格：
- 版本前缀：`/api/v1`
- 资源名用复数
- 病例下游资源使用子资源路径

示例：
- `/api/v1/patients`
- `/api/v1/visits`
- `/api/v1/cases`
- `/api/v1/cases/{caseId}/images`
- `/api/v1/analysis/tasks`
- `/api/v1/reports/{reportId}/export`

### 4.2 当前不应误写的路径

不要写成：
- `/api/v1/ai/tasks` 当前并不存在
- `/api/v1/followups` 当前并不是已实现主路径

## 5. AI 协同命名约定

### 5.1 当前已存在对象

| 对象 | 当前状态 |
| --- | --- |
| `AiAnalysisRequestDTO` | 已实现 |
| `AiAnalysisCallbackDTO` | 已实现 |
| `AnalysisRequestedEvent` | 已实现 |
| `AnalysisCompletedEvent` | 已实现 |
| `AnalysisFailedEvent` | 已实现 |

### 5.2 当前建议冻结的命名

建议保持以下命名不再轻易变动：
- `taskNo`
- `taskStatusCode`
- `modelVersion`
- `summary`
- `rawResultJson`
- `visualAssets`
- `riskAssessment`
- `errorMessage`

建议新增并冻结：
- `traceId`
- `inferenceMillis`

### 5.3 事件命名

当前真实事件名：
- `analysis.requested`
- `analysis.completed`
- `analysis.failed`

对应 routing key：
- `analysis.requested`
- `analysis.completed`
- `analysis.failed`

### 5.4 资源访问字段命名建议

当前消息里只有：
- `bucketName`
- `objectKey`

建议后续统一命名增加：
- `accessUrl`
- `accessExpireAt`
- `storageProviderCode`
- `localStoragePath`
- `attachmentMd5`

## 6. 对象存储命名修正建议

当前问题：
- local 配置默认 provider code 为 `MINIO`
- `MINIO` 对应 `MinioObjectStorageService`，`LOCAL_FS` 对应 `LocalObjectStorageService`

建议统一口径：
- `LOCAL_FS`
- `MINIO`
- `OSS`

文档要求：
- 当前本地 profile 默认写 `MINIO`；只有 E2E 或共享卷联调才写 `LOCAL_FS`
- MinIO 已经接入实现；OSS 仍只是预留 provider 口径

## 7. ModelAdmin 与 Risk 术语使用规则

### 7.1 ModelAdmin

允许写：
- 模型治理能力
- 模型版本治理方向
- 候选模型 / 审批上线流程

不允许写：
- 当前已存在独立 `ModelAdmin` 模块
- 当前已有独立 `ModelAdmin` API / 表 / 管理端

### 7.2 Risk

允许写：
- 风险评估能力
- 风险等级结果
- 风险分布统计

不允许写：
- 当前已存在独立 `Risk` 模块
- 当前已有独立 `caries-risk`

## 8. 文档与代码统一规则

文档里必须与当前代码保持一致的事实：
1. `LOCAL_FS` 本地对象存储实现是 `LocalObjectStorageService`，默认 `MINIO` 实现是 `MinioObjectStorageService`
2. 风险评估不是独立模块
3. ModelAdmin 不是当前已实现模块
4. local profile 分析消息模式是 `rabbit`
5. 导出接口当前是导出审计，不是完整下载接口
6. PDF 当前是极简 ASCII 生成

## 9. 建议新增命名对象

为后续治理扩展保留的推荐命名：
- `ana_model_version_registry`
- `training_candidate_flag`
- `desensitized_export_flag`
- `dataset_snapshot_no`
- `review_status_code`
- `reviewed_by`
- `reviewed_at`

这些名字可以进入设计文档，但必须标注：
- 当前建议新增
- 当前数据库未落地

