# Image 模块开发检查与完成情况

## 1. 当前结论

`image` 模块已经完成当前 V1 所需的上传、附件访问、病例影像关联和质检能力，可稳定支撑 analysis/report 主链路。

## 2. 已实现接口

- `POST /api/v1/files/upload`
- `GET /api/v1/files/{attachmentId}/access-url`
- `GET /api/v1/files/{attachmentId}/content`
- `POST /api/v1/cases/{caseId}/images`
- `GET /api/v1/cases/{caseId}/images`
- `GET /api/v1/cases/{caseId}/images/detail/{imageId}`
- `POST /api/v1/images/{imageId}/quality-checks`
- `GET /api/v1/images/{imageId}/quality-checks/current`

## 3. 真实实现细节

### 3.1 附件上传

- 通过 `AttachmentAppService` 处理
- 上传前读取文件字节并计算 MD5
- 同机构下若已存在相同 MD5，会复用已有附件记录
- 当前 `med_attachment.biz_module_code` 初始写为 `CASE`

### 3.2 对象存储

- 接口抽象：`ObjectStorageService`
- 当前实现：`LocalObjectStorageService`
- 落盘目录：`${user.dir}/var/image-storage/<bucket>/attachments/yyyy/MM/dd/...`

### 3.3 附件访问

- `access-url` 接口需要登录权限
- `content` 接口不要求 JWT，但必须带签名参数
- 签名内容包含：`attachmentId:bucketName:objectKey:expireAt`

### 3.4 病例影像关联

- 关联时会校验 `caseId / visitId / patientId / attachmentId` 一致性
- 同一 `caseId + imageTypeCode` 下若新增主图，会先清理旧主图标记
- 首张影像落库且病例还在 `CREATED` 时，会自动把病例推进到 `QC_PENDING`

### 3.5 影像质检

- 质检记录写入 `med_image_quality_check`
- 当前接口支持保存最新质检并查询“当前质检”
- analysis 创建任务时只会选择 `quality_status_code = PASS` 的影像

## 4. 当前数据库表

- `med_attachment`
- `med_image_file`
- `med_image_quality_check`

## 5. 当前限制

- 对象存储配置项默认写的是 `MINIO`，但真实实现仍是本地文件系统
- 没有真正的云存储预签名 URL，当前是应用层自定义签名访问
- 影像质检没有单独的批处理流程或自动任务，完全通过 API 写入

## 6. 测试覆盖情况

`image` 模块能力主要在 boot 真库主链路中被覆盖：

- 文件上传
- 影像关联
- 质检写入
- 签名访问链路的间接验证
