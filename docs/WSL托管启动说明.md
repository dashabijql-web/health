# Health WSL托管启动说明

更新时间：2026-05-19 02:10

## 本次落地结果

- 前端代码主工作目录：`/home/j/code/health/HealthShow`
- 后端代码主工作目录：`/home/j/code/health/HealthData`
- WSL 本地运行时优先自动发现：
  - Java：优先 `HEALTH_JAVA_HOME` / `JAVA_HOME`，否则回退到 PATH 中的 `java`
  - Maven：优先 `HEALTH_MAVEN_HOME` / `MAVEN_HOME`，否则回退到 PATH 中的 `mvn`
  - Python：`python3`
  - Node / npm：PATH 中的 `node` / `npm`
- 已新增托管脚本：
  - `tools/health-wsl-env.sh`
  - `tools/health-wsl-stack.sh`
  - `tools/health-wsl-backend-run.sh`
  - `tools/health-wsl-frontend-run.sh`
  - `tools/health-wsl-simulator-run.sh`
  - `tools/health-wsl-capture-classpath.sh`
- 已生成后端运行类路径快照：
  - `HealthData/target/runtime-classpath.txt`

## 当前推荐运行方式

### 1. 一键托管启动

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh all start
```

WSL 原生 full-stack runner：

```bash
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both
```

WSL detached runner：

```bash
python3 /home/j/code/health/tools/start-health-runner.py --data-source both
```

根级 loop runner：

```bash
python3 /home/j/code/health/tests/run-health-loop.py --profile full --data-source both
```

查看状态：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh status
```

停止：

```bash
bash /home/j/code/health/tools/health-wsl-stack.sh all stop
```

说明：

- `health-wsl-stack.sh` 默认优先使用 `tmux` 做 detached 托管；父终端退出后，前端和模拟器不会再跟着退出
- 如果你明确不想用 `tmux`，可以手动指定：

```bash
HEALTH_STACK_LAUNCHER=nohup bash /home/j/code/health/tools/health-wsl-stack.sh all start
```

### 2. 先加载环境

```bash
source /home/j/code/health/tools/health-wsl-env.sh
```

### 3. 后端

优先使用直启命令：

```bash
cd /home/j/code/health/HealthData
SERVER_PORT=8080 NETTY_SERVER_PORT=9000 \
"$JAVA_HOME/bin/java" \
  -cp "target/classes:$(cat target/runtime-classpath.txt)" \
  com.xzkj.health.HealthApplication
```

说明：

- 这样启动不会再走 Maven 的 `spring-boot:run` 常规启动链
- 启动速度比首次 `mvn spring-boot:run` 稳定
- 当前后端默认：
  - HTTP：`8080`
  - TCP：`9000`

### 4. 前端

```bash
cd /home/j/code/health/HealthShow
npm run dev
```

默认访问地址：

- `http://127.0.0.1:9528`

### 5. 模拟器

```bash
cd /home/j/code/health/HealthShow
python3 watch_tcp_simulator_1000.py
```

默认配置：

- 目标主机：`127.0.0.1`
- 目标端口：`9000`
- 手表数量：`1000`
- 最大并发：`100`

也支持环境变量覆盖：

- `HEALTH_SIM_SERVER_HOST`
- `HEALTH_SIM_SERVER_PORT`
- `HEALTH_SIM_TOTAL_WATCHES`
- `HEALTH_SIM_MAX_CONCURRENT`
- `HEALTH_SIM_INTERVAL_MIN`
- `HEALTH_SIM_INTERVAL_MAX`
- `HEALTH_SIM_ANOMALY_RATE`

## 托管脚本说明

`tools/health-wsl-stack.sh` 已补齐以下入口：

```bash
health-wsl-stack.sh backend start|stop|restart|status
health-wsl-stack.sh frontend start|stop|restart|status
health-wsl-stack.sh simulator start|stop|restart|status
health-wsl-stack.sh all start|stop|restart|status
```

说明：

- 脚本本身已具备托管逻辑
- 默认优先走 `tmux` 托管，普通 WSL 终端和自动化会话都能稳定保活
- 若机器没有 `tmux`，会自动回退到 `nohup`

## 依赖或类路径变更后怎么刷新

如果后端依赖发生变化，先把后端按一次正常方式跑起来，然后执行：

```bash
/home/j/code/health/tools/health-wsl-capture-classpath.sh
```

它会从当前运行中的 `HealthApplication` 进程抓取实际 classpath，并覆盖：

- `HealthData/target/runtime-classpath.txt`

## 本次关键修改

- 修复 WSL 下前端 dev 启动兼容：
  - `HealthShow/scripts/run-vite-dev.mjs`
- 模拟器支持环境变量切端口/目标：
  - `HealthShow/watch_tcp_simulator_1000.py`
- 新增 WSL 托管脚本和后端直启链路：
  - `tools/health-wsl-*.sh`
- `tools/health-wsl-env.sh` 现已补齐 Java/Maven/Python/Node/npm 自动发现
- `tools/health-wsl-stack.sh` 现已补齐 `tmux` detached 托管，避免前端和模拟器随父会话退出

## 验证口径

本次已验证过的链路：

- 前端页面可访问：`9528`
- 后端健康检查可访问：`8080/health/actuator/health`
- 登录接口可通：`/dev-api/auth/login`
- 模拟器可持续向 `9000` 发包
- 后端日志可见设备登录、健康包、GPS 包、Redis flush、批量写库
