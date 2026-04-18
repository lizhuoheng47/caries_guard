# backend-java

`backend-java/` 是 CariesGuard 的业务主链和状态权威。

它负责承接患者、病例、影像、分析任务、复核、报告、随访、权限和看板等业务域，并把 Python 回传的 AI 结果纳入业务状态机。

## 职责

- 用户、角色、菜单、数据权限、字典、审计
- 患者、就诊、病例、影像、附件
- AI 分析任务创建、查询、重试、callback 落库
- 报告、RAG 集成、医生复核、纠偏反馈
- 随访计划、任务、记录
- 业务看板和 AI Runtime 看板
- 模型治理、数据集快照、评估记录

## 模块

父工程模块来自 [pom.xml](./pom.xml)：

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

## 与 Python 的边界

- Java 不负责模型推理、向量检索和 LLM 生成。
- Java 不直接写 Python 的推理日志、检索日志和知识库表。
- Java 负责业务鉴权、状态推进、附件主数据和结果对外暴露。
- Python 负责生成结构化 AI 结果，Java 负责将结果纳入业务流程。

## Competition Mode

Java 侧通过 `CARIES_COMPETITION_MODE_ENABLED` 控制比赛模式。

开启后主要收缩：

- `system:*`
- `followup:*`
- `visit:*`
- `report:template:*`
- `report:export`
- 通用 `/dashboard`
- `/visits`

保留主线：

- `analysis`
- `review`
- `rag`
- `/dashboard/model-runtime`

## 本地运行

编译：

```powershell
mvn -pl caries-boot -am -DskipTests compile
```

启动：

```powershell
mvn -pl caries-boot -am spring-boot:run
```

健康检查：

```text
http://127.0.0.1:8080/actuator/health
```

推荐使用仓库根目录 `docker compose up -d --build` 统一联调。

## 测试

示例：

```powershell
mvn -pl caries-system -am test -Dtest=AuthAppServiceTests,SystemAdminQueryAppServiceTests,SystemPermissionAuthorityServiceTests -Dsurefire.failIfNoSpecifiedTests=false
```

`caries-boot/src/test/java/com/cariesguard/boot/e2e/` 已覆盖分析主链、看板、随访、模型治理等集成测试。

## 相关文档

- [项目概览](../Documents/01_项目概览.md)
- [功能说明](../Documents/02_功能说明.md)
- [接口与集成说明](../Documents/04_接口与集成说明.md)
- [数据字典](../Documents/05_数据字典.md)
