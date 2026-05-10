function parseDateTime(value) {
  if (!value) return null
  if (value instanceof Date) return value
  if (typeof value === 'number') return new Date(value)
  const raw = String(value).trim()
  if (!raw) return null
  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T')
  const direct = new Date(normalized)
  if (!Number.isNaN(direct.getTime())) return direct
  const fallback = new Date(raw.replace(/-/g, '/'))
  return Number.isNaN(fallback.getTime()) ? null : fallback
}

export function normalizeWarningLevel(value) {
  const text = String(value ?? '').trim().toLowerCase()
  if (!text) return 'info'
  if (['3', 'danger', 'high', 'highrisk', '高', '高危', '危险', '危急', '严重'].includes(text)) return 'danger'
  if (['2', 'warn', 'warning', 'mid', 'medium', '中', '中危', '预警', '警告'].includes(text)) return 'warn'
  if (['1', 'info', 'tip', 'low', '低', '低危', '提示', '注意'].includes(text)) return 'info'
  return 'info'
}

export function levelLabel(value) {
  return {
    danger: '危险',
    warn: '预警',
    info: '提示'
  }[normalizeWarningLevel(value)]
}

export function isWarningHandled(item = {}) {
  const value = item.handled ?? item.isHandled
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value === 1
  const text = String(value ?? '').trim().toLowerCase()
  return ['1', 'true', 'yes', 'y', '已处理', 'handled'].includes(text)
}

export function warningHandledStatusLabel(item = {}) {
  return isWarningHandled(item) ? '已处理' : '待处理'
}

export function levelClass(value) {
  return {
    danger: 'badge-danger',
    warn: 'badge-warn',
    info: 'badge-info'
  }[normalizeWarningLevel(value)]
}

export function markWarningHandled(item = {}) {
  item.handled = true
  item.isHandled = true
  item.slaStatus = 'handled'
  item.slaStatusText = '已处理'
  item.slaClockLabel = '完成'
  item.slaClockText = '--'
  item.remainingSeconds = 0
  item.overdueSeconds = 0
  return item
}

export function buildWarningLifecycleItem(item = {}, options = {}) {
  const handled = isWarningHandled(item)
  const lifecycle = buildWarningLifecycleView({ ...item, handled }, options)
  return {
    ...item,
    handled,
    warningLevelLabel: levelLabel(item.warningLevel),
    warningLevelClass: levelClass(item.warningLevel),
    ...lifecycle
  }
}

export function buildWarningLifecycleView(item = {}, options = {}) {
  const slaMinutes = Number(options.slaMinutes ?? item.slaMinutes ?? 30) || 30
  const warningWindowMinutes = Number(options.warningWindowMinutes ?? 10) || 10
  const nowMs = Number(options.now ?? Date.now())
  const createDate = parseDateTime(item.createTime || item.warningTime || item.recordTime || item.time)
  if (!createDate) {
    return {
      slaMinutes,
      warningWindowMinutes,
      slaDeadline: '--',
      remainingSeconds: 0,
      overdueSeconds: 0,
      slaStatus: 'unknown',
      slaStatusText: '未知',
      slaClockLabel: '未知',
      slaClockText: '--'
    }
  }

  const createMs = createDate.getTime()
  const deadlineMs = createMs + slaMinutes * 60 * 1000
  const remainingMs = deadlineMs - nowMs
  const overdueMs = Math.max(0, nowMs - deadlineMs)
  const remainingSeconds = Math.max(0, Math.ceil(remainingMs / 1000))
  const overdueSeconds = Math.max(0, Math.ceil(overdueMs / 1000))
  const isHandled = isWarningHandled(item)
  const slaStatus = isHandled ? 'handled' : (overdueMs > 0 ? 'overdue' : (remainingMs <= warningWindowMinutes * 60 * 1000 ? 'warning' : 'ok'))
  const slaStatusText = slaStatus === 'handled' ? '已处理' : slaStatus === 'overdue' ? '已超时' : slaStatus === 'warning' ? '即将超时' : '正常'
  const slaClock = buildSlaClockView(slaStatus, remainingSeconds, overdueSeconds)

  return {
    slaMinutes,
    warningWindowMinutes,
    slaDeadline: formatDateTime(deadlineMs),
    remainingSeconds: isHandled ? 0 : remainingSeconds,
    overdueSeconds: isHandled ? 0 : overdueSeconds,
    slaStatus,
    slaStatusText,
    ...slaClock
  }
}

function buildSlaClockView(status, remainingSeconds, overdueSeconds) {
  if (status === 'handled') return { slaClockLabel: '完成', slaClockText: '--' }
  if (status === 'overdue') return { slaClockLabel: '超时', slaClockText: formatDurationText(overdueSeconds) }
  return { slaClockLabel: '剩余', slaClockText: formatDurationText(remainingSeconds) }
}

function formatDurationText(seconds) {
  const totalSeconds = Math.max(0, Math.ceil(Number(seconds) || 0))
  if (totalSeconds <= 0) return '0分钟'
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.ceil((totalSeconds % 3600) / 60)
  if (hours > 0) return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  return `${Math.max(1, Math.ceil(totalSeconds / 60))}分钟`
}

export function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
