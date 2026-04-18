# 14_Codex工程实施总纲

## 1. 文档用途

本文档不是泛化建议，而是面向 **Codex 直接执行代码修改** 的工程总纲。  
目标是在当前 `caries_guard` 仓库基础上，完成一轮“比赛版精修与收官”改造，使仓库、运行时、演示环境、样例数据、页面暴露面和 AI 证据链全部统一到人工智能实践赛口径。

本文档默认以下事实已经成立：

- 根 README 已经将项目定义为“龋病筛查场景下的医疗 AI 辅助决策系统”；
- Java 侧已有 `caries.competition.enabled` 配置入口；
- Python 推理主链已经输出 `phase5d-1` 的 `riskLevel`、`riskFactors`、`reviewReason`、`knowledgeVersion`、`evidenceRefs` 等字段；
- Docker 已包含 MySQL、Redis、RabbitMQ、MinIO、Java、Python 六类核心服务；
- Java 仍保留多模块父工程，Python 仍保留 AI / RAG / risk / governance 主链。

本计划的原则是：

1. **不再做架构级推倒重来**；
2. **不再继续做宽后台扩张**；
3. **不大拆 Java 模块结构**；
4. **保留代码，收束暴露，统一口径，补齐演示与证据**；
5. **所有修改都围绕 AI 主链服务，而不是围绕后台完整性服务**。

---

## 2. 当前仓库基线

Codex 在执行前，需要接受以下基线判断：

### 2.1 已经成立的优势
- 项目主线已从“普通后台系统”收束到“医疗 AI 闭环”；
- Java / Python 边界清晰，MQ + callback 主链合理；
- Python 已具备质量检查、检测、分割、分级、不确定性、风险融合、RAG、LLM gateway、日志治理等主链能力；
- `rawResultJson` 已成为主要 AI 扩展承载区；
- 比赛模式已经被引入配置层。

### 2.2 仍需解决的问题
- README 口径可能仍未在根、Java、Python、Documents 中完全统一；
- `CARIES_COMPETITION_MODE_ENABLED` 需要真正打通到 Docker 运行时；
- 菜单、权限、接口、看板的比赛版暴露面需要形成可验证收束；
- 缺少默认可复现的 demo 数据与知识库样例；
- 缺少标准化比赛启动脚本、重置脚本、验收脚本；
- 缺少比赛证据包和外部审查清单；
- 页面可能仍偏“系统管理化”，而不是“AI 结果展示化”。

---

## 3. 本轮改造的目标状态

本轮改造完成后，仓库应达到以下状态：

### 3.1 仓库口径统一
- 根 README、`backend-java/README.md`、`backend-python/README.md`、`Documents/` 的首屏叙事统一；
- 所有文档都围绕“影像分析—不确定性复核—RAG 解释—风险融合—医生反馈回流”展开；
- 不再把作品对外描述成“完整口腔后台系统”。

### 3.2 比赛模式闭环
- Docker 启动时可以显式开启比赛模式；
- 开启后自动隐藏非比赛核心模块暴露；
- 菜单、权限、接口、看板四层都体现收束；
- 默认比赛环境即为 competition mode。

### 3.3 可复现演示
- 存在标准 demo 种子数据；
- 存在固定病例、固定影像、固定知识库、固定问答样例；
- Docker 启动后，无需人工拼装数据即可进入演示状态。

### 3.4 AI 结果可展示
- 结果页能直接展示 grading / uncertainty / review / risk / citations / visual assets；
- 复核页可展示 AI 结果与医生修正的差异；
- RAG 页可展示答案、引用、知识版本与安全边界；
- 看板页可展示 AI 治理指标，而非仅日志。

### 3.5 可审查
- 仓库干净、结构清晰；
- 具备比赛版说明、演示脚本、验收清单和外部挑瑕疵清单；
- 可直接交给 Claude、工程师、评委进行挑刺。

---

## 4. 变更边界

## 4.1 允许修改的内容
- README 与比赛文档；
- Docker 和环境变量；
- 比赛模式配置和菜单/权限过滤；
- 演示入口与页面文案；
- Demo seed data；
- 脚本、测试、证据目录；
- RAG / risk / review 展示细节；
- 看板指标和页面组织。

## 4.2 禁止做的事情
- 物理删除大批 Java 模块；
- 破坏 Java 是业务状态权威这一边界；
- 让 Python 直接写 `caries_biz` 业务主表；
- 破坏 callback 契约；
- 把新增 AI 字段从 `rawResultJson` 大规模移出到不稳定顶层；
- 在无充分验证的前提下，把比赛版改造成“强依赖外网大模型”的方案；
- 引入不必要的复杂基础设施，如 K8s、链路平台、复杂 observability 套件。

---

## 5. Codex 执行总策略

本轮实施要求 Codex 采用 **小步提交、逐层收口、每步可回归** 的策略。

### 5.1 执行要求
- 每完成一个任务包，先本地验证，再进入下一包；
- 先改配置和文档，再改暴露面，再补 demo 数据，再补页面和证据；
- 每一步都同步更新 README/脚本/测试，而不是只改代码。

### 5.2 提交要求
建议按任务包拆成多次提交，示例：

- `feat(competition): wire competition mode into docker runtime`
- `feat(competition): add menu and permission exposure filter`
- `feat(demo): add competition demo seed data and bootstrap scripts`
- `feat(ai-ui): refine analysis/review/rag display for competition mode`
- `docs(competition): unify repo narrative and add release guide`
- `test(competition): add acceptance script and evidence pack skeleton`

### 5.3 回归要求
每个任务包完成后至少执行：

1. Java 编译或测试通过；
2. Python 启动与核心测试通过；
3. Docker 能启动；
4. Health 接口正常；
5. 至少一个 demo task 可以跑通；
6. README 与脚本同步更新。

---

## 6. 任务包总览

本轮实施拆为 8 个任务包，Codex 必须逐包落地：

### 任务包 A：仓库口径统一与比赛版说明重写
重写根 README、Java README、Python README、发布说明，统一比赛叙事。

### 任务包 B：比赛模式运行时闭环
把 `CARIES_COMPETITION_MODE_ENABLED` 打通到 Docker、Spring 配置与暴露面过滤。

### 任务包 C：菜单、权限、接口、看板暴露面收束
实现比赛版菜单树、权限树与页面入口裁剪。

### 任务包 D：Docker 比赛环境与启动/重置脚本
新增比赛环境文件与一键启动/重置脚本。

### 任务包 E：Demo seed data 与知识库样例
补齐病例、影像、知识库、RAG 问题集、反馈样例。

### 任务包 F：AI 结果页、复核页、RAG 页、风险展示页精修
把页面从后台管理式改成证据展示式。

### 任务包 G：AI 运行与评估看板、证据包、验收脚本
形成 AI 可治理展示面和可复现验收机制。

### 任务包 H：仓库卫生与比赛发布收尾
清理无关文件，补 `RELEASE_COMPETITION.md`、外部审查清单和提交前校验。

---

## 7. 任务包完成定义

一轮完整实施完成后，应满足以下完成定义：

1. 比赛模式默认可启动；
2. Docker 一键启动后可进入比赛界面；
3. 菜单只保留 AI 主链；
4. 具备至少两套 demo 病例；
5. 具备固定知识库样例与 RAG 问题集；
6. AI 结果页可展示证据链；
7. 复核页可展示 AI 与医生差异；
8. 看板可展示 AI 运行与评估指标；
9. 仓库 README 与文档全部统一；
10. 可直接交由 Claude / 工程师 / 评委挑瑕疵。

---

## 8. Codex 必须交付的文件清单

Codex 最终应至少交付以下新增或修改成果：

### 8.1 文档类
- `README.md`
- `backend-java/README.md`
- `backend-python/README.md`
- `RELEASE_COMPETITION.md`
- `Documents/13_比赛版环境与演示运行说明.md`
- `Documents/14_比赛版Demo数据说明.md`
- `Documents/15_比赛版验收与回归说明.md`

### 8.2 配置与脚本类
- `docker-compose.yml`
- `.env.docker.example`
- `env/competition.env`
- `scripts/competition-up.ps1`
- `scripts/competition-up.sh`
- `scripts/competition-reset.ps1`
- `scripts/competition-reset.sh`
- `scripts/competition-acceptance.ps1`
- `scripts/competition-acceptance.sh`

### 8.3 数据与证据类
- `scripts/seed_demo_data.sql` 或等价脚本
- `scripts/seed_demo_knowledge.py` 或等价脚本
- `scripts/seed_demo_assets.ps1/sh` 或等价脚本
- `evidence/demo-screenshots/.gitkeep`
- `evidence/payloads/.gitkeep`
- `evidence/metrics/.gitkeep`
- `evidence/qa-cases/.gitkeep`
- `evidence/review-cases/.gitkeep`

### 8.4 Java/Python 代码类
- 比赛模式配置接入代码
- 菜单/权限过滤代码
- AI 结果页/复核页/RAG 页/看板精修代码
- Demo seed data 对应的数据导入/初始化代码
- 对应测试代码

---

## 9. 给 Codex 的总指令

Codex 在执行本轮修改时，应严格遵守以下总指令：

1. **先读当前仓库，再修改**，不要凭空重构；
2. **沿用现有架构边界**，不要破坏 Java / Python 分工；
3. **新增 AI 字段优先保留在 `rawResultJson`**；
4. **比赛版以收束暴露和证据展示为目标**，不是做更多后台功能；
5. **所有新增脚本必须可在 Windows PowerShell 和 Linux/macOS Shell 两侧运行**；
6. **所有新增文档必须与根 README 口径一致**；
7. **所有新增页面文案必须服务 AI 主链展示**；
8. **每个任务完成后都必须补验证步骤和验收标准**。

---

## 10. 总结

本轮工程不再属于“继续开发功能”，而属于“把已有系统收成比赛作品”。  
因此，所有实现动作都必须围绕以下一句话展开：

> 让任何一个外部审查者，从代码、文档、界面、演示任一入口进入，都能明确看出：这是一个可追踪、可解释、可复核、可治理的医疗 AI 实践系统，而不是一个普通后台系统。
