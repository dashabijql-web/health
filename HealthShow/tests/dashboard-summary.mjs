import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

import {
  buildWarningTypeData,
  getLatestDangerEvent
} from '../src/views/health-monitor/dashboard/dashboard-summary.js'
import { buildDashboardMetricCards } from '../src/views/health-monitor/dashboard/dashboard-view-model.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const src = (relativePath) => readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

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

test('buildWarningTypeData falls back to warning event types', () => {
  const result = buildWarningTypeData({
    warningTypesData: [],
    warningEvents: [
      { type: '心率异常' },
      { type: '心率异常' },
      { type: '血氧偏低' }
    ]
  })

  assert.deepEqual(result.map((item) => [item.name, item.value, item.pct]), [
    ['心率异常', 2, 67],
    ['血氧偏低', 1, 33]
  ])
})

test('buildDashboardMetricCards fills the sixth detection slot with all-metric coverage', () => {
  const viewSource = src('src/views/health-monitor/dashboard/index.vue')
  const methodSource = src('src/views/health-monitor/dashboard/dashboard-chart-methods.js')
  const cards = buildDashboardMetricCards({
    metricList: [
      { key: 'heartRate', label: '心率', color: '#00d4ff' },
      { key: 'bloodOxygen', label: '血氧', color: '#67C23A' },
      { key: 'steps', label: '步数', color: '#F56C6C' },
      { key: 'temperature', label: '体温', color: '#00c8c8' },
      { key: 'pressure', label: '压力', color: '#3eb7ff' }
    ],
    personCounts: {
      totalPersons: 100,
      heartRate: 100,
      bloodOxygen: 90,
      steps: 80,
      temperature: 70,
      pressure: 60
    },
    checkData: {},
    kpiRealtimeTotal: 100
  })

  assert.equal(cards.length, 6, 'dashboard detection grid should not leave the sixth two-column slot empty')
  assert.deepEqual(
    {
      key: cards[5].key,
      label: cards[5].label,
      val: cards[5].val,
      unit: cards[5].unit,
      pct: cards[5].pct,
      metricDetail: cards[5].metricDetail
    },
    {
      key: 'allCoverage',
      label: '全项覆盖',
      val: 60,
      unit: '%',
      pct: 60,
      metricDetail: false
    }
  )
  assert.match(viewSource, /m\.unit/, 'dashboard metric cards should render units for derived summary cards')
  assert.match(
    methodSource,
    /m\.metricDetail\s*===\s*false[\s\S]*openDeptPersonModal\(\)/,
    'dashboard derived metric summary cards should open department details instead of invalid metric detail requests'
  )
})
