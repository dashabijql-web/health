import dayjs from 'dayjs'

function hasValue(value) {
  return value !== null && value !== undefined && value !== ''
}

function colorFromStatusClass(statusClass, okColor) {
  if (statusClass.includes('danger')) return '#ff5252'
  if (statusClass.includes('warn')) return '#ffd200'
  return okColor
}

export function classifyHeartRate(value) {
  if (!value) return 'c-dim'
  if (value < 50 || value > 120) return 'c-danger'
  if (value < 60 || value > 100) return 'c-warn'
  return 'c-ok'
}

export function classifyBloodOxygen(value) {
  if (!value) return 'c-dim'
  if (value < 90) return 'c-danger'
  if (value < 95) return 'c-warn'
  return 'c-ok'
}

export function classifyTemperature(value) {
  if (!value) return 'c-dim'
  if (value < 35 || value > 38) return 'c-danger'
  if (value < 36 || value > 37.5) return 'c-warn'
  return 'c-ok'
}

export function classifySystolic(value) {
  if (!value) return 'c-dim'
  if (value >= 160) return 'c-danger'
  if (value >= 140 || value < 90) return 'c-warn'
  return 'c-bp'
}

export function classifyDiastolic(value) {
  if (!value) return 'c-dim'
  if (value >= 100) return 'c-danger'
  if (value >= 90 || value < 60) return 'c-warn'
  return 'c-bp'
}

export function classifyPressure(value) {
  if (value === null || value === undefined) return 'c-dim'
  if (value >= 85) return 'c-danger'
  if (value >= 70) return 'c-warn'
  return 'c-pressure'
}

export function getRealtimeIndicator(user) {
  if (user.heartRate && (user.heartRate < 50 || user.heartRate > 120)) return `心率 ${user.heartRate}`
  if (user.bloodOxygen && user.bloodOxygen < 90) return `血氧 ${user.bloodOxygen}%`
  if (user.temperature && (user.temperature < 35 || user.temperature > 38)) return `体温 ${user.temperature}°`
  if (user.bloodPressureHigh && user.bloodPressureHigh >= 160) return `血压 ${user.bloodPressureHigh}`
  if (user.pressure && user.pressure >= 85) return `压力 ${user.pressure}`
  return '体征异常'
}

export function getRealtimeRowClass(row) {
  if (row.status !== 'warning') return ''
  const isDanger = (row.heartRate && (row.heartRate < 45 || row.heartRate > 130))
    || (row.bloodOxygen && row.bloodOxygen < 88)
    || (row.temperature && (row.temperature < 34.5 || row.temperature > 39))
    || (row.bloodPressureHigh && row.bloodPressureHigh >= 180)
    || (row.pressure && row.pressure >= 90)
  return isDanger ? 'row-danger' : 'row-warning'
}

export function formatRealtimeTime(value) {
  if (!value) return '--'
  return dayjs(value).format('HH:mm:ss')
}

export function buildRealtimeDetailItems(user) {
  if (!user) return []

  return [
    {
      label: '心率',
      value: hasValue(user.heartRate) ? `${user.heartRate} bpm` : '--',
      color: colorFromStatusClass(classifyHeartRate(user.heartRate), '#52c41a')
    },
    {
      label: '血氧',
      value: hasValue(user.bloodOxygen) ? `${user.bloodOxygen}%` : '--',
      color: colorFromStatusClass(classifyBloodOxygen(user.bloodOxygen), '#52c41a')
    },
    {
      label: '体温',
      value: hasValue(user.temperature) ? `${user.temperature}°C` : '--',
      color: colorFromStatusClass(classifyTemperature(user.temperature), '#52c41a')
    },
    {
      label: '步数',
      value: hasValue(user.steps) ? `${user.steps} 步` : '--',
      color: '#22c55e'
    },
    {
      label: '收缩压',
      value: hasValue(user.bloodPressureHigh) ? `${user.bloodPressureHigh} mmHg` : '--',
      color: colorFromStatusClass(classifySystolic(user.bloodPressureHigh), '#a78bfa')
    },
    {
      label: '舒张压',
      value: hasValue(user.bloodPressureLow) ? `${user.bloodPressureLow} mmHg` : '--',
      color: colorFromStatusClass(classifyDiastolic(user.bloodPressureLow), '#a78bfa')
    },
    {
      label: '压力指数',
      value: hasValue(user.pressure) ? user.pressure : '--',
      color: colorFromStatusClass(classifyPressure(user.pressure), '#fb923c')
    },
    { label: '部门', value: user.deptName || '--', color: '#a8c5e6' },
    { label: '工号', value: user.userCode || '--', color: '#a8c5e6' }
  ]
}

export function normalizeRealtimeUsersResponse(data) {
  const list = data?.list ? data.list : (Array.isArray(data) ? data : [])
  const total = data?.total ?? list.length
  return { list, total }
}

export function resolveRealtimeFetchSize(width) {
  return width < 992 ? 200 : 1000
}

export function sortWarningUsers(users) {
  return [...users]
    .filter((user) => user.status === 'warning')
    .sort((a, b) => new Date(b.lastUpdate) - new Date(a.lastUpdate))
}

export function filterRealtimeUsers(users, searchForm, hrFilter) {
  let list = [...users]

  if (searchForm.name) {
    const query = searchForm.name.trim().toLowerCase()
    list = list.filter((user) =>
      (user.userName || '').toLowerCase().includes(query)
      || (user.userCode || '').toLowerCase().includes(query)
    )
  }

  if (searchForm.dept) {
    list = list.filter((user) => user.deptName === searchForm.dept)
  }

  if (searchForm.status) {
    list = list.filter((user) => user.status === searchForm.status)
  }

  if (hrFilter) {
    list = list.filter((user) => user.heartRate >= hrFilter.min && user.heartRate <= hrFilter.max)
  }

  return list.sort((a, b) => {
    if (a.status === 'warning' && b.status !== 'warning') return -1
    if (a.status !== 'warning' && b.status === 'warning') return 1
    return 0
  })
}
