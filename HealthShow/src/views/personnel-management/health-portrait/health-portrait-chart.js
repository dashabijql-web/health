function buildFallbackTrendDates() {
  const dates = []
  for (let i = 6; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    dates.push(`${date.getMonth() + 1}/${date.getDate()}`)
  }
  return dates
}

export function buildPortraitTrendChartOption(echarts, trendData) {
  const rawDates = trendData?.dates || []
  const hasData = rawDates.length > 0
  const dates = hasData ? rawDates : buildFallbackTrendDates()
  const heartRates = hasData ? (trendData?.heartRates || []) : []
  const bloodOxygens = hasData ? (trendData?.bloodOxygens || []) : []

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(20,24,48,.95)',
      borderColor: '#2d3561',
      textStyle: { color: '#c8d8e8', fontSize: 12 }
    },
    legend: { data: ['心率', '血氧'], bottom: 2, textStyle: { color: '#7eb8d4', fontSize: 11 } },
    grid: { top: 16, right: 52, bottom: 38, left: 52 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#232b4d' } },
      axisLabel: { color: '#7eb8d4', fontSize: 10 }
    },
    yAxis: [
      {
        type: 'value',
        name: '心率',
        min: 40,
        max: 140,
        nameTextStyle: { color: '#7eb8d4', fontSize: 10 },
        axisLine: { lineStyle: { color: '#232b4d' } },
        splitLine: { lineStyle: { color: '#232b4d' } },
        axisLabel: { color: '#7eb8d4', fontSize: 10 }
      },
      {
        type: 'value',
        name: '血氧%',
        min: 85,
        max: 100,
        nameTextStyle: { color: '#7eb8d4', fontSize: 10 },
        axisLine: { lineStyle: { color: '#232b4d' } },
        splitLine: { show: false },
        axisLabel: { color: '#7eb8d4', fontSize: 10 }
      }
    ],
    series: [
      {
        name: '心率',
        type: 'line',
        data: heartRates,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2 },
        itemStyle: { color: '#ff5252' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,82,82,.3)' },
            { offset: 1, color: 'rgba(255,82,82,0)' }
          ])
        }
      },
      {
        name: '血氧',
        type: 'line',
        yAxisIndex: 1,
        data: bloodOxygens,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2 },
        itemStyle: { color: '#00d4ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,212,255,.3)' },
            { offset: 1, color: 'rgba(0,212,255,0)' }
          ])
        }
      }
    ]
  }
}

export function buildPortraitRadarChartOption(healthScores) {
  const scores = healthScores || {}
  return {
    backgroundColor: 'transparent',
    radar: {
      center: ['50%', '50%'],
      radius: '68%',
      indicator: [
        { name: '心率', max: 100 },
        { name: '血氧', max: 100 },
        { name: '活动', max: 100 },
        { name: '血压', max: 100 },
        { name: '体温', max: 100 },
        { name: '压力', max: 100 }
      ],
      axisName: { color: '#7eb8d4', fontSize: 11 },
      axisLine: { lineStyle: { color: '#232b4d' } },
      splitLine: { lineStyle: { color: '#232b4d' } },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,.02)', 'rgba(0,212,255,.05)'] } }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [
              scores.heartRate || null,
              scores.bloodOxygen || null,
              scores.activity || null,
              scores.bloodPressure || null,
              scores.temperature || null,
              scores.pressure || null
            ],
            name: '健康评分',
            areaStyle: { color: 'rgba(0,212,255,.18)' },
            lineStyle: { color: '#00d4ff', width: 2 },
            itemStyle: { color: '#00d4ff' }
          }
        ]
      }
    ]
  }
}

export function renderPortraitTrendChart({ echarts, element, chart, trendData }) {
  if (!element) return chart
  chart?.dispose()
  const nextChart = echarts.init(element)
  nextChart.setOption(buildPortraitTrendChartOption(echarts, trendData))
  return nextChart
}

export function renderPortraitRadarChart({ echarts, element, chart, healthScores }) {
  if (!element) return chart
  chart?.dispose()
  const nextChart = echarts.init(element)
  nextChart.setOption(buildPortraitRadarChartOption(healthScores))
  return nextChart
}

export function disposePortraitChart(chart) {
  chart?.dispose()
  return null
}

