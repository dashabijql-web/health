# Health 新库/旧库统一测试方法

> 入口更新：后续 Health 测试闭环、自主测试、Hermes/OpenClaw/Codex 分工和竞品雷达，以 `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md` 为第一执行入口。本文件保留为新库/旧库测试口径细节参考。

整理日期：2026-05-08

依据文档：

- `D:/Health/项目重新评估结果.md`
- `D:/Health/目标达成实施计划.md`
- `D:/Health/旧库项目重新评估结果.md`
- `D:/Health/旧库目标达成实施计划.md`

本文目的：

- 把新库 `health_new` 和旧库 `health` 的测试口径拆开。
- 把“能启动、能打开页面”升级为“数据源正确、组件非空或空态合理、功能闭环、性能可接受、页面可验收”。
- 给 Hermes 编排测试、Codex 修复问题、人工现场验收提供同一套执行方法。

工具链详细用法：

- `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md`

说明：本文定义测试标准和验收口径；Hermes、OpenClaw、Codex、skills、MCP、脚本怎么用，详见工具链文档。

---

## 1. 总原则

### 1.1 先分清新库和旧库

不能再用一套结论同时判断两个库。

| 场景 | 数据源 | 主要用途 | 测试结论口径 |
| --- | --- | --- | --- |
| 新库 | `health_new` / `new` | 真实手表、空库启用、上线切换 | 允许业务数据为空，但必须验证空态、初始化流程、真实手表写入 |
| 旧库 | `health` / `old` | 模拟器、演示、高数据量历史库、页面非空 | 核心组件不应为空，空组件默认视为问题，除非白名单说明 |

### 1.2 所有测试先确认数据源

每轮测试开头必须记录：

- 前端顶栏当前显示：`新库` 或 `老库`
- 请求头：`X-Health-Data-Source`
- 响应头：`X-Health-Data-Source`
- `audit:data` 报告中的 `data_source`
- 后端实际 SQL 库：`health` 或 `health_new`

旧库测试时，报告必须满足：

```text
data_source: old
expect_non_empty: true
failed: 0
```

新库测试时，报告必须满足：

```text
data_source: new
expect_non_empty: false
```

### 1.3 测试分层

测试分成 7 层，不能只跑其中一层就说项目达标。

1. 环境预检：前端、后端、Redis、SQL Server、TCP、模拟器/真实手表。
2. 静态和结构门禁：构建、路由、菜单、大页结构。
3. API 和数据密度：接口可用、旧库非空、新库空态合理。
4. 写入和流水：Redis buffer、TCP 写入、健康流水、预警流水。
5. 功能闭环：预警处理、SOS、入井准入、AI、报表、后台 CRUD。
6. 性能和观测：慢接口预算、慢 SQL、actuator metrics。
7. 页面和交互：桌面/移动端截图、布局、按钮、弹窗、表格、图表。

---

## 2. 环境预检方法

### 2.1 服务预检

```powershell
Invoke-WebRequest -Uri 'http://localhost:8080/health' -Method Head -TimeoutSec 5
Invoke-WebRequest -Uri 'http://localhost:9528/' -Method Head -TimeoutSec 5
```

### 2.2 端口预检

```powershell
Get-NetTCPConnection -LocalPort 8080,9000,9528 -State Listen |
  Select-Object LocalAddress,LocalPort,OwningProcess
```

### 2.3 后端启动

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn spring-boot:run -f D:/Health/HealthData/pom.xml
```

### 2.4 前端启动

```powershell
cd D:/Health/HealthShow
npm run dev
```

### 2.5 模拟器控制

旧库演示、旧库数据密度、旧库 pipeline 测试需要模拟器。

```powershell
cd D:/Health/HealthShow
python watch_tcp_simulator_1000.py
```

新库真实手表验收前必须停止模拟器，避免误判：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
```

---

## 3. 通用自动化基线

无论新库还是旧库，只要改动代码，都先跑通用基线。

### 3.1 后端基线

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
```

### 3.2 前端结构和构建基线

```powershell
cd D:/Health/HealthShow
npm run audit:structure
npm run build
```

### 3.3 前端业务基线

```powershell
cd D:/Health/HealthShow
npm run audit:api
npm run audit:write
npm run audit:auth
npm run audit:e2e
```

注意：

- `audit:auth` 会主动重启后端，不能和 `audit:e2e`、`audit:pipeline` 并行。
- `audit:e2e` 会先跑 `audit:preflight`。
- 失败时先看脚本产物和接口实际状态，不要根据一次瞬时 500 直接改生产代码。

---

## 4. 旧库专项测试方法

旧库测试目标：

- 证明 `health` 下核心页面和组件不再大面积空。
- 证明千万级历史数据下接口可接受。
- 证明模拟器、旧库 pipeline、预警流水和页面展示闭环可跑。

### 4.1 旧库数据源确认

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data
```

通过要求：

- `data_source: old`
- `failed: 0`
- 核心组件返回非空或命中旧库空态白名单

### 4.2 旧库完整回归

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:structure
npm run audit:api
npm run audit:data
npm run audit:write
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
npm run audit:pipeline-warning
npm run build
```

### 4.3 旧库必须验证的数据契约

| 契约 | 验证方法 | 失败判断 |
| --- | --- | --- |
| `health_record` 主表为空但分表有数据 | 查 `health_record_20*` 和对应接口 | 新接口只查主表导致组件空 |
| `warning_record` 主表为空但分表有数据 | 查 `warning_record_20*` 和预警接口 | 预警页或通知页异常空 |
| 温度缩放 | SQL 原始值和页面展示对照 | `364.0` 被展示成 `364` 而不是 `36.4` |
| 局部指标 `NULL` | 取最新健康记录样本 | 页面把缺失值误判为 0 或异常 |
| 在线状态来源 | 对照 `device.online.count`、最近健康记录、状态表 | 状态表空时维护者无法解释在线 1000 |

### 4.4 旧库数据密度扩展要求

`audit:data` 后续应从当前关键检查扩展到至少 50 项，覆盖：

- `safety-command`：KPI、区域/地图、部门排行、告警摘要。
- `dashboard`：KPI、趋势、部门、排行、准入、AI 摘要。
- `workbench`：日历、待办、准入、预警摘要。
- `real-time`：在线列表、实时告警、设备状态、体征卡。
- 指标页：心率、压力、血压、血氧、睡眠。
- `risk-warning` / `trend-warning`：列表、趋势、类型、部门统计。
- `report-center`：报表列表、图表、导出入口。
- `ai-chat`：会话、示例问题、查询结果、降级提示。
- 预警管理：通知、SOS、记录、配置。
- 后台管理：设备、用户、角色、部门、工种。

### 4.5 旧库性能预算

旧库高数据量下必须记录耗时。建议阈值：

| 接口类型 | 目标 |
| --- | ---: |
| 顶部 KPI / overview | 800ms 内 |
| 页面首屏核心接口 | 1200ms 内 |
| 历史记录分页 | 1500ms 内 |
| 跨月重统计 | 2500ms 内，且需要缓存 |
| 导出类接口 | 允许异步或进度提示，不阻塞首屏 |

处理规则：

- 500ms 以上进入观察清单。
- 1000ms 以上必须解释原因或优化。
- 2500ms 以上不得作为页面首屏硬依赖。
- 5 秒级接口必须建 issue，除非它是人工触发的长任务并有进度反馈。

---

## 5. 新库专项测试方法

新库测试目标：

- 证明 `health_new` 空业务数据是预期空态，不是接口坏。
- 证明真实手表写入新库。
- 证明模拟器仍只写旧库。
- 证明新库初始化流程可执行。

### 5.1 新库数据源确认

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='new'
$env:API_EXPECT_NON_EMPTY='0'
npm run audit:data
```

通过要求：

- `data_source: new`
- 页面允许空的组件必须显示新库空态说明。
- 不允许把老库缓存数据显示到新库页面。

### 5.2 新库空态测试

必须检查：

- 登录可用：`admin / admin123`。
- 系统基础表可用：角色、权限、工种、阈值配置。
- 业务基础表预期为空：部门、员工、设备、绑定、流水、预警。
- 页面文案区分：
  - 新库预期空：提示“新库暂无业务数据，请初始化员工、部门、设备或接入真实手表”。
  - 旧库异常空：提示“旧库数据异常或数据源选择错误”。

### 5.3 新库初始化测试

验收步骤：

1. 切到新库。
2. 停止模拟器。
3. 确认 `health_new.department / employee / device / device_user` 当前状态。
4. 通过脚本或页面重建部门、员工、设备、绑定。
5. 刷新页面，确认对应管理页从空态变为有数据。
6. 再跑：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='new'
$env:API_EXPECT_NON_EMPTY='0'
npm run audit:data
```

如果新库已经初始化了业务数据，可以临时改为：

```powershell
$env:API_EXPECT_NON_EMPTY='1'
```

但必须在报告里注明“新库已初始化，不再按空库验收”。

### 5.4 真实手表写入新库测试

验收步骤：

1. 停止模拟器。
2. 确认真实手表 IMEI 不命中 `^3594567800\d{5}$`。
3. 真实手表连接 TCP `9000`。
4. SQL 查询 `health_new.device`、`health_new.health_record_YYYYMM` 或对应分表。
5. 前端切到新库，确认真实手表数据可见。
6. 响应头必须是 `X-Health-Data-Source: new`。

失败判断：

- 真实手表写入旧库。
- 新库页面出现旧库模拟器数据。
- 模拟器进程仍在后台影响新库验收。

---

## 6. 双库隔离测试方法

双库相关改动必须做隔离测试。

### 6.1 请求侧隔离

验证点：

- 不带请求头时，后端默认策略符合 `application.yml`。
- 带 `X-Health-Data-Source: old` 时查旧库。
- 带 `X-Health-Data-Source: new` 时查新库。
- 响应头回写实际数据源。
- Cookie `Health-Data-Source` 与顶栏一致。

### 6.2 写入侧隔离

验证点：

- 真实手表默认写新库。
- 模拟器默认写旧库。
- Redis key 使用：
  - `health:buffer:old`
  - `health:buffer:new`
- Redis flush 后进入对应 SQL 库。

### 6.3 切换交互

前端必须验证：

- 顶栏从老库切新库后，页面刷新请求头变为 `new`。
- 顶栏从新库切老库后，页面刷新请求头变为 `old`。
- 切换后旧页面缓存不污染当前数据源。
- 空态文案随数据源变化。

---

## 7. 功能闭环测试方法

自动化脚本能证明“页面能打开”，但不能替代业务闭环。下面这些必须有人工或 Playwright 操作证据。

### 7.1 预警处理闭环

步骤：

1. 旧库打开消息通知中心。
2. 选择一条待处理预警。
3. 执行处理或批量处理。
4. 确认通知列表状态变化。
5. 确认预警记录同步变化。
6. 确认大盘待处理数字变化。
7. 保存截图和 SQL/API 证据。

自动化补充：

```powershell
cd D:/Health/HealthShow
npm run audit:pipeline-warning
```

### 7.2 SOS 闭环

步骤：

1. 构造或选择 SOS 数据。
2. 查看 SOS 列表。
3. 执行处理。
4. 查看记录页。
5. 确认大盘/通知相关数字变化。

### 7.3 入井准入闭环

步骤：

1. 查看准入名单。
2. 查看禁入或待复核人员原因。
3. 执行复核或处理动作。
4. 确认统计变化。
5. 记录规则来源和口径。

### 7.4 报表和导出闭环

步骤：

1. 选择旧库代表性时间范围。
2. 生成报表。
3. 导出 Excel/PDF。
4. 校验字段、行数、统计口径。
5. 大数据量导出不得卡死页面。

### 7.5 AI 闭环

必须覆盖三类路径：

- 成功：AI 聊天和 AI 报告能正常返回。
- 拒绝：危险 SQL 或越权问题被拒绝，前端提示清楚。
- 失败：模型不可用、超时、限流时，前端不白屏，后端有日志和指标。

### 7.6 后台管理 CRUD

范围：

- 设备
- 用户
- 角色
- 部门
- 工种
- 阈值配置

每个页面至少确认：

- 列表
- 查询
- 新增或编辑
- 删除、停用或只读边界
- 权限变化或角色影响

---

## 8. 页面美观与交互测试方法

页面测试不只看 DOM 通过，还要有截图和交互证据。

### 8.1 必测页面

桌面必测：

- `/safety-command/index`
- `/health-monitor/dashboard`
- `/health-monitor/workbench`
- `/health-monitor/real-time`
- `/health-monitor/risk-warning`
- `/health-monitor/employee-archive`
- `/health-monitor/report-center`
- `/ai-chat/index`
- `/device-management/index`
- `/user-list/index`
- `/role-management/index`

移动端必测：

- `/safety-command/index`
- `/health-monitor/dashboard`
- `/health-monitor/workbench`
- `/health-monitor/real-time`
- `/health-monitor/risk-warning`
- `/alert-management/notifications`

### 8.2 视口

最低覆盖：

- `1440 x 900`
- `1920 x 1080`
- `390 x 844`

重要演示前补充：

- `1366 x 768`
- `414 x 896`

### 8.3 自动判断项

Hermes/Playwright 优先判断这些明确问题：

- 页面白屏。
- 控制台报错。
- 核心按钮不可点击。
- 弹窗、抽屉、分页、筛选无响应。
- 图表容器 0 宽高。
- 元素重叠、遮挡、溢出视口。
- 表格列严重错位。
- 移动端横向滚动异常。
- 文本严重截断。
- 接口失败被伪装成“暂无数据”。

### 8.4 人工判断项

这些只能作为人工验收结论，不能完全交给自动化裁定：

- 是否高级。
- 配色是否更好。
- 视觉风格是否统一。
- 信息密度是否适合现场投屏。

人工判断必须配截图和具体位置，不要只写“页面不好看”。

### 8.5 UI issue 必填字段

```json
{
  "issue_id": "healthshow-ui-20260508-001",
  "project": "HealthShow",
  "category": "ui-e2e",
  "page": "/health-monitor/dashboard",
  "viewport": "1440x900",
  "interaction": "页面加载后默认态",
  "expected": "核心卡片、图表和操作按钮不重叠，数据可读",
  "actual": "右侧卡片遮挡趋势图 legend",
  "failure_signature": "layout-overlap:dashboard-right-panel",
  "artifact_paths": [
    "D:/Health/tests/runs/<run-id>/screenshots/dashboard-1440-overlap.png"
  ],
  "suspected_files": [
    "D:/Health/HealthShow/src/views/health-monitor/dashboard/index.vue",
    "D:/Health/HealthShow/src/views/health-monitor/dashboard/dashboard.scss"
  ],
  "verification_command": "cd D:/Health/HealthShow && npm run audit:e2e"
}
```

---

## 9. Hermes 编排方法

Hermes 不直接判断业务是否正确，它负责：

- 调度命令。
- 归档日志。
- 拷贝脚本产物。
- 聚类失败。
- 生成单个 issue 包。
- 让 Codex 定点修复。
- 修复后回归验证。

### 9.1 推荐运行目录

```text
D:/Health/tests/runs/<run-id>/summary.json
D:/Health/tests/runs/<run-id>/summary.md
D:/Health/tests/runs/<run-id>/logs/*.combined.log
D:/Health/tests/runs/<run-id>/issues/*.json
D:/Health/tests/runs/<run-id>/issues/*.md
D:/Health/tests/runs/<run-id>/screenshots/*
D:/Health/tests/runs/<run-id>/videos/*
D:/Health/tests/latest-run.json
D:/Health/tests/open-issues.json
```

### 9.2 标准分组

建议 Hermes 每轮分组输出：

| stage | 命令 |
| --- | --- |
| `backend-test` | `mvn -q test` |
| `backend-regression` | `python run_backend_regression.py` |
| `frontend-structure` | `npm run audit:structure` |
| `frontend-api` | `npm run audit:api` |
| `frontend-data-old` | `API_DATA_SOURCE=old API_EXPECT_NON_EMPTY=1 npm run audit:data` |
| `frontend-data-new` | `API_DATA_SOURCE=new API_EXPECT_NON_EMPTY=0 npm run audit:data` |
| `frontend-write` | `npm run audit:write` |
| `frontend-auth` | `npm run audit:auth` |
| `frontend-e2e` | `npm run audit:e2e` |
| `pipeline-health` | `npm run audit:pipeline` |
| `pipeline-warning` | `npm run audit:pipeline-warning` |
| `frontend-build` | `npm run build` |

### 9.3 运行节奏

提交前轻量：

```powershell
cd D:/Health/HealthShow
npm run audit:structure
npm run build
```

改后端：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
```

改数据源、接口或页面：

```powershell
cd D:/Health/HealthShow
npm run audit:api
npm run audit:data
npm run audit:e2e
```

发布前完整：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
npm run audit:structure
npm run audit:api
npm run audit:data
npm run audit:write
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
npm run audit:pipeline-warning
npm run build
```

---

## 10. 失败分诊方法

### 10.1 先判断环境失败还是代码失败

环境失败特征：

- 前端或后端端口不可访问。
- SQL Server / Redis 不可用。
- WSL 访问 Windows localhost 失败。
- `audit:auth` 重启后端期间并行测试误失败。
- 模拟器没启动导致旧库 pipeline 无数据。

代码失败特征：

- 同一命令复跑稳定失败。
- 同一接口手工请求也失败。
- 产物里有稳定断言失败。
- 截图显示固定页面错位或白屏。
- 日志指向具体异常栈。

### 10.2 issue 严重级别

| 级别 | 判断 |
| --- | --- |
| `blocker` | 后端无法启动、前端无法启动、登录完全失效、主 API 全部失败 |
| `high` | 写入链路失败、双库写错库、核心大盘不可用、真实手表链路断 |
| `medium` | 单个业务页面或操作失败、慢接口超预算、核心组件旧库为空 |
| `low` | 非关键 UI 偏移、文案、轻微 warning |

### 10.3 给 Codex 的任务包规则

每个 issue 只包含一个问题。

必须包含：

- 失败命令。
- 数据源。
- 复现步骤。
- 期望结果。
- 实际结果。
- 失败签名。
- 日志摘录。
- 产物路径。
- 怀疑文件。
- 修复后验证命令。

不允许：

- 一个 issue 混入多个页面、多个接口、多个根因。
- 只有“很多页面有问题”这类泛泛描述。
- 没有数据源的失败报告。

---

## 11. 最终验收方法

### 11.1 旧库达标

旧库可以说“测试达标”的条件：

- 通用自动化基线通过。
- `audit:data` 在 old 下通过，核心组件非空。
- 健康 pipeline 和预警 pipeline 通过。
- 慢接口有预算、报告和整改记录。
- 分表、温度缩放、局部 NULL、在线状态来源有文档说明。
- 预警、准入、报表、AI、后台 CRUD 至少有一轮操作证据。
- 关键页面有桌面和移动端截图基线。

### 11.2 新库达标

新库可以说“测试达标”的条件：

- 通用自动化基线通过。
- `audit:data` 在 new 下通过，空态符合预期。
- 新库初始化流程有步骤或脚本。
- 真实手表写入 `health_new` 成功。
- 模拟器仍写旧库。
- 前端切新库不显示老库缓存。
- 响应头与 SQL 证据一致。

### 11.3 双库整体达标

双库可以说“整体达标”的条件：

- 请求侧 old/new 可切换。
- 写入侧真实手表/模拟器隔离。
- Redis buffer old/new 隔离。
- 新库空态和旧库异常空态文案不同。
- `AGENTS.md`、`docs/archive/历史归档-HEALTH_HANDOFF.md`、`DUAL_DB_CUTOVER.md` 记录最新事实。

---

## 12. 下次给 Hermes 的短提示词

```text
你正在按 D:/Health/HEALTH_TEST_METHOD.md 编排 Health 项目测试。

必须先区分数据源：
- old: 旧库 health，核心组件应非空，模拟器可运行
- new: 新库 health_new，业务空态可能是预期，真实手表验收前必须停模拟器

每轮输出：
- run-id
- 执行命令
- 数据源
- 结果
- 产物路径
- open issues

失败时只生成单个结构化 issue，不要把多个根因混在一起。
每个 issue 必须带失败命令、数据源、复现步骤、期望、实际、失败签名、截图或日志、怀疑文件、验证命令。
```

## 13. 下次给 Codex 的短提示词

```text
你正在处理 D:/Health 的单个测试失败 issue。

先读：
- D:/Health/AGENTS.md
- D:/Health/HEALTH_TEST_METHOD.md
- issue json/md

要求：
- 先确认数据源 old/new
- 先复现
- 最小范围修改
- 不回滚用户现有改动
- 修复后跑 issue 指定 verification_command
- 如果涉及数据源、写入、预警、登录或大屏，再跑相关回归
- 输出修改文件、根因、验证命令、验证结果、残余风险
```


