import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const ROOT = process.cwd()
const auditPath = path.join(ROOT, 'tests', 'visual', 'page-layout-audit.mjs')
const auditSource = fs.readFileSync(auditPath, 'utf8')

test('visual audit writes route-level artifact summaries', () => {
  assert.match(auditSource, /summary\.routeSummaries\s*=/)
  assert.match(auditSource, /passedViewports/)
  assert.match(auditSource, /failedViewports/)
  assert.match(auditSource, /issueCount/)
  assert.match(auditSource, /path\.basename\(result\.screenshot\)/)
})

test('visual audit markdown includes triage table and rerun hints', () => {
  assert.match(auditSource, /## Route Summary/)
  assert.match(auditSource, /\| route \| status \| passed viewports \| failed viewports \| issues \| screenshots \|/)
  assert.match(auditSource, /rerun_all: npm run audit:visual/)
  assert.match(auditSource, /rerun_one_route: VISUAL_ROUTES=<route-slug> npm run audit:visual/)
})
