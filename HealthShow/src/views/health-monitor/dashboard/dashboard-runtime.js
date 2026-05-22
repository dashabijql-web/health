import dayjs from 'dayjs'
import { getMineAiReport, generateMineAiReport } from '@/api/ai'
import {
  buildDashboardAbnormalUserCount,
  collectDashboardNewDangerEvents,
  fetchDashboardBodyIndicatorData,
  fetchDashboardDeptState,
  fetchDashboardDeviceState,
  fetchDashboardKpiSnapshot,
  fetchDashboardOverviewData,
  fetchDashboardPersonCountData,
  fetchDashboardPreShiftData,
  fetchDashboardTop5Data,
  fetchDashboardWarningEventState,
  formatDashboardRefreshText
} from './dashboard-runtime-data'

export const dashboardRuntimeMethods = {
  async handleMineAi(force = false) {
    if (this.mineAiLoading) return
    this.mineAiLoading = true
    try {
      const res = await generateMineAiReport(force)
      if (res.code === 200 && res.data) {
        this.mineAiReport = res.data.reportContent
        this.mineAiTime = res.data.generateTime
        this.$message.success('全矿 AI 分析完成')
      } else {
        this.$message.error(res.message || '生成失败')
      }
    } catch {
      this.$message.error('AI 服务暂时不可用，请稍后重试')
    } finally {
      this.mineAiLoading = false
    }
  },

  async loadMineAiCache() {
    try {
      const res = await getMineAiReport()
      if (res.code === 200 && res.data) {
        this.mineAiReport = res.data.reportContent
        this.mineAiTime = res.data.generateTime
      }
    } catch {}
  },

  toggleMineAiPanel() {
    if (this.mineAiReport) {
      this.mineAiDialogVisible = true
      return
    }
    return this.handleMineAi(false)
  },

  initTime() {
    this.updateTime()
    this._timeTask?.start()
  },

  updateTime() {
    this.currentTime = dayjs().format('YYYY年MM月DD日 HH:mm:ss')
  },

  toggleFullscreen() {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {})
    } else {
      document.exitFullscreen().catch(() => {})
    }
  },

  triggerDangerNotification(event) {
    if (!('Notification' in window) || Notification.permission !== 'granted') return
    const typeNames = { heartRate: '心率异常', bloodOxygen: '血氧偏低', temperature: '体温异常', pressure: '压力异常', SOS: 'SOS求助', fall: '跌倒' }
    const typeName = typeNames[event.type] || event.type || '健康预警'
    const n = new Notification(`高危预警：${event.userName || '未知人员'}`, {
      body: `${typeName}  ${event.value || ''}  —  请立即处理`,
      icon: '/favicon.ico',
      tag: `alert-${event.id}`
    })
    n.onclick = () => { window.focus(); n.close() }
  },

  async fetchData(force = false) {
    if (this.isRefreshing) return
    this.isRefreshing = true
    await Promise.allSettled([
      this.fetchDashboardData(),
      this.fetchBodyIndicators(),
      this.fetchPersonCounts(),
      this.fetchDeviceData(),
      this.fetchWarningEvents(),
      this.fetchTop5Data(),
      this.loadDeptData(force),
      this.fetchTrendDaily(force),
      this.fetchWarningDist(force),
      this.fetchWarningTypes(),
      this.fetchPreShiftRate(force)
    ])
    this.isRefreshing = false
    this.lastRefreshTime = Date.now()
    this.updateRefreshText()
    const total = this.deviceStats.total || 0
    const onDutyCount = Math.round(total * (this.deviceStats.usageRate || 0) / 100)
    this.onDutyStats.onDuty = onDutyCount
    this.onDutyStats.offDuty = total - onDutyCount
    this.$nextTick(() => {
      this.initHourDistChart()
      this.initWarnTypeChart()
      this.initUnifiedTrendChart()
      this.initDeptChart()
    })
  },

  async fetchKpiData() {
    const snapshot = await fetchDashboardKpiSnapshot(this.warningEvents)
    this.kpiRealtimeOnline = snapshot.kpiRealtimeOnline
    this.kpiRealtimeTotal = snapshot.kpiRealtimeTotal
    this.kpiTodayWarnings = snapshot.kpiTodayWarnings
    this.kpiYesterdayWarnings = snapshot.kpiYesterdayWarnings
    this.kpiUnhandledHigh = snapshot.kpiUnhandledHigh
    this.kpiUnhandledMid = snapshot.kpiUnhandledMid
  },

  updateRefreshText() {
    this.lastRefreshText = formatDashboardRefreshText(this.lastRefreshTime)
  },

  async fetchTop5Data() {
    this.top5Data = await fetchDashboardTop5Data(this.periodRange)
  },

  async fetchDashboardData() {
    this.checkData = await fetchDashboardOverviewData(this.periodRange)
  },

  async fetchPreShiftRate(force = false) {
    const nextPreShiftData = await fetchDashboardPreShiftData(force)
    if (nextPreShiftData) this.preShiftData = nextPreShiftData
  },

  async fetchPersonCounts() {
    this.personCounts = await fetchDashboardPersonCountData(this.periodRange)
  },

  async fetchBodyIndicators() {
    this.bodyIndicators = await fetchDashboardBodyIndicatorData(this.periodRange)
  },

  async fetchDeviceData() {
    const nextState = await fetchDashboardDeviceState(this.periodRange)
    this.deviceStats = nextState.deviceStats
    this.warningRates = nextState.warningRates
    this.$nextTick(() => { this.initDeviceCharts() })
  },

  async fetchWarningEvents() {
    this.warningEvents = await fetchDashboardWarningEventState(this.periodRange)
    this.onDutyStats.abnormal = buildDashboardAbnormalUserCount(this.warningEvents)
    collectDashboardNewDangerEvents(this.warningEvents, this.seenAlertIds).forEach((event) => {
      this.triggerDangerNotification(event)
    })
  },

  async loadDeptData(force = false) {
    const nextState = await fetchDashboardDeptState(this.periodRange, force)
    this.deptDataList = nextState.deptDataList
    this.deptPersonStatsList = nextState.deptPersonStatsList
    this.$nextTick(() => { this.initDeptChart() })
  },

  startAutoRefresh() {
    this._refreshTask?.start()
  },

  onVisibilityChange() {
    if (document.hidden) {
      this._timeTask?.stop()
      this._refreshTask?.stop()
      this._kpiRefreshTask?.stop()
      this._refreshTextTask?.stop()
    } else {
      this.fetchData(true)
      this.fetchKpiData()
      this._timeTask?.start()
      this.startAutoRefresh()
      this._kpiRefreshTask?.start()
      this._refreshTextTask?.start()
    }
  },

  handleResize() {
    this._resizeTask?.start()
  }
}
