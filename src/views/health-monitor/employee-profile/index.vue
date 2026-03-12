<template>
  <div class="ep-page">
    <!-- 顶部标题 -->
    <div class="ep-header">
      <button class="ep-back" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
      <div class="ep-title">职工健康画像</div>
      <div class="ep-header-emp">
        <span class="ep-hname">{{ empInfo.empName }}</span>
        <span :class="['ep-online', isOnline ? 'on' : 'off']">
          <i class="ep-dot"></i>{{ isOnline ? '在线' : '离线' }}
        </span>
        <span class="ep-update">更新于：{{ lastUpdate }}</span>
      </div>
    </div>

    <!-- 主体三列 -->
    <div class="ep-body" v-loading="loading">

      <!-- ═══════ 左栏 ═══════ -->
      <aside class="ep-left">

        <!-- 基本信息 -->
        <div class="ep-panel ep-basic">
          <div class="ep-ph"><span class="ep-ph-bar"></span>职工基本信息</div>
          <div class="ep-basic-body">
            <div class="ep-basic-name">{{ empInfo.empName || '--' }}</div>
            <div class="ep-basic-grid">
              <div class="ep-bi"><span>性别</span><b>{{ empInfo.gender === 2 ? '女' : '男' }}</b></div>
              <div class="ep-bi"><span>年龄</span><b>{{ calcAge(empInfo.birthDate) }}</b></div>
              <div class="ep-bi"><span>部门</span><b>{{ empInfo.deptName || '--' }}</b></div>
              <div class="ep-bi"><span>岗位</span><b>{{ empInfo.jobTypeName || '--' }}</b></div>
              <div class="ep-bi full"><span>手机</span><b>{{ empInfo.phone || '--' }}</b></div>
              <div class="ep-bi full"><span>工号</span><b class="code">{{ empInfo.empCode || '--' }}</b></div>
            </div>
          </div>
        </div>

        <!-- 7天体征趋势 -->
        <div class="ep-panel ep-trend">
          <div class="ep-ph"><span class="ep-ph-bar"></span>7天体征趋势</div>
          <div ref="trendRef" class="ep-trend-chart"></div>
        </div>

        <!-- 近期预警 -->
        <div class="ep-panel ep-warns">
          <div class="ep-ph">
            <span class="ep-ph-bar"></span>近期预警
            <span class="ep-warn-count" v-if="warnings.length">({{ warnings.length }})</span>
          </div>
          <div v-if="warnings.length === 0" class="ep-empty-warn">暂无预警记录</div>
          <div class="ep-warn-list">
            <div v-for="w in warnings" :key="w.id || w.time" class="ep-warn-item">
              <span :class="['ep-wdot', w.handled ? 'done' : 'pend']"></span>
              <span class="ep-wtype">{{ w.warningType || w.type || '--' }}</span>
              <span class="ep-wtime">{{ fmtTime(w.createTime || w.time) }}</span>
              <span :class="['ep-wst', w.handled ? 'done' : 'pend']">{{ w.handled ? '已处理' : '未处理' }}</span>
            </div>
          </div>
        </div>

      </aside>

      <!-- ═══════ 中栏：左面板 | 矿工 | 右面板 三列网格 ═══════ -->
      <main class="ep-center">

        <!-- 左侧两个滚动面板 -->
        <div class="ep-side-panels ep-side-left">
          <!-- 心率数据 -->
          <div class="ep-scroll-panel sp-red">
            <div class="ep-sp-header">
              <span class="ep-sp-dot red"></span>
              <span class="ep-sp-name">心率数据</span>
              <span class="ep-sp-tag">实时</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-a">
                <div v-for="(item, idx) in hrItems.concat(hrItems)" :key="'hr'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 体征数据 -->
          <div class="ep-scroll-panel sp-yellow">
            <div class="ep-sp-header">
              <span class="ep-sp-dot yellow"></span>
              <span class="ep-sp-name">体征数据</span>
              <span class="ep-sp-tag">实时</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-b">
                <div v-for="(item, idx) in vitalItems.concat(vitalItems)" :key="'vt'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中央矿工舞台 -->
        <div class="ep-miner-stage">
          <div class="ep-ring ep-ring1"></div>
          <div class="ep-ring ep-ring2"></div>
          <div class="ep-scan-overlay"></div>
          <img class="ep-miner" src="/assets/miner-worker.png" />
          <div class="ep-glow-base"></div>
        </div>

        <!-- 右侧两个滚动面板 -->
        <div class="ep-side-panels ep-side-right">
          <!-- 血氧监测 -->
          <div class="ep-scroll-panel sp-blue">
            <div class="ep-sp-header">
              <span class="ep-sp-dot blue"></span>
              <span class="ep-sp-name">血氧监测</span>
              <span class="ep-sp-tag">实时</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-c">
                <div v-for="(item, idx) in spo2Items.concat(spo2Items)" :key="'sp'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 预警摘要 -->
          <div class="ep-scroll-panel sp-orange">
            <div class="ep-sp-header">
              <span class="ep-sp-dot orange"></span>
              <span class="ep-sp-name">预警摘要</span>
              <span class="ep-sp-tag">统计</span>
            </div>
            <div class="ep-sp-body">
              <div class="ep-sp-inner ep-sp-scroll-d">
                <div v-for="(item, idx) in warnItems.concat(warnItems)" :key="'wn'+idx" class="ep-sp-row">
                  <span class="ep-sp-label">{{ item.label }}</span>
                  <span :class="['ep-sp-val', item.cls]">{{ item.v }}</span>
                  <span class="ep-sp-unit">{{ item.unit }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </main>

      <!-- ═══════ 右栏 ═══════ -->
      <aside class="ep-right">

        <!-- 实时体征数据 -->
        <div class="ep-panel ep-vitals">
          <div class="ep-ph"><span class="ep-ph-bar"></span>实时体征数据<span class="ep-ph-link" @click="refresh">刷新</span></div>
          <div class="ep-vital-grid">
            <div class="ep-vital-card hr">
              <div class="ep-vc-label">心率</div>
              <div class="ep-vc-val"><span :class="hrClass(vitals.heartRate)">{{ vitals.heartRate || '--' }}</span> bpm</div>
              <div class="ep-vc-bar"><div :style="{ width: hrPct(vitals.heartRate) + '%' }" class="ep-vc-fill hr-fill"></div></div>
              <div class="ep-vc-range">正常 60-100 bpm</div>
            </div>
            <div class="ep-vital-card spo2">
              <div class="ep-vc-label">血氧</div>
              <div class="ep-vc-val"><span :class="spo2Class(vitals.bloodOxygen)">{{ vitals.bloodOxygen || '--' }}</span> %</div>
              <div class="ep-vc-bar"><div :style="{ width: spo2Pct(vitals.bloodOxygen) + '%' }" class="ep-vc-fill spo2-fill"></div></div>
              <div class="ep-vc-range">正常 ≥ 95%</div>
            </div>
            <div class="ep-vital-card temp">
              <div class="ep-vc-label">体温</div>
              <div class="ep-vc-val"><span :class="tempClass(vitals.temperature)">{{ fmtTemp(vitals.temperature) }}</span> °C</div>
              <div class="ep-vc-bar"><div :style="{ width: tempPct(vitals.temperature) + '%' }" class="ep-vc-fill temp-fill"></div></div>
              <div class="ep-vc-range">正常 36-37.3 °C</div>
            </div>
            <div class="ep-vital-card steps">
              <div class="ep-vc-label">今日步数</div>
              <div class="ep-vc-val"><span class="cyan">{{ vitals.steps || '--' }}</span> 步</div>
              <div class="ep-vc-bar"><div :style="{ width: Math.min(100, (vitals.steps||0)/100) + '%' }" class="ep-vc-fill steps-fill"></div></div>
              <div class="ep-vc-range">目标 10,000 步</div>
            </div>
          </div>
          <div class="ep-update-time">更新于：{{ lastUpdate }}</div>
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

        <!-- ECG实时波形 -->
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

      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import HeartRateWave from '@/components/HeartRateWave.vue'
import { getUserRealtimeData } from '@/api/realtime'
import { getHealthRecords } from '@/api/health'
import { getRiskWarningList } from '@/api/risk-warning'

const route = useRoute()
const router = useRouter()

// ─── 员工基本信息（从路由 query 传入）────────────────────────
const empInfo = ref({
  empCode:     route.query.empCode || '',
  empName:     route.query.empName || '',
  gender:      Number(route.query.gender) || 1,
  birthDate:   route.query.birthDate || '',
  deptName:    route.query.deptName || '',
  jobTypeName: route.query.jobTypeName || '',
  phone:       route.query.phone || '',
})

const loading    = ref(false)
const vitals     = ref({})
const warnings   = ref([])
const isOnline   = ref(false)
const lastUpdate = ref('--')
const trendRef   = ref(null)
let   trendChart = null

// ─── 趋势数据 7 天均值 ─────────────────────────────────────
const trend7 = ref({ avgHr: 0, avgSpo2: 0, avgTemp: 0 })

// ─── 预警统计 ──────────────────────────────────────────────
const warnCount  = computed(() => warnings.value.length)
const pendCount  = computed(() => warnings.value.filter(w => !w.handled).length)
const warn7Count = computed(() => {
  const cutoff = Date.now() - 7 * 86400000
  return warnings.value.filter(w => {
    const t = new Date(w.createTime || w.time || 0).getTime()
    return t > cutoff
  }).length
})

// ─── 体征辅助 ─────────────────────────────────────────────
const fmtTemp  = (t) => !t ? '--' : t > 100 ? (t / 10).toFixed(1) : Number(t).toFixed(1)
const fmtTime  = (t) => !t ? '' : String(t).length > 16 ? String(t).substring(5, 16) : String(t)
const hrClass  = (v) => !v ? '' : (v < 60 || v > 100) ? 'red' : (v < 65 || v > 90) ? 'yellow' : 'green'
const spo2Class= (v) => !v ? '' : v < 90 ? 'red' : v < 95 ? 'yellow' : 'green'
const tempClass= (v) => { if (!v) return ''; const t = v > 100 ? v/10 : v; return (t > 37.3 || t < 36) ? 'red' : t > 37 ? 'yellow' : 'green' }
const pressClass=(v) => !v ? '' : v > 70 ? 'red' : v > 50 ? 'yellow' : 'green'
const hrPct    = (v) => !v ? 0 : Math.min(100, Math.max(0, (v - 40) / 80 * 100))
const spo2Pct  = (v) => !v ? 0 : Math.min(100, Math.max(0, (v - 85) / 15 * 100))
const tempPct  = (v) => { if (!v) return 0; const t = v > 100 ? v/10 : v; return Math.min(100, Math.max(0, (t - 35) / 5 * 100)) }
const calcAge  = (b) => { if (!b) return '--'; const age = new Date().getFullYear() - new Date(b).getFullYear(); return age > 0 && age < 100 ? age + '岁' : '--' }

// ─── 四个滚动面板数据 ──────────────────────────────────────
const hrItems = computed(() => {
  const hr = vitals.value.heartRate
  const status = !hr ? '--' : hr < 60 ? '偏低' : hr > 100 ? '偏高' : '正常'
  return [
    { key:'h1', label:'当前心率', v: hr || '--', unit:'bpm', cls: hrClass(hr) },
    { key:'h2', label:'7日均值', v: trend7.value.avgHr || '--', unit:'bpm', cls:'cyan' },
    { key:'h3', label:'正常范围', v:'60~100', unit:'bpm', cls:'dim' },
    { key:'h4', label:'心率状态', v: status, unit:'', cls: hrClass(hr) || 'green' },
    { key:'h5', label:'压力指数', v: vitals.value.pressure || '--', unit:'', cls: pressClass(vitals.value.pressure) },
    { key:'h6', label:'7日预警', v: warn7Count.value, unit:'次', cls: warn7Count.value > 3 ? 'red' : 'green' },
  ]
})
const spo2Items = computed(() => {
  const s = vitals.value.bloodOxygen
  const status = !s ? '--' : s < 90 ? '严重偏低' : s < 95 ? '偏低' : '正常'
  return [
    { key:'s1', label:'当前血氧', v: s || '--', unit:'%', cls: spo2Class(s) },
    { key:'s2', label:'7日均值', v: trend7.value.avgSpo2 || '--', unit:'%', cls:'cyan' },
    { key:'s3', label:'正常值', v:'≥ 95', unit:'%', cls:'dim' },
    { key:'s4', label:'血氧状态', v: status, unit:'', cls: spo2Class(s) || 'green' },
    { key:'s5', label:'近期预警', v: warnCount.value, unit:'次', cls: warnCount.value > 5 ? 'red' : 'green' },
    { key:'s6', label:'未处理', v: pendCount.value, unit:'条', cls: pendCount.value > 0 ? 'red' : 'green' },
  ]
})
const vitalItems = computed(() => {
  const t = vitals.value.temperature
  const tStatus = !t ? '--' : (tempClass(t) === 'red' ? '异常' : tempClass(t) === 'yellow' ? '偏高' : '正常')
  return [
    { key:'v1', label:'体温', v: fmtTemp(t), unit:'°C', cls: tempClass(t) },
    { key:'v2', label:'今日步数', v: vitals.value.steps || '--', unit:'步', cls:'green' },
    { key:'v3', label:'压力指数', v: vitals.value.pressure || '--', unit:'', cls: pressClass(vitals.value.pressure) },
    { key:'v4', label:'目标步数', v:'10,000', unit:'步', cls:'dim' },
    { key:'v5', label:'体温状态', v: tStatus, unit:'', cls: tempClass(t) || 'green' },
    { key:'v6', label:'综合评分', v: trend7.value.avgHr ? Math.max(60, 100 - warnCount.value * 3) : '--', unit:'分', cls:'cyan' },
  ]
})
const warnItems = computed(() => {
  const hrW  = warnings.value.filter(w => (w.warningType||'').includes('心率')).length
  const s2W  = warnings.value.filter(w => (w.warningType||'').includes('血氧')).length
  const prW  = warnings.value.filter(w => (w.warningType||'').includes('压力')).length
  return [
    { key:'w1', label:'近30日预警', v: warnCount.value, unit:'次', cls: warnCount.value > 10 ? 'red' : warnCount.value > 5 ? 'yellow' : 'green' },
    { key:'w2', label:'未处理', v: pendCount.value, unit:'条', cls: pendCount.value > 0 ? 'red' : 'green' },
    { key:'w3', label:'7日预警', v: warn7Count.value, unit:'次', cls:'cyan' },
    { key:'w4', label:'心率预警', v: hrW, unit:'次', cls: hrW > 0 ? 'red' : 'green' },
    { key:'w5', label:'血氧预警', v: s2W, unit:'次', cls: s2W > 0 ? 'red' : 'green' },
    { key:'w6', label:'压力预警', v: prW, unit:'次', cls: prW > 0 ? 'yellow' : 'green' },
  ]
})

// ─── 风险评估（基于实时数据计算）─────────────────────────
const riskItems = computed(() => {
  const hr  = vitals.value.heartRate  || 0
  const spo2= vitals.value.bloodOxygen|| 0
  const t   = vitals.value.temperature ? (vitals.value.temperature > 100 ? vitals.value.temperature/10 : vitals.value.temperature) : 0
  const prs = vitals.value.pressure   || 0
  const wc  = warnCount.value

  const mk = (name, icon, val, thHigh, thMid, maxV) => {
    const pct = Math.min(100, Math.round(val / maxV * 100))
    const level = val >= thHigh ? 'high' : val >= thMid ? 'mid' : 'low'
    const levelText = level === 'high' ? '风险高' : level === 'mid' ? '风险中' : '风险低'
    const color = level === 'high' ? '#ff4444' : level === 'mid' ? '#ffaa00' : '#00c853'
    return { name, icon, pct, level, levelText, color }
  }

  const hrRisk  = hr  === 0 ? 0 : Math.abs(hr  - 75) / 25 * 60
  const spo2Risk= spo2=== 0 ? 0 : Math.max(0, (100 - spo2) * 5)
  const tRisk   = t   === 0 ? 0 : Math.abs(t   - 36.5) / 1.5 * 60
  const pRisk   = prs > 0 ? prs * 0.7 : 20
  const wRisk   = Math.min(80, wc * 5)

  return [
    mk('心率健康',   '💓', hrRisk,   60, 30, 100),
    mk('血氧健康',   '🩸', spo2Risk, 60, 30, 100),
    mk('体温健康',   '🌡️',  tRisk,   60, 30, 100),
    mk('压力水平',   '🧠', pRisk,    60, 30, 100),
    mk('预警风险',   '⚠️',  wRisk,   50, 20, 100),
    mk('综合健康',   '❤️',  Math.min(100,(hrRisk+spo2Risk+tRisk)/3), 60, 30, 100),
  ]
})

// ─── 数据加载 ─────────────────────────────────────────────
const refresh = async () => {
  const code = empInfo.value.empCode
  if (!code) return

  const [rtRes, warnRes] = await Promise.allSettled([
    getUserRealtimeData(code),
    getRiskWarningList({ userCode: code, page: 1, size: 20 })
  ])

  if (rtRes.status === 'fulfilled' && rtRes.value?.data) {
    const d = rtRes.value.data
    vitals.value   = d
    isOnline.value = true
    lastUpdate.value = new Date().toLocaleString('zh-CN')
  }

  if (warnRes.status === 'fulfilled' && warnRes.value?.data) {
    const wd = warnRes.value.data
    warnings.value = Array.isArray(wd) ? wd : (wd.records || wd.list || [])
  }

  await nextTick()
  buildTrendChart()
}

const buildTrendChart = async () => {
  if (!trendRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendRef.value)

  const code = empInfo.value.empCode
  const labels = [], dateKeys = [], dateMap = {}
  for (let i = 6; i >= 0; i--) {
    const d = new Date(); d.setDate(d.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    labels.push(`${d.getMonth()+1}/${d.getDate()}`)
    dateKeys.push(key)
    dateMap[key] = { hr: [], spo2: [], temp: [] }
  }

  if (code) {
    try {
      const res = await getHealthRecords({ userCode: code, size: 500, startTime: dateKeys[0], endTime: dateKeys[6] })
      if (res?.data) {
        const recs = Array.isArray(res.data) ? res.data : (res.data.records || res.data.list || [])
        recs.forEach(r => {
          const dt = (r.recordTime || r.record_time || '').substring(0, 10)
          if (!dateMap[dt]) return
          if (r.heartRate)   dateMap[dt].hr.push(+r.heartRate)
          if (r.bloodOxygen) dateMap[dt].spo2.push(+r.bloodOxygen)
          if (r.temperature) { const t = r.temperature > 100 ? r.temperature/10 : +r.temperature; dateMap[dt].temp.push(t) }
        })
      }
    } catch {}
  }

  const avg = arr => arr.length ? +(arr.reduce((a,b)=>a+b,0)/arr.length).toFixed(1) : null
  const hrV   = dateKeys.map(k => avg(dateMap[k].hr))
  const spo2V = dateKeys.map(k => avg(dateMap[k].spo2))

  const validHr   = hrV.filter(Boolean)
  const validSpo2 = spo2V.filter(Boolean)
  if (validHr.length)   trend7.value.avgHr   = Math.round(validHr.reduce((a,b)=>a+b,0)/validHr.length)
  if (validSpo2.length) trend7.value.avgSpo2 = +(validSpo2.reduce((a,b)=>a+b,0)/validSpo2.length).toFixed(1)

  trendChart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 40, right: 50, top: 12, bottom: 28 },
    xAxis: { type: 'category', data: labels, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } }, axisLabel: { fontSize: 10, color: 'rgba(255,255,255,0.45)' } },
    yAxis: [
      { type: 'value', min: 50, max: 120, axisLine: { lineStyle: { color: '#ff5252' } }, axisLabel: { fontSize: 9, color: '#ff5252' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } } },
      { type: 'value', min: 85, max: 100, axisLine: { lineStyle: { color: '#1890ff' } }, axisLabel: { fontSize: 9, color: '#1890ff' }, splitLine: { show: false } }
    ],
    series: [
      { name: '心率', type: 'line', yAxisIndex: 0, data: hrV, smooth: true, connectNulls: false, lineStyle: { color: '#ff5252', width: 2 }, itemStyle: { color: '#ff5252' }, areaStyle: { color: 'rgba(255,82,82,0.1)' }, symbol: 'circle', symbolSize: 4 },
      { name: '血氧', type: 'line', yAxisIndex: 1, data: spo2V, smooth: true, connectNulls: false, lineStyle: { color: '#1890ff', width: 2 }, itemStyle: { color: '#1890ff' }, areaStyle: { color: 'rgba(24,144,255,0.1)' }, symbol: 'circle', symbolSize: 4 }
    ],
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,22,40,0.95)', borderColor: 'rgba(0,180,255,0.3)', textStyle: { color: '#fff', fontSize: 11 } },
    legend: { top: 0, right: 0, textStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 } }
  })
}

let timer = null
onMounted(async () => {
  loading.value = true
  await refresh()
  loading.value = false
  timer = setInterval(refresh, 30000)
})
onUnmounted(() => {
  clearInterval(timer)
  if (trendChart) { trendChart.dispose(); trendChart = null }
})
</script>

<style scoped>
/* ═══ 整体 ═══ */
/* app-main 有 padding-top: 50px（fixed-header 场景）+ header 本身 50px = 100px 总偏移 */
.ep-page {
  height: calc(100vh - 100px);
  background: #080d1a;
  color: #c8d8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ═══ 顶部 ═══ */
.ep-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 24px 12px;
  background: linear-gradient(180deg, #0c1228 0%, #080d1a 100%);
  border-bottom: 1px solid #1a2545;
  flex-shrink: 0;
}
.ep-back {
  display: flex; align-items: center; gap: 6px;
  background: rgba(0,180,255,0.1); border: 1px solid rgba(0,180,255,0.3);
  color: #00b4ff; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px;
  transition: background 0.2s; white-space: nowrap;
}
.ep-back:hover { background: rgba(0,180,255,0.2); }
.ep-title {
  font-size: 20px; font-weight: 700; color: #e8f4ff; letter-spacing: 2px;
  flex: 1; text-align: center; text-shadow: 0 0 20px rgba(0,180,255,0.4);
}
.ep-header-emp { display: flex; align-items: center; gap: 12px; }
.ep-hname { font-size: 16px; font-weight: 700; color: #e8f4ff; }
.ep-online { display: flex; align-items: center; gap: 5px; font-size: 12px; }
.ep-online.on { color: #00e676; }
.ep-online.off { color: #5a7090; }
.ep-dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; display: inline-block; }
.ep-online.on .ep-dot { box-shadow: 0 0 6px #00e676; }
.ep-update { font-size: 11px; color: #4a7090; }

/* ═══ 主体 ═══ */
.ep-body {
  flex: 1;
  display: grid;
  grid-template-columns: 250px 1fr 310px;
  gap: 12px;
  padding: 12px 18px;
  overflow: hidden;
  min-height: 0;
}
.ep-center { height: 100%; }

/* ═══ 通用面板 ═══ */
.ep-panel {
  background: linear-gradient(145deg, #0c1630 0%, #0a1225 100%);
  border: 1px solid #1a2d50;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
}
.ep-ph {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 700; color: #c0d8f8;
  margin-bottom: 10px; padding-bottom: 7px;
  border-bottom: 1px solid #1a2d50;
}
.ep-ph-bar { width: 3px; height: 14px; background: #00b4ff; border-radius: 2px; flex-shrink: 0; }
.ep-ph-link { margin-left: auto; font-size: 11px; color: #00b4ff; cursor: pointer; font-weight: 400; }
.ep-ph-link:hover { text-decoration: underline; }
.ep-warn-count { font-size: 12px; color: #ff5252; margin-left: 4px; }

/* ═══ 左栏 ═══ */
.ep-left { display: flex; flex-direction: column; overflow: hidden; }

/* 基本信息 */
.ep-basic-body { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.ep-basic-avatar { width: 70px; height: 90px; overflow: hidden; }
.ep-avatar-img { width: 100%; height: 100%; object-fit: contain; object-position: top; filter: drop-shadow(0 0 8px rgba(0,180,255,0.4)); animation: epFloat 3s ease-in-out infinite alternate; }
@keyframes epFloat { 0% { transform: translateY(0); } 100% { transform: translateY(-6px); } }
.ep-basic-name { font-size: 18px; font-weight: 700; color: #e8f4ff; }
.ep-basic-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 5px 10px; width: 100%; }
.ep-bi { display: flex; gap: 6px; font-size: 12px; }
.ep-bi.full { grid-column: span 2; }
.ep-bi span { color: #4a7090; min-width: 28px; }
.ep-bi b { color: #a0c0e8; font-weight: 500; }
.ep-bi b.code { color: #00c8ff; font-family: monospace; font-size: 11px; }

/* 趋势图 */
.ep-trend-chart { width: 100%; height: 140px; }

/* 预警列表 */
.ep-warns { flex: 1; overflow: hidden; }
.ep-empty-warn { text-align: center; color: #3a5070; font-size: 12px; padding: 12px 0; }
.ep-warn-list { display: flex; flex-direction: column; gap: 5px; max-height: 160px; overflow-y: auto; }
.ep-warn-list::-webkit-scrollbar { width: 3px; }
.ep-warn-list::-webkit-scrollbar-thumb { background: #1a3060; border-radius: 2px; }
.ep-warn-item { display: flex; align-items: center; gap: 6px; font-size: 11px; padding: 4px 6px; background: rgba(0,0,0,0.2); border-radius: 4px; }
.ep-wdot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.ep-wdot.pend { background: #ff5252; box-shadow: 0 0 4px #ff5252; }
.ep-wdot.done { background: #00c853; }
.ep-wtype { flex: 1; font-weight: 600; color: #a0c0e8; }
.ep-wtime { color: #4a7090; }
.ep-wst { font-size: 10px; padding: 1px 5px; border-radius: 3px; }
.ep-wst.pend { background: rgba(255,82,82,0.2); color: #ff5252; }
.ep-wst.done { background: rgba(0,200,83,0.15); color: #00c853; }

/* ═══ 中栏：左面板列 | 矿工 | 右面板列 ═══ */
.ep-center {
  display: grid;
  grid-template-columns: 200px 1fr 200px;
  height: 100%;
}

/* 侧面板列 */
.ep-side-panels {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 8px;
  justify-content: center;
}

/* 滚动面板 */
.ep-scroll-panel {
  background: rgba(5, 18, 40, 0.88);
  border: 1px solid rgba(0,180,255,0.3);
  border-radius: 8px;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 14px rgba(0,80,200,0.1);
}
.ep-scroll-panel.sp-red    { border-color: rgba(255,80,80,0.4); }
.ep-scroll-panel.sp-yellow { border-color: rgba(255,170,0,0.35); }
.ep-scroll-panel.sp-blue   { border-color: rgba(0,150,255,0.4); }
.ep-scroll-panel.sp-orange { border-color: rgba(255,120,0,0.4); }

.ep-sp-header {
  display: flex; align-items: center; gap: 7px;
  padding: 7px 10px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  background: rgba(0,0,0,0.25); flex-shrink: 0;
}
.ep-sp-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ep-sp-dot.red    { background: #ff5252; box-shadow: 0 0 5px #ff5252; }
.ep-sp-dot.yellow { background: #ffaa00; box-shadow: 0 0 5px #ffaa00; }
.ep-sp-dot.blue   { background: #1890ff; box-shadow: 0 0 5px #1890ff; }
.ep-sp-dot.orange { background: #ff7700; box-shadow: 0 0 5px #ff7700; }
.ep-sp-name { font-size: 12px; font-weight: 700; color: #c0d8f8; flex: 1; }
.ep-sp-tag  { font-size: 10px; color: #4a7090; background: rgba(0,180,255,0.08); padding: 1px 5px; border-radius: 2px; }

.ep-sp-body { flex: 1; overflow: hidden; }
.ep-sp-row {
  display: flex; align-items: center; gap: 4px;
  padding: 7px 10px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.ep-sp-label { font-size: 11px; color: #4a6880; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ep-sp-val   { font-size: 14px; font-weight: 700; font-family: monospace; white-space: nowrap; }
.ep-sp-unit  { font-size: 10px; color: #4a7090; min-width: 16px; white-space: nowrap; }

/* 滚动动画 — 4种速度 */
@keyframes spScrollUp { 0% { transform: translateY(0); } 100% { transform: translateY(-50%); } }
.ep-sp-scroll-a { animation: spScrollUp  9s linear infinite; }
.ep-sp-scroll-b { animation: spScrollUp 11s linear infinite; }
.ep-sp-scroll-c { animation: spScrollUp 10s linear infinite; }
.ep-sp-scroll-d { animation: spScrollUp 13s linear infinite; }
.ep-sp-scroll-a:hover,.ep-sp-scroll-b:hover,
.ep-sp-scroll-c:hover,.ep-sp-scroll-d:hover { animation-play-state: paused; }

/* 矿工舞台 */
.ep-miner-stage {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 旋转圆环 */
.ep-ring { position: absolute; border-radius: 50%; border: 1px solid transparent; }
.ep-ring1 {
  width: 300px; height: 300px;
  top: 50%; left: 50%; transform: translate(-50%, 20%);
  background: conic-gradient(rgba(0,180,255,0.6), rgba(0,180,255,0.05), rgba(0,180,255,0.6)) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: destination-out; mask-composite: exclude;
  animation: epRing1Centered 4s linear infinite;
}
.ep-ring2 {
  width: 250px; height: 250px;
  top: 50%; left: 50%; transform: translate(-50%, 15%);
  background: conic-gradient(rgba(0,220,255,0.3), transparent, rgba(0,220,255,0.3)) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: destination-out; mask-composite: exclude;
  animation: epRing1Centered 6s linear infinite reverse;
}
@keyframes epRing1Centered { 0% { transform: translate(-50%, 20%) rotate(0deg); } 100% { transform: translate(-50%, 20%) rotate(360deg); } }

/* 扫描线 */
.ep-scan-overlay {
  position: absolute; inset: 0;
  background: repeating-linear-gradient(rgba(0,200,255,0.04) 0, rgba(0,200,255,0) 2px, rgba(0,200,255,0) 4px);
  animation: epScan 5s linear infinite alternate;
  pointer-events: none; z-index: 3;
}
@keyframes epScan { 0% { transform: translateY(-4px); } 100% { transform: translateY(4px); } }

/* 矿工图 */
.ep-miner {
  position: relative;
  height: 90%; width: auto;
  filter: drop-shadow(0 0 24px rgba(0,180,255,0.55)) drop-shadow(0 0 48px rgba(0,100,255,0.35));
  animation: epMinerFloat 3s ease-in-out infinite alternate;
  z-index: 2;
}
@keyframes epMinerFloat { 0% { transform: translateY(0); } 100% { transform: translateY(-12px); } }

/* 底部光晕 */
.ep-glow-base {
  position: absolute;
  top: 73%; left: 50%; transform: translateX(-50%);
  width: 200px; height: 24px;
  background: radial-gradient(ellipse at center, rgba(0,180,255,0.55) 0%, transparent 70%);
  border-radius: 50%; z-index: 1;
}
.ep-fv.dim    { color: #5a8090; font-size: 13px; }
.ep-fu { font-size: 11px; color: #4a7090; }

/* ═══ 右栏 ═══ */
.ep-right { display: flex; flex-direction: column; overflow: hidden; }

/* 实时体征 */
.ep-vital-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px; }
.ep-vital-card { padding: 10px; border-radius: 7px; border: 1px solid rgba(255,255,255,0.06); background: rgba(0,0,0,0.2); }
.ep-vital-card.hr   { border-left: 3px solid #ff5252; }
.ep-vital-card.spo2 { border-left: 3px solid #1890ff; }
.ep-vital-card.temp { border-left: 3px solid #ffaa00; }
.ep-vital-card.steps{ border-left: 3px solid #00c853; }
.ep-vc-label { font-size: 11px; color: #4a7090; margin-bottom: 3px; }
.ep-vc-val { font-size: 20px; font-weight: 700; margin-bottom: 5px; color: #e0f0ff; font-family: monospace; }
.ep-vc-val .red    { color: #ff4444; }
.ep-vc-val .yellow { color: #ffaa00; }
.ep-vc-val .green  { color: #00e676; }
.ep-vc-val .cyan   { color: #00c8ff; }
.ep-vc-bar { height: 3px; background: rgba(255,255,255,0.08); border-radius: 2px; overflow: hidden; margin-bottom: 4px; }
.ep-vc-fill { height: 100%; border-radius: 2px; transition: width 0.8s; }
.hr-fill   { background: linear-gradient(90deg, #ff5252, #ff8a80); }
.spo2-fill { background: linear-gradient(90deg, #1890ff, #40c4ff); }
.temp-fill { background: linear-gradient(90deg, #ffaa00, #ffd740); }
.steps-fill{ background: linear-gradient(90deg, #00c853, #69f0ae); }
.ep-vc-range { font-size: 10px; color: rgba(255,255,255,0.25); }
.ep-update-time { font-size: 10px; color: #3a5070; text-align: right; margin-top: 4px; }

/* 风险评估 */
.ep-risk-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.ep-risk-card { display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 7px; background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.05); }
.ep-risk-card.high { border-color: rgba(255,60,60,0.25); }
.ep-risk-card.mid  { border-color: rgba(255,170,0,0.2); }
.ep-risk-card.low  { border-color: rgba(0,200,83,0.15); }
.ep-risk-icon { font-size: 20px; flex-shrink: 0; }
.ep-risk-body { flex: 1; overflow: hidden; }
.ep-risk-name { font-size: 11px; color: #8ab0d0; margin-bottom: 4px; }
.ep-risk-bar-wrap { display: flex; align-items: center; gap: 6px; }
.ep-risk-bar-track { flex: 1; height: 4px; background: rgba(255,255,255,0.08); border-radius: 2px; overflow: hidden; }
.ep-risk-bar-fill { height: 100%; border-radius: 2px; transition: width 1s; }
.ep-risk-label { font-size: 10px; white-space: nowrap; }
.ep-risk-label.high { color: #ff4444; }
.ep-risk-label.mid  { color: #ffaa00; }
.ep-risk-label.low  { color: #00c853; }
.ep-risk-pct { font-size: 10px; color: rgba(255,255,255,0.35); margin-left: 2px; }

/* ECG */
.ep-ecg { flex-shrink: 0; }
.ep-no-ecg { height: 90px; display: flex; align-items: center; justify-content: center; color: rgba(126,184,247,0.3); font-size: 12px; background: rgba(255,255,255,0.02); border: 1px dashed rgba(126,184,247,0.1); border-radius: 4px; }

/* 颜色工具类 */
.red    { color: #ff4444; }
.yellow { color: #ffaa00; }
.green  { color: #00e676; }
.cyan   { color: #00c8ff; }
.dim    { color: #5a8090; }
</style>
