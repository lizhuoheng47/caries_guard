# CariesGuard —— Java 端 MinIO 改造实施文档（可直接执行版）

> 文档定位：**Java 后端将对象存储从本地文件系统/伪对象存储口径，正式切换为 MinIO 的唯一实施文档**  
> 适用对象：Java 后端工程师、项目负责人、联调工程师、测试工程师  
> 适用范围：`backend-java/` 下的 `caries-image`、`caries-analysis`、`caries-report`、`caries-integration` 等模块  
> 目标：让 Java 后端真正以 **MinIO 作为唯一对象存储实现**，并与 Python AI 服务完成稳定联调

---

# 目录

1. 为什么必须切到 MinIO
2. 本次改造的最终目标
3. 改造后的统一口径
4. 需要修改的模块与边界
5. 配置方案
6. 数据表与元数据口径
7. 存储抽象设计
8. Java 端核心实现方案
9. 预签名访问方案
10. 与 Python 端联调协议
11. 迁移步骤
12. 测试与验收清单
13. 风险点与处理策略
14. 任务拆解建议
15. 代码骨架示例

---

# 1. 为什么必须切到 MinIO

你当前项目的顶层设计、数据库建模、接口契约、对象命名规范，本质上都已经默认“对象存储”存在：

- 原始影像需要统一存储并通过 `bucketName + objectKey` 被 AI 服务读取；
- Python 侧可视化产物需要回传后供 Java 业务平台读取；
- 报告 PDF、导出文件需要统一归档到附件体系；
- 前端不能直接暴露真实物理路径，必须通过 Java 控制访问。

如果继续沿用本地文件系统方案，会有 5 个问题：

1. **文档口径和真实实现继续冲突**；
2. **Python 端被迫依赖共享目录或宿主机路径**，部署不稳定；
3. **报告、影像、可视化产物无法统一走附件体系**；
4. **前端访问无法正规化走私有对象 + 预签名访问**；
5. **后续 Docker Compose / 演示部署会非常脆弱**。

所以这次改造的本质不是“存储优化”，而是：

> **把对象存储从“临时实现”升级为“正式基础设施”。**

---

# 2. 本次改造的最终目标

本次改造完成后，Java 端必须达到以下状态：

## 2.1 单一事实源

Java 业务系统中所有影像、可视化产物、报告 PDF、导出文件都只认：

- `storageProviderCode = MINIO`
- `bucketName`
- `objectKey`
- `endpoint`
- `attachmentId`

禁止再把“本地绝对路径”作为业务侧长期口径。

## 2.2 前后端与 Python 统一

- 前端上传文件给 Java；
- Java 上传 MinIO 并写附件元数据；
- Java 分析任务只把 `bucketName + objectKey` 发给 Python；
- Python 从 MinIO 下载原图、上传 visual 产物到 MinIO；
- Java 通过附件/对象元数据统一查询和展示。

## 2.3 报告与导出统一归档

- 医生版/患者版报告 PDF 存到 `caries-report`；
- 导出临时文件存到 `caries-export`；
- 下载一律通过 Java 校验权限后签发预签名链接或中转下载。

---

# 3. 改造后的统一口径

## 3.1 Bucket 冻结

统一使用以下 4 个 bucket：

| Bucket 名 | 用途 | 写入方 | 读取方 |
|---|---|---|---|
| `caries-image` | 原始影像（全景片/根尖片/口内照） | Java | Java / Python |
| `caries-visual` | mask、overlay、heatmap 等可视化产物 | Python | Java |
| `caries-report` | 报告 PDF | Java | Java |
| `caries-export` | 临时导出文件 | Java | Java |

## 3.2 Object Key 冻结

统一规则：

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

## 3.3 访问控制冻结

- bucket 默认私有；
- 前端不得直接保存 MinIO AK/SK；
- Java 统一签发短时预签名 URL 或执行鉴权后文件流中转；
- Python 拿内部 MinIO 凭证，不走预签名下载原图。

---

# 4. 需要修改的模块与边界

## 4.1 `caries-integration`

新增或改造：

- `ObjectStorageClient` 接口
- `MinioObjectStorageClient` 实现
- `StorageProperties` 配置类
- `ObjectStorageAutoConfig` 自动装配

该模块只做基础设施适配，不写业务规则。

## 4.2 `caries-image`

需要承担：

- 影像上传
- 附件元数据入库
- `med_image_file` 与 `med_attachment` 绑定
- 影像预览/下载预签名能力

## 4.3 `caries-analysis`

需要承担：

- 构造分析请求，把 `bucketName + objectKey` 发给 Python
- 接收 Python 回调的 visual asset 元数据
- 把 visual 产物落入附件体系或结果表引用

## 4.4 `caries-report`

需要承担：

- 报告 PDF 生成后上传 `caries-report`
- `rpt_record` 绑定附件引用
- 导出审计与下载控制

## 4.5 `caries-boot`

需要承担：

- 读取 MinIO 连接配置
- dev / test / prod profile 切换
- 启动健康检查

---

# 5. 配置方案

## 5.1 application.yml 建议

```yaml
caries:
  storage:
    provider: MINIO
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
    secure: false
    region: us-east-1
    buckets:
      image: caries-image
      visual: caries-visual
      report: caries-report
      export: caries-export
    presign:
      expiry-seconds: 900
      upload-expiry-seconds: 600
```

## 5.2 配置类建议

```java
@ConfigurationProperties(prefix = "caries.storage")
public class StorageProperties {
    private String provider;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Boolean secure;
    private String region;
    private BucketProperties buckets;
    private PresignProperties presign;
}
```

## 5.3 profile 策略

### dev
- 使用本地 Docker MinIO
- AK/SK 可写在 `.env` 或本地 secret 文件

### test
- 集成测试可用独立 MinIO 容器
- 不要连开发环境 MinIO

### prod/demo
- endpoint 改为容器服务名或正式地址
- AK/SK 走环境变量注入

---

# 6. 数据表与元数据口径

## 6.1 `med_attachment` 最低要求

建议至少具备以下字段语义：

| 字段 | 说明 |
|---|---|
| `id` | 附件主键 |
| `org_id` | 机构隔离 |
| `biz_module_code` | 业务模块，如 CASE_IMAGE / REPORT / VISUAL |
| `bucket_name` | bucket 名 |
| `object_key` | MinIO object key |
| `storage_provider_code` | 固定为 MINIO |
| `original_filename` | 原文件名 |
| `content_type` | MIME 类型 |
| `file_size_bytes` | 文件大小 |
| `etag` | 对象 etag |
| `storage_status_code` | ACTIVE / DELETED |
| `created_at` | 创建时间 |

如果你当前已有 `med_attachment`，则按现有字段映射；核心是必须能承载 bucket/objectKey/provider 这三个维度。

## 6.2 `med_image_file`

建议至少关联：

- `attachment_id`
- `case_id`
- `image_type_code`
- `quality_status_code`
- `main_image_flag`

## 6.3 visual 产物承载

可选两种方案：

### 方案 A：落附件表 + 结果表引用附件 ID
推荐。

优点：
- 统一管理所有对象；
- 便于预签名与权限；
- 后续报告嵌图更顺。

### 方案 B：结果表直接存 bucketName/objectKey
只适合一期临时简化，不建议长期使用。

---

# 7. 存储抽象设计

## 7.1 不要让业务模块直接依赖 MinIO SDK

业务模块只能依赖统一接口：

```java
public interface ObjectStorageClient {
    UploadObjectResult upload(UploadObjectCommand command);
    void remove(String bucketName, String objectKey);
    InputStream download(String bucketName, String objectKey);
    String presignGetUrl(String bucketName, String objectKey, Duration expiry);
    String presignPutUrl(String bucketName, String objectKey, Duration expiry);
    StatObjectResult stat(String bucketName, String objectKey);
    boolean exists(String bucketName, String objectKey);
}
```

这样做的目的：

- 隔离 SDK 细节；
- 后续如果要接 OSS/S3，不用重写业务代码；
- 单元测试可 Mock。

## 7.2 上传命令对象建议

```java
public class UploadObjectCommand {
    private String bucketName;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private long size;
    private InputStream inputStream;
    private Map<String, String> userMetadata;
}
```

## 7.3 上传结果对象建议

```java
public class UploadObjectResult {
    private String bucketName;
    private String objectKey;
    private String etag;
    private String versionId;
    private long size;
    private String contentType;
}
```

---

# 8. Java 端核心实现方案

## 8.1 上传影像主流程

```text
前端上传文件
→ ImageController 接收 MultipartFile
→ ImageAppService 校验病例权限、文件类型、大小
→ 生成 objectKey
→ 调用 ObjectStorageClient.upload()
→ 写 med_attachment
→ 写 med_image_file
→ 返回 imageId / attachmentId / objectKey
```

## 8.2 生成 objectKey 规则

建议统一由独立组件处理：

```java
public interface ObjectKeyGenerator {
    String generateCaseImageKey(String caseNo, String filename, LocalDateTime now);
    String generateReportKey(String reportNo, String filename, LocalDateTime now);
    String generateExportKey(String exportNo, String filename, LocalDateTime now);
}
```

禁止在 Controller 或 Service 里手拼字符串。

## 8.3 构造分析请求

在 `caries-analysis` 中，不再传本地路径：

```json
{
  "imageId": 90001,
  "bucketName": "caries-image",
  "objectKey": "org/1001/case/CASE202604150001/image/PANORAMIC/2026/04/15/90001/original_01.jpg",
  "imageTypeCode": "PANORAMIC"
}
```

Python 拿到后直接走 MinIO 下载。

## 8.4 处理 Python 回调

Python 成功回调时，建议至少回传：

```json
{
  "visualAssets": [
    {
      "assetTypeCode": "MASK",
      "bucketName": "caries-visual",
      "objectKey": "org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/MASK/90001/16/91001.png",
      "contentType": "image/png",
      "relatedImageId": 90001,
      "toothCode": "16"
    }
  ]
}
```

Java 侧接收后：
- 校验 bucket 与 objectKey；
- 写附件元数据；
- 结果表引用附件 ID；
- 不需要重新下载再上传一次。

---

# 9. 预签名访问方案

## 9.1 适用场景

### 需要预签名
- 前端查看原图
- 前端查看 overlay / mask / heatmap
- 下载报告 PDF
- 下载导出文件

### 不需要预签名
- Java 内部上传对象
- Python 内部读取原图
- Python 内部上传 visual 产物

## 9.2 Controller 建议

### 获取附件下载链接

```http
GET /api/v1/attachments/{attachmentId}/presign
```

返回：

```json
{
  "attachmentId": 5001,
  "url": "http://minio:9000/...",
  "expiresInSeconds": 900
}
```

## 9.3 安全要求

- 先校验用户是否有该病例/报告访问权限；
- 预签名有效期控制在 5~15 分钟；
- 不把真实 AK/SK 暴露给前端；
- 不长期缓存预签名 URL 到数据库。

---

# 10. 与 Python 端联调协议

## 10.1 Java 发给 Python

Java 必须传：

- `bucketName`
- `objectKey`
- `imageTypeCode`
- `originalFilename`
- `widthPx/heightPx`（有则传）
- `caseNo`
- `callbackUrl`
- `callbackToken`

## 10.2 Python 发回 Java

Python 必须回：

- 分析结果摘要
- 风险结果
- `visualAssets[]`
- 每个资产的 `bucketName + objectKey + contentType + relatedImageId`

## 10.3 边界冻结

### Java 负责
- 上传原始图
- 管理附件元数据
- 权限、审计、报告、病例状态

### Python 负责
- 下载原图
- 推理
- 上传 visual 对象
- 回调对象元信息

### 双方都不做
- 不共享宿主机文件夹作为长期方案
- 不在消息里传 Linux 本地路径
- 不把 object url 当长期持久化字段

---

# 11. 迁移步骤

建议分 4 步执行，不要一次改完所有模块。

## Step 1：先搭 MinIO 基座

完成：
- Docker Compose 启动 MinIO
- 创建 4 个 bucket
- Java 能成功连接并完成最小上传/下载

## Step 2：先切 `caries-image`

完成：
- 上传影像走 MinIO
- `med_attachment` 真写 bucket/objectKey
- 查询影像可签发预签名链接

这是整个改造的第一优先级。

## Step 3：再切 `caries-analysis`

完成：
- 分析请求改成 bucket/objectKey
- Python 回调 visual 资产元数据
- Java 结果表能引用 visual 附件

## Step 4：最后切 `caries-report` 与导出

完成：
- PDF 上传 `caries-report`
- 导出文件上传 `caries-export`
- `rpt_export_log` 走真实文件归档与审计

---

# 12. 测试与验收清单

## 12.1 基础连接测试

- [ ] Java 能连接 MinIO
- [ ] 自动建 bucket 或启动时校验 bucket 存在
- [ ] 上传一个 jpg 成功
- [ ] 下载对象成功
- [ ] stat 成功
- [ ] presign 成功

## 12.2 影像模块验收

- [ ] 上传病例影像后，`med_attachment` 有 bucket/objectKey/provider
- [ ] `med_image_file` 正确关联 attachment
- [ ] 前端能查看原图
- [ ] 删除/禁用后访问受控

## 12.3 分析模块验收

- [ ] Java 发给 Python 的 payload 不再包含本地绝对路径
- [ ] Python 可正常从 MinIO 读取原图
- [ ] Python 上传 visual 到 `caries-visual`
- [ ] Java 可展示 visual 结果

## 12.4 报告模块验收

- [ ] 医生版报告 PDF 归档成功
- [ ] 患者版报告 PDF 归档成功
- [ ] `rpt_record` 正确引用附件
- [ ] 导出日志产生后能受控下载

## 12.5 安全验收

- [ ] 前端不持有 AK/SK
- [ ] 预签名 URL 过期后失效
- [ ] 无权限用户无法获取预签名链接

---

# 13. 风险点与处理策略

## 风险 1：旧代码仍在读本地路径

### 现象
部分服务仍写 `File file = new File(localPath)`。

### 处理
统一排查关键词：
- `LocalObjectStorageService`
- `localPath`
- `File.separator`
- `new File(`
- `Paths.get(uploadDir`)

把业务侧对物理路径的依赖收口到基础设施层。

## 风险 2：对象元数据未入库完整

### 现象
虽然文件已经上传 MinIO，但数据库只有 URL 或只有 filename。

### 处理
`med_attachment` 至少保留：
- provider
- bucket
- objectKey
- contentType
- size
- etag

## 风险 3：Python 仍假设共享目录

### 现象
Python 代码仍然读取 `/shared/images/...`。

### 处理
联调前明确冻结：**Python 只认 MinIO，不认共享宿主机目录。**

## 风险 4：前端直接拼 MinIO 公网地址

### 现象
前端把 endpoint + bucket + objectKey 直接拼成图片 URL。

### 处理
禁止。必须走 Java 的预签名接口或受控下载接口。

---

# 14. 任务拆解建议

## P0：必须先做

1. `caries-integration`：MinIO 客户端与配置
2. `caries-image`：上传影像走 MinIO
3. `med_attachment`：补全对象元数据
4. 预签名下载接口

## P1：紧接着做

5. `caries-analysis`：分析请求改为 bucket/objectKey
6. Python 回调 visual 资产接入
7. 结果页图片预览

## P2：随后做

8. `caries-report`：PDF 归档到 `caries-report`
9. `caries-export`：导出文件归档
10. 清理历史本地文件逻辑

---

# 15. 代码骨架示例

## 15.1 Maven 依赖

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.6.0</version>
</dependency>
```

## 15.2 MinIO Client 配置

```java
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class ObjectStorageAutoConfig {

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    public ObjectStorageClient objectStorageClient(MinioClient minioClient,
                                                   StorageProperties properties) {
        return new MinioObjectStorageClient(minioClient, properties);
    }
}
```

## 15.3 MinIO 实现示例

```java
@RequiredArgsConstructor
public class MinioObjectStorageClient implements ObjectStorageClient {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    @Override
    public UploadObjectResult upload(UploadObjectCommand command) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(command.getBucketName())
                            .object(command.getObjectKey())
                            .stream(command.getInputStream(), command.getSize(), -1)
                            .contentType(command.getContentType())
                            .userMetadata(command.getUserMetadata())
                            .build()
            );

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(command.getBucketName())
                            .object(command.getObjectKey())
                            .build()
            );

            UploadObjectResult result = new UploadObjectResult();
            result.setBucketName(command.getBucketName());
            result.setObjectKey(command.getObjectKey());
            result.setEtag(stat.etag());
            result.setSize(stat.size());
            result.setContentType(stat.contentType());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("upload to minio failed", e);
        }
    }

    @Override
    public String presignGetUrl(String bucketName, String objectKey, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry((int) expiry.getSeconds())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("presign get url failed", e);
        }
    }
}
```

## 15.4 影像上传应用服务示例

```java
@Service
@RequiredArgsConstructor
public class ImageAppService {

    private final ObjectStorageClient objectStorageClient;
    private final AttachmentRepository attachmentRepository;
    private final ImageFileRepository imageFileRepository;
    private final ObjectKeyGenerator objectKeyGenerator;

    @Transactional(rollbackFor = Exception.class)
    public UploadImageResult uploadCaseImage(Long caseId, String caseNo, MultipartFile file) {
        String objectKey = objectKeyGenerator.generateCaseImageKey(
                caseNo,
                file.getOriginalFilename(),
                LocalDateTime.now()
        );

        UploadObjectCommand command = new UploadObjectCommand();
        command.setBucketName("caries-image");
        command.setObjectKey(objectKey);
        command.setOriginalFilename(file.getOriginalFilename());
        command.setContentType(file.getContentType());
        command.setSize(file.getSize());
        try {
            command.setInputStream(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        UploadObjectResult uploadResult = objectStorageClient.upload(command);

        Long attachmentId = attachmentRepository.saveAttachment(uploadResult, file);
        Long imageId = imageFileRepository.saveCaseImage(caseId, attachmentId);

        return new UploadImageResult(imageId, attachmentId, objectKey);
    }
}
```

---

# 最终结论

本次改造的核心不是“把文件从 A 目录挪到 B 目录”，而是：

> **让 Java 后端真正成为 MinIO 对象存储的业务控制面。**

只要你按本文档执行，最终会得到以下稳定结构：

- Java：负责上传、元数据、权限、预签名、审计；
- Python：负责下载原图、推理、上传可视化；
- 前端：只通过 Java 受控访问对象；
- MinIO：成为原始影像、可视化产物、报告、导出文件的统一底座。

这才是和你当前项目架构、数据库、接口契约真正一致的方案。
