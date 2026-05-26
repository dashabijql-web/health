# Health 项目 WSL2 / Hermes 自动化治理适配设计

## 1. 背景

`2026-05-26-Windows-Codex-近全自动进化开发方法论(1).md` 提供了一套较完整的自动化推进方法，但它默认的项目画像更接近：

- Windows 本机作为主执行环境
- PowerShell 作为主 runner 入口
- 顶层治理、自动化映射、接力系统、远端诊断为独立文档与脚本

Health 项目当前真实状态与其并不完全一致。当前代码和脚本表明，本项目已经演进为：

- `WSL2-first` 执行环境
- 根目录 monorepo
- `tmux + bash + python3` 作为主 runner 托管链
- `HealthShow/tests/run-full-stack-local.py`、`tests/run-health-loop.py`、`tools/start-health-runner.py` 作为主要自动化入口
- `summary.json`、`summary.md`、`hermes-archive.md`、`tests/latest-run.json` 作为主要运行证据

同时，仓库内仍存在若干历史文档漂移：

- `AGENTS.md` 与 `单仓库迁移实施计划.md` 已表明根目录是唯一 git 仓库。
- `HEALTH_EXECUTION_ENTRY.md`、`HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`、`docs/archive/历史归档-HEALTH_HANDOFF.md` 仍保留“根目录不是 git 仓库 / 双仓协作”的旧叙述。
- Hermes、OpenClaw、runner 的边界虽然在多个文档中已有描述，但缺少一份面向当前 WSL2 现实的统一治理适配设计。

本设计的目标不是推翻现有 Health 自动化链路，而是把方法论翻译成适合本项目的 `WSL2 + Hermes + monorepo` 版本，并以最小增量补齐当前缺口。

## 2. 目标与非目标

### 2.1 目标

- 把方法论文档中的“Windows 主链”改写为 Health 当前真实的 `WSL2-first` 主链。
- 明确 Hermes 在本项目中的主线程 / 监督者定位，而不是把它与 OpenClaw 或 Codex 混用。
- 统一 monorepo、runner、summary、handoff、issue、supervisor 的事实源。
- 基于现有脚本能力，补出最缺的治理层资产，而不是重复造一套平行 runner。
- 为后续 `项目自动化映射.md`、接力文档生成器、远端诊断和夜间 supervisor 第二轮收口提供设计依据。

### 2.2 非目标

- 不重写现有 `HealthShow/tests/run-full-stack-local.py`、`tests/run-health-loop.py` 的业务验证逻辑。
- 不把 Hermes 改造成直接长期托管服务或直接写业务代码的执行器。
- 不在本设计中直接落地所有治理脚本，只定义边界、优先级和目标结构。
- 不回退到旧双仓 / 旧 PowerShell 主执行流。

## 3. 当前事实

### 3.1 执行环境事实

当前首选执行链路已经是 WSL2：

- 栈管理：`tools/health-wsl-stack.sh`
- 环境探测：`tools/health-wsl-env.sh`
- detached runner：`tools/start-health-runner.py`
- root loop runner：`tests/run-health-loop.py`
- full-stack runner：`HealthShow/tests/run-full-stack-local.py`
- 日志目录：`runtime-logs/wsl-stack/`

这意味着方法论中的下列默认项必须替换：

- `win-stack.ps1` -> `health-wsl-stack.sh`
- `win-test.ps1` -> `run-health-loop.py`
- PowerShell hidden window / Windows service host -> `tmux` 托管的 WSL2 session
- `runtime-logs/win-stack` -> `runtime-logs/wsl-stack`

### 3.2 仓库事实

当前根目录 `/home/j/code/health` 已经是 monorepo 根仓库，`HealthShow` 和 `HealthData` 是子目录，不再是独立 git 仓库。

因此方法论中所有“分仓执行 git status / commit / push”的规则，在 Health 当前态都必须重写为：

- 所有 git 操作都在根目录执行
- commit 只能包含本次任务确认的文件
- 不回退用户现有脏工作树

### 3.3 运行证据事实

项目已具备较成熟的证据化自动化产物：

- `tests/latest-run.json`
- `tests/open-issues.json`
- `tests/evolution-backlog.json`
- `HealthShow/tests/runs/<run-id>/full-stack-local-summary.json`
- `HealthShow/tests/runs/<run-id>/summary.md`
- `HealthShow/tests/runs/<run-id>/hermes-archive.md`
- `tests/runs/<run-id>/summary.json`
- `tests/runs/<run-id>/summary.md`

这说明方法论中“任何自动化都必须留下证据”的核心原则，在 Health 项目并不是空白，而是已经落成了第一批主干资产。

### 3.4 角色事实

当前更合理的职责边界应是：

| 角色 | 正确职责 | 不应承担的职责 |
| --- | --- | --- |
| WSL2 runner | 启动服务、跑回归、生成 summary / issue 证据 | 根因仲裁、代码修复、业务结论裁决 |
| Hermes | 主线程监督、读取 latest run、分诊、归档、单 issue 包、handoff | 长期托管本地服务、直接改业务代码、混多个根因 |
| Codex | 读取单 issue、最小修改、验证、说明残余风险 | 长期无人值守编排、替代 Hermes 做全局调度 |
| OpenClaw | 外部入口、通知、触发 runner、必要时多 agent 分发 | 直接承担主 runner、直接修改 Health 业务代码 |

## 4. 方案比较

### 4.1 方案 A：照搬 Windows 方法论

直接按原文补 `win-stack.ps1`、`remote-check.ps1`、PowerShell handoff 脚本，并继续以 Windows 脚本为主链。

优点：

- 对原方法论文档改动最少。

缺点：

- 与项目当前 `WSL2-first` 现实不符。
- 会制造一套和现有 WSL runner 平行的第二执行流。
- 后续容易再次出现“文档写一套、实际跑一套”。

### 4.2 方案 B：WSL2-first 适配，复用现有 runner 主链

保留现有 `health-wsl-stack`、`run-full-stack-local.py`、`run-health-loop.py`、`start-health-runner.py`，把方法论文档中的治理层概念翻译成 WSL2 对应物，只补当前缺失的映射、handoff 和 remote-check。

优点：

- 与当前代码事实一致。
- 最大化复用现有 runner、summary、artifact 链。
- 对现有开发节奏扰动最小。

缺点：

- 需要先统一历史文档口径。
- 需要设计一批新的治理层产物和字段。

### 4.3 方案 C：让 Hermes 直接接管主 runner

让 Hermes 直接启动 / 停止服务、直接维护长期测试循环，并在 WSL2 内自行判断环境 readiness。

优点：

- 从表面上看，自动化“更集中”。

缺点：

- 容易让 Hermes 同时承担执行与裁决，边界变脏。
- 会绕开现有稳定的 WSL runner 证据链。
- 一旦 Hermes 判断失真，问题难以定位是 runner 失稳还是业务回归。

### 4.4 推荐方案

采用方案 B：`WSL2-first 适配，复用现有 runner 主链`。

## 5. 推荐架构

目标架构应收口为以下形态：

```text
用户 / 外部入口
  -> OpenClaw（可选）
  -> Hermes（主线程监督 / 分诊 / 归档）
  -> WSL2 runner（执行）
  -> summary / issue / backlog / handoff
  -> Codex（单 issue 修复）
  -> runner 复跑
  -> Hermes 更新归档
```

### 5.1 WSL2 runner 是执行主链

执行主链固定为：

- `bash tools/health-wsl-stack.sh ...`
- `python3 HealthShow/tests/run-full-stack-local.py --data-source ...`
- `python3 tests/run-health-loop.py --profile ... --data-source ...`
- `python3 tools/start-health-runner.py --data-source ...`

任何新治理脚本都不能再创建一个与上述平行的“主测试执行器”。

### 5.2 Hermes 是主线程监督者

Hermes 在本项目中的推荐职责固定为：

- 读取 `AGENTS.md`、自动化映射、实施计划、latest run、latest handoff
- 裁决当前唯一主线
- 触发 runner 或读取 runner 已完成产物
- 把失败压缩成一个最高优先级 issue
- 分发给 Codex
- 复跑后更新 handoff / 里程碑 / 阻塞记录

Hermes 不应直接：

- 长时间托管 `health-wsl-stack.sh`
- 替代 runner 直接手写全量测试命令
- 直接修改业务代码
- 同时分发多个互相耦合的修复任务

### 5.3 OpenClaw 是入口和通知层

OpenClaw 继续保留以下角色：

- 外部消息入口
- 夜间 supervisor 触发点
- 结果通知
- 多 agent 分发入口

但不应成为 Health 主测试稳定性的唯一依赖。

## 6. 需要新增或收口的治理资产

### 6.1 项目自动化映射

应新增一份顶层文档，例如：

- `项目自动化映射.md`

必须明确：

- monorepo 根路径
- 主分支 / 工作分支规则
- WSL2 runner 入口
- `tmux` 托管约定
- latest run 指针位置
- handoff 文档位置
- 里程碑 / 阻塞记录目录
- 通知 / 待发送队列位置
- OpenClaw / Hermes / Codex 分工
- 上下文硬切规则

### 6.2 结构化 handoff

当前项目已有 summary 和 archive，但仍缺方法论意义上的统一接力快照。

建议新增：

- `接力文档/` 目录
- 机器生成的 latest handoff json / md
- `查找最新接力文档` 对应脚本
- `handoff smoke` 对应脚本

最少字段应包含：

- `head_commit`
- `working_tree_state`
- `active_workstream`
- `next_single_action`
- `next_action_done_when`
- `blocked_by`
- `services_runtime_state`
- `last_verified_commands`
- `required_preread_docs`
- `latest_run_id`
- `latest_milestone`
- `latest_blocker`

### 6.3 里程碑与阻塞台账

当前 run summary 很强，但“里程碑版本 / 阻塞升级 / 恢复记录”仍偏分散。

建议新增固定目录：

- `运行记录/里程碑版本/`
- `运行记录/阻塞升级/`
- `运行记录/待发送队列/`

这些目录可以先从 markdown 开始，不要求第一版就引入数据库或复杂索引。

### 6.4 WSL2 版 remote-check

当前仓库缺少方法论里的远端链路分层诊断脚本。

应新增 WSL2 版：

- `tools/remote-check.sh` 或 `tools/remote-check.py`
- `tools/remote-env.sh`（如确有代理需求）

职责应包括：

- GitHub 根站可达性
- repo 页面可达性
- `git ls-remote origin HEAD`
- 当前分支 ahead / behind
- push 失败证据落盘

其输出应进入 `runtime-logs/`，并能被 handoff 读取。

## 7. 文档统一原则

后续应把下列旧叙述统一改成当前事实：

- “根目录不是 git 仓库” -> 改为“根目录是唯一 git 仓库”
- “PowerShell 是默认主 runner” -> 改为“WSL2 runner 是默认主执行链”
- “Hermes 不适合直接长时间控制 Windows 命令” -> 改写为“Hermes 不适合直接承担主 runner，但适合在 WSL2 里做监督、分诊和归档”

优先需要同步的文件：

- `AGENTS.md`
- `HEALTH_EXECUTION_ENTRY.md`
- `HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
- `HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md`
- `docs/archive/历史归档-HEALTH_HANDOFF.md`

## 8. 分阶段落地顺序

### 8.1 Phase 1：统一事实源

先统一文档中的以下事实：

- monorepo
- WSL2 runner
- Hermes / OpenClaw / Codex 边界

退出条件：

- 不再存在“当前主执行链到底是 PowerShell 还是 WSL2”这种歧义。

### 8.2 Phase 2：补治理骨架

新增：

- `项目自动化映射.md`
- handoff 目录和 latest 指针
- 里程碑 / 阻塞记录目录

退出条件：

- 新会话可在 1 分钟内读懂当前主线、最新 run 和下一动作。

### 8.3 Phase 3：补远端诊断

新增 remote-check 与 push 失败留痕。

退出条件：

- push 失败不再只有一句“失败了”，而有分层证据。

### 8.4 Phase 4：收口 supervisor 第二轮

在前 3 阶段完成后，再增强：

- `tools/start-openclaw-evolution-supervisor.py`

增强方向：

- checkpoint 前置
- latest handoff 读取
- latest milestone / blocker 前移
- remote-check 读取
- “停止本轮后停”之外的流程自愈能力

## 9. 成功标准

当以下条件同时满足时，才算这套方法论已经真正适配到 Health 项目：

- 主执行链明确为 WSL2 runner，而不是文档口头假设。
- Hermes 被明确成主线程监督者，而不是模糊的“也许可以做总控”。
- monorepo、runner、summary、handoff、issue、backlog 的事实源一致。
- 新会话可通过 latest handoff 和 latest run 快速接班。
- push / 远端异常有分层证据，不再只能口头复盘。
- OpenClaw / Hermes / Codex 不再在职责上相互覆盖。

## 10. 推荐下一步

本设计确认后，第一批实现应只做治理层收口，不碰业务功能：

1. 新增 `项目自动化映射.md`
2. 新增 handoff 结构与 latest 指针
3. 新增 WSL2 版 remote-check
4. 同步修正文档中的 monorepo / WSL2 / Hermes 漂移事实

不建议第一步就直接重写 runner 或扩写夜间 supervisor。
