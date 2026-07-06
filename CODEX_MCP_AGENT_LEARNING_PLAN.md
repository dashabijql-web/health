# Codex + MCP + Health 项目学习落地计划

创建日期：2026-05-13  
适用目录：`D:/Health`  
定位：把本地 Agent、MCP、Codex 的学习成果落到 Health 项目中，作为后续边学边做的入口文档。

---

## 1. 当前已经掌握的结论

### 1.1 MCP 是什么

MCP 不是模型本身，也不是 Agent 本身。

```text
MCP = 把本地能力包装成工具，交给模型调用的协议
```

一个 MCP 工具通常包含：

```text
工具名
description
inputSchema
handler
返回 content / structuredContent
错误处理
debug log
```

在 `D:/AI-Agent/file-mcp` 中已经实操过：

```text
list_files      列目录
read_file       读文件内容
search_text     搜索文本
get_file_info   查看文件或目录元信息
```

### 1.2 server.js 和 fileTools.js 的关系

在 `file-mcp` 里：

```text
server.js      = MCP 服务入口 / 工具注册层
fileTools.js   = 工具实现层 / 本地文件操作函数
```

调用链是：

```text
Cherry / 其他 MCP 客户端
        ↓ MCP 协议
server.js
        ↓ 普通 JS 函数调用
fileTools.js
        ↓ Node 文件系统 API
本地文件
```

`fileTools.js` 不是客户端，它是 MCP 服务内部的工具实现模块。

### 1.3 Cherry 和 Codex 的分工

当前学习路线调整为：

```text
Codex = 主力本地开发 Agent
Cherry = MCP 客户端学习样本
MCP = 工具协议
GPT-5.5 = 主力模型
Ollama/qwen3 = 可选实验，不当主力
```

原因：

- Codex 本身能读文件、改代码、跑命令、执行测试。
- Cherry 更适合验证“普通聊天客户端如何调用 MCP”。
- 本地小模型在工具调用上不稳定，容易把工具调用 JSON 当普通文本输出。

### 1.4 本地 Agent 的核心风险

本地 Agent 能碰本机文件和命令，所以重点不是“相信模型乖”，而是工具和流程必须有硬边界。

必须关注：

```text
根目录限制
路径越界拒绝
只读工具和写入工具分离
最大读取字节数
忽略 node_modules / .git / dist / build
写入前确认
测试验证
debug log
```

---

## 2. Health 项目里的基本事实

先记住 `D:/Health` 的项目结构：

```text
D:/Health              工作区，不是 git 仓库
D:/Health/HealthShow   前端 git 仓库
D:/Health/HealthData   后端 git 仓库
```

任何 git 操作都必须分别进入子项目：

```powershell
git -C D:\Health\HealthShow status --short
git -C D:\Health\HealthData status --short
```

Health 项目的长期入口：

```text
D:/Health/AGENTS.md
D:/Health/CODEX.md
D:/Health/HEALTH_EXECUTION_ENTRY.md
```

项目技术栈：

```text
前端：Vue 3 + Vite 5 + Vuex 4 + Vue Router 4 + Element Plus + ECharts
后端：Spring Boot 3.2.12 + Java 17 + MyBatis-Plus + Sa-Token + Redis + Netty + SQL Server
```

服务地址：

```text
前端：http://localhost:9528/
后端：http://localhost:8080/health
TCP：9000
登录：admin / admin123
```

---

## 3. 在 Health 项目中怎么用 Codex 学

以后不要只说：

```text
帮我看看 Health 项目
```

这类任务太大，容易导致 Agent 乱探索。

应该按这个模板给 Codex 下任务：

```text
目标：
范围：
限制：
验证：
输出：
```

示例：

```text
目标：只读分析 HealthShow 的登录与权限流程。
范围：只读取 D:/Health/HealthShow/src 下与登录、权限、request、router 相关的文件。
限制：不要改代码，不要启动服务。
验证：用文件引用说明结论来源。
输出：按“调用链 / 关键文件 / 风险点 / 下一步建议”总结。
```

再比如改代码：

```text
目标：修复 HealthShow 某个页面的一个明确问题。
范围：只允许修改相关页面、对应 API 文件、必要测试文件。
限制：不能顺手重构无关模块，不能改全局认证逻辑。
验证：运行项目指定的最小测试命令。
输出：说明改了哪些文件、为什么改、验证结果、剩余风险。
```

---

## 4. Codex 是否靠谱的检查标准

每次让 Codex 干活后，用下面 6 条检查：

```text
1. 它有没有先读相关入口文档和相关代码
2. 它有没有明确区分 HealthShow 和 HealthData
3. 它有没有控制修改范围
4. 它有没有尊重现有未提交改动
5. 它有没有运行合适的验证命令
6. 它最后有没有给出证据，而不是只说“完成了”
```

如果是只读分析任务，结果里应该有文件路径引用。

如果是代码修改任务，结果里应该有：

```text
修改文件
测试命令
测试结果
未验证项
剩余风险
```

---

## 5. Health 项目学习路线

### 第 1 阶段：只读理解项目

目标：不改代码，只让 Codex 帮助建立项目地图。

建议任务：

```text
1. 只读分析 HealthShow 前端入口、路由、鉴权、请求封装。
2. 只读分析 HealthData 后端启动、HTTP 鉴权、Controller-Service-Mapper 层级。
3. 只读分析设备 TCP 上报到数据库写入的链路。
4. 只读分析 old/new 双库切换规则。
5. 只读分析现有测试命令和测试分层。
```

每次分析都要求输出：

```text
调用链
关键文件
关键约束
不要误判的点
下一步可做任务
```

### 第 2 阶段：小范围验证

目标：不改业务，只学习如何启动和验证。

建议任务：

```text
1. 检查 HealthShow / HealthData 工作树状态。
2. 只运行最小前端测试或静态检查。
3. 只运行后端最小回归或 Maven 测试。
4. 学会区分 old 数据源和 new 数据源。
5. 学会看 runtime-logs、tests/runs、summary.md。
```

注意：

- 不要一上来跑 full 流程。
- 启动模拟器前必须确认是否已有模拟器进程。
- 新库为空是预期，不要当成 bug。

### 第 3 阶段：最小代码改动

目标：选一个风险小的问题，走完整 Codex 闭环。

推荐任务类型：

```text
前端文案或展示小修
单个 API 调用路径梳理
单个组件的空态显示优化
单个后端接口返回字段解释
测试脚本小修
文档补充
```

每次改动必须要求：

```text
先读相关文件
说明修改计划
只改必要文件
运行最小验证
记录验证结果
```

### 第 4 阶段：结合 MCP

目标：把 MCP 学习成果用到 Health 项目辅助工具上。

可做方向：

```text
1. Health 只读文件 MCP
   - 限制根目录为 D:/Health
   - 工具只读
   - 支持列目录、读文件、搜索文本、查看文件信息

2. Health 日志查询 MCP
   - 只读 logs/runtime-logs/tests/runs
   - 按时间、关键词、测试 run 查询
   - 不执行命令，不删除日志

3. Health 测试报告 MCP
   - 读取 summary.md
   - 提取 failed、data_source、关键错误
   - 输出结构化测试摘要
```

优先级：

```text
先做只读工具
再做报告解析工具
最后才考虑带执行能力的工具
```

写入类或执行类 MCP 暂时不要急着做。

---

## 6. 推荐下一步任务

下一步建议从只读理解 HealthShow 开始。

推荐给 Codex 的任务：

```text
目标：只读分析 D:/Health/HealthShow 的前端启动、路由、登录鉴权、请求封装流程。
范围：读取 D:/Health/AGENTS.md、D:/Health/CODEX.md、D:/Health/HEALTH_EXECUTION_ENTRY.md，以及 HealthShow/src 下相关入口文件。
限制：不要改代码，不要启动服务，不要运行测试。
验证：每个结论都引用具体文件路径。
输出：按“整体调用链 / 关键文件 / 登录鉴权流程 / 请求封装流程 / 后续学习建议”总结。
```

完成后，再做后端：

```text
目标：只读分析 D:/Health/HealthData 的后端启动、HTTP 鉴权、Controller-Service-Mapper 调用结构。
范围：读取 HealthData/pom.xml、启动类、配置文件、认证配置、一个典型业务模块。
限制：不要改代码，不要启动服务。
验证：每个结论都引用具体文件路径。
输出：按“启动链路 / 鉴权链路 / 典型接口链路 / 数据访问层 / 后续学习建议”总结。
```

---

## 7. 学习判断标准

不用追求一次看懂整个 Health。

每一轮只要能回答下面几个问题，就算有效：

```text
这个任务碰了哪个子项目：HealthShow 还是 HealthData？
入口文件在哪里？
核心调用链是什么？
哪些文件是配置，哪些文件是真正业务逻辑？
这次有没有改代码？
如果改了，用什么命令验证？
有没有 old/new 数据源差异？
有没有现有文档或代码和结论冲突？
```

---

## 8. 当前学习主线

当前主线不是继续证明 MCP 能不能用，而是：

```text
用 Codex 学会指挥本地 Agent
用 Health 项目训练任务拆解和验证
用 MCP 作为后续扩展本地工具能力的方式
```

短期不要追求“大而全 Agent”。

先做到：

```text
只读分析准确
修改范围可控
验证命令明确
结果有证据
文档能沉淀
```

