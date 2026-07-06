# Health 四代理自动化迭代试跑记录

日期：2026-05-08  
总控：Hermes  
目标：按 `HEALTH_EXECUTION_ENTRY.md` 跑一遍“软件开发、测试、迭代自动化”流程，并尝试同时调用 Claude Code、Codex、OpenClaw，形成 Health 下一轮 Phase 1 的可执行任务包。

---

## 1. 本轮目标

本轮不直接大规模改业务代码，先把四代理协作流程跑通，并围绕 Codex 竞品雷达给出的首选方向形成可执行方案。

选定方向：

- `tests/competitor-improvement-plan.md` Phase 1
- 主题：预警通知 SLA / 升级链路
- 主落点：
  - `/alert-management/notifications`
  - `/alert-management/records`
  - `/health-monitor/dashboard`

原因：

- 竞品雷达把它评为第一优先级。
- 价值高、风险中等。
- 比地图联动、视频证据、自由 dashboard 改造更适合第一刀。

---

## 2. 执行入口与约束

已参考：

- `D:/Health/HEALTH_EXECUTION_ENTRY.md`
- `D:/Health/tests/competitor-improvement-plan.md`
- `D:/Health/tests/runs/20260508-164000-competitor/competitor/findings.md`

关键约束：

- `D:/Health` 本身不是 git 仓库。
- 前端仓库：`D:/Health/HealthShow`
- 后端仓库：`D:/Health/HealthData`
- old = `health`，用于模拟器、演示、高数据量旧库。
- new = `health_new`，用于真实手表、空库上线切换。
- new 下员工、部门、设备为空是预期，不应误判为 bug。
- 当前两个子仓库已有大量未提交改动，本轮不做回滚、删除、覆盖。

---

## 3. 四代理调用结果

### 3.1 Hermes 总控

Hermes 完成：

1. 读取执行入口和竞品计划。
2. 确定本轮目标为 Phase 1：预警通知 SLA / 升级链路。
3. 检查代理工具可用性。
4. 尝试调用 Claude Code、Codex、OpenClaw。
5. 在外部代理受限时，使用只读子代理补齐 Claude Code 的大上下文架构分析角色。
6. 汇总成本轮可执行任务包。

### 3.2 Claude Code

检测结果：

- 命令存在：`claude`
- 版本：`2.1.42 (Claude Code)`

调用尝试：

- 尝试使用 `claude -p` 做只读架构分析。
- 命令被当前环境审批拦截，未执行成功。
- 因此未让 Claude Code 修改任何文件，也未产生业务代码改动。

本轮处理：

- 用 Hermes `delegate_task` 启动只读架构分析子代理，替代 Claude Code 的“大上下文架构分析”角色。
- 该子代理只读扫描了相关前后端文件，输出了 Phase 1 拆解。

结论：

- Claude Code 已安装，但当前会话执行外部 agent 命令受审批限制。
- 后续如果要充分使用 Claude Code，建议单独允许 `claude -p` 只读命令，或在终端手动运行我生成的任务提示词。

### 3.3 Codex

检测结果：

- 命令存在：`codex`
- 但运行时报错：

```text
Error: Missing optional dependency @openai/codex-linux-x64. Reinstall Codex: npm install -g @openai/codex@latest
```

结论：

- Codex 当前不可执行。
- 原因不是项目问题，而是 Codex CLI 安装缺少 Linux x64 optional dependency。
- 在 WSL 中需要重装或修复 Codex CLI 后才能作为执行代理。

建议修复命令：

```bash
npm install -g @openai/codex@latest
```

如果 npm 环境走 Windows Node / WSL Node 混用，建议统一在 WSL 环境内安装并确认：

```bash
which codex
codex --version
```

### 3.4 OpenClaw

检测结果：

- 命令存在：`openclaw`
- 版本：`OpenClaw 2026.4.15 (041266a)`

调用尝试：

- `openclaw --help` 成功。
- `openclaw agent --help` 超时。
- 尝试运行：

```bash
openclaw agent --message "只读任务..."
```

结果：

- 进程运行 87 秒无输出。
- 为避免挂起，已终止进程。

结论：

- OpenClaw CLI 可用，但 agent/gateway 侧可能未就绪、需要配置、或当前调用方式不适合本环境。
- 本轮没有拿到 OpenClaw 的有效分析输出。

后续建议先跑：

```bash
openclaw doctor
openclaw status
openclaw gateway status
openclaw agent --help
```

确认 gateway / provider / approvals 后，再作为执行代理接入。

---

## 4. 只读架构分析结论

### 4.1 前端涉及区域

主要页面：

- `HealthShow/src/views/alert-management/notifications/index.vue`
- `HealthShow/src/views/alert-management/notifications/notifications.scss`
- `HealthShow/src/views/alert-management/records/index.vue`
- `HealthShow/src/views/alert-management/records/records.scss`
- `HealthShow/src/views/health-monitor/dashboard/index.vue`
- `HealthShow/src/views/health-monitor/dashboard/components/DashboardWarningStream.vue`
- `HealthShow/src/views/health-monitor/dashboard/dashboard-runtime-data.js`
- `HealthShow/src/views/health-monitor/dashboard/dashboard-summary.js`
- `HealthShow/src/views/health-monitor/dashboard/dashboard-detail-methods.js`
- `HealthShow/src/api/risk-warning.js`

当前能力：

- 通知中心可以展示待处理预警、筛选、处理、批量处理、跳转人员画像和记录页。
- 预警记录页已有处理弹窗，且 UI 上已经出现“通知主管”开关。
- Dashboard 已有实时预警流和预警处理入口。

主要缺口：

- 没有显式 SLA 倒计时。
- 没有即将超时 / 已超时 / 已升级状态。
- 没有当前责任人、升级层级、首次响应时间。
- “通知主管”UI 暂未形成真实后端闭环。
- Dashboard 的 warning level 映射可能没有完整兼容“高危/中危/低危”等中文级别。

### 4.2 后端涉及区域

主要文件：

- `HealthData/src/main/java/com/xzkj/health/controller/RiskWarningController.java`
- `HealthData/src/main/java/com/xzkj/health/service/RiskWarningService.java`
- `HealthData/src/main/java/com/xzkj/health/mapper/RiskWarningMapper.java`
- `HealthData/src/main/java/com/xzkj/health/dto/riskwarning/RiskWarningItemView.java`
- `HealthData/src/main/java/com/xzkj/health/dto/riskwarning/RiskWarningOverviewView.java`
- Dashboard 相关 service / mapper

当前能力：

- `/risk-warning/list`
- `/risk-warning/overview`
- `/risk-warning/trend`
- `/risk-warning/dept-stats`
- `/risk-warning/handle/{id}`
- `/risk-warning/handle-batch`
- `/dashboard/warning-events`

当前数据库字段：

- `id`
- `user_code`
- `warning_type`
- `indicator_name`
- `indicator_value`
- `warning_level`
- `is_handled`
- `handle_time`
- `handle_by`
- `remark`
- `create_time`
- `update_time`

主要缺口：

- 缺 SLA 字段。
- 缺升级字段。
- 缺操作流水表。
- 缺 SLA summary 聚合接口。
- 分表场景下不能只靠 warning_id 关联日志，必须带 create_time 或 warning_month。

---

## 5. 推荐的最小落地切片

### Slice 1：SLA 可视化，不改数据库

目标：先让用户看到“哪些告警快超时 / 已超时”。

后端：

- 在 `/risk-warning/list` 返回中动态计算 SLA 字段。
- 不改库表。
- 根据 `create_time + warning_level` 推导：
  - `slaDeadline`
  - `remainingSeconds`
  - `overdueSeconds`
  - `slaStatus`
  - `slaStatusText`

前端：

- 通知中心列表显示 SLA tag、剩余时间、超时时间。
- 通知中心增加 SLA 筛选。
- Dashboard 预警流显示 SLA 状态。
- 修正 Dashboard 中文 warning level 映射。

优点：

- 风险最低。
- 不动数据库和分表。
- 可最快看到价值。

验收：

```powershell
cd D:/Health/HealthShow
npm run audit:api
npm run audit:e2e
npm run audit:pipeline-warning
npm run build
```

### Slice 2：新增 SLA summary 接口

目标：让通知中心和 Dashboard 有统一统计口径。

后端新增：

```text
GET /risk-warning/sla-summary
```

返回建议：

- `totalPending`
- `highPending`
- `dueSoon`
- `overdue`
- `escalated`
- `avgResponseMinutes`
- `handledRate`
- `maxOverdueMinutes`

前端：

- 通知中心顶部卡片改用 summary。
- Dashboard 调度行动卡接入 summary。

验收：

```powershell
cd D:/Health/HealthShow
npm run audit:api
npm run audit:e2e
npm run build
```

### Slice 3：升级动作最小闭环

目标：从“展示 SLA”推进到“可升级、可追踪”。

建议后端新增：

```text
POST /risk-warning/escalate/{id}
GET /risk-warning/timeline/{id}?createTime=...
```

建议新增表：

```text
warning_action_log
```

关键字段：

- `warning_id`
- `warning_create_time`
- `action_type`
- `operator`
- `target_user`
- `target_role`
- `remark`
- `action_time`
- `data_source`

前端：

- 通知中心增加“升级”按钮。
- 记录页详情抽屉显示时间线。
- Dashboard 对已升级事件做视觉标记。

风险：

- 分表 id 可能重复，日志必须带 `warning_create_time` 或 `warning_month`。

### Slice 4：确认 / 首次响应

目标：区分“确认收到”和“处理关闭”。

后端新增：

```text
POST /risk-warning/ack/{id}
```

前端：

- 通知中心增加“确认”按钮。
- 记录页显示首次响应时间。
- SLA 分成首次响应 SLA 与处理关闭 SLA。

### Slice 5：持久化 SLA/升级字段

目标：稳定产品化。

需要同步：

- `warning_record`
- `warning_record_YYYYMM`
- `v_warning_record`
- `sp_update_monthly_views`
- `sp_create_monthly_tables`
- Java DTO / Mapper / Service
- new 库建库脚本

建议放到 Phase 1.2，不作为第一刀。

---

## 6. 建议四代理分工

### Hermes

职责：

- 总控。
- 读取执行入口。
- 判断 old/new 数据源。
- 拆任务。
- 检查代理可用性。
- 汇总测试结果。
- 更新执行文档。

### Claude Code

最适合任务：

- 大上下文理解。
- 设计方案复核。
- 检查前后端链路是否漏文件。
- 复杂页面结构整理。

建议任务提示词：

```text
只读分析 D:/Health。参考 HEALTH_EXECUTION_ENTRY.md 和 tests/competitor-improvement-plan.md。
围绕 Phase 1 预警通知 SLA / 升级链路，检查 HealthShow 和 HealthData 现有相关实现。
输出：涉及文件、接口、DTO、分表风险、最小切片、验收命令。
禁止修改文件。
```

### Codex

最适合任务：

- 明确代码修改。
- 小 patch。
- 跑测试。
- 修失败。

但当前状态：

- Codex CLI 缺少 optional dependency，暂不可用。

修复后建议第一个 Codex 任务：

```text
在 D:/Health/HealthShow 和 D:/Health/HealthData 中实现 Slice 1：SLA 可视化，不改数据库。
要求：
1. 后端 /risk-warning/list 动态补充 slaStatus、slaDeadline、remainingSeconds、overdueSeconds。
2. 前端 notifications 显示 SLA tag 和剩余/超时时间。
3. Dashboard warning stream 显示 SLA 状态，并修复中文 warningLevel 映射。
4. 不修改数据库结构。
5. 跑 npm run audit:api、npm run audit:e2e、npm run audit:pipeline-warning、npm run build。
```

### OpenClaw

最适合任务：

- 作为第二代码执行代理。
- 或做独立 review / 产品化视角复核。
- 适合在 gateway 配好后做长任务。

但当前状态：

- CLI 存在。
- agent 命令调用没有产生输出，可能需要 gateway / provider / approvals 配置。

修复后建议 OpenClaw 任务：

```text
只读 review Codex 对 Slice 1 的改动。
检查：
1. 是否误改 old/new 数据源逻辑。
2. 是否误判 new 空数据。
3. 是否破坏现有 risk-warning handle/batch handle。
4. 是否遗漏 dashboard 中文 warningLevel 映射。
5. 是否需要补 audit 覆盖。
输出 review 结论，不直接改文件。
```

---

## 7. 推荐下一步

强烈建议下一轮只做 Slice 1，不要同时做数据库、时间线、升级表、dashboard 重构。

下一轮执行顺序：

1. 修复 Codex CLI：`npm install -g @openai/codex@latest`
2. 确认 OpenClaw agent/gateway 可用。
3. Hermes 生成 Slice 1 详细任务。
4. Codex 执行 Slice 1 小 patch。
5. Claude Code 做只读复核。
6. OpenClaw 做只读 review。
7. Hermes 跑验证并汇总。

---

## 8. 本轮结论

本轮已经验证了四代理流程的真实边界：

- Hermes 适合总控、拆解、沉淀流程。
- Claude Code 已安装，但当前被审批拦截，需允许只读调用才能发挥价值。
- Codex 当前安装损坏，需修复后才能承担代码执行。
- OpenClaw CLI 可用，但 agent/gateway 调用未成功，需要先做配置健康检查。

因此，本轮没有贸然改业务代码，而是产出了下一轮可执行的 Phase 1 任务包。这比在代理状态不稳定时直接让多个 agent 同时改代码更安全。
