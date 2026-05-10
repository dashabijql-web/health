# HealthShow 接力执行计划

日期：`2026-05-05`  
范围：`D:/Health/HealthShow`

这份文档给下一位 Codex 直接执行，不用再从旧计划、旧聊天里二次还原上下文。

## 1. 当前结论

`HealthShow` 现在不是失控屎山，但仍然是中高技术债，而且风险点已经从“单纯大文件”变成：

- 巨石页面仍然很多
- 重构进行中，跨文件接线容易接错
- 工作树很脏，不能假设是干净仓库

今天刚发生过一个典型问题：

- “部门综合看板空白”不是后端没数据
- 也不是接口坏了
- 是前端拆分后把图表 builder 接错了

这说明现在最该防的是：

- 接线错误
- 局部重构带回归
- 在脏工作树上误回滚用户现有改动

## 2. 执行前固定事实

- 前端目录：`D:/Health/HealthShow`
- 后端目录：`D:/Health/HealthData`
- 当前前端开发端口：`9528`
- 后端端口：`8080`
- TCP：`9000`
- 登录：`admin / admin123`
- Token 在 Cookie，不在 localStorage

当前仓库状态非常重要：

- `HealthShow` 工作树大量未提交改动
- 这些改动默认视为用户在做的工作
- 不允许为了“清理环境”回滚这些改动

## 3. 当前真实债务排序

按现在的实际风险重新排，不沿用更早的顺序。

### P0：必须优先处理

1. `dashboard`
2. `health-portrait`
3. `safety-command`

### P1：第二梯队

1. `heart-rate`
2. `pressure`
3. `blood-pressure`
4. `blood-oxygen`
5. `risk-warning`
6. `sleep`

### P2：第三梯队

1. `device-management`
2. `employee-profile`
3. 菜单派生和细节导航
4. 文档漂移

## 4. 当前体量快照

当前最重的页面：

- `src/views/health-monitor/dashboard/index.vue`：`2171` 行
- `src/views/personnel-management/health-portrait/index.vue`：`1212` 行
- `src/views/safety-command/index.vue`：`1200` 行
- `src/views/health-monitor/heart-rate/index.vue`：`1096` 行
- `src/views/device-management/index.vue`：`1037` 行
- `src/views/health-monitor/employee-profile/index.vue`：`999` 行
- `src/views/health-monitor/risk-warning/index.vue`：`989` 行
- `src/views/health-monitor/blood-oxygen/index.vue`：`984` 行
- `src/views/health-monitor/blood-pressure/index.vue`：`965` 行
- `src/views/health-monitor/report-center/index.vue`：`925` 行
- `src/views/health-monitor/sleep/index.vue`：`917` 行
- `src/views/health-monitor/pressure/index.vue`：`913` 行
- `src/views/health-monitor/real-time/index.vue`：`891` 行

## 5. 当前已经做过的事

这些不要重复做，也不要无意打回去：

- 路由入口链已经比以前稳定
- `AppMain` 的强制重建问题已经收过一轮
- `dashboard / real-time / employee-profile` 已经有外拆 helper
- 一部分 timer 已经统一到：
  - `src/composables/useIntervalTask.js`
  - `src/composables/useTimeoutTask.js`
  - `src/composables/useScrollLoop.js`
  - `src/utils/task-timer.js`
- 菜单分组已经集中到 `src/layout/menu/`
- `build`、局部 e2e 目前能通过

今天已确认修复的一点：

- `dashboard` 的“部门综合看板”已恢复
- 根因是 `dashboard-view-actions.js` 里 `initDeptChart()` 接错了 builder

## 6. 当前仍然明显脏的点

### 6.1 巨石页依然存在

虽然 `dashboard` 降到了 `2171` 行，但它仍然不是纯页面壳。

`health-portrait`、`safety-command` 现在已经比 `real-time` 更重，下一轮不能再只盯 `real-time`。

### 6.2 页面级裸 timer 还散

仍然保留明显裸 `setInterval / setTimeout` 的典型页：

- `src/views/safety-command/index.vue`
- `src/views/health-monitor/heart-rate/index.vue`
- `src/views/health-monitor/pressure/index.vue`
- `src/views/health-monitor/blood-pressure/index.vue`
- `src/views/health-monitor/blood-oxygen/index.vue`
- `src/views/health-monitor/risk-warning/index.vue`
- `src/views/health-monitor/sleep/index.vue`

### 6.3 接线风险高于单纯“大文件”

现在很多页已拆成：

- 主页面壳
- runtime helper
- chart helper
- summary helper
- view-model helper

这比全堆一个文件强，但也更容易出现：

- builder 接错
- 传参字段不一致
- chart option 和实际数据结构不匹配

## 7. 下一位 Codex 的执行顺序

严格按这个顺序，不要跳。

### 阶段 A：先封 `dashboard`

目标：

- 继续把 `dashboard/index.vue` 收到“页面壳 + 少量路由交互”
- 不再让主文件继续背数据编排、图表接线、细节弹窗

优先动作：

1. 清点 `dashboard/index.vue` 里还剩哪些长方法块没有外提
2. 优先拆：
   - detail/modal 逻辑
   - chart init / resize / dispose
   - 剩余的数据组装
3. 每拆一小块就验证，不做整页大改写

验收：

- 主文件继续明显下降
- `dashboard` 不再出现空白图表、点击无响应、404 跳转
- `npm run build` 通过
- `dashboard` 定向 e2e 通过

### 阶段 B：再拆 `health-portrait`

目标：

- 把画像页从“页面 + 打印 + 风险计算 + AI + 滚动”拆成清晰层次

优先动作：

1. 抽风险计算和视图模型
2. 抽轮询和自动滚动
3. 抽打印和导出

验收：

- 页面壳只负责组合
- 风险/画像/打印逻辑不再混在同一段
- `build + e2e` 通过

### 阶段 C：处理 `safety-command`

目标：

- 把时钟、轮询、数据拉取、事件映射、部门/区域构造分层

优先动作：

1. 收定时器
2. 收 `fetchAllData / fetchCritical` 一类大方法
3. 收事件映射和部门列表构造

验收：

- `safety-command/index.vue` 明显降重
- 页面切换时不再容易出现整块重刷体感
- `build + e2e` 通过

### 阶段 D：批量收重复模式页

目标：

- 统一分析页里的重复滚动、top5、图表 resize、定时刷新模式

优先页面：

1. `heart-rate`
2. `pressure`
3. `blood-pressure`
4. `blood-oxygen`
5. `risk-warning`
6. `sleep`

原则：

- 不追求一次拆干净
- 先抽共性，再降每个页面体量

### 阶段 E：最后收 `device-management / employee-profile`

这两页还重，但已经比前三批次风险低，放后面。

## 8. 每一步都要跑的验证

### 最小验证

每轮改动后至少跑：

```powershell
npm run build
```

### 页面级验证

改某个关键页时，优先跑定向 e2e，不要每次上来全量跑：

```powershell
$env:HTTP_PROXY=''
$env:HTTPS_PROXY=''
$env:ALL_PROXY=''
$env:NO_PROXY='127.0.0.1,localhost'
$env:E2E_ROUTE_FILTER='dashboard'
$env:E2E_SKIP_NAV_AUDITS='1'
$env:E2E_SKIP_EMPLOYEE_FLOW='1'
$env:E2E_SKIP_MOBILE='1'
node tests/e2e/playwright-audit.mjs
```

按页面替换 `E2E_ROUTE_FILTER` 即可。

### 阶段性验证

一整阶段结束后再跑全量：

```powershell
npm run audit:e2e
```

## 9. 重要坑

### 9.1 不要对巨型 Vue 文件做大范围文本改写

`dashboard/index.vue` 已经出过一次文本改写损坏。  
后续对大文件只允许：

- 小块 `apply_patch`
- 小步拆分
- 每次改后马上验证

不要再做整段搬运式批量替换。

### 9.2 本地请求可能被系统代理劫持到 `127.0.0.1:7890`

如果本地接口明明是通的，但命令行请求报连接 `7890`，优先按下面方式绕过：

```powershell
curl.exe --noproxy "*" http://localhost:9528/
curl.exe --noproxy "*" http://localhost:8080/health/
```

跑 Node / Playwright 时也优先清空代理环境变量。

### 9.3 不要因为页面空白就先怀疑后端

这次“部门综合看板空白”已经证明：

- 页面空白不一定是接口没数据
- 先看前端 builder、映射、图表 init 链

## 10. 执行规则

- 默认后台静默操作，不碰用户前台浏览器
- 不要回滚用户现有未提交改动
- 每次只解决一类结构问题
- 每次改完先验证，再继续拆
- 如果发现收益开始明显下降，就停在当前阶段，不硬追“全部拆完”

## 11. 这一轮的目标线

当下面三条同时成立时，可以认为这一轮执行到合理停点：

- `dashboard` 不再是主要风险源
- `health-portrait` 与 `safety-command` 进入可持续拆分状态
- 页面级裸 timer 大幅减少

