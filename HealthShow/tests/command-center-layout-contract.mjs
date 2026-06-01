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
})
