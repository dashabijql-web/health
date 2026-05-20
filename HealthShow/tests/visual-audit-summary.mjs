import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const ROOT = process.cwd()
const auditPath = path.join(ROOT, 'tests', 'visual', 'page-layout-audit.mjs')
const auditSource = fs.readFileSync(auditPath, 'utf8')

test('visual audit default route inventory includes warning lifecycle records route', () => {
  assert.match(auditSource, /slug:\s*'alert-notifications'/)
  assert.match(auditSource, /slug:\s*'alert-records'/)
  assert.match(auditSource, /path:\s*'\/alert-management\/records'/)
  assert.match(auditSource, /slug:\s*'workbench'/)
  assert.match(auditSource, /path:\s*'\/health-monitor\/workbench'/)
  assert.match(auditSource, /slug:\s*'employee-profile'/)
  assert.match(auditSource, /path:\s*'\/health-monitor\/employee-profile'/)
  assert.match(auditSource, /slug:\s*'mine-entry'/)
  assert.match(auditSource, /path:\s*'\/health-monitor\/mine-entry'/)
  assert.match(auditSource, /slug:\s*'trend-warning'/)
  assert.match(auditSource, /path:\s*'\/health-monitor\/trend-warning'/)
})

test('visual audit writes route-level artifact summaries', () => {
  assert.match(auditSource, /summary\.routeSummaries\s*=/)
  assert.match(auditSource, /passedViewports/)
  assert.match(auditSource, /failedViewports/)
  assert.match(auditSource, /issueCount/)
  assert.match(auditSource, /rerunCommand:\s*`node scripts\/with-env\.mjs VISUAL_ROUTES=\$\{routeSlug\} -- npm run audit:visual`/)
  assert.match(auditSource, /path\.basename\(result\.screenshot\)/)
})

test('visual audit markdown includes triage table and rerun hints', () => {
  assert.match(auditSource, /## Route Summary/)
  assert.match(auditSource, /\| route \| status \| passed viewports \| failed viewports \| issues \| rerun \| screenshots \|/)
  assert.match(auditSource, /rerun_all: npm run audit:visual/)
  assert.match(auditSource, /rerun_one_route: node scripts\/with-env\.mjs VISUAL_ROUTES=<route-slug> -- npm run audit:visual/)
})

test('visual audit groups issues by type for faster failure triage', () => {
  assert.match(auditSource, /summary\.issueTypeSummaries\s*=/)
  assert.match(auditSource, /## Issue Type Summary/)
  assert.match(auditSource, /\| issue type \| count \| routes \| viewports \|/)
  assert.match(auditSource, /routes:\s*Array\.from\(item\.routes\)\.sort\(\)/)
  assert.match(auditSource, /viewports:\s*Array\.from\(item\.viewports\)\.sort\(\)/)
})
