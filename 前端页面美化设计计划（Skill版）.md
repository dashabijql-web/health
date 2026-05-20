# 前端页面美化设计计划（Skill版）

## 文档定位

本文档用于把当前会话可用的 `design-taste-frontend` skill，翻译成 `HealthShow` 可直接执行的前端美化计划。

说明：

- 当前前端仓库技术栈是 `Vue 3 + Vite + SCSS + Element Plus`
- `design-taste-frontend` 中涉及 `React / Next.js / Tailwind / Framer Motion` 的实现方式不直接照搬，只保留可迁移的设计约束和性能护栏
- 用户提到的 `superpowers` skill 不在当前会话可用列表内，因此本文档不假设它的专属流程，只用现有 skill、代码事实和视觉审计产物制定计划
- 本机已存在 `superpowers` 插件目录：`/home/j/.codex/.tmp/plugins/plugins/superpowers/skills/`
- 本文额外吸收其中两条对当前任务最有价值的约束：
  - `writing-plans`：先锁文件职责，再写阶段计划
  - `verification-before-completion`：所有“已完成 / 已通过 / 已稳定”结论必须先跑新鲜验证命令
- 本文档是“执行计划”，不是纯审美意见；结论必须能映射到页面、组件、样式入口和验收命令

## 一、当前代码事实

### 1. 已有公共资产

当前仓库已经有可复用基础，不应该再从零造一套新壳层：

- 共享样式 token 已存在于 `HealthShow/src/styles/index.scss`
  - 颜色：`--bg-*`、`--border-*`、`--accent-*`、`--text-*`
  - 字体：`--font-display`、`--font-body`、`--font-mono`
  - 结构：`--radius-*`、`--space-*`、`--shadow-*`、`--panel-header-height`
- 健康监测共享样式入口已存在于 `HealthShow/src/styles/health-monitor.scss`
- 共享壳组件已存在：
  - `HealthShow/src/components/health-shell/PageHeroHeader.vue`
  - `HealthShow/src/components/health-shell/MetricStrip.vue`
  - `HealthShow/src/components/health-shell/PanelShell.vue`
  - `HealthShow/src/components/health-shell/PageEmptyState.vue`
- 移动端底栏已切为正式图标体系：
  - `HealthShow/src/layout/components/MobileBottomNav.vue`
  - `HealthShow/src/layout/menu/navigation.mjs`
- 预警工作线已存在共享导航：
  - `HealthShow/src/components/WarningCenterNav.vue`

结论：

- 第二轮不是“新建全套视觉系统”，而是“收口已有系统，清理旧风格残留，补齐缺失状态和工具条能力”。

### 2. 当前仍然明显分散的地方

从现有样式和页面代码看，下面这些问题还在拖慢统一设计：

- 多个页面仍然直接写页面级字体栈，典型包括：
  - `real-time.scss`
  - `heart-rate.scss`
  - `blood-pressure.scss`
  - `trend-warning.scss`
- 多个指标页仍保留旧“发光 + 霓虹 + 紫色”表达，和当前全局 token 方向不一致，典型是：
  - `blood-pressure.scss` 仍有紫色主视觉
  - `heart-rate.scss`、`real-time.scss` 仍有较重 `text-shadow / glow`
- 个别页面仍有内联样式和局部硬编码颜色，典型是：
  - `dashboard/components/DashboardDialogs.vue`
  - `dashboard/components/DashboardWarningStream.vue`
- 共享壳组件虽已存在，但不同页面还没有形成同一套 header / toolbar / empty / status 的稳定口径

结论：

- 当前最大欠账不是“缺设计灵感”，而是“共享资产存在但没有强制收口”。

### 3. 按 superpowers 补齐后的计划硬约束

为避免文档只停留在“方向正确、执行模糊”，本文后续统一按下面三条写法收口：

- 先锁文件责任：
  - 每个阶段先给出实际要改的文件，而不是只写抽象目标
- 再写阶段动作：
  - 每个阶段至少说明共享层、页面层、样式层各自负责什么
- 最后写验证口径：
  - 任何“本阶段完成”结论，都必须能对应到具体命令和最新产物

## 二、Skill 约束如何映射到本仓库

### 1. 基线拨盘

按 `design-taste-frontend` 的默认值，当前项目沿用以下基线：

- `DESIGN_VARIANCE = 8`
- `MOTION_INTENSITY = 6`
- `VISUAL_DENSITY = 4`

翻译到本项目：

- 页面结构允许不完全对称，但必须有明确阅读路径
- 动效只做层级反馈、状态反馈和切换反馈，不做演示型持续特效
- 保留监测产品所需的数据密度，但不给页面堆过多面板和边框

### 2. 必须继承的设计规则

- 单主强调色，不允许同一页面同时漂移青蓝、紫、粉、橙多套主风格
- Dashboard 类页面禁止回到“门户标题 + 控制台数字 + 老后台正文”混搭
- 卡片只在需要表达层级时使用，不能把信息分组全部做成发光面板
- 移动端优先保证标题、tabs、按钮、底栏安全区稳定
- 所有页面必须统一 `loading / empty / error / disabled / active / handled / pending`
- 不再使用 emoji 作为正式导航、状态或关键操作图标

### 3. 直接禁用的反模式

- 紫色主风格回流
- 大面积霓虹描边或持续发光
- 纯靠大字号堆压迫感
- 三列平均卡片机械平铺
- 在滚动容器上叠重型滤镜
- 用 `top / left / width / height` 做动画
- 为了“高级感”牺牲信息密度和操作效率

## 三、当前问题不是审美抽象，而是页面优先级失衡

本轮判断以 `HealthShow/tests/visual/artifacts/` 最新一轮 `layout-summary.md` 为准，不把时间戳文件名写死进规则。

按最近视觉审计结果，当前最核心的问题有两类：

- `text_overlap`
  - 主要集中在移动端
  - 典型页面：`dashboard`、`risk-warning`、`real-time`、`safety-command`、`alert-notifications`、`alert-records`、`report-center`、`ai-chat`
- `chart_container_too_small`
  - 当前主要集中在 `dashboard`

这说明当前最优先的问题不是颜色不够高级，而是：

1. 移动端头部层级失控
2. Cockpit 页和 Workbench 页仍混用旧表达
3. 已有公共组件未形成统一落地模式
4. 局部旧样式仍在抢主视觉

## 四、页面分线必须更明确

### 1. Cockpit 线

适用页面：

- `dashboard`
- `real-time`
- `safety-command`
- `risk-warning`

目标气质：

- 冷静
- 锐利
- 状态优先
- 读路径明确

页面规则：

- 首屏结构固定为 `身份 -> 当前状态 -> 核心 KPI -> 主体面板`
- 桌面端允许非对称分栏，但必须能一眼看出主次
- 移动端首屏只能保留标题、关键状态、主操作和少量 KPI
- 图表、排行、告警、应急动作之间必须有清晰顺位
- 允许轻量 `transform + opacity` 反馈，不允许演示型特效堆叠

### 2. Workbench 线

适用页面：

- `alert-management/notifications`
- `alert-management/records`
- `health-monitor/report-center`
- `ai-chat`
- 后续再扩到 `workbench`、`employee-*`、`mine-entry`、`admin`

目标气质：

- 易读
- 易扫
- 易操作
- 长时间使用不压迫

页面规则：

- 标题区、筛选区、操作区、主内容区必须强制分层
- 工具条优先于装饰性副标题
- 列表、表单、导出、详情是主体，不再套 Cockpit 式强压迫外壳
- 移动端优先保留筛选入口、主操作、列表内容和底部输入安全区

## 五、优先级排序

### P0：系统级止血

先做直接影响多页稳定性的公共问题：

1. `PageHeroHeader` 移动端优先级收口
2. `WarningCenterNav` 移动端导航收口
3. `MobileBottomNav` 安全区和遮挡关系复核
4. 页面级字体、发光、紫色旧风格清理规则

### P1：Cockpit 线样板页

优先顺序：

1. `dashboard`
2. `real-time`
3. `safety-command`
4. `risk-warning`

原因：

- `dashboard` 既有移动端头部问题，也有桌面图表容器问题，且它是整条 Cockpit 线的样板页
- `real-time` 和 `safety-command` 目前仍带较多旧风格负担
- `risk-warning` 的问题更偏头部和导航，可在共享层收口后跟进

### P2：Workbench 线样板页

优先顺序：

1. `alert-management/notifications`
2. `alert-management/records`
3. `health-monitor/report-center`
4. `ai-chat`

原因：

- 这四页都在最近视觉审计中暴露移动端头部或工具区拥挤问题
- 它们共同决定 Workbench 线最终语言，不应继续各页自行分叉

## 六、文件责任图（superpowers 版）

### 1. S1 移动端头部系统

核心文件：

- 修改：`HealthShow/src/components/health-shell/PageHeroHeader.vue`
- 修改：`HealthShow/src/components/WarningCenterNav.vue`
- 修改：`HealthShow/src/layout/components/MobileBottomNav.vue`
- 修改：`HealthShow/src/styles/health-monitor.scss`

按页补位文件：

- `HealthShow/src/views/health-monitor/risk-warning/risk-warning.scss`
- `HealthShow/src/views/alert-management/notifications/notifications.scss`
- `HealthShow/src/views/alert-management/records/records.scss`
- `HealthShow/src/views/health-monitor/report-center/report-center.scss`
- `HealthShow/src/views/ai-chat/ai-chat.scss`

职责边界：

- 共享组件负责标题、说明、tabs、主操作和安全区的优先级
- 页面 SCSS 只处理个别布局补位，不再各页自行发明头部规则

### 2. S2-S3 Cockpit 公共壳与 Dashboard 样板

核心文件：

- 修改：`HealthShow/src/components/health-shell/PageHeroHeader.vue`
- 修改：`HealthShow/src/components/health-shell/MetricStrip.vue`
- 修改：`HealthShow/src/components/health-shell/PanelShell.vue`
- 修改：`HealthShow/src/views/health-monitor/dashboard/index.vue`
- 修改：`HealthShow/src/views/health-monitor/dashboard/dashboard.scss`

可能涉及的 Dashboard 子组件：

- `HealthShow/src/views/health-monitor/dashboard/components/DashboardDispatchPanel.vue`
- `HealthShow/src/views/health-monitor/dashboard/components/DashboardDevicePanel.vue`
- `HealthShow/src/views/health-monitor/dashboard/components/DashboardRightSidebar.vue`
- `HealthShow/src/views/health-monitor/dashboard/components/DashboardWarningStream.vue`
- `HealthShow/src/views/health-monitor/dashboard/components/DashboardDialogs.vue`

职责边界：

- 共享壳组件负责 hero、KPI、panel 的统一语法
- Dashboard 页面负责把当前业务内容装配进公共壳，不负责重新定义另一套视觉系统
- Dashboard 子组件只允许处理本区块信息密度和布局，不再内联硬编码主视觉

### 3. S4 Real-time / Safety Command 对齐

`real-time` 核心文件：

- 修改：`HealthShow/src/views/health-monitor/real-time/index.vue`
- 修改：`HealthShow/src/views/health-monitor/real-time/realtime.scss`
- 修改：`HealthShow/src/views/health-monitor/real-time/components/RealtimeHeader.vue`
- 修改：`HealthShow/src/views/health-monitor/real-time/components/RealtimeUserTable.vue`

`safety-command` 核心文件：

- 修改：`HealthShow/src/views/safety-command/index.vue`
- 修改：`HealthShow/src/views/safety-command/safety-command.scss`
- 视情况修改：
  - `HealthShow/src/views/safety-command/components/KpiCardRow.vue`
  - `HealthShow/src/views/safety-command/components/EventPanel.vue`
  - `HealthShow/src/views/safety-command/components/RiskPersonPanel.vue`

职责边界：

- `real-time` 重点收实时列表扫读性和工具条优先级
- `safety-command` 重点收指挥态、SOS 区、应急动作的首屏顺位

### 4. S5 预警工作线模板化

核心文件：

- 修改：`HealthShow/src/components/WarningCenterNav.vue`
- 修改：`HealthShow/src/views/health-monitor/risk-warning/index.vue`
- 修改：`HealthShow/src/views/health-monitor/risk-warning/risk-warning.scss`
- 修改：`HealthShow/src/views/alert-management/notifications/index.vue`
- 修改：`HealthShow/src/views/alert-management/notifications/notifications.scss`
- 修改：`HealthShow/src/views/alert-management/records/index.vue`
- 修改：`HealthShow/src/views/alert-management/records/records.scss`
- 复核：`HealthShow/src/views/alert-management/common/warning-lifecycle.js`

职责边界：

- `WarningCenterNav` 只负责跨页导航语义和移动端滚动行为
- 各页负责本页筛选、列表、SLA 信息块的具体落位
- 状态口径继续以 `warning-lifecycle.js` 为事实源，不在页面里重新发明 handled / pending 文案

### 5. S6 Workbench 样板页

`report-center` 核心文件：

- 修改：`HealthShow/src/views/health-monitor/report-center/index.vue`
- 修改：`HealthShow/src/views/health-monitor/report-center/report-center.scss`
- 复核：
  - `HealthShow/src/views/health-monitor/report-center/report-center-view-model.js`
  - `HealthShow/src/views/health-monitor/report-center/report-center-export.js`

`ai-chat` 核心文件：

- 修改：`HealthShow/src/views/ai-chat/index.vue`
- 修改：`HealthShow/src/views/ai-chat/ai-chat.scss`
- 复核：
  - `HealthShow/src/views/ai-chat/ai-chat-query-result.js`
  - `HealthShow/src/views/ai-chat/ai-chat-chart.js`
  - `HealthShow/src/views/ai-chat/ai-chat-text.js`

职责边界：

- `report-center` 重点收标题区、导出工具条、摘要和图表顺位
- `ai-chat` 重点收快捷问题、结果容器和移动端输入区的冲突关系

## 七、第二轮真正要沉淀的公共层

### 1. 已存在但需要加固的资产

- `PageHeroHeader`
  - 增加移动端信息优先级约束
  - 约束 `eyebrow / description / actions / meta` 的显示顺序
- `MetricStrip`
  - 收口 Cockpit 页 KPI 密度、最小宽度、移动端折叠方式
- `PanelShell`
  - 收口标题、说明、副操作、内边距、面板层级
- `PageEmptyState`
  - 统一空态结构和文案节奏

### 2. 当前确实还缺的能力

- `StatusChip`
  - 统一 `critical / high / medium / low / handled / pending / online / offline`
- `FilterToolbar`
  - 统一筛选项、搜索、导出、批量操作、主按钮布局
- `InlineErrorState`
  - 统一列表区、图表区、详情区的行内错误态
- `PageSkeletonState`
  - 为 Cockpit 页和 Workbench 页分别提供对应骨架屏

### 3. Token 层本轮不新增第二套，只做收口

当前 `index.scss` 已有 token 基座，本轮原则是：

- 优先继续使用 `--accent-primary / --accent-success / --accent-warning / --accent-danger`
- 优先继续使用 `--font-display / --font-body / --font-mono`
- 优先继续使用 `--radius-* / --shadow-* / --space-*`
- 页面级 SCSS 不再自行引入新的主色体系
- 页面级 SCSS 不再自行声明与全局冲突的字体主栈

## 八、分阶段执行计划

## Phase S1：移动端头部系统重构

目标：

先解决 `text_overlap` 最高频问题，把所有页面顶部信息做出稳定优先级。

优先文件：

- `HealthShow/src/components/health-shell/PageHeroHeader.vue`
- `HealthShow/src/components/WarningCenterNav.vue`
- `HealthShow/src/layout/components/MobileBottomNav.vue`
- `HealthShow/src/styles/health-monitor.scss`

执行内容：

- 定义移动端 hero 四级优先级：
  - 一级：页面标题
  - 二级：关键状态 / 当前时间 / 主操作
  - 三级：时间范围 tabs 或工具条
  - 四级：副标题 / eyebrow / 说明文案
- `PageHeroHeader` 在移动端默认允许弱化或折叠 `eyebrow`、长副标题
- `WarningCenterNav` 保持横向滚动，不再让顶部标题区和 tabs 互相挤压
- `MobileBottomNav` 与页面主体之间的底部留白统一使用安全区变量，避免输入区、操作条和底栏撞位

验收页：

- `dashboard`
- `risk-warning`
- `alert-notifications`
- `alert-records`
- `report-center`
- `ai-chat`

通过标准：

- 不再出现标题压标题
- 不再出现标题压 tabs
- 不再出现标题区与底栏导航互相侵占

## Phase S2：Cockpit 公共模式收口

目标：

把 `dashboard` 已经出现的新壳层能力，收成可复用的 Cockpit 模式，而不是只在单页有效。

执行内容：

- `PageHeroHeader` 输出 Cockpit 标准结构：
  - 页面身份
  - 当前状态
  - 时间范围
  - 主操作
- `MetricStrip` 输出 Cockpit KPI 标准结构：
  - 标签
  - 数值
  - 单位
  - 副文案
  - 点击反馈
- `PanelShell` 输出 Cockpit 面板标准结构：
  - 标题
  - 辅助说明
  - 右上角操作
  - 主体区

涉及页面：

- `dashboard`
- `real-time`
- `safety-command`
- `risk-warning`

## Phase S3：Dashboard 样板页精修

目标：

把 `dashboard` 从“局部试跑页”升级成整条 Cockpit 线的模板页。

当前证据：

- 移动端仍有 `text_overlap`
- 桌面端仍有 `chart_container_too_small`

执行内容：

- 缩短 hero 中非必要说明文字
- 重新定义首屏 KPI 与 hero 的上下关系
- 修正触发 `chart_container_too_small` 的图表容器高度和分栏比例
- 清理重复边框、重复说明块和无效强调色
- 强化三段阅读路径：
  - 左：当前态势
  - 中：趋势与决策
  - 右：排行与行动入口

## Phase S4：Real-time / Safety Command 对齐

目标：

让另外两张 Cockpit 大页跟上 `dashboard` 的语言，而不是继续保留旧监控页质感。

### `real-time`

重点：

- 头部 ticker 降噪
- 状态统计与筛选区收成一条工具带
- 表格强化扫读结构，不再靠高亮色堆层级
- 移动端改为卡片化主视图，不保留完整桌面表格心智

### `safety-command`

重点：

- `SOS` 区与标题区彻底拆层
- 强化指挥态和应急动作优先级
- 去掉 demo 感强的装饰性元素

## Phase S5：预警工作线模板化

目标：

把 `risk-warning / notifications / records / config / sos` 收成同一条预警工作线。

执行内容：

- 统一 `WarningCenterNav` 与标题区关系
- 统一筛选栏、导出区、批量操作区
- 统一列表卡片、状态块、SLA 提示块
- 统一 handled / pending / danger / overdue 的全局口径

建议沉淀：

- `WarningPageShell`
- `WarningToolbar`
- `WarningSummaryStrip`

## Phase S6：Workbench 样板页收口

目标：

确定 Workbench 线最终表达，不再让 `report-center` 和 `ai-chat` 各做一套风格。

### `report-center`

重点：

- 标题区明确告诉用户当前可导出什么、为什么能导出或不能导出
- 导出动作真正成为工具条而不是装饰按钮
- 图表、摘要、导出区建立主次顺序

### `ai-chat`

重点：

- 标题区、快捷问题区、对话区、结果区彻底拆层
- 结果区按文本、表格、图表、状态提示拆区
- 移动端输入区与底栏完全解耦

## 九、设计与实现护栏

### 1. 动效护栏

- 只允许 `transform / opacity`
- 不允许 `top / left / width / height` 参与动画
- 不允许全页持续闪烁
- 阴影只用于层级，不用于到处发光
- 所有点击反馈优先使用轻量 `translateY` 或 `scale`

### 2. 组件护栏

- 不新增前端库，只用现有 `Vue + SCSS + Element Plus`
- 不为了美化拆坏现有 `view-model / runtime / scss` 分层
- 新公共层必须兼容现有路由、双库切换和移动端底栏

### 3. 业务护栏

- 不隐藏桌面端关键 KPI 以换取“更简洁”
- 不把移动端内容压缩到不可操作
- 不修改数据链路、鉴权链路、双库语义去迁就视觉实现
- 不把视觉修复变成页面结构回退

## 十、验收标准

### 1. 工程门禁

必须通过：

- `npm run audit:structure`
- `npm run build`

### 2. 定向视觉门禁

按页面批次跑：

- `node scripts/with-env.mjs VISUAL_ROUTES=dashboard,risk-warning -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=real-time,safety-command -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=alert-notifications,alert-records -- npm run audit:visual`
- `node scripts/with-env.mjs VISUAL_ROUTES=report-center,ai-chat -- npm run audit:visual`

查看结果时，以 `HealthShow/tests/visual/artifacts/` 最新一轮 `layout-summary.md` 为准。

### 3. 页面体验验收

Cockpit 页首屏必须在 3 秒内看清：

- 当前页是什么
- 当前风险或状态在哪里
- 下一步能做什么

Workbench 页首屏必须在 3 秒内看清：

- 当前要处理什么
- 当前筛选条件是什么
- 核心操作入口在哪里

移动端必须满足：

- 标题不重叠
- tabs 不压标题
- 主操作可点击
- 底栏不遮挡输入区和关键按钮

### 4. 完成前验证口径（superpowers 版）

任何以下说法，必须先有同一轮新鲜命令证据：

- “Phase S1 已完成”
- “移动端头部已经稳定”
- “Dashboard 已通过视觉收口”
- “Workbench 线已经成型”

最低验证规则：

- 共享层改动：
  - `npm run audit:structure`
  - `npm run build`
- 头部和布局改动：
  - 跑对应 `VISUAL_ROUTES` 的 `npm run audit:visual`
- 只改单页时：
  - 至少重跑该页所在批次的视觉审计

报告规则：

- 如果命令未跑，只能写“已修改，未验证”
- 如果命令已跑但有失败，只能按失败事实汇报，不得写“已完成”
- 最终结论以最新产物和退出码为准，不以主观观感代替

## 十一、建议推进顺序

1. 先做 `Phase S1`
2. 再做 `Phase S2 + S3`
3. 再做 `Phase S4`
4. 再做 `Phase S5`
5. 最后做 `Phase S6`

原因：

- 当前最大问题是移动端头部系统，不是再换一轮颜色
- `dashboard` 必须先成为稳定样板页，其它 Cockpit 页面才有统一模板可跟
- 预警线和 AI / 报表线要分开收，避免 Workbench 线再次漂成另一套视觉语言

## 十二、结论

按 `design-taste-frontend` 的标准，当前项目下一轮最正确的方向不是“继续加玻璃感”或“继续调配色”，而是：

- 先把移动端头部系统做稳定
- 再把 Cockpit 线收成一套
- 再把 Workbench 线收成一套
- 最后用视觉审计和结构门禁把重叠、拥挤、旧风格回流问题清掉

这才是当前仓库最符合 skill 护栏、也最适合实际落地的前端美化推进路线。
