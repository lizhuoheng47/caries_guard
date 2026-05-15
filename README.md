# CariesGuard

CariesGuard 是一个面向龋病筛查场景的医疗 AI 辅助决策系统，当前主线为：

`影像上传 -> AI 分析 -> 不确定性评估 -> 医生复核 -> RAG 解释 -> 风险评估 -> 报告 / 随访`

AI 结果用于辅助分析、解释和风险提示，不替代医生最终诊断。

> English version: [README.en.md](README.en.md)

## 当前状态

当前仓库已经包含一条可联调的主流程：

- 新建病例
- 上传 X 光影像
- 创建分析任务
- 查看分析详情
- 进入复核工作台
- 提交复核结果

前端当前已接入的主要页面：

- `dashboard/ai`：AI 业务看板
- `analysis`：分析任务队列
- `analysis/:taskId`：分析详情页
- `review/:taskId`：复核工作台
- `cases`：病例入口与影像上传
- `rag`：RAG 控制台
- `knowledge`：知识库页面

补充说明：

- 复核工作台已经支持左侧病例切换、影像联动、当前原始内容与当前修改内容展示。
- 病例入口已经支持点击选择文件、拖拽上传，以及“创建患者 -> 创建就诊 -> 创建病例 -> 上传影像 -> 发起分析”的完整前端链路。
- 为避免 Java `Long` 主键在前端丢精度，病例创建相关接口返回的关键 ID 已按字符串透传。

## 架构概览

```text
Browser / Script
  -> frontend/ (Vue 3 + Vite)
  -> backend-java/ (Spring Boot, business workflow)
      -> caries_biz (MySQL)
      -> Redis
      -> RabbitMQ
      -> MinIO
      -> backend-python/ (FastAPI, AI / RAG)
          -> caries_ai (MySQL)
          -> OpenSearch
          -> Neo4j
          -> OpenAI-compatible LLM Provider
```

职责边界：

- Java 负责业务主链、状态机、权限、报告、复核、随访和对外 API。
- Python 负责 AI 推理、RAG、知识库、运行日志和模型治理。
- RabbitMQ 负责分析任务异步投递。
- MinIO 负责原始影像、可视化产物、报告和导出文件存储。

## 仓库结构

- `frontend/`：Vue 3 + Vite + TypeScript 前端
- `backend-java/`：Java 业务后端，多模块 Maven 工程
- `backend-python/`：Python AI / RAG 服务，FastAPI + MQ Worker
- `Documents/`：正式文档与说明
- `scripts/`：启动、验收、灌数与辅助脚本
- `infra/`：基础设施初始化资源
- `env/`：环境预设

## 依赖与版本

本机已验证可运行的版本：

- Node.js `v24.13.0`
- Python `3.10.11`
- Java `17.0.12`
- Maven `3.9.12`

建议至少满足：

- Node.js 20+
- Python 3.10+
- JDK 17+
- Maven 3.9+
- Docker / Docker Compose（可选，但推荐）

## 快速启动

### 方案 A：Docker 启动后端与基础设施，前端本地运行

仓库当前的 `docker-compose.yml` 会启动：

- MySQL
- Redis
- RabbitMQ
- MinIO
- OpenSearch
- Neo4j
- Java 后端
- Python 后端

它不会启动前端，所以前端仍需单独运行。

1. 启动容器：

```powershell
docker compose up -d --build
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait-for-health.ps1
```

2. 检查健康状态：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

3. 启动前端：

```powershell
cd frontend
npm install
npm run dev
```

4. 打开页面：

- 前端开发服务器：`http://127.0.0.1:5173`
- Java 健康检查：`http://127.0.0.1:8080/actuator/health`
- Python 健康检查：`http://127.0.0.1:8001/ai/v1/health`
- RabbitMQ 管理台：`http://127.0.0.1:15672`
- MinIO Console：`http://127.0.0.1:9001`
- Neo4j Browser：`http://127.0.0.1:7474`

Docker 默认宿主机端口还包括：

- MySQL：`13306`
- Redis：`16379`
- OpenSearch：`9200`

比赛演示预设：

```powershell
docker compose --env-file env/competition.env up -d --build
```

### 方案 B：完全本地启动，不使用 Docker

完整联调需要先准备以下依赖服务：

- MySQL 8.x
- Redis
- RabbitMQ
- MinIO
- OpenSearch
- Neo4j

默认本地配置见：

- [application-local.yml](backend-java/caries-boot/src/main/resources/application-local.yml)
- [config.py](backend-python/app/core/config.py)
- [vite.config.ts](frontend/vite.config.ts)

建议使用以下默认参数：

- MySQL：`127.0.0.1:3306`，库 `caries_biz` / `caries_ai`
- Redis：`127.0.0.1:6379`
- RabbitMQ：`127.0.0.1:5672`
- MinIO：`http://127.0.0.1:9000`
- OpenSearch：`http://127.0.0.1:9200`
- Neo4j：`bolt://127.0.0.1:7687`

Java 后端：

```powershell
cd backend-java
$env:SPRING_PROFILES_ACTIVE="local"
mvn -pl caries-boot -am spring-boot:run
```

Python 后端：

```powershell
cd backend-python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m app.main
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

## 默认账号

- 用户名：`admin`
- 密码：`123456`

## 开发命令

前端构建：

```powershell
cd frontend
npm run build
```

Java 编译：

```powershell
cd backend-java
mvn -pl caries-boot -am -DskipTests compile
```

Python 单测示例：

```powershell
cd backend-python
.\.venv\Scripts\python -m pytest tests\unit\test_rag_service.py
```

## 前端联调说明

- 前端默认读取 [frontend/.env](frontend/.env)，当前配置为：
  - `VITE_API_BASE_URL=/api/v1`
  - `VITE_USE_MOCK=false`
- Vite 会把 `/api` 代理到 `http://localhost:8080`。
- 因此本地开发时，Java 后端建议直接运行在 `8080` 端口。

如果只想快速看界面，可临时把 `frontend/.env` 改为：

```env
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCK=true
```

但需要注意：

- 当前并不是所有页面都完全 mock 化。
- `cases`、`analysis`、`review` 等主流程页面更适合连接真实后端联调。

## 当前联调注意事项

- Docker Compose 默认不会启动前端，这是当前最容易被忽略的点。
- 病例创建链路依赖 MinIO、RabbitMQ、MySQL；任一服务异常都可能导致“创建并分析”失败。
- 复核工作台前端已经可用，但如果后端没有返回足够的复核队列数据，前端会基于分析任务列表展示复核入口。
- Python 默认推荐使用 `mock` 运行模式进行开发联调；`real` 只适合在模型、回调链路和知识库依赖都准备好后启用。

## 运行建议

- 开发 / 演示优先使用 Python `mock` 模式。
- 正式知识治理与 RAG 主路径为 `OpenSearch + Neo4j + OpenAI-compatible provider`。
- `real` 模式只应在模型权重、索引、图谱、回调和对象存储全部确认后启用。
