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

test('risk warning desktop layout gives the department chart priority over the table height', () => {
  const source = readSource('src/views/health-monitor/risk-warning/risk-warning.scss')

  assert.match(
    source,
    /\.rw-root\s*\{[\s\S]*height:\s*auto;[\s\S]*min-height:\s*calc\(100vh - 50px\);[\s\S]*overflow:\s*visible;/,
    'risk warning root should allow the taller chart-first work area to scroll instead of clipping at one viewport'
  )
  assert.match(
    source,
    /\.rw-bd\s*\{[\s\S]*flex:\s*none;[\s\S]*align-items:\s*flex-start;[\s\S]*overflow:\s*visible;/,
    'risk warning body should size to the work area instead of stretching the table to the full viewport'
  )
  assert.match(
    source,
    /\.rw-aside\s*\{[\s\S]*width:\s*clamp\(320px,\s*20vw,\s*400px\);/,
    'risk warning aside should give the department distribution chart a wider desktop rail'
  )
  assert.match(
    source,
    /\.rw-aside-bot\s*\{[\s\S]*height:\s*clamp\(380px,\s*38vh,\s*460px\);/,
    'department distribution panel should keep a stable chart-first height on desktop'
  )
  assert.match(
    source,
    /\.rw-panel-list\s*\{[\s\S]*height:\s*clamp\(260px,\s*26vh,\s*320px\);[\s\S]*max-height:\s*320px;/,
    'warning list panel should be bounded so it does not show too many rows on tall desktop viewports'
  )
})
