import test from 'node:test'
import assert from 'node:assert/strict'

import { buildCockpitSnapshot } from '../src/data/cockpit-data'

test('cockpit snapshot exposes the flagship desktop modules', () => {
  const snapshot = buildCockpitSnapshot()

  assert.equal(snapshot.title, '统一健康驾驶舱')
  assert.equal(snapshot.capabilityPanels.length, 6)
  assert.ok(snapshot.alertFeed.length >= 5)
  assert.equal(snapshot.alertFeed[0]?.severity, 'critical')
  assert.equal(snapshot.bottomModules.length, 4)
})
