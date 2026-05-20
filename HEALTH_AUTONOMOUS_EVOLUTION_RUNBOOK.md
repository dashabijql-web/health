# Health 自主测试与自我进化总流程

创建日期：2026-05-08  
适用目录：`D:/Health`  
定位：取代分散的测试闭环、工具链、Hermes/Codex 协作和新库/旧库测试文档，作为后续 Health 自主测试与持续优化的第一执行入口。

---

## 0. 一句话目标

让 Health 项目形成一条低人工干预闭环：

```text
自动确认环境
  -> 自动选择测试档位
  -> 自动跑测试并归档
  -> 自动分诊失败
  -> 自动生成单 issue
  -> Codex 定点修复
  -> 自动回归
  -> 自动研究竞品和最佳实践
  -> 自动生成优化候选
  -> 通过安全门禁后进入下一轮
```

最终目标不是“自动乱改”，而是让系统持续发现问题、提出可验证改进、做最小安全修改，并用测试证明结果。

---

## 1. 本文件取代哪些执行入口

后续新开任务时，优先读：

1. 当前代码与配置
2. `D:/Health/AGENTS.md`
3. 本文件：`D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`

下列文件保留为参考，不再作为第一执行入口：

- `D:/Health/HEALTH_CODEX_TEST_CLOSED_LOOP_RUNBOOK.md`
- `D:/Health/HEALTH_TEST_METHOD.md`
- `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md`
- `D:/Health/HERMES_CODEX_HEALTH_EXECUTION_PLAN.md`
- `D:/Health/目标达成实施计划.md`
- `D:/Health/旧库目标达成实施计划.md`
- `D:/Health/docs/archive/历史归档-HEALTH_HANDOFF.md`
- `D:/Health/docs/archive/历史归档-治理清单.md`
- `D:/Health/docs/archive/历史归档-CODEX_MULTI_DAY_TASKBOOK.md`

如果旧文档和本文件冲突，以当前代码、`AGENTS.md` 和本文件为准。

---

## 2. 角色结论：Hermes 和 OpenClaw 要不要参与

### 2.1 当前结论

要参与，但分阶段参与，不能一开始就让它们接管所有事情。

| 组件 | 当前定位 | 是否作为主 runner | 原因 |
| --- | --- | --- | --- |
| PowerShell | 稳定测试执行器 | 是 | Health 依赖 Windows 本地前端、后端、SQL Server、Redis、TCP、模拟器 |
| Codex | 修复执行器 | 否 | 负责定位、改代码、跑验证，不负责长期无人值守调度 |
| Hermes | 测试编排、归档、分诊、issue 生成 | 暂不直接跑长任务 | 适合做计划和总结，不适合当前环境下直接长时间控制 Windows 命令 |
| OpenClaw | 外部入口、通知、多 Agent 编排、竞品雷达入口 | 否 | 适合作为协作层，不应直接承担 Health 主测试稳定性 |
| Playwright | 页面执行和截图 | 是，针对浏览器任务 | 负责真实浏览器验证 |
| SQL/Redis MCP | 只读核对 | 否 | 只做状态核对，不直接改业务数据 |

### 2.2 推荐成熟路线

```text
阶段 1：PowerShell runner 固化
阶段 2：Hermes 生成计划、归档、issue 包
阶段 3：Codex 自动处理单 issue
阶段 4：OpenClaw 负责通知和任务入口
阶段 5：竞品雷达定期生成优化候选
阶段 6：低风险优化自动落地，高风险优化等待人工批准
```

不要跳过前两阶段直接做“全自动自我进化”。否则系统会把环境问题、数据源误判、测试 flaky 和真实 bug 混在一起。

---

## 3. 不可突破的安全边界

### 3.1 仓库边界

- `D:/Health` 不是 git 仓库。
- `D:/Health/HealthShow` 是前端独立 git 仓库。
- `D:/Health/HealthData` 是后端独立 git 仓库。
- 所有 `git status`、`git diff`、提交、回滚必须分别在两个子仓库执行。

### 3.2 自动化禁止事项

任何自动化流程都不能做：

- 在 `D:/Health` 根目录执行 git 修改、提交、回滚。
- 使用 `git reset --hard`、`git clean`、`git checkout --` 丢弃用户改动。
- 删除或弱化测试断言来通过测试。
- 未经确认修改数据库结构、清空业务数据、批量写生产数据。
- 未经确认停止不是本轮创建的长期服务。
- 把多个根因混成一个 Codex 修复任务。
- 让 OpenClaw 或 Hermes 在当前环境下直接长时间接管 Windows runner。
- 把真实员工、设备、健康数据上传给外部模型或竞品分析服务。
- 自动部署到生产环境。

### 3.3 自动修改分级

| 级别 | 自动化可做 | 是否需要人工 |
| --- | --- | --- |
| L0 文档、日志、summary | 可自动改 | 不需要 |
| L1 测试脚本、runner、非生产配置 | 可自动改并回归 | 通常不需要 |
| L2 前端展示 bug、小范围样式、空态文案 | 可自动改并截图验证 | 失败或大面积影响时需要 |
| L3 后端 service/mapper/query 逻辑 | 可生成 patch，但要完整回归 | 建议人工 review |
| L4 数据库结构、真实手表路由、鉴权策略、安全策略 | 只生成方案和 issue | 必须人工批准 |
| L5 生产部署、数据清理、权限变更 | 禁止自动执行 | 必须人工执行 |

---

## 4. 数据源规则是所有测试的前置条件

Health 有两个业务数据源：

| 数据源 | 数据库 | 用途 | 测试口径 |
| --- | --- | --- | --- |
| old | `health` | 模拟器、演示、高数据量历史库 | 核心组件应非空 |
| new | `health_new` | 真实手表、空库上线切换 | 业务数据为空可能是预期 |

每轮测试必须记录：

- 前端顶栏当前显示：新库 / 老库。
- Cookie：`Health-Data-Source`。
- 请求头：`X-Health-Data-Source`。
- 响应头：`X-Health-Data-Source`。
- Redis key：`health:buffer:old` / `health:buffer:new`。
- SQL 库：`health` / `health_new`。
- pipeline 写入的表名，例如 `health_record_202605`。

固定操作顺序：

- 验真实手表或新库空态：先切新库，再停模拟器。
- 验模拟器、旧库页面密度、旧库 pipeline：先切老库，再开模拟器。

---

## 5. 测试档位

### 5.1 Preflight：环境预检

任何测试档位都先跑：

```powershell
$PSVersionTable.PSVersion
node -v
npm -v
python --version
D:/apache-maven-3.8.1/bin/mvn.cmd -v

Get-NetTCPConnection -LocalPort 8080,9000,9528 -State Listen -ErrorAction SilentlyContinue |
  Select-Object LocalAddress,LocalPort,OwningProcess

Invoke-WebRequest -Uri 'http://localhost:8080/health' -Method Head -TimeoutSec 5
Invoke-WebRequest -Uri 'http://localhost:9528/' -Method Head -TimeoutSec 5
```

如果前端、后端、TCP、Redis、SQL Server 任一关键依赖不可用，本轮直接标记 `blocked`，不要继续跑业务测试。

### 5.2 Smoke：最快有价值检查

适合每次小改后运行：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
npm run audit:api
npm run audit:auth
```

### 5.3 Standard：标准闭环

适合常规自动闭环，也是默认档位：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
npm run audit:api
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
```

### 5.4 Full：发布前或较大改动

适合接口、页面、双库、写入链路改动后运行：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
D:/apache-maven-3.8.1/bin/mvn.cmd -q test -f D:/Health/HealthData/pom.xml
python D:/Health/HealthData/scripts/run_backend_regression.py

cd D:/Health/HealthShow
npm run audit:structure
npm run audit:api
$env:API_DATA_SOURCE='old'
$env:API_EXPECT_NON_EMPTY='1'
npm run audit:data
npm run audit:write
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
npm run audit:pipeline-warning
npm run build
```

### 5.5 Dual-DB：双库专项

适合改数据源、请求头、Cookie、写入路由、模拟器识别规则后运行：

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

必须额外核对：

- old 请求返回 old 响应头。
- new 请求返回 new 响应头。
- 模拟器 IMEI 写 old。
- 真实手表默认写 new。
- Redis buffer old/new 分离。

### 5.6 UI-Product：页面产品化专项

适合做页面美观度、交互和竞品对标后运行：

```powershell
cd D:/Health/HealthShow
npm run audit:e2e
npm run build
```

再用 Playwright 截图覆盖：

- `1440x900`
- `1920x1080`
- `390x844`
- `414x896`

必测页面：

- `/safety-command/index`
- `/health-monitor/dashboard`
- `/health-monitor/workbench`
- `/health-monitor/real-time`
- `/health-monitor/risk-warning`
- `/health-monitor/employee-archive`
- `/health-monitor/report-center`
- `/ai-chat/index`
- `/admin/device-list`
- `/admin/user-list`
- `/admin/role`

### 5.7 Nightly：夜间长期回归

适合无人值守，但必须先让 Standard 连续稳定：

```powershell
cd D:/Health/HealthShow
npm run audit:nightly
```

Nightly 不自动改代码，只生成 summary 和 issue 候选。

---

## 6. 统一产物目录

每轮必须创建唯一 run-id：

```text
YYYYMMDD-HHMMSS-<profile>
```

例如：

```text
20260508-230000-standard
20260508-233000-full
20260509-020000-nightly
```

目录结构：

```text
D:/Health/tests/runs/<run-id>/
  summary.md
  summary.json
  logs/
  issues/
  artifacts/
  screenshots/
  videos/
  competitor/
  patches/
```

索引文件：

```text
D:/Health/tests/latest-run.json
D:/Health/tests/open-issues.json
D:/Health/tests/evolution-backlog.json
D:/Health/tests/competitor-index.json
```

---

## 7. PowerShell Runner 标准

后续应实现一个统一 runner：

```text
D:/Health/tests/run-health-loop.py
```

建议参数：

```powershell
param(
  [ValidateSet('preflight','smoke','standard','full','dual-db','ui-product','nightly')]
  [string]$Profile = 'standard',

  [ValidateSet('old','new','both','auto')]
  [string]$DataSource = 'auto',

  [switch]$AllowFix,
  [switch]$AllowCompetitorResearch,
  [switch]$AllowOpenClawNotify
)
```

Runner 必须做到：

- 每条命令独立 timeout。
- stdout/stderr 合并成 `*.combined.log`。
- 记录命令、exit code、开始时间、结束时间、耗时。
- 失败后继续生成 summary。
- 不并行运行 `audit:auth` 和任何 e2e/pipeline。
- 不在根目录执行 git 修改命令。
- 如果环境阻塞，停止后续业务测试并标记 `blocked`。

---

## 8. Summary 标准结构

`summary.json` 最少包含：

```json
{
  "run_id": "20260508-230000-standard",
  "profile": "standard",
  "status": "passed",
  "started_at": "2026-05-08T23:00:00+08:00",
  "finished_at": "2026-05-08T23:20:00+08:00",
  "environment": {
    "frontend_url": "http://localhost:9528/",
    "backend_url": "http://localhost:8080/health",
    "runner": "Windows PowerShell",
    "java_home": "C:/Program Files/Java/jdk-17"
  },
  "data_source": {
    "requested": "old",
    "cookie": "old",
    "request_header": "old",
    "response_header": "old",
    "redis_key": "health:buffer:old",
    "sql_db": "health"
  },
  "git_status": {
    "HealthShow_before": "logs/git-before-healthshow.log",
    "HealthData_before": "logs/git-before-healthdata.log",
    "HealthShow_after": "logs/git-after-healthshow.log",
    "HealthData_after": "logs/git-after-healthdata.log"
  },
  "commands": [],
  "issues": [],
  "competitor_findings": [],
  "fixes": [],
  "next_actions": []
}
```

`summary.md` 必须让人 1 分钟内看懂：

- 本轮状态。
- 跑了哪些命令。
- 哪些通过、哪些失败。
- 数据源到底是哪一个。
- 产物在哪里。
- 如果失败，最高优先级 issue 是哪个。
- 如果修复，改了什么、怎么验证。
- 是否有竞品发现和优化候选。

---

## 9. 失败分诊与 issue 包

### 9.1 优先级

多个失败只处理一个最高优先级 issue：

1. 环境阻塞：前端/后端/SQL/Redis/TCP 不可用。
2. 鉴权失败：`audit:auth`。
3. 写入链路失败：`audit:pipeline` / `audit:write` / `pipeline-warning`。
4. 双库错路由：old/new 请求或写入混淆。
5. API 大面积失败：`audit:api`。
6. 页面基础失败：`audit:e2e`。
7. 构建失败：`npm run build`。
8. 数据密度失败：`audit:data`。
9. UI 质量问题。
10. 性能预算超限。

### 9.2 issue json

```json
{
  "issue_id": "healthshow-auth-20260508-001",
  "project": "HealthShow",
  "category": "auth",
  "severity": "high",
  "data_source": "old",
  "title": "后端重启后旧 token 未正确收敛到登录页",
  "run_id": "20260508-230000-standard",
  "failing_command": "cd D:/Health/HealthShow && npm run audit:auth",
  "exit_code": 1,
  "expected": "旧 token 失效后清理 cookie 并回到登录页",
  "actual": "并发 401 后页面停留在受保护路由",
  "failure_signature": "auth:concurrent-401:not-login",
  "evidence": {
    "log_files": [],
    "artifacts": [],
    "screenshots": []
  },
  "suspected_files": [],
  "constraints": [
    "只修复此 issue",
    "不要顺手重构",
    "不要删除或弱化测试断言",
    "不要修改生成物",
    "HealthShow 和 HealthData 分别检查 git status"
  ],
  "verification_commands": [],
  "status": "open"
}
```

---

## 10. Codex 自动修复流程

Codex 只能吃一个 issue 包。

固定步骤：

1. 读 `AGENTS.md`。
2. 读本文件。
3. 读 issue json/md。
4. 在对应子仓库记录 `git status --short`。
5. 复现失败命令。
6. 读失败日志、测试脚本、相关源码。
7. 做最小修改。
8. 跑 issue 指定 verification command。
9. 高风险链路补跑相关回归。
10. 更新 issue 状态和 run summary。

高风险链路必须补回归：

| 改动范围 | 补跑 |
| --- | --- |
| 登录、Cookie、权限 | `audit:auth` + `audit:e2e` |
| 数据源切换 | `dual-db` profile |
| TCP / Redis / 手表写入 | `audit:pipeline` + `pipeline-warning` |
| Dashboard / real-time / safety-command | `audit:api` + `audit:e2e` + 截图 |
| Mapper / SQL / service | Maven test + backend regression + API smoke |
| 页面结构 | `audit:structure` + `audit:e2e` + build |

---

## 11. 竞品雷达：让 Health 自己找优化方向

### 11.1 目标

竞品雷达不是抄界面，而是持续收集行业产品能力和交互模式，转成可验证的 Health 优化候选。

目标输出：

```text
竞品/参考产品
  -> 可观察能力
  -> Health 当前差距
  -> 可测试验收标准
  -> 风险和成本
  -> 优先级
  -> issue / backlog
```

### 11.2 竞品来源

优先公开、合规、稳定来源：

- 智慧矿山 / 安全生产平台公开资料。
- 可穿戴健康监测平台公开资料。
- 企业健康管理 SaaS 产品公开资料。
- 工业 IoT 设备管理平台公开资料。
- EHS / HSE / 安全预警平台公开资料。
- 官方产品页面、白皮书、帮助中心、公开演示视频、公开截图。

禁止：

- 绕过登录抓取非公开系统。
- 抓取含个人信息或企业敏感数据的页面。
- 复制受版权保护的整套页面、图形、文案。
- 把 Health 内部真实数据发给外部竞品分析服务。

### 11.3 竞品雷达档位

| 档位 | 频率 | 输出 |
| --- | --- | --- |
| quick | 每周 | 3-5 条低风险优化候选 |
| standard | 每两周 | 竞品能力矩阵 + backlog |
| deep | 每月 | 重点页面对标、截图、交互拆解、验证计划 |

### 11.4 竞品发现流程

```text
1. 读取 Health 当前页面和功能清单
2. 生成关键词
3. 搜索公开资料
4. 记录来源 URL、日期、截图或摘要
5. 抽取能力点
6. 映射到 Health 页面
7. 生成优化候选
8. 去重、打分、排序
9. 写入 evolution-backlog.json
10. 只对低风险候选生成 issue
```

关键词示例：

- 智慧矿山 安全监测 平台
- 矿工 健康监测 手环 管理系统
- 工业安全 指挥中心 大屏
- worker safety wearable dashboard
- EHS incident alert dashboard
- connected worker health monitoring
- IoT device fleet management dashboard

### 11.5 竞品能力矩阵

```json
{
  "finding_id": "competitor-20260508-001",
  "source": {
    "name": "公开产品或资料名",
    "url": "https://example.com",
    "accessed_at": "2026-05-08T23:00:00+08:00"
  },
  "capability": "预警处理闭环中展示责任人、处置时限和升级状态",
  "health_gap": "当前通知列表有处理动作，但缺少 SLA 和升级视图",
  "mapped_pages": [
    "/alert-management/notifications",
    "/health-monitor/dashboard"
  ],
  "value": "high",
  "risk": "medium",
  "effort": "medium",
  "testability": "high",
  "acceptance": [
    "待处理预警显示处置时限",
    "超时预警进入升级状态",
    "dashboard 显示超时待办数量"
  ],
  "status": "candidate"
}
```

### 11.6 优化候选评分

每个候选按 100 分排序：

| 维度 | 权重 |
| --- | ---: |
| 现场价值 | 30 |
| 可测试性 | 20 |
| 实现成本低 | 15 |
| 风险低 | 15 |
| 与 Health 当前目标匹配 | 15 |
| 竞品证据质量 | 5 |

自动进入 issue 的条件：

- 总分 >= 75。
- 风险不高于 medium。
- 不涉及数据库结构或生产安全策略。
- 有明确验收命令或截图验证方式。

高风险候选只进入 backlog，不自动改代码。

---

## 12. 自我进化循环

### 12.1 每轮循环

```text
Observe
  收集测试结果、日志、慢接口、页面截图、用户反馈、竞品发现

Orient
  分类：bug、性能、体验、功能缺口、测试缺口、文档漂移

Decide
  只选择一个最高价值、可验证、低风险 issue

Act
  Codex 最小修改

Verify
  跑对应 verification + 必要回归

Record
  summary、issue 状态、backlog、风险记录
```

### 12.2 每轮只允许一个主问题

不要一轮同时修：

- 登录问题
- dashboard 性能
- 页面样式
- SQL mapper
- 竞品功能

一轮一个主问题，修完再进下一轮。

### 12.3 自动进入下一轮的条件

必须同时满足：

- 当前 run status 是 `passed` 或 `fixed`。
- 没有 blocker/high open issue。
- 本轮修改文件数低于阈值，例如小于 12。
- 验证命令全部通过。
- 没有触碰 L4/L5 禁区。

否则停止，等待人工。

---

## 13. 性能与观测

每轮 Full 或 Nightly 应记录：

- 慢接口数量。
- 慢 SQL 数量。
- `health.sql.slow.total`。
- `health.buffer.queue.size`。
- `health.buffer.dead_letter.total`。
- `health.datasource.request.total`。
- `health.datasource.watch.route.total`。
- `health.warning.generated.total`。
- `health.ai.reject.total`。

性能预算：

| 接口类型 | 目标 |
| --- | ---: |
| KPI / overview | 800ms 内 |
| 页面首屏核心接口 | 1200ms 内 |
| 历史记录分页 | 1500ms 内 |
| 跨月重统计 | 2500ms 内且需要缓存 |
| 人工导出 | 可慢，但必须有反馈 |

超过预算的接口进入 `evolution-backlog.json`。

---

## 14. 页面产品化检查

自动化只判断明确问题：

- 白屏。
- 控制台 error。
- API 失败后页面伪装成暂无数据。
- 图表容器 0 宽高。
- 元素重叠、遮挡、横向溢出。
- 按钮、弹窗、抽屉、分页、筛选不可用。
- 移动端布局塌陷。
- 关键数据卡片为空且没有合理空态。

主观审美不能直接自动判定。竞品对标只能生成候选和截图证据，不直接说“必须照抄”。

---

## 15. OpenClaw 的正确接入方式

OpenClaw 不做主 runner。它适合做：

- 外部消息入口：例如“跑 standard”、“跑 full”、“查看 latest summary”。
- 测试完成通知。
- open issue 推送。
- 触发竞品雷达。
- 未来多 Agent 对比：Codex 修复、Claude review、Hermes 分诊。

OpenClaw 接入前必须满足：

- `openclaw --version` 稳定。
- `openclaw status` 稳定。
- channel health 稳定。
- 能把任务转给本地 PowerShell runner，而不是自己长时间跑命令。

建议消息格式：

```json
{
  "task": "run-health-loop",
  "profile": "standard",
  "data_source": "old",
  "allow_fix": false,
  "allow_competitor_research": false
}
```

---

## 16. Hermes 的正确接入方式

Hermes 适合做：

- 根据本文件生成测试计划。
- 为每轮生成 run-id。
- 读取 runner 输出并生成 summary。
- 从失败日志生成单 issue 包。
- 给 Codex 分发单 issue。
- 修复后调度回归。
- 汇总多轮趋势。

Hermes 暂不适合做：

- 直接长时间运行 Windows PowerShell 测试。
- 在 WSL 里用 `localhost` 判断 Windows 服务状态。
- 直接修改 Health 生产代码。
- 直接决定高风险优化自动落地。

推荐模式：

```text
Hermes 生成计划
  -> PowerShell runner 执行
  -> Hermes 解析结果
  -> Hermes 生成 issue
  -> Codex 修复
  -> PowerShell runner 验证
  -> Hermes 汇总
```

---

## 17. 人工介入点

目标是少人工，不是零人工。

必须人工介入：

- 数据库结构调整。
- 安全策略调整。
- 真实手表写库规则调整。
- 生产部署。
- 大范围 UI 方向改变。
- 竞品启发的新功能超过 2 天工作量。
- 自动化连续两轮失败。
- 同一 issue 修复后复发。
- 测试结论和现场观察冲突。

可以不人工介入：

- 文档归档。
- summary 生成。
- 临时日志清理。
- 单个测试脚本等待条件修复。
- 小范围页面溢出/空态修复。
- 低风险接口返回字段兼容。

---

## 18. 最终目标状态

Health 达到自主进化目标时，应具备：

1. 一条稳定 PowerShell runner。
2. 每轮测试都有 run-id、日志、summary、issue。
3. old/new 数据源证据自动记录。
4. 失败能自动分诊成单 issue。
5. Codex 能按 issue 最小修复并验证。
6. Hermes 能生成计划、汇总、分诊和趋势报告。
7. OpenClaw 能接收外部任务和推送结果。
8. 竞品雷达能定期生成优化候选。
9. 低风险候选能自动进入实现和验证。
10. 高风险候选必须进入人工审批。
11. 项目每轮进化都有测试证据，不靠主观感觉。

---

## 19. 下一步落地顺序

### Step 1：写统一 runner

创建：

```text
D:/Health/tests/run-health-loop.py
```

先支持：

- `preflight`
- `standard`
- `full`

### Step 2：自动解析 summary

让 runner 自动解析：

- `audit:api`
- `audit:auth`
- `audit:e2e`
- `audit:pipeline`
- `audit:data`

### Step 3：自动生成 issue 包

只生成最高优先级一个 issue。

### Step 4：Codex issue 修复闭环

实现：

```text
issue -> Codex -> patch -> verification -> summary update
```

### Step 5：竞品雷达只读模式

先只生成：

```text
tests/runs/<run-id>/competitor/findings.md
tests/evolution-backlog.json
```

不自动改代码。

### Step 6：低风险优化自动落地

只允许 L0-L2 自动落地，且必须跑验证。

### Step 7：OpenClaw 通知入口

只做：

- 开始通知
- 结束通知
- latest summary
- open issue 列表

不要让 OpenClaw 直接跑 Health 长任务。

---

## 20. 推荐给下一轮 Codex 的启动提示

```text
你现在在 D:/Health 执行 Health 自主测试与自我进化流程。

第一入口：
- D:/Health/AGENTS.md
- D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md

要求：
1. 不在 D:/Health 根目录做 git 操作。
2. HealthShow 和 HealthData 分别检查 git 状态。
3. 根据任务选择 preflight/smoke/standard/full/dual-db/ui-product/nightly 档位。
4. 所有产物写入 D:/Health/tests/runs/<run-id>/。
5. 每轮只处理一个最高优先级 issue。
6. 不删除或弱化测试断言。
7. 不自动执行 L4/L5 高风险操作。
8. 如果启用竞品雷达，只使用公开资料，只生成候选，不复制竞品实现。
9. 最终输出 run_id、状态、命令结果、issue、修复、验证、残余风险。
```



