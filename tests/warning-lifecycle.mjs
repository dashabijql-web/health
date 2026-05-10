import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import {
  buildWarningLifecycleItem,
  buildWarningLifecycleView,
  formatDateTime,
  markWarningHandled,
  normalizeWarningLevel,
  warningHandledStatusLabel
} from '../src/views/alert-management/common/warning-lifecycle.js'

test('normalizeWarningLevel maps Chinese labels and numeric levels', () => {
  assert.equal(normalizeWarningLevel('高危'), 'danger')
  assert.equal(normalizeWarningLevel('危险'), 'danger')
  assert.equal(normalizeWarningLevel('中危'), 'warn')
  assert.equal(normalizeWarningLevel('预警'), 'warn')
  assert.equal(normalizeWarningLevel('提示'), 'info')
  assert.equal(normalizeWarningLevel(3), 'danger')
  assert.equal(normalizeWarningLevel(2), 'warn')
  assert.equal(normalizeWarningLevel(1), 'info')
})

test('warningHandledStatusLabel keeps notifications and records pending wording aligned', () => {
  assert.equal(warningHandledStatusLabel({ handled: false }), '待处理')
  assert.equal(warningHandledStatusLabel({ isHandled: 0 }), '待处理')
  assert.equal(warningHandledStatusLabel({ handled: true }), '已处理')
  assert.equal(warningHandledStatusLabel({ isHandled: 1 }), '已处理')
})

test('alert management pages use shared pending handled wording', () => {
  const files = [
    'src/views/alert-management/notifications/index.vue',
    'src/views/alert-management/records/index.vue'
  ]

  for (const file of files) {
    const source = readFileSync(file, 'utf8')
    assert.doesNotMatch(source, /label="未处理"|['`]未处理['`]/, file)
  }
})

test('buildWarningLifecycleView derives deadline and overdue seconds from createTime', () => {
  const createTime = '2026-05-08T10:00:00+08:00'
  const now = Date.parse('2026-05-08T10:35:00+08:00')
  const view = buildWarningLifecycleView({ createTime }, { now, slaMinutes: 30, warningWindowMinutes: 10 })

  assert.equal(view.slaMinutes, 30)
  assert.equal(view.slaStatus, 'overdue')
  assert.equal(view.overdueSeconds, 300)
  assert.equal(view.remainingSeconds, 0)
  assert.equal(view.slaDeadline, formatDateTime(Date.parse(createTime) + 30 * 60 * 1000))
})

test('buildWarningLifecycleView marks handled warnings as handled instead of overdue', () => {
  const createTime = '2026-05-08T10:00:00+08:00'
  const now = Date.parse('2026-05-08T11:00:00+08:00')
  const view = buildWarningLifecycleView({ createTime, handled: true }, { now, slaMinutes: 30, warningWindowMinutes: 10 })

  assert.equal(view.slaStatus, 'handled')
  assert.equal(view.slaStatusText, '已处理')
  assert.equal(view.remainingSeconds, 0)
  assert.equal(view.overdueSeconds, 0)
})

test('buildWarningLifecycleView marks near-deadline warnings as warning', () => {
  const createTime = '2026-05-08T10:00:00Z'
  const now = Date.parse('2026-05-08T10:22:00Z')
  const view = buildWarningLifecycleView({ createTime }, { now, slaMinutes: 30, warningWindowMinutes: 10 })

  assert.equal(view.slaStatus, 'warning')
  assert.equal(view.remainingSeconds, 480)
  assert.equal(view.overdueSeconds, 0)
})

test('buildWarningLifecycleView exposes operator-facing SLA clock text', () => {
  const createTime = '2026-05-08T10:00:00Z'

  const nearDeadline = buildWarningLifecycleView(
    { createTime },
    { now: Date.parse('2026-05-08T10:22:00Z'), slaMinutes: 30, warningWindowMinutes: 10 }
  )
  assert.equal(nearDeadline.slaClockLabel, '剩余')
  assert.equal(nearDeadline.slaClockText, '8分钟')

  const overdue = buildWarningLifecycleView(
    { createTime },
    { now: Date.parse('2026-05-08T10:35:00Z'), slaMinutes: 30, warningWindowMinutes: 10 }
  )
  assert.equal(overdue.slaClockLabel, '超时')
  assert.equal(overdue.slaClockText, '5分钟')
})

test('buildWarningLifecycleItem normalizes isHandled and level display fields', () => {
  const createTime = '2026-05-08T10:00:00+08:00'
  const now = Date.parse('2026-05-08T11:00:00+08:00')
  const item = buildWarningLifecycleItem({ warningLevel: '高危', createTime, isHandled: 1 }, { now, slaMinutes: 30 })

  assert.equal(item.handled, true)
  assert.equal(item.warningLevelClass, 'badge-danger')
  assert.equal(item.warningLevelLabel, '危险')
  assert.equal(item.slaStatus, 'handled')
  assert.equal(item.slaStatusText, '已处理')
})

test('markWarningHandled refreshes stale overdue SLA fields after local handling', () => {
  const item = {
    id: 1,
    handled: false,
    isHandled: 0,
    slaStatus: 'overdue',
    slaStatusText: '已超时',
    overdueSeconds: 600,
    remainingSeconds: 0
  }

  const result = markWarningHandled(item)

  assert.equal(result, item)
  assert.equal(item.handled, true)
  assert.equal(item.isHandled, true)
  assert.equal(item.slaStatus, 'handled')
  assert.equal(item.slaStatusText, '已处理')
  assert.equal(item.overdueSeconds, 0)
  assert.equal(item.remainingSeconds, 0)
})
