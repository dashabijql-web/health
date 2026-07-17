# 双库切换说明（2026-05-06）

## 当前状态

- 老库：`health`
- 新库：`health_new`
- 新库已按“可写复制库 + 清空业务流水”方式创建完成
- 保留的数据：
  - `sys_user`
  - `sys_role` / `sys_permission` / `sys_user_role` / `sys_role_permission`
  - `job_type`
  - `alert_config`
- 已清空的数据：
  - `department`
  - `employee`
  - `device`
  - `device_user`
  - `device_data_buffer`
  - `health_record*`
  - `warning_record*`
  - `realtime_data`
  - `user_online_status`
  - `ai_health_report`

## 当前新库核对结果（2026-05-07）

- `department = 0`
- `employee = 0`
- `device = 0`
- `device_user = 0`
- `realtime_data = 0`
- `user_online_status = 0`
- `job_type = 12`
- `alert_config = 20`
- `sys_user = 1`

说明：

- 新库已不再保留组织和员工基础数据，后续需要在 `health_new` 内重建。
- 目前仍保留登录、角色权限、工种、预警阈值等系统基础配置。
- 默认保留可登录超级管理员：`admin / admin123`，并绑定 `SUPER_ADMIN`。

## 代码默认规则

- 无请求头的直接 HTTP 默认走：`old`
- 前端页面默认显式请求：`new`
- 手表数据默认写入：`new`
- 模拟器 IMEI 正则：`^3594567800\\d{5}$`
- 命中上述正则的设备写入：`old`
- Redis 缓冲已按库拆分：
  - `health:buffer:old`
  - `health:buffer:new`

## 前端切换

- 顶栏新增“数据源”下拉，可在“新库 / 老库”之间切换
- 切换后会刷新页面
- 所有请求统一带请求头：`X-Health-Data-Source`
- 下拉框只是前端选择，接口是否真正切库以响应头 `X-Health-Data-Source` 为准
- 后端数据源过滤器早于 Sa-Token 拦截器执行，已改为直接从当前请求的 `satoken` header/cookie 解析用户后授权切库

## 可调环境变量

- `DB_NAME_OLD`：默认 `health`
- `DB_NAME_NEW`：默认 `health_new`
- `HEALTH_DEFAULT_SOURCE`：默认 `new`
- `HEALTH_REQUEST_SOURCE`：默认 `old`
- `HEALTH_WATCH_SOURCE`：默认 `new`
- `HEALTH_SIMULATOR_SOURCE`：默认 `old`
- `HEALTH_SIMULATOR_IMEI_REGEX`：默认 `^3594567800\\d{5}$`

## 指挥中心事件迁移与灰度

- 统一事件接口 `/command-center/incidents` 及其详情、确认、分派、处理、误报、外部动作和时间线均按当前 `X-Health-Data-Source` 路由。
- 事件唯一定位键固定为 `warningId + occurredAt`。预警按月分表，不能仅凭裸 `warningId` 读取、处理或审计事件。
- 部署或切换到尚未迁移的数据库前，必须在 `health` 和 `health_new` 分别执行 `src/main/resources/sql/command_center_incident.sql`。
- 迁移只新增 `command_center_incident` 与 `command_center_incident_action`，不修改历史预警。应用不会运行时建表，因为 Druid SQL 防火墙会拒绝条件 DDL；缺表时接口会返回 `503` 并提示执行迁移。
- 模拟器只写老库。因此老库演示可以验证事件状态、责任人、SLA 和审计；新库空业务态返回空事件列表是预期，不得据此回退到老库数据。
- 前端默认启用 V2。出现页面回归时可将 `VITE_SAFETY_COMMAND_V2=false` 或 `VITE_UNIFIED_CONTROL_V2=false` 后重新构建，原路径保持不变；回滚只影响页面入口，不会删除事件表或审计记录。

## 明天上线前建议核对

1. 真实手表 IMEI 是否不会命中模拟器正则。
2. 若真实手表也可能是 `3594567800xxxxx`，先改 `HEALTH_SIMULATOR_IMEI_REGEX`，再启动后端。
3. 让一台真实手表先连一次，确认它出现在新库 `device` 表。
4. 再跑一次模拟器，确认它只写老库。
