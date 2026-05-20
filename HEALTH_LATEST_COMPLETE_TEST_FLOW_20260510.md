# Health 最新完整测试流程（2026-05-10）

适用范围：`D:/Health` 全栈项目，包括 `HealthShow` 前端、`HealthData` 后端、SQL Server、Redis、TCP 9000、模拟器、旧库/新库切换和 OpenClaw/Hermes/Codex 协作。

本文档是当前测试执行的第一操作说明。旧文档继续保留为背景材料，但执行时以当前代码、`D:/Health/AGENTS.md`、本文档和 runner 实现为准。

---

## 0. 当前结论

当前最稳的执行模型是：

```text
OpenClaw 作为外部入口和通知层
  -> Windows PowerShell runner 执行真实命令
  -> Hermes 读取 summary、归档、生成单 issue 包
  -> Codex 判断根因、修 runner/测试/代码并复跑
```

关键结论：

- 默认测试数据源是旧库 `old`，即 SQL Server 数据库 `health`。
- 当前稳定主入口已切到 `/home/j/code/health/HealthShow/tests/run-full-stack-local.py`。
- OpenClaw 可以先启动标准测试，但失败后的根因判断和修复必须交给 Codex。
- Hermes 需要参与，但更适合在 WSL2 中做报告整理、issue 拆分和归档，不适合直接长期控制 Windows 本地服务。
- 真正执行命令的是 Windows PowerShell runner，不是某个 agent 的临时命令流。
- 新库 `new` 当前可能是空业务库。新库缺员工、设备、warning 探针目标时，不应自动按产品 bug 处理，应按 `skipped`、`blocked` 或空态验收处理。
- 2026-05-10 已修复本机 OpenClaw agent 的中转配置问题：默认模型切到 `psydo/gpt-5.5`，key 同步到 Codex 当前可用中转配置；Codex 配置基址为 `https://api.psydo.top`，OpenClaw 的等价 OpenAI-compatible 配置为 `https://api.psydo.top/v1` + `openai-completions`。`C:\Users\j\.openclaw\gateway.cmd` 不再把 LLM 请求代理到不可用的 `127.0.0.1:7890`。调用 Health 任务时使用 `openclaw agent --agent main --thinking low ...`。
- 如果怀疑 OpenClaw 中转配置漂移，直接核对最新 `openclaw.json`、agent `models.json`、gateway probe 与实际 agent 响应；旧 Windows 同步脚本已退役。确实需要 Telegram 通知时，单独验证 Telegram 网络。重启 gateway 后先等 `C:\Users\j\.openclaw\gateway-restart.out.log` 出现 `ready`；本机实测启动约 18 秒，启动窗口内 probe 可能因 3 秒预算误报 timeout。
- OpenClaw 修复后的最低验收是两条都通过：`openclaw gateway probe` 显示 `RPC: ok`；`openclaw agent --agent main --thinking low --message "只回复一行：OPENCLAW_GATEWAY_LLM_OK"` 返回目标文本，且输出中没有 `falling back to embedded`。
- 2026-05-10 13:24 再验收：同步并重启 gateway 后配置仍与 Codex 当前中转一致，gateway 刚 `ready` 时 3 秒 `probe` 可能 timeout；等待 `plugins embedded acpx runtime backend ready` 后，agent LLM 调用返回 `OPENCLAW_GATEWAY_LLM_OK_1322`，随后 `openclaw gateway probe` 返回 `Connect: ok` / `RPC: ok`。
- 2026-05-10 23:58/2026-05-11 00:03 再次排查 dashboard `disconnected (1006)`：本机 `18789` 端口和 `openclaw gateway probe` 均正常，`openclaw agent --agent main --thinking low` 也能返回目标文本。实际操作时不要打开裸 `http://127.0.0.1:18789/` 后手填空令牌；应执行 `openclaw dashboard`，使用它生成并复制的带 `#token=...` 的 dashboard URL。若仍怀疑网关漂移，直接复核 gateway 配置与 probe，不再依赖旧 Windows 同步脚本。
- 2026-05-11 夜间自主进化启用本地监督脚本 `/home/j/code/health/tools/start-openclaw-evolution-supervisor.py`，目标运行到 `2026-05-11 08:30 +08:00`。该脚本只负责协调：先确认 OpenClaw gateway 正常，再等待已有 `run-full-stack-local.py` 空闲，然后调用 OpenClaw 执行一个 bounded round，最后通过 `/home/j/code/health/tools/start-health-runner.py --data-source both` 跑双库 runner。日志写入 `D:/Health/tests/runs/openclaw-night-supervisor-*`。
- 2026-05-11 夜间高风险保护点已推送到 GitHub 独立分支，后续自动改动基于该分支继续：HealthShow `origin/autonomous-evolution-20260510-night` commit `946e39d`，HealthData `origin/autonomous-evolution-20260510-night` commit `554f3bb`。
- 2026-05-11 00:33 runner 高风险修复前已推送 HealthShow checkpoint `checkpoint/health-evolution-20260511-003326-runner-simulator`，commit `946e39d`。根因：旧库 `test-full-old` 内的 `audit:pipeline` 选中 `359456780000001` 时，1000 设备模拟器仍在并发发送同一 IMEI，导致 TCP 探针连接被抢占、SQL 只出现模拟器随机值。修复：runner 在 `test-perf-old` 后、`test-full-old` 前停止模拟器，reason=`old-full-pipeline-exclusive-tcp`，让 pipeline/warning pipeline 独占 TCP 探针；`npm run test:fast` 通过 `tests/auth-backend-start-hidden.mjs` 守护该顺序。
- 2026-05-11 00:55/01:17 夜间 supervisor 发现 OpenClaw 空转风险：prompt 文件完整，但传入 OpenClaw session 的实际消息先只剩第一行 `You are the OpenClaw worker...`；随后单行 message 又因旧 Windows 参数边界触发 `too many arguments for 'agent'`。当前已切到 `/home/j/code/health/tools/start-openclaw-evolution-supervisor.py`，由 WSL Python 入口维护 round 编号、stdout 校验和 fallback 产物。
- 2026-05-11 01:23 再收紧 supervisor：OpenClaw Round 14 实际已执行并输出 `DONE_ROUND_14`，但 marker 位于第二行，旧校验要求 stdout 第一行命中导致误写 fallback。现改为任意行行首命中 `DONE_ROUND_<n>` 或 `OPENCLAW_NO_TOOLS` 即可接受，同时继续拒绝空 stdout 和 CLI `error:` stderr。
- 2026-05-11 02:25 再收紧夜间自主进化：Rounds 16-18 连续选择 no-op evidence round，说明在基线稳定时 OpenClaw 可能退化成只记录“不改动”。当前 supervisor 入口会统计最近 no-op streak；当 streak >= 1 时，prompt 明确禁止 no-op，要求改动 `D:/Health/tests/runs` 之外的 durable 文件（代码、测试、runner/tooling 或 md 文档）。若 OpenClaw 仍输出重复 no-op，supervisor 会写 fallback、拒收本轮并跳过后续 runner，避免把空转当成有效进化轮次。
- 2026-05-11 05:20 本机复核 OpenClaw `disconnected (1006)`：`openclaw gateway probe` 返回 `Connect: ok / RPC: ok`，`openclaw agent --agent main --thinking low --timeout 120 --message "只回复一行：OPENCLAW_HEALTH_PROBE_OK_0511"` 返回目标文本。若 dashboard 仍显示 1006，优先用 `openclaw dashboard --no-open` 重新获取带 `#token=...` 的 URL，不要打开裸 `http://127.0.0.1:18789/` 手填空令牌；只有 probe 或 agent 探针失败时才继续排查 gateway 配置。
- 2026-05-11 05:25 本轮长跑目标延长到 `2026-05-12 08:30 +08:00`。当前 `2026-05-11 04:23` 启动的 supervisor 仍按旧目标 `2026-05-11 08:30 +08:00` 运行；后续必须用 `/home/j/code/health/tools/start-openclaw-evolution-supervisor.py --stop-at 2026-05-12T08:30:00+08:00` 接力。接力前先等待 `run-full-stack-local.py` 空闲，避免中断正在执行的双库 runner。
- 2026-05-11 05:28 supervisor 增加 GitHub checkpoint 守护：每轮进入 OpenClaw 前会对 `D:/Health/HealthShow` 与 `D:/Health/HealthData` 的 durable 改动执行 `git add`、`git commit`、`git push -u origin <branch>`；自动排除 `tests/runs`、`tests/**/artifacts`、日志、`node_modules` 和 `dist`。如果 checkpoint 或 push 失败，本轮跳过 OpenClaw 编辑，避免在未推送状态上继续高风险改动；停止前还会再做一次 final checkpoint。
- 2026-05-11 06:55 supervisor/runner 单实例匹配再修：此前只按命令行包含旧 runner 路径判断活跃任务，导致含有该路径变量的接力脚本也被误判为 runner，并让 supervisor 等满 `RUNNER_WAIT_TIMEOUT minutes=90`。当前 WSL supervisor 与 detached runner 已改为只认真实运行中的 Python runner / tmux session，避免监督脚本被误判。
- 2026-05-11 07:12 supervisor 增加 OpenClaw stdout 卡住自愈：OpenClaw 有时已经在 gateway 日志输出 `DONE_ROUND_<n>`，但 CLI stdout 文件为空且 agent 进程不退出。当前 WSL supervisor 会记录 `EARLY_COMPLETION_MARKER` 并在超时前退出卡住的 agent。marker 轮号不匹配仍按无效轮次处理。
- 2026-05-11 08:45 supervisor 补上本该更早加入的流程自进化硬门禁：`/home/j/code/health/tools/request-openclaw-supervisor-stop.py` 会写入 `D:/Health/tests/runs/openclaw-night-supervisor.stop`，supervisor 在当前 full-stack runner 空闲后立即记录 `STOP_AFTER_CURRENT_REQUESTED` 和 `ROUND_STOP_AFTER_CURRENT` 并停止，不再进入下一轮 OpenClaw agent；OpenClaw final reply 现在必须包含 `ROUND_RESULT_JSON`，其中必须写 `process_retrospective`、`flow_guard_added` 和下一轮建议，否则本轮不被接受；新增 `HealthShow/tests/openclaw-supervisor-contract.mjs` 与 `npm run test:openclaw-supervisor-contract`，并纳入 `test:fast`，防止停止开关、结构化 round 结果和 final stop_reason 再次漂移。

最新已确认基线：

```text
run_id: 20260510-021121-full-stack-local-old
status: passed
data_source: old
summary: D:/Health/HealthShow/tests/runs/20260510-021121-full-stack-local-old/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260510-021121-full-stack-local-old/full-stack-local-summary.json
finished_at: 2026-05-10 02:18:46 +08:00
```

该 run 已通过：

- `backend-maven-test`
- `backend-regression`
- `test-fast`
- `test-frontend`
- `test-quality`
- `test-integration-old`
- `test-perf-old`
- `test-full-old`

最新 OpenClaw 触发旧库 run：

```text
run_id: 20260510-105044-full-stack-local-old
status: passed
data_source: old
trigger: OpenClaw agent -> exec -> hidden Windows PowerShell runner
summary: D:/Health/HealthShow/tests/runs/20260510-105044-full-stack-local-old/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260510-105044-full-stack-local-old/full-stack-local-summary.json
hermes_archive: D:/Health/HealthShow/tests/runs/20260510-105044-full-stack-local-old/hermes-archive.md
finished_at: 2026-05-10 10:57:59 +08:00
```

最新 OpenClaw 触发新库 run：

```text
run_id: 20260510-111131-full-stack-local-new
status: skipped
status_reason: allowed skipped steps: 1
data_source: new
trigger: OpenClaw agent -> exec -> hidden Windows PowerShell runner
summary: D:/Health/HealthShow/tests/runs/20260510-111131-full-stack-local-new/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260510-111131-full-stack-local-new/full-stack-local-summary.json
hermes_archive: D:/Health/HealthShow/tests/runs/20260510-111131-full-stack-local-new/hermes-archive.md
data_source_facts: health_new department=0, employee=0, device=0, device_user=0, realtime_data=0, user_online_status=0, simulatorDeviceRows=0
finished_at: 2026-05-10 11:16:24 +08:00
```

最新 OpenClaw 触发双库 run：

```text
run_id: 20260510-141038-full-stack-local-both
status: skipped
status_reason: allowed skipped steps: 1
data_source: both
trigger: OpenClaw agent -> exec -> hidden Windows PowerShell runner
summary: D:/Health/HealthShow/tests/runs/20260510-141038-full-stack-local-both/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260510-141038-full-stack-local-both/full-stack-local-summary.json
hermes_archive: D:/Health/HealthShow/tests/runs/20260510-141038-full-stack-local-both/hermes-archive.md
old_data_source_facts: health department=20, employee=1000, device=1000, device_user=1000, simulatorDeviceRows=1000
new_data_source_facts: health_new department=0, employee=0, device=0, device_user=0, simulatorDeviceRows=0
simulator_evidence: started pid=16708 in old phase, stopped pid=16708 before new phase, final simulatorRunning=false
finished_at: 2026-05-10 14:21:48 +08:00
hermes_triage: Hermes WSL /root/.local/bin/hermes -z read the latest archive and confirmed failed=0, skipped=1 allowed, no Health code repair required
flow_improvement: /home/j/code/health/tools/start-health-runner.py prevents duplicate full-stack runner starts; --dry-run remains JSON-only and is guarded by npm run test:fast
```

最新夜间监督触发双库 run：

```text
run_id: 20260510-234043-full-stack-local-both
status: skipped
status_reason: allowed skipped steps: 1
data_source: both
summary: D:/Health/HealthShow/tests/runs/20260510-234043-full-stack-local-both/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260510-234043-full-stack-local-both/full-stack-local-summary.json
failed_steps: none
old_data_source_facts: health department=20, employee=1000, device=1000, device_user=1000, simulatorDeviceRows=1000
new_data_source_facts: health_new department=0, employee=0, device=0, device_user=0, simulatorDeviceRows=0
finished_at: 2026-05-10 23:52:07 +08:00
```

---

## 1. 文件和入口

### 1.1 第一阅读顺序

新开测试任务时按这个顺序读：

1. `D:/Health/AGENTS.md`
2. `D:/Health/HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md`
3. `D:/Health/HealthShow/HEALTHSHOW_OPENCLAW_TEST_ENTRY_20260510.md`
4. `D:/Health/HealthShow/HEALTHSHOW_TEST_FLOW_CODEX_REFINED_20260510.md`
5. `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
6. 当前代码与 runner：
   - `/home/j/code/health/HealthShow/tests/run-full-stack-local.py`
   - `D:/Health/HealthShow/scripts/health-test-runner.mjs`
   - `D:/Health/HealthShow/package.json`

如果文档互相冲突：

```text
当前代码和 runner > AGENTS.md > 本文档 > 2026-05-10 以前旧文档
```

### 1.2 当前稳定 runner

```text
/home/j/code/health/HealthShow/tests/run-full-stack-local.py
```

这个 runner 当前负责：

- 检查或启动 SQL Server `127.0.0.1:58135`
- 检查或启动 Redis `127.0.0.1:6379`
- 检查或启动后端 HTTP `127.0.0.1:8080/health`
- 检查或启动后端 TCP `127.0.0.1:9000`
- 检查或启动前端 Vite `127.0.0.1:9528`
- 旧库阶段按需启动 `watch_tcp_simulator_1000.py`
- 旧库阶段跑完 `test-perf-old` 后会在 `test-full-old` 前停止模拟器；原因是 `test-full-old` 内的 TCP pipeline 需要独占探针 IMEI，避免 1000 设备模拟器并发抢占同一连接。
- 新库阶段默认停止模拟器
- 后端、前端、Redis、模拟器等长驻进程必须通过 runner 的 `Start-NoWindowProcess` 启动，禁止回退到会弹出控制台的服务启动方式。
- 执行后端单测、后端 regression、前端分层测试
- 生成结构化 summary 和人读 summary
- summary 必须包含 `dataSourceFacts`，至少记录本轮数据源对应数据库、`department/employee/device/device_user/realtime_data/user_online_status` 行数，以及 `simulatorDeviceRows`。这用于及时发现新库混入模拟器 IMEI 的历史污染。

### 1.3 OpenClaw 入口文档

```text
D:/Health/HealthShow/HEALTHSHOW_OPENCLAW_TEST_ENTRY_20260510.md
```

OpenClaw 只应该调用稳定 runner，不应该自己拼长串 `npm run audit:*`。

---

## 2. 角色分工

| 角色 | 正确职责 | 不应承担的职责 |
| --- | --- | --- |
| Windows PowerShell runner | 启动服务、跑测试、归档日志、生成 summary | 根因判断、代码修复、业务结论仲裁 |
| OpenClaw | 接收外部指令、触发 runner、读取 latest summary、通知结果、需要时拆给多个 Codex | 直接长期手写测试命令、直接改代码、直接把 failed 改 passed |
| Hermes | 计划整理、summary 归档、失败摘要、单 issue 包生成、趋势报告 | 在 WSL2 中用 `localhost` 直接判断 Windows 服务、直接修代码、直接长期跑 Windows 测试 |
| Codex | 判断环境/测试/业务根因、修 runner、修测试、修代码、复跑关键命令、最终技术判断 | 长期无人值守调度、把多个根因混在一个修复任务中 |

推荐顺序：

```text
先找 OpenClaw 跑标准旧库测试
  -> 如果 passed，OpenClaw 通知即可
  -> 如果 failed/blocked/skipped，Hermes 整理单 issue
  -> Codex 接单 issue 定位修复
  -> 修完由 runner 复跑
```

仍应先找 Codex 的场景：

- 测试流程、runner 或判断口径要改。
- 双库切换、模拟器、真实手表、新库空态有争议。
- summary 结构异常或 runner 自身跑不下去。
- 失败需要改代码或改测试。
- 发布前最终技术验收。

---

## 3. 数据源规则

Health 有两个业务数据源：

| 数据源 | 数据库 | 用途 | 测试口径 |
| --- | --- | --- | --- |
| `old` | `health` | 模拟器、演示数据、旧库数据密度、主默认验收 | 核心组件必须非空 |
| `new` | `health_new` | 真实手表、空库上线、隔离验证 | 业务空态可能是预期 |

固定操作顺序：

```text
旧库测试：切 old -> 启动模拟器 -> 跑 old 数据密度/pipeline/perf/full
新库测试：切 new -> 停止模拟器 -> 跑 new 空态/真实手表/隔离验证
双库测试：先 old -> 再停模拟器 -> 再 new
```

每轮 summary 必须能回答：

- 本轮请求的数据源是 `old` 还是 `new`。
- `API_DATA_SOURCE` 是什么。
- `API_EXPECT_NON_EMPTY` 是 `1` 还是 `0`。
- 请求头 `X-Health-Data-Source` 是否正确。
- 响应头 `X-Health-Data-Source` 是否正确。
- Cookie `Health-Data-Source` 是否符合预期。
- SQL 实际库是 `health` 还是 `health_new`。
- Redis key 是 `health:buffer:old` 还是 `health:buffer:new`。
- 模拟器是否运行。
- 新库 `health_new` 是否混入模拟器 IMEI 行，summary 中看 `dataSourceFacts.new.simulatorDeviceRows`。

旧库规则：

- `audit:data:old` 不允许核心组件为空。
- 旧库没有 pipeline 探针目标，默认是失败。
- 旧库 warning pipeline 失败，默认是失败。

新库规则：

- 新库员工、部门、设备、业务流水为空可能是预期。
- 新库没有 warning 探针目标时，warning pipeline 可以 `skipped`，不得误报为产品代码失败。
- 新库缺少绑定设备、未处理 warning 或 AI 报告员工候选时，`audit:write` 的业务写探针应记为 `skipped`；登录和配置写入仍必须执行，旧库仍保持严格失败口径。
- 如果新库已经接入真实手表或种子数据，则报告必须注明“本轮 new 不是空库验收”。
- 2026-05-10 发现并清理过一条历史残留的 `health_new.device` 模拟器 IMEI：`359456780000001`。清理后最新 new run 的 `dataSourceFacts.new.simulatorDeviceRows=0`，`deviceRows=0`。

---

## 4. 标准命令

所有命令默认在 Windows PowerShell 执行。

### 4.1 默认完整测试：旧库

这是默认入口，也是 OpenClaw 的推荐默认命令：

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py
```

等价于：

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source old
```

### 4.2 新库完整测试

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source new
```

runner 会在新库阶段默认停止模拟器。

### 4.3 双库完整测试

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both
```

执行顺序：

```text
backend phase
frontend-common phase
old-data-source phase
new-data-source phase
```

### 4.4 临时跳过后端单测

只适合排障或快速复核，不作为发布验收：

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --skip-backend-tests
```

### 4.5 不启动或不停止模拟器

只适合临时排障：

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source old --no-start-simulator-for-old
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source new --no-stop-simulator-for-new
```

这两个参数不能作为 OpenClaw 默认入口。

---

## 5. runner 当前阶段细节

`run-full-stack-local.py` 当前参数：

```bash
--data-source old|new|both
--skip-backend-tests
--no-start-simulator-for-old
--no-stop-simulator-for-new
```
)
```

### 5.1 readiness

runner 启动测试前检查：

| 项 | 目标 |
| --- | --- |
| SQL Server | `127.0.0.1:58135` |
| Redis | `127.0.0.1:6379` |
| backend HTTP | `127.0.0.1:8080` |
| backend TCP | `127.0.0.1:9000` |
| frontend | `127.0.0.1:9528` |
| SQL password | `SQL_PASSWORD` 或从 `application.yml` 读取 |

任一必要项不可用，本轮状态必须是 `blocked`。

### 5.2 backend phase

默认执行：

```powershell
D:\apache-maven-3.8.1\bin\mvn.cmd -q test -f D:\Health\HealthData\pom.xml
python D:\Health\HealthData\scripts\run_backend_regression.py
```

只有显式传 `-SkipBackendTests` 才跳过。

### 5.3 frontend-common phase

执行：

```powershell
cd D:/Health/HealthShow
npm run test:fast
npm run test:frontend
npm run test:quality
```

含义：

- `test:fast`：纯 Node 和 warning 语义测试。
- `test:fast` 同时守护 WSL 启动约束：`tests/auth-backend-start-hidden.mjs` 会防止 `audit:auth` 的后端重启回退到旧 Windows WMI 流程，要求通过 `health-wsl-stack.sh` 控制后端；也会防止 `run-full-stack-local.py` 回退到手写长驻子进程启动，而不是统一经过 WSL stack / tmux 入口。
- `test:frontend`：warning semantics、导航/页面结构、build。
- `test:quality`：代码健康和产品体验启发式扫描。

### 5.4 old-data-source phase

执行前：

- 如果没有模拟器进程，启动 `watch_tcp_simulator_1000.py`。
- 数据源默认 `old`。
- 旧库必须期待非空数据。

执行命令：

```powershell
npm run test:integration:old
npm run test:perf:old
npm run test:full:old
```

### 5.5 new-data-source phase

执行前：

- 默认停止 `watch_tcp_simulator_1000.py`。
- 数据源切到 `new`。
- 默认允许空态。

执行命令：

```powershell
npm run test:integration:new
npm run test:perf:new
npm run test:full:new
```

特别说明：

- `test-full:new` 中 warning pipeline 遇到 new 空库无 warning 探针目标时，应写入 skipped。
- `test-full:new` 中 `audit:write` 遇到 new 空库缺少业务写探针前置数据时，应写入 `skipped`，且 `failedCount` 必须为 `0`；旧库同类问题仍必须失败。
- 如果 new 阶段出现旧库数据，属于双库隔离失败。

---

## 6. npm 分层测试

`D:/Health/HealthShow/package.json` 当前关键脚本：

| 档位 | 命令 | 用途 |
| --- | --- | --- |
| fast | `npm run test:fast` | 无后端、无浏览器依赖的最快检查 |
| frontend | `npm run test:frontend` | 结构、导航、页面结构、build |
| quality | `npm run test:quality` | 代码健康、产品体验启发式扫描 |
| integration old | `npm run test:integration:old` | old API/data/auth |
| integration new | `npm run test:integration:new` | new API/data/auth |
| perf old | `npm run test:perf:old` | old API 和页面性能 |
| perf new | `npm run test:perf:new` | new API 和页面性能 |
| full old | `npm run test:full:old` | old 全前端门禁、e2e、pipeline、write |
| full new | `npm run test:full:new` | new 全前端门禁、e2e、pipeline、write |

`scripts/health-test-runner.mjs` 当前 profile：

| profile | 内容 |
| --- | --- |
| `fast` | `test:warning-semantics` |
| `frontend` | `test:warning-semantics`、`audit:structure`、`build` |
| `integration` | `audit:api`、`audit:data:<source>`、`audit:auth` |
| `perf` | `audit:perf:<source>` |
| `quality` | `audit:quality:code`、`audit:product` |
| `full` | `test:frontend`、`test:quality`、`audit:api`、`audit:data:<source>`、`audit:auth`、`audit:e2e`、`audit:perf:<source>`、`audit:pipeline`、`audit:pipeline-warning`、`audit:write` |

---

## 7. 结果状态语义

所有测试结果只能落到以下状态：

| 状态 | 含义 | 是否可继续 |
| --- | --- | --- |
| `passed` | 命令执行完成且断言通过 | 可继续下一档 |
| `failed` | 代码行为、接口契约、页面行为、数据密度或性能预算失败 | 不可发布 |
| `blocked` | 环境、依赖、服务、凭据、端口、浏览器不可用 | 不可发布，但不能直接改业务代码 |
| `skipped` | 当前数据源缺少允许缺失的前置数据 | 可继续，但报告必须解释 |
| `warning` | 非阻断风险或启发式扫描发现 | 可继续，但进入 backlog |

强制规则：

- `blocked` 不得包装成 `passed`。
- `failed` 不得因为 agent 想继续跑而改成 `warning`。
- 新库空态允许 `skipped`，旧库核心数据空不允许 `skipped`。
- 任何脚本降级都必须写入 `summary.md` 和 `summary.json`。

---

## 8. 产物目录和报告

runner 每轮产物：

```text
D:/Health/HealthShow/tests/runs/<run-id>/
  full-stack-local-summary.json
  summary.md
  full-stack-local.log
  *.out.log
  *.err.log
  *.combined.log
  git-healthshow-status.log
  git-healthdata-status.log
```

最近一次 full-stack-local 指针：

```text
D:/Health/HealthShow/tests/runs/latest-full-stack-local.json
```

OpenClaw 默认只读：

```text
D:/Health/HealthShow/tests/runs/latest-full-stack-local.json
D:/Health/HealthShow/tests/runs/<run-id>/full-stack-local-summary.json
D:/Health/HealthShow/tests/runs/<run-id>/summary.md
D:/Health/HealthShow/tests/runs/<run-id>/hermes-archive.md
```

除非失败需要生成 issue，不要默认读取所有大日志。

`summary.md` 必须包含：

- run id
- 总状态
- 数据源
- readiness
- step 表格
- failed steps
- skipped steps
- hermes archive path
- git status log 路径

`full-stack-local-summary.json` 必须包含：

- `runId`
- `profile`
- `dataSource`
- `status`
- `statusReason`
- `parameters`
- `environment`
- `readiness`
- `gitStatus`
- `startedProcesses`
- `stoppedProcesses`
- `phases`
- `steps`
- `skippedSteps`

---

## 9. 失败分诊

一轮失败只处理一个最高优先级根因。

优先级：

1. 环境阻塞：SQL、Redis、后端 HTTP、后端 TCP、前端、SQL_PASSWORD。
2. runner 自身错误：summary 写不出、日志路径错、命令转义错、OpenClaw 无法触发。
3. 登录鉴权失败：`audit:auth`。
4. 双库错路由：请求头、响应头、Cookie、SQL 库、Redis key 不一致。
5. 写入链路失败：TCP -> Redis -> SQL -> API -> 页面。
6. warning pipeline 失败：预警生成、状态处理、页面展示不闭环。
7. API smoke 大面积失败。
8. 旧库数据密度失败。
9. 页面 e2e 白屏、console error、关键操作失败。
10. build、结构、导航失败。
11. 性能预算失败。
12. 代码健康或产品体验 warning。

issue 包必须包含：

```json
{
  "issue_id": "health-<category>-<date>-001",
  "source": "openclaw-or-hermes",
  "run_id": "<run-id>",
  "data_source": "old",
  "status": "failed",
  "failing_command": "npm run test:full:old",
  "exit_code": 1,
  "expected": "应当发生什么",
  "actual": "实际发生什么",
  "failure_signature": "稳定可搜索的失败特征",
  "artifact_paths": [],
  "suspected_files": [],
  "verification_command": "最小复验命令",
  "constraints": [
    "一次只修一个最高优先级失败",
    "不要回滚用户未提交改动",
    "不要删除或弱化测试断言",
    "HealthShow 和 HealthData 分别检查 git status"
  ]
}
```

---

## 10. OpenClaw 执行流程

### 10.1 标准执行

OpenClaw 接到“跑 Health 完整测试”时默认执行：

```powershell
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py
```

### 10.2 后台启动方式

如果 OpenClaw 不适合长时间等待，可以后台启动：

```powershell
python3 /home/j/code/health/tools/start-health-runner.py --data-source both
```

之后监控：

```powershell
Get-Content -LiteralPath "D:\Health\HealthShow\tests\runs\latest-full-stack-local.json" -Raw
```

### 10.3 OpenClaw 可以拆给多个 Codex 的场景

只有在 runner 已经完成一轮，并且 summary 中有多个互相独立的问题时，OpenClaw 才适合拆给多个 Codex。

允许并行拆分的例子：

- Codex A：只处理后端 regression 失败。
- Codex B：只处理前端 `audit:structure` 失败。
- Codex C：只处理性能报告中的单一慢接口分析。
- Codex D：只处理文档或 summary 结构问题。

不允许并行拆分的例子：

- 同一个 `request.js`、数据源切换、Cookie 或登录链路让多个 Codex 同时改。
- 同一个 pipeline 根因拆成多个修复任务。
- 没有稳定 failure signature 就并行猜测。
- 未读 git 状态就让多个 Codex 改同一仓库同一文件区域。

并行 Codex 任务必须写清：

- 只读哪些 artifact。
- 只改哪些文件或完全不改文件。
- 复验命令是什么。
- 不得回滚其他人的改动。

---

## 11. Hermes 执行流程

Hermes 在 WSL2 中运行，因此必须注意：

- WSL2 里的 `localhost` 不一定等价于 Windows 服务视角。
- Hermes 不应该直接用 WSL2 的 `localhost:8080`、`localhost:9528` 作为最终 Windows readiness 证据。
- Hermes 更适合读取 Windows runner 已生成的文件，例如 `/mnt/d/Health/HealthShow/tests/runs/...`。

Hermes 正确流程：

```text
1. 读取 latest-full-stack-local.json
2. 读取 summary.md 和 full-stack-local-summary.json
3. 判断 status
4. 如果 passed，生成归档摘要
5. 如果 failed/blocked/skipped，生成一个最高优先级 issue
6. 把 issue 交给 Codex 或 OpenClaw 分发
7. 修复后读取新 run 的 summary 做趋势对比
```

Hermes 不应该：

- 直接长期跑 PowerShell 命令。
- 自己修改 HealthShow/HealthData 代码。
- 在没有 Windows runner summary 的情况下裁定服务可用性。
- 同时生成多个根因混合 issue。

---

## 12. Codex 修复闭环

Codex 收到 issue 后固定流程：

1. 读 `D:/Health/AGENTS.md`。
2. 读本文档。
3. 读 issue 包。
4. 分别在 `D:/Health/HealthShow` 和 `D:/Health/HealthData` 查看 `git status --short`。
5. 复现或读取 failure artifact。
6. 判断是环境、runner、测试、数据前置还是产品代码问题。
7. 做最小改动。
8. 跑 issue 指定 verification command。
9. 如果涉及高风险链路，补跑对应档位。
10. 更新或说明 run summary、残余风险和下一步。

补跑规则：

| 改动范围 | 必须补跑 |
| --- | --- |
| runner | `run-full-stack-local.py` 对应最小 `--data-source old/new` |
| 登录、Cookie、鉴权 | `npm run audit:auth`，必要时 `npm run audit:e2e` |
| 双库切换 | `test:integration:old` + `test:integration:new` 或 `-DataSource both` |
| TCP/Redis/写入 | `npm run audit:pipeline` + `npm run audit:pipeline-warning` |
| 后端 mapper/service | Maven test + backend regression + API smoke |
| 页面结构/路由 | `npm run audit:structure` + `npm run build` |
| 核心页面 UI | `npm run audit:e2e` + 截图检查 |

---

## 13. 手工和专项验收

### 13.1 双库隔离

必须验证：

- old 请求返回 old 响应头。
- new 请求返回 new 响应头。
- old 数据密度非空。
- new 空态不显示 old 缓存。
- 模拟器 IMEI 只写 old。
- 真实手表默认写 new。
- Redis 使用 `health:buffer:old` / `health:buffer:new`。

### 13.2 关键页面截图

自动 e2e 通过后，关键页面仍建议做截图验证：

分辨率：

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

### 13.3 真实手表验收

新库上线前必须做：

1. 停止模拟器。
2. 确认真实手表 IMEI 不命中模拟器正则 `^3594567800\d{5}$`。
3. 手表连入 TCP 9000。
4. SQL 证明写入 `health_new.device` 或对应月分表。
5. 前端切 new 后能看到真实数据或明确空态。

---

## 14. 发布前验收口径

发布前至少需要：

```text
old full-stack-local passed
backend Maven test passed
backend regression passed
frontend build passed
audit:auth passed
audit:e2e passed
audit:pipeline passed
audit:pipeline-warning passed 或 new 空库合理 skipped
audit:data:old passed
```

如果涉及新库上线或真实手表：

```text
new 阶段必须停模拟器
new 空态必须有证据
真实手表写 new 必须有 SQL 证据
不得出现 old 数据串到 new 页面
```

如果只是旧库演示或模拟器压测：

```text
默认 DataSource old
模拟器应运行
old 数据密度必须非空
pipeline 和 warning pipeline 必须 passed
```

---

## 15. 跑不下去时的处理规则

如果 runner 跑不下去：

1. 先看 `summary.md` 是否生成。
2. 再看 `full-stack-local.log`。
3. 再看失败 step 的 `*.combined.log`。
4. 判断状态：
   - 服务或端口不可用：`blocked`
   - 命令退出非 0：`failed`
   - new 空库缺探针：`skipped`
   - summary 写入异常：runner bug
5. 只修一个最高优先级阻塞。
6. 修完从同一档位继续跑，不从旧文档手动拼命令。

常见修复方向：

| 现象 | 优先处理 |
| --- | --- |
| SQL 58135 不通 | 检查 SQL Server 服务 |
| Redis 6379 不通 | 检查 Redis 服务或 runner 启动日志 |
| backend 8080 不通 | 看 `backend.err.log` 和 Maven 启动日志 |
| 跑 runner 或 `audit:auth` 时后端无法重启，或 `audit:auth` 卡在 `starting backend process` | 检查 `tests/run-full-stack-local.py` 是否仍通过 `health-wsl-stack.sh` 管理后端、前端和模拟器；检查 `tests/e2e/auth-session-regression.mjs` 是否仍通过 WSL stack 控制后端启停；然后跑 `npm run test:fast` 和 `npm run audit:auth` |
| TCP 9000 不通 | 看后端是否启动完成 |
| frontend 9528 不通 | 看 `frontend.err.log` 和 Vite 端口 |
| `sqlPasswordSet=false` | 检查 `SQL_PASSWORD` 或 `application.yml` |
| old 数据密度空 | 先确认是否误切 new 或模拟器未运行 |
| new 出现 old 数据 | 查 Cookie、请求头、响应头和缓存 |
| warning pipeline new 失败 | 先判断是否是 new 空库无 warning 探针 |
| `audit:write` 在 new 空库失败 | 先确认是否缺少绑定设备、未处理 warning 或 AI 报告员工候选；这些业务写探针在 new 空态应为 `skipped`，但 old 不允许跳过 |

---

## 16. 当前仍需改进

当前流程已经可以交给 OpenClaw 作为标准入口，但仍建议继续改进：

1. 补一轮 `-DataSource new` 的最新 run summary，确认 new 空态和 skipped 语义。
2. 补一轮 `-DataSource both` 的最新 run summary，确认 old -> new 模拟器切换。
3. 让 OpenClaw 固定只读 latest 指针和 summary，不扫全量日志。
4. 让 Hermes 的 issue 包模板固定化，避免一次混多个根因。
5. 给 runner 增加 latest passed old/new/both 指针。
6. 给截图专项增加固定脚本和产物目录。
7. 根级统一 runner 现已切到 `/home/j/code/health/tests/run-health-loop.py`；后续继续以它和 `run-full-stack-local.py` 作为主入口。

---

## 17. 给 OpenClaw 的标准任务文本

可以直接把下面这段交给 OpenClaw：

```text
在 Windows 上运行 Health 标准完整测试，默认使用旧库 old。

执行命令：
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py

只读取以下结果：
D:\Health\HealthShow\tests\runs\latest-full-stack-local.json
D:\Health\HealthShow\tests\runs\<run-id>\full-stack-local-summary.json
D:\Health\HealthShow\tests\runs\<run-id>\summary.md

如果 status=passed，只汇报 run_id、dataSource、summary 路径。
如果 status=failed/blocked/skipped，交给 Hermes 生成一个最高优先级 issue，或者交给 Codex 判断根因。
不要直接修改 HealthShow 或 HealthData 代码。
不要把 failed 改成 passed。
不要读取所有大日志，除非 summary 指向失败 step。
```

如果需要允许 OpenClaw 拆给多个 Codex，追加：

```text
如果 summary 显示多个互相独立的问题，可以拆给多个 Codex。
每个 Codex 只能处理一个明确 issue，必须声明只读/可改文件范围和复验命令。
不要让多个 Codex 同时修改同一链路或同一文件区域。
```

---

## 18. 给 Hermes 的标准任务文本

```text
你在 WSL2 中参与 Health 测试归档和分诊。

不要直接用 WSL2 localhost 判断 Windows 服务是否可用。
请读取 Windows runner 产物：
/mnt/d/Health/HealthShow/tests/runs/latest-full-stack-local.json
/mnt/d/Health/HealthShow/tests/runs/<run-id>/full-stack-local-summary.json
/mnt/d/Health/HealthShow/tests/runs/<run-id>/summary.md

如果 passed，生成简短归档报告。
如果 failed/blocked/skipped，只生成一个最高优先级 issue 包。
issue 包必须包含 run_id、data_source、failing_command、exit_code、expected、actual、failure_signature、artifact_paths、suspected_files、verification_command。
不要直接修改 HealthShow 或 HealthData。
```

---

## 19. 给 Codex 的标准任务文本

```text
你在 D:/Health 处理 Health 测试失败闭环。

先读：
D:/Health/AGENTS.md
D:/Health/HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md
以及 issue 包和 runner summary。

要求：
1. D:/Health 不是 git 仓库，HealthShow 和 HealthData 分别查看 git status。
2. 不回滚用户未提交改动。
3. 一次只修一个最高优先级失败。
4. 不删除或弱化测试断言。
5. 先判断环境、runner、测试前置、数据源，再判断产品代码。
6. 做最小修改后跑 issue 指定 verification command。
7. 涉及双库、pipeline、登录、后端 mapper/service 时补跑对应高风险回归。
```

---

## 20. 最终判断

现在可以先找 OpenClaw 开始标准旧库完整测试，前提是 OpenClaw 只做入口、执行和通知。

推荐实际闭环：

```text
用户 -> OpenClaw
OpenClaw -> Windows runner
Windows runner -> summary
Hermes -> 归档/单 issue
Codex -> 根因判断/修复/复跑
OpenClaw -> 通知最终状态
```

这套流程目前已经达到“可以先找 OpenClaw”的程度；但它不是“OpenClaw 独立决定一切”，而是“OpenClaw 触发稳定 runner，失败后让 Hermes/Codex 接力”。

最新夜间监督触发双库 run（2026-05-11 01:12 更新）：

```text
run_id: 20260511-010107-full-stack-local-both
status: skipped
status_reason: allowed skipped steps: 1
data_source: both
summary: D:/Health/HealthShow/tests/runs/20260511-010107-full-stack-local-both/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260511-010107-full-stack-local-both/full-stack-local-summary.json
hermes_archive: D:/Health/HealthShow/tests/runs/20260511-010107-full-stack-local-both/hermes-archive.md
failed_steps: none
skipped_steps: test-full-new only, allowed because health_new is currently empty business data
old_data_source_facts: health department=20, employee=1000, device=1000, device_user=1000, simulatorDeviceRows=1000
new_data_source_facts: health_new department=0, employee=0, device=0, device_user=0, simulatorDeviceRows=0
finished_at: 2026-05-11T01:12:08.3013949+08:00
```


最新夜间监督触发双库 run（2026-05-11 01:29 更新）：

```text
run_id: 20260511-011838-full-stack-local-both
status: skipped
status_reason: allowed skipped steps: 1
data_source: both
summary: D:/Health/HealthShow/tests/runs/20260511-011838-full-stack-local-both/summary.md
summary_json: D:/Health/HealthShow/tests/runs/20260511-011838-full-stack-local-both/full-stack-local-summary.json
hermes_archive: D:/Health/HealthShow/tests/runs/20260511-011838-full-stack-local-both/hermes-archive.md
phase_statuses: backend=passed, frontend-common=passed, old-data-source=passed, new-data-source=skipped
failed_steps: none
skipped_steps: test-full-new only, allowed because health_new is currently empty business data
old_data_source_facts: health department=20, employee=1000, device=1000, device_user=1000, simulatorDeviceRows=1000
new_data_source_facts: health_new department=0, employee=0, device=0, device_user=0, simulatorDeviceRows=0
simulator_evidence: started pid=45256 during old phase, stopped pid=45256 before old full pipeline, reason=old-full-pipeline-exclusive-tcp, final simulatorRunning=false
hermes_triage: failed steps=0, allowed skipped steps=1, no Health code repair required from this cycle
finished_at: 2026-05-11T01:29:47.8671909+08:00
```

