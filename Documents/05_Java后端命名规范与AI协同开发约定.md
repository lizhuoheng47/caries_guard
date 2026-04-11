# Java 后端命名规范与 AI 协同开发约定

> 项目：多模态龋齿智能识别与分级预警平台（CariesGuard）
> 
> 目标：统一数据库、Java 代码、接口、消息、对象存储、AI 协同开发的命名语言

---

# 1. 命名总原则

1. **统一**：同一语义在数据库、后端、前端、AI 提示词中必须保持一致。
2. **稳定**：一旦字段和状态冻结，不允许频繁重命名。
3. **可读**：优先完整单词，不用晦涩拼音缩写。
4. **分层清晰**：数据库命名、Java 类命名、接口命名、消息命名分别遵循各自规则。
5. **面向 AI 协作**：命名必须足够直白，使 Codex、Gemini、Claude Code 不会误判语义。

---

# 2. 数据库命名规范

## 2.1 表名

格式：

```text
域前缀_业务名词
```

示例：

- `sys_user`
- `sys_role`
- `pat_patient`
- `med_case`
- `med_image_file`
- `rpt_record`
- `fup_task`
- `msg_notify_record`

## 2.2 字段命名

### 主键

- `id`

### 业务编号

- `*_no`

示例：

- `user_no`
- `visit_no`
- `case_no`
- `report_no`
- `plan_no`
- `task_no`

### 外键

- `xxx_id`

示例：

- `patient_id`
- `case_id`
- `visit_id`
- `attachment_id`
- `role_id`

### 类型与状态

- `*_code` 或 `status`

示例：

- `user_type_code`
- `case_status_code`
- `report_status_code`
- `quality_status_code`
- `status`

### 布尔值

- `*_flag`

示例：

- `deleted_flag`
- `report_ready_flag`
- `followup_required_flag`
- `is_primary` 不推荐，建议统一为 `primary_flag`

> 说明：如果历史表已经使用 `is_primary`，可以保留，但新表优先统一 `*_flag`。

### 时间字段

- `*_at`：时间点
- `*_date`：日期

示例：

- `created_at`
- `updated_at`
- `generated_at`
- `visit_date`
- `next_followup_date`

### JSON 字段

- `*_json`

示例：

- `ext_json`
- `column_mask_policy_json`
- `raw_result_json`

---

# 3. Java 包命名规范

统一小写英文包名：

```text
com.cariesguard
```

模块包示例：

```text
com.cariesguard.system
com.cariesguard.patient
com.cariesguard.image
com.cariesguard.analysis
com.cariesguard.report
com.cariesguard.followup
com.cariesguard.dashboard
```

---

# 4. Java 类命名规范

## 4.1 Entity / DO

推荐：

- `SysUserDO`
- `PatPatientDO`
- `MedCaseDO`
- `RptRecordDO`

说明：

- 若你团队偏向 `Entity`，也可统一为 `SysUserEntity`；
- 但必须全项目统一，不要一半 `DO` 一半 `Entity`。

**推荐本项目统一使用 `DO`**，因为更适合和 DTO / VO / Query / Command 区分。

## 4.2 Mapper

- `SysUserMapper`
- `MedCaseMapper`
- `RptRecordMapper`

## 4.3 Repository

### 领域接口

- `PatientRepository`
- `CaseRepository`

### 基础设施实现

- `PatientRepositoryImpl`
- `CaseRepositoryImpl`

## 4.4 Service

### 应用服务

- `PatientAppService`
- `CaseAppService`
- `ReportAppService`

### 领域服务

- `CaseDomainService`
- `ReportDomainService`
- `FollowupDomainService`

## 4.5 Controller

- `PatientController`
- `CaseController`
- `ImageController`
- `ReportController`

## 4.6 Convert / MapperStruct

- `PatientConvert`
- `CaseConvert`
- `ReportConvert`

## 4.7 Client / Adapter

- `AiAnalysisClient`
- `AiRiskClient`
- `MinioStorageClient`
- `RabbitPublisher`

---

# 5. DTO / VO / Query / Command 命名规范

## 5.1 DTO

适用于跨层数据传输对象。

示例：

- `PatientDTO`
- `CaseSummaryDTO`
- `ImageQualityDTO`

## 5.2 VO

适用于接口返回视图对象。

示例：

- `PatientDetailVO`
- `CaseListVO`
- `ReportPreviewVO`

## 5.3 Query

适用于列表查询、筛选查询。

示例：

- `PatientPageQuery`
- `CaseSearchQuery`
- `FollowupTaskPageQuery`

## 5.4 Command

适用于写操作命令对象。

示例：

- `CreatePatientCommand`
- `CreateCaseCommand`
- `UploadImageCommand`
- `SubmitCaseReviewCommand`

## 5.5 Request / Response

只有在第三方集成或极简模块中才使用：

- `AiAnalyzeRequest`
- `AiAnalyzeResponse`
- `MinioUploadResponse`

不建议业务 Controller 入口全部都叫 `XXXRequest`，优先 `Command / Query / VO`。

---

# 6. 方法命名规范

## 6.1 Controller 层

统一使用动词 + 业务语义：

- `createPatient`
- `getPatientDetail`
- `pagePatients`
- `createCase`
- `uploadCaseImage`
- `triggerCaseAnalysis`
- `generateDoctorReport`
- `submitCaseReview`

## 6.2 AppService 层

- `createPatient`
- `createCase`
- `uploadImage`
- `startAnalysis`
- `generateReport`
- `submitReview`
- `createFollowupPlan`

## 6.3 Repository 层

- `save`
- `updateById`
- `findById`
- `findByCaseNo`
- `pageQuery`
- `existsByUsername`

## 6.4 禁止命名

禁止使用：

- `doSomething`
- `handleData`
- `processInfo`
- `aaa`
- `temp`
- `test1`
- `finalDeal`

---

# 7. API 路径命名规范

统一：

```text
/api/v1/资源复数
```

示例：

- `GET /api/v1/patients`
- `POST /api/v1/patients`
- `GET /api/v1/cases/{caseId}`
- `POST /api/v1/cases/{caseId}/images`
- `POST /api/v1/cases/{caseId}/analysis`
- `POST /api/v1/cases/{caseId}/reviews`
- `GET /api/v1/reports/{reportId}`
- `POST /api/v1/reports/{reportId}/export`

不推荐：

- `/getPatientList`
- `/saveCaseInfo`
- `/doUpload`
- `/queryReportByCondition`

---

# 8. 枚举与状态码命名规范

## 8.1 Java 枚举类

命名格式：

- `UserTypeEnum`
- `CaseStatusEnum`
- `ReportStatusEnum`
- `FollowupTaskStatusEnum`

## 8.2 枚举值

统一全大写英文：

- `ACTIVE`
- `DISABLED`
- `CREATED`
- `ANALYZING`
- `REPORT_READY`
- `DONE`
- `FAILED`

## 8.3 原则

- 数据库存编码；
- Java 枚举与数据库编码一致；
- 前端通过字典接口映射中文标签；
- 不在数据库里直接存中文状态。

---

# 9. 数据库字段与 Java 属性映射规范

## 9.1 映射方式

数据库字段：下划线风格

Java 属性：驼峰风格

示例：

| DB 字段 | Java 属性 |
|---|---|
| `patient_no` | `patientNo` |
| `case_status_code` | `caseStatusCode` |
| `created_at` | `createdAt` |
| `report_ready_flag` | `reportReadyFlag` |

## 9.2 不要做的事

- Java 属性名和数据库字段语义不一致；
- 一个地方叫 `followupPlanId`，另一个地方叫 `planId` 却表达同一层含义；
- DTO 为了“简写”而丢失语义。

---

# 10. 对象存储命名规范

## 10.1 Bucket 命名建议

- `caries-image`
- `caries-report`
- `caries-export`
- `caries-visual`

## 10.2 Object Key 规则

推荐：

```text
{biz-module}/{yyyy}/{MM}/{dd}/{biz-id}/{filename}
```

示例：

```text
case-image/2026/04/11/CASE202604110001/original_01.jpg
report/2026/04/11/RPT202604110001/doctor_v1.pdf
visual/2026/04/11/CASE202604110001/lesion_mask.png
```

---

# 11. MQ 事件命名规范

## 11.1 事件名称

统一小写英文 + 点分层：

- `image.uploaded`
- `analysis.requested`
- `analysis.completed`
- `analysis.failed`
- `report.generated`
- `followup.plan.created`
- `followup.task.due`
- `review.submitted`

## 11.2 消费者命名

- `ImageUploadedConsumer`
- `AnalysisCompletedConsumer`
- `FollowupTaskDueConsumer`

---

# 12. SQL 脚本命名规范

```text
V001__init_schema.sql
V002__init_sys_dict.sql
V003__init_sys_menu.sql
V004__create_patient_case_tables.sql
V005__create_image_tables.sql
```

要求：

- 版本号递增；
- 文件名表达变更目的；
- 不允许 `final.sql`、`new.sql`、`最新版.sql` 这种命名。

---

# 13. 测试类命名规范

- `PatientAppServiceTest`
- `CaseControllerTest`
- `ReportGenerateIntegrationTest`
- `FollowupPlanDomainServiceTest`

测试方法建议：

- `should_create_patient_successfully`
- `should_reject_case_review_when_status_invalid`
- `should_generate_doctor_report_after_analysis_done`

---

# 14. Git 分支命名规范

- `main`
- `develop`
- `feature/patient-module`
- `feature/image-upload`
- `feature/report-generate`
- `fix/case-status-transition`
- `refactor/system-auth`
- `docs/java-dev-manual`

---

# 15. AI 协同开发约定

## 15.1 给 AI 的任务命名必须具体

推荐：

- “生成患者模块的 DO / Mapper / Repository / AppService / Controller”
- “根据数据字典生成 med_case 对应的 Java 代码骨架”
- “为报告模块生成 PDF 导出用例，不要修改其他模块”

不推荐：

- “把整个后端写出来”
- “帮我把系统都补全”
- “自由发挥优化架构”

## 15.2 AI 输出代码必须遵守的固定术语

- 患者：`patient`
- 就诊：`visit`
- 病例：`case`
- 影像：`image`
- 报告：`report`
- 随访：`followup`
- 修正：`review` / `correction`
- 组织：`org`
- 字典：`dict`

## 15.3 AI 禁止做的事

- 擅自替换已冻结字段名；
- 把 `*_code` 改成数字枚举；
- 把模块化单体改写成微服务脚手架；
- 把病例主线和 AI 训练治理表混成一个库；
- 用中文直接做数据库字段名；
- 自创与数据字典不一致的缩写。

---

# 16. 最终强制规则

从进入正式开发开始，以下规则必须强制执行：

1. **数据库命名以数据字典为准**；
2. **Java 命名以本规范为准**；
3. **接口命名以 REST 资源风格为准**；
4. **消息命名以事件风格为准**；
5. **AI 协同开发必须以模块和用例为边界**；
6. **任何新命名先看是否已经有既定术语，不允许重复造词**。

这份规范的目标只有一个：

> 让你的后端、人类开发者、前端、数据库、AI 工具说同一种语言。
