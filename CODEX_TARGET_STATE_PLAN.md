# Health 项目目标态执行计划

更新时间：2026-05-08

适用范围：

- 前端仓库：`D:/Health/HealthShow`
- 后端仓库：`D:/Health/HealthData`
- 工作区根目录：`D:/Health`

---

## 1. 这份文档解决什么问题

- `docs/archive/历史归档-治理清单.md` 是问题总表。
- `docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md` 是第一轮治理手册。
- 本文件不是重复那两份文档。
- 本文件回答的是另一件事：
  - 如果目标不是“完成第一轮治理”，而是“把屎山代码和健壮度问题压到基本可控”，那么还要做到什么程度。

换句话说：

- 前两份文档解决的是“先止血、先拆第一刀、先补第一批回归”。
- 本文件解决的是“做完哪些阶段，才能说项目从高风险屎山降到可维护、可继续演进的状态”。

---

## 2. 目标定义

这里的“达到目标”不定义为“所有技术债归零”。

这里的“达到目标”定义为：

1. 核心高风险链路不再靠人肉记忆维护。
2. 核心大类和大页不再继续失控膨胀。
3. 常改、常看的业务域已经有稳定边界、统一入口和最小自动化回归。
4. 发生错库、慢查询、登录异常、AI 异常、缓冲堆积、页面结构回归时，可以较快定位，不再只能靠猜。
5. 新功能继续追加时，默认落在已收口的结构里，而不是重新堆回超级类和超级页。

如果这些条件同时满足，就可以把当前项目定义为：

- 仍然有历史债。
- 但不再是“不可控的屎山”。
- 已经进入“可维护、可接手、可继续重构”的状态。

---

## 3. 达标标准

只有下面四组标准同时成立，才算“你的目标基本达到”。

### 3.1 后端结构达标

- `DashboardController`、`RealtimeController`、`HealthPortraitController`、`TrendWarningController` 这类高频 controller 不再直接调用 mapper。
- `DashboardController` 的缓存、表源选择、日期范围解析、聚合编排全部下沉到 service。
- `DashboardMapper` 不再承担继续膨胀的超级 SQL 仓库角色，至少按主题拆出稳定边界。
- `WatchDataHandler` 不重新膨胀回超级入口类。
- `DataProcessService` 不重新吸回设备上下文、路由、预警、缓冲等职责。
- 对外高频接口不再继续新增裸 `Map<String, Object>` 返回。
- 核心业务域出现异常时，返回语义统一，不再继续堆“获取失败: + e.getMessage()` 这类拼接。

### 3.2 前端结构达标

- 路由、菜单、移动底栏、权限可见性使用同一套元数据来源。
- 历史 `.js` / `.mjs` 路由双轨状态结束，保留一套真实来源。
- `dashboard`、`real-time`、`safety-command`、`health-portrait`、`employee-profile`、`workbench` 形成统一的大页目录规范。
- 五类指标页形成统一 page engine 或等价抽象，不再按整页复制。
- 关键页面的计时器、事件监听、图表实例生命周期进入统一 composable 或 runtime 约束。
- 核心页面的 `index.vue` 变成页面壳，不再继续堆积业务细节。

### 3.3 健壮度达标

- 后端完整测试 `mvn -q test -f D:\Health\HealthData\pom.xml` 稳定通过。
- 前端最小回归基线稳定通过：
  - `npm run audit:structure`
  - `npm run audit:nav`
  - `npm run audit:page-structure`
  - `npm run audit:api`
  - `npm run audit:auth`
  - `npm run audit:e2e`
  - `npm run audit:pipeline`
- 双库、登录态、Redis buffer、AI SQL、异步上下文、手表入库这些链路都有自动化回归，不再只是人工口头约定。
- 出现慢、错、堵、丢、错库时，日志和指标足够定位问题，不再只能重读代码。

### 3.4 文档与交接达标

- `AGENTS.md`
- `docs/archive/历史归档-治理清单.md`
- `docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md`
- `HealthData/DUAL_DB_CUTOVER.md`
- `HealthShow/TECHNICAL_DOCS.md`

以上文档至少在以下事实上一致：

- 双库路由规则
- 登录态真实行为
- 前端真实路由结构
- 当前必跑回归命令
- 手表入库路径
- AI SQL 边界

---

## 4. 当前状态与剩余债

截至 2026-05-08，当前状态更接近：

- 第一轮治理和目标态 `G1-G6` 已按当前定义闭环。
- 高风险基础设施已经收口并有回归门禁。
- 后端结构拆分已经做到 `WatchDataHandler`、`DataProcessService`、`Dashboard 查询栈`，并完成核心高频域的 typed row / service 边界第一轮治理。
- `DashboardController` 已去掉历史 `DashboardMapper` 直连，Dashboard 缓存已下沉到 service，Dashboard 查询已拆出 `DashboardCalendarMapper`、`DashboardEntryMapper`、`DashboardOverviewMapper`、`DashboardDepartmentMapper` 四个主题边界。
- 前端结构拆分已经完成目标态收口：路由事实源和已迁移大页目录已固化；`workbench`、四个核心指标页、`trend-warning`、`mine-entry`、`alert-management/notifications`、`alert-management/records`、`ai-chat` 已纳入 `audit:page-structure` 完成线，当前跟踪 `21` 个页面，`legacyTrackedPages = 0`。指标页已具备共享 scroll/export/lifecycle/data-loader runtime，完整 page engine 可作为第二轮继续优化。

目标态达成后仍保留以下可接受债务：

1. 仍依赖 `v_health_record` / `v_warning_record` 的查询可继续做性能第二轮。
2. 指标页模板块仍可继续收口到更完整 page engine。
3. 高频接口的数据契约仍需通过新增接口契约治理防止裸 `Map<String,Object>` 回流。
4. actuator metrics 已是观测事实源，但还没有现场 dashboard / 告警面板。
5. 文档需要随着后续代码变化继续同步，当前版本已对齐本轮事实源。

---

## 5. 执行策略

原则只有三条：

1. 先完成“第一轮治理未收尾的骨架项”。
2. 再做“达到目标态所需的第二轮结构与契约治理”。
3. 最后用测试、观测、文档把结果钉死。

禁止做法：

- 把后端结构拆分和前端大页迁移混成一个超大批次。
- 只改结构不补验证。
- 只补脚本不改边界。
- 只写文档不压缩热点代码。

---

## 6. Phase G1：完成第一轮治理收尾

### 目标

- 把现有第一轮治理真正收稳，避免在半成品上继续叠第二轮目标。

### 重点范围

- `DashboardController`
- `DashboardService`
- `DashboardServiceImpl`
- Dashboard 主题 mapper（`DashboardCalendarMapper` / `DashboardEntryMapper` / `DashboardOverviewMapper` / `DashboardDepartmentMapper`）
- `HealthShow` 当前已拆分中的路由与大页目录

### 要做什么

- 完成 `P1-03 Dashboard 查询栈`。
- 完成 `P1-04 后端 Map 返回治理` 的第一轮。
- 固化前端路由真实来源。
- 固化前端大页目录约定。

### 完成定义

- `DashboardController` 不再直接注入或调用历史 `DashboardMapper`。
- Dashboard 慢查询缓存全部在 service 层。
- 高风险 dashboard 接口的 controller 出口不再继续裸拼装 `Map`。
- 前端路由不再同时维护两套事实源。

### 当前进度（2026-05-08）

- 后端 Dashboard 部分已完成：
  - `DashboardController` 不再直接注入或调用 `DashboardMapper`
  - Dashboard 慢查询缓存已在 `DashboardServiceImpl`
  - `DashboardService` 已移除公开 `Map` 过渡查询方法
  - 历史 `DashboardMapper` 已退出，Dashboard 查询已按 `calendar` / `entry` / `overview` / `department` 四个 mapper 主题拆分
  - `StatisticsController` 已移除异常消息直返
- 前端 G1 收尾已完成：
  - `src/router/app-routes.mjs` 是业务路由事实源，旧 `.js` 路由文件由 `audit:nav` 防回退
  - 侧栏与移动底栏从 `src/layout/menu/navigation.mjs` 派生
  - 已拆大页目录模板已写入 `HealthShow/TECHNICAL_DOCS.md`
  - `npm run audit:page-structure` 已把 `21` 个已迁移页面纳入结构门禁，`legacyTrackedPages = 0`
- `G1` 当前可视为完成；`G2` 也已在后续阶段完成目标态边界收口。

### 最小验证

- `mvn -q test -f D:\Health\HealthData\pom.xml`
- `npm run audit:structure`
- `npm run audit:nav`
- `npm run audit:api`

---

## 7. Phase G2：后端核心域脱泥

### 目标

- 把“核心查询域仍然耦合在 controller + mapper + Map 返回里”的问题打掉。

### 优先域

- Dashboard
- Realtime
- Health Portrait
- Trend Warning
- Statistics

### 要做什么

- controller 只保留参数接收、权限、Result 包装、极薄编排。
- service 承接缓存、时间范围、数据源、聚合编排。
- mapper 按主题拆分，避免继续在单文件堆大 SQL。
- 高频接口逐步用 DTO/VO 收口外部契约。

### 完成定义

- 上述高频域不再由 controller 直连 mapper。
- 核心接口字段来源清晰，新增字段不再需要跨多层盲改。
- `Map<String,Object>` 主要收缩到内部过渡层，而不是继续暴露到 controller 出口。

### 当前进度（2026-05-08）

- 核心 controller 边界已补自动化护栏：
  - `CoreControllerBoundaryTest` 覆盖 `DashboardController`、`RealtimeController`、`HealthPortraitController`、`TrendWarningController`、`StatisticsController`
  - 防止这些 controller 回退到 mapper 直连、裸 `Map<String,Object>` 出口、`Result.error(ex.getMessage())` 或局部 broad catch
- `TrendWarning` 第一批已完成：
  - mapper 行结果从 `List<Map<String,Object>>` 改为 `List<TrendWarningDailyAverageRow>`
  - 预测算法拆到 `TrendWarningPredictionCalculator`
  - `TrendWarningService` 只保留表源选择、缓存和 mapper 编排
- `Realtime` 第一批已完成：
  - 在线用户无过期缓存时的加载失败改抛 `BusinessException(503, ...)`
  - controller 不再本地翻译异常，统一交给 `GlobalExceptionHandler`
- `HealthPortrait` / `Statistics` 第一批已完成：
  - `HealthPortraitMapper` 返回 `Portrait*Row` typed row，`HealthPortraitService` 不再通过 `Map<String,Object>` 消化 mapper 行结果
  - `StatisticsMapper` 返回 `*Row` typed row，`StatisticsService` 不再通过 mapper `Map` 行结果做字段转换
  - `AiHealthReportService` 已同步适配画像员工 typed row
  - `AiHealthReportMapper` 统计查询已改为 `AiHealthStatsRow` / `AiWarningStatsRow`，AI 健康报告 prompt 构造不再通过 `Map` 字段名取值
  - `PressureMapper` 已改为 `Pressure*Row`，`PressureServiceImpl` 不再通过 mapper `Map` 行结果做字段转换
  - `BloodOxygenMapper` 已改为 `BloodOxygen*Row`，`BloodOxygenServiceImpl` 不再通过 mapper `Map` 行结果做字段转换
  - 旧 `AiReportService` / `AiReportScheduler` 已从 `SqlExecutorMapper` 动态 SQL 摘出，改走参数化 `AiReportMapper` + typed row；`SqlExecutorMapper` 当前只保留给 AI 对话 Text2SQL 动态列场景
- `Dashboard` 第二批已完成：
  - `DashboardServiceImpl` 已拆出 `DashboardCalendarQueryService`、`DashboardEntryQueryService`、`DashboardDepartmentQueryService`
  - `DashboardServiceImpl` 当前约 `458` 行，calendar、entry、department/person/comparison 查询主题已从主 service 中移出
  - `DashboardCalendarQueryService` / `DashboardEntryQueryService` 已切到 typed row，calendar、entry、day-rank 子域不再消费 `Map<String,Object>` 行结果
  - 新增 `DashboardOverviewMapper` 承接首页总览、设备活跃、预警事件、预警小时分布和日汇总查询
  - 历史 `DashboardMapper` 已退出，部门/人员统计主题已迁到 `DashboardDepartmentMapper`
  - `AiChatService` 的 Text2SQL 动态结果已封装为 `AiSqlResultSet(columns, rows, rowCount)`，前端 AI 聊天 `[DATA]` 事件兼容新旧数据形态
  - 已新增/调整 `DashboardCalendarQueryServiceTest`、`DashboardEntryQueryServiceTest`、`DashboardDepartmentQueryServiceTest`、`DashboardServiceImplTest`
  - 新增/调整 `BloodOxygenServiceImplTest`、`AiReportServiceTest`

`G2` 当前状态：

- 目标态要求的后端核心域边界已满足：核心 controller 护栏、Dashboard 主题 mapper、主要统计/画像/趋势 typed row、AI 动态结果集封装均已落地。
- 后续仍可继续优化仍依赖 `v_health_record` / `v_warning_record` 的查询性能，但这属于性能第二轮，不再是 `G2` 结构达标阻塞。

### 最小验证

- 后端完整测试通过。
- 新增 dashboard/realtime/portrait/trend-warning 相关 service 测试。

---

## 8. Phase G3：前端入口与页面骨架统一

### 目标

- 结束“页面能跑，但结构漂移、入口分裂、菜单和路由可能不同步”的状态。

### 重点范围

- `src/router/*`
- `src/layout/*`
- `src/views/health-monitor/dashboard/*`
- `src/views/health-monitor/real-time/*`
- `src/views/safety-command/*`
- `src/views/personnel-management/health-portrait/*`
- `src/views/health-monitor/employee-profile/*`

### 要做什么

- 收口路由、菜单、移动导航、权限来源。
- 固化大页目录模板：
  - `page-state`
  - `runtime`
  - `view-model`
  - `actions`
  - `components`
  - `scss`
- 清理旧路由文件和兼容分支。
- 让 `index.vue` 只保留页面壳与组合关系。

### 完成定义

- 关键页面结构一致，后续新增功能默认落在对应模块，不再重新堆回页面根文件。
- 路由与菜单变更能被 `audit:nav` 及时发现。
- 登录态、数据源头部注入、导航显示逻辑不再散落多处。

### 当前进度（2026-05-08）

- `src/router/app-routes.mjs` 是业务路由事实源，`src/layout/menu/navigation.mjs` 派生侧栏与移动底栏。
- `audit:page-structure` 当前跟踪 `21` 个页面，新增覆盖 `trend-warning`、`mine-entry`、`alert-management/notifications`、`alert-management/records`、`ai-chat`。
- `ai-chat` 已拆出图表、导出、查询结果归一化、会话快捷问题和 Markdown 文本处理模块，`index.vue` 降到约 `331` 行。
- `trend-warning` 已把内联 `SparkLine` 图表组件拆出，`mine-entry` 已把准入展示判断拆出，预警通知/记录已拆出样式模块。
- 已通过 `npm run audit:structure` 与 `npm run build`。

### 最小验证

- `npm run audit:nav`
- `npm run audit:auth`
- `npm run audit:api`

---

## 9. Phase G4：前端大页与副作用治理

### 目标

- 把“页面能工作但内部非常难改”的状态压回可维护区间。

### 优先页面

- `heart-rate`
- `blood-oxygen`
- `pressure`
- `blood-pressure`
- `sleep`
- `device-management`
- `report-center`
- `user-list`
- `role-management`
- `employee-archive`

### 要做什么

- 五类指标页抽共用 runtime/page engine。
- 页面副作用统一迁移到 `useIntervalTask`、`useTimeoutTask`、`useScrollLoop` 或同级抽象。
- 图表初始化、resize、dispose 进入统一生命周期。
- 大页对话框、图表配置、数据组装从 `index.vue` 拆出。

### 完成定义

- 指标页不再整页复制。
- 核心页面离开后不残留无主 timer、listener、chart 实例。
- 高频页面的改动点能稳定收敛到 1 到 3 个模块，而不是整页搜代码。

### 当前进度（2026-05-08）

- 五类指标页的第一层共享 runtime 已启动：
  - 新增 `src/views/health-monitor/metric-page/metric-scroll.js`
  - `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 的 Top5 自动滚动生命周期已统一走 `startMetricTop5Scroll` / `stopMetricTop5Scroll`
  - `heart-rate` 保留暂停滚动参数，其余页面使用统一默认滚动节奏
  - 新增 `src/views/health-monitor/metric-page/metric-export.js`
  - `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 的 Excel 导出空数据提示、文件名日期和成功提示已统一，页面只保留字段映射和列定义
  - 新增 `src/views/health-monitor/metric-page/metric-page-mixin.js`
  - 四个核心指标页的 Top5 启停和卸载清理由 `metric-page-mixin` 统一承接
  - 新增 `src/views/health-monitor/metric-page/metric-data-loader.js`
  - 四个核心指标页已实际接入共享数据加载、导出和 Top5 lifecycle 层
  - `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 已拆出同目录 `*-chart.js` 和 `*.scss`
  - `npm run audit:page-structure` 已将四个核心指标页加入结构门禁
- `workbench` 第一层拆分已启动：
  - 新增 `src/views/health-monitor/workbench/workbench-view-model.js`，承接日历格子、摘要、排行状态类和部门体征状态类
  - 新增 `src/views/health-monitor/workbench/workbench-chart.js`，承接部门雷达图 option 和评分归一化
  - 新增 `src/views/health-monitor/workbench/workbench.scss`，`workbench/index.vue` 已纳入 `audit:page-structure`
  - 部门雷达图已接入 `createEventBinding` 管理 resize，并在 `onUnmounted` 中释放图表实例
- 已迁移页面的页面级 DOM 事件监听已进一步收口：
  - `dashboard`、`real-time`、`report-center`、`health-portrait`、`safety-command` 的 resize / visibility / fullscreen 监听统一走 `createEventBinding`
  - 当前健康监测与关键大页搜索只剩 Dashboard 一处一次性重试 `setTimeout`，不再残留页面级手写 add/removeEventListener
- 新增纳入门禁页面：
  - `trend-warning`：`index.vue + TrendSparkLine.js + trend-warning.scss`
  - `mine-entry`：`index.vue + mine-entry-view-model.js + mine-entry.scss`
  - `alert-management/notifications`：`index.vue + notifications.scss`
  - `alert-management/records`：`index.vue + records.scss`
  - `ai-chat`：`index.vue + ai-chat-chart/export/query-result/session/text/scss`
- 验证已通过：
  - `npm run audit:structure`
  - `npm run build`

### 最小验证

- `npm run audit:e2e`
- `npm run audit:pipeline`

---

## 10. Phase G5：健壮度和回归门禁补齐

### 目标

- 让“结构变好了”真正转化为“后续不容易再坏掉”。

### 要做什么

- 补齐后端第二层业务测试：
  - Dashboard service
  - Realtime service
  - Health portrait query/service
  - Trend warning service
  - DTO/VO 映射与异常语义
- 固化前端最小门禁：
  - `audit:nav`
  - `audit:api`
  - `audit:auth`
  - `audit:e2e`
  - `audit:pipeline`
- 明确哪些脚本是本地必跑，哪些是流水线必跑。
- 为关键失败场景补负向用例：
  - 错库
  - 401 收敛
  - 坏 buffer 数据
  - AI SQL 越界
  - 手表模拟器/真实设备分流

### 完成定义

- 关键结构改动不再只能靠人工回归。
- 新人接手时能明确知道“改完至少要跑什么”。
- 关键风险链路有可重复的失败样例。

### 当前进度（2026-05-08）

- 后端完整测试已通过：`mvn -q test -f D:\Health\HealthData\pom.xml`。
- 前端主门禁已通过：
  - `npm run audit:structure`
  - `npm run audit:api`
  - `npm run audit:write`
  - `npm run audit:auth`
  - `npm run audit:e2e`
  - `npm run audit:pipeline`
  - `npm run audit:pipeline-warning`
  - `npm run build`
- `audit:write` 已从“等待后台模拟器产生新记录”改为主动 TCP `9000` 探针，覆盖手表写入、SQL 可见、健康记录 API 可见、realtime user API 可见，并在验证后清理探针健康记录。
- `HealthMetricsServiceTest` 已补入，防止关键 actuator metric family 从启动可见状态回退。

`G5` 当前可视为完成。

---

## 11. Phase G6：观测、文档、交付收口

### 目标

- 把“这套结构现在能工作”沉淀成可交接的长期状态。

### 要做什么

- 补齐日志和指标：
  - AI 调用耗时、拒绝次数、自动修复次数
  - Redis buffer 长度、flush 成功/失败
  - 双库 source 命中和写入量
  - 当前在线手表数
  - 预警生成量与去重命中量
  - 慢查询统计
- 同步文档事实源。
- 形成一份最终 handoff 文档：
  - 当前架构边界
  - 当前回归入口
  - 当前高风险操作清单
  - 当前未解决但可接受的剩余债

### 完成定义

- 遇到现场问题时，能先看日志、指标、回归入口，而不是先问人。
- 文档之间不再大面积互相打架。

### 当前进度（2026-05-08）

- `http://localhost:8080/health/actuator/metrics` 已在后端启动后直接暴露关键业务 metric family：
  - `health.ai.call.duration`
  - `health.ai.reject.total`
  - `health.ai.sql.auto_repair.total`
  - `health.buffer.queue.size`
  - `health.buffer.push.total`
  - `health.buffer.flush.total`
  - `health.buffer.dead_letter.total`
  - `health.datasource.request.total`
  - `health.datasource.watch.route.total`
  - `health.watch.online.count`
  - `health.warning.generated.total`
  - `health.warning.dedup.total`
  - `health.sql.statement.duration`
  - `health.sql.slow.total`
- `AGENTS.md`、`docs/archive/历史归档-HEALTH_HANDOFF.md`、`docs/archive/历史归档-治理清单.md`、`docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md`、`HealthData/DUAL_DB_CUTOVER.md`、`HealthShow/TECHNICAL_DOCS.md` 的事实源已围绕双库、登录态、路由、回归入口、手表入库、AI SQL 边界完成同步。
- 最终 handoff 已更新到 `D:/Health/docs/archive/历史归档-HEALTH_HANDOFF.md`，包含当前架构边界、回归入口、高风险操作清单、可接受剩余债。

`G6` 当前可视为完成。

---

## 12. 执行顺序

推荐顺序如下：

1. `Phase G1`
2. `Phase G2`
3. `Phase G3`
4. `Phase G4`
5. `Phase G5`
6. `Phase G6`

原因很简单：

- `G1` 已完成，后续先推进 `G2`，避免后端核心域继续失控。
- `G2` 不完成，后端核心域还是会继续失控。
- `G3` 不完成，前端再拆页面也会继续漂。
- `G4` 不完成，前端仍然难改。
- `G5` 不完成，结构好转也很快会再次回退。
- `G6` 不完成，交接和长期维护成本仍然偏高。

---

## 13. 并行规则

可以并行：

- 后端 `G2` 和前端 `G3/G4` 可以分主题推进。
- 测试补齐和观测补齐可以在结构拆分接近稳定后并行补入。

不要并行：

- 不要把双库基础设施改造和前端大页迁移混做。
- 不要在同一批次同时重构 Dashboard 后端和 Dashboard 前端。
- 不要在没有完成最小验证前继续叠下一批结构改动。

---

## 14. 达到目标后的项目状态

如果本计划全部执行完，项目应达到下面的状态：

- 仍然有历史技术债，但已经不是“改一个地方，哪里都会炸”的状态。
- 关键业务域有清晰边界，新增功能默认有落点。
- 大类和大页的继续膨胀趋势被遏制。
- 双库、登录、Redis、AI、手表链路不再是高不确定性黑盒。
- 日常改动有明确回归入口，失败场景有明确定位路径。

更直白地说：

- 这时项目不会变成“教科书级架构”。
- 但可以合理称为“从不可控屎山，降到了可控、可维护的工程状态”。

---

## 15. 完成判定

只有下面条件同时满足，才允许对外说“屎山代码和健壮度问题已基本解决”：

1. `G1` 到 `G6` 全部完成。
2. 后端完整测试通过。
3. 前端 `audit:structure`、`audit:nav`、`audit:page-structure`、`audit:api`、`audit:write`、`audit:auth`、`audit:e2e`、`audit:pipeline`、`audit:pipeline-warning`、`npm run build` 通过。
4. 前后端关键热点域的结构规范已经稳定，不再靠口头约束。
5. 文档事实源同步完成。

当前状态（2026-05-08）：

- 以上 5 项已按当前目标态定义满足。
- 可以对外说“屎山代码和健壮度问题已基本解决到可控状态”。
- 这不等于历史债清零；后续仍可继续做性能第二轮、指标页 page engine 第二轮、现场 dashboard / 告警面板。

缺任何一项时，只能说：

- 第一轮治理完成。
- 或第二轮治理推进中。

不能说“目标已经达到”。



