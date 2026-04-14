# Java后端开发说明书

本文档按当前 `backend-java` 代码实现编写，适合后端开发、测试联调和 AI 协同开发共同使用。

## 1. 开发基线

### 1.1 技术栈

当前真实基线：
- JDK `17`
- Spring Boot `3.2.12`
- Spring Security 6
- MyBatis-Plus `3.5.7`
- MySQL + Flyway
- Redis
- RabbitMQ
- SpringDoc OpenAPI
- BCrypt 密码编码器

当前不应写入为已实现基线的内容：
- MinIO 已落地
- 独立 Python AI 服务工程已交付
- 模型治理平台已交付
- 标注平台已交付
- 独立 `Risk` 模块已交付

### 1.2 工程结构

当前工程采用多模块 Maven：
- `caries-boot`
- `caries-common`
- `caries-framework`
- `caries-system`
- `caries-patient`
- `caries-image`
- `caries-analysis`
- `caries-report`
- `caries-followup`
- `caries-dashboard`
- `caries-integration`

### 1.3 分层约定

当前主要分层：
- `controller`：HTTP 接口层
- `app`：应用服务层
- `domain`：领域模型、领域服务、仓储接口
- `infrastructure`：仓储实现、消息、外部资源适配
- `interfaces.command / dto / vo / query`：接口模型

## 2. 当前必须遵守的开发原则

1. 所有病例状态变更必须走 `CaseCommandAppService`。
2. 分析模块和报告模块不得旁路直接修改 `med_case`。
3. 当前对象存储默认 provider 为 `MINIO`，对应 `MinioObjectStorageService`；`LOCAL_FS` 对应 `LocalObjectStorageService`。
4. `modelVersion` 当前只是留痕基础，不得在没有表、API、管理端的情况下宣称已实现模型治理平台。
5. 风险评估是能力，不是当前独立模块。
6. 业务平台数据库与训练数据治理平台必须分开表述。

## 3. 当前模块开发重点

### 3.1 `caries-system`

当前职责：
- 登录认证
- 当前用户与权限查询
- 用户/角色/菜单管理
- 字典与配置查询
- 数据权限规则维护

关键类：
- `AuthAppService`
- `SystemAdminQueryAppService`
- `SystemUserCommandAppService`
- `SystemRoleCommandAppService`
- `SystemMenuCommandAppService`
- `SystemDataPermissionRuleAppService`

当前问题：
- `sys_menu` 由 Flyway `V014__14_minio_model_governance_and_permissions.sql` 初始化业务菜单
- 默认只有 `SYS_ADMIN`
- 菜单授权和普通角色演示不完整

开发建议：
- 通过 Flyway 增加角色/菜单种子
- 先补 `ORG_ADMIN`、`DOCTOR`、`SCREENER`

### 3.2 `caries-patient`

当前职责：
- 患者、监护人、就诊、病例、诊断、牙位记录
- 病例状态机与状态日志

关键类：
- `PatientCommandAppService`
- `VisitCommandAppService`
- `CaseCommandAppService`
- `CaseClinicalRecordAppService`
- `CaseStatusMachine`

开发要求：
- 任何新增业务都要围绕病例主线组织
- `source_image_id` 等追溯字段必须与字典和查询口径保持一致

### 3.3 `caries-image`

当前职责：
- 文件上传
- 附件元数据
- 病例图像
- 图像质检
- 本地对象存储

关键类：
- `AttachmentAppService`
- `CaseImageAppService`
- `LocalObjectStorageService`
- `ObjectStorageService`

当前问题：
- provider code 默认值为 `MINIO`
- 实际实现却是本地文件系统

必须修正文档和配置口径：
- `application-local.yml` 默认表达 `MINIO`，`application-e2e.yml` 表达 `LOCAL_FS`
- 文档中统一把当前存储写成 Local FS

建议补强：
- 新增分析专用取图接口
- 或在分析消息中直接下发签名访问地址

### 3.4 `caries-analysis`

当前职责：
- 分析任务创建与重试
- Rabbit/Logging 任务发布
- AI 回调验签与写回
- 医生纠偏反馈

关键类：
- `AnalysisTaskAppService`
- `AnalysisCallbackAppService`
- `AnalysisTaskDomainService`
- `AnalysisIdempotencyDomainService`
- `AnalysisCallbackDomainService`
- `RabbitAnalysisTaskEventPublisher`
- `AiCallbackSignatureVerifier`

当前事实：
- `local` profile 默认 `rabbit`
- AI 请求载荷当前只发 `bucketName + objectKey`
- 回调 DTO 当前是 `AiAnalysisCallbackDTO`
- 回调已具备重复终态 ACK 和晚到回调保护

建议补强：
1. `AiAnalysisRequestDTO` 增加可直接取图字段
2. 回调契约正式冻结，避免只靠示例 JSON 联调
3. `modelVersion` 从留痕字段升级为可统计、可审计字段
4. 增强幂等回调测试覆盖

### 3.5 `caries-report`

当前职责：
- 模板管理
- 报告生成
- PDF 附件落盘
- 导出审计

关键类：
- `ReportAppService`
- `ReportTemplateAppService`
- `ReportDomainService`
- `ReportTemplateResolver`
- `ReportRenderService`
- `ReportPdfService`

当前事实：
- 支持 `DOCTOR` / `PATIENT` 两类报告类型
- 默认模板已内置
- `exportReport` 当前只写 `rpt_export_log`
- `ReportPdfService` 当前只支持 ASCII

建议补强：
1. 升级 PDF 生成方案，支持中文、图片、基础表格与更好的版式
2. 导出接口升级成“审计 + 下载能力”

### 3.6 `caries-followup`

当前职责：
- 计划、任务、记录管理
- 报告触发随访

关键类：
- `FollowupPlanAppService`
- `FollowupTaskAppService`
- `FollowupRecordAppService`
- `FollowupTriggerService`
- `FollowupDomainService`

当前事实：
- 高风险或建议复查会自动触发随访
- 幂等键为 `case_id + trigger_source_code + trigger_ref_id`
- 任务完成或取消后会自动收口计划

### 3.7 `caries-dashboard`

当前职责：
- 业务总览、状态分布、风险分布、任务汇总、积压、趋势、模型运行摘要

关键类：
- `DashboardStatsRepository`
- `DashboardOverviewAppService`
- `DashboardTrendAppService`
- `DashboardOpsMetricsAppService`

当前不足：
- AI 治理维度不够丰富

建议补强：
- 按模型版本聚合
- 平均推理时长
- 高不确定性占比
- reviewSuggestedFlag 占比
- 修正反馈率

## 4. AI 协同开发约束

### 4.1 当前已冻结的最小协同口径

Java 已提供：
- 分析任务 DTO：`AiAnalysisRequestDTO`
- 分析回调 DTO：`AiAnalysisCallbackDTO`
- 回调签名校验：`AiCallbackSignatureVerifier`
- 事件名：`analysis.requested` / `analysis.completed` / `analysis.failed`

### 4.2 当前未冻结但建议立刻冻结的内容

建议新增或补强：
- `AiAnalysisResultCallbackCommand` 作为正式回调 command
- `traceId`
- `inferenceMillis`
- 标准错误码/错误分类
- 标准 visual asset 类型码

### 4.3 当前文档必须说清的边界

- 当前仓库没有 Python AI 服务实现
- 当前仓库没有训练数据集治理实现
- 当前消息载荷还不适合异构部署时直接取图

## 5. 测试与验证要求

### 5.1 已有关键测试主线

已通过的关键测试应继续保留：
- 主链路 E2E
- 分析到报告
- 报告到随访
- 随访幂等
- dashboard 总览和趋势

### 5.2 必须继续补的测试

#### `caries-analysis`
- 同一 `SUCCESS` 回调重复两次，不重复写 summary / risk / visualAssets
- `FAILED -> retry -> old SUCCESS late arrival` 不覆盖新链路
- `PROCESSING -> SUCCESS` 时间链完整
- `FAILED` 时病例正确回退到 `QC_PENDING`

#### `caries-report`
- 报告类型 `DOCTOR` / `PATIENT` 都可生成
- 报告版本递增逻辑正确
- 导出审计记录正确

#### `caries-system`
- 普通角色菜单与权限联动
- 数据权限范围下推查询正确

## 6. 数据库与 Flyway 补强建议

建议优先级最高的结构补强：
1. `ana_model_version_registry`
2. `ana_correction_feedback` 增加训练准入治理字段
3. 继续核查牙位记录中的 `source_image_id` 追溯链是否贯通

## 7. 当前交付说明

当前 Java 后端可以被准确描述为：
- 已完成核心业务闭环
- 已具备 AI 联调接口和回调承载
- 已具备风险结果、报告、随访和看板能力

当前 Java 后端不能被准确描述为：
- 已完成模型治理平台
- 已完成训练数据治理平台
- 已完成正式对象存储平台
- 已完成生产级报告排版系统

