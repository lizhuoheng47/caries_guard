# CariesGuard Backend

## 当前基线

本文档按 `2026-04-14` 仓库代码、Flyway 迁移和本机 `cg` 数据库真实内容重写。

当前项目是 `Spring Boot 3.2.12 + JDK 17` 的多模块单体后端，已经实现以下业务域：

- `system`：认证、当前用户、字典、配置、用户/角色/菜单/数据权限规则管理
- `patient`：患者、监护人、就诊、病例、诊断、牙位记录、病例状态流转
- `image`：文件上传、病例影像关联、签名访问、影像质检
- `analysis`：AI 分析任务创建/重试、回调写回、医生修正反馈
- `report`：报告模板、报告生成、报告查询、导出审计
- `followup`：随访计划、任务、记录、报告触发自动随访
- `dashboard`：总览、分布、积压、模型运行情况、趋势统计

## 当前数据库现状

本地默认库为 `cg`，`application-local.yml` 配置如下：

- host: `127.0.0.1:3306`
- database: `cg`
- username: `root`
- password: `1234`

`cg` 当前共有 `38` 张表，其中：

- `13` 条 Flyway 迁移历史
- `37` 张业务表/系统表
- 当前有真实种子数据的表主要是：
  - `sys_dept = 1`
  - `sys_role = 1`
  - `sys_user = 1`
  - `sys_user_role = 1`
  - `sys_dict_type = 4`
  - `sys_dict_item = 18`
- 其余业务表在当前本地库中基本为空，说明仓库已具备闭环代码，但默认演示库未预置业务样例数据

默认开发管理员：

- username: `admin`
- password: `123456`
- role: `SYS_ADMIN`

## 代码结构

- `caries-common`：统一响应、错误码、异常、TraceId
- `caries-framework`：Security、JWT、OpenAPI、敏感数据处理
- `caries-system`：认证、后台管理、字典/配置/权限
- `caries-patient`：患者、就诊、病例、状态机
- `caries-image`：附件、影像、质检、本地对象存储
- `caries-analysis`：AI 任务、回调、纠偏反馈、事件发布
- `caries-report`：模板、报告、导出审计
- `caries-followup`：计划、任务、记录、通知、自动触发
- `caries-dashboard`：统计聚合 SQL 与接口
- `caries-integration`：集成支撑模块
- `caries-boot`：启动工程与 E2E/真库集成测试

## 关键实现事实

- 统一接口前缀为 ` /api/v1/** `
- 统一返回体为 `ApiResponse<T>`
- 鉴权使用 JWT Bearer Token
- `org_id` 是主要机构隔离锚点
- 患者/用户等敏感字段使用 `AES-GCM + HMAC-SHA256 + 脱敏串`
- 病例主状态机已经落在 `CaseStatusMachine`
- 文件访问使用签名 URL，`/api/v1/files/{attachmentId}/content` 为签名保护的公开入口
- AI 回调使用 `X-AI-Timestamp` 和 `X-AI-Signature` 进行 HMAC 验签
- 本地 profile 下 AI 事件发布模式为 `rabbit`
- E2E profile 下 AI 事件发布模式为 `logging`

## 运行要求

最低要求：

- JDK 17
- Maven 3.9+
- MySQL 8.x
- RabbitMQ（本地 profile 要创建分析任务时需要）

注意：

- Redis 在配置中存在，但当前业务代码没有形成显式依赖链路
- 对象存储当前实现为本地文件系统，默认目录为 `${user.dir}/var/image-storage`
- 对象存储统一使用 `caries.storage`，当前长期实现为 `caries-integration` 的 `MinioObjectStorageClient`

## 启动方式

```bash
mvn -pl caries-boot -am spring-boot:run
```

默认访问地址：

- app: `http://127.0.0.1:8080`
- OpenAPI: `http://127.0.0.1:8080/v3/api-docs`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- ping: `http://127.0.0.1:8080/api/v1/system/ping`

## 测试

运行全部测试：

```bash
mvn test
```

运行 boot 真库关键链路：

```bash
mvn -q -pl caries-boot -am "-Dtest=MainlineWorkflowE2ETest,AnalysisToReportE2ETest,AnalysisReportFollowupE2ETest,FollowupTriggerIdempotencyE2ETest,DashboardOverviewIntegrationTest,DashboardTrendIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

仓库中已存在的关键测试类型：

- system / analysis 模块单测
- boot 真库 E2E
- dashboard 真库集成测试
- analysis -> report -> followup 跨模块链路测试

## 当前主要接口分组

- 认证：`/api/v1/auth/*`
- 系统管理：`/api/v1/system/*`
- 患者：`/api/v1/patients`
- 就诊：`/api/v1/visits`
- 病例：`/api/v1/cases`
- 文件：`/api/v1/files/*`
- 影像：`/api/v1/cases/{caseId}/images`、`/api/v1/images/{imageId}/quality-checks`
- 分析：`/api/v1/analysis/tasks`、`/api/v1/internal/ai/callbacks/analysis-result`
- 报告：`/api/v1/cases/{caseId}/reports`、`/api/v1/report-templates`
- 随访：`/api/v1/cases/{caseId}/followup/*`、`/api/v1/followup/*`
- 看板：`/api/v1/dashboard/*`

## 当前已知限制

- `sys_menu` 当前本地库无种子数据，普通角色权限菜单体系仍需补完整初始化数据
- 数据权限规则表与服务已实现，但跨 patient/image/report/followup 的细粒度数据域裁剪尚未全面接入，当前主要依赖 `org_id` 检查
- 报告导出接口当前只记录 `rpt_export_log`，不直接返回文件流
- `ReportPdfService` 生成的是极简 ASCII PDF，中文内容会退化为 `?`
- 报告模板更新不会自动递增 `version_no`
- 仓库内没有 AI 消费者或 Python 服务实现，只有发布契约与回调契约

## 文档索引

建议优先阅读：

- `docs/00_后端V1基线冻结与问题总解决方案.md`
- `docs/01_数据库与Flyway整改方案.md`
- `docs/03_核心API契约与模块接口规范.md`
- `docs/15_部署与环境说明.md`
- `docs/16_API契约与当前实现差异清单.md`
- `docs/17_测试结果摘要与答辩证据清单.md`
