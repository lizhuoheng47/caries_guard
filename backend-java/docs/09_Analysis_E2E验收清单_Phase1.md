# Analysis E2E 验收清单（Phase 1：目标态收口）

> 本文档配套《下一阶段逐模块开发清单 v2》Phase 1。  
> 目的：在进入 Report 主线开发之前，把 `visualAssets → med_attachment → ana_visual_asset` 这条主链路、以及医生纠错回路，固化成一份可重复执行、可逐项打勾的验收记录。

---

## 0. 适用范围

- 仅用于本地 / 联调环境的「目标态」E2E 验收。
- 触发方式：通过 Java `POST /api/v1/analysis/tasks` 创建 analysis 任务，待 Python pipeline 完成后回调 Java，再由人工运行下方 SQL / MinIO 检查。
- 验收 3 条标准样例（见 §5）。

---

## 1. 关键配置确认（每次环境切换都要核对一遍）

| 项 | 期望值 | 说明 |
| --- | --- | --- |
| `CG_CALLBACK_VISUAL_ASSET_MODE` | `metadata` | Python 端：必须发送顶层 `visualAssets` 的 `bucketName/objectKey/contentType/fileSizeBytes/md5`，让 Java 自行登记 attachment。Docker compose 已默认设置（[docker-compose.yml:171](../../docker-compose.yml#L171)）。 |
| `CARIES_ANALYSIS_CALLBACK_SECRET` / `CG_ANALYSIS_CALLBACK_SECRET` | 两端一致 | HMAC 签名校验。 |
| `CARIES_BUCKET_VISUAL` / `CG_MINIO_BUCKET_VISUAL` | `caries-visual` | 视觉产物 bucket 名一致。 |
| `CG_AI_DOWNLOAD_IMAGES` | `true` | Python 必须真实下载源图，否则 visual asset 上传会被跳过。 |

> 任何一项不符，整个验收作废，重新跑。

---

## 2. T1：Java 回调目标态再验收

### 2.1 单元测试覆盖

[AnalysisCallbackAppServiceTests](../caries-analysis/src/test/java/com/cariesguard/analysis/app/AnalysisCallbackAppServiceTests.java) 已在本轮新增三个用例，覆盖 metadata 模式：

- `handleSuccessCallbackWithMetadataAssetsShouldRegisterAttachments` —— 顶层 `visualAssets` 仅带 `bucketName/objectKey/...` 时，Java 自动调用 `AttachmentAppService.registerExternalObject` 登记 attachment 并把生成的 `attachmentId` 写入 `ana_visual_asset`。同时校验登记字段（`bizModuleCode=ANALYSIS`、`fileCategoryCode=VISUAL`、`caseNo/taskNo/modelVersion/relatedImageId/toothCode/sourceAttachmentId/orgId` 全部正确）。
- `handleSuccessCallbackWithoutAttachmentIdOrMetadataShouldReject` —— 既无 `attachmentId` 又无 `bucketName/objectKey` 的非法回调被拒，且不会调用 `registerExternalObject`。
- `handleSuccessCallbackWithMismatchedRelatedImageShouldReject` —— `relatedImageId` 指向其他 case 的图像时直接拒绝，不会污染 `ana_visual_asset`。

### 2.2 跑通命令

```bash
cd backend-java
mvn -pl caries-analysis -am test -Dtest=AnalysisCallbackAppServiceTests
```

期望：`Tests run: 11, Failures: 0, Errors: 0`。

### 2.3 完成口径

- [ ] 上述命令通过
- [ ] Docker 环境下 Python 真跑一次任务，Java 收到的 `success` 回调返回 200
- [ ] `ana_task_record.task_status_code='SUCCESS'`（见 §4 SQL 1）
- [ ] `ana_visual_asset` 入库行数 > 0（见 §4 SQL 2）

---

## 3. T2：visualAsset 数据一致性核对

把同一个 `taskNo` 在「MinIO 对象 / `med_attachment` 登记 / `ana_visual_asset` 业务快照 / `raw_result_json.visualAssets`」四层对齐。

### 3.1 一次性查询脚本（手动执行）

把下列 `:taskNo` 替换成被验收的 task no（如 `'AT20260417000001'`），按顺序在 `caries_biz` 库执行：

```sql
-- SQL 1：任务终态
SELECT id, task_no, task_status_code, completed_at, model_version, error_code
FROM ana_task_record
WHERE task_no = :taskNo;

-- SQL 2：业务快照层（ana_visual_asset）
SELECT id, task_id, case_id, asset_type_code, attachment_id,
       related_image_id, source_attachment_id, tooth_code, model_version
FROM ana_visual_asset
WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = :taskNo);

-- SQL 3：附件登记层（med_attachment）—— 核对 ana_visual_asset.attachment_id 都能反查到
SELECT id, biz_module_code, biz_id, file_category_code, asset_type_code,
       source_attachment_id, bucket_name, object_key, file_size_bytes, md5,
       org_id, status
FROM med_attachment
WHERE id IN (
  SELECT attachment_id FROM ana_visual_asset
  WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = :taskNo)
);

-- SQL 4：原始 callback 留痕（raw_result_json.visualAssets）
SELECT JSON_EXTRACT(raw_result_json, '$.visualAssets') AS callback_visual_assets,
       JSON_LENGTH(JSON_EXTRACT(raw_result_json, '$.visualAssets')) AS callback_count
FROM ana_result_summary
WHERE task_id = (SELECT id FROM ana_task_record WHERE task_no = :taskNo);
```

### 3.2 MinIO 对象核对

```bash
# 列出该 task 的所有 visual 对象
mc ls --recursive caries/caries-visual/ | grep <case_no>/analysis/<task_no>/
```

任选 1～2 个对象，确认它在 SQL 3 的 `bucket_name`/`object_key` 列里能找到。

### 3.3 一致性判定（每条样例都要满足）

| 维度 | 期望关系 |
| --- | --- |
| MinIO 对象数 | = SQL 3 行数 = SQL 2 行数 |
| SQL 4 的 `callback_count` | = SQL 2 行数（顶层 visualAssets 的元数据应该和最终入库一一对应） |
| `med_attachment.biz_module_code` | 全部为 `ANALYSIS` |
| `med_attachment.file_category_code` | 全部为 `VISUAL` |
| `med_attachment.asset_type_code` | ∈ {`MASK`, `OVERLAY`, `HEATMAP`} |
| `med_attachment.source_attachment_id` | = 对应原始口腔图 `med_attachment.id`（即 `relatedImageId` 反查出来的 attachment） |
| `ana_visual_asset.org_id` / `med_attachment.org_id` | = 任务所属 case 的 `org_id` |
| `ana_visual_asset.model_version` | = `ana_task_record.model_version`（或 callback 显式上报的版本） |
| `ana_visual_asset.tooth_code` | OVERLAY/MASK 必填，HEATMAP 可空 |

### 3.4 完成口径

- [ ] 3 条样例的 §3.3 检查全部通过
- [ ] 任一样例发现「MinIO 有对象但 `med_attachment` 没登记」或「`ana_visual_asset` 指向不存在的 `attachment_id`」，整轮验收作废

---

## 4. T3：医生纠错回路验收

### 4.1 单元测试覆盖

[CorrectionFeedbackAppServiceTests](../caries-analysis/src/test/java/com/cariesguard/analysis/app/CorrectionFeedbackAppServiceTests.java) 已覆盖：

- 正常提交并落 `ana_correction_feedback`
- case 状态不允许纠错时拒绝
- 跨组织访问拒绝（非 ADMIN 角色）
- `sourceImageId` 不属于本 case 时拒绝

```bash
mvn -pl caries-analysis -am test -Dtest=CorrectionFeedbackAppServiceTests
```

### 4.2 联调验收

针对 §5 的 case-A：

1. case 状态被 SUCCESS 回调推到 `REVIEW_PENDING` 后，由医生账号调用 `POST /api/v1/analysis/correction-feedback`。
2. 校验：

```sql
SELECT id, case_id, diagnosis_id, source_image_id, source_attachment_id,
       feedback_type_code, review_status_code, created_by, created_at
FROM ana_correction_feedback
WHERE case_id = :caseId
ORDER BY id DESC LIMIT 5;
```

期望：

- 至少出现一条新行，`feedback_type_code` ∈ {`RE_GRADE`, `RE_LOCATE`, `RE_TYPE`, `RE_RISK`, `OTHER`}
- `source_attachment_id` 与 `source_image_id` 关联的 `med_image_file.attachment_id` 一致
- `created_by` 为执行医生的 `userId`，`created_at` 为提交时间
- `review_status_code='PENDING'`，等待后续审核

### 4.3 完成口径

- [ ] 单测通过
- [ ] 至少完成 1 条 correction 的「创建 → 查询 → 列表展示」全链路
- [ ] correction 行可被后续 report 模块读到（验收时只需确认能查询到，Report 实际消费在 Phase 2）

---

## 5. 标准验收样例（3 条，每轮验收都要全跑一遍）

| 样例 | 关键属性 | 预期分支 |
| --- | --- | --- |
| **case-A：典型成功** | 1 张 PERIAPICAL，1 颗目标牙 16，severity=C1，uncertainty=0.10 | Java 回调 SUCCESS → 写 result/visual/risk → case → REVIEW_PENDING → 医生提交 1 条 correction |
| **case-B：高不确定性 + review** | 2 张图，severity=C2，`reviewSuggestedFlag=1`，uncertainty=0.45 | 同 SUCCESS 主链路；`ana_result_summary.review_suggested_flag='1'`；进入复核队列 |
| **case-C：失败回退** | Python pipeline 主动抛 BusinessException（如缺 image） | Java 回调 FAILED → `ana_task_record.task_status_code='FAILED'` 且 `error_code/error_message` 非空 → case → QC_PENDING；`ana_visual_asset` 行数为 0 |

每条样例都要走完下表 8 项：

| 检查项 | case-A | case-B | case-C |
| --- | --- | --- | --- |
| 1. task 入库 (`ana_task_record`) |  |  |  |
| 2. image 真实下载（Python 工作目录有源图 / 失败可空） |  |  |  |
| 3. visual upload 到 MinIO（C 不要求） |  |  |  |
| 4. callback 返回 200 |  |  |  |
| 5. `ana_result_summary` 行（C 不要求） |  |  |  |
| 6. `ana_visual_asset` 行（C 应为 0） |  |  |  |
| 7. `med_risk_assessment_record` 行（C 不要求） |  |  |  |
| 8. correction 链路（仅 A，可选 B） |  |  |  |

> 全部打勾即为 Phase 1 通过。

---

## 6. Phase 1 收口产出

- [ ] 本文档每个 [ ] 都被勾选
- [ ] 上述 SQL/MinIO 检查的执行结果（截图或导出）归档到 `Documents/E2E验收记录/Phase1/<日期>/`
- [ ] Phase 1 收尾后，正式进入 Phase 2（Report 模块完整落地）：见 [下一阶段逐模块开发清单_v2.md](../../下一阶段逐模块开发清单_v2.md) §6

---

## 7. 已知边界与注意事项

- 回调 metadata 模式不向后兼容「Python 自己 pre-register attachment 再回填 attachmentId」的老路径——如果上游 mock 仍在用 `attachmentId` 直传，回调依旧能工作（[AnalysisCallbackAppService](../caries-analysis/src/main/java/com/cariesguard/analysis/app/AnalysisCallbackAppService.java#L286)），但生产环境强制通过 metadata 模式登记，避免重复登记。
- `relatedImageId` 必须属于本 case，否则拒绝（[AnalysisCallbackAppService:347-350](../caries-analysis/src/main/java/com/cariesguard/analysis/app/AnalysisCallbackAppService.java#L347-L350)），跨 case 引用是数据脏的强信号。
- `assetTypeCode` 必填，且会被规范化为大写（[AnalysisCallbackAppService:324-329](../caries-analysis/src/main/java/com/cariesguard/analysis/app/AnalysisCallbackAppService.java#L324-L329)）。
- `ana_visual_asset.replaceByTaskId` 是「按 taskId 全量覆盖」，重跑同一 task 不会留下旧行——便于失败重试，但联调时不要在重跑后期望「旧行 + 新行」共存。
