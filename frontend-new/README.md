# DentAI · 智齿 — Vue 3 Frontend

AI-assisted caries diagnostic system, built with **Vue 3 + TypeScript + Vite + Naive UI**.

## 快速开始

```bash
cd dentai-vue
npm install
npm run dev
```

访问 http://localhost:5173

## 构建

```bash
npm run build        # 类型检查 + 打包到 dist/
npm run preview      # 本地预览构建结果
npm run type-check   # 仅类型检查
```

## 项目结构

```
dentai-vue/
├── index.html                # Vite 入口
├── vite.config.ts            # Vite 配置 (含 @/ 路径别名)
├── tsconfig.*.json           # TypeScript 配置
├── package.json
└── src/
    ├── main.ts               # 应用入口：挂载 pinia / router / i18n
    ├── App.vue               # 根组件 (Naive UI ConfigProvider)
    ├── theme.ts              # Naive UI 主题覆盖 (品牌色)
    ├── i18n/                 # 中英双语字典 (vue-i18n)
    ├── router/               # vue-router 配置 (hash 模式)
    ├── styles/
    │   ├── tokens.css        # 设计 tokens (色板/间距/阴影)
    │   └── app.css           # 全局样式
    ├── components/
    │   ├── AppIcon.vue       # SVG 图标集
    │   └── ToothImage.vue    # 合成牙列 SVG 渲染
    ├── layouts/
    │   └── AppShell.vue      # 左侧导航 + 顶栏 + <RouterView />
    └── pages/
        ├── LoginPage.vue     # 登录 (粒子鼠标动效)
        ├── HomePage.vue      # 工作台首页
        ├── AnalyzePage.vue   # AI 分析核心页 (扫描动效 + 病灶标注)
        ├── ReportPage.vue    # 诊断报告详情
        ├── LibraryPage.vue   # 病例库
        └── SettingsPage.vue  # 设置 (占位)
```

## 技术栈

| 模块           | 选型             | 说明                        |
|----------------|------------------|-----------------------------|
| 构建           | Vite 5           | 极速 HMR                    |
| 框架           | Vue 3.5          | 组合式 API / `<script setup>` |
| 类型           | TypeScript 5.6   | 严格模式                    |
| UI 组件库      | Naive UI 2.38    | 主题与品牌色深度集成        |
| 状态           | Pinia 2          | 已接入，目前页面无共享状态  |
| 路由           | vue-router 4     | Hash 模式，便于部署到任意路径 |
| 国际化         | vue-i18n 10      | 中 / EN 切换，写入 localStorage |

## 路由

| 路径            | 页面          |
|-----------------|---------------|
| `/#/login`      | 登录          |
| `/#/app/home`   | 工作台        |
| `/#/app/analyze`| AI 分析 (核心) |
| `/#/app/report` | 诊断报告      |
| `/#/app/library`| 病例库        |
| `/#/app/settings`| 设置         |

## 下一步建议 (对接真实后端)

1. 将 `AnalyzePage.vue` 顶部的 `LESIONS` 常量替换为接口返回，由 Pinia store 管理
2. `ToothImage` 组件替换为 `<img :src="caseImage" />`，或基于 Cornerstone.js 渲染 DICOM
3. 在 `src/api/` 下建立 axios 封装；登录后写入 token
4. `setLang` 与 Pinia 结合，统一用户偏好

## 浏览器支持

Chrome / Edge / Firefox / Safari 最近 2 版。用到了 `backdrop-filter`、CSS Grid、`oklch` 等现代特性。
