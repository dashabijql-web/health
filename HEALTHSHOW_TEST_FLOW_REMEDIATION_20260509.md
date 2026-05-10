# HealthShow 测试流程整改说明

创建日期：2026-05-09
适用目录：`D:/Health/HealthShow`
定位：把零散 npm audit/test 命令收敛成分层质量门禁，方便 Hermes / Codex / Claude Code / 人工统一执行和判断。

---

## 1. 分层测试档位

### Fast：纯前端逻辑/源码守护

```bash
npm run test:fast
```

等价于：

```bash
node scripts/health-test-runner.mjs fast
```

特点：
- 不依赖后端
- 不依赖浏览器
- 不依赖数据库
- 默认以 `TZ=UTC` 跑 warning semantics 测试

适用：
- helper 修改
- Vue SFC wiring 小改
- warning level / handled / SLA 语义保护

### Frontend：前端默认质量门禁

```bash
npm run test:frontend
```

包含：
- `test:warning-semantics`
- `audit:structure`
- `build`

适用：
- 前端页面/组件/样式/路由改动
- Codex/Claude 修复前端 issue 后的默认验收

### Integration：前后端接口门禁

```bash
npm run test:integration
npm run test:integration:old
npm run test:integration:new
```

前置条件：
- 后端 TCP `127.0.0.1:8080` 可连接
- 前端 `http://127.0.0.1:9528/` 已启动
- Playwright browser 已安装

包含：
- `audit:api`
- `audit:data:old` 或 `audit:data:new`
- `audit:auth`

如果服务或浏览器没准备好，runner 输出 `BLOCKED` 并以 exit code `3` 退出，不把环境问题误报为代码 `FAILED`。

### Full：完整链路门禁

```bash
npm run test:full
npm run test:full:old
npm run test:full:new
```

前置条件：
- 前端已启动
- 后端已启动
- Playwright browser 已安装
- `SQL_PASSWORD` 已设置
- `sqlcmd` 可用，或已设置 `SQLCMD_BIN`
- SQL Server 端口可连
- TCP 9000 可连
- Redis 6379 可连

包含 frontend、api、data、write、auth、e2e、pipeline、pipeline-warning。

---

## 2. old/new 数据源脚本

```bash
npm run audit:data:old
npm run audit:data:new
npm run test:integration:old
npm run test:integration:new
npm run test:full:old
npm run test:full:new
```

规则：
- `old` 自动注入：
  - `API_DATA_SOURCE=old`
  - `API_EXPECT_NON_EMPTY=1`
  - `PIPELINE_DATA_SOURCE=old`
  - `SQL_DB=health`
- `new` 自动注入：
  - `API_DATA_SOURCE=new`
  - `API_EXPECT_NON_EMPTY=0`
  - `PIPELINE_DATA_SOURCE=new`
  - `SQL_DB=health_new`

这样 API/data audit 与 pipeline audit 不会出现 old/new 数据源错位。

---

## 3. 统一 runner 输出语义

runner：

```bash
node scripts/health-test-runner.mjs <fast|frontend|integration|full> [--source old|new]
```

输出状态：
- `PASS`：测试命令全部通过
- `FAILED`：命令实际执行失败，属于代码/测试失败
- `BLOCKED`：前置条件不满足，例如服务、浏览器、SQL、Redis 没准备好

机器可读报告输出到：

```text
tests/runs/<RUN_ID>/health-test-runner-summary.json
```

Hermes/Codex/Claude 后续应优先读取该 JSON 判断下一步。

---

## 4. 共享测试工具与维护性约束

公共 helper 已收敛到：

```text
tests/shared/health-test-utils.mjs
```

覆盖：
- `LOGIN_CREDENTIALS`
- API target 列表
- `truncate` / `assert` / `isObject`
- artifact 清理
- 登录与 session 解析
- auth headers
- JSON 请求
- 结果断言
- 前端 base URL 探测

API smoke、data density、write regression、Playwright audit、auth session regression、pipeline regression、warning pipeline regression 均复用 shared helper。后续新增测试不要重新复制登录、requestJson、artifact cleanup、基础断言等逻辑。

---

## 5. warning semantics 测试命名

新增语义更准确的别名：

```bash
npm run test:warning-semantics
```

旧命令 `test:warning-lifecycle` 保留兼容，但新流程优先使用 `test:warning-semantics`。

该测试覆盖：
- warning lifecycle
- dashboard runtime import
- dashboard handled 归一化
- dashboard summary latest danger
- dashboard warning level helper
- dashboard warn curve marker
- dashboard dialogs level wiring

---

## 6. Codex quick review 模板

测试流程或 warning semantics 改动后，可使用短复审：

```text
只读复审 HealthShow 测试流程整改。
重点检查：
1. package.json 分层命令是否清楚：test:fast/test:frontend/test:integration/test:full/test:warning-semantics/audit:data:old/audit:data:new
2. scripts/health-test-runner.mjs 是否正确区分 PASS/FAILED/BLOCKED
3. integration/full 是否不会在服务未启动或依赖缺失时误报代码失败
4. old/new 数据源环境变量是否被统一注入 API/data/pipeline
5. tests/shared/health-test-utils.mjs 是否承担公共登录、请求、artifact 清理和断言职责
6. 是否引入新依赖、破坏 Windows/WSL 兼容性、或让测试流程更复杂难维护
只输出 PASS 或 FAIL；FAIL 列必须修复项；PASS 列最多 5 个非阻塞建议。
```
