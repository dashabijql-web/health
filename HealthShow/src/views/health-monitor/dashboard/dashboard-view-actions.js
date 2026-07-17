import { markRaw } from 'vue'
import * as echarts from '@/utils/echarts-setup-radar'
import { buildDeptChartOption, buildDeptPersonChartOption } from './dashboard-chart-options'
import { fetchDashboardDeptPersonChartData } from './dashboard-chart-data'

function formatDayRange(daysBack = 6) {
  const today = new Date()
  const start = new Date(today)
  start.setDate(start.getDate() - daysBack)
  const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return [fmt(start), fmt(today)]
}

export const dashboardViewActions = {
  goToEmployeeProfile(item) {
    const code = item?.userCode || item?.empCode
    if (!code) return
    this.$router.push({
      path: '/health-monitor/employee-profile',
      query: {
        empCode: code,
        empName: item.userName || item.empName || '',
        deptName: item.deptName || '',
        jobTypeName: item.jobTypeName || '',
        phone: item.phone || '',
        imei: item.imei || '',
        from: this.$route.fullPath
      }
    })
  },

  goToCommandIncident(event) {
    if (!event?.id || !event?.occurredAt) return
    this.$router.push({
      path: '/safety-command/index',
      query: {
        warningId: String(event.id),
        occurredAt: event.occurredAt,
        incidentId: event.incidentId || '',
        person: event.userName || '',
        area: event.location || event.deptName || '',
        from: 'dashboard'
      }
    })
  },

  openDeptPersonModal() {
    this.deptPersonModal.dateRange = formatDayRange()
    this.deptPersonModal.visible = true
  },

  async loadDeptPersonChart(elArg) {
    const modal = this.deptPersonModal
    modal.loading = true
    const [startTime, endTime] = modal.dateRange
    try {
      const chartData = await fetchDashboardDeptPersonChartData({ startTime, endTime })
      if (!chartData) return
      await this.$nextTick()
      const el = elArg || this.$refs.deptPersonChartRef
      if (!el) return
      if (modal.chart) modal.chart.dispose()
      const echartsLib = this.$echarts || window.echarts || echarts
      modal.chart = markRaw(echartsLib.init(el, null, { renderer: 'canvas' }))
      modal.chart.setOption(buildDeptPersonChartOption(chartData))
    } finally {
      modal.loading = false
    }
  },

  switchPeriod(val) {
    if (this.activePeriod === val) return
    this.activePeriod = val
    this.fetchData().then(() => this.fetchKpiData())
  },

  getReadyChartDom(target, retryFn) {
    const dom = typeof target === 'string' ? document.getElementById(target) : target
    if (!dom) return null
    if (dom.clientWidth === 0 || dom.clientHeight === 0) {
      if (retryFn) setTimeout(() => retryFn.call(this), 160)
      return null
    }
    return dom
  },

  initDeptChart() {
    const dom = this.getReadyChartDom('deptDataChart', this.initDeptChart)
    if (!dom) return
    if (this.charts.dept) this.charts.dept.dispose()
    const chart = echarts.init(dom)
    this.charts.dept = chart
    chart.setOption(buildDeptChartOption({
      deptStatsList: this.deptPersonStatsList,
      deptDataList: this.deptDataList,
      riskDeptList: this.riskDeptList
    }))
    chart.on('click', params => {
      if (params.name) this.openDeptDetailModal(params.name)
    })
    chart.getZr().setCursorStyle('pointer')
    this.$nextTick(() => { chart.resize() })
  }
}
