# CariesGuard

CariesGuard 是面向牙科影像筛查的辅助分析系统。当前仓库只保留一套运行方式：Docker Compose 全链路启动。

## 唯一运行链路

```text
浏览器
  -> Vue 前端
  -> Java 业务服务
  -> MySQL / Redis / MinIO / RabbitMQ
  -> Python AI 服务
  -> 质量规则检查
  -> 牙位候选规则
  -> DC1000 UNet TorchScript 病灶分割
  -> 分级与风险规则
  -> Java 回调、复核与报告
```

项目不再提供比赛模式、演示模式、staging 预设或独立分割服务启动入口。训练和评估脚本仍然保留，但不属于应用启动方式。

> 当前只有病灶分割阶段使用已经训练的真实模型。质量、牙位候选、分级和风险阶段仍为明确标识的规则实现，不应视为临床验证模型。

## 环境要求

- Docker Desktop
- Docker Compose
- Windows 使用 GPU 时，需要开启 WSL2 和 Docker Desktop NVIDIA GPU 支持
- 推荐显卡：NVIDIA RTX 4060 8GB 或更高

## 启动

首次运行可复制配置模板：

```powershell
Copy-Item .env.docker.example .env
```

然后只使用这一条启动命令：

```powershell
docker compose up -d --build
```

等待服务健康：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\wait-for-health.ps1
docker compose ps
```

访问地址：

- 系统页面：http://127.0.0.1:5173
- Java 健康检查：http://127.0.0.1:8080/actuator/health
- Python 健康检查：http://127.0.0.1:8001/ai/v1/health
- RabbitMQ：http://127.0.0.1:15672
- MinIO：http://127.0.0.1:9001

停止系统但保留数据库和影像：

```powershell
docker compose down
```

查看日志：

```powershell
docker compose logs -f backend-java backend-python frontend
```

## 配置

唯一配置模板是 `.env.docker.example`。复制成 `.env` 后可以修改密码、端口、SMTP 和推理设备。

默认使用 GPU：

```env
CG_MODEL_DEVICE=cuda:0
```

如果只是临时排查 GPU 环境，可改成 `cpu`；这不会切换应用模式，只改变模型运行设备。

## 目录

- `frontend/`：Vue 3 前端
- `backend-java/`：Spring Boot 业务服务
- `backend-python/`：FastAPI、消息消费者和模型推理
- `backend-python/training/`：训练与评估脚本
- `backend-python/assets/models/`：应用模型清单与发布权重
- `data/`：原始和处理后的训练数据
- `artifacts/`：训练结果
- `infra/`：数据库初始化资源
- `scripts/`：健康检查、数据准备和测试脚本

## 验证

```powershell
cd frontend
npm run build

cd ..\backend-java
mvn -pl caries-boot -am test

cd ..\backend-python
.\.venv\Scripts\python.exe -m pytest
```

AI 结果仅用于辅助研究和提示，不替代牙科医生的诊断。
