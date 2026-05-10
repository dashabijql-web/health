# HealthShow 完整测试流程（2026-05-09）

本文档是 HealthShow/HealthData 本地完整质量门禁的执行流程。目标是让测试不只验证“功能能跑”，还覆盖性能、页面加载、产品体验、代码健康、old/new 数据源、完整服务依赖和失败语义。

## 1. 结果语义

所有测试结果按以下语义解释：

- PASS：测试和质量门禁通过。
- FAILED：代码行为或测试断言失败，需要修代码或修测试断言。
- BLOCKED：环境或前置条件缺失，例如服务未启动、端口不可达、SQL_PASSWORD 缺失、Playwright browser 缺失、new 数据源缺少必要种子数据。
- skipped：某个数据源当前不具备执行该链路的种子数据，但其它门禁仍可执行；必须在报告中明确说明原因，不能伪造 PASS。

## 2. 服务依赖

完整服务栈需要：

| 组件 | 地址/端口 | 说明 |
| --- | --- | --- |
| SQL Server | `127.0.0.1:58135` | HealthData 数据库 |
| Redis | `127.0.0.1:6379` | 后端缓存/缓冲队列 |
| HealthData HTTP | `127.0.0.1:8080` | 后端 API |
| HealthData TCP | `127.0.0.1:9000` | 手表/设备 TCP 数据入口 |
| HealthShow frontend | `127.0.0.1:9528` | Vite 前端 |
| watch simulator | 本地进程 | 用于 TCP pipeline 验证 |

凭据要求：

- `SQL_PASSWORD` 必须设置。
- 任何报告不得输出真实密码、token、connection string。
- 日志中出现 `-P ...` 必须脱敏为 `-P [REDACTED]`。

## 3. 推荐执行入口

### 3.1 快速本地检查

```bash
npm run test:fast
```

用途：纯前端逻辑、源码守护，不依赖后端/浏览器/数据库。

### 3.2 前端构建与结构检查

```bash
npm run test:frontend
```

用途：包含 fast、结构审计、生产构建。

### 3.3 代码健康与产品体验

```bash
npm run test:quality
```

包含：

- `audit:quality:code`
- `audit:product`

用于识别：

- 高危硬编码
- 危险 API
- 大文件/复杂度趋势
- 重复/脆弱模式
- 页面体验 rubric 问题

### 3.4 集成测试 old/new

```bash
npm run test:integration:old
npm run test:integration:new
```

用途：API、auth、data、e2e 等依赖前后端的集成验证。

old 数据源：

- `API_DATA_SOURCE=old`
- `API_EXPECT_NON_EMPTY=1`

new 数据源：

- `API_DATA_SOURCE=new`
- `API_EXPECT_NON_EMPTY=0`

### 3.5 性能测试 old/new

```bash
npm run test:perf:old
npm run test:perf:new
```

覆盖：

- API 查询性能：p50/p95/max/failure count
- 页面加载性能：route load metrics

注意：

- 性能测试不放进 fast/frontend，避免日常开发变慢。
- old 历史库允许更宽阈值，但仍应记录真实 p50/p95/max。
- new 数据源保持严格阈值。

### 3.6 完整门禁 old/new

```bash
npm run test:full:old
npm run test:full:new
```

full 包含：

1. frontend
2. quality
3. API smoke
4. data density
5. auth/session regression
6. Playwright e2e route audit
7. performance API/page load
8. TCP -> Redis -> SQL -> API -> 页面 pipeline
9. warning pipeline
10. write regression

## 4. 一键完整服务栈实跑

推荐使用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File 'D:\Health\HealthShow	estsun-full-stack-local.ps1'
```

该脚本会：

1. 检查/启动 SQL Server；
2. 检查/启动 Redis；
3. 检查/启动 HealthData 后端；
4. 检查/启动 HealthShow 前端；
5. 检查/启动 watch simulator；
6. 输出 readiness；
7. 顺序执行：
   - `test:fast`
   - `test:frontend`
   - `test:quality`
   - `test:integration:old`
   - `test:integration:new`
   - `test:perf:old`
   - `test:perf:new`
   - `test:full:old`
   - `test:full:new`
8. 归档 stdout/stderr/summary。

归档目录：

`tests/runs/<run-id>-full-stack-local/`

## 5. 产物说明

常见产物：

- `tests/runs/<run-id>-full-stack-local/full-stack-local.log`
- `tests/runs/<run-id>-full-stack-local/full-stack-local-summary.json`
- `tests/runs/<run-id>-full-stack-local/*.out.log`
- `tests/runs/<run-id>-full-stack-local/*.err.log`
- `tests/api/artifacts/*/summary.md`
- `tests/api/artifacts/*/data-density.md`
- `tests/e2e/artifacts/*/summary.md`
- `tests/performance/artifacts/*/api-latency.md`
- `tests/performance/artifacts/*/page-load.md`
- `tests/pipeline/artifacts/*/summary.md`
- `tests/pipeline/artifacts/*/warning-summary.md`
- `tests/quality/artifacts/*/code-health.md`
- `tests/product/artifacts/*/ux-competitor-benchmark.md`

## 6. 排障顺序

如果失败：

1. 先看 full-stack-local.log 确认是哪一步 exit 非 0。
2. 打开对应 `*.out.log` 和 `*.err.log`。
3. 判断是 FAILED 还是 BLOCKED：
   - 端口不可达、服务未启动、SQL_PASSWORD 缺失、浏览器缺失：BLOCKED。
   - API 返回结构错误、断言失败、页面错误、性能超阈值：FAILED。
   - new 数据源缺少特定 pipeline 种子数据：应记录 skipped/BLOCKED-style note，不能误报代码失败。
4. 修复后至少重跑失败的最小入口。
5. 发布前重跑 `run-full-stack-local.ps1`。

## 7. 当前已知状态

截至本报告生成时，最新完整 run 为：

`/mnt/d/Health/HealthShow/tests/runs/20260509-234023-full-stack-local`

已确认 PASS：

- fast
- frontend
- quality
- integration old/new
- perf old/new
- full old

待闭环：

- full new 的 warning pipeline 在 new 数据源缺少绑定 warning 探针目标时仍 FAILED，应改为明确 skipped/BLOCKED-style 语义或补齐 new 数据源种子数据。

