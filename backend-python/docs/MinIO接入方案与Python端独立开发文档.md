# CariesGuard —— MinIO 接入详细解决方案与 Python 端独立开发文档

> 文档定位：本文件用于将 CariesGuard 当前对象存储从“本地文件系统 / 伪对象存储”切换为 **标准 MinIO（S3 兼容）**，并给出一份可直接交给 Python 端执行的、**不依赖其他项目文档** 的实施说明。
> 
> 适用对象：项目负责人、Java 后端工程师、Python 工程师、联调工程师、部署工程师。
> 
> 使用原则：本文件优先解决“现在就能落地”的方案，不讨论过度工程化集群方案。

---

# 1. 为什么现在要切到 MinIO

当前项目的顶层设计、数据库字典、接口契约、报告归档思路，本质上都更适合建立在“标准对象存储”之上，而不是继续使用本地共享目录或仅在数据库里保存伪路径。

切到 MinIO 后，可以一次性解决以下问题：

1. **Java / Python 存储边界清晰**：Java 负责元数据与业务编排，Python 负责读原图、写可视化产物，二者通过 bucket + objectKey 协同，不再依赖共享磁盘。
2. **接口契约稳定**：分析请求、回调结果、报告导出都可以围绕 bucketName / objectKey / presigned URL 展开。
3. **附件体系真实落地**：`med_attachment` 中的 `bucket_name`、`object_key`、`storage_provider_code` 终于与真实基础设施一致。
4. **答辩可信度提升**：可以明确回答“我们使用标准 S3 兼容对象存储，支持权限控制、预签名访问、附件归档、可视化资产沉淀”。
5. **后续扩展更容易**：未来从 MinIO 切到 OSS / S3 时，只需要替换对象存储适配层，而不是重做业务逻辑。

---

# 2. 本次切换的总目标

本次不是把 MinIO 当“一个可选组件”挂上去，而是把它作为 **唯一标准对象存储实现**。

## 2.1 切换完成后的目标状态

- 原始病例影像上传后，落到 MinIO 的 `caries-image` bucket。
- Python 端分析时，不再读本地共享目录，而是直接从 MinIO 下载原图。
- Python 端生成的 mask / overlay / heatmap 上传到 `caries-visual` bucket。
- Java 端生成的 PDF 报告归档到 `caries-report` bucket。
- 临时导出文件归档到 `caries-export` bucket。
- 数据库 `med_attachment` 保存 bucket、objectKey、providerCode、visibility 等元数据。
- 前端不直接拼接对象地址；所有下载走 Java 端鉴权后返回预签名地址，或走受控下载接口。

## 2.2 本次不做的事

- 不做分布式 MinIO 集群。
- 不做复杂 IAM 多租户精细策略。
- 不做对象生命周期自动归档策略。
- 不做 KMS 加密、Object Lock、版本化等高级治理。

这些都可以后续迭代，但不是当前国赛项目必须项。

---

# 3. 推荐落地架构

```text
前端
  ↓
Java Spring Boot
  ├── MySQL（业务数据、附件元数据）
  ├── RabbitMQ（分析任务异步化）
  └── MinIO（原图、可视化、报告、导出文件）
                ↑                 ↓
             Python AI 服务（读原图、写 visual assets）
```

## 3.1 职责边界

### Java 负责

- 上传文件到 MinIO
- 生成对象键（objectKey）
- 保存 `med_attachment` 元数据
- 向 Python 下发 `bucketName + objectKey`
- 为前端生成受控下载链接或预签名链接
- 报告 PDF 的生成和归档

### Python 负责

- 按 `bucketName + objectKey` 从 MinIO 拉取原图
- 不直接读业务数据库
- 将分析产物上传到 `caries-visual`
- 回调 Java 时返回 visual assets 的 bucket / objectKey / file metadata

### 前端负责

- 不直接访问 MinIO 管理口
- 不保存 AccessKey / SecretKey
- 只通过 Java 后端获取受控访问能力

---

# 4. Bucket 规划（冻结版）

统一使用以下 bucket 名称：

| Bucket | 用途 | 谁写 | 谁读 | 备注 |
|---|---|---|---|---|
| `caries-image` | 原始病例影像 | Java | Python、Java | 全景片 / 根尖片 / 口内照 |
| `caries-visual` | AI 可视化产物 | Python | Java | mask / overlay / heatmap |
| `caries-report` | 报告 PDF | Java | Java | 医生版 / 患者版 |
| `caries-export` | 临时导出文件 | Java | Java | 临时下载使用 |

## 4.1 严格约束

- Python **禁止** 写 `caries-image` 和 `caries-report`。
- 前端 **禁止** 直接公开读 bucket。
- 所有 bucket 默认按私有对象处理。
- 任何跨模块共享都通过 object metadata 和后端受控链接实现。

---

# 5. Object Key 规则（冻结版）

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

## 5.1 原始影像

```text
org/1001/case/CASE202604150001/image/PANORAMIC/2026/04/15/90001/pan_01.jpg
org/1001/case/CASE202604150001/image/INTRAORAL/2026/04/15/90002/intra_01.jpg
```

## 5.2 可视化产物

```text
org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/MASK/90001/16/91001.png
org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/OVERLAY/90001/16/91002.png
org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/HEATMAP/90001/NA/91003.png
```

## 5.3 报告 PDF

```text
org/1001/case/CASE202604150001/report/DOCTOR/v1/RPT202604150001.pdf
org/1001/case/CASE202604150001/report/PATIENT/v1/RPT202604150002.pdf
```

## 5.4 导出文件

```text
org/1001/export/2026/04/15/20001/EXP202604150001/report_zip_01.zip
```

## 5.5 命名要求

- objectKey 由服务端生成，不由前端拼。
- 文件名尽量只使用小写英文、数字、下划线、短横线。
- 不把中文文件名直接作为 objectKey。
- objectKey 一旦入库，视为业务定位符，不允许随意重写。

---

# 6. MinIO 部署方案（单机 Docker Compose 版）

这是当前项目最稳的落地方式。

## 6.1 docker-compose.yml

```yaml
version: "3.9"

services:
  minio:
    image: minio/minio:latest
    container_name: caries-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: cariesadmin
      MINIO_ROOT_PASSWORD: ChangeMe_123456
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - ./data/minio:/data
    restart: unless-stopped
```

## 6.2 启动

```bash
docker compose up -d
```

## 6.3 控制台

- S3 API 端口：`9000`
- Console 端口：`9001`

浏览器访问：

```text
http://localhost:9001
```

登录后创建 4 个 bucket：

- `caries-image`
- `caries-visual`
- `caries-report`
- `caries-export`

## 6.4 必须做的初始化

1. 创建上述 4 个 bucket。
2. 确认 bucket 都是私有。
3. 上传一个测试文件，验证 Java / Python 两端都能读写。
4. 将根账号只用于初始化；联调阶段建议额外创建应用账号。

---

# 7. Java 端改造方案

## 7.1 改造目标

把当前 `LocalObjectStorageService` 替换为标准 `MinioObjectStorageService`，并保留统一接口抽象。

## 7.2 推荐接口

```java
public interface ObjectStorageService {
    UploadResult upload(InputStream inputStream,
                        long size,
                        String contentType,
                        String bucketName,
                        String objectKey,
                        Map<String, String> metadata);

    InputStream download(String bucketName, String objectKey);

    String getPresignedGetUrl(String bucketName, String objectKey, Duration expiry);

    String getPresignedPutUrl(String bucketName, String objectKey, Duration expiry);

    StatObjectResult stat(String bucketName, String objectKey);

    void remove(String bucketName, String objectKey);

    boolean exists(String bucketName, String objectKey);
}
```

## 7.3 推荐实现类

- `MinioObjectStorageService`
- `MinioBucketInitializer`
- `MinioObjectKeyGenerator`

## 7.4 Java 配置项

```yaml
caries:
  storage:
    provider: MINIO
    endpoint: http://127.0.0.1:9000
    access-key: caries_app
    secret-key: change_me
    secure: false
    buckets:
      image: caries-image
      visual: caries-visual
      report: caries-report
      export: caries-export
```

## 7.5 `med_attachment` 落库要求

每次上传成功后，Java 必须持久化以下关键字段：

- `bucket_name`
- `object_key`
- `storage_provider_code = MINIO`
- `visibility_code`
- `file_name`
- `original_name`
- `content_type`
- `file_ext`
- `file_size_bytes`
- `md5`
- `biz_module_code`
- `biz_id`
- `org_id`

## 7.6 Java 上传主链路

### 影像上传

1. 前端上传病例影像到 Java
2. Java 生成 objectKey
3. Java 调用 MinIO SDK 上传到 `caries-image`
4. Java 写 `med_attachment`
5. Java 写 `med_image_file`
6. Java 触发 analysis task
7. analysis 请求体中带上 `bucketName` 和 `objectKey`

### 报告归档

1. Java 生成 PDF
2. Java 上传到 `caries-report`
3. Java 写 `med_attachment`
4. Java 写 `rpt_record.attachmentId`
5. 导出时由 Java 生成预签名下载地址或代理下载

---

# 8. Python 端改造目标

这一部分是给 Python 端直接执行的核心内容。

## 8.1 Python 端的唯一原则

**Python 端只依赖 MinIO 和 Java 下发的对象元信息，不依赖本地共享目录。**

也就是说，Python 工程师不应该再写：

- “从某个共享目录直接读 jpg”
- “假设 Java 和 Python 共用同一个挂载盘”
- “直接拼接本机文件路径”

Python 端只能接受：

- `bucketName`
- `objectKey`
- `originalFilename`
- `contentType`
- `caseNo`
- `imageId`
- `imageTypeCode`

然后自己通过 MinIO SDK 下载对象。

---

# 9. Python 端环境变量（可直接使用）

```env
CG_MINIO_ENDPOINT=127.0.0.1:9000
CG_MINIO_ACCESS_KEY=caries_ai
CG_MINIO_SECRET_KEY=change_me
CG_MINIO_SECURE=false
CG_BUCKET_IMAGE=caries-image
CG_BUCKET_VISUAL=caries-visual
CG_BUCKET_REPORT=caries-report
CG_BUCKET_EXPORT=caries-export
CG_MINIO_REGION=
CG_MINIO_CONNECT_TIMEOUT_SECONDS=5
CG_MINIO_READ_TIMEOUT_SECONDS=30
CG_TEMP_DIR=/tmp/cariesguard
```

说明：

- `CG_MINIO_SECURE=false` 表示本地联调用 HTTP。
- 演示环境如果挂 HTTPS，把它改成 `true`，并同步换 endpoint。

---

# 10. Python 端依赖

```txt
minio>=7.2.0
Pillow>=10.0.0
orjson>=3.9.0
pydantic>=2.0.0
httpx>=0.27.0
```

安装：

```bash
pip install minio Pillow orjson pydantic httpx
```

---

# 11. Python 端目录建议（独立可执行版）

```text
ai-python/
├── app/
│   ├── main.py
│   ├── core/
│   │   ├── config.py
│   │   └── logging.py
│   ├── infra/
│   │   └── storage/
│   │       └── minio_client.py
│   ├── services/
│   │   ├── image_fetch_service.py
│   │   ├── visual_asset_service.py
│   │   └── callback_service.py
│   ├── schemas/
│   │   ├── request.py
│   │   └── callback.py
│   └── pipelines/
│       └── inference_pipeline.py
└── tests/
```

---

# 12. Python 端必须实现的 3 个核心能力

## 12.1 下载原始影像

输入：

- `bucketName`
- `objectKey`

输出：

- 本地临时文件路径，或内存字节流

要求：

- 不把业务图永久存本地
- 用完即删
- 下载失败时返回明确错误码

## 12.2 上传 visual assets

输入：

- 本地生成的 mask / overlay / heatmap
- `caseNo`
- `imageId`
- `toothCode`（如果有）

输出：

- `bucketName`
- `objectKey`
- `fileName`
- `contentType`
- `fileSizeBytes`

要求：

- 统一写入 `caries-visual`
- objectKey 命名符合本文档规则
- 上传成功后必须回传完整元信息给 Java

## 12.3 回调 Java

回调时，visual assets 不能只传一个“路径字符串”，必须传结构化对象，例如：

```json
{
  "assetTypeCode": "MASK",
  "bucketName": "caries-visual",
  "objectKey": "org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/MASK/90001/16/91001.png",
  "fileName": "mask_90001_16.png",
  "contentType": "image/png",
  "fileSizeBytes": 18342,
  "imageId": 90001,
  "toothCode": "16"
}
```

---

# 13. Python 端 MinIO 客户端代码（可直接落地）

## 13.1 `minio_client.py`

```python
from __future__ import annotations

import io
from dataclasses import dataclass
from datetime import timedelta
from pathlib import Path
from typing import BinaryIO

from minio import Minio
from minio.error import S3Error


@dataclass
class ObjectRef:
    bucket_name: str
    object_key: str


@dataclass
class UploadedObject:
    bucket_name: str
    object_key: str
    etag: str | None
    version_id: str | None
    size: int
    content_type: str
    file_name: str


class MinioStorageClient:
    def __init__(
        self,
        endpoint: str,
        access_key: str,
        secret_key: str,
        secure: bool = False,
    ) -> None:
        self.client = Minio(
            endpoint,
            access_key=access_key,
            secret_key=secret_key,
            secure=secure,
        )

    def ensure_bucket(self, bucket_name: str) -> None:
        if not self.client.bucket_exists(bucket_name):
            self.client.make_bucket(bucket_name)

    def download_to_file(self, ref: ObjectRef, target_path: str) -> str:
        path = Path(target_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        self.client.fget_object(ref.bucket_name, ref.object_key, str(path))
        return str(path)

    def download_bytes(self, ref: ObjectRef) -> bytes:
        response = self.client.get_object(ref.bucket_name, ref.object_key)
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()

    def upload_file(
        self,
        bucket_name: str,
        object_key: str,
        local_path: str,
        content_type: str,
    ) -> UploadedObject:
        result = self.client.fput_object(
            bucket_name,
            object_key,
            local_path,
            content_type=content_type,
        )
        size = Path(local_path).stat().st_size
        return UploadedObject(
            bucket_name=bucket_name,
            object_key=object_key,
            etag=getattr(result, "etag", None),
            version_id=getattr(result, "version_id", None),
            size=size,
            content_type=content_type,
            file_name=Path(local_path).name,
        )

    def upload_bytes(
        self,
        bucket_name: str,
        object_key: str,
        data: bytes,
        content_type: str,
        file_name: str,
    ) -> UploadedObject:
        result = self.client.put_object(
            bucket_name,
            object_key,
            io.BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        return UploadedObject(
            bucket_name=bucket_name,
            object_key=object_key,
            etag=getattr(result, "etag", None),
            version_id=getattr(result, "version_id", None),
            size=len(data),
            content_type=content_type,
            file_name=file_name,
        )

    def presigned_get_url(self, bucket_name: str, object_key: str, expires_seconds: int = 900) -> str:
        return self.client.presigned_get_object(
            bucket_name,
            object_key,
            expires=timedelta(seconds=expires_seconds),
        )
```

---

# 14. Python 端获取原图示例

```python
from pathlib import Path
from app.infra.storage.minio_client import MinioStorageClient, ObjectRef


def fetch_image_for_inference(storage: MinioStorageClient, bucket_name: str, object_key: str, temp_dir: str) -> str:
    target = Path(temp_dir) / object_key.replace("/", "_")
    return storage.download_to_file(ObjectRef(bucket_name, object_key), str(target))
```

---

# 15. Python 端上传 visual assets 示例

```python
from datetime import datetime


def build_visual_object_key(case_no: str, file_name: str) -> str:
    now = datetime.now()
    return f"visual/{now:%Y/%m/%d}/{case_no}/{file_name}"


def upload_mask(storage: MinioStorageClient, case_no: str, local_mask_path: str) -> dict:
    file_name = Path(local_mask_path).name
    object_key = build_visual_object_key(case_no, file_name)
    uploaded = storage.upload_file(
        bucket_name="caries-visual",
        object_key=object_key,
        local_path=local_mask_path,
        content_type="image/png",
    )
    return {
        "bucketName": uploaded.bucket_name,
        "objectKey": uploaded.object_key,
        "fileName": uploaded.file_name,
        "contentType": uploaded.content_type,
        "fileSizeBytes": uploaded.size,
    }
```

---

# 16. Java 发给 Python 的分析请求（建议冻结版）

```json
{
  "traceId": "trace-001",
  "taskNo": "TASK202604150001",
  "caseId": 10001,
  "caseNo": "CASE202604150001",
  "patientId": 70001,
  "orgId": 1001,
  "images": [
    {
      "imageId": 90001,
      "imageTypeCode": "PANORAMIC",
      "bucketName": "caries-image",
      "objectKey": "org/1001/case/CASE202604150001/image/PANORAMIC/2026/04/15/90001/pan_01.jpg",
      "originalFilename": "pan_01.jpg",
      "contentType": "image/jpeg"
    },
    {
      "imageId": 90002,
      "imageTypeCode": "INTRAORAL",
      "bucketName": "caries-image",
      "objectKey": "org/1001/case/CASE202604150001/image/INTRAORAL/2026/04/15/90002/intra_01.jpg",
      "originalFilename": "intra_01.jpg",
      "contentType": "image/jpeg"
    }
  ],
  "callbackUrl": "http://java-backend:8080/api/v1/ai/callbacks/analysis",
  "callbackToken": "token-xxx",
  "requestedAt": "2026-04-15T10:00:00+08:00"
}
```

## 16.1 关键要求

- `images[]` 中必须给 `bucketName + objectKey`
- 不给“服务器本地路径”
- Python 不依赖共享盘
- 所有下载能力都通过 MinIO SDK 完成

---

# 17. Python 回调 Java 的 visual assets 建议结构

```json
{
  "taskNo": "TASK202604150001",
  "taskStatusCode": "SUCCESS",
  "modelVersion": "cg_release_2026_04_v1",
  "visualAssets": [
    {
      "assetTypeCode": "MASK",
      "imageId": 90001,
      "toothCode": "16",
      "bucketName": "caries-visual",
      "objectKey": "org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/MASK/90001/16/91001.png",
      "fileName": "mask_90001_16.png",
      "contentType": "image/png",
      "fileSizeBytes": 18342
    },
    {
      "assetTypeCode": "OVERLAY",
      "imageId": 90001,
      "toothCode": "16",
      "bucketName": "caries-visual",
      "objectKey": "org/1001/case/CASE202604150001/analysis/TASK202604150001/caries-v1/OVERLAY/90001/16/91002.png",
      "fileName": "overlay_90001_16.png",
      "contentType": "image/png",
      "fileSizeBytes": 25492
    }
  ]
}
```

## 17.1 Java 侧拿到回调后必须做的事

- 将 visual asset 写入附件体系或结果表
- 不只保存 objectKey 字符串
- 要保存 bucket、objectKey、contentType、size、assetType

---

# 18. 权限与安全最小方案

## 18.1 最小账号方案

建议至少准备两个应用账号：

### Java 应用账号

权限：

- `caries-image`: 读写删查
- `caries-visual`: 读写删查
- `caries-report`: 读写删查
- `caries-export`: 读写删查

### Python 应用账号

权限：

- `caries-image`: 只读
- `caries-visual`: 读写
- `caries-report`: 无权限
- `caries-export`: 无权限

## 18.2 前端访问策略

- 前端绝不持有 MinIO AccessKey / SecretKey
- 前端绝不直接访问 Console
- 前端下载文件时，由 Java 校验权限后生成短时预签名 GET URL

## 18.3 演示环境最小安全要求

- 不使用默认密码
- Console 不暴露到公网
- 如果需要公网演示，MinIO 前面挂 Nginx / HTTPS

---

# 19. 联调顺序（强烈建议照这个顺序执行）

## 第一阶段：基础设施连通

1. 部署 MinIO
2. 创建 4 个 bucket
3. Java 端完成上传/下载
4. Python 端完成上传/下载
5. 人工验证 4 个 bucket 的读写权限

验收标准：

- Java 可上传 1 张测试图
- Python 可从 `caries-image` 下载该图
- Python 可向 `caries-visual` 上传 1 张测试 mask
- Java 可读取该 mask 的元信息和下载链接

## 第二阶段：病例主链路

1. 前端上传病例影像
2. Java 入库 `med_attachment` + `med_image_file`
3. Java 发起分析任务
4. Python 下载原图并执行 mock 推理
5. Python 上传 visual assets
6. Python 回调 Java
7. Java 保存结果并可在前端展示

验收标准：

- 无共享目录依赖
- 全链路仅使用 MinIO 作为对象存储
- visual assets 能在前端看到

## 第三阶段：报告与导出

1. Java 生成医生版 / 患者版 PDF
2. 上传到 `caries-report`
3. 导出接口生成预签名下载 URL
4. 写 `rpt_export_log`

验收标准：

- PDF 真实归档到 MinIO
- 数据库可追溯 objectKey
- 导出日志可审计

---

# 20. 测试清单

## 20.1 Java 端测试

- bucket 不存在时能自动初始化或明确报错
- objectKey 重复时不覆盖历史文件
- 上传后 `med_attachment` 落库完整
- 预签名 GET URL 可正常下载
- 删除前能校验业务引用

## 20.2 Python 端测试

- 正常下载原图
- 原图不存在时抛明确异常
- 正常上传 mask / overlay / heatmap
- 上传后回调体中 metadata 完整
- 临时文件推理后可清理

## 20.3 联调测试

- Java 上传，Python 能读
- Python 上传 visual，Java 能展示
- Python 无法写 `caries-report`
- 前端无法绕过 Java 直接读私有对象

---

# 21. 常见错误与规避办法

## 错误 1：Python 还在读共享目录

这是最常见的旧实现残留。

**处理办法**：
- 删除或禁用本地共享目录方案
- 所有图像输入统一改为 bucket + objectKey

## 错误 2：只保存 objectKey，不保存 bucketName

这会让后续跨 bucket 迁移和查询混乱。

**处理办法**：
- 所有对象元信息必须同时保存 bucketName + objectKey

## 错误 3：前端直接拼接对象地址

这会绕开权限控制。

**处理办法**：
- 所有下载必须走 Java 鉴权后签发 URL

## 错误 4：Python 直接拿 root 账号连接 MinIO

这会导致安全边界很差。

**处理办法**：
- Python 使用独立应用账号
- 权限只开到 `caries-image` 读和 `caries-visual` 写

## 错误 5：把 MinIO Console 地址当对象访问地址

Console 是管理界面，不是对象访问 API。

**处理办法**：
- 对象访问统一通过 MinIO S3 API endpoint 或 presigned URL

---

# 22. 项目负责人最终决策建议

如果你现在决定“这次就切到 MinIO”，那最稳的执行法不是两套并存，而是：

## 22.1 一次性冻结 5 条规则

1. **对象存储唯一标准实现就是 MinIO**
2. **Java/Python 不再依赖共享目录**
3. **附件体系以 bucketName + objectKey 为核心主键语义**
4. **前端不直接访问私有对象**
5. **Python 只读原图、只写 visual assets**

## 22.2 推荐执行顺序

- 先部署 MinIO
- 再改 Java 上传链路
- 再改 Python 下载/上传链路
- 再改回调 payload
- 最后改报告归档与导出

不要一上来就同时改所有业务模块。

---

# 23. 给 Python 端的直接任务单

以下内容可以直接发给 Python 工程师执行。

## Python 端本周必须完成

1. 接入 MinIO Python SDK
2. 完成 `MinioStorageClient`
3. 分析请求只接受 `bucketName + objectKey`
4. 下载原图到临时目录后推理
5. 将 mask / overlay / heatmap 上传到 `caries-visual`
6. 回调体返回完整 visual asset metadata
7. 删除共享目录依赖
8. 补 3 个测试：下载原图、上传 visual、临时文件清理

## Python 端提交物

- `app/infra/storage/minio_client.py`
- `app/services/image_fetch_service.py`
- `app/services/visual_asset_service.py`
- `tests/unit/test_minio_client.py`
- `tests/integration/test_visual_upload_flow.py`

---

# 24. 最终结论

对于你这个项目，**切 MinIO 是对的，而且应该尽快切**。

原因很简单：

- 你们的数据库、接口、报告、附件、答辩口径，本来就更适合标准对象存储；
- 当前继续用本地文件系统，只会让 Java/Python 协同越来越别扭；
- 一旦切到 MinIO，病例影像、可视化产物、报告 PDF、导出文件这四条线都会被统一；
- 这不仅是工程优化，更是答辩可信度优化。

如果按本文档执行，你可以把“当前文档里写 MinIO、实际却不是”的矛盾，正式消掉。
