# Health Codex 测试闭环演练执行计划

> 入口更新：后续 Health 测试闭环、自主测试、Hermes/OpenClaw/Codex 分工和竞品雷达，以 `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md` 为第一执行入口。本文件保留为 2026-05-08 闭环演练历史参考。

创建日期：2026-05-08  
适用目录：`D:/Health`  
执行对象：Health 工作区下的新一轮 Codex  
目标：按既定工具链角色分工，完整走通一次“测试运行 -> 归档 -> 失败分诊 -> issue 包 -> Codex 修复 -> 回归验证 -> 交付总结”的闭环。

---

## 0. 核心定位

本项目的正确打开方式：

```text
D:/Health = 测试闭环主战场
Codex = 主力修复执行器
Claude Code = 测试架构师 / Reviewer
PowerShell = 稳定 runner
Hermes = 编排和归档
OpenClaw = 后续实验性编排层
```

本次任务不是重写测试体系，也不是直接验证 OpenClaw 的完整能力，而是先验证 Health 项目现有测试闭环是否可以稳定运转。

---

## 1. 本次演练目标

### 1.1 主目标

走通一次最小但真实的测试闭环：

```text
读取项目规则
  -> 检查前后端 git 状态
  -> 建立 run-id 和产物目录
  -> 用 PowerShell 执行标准测试集
  -> 收集日志和测试产物
  -> 判断是否有失败
  -> 如有失败，生成单 issue 修复任务包
  -> Codex 针对单 issue 做最小修复
  -> 运行对应验证命令
  -> 输出闭环总结
```

### 1.2 成功标准

满足以下任一结果，即视为本次闭环演练成功：

1. 标准测试集全部通过，并生成完整 `summary.md` / `summary.json`。
2. 标准测试集存在失败，但成功生成结构化 issue 包，并完成一个单 issue 的修复和回归验证。
3. 标准测试集因环境问题无法继续，但能明确归档阻塞原因、证据日志、下一步处理建议。

### 1.3 失败标准

以下情况视为演练失败：

- 没有按 run-id 归档日志和结果。
- 一次性修多个无关问题。
- 在 `D:/Health` 根目录执行 git 修改、提交、回滚等操作。
- 未区分 `HealthShow` 和 `HealthData` 两个独立仓库。
- 遇到失败后凭猜测修改生产代码。
- 跳过失败测试或删除断言来“通过测试”。
- 直接让 OpenClaw 接管主测试 runner。

---

## 2. 必读上下文

执行前必须先阅读这些文件：

```text
D:/Health/AGENTS.md
D:/Health/HEALTH_TEST_METHOD.md
D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md
D:/Health/HERMES_CODEX_HEALTH_EXECUTION_PLAN.md
```

读取原则：

1. 当前代码和配置优先级最高。
2. `AGENTS.md` 是当前短期协作记忆。
3. `HEALTH_TEST_METHOD.md` 定义测试口径和验收标准。
4. `HEALTH_TEST_TOOLCHAIN_GUIDE.md` 定义 Hermes / OpenClaw / Codex / MCP / scripts 的使用边界。
5. `HERMES_CODEX_HEALTH_EXECUTION_PLAN.md` 定义 Hermes + Codex 的闭环协作模型。
6. 如果文档和当前代码冲突，以当前代码为准，并在 summary 中记录冲突。

---

## 3. 仓库边界与禁止事项

### 3.1 仓库边界

```text
D:/Health              不是 git 仓库
D:/Health/HealthShow   前端独立 git 仓库
D:/Health/HealthData   后端独立 git 仓库
```

所有 git 操作必须分别在子项目内执行：

```powershell
git -C D:/Health/HealthShow status --short
git -C D:/Health/HealthData status --short
```

### 3.2 禁止事项

严禁：

- 在 `D:/Health` 根目录执行 `git add`、`git commit`、`git reset`、`git checkout`、`git clean`。
- 使用破坏性 git 命令丢弃用户改动。
- 未经确认修改数据库结构或清空业务数据。
- 未经确认停止非本次演练创建的长期服务。
- 把多个不相关失败打成一个 Codex 修复任务。
- 为了通过测试而降低测试质量。
- 直接修改 `node_modules`、`target`、`.vite`、日志目录内的第三方产物。

---

## 4. 本次角色分工

| 角色 | 工具 | 本次职责 | 不做什么 |
| --- | --- | --- | --- |
| 测试战场 | `D:/Health` | 存放测试脚本、产物、issue 包、前后端项目 | 不作为 git 仓库操作对象 |
| 主力修复执行器 | Codex | 读取任务包、定位根因、最小修复、跑验证命令 | 不自由发挥、不一次修多个 issue |
| 测试架构师 / Reviewer | Claude Code | 设计流程、审查测试缺口、审查 Codex 结果 | 不替代 PowerShell 长时间跑测试 |
| 稳定 runner | Windows PowerShell | 执行 Maven、npm、Python、Playwright、SQL/Redis 相关脚本 | 不做复杂业务判断 |
| 编排和归档 | Hermes | 后续用于 run-id、日志归档、issue 聚类、summary | 当前不直接长时间控制 Windows 命令 |
| 实验性编排层 | OpenClaw | 后续任务转发、多 Agent 对比、通知入口 | 当前不作为 Health 主 runner |

---

## 5. 产物目录规范

本次演练必须创建唯一 run-id。

### 5.1 run-id 格式

```text
YYYYMMDD-HHMMSS-codex-loop
```

示例：

```text
20260508-143000-codex-loop
```

### 5.2 目录结构

所有产物写入：

```text
D:/Health/tests/runs/<run-id>/summary.md
D:/Health/tests/runs/<run-id>/summary.json
D:/Health/tests/runs/<run-id>/logs/
D:/Health/tests/runs/<run-id>/issues/
D:/Health/tests/runs/<run-id>/artifacts/
D:/Health/tests/runs/<run-id>/screenshots/
D:/Health/tests/runs/<run-id>/videos/
```

同时维护索引：

```text
D:/Health/tests/latest-run.json
D:/Health/tests/open-issues.json
```

### 5.3 日志命名建议

```text
logs/00-preflight.combined.log
logs/01-git-status.combined.log
logs/02-backend-regression.combined.log
logs/03-audit-api.combined.log
logs/04-audit-auth.combined.log
logs/05-audit-e2e.combined.log
logs/06-audit-pipeline.combined.log
logs/07-verification.combined.log
```

---

## 6. 执行前预检

### 6.1 PowerShell 环境预检

使用 Windows PowerShell 执行，不要优先使用 WSL 执行 Health 测试。

建议记录以下命令输出：

```powershell
$PSVersionTable.PSVersion
node -v
npm -v
python --version
D:/apache-maven-3.8.1/bin/mvn -v
```

### 6.2 服务端口预检

```powershell
Get-NetTCPConnection -LocalPort 8080,9000,9528 -State Listen -ErrorAction SilentlyContinue |
  Select-Object LocalAddress,LocalPort,OwningProcess
```

### 6.3 HTTP 预检

```powershell
Invoke-WebRequest -Uri 'http://localhost:8080/health' -Method Head -TimeoutSec 5
Invoke-WebRequest -Uri 'http://localhost:9528/' -Method Head -TimeoutSec 5
```

如果服务未启动，按 `AGENTS.md` 和 `HEALTH_TEST_METHOD.md` 中的命令启动。

---

## 7. Git 状态检查

必须在测试前记录两个子仓库状态。

```powershell
git -C D:/Health/HealthShow status --short
git -C D:/Health/HealthData status --short
```

如存在未提交改动：

1. 不要回滚。
2. 不要覆盖。
3. 在 `summary.md` 中记录。
4. 修复时只改本次 issue 明确需要的文件。
5. 如果未提交改动和本次 issue 冲突，停止并输出 blocker。

---

## 8. 标准测试集

本次先跑第一阶段标准测试集，不直接跑全天候 soak test。

### 8.1 后端回归

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py
```

如需要单独 Maven 测试：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
```

### 8.2 前端 API smoke

```powershell
cd D:/Health/HealthShow
npm run audit:api
```

### 8.3 前端鉴权回归

注意：`audit:auth` 可能主动重启后端，不要和其他 e2e / pipeline 并行。

```powershell
cd D:/Health/HealthShow
npm run audit:auth
```

### 8.4 前端 E2E 基础审计

```powershell
cd D:/Health/HealthShow
npm run audit:e2e
```

### 8.5 健康流水 pipeline

旧库 pipeline 通常需要模拟器。

```powershell
cd D:/Health/HealthShow
npm run audit:pipeline
```

### 8.6 可选重型测试

只有标准测试集稳定后再跑：

```powershell
cd D:/Health/HealthShow
npm run audit:nightly
```

---

## 9. 数据源注意事项

Health 项目有双库路由：

```text
old = health      老库，模拟器/演示/历史数据
new = health_new  新库，真实手表/空库上线切换
```

测试报告必须记录：

- 前端顶栏显示的数据源。
- 请求头 `X-Health-Data-Source`。
- 响应头 `X-Health-Data-Source`。
- `audit:data` 中的 `data_source`。
- 后端实际 SQL 库。

本次标准闭环如果没有特别说明，优先使用本地开发默认旧库口径。

如跑旧库数据密度：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data
```

如跑新库空态：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='new'
$env:API_EXPECT_NON_EMPTY='0'
npm run audit:data
```

切到新库做真实手表或空库验收前，必须停止模拟器，避免误判。

---

## 10. 失败分诊规则

遇到失败时，不要马上改代码。先分类。

### 10.1 分类维度

| 类型 | 判断依据 | 处理方式 |
| --- | --- | --- |
| 环境问题 | 服务未启动、端口不通、数据库/Redis 不可用 | 记录 blocker，不改业务代码 |
| 测试脚本问题 | 脚本断言和当前业务事实冲突、等待条件不合理 | 生成测试脚本 issue |
| 前端问题 | 页面状态、请求头、路由、登录态、组件渲染异常 | 生成 HealthShow issue |
| 后端问题 | API 500、数据源路由、SQL、鉴权、pipeline 写入异常 | 生成 HealthData issue |
| 数据问题 | old/new 口径错误、老库空数据、新库误有模拟器数据 | 先核对数据源和测试口径 |
| flaky 问题 | 重跑一次通过、超时随机、异步等待不稳定 | 记录 flaky 证据，避免盲改业务 |

### 10.2 分诊要求

每个失败至少记录：

- failing command
- exit code
- 关键错误片段
- 相关日志文件
- 相关产物路径
- suspected project: `HealthShow` / `HealthData` / `Environment` / `TestScript`
- suspected files
- recommended verification command

---

## 11. Issue 包格式

如果存在失败，只生成一个最高优先级 issue 包给 Codex 修复。

路径：

```text
D:/Health/tests/runs/<run-id>/issues/<issue-id>.json
D:/Health/tests/runs/<run-id>/issues/<issue-id>.md
```

### 11.1 JSON 模板

```json
{
  "issue_id": "healthshow-auth-20260508-001",
  "project": "HealthShow",
  "category": "auth",
  "title": "backend restart 后前端会话状态异常",
  "severity": "high",
  "stage": "regression",
  "run_id": "20260508-143000-codex-loop",
  "failing_command": "cd D:/Health/HealthShow && npm run audit:auth",
  "exit_code": 1,
  "expected": "鉴权回归脚本通过，过期会话被正确清理或刷新",
  "actual": "脚本在 backend restart 场景失败，页面或接口状态不符合预期",
  "evidence": {
    "log_files": [
      "D:/Health/tests/runs/20260508-143000-codex-loop/logs/04-audit-auth.combined.log"
    ],
    "artifacts": [],
    "screenshots": []
  },
  "suspected_files": [],
  "constraints": [
    "只修复此 issue",
    "不要顺手重构无关代码",
    "不要删除测试断言来通过测试",
    "不要修改 node_modules、target、dist 等生成物",
    "HealthShow 和 HealthData 是独立 git 仓库，分别检查 git 状态"
  ],
  "verification_commands": [
    "cd D:/Health/HealthShow && npm run audit:auth"
  ],
  "fallback_commands": [
    "cd D:/Health/HealthShow && npm run audit:e2e",
    "cd D:/Health/HealthShow && npm run build"
  ],
  "status": "open"
}
```

### 11.2 Markdown 模板

```markdown
# <issue-id>: <title>

## 背景

本 issue 来自 run `<run-id>` 的测试闭环演练。

## 失败命令

```powershell
<failing_command>
```

## 期望结果

<expected>

## 实际结果

<actual>

## 证据

- 日志：...
- 产物：...
- 截图：...

## 疑似范围

- 项目：HealthShow / HealthData / Environment / TestScript
- 文件：...

## Codex 修复要求

1. 只修复这个 issue。
2. 做最小修改。
3. 不要顺手重构。
4. 不要删除或弱化测试断言。
5. 修改前后分别记录 git status。
6. 修改后运行验证命令。

## 验证命令

```powershell
<verification_command>
```

## 交付格式

Codex 最终回复必须包含：

- 根因
- 修改文件
- 关键修改点
- 验证命令和结果
- 未解决风险
```

---

## 12. Codex 修复流程

当 issue 包生成后，Codex 按以下流程执行。

### 12.1 读取任务包

读取：

```text
D:/Health/tests/runs/<run-id>/issues/<issue-id>.json
D:/Health/tests/runs/<run-id>/issues/<issue-id>.md
```

### 12.2 确认项目边界

如果 `project = HealthShow`：

```powershell
git -C D:/Health/HealthShow status --short
```

如果 `project = HealthData`：

```powershell
git -C D:/Health/HealthData status --short
```

### 12.3 定位根因

Codex 应优先读取：

1. 失败日志。
2. 失败测试脚本。
3. 相关源码。
4. 相关配置。
5. 最近一次通过的产物，如果存在。

不要先猜测并改代码。

### 12.4 做最小修复

修复原则：

- 优先修真实 bug。
- 如果是测试脚本问题，只改测试脚本。
- 如果是环境问题，不改代码，只输出 blocker。
- 如果是数据源口径问题，先修测试执行参数或文档，不要改业务默认行为。
- 不做无关重构。

### 12.5 验证

至少运行 issue 包中的 `verification_commands`。

如果修改前端核心逻辑，额外考虑：

```powershell
cd D:/Health/HealthShow
npm run audit:structure
npm run build
```

如果修改后端核心逻辑，额外考虑：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
```

---

## 13. Summary 输出格式

每次 run 必须生成：

```text
D:/Health/tests/runs/<run-id>/summary.md
D:/Health/tests/runs/<run-id>/summary.json
```

### 13.1 summary.json 建议结构

```json
{
  "run_id": "20260508-143000-codex-loop",
  "started_at": "2026-05-08T14:30:00+08:00",
  "finished_at": "2026-05-08T15:10:00+08:00",
  "status": "passed|failed|blocked|fixed",
  "environment": {
    "frontend_url": "http://localhost:9528/",
    "backend_url": "http://localhost:8080/health",
    "runner": "Windows PowerShell",
    "java_home": "C:/Program Files/Java/jdk-17"
  },
  "git_status": {
    "HealthShow_before": [],
    "HealthData_before": [],
    "HealthShow_after": [],
    "HealthData_after": []
  },
  "commands": [
    {
      "name": "backend-regression",
      "command": "python D:/Health/HealthData/scripts/run_backend_regression.py",
      "status": "passed|failed|skipped|blocked",
      "exit_code": 0,
      "log": "D:/Health/tests/runs/<run-id>/logs/02-backend-regression.combined.log"
    }
  ],
  "issues": [
    {
      "issue_id": "healthshow-auth-20260508-001",
      "status": "open|fixed|blocked",
      "path": "D:/Health/tests/runs/<run-id>/issues/healthshow-auth-20260508-001.json"
    }
  ],
  "next_actions": []
}
```

### 13.2 summary.md 建议结构

```markdown
# Health 测试闭环演练总结：<run-id>

## 结论

status: passed / failed / blocked / fixed

## 本次角色分工

- PowerShell: runner
- Codex: 修复执行
- Claude Code: 测试架构 / review
- Hermes: 归档模型
- OpenClaw: 未作为主 runner

## 环境

- 前端：...
- 后端：...
- 数据源：old/new/未验证

## Git 状态

### HealthShow before

```text
...
```

### HealthData before

```text
...
```

## 命令结果

| 命令 | 状态 | 日志 |
| --- | --- | --- |
| backend-regression | passed | logs/... |
| audit-api | failed | logs/... |

## 失败分诊

...

## 生成的 issue 包

...

## Codex 修复结果

- 根因：...
- 修改文件：...
- 验证：...

## 残余风险

...

## 下一步建议

...
```

---

## 14. latest-run 和 open-issues

### 14.1 latest-run.json

路径：

```text
D:/Health/tests/latest-run.json
```

内容：

```json
{
  "run_id": "20260508-143000-codex-loop",
  "summary": "D:/Health/tests/runs/20260508-143000-codex-loop/summary.json",
  "status": "passed|failed|blocked|fixed",
  "updated_at": "2026-05-08T15:10:00+08:00"
}
```

### 14.2 open-issues.json

路径：

```text
D:/Health/tests/open-issues.json
```

如果没有 open issue：

```json
{
  "updated_at": "2026-05-08T15:10:00+08:00",
  "issues": []
}
```

如果有 open issue：

```json
{
  "updated_at": "2026-05-08T15:10:00+08:00",
  "issues": [
    {
      "issue_id": "healthshow-auth-20260508-001",
      "project": "HealthShow",
      "severity": "high",
      "title": "backend restart 后前端会话状态异常",
      "path": "D:/Health/tests/runs/20260508-143000-codex-loop/issues/healthshow-auth-20260508-001.json",
      "status": "open"
    }
  ]
}
```

---

## 15. OpenClaw 本次处理方式

本次不让 OpenClaw 作为主测试 runner。

原因：

- 现有文档记录当前本机 `openclaw --version` 和 `openclaw --help` 可用。
- 但 `openclaw status`、`mcp`、`agent`、`channels` 等子命令曾出现超时。
- Health 测试强依赖 Windows PowerShell、SQL Server、Redis、Playwright、模拟器。
- 当前最稳执行器仍是 Windows PowerShell。

本次最多记录 OpenClaw 状态，不阻塞主闭环：

```powershell
openclaw --version
openclaw --help
```

不要因为 OpenClaw 状态异常而停止 Health 标准测试闭环。

后续 OpenClaw 可作为以下实验：

1. issue 包转发入口。
2. Codex / Claude Code 多 Agent 对比入口。
3. 测试 summary 通知入口。
4. 夜间测试结果分发入口。

---

## 16. Hermes 本次处理方式

Hermes 当前更适合作为编排和归档模型，而不是直接长时间控制 Windows 测试命令。

本次可以按 Hermes 风格生成：

- run-id
- logs
- summary
- issue 包
- open issue 索引

但不要强依赖 Hermes gateway 或消息平台。

如果需要核对 Hermes：

```powershell
wsl -e bash -lc '/root/.local/bin/hermes --version'
wsl -e bash -lc '/root/.local/bin/hermes --help'
```

Hermes 不可用不应阻塞本次 PowerShell runner 测试闭环。

---

## 17. 推荐执行顺序

Codex 按以下顺序执行：

1. 阅读本文件。
2. 阅读 `AGENTS.md`、`HEALTH_TEST_METHOD.md`、`HEALTH_TEST_TOOLCHAIN_GUIDE.md`、`HERMES_CODEX_HEALTH_EXECUTION_PLAN.md`。
3. 创建 run-id。
4. 创建产物目录。
5. 记录 PowerShell / node / npm / python / maven 版本。
6. 记录端口和 HTTP 预检结果。
7. 记录 `HealthShow` / `HealthData` git status。
8. 执行后端回归。
9. 执行 `audit:api`。
10. 执行 `audit:auth`。
11. 执行 `audit:e2e`。
12. 执行 `audit:pipeline`。
13. 汇总命令结果。
14. 如果全部通过，生成 passed summary。
15. 如果有失败，选择最高优先级的一个失败生成 issue 包。
16. 如果 issue 是可修复代码/测试问题，做最小修复。
17. 运行 issue 对应验证命令。
18. 更新 summary、latest-run、open-issues。
19. 输出最终交付说明。

---

## 18. 优先级规则

如果多个测试失败，只处理一个最高优先级 issue。

优先级从高到低：

1. 环境阻塞：服务无法启动、数据库/Redis 不可用。
2. 鉴权失败：`audit:auth`。
3. 写入和 pipeline 失败：`audit:pipeline`。
4. API smoke 大面积失败：`audit:api`。
5. E2E 页面基础失败：`audit:e2e`。
6. 构建失败：`npm run build`。
7. 结构门禁失败：`audit:structure`。
8. 数据密度专项失败：`audit:data`。

说明：

- 环境阻塞不一定需要修代码。
- 鉴权和 pipeline 影响核心闭环，优先级高。
- API 大面积失败通常先查后端服务和数据源。
- E2E 单页失败需要结合截图和接口结果判断。

---

## 19. Codex 最终回复要求

Codex 执行完成后，最终回复必须包含：

```text
1. run_id
2. 总体状态：passed / failed / blocked / fixed
3. 执行了哪些命令
4. 每个命令结果
5. 产物目录
6. 如果失败：生成了哪个 issue 包
7. 如果修复：根因、修改文件、验证结果
8. 未解决风险
9. 建议下一步
```

不要只说“已完成”。必须给出可追踪路径。

---

## 20. 可直接交给 Codex 的启动提示词

下面这段可以直接复制给 Health 下的新 Codex：

```text
你现在在 D:/Health 工作区执行一次 Health 测试闭环演练。

请严格按照 D:/Health/HEALTH_CODEX_TEST_CLOSED_LOOP_RUNBOOK.md 执行。

核心定位：
D:/Health = 测试闭环主战场
Codex = 主力修复执行器
Claude Code = 测试架构师 / Reviewer
PowerShell = 稳定 runner
Hermes = 编排和归档
OpenClaw = 后续实验性编排层

要求：
1. 先读 AGENTS.md、HEALTH_TEST_METHOD.md、HEALTH_TEST_TOOLCHAIN_GUIDE.md、HERMES_CODEX_HEALTH_EXECUTION_PLAN.md。
2. 不要在 D:/Health 根目录做 git 操作。
3. HealthShow 和 HealthData 是两个独立 git 仓库，分别检查状态。
4. 创建 run-id 和 D:/Health/tests/runs/<run-id>/ 产物目录。
5. 使用 Windows PowerShell 作为 runner。
6. 执行标准测试集：后端 regression、audit:api、audit:auth、audit:e2e、audit:pipeline。
7. 归档日志到 logs/。
8. 生成 summary.md 和 summary.json。
9. 如果有失败，只选最高优先级的一个失败生成 issue 包。
10. 如果问题明确且可修复，做最小修复并运行对应验证命令。
11. 不要跳过测试，不要删除断言，不要顺手重构。
12. 最终回复 run_id、状态、命令结果、产物路径、修复结果和残余风险。
```

---

## 21. 本次演练后的评估问题

执行完成后，由 Claude Code / 人工 reviewer 评估：

1. PowerShell runner 是否稳定？
2. 产物目录是否足够清晰？
3. summary 是否能让人快速判断成败？
4. issue 包是否足够让 Codex 定点修复？
5. Codex 是否做到了最小修改？
6. 验证命令是否足够证明修复有效？
7. 有没有必要引入 Hermes 自动生成 run-id 和 issue 包？
8. OpenClaw 是否有必要参与任务转发或多 Agent 对比？
9. 哪个环节最耗人工？
10. 下一轮应该扩大到 `audit:nightly` 还是先打磨 issue 包质量？

---

## 22. 推荐下一轮方向

如果本次闭环成功，下一轮建议三选一：

### 方向 A：增强 issue 包自动化

让脚本自动从日志中提取：

- failing command
- exit code
- error snippet
- artifact paths
- suspected project

### 方向 B：增加测试缺口专项

优先目标：

```text
D:/Health/HealthData/src/main/java/com/xzkj/health/ai/AiSqlGuard.java
D:/Health/HealthData/src/test/java/com/xzkj/health/ai/AiSqlGuardTest.java
```

目标：补充 SQL 安全边界测试。

### 方向 C：引入 OpenClaw 对比实验

同一个 issue：

```text
Codex 单独修
Claude Code 单独修
OpenClaw 编排 Codex + Claude review 修
```

比较：

- 成功率
- 修改范围
- 测试通过率
- 回归风险
- 人工介入次数
- 日志可读性

---

## 23. 最终提醒

本 runbook 的重点不是“多工具炫技”，而是建立可靠闭环：

```text
测得出来
归档清楚
失败可分诊
任务可交接
Codex 能定点修
修完能验证
结果能复盘
```

只有这个闭环跑通后，Hermes 和 OpenClaw 才有接入价值。
