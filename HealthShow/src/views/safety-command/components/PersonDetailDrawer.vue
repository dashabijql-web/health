<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="userName || '人员详情'"
    direction="rtl"
    size="440px"
    class="person-drawer"
    :close-on-click-modal="true"
  >
    <div class="pd-body" v-loading="loading">
      <!-- 人员基本信息 -->
      <div class="pd-header">
        <div class="pd-avatar">{{ (userName || '?')[0] }}</div>
        <div class="pd-info">
          <div class="pd-name">{{ userName }}</div>
          <div class="pd-meta">
            <span v-if="realtimeData.deptName">{{ realtimeData.deptName }}</span>
            <span :class="['pd-status', realtimeData.online ? 'online' : 'offline']">
              <i class="status-dot"></i>{{ realtimeData.online ? '在线' : '离线' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 实时体征卡片 -->
      <div class="vital-grid">
        <div class="vital-card v-hr">
          <div class="vc-label">心率</div>
          <div class="vc-value">
            <span :class="['vc-num', getHrClass(realtimeData.heartRate)]">{{ realtimeData.heartRate || '--' }}</span>
            <span class="vc-unit">bpm</span>
          </div>
          <div class="vc-range">正常 60-100</div>
        </div>
        <div class="vital-card v-spo2">
          <div class="vc-label">血氧</div>
          <div class="vc-value">
            <span :class="['vc-num', getSpo2Class(realtimeData.bloodOxygen)]">{{ realtimeData.bloodOxygen || '--' }}</span>
            <span class="vc-unit">%</span>
          </div>
          <div class="vc-range">正常 ≥95</div>
        </div>
        <div class="vital-card v-temp">
          <div class="vc-label">体温</div>
          <div class="vc-value">
            <span :class="['vc-num', getTempClass(realtimeData.temperature)]">{{ formatTemp(realtimeData.temperature) }}</span>
            <span class="vc-unit">°C</span>
          </div>
          <div class="vc-range">正常 36.0-37.3</div>
        </div>
        <div class="vital-card v-steps">
          <div class="vc-label">今日步数</div>
          <div class="vc-value">
            <span class="vc-num blue">{{ realtimeData.steps || '--' }}</span>
            <span class="vc-unit">步</span>
          </div>
          <div class="vc-range">
            <el-progress
              :percentage="Math.min(100, ((realtimeData.steps || 0) / 10000) * 100)"
              :stroke-width="4"
              :show-text="false"
              color="#1890ff"
            />
          </div>
        </div>
      </div>

      <!-- ECG 波形 -->
      <div class="pd-section">
        <div class="sec-title">实时心电图</div>
        <HeartRateWave
          v-if="realtimeData.heartRate"
          :width="396"
          :height="120"
          :heartRate="realtimeData.heartRate"
          waveColor="#00ff00"
          :speed="2"
          :showWarning="false"
        />
        <div v-else class="pd-no-ecg">暂无心电数据</div>
      </div>

      <!-- 7天趋势图 -->
      <div class="pd-section">
        <div class="sec-title">7天趋势</div>
        <div ref="trendChartRef" class="trend-chart"></div>
      </div>

      <!-- 近期预警 -->
      <div class="pd-section" v-if="recentWarnings.length > 0">
        <div class="sec-title">近期预警 ({{ recentWarnings.length }})</div>
        <div class="warn-list">
          <div v-for="w in recentWarnings" :key="w.id" class="warn-item">
            <span :class="['wi-dot', w.handled ? 'done' : 'pending']"></span>
            <span class="wi-type">{{ w.warningType || w.type }}</span>
            <span class="wi-time">{{ formatTime(w.createTime || w.time) }}</span>
            <span :class="['wi-status', w.handled ? 'done' : 'pending']">{{ w.handled ? '已处理' : '未处理' }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="pd-actions">
        <el-button type="primary" @click="$emit('call', userCode)">呼叫</el-button>
        <el-button type="warning" @click="$emit('notify', userCode)">通知</el-button>
        <el-button @click="$emit('viewRecord', userCode)">完整档案</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted } from 'vue'
import * as echarts from '@/utils/echarts-setup'
import HeartRateWave from '@/components/HeartRateWave.vue'
import { getUserRealtimeData } from '@/api/realtime'
import { getHealthRecords } from '@/api/health'
import { getRiskWarningList } from '@/api/risk-warning'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userCode: { type: String, default: '' },
  userName: { type: String, default: '' }
})

defineEmits(['update:visible', 'call', 'notify', 'viewRecord'])

const loading = ref(false)
const realtimeData = ref({})
const recentWarnings = ref([])
const trendChartRef = ref(null)
let trendChart = null

const formatTemp = (t) => {
  if (!t) return '--'
  // Backend stores temp as int * 10 (367 = 36.7)
  return t > 100 ? (t / 10).toFixed(1) : t.toFixed ? t.toFixed(1) : t
}

const formatTime = (t) => {
  if (!t) return ''
  if (typeof t === 'string' && t.length > 16) return t.substring(5, 16)
  return t
}

const getHrClass = (v) => !v ? '' : v < 60 || v > 100 ? 'danger' : v < 65 || v > 90 ? 'warn' : 'normal'
const getSpo2Class = (v) => !v ? '' : v < 90 ? 'danger' : v < 95 ? 'warn' : 'normal'
const getTempClass = (v) => {
  if (!v) return ''
  const t = v > 100 ? v / 10 : v
  return t > 37.3 || t < 36.0 ? 'danger' : t > 37.0 ? 'warn' : 'normal'
}

const fetchData = async () => {
  if (!props.userCode) return
  loading.value = true
  try {
    const [rtRes, warnings] = await Promise.allSettled([
      getUserRealtimeData(props.userCode),
      getRiskWarningList({ userCode: props.userCode, page: 1, size: 5 })
    ])

    if (rtRes.status === 'fulfilled' && rtRes.value?.data) {
      const d = rtRes.value.data
      realtimeData.value = {
        heartRate: d.heartRate,
        bloodOxygen: d.bloodOxygen,
        temperature: d.temperature,
        steps: d.steps,
        deptName: d.deptName,
        online: true
      }
    }

    if (warnings.status === 'fulfilled' && warnings.value?.data) {
      const wData = warnings.value.data
      recentWarnings.value = Array.isArray(wData) ? wData : (wData.records || wData.list || [])
    }

    await nextTick()
    buildTrendChart()
  } catch { /* silent */
  } finally {
    loading.value = false
  }
}

const buildTrendChart = async () => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  // Build 7-day date labels and lookup map
  const labels = []
  const dateKeys = []
  const dateMap = {}
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    labels.push(`${d.getMonth() + 1}/${d.getDate()}`)
    dateKeys.push(key)
    dateMap[key] = { hr: [], spo2: [], temp: [] }
  }

  // Fetch this person's health records for the last 7 days
  if (props.userCode) {
    try {
      const res = await getHealthRecords({
        userCode: props.userCode,
        size: 500,
        startTime: dateKeys[0],
        endTime: dateKeys[6]
      })
      if (res?.data) {
        const data = res.data
        const records = Array.isArray(data) ? data : (data.records || data.list || [])
        records.forEach(r => {
          const dt = (r.recordTime || r.record_time || '').substring(0, 10)
          if (!dateMap[dt]) return
          if (r.heartRate) dateMap[dt].hr.push(+r.heartRate)
          if (r.bloodOxygen) dateMap[dt].spo2.push(+r.bloodOxygen)
          if (r.temperature) {
            const t = r.temperature > 100 ? r.temperature / 10 : +r.temperature
            dateMap[dt].temp.push(t)
          }
        })
      }
    } catch (e) { /* silent */ }
  }

  const avg = (arr) => arr.length ? +(arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(1) : null
  const hrValues = dateKeys.map(k => avg(dateMap[k].hr))
  const spo2Values = dateKeys.map(k => avg(dateMap[k].spo2))
  const tempValues = dateKeys.map(k => avg(dateMap[k].temp))

  const hasHr = hrValues.some(v => v !== null)
  const hasSpo2 = spo2Values.some(v => v !== null)
  const hasTemp = tempValues.some(v => v !== null)

  if (!hasHr && !hasSpo2 && !hasTemp) {
    trendChart.setOption({
      graphic: [{
        type: 'text', left: 'center', top: 'middle',
        style: { text: '暂无趋势数据', fill: 'rgba(255,255,255,0.3)', fontSize: 13 }
      }]
    })
    return
  }

  const series = []
  if (hasHr) series.push({
    name: '心率', type: 'line', yAxisIndex: 0, data: hrValues,
    smooth: true, connectNulls: false,
    lineStyle: { color: '#ff5252', width: 2 }, itemStyle: { color: '#ff5252' },
    areaStyle: { color: 'rgba(255,82,82,0.1)' }, symbol: 'circle', symbolSize: 4
  })
  if (hasSpo2) series.push({
    name: '血氧', type: 'line', yAxisIndex: 1, data: spo2Values,
    smooth: true, connectNulls: false,
    lineStyle: { color: '#1890ff', width: 2 }, itemStyle: { color: '#1890ff' },
    areaStyle: { color: 'rgba(24,144,255,0.1)' }, symbol: 'circle', symbolSize: 4
  })
  if (hasTemp) series.push({
    name: '体温', type: 'line', yAxisIndex: 2, data: tempValues,
    smooth: true, connectNulls: false,
    lineStyle: { color: '#faad14', width: 1.5, type: 'dashed' },
    itemStyle: { color: '#faad14' }, symbol: 'circle', symbolSize: 3
  })

  trendChart.setOption({
    grid: { left: 40, right: hasTemp ? 70 : 20, top: 10, bottom: 25 },
    xAxis: {
      type: 'category', data: labels,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
      axisLabel: { fontSize: 10, color: 'rgba(255,255,255,0.5)' }
    },
    yAxis: [
      { type: 'value', name: 'HR', min: 50, max: 120,
        axisLine: { lineStyle: { color: '#ff5252' } },
        axisLabel: { fontSize: 9, color: '#ff5252' },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } } },
      { type: 'value', name: 'SpO2', min: 85, max: 100,
        axisLine: { lineStyle: { color: '#1890ff' } },
        axisLabel: { fontSize: 9, color: '#1890ff' }, splitLine: { show: false } },
      { type: 'value', name: '℃', min: 35, max: 40,
        axisLine: { lineStyle: { color: '#faad14' } },
        axisLabel: { fontSize: 9, color: '#faad14' }, splitLine: { show: false } }
    ],
    series,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10,22,40,0.9)',
      borderColor: 'rgba(24,144,255,0.3)',
      textStyle: { color: '#fff', fontSize: 11 }
    }
  })
}

watch(() => props.visible, (val) => {
  if (val && props.userCode) {
    realtimeData.value = {}
    recentWarnings.value = []
    fetchData()
  }
})

onUnmounted(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped lang="scss">
$blue: #1890ff;
$red: #ff5252;
$orange: #ff9800;
$yellow: #faad14;
$green: #52c41a;
$bg: #0d1f3c;
$text: rgba(255,255,255,.9);
$text2: rgba(255,255,255,.6);

.pd-body {
  padding: 0 4px;
  color: $text;
}

.pd-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba($blue, .08);
  border: 1px solid rgba($blue, .25);
  border-radius: 8px;
  margin-bottom: 14px;
}

.pd-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, $blue, #40a9ff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
}

.pd-info { flex: 1; }
.pd-name { font-size: 16px; font-weight: bold; margin-bottom: 4px; }
.pd-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: $text2;
}
.pd-status {
  display: flex;
  align-items: center;
  gap: 4px;
  &.online { color: $green; }
  &.offline { color: $text2; }
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  .online & { background: $green; box-shadow: 0 0 6px $green; }
  .offline & { background: #666; }
}

// Vital signs grid
.vital-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 14px;
}

.vital-card {
  padding: 10px;
  border-radius: 6px;
  border: 1px solid rgba($blue, .25);
  background: rgba(0,0,0,.2);
}

.vc-label {
  font-size: 11px;
  color: $text2;
  margin-bottom: 4px;
}

.vc-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.vc-num {
  font-size: 26px;
  font-weight: bold;
  font-family: 'Courier New', monospace;
  &.normal { color: $green; }
  &.warn { color: $orange; }
  &.danger { color: $red; }
  &.blue { color: $blue; }
}

.vc-unit {
  font-size: 11px;
  color: $text2;
}

.vc-range {
  font-size: 9px;
  color: rgba(255,255,255,.35);
  margin-top: 4px;
}

.v-hr { border-left: 3px solid $red; }
.v-spo2 { border-left: 3px solid $blue; }
.v-temp { border-left: 3px solid $orange; }
.v-steps { border-left: 3px solid $green; }

// Sections
.pd-section {
  margin-bottom: 14px;
}
.pd-no-ecg {
  height: 120px; display: flex; align-items: center; justify-content: center;
  color: rgba(126,184,247,0.4); font-size: 12px; background: rgba(255,255,255,0.02);
  border: 1px dashed rgba(126,184,247,0.15); border-radius: 4px;
}

.sec-title {
  font-size: 12px;
  font-weight: bold;
  color: rgba($blue, .9);
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid rgba($blue, .2);
}

.trend-chart {
  width: 100%;
  height: 160px;
  background: rgba(0,0,0,.15);
  border-radius: 6px;
}

// Warning list
.warn-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.warn-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  background: rgba(0,0,0,.15);
  border-radius: 4px;
  font-size: 11px;
}

.wi-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  &.pending { background: $red; }
  &.done { background: $green; }
}

.wi-type { font-weight: bold; flex: 1; }
.wi-time { color: $text2; font-size: 10px; }
.wi-status {
  font-size: 9px;
  padding: 1px 5px;
  border-radius: 2px;
  &.pending { background: rgba($red, .2); color: $red; }
  &.done { background: rgba($green, .2); color: $green; }
}

// Actions
.pd-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  padding-top: 12px;
  border-top: 1px solid rgba($blue, .15);
}
</style>
