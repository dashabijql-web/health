# Health 全局产品与页面重构方案

更新日期：2026-05-03

## 1. 目标与依据

这份方案的目标不是“重画几个页面”，而是把 `HealthShow` 从“模块并列堆放”调整为“按工作任务组织”的产品结构，并给出第一阶段可以直接开工的实施清单。

本方案基于当前真实代码，而不是基于旧文档假设。主要核对依据：

- 路由：`src/router/index.js`、`src/router/health-monitor.js`、`src/router/alert-management.js`
- 菜单渲染：`src/layout/components/Sidebar/index.vue`、`src/layout/components/Sidebar/SidebarItem.vue`
- 权限过滤：`src/store/modules/user.js`
- 关键页面：
  - `src/views/health-monitor/dashboard/index.vue`
  - `src/views/health-monitor/real-time/index.vue`
  - `src/views/health-monitor/risk-warning/index.vue`
  - `src/views/alert-management/notifications/index.vue`
  - `src/views/health-monitor/employee-archive/index.vue`
  - `src/views/health-monitor/employee-profile/index.vue`
  - `src/views/health-monitor/mine-entry/index.vue`
  - `src/views/health-monitor/workbench/index.vue`
  - `src/views/health-monitor/report-center/index.vue`
  - `src/views/health-monitor/trend-warning/index.vue`
  - `src/views/ai-chat/index.vue`
  - `src/views/safety-command/index.vue`
  - `src/layout/components/MobileBottomNav.vue`

## 2. 当前产品形态结论

当前系统已经不是“功能缺失”，而是“功能存在但任务流不够清晰”。

### 2.1 当前已经具备的能力

- `dashboard` 已经具备综合值班首页能力，但同时承载了 KPI、趋势、预警、设备、排行、AI 摘要等过多内容。
- `real-time` 已经接近实时工作台，具备筛选、表格、实时消息、下发消息等操作能力。
- `risk-warning` 与 `notifications` 都在做预警相关工作，只是一个偏分析，一个偏消息与处理。
- `employee-archive`、`employee-profile`、`mine-entry`、`workbench` 已经构成“人员主线”的雏形。
- `report-center`、`trend-warning`、`ai-chat` 已经具备“分析/解释/报告”能力，但入口仍然分散。
- `safety-command` 已经形成另一条偏指挥态的大屏视图。

### 2.2 当前主要问题

1. 菜单按“页面名”组织得比按“工作任务”更明显。
2. `dashboard`、`real-time`、`risk-warning` 都在承担“总览”角色，导致职责重叠。
3. 预警相关能力散落在 `risk-warning`、`notifications`、`records`、`sos`、`config` 多处，用户处理一条事件需要跨页。
4. 人员相关能力已经很多，但档案、画像、准入、月历之间仍然是“跳转关系”，不是“统一人员视角”。
5. `AI` 当前更像独立页面，而不是嵌入决策流程的助手。
6. 移动端底部导航和桌面端主导航的任务分组还没有完全统一。

## 3. 设计原则

### 3.1 产品原则

- 优先展示“要处理什么”，其次才是“能分析什么”。
- 每个一级模块只承担一条主任务线。
- 分析页和处理页要在信息层级上明显区分。
- AI 作为解释层和建议层嵌入页面，不优先做孤立入口。

### 3.2 技术原则

- 第一阶段优先保留现有接口、现有路径和现有数据模型，减少前后端联动风险。
- 需要重组一级菜单时，优先通过调整顶层路由结构和重定向实现，而不是只改标题。
- 保留旧路径兼容入口，避免打断已有收藏、测试脚本和移动端导航。

## 4. 目标信息架构

### 4.1 新的一级导航

建议将当前系统收敛为 6 组一级任务导航。

| 目标导航 | 当前页面来源 | 目标职责 |
| --- | --- | --- |
| 指挥中心 | `safety-command`、`dashboard` | 用于班次值守、全局态势判断、待办指挥 |
| 监测中心 | `real-time`、`heart-rate`、`pressure`、`blood-pressure`、`blood-oxygen`、`trend-warning` | 用于实时监测、指标分析、趋势识别 |
| 预警中心 | `risk-warning`、`notifications`、`sos`、`records`、`config` | 用于风险识别、处置流转、阈值治理 |
| 人员中心 | `employee-archive`、`employee-profile`、`mine-entry`、`workbench` | 用于按员工视角查看健康、准入和历史 |
| 报告与 AI | `report-center`、`ai-chat` | 用于产出报告、解释分析、生成总结 |
| 系统管理 | `admin/*` | 用于设备、用户、组织、角色、工种维护 |

### 4.2 路由落地建议

当前侧边栏直接消费 `resultAllRoutes`，而 `resultAllRoutes` 来自 `constantRoutes` 的过滤结果。这意味着：

- 菜单主分组不是单纯换文案。
- 如果要让一级导航真正变成上表结构，需要重组顶层路由对象。
- 第一阶段可以保留页面路径不变，通过新增父级路由壳和兼容重定向过渡。

建议的路由策略：

- 保留已有业务路径，如 `/health-monitor/dashboard`、`/alert-management/notifications`。
- 新增更清晰的任务型父路由分组。
- 对原有入口增加 `redirect` 或 `meta.activeMenu`，保证旧链接继续可用。
- `risk-warning` 不建议立即删除，第一阶段先把它定位成“预警中心/风险总览”。

### 4.3 移动端导航建议

`src/layout/components/MobileBottomNav.vue` 当前已经非常接近正确方向：

- 概览
- 实时
- 准入
- 预警
- 人员

第一阶段只需与新的一级导航命名统一，不建议让移动端承载过多二级跳转。

## 5. 模块职责重定义

### 5.1 指挥中心

#### 5.1.1 `dashboard`

新定位：值班首页，而不是全量大屏集合页。

它只回答 3 个问题：

1. 今天当前的健康态势如何。
2. 现在最需要处理的异常是什么。
3. 哪些人员、部门、设备需要重点关注。

#### 5.1.2 `safety-command`

新定位：应急/指挥视图。

它负责：

- 高危态势放大
- 指挥动作入口
- 部门/区域/高危人员快速处置

它不负责承载通用健康分析明细。

### 5.2 监测中心

#### 5.2.1 `real-time`

新定位：实时监测工作台。

主任务：

- 快速筛人
- 快速看状态
- 快速下发消息
- 快速进入画像和处置

不再承担综合大屏职责。

#### 5.2.2 各单指标页

- `heart-rate`
- `pressure`
- `blood-pressure`
- `blood-oxygen`

新定位：专题分析视图。

这些页面应统一成同一种结构：

- 顶部摘要
- 中部主图
- 底部明细/列表

不要每页都长成不同的微型系统。

#### 5.2.3 `trend-warning`

新定位：趋势识别与提前干预入口。

它不应只做“预测名单”，而应成为监测中心中从历史走向未来的一层判断。

### 5.3 预警中心

#### 5.3.1 `risk-warning`

新定位：预警总览页。

保留它的强项：

- 异常趋势
- 部门风险分布
- 当天预警清单
- 体征回看抽屉

但它不再承担全部预警处理流。

#### 5.3.2 `notifications`

新定位：待处理消息入口。

用于：

- 查看新预警
- 快速筛选
- 单条/批量处理
- 跳转到人员画像

#### 5.3.3 `records`

新定位：处置历史库。

#### 5.3.4 `sos`

新定位：紧急事件入口。

#### 5.3.5 `config`

新定位：阈值治理与规则维护。

### 5.4 人员中心

#### 5.4.1 `employee-archive`

新定位：员工总入口。

职责：

- 查人
- 筛部门
- 查看状态
- 进入画像
- 触发 AI 报告

#### 5.4.2 `employee-profile`

新定位：统一人员主档案。

这个页面应成为人员中心的核心页，挂载：

- 基本信息
- 实时体征
- 7 天趋势
- 近期预警
- AI 风险解释
- 准入状态
- 月度日历入口

#### 5.4.3 `mine-entry`

新定位：班前准入决策页。

它应强调：

- 准入/禁入结论
- 具体禁入原因
- 需要复检或复核的指标
- 快速进入人员画像

#### 5.4.4 `workbench`

新定位：员工月度健康日历。

它更适合成为从人员画像进入的延展页，而不是强独立入口。

### 5.5 报告与 AI

#### 5.5.1 `report-center`

新定位：结构化输出中心。

它承接：

- 月度报表
- 部门对比
- 趋势说明
- 导出输出

#### 5.5.2 `ai-chat`

新定位：探索式问答入口，而不是唯一 AI 载体。

AI 的主要价值应分流到具体页面内：

- `dashboard`：一句值班总结
- `employee-profile`：个体风险解释
- `mine-entry`：禁入原因摘要
- `report-center`：报告摘要与建议

## 6. 关键页面方案

### 6.1 `dashboard` 方案

目标：把当前“信息很全”改成“决策优先级更清楚”。

建议结构：

- 第一层：今日关键 KPI
  - 检测人数
  - 高危人数
  - 待处理预警
  - 异常人数
  - 班前达标率
  - 设备在线率
- 第二层：当前待处理区
  - 高危人员清单
  - 最新危险预警
  - 部门风险排行
- 第三层：分析区
  - 近 7/30 天异常率趋势
  - 预警时段分布
  - 设备与环境概况
- 第四层：AI 结论区
  - 今日风险摘要
  - 需重点关注的部门/人员

从当前页面迁移建议：

- 保留现有 `warningEvents`、`metricCards`、设备状态和趋势图。
- 降低“装饰型信息块”权重。
- 把“待处理事项”提升到趋势图之前。

### 6.2 统一预警中心方案

目标：让用户在一个模块里完成“看预警、筛预警、处理预警、回看处置”。

建议结构：

- 顶部摘要条
  - 危险
  - 预警
  - 提示
  - 待处理
- 主体四个 tab
  - `总览`
  - `待处理`
  - `处置记录`
  - `阈值配置`
- 右侧或抽屉
  - 人员详情
  - 预警时刻前后曲线
  - 处置备注与动作

当前页面对应关系：

- `risk-warning` -> `总览`
- `notifications` -> `待处理`
- `records` -> `处置记录`
- `config` -> `阈值配置`
- `sos` -> 独立强调的紧急入口

### 6.3 人员主线方案

目标：让“查员工健康”从跳页面变成一条连续路径。

建议路径：

1. `employee-archive` 查找员工
2. 进入 `employee-profile`
3. 从画像页进入：
   - `mine-entry` 的准入视图
   - `workbench` 的月历视图
   - `report-center` 或 AI 报告

画像页新增建议：

- 增加“最近一次准入结果”
- 增加“最近 7 天风险摘要”
- 增加“最近一次 AI 结论”

### 6.4 `real-time` 方案

目标：把当前页面明确成“监控与操作工作台”。

建议结构：

- 左：筛选区
  - 姓名/工号
  - 部门
  - 状态
  - 快捷异常筛选
- 中：人员状态表
  - 实时指标
  - 状态
  - 更新时间
  - 快捷操作
- 右：详情抽屉
  - 当前体征
  - 最近异常
  - 快捷消息
  - 跳转画像/处置

当前表格能力已经够用，重点是补“右侧详情”和“异常优先级”。

### 6.5 `mine-entry` 方案

目标：让准入页更像决策页，而不是结果展示页。

建议增加：

- 禁入原因聚合排序
- 复检建议
- 按部门/班次查看禁入情况
- 与画像页互跳

### 6.6 `report-center` 与 `ai-chat` 方案

目标：形成“发现问题 -> 解释问题 -> 输出报告”的闭环。

建议：

- `report-center` 保持结构化输出
- `ai-chat` 保持探索式问答
- 报表页内嵌 AI 总结，不再要求用户每次跳到独立聊天页

## 7. 第一阶段实施清单

第一阶段只做高收益、低破坏的改造，不追求一次性重写所有页面。

### 7.1 任务一：重构菜单与路由分组

目标：让一级导航按任务线组织。

涉及文件：

- `src/router/index.js`
- `src/router/health-monitor.js`
- `src/router/alert-management.js`
- `src/layout/components/MobileBottomNav.vue`

实施要点：

- 保留旧路径兼容。
- 抽出新的一级菜单分组。
- 将 `risk-warning` 明确挂到预警中心。
- 统一桌面端与移动端主任务标签。

验收标准：

- 用户在 5 秒内能找到“监测”“预警”“人员”“报告”四条主线。
- 旧路径可继续访问。

### 7.2 任务二：重构 `dashboard`

目标：把首页从“满屏展示”改成“值班决策页”。

涉及文件：

- `src/views/health-monitor/dashboard/index.vue`

实施要点：

- 保留现有数据接口和图表数据源。
- 调整区域优先级。
- 将待处理区提升到图表之前。
- 为关键 KPI 增加明确跳转目标。

验收标准：

- 打开首页 5 秒内知道“当前最该处理什么”。
- 待处理区无需滚动即可进入首屏。

### 7.3 任务三：统一预警中心

目标：把分散的预警处理路径并到一个模块内。

涉及文件：

- `src/views/health-monitor/risk-warning/index.vue`
- `src/views/alert-management/notifications/index.vue`
- `src/views/alert-management/records/index.vue`
- `src/views/alert-management/config/index.vue`
- `src/views/alert-management/sos/index.vue`

实施要点：

- 以 `notifications` 为“待处理”主入口。
- 以 `risk-warning` 为“总览/分析”页。
- 用统一标题、统一筛选、统一状态色完成视觉归一。

验收标准：

- 处理一条预警不需要跨两个一级菜单。
- 同一员工的预警详情和画像跳转路径稳定。

### 7.4 任务四：打通人员主线

目标：让档案、画像、准入、月历形成连续路径。

涉及文件：

- `src/views/health-monitor/employee-archive/index.vue`
- `src/views/health-monitor/employee-profile/index.vue`
- `src/views/health-monitor/mine-entry/index.vue`
- `src/views/health-monitor/workbench/index.vue`

实施要点：

- 统一“进入画像”的动作文案。
- 在画像页增加准入结果和月历入口。
- 从准入页和月历页都能回到画像页。

验收标准：

- 查一个员工的健康全貌不需要在多个一级导航之间来回找。

### 7.5 任务五：AI 从独立入口转为嵌入增强

目标：让 AI 更像辅助决策层，而不是另一个孤立系统。

涉及文件：

- `src/views/health-monitor/report-center/index.vue`
- `src/views/ai-chat/index.vue`
- `src/views/health-monitor/dashboard/index.vue`
- `src/views/health-monitor/employee-profile/index.vue`

实施要点：

- 首页增加一句 AI 风险摘要。
- 画像页增加个体解释。
- 报表页增加导出前总结。
- `ai-chat` 保留，但不再承担唯一 AI 价值。

验收标准：

- 用户在不打开聊天页的情况下，也能看到 AI 带来的解释增量。

## 8. 建议的实施顺序

1. 菜单与路由分组
2. `dashboard`
3. 预警中心整合
4. 人员主线打通
5. 报表与 AI 嵌入

原因：

- 先改信息架构，后改页面内容，能避免同一页面反复返工。
- `dashboard` 和预警中心是收益最高的两个入口。
- 人员主线依赖导航结构稳定后再做更顺。

## 9. 不建议在第一阶段做的事

- 不建议第一阶段大规模改后端接口命名。
- 不建议立即删除旧页面或旧路径。
- 不建议同时重做所有单指标页视觉风格。
- 不建议把 AI 聊天页扩成新的一级复杂系统。

## 10. 交付定义

第一阶段完成后，产品应达到以下状态：

- 用户能够按任务线理解系统，而不是按页面名猜功能。
- 首页优先呈现“异常与待办”。
- 预警处理形成统一入口。
- 员工健康信息形成统一主档案视角。
- AI 从独立功能转变为页面内增强能力。

