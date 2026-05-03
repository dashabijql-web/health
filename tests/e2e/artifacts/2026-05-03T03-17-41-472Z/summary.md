# Playwright audit report

- run_id: 2026-05-03T03-17-41-472Z
- base_url: http://127.0.0.1:9528
- started_at: 2026-05-03T03:17:41.472Z
- finished_at: 2026-05-03T03:19:02.234Z

## Totals

- passed: 32
- passed_with_warnings: 2
- passed_with_issues: 0
- failed: 0
- issue_count: 14

## Navigation audits

- desktop/sidebar-groups: labels=指挥中心, 安全指挥中心, 统一管控, 监测中心, 实时监控, 心率分析, 压力分析, 血压分析, 血氧分析, 趋势预警, 预警中心, 风险预警, 消息通知中心, 预警记录, 阈值配置, SOS 紧急救援, 人员中心, 职工健康档案库, 入井准入管理, 工作台日历, 报告与AI, 报表中心, AI健康助手, 系统管理, 设备列表, 用户列表, 角色管理, 部门管理, 工种管理
- mobile/bottom-nav: labels=指挥, 监测, 准入, 预警, 人员

## Routes

| device | route | status | issues | ms | final_hash | screenshot |
| --- | --- | --- | ---: | ---: | --- | --- |
| desktop | safety-command | passed | 0 | 1918 | #/safety-command/index | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-safety-command.jpeg |
| desktop | dashboard | passed_with_warnings | 6 | 1999 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-dashboard.jpeg |
| desktop | workbench | passed | 0 | 1871 | #/health-monitor/workbench | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-workbench.jpeg |
| desktop | real-time | passed | 0 | 2992 | #/health-monitor/real-time | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-real-time.jpeg |
| desktop | heart-rate | passed | 0 | 1931 | #/health-monitor/heart-rate | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-heart-rate.jpeg |
| desktop | pressure | passed | 0 | 4409 | #/health-monitor/pressure | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-pressure.jpeg |
| desktop | blood-pressure | passed | 0 | 1924 | #/health-monitor/blood-pressure | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-blood-pressure.jpeg |
| desktop | blood-oxygen | passed | 0 | 3906 | #/health-monitor/blood-oxygen | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-blood-oxygen.jpeg |
| desktop | sleep | passed | 0 | 1868 | #/health-monitor/sleep | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-sleep.jpeg |
| desktop | risk-warning | passed | 0 | 1873 | #/health-monitor/risk-warning | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-risk-warning.jpeg |
| desktop | employee-archive | passed | 0 | 1876 | #/health-monitor/employee-archive | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-employee-archive.jpeg |
| desktop | mine-entry | passed | 0 | 1863 | #/health-monitor/mine-entry | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-mine-entry.jpeg |
| desktop | report-center | passed | 0 | 1901 | #/health-monitor/report-center | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-report-center.jpeg |
| desktop | trend-warning | passed | 0 | 2084 | #/health-monitor/trend-warning | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-trend-warning.jpeg |
| desktop | alert-notifications | passed | 0 | 1986 | #/alert-management/notifications | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-alert-notifications.jpeg |
| desktop | alert-sos | passed | 0 | 1860 | #/alert-management/sos | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-alert-sos.jpeg |
| desktop | alert-config | passed | 0 | 1853 | #/alert-management/config | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-alert-config.jpeg |
| desktop | alert-records | passed | 0 | 1852 | #/alert-management/records | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-alert-records.jpeg |
| desktop | device-list | passed | 0 | 1875 | #/admin/device-list | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-device-list.jpeg |
| desktop | user-list | passed | 0 | 1851 | #/admin/user-list | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-user-list.jpeg |
| desktop | role-management | passed | 0 | 2242 | #/admin/role | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-role-management.jpeg |
| desktop | department-management | passed | 0 | 2770 | #/admin/department | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-department-management.jpeg |
| desktop | job-type-management | passed | 0 | 2263 | #/admin/job-type | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-job-type-management.jpeg |
| desktop | ai-chat | passed | 0 | 1852 | #/ai-chat/index | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-ai-chat.jpeg |
| desktop | command-center-entry | passed_with_warnings | 6 | 1972 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-command-center-entry.jpeg |
| desktop | monitoring-center-entry | passed | 0 | 3015 | #/health-monitor/real-time | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-monitoring-center-entry.jpeg |
| desktop | warning-center-entry | passed | 0 | 1929 | #/alert-management/notifications | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-warning-center-entry.jpeg |
| desktop | people-center-entry | passed | 0 | 2085 | #/health-monitor/employee-archive | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-people-center-entry.jpeg |
| desktop | report-ai-entry | passed | 0 | 3078 | #/health-monitor/report-center | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-report-ai-entry.jpeg |
| desktop | employee-profile-flow | passed | 0 | 2802 | #/health-monitor/employee-profile?empCode=EMP1000&empName=%E7%BD%97%E4%BF%8A | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\desktop-employee-profile-flow.jpeg |
| mobile | mobile-safety-command | passed | 0 | 2353 | #/safety-command/index | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\mobile-mobile-safety-command.jpeg |
| mobile | mobile-dashboard | passed | 0 | 1921 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\mobile-mobile-dashboard.jpeg |
| mobile | mobile-workbench | passed | 0 | 1894 | #/health-monitor/workbench | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\mobile-mobile-workbench.jpeg |
| mobile | mobile-real-time | passed | 0 | 3369 | #/health-monitor/real-time | tests\e2e\artifacts\2026-05-03T03-17-41-472Z\mobile-mobile-real-time.jpeg |

## Issues

| device | route | type | severity | detail |
| --- | --- | --- | --- | --- |
| desktop | login | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | login | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | dashboard | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | dashboard | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | dashboard | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | dashboard | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | dashboard | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | dashboard | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | command-center-entry | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | command-center-entry | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | command-center-entry | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | command-center-entry | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | command-center-entry | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | command-center-entry | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |