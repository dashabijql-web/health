# Hermes + Codex 执行方案（D:\Health）

> 入口更新：后续 Health 测试闭环、自主测试、Hermes/OpenClaw/Codex 分工和竞品雷达，以 `D:/Health/HEALTH_AUTONOMOUS_EVOLUTION_RUNBOOK.md` 为第一执行入口。本文件保留为 Hermes + Codex 协作模型历史参考。

最后更新：2026-05-08

## 1. 目标

在 `D:\Health` 上建立一条半自动闭环：

`Hermes 长时间跑测试 -> 归档结构化结果 -> 生成修复任务包 -> Codex 定点修复 -> Hermes 回归验证`

这条链路的核心不是让 Hermes 直接改代码，而是：

- `Hermes` 负责测试编排、长时间观察、失败归并、任务包生成、回归验证。
- `Codex` 负责读取任务包、分析根因、修改代码、跑对应验证。

测试方法总入口：

- `D:\Health\HEALTH_TEST_METHOD.md`
- `D:\Health\HEALTH_TEST_TOOLCHAIN_GUIDE.md`

说明：本文定义 Hermes 和 Codex 的协作链路；具体怎么区分新库/旧库、怎么跑数据密度、性能、功能闭环、页面截图和双库隔离测试，以 `HEALTH_TEST_METHOD.md` 为准。Hermes、OpenClaw、Codex、skills、MCP、脚本怎么组合使用，以 `HEALTH_TEST_TOOLCHAIN_GUIDE.md` 为准。

## 2. 当前项目事实

### 2.1 仓库边界

- `D:\Health` 不是 git 仓库。
- `D:\Health\HealthShow` 是前端独立 git 仓库。
- `D:\Health\HealthData` 是后端独立 git 仓库。
- 所有 `git status / git diff / commit` 必须分别在子项目目录内执行。

### 2.2 技术栈

- 前端：`D:\Health\HealthShow`
  - Vue 3
  - Vite 5
  - Vue Router 4
  - Vuex 4
  - Element Plus
  - Playwright 已接入
- 后端：`D:\Health\HealthData`
  - Spring Boot `3.2.12`
  - Java `17`
  - MyBatis-Plus
  - Redis
  - Netty
  - SQL Server

### 2.3 当前本地入口

- 前端地址：`http://localhost:9528/`
- 后端地址：`http://localhost:8080/health`
- 登录账号：`admin / admin123`
- 前端 token 主要存放在 Cookie

### 2.4 当前已有测试能力

前端已有现成脚本：

- `npm run audit:api`
- `npm run audit:write`
- `npm run audit:auth`
- `npm run audit:e2e`
- `npm run audit:pipeline`
- `npm run audit:pipeline-warning`
- `npm run audit:nightly`

后端已有现成脚本：

- `python D:\Health\HealthData\scripts\run_backend_regression.py`

不要重造一套平行测试体系，优先复用这些入口。

## 3. 角色分工

### 3.1 Hermes 职责

- 启动和编排测试
- 维护测试运行日志和产物目录
- 把失败结果整理成结构化 issue
- 合并重复失败
- 区分新问题 / 已知问题 / 回归问题
- 为 Codex 生成“修复任务包”
- 在 Codex 修改后重跑对应验证

### 3.2 Codex 职责

- 读取 Hermes 生成的单个任务包
- 在 `HealthShow` 或 `HealthData` 内定位根因
- 做最小修复
- 运行任务包中指定的验证命令
- 输出修复说明、影响面和未解决风险

### 3.3 明确禁止

- 不允许 Hermes 直接大面积修改生产代码
- 不允许 Codex 接收海量原始日志后自由发挥
- 不允许一次把多个不相关失败打包给 Codex
- 不允许在 `D:\Health` 根目录直接做 git 操作

## 4. 推荐落地范围

第一阶段只覆盖最值钱的 4 类问题：

- 登录与会话回归
- 健康 API 基础可用性
- 健康写入链路 / pipeline 回归
- 核心 E2E 页面与主路径回归

不建议第一阶段就覆盖：

- 全量 UI 像素检查
- 全天候 soak test
- 自动提交 / 自动合并
- 多问题并行自动修复

## 5. 目录规范

Hermes 所有新产物统一落到：

- `D:\Health\tests\runs\<run-id>\summary.json`
- `D:\Health\tests\runs\<run-id>\summary.md`
- `D:\Health\tests\runs\<run-id>\issues\<issue-id>.json`
- `D:\Health\tests\runs\<run-id>\issues\<issue-id>.md`
- `D:\Health\tests\runs\<run-id>\logs\...`
- `D:\Health\tests\runs\<run-id>\screenshots\...`
- `D:\Health\tests\runs\<run-id>\videos\...`
- `D:\Health\tests\runs\<run-id>\artifacts\...`

另外维护一个稳定索引：

- `D:\Health\tests\latest-run.json`
- `D:\Health\tests\open-issues.json`

## 6. 测试执行命令

### 6.1 启动命令

后端：

```powershell
JAVA_HOME="C:/Program Files/Java/jdk-17" /d/apache-maven-3.8.1/bin/mvn spring-boot:run -f D:/Health/HealthData/pom.xml
```

前端：

```powershell
cd D:\Health\HealthShow
npm run dev
```

模拟器：

```powershell
cd D:\Health\HealthShow
python watch_tcp_simulator_1000.py
```

### 6.2 第一阶段标准测试集

后端：

```powershell
python D:\Health\HealthData\scripts\run_backend_regression.py
```

前端：

```powershell
cd D:\Health\HealthShow
npm run audit:api
npm run audit:auth
npm run audit:e2e
npm run audit:pipeline
```

如需更重的一轮：

```powershell
cd D:\Health\HealthShow
npm run audit:nightly
```

## 7. 运行模式

### 7.1 第一阶段：手动触发

推荐先不要做定时任务。先让 Hermes 在人工触发下跑：

1. 启动前后端
2. 运行标准测试集
3. 生成任务包
4. 人工确认一个任务包
5. 交给 Codex 修复
6. Hermes 回归验证

### 7.2 第二阶段：定时触发

第一阶段稳定后，再考虑：

- 每晚跑一次 `audit:nightly`
- 每次后端结构改动后跑一次后端回归
- 每次前端鉴权或核心大盘改动后跑 `audit:auth + audit:e2e`

## 8. 任务包格式

Hermes 输出给 Codex 的最小任务包格式如下：

```json
{
  "issue_id": "healthshow-auth-20260507-001",
  "project": "HealthShow",
  "category": "auth",
  "title": "backend 重启后前端未正确清理过期会话",
  "severity": "high",
  "stage": "regression",
  "failing_command": "cd D:/Health/HealthShow && npm run audit:auth",
  "environment": {
    "frontend": "http://localhost:9528",
    "backend": "http://localhost:8080/health",
    "login": "admin/admin123"
  },
  "repro_steps": [
    "登录 admin/admin123",
    "重启 backend",
    "刷新前端页面"
  ],
  "expected": "跳回登录页并清理无效 cookie",
  "actual": "页面停留在受保护路由，接口持续 401",
  "failure_signature": "concurrent 401 requests did not converge to login",
  "suspected_files": [
    "D:/Health/HealthShow/src/permission.js",
    "D:/Health/HealthShow/src/utils/request.js"
  ],
  "log_excerpt": "auth-session-regression 失败摘要",
  "artifact_paths": [
    "D:/Health/tests/runs/20260507-220000/logs/auth-session-regression.log",
    "D:/Health/tests/runs/20260507-220000/screenshots/auth-session.png"
  ],
  "verification_command": "cd D:/Health/HealthShow && npm run audit:auth",
  "regression_risk": "登录态、401 收敛、cookie 清理"
}
```

### 8.1 必填字段

- `issue_id`
- `project`
- `category`
- `title`
- `severity`
- `failing_command`
- `repro_steps`
- `expected`
- `actual`
- `failure_signature`
- `suspected_files`
- `artifact_paths`
- `verification_command`

### 8.2 issue 分类建议

- `auth`
- `api`
- `write-regression`
- `pipeline`
- `e2e`
- `backend-regression`
- `dual-db`

## 9. Hermes 执行流程

### 9.1 单轮执行

Hermes 每一轮按这个顺序执行：

1. 确认前后端可用
2. 记录环境信息
3. 执行测试命令
4. 收集 stdout/stderr
5. 拷贝测试脚本自身产物
6. 解析失败结果
7. 聚类成 issue
8. 输出 `summary.json / summary.md`
9. 为每个 issue 生成单独任务包

### 9.2 聚类规则

Hermes 不要把每个失败都当独立问题。至少按下面规则做聚类：

- 同一命令、同一失败签名：合并
- 同一页面、同一接口、同一断言：合并
- 后端 compile 失败：单独视为阻断级问题
- 登录链路失败：优先级高于一般页面回归
- pipeline 链路失败：高于纯展示类问题

### 9.3 严重级别建议

- `blocker`
  - 后端无法启动
  - 前端无法启动
  - 登录完全失效
  - API 主入口全部失败
- `high`
  - 核心业务写入失败
  - 核心大盘不可用
  - 双库切换错误
- `medium`
  - 某个业务页面失效
  - 某个回归断言失败
- `low`
  - 非主路径 UI 问题
  - 文案、样式、轻微 warning

## 10. Codex 执行流程

Codex 处理单个 issue 时必须遵守：

1. 只处理一个 issue
2. 先读任务包，再读相关代码
3. 先跑任务包里的验证命令复现
4. 做最小范围修复
5. 只在对应子仓库内查看 git 状态
6. 修复后先跑 issue 对应命令
7. 如果问题涉及鉴权 / 核心大盘 / 双库，再补一轮相关回归

Codex 输出结果至少包含：

- 修改摘要
- 根因判断
- 实际修改文件
- 验证命令
- 验证结果
- 残余风险

## 11. Hermes 发给 Codex 的标准提示词

下面这段可以直接给 Hermes 作为生成修复任务时的固定模板：

```text
你正在为 D:\Health 生成一个给 Codex 的修复任务。

约束：
- 一次只生成一个问题的任务包
- 不要泛泛总结
- 不要输出多个不相关问题
- 必须明确 project 是 HealthShow 或 HealthData
- 必须给出可直接执行的 verification_command
- 必须给出 suspected_files
- 必须给出 log_excerpt 和 artifact_paths

输出格式：
1. 一份 issue json
2. 一份给 Codex 的简短 markdown 说明

目标：
让 Codex 在最少上下文下也能复现、定位、修复并验证这个问题。
```

## 12. 给 Codex 的标准提示词

下面这段可以直接交给另一个 Codex：

```text
你正在处理 D:\Health 的单个修复任务。

项目约束：
- D:\Health 本身不是 git 仓库
- 只能在 D:\Health\HealthShow 或 D:\Health\HealthData 内做 git 操作
- 优先复用现有测试命令，不要另造脚本
- 先复现，再修复，再验证
- 不要顺手改无关问题

你的输入包括：
- issue json
- 失败命令
- 可疑文件
- 日志摘录
- 验证命令

你的任务：
1. 复现问题
2. 定位根因
3. 做最小修复
4. 跑 verification_command
5. 输出修改摘要、验证结果、残余风险

如果 issue 属于 auth / pipeline / dual-db，必须额外检查是否影响：
- 登录态
- 主数据源切换
- 核心页面主路径
```

## 13. 里程碑

### 里程碑 1：测试收口

完成标准：

- Hermes 能稳定跑：
  - 后端回归
  - `audit:api`
  - `audit:auth`
  - `audit:e2e`
  - `audit:pipeline`
- 每轮都能落盘产物

### 里程碑 2：任务包可用

完成标准：

- Hermes 能把失败转成结构化 issue
- 单个 issue 足够让 Codex 独立修复

### 里程碑 3：单问题闭环

完成标准：

- Codex 能凭任务包修复至少 1 个问题
- Hermes 能正确判断修复成功或失败

### 里程碑 4：稳定回归

完成标准：

- 连续 3 轮都能：
  - 跑测试
  - 生成任务包
  - 修复
  - 回归

## 14. 风险与停止条件

以下情况应暂停自动闭环，转为人工判断：

- 前后端启动本身不稳定
- 测试结果随机波动很大
- 多个 issue 明显共享根因但无法区分
- 双库逻辑改动期间
- 登录链路、cookie、请求头、数据源切换同时变化

出现以下情况时，不要继续把任务自动下发给 Codex：

- 后端 compile 失败
- 前端 dev/build 失败
- 数据库连接失败
- Redis / TCP pipeline 环境异常

## 15. 推荐起步顺序

建议执行顺序如下：

1. 先实现 Hermes 测试编排和产物归档
2. 再实现 issue 聚类和任务包生成
3. 再让 Codex 处理单个 issue
4. 最后才考虑定时任务和全自动回归

最小成功标准：

- 不追求自动修复全部问题
- 只要求第一轮能稳定完成一个问题闭环

## 16. 结论

对 `D:\Health` 来说，最合理的路线不是重做测试平台，而是：

- 复用现有 `HealthShow` 与 `HealthData` 测试入口
- 让 Hermes 负责长时间测试、归档、分诊、回归
- 让 Codex 只吃结构化任务包做定点修复

先把一条稳定的半自动闭环做出来，再考虑扩展到夜间长期运行。

## 17. 页面美观与交互测试策略

Hermes 可以承担 `HealthShow` 页面“美观 + 交互 bug”测试的编排工作，但必须明确：

- `Hermes` 不是浏览器测试引擎本体
- 真正执行页面点击、截图、断言的应是 `Playwright` 或现有 e2e 脚本
- Hermes 负责调用这些测试、汇总结果、生成任务包

### 17.1 适合自动化判断的页面问题

Hermes 应优先关注这些“可观察、可截图、可断言”的问题：

- 页面是否正常加载
- 路由切换后是否空白
- 按钮点击后是否无响应
- 表单提交后是否无反馈
- 弹窗、抽屉、分页、筛选是否可正常操作
- 表格、卡片、图表是否错位
- 文本是否被截断
- 元素是否重叠、溢出、遮挡
- 移动端和桌面端是否布局塌陷
- 控制台是否出现明显报错
- 接口失败时页面是否进入错误状态

### 17.2 不适合完全自动化裁定的问题

以下问题可以做辅助点评，但不要把自动判断结果当最终结论：

- 页面“是否高级”
- 配色“是否更好看”
- 视觉风格“是否更统一”
- 页面“是否有设计感”

换句话说：

- 自动化适合发现“明显难看 / 明显错 / 明显坏”
- 主观审美仍应保留人工判断

### 17.3 对 D:\Health 推荐覆盖的 UI 测试类型

#### A. 可用性测试

目标：

- 页面能打开
- 核心按钮能点击
- 核心流程能走通

建议覆盖：

- 登录页
- 安全指挥中心
- dashboard
- workbench
- real-time
- risk-warning
- employee-archive
- user-list

#### B. 稳定性交互测试

目标：

- 点击和切换不会触发前端异常
- 异常接口下页面能正确降级

建议覆盖：

- 登录 / 刷新 / 退出
- 列表筛选
- 表格分页
- 弹窗开关
- 抽屉详情打开关闭
- tab 切换
- 数据源切换（老库 / 新库）

#### C. 布局质量测试

目标：

- 桌面和移动端主要视口下页面不变形

建议检查：

- 横向溢出
- 垂直遮挡
- 按钮挤压
- 文本截断
- 图表容器宽高为 0
- 弹窗超出视口
- 表格列严重错位

#### D. 视觉回归测试

目标：

- 修复代码后，页面没有明显回退

建议做法：

- 保存关键页面基线截图
- 改动后重新截图
- Hermes 只在差异超过阈值时创建 issue

### 17.4 推荐视口

建议至少覆盖：

- Desktop:
  - `1440 x 900`
  - `1920 x 1080`
- Mobile:
  - `390 x 844`
  - `414 x 896`

如第一阶段负担过重，可先只跑：

- `1440 x 900`
- `390 x 844`

### 17.5 第一阶段页面清单

第一阶段不要全量扫所有页面，优先盯这些：

- `/login`
- `/safety-command/index`
- `/health-monitor/dashboard`
- `/health-monitor/workbench`
- `/health-monitor/real-time`
- `/health-monitor/risk-warning`
- `/health-monitor/employee-archive`
- `/user-list/index`

### 17.6 页面测试结果格式

Hermes 对页面问题生成的 issue 至少应带这些字段：

```json
{
  "issue_id": "healthshow-ui-20260507-001",
  "project": "HealthShow",
  "category": "ui-e2e",
  "title": "安全指挥中心桌面视口下 KPI 卡片遮挡地图区域",
  "severity": "medium",
  "page": "/safety-command/index",
  "viewport": "1440x900",
  "interaction": "页面加载后默认态",
  "expected": "卡片与地图区块不重叠，信息可完整查看",
  "actual": "KPI 卡片覆盖地图顶部，部分按钮不可见",
  "failure_signature": "layout-overlap:kpi-map",
  "artifact_paths": [
    "D:/Health/tests/runs/20260507-220000/screenshots/safety-command-overlap.png"
  ],
  "suspected_files": [
    "D:/Health/HealthShow/src/views/safety-command/index.vue",
    "D:/Health/HealthShow/src/views/safety-command/safety-command.scss"
  ],
  "verification_command": "cd D:/Health/HealthShow && npm run audit:e2e"
}
```

### 17.7 Hermes 对 UI 问题的判断规则

建议 Hermes 遵守以下分级：

- `blocker`
  - 页面打不开
  - 白屏
  - 关键按钮完全不可点击
- `high`
  - 关键流程中断
  - 主视图严重错位
  - 数据源切换后页面不可用
- `medium`
  - 主要区域遮挡
  - 图表/表格错位
  - 文本明显截断
- `low`
  - 间距不一致
  - 单个非关键元素轻微偏移

### 17.8 推荐给 Hermes 的 UI 测试提示词

```text
你正在为 D:\Health 的 HealthShow 前端执行页面质量巡检。

你的职责不是主观评论“好不好看”，而是发现可观察、可复现的 UI 与交互问题。

重点关注：
- 页面是否能正常加载
- 是否有白屏、空白、卡死
- 按钮、弹窗、抽屉、分页、筛选是否可正常操作
- 元素是否重叠、错位、被截断、超出视口
- 图表容器是否出现 0 宽高或渲染异常
- 桌面和移动端主要视口是否布局塌陷
- 控制台错误、接口异常是否导致页面不可用

不要输出泛泛的审美评价。
只在问题可复现、可截图、可归档时创建 issue。
每个 issue 必须带页面路径、视口、交互动作、截图路径、怀疑文件、验证命令。
```

### 17.9 推荐给 Codex 的 UI 修复提示词

```text
你正在修复 D:\Health\HealthShow 的单个 UI/交互问题。

约束：
- 先复现再修改
- 优先最小改动
- 保持现有布局体系与组件风格
- 修改后至少验证对应页面桌面端和移动端主要视口
- 如涉及安全指挥中心、dashboard、workbench、real-time，视为高风险改动

任务目标：
- 修复 issue 中描述的页面错位、遮挡、按钮无响应或交互异常
- 输出修改说明、验证结果、残余风险
```

### 17.10 实际落地建议

对 `D:\Health` 来说，页面美观与交互 bug 自动测试的正确落地方式是：

- 复用现有 `audit:e2e`
- 按页面补充更细的交互脚本
- 为关键页面建立截图基线
- 让 Hermes 调度这些脚本并汇总问题
- 让 Codex 只处理结构化 UI issue

不要让 Hermes 自己主观评价设计。
让它只负责发现“明确可验证的问题”，这样最稳。
