<template>
  <div class="portrait-root">
    <!-- ══ TOP BAR ══ -->
    <div class="top-bar">
      <div class="top-bar-left">
        <div class="avatar-badge">
          <span class="avatar-letter">{{ (portrait.empName || '?').charAt(0) }}</span>
        </div>
        <div class="person-meta">
          <div class="person-name">{{ portrait.empName || '--' }}</div>
          <div class="person-tags">
            <span class="ptag"><el-icon><Postcard /></el-icon>{{ portrait.empCode || '--' }}</span>
            <span class="ptag"><el-icon><OfficeBuilding /></el-icon>{{ portrait.deptName || '--' }}</span>
            <span class="ptag"><el-icon><Suitcase /></el-icon>{{ portrait.jobTypeName || '--' }}</span>
            <span v-if="portrait.gender" class="ptag dim">{{ portrait.gender === 1 ? '男' : '女' }}</span>
            <span v-if="portrait.bloodType" class="ptag dim">血型 {{ portrait.bloodType }}</span>
            <span v-if="portrait.height" class="ptag dim">{{ portrait.height }}cm</span>
            <span v-if="portrait.weight" class="ptag dim">{{ portrait.weight }}kg</span>
          </div>
        </div>
      </div>

      <div class="vitals-strip">
        <div class="vcard heart">
          <div class="vcard-icon">♥</div>
          <div class="vcard-info">
            <div class="vcard-val">{{ vitals.heartRate ?? '--' }}<span class="vcard-unit">bpm</span></div>
            <div class="vcard-label">心率</div>
          </div>
          <div class="vstatus" :class="vitalStatus(vitals.heartRate, 60, 100)">{{ vitalStatusText(vitals.heartRate, 60, 100) }}</div>
        </div>
        <div class="vcard oxygen">
          <div class="vcard-icon">💨</div>
          <div class="vcard-info">
            <div class="vcard-val">{{ vitals.bloodOxygen ?? '--' }}<span class="vcard-unit">%</span></div>
            <div class="vcard-label">血氧</div>
          </div>
          <div class="vstatus" :class="vitalStatus(vitals.bloodOxygen, 95, 100)">{{ vitalStatusText(vitals.bloodOxygen, 95, 100) }}</div>
        </div>
        <div class="vcard temp">
          <div class="vcard-icon">🌡</div>
          <div class="vcard-info">
            <div class="vcard-val">{{ vitals.temperature ?? '--' }}<span class="vcard-unit">°C</span></div>
            <div class="vcard-label">体温</div>
          </div>
          <div class="vstatus" :class="vitalStatus(vitals.temperature, 36, 37.3)">{{ vitalStatusText(vitals.temperature, 36, 37.3) }}</div>
        </div>
        <div class="vcard bp">
          <div class="vcard-icon">🫀</div>
          <div class="vcard-info">
            <div class="vcard-val">{{ vitals.systolic ?? '--' }}/{{ vitals.diastolic ?? '--' }}<span class="vcard-unit">mmHg</span></div>
            <div class="vcard-label">血压</div>
          </div>
          <div class="vstatus" :class="vitalStatus(vitals.systolic, 90, 140)">{{ vitalStatusText(vitals.systolic, 90, 140) }}</div>
        </div>
      </div>

      <div class="top-bar-right">
        <div class="clock-badge"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
        <el-button size="small" type="primary" @click="goRealtime"><el-icon><Monitor /></el-icon>实时监控</el-button>
        <el-button size="small" @click="router.back()"><el-icon><Back /></el-icon>返回</el-button>
      </div>
    </div>

    <div v-if="!empCode" class="empty-tip">
      <el-icon size="60" color="#2d3561"><UserFilled /></el-icon>
      <p>请从职工列表点击姓名进入健康画像</p>
      <el-button type="primary" @click="router.push('/personnel-management/employee')">前往职工列表</el-button>
    </div>

    <!-- 轮询刷新不触发白色遮罩，只有首次加载才显示 loading -->
    <div v-else v-loading="firstLoading" class="main-grid">

      <!-- LEFT: 趋势图 + 预警表 -->
      <div class="col-left">
        <div class="panel trend-panel">
          <div class="panel-hd"><span class="title-bar"></span>7天趋势</div>
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
        <div class="panel warn-panel">
          <div class="panel-hd">
            <span class="title-bar warn-bar"></span>近30天预警记录
            <span class="badge">共 {{ warnings.length }} 条</span>
          </div>
          <el-table :data="warnings" stripe style="width:100%" max-height="99999"
            :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'12px', padding:'7px 0' }"
            :row-style="{ background:'#1a1f3a' }"
            :cell-style="{ padding:'5px 0', fontSize:'12px' }">
            <el-table-column prop="createTime" label="时间" min-width="110" :formatter="fmtTime" />
            <el-table-column prop="warningType" label="类型" min-width="88" />
            <el-table-column prop="indicatorName" label="指标" width="72" align="center" />
            <el-table-column prop="warningValue" label="数值" width="72" align="center" />
            <el-table-column label="级别" width="60" align="center">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.warningLevel)" size="small" effect="dark">{{ row.warningLevel || '--' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- CENTER: 评分面板（填满） -->
      <div class="col-center">
        <div class="panel center-panel">
          <div class="panel-hd"><span class="title-bar radar-bar"></span>健康评分</div>

          <!-- ① 综合等级卡 -->
          <div class="grade-card" :class="gradeInfo.cls">
            <div class="grade-letter">{{ gradeInfo.grade }}</div>
            <div class="grade-right">
              <div class="grade-score">{{ gradeInfo.score }}<span class="grade-unit">/ 100</span></div>
              <div class="grade-label">{{ gradeInfo.label }}</div>
            </div>
          </div>

          <!-- ② 雷达图 -->
          <div ref="radarChartRef" class="radar-chart"></div>

          <!-- ③ 评分进度条 -->
          <div class="score-pills">
            <div v-for="(item, key) in scorePills" :key="key" class="pill">
              <div class="pill-label">{{ item.label }}</div>
              <div class="pill-bar"><div class="pill-fill" :style="{ width: item.value + '%', background: item.color }"></div></div>
              <div class="pill-val" :style="{ color: item.color }">{{ item.value }}</div>
            </div>
          </div>

          <!-- ④ 体征状态徽章 -->
          <div class="status-row">
            <div class="status-badge" :class="vitalStatus(vitals.heartRate, 60, 100)"><span class="sb-dot"></span>心率</div>
            <div class="status-badge" :class="vitalStatus(vitals.bloodOxygen, 95, 100)"><span class="sb-dot"></span>血氧</div>
            <div class="status-badge" :class="vitalStatus(vitals.temperature, 36, 37.3)"><span class="sb-dot"></span>体温</div>
            <div class="status-badge" :class="vitalStatus(vitals.systolic, 90, 140)"><span class="sb-dot"></span>血压</div>
            <div class="status-badge" :class="pressureStatus(vitals.pressure)"><span class="sb-dot"></span>压力</div>
          </div>

          <!-- ⑤ 建议复查 -->
          <div class="recheck-tip">
            📅 <span>建议复查：<strong>{{ nextCheckDate }}</strong></span>
          </div>

          <!-- ⑥ 本周关键统计 2×2 -->
          <div class="section-label">本周健康统计</div>
          <div class="stats-grid">
            <div class="stat-card" :class="weekStats.minOxygen < 90 ? 'warn' : 'ok'">
              <div class="stat-label">最低血氧</div>
              <div class="stat-val">{{ weekStats.minOxygen || '--' }}<span>%</span></div>
              <div class="stat-sub">{{ weekStats.minOxygen ? (weekStats.minOxygen < 95 ? '低于正常值' : '正常范围') : '暂无数据' }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">平均心率</div>
              <div class="stat-val" style="color:#ff7070">{{ weekStats.avgHr || '--' }}<span>bpm</span></div>
              <div class="stat-sub">{{ weekStats.avgHr ? (weekStats.avgHr >= 60 && weekStats.avgHr <= 100 ? '正常范围内' : '需关注') : '暂无数据' }}</div>
            </div>
            <div class="stat-card ok">
              <div class="stat-label">平均体温</div>
              <div class="stat-val">{{ weekStats.avgTemp || '--' }}<span>°C</span></div>
              <div class="stat-sub">{{ weekStats.avgTemp ? '维持正常区间' : '暂无数据' }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">预警次数</div>
              <div class="stat-val" style="color:#ffd200">{{ warnings.length }}<span>次</span></div>
              <div class="stat-sub">近30天累计</div>
            </div>
          </div>

          <!-- ⑦ 风险等级分布 -->
          <div class="section-label">风险等级分布</div>
          <div class="risk-strip">
            <div v-for="item in riskLevels" :key="item.name" class="risk-row">
              <div class="risk-name">{{ item.name }}</div>
              <div class="risk-bar-wrap">
                <div class="risk-bar-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
              </div>
              <div class="risk-lv" :style="{ color: item.color }">{{ item.label }}</div>
            </div>
          </div>

          <!-- ⑧ 心率时段热力图 -->
          <div class="section-label">今日心率时段分布</div>
          <div class="heatmap-wrap">
            <div class="heat-row">
              <div v-for="(cell, i) in heatmapCells.slice(0, 12)" :key="i"
                class="heat-cell" :style="{ background: cell.color }" :title="cell.label"></div>
            </div>
            <div class="heat-row">
              <div v-for="(cell, i) in heatmapCells.slice(12)" :key="i+12"
                class="heat-cell" :style="{ background: cell.color }" :title="cell.label"></div>
            </div>
            <div class="heat-axis">
              <span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>23:00</span>
            </div>
            <div class="heat-legend">
              <span class="hl-dot" style="background:#1a2a4d"></span>低
              <span class="hl-dot" style="background:#1565c0;margin-left:6px"></span>正常
              <span class="hl-dot" style="background:#ff5252;margin-left:6px"></span>偏高
            </div>
          </div>

          <div style="flex:1"></div>
        </div>
      </div>

      <!-- RIGHT: AI 报告 -->
      <div class="col-right">
        <div class="panel ai-panel">
          <div class="panel-hd ai-hd">
            <div class="ai-hd-left">
              <span class="title-bar ai-bar"></span>
              <span>AI 健康分析报告</span>
              <span v-if="aiReport.generateTime" class="ai-ts">· 生成于 {{ aiReport.generateTime }}</span>
            </div>
            <div class="ai-hd-right">
              <el-button type="primary" size="small" :loading="aiLoading" @click="handleGenerateReport(false)">
                {{ aiReport.content ? '刷新报告' : '生成 AI 分析' }}
              </el-button>
              <el-button v-if="aiReport.content" size="small" plain :loading="aiLoading" @click="handleGenerateReport(true)">重新生成</el-button>
            </div>
          </div>
          <div v-if="aiLoading" class="ai-loading">
            <div class="ai-dot"></div>
            <span>DeepSeek AI 正在分析，请稍候（约15-30秒）…</span>
          </div>
          <div v-else-if="!aiReport.content" class="ai-empty">
            <div class="ai-bot">🤖</div>
            <p>点击「生成 AI 分析」，DeepSeek 将根据近30天健康数据生成专属分析报告</p>
          </div>
          <div v-else class="ai-content" v-html="renderedReport"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Timer, UserFilled, Back, Postcard, OfficeBuilding, Suitcase, Monitor } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getHealthPortrait } from '@/api/health-portrait'
import { getCachedAiReport, generateAiReport } from '@/api/ai'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

const currentTime = ref('')
const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}
updateTime()
const clockTimer = setInterval(updateTime, 1000)

const empCode = ref(route.query.empCode || '')
const loading = ref(false)
const firstLoading = ref(false)

const portrait = reactive({ empName: '', empCode: '', deptName: '', jobTypeName: '', gender: null, bloodType: '', height: null, weight: null })

function goRealtime() {
  router.push({
    path: '/health-monitor/employee-profile',
    query: {
      empCode: portrait.empCode, empName: portrait.empName,
      gender: portrait.gender ?? 1, deptName: portrait.deptName,
      jobTypeName: portrait.jobTypeName
    }
  })
}
const vitals = reactive({ heartRate: null, bloodOxygen: null, temperature: null, systolic: null, diastolic: null, pressure: null })
const trendData = ref({ dates: [], heartRates: [], bloodOxygens: [] })
const hourlyHrData = ref(new Array(24).fill(0))
const healthScores = ref({ heartRate: 0, bloodOxygen: 0, temperature: 0, bloodPressure: 0, pressure: 0, activity: 0 })
const warnings = ref([])

const trendChartRef = ref(null)
const radarChartRef = ref(null)
let trendChart = null
let radarChart = null
let pollTimer = null

const aiReport = reactive({ content: '', generateTime: '', expiresAt: '' })
const aiLoading = ref(false)

// ── 综合等级 ──
const gradeInfo = computed(() => {
  const s = healthScores.value
  const vals = [s.heartRate, s.bloodOxygen, s.temperature, s.bloodPressure, s.pressure].filter(v => v > 0)
  const avg = vals.length ? Math.round(vals.reduce((a, b) => a + b, 0) / vals.length) : 0
  if (avg >= 90) return { grade: 'A', score: avg, label: '健康状态优秀', cls: 'grade-a' }
  if (avg >= 75) return { grade: 'B', score: avg, label: '健康状态良好', cls: 'grade-b' }
  if (avg >= 60) return { grade: 'C', score: avg, label: '健康状态一般', cls: 'grade-c' }
  if (avg >  0)  return { grade: 'D', score: avg, label: '健康状态较差，需关注', cls: 'grade-d' }
  return { grade: '--', score: 0, label: '暂无评分数据', cls: 'grade-none' }
})

// ── 建议复查日期 ──
const nextCheckDate = computed(() => {
  const hasHigh = warnings.value.some(w => w.warningLevel === '高危' || w.warningLevel === '危急')
  const hasMid  = warnings.value.some(w => w.warningLevel === '中')
  const d = new Date()
  if (hasHigh) d.setDate(d.getDate() + 1)
  else if (hasMid) d.setDate(d.getDate() + 3)
  else d.setDate(d.getDate() + 7)
  return `${d.getMonth() + 1}月${d.getDate()}日`
})

// ── 本周健康统计（从 trendData 推算） ──
const weekStats = computed(() => {
  const hrs = trendData.value.heartRates || []
  const bos = trendData.value.bloodOxygens || []
  const temps = trendData.value.temperatures || []
  const avgHr   = hrs.length  ? +(hrs.reduce((a, b) => a + b, 0) / hrs.length).toFixed(1)   : null
  const minOxy  = bos.length  ? Math.min(...bos)  : null
  const avgTemp = temps.length ? +(temps.reduce((a, b) => a + b, 0) / temps.length).toFixed(1) : (vitals.temperature || null)
  return { avgHr, minOxygen: minOxy, avgTemp }
})

// ── 风险等级分布（基于 warnings 统计） ──
const riskLevels = computed(() => {
  const total = warnings.value.length || 1
  const byIndicator = {}
  warnings.value.forEach(w => {
    const k = w.indicatorName || w.warningType || '其他'
    if (!byIndicator[k]) byIndicator[k] = { high: 0, mid: 0, low: 0 }
    if (w.warningLevel === '高危' || w.warningLevel === '危急' || w.warningLevel === '高') byIndicator[k].high++
    else if (w.warningLevel === '中') byIndicator[k].mid++
    else byIndicator[k].low++
  })
  // 取前4个指标
  const keys = Object.keys(byIndicator).slice(0, 4)
  const colorMap = { high: 'linear-gradient(90deg,#ff5252,#ff8a65)', mid: 'linear-gradient(90deg,#ffd200,#ffe082)', low: 'linear-gradient(90deg,#38ef7d,#69f0ae)' }
  const labelMap = { high: '高', mid: '中', low: '低' }
  const textColorMap = { high: '#ff5252', mid: '#ffd200', low: '#38ef7d' }
  return keys.map(k => {
    const d = byIndicator[k]
    const dominant = d.high > 0 ? 'high' : d.mid > 0 ? 'mid' : 'low'
    const pct = Math.min(100, Math.round(((d.high * 3 + d.mid * 2 + d.low) / (total * 0.5)) * 100))
    return { name: k.slice(0, 3), pct: Math.max(8, pct), color: colorMap[dominant], label: labelMap[dominant], textColor: textColorMap[dominant] }
  })
})

// ── 心率时段热力图（今日每小时真实心率）──
const heatmapCells = computed(() => {
  return hourlyHrData.value.map((hr, h) => {
    const label = `${String(h).padStart(2,'0')}:00`
    if (!hr) return { color: '#1a2a4d', label }
    if (hr > 100 || hr < 55) return { color: '#ff5252', label: `${label} ${hr}bpm ⚠` }
    if (hr > 90) return { color: '#f97316', label: `${label} ${hr}bpm` }
    return { color: '#1565c0', label: `${label} ${hr}bpm` }
  })
})

// ── AI ──
const renderedReport = computed(() => {
  if (!aiReport.content) return ''
  return aiReport.content
    .replace(/^## (.+)$/gm, '<h4>$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
})

const scorePills = computed(() => {
  const s = healthScores.value
  return {
    heartRate:     { label: '心率',   value: s.heartRate    || 0, color: '#ff5252' },
    bloodOxygen:   { label: '血氧',   value: s.bloodOxygen  || 0, color: '#00d4ff' },
    temperature:   { label: '体温',   value: s.temperature  || 0, color: '#ffd200' },
    bloodPressure: { label: '血压',   value: s.bloodPressure|| 0, color: '#38ef7d' },
    pressure:      { label: '压力',   value: s.pressure     || 0, color: '#fb923c' },
    activity:      { label: '活动',   value: s.activity     || 0, color: '#764ba2' }
  }
})

const loadCachedReport = async () => {
  if (!empCode.value) return
  try {
    const res = await getCachedAiReport(empCode.value)
    if (res.code === 200 && res.data) {
      aiReport.content = res.data.reportContent
      aiReport.generateTime = res.data.generateTime
      aiReport.expiresAt = res.data.expiresAt
    }
  } catch (_) {}
}

const handleGenerateReport = async (force = false) => {
  if (aiLoading.value) return
  aiLoading.value = true
  try {
    const res = await generateAiReport(empCode.value, force)
    if (res.code === 200 && res.data) {
      aiReport.content = res.data.reportContent
      aiReport.generateTime = res.data.generateTime
      aiReport.expiresAt = res.data.expiresAt
      ElMessage.success('AI 报告生成成功')
    } else {
      ElMessage.error(res.message || 'AI 分析失败')
    }
  } catch (_) {} finally { aiLoading.value = false }
}

const fmtTime = (row, col, val) => {
  if (!val) return '--'
  const s = String(val).replace('T', ' ').replace(/\.\d+.*$/, '')
  return s.length >= 16 ? s.slice(5, 16) : s
}

const levelTagType = (level) => {
  if (level === '高危' || level === '危急' || level === '高') return 'danger'
  if (level === '中') return 'warning'
  return 'info'
}

const vitalStatus = (val, min, max) => {
  if (val == null) return 'unknown'
  return val >= min && val <= max ? 'normal' : 'abnormal'
}

const pressureStatus = (val) => {
  if (val == null) return 'unknown'
  return val < 70 ? 'normal' : 'abnormal'
}

const vitalStatusText = (val, min, max) => {
  if (val == null) return '--'
  return val >= min && val <= max ? '正常' : '异常'
}

const fetchPortrait = async (isFirstLoad = false) => {
  if (!empCode.value) return
  if (isFirstLoad) firstLoading.value = true
  loading.value = true
  try {
    const res = await getHealthPortrait(empCode.value)
    if (res.code === 200 && res.data) {
      const d = res.data
      Object.assign(portrait, {
        empName: d.empName || d.employee?.empName || '',
        empCode: d.empCode || d.employee?.empCode || empCode.value,
        deptName: d.deptName || d.employee?.deptName || '',
        jobTypeName: d.jobTypeName || d.employee?.jobTypeName || '',
        gender: d.gender ?? d.employee?.gender ?? null,
        bloodType: d.bloodType || d.employee?.bloodType || '',
        height: d.height ?? d.employee?.height ?? null,
        weight: d.weight ?? d.employee?.weight ?? null
      })
      if (d.vitals || d.realtime) {
        const v = d.vitals || d.realtime
        Object.assign(vitals, {
          heartRate: v.heartRate ?? v.heart_rate ?? null,
          bloodOxygen: v.bloodOxygen ?? v.blood_oxygen ?? null,
          temperature: v.temperature ?? null,
          systolic: v.systolic ?? v.sbp ?? null,
          diastolic: v.diastolic ?? v.dbp ?? null,
          pressure: v.pressure ?? null
        })
      }
      if (d.trend) trendData.value = d.trend
      if (d.hourlyHr) hourlyHrData.value = d.hourlyHr
      if (d.healthScores) {
        healthScores.value = d.healthScores
      } else {
        const hr = vitals.heartRate, spo = vitals.bloodOxygen, tmp = vitals.temperature
        const sbp = vitals.systolic, dbp = vitals.diastolic
        const prs = vitals.pressure
        healthScores.value = {
          heartRate:    hr  ? (hr  >= 60  && hr  <= 100 ? 90 : hr  >= 50 && hr  <= 110 ? 70 : 50) : 0,
          bloodOxygen:  spo ? (spo >= 95  ? 95 : spo >= 90 ? 65 : 40) : 0,
          temperature:  tmp ? (tmp >= 36.0 && tmp <= 37.5 ? 90 : tmp >= 35.5 && tmp <= 38.0 ? 65 : 50) : 0,
          bloodPressure: (sbp && dbp) ? (sbp <= 135 && dbp <= 85 ? 85 : sbp <= 145 && dbp <= 95 ? 65 : 50) : 0,
          pressure: prs != null ? (prs < 70 ? 90 : prs < 85 ? 65 : 40) : 0,
          activity: (() => {
            const steps = d.exercise?.todaySteps ?? 0
            if (steps >= 10000) return 90
            if (steps >= 5000)  return 65
            if (steps >= 2000)  return 45
            return steps > 0 ? 30 : 0
          })()
        }
      }
      warnings.value = d.warnings || d.recentWarnings || []
      await nextTick()
      initTrendChart()
      initRadarChart()
    }
  } catch (e) {
    if (isFirstLoad) ElMessage.error('加载健康画像失败')
  } finally {
    loading.value = false
    firstLoading.value = false
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const dates = trendData.value.dates || []
  const hrs = trendData.value.heartRates || []
  const bos = trendData.value.bloodOxygens || []
  const hasData = dates.length > 0
  if (!hasData) {
    for (let i = 6; i >= 0; i--) {
      const d = new Date(); d.setDate(d.getDate() - i)
      dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
    }
  }
  trendChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(20,24,48,.95)', borderColor: '#2d3561', textStyle: { color: '#c8d8e8', fontSize: 12 } },
    legend: { data: ['心率', '血氧'], bottom: 2, textStyle: { color: '#7eb8d4', fontSize: 11 } },
    grid: { top: 16, right: 52, bottom: 38, left: 52 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#232b4d' } }, axisLabel: { color: '#7eb8d4', fontSize: 10 } },
    yAxis: [
      { type: 'value', name: '心率', min: 40, max: 140, nameTextStyle: { color: '#7eb8d4', fontSize: 10 }, axisLine: { lineStyle: { color: '#232b4d' } }, splitLine: { lineStyle: { color: '#232b4d' } }, axisLabel: { color: '#7eb8d4', fontSize: 10 } },
      { type: 'value', name: '血氧%', min: 85, max: 100, nameTextStyle: { color: '#7eb8d4', fontSize: 10 }, axisLine: { lineStyle: { color: '#232b4d' } }, splitLine: { show: false }, axisLabel: { color: '#7eb8d4', fontSize: 10 } }
    ],
    series: [
      { name: '心率', type: 'line', data: hasData ? hrs : [], smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { width: 2 }, itemStyle: { color: '#ff5252' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(255,82,82,.3)' }, { offset: 1, color: 'rgba(255,82,82,0)' }]) } },
      { name: '血氧', type: 'line', yAxisIndex: 1, data: hasData ? bos : [], smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { width: 2 }, itemStyle: { color: '#00d4ff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0,212,255,.3)' }, { offset: 1, color: 'rgba(0,212,255,0)' }]) } }
    ]
  })
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const scores = healthScores.value
  radarChart.setOption({
    backgroundColor: 'transparent',
    radar: {
      center: ['50%', '50%'], radius: '68%',
      indicator: [
        { name: '心率', max: 100 }, { name: '血氧', max: 100 },
        { name: '活动', max: 100 }, { name: '血压', max: 100 },
        { name: '体温', max: 100 }, { name: '压力', max: 100 }
      ],
      axisName: { color: '#7eb8d4', fontSize: 11 },
      axisLine: { lineStyle: { color: '#232b4d' } },
      splitLine: { lineStyle: { color: '#232b4d' } },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,.02)', 'rgba(0,212,255,.05)'] } }
    },
    series: [{ type: 'radar', data: [{ value: [scores.heartRate||0, scores.bloodOxygen||0, scores.activity||0, scores.bloodPressure||0, scores.temperature||0, scores.pressure||0], name: '健康评分', areaStyle: { color: 'rgba(0,212,255,.18)' }, lineStyle: { color: '#00d4ff', width: 2 }, itemStyle: { color: '#00d4ff' } }] }]
  })
}

const handleResize = () => { trendChart?.resize(); radarChart?.resize() }

onMounted(() => {
  fetchPortrait(true)
  loadCachedReport()
  window.addEventListener('resize', handleResize)
  pollTimer = setInterval(() => fetchPortrait(false), 10000)
})

onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(pollTimer)
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  radarChart?.dispose()
})
</script>

<style scoped lang="scss">
@import '@/styles/dark-admin.scss';

.portrait-root {
  height: calc(100vh - 50px);
  display: flex; flex-direction: column;
  background: #0a0e27;
  padding: 10px 14px;
  box-sizing: border-box;
  overflow: hidden;
  gap: 10px;
  color: #c8d8e8;
}

/* TOP BAR */
.top-bar {
  display: flex; align-items: center; gap: 12px;
  background: linear-gradient(135deg, #0d1b4b 0%, #1a2a6c 50%, #0d1b4b 100%);
  border: 1px solid #2a3f7a; border-radius: 10px;
  padding: 10px 18px; flex-shrink: 0; min-height: 72px;
}
.top-bar-left { display: flex; align-items: center; gap: 12px; flex-shrink: 0; min-width: 300px; }
.avatar-badge {
  width: 46px; height: 46px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700; color: #fff;
  border: 2px solid rgba(0,212,255,.35); flex-shrink: 0;
}
.person-meta { display: flex; flex-direction: column; gap: 5px; }
.person-name { font-size: 17px; font-weight: 700; color: #e8f4ff; letter-spacing: 1px; }
.person-tags { display: flex; flex-wrap: wrap; gap: 5px; }
.ptag {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 11px; color: #c8d8e8;
  background: rgba(0,212,255,.07); border: 1px solid rgba(0,212,255,.15);
  padding: 2px 7px; border-radius: 4px;
  &.dim { color: #7eb8d4; background: rgba(255,255,255,.04); border-color: #232b4d; }
}
.vitals-strip { display: flex; gap: 8px; flex: 1; justify-content: center; }
.vcard {
  display: flex; align-items: center; gap: 9px;
  background: rgba(20,24,48,.85); border: 1px solid #232b4d;
  border-radius: 8px; padding: 8px 12px; flex: 1; max-width: 190px;
  transition: box-shadow .2s;
  &:hover { box-shadow: 0 4px 16px rgba(0,212,255,.18); }
  &.heart { border-left: 3px solid #ff5252; }
  &.oxygen { border-left: 3px solid #00d4ff; }
  &.temp { border-left: 3px solid #ffd200; }
  &.bp { border-left: 3px solid #38ef7d; }
}
.vcard-icon { font-size: 20px; flex-shrink: 0; }
.vcard-info { flex: 1; }
.vcard-val { font-size: 17px; font-weight: 700; color: #e8f4ff; }
.vcard-unit { font-size: 10px; font-weight: 400; color: #7eb8d4; margin-left: 2px; }
.vcard-label { font-size: 10px; color: #7eb8d4; margin-top: 1px; }
.vstatus {
  font-size: 10px; font-weight: 600; padding: 2px 6px; border-radius: 8px; white-space: nowrap;
  &.normal { color: #38ef7d; background: rgba(56,239,125,.1); }
  &.abnormal { color: #ff5252; background: rgba(255,82,82,.1); }
  &.unknown { color: #7eb8d4; background: rgba(126,184,212,.06); }
}
.top-bar-right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.clock-badge {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: #7eb8d4;
  background: rgba(0,212,255,.06); border: 1px solid rgba(0,212,255,.18);
  padding: 4px 10px; border-radius: 14px; white-space: nowrap;
}

/* MAIN GRID */
.main-grid {
  display: grid; grid-template-columns: 38fr 20fr 42fr;
  gap: 10px; flex: 1; min-height: 0; overflow: hidden;
}
.col-left, .col-center, .col-right {
  display: flex; flex-direction: column; gap: 10px; min-height: 0; overflow: hidden;
}

/* PANEL */
.panel {
  background: #141830; border: 1px solid #232b4d;
  border-radius: 10px; display: flex; flex-direction: column; overflow: hidden;
}
.panel-hd {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; font-size: 13px; font-weight: 600; color: #c8d8e8;
  border-bottom: 1px solid #232b4d; flex-shrink: 0;
}
.title-bar { display: inline-block; width: 3px; height: 14px; background: #00d4ff; border-radius: 2px; flex-shrink: 0; }
.warn-bar { background: #ffd200; }
.radar-bar { background: linear-gradient(135deg, #667eea, #764ba2); }
.ai-bar { background: linear-gradient(135deg, #667eea, #764ba2); }
.badge {
  margin-left: auto; font-size: 11px; color: #7eb8d4;
  background: rgba(0,212,255,.06); border: 1px solid rgba(0,212,255,.14);
  padding: 2px 9px; border-radius: 9px;
}

/* LEFT COL */
.trend-panel { flex: 0 0 53%; min-height: 0; }
.trend-chart { flex: 1; width: 100%; min-height: 0; padding: 6px 8px; }
.warn-panel { flex: 1; min-height: 0; overflow: hidden; }
.warn-panel :deep(.el-table) {
  background: transparent; color: #c8d8e8;
  --el-table-border-color: #232b4d;
  --el-table-row-hover-bg-color: #1e2545;
  th.el-table__cell { border-bottom-color: #232b4d !important; }
  td.el-table__cell { border-bottom-color: #1e2545 !important; }
  .el-table__row--striped td { background: #171d38 !important; }
  tr:hover > td { background: #1e2545 !important; }
}

/* CENTER COL */
.center-panel {
  flex: 1; min-height: 0;
  overflow-y: auto;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: #232b4d; border-radius: 2px; }
}

/* ① 等级卡 */
.grade-card {
  display: flex; align-items: center; gap: 12px;
  margin: 10px 12px 0; padding: 10px 14px;
  border-radius: 8px; border: 1px solid #232b4d; flex-shrink: 0;
  &.grade-a { background: rgba(56,239,125,.08); border-color: rgba(56,239,125,.3); .grade-letter { color: #38ef7d; border-color: rgba(56,239,125,.4); } }
  &.grade-b { background: rgba(0,212,255,.07); border-color: rgba(0,212,255,.25); .grade-letter { color: #00d4ff; border-color: rgba(0,212,255,.4); } }
  &.grade-c { background: rgba(255,210,0,.07); border-color: rgba(255,210,0,.25); .grade-letter { color: #ffd200; border-color: rgba(255,210,0,.4); } }
  &.grade-d { background: rgba(255,82,82,.07); border-color: rgba(255,82,82,.25); .grade-letter { color: #ff5252; border-color: rgba(255,82,82,.4); } }
  &.grade-none { background: rgba(126,184,212,.05); .grade-letter { color: #7eb8d4; } }
}
.grade-letter {
  width: 50px; height: 50px; border-radius: 10px; border: 2px solid #232b4d;
  font-size: 32px; font-weight: 900; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-family: 'Arial Black', Arial, sans-serif;
}
.grade-right { flex: 1; }
.grade-score { font-size: 20px; font-weight: 700; color: #e8f4ff; }
.grade-unit { font-size: 10px; color: #7eb8d4; margin-left: 3px; }
.grade-label { font-size: 11px; color: #7eb8d4; margin-top: 2px; }

/* ② 雷达图 */
.radar-chart { height: 170px; flex-shrink: 0; padding: 4px 6px; }

/* ③ 评分条 */
.score-pills { padding: 4px 14px 4px; display: flex; flex-direction: column; gap: 5px; flex-shrink: 0; }
.pill { display: flex; align-items: center; gap: 8px; }
.pill-label { font-size: 11px; color: #7eb8d4; width: 28px; flex-shrink: 0; }
.pill-bar { flex: 1; height: 5px; background: #232b4d; border-radius: 3px; overflow: hidden; }
.pill-fill { height: 100%; border-radius: 3px; transition: width .6s ease; }
.pill-val { font-size: 11px; font-weight: 600; width: 24px; text-align: right; flex-shrink: 0; }

/* ④ 状态徽章 */
.status-row { display: flex; gap: 5px; padding: 5px 12px; flex-shrink: 0; border-top: 1px solid #1e2545; }
.status-badge {
  display: flex; align-items: center; gap: 4px; font-size: 10px;
  padding: 3px 6px; border-radius: 10px; flex: 1; justify-content: center;
  &.normal { color: #38ef7d; background: rgba(56,239,125,.1); border: 1px solid rgba(56,239,125,.2); }
  &.abnormal { color: #ff5252; background: rgba(255,82,82,.1); border: 1px solid rgba(255,82,82,.2); }
  &.unknown { color: #7eb8d4; background: rgba(126,184,212,.07); border: 1px solid #232b4d; }
}
.sb-dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; flex-shrink: 0; }

/* ⑤ 复查提示 */
.recheck-tip {
  display: flex; align-items: center; gap: 7px;
  padding: 4px 14px 4px; font-size: 11px; color: #7eb8d4; flex-shrink: 0;
  strong { color: #00d4ff; }
}

/* 通用小节标题 */
.section-label {
  font-size: 10px; font-weight: 600; color: #7eb8d4;
  padding: 5px 14px 0; flex-shrink: 0; letter-spacing: 0.5px;
  border-top: 1px solid #1e2545;
}

/* ⑥ 统计卡 2×2 */
.stats-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 6px;
  padding: 5px 12px 0; flex-shrink: 0;
}
.stat-card {
  background: #1a1f3a; border: 1px solid #232b4d; border-radius: 7px;
  padding: 7px 10px; display: flex; flex-direction: column; gap: 2px;
  &.warn { border-color: rgba(255,82,82,.3); }
  &.ok  { border-color: rgba(56,239,125,.2); }
}
.stat-label { font-size: 10px; color: #7eb8d4; }
.stat-val { font-size: 15px; font-weight: 700; color: #e8f4ff; span { font-size: 9px; font-weight: 400; color: #7eb8d4; margin-left: 2px; } }
.stat-sub { font-size: 9px; color: #4a5578; }
.stat-card.warn .stat-val { color: #ff5252; }
.stat-card.ok  .stat-val { color: #38ef7d; }

/* ⑦ 风险分布 */
.risk-strip { display: flex; flex-direction: column; gap: 5px; padding: 5px 12px 0; flex-shrink: 0; }
.risk-row { display: flex; align-items: center; gap: 8px; }
.risk-name { font-size: 10px; color: #7eb8d4; width: 30px; flex-shrink: 0; }
.risk-bar-wrap { flex: 1; height: 6px; background: #1a1f3a; border-radius: 3px; overflow: hidden; }
.risk-bar-fill { height: 100%; border-radius: 3px; }
.risk-lv { font-size: 10px; font-weight: 600; width: 16px; text-align: right; flex-shrink: 0; }

/* ⑧ 热力图 */
.heatmap-wrap { padding: 5px 12px 0; flex-shrink: 0; }
.heat-row { display: flex; gap: 3px; margin-bottom: 3px; }
.heat-cell { width: 14px; height: 13px; border-radius: 2px; flex-shrink: 0; cursor: default; transition: opacity .2s; &:hover { opacity: .75; } }
.heat-axis { display: flex; justify-content: space-between; font-size: 9px; color: #4a5578; margin-top: 3px; }
.heat-legend { display: flex; align-items: center; gap: 3px; font-size: 9px; color: #4a5578; margin-top: 5px; }
.hl-dot { width: 8px; height: 8px; border-radius: 2px; display: inline-block; }

/* RIGHT COL */
.ai-panel { flex: 1; }
.ai-hd { flex-wrap: wrap; }
.ai-hd-left { display: flex; align-items: center; gap: 6px; flex: 1; }
.ai-hd-right { display: flex; align-items: center; gap: 6px; }
.ai-ts { font-size: 10px; color: #7eb8d4; }
.ai-loading { display: flex; align-items: center; gap: 10px; padding: 30px 20px; color: #7eb8d4; font-size: 12px; }
.ai-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #667eea; flex-shrink: 0;
  animation: ai-pulse 1.2s ease-in-out infinite;
}
@keyframes ai-pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:.4;transform:scale(.7)} }
.ai-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 30px 20px; gap: 8px; color: #7eb8d4;
  .ai-bot { font-size: 36px; }
  p { margin: 0; font-size: 12px; text-align: center; max-width: 300px; line-height: 1.6; }
}
.ai-content {
  padding: 12px 16px; font-size: 13px; color: #c8d8e8; line-height: 1.8;
  overflow-y: auto; flex: 1; min-height: 0;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: #0a0e27; }
  &::-webkit-scrollbar-thumb { background: #232b4d; border-radius: 2px; }
  :deep(h4) { font-size: 13px; font-weight: 700; color: #00d4ff; margin: 10px 0 4px; padding-left: 7px; border-left: 3px solid #00d4ff; }
  :deep(strong) { color: #e8f4ff; }
}

/* EMPTY */
.empty-tip {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; flex: 1; gap: 14px;
  p { color: #7eb8d4; font-size: 13px; margin: 0; }
}
</style>
