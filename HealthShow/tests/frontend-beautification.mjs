import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const healthShowRoot = path.resolve(__dirname, '..')

function readSource(relativePath) {
  return readFileSync(path.join(healthShowRoot, relativePath), 'utf8')
}

test('target beautification pages do not keep banned emoji UI shortcuts', () => {
  const targetFiles = [
    'src/views/health-monitor/trend-warning/index.vue',
    'src/views/health-monitor/risk-warning/index.vue',
    'src/views/alert-management/config/index.vue',
    'src/views/health-monitor/workbench/index.vue',
    'src/views/health-monitor/employee-profile/index.vue',
    'src/views/health-monitor/employee-profile/employee-profile-view-model.js',
    'src/views/health-monitor/mine-entry/index.vue',
    'src/views/personnel-management/health-portrait/index.vue',
    'src/views/personnel-management/health-portrait/health-portrait-view-model.js',
    'src/views/safety-command/index.vue',
    'src/views/safety-command/components/SafetyCommandDialogs.vue',
    'src/views/safety-command/components/EventPanel.vue',
    'src/views/safety-command/components/KpiCardRow.vue',
    'src/views/safety-command/components/RiskPersonPanel.vue',
    'src/views/safety-command/components/PersonDetailDrawer.vue',
    'src/views/safety-command/safety-command-view-model.js',
    'src/views/safety-command/safety-command-interactions.js',
    'src/views/alert-management/sos/index.vue'
  ]
  const bannedUiTokens = [
    '❤',
    '♥',
    '❤️',
    '🩸',
    '💓',
    '💖',
    '💘',
    '📞',
    '📢',
    '🚨',
    '💨',
    '🫀',
    '📈',
    '👟',
    '⚡',
    '🌡',
    '🌡️',
    '📅',
    '✅',
    '✓',
    '⚠',
    '⚠️',
    '🆘',
    '🔇',
    '😴',
    '⛏️',
    '⌚',
    '🫂'
  ]

  const offenders = targetFiles.filter((relativePath) => {
    const source = readSource(relativePath)
    return bannedUiTokens.some((token) => source.includes(token))
  })
  assert.deepEqual(offenders, [], `Remove banned emoji UI shortcuts from: ${offenders.join(', ')}`)
})

test('shared hero shell is adopted on the remaining beautification targets', () => {
  const pages = [
    'src/views/health-monitor/employee-archive/index.vue',
    'src/views/health-monitor/employee-profile/index.vue',
    'src/views/health-monitor/trend-warning/index.vue',
    'src/views/alert-management/config/index.vue'
  ]

  for (const relativePath of pages) {
    const source = readSource(relativePath)
    assert.match(source, /PageHeroHeader/, `${relativePath} should use PageHeroHeader`)
  }
})

test('admin CRUD pages use the shared hero shell', () => {
  const adminPages = [
    'src/views/device-management/index.vue',
    'src/views/user-list/index.vue',
    'src/views/role-management/index.vue',
    'src/views/org-management/department/index.vue',
    'src/views/org-management/job-type/index.vue'
  ]

  for (const relativePath of adminPages) {
    const source = readSource(relativePath)
    assert.match(source, /PageHeroHeader/, `${relativePath} should use PageHeroHeader`)
    assert.match(source, /hm-admin-page/, `${relativePath} should use the shared hm-admin-page shell class`)
  }
})

test('empty-state-sensitive pages use the shared PageEmptyState component', () => {
  const pages = [
    'src/views/health-monitor/trend-warning/index.vue',
    'src/views/alert-management/config/index.vue',
    'src/views/health-monitor/mine-entry/index.vue',
    'src/views/ai-chat/index.vue',
    'src/views/health-monitor/employee-archive/index.vue'
  ]

  for (const relativePath of pages) {
    const source = readSource(relativePath)
    assert.match(source, /PageEmptyState/, `${relativePath} should use PageEmptyState`)
  }
})

test('dashboard support band keeps device and environment panels on equal-height rails', () => {
  const source = readSource('src/views/health-monitor/dashboard/dashboard.scss')

  assert.match(
    source,
    /\.dm-support-band\s*\{[\s\S]*align-items:\s*stretch;/,
    'dashboard support band should stretch both columns to the same height'
  )
  assert.match(
    source,
    /\.dm-support-band\s*>\s*\*\s*\{[\s\S]*height:\s*100%;[\s\S]*min-height:\s*0;/,
    'dashboard support band children should inherit the stretched height'
  )
  assert.match(
    source,
    /\.dm-main-device\s*\{[\s\S]*height:\s*100%;/,
    'device status panel should fill the stretched rail height'
  )
  assert.match(
    source,
    /\.dm-main-env\s*\{[\s\S]*height:\s*100%;/,
    'environment panel should fill the stretched rail height'
  )
})

test('dashboard warning stream action cluster keeps pending label and handle button on a dedicated inline rail', () => {
  const viewSource = readSource('src/views/health-monitor/dashboard/components/DashboardWarningStream.vue')
  const styleSource = readSource('src/views/health-monitor/dashboard/dashboard.scss')

  assert.match(
    viewSource,
    /class="dm-ev-actions"/,
    'dashboard warning stream should use a dedicated action wrapper for pending state controls'
  )
  assert.match(
    styleSource,
    /\.dm-ev-actions\s*\{[\s\S]*display:\s*inline-flex;[\s\S]*gap:\s*[0-9]+px;/,
    'dashboard warning stream action wrapper should reserve explicit spacing between label and button'
  )
})

test('dashboard overview rail keeps the health snapshot in a single-column layout with readable two-column vital cards', () => {
  const viewSource = readSource('src/views/health-monitor/dashboard/index.vue')
  const styleSource = readSource('src/views/health-monitor/dashboard/dashboard.scss')

  assert.match(viewSource, /class="dm-vital-head"/, 'dashboard vital cards should expose a dedicated header row')
  assert.match(viewSource, /class="dm-vital-reading"/, 'dashboard vital cards should expose a dedicated value row')
  assert.match(viewSource, /class="dm-vital-foot"/, 'dashboard vital cards should expose a dedicated status row')
  assert.match(viewSource, /class="dm-assess-bars"/, 'dashboard overview rail should include the compact health assessment footer')
  assert.doesNotMatch(viewSource, /dm-command-section--dept/, 'dashboard overview rail should not keep the broken hidden dept chart section')

  assert.match(
    styleSource,
    /\.dm-command-overview-body\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-direction:\s*column;/,
    'dashboard overview body should stack its internal sections vertically in the narrow rail'
  )
  assert.match(
    styleSource,
    /\.dm-vitals-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);/,
    'dashboard overview rail should keep vital cards in a readable two-column grid'
  )
  assert.match(
    styleSource,
    /\.dm-vital-card\s*\{[\s\S]*display:\s*flex;[\s\S]*flex-direction:\s*column;/,
    'dashboard vital cards should use a stacked layout instead of the collapsed inline micro-card layout'
  )
})

test('dashboard duty panel uses a workflow shell instead of duplicated stat cards', () => {
  const source = readSource('src/views/health-monitor/dashboard/components/DashboardDispatchPanel.vue')

  assert.match(source, /dm-dispatch-mission/, 'dashboard duty panel should expose the mission banner shell')
  assert.match(source, /class="dm-dispatch-mission-cta"/, 'dashboard duty panel should expose the primary workflow CTA')
  assert.match(source, /class="dm-dispatch-workspace"/, 'dashboard duty panel should expose the two-zone workspace shell')
  assert.match(source, /class="dm-dispatch-queue"/, 'dashboard duty panel should expose the closure queue rail')
  assert.match(source, /class="dm-dispatch-queue-list"/, 'dashboard duty panel should render queue rows inside a dedicated list')
  assert.match(source, /dm-dispatch-queue-item/, 'dashboard duty panel should render action rows instead of metric cards')
  assert.match(source, /class="dm-dispatch-assist"/, 'dashboard duty panel should expose the assist rail')
  assert.match(source, /class="dm-dispatch-assist-card dm-dispatch-assist-card--summary"/, 'dashboard duty panel should keep the AI summary in a dedicated assist card')
  assert.match(source, /class="dm-dispatch-assist-card dm-dispatch-assist-card--focus"/, 'dashboard duty panel should keep the focus people list in a dedicated assist card')
  assert.doesNotMatch(source, /class="dm-dispatch-actions"/, 'dashboard duty panel should retire the old three-card action strip')
  assert.doesNotMatch(source, /class="dm-dispatch-grid"/, 'dashboard duty panel should retire the old duplicated stat grid')
})

test('dashboard medium desktop breakpoint rebalances the three-column command band before the center workflow collapses', () => {
  const source = readSource('src/views/health-monitor/dashboard/dashboard.scss')

  assert.match(source, /@media \(max-width:\s*1500px\) and \(min-width:\s*1101px\)/)
  assert.match(
    source,
    /@media \(max-width:\s*1500px\) and \(min-width:\s*1101px\)[\s\S]*\.dm-command-band\s*\{[\s\S]*grid-template-columns:\s*[0-9]+px minmax\(0,\s*1fr\);/,
    'dashboard medium desktop layout should collapse the right rail under the main workspace before the center column becomes unreadable'
  )
  assert.match(
    source,
    /@media \(max-width:\s*1500px\) and \(min-width:\s*1101px\)[\s\S]*\.dm-command-sidebar\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);/,
    'dashboard medium desktop layout should turn the right rail into a two-card strip below the main columns'
  )
})

test('employee profile desktop shell prioritizes full-page scrolling over fixed one-screen locking', () => {
  const styleSource = readSource('src/views/health-monitor/employee-profile/employee-profile.scss')
  const desktopShellSection = styleSource.split('.ep-hero')[0]

  assert.match(
    desktopShellSection,
    /\.ep-page\s*\{[\s\S]*\n\s*height:\s*auto;/,
    'employee profile desktop shell should allow content height to extend the page'
  )
  assert.match(
    desktopShellSection,
    /\.ep-page\s*\{[\s\S]*\n\s*min-height:\s*calc\(100vh - 50px\);/,
    'employee profile desktop shell should still preserve a full-screen minimum height baseline'
  )
  assert.match(
    styleSource,
    /\.ep-body\s*\{[\s\S]*overflow:\s*visible;[\s\S]*min-height:\s*0;/,
    'employee profile body should not clip content inside the cockpit grid'
  )
  assert.match(
    styleSource,
    /\.ep-left\s*\{[\s\S]*overflow:\s*visible;/,
    'employee profile left rail should allow full content to extend the page'
  )
  assert.match(
    styleSource,
    /\.ep-center\s*\{[\s\S]*overflow:\s*visible;/,
    'employee profile center stage should allow full content to extend the page'
  )
  assert.match(
    styleSource,
    /\.ep-right\s*\{[\s\S]*overflow:\s*visible;/,
    'employee profile right rail should allow full content to extend the page'
  )
})

test('safety command narrow desktop leaderboard drops the status pill before columns collide', () => {
  const source = readSource('src/views/safety-command/components/DeptRankTable.vue')

  assert.match(source, /@media \(max-width: 1500px\) and \(min-width: 769px\)/)
  assert.match(
    source,
    /th:nth-child\(7\),[\s\S]*td:nth-child\(7\)[\s\S]*display:\s*none;/,
    'narrow desktop leaderboard should hide the status column before text overlaps'
  )
})

test('safety command adopts the shared flagship command shell and summary strip', () => {
  const source = readSource('src/views/safety-command/index.vue')

  assert.match(
    source,
    /class="hm-page-shell cc sc-page"|class="cc sc-page hm-page-shell"|class="cc hm-page-shell sc-page"/,
    'safety command should opt into the shared flagship page shell'
  )
  assert.match(source, /PageHeroHeader/, 'safety command should use the shared cockpit hero')
  assert.match(source, /MetricStrip/, 'safety command should expose the shared metric strip')
  assert.doesNotMatch(source, /<KpiCardRow/, 'safety command should not keep a second bespoke KPI strip in the body grid')
})

test('safety command desktop shell avoids fixed one-screen locking after redesign', () => {
  const styleSource = readSource('src/views/safety-command/safety-command.scss')
  const desktopShellSection = styleSource.split('@media (max-width: 768px)')[0]

  assert.match(
    desktopShellSection,
    /\.cc\s*\{[\s\S]*\n\s*height:\s*auto;/,
    'safety command desktop shell should allow content height to extend the page'
  )
  assert.match(
    desktopShellSection,
    /\.cc\s*\{[\s\S]*\n\s*min-height:\s*calc\(100vh - 50px\);/,
    'safety command desktop shell should preserve a full-screen minimum height baseline'
  )
  assert.match(
    desktopShellSection,
    /\.cc\s*\{[\s\S]*\n\s*overflow-y:\s*auto;/,
    'safety command desktop shell should keep vertical page scrolling available'
  )
})

test('dashboard command hero and command band prioritize unified control decisions', () => {
  const viewSource = readSource('src/views/health-monitor/dashboard/index.vue')
  const styleSource = readSource('src/views/health-monitor/dashboard/dashboard.scss')

  assert.match(
    viewSource,
    /title="统一管控"/,
    'dashboard hero should make unified control the primary page identity'
  )
  assert.match(
    styleSource,
    /\.dm-command-band\s*\{[\s\S]*grid-template-columns:\s*minmax\(260px,\s*0\.72fr\)\s+minmax\(560px,\s*1\.45fr\)\s+minmax\(280px,\s*0\.74fr\);/,
    'dashboard command band should reserve the widest desktop rail for the duty decision panel'
  )
})

test('risk warning medium desktop breakpoint preserves dept chart height', () => {
  const source = readSource('src/views/health-monitor/risk-warning/risk-warning.scss')

  assert.match(source, /@media \(max-width: 1600px\) and \(min-width: 769px\)/)
  assert.match(
    source,
    /\.rw-aside-top\s*\{[\s\S]*height:\s*165px;/,
    'risk warning medium desktop breakpoint should shorten the stats panel to free chart space'
  )
  assert.match(
    source,
    /\.rw-aside-bot[\s\S]*\.rw-pc\s*>\s*div\s*\{[\s\S]*min-height:\s*160px;/,
    'risk warning medium desktop breakpoint should enforce a minimum chart body height'
  )
})

test('dashboard loading does not schedule repeated forced scroll-to-top resets', () => {
  const source = readSource('src/views/health-monitor/dashboard/dashboard-lifecycle.js')

  assert.doesNotMatch(source, /const checkpoints\s*=\s*\[/)
  assert.doesNotMatch(source, /window\.setTimeout\(\(\)\s*=>\s*forceDashboardScrollTop/)
  assert.doesNotMatch(source, /vm\._scrollResetTimers/)
  assert.doesNotMatch(source, /vm\.fetchData\(\)\.then\(\(\)\s*=>\s*\{\s*resetDashboardScroll\(vm\)/)
})

test('real-time desktop table keeps a deliberate horizontal scroll strategy instead of clipping action columns', () => {
  const source = readSource('src/views/health-monitor/real-time/realtime.scss')
  const viewSource = readSource('src/views/health-monitor/real-time/components/RealtimeUserTable.vue')

  assert.doesNotMatch(
    viewSource,
    /<el-table-column label="#"/,
    'real-time desktop table should drop the serial number column to free space for the right-side fields'
  )

  assert.match(
    source,
    /\.rt-tbl-wrap\s*\{[\s\S]*overflow-x:\s*auto;/,
    'real-time table wrapper should preserve horizontal scrolling on desktop'
  )
  assert.match(
    source,
    /\.el-table\s*\{[\s\S]*min-width:\s*13[0-9]{2}px;/,
    'real-time table should keep an explicit desktop min-width baseline'
  )
  assert.match(
    source,
    /@media \(max-width:\s*1600px\)\s*and\s*\(min-width:\s*769px\)/,
    'real-time table should define a medium desktop breakpoint for denser table layouts'
  )
  assert.match(
    source,
    /\.rt-tbl-wrap \.el-table\s*\{[\s\S]*min-width:\s*12[0-9]{2}px;/,
    'real-time medium desktop breakpoint should reduce table min-width to keep more columns visible'
  )
})

test('employee profile desktop body must remain vertically scrollable when content exceeds one screen', () => {
  const styleSource = readSource('src/views/health-monitor/employee-profile/employee-profile.scss')
  const desktopShellSection = styleSource.split('@media (max-width: 768px)')[0]

  assert.match(
    desktopShellSection,
    /\.ep-page\s*\{[\s\S]*\n\s*overflow-y:\s*auto;/,
    'employee profile desktop shell should allow vertical page scrolling'
  )
  assert.match(
    desktopShellSection,
    /\.ep-body\s*\{[\s\S]*\n\s*overflow:\s*visible;/,
    'employee profile body should not trap vertical overflow inside a clipped cockpit grid'
  )
})

test('employee profile scroll mode keeps cockpit proportions bounded on desktop', () => {
  const styleSource = readSource('src/views/health-monitor/employee-profile/employee-profile.scss')

  assert.match(
    styleSource,
    /\.ep-body\s*\{[\s\S]*align-items:\s*start;/,
    'employee profile body should stop stretching all three columns to the tallest content'
  )
  assert.match(
    styleSource,
    /\.ep-side-panels\s*\{[\s\S]*justify-content:\s*flex-start;/,
    'employee profile side panels should stack from the top instead of centering through a giant column'
  )
  assert.match(
    styleSource,
    /\.ep-scroll-panel\s*\{[\s\S]*flex:\s*0 0 auto;/,
    'employee profile side scroll panels should use bounded panel heights in page-scroll mode'
  )
  assert.match(
    styleSource,
    /\.ep-miner-stage\s*\{[\s\S]*max-height:\s*[0-9]+px;/,
    'employee profile miner stage should have a desktop max-height guardrail'
  )
})

test('employee profile trend and warning modules use summary rails instead of raw marquee blocks', () => {
  const viewSource = readSource('src/views/health-monitor/employee-profile/index.vue')
  const styleSource = readSource('src/views/health-monitor/employee-profile/employee-profile.scss')
  const viewModelSource = readSource('src/views/health-monitor/employee-profile/use-employee-profile-page.js')
  const leftSection = viewSource.split('<main class="ep-center">')[0]

  assert.match(viewSource, /class="ep-trend-stats"/)
  assert.match(viewSource, /class="ep-warn-summary"/)
  assert.match(viewSource, /class="ep-warn-footer"/)
  assert.match(viewSource, /class="ep-panel ep-warning-band"/)
  assert.doesNotMatch(leftSection, /class="ep-panel ep-warns"/)
  assert.doesNotMatch(viewSource, /warnings\.concat\(warnings\)/)
  assert.match(styleSource, /\.ep-trend-stats\s*\{/)
  assert.match(styleSource, /\.ep-warn-summary\s*\{/)
  assert.match(styleSource, /\.ep-warning-band__body\s*\{/)
  assert.match(styleSource, /\.ep-trend-chart\s*\{[\s\S]*height:\s*1[0-5][0-9]px;/)
  assert.match(styleSource, /\.ep-warning-band__list\s*\{/)
  assert.match(viewModelSource, /recentWarnings\s*=\s*computed\(\(\)\s*=>\s*warnings\.value\.slice\(0,\s*4\)\)/)
})
