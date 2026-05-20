# CODEX Multi-Day Taskbook

> 历史归档提示：这是第一轮多日治理手册，很多 Phase 已在 2026-05-08 前后收口。新开 goal-mode 不要从本文件机械重跑 P0/P1；当前执行入口优先看 `D:/Health/AGENTS.md` 和 `D:/Health/目标达成实施计划.md`。

更新时间：2026-05-08

用途：

- 这是给 Codex 连续执行几天治理任务用的执行手册。
- 目标不是一次性“做完所有技术债”，而是按 phase 连续推进。
- 如果目标已经从“第一轮治理”切换到“达到可维护目标态”，请同时查看 `D:/Health/CODEX_TARGET_STATE_PLAN.md`。
- 每个 phase 都要求：
  - 有明确边界
  - 有明确输入输出
  - 有明确验收
  - 能独立停下来交接

---

## 1. 总目标

### Goal

在不破坏当前可运行环境的前提下，完成 Health 项目的第一轮系统治理，重点解决：

- Redis 缓冲链路毒化风险
- AI SQL 安全边界不硬的问题
- 双库切换边界不清的问题
- 异步线程与数据源上下文耦合的问题
- 超大后端类与超大前端页面的持续膨胀问题
- 测试、监控、文档不足的问题

### 最终交付

- 关键 P0 风险项收口
- 关键 P1 结构项完成第一轮拆分
- 回归脚本可复用
- 文档可交接

### Current Status（2026-05-08）

整体完成度估算：

- 当前建议对外口径为：`CODEX_TARGET_STATE_PLAN.md` 定义的目标态已达成。
- `Phase 0` 到 `Phase 5` 第一轮治理已完成，目标态 `G1-G6` 已闭环。
- `Phase 4 / G3 / G4` 已把路由事实源、导航派生、大页目录和 `21` 个页面结构门禁固化；指标页 page engine 仍可继续第二轮，但不是目标态阻塞。
- 后续不再按“继续执行所有治理 phase”推进，只处理明确回归、性能第二轮、新增接口契约治理或指标页 page engine 第二轮。

已完成：

- `Phase 0` 已完成。
- `Phase 1` 已完成。
- `Phase 2` 已完成。
- `Phase 3` 已完成 `P1-01 WatchDataHandler`、`P1-02 DataProcessService`、`P1-03 Dashboard 查询栈` 后端第一轮拆分。
- `audit:auth` 已补入并通过，登录页 `admin / admin123` 预填、刷新恢复、退出、后端重启、并发 `401` 收敛都已有基线。
- 前端回归脚本已补到第一轮：
  - `audit:nav`
  - `audit:page-structure`
  - `audit:structure`
  - `audit:preflight`
  - `audit:auth`
  - `audit:e2e`
  - `audit:pipeline`
  - `audit:pipeline-warning`
- 双库与异步治理层的第一轮关键证据已补齐：
  - 请求侧 `Header/Cookie` 切库测试
  - 模拟器 IMEI 命中老库测试
  - Redis `health:buffer:old/new` push 与 flush 测试
  - `MonthlyTableScheduler` 三个双库入口测试
  - `DataProcessService` 缓冲写入上下文测试
  - `HealthAsyncQueryExecutor` 上下文跨线程测试
- `WatchDataHandler` 已完成第一轮结构拆分：
  - 主入口已收口为 `dispatcher + protocol registry`
  - 协议实现已迁移到 `HealthData/src/main/java/com/xzkj/health/handler/watch/*`
  - 已补 `WatchDataHandlerTest`
- `DataProcessService` 已完成第一轮结构拆分：
  - 主类保留外部协议入口方法签名，约 `400` 行
  - 设备上下文拆到 `WatchDeviceContextService`
  - 未绑定缓冲、绑定后 Redis 入队、健康记录映射拆到 `WatchDataPersistenceService`
  - 健康阈值预警拆到 `WatchHealthWarningService`
  - 行为类报警拆到 `WatchBehaviorAlertService`
  - 已补 `DataProcessServiceTest`、`WatchDeviceContextServiceTest`、`WatchDataPersistenceServiceTest`、`WatchHealthWarningServiceTest`
- `P1-03 Dashboard 查询栈` 后端第一轮已完成：
  - `DashboardController` 不再注入或调用 `DashboardMapper`
  - Dashboard 慢查询缓存已在 `DashboardServiceImpl`
  - `DashboardService` 不再公开内部 `Map` 过渡查询方法
  - 已拆出 `DashboardCalendarMapper` 与 `DashboardEntryMapper`
  - 历史 `DashboardMapper` 已退出，Dashboard 查询已拆到 `DashboardCalendarMapper`、`DashboardEntryMapper`、`DashboardOverviewMapper`、`DashboardDepartmentMapper`
  - `StatisticsController` 已移除异常消息直返
- `Phase G1` 前端收尾已完成：
  - `app-routes.mjs` 成为业务路由事实源
  - `layout/menu/navigation.mjs` 成为侧栏和移动底栏派生入口
  - `audit:page-structure` 已把 `21` 个已迁移页面纳入目录结构门禁
- `Phase G2` 第一批已完成：
  - `TrendWarningMapper` 返回 `TrendWarningDailyAverageRow`
  - `TrendWarningService` 只保留表源、mapper、缓存编排，预测算法下沉到 `TrendWarningPredictionCalculator`
  - `RealtimeController` 移除本地 `Result.error(ex.getMessage())`，实时列表无缓存失败改走 `BusinessException(503, ...)`
  - 新增 `CoreControllerBoundaryTest` 防止核心 controller 回退到 mapper 直连、裸 `Map` 或局部异常拼接
  - `HealthPortraitMapper` / `StatisticsMapper` 返回 typed row，画像和统计 service 不再通过 mapper `Map` 行结果做字段转换
  - `AiHealthReportService` 已同步适配画像员工 typed row
  - `AiHealthReportMapper` 统计查询已改为 `AiHealthStatsRow` / `AiWarningStatsRow`，AI 健康报告 prompt 构造不再通过 `Map` 字段名取值
  - `PressureMapper` 已改为 `Pressure*Row`，`PressureServiceImpl` 不再通过 mapper `Map` 行结果做字段转换
  - `BloodOxygenMapper` 已改为 `BloodOxygen*Row`，`BloodOxygenServiceImpl` 不再通过 mapper `Map` 行结果做字段转换
  - 旧 `AiReportService` / `AiReportScheduler` 已改走参数化 `AiReportMapper` + typed row，不再通过 `SqlExecutorMapper` 拼接部门名或工号 SQL
  - `DashboardServiceImpl` 已拆出 `DashboardCalendarQueryService`、`DashboardEntryQueryService`、`DashboardDepartmentQueryService`
  - `DashboardOverviewMapper` 已拆出首页总览、设备活跃、预警事件、预警小时分布和日汇总查询
  - `DashboardCalendarQueryService` / `DashboardEntryQueryService` 已切到 typed row，calendar、entry、day-rank 子域不再消费 mapper `Map` 行结果
  - `DashboardServiceImpl` 当前约 `458` 行，历史 `DashboardMapper` 已退出，新增/调整对应 service 单测
  - `AiChatService` 的 Text2SQL 动态结果已封装为 `AiSqlResultSet(columns, rows, rowCount)`，前端 AI 聊天 `[DATA]` 事件兼容新旧数据形态
- `mvn -q test -f D:\Health\HealthData\pom.xml` 当前通过。
- `npm run audit:api` 当前仍通过，最新结果为 `53 checks / 0 failed / 0 warning`，最新产物 `D:/Health/HealthShow/tests/api/artifacts/2026-05-07T18-15-23-368Z/summary.md`。
- `npm run audit:nav` 当前通过，结果为 `6 navGroups / 23 visibleNavLeaves / 5 mobileNavItems`。
- `npm run audit:page-structure` 当前通过，已把 `21` 个已迁移页面纳入目录结构门禁，`legacyTrackedPages = 0`。
- `npm run audit:structure` 当前通过，连续执行 `audit:nav` 与 `audit:page-structure`。

最终收口（2026-05-08）：

- 新 Codex 不要再重做 `Phase 0` / `Phase 1` / `Phase 2`，除非发现明确回归。
- 不要回头重做 `P1-01 WatchDataHandler` 第一轮拆分，除非出现明确协议回归或需要继续细拆 parser/handler。
- 不要回头重做 `P1-02 DataProcessService` 第一轮拆分，除非出现明确入库、预警或双库上下文回归。
- 不要回头重做 `P1-03 Dashboard 查询栈` 后端第一轮，除非出现明确 Dashboard 接口回归。
- 后端 `G2` 目标态边界已基本满足，后续只处理明确回归或性能第二轮。
- 前端 `Phase G3/G4` 已把 `21` 个页面纳入结构门禁；指标页 page engine 仍可做第二轮，但不再是目标态阻塞。
- `Phase G5/G6` 已完成：完整回归、`audit:write` 主动 TCP 探针、启动即暴露的 actuator metric family、文档事实源同步均已落地。
- 当前可以按 `CODEX_TARGET_STATE_PLAN.md` 的定义视为目标态达成。

接手提醒：

- 先看 `D:/Health/AGENTS.md` 与 `D:/Health/HealthData/DUAL_DB_CUTOVER.md`。
- 不要再把登录页 `admin / admin123` 预填清掉；这是当前新库现场的管理员 bootstrap 入口。
- 后端继续保持结构拆分主题化；前端结构拆分按独立主题推进，不要混做。
- 下一入口不要从 `P1-03 Dashboard 查询栈` 重新开始；后端只处理明确回归或性能第二轮，前端只处理新增大页、明确回归或指标页 page engine 第二轮。
- 如果怀疑双库行为回归，先重跑 `HealthDataSourceRequestFilterTest`、`WatchDataSourceResolverTest`、`RedisHealthBufferServiceTest`、`MonthlyTableSchedulerTest`、`DataProcessServiceTest`，再继续结构拆分。

下一轮 Codex 开工块：

- 先跑：
  - `git -C D:\Health\HealthShow status --short`
  - `git -C D:\Health\HealthData status --short`
- 不要重复执行：
  - `DashboardController` 去 `DashboardMapper`
  - Dashboard controller 缓存下沉
  - `DashboardService` 公开 `Map` 方法内收
  - `DashboardCalendarMapper` / `DashboardEntryMapper` 第一轮拆分
- 后端下一轮建议改动范围：
  - `HealthData/src/main/java/com/xzkj/health/service/impl/DashboardServiceImpl.java`
  - `HealthData/src/main/java/com/xzkj/health/service/dashboard/*`
  - `HealthData/src/main/java/com/xzkj/health/mapper/DashboardDepartmentMapper.java`
  - `HealthData/src/main/java/com/xzkj/health/service/RealtimeService.java`
  - `HealthData/src/main/java/com/xzkj/health/service/HealthPortraitService.java`
  - `HealthData/src/main/java/com/xzkj/health/service/StatisticsService.java`
  - 必要时新增更细主题 service/mapper
- 前端下一轮建议改动范围：
  - `HealthShow/src/router/*`
  - `HealthShow/src/layout/*`
  - 已拆分中的大页目录
- 验证命令：
  - `mvn -q test -f D:\Health\HealthData\pom.xml`
  - `npm run audit:structure`（在 `D:\Health\HealthShow`）
  - `npm run audit:api`（在 `D:\Health\HealthShow`）
  - `npm run audit:write`（在 `D:\Health\HealthShow`）
  - `npm run audit:auth`（在 `D:\Health\HealthShow`，会重启后端）
  - `npm run audit:e2e`（在 `D:\Health\HealthShow`）
  - `npm run audit:pipeline`（在 `D:\Health\HealthShow`）
  - `npm run audit:pipeline-warning`（在 `D:\Health\HealthShow`）

---

## 2. 总约束

### 代码约束

- 不回滚用户已有未提交改动。
- 不做无必要的大范围格式化。
- 不混做前后端超大改造。
- 每个 phase 尽量控制在单一主题。

### 仓库约束

- `D:/Health/HealthShow` 与 `D:/Health/HealthData` 分仓处理。
- 所有 git 操作必须分别在子仓库进行。

### 验证约束

- 每个 phase 结束必须执行最小验证。
- 关键 phase 必须补测试，而不是只手测。

### 运行约束

- 默认维持以下本地链路可工作：
  - SQL Server `58135`
  - Backend `8080`
  - TCP `9000`
  - Frontend `9528`
  - Simulator 正常连入

---

## 3. 开始前固定动作

每次开始新 phase 前，先做：

1. 分别检查两个仓库工作树状态。
2. 记录本 phase 要动的文件范围。
3. 明确本 phase 的非目标。
4. 跑最小基线：
   - 前端至少 `npm run audit:api`
   - 后端至少跑与本 phase 相关的单测
5. 开始改动前确认当前服务在线。

建议固定命令：

```powershell
git -C D:\Health\HealthShow status --short
git -C D:\Health\HealthData status --short
```

```powershell
Invoke-WebRequest 'http://localhost:8080/health' -UseBasicParsing
Invoke-WebRequest 'http://localhost:9528/' -UseBasicParsing
```

---

## 4. Phase 0：基线与护栏（已完成）

### 目标

- 先把治理过程本身的基线固化。
- 防止后续多天治理中反复失去上下文。

### 要做什么

- 检查并确认：
  - 前端、后端、模拟器、数据库在线
  - 回归命令可执行
- 在根目录保留最新治理文档：
  - `docs/archive/历史归档-治理清单.md`
  - `docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md`
- 列出本轮治理的文件写入策略：
  - 哪些 phase 主要动后端
  - 哪些 phase 主要动前端

### 不做什么

- 不改业务代码。

### 验收

- 治理文档存在。
- 基线命令可跑。

---

## 5. Phase 1：P0 止血层（已完成）

### 目标

- 优先修复会造成“持续性错误、堆积、错库、拖垮线程、暴露默认口令”的风险。

### 范围

后端优先：

- `RedisHealthBufferService`
- `AiSqlGuard`
- `AiChatService`
- `SqlExecutorMapper`
- `DeepSeekClient`
- `HealthDataSourceRequestFilter`
- `HealthDataSourceContext`
- `WatchDataSourceResolver`
- `AsyncExecutorConfig`
- `GlobalExceptionHandler`

前端与配置配套：

- `login/index.vue`
- `request.js`
- `auth.js`
- `application.yml`
- `SaTokenConfig`

### 任务项

1. 修 Redis buffer 毒化问题。
2. 给 AI SQL 增加硬性限制。
3. 收口 Header/Cookie 切库能力。
4. 统一异步线程与数据源上下文携带方式。
5. 重做 AI 线程池背压策略。
6. 收口 CORS、Druid、默认凭据。
7. 统一异常返回。

说明：

- 双库切换现场的登录页是例外约束。
- 当前 `health_new` 需要保留 `admin / admin123` 作为可直接登录的管理员入口，并在登录页预填和展示。
- 在替代 bootstrap 入口落地前，不要再按旧 P0-06 文案把这组默认凭据从 UI 清掉。

### 最小验证

```powershell
npm run audit:api
```

```powershell
mvn -q test -f D:\Health\HealthData\pom.xml
```

```powershell
npm run audit:auth
```

补充验证：

- 人工注入坏 buffer 数据时，flush 仍可处理正常记录。
- AI 查询超大明细时被拒绝或受控收敛。
- 普通请求不能随意切换 `old/new` 源。
- `audit:auth` 应覆盖预填 `admin / admin123`、刷新恢复、退出、后端重启、并发 401 收敛。

### 交付标准

- P0 风险项至少完成第一轮收口。
- 所有新增护栏都有回归。

当前结果：

- 本阶段已完成，可视为当前稳定基线。
- 新起一轮工作时，不要再回头重新执行一遍 P0 改造，除非发现明确回归。

---

## 6. Phase 2：双库与异步治理层（已完成）

### 目标

- 把“错库风险”从隐式行为变成显式规则。
- 把双库行为验证补齐到可重复执行，而不是继续依赖人工切页面和看日志。

### 范围

- `config/datasource/*`
- `DataProcessService`
- `RedisHealthBufferService`
- `MonthlyTableScheduler`
- 所有使用 `CompletableFuture.supplyAsync` 的数据库查询代码

### 任务项

1. 复核并补齐统一异步执行器的覆盖范围，继续清理残留裸 `supplyAsync` 数据库查询入口。
2. 补齐双库行为测试：
   - 请求侧切库
   - 模拟器侧切库
   - flush 到不同库
   - 定时任务轮询双库
3. 增加双库相关观测日志。
4. 把测试和日志优先落在已改过的双库基础设施上，不在这一阶段扩散到页面重构或大类拆分。

建议起手顺序：

1. 先补 `HealthDataSourceRequestFilter` / 请求侧切库测试。
2. 再补模拟器 IMEI 命中老库测试。
3. 再补 Redis `health:buffer:old` / `health:buffer:new` flush 测试。
4. 最后补 `MonthlyTableScheduler` 和并行查询链路的双库一致性测试。

### 不做什么

- 不在这一 phase 大拆 dashboard 页面。

### 验收

- 双库相关测试可重复执行。
- 并行查询不丢数据源上下文。
- 模拟器命中旧库逻辑可验证。
- 关键双库行为在日志中可追踪，不再只能靠人工读代码判断。

当前结果：

- 以上四项均已满足第一轮验收。
- 新增或增强的回归集中在：
  - `HealthDataSourceRequestFilterTest`
  - `WatchDataSourceResolverTest`
  - `RedisHealthBufferServiceTest`
  - `MonthlyTableSchedulerTest`
  - `DataProcessServiceTest`
  - `HealthAsyncQueryExecutorTest`
- 已补最小双库观测日志，足以在本地继续排查 source 漂移问题。
- 本阶段后续如无回归，不再继续堆相同类型的基础测试，直接进入结构拆分。

---

## 7. Phase 3：后端结构拆分层

### 目标

- 优先拆掉最危险的超大后端类和耦合最深的链路。

### 范围

优先顺序：

1. `WatchDataHandler`
2. `DataProcessService`
3. `DashboardController`
4. `DashboardServiceImpl`
5. Dashboard 主题 mapper
6. `AiChatService`

### 任务项

1. 把协议入口拆成 dispatcher + strategy。
2. 把设备数据处理拆成多个职责服务。
3. dashboard controller 去 mapper 直连。
4. dashboard 缓存下沉到 service。
5. 高频接口引入 DTO/VO。
6. 把 mapper 超长查询按主题拆开。

### 方法

- 先做“结构拆分不改语义”。
- 再做“边界收口和类型替换”。
- 最后补单测和回归。

### 验收

- 新增协议和新增 dashboard 图表时，不再强制改超级类。
- dashboard 关键接口行为保持不变。

### 当前结果（2026-05-07）

- `P1-01` 已完成第一轮验收：
  - `WatchDataHandler` 已从 `1162` 行收口到 `144` 行 dispatcher
  - 已建立协议注册表与独立 handler 目录 `handler/watch/*`
  - 登录、定位、心跳、报警、健康、通用上行、下行响应已拆开
- 已补最小协议分发回归：
  - `WatchDataHandlerTest`
  - 组合验证命令：`mvn -q "-Dtest=WatchDataHandlerTest,DataProcessServiceTest" test -f D:\Health\HealthData\pom.xml`
- 仍未完成的 `P1-01` 延伸项：
  - 定位解析进一步下沉到 parser/service
  - 更细粒度 handler 单测
  - 真实 TCP/SCTP 端到端回归
- `P1-02` 已完成第一轮验收：
  - `DataProcessService` 已从约 `620` 行收口到 `400` 行，外部协议入口方法签名保持不变
  - 新增 `service/watch/*` 承接设备上下文、持久化路由、健康预警、行为报警
  - 已补 `DataProcessServiceTest`、`WatchDeviceContextServiceTest`、`WatchDataPersistenceServiceTest`、`WatchHealthWarningServiceTest`
  - 组合验证命令：`mvn -q "-Dtest=DataProcessServiceTest,WatchDeviceContextServiceTest,WatchDataPersistenceServiceTest,WatchHealthWarningServiceTest,WatchDataHandlerTest" test -f D:\Health\HealthData\pom.xml`
  - 完整后端验证：`mvn -q test -f D:\Health\HealthData\pom.xml`
  - 前端 API 基线：`npm run audit:api`，结果 `31 checks / 0 failed / 0 warning`
- 仍未完成的 `P1-02` 延伸项：
  - 协议字段标准化/校验继续从 `DataProcessService` 下沉
  - 上报事件 DTO 替代散落的 `Map<String,Object>`
  - 行为报警服务补更细粒度单测
  - 真实 TCP 端到端回归
- `P1-03` 后端第一轮结果：
  - `DashboardController` 已收口到约 `213` 行，不再直接注入或调用 `DashboardMapper`。
  - `DashboardServiceImpl` 已承接 mapper 调用、日期范围解析、表源选择、慢查询缓存。
  - 历史 `DashboardMapper` 已退出，Dashboard 查询已拆出 `DashboardCalendarMapper`、`DashboardEntryMapper`、`DashboardOverviewMapper`、`DashboardDepartmentMapper`。
  - 所有 `/dashboard/*` 路径、参数名、返回字段名保持不变。
  - 已通过后端完整测试、前端 `audit:api`、前端 `audit:nav`。
- `P1-03` / `G2` 后续进展：
  - `DashboardServiceImpl` 已从约 `841` 行继续降到约 `458` 行。
  - calendar、entry、department/person/comparison 查询主题已拆入 `HealthData/src/main/java/com/xzkj/health/service/dashboard/*`。
  - `DashboardCalendarQueryService` / `DashboardEntryQueryService` 已切到 typed row，calendar、entry、day-rank 子域不再消费 mapper `Map` 行结果。
  - `HealthPortraitMapper`、`StatisticsMapper` 已改 typed row，相关 service 不再消费 mapper `Map` 行结果。
  - `AiHealthReportMapper` 统计查询已改为 `AiHealthStatsRow` / `AiWarningStatsRow`，AI 健康报告 prompt 构造不再通过 `Map` 字段名取值。
  - `PressureMapper` 已改为 `Pressure*Row`，`PressureServiceImpl` 不再通过 mapper `Map` 行结果做字段转换。
  - `BloodOxygenMapper` 已改为 `BloodOxygen*Row`，`BloodOxygenServiceImpl` 不再通过 mapper `Map` 行结果做字段转换。
  - 旧 `AiReportService` / `AiReportScheduler` 已改走参数化 `AiReportMapper` + typed row，不再通过 `SqlExecutorMapper` 拼接部门名或工号 SQL。
- `P1-03` 后续仍未完成：
  - 历史 `DashboardMapper` 已退出，部门/人员统计主题已迁到 `DashboardDepartmentMapper`。
  - `AiChatService` 的 Text2SQL 动态列结果已封装为 `AiSqlResultSet`，保留动态列能力，同时向前端提供 `columns/rows/rowCount` 结果集契约。
  - 前端 Dashboard 页面拆分不属于本轮后端第一刀结果。

---

## 8. Phase 4：前端结构拆分层

### 目标

- 把多个 800 到 1100 行的大页面压回到可维护区间。

### 范围

优先页面：

- `health-monitor/heart-rate`
- `health-monitor/blood-oxygen`
- `health-monitor/pressure`
- `health-monitor/blood-pressure`
- `health-monitor/sleep`
- `health-monitor/real-time`
- `device-management`
- `report-center`
- `user-list`
- `role-management`

### 任务项

1. 指标分析页框架化。
2. 共用 runtime / state / actions / chart lifecycle。
3. 清理手写 timer、listener、chart dispose。
4. 统一移动端与桌面端页面收口方式。
5. 路由与菜单元数据收口。

### 方法

- 先抽框架页。
- 再迁移两个页面验证抽象是否稳定。
- 稳定后批量迁移其他相似页面。

### 验收

- 五类指标页不再各自复制相似逻辑。
- 关键页面拆分后功能不回退。
- 路由与菜单回归可通过。

### 当前结果（2026-05-07）

- `HealthShow` 已出现第一轮前端结构拆分与路由收口工作树：
  - `router/index.js`、`router/app-routes.mjs`、`router/health-monitor.mjs`、`router/alert-management.mjs`、`router/app-route-access.js` 已开始收口
  - `layout/*`、`views/health-monitor/dashboard/*`、`views/health-monitor/real-time/*`、`views/safety-command/*`、`views/personnel-management/health-portrait/*` 已进入模块化拆分
  - `tests/navigation-structure.mjs`、`tests/preflight.mjs`、`tests/e2e/auth-session-regression.mjs` 已加入前端回归入口
- 2026-05-08 已启动五类指标页共享 runtime 第一刀：
  - 新增 `src/views/health-monitor/metric-page/metric-scroll.js`
  - `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 的 Top5 自动滚动生命周期已统一
  - 新增 `src/views/health-monitor/metric-page/metric-export.js`
  - `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 的 Excel 导出外壳已统一，页面保留各自字段映射
  - 新增 `src/views/health-monitor/metric-page/metric-page-mixin.js`，四个核心指标页的 Top5 启停和卸载清理已统一
  - `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 已拆出同目录 `*-chart.js` 和 `*.scss`
  - `workbench` 已拆出 `workbench-view-model.js` 与 `workbench-chart.js`，日历派生、排行样式、部门雷达图 option 不再堆在页面根文件
  - `workbench` 已拆出 `workbench.scss`，并纳入 `audit:page-structure`
  - `trend-warning` 已拆出 `TrendSparkLine.js` 与 `trend-warning.scss`
  - `mine-entry` 已拆出 `mine-entry-view-model.js` 与 `mine-entry.scss`
  - `alert-management/notifications` 与 `alert-management/records` 已拆出各自样式模块
  - `ai-chat` 已拆出图表、导出、查询结果、会话快捷问题、文本渲染和样式模块
  - `npm run audit:page-structure` 当前跟踪 `21` 个已迁移页面，`legacyTrackedPages = 0`
  - `workbench` 部门雷达图 resize/dispose 已接入统一事件绑定与卸载释放
  - `dashboard`、`real-time`、`report-center`、`health-portrait`、`safety-command` 的页面级 resize / visibility / fullscreen 监听已统一走 `createEventBinding`
  - `npm run audit:structure` 与 `npm run build` 已通过
- 这一轮前端改动要和后端 `P1-03` 分批交付，不要混成一个超大改动。

---

## 9. Phase 5：测试与固化层

### 目标

- 把治理成果固化成可持续维护状态。

### 任务项

1. 后端补测试：
   - Redis buffer
   - AI SQL guard
   - 双库过滤器
   - 数据源 resolver
   - dashboard service
2. 前端补回归：
   - `audit:nav`
   - `audit:preflight`
   - `audit:auth`
   - `audit:api`
   - `audit:e2e`
   - `audit:pipeline`
   - `audit:pipeline-warning`
3. 补观测：
   - AI 调用
   - buffer 长度
   - flush 成功率
   - 切库行为
4. 重写架构文档与技术状态文档。

### 验收

- 治理后不是靠“记住系统行为”，而是靠测试和文档。

---

## 10. 每个 Phase 的输出模板

每完成一个 phase，输出一份简洁 handoff：

```text
Phase:
目标:
完成项:
未完成项:
改动文件范围:
验证命令:
验证结果:
遗留风险:
下一 phase 建议入口:
```

---

## 11. 提交策略

建议按 phase 或子主题提交，不要一次性堆一个超级提交。

推荐粒度：

- 一个基础设施风险项一个提交
- 一个后端结构拆分主题一个提交
- 一个前端页面家族迁移一个提交
- 测试补齐与文档补齐可单独提交

禁止行为：

- 把 P0 止血和大规模 UI 重构放一起
- 在不验证的情况下连续跨多天堆积未测改动
- 修改双库基础设施时顺手大量改页面

---

## 12. 推荐日程

当前建议：

- 新 Codex 不要再从 `P1-03 Dashboard 查询栈` 重新开始。
- `P1-01`、`P1-02`、`P1-03` 后端第一轮已完成，`Phase G1/G2` 已达目标态边界，前端 `Phase G3/G4` 当前已有 `21` 页结构门禁；`Phase G5/G6` 的完整回归、观测和文档收口已完成。
- `Phase 4` 已经有第一轮前端工作树；后续按独立主题推进，但不要和后端结构拆分混做。

Day 1：

- Phase 0
- Phase 1 前半

Day 2：

- Phase 1 后半
- Phase 2

Day 3：

- Phase 3 前半

Day 4：

- Phase 3 后半

Day 5：

- Phase 4 前半

Day 6：

- Phase 4 后半

Day 7：

- Phase 5

说明：

- 如果 P0 修复中暴露新的系统性问题，优先扩展 Phase 1，不要急着进入大拆分。
- 如果双库上下文问题没有收稳，不要进入大规模 mapper/页面重构。

---

## 13. 成功判定

本轮治理完成，至少满足：

1. Redis、AI、双库、异步这四类高风险链路有明确护栏。
2. 至少一组后端超大类和一组前端超大页完成有效拆分。
3. 自动化回归覆盖治理后的关键行为。
4. 新人可依据文档快速理解系统关键边界。


