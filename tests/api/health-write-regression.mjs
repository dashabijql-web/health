import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);

const LOGIN_CREDENTIALS = { username: 'admin', password: 'admin123' };
const RETENTION = Number.parseInt(process.env.API_WRITE_ARTIFACT_RETENTION || '10', 10);
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const ARTIFACT_DIR = path.resolve(process.cwd(), 'tests', 'api', 'artifacts', RUN_ID);
const REPORT_JSON = path.join(ARTIFACT_DIR, 'summary.json');
const REPORT_MD = path.join(ARTIFACT_DIR, 'summary.md');

const SQL_SERVER = process.env.SQL_SERVER || 'localhost,58135';
const SQL_USER = process.env.SQL_USER || 'sa';
const SQL_PASSWORD = process.env.SQL_PASSWORD || '123abcd.';
const SQL_DB = process.env.SQL_DB || 'health';
const SQLCMD_BIN = process.env.SQLCMD_BIN || 'sqlcmd';
const CURRENT_MONTH = new Date().toISOString().slice(0, 7).replace('-', '');

const TARGETS = [
  { label: 'vite-127', origin: 'http://127.0.0.1:9528', apiPrefix: '/dev-api' },
  { label: 'vite-localhost', origin: 'http://localhost:9528', apiPrefix: '/dev-api' },
  { label: 'backend-127', origin: 'http://127.0.0.1:8080', apiPrefix: '/health' },
  { label: 'backend-localhost', origin: 'http://localhost:8080', apiPrefix: '/health' }
];

const summary = {
  runId: RUN_ID,
  startedAt: new Date().toISOString(),
  target: null,
  checks: [],
  warnings: []
};

await fs.mkdir(ARTIFACT_DIR, { recursive: true });
await pruneArtifacts(path.dirname(ARTIFACT_DIR), RETENTION);

function truncate(value, max = 260) {
  if (!value) return '';
  return value.length > max ? `${value.slice(0, max - 3)}...` : value;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function pruneArtifacts(rootDir, keep = 10) {
  if (!Number.isFinite(keep) || keep <= 0) return;
  const entries = await fs.readdir(rootDir, { withFileTypes: true }).catch(() => []);
  const dirs = entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name).sort();
  const obsolete = dirs.slice(0, Math.max(0, dirs.length - keep));
  await Promise.allSettled(
    obsolete.map((dirName) => fs.rm(path.join(rootDir, dirName), { recursive: true, force: true }))
  );
}

async function tryLogin(target) {
  const response = await fetch(`${target.origin}${target.apiPrefix}/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(LOGIN_CREDENTIALS)
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const payload = await response.json();
  assert(payload?.code === 200, payload?.message || 'login rejected');
  assert(payload?.data?.token, 'login response missing token');

  return { target, token: payload.data.token };
}

async function resolveSession() {
  const errors = [];
  for (const target of TARGETS) {
    try {
      return await tryLogin(target);
    } catch (error) {
      errors.push(`${target.label}: ${truncate(String(error))}`);
    }
  }
  throw new Error(`unable to login to any target: ${errors.join(' | ')}`);
}

function buildHeaders(session, extraHeaders = {}) {
  return {
    accept: 'application/json',
    satoken: session.token,
    cookie: `User-Token=${session.token}; satoken=${session.token}`,
    ...extraHeaders
  };
}

async function requestJson(session, method, routePath, options = {}) {
  const url = new URL(`${session.target.origin}${session.target.apiPrefix}${routePath}`);
  const { query, data, headers } = options;

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const response = await fetch(url, {
    method,
    headers: buildHeaders(session, data ? { 'content-type': 'application/json', ...headers } : headers),
    body: data ? JSON.stringify(data) : undefined
  });

  const text = await response.text();
  let payload = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch {
    payload = null;
  }

  return { method, url: url.toString(), status: response.status, payload, raw: text };
}

function assertResultOk(result) {
  assert(result.status >= 200 && result.status < 300, `HTTP ${result.status}`);
  assert(result.payload && typeof result.payload === 'object', 'response body is not JSON object');
  assert(result.payload.code === 200, result.payload.message || `unexpected code ${result.payload.code}`);
}

async function runCheck(name, fn) {
  const startedAt = Date.now();
  const record = {
    name,
    status: 'passed',
    durationMs: 0,
    note: ''
  };

  try {
    const note = await fn();
    if (note) record.note = note;
  } catch (error) {
    record.status = 'failed';
    record.note = truncate(String(error), 320);
  }

  record.durationMs = Date.now() - startedAt;
  summary.checks.push(record);
  return record;
}

async function sqlJson(query) {
  const batch = `SET NOCOUNT ON; ${query}`;
  const { stdout } = await execFileAsync(
    SQLCMD_BIN,
    ['-S', SQL_SERVER, '-U', SQL_USER, '-P', SQL_PASSWORD, '-d', SQL_DB, '-W', '-h-1', '-Q', batch],
    { maxBuffer: 10 * 1024 * 1024 }
  );

  const text = stdout.trim();
  if (!text) return null;
  const starts = [text.indexOf('['), text.indexOf('{')].filter((idx) => idx >= 0);
  const start = starts.length ? Math.min(...starts) : -1;
  if (start < 0) return null;
  return JSON.parse(text.slice(start));
}

async function sqlExecRows(query) {
  const result = await sqlJson(`${query}; SELECT @@ROWCOUNT AS rows FOR JSON PATH, WITHOUT_ARRAY_WRAPPER`);
  return result?.rows ?? 0;
}

async function restoreWarningRow(id, createTime) {
  const monthTable = `warning_record_${tableSuffixFromTime(createTime)}`;
  const monthRows = await sqlExecRows(`
    UPDATE ${monthTable}
    SET is_handled = 0,
        handle_time = NULL,
        handle_by = NULL,
        remark = NULL
    WHERE id = ${id}
  `).catch(() => 0);

  if (monthRows > 0) return monthRows;

  return sqlExecRows(`
    UPDATE warning_record
    SET is_handled = 0,
        handle_time = NULL,
        handle_by = NULL,
        remark = NULL
    WHERE id = ${id}
  `);
}

function tableSuffixFromTime(timeValue) {
  const month = String(timeValue).slice(0, 7).replace('-', '');
  return month;
}

async function getLatestHealthRecordPage(session, empCode) {
  const result = await requestJson(session, 'GET', '/api/health/record/page', {
    query: { current: 1, size: 1, userCode: empCode }
  });
  assertResultOk(result);
  const records = result.payload?.data?.records || [];
  return records[0] || null;
}

async function getLatestGlobalHealthRecord() {
  return sqlJson(`
    SELECT TOP 1
      id,
      user_code AS userCode,
      record_time AS recordTime,
      heart_rate AS heartRate,
      pressure
    FROM health_record_${CURRENT_MONTH}
    ORDER BY id DESC
    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
  `);
}

async function waitForNewGlobalRecord(baselineId, timeoutMs = 15000) {
  const startedAt = Date.now();
  let last = baselineId;

  while (Date.now() - startedAt < timeoutMs) {
    const current = await getLatestGlobalHealthRecord();
    if (current && current.id && current.id !== baselineId) {
      return current;
    }
    last = current?.id ?? last;
    await sleep(1000);
  }

  throw new Error(`no new record observed for ${empCode} (baseline=${baselineId}, last=${last ?? '-'})`);
}

async function findWritableEmployeeCandidate() {
  return sqlJson(`
    SELECT TOP 1
      e.emp_code AS empCode,
      e.emp_name AS empName,
      COUNT(hr.id) AS recordCount
    FROM employee e
    INNER JOIN v_health_record hr
      ON hr.user_code = e.emp_code
     AND hr.record_time >= DATEADD(DAY, -30, GETDATE())
    WHERE NOT EXISTS (
      SELECT 1
      FROM ai_health_report r
      WHERE r.emp_code = e.emp_code
    )
    GROUP BY e.emp_code, e.emp_name
    HAVING COUNT(hr.id) > 0
    ORDER BY COUNT(hr.id) DESC, e.emp_code ASC
    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
  `);
}

async function main() {
  const session = await resolveSession();
  summary.target = session.target;

  await runCheck('auth.info', async () => {
    const result = await requestJson(session, 'GET', '/auth/info');
    assertResultOk(result);
    const data = result.payload.data;
    assert(data && typeof data === 'object', 'auth info data is not object');
    assert(typeof data.name === 'string' && data.name.length > 0, 'auth info missing name');
    return data.name;
  });

  await runCheck('realtime.chain', async () => {
    const baseline = await getLatestGlobalHealthRecord();
    assert(baseline?.id && baseline?.userCode, 'no live health record baseline found');

    const observed = await waitForNewGlobalRecord(baseline.id, 20000);
    assert(observed?.id && observed?.userCode, 'no new global health record observed');

    const pageAfter = await requestJson(session, 'GET', '/api/health/record/page', {
      query: { current: 1, size: 1, userCode: observed.userCode }
    });
    assertResultOk(pageAfter);
    const latestPageRow = pageAfter.payload?.data?.records?.[0];
    assert(latestPageRow?.id === observed.id, 'API did not expose the observed live record');

    const realtimeResult = await requestJson(session, 'GET', `/realtime/user/${observed.userCode}`);
    assertResultOk(realtimeResult);
    assert(realtimeResult.payload?.data && typeof realtimeResult.payload.data === 'object', 'realtime user data is not object');

    return `${observed.userCode} @ ${observed.recordTime || observed.time}`;
  });

  await runCheck('risk-warning.handle', async () => {
    const listResult = await requestJson(session, 'GET', '/risk-warning/list', {
      query: { page: 1, size: 20, handled: false }
    });
    assertResultOk(listResult);
    const row = (listResult.payload?.data?.list || []).find((item) => item?.id && !item?.handled);
    assert(row?.id && row?.createTime, 'no unhandled warning found');

    const createTime = String(row.createTime);
    const tableName = `warning_record_${tableSuffixFromTime(createTime)}`;
    const handleBody = {
      handleBy: 'system',
      handleRemark: 'health write regression',
      createTime
    };

    try {
      const handleResult = await requestJson(session, 'POST', `/risk-warning/handle/${row.id}`, {
        data: handleBody
      });
      assertResultOk(handleResult);

      const handledRow = await sqlJson(`
        SELECT TOP 1 id, is_handled AS handled, handle_by AS handleBy, remark AS handleRemark
        FROM ${tableName}
        WHERE id = ${row.id}
        FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
      `) || await sqlJson(`
        SELECT TOP 1 id, is_handled AS handled, handle_by AS handleBy, remark AS handleRemark
        FROM warning_record
        WHERE id = ${row.id}
        FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
      `);

      assert(handledRow?.handled === true || handledRow?.handled === 1, 'warning was not marked handled');
      return `warning ${row.id} handled`;
    } finally {
      await restoreWarningRow(row.id, createTime);
    }
  });

  await runCheck('alert-config.update', async () => {
    const listResult = await requestJson(session, 'GET', '/alert-config/list');
    assertResultOk(listResult);
    const row = (listResult.payload?.data || []).find((item) => item?.id);
    assert(row?.id, 'no alert config found');

    const original = { ...row };
    const mutated = { ...row, enabled: row.enabled ? 0 : 1 };

    try {
      let updateResult = await requestJson(session, 'PUT', '/alert-config/update', { data: mutated });
      assertResultOk(updateResult);

      let verifyResult = await requestJson(session, 'GET', '/alert-config/list');
      assertResultOk(verifyResult);
      const changed = (verifyResult.payload?.data || []).find((item) => item?.id === row.id);
      assert(changed && changed.enabled === mutated.enabled, 'alert config update did not persist');

      updateResult = await requestJson(session, 'PUT', '/alert-config/update', { data: original });
      assertResultOk(updateResult);

      verifyResult = await requestJson(session, 'GET', '/alert-config/list');
      assertResultOk(verifyResult);
      const restored = (verifyResult.payload?.data || []).find((item) => item?.id === row.id);
      assert(restored && restored.enabled === original.enabled, 'alert config rollback failed');

      return `config ${row.id}`;
    } catch (error) {
      await requestJson(session, 'PUT', '/alert-config/update', { data: original }).catch(() => {});
      throw error;
    }
  });

  await runCheck('ai.report.employee', async () => {
    const candidate = await findWritableEmployeeCandidate();
    assert(candidate?.empCode, 'no writable AI report candidate found');

    try {
      const result = await requestJson(session, 'POST', '/ai/report/employee', {
        data: { empCode: candidate.empCode },
        headers: { 'content-type': 'application/json' }
      });
      assertResultOk(result);
      const report = result.payload?.data?.report || result.payload?.data?.reportContent || '';
      assert(typeof report === 'string' && report.length > 50, 'AI report content is empty');
      return `${candidate.empCode} ${report.length} chars`;
    } finally {
      await sqlExecRows(`DELETE FROM ai_health_report WHERE emp_code = '${String(candidate.empCode).replace(/'/g, "''")}'`)
        .catch(() => {});
    }
  });
}

function buildMarkdownReport() {
  const totals = {
    passed: summary.checks.filter((item) => item.status === 'passed').length,
    failed: summary.checks.filter((item) => item.status === 'failed').length
  };

  return [
    '# Health write regression report',
    '',
    `- run_id: ${summary.runId}`,
    `- target: ${summary.target?.label || '-'} (${summary.target?.origin || '-'})`,
    `- started_at: ${summary.startedAt}`,
    `- finished_at: ${summary.finishedAt}`,
    '',
    '## Totals',
    '',
    `- passed: ${totals.passed}`,
    `- failed: ${totals.failed}`,
    `- warnings: ${summary.warnings.length}`,
    '',
    '## Checks',
    '',
    '| check | status | ms | note |',
    '| --- | --- | ---: | --- |',
    ...summary.checks.map((item) => `| ${item.name} | ${item.status} | ${item.durationMs} | ${item.note || '-'} |`),
    '',
    '## Warnings',
    '',
    ...(summary.warnings.length ? summary.warnings.map((item) => `- ${item}`) : ['- none'])
  ].join('\n');
}

try {
  await main();
} finally {
  summary.finishedAt = new Date().toISOString();
  await fs.writeFile(REPORT_JSON, JSON.stringify(summary, null, 2), 'utf8');
  await fs.writeFile(REPORT_MD, buildMarkdownReport(), 'utf8');
}

const failedCount = summary.checks.filter((item) => item.status === 'failed').length;

console.log(JSON.stringify({
  artifactDir: ARTIFACT_DIR,
  target: summary.target,
  reportFile: REPORT_MD,
  checkCount: summary.checks.length,
  failedCount,
  warningCount: summary.warnings.length
}, null, 2));

if (failedCount > 0) {
  process.exitCode = 1;
}
