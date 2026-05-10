import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import { buildRealtimeDetailItems } from '../src/views/health-monitor/real-time/realtime-helpers.js'

const detailDialogSource = readFileSync('src/views/health-monitor/real-time/components/RealtimeDetailDialog.vue', 'utf8')
const tableSource = readFileSync('src/views/health-monitor/real-time/components/RealtimeUserTable.vue', 'utf8')

test('real-time detail items include watch identity and latest update for operator context', () => {
  const items = buildRealtimeDetailItems({
    userName: '张三',
    userCode: 'EMP001',
    deptName: '综采一队',
    imei: '359456780000001',
    heartRate: 88,
    bloodOxygen: 97,
    temperature: 36.5,
    steps: 1234,
    bloodPressureHigh: 122,
    bloodPressureLow: 78,
    pressure: 45,
    lastUpdate: '2026-05-11T03:30:12+08:00'
  })

  const byLabel = Object.fromEntries(items.map((item) => [item.label, item.value]))
  assert.equal(byLabel['手表IMEI'], '359456780000001')
  assert.equal(byLabel['最近上报'], '03:30:12')
  assert.equal(byLabel['工号'], 'EMP001')
})

test('real-time detail dialog exposes close footer and keeps mobile card keyboard entry', () => {
  assert.match(detailDialogSource, /#footer/, 'detail dialog should expose an explicit footer action')
  assert.match(detailDialogSource, /关闭/, 'detail dialog should have a visible close button')
  assert.match(tableSource, /@keydown\.enter\.prevent="\$emit\('showUserDetail', row\)"/, 'mobile cards keep Enter detail access')
  assert.match(tableSource, /@keydown\.space\.prevent="\$emit\('showUserDetail', row\)"/, 'mobile cards keep Space detail access')
})
