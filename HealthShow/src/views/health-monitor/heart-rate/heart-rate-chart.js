import { HR } from '@/constants/health-thresholds'
import { emptyOption, chartTooltip, categoryAxis, valueAxis, deptGrid, trendGrid, hourlyGrid, barLabel } from '@/utils/echarts-config'
import { initChart, gaugeOption, gradH, gradV } from '@/utils/chart-helpers'

export const heartRateChartMethods = {
  initGauge() {
    const c = initChart(this.charts, 'gauge', this.$refs.gaugeRef)
    if (c) c.setOption(gaugeOption(this.overview.avgHeartRate || 0, {
      min: 0,
      max: 160,
      colors: [[0.34, '#4FC3F7'], [0.75, '#52c41a'], [1, '#FFB84D']]
    }))
  },

  initDept(data) {
    const c = initChart(this.charts, 'dept', this.$refs.deptRef)
    if (!c) return
    if (!data.length) {
      c.setOption(emptyOption())
      return
    }
    const d = data.map(x => {
      const low = x.lowCount || 0
      const high = x.highCount || 0
      const total = x.totalCount || 1
      return {
        deptName: x.deptName || x.name,
        rate: Math.round((low + high) / total * 100)
      }
    })
    c.setOption({
      backgroundColor: 'transparent',
      grid: deptGrid(),
      xAxis: { ...valueAxis(), max: v => Math.ceil(v.max) + 1 },
      yAxis: { ...categoryAxis(d.map(x => x.deptName)), inverse: true },
      series: [{
        type: 'bar',
        barWidth: '46%',
        data: d.map(x => x.rate),
        itemStyle: { color: gradH('#FFB84D', '#FF6B35'), borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: '#FFB84D', fontSize: 11, fontFamily: 'Consolas', formatter: p => p.value + '%' }
      }]
    })
    c.off('click')
    c.on('click', (params) => {
      this.filterDept = this.filterDept === params.name ? '' : params.name
    })
  },

  initTrend(data) {
    const c = initChart(this.charts, 'trend', this.$refs.trendRef)
    if (!c) return
    const dates = data.dates || []
    const vals = data.values || []
    if (!dates.length) {
      c.setOption(emptyOption('暂无趋势数据'))
      return
    }
    c.setOption({
      backgroundColor: 'transparent',
      tooltip: chartTooltip(p => `${p[0].name}<br/>平均心率：<b style="color:#00d4ff">${p[0].value}</b> 次/分`),
      grid: trendGrid(),
      xAxis: { ...categoryAxis(dates, { fontSize: 10, interval: 4 }), boundaryGap: false },
      yAxis: valueAxis({ min: v => Math.max(0, v.min - 3), max: v => v.max + 3 }),
      series: [{
        type: 'line',
        data: vals,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#00d4ff', width: 2 },
        areaStyle: { color: gradV('rgba(0,212,255,0.28)', 'rgba(0,212,255,0.02)') },
        markPoint: {
          symbol: 'circle',
          symbolSize: 6,
          label: { fontSize: 10, fontWeight: 'bold', fontFamily: 'Consolas', offset: [0, -14] },
          data: [
            { type: 'max', name: '最高', itemStyle: { color: '#FFB84D' }, label: { color: '#FFB84D', formatter: p => '▲' + p.value } },
            { type: 'min', name: '最低', itemStyle: { color: '#4FC3F7' }, label: { color: '#4FC3F7', formatter: p => '▼' + p.value } }
          ]
        },
        markLine: {
          silent: true,
          symbol: 'none',
          data: [
            { yAxis: HR.HIGH, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '偏高 ' + HR.HIGH } },
            { yAxis: HR.LOW, lineStyle: { color: '#4FC3F7', type: 'dashed', width: 1 }, label: { color: '#4FC3F7', fontSize: 10, formatter: '偏低 ' + HR.LOW } }
          ]
        }
      }]
    })
  },

  renderHourly(vals) {
    const c = initChart(this.charts, 'hourly', this.$refs.hourlyRef)
    if (!c) return
    const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
    c.setOption({
      backgroundColor: 'transparent',
      tooltip: chartTooltip(p => p[0].value != null
        ? `${p[0].name}<br/>心率：<b style="color:#00d4ff">${p[0].value}</b> bpm`
        : `${p[0].name}<br/>暂无数据`),
      grid: hourlyGrid(),
      xAxis: { ...categoryAxis(hours, { fontSize: 9, interval: 3, lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: false },
      yAxis: valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)', min: v => v.min > 0 ? v.min - 4 : 50, max: v => v.max > 0 ? v.max + 4 : 120 }),
      series: [{
        type: 'line',
        data: vals,
        smooth: true,
        symbol: 'none',
        connectNulls: false,
        lineStyle: { color: '#a78bfa', width: 1.5 },
        areaStyle: { color: gradV('rgba(167,139,250,0.22)', 'rgba(167,139,250,0.02)') }
      }]
    })
  },

  renderHourlyDaily(dates, vals) {
    const c = initChart(this.charts, 'hourly', this.$refs.hourlyRef)
    if (!c) return
    if (!dates.length) {
      c.setOption(emptyOption('暂无数据', 13))
      return
    }
    c.setOption({
      backgroundColor: 'transparent',
      tooltip: chartTooltip(p => `${p[0].name}<br/>心率：<b style="color:#a78bfa">${p[0].value}</b> bpm`),
      grid: hourlyGrid(),
      xAxis: { ...categoryAxis(dates, { fontSize: 9, interval: Math.floor(dates.length / 5), lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: true },
      yAxis: valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)', min: v => v.min > 0 ? v.min - 4 : 50, max: v => v.max > 0 ? v.max + 4 : 120 }),
      series: [{
        type: 'bar',
        data: vals,
        barMaxWidth: 14,
        itemStyle: {
          color: gradV('#a78bfa', 'rgba(167,139,250,0.2)'),
          borderRadius: [3, 3, 0, 0]
        }
      }]
    })
  },

  renderDailyAnomaly(dates, counts) {
    const c = initChart(this.charts, 'hourly', this.$refs.hourlyRef)
    if (!c) return
    if (!dates.length) {
      c.setOption(emptyOption('暂无数据', 13))
      return
    }
    const sorted = [...counts].sort((a, b) => a - b)
    const median = sorted[Math.floor(sorted.length / 2)] || 1
    const yMax = Math.max(median * 3, 10)
    c.setOption({
      backgroundColor: 'transparent',
      tooltip: chartTooltip(p => `${p[0].name}<br/>异常人数：<b style="color:#FFB84D">${p[0].value}</b> 人`),
      grid: hourlyGrid(),
      xAxis: { ...categoryAxis(dates, { fontSize: 9, interval: Math.floor(dates.length / 5), lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: true },
      yAxis: valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)', max: yMax }),
      series: [{
        type: 'bar',
        data: counts,
        barMaxWidth: 14,
        itemStyle: { color: gradV('#FFB84D', 'rgba(255,184,77,0.2)'), borderRadius: [3, 3, 0, 0] },
        label: {
          show: true,
          position: 'top',
          color: '#FFB84D',
          fontSize: 9,
          fontFamily: 'Consolas',
          formatter: p => {
            if (p.value > yMax) {
              const v = p.value >= 1000 ? (p.value / 1000).toFixed(1) + 'k' : p.value
              return v + '↑'
            }
            return p.value
          }
        }
      }]
    })
  },

  initTrendDay(vals) {
    const c = initChart(this.charts, 'trend', this.$refs.trendRef)
    if (!c) return
    const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
    c.setOption({
      backgroundColor: 'transparent',
      tooltip: chartTooltip(p => p[0].value != null
        ? `${p[0].name}<br/>心率：<b style="color:#00d4ff">${p[0].value}</b> bpm`
        : `${p[0].name}<br/>暂无数据`),
      grid: trendGrid(),
      xAxis: { ...categoryAxis(hours, { fontSize: 10, interval: 3 }), boundaryGap: false },
      yAxis: valueAxis({ min: v => v.min > 0 ? v.min - 4 : 50, max: v => v.max > 0 ? v.max + 4 : 120 }),
      series: [{
        type: 'line',
        data: vals,
        smooth: true,
        symbol: 'none',
        connectNulls: false,
        lineStyle: { color: '#00d4ff', width: 2 },
        areaStyle: { color: gradV('rgba(0,212,255,0.28)', 'rgba(0,212,255,0.02)') },
        markLine: {
          silent: true,
          symbol: 'none',
          data: [
            { yAxis: HR.HIGH, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '偏高 ' + HR.HIGH } },
            { yAxis: HR.LOW, lineStyle: { color: '#4FC3F7', type: 'dashed', width: 1 }, label: { color: '#4FC3F7', fontSize: 10, formatter: '偏低 ' + HR.LOW } }
          ]
        }
      }]
    })
  }
}
