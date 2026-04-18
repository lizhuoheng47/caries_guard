# backend-java

`backend-java/` 是 CariesGuard 的业务主链和状态权威，不是一个独立宣传的“通用后台工程”。

它负责把龋病筛查场景中的病例、影像、分析任务、复核状态、RAG 调用和治理指标组织成可追踪的业务闭环。

## 当前职责

- 鉴权、审计、数据脱敏与基础业务约束
- 病例、影像、分析任务、结果摘要、复核、报告状态管理
- 向 RabbitMQ 投递分析任务
- 接收 Python callback 并更新业务状态
- 聚合 RAG 接口、AI 运行指标和 review 结果

Java 是状态权威，Python 不是。分析任务是否创建、是否成功、是否进入复核、是否允许进入后续业务状态，都由 Java 侧落库和推进。

## 比赛版保留的核心域

比赛口径下，Java 侧对外主线聚焦四类能力：

1. `analysis`
2. `review`
3. `rag integration`
4. `ai runtime dashboard`

当前比赛模式在界面级进一步收束为 6 个比赛语义入口，分别通过现有病例、分析、结果、报告、影像和 runtime 页面承接。

**当前实现边界：**
- 当前比赛模式已经完成菜单、权限和角色菜单绑定收束；
- 页面级语义重构仍在推进；
- 因此比赛版当前是“入口层已成形，页面层继续打磨”。

其余域仍作为支撑层保留在工程中，但不作为主展示面：

- `system`
- `patient`
- `image`
- `report`
- `followup`

这不是否认这些域存在，而是避免把作品叙事带回“宽后台系统”。

## 模块结构

当前父工程模块来自 [pom.xml](./pom.xml)：

- `caries-common`
- `caries-framework`
- `caries-system`
- `caries-integration`
- `caries-patient`
- `caries-image`
- `caries-analysis`
- `caries-report`
- `caries-followup`
- `caries-dashboard`
- `caries-boot`

比赛模式主要影响的是暴露面，不是物理模块删除。

## competition mode

Java 侧通过 `caries.competition.enabled` 统一读取比赛模式，Docker 环境变量为：

```env
CARIES_COMPETITION_MODE_ENABLED=true
```

开启后，当前代码会过滤：

- `system:*`
- `followup:*`
- `visit:*`
- `report:template:*`
- `report:export`
- 通用 `/dashboard`
- `/visits`

并保留：

- `analysis`
- `review`
- `rag`
- `/dashboard/model-runtime`

过滤发生在权限聚合、权限校验和菜单查询层，不只是“前端不显示”。

## 与 Python 的边界

- Java 不负责模型推理、向量检索和 LLM 生成
- Java 不直接拥有 Python AI 库中的运行日志主写入逻辑
- Java 负责业务鉴权、状态推进、任务编排和结果对外暴露
- Python 回传的是结构化 AI 结果，Java 负责将其纳入业务状态机

## 本地开发与 Docker 的区别

### Docker

仓库根目录 `docker-compose.yml` 会同时启动 MySQL、Redis、RabbitMQ、MinIO、Java、Python。  
这是当前最完整、最接近比赛复现的运行方式。

### 本地开发

如果单独运行 Java，需要自己准备：

- MySQL
- Redis
- RabbitMQ
- MinIO
- 可访问的 Python AI 服务

当前更推荐用根目录 Compose 统一联调，再在 Java 模块内做编译和测试。

## 常用命令

编译：

```powershell
mvn -pl caries-boot -am -DskipTests compile
```

定向测试：

```powershell
mvn -pl caries-system -am test -Dtest=AuthAppServiceTests,SystemAdminQueryAppServiceTests,SystemPermissionAuthorityServiceTests -Dsurefire.failIfNoSpecifiedTests=false
```

如需本地启动 Spring Boot，可在基础依赖准备完毕后从本目录尝试：

```powershell
mvn -pl caries-boot -am spring-boot:run
```

Java 健康检查：

```text
http://127.0.0.1:8080/actuator/health
```

## 对外叙事限制

本目录相关文档和说明统一遵守以下约束：

- 不把 Java 工程描述成“完整医疗管理平台”
- 不把 callback、review、report 说成 AI 之外的主卖点
- 不把 Java 描述成模型推理主体
- 不把系统输出描述成医生诊断结论
