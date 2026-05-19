import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'

const supervisorPath = path.resolve('..', 'tools', 'start-openclaw-evolution-supervisor.py')
const stopRequestPath = path.resolve('..', 'tools', 'request-openclaw-supervisor-stop.py')
const supervisorSource = fs.readFileSync(supervisorPath, 'utf8')

test('OpenClaw supervisor supports a stop-after-current request gate', () => {
  assert.match(
    supervisorSource,
    /DEFAULT_STOP_FILE = .*openclaw-night-supervisor\.stop/,
    'supervisor must expose a stable stop-after-current file path parameter'
  )
  assert.match(
    supervisorSource,
    /def test_stop_after_current_requested/,
    'supervisor must centralize stop-after-current checks'
  )
  assert.match(
    supervisorSource,
    /STOP_AFTER_CURRENT_REQUESTED/,
    'supervisor must log when an operator requests a graceful stop'
  )
  assert.match(
    supervisorSource.replace(/\s+/g, ' '),
    /wait_full_stack_runner_idle\(args\.runner_timeout_minutes\)[\s\S]+if test_stop_after_current_requested\(stop_file\)/,
    'supervisor must check the stop gate immediately after the current runner goes idle'
  )
  assert.match(
    supervisorSource,
    /ROUND_STOP_AFTER_CURRENT/,
    'supervisor must stop before starting another OpenClaw agent when the gate is set'
  )
})

test('OpenClaw supervisor prompts for structured process retrospectives', () => {
  assert.match(
    supervisorSource,
    /process_retrospective/,
    'prompt must require each accepted round to document process lessons and guard changes'
  )
  assert.match(
    supervisorSource,
    /ROUND_RESULT_JSON/,
    'prompt must require a machine-readable round result block in addition to the DONE marker'
  )
  assert.match(
    supervisorSource,
    /flow_guard_added/,
    'round result JSON must distinguish project changes from flow guard improvements'
  )
  assert.match(
    supervisorSource,
    /def valid_round_result_json/,
    'supervisor must validate the structured round result contract before accepting a round'
  )
})

test('OpenClaw supervisor final report records explicit stop reason', () => {
  assert.match(
    supervisorSource,
    /stop_reason =/,
    'supervisor must track stop reason in state instead of hard-coding stopAt reached'
  )
  assert.match(
    supervisorSource,
    /stop_reason: \{stop_reason\}/,
    'final report must write the actual stop reason'
  )
})

test('OpenClaw graceful stop request script writes the shared stop file', () => {
  assert.ok(fs.existsSync(stopRequestPath), 'operator stop request script must exist')
  const source = fs.readFileSync(stopRequestPath, 'utf8')
  assert.match(source, /openclaw-night-supervisor\.stop/, 'stop script must target the supervisor stop file')
  assert.match(source, /STOP_AFTER_CURRENT_REQUESTED/, 'stop script must write an auditable request marker')

  const result = spawnSync(
    'python3',
    [
      stopRequestPath,
      '--dry-run'
    ],
    {
      cwd: path.resolve('..'),
      encoding: 'utf8'
    }
  )
  assert.equal(result.status, 0, result.stderr || result.stdout)
  const payload = JSON.parse(result.stdout)
  assert.equal(payload.status, 'DRY_RUN')
  assert.match(payload.stopFile, /openclaw-night-supervisor\.stop$/)
})
