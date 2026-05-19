import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';

const authAuditPath = path.resolve('tests', 'e2e', 'auth-session-regression.mjs');
const fullStackRunnerPath = path.resolve('tests', 'run-full-stack-local.py');
const openClawRunnerScriptPath = path.resolve('..', 'tools', 'start-health-runner.py');

test('auth audit restarts backend through the WSL stack manager', () => {
  const source = fs.readFileSync(authAuditPath, 'utf8');

  assert.match(source, /health-wsl-stack\.sh/, 'auth audit must control backend lifecycle through the WSL stack script');
  assert.match(source, /async function execBash/, 'auth audit must shell out through bash instead of PowerShell');
  assert.match(source, /stackAction\('backend', 'stop'\)/, 'auth audit must stop backend through the stack script before stale-token restart');
  assert.match(source, /stackAction\('backend', 'start'\)/, 'auth audit must start backend through the stack script');
  assert.doesNotMatch(source, /powershell/i, 'auth audit backend restart must no longer depend on PowerShell');
  assert.doesNotMatch(source, /Win32_Process/, 'auth audit backend restart must no longer depend on Windows WMI process creation');
  assert.match(
    source,
    /try\s*{\s*await ensureBackendReady\(5000\);[\s\S]+catch\s*{\s*await startBackend\(\);/,
    'auth audit must self-start the backend when it is not already listening'
  );
});

test('full stack runner manages backend and simulator through the WSL stack', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /health-wsl-stack\.sh/,
    'runner must use the WSL stack script for long-lived services'
  );
  assert.match(
    source,
    /self\.stack_action\("backend", "start"\)/,
    'runner must start backend through the stack script'
  );
  assert.match(
    source,
    /self\.stack_action\("simulator", "start"\)/,
    'runner must start the simulator through the stack script'
  );
});

test('detached runner wrapper uses the WSL tmux entry point', () => {
  const source = fs.readFileSync(openClawRunnerScriptPath, 'utf8');

  assert.match(
    source,
    /choices=\["old", "new", "both"\]/,
    'detached runner wrapper must expose the same old/new/both data-source contract as the full stack runner'
  );
  assert.match(
    source,
    /tmux/,
    'detached runner wrapper must use tmux to keep the runner alive in WSL'
  );
  assert.doesNotMatch(
    source,
    /powershell/i,
    'detached runner wrapper must not depend on PowerShell once the project is WSL-only'
  );
  assert.match(
    source,
    /RUN_STARTED pid=/,
    'OpenClaw wrapper must print a stable RUN_STARTED line for agent monitoring'
  );
  assert.match(
    source,
    /RUN_ALREADY_ACTIVE pid=/,
    'OpenClaw wrapper must report an existing full-stack runner instead of starting a duplicate'
  );
  assert.match(source, /"status": "DRY_RUN"/, 'detached runner wrapper must expose a DRY_RUN payload');
});

test('detached runner wrapper dry-run reports runner arguments without starting a run', () => {
  const result = spawnSync(
    'python3',
    [
      openClawRunnerScriptPath,
      '--data-source',
      'both',
      '--dry-run'
    ],
    {
      cwd: path.resolve('..'),
      encoding: 'utf8'
    }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  const payload = JSON.parse(result.stdout);
  assert.equal(payload.status, 'DRY_RUN');
  assert.match(
    payload.workingDirectory,
    /(HealthShow)$/i,
    'dry-run workingDirectory must point at the HealthShow workspace in either Windows or WSL path form'
  );
  assert.deepEqual(payload.arguments.slice(0, 2), [payload.arguments[0], '--data-source']);
  assert.match(
    payload.arguments[0],
    /run-full-stack-local\.py$/i,
    'dry-run runner path must target the Python full-stack runner'
  );
  assert.equal(payload.arguments[2], 'both');
});

test('full stack runner treats one existing simulator process as already running', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /before\.get\("state"\) != "running" and after\.get\("state"\) == "running"/,
    'runner must only record a simulator start when the WSL stack transitions from stopped to running'
  );
});

test('full stack runner stops simulator before old full pipeline probes', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /old-full-pipeline-exclusive-tcp/,
    'runner must have a dedicated simulator stop before old full TCP pipeline probes'
  );
  assert.match(
    source.replace(/\s+/g, ' '),
    /test-perf-old[\s\S]+stack_action\("simulator", "stop", stop_reason="old-full-pipeline-exclusive-tcp"\)[\s\S]+test-full-old/,
    'runner must stop the old-source simulator after perf coverage and before test-full-old runs audit:pipeline'
  );
});

test('full stack runner records data-source database facts and simulator contamination', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /def get_data_source_facts/,
    'runner must collect source-specific database facts for each full-stack run'
  );
  assert.match(
    source,
    /simulatorDeviceRows/,
    'runner summary must expose simulator IMEI rows so new-source contamination is visible'
  );
  assert.match(
    source,
    /health_new/,
    'runner source facts must explicitly know the health_new database mapping'
  );
  assert.match(
    source,
    /"dataSourceFacts": self\.data_source_facts/,
    'runner JSON summary must include the collected data-source facts'
  );
  assert.match(
    source,
    /## Data Source Facts/,
    'runner markdown summary must include a data-source facts section'
  );
});

test('full stack runner writes a Hermes archive for automation triage', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /hermes-archive\.md/,
    'runner must define a stable hermes-archive.md path in each run directory'
  );
  assert.match(
    source,
    /def write_hermes_archive/,
    'runner must have an explicit Hermes archive writer instead of relying on a manual post-run note'
  );
  assert.match(
    source,
    /"hermesArchive": str\(self\.hermes_archive_file\)/,
    'runner latest/JSON metadata must expose the Hermes archive path'
  );
  assert.match(
    source,
    /- hermes_archive: \{self\.hermes_archive_file\}/,
    'runner markdown summary must expose the Hermes archive path'
  );
  assert.match(
    source,
    /self\.write_hermes_archive\(summary, skipped_steps\)/,
    'runner must write the Hermes archive from the same summary object used for JSON and Markdown'
  );
});

test('full stack runner logs allowed skipped results distinctly from passed runs', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /if skipped:[\s\S]+self\.log\(f"RESULT SKIPPED \{reason\}"\)[\s\S]+return 0/,
    'runner must log allowed skipped runs as RESULT SKIPPED so automation does not misread them as full passes'
  );
});
