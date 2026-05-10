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
  buildProfileRiskSummary,
  buildProfileSummaryCards,
  buildRiskItems,
  buildHrItems,
  buildSpo2Items,
  buildVitalItems,
  buildWarnItems,
  calcAge,
  fmtTemp,
  fmtTime,
  hrClass,
  pressClass,
  spo2Class,
  tempClass
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
  const isOnline = ref(false)
  const lastUpdate = ref('--')
  const trendRef = ref(null)
  const printWindowRef = ref(null)
  const trend7 = ref({ avgHr: 0, avgSpo2: 0, avgTemp: 0 })
  const portraitTrend = ref(null)
  let trendChart = null

  const aiReportVisible = ref(false)
  const aiReportLoading = ref(false)
  const aiReportContent = ref('')
  const aiReportHtml = ref('')

  const { start: startPrintTask } = useTimeoutTask(() => printWindowRef.value?.print(), 600)

  const warnCount = computed(() => warnings.value.length)
  const pendCount = computed(() => warnings.value.filter((w) => !w.handled).length)
  const warn7Count = computed(() => {
    const cutoff = Date.now() - 7 * 86400000
    return warnings.value.filter((w) => {
      const t = new Date(w.createTime || w.time || 0).getTime()
      return t > cutoff
    }).length
  })

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
    vitals: vitals.value,
    pendCount: pendCount.value,
    warnCount: warnCount.value,
    warn7Count: warn7Count.value
  }))

  const hrItems = computed(() => buildHrItems({
    vitals: vitals.value,
    trend7: trend7.value,
    warnings: warnings.value,
    warn7Count: warn7Count.value
  }))
  const spo2Items = computed(() => buildSpo2Items({
    vitals: vitals.value,
    trend7: trend7.value,
    warnings: warnings.value,
    warnCount: warnCount.value,
    pendCount: pendCount.value
  }))
  const vitalItems = computed(() => buildVitalItems({
    vitals: vitals.value,
    exercise: exercise.value
  }))
  const warnItems = computed(() => buildWarnItems({
    warnings: warnings.value,
    warnCount: warnCount.value,
    pendCount: pendCount.value,
    warn7Count: warn7Count.value
  }))
  const riskItems = computed(() => buildRiskItems({
    vitals: vitals.value,
    warnCount: warnCount.value
  }))
  const profileRiskSummary = computed(() => buildProfileRiskSummary({
    pendCount: pendCount.value,
    warn7Count: warn7Count.value,
    vitals: vitals.value
  }))
  const nextActionSummary = computed(() => buildNextActionSummary({
    pendCount: pendCount.value,
    isOnline: isOnline.value,
    vitals: vitals.value
  }))
  const profileSummaryCards = computed(() => buildProfileSummaryCards({
    profileRiskSummary: profileRiskSummary.value,
    pendCount: pendCount.value,
    warnCount: warnCount.value,
    isOnline: isOnline.value,
    lastUpdate: lastUpdate.value,
    nextActionSummary: nextActionSummary.value
  }))

  function resetProfileState() {
    resetEmployeeProfileState({
      vitals,
      exercise,
      warnings,
      isOnline,
      lastUpdate,
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

    const [portraitRes, warnRes] = await Promise.allSettled([
      getHealthPortrait(code),
      getRiskWarningList({ userCode: code, page: 1, size: 20 })
    ])

    if (portraitRes.status === 'fulfilled' && portraitRes.value?.data) {
      applyEmployeePortrait(portraitRes.value.data, {
        vitals,
        exercise,
        isOnline,
        lastUpdate,
        trend7,
        portraitTrend
      })
    }

    if (warnRes.status === 'fulfilled' && warnRes.value?.data) {
      warnings.value = normalizeEmployeeWarnings(warnRes.value.data)
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
    fmtTemp,
    goArchive,
    goMineEntry,
    goReportCenter,
    goWorkbench,
    hrClass,
    hrPct: (v) => {
      if (!v) return 0
      return Math.min(100, Math.max(0, ((v - 40) / 100) * 100))
    },
    hrItems,
    isBpAbnormal,
    isBpDanger,
    isHrAbnormal,
    isHrDanger,
    isOnline,
    isPressureHigh,
    isSpo2Abnormal,
    isSpo2Danger,
    isTempAbnormal,
    isTempDanger,
    lastUpdate,
    loading,
    nextActionSummary,
    openAiReport,
    pendCount,
    printAiReport,
    profileInsightLines,
    profileSummaryCards,
    pressClass,
    refresh,
    riskItems,
    spo2Class,
    spo2Pct: (v) => {
      if (!v) return 0
      return Math.min(100, Math.max(0, ((v - 80) / 20) * 100))
    },
    spo2Items,
    tempClass,
    tempPct: (v) => {
      if (!v) return 0
      const t = v > 100 ? v / 10 : v
      return Math.min(100, Math.max(0, ((t - 35) / 5) * 100))
    },
    trendRef,
    vitalItems,
    vitals,
    warn7Count,
    warnCount,
    warnItems,
    warnings,
    exercise
  }
}
