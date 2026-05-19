# HealthShow 执行计划

日期：`2026-05-05`

目标：在不破坏当前可用性的前提下，继续收敛 `HealthShow` 的结构债。`dashboard / real-time / employee-profile / safety-command / health-portrait` 都已经收过至少一轮；当前重点从 `health-portrait` 切回剩余主战场，继续压缩 `dashboard / employee-profile` 的页面壳和控制层，确保回归链持续稳定。

## 当前事实

- 前端仓库是 `D:/Health/HealthShow`，后端仓库是 `D:/Health/HealthData`。
- 当前工作树不是干净仓库，已有改动视为用户现有工作，禁止无原因回滚。
- 入口链、路由静态注册、`#/404` 和登录首跳问题已基本收口。
- 当前最大债务仍在页面层，不在入口层。
- `health-portrait/index.vue` 已降到约 `369` 行，页面控制层已下沉到 `use-health-portrait-page.js`。
- `safety-command/index.vue` 已降到约 `373` 行，已经退出主战场。
- `employee-profile/index.vue` 已降到约 `339` 行，页面控制层已下沉到 `use-employee-profile-page.js`。
- `dashboard/index.vue` 已降到约 `264` 行，页面默认态拆到 `dashboard-page-state.js`，computed 拆到 `dashboard-computed.js`，生命周期编排已下沉到 `dashboard-lifecycle.js`。
- `dashboard-runtime.js` 已降到约 `175` 行，API 拉数、cache、归一化已下沉到 `dashboard-runtime-data.js`，失效的父层 scroll loop 已清掉。
- `dashboard-chart-methods.js` 约 `170` 行，图表数据准备、cache、分布解析和部门人次曲线数据已下沉到 `dashboard-chart-data.js`。
- `dashboard-detail-methods.js` 已降到约 `143` 行，员工抽屉和预警曲线的 API / 归一化已下沉到 `dashboard-detail-data.js`。
- `dashboard` 当前剩余重量不再在页面壳，主要在 `dashboard-chart-methods.js`、部分详情图表初始化和少量右侧面板细节。

## 当前优先级

### P0

- `dashboard-chart-methods`：继续收图表初始化和弹窗图表装配
- `dashboard` 详情图表：视情况把员工抽屉图表 DOM 初始化也统一到组件 ref 链路

### P1

- `employee-profile`：守稳 composable 外拆结果，不回退趋势图 / AI / 打印链路
- `real-time`：保持轮询、滚动和筛选逻辑稳定
- `health-portrait`：守稳本轮外拆结果，不回退图表 / AI / 导出链路
- `timer` 统一：继续把裸 `setInterval / setTimeout` 收进统一工具

### P2

- 菜单派生细节
- 文档同步
- 回归基线补充

## 本轮实施切片

### 1. `dashboard` 方法层继续压缩

目标：

- 保持 `dashboard/index.vue` 继续做页面壳
- 把剩余图表装配和详情图表初始化继续外提到 helper / 组件

验收：

- 主文件继续保持轻量，不回流 data/computed/大段模板
- `detail/runtime/chart` 都继续向 helper / 组件收口
- `build` 通过

### 2. `employee-profile` 本轮只守稳

目标：

- 不在下一轮里回流控制层
- 保持详情、趋势图和 AI 报告行为不变

验收：

- `build` 通过
- 页面行为不回退

### 3. 其余已收页面只做守稳

目标：

- 不新增新的扩面重构
- 只修复当前能顺手统一的生命周期和重复代码

验收：

- 页面行为不变
- 重复代码减少

## 执行规则

- 每次只动一类问题
- 每次改完先验证，再继续下一步
- 不把一个大文件拆成多个同样复杂的小文件
- 不在没有验证的情况下回头重写入口链

## 停点

当下面两条同时满足时，本轮可以停：

- `dashboard / employee-profile` 已继续各收一刀
- `build + 关键回归` 保持稳定

