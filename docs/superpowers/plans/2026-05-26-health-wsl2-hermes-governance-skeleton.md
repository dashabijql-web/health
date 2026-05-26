# Health WSL2 / Hermes Governance Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first governance-layer skeleton for the Health monorepo so WSL2 runner entrypoints, Hermes/OpenClaw/Feishu roles, handoff artifacts, and remote-check evidence are explicit, scriptable, and testable without touching business features.

**Architecture:** Keep the current WSL2 runner chain (`health-wsl-stack.sh`, `run-full-stack-local.py`, `run-health-loop.py`, `start-health-runner.py`) as the only execution path. Add a thin governance layer around it: one automation mapping doc, one handoff generator, one remote-check script, one notification-queue skeleton, and a small set of doc corrections that remove Windows/double-repo drift.

**Tech Stack:** Markdown, Python 3, Bash, Git, Node built-in test runner

---

## File Structure

- `项目自动化映射.md`
  Current-source governance map for monorepo paths, WSL2 entrypoints, runner pointers, OpenClaw/Hermes/Codex/Feishu boundaries, and hard-switch rules.
- `接力文档/README.md`
  Documents the handoff directory contract and what generated files are expected to contain.
- `tools/generate-health-handoff.py`
  Reads git state, `tests/latest-run.json`, runtime status, and optional remote-check output; writes a structured handoff snapshot.
- `tools/find-latest-handoff.py`
  Stable lookup for the newest handoff snapshot.
- `tools/remote-check.py`
  Produces layered GitHub/remote diagnostics and writes JSON/Markdown evidence into `runtime-logs/remote-check/`.
- `tools/queue-health-notification.py`
  Writes structured pending notification payloads for Feishu fallback delivery.
- `tests/handoff-smoke.mjs`
  Verifies the handoff generator and lookup script contracts.
- `tests/remote-check-smoke.mjs`
  Verifies the remote-check script output contract without assuming network success.
- `tests/notification-queue-smoke.mjs`
  Verifies queue file schema and Feishu-targeted pending message payload shape.
- `AGENTS.md`
  Project guardrails; must stop contradicting WSL2-first/monorepo/OpenClaw/Feishu reality.
- `HEALTH_EXECUTION_ENTRY.md`
  First-screen execution doc; must stop advertising PowerShell and split-repo flow as current.
- `HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
  High-level automation runbook; must express WSL2 runner, OpenClaw, Hermes, and Feishu roles consistently.
- `HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md`
  Historical-but-still-authoritative process doc; must be corrected where it still treats hidden Windows PowerShell runner as current.
- `.gitignore`
  Must keep generated handoff snapshots and pending queue payloads out of git while allowing README placeholders.

---

### Task 1: Create the project automation map

**Files:**
- Create: `项目自动化映射.md`
- Review: `AGENTS.md`
- Review: `docs/WSL托管启动说明.md`
- Review: `docs/superpowers/specs/2026-05-26-Health项目WSL2-Hermes自动化治理适配设计.md`

- [ ] **Step 1: Write the automation-map document skeleton**

```markdown
# 项目自动化映射

## 1. 当前事实
- monorepo 根目录
- WSL2-first
- 唯一主执行链

## 2. 角色边界
- Hermes
- OpenClaw
- Codex
- 飞书

## 3. 入口与指针
- stack / runner / latest-run / handoff / remote-check
```

- [ ] **Step 2: Fill the document with concrete current paths and commands**

```markdown
## 3. WSL2 主执行链
- `bash /home/j/code/health/tools/health-wsl-stack.sh all start`
- `python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both`
- `python3 /home/j/code/health/tests/run-health-loop.py --profile full --data-source both`
- `python3 /home/j/code/health/tools/start-health-runner.py --data-source both`

## 4. 核心指针
- latest run: `/home/j/code/health/tests/latest-run.json`
- handoff dir: `/home/j/code/health/接力文档/`
- remote-check dir: `/home/j/code/health/runtime-logs/remote-check/`
```

- [ ] **Step 3: Verify the new doc does not preserve Windows entrypoints as current paths**

Run: `rg -n "PowerShell|win-|隐藏 PowerShell|不是 git 仓库" 项目自动化映射.md`

Expected: no matches that describe the current execution path

- [ ] **Step 4: Commit**

```bash
git add 项目自动化映射.md
git commit -m "文档：新增项目自动化映射"
```

### Task 2: Add the structured handoff generator and lookup scripts

**Files:**
- Create: `接力文档/README.md`
- Create: `tools/generate-health-handoff.py`
- Create: `tools/find-latest-handoff.py`
- Create: `tests/handoff-smoke.mjs`
- Modify: `.gitignore`

- [ ] **Step 1: Reserve the handoff directory and ignore generated snapshots**

```gitignore
接力文档/*
!接力文档/README.md
!接力文档/.gitkeep
```

- [ ] **Step 2: Document the handoff contract**

```markdown
# 接力文档目录说明

- `latest-handoff.json`
- `latest-handoff.md`
- `<timestamp>-health-handoff.json`
- `<timestamp>-health-handoff.md`

机器字段至少包括：
- `head_commit`
- `working_tree_state`
- `active_workstream`
- `next_single_action`
- `latest_run_id`
```

- [ ] **Step 3: Implement the generator with a testable dry-run/output-dir contract**

```python
parser.add_argument("--output-dir", default=str(ROOT / "接力文档"))
parser.add_argument("--dry-run", action="store_true")

payload = {
    "head_commit": head_commit,
    "working_tree_state": working_tree_state,
    "active_workstream": active_workstream,
    "latest_run_id": latest_run_id,
    "next_single_action": next_single_action,
}
```

- [ ] **Step 4: Implement the stable latest-handoff lookup script**

```python
latest = sorted(output_dir.glob("*-health-handoff.json"))[-1]
print(json.dumps({"latest": str(latest)}, ensure_ascii=False))
```

- [ ] **Step 5: Add a smoke test that generates into a temporary directory and verifies the contract**

```javascript
test('handoff generator writes latest pointers and required fields', async () => {
  const result = spawnSync('python3', ['tools/generate-health-handoff.py', '--output-dir', tempDir]);
  assert.equal(result.status, 0);
  const latest = JSON.parse(fs.readFileSync(path.join(tempDir, 'latest-handoff.json'), 'utf8'));
  assert.ok(latest.head_commit);
  assert.ok(latest.latest_run_id !== undefined);
});
```

- [ ] **Step 6: Run the smoke test and script dry-run**

Run: `node --test tests/handoff-smoke.mjs`

Expected: PASS

Run: `python3 tools/generate-health-handoff.py --dry-run`

Expected: JSON or markdown preview with required handoff fields and no file write error

- [ ] **Step 7: Commit**

```bash
git add .gitignore 接力文档/README.md tools/generate-health-handoff.py tools/find-latest-handoff.py tests/handoff-smoke.mjs
git commit -m "自动化：新增 handoff 生成与查找脚本"
```

### Task 3: Add WSL2 remote-check evidence and smoke coverage

**Files:**
- Create: `tools/remote-check.py`
- Create: `tests/remote-check-smoke.mjs`

- [ ] **Step 1: Implement layered remote diagnostics with structured output**

```python
checks = [
    {"name": "github-root", "command": ["curl", "-I", "https://github.com"]},
    {"name": "origin-head", "command": ["git", "ls-remote", "origin", "HEAD"]},
    {"name": "branch-state", "command": ["git", "status", "--short", "--branch"]},
]
```

- [ ] **Step 2: Write JSON and markdown reports into `runtime-logs/remote-check/`**

```python
report_dir = ROOT / "runtime-logs" / "remote-check"
json_path = report_dir / f"{timestamp}-remote-check.json"
md_path = report_dir / f"{timestamp}-remote-check.md"
latest_path = report_dir / "latest.json"
```

- [ ] **Step 3: Support dry-run and custom output directory for local verification**

```python
parser.add_argument("--output-dir", default=str(ROOT / "runtime-logs" / "remote-check"))
parser.add_argument("--dry-run", action="store_true")
```

- [ ] **Step 4: Add a smoke test that verifies schema, not network success**

```javascript
test('remote-check emits a structured report even when a check fails', async () => {
  const result = spawnSync('python3', ['tools/remote-check.py', '--output-dir', tempDir]);
  assert.equal(result.status, 0);
  const latest = JSON.parse(fs.readFileSync(path.join(tempDir, 'latest.json'), 'utf8'));
  assert.ok(Array.isArray(latest.checks));
  assert.ok(latest.checks.some(check => check.name === 'origin-head'));
});
```

- [ ] **Step 5: Run the smoke test and a dry-run**

Run: `node --test tests/remote-check-smoke.mjs`

Expected: PASS

Run: `python3 tools/remote-check.py --dry-run`

Expected: structured remote-check output without writing to the real runtime directory

- [ ] **Step 6: Commit**

```bash
git add tools/remote-check.py tests/remote-check-smoke.mjs
git commit -m "自动化：新增 WSL2 remote-check 证据脚本"
```

### Task 4: Add OpenClaw/Feishu queue skeleton and pending-message contract

**Files:**
- Create: `运行记录/待发送队列/README.md`
- Create: `tools/queue-health-notification.py`
- Create: `tests/notification-queue-smoke.mjs`
- Modify: `项目自动化映射.md`
- Modify: `.gitignore`

- [ ] **Step 1: Document the pending queue contract for Feishu fallback**

```markdown
# 待发送队列说明

默认目标：
- `feishu`

消息类型：
- `run_started`
- `run_finished`
- `blocked`
- `issue_opened`
- `recovered`
```

- [ ] **Step 2: Implement a queue writer that stores structured JSON payloads**

```python
payload = {
    "channel": "feishu",
    "event": args.event,
    "status": args.status,
    "run_id": args.run_id,
    "summary_path": args.summary_path,
    "created_at": created_at,
}
```

- [ ] **Step 3: Ignore generated queue payloads while keeping the README tracked**

```gitignore
运行记录/待发送队列/*
!运行记录/待发送队列/README.md
```

- [ ] **Step 4: Update the automation map so OpenClaw and Feishu are explicit, not implied**

```markdown
## 5. OpenClaw
- 外部入口
- 只触发稳定 runner

## 6. 飞书
- 主通知通道
- 失败时写入 `运行记录/待发送队列/`
```

- [ ] **Step 5: Add a smoke test for queue file shape**

```javascript
test('notification queue writer creates feishu-targeted pending payloads', async () => {
  const result = spawnSync('python3', [
    'tools/queue-health-notification.py',
    '--output-dir', tempDir,
    '--channel', 'feishu',
    '--event', 'run_finished',
    '--status', 'failed',
    '--run-id', 'demo-run'
  ]);
  assert.equal(result.status, 0);
  const files = fs.readdirSync(tempDir).filter(name => name.endsWith('.json'));
  assert.equal(files.length, 1);
});
```

- [ ] **Step 6: Run the smoke test**

Run: `node --test tests/notification-queue-smoke.mjs`

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add .gitignore 运行记录/待发送队列/README.md tools/queue-health-notification.py tests/notification-queue-smoke.mjs 项目自动化映射.md
git commit -m "自动化：新增 OpenClaw 与飞书待发送队列骨架"
```

### Task 5: Sync top-level docs to WSL2-first, monorepo, and notification reality

**Files:**
- Modify: `AGENTS.md`
- Modify: `HEALTH_EXECUTION_ENTRY.md`
- Modify: `HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
- Modify: `HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md`
- Modify: `docs/archive/历史归档-HEALTH_HANDOFF.md`

- [ ] **Step 1: Update first-screen execution docs to stop advertising split repos and PowerShell as current**

```markdown
- `/home/j/code/health` 是唯一 git 仓库
- WSL2 runner 是当前唯一主执行链
- PowerShell 历史入口已归档，不再作为当前方案
```

- [ ] **Step 2: Update role boundaries so Hermes, OpenClaw, Codex, and Feishu each have one clear job**

```markdown
- Hermes：主线程监督、归档、单 issue 分诊
- OpenClaw：外部入口、只触发稳定 runner、读取 latest 指针
- Codex：单 issue 修复
- 飞书：主通知通道，失败时回落待发送队列
```

- [ ] **Step 3: Add archive banners where older docs still mention hidden Windows runner or split repos**

```markdown
> 历史归档提示：本文件保留旧执行链背景，当前以 monorepo + WSL2 runner + OpenClaw/Hermes/Feishu 映射为准。
```

- [ ] **Step 4: Verify the doc set no longer presents Windows runner or split repos as current**

Run: `rg -n "不是 git 仓库|PowerShell runner|hidden Windows|双仓库|分别在两个子仓库" AGENTS.md HEALTH_EXECUTION_ENTRY.md HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md docs/archive/历史归档-HEALTH_HANDOFF.md`

Expected: remaining matches are only historical/archive disclaimers, not current instructions

- [ ] **Step 5: Commit**

```bash
git add AGENTS.md HEALTH_EXECUTION_ENTRY.md HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md docs/archive/历史归档-HEALTH_HANDOFF.md
git commit -m "文档：统一 WSL2 monorepo 与通知治理事实"
```

### Task 6: Verify the governance skeleton end to end

**Files:**
- Review: `项目自动化映射.md`
- Review: `接力文档/README.md`
- Review: `运行记录/待发送队列/README.md`
- Review: `runtime-logs/remote-check/*`

- [ ] **Step 1: Run all smoke tests**

Run: `node --test tests/handoff-smoke.mjs tests/remote-check-smoke.mjs tests/notification-queue-smoke.mjs`

Expected: PASS

- [ ] **Step 2: Generate a real handoff snapshot into the project directory**

Run: `python3 tools/generate-health-handoff.py`

Expected: `接力文档/latest-handoff.json` and `接力文档/latest-handoff.md` are created

- [ ] **Step 3: Run a real remote-check**

Run: `python3 tools/remote-check.py`

Expected: `runtime-logs/remote-check/latest.json` and corresponding markdown report are created

- [ ] **Step 4: Queue a sample failed notification**

Run: `python3 tools/queue-health-notification.py --channel feishu --event blocked --status blocked --run-id demo-governance --summary-path /home/j/code/health/tests/latest-run.json`

Expected: one JSON payload appears under `运行记录/待发送队列/`

- [ ] **Step 5: Inspect the generated artifacts before claiming completion**

```text
接力文档/latest-handoff.json
runtime-logs/remote-check/latest.json
运行记录/待发送队列/*.json
```

- [ ] **Step 6: Commit**

```bash
git add 项目自动化映射.md 接力文档/README.md 运行记录/待发送队列/README.md tests/handoff-smoke.mjs tests/remote-check-smoke.mjs tests/notification-queue-smoke.mjs tools/generate-health-handoff.py tools/find-latest-handoff.py tools/remote-check.py tools/queue-health-notification.py .gitignore AGENTS.md HEALTH_EXECUTION_ENTRY.md HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md HEALTH_LATEST_COMPLETE_TEST_FLOW_20260510.md docs/archive/历史归档-HEALTH_HANDOFF.md
git commit -m "自动化：落地 WSL2 Hermes 治理骨架"
```
