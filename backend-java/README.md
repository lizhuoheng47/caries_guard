# CariesGuard Java backend

该模块负责用户与权限、患者、就诊、病例、影像、分析任务、医生复核、报告和随访等业务流程。

应用只通过仓库根目录的完整 Docker Compose 链路启动，不再包含比赛菜单过滤或比赛权限模式：

```powershell
docker compose up -d --build
```

单独执行测试：

```powershell
mvn -pl caries-boot -am test
```

健康检查：`http://127.0.0.1:8080/actuator/health`。
