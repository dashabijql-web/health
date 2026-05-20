# HealthShow 下一轮执行计划

日期：`2026-05-05`

这份文档只记录下一轮要落地的事，目标是把剩余技术债按顺序收掉，避免上下文丢失：

- 先封 `dashboard`
- 再收 `real-time / employee-profile`
- 再统一 timer / 轮询 / 滚动
- 最后检查路由切换与菜单细节

## 当前前提

- 前端仓库：`D:/Health/HealthShow`
- 后端仓库：`D:/Health/HealthData`
- 当前系统已经可以构建，且基础回归链已经存在
- 当前最重的结构债仍然是巨石页面，而不是入口路由

## 执行顺序

### P0：先封 `dashboard`

目标：

- 把 `dashboard/index.vue` 从巨石页收敛成“页面壳 + 数据块 + 图表块 + 指挥块”
- 不先追求视觉重画，先追求职责分离

建议拆分：

- `page-shell`
- `dashboard-summary`
- `dashboard-chart-options`
- `DashboardDispatchPanel`
- `data-loader`

验收：

- `dashboard/index.vue` 只保留页面组装逻辑
- 业务数据获取和图表配置不再直接堆在主文件里
- `npm run build` 通过
- `npm run audit:e2e` 通过

### P1：再收 `real-time` 和 `employee-profile`

目标：

- 把实时页和画像页的公共逻辑抽出来
- 把查询、轮询、滚动、详情切换分开

建议拆分：

- `realtime-helpers`
- `useIntervalTask`
- `useTimeoutTask`
- `useScrollLoop`
- `useClock`

验收：

- 页面重复 timer 代码显著减少
- 复用路由场景下，query 变化能正确刷新页面
- `npm run build` 通过
- `npm run audit:e2e` 通过

### P1：统一 timer / 轮询 / 滚动

目标：

- 收口所有页面级 `setInterval / setTimeout`
- 避免隐藏标签页继续跑无用任务

验收：

- 只保留统一 hook / utility 入口
- 页面卸载时能正确停止定时任务
- 不再新增散落在组件里的裸 timer

### P2：收路由切换和菜单细节

目标：

- 保持当前入口稳定
- 减少 `AppMain` 的重建体感
- 菜单仍以任务分组呈现，但降低派生复杂度

验收：

- 菜单结构不再依赖临时补丁
- 页面切换不再出现明显“像整页刷新”的体感
- 现有书签和旧路径保持可用

## 执行规则

- 每次只动一类问题
- 每次改完都跑回归
- 不把一个大文件拆成多个同样复杂的小文件
- 不为了“更安全”再叠一层补丁

## 停点

当下面两条同时满足时，本轮可以停：

- `dashboard / real-time / employee-profile` 的职责边界明显收紧
- `build + e2e` 回归保持稳定

