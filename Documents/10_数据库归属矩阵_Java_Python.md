# 10 数据库归属矩阵（Java `caries_biz` ↔ Python `caries_ai`）

## 1. 文档定位

本文档是 CariesGuard 第二阶段「边界刻画」的总开关。  
所有后续 Java / Python 表的新增、迁移、扩展，必须先在本矩阵登记归属与跨界协议，禁止越界。

- Java 业务平台只连 `caries_biz`
- Python AI / RAG / 模型治理只连 `caries_ai`
- 两库共用同一个 MySQL 实例，但**逻辑互不可见**：
  - 禁止跨库 join
  - 禁止 Python 直接 update / insert Java 业务表
  - 双方仅通过 RabbitMQ、HTTP callback、MinIO 对象引用、重点字段（`org_id` / `case_no` / `task_no` / `image_id` / `attachment_id` / `report_no`）协同

## 2. 物理拓扑

| 资源 | 用途 | 归属服务 |
| --- | --- | --- |
| MySQL 实例 `mysql:3306` | 唯一 MySQL 进程 | 共用基础设施 |
| Database `caries_biz` | Java 业务平台库 | `backend-java` |
| Database `caries_ai`  | Python AI / RAG / 模型治理库 | `backend-python` |

容器首启时由 `infra/mysql/init/01_init_databases.sql` 同时建库。

环境变量：

| 变量 | 默认值 | 注入对象 |
| --- | --- | --- |
| `CARIES_MYSQL_DATABASE_BIZ` | `caries_biz` | `backend-java` 的 `CARIES_MYSQL_DATABASE` |
| `CARIES_MYSQL_DATABASE_AI`  | `caries_ai`  | `backend-python` 的 `CG_MYSQL_DATABASE` |

历史 `cg` 单库形态已下线，不再保留兼容路径。

## 3. 归属判定原则

### 3.1 判进 Java `caries_biz` 的硬条件

满足任一即归 `caries_biz`：

- 直接落入病例主线（`med_case` / `med_visit` / `med_attachment` / `med_image_file` 等）
- 直接被前端列表 / 详情 / 报告页面消费
- 直接参与状态机流转
- 直接归属权限与审计
- 直接被报告、随访、通知消费

### 3.2 判进 Python `caries_ai` 的硬条件

满足任一即归 `caries_ai`：

- 描述 AI 任务运行过程（job、image、artifact、callback 重试）
- 描述知识库、文档、切片、向量索引、RAG 检索 / LLM 调用日志
- 描述模型版本、训练运行、评估、审批、数据集快照、标注治理
- 不被 Java 业务前端直接消费，仅通过 Java 业务侧快照间接展现

## 4. Java `caries_biz` 表归属清单（38 张）

> 来源：`backend-java/caries-boot/src/main/resources/db/migration/V001__baseline_schema.sql`。

### 4.1 系统管理域

| 表名 | 用途 | 跨界可读？ |
| --- | --- | --- |
| `sys_role` | 角色基线 | 否 |
| `sys_dept` | 组织 / 科室 | 否 |
| `sys_user` | 系统用户 | 否 |
| `sys_post` | 岗位 | 否 |
| `sys_menu` | 菜单权限 | 否 |
| `sys_user_role` | 用户角色关联 | 否 |
| `sys_user_post` | 用户岗位关联 | 否 |
| `sys_role_menu` | 角色菜单关联 | 否 |
| `sys_config` | 参数配置 | 否 |
| `sys_data_permission_rule` | 数据权限规则（JSON 虚拟列） | 否 |
| `sys_dict_type` | 字典类型 | 否 |
| `sys_dict_item` | 字典项 | 否 |
| `sys_login_log` | 登录日志 | 否 |
| `sys_oper_log` | 操作日志 | 否 |

### 4.2 患者与病例域

| 表名 | 用途 | 跨界可读？ |
| --- | --- | --- |
| `pat_patient` | 患者主档 | 否 |
| `pat_guardian` | 监护人 | 否 |
| `pat_profile` | 患者扩展画像 | 否 |
| `med_visit` | 就诊记录 | 否 |
| `med_case` | 病例主表 | 否 |
| `med_case_status_log` | 病例状态流转日志 | 否 |
| `med_case_diagnosis` | 病例诊断结论 | 否 |
| `med_case_tooth_record` | 病例牙位记录 | 否 |

### 4.3 影像与附件域

| 表名 | 用途 | 跨界可读？ |
| --- | --- | --- |
| `med_attachment` | 附件对象 / 对象引用 / 签名访问 | 否 |
| `med_image_file` | 病例影像（绑定附件 + tooth + 类型） | 否 |
| `med_image_quality_check` | 影像质检结果 | 否 |

### 4.4 Analysis 业务快照域（与 AI 协同的 Java 侧）

| 表名 | 用途 | 跨界可读？ |
| --- | --- | --- |
| `ana_task_record` | 分析任务承载（状态机、重试谱系、callback 状态） | 否 |
| `ana_result_summary` | AI 摘要提点（含虚拟列、报告 / dashboard 直接消费） | 否 |
| `ana_visual_asset` | AI 可视化资产业务快照（mask / overlay / heatmap） | 否 |
| `ana_correction_feedback` | 医生修正纠偏反馈 | 否 |
| `med_risk_assessment_record` | 多模态风险研判 | 否 |
| `ana_model_version_registry` | Java 侧模型版本登记副本（仅供任务校验，正本在 `caries_ai.mdl_model_version`） | 否 |

> **注意**：`ana_*` 与 `med_risk_assessment_record` 留在 Java，是因为它们承载「业务快照」「页面 / 报告 / 审计直接消费」「医生操作」三种职责；不是模型训练正本。

### 4.5 报告与随访域

| 表名 | 用途 | 跨界可读？ |
| --- | --- | --- |
| `rpt_template` | 报告模板 | 否 |
| `rpt_record` | 报告记录（医生版 / 患者版 / version_no） | 否 |
| `rpt_export_log` | 导出审计 | 否 |
| `fup_plan` | 随访计划（含触发源幂等键） | 否 |
| `fup_task` | 随访任务 | 否 |
| `fup_record` | 随访记录 | 否 |
| `msg_notify_record` | 站内 / 短信 / 推送日志 | 否 |

## 5. Python `caries_ai` 表归属清单（19 张）

> 来源：`backend-python/app/repositories/metadata_repository.py`。  
> 当前由 `MetadataRepository.ensure_schema()` 在启动时建表；后续将切换为 Alembic（详见 §8）。

### 5.1 AI 运行域

| 表名 | 用途 | Java 可读？ |
| --- | --- | --- |
| `ai_infer_job` | 单次推理任务（关联 `java_task_no` + `case_no`） | 否，由 callback 反向同步状态 |
| `ai_infer_job_image` | 推理任务的图像分项 | 否 |
| `ai_infer_artifact` | mask / overlay / heatmap 等中间产物（同步上传至 `caries-visual`） | 否 |
| `ai_callback_log` | callback 重试 / 响应日志 | 否 |

### 5.2 RAG / 知识库域

| 表名 | 用途 | Java 可读？ |
| --- | --- | --- |
| `kb_knowledge_base` | 知识库登记 | 否 |
| `kb_document` | 知识文档（patient_guide / doctor_qa） | 否 |
| `kb_document_chunk` | 切片（embedding model + vector_id） | 否 |
| `kb_rebuild_job` | 重建作业 | 否 |
| `rag_session` | RAG 会话（绑定 `related_biz_no` / `patient_uuid`） | 否 |
| `rag_request_log` | 请求日志（含 latency / safety_flag） | 否 |
| `rag_retrieval_log` | 检索命中明细 | 否 |
| `llm_call_log` | LLM 调用日志（含 prompt / completion / token） | 否 |

### 5.3 模型治理域

| 表名 | 用途 | Java 可读？ |
| --- | --- | --- |
| `mdl_model_version` | 模型版本主档（**正本**） | 否 |
| `mdl_model_eval_record` | 模型评估记录 | 否 |
| `mdl_model_approval_record` | 模型上线审批 | 否 |
| `trn_dataset_snapshot` | 训练数据集快照 | 否 |
| `trn_dataset_sample` | 数据集样本 | 否 |
| `trn_training_run` | 训练运行 | 否 |
| `ann_annotation_record` | 标注记录 | 否 |
| `ann_gold_set_item` | 金标准集 | 否 |

## 6. 跨库协同协议

跨库交互**只允许**通过以下四条通道：

1. **RabbitMQ**  
   - exchange：`caries.analysis.exchange`  
   - 请求队列：`caries.analysis.requested.queue`（Python 消费）  
   - routing key：`analysis.requested`

2. **HTTP Callback**  
   - 端点：`POST http://backend-java:8080/api/v1/internal/ai/callbacks/analysis-result`  
   - HMAC：`X-AI-Timestamp`、`X-AI-Signature`  
   - 顶层 `visualAssets` 按冻结契约回传 `{bucketName, objectKey, ...}`，Java 自动落 `med_attachment` + `ana_visual_asset`

3. **MinIO 对象引用**  
   - 唯一寻址：`(bucketName, objectKey)`  
   - bucket 划分：`caries-image`（只读源）、`caries-visual`（Python 写）、`caries-report` / `caries-export`（Java 写）

4. **重点协同字段**（Python 表中允许出现，仅作弱引用）：  
   `org_id`、`case_no`、`task_no`、`patient_uuid`、`image_id`、`attachment_id`、`report_no`

### 6.1 硬性禁令

- ❌ Python 直接 `INSERT` / `UPDATE` Java 业务表
- ❌ Python 跨库 `JOIN` 到 `caries_biz`
- ❌ Java 直接读 `caries_ai` 任何 `ai_*` / `kb_*` / `rag_*` / `llm_*` / `mdl_*` / `trn_*` / `ann_*`
- ❌ 在 Java 侧重新建 RAG / 模型治理库的「正本」表

## 7. Schema 演进协议

| 服务 | 工具 | 入口 |
| --- | --- | --- |
| Java `caries_biz` | Flyway | `backend-java/caries-boot/src/main/resources/db/migration/V*__*.sql` |
| Python `caries_ai` | 当前由 `MetadataRepository.ensure_schema()` 启动时建表，下一阶段切 Alembic | `backend-python/app/repositories/metadata_repository.py` |

新增表必须：

1. 先在本文档第 4 / 5 节登记归属；
2. 由对应服务的迁移工具落地；
3. 不得绕过对方迁移工具去对方库内建表。

## 8. 与 Phase A 的对齐项

| Phase A 检查项 | 状态 |
| --- | --- |
| A1. 数据库归属矩阵 | 本文档 |
| A2. `caries_biz` / `caries_ai` 双库 | `infra/mysql/init/01_init_databases.sql` 已落地 |
| A3. Java 仅连 `caries_biz`，Python 仅连 `caries_ai` | `application-*.yml`、`config.py`、`docker-compose.yml` 已切换 |
| A4. 业务库内的 Python 表迁出 | Python 历史使用同库 `cg`，本次起 `MetadataRepository` 直接连 `caries_ai`；旧库下线，无需热迁移 |
| A5. Java analysis 核心表补齐 | `ana_task_record` / `ana_result_summary` / `ana_visual_asset` / `ana_correction_feedback` / `med_risk_assessment_record` 均已在 V001 基线内 |

## 9. 后续维护约定

- 任何「新增表 / 改动归属 / 跨界字段变更」必须先在本文档登记。
- Phase 1 收尾 `ana_visual_asset` 落库正式化时，本文档无需改动。
- Phase 2 报告模块新增 `rpt_*` 字段时，仅在第 4.5 节追加。
- Phase 3 RAG 接入 Java 业务表面时，新增的 Java 侧表仅做「展现快照」，正本仍留在 `caries_ai`。
- Phase 4 Python 切换 Alembic 后，§7 表中「当前由 ensure_schema」一行同步更新。
