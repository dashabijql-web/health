import dayjs from 'dayjs'
import {
  getDashboardOverview,
  getBodyIndicators,
  getDataTop5,
  getDeviceActivation,
  getDeptHealthCounts,
  getDeptPersonStats,
  getPersonCounts,
  getPreShiftCompliance
} from '@/api/health'
import { getCommandCenterDashboardSummary, getCommandCenterIncidents } from '@/api/command-center'
import { getRealtimeHealthSnapshot, getRealtimeStatistics } from '@/api/realtime'
import { dashboardCache as _cache } from './dashboard-cache'
import { normalizeWarningLevel, isWarningHandled } from '../../alert-management/common/warning-lifecycle'

function postPerfLog(message) {
  fetch('/perf-log', { method: 'POST', body: message }).catch(() => {})
}

function countWarningRows(response) {
  if (!response || response.code !== 200 || !response.data) return 0
  if (Array.isArray(response.data)) return response.data.length
  return response.data.total ?? response.data.totalElements ?? 0
}

function mapDashboardTop5Rows(rawRows) {
  return rawRows.map((item) => ({
    userName: item.userName || item.real_name || item.empName || item.userCode || '--',
    empCode: item.userCode || item.empCode || item.userId || '',
    count: item.count || 0
  }))
}

function mapDashboardWarningEvent(event) {
  const incidentSeverity = String(event.severity || '').toUpperCase()
  return {
    id: event.warningId ?? event.id,
    incidentId: event.incidentId || '',
    occurredAt: event.occurredAt || event.createTime || event.recordTime || event.warningTime || event.time,
    type: event.typeLabel || event.warningType || event.type || event.indicatorName || '--',
    indicator: event.vitalSnapshot?.indicator || event.indicatorName || event.indicator || event.warningType || '--',
    value: event.vitalSnapshot?.value || event.warningValue || event.value || event.actualValue || '--',
    time: event.occurredAt || event.createTime || event.recordTime || event.warningTime || event.time,
    userName: event.person?.name || event.empName || event.userName || event.name || '--',
    userCode: event.person?.userCode || event.empCode || event.userCode || event.code,
    deptName: event.person?.department || event.deptName || event.department || '--',
    location: event.location?.status === 'UNAVAILABLE'
      ? '未接入定位'
      : (event.location?.label || event.location || event.deptName || '--'),
    owner: event.owner?.name || (event.owner?.status === 'UNASSIGNED' ? '未分派' : '--'),
    slaText: event.sla?.configured
      ? (event.sla.deadlineAt || '--') + ' · ' + (event.sla.message || '--')
      : 'SLA 未配置',
    level: incidentSeverity === 'CRITICAL'
      ? 'danger'
      : incidentSeverity === 'HIGH'
        ? 'warn'
        : normalizeWarningLevel(event.warningLevel ?? event.level),
    handled: event.status === 'RESOLVED' || isWarningHandled(event) || event.status === 1
  }
}

function mapDashboardDeptRows(rows) {
  return rows
    .map((item) => ({
      name: item.name || item.deptName || '',
      count: item.count || item.dataCount || 0,
      prevCount: item.prevCount || item.previousCount || 0
    }))
    .filter((item) => item.name && item.name !== '')
}

export function formatDashboardRefreshText(lastRefreshTime) {
  if (!lastRefreshTime) return '加载中...'
  const secs = Math.round((Date.now() - lastRefreshTime) / 1000)
  if (secs < 5) return '刚刚更新'
  if (secs < 60) return `${secs}秒前`
  return `${Math.floor(secs / 60)}分钟前`
}

export function buildDashboardAbnormalUserCount(warningEvents) {
  const abnormalUsers = new Set(
    (warningEvents || [])
      .filter((event) => !event.handled)
      .map((event) => event.userCode)
      .filter(Boolean)
  )
  return abnormalUsers.size
}

export function collectDashboardNewDangerEvents(warningEvents, seenAlertIds) {
  return (warningEvents || []).filter((event) => {
    if (event.level !== 'danger' || event.handled || !event.id || seenAlertIds.has(event.id)) {
      return false
    }
    seenAlertIds.add(event.id)
    return true
  })
}

export function buildDashboardUnhandledStats(warningEvents) {
  const unhandled = (warningEvents || []).filter((event) => !event.handled)
  return {
    kpiUnhandledHigh: unhandled.filter((event) => event.level === 'danger').length,
    kpiUnhandledMid: unhandled.filter((event) => event.level === 'warn').length
  }
}

export async function fetchDashboardKpiSnapshot(warningEvents) {
  const today = dayjs().format('YYYY-MM-DD')
  const now = Date.now()
  const kpiStale = (now - _cache.kpiFetchedAt) > 25000 || _cache.kpiCacheDate !== today

  try {
    if (kpiStale) {
      const statsRes = await getRealtimeStatistics()
      if (statsRes.code === 200 && statsRes.data) {
        const data = statsRes.data
        _cache.kpiOnline = data.onlineCount ?? data.onlineUsers ?? data.onlineDevices ?? 0
        _cache.kpiTotal = data.totalCount ?? data.totalUsers ?? data.totalEmployees ?? data.totalDevices ?? 0
      }

      const yesterday = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
      const [todayRes, yesRes] = await Promise.all([
        getCommandCenterIncidents({ scope: 'range', status: 'ALL', startAt: today, endAt: today, page: 1, size: 1 }),
        getCommandCenterIncidents({ scope: 'range', status: 'ALL', startAt: yesterday, endAt: yesterday, page: 1, size: 1 })
      ])

      _cache.kpiTodayWarnings = countWarningRows(todayRes)
      _cache.kpiYesterdayWarnings = countWarningRows(yesRes)
      _cache.kpiFetchedAt = now
      _cache.kpiCacheDate = today
    }
  } catch {}

  return {
    kpiRealtimeOnline: _cache.kpiOnline || 0,
    kpiRealtimeTotal: _cache.kpiTotal || 0,
    kpiTodayWarnings: _cache.kpiTodayWarnings || 0,
    kpiYesterdayWarnings: _cache.kpiYesterdayWarnings || 0,
    ...buildDashboardUnhandledStats(warningEvents)
  }
}

export async function fetchCommandCenterDashboardSummary(period = 'day') {
  try {
    const response = await getCommandCenterDashboardSummary({ period })
    return response.code === 200 && response.data ? response.data : null
  } catch {
    return null
  }
}

export async function fetchDashboardTop5Data(periodRange) {
  try {
    const res = await getDataTop5(periodRange)
    if (res.code !== 200 || !res.data) return []
    const raw = Array.isArray(res.data) ? res.data : (res.data.list || res.data.records || [])
    return mapDashboardTop5Rows(raw)
  } catch {
    return []
  }
}

export async function fetchDashboardOverviewData(periodRange) {
  try {
    const res = await getDashboardOverview(periodRange)
    return res.code === 200 ? (res.data || {}) : {}
  } catch {
    return {}
  }
}

export async function fetchDashboardPreShiftData(force = false) {
  const now = Date.now()
  if (!force && _cache.preShiftFetchedAt && (now - _cache.preShiftFetchedAt) < 5 * 60 * 1000) {
    return _cache.preShiftData || null
  }

  try {
    const res = await getPreShiftCompliance()
    if (res.code === 200 && res.data) {
      _cache.preShiftFetchedAt = Date.now()
      _cache.preShiftData = res.data
      return res.data
    }
  } catch {}

  return null
}

export async function fetchDashboardPersonCountData(periodRange) {
  try {
    const res = await getPersonCounts(periodRange)
    return res.code === 200 ? (res.data || {}) : {}
  } catch {
    return {}
  }
}

export async function fetchDashboardBodyIndicatorData(periodRange) {
  try {
    const res = await getBodyIndicators(periodRange)
    return res.code === 200 ? (res.data || {}) : {}
  } catch {
    return {}
  }
}

export async function fetchDashboardHealthSnapshot() {
  try {
    const res = await getRealtimeHealthSnapshot()
    return res.code === 200 && res.data ? res.data : null
  } catch {
    return null
  }
}

export async function fetchDashboardDeviceState(periodRange) {
  try {
    const res = await getDeviceActivation(periodRange)
    if (res.code === 200) {
      const data = res.data || {}
      return {
        deviceStats: data.stats || { total: 0, activeRate: 0, usageRate: 0, warningRate: 0 },
        warningRates: data.warningRates || []
      }
    }
  } catch {}

  return {
    deviceStats: { total: 0, activeRate: 0, usageRate: 0, warningRate: 0 },
    warningRates: []
  }
}

export async function fetchDashboardWarningEventState(periodRange) {
  try {
    const res = await getCommandCenterIncidents({
      scope: 'range',
      status: 'ALL',
      startAt: periodRange.startTime,
      endAt: periodRange.endTime,
      page: 1,
      size: 200
    })
    if (res.code === 200 && Array.isArray(res.data?.items)) {
      return res.data.items.map(mapDashboardWarningEvent)
    }
  } catch {}
  return []
}

export async function fetchDashboardDeptState(periodRange, force = false) {
  const now = Date.now()
  const periodKey = JSON.stringify(periodRange)
  if (!force && _cache.deptDataFetchedAt && (now - _cache.deptDataFetchedAt) < 2 * 60 * 1000 && _cache.deptDataPeriodKey === periodKey) {
    return {
      deptDataList: _cache.deptDataList || [],
      deptPersonStatsList: _cache.deptPersonStatsList || []
    }
  }

  const start = performance.now()
  const [countsResult, statsResult] = await Promise.allSettled([
    getDeptHealthCounts(periodRange),
    getDeptPersonStats(periodRange)
  ])
  postPerfLog(`[dashboard] loadDeptData(parallel): ${(performance.now() - start).toFixed(0)}ms`)

  const counts = countsResult.status === 'fulfilled' ? countsResult.value : null
  const stats = statsResult.status === 'fulfilled' ? statsResult.value : null
  const deptDataList = counts?.code === 200 && Array.isArray(counts.data) ? mapDashboardDeptRows(counts.data) : []
  const deptPersonStatsList = stats?.code === 200 && Array.isArray(stats.data) ? stats.data : []

  _cache.deptDataFetchedAt = Date.now()
  _cache.deptDataPeriodKey = periodKey
  _cache.deptDataList = deptDataList
  _cache.deptPersonStatsList = deptPersonStatsList

  return { deptDataList, deptPersonStatsList }
}

export async function fetchDashboardDeptPersonStats(periodRange) {
  const start = performance.now()
  try {
    const res = await getDeptPersonStats(periodRange)
    postPerfLog(`[dashboard] getDeptPersonStats: ${(performance.now() - start).toFixed(0)}ms`)
    return res.code === 200 && Array.isArray(res.data) ? res.data : []
  } catch {
    return []
  }
}
