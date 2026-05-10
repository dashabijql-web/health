import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';

const authAuditPath = path.resolve('tests', 'e2e', 'auth-session-regression.mjs');
const fullStackRunnerPath = path.resolve('tests', 'run-full-stack-local.ps1');
const openClawRunnerScriptPath = path.resolve('..', 'tools', 'start-health-runner-hidden.ps1');

test('auth audit restarts backend through a no-window detached process', () => {
  const source = fs.readFileSync(authAuditPath, 'utf8');

  assert.match(source, /Win32_ProcessStartup/, 'backend restart must use WMI startup options so Maven is detached from the parent PowerShell pipes');
  assert.match(source, /ShowWindow\s*=\s*0/, 'backend restart must request a hidden WMI process window');
  assert.match(source, /Win32_Process['"]\)\.Create\(\$command,\s*['"]D:\\\\Health\\\\HealthData['"]/, 'backend restart must create Maven from WMI in the HealthData working directory');
  assert.match(source, /set JAVA_HOME=C:\\\\Program Files\\\\Java\\\\jdk-17/, 'backend restart must set JAVA_HOME inside the detached command');
  assert.match(source, /D:\\\\apache-maven-3\.8\.1\\\\bin\\\\mvn\.cmd spring-boot:run/, 'backend restart must call the Maven command script without cmd quote escaping');
  assert.doesNotMatch(source, /['"]\\"D:\\\\apache-maven-3\.8\.1\\\\bin\\\\mvn\.cmd\\"/, 'backend restart command must not include backslash-escaped quotes');
  assert.match(source, /1>>\s*\$\{BACKEND_LOG\}\s*2>>\s*\$\{BACKEND_ERR_LOG\}/, 'backend restart must append Maven output to backend log files');
  assert.match(source, /assertNoVisibleBackendWindows/, 'auth audit must verify no visible backend Java or Maven window remains after restart');
  assert.match(source, /IsWindowVisible/, 'auth audit must use a real Win32 visible-window probe, not just source inspection');
  assert.doesNotMatch(source, /spawn\(\s*['"]cmd\.exe['"]/, 'backend restart must not use Node spawn(cmd.exe), which can leave Maven java.exe visible');
  assert.doesNotMatch(source, /stdio:\s*\[\s*['"]ignore['"]\s*,\s*stdoutFd\s*,\s*stderrFd\s*\]/, 'backend restart must not depend on inherited Node file descriptors');
  assert.doesNotMatch(source, />>\s*["'`]/, 'backend restart must not rely on shell redirection outside the no-window ProcessStartInfo path');
  assert.match(source, /windowsHide:\s*true/, 'backend restart must suppress child process windows');
  assert.match(source, /D:\\\\Health\\\\HealthData\\\\pom\.xml/, 'backend restart must target the HealthData Maven project');
  assert.doesNotMatch(
    source,
    /Start-Process[\s\S]+D:\\\\Health\\\\HealthData\\\\pom\.xml/,
    'backend restart must not use PowerShell Start-Process because execFile can wait on inherited redirected streams'
  );
  assert.match(
    source,
    /try\s*{\s*await ensureBackendReady\(5000\);[\s\S]+catch\s*{\s*await startBackend\(\);/,
    'auth audit must self-start the backend when it is not already listening'
  );
});

test('full stack runner starts backend through a no-window process helper', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /function Start-NoWindowProcess[\s\S]+UseShellExecute\s*=\s*\$false[\s\S]+CreateNoWindow\s*=\s*\$true/,
    'runner must use a no-window process helper for Windows child processes'
  );
  assert.doesNotMatch(
    source,
    /Start-Process\s+-FilePath\s+\$filePath/,
    'runner helper must not use Start-Process for service startup because child consoles can become visible'
  );
  assert.match(
    source,
    /function Start-IfPortClosed[\s\S]+Start-NoWindowProcess/,
    'runner port-gated service startup must route through Start-NoWindowProcess'
  );
  assert.match(
    source,
    /function Stop-VisibleBackendProcesses[\s\S]+Test-ProcessHasVisibleWindow[\s\S]+visible-window-restart/,
    'runner must stop visible HealthData backend windows instead of reusing them just because port 8080 is open'
  );
  assert.match(
    source,
    /if\s*\(Stop-VisibleBackendProcesses\)\s*{[\s\S]+Wait-PortClosed\s+['"]backend-http['"][\s\S]+Start-IfPortClosed\s+['"]backend['"]\s+8080/,
    'runner must perform visible-backend cleanup before deciding whether to start or reuse the backend'
  );
  assert.match(
    source,
    /IsWindowVisible\(IntPtr hWnd\)/,
    'runner must use a real Win32 visible-window probe before stopping a backend process'
  );
  assert.match(
    source,
    /function Start-SimulatorForOld[\s\S]+Start-NoWindowProcess\s+['"]python['"]\s+@\(['"]watch_tcp_simulator_1000\.py['"]\)/,
    'runner simulator startup must also route through Start-NoWindowProcess'
  );
  assert.doesNotMatch(
    source,
    /Start-Process\s+-FilePath\s+['"]python['"][\s\S]+watch_tcp_simulator_1000\.py/,
    'runner must not start the simulator through Start-Process because old-source rounds should not pop a console window'
  );
});

test('OpenClaw runner wrapper uses a stable hidden PowerShell entry point', () => {
  const source = fs.readFileSync(openClawRunnerScriptPath, 'utf8');

  assert.match(
    source,
    /ValidateSet\('old', 'new', 'both'\)/,
    'OpenClaw wrapper must expose the same old/new/both data-source contract as the full stack runner'
  );
  assert.match(
    source,
    /Start-Process[\s\S]+-WindowStyle\s+Hidden[\s\S]+-PassThru/,
    'OpenClaw wrapper must start the runner hidden and return the process id'
  );
  assert.doesNotMatch(
    source,
    /\$_\.Id/,
    'OpenClaw wrapper must not rely on $_.Id because nested PowerShell command text can expand it before execution'
  );
  assert.match(
    source,
    /RUN_STARTED pid=\$\(\$process\.Id\) dataSource=\$DataSource/,
    'OpenClaw wrapper must print a stable RUN_STARTED line for agent monitoring'
  );
  assert.match(
    source,
    /RUN_ALREADY_ACTIVE pid=\$\(\$activeRunner\.ProcessId\) dataSource=\$DataSource/,
    'OpenClaw wrapper must report an existing full-stack runner instead of starting a duplicate'
  );
  assert.ok(
    source.indexOf('if ($DryRun)') >= 0 &&
      source.indexOf('$activeRunner = Get-ActiveRunnerProcess') > source.indexOf('if ($DryRun)'),
    'OpenClaw wrapper must let -DryRun return JSON even when another full-stack runner is active'
  );
});

test('OpenClaw runner wrapper dry-run reports runner arguments without starting a run', () => {
  const result = spawnSync(
    'powershell.exe',
    [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      openClawRunnerScriptPath,
      '-DataSource',
      'both',
      '-DryRun'
    ],
    {
      cwd: path.resolve('..'),
      encoding: 'utf8'
    }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  const payload = JSON.parse(result.stdout);
  assert.equal(payload.status, 'DRY_RUN');
  assert.equal(payload.workingDirectory, 'D:\\Health\\HealthShow');
  assert.deepEqual(payload.arguments, [
    '-NoProfile',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    'D:\\Health\\HealthShow\\tests\\run-full-stack-local.ps1',
    '-DataSource',
    'both'
  ]);
});

test('full stack runner treats one existing simulator process as already running', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /function Start-SimulatorForOld[\s\S]+\$running\s*=\s*@\(\s*Get-SimulatorProcesses\s*\)/,
    'runner must array-wrap simulator process detection before checking Count so a single existing simulator is not missed'
  );
});

test('full stack runner stops simulator before old full pipeline probes', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /function Stop-SimulatorForOldPipeline[\s\S]+old-full-pipeline-exclusive-tcp/,
    'runner must have a dedicated simulator stop before old full TCP pipeline probes'
  );
  assert.match(
    source,
    /Invoke-Step\s+['"]test-perf-old['"][\s\S]+Stop-SimulatorForOldPipeline[\s\S]+Invoke-Step\s+['"]test-full-old['"]/,
    'runner must stop the old-source simulator after perf coverage and before test-full-old runs audit:pipeline'
  );
});

test('full stack runner records data-source database facts and simulator contamination', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /function Get-DataSourceFacts/,
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
    /dataSourceFacts\s*=\s*\$dataSourceFacts/,
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
    /\$hermesArchiveFile\s*=\s*Join-Path\s+\$outDir\s+['"]hermes-archive\.md['"]/,
    'runner must define a stable hermes-archive.md path in each run directory'
  );
  assert.match(
    source,
    /function Write-HermesArchive/,
    'runner must have an explicit Hermes archive writer instead of relying on a manual post-run note'
  );
  assert.match(
    source,
    /hermesArchive\s*=\s*\$hermesArchiveFile/,
    'runner latest/JSON metadata must expose the Hermes archive path'
  );
  assert.match(
    source,
    /hermes_archive:\s*\$hermesArchiveFile/,
    'runner markdown summary must expose the Hermes archive path'
  );
  assert.match(
    source,
    /Write-HermesArchive\s+\$summary\s+\$skippedSteps/,
    'runner must write the Hermes archive from the same summary object used for JSON and Markdown'
  );
});

test('full stack runner logs allowed skipped results distinctly from passed runs', () => {
  const source = fs.readFileSync(fullStackRunnerPath, 'utf8');

  assert.match(
    source,
    /if\s*\(\$skipped\.Count\s+-gt\s+0\)\s*{\s*Log\s+"RESULT SKIPPED \$reason"\s*exit\s+0\s*}/,
    'runner must log allowed skipped runs as RESULT SKIPPED so automation does not misread them as full passes'
  );
});
