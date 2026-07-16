import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'

const fullStackRunnerPath = path.resolve('tests', 'run-full-stack-local.py')
const auditWriteOncePath = path.resolve('tests', 'run-audit-write-once.py')
const wslRunnerWrapperPath = path.resolve('..', 'tools', 'start-health-runner.py')
const healthLoopRunnerPath = path.resolve('..', 'tests', 'run-health-loop.py')
const watchCapturePath = path.resolve('..', 'tools', 'watch-capture', 'run_three_hour_capture.py')
const watchCaptureDebugPath = path.resolve('..', 'tools', 'watch-capture', 'run_three_hour_backend_debug_capture.py')
const stackManagerPath = path.resolve('..', 'tools', 'health-wsl-stack.sh')

test('WSL stack manager rejects half-closed backend instances', () => {
  const source = fs.readFileSync(stackManagerPath, 'utf8')

  assert.match(source, /backend_healthy/, 'backend lifecycle must verify actuator health, not only process presence')
  assert.match(source, /health=DOWN/, 'backend status must expose an unhealthy running process')
  assert.match(source, /process is running but unhealthy; restarting/, 'backend start must replace half-closed instances')
  assert.match(source, /wait_for_port_closed/, 'backend stop must wait until HTTP and TCP listeners are released')
  assert.match(source, /kill -TERM -- "-\$process_group"/, 'tmux shutdown must terminate the complete backend process group')
})

test('WSL full stack runner uses the WSL stack manager and preserves summary contracts', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8')

  assert.match(source, /health-wsl-stack\.sh/, 'runner must manage backend/frontend/simulator through the WSL stack script')
  assert.match(source, /test-full-old/, 'runner must still execute the old-source full gate')
  assert.match(source, /test-full-new/, 'runner must still execute the new-source full gate')
  assert.match(source, /old-full-pipeline-exclusive-tcp/, 'runner must still stop the old-source simulator before exclusive TCP probes')
  assert.match(source, /simulatorDeviceRows/, 'runner summary must keep simulator contamination facts in dataSourceFacts')
  assert.match(source, /hermes-archive\.md/, 'runner must keep writing a hermes archive for automation triage')
  assert.match(source, /RESULT SKIPPED/, 'runner must distinguish allowed skipped runs from full passes')
})

test('WSL detached runner wrapper exposes dry-run and single-session contracts', () => {
  const source = fs.readFileSync(wslRunnerWrapperPath, 'utf8')

  assert.match(source, /tmux/, 'wrapper must detach through tmux in WSL')
  assert.match(source, /RUN_STARTED pid=/, 'wrapper must print a stable RUN_STARTED line')
  assert.match(source, /RUN_ALREADY_ACTIVE pid=/, 'wrapper must report an active runner session instead of starting a duplicate')
  assert.match(source, /"status": "DRY_RUN"/, 'wrapper must expose a dry-run payload contract')
})

test('WSL detached runner wrapper dry-run reports python entrypoint', () => {
  const result = spawnSync(
    'python3',
    [wslRunnerWrapperPath, '--data-source', 'both', '--dry-run'],
    {
      cwd: path.resolve('..'),
      encoding: 'utf8'
    }
  )

  assert.equal(result.status, 0, result.stderr || result.stdout)
  const payload = JSON.parse(result.stdout)
  assert.equal(payload.status, 'DRY_RUN')
  assert.equal(payload.workingDirectory, path.resolve('..', 'HealthShow'))
  assert.deepEqual(payload.arguments, [
    path.resolve('tests', 'run-full-stack-local.py'),
    '--data-source',
    'both'
  ])
})

test('WSL audit-write wrapper resolves SQL password and runs the existing npm gate', () => {
  const source = fs.readFileSync(auditWriteOncePath, 'utf8')

  assert.match(source, /DB_PASSWORD:/, 'audit-write wrapper must still recover the SQL password from application.yml when env is missing')
  assert.match(source, /npm", "run", "audit:write/, 'audit-write wrapper must reuse the existing npm audit gate')
})

test('WSL health loop runner delegates full profiles to the Python full-stack runner', () => {
  const source = fs.readFileSync(healthLoopRunnerPath, 'utf8')

  assert.match(source, /run-full-stack-local\.py/, 'root health loop runner must target the WSL Python full-stack runner')
  assert.match(source, /profile == "dual-db"/, 'health loop runner must keep a dual-db profile entry')
  assert.match(source, /summary\.json/, 'health loop runner must still write a structured summary payload')
  assert.match(source, /open-issues\.json/, 'health loop runner must still merge open issue tracking')
})

test('WSL watch capture scripts manage backend lifecycle through WSL runners', () => {
  const capture = fs.readFileSync(watchCapturePath, 'utf8')
  const debugCapture = fs.readFileSync(watchCaptureDebugPath, 'utf8')

  assert.match(capture, /health-wsl-backend-run\.sh/, 'capture proxy workflow must start backend through the WSL backend runner')
  assert.match(capture, /health-wsl-stack\.sh/, 'capture proxy workflow must restore the normal backend through the WSL stack script')
  assert.match(debugCapture, /backend_log_to_capture\.py/, 'backend debug capture must still convert backend logs into capture frames')
  assert.match(debugCapture, /health-wsl-stack\.sh/, 'backend debug capture must restore the standard WSL backend service after capture')
})
