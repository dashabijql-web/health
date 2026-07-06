# Health 终极执行总流程

创建日期：2026-05-08
适用范围：`D:/Health`
用途：把当前项目的代码结构、测试闭环、双库规则、工具分工、验收标准和交接方式统一成一份主流程文档。

---

## 0. 这份文档是什么

这是 `D:/Health` 的“终极流程”总入口。

它的目标不是替代所有历史文档，而是把当前最稳定、最可执行、最少歧义的做法统一起来，避免后续每次都重新判断：

- 该读哪份文档
- 该用老库还是新库
- 该让谁执行：我、Codex、Claude Code、还是 PowerShell runner
- 该先跑哪些验证
- 该如何验收和归档

如果这份文档与当前代码、`AGENTS.md`、或仓库内更近的事实冲突，以“当前代码 + `AGENTS.md` + 最新验证产物”为准。

---

## 1. 目录边界

### 1.1 工作区不是单一 git 仓库

`D:/Health` 根目录不是 git 仓库。

真正的两个子仓库是：

- `D:/Health/HealthShow`：前端
- `D:/Health/HealthData`：后端

任何 `git status`、`git diff`、提交、回滚，都必须在对应子仓库内分别执行。

### 1.2 当前技术栈

前端：

- Vue 3
- Vite 5
- Vuex 4
- Vue Router 4
- Element Plus
- ECharts

后端：

- Spring Boot 3.2.12
- Java 17
- MyBatis-Plus
- Sa-Token
- Redis
- Netty
- SQL Server

---

## 2. 一句话总目标

让 Health 项目持续形成一个安全、可验证、可回归、可迭代的闭环：

1. 自动确认环境
2. 自动识别数据源
3. 自动跑对应测试档位
4. 自动归档结果
5. 自动分诊失败
6. 交给 Codex 做最小修复
7. 自动回归验证
8. 必要时生成下一轮优化候选
9. 保持 old/new 双库语义稳定


---

## 3. 文档优先级

后续新任务，默认按这个顺序看：

1. 当前代码和配置
2. `D:/Health/AGENTS.md`
3. 本文件：`D:/Health/HEALTH_ULTIMATE_FLOW.md`
4. `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md`
5. `D:/Health/HEALTH_TEST_METHOD.md`
6. `D:/Health/HEALTH_TEST_TOOLCHAIN_GUIDE.md`
7. `D:/Health/目标达成实施计划.md`
8. `D:/Health/旧库目标达成实施计划.md`
9. `D:/Health/项目重新评估结果.md`
10. `D:/Health/旧库项目重新评估结果.md`
11. `D:/Health/HealthData/DUAL_DB_CUTOVER.md`
12. `D:/Health/HealthShow/TECHNICAL_DOCS.md`

历史归档继续保留，但不作为第一执行入口。

---

## 4. 当前项目地图

### 4.1 前端主目录

`D:/Health/HealthShow/src` 下主要是：

- `router/`：路由事实源
- `layout/`：框架布局、导航、侧边栏、移动端底栏
- `views/`：业务页面
- `api/`：前端接口封装
- `utils/`：请求、数据源、图表、导出、鉴权、工具函数
- `composables/`：副作用、计时器、滚动等复用逻辑

典型重点页面：

- `src/views/health-monitor/dashboard/`
- `src/views/health-monitor/real-time/`
- `src/views/health-monitor/workbench/`
- `src/views/health-monitor/risk-warning/`
- `src/views/health-monitor/employee-archive/`
- `src/views/health-monitor/employee-profile/`
- `src/views/health-monitor/report-center/`
- `src/views/health-monitor/trend-warning/`
- `src/views/safety-command/`
- `src/views/ai-chat/`
- `src/views/alert-management/*`
- `src/views/device-management/`
- `src/views/user-list/`
- `src/views/role-management/`
- `src/views/org-management/*`

### 4.2 后端主目录

`D:/Health/HealthData/src/main/java/com/xzkj/health` 下主要是：

- `config/`：数据源、异步、Netty、安全、MyBatis
- `ai/`：AI 聊天、AI 报告、SQL 守卫、Schema
- `mapper/`：查询和写入映射
- `dto/`：typed row / view / response 结构
- `controller/`：接口入口
- `service/`：业务编排
- `handler/`：设备协议、数据处理

典型重点域：

- Dashboard
- Realtime
- Health Portrait
- Trend Warning
- Statistics
- AI Chat / AI Report
- Risk Warning
- Device Management
- Org / User / Role 管理

---

## 5. 双库规则：这是所有测试的前置条件

Health 现在有两个业务数据源：

| 数据源 | 数据库 | 用途 |
| --- | --- | --- |
| old | `health` | 模拟器、演示、高数据量历史库 |
| new | `health_new` | 真实手表、空库切换、上线切换 |

### 5.1 核心事实

- 直接 HTTP 请求如果不带头，后端默认走 `old`
- 前端开发环境默认会显式请求某一数据源
- 顶栏切换“数据源”时，会写入 `X-Health-Data-Source`
- 后端响应头也会回写 `X-Health-Data-Source`
- 手表数据默认写 `new`
- 模拟器默认写 `old`
- Redis 缓冲已拆分为：
  - `health:buffer:old`
  - `health:buffer:new`

### 5.2 真实操作顺序

验真实手表或新库空态：

1. 先切到 `new`
2. 再停模拟器
3. 再验真实手表写入和空库页面

验模拟器或旧库页面密度：

1. 先切到 `old`
2. 再开模拟器
3. 再验旧库页面密度和写入闭环

### 5.3 新库当前状态

按当前事实：

- `department = 0`
- `employee = 0`
- `device = 0`
- `device_user = 0`
- `realtime_data = 0`
- `user_online_status = 0`
- `job_type = 12`
- `alert_config = 20`
- `sys_user = 1`
- 保留登录账号：`admin / admin123`

因此，`new` 下员工、部门、设备为空是预期行为，不应误判为 bug。

---

## 6. 工具分工：我、Codex、Claude Code、PowerShell

### 6.1 我适合做什么

我更适合做“总控”和“编排”：

- 读项目和文档
- 判断该跑哪个档位
- 拆分任务
- 选择谁执行更合适
- 汇总结果
- 统一验收
- 生成 issue 包、计划、总结、终极流程

### 6.2 Codex 适合做什么

Codex 更适合做“代码执行者”：

- 读 issue 包
- 定位根因
- 做最小 patch
- 跑验证命令
- 输出修改文件和验证结果

尤其适合：

- 单 issue 修复
- 明确的代码修改
- 需要连续执行多轮验证的工程任务

### 6.3 Claude Code 适合做什么

Claude Code 适合做“代码理解和局部开发执行者”：

- 大仓库理解
- 局部重构
- 终端开发任务
- 文件拆分、页面整理、逻辑梳理

### 6.4 PowerShell runner 适合做什么

PowerShell runner 是当前 Health 测试闭环里最稳定的实际执行层：

- 跑前端 / 后端测试
- 跑 Playwright / audit 脚本
- 跑模拟器
- 跑 SQL / Redis / TCP 相关验证
- 生成实际产物

### 6.5 简单决策

- 单点修复：直接给 Codex
- 大段重构：可给 Claude Code
- 多步骤、要统筹、要验收：先给我
- 实际跑测试和现场验证：PowerShell runner

---

## 7. 标准工作流

### 7.1 Intake：接任务

先确认这四件事：

1. 目标是什么
2. 属于前端、后端、双库、测试、文档、还是跨域任务
3. 数据源是 old 还是 new
4. 是否会影响用户已有未提交改动

### 7.2 Discover：先看事实

每次动手前先看：

- 当前代码
- 当前配置
- 当前工作树状态
- 当前日志
- 当前测试产物

不要只凭记忆或历史文档判断。

### 7.3 Preflight：先过环境

至少检查：

```powershell
$PSVersionTable.PSVersion
node -v
npm -v
python --version
D:/apache-maven-3.8.1/bin/mvn.cmd -v
Get-NetTCPConnection -LocalPort 8080,9000,9528 -State Listen -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess
```

如果前端、后端、TCP、Redis、SQL Server 任一关键依赖不可用，本轮应标记 blocked，不要硬跑业务测试。

### 7.4 Classify：选测试档位

#### Smoke

适合小改动后快速确认：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py
cd D:/Health/HealthShow
npm run audit:api
npm run audit:auth
```

#### Standard

默认常规闭环：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
python D:/Health/HealthData/scripts/run_backend_regression.py
cd D:/Health/HealthShow
npm run audit:api
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
```

#### Full

适合接口、页面、双库、写入链路改动：

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

#### Dual-DB

适合双库切换、数据源路由、模拟器/真手表验收：

- 先确认前端顶栏数据源
- 再确认 Cookie / 请求头 / 响应头
- 再确认实际写入库名
- 再确认模拟器是否只写 old
- 再确认真实手表是否只写 new

---

## 8. 目前已经固化的稳定脚本

前端 `D:/Health/HealthShow/package.json` 中，当前关键脚本是：

- `npm run audit:structure`
- `npm run audit:api`
- `npm run audit:data`
- `npm run audit:write`
- `npm run audit:auth`
- `npm run audit:e2e`
- `npm run audit:pipeline`
- `npm run audit:pipeline-warning`
- `npm run build`
- `npm run dev`

后端关键脚本与入口：

- `mvn -q test -f D:/Health/HealthData/pom.xml`
- `python D:/Health/HealthData/scripts/run_backend_regression.py`
- `python D:/Health/HealthData/scripts/probe_redis_buffer_flush.py`

前端常用入口：

- `http://localhost:9528/`
- 后端：`http://localhost:8080/health`

---

## 9. 任务执行原则

### 9.1 一次只做一个主题

不要把下面这些混成一个批次：

- 后端 mapper 重构
- 前端页面视觉重构
- 双库规则调整
- AI 服务改造
- 测试脚本改造

### 9.2 先验证，再继续

每改完一小步，先跑对应验证。

不要先写总结，再补验证。

### 9.3 不回滚用户已有工作

当前工作树里已有大量未提交改动，默认视为用户内容。

修改时：

- 优先兼容
- 不无原因回滚
- 不大范围覆盖
- 不删除已有验证产物

### 9.4 不做危险操作

默认禁止：

- `git reset --hard`
- `git clean`
- 未确认的批量删除
- 未确认的数据库清理
- 未确认的生产部署
- 直接改真实手表/真实库关键规则而不留证据

---

## 10. 终极验收标准

### 10.1 代码层

- 核心 controller 不再直连 mapper
- 高频接口不再继续裸暴露 `Map<String,Object>`
- broad catch 不再把原始异常消息随意拼前端
- Dashboard、Realtime、Health Portrait、Trend Warning、Statistics、AI 报告、预警链路边界清晰
- 页面壳、runtime、view-model、scss 分层稳定

### 10.2 测试层

至少应稳定通过：

- `mvn -q test -f D:\Health\HealthData\pom.xml`
- `npm run build`
- `npm run audit:structure`
- `npm run audit:api`
- `npm run audit:data`
- `npm run audit:write`
- `npm run audit:auth`
- `npm run audit:e2e`
- `npm run audit:pipeline`
- `npm run audit:pipeline-warning`

### 10.3 双库层

- old 和 new 不互相污染结论
- old 负责模拟器和高密度历史数据
- new 负责真实手表和空库上线切换
- 切库规则可解释、可回写、可核对

### 10.4 交付层

最终输出要能明确回答：

- 当前完成了什么
- 还剩什么债
- 证据在哪里
- 下一步谁来做
- 用哪个数据源
- 用哪些命令复现

---

## 11. 推荐的协作方式

### 11.1 你把什么交给我

适合先交给我：

- 长 runbook
- 跨前后端任务
- 需要拆分为多个子任务的工作
- 需要我统一判断 old/new 数据源的任务
- 需要我制定执行顺序和验收标准的任务

### 11.2 你直接交给 Codex 的情况

适合直接交给 Codex：

- 一个明确 bug
- 一个明确文件
- 一个小范围 patch
- 一个可以直接验证的 issue

### 11.3 你直接交给 Claude Code 的情况

适合直接交给 Claude Code：

- 大仓库局部重构
- 目录整理
- 页面层拆分
- 代码理解后再落 patch

---

## 12. 终极执行顺序

如果以后完全按一条线走，建议顺序就是：

1. 读 `AGENTS.md`
2. 读本文件
3. 识别当前任务和数据源
4. 查当前代码和工作树
5. 先 preflight
6. 选 smoke / standard / full / dual-db
7. 执行测试或修复
8. 归档产物
9. 更新相关文档
10. 进入下一轮

---

## 13. 当前可直接复用的启动命令

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

停止模拟器：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
```

---

## 14. 最后一句

Health 的正确姿势不是“单次把事情做完”，而是“每次都能稳定地把事情做对、做完、留证据、可复现”。
