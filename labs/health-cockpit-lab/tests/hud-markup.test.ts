import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { buildCockpitSnapshot } from '../src/data/cockpit-data'
import { renderHudShell } from '../src/ui/render-hud'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

test('hud shell renders the core command sections', () => {
  const html = renderHudShell(buildCockpitSnapshot())

  assert.match(html, /AI CORE ONLINE/)
  assert.match(html, /告警信息/)
  assert.match(html, /实时信号/)
  assert.match(html, /统一健康驾驶舱/)
  assert.match(html, /cockpit-bottom-nav/)
})

test('hud shell includes the richer flagship cockpit structures', () => {
  const html = renderHudShell(buildCockpitSnapshot())

  assert.match(html, /cockpit-top-icons/)
  assert.match(html, /holo-human-shell/)
  assert.match(html, /module-radar/)
  assert.match(html, /module-donut/)
})

test('hud shell includes reference-like support modules and stage details', () => {
  const html = renderHudShell(buildCockpitSnapshot())

  assert.match(html, /ship-status-panel/)
  assert.match(html, /alert-feed__footer/)
  assert.match(html, /signal-feed__footer/)
  assert.match(html, /holo-human-shell__pedestal/)
  assert.match(html, /holo-human-shell__node-grid/)
})

test('desktop fidelity overlay exists for animated high-fidelity mode', () => {
  const html = renderHudShell(buildCockpitSnapshot())

  assert.match(html, /desktop-fidelity-overlay/)
  assert.match(html, /desktop-fidelity-overlay__scan/)
  assert.match(html, /desktop-fidelity-overlay__pulse--core/)
  assert.match(html, /desktop-fidelity-overlay__shimmer--alerts/)
})

test('desktop fidelity overlay provides real interaction hotspots', () => {
  const html = renderHudShell(buildCockpitSnapshot())

  assert.match(html, /desktop-fidelity-controls/)
  assert.match(html, /desktop-fidelity-command/)
  assert.match(html, /desktop-fidelity-nav/)
})

test('desktop cockpit does not depend on the reference image asset', () => {
  const styles = readFileSync(path.join(__dirname, '../src/styles.css'), 'utf8')

  assert.doesNotMatch(styles, /reference-bg\.png/)
})
