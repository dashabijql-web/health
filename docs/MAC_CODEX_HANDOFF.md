# Health 项目 MacBook Codex 移交手册

更新时间：2026-07-16（Asia/Shanghai）

## 1. 移交目标

本手册用于把 Health monorepo 的开发、测试与维护工作移交给 MacBook 上的 Codex。交接不是只复制源码：目标机必须同时恢复 Git 历史、当前未提交现场、SQL Server 双库、Redis、Java/Node/Python 工具链，并用真实探针完成验收。

接管后的第一原则：先读根目录 `AGENTS.md`，再读本文件。代码与当前配置优先于历史文档；只在 monorepo 根目录执行 Git 操作。

## 2. 本次移交现场

- 仓库：`health-monorepo`
- 当前分支：`codex/health-local-fixes-20260709`
- 打包基线提交：以交接包 `MANIFEST.md` 为准
- 当前工作树：非干净状态，包含已修改文件和未跟踪的新功能文件
- 前端：Vue 3 + Vite 5，开发端口 `9528`
- 后端：Spring Boot 3.2.12 + Java 17，HTTP `8080/health`，手表 TCP `9000`
- 数据：SQL Server 双库 `health`（老库）与 `health_new`（新库）
- Redis：默认 `127.0.0.1:6379`
- 登录入口：`admin / admin123`
- 当前开发默认数据源：老库；真实手表默认写新库，模拟器默认写老库

重要：当前未提交改动是有效开发现场，不是可丢弃的临时文件。Mac 上首次打开后禁止执行 `git reset --hard`、`git clean -fd` 或覆盖式 checkout。

## 3. 交接包结构

完整交接目录包含：

```text
health-codex-handoff-20260716/
├── START_HERE.md
├── MANIFEST.md
├── Health项目MacBook-Codex移交手册.docx
├── source/
│   ├── health-repository-current.tar.gz
│   └── health-repository-history.bundle
├── database/
│   ├── health_full_20260716.bak
│   ├── health_new_full_20260716.bak
│   └── migrations/
├── state/
│   ├── git-status.txt
│   ├── git-diff-stat.txt
│   ├── git-head.txt
│   └── runtime-status.txt
└── checksums/
    └── SHA256SUMS
```

`health-repository-current.tar.gz` 是首选恢复入口，包含 `.git`、完整历史和当前未提交工作树；已排除依赖缓存、构建产物、运行日志、测试截图产物和本地密钥。`.bundle` 是 Git 历史的独立恢复保险，不包含未提交改动。

## 4. Mac 架构与 SQL Server 决策

先执行：

```bash
uname -m
```

- `x86_64`（Intel Mac）：可在 Docker Desktop 的 Linux VM 中运行 SQL Server x86-64 容器。
- `arm64`（Apple Silicon）：微软当前说明 SQL Server Linux 容器只支持 Intel/AMD x86-64 Linux 主机，Rosetta 2、QEMU 等仿真/翻译环境未经测试且不受支持。

Apple Silicon 推荐方案按可靠性排序：

1. 连接现有 Windows/Linux x86-64 SQL Server（推荐，开发与维护最稳妥）。
2. 在独立 x86-64 虚拟机或服务器运行 SQL Server，Mac 只运行前后端与 Redis。
3. 仅个人临时开发时尝试 Docker `--platform linux/amd64`；这不是受支持方案，不能作为生产或唯一数据库环境。

官方依据：

- Microsoft Learn: https://learn.microsoft.com/sql/linux/quickstart-install-connect-docker
- Microsoft Learn: https://learn.microsoft.com/sql/linux/sql-server-linux-setup

不要用 PostgreSQL、MySQL 或 SQLite 直接替换。本项目存在 SQL Server 月分表、动态 SQL、存储过程和方言依赖，这不是简单换连接串即可完成的迁移。

## 5. Mac 基础环境

推荐通过 Homebrew 安装：

```bash
xcode-select --install
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install openjdk@17 maven node@20 python redis tmux coreutils git
```

配置 Java 与 Node（Apple Silicon Homebrew 通常位于 `/opt/homebrew`，Intel 通常位于 `/usr/local`）：

```bash
export PATH="$(brew --prefix openjdk@17)/bin:$(brew --prefix node@20)/bin:$PATH"
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
java -version
mvn -version
node -v
npm -v
python3 --version
```

安装 Codex 后，把解压出的仓库目录作为项目打开。不要让 Codex 从远端分支重新克隆后覆盖当前快照。

## 6. 恢复源码与 Git 现场

从交接目录执行：

```bash
mkdir -p ~/code
tar -xzf source/health-repository-current.tar.gz -C ~/code
cd ~/code/health
git status --short --branch
git remote -v
```

将输出与 `state/git-status.txt`、`state/git-head.txt` 比较。文件条目应一致；macOS 可能只产生文件权限差异，不能在未核对前批量回滚。

如果主归档损坏，使用 Git bundle 恢复已提交历史：

```bash
git clone source/health-repository-history.bundle ~/code/health-history-recovery
```

注意：bundle 不包含打包时的未提交改动，所以只能作为历史恢复，不能替代主归档。

## 7. 恢复数据库

项目不能从空库自动完整初始化。必须恢复 `health` 和 `health_new` 两个 `.bak`，否则双数据源后端无法正常接管。

在目标 SQL Server 主机创建 SQL Server 可读的备份目录，并复制：

```text
database/health_full_20260716.bak
database/health_new_full_20260716.bak
```

先检查逻辑文件名：

```sql
RESTORE FILELISTONLY FROM DISK = N'<server-visible-path>/health_full_20260716.bak';
RESTORE FILELISTONLY FROM DISK = N'<server-visible-path>/health_new_full_20260716.bak';
```

再按实际 `LogicalName` 执行恢复。示例（逻辑名必须以检查结果为准）：

```sql
RESTORE DATABASE [health]
FROM DISK = N'<server-visible-path>/health_full_20260716.bak'
WITH REPLACE,
MOVE N'health' TO N'<sql-data-path>/health.mdf',
MOVE N'health_log' TO N'<sql-data-path>/health_log.ldf',
STATS = 5;
```

对 `health_new` 重复操作并使用独立的 `.mdf/.ldf` 路径。恢复后，对两个库分别执行 `database/migrations/` 中的交接期迁移 SQL；迁移脚本应保持幂等，执行前仍需备份。

数据库账号和密码由接管者在目标机设置。不要把密码、GitHub token 或 AI key 写入仓库、交接文档、Codex 消息、提交信息或测试产物。

## 8. 环境变量

当前代码默认 SQL Server 端口是 `1433`。建议新建仅本机可读、且位于仓库外的 `~/.config/health/dev.env`：

```bash
export DB_HOST='<sql-server-host>'
export DB_PORT=1433
export DB_USERNAME=sa
export DB_PASSWORD='<local-secret>'
export DB_NAME_OLD=health
export DB_NAME_NEW=health_new

export HEALTH_DEFAULT_SOURCE=new
export HEALTH_REQUEST_SOURCE=old
export HEALTH_WATCH_SOURCE=new
export HEALTH_SIMULATOR_SOURCE=old
export HEALTH_SIMULATOR_IMEI_REGEX='^3594567800\d{5}$'
```

启动前加载：

```bash
source ~/.config/health/dev.env
```

Redis 默认 `localhost:6379`。如果 Redis 不在本机，再按当前 `application.yml` 支持范围调整配置；不要把机器专属凭据提交到 Git。

## 9. 安装依赖与启动

```bash
cd ~/code/health/HealthData
mvn -q test
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt

cd ../HealthShow
npm ci
npm run audit:e2e:install
```

优先尝试根级管理脚本：

```bash
cd ~/code/health
bash tools/health-wsl-stack.sh all start
bash tools/health-wsl-stack.sh status
```

脚本名保留 `wsl` 是历史原因。`health-wsl-env.sh` 会探测系统命令，但 macOS 对 GNU/Linux 命令行为仍可能有差异；已安装 `coreutils` 和 `tmux` 后再使用。若脚本失败，先看错误和 `runtime-logs/wsl-stack/`，不要盲改业务代码。

手动分开启动：

```bash
# 后端
cd ~/code/health/HealthData
SERVER_PORT=8080 NETTY_SERVER_PORT=9000 mvn spring-boot:run

# 前端（另一个终端）
cd ~/code/health/HealthShow
npm run dev -- --host 0.0.0.0 --port 9528

# 模拟器（只在老库演示/压测时启动，另一个终端）
cd ~/code/health/HealthShow
python3 watch_tcp_simulator_1000.py
```

模拟器最多一个实例。固定顺序：切新库 -> 停模拟器 -> 验真实手表/空库；切老库 -> 开模拟器 -> 验模拟数据。

## 10. 接管验收

基础探针：

```bash
redis-cli ping
curl -fsS http://localhost:8080/health/actuator/health
curl -I http://localhost:9528/
nc -zv 127.0.0.1 9000
```

Actuator 顶层必须为 `status=UP`，且 old/new data source 和 Redis 均为 `UP`。仅有 Java 进程或端口监听不算后端可用。

代码门禁：

```bash
cd ~/code/health/HealthData
mvn -q test

cd ../HealthShow
npm run test:fast
npm run audit:structure
npm run build
```

全栈双库门禁：

```bash
cd ~/code/health
python3 HealthShow/tests/run-full-stack-local.py --data-source both
```

新库保持空业务库时，部分需要员工、设备或预警数据的写探针允许 `skipped`；老库同类探针不允许跳过。视觉变更使用五档 `npm run audit:visual`，不能只看单一桌面截图。

## 11. Codex 首轮提示词

在 Mac Codex 打开本项目后的第一条消息使用：

```text
先完整阅读根目录 AGENTS.md、docs/MAC_CODEX_HANDOFF.md 和交接目录 MANIFEST.md。当前目录是从原开发机打包的完整 Git 工作树，包含尚未提交的有效改动；禁止 reset、clean 或覆盖它们。先只做环境核对：uname -m、git status、Java/Maven/Node/Python 版本、SQL Server 双库与 Redis 连通、后端 actuator、前端 9528、TCP 9000。结论必须基于本机输出。所有 Git 操作只在 monorepo 根目录执行，凭据只从目标机本地安全文件或环境变量读取，不得写入仓库。核对完成后给出差异和接管验收结果，不要先改业务代码。
```

## 12. 日常维护边界

- 每次任务先看 `AGENTS.md` 和当前工作树，用户现有改动默认不可回滚。
- 修改双库、runner、测试编排或页面治理时，同步更新对应权威文档。
- 每个明确任务只提交本任务确认的文件，在根目录提交并推送当前 monorepo 远端。
- GitHub 凭据在新 Mac 上重新配置；交接包不包含原机器 token。
- 数据库迁移对 `health` 和 `health_new` 分别执行，事件定位继续使用 `warningId + occurredAt`。
- 不在应用请求里自动建事件表；缺表应通过部署迁移解决。
- 不伪造未接入的复检、SLA、低电、设备故障或外部呼叫状态。
- 启动判断以 Actuator 顶层 `UP` 为准，停止后端必须确认 `8080/9000` 已释放。

## 13. 常见失败

- 后端双库启动失败：只恢复了一个数据库，或目标机端口/主机未通过环境变量覆盖。
- Apple Silicon SQL 容器无法启动或不稳定：这是 x86-64 仿真路径的已知支持边界，切换到远程 x86-64 SQL Server。
- `npm install` 造成锁文件漂移：删除本机 `node_modules` 后用 `npm ci`，不要提交平台缓存。
- 脚本找不到 Java/Maven：确认 `JAVA_HOME`、Homebrew PATH，以及 `health-wsl-env.sh` 的探测结果。
- 进程存在但 Actuator 为 `DOWN`：完整重启后端并查 Druid/数据库日志，不要只删除 pid 文件。
- 页面有菜单但业务组件为空：先确认右上角数据源和响应头 `X-Health-Data-Source`，再按新库空态/老库有数据的口径判断。
- 恢复数据库后事件接口 `503`：对两个库执行交接包中的 command center 迁移 SQL。

## 14. 移交完成标准

- 主源码归档 SHA-256 校验通过，`git status` 与交接记录一致。
- `health`、`health_new` 均可连接，必要迁移已在两库执行。
- Redis `PING` 成功。
- Actuator 顶层与双数据源、Redis均为 `UP`。
- 前端 `9528` 返回成功，后端 `8080` 与 TCP `9000` 可用。
- 后端测试、前端快速门禁、结构门禁和构建通过。
- Mac Codex 已理解未提交现场、双库语义、模拟器单实例和根仓 Git 规则。
- 新 Mac 已单独配置 GitHub 与外部 AI 凭据，原机器凭据未进入交接包。
