import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from '@/utils/echarts-setup'
import { getRiskWarningList } from '@/api/risk-warning'
import { getHealthPortrait } from '@/api/health-portrait'
import { generateEmployeeReport } from '@/api/ai'
import { renderMarkdown } from '@/utils/lazy-vendors'
import { useIntervalTask } from '@/composables/useIntervalTask'
import { useTimeoutTask } from '@/composables/useTimeoutTask'
import {
  buildNextActionSummary,
  buildProfileInsightLines,
  buildProfileSummaryCards,
  calcAge,
  fmtTime
} from './employee-profile-view-model'
import {
  applyEmployeePortrait,
  buildEmployeeProfilePrintHtml,
  buildMineEntryRoute,
  buildReportCenterRoute,
  buildWorkbenchRoute,
  disposeEmployeeChart,
  loadEmployeeAiReport,
  normalizeEmployeeWarnings,
  renderEmployeeTrendChart,
  resetEmployeeProfileState,
  syncEmpInfoFromRoute
} from './employee-profile-runtime'

export function useEmployeeProfilePage() {
  const route = useRoute()
  const router = useRouter()

  const empInfo = ref(syncEmpInfoFromRoute(route))
  const loading = ref(false)
  const vitals = ref({})
  const exercise = ref({ todaySteps: 0, todayCalories: 0 })
  const warnings = ref([])
  const warningTotal = ref(0)
  const pendingTotal = ref(0)
  const warning7Total = ref(0)
  const isOnline = ref(false)
  const lastUpdate = ref('--')
  const freshnessStatus = ref('no_data')
  const trendRef = ref(null)
  const printWindowRef = ref(null)
  const trend7 = ref({ avgHr: 0, avgSpo2: 0, avgTemp: 0 })
  const portraitTrend = ref(null)
  const personCommandVisible = ref(false)
  const incidentDrawerVisible = ref(false)
  const currentIncidentEvent = ref(null)
  let trendChart = null

  const aiReportVisible = ref(false)
  const aiReportLoading = ref(false)
  const aiReportContent = ref('')
  const aiReportHtml = ref('')

  const { start: startPrintTask } = useTimeoutTask(() => printWindowRef.value?.print(), 600)

  const warnCount = computed(() => warningTotal.value)
  const pendCount = computed(() => pendingTotal.value)
  const warn7Count = computed(() => warning7Total.value)

  const isHrAbnormal = computed(() => {
    const v = vitals.value.heartRate
    return v && (v < 60 || v > 100)
  })
  const isHrDanger = computed(() => {
    const v = vitals.value.heartRate
    return v && (v < 50 || v > 120)
  })
  const isSpo2Abnormal = computed(() => {
    const v = vitals.value.bloodOxygen
    return v && v < 95
  })
  const isSpo2Danger = computed(() => {
    const v = vitals.value.bloodOxygen
    return v && v < 90
  })
  const isTempAbnormal = computed(() => {
    const v = vitals.value.temperature
    const t = v > 100 ? v / 10 : v
    return t && (t < 36 || t > 37.3)
  })
  const isTempDanger = computed(() => {
    const v = vitals.value.temperature
    const t = v > 100 ? v / 10 : v
    return t && (t < 35 || t > 38.5)
  })
  const isBpAbnormal = computed(() => {
    const s = vitals.value.systolic
    const d = vitals.value.diastolic
    return (s && s >= 140) || (d && d >= 90)
  })
  const isBpDanger = computed(() => {
    const s = vitals.value.systolic
    const d = vitals.value.diastolic
    return (s && s >= 160) || (d && d >= 100)
  })
  const isPressureHigh = computed(() => {
    const v = vitals.value.pressure
    return v && v >= 70
  })

  const profileInsightLines = computed(() => buildProfileInsightLines({
    pendCount: pendCount.value,
    warnCount: warnCount.value,
    warn7Count: warn7Count.value
  }))

  const nextActionSummary = computed(() => buildNextActionSummary({
    pendCount: pendCount.value,
    isOnline: isOnline.value
  }))
  const profileSummaryCards = computed(() => buildProfileSummaryCards({
    pendCount: pendCount.value,
    warnCount: warnCount.value,
    isOnline: isOnline.value,
    lastUpdate: lastUpdate.value,
    nextActionSummary: nextActionSummary.value
  }))
  const trendStats = computed(() => [
    {
      key: 'avg-hr',
      label: '7日均心率',
      value: trend7.value.avgHr || '--',
      unit: 'bpm',
      tone: (vitals.value.heartRate && (vitals.value.heartRate < 60 || vitals.value.heartRate > 100)) ? 'danger' : 'accent'
    },
    {
      key: 'avg-spo2',
      label: '7日均血氧',
      value: trend7.value.avgSpo2 || '--',
      unit: '%',
      tone: (vitals.value.bloodOxygen && vitals.value.bloodOxygen < 95) ? 'warning' : 'safe'
    },
    {
      key: 'warn-7d',
      label: '7日预警',
      value: warn7Count.value,
      unit: '次',
      tone: warn7Count.value > 0 ? 'danger' : 'muted'
    }
  ])
  const recentWarningSummary = computed(() => [
    { key: 'pending', label: '未处理', value: pendCount.value, tone: pendCount.value > 0 ? 'danger' : 'safe' },
    { key: 'warn7', label: '近7日', value: warn7Count.value, tone: warn7Count.value > 0 ? 'warning' : 'muted' },
    { key: 'warn30', label: '近30日', value: warnCount.value, tone: warnCount.value > 0 ? 'accent' : 'muted' }
  ])
  const recentWarnings = computed(() => warnings.value.slice(0, 4))
  const recentWarningFootnote = computed(() => {
    if (!warnCount.value) return '当前没有近30日预警记录。'
    if (warnCount.value > recentWarnings.value.length) {
      return `已展示最近 ${recentWarnings.value.length} 条，更多记录可进入预警中心继续查看。`
    }
    return '当前按时间倒序展示最近预警。'
  })

  function resetProfileState() {
    resetEmployeeProfileState({
      vitals,
      exercise,
      warnings,
      warningTotal,
      pendingTotal,
      warning7Total,
      isOnline,
      lastUpdate,
      freshnessStatus,
      trend7,
      portraitTrend,
      aiReportVisible,
      aiReportLoading,
      aiReportContent,
      aiReportHtml
    })
  }

  async function refresh() {
    const code = empInfo.value.empCode
    if (!code) return

    const now = new Date()
    const endDate = now.toISOString().slice(0, 10)
    const start30 = new Date(now.getTime() - 29 * 86400000).toISOString().slice(0, 10)
    const start7 = new Date(now.getTime() - 6 * 86400000).toISOString().slice(0, 10)
    const [portraitRes, warnRes, pendingRes, warn7Res] = await Promise.allSettled([
      getHealthPortrait(code),
      getRiskWarningList({ userCode: code, startDate: start30, endDate, page: 1, size: 4 }),
      getRiskWarningList({ userCode: code, handled: false, startDate: start30, endDate, page: 1, size: 1 }),
      getRiskWarningList({ userCode: code, startDate: start7, endDate, page: 1, size: 1 })
    ])

    if (portraitRes.status === 'fulfilled' && portraitRes.value?.data) {
      applyEmployeePortrait(portraitRes.value.data, {
        vitals,
        exercise,
        isOnline,
        lastUpdate,
        freshnessStatus,
        trend7,
        portraitTrend
      })
    }

    if (warnRes.status === 'fulfilled' && warnRes.value?.data) {
      warnings.value = normalizeEmployeeWarnings(warnRes.value.data)
      warningTotal.value = Number(warnRes.value.data.total) || 0
    }
    if (pendingRes.status === 'fulfilled' && pendingRes.value?.data) {
      pendingTotal.value = Number(pendingRes.value.data.total) || 0
    }
    if (warn7Res.status === 'fulfilled' && warn7Res.value?.data) {
      warning7Total.value = Number(warn7Res.value.data.total) || 0
    }

    await nextTick()
    renderTrendChart()
  }

  async function loadProfilePage() {
    empInfo.value = syncEmpInfoFromRoute(route)
    resetProfileState()
    if (!empInfo.value.empCode) return

    loading.value = true
    try {
      await refresh()
    } finally {
      loading.value = false
    }
  }

  const { start: startProfileRefresh } = useIntervalTask(refresh, 30000)

  function renderTrendChart() {
    trendChart = renderEmployeeTrendChart({
      el: trendRef.value,
      chart: trendChart,
      trend: portraitTrend.value,
      echartsLib: echarts
    })
  }

  const openAiReport = async () => {
    if (!empInfo.value.empCode) return
    aiReportContent.value = ''
    aiReportHtml.value = ''
    aiReportLoading.value = true
    aiReportVisible.value = true
    try {
      const nextReport = await loadEmployeeAiReport(empInfo.value.empCode, generateEmployeeReport, renderMarkdown)
      aiReportContent.value = nextReport.content
      aiReportHtml.value = nextReport.html
    } catch {
      aiReportContent.value = ''
      aiReportHtml.value = ''
    } finally {
      aiReportLoading.value = false
    }
  }

  function printAiReport() {
    if (!aiReportContent.value) return
    const html = buildEmployeeProfilePrintHtml(empInfo.value.empName, aiReportHtml.value)
    const win = window.open('', '_blank')
    if (!win) return
    win.document.write(html)
    win.document.close()
    printWindowRef.value = win
    startPrintTask()
  }

  function goArchive() {
    router.push('/health-monitor/employee-archive')
  }

  function goMineEntry() {
    router.push(buildMineEntryRoute(empInfo.value))
  }

  function goWorkbench() {
    router.push(buildWorkbenchRoute(empInfo.value))
  }

  function goReportCenter() {
    router.push(buildReportCenterRoute(empInfo.value))
  }

  function goWarningCenter() {
    router.push({
      path: '/alert-management/records',
      query: {
        keyword: empInfo.value.empName || empInfo.value.empCode || ''
      }
    })
  }

  function openPersonCommand() {
    if (!empInfo.value.empCode) return
    personCommandVisible.value = true
  }

  function openEmergency(event) {
    if (!event?.id || !event?.occurredAt) return
    currentIncidentEvent.value = event
    incidentDrawerVisible.value = true
  }

  async function handleIncidentUpdated() {
    await refresh()
  }

  onMounted(async () => {
    await loadProfilePage()
    startProfileRefresh()
  })

  watch(() => route.fullPath, () => {
    if (route.name !== 'EmployeeProfile') return
    void loadProfilePage()
  })

  onUnmounted(() => {
    trendChart = disposeEmployeeChart(trendChart)
  })

  return {
    aiReportContent,
    aiReportHtml,
    aiReportLoading,
    aiReportVisible,
    calcAge,
    empInfo,
    fmtTime,
    goArchive,
    goMineEntry,
    goReportCenter,
    goWorkbench,
    isBpAbnormal,
    isBpDanger,
    isHrAbnormal,
    isHrDanger,
    isOnline,
    freshnessStatus,
    isPressureHigh,
    isSpo2Abnormal,
    isSpo2Danger,
    isTempAbnormal,
    isTempDanger,
    lastUpdate,
    loading,
    nextActionSummary,
    openAiReport,
    openEmergency,
    openPersonCommand,
    pendCount,
    printAiReport,
    profileInsightLines,
    profileSummaryCards,
    personCommandVisible,
    incidentDrawerVisible,
    currentIncidentEvent,
    handleIncidentUpdated,
    recentWarnings,
    recentWarningSummary,
    recentWarningFootnote,
    refresh,
    trendStats,
    trendRef,
    vitals,
    warn7Count,
    warnCount,
    warnings,
    exercise,
    goWarningCenter
  }
}
