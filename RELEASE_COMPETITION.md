# RELEASE_COMPETITION

## 1. 当前比赛版基线

- 仓库基线版本：`0.1.0-SNAPSHOT`
- 发布版本号：`v1.0.0-competition-release`
- Git commit hash：`[LATEST_COMMIT_HASH]`
- 发布日期：`2026-04-18`
- 适用分支：`main`
- 作品定位：面向龋病筛查场景的医疗 AI 辅助决策系统
- 业务主线：影像分析 -> uncertainty 复核 -> RAG 解释 -> 风险融合 -> 医生反馈回流

## 2. 当前默认运行参数

### Java 侧

- `CARIES_ANALYSIS_MODEL_VERSION=caries-v1`
- `CARIES_COMPETITION_MODE_ENABLED=false`（开发默认）

### Python 侧

- `CG_AI_RUNTIME_MODE=mock`
- `CG_RAG_KNOWLEDGE_VERSION=v1.0`
- `CG_LLM_PROVIDER_CODE=MOCK`
- `CG_LLM_MODEL_NAME=template-llm-v1`

说明：仓库默认值以“可复现、可离线演示”为优先，不以追求外部大模型效果为优先。

## 3. 推荐比赛版启动流程与验收方式

相比于早期开发版需要手动配置环境变量，当前基线提供了明确的“比赛流预设”脚本编排。请在代码库根目录下使用以下命令流：

**步骤一：一键部署比赛专版**
这将会基于 `env/competition.env` 加载内置离线 demo 配置与约束模式，并监控组件直至稳定：
```powershell
./scripts/competition-up.ps1
# Mac/Linux 用户执行 ./scripts/competition-up.sh
```

**步骤二：自动化基础验收**
在给验收专家展示前检测环境可用性（目前会评估 Java/Python 状态及比赛模式是否就绪）：
```powershell
./scripts/competition-acceptance.ps1
# Mac/Linux 用户执行 ./scripts/competition-acceptance.sh
```

**步骤三：演示完成后的彻底隔离销毁**
如果需要清理整个现场防止历史遗留对评审产生影响，使用：
```powershell
./scripts/competition-reset.ps1
# Mac/Linux 用户执行 ./scripts/competition-reset.sh
```

## 4. 默认 demo 样例

当前仓库已经内置标准的“比赛 seed 包”，不再依赖由于耗时不稳定所导致的老版本 E2E 生成临时数据。默认 demo 数据包含：

### 分析与复核主链

- `demo-doctor-01`：默认掩码演示医生
- `demo-case-low-uncertainty`：低 uncertainty 正常筛查闭环场景
- `demo-case-high-uncertainty`：高 uncertainty 触发人工复核场景

### 知识库与问答主链

- `demo-kb-caries-default`：内置基于龋病指南的预计算索引
- `demo-rag-question-set-v1`：预置演示用标准问答包

## 5. 比赛模式下已隐藏的暴露面

当前 Java 代码在比赛模式下会收缩以下暴露面：

- `system:*`
- `followup:*`
- `visit:*`
- `report:template:*`
- `report:export`
- 通用 `/dashboard`
- `/visits`

同时保留的 6 个比赛语义入口及底层复用关系如下表所示：

| 比赛入口 | 当前承接页面/路由 | 状态 |
| --- | --- | --- |
| Cases & Imaging | `/patients` | 需继续做比赛语义重构 |
| AI Analysis Tasks | `/analysis/tasks` | 已可用 |
| AI Result Detail | `/cases` | 需增强 AI 结果展示 |
| Knowledge & Citation | `/reports` | 需重构为 RAG 引用页 |
| Review & Feedback | `/images` | 需重构为 review 语义页 |
| AI Runtime & Evaluation | `/dashboard/model-runtime` | 需增强治理指标 |

## 6. 当前已知边界

- 当前仓库是医疗 AI 辅助决策系统，不是自动诊断系统。
- 当前仓库不是“高精度医学大模型”项目，默认运行模式仍以 `mock` 为主。
- 当前 demo 数据以脚本生成的脱敏临时 fixture 为主，不是完整静态比赛数据包。
- analysis 和 RAG 已有较完整自动化脚本；doctor review / feedback 仍需要结合接口或页面做人工验收。
- Java 是业务状态权威，Python 是 AI / RAG 能力提供方，这一边界不应被打破。

## 7. 建议给评委或审查者的阅读顺序

1. [README.md](README.md)
2. [backend-java/README.md](backend-java/README.md)
3. [backend-python/README.md](backend-python/README.md)
4. [Documents/07_AI闭环演示脚本.md](Documents/07_AI闭环演示脚本.md)
5. [Documents/13_比赛版环境与演示运行说明.md](Documents/13_比赛版环境与演示运行说明.md)
6. [Documents/15_比赛版验收与回归说明.md](Documents/15_比赛版验收与回归说明.md)
