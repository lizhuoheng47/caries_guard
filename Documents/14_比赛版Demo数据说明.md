# 比赛版Demo数据说明

## 1. 文档目的

说明当前仓库中哪些 demo 数据是真实存在的，哪些只是比赛叙事中的目标状态。

## 2. 当前已经存在的数据来源

### 2.1 Java 基线迁移数据

来自：

- [V001__baseline_schema.sql](/E:/caries_guard/backend-java/caries-boot/src/main/resources/db/migration/V001__baseline_schema.sql)

当前已包含的基础数据：

- 默认机构与角色
- 默认菜单
- 开发管理员账号 `admin`
- 与病例、影像、分析、复核、报告相关的基础表结构

这些数据用于让系统能启动和登录，不等于比赛展示样例本身。

### 2.2 Python AI 库初始化

来自：

- `infra/mysql/init/02_caries_ai_schema.sql`
- Python 启动时的 Alembic 迁移

它们提供的是 AI / RAG 表结构基础，不直接提供可演示的病例样例。

## 3. 当前可复现的 demo fixture

### 3.1 分析 demo fixture

来自：

- [phase5-analysis-docker-e2e.ps1](/E:/caries_guard/scripts/phase5-analysis-docker-e2e.ps1)

脚本会动态生成：

- 脱敏患者
- 脱敏就诊
- 脱敏病例
- 原始影像附件
- analysis task
- visual assets
- 结果摘要与 `rawResultJson`

比赛演示最推荐使用的两个场景：

1. `5c-b-hybrid-grading-low-uncertainty`
2. `5c-c-hybrid-grading-high-uncertainty`

它们分别用于展示：

- AI 正常输出与低 uncertainty
- 高 uncertainty 触发复核语义

### 3.2 RAG demo fixture

来自：

- [phase3-rag-docker-e2e.ps1](/E:/caries_guard/scripts/phase3-rag-docker-e2e.ps1)

脚本会动态生成：

- 最小 analyzed case
- 影像与 visual asset 记录
- 风险与牙位记录
- 医生问答请求与日志

默认使用：

- `kbCode=caries-default`
- 医生问题：`How should high uncertainty caries cases be handled?`

## 4. 数据特征

当前 demo 数据遵循以下规则：

- 全部为脱敏或脚本生成数据
- 使用合成病例号、任务号、对象存储路径
- 主要用于验证 AI 闭环，而不是表示真实临床分布
- 每次脚本运行都会生成新的编号和对象 key

## 5. 当前尚未内置的内容

以下内容在仓库里尚未以固定 seed 的形式内置：

- 固定不变的比赛患者样例集
- 固定不变的医生反馈样例集
- 一键导入的比赛知识文档包
- 一键恢复到同一 case 编号的脚本

因此当前 demo 数据说明必须强调：

当前仓库的 demo 是“可脚本复现的临时 fixture”，不是“静态比赛样例包”。

## 6. 对比赛演示的建议

如果需要稳定展示主线，建议按以下顺序组织：

1. 用 `phase5` 生成低 uncertainty 样例
2. 用 `phase5` 生成高 uncertainty 样例
3. 用 `phase3` 展示 RAG 引用与日志
4. 在高 uncertainty 样例上补充人工 review / feedback 演示

## 7. 不应过度宣传的地方

当前 demo 数据说明不应声称：

- 仓库已经自带完整临床级样本库
- 仓库已经自带固定比赛 seed 数据包
- 仓库已经自动化验证了所有 review / feedback 样例
