import test from 'node:test'
import assert from 'node:assert/strict'

import { getLatestDangerEvent } from '../src/views/health-monitor/dashboard/dashboard-summary.js'

test('getLatestDangerEvent returns only unhandled danger events', () => {
  const infoOnly = [
    { id: 1, level: 'info', handled: false },
    { id: 2, level: 'warn', handled: false }
  ]
  assert.equal(getLatestDangerEvent(infoOnly), null)

  const handledDanger = [
    { id: 3, level: 'danger', handled: true },
    { id: 4, level: 'warn', handled: false }
  ]
  assert.equal(getLatestDangerEvent(handledDanger), null)

  const danger = { id: 5, level: 'danger', handled: false }
  assert.equal(getLatestDangerEvent([{ id: 6, level: 'info', handled: false }, danger]), danger)
})
