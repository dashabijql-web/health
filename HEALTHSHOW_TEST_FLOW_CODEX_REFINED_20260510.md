# HealthShow 测试流程 Codex 审核完善版（2026-05-10）

本文档是在 Hermes 版流程与实跑报告基础上的收口版。目标不是替代已有脚本，而是把 Health 当前最容易误判的边界固定下来：双库、模拟器、后端门禁、全栈产物、失败语义和 Codex/Hermes 分工。

## 1. 结论

当前最稳的测试执行模型：

```text
Windows PowerShell runner 负责真实执行
  -> Hermes 负责计划、归档、摘要、单 issue 包
  -> Codex 负责定点修复、关键链路复跑和最终技术判断
```

如果只是无人值守回归、归档和生成报告，可以交给 Hermes 编排，但 Hermes 不应直接长时间接管 Windows 测试命令。如果测试结果会触发代码修改、双库判断、pipeline 修复或上线前验收，应该交给 Codex 主导执行和判断。

原因：

- Health 的真实依赖在 Windows 本地：SQL Server、Redis、Spring Boot、Vite、TCP 9000、模拟器。
- 当前 Hermes 更适合读 summary、整理日志、生成 issue，不适合做最终环境判断。
- Codex 可以同时读代码、修脚本、跑验证、判断 failure 是环境问题、测试问题还是代码问题。
- 主 runner 应该是脚本，不应该是某个 agent 的临时命令流。

## 2. Hermes 版流程评价后需要修正的点

Hermes 已经做对的部分：

- 已建立 `test:fast`、`test:frontend`、`test:quality`、`test:integration:*`、`test:perf:*`、`test:full:*` 分层入口。
- 已有 `tests/run-full-stack-local.ps1`，能启动/检查 SQL、Redis、后端、前端和模拟器。
- 已把 `old/new` 数据源、数据密度、性能、pipeline 和 warning pipeline 纳入前端脚本。
- 最新 run 有真实产物，不是纯文档。

必须修正或补强的部分：

- 一键命令路径在 Hermes 流程文档中被转义损坏，必须写成可复制的 PowerShell 命令。
- `full-stack-local-summary.json` 的 `steps` 不应混入日志字符串，应只保留结构化 step 对象。
- `test:integration` 文档说覆盖 e2e，但真实 runner 只跑 API/data/auth，文档和实现必须对齐。
- `full:new` 失败不是产品代码必然失败，而是 new 库无 warning 探针目标；应记为 `skipped` 或 `blocked`，不能误报为代码失败。
- 当前 full stack runner 连续跑 old/new 时会保持模拟器运行；这不符合“新库验收先停模拟器”的项目规则。
- full stack 还缺后端 `mvn -q test` 和后端 regression 作为硬门禁。
- warning pipeline 需要适配 `health:buffer:old` / `health:buffer:new`，不能只看历史 `health:buffer`。
- 产品体验脚本是关键词启发式扫描，只能作为弱信号，不能替代截图和人工/Playwright 交互验收。

## 3. 结果语义

所有测试结果只允许落到以下状态：

| 状态 | 含义 | 是否可发布 |
| --- | --- | --- |
| `passed` | 测试执行完成且断言通过 | 可继续下一档 |
| `failed` | 代码行为、接口契约、页面行为或性能预算失败 | 不可发布 |
| `blocked` | 环境、依赖、凭据、浏览器、SQL/Redis/TCP 不可用 | 不可发布，但不直接改业务代码 |
| `skipped` | 当前数据源不具备测试前置数据，且这是项目规则允许的状态 | 需在报告中解释 |
| `warning` | 非阻断风险，例如性能接近阈值、启发式 UX 扫描缺口 | 可继续，但需进入 backlog |

关键规则：

- 旧库 `old` 无探针目标、核心组件为空、pipeline 缺数据，默认是 `failed`。
- 新库 `new` 缺业务数据、缺 warning 探针目标，若符合空库设计，应是 `skipped` 或 `blocked`，不是 `failed`。
- 任何 `blocked` 都不能包装成 `passed`。
- 任何脚本降级都必须在 summary 中写明原因。

## 4. 数据源前置规则

Health 的测试必须先分清数据源：

| 数据源 | 数据库 | 用途 | 自动化口径 |
| --- | --- | --- | --- |
| `old` | `health` | 模拟器、演示、历史数据密度 | 核心组件应非空 |
| `new` | `health_new` | 真实手表、空库上线、初始化流程 | 业务空态可预期 |

固定操作顺序：

```text
旧库测试：切 old -> 启动模拟器 -> 跑 old 数据密度/pipeline/perf
新库测试：切 new -> 停止模拟器 -> 跑 new 空态/真实手表/隔离验证
```

每轮 summary 必须记录：

- `API_DATA_SOURCE`
- `API_EXPECT_NON_EMPTY`
- `X-Health-Data-Source` 请求头
- `X-Health-Data-Source` 响应头
- Cookie `Health-Data-Source`
- SQL 库名：`health` 或 `health_new`
- Redis key：`health:buffer:old` 或 `health:buffer:new`
- 模拟器进程是否运行

## 5. 测试档位

### 5.1 fast：纯前端快速检查

用途：小改后快速防止明显回归，不依赖后端、数据库和浏览器。

```powershell
cd D:/Health/HealthShow
npm run test:fast
```

最低通过条件：

- warning 语义测试通过。
- 不产生后端、数据库、浏览器依赖。

### 5.2 frontend：前端结构与构建

用途：改路由、页面结构、样式、组件后运行。

```powershell
cd D:/Health/HealthShow
npm run test:frontend
```

等价关键门禁：

- `test:warning-semantics`
- `audit:structure`
- `npm run build`

### 5.3 backend：后端硬门禁

用途：任何后端、SQL、双库、TCP、Redis、AI、mapper/service 相关改动。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn.cmd -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
```

通过条件：

- Maven test 通过。
- 后端 regression 通过。
- 不出现字段同步、Redis flush、mapper 类型回退类问题。

### 5.4 integration-old：旧库集成验证

用途：确认老库演示数据、页面密度、API 和登录正常。

前置：

- 后端 `8080` 可用。
- 前端 `9528` 可用。
- SQL Server 和 Redis 可用。
- 模拟器按需运行。

命令：

```powershell
cd D:/Health/HealthShow
npm run test:integration:old
```

必须确认：

- `API_DATA_SOURCE=old`
- `API_EXPECT_NON_EMPTY=1`
- `audit:data:old` 不允许核心组件为空。

### 5.5 integration-new：新库空态验证

用途：确认 new 库空业务数据不是接口坏，不显示旧库缓存。

前置：

- 停止模拟器。
- 确认 `health_new.department/employee/device/device_user` 当前是否为空。

命令：

```powershell
cd D:/Health/HealthShow
npm run test:integration:new
```

必须确认：

- `API_DATA_SOURCE=new`
- `API_EXPECT_NON_EMPTY=0`
- 空态应可解释，不应把旧库数据带到新库页面。

### 5.6 perf-old / perf-new：性能门禁

用途：记录 API 和页面加载性能。性能门禁不得代替功能门禁。

```powershell
cd D:/Health/HealthShow
npm run test:perf:old
npm run test:perf:new
```

判定：

- old 库允许 `health-record.page` 使用 legacy 阈值，但必须记录真实 p50/p95/max。
- new 库应保持严格阈值。
- 性能报告必须引用 `tests/performance/artifacts/<run-id>/*.md`。

### 5.7 full-old：旧库完整验收

用途：发布前、演示前、老库功能闭环验收。

推荐顺序：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn.cmd -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
npm run test:full:old
```

额外要求：

- 模拟器应运行。
- `audit:pipeline` 必须通过。
- `audit:pipeline-warning` 必须通过。
- `audit:data:old` 必须非空通过。

### 5.8 full-new：新库完整验收

用途：真实手表、新库空态、上线切换前验证。

推荐顺序：

```powershell
Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -match 'watch_tcp_simulator_1000\.py' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn.cmd -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
npm run test:full:new
```

`full-new` 的特别规则：

- 如果 new 库没有绑定设备/员工，普通 pipeline 可 `skipped`。
- 如果 new 库没有 warning 探针目标，warning pipeline 应 `skipped` 或 `blocked`，不是 `failed`。
- 如果 new 库已初始化真实业务数据，则可改为严格执行 pipeline，但报告必须注明 new 库不是空库。
- 模拟器仍在运行时，`full-new` 结果不能作为最终验收结果。

## 6. 一键 runner 应如何修正

现有脚本：

```text
D:/Health/HealthShow/tests/run-full-stack-local.ps1
```

可继续保留，但建议改造为两个互斥阶段：

```text
phase old:
  启动模拟器
  跑 test:full:old

phase new:
  停止模拟器
  跑 test:full:new
```

建议新增或调整参数：

```powershell
param(
  [ValidateSet('old','new','both')]
  [string]$DataSource = 'old',

  [switch]$IncludeBackendTests,
  [switch]$StartSimulatorForOld,
  [switch]$StopSimulatorForNew
)
```

runner 必须满足：

- 每条命令独立 timeout。
- `summary.json` 只保存结构化对象，不混入日志行。
- stdout/stderr 分开保存，并生成 combined log。
- 失败后继续写 summary。
- 不在 `D:/Health` 根目录执行 git 命令。
- 不并行运行 `audit:auth` 与 e2e/pipeline。
- 不输出真实密码、token、SQL 连接串。
- 记录启动或复用的进程 PID。
- 如果已有 `HealthData` 后端进程带可见窗口，必须先停止并静默重启；不能只因为 `8080` 已监听就复用手工启动的 Java/Maven 控制台。
- `audit:auth` 的后端重启必须 detached：使用 `Win32_ProcessStartup(ShowWindow=0)` + `Win32_Process.Create`，避免 Maven 子进程继承父 PowerShell stdout/stderr 管道导致 auth 审计卡住。
- 对自己启动的进程负责清理；不要无差别杀掉用户长期服务。

可复制的一键命令必须写成：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\HealthShow\tests\run-full-stack-local.ps1"
```

## 7. 报告标准

每轮必须有：

```text
tests/runs/<run-id>/
  summary.json
  summary.md
  *.out.log
  *.err.log
  *.combined.log
```

`summary.json` 至少包含：

```json
{
  "run_id": "20260510-010000-full-old",
  "profile": "full-old",
  "status": "passed",
  "started_at": "2026-05-10T01:00:00+08:00",
  "finished_at": "2026-05-10T01:12:00+08:00",
  "environment": {
    "frontend": "http://127.0.0.1:9528/",
    "backend": "http://127.0.0.1:8080/health",
    "sql_server": "127.0.0.1:58135",
    "redis": "127.0.0.1:6379",
    "tcp": "127.0.0.1:9000"
  },
  "data_source": {
    "requested": "old",
    "expect_non_empty": true,
    "sql_db": "health",
    "redis_key": "health:buffer:old",
    "simulator_running": true
  },
  "git": {
    "HealthShow_status": "logs/git-healthshow-status.log",
    "HealthData_status": "logs/git-healthdata-status.log"
  },
  "commands": [
    {
      "name": "frontend-full-old",
      "command": "npm run test:full:old",
      "exit_code": 0,
      "status": "passed",
      "duration_ms": 300000,
      "stdout": "test-full-old.out.log",
      "stderr": "test-full-old.err.log"
    }
  ],
  "artifacts": [],
  "issues": []
}
```

`summary.md` 必须 1 分钟内可读：

- 本轮总体状态。
- 运行环境是否 ready。
- 数据源和模拟器状态。
- 命令通过/失败表。
- 关键 artifact 路径。
- 失败分类：failed / blocked / skipped。
- 最高优先级 issue。
- 下一步建议。

## 8. 失败分诊优先级

失败只生成一个最高优先级 issue，不要一轮混多个根因。

优先级：

1. 服务不可用：SQL、Redis、后端、TCP、前端。
2. 登录和权限：`audit:auth`。
3. 双库错路由：请求头、响应头、Cookie、SQL 库不一致。
4. 写入链路：TCP、Redis、SQL 分表、API、页面任一断。
5. 预警链路：warning 生成、列表、处理、SQL 状态不同步。
6. API smoke 大面积失败。
7. 旧库数据密度失败。
8. 页面 e2e 白屏、console error、关键操作失败。
9. build / structure 失败。
10. 性能预算超限。
11. 启发式 UX / code-health warning。

issue 包必须包含：

- `data_source`
- `failing_command`
- `exit_code`
- `expected`
- `actual`
- `failure_signature`
- `artifact_paths`
- `suspected_files`
- `verification_command`
- 是否允许自动修复

## 9. 必须补齐的专项验收

### 9.1 双库隔离

至少验证：

- old 请求返回 old 响应头。
- new 请求返回 new 响应头。
- old 数据密度非空。
- new 空态不显示 old 缓存。
- 模拟器 IMEI 只写 old。
- 真实手表默认写 new。
- Redis key 使用 `health:buffer:old/new`。

### 9.2 页面截图

自动 e2e 通过后，关键页面仍需截图验证：

- `1440x900`
- `1920x1080`
- `390x844`
- `414x896`

重点页面：

- `/safety-command/index`
- `/health-monitor/dashboard`
- `/health-monitor/workbench`
- `/health-monitor/real-time`
- `/health-monitor/risk-warning`
- `/health-monitor/employee-archive`
- `/health-monitor/report-center`
- `/alert-management/notifications`
- `/ai-chat/index`

### 9.3 真实手表

新库上线前必须做人工或现场协助验证：

- 停止模拟器。
- 确认真实手表 IMEI 不命中模拟器正则。
- 手表连入 TCP 9000。
- SQL 证明写入 `health_new.device` 或对应月分表。
- 前端切 new 后能看到真实数据或明确空态。

## 10. 谁来跑流程

推荐分工：

| 场景 | 应由谁主导 | 原因 |
| --- | --- | --- |
| 日常无人值守回归 | Hermes 编排 + PowerShell runner 执行 | Hermes 适合计划、归档、摘要 |
| 只想生成测试报告 | Hermes | 不需要改代码时，Hermes 成本更低 |
| 失败后要判断根因 | Codex | 需要读代码、读脚本、判断环境/测试/业务 |
| 失败后要修复 | Codex | 需要最小改动和验证 |
| 双库、pipeline、真实手表验收 | Codex 主导，Hermes 可整理报告 | 需要严格业务判断 |
| 发布前最终验收 | Codex 主导跑关键命令，Hermes 归档 | 最终技术责任应在能读代码和修复的一方 |
| 长时间 nightly | Hermes 调度 runner，不直接手写命令流 | 避免 agent 会话不稳定 |

一句话：

```text
Hermes 适合当测试秘书和报告员；Codex 适合当测试负责人和修复负责人；真正执行命令的应该是稳定 PowerShell runner。
```

## 11. 当前落地状态与下一步

2026-05-10 已落地：

- `tests/run-full-stack-local.ps1` 已改为 OpenClaw 可触发的参数化入口：`-DataSource old/new/both`。
- runner 默认数据源已设为 `old`；新库和双库必须显式传 `-DataSource new/both`。
- runner 已输出结构化 `full-stack-local-summary.json` 和人读 `summary.md`。
- runner 已拆分 old/new 阶段：old 阶段按需启动模拟器，new 阶段默认停止模拟器。
- runner 已把后端 `mvn -q test` 和 `run_backend_regression.py` 纳入 full stack 硬门禁，可用 `-SkipBackendTests` 临时跳过。
- `health-warning-pipeline-regression.mjs` 已改为：new 数据源无 warning 探针目标时输出 `skipped`，不误报 failed。
- warning pipeline Redis 观测和清理已覆盖 `health:buffer:<source>` 与历史 `health:buffer`。
- 新增 OpenClaw 入口说明：`HEALTHSHOW_OPENCLAW_TEST_ENTRY_20260510.md`。

还建议继续补：

1. 实跑一轮 `-DataSource old` 和 `-DataSource new`，沉淀最新基线 summary。
2. 让 OpenClaw 只读取 `summary.md` 和 `full-stack-local-summary.json`，不要读取全量日志。
3. 后续可把 runner 的 latest-run 指针写入固定文件，方便 OpenClaw 查询最近一次结果。
4. 若 full run 发现真实失败，再按 summary 中最高优先级失败生成单 issue 交给 Codex。

到这里，OpenClaw 已可以作为标准测试触发入口；但失败根因判断、脚本修改、代码修复和发布前最终判断仍应交给 Codex。
