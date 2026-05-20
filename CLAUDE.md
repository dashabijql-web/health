# Health 项目工作区

优先执行入口：`D:/Health/HEALTH_EXECUTION_ENTRY.md`。更完整的总流程见 `D:/Health/HEALTH_ULTIMATE_FLOW.md`。

## 目录结构

- **前端** `D:/Health/HealthShow` — Vue 3 + Vite 5，`npm run dev` → port 9528
- **后端** `D:/Health/HealthData` — Spring Boot 3.2.12，Maven WAR 项目

## 启动命令

```bash
# 一次性拉起三件套
bash /home/j/code/health/tools/health-wsl-stack.sh all start

# WSL 原生 full-stack runner
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both

# 根级 WSL loop runner
python3 /home/j/code/health/tests/run-health-loop.py --profile full --data-source both
```

### 模拟器单实例

`watch_tcp_simulator_1000.py` 同时只能跑一个。旧库演示/压测前先用下面命令确保单实例；新库验收前应停止所有模拟器。

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh simulator start
bash /home/j/code/health/tools/health-wsl-stack.sh simulator status
bash /home/j/code/health/tools/health-wsl-stack.sh simulator stop
```

## 访问地址

- 前端：http://localhost:9528/
- 后端 API：http://localhost:8080/health
- TCP (Netty)：port 9000

## 停止进程

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh all stop
```

## 浏览器自动化测试（Playwright MCP）

Claude Code 内置 Playwright MCP 工具，可直接操控浏览器测试前端页面，无需用户手动操作。

### 常用工具

| 工具 | 用途 |
|------|------|
| `mcp__playwright__browser_navigate` | 打开 URL |
| `mcp__playwright__browser_snapshot` | 获取页面结构（用于定位元素 ref） |
| `mcp__playwright__browser_take_screenshot` | 截图查看页面效果 |
| `mcp__playwright__browser_click` | 点击元素（需先 snapshot 获取 ref） |
| `mcp__playwright__browser_fill_form` | 填写表单 |
| `mcp__playwright__browser_type` | 输入文字 |
| `mcp__playwright__browser_console_messages` | 查看控制台报错 |
| `mcp__playwright__browser_close` | 关闭/重置浏览器（报错时先 close 再 navigate） |

### 标准测试流程

```
1. browser_close          → 重置浏览器状态（防止 "Target closed" 报错）
2. browser_navigate       → 打开登录页 http://localhost:9528/login
3. browser_snapshot       → 获取元素 ref
4. browser_click          → 点击登录按钮（ref=eXX）
5. browser_navigate       → 跳转到目标页面
6. browser_take_screenshot → 截图验证效果
```

### 登录方式

- 账号：admin / admin123
- 登录后 token 存入前端 `User-Token` Cookie，由 `src/utils/auth.js` 统一读写
- 若跳回登录页说明 token 过期或后端已重启，重新执行登录流程即可

### 注意事项

- 首次调用前务必执行 `browser_close` 清除旧上下文，否则报 `Target page has been closed`
- `browser_snapshot` 返回的 `ref=eXX` 才能用于 click/type 等操作，不能凭猜测
- curl 测试后端接口时加 `--noproxy localhost` 绕过系统 VPN 代理

## 2026-05-10 本机补充

- `AGENTS.md` 是 Health 根工作区优先入口；`HealthShow` 和 `HealthData` 是独立 git 仓库。
- 旧库模拟器最多一个实例；新库验收前必须停止模拟器。
- 健康数据、员工数据、手表数据和 AI 报告默认按隐私数据处理。
