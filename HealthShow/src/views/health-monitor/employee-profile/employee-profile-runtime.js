import { buildEmpInfoFromRoute } from './employee-profile-view-model'

export function syncEmpInfoFromRoute(route) {
  return buildEmpInfoFromRoute(route)
}

export function resetEmployeeProfileState(state) {
  state.vitals.value = {}
  state.exercise.value = { todaySteps: 0, todayCalories: 0 }
  state.warnings.value = []
  state.isOnline.value = false
  state.lastUpdate.value = '--'
  state.trend7.value = { avgHr: 0, avgSpo2: 0, avgTemp: 0 }
  state.portraitTrend.value = null
  state.aiReportVisible.value = false
  state.aiReportLoading.value = false
  state.aiReportContent.value = ''
  state.aiReportHtml.value = ''
}

export function applyEmployeePortrait(payload, state) {
  const data = payload || {}
  state.vitals.value = data.vitals || {}
  state.exercise.value = data.exercise || { todaySteps: 0, todayCalories: 0 }
  state.isOnline.value = true
  state.lastUpdate.value = new Date().toLocaleString('zh-CN')

  const nextTrend = { avgHr: 0, avgSpo2: 0, avgTemp: 0 }
  if (data.trend) {
    state.portraitTrend.value = data.trend
    if (data.trend.heartRates?.length) {
      const valid = data.trend.heartRates.filter(Boolean)
      if (valid.length) nextTrend.avgHr = Math.round(valid.reduce((a, b) => a + b, 0) / valid.length)
    }
    if (data.trend.bloodOxygens?.length) {
      const valid = data.trend.bloodOxygens.filter(Boolean)
      if (valid.length) nextTrend.avgSpo2 = +(valid.reduce((a, b) => a + b, 0) / valid.length).toFixed(1)
    }
  }
  state.trend7.value = nextTrend
}

export function normalizeEmployeeWarnings(payload) {
  return Array.isArray(payload) ? payload : (payload?.records || payload?.list || [])
}

export function buildEmployeeTrendOption(trend) {
  const labels = trend?.dates?.map((d) => {
    const p = d.split('-')
    return `${+p[1]}/${+p[2]}`
  }) || []

  return {
    backgroundColor: 'transparent',
    grid: { left: 40, right: 50, top: 12, bottom: 28 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } },
      axisLabel: { fontSize: 10, color: 'rgba(255,255,255,0.45)' }
    },
    yAxis: [
      {
        type: 'value',
        min: 50,
        max: 120,
        axisLine: { lineStyle: { color: '#ff5252' } },
        axisLabel: { fontSize: 9, color: '#ff5252' },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
      },
      {
        type: 'value',
        min: 85,
        max: 100,
        axisLine: { lineStyle: { color: '#1890ff' } },
        axisLabel: { fontSize: 9, color: '#1890ff' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '心率',
        type: 'line',
        yAxisIndex: 0,
        data: trend?.heartRates || [],
        smooth: true,
        connectNulls: false,
        lineStyle: { color: '#ff5252', width: 2 },
        itemStyle: { color: '#ff5252' },
        areaStyle: { color: 'rgba(255,82,82,0.1)' },
        symbol: 'circle',
        symbolSize: 4
      },
      {
        name: '血氧',
        type: 'line',
        yAxisIndex: 1,
        data: trend?.bloodOxygens || [],
        smooth: true,
        connectNulls: false,
        lineStyle: { color: '#1890ff', width: 2 },
        itemStyle: { color: '#1890ff' },
        areaStyle: { color: 'rgba(24,144,255,0.1)' },
        symbol: 'circle',
        symbolSize: 4
      }
    ],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10,22,40,0.95)',
      borderColor: 'rgba(0,180,255,0.3)',
      textStyle: { color: '#fff', fontSize: 11 }
    },
    legend: {
      top: 0,
      right: 0,
      textStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 }
    }
  }
}

export function renderEmployeeTrendChart({ el, chart, trend, echartsLib }) {
  if (!el) return chart
  if (chart) chart.dispose()
  const nextChart = echartsLib.init(el)
  nextChart.setOption(buildEmployeeTrendOption(trend))
  return nextChart
}

export function disposeEmployeeChart(chart) {
  if (chart) chart.dispose()
  return null
}

export async function loadEmployeeAiReport(empCode, generateEmployeeReport, renderMarkdown) {
  const empty = { content: '', html: '' }
  if (!empCode) return empty

  try {
    const res = await generateEmployeeReport(empCode)
    if (res.code !== 200) return empty
    const content = res.data?.report || ''
    const html = content ? await renderMarkdown(content) : ''
    return { content, html }
  } catch {
    return empty
  }
}

export function buildEmployeeProfilePrintHtml(empName, reportHtml) {
  return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
<title>AI健康报告 - ${empName}</title>
<style>body{font-family:'Microsoft YaHei',sans-serif;max-width:800px;margin:0 auto;padding:24px;color:#1a1a2e}
h2{color:#0066cc;border-bottom:1px solid #dde;padding-bottom:6px;margin-top:24px}
table{border-collapse:collapse;width:100%;margin:12px 0}
th{background:#e8f0ff;padding:8px 12px;text-align:left}td{padding:7px 12px;border-bottom:1px solid #eee}
blockquote{border-left:4px solid #0066cc;margin:12px 0;padding:8px 16px;background:#f5f8ff}</style>
</head><body>${reportHtml}</body></html>`
}

export function buildMineEntryRoute(empInfo) {
  return {
    path: '/health-monitor/mine-entry',
    query: {
      empCode: empInfo.empCode || '',
      empName: empInfo.empName || ''
    }
  }
}

export function buildWorkbenchRoute(empInfo) {
  return {
    path: '/health-monitor/workbench',
    query: {
      empCode: empInfo.empCode || '',
      empName: empInfo.empName || ''
    }
  }
}

export function buildReportCenterRoute(empInfo) {
  return {
    path: '/health-monitor/report-center',
    query: {
      empCode: empInfo.empCode || '',
      empName: empInfo.empName || '',
      deptName: empInfo.deptName || ''
    }
  }
}
