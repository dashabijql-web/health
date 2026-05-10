# HealthShow OpenClaw 测试入口说明（2026-05-10）

本文档定义 OpenClaw 何时可以作为 HealthShow 测试入口，以及它应该触发什么命令、读取什么结果。OpenClaw 不直接判断 Health 业务正确性，只负责接收外部指令、调用稳定 runner、推送 summary。

## 1. 当前可用入口

稳定入口脚本：

```text
D:/Health/HealthShow/tests/run-full-stack-local.ps1
```

推荐 OpenClaw 只调用这个脚本，不直接拼接 `npm run audit:*` 长命令。

OpenClaw 本地 agent 触发 Windows runner 前，需要允许 PowerShell 执行入口：

```powershell
openclaw approvals allowlist add "powershell.exe"
openclaw approvals allowlist add "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"
```

这是 OpenClaw 自己的 exec allowlist，不是 Health 业务权限。

## 2. 标准触发命令

默认完整测试走旧库：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\HealthShow\tests\run-full-stack-local.ps1"
```

旧库完整测试：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\HealthShow\tests\run-full-stack-local.ps1" -DataSource old
```

新库完整测试：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\HealthShow\tests\run-full-stack-local.ps1" -DataSource new
```

旧库 + 新库完整测试：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\HealthShow\tests\run-full-stack-local.ps1" -DataSource both
```

临时跳过后端单测的快速全栈测试：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\HealthShow\tests\run-full-stack-local.ps1" -SkipBackendTests
```

如果由 OpenClaw agent 触发，推荐让它后台启动 runner 并立即返回，不要让 OpenClaw agent 等完整测试结束：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:\Health\tools\start-health-runner-hidden.ps1" -DataSource both
```

该脚本只负责隐藏启动 `run-full-stack-local.ps1` 并打印 `RUN_STARTED pid=<pid>`。如果必须直接把内层 PowerShell 嵌进 `openclaw agent --message`，优先转成 `-EncodedCommand` 再交给 OpenClaw。不要在多层命令文本里直接写 `$_.Id`，它可能被外层 PowerShell 提前展开成 `.Id`，导致 OpenClaw exec 启动失败。2026-05-10 12:29 已用 `-EncodedCommand` 成功触发双库 run `20260510-122940-full-stack-local-both`。

之后由监控方读取 `latest-full-stack-local.json`。

runner 内部启动后端、前端、Redis 和模拟器时，必须继续走 `Start-NoWindowProcess`；`audit:auth` 重启后端时必须继续走 `Win32_ProcessStartup(ShowWindow=0)` + `Win32_Process.Create` detached 启动，不能让 Maven 子进程继承父 PowerShell stdout/stderr 管道。否则会出现无可见窗口但 `audit:auth` 卡在 `starting backend process` 的假死。现场看到 Java/Maven 控制台窗口时，runner 需要先检测并停止已有的可见 `HealthData` 后端窗口，再静默重启；不要因为 `8080` 已监听就复用可见窗口。遇到这类问题先跑 `npm run test:fast` 和 `npm run audit:auth`，不要继续开新轮次堆进程。

如果 `openclaw agent` 返回模型网络错误，但 `openclaw gateway probe` 显示 RPC 正常，不要先把它当作 Health runner 失败。2026-05-10 已确认一个本机根因：`C:\Users\j\.openclaw\gateway.cmd` 曾硬编码 `127.0.0.1:7890` 代理，而当前可用的 Codex 中转基址是 `https://api.psydo.top`；映射到 OpenClaw 的 OpenAI-compatible provider 时应写成 `https://api.psydo.top/v1` + `openai-completions`。修复步骤：

1. 优先运行 `D:/Health/tools/sync-openclaw-codex-api.ps1 -RestartGateway`，从 Codex 的 `auth.json` / `config.toml` 同步 OpenClaw 的 `psydo` key、baseUrl 和默认模型，并隐藏重启 gateway。
2. 该同步脚本默认会禁用 `channels.telegram.enabled`。Health 本地自动化只需要 loopback gateway + agent/exec；Telegram sidecar 如果走不可用代理，会让 gateway WebSocket 握手超时，表现为 `gateway closed`、`probe timeout` 或 agent 退回 embedded。确实需要 Telegram 通知时，手动传 `-KeepTelegramChannel` 并单独验证 Telegram 网络。
3. 确认 `openclaw models status --json` 中默认模型为 `psydo/gpt-5.5`，`psydo` key 来自当前可用中转配置。
4. 确认 `C:\Users\j\.openclaw\gateway.cmd` 不再设置 `HTTP_PROXY` / `HTTPS_PROXY` / `ALL_PROXY` 到 `127.0.0.1:7890`；只保留 `NO_PROXY=localhost,127.0.0.1,::1`。
5. 重启或隐藏启动 gateway 后，先等 `C:\Users\j\.openclaw\gateway-restart.out.log` 出现 `ready`，再跑 `openclaw gateway probe`，最后跑 `openclaw agent --agent main --thinking low --message "..."`。本机 gateway 启动约 18 秒，启动窗口内 probe 可能因 3 秒预算误报 timeout；最终两者都必须通过，且 agent 输出里不能出现 `falling back to embedded`。
6. `gpt-5.5` 不支持 `minimal` thinking；OpenClaw 调用 Health 任务时固定传 `--thinking low`。

如果 gateway 服务进程仍复用旧环境，可临时停止服务后用当前 PowerShell 环境隐藏启动 gateway；必须记录这是本机服务环境修复，不是 Health 测试失败。只有 OpenClaw agent 仍不可用时，才可由 Windows PowerShell 直接执行同一条后台启动命令，并在报告里记录“OpenClaw gateway 正常、agent LLM 网络失败、runner 已本地降级启动”。

## 3. OpenClaw 只需读取的文件

runner 每轮会创建：

```text
D:/Health/HealthShow/tests/runs/<run-id>/
  full-stack-local-summary.json
  summary.md
  hermes-archive.md
  full-stack-local.log
  *.combined.log
```

OpenClaw 应优先读取：

```text
D:/Health/HealthShow/tests/runs/latest-full-stack-local.json
D:/Health/HealthShow/tests/runs/<run-id>/full-stack-local-summary.json
D:/Health/HealthShow/tests/runs/<run-id>/summary.md
D:/Health/HealthShow/tests/runs/<run-id>/hermes-archive.md
```

`latest-full-stack-local.json` 是最近一次 full-stack-local run 指针。不要默认读取所有 `*.out.log` / `*.err.log`，除非 summary 显示失败且需要生成 issue 包。

## 4. 结果解释

OpenClaw 只做状态转发：

| runner status | OpenClaw 动作 |
| --- | --- |
| `passed` | 推送通过摘要和 run-id |
| `failed` | 推送失败摘要，并交给 Hermes 生成单 issue 或交给 Codex 判断 |
| `blocked` | 推送环境阻塞信息，不触发代码修复 |
| `skipped` | 推送跳过原因，不触发代码修复 |

OpenClaw 不应自行把 `failed` 改成 `passed`，也不应自行修改代码。

## 5. old/new 行为由 runner 负责

OpenClaw 不需要理解模拟器细节。默认不传 `-DataSource` 时走 `old`：

- `old`：runner 会按旧库阶段运行，并按需启动模拟器。
- `new`：runner 会按新库阶段运行，并在默认情况下停止模拟器。
- `both`：runner 会先跑 old，再停模拟器跑 new。

新库空业务数据时，`audit:write` 可以把依赖绑定设备、未处理 warning 或 AI 报告员工候选的业务写探针记为 `skipped`；登录和配置写入仍必须执行，旧库不允许同类跳过。

如果现场明确要求不启动或不停止模拟器，可以使用：

```powershell
-NoStartSimulatorForOld
-NoStopSimulatorForNew
```

这些参数只适合临时排障，不适合作为默认 OpenClaw 入口。

## 6. 推荐 OpenClaw 消息模板

触发测试：

```json
{
  "task": "healthshow-full-stack-test",
  "dataSource": "old",
  "command": "powershell.exe -NoProfile -ExecutionPolicy Bypass -File \"D:\\Health\\HealthShow\\tests\\run-full-stack-local.ps1\"",
  "reportFiles": [
    "full-stack-local-summary.json",
    "summary.md"
  ],
  "onFailure": "send-to-codex"
}
```

失败转给 Codex：

```json
{
  "task": "healthshow-fix-single-failure",
  "source": "openclaw",
  "runSummary": "D:/Health/HealthShow/tests/runs/<run-id>/full-stack-local-summary.json",
  "humanRule": "一次只修一个最高优先级失败，不要回滚用户改动"
}
```

## 7. 何时可以先找 OpenClaw

满足以下条件时，可以先找 OpenClaw：

- 你只想触发 `old`、`new` 或 `both` 的标准测试。
- 默认标准测试就是旧库 `old`。
- 你只需要 summary 和通知。
- 你接受失败后再交给 Codex 判断和修复。
- 不涉及临时改测试口径、真实手表现场判断、数据库结构调整。

仍应先找 Codex 的场景：

- 测试流程本身要改。
- runner 或 summary 失败。
- 双库、pipeline、真实手表、新库空态结论有争议。
- 需要修复代码或测试脚本。
- 发布前最终技术验收。

## 8. 当前结论

现在可以让 OpenClaw 作为标准测试入口，但它的职责边界必须很窄：

```text
OpenClaw 接收指令
  -> 调用 run-full-stack-local.ps1
  -> 读取 summary.md / full-stack-local-summary.json
  -> 推送结果
  -> 失败时交给 Codex
```

不要让 OpenClaw 直接替代 Codex 做失败根因判断，也不要让它直接修改 HealthShow 或 HealthData。
