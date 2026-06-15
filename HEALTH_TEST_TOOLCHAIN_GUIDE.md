# Health 测试工具链使用文档：Hermes / OpenClaw / Codex / Skills / MCP / Scripts

> 入口更新：后续 Health 测试闭环、自主测试、Hermes/OpenClaw/Codex 分工和竞品雷达，以 `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md` 为第一执行入口。本文件保留为工具链细节参考。

整理日期：2026-05-08  
适用目录：`D:/Health`  
配套主测试方法：`D:/Health/HEALTH_TEST_METHOD.md`

本文只讲一件事：以后怎么用 Hermes、OpenClaw、Codex、skills、MCP 和现有脚本，把 `D:/Health` 的测试跑清楚、归档清楚、失败分诊清楚。

---

## 1. 当前结论

当前最稳的执行模型是：

```text
WSL Python runner
  -> 真正执行 HealthData / HealthShow 测试命令

Hermes
  -> 生成测试计划、编排 run-id、汇总日志、生成 issue 包、让 Codex 修复

Codex
  -> 读取 issue 包、定位根因、最小修改、跑验证命令

OpenClaw
  -> 作为可选代理入口、消息/任务转发入口、后续团队协作入口
  -> 当前不建议作为 Health 测试的主 runner
```

原因：

- Health 前端、后端、SQL Server、Redis、模拟器当前都已通过 WSL 主入口统一管理。
- Hermes 当前更适合在 WSL 内做计划、汇总和 issue 分诊，而不是跨到旧 Windows runner。
- OpenClaw 仍可作为可选代理入口，但 Health 测试主执行链已经改成 WSL Python runner。
- 项目本身已经有成熟脚本：`npm run audit:*`、`mvn test`、后端 Python regression。不要重造平行测试体系。

---

## 2. 本机工具可用性核对

### 2.1 Hermes

当前检测结果：

```text
Windows PATH: hermes not found
WSL path: /root/.local/bin/hermes
Version: Hermes Agent v0.12.0 (2026.4.30)
Python: 3.11.15
```

常用命令：

```powershell
wsl -e bash -lc '/root/.local/bin/hermes --version'
wsl -e bash -lc '/root/.local/bin/hermes --help'
wsl -e bash -lc '/root/.local/bin/hermes status'
wsl -e bash -lc '/root/.local/bin/hermes mcp list'
wsl -e bash -lc '/root/.local/bin/hermes doctor'
```

已知状态：

- Hermes 模型端可用。
- Hermes Gateway 当前不是 Health 测试的必要条件。
- Hermes 消息平台在当前核对时未作为 Health 测试依赖。
- 不要在文档或日志里输出 API key。

### 2.2 OpenClaw

当前检测结果：

```text
Windows path: C:/nvm4w/nodejs/openclaw.cmd
WSL path: /mnt/c/nvm4w/nodejs/openclaw
Version: OpenClaw 2026.4.15 (041266a)
```

基础命令：

```powershell
openclaw --version
openclaw --help
openclaw configure
openclaw doctor
openclaw status
```

当前注意：

- `openclaw --version` 和 `openclaw --help` 可正常返回。
- 本次核对时，`openclaw status`、`openclaw mcp --help`、`openclaw agent --help`、`openclaw channels --help` 在当前环境下出现超时。
- 因此，OpenClaw 暂时不作为 Health 测试的稳定主执行器。
- 后续如果要用 OpenClaw 接 Telegram/Discord/其他消息渠道，必须先让 `openclaw status` 和对应 channel health 稳定返回。

### 2.3 Codex

Codex 当前承担：

- 读 `AGENTS.md`、测试文档、issue 包。
- 检查代码和脚本。
- 用 `apply_patch` 做最小修改。
- 用 WSL shell / Python runner 跑验证命令。
- 输出根因、修改文件、验证结果和残余风险。

Codex 不应该承担：

- 长时间无人值守 soak test。
- 一次修多个无关 issue。
- 直接用猜测改双库、登录、pipeline 关键路径。
- 回滚用户已有未提交改动。

---

## 3. 角色分工

| 工具 | 定位 | 当前推荐职责 | 当前不推荐职责 |
| --- | --- | --- | --- |
| Windows PowerShell runner | 实际执行环境 | 跑后端、前端、SQL、Redis、Playwright、模拟器测试 | 生成复杂业务判断 |
| Hermes | 编排和分诊 | 计划、归档、聚类、issue 包、回归调度 | 直接长时间控制 Windows PowerShell |
| Codex | 修复执行 | 单 issue 定位、最小修改、验证 | 长时间测试调度器 |
| OpenClaw | 可选代理/协作入口 | 后续做消息通知、任务转发、agent 入口 | 当前直接跑 Health 主测试 |
| Playwright MCP | 浏览器操作 | 页面点击、截图、console、交互定位 | 替代项目现有 `audit:e2e` |
| SQL Server MCP | 只读数据库核对 | 查库、看表、核对 old/new 数据源 | 写业务数据、改库结构 |
| Redis MCP | 只读 Redis 核对 | 看 Redis 状态、key 数、client | 直接篡改 buffer |

---

## 4. 标准测试目录和产物

所有 Hermes / runner 产物统一写到：

```text
D:/Health/tests/runs/<run-id>/summary.json
D:/Health/tests/runs/<run-id>/summary.md
D:/Health/tests/runs/<run-id>/logs/*.combined.log
D:/Health/tests/runs/<run-id>/issues/*.json
D:/Health/tests/runs/<run-id>/issues/*.md
D:/Health/tests/runs/<run-id>/screenshots/*
D:/Health/tests/runs/<run-id>/videos/*
D:/Health/tests/runs/<run-id>/artifacts/*
D:/Health/tests/latest-run.json
D:/Health/tests/open-issues.json
```

最近一次已通过样例：

```text
D:/Health/tests/runs/20260508-103012/summary.md
status: passed
passed: 5
failed: 0
issues: 0
```

包含命令：

```text
backend-regression
audit-api
audit-auth
audit-e2e
audit-pipeline
```

---

## 5. 现有测试脚本清单

### 5.1 后端脚本

目录：`D:/Health/HealthData/scripts`

| 脚本 | 用途 | 说明 |
| --- | --- | --- |
| `run_backend_regression.py` | 后端本地回归总入口 | 执行 Maven compile、字段同步检查、Redis buffer flush probe |
| `check_health_field_sync.py` | 健康字段同步检查 | 检查健康字段在月分表、视图、实体、Mapper、前端绑定等链路是否同步 |
| `probe_redis_buffer_flush.py` | Redis buffer 到 SQL 分表落库探针 | 当前已适配 `health:buffer:old/new`，默认 old |

推荐命令：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
```

Redis probe 关键环境变量：

```powershell
$env:HEALTH_BUFFER_PROBE_SOURCE='old'   # old 或 new
$env:HEALTH_BUFFER_KEY='health:buffer:old'
$env:SQL_DB='health'
python D:/Health/HealthData/scripts/probe_redis_buffer_flush.py
```

新库 probe 示例：

```powershell
$env:HEALTH_BUFFER_PROBE_SOURCE='new'
$env:HEALTH_BUFFER_KEY='health:buffer:new'
$env:SQL_DB='health_new'
python D:/Health/HealthData/scripts/probe_redis_buffer_flush.py
```

### 5.2 前端 package scripts

目录：`D:/Health/HealthShow`

| 命令 | 用途 | 产物 |
| --- | --- | --- |
| `npm run audit:nav` | 路由、菜单、移动底栏一致性 | 控制台 |
| `npm run audit:page-structure` | 大页目录结构门禁 | 控制台 |
| `npm run audit:structure` | `audit:nav + audit:page-structure` | 控制台 |
| `npm run audit:api` | API smoke | `tests/api/artifacts/<run-id>/summary.*` |
| `npm run audit:data` | 数据密度检查 | `tests/api/artifacts/<run-id>/data-density.*` |
| `npm run audit:write` | 写入接口回归 | `tests/api/artifacts/<run-id>/summary.*` |
| `npm run audit:auth` | 登录态、刷新、退出、后端重启、并发 401 | `tests/e2e/artifacts/<run-id>/auth-summary.*` |
| `npm run audit:e2e` | 页面路由和基础交互审计 | `tests/e2e/artifacts/<run-id>/summary.*` |
| `npm run audit:pipeline` | TCP 健康流水写入到页面可见 | `tests/pipeline/artifacts/<run-id>/summary.*` |
| `npm run audit:pipeline-warning` | 高危预警流水写入、页面处理 | `tests/pipeline/artifacts/<run-id>/warning-summary.*` |
| `npm run audit:nightly` | 夜间完整组合 | 多目录 |
| `npm run build` | 前端构建 | `dist/` |

### 5.3 前端测试文件

| 文件 | 用途 |
| --- | --- |
| `tests/navigation-structure.mjs` | 路由、菜单、移动导航派生关系 |
| `tests/page-structure.mjs` | 已迁移页面结构、`index.vue`、runtime/view-model/scss 边界 |
| `tests/preflight.mjs` | e2e / pipeline 前置环境检查 |
| `tests/api/health-smoke.mjs` | 53 项 API smoke |
| `tests/api/data-density.mjs` | old/new 数据源下组件非空或空态检查 |
| `tests/api/health-write-regression.mjs` | 写接口回归 |
| `tests/e2e/auth-session-regression.mjs` | 鉴权专项回归，会重启后端 |
| `tests/e2e/playwright-audit.mjs` | 页面路由、桌面/移动端基础 e2e |
| `tests/e2e/health-pipeline-regression.mjs` | TCP 健康流水 pipeline |
| `tests/e2e/health-warning-pipeline-regression.mjs` | 高危预警 pipeline |

---

## 6. 新库/旧库测试命令

### 6.1 旧库 old：演示、模拟器、高数据量

旧库测试前确认模拟器可运行：

```powershell
cd D:/Health/HealthShow
python watch_tcp_simulator_1000.py
```

旧库数据密度：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data
```

旧库完整回归：

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

旧库判定：

- 核心组件不应为空。
- 空组件必须进入 issue 或空态白名单。
- 超过性能预算的接口必须记录耗时。

### 6.2 新库 new：真实手表、空库上线

新库真实手表验收前先停模拟器：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
```

新库空态测试：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='new'
$env:API_EXPECT_NON_EMPTY='0'
npm run audit:data
```

新库判定：

- 业务数据为空可以是预期。
- 页面必须显示新库空态，不得显示旧库缓存。
- 真实手表必须写入 `health_new`。
- 模拟器不得写入 `health_new`。

---

## 7. Hermes 怎么用

### 7.1 Hermes 推荐用法

Hermes 适合做：

- 根据 `HEALTH_TEST_METHOD.md` 生成一轮测试计划。
- 生成 run-id。
- 让 WSL runner 执行命令。
- 汇总日志和产物。
- 把失败变成单个 issue 包。
- 让 Codex 修复后再调度回归。

Hermes 不适合直接做：

- 长时间跨环境调度历史 Windows 命令流。
- 跳过本地 runner 直接拼接大段临时测试命令。
- 自己主观判断页面是否好看。

### 7.2 Hermes 一次性规划命令

```powershell
wsl -e bash -lc "cd /mnt/d/Health && /root/.local/bin/hermes -z '按 /mnt/d/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md 和 /mnt/d/Health/HEALTH_TEST_METHOD.md，生成本轮 old 数据源完整测试计划。只输出命令、run-id、产物目录、失败分诊规则，不要直接修改代码。'"
```

新库计划：

```powershell
wsl -e bash -lc "cd /mnt/d/Health && /root/.local/bin/hermes -z '按 /mnt/d/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md，生成 new 数据源空态和真实手表验收测试计划。强调先停模拟器，只输出计划和命令。'"
```

### 7.3 Hermes 生成 issue 包提示词

```text
你是 Hermes，现在为 D:/Health 生成一个给 Codex 的单问题修复任务包。

必须读取：
- D:/Health/HEALTH_TEST_METHOD.md
- D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md
- 本轮 run 的 summary.md
- 失败命令对应 combined.log

要求：
- 一次只生成一个 issue
- 明确数据源 old/new
- 明确 failing_command
- 明确 repro_steps
- 明确 expected / actual
- 明确 failure_signature
- 明确 suspected_files
- 明确 artifact_paths
- 明确 verification_command
- 不要把多个根因混到一个 issue
```

### 7.4 Hermes 输出 issue 模板

```json
{
  "issue_id": "healthshow-data-old-20260508-001",
  "project": "HealthShow",
  "category": "data-density",
  "data_source": "old",
  "title": "旧库 dashboard 某核心组件为空",
  "severity": "medium",
  "failing_command": "cd D:/Health/HealthShow && API_DATA_SOURCE=old API_EXPECT_NON_EMPTY=1 npm run audit:data",
  "repro_steps": [
    "启动后端和前端",
    "确认数据源为 old",
    "运行 audit:data"
  ],
  "expected": "旧库核心组件返回非空数据",
  "actual": "dashboard.xxx value=0",
  "failure_signature": "data-density:dashboard.xxx:old:value=0",
  "suspected_files": [
    "D:/Health/HealthShow/tests/api/data-density.mjs",
    "D:/Health/HealthShow/src/views/health-monitor/dashboard/index.vue"
  ],
  "artifact_paths": [
    "D:/Health/HealthShow/tests/api/artifacts/<run-id>/data-density.md",
    "D:/Health/tests/runs/<run-id>/logs/frontend-data-old.combined.log"
  ],
  "verification_command": "cd D:/Health/HealthShow && $env:API_DATA_SOURCE='old'; $env:API_EXPECT_NON_EMPTY='1'; npm run audit:data"
}
```

---

## 8. OpenClaw 怎么用

### 8.1 当前建议定位

OpenClaw 当前作为可选入口：

- 用来让外部消息渠道触发测试任务。
- 用来转发 Hermes 生成的 issue 包。
- 用来让另一个 agent 读取任务并执行。
- 用来后续和 Telegram / Discord 等协作渠道打通。

当前不建议：

- 直接让 OpenClaw 跑 Health 的完整测试命令。
- 在 `openclaw status` 不稳定前，把它当无人值守调度器。
- 把 Health 的测试正确性建立在 OpenClaw channel 上。

### 8.2 OpenClaw 基础检查

```powershell
openclaw --version
openclaw --help
openclaw doctor
openclaw status
```

如果 `status` 或子命令超时：

- 不要继续让 OpenClaw 直接接管测试。
- 先用 Windows PowerShell runner 跑测试。
- 只把 OpenClaw 当作待修复的协作入口。
- 将超时记录进 run summary 的 `environment_notes`。

### 8.3 OpenClaw 后续可接入流程

目标流程：

```text
OpenClaw channel 收到“跑 old 完整回归”
  -> 转给 Hermes 生成 run plan
  -> Windows runner 执行测试
  -> Hermes 汇总 run
  -> OpenClaw 把 summary / open issues 发回 channel
  -> Codex 按单个 issue 修复
```

OpenClaw 任务消息建议格式：

```text
任务：跑 Health old 完整回归
项目：D:/Health
数据源：old
执行方法：D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md
要求：
- 用 Windows PowerShell runner 跑命令
- 产物写入 D:/Health/tests/runs/<run-id>
- 不要直接改代码
- 失败只生成单问题 issue
```

OpenClaw 转给 Codex 的修复消息建议：

```text
你正在处理 D:/Health 的单个 issue。

先读：
- D:/Health/AGENTS.md
- D:/Health/HEALTH_TEST_METHOD.md
- D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md
- issue json/md

要求：
- 先复现
- 确认数据源 old/new
- 最小修改
- 跑 verification_command
- 输出根因、修改文件、验证结果、残余风险
```

---

## 9. Skills 使用说明

### 9.1 本次文档编写实际使用

本次生成本文档时，没有调用专项 `SKILL.md` 工作流；使用的是 Codex 的通用工程能力和本地命令检查。

实际使用的工具：

- `functions.shell_command`：检查 Hermes/OpenClaw 版本、路径、已有脚本和测试产物。
- `tool_search`：发现当前可用 MCP 工具命名空间。
- `functions.apply_patch`：新增和修改 Markdown 文件。

### 9.2 后续测试推荐 skills

| Skill | 什么时候用 | 说明 |
| --- | --- | --- |
| `playwright` | 需要从终端跑浏览器自动化、截图、点击、UI 调试 | 适合 `audit:e2e` 失败、页面交互 bug |
| `browser-use:browser` | 需要在 Codex 内置浏览器打开 localhost 并观察页面 | 适合快速看页面、截图、手动点击验证 |
| `debug-triage` | 有失败日志、堆栈、测试失败，需要分诊 | 适合把失败收敛成根因和下一步 |
| `runtime-log-diagnose` | 有运行日志、soak log、现场日志 | 适合分析长时间运行问题 |
| `implementation-options` | 需要比较多种修复方案 | 适合大改之前做取舍 |
| `openai-docs` | 涉及 OpenAI API、模型、Responses 等官方用法 | 只用于 OpenAI 产品问题 |

使用原则：

- 跑页面和截图时优先 `playwright` 或 `browser-use:browser`。
- 分析失败先用 `debug-triage` 思路，避免直接改。
- 不要为了“用了 skill”而强行调用 skill；只有任务匹配时再用。

---

## 10. MCP 使用说明

### 10.1 本次发现的可用 MCP

通过工具发现，当前环境可用的 MCP 命名空间包括：

| MCP | 推荐用途 | 注意 |
| --- | --- | --- |
| `mcp__playwright__` | 浏览器导航、截图、snapshot、点击、console | 修改前后页面验证，优先保存截图 |
| `mcp__sqlserver__` | 只读 SQL Server 查询、表结构、表统计 | 只做 SELECT 和结构核对，不做写入 |
| `mcp__redis__` | Redis info、client、dbsize | 只做状态核对，不直接改 buffer |
| `mcp__filesystem__` | MCP 客户端读写文件 | Codex 本地编辑仍优先 `apply_patch` |
| `mcp__context7__` | 查第三方库官方文档 | 仅在需要最新库文档时用 |
| `mcp__memory__` | 搜索长期记忆 | 可做上下文辅助，不替代代码事实 |
| `mcp__github__` | GitHub 仓库/PR/CI 辅助 | 当前 D:/Health 两个子仓库本地为主 |
| `mcp__windows_uia_desktop__` | Windows 桌面 UI 坐标点击 | 仅在无法用浏览器自动化时兜底 |

### 10.2 Playwright MCP 标准流程

适合人工/agent 做页面验证：

```text
1. browser_tabs / browser_navigate 打开 http://localhost:9528/
2. browser_resize 设置视口，例如 1440x900 或 390x844
3. browser_snapshot 获取可点击元素和文本结构
4. browser_click 执行按钮、菜单、弹窗交互
5. browser_console_messages 读取 error/warning
6. browser_take_screenshot 保存截图
```

页面测试必须记录：

- 页面路径。
- 数据源 old/new。
- 视口。
- 操作步骤。
- 截图路径。
- console 错误。
- 对应代码文件。

### 10.3 SQL Server MCP 标准流程

适合只读核对：

```text
1. test_connection
2. list_databases
3. list_tables / describe_table
4. execute_query 做 SELECT
5. get_table_stats 看行数
```

典型查询：

```sql
SELECT COUNT(*) AS cnt FROM employee;
SELECT COUNT(*) AS cnt FROM device;
SELECT COUNT(*) AS cnt FROM health_record_202605;
SELECT COUNT(*) AS cnt FROM warning_record_202605;
```

注意：

- 双库测试时必须确认当前连接的是 `health` 还是 `health_new`。
- SQL MCP 只用于只读核对；写入测试用项目脚本或业务接口。

### 10.4 Redis MCP 标准流程

适合状态核对：

```text
1. info
2. dbsize
3. client_list
```

旧库 / 新库 buffer 仍以项目脚本验证为准：

```powershell
python D:/Health/HealthData/scripts/probe_redis_buffer_flush.py
```

---

## 11. Windows PowerShell runner 标准流程

### 11.1 为什么用 Windows runner

Health 的真实依赖在 Windows：

- 前端 dev server：`localhost:9528`
- 后端 Spring Boot：`localhost:8080/health`
- TCP Netty：`9000`
- SQL Server：`localhost,11433`
- Redis：`127.0.0.1:6379`
- 模拟器：Windows Python 进程

WSL 中的 `localhost`、Windows 代理、WSLInterop 都可能干扰判断。因此真实测试命令优先在 Windows PowerShell 跑。

### 11.2 每条命令的 runner 要求

每条命令必须：

- 独立 timeout。
- stdout / stderr 分文件保存。
- 合并日志保存为 `*.combined.log`。
- 记录 exit code。
- 记录开始时间、结束时间、耗时。
- 失败时仍生成 summary。

日志命名建议：

```text
logs/backend-test.combined.log
logs/backend-regression.combined.log
logs/frontend-api.combined.log
logs/frontend-data-old.combined.log
logs/frontend-auth.combined.log
logs/frontend-e2e.combined.log
logs/pipeline-health.combined.log
```

### 11.3 标准 run summary 字段

`summary.json` 至少包含：

```json
{
  "run_id": "20260508-xxxxxx",
  "project": "D:/Health",
  "data_source": "old",
  "started_at": "2026-05-08T11:30:00+08:00",
  "finished_at": "2026-05-08T11:45:00+08:00",
  "status": "passed",
  "commands": [
    {
      "name": "audit-data-old",
      "command": "cd D:/Health/HealthShow && npm run audit:data",
      "exit_code": 0,
      "seconds": 12.3,
      "log": "D:/Health/tests/runs/<run-id>/logs/audit-data-old.combined.log"
    }
  ],
  "issues": []
}
```

---

## 12. 推荐执行场景

### 12.1 改后端代码

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
cd D:/Health/HealthShow
npm run audit:api
npm run audit:data
```

如果涉及双库、Redis、手表写入，再补：

```powershell
npm run audit:pipeline
npm run audit:pipeline-warning
```

### 12.2 改前端页面

```powershell
cd D:/Health/HealthShow
npm run audit:structure
npm run build
npm run audit:e2e
```

如果页面展示数据：

```powershell
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data
```

如果是登录、请求拦截、Cookie：

```powershell
npm run audit:auth
```

### 12.3 改数据源切换

必须跑：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data

$env:API_DATA_SOURCE='new'
$env:API_EXPECT_NON_EMPTY='0'
npm run audit:data

npm run audit:api
npm run audit:auth
npm run audit:pipeline
```

再手工核对：

- 顶栏显示 old/new。
- 请求头为 `X-Health-Data-Source`。
- 响应头回写实际 source。
- SQL old/new 证据一致。

### 12.4 改页面美观或交互

必须跑：

```powershell
cd D:/Health/HealthShow
npm run audit:e2e
npm run build
```

再用 Playwright MCP 或浏览器截图覆盖：

- `1440x900`
- `1920x1080`
- `390x844`

截图必须落盘，并在 issue 或 summary 中引用。

---

## 13. 失败分诊规则

### 13.1 先判定环境问题

环境问题包括：

- 后端未启动。
- 前端未启动。
- Redis 未启动。
- SQL Server 不可用。
- 模拟器未启动但在跑旧库 pipeline。
- `audit:auth` 重启后端时并行跑了其他测试。
- WSL 无法调用 Windows PowerShell。
- OpenClaw gateway / status 卡住。

处理：

- 不要马上改业务代码。
- 先复跑单个失败命令。
- 手工请求一个失败接口。
- 确认端口和进程。

### 13.2 再判定代码问题

代码问题包括：

- 同一命令稳定失败。
- 同一接口手工请求也失败。
- e2e 截图稳定白屏/遮挡。
- 日志有稳定异常栈。
- old 数据源核心组件稳定为空。
- new 数据源显示 old 缓存。

处理：

- 生成单 issue。
- 让 Codex 修一个根因。
- 修后只跑相关验证，再补必要回归。

---

## 14. Codex 修复单 issue 流程

### 14.1 输入

Codex 必须先读：

- `D:/Health/AGENTS.md`
- `D:/Health/HEALTH_TEST_METHOD.md`
- `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md`
- issue json/md
- 失败日志或截图

### 14.2 执行

步骤：

1. 确认 issue 数据源 old/new。
2. 在对应子仓库看 `git status --short`。
3. 复现失败命令。
4. 读怀疑文件和相邻代码。
5. 做最小修改。
6. 跑 `verification_command`。
7. 涉及高风险链路时补回归。
8. 更新 issue 或 summary。

高风险链路：

- 登录 / Cookie / 请求拦截。
- 数据源切换。
- Redis buffer。
- TCP pipeline。
- 预警。
- Dashboard / safety-command / real-time。
- AI SQL。

### 14.3 输出

Codex 最终输出必须包含：

```text
根因：
修改文件：
验证命令：
验证结果：
产物路径：
残余风险：
```

---

## 15. 不要做的事

- 不要把新库空数据当旧库 bug。
- 不要把旧库空组件解释成“可能正常”，除非白名单写清。
- 不要让 Hermes 直接长时间调 Windows PowerShell。
- 不要在 OpenClaw status 不稳定时把它当主 runner。
- 不要并行跑 `audit:auth` 和 `audit:e2e/pipeline`。
- 不要用 WSL 的 `curl localhost` 判断 Windows 服务。
- 不要让 SQL MCP 或 Redis MCP 写生产数据。
- 不要把多个失败塞给 Codex 一次修。
- 不要在 `D:/Health` 根目录做 git 操作。

---

## 16. 最小可执行方案

如果只想快速跑一轮最有价值测试：

```powershell
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
Write-Host "run_id=$runId"

$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:api
npm run audit:data
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
npm run build
```

如果这轮失败：

1. 保存控制台输出到 `D:/Health/tests/runs/<run-id>/logs`。
2. 复制对应测试脚本产物路径。
3. 让 Hermes 生成单 issue。
4. 让 Codex 修复。
5. 重跑 `verification_command`。

---

## 17. 后续要补的自动化

后续最值得补：

1. 一个稳定的 Windows PowerShell runner 脚本，自动生成 `summary.json/md` 和 `combined.log`。
2. `audit:data` 从 21 项扩到至少 50 项。
3. 关键页面截图基线和视觉 diff。
4. `audit:perf` 或把耗时预算纳入 `audit:data`。
5. OpenClaw status / channel 稳定后，把 summary 和 open issues 推送到消息渠道。
6. Hermes issue 聚类规则固化成脚本或固定 prompt。

---

## 18. 文档关系

以后按这个顺序读：

1. `D:/Health/AGENTS.md`
2. `D:/Health/HEALTH_TEST_METHOD.md`
3. `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md`
4. 旧库任务：`D:/Health/旧库项目重新评估结果.md`、`D:/Health/旧库目标达成实施计划.md`
5. 新库任务：`D:/Health/项目重新评估结果.md`、`D:/Health/目标达成实施计划.md`
6. Hermes 已踩坑：核心经验已吸收进 `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
7. Hermes/Codex 协作方案：`D:/Health/HERMES_CODEX_HEALTH_EXECUTION_PLAN.md`



