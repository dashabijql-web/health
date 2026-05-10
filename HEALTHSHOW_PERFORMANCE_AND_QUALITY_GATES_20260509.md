# HealthShow 性能、竞品对标与代码健康质量门禁

创建日期：2026-05-09
适用目录：`D:/Health/HealthShow`
定位：在功能测试门禁之外，补齐性能、产品体验、竞品对标、屎山代码风险与健壮性扫描。

---

## 1. 新增 npm 入口

### 性能测试

```bash
npm run test:perf
npm run test:perf:old
npm run test:perf:new
```

直接脚本：

```bash
npm run audit:perf:api
npm run audit:perf:page
npm run audit:perf
npm run audit:perf:old
npm run audit:perf:new
```

### 代码健康与竞品对标

```bash
npm run test:quality
npm run audit:quality:code
npm run audit:product
```

---

## 2. 性能测试覆盖范围

### API 查询速度

脚本：`tests/performance/api-latency.mjs`

覆盖关键查询接口：

- dashboard overview
- dashboard body indicators
- realtime statistics
- realtime online users
- risk-warning list
- employee list/detail
- employee stats
- health-record page
- heart-rate overview
- blood-oxygen overview
- pressure overview
- blood-pressure overview

输出：

- p50
- p95
- max
- failure rate
- 每接口阈值
- JSON + Markdown 报告

产物目录：

```text
tests/performance/artifacts/<RUN_ID>/api-latency.json
tests/performance/artifacts/<RUN_ID>/api-latency.md
```

默认阈值可通过环境变量调整：

```bash
PERF_API_ITERATIONS=5
PERF_API_WARMUP=1
PERF_API_P95_MS=1200
PERF_API_MAX_MS=2500
PERF_API_FAILURE_RATE=0
```

### 页面加载速度

脚本：`tests/performance/page-load.mjs`

覆盖关键页面：

- dashboard
- real-time
- risk-warning
- alert-notifications
- employee-archive
- report-center

输出：

- DOMContentLoaded
- 页面 ready time
- 页面 API p95
- API 失败请求
- JSON + Markdown 报告

产物目录：

```text
tests/performance/artifacts/<RUN_ID>/page-load.json
tests/performance/artifacts/<RUN_ID>/page-load.md
```

默认阈值可通过环境变量调整：

```bash
PERF_PAGE_ITERATIONS=3
PERF_PAGE_READY_MS=3500
PERF_PAGE_DCL_MS=2500
PERF_PAGE_API_P95_MS=1500
```

---

## 3. old/new 数据源支持

性能脚本复用统一测试工具：

- `API_DATA_SOURCE=old|new`
- `API_EXPECT_NON_EMPTY=1|0`
- Cookie：`Health-Data-Source`
- Header：`X-Health-Data-Source`

推荐：

```bash
npm run test:perf:old
npm run test:perf:new
```

---

## 4. BLOCKED / FAILED 语义

`test:perf` 接入统一 runner：

```bash
node scripts/health-test-runner.mjs perf
```

preflight 使用 integration 级别检查：

- Node
- cwd
- data-source
- package-lock
- backend 8080
- frontend 9528
- Playwright browser

服务未启动时返回：

```text
BLOCKED / exit 3
```

性能超阈值或 API/page 断言失败时返回：

```text
FAILED / exit 1
```

---

## 5. 竞品对标、页面美观、布局合理度、功能完善度

脚本：`tests/product/ux-competitor-benchmark.mjs`

它不是替代人工设计评审的“视觉 AI”，而是一个可重复运行的竞品启发式雷达。对标对象包括：

- Apple Health：健康数据清晰度、趋势表达、卡片层级
- Fitbit / Garmin：指标完整度、运动/健康数据闭环
- Datadog / Grafana：监控台布局、状态语义、下钻与告警体验
- 医疗/工业健康监控类产品：人员、设备、预警、报告闭环

评分维度：

- 视觉系统一致性
- 布局与信息架构
- 功能完善度
- 运营可用性
- 前端实现质量

默认最低分：

```bash
PRODUCT_MIN_SCORE=72
```

产物目录：

```text
tests/product/artifacts/<RUN_ID>/ux-competitor-benchmark.json
tests/product/artifacts/<RUN_ID>/ux-competitor-benchmark.md
```

---

## 6. 屎山代码程度与健壮性扫描

脚本：`tests/quality/code-health.mjs`

扫描范围：

- `src`
- `tests/api`
- `tests/e2e`
- `tests/shared`
- `tests/performance`

检查项：

- 超大 Vue/JS 文件
- 超长函数
- 简易圈复杂度
- `console.log`
- `TODO/FIXME`
- `setTimeout/setInterval` 生命周期风险
- `eval/new Function`
- 未发现 sanitizer 证据的 `v-html`

默认策略：

- `eval/new Function`：硬失败
- `console.log` 超过阈值：硬失败
- 未净化 `v-html` 超过阈值：硬失败
- 大文件/复杂函数/TODO/timer：先作为 warn，用于持续治理，不一刀切阻塞

阈值：

```bash
QUALITY_MAX_VUE_LINES=650
QUALITY_MAX_JS_LINES=500
QUALITY_MAX_FUNCTION_COMPLEXITY=18
QUALITY_MAX_FUNCTION_LINES=120
QUALITY_MAX_CONSOLE_LOG=0
QUALITY_MAX_VHTML_UNSANITIZED=0
```

产物目录：

```text
tests/quality/artifacts/<RUN_ID>/code-health.json
tests/quality/artifacts/<RUN_ID>/code-health.md
```

---

## 7. 推荐使用方式

日常前端改动：

```bash
npm run test:frontend
npm run test:quality
```

涉及接口/性能/页面体验：

```bash
npm run test:integration
npm run test:perf
```

发布或大改前：

```bash
npm run test:full
```

如果服务未启动，`test:perf` / `test:full` 返回 BLOCKED，不应当被当作代码失败。
