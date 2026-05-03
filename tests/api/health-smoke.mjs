import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const LOGIN_CREDENTIALS = { username: 'admin', password: 'admin123' };
const RETENTION = Number.parseInt(process.env.API_ARTIFACT_RETENTION || '10', 10);
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const ARTIFACT_DIR = path.resolve(process.cwd(), 'tests', 'api', 'artifacts', RUN_ID);
const REPORT_JSON = path.join(ARTIFACT_DIR, 'summary.json');
const REPORT_MD = path.join(ARTIFACT_DIR, 'summary.md');
const MONTH_KEY = new Date().toISOString().slice(0, 7);
const TODAY_KEY = new Date().toISOString().slice(0, 10);
const [CURRENT_YEAR, CURRENT_MONTH] = MONTH_KEY.split('-').map(Number);

const TARGETS = [
  { label: 'vite-127', origin: 'http://127.0.0.1:9528', apiPrefix: '/dev-api' },
  { label: 'vite-localhost', origin: 'http://localhost:9528', apiPrefix: '/dev-api' },
  { label: 'backend-127', origin: 'http://127.0.0.1:8080', apiPrefix: '/health' },
  { label: 'backend-localhost', origin: 'http://localhost:8080', apiPrefix: '/health' }
];

const summary = {
  runId: RUN_ID,
  month: MONTH_KEY,
  startedAt: new Date().toISOString(),
  target: null,
  checks: [],
  warnings: []
};

await fs.mkdir(ARTIFACT_DIR, { recursive: true });
await pruneArtifacts(path.dirname(ARTIFACT_DIR), RETENTION);

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function truncate(value, max = 220) {
  if (!value) return '';
  return value.length > max ? `${value.slice(0, max - 3)}...` : value;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
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
  const loginUrl = `${target.origin}${target.apiPrefix}/auth/login`;
  const response = await fetch(loginUrl, {
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

  return {
    target,
    token: payload.data.token
  };
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

  const finalHeaders = buildHeaders(
    session,
    data ? { 'content-type': 'application/json', ...headers } : headers
  );

  const response = await fetch(url, {
    method,
    headers: finalHeaders,
    body: data ? JSON.stringify(data) : undefined
  });

  const text = await response.text();
  let payload = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch {
    payload = null;
  }

  return {
    method,
    url: url.toString(),
    status: response.status,
    payload,
    raw: text
  };
}

function assertResultOk(result) {
  assert(result.status >= 200 && result.status < 300, `HTTP ${result.status}`);
  assert(isObject(result.payload), 'response body is not JSON object');
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
    record.note = truncate(String(error), 300);
  }

  record.durationMs = Date.now() - startedAt;
  summary.checks.push(record);
  return record;
}

function pickArray(payloadData) {
  if (Array.isArray(payloadData)) return payloadData;
  if (Array.isArray(payloadData?.list)) return payloadData.list;
  if (Array.isArray(payloadData?.records)) return payloadData.records;
  return [];
}

function buildMarkdownReport() {
  const failed = summary.checks.filter((item) => item.status === 'failed').length;
  const passed = summary.checks.length - failed;

  return [
    '# Health API smoke report',
    '',
    `- run_id: ${summary.runId}`,
    `- month: ${summary.month}`,
    `- target: ${summary.target?.label || '-'} (${summary.target?.origin || '-'})`,
    `- started_at: ${summary.startedAt}`,
    `- finished_at: ${summary.finishedAt}`,
    '',
    '## Totals',
    '',
    `- passed: ${passed}`,
    `- failed: ${failed}`,
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
    ...(summary.warnings.length
      ? summary.warnings.map((item) => `- ${item}`)
      : ['- none'])
  ].join('\n');
}

const session = await resolveSession();
summary.target = session.target;
let sampleCalendarDate = TODAY_KEY;

await runCheck('auth.info', async () => {
  const result = await requestJson(session, 'GET', '/auth/info');
  assertResultOk(result);
  const data = result.payload.data;
  assert(isObject(data), 'auth info data is not object');
  assert(typeof data.name === 'string' && data.name.length > 0, 'auth info missing name');
  return data.name;
});

await runCheck('dashboard.overview', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/overview');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'dashboard overview data is not object');
});

await runCheck('dashboard.body-indicators', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/body-indicators');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'body indicators data is not object');
});

await runCheck('dashboard.warning-events', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/warning-events');
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'warning events data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('dashboard.pre-shift-compliance', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/pre-shift-compliance');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'pre-shift compliance data is not object');
});

await runCheck('dashboard.calendar', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/calendar', {
    query: { year: CURRENT_YEAR, month: CURRENT_MONTH }
  });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'calendar data is not array');
  sampleCalendarDate = result.payload.data.find((item) => item?.date)?.date || TODAY_KEY;
  return `${result.payload.data.length} days @ ${sampleCalendarDate}`;
});

await runCheck('dashboard.calendar.day-heart-rate', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/calendar/day-heart-rate', {
    query: { date: sampleCalendarDate }
  });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'day heart rate rank data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('dashboard.calendar.day-blood-oxygen', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/calendar/day-blood-oxygen', {
    query: { date: sampleCalendarDate }
  });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'day blood oxygen rank data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('dashboard.calendar.day-steps', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/calendar/day-steps', {
    query: { date: sampleCalendarDate }
  });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'day steps rank data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('dashboard.calendar.day-warnings', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/calendar/day-warnings', {
    query: { date: sampleCalendarDate }
  });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'day warnings data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('dashboard.mine-entry-list', async () => {
  const result = await requestJson(session, 'GET', '/dashboard/mine-entry-list', { query: { size: 20 } });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'mine entry list data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('realtime.overview', async () => {
  const result = await requestJson(session, 'GET', '/realtime/overview');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'realtime overview data is not object');
});

await runCheck('realtime.online-users', async () => {
  const result = await requestJson(session, 'GET', '/realtime/online-users', { query: { page: 1, size: 20 } });
  assertResultOk(result);
  assert(isObject(result.payload.data), 'online users data is not object');
  const list = pickArray(result.payload.data);
  return `${list.length} rows`;
});

await runCheck('realtime.statistics', async () => {
  const result = await requestJson(session, 'GET', '/realtime/statistics');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'realtime statistics data is not object');
});

await runCheck('realtime.alerts', async () => {
  const result = await requestJson(session, 'GET', '/realtime/alerts', { query: { limit: 20 } });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'realtime alerts data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('risk-warning.overview', async () => {
  const result = await requestJson(session, 'GET', '/risk-warning/overview');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'risk overview data is not object');
});

await runCheck('risk-warning.list', async () => {
  const result = await requestJson(session, 'GET', '/risk-warning/list', { query: { page: 1, size: 10 } });
  assertResultOk(result);
  assert(isObject(result.payload.data), 'risk list data is not object');
  const list = pickArray(result.payload.data);
  return `${list.length} rows`;
});

await runCheck('risk-warning.trend', async () => {
  const result = await requestJson(session, 'GET', '/risk-warning/trend', { query: { days: 30 } });
  assertResultOk(result);
  assert(isObject(result.payload.data), 'risk trend data is not object');
});

await runCheck('risk-warning.dept-stats', async () => {
  const result = await requestJson(session, 'GET', '/risk-warning/dept-stats');
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'risk dept stats data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('risk-warning.type-distribution', async () => {
  const result = await requestJson(session, 'GET', '/risk-warning/type-distribution');
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'risk type distribution data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('statistics.dept-summary', async () => {
  const result = await requestJson(session, 'GET', '/statistics/dept-summary');
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'dept summary data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('statistics.monthly-summary', async () => {
  const result = await requestJson(session, 'GET', '/statistics/monthly-summary', { query: { month: MONTH_KEY } });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'monthly summary data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('statistics.daily-counts', async () => {
  const result = await requestJson(session, 'GET', '/statistics/daily-counts', { query: { month: MONTH_KEY } });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'daily counts data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('statistics.warning-types', async () => {
  const result = await requestJson(session, 'GET', '/statistics/warning-types', { query: { month: MONTH_KEY } });
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'warning types data is not array');
  return `${result.payload.data.length} rows`;
});

await runCheck('trend-warning.predict', async () => {
  const result = await requestJson(session, 'GET', '/trend-warning/predict');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'trend warning predict data is not object');
});

let sampleEmpCode = '';

await runCheck('employee.list-detail', async () => {
  const result = await requestJson(session, 'GET', '/employee/list-detail');
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'employee list detail data is not array');
  const sample = result.payload.data.find((item) => item?.empCode || item?.userCode) || null;
  sampleEmpCode = sample?.empCode || sample?.userCode || '';
  if (!sampleEmpCode) {
    summary.warnings.push('employee.list-detail returned no sample empCode; portrait smoke was skipped');
    return 'no sample employee';
  }
  return sampleEmpCode;
});

await runCheck('employee.stats', async () => {
  const result = await requestJson(session, 'GET', '/employee/stats');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'employee stats data is not object');
});

await runCheck('health-portrait.sample', async () => {
  if (!sampleEmpCode) return 'skipped';
  const result = await requestJson(session, 'GET', `/health-portrait/${encodeURIComponent(sampleEmpCode)}`);
  assertResultOk(result);
  const data = result.payload.data;
  assert(isObject(data), 'health portrait data is not object');
  assert(isObject(data.vitals), 'health portrait missing vitals');
  assert(isObject(data.trend), 'health portrait missing trend');
  assert(Array.isArray(data.warnings), 'health portrait warnings is not array');
  return sampleEmpCode;
});

await runCheck('health-record.page', async () => {
  const result = await requestJson(session, 'GET', '/api/health/record/page', {
    query: { current: 1, size: 10, userCode: sampleEmpCode || undefined }
  });
  assertResultOk(result);
  assert(isObject(result.payload.data), 'health record page data is not object');
  assert(Array.isArray(result.payload.data.records), 'health record page records is not array');
  return `${result.payload.data.records.length} rows`;
});

await runCheck('device.online', async () => {
  const result = await requestJson(session, 'GET', '/api/device/online');
  assertResultOk(result);
  assert(isObject(result.payload.data), 'device online data is not object');
  assert(Array.isArray(result.payload.data.devices), 'device online devices is not array');
  return `${result.payload.data.count || result.payload.data.devices.length} devices`;
});

await runCheck('alert-config.list', async () => {
  const result = await requestJson(session, 'GET', '/alert-config/list');
  assertResultOk(result);
  assert(Array.isArray(result.payload.data), 'alert config list data is not array');
  return `${result.payload.data.length} rows`;
});

summary.finishedAt = new Date().toISOString();

await fs.writeFile(REPORT_JSON, JSON.stringify(summary, null, 2), 'utf8');
await fs.writeFile(REPORT_MD, buildMarkdownReport(), 'utf8');

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
