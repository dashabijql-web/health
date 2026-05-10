import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { execFile as execFileCallback } from 'node:child_process';
import { promisify } from 'node:util';
import { chromium } from 'playwright';
import { assert, LOGIN_CREDENTIALS, truncate } from '../shared/health-test-utils.mjs';

const execFile = promisify(execFileCallback);

const BASE_URL = process.env.AUTH_AUDIT_BASE_URL || 'http://127.0.0.1:9528';
const API_BASE = `${BASE_URL}/dev-api`;
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const ARTIFACT_DIR = path.resolve(process.cwd(), 'tests', 'e2e', 'artifacts', RUN_ID);
const REPORT_JSON = path.join(ARTIFACT_DIR, 'auth-summary.json');
const REPORT_MD = path.join(ARTIFACT_DIR, 'auth-summary.md');
const BACKEND_LOG = 'D:\\Health\\HealthData\\backend-start.log';
const BACKEND_ERR_LOG = 'D:\\Health\\HealthData\\backend-start.err.log';

const summary = {
  runId: RUN_ID,
  startedAt: new Date().toISOString(),
  baseUrl: BASE_URL,
  checks: []
};

await fs.mkdir(ARTIFACT_DIR, { recursive: true });

function timeoutAfter(ms, message) {
  return new Promise((_, reject) => {
    const timer = setTimeout(() => reject(new Error(message)), ms);
    timer.unref?.();
  });
}

async function cleanupWithTimeout(label, cleanup, timeoutMs = 10000) {
  try {
    await Promise.race([
      cleanup(),
      timeoutAfter(timeoutMs, `${label} cleanup timed out after ${timeoutMs}ms`)
    ]);
  } catch (error) {
    console.warn(`[auth-audit] cleanup warning ${label}: ${truncate(error?.message || String(error))}`);
  }
}

async function runCheck(name, fn) {
  console.log(`[auth-audit] start ${name}`);
  const startedAt = Date.now();
  const record = { name, status: 'passed', durationMs: 0, note: '' };
  try {
    const note = await fn();
    if (note) record.note = note;
  } catch (error) {
    record.status = 'failed';
    record.note = truncate(error?.stack || String(error), 500);
  }
  record.durationMs = Date.now() - startedAt;
  summary.checks.push(record);
  if (record.status !== 'passed') {
    console.log(`[auth-audit] fail ${name}: ${record.note}`);
    throw new Error(`${name} failed: ${record.note}`);
  }
  console.log(`[auth-audit] pass ${name}${record.note ? ` :: ${record.note}` : ''}`);
  return record;
}

async function waitForAppReady(page) {
  await page.waitForLoadState('domcontentloaded');
  await page.locator('#app').first().waitFor({ state: 'visible', timeout: 15000 });
}

async function waitForNonLoginHash(page) {
  await page.waitForFunction(() => !window.location.hash.includes('/login'), null, { timeout: 15000 });
}

async function waitForLoginHash(page) {
  await page.waitForFunction(() => window.location.hash.includes('/login'), null, { timeout: 15000 });
}

async function ensureBackendReady(timeoutMs = 90000) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    try {
      const response = await fetch('http://127.0.0.1:8080/health');
      if (response.ok) {
        return;
      }
    } catch {
      // keep polling
    }
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  throw new Error('backend did not become ready within timeout');
}

async function ensureBackendStopped(timeoutMs = 30000) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    try {
      await fetch('http://127.0.0.1:8080/health');
    } catch {
      return;
    }
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  throw new Error('backend did not stop within timeout');
}

async function execPowerShell(command) {
  const { stdout, stderr } = await execFile('powershell', ['-NoProfile', '-Command', command], {
    windowsHide: true,
    maxBuffer: 1024 * 1024
  });
  return { stdout, stderr };
}

async function assertNoVisibleBackendWindows() {
  const command = [
    "Add-Type @'\nusing System;\nusing System.Runtime.InteropServices;\npublic static class Win32WindowProbe {\n  [DllImport(\"user32.dll\")] public static extern bool IsWindowVisible(IntPtr hWnd);\n}\n'@",
    "$backend = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and ($_.CommandLine.Contains('D:\\Health\\HealthData\\pom.xml') -or $_.CommandLine.Contains('com.xzkj.health.HealthApplication')) }",
    "$visible = foreach ($item in $backend) { $p = Get-Process -Id $item.ProcessId -ErrorAction SilentlyContinue; if ($p -and $p.MainWindowHandle -ne 0 -and [Win32WindowProbe]::IsWindowVisible($p.MainWindowHandle)) { [pscustomobject]@{ processId = $item.ProcessId; name = $item.Name; title = $p.MainWindowTitle } } }",
    "if ($visible) { $visible | ConvertTo-Json -Compress; exit 1 }"
  ].join('; ');
  await execPowerShell(command);
}

async function startBackend() {
  console.log('[auth-audit] starting backend process');
  const startCommand = [
    "$startup = ([wmiclass]'Win32_ProcessStartup').CreateInstance()",
    "$startup.ShowWindow = 0",
    `$command = 'cmd.exe /d /s /c "set JAVA_HOME=C:\\Program Files\\Java\\jdk-17&& set Path=C:\\Program Files\\Java\\jdk-17\\bin;D:\\apache-maven-3.8.1\\bin;%Path%&& D:\\apache-maven-3.8.1\\bin\\mvn.cmd spring-boot:run -f D:\\Health\\HealthData\\pom.xml 1>> ${BACKEND_LOG} 2>> ${BACKEND_ERR_LOG}"'`,
    "$result = ([wmiclass]'Win32_Process').Create($command, 'D:\\Health\\HealthData', $startup)",
    "if ($result.ReturnValue -ne 0) { throw \"Win32_Process.Create failed: $($result.ReturnValue)\" }",
    "Write-Output $result.ProcessId"
  ].join('; ');
  await execPowerShell(startCommand);
  await ensureBackendReady();
  await assertNoVisibleBackendWindows();
  console.log('[auth-audit] backend is ready');
}

async function restartBackend() {
  console.log('[auth-audit] stopping backend process');
  const stopCommand = [
    "$pids = foreach ($port in 8080,9000) { Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess }",
    "if ($pids) { foreach ($pidValue in $pids) { Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue } }"
  ].join('; ');
  await execPowerShell(stopCommand);
  await ensureBackendStopped();
  await startBackend();
}

async function loginViaApi(requestContext) {
  const response = await requestContext.post(`${API_BASE}/auth/login`, {
    data: LOGIN_CREDENTIALS
  });
  assert(response.ok(), `login HTTP ${response.status()}`);
  const payload = await response.json();
  assert(payload?.code === 200, payload?.message || 'login rejected');
  assert(payload?.data?.token, 'login response missing token');
  return payload.data.token;
}

async function bootstrapSession(page, token) {
  await page.context().addCookies([
    {
      name: 'User-Token',
      value: token,
      url: BASE_URL
    }
  ]);
  await page.goto(`${BASE_URL}/#/health-monitor/dashboard`, { waitUntil: 'domcontentloaded' });
  await waitForAppReady(page);
  await waitForNonLoginHash(page);
}

async function invalidateServerSideToken(requestContext, token) {
  const response = await requestContext.post(`${API_BASE}/auth/logout`, {
    headers: {
      satoken: token,
      cookie: `User-Token=${token}; satoken=${token}`
    }
  });
  assert(response.ok(), `logout HTTP ${response.status()}`);
}

function buildMarkdownReport() {
  return [
    '# Auth Session Regression',
    '',
    `- run_id: ${summary.runId}`,
    `- base_url: ${summary.baseUrl}`,
    `- started_at: ${summary.startedAt}`,
    `- finished_at: ${summary.finishedAt}`,
    '',
    '| check | status | ms | note |',
    '| --- | --- | ---: | --- |',
    ...summary.checks.map(item => `| ${item.name} | ${item.status} | ${item.durationMs} | ${item.note || '-'} |`)
  ].join('\n');
}

try {
  await ensureBackendReady(5000);
} catch {
  await startBackend();
}

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({ baseURL: BASE_URL });
const page = await context.newPage();

try {
  await runCheck('login.ui.prefilled-admin', async () => {
    await page.goto(`${BASE_URL}/#/login`, { waitUntil: 'domcontentloaded' });
    await waitForAppReady(page);
    const username = await page.locator('input[name="username"]').inputValue();
    const password = await page.locator('input[name="password"]').inputValue();
    assert(username === LOGIN_CREDENTIALS.username, `username input should default to ${LOGIN_CREDENTIALS.username}`);
    assert(password === LOGIN_CREDENTIALS.password, 'password input should match configured test password');
    return `login form is prefilled with ${LOGIN_CREDENTIALS.username}/<configured-password>`;
  });

  await runCheck('login.ui.submit', async () => {
    await page.locator('input[name="username"]').fill(LOGIN_CREDENTIALS.username);
    await page.locator('input[name="password"]').fill(LOGIN_CREDENTIALS.password);
    await page.locator('.login-form button').click();
    await waitForNonLoginHash(page);
    await page.locator('.navbar').waitFor({ state: 'visible', timeout: 15000 });
    return await page.evaluate(() => window.location.hash);
  });

  await runCheck('auth.refresh.restore', async () => {
    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForAppReady(page);
    await waitForNonLoginHash(page);
    await page.locator('.navbar').waitFor({ state: 'visible', timeout: 15000 });
    return await page.evaluate(() => window.location.hash);
  });

  await runCheck('auth.logout.ui', async () => {
    await page.locator('.avatar-wrapper').click();
    await page.locator('.user-dropdown').getByText('退出').click();
    await waitForLoginHash(page);
    const cookies = await context.cookies(BASE_URL);
    const userToken = cookies.find(cookie => cookie.name === 'User-Token');
    assert(!userToken, 'User-Token cookie should be removed after logout');
    return 'logout cleared cookie and returned to login';
  });

  let token = await loginViaApi(context.request);
  await runCheck('auth.bootstrap.after-logout', async () => {
    await bootstrapSession(page, token);
    return await page.evaluate(() => window.location.hash);
  });

  await runCheck('auth.backend-restart.redirects-stale-token', async () => {
    await restartBackend();
    const response = await context.request.get(`${API_BASE}/auth/info`, {
      headers: {
        satoken: token,
        cookie: `User-Token=${token}; satoken=${token}`
      }
    });
    const payload = await response.json();
    assert(response.ok(), `auth info HTTP ${response.status()}`);
    assert(payload?.code === 401, `expected stale token to become 401 after restart, got ${JSON.stringify(payload)}`);
    console.log('[auth-audit] stale token confirmed as 401 after backend restart');

    await page.goto(`${BASE_URL}/?authAudit=${Date.now()}#/health-monitor/dashboard`, { waitUntil: 'domcontentloaded' });
    await waitForAppReady(page);
    await waitForLoginHash(page);
    return 'stale cookie redirected to login after backend restart';
  });

  token = await loginViaApi(context.request);
  await bootstrapSession(page, token);
  await new Promise(resolve => setTimeout(resolve, 1500));

  await runCheck('auth.concurrent-401-redirects-to-login', async () => {
    await invalidateServerSideToken(context.request, token);
    const result = await page.evaluate(async () => {
      const [{ default: request }] = await Promise.all([
        import('/src/utils/request.js')
      ]);

      await Promise.allSettled(
        Array.from({ length: 4 }, () => request({ url: '/dashboard/overview', method: 'get' }))
      );
      await new Promise(resolve => setTimeout(resolve, 1500));

      return {
        hash: window.location.hash
      };
    });

    assert(result.hash.includes('/login'), `expected login redirect, got ${result.hash}`);
    const cookies = await context.cookies(BASE_URL);
    const userToken = cookies.find(cookie => cookie.name === 'User-Token');
    assert(!userToken, 'User-Token cookie should be removed after concurrent 401 requests');
    return `hash=${result.hash}, cookieCleared=true`;
  });
} finally {
  summary.finishedAt = new Date().toISOString();
  await fs.writeFile(REPORT_JSON, JSON.stringify(summary, null, 2), 'utf8');
  await fs.writeFile(REPORT_MD, buildMarkdownReport(), 'utf8');
  await cleanupWithTimeout('playwright-context', () => context.close());
  await cleanupWithTimeout('playwright-browser', () => browser.close());
}

console.log(JSON.stringify({
  artifactDir: ARTIFACT_DIR,
  reportFile: REPORT_MD,
  checkCount: summary.checks.length,
  failedCount: summary.checks.filter(item => item.status !== 'passed').length
}, null, 2));

process.exit(0);
