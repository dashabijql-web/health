# CODEX.md

## 定位

这是 `D:/Health` 工作区的长期记忆，记录相对稳定的架构、数据流、约束和命名规则。它偏“长期有效的事实”，除非项目发生架构级重构，否则应长期成立。

优先执行入口：`D:/Health/HEALTH_EXECUTION_ENTRY.md`。更完整的总流程见 `D:/Health/HEALTH_ULTIMATE_FLOW.md`。

## 一、工作区整体架构

- 工作区由两个项目组成：
  - `HealthShow`：前端单页应用
  - `HealthData`：后端 HTTP + TCP 服务
- 浏览器链路：
  - Vue 页面 → Vite 代理 `/dev-api` → Spring Boot `/health` → SQL Server / Redis
- 设备链路：
  - 手表 TCP 上报 → Netty `9000` → 协议解码 → 业务处理 → Redis 写缓冲 → 分表入库

## 二、前端长期结构

### 2.1 技术栈与入口

- Vue `3.x`
- Vite `5.x`
- Vuex `4.x`
- Vue Router `4.x`，使用 Hash 路由
- Element Plus `2.x`
- ECharts `5.x`

主入口在 `HealthShow/src/main.js`，固定做四件事：

- 注册 Element Plus 与图标
- 注入 `store` 与 `router`
- 加载 `permission.js`
- 加载 `heartbeat.js`

### 2.2 认证与权限模型

- 前端认证头固定使用 `satoken`
- Token 由 `src/utils/auth.js` 管理，存 Cookie
- `src/utils/request.js` 负责：
  - 自动注入 token
  - 统一业务错误处理
  - 401/部分 5xx 自动跳登录
  - `/auth/info` 等鉴权请求静默处理
- `src/permission.js` 负责：
  - 路由前置守卫
  - 首次进入或刷新后调用 `/auth/info`
  - 拉取权限码并过滤菜单
- `src/heartbeat.js` 负责：
  - 每 30 秒调用 `/auth/info` 保活
  - 后端重启或 token 失效时静默回登录页

### 2.3 路由组织规则

- 路由全部是静态注册，再由权限码过滤显示，不靠动态注册路由。
- 权限来源是 `/auth/info` 返回的 `routes` 数组。
- 页面权限的唯一前端入口是 `meta.permCode`。
- 隐藏页用 `hidden: true`，路由存在但不出现在菜单。

### 2.4 前端模块分组

长期稳定的路由分组如下：

- 安全指挥中心：`/safety-command`
- 健康监测：`/health-monitor/*`
- 预警管理：`/alert-management/*`
- 后台管理：`/admin/*`
- AI 助手：`/ai-chat/index`

### 2.5 前端实现风格

- 页面多为“大型单文件组件 + 本页状态 + 本页轮询 + 本页 ECharts”。
- `dashboard`、`real-time`、`safety-command` 这类页面既有桌面布局，也内置了移动端样式分支。
- 请求层按业务模块拆在 `src/api/*.js`。
- 新请求应继续放到对应 API 文件，不要把 URL 散写进页面。

## 三、后端长期结构

### 3.1 运行时栈

- Spring Boot `3.2.12`
- Java `17`
- MyBatis-Plus `3.5.7`
- Sa-Token `1.44.0`
- Spring Security 仅保留密码编码，不承担鉴权
- Redis 作为实时缓冲和辅助缓存
- Netty 处理设备 TCP
- SQL Server 作为主库

### 3.2 HTTP 安全模型

- `SecurityConfig` 放行所有 HTTP 请求，避免 Spring Security 抢占登录流程。
- `SaTokenConfig` 才是真正的登录校验入口。
- 默认白名单只有登录、退出、错误页等极少数路径。
- 所有业务接口统一返回 `Result<T>`，格式固定为 `code/message/data`。

### 3.3 写路径：设备数据入库

稳定写路径如下：

1. `NettyServerConfig` 在 `9000` 端口监听设备连接
2. `WatchProtocolDecoder` 把原始协议解码为 `WatchMessage`
3. `WatchDataHandler` 按协议号分发
4. `DataProcessService` 负责业务归一化
5. 已绑定设备数据进入 `RedisHealthBufferService`
6. Redis 队列每 5 秒批量刷入数据库
7. 最终写入 `health_record_YYYYMM` 月分表

`DataProcessService` 的长期职责：

- 自动注册设备
- 维护在线状态/电量
- 未绑定设备数据写 `device_data_buffer`
- 已绑定设备数据转为 `HealthRecord`
- 根据心率推导压力指数
- 依据 `AlertConfig` 阈值生成预警
- 做预警去重

预警去重规则是稳定业务约束：

- 大多数指标：4 小时冷却
- 血氧：24 小时冷却

### 3.4 读路径：查询与看板

稳定读路径如下：

1. Controller 接收参数
2. Service 解析日期范围、路由到分表或聚合查询
3. Mapper 执行 SQL
4. 返回 `Result<T>`

当前读侧的长期设计原则：

- 高频接口优先直查分区表，不依赖全量 `UNION ALL` 视图
- Dashboard 查询缓存与聚合编排已逐步下沉到 service；Realtime 等高频读链路仍会使用内存 TTL 或分层缓存
- 轮询频繁的接口会主动压缩查询范围、限制条数或分层缓存

## 四、数据库与分表约束

### 4.1 核心表模型

- 健康数据主线：
  - `health_record_YYYYMM`
- 预警数据主线：
  - `warning_record_YYYYMM`
- 统一视图：
  - `v_health_record`
  - `v_warning_record`
- 设备绑定/缓冲：
  - `device`
  - `device_user`
  - `device_data_buffer`

### 4.2 分表维护机制

`MonthlyTableScheduler` 是长期关键组件：

- 应用启动时确保当前月和下个月分表存在
- 定时调用 `sp_create_monthly_tables`
- 定时调用 `sp_update_monthly_views`
- 每 5 分钟刷新今日 `health_daily_stats`

### 4.3 重要字段语义

- `temperature`：按整数 `x10` 存储，前端显示时再除以 10
- `sleep_minutes`：单位分钟
- `pressure`：压力指数，不是血压
- `blood_pressure_high / low`：收缩压 / 舒张压
- `calories`：独立字段，不可与 `steps` 混用

## 五、当前稳定业务能力

除了传统监测页，当前代码已稳定具备以下能力：

- 设备管理与手表消息下发
- 语音广播模板推送
- 入井准入看板
- 工作台日历
- AI 健康报告
- AI 多轮对话与 SSE 流式输出
- 趋势预警：最近 14 天均值 + 线性回归预测未来 7 天风险

## 六、命名与接口规律

### 6.1 常见接口族

- 认证：`/auth/*`
- 看板：`/dashboard/*`
- 实时监控：`/realtime/*`
- 预警：`/risk-warning/*`
- 指标分析：`/heart-rate/*`、`/pressure/*`、`/blood-pressure/*`、`/blood-oxygen/*`
- AI：`/ai/chat`、`/ai/health-report/*`、`/ai/report/*`

### 6.2 例外前缀

当前代码里有三个显著例外，属于既成事实：

- 设备管理：`/api/device/*`
- 语音广播：`/api/device/voice/*`
- 健康记录分页：`/api/health/record/page`

新增接口时不要盲目模仿旧文档，先看所属模块现有前缀。

## 七、长期修改约束

### 7.1 添加新健康字段时必须全链路同步

至少同步以下层级：

- 月分表结构
- 视图定义
- `sp_update_monthly_views`
- Java 实体
- Mapper INSERT / SELECT
- 直查分表 SQL
- Service 返回值
- 前端 API 与页面绑定
- 数据流验证：设备/模拟器 → 日志 → 数据库 → API → 页面

### 7.2 需要完整重启后端的场景

- MyBatis Mapper SQL 修改
- Mapper 新增方法
- 分表/视图/存储过程变化
- `application.yml` 变化

### 7.3 前端修改约束

- 所有 HTTP 请求统一走 `src/utils/request.js`
- 新页面权限统一挂在 `meta.permCode`
- 新菜单项必须同时检查：
  - `router`
  - 权限码
  - 对应 `src/api`
  - 对应后端 controller/service

### 7.4 文档冲突处理

- 注释、旧 md、快照、截图都可能滞后。
- 只要代码、路由、API、Controller 与文档冲突，一律以代码实现为最终事实。
