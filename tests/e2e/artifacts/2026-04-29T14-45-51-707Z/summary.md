# Playwright audit report

- run_id: 2026-04-29T14-45-51-707Z
- base_url: http://localhost:9528
- started_at: 2026-04-29T14:45:51.708Z
- finished_at: 2026-04-29T14:47:01.841Z

## Totals

- passed: 26
- passed_with_warnings: 2
- passed_with_issues: 0
- failed: 0
- issue_count: 2

## Routes

| device | route | status | issues | ms | final_hash | screenshot |
| --- | --- | --- | ---: | ---: | --- | --- |
| desktop | safety-command | passed | 0 | 1884 | #/safety-command/index | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-safety-command.jpeg |
| desktop | dashboard | passed | 0 | 2011 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-dashboard.jpeg |
| desktop | workbench | passed | 0 | 1881 | #/health-monitor/workbench | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-workbench.jpeg |
| desktop | real-time | passed | 0 | 3087 | #/health-monitor/real-time | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-real-time.jpeg |
| desktop | heart-rate | passed | 0 | 1916 | #/health-monitor/heart-rate | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-heart-rate.jpeg |
| desktop | pressure | passed | 0 | 4496 | #/health-monitor/pressure | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-pressure.jpeg |
| desktop | blood-pressure | passed | 0 | 1910 | #/health-monitor/blood-pressure | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-blood-pressure.jpeg |
| desktop | blood-oxygen | passed_with_warnings | 1 | 3763 | #/health-monitor/blood-oxygen | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-blood-oxygen.jpeg |
| desktop | sleep | passed | 0 | 1861 | #/health-monitor/sleep | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-sleep.jpeg |
| desktop | risk-warning | passed | 0 | 1872 | #/health-monitor/risk-warning | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-risk-warning.jpeg |
| desktop | employee-archive | passed | 0 | 1898 | #/health-monitor/employee-archive | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-employee-archive.jpeg |
| desktop | mine-entry | passed | 0 | 1909 | #/health-monitor/mine-entry | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-mine-entry.jpeg |
| desktop | report-center | passed | 0 | 2047 | #/health-monitor/report-center | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-report-center.jpeg |
| desktop | trend-warning | passed | 0 | 6680 | #/health-monitor/trend-warning | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-trend-warning.jpeg |
| desktop | alert-notifications | passed_with_warnings | 1 | 2096 | #/alert-management/notifications | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-alert-notifications.jpeg |
| desktop | alert-sos | passed | 0 | 1854 | #/alert-management/sos | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-alert-sos.jpeg |
| desktop | alert-config | passed | 0 | 1864 | #/alert-management/config | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-alert-config.jpeg |
| desktop | alert-records | passed | 0 | 1849 | #/alert-management/records | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-alert-records.jpeg |
| desktop | device-list | passed | 0 | 1869 | #/admin/device-list | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-device-list.jpeg |
| desktop | user-list | passed | 0 | 1856 | #/admin/user-list | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-user-list.jpeg |
| desktop | role-management | passed | 0 | 1857 | #/admin/role | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-role-management.jpeg |
| desktop | department-management | passed | 0 | 1865 | #/admin/department | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-department-management.jpeg |
| desktop | job-type-management | passed | 0 | 1864 | #/admin/job-type | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-job-type-management.jpeg |
| desktop | ai-chat | passed | 0 | 1882 | #/ai-chat/index | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\desktop-ai-chat.jpeg |
| mobile | mobile-safety-command | passed | 0 | 2716 | #/safety-command/index | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\mobile-mobile-safety-command.jpeg |
| mobile | mobile-dashboard | passed | 0 | 1929 | #/health-monitor/dashboard | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\mobile-mobile-dashboard.jpeg |
| mobile | mobile-workbench | passed | 0 | 1904 | #/health-monitor/workbench | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\mobile-mobile-workbench.jpeg |
| mobile | mobile-real-time | passed | 0 | 3402 | #/health-monitor/real-time | tests\e2e\artifacts\2026-04-29T14-45-51-707Z\mobile-mobile-real-time.jpeg |

## Issues

| device | route | type | severity | detail |
| --- | --- | --- | --- | --- |
| desktop | blood-oxygen | console.warning | warn | [ECharts] Can't get DOM width or height. Please check dom.clientWidth and dom.clientHeight. They should not be 0.For example, you may need to call this in the callback of window... |
| desktop | alert-notifications | console.warning | warn | ElementPlusError: [el-pagination] [API] small is about to be deprecated in version 3.0.0, please use size instead.
For more detail, please visit: https://element-plus.org/zh-CN/... |