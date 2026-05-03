# Playwright audit report

- run_id: 2026-04-11T06-22-37-541Z
- base_url: http://127.0.0.1:4173
- started_at: 2026-04-11T06:22:37.542Z
- finished_at: 2026-04-11T06:23:53.614Z

## Totals

- passed: 26
- passed_with_warnings: 2
- passed_with_issues: 0
- failed: 0
- issue_count: 2

## Routes

| device | route | status | issues | ms | final_hash | screenshot |
| --- | --- | --- | ---: | ---: | --- | --- |
| desktop | safety-command | passed | 0 | 1874 | #/safety-command/index | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-safety-command.jpeg |
| desktop | dashboard | passed | 0 | 2010 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-dashboard.jpeg |
| desktop | workbench | passed | 0 | 1869 | #/health-monitor/workbench | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-workbench.jpeg |
| desktop | real-time | passed | 0 | 1921 | #/health-monitor/real-time | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-real-time.jpeg |
| desktop | heart-rate | passed | 0 | 1903 | #/health-monitor/heart-rate | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-heart-rate.jpeg |
| desktop | pressure | passed | 0 | 1928 | #/health-monitor/pressure | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-pressure.jpeg |
| desktop | blood-pressure | passed | 0 | 1912 | #/health-monitor/blood-pressure | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-blood-pressure.jpeg |
| desktop | blood-oxygen | passed_with_warnings | 1 | 1905 | #/health-monitor/blood-oxygen | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-blood-oxygen.jpeg |
| desktop | sleep | passed | 0 | 1873 | #/health-monitor/sleep | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-sleep.jpeg |
| desktop | risk-warning | passed | 0 | 1867 | #/health-monitor/risk-warning | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-risk-warning.jpeg |
| desktop | employee-archive | passed | 0 | 1884 | #/health-monitor/employee-archive | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-employee-archive.jpeg |
| desktop | mine-entry | passed | 0 | 1861 | #/health-monitor/mine-entry | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-mine-entry.jpeg |
| desktop | report-center | passed | 0 | 1909 | #/health-monitor/report-center | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-report-center.jpeg |
| desktop | trend-warning | passed | 0 | 6771 | #/health-monitor/trend-warning | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-trend-warning.jpeg |
| desktop | alert-notifications | passed_with_warnings | 1 | 2222 | #/alert-management/notifications | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-alert-notifications.jpeg |
| desktop | alert-sos | passed | 0 | 1855 | #/alert-management/sos | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-alert-sos.jpeg |
| desktop | alert-config | passed | 0 | 1859 | #/alert-management/config | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-alert-config.jpeg |
| desktop | alert-records | passed | 0 | 1867 | #/alert-management/records | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-alert-records.jpeg |
| desktop | device-list | passed | 0 | 1876 | #/admin/device-list | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-device-list.jpeg |
| desktop | user-list | passed | 0 | 1864 | #/admin/user-list | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-user-list.jpeg |
| desktop | role-management | passed | 0 | 1859 | #/admin/role | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-role-management.jpeg |
| desktop | department-management | passed | 0 | 1863 | #/admin/department | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-department-management.jpeg |
| desktop | job-type-management | passed | 0 | 1872 | #/admin/job-type | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-job-type-management.jpeg |
| desktop | ai-chat | passed | 0 | 1865 | #/ai-chat/index | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\desktop-ai-chat.jpeg |
| mobile | mobile-safety-command | passed | 0 | 2382 | #/safety-command/index | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\mobile-mobile-safety-command.jpeg |
| mobile | mobile-dashboard | passed | 0 | 1919 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\mobile-mobile-dashboard.jpeg |
| mobile | mobile-workbench | passed | 0 | 1908 | #/health-monitor/workbench | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\mobile-mobile-workbench.jpeg |
| mobile | mobile-real-time | passed | 0 | 3588 | #/health-monitor/real-time | tests\e2e\artifacts\2026-04-11T06-22-37-541Z\mobile-mobile-real-time.jpeg |

## Issues

| device | route | type | severity | detail |
| --- | --- | --- | --- | --- |
| desktop | blood-oxygen | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | alert-notifications | console.warning | warn | ElementPlusError: [el-pagination] [API] small is about to be deprecated in version 3.0.0, please use size instead.
For more detail, please visit: https://element-plus.org/zh-CN/... |