# CariesGuard —— Java ↔ Python 接口契约对照表

> 文档代号：CONTRACT-V1
> 文档定位：Java 后端与 Python AI 服务之间所有字段、状态码、枚举值的**唯一权威对照表**
> 适用对象：Java 后端工程师、Python 算法服务工程师、联调工程师、测试工程师、答辩准备人员
> 口径约定：
> 1. 本文档以 **Java 端已实现的 DTO 和数据库表为既成事实**，Python 端必须严格反向对齐
> 2. 凡 Python 端发给 Java 的回调字段，必须可以直接被 Java 端 MyBatis-Plus 的 DO/DTO 映射落库
> 3. 所有字段名一律 **camelCase**（与 Java 命名规范 05 号文档 §9.1 一致）
> 4. 所有枚举值一律 **全大写 + 下划线 / 短横线**
> 5. 本文档任何一处变更，都必须在 Java、Python、前端三方同步更新后才能生效

---

## 目录

1. 术语与标识符约定
2. Bucket 与 Object Key 约定
3. 模块路由与请求方法一览
4. 通用响应体结构
5. 通用错误码与任务状态码
6. 接口 1：影像质量检查（同步）
7. 接口 2：综合分析任务（异步）
8. 接口 3：风险评估（同步）
9. 接口 4：模型版本查询（同步）
10. 回调接口 1：分析完成回调
11. 回调接口 2：分析失败回调
12. RabbitMQ 消息契约
13. 幂等性与重试策略
14. 字段级反向对齐清单
15. 联调 Checklist
16. 契约变更流程

---

## 1. 术语与标识符约定

### 1.1 主键标识符

| 标识符 | 类型 | 说明 | 产生方 | 长度约束 |
|---|---|---|---|---|
| `traceId` | string | 全链路追踪 ID，每次请求由 Java 生成一次，透传到 Python、日志、MQ、回调 | Java 网关层 | 最长 64 |
| `taskNo` | string | AI 任务业务编号，唯一定位一次分析任务 | Java `caries-analysis` | 最长 32，格式 `TASK` + 14 位时间 + 4 位序号 |
| `caseId` | long | 病例主键，对应 `med_case.case_id` | Java | 64 位整型 |
| `patientId` | long | 患者主键，对应 `pat_patient.patient_id` | Java | 64 位整型 |
| `orgId` | long | 机构 ID，用于数据隔离 | Java | 64 位整型 |
| `imageId` | long | 影像主键，对应 `med_image_file.image_id` | Java | 64 位整型 |
| `modelVersion` | string | 当前使用的模型版本号，格式 `cg_release_YYYY_MM_vN` | Java 读取 Python 发布表 | 最长 64 |
| `callbackUrl` | string | Python 回调 Java 的完整 URL | Java | HTTPS 优先 |
| `callbackToken` | string | Java 预生成的一次性签名 token，Python 回调时原样带回 | Java | 最长 256 |

### 1.2 时间字段

统一约定：

- 所有时间字段 **ISO-8601** 格式带时区，例如 `2026-04-13T10:00:12+08:00`
- 字段命名：`createdAt` / `updatedAt` / `startedAt` / `completedAt` / `callbackAt`
- 数据库落库时由 Java 侧统一转换为 `DATETIME(3)`，Python 端**不需要**感知数据库时区

### 1.3 编码与语言

- 请求/响应体一律 **UTF-8**
- Content-Type：`application/json`
- 文本字段如遇中文，Java 端已启用 Jackson UTF-8 配置，Python 端使用 `orjson` 或标准 `json` 均可
- Python 回调时 HTTP header 必须包含 `Content-Type: application/json; charset=utf-8`

---

## 2. Bucket 与 Object Key 约定

**已冻结**（与 05 号文档 §10 一致）：

| Bucket 名 | 用途 | 读写权限 |
|---|---|---|
| `caries-image` | 原始影像（全景片 / 根尖片 / 口内照） | Java 写，Python 读 |
| `caries-visual` | 分析可视化产物（mask、overlay、热力图） | Python 写，Java 读 |
| `caries-report` | 生成的 PDF 报告 | Java 写，Java 读 |
| `caries-export` | 用户导出的临时文件 | Java 写 |

### Object Key 规则

```text
RAW_IMAGE:
org/{orgId}/case/{caseNo}/image/{imageTypeCode}/{yyyy}/{MM}/{dd}/{attachmentId}/{filename}

VISUAL:
org/{orgId}/case/{caseNo}/analysis/{taskNo}/{modelVersion}/{assetTypeCode}/{relatedImageId}/{toothCode}/{attachmentId}.{ext}

REPORT:
org/{orgId}/case/{caseNo}/report/{reportTypeCode}/v{versionNo}/{reportNo}.pdf

EXPORT:
org/{orgId}/export/{yyyy}/{MM}/{dd}/{operatorId}/{exportLogId}/{reportNo}.{ext}
```

规则要点：
- `caseNo` 不是 `caseId`，是 Java 端生成的对外病例号（如 `CASE202604130001`）
- Python 端回调可视化产物时使用 `VISUAL` 语义，必须带 `bucketName + objectKey`
- Python 端 **禁止** 写入 `caries-image` 和 `caries-report` 两个 bucket
- `caries-visual` 默认 30 天自动清理，`caries-export` 默认 7 天自动清理

---

## 3. 模块路由与请求方法一览

### 3.1 Java → Python（Java 调用方，Python 是服务端）

| 路径 | 方法 | 模式 | 用途 | 超时（Java 侧）|
|---|---|---|---|---|
| `/ai/v1/health` | GET | 同步 | 健康检查 | 2s |
| `/ai/v1/quality-check` | POST | 同步 | 影像质量检查，单张 | 10s |
| `/ai/v1/analyze` | POST | 异步受理 | 提交综合分析任务 | 5s（只等受理）|
| `/ai/v1/assess-risk` | POST | 同步 | 风险融合评估 | 8s |
| `/ai/v1/model-version` | GET | 同步 | 查询当前推理模型版本 | 3s |

**冻结口径**：
- Python FastAPI 根路径 **必须** 是 `/ai/v1`
- 凡 `analyze` 接口，Python 只返回"已受理"，真正结果通过回调送回
- 所有接口 Python 必须支持 **HEAD + GET** 健康探针（Java 侧做 liveness 检查）

### 3.2 Python → Java（Python 调用方，Java 是服务端）

| 路径 | 方法 | 用途 | 签名机制 |
|---|---|---|---|
| `/api/v1/ai/callbacks/analysis` | POST | 分析完成回调（成功/失败统一此端点） | `X-Callback-Token` header |

**安全约定**：
- Java 在发起 `/ai/v1/analyze` 时生成一次性 `callbackToken`，写入 Redis（key = taskNo，ttl = 2h）
- Python 回调时在 HTTP header `X-Callback-Token` 里原样带回
- Java 侧验证 token 存在性、未使用、未过期，验证通过后从 Redis 删除（防重放）
- **Python 不保存 token，只保存 callbackUrl，每次回调都从任务上下文里取 token**

---

## 4. 通用响应体结构

**Java 和 Python 双向统一使用以下包装结构**（与 Java 端 `com.cariesguard.common.api.R` 一致）：

```json
{
  "code": "00000",
  "message": "success",
  "data": { ... },
  "traceId": "trace-xxxxx",
  "timestamp": "2026-04-13T10:00:00+08:00"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `code` | string | 是 | 5 位业务错误码，`00000` 表示成功 |
| `message` | string | 是 | 业务消息文本，成功时为 `success` |
| `data` | object / null | 否 | 业务数据；失败时为 `null` |
| `traceId` | string | 是 | 原样回传请求里的 traceId |
| `timestamp` | string | 是 | 响应生成时间，ISO-8601 |

**Python 端实现要点**：
- 所有路由函数的返回值必须走 `ApiResponse` 统一包装
- 业务异常通过自定义 `BusinessException` 抛出，由全局 exception handler 转为标准响应
- 框架层 `RequestValidationError` 必须被拦截并转为 `code=A0400` 的业务错误响应，**不能** 直接返回 FastAPI 默认的 422

---

## 5. 通用错误码与任务状态码

### 5.1 错误码（Python 必须使用以下码值，Java 已定义）

| code | 含义 | HTTP 状态 | 使用场景 |
|---|---|---|---|
| `00000` | 成功 | 200 | 正常返回 |
| `A0400` | 请求参数错误 | 200 | Schema 校验失败、字段类型错 |
| `A0401` | 未授权 / token 无效 | 200 | 签名失败 |
| `A0404` | 资源不存在 | 200 | 影像 object key 在 MinIO 中找不到 |
| `B0001` | 业务规则冲突 | 200 | 任务已被处理、状态不允许 |
| `B0002` | 模型未加载 | 200 | 服务启动阶段模型未就绪 |
| `B0003` | 影像质量不合格 | 200 | quality-check 主动判定为拒收 |
| `C3001` | AI 服务不可用 | 200 | Python 端内部异常兜底 |
| `C3002` | 模型推理超时 | 200 | 单步推理超过预设阈值 |
| `C3003` | 下游依赖失败 | 200 | MinIO / MQ / Java 回调不通 |
| `C9999` | 未知异常 | 200 | 最后兜底 |

**注意**：HTTP 状态码几乎永远是 200（除非框架层完全崩溃），业务成功/失败通过 `code` 字段区分。这与 Java 端 `GlobalExceptionHandler` 的约定一致。

### 5.2 任务状态码 `taskStatusCode`

```text
QUEUEING       -- 已受理，等待 Python worker 拉取
PROCESSING     -- Python 正在推理
SUCCESS        -- 推理完成，回调已发送
FAILED         -- 推理失败，回调已发送
QUALITY_REJECT -- 质量检查未通过，不进入推理
TIMEOUT        -- 处理超时（由 Java 侧基于任务创建时间判定）
CANCELLED      -- 用户主动取消
```

**状态机冻结**：

```text
QUEUEING → PROCESSING → SUCCESS
QUEUEING → PROCESSING → FAILED
QUEUEING → QUALITY_REJECT
QUEUEING → CANCELLED
任意状态 → TIMEOUT（Java 侧扫描 ana_task_record 触发）
```

### 5.3 病灶分级码 `severityCode`

```text
C0 -- 无龋 / 健康
C1 -- 浅龋（釉质层）
C2 -- 中龋（牙本质层浅）
C3 -- 深龋（近髓或已达髓）
```

### 5.4 风险等级码 `riskLevelCode`

```text
LOW
MEDIUM
HIGH
```

### 5.5 影像类型码 `imageTypeCode`

```text
PANORAMIC  -- 全景片
PERIAPICAL -- 根尖片
BITEWING   -- 咬翼片（可选）
INTRAORAL  -- 口内照
```

### 5.6 复核建议标志 `reviewRecommendedFlag` / `highRiskFlag`

- 统一使用字符串 `"0"` / `"1"`
- **不要** 使用 boolean true/false（Java 端数据库用的是 `char(1)`）

---

## 6. 接口 1：影像质量检查（同步）

### 6.1 Java → Python 请求

`POST /ai/v1/quality-check`

```json
{
  "traceId": "trace-001",
  "taskNo": "TASK202604130001",
  "caseId": 10001,
  "imageId": 90001,
  "patientId": 70001,
  "orgId": 1001,
  "imageTypeCode": "PANORAMIC",
  "bucketName": "caries-image",
  "objectKey": "org/1001/case/CASE202604130001/image/PANORAMIC/2026/04/13/90001/original_01.jpg"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `traceId` | string | 是 | 链路追踪 |
| `taskNo` | string | 是 | 对应 `med_image_quality_check.check_task_no` |
| `caseId` | long | 是 | |
| `imageId` | long | 是 | |
| `patientId` | long | 是 | |
| `orgId` | long | 是 | 数据隔离 |
| `imageTypeCode` | string | 是 | 枚举见 §5.5 |
| `bucketName` | string | 是 | 固定 `caries-image` |
| `objectKey` | string | 是 | MinIO 对象键 |

### 6.2 Python → Java 响应

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "checkResultCode": "PASS",
    "qualityScore": 86,
    "blurScore": 84,
    "exposureScore": 88,
    "integrityScore": 90,
    "occlusionScore": 81,
    "issueCodes": [],
    "suggestionText": "质量合格"
  },
  "traceId": "trace-001",
  "timestamp": "2026-04-13T10:00:00+08:00"
}
```

| 字段 | 类型 | 必填 | 值域 / 说明 | 对应 Java 落库字段 |
|---|---|---|---|---|
| `checkResultCode` | string | 是 | `PASS` / `WARN` / `REJECT` | `med_image_quality_check.check_result_code` |
| `qualityScore` | int | 是 | 0~100，综合分 | `quality_score` |
| `blurScore` | int | 否 | 0~100，模糊度 | `blur_score` |
| `exposureScore` | int | 否 | 0~100，曝光度 | `exposure_score` |
| `integrityScore` | int | 否 | 0~100，完整度 | `integrity_score` |
| `occlusionScore` | int | 否 | 0~100，遮挡度 | `occlusion_score` |
| `issueCodes` | string[] | 是 | 问题代码数组，见下 | `issue_codes_json` |
| `suggestionText` | string | 是 | 给前端展示的建议文本 | `suggestion_text` |

**`issueCodes` 枚举值**：
```text
BLUR           -- 画面模糊
OVEREXPOSURE   -- 过曝
UNDEREXPOSURE  -- 欠曝
OCCLUSION      -- 金属/异物遮挡
INCOMPLETE     -- 影像不完整
WRONG_TYPE     -- 影像类型不匹配
LOW_RESOLUTION -- 分辨率不足
```

---

## 7. 接口 2：综合分析任务（异步）

### 7.1 Java → Python 请求

`POST /ai/v1/analyze`

```json
{
  "traceId": "trace-002",
  "taskNo": "TASK202604130002",
  "caseId": 10001,
  "patientId": 70001,
  "orgId": 1001,
  "caseNo": "CASE202604130001",
  "imageIds": [90001, 90002],
  "images": [
    {
      "imageId": 90001,
      "imageTypeCode": "PANORAMIC",
      "bucketName": "caries-image",
      "objectKey": "org/1001/case/CASE202604130001/image/PANORAMIC/2026/04/13/90001/pan_01.jpg",
      "originalFilename": "pan_01.jpg",
      "widthPx": 2880,
      "heightPx": 1504
    },
    {
      "imageId": 90002,
      "imageTypeCode": "INTRAORAL",
      "bucketName": "caries-image",
      "objectKey": "org/1001/case/CASE202604130001/image/INTRAORAL/2026/04/13/90002/intra_01.jpg",
      "originalFilename": "intra_01.jpg",
      "widthPx": 1920,
      "heightPx": 1080
    }
  ],
  "patientProfile": {
    "age": 28,
    "genderCode": "M",
    "brushingFrequencyCode": "TWICE_DAILY",
    "sugarDietLevelCode": "MEDIUM",
    "fluorideUseFlag": "1",
    "previousCariesCount": 2,
    "lastDentalCheckMonths": 18
  },
  "modelVersion": "cg_release_2026_04_v1",
  "callbackUrl": "http://java-backend:8080/api/v1/ai/callbacks/analysis",
  "callbackToken": "c7b2...a1f9",
  "requestedAt": "2026-04-13T10:00:00+08:00"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `traceId` | string | 是 | |
| `taskNo` | string | 是 | 唯一任务号 |
| `caseId` | long | 是 | |
| `patientId` | long | 是 | |
| `orgId` | long | 是 | |
| `caseNo` | string | 是 | 对外病例号，Python 端用它拼 objectKey |
| `imageIds` | long[] | 是 | 冗余字段，便于幂等 |
| `images` | Image[] | 是 | 影像清单，见下 |
| `patientProfile` | object | 否 | 患者画像，用于后续风险融合 |
| `modelVersion` | string | 否 | 不传则 Python 使用当前默认版本 |
| `callbackUrl` | string | 是 | Python 完成后必须回调此地址 |
| `callbackToken` | string | 是 | 回调时放在 `X-Callback-Token` header |
| `requestedAt` | string | 是 | Java 发起时间 |

**Image 对象字段**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `imageId` | long | 是 | |
| `imageTypeCode` | string | 是 | §5.5 |
| `bucketName` | string | 是 | 固定 `caries-image` |
| `objectKey` | string | 是 | |
| `originalFilename` | string | 否 | 记录追溯用 |
| `widthPx` | int | 否 | Java 侧上传时已计算 |
| `heightPx` | int | 否 | |

**patientProfile 字段**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `age` | int | 年龄 |
| `genderCode` | string | `M` / `F` / `U` |
| `brushingFrequencyCode` | string | `NONE` / `ONCE_DAILY` / `TWICE_DAILY` / `MORE` |
| `sugarDietLevelCode` | string | `LOW` / `MEDIUM` / `HIGH` |
| `fluorideUseFlag` | string | `"0"` / `"1"` |
| `previousCariesCount` | int | 既往龋齿数 |
| `lastDentalCheckMonths` | int | 上次检查距今月份数 |

### 7.2 Python → Java 响应（同步受理）

```json
{
  "code": "00000",
  "message": "accepted",
  "data": {
    "taskNo": "TASK202604130002",
    "taskStatusCode": "QUEUEING",
    "estimatedSeconds": 12
  },
  "traceId": "trace-002",
  "timestamp": "2026-04-13T10:00:00+08:00"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `taskNo` | string | 原样返回 |
| `taskStatusCode` | string | 首次受理固定 `QUEUEING` |
| `estimatedSeconds` | int | 预计完成秒数，便于前端展示进度条 |

---

## 8. 接口 3：风险评估（同步）

`POST /ai/v1/assess-risk`

> **说明**：风险评估有两种触发方式：
> 1. 由综合分析流水线内部调用（推荐主路径）
> 2. 由 Java 单独触发（用于"只想重新算一次风险等级"的场景）
> 本接口契约覆盖第 2 种。

### 8.1 Java → Python 请求

```json
{
  "traceId": "trace-003",
  "caseId": 10001,
  "patientId": 70001,
  "imageSummary": {
    "overallHighestSeverity": "C2",
    "suspiciousToothCount": 3,
    "overallUncertaintyScore": 0.23,
    "lesionAreaRatio": 0.017
  },
  "patientProfile": {
    "age": 28,
    "genderCode": "M",
    "brushingFrequencyCode": "TWICE_DAILY",
    "sugarDietLevelCode": "MEDIUM",
    "fluorideUseFlag": "1",
    "previousCariesCount": 2,
    "lastDentalCheckMonths": 18
  },
  "modelVersion": "cg_risk_fusion_2026_04_v1"
}
```

### 8.2 Python → Java 响应

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "riskLevelCode": "MEDIUM",
    "riskScore": 62,
    "recommendedCycleDays": 180,
    "explanationFactors": [
      {"featureCode": "previous_caries_count", "contribution": 0.31, "direction": "POSITIVE"},
      {"featureCode": "overall_highest_severity", "contribution": 0.27, "direction": "POSITIVE"},
      {"featureCode": "sugar_diet_level", "contribution": 0.18, "direction": "POSITIVE"},
      {"featureCode": "fluoride_use", "contribution": -0.09, "direction": "NEGATIVE"}
    ],
    "modelVersion": "cg_risk_fusion_2026_04_v1"
  },
  "traceId": "trace-003",
  "timestamp": "2026-04-13T10:00:08+08:00"
}
```

| 字段 | 类型 | 说明 | 对应 Java 落库字段 |
|---|---|---|---|
| `riskLevelCode` | string | §5.4 | `med_risk_assessment_record.risk_level_code` |
| `riskScore` | int | 0~100 | `risk_score` |
| `recommendedCycleDays` | int | 推荐复查周期天数 | `recommended_cycle_days` |
| `explanationFactors` | object[] | SHAP 解释因子，按 |contribution| 降序 | `explanation_factors_json` |
| `modelVersion` | string | 风险融合模型版本 | `risk_model_version` |

**explanationFactors 元素字段**：
- `featureCode`：特征代号，小写下划线
- `contribution`：SHAP 贡献值，范围 -1~1
- `direction`：`POSITIVE` 表示推高风险，`NEGATIVE` 表示降低风险

---

## 9. 接口 4：模型版本查询（同步）

`GET /ai/v1/model-version`

### 9.1 响应

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "toothDetect": {
      "modelVersion": "cg_tooth_detect_2026_03_v2",
      "modelArtifactMd5": "a3f5...",
      "releasedAt": "2026-03-15T10:00:00+08:00"
    },
    "tpcNet": {
      "modelVersion": "cg_tpc_net_2026_04_v1",
      "modelArtifactMd5": "b4a2...",
      "releasedAt": "2026-04-02T10:00:00+08:00"
    },
    "edlGrade": {
      "modelVersion": "cg_edl_grade_2026_04_v1",
      "modelArtifactMd5": "c9d7...",
      "releasedAt": "2026-04-02T10:00:00+08:00"
    },
    "riskFusion": {
      "modelVersion": "cg_risk_fusion_2026_04_v1",
      "modelArtifactMd5": "e1f3...",
      "releasedAt": "2026-04-05T10:00:00+08:00"
    }
  },
  "traceId": "trace-x",
  "timestamp": "2026-04-13T10:00:00+08:00"
}
```

用途：
- Java `caries-analysis` 模块在提交 `/ai/v1/analyze` 前调用一次，把 `modelVersion` 字段写进 `ana_task_record.model_version_code`
- 答辩演示时前端直接调用展示"当前推理栈版本"

---

## 10. 回调接口 1：分析完成回调

`POST {callbackUrl}`（由 Java 端决定实际路径，当前冻结为 `/api/v1/ai/callbacks/analysis`）

### 10.1 请求 Header

```text
Content-Type: application/json; charset=utf-8
X-Callback-Token: c7b2...a1f9
X-Trace-Id: trace-002
```

### 10.2 Python → Java 请求体

```json
{
  "traceId": "trace-002",
  "taskNo": "TASK202604130002",
  "taskStatusCode": "SUCCESS",
  "caseId": 10001,
  "patientId": 70001,
  "orgId": 1001,
  "modelVersion": "cg_release_2026_04_v1",
  "startedAt": "2026-04-13T10:00:01+08:00",
  "completedAt": "2026-04-13T10:00:12+08:00",
  "summary": {
    "overallHighestSeverity": "C2",
    "suspiciousToothCount": 3,
    "overallUncertaintyScore": 0.23,
    "lesionAreaRatio": 0.017,
    "reviewRecommendedFlag": "1",
    "highRiskFlag": "0"
  },
  "toothDetections": [
    {
      "imageId": 90001,
      "toothCode": "16",
      "bbox": [112, 244, 188, 320],
      "detectionScore": 0.95
    },
    {
      "imageId": 90001,
      "toothCode": "26",
      "bbox": [530, 248, 604, 324],
      "detectionScore": 0.92
    }
  ],
  "lesionResults": [
    {
      "imageId": 90001,
      "toothCode": "16",
      "severityCode": "C2",
      "uncertaintyScore": 0.21,
      "lesionAreaPx": 482,
      "lesionAreaRatio": 0.009,
      "maskAsset": {
        "bucketName": "caries-visual",
        "objectKey": "org/1001/case/CASE202604130001/analysis/TASK202604130001/caries-v1/MASK/90001/16/91001.png",
        "widthPx": 2880,
        "heightPx": 1504
      },
      "overlayAsset": {
        "bucketName": "caries-visual",
        "objectKey": "org/1001/case/CASE202604130001/analysis/TASK202604130001/caries-v1/OVERLAY/90001/16/91002.png"
      }
    }
  ],
  "riskAssessment": {
    "riskLevelCode": "MEDIUM",
    "riskScore": 62,
    "recommendedCycleDays": 180,
    "explanationFactors": [
      {"featureCode": "previous_caries_count", "contribution": 0.31, "direction": "POSITIVE"},
      {"featureCode": "overall_highest_severity", "contribution": 0.27, "direction": "POSITIVE"}
    ],
    "modelVersion": "cg_risk_fusion_2026_04_v1"
  },
  "qualityCheckResults": [
    {
      "imageId": 90001,
      "checkResultCode": "PASS",
      "qualityScore": 86
    },
    {
      "imageId": 90002,
      "checkResultCode": "PASS",
      "qualityScore": 90
    }
  ],
  "rawResultJson": {
    "pipelineVersion": "p1.3",
    "gpuDeviceName": "NVIDIA RTX 4090",
    "inferenceSecondsByStage": {
      "preprocess": 0.8,
      "toothDetect": 1.2,
      "segmentation": 4.6,
      "grading": 1.1,
      "riskFusion": 0.3,
      "explain": 2.8
    }
  }
}
```

### 10.3 字段级说明（最关键的一张表）

| 顶层字段 | 类型 | 必填 | 对应 Java 落库位置 | 说明 |
|---|---|---|---|---|
| `traceId` | string | 是 | `ana_task_record.trace_id` | |
| `taskNo` | string | 是 | `ana_task_record.task_no` | 主键定位 |
| `taskStatusCode` | string | 是 | `ana_task_record.task_status_code` | §5.2 |
| `caseId` | long | 是 | - | Java 通过 taskNo 反查，但必须传用于幂等校验 |
| `patientId` | long | 是 | - | 同上 |
| `orgId` | long | 是 | - | 同上 |
| `modelVersion` | string | 是 | `ana_task_record.model_version_code` | |
| `startedAt` | string | 是 | `ana_task_record.started_at` | |
| `completedAt` | string | 是 | `ana_task_record.completed_at` | |
| `summary` | object | 是 | `ana_result_summary.*` | 见下 |
| `toothDetections` | array | 否 | `med_case_tooth_record` 批量插入 | 见下 |
| `lesionResults` | array | 是 | `ana_result_summary.lesions_json` + `ana_visual_asset` | 见下 |
| `riskAssessment` | object | 否 | `med_risk_assessment_record.*` | 见下 |
| `qualityCheckResults` | array | 否 | `med_image_quality_check.*` 追加 | |
| `rawResultJson` | object | 否 | `ana_result_summary.raw_result_json` | Python 任意结构，Java 只透传 |

**summary 子字段**：

| 字段 | 类型 | 必填 | Java 落库字段 |
|---|---|---|---|
| `overallHighestSeverity` | string | 是 | `ana_result_summary.overall_highest_severity` |
| `suspiciousToothCount` | int | 是 | `ana_result_summary.suspicious_tooth_count` |
| `overallUncertaintyScore` | float | 是 | `ana_result_summary.overall_uncertainty_score` |
| `lesionAreaRatio` | float | 否 | `ana_result_summary.lesion_area_ratio` |
| `reviewRecommendedFlag` | string | 是 | `ana_result_summary.review_recommended_flag` |
| `highRiskFlag` | string | 是 | `ana_result_summary.high_risk_flag` |

**toothDetections 元素字段**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `imageId` | long | 是 | |
| `toothCode` | string | 是 | FDI 编号，两位字符，如 `"16"`、`"37"` |
| `bbox` | int[4] | 是 | `[x1, y1, x2, y2]`，左上 + 右下 |
| `detectionScore` | float | 是 | YOLO 置信度 0~1 |

**lesionResults 元素字段**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `imageId` | long | 是 | |
| `toothCode` | string | 否 | 若分割结果无法映射到具体牙位可留空 |
| `severityCode` | string | 是 | §5.3 |
| `uncertaintyScore` | float | 是 | 0~1，EDL 输出 |
| `lesionAreaPx` | int | 否 | 病灶像素数 |
| `lesionAreaRatio` | float | 否 | 病灶占牙面比例 |
| `maskAsset` | object | 是 | 二值 mask PNG |
| `overlayAsset` | object | 否 | 叠加可视化 PNG |

**Asset 对象字段**：

| 字段 | 类型 | 必填 |
|---|---|---|
| `bucketName` | string | 是 |
| `objectKey` | string | 是 |
| `widthPx` | int | 否 |
| `heightPx` | int | 否 |

### 10.4 Java → Python 回调响应

Python 收到 Java 的响应后：
- `code=00000`：回调成功，标记任务为 `CALLBACK_ACKED`
- 其他 code：重试（最多 3 次，指数退避 2s / 8s / 30s），全部失败后把任务放入死信队列

```json
{
  "code": "00000",
  "message": "callback accepted",
  "data": null,
  "traceId": "trace-002",
  "timestamp": "2026-04-13T10:00:13+08:00"
}
```

---

## 11. 回调接口 2：分析失败回调

同一端点 `/api/v1/ai/callbacks/analysis`，`taskStatusCode` 为 `FAILED` 或 `QUALITY_REJECT`。

### 11.1 失败回调体

```json
{
  "traceId": "trace-002",
  "taskNo": "TASK202604130002",
  "taskStatusCode": "FAILED",
  "caseId": 10001,
  "patientId": 70001,
  "orgId": 1001,
  "modelVersion": "cg_release_2026_04_v1",
  "startedAt": "2026-04-13T10:00:01+08:00",
  "completedAt": "2026-04-13T10:00:06+08:00",
  "errorCode": "C3002",
  "errorMessage": "segmentation inference timeout after 30s",
  "errorStage": "SEGMENTATION",
  "retryable": true,
  "rawResultJson": {
    "failedAt": "segmentation",
    "exception": "torch.cuda.OutOfMemoryError"
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `errorCode` | string | 是 | §5.1 |
| `errorMessage` | string | 是 | 长度不超过 1000 |
| `errorStage` | string | 是 | `PREPROCESS` / `QUALITY_CHECK` / `TOOTH_DETECT` / `SEGMENTATION` / `GRADING` / `RISK_FUSION` / `EXPLAIN` / `CALLBACK` |
| `retryable` | boolean | 是 | 告诉 Java 该任务是否值得重新提交 |

### 11.2 质量拒收回调体

```json
{
  "traceId": "trace-002",
  "taskNo": "TASK202604130002",
  "taskStatusCode": "QUALITY_REJECT",
  "caseId": 10001,
  "patientId": 70001,
  "orgId": 1001,
  "startedAt": "2026-04-13T10:00:01+08:00",
  "completedAt": "2026-04-13T10:00:03+08:00",
  "qualityCheckResults": [
    {
      "imageId": 90001,
      "checkResultCode": "REJECT",
      "qualityScore": 42,
      "issueCodes": ["BLUR", "UNDEREXPOSURE"],
      "suggestionText": "图像模糊且曝光不足，建议重新拍摄"
    }
  ]
}
```

Java 收到后将病例状态改为 `QC_FAILED`，并写入 `med_image_quality_check`。

---

## 12. RabbitMQ 消息契约

### 12.1 Exchange 与 Queue 冻结

| Exchange 名 | 类型 | 说明 |
|---|---|---|
| `cg.analysis.exchange` | topic | 分析相关事件 |
| `cg.analysis.dlx` | direct | 死信交换机 |

| Queue 名 | 消费者 | Routing Key |
|---|---|---|
| `cg.analysis.request.queue` | Python worker | `analysis.requested` |
| `cg.analysis.completed.queue` | Java analysis module | `analysis.completed` |
| `cg.analysis.failed.queue` | Java analysis module | `analysis.failed` |

**注意**：MQ 路径与 HTTP 回调路径并存——`analysis.requested` 由 Java 发 Python 消费；分析完成时 Python **优先** 走 HTTP 回调，MQ 作为 HTTP 失败时的兜底通道。

### 12.2 `analysis.requested` 消息体

```json
{
  "traceId": "trace-002",
  "taskNo": "TASK202604130002",
  "caseId": 10001,
  "patientId": 70001,
  "orgId": 1001,
  "payloadType": "FULL",
  "payload": { 
    "_ref": "与 /ai/v1/analyze 请求体完全相同"
  },
  "publishedAt": "2026-04-13T10:00:00+08:00"
}
```

`payloadType` 字段：
- `FULL`：消息体直接包含完整分析请求（适合开发期和小规模演示）
- `REF`：消息体只带 `taskNo`，Python 通过 `GET /api/v1/ai/tasks/{taskNo}` 反查（适合后期生产）

**一期统一使用 `FULL` 模式**，REF 模式留作未来升级点。

### 12.3 消息属性要求

| Property | 值 | 说明 |
|---|---|---|
| `content_type` | `application/json` | |
| `content_encoding` | `utf-8` | |
| `delivery_mode` | `2` | 持久化 |
| `correlation_id` | `{taskNo}` | 用于幂等 |
| `message_id` | `{traceId}:{taskNo}` | 唯一标识 |
| `expiration` | `7200000` | 2 小时 TTL |

---

## 13. 幂等性与重试策略

### 13.1 Python worker 幂等要求

Python 端 **必须** 实现以下幂等机制：

1. **任务级幂等**：同一个 `taskNo` 被重复投递时，只执行一次推理
   - 使用 Redis `SETNX cg:task:{taskNo}` 作为分布式锁
   - 锁 TTL 与任务超时对齐（默认 600s）
2. **回调级幂等**：单次任务的回调必须能被重复发送不影响 Java 落库
   - Java 侧已实现 `ana_task_record.task_no` 唯一索引 + 状态校验
   - Python 侧只负责"确保回调能最终送达"，不负责"确保只送一次"

### 13.2 Python 回调重试策略

| 次数 | 等待 | 终止条件 |
|---|---|---|
| 1 | 立即 | HTTP 2xx 且 `code=00000` |
| 2 | 2s | 同上 |
| 3 | 8s | 同上 |
| 4 | 30s | 同上 |
| 失败 | - | 将失败事件发布到 `cg.analysis.dlx`，由 Java 侧定时扫描 |

### 13.3 Java 端超时扫描

Java `caries-analysis` 模块每 60s 扫描一次：

```sql
SELECT task_no FROM ana_task_record 
WHERE task_status_code IN ('QUEUEING', 'PROCESSING')
  AND requested_at < NOW() - INTERVAL 10 MINUTE
```

命中的任务标记为 `TIMEOUT`，并向 Python 发送取消信号。

---

## 14. 字段级反向对齐清单

**本节供联调前由双方开发负责人逐行勾选。**

### 14.1 Python → Java 必须返回的字段（最小集）

用于确保 Java 的 `ana_result_summary` 和 `ana_visual_asset` 能成功落库：

- [ ] `traceId`
- [ ] `taskNo`
- [ ] `taskStatusCode`
- [ ] `caseId`
- [ ] `patientId`
- [ ] `orgId`
- [ ] `modelVersion`
- [ ] `startedAt`
- [ ] `completedAt`
- [ ] `summary.overallHighestSeverity`
- [ ] `summary.suspiciousToothCount`
- [ ] `summary.overallUncertaintyScore`
- [ ] `summary.reviewRecommendedFlag`
- [ ] `summary.highRiskFlag`
- [ ] `lesionResults[].imageId`
- [ ] `lesionResults[].severityCode`
- [ ] `lesionResults[].uncertaintyScore`
- [ ] `lesionResults[].maskAsset.bucketName`
- [ ] `lesionResults[].maskAsset.objectKey`

### 14.2 Java 必须提前准备好的前置条件

- [ ] `ana_task_record` 表结构就绪，含以上全部字段
- [ ] `ana_result_summary` 表结构就绪
- [ ] `ana_visual_asset` 表结构就绪
- [ ] `/api/v1/ai/callbacks/analysis` 端点已实现
- [ ] `X-Callback-Token` 校验逻辑已实现
- [ ] MinIO `caries-visual` bucket 已创建，Python 用户有读写权限
- [ ] RabbitMQ `cg.analysis.exchange` 和 3 个 queue 已声明

### 14.3 Python 必须提前准备好的前置条件

- [ ] FastAPI 服务在 `:8001` 启动，路径前缀 `/ai/v1`
- [ ] 4 个业务接口全部可用（quality-check / analyze / assess-risk / model-version）
- [ ] `CallbackService` 已实现，支持 HMAC token + 指数退避重试
- [ ] MinIO client 可读 `caries-image`、可写 `caries-visual`
- [ ] RabbitMQ consumer 订阅 `cg.analysis.request.queue`
- [ ] 所有响应包装在 `ApiResponse` 结构中

---

## 15. 联调 Checklist

按以下顺序联调，每一步完成后再进入下一步：

### 15.1 L0：健康检查互通

- [ ] Java 调用 `GET http://python:8001/ai/v1/health` 返回 200
- [ ] Python 调用 `GET http://java:8080/api/v1/actuator/health` 返回 200

### 15.2 L1：单接口 happy path

- [ ] 构造一张全景片上传到 `caries-image`
- [ ] Java 调用 `POST /ai/v1/quality-check`，Python 返回 `PASS`
- [ ] Java 调用 `POST /ai/v1/analyze`，Python 返回 `QUEUEING`
- [ ] Python mock pipeline 2 秒后向 Java 回调成功响应体
- [ ] Java 在 `ana_result_summary` 里查到对应记录

### 15.3 L2：异常路径

- [ ] Java 传入错误 objectKey，Python 返回 `A0404`
- [ ] Java 传入 token 不对的回调，Python 收到 `A0401`
- [ ] Python 主动返回 `B0003` 质量拒收，Java 正确推进状态到 `QC_FAILED`
- [ ] Python 主动返回 `C3002` 推理超时，Java 正确落 `FAILED`

### 15.4 L3：压测

- [ ] 并发提交 20 个分析任务，Python 无内存泄漏
- [ ] 所有 20 个任务都有明确最终状态（SUCCESS 或 FAILED，不允许 PROCESSING 悬挂）
- [ ] Java `ana_task_record` 记录数 = 20

### 15.5 L4：端到端

- [ ] 前端创建病例 → 上传影像 → 提交分析 → 展示结果 → 生成报告 全流程跑通
- [ ] 报告 PDF 中出现 Python 生成的 overlay 图

---

## 16. 契约变更流程

**本文档一旦 v1.0 签发，禁止任何一方单边修改。**

变更流程：

1. 提起方在项目 issue 中新建 `[CONTRACT-CHANGE]` 标题的 issue
2. 填写变更字段、原因、对两侧的影响
3. Java 负责人和 Python 负责人必须同时 approve
4. Approve 后更新本文档并升版号（`v1.0` → `v1.1`）
5. 两侧代码同步更新并跑完 L0~L4 联调

**禁止以下做法**：
- 在代码里加了一个新字段但不更新本文档
- 为了"避免影响联调"偷偷让 Python 返回两套字段
- 为了"兼容老接口"保留含糊不清的废弃字段

---

## 附录 A：最小字段集 vs 完整字段集

- **最小字段集**：对应 §14.1 勾选项，是 L1 联调的验收标准
- **完整字段集**：对应 §10.2 完整 JSON，是答辩演示的目标

联调阶段建议先用最小字段集跑通，再逐步补齐完整字段集，避免一次性背太大的字段对齐任务。

---

## 附录 B：与 Java 侧数据字典的映射状态

> **重要**：截至本文档签发时，Java 端 04 号开发说明书和 1 号架构文档均提到 `ana_task_record` / `ana_result_summary` / `ana_visual_asset` / `ana_correction_feedback` 四张表，但 **07 号数据字典中暂未给出这四张表的字段定义**。
> 
> 本对照表的字段命名以 Java 端已实际开发的 DO/DTO 代码为"事实标准"。Java 团队在下周必须完成这四张表的字段定义回填到 07 号数据字典中，确保三处口径一致：
> - Java 端 DO/DTO 代码
> - 07 号数据字典
> - 本接口对照表
> 
> 如果发现 Java 端代码里实际字段名与本文档不一致，**以 Java 端代码为准，同步修改本文档**，然后通知 Python 端跟进。

---

**签发**

| 角色 | 姓名 | 签字 | 日期 |
|---|---|---|---|
| Java 负责人 | | | |
| Python 负责人 | | | |
| 项目总负责人 | | | |
