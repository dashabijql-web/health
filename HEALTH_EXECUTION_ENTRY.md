# Health 最简执行入口

创建日期：2026-05-08
适用目录：`D:/Health`
定位：给 Hermes / Codex / Claude Code / 人工接手时看的第一屏入口。详细流程见 `D:/Health/HEALTH_ULTIMATE_FLOW.md`。

---

## 1. 先记住三件事

1. `D:/Health` 不是 git 仓库。
   - 前端仓库：`D:/Health/HealthShow`
   - 后端仓库：`D:/Health/HealthData`
   - git 操作必须分别在两个子仓库内执行。

2. 所有测试先确认数据源。
   - `old` = `health`：模拟器、演示、高数据量旧库。
   - `new` = `health_new`：真实手表、空库上线切换。
   - 新库员工、部门、设备为空是预期，不要当 bug。

3. 默认分工。
   - Hermes：总控、读文档、拆任务、归档、分诊、验收。
   - Codex：单 issue 最小修复、跑验证、交付 patch。
   - Claude Code：局部重构、大仓库理解、页面/代码结构整理。
   - PowerShell runner：实际跑 Windows 环境测试、前后端服务、SQL、Redis、Playwright、模拟器。

---

## 2. 每轮任务固定顺序

```text
读 AGENTS.md
  -> 读 HEALTH_EXECUTION_ENTRY.md
  -> 必要时读 HEALTH_ULTIMATE_FLOW.md
  -> 判断 old/new 数据源
  -> 分别检查两个子仓库工作树
  -> 跑环境 preflight
  -> 选择测试档位
  -> 执行修复或测试
  -> 归档产物
  -> 更新相关文档
```

---

## 3. 开工前必查

```powershell
git -C D:\Health\HealthShow status --short
git -C D:\Health\HealthData status --short
```

已有未提交改动默认视为用户内容，禁止无原因回滚、覆盖、删除。

---

## 4. 数据源判断

### 旧库 old

用于：模拟器、演示、高数据量旧库、页面非空验收。

旧库测试必须确认：

```powershell
cd D:/Health/HealthShow
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data
```

期望：

```text
data_source: old
expect_non_empty: true
failed: 0
```

### 新库 new

用于：真实手表、空库上线切换。

新库测试必须：

1. 切到新库
2. 停止模拟器
3. 确认请求头/响应头为 `X-Health-Data-Source: new`
4. 确认真实手表写入 `health_new`

停止模拟器：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
```

---

## 5. 常用启动命令

后端：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn spring-boot:run -f D:/Health/HealthData/pom.xml
```

前端：

```powershell
cd D:/Health/HealthShow
npm run dev
```

模拟器：

```powershell
cd D:/Health/HealthShow
python watch_tcp_simulator_1000.py
```

访问：

- 前端：`http://localhost:9528/`
- 后端：`http://localhost:8080/health`
- 登录：`admin / admin123`

---

## 6. 测试档位

### Smoke：小改后最快确认

前端纯逻辑/预警语义小改优先：

```powershell
cd D:/Health/HealthShow
npm run test:fast
```

接口 smoke 需要前后端服务已启动：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py
cd D:/Health/HealthShow
npm run audit:api
npm run audit:auth
```

### Standard：默认闭环

前端默认闭环：

```powershell
cd D:/Health/HealthShow
npm run test:frontend
```

跨前后端默认闭环：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py
cd D:/Health/HealthShow
npm run test:integration
```

### Full：接口、页面、双库、写入链路改动后

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn.cmd -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py
cd D:/Health/HealthShow
npm run test:full
```

---

## 7. 什么时候读详细文档

- 要判断全局流程：读 `HEALTH_ULTIMATE_FLOW.md`
- 要做自主测试闭环：读 `HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
- 要查测试细节：读 `HEALTH_TEST_METHOD.md`
- 要查工具链：读 `HEALTH_TEST_TOOLCHAIN_GUIDE.md`
- 要做目标态推进：读 `目标达成实施计划.md`
- 要做旧库专项：读 `旧库目标达成实施计划.md`
- 要改双库：读 `HealthData/DUAL_DB_CUTOVER.md`
- 要参考竞品驱动产品改进：读 `tests/competitor-improvement-plan.md`，证据见 `tests/runs/20260508-164000-competitor/competitor/findings.md`
- 要复用四代理自动化试跑经验：读 `HEALTH_FOUR_AGENT_AUTOMATION_RUN_20260508.md`
- 要排查 OpenClaw 接入问题：读 `OPENCLAW_HEALTH_INTEGRATION_DIAGNOSIS_20260508.md`
- 要查看测试流程整改与分层门禁：读 `HealthShow/HEALTHSHOW_TEST_FLOW_REMEDIATION_20260509.md`

---

## 8. 最后判断

如果任务很短：直接按本文件执行。

如果任务跨多个模块、多个数据源、多个代理：先读 `HEALTH_ULTIMATE_FLOW.md`，由 Hermes 做总控，再分发给 Codex 或 Claude Code。
