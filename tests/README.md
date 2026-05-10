# Test Baseline

## Local prerequisites

- Frontend: `npm run dev`
- Backend: `mvn spring-boot:run -f D:/Health/HealthData/pom.xml`
- Simulator: `python watch_tcp_simulator_1000.py`
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
- `audit:auth` actively restarts the backend during the stale-token check. It must restart through hidden detached `cmd.exe` with `windowsHide=true` and explicit log file descriptors, not direct `spawn(mvn.cmd)` or inherited stdio, so long local loops do not open a visible Java console or hang on child handles. `npm run test:fast` includes `tests/auth-backend-start-hidden.mjs` to guard this.
- `tests/run-full-stack-local.ps1` must start long-lived services through `Start-NoWindowProcess` (`UseShellExecute=false`, `CreateNoWindow=true`) rather than direct `Start-Process` service startup. The same fast guard blocks regressions that reopen backend/frontend/simulator consoles during long loops.
- `D:/Health/tools/start-health-runner-hidden.ps1` is the stable OpenClaw entry for background runner starts. `npm run test:fast` verifies its `-DryRun` output and guards against nested PowerShell patterns such as `$_.Id` that can be expanded before OpenClaw exec runs the command.
- `tests/run-full-stack-local.ps1` writes `dataSourceFacts` into both JSON and Markdown summaries. The facts record the selected SQL database, core table counts, and `simulatorDeviceRows`; this catches stale simulator IMEI rows in `health_new` even when the new-source run is otherwise an allowed empty-state skip.
- `tests/run-full-stack-local.ps1` writes `hermes-archive.md` from the same summary object as JSON/Markdown so Hermes/OpenClaw can triage a compact run record without scanning combined logs. `npm run test:fast` guards the archive path and writer.
- When `tests/run-full-stack-local.ps1` has only allowed skipped steps, summary status remains `skipped` and the final log line must be `RESULT SKIPPED <reason>`, not `RESULT PASS all steps`. `npm run test:fast` guards this so OpenClaw/Hermes/Codex automation does not mistake an expected new-source empty-state skip for a full pass.
- Do not run `audit:auth` in parallel with `audit:e2e` or `audit:pipeline`, or you will get false failures.
- `audit:e2e` runs `audit:preflight` first and fails fast with hints if the local environment is incomplete.
- `audit:e2e` keeps the employee archive -> employee profile -> workbench flow on the old data source because `health_new` is expected to have empty `employee/department` bootstrap data.
- Pipeline tests expect Redis, TCP `9000`, and the simulator to be available; `audit:preflight:pipeline` only verifies the detectable prerequisites.
- `audit:pipeline` now pins the browser/API leg to the same data source as `SQL_DB` (`health` -> `old`, `health_new` -> `new`) so the TCP probe, API verification, and Playwright page check stay on one DB route.
- `audit:pipeline` still tries to observe the transient Redis buffer, but it no longer fails solely because the probe flushed to SQL before the poll window caught it; SQL/API/page verification is the hard gate.
- `tests/run-full-stack-local.ps1` stops the old-source simulator after `test-perf-old` and before `test-full-old`, so `audit:pipeline` and `audit:pipeline-warning` get exclusive TCP probe access instead of racing the 1000-watch simulator for the same IMEI.
