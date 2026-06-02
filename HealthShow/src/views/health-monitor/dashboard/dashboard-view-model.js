export function buildDashboardHealthPassRate(warningRates) {
  const rates = warningRates || []
  if (!rates.length) return null
  const avgWarn = rates.reduce((sum, item) => sum + (item.rate || 0), 0) / rates.length
  return Math.max(0, Math.min(100, Math.round(100 - avgWarn)))
}

export function buildDashboardHeaderKpis({
  kpiRealtimeOnline,
  kpiRealtimeTotal,
  kpiTodayWarnings,
  kpiYesterdayWarnings,
  kpiUnhandledHigh,
  kpiUnhandledMid,
  warningEvents,
  personCounts,
  healthPassRate,
  preShiftData,
  periodLabel
}) {
  const delta = kpiYesterdayWarnings
    ? Math.round((kpiTodayWarnings - kpiYesterdayWarnings) / kpiYesterdayWarnings * 100)
    : null

  const deltaText = delta !== null
    ? `${delta > 0 ? '↑' : '↓'}${Math.abs(delta)}% 较昨${kpiYesterdayWarnings}件`
    : `昨日 ${kpiYesterdayWarnings}件`

  const abnormalUsers = new Set(
    (warningEvents || [])
      .map((item) => item.userCode || item.empCode || item.id)
      .filter(Boolean)
  ).size

  const monitored = Number(personCounts?.heartRate || 0)
  const total = kpiRealtimeTotal || 0
  const coverageRate = total > 0 ? Math.round(monitored / total * 100) : null

  return [
    {
      label: '今日监测覆盖率',
      val: coverageRate !== null ? `${coverageRate}%` : '--',
      cls: coverageRate !== null && coverageRate < 80 ? 'kpi-orange' : 'kpi-teal',
      clickable: false,
      sub: total > 0 ? `已监测 ${monitored} / ${total} 人` : '数据加载中...'
    },
    {
      label: '今日新增预警',
      val: kpiTodayWarnings,
      cls: 'kpi-red',
      clickable: true,
      route: '/health-monitor/risk-warning',
      sub: deltaText,
      subCls: delta !== null && delta > 0 ? 'sub-up' : 'sub-down'
    },
    {
      label: '未处理预警',
      val: `${kpiUnhandledHigh}高危 ${kpiUnhandledMid}中危`,
      cls: 'kpi-red',
      clickable: true,
      route: '/health-monitor/risk-warning'
    },
    {
      label: '异常人员数',
      val: abnormalUsers,
      cls: 'kpi-orange',
      clickable: true,
      route: '/health-monitor/risk-warning',
      sub: `${periodLabel}累计`
    },
    {
      label: '健康达标率',
      val: healthPassRate !== null ? `${healthPassRate}%` : '--',
      cls: 'kpi-teal',
      clickable: false
    },
    {
      label: '班前达标率',
      val: preShiftData.preShiftRate !== null ? `${preShiftData.preShiftRate}%` : '--',
      cls: preShiftData.preShiftRate !== null && preShiftData.preShiftRate < 80 ? 'kpi-orange' : 'kpi-teal',
      clickable: true,
      sub: preShiftData.totalToday > 0
        ? `达标 ${preShiftData.qualifiedCount} / ${preShiftData.totalToday} 人`
        : '今日暂无数据',
      route: '/health-monitor/mine-entry'
    }
  ]
}

export function buildDashboardMetricCards({ metricList, personCounts, checkData, kpiRealtimeTotal }) {
  const metrics = metricList || []
  const personValues = metrics.map((item) => Number(personCounts[item.key] || 0))
  const maxPersonValue = Math.max(...personValues, 0)
  const total = Number(personCounts.totalPersons || kpiRealtimeTotal || maxPersonValue || 1)
  const maxRec = Math.max(...metrics.map((item) => Number(checkData[item.key] || 0)), 1)

  const cards = metrics.map((item) => {
    const personValue = Number(personCounts[item.key] || 0)
    const recordValue = Number(checkData[item.key] || 0)
    return {
      key: item.key,
      label: item.label,
      color: item.color,
      val: personValue,
      rate: Math.round(personValue / total * 100),
      records: recordValue,
      recPct: Math.round(recordValue / maxRec * 100),
      pct: Math.round(personValue / total * 100),
      metricDetail: true
    }
  })

  const coveredPersons = personValues.length ? Math.min(...personValues) : 0
  const coverageRate = Math.max(0, Math.min(100, Math.round(coveredPersons / total * 100)))
  const coverageColor = coverageRate >= 80 ? '#38ef7d' : coverageRate >= 60 ? '#ff8c00' : '#ff5252'

  cards.push({
    key: 'allCoverage',
    label: '全项覆盖',
    color: coverageColor,
    val: coverageRate,
    unit: '%',
    rate: coverageRate,
    records: coveredPersons,
    recPct: coverageRate,
    pct: coverageRate,
    metricDetail: false,
    summary: true
  })

  return cards
}

export function buildDashboardVitalCards({ bodyIndicators, warningRates = [] }) {
  const b = bodyIndicators || {}
  return [
    {
      label: '心率均值', val: b.avgHeartRate || '--', unit: 'bpm', color: '#00d4ff', icon: 'Monitor',
      route: '/health-monitor/heart-rate',
      tag: !b.avgHeartRate ? '-' : b.avgHeartRate > 100 ? '偏快' : b.avgHeartRate < 55 ? '偏慢' : '正常',
      tagCls: !b.avgHeartRate ? '' : (b.avgHeartRate > 100 || b.avgHeartRate < 55) ? 'vtag-warn' : 'vtag-ok'
    },
    {
      label: '血氧均值', val: b.avgBloodOxygen || '--', unit: '%', color: '#67C23A', icon: 'FirstAidKit',
      route: '/health-monitor/blood-oxygen',
      tag: !b.avgBloodOxygen ? '-' : b.avgBloodOxygen < 90 ? '过低' : b.avgBloodOxygen < 95 ? '偏低' : '良好',
      tagCls: !b.avgBloodOxygen ? '' : b.avgBloodOxygen < 90 ? 'vtag-danger' : b.avgBloodOxygen < 95 ? 'vtag-warn' : 'vtag-ok'
    },
    {
      label: '压力均值', val: b.avgPressure || '--', unit: '', color: '#3eb7ff', icon: 'MagicStick',
      route: '/health-monitor/pressure',
      tag: !b.avgPressure ? '-' : b.avgPressure > 80 ? '过高' : b.avgPressure > 60 ? '偏高' : '适中',
      tagCls: !b.avgPressure ? '' : b.avgPressure > 80 ? 'vtag-danger' : b.avgPressure > 60 ? 'vtag-warn' : 'vtag-ok'
    },
    {
      label: '体温均值', val: b.avgTemperature || '--', unit: '°C', color: '#00c8c8', icon: 'Sunny',
      tag: !b.avgTemperature ? '-' : b.avgTemperature > 37.5 ? '偏高' : b.avgTemperature < 36 ? '偏低' : '正常',
      tagCls: !b.avgTemperature ? '' : (b.avgTemperature > 37.5 || b.avgTemperature < 36) ? 'vtag-warn' : 'vtag-ok'
    },
    {
      label: '日均步数', val: b.avgSteps ? Math.round(b.avgSteps / 1000 * 10) / 10 + 'k' : '--', unit: '', color: '#F56C6C', icon: 'Promotion',
      tag: !b.avgSteps ? '-' : b.avgSteps < 5000 ? '偏少' : b.avgSteps > 12000 ? '充足' : '达标',
      tagCls: !b.avgSteps ? '' : b.avgSteps < 5000 ? 'vtag-warn' : 'vtag-ok'
    },
    {
      label: '高压均值',
      val: b.avgBloodPressureHigh ? Math.round(b.avgBloodPressureHigh) : '--',
      unit: 'mmHg',
      color: '#f87171',
      icon: 'Top',
      route: '/health-monitor/blood-pressure',
      tag: !b.avgBloodPressureHigh ? '-'
        : b.avgBloodPressureHigh <= 120 ? '正常'
        : b.avgBloodPressureHigh <= 140 ? '偏高'
        : '高血压',
      tagCls: !b.avgBloodPressureHigh ? ''
        : b.avgBloodPressureHigh <= 120 ? 'vtag-ok'
        : 'vtag-warn'
    },
    {
      label: '低压均值',
      val: b.avgBloodPressureLow ? Math.round(b.avgBloodPressureLow) : '--',
      unit: 'mmHg',
      color: '#36d399',
      icon: 'Bottom',
      route: '/health-monitor/blood-pressure',
      tag: !b.avgBloodPressureLow ? '-'
        : b.avgBloodPressureLow <= 80 ? '正常'
        : b.avgBloodPressureLow <= 90 ? '偏高'
        : '高血压',
      tagCls: !b.avgBloodPressureLow ? ''
        : b.avgBloodPressureLow <= 80 ? 'vtag-ok'
        : 'vtag-warn'
    },
    {
      label: '热量均值',
      val: b.avgCalories ? Math.round(b.avgCalories) : '--',
      unit: 'kcal',
      color: '#FFB84D',
      icon: 'Odometer',
      tag: !b.avgCalories ? '-'
        : b.avgCalories < 300 ? '偏低'
        : b.avgCalories > 800 ? '充足'
        : '适中',
      tagCls: !b.avgCalories ? ''
        : b.avgCalories < 300 ? 'vtag-warn'
        : 'vtag-ok'
    },
    (() => {
      const pressure = b.avgPressure || 0
      const hr = b.avgHeartRate || 0
      const hrDev = hr > 0 ? Math.min(100, Math.abs(hr - 75) / 25 * 100) : 0
      const warnRate = warningRates.length
        ? warningRates.reduce((sum, item) => sum + (item.rate || 0), 0) / warningRates.length
        : 0
      const fatigue = Math.min(100, Math.round(pressure * 0.4 + hrDev * 0.3 + warnRate * 0.3))
      return {
        label: '疲劳指数', val: pressure > 0 ? fatigue : '--', unit: '',
        color: fatigue >= 70 ? '#ff5252' : fatigue >= 45 ? '#ffd200' : '#38ef7d',
        icon: 'Cpu',
        tag: fatigue >= 70 ? '高疲劳' : fatigue >= 45 ? '中疲劳' : pressure > 0 ? '良好' : '-',
        tagCls: fatigue >= 70 ? 'vtag-danger' : fatigue >= 45 ? 'vtag-warn' : pressure > 0 ? 'vtag-ok' : ''
      }
    })()
  ]
}

export function buildDashboardDeviceCards({ deviceStats, deviceOnline, deviceOffline, deviceWarningCount, lowBatteryCount }) {
  const hasDeviceData = (deviceStats.total > 0) || (deviceStats.boundDevices > 0)
  const showVal = (val) => hasDeviceData ? val : '--'

  return [
    { label: '设备总数', val: showVal(deviceStats.boundDevices ?? deviceStats.total), cls: 'dc-blue', route: { path: '/admin/device-list' } },
    { label: '在线设备', val: showVal(deviceOnline), cls: 'dc-green', route: { path: '/admin/device-list', query: { online: 1 } } },
    { label: '离线设备', val: showVal(deviceOffline), cls: 'dc-gray', route: { path: '/admin/device-list', query: { online: 0 } } },
    { label: '预警设备', val: showVal(deviceWarningCount), cls: 'dc-red', route: { path: '/admin/device-list', query: { filter: 'warning' } } },
    { label: '电量不足', val: showVal(lowBatteryCount), cls: 'dc-orange', route: { path: '/admin/device-list', query: { filter: 'lowBattery' } } }
  ]
}

export function buildDashboardTop5DisplayData(top5Data) {
  return (top5Data || [])
    .filter((d) => d.userName && d.userName !== '--')
    .slice(0, 15)
}
