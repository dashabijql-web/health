# HealthShow 技术债清单

日期：`2026-05-05`  
范围：`D:/Health/HealthShow`

这份文档不是泛泛而谈的“代码规范建议”，而是基于当前真实代码状态整理的技术债清单，目标是：

- 识别当前最影响稳定性和迭代效率的结构问题
- 按 `P0 / P1 / P2` 排优先级
- 给出“建议怎么改”以及“不要怎么改”

## 总体判断

`HealthShow` 当前不是“彻底失控”的项目，但已经明显进入：

- 高复杂业务
- 多轮重构叠加
- 局部补丁式修复增多
- 结构健康度下降

也就是说：

- 还能持续开发
- 但继续只堆功能、不收结构，后面每次改动成本都会更高

## 当前进展（2026-05-05）

- 路由入口链已收口到静态注册，`main.js` / `router/index.js` / `permission.js` / `user.js` 不再互相抢路由生命周期。
- `AppMain` 已去掉多余的外层 key，保留更轻的单层路由切换策略；同时把 `employee-profile`、`health-portrait`、`records`、`device-management` 的 query 同步补上了。
- 页面级轮询已开始标准化，`employee-profile`、`health-portrait`、`device-management`、`mine-entry` 已接入 `useIntervalTask`，减少各页重复写 `setInterval/clearInterval`。
- 继续补了两处零散 timer：`HealthTips` 和 `safety-command/components/DeptRankTable` 也已改为统一 hook 管理，减少手写 `setInterval/setTimeout`。
- `real-time` 和 `dashboard` 的基础刷新/时钟/重绘 timer 已改成统一 `createIntervalTask / createTimeoutTask` 管理，隐藏标签页时也会一并停掉。
- `dashboard` 又继续拆出了一层图表 option / 弹窗 option / 视图构造层，`dashboard-chart-options.js`、`dashboard-summary.js`、`dashboard-view-model.js` 现在已经承接部门、人次、预警、环境、仪表盘和一部分纯展示逻辑。
- `dashboard` 继续把时钟、自动刷新、KPI 刷新、resize 和列表滚动切到统一 task/scroll hook，并清掉了已经失效的旧 timer 字段与旧滚动方法；当前主文件已降到约 `2171` 行。
- 本轮拆分过程中，`dashboard/index.vue` 一度因为文本改写出现编码损坏；当前已经先恢复到最近一次已提交的干净版本，再把缓存和纯视图派生逻辑重新接回 helper。
- 这意味着 `dashboard` 当前仍然不是“页面壳”，而是“已恢复可运行 + 已重新接回一部分 helper”的状态；后续还要继续把 `runtime / detail / chart` 这三层按小步方式接回。
- `real-time` 的在线用户拉取、自动滚动、消息/语音发送和状态格式化方法已外提到 `realtime-runtime.js`，主文件已从约 `1050` 行降到约 `891` 行。
- `employee-profile` 的打印延迟已经改成 `useTimeoutTask`，当前主文件约 `999` 行。
- 菜单结构和测试环境开始标准化：`src/layout/menu/navigation.mjs` 集中管理分组，`src/router/*.mjs` 路由定义也统一成 ESM，`tests/preflight.mjs` 可以先检查前端、后端、Playwright 和 TCP `9000`，`audit:nav` 会在构建前校验菜单/路由结构。
- `dashboard` 继续拆出纯展示/配置层：`DashboardDispatchPanel.vue`、`dashboard-summary.js`、`dashboard-chart-options.js` 已承接一部分展示模型和图表 option，页面本体不再直接背全部视觉配置。
- 入口层的“新手教程式长注释”已收短，`main.js / router/index.js / permission.js / user.js` 只保留和当前机制一致的说明，减少注释漂移和无效 diff。
- 路由静态注册、根入口 redirect、404 返回和登录恢复流程已视为正式机制，不再混用“动态补注入 + 多层兜底”的旧路径。
- 当前的验证基线已经补齐到 `audit:api / audit:write / audit:pipeline / audit:pipeline-warning / audit:e2e / build` 全绿。
- 当前技术债里最重的仍然是巨石页面和分散轮询，其次才是菜单树和文档漂移。

## 快速体量感知

当前几个代表性大文件：

- [dashboard/index.vue](/D:/Health/HealthShow/src/views/health-monitor/dashboard/index.vue)：`2171` 行
- [real-time/index.vue](/D:/Health/HealthShow/src/views/health-monitor/real-time/index.vue)：`891` 行
- [heart-rate/index.vue](/D:/Health/HealthShow/src/views/health-monitor/heart-rate/index.vue)：`1163` 行
- [employee-profile/index.vue](/D:/Health/HealthShow/src/views/health-monitor/employee-profile/index.vue)：`999` 行
- [router/index.js](/D:/Health/HealthShow/src/router/index.js)：`168` 行
- [permission.js](/D:/Health/HealthShow/src/permission.js)：`116` 行
- [main.js](/D:/Health/HealthShow/src/main.js)：`90` 行
- [Sidebar/index.vue](/D:/Health/HealthShow/src/layout/components/Sidebar/index.vue)：`149` 行

这些数字本身不说明一切，但已经足够说明：

- 页面层有明显“巨石组件”问题
- 路由与入口层有明显“职责缠绕”问题

---

## P0：必须尽快处理

### 1. 路由入口链职责缠绕

涉及文件：

- [main.js](/D:/Health/HealthShow/src/main.js)
- [router/index.js](/D:/Health/HealthShow/src/router/index.js)
- [permission.js](/D:/Health/HealthShow/src/permission.js)
- [store/modules/user.js](/D:/Health/HealthShow/src/store/modules/user.js)

#### 现状

现在入口逻辑分散在多处：

- `main.js` 在启动时预注入业务路由
- `router/index.js` 管根入口、常量路由、延迟加载业务路由
- `permission.js` 管登录态放行、补注入路由、补拉用户信息
- `user.js` 的 `getInfo()` 又会触发路由加载和权限过滤

这意味着：

- 多个层级都在“懂一点路由初始化”
- 出问题时很难一眼看出哪层是真正的入口控制者

#### 真实风险

- 根入口问题再次复发
- 登录首跳竞态
- 直达书签页不稳定
- 某次局部优化再次把 `#/404` 这类问题带回来

#### 建议改法

目标是把职责拆清：

- `main.js`
  - 只负责应用启动和基础依赖安装
- `router/index.js`
  - 只负责路由定义和业务路由注册器
- `permission.js`
  - 只负责导航守卫，不再承担额外的“补初始化”
- `user.js`
  - 只负责用户信息和权限数据，不顺便控制路由生命周期

最好形成一个明确规则：

- 业务路由什么时候注册：唯一入口
- 用户信息什么时候拉取：唯一入口
- 根路由怎么分发：唯一入口

#### 不要怎么改

- 不要继续在多个文件里各补一层兜底逻辑
- 不要为了“稳一点”同时保留两三套初始化路径
- 不要把新的入口判断继续塞进 `beforeEach`

---

### 2. `AppMain` 路由承载层可能导致“像整页刷新”

涉及文件：

- [AppMain.vue](/D:/Health/HealthShow/src/layout/components/AppMain.vue)

#### 现状

当前写法：

- `router-view :key="$route.matched[0]?.path"`
- 内层 `<component :is="Component" :key="route.path" />`
- 外加 `fade-page` 切换动画

#### 风险

这很容易带来两类问题：

- 页面切换时整块内容区被强制重建
- 用户体感上像“网页刷新一下”

尤其在大屏页、图表页、带轮询页上会更明显，因为它们的 mounted / init / fetch 成本都高。

#### 建议改法

先明确需求，再决定 key 策略：

- 哪些一级路由必须重建
- 哪些只需要普通切换
- 哪些需要 `keep-alive`

更稳的方向一般是：

- 尽量减少 `router-view` 上的强制 key
- 不要同时对 `router-view` 和 `component` 双重加重建 key
- 动画层和生命周期控制层不要混在一起承担“修空白页”的责任

#### 不要怎么改

- 不要直接把所有 key 都删掉就算完
- 不要一边保留动画，一边继续靠 key 强制解决空白页
- 不要在未验证之前就把所有页面都放进 `keep-alive`

---

### 3. 巨石页面已经影响维护

重点文件：

- [dashboard/index.vue](/D:/Health/HealthShow/src/views/health-monitor/dashboard/index.vue)
- [real-time/index.vue](/D:/Health/HealthShow/src/views/health-monitor/real-time/index.vue)
- [heart-rate/index.vue](/D:/Health/HealthShow/src/views/health-monitor/heart-rate/index.vue)
- [employee-profile/index.vue](/D:/Health/HealthShow/src/views/health-monitor/employee-profile/index.vue)

#### 现状

单文件同时承载：

- API 请求
- 数据整形
- 轮询逻辑
- 图表初始化
- 页面交互
- DOM/滚动控制
- 样式
- 业务口径判断

#### 风险

- 一改就容易带出旁路问题
- 新人几乎无法快速定位
- 重复逻辑难以抽离
- 生命周期副作用很难彻底清理

#### 建议改法

不是“把一个 3000 行文件拆成 10 个小垃圾文件”，而是按职责拆：

- `page-shell`
- `data-loader`
- `chart-panel`
- `summary-card`
- `alert-list`
- `composables / utils`

建议顺序：

1. 先拆 `dashboard`
2. 再拆 `real-time`
3. 再处理几个分析页的共性

#### 不要怎么改

- 不要只做视觉拆分，不拆逻辑
- 不要把同样复杂度从一个文件搬到另一个文件
- 不要在没有测试兜底时一口气大拆所有页面

---

## P1：应该尽快安排

### 4. 菜单树不是稳定配置，而是运行时重组结果

涉及文件：

- [Sidebar/index.vue](/D:/Health/HealthShow/src/layout/components/Sidebar/index.vue)
- [router/health-monitor.mjs](/D:/Health/HealthShow/src/router/health-monitor.mjs)
- [router/alert-management.mjs](/D:/Health/HealthShow/src/router/alert-management.mjs)
- [router/app-routes.mjs](/D:/Health/HealthShow/src/router/app-routes.mjs)

#### 现状

现在侧边栏不是直接消费一个明确菜单树，而是：

1. 从权限路由里取叶子节点
2. 按 `navGroup` 再分组
3. 重新组出展示菜单

#### 风险

- 菜单问题很难排查
- 页面新增时容易漏 `navGroup`
- 路由和菜单的边界变得模糊

#### 建议改法

保留“路由驱动菜单”的大方向，但至少要做到：

- 菜单分组配置独立
- 菜单树构造逻辑集中
- 明确哪些路由是导航路由，哪些是隐藏业务页

#### 不要怎么改

- 不要再往 `Sidebar` 里继续塞更多派生逻辑
- 不要把更多 UI 状态判断耦进去

---

### 5. 页面级轮询和定时器过于分散

涉及范围很多，包括：

- `dashboard`
- `real-time`
- `heart-rate`
- `employee-profile`
- `notifications`
- `safety-command`
- `heartbeat`

#### 现状

当前项目里存在大量：

- `setInterval`
- `setTimeout`
- 自动滚动 timer
- 刷新 timer
- 轮询 timer

它们分散在不同页面、不同 mixin、不同组件中。

#### 风险

- 切页时清理不彻底
- 页面闪动、重复请求、性能波动
- 某些页面挂久了以后更容易出问题

#### 建议改法

逐步统一：

- 页面数据轮询
- 页面滚动轮询
- 心跳轮询

至少要把“创建/销毁/恢复”规则整理成一致模式。

#### 不要怎么改

- 不要把所有 timer 一把梭地抽成一个全局大对象
- 不要先做抽象再想业务，先归类真实模式

---

### 6. 测试链变强了，但环境一致性还不够

涉及文件：

- [package.json](/D:/Health/HealthShow/package.json)
- [tests/e2e/playwright-audit.mjs](/D:/Health/HealthShow/tests/e2e/playwright-audit.mjs)

#### 现状

现在有：

- `audit:api`
- `audit:write`
- `audit:e2e`
- `audit:pipeline`
- `audit:pipeline-warning`

但本机环境已经再次暴露：

- 仓库 `node_modules` 里没有标准 `playwright`
- 某些回归依赖“机器上刚好还有可用环境”

#### 风险

- 同样的命令在不同机器不一定能跑
- CI 和本地行为可能继续漂移

#### 建议改法

- 前端仓库明确补齐 E2E 依赖
- 把“脚本能跑的前提”写进文档
- 减少对外部临时环境的隐式依赖

#### 不要怎么改

- 不要继续靠“本机能跑就算过”
- 不要让测试链越来越复杂，但前置条件越来越隐蔽

---

## P2：中期优化

### 7. 代码注释很多，但不是都在帮忙

#### 现状

项目里有不少“新手必读”式长注释。  
它们有价值，但也带来两个问题：

- 文件变更噪音大
- 注释容易和真实代码漂移

#### 建议改法

- 核心机制保留解释
- 明显重复、过时、和代码不同步的长注释逐步收掉

#### 不要怎么改

- 不要一次性删光
- 不要把注释收完后完全失去上下文

---

### 8. 页面视觉系统与逻辑系统耦合偏重

#### 现状

很多页面同时在组件里管：

- 视觉布局
- 业务状态
- 图表口径
- 滚动节奏
- 自动刷新

这会让 UI 调整很容易误伤业务逻辑。

#### 建议改法

- 把视觉容器和业务容器分层
- 图表组件尽量只接收干净数据

---

### 9. 一些“能跑就行”的兼容补丁已经开始沉淀

#### 现状

比如：

- 启动时预注入业务路由
- 登录后补拉用户信息再跳
- 404 页面兜底返回

这些当前都合理，但如果继续叠，就会越来越像补丁系统。

#### 建议改法

对每个补丁都问一句：

- 它是临时兜底
- 还是已经应该升级成正式机制

---

## 当前最值得做的顺序

### 第一阶段

1. 收路由入口链
2. 收 `AppMain` 的切换/重建策略
3. 确认菜单点击“像刷新”的真实机制

### 第二阶段

1. 拆 `dashboard`
2. 拆 `real-time`
3. 收共性图表/轮询逻辑

### 第三阶段

1. 收菜单树结构
2. 收测试环境标准化
3. 收文档和注释漂移

---

## 哪些地方先别碰

- 先别继续做零碎包体优化
- 先别重写 AI / 报表导出边缘功能
- 先别一口气重构整套侧边栏
- 先别在没有测试兜底时拆所有大页面

---

## 最后结论

`HealthShow` 当前最大的技术债，不是“代码风格不好”，而是：

- 入口链复杂
- 页面过大
- 轮询分散
- 菜单树派生过多
- 一些补丁已经开始制度化

如果继续只加功能不收结构，最先出问题的通常不会是“某个图不好看”，而是：

- 入口不稳定
- 菜单切换体感差
- 页面偶发闪动/重建
- 新需求越来越难改

所以后续最值得投的，不是再加一个新模块，而是先把 `P0` 这几项收一轮。
