# Test Baseline

## Local prerequisites

- Preferred WSL stack: `bash /home/j/code/health/tools/health-wsl-stack.sh all start`
- Frontend: `npm run dev`
- Backend: `source /home/j/code/health/tools/health-wsl-env.sh && "$HEALTH_MAVEN_CMD" spring-boot:run -f /home/j/code/health/HealthData/pom.xml`
- Simulator: `python3 watch_tcp_simulator_1000.py`
- Playwright browser: `npm run audit:e2e:install`
- Local self-check: `npm run audit:preflight`
- Pipeline self-check: `npm run audit:preflight:pipeline`

## Main checks

- `npm run audit:nav`
- `npm run audit:page-structure`
- `npm run audit:structure`
- `npm run audit:api`
- `npm run audit:data`
- `npm run audit:write`
- `npm run audit:auth`
- `npm run audit:e2e`
- `npm run audit:pipeline`
- `npm run audit:pipeline-warning`
- `npm run audit:nightly`

## Notes

- `audit:ci` is the safe CI baseline.
- `audit:ci` now includes `audit:structure` to catch menu/route drift and migrated large-page structure drift before build.
- `audit:nav` checks that route/menu/mobile navigation share `src/router/app-routes.mjs` and `src/layout/menu/navigation.mjs` as their source.
- `audit:page-structure` checks that already migrated large pages keep their `index.vue + runtime/view-model/scss/components` module boundaries.
- `audit:data` checks the old data source by default and fails when core page components have structurally valid but empty payloads. Use `API_DATA_SOURCE=new API_EXPECT_NON_EMPTY=0 npm run audit:data` only when validating the intentionally sparse new database.
- `audit:write` is source-aware. On `SQL_DB=health_new` or `API_DATA_SOURCE=new`, write probes that require bound devices, unhandled warnings, or writable AI report employees are recorded as `skipped` when that seed data is absent; auth/config writes still run, and the old data source remains strict.
- `audit:auth` covers the first-round auth contract: prefilled `admin / admin123`, login success, refresh restore, logout, stale-token redirect after backend restart, and concurrent 401 requests converging back to the login page with cookie cleanup.
- `audit:auth` still contains a Windows-hosted backend-restart guard for legacy flows, but day-to-day local execution now prefers WSL stack management.
- `tests/run-full-stack-local.py` is the WSL-native full-stack runner. It must manage backend/frontend/simulator through `tools/health-wsl-stack.sh`, keep `dataSourceFacts` in JSON/Markdown summaries, keep writing `hermes-archive.md`, and log `RESULT SKIPPED <reason>` for allowed empty-state skips. `npm run test:fast` includes `tests/wsl-runner-contract.mjs` to guard that contract.
- `tools/start-health-runner.py` is the WSL-native detached background entry point. It must expose `RUN_STARTED`, `RUN_ALREADY_ACTIVE`, and `DRY_RUN`, and it detaches through `tmux` rather than PowerShell hidden windows.
- Do not run `audit:auth` in parallel with `audit:e2e` or `audit:pipeline`, or you will get false failures.
- `audit:e2e` runs `audit:preflight` first and fails fast with hints if the local environment is incomplete.
- `audit:e2e` keeps the employee archive -> employee profile -> workbench flow on the old data source because `health_new` is expected to have empty `employee/department` bootstrap data.
- Pipeline tests expect Redis, TCP `9000`, and the simulator to be available; `audit:preflight:pipeline` only verifies the detectable prerequisites.
- `audit:pipeline` now pins the browser/API leg to the same data source as `SQL_DB` (`health` -> `old`, `health_new` -> `new`) so the TCP probe, API verification, and Playwright page check stay on one DB route.
- `audit:pipeline` still tries to observe the transient Redis buffer, but it no longer fails solely because the probe flushed to SQL before the poll window caught it; SQL/API/page verification is the hard gate.
- `tests/run-full-stack-local.py` stops the old-source simulator after `test-perf-old` and before `test-full-old`, so `audit:pipeline` and `audit:pipeline-warning` get exclusive TCP probe access instead of racing the 1000-watch simulator for the same IMEI.
