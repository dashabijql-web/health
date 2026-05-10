# HealthShow 完整测试流程与实跑手册

创建日期：2026-05-09
适用范围：`D:/Health/HealthShow` + `D:/Health/HealthData`
定位：把 Health 项目的功能正确性、性能、页面体验、竞品对标、代码健康、双库、模拟器、数据库、Redis、前后端服务统一成一套可复跑的质量门禁流程。

> 安全约定：本文档不记录任何真实密码、token、API key、连接串密钥。需要凭据时使用环境变量，示例统一写为 `[REDACTED]`。

---

## 1. 项目边界

`D:/Health` 不是一个 git 仓库，实际有两个主要子仓库：

- 前端：`D:/Health/HealthShow`
- 后端：`D:/Health/HealthData`

所有 git、测试、构建命令都必须在对应子仓库里执行，不能把 `D:/Health` 当成统一仓库操作。

---

## 2. 数据源约定

Health 当前支持 old/new 双数据源。

| 数据源 | 数据库 | 用途 | 非空预期 |
| --- | --- | --- | --- |
| `old` | `health` | 模拟器、演示、高数据量页面验收、历史数据回归 | 应该非空 |
| `new` | `health_new` | 真实手表、上线切换、空库场景 | 可以为空 |

统一环境变量：

```powershell
$env:API_DATA_SOURCE='old'       # 或 new
$env:API_EXPECT_NON_EMPTY='1'    # old=1, new=0
$env:PIPELINE_DATA_SOURCE='old'  # pipeline 写入链路同步使用
$env:SQL_DB='health'             # old=health, new=health_new
```

前端测试 runner 会自动根据 `--source old/new` 设置：

- `API_DATA_SOURCE`
- `API_EXPECT_NON_EMPTY`
- `PIPELINE_DATA_SOURCE`
- `SQL_DB`

---

## 3. PASS / FAILED / BLOCKED 语义

统一 runner：

```powershell
cd D:/Health/HealthShow
npm run test:fast
npm run test:frontend
npm run test:quality
npm run test:integration
npm run test:perf
npm run test:full
```

退出码语义：

| 状态 | exit code | 含义 |
| --- | ---: | --- |
| `PASS` | 0 | 测试实际执行并通过 |
| `FAILED` | 1/其它非 3 | 代码、断言、接口契约或性能阈值失败 |
| `BLOCKED` | 3 | 环境未就绪，例如前端/后端/SQL/Redis/sqlcmd/Playwright browser 未启动或缺失 |

重要原则：

- 服务没启动不能报 FAILED，必须报 BLOCKED。
- API contract error 不能吞成 warning，必须 FAILED。
- old 数据源为空通常 FAILED；new 数据源为空可以 PASS 或 warning，取决于具体测试。

---

## 4. 环境前置条件

### 4.1 Windows / PowerShell 环境

推荐从 Windows PowerShell 启动完整服务栈，因为 SQL Server、Redis、Java/Maven、前端 dev server 多数在 Windows 侧更稳定。

必需工具：

- Java 17
- Maven：`D:/apache-maven-3.8.1/bin/mvn.cmd`
- Node/npm
- Python
- SQL Server，默认监听 `localhost,58135`
- `sqlcmd`
- Redis，默认监听 `localhost,6379`
- Playwright Chromium

### 4.2 关键端口

| 服务 | 地址 |
| --- | --- |
| 后端 HTTP | `127.0.0.1:8080` |
| 后端 TCP 手表接入 | `127.0.0.1:9000` |
| 前端 Vite | `127.0.0.1:9528` |
| SQL Server | `127.0.0.1:58135` |
| Redis | `127.0.0.1:6379` |

---

## 5. 完整服务栈启动顺序

推荐顺序：

1. 启动 SQL Server。
2. 启动 Redis。
3. 启动 HealthData 后端。
4. 等待后端 HTTP 8080 和 TCP 9000 ready。
5. 启动 HealthShow 前端。
6. 等待前端 9528 ready。
7. old 数据源场景下启动手表模拟器。
8. 跑测试门禁。

### 5.1 SQL Server

SQL Server 必须能响应：

```powershell
sqlcmd -S localhost,58135 -U sa -P '[REDACTED]' -d health -Q "SET NOCOUNT ON; SELECT 1 AS ok;"
sqlcmd -S localhost,58135 -U sa -P '[REDACTED]' -d health_new -Q "SET NOCOUNT ON; SELECT 1 AS ok;"
```

实际密码不要写入文档，运行时设置：

```powershell
$env:SQL_PASSWORD='[REDACTED]'
$env:DB_PASSWORD='[REDACTED]'
```

### 5.2 Redis

Redis 必须能响应：

```powershell
redis-cli -h 127.0.0.1 -p 6379 ping
```

期望：

```text
PONG
```

### 5.3 后端 HealthData

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17'
$env:DB_PASSWORD='[REDACTED]'
$env:SQL_PASSWORD='[REDACTED]'
D:/apache-maven-3.8.1/bin/mvn.cmd spring-boot:run -f D:/Health/HealthData/pom.xml
```

ready 判断：

- `127.0.0.1:8080` 可连接
- `127.0.0.1:9000` 可连接

### 5.4 前端 HealthShow

```powershell
cd D:/Health/HealthShow
npm run dev
```

ready 判断：

- `http://127.0.0.1:9528/` 返回 HTTP 200/3xx/有效 HTML。

### 5.5 手表模拟器

old 数据源、高数据量和 TCP 写入链路测试使用模拟器：

```powershell
cd D:/Health/HealthShow
python watch_tcp_simulator_1000.py
```

模拟器连接：

- 目标：`127.0.0.1:9000`
- 用途：持续向后端 TCP 端口写入模拟手表数据。

new 数据源真实手表切换时，应停止模拟器，避免污染判断。

---

## 6. 测试档位

### 6.1 Fast：纯逻辑最快门禁

```powershell
cd D:/Health/HealthShow
npm run test:fast
```

包含：

- warning semantics
- dashboard runtime/source guard
- 不依赖后端、前端 dev server、数据库、Redis、浏览器。

### 6.2 Frontend：前端默认门禁

```powershell
cd D:/Health/HealthShow
npm run test:frontend
```

包含：

- `test:fast`
- 导航/页面结构 audit
- production build

不依赖后端和数据库。

### 6.3 Quality：代码健康 + 产品体验门禁

```powershell
cd D:/Health/HealthShow
npm run test:quality
```

包含：

- `audit:quality:code`
- `audit:product`

覆盖：

- 超大文件
- 超长函数
- 简易复杂度
- Vue Options API / object method 扫描
- `console.log`
- `v-html`
- `eval/new Function`
- TODO/FIXME
- setTimeout/setInterval 生命周期风险
- 页面体验/竞品启发式 checklist

### 6.4 Integration：前后端接口门禁

```powershell
cd D:/Health/HealthShow
npm run test:integration:old
npm run test:integration:new
```

依赖：

- 后端 8080
- 前端 9528
- Playwright browser

覆盖：

- API smoke
- 数据密度 old/new
- 登录态回归

### 6.5 Performance：性能门禁

```powershell
cd D:/Health/HealthShow
npm run test:perf:old
npm run test:perf:new
```

依赖：

- 后端 8080
- 前端 9528
- Playwright browser

覆盖：

- API 查询速度：p50 / p95 / max / failedCount
- 页面加载速度：DOMContentLoaded / ready / API p95 / failed requests
- old/new 数据源传递：header + cookie + runner env

报告产物：

- `tests/performance/artifacts/<RUN_ID>/api-latency.json`
- `tests/performance/artifacts/<RUN_ID>/api-latency.md`
- `tests/performance/artifacts/<RUN_ID>/page-load.json`
- `tests/performance/artifacts/<RUN_ID>/page-load.md`

### 6.6 Full：发布级全量门禁

```powershell
cd D:/Health/HealthShow
npm run test:full:old
npm run test:full:new
```

依赖：

- 后端 8080
- 前端 9528
- Playwright browser
- SQL_PASSWORD
- sqlcmd
- SQL Server 58135
- TCP 9000
- Redis 6379

包含：

- frontend
- quality
- API smoke
- data density
- write regression
- auth regression
- e2e page audit
- performance
- pipeline regression
- warning pipeline regression

---

## 7. 后端回归

后端独立回归：

```powershell
$env:SQL_PASSWORD='[REDACTED]'
$env:DB_PASSWORD='[REDACTED]'
python D:/Health/HealthData/scripts/run_backend_regression.py
```

覆盖：

- 双库上下文
- Redis buffer flush
- SQL 写入链路
- typed row / API contract 相关后端 guard

---

## 8. 推荐完整实跑命令

在服务栈已启动后执行：

```powershell
cd D:/Health/HealthShow

npm run test:fast
npm run test:frontend
npm run test:quality
npm run test:integration:old
npm run test:integration:new
npm run test:perf:old
npm run test:perf:new
npm run test:full:old
npm run test:full:new
```

如需后端也一起验：

```powershell
python D:/Health/HealthData/scripts/run_backend_regression.py
```

---

## 9. 结果归档

前端统一 runner 每次会生成：

```text
D:/Health/HealthShow/tests/runs/<RUN_ID>/health-test-runner-summary.json
```

性能报告：

```text
D:/Health/HealthShow/tests/performance/artifacts/<RUN_ID>/
```

代码健康报告：

```text
D:/Health/HealthShow/tests/quality/artifacts/<RUN_ID>/
```

产品/竞品体验报告：

```text
D:/Health/HealthShow/tests/product/artifacts/<RUN_ID>/
```

---

## 10. 失败处理规则

### BLOCKED

先补环境，不改代码。例如：

- 后端 8080 未启动
- 前端 9528 未启动
- SQL Server 未启动
- Redis 未启动
- sqlcmd 缺失
- SQL_PASSWORD 未设置
- Playwright browser 未安装

### FAILED

按 systematic debugging 处理：

1. 读完整错误。
2. 复现最小失败命令。
3. 查最近改动和相关代码。
4. 明确 root cause。
5. 写/补回归测试。
6. 修复。
7. 重跑对应档位和上层档位。

---

## 11. 当前已实现的测试入口清单

```json
{
  "test:fast": "node scripts/health-test-runner.mjs fast",
  "test:frontend": "node scripts/health-test-runner.mjs frontend",
  "test:quality": "node scripts/health-test-runner.mjs quality",
  "test:integration": "node scripts/health-test-runner.mjs integration",
  "test:integration:old": "node scripts/health-test-runner.mjs integration --source old",
  "test:integration:new": "node scripts/health-test-runner.mjs integration --source new",
  "test:perf": "node scripts/health-test-runner.mjs perf",
  "test:perf:old": "node scripts/health-test-runner.mjs perf --source old",
  "test:perf:new": "node scripts/health-test-runner.mjs perf --source new",
  "test:full": "node scripts/health-test-runner.mjs full",
  "test:full:old": "node scripts/health-test-runner.mjs full --source old",
  "test:full:new": "node scripts/health-test-runner.mjs full --source new"
}
```

---

## 12. 接手人验收标准

一次完整闭环至少要能说明：

- 哪个数据源：old/new。
- 哪些服务已启动。
- 哪些命令 PASS。
- 哪些命令 BLOCKED，以及具体缺什么环境。
- 若 FAILED，给出 root cause，不只贴失败现象。
- 报告文件路径。
- 是否经过 Codex/Claude Code 复审。

