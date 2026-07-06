# HEALTH_HANDOFF

> 历史归档提示：本文件适用时间点为 2026-05-07，部分双库默认值和当前进度已被 `D:/Health/AGENTS.md` 覆盖。新任务优先读 `AGENTS.md`、`HEALTH_TEST_METHOD.md` 和对应执行计划；本文件仅作交接背景。

## 1. 这份文档解决什么问题

这是一份面向接手者的当前事实源，目标不是重复 `docs/archive/历史归档-治理清单.md` 或 `CODEX_TARGET_STATE_PLAN.md`，而是把“现在代码已经跑到哪里、改完至少跑什么、哪些操作最容易出事故、哪些债还没收完”集中到一处。

适用时间点：`2026-05-07`

---

## 2. 当前架构边界

### 2.1 仓库边界

- `D:/Health` 不是 git 仓库。
- `D:/Health/HealthShow` 和 `D:/Health/HealthData` 是两个独立 git 仓库。
- 任何 `git status`、`git diff`、提交、回滚，都必须分别在两个子项目内执行。

### 2.2 前端边界

- 项目：`D:/Health/HealthShow`
- 栈：`Vue 3 + Vite 5 + Vuex 4 + Vue Router 4 + Element Plus + ECharts`
- 开发地址：`http://localhost:9528/`
- 请求统一入口：`src/utils/request.js`
- 当前已明确拆分的大页：
  - `dashboard`：`index + runtime/chart/view-model/actions/scss`
  - `real-time`：`index + runtime/helpers/components`
  - `device-management`：`index + use-page + runtime/view-model/scss`
  - `employee-archive`：`index + use-page + runtime/view-model/scss`
  - `employee-profile`：`index + use-page + runtime/view-model/scss`
  - `health-portrait`：`index + use-page + runtime/chart/export/scss`
  - `report-center`：`index + runtime/chart/view-model/export/scss`
  - `sleep`：`index + page-state/runtime/view-model/scss`
  - `risk-warning`：`index + page-state/runtime/view-model/scss`
  - `user-list`：`index + use-page + runtime/view-model/scss`
  - `role-management`：`index + use-page + runtime/view-model/scss`

### 2.3 后端边界

- 项目：`D:/Health/HealthData`
- 栈：`Spring Boot 3.2.12 + Java 17 + MyBatis-Plus + Sa-Token + Redis + Netty + SQL Server`
- HTTP：`/health`，默认 `8080`
- TCP：`9000`
- Actuator 指标入口：`/health/actuator/metrics`
- 当前已完成第一轮核心拆分的热点域：
  - `DataProcessService` 外部协议入口保留，设备上下文 / 落库 / 预警已下沉到 `service/watch/*`
  - `Dashboard / Realtime / Statistics / Trend Warning / Health Portrait` 已开始或完成 service 边界收口
  - `HeartRate` 已收口为 `typed DTO + service cache`，controller 不再持有模块级缓存

### 2.4 双库边界

- 老库：`health`
- 新库：`health_new`
- 前端切库请求头：`X-Health-Data-Source`
- 本地开发无 Cookie 时默认请求老库；生产/预发默认新库，最终以接口响应头回写为准
- 真实手表默认写新库
- 模拟器默认写老库
- 模拟器识别正则：`^3594567800\d{5}$`
- Redis buffer 已分为：
  - `health:buffer:old`
  - `health:buffer:new`

---

## 3. 当前回归入口

### 3.1 后端

- 全量测试：
  - `mvn -q test -f D:/Health/HealthData/pom.xml`

### 3.2 前端

- 构建：
  - `npm run build`
- 最小门禁：
  - `npm run audit:nav`
  - `npm run audit:page-structure`
  - `npm run audit:structure`
  - `npm run audit:api`
  - `npm run audit:write`
  - `npm run audit:auth`
  - `npm run audit:e2e`
  - `npm run audit:pipeline`
  - `npm run audit:pipeline-warning`

### 3.3 需要知道的执行事实

- `audit:auth` 会主动重启后端，不要和 `audit:e2e` / `audit:pipeline` 并行跑。
- `audit:e2e` 会先跑 `audit:preflight`。
- `audit:pipeline` 会先跑 `audit:preflight:pipeline`，并要求本地 TCP `9000` 可连。
- `audit:pipeline` 现在会把浏览器/API 验证固定到与 `SQL_DB` 一致的数据源，避免新老库串路由。
- `audit:write` 已改为主动向 TCP `9000` 发送最小手表探针，验证 SQL/API/realtime 可见后清理探针健康记录，不再依赖后台模拟器刚好产生新数据。

### 3.4 观测入口

- `http://localhost:8080/health/actuator/metrics`
- 当前关键指标族已在后端启动时预注册，重启后无需先触发业务事件即可在 actuator 看到：
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
- 慢 SQL 门限：`HEALTH_SLOW_QUERY_THRESHOLD_MS`，默认 `500`

### 3.5 本轮最新已过验证

- `mvn -q test -f D:/Health/HealthData/pom.xml`
- `npm run build`
- `npm run audit:structure`
- `npm run audit:api`
- `npm run audit:write`
- `npm run audit:nav`
- `npm run audit:auth`
- `npm run audit:e2e`
- `npm run audit:pipeline`
- `npm run audit:pipeline-warning`

本轮产物：

- `D:/Health/HealthShow/tests/api/artifacts/2026-05-07T20-14-48-167Z/summary.md`
- `D:/Health/HealthShow/tests/api/artifacts/2026-05-07T20-15-09-061Z/summary.md`
- `D:/Health/HealthShow/tests/e2e/artifacts/2026-05-07T20-14-11-140Z/auth-summary.md`
- `D:/Health/HealthShow/tests/e2e/artifacts/2026-05-07T20-15-37-731Z/summary.md`
- `D:/Health/HealthShow/tests/pipeline/artifacts/2026-05-07T20-17-47-153Z/summary.md`
- `D:/Health/HealthShow/tests/pipeline/artifacts/2026-05-07T20-18-23-921Z/warning-summary.md`

---

## 4. 当前高风险操作清单

### 4.1 健康字段扩展

新增健康字段不能只改实体或前端页面，必须同步检查：

- 月分表
- 视图
- `sp_update_monthly_views`
- 实体
- Mapper INSERT
- 直查分表 SQL
- Service 返回值
- 前端绑定

### 4.2 MyBatis / SQL 改动

- Mapper SQL 或方法签名改动后，后端必须完整重启。
- 双库相关 SQL 改动前，先看：
  - `D:/Health/HealthData/DUAL_DB_CUTOVER.md`
  - `D:/Health/AGENTS.md`

### 4.3 双库误路由

- 不带 `X-Health-Data-Source` 的直接 HTTP 请求，后端默认走老库。
- 不要把“新库无数据”理解成“保留部门和员工，只清业务流水”，这个口径已经过时。
- `health_new` 当前 `department` 和 `employee` 也是空，需要后续重建。

### 4.4 TCP / 手表链路

- 模拟器和真实设备分流依赖 IMEI 正则，改这个值前先确认不会误伤真实设备。
- TCP 协议响应不要再直接假设 `String` 写出总是安全，当前链路已经处理过 `APHP/AP49` 这类响应出站问题。

### 4.5 前端大页改动

- `dashboard`
- `real-time`
- `employee-profile`
- `health-portrait`
- `report-center`

这些页已经开始按 runtime/chart/view-model 拆分，新增功能不要再塞回 `index.vue` 根文件。

---

## 5. 当前未解决但可接受的剩余债

### 5.1 后端

- 一批指标域 service 内部仍保留 `Map<String,Object>` 过渡层，虽然 controller 出口已明显收口，但内部类型治理还没完全结束。
- `AI / 报表 / 指标` 相关 mapper 仍有继续按主题拆分空间。
- 观测事实源已经落到 actuator metrics，但还没有做成现场 dashboard / 告警面板。

### 5.2 前端

- `role-management` 已拆到 `use-page + runtime + view-model`，但权限树 / 弹窗仍有再细分空间。
- 五类指标页虽然 timer 清理和滚动副作用已开始统一，但还没完全收成同一类 page engine。
- `sleep` 与 `risk-warning` 已拆到 `page-state/runtime/view-model/scss`，但五类指标页仍未完全收成同一类 page engine。

### 5.3 文档

- `AGENTS.md` 是最重要的当前状态文档，但仍需要随着工作树继续同步。
- `TECHNICAL_DOCS.md` 现在只适合当辅助文档，不能压过代码和 `AGENTS.md`。

---

## 6. 新开会话时建议先读什么

1. `D:/Health/AGENTS.md`
2. `D:/Health/项目重新评估结果.md`
3. `D:/Health/目标达成实施计划.md`
4. `D:/Health/CODEX_TARGET_STATE_PLAN.md`
5. `D:/Health/docs/archive/历史归档-治理清单.md`
6. `D:/Health/docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md`
7. `D:/Health/docs/archive/历史归档-HEALTH_HANDOFF.md`

按任务追加：

- 双库 / 手表入库：`D:/Health/HealthData/DUAL_DB_CUTOVER.md`
- 前端结构 / 路由 / 回归：`D:/Health/HealthShow/TECHNICAL_DOCS.md`、`D:/Health/HealthShow/tests/README.md`

说明：

- `项目重新评估结果.md` 回答当前屎山代码、健壮度、功能完善性、页面美观度真实状态。
- `目标达成实施计划.md` 是 2026-05-08 之后 goal-mode Codex 的主执行入口。
- `docs/archive/历史归档-治理清单.md` 和 `docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md` 继续保留，但不要从 P0/P1 历史任务开始机械重跑。

---

## 7. 现在可以对外宣称什么

当前可以说：

- `CODEX_TARGET_STATE_PLAN.md` 的 `G1-G6` 已按当前定义闭环。
- 后端完整测试、前端结构/API/写入/鉴权/e2e/pipeline/warning pipeline 与 build 均已通过。
- 核心热点域已经形成结构边界、回归门禁和观测入口，可以说“屎山代码和健壮度问题已基本解决到可控状态”。
- 如果目标包含功能现场验收和页面产品级美观度，则还需要继续执行 `目标达成实施计划.md`。

当前仍不能说：

- “所有历史技术债已经清零”
- “已经是教科书级架构”

仍保留的可接受债包括：

- 指标页 page engine 还能继续做第二轮抽象，但当前已有共享 runtime 和结构门禁。
- 部分 service 内部 `Map<String,Object>` 过渡层仍可继续收缩，但核心 controller 出口和高频 mapper 已有护栏。
- actuator metrics 已是事实源，但还没有现场 dashboard / 告警面板。



