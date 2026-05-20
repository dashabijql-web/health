# HealthShow full stack local run

- run_id: 20260519-111021-full-stack-local-both
- status: skipped
- status_reason: allowed skipped steps: 1
- data_source: both
- out_dir: /home/j/code/health/HealthShow/tests/runs/20260519-111021-full-stack-local-both
- summary_json: /home/j/code/health/HealthShow/tests/runs/20260519-111021-full-stack-local-both/full-stack-local-summary.json
- hermes_archive: /home/j/code/health/HealthShow/tests/runs/20260519-111021-full-stack-local-both/hermes-archive.md
- latest_pointer: /home/j/code/health/HealthShow/tests/runs/latest-full-stack-local.json

## Readiness

- sql58135: True
- redis6379: True
- backend8080: True
- backendTcp9000: True
- frontend9528: True
- sqlPasswordSet: True
- simulatorRunning: False

## Data Source Facts

- old: database=health, ok=True, department=20, employee=1000, device=1000, device_user=1000, realtime_data=0, user_online_status=0, simulatorDeviceRows=1000
- new: database=health_new, ok=True, department=0, employee=0, device=0, device_user=0, realtime_data=0, user_online_status=0, simulatorDeviceRows=0

## Steps

| step | status | exit | ms | log |
| --- | --- | ---: | ---: | --- |
| backend-maven-test | passed | 0 | 9623 | backend-maven-test.combined.log |
| backend-regression | passed | 0 | 10375 | backend-regression.combined.log |
| test-fast | passed | 0 | 1726 | test-fast.combined.log |
| test-frontend | passed | 0 | 17763 | test-frontend.combined.log |
| test-quality | passed | 0 | 865 | test-quality.combined.log |
| test-integration-old | passed | 0 | 48214 | test-integration-old.combined.log |
| test-perf-old | passed | 0 | 59545 | test-perf-old.combined.log |
| test-full-old | passed | 0 | 294374 | test-full-old.combined.log |
| test-integration-new | passed | 0 | 32842 | test-integration-new.combined.log |
| test-perf-new | passed | 0 | 22311 | test-perf-new.combined.log |
| test-full-new | skipped | 0 | 214091 | test-full-new.combined.log |

## Failed Steps

- none

## Skipped Steps

- test-full-new: exit=0, log=/home/j/code/health/HealthShow/tests/runs/20260519-111021-full-stack-local-both/test-full-new.combined.log

## Git Status Logs

- HealthShow: /home/j/code/health/HealthShow/tests/runs/20260519-111021-full-stack-local-both/git-healthshow-status.log
- HealthData: /home/j/code/health/HealthShow/tests/runs/20260519-111021-full-stack-local-both/git-healthdata-status.log
