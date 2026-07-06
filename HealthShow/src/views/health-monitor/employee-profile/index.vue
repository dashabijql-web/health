<template>
  <div class="ep-page hm-page-shell">
    <PageHeroHeader
      class="ep-hero"
      variant="cockpit"
      eyebrow="Employee Portrait"
      title="职工健康画像"
      :description="`聚焦 ${empInfo.empName || '--'} 的实时体征、预警闭环和 7 日趋势，给值班与画像分析一个统一视角。`"
    >
      <template #meta>
        <div class="ep-hero-meta">
          <span class="hm-status-chip">{{ empInfo.empName || '--' }}</span>
          <span :class="['hm-status-chip', isOnline ? 'hm-status-chip--success' : 'hm-status-chip--warning']">
            {{ isOnline ? '在线中' : '当前离线' }}
          </span>
          <span class="hm-status-chip">最近更新 {{ lastUpdate }}</span>
        </div>
      </template>
      <template #actions>
        <button type="button" class="hm-action-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <button
          type="button"
          class="hm-action-btn hm-action-btn--success"
          @click="openAiReport"
          :disabled="aiReportLoading"
        >
          <el-icon><Document /></el-icon>
          {{ aiReportLoading ? '生成中...' : 'AI 诊断报告' }}
        </button>
      </template>
    </PageHeroHeader>

    <MetricStrip
      class="ep-summary-strip"
      :items="summaryMetricItems"
    />

    <!-- 实时状态横幅 -->
    <div :class="['ep-status-banner', `ep-sb--${statusBannerTone}`]">
      <div class="ep-sb-dot"></div>
      <div class="ep-sb-content">
        <span class="ep-sb-title">{{ statusText }}</span>
        <span class="ep-sb-detail">{{ statusDetail }}</span>
      </div>
      <div class="ep-sb-right">
        <span class="ep-sb-pending" v-if="pendCount > 0">{{ pendCount }} 条未处理预警</span>
        <span class="ep-sb-update">更新：{{ lastUpdate }}</span>
      </div>
    </div>

    <!-- AI 报告 Dialog -->
    <el-dialog v-model="aiReportVisible" :title="'AI 健康诊断报告 — ' + empInfo.empName" width="820px" :close-on-click-modal="false">
      <div v-if="aiReportLoading" style="text-align:center;padding:40px 0">
        <div class="ep-report-dots"><span></span><span></span><span></span></div>
        <p style="color:#8ba6c8;margin-top:16px">正在生成健康诊断报告，请稍候（约15~30秒）...</p>
      </div>
      <div v-else-if="aiReportContent" v-html="aiReportHtml" class="ep-report-content"></div>
      <div v-else style="text-align:center;color:#666;padding:40px 0">生成失败，请重试</div>
      <template #footer>
        <el-button @click="aiReportVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!aiReportContent || aiReportLoading" @click="printAiReport">打印 / 导出 PDF</el-button>
      </template>
    </el-dialog>

    <!-- 主体三列 -->
    <div class="ep-body" v-loading="loading">

      <!-- ═══════ 左栏 ═══════ -->
      <aside class="ep-left">

        <!-- 7日体征趋势（优先展示，做大） -->
        <div class="ep-panel ep-trend">
          <div class="ep-ph">
            <span class="ep-ph-bar"></span>7日体征趋势
          </div>
          <div class="ep-trend-stats">
            <div v-for="item in trendStats" :key="item.key" :class="['ep-trend-stat', `tone-${item.tone}`]">
              <span class="ep-trend-stat-label">{{ item.label }}</span>
              <span class="ep-trend-stat-value">{{ item.value }}<em v-if="item.unit">{{ item.unit }}</em></span>
            </div>
          </div>
          <div class="ep-trend-chart-wrap">
            <div ref="trendRef" class="ep-trend-chart"></div>
            <div v-if="!hasTrendData" class="ep-trend-nodata">
              <span>暂无7日趋势数据</span>
              <em>数据接入后自动更新</em>
            </div>
          </div>
        </div>

        <!-- AI 风险摘要 -->
        <div class="ep-panel ep-ai-summary">
          <div class="ep-ph">
            <span class="ep-ph-bar"></span>AI 风险摘要
            <span class="ep-ph-link" @click="openAiReport">完整报告</span>
          </div>
          <div class="ep-ai-list">
            <div v-for="(item, idx) in profileInsightLines" :key="idx" class="ep-ai-item">
              <span class="ep-ai-dot"></span>
              <span>{{ item }}</span>
            </div>
          </div>
        </div>

        <!-- 基本信息（紧凑，置底） -->
        <div class="ep-panel ep-basic">
          <div class="ep-ph"><span class="ep-ph-bar"></span>职工信息</div>
          <div class="ep-basic-compact">
            <div class="ep-bc-name">{{ empInfo.empName || '--' }}</div>
            <div class="ep-bc-row">
              <div class="ep-bi"><span>性别</span><b>{{ empInfo.gender === 2 ? '女' : '男' }}</b></div>
              <div class="ep-bi"><span>年龄</span><b>{{ calcAge(empInfo.birthDate) }}</b></div>
              <div class="ep-bi"><span>部门</span><b>{{ empInfo.deptName || '--' }}</b></div>
              <div class="ep-bi"><span>岗位</span><b>{{ empInfo.jobTypeName || '--' }}</b></div>
            </div>
            <div class="ep-bc-code">工号：<code>{{ empInfo.empCode || '--' }}</code></div>
          </div>
        </div>

      </aside>

      <!-- ═══════ 中栏：矿工全宽 ═══════ -->
      <main class="ep-center">
        <div class="ep-miner-stage">
          <!-- HUD 角标 -->
          <div class="ep-miner-corner ep-mc-tl"></div>
          <div class="ep-miner-corner ep-mc-tr"></div>
          <div class="ep-miner-corner ep-mc-bl"></div>
          <div class="ep-miner-corner ep-mc-br"></div>
          <div class="ep-ring ep-ring1"></div>
          <div class="ep-ring ep-ring2"></div>
          <div class="ep-scan-overlay"></div>
          <img class="ep-miner" src="/assets/miner-worker.png" />
          <!-- 热区：数值常驻显示，异常时高亮 -->
          <div class="ep-hotspot ep-hs-heart"
            :class="{ 'has-data': !!vitals.heartRate, active: isHrAbnormal, danger: isHrDanger }">
            <span class="ep-hs-pulse"></span>
            <div class="ep-hs-data">
              <span class="ep-hs-val" :class="hrClass(vitals.heartRate)">{{ vitals.heartRate || '--' }}</span>
              <span class="ep-hs-u">bpm</span>
            </div>
            <span class="ep-hs-name">心率</span>
          </div>
          <div class="ep-hotspot ep-hs-lung"
            :class="{ 'has-data': !!vitals.bloodOxygen, active: isSpo2Abnormal, danger: isSpo2Danger }">
            <span class="ep-hs-pulse"></span>
            <div class="ep-hs-data">
              <span class="ep-hs-val" :class="spo2Class(vitals.bloodOxygen)">{{ vitals.bloodOxygen || '--' }}</span>
              <span class="ep-hs-u">%</span>
            </div>
            <span class="ep-hs-name">血氧</span>
          </div>
          <div class="ep-hotspot ep-hs-head"
            :class="{ 'has-data': !!vitals.temperature, active: isTempAbnormal || isPressureHigh, danger: isTempDanger }">
            <span class="ep-hs-pulse"></span>
            <div class="ep-hs-data">
              <span class="ep-hs-val" :class="tempClass(vitals.temperature)">{{ tempDisplayVal }}</span>
              <span class="ep-hs-u">°C</span>
            </div>
            <span class="ep-hs-name">体温</span>
          </div>
          <div class="ep-hotspot ep-hs-arm"
            :class="{ 'has-data': !!(vitals.systolic || vitals.diastolic), active: isBpAbnormal, danger: isBpDanger }">
            <span class="ep-hs-pulse"></span>
            <div class="ep-hs-data">
              <span class="ep-hs-val" :class="bpClass">{{ (vitals.systolic && vitals.diastolic) ? vitals.systolic + '/' + vitals.diastolic : '--' }}</span>
              <span class="ep-hs-u">mmHg</span>
            </div>
            <span class="ep-hs-name">血压</span>
          </div>
          <div class="ep-glow-base"></div>
        </div>
      </main>

      <!-- ═══════ 右栏 ═══════ -->
      <aside class="ep-right">

        <!-- 实时体征：SVG 圆弧仪表盘 -->
        <div class="ep-panel ep-vitals">
          <div class="ep-ph">
            <span class="ep-ph-bar"></span>实时体征
            <span class="ep-ph-link" @click="refresh">刷新</span>
          </div>
          <div class="ep-gauge-grid">
            <!-- 心率 -->
            <div class="ep-gauge-wrap">
              <svg viewBox="0 0 120 120" class="ep-gauge-svg">
                <circle cx="60" cy="60" r="45" fill="none" stroke="rgba(255,255,255,0.07)" stroke-width="7"
                  stroke-dasharray="211.9 70.7" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"/>
                <circle cx="60" cy="60" r="45" fill="none" :stroke="hrGaugeColor" stroke-width="7"
                  :stroke-dasharray="hrGaugeStroke" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"
                  style="transition:stroke-dasharray 0.8s ease"/>
                <text x="60" y="54" text-anchor="middle" fill="#e0f0ff" font-size="20" font-weight="700" font-family="monospace">{{ vitals.heartRate || '--' }}</text>
                <text x="60" y="69" text-anchor="middle" fill="#4a7090" font-size="10">bpm</text>
                <text x="60" y="84" text-anchor="middle" fill="#6a8aaa" font-size="9">心　率</text>
              </svg>
              <div v-if="isHrAbnormal" :class="['ep-gauge-tag', isHrDanger ? 'danger' : 'warning']">{{ isHrDanger ? '危险' : '偏高' }}</div>
            </div>
            <!-- 血氧 -->
            <div class="ep-gauge-wrap">
              <svg viewBox="0 0 120 120" class="ep-gauge-svg">
                <circle cx="60" cy="60" r="45" fill="none" stroke="rgba(255,255,255,0.07)" stroke-width="7"
                  stroke-dasharray="211.9 70.7" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"/>
                <circle cx="60" cy="60" r="45" fill="none" :stroke="spo2GaugeColor" stroke-width="7"
                  :stroke-dasharray="spo2GaugeStroke" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"
                  style="transition:stroke-dasharray 0.8s ease"/>
                <text x="60" y="54" text-anchor="middle" fill="#e0f0ff" font-size="20" font-weight="700" font-family="monospace">{{ vitals.bloodOxygen || '--' }}</text>
                <text x="60" y="69" text-anchor="middle" fill="#4a7090" font-size="10">%</text>
                <text x="60" y="84" text-anchor="middle" fill="#6a8aaa" font-size="9">血　氧</text>
              </svg>
              <div v-if="isSpo2Abnormal" :class="['ep-gauge-tag', isSpo2Danger ? 'danger' : 'warning']">{{ isSpo2Danger ? '危险' : '偏低' }}</div>
            </div>
            <!-- 体温 -->
            <div class="ep-gauge-wrap">
              <svg viewBox="0 0 120 120" class="ep-gauge-svg">
                <circle cx="60" cy="60" r="45" fill="none" stroke="rgba(255,255,255,0.07)" stroke-width="7"
                  stroke-dasharray="211.9 70.7" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"/>
                <circle cx="60" cy="60" r="45" fill="none" :stroke="tempGaugeColor" stroke-width="7"
                  :stroke-dasharray="tempGaugeStroke" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"
                  style="transition:stroke-dasharray 0.8s ease"/>
                <text x="60" y="54" text-anchor="middle" fill="#e0f0ff" font-size="20" font-weight="700" font-family="monospace">{{ tempDisplayVal }}</text>
                <text x="60" y="69" text-anchor="middle" fill="#4a7090" font-size="10">°C</text>
                <text x="60" y="84" text-anchor="middle" fill="#6a8aaa" font-size="9">体　温</text>
              </svg>
              <div v-if="isTempAbnormal" :class="['ep-gauge-tag', isTempDanger ? 'danger' : 'warning']">{{ isTempDanger ? '危险' : '偏高' }}</div>
            </div>
            <!-- 血压 -->
            <div class="ep-gauge-wrap">
              <svg viewBox="0 0 120 120" class="ep-gauge-svg">
                <circle cx="60" cy="60" r="45" fill="none" stroke="rgba(255,255,255,0.07)" stroke-width="7"
                  stroke-dasharray="211.9 70.7" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"/>
                <circle cx="60" cy="60" r="45" fill="none" :stroke="bpGaugeColor" stroke-width="7"
                  :stroke-dasharray="bpGaugeStroke" :stroke-dashoffset="GAUGE_OFFSET" stroke-linecap="round"
                  style="transition:stroke-dasharray 0.8s ease"/>
                <text x="60" y="50" text-anchor="middle" fill="#e0f0ff" font-size="14" font-weight="700" font-family="monospace">{{ vitals.systolic || '--' }}</text>
                <text x="60" y="62" text-anchor="middle" fill="rgba(255,255,255,0.18)" font-size="9">─────</text>
                <text x="60" y="74" text-anchor="middle" fill="#a0c0e8" font-size="13" font-family="monospace">{{ vitals.diastolic || '--' }}</text>
                <text x="60" y="87" text-anchor="middle" fill="#6a8aaa" font-size="9">血　压</text>
              </svg>
              <div v-if="isBpAbnormal" :class="['ep-gauge-tag', isBpDanger ? 'danger' : 'warning']">{{ isBpDanger ? '危险' : '偏高' }}</div>
            </div>
          </div>
          <!-- 运动数据条 -->
          <div class="ep-exercise-strip">
            <div class="ep-ex-item">
              <span class="ep-ex-label">今日步数</span>
              <span class="ep-ex-val cyan">{{ exercise.todaySteps > 0 ? exercise.todaySteps.toLocaleString() : '--' }}</span>
              <div class="ep-ex-bar"><div class="ep-ex-fill steps-fill" :style="{width: Math.min(100,(exercise.todaySteps||0)/100)+'%'}"></div></div>
              <span class="ep-ex-target">/ 10,000步</span>
            </div>
            <div class="ep-ex-item">
              <span class="ep-ex-label">今日消耗</span>
              <span class="ep-ex-val cyan">{{ exercise.todayCalories > 0 ? exercise.todayCalories : '--' }}</span>
              <div class="ep-ex-bar"><div class="ep-ex-fill cals-fill" :style="{width: Math.min(100,(exercise.todayCalories||0)/20)+'%'}"></div></div>
              <span class="ep-ex-target">/ 2,000kcal</span>
            </div>
          </div>
          <div class="ep-update-time">更新于：{{ lastUpdate }}</div>
        </div>

        <!-- ECG实时波形（紧接体征下方） -->
        <div class="ep-panel ep-ecg">
          <div class="ep-ph"><span class="ep-ph-bar"></span>实时心电图</div>
          <HeartRateWave
            v-if="vitals.heartRate"
            :width="340" :height="90"
            :heartRate="vitals.heartRate"
            waveColor="#00ff88" :speed="2" :showWarning="false"
          />
          <div v-else class="ep-no-ecg">暂无心电数据</div>
        </div>

        <!-- 健康风险评估 -->
        <div class="ep-panel ep-risk">
          <div class="ep-ph"><span class="ep-ph-bar"></span>健康风险评估</div>
          <div class="ep-risk-grid">
            <div v-for="r in riskItems" :key="r.name" :class="['ep-risk-card', r.level]">
              <div class="ep-risk-icon">{{ r.icon }}</div>
              <div class="ep-risk-body">
                <div class="ep-risk-name">{{ r.name }}</div>
                <div class="ep-risk-bar-wrap">
                  <div class="ep-risk-bar-track">
                    <div class="ep-risk-bar-fill" :style="{ width: r.pct + '%', background: r.color }"></div>
                  </div>
                  <span :class="['ep-risk-label', r.level]">{{ r.levelText }}</span>
                  <span class="ep-risk-pct">{{ r.pct }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </aside>
    </div>

    <section class="ep-panel ep-warning-band">
      <div class="ep-ph">
        <span class="ep-ph-bar"></span>近期预警轨迹
        <span class="ep-warn-count" v-if="warnings.length">({{ warnings.length }})</span>
      </div>
      <div class="ep-warning-band__body">
        <div class="ep-warning-band__meta">
          <div class="ep-warn-summary">
            <div v-for="item in recentWarningSummary" :key="item.key" :class="['ep-warn-pill', `tone-${item.tone}`]">
              <span class="ep-warn-pill-label">{{ item.label }}</span>
              <strong class="ep-warn-pill-value">{{ item.value }}</strong>
            </div>
          </div>
          <div class="ep-warn-footer">
            <span class="ep-warn-footnote">{{ recentWarningFootnote }}</span>
            <button type="button" class="ep-warn-more" @click="goWarningCenter">查看全部</button>
          </div>
        </div>
        <div class="ep-warning-band__list">
          <div v-if="warnings.length === 0" class="ep-empty-warn">暂无预警记录</div>
          <div v-else class="ep-warn-list">
            <div v-for="(w, idx) in recentWarnings" :key="(w.id || w.time) + '_' + idx" class="ep-warn-item">
              <span :class="['ep-wdot', w.handled ? 'done' : 'pend']"></span>
              <div class="ep-warn-copy">
                <span class="ep-wtype">{{ w.warningType || w.type || '--' }}</span>
                <span class="ep-wtime">{{ fmtTime(w.createTime || w.time) }}</span>
              </div>
              <span :class="['ep-wst', w.handled ? 'done' : 'pend']">{{ w.handled ? '已处理' : '未处理' }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import MetricStrip from '@/components/health-shell/MetricStrip.vue'
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import HeartRateWave from '@/components/HeartRateWave.vue'
import { useEmployeeProfilePage } from './use-employee-profile-page'

const {
  aiReportContent,
  aiReportHtml,
  aiReportLoading,
  aiReportVisible,
  calcAge,
  empInfo,
  exercise,
  fmtTime,
  fmtTemp,
  goArchive,
  goMineEntry,
  goReportCenter,
  goWorkbench,
  hrClass,
  hrPct,
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
  openAiReport,
  pendCount,
  printAiReport,
  profileInsightLines,
  profileSummaryCards,
  pressClass,
  recentWarningFootnote,
  recentWarnings,
  recentWarningSummary,
  refresh,
  riskItems,
  spo2Class,
  spo2Pct,
  spo2Items,
  tempClass,
  tempPct,
  trendStats,
  trendRef,
  vitalItems,
  vitals,
  warnings,
  warnItems,
  goWarningCenter
} = useEmployeeProfilePage()

const bpClass = computed(() => {
  if (isBpDanger.value) return 'red'
  if (isBpAbnormal.value) return 'yellow'
  return vitals.value.systolic ? 'green' : ''
})

const bpBarPct = (s) => {
  if (!s) return 0
  return Math.min(100, Math.max(0, ((s - 90) / 90) * 100))
}

// ── 状态横幅 ──
const statusBannerTone = computed(() => {
  if (!vitals.value.heartRate) return 'idle'
  if (isBpDanger.value || isHrDanger.value || isTempDanger.value || isSpo2Danger.value) return 'danger'
  if (isBpAbnormal.value || isHrAbnormal.value || isTempAbnormal.value || isSpo2Abnormal.value) return 'warning'
  return 'safe'
})

const statusText = computed(() => {
  const t = statusBannerTone.value
  if (t === 'danger') return '⚠ 危险预警 · 立即关注'
  if (t === 'warning') return '体征异常 · 建议关注'
  if (t === 'safe') return '体征平稳'
  return '暂无实时数据'
})

const statusDetail = computed(() => {
  const alerts = []
  if (isHrDanger.value) alerts.push(`心率危险 ${vitals.value.heartRate} bpm`)
  else if (isHrAbnormal.value) alerts.push(`心率${vitals.value.heartRate > 100 ? '偏高' : '偏低'} ${vitals.value.heartRate} bpm`)
  if (isSpo2Danger.value) alerts.push(`血氧危险 ${vitals.value.bloodOxygen}%`)
  else if (isSpo2Abnormal.value) alerts.push(`血氧偏低 ${vitals.value.bloodOxygen}%`)
  if (isTempDanger.value) alerts.push(`体温危险 ${fmtTemp(vitals.value.temperature)}°C`)
  else if (isTempAbnormal.value) alerts.push(`体温偏${vitals.value.temperature > 37.3 ? '高' : '低'} ${fmtTemp(vitals.value.temperature)}°C`)
  if (isBpDanger.value) alerts.push(`血压危险 ${vitals.value.systolic}/${vitals.value.diastolic}`)
  else if (isBpAbnormal.value) alerts.push(`血压偏高 ${vitals.value.systolic}/${vitals.value.diastolic}`)
  if (alerts.length) return alerts.join(' · ')
  return vitals.value.heartRate ? '各项指标均在正常范围内' : '--'
})

// ── SVG 圆弧仪表盘 (r=45, 270° 弧, cx=cy=60, viewBox 0 0 120 120) ──
// 圆周 C = 2π*45 ≈ 282.7；270° 弧长 ≈ 211.9；dashoffset=-106 使缺口居中于底部
const GAUGE_CIRC = 282.7
const GAUGE_ARC  = 211.9
const GAUGE_OFFSET = -106

function gaugeStroke(pct) {
  const fill = Math.max(0, Math.min(100, pct)) / 100 * GAUGE_ARC
  return `${fill.toFixed(1)} ${(GAUGE_CIRC - fill).toFixed(1)}`
}

const hrGaugeStroke   = computed(() => gaugeStroke(vitals.value.heartRate ? (vitals.value.heartRate / 200) * 100 : 0))
const hrGaugeColor    = computed(() => isHrDanger.value ? '#ff4444' : isHrAbnormal.value ? '#ffaa00' : '#ff5252')

const spo2GaugeStroke = computed(() => gaugeStroke(vitals.value.bloodOxygen ? ((vitals.value.bloodOxygen - 80) / 20) * 100 : 0))
const spo2GaugeColor  = computed(() => isSpo2Danger.value ? '#ff4444' : isSpo2Abnormal.value ? '#ffaa00' : '#1890ff')

const tempGaugeStroke = computed(() => {
  const v = vitals.value.temperature
  if (!v) return gaugeStroke(0)
  const t = v > 100 ? v / 10 : v
  return gaugeStroke(((t - 34) / 8) * 100)
})
const tempGaugeColor  = computed(() => isTempDanger.value ? '#ff4444' : isTempAbnormal.value ? '#ffaa00' : '#ffaa00')
const tempDisplayVal  = computed(() => fmtTemp(vitals.value.temperature) || '--')

const bpGaugeStroke   = computed(() => gaugeStroke(vitals.value.systolic ? ((vitals.value.systolic - 80) / 100) * 100 : 0))
const bpGaugeColor    = computed(() => isBpDanger.value ? '#ff4444' : isBpAbnormal.value ? '#ffaa00' : '#e040fb')

// 7日趋势有无数据：avgHr 有值即认为有数据
const hasTrendData = computed(() => trendStats.value[0].value !== '--')

const summaryMetricItems = computed(() => profileSummaryCards.value.map((card) => ({
  key: card.label,
  label: card.label,
  value: card.value,
  note: card.sub,
  tone: card.tone === 'danger'
    ? 'danger'
    : card.tone === 'warn'
      ? 'warning'
      : card.tone === 'safe'
        ? 'success'
        : 'primary'
})))
</script>

<style scoped>
@import './employee-profile.scss';
</style>

