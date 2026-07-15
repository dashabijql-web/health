import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { resolveWarningTotals } from '../src/views/safety-command/safety-command-view-model.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = (relativePath) => readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

test('safety command uses server totals instead of the visible page size', () => {
  assert.deepEqual(
    resolveWarningTotals({ totalWarnings: 125, pendingWarnings: 91, handledWarnings: 34, dangerCount: 12 }, 50),
    { total: 125, pending: 91, handled: 34, critical: 12 }
  )
})

test('safety command requests the current day and carries authoritative totals', () => {
  const runtime = source('src/views/safety-command/safety-command-runtime.js')
  const page = source('src/views/safety-command/use-safety-command-page-data.js')
  const view = source('src/views/safety-command/index.vue')

  assert.match(runtime, /getRiskWarningOverview\(today, today\)/)
  assert.match(runtime, /getCommandCenterIncidents\(\{ scope: 'today', status: 'OPEN'/)
  assert.match(runtime, /getCommandCenterDashboardSummary\(\)/)
  assert.match(runtime, /data\.items \|\| \[\]/)
  assert.match(runtime, /pendingWarningsRef\.value = totals\.pending/)
  assert.match(runtime, /totalWarningsRef\.value = totals\.total/)
  assert.match(page, /const pendingWarnings = ref\(null\)/)
  assert.match(view, /summaryWarning\.value\.criticalPending/)
  assert.match(view, /summaryWarning\.value\.unassignedTotal/)
  assert.match(view, /summaryWarning\.value\.overdueTotal/)
})

test('safety command removes fabricated and duplicate information from the active page', () => {
  const view = source('src/views/safety-command/index.vue')
  const support = source('src/views/safety-command/components/SafetyCommandSupportGrid.vue')
  const workflow = source('src/views/safety-command/safety-command-workflow.js')
  const areaGrid = source('src/views/safety-command/components/AreaMapGrid.vue')

  assert.doesNotMatch(view, /buildDonutSegments|buildTypeHandleProgress|buildVitalsRows|buildTop5RiskPersons/)
  assert.doesNotMatch(view, /其他预警|平均体温|手表状态|井下人数/)
  assert.doesNotMatch(support, /预警类型分布|体征均值走势|部门预警分布|EventPanel|typeHandleProgress/)
  assert.match(support, /今日处置闭环/)
  assert.match(support, /近7日预警趋势/)
  assert.doesNotMatch(workflow, /slice\(0,\s*5\)/)
  assert.match(workflow, /event\.occurredAt/)
  assert.match(areaGrid, /预警 <b>\{\{ a\.count \}\}<\/b>/)
  assert.doesNotMatch(areaGrid, /人数: \$\{a\.count\}/)
})

test('legacy safety command data is isolated from the active page fetch path', () => {
  const pageData = source('src/views/safety-command/use-safety-command-page-data.js')
  const legacyPage = source('src/views/safety-command/legacy-20260601/index.vue')

  assert.match(pageData, /options\.legacy === true/)
  assert.match(pageData, /\.\.\.\(legacy \? \{/)
  assert.match(legacyPage, /useSafetyCommandPageData\(\{ legacy: true \}\)/)
})

test('emergency actions do not claim success without an integrated device', () => {
  const interactions = source('src/views/safety-command/safety-command-interactions.js')
  const dialogs = source('src/views/safety-command/components/SafetyCommandDialogs.vue')

  assert.match(interactions, /广播未发送/)
  assert.match(interactions, /撤离未下发/)
  assert.doesNotMatch(interactions, /紧急广播已发送/)
  assert.doesNotMatch(interactions, /撤离指令已下达/)
  assert.match(dialogs, /confirmBroadcast/)
})

test('both command page headers reserve a separate mobile KPI row', () => {
  const safetyStyle = source('src/views/safety-command/safety-command.scss')
  const dashboardStyle = source('src/views/health-monitor/dashboard/dashboard.scss')

  for (const style of [safetyStyle, dashboardStyle]) {
    assert.match(style, /@media\s*\(max-width:\s*768px\)[\s\S]*grid-template-areas:[\s\S]*'title controls'[\s\S]*'kpis kpis'/)
    assert.match(style, /grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  }
})
