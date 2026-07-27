# Health 智慧健康管理平台

`health` 是面向矿区职工健康与安全管理的 monorepo，包含 Vue 3 管理端、Spring Boot 后端、ESP32-C3 手表原型、设备模拟器及自动化测试工具。系统覆盖实时体征监测、风险预警、事件处置、职工健康画像、设备管理和 AI 辅助分析。

## 项目结构

```text
health/
├── HealthShow/                    Vue 3 + Vite 前端
├── HealthData/                    Spring Boot 后端
├── firmware/esp32c3-wifi-watch/  ESP32-C3 手表原型
├── tools/                         启动、探针和运行辅助脚本
└── tests/                         根级测试与闭环 runner
```

主要技术栈：

- 前端：Vue 3、Vite 5、Vue Router、Vuex、Element Plus、ECharts
- 后端：Java 17、Spring Boot 3.2、MyBatis-Plus、Sa-Token、Netty
- 基础设施：SQL Server、Redis
- 测试：Node.js Test Runner、Playwright、Maven Test

## 运行依赖

- Java 17
- Maven 3.8+
- Node.js 18+ 与 npm
- Python 3
- Redis
- SQL Server 2022（需准备 `health` 和 `health_new` 两个数据库）

推荐在 Windows 的 WSL Linux 文件系统内运行项目。macOS 可原生运行，SQL Server 可使用 Docker 容器提供。

## 快速启动

### WSL

在仓库根目录执行：

```bash
tools/health-wsl-stack.sh all start
tools/health-wsl-stack.sh status
```

停止全部服务：

```bash
tools/health-wsl-stack.sh all stop
```

也可以单独管理服务：

```bash
tools/health-wsl-stack.sh backend start
tools/health-wsl-stack.sh frontend start
tools/health-wsl-stack.sh simulator start
```

模拟器用于旧库演示和压测。验收真实手表或新库时应停止模拟器。

### macOS

先准备名为 `local-mssqlserver2022` 的 SQL Server Docker 容器，然后在不同终端中执行：

```bash
tools/run-redis-mac.sh
tools/run-backend-mac.sh
tools/run-frontend-mac.sh
```

数据库连接建议通过环境变量配置：

```bash
export DB_HOST=127.0.0.1
export DB_PORT=1433
export DB_USERNAME=sa
export DB_PASSWORD='<your-local-password>'
```

## 服务地址

| 服务 | 默认地址 |
| --- | --- |
| 前端 | http://localhost:9528/ |
| 后端 API | http://localhost:8080/health |
| 健康检查 | http://localhost:8080/health/actuator/health |
| Actuator 指标 | http://localhost:8080/health/actuator/metrics |
| 手表 TCP | 127.0.0.1:9000 |
| Redis | 127.0.0.1:6379 |
| SQL Server | 127.0.0.1:1433 |

本地默认登录账号为 `admin / admin123`。生产环境必须使用独立凭证。

启动后至少确认健康检查返回顶层 `"status":"UP"`：

```bash
curl http://127.0.0.1:8080/health/actuator/health
curl -I http://127.0.0.1:9528/
```

## 双数据源

系统同时连接两个逻辑数据源：

| 数据源 | 数据库 | 用途 |
| --- | --- | --- |
| `old` | `health` | 模拟器、演示数据和旧库回归 |
| `new` | `health_new` | 真实手表接入，业务数据为空或稀疏是允许状态 |

HTTP 请求可通过 `X-Health-Data-Source: old|new` 选择数据源，前端顶栏也提供切换入口。真实手表默认写入 `new`，模拟器默认写入 `old`。

```bash
curl -H 'X-Health-Data-Source: old' http://127.0.0.1:8080/health/actuator/health
```

## 常用测试

前端测试和构建：

```bash
cd HealthShow
npm install
npm run test:fast
npm run test:frontend
npm run build
```

需要完整运行环境时，可分别验证两个数据源：

```bash
cd HealthShow
npm run test:integration -- --source old
npm run test:integration -- --source new
npm run audit:visual
```

后端测试：

```bash
cd HealthData
mvn test
```

根级闭环测试：

```bash
python3 tests/run-health-loop.py --profile full --data-source both
```

## 协作约定

根目录 [AGENTS.md](AGENTS.md) 是本仓库唯一的详细协作与运行事实源。修改代码前请先阅读其中的数据源路由、认证、手表协议、测试验收和 Git 操作约定；代码、配置和实际运行结果优先于文档。

本仓库只有一个 Git 根目录，`HealthShow` 和 `HealthData` 都不是独立仓库。除本 README 外，不新增分散的 Markdown 说明文件。
