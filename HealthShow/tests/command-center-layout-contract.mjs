import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const src = (relativePath) => readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

test('A+C command pages avoid self-amplifying stretch layouts', () => {
  const safetyStyle = src('src/views/safety-command/safety-command.scss')
  const dashboardStyle = src('src/views/health-monitor/dashboard/dashboard.scss')

  assert.match(
    safetyStyle,
    /\.sc-war-grid\s*\{[\s\S]*align-items:\s*start;/,
    'safety command primary grid should not force the stage and queue to equal-height stretch'
  )
  assert.doesNotMatch(
    safetyStyle,
    /\.sc-support-rank,\s*\n\.sc-support-events\s*\{[\s\S]*grid-row:\s*span\s+2;/,
    'support panels should not span auto-sized rows because long lists inflate unrelated cards'
  )
  assert.match(
    safetyStyle,
    /\.sc-support-events\s*\{[\s\S]*height:\s*clamp\(/,
    'event support panel should have a bounded height so its list scrolls internally'
  )
  assert.doesNotMatch(
    safetyStyle,
    /\.sc-war-support\s+:deep\(\.(?:area-panel|rank-panel|ev-panel)\)[\s\S]*height:\s*100%;/,
    'support child roots must not be reset to height:100%; that reintroduces auto-row amplification'
  )
  assert.match(
    safetyStyle,
    /\.sc-war-support\s+\.sc-support-area\s*\{[\s\S]*height:\s*clamp\(/,
    'area distribution panel should have a bounded height instead of expanding with every area tile'
  )

  assert.match(
    dashboardStyle,
    /\.db-control-grid\s*\{[\s\S]*align-items:\s*start;/,
    'dashboard control grid should use content-driven panel heights'
  )
  assert.doesNotMatch(
    dashboardStyle,
    /\.db-governance-workspace\s*\{[\s\S]*grid-template-rows:\s*minmax\(420px,\s*\.92fr\)\s+minmax\(460px,\s*1\.08fr\);/,
    'dashboard governance workspace should not use fractional auto rows that can expand to tens of thousands of pixels'
  )
  assert.match(
    dashboardStyle,
    /\.db-duty-console\s+\.dm-dispatch-shell\s*\{[\s\S]*height:\s*auto;/,
    'dashboard dispatch component should not inherit a 100% height inside an auto-sized control grid'
  )
  assert.match(
    dashboardStyle,
    /\.db-support-band\s+\.dm-main-env\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;/,
    'dashboard environment chart should span the support band when the reused sidebar occupies a full row'
  )
  assert.match(
    dashboardStyle,
    /\.db-control-system\s+\.db-support-sidebar\.dm-command-sidebar\s*\{[\s\S]*display:\s*grid;/,
    'dashboard support sidebar should override the legacy command-sidebar flex stack so rank/action panels do not collapse'
  )
  assert.match(
    dashboardStyle,
    /\.db-health-snapshot\s*\{[\s\S]*align-self:\s*stretch;/,
    'dashboard health snapshot should stretch across the flex rail instead of shrink-wrapping two-column vital cards'
  )
  assert.match(
    dashboardStyle,
    /\.db-health-snapshot\s+\.dm-vitals-supplemental__item:nth-child\(3\):last-child\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;/,
    'dashboard health snapshot should not leave an empty supplemental cell in its narrow two-column rail'
  )
  assert.match(
    dashboardStyle,
    /\.db-health-snapshot\s+\.dm-assess-row:nth-child\(3\):last-child\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;/,
    'dashboard health snapshot assessment summary should not leave an empty cell in its narrow two-column rail'
  )
  assert.match(
    dashboardStyle,
    /@media\s*\(min-width:\s*1500px\)\s*\{[\s\S]*\.db-health-snapshot\s+\.dm-vitals-grid,[\s\S]*\.db-health-snapshot\s+\.dm-vitals-supplemental,[\s\S]*\.db-health-snapshot\s+\.dm-assess-bars\s*\{[\s\S]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\);/,
    'dashboard health snapshot should switch to a 3-column matrix only when the rail is wide enough'
  )
  assert.match(
    dashboardStyle,
    /@media\s*\(max-width:\s*768px\)\s*\{[\s\S]*\.db-health-rail\s*\{[\s\S]*align-self:\s*stretch;[\s\S]*width:\s*100%;/,
    'dashboard mobile health rail should fill the viewport instead of inheriting desktop shrink-wrap alignment'
  )
  assert.match(
    dashboardStyle,
    /\.db-health-rail\s*\{\s*position:\s*sticky;[\s\S]*top:\s*62px;[\s\S]*align-self:\s*stretch;/,
    'dashboard health rail should stretch to the governance row so the device panel absorbs leftover vertical space'
  )
  assert.match(
    dashboardStyle,
    /\.db-device-rail\s*\{[^}]*min-height:\s*214px;[^}]*flex:\s*1\s+1\s+auto;/,
    'dashboard device panel should flex to fill the remaining health rail height instead of leaving page-background whitespace below it'
  )
  assert.match(
    dashboardStyle,
    /\.db-device-rail\s+\.dm-device-body\s*\{[^}]*flex:\s*1\s+1\s+auto;[^}]*justify-content:\s*space-between;/,
    'dashboard device panel body should distribute cards and gauges when the panel grows'
  )
  assert.match(
    dashboardStyle,
    /\.db-control-system\s+\.dm-right-preshift\s+\.dm-preshift-body\s*\{[^}]*flex:\s*1\s+1\s+auto;[^}]*display:\s*grid;[^}]*grid-template-columns:\s*minmax\(128px,\s*\.72fr\)\s+minmax\(0,\s*1fr\);/,
    'dashboard pre-shift panel body should fill the stretched panel and use a two-column matrix instead of leaving blank lower space'
  )
  assert.match(
    dashboardStyle,
    /\.db-control-system\s+\.dm-right-preshift\s+\.dm-ps-ring-wrap\s*\{[^}]*width:\s*100%;[^}]*height:\s*100%;[^}]*min-height:\s*154px;[^}]*display:\s*grid;[^}]*place-items:\s*center;/,
    'dashboard pre-shift ring should center inside the expanded body area'
  )
  assert.match(
    dashboardStyle,
    /\.db-control-system\s+\.dm-right-preshift\s+\.dm-ps-stats\s*\{[^}]*display:\s*grid;[^}]*grid-template-rows:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\);/,
    'dashboard pre-shift stats should fill the panel height as three balanced rows'
  )
  assert.match(
    dashboardStyle,
    /\.db-device-rail\.dm-main-device\s*\{[\s\S]*height:\s*auto;[\s\S]*overflow:\s*visible;/,
    'dashboard device rail should not inherit the shared height:100% clipped device panel shell'
  )
  assert.match(
    dashboardStyle,
    /\.db-device-rail\s+\.dm-device-cards\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);/,
    'dashboard device rail cards should avoid a 3+2 matrix with an empty final slot'
  )
  assert.match(
    dashboardStyle,
    /@media\s*\(max-width:\s*768px\)\s*\{[\s\S]*\.db-device-rail\s+\.dm-device-cards\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/,
    'dashboard mobile device cards should use a deliberate two-column grid instead of a 3+2 layout with an empty slot'
  )
  assert.match(
    dashboardStyle,
    /\.db-device-rail\s+\.dm-dcard:nth-child\(5\):last-child\s*\{[\s\S]*grid-column:\s*1\s*\/\s*-1;/,
    'dashboard mobile device card grid should let the odd final device card span the row'
  )
  assert.match(
    dashboardStyle,
    /\.db-control-system\s+\.db-trend-command\.dm-main-model\s*\{[\s\S]*min-height:\s*clamp\(360px,\s*34vh,\s*430px\);[\s\S]*overflow:\s*visible;/,
    'dashboard trend command panel should be bounded to chart content instead of keeping the oversized model shell'
  )
  assert.match(
    dashboardStyle,
    /@media\s*\(max-width:\s*1100px\)\s*\{[\s\S]*\.db-control-system\s+\.db-trend-command\.dm-main-model\s*\{[\s\S]*height:\s*auto;[\s\S]*min-height:\s*0;/,
    'dashboard medium/mobile trend panel should override the shared 640px model shell height'
  )
  assert.match(
    dashboardStyle,
    /@media\s*\(max-width:\s*1800px\)\s*and\s*\(min-width:\s*1101px\)\s*\{[\s\S]*\.db-intel-band\s*\{[\s\S]*grid-template-columns:\s*1fr;/,
    'dashboard trend and insight band should collapse before the right sidebar creates a large blank area under the trend charts'
  )
})

test('safety command uses one filterable incident queue', () => {
  const safetyPage = src('src/views/safety-command/index.vue')
  const supportGrid = src('src/views/safety-command/components/SafetyCommandSupportGrid.vue')

  assert.match(safetyPage, /class="sc-queue-filters"/)
  assert.match(safetyPage, /展示前50条开放事件，按风险优先/)
  assert.doesNotMatch(safetyPage, /<RiskPersonPanel/)
  assert.doesNotMatch(supportGrid, /EventPanel|RiskPersonPanel/)
})

test('safety command situation stage exposes regional relations and an actionable incident', () => {
  const safetyPage = src('src/views/safety-command/index.vue')
  const safetyStyle = src('src/views/safety-command/safety-command.scss')

  assert.match(
    safetyPage,
    /class="sc-stage-section-head"/,
    'situation stage should label the single authoritative department matrix'
  )
  assert.doesNotMatch(safetyPage, /class="sc-stage-intel"/)
  assert.match(
    safetyPage,
    /class="sc-stage-operational-grid"/,
    'situation stage should render a concrete regional relation matrix'
  )
  assert.match(
    safetyPage,
    /sc-priority-incident/,
    'situation stage should expose the current priority incident with a direct action'
  )
  assert.match(
    safetyPage,
    /责任：\{\{ priorityEvent\.owner \|\| '未分派' \}\}/,
    'priority incident should expose its owner instead of a decorative status only'
  )
  assert.match(
    safetyPage,
    /SLA：\{\{ priorityEvent\.sla \|\| '未配置' \}\}/,
    'priority incident should make missing SLA configuration explicit'
  )
  assert.match(
    safetyStyle,
    /\.sc-stage-operational-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);/,
    'regional relation matrix should have deliberate stable tracks'
  )
})

test('safety command response queue is bounded so support grid does not leave a blank left gutter', () => {
  const safetyStyle = src('src/views/safety-command/safety-command.scss')

  assert.match(
    safetyStyle,
    /\.sc-response-queue\s*\{[\s\S]*height:\s*clamp\(440px,\s*25vw,\s*500px\);[\s\S]*max-height:\s*clamp\(440px,\s*25vw,\s*500px\);/,
    'desktop response queue should be bounded to the stage panel height so the next support row starts without a left-side blank gap'
  )
  assert.match(
    safetyStyle,
    /\.sc-queue-list\s*\{[\s\S]*flex:\s*1\s+1\s+auto;[\s\S]*overflow-y:\s*auto;/,
    'response queue cards should scroll internally instead of increasing the whole grid row height'
  )
  assert.doesNotMatch(safetyStyle, /\.sc-risk-lane\s*\{/)
})
