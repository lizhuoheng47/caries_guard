# 15_Codex任务拆解与修改说明

## 使用说明

本文档是给 Codex 直接执行代码修改用的任务拆解清单。  
每个任务包都包含：

- 修改目标
- 需要编辑/新增的文件
- 具体修改动作
- 实施约束
- 完成判定
- 回归检查

Codex 必须按任务包顺序执行，但可以在同一任务包内部做必要的文件发现与路径适配。

---

## 任务包 A：仓库口径统一与比赛版说明重写

### A.1 修改目标
把根 README、Java README、Python README、发布说明、比赛导向文档全部统一到一个叙事：

- 场景：龋病筛查
- 方法：影像分析 + uncertainty 复核 + RAG 解释 + 风险融合 + 医生反馈回流
- 边界：AI 辅助，不替代医生
- 架构：Java 业务主链 + Python AI/RAG + MQ + callback
- 比赛版：收缩暴露面，聚焦 AI 主链

### A.2 需要编辑/新增的文件

**必须编辑**
- `README.md`
- `backend-java/README.md`
- `backend-python/README.md`

**建议新增**
- `RELEASE_COMPETITION.md`
- `Documents/13_比赛版环境与演示运行说明.md`
- `Documents/14_比赛版Demo数据说明.md`
- `Documents/15_比赛版验收与回归说明.md`

### A.3 具体修改动作

1. 重写根 README 的“目录”“Docker”“环境变量”“文档”部分：
   - 增加“比赛版启动方法”
   - 增加“比赛版默认主链”
   - 增加“比赛版边界与非目标”
   - 增加“外部审查入口”

2. 新建或重写 `backend-java/README.md`
   内容必须包含：
   - Java 只负责业务主链和状态权威；
   - 当前比赛版保留的核心域：
     - analysis
     - review
     - rag integration
     - ai runtime dashboard
   - 其他模块作为支撑层保留，不作为比赛主展示面；
   - competition mode 的作用范围；
   - 本地运行和 Docker 运行的区别。

3. 新建或重写 `backend-python/README.md`
   内容必须包含：
   - Python 当前实现的 AI 主链；
   - quality / detection / segmentation / grading / uncertainty / risk / rag / llm gateway / governance；
   - mock / heuristic / real adapter 的边界说明；
   - 当前比赛版强调“受控解释与可追踪”，而非“自称高精度医学大模型”。

4. 新增 `RELEASE_COMPETITION.md`
   内容必须包含：
   - 当前比赛版版本号
   - 当前 model version
   - 当前 knowledge version
   - 当前 runtime mode
   - 默认启动方式
   - 默认 demo 样例
   - 已隐藏模块
   - 已知边界

### A.4 实施约束
- 所有 README 口径必须一致；
- 不允许一个 README 说“宽平台”，另一个 README 说“AI 闭环”；
- 文案不能过度夸大模型能力；
- 必须明确 AI 不替代医生。

### A.5 完成判定
- 根 README、Java README、Python README 首屏叙事一致；
- 仓库任意入口都能读出同一个作品；
- `RELEASE_COMPETITION.md` 可直接给评委或工程师查看。

### A.6 回归检查
- 文档链接是否可跳转；
- README 中的目录与实际存在的文件路径是否一致；
- README 中写到的环境变量在实际配置里是否存在。

---

## 任务包 B：比赛模式运行时闭环

### B.1 修改目标
把 `CARIES_COMPETITION_MODE_ENABLED` 从“文档存在”变为“运行时真实生效”的能力。

### B.2 需要编辑/新增的文件

**必须编辑**
- `docker-compose.yml`
- `.env.docker.example`
- `backend-java/caries-boot/src/main/resources/application-docker.yml`

**需要搜索并编辑**
- Java 中读取菜单/权限/路由/页面暴露的相关类
- Java 中 dashboard 菜单或页面聚合类
- Java 中 system / followup / report 相关菜单或权限生成代码

**建议新增**
- `env/competition.env`
- Java 比赛模式配置类 / 过滤器 / 服务类
- 比赛模式相关单元测试或集成测试

### B.3 具体修改动作

1. 在 `docker-compose.yml` 的 `backend-java.environment` 中补充：
   ```yaml
   CARIES_COMPETITION_MODE_ENABLED: ${CARIES_COMPETITION_MODE_ENABLED:-true}
   ```

2. 在 `.env.docker.example` 中把比赛版环境变量归类整理：
   - 基础数据库
   - 存储
   - MQ
   - AI runtime
   - competition mode
   - demo/knowledge

3. 新增 `env/competition.env`
   推荐默认值：
   - `CARIES_COMPETITION_MODE_ENABLED=true`
   - `CG_AI_RUNTIME_MODE=hybrid` 或经验证的 `real`
   - 固定 `CG_UNCERTAINTY_REVIEW_THRESHOLD`
   - 固定 `CG_LLM_PROVIDER_CODE`
   - 固定 `CARIES_ANALYSIS_MODEL_VERSION`
   - 固定 `CG_RAG_KNOWLEDGE_VERSION`

4. 在 Java 中实现 competition mode 统一读取入口
   若仓库已有对应配置绑定类，则扩展；
   若无，则新增：
   - `CompetitionModeProperties`
   - `CompetitionModeService`

5. 在菜单聚合逻辑中增加比赛模式过滤
   规则：
   - 隐藏 `system:*`
   - 隐藏 `followup:*`
   - 隐藏 `report:template:*`
   - 隐藏 `report:export`
   - 隐藏通用 `/dashboard`
   - 保留 AI runtime dashboard、analysis、review、rag 主链入口

6. 在权限聚合逻辑中同步过滤
   不允许只是前端不显示，权限仍然下发。

7. 在必要时增加接口级保护
   对比赛版明确不开放的管理接口，可根据 competition mode 拒绝访问或不注册菜单入口。

8. 新增比赛模式自检接口或日志摘要
   启动时输出：
   - competition mode 开启/关闭
   - 被隐藏的域
   - 保留的核心域

### B.4 实施约束
- 不大规模删除模块；
- 不破坏开发版；
- 不影响 analysis / review / rag / dashboard/model-runtime 主链；
- 不改动核心 callback 契约。

### B.5 完成判定
- competition mode 开启时，系统主菜单只保留比赛主链；
- 关闭时，开发版仍可跑；
- Docker 比赛环境可直接生效。

### B.6 回归检查
- Spring 启动成功；
- competition mode 值打印正确；
- 菜单树和权限树与预期一致；
- 隐藏模块不会误伤核心链路。

---

## 任务包 C：菜单、页面、入口与文案收束

### C.1 修改目标
把页面组织从“后台系统式”调整为“AI 演示主线式”。

### C.2 需要搜索并编辑的内容
Codex 需要在前端或返回菜单树的后端代码中，定位以下类型文件：

- 菜单树装配器
- 仪表盘入口
- 路由配置
- 页面标题/菜单标题
- AI 结果页/任务页/复核页/RAG 页的文案配置

### C.3 具体修改动作

1. 统一比赛版一级菜单为以下 6 类（命名可微调，但语义必须一致）：
   - 病例与影像
   - AI 分析任务
   - AI 结果详情
   - 智能解释与引用
   - 医生复核与反馈
   - AI 运行与评估

2. 隐藏或从一级菜单下移的内容：
   - 系统管理
   - 通用字典
   - 权限维护
   - 复杂报告模板维护
   - 随访模块
   - 通用运营看板

3. 调整比赛版首页入口
   默认不进入系统门户；
   默认进入病例/任务列表或 AI 分析入口。

4. 统一文案风格
   - “RAG 问答” 改为 “智能解释与知识引用”
   - “模型看板” 改为 “AI 运行与评估看板”
   - “报告管理” 若保留，改为 “结果说明与报告”
   - 避免“模板管理”“系统配置”这类后台感过强入口出现在比赛版主视图

5. 清理弱价值操作按钮
   只保留比赛主链必要动作：
   - 发起分析
   - 查看结果
   - 查看视觉证据
   - 发起智能解释
   - 提交复核
   - 查看 AI 指标

### C.4 实施约束
- 页面名称要服务答辩，不要服务开发者术语；
- 不得误删开发版能力；
- 比赛版页面跳转路径尽量短。

### C.5 完成判定
- 评委进入系统 10 秒内能看到 AI 主线；
- 页面数量明显收束；
- 所有一级入口都与 AI 主链相关。

### C.6 回归检查
- 菜单点击是否正常；
- 路由是否存在 404；
- 页面标题是否与 README 和答辩口径一致。

---

## 任务包 D：Docker 比赛环境与启动/重置脚本

### D.1 修改目标
形成标准比赛环境，而不是仅有一个开发联调 compose。

### D.2 需要编辑/新增的文件

**编辑**
- `docker-compose.yml`
- `.env.docker.example`

**新增**
- `env/competition.env`
- `scripts/competition-up.ps1`
- `scripts/competition-up.sh`
- `scripts/competition-reset.ps1`
- `scripts/competition-reset.sh`

### D.3 具体修改动作

1. 比赛启动脚本 `competition-up`
   脚本职责：
   - 读取 `env/competition.env`
   - 启动 docker compose
   - 轮询 Java / Python health
   - 执行 demo 数据导入
   - 执行知识库导入/重建
   - 输出 demo 访问地址、账户、案例编号、competition mode 状态

2. 比赛重置脚本 `competition-reset`
   脚本职责：
   - 关闭容器
   - 清理比赛样例数据
   - 重建 compose
   - 重新导入 demo data 与 knowledge
   - 再次执行 acceptance 检查

3. 比赛验收脚本 `competition-acceptance`
   脚本职责：
   - 检查 Java health
   - 检查 Python health
   - 检查 competition mode
   - 检查 demo case 是否存在
   - 检查知识库版本是否正确
   - 检查至少一条分析任务是否可成功闭环

4. 输出结果格式标准化
   启动和验收脚本都要输出：
   - PASS/FAIL
   - 服务地址
   - 关键环境变量摘要
   - 当前 demo 状态

### D.4 实施约束
- PowerShell 与 Shell 两侧都要支持；
- 尽量避免依赖本机特殊软件；
- 输出要可读，不要满屏无意义日志。

### D.5 完成判定
- 新机器上只按文档执行脚本即可启动比赛环境；
- 不需要手工导入大量数据；
- 可以快速重置到标准状态。

### D.6 回归检查
- Windows 脚本可运行；
- Linux/macOS 脚本可运行；
- 脚本多次执行具备幂等性或至少可重复恢复。

---

## 任务包 E：Demo seed data、知识库样例与固定问答集

### E.1 修改目标
建立比赛版标准样例集，支撑完整演示与答辩。

### E.2 需要新增的文件
- `scripts/seed_demo_data.sql` 或等价导入脚本
- `scripts/seed_demo_knowledge.py` 或等价脚本
- `scripts/seed_demo_assets.ps1` / `.sh`
- `Documents/14_比赛版Demo数据说明.md`
- `evidence/qa-cases/`
- `evidence/review-cases/`

### E.3 具体修改动作

1. 建立基础实体样例
   至少包括：
   - 1 个机构
   - 2 个医生账号
   - 2 个患者
   - 2 个病例
   - 4~6 张脱敏影像

2. 建立两个固定主样例
   **样例 A：低 uncertainty 正常闭环**
   - 分析成功
   - `needsReview=false`
   - 可进入 RAG 患者解释与医生问答
   - 风险等级中低

   **样例 B：高 uncertainty 复核闭环**
   - 分析成功
   - `needsReview=true`
   - 可进入 review pending
   - 医生可提交修正结论
   - 能展示反馈回流

3. 建立知识库样例
   文档至少包含：
   - 龋病指南摘要
   - 患者教育材料
   - 风险因素定义
   - 复查周期规则
   - 项目内部解释模板

4. 建立 RAG 问题集
   至少准备：
   - 5 个患者端问题
   - 5 个医生端问题
   每个问题配：
   - 预期回答要点
   - 预期引用文档
   - 是否允许直接结论
   - 是否需要保守措辞

5. 建立 review 差异样例
   - AI 结论与医生一致样例
   - AI 结论与医生不一致样例
   - 高 uncertainty 正确触发复核样例

### E.4 实施约束
- 全部数据必须脱敏；
- 不得混入敏感明文；
- 所有知识文档必须标注来源与版本；
- 演示样例不追求数量，追求稳定可复现。

### E.5 完成判定
- 启动脚本执行后，比赛环境即带完整样例；
- 不需要现场临时造病例；
- Demo 主链可完全复现。

### E.6 回归检查
- 导入脚本是否幂等；
- 知识库重建是否成功；
- RAG 问题是否确实能命中知识样本。

---

## 任务包 F：AI 结果页、复核页、RAG 页、风险展示页精修

### F.1 修改目标
把比赛页面从“后台字段堆砌页”改造成“AI 证据展示页”。

### F.2 需要搜索并编辑的内容
Codex 需要定位：
- analysis 结果详情页
- visual asset 展示页或组件
- review 页面
- rag 问答页面
- dashboard/model-runtime 页面
- 与这些页面对应的 API DTO 或返回组装层

### F.3 具体修改动作

1. 结果页重构为 6 区块：
   - 原图与 visual assets
   - grading / confidence
   - uncertainty / review
   - risk level / risk factors / follow-up
   - RAG answer / citations
   - 折叠 JSON

2. 结果页必须显式展示：
   - `gradingLabel`
   - `confidenceScore`
   - `uncertaintyScore`
   - `needsReview`
   - `riskLevel`
   - `followUpRecommendation`
   - `reviewReason`
   - `knowledgeVersion`

3. `reviewReason` 需要从机器码映射到人可读标签：
   - `HIGH_UNCERTAINTY`
   - `QUALITY_ALERT`
   - `RISK_RULE_REVIEW`
   - `MANUAL_REVIEW_REQUIRED`

4. `evidenceRefs` 分类展示
   分成：
   - 风险因素证据
   - 视觉证据
   - 知识证据

5. 复核页结构化
   复核表单中加入：
   - 医生确认分级
   - 是否同意 AI
   - 修正原因分类
   - 是否同意 RAG 解释
   - 复查建议

6. RAG 页增加：
   - `answer`
   - `citations`
   - `retrievedChunks`
   - `confidence`
   - `safetyFlags`
   - `refusalReason`
   - `knowledgeVersion`

7. 风险展示页/区域增加：
   - 风险等级
   - 风险分数
   - 风险因子列表
   - 跟踪建议
   - 风险解释文本

### F.4 实施约束
- 不改动核心 callback 顶层结构；
- 页面主视图不要被 JSON 占据；
- 不要把 AI 信息埋在二级弹窗里；
- 文案必须保守，不得越权诊断。

### F.5 完成判定
- 不懂代码的人也能看懂 AI 给出了什么、为什么这样给；
- 复核页能体现“AI 辅助 + 医生决策”；
- RAG 页能体现“知识引用 + 安全边界”。

### F.6 回归检查
- DTO 返回是否兼容；
- 页面字段是否完整；
- visual assets 是否正常加载；
- review 提交是否不破坏原有业务状态。

---

## 任务包 G：AI 运行看板、证据包与验收机制

### G.1 修改目标
把 `/dashboard/model-runtime` 真正做成比赛可讲的“AI 治理看板”。

### G.2 需要编辑/新增的内容
- model-runtime 页面/组件
- 对应后端接口或聚合服务
- `evidence/` 目录骨架
- `scripts/competition-acceptance.*`
- `Documents/15_比赛版验收与回归说明.md`

### G.3 具体修改动作

1. 看板指标重构
   至少展示：
   - task 成功率
   - callback 成功率
   - visual asset 成功率
   - high uncertainty 触发率
   - review 完成率
   - risk 输出覆盖率
   - RAG 引用完整率
   - 医生复核一致率

2. 看板状态摘要
   同时展示：
   - `modelVersion`
   - `knowledgeVersion`
   - `runtimeMode`
   - `llmProviderCode`
   - `llmModelName`

3. 证据目录骨架
   新增：
   ```text
   evidence/
     demo-screenshots/
     payloads/
     metrics/
     qa-cases/
     review-cases/
   ```

4. 验收脚本
   输出：
   - 服务健康状态
   - competition mode
   - demo 数据是否存在
   - 样例任务是否可跑
   - 知识库是否命中
   - RAG 是否返回引用

5. 在文档中定义比赛验收标准
   - 什么算通过
   - 什么算失败
   - 失败后如何恢复

### G.4 实施约束
- 看板要偏“可治理”，不是偏“运维监控”；
- 数据可以先基于现有日志与记录聚合，不要求引入新基础设施；
- 脚本输出必须让非开发者也能读懂。

### G.5 完成判定
- 看板可用于答辩；
- evidence 目录结构就绪；
- 验收脚本可运行。

### G.6 回归检查
- 看板接口不报错；
- 指标值可获取；
- acceptance 脚本 PASS/FAIL 逻辑正确。

---

## 任务包 H：仓库卫生与比赛发布收尾

### H.1 修改目标
把仓库整理到“可公开审查”的程度。

### H.2 需要编辑/新增的内容
- `.gitignore`
- 根目录无关文件
- `RELEASE_COMPETITION.md`
- `Documents/外部审查清单`（可选）
- 可能还需要清理 `.idea/`、日志、临时文件

### H.3 具体修改动作

1. 删除无关文件
   - `.idea/`
   - 临时日志
   - 本地调试输出
   - 无意义中间文件

2. 完善 `.gitignore`
   覆盖：
   - Java `target/`
   - Python `__pycache__/`
   - `.idea/`
   - `.vscode/`
   - 日志文件
   - 本地 `.env.local`
   - 临时 evidence 输出

3. 增加外部审查清单文档
   建议新增：
   - `Documents/16_外部审查挑瑕疵清单.md`
   内容包括：
   - 评委会问什么
   - Claude 可能挑什么
   - 工程师会抓什么
   - 仓库如何自检

4. 发布说明补充
   在 `RELEASE_COMPETITION.md` 中补充：
   - 启动方式
   - demo 路线
   - 已隐藏功能
   - 仍保留但不展示的模块
   - 已知边界与保守表述

### H.4 实施约束
- 不把大量二进制截图直接堆进仓库；
- 保持仓库清洁；
- 所有文档都必须与当前代码状态一致。

### H.5 完成判定
- `git status` 清爽；
- 无无关文件残留；
- 发布说明齐全。

### H.6 回归检查
- `.gitignore` 生效；
- 清理后不影响运行；
- 发布文档中的路径和脚本全部真实存在。

---

## 最终输出要求

Codex 完成本轮修改后，必须额外输出一份简短变更报告，至少包含：

1. 本轮新增/修改了哪些文件；
2. competition mode 如何生效；
3. demo 数据如何导入；
4. 默认演示样例有哪些；
5. 运行了哪些测试/脚本；
6. 当前仍存在的已知边界是什么。
