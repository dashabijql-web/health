# 竞品驱动改进计划

日期：2026-05-08  
来源：竞品雷达公开资料 + 当前 Health 竞品候选结果  
目标：把“可见差距”转成“可验证、可分期、可回归”的改进路线，优先做低风险高价值项。

## 参考来源

- [Blackline Safety 24/7 Live Monitoring](https://www.blacklinesafety.com/solutions/services/24-7-live-monitoring)
- [VelocityEHS Action Management](https://www.ehs.com/solution/safety/action-management/)
- [位智物联 智慧矿山](https://www.locsmart.cn/mine)
- [小视科技 智慧矿山](https://www.minivision.cn/zhks.html)
- [Honeywell Dashboard Help](https://ss.honeywell.com/help_ssrt/Content/Guide/dashboard.html)
- 当前竞品雷达结果：[tests/runs/20260508-164000-competitor/competitor/findings.md](./runs/20260508-164000-competitor/competitor/findings.md)

## 总体排序

| 顺序 | 方向 | 价值 | 风险 | 首个落点 |
| --- | --- | --- | --- | --- |
| 1 | 预警通知 SLA / 升级链路 | 高 | 中 | `/alert-management/notifications` |
| 2 | 告警动作责任人 / 到期 / 逾期管理 | 高 | 中 | `/alert-management/notifications`、`/alert-management/records` |
| 3 | 安全指挥中心地图联动 | 高 | 高 | `/safety-command/index` |
| 4 | dashboard 可配置 widget 聚合 | 中 | 中 | `/health-monitor/dashboard` |
| 5 | 告警证据图片 / 视频闭环 | 中 | 高 | `/alert-management/records`、`/risk-warning` |

## 1. 预警通知 SLA / 升级链路

### 现状

当前通知列表、预警记录和 dashboard 已能展示状态，但还没有把“响应速度”和“升级状态”作为一等信息呈现。

2026-05-10 第一轮已补强 `/alert-management/notifications`：通知列表现在展示 SLA 状态、剩余/超时时长和到期时间，`warning-lifecycle` 统一派生 `slaClockLabel` / `slaClockText`，并由 `tests/warning-lifecycle.mjs` 守护。

### 目标

把每条待处理预警变成一条可追踪的 SLA 对象，让值班人员一眼看到：

- 这条告警何时产生。
- 还有多少时间到期。
- 谁在处理。
- 是否已经超时。
- 是否进入升级流程。

### 计划改动

- 在 `/alert-management/notifications` 增加 SLA 倒计时、超时标签、升级状态。
- 在 `/alert-management/records` 增加处置时长、责任人、升级原因。
- 在 `/health-monitor/dashboard` 增加“逾期待办数”“升级中告警数”“平均响应时长”摘要卡。
- 统一告警状态枚举，避免通知页、记录页、统计卡三套口径。

### 验收标准

- 待处理预警能显示剩余 SLA 时间。
- 超时告警在列表里有明确视觉标记。
- 升级中的告警可以从通知页跳到记录页看到完整过程。
- dashboard 上能看见逾期和升级数量。

### 回归点

- `audit:api`
- `audit:e2e`
- `audit:pipeline`

## 2. 告警动作责任人 / 到期 / 逾期管理

### 现状

Health 的告警处理更像“列表 + 状态切换”，缺少标准化的动作管理闭环。

### 目标

把告警处理从“看见了”推进到“谁负责、何时完成、是否逾期、是否升级”。

### 计划改动

- 给告警项补责任人字段。
- 给告警项补到期时间字段。
- 允许告警项进入“处理中 / 待升级 / 已完成 / 已关闭”流程。
- 在工作台或通知页增加统一待办视图。
- 提供按部门、责任人、状态、超时程度的筛选。

### 验收标准

- 告警可以分派责任人。
- 告警可以设置到期时间。
- 逾期动作自动进入升级态。
- 工作台能按责任人聚合待办。

### 回归点

- `audit:api`
- `audit:auth`
- `audit:e2e`

## 3. 安全指挥中心地图联动

### 现状

安全指挥中心已经有态势展示，但和实时位置、轨迹、告警点的联动还不够强。

### 目标

把地图变成安全事件的主入口，而不是仅仅一块展示区域。

### 计划改动

- 在 `/safety-command/index` 强化人员 / 设备 / 告警的统一地图层。
- 支持实时定位、轨迹回放、区域高亮、告警点联动。
- 预留 2D / 2.5D / 3D 的扩展接口，但第一期只做 2D 统一态势。
- 把地图上的告警与右侧事件列表双向联动。

### 验收标准

- 点击事件可定位到地图点位。
- 地图高亮能反向驱动列表筛选。
- 支持查看最近一段时间的轨迹或活动路径。

### 风险控制

- 这块属于高风险高成本，不建议和前两项绑在同一轮。
- 先做“统一联动”，不要先做复杂 3D。

### 回归点

- `audit:e2e`
- 桌面和移动端截图
- `audit:pipeline`

## 4. dashboard 可配置 widget 聚合

### 现状

dashboard 已经有足够的数据，但模块组合还偏固定，缺少更清晰的“一屏聚合”模板。

### 目标

让 dashboard 按角色或场景切换模块布局，例如：

- 值班态势。
- 设备态势。
- 风险态势。
- 人员态势。

### 计划改动

- 把 worker status、device status、risk、live events 拆成可复用 widget。
- 提供 2 - 3 套固定布局模板，而不是一次性做自由拖拽。
- 统一卡片尺寸、标题层级和空态样式。
- 让 dashboard 和 real-time 的模块命名、数据口径保持一致。

### 验收标准

- 同一页面能切换不同聚合视图。
- 常用态势模块能稳定复用。
- 不出现组件空白或布局跳变。

### 回归点

- `audit:api`
- `audit:e2e`
- `npm run build`

## 5. 告警证据图片 / 视频闭环

### 现状

当前事件闭环更多是文本和表格，缺少可回放的证据链。

### 目标

让告警和处置记录支持“看得见的证据”，便于复核和追责。

### 计划改动

- 在告警详情里支持图片附件。
- 预留视频证据入口，但第一期可以只做图片 / 截图。
- 给处置记录增加备注、证据、审计时间线。
- 让风险页和记录页能展示关键证据摘要。

### 验收标准

- 告警详情可挂载图片。
- 记录页可看到证据摘要。
- 处置过程可回看。

### 风险控制

- 视频播放、上传和存储是高成本项，先做图片和截图。
- 不要为了证据闭环重构整套媒体系统。

### 回归点

- `audit:e2e`
- `audit:pipeline`
- `npm run build`

## 建议落地节奏

### Phase 1

先做低风险、高价值项：

1. 预警通知 SLA / 升级链路
2. 告警动作责任人 / 到期 / 逾期管理

这两项能直接提升运营效率，也最容易验收。

### Phase 2

再做中高风险项：

3. dashboard 可配置 widget 聚合
4. 告警证据图片闭环

### Phase 3

最后做高风险、大体量项：

5. 安全指挥中心地图联动

## 不建议同时做的事

- 不要把 SLA、地图、视频证据、dashboard 重构一轮全上。
- 不要先做自由拖拽 dashboard。
- 不要先上 3D 地图。
- 不要把视频证据和处置流转一起重构。

## 预期产出

- 1 份可落地的 UI / 数据 / 回归计划。
- 1 组可验证的低风险 issue。
- 1 套能持续扩展的 backlog。
