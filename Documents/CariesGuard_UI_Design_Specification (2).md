# CariesGuard UI Design Specification

## Holographic Command Console · Neural V2

> 本文档是 CariesGuard 医疗 AI 辅助决策系统的完整前端设计规范。  
> 面向前端工程师，覆盖所有 8 个核心页面的布局、色彩、组件、交互与动效细节。  
> 设计风格：**深空暗色 + 全息 HUD + 玻璃拟态 + 精细光效**

---

## 一、全局设计语言 / Global Design Language

### 1.1 设计哲学

CariesGuard 的视觉定位不是"后台管理系统"，而是**医疗 AI 诊断指挥舱**。评委在打开系统的前 3 秒内必须感受到：

- 这是一个 AI 系统，不是管理后台
- 系统正在"活着运行"——有脉冲、有扫描线、有实时数据流
- 每个颜色、每个动画都承担信息语义，不是装饰

核心视觉灵感来源：

- Apple Vision Pro 的空间 UI
- Arc Browser 的深色调设计
- 科幻电影 HUD（钢铁侠头盔 / 普罗米修斯全息台）
- 顶级医疗 AI 公司（Aidoc / Viz.ai / PathAI）

### 1.2 色彩系统 / Neural Palette

每个颜色都承担明确的**信号语义**：

| 名称 | HEX 值 | CSS 变量 | 语义角色 | 使用场景 |
|------|--------|---------|---------|---------|
| Neural Cyan | `#00E5FF` | `--cyan` | AI · 主色 · 正在运算 | 品牌色、主操作按钮、激活状态、侧边栏高亮、扫描线 |
| Soft Cyan | `#5FDCFF` | `--cyan-soft` | AI 辅助色 | 次级文字高亮、URL 栏、坐标读数 |
| Knowledge Violet | `#8B5CF6` | `--violet` | 知识 · RAG · 引用 | RAG 面板标题、citation 标签、知识图库、引用来源 |
| Alert Amber | `#FFB547` | `--amber` | 不确定 · 需复核 | uncertainty 进度条、待复核徽章、阈值线、警告提示 |
| Critical Magenta | `#FF3D7F` | `--magenta` | 高风险 · 错误 · 危险 | 高风险病例标签、失败状态、G4 分级、错误日志 |
| Safe Emerald | `#00FFA3` | `--emerald` | 安全 · 成功 · 在线 | 成功状态、低 uncertainty、系统在线、医生确认 |
| Void Black | `#030812` | `--void` | 深空底色 | 页面背景、侧边栏底色 |
| Deep Surface | `#0A1428` | `--surf` | 面板底色 | 卡片/面板背景、影像区域背景 |
| Elevated | `#0F1B33` | `--elev` | 悬浮面板 | 弹窗、工具栏、下拉菜单 |
| Ice White | `#E4F0FF` | `--tp` | 主文字 | 标题、核心数值、重要信息 |
| Steel Blue | `#8BA3C7` | `--ts` | 次级文字 | 正文、说明文字、表格内容 |
| Ghost Dim | `#4A5F82` | `--td` | 弱化文字 | 标签、占位符、禁用状态、时间戳 |
| Line Subtle | `rgba(100,200,255,0.12)` | `--ln` | 常规边框 | 面板边框、分割线、表格线 |
| Line Hot | `rgba(100,200,255,0.25)` | `--lh` | 强调边框 | 激活输入框、hover 状态 |

辉光规则——所有重要色彩在关键位置使用 `text-shadow` 或 `box-shadow` 产生**发光效果**：

```css
/* 文字辉光 */
.val-cyan { color: #00E5FF; text-shadow: 0 0 12px rgba(0,229,255,0.4); }
.val-amber { color: #FFB547; text-shadow: 0 0 12px rgba(255,181,71,0.3); }
.val-magenta { color: #FF3D7F; text-shadow: 0 0 12px rgba(255,61,127,0.3); }

/* 元素辉光 */
.glow-cyan { box-shadow: 0 0 12px rgba(0,229,255,0.4); }
.glow-emerald { box-shadow: 0 0 8px rgba(0,255,163,0.3); }
```

### 1.3 字体系统 / Typography

| 角色 | 字体 | 大小 | 权重 | 用途 |
|------|------|------|------|------|
| 品牌字体 | `"SF Mono", Consolas, Monaco, monospace` | — | — | 所有数字、状态标签、按钮文字、标识符 |
| 正文字体 | `-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif` | — | — | 标题、正文、描述文字 |
| 页面大标题 | sans-serif | 18–20px | 500 | 页面名称，例如"影像分析结果" |
| 面板标题 | sans-serif | 11–12px | 500 | 面板内部标题 |
| KPI 数值 | monospace | 18–26px | 500 | 核心指标数字 |
| KPI 标签 | monospace | 8–9px | 400 | 指标名称，全大写 + 0.12em letter-spacing |
| 正文内容 | sans-serif | 11–12px | 400 | 表格内容、描述信息 |
| 弱化标注 | monospace | 8–9px | 400 | 时间戳、坐标、版本号 |
| 按钮 | monospace | 9–10px | 500 | 全大写 + 0.08em letter-spacing |

所有数字必须使用 `font-variant-numeric: tabular-nums;` 确保等宽对齐。

### 1.4 间距系统 / Spacing

| Token | 值 | 使用场景 |
|-------|-----|---------|
| `--gap-xs` | 4px | 图标与文字间距、迷你元素 |
| `--gap-sm` | 8px | 紧凑组件内部、列表项间距 |
| `--gap-md` | 12px | 面板内部间距、卡片之间 |
| `--gap-lg` | 16px | 区域间距、主内容区 padding |
| `--gap-xl` | 22px | 页面主内容区 padding |
| `--gap-xxl` | 32px | Hero 区域 padding |

### 1.5 圆角系统 / Border Radius

| Token | 值 | 使用场景 |
|-------|-----|---------|
| `--radius-xs` | 2px | 标签、小徽章、状态芯片 |
| `--radius-sm` | 3px | 按钮、输入框 |
| `--radius-md` | 6px | 面板、卡片 |
| `--radius-lg` | 10px | 页面外框、弹窗 |

注意：HUD 风格整体偏向**直角/微圆角**，不使用大圆角。科技感来源于精密的直边，而不是柔和的圆润。

### 1.6 背景系统 / Background

所有页面共用同一套深空背景：

```css
.page-bg {
  background:
    radial-gradient(ellipse at top right, rgba(0,229,255,0.06) 0%, transparent 45%),
    radial-gradient(ellipse at bottom left, rgba(139,92,246,0.05) 0%, transparent 45%),
    #030812;
}

/* 叠加网格纹理 */
.page-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,229,255,0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,229,255,0.025) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
}
```

径向光晕（右上角 Cyan + 左下角 Violet）营造"光源在空间中"的深度感。网格线极弱，仅在仔细看时才能发现，暗示精密仪器的刻度。

### 1.7 动效系统 / Animation

| 动效名称 | CSS | 持续时间 | 使用场景 |
|---------|-----|---------|---------|
| 脉冲呼吸 | `opacity: 1 → 0.3 → 1` | 1.2–1.5s infinite | LIVE 标签、状态点、当前时间轴节点 |
| 扫描线 | `top: 10% → 85% → 10%` | 3s ease-in-out infinite | 影像查看器扫描线 |
| 轨道旋转 | `rotate: 0deg → 360deg` | 20s linear infinite | 登录页 logo 轨道环 |
| 粒子浮动 | `translateY(0) → translateY(-12px)` | 4s ease-in-out infinite | 登录页背景粒子 |
| 光标闪烁 | `opacity: 1 → 0 → 1` | 1s infinite | 输入框光标 |

所有动画必须加 `@media (prefers-reduced-motion: no-preference)` 适配无动画偏好。

### 1.8 推荐技术栈

| 类别 | 推荐方案 | 备注 |
|------|---------|------|
| 框架 | Vue 3 + TypeScript | 与 Java 后端常见搭配 |
| 构建工具 | Vite | 开发体验最佳 |
| CSS 方案 | Tailwind CSS + 自定义 CSS 变量 | 基础组件用 Tailwind，HUD 特效用自定义 CSS |
| 组件库 | 不使用现成组件库 | 避免"若依感"，所有组件手写 |
| 图表库 | Apache ECharts (深色主题) | 支持 SVG / Canvas 混合渲染 |
| 影像查看器 | Cornerstone.js 或 OHIF Viewer | 专业医学影像 DICOM 支持 |
| 代码字体 | JetBrains Mono / IBM Plex Mono | 数字显示效果最佳 |
| 图标 | Lucide Icons（线性风格） | 线条图标 + 1.3px stroke 宽度 |
| 状态管理 | Pinia | 轻量 |
| 路由 | Vue Router | 标配 |
| HTTP | Axios | 标配 |

---

## 二、全局布局框架 / Layout Framework

### 2.1 整体布局结构

```
┌──────────────────────────────────────────────────────────┐
│ Chrome Bar (28px) — 三点 + URL + 状态                      │
├──────────┬───────────────────────────────────────────────┤
│          │ Header (约 50px)                                │
│          │  ├── Breadcrumb (mono, 9px, --td)              │
│          │  ├── Page Title (18px) + LIVE badge            │
│          │  └── Action Buttons (右对齐)                    │
│  Sidebar │─────────────────────────────────────────────── │
│  (180px) │ Content Area (flex: 1)                         │
│          │  ├── KPI Row (可选)                             │
│          │  ├── Main Content (flex: 1, grid/flex 布局)     │
│          │  └── Bottom Bar (时间轴/分页, 可选)              │
│          │                                                 │
└──────────┴───────────────────────────────────────────────┘
```

### 2.2 Chrome 顶栏

- 高度 28px，背景 `rgba(3,8,18,0.95)`
- 左侧三个圆点（7px），第三个点为绿色发光（`--emerald`），表示系统在线
- 中间 URL 文字：monospace, 9px, `--cyan`, letter-spacing 0.08em
- 右侧状态文字：monospace, 9px, `--emerald`，前带脉动绿点

### 2.3 侧边栏

- 宽度：180px（PC 端），可折叠至 60px（图标模式）
- 背景：`rgba(3,8,18,0.7)` + `backdrop-filter: blur(20px)`
- 右边框：0.5px `--ln`

**品牌区域**（顶部）：

- Logo Mark：26×26px，外层旋转 45° 的正方形边框（`--cyan`，带辉光），内层渐变填充（cyan → violet）
- 品牌名："CariesGuard"，12px, 500, `--tp`
- 副标题："NEURAL V2"，monospace, 8px, `--cyan`, letter-spacing 0.2em
- 底部 0.5px 分割线

**导航分类标签**：

- monospace, 8.5px, `--td`, letter-spacing 0.2em, 全大写
- 前置菱形装饰点（`--cyan`，3px 旋转 45°）
- 后接渐隐线 `linear-gradient(90deg, rgba(0,229,255,0.2), transparent)`

**导航项**：

- padding: 7px 10px，font-size: 12px, color: `--ts`
- 圆角 6px，hover 时 `background: rgba(0,229,255,0.05)`
- 激活状态：渐变背景 + 左侧 2px 竖线（`--cyan` + 辉光）+ 图标变为 `--cyan`
- 复核项带橙色徽章（pending 数量）

**比赛模式下的菜单结构**：

```
AI CORE
  ├── 影像扫描      (F1)
  ├── 分析队列      (F2)
  ├── 医生复核      (F3, 带 badge)
  └── 智能解释      (F4)

INTELLIGENCE
  ├── AI 评估看板
  ├── RAG 轨迹
  └── 知识图库
```

隐藏的菜单（比赛模式下不显示）：用户管理、角色管理、菜单管理、字典管理、配置管理、随访管理、报告模板、报告导出、通用 Dashboard。

### 2.4 KPI 卡片行

- 4–5 列等宽 grid，间距 8px
- 每张卡片：`rgba(10,20,40,0.5)` + `backdrop-filter: blur(8px)` + 0.5px `--ln` 边框
- 圆角 6px
- 右上角切角装饰（10×10px，仅上+右边框，`--cyan`, opacity 0.4）
- 内部结构（从上到下）：标签（monospace, 8px, `--td`, 全大写）→ 数值（monospace, 18–26px, 带辉光的语义色）→ 趋势（monospace, 8px, emerald/amber）
- 右下角嵌入迷你 sparkline SVG（42×16px, opacity 0.7, 同色描边 + drop-shadow）

---

## 三、八大核心页面设计 / Page-by-Page Specification

---

### Page 01: 登录页 / Neural Console Entry

**路由**: `/login`

**布局**: 全屏深空背景，无侧边栏，内容居中分左右两栏。

**左栏（品牌区）**：

- Logo Mark 放大到 40×40px，外加旋转轨道环（dashed 圆形, `--cyan`, opacity 0.3, 20s 匀速旋转）
- 品牌名 18px + 副标题 "NEURAL CONSOLE V2"
- 系统状态条："SYSTEM ONLINE · ALL CHECKS PASSED"（emerald，带脉动点）
- 主标题使用渐变文字（`--tp` → `--cyan-soft` → `--violet`），字号 26px，两行
- 副标题 11px, `--ts`, 描述系统核心能力
- 底部三个指标（UPTIME 99.2% / MODEL v1.0 / ONLINE 24/7），用分割线隔开

**右栏（登录卡）**：

- 玻璃拟态面板：`rgba(10,20,40,0.5)` + `backdrop-filter: blur(20px)` + 0.5px `--cyan` 20% 边框
- 四角切角装饰（8×8px L 形 `--cyan` 边框）
- 面板阴影：`0 20px 40px rgba(0,0,0,0.4)`
- 标题 "Secure Access"（14px, 500），左侧 3px cyan 竖条
- 副标题 "AUTHENTICATION REQUIRED"（monospace, 8px, `--td`）
- 用户名输入框：深色底 `rgba(3,8,18,0.7)` + 0.5px `--ln` 边框，圆角 4px，左侧用户图标
- 密码输入框：同上，激活态（focus）边框变 `--cyan` + 2px 外发光 + 闪烁光标
- 记住登录复选框 + 忘记密码链接（monospace 文字）
- 登录按钮：渐变底色 `rgba(0,229,255,0.3) → rgba(139,92,246,0.25)`，`--cyan` 边框，16px 圆角，`0 0 16px` 辉光，monospace 全大写文字 + 箭头
- 底部 session ID 和安全状态（monospace, 8px）

**背景特效**：

- 5 个浮动粒子（3px 圆形，不同颜色 cyan/violet/emerald/magenta），4s 浮动动画（上下移动 12px + 缩放）
- 三层径向渐变光晕（cyan 20% / violet 80% / emerald 中心）+ blur(40px)
- 网格纹理（36px 间距）用 radial mask 渐隐到边缘

---

### Page 02: 影像分析详情页 / Neural Diagnostic Console

**路由**: `/analysis/:taskId`

**这是答辩时评委停留最长的页面，也是整个系统的视觉核心。**

**布局**: 侧边栏 180px + 主内容区。主内容区内部为：Header → KPI 行(4 列) → 三栏分割（1.45fr + 0.9fr + 0.85fr）→ 底部时间轴条。

**三栏内容**：

**左栏：影像扫描器 (Panoramic X-Ray Scanner)**

- 面板标题带 `--cyan` 竖条，右侧显示工具切换条（OVR / DET / MSK / HT），激活项用 `--cyan` 底色 + 辉光
- 影像区域：深色底 `radial-gradient(ellipse at center, #0A1428, #030812)`
- 叠加效果层（按 Z 轴从低到高）：
  1. 扫描网格（16px 间距，cyan 3% 不透明度线条）
  2. 牙齿轮廓（8 个 div，每个用 border-radius 模拟牙形 + radial-gradient 模拟 X 光灰度）
  3. Uncertainty 热力图（2 个 blur(8px) 的径向渐变色块，mix-blend-mode: screen）
  4. 检测目标框（四角 L 形切角装饰，amber/magenta 颜色，顶部标签显示分级和置信度）
  5. 扫描线动画（从 10% 到 85% 循环，cyan 水平线 + 辉光）
  6. HUD 叠加芯片（左上角：NEURAL ACTIVE + 检出警告，monospace + 脉动点）
  7. 坐标读数（左下角：X/Y/ZOOM，monospace, `--cyan-soft`）
  8. 图例面板（右下角：玻璃拟态底 + 色块说明）

**中栏：AI 诊断面板 (AI Diagnostic Report)**

- 面板标题带 `--violet` 竖条
- 径向仪表盘：SVG 绘制，80×80px，圆环用 `stroke-dasharray` 控制填充比例，描边渐变（cyan → amber → magenta），中心显示分级字母 + "GRADE" 标签
- 右侧显示三行键值对（LESIONS / MAX GRADE / ACTION），monospace
- Uncertainty 区块：amber 底色边框面板，顶部标签 + 0.72 数值（amber 辉光），渐变进度条（cyan → amber → magenta），阈值刻度 Θ 0.35，触发原因说明文字

**右栏：RAG 智能解释 (Knowledge RAG Response)**

- 面板标题带 `--violet` 竖条
- 引用文本块：左侧 2px violet 边框 + 浅 violet 底色，正文中嵌入 citation 上标标签（violet 底色 + 边框，monospace 8px）
- 来源列表：每条来源一行，序号用 violet 底色小方块，右侧显示页码

**底部时间轴**：

- 单行 6 节点横条：影像上传 → 任务创建 → 神经推理 → RAG 解释 → 医生复核 → 反馈留痕
- 已完成节点：cyan 实心圆 + 对勾 + 辉光
- 当前节点：amber 空心圆 + 脉动内圆 + 辉光环
- 未来节点：灰色空心圆，弱化文字
- 连接线：完成部分用 cyan，当前到未来用渐隐
- 四角切角装饰框

---

### Page 03: AI 评估看板 / Intelligence Overview

**路由**: `/dashboard/ai`

**布局**: 侧边栏 + 主内容区。主内容区：Header（含时间段选择器 24H/7D/30D/ALL）→ KPI 行(5 列) → 2×3 grid 面板布局。

**六个面板**（2 行 × 3 列）：

| 位置 | 面板名称 | 标题色 | 内容 |
|------|---------|--------|------|
| 左上 | Uncertainty Distribution | amber | SVG 面积图，Y 轴=频次，X 轴=0.0–1.0，标注阈值线 Θ=0.35（amber 虚线），峰值点标注 |
| 中上 | Confusion Matrix | emerald | 4×4 格子（G0–G3），对角线颜色最深（emerald 70–80% 不透明度），非对角线按偏差递减不透明度，底部显示 ACCURACY 和 F1 |
| 右上 | Model Capability | violet | 五轴雷达图（DETECT / GRADE / SPEED / SEGM / UNC），v1.0 用 cyan 实线 + 填充，v0.9 用 violet 虚线 + 弱填充，底部图例 |
| 左下 | Grading Distribution | cyan | 5 行横向条形图（G0–G4），每行：左侧标签（语义色 + 分级名）→ 右侧数值百分比 → 渐变进度条 + 辉光 |
| 中下 | Weekly Activity Heatmap | cyan | 7×12 热力图格子（7 天 × 12 小时），色块不透明度映射活跃度（0.05–1.0），高活跃度带辉光 |
| 右下 | System Events | amber | 实时日志列表，每行：时间戳（cyan mono）+ 级别徽章（OK/WRN/INF/ERR，不同颜色底）+ 消息内容 + 耗时 |

---

### Page 04: 分析任务队列 / Analysis Pipeline

**路由**: `/analysis`

**布局**: 侧边栏 + 主内容区。主内容区：Header → 筛选/搜索栏 → 数据表格 → 分页条。

**筛选/搜索栏**：

- 搜索框：深色底 + 放大镜图标 + 占位文字 + ⌘K 快捷键标签
- 状态切换 tabs（ALL / DONE / RUNNING / REVIEW / FAILED），每个 tab 后面带数量，激活态 cyan 底色

**数据表格**：

- 表头：`rgba(3,8,18,0.7)` 深色底，monospace, 8px, `--ts`, 全大写, letter-spacing 0.12em
- 列：TASK ID / PATIENT / GRADE / UNCERTAINTY / STATUS / CREATED / DURATION / ACTIONS
- 每行左侧 2px 彩色状态条：
  - DONE = `--emerald`
  - RUNNING = `--cyan`（带进度条显示百分比）
  - REVIEW = `--amber`
  - FAILED = `--magenta`
  - QUEUED = `--violet`
- TASK ID：monospace, 10px, `--cyan`
- GRADE 徽章：不同颜色底（G0=emerald, G1=cyan, G2=amber, G3=magenta, G4=violet）
- Uncertainty 迷你进度条（4px 高，颜色由数值决定）
- 状态芯片：对应颜色底 + 边框 + 脉动点
- 操作按钮：20×20px 图标按钮组，hover 变 cyan

**分页条**：

- 左侧显示 "SHOWING 1-9 OF 2,847 · SORT BY CREATED ↓"
- 右侧数字分页按钮，激活页用 cyan 底色 + 辉光

---

### Page 05: 医生复核工作台 / Review Workbench

**路由**: `/review/:taskId`

**布局**: 侧边栏 180px + 复核队列 200px + 主工作区。三栏布局。

**复核队列（第二栏）**：

- 标题 "REVIEW QUEUE"（amber mono）+ pending 计数
- 队列卡片：深色底 + 0.5px 边框，显示 Task ID、患者信息、分级徽章、UC 数值
- 激活卡片：amber 边框 + 辉光 + 左侧 2px amber 竖条

**主工作区分为左右两个面板**：

**左面板：AI 标注影像 + 医生编辑**

- 与 Page 02 相同的影像查看器
- 额外增加：**医生修正框**（绿色虚线 1.5px, `--emerald`）覆盖在 AI 原始框（amber）之上
- AI 原始框降低到 50% 不透明度
- HUD 叠加显示 "AI ORIGINAL · 2 LESIONS" + "DOCTOR · 1 MODIFIED"

**右面板：医生修正面板**

- **AI vs Doctor 对比卡**：两列对比布局，AI 侧（amber 数值 G3）→ 箭头 → 医生侧（emerald 数值 G2）
- **分级修正选择器**：5 个选项横排（G0–G4），医生选中的用 emerald + 对勾徽章，AI 原始的用 amber 弱边框标注
- **修正原因标签组**：可多选的标签按钮（"病灶范围过估" / "边界误判" / "影像伪影" / "分级错误" / "深度判断偏差" / "其他"），选中态用 cyan
- **临床备注文本框**：深色底 + focus 时 cyan 边框，带闪烁光标
- **操作按钮行（三列）**：Save Draft（灰色）/ Request 2nd Opinion（magenta）/ Submit & Log（emerald 渐变 + 辉光）

---

### Page 06: 智能解释 / RAG Knowledge Console

**路由**: `/rag`

**布局**: 侧边栏 + 主内容区。主内容区分为三栏：左侧病例上下文 + 中间对话/问答区 + 右侧引用追溯面板。

**左栏：病例上下文（240px）**

- 患者信息卡（ID、年龄、性别、就诊日期）
- 病例摘要卡（主诉、影像描述、AI 分级结果）
- 影像缩略图（小尺寸，点击可放大）

**中栏：对话区**

- 模式切换：患者版解释 / 医生版问答
- 对话气泡设计：
  - 用户消息（右侧）：`rgba(0,229,255,0.1)` 底色 + cyan 边框
  - AI 回复（左侧）：`rgba(139,92,246,0.05)` 底色 + violet 左边框
  - 回复中嵌入 citation 上标（`[1]` `[2]`，violet 底色小标签）
  - Safety flags 提示条（amber 底色，"AI 建议仅供参考，请遵医嘱"）
- 输入框：底部固定，深色底 + 发送按钮

**右栏：引用追溯（260px）**

- 当前回复引用的 chunk 列表
- 每条 chunk 卡片：序号 + 来源文档名 + 页码 + chunk 片段预览
- 底部显示：知识库版本号、最后更新时间、文档数量
- 置信度指标和 safety flag 汇总

---

### Page 07: 知识图库管理 / Knowledge Repository

**路由**: `/knowledge`

**布局**: 侧边栏 + 主内容区。主内容区：Header → 统计卡片行 → 文档列表表格。

**统计卡片行（4 列）**：

- 文档总数（violet）、总 chunks 数（cyan）、最后索引时间（emerald）、知识版本号（amber）

**文档列表表格**：

- 列：文档名 / 类型 / 状态 / Chunks / 版本 / 上传时间 / 操作
- 文档类型标签（指南/手册/论文/病例集，不同 violet 深浅）
- 状态：INDEXED（emerald）/ INDEXING（cyan + 进度）/ PENDING（amber）/ ERROR（magenta）
- 操作：查看详情 / 重建索引 / 删除

**上传区域**：

- 拖拽上传区域：虚线边框（`--violet` 20%），中心图标 + "拖拽文件到此处或点击上传"
- 支持 PDF / DOCX / TXT 格式标注

---

### Page 08: 病例与影像管理 / Case & Image Portal

**路由**: `/cases`

**布局**: 侧边栏 + 主内容区。主内容区：Header → 搜索/筛选 → 病例卡片网格。

**病例卡片（grid 布局, 3 列）**：

- 每张卡片：深色面板底 + 0.5px 边框
- 顶部：影像缩略图（暗色 X 光风格，16:9 比例）
- 中部：患者 ID + 年龄性别 + 就诊时间
- 底部：分析状态（最新一次 task 的状态芯片）+ 分级徽章 + uncertainty 迷你条
- 点击卡片进入影像分析详情页

**新建病例按钮**：

- 触发抽屉/弹窗（玻璃拟态面板）
- 表单字段：患者 ID / 年龄 / 性别 / 主诉 / 影像上传
- 影像上传区域：虚线边框 + 拖拽支持

---

## 四、核心组件规范 / Component Library

### 4.1 状态芯片 / Status Chip

```
┌──●──RUNNING──┐     ← monospace 8px, 全大写, letter-spacing 0.1em
└──────────────┘
```

- 脉动点（4px 圆，同色 + box-shadow）
- 背景：语义色 10% 不透明度
- 边框：语义色 30% 不透明度
- 圆角 2px（HUD 风格）

五种状态：DONE(emerald) / RUNNING(cyan) / REVIEW(amber) / FAILED(magenta) / QUEUED(violet)

### 4.2 Uncertainty 进度条

```
┌───LOW──────────────────Θ 0.35──────────────HIGH───┐
│ ████████████████████████████████░░░░░░░░░░░░░░░░░ │  ← 6px 高
└──────────────────────────────────────────────────── ┘
```

- 底色：`rgba(0,0,0,0.3)` + 0.5px `--ln` 边框
- 填充：渐变（cyan → amber → magenta），宽度对应数值百分比
- 末端脉冲点：2px `--amber` + box-shadow
- 上方行：左标签（monospace, amber, 全大写）+ 右数值（monospace, 16px, amber + 辉光）
- 下方行：LOW / Θ 0.35 / HIGH 刻度标签（monospace, 8px, `--td`）

### 4.3 检测目标框 / Detection Reticle

```
┌─┐                 ┌─┐
│                     │
│    (target area)    │   ← 1px 语义色边框
│                     │
└─┘                 └─┘
```

- 四角 L 形装饰（5×5px 边框，同色）
- 顶部标签栏：语义色底色，monospace 7.5px, 显示 "G3 · CONF 0.72"
- amber = 中度，magenta = 高危
- 医生修正框：1.5px 虚线，emerald 色

### 4.4 KPI 卡片

```
┌────────────────────────────────────┐
│ ● SCANS TODAY                   /┐│  ← 右上切角装饰
│ 128                          ╱╱╱ │  ← sparkline
│ ▲ +12%                           │
└────────────────────────────────────┘
```

- 切角装饰：10×10px，仅 top + right 边框，`--cyan`, opacity 0.4
- 标签行：脉动点 + monospace 全大写标签
- 数值行：monospace 18–26px + 辉光
- 趋势行：上升用 emerald ▲，下降用 amber ▼
- sparkline：SVG polyline，同色描边 + drop-shadow

### 4.5 面板 / Panel

```
┌──┃ Title                    meta ─┐
│  ┃                                │  ← 0.5px 边框
│  content                          │
│                                   │
└───────────────────────────────────┘
```

- 标题栏：8px padding，深色底 `rgba(3,8,18,0.5)`，0.5px 底边框
- 标题左侧 3px 竖条（宽 3px × 高 11px），颜色对应面板语义
- 内容区：10–14px padding
- 整体：`rgba(10,20,40,0.5)` + `backdrop-filter: blur(10px)` + 0.5px `--ln` 边框 + 6px 圆角

### 4.6 按钮 / Button

三种变体：

| 变体 | 背景 | 边框 | 文字色 | 使用场景 |
|------|------|------|--------|---------|
| Ghost | `rgba(0,229,255,0.05)` | 0.5px cyan 25% | `--cyan-soft` | 次要操作（Export / Refresh） |
| Primary | cyan→violet 渐变 20% | 0.5px `--cyan` | `--tp` | 主操作（Submit Review / New Task） |
| Danger | magenta 10% | 0.5px magenta 35% | `--magenta` | 危险操作（Reject / Delete） |
| Success | emerald→cyan 渐变 25% | 0.5px `--emerald` | `--tp` | 确认操作（Confirm & Next） |

所有按钮：monospace, 9–10px, 500, 全大写, letter-spacing 0.08em, padding 6px 12px, 圆角 3px。

Primary 按钮额外加：`box-shadow: 0 0 14px rgba(0,229,255,0.25)`。

### 4.7 时间轴 / Timeline

```
──●────●────●────●────◉────○──
  ✓    ✓    ✓    ✓   ▶    ·
 上传  创建  推理  RAG  复核 反馈
14:02 14:02 14:02 14:02 LIVE  —
```

- 连接线：1px，完成段用 cyan，当前→未来用渐隐
- 完成节点：16–24px 圆, cyan 实心 + 辉光 + 白色对勾
- 当前节点：空心圆, amber 边框 + 3px amber 外环 + 内部脉动点
- 未来节点：空心圆, 弱边框, 灰色文字
- 四角切角装饰框包裹整条时间轴

### 4.8 HUD 芯片 / HUD Chip

```
┌──●──NEURAL ACTIVE──┐
└────────────────────┘
```

- 背景：`rgba(3,8,18,0.75)` + `backdrop-filter: blur(8px)`
- 边框：0.5px 语义色 25%
- 文字：monospace, 7.5px, 语义色, letter-spacing 0.08em
- 左侧脉动点（4px, 同色 + box-shadow）
- 用于影像查看器叠加层

### 4.9 Citation 标签

```
 text content [1] more text [2]
```

- 上标定位（`vertical-align: super`）
- 背景：violet 20% 不透明度
- 边框：0.5px violet 40%
- 文字：monospace, 7–8px, `--violet`
- margin-left: 2px

### 4.10 Grade 徽章

| 分级 | 背景色 | 文字色 |
|------|--------|--------|
| G0 | `rgba(0,255,163,0.15)` | `--emerald` |
| G1 | `rgba(0,229,255,0.15)` | `--cyan` |
| G2 | `rgba(255,181,71,0.15)` | `--amber` |
| G3 | `rgba(255,61,127,0.15)` | `--magenta` |
| G4 | `rgba(139,92,246,0.15)` | `--violet` |

monospace, 10px, 500, padding 2px 6px, 圆角 2px。

---

## 五、交互规范 / Interaction Patterns

### 5.1 页面过渡

- 页面切换使用 `opacity: 0 → 1` + `translateY(8px) → 0` 的淡入上移动画，200ms ease-out
- 避免 slide 等过于花哨的过渡

### 5.2 Loading 状态

- 全页 loading：中心放大版 logo mark + 脉动光晕 + "NEURAL ENGINE INITIALIZING..." 文字
- 局部 loading：面板内显示骨架屏（深色矩形块 + shimmer 动画，shimmer 用 `linear-gradient(90deg, transparent, rgba(0,229,255,0.05), transparent)` 横移）

### 5.3 Toast 通知

- 右上角弹出，玻璃拟态底 + 语义色左边框
- 自动 3s 消失，支持手动关闭
- 四种类型：success(emerald) / warning(amber) / error(magenta) / info(cyan)

### 5.4 空状态

- 居中 SVG 线条图标（64px, `--td`）
- 下方主文字（14px, `--ts`）+ 副文字（12px, `--td`）
- 操作按钮（Ghost 风格）

### 5.5 快捷键

- `F1`–`F4`: 快速切换 AI CORE 四个页面
- `⌘K` / `Ctrl+K`: 全局搜索
- `Esc`: 关闭弹窗/返回

---

## 六、响应式适配 / Responsive Strategy

| 断点 | 宽度 | 侧边栏 | 内容布局 | 说明 |
|------|------|--------|---------|------|
| Desktop XL | ≥ 1440px | 180px 展开 | 三栏 | 最佳展示效果 |
| Desktop | 1280–1440px | 180px 展开 | 三栏压缩 | 常规笔记本 |
| Desktop SM | 1024–1280px | 60px 折叠（仅图标） | 两栏 | 小屏笔记本 |
| Tablet | < 1024px | 抽屉式隐藏 | 单栏 | 不推荐答辩使用 |

答辩推荐分辨率：**1440 × 900** 或 **1920 × 1080**。

---

## 七、ECharts 深色主题配置 / Chart Theme

```javascript
const cariesGuardTheme = {
  backgroundColor: 'transparent',
  textStyle: { fontFamily: '"SF Mono", Consolas, monospace', color: '#8BA3C7' },
  title: { textStyle: { color: '#E4F0FF', fontSize: 12, fontWeight: 500 } },
  legend: { textStyle: { color: '#8BA3C7', fontSize: 10 } },
  tooltip: {
    backgroundColor: 'rgba(10,20,40,0.9)',
    borderColor: 'rgba(0,229,255,0.25)',
    textStyle: { color: '#E4F0FF', fontFamily: '"SF Mono", Consolas, monospace', fontSize: 11 }
  },
  xAxis: {
    axisLine: { lineStyle: { color: 'rgba(100,200,255,0.15)' } },
    splitLine: { lineStyle: { color: 'rgba(100,200,255,0.06)' } },
    axisLabel: { color: '#4A5F82', fontSize: 9, fontFamily: '"SF Mono", monospace' }
  },
  yAxis: {
    axisLine: { lineStyle: { color: 'rgba(100,200,255,0.15)' } },
    splitLine: { lineStyle: { color: 'rgba(100,200,255,0.06)' } },
    axisLabel: { color: '#4A5F82', fontSize: 9, fontFamily: '"SF Mono", monospace' }
  },
  color: ['#00E5FF', '#8B5CF6', '#FFB547', '#FF3D7F', '#00FFA3'],
  series: {
    line: {
      smooth: true,
      lineStyle: { width: 1.5 },
      areaStyle: { opacity: 0.15 },
      symbol: 'circle', symbolSize: 4
    }
  }
};
```

---

## 八、文件与目录结构建议 / Recommended Project Structure

```
frontend/
├── public/
│   └── favicon.svg                    # Logo mark SVG
├── src/
│   ├── assets/
│   │   ├── styles/
│   │   │   ├── _variables.css         # CSS 变量（色彩、间距、圆角）
│   │   │   ├── _animations.css        # 全局动效
│   │   │   ├── _base.css              # 重置与基础样式
│   │   │   ├── _hud.css               # HUD 特效组件
│   │   │   └── _layout.css            # 布局框架
│   │   └── fonts/
│   │       └── JetBrainsMono.woff2
│   ├── components/
│   │   ├── layout/
│   │   │   ├── AppSidebar.vue         # 侧边栏
│   │   │   ├── ChromeBar.vue          # 顶部状态栏
│   │   │   └── MainLayout.vue         # 主布局框架
│   │   ├── shared/
│   │   │   ├── KpiCard.vue            # KPI 卡片
│   │   │   ├── StatusChip.vue         # 状态芯片
│   │   │   ├── GradeBadge.vue         # 分级徽章
│   │   │   ├── UncertaintyBar.vue     # Uncertainty 进度条
│   │   │   ├── Timeline.vue           # 时间轴
│   │   │   ├── HudChip.vue            # HUD 叠加芯片
│   │   │   ├── DetectionReticle.vue   # 检测目标框
│   │   │   ├── CitationTag.vue        # RAG 引用标签
│   │   │   ├── Panel.vue              # 通用面板
│   │   │   ├── NeuralButton.vue       # 按钮
│   │   │   └── ScanlineOverlay.vue    # 扫描线动画
│   │   └── charts/
│   │       ├── AreaChart.vue           # 面积图
│   │       ├── RadarChart.vue          # 雷达图
│   │       ├── ConfusionMatrix.vue     # 混淆矩阵
│   │       └── ActivityHeatmap.vue     # 活跃度热力图
│   ├── views/
│   │   ├── LoginView.vue              # Page 01
│   │   ├── AnalysisDetailView.vue     # Page 02
│   │   ├── DashboardView.vue          # Page 03
│   │   ├── TaskQueueView.vue          # Page 04
│   │   ├── ReviewWorkbenchView.vue    # Page 05
│   │   ├── RagConsoleView.vue         # Page 06
│   │   ├── KnowledgeRepoView.vue      # Page 07
│   │   └── CasePortalView.vue         # Page 08
│   ├── composables/
│   │   ├── useTheme.ts                # 主题管理
│   │   ├── useAnalysis.ts             # 分析任务逻辑
│   │   └── useRAG.ts                  # RAG 问答逻辑
│   ├── stores/
│   │   ├── auth.ts                    # 认证
│   │   ├── analysis.ts                # 分析任务
│   │   └── rag.ts                     # RAG
│   ├── router/
│   │   └── index.ts
│   ├── api/
│   │   ├── auth.ts
│   │   ├── analysis.ts
│   │   ├── review.ts
│   │   ├── rag.ts
│   │   └── dashboard.ts
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

---

## 九、答辩演示路径建议 / Demo Flow

推荐按以下顺序展示，每页停留 1–2 分钟：

1. **登录页** → 展示视觉冲击力和品牌感
2. **病例与影像管理** → 快速展示数据入口
3. **影像分析详情页** → 核心页面，展示扫描线、检测框、uncertainty、RAG 引用、时间轴闭环
4. **分析任务队列** → 展示批量处理能力和状态管理
5. **医生复核工作台** → 展示 AI vs Doctor 对比、修正留痕、反馈闭环
6. **智能解释 RAG** → 展示知识引用、安全约束
7. **AI 评估看板** → 展示量化证据（混淆矩阵、雷达图、一致率）
8. **知识图库** → 展示知识治理能力

---

## 十、设计禁忌清单 / Anti-Patterns

| 禁止做的 | 原因 |
|---------|------|
| 使用 Ant Design / Element UI 默认主题 | 瞬间变成"若依系统" |
| 白色背景 + 蓝色按钮 | 与所有后台系统雷同 |
| 全圆角设计（12px+） | 消费级 App 风格，与医疗精密感冲突 |
| 彩虹色配色 | 色彩无语义，评委无法建立信息映射 |
| 大面积空白 | 浪费答辩展示面积 |
| 纯文字列表展示 AI 结果 | 没有视觉化就没有说服力 |
| 中文全角标点用于 UI 标签 | HUD 风格使用英文标签 + monospace |
| 使用 emoji 图标 | 科技系统使用 SVG 线条图标 |

---

*Document version: v2.0 · Generated for CariesGuard Neural Console V2*  
*Design system: Holographic Command Console · Deep Space + HUD + Glassmorphism*
