import { getRiskWarningOverview, getRiskWarningList, getRiskWarningTrend, getDeptWarningStats } from '@/api/risk-warning'
import { getRealtimeOverview, getRealtimeStatistics } from '@/api/realtime'
import { getDeviceActivation, getBodyIndicators } from '@/api/health'
import { getEmployeeStats } from '@/api/employee'
import { getHourlyHeartRate } from '@/api/heart-rate'
import { getHourlyBloodOxygen } from '@/api/blood-oxygen'
import { getDeptAiReport, generateDeptAiReport } from '@/api/ai'
import {
  buildAreasFromDepartments,
  buildDepartmentListFromWarningStats,
  buildWatchStatus,
  normalizeWarningTrendData
} from './safety-command-view-model'

export async function loadSafetyCommandDeptAi(deptName) {
  if (!deptName) return null
  try {
    const res = await getDeptAiReport(deptName)
    if (res.code === 200 && res.data) return res.data
  } catch {}
  return null
}

export async function generateSafetyCommandDeptAi(deptName, force = false) {
  if (!deptName) return { ok: false, message: '缺少部门名称' }
  try {
    const res = await generateDeptAiReport(deptName, force)
    if (res.code === 200 && res.data) return { ok: true, data: res.data }
    return { ok: false, message: res.message || '生成失败' }
  } catch {
    return { ok: false, message: 'AI 服务暂时不可用' }
  }
}

export async function fetchSafetyCommandCritical({ processWarningData, handledCountRef }) {
  try {
    const [overviewResult, listResult] = await Promise.allSettled([
      getRiskWarningOverview(),
      getRiskWarningList({ handled: false, page: 1, size: 50 })
    ])

    if (listResult.status === 'fulfilled' && listResult.value?.data) {
      processWarningData(listResult.value.data)
    }

    if (overviewResult.status === 'fulfilled' && overviewResult.value?.data) {
      const data = overviewResult.value.data
      handledCountRef.value = data.handledWarnings || data.handledCount || data.handled || handledCountRef.value
    }
  } catch {}
}

export async function fetchSafetyCommandHourlyVitals(vitalsHistory) {
  const today = getLocalToday()
  const [hrResult, oxygenResult] = await Promise.allSettled([
    getHourlyHeartRate(today, today),
    getHourlyBloodOxygen(today, today)
  ])

  if (hrResult.status === 'fulfilled' && Array.isArray(hrResult.value?.data) && hrResult.value.data.length >= 2) {
    vitalsHistory.hr = hrResult.value.data
  }
  if (oxygenResult.status === 'fulfilled' && Array.isArray(oxygenResult.value?.data) && oxygenResult.value.data.length >= 2) {
    vitalsHistory.bo = oxygenResult.value.data
  }
}

export async function fetchSafetyCommandData({
  statsRef,
  watchStatusRef,
  handledCountRef,
  vitalAvgRef,
  warningTrendRef,
  departmentsRef,
  areasRef,
  processWarningData
}) {
  try {
    const results = await Promise.allSettled([
      getRiskWarningOverview(),
      getRiskWarningList({ handled: false, page: 1, size: 50 }),
      getDeptWarningStats(),
      getRealtimeStatistics(),
      getRiskWarningTrend(7),
      getDeviceActivation(),
      getRealtimeOverview(),
      getEmployeeStats(),
      getBodyIndicators()
    ])

    if (results[1].status === 'fulfilled' && results[1].value?.data) {
      processWarningData(results[1].value.data)
    }

    if (results[0].status === 'fulfilled' && results[0].value?.data) {
      const data = results[0].value.data
      statsRef.value = {
        ...statsRef.value,
        underground: data.totalOnline || data.underground || statsRef.value.underground,
        total: data.totalUsers || data.total || statsRef.value.total
      }
      handledCountRef.value = data.handledWarnings || data.handledCount || data.handled || 0
    }

    if (results[2].status === 'fulfilled' && results[2].value?.data) {
      departmentsRef.value = buildDepartmentListFromWarningStats(results[2].value.data)
    }

    if (results[3].status === 'fulfilled' && results[3].value?.data) {
      watchStatusRef.value = buildWatchStatus(results[3].value.data)
    }

    if (results[4].status === 'fulfilled' && results[4].value?.data) {
      warningTrendRef.value = normalizeWarningTrendData(results[4].value.data)
    }

    if (results[5].status === 'fulfilled' && results[5].value?.data) {
      const deviceData = results[5].value.data
      if (deviceData.stats && !watchStatusRef.value.total) {
        watchStatusRef.value.total = deviceData.stats.totalDevices || 0
        watchStatusRef.value.online = deviceData.stats.activeDevices || 0
        watchStatusRef.value.offline = Math.max(0, watchStatusRef.value.total - watchStatusRef.value.online)
      }
    }

    if (results[6].status === 'fulfilled' && results[6].value?.data) {
      const overview = results[6].value.data
      const undergroundCount = overview.onlineCount || overview.underground || 0
      if (undergroundCount > 0) statsRef.value.underground = undergroundCount
    }

    if (results[7].status === 'fulfilled' && results[7].value?.data) {
      const employeeStats = results[7].value.data
      const totalCount = employeeStats.total || employeeStats.totalCount || 0
      if (totalCount > 0) statsRef.value.total = totalCount
    }

    if (results[8].status === 'fulfilled' && results[8].value?.data) {
      const avg = results[8].value.data
      vitalAvgRef.value = {
        heartRate: avg.avgHeartRate || 0,
        bloodOxygen: avg.avgBloodOxygen || 0,
        temperature: avg.avgTemperature || 0,
        pressure: avg.avgPressure || 0
      }
    }

    areasRef.value = buildAreasFromDepartments(departmentsRef.value)
  } catch {}
}

function getLocalToday() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
