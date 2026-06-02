import * as echarts from '@/utils/echarts-setup-radar'
import { markRaw } from 'vue'
import {
  buildDeptDetailChartOption,
  buildEnvHealthChartOption,
  buildGaugeChartOption,
  buildHourDistChartOption,
  buildUnifiedTrendChartOption,
  buildWarnTypeChartOption
} from './dashboard-chart-options'
import {
  buildDashboardDateRange,
  buildDashboardEnvSeries,
  fetchDashboardDeptDetailData,
  fetchDashboardMetricDetailData,
  fetchDashboardTrendDailyData,
  fetchDashboardWarningDistData,
  fetchDashboardWarningTypesData,
  resolveDashboardWarningDistDate,
  resolveDashboardWarningDistSeries
} from './dashboard-chart-data'

export const dashboardChartMethods = {
  openDeptDetailModal(deptName) {
    this.deptDetailModal.deptName = deptName
    this.deptDetailModal.dateRange = buildDashboardDateRange()
    this.deptDetailModal.visible = true
  },

  async loadDeptDetailChart(elArg) {
    const modal = this.deptDetailModal
    modal.loading = true
    const [startTime, endTime] = modal.dateRange
    try {
      const detailData = await fetchDashboardDeptDetailData({ deptName: modal.deptName, startTime, endTime })
      if (!detailData) return
      await this.$nextTick()
      const el = elArg || this.$refs.deptDetailChartRef
      if (!el) return
      if (modal.chart) modal.chart.dispose()
      const echartsLib = this.$echarts || window.echarts || (await import('@/utils/echarts-setup-radar'))
      modal.chart = markRaw(echartsLib.init(el, null, { renderer: 'canvas' }))
      modal.chart.setOption(buildDeptDetailChartOption(detailData))
    } finally {
      modal.loading = false
    }
  },

  initDeviceCharts() {
    const totalDevices = this.deviceStats.boundDevices ?? this.deviceStats.total ?? 1
    const onlineRate = totalDevices > 0 ? Math.round((this.deviceOnline / totalDevices) * 100) : 0
    this.initGauge('onlineRateChart', onlineRate, '#00c8ff', '#ff3b3b')
    this.initGauge('activeRateChart', this.deviceStats.activeRate || 0, '#00e676', '#ff3b3b')
    this.initGauge('usageRateChart', this.deviceStats.usageRate || 0, '#00c8ff', '#ff8c00')
    this.initGauge('warningRateChart', this.deviceStats.warningRate || 0, '#ff3b3b', '#00e676')
  },

  initEnvHealthChart() {
    const el = this.getReadyChartDom(this.$refs.envChartRef, this.initEnvHealthChart)
    if (!el) return
    if (this._envChart) this._envChart.dispose()
    this._envChart = echarts.init(el, 'dark')
    this._envChart.setOption(buildEnvHealthChartOption(buildDashboardEnvSeries()), true)
  },

  async fetchTrendDaily(force = false) {
    this.trendDailyData = await fetchDashboardTrendDailyData(this.activePeriod, force)
    this.$nextTick(() => this.initUnifiedTrendChart())
  },

  async fetchWarningDist(force = false) {
    this.warningDistData = await fetchDashboardWarningDistData(this.periodRange, this.activePeriod, force)
    this.$nextTick(() => this.initHourDistChart())
  },

  async fetchWarningTypes() {
    this.warningTypesData = await fetchDashboardWarningTypesData(this.periodRange)
    this.$nextTick(() => this.initWarnTypeChart())
  },

  initUnifiedTrendChart() {
    const dom = this.getReadyChartDom(this.$refs.unifiedTrendChart, this.initUnifiedTrendChart)
    if (!dom) return
    if (this.charts.unifiedTrend) this.charts.unifiedTrend.dispose()
    const chart = echarts.init(dom)
    this.charts.unifiedTrend = chart

    const rawData = this.trendDailyData
    chart.setOption(buildUnifiedTrendChartOption({
      rawData,
      vitalRanges: this.VITAL_NORMAL_RANGES
    }))
    if (!rawData.length) return
    const dates = rawData.map(d => d.date)
    chart.getZr().off('click')
    chart.getZr().on('click', e => {
      const pt = chart.convertFromPixel({ seriesIndex: 0 }, [e.offsetX, e.offsetY])
      if (!pt) return
      const idx = Math.round(pt[0])
      if (idx < 0 || idx >= dates.length) return
      const date = dates[idx]
      if (!date) return
      this.$router.push({ path: '/alert-management/records', query: { startDate: date, endDate: date } })
    })
  },

  initWarnTypeChart() {
    const dom = this.getReadyChartDom('warnTypeChart', this.initWarnTypeChart)
    if (!dom) return
    if (this.charts.warnType) this.charts.warnType.dispose()
    const chart = echarts.init(dom)
    this.charts.warnType = chart
    chart.setOption(buildWarnTypeChartOption(this.warnTypeData))
  },

  initHourDistChart() {
    const dom = this.getReadyChartDom('hourDistChart', this.initHourDistChart)
    if (!dom) return
    if (this.charts.hourDist) this.charts.hourDist.dispose()
    const chart = echarts.init(dom)
    this.charts.hourDist = chart

    const dist = this.warningDistData || { labels: [], counts: [] }
    const seriesData = resolveDashboardWarningDistSeries(dist, this.activePeriod)
    chart.setOption(buildHourDistChartOption({
      labels: seriesData.labels,
      vals: seriesData.vals,
      activePeriod: this.activePeriod,
      maxVal: seriesData.maxVal,
      yMax: seriesData.yMax
    }))
    chart.off('click')
    chart.on('click', (params) => {
      if (params.value === 0) return
      const date = resolveDashboardWarningDistDate(dist, this.activePeriod, params.dataIndex)
      if (!date) return
      this.$router.push({ path: '/alert-management/records', query: { startDate: date, endDate: date } })
    })
  },

  initGauge(id, value, highColor, lowColor) {
    const dom = this.getReadyChartDom(id, () => this.initGauge(id, value, highColor, lowColor))
    if (!dom) return
    if (this.charts[id]) this.charts[id].dispose()
    const chart = echarts.init(dom)
    this.charts[id] = chart
    chart.setOption(buildGaugeChartOption({ value, highColor, lowColor }))
  },

  onKpiClick(k) {
    if (k.route) this.$router.push(k.route)
  },

  goToDeviceList(card) {
    this.$router.push(card.route)
  },

  onMetricCardClick(m) {
    if (m.metricDetail === false) {
      this.openDeptPersonModal()
      return
    }
    this.metricDetailModal.metricType = m.key
    this.metricDetailModal.metricLabel = m.label
    this.metricDetailModal.metricColor = m.color
    this.metricDetailModal.dateRange = buildDashboardDateRange()
    this.metricDetailModal.visible = true
  },

  async loadMetricDetailChart(elArg) {
    const modal = this.metricDetailModal
    modal.loading = true
    const [startTime, endTime] = modal.dateRange
    try {
      const detailData = await fetchDashboardMetricDetailData({ metricType: modal.metricType, startTime, endTime })
      if (!detailData) return
      await this.$nextTick()
      const el = elArg || this.$refs.metricDetailChartRef
      if (!el) return
      if (modal.chart) modal.chart.dispose()
      const echartsLib = this.$echarts || window.echarts || (await import('@/utils/echarts-setup-radar'))
      modal.chart = markRaw(echartsLib.init(el, null, { renderer: 'canvas' }))
      modal.chart.setOption(buildDeptDetailChartOption({
        ...detailData,
        personColor: modal.metricColor
      }))
    } finally {
      modal.loading = false
    }
  }
}
