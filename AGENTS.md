# AGENTS.md

本文件是 `health` monorepo 唯一的项目协作与运行事实源，根目录 `README.md` 仅作为项目概览和快速入口。代码、配置和实际运行结果优先于本文；发现漂移时先修正代码或配置，再更新本文。除 `README.md` 外，仓库内不再新增其他 Markdown 文档，测试产生的临时 Markdown 产物在交付前清理。

## 仓库边界与协作规则

- 根目录是唯一 Git 仓库，远端为 `https://github.com/dashabijql-web/health.git`；`HealthShow` 和 `HealthData` 是子目录，不作为独立仓库操作。
- 所有 `git status`、`git diff`、提交、回滚、tag 和推送都在根目录执行。不要恢复旧的双仓工作流或旧分支名。
- 改动前先查看本次任务范围内的现有工作树，保留用户已有改动，不做无关回滚。任务结束先检查 `git status` 和 `git diff --check`。
- GitHub HTTPS 凭证只从用户机器上的本地凭证文件按需读取，不写入代码、日志、提交信息或本文，也不在输出中显示凭证内容。未得到明确要求时不改变 `origin` 地址、不自动提交或推送。
- 修改前端页面、后端接口、双库路由、协议或测试入口时，本文是唯一应同步更新的项目文档。
- 不要把固定日期、某次 run id、一次性截图路径、旧分支状态或“当前工作树有哪些未提交文件”写成长期事实。

## 项目结构

```text
health/
├── HealthShow/                     Vue 3 前端
├── HealthData/                     Spring Boot 后端
├── firmware/esp32c3-wifi-watch/   ESP32-C3 手表原型
├── tools/                          WSL/Mac 启动、runner、抓包和探针
└── tests/                          根级 loop runner 与测试数据
```

前端技术栈是 Vue 3、Vite 5、Vuex 4、Vue Router 4、Element Plus、ECharts。后端是 Spring Boot `3.2.12`、Java 17、MyBatis-Plus `3.5.7`、Sa-Token `1.44.0`、Redis、Netty、SQL Server 和 Actuator。前端依赖安装目前可能报告 npm vulnerabilities；文档清理任务不要顺手升级依赖。

## 端口与地址

| 服务 | 默认地址 | 作用 |
| --- | --- | --- |
| 前端 | `http://localhost:9528/` | Vite 开发服务器 |
| 后端 HTTP | `http://localhost:8080/health` | Spring Boot API |
| 后端健康 | `http://localhost:8080/health/actuator/health` | 顶层 `status=UP` 才算可用 |
| 后端指标 | `http://localhost:8080/health/actuator/metrics` | Micrometer 指标 |
| 手表 TCP | `127.0.0.1:9000` | Netty 手表协议 |
| 手表 SCTP | `9001` | 默认关闭，仅 Linux 生产按需启用 |
| Redis | `127.0.0.1:6379` | 手表数据缓冲和 Sa-Token 存储 |
| SQL Server | `127.0.0.1:1433` | 双库数据库 |

前端 `/dev-api/*` 由 Vite 代理到后端 `/health/*`，前端业务代码不应绕过 `HealthShow/src/utils/request.js` 直接创建请求客户端。

## 登录与权限

- 本地默认可登录账号是 `admin / admin123`；登录页也预填该账号。实际部署应通过数据库和环境变量管理密码，不要把生产密码写进仓库。
- `POST /health/auth/login` 校验账号密码并由 Sa-Token 生成 token。前端用 Cookie `User-Token` 保存 token，后续请求从 Cookie 读取并放入 `satoken` 请求头；token 不以 localStorage 作为主存储。
- `/auth/login`、`/auth/logout` 和 `/error` 是认证白名单；其他业务请求必须通过 Sa-Token 登录校验。
- 数据源过滤器早于 MVC 认证拦截器执行。需要判断切库权限时，必须直接从当前请求的 `satoken` header 或 Sa-Token cookie 解析用户，不能只依赖当前线程的 `StpUtil.isLogin()`。
- CORS 默认允许 `http://localhost:9528` 和 `http://127.0.0.1:9528`，允许凭证和自定义 `satoken`、`X-Health-Data-Source` 请求头；生产环境应收紧来源。

## 前端路由与功能域

路由事实源是 `HealthShow/src/router/app-routes.mjs`、`health-monitor.mjs` 和 `alert-management.mjs`。页面通过 Layout 懒加载，权限码来自后端权限列表。

- 安全指挥中心：`/safety-command/index`
- 健康监测：`/health-monitor/dashboard`、`workbench`、`real-time`、`heart-rate`、`pressure`、`blood-pressure`、`blood-oxygen`、`risk-warning`、`employee-archive`、`mine-entry`、`report-center`、`trend-warning`
- 隐藏/辅助页面：`/health-monitor/sleep`、`employee-profile`、`health-portrait`、`watch-raw`、`watch-control`
- 预警管理：`/alert-management/notifications`、`sos`、`config`、`records`
- 后台管理：`/admin/device-list`、`user-list`、`role`、`department`、`job-type`
- 独立页面：`/ai-chat/index`
- 历史入口 `/command-center`、`/monitoring-center`、`/warning-center`、`/people-center`、`/report-ai` 等只做隐藏重定向，不新增第二套页面实现。

灰度开关 `VITE_SAFETY_COMMAND_V2` 和 `VITE_UNIFIED_CONTROL_V2` 默认 `true`。设为 `false` 并重新构建时，公共路径保持不变并切换到可构建的 legacy 页面。

## 后端数据流

手表数据流为：TCP 字节流 -> `WatchProtocolDecoder` -> `WatchDataHandler`/协议处理器 -> 设备与人员绑定解析 -> `DataProcessService` 及 `service/watch/*` -> Redis 缓冲 -> 批量写入 SQL Server 月分表 -> 阈值判断和预警记录。高频设备数据不得在 Netty 线程中同步执行大批量数据库写入。

- `DataProcessService` 负责外部协议入口；设备上下文、落库、健康预警和原始报文职责位于 `service/watch/*`。
- Redis 队列按数据源隔离：`health:buffer:old`、`health:buffer:new`；失败重试和 dead-letter key 也带数据源后缀。缓冲刷写默认 `@Scheduled(fixedDelay = 5000)`，队列满时使用有界 `taskExecutor` 回压，线程名前缀为 `watch-data-`。
- 健康流水表按月命名：`health_record_YYYYMM`、`warning_record_YYYYMM`；月表调度器提前建表并执行 `sp_update_monthly_views`。新增健康字段时必须同步月表、视图、存储过程、实体、Mapper INSERT、分月直查 SQL、Service 返回值和前端绑定。
- 动态表名只能来自 `TableNameUtil` 或已校验的月表白名单，禁止将用户输入直接拼接 SQL。

## 双库语义

| 逻辑源 | 数据库 | 用途 |
| --- | --- | --- |
| `old` | `health` | 模拟器、演示数据、非空数据密度和旧库回归 |
| `new` | `health_new` | 真实手表接入；业务数据稀疏或为空是允许状态 |

配置默认值位于 `HealthData/src/main/resources/application.yml`：

```text
DB_NAME_OLD=health
DB_NAME_NEW=health_new
HEALTH_DEFAULT_SOURCE=new
HEALTH_REQUEST_SOURCE=old
HEALTH_WATCH_SOURCE=new
HEALTH_SIMULATOR_SOURCE=old
HEALTH_SIMULATOR_IMEI_REGEX=^3594567800\d{5}$
HEALTH_SLOW_QUERY_THRESHOLD_MS=500
```

SQL Server 默认端口是 `1433`；配置中的开发回退密码是 `123abcd,`，应优先通过 `DB_PASSWORD`、`DB_PASSWORD_OLD`、`DB_PASSWORD_NEW` 环境变量覆盖。

切库规则：

1. HTTP 请求优先读取 `X-Health-Data-Source`，没有请求头时读取 Cookie `Health-Data-Source`，都没有时按 `HEALTH_REQUEST_SOURCE`（本地默认 `old`）。响应会回写实际生效的 `X-Health-Data-Source`。
2. 前端顶栏选择会写 Cookie 并发送请求头；下拉选择只是请求意图，最终以响应头和接口数据为准。
3. 真实手表默认写 `new`；模拟器 IMEI 命中正则时写 `old`。真实手表 IMEI 不得意外命中模拟器正则。
4. 验收新库或真实手表前执行“切新库 -> 停模拟器”；验收旧库、压测或演示数据前执行“切旧库 -> 开模拟器”。模拟器最多一个进程。
5. 新库可以只保留登录、角色权限、工种、预警阈值等基础配置；`department`、`employee`、`device`、`device_user`、流水、在线状态和 AI 报告为空都可能是预期。依赖这些表的页面应显示明确空态，不得用旧库数据冒充新库。
6. 新库初始化/迁移必须显式执行 SQL 脚本，不能在业务请求中自动建表。安全指挥中心事件表缺失应返回 `503` 并提示部署迁移；Druid SQL 防火墙会拒绝条件 DDL。

接口手工验证可使用：

```bash
curl -i http://127.0.0.1:8080/health/actuator/health
curl -i -H 'X-Health-Data-Source: old' http://127.0.0.1:8080/health/...
curl -i -H 'X-Health-Data-Source: new' http://127.0.0.1:8080/health/...
```

## 指挥中心与事件处置

- 安全指挥中心和统一管控共用 `/command-center/incidents` 事件模型，事件定位键必须是 `warningId + occurredAt`；分月预警表场景禁止只用裸 `warningId`。
- 查询、详情、确认、分派、处理、误报、时间线和外部动作都遵循双库请求头。迁移脚本必须分别作用于 `health` 和 `health_new`。
- `IncidentCommandDrawer` 是两页共享的处置入口，跨页保留事件、人员、区域和数据源上下文；成功后重新读取服务端状态，不能只修改前端数组。
- 呼叫、广播、撤离只能在具体事件详情中发起。外部系统未接入时记录 `NOT_CONFIGURED` 审计，界面不得显示“已下发”。班前复检、责任人、SLA、设备中断等后端事实未接入时显示“未接入/未分派”，不得用 `0` 或虚构人员填充。
- `GET /command-center/dashboard-summary` 是指挥摘要权威来源。`period=day|week|month` 返回对应周期的 `periodNew`；预警总数、高危待办、待办总数、未分派和超时必须以后端聚合为准，不能从前端已加载事件条数推算。
- 安全指挥中心可以展示真实设备覆盖、去重后的重点风险人员、事件范围和趋势，但不得恢复模拟体征趋势、均摊处理率、样本冒充全量、重复事件列表或无具体事件上下文的批量呼叫。未经用户确认，不做大幅信息删减或恢复。

### 风险事件体系改造计划

目标是将不同触发机制结构化区分，同时复用统一处置生命周期：体征越界属于 `HEALTH_THRESHOLD`，手表主动上报属于 `DEVICE_ALARM`，趋势预测属于 `TREND_WARNING`。`SOS` 只是 `DEVICE_ALARM` 下的一种事件代码，任何高危体征记录都不得被页面或接口冒充为 SOS。

实施状态：

- [x] 阶段一：为月度预警表和 `v_warning_record` 增加 `event_source`、`event_code`、`device_imei`、`threshold_snapshot`，由 `HealthData/src/main/resources/sql/risk_event_classification.sql` 提供同时作用于 `health` 与 `health_new` 的显式迁移脚本；历史记录通过兼容表达式分类，不在业务请求中自动执行 DDL。
- [x] 阶段二：手表体征、手表行为报警分别写入稳定事件来源和代码；预警列表 API 支持 `eventSource`、`eventCode` 筛选并在 DTO 中返回结构化分类。中文 `warning_type` 和 `indicator_name` 只用于展示，不再作为新逻辑的唯一分类依据。
- [x] 阶段三：`/alert-management/sos` 改为紧急事件页，只统计和展示设备主动报警，并提供 SOS、跌倒、房颤等事件代码筛选；SOS 统计必须由服务端全量总数得出，不能用当前页条数代替。
- [x] 阶段四：消息通知中心更名为待办事件，移除“全部已读”等错误语义；预警记录更名为处置记录并默认展示已处理事件。确认、分派、处置、误报和关闭继续复用统一事件链路，事件定位使用 `warningId + occurredAt`。
- [x] 阶段五：阈值配置升级为规则配置，明确区分体征阈值与设备报警策略；趋势预警读取 `alert_config` 的岗位默认配置，不再维护第二套硬编码阈值。
- [x] 阶段六：同时验证 old/new 数据源的迁移、分类查询、SOS 精确筛选、体征阈值生成、设备报警生成、前端构建与视觉/结构门禁。新库允许业务空态，但配置读取、接口字段和筛选语义必须通过。

验证备注：风险事件定向单测、后端全量单测、前端构建、预警生命周期测试、双库迁移和隔离端口接口验证已通过。全量 `test:fast` 当前仅剩工作树既有 `dashboard-runtime-handled` 契约失败；`audit:structure` 当前仅报告既有 `safety-command` 页面副作用和 `mine-entry` 行数超限，均不属于本次风险事件改造文件。

不可破坏的口径：

- “已读”“已确认”“已处理”“已关闭”是不同动作；没有独立通知投递模型前，页面不得提供虚假的已读状态。
- 事件严重程度与事件来源正交：高危不等于 SOS，设备报警也不一定都是高危。
- 设备报警不配置数值阈值，但可以配置启用状态、严重级别、SLA 和通知升级策略；尚未接入的策略必须显示未接入，不能假装已经执行。
- 历史无结构化分类字段的数据可以在查询层兼容推断；所有新增记录必须直接写入结构化来源和代码。

人员快速处置：

- `GET /employee/command-search` 按姓名、工号、手机号或 IMEI 搜索当前数据源的人员、绑定设备和 Netty 在线状态。
- `PersonDetailDrawer` 是指挥中心、统一管控和职工健康画像共用的人员入口。已绑定手表才启用文字消息和单人语音；未绑定设备必须禁用下发。
- SOS 只能表示手表主动上报的求救事件，管理端不得伪造“发送 SOS”。人员抽屉的应急处置只能关联该人员已有未处理预警，并以 `warningId + occurredAt` 打开事件抽屉。

## 实时监控与健康画像口径

- `/health-monitor/real-time` 在线窗口默认 15 分钟，由 `HEALTH_REALTIME_ONLINE_WINDOW_MINUTES` 配置；体征新鲜度默认 5 分钟，由 `HEALTH_REALTIME_FRESHNESS_MINUTES` 配置。
- 心率分析的部门异常图直接展示各部门偏低、偏高心率记录数，不在前端换算百分比；数值轴使用“条”，悬浮提示展示两类记录数及异常合计。
- 实时状态使用 `normal`、`warning`、`stale`、`no_data`。刷新失败保留上次数据并显示失败/缓存状态；不能用当前请求时间掩盖设备采集时间，也不能丢掉后端 `stale=true`。
- 快照按人员在窗口内为每个指标取最新非空值；筛选、总数、摘要和分页由后端完成，不能用当前已加载数组长度冒充总人数或异常数。
- 阈值统一来自 `alert_config`；后端返回 `warningReasons` 和 `indicatorStates`，前端只呈现。实时读数队列不是预警生命周期，确认、分派、处理和误报必须走预警/事件处置链路。
- `GET /realtime/health-snapshot` 的异常人数按人去重，主值写成“异常/覆盖”，并显示极值、覆盖人数、窗口、新鲜度和 `NORMAL/PARTIAL/STALE/NO_DATA`；禁止用群体均值判断个人异常或用事件列表估算实时异常人数。
- 职工健康画像展示身份、处置、当前体征及逐指标采集时间、数据新鲜度、7 日趋势、今日活动和权威预警轨迹。不得使用装饰人体热区、前端虚构医学百分比或把动画示意称为真实 ECG。
- 历史趋势走 `GET /api/health/record/history/trend`，按员工和日期范围直查相关月表；明细走 `/api/health/record/page` 服务端分页。7 日内返回原始记录点，超过 7 日按日聚合，并返回完整样本数，不能截取前 200 条或当前页自行计算。历史区提供“查询”按钮；结束日期为今天时随画像刷新同步，纯历史日期不轮询。
- 画像自动刷新默认 30 秒，手动刷新、切换员工和自动刷新都要重置倒计时，且不允许并发重复请求。画像页联系处置复用 `PersonDetailDrawer` 的 `contact` 模式，不重复铺设体征、趋势和完整画像入口。

## 本地启动

### WSL

WSL 统一入口会管理后端、前端和单实例模拟器：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh all start
bash /home/j/code/health/tools/health-wsl-stack.sh status
bash /home/j/code/health/tools/health-wsl-stack.sh all stop
```

仓库不要求路径固定为 `/home/j/code/health`；从其他 WSL 路径运行时，使用当前 checkout 下的 `tools/health-wsl-stack.sh`。该入口会探测 Java、Maven、Python、Node 和 npm，并通过 tmux 或后台进程管理 PID、日志和进程组。后端必须以 Actuator 顶层 `status=UP` 判定可用；进程存在但健康为 `DOWN` 时应完整重启。日志默认在 `runtime-logs/wsl-stack/`。

WSL 分开启动：

```bash
source tools/health-wsl-env.sh
tools/health-wsl-stack.sh backend start
tools/health-wsl-stack.sh frontend start
tools/health-wsl-stack.sh simulator start
```

### macOS

仓库提供：

```bash
tools/run-redis-mac.sh
tools/run-backend-mac.sh
tools/run-frontend-mac.sh
tools/sqlcmd-docker.sh
```

Mac 原生运行前需准备 Java 17、Maven、Node/npm、Python 和 Redis；SQL Server 可使用名为 `local-mssqlserver2022` 的 Docker 容器映射到 `1433`。后端脚本从容器读取 SQL 密码并设置双库环境变量，前端脚本默认 `VITE_TARGET=http://localhost:8080`、本地数据源 `old`。启动后逐项检查 `6379/8080/9000/9528/1433`，停止后再次检查监听端口，不能只凭 session 或 PID 文件判断已停止。

### Windows

推荐在 WSL Linux 文件系统内运行上述 WSL 入口；Windows 主机只提供 Docker SQL Server、网络和浏览器。不要重新引入已退役的旧 PowerShell 双仓启动脚本。手表原型和协议探针可在 PlatformIO/串口环境中单独运行。

## 模拟器与手表协议

- 模拟器文件是 `HealthShow/watch_tcp_simulator_1000.py`，默认连接 `127.0.0.1:9000`，默认 1000 个手表；由 WSL stack 管理时最多一个实例。
- 需要查看、压测或填充旧库时才启动模拟器；新库/真实手表验收时必须停止它。
- 手表登录后，后端下发 `BP33` 工作模式，并用 `BP86/BP87` 关闭设备内部周期，避免设备和服务端两套调度重叠。后端每 60 秒只下发一项测量，按 `BPXL -> BPXY -> BPXZ -> BPXT` 轮换，不并发启动传感器，也不把定位 `BP16` 混入健康周期；每项指标约每 4 分钟触发一次。
- `BPXL/BPXY/BPXZ/BPXT` 也可用于人工立即测量。对应的 `APXL/APXY/APXZ/APXT` 只是命令确认，实际数值仍以随后到达的 `AP49/AP50/APHT/APHP` 为准；未佩戴时数值可能为 `0`，不能当作有效健康数据。
- ESP32-C3 原型位于 `firmware/esp32c3-wifi-watch/`，通过 PlatformIO 构建。`src/main.cpp` 默认 TCP `9000`，Wi-Fi、服务器地址和 IMEI 通过编译宏配置，不能提交真实 Wi-Fi 密码。
- 原型协议包含 `IW*AP00*<IMEI>#` 登录和 `IW*APHP*...#` 健康数据；`firmware/esp32c3-wifi-watch/tools/protocol_probe.py` 可向后端发送探针。默认占位地址是 `192.168.1.100`，现场按实际局域网修改。

## 测试与验收

前端测试入口集中在 `HealthShow/scripts/health-test-runner.mjs`：

```bash
cd HealthShow
npm run test:fast
npm run test:frontend
npm run test:integration -- --source old
npm run test:integration -- --source new
npm run test:full -- --source old
npm run test:full -- --source new
npm run audit:structure
npm run audit:visual
npm run build
```

可用 profile 为 `fast`、`frontend`、`integration`、`perf`、`quality`、`full`。`old` 默认要求关键页面有非空数据，`new` 允许业务空态；登录和配置写入仍必须执行。`audit:visual` 用 Playwright 覆盖桌面与移动视口；页面布局、滚动、重叠和信息密度问题以截图和布局摘要为证据。`audit:auth`、`audit:e2e`、`audit:pipeline`、`audit:pipeline-warning` 会争用服务和测试数据，不能并行运行；需要浏览器时先完成 preflight。

根级 loop runner：

```bash
python3 tools/start-health-runner.py --data-source both
python3 tests/run-health-loop.py --profile full --data-source both
python3 tools/start-openclaw-evolution-supervisor.py
python3 tools/request-openclaw-supervisor-stop.py
```

runner 必须保持单实例保护；`--dry-run` 先返回结构化 JSON。full-stack 过程中按阶段独占 TCP 探针和模拟器，测试完成后停止服务。判断结果看最新 run 目录中的 JSON、日志、payload 和实际 HTTP 响应，不以固定历史 run id 或文档中的“已通过”代替运行证据。OpenClaw dashboard 使用 `openclaw dashboard` 生成的带 token URL。

最小运行验收：

1. `curl` 检查前端 `9528`、后端 Actuator `8080` 和响应状态。
2. 登录后确认 `satoken` 认证、Cookie 和 `X-Health-Data-Source` 响应头。
3. 按目标数据源确认页面摘要、分页 `total`、状态和空态符合 old/new 语义。
4. 需要设备链路时确认 TCP `9000`、模拟器单实例、Redis 队列和数据库月表写入。
5. 停止服务后确认监听端口和项目进程已释放。

## 观测、性能与安全

- Actuator 指标入口是 `/health/actuator/metrics`。重点指标包括请求数据源、手表路由、在线人数、预警生成/去重、Redis buffer 入队/刷写/dead-letter、AI 调用耗时/拒绝/SQL 自动修复、SQL 耗时/慢查询。
- 慢查询阈值由 `HEALTH_SLOW_QUERY_THRESHOLD_MS` 控制，默认 `500ms`。排障先看 Actuator、后端日志、Redis 队列和 SQL，再看 controller 日志。
- 前端性能问题分别判断 JavaScript/Node、浏览器渲染与 ECharts、网络/视频解码，不把“资源占用”当成单一指标。保留产品需要的视觉信息，不为降低负载擅自删业务内容。
- AI 聊天支持受控 Text2SQL 和报告能力。SQL 动态列结果必须经过白名单、参数化和结果集封装；禁止把用户输入直接拼 SQL，禁止在提示词或日志中泄露 token、密码、个人敏感健康信息。AI 规则提示不是医学诊断，AI 报告是用户主动触发的二级能力。
- 外部呼叫、广播、撤离和其他设备控制必须保留审计和明确的未配置状态，不能用前端成功提示冒充外部系统已执行。

## 变更检查清单

- 新增接口：同步 controller、service、mapper/DTO、权限、数据源路由、响应状态和前端 API；补源守护或集成测试。
- 新增健康字段：按“月表/视图/存储过程 -> entity -> mapper insert/select -> service -> API -> 前端”逐层核对。
- 修改实时页：确认在线窗口、新鲜度、逐指标最新值、服务端分页和 `normal/warning/stale/no_data`。
- 修改双库：同时验证 old 非空和 new 空/稀疏；检查请求头、Cookie、响应头、Redis key、手表/模拟器路由和权限。
- 修改指挥中心或人员处置：保留复合事件键、数据源上下文、服务端状态刷新和未接入/未配置语义。
- 修改页面布局：至少运行对应结构门禁和五档视觉审计，不以单一可见浏览器视口口头判断。
- 修改后端异步链路：确认使用有界 `taskExecutor`，不会回退到无限创建线程的 `SimpleAsyncTaskExecutor`。
- 任务结束：根目录 `git status`、`git diff --check`，只保留本次任务文件；本文之外不得留下 Markdown。
