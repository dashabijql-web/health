# Mac Codex Handoff

## 目标

把当前 Health monorepo 交给 Mac 上的 Codex 继续开发。Mac 已有 SQL Server 2022 和 Redis，但没有业务数据库数据；因此先迁移 SQL Server 数据库，再启动后端、前端和模拟器。

当前项目不是“空库可自动初始化”的架构。仓库里没有完整 DDL/Flyway/Liquibase 初始化链路，后端启动不会自动创建 `health` / `health_new` 全量表、视图、存储过程和基础数据。不要跳过数据库备份还原。

## 必须迁移的数据库

- `health`：老库，承接模拟器数据和本地演示数据密度验收。
- `health_new`：新库，承接真实手表和空业务数据验收。

推荐直接从当前可运行机器备份并迁移两个库。这样 Mac 侧能保留：

- 老库的演示/模拟数据。
- 新库的系统基础配置。
- `admin / admin123` 登录入口。
- 月分表、视图、存储过程、权限表、预警配置等非代码资产。

## 源机器导出

在当前可运行机器上确认 SQL Server 可连：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "SELECT name FROM sys.databases WHERE name IN ('health','health_new');"
```

Linux / Docker SQL Server 常用备份路径：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "BACKUP DATABASE [health] TO DISK = N'/var/opt/mssql/backup/health_full.bak' WITH COPY_ONLY, INIT, COMPRESSION, STATS = 5;"
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "BACKUP DATABASE [health_new] TO DISK = N'/var/opt/mssql/backup/health_new_full.bak' WITH COPY_ONLY, INIT, COMPRESSION, STATS = 5;"
```

如果源机器是 Windows 原生 SQL Server，把备份路径换成 SQL Server 服务账号可写目录，例如：

```sql
BACKUP DATABASE [health]
TO DISK = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\Backup\health_full.bak'
WITH COPY_ONLY, INIT, COMPRESSION, STATS = 5;
```

把两个 `.bak` 文件复制到 Mac。不要把 `.bak` 放进 git 仓库。

## Mac 还原

如果 Mac 的 SQL Server 2022 跑在 Docker 容器里，先复制备份进容器：

```bash
docker cp health_full.bak <mssql-container>:/var/opt/mssql/backup/health_full.bak
docker cp health_new_full.bak <mssql-container>:/var/opt/mssql/backup/health_new_full.bak
```

先查看逻辑文件名：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "RESTORE FILELISTONLY FROM DISK = N'/var/opt/mssql/backup/health_full.bak';"
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "RESTORE FILELISTONLY FROM DISK = N'/var/opt/mssql/backup/health_new_full.bak';"
```

按 `RESTORE FILELISTONLY` 返回的 LogicalName 调整 `MOVE`。如果逻辑名仍是 `health` / `health_log`，可直接执行：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "IF DB_ID(N'health') IS NOT NULL ALTER DATABASE [health] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE [health] FROM DISK = N'/var/opt/mssql/backup/health_full.bak' WITH REPLACE, MOVE N'health' TO N'/var/opt/mssql/data/health.mdf', MOVE N'health_log' TO N'/var/opt/mssql/data/health_log.ldf', STATS = 5; ALTER DATABASE [health] SET MULTI_USER;"
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "IF DB_ID(N'health_new') IS NOT NULL ALTER DATABASE [health_new] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE [health_new] FROM DISK = N'/var/opt/mssql/backup/health_new_full.bak' WITH REPLACE, MOVE N'health' TO N'/var/opt/mssql/data/health_new.mdf', MOVE N'health_log' TO N'/var/opt/mssql/data/health_new_log.ldf', STATS = 5; ALTER DATABASE [health_new] SET MULTI_USER;"
```

如果 `health_new_full.bak` 的逻辑名已经是 `health_new` / `health_new_log`，把第二条命令中的 `MOVE N'health'` 和 `MOVE N'health_log'` 改成实际逻辑名。

## 只有 `health` 备份时创建 `health_new`

优先迁移两个库。只有 `health` 备份时，可先从 `health` 复制出 `health_new`，再清空新库业务数据：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "BACKUP DATABASE [health] TO DISK = N'/var/opt/mssql/backup/health_new_seed.bak' WITH COPY_ONLY, INIT, COMPRESSION, STATS = 5;"
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -Q "IF DB_ID(N'health_new') IS NOT NULL ALTER DATABASE [health_new] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE [health_new] FROM DISK = N'/var/opt/mssql/backup/health_new_seed.bak' WITH REPLACE, MOVE N'health' TO N'/var/opt/mssql/data/health_new.mdf', MOVE N'health_log' TO N'/var/opt/mssql/data/health_new_log.ldf', STATS = 5; ALTER DATABASE [health_new] SET MULTI_USER;"
```

然后运行清空脚本逻辑。参考仓库文件：

```bash
HealthData/src/main/resources/sql/create_health_new_seed.sql
```

该脚本内置的是旧 Windows 路径；Mac 上不要整段直接运行。只复用 `USE [health_new]` 之后的 `DELETE`、默认管理员、角色绑定和存储过程刷新部分。

## Mac 环境变量

项目默认假设 SQL Server 在 `localhost:11433`，Redis 在 `localhost:6379`。如果 Mac 实际端口不同，在 shell profile 或启动命令前导出：

```bash
export DB_HOST=localhost
export DB_PORT=11433
export DB_USERNAME=sa
export DB_PASSWORD='<your-local-sa-password>'
export DB_NAME_OLD=health
export DB_NAME_NEW=health_new

export HEALTH_DEFAULT_SOURCE=new
export HEALTH_REQUEST_SOURCE=old
export HEALTH_WATCH_SOURCE=new
export HEALTH_SIMULATOR_SOURCE=old
export HEALTH_SIMULATOR_IMEI_REGEX='^3594567800\d{5}$'
```

不要把 `DB_PASSWORD`、GitHub token、DeepSeek key 写入仓库、日志、提交信息或文档。

## Mac 本地依赖

建议版本：

- JDK 17
- Maven 3.9+
- Node.js 20+
- npm
- Python 3
- Playwright Chromium：在 `HealthShow` 下运行 `npm run audit:e2e:install`
- `sqlcmd`：安装 `mssql-tools18` 或等效 SQL Server 命令行工具

## 启动

在 Mac 上克隆/同步仓库后，从仓库根目录执行：

```bash
cd /path/to/health
```

后端依赖预编译：

```bash
cd HealthData
mvn -q test
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
```

前端依赖：

```bash
cd ../HealthShow
npm install
```

启动全栈：

```bash
cd /path/to/health
bash tools/health-wsl-stack.sh all start
```

这个脚本名保留了 `wsl`，但实际是 Bash 管理脚本；Mac 可用前提是 `readlink -f` 可用。如果 macOS 自带 `readlink` 不支持 `-f`，先安装 coreutils 并确保 GNU `readlink` 在 PATH 中，或手动分开启动。

手动启动方式：

```bash
cd /path/to/health/HealthData
SERVER_PORT=8080 NETTY_SERVER_PORT=9000 mvn spring-boot:run
```

```bash
cd /path/to/health/HealthShow
npm run dev -- --host 0.0.0.0 --port 9528
```

```bash
cd /path/to/health/HealthShow
python3 watch_tcp_simulator_1000.py
```

模拟器最多只允许一个实例。新库/真实手表验收前必须停止模拟器；旧库/演示数据密度验收时再启动模拟器。

## 验证

基础连通：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -d health -Q "SELECT 1 AS old_ok;"
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -d health_new -Q "SELECT 1 AS new_ok;"
redis-cli -h 127.0.0.1 -p 6379 ping
curl -i http://localhost:8080/health/actuator/health
curl -I http://localhost:9528/
```

核对新库空态：

```bash
sqlcmd -S localhost,11433 -U sa -P "$DB_PASSWORD" -d health_new -Q "SET NOCOUNT ON; SELECT 'department' AS table_name, COUNT(*) AS row_count FROM department UNION ALL SELECT 'employee', COUNT(*) FROM employee UNION ALL SELECT 'device', COUNT(*) FROM device UNION ALL SELECT 'device_user', COUNT(*) FROM device_user UNION ALL SELECT 'realtime_data', COUNT(*) FROM realtime_data UNION ALL SELECT 'user_online_status', COUNT(*) FROM user_online_status UNION ALL SELECT 'job_type', COUNT(*) FROM job_type UNION ALL SELECT 'alert_config', COUNT(*) FROM alert_config UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user;"
```

前端快速门禁：

```bash
cd /path/to/health/HealthShow
npm run test:fast
npm run audit:structure
npm run build
```

全栈门禁：

```bash
cd /path/to/health
python3 HealthShow/tests/run-full-stack-local.py --data-source both
```

如果 `health_new` 保持空业务库，`test-full-new` 中绑定设备、未处理 warning、AI 报告员工候选这类业务写探针允许显示为 `skipped`；旧库不允许同类跳过。

## 给 Mac Codex 的首轮提示

建议在 Mac Codex 新会话第一条消息中明确：

```text
先读根目录 AGENTS.md 和 docs/MAC_CODEX_HANDOFF.md。当前 Mac 有 SQL Server 2022 和 Redis；数据库应已还原 health / health_new。请先做环境核对：git status、SQL/Redis 连通、后端 actuator、前端 9528，然后只在根目录处理 git。不要回滚现有用户改动，不要把 token 或数据库密码写入仓库。
```

## 常见失败点

- 只还原 `health`，没创建 `health_new`：后端双数据源启动失败。
- `RESTORE` 的逻辑文件名不匹配：先跑 `RESTORE FILELISTONLY`，再改 `MOVE`。
- SQL Server 端口不是 `11433`：导出 `DB_PORT` 或改启动环境。
- Redis 未启动：后端可启动但 AI 会话、手表缓冲和 pipeline 探针会失败。
- Mac 上 `tools/health-wsl-stack.sh` 因 `readlink -f` 失败：安装 GNU coreutils 或手动启动。
- 新库业务表为空被误判为失败：这是当前设计，`health_new.department/employee/device/... = 0` 是预期空态。
