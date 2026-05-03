# Playwright audit report

- run_id: 2026-05-03T02-59-05-262Z
- base_url: http://127.0.0.1:9528
- started_at: 2026-05-03T02:59:05.263Z
- finished_at: 2026-05-03T03:00:21.082Z

## Totals

- passed: 33
- passed_with_warnings: 0
- passed_with_issues: 0
- failed: 0
- issue_count: 0

## Navigation audits

- desktop/sidebar-groups: labels=指挥中心, 安全指挥中心, 统一管控, 监测中心, 实时监控, 心率分析, 压力分析, 血压分析, 血氧分析, 趋势预警, 预警中心, 风险预警, 消息通知中心, 预警记录, 阈值配置, SOS 紧急救援, 人员中心, 职工健康档案库, 入井准入管理, 工作台日历, 报告与AI, 报表中心, AI健康助手, 系统管理, 设备列表, 用户列表, 角色管理, 部门管理, 工种管理
- mobile/bottom-nav: labels=指挥, 监测, 准入, 预警, 人员

## Routes

| device | route | status | issues | ms | final_hash | screenshot |
| --- | --- | --- | ---: | ---: | --- | --- |
| desktop | safety-command | passed | 0 | 1999 | #/safety-command/index | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-safety-command.jpeg |
| desktop | dashboard | passed | 0 | 1999 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-dashboard.jpeg |
| desktop | workbench | passed | 0 | 1861 | #/health-monitor/workbench | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-workbench.jpeg |
| desktop | real-time | passed | 0 | 2979 | #/health-monitor/real-time | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-real-time.jpeg |
| desktop | heart-rate | passed | 0 | 1897 | #/health-monitor/heart-rate | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-heart-rate.jpeg |
| desktop | pressure | passed | 0 | 4440 | #/health-monitor/pressure | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-pressure.jpeg |
| desktop | blood-pressure | passed | 0 | 1896 | #/health-monitor/blood-pressure | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-blood-pressure.jpeg |
| desktop | blood-oxygen | passed | 0 | 3869 | #/health-monitor/blood-oxygen | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-blood-oxygen.jpeg |
| desktop | sleep | passed | 0 | 1874 | #/health-monitor/sleep | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-sleep.jpeg |
| desktop | risk-warning | passed | 0 | 1890 | #/health-monitor/risk-warning | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-risk-warning.jpeg |
| desktop | employee-archive | passed | 0 | 1894 | #/health-monitor/employee-archive | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-employee-archive.jpeg |
| desktop | mine-entry | passed | 0 | 1895 | #/health-monitor/mine-entry | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-mine-entry.jpeg |
| desktop | report-center | passed | 0 | 1945 | #/health-monitor/report-center | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-report-center.jpeg |
| desktop | trend-warning | passed | 0 | 2068 | #/health-monitor/trend-warning | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-trend-warning.jpeg |
| desktop | alert-notifications | passed | 0 | 2046 | #/alert-management/notifications | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-alert-notifications.jpeg |
| desktop | alert-sos | passed | 0 | 1861 | #/alert-management/sos | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-alert-sos.jpeg |
| desktop | alert-config | passed | 0 | 1855 | #/alert-management/config | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-alert-config.jpeg |
| desktop | alert-records | passed | 0 | 1865 | #/alert-management/records | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-alert-records.jpeg |
| desktop | device-list | passed | 0 | 1864 | #/admin/device-list | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-device-list.jpeg |
| desktop | user-list | passed | 0 | 1862 | #/admin/user-list | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-user-list.jpeg |
| desktop | role-management | passed | 0 | 1863 | #/admin/role | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-role-management.jpeg |
| desktop | department-management | passed | 0 | 1885 | #/admin/department | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-department-management.jpeg |
| desktop | job-type-management | passed | 0 | 1895 | #/admin/job-type | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-job-type-management.jpeg |
| desktop | ai-chat | passed | 0 | 1861 | #/ai-chat/index | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-ai-chat.jpeg |
| desktop | command-center-entry | passed | 0 | 1962 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-command-center-entry.jpeg |
| desktop | monitoring-center-entry | passed | 0 | 2588 | #/health-monitor/real-time | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-monitoring-center-entry.jpeg |
| desktop | warning-center-entry | passed | 0 | 1914 | #/alert-management/notifications | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-warning-center-entry.jpeg |
| desktop | people-center-entry | passed | 0 | 2087 | #/health-monitor/employee-archive | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-people-center-entry.jpeg |
| desktop | report-ai-entry | passed | 0 | 3113 | #/health-monitor/report-center | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\desktop-report-ai-entry.jpeg |
| mobile | mobile-safety-command | passed | 0 | 2366 | #/safety-command/index | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\mobile-mobile-safety-command.jpeg |
| mobile | mobile-dashboard | passed | 0 | 1925 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\mobile-mobile-dashboard.jpeg |
| mobile | mobile-workbench | passed | 0 | 1893 | #/health-monitor/workbench | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\mobile-mobile-workbench.jpeg |
| mobile | mobile-real-time | passed | 0 | 3370 | #/health-monitor/real-time | tests\e2e\artifacts\2026-05-03T02-59-05-262Z\mobile-mobile-real-time.jpeg |

## Issues

| device | route | type | severity | detail |
| --- | --- | --- | --- | --- |
| - | - | - | - | none |