import fs from 'node:fs/promises';
import net from 'node:net';
import path from 'node:path';
import process from 'node:process';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { chromium } from 'playwright';

const execFileAsync = promisify(execFile);

const LOGIN_CREDENTIALS = { username: 'admin', password: 'admin123' };
const DEFAULT_BASE_URL = 'http://127.0.0.1:9528';
const SQLCMD_BIN = process.env.SQLCMD_BIN || 'sqlcmd';
const SQL_SERVER = process.env.SQL_SERVER || 'localhost,58135';
const SQL_USER = process.env.SQL_USER || 'sa';
const SQL_PASSWORD = process.env.SQL_PASSWORD || '123abcd.';
const SQL_DB = process.env.SQL_DB || 'health';
const TCP_HOST = process.env.WATCH_TCP_HOST || '127.0.0.1';
const TCP_PORT = Number.parseInt(process.env.WATCH_TCP_PORT || '9000', 10);
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1';
const REDIS_PORT = Number.parseInt(process.env.REDIS_PORT || '6379', 10);
const REDIS_TIMEOUT_MS = Number.parseInt(process.env.REDIS_TIMEOUT_MS || '3000', 10);
const PIPELINE_SQL_WAIT_MS = Number.parseInt(process.env.PIPELINE_SQL_WAIT_MS || '20000', 10);
const PIPELINE_REDIS_WAIT_MS = Number.parseInt(process.env.PIPELINE_REDIS_WAIT_MS || '8000', 10);
const ARTIFACT_RETENTION = Number.parseInt(process.env.PIPELINE_ARTIFACT_RETENTION || '5', 10);
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const ARTIFACT_DIR = path.resolve(process.cwd(), 'tests', 'pipeline', 'artifacts', RUN_ID);
const REPORT_JSON = path.join(ARTIFACT_DIR, 'summary.json');
const REPORT_MD = path.join(ARTIFACT_DIR, 'summary.md');
const SCREENSHOT_PATH = path.join(ARTIFACT_DIR, 'employee-profile.jpeg');
const MONTH_SUFFIX = new Date().toISOString().slice(0, 7).replace('-', '');
const HEALTH_TABLE = `health_record_${MONTH_SUFFIX}`;

const PROBE = {
  heartRate: 77,
  systolic: 118,
  diastolic: 76,
  bloodOxygen: 98,
  temperature: 36.8,
  temperatureStored: 368,
  steps: 23456,
  calories: 987,
  rollovers: 12,
  glucose: 5.1
};

const TARGETS = [
  { label: 'vite-127', origin: 'http://127.0.0.1:9528', apiPrefix: '/dev-api' },
  { label: 'vite-localhost', origin: 'http://localhost:9528', apiPrefix: '/dev-api' },
  { label: 'backend-127', origin: 'http://127.0.0.1:8080', apiPrefix: '/health' },
  { label: 'backend-localhost', origin: 'http://localhost:8080', apiPrefix: '/health' }
];

const summary = {
  runId: RUN_ID,
  startedAt: new Date().toISOString(),
  baseUrl: '',
  target: null,
  table: HEALTH_TABLE,
  probe: { ...PROBE },
  stages: [],
  warnings: [],
  cleanup: {
    deletedRows: 0,
    redisTrimmed: 0
  }
};

await fs.mkdir(ARTIFACT_DIR, { recursive: true });
await pruneArtifacts(path.dirname(ARTIFACT_DIR), ARTIFACT_RETENTION);

function truncate(value, max = 240) {
  if (!value) return '';
  return value.length > max ? `${value.slice(0, max - 3)}...` : value;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function stage(name, status, note = '') {
  summary.stages.push({ name, status, note, at: new Date().toISOString() });
}

function normalizeTemp(value) {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  if (!Number.isFinite(n)) return null;
  return Number((n > 100 ? n / 10 : n).toFixed(1));
}

async function pruneArtifacts(rootDir, keep = 5) {
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
    status: response.status,
    payload,
    raw: text,
    url: url.toString()
  };
}

function assertResultOk(result) {
  assert(result.status >= 200 && result.status < 300, `HTTP ${result.status}`);
  assert(result.payload && typeof result.payload === 'object', 'response body is not JSON object');
  assert(result.payload.code === 200, result.payload.message || `unexpected code ${result.payload.code}`);
}

async function sqlRaw(query) {
  const { stdout, stderr } = await execFileAsync(
    SQLCMD_BIN,
    [
      '-S',
      SQL_SERVER,
      '-U',
      SQL_USER,
      '-P',
      SQL_PASSWORD,
      '-d',
      SQL_DB,
      '-w',
      '65535',
      '-y',
      '0',
      '-Y',
      '0',
      '-h-1',
      '-Q',
      `SET NOCOUNT ON; ${query}`
    ],
    { encoding: 'utf8', windowsHide: true, timeout: 120000 }
  );

  if (stderr && stderr.trim()) {
    throw new Error(stderr.trim());
  }

  return stdout.trim();
}

function parseJsonPayload(raw) {
  const start = Math.min(
    ...['[', '{']
      .map((char) => raw.indexOf(char))
      .filter((index) => index >= 0)
  );
  if (!Number.isFinite(start)) {
    throw new Error(`sql output missing JSON payload: ${truncate(raw, 300)}`);
  }
  return JSON.parse(raw.slice(start));
}

async function sqlJson(query) {
  const raw = await sqlRaw(`${query} FOR JSON PATH`);
  return raw ? parseJsonPayload(raw) : [];
}

async function sqlJsonObject(query) {
  const raw = await sqlRaw(`${query} FOR JSON PATH, WITHOUT_ARRAY_WRAPPER`);
  return raw ? parseJsonPayload(raw) : null;
}

async function getProbeTarget() {
  const row = await sqlJsonObject(
    "SELECT TOP 1 d.id AS deviceId, d.imei, du.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName " +
    "FROM device d " +
    "JOIN device_user du ON du.device_id = d.id AND du.unbind_time IS NULL " +
    "JOIN employee e ON e.id = du.emp_id " +
    "WHERE d.imei IS NOT NULL AND LEN(d.imei) = 15 " +
    "ORDER BY d.id"
  );
  assert(row?.imei && row?.empCode, 'no bound probe target found');
  return row;
}

async function getBaseline(target) {
  const row = await sqlJsonObject(
    `SELECT ISNULL(MAX(id), 0) AS maxId, COUNT(*) AS totalCount ` +
    `FROM ${HEALTH_TABLE} WHERE user_code = '${target.empCode}'`
  );
  return {
    maxId: Number(row?.maxId || 0),
    totalCount: Number(row?.totalCount || 0)
  };
}

async function queryProbeRows(target, baseline) {
  return sqlJson(
    `SELECT id, user_code AS userCode, heart_rate AS heartRate, blood_oxygen AS bloodOxygen, ` +
    `blood_pressure_high AS systolic, blood_pressure_low AS diastolic, temperature, steps, calories, record_time AS recordTime ` +
    `FROM ${HEALTH_TABLE} ` +
    `WHERE user_code = '${target.empCode}' AND id > ${baseline.maxId} AND (` +
    `(heart_rate = ${PROBE.heartRate} AND blood_oxygen = ${PROBE.bloodOxygen} ` +
    `AND blood_pressure_high = ${PROBE.systolic} AND blood_pressure_low = ${PROBE.diastolic} ` +
    `AND CAST(ROUND(CAST(temperature AS FLOAT), 0) AS INT) = ${PROBE.temperatureStored}) ` +
    `OR (steps = ${PROBE.steps} AND calories = ${PROBE.calories})` +
    `) ORDER BY id`
  );
}

async function deleteProbeRows(rowIds) {
  if (!rowIds.length) return 0;
  const row = await sqlJsonObject(
    `DELETE FROM ${HEALTH_TABLE} WHERE id IN (${rowIds.join(',')}); SELECT @@ROWCOUNT AS deletedRows`
  );
  return Number(row?.deletedRows || 0);
}

async function verifyNoProbeRows(target, baseline) {
  const row = await sqlJsonObject(
    `SELECT COUNT(*) AS probeCount FROM ${HEALTH_TABLE} ` +
    `WHERE user_code = '${target.empCode}' AND id > ${baseline.maxId} AND (` +
    `(heart_rate = ${PROBE.heartRate} AND blood_oxygen = ${PROBE.bloodOxygen} ` +
    `AND blood_pressure_high = ${PROBE.systolic} AND blood_pressure_low = ${PROBE.diastolic} ` +
    `AND CAST(ROUND(CAST(temperature AS FLOAT), 0) AS INT) = ${PROBE.temperatureStored}) ` +
    `OR (steps = ${PROBE.steps} AND calories = ${PROBE.calories})` +
    `)`
  );
  return Number(row?.probeCount || 0);
}

class RedisSocketClient {
  constructor(sock) {
    this.sock = sock;
    this.buffer = Buffer.alloc(0);
    this.waiters = [];
    this.closed = false;
    this.sock.on('data', (chunk) => {
      this.buffer = Buffer.concat([this.buffer, chunk]);
      this.flushWaiters();
    });
    this.sock.on('close', () => {
      this.closed = true;
      this.flushWaiters();
    });
    this.sock.on('error', () => {
      this.closed = true;
      this.flushWaiters();
    });
  }

  static async connect(host, port, timeoutMs = 3000) {
    const sock = net.createConnection({ host, port });
    await Promise.race([
      new Promise((resolve, reject) => {
        sock.once('connect', resolve);
        sock.once('error', reject);
      }),
      new Promise((_, reject) => setTimeout(() => reject(new Error('redis connect timeout')), timeoutMs))
    ]);
    sock.setTimeout(timeoutMs);
    return new RedisSocketClient(sock);
  }

  close() {
    this.sock.destroy();
  }

  flushWaiters() {
    const waiters = this.waiters.splice(0, this.waiters.length);
    for (const waiter of waiters) waiter();
  }

  async waitForData() {
    if (this.buffer.length > 0 || this.closed) return;
    await new Promise((resolve) => this.waiters.push(resolve));
  }

  async execute(...parts) {
    const chunks = [Buffer.from(`*${parts.length}\r\n`, 'utf8')];
    for (const part of parts) {
      const raw = Buffer.from(String(part), 'utf8');
      chunks.push(Buffer.from(`$${raw.length}\r\n`, 'utf8'));
      chunks.push(raw);
      chunks.push(Buffer.from('\r\n', 'utf8'));
    }
    this.sock.write(Buffer.concat(chunks));
    return this.readReply();
  }

  async readReply() {
    while (true) {
      const parsed = await this.tryParseReply(0);
      if (parsed) {
        this.buffer = this.buffer.subarray(parsed.nextOffset);
        return parsed.value;
      }
      await this.waitForData();
      if (this.closed && this.buffer.length === 0) {
        throw new Error('redis connection closed');
      }
    }
  }

  async tryParseReply(offset) {
    if (this.buffer.length <= offset) return null;
    const prefix = String.fromCharCode(this.buffer[offset]);
    if (prefix === '+' || prefix === '-' || prefix === ':') {
      const lineEnd = this.buffer.indexOf('\r\n', offset);
      if (lineEnd === -1) return null;
      const line = this.buffer.toString('utf8', offset + 1, lineEnd);
      if (prefix === '-') throw new Error(line);
      if (prefix === ':') return { value: Number.parseInt(line, 10), nextOffset: lineEnd + 2 };
      return { value: line, nextOffset: lineEnd + 2 };
    }

    if (prefix === '$') {
      const lineEnd = this.buffer.indexOf('\r\n', offset);
      if (lineEnd === -1) return null;
      const length = Number.parseInt(this.buffer.toString('utf8', offset + 1, lineEnd), 10);
      if (length === -1) return { value: null, nextOffset: lineEnd + 2 };
      const bodyStart = lineEnd + 2;
      const bodyEnd = bodyStart + length;
      if (this.buffer.length < bodyEnd + 2) return null;
      const value = this.buffer.toString('utf8', bodyStart, bodyEnd);
      return { value, nextOffset: bodyEnd + 2 };
    }

    if (prefix === '*') {
      const lineEnd = this.buffer.indexOf('\r\n', offset);
      if (lineEnd === -1) return null;
      const count = Number.parseInt(this.buffer.toString('utf8', offset + 1, lineEnd), 10);
      if (count === -1) return { value: null, nextOffset: lineEnd + 2 };
      let nextOffset = lineEnd + 2;
      const values = [];
      for (let i = 0; i < count; i += 1) {
        const child = await this.tryParseReply(nextOffset);
        if (!child) return null;
        values.push(child.value);
        nextOffset = child.nextOffset;
      }
      return { value: values, nextOffset };
    }

    throw new Error(`unsupported redis reply prefix: ${prefix}`);
  }
}

async function findProbePayloads(redisClient, target) {
  const items = await redisClient.execute('LRANGE', 'health:buffer', '0', '-1');
  const rows = Array.isArray(items) ? items.filter((item) => typeof item === 'string') : [];
  return rows.filter((row) => {
    const matchUser = row.includes(`"userCode":"${target.empCode}"`);
    const matchVitals =
      row.includes(`"heartRate":${PROBE.heartRate}`) &&
      row.includes(`"bloodOxygen":${PROBE.bloodOxygen}`);
    const matchSteps = row.includes(`"steps":${PROBE.steps}`) && row.includes(`"calories":${PROBE.calories}`);
    return matchUser && (matchVitals || matchSteps);
  });
}

async function observeRedis(redisClient, target) {
  const startedAt = Date.now();
  const matchedPayloads = new Set();
  while (Date.now() - startedAt < PIPELINE_REDIS_WAIT_MS) {
    const payloads = await findProbePayloads(redisClient, target);
    for (const payload of payloads) matchedPayloads.add(payload);
    if (matchedPayloads.size > 0) {
      return { matched: true, payloads: [...matchedPayloads] };
    }
    await sleep(150);
  }
  return { matched: false, payloads: [...matchedPayloads] };
}

async function cleanupRedisPayloads(redisClient, payloads) {
  let removed = 0;
  for (const payload of payloads) {
    try {
      const delta = await redisClient.execute('LREM', 'health:buffer', '0', payload);
      removed += Number(delta || 0);
    } catch {
      // Best-effort cleanup.
    }
  }
  return removed;
}

async function sendPacket(socket, packet) {
  socket.write(packet, 'utf8');
  await sleep(180);
}

async function sendProbePackets(target) {
  const socket = await new Promise((resolve, reject) => {
    const conn = net.createConnection({ host: TCP_HOST, port: TCP_PORT }, () => resolve(conn));
    conn.once('error', reject);
  });

  socket.setTimeout(5000);
  socket.on('data', () => {
    // Server responses are optional for this regression; ignore the body.
  });

  try {
    await sendPacket(socket, `IW*AP00*${target.imei}#`);
    await sleep(1200);
    await sendPacket(socket, `IW*AP03*1,${PROBE.steps},${PROBE.rollovers},${PROBE.calories}#`);
    await sleep(1100);
    await sendPacket(
      socket,
      `IW*APHP*${PROBE.heartRate},${PROBE.systolic},${PROBE.diastolic},${PROBE.bloodOxygen},${PROBE.glucose},${PROBE.temperature},,,,,,,#`
    );
    await sleep(1100);
    await sendPacket(
      socket,
      `IW*APHP*${PROBE.heartRate},${PROBE.systolic},${PROBE.diastolic},${PROBE.bloodOxygen},${PROBE.glucose},${PROBE.temperature},,,,,,,#`
    );
    await sleep(300);
  } finally {
    socket.end();
    socket.destroy();
  }
}

async function waitForSqlRows(target, baseline) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < PIPELINE_SQL_WAIT_MS) {
    const rows = await queryProbeRows(target, baseline);
    if (rows.length >= 2) {
      return rows;
    }
    await sleep(500);
  }
  return [];
}

function findProbeRow(rows, predicate) {
  return rows.find((row) => predicate(row)) || null;
}

async function launchBrowser() {
  for (const channel of ['msedge', 'chrome']) {
    try {
      return await chromium.launch({ headless: true, channel });
    } catch {
      // Try the next browser channel.
    }
  }
  return chromium.launch({ headless: true });
}

async function loginViaApi(page, session) {
  await page.context().addCookies([
    {
      name: 'User-Token',
      value: session.token,
      url: session.target.origin
    },
    {
      name: 'satoken',
      value: session.token,
      url: session.target.origin
    }
  ]);
}

async function verifyEmployeeProfilePage(session, target) {
  const browser = await launchBrowser();
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } });
    const page = await context.newPage();
    await loginViaApi(page, session);
    const route = `${session.target.origin}/#/health-monitor/employee-profile?empCode=${encodeURIComponent(target.empCode)}&empName=${encodeURIComponent(target.empCode)}`;
    await page.goto(route, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('.ep-page', { timeout: 20000 });
    await page.waitForFunction(
      (probe) => {
        const hr = document.querySelector('.ep-vital-card.hr .ep-vc-val')?.textContent || '';
        const spo2 = document.querySelector('.ep-vital-card.spo2 .ep-vc-val')?.textContent || '';
        const temp = document.querySelector('.ep-vital-card.temp .ep-vc-val')?.textContent || '';
        return hr.includes(String(probe.heartRate)) &&
          spo2.includes(String(probe.bloodOxygen)) &&
          temp.includes(probe.temperature.toFixed(1));
      },
      PROBE,
      { timeout: 20000 }
    );
    await page.screenshot({ path: SCREENSHOT_PATH, type: 'jpeg', quality: 72 });

    const snapshot = await page.evaluate(() => ({
      hr: document.querySelector('.ep-vital-card.hr .ep-vc-val')?.textContent?.trim() || '',
      spo2: document.querySelector('.ep-vital-card.spo2 .ep-vc-val')?.textContent?.trim() || '',
      temp: document.querySelector('.ep-vital-card.temp .ep-vc-val')?.textContent?.trim() || '',
      bp: document.querySelector('.ep-vital-card.steps')?.textContent?.trim() || '',
      profileCode: document.querySelector('.ep-bi .code')?.textContent?.trim() || ''
    }));

    await context.close();
    return snapshot;
  } finally {
    await browser.close();
  }
}

function buildMarkdownReport() {
  return [
    '# Health pipeline regression report',
    '',
    `- run_id: ${summary.runId}`,
    `- started_at: ${summary.startedAt}`,
    `- finished_at: ${summary.finishedAt}`,
    `- base_url: ${summary.baseUrl}`,
    `- target: ${summary.target ? `${summary.target.empCode} / ${summary.target.imei}` : '-'}`,
    `- health_table: ${summary.table}`,
    '',
    '## Stages',
    '',
    '| stage | status | note |',
    '| --- | --- | --- |',
    ...summary.stages.map((item) => `| ${item.name} | ${item.status} | ${item.note || '-'} |`),
    '',
    '## Cleanup',
    '',
    `- deleted_rows: ${summary.cleanup.deletedRows}`,
    `- redis_trimmed: ${summary.cleanup.redisTrimmed}`,
    '',
    '## Warnings',
    '',
    ...(summary.warnings.length ? summary.warnings.map((item) => `- ${item}`) : ['- none'])
  ].join('\n');
}

let redisClient = null;
let redisPayloads = [];
let cleanupRowIds = [];
let baselineSnapshot = null;
let fatalError = null;

try {
  const session = await resolveSession();
  summary.baseUrl = session.target.origin;
  stage('resolve-session', 'passed', `${session.target.label} (${session.target.origin})`);

  const target = await getProbeTarget();
  summary.target = target;
  stage('pick-target', 'passed', `${target.empCode} / ${target.imei}`);

  baselineSnapshot = await getBaseline(target);
  stage('baseline', 'passed', `max_id=${baselineSnapshot.maxId}, count=${baselineSnapshot.totalCount}`);

  redisClient = await RedisSocketClient.connect(REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT_MS);
  stage('redis-connect', 'passed', `${REDIS_HOST}:${REDIS_PORT}`);

  await sendProbePackets(target);
  stage('tcp-probe', 'passed', `${TCP_HOST}:${TCP_PORT} <- ${target.imei}`);

  const redisObservation = await observeRedis(redisClient, target);
  assert(redisObservation.matched, 'probe payload was not observed in Redis health:buffer');
  redisPayloads = redisObservation.payloads;
  stage('redis-buffer', 'passed', `${redisPayloads.length} payload(s) observed`);

  const probeRows = await waitForSqlRows(target, baselineSnapshot);
  assert(probeRows.length >= 2, `expected probe rows in ${HEALTH_TABLE}, found ${probeRows.length}`);

  const vitalsRow = findProbeRow(
    probeRows,
    (row) =>
      Number(row.heartRate) === PROBE.heartRate &&
      Number(row.bloodOxygen) === PROBE.bloodOxygen &&
      Number(row.systolic) === PROBE.systolic &&
      Number(row.diastolic) === PROBE.diastolic &&
      normalizeTemp(row.temperature) === PROBE.temperature
  );
  const movementRow = findProbeRow(
    probeRows,
    (row) => Number(row.steps) === PROBE.steps && Number(row.calories) === PROBE.calories
  );

  assert(vitalsRow, 'vitals probe row not found in SQL');
  assert(movementRow, 'heartbeat probe row not found in SQL');
  cleanupRowIds = [...new Set(probeRows.map((row) => Number(row.id)).filter(Number.isFinite))];
  stage('sql-flush', 'passed', `rows=${cleanupRowIds.join(',')}`);

  const healthRecords = await requestJson(session, 'GET', '/api/health/record/page', {
    query: { current: 1, size: 20, userCode: target.empCode }
  });
  assertResultOk(healthRecords);
  const recordList = Array.isArray(healthRecords.payload?.data?.records) ? healthRecords.payload.data.records : [];
  assert(recordList.some((row) => cleanupRowIds.includes(Number(row.id))), 'health record page missing probe rows');
  stage('api.health-record-page', 'passed', `${recordList.length} rows`);

  const portrait = await requestJson(session, 'GET', `/health-portrait/${encodeURIComponent(target.empCode)}`);
  assertResultOk(portrait);
  const portraitVitals = portrait.payload?.data?.vitals || {};
  assert(Number(portraitVitals.heartRate) === PROBE.heartRate, 'health portrait heartRate mismatch');
  assert(Number(portraitVitals.bloodOxygen) === PROBE.bloodOxygen, 'health portrait bloodOxygen mismatch');
  assert(normalizeTemp(portraitVitals.temperature) === PROBE.temperature, 'health portrait temperature mismatch');
  stage(
    'api.health-portrait',
    'passed',
    `hr=${portraitVitals.heartRate}, spo2=${portraitVitals.bloodOxygen}, temp=${normalizeTemp(portraitVitals.temperature)}`
  );

  const realtime = await requestJson(session, 'GET', `/realtime/user/${encodeURIComponent(target.empCode)}`);
  assertResultOk(realtime);
  const realtimeData = realtime.payload?.data || {};
  assert(Number(realtimeData.heartRate) === PROBE.heartRate, 'realtime user heartRate mismatch');
  assert(Number(realtimeData.bloodOxygen) === PROBE.bloodOxygen, 'realtime user bloodOxygen mismatch');
  assert(normalizeTemp(realtimeData.temperature) === PROBE.temperature, 'realtime user temperature mismatch');
  stage(
    'api.realtime-user',
    'passed',
    `hr=${realtimeData.heartRate}, spo2=${realtimeData.bloodOxygen}, temp=${normalizeTemp(realtimeData.temperature)}`
  );

  const pageSnapshot = await verifyEmployeeProfilePage(session, target);
  stage(
    'page.employee-profile',
    'passed',
    `${pageSnapshot.hr} | ${pageSnapshot.spo2} | ${pageSnapshot.temp}`
  );
} catch (error) {
  fatalError = error;
  stage('pipeline', 'failed', truncate(String(error), 320));
} finally {
  if (redisClient && redisPayloads.length > 0) {
    summary.cleanup.redisTrimmed = await cleanupRedisPayloads(redisClient, redisPayloads);
  }
  if (redisClient) {
    redisClient.close();
  }

  if (cleanupRowIds.length > 0) {
    summary.cleanup.deletedRows = await deleteProbeRows(cleanupRowIds);
    const residue = baselineSnapshot ? await verifyNoProbeRows(summary.target, baselineSnapshot) : 0;
    if (residue > 0) {
      summary.warnings.push(`probe residue still present after cleanup: ${residue}`);
    }
  }

  summary.finishedAt = new Date().toISOString();
  await fs.writeFile(REPORT_JSON, JSON.stringify(summary, null, 2), 'utf8');
  await fs.writeFile(REPORT_MD, buildMarkdownReport(), 'utf8');
}

const failedStages = summary.stages.filter((item) => item.status === 'failed').length;

console.log(JSON.stringify({
  artifactDir: ARTIFACT_DIR,
  reportFile: REPORT_MD,
  stageCount: summary.stages.length,
  failedStages,
  warnings: summary.warnings.length,
  cleanup: summary.cleanup
}, null, 2));

if (fatalError) {
  console.error(truncate(String(fatalError), 400));
}

if (failedStages > 0) {
  process.exitCode = 1;
}
