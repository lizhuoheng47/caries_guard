# Java 后端开发说明书

> 项目：多模态龋齿智能识别与分级预警平台（CariesGuard）
> 
> 文档性质：Java 后端实施级开发说明书
> 
> 适用对象：后端开发工程师、AI 编码工具（Codex / Gemini / Claude Code）、测试工程师、项目负责人
> 
> 适用范围：Spring Boot 业务平台，不覆盖 Python 训练体系的内部实现细节
> 
> 当前目标：作为 Java 后端编码、联调、测试、部署的统一执行依据

---

# 1. 文档目标

本说明书用于解决以下问题：

1. 把当前项目文档体系收敛为一套可编码的 Java 后端规范；
2. 让人类开发者与 AI IDE 工具使用同一套术语、目录、分层和命名约定；
3. 避免“业务文档是一个版本、数据库文档是一个版本、AI 生成代码又是另一个版本”的问题；
4. 让 Java 后端能够稳定支撑病例主线、AI 联动、报告、随访、审计和后续答辩展示。

---

# 2. 开发基线

## 2.1 项目定位

本项目不是普通的“AI 识别网页”，而是面向真实口腔筛查流程的**可信辅助决策平台**。Java 后端必须服务以下闭环：

> 登录鉴权 → 患者建档 → 就诊建档 → 病例创建 → 影像上传 → AI 分析 → 风险评估 → 报告生成 → 医生复核修正 → 随访管理 → 审计追踪

## 2.2 Java 后端职责

Java 后端负责：

- 身份认证、权限控制、菜单路由、组织树、数据权限；
- 患者、监护人、画像、就诊、病例、牙位记录等业务主线；
- 影像上传、对象存储、元数据管理、影像质量记录；
- AI 任务编排、结果回收、状态跟踪、异常处理；
- 风险评估结果聚合；
- 医生版/患者版报告生成与导出归档；
- 随访计划、任务、记录与通知；
- 审计日志、登录日志、配置、监控与运维接口。

## 2.3 Python AI 服务职责

Python 侧负责：

- 图像质量检查；
- 牙位检测与编码映射；
- 病灶分割；
- 龋病分级；
- 风险评估子模型；
- 可解释性图层生成。

Java 后端不重复实现模型推理逻辑，只做**业务编排和结果承载**。

---

# 3. 总体架构与工程落地原则

## 3.1 固定架构

当前工程固定采用：

> **模块化单体 + 独立 AI 服务 + RabbitMQ 解耦**

## 3.2 为什么当前不拆微服务

现阶段不拆独立微服务，原因如下：

- 竞赛项目更重视稳定演示和完整闭环；
- 微服务会放大部署复杂度和排障成本；
- 业务体量尚不足以支撑服务拆分收益；
- 模块化单体依然可以通过明确的边界、事件、接口实现良好解耦。

## 3.3 工程设计原则

1. **业务优先**：先保证病例主线闭环，再考虑复杂工程优化。
2. **模块边界清晰**：一个模块只负责一个业务域。
3. **控制反向依赖**：基础模块可被上层模块依赖，上层模块不能反向入侵基础模块。
4. **面向协同开发**：目录结构和命名必须让 AI 工具也容易理解。
5. **可答辩**：每个模块都要能演示、能解释、能追踪。

---

# 4. 技术栈与版本建议

## 4.1 Java 平台基线

- JDK：**17**
- Spring Boot：**3.2.x**
- Spring MVC：REST API
- Spring Security 6：认证与授权
- JWT：登录态
- MyBatis-Plus 3.5.x：ORM / CRUD 增强
- HikariCP：数据库连接池
- Spring Validation：参数校验
- MapStruct：对象转换
- Lombok：减少样板代码
- SpringDoc OpenAPI / Knife4j：接口文档
- Redis 7：缓存与分布式协同
- RabbitMQ：异步任务解耦
- MinIO：对象存储
- Logback：日志
- Spring Boot Actuator：健康检查与轻量监控

## 4.2 暂不纳入一期基线

以下能力不是一期必须：

- Elasticsearch
- Kafka
- ELK
- K8s
- Prometheus / Grafana
- 复杂配置中心

说明：保留扩展位，但不纳入当前必须开发范围。

---

# 5. 工程组织方式

## 5.1 推荐仓库结构

建议使用一个 Git 仓库，内部按后端、AI、前端、部署与文档分目录：

```text
caries-guard/
├── backend-java/
├── ai-python/
├── frontend-web/
├── deploy/
├── docs/
└── scripts/
```

Java 后端项目位于 `backend-java/`。

## 5.2 Java 后端推荐结构

建议采用 **Maven 多模块** 或 **Gradle 多模块**。如果你当前更偏 IDEA + MyBatis-Plus + 常规开发习惯，推荐 Maven 多模块。

```text
backend-java/
├── pom.xml
├── caries-boot/                 # 启动模块
├── caries-common/               # 通用基础能力
├── caries-framework/            # 安全、鉴权、日志、异常、配置
├── caries-system/               # 用户、角色、菜单、部门、字典、配置
├── caries-patient/              # 患者、监护人、画像、就诊、病例
├── caries-image/                # 附件、影像、质检、对象存储
├── caries-analysis/             # AI 分析编排、任务状态、结果聚合
├── caries-report/               # 报告模板、报告生成、导出
├── caries-followup/             # 随访计划、任务、记录、通知
├── caries-dashboard/            # 统计分析与聚合接口
├── caries-integration/          # AI/MinIO/MQ 等外部集成适配
└── sql/                         # DDL、初始化脚本、字典脚本
```

## 5.3 模块依赖规则

推荐依赖方向：

```text
caries-boot
  └── framework
        ├── common
        ├── system
        ├── patient
        ├── image
        ├── analysis
        ├── report
        ├── followup
        ├── dashboard
        └── integration
```

约束如下：

- `common` 不依赖业务模块；
- `framework` 可以依赖 `common`，不能依赖具体业务实现；
- `system` 为基础业务模块，其他业务模块可用其接口和公共能力；
- `patient / image / analysis / report / followup / dashboard` 尽量只依赖 `common / framework / system`；
- 外部系统访问能力统一放在 `integration`，业务模块尽量依赖适配器接口而不是直接写 HTTP 客户端。

---

# 6. 分层架构规范

每个业务模块内部统一使用以下分层：

```text
module/
├── controller/
├── app/
├── domain/
│   ├── model/
│   ├── service/
│   ├── event/
│   └── repository/
├── infrastructure/
│   ├── mapper/
│   ├── repository/
│   ├── convert/
│   └── client/
└── interfaces/
    ├── dto/
    ├── vo/
    ├── query/
    └── command/
```

## 6.1 各层职责

### controller

- 只负责 HTTP 入参与出参；
- 不写业务编排；
- 不直接操作 Mapper。

### app

- 负责应用编排；
- 组织一个完整用例；
- 处理事务边界；
- 调用 domain service。

### domain

- 负责核心业务规则；
- 负责状态流转、校验、领域事件；
- 不直接依赖控制器和前端对象。

### infrastructure

- 负责数据库、MQ、HTTP 客户端、对象存储等技术实现；
- Mapper、Repository 实现、第三方适配器均放此层。

### interfaces

- 放 DTO、VO、Command、Query；
- 不与 Entity 混用；
- 作为 controller 与 app 之间的契约对象。

---

# 7. 管理域设计原则（RuoYi 思想吸收方案）

## 7.1 采用方式

推荐吸收 RuoYi 的以下能力模型：

- 用户
- 角色
- 菜单
- 部门
- 岗位
- 字典
- 参数配置
- 操作日志
- 登录日志
- 数据权限

## 7.2 不直接照搬的部分

以下内容不建议直接整体复制：

- 旧版工程脚手架结构；
- 过于耦合的代码生成器逻辑；
- 业务域完全围绕后台管理代码风格建模；
- 与当前 Spring Boot 3 / Security 6 不完全兼容的旧实现。

## 7.3 推荐做法

你可以将 `sys_user / sys_role / sys_menu / sys_dept / sys_post / sys_dict_* / sys_config / sys_oper_log / sys_login_log / sys_data_permission_rule` 作为平台管理域标准模型，并在 Java 工程中实现为独立的 `caries-system` 模块。

---

# 8. 核心业务模块说明

## 8.1 caries-system

### 责任

- 用户登录、登出、JWT 签发；
- 角色权限控制；
- 菜单与按钮权限；
- 组织树与岗位；
- 字典与配置中心；
- 登录日志与操作日志；
- 数据权限规则。

### 核心对象

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_dept`
- `sys_post`
- `sys_dict_type`
- `sys_dict_item`
- `sys_config`
- `sys_login_log`
- `sys_oper_log`
- `sys_data_permission_rule`

## 8.2 caries-patient

### 责任

- 患者主档；
- 监护人信息；
- 患者画像；
- 就诊登记；
- 病例创建与状态流转；
- 牙位记录与医生诊断。

### 核心对象

- `pat_patient`
- `pat_guardian`
- `pat_profile`
- `med_visit`
- `med_case`
- `med_case_status_log`
- `med_case_diagnosis`
- `med_case_tooth_record`

## 8.3 caries-image

### 责任

- 附件元数据管理；
- 影像上传；
- 影像类型、来源、主图管理；
- 质检记录；
- MinIO 对象存储访问。

### 核心对象

- `med_attachment`
- `med_image_file`
- `med_image_quality_check`

## 8.4 caries-analysis

### 责任

- 调用 Python AI 服务；
- 创建分析任务；
- 跟踪分析状态；
- 保存 AI 业务结果摘要；
- 接收分析完成事件；
- 支撑医生修正回流。

### 注意

本模块只保存**业务侧需要消费的 AI 结果快照**，不承担完整训练治理平台职责。

### 建议核心表（业务侧）

建议在业务库中补充最小集合：

- `ana_task_record`
- `ana_result_summary`
- `ana_visual_asset`
- `ana_correction_feedback`

如果你暂时不想扩展新表，可以先把结果存放在 JSON 摘要字段中，但必须预留后续拆表能力。

## 8.5 caries-report

### 责任

- 报告模板管理；
- 医生版报告生成；
- 患者版报告生成；
- PDF 生成、版本归档；
- 报告导出审计。

### 核心对象

- `rpt_template`
- `rpt_record`
- `rpt_export_log`

## 8.6 caries-followup

### 责任

- 随访计划生成；
- 随访任务分派；
- 随访结果记录；
- 通知触发与发送记录。

### 核心对象

- `fup_plan`
- `fup_task`
- `fup_record`
- `msg_notify_record`

## 8.7 caries-dashboard

### 责任

- 聚合统计接口；
- 趋势分析；
- 风险分层统计；
- 随访完成率统计；
- 机构/医生维度看板接口。

### 原则

不单独重复存储大批统计表，优先通过聚合查询 + 必要缓存完成。

---

# 9. 领域主线与核心流程

## 9.1 基础筛查流程

1. 登录成功；
2. 创建患者；
3. 创建就诊；
4. 创建病例；
5. 上传影像；
6. 完成影像质检；
7. 触发 AI 分析；
8. 接收病灶定位、分级、不确定性结果；
9. 生成风险评估结果；
10. 生成医生版/患者版报告；
11. 高风险或高不确定性进入复核；
12. 医生确认或修正；
13. 必要时创建随访计划。

## 9.2 复诊对比流程

1. 选择历史患者；
2. 创建新就诊与新病例；
3. 关联历史病例；
4. 上传新影像并分析；
5. 对比前后风险与结论；
6. 更新报告；
7. 必要时更新随访计划。

## 9.3 医生修正回流流程

1. 医生修改牙位、分割、分级或结论；
2. 写入修正记录；
3. 记录原始值与修改值；
4. 更新报告版本；
5. 推入候选回流池；
6. 数据管理员后续脱敏导出。

---

# 10. 状态机设计要求

## 10.1 病例状态

推荐使用：

- `CREATED`
- `IMAGE_UPLOADED`
- `QC_PASS`
- `QC_REJECT`
- `ANALYZING`
- `ANALYSIS_DONE`
- `REVIEW_PENDING`
- `REVIEWED`
- `REPORT_READY`
- `FOLLOWUP_REQUIRED`
- `CLOSED`
- `CANCELLED`

要求：

- 所有状态变化必须记录到 `med_case_status_log`；
- 不允许直接跨越非法状态；
- 业务状态判断优先依据状态编码，不依赖文本描述。

## 10.2 报告状态

- `DRAFT`
- `FINAL`
- `ARCHIVED`

要求：

- 最终报告不可覆盖，只能新建版本；
- 患者版报告和医生版报告分别管理版本。

## 10.3 随访任务状态

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `OVERDUE`
- `CANCELLED`

---

# 11. API 设计规范

## 11.1 接口风格

统一 RESTful 风格，接口前缀使用版本号：

```text
/api/v1/**
```

## 11.2 URL 命名规范

- 使用复数资源名：`/patients`、`/cases`、`/reports`
- 不在 URL 中混用动词与中文
- 业务动作使用子资源或动作端点，例如：
  - `POST /api/v1/cases/{caseId}/images`
  - `POST /api/v1/cases/{caseId}/analysis`
  - `POST /api/v1/cases/{caseId}/reviews`
  - `POST /api/v1/reports/{reportId}/export`

## 11.3 统一响应结构

推荐：

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "traceId": "...",
  "timestamp": "2026-04-11T12:00:00"
}
```

## 11.4 查询接口规范

查询条件统一用 `Query` 对象，例如：

- `PatientPageQuery`
- `CaseListQuery`
- `ReportSearchQuery`

禁止把 20 个散乱参数都堆到 Controller 方法参数列表里。

## 11.5 写接口规范

写操作统一使用 `Command` / `Request` 对象，例如：

- `CreatePatientCommand`
- `CreateCaseCommand`
- `UploadImageCommand`
- `GenerateReportCommand`
- `SubmitCaseReviewCommand`

---

# 12. 安全与权限规范

## 12.1 认证方案

- 登录接口校验用户名密码；
- 登录成功返回 JWT + 刷新策略；
- 服务端在 Redis 中保存会话辅助信息（可选）；
- 所有受保护接口都要进行 Token 校验。

## 12.2 授权方案

采用 **RBAC + 数据权限**：

- 功能权限：菜单、按钮、接口级权限；
- 数据权限：ALL / ORG / DEPT / SELF / CUSTOM；
- 列级保护：手机号、出生日期、诊疗备注、报告摘要等敏感字段按角色脱敏。

## 12.3 数据隔离

所有主业务表都必须具备 `org_id` 字段，查询必须默认带入机构隔离条件。

## 12.4 敏感字段处理

建议保护字段包括：

- 患者姓名
- 手机号
- 出生日期
- 监护人信息
- 临床备注
- 报告摘要
- 通知正文摘要

处理策略：

- 数据库加密或脱敏存储；
- 列级权限控制；
- 导出审批；
- 日志中不打印原文。

---

# 13. 数据库实现规范

## 13.1 表设计必须遵循数据字典

工程开发时，**一律以 `07_完整数据字典与逻辑表设计.md` 为字段冻结依据**。

不允许：

- 开发者私自新增不一致字段命名；
- 一个表里用 `is_deleted`，另一个表里用 `deleted`；
- 状态字段到处写中文；
- 前端和后端各维护一套状态值。

## 13.2 公共字段

除纯关系表和纯日志表外，业务表统一包含：

- `id`
- `org_id`
- `status`
- `deleted_flag`
- `remark`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`

## 13.3 外键策略

逻辑外键强制存在，物理外键按性能和迁移策略决定是否落地。

## 13.4 代码集策略

- 数据库存稳定英文编码；
- 展示层通过字典接口返回中文标签；
- 导入导出支持编码 + 标签双列。

---

# 14. AI 服务集成规范

## 14.1 调用方式

Java → Python 的推荐模式：

- 轻量状态查询：HTTP 同步
- 重分析任务：MQ 异步 + 回调/结果拉取

## 14.2 推荐接口

### 影像质量检查

- `POST /ai/v1/quality-check`

### 综合分析

- `POST /ai/v1/analyze`

### 风险评估

- `POST /ai/v1/assess-risk`

### 模型版本查询

- `GET /ai/v1/model-version`

## 14.3 Java 侧 AI 适配层

统一放在 `caries-integration` 中：

- `AiAnalysisClient`
- `AiRiskClient`
- `AiModelClient`
- `MinioStorageClient`
- `MqPublisher`

业务模块只依赖这些接口，不直接手写散乱 HTTP 请求。

## 14.4 超时与容错策略

- 质量检查：短超时，失败可立即提示重试；
- 分析接口：支持异步；
- AI 不可用时：病例状态进入 `REVIEW_PENDING` 或 `ANALYSIS_FAILED`，前端可提示“稍后复核”；
- 所有失败都必须带 `traceId`。

---

# 15. MQ 与事件规范

## 15.1 当前用途

RabbitMQ 用于：

- 影像上传后触发分析；
- 分析完成后生成报告；
- 随访提醒与通知；
- 修正记录归档通知；
- 异步审计补充任务。

## 15.2 推荐事件命名

- `image.uploaded`
- `analysis.requested`
- `analysis.completed`
- `analysis.failed`
- `report.generate.requested`
- `report.generated`
- `followup.plan.created`
- `followup.task.due`
- `case.review.submitted`

## 15.3 消息载荷规范

消息体必须至少包含：

- `eventId`
- `eventType`
- `occurredAt`
- `traceId`
- `operatorId`
- `orgId`
- `bizId`
- `payload`

---

# 16. 对象存储规范

## 16.1 存储对象类型

- 原始影像
- 质量检查图
- 分割可视化图
- 热力图
- 医生版报告 PDF
- 患者版报告 PDF
- 导出附件

## 16.2 Java 侧职责

Java 只负责：

- 上传对象；
- 记录元数据；
- 生成预签名 URL；
- 控制可见性；
- 绑定业务引用。

## 16.3 安全要求

- 默认 `PRIVATE`；
- 对外访问必须通过短时预签名；
- 不允许前端拼接固定公网地址直连敏感对象。

---

# 17. 报告生成规范

## 17.1 报告类型

### 医生版报告

包含：

- 病例摘要
- 影像列表
- 病灶定位
- 分级结果
- 不确定性提示
- 风险评估
- 复核建议
- 医生结论

### 患者版报告

包含：

- 通俗结果解释
- 可能异常牙位
- 风险等级卡片
- 复查建议
- 日常护理建议

## 17.2 模板策略

- 模板存于 `rpt_template`
- 渲染生成 HTML
- 再转换 PDF
- 最终文件存 MinIO
- 报告记录写入 `rpt_record`

## 17.3 版本规则

- 每次生成都新增版本；
- FINAL 不覆盖历史版本；
- 导出/打印/分享必须写 `rpt_export_log`。

---

# 18. 随访规范

## 18.1 随访触发条件

建议以下场景触发：

- 高风险病例；
- 高不确定性且医生要求复查；
- 患者版报告中建议复查；
- 医生手工创建随访计划。

## 18.2 随访结构

- `fup_plan`：计划层
- `fup_task`：执行层
- `fup_record`：结果层
- `msg_notify_record`：通知层

## 18.3 状态联动要求

- `plan_status_code` 与 `task_status_code` 必须联动；
- 任务完成后可决定计划结束或继续；
- 每次联系尝试都应更新 `attempt_count` 和 `last_contact_at`。

---

# 19. 日志、审计与追踪规范

## 19.1 日志分类

- 应用日志
- 审计日志
- 登录日志
- AI 调用日志
- 异常日志

## 19.2 TraceId 要求

每次请求都必须生成或透传 `traceId`，并贯穿：

- HTTP 请求
- MQ 消息
- AI 服务调用
- 报告生成
- 异常日志

## 19.3 审计要求

以下操作必须入审计日志：

- 登录、登出、登录失败
- 患者建档
- 病例状态流转
- 影像上传/删除
- 报告生成/导出
- 医生修正
- 随访结果更新
- 配置变更
- 权限变更

---

# 20. 异常码与错误处理规范

## 20.1 统一原则

- 系统异常和业务异常分开；
- 错误码稳定，不可频繁改；
- 错误信息面向前端可读，日志面向排障可追踪。

## 20.2 推荐分类

- `Axxxx`：认证授权错误
- `Bxxxx`：业务校验错误
- `Cxxxx`：外部系统调用错误
- `Dxxxx`：数据访问错误
- `Exxxx`：系统内部错误

## 20.3 示例

- `A0001` Token 无效
- `A0002` 权限不足
- `B1001` 患者不存在
- `B2001` 病例状态不允许当前操作
- `C3001` AI 服务不可用
- `C3002` MinIO 上传失败
- `D4001` 数据库写入失败
- `E5000` 系统内部异常

---

# 21. 测试规范

## 21.1 测试分层

### 单元测试

覆盖：

- 状态流转逻辑
- 校验逻辑
- 数据转换逻辑
- 工具类

### 集成测试

覆盖：

- Controller → App → DB 主链路
- MinIO 集成
- RabbitMQ 集成
- AI 客户端调用

### 端到端测试

覆盖：

- 患者建档到报告生成
- 高风险病例随访
- 医生修正回流

## 21.2 必测业务链路

1. 登录成功/失败
2. 患者建档
3. 病例创建
4. 影像上传
5. AI 分析成功/失败
6. 报告生成
7. 医生修正
8. 随访创建与完成
9. 数据权限过滤
10. 敏感字段脱敏展示

---

# 22. 本地开发与环境规范

## 22.1 环境分层

- `local`：本地开发
- `dev`：联调环境
- `test`：测试环境
- `prod-demo`：答辩/演示环境

## 22.2 配置管理

- 不同环境使用不同 `application-*.yml`
- 密钥不提交 Git
- MinIO / JWT / DB / MQ 密码使用环境变量或外置配置

## 22.3 Docker Compose 建议组件

- mysql
- redis
- rabbitmq
- minio
- backend-java
- ai-python
- frontend-web
- nginx

---

# 23. AI IDE 协同开发规范

## 23.1 总原则

AI 工具一次只负责一个明确、可验证、边界清晰的任务。

## 23.2 推荐拆分方式

### 第一类：按模块生成

例如：

- 先生成 `caries-system` 的用户、角色、菜单、字典接口
- 再生成 `caries-patient` 的患者、就诊、病例模块
- 再生成 `caries-image`

### 第二类：按层生成

例如一个病例模块先后生成：

1. Entity / DO
2. Mapper
3. Repository
4. Command / Query / DTO / VO
5. AppService
6. Controller
7. 测试用例

### 第三类：按用例生成

例如：

- 创建患者
- 创建病例
- 上传影像
- 提交 AI 分析
- 生成报告
- 提交医生修正

## 23.3 不要让 AI 一次做的事情

不要一次性让 AI：

- 生成整个后端所有代码；
- 同时决定数据库、接口、前端字段、业务规则；
- 跨多个模块自由发挥命名；
- 自动修改已冻结字段和状态码。

## 23.4 推荐提示词模板

```text
你现在是本项目的 Java 后端开发助手。
请严格遵守以下约束：
1. 架构为 Spring Boot 3 + JDK17 + 模块化单体 + 独立 AI 服务 + RabbitMQ。
2. 字段命名必须遵守数据字典，禁止擅自改名。
3. 使用 MyBatis-Plus、MapStruct、Spring Validation。
4. Controller 不直接操作 Mapper。
5. 生成的代码必须包含中文注释、必要的校验、异常处理和单元测试建议。
6. 本次只生成 [某模块/某用例] 的代码，不生成其他模块。
```

---

# 24. 代码生成顺序建议

## 第一阶段：基础底座

1. `common`
2. `framework`
3. `system`
4. 统一返回、异常、日志、认证、字典

## 第二阶段：业务主线

1. `patient`
2. `image`
3. `analysis`
4. `report`
5. `followup`

## 第三阶段：增强能力

1. `dashboard`
2. 报告模板优化
3. WebSocket 推送
4. 统计缓存优化
5. 审计增强

---

# 25. 当前开发冻结清单

在正式编码前，以下内容必须冻结：

1. 数据字典字段名
2. 状态码与代码集
3. 模块名称
4. 包路径规则
5. AI 接口协议
6. MQ 事件命名
7. 统一异常码
8. 统一响应结构
9. 报告模板字段映射
10. 数据权限范围规则

---

# 26. 最终执行要求

从本说明书开始，Java 后端开发必须遵守以下底线：

- 不擅自改数据库字段命名；
- 不把模块化单体偷偷做成“伪微服务”；
- 不让 Controller 直接充当业务层；
- 不让 AI 工具跨模块无边界生成代码；
- 不把训练数据治理直接塞进业务库；
- 不跳过测试直接拼装演示代码；
- 不允许一边写代码一边改状态码。

这份文档的作用，不是写给评委看的，而是让你的 Java 后端**真的能被团队和 AI 工具稳定落地**。
