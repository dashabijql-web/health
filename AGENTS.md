# AGENTS.md

## 定位

这是 `D:/Health` 工作区的项目级协作护栏与当前高优先级事实。它既记录当前状态，也约束启动、测试、双库和 runner 协作；如果代码、流程或工作树变化，应优先更新这里，而不是继续把关键信息散落到单次运行产物里。

遇到问题时，先查本地日志、报错和当前配置定位现象；同时查官方文档、GitHub issues 和既有案例，结论以本地证据为准，优先采用已验证的解决方案，不盲猜。

## 仓库边界

- 当前工作区按单仓库（monorepo）运行；根目录是唯一 git 仓库。
- `HealthShow` 和 `HealthData` 现在是 monorepo 子目录，不再作为独立 git 仓库处理。
- 所有 `git status`、`git diff`、提交、回滚、打 tag、推送，都必须在根目录执行。
- 每完成一个明确任务后，必须先在根目录核对 `git status`，只提交本次任务已确认的文件，再 `push` 到当前 monorepo 远端。
- 即使任务只改动 `HealthShow` 或只改动 `HealthData`，提交与推送也仍然在根目录完成；不要回退到旧双仓工作流。
- 旧 `health.git` / `health-backend.git` 只作为历史来源或回退参考，不再作为新开发提交入口。
- GitHub HTTPS token 固定存放在 `C:\Users\j\Desktop\githubtoken.txt`；WSL 路径为 `/mnt/c/Users/j/Desktop/githubtoken.txt`。
- 需要执行 `git push` 时，优先从上述本地文件一次性读取 token 完成认证；不要把 token 内容写入仓库文件、终端日志、提交信息、测试产物或 `AGENTS.md`。
- 如无特殊说明，不修改 `origin` 永久地址；优先使用一次性 header 或一次性凭证方式完成当前 push。
- 根级 `AGENTS.md` 属于 monorepo 工作区护栏文件，更新后应随 monorepo 一并提交和推送。

## 信息优先级

1. 当前代码与配置文件
2. 本文件
3. `D:/Health/HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md`
4. `D:/Health/HEALTH_EXECUTION_ENTRY.md`
5. `D:/Health/HEALTH_ULTIMATE_FLOW.md`
6. `D:/Health/tests/competitor-improvement-plan.md`
7. `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
8. `D:/Health/HEALTH_TEST_METHOD.md`
9. `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md`
10. `D:/Health/旧库项目重新评估结果.md`
11. `D:/Health/旧库目标达成实施计划.md`
12. `D:/Health/旧库评估过程方法与踩坑记录.md`
13. `D:/Health/项目重新评估结果.md`
14. `D:/Health/目标达成实施计划.md`
15. `D:/Health/docs/archive/历史归档-HEALTH_HANDOFF.md`
16. `D:/Health/CLAUDE.md`
17. `D:/Health/HealthShow/CLAUDE.md`
18. `D:/Health/HealthShow/TECHNICAL_DOCS.md`
19. `D:/Health/HealthShow/dashboard_snapshot.md`、`D:/Health/HealthShow/real-time-snapshot.md`、`D:/Health/HealthShow/tests/e2e/artifacts/` 下最近一轮的 `summary.md` / `auth-summary.md`

说明：

- 快照类 md 是验证产物，不是架构规范。
- 旧文档与代码冲突时，一律以代码为准。
- 涉及测试编排、Hermes/OpenClaw/Codex 分工、页面截图、数据密度、新库/旧库验收、runner 调用和测试报告时，先读 `D:/Health/HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md`。
- 涉及竞品雷达和自主优化时，再读 `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`。
- `D:/Health/HEALTH_TEST_METHOD.md` 和 `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md` 保留为细节参考，不再作为第一执行入口。
- 2026-05-08 之后，新开 goal-mode Codex 应以 `目标达成实施计划.md` 为主执行入口；`docs/archive/历史归档-治理清单.md` 和 `docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md` 是历史总表与第一轮手册，不要从头机械重跑已完成 phase。
- 如果目标是“按旧库/模拟器/演示数据解决页面空组件、性能、旧库功能闭环”，新开 goal-mode Codex 应改以 `旧库目标达成实施计划.md` 为主执行入口，并先确认 `audit:data` 为 `data_source: old`。

## 当前项目拆分

- 前端：`D:/Health/HealthShow`
  - Vue 3 + Vite 5 + Vuex 4 + Vue Router 4 + Element Plus + ECharts
  - 开发端口：`9528`
- 后端：`D:/Health/HealthData`
  - Spring Boot `3.2.12` + Java `17` + MyBatis-Plus + Sa-Token + Redis + Netty + SQL Server
  - HTTP：`/health`，默认端口 `8080`
  - TCP：`9000`
  - 2026-05-06 起支持双库路由：老库 `health` / 新库 `health_new`

## 常用启动命令

WSL 首选入口：

```bash
# 一次性拉起后端 + 前端 + 模拟器
bash /home/j/code/health/tools/health-wsl-stack.sh all start

# WSL 原生 full-stack runner
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both

# WSL detached runner
python3 /home/j/code/health/tools/start-health-runner.py --data-source both

# 根级 WSL loop runner
python3 /home/j/code/health/tests/run-health-loop.py --profile full --data-source both

# 查看状态
bash /home/j/code/health/tools/health-wsl-stack.sh status

# 停止全部
bash /home/j/code/health/tools/health-wsl-stack.sh all stop
```

说明：

- 本地开发、联调、full-stack 回归默认以 WSL 链路为准。
- 旧 Windows PowerShell 启动脚本已删除；历史文档里出现的旧脚本名只作归档参考，不再作为执行入口。
- watch capture 也优先走 WSL Python 入口：
  - `python3 /home/j/code/health/tools/watch-capture/run_three_hour_capture.py`
  - `python3 /home/j/code/health/tools/watch-capture/run_three_hour_backend_debug_capture.py`

WSL 手动分开启动：

```bash
# 加载 WSL 工具链探测
source /home/j/code/health/tools/health-wsl-env.sh

# 后端
cd /home/j/code/health/HealthData
SERVER_PORT=8080 NETTY_SERVER_PORT=9000 \
"$JAVA_HOME/bin/java" -cp "target/classes:$(cat target/runtime-classpath.txt)" \
  com.xzkj.health.HealthApplication

# 前端
cd /home/j/code/health/HealthShow
npm run dev -- --host 0.0.0.0 --port 9528

# 模拟器
cd /home/j/code/health/HealthShow
python3 watch_tcp_simulator_1000.py
```

WSL 进程核对/日志：

```bash
# 核对三件套状态
bash /home/j/code/health/tools/health-wsl-stack.sh status

# 查看后端/前端日志
tail -f /home/j/code/health/runtime-logs/wsl-stack/backend.log
tail -f /home/j/code/health/runtime-logs/wsl-stack/frontend.log
tail -f /home/j/code/health/runtime-logs/wsl-stack/simulator.log
```

历史 Windows PowerShell 启动/停进程命令已经退役，不再在本文件保留执行示例。

模拟器单实例规则：

- WSL 下同样最多只允许 `1` 个模拟器实例；优先通过 `bash /home/j/code/health/tools/health-wsl-stack.sh simulator start|stop|status` 管理。
- 旧库压测、旧库数据密度、旧库演示时最多只允许 `1` 个 `watch_tcp_simulator_1000.py` 进程。
- 不要在未检查现有进程的情况下直接重复执行 `python3 watch_tcp_simulator_1000.py`。
- 如果发现 `2` 个或更多模拟器进程，优先停止多余进程；不确定保留哪个时，直接全部停止后按单实例命令重启一个。
- 2026-05-10 曾出现过“手动后台模拟器 + 旧 runner old 阶段再次启动”的重复进程，结果是两份模拟器同时连 `127.0.0.1:9000`，会让模拟数据量和在线设备表现偏高。
- 当前 WSL runner 会在 `test-perf-old` 后、`test-full-old` 前停止模拟器，停止原因记录为 `old-full-pipeline-exclusive-tcp`；`audit:pipeline` / `audit:pipeline-warning` 需要独占 TCP 探针，不要把此时模拟器不运行误判为 runner 退化。

确保只运行一个模拟器（WSL，推荐）：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator start
bash /home/j/code/health/tools/health-wsl-stack.sh simulator status
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
```

强制重建单实例模拟器（WSL，最干净）：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
bash /home/j/code/health/tools/health-wsl-stack.sh simulator start
```

## 当前访问与登录事实

- 前端地址：`http://localhost:9528/`
- 后端地址：`http://localhost:8080/health`
- 登录账号：`admin / admin123`
- Token 实际存储在 Cookie，不是 localStorage。
- 前端开发环境通过 Vite 代理把 `/dev-api/*` 重写到 `/health/*`。
- 前端顶栏可切换“新库 / 老库”，请求头为 `X-Health-Data-Source`
- 本地开发环境 `.env.development` 默认 `VITE_DEFAULT_DATA_SOURCE=old`，用于配合模拟器检查页面数据密度；生产/预发默认 `new`。

## 双库切换流程（2026-05-07）

当前双库目标：

- 老库：`health`
- 新库：`health_new`
- 老库继续承接模拟器数据。
- 新库承接真实手表数据。

流程入口：

- 说明文档：`D:/Health/HealthData/DUAL_DB_CUTOVER.md`
- 建库脚本：`D:/Health/HealthData/src/main/resources/sql/create_health_new_seed.sql`
- 后端配置：`D:/Health/HealthData/src/main/resources/application.yml`

建新库步骤：

1. 以老库 `health` 为源，通过 `create_health_new_seed.sql` 备份并恢复出 `health_new`。
2. 在 `health_new` 中清空业务流水和待重建基础数据：
   - `department`
   - `employee`
   - `device`
   - `device_user`
   - `device_data_buffer`
   - `health_record`
   - `warning_record`
   - `health_record_20*`
   - `warning_record_20*`
   - `realtime_data`
   - `user_online_status`
   - `ai_health_report`
3. 保留系统基础配置：
   - `sys_user`
   - `sys_role` / `sys_permission` / `sys_user_role` / `sys_role_permission`
   - `job_type`
   - `alert_config`
4. 确保默认超级管理员可登录：
   - 账号：`admin`
   - 密码：`admin123`
   - 角色：`SUPER_ADMIN`

当前新库实际状态：

- `department = 0`
- `employee = 0`
- `device = 0`
- `device_user = 0`
- `realtime_data = 0`
- `user_online_status = 0`
- `job_type = 12`
- `alert_config = 20`
- `sys_user = 1`
- 当前保留登录账号：`admin / admin123`
- 2026-05-10 11:16 新库空态已由 OpenClaw 触发 run `20260510-111131-full-stack-local-new` 复核；summary 的 `dataSourceFacts.new` 显示 `department/employee/device/device_user/realtime_data/user_online_status = 0`，`simulatorDeviceRows = 0`。此前发现并清理过一条 2026-05-07 历史残留的模拟器 IMEI `359456780000001`。

切库规则：

- 前端页面会按 Cookie `Health-Data-Source` 显式请求所选库；无 Cookie 时本地开发默认老库，生产/预发默认新库。
- 2026-05-08 已修复一个本地开发易错点：如果浏览器历史遗留 `Health-Data-Source=new` 且尚无初始化标记，本地开发会自动重置为 `old`，避免“环境默认老库但页面仍走新库空数据”；之后手动从顶栏切新库仍可生效。
- 顶栏切换“数据源”时，请求头使用 `X-Health-Data-Source`。
- 顶栏下拉只表示前端选择，最终以接口响应头 `X-Health-Data-Source` 为准。
- `HealthDataSourceRequestFilter` 运行早于 Sa-Token MVC 拦截器，切库授权必须直接从当前请求的 `satoken` header/cookie 解析用户，不能只依赖 `StpUtil.isLogin()`。
- 直接 HTTP 请求如果不带请求头，后端默认走老库。
- 真实手表数据默认写新库。
- 模拟器数据默认写老库。
- 当前模拟器识别规则：IMEI 命中 `^3594567800\d{5}$`。
- 切到新库做真实手表或空库验收前，必须先停止模拟器，避免模拟器数据干扰新库判断。
- 切到旧库做模拟数据、压测或大盘填充时，再启动模拟器；模拟器只应作为老库数据来源使用。

关键环境变量：

- `DB_NAME_OLD=health`
- `DB_NAME_NEW=health_new`
- `HEALTH_DEFAULT_SOURCE=new`
- `HEALTH_REQUEST_SOURCE=old`
- `HEALTH_WATCH_SOURCE=new`
- `HEALTH_SIMULATOR_SOURCE=old`
- `HEALTH_SIMULATOR_IMEI_REGEX=^3594567800\d{5}$`

页面和接口怎么切：

- 前端右上角数据源下拉：
  - `新库` = `health_new`
  - `老库` = `health`
- 本地开发首次打开默认 `老库`，如果要验真实手表或新库空库，需要手动切到 `新库` 并停止模拟器。
- 自己调接口时手动带头：
  - `X-Health-Data-Source: new`
  - `X-Health-Data-Source: old`

上线前校验步骤：

1. 确认真实手表 IMEI 不会命中模拟器正则；若会命中，先改 `HEALTH_SIMULATOR_IMEI_REGEX`。
2. 启动后端后，确认响应头会回写 `X-Health-Data-Source`。
3. 切到新库后，确认大盘业务接口接近空数据而不是老库缓存。
4. 让一台真实手表连一次，确认它写入 `health_new.device`。
5. 再跑一次模拟器，确认它只写入老库 `health.device`。

常用核对命令：

```powershell
sqlcmd -S localhost,11433 -U sa -P [REDACTED] -d health_new -Q "SET NOCOUNT ON; SELECT 'department' AS table_name, COUNT(*) AS row_count FROM department UNION ALL SELECT 'employee', COUNT(*) FROM employee UNION ALL SELECT 'device', COUNT(*) FROM device UNION ALL SELECT 'device_user', COUNT(*) FROM device_user UNION ALL SELECT 'realtime_data', COUNT(*) FROM realtime_data UNION ALL SELECT 'user_online_status', COUNT(*) FROM user_online_status UNION ALL SELECT 'job_type', COUNT(*) FROM job_type UNION ALL SELECT 'alert_config', COUNT(*) FROM alert_config UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user;"
```

协作提醒：

- 不要再把“新库无数据”理解成“保留员工和部门，只清业务流水”；这条已经过时。
- 现在 `department` 和 `employee` 也在新库清空范围内，后续需要在 `health_new` 里重建。
- 员工档案、部门树、画像等依赖 `employee/department` 的页面，在新库下显示空列表属于预期。
- 登录页当前默认预填 `admin / admin123`，如果改掉，需要确认现场仍有可直接登录的入口。
- 涉及双库逻辑调整时，优先同步更新 `AGENTS.md` 和 `HealthData/DUAL_DB_CUTOVER.md`。
- 操作顺序固定为：`切新库 -> 停模拟器 -> 验真实手表/空库`；`切旧库 -> 开模拟器 -> 验模拟数据`。不要在新库验收时保留后台模拟器进程。

## 当前工作树状态（2026-05-08）

`HealthShow` 有未提交改动，重点集中在：

- `src/router/health-monitor.js`
- `src/router/alert-management.js`
- `src/views/health-monitor/dashboard/index.vue`
- `src/views/health-monitor/heart-rate/index.vue`
- `src/views/health-monitor/risk-warning/index.vue`
- `src/views/health-monitor/workbench/index.vue`
- `src/views/health-monitor/employee-archive/index.vue`
- `src/views/health-monitor/employee-profile/index.vue`
- `src/views/health-monitor/mine-entry/index.vue`
- `src/views/ai-chat/index.vue`
- 新增 `trend-warning`、`report-center`、`alert-management/notifications`、`alert-management/sos`

`HealthData` 有未提交改动，重点集中在：

- `src/main/java/com/xzkj/health/ai/AiChatController.java`
- `src/main/java/com/xzkj/health/ai/AiChatService.java`
- `src/main/java/com/xzkj/health/ai/SchemaProvider.java`
- 新增 `AiReportController`、`AiReportService`、`AiReportScheduler`
- 新增 `TrendWarningController`、`TrendWarningService`、`TrendWarningMapper`
- `Phase 3 / P1-02 DataProcessService` 第一轮拆分已完成：
  - `DataProcessService` 主类保留外部协议入口，约 `400` 行
  - 新增 `src/main/java/com/xzkj/health/service/watch/*`
  - 新增/调整 `DataProcessServiceTest`、`WatchDeviceContextServiceTest`、`WatchDataPersistenceServiceTest`、`WatchHealthWarningServiceTest`

规则：

- 默认把这些改动视为用户现有工作，禁止无原因回滚。
- 修改时优先兼容现有变更，不要假设工作树干净。

## 当前代码真实模块

前端当前可见模块，按路由文件和 e2e 审计结果整理：

- 安全指挥中心：`/safety-command/index`
- 健康监测：`workbench`、`dashboard`、`real-time`、`heart-rate`、`pressure`、`blood-pressure`、`blood-oxygen`、`risk-warning`、`employee-archive`、`mine-entry`、`report-center`、`trend-warning`
- 健康监测隐藏页：`sleep`、`employee-profile`、`health-portrait`
  - `sleep` 当前已拆成 `index + page-state/runtime/view-model/scss`
- `risk-warning` 当前已拆成 `index + page-state/runtime/view-model/scss`
- 预警管理：`notifications`、`sos`、`config`、`records`
- 后台管理：`device-list`、`user-list`、`role`、`department`、`job-type`
- 独立页：`/ai-chat/index`

## 2026-07-13 指挥中心与统一管控护栏

- 两页共用 `/command-center/incidents` 事件模型和 `warningId + occurredAt` 定位键。分月预警表下禁止仅使用裸 `warningId` 查询、处理或审计。
- 统一事件详情、确认、分派、处理、误报、外部动作和时间线都按 `X-Health-Data-Source` 双库路由；迁移脚本为 `HealthData/src/main/resources/sql/command_center_incident.sql`，必须对 `health` 与 `health_new` 分别执行。
- 不在应用请求内自动建事件表。Druid SQL 防火墙会拒绝条件 DDL；缺表应明确返回 `503`，由部署迁移解决。
- 呼叫、广播和撤离只能在具体事件详情中发起。未接入外部系统时，必须记录 `NOT_CONFIGURED` 审计且界面不得显示已下发。
- `IncidentCommandDrawer` 是安全指挥中心和统一管控的共享处置入口。跨页必须保留 `warningId`、`occurredAt`、`incidentId`、人员和区域上下文；处理成功后刷新服务端事件状态，不得改本地数组伪造闭环。
- 班前复检超时、设备数据中断目前没有后端事实数据。页面只能明确标记“未接入”，禁止虚构人数、人员列表、责任人或 SLA。
- 灰度与回滚开关：`VITE_SAFETY_COMMAND_V2`、`VITE_UNIFIED_CONTROL_V2`，默认 `true`。配置为 `false` 后重新构建，路径保持不变并切回可构建的 `legacy-20260601` 页面。
- 2026-07-13 本轮已验证：`audit:api` 53/53、`audit:auth` 7/7、`audit:e2e`、`audit:structure`、`safety-command,dashboard` 五档视觉审计和 `npm run build` 均通过。认证拦截必须从 `SaHolder.getRequest().getSource()` 获取 Servlet request；不可再把 `SaInterceptor` 的 handler 参数误判为 request，否则会放行所有业务接口。
- 2026-07-14 统一管控指挥摘要改走 `GET /command-center/dashboard-summary`：今日新增、高危待办、待办总数不得再由前端前 `200` 条事件推算；未分派和已超时来自事件状态表聚合。复检状态/时限、低电、数据中断、设备故障未建模时必须返回 `status=UNAVAILABLE, value=null`，禁止用 `0` 伪装已接入。
- 2026-07-15 安全指挥中心完成信息治理：头部统计只使用 `dashboard-summary` 权威口径，开放事件统一进入一个可筛选处置队列；页面只保留一个优先事件、一个部门预警矩阵、一个今日闭环和一个真实 7 日趋势。禁止重新加入模拟体征趋势、均摊处理率、样本与全量混算图表、重复部门榜单、重复事件列表或无具体事件上下文的批量呼叫。
- 安全指挥中心所有数量必须明确对象和范围：部门矩阵展示“预警条数”，队列“已加载条数”不得冒充权威待办总数；未接入责任人、SLA 或外部动作必须显示“未分派 / 未配置 / 未接入”。本轮信息恢复后的 `safety-command` 五档滚动视觉审计通过，结果位于 `HealthShow/tests/visual/artifacts/2026-07-15T03-36-35-040Z/layout-summary.md`。
- 2026-07-15 信息治理后的恢复原则：安全指挥中心应保留权威设备覆盖、开放事件去重后的重点风险人员、以及明确标注“已加载事件范围”的类型与处置信号；这些属于处置所需信息，不应作为冗余删除。仍禁止恢复模拟体征、均摊处理率、第二套事件明细或样本冒充全量的统计。
- 2026-07-15 用户明确要求安全指挥中心恢复到大幅信息清理前的完整版本；当前展示层以提交 `72901c3` 的页面为恢复基线，保留完整态势雷达、遥测、部门榜、闭环、体征趋势、区域/类型分布、事件流和高危人员。统一事件 API、共享人员抽屉和可构建的 legacy 回退页不随展示恢复而倒退。恢复后的五档滚动视觉审计通过，结果位于 `HealthShow/tests/visual/artifacts/2026-07-15T07-36-51-730Z/layout-summary.md`。后续不得未经用户确认再次做大幅信息删减。

## 2026-07-15 人员快速处置与健康画像护栏

- 统一管控通过 `GET /employee/command-search` 按姓名、工号、手机号或 IMEI 快速找人；结果必须包含当前库的人员身份、绑定设备和 Netty 实时在线状态，并继续遵循 `X-Health-Data-Source` 双库路由。
- `PersonDetailDrawer` 是统一管控、安全指挥中心和职工健康画像共用的人员综合管控入口。文字消息和单人语音可直接向已绑定手表下发；未绑定设备时必须禁用下发入口。
- SOS 是手表端主动上报的求救事件，管理端不得伪造“发送 SOS”。人员抽屉的“应急处置”只能关联该人员已有的未处理预警，并以 `warningId + occurredAt` 打开 `IncidentCommandDrawer`。
- 职工健康画像负责完整实时体征、趋势和预警分析；统一管控负责快速检索和快捷处置。两页复用同一人员抽屉，不复制通信实现。

## 2026-07-15 实时监控口径与页面护栏

- `/health-monitor/real-time` 的“在线人员”不再使用近 7 天活跃口径。后端 `/realtime/online-users` 默认统计最近 `15` 分钟有上报的人员；环境变量为 `HEALTH_REALTIME_ONLINE_WINDOW_MINUTES`。
- 数据新鲜度默认 `5` 分钟，环境变量为 `HEALTH_REALTIME_FRESHNESS_MINUTES`。接口状态统一为 `normal / warning / stale / no_data`，页面必须分别展示，禁止用当前系统时钟掩盖陈旧或失败数据。
- 实时快照会在在线窗口内为每个指标取最新非空值，不能回退到“只取最新一条数据包”，否则心跳包或单指标包会把同一人员其他体征显示成 `--`。
- 姓名/工号、部门和状态筛选在后端完整快照上执行；桌面和移动端都使用接口 `total`、`summary` 和服务端分页。禁止再次用当前已加载数组长度冒充在线总数或异常总数。
- 阈值优先复用 `alert_config`，接口返回 `warningReasons` 和 `indicatorStates`，前端只负责呈现。禁止在表格、移动卡片、跑马灯、侧栏中各自维护不同阈值。
- 页面已移除重复预警跑马灯；移动端不再重复显示异常侧栏。右侧“当前异常体征”只是实时读数队列，不等同于预警事件生命周期；真正的确认、分派、处理和误报仍进入预警中心/统一事件处置链路。
- 刷新失败必须保留上次数据并显示失败或缓存状态。后端 `stale=true` 不得在前端归一化时丢弃。

## 2026-07-15 职工健康画像信息治理护栏

- `/health-monitor/employee-profile` 只保留人员身份与处置、当前体征及逐指标采集时间、数据新鲜度、7日趋势、今日活动和权威预警轨迹。禁止恢复装饰性人体热区、同一体征多处重复展示或前端自行生成的医学风险百分比。
- 页面不得把接口请求成功时间当作设备采集时间，也不得把“任意上报在线”和“体征数据新鲜”混成一个口径。画像接口按 `HEALTH_REALTIME_ONLINE_WINDOW_MINUTES` 判断在线，按 `HEALTH_REALTIME_FRESHNESS_MINUTES` 判断体征新鲜度，并返回每个体征的最近采集时间。
- 页面不得把分页列表长度冒充近30日、近7日或待处理预警总数；三个数量必须使用 `/risk-warning/list` 对应过滤条件返回的 `total`。最近列表只表示已加载记录。
- `HeartRateWave` 是根据心率生成的动画示意，不是真实 ECG 数据；职工健康画像禁止将其标为“实时心电图”。只有后端接入真实 ECG 波形及采集时间后才允许恢复心电模块。
- 规则模板必须明确标为“规则提示”，不得冒充 AI 结论；AI 诊断报告继续作为用户主动触发的二级能力。
- 信息治理回归入口为 `npm run test:employee-profile-governance`。页面布局改动继续运行 `node scripts/with-env.mjs VISUAL_ROUTES=employee-profile -- npm run audit:visual` 并检查五档截图。2026-07-15 本轮五档结果位于 `HealthShow/tests/visual/artifacts/2026-07-15T07-40-13-080Z/layout-summary.md`。
- 画像页历史趋势的唯一入口是“历史健康数据”，默认近7日，并支持今日、近30日和最长365天自定义范围；不要再恢复另一套固定7日曲线。7天内由后端按小时聚合，超过7天按日聚合，完整样本数必须随接口返回。
- 历史曲线走 `GET /api/health/record/history/trend`，按当前员工和日期范围直查涉及的月分表；原始明细继续走 `/api/health/record/page` 服务端分页。禁止前端截取前200条或当前页数据自行计算时间段曲线。
- 历史区必须始终显示员工姓名、工号、起止日期、聚合粒度、完整样本数和明细总数；曲线与明细都继续遵循 `X-Health-Data-Source` 双库路由。
- 本轮历史区五档滚动视觉审计通过，结果位于 `HealthShow/tests/visual/artifacts/2026-07-15T08-21-43-765Z/layout-summary.md`；真实交互守护为 `node tests/e2e/employee-profile-history-check.mjs`。

## 当前验证基线

2026-05-08 目标态收口后验证：

- `mvn -q test -f D:\Health\HealthData\pom.xml` 通过。
- `npm run audit:api` 通过；查看结果时优先读 `D:/Health/HealthShow/tests/api/artifacts/` 下最新一轮 `summary.md`。
- `npm run audit:data` 是数据密度门禁，默认以老库执行关键页面组件非空检查；用于防止“页面能打开但核心组件空”的回归。查看结果时优先读 `D:/Health/HealthShow/tests/api/artifacts/` 下最新一轮 `data-density.md`。
- `npm run audit:write` 老库严格通过；新库空业务数据时，绑定设备、未处理 warning、AI 报告员工候选这类业务写探针允许记为 `skipped`，但登录和配置写入仍必须执行，旧库不允许同类跳过。
- `npm run audit:nav` 通过，结果 `6 navGroups / 23 visibleNavLeaves / 5 mobileNavItems`。
- `npm run audit:page-structure` 通过，已把 `21` 个已迁移页面纳入结构门禁，`legacyTrackedPages = 0`。
- `npm run audit:structure` 通过，当前会连续执行 `audit:nav` 与 `audit:page-structure`。
- `npm run audit:visual` 现已把 `safety-command`、`dashboard`、`workbench`、`real-time`、`risk-warning`、`employee-profile`、`mine-entry`、`trend-warning`、`alert-management/notifications`、`alert-management/records`、`report-center`、`ai-chat` 纳入默认路由清单；它会用 Playwright 无头 Chromium 自动登录并按 `desktop-1440`、`desktop-1707`、`desktop-1920`、`mobile-390`、`mobile-414` 五档视口出图，产出 `D:/Health/HealthShow/tests/visual/artifacts/` 下最新一轮 `layout-summary.md` 和对应 PNG。`employee-profile` 会先经 `employee-archive` 实际点入画像页，再截图取证。前端美化、布局、响应式、信息密度任务优先使用这条门禁，不要只凭肉眼、单一视口或口头描述判断。
- `npm run audit:auth` 通过。
- `npm run test:fast` 现在包含 `tests/auth-backend-start-hidden.mjs`、`tests/health-write-source-semantics.mjs` 和 `tests/openclaw-supervisor-contract.mjs` 等源守护，用于防止 `audit:auth` 后端重启、`run-full-stack-local.py` / `run-health-loop.py` 回退到旧 Windows 流程，防止新库空态 `audit:write` 语义回退成失败，并防止 OpenClaw supervisor 丢失“本轮后停止”、`ROUND_RESULT_JSON`/`process_retrospective`、final `stop_reason` 等流程门禁。
- `npm run audit:e2e` 通过；查看结果时优先读 `D:/Health/HealthShow/tests/e2e/artifacts/` 下最新一轮 `summary.md` 或 `auth-summary.md`。
- `npm run audit:pipeline` 通过；查看结果时优先读 `D:/Health/HealthShow/tests/pipeline/artifacts/` 下最新一轮 `summary.md`。
- `npm run audit:pipeline-warning` 通过；查看结果时优先读 `D:/Health/HealthShow/tests/pipeline/artifacts/` 下最新一轮 `warning-summary.md`。
- `npm run build` 通过。
- `http://localhost:8080/health` 与 `http://localhost:9528/` 均返回 `200`。
- OpenClaw 双库 runner 入口固定为 `python3 /home/j/code/health/tools/start-health-runner.py --data-source both`；如果 `health_new` 保持空业务库，则 `test-full-new` 允许以 `allowed skipped steps` 口径结束，不要误判成失败。
- runner 必须保持单实例保护；真实启动时若已有 `run-full-stack-local.py` 在跑，应返回 `RUN_ALREADY_ACTIVE`。`--dry-run` 必须先于单实例拦截返回结构化结果，避免测试脚本把提示文本当成 JSON。
- OpenClaw / gateway 配置校验以最新 `openclaw.json`、`models.json`、gateway probe 和实际 agent 响应为准，不要以某次历史 run id 为准；旧 Windows 同步脚本已退役。
- OpenClaw dashboard 应通过 `openclaw dashboard` 生成的带 token URL 打开，不要直接裸开 `http://127.0.0.1:18789/`。
- 夜间 supervisor 入口固定为 `python3 /home/j/code/health/tools/start-openclaw-evolution-supervisor.py`；要求“本轮后停”时，用 `python3 /home/j/code/health/tools/request-openclaw-supervisor-stop.py` 写入 `D:/Health/tests/runs/openclaw-night-supervisor.stop`，不要手工杀 runner。
- 判断最近一轮双库 runner 是否健康时，以 `D:/Health/HealthShow/tests/runs/` 下最新 run 目录的 `summary` / `hermes-archive` 为准，不要把某个固定 run id 写死到规则里。
- 2026-05-11 03:20 Round20 做了预警生命周期文案收口：`HealthShow/src/views/alert-management/common/warning-lifecycle.js` 新增 `warningHandledStatusLabel`，通知/记录页处理状态统一为 `待处理/已处理`，`tests/warning-lifecycle.mjs` 新增页面硬编码 `未处理` 回归保护。Codex 复核后补掉通知筛选项漏改并已跑过 `npm run test:warning-lifecycle`、`npm run audit:structure`。
- 2026-05-11 04:10 Round23 做了报表中心导出状态优化：`report-center-view-model.js` 新增导出可用性/禁用原因，`report-center/index.vue` 的 Excel/PDF 导出在加载、导出中、无可导出数据时禁用并显示 tooltip。Codex 复核后补上 Excel/PDF 各自导出中的原因文案，并已跑过 `node --test tests/report-center-export-state.mjs`、`npm run audit:structure`、`npm run build`。
- `http://localhost:8080/health/actuator/metrics` 已暴露业务指标；关键 metric families 已在后端启动时预注册，重启后无需先触发业务事件即可看到：
  - `health.ai.call.duration`
  - `health.ai.reject.total`
  - `health.ai.sql.auto_repair.total`
  - `health.buffer.queue.size` / `health.buffer.push.total` / `health.buffer.flush.total` / `health.buffer.dead_letter.total`
  - `health.datasource.request.total` / `health.datasource.watch.route.total`
  - `health.watch.online.count`
  - `health.warning.generated.total` / `health.warning.dedup.total`
  - `health.sql.statement.duration` / `health.sql.slow.total`

历史 Playwright 路由审计基线显示：

- 桌面 `1440 / 1707 / 1920` 和移动 `390 / 414` 已由 `audit:visual` 无头截图覆盖；该脚本不会弹出可见浏览器窗口，排查时应直接查看 `layout-summary.md` 和对应 PNG，而不是误以为“没打开浏览器就没截图”。
- 当前已知轻量警告主要有两个：
  - `blood-oxygen` 页 ECharts 初始化时偶发容器宽高为 `0` 的 warning。
  - `alert-notifications` 页 `el-pagination` 仍在使用即将废弃的 `small` 属性。

历史基线产物统一保留在以下目录；排查时只看各目录最新一轮，不要把时间戳文件写死进规则：

- `D:/Health/HealthShow/tests/api/artifacts/`
- `D:/Health/HealthShow/tests/visual/artifacts/`
- `D:/Health/HealthShow/tests/e2e/artifacts/`
- `D:/Health/HealthShow/tests/pipeline/artifacts/`

如果改动以下页面，至少回看桌面和移动端布局：

- `safety-command`
- `dashboard`
- `workbench`
- `real-time`

优先操作：

- 先跑 `node scripts/with-env.mjs VISUAL_ROUTES=<route-slug> -- npm run audit:visual`，再看 `D:/Health/HealthShow/tests/visual/artifacts/` 下最新一轮 `layout-summary.md` 与对应 PNG。
- 不要只开可见浏览器手工扫一遍就下结论；前端美观、密度、滚动、重叠、底栏遮挡这类问题，优先以无头自动截图证据为准。

## 当前易错点

- 添加新的健康字段时，不能只改实体或页面。
  - 必须同步检查：月分表、视图、`sp_update_monthly_views`、实体、Mapper INSERT、直查分表 SQL、Service 返回值、前端绑定。
  - MyBatis Mapper SQL 或方法签名改动后，后端需要完整重启。
- 当前接口前缀并不统一：
  - 常规业务多为 `/dashboard`、`/realtime`、`/risk-warning`、`/heart-rate` 等
  - 设备相关走 `/api/device`、`/api/device/voice`
  - 健康记录列表走 `/api/health/record/page`
- 双库模式下不要再假设只有一个 `health:buffer`
  - 当前 Redis 已拆为 `health:buffer:old` / `health:buffer:new`
- 现场排障不要只盯 controller 日志
  - 先看 `http://localhost:8080/health/actuator/metrics`
  - 慢 SQL 阈值当前由 `HEALTH_SLOW_QUERY_THRESHOLD_MS` 控制，默认 `500ms`
- 双库模式下不要把“新库无数据”理解成“完全空库”
  - `health_new` 当前只保留系统登录、角色权限、工种、预警阈值等基础配置
  - `department` 和 `employee` 已清空，后续需要在新库里重建
  - 已清空的是设备、缓冲、健康记录、预警记录、AI 报告等业务流水
- 手表数据默认写新库，模拟器默认写老库
  - 当前模拟器识别规则：IMEI 命中 `^3594567800\d{5}$`
  - 如果真实手表 IMEI 也可能命中这个规则，先改 `HEALTH_SIMULATOR_IMEI_REGEX`
  - 切到新库时先停模拟器；切到旧库时再开模拟器
- `TECHNICAL_DOCS.md` 已补入路由事实源、大页目录模板、`audit:structure` / `audit:page-structure`，并覆盖 AI 聊天、趋势预警、入井准入、预警通知/记录的最新拆分事实。
- 部分注释或旧文档仍可能漂移：
  - 根 `CLAUDE.md` 当前已写明 token 存 Cookie；如后续再出现冲突，以 `AGENTS.md` 和当前代码为准
  - `HealthApplication.java` 中 Druid 登录说明已不可靠，实际以 `application.yml` 和环境变量为准

## 当前协作建议

- 先看 `router`、`src/api`、对应 `controller/service`，再决定改动点。
- 涉及前端页面美化、布局、滚动、卡片密度、表格裁切、图表容器、底栏遮挡、响应式适配时，必须优先跑 `npm run audit:visual` 或 `node scripts/with-env.mjs VISUAL_ROUTES=<route-slug> -- npm run audit:visual`；默认先看自动截图产物，再决定是否需要人工打开可见浏览器复核。
- 前端所有请求都应继续走 `src/utils/request.js`。
- 后端高频接口优先复用现有“内存 TTL 缓存 + 直查分表”的模式，不要回退到全视图扫描。
- 后端注解 SQL 返回 typed Row DTO 时，数值字段如果继续使用 `Number`，必须保留 `com.xzkj.health.config.mybatis.NumberTypeHandler` 和 `mybatis-plus.type-handlers-package` 注册；SQL 列别名优先使用 `snake_case` 配合 `map-underscore-to-camel-case`，否则容易出现接口 `200` 但页面组件全 0/空。
- `heart-rate`、`pressure`、`blood-oxygen` 域当前已改成 `typed DTO + service 内 TTL cache`，其中 `PressureMapper` 已切到 `Pressure*Row`，`BloodOxygenMapper` 已切到 `BloodOxygen*Row`；后续处理其他指标页时沿用同样模式，不要把 cache 放回 controller。
- `P1-03 Dashboard 查询栈` 后端第一轮已完成；不要重复做 `DashboardController` 去 `DashboardMapper`、controller 缓存下沉、DashboardService 公开 `Map` 方法内收。
- Dashboard 当前后端状态：
  - `DashboardController` 约 `213` 行，只保留 HTTP 编排
  - `DashboardServiceImpl` 约 `458` 行，已拆出 `service/dashboard/*` 承接 calendar、entry、department/person/comparison 三组查询主题
  - 新增 `DashboardOverviewMapper` 承接首页总览、设备活跃、预警事件、预警小时分布和日汇总查询
  - 历史 `DashboardMapper` 已退出；Dashboard 查询已拆出 `DashboardCalendarMapper`、`DashboardEntryMapper`、`DashboardOverviewMapper`、`DashboardDepartmentMapper`
  - `DashboardCalendarQueryService` / `DashboardEntryQueryService` 已切到 typed row，calendar、entry、day-rank 子域不再消费 mapper `Map` 行结果
- 后端 `Phase G2` 目标态边界已基本满足；后续只处理明确回归、性能第二轮或新增接口契约治理。不要重复做 `DashboardDepartmentMapper`、`DashboardOverviewMapper`、`BloodOxygenMapper` 或旧 `AiReportService` typed row 收口，也不要把设备上下文、落库路由、预警判断重新塞回 `DataProcessService`。
- `Phase G2` 第一批已完成：
  - `TrendWarningMapper` 的趋势日均值结果改为 `TrendWarningDailyAverageRow`，不再向 service 暴露 `List<Map<String,Object>>`
  - `TrendWarningService` 已收为查询、缓存、表源编排，预测算法下沉到 `service/trend/TrendWarningPredictionCalculator`
  - `RealtimeController` 不再本地 catch 并 `Result.error(ex.getMessage())`，实时列表无缓存失败改抛 `BusinessException(503, ...)` 走全局异常处理
  - 新增 `CoreControllerBoundaryTest`，保护 `Dashboard/Realtime/HealthPortrait/TrendWarning/Statistics` controller 不回退到 mapper 直连、裸 `Map` 出口或本地异常拼接
  - `HealthPortraitMapper` / `StatisticsMapper` 已改为 typed row，`HealthPortraitService` / `StatisticsService` 不再通过 `Map<String,Object>` 消化 mapper 行结果；`AiHealthReportService` 已同步适配画像员工 row
  - `AiHealthReportMapper` 统计查询已改为 `AiHealthStatsRow` / `AiWarningStatsRow`，AI 健康报告 prompt 构造不再通过 `Map` 字段名取值
  - `PressureMapper` 已改为 `Pressure*Row`，`PressureServiceImpl` 不再通过 mapper `Map` 行结果做字段转换
  - `BloodOxygenMapper` 已改为 `BloodOxygen*Row`，`BloodOxygenServiceImpl` 不再通过 mapper `Map` 行结果做字段转换
  - 旧 `AiReportService` / `AiReportScheduler` 已改走参数化 `AiReportMapper` + typed row；`SqlExecutorMapper` 当前只保留给 `AiChatService` 的 Text2SQL 动态列场景
  - `DashboardServiceImpl` 已拆出 `DashboardCalendarQueryService`、`DashboardEntryQueryService`、`DashboardDepartmentQueryService`、`DashboardOverviewMapper`，并新增对应 service 单测；`DashboardCalendarQueryService` / `DashboardEntryQueryService` 已完成 typed row 收口
  - 历史 `DashboardMapper` 已退出，部门/人员统计主题已迁到 `DashboardDepartmentMapper`
  - `AiChatService` 的 Text2SQL 动态列结果已封装为 `AiSqlResultSet(columns, rows, rowCount)`，前端 AI 聊天 `[DATA]` 事件兼容新旧数据形态
- 后端下一入口：只处理明确回归、性能第二轮或新增接口契约治理；`pressure`、`blood-oxygen`、旧 `AiReportService`、`AiHealthReportService` 统计链路和 Dashboard department/overview/calendar/entry typed row 不要重复做。
- 前端 `Phase G1` 已收口：`src/router/app-routes.mjs` 是路由事实源，`src/layout/menu/navigation.mjs` 是菜单/移动底栏派生入口，已拆大页目录由 `audit:page-structure` 保护。
- 前端 `Phase G4` 已完成新一轮门禁扩面：`audit:page-structure` 当前覆盖 `21` 个已迁移页面，包括 `trend-warning`、`mine-entry`、`alert-management/notifications`、`alert-management/records`、`ai-chat`。
- 前端 `Phase G4` 指标页共享 runtime 第一刀已落地：`src/views/health-monitor/metric-page/metric-scroll.js` 统一了 `heart-rate`、`blood-oxygen`、`pressure`、`blood-pressure` 的 Top5 自动滚动生命周期；`metric-export.js` 统一四个指标页 Excel 导出外壳；`metric-data-loader.js` 已被四个指标页实际使用。已通过 `npm run audit:structure` 和 `npm run build`。
- `workbench` 已拆出 `workbench-view-model.js`、`workbench-chart.js`、`workbench.scss` 并纳入 `audit:page-structure`，部门雷达图 resize/dispose 已接入统一事件绑定。
- `ai-chat` 已拆出 `ai-chat-chart.js`、`ai-chat-export.js`、`ai-chat-query-result.js`、`ai-chat-session.js`、`ai-chat-text.js`、`ai-chat.scss`；`trend-warning` 已拆出 `TrendSparkLine.js` 和 `trend-warning.scss`；`mine-entry` 已拆出 `mine-entry-view-model.js` 和 `mine-entry.scss`。
- `alert-management/notifications` 当前已把 SLA 状态、剩余/超时时长和到期时间直接展示在通知列表；SLA 派生口径集中在 `alert-management/common/warning-lifecycle.js`，由 `tests/warning-lifecycle.mjs` 守护。
- 已迁移大页的页面级 DOM 事件监听已进一步收口到 `createEventBinding`：`dashboard`、`real-time`、`report-center`、`health-portrait`、`safety-command` 不应再回退到手写 add/removeEventListener。
- 前端下一入口：只处理新增大页、明确回归或指标页 page engine 第二轮；不要重复迁移已纳入 21 页门禁的页面。完整回归与文档事实源已按 `G5/G6` 收口。
- 若要理解页面结构，优先看真实路由和 `tests/e2e/artifacts`，不要只看 `TECHNICAL_DOCS.md`。
- 涉及 SQL Server 切库或手表入库时，先看 `HealthData/DUAL_DB_CUTOVER.md`

## 2026-05-10 补充护栏

- 根目录仍然不是 git 仓库；`HealthShow` 和 `HealthData` 的 git 状态、提交、回滚必须分开处理。
- 当前工作树存在大量用户未提交/未跟踪改动，任何改动都只能触碰本次任务文件。
- 2026-07-13 统一管控 dashboard 的“监测覆盖与数据质量”和“健康异常快照”面板已完成；覆盖率支持实时总人数回退，异常人数来自统一事件流并按体征去重。相关契约、结构、构建和 dashboard 五档视觉审计均通过。
- 2026-07-14 统一管控已按完整长图修复纵向空白：顶部“覆盖/健康”和“准入/闭环”两列等高，设备概览/例外独立成带，值班决策全宽并按三行内容组织。dashboard 布局修改除五档 `audit:visual` 外必须运行 `npm run audit:visual:dashboard-full`；完整长图覆盖桌面 `1440 / 1707 / 1920`，顶部双列高度差不得超过 `24px`，页面总高不得异常超过 `5000px`。禁止用 `vh`、`flex: 1`、`height: 100%` 形成循环拉伸，也不得隐藏图表占位或单纯拉高卡片填空。
- 前端旧库演示/压测最多一个模拟器进程；新库验收前必须停掉模拟器。
- 后端/前端/模拟器长驻进程应优先用现有脚本静默启动，避免弹出可见控制台污染长期测试。
- 写入健康数据、手表数据、员工数据、AI 报告和日志前，默认按隐私数据处理。
- md文件要用中文名
- 第一性原理
  - 当前 `统一管控（dashboard）` 的第一性是 `美观`，优先级高于“尽量少改旧结构”“尽量保留历史堆叠方式”“单纯把数据全塞进首屏”。
  - 对统一管控页做判断时，先看视觉秩序，再看数据密度：
    - 首屏阅读路径是否清晰
    - 左 / 中 / 右是否形成稳定指挥带，而不是高度失衡的三列堆叠
    - 图表和列表是否被容器策略异常拉高
    - 滚动是否自然，不能靠嵌套滚动或错误落点掩盖布局问题
  - 如果“能跑”和“好看”冲突，在统一管控页当前阶段先保证好看，再回头为实现补结构。
