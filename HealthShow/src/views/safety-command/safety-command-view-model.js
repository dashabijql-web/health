const DONUT_CIRCUMFERENCE = 339.3
const WARNING_TYPE_KEYS = ['心率', '血氧', '体温', '压力']
const WARNING_TYPE_COLORS = ['#ff3b3b', '#ff8c00', '#00e676', '#00c8ff']
const EVENT_LEVEL_TEXT_MAP = { critical: '特急', high: '紧急', medium: '一般', low: '轻微' }
const DEPT_LEVEL_TEXT_MAP = { H: '高危', M: '中危', L: '低', N: '正常' }

export function getEventLevelText(level) {
  return EVENT_LEVEL_TEXT_MAP[level] || level
}

export function buildKpiDetailText(key, stats, watchStatus) {
  const statData = stats || {}
  const watchData = watchStatus || {}
  return {
    underground: `井下人员 ${statData.underground || 0} 人，合计 ${statData.total || 0} 人。`,
    sos: `当前 SOS 求救 ${statData.sos || 0} 条。`,
    fall: `当前跌倒事件 ${statData.fall || 0} 条。`,
    alerts: `静止 ${statData.static || 0} 条，异常 ${statData.abnormal || 0} 条。`,
    watch: `在线 ${watchData.online || 0}/${watchData.total || 0}，离线 ${watchData.offline || 0}，低电量 ${watchData.lowBattery || 0}。`
  }[key] || '暂无详情'
}

export function buildRenderedDeptAiReport(content) {
  if (!content) return ''
  return content
    .replace(/^## (.+)$/gm, '<h4>$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

export function mapWarningToEvent(record) {
  const warningType = record.warningType || record.warning_type || record.type || ''
  let eventType = 'abnormal'
  let level = 'medium'
  let icon = '⚠️'

  if (warningType.includes('SOS') || warningType.includes('sos')) {
    eventType = 'sos'
    level = 'critical'
    icon = '🆘'
  } else if (warningType.includes('跌倒') || warningType.includes('fall')) {
    eventType = 'fall'
    level = 'high'
    icon = '🚨'
  } else if (warningType.includes('静止') || warningType.includes('static')) {
    eventType = 'static'
    level = 'medium'
    icon = '🔇'
  } else if (warningType.includes('心率')) {
    level = 'high'
    icon = '💓'
  } else if (warningType.includes('血氧')) {
    level = 'high'
    icon = '🩸'
  } else if (warningType.includes('体温')) {
    level = 'medium'
    icon = '🌡️'
  } else if (warningType.includes('疲劳') || warningType.includes('睡眠')) {
    level = 'low'
    icon = '😴'
  }

  const createdAt = record.createTime || record.create_time || record.time || ''
  let durationMinutes = 0
  if (createdAt) {
    const createdDate = new Date(createdAt)
    if (!isNaN(createdDate)) {
      durationMinutes = Math.round((Date.now() - createdDate.getTime()) / 60000)
    }
  }

  return {
    id: record.id,
    icon,
    type: warningType,
    user: record.userName || record.user_name || '',
    userCode: record.userCode || record.user_code || '',
    dept: record.deptName || record.dept_name || '',
    location: record.location || record.deptName || record.dept_name || '',
    time: formatEventAge(createdAt),
    level,
    eventType,
    durationMinutes
  }
}

export function buildProcessedWarningData(raw) {
  const records = Array.isArray(raw) ? raw : (raw?.records || raw?.list || [])
  const events = records.map(mapWarningToEvent)
  return {
    events,
    ...buildEventRiskSummary(events)
  }
}

export function buildEventRiskSummary(events) {
  const list = Array.isArray(events) ? events : []
  const personMap = new Map()

  list.forEach((event) => {
    if (!event.user) return
    if (!personMap.has(event.user)) {
      personMap.set(event.user, {
        id: event.id,
        name: event.user,
        userCode: event.userCode,
        dept: event.dept,
        count: 0
      })
    }
    personMap.get(event.user).count++
  })

  return {
    riskPersons: Array.from(personMap.values()).sort((a, b) => b.count - a.count),
    sosCount: list.filter((event) => event.eventType === 'sos').length,
    fallCount: list.filter((event) => event.eventType === 'fall').length
  }
}

export function buildDeptRankData(departments) {
  return [...(departments || [])]
    .map((dept) => {
      const warnings = (dept.sos || 0) + (dept.fall || 0) + (dept.abnormal || 0)
      const level = (dept.sos > 0 || dept.fall > 0) ? 'H' : warnings > 2 ? 'M' : warnings > 0 ? 'L' : 'N'
      return {
        ...dept,
        warnings,
        level,
        statusText: DEPT_LEVEL_TEXT_MAP[level] || '正常'
      }
    })
    .sort((a, b) => b.warnings - a.warnings)
}

export function buildTop5RiskPersons(riskPersons) {
  return (riskPersons || [])
    .slice(0, 5)
    .map((person) => ({
      ...person,
      tags: person.tags || [person.dept || '未分组', `预警×${person.count}`]
    }))
}

export function buildDonutSegments(events) {
  const list = events || []
  const counts = WARNING_TYPE_KEYS.map((key) => list.filter((event) => (event.type || '').includes(key)).length)
  const total = counts.reduce((sum, count) => sum + count, 0) || 1
  let offset = 0

  return WARNING_TYPE_KEYS.map((name, index) => {
    const pct = counts[index] / total
    const dash = +(pct * DONUT_CIRCUMFERENCE).toFixed(1)
    const rem = +(DONUT_CIRCUMFERENCE - dash).toFixed(1)
    const segment = {
      name,
      cnt: counts[index],
      pct: Math.round(pct * 100),
      color: WARNING_TYPE_COLORS[index],
      dash,
      rem,
      offset: +(-offset).toFixed(1)
    }
    offset += dash
    return segment
  })
}

export function buildTypeHandleProgress(events, handledCount) {
  const list = events || []
  const handled = handledCount || 0
  return WARNING_TYPE_KEYS.map((key, index) => {
    const pending = list.filter((event) => (event.type || '').includes(key)).length
    const handledShare = Math.floor(handled / 4) + (index < handled % 4 ? 1 : 0)
    const total = pending + handledShare
    const rate = total > 0 ? Math.round(handledShare / total * 100) : 0
    return {
      key,
      rate,
      color: rate >= 90 ? '#00e676' : rate >= 60 ? '#ffd600' : '#ff8c00'
    }
  })
}

export function buildVitalsRows(vitalAvg, vitalsHistory) {
  const avg = vitalAvg || {}
  const history = vitalsHistory || {}
  const hr = avg.heartRate || 0
  const bloodOxygen = avg.bloodOxygen || 0
  const temperature = avg.temperature || 0
  const pressure = avg.pressure || 0

  const hrVals = (history.hr || []).length >= 2
    ? history.hr.map((item) => item.avgHeartRate || 0).filter((value) => value > 0)
    : [hr * 0.96, hr * 0.98, hr * 0.99, hr, hr * 1.01, hr]

  const oxygenVals = (history.bo || []).length >= 2
    ? history.bo.map((item) => item.avgBloodOxygen || 0).filter((value) => value > 0)
    : [bloodOxygen - 0.5, bloodOxygen - 0.2, bloodOxygen, bloodOxygen + 0.1, bloodOxygen, bloodOxygen - 0.1]

  const temperatureVals = [temperature * 0.999, temperature * 1.0, temperature * 1.001, temperature, temperature * 0.999, temperature]
  const pressureVals = [pressure * 0.95, pressure * 0.98, pressure, pressure * 1.02, pressure, pressure * 0.97]

  const hrPath = buildSparkPath(hrVals, 50, 120)
  const oxygenPath = buildSparkPath(oxygenVals, 90, 100)
  const temperaturePath = buildSparkPath(temperatureVals, 36, 39)
  const pressurePath = buildSparkPath(pressureVals, 0, 100)

  return [
    { key: 'hr', label: '心率', val: hr > 0 ? Math.round(hr) : '--', unit: 'bpm', color: '#ff3b3b', gradId: 'vg-hr', ...hrPath },
    { key: 'bo', label: '血氧', val: bloodOxygen > 0 ? Math.round(bloodOxygen) : '--', unit: '%', color: '#ff8c00', gradId: 'vg-bo', ...oxygenPath },
    { key: 'temp', label: '体温', val: temperature > 0 ? temperature.toFixed(1) : '--', unit: '°C', color: '#00e676', gradId: 'vg-temp', ...temperaturePath },
    { key: 'pres', label: '压力', val: pressure > 0 ? Math.round(pressure) : '--', unit: 'idx', color: '#a855f7', gradId: 'vg-pres', ...pressurePath }
  ]
}

export function buildTrendPath(warningTrend) {
  const list = warningTrend || []
  if (!list.length) return null
  const counts = list.map((item) => item.count || item.cnt || 0)
  const maxVal = Math.max(...counts, 1)
  const width = 380
  const height = 175
  const padTop = 15
  const padBottom = 20
  const pts = counts.map((count, index) => ({
    x: Math.round(index / Math.max(counts.length - 1, 1) * (width - 4) + 2),
    y: Math.round(height - padBottom - (count / maxVal) * (height - padTop - padBottom))
  }))
  const line = `M${pts.map((point) => `${point.x},${point.y}`).join(' L ')}`
  return { line, area: `${line} L${width},${height} L0,${height} Z`, pts }
}

export function buildTrend7dayTotal(warningTrend) {
  return (warningTrend || []).reduce((sum, item) => sum + (item.count || item.cnt || 0), 0)
}

export function buildTrendChange(warningTrend) {
  const list = warningTrend || []
  if (list.length < 2) return 0
  const today = list[list.length - 1]?.count || list[list.length - 1]?.cnt || 0
  const yesterday = list[list.length - 2]?.count || list[list.length - 2]?.cnt || 0
  return yesterday ? Math.round((today - yesterday) / yesterday * 100) : 0
}

export function buildAreasFromDepartments(departments) {
  return (departments || []).map((dept, index) => ({
    id: dept.id || index + 1,
    name: dept.name,
    count: dept.abnormal || 0,
    sos: dept.sos || 0,
    fall: dept.fall || 0,
    warning: dept.abnormal || 0,
    level: dept.status || 'safe'
  }))
}

export function buildDepartmentListFromWarningStats(deptData) {
  if (!Array.isArray(deptData) || deptData.length === 0) return []
  return deptData.map((dept, index) => {
    const totalWarn = dept.total || (dept.heartRate || 0) + (dept.bloodOxygen || 0) + (dept.sleep || 0) + (dept.temperature || 0) + (dept.pressure || 0)
    return {
      id: dept.id || index + 1,
      name: dept.deptName || dept.dept_name || dept.name || `部门${index + 1}`,
      online: 0,
      sos: 0,
      fall: 0,
      static: 0,
      abnormal: totalWarn,
      status: totalWarn >= 10 ? 'danger' : totalWarn >= 3 ? 'warning' : 'safe'
    }
  })
}

export function buildWatchStatus(realtimeStats) {
  const data = realtimeStats || {}
  const online = data.onlineUsers || data.onlineDevices || data.online || 0
  const total = data.totalUsers || data.totalDevices || data.total || 0
  return {
    total,
    online,
    offline: Math.max(0, total - online),
    lowBattery: data.lowBattery || 0
  }
}

export function normalizeWarningTrendData(rawTrend) {
  if (Array.isArray(rawTrend)) return rawTrend
  if (rawTrend?.dates && Array.isArray(rawTrend.dates)) {
    const seriesArrays = Object.values(rawTrend.series || {}).filter(Array.isArray)
    return rawTrend.dates.map((date, index) => ({
      date,
      count: seriesArrays.reduce((sum, series) => sum + (series[index] || 0), 0)
    }))
  }
  return []
}

function buildSparkPath(values, minV, maxV) {
  if (!values || values.length < 2) return null
  const width = 260
  const height = 44
  const padTop = 6
  const padBottom = 8
  const range = maxV - minV || 1
  const points = values.map((value, index) => ({
    x: Math.round(index / Math.max(values.length - 1, 1) * width),
    y: Math.round(padTop + (1 - Math.max(0, Math.min(1, (value - minV) / range))) * (height - padTop - padBottom))
  }))
  const linePath = points.reduce((acc, point, index) => {
    if (index === 0) return `M${point.x},${point.y}`
    const prev = points[index - 1]
    const cx = (prev.x + point.x) / 2
    return `${acc} C${cx},${prev.y} ${cx},${point.y} ${point.x},${point.y}`
  }, '')
  return {
    linePath,
    areaPath: `${linePath} L${width},${height} L0,${height} Z`,
    endX: points[points.length - 1].x,
    endY: points[points.length - 1].y
  }
}

function formatEventAge(value) {
  if (!value) return ''
  const date = new Date(value)
  if (isNaN(date)) return value
  const diffMinutes = Math.round((Date.now() - date.getTime()) / 60000)
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  return `${Math.floor(diffMinutes / 60)}小时前`
}
