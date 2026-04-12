# Image 模块开发检查与完成情况

## 文档定位

本文件用于回填 `image` 模块开发完成后的实现状态、与检查清单的对齐结果，以及仍需在后续模块联调中继续观察的风险点。

当前结论：`image` 模块已达到 `P3` 交付基线，可继续支撑 `analysis` 模块开发。

## 已完成范围

- 附件上传：`POST /api/v1/files/upload`
- 签名访问地址：`GET /api/v1/files/{attachmentId}/access-url`
- 签名内容读取：`GET /api/v1/files/{attachmentId}/content`
- 病例影像关联：`POST /api/v1/cases/{caseId}/images`
- 病例影像列表：`GET /api/v1/cases/{caseId}/images`
- 单张影像详情：`GET /api/v1/cases/{caseId}/images/detail/{imageId}`
- 影像质检写入：`POST /api/v1/images/{imageId}/quality-checks`
- 当前质检查询：`GET /api/v1/images/{imageId}/quality-checks/current`

## 与检查清单对齐结果

### 1. attachment / image 分层

已对齐。

- 附件元数据落在 `med_attachment`
- 影像业务属性落在 `med_image_file`
- 影像上传先得到 `attachmentId`，再进行病例影像关联

### 2. 上传编排与补偿

已修正。

- `AttachmentAppService` 负责上传编排
- 上传成功但附件元数据写库失败时，会回滚已写入的对象存储文件，避免留下半成品对象

### 3. MD5 去重

已对齐。

- 以 `org_id + md5` 做复用判断
- 相同机构内重复文件不会重复写对象存储和附件元数据

### 4. 主图规则

已对齐。

- 应用层在同一 `case_id + image_type_code` 下先清旧主图，再写新主图
- 数据库侧已通过后续 Flyway 做约束强化

### 5. 质检记录与影像状态

已对齐。

- `med_image_quality_check` 支持多次写入
- 新写入记录会切换 `current_flag`
- `med_image_file.quality_status_code` 与当前有效质检结果同步

### 6. 病例状态联动

已修正。

- 首张有效影像关联后，病例状态会从 `CREATED` 进入 `QC_PENDING`
- 满足 `analysis` 模块的前置条件

### 7. org 隔离

已对齐。

- 上传、影像关联、列表查询、详情查询、质检写入与读取都校验 `org_id`
- 管理员角色保留跨机构访问能力

### 8. 下游可用性

已修正。

- 影像列表返回当前质检摘要
- 单张影像详情接口可直接提供给 `analysis`、`report` 或管理端使用

### 9. 对象存储抽象

已对齐。

- 通过 `ObjectStorageService` 抽象存储能力
- 当前本地实现为 `LocalObjectStorageService`
- 后续可平滑替换为 MinIO 或其他对象存储实现

### 10. 排序与查询稳定性

已对齐。

- 列表排序规则为：主图优先、序号升序、ID 升序
- 当前质检记录按 `checked_at` 最新有效记录读取

## 当前实现文件

- `caries-image/src/main/java/com/cariesguard/image/app/AttachmentAppService.java`
- `caries-image/src/main/java/com/cariesguard/image/app/CaseImageAppService.java`
- `caries-image/src/main/java/com/cariesguard/image/controller/FileController.java`
- `caries-image/src/main/java/com/cariesguard/image/controller/CaseImageController.java`
- `caries-image/src/main/java/com/cariesguard/image/controller/ImageQualityCheckController.java`
- `caries-image/src/main/java/com/cariesguard/image/infrastructure/repository/ImageCommandRepositoryImpl.java`
- `caries-image/src/main/java/com/cariesguard/image/infrastructure/repository/ImageQueryRepositoryImpl.java`
- `caries-image/src/main/java/com/cariesguard/image/infrastructure/storage/LocalObjectStorageService.java`

## 已完成验证

- 单元测试覆盖：
  - 文件上传复用
  - 上传失败补偿删除
  - 主图切换
  - 首图驱动病例进入 `QC_PENDING`
  - 质检写入与读取
  - 影像列表返回质检摘要
  - 单张影像详情查询
- 模块构建验证：
  - `mvn test`
  - `mvn -DskipTests package`

## 仍需在后续联调继续观察的点

- 当前对象存储为本地实现，生产态替换 MinIO 后需要再做一次完整回归
- `analysis` 回调写回后，影像与病例聚合展示还需要继续补充结果摘要联动
- 端到端联调测试仍需在 `P8` 统一收口
