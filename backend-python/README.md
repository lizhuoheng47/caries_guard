# CariesGuard Python AI service

该模块消费 RabbitMQ 分析任务，读取 MinIO 影像，执行质量检查、牙位候选、病灶分割、分级和风险计算，并把结构化结果回调给 Java。

应用只通过仓库根目录的完整 Docker Compose 链路启动：

```powershell
docker compose up -d --build
```

运行链路固定启用全部阶段。其中 DC1000 UNet TorchScript 是真实分割模型；质量、牙位候选、分级和风险仍为规则实现。不存在面向用户的 mock、比赛或独立分割启动入口。

训练、导出和评估脚本位于 `training/scripts/`，它们不是应用启动模式。

健康检查：`http://127.0.0.1:8001/ai/v1/health`。
