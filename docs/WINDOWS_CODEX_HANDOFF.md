# Windows Codex Handoff

## 目标

把当前 Health monorepo 交给另一台 Windows 电脑上的 Codex 继续开发。目标 Windows 电脑已有 SQL Server 2022 和 Redis，但没有项目数据库数据；因此必须先迁移 SQL Server 数据库，再启动后端、前端和模拟器。

当前项目不是“空 SQL Server 自动建库”的架构。仓库内没有完整 DDL/Flyway/Liquibase 初始化链路，Spring Boot 启动不会自动创建 `health` / `health_new` 全量表、视图、存储过程和基础数据。不要跳过数据库备份还原。

## 必须迁移的数据库

- `health`：老库，承接模拟器数据和本地演示数据密度验收。
- `health_new`：新库，承接真实手表和空业务数据验收。

推荐直接从当前可运行机器备份并迁移两个库。这样 Windows 目标机能保留：

- 老库演示/模拟数据。
- 新库系统基础配置。
- `admin / admin123` 登录入口。
- 月分表、视图、存储过程、权限表、预警配置等非代码资产。

## 访问入口和账号

- 前端访问地址：`http://localhost:9528/`
- 后端访问地址：`http://localhost:8080/health`
- 后端健康检查：`http://localhost:8080/health/actuator/health`
- 后端业务指标：`http://localhost:8080/health/actuator/metrics`
- 手表 TCP 端口：`9000`
- 管理员账号：`admin`
- 管理员密码：`admin123`
- 登录 token 存在 Cookie，不是 localStorage。
- 前端本地开发通过 Vite 代理把 `/dev-api/*` 重写到 `/health/*`。
- 前端右上角可切换“新库 / 老库”，请求头是 `X-Health-Data-Source`。

## 手表模拟器

模拟器脚本：

```text
HealthShow\watch_tcp_simulator_1000.py
```

Windows 手动启动：

```powershell
cd D:\Health\HealthShow
python watch_tcp_simulator_1000.py
```

默认模拟器连接：

- 主机：`127.0.0.1`
- TCP 端口：`9000`
- 默认数量：`1000` 块模拟手表
- 模拟器 IMEI 正则：`^3594567800\d{5}$`
- 命中模拟器正则的数据默认写入老库 `health`

规则：

- 同一台机器最多只运行一个模拟器实例。
- 做老库演示、压测、数据密度验收时启动模拟器。
- 做新库空态、真实手表验收时先停止模拟器。
- 如果页面右上角切到“新库”，业务表为空是预期；如果切到“老库”，模拟器数据应进入 `health`。

## 源机器导出

在能访问当前 SQL Server 的机器上执行。以下示例使用 SQL Server 2022 默认实例目录；如果实例名或安装目录不同，先改路径。

PowerShell：

```powershell
$env:DB_PASSWORD = '<source-sa-password>'
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "SELECT name FROM sys.databases WHERE name IN (N'health', N'health_new');"
```

备份两个库：

```powershell
$backupDir = 'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\Backup'
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "BACKUP DATABASE [health] TO DISK = N'$backupDir\health_full.bak' WITH COPY_ONLY, INIT, COMPRESSION, STATS = 5;"
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "BACKUP DATABASE [health_new] TO DISK = N'$backupDir\health_new_full.bak' WITH COPY_ONLY, INIT, COMPRESSION, STATS = 5;"
```

把下面两个文件复制到目标 Windows 电脑：

- `health_full.bak`
- `health_new_full.bak`

不要把 `.bak` 文件提交进 git。

## 目标 Windows 还原

先把 `.bak` 放到 SQL Server 服务账号可读目录，例如：

```text
C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\Backup\
```

检查逻辑文件名：

```powershell
$env:DB_PASSWORD = '<target-sa-password>'
$backupDir = 'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\Backup'
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "RESTORE FILELISTONLY FROM DISK = N'$backupDir\health_full.bak';"
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "RESTORE FILELISTONLY FROM DISK = N'$backupDir\health_new_full.bak';"
```

按 `RESTORE FILELISTONLY` 的 LogicalName 调整 `MOVE`。如果逻辑名仍是 `health` / `health_log`，可执行：

```powershell
$dataDir = 'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA'
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "IF DB_ID(N'health') IS NOT NULL ALTER DATABASE [health] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE [health] FROM DISK = N'$backupDir\health_full.bak' WITH REPLACE, MOVE N'health' TO N'$dataDir\health.mdf', MOVE N'health_log' TO N'$dataDir\health_log.ldf', STATS = 5; ALTER DATABASE [health] SET MULTI_USER;"
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -Q "IF DB_ID(N'health_new') IS NOT NULL ALTER DATABASE [health_new] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE [health_new] FROM DISK = N'$backupDir\health_new_full.bak' WITH REPLACE, MOVE N'health' TO N'$dataDir\health_new.mdf', MOVE N'health_log' TO N'$dataDir\health_new_log.ldf', STATS = 5; ALTER DATABASE [health_new] SET MULTI_USER;"
```

如果 `health_new_full.bak` 的逻辑名已经是 `health_new` / `health_new_log`，把第二条命令中的 `MOVE N'health'` 和 `MOVE N'health_log'` 改成实际逻辑名。

## 只有 `health` 备份时创建 `health_new`

优先迁移两个库。只有 `health` 备份时，可先从 `health` 复制出 `health_new`，再清空新库业务数据。参考包内脚本：

```text
database\scripts\create-health-new-from-health-windows.sql
```

该脚本需要先按目标机 SQL Server 实例路径调整 `.bak`、`.mdf`、`.ldf` 路径。

## Windows 环境变量

项目默认假设 SQL Server 在 `localhost:11433`，Redis 在 `localhost:6379`。如果目标机 SQL Server 使用默认 `1433`，要么把 SQL Server 监听端口改成 `11433`，要么启动前设置 `DB_PORT=1433`。

PowerShell 当前会话：

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '11433'
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = '<target-sa-password>'
$env:DB_NAME_OLD = 'health'
$env:DB_NAME_NEW = 'health_new'

$env:HEALTH_DEFAULT_SOURCE = 'new'
$env:HEALTH_REQUEST_SOURCE = 'old'
$env:HEALTH_WATCH_SOURCE = 'new'
$env:HEALTH_SIMULATOR_SOURCE = 'old'
$env:HEALTH_SIMULATOR_IMEI_REGEX = '^3594567800\d{5}$'
```

不要把 `DB_PASSWORD`、GitHub token、DeepSeek key 写入仓库、日志、提交信息或文档。

## Windows 本地依赖

建议版本：

- Git for Windows
- JDK 17
- Maven 3.9+
- Node.js 20+
- npm
- Python 3
- SQL Server command line tools: `sqlcmd`
- Redis for Windows 或 Docker Redis
- Playwright Chromium：在 `HealthShow` 下运行 `npm run audit:e2e:install`

## 启动方式

当前仓库主运行入口以 WSL/Bash 脚本为准；如果目标 Windows 电脑没有 WSL，直接手动分开启动更稳。

后端：

```powershell
cd D:\Health\HealthData
mvn -q test
mvn -q dependency:build-classpath -Dmdep.outputFile=target\runtime-classpath.txt
$env:SERVER_PORT = '8080'
$env:NETTY_SERVER_PORT = '9000'
mvn spring-boot:run
```

前端：

```powershell
cd D:\Health\HealthShow
npm install
npm run dev -- --host 0.0.0.0 --port 9528
```

模拟器：

```powershell
cd D:\Health\HealthShow
python watch_tcp_simulator_1000.py
```

模拟器最多只允许一个实例。新库/真实手表验收前必须停止模拟器；旧库/演示数据密度验收时再启动模拟器。

启动后访问：

```text
http://localhost:9528/
```

登录：

```text
admin / admin123
```

## 验证

基础连通：

```powershell
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -d health -Q "SELECT 1 AS old_ok;"
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -d health_new -Q "SELECT 1 AS new_ok;"
redis-cli -h 127.0.0.1 -p 6379 ping
curl.exe -i http://localhost:8080/health/actuator/health
curl.exe -I http://localhost:9528/
```

核对新库空态：

```powershell
sqlcmd -S localhost,11433 -U sa -P $env:DB_PASSWORD -d health_new -Q "SET NOCOUNT ON; SELECT 'department' AS table_name, COUNT(*) AS row_count FROM department UNION ALL SELECT 'employee', COUNT(*) FROM employee UNION ALL SELECT 'device', COUNT(*) FROM device UNION ALL SELECT 'device_user', COUNT(*) FROM device_user UNION ALL SELECT 'realtime_data', COUNT(*) FROM realtime_data UNION ALL SELECT 'user_online_status', COUNT(*) FROM user_online_status UNION ALL SELECT 'job_type', COUNT(*) FROM job_type UNION ALL SELECT 'alert_config', COUNT(*) FROM alert_config UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user;"
```

前端快速门禁：

```powershell
cd D:\Health\HealthShow
npm run test:fast
npm run audit:structure
npm run build
```

如果安装了 WSL，并按仓库既有 WSL 流程运行，可继续使用：

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both
```

如果 `health_new` 保持空业务库，`test-full-new` 中绑定设备、未处理 warning、AI 报告员工候选这类业务写探针允许显示为 `skipped`；旧库不允许同类跳过。

## 给 Windows Codex 的首轮提示

建议在目标 Windows Codex 新会话第一条消息中明确：

```text
先读根目录 AGENTS.md 和 docs/WINDOWS_CODEX_HANDOFF.md。当前 Windows 电脑有 SQL Server 2022 和 Redis；数据库应已还原 health / health_new。请先做环境核对：git status、SQL/Redis 连通、后端 actuator、前端 9528，然后只在根目录处理 git。不要回滚现有用户改动，不要把 token 或数据库密码写入仓库。
```

## 常见失败点

- 只还原 `health`，没创建 `health_new`：后端双数据源启动失败。
- SQL Server 实际端口是 `1433`，但项目默认 `11433`：设置 `DB_PORT` 或调整 SQL Server 监听端口。
- `RESTORE` 的逻辑文件名不匹配：先跑 `RESTORE FILELISTONLY`，再改 `MOVE`。
- `.bak` 放在普通用户目录，SQL Server 服务账号无权读取：复制到 SQL Server `Backup` 目录。
- Redis 未启动：后端可启动但 AI 会话、手表缓冲和 pipeline 探针会失败。
- 新库业务表为空被误判为失败：这是当前设计，`health_new.department/employee/device/... = 0` 是预期空态。
