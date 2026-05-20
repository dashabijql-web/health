# 矿山工人健康安全管理系统 — 技术文档

> 版本：v1.1 | 最近更新：2026-05-10 | 适用人员：前后端开发、运维、新入职工程师

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [项目结构](#3-项目结构)
4. [开发环境搭建](#4-开发环境搭建)
5. [系统架构](#5-系统架构)
6. [前端模块说明](#6-前端模块说明)
7. [路由与权限](#7-路由与权限)
8. [状态管理（Vuex）](#8-状态管理vuex)
9. [HTTP 请求封装](#9-http-请求封装)
10. [API 接口说明](#10-api-接口说明)
11. [数据库结构](#11-数据库结构)
12. [关键业务逻辑](#12-关键业务逻辑)
13. [ECharts 图表使用规范](#13-echarts-图表使用规范)
14. [常见问题与已知坑点](#14-常见问题与已知坑点)
15. [部署说明](#15-部署说明)

---

## 1. 项目概述

本系统面向矿山企业，实现对井下工人健康状态的实时监测与安全管控。

**核心功能：**

| 模块 | 描述 |
|------|------|
| 安全指挥中心 | 大屏实时展示井下人员分布、SOS/跌倒告警、部门安全排行 |
| 健康监测 | 统一管控、实时监控、心率/血氧/睡眠分析、风险预警 |
| 人员管理 | 员工档案、健康画像评分 |
| 组织管理 | 部门、岗位类型管理 |
| 设备管理 | 手表设备列表、绑定/解绑 |
| 告警管理 | 告警记录查询、预警规则配置 |
| 统计分析 | 月度健康报告、部门报表、雷达图 |
| 系统管理 | 用户管理、角色权限管理 |

---

## 2. 技术栈

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.x | 前端框架（Composition API） |
| Vite | 5.x | 构建工具（替代 Webpack，开发极速） |
| Vuex | 4.x | 全局状态管理（用户信息、路由权限） |
| Vue Router | 4.x | 单页应用路由（Hash 模式） |
| Element Plus | 2.x | UI 组件库（表格、表单、弹窗等） |
| ECharts | 5.x | 图表库（折线图、柱状图、雷达图、矩形树图等） |
| Axios | 1.x | HTTP 请求（已封装，见 `src/utils/request.js`） |
| SCSS | - | CSS 预处理器 |
| NProgress | - | 路由切换进度条 |
| dayjs | - | 日期时间处理 |

### 后端（参考）

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.12 | 后端框架 |
| MyBatis-Plus | 3.x | ORM 框架 |
| Sa-Token | - | 认证授权（Token 存储于 Cookie+Header） |
| Redis | 5.0.14 | 实时数据缓存（手表数据 Buffer） |
| Netty | - | TCP 服务器（接收手表设备数据） |
| SQL Server | 2019 | 主数据库 |

---

## 3. 项目结构

```
D:\HealthShow\                      # 前端项目根目录
├── src/
│   ├── api/                        # API 请求函数（按业务模块分文件）
│   │   ├── alert-config.js         # 告警规则配置 API
│   │   ├── blood-oxygen.js         # 血氧数据 API
│   │   ├── department.js           # 部门管理 API
│   │   ├── device.js               # 设备管理 API
│   │   ├── employee.js             # 员工管理 API
│   │   ├── health.js               # 健康记录 API（主数据）
│   │   ├── health-portrait.js      # 健康画像 API
│   │   ├── heart-rate.js           # 心率数据 API
│   │   ├── job-type.js             # 岗位类型 API
│   │   ├── position.js             # 职位 API
│   │   ├── realtime.js             # 实时监控 API（Redis 实时数据）
│   │   ├── risk-warning.js         # 风险预警 API
│   │   ├── role.js                 # 角色管理 API
│   │   ├── shift-group.js          # 班组 API
│   │   ├── sleep.js                # 睡眠数据 API
│   │   ├── statistics.js           # 统计分析 API
│   │   └── user.js                 # 用户认证 API（登录/退出/获取信息）
│   │
│   ├── assets/                     # 静态资源
│   │   ├── css/                    # 全局 CSS
│   │   ├── font/                   # 字体文件
│   │   └── images/                 # 图片资源
│   │
│   ├── components/                 # 全局通用组件
│   │   ├── AnimatedNumber.vue      # 数字滚动动画组件
│   │   ├── HeartRateWave.vue       # 心电波形 Canvas 组件
│   │   ├── MinerModel3D.vue        # 矿工 3D 模型展示组件
│   │   ├── ParticleBackground.vue  # 粒子背景特效组件
│   │   └── RadarScan.vue           # 雷达扫描动画组件
│   │
│   ├── layout/                     # 主布局组件
│   │   ├── index.vue               # 布局根组件（侧边栏+顶部+内容区）
│   │   ├── menu/
│   │   │   └── navigation.mjs      # 侧栏/移动底栏导航派生入口
│   │   └── components/
│   │       ├── AppMain.vue         # 内容区（包裹 <router-view>）
│   │       ├── Navbar.vue          # 顶部导航栏
│   │       └── Sidebar/            # 侧边栏菜单
│   │
│   ├── router/                     # 路由配置
│   │   ├── index.js                # 路由入口（汇总 app-routes.mjs）
│   │   ├── app-routes.mjs          # 主应用路由事实源
│   │   ├── health-monitor.mjs      # 健康监测模块路由
│   │   ├── alert-management.mjs    # 预警管理模块路由
│   │   ├── app-route-access.js     # 路由可见性/权限裁剪
│   │
│   ├── store/                      # Vuex 状态管理
│   │   ├── index.js                # Store 根配置
│   │   ├── getters.js              # 全局 Getters（快捷访问）
│   │   └── modules/
│   │       ├── user.js             # 用户模块（token/roles/路由权限过滤）
│   │       └── home.js             # 首页模块（历史遗留，已废弃）
│   │
│   ├── styles/                     # 全局样式
│   │   ├── index.scss              # 样式入口
│   │   ├── dark-admin.scss         # 深色管理台主题（大多数管理页用）
│   │   └── health-monitor.scss     # 健康监测模块样式变量
│   │
│   ├── utils/
│   │   ├── request.js              # Axios 封装（Token 注入、错误处理）
│   │   └── auth.js                 # Cookie 操作（Token 的读/写/删）
│   │
│   ├── views/                      # 页面组件（按模块分目录）
│   │   ├── login/                  # 登录页
│   │   ├── demo-screen/            # 演示大屏（不在菜单中）
│   │   ├── safety-command/         # 安全指挥中心大屏
│   │   │   └── components/         # 子组件（KpiCardRow、EventPanel 等）
│   │   ├── health-monitor/         # 健康监测模块
│   │   │   ├── dashboard/          # 统一管控（index + runtime/chart/view-model/actions/scss）
│   │   │   ├── real-time/          # 实时监控（index + runtime/helpers/components）
│   │   │   ├── heart-rate/         # 心率分析（index + chart/scss + metric-page runtime）
│   │   │   ├── pressure/           # 压力分析（index + chart/scss + metric-page runtime）
│   │   │   ├── blood-pressure/     # 血压分析（index + chart/scss + metric-page runtime）
│   │   │   ├── blood-oxygen/       # 血氧分析（index + chart/scss + metric-page runtime）
│   │   │   ├── sleep/              # 睡眠分析（已隐藏，index + page-state/runtime/view-model/scss）
│   │   │   └── risk-warning/       # 风险预警（index + page-state/runtime/view-model/scss）
│   │   │   ├── employee-archive/    # 员工档案库（index + use-page + runtime/view-model/scss）
│   │   │   ├── employee-profile/    # 员工健康画像（index + use-page + runtime/view-model/scss）
│   │   │   ├── mine-entry/         # 班前准入（index + view-model/scss）
│   │   │   ├── workbench/          # 月度工作台（index + view-model/chart/scss）
│   │   │   ├── report-center/      # 报告中心（index + runtime/chart/view-model/export/scss）
│   │   │   └── trend-warning/      # 趋势预警（index + sparkline/scss）
│   │   ├── device-management/      # 设备管理（index + use-page + runtime/view-model/scss）
│   │   ├── user-list/              # 用户管理（index + use-page + runtime/view-model/scss）
│   │   ├── role-management/        # 角色权限管理（index + use-page + runtime/view-model/scss）
│   │   ├── org-management/         # 组织管理（部门/岗位）
│   │   ├── personnel-management/   # 人员管理（员工/健康画像，health-portrait 已拆 use-page/runtime/chart/export/scss）
│   │   ├── alert-management/       # 告警管理（notifications/records 已拆 scss）
│   │   ├── ai-chat/                # AI 问答（index + chart/export/query/session/text/scss）
│   │   └── statistics/             # 统计分析（月度/部门报表）
│   │
│   ├── heartbeat.js                # 心跳检测（每30s请求/auth/info 保持登录）
│   ├── main.js                     # 应用入口
│   ├── App.vue                     # 根组件
│   └── permission.js               # 路由守卫（权限控制）
│
├── public/
│   ├── index.html                  # HTML 模板
│   └── js/                         # 第三方静态 JS（CDN 替代）
│
├── .env.development                # 开发环境变量
├── .env.staging                    # 测试环境变量
├── .env.production                 # 生产环境变量
├── vite.config.mjs                 # Vite 构建配置（代理、插件、别名）
├── package.json                    # 依赖声明与 npm scripts
└── TECHNICAL_DOCS.md               # 本文档
```

### 3.1 当前前端事实源

- 路由事实源：`src/router/app-routes.mjs` 汇总业务路由，模块路由只保留 `health-monitor.mjs` 和 `alert-management.mjs` 等稳定边界。
- 权限裁剪入口：`src/router/app-route-access.js`，`store/modules/user.js` 只保存用户信息和过滤后的可见路由。
- 侧栏与移动底栏事实源：`src/layout/menu/navigation.mjs`，从同一份路由元数据派生菜单组和移动端入口。
- 回归护栏：`npm run audit:nav` 会检查旧 `.js` 路由文件是否残留、入口是否引用 `.mjs`、可见菜单是否具备 `navGroup/navOrder`。

### 3.2 大页目录模板

已经迁移的大页默认按以下边界组织：

```text
page/
├── index.vue                 # 页面壳：模板、组合关系、少量路由交互
├── use-xxx-page.js           # Composition API 页面组合入口，按需存在
├── xxx-page-state.js         # 默认状态与常量，按需存在
├── xxx-runtime.js            # API、轮询、生命周期、图表实例编排
├── xxx-view-model.js         # 展示模型、字段归一化、状态派生
├── xxx-actions.js            # 交互动作，按需存在
├── xxx-chart.js              # 图表 option 或图表数据，按需存在
├── xxx-export.js             # 导出/打印，按需存在
├── xxx.scss                  # 页面样式
└── components/               # 页面内可复用组件，按需存在
```

当前纳入 `npm run audit:page-structure` 的已迁移页面包括：

- `dashboard`
- `real-time`
- `safety-command`
- `health-portrait`
- `employee-profile`
- `report-center`
- `risk-warning`
- `trend-warning`
- `mine-entry`
- `sleep`
- `device-management`
- `user-list`
- `role-management`
- `alert-notifications`
- `alert-records`
- `ai-chat`
- `heart-rate`
- `blood-oxygen`
- `pressure`
- `blood-pressure`
- `workbench`

当前 `audit:page-structure` 跟踪 `21` 个已迁移页面，`legacyTrackedPages = 0`。

指标页当前第一轮边界：

- `src/views/health-monitor/metric-page/metric-scroll.js` 统一 Top5 滚动循环。
- `src/views/health-monitor/metric-page/metric-export.js` 统一导出空数据提示、文件名日期和成功提示。
- `src/views/health-monitor/metric-page/metric-page-mixin.js` 统一 Top5 滚动启动参数和卸载清理。
- `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 均已拆出同目录 `*-chart.js` 与 `*.scss`。
- `ai-chat` 已拆出 `ai-chat-chart.js`、`ai-chat-export.js`、`ai-chat-query-result.js`、`ai-chat-session.js`、`ai-chat-text.js` 与 `ai-chat.scss`。
- `trend-warning` 已把内联迷你图拆到 `TrendSparkLine.js`，页面样式拆到 `trend-warning.scss`。
- `mine-entry` 已把准入展示判断拆到 `mine-entry-view-model.js`，样式拆到 `mine-entry.scss`。
- `alert-management/notifications` 与 `alert-management/records` 已把样式移入各自 `*.scss`，并纳入结构门禁。
- `alert-management/notifications` 的预警 SLA 展示由 `alert-management/common/warning-lifecycle.js` 统一派生，列表直接显示状态、剩余/超时时长和到期时间；新增或调整 SLA 口径时同步跑 `node --test tests/warning-lifecycle.mjs`。

后续 `G4` 第二轮继续把指标页数据加载、状态派生和模板块抽成 page engine，不再把新逻辑堆回 `index.vue`。

当前大页副作用约束：

- `dashboard`、`real-time`、`report-center`、`health-portrait`、`safety-command` 的页面级 resize / visibility / fullscreen 监听统一通过 `src/utils/task-timer.js` 的 `createEventBinding` 注册和释放。
- 新增页面级 DOM 事件监听时，不要手写 `addEventListener/removeEventListener` 成对逻辑，优先复用 `createEventBinding`。

---

## 4. 开发环境搭建

### 前提条件

- Node.js >= 18.x
- npm >= 9.x（或 yarn/pnpm）
- 后端服务已启动（见下方）

### 安装与启动

```bash
# 1. 进入前端目录
cd D:/HealthShow

# 2. 安装依赖（首次或依赖变更后执行）
npm install

# 3. 启动开发服务器
npm run dev
# 启动后访问：http://localhost:9528/

# 4. 生产构建（打包为静态文件）
npm run build
# 产物目录：dist/
```

### 后端服务（参考，由运维/后端同学操作）

```bash
# 后端目录：D:/HealthData
# Java：C:/Program Files/Java/jdk-17
# Maven：D:/apache-maven-3.8.1

# 启动后端
JAVA_HOME="C:/Program Files/Java/jdk-17" \
  /d/apache-maven-3.8.1/bin/mvn spring-boot:run -f D:/HealthData/pom.xml

# 后端地址：http://localhost:8080/health
# Netty TCP 端口（手表设备）：9000
# Redis：localhost:6379
```

### 环境变量说明

`.env.development`（开发环境）：
```env
VITE_BASE_API=/dev-api          # 前端请求前缀，Vite 代理到后端
VITE_TARGET=http://localhost:8080  # 后端地址（代理目标）
```

`.env.production`（生产环境）：
```env
VITE_BASE_API=/prod-api         # 生产环境通过 Nginx 反代
```

---

## 5. 系统架构

### 整体数据流

```
手表设备
  │ TCP（9000端口）
  ↓
Netty TCP Server
  │ DataProcessService 解析数据包
  ↓
RedisHealthBufferService
  │ push() 写入 Redis List（health:buffer:old / health:buffer:new）
  │ flush() 每5s批量写入数据库
  ↓
SQL Server（health / health_new 双库）
  │ 月度分区表（health_record_202601 等）
  │
  ├─ Redis（realtime:user:{imei}）← 实时数据缓存（前端轮询）
  │
  └─ Spring Boot REST API
       │
       ↓
  Vue 3 前端
  │ 30s 全量轮询 + 5s 告警快轮询（安全指挥中心）
  │ 页面实时显示健康数据
  ↓
用户浏览器
```

### 5.1 运维与观测入口

- 后端健康检查：`http://localhost:8080/health/actuator/health`
- 后端指标入口：`http://localhost:8080/health/actuator/metrics`
- 当前关键指标族：
  - `health.ai.call.duration`
  - `health.ai.reject.total`
  - `health.ai.sql.auto_repair.total`
  - `health.buffer.queue.size`
  - `health.buffer.push.total`
  - `health.buffer.flush.total`
  - `health.datasource.request.total`
  - `health.datasource.watch.route.total`
  - `health.watch.online.count`
  - `health.warning.generated.total`
  - `health.warning.dedup.total`
  - `health.sql.statement.duration`
  - `health.sql.slow.total`

### 关键设计决策

| 决策 | 原因 |
|------|------|
| Hash 路由（`#/`） | 无需 Nginx 额外配置，内网部署友好 |
| Redis 实时缓存 | 手表数据上报频率高，直接写 SQL 会有性能瓶颈 |
| 月度分区表 | 健康记录量大，分区查询性能好（health_record_202601~） |
| Sa-Token 认证 | 国产框架，与 Spring Boot 3 兼容好，配置简单 |
| 前端轮询（非 WebSocket） | 内网环境稳定，实现简单，不依赖 WebSocket 基础设施 |

---

## 6. 前端模块说明

### 6.1 安全指挥中心（`/safety-command`）

大屏实时监控页面，是系统最核心的展示界面。

**文件：** `src/views/safety-command/index.vue`

**子组件：**
| 组件 | 文件 | 功能 |
|------|------|------|
| KpiCardRow | `components/KpiCardRow.vue` | 顶部5个 KPI 卡片（井下人数/SOS/跌倒/告警/手表） |
| EventPanel | `components/EventPanel.vue` | 实时事件列表 + 7天趋势折线图 |
| RiskPersonPanel | `components/RiskPersonPanel.vue` | 高危人员列表 |
| AreaMapGrid | `components/AreaMapGrid.vue` | 井下区域分布（网格视图 / 矩形树图） |
| DeptRankTable | `components/DeptRankTable.vue` | 部门安全排行榜 |
| EventHandleDialog | `components/EventHandleDialog.vue` | 事件处理弹窗 |
| PersonDetailDrawer | `components/PersonDetailDrawer.vue` | 人员详情抽屉（实时体征+7天趋势） |

**轮询策略：**
- `fetchCritical()`：每 **5秒** 刷新 SOS/跌倒等紧急告警
- `fetchAllData()`：每 **30秒** 刷新所有数据（部门、设备状态等）

**安全评分公式：**
```
安全评分 = 100 - SOS条数×15 - 跌倒条数×10 - 静止条数×3 - 异常条数×5
评分阈值：≥90=优秀(绿) | ≥80=良好(蓝) | ≥60=警惕(黄) | <60=危险(红)
```

### 6.2 健康监测（`/health-monitor`）

#### 统一管控（dashboard）
数据总览大屏，显示当月健康检测统计、体征均值、预警趋势、部门 TOP5 等。
使用 Options API（Vue 2 风格），历史代码，后续可逐步迁移到 Composition API。

#### 实时监控（real-time）
显示当前在线员工列表，支持按部门筛选、按姓名搜索。
数据来自 Redis 实时缓存，每30s自动刷新。
表格列：员工编号/姓名/部门/心率/血氧/体温/步数/睡眠/最后更新时间/告警状态。

#### 心率分析（heart-rate）
- 左侧：概况卡片、心率分布饼图、异常 TOP5 列表
- 右侧：7天趋势折线图、24小时波形（今日各小时均值）
- 右下：异常记录表格（分页）

#### 血氧分析（blood-oxygen）
结构与心率分析相同，数据来源不同。
血氧预警阈值：< 95% 预警，< 90% 危险。

#### 睡眠分析（sleep）
当前已从菜单隐藏（`hidden: true`）。
原因：手表设备仅在矿井下（4G内网）使用，员工上井后设备归还，无法采集睡眠数据。
当前结构：`index.vue + sleep-page-state.js + sleep-runtime.js + sleep-view-model.js + sleep.scss`。
`index.vue` 只保留模板、组合模块引用和弹窗全局样式，数据加载、导出、图表渲染和定时刷新在 runtime 中维护。

#### 风险预警（risk-warning）
历史预警记录的统计分析，包含预警趋势、类型分布、部门统计等。
当前结构：`index.vue + risk-warning-page-state.js + risk-warning-runtime.js + risk-warning-view-model.js + risk-warning.scss`。
`index.vue` 只保留模板、组合模块引用和抽屉全局样式；自动滚动使用共享 `createScrollLoop`，resize 回到 `chartPageMixin` 统一管理。

### 6.3 统计分析（`/statistics`）

#### 月度报告（monthly）
按月展示健康统计数据，包含：
- 折线图：每日健康记录数量趋势
- 饼图：预警类型分布（心率/血氧/体温/疲劳）
- 数据表格：每月汇总数据

健康评分公式（月度）：
```
月度健康分 = 85
  + (avg_blood_oxygen - 95) × 3        # 血氧加分
  - (avg_heart_rate > 100 ? avg_heart_rate - 100 : 0)  # 心率扣分
  - (avg_temperature > 37.5 ? (avg_temperature - 37.5) × 10 : 0)  # 体温扣分
范围：0-100，精度：整数
```

#### 部门报表（dept-report）
部门横向对比，雷达图展示各维度评分，表格列出每个部门预警数量和健康分。

健康评分公式（部门）：
```
部门健康分 = 100 - (预警总数 / 员工数) × 3
# 用人均预警率而非绝对数，避免大部门因人多而分数偏低
范围：0-100
```

---

## 7. 路由与权限

### 路由结构

```
/login                              # 登录页（不需要权限）
/404                                # 404 页面
/safety-command/index               # 安全指挥中心
/health-monitor/dashboard           # 统一管控（permCode: health:dashboard）
/health-monitor/real-time           # 实时监控（permCode: health:realtime）
/health-monitor/heart-rate          # 心率分析（permCode: health:heart）
/health-monitor/pressure            # 压力分析（permCode: health:pressure）
/health-monitor/blood-pressure      # 血压分析（permCode: health:bloodpressure）
/health-monitor/blood-oxygen        # 血氧分析（permCode: health:oxygen）
/health-monitor/sleep               # 睡眠分析（隐藏，permCode: health:sleep）
/health-monitor/risk-warning        # 风险预警（permCode: health:risk）
/health-monitor/employee-archive    # 职工健康档案库（permCode: health:employee）
/health-monitor/employee-profile    # 职工健康画像（隐藏）
/health-monitor/health-portrait     # 健康画像兼容入口（隐藏）
/health-monitor/mine-entry          # 入井准入管理（permCode: health:mine-entry）
/health-monitor/report-center       # 报表中心（permCode: health:report）
/health-monitor/trend-warning       # 趋势预警（permCode: health:trend）
/alert-management/notifications     # 消息通知中心（permCode: alert:notifications）
/alert-management/sos               # SOS 紧急救援（permCode: alert:sos）
/alert-management/records           # 告警记录（permCode: alert:records）
/alert-management/config            # 告警配置（permCode: alert:config）
/admin/device-list                  # 设备列表（permCode: device:list）
/admin/user-list                    # 用户列表（permCode: user:list）
/admin/role                         # 角色管理（permCode: role:list）
/admin/department                   # 部门管理（permCode: org:department）
/admin/job-type                     # 工种管理（permCode: org:job-type）
/ai-chat/index                      # AI健康助手
```

当前没有单独维护菜单配置文件。侧栏、移动底栏、面包屑显示都从路由 `meta` 和 `src/layout/menu/navigation.mjs` 派生。

### 权限控制流程

1. 用户登录 → 后端返回 Sa-Token token、角色、权限码等信息
2. `store/modules/user.js` 将 token 写入 `User-Token` Session Cookie，并同步到 Vuex
3. `permission.js` 的路由守卫优先检测 Cookie 中 token；若刷新后 Vuex 为空，则调用 `/auth/info`
4. `getPermittedAppRoutes(data.routes)` 根据后端返回的 `routes[]` 权限码生成可见菜单树
5. 过滤结果存入 Vuex，侧边栏和移动端导航都从同一份路由元数据读取

### 给页面添加/去掉权限控制

**去掉权限（所有人可见）：** 删除路由 meta 中的 `permCode` 字段即可。

**加上权限：** 在路由 meta 中添加 `permCode: 'xxx:yyy'`，并在后端给对应角色分配该权限码。

---

## 8. 状态管理（Vuex）

### Store 结构

```
store/
├── index.js        # 注册所有模块
├── getters.js      # 全局快捷访问（token/name/avatar/sidebar/routes）
└── modules/
    └── user.js     # 用户模块（最重要）
```

### user 模块核心状态

| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | String | Sa-Token token，从 `User-Token` Session Cookie 读取 |
| `name` | String | 用户显示名 |
| `canSwitchDataSource` | Boolean | 当前用户是否允许显式切换“新库 / 老库” |
| `roles` | Array | 角色列表（`roles.length > 0` 也作为“用户信息已恢复”的标志） |
| `routes` | Array | 后端返回的权限码数组 |
| `buttons` | Array | 按钮级权限码 |
| `resultAllRoutes` | Array | 通过 `getPermittedAppRoutes()` 过滤后的可见路由（侧边栏菜单用） |

### 在组件中使用 Store

```javascript
import { useStore } from 'vuex'
const store = useStore()

// 读取
const token = computed(() => store.getters.token)
const userName = computed(() => store.getters.name)

// 触发 Action
await store.dispatch('user/getInfo')
await store.dispatch('user/logout')
```

---

## 9. HTTP 请求封装

所有 HTTP 请求必须通过 `src/utils/request.js` 中封装的 Axios 实例发送。

### 基本用法

```javascript
import request from '@/utils/request'

// GET 请求
const res = await request({ url: '/risk-warning/overview', method: 'get' })
// res.data 即为后端返回的业务数据

// GET 带参数
const res = await request({
  url: '/risk-warning/list',
  method: 'get',
  params: { page: 1, size: 20, handled: false }  // 拼到 URL: ?page=1&size=20&handled=false
})

// POST 请求
const res = await request({
  url: `/risk-warning/handle/${id}`,
  method: 'post',
  data: { handleNote: '已通知班长' }  // 放在请求体中
})
```

### 请求拦截器（自动加 Token）

```
请求前：自动在 Header 中添加 satoken: <token值>
若当前用户具备切库权限，再附加 X-Health-Data-Source: new|old
```

### 响应拦截器（统一错误处理）

| 响应情况 | 处理方式 |
|---------|---------|
| HTTP 200 + code 200 | 返回 res（含 code/message/data） |
| HTTP 200 + code 401 | 静默跳转登录 |
| HTTP 401 | 静默跳转登录 |
| HTTP 500 + 有 Token | 静默跳转登录（后端重启时的 Token 失效） |
| 并发多个 401 | 通过内部锁收敛为一次跳转登录和一次 Cookie 清理 |
| HTTP 502/503 | 弹出"服务暂时不可用"提示 |
| 超时 | 弹出"请求超时"提示 |
| /auth/info 路径失败 | 静默（心跳检测路径，不弹错误） |

### URL 规则

```
开发环境：
  request({ url: '/risk-warning/list' })
  → 实际请求: /dev-api/risk-warning/list
  → Vite 代理重写: /health/risk-warning/list
  → 到达后端: http://localhost:8080/health/risk-warning/list

生产环境：
  /prod-api/risk-warning/list
  → Nginx 反代到后端真实地址
```

---

## 10. API 接口说明

### 10.1 认证相关（`/auth/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/auth/login` | POST | 登录，返回 Token |
| `/auth/logout` | POST | 退出登录 |
| `/auth/info` | GET | 获取当前用户信息（权限码） |

### 10.2 实时数据（`/realtime/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/realtime/overview` | GET | 实时概览（在线人数等） |
| `/realtime/online-users` | GET | 在线用户列表（分页） |
| `/realtime/user/{userCode}` | GET | 单人实时体征 |
| `/realtime/statistics` | GET | 设备统计（在线/离线/低电量） |
| `/realtime/alerts` | GET | 最近未处理告警列表 |

### 10.3 风险预警（`/risk-warning/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/risk-warning/overview` | GET | 预警统计概览（SOS/跌倒/异常数量） |
| `/risk-warning/list` | GET | 预警记录列表（支持分页/筛选） |
| `/risk-warning/trend` | GET | 预警趋势（按天，days=7/30） |
| `/risk-warning/dept-stats` | GET | 各部门预警统计 |
| `/risk-warning/handle/{id}` | POST | 处理单条预警 |
| `/risk-warning/handle-batch` | POST | 批量处理预警 |
| `/risk-warning/type-distribution` | GET | 预警类型分布 |

### 10.4 告警管理（`/alert-config/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/alert-config/list` | GET | 阈值配置列表 |
| `/alert-config/update` | PUT | 更新配置 |
| `/alert-config/toggle/{id}` | PUT | 启用/停用切换 |

### 10.5 健康记录与工作台（`/api/health/...`、`/dashboard/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/health/record/page` | GET | 健康记录分页 |
| `/dashboard/overview` | GET | 当月检测统计 |
| `/dashboard/body-indicators` | GET | 体征均值（心率/血氧/体温/步数） |
| `/dashboard/daily-trend` | GET | 每日异常率趋势 |
| `/dashboard/warning-counts` | GET | 预警按时间统计 |
| `/dashboard/calendar` | GET | 工作台月历 |
| `/dashboard/pre-shift-compliance` | GET | 班前健康达标率 |
| `/dashboard/mine-entry-list` | GET | 今日准入名单 |
| `/dashboard/dept-health-comparison` | GET | 部门健康对比 |

### 10.6 心率与指标分析（`/heart-rate/...`、`/pressure/...`、`/blood-pressure/...`、`/blood-oxygen/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/heart-rate/overview` | GET | 心率统计概览 |
| `/heart-rate/trend` | GET | 历史趋势（days=7） |
| `/heart-rate/hourly` | GET | 今日24小时均值（date=YYYY-MM-DD） |
| `/heart-rate/distribution` | GET | 心率分布（正常/偏快/过快/偏慢） |
| `/heart-rate/abnormal` | GET | 异常记录列表 |
| `/pressure/overview` | GET | 压力统计概览 |
| `/blood-pressure/overview` | GET | 血压统计概览 |
| `/blood-oxygen/overview` | GET | 血氧统计概览 |

### 10.7 AI 能力（`/ai/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/ai/health-report` | GET | 查询已缓存 AI 报告 |
| `/ai/health-report/generate` | POST | 生成个人 AI 报告 |
| `/ai/health-report/mine` | GET | 查询全矿 AI 报告 |
| `/ai/health-report/mine/generate` | POST | 生成全矿 AI 报告 |
| `/ai/health-report/dept` | GET | 查询部门 AI 报告 |
| `/ai/health-report/dept/generate` | POST | 生成部门 AI 报告 |
| `/ai/report/employee` | POST | 生成员工健康诊断报告 |
| `/ai/report/department` | POST | 生成部门健康诊断报告 |
| `/ai/chat` | POST | AI 问答 |

### 10.8 统计分析（`/statistics/...`）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/statistics/monthly` | GET | 月度数据（year/month 参数） |
| `/statistics/dept-health-summary` | GET | 部门健康汇总（预警数+员工数） |
| `/statistics/daily-record-counts` | GET | 每日记录条数 |
| `/statistics/warning-types` | GET | 预警类型分布 |

---

## 11. 数据库结构

### 主要表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `sys_user` | 系统用户（管理员账号） | id, username, password, role_id |
| `sys_role` | 角色表 | id, role_name, permission_codes(JSON) |
| `employee` | 员工档案 | id, emp_code(EMP0001~), emp_name, dept_id, status |
| `department` | 部门表 | id, dept_code(DEPT01~), dept_name, parent_id |
| `job_type` | 岗位类型 | id, job_name |
| `device` | 手表设备 | id, imei(3594567800XXXXX), device_name, status |
| `device_user` | 设备-员工绑定 | id, device_id, emp_id, bind_type(INT), bind_time |
| `health_record` | 健康记录主表（路由到分区表） | - |
| `health_record_YYYYMM` | 月度分区表（共13张） | id, emp_code, dept_id, heart_rate, blood_oxygen, temperature, sleep_minutes, steps, record_time |
| `warning_record` | 预警记录 | id, emp_code, dept_id, warning_type, warning_level, is_handled, create_time |
| `realtime_data` | 实时数据缓存（Redis为主，此表备用） | - |

### 视图

| 视图名 | 说明 |
|--------|------|
| `v_health_record` | 联合所有分区表，自动路由到当月分区 |
| `v_warning_record` | 预警记录视图（含员工/部门关联） |
| `v_current_month_stats` | 当月统计摘要 |
| `v_device_current_user` | 设备当前绑定员工 |

### IMEI 格式

```
3594567800XXXXX（15位）
后5位对应员工编号后4位数字
JOIN 方式：CAST(RIGHT(device.imei, 5) AS BIGINT) = CAST(RIGHT(employee.emp_code, 4) AS BIGINT)
```

### 重要字段注意事项

| 字段 | 存储方式 | 前端处理 |
|------|---------|---------|
| `temperature` | 整数×10（如 367 = 36.7°C） | `t > 100 ? t / 10 : t` |
| `sleep_minutes` | 整数分钟 | `/60` 转换为小时 |
| `record_time` | DATETIME | 格式化显示 |
| `is_handled` | 0/1 | `Boolean` 转换 |

---

## 12. 关键业务逻辑

### 12.1 手表数据写入路径

```
手表 TCP → Netty → DataProcessService.handleData()
  → 解析数据包（IMEI、心率、血氧、体温等）
  → RedisHealthBufferService.push(record)  # 写入 Redis List
  → 每5s: flush() → healthRecordService.batchInsert(records)
  → HealthRecordMapper.insertToTable(tableName, record)  # 写月度分区表
```

> ⚠️ **禁止直接调用：**
> - `healthRecordMapper.insert(record)` ← 不走分区路由
> - `healthRecordService.saveBatch(records)` ← JDBC batch 与 SQL Server getGeneratedKeys() 不兼容

### 12.2 用户认证流程

```
1. 当前双库切换现场的登录页默认预填 `admin / admin123`，保证 `health_new` 空库状态仍有管理员入口
2. 前端 POST /auth/login {username, password}
3. 后端 Sa-Token 验证，返回 { token, roles, routes, buttons, canSwitchDataSource, ... }
4. 前端把 token 写入 `User-Token` Session Cookie，Vuex 只保存当前内存态
5. 页面刷新后，`permission.js` 发现 Cookie 有 token 但 Vuex 无角色信息时，会调用 `/auth/info` 恢复状态
6. 后续请求统一注入 `satoken`；只有有切库权限的用户才会附加 `X-Health-Data-Source`
7. `heartbeat.js` 每 30 秒调用 `/auth/info` 检测会话；Token 失效、后端重启或并发 401 时都会静默收敛到登录页
```

### 12.3 路由权限过滤

```javascript
// store/modules/user.js 中的 filterRoutes 函数
// 输入：constantRoutes（全部路由）+ permCodes（用户权限码）
// 输出：过滤后的可见路由

// 规则：
// 1. hidden=true 的路由始终保留（/login、/404）
// 2. 有 children 的父路由：递归过滤子路由，有可见子路由才保留父路由
// 3. 叶子路由：无 permCode 直接保留，有 permCode 需在权限列表中
```

### 12.4 安全指挥中心双轮询

```javascript
// 两个独立轮询，互不干扰
pollCriticalTimer = setInterval(fetchCritical, 5000)   // 5s：只刷新SOS/跌倒
pollTimer = setInterval(fetchAllData, 30000)            // 30s：刷新所有数据

// fetchCritical 只请求：
//   1. getRiskWarningOverview() - 更新 SOS/跌倒计数
//   2. getRiskWarningList()     - 更新事件列表
```

---

## 13. ECharts 图表使用规范

### 基本模式

```javascript
// 1. 声明 ref 和变量
const chartRef = ref(null)
let chart = null

// 2. 初始化（必须在 DOM 渲染后）
const initChart = () => {
  if (!chartRef.value) return
  if (chart) chart.dispose()  // 先销毁旧实例，防止内存泄漏
  chart = echarts.init(chartRef.value)
  chart.setOption({ /* 配置 */ })
}

// 3. 组件卸载时销毁
onUnmounted(() => {
  if (chart) { chart.dispose(); chart = null }
})

// 4. 窗口大小变化时 resize
window.addEventListener('resize', () => chart?.resize())
// 记得 onUnmounted 时 removeEventListener
```

### 深色主题色彩规范

```scss
// 与项目主题保持一致的颜色变量
$blue:   #1890ff    // 主色（在线人数、正常数据）
$green:  #52c41a    // 安全/正常状态
$yellow: #faad14    // 预警状态
$orange: #ff9800    // 中等危险
$red:    #ff5252    // 危险/SOS

// 背景色
$panel-bg: rgba(14, 33, 60, 0.65)  // 面板背景（毛玻璃效果）
$border:   rgba(24, 144, 255, 0.35) // 边框色
```

### 空数据处理

图表初始化时，如果 API 没有返回数据，使用 ECharts graphic 组件显示"暂无数据"：

```javascript
if (!hasData) {
  chart.setOption({
    graphic: [{
      type: 'text',
      left: 'center',
      top: 'middle',
      style: { text: '暂无数据', fill: 'rgba(255,255,255,0.3)', fontSize: 13 }
    }]
  })
  return
}
```

---

## 14. 常见问题与已知坑点

### Q1：页面出现右侧滚动条，无法一屏显示

**原因：** 页面容器没有限高，子内容撑开了页面高度。

**解决方案：** 在页面根元素加：
```scss
.page-root {
  height: calc(100vh - 50px);  // 50px 是顶部 Navbar 的高度
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
```

### Q2：el-table 内部滚动不生效

**原因：** el-table 的 `height="100%"` 需要父容器有明确高度。

**解决方案：**
```html
<div class="table-wrapper" style="flex:1; min-height:0; overflow:hidden">
  <el-table height="100%" :data="tableData">...</el-table>
</div>
```

### Q3：ECharts 图表显示空白

**常见原因：**
1. 容器 DOM 尚未渲染完成就初始化图表 → 在 `nextTick` 后初始化
2. 容器高度为 0 → 确保容器有明确的 `height` 值
3. 旧实例未销毁就再次初始化 → 先 `dispose()` 再 `init()`

### Q4：后端重启后前端自动跳登录

**这是预期行为。** `request.js` 检测到 HTTP 500 + 有 Token = 后端重启/Token 失效，自动跳转 `/login`。
重新登录即可正常使用。

### Q5：`BindingException: Invalid bound statement`

**原因：** JRebel 热重载不支持向 Mapper 接口新增方法。

**解决方案：** 完整重启后端（而非 JRebel 热重载）。

### Q6：`setInterval` 没有在组件卸载时清除

**正确写法（Composition API）：**
```javascript
let timer = null
onMounted(() => { timer = setInterval(fn, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
```

**正确写法（Options API）：**
```javascript
beforeUnmount() {
  clearInterval(this.timer)
  if (this.chart) this.chart.dispose()
}
```

### Q7：部门报表健康分全部为 0

**原因：** 公式 `100 - warningCount × 5` 当预警数量超过 20 时结果为负数，被 clamp 到 0。

**已修复：** 改为人均预警率公式 `100 - (warningCount / employeeCount) × 3`。

### Q8：菜单中没有显示某个页面

**检查步骤：**
1. 确认路由 meta 中没有 `permCode`，或者确认该 permCode 已分配给当前用户的角色
2. 在后端数据库 `sys_role.permission_codes` 中查看角色的权限码列表
3. 在前端 DevTools → Application → Cookies → `User-Token` → 确认 Token 有效
4. 清除 Cookie 重新登录

### Q9：sqlcmd 插入中文失败

**解决方案：**
1. SQL 文件保存为 **UTF-16** 编码（非 UTF-8）
2. 命令加 `-I` 参数：`sqlcmd -S "..." -E -d health -I -i file.sql`

### Q10：如何跑写回归和实时链路探针

**推荐命令：**
```bash
npm run audit:ci
npm run audit:structure
npm run audit:page-structure
npm run audit:nightly
npm run audit:api
npm run audit:write
npm run audit:pipeline
npm run audit:pipeline-warning
npm run audit:e2e
```

`audit:write` 会自动：
1. 回写一个预警处理并恢复原状态
2. 更新一条告警配置并恢复原值
3. 向 TCP `9000` 发送最小手表协议探针，等待探针健康记录从 SQL 被 API 和实时接口读回，并清理探针健康记录
4. 生成一份员工 AI 报告并清理缓存行

`audit:pipeline` 会额外跑一条更重的端到端链路：
1. 向 TCP `9000` 发送真实手表协议探针（`AP00 + AP03 + APHP`）
2. 观察 Redis `health:buffer:old` / `health:buffer:new`（兼容回看旧 `health:buffer`）确实出现探针 payload
3. 等待定时 flush 写入当月 `health_record_YYYYMM`
4. 验证 `/api/health/record/page`、`/health-portrait/{empCode}`、`/realtime/user/{userCode}`
5. 用 Playwright 打开 `employee-profile` 页面确认体征卡片
6. 自动删除探针 SQL 行并清理残留 Redis payload

`audit:pipeline-warning` 则专门覆盖异常链路：
1. 向 TCP `9000` 发送高温综合探针（`AP00 + APHP`，体温超出当前高危阈值）
2. 等待 `warning_record_YYYYMM` 写入新的 `体温异常 / 高危` 预警
3. 验证 `/risk-warning/list` 能返回该预警
4. 用 Playwright 打开 `/alert-management/notifications`，筛到该员工并点一次“处理”
5. 回查 SQL 确认该预警已标记处理
6. 自动删除探针产生的健康记录、预警记录和残留 Redis payload

注意：`audit:pipeline` 会短暂写入和回收真实本地数据，因此默认不并入 `audit:all`。
`audit:structure` 会连续执行 `audit:nav` 和 `audit:page-structure`，用于检查路由/菜单事实源和已迁移大页目录边界。
`audit:ci` 只跑云端安全的构建检查，供 GitHub Actions 使用。
`audit:nightly` 跑完整本地回归，适合自托管 runner 或手动夜间执行。
其余 `audit:*` 依赖本地后端、SQL Server、Redis、TCP 9000 或浏览器环境，仍属于本地回归。
nightly workflow 假设 runner 主机已先行启动后端、Redis、SQL Server 和 TCP 9000 模拟器；没有这些基础设施时只会跑 CI-safe 流程。
`audit:auth` 会在“stale token after backend restart”步骤里主动重启后端，因此不要和 `audit:e2e` / `audit:pipeline` 并行跑，否则很容易把后两者打脏。

---

## 15. 部署说明

### 开发环境

前端：`npm run dev` → `http://localhost:9528`
后端：Maven Spring Boot → `http://localhost:8080/health`
Redis：本地默认端口 6379
数据库：SQL Server 2019，实例 `R9000K3080\MSSQLSERVER2019`

### 生产环境（Nginx 示例）

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件（npm run build 的产物）
    root /var/www/health-show/dist;
    index index.html;

    # SPA 路由：所有路径都返回 index.html（Hash 模式不需要此配置，但保留无妨）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反代后端 API
    location /prod-api/ {
        proxy_pass http://localhost:8080/health/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

> **注意：** 本项目使用 Hash 路由（`/#/`），Nginx 不需要特殊配置即可正常工作。
> 如果将来改为 History 模式，需要加上 `try_files` 配置。

### 生产环境变量

修改 `.env.production`：
```env
VITE_BASE_API=/prod-api
# VITE_TARGET 在生产构建中不使用（由 Nginx 反代，不需要 Vite 代理）
```

---

*文档持续更新，如有疑问请联系项目负责人。*
