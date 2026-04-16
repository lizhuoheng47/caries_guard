# CariesGuard

多模态龋齿智能识别与分级预警平台。

## 项目简介

CariesGuard 面向口腔影像病例管理、AI 分析、医生复核、报告生成、随访触发与数据治理场景。当前仓库重点包含 Java 后端、Python AI 服务底座、Docker Compose 基础设施和项目设计文档。

## 总体架构

- `frontend-web`：前端联调目标，当前仓库未包含完整前端实现
- `backend-java`：Spring Boot 主业务后端
- `backend-python`：AI 服务底座与 Java/Python 回调契约
- `MySQL`：业务数据与治理数据
- `Redis`：缓存基础设施
- `RabbitMQ`：AI 任务异步消息
- `MinIO`：对象存储，承载原始影像、AI 可视化、报告和导出文件

## 仓库结构

- `backend-java/`：Java 多模块后端工程
- `backend-python/`：Python AI 服务底座
- `Documents/`：总体设计、数据字典、开发规范与答辩文档
- `docker-compose.yml`：本地容器化联调环境
- `DOCKER.md`：Docker Compose 联调说明
- `.env.docker.example`：Docker 环境变量示例

## 快速启动

复制环境变量示例：

```bash
cp .env.docker.example .env
```

启动完整联调环境：

```bash
docker compose up --build
```

后台启动：

```bash
docker compose up -d --build
```

查看配置展开结果：

```bash
docker compose config
```

## 默认端口

- `backend-java`: `8080`
- `mysql`: `13306`
- `redis`: `16379`
- `rabbitmq`: `5672` / `15672`
- `minio`: `9000` / `9001`

## 当前完成度

- Java 主业务链已落地
- Python 侧为 AI 服务底座与接口契约阶段
- MinIO 四桶方案与结构化 object key 已接入
- 文件访问主模式为 MinIO presigned URL
- 报告导出与 AI 可视化资产已接入 attachment 血缘治理

## MinIO 对象存储约定

固定 bucket：

- `caries-image`：原始影像，长期保留
- `caries-visual`：AI 可视化资产，30 天自动清理
- `caries-report`：报告 PDF，长期保留
- `caries-export`：导出文件，7 天自动清理

Attachment 类型语义：

- `RAW_IMAGE`
- `VISUAL`
- `REPORT`
- `EXPORT`

Object key 规则：

```text
org/{orgId}/case/{caseNo}/image/{imageTypeCode}/{yyyy}/{MM}/{dd}/{attachmentId}/{filename}
org/{orgId}/case/{caseNo}/analysis/{taskNo}/{modelVersion}/{assetTypeCode}/{relatedImageId}/{toothCode}/{attachmentId}.{ext}
org/{orgId}/case/{caseNo}/report/{reportTypeCode}/v{versionNo}/{reportNo}.pdf
org/{orgId}/export/{yyyy}/{MM}/{dd}/{operatorId}/{exportLogId}/{reportNo}.{ext}
```

文件访问主模式为 presigned URL：

- 主路径：`GET /api/v1/files/{attachmentId}/access-url`
- 兜底路径：`GET /api/v1/files/{attachmentId}/content`

`/content` 仅作为受控代理兜底入口保留，不作为前端常规访问主路径。

## Git 双远程推送说明

本项目当前默认分支为 `master`，Gitee 远程仓库使用 `origin`，GitHub 远程仓库使用 `github`。

查看当前分支：

```bash
git branch --show-current
```

查看远程仓库：

```bash
git remote -v
```

推荐的远程配置：

```bash
origin  https://gitee.com/monologue47/caries_guard.git
github  https://github.com/lizhuoheng47/caries_guard.git
```

如果 GitHub 远程地址误写成 `git@https://github.com/...`，需要修正：

```bash
git remote set-url github https://github.com/lizhuoheng47/caries_guard.git
```

推送到 Gitee：

```bash
git push origin master
```

推送到 GitHub：

```bash
git push github master
```

第一次推送到 GitHub 时绑定 upstream：

```bash
git push -u github master
```

如果执行 `git push -u github main` 出现：

```text
error: src refspec main does not match any
```

说明本地没有 `main` 分支。本项目当前分支是 `master`，应改用：

```bash
git push -u github master
```

如果后续确实要把默认分支从 `master` 改成 `main`：

```bash
git branch -m master main
git push -u github main
```

在 Gitee 仍使用 `master` 的情况下，建议先保持 `master`。

## 文档索引

- [backend-java/README.md](backend-java/README.md)
- [backend-python/README.md](backend-python/README.md)
- [DOCKER.md](DOCKER.md)
- [Documents/03_项目总体设计文档.md](Documents/03_项目总体设计文档.md)
- [Documents/04_Java后端开发说明书.md](Documents/04_Java后端开发说明书.md)
