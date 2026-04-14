# CariesGuard Backend

## 当前状态

`backend-java` 当前已经不是工程骨架阶段，而是进入 `P8 测试与交付收口` 阶段。

已完成的主模块：

- `system`
- `patient`
- `image`
- `analysis`
- `report`
- `followup`
- `dashboard`

当前已具备：

- Spring Boot 3.2.12 + JDK 17 多模块单体工程
- MySQL + Flyway 迁移
- JWT 登录与当前用户查询
- 敏感字段加密 / 哈希 / 脱敏
- 病例状态机与状态日志
- 影像上传、影像建档、质检
- AI 任务创建、回调写回、修正反馈
- 报告生成、版本归档、导出审计
- 随访计划、任务、记录、通知留痕
- dashboard 聚合接口
- boot 真库 E2E 与集成测试

当前仍未切真实外部依赖：

- RabbitMQ 仍为 logging publisher 阶段
- 对象存储生产形态未切 MinIO / OSS，当前默认本地文件存储

## 模块

- `caries-common`: 通用响应、异常、TraceId
- `caries-framework`: 安全、JWT、OpenAPI、Web 基础设施
- `caries-system`: 认证、系统管理、字典与配置
- `caries-patient`: 患者、就诊、病例、状态机
- `caries-image`: 附件、影像、质检、本地对象存储
- `caries-analysis`: AI 任务、回调、修正反馈
- `caries-report`: 模板、报告、导出审计
- `caries-followup`: 随访计划、任务、记录、通知
- `caries-dashboard`: 管理端统计聚合
- `caries-integration`: 集成测试与联调支撑
- `caries-boot`: 启动工程

## 运行环境

最低要求：

- JDK 17
- Maven 3.9+
- MySQL 8

当前本地默认配置在：

- `caries-boot/src/main/resources/application.yml`
- `caries-boot/src/main/resources/application-local.yml`

默认本地数据库：

- database: `cg`
- username: `root`
- password: `1234`

默认开发管理员：

- username: `admin`
- password: `123456`

## 本地启动

先确保 MySQL 可用，并已创建或允许自动创建数据库 `cg`。

启动：

```bash
mvn -pl caries-boot -am spring-boot:run
```

默认访问：

- app: `http://127.0.0.1:8080`
- OpenAPI: `http://127.0.0.1:8080/v3/api-docs`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- ping: `http://127.0.0.1:8080/api/v1/system/ping`

## 测试

运行模块测试：

```bash
mvn test
```

运行 boot 真库关键链路：

```bash
mvn -q -pl caries-boot -am "-Dtest=MainlineWorkflowE2ETest,AnalysisToReportE2ETest,AnalysisReportFollowupE2ETest,FollowupTriggerIdempotencyE2ETest,DashboardOverviewIntegrationTest,DashboardTrendIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

当前新增的主链路真库测试：

- `MainlineWorkflowE2ETest`

它覆盖：

- 登录
- 当前用户查询
- 患者建档
- 就诊创建
- 病例创建
- 文件上传
- 影像建档与质检
- AI 回调
- 医生修正
- 报告生成与导出
- 随访完成
- dashboard 汇总校验

## 当前核心接口

已实现的核心入口包括：

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/auth/permissions`
- `GET /api/v1/system/ping`
- `POST /api/v1/patients`
- `POST /api/v1/visits`
- `POST /api/v1/cases`
- `POST /api/v1/files/upload`
- `POST /api/v1/cases/{caseId}/images`
- `POST /api/v1/cases/{caseId}/analysis`
- `POST /api/v1/cases/{caseId}/corrections`
- `POST /api/v1/cases/{caseId}/reports`
- `POST /api/v1/followup/records`
- `GET /api/v1/dashboard/*`

## 关键文档

建议优先阅读：

- `docs/development-plan.md`
- `docs/14_P8_主链路E2E与交付收口.md`
- `docs/15_部署与环境说明.md`
- `docs/16_API契约与当前实现差异清单.md`
- `docs/17_测试结果摘要与答辩证据清单.md`

## 当前下一步

当前工作重点不是新增业务功能，而是：

1. 部署与环境文档收口
2. API 契约与真实实现对齐
3. 测试结果摘要与答辩证据整理
