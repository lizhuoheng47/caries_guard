# CariesGuard

CariesGuard 是一个面向龋病筛查场景的医疗 AI 辅助决策系统。仓库主线固定为：

`影像分析 -> uncertainty 复核 -> RAG 解释 -> 风险融合 -> 医生反馈回流`

这里的 AI 用于辅助分析、解释和风险提示，不替代医生最终诊断。

> English version: [README.en.md](README.en.md)

## 项目定位

- 场景：口腔龋病筛查与解释
- 方法：影像分析、知识检索增强生成、风险融合、医生复核反馈
- 边界：AI 提供结构化结果和证据链，医生保留最终判断权
- 架构：Java 负责业务主链和状态权威，Python 负责 AI/RAG 能力提供

## 系统主线

```text
脱敏影像 / 病例上下文 / 知识文档 / 医生反馈
  -> Java Backend 创建业务任务、维护状态、鉴权与审计
  -> RabbitMQ 投递分析任务
  -> Python Backend 执行 quality / detection / segmentation / grading / uncertainty / risk / rag
  -> HTTP callback 回传结构化结果与 visual assets 元数据
  -> Java Backend 落库、推进 review / report / feedback 状态
```

## 角色边界

### Java Backend

- 业务主链入口
- 状态权威与任务编排
- MQ 投递、callback 接收、结果落库
- review、报告、RAG 集成、AI 运行指标聚合

### Python Backend

- 影像分析流水线
- uncertainty 评估与风险融合
- RAG、知识检索、LLM 网关
- AI 运行日志、检索日志、知识治理

这两个后端是协作关系，不是相互替代关系。Python 不直接拥有 `caries_biz` 业务主表状态；Java 不直接实现模型推理和向量检索主链。

## 比赛版与开发版

| 维度 | 比赛版 | 开发版 |
| --- | --- | --- |
| 对外叙事 | 医疗 AI 辅助决策闭环 | 全量工程联调与模块开发 |
| 菜单/权限暴露 | 收束到 analysis / review / RAG / AI dashboard 主链 | 保留更宽的业务与管理暴露 |
| 关键开关 | `CARIES_COMPETITION_MODE_ENABLED=true` | `CARIES_COMPETITION_MODE_ENABLED=false` |
| AI 运行模式 | 优先可复现演示，推荐 `mock` 或经验证的 `hybrid` | 允许更自由的本地联调 |
| 样例数据 | 以脚本生成的脱敏 demo fixture 为主 | 以迁移基线和开发自建数据为主 |

比赛模式不会删除模块代码，只会收缩权限和菜单暴露面。当前隐藏规则由 Java 侧统一控制，默认针对：

- `system:*`
- `followup:*`
- `report:template:*`
- `report:export`
- 通用 `/dashboard`

同时保留 AI 主链相关能力，包括 `/dashboard/model-runtime`。

## 仓库结构

- `backend-java/`：业务主链、状态权威、RAG 集成、review、dashboard 聚合
- `backend-python/`：AI 推理、RAG、知识治理、LLM 网关、运行日志
- `Documents/`：架构、契约、比赛导向说明、演示和验收文档
- `scripts/`：现有 Docker E2E 与证据采集脚本
- `infra/`：MySQL 初始化脚本和基础设施资源
- `RELEASE_COMPETITION.md`：比赛版发布口径和已知边界

## Docker 启动

推荐先复制环境文件：

```powershell
Copy-Item .env.docker.example .env
```

比赛版最小建议配置：

```env
CARIES_COMPETITION_MODE_ENABLED=true
CG_AI_RUNTIME_MODE=mock
CARIES_ANALYSIS_MODEL_VERSION=caries-v1
CG_RAG_KNOWLEDGE_VERSION=v1.0
CG_LLM_PROVIDER_CODE=MOCK
CG_LLM_MODEL_NAME=template-llm-v1
CG_UNCERTAINTY_REVIEW_THRESHOLD=0.35
```

启动全栈：

```powershell
docker compose up -d --build
docker compose ps
```

健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8001/ai/v1/health
```

默认开发管理员账号来自 Java 基线迁移：

- 用户名：`admin`
- 密码：`123456`

## 当前可复现的比赛演示入口

仓库当前没有单独前端仓库，比赛演示以接口、任务闭环和证据脚本为主。最稳定的入口有三类：

1. 影像分析闭环  
   `scripts/phase5-analysis-docker-e2e.ps1` 会生成脱敏病例、影像、分析任务和 visual asset 证据。

2. RAG 解释闭环  
   `scripts/phase3-rag-docker-e2e.ps1` 会生成最小分析病例，调用 Java 的医生问答接口，并校验 citations / retrieval / llm 日志。

3. 比赛说明与发布口径  
   [RELEASE_COMPETITION.md](RELEASE_COMPETITION.md)、`Documents/07_...`、`Documents/13_...`、`Documents/15_...`

推荐命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\phase5-analysis-docker-e2e.ps1 -SkipComposeUp -Phase5COnly -WaitSeconds 180
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\phase3-rag-docker-e2e.ps1 -SkipComposeUp
```

## 关键环境变量

当前仓库中最重要的运行时变量如下：

```env
CARIES_COMPETITION_MODE_ENABLED=false
CARIES_ANALYSIS_MODEL_VERSION=caries-v1

CG_AI_RUNTIME_MODE=mock
CG_MODEL_QUALITY_ENABLED=false
CG_MODEL_TOOTH_DETECT_ENABLED=false
CG_MODEL_SEGMENTATION_ENABLED=false
CG_MODEL_GRADING_ENABLED=false
CG_MODEL_RISK_ENABLED=false
CG_UNCERTAINTY_REVIEW_THRESHOLD=0.35

CG_RAG_KNOWLEDGE_VERSION=v1.0
CG_LLM_PROVIDER_CODE=MOCK
CG_LLM_MODEL_NAME=template-llm-v1
```

说明：

- `CARIES_ANALYSIS_MODEL_VERSION` 由 Java 作为业务主链版本号下发，同时 Python 通过 `CG_MODEL_VERSION` 继承该值。
- `CG_AI_RUNTIME_MODE` 支持 `mock`、`hybrid`、`real`。
- 当前 Docker 默认最稳妥的可复现模式仍是 `mock`。

## 文档入口

- `Documents/06_人工智能实践赛改造说明.md`
- `Documents/07_AI闭环演示脚本.md`
- `Documents/08_AI评估指标与实验设计.md`
- `Documents/09_知识图库建设与治理规范.md`
- `Documents/10_答辩问答与作品话术.md`
- `Documents/11_参赛作品说明书重构稿.md`
- `Documents/12_功能精简与赛道聚焦建议.md`
- `Documents/13_比赛版环境与演示运行说明.md`
- `Documents/14_比赛版Demo数据说明.md`
- `Documents/15_比赛版验收与回归说明.md`

## 比赛版边界与非目标

以下表述是当前仓库的明确边界：

- 这不是一个“普通后台系统”的包装版本，核心展示面是 AI 主链，而不是系统管理能力。
- 这也不是“高精度医学大模型”项目。当前 Python 侧提供的是可控的推理、解释、检索和日志治理能力。
- AI 输出包含 `uncertainty`、`reviewReason`、`citations`、`knowledgeVersion` 和 `riskFactors`，目的是支持复核，不是替代医生结论。
- 当前 Docker 演示最稳定的是脚本生成的脱敏样例，不是完整静态比赛 seed 套件。
- 医生反馈回流是系统设计主线的一部分；当前自动化回归重点已覆盖 analysis 和 RAG，review / feedback 仍需结合接口或页面做人工验收。

## 外部审查入口

如果要快速判断这个仓库是不是围绕医疗 AI 主线组织，可以直接看：

1. [README.md](README.md)
2. [RELEASE_COMPETITION.md](RELEASE_COMPETITION.md)
3. [backend-java/README.md](backend-java/README.md)
4. [backend-python/README.md](backend-python/README.md)
5. [Documents/07_AI闭环演示脚本.md](Documents/07_AI闭环演示脚本.md)
6. [Documents/15_比赛版验收与回归说明.md](Documents/15_比赛版验收与回归说明.md)
