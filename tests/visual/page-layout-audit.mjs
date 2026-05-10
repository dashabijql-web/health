import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { chromium, devices } from 'playwright';
import {
  LOGIN_CREDENTIALS,
  pruneArtifacts,
  resolveFrontendBaseUrl,
  truncate
} from '../shared/health-test-utils.mjs';

const BASE_URL = await resolveFrontendBaseUrl();
const API_DATA_SOURCE = process.env.API_DATA_SOURCE || 'old';
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const ARTIFACT_DIR = path.resolve(process.cwd(), 'tests', 'visual', 'artifacts', RUN_ID);
const ARTIFACT_RETENTION = Number.parseInt(process.env.VISUAL_ARTIFACT_RETENTION || '8', 10);

const DEFAULT_ROUTES = [
  { slug: 'safety-command', path: '/safety-command/index' },
  { slug: 'dashboard', path: '/health-monitor/dashboard' },
  { slug: 'real-time', path: '/health-monitor/real-time' },
  { slug: 'risk-warning', path: '/health-monitor/risk-warning' },
  { slug: 'alert-notifications', path: '/alert-management/notifications' },
  { slug: 'report-center', path: '/health-monitor/report-center' },
  { slug: 'ai-chat', path: '/ai-chat/index' }
];

const VIEWPORTS = [
  { slug: 'desktop-1440', width: 1440, height: 900, deviceScaleFactor: 1 },
  { slug: 'desktop-1920', width: 1920, height: 1080, deviceScaleFactor: 1 },
  { slug: 'mobile-390', width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true },
  { slug: 'mobile-414', width: 414, height: 896, deviceScaleFactor: 2, isMobile: true, hasTouch: true }
];

function selectedRoutes() {
  const filter = new Set(
    (process.env.VISUAL_ROUTES || '')
      .split(',')
      .map((value) => value.trim())
      .filter(Boolean)
  );
  if (filter.size === 0) return DEFAULT_ROUTES;
  return DEFAULT_ROUTES.filter((route) => filter.has(route.slug));
}

const summary = {
  runId: RUN_ID,
  baseUrl: BASE_URL,
  dataSource: API_DATA_SOURCE,
  startedAt: new Date().toISOString(),
  routes: selectedRoutes().map((route) => route.slug),
  viewports: VIEWPORTS.map(({ slug, width, height }) => ({ slug, width, height })),
  results: [],
  issues: []
};

await fs.mkdir(ARTIFACT_DIR, { recursive: true });
await pruneArtifacts(path.dirname(ARTIFACT_DIR), ARTIFACT_RETENTION);

function issue(route, viewport, type, message, extra = {}) {
  const item = { route: route.slug, viewport: viewport.slug, type, message: truncate(message, 300), ...extra };
  summary.issues.push(item);
  return item;
}

async function loginViaApi(context, page) {
  const response = await context.request.post(`${BASE_URL}/dev-api/auth/login`, { data: LOGIN_CREDENTIALS });
  if (!response.ok()) throw new Error(`login HTTP ${response.status()}`);
  const payload = await response.json();
  if (payload?.code !== 200 || !payload?.data?.token) {
    throw new Error(`login rejected: ${payload?.message || 'missing token'}`);
  }
  await context.addCookies([
    { name: 'User-Token', value: payload.data.token, url: BASE_URL },
    { name: 'satoken', value: payload.data.token, url: BASE_URL },
    { name: 'Health-Data-Source', value: API_DATA_SOURCE, url: BASE_URL }
  ]);
  await page.addInitScript((dataSource) => {
    window.localStorage.setItem('Health-Data-Source', dataSource);
  }, API_DATA_SOURCE);
}

async function waitForShell(page) {
  await Promise.race([
    page.locator('.app-main').first().waitFor({ state: 'visible', timeout: 15000 }),
    page.locator('.login-container').first().waitFor({ state: 'visible', timeout: 15000 }),
    page.locator('.wscn-http404-container').first().waitFor({ state: 'visible', timeout: 15000 })
  ]).catch(() => {});
  await page.waitForLoadState('networkidle', { timeout: 12000 }).catch(() => {});
  await page.waitForTimeout(800);
}

async function collectLayoutIssues(page, route, viewport) {
  return await page.evaluate(({ routeSlug, viewportSlug }) => {
    const issues = [];
    const vw = document.documentElement.clientWidth;
    const bodyWidth = Math.max(document.body.scrollWidth, document.documentElement.scrollWidth);
    if (bodyWidth > vw + 2) {
      issues.push({ type: 'horizontal_overflow', message: `scrollWidth ${bodyWidth}px exceeds viewport ${vw}px` });
    }

    const isVisible = (element) => {
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return style.visibility !== 'hidden' && style.display !== 'none' && Number(style.opacity) !== 0 && rect.width > 0 && rect.height > 0;
    };

    const textNodes = Array.from(document.body.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span,a,button,label,td,th,.el-button,.el-tag'))
      .filter(isVisible)
      .map((element) => ({
        element,
        text: (element.innerText || element.textContent || '').trim().slice(0, 80),
        rect: element.getBoundingClientRect(),
        tag: element.tagName.toLowerCase()
      }))
      .filter((item) => item.text && item.rect.width > 3 && item.rect.height > 3)
      .slice(0, 220);

    const isAncestorPair = (a, b) => a.element !== b.element && (a.element.contains(b.element) || b.element.contains(a.element));
    const hasSameRoundedRect = (a, b) => ['left', 'top', 'right', 'bottom'].every((key) => Math.round(a.rect[key]) === Math.round(b.rect[key]));
    const closest = (item, selector) => item.element.closest(selector);
    const isStructuredHeaderBodyPair = (a, b) => {
      const aHeader = closest(a, '.rw-list-hd, thead, .el-table__header-wrapper');
      const bHeader = closest(b, '.rw-list-hd, thead, .el-table__header-wrapper');
      const aBody = closest(a, '.rw-list-row, tbody, .el-table__body-wrapper');
      const bBody = closest(b, '.rw-list-row, tbody, .el-table__body-wrapper');
      return (aHeader && bBody) || (bHeader && aBody);
    };
    const rectsOverlap = (a, b, pad = 4) => Math.min(a.right, b.right) - Math.max(a.left, b.left) > pad && Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top) > pad;
    const isTickerPair = (a, b) => closest(a, '.rt-ticker-wrap') && closest(b, '.rt-ticker-wrap');
    const isClippedTickerArtifact = (a, b) => {
      const aTicker = closest(a, '.rt-ticker-wrap');
      const bTicker = closest(b, '.rt-ticker-wrap');
      if (!aTicker && !bTicker) return false;
      if (aTicker && bTicker) return true;
      const tickerRect = (aTicker || bTicker).getBoundingClientRect();
      const otherRect = aTicker ? b.rect : a.rect;
      return !rectsOverlap(tickerRect, otherRect, 4);
    };

    for (let i = 0; i < textNodes.length; i += 1) {
      for (let j = i + 1; j < textNodes.length; j += 1) {
        if (isAncestorPair(textNodes[i], textNodes[j])) continue;
        if (isStructuredHeaderBodyPair(textNodes[i], textNodes[j])) continue;
        if (isTickerPair(textNodes[i], textNodes[j])) continue;
        if (isClippedTickerArtifact(textNodes[i], textNodes[j])) continue;
        if (textNodes[i].text === textNodes[j].text && hasSameRoundedRect(textNodes[i], textNodes[j])) continue;
        const a = textNodes[i].rect;
        const b = textNodes[j].rect;
        const xOverlap = Math.min(a.right, b.right) - Math.max(a.left, b.left);
        const yOverlap = Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top);
        if (xOverlap > 4 && yOverlap > 4) {
          const overlapArea = xOverlap * yOverlap;
          const smallerArea = Math.min(a.width * a.height, b.width * b.height);
          if (smallerArea > 0 && overlapArea / smallerArea > 0.35) {
            issues.push({
              type: 'text_overlap',
              message: `text boxes overlap: "${textNodes[i].text}" / "${textNodes[j].text}"`,
              overlap: { x: Math.round(xOverlap), y: Math.round(yOverlap) }
            });
            if (issues.filter((entry) => entry.type === 'text_overlap').length >= 5) break;
          }
        }
      }
      if (issues.filter((entry) => entry.type === 'text_overlap').length >= 5) break;
    }

    if (vw >= 1000) {
      const charts = Array.from(document.querySelectorAll('.echarts, canvas, [data-visual-audit-chart="true"], .health-chart, .metric-chart, .trend-chart, .dashboard-chart, .rc-chart'))
        .filter(isVisible)
        .filter((element) => {
          const rect = element.getBoundingClientRect();
          const className = String(element.className || '');
          const tag = element.tagName.toLowerCase();
          if (tag === 'svg' && rect.width < 240 && rect.height < 160) return false;
          if (/icon|spark|mini|thumb|avatar|badge|dot/i.test(className)) return false;
          return rect.width >= 120 || rect.height >= 120;
        })
        .map((element) => element.getBoundingClientRect())
        .filter((rect) => rect.width > 20 || rect.height > 20);
      charts.forEach((rect, index) => {
        if (rect.width < 240 || rect.height < 160) {
          issues.push({
            type: 'chart_container_too_small',
            message: `chart-like container #${index + 1} is ${Math.round(rect.width)}x${Math.round(rect.height)}`
          });
        }
      });
    }

    const actionButtons = Array.from(document.querySelectorAll('button,.el-button,[role="button"]')).filter(isVisible);
    if (actionButtons.length === 0) {
      issues.push({ type: 'no_primary_action_visible', message: 'no visible button/action found on page' });
    }

    const clippedTables = Array.from(document.querySelectorAll('.el-table__body-wrapper,.el-table,.el-table__inner-wrapper'))
      .filter(isVisible)
      .filter((element) => {
        const scrollHost = element.closest('.rt-tbl-wrap');
        if (!scrollHost) return true;
        const hostRect = scrollHost.getBoundingClientRect();
        const hostStyle = window.getComputedStyle(scrollHost);
        const canScrollX = ['auto', 'scroll'].includes(hostStyle.overflowX) && scrollHost.scrollWidth > scrollHost.clientWidth + 2;
        return !(canScrollX && hostRect.left >= -2 && hostRect.right <= vw + 2);
      })
      .map((element) => element.getBoundingClientRect())
      .filter((rect) => rect.right > vw + 2 || rect.left < -2);
    clippedTables.forEach((rect) => {
      issues.push({
        type: 'table_clipped_outside_viewport',
        message: `table wrapper outside viewport: left=${Math.round(rect.left)}, right=${Math.round(rect.right)}, viewport=${vw}`
      });
    });

    return issues.map((entry) => ({ route: routeSlug, viewport: viewportSlug, ...entry }));
  }, { routeSlug: route.slug, viewportSlug: viewport.slug });
}

async function auditRoute(browser, route, viewport) {
  const contextOptions = viewport.isMobile ? { ...devices['iPhone 13'], viewport: { width: viewport.width, height: viewport.height } } : { viewport };
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  const result = { route: route.slug, path: route.path, viewport: viewport.slug, status: 'passed', screenshot: null, issues: [] };
  try {
    await loginViaApi(context, page);
    await page.goto(`${BASE_URL}/#${route.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
    await waitForShell(page);
    result.finalHash = await page.evaluate(() => window.location.hash);
    if (result.finalHash.includes('/login')) {
      result.issues.push(issue(route, viewport, 'redirected_to_login', 'route redirected to login'));
    }
    if (result.finalHash.includes('/404')) {
      result.issues.push(issue(route, viewport, 'not_found', 'route rendered 404'));
    }
    const layoutIssues = await collectLayoutIssues(page, route, viewport);
    for (const found of layoutIssues) {
      result.issues.push(issue(route, viewport, found.type, found.message, found));
    }
    const screenshotName = `${route.slug}-${viewport.slug}.png`;
    result.screenshot = path.join(ARTIFACT_DIR, screenshotName);
    await page.screenshot({ path: result.screenshot, fullPage: true });
    if (result.issues.length > 0) result.status = 'failed';
  } catch (error) {
    result.status = 'failed';
    result.issues.push(issue(route, viewport, 'audit_error', error.message || String(error)));
  } finally {
    await context.close();
  }
  summary.results.push(result);
}

const browser = await chromium.launch({ headless: true });
try {
  for (const route of selectedRoutes()) {
    for (const viewport of VIEWPORTS) {
      await auditRoute(browser, route, viewport);
    }
  }
} finally {
  await browser.close();
}

summary.finishedAt = new Date().toISOString();
summary.failedRoutes = new Set(summary.results.filter((result) => result.status !== 'passed').map((result) => result.route)).size;
summary.failedChecks = summary.results.filter((result) => result.status !== 'passed').length;
summary.status = summary.failedChecks === 0 ? 'passed' : 'failed';
summary.routeSummaries = summary.routes.map((routeSlug) => {
  const routeResults = summary.results.filter((result) => result.route === routeSlug);
  const routeIssues = summary.issues.filter((item) => item.route === routeSlug);
  return {
    route: routeSlug,
    status: routeResults.every((result) => result.status === 'passed') ? 'passed' : 'failed',
    passedViewports: routeResults.filter((result) => result.status === 'passed').length,
    failedViewports: routeResults.filter((result) => result.status !== 'passed').length,
    issueCount: routeIssues.length,
    screenshots: routeResults.map((result) => ({
      viewport: result.viewport,
      status: result.status,
      file: result.screenshot ? path.basename(result.screenshot) : null
    }))
  };
});

const jsonPath = path.join(ARTIFACT_DIR, 'layout-summary.json');
const mdPath = path.join(ARTIFACT_DIR, 'layout-summary.md');
await fs.writeFile(jsonPath, JSON.stringify(summary, null, 2));

const lines = [
  `# Visual Layout Audit ${RUN_ID}`,
  '',
  `- status: ${summary.status}`,
  `- base_url: ${BASE_URL}`,
  `- data_source: ${API_DATA_SOURCE}`,
  `- failed_routes: ${summary.failedRoutes}`,
  `- failed_checks: ${summary.failedChecks}`,
  `- artifact_dir: ${ARTIFACT_DIR}`,
  `- rerun_all: npm run audit:visual`,
  `- rerun_one_route: VISUAL_ROUTES=<route-slug> npm run audit:visual`,
  '',
  '## Route Summary',
  '',
  '| route | status | passed viewports | failed viewports | issues | screenshots |',
  '| --- | --- | ---: | ---: | ---: | --- |',
  ...summary.routeSummaries.map((route) => {
    const screenshots = route.screenshots
      .map((screenshot) => `${screenshot.viewport}:${screenshot.file || '-'}`)
      .join(', ');
    return `| ${route.route} | ${route.status} | ${route.passedViewports} | ${route.failedViewports} | ${route.issueCount} | ${screenshots} |`;
  }),
  '',
  '## Route / Viewport Results',
  '',
  ...summary.results.map((result) => `- ${result.status === 'passed' ? 'PASS' : 'FAIL'} ${result.route} ${result.viewport} screenshot=${path.basename(result.screenshot || '')}`)
];

if (summary.issues.length > 0) {
  lines.push('', '## Issues', '');
  for (const item of summary.issues) {
    lines.push(`- ${item.route} ${item.viewport} ${item.type}: ${item.message}`);
  }
}

await fs.writeFile(mdPath, `${lines.join('\n')}\n`);
console.log(`VISUAL_AUDIT_${summary.status.toUpperCase()} failedRoutes=${summary.failedRoutes} artifactDir=${ARTIFACT_DIR}`);
if (summary.status !== 'passed') process.exitCode = 1;
