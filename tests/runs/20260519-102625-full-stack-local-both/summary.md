# HealthShow full stack local run

- run_id: 20260519-102625-full-stack-local-both
- status: failed
- status_reason: nonzero steps: 7
- data_source: both
- out_dir: /home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both
- summary_json: /home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/full-stack-local-summary.json
- hermes_archive: /home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/hermes-archive.md
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
| backend-maven-test | passed | 0 | 122323 | backend-maven-test.combined.log |
| backend-regression | failed | 1 | 201 | backend-regression.combined.log |
| test-fast | passed | 0 | 1871 | test-fast.combined.log |
| test-frontend | passed | 0 | 22038 | test-frontend.combined.log |
| test-quality | passed | 0 | 888 | test-quality.combined.log |
| test-integration-old | failed | 3 | 407 | test-integration-old.combined.log |
| test-perf-old | failed | 3 | 412 | test-perf-old.combined.log |
| test-full-old | failed | 3 | 557 | test-full-old.combined.log |
| test-integration-new | failed | 3 | 415 | test-integration-new.combined.log |
| test-perf-new | failed | 3 | 415 | test-perf-new.combined.log |
| test-full-new | failed | 3 | 440 | test-full-new.combined.log |

## Failed Steps

- backend-regression: exit=1, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/backend-regression.combined.log
- test-integration-old: exit=3, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/test-integration-old.combined.log
- test-perf-old: exit=3, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/test-perf-old.combined.log
- test-full-old: exit=3, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/test-full-old.combined.log
- test-integration-new: exit=3, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/test-integration-new.combined.log
- test-perf-new: exit=3, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/test-perf-new.combined.log
- test-full-new: exit=3, log=/home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/test-full-new.combined.log

## Skipped Steps

- none

## Git Status Logs

- HealthShow: /home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/git-healthshow-status.log
- HealthData: /home/j/code/health/HealthShow/tests/runs/20260519-102625-full-stack-local-both/git-healthdata-status.log
