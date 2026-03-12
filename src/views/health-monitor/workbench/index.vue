<template>
  <div class="wb-page">
    <!-- 头部：月份切换 -->
    <div class="wb-header">
      <div class="wb-title">工作台日历</div>
      <div class="wb-nav">
        <el-button :icon="ArrowLeft" circle size="small" @click="prevMonth" />
        <span class="wb-month-label">{{ yearLabel }}年 {{ monthLabel }}月</span>
        <el-button :icon="ArrowRight" circle size="small" @click="nextMonth" :disabled="isCurrentMonth" />
      </div>
      <div class="wb-legend">
        <span class="leg-dot leg-good"></span><span>健康</span>
        <span class="leg-dot leg-warn"></span><span>有预警</span>
        <span class="leg-dot leg-empty"></span><span>无数据</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="wb-stats">
      <div class="stat-card">
        <div class="sc-val">{{ summary.totalDays }}</div>
        <div class="sc-lbl">有数据天数</div>
      </div>
      <div class="stat-card">
        <div class="sc-val text-green">{{ summary.goodDays }}</div>
        <div class="sc-lbl">健康天数</div>
      </div>
      <div class="stat-card">
        <div class="sc-val text-orange">{{ summary.warnDays }}</div>
        <div class="sc-lbl">有预警天数</div>
      </div>
      <div class="stat-card">
        <div class="sc-val">{{ summary.avgHR || '--' }}</div>
        <div class="sc-lbl">月均心率 (bpm)</div>
      </div>
      <div class="stat-card">
        <div class="sc-val">{{ summary.avgBO || '--' }}</div>
        <div class="sc-lbl">月均血氧 (%)</div>
      </div>
      <div class="stat-card">
        <div class="sc-val text-red">{{ summary.totalWarnings }}</div>
        <div class="sc-lbl">月度总预警</div>
      </div>
    </div>

    <!-- 日历主体 -->
    <div class="wb-calendar" v-loading="loading">
      <!-- 星期标题 -->
      <div class="cal-week-row">
        <div class="cal-week-cell" v-for="w in weekDays" :key="w">{{ w }}</div>
      </div>
      <!-- 日期格子 -->
      <div class="cal-body">
        <div
          v-for="(cell, idx) in calCells"
          :key="idx"
          class="cal-cell"
          :class="cellClass(cell)"
          @click="cell.day && selectDay(cell)"
        >
          <template v-if="cell.day">
            <div class="cell-day">{{ cell.day }}</div>
            <template v-if="cell.data">
              <div class="cell-metrics">
                <span class="cm-item cm-hr" title="心率">❤ {{ cell.data.avgHeartRate || '--' }}</span>
                <span class="cm-item cm-bo" title="血氧">🩸 {{ cell.data.avgBloodOxygen || '--' }}</span>
              </div>
              <div class="cell-warn" v-if="cell.data.warningCount > 0">
                <el-badge :value="cell.data.warningCount" type="danger" class="warn-badge" />
              </div>
            </template>
            <div v-else class="cell-no-data">--</div>
          </template>
        </div>
      </div>
    </div>

    <!-- 选中日期详情 -->
    <div class="wb-detail" v-if="selected">
      <div class="detail-header">
        <span class="detail-date">{{ selected.dateStr }} 详情</span>
        <el-button text :icon="Close" @click="selected = null; activeRank = null" />
      </div>
      <div class="detail-body">
        <div class="detail-item di-clickable" :class="{ 'di-active': activeRank === 'heartRate' }" @click="toggleRank('heartRate')">
          <span class="di-label">平均心率</span>
          <span class="di-val">{{ selected.data.avgHeartRate || '--' }} <small>bpm</small></span>
          <span class="di-hint">排行 ▾</span>
        </div>
        <div class="detail-item di-clickable" :class="{ 'di-active': activeRank === 'bloodOxygen' }" @click="toggleRank('bloodOxygen')">
          <span class="di-label">平均血氧</span>
          <span class="di-val">{{ selected.data.avgBloodOxygen || '--' }} <small>%</small></span>
          <span class="di-hint">排行 ▾</span>
        </div>
        <div class="detail-item di-clickable" :class="{ 'di-active': activeRank === 'steps' }" @click="toggleRank('steps')">
          <span class="di-label">平均步数</span>
          <span class="di-val">{{ selected.data.avgSteps || '--' }} <small>步</small></span>
          <span class="di-hint">排行 ▾</span>
        </div>
        <div class="detail-item di-clickable" :class="{ 'di-active': activeRank === 'warnings' }" @click="toggleRank('warnings')">
          <span class="di-label">预警次数</span>
          <span class="di-val" :class="selected.data.warningCount > 0 ? 'text-red' : 'text-green'">{{ selected.data.warningCount }}</span>
          <span class="di-hint">列表 ▾</span>
        </div>
      </div>

      <!-- 排行榜 / 预警列表 -->
      <div class="rank-panel" v-if="activeRank" v-loading="rankLoading">
        <div class="rank-title">{{ rankTitle }}</div>
        <div v-if="!rankLoading && rankData.length === 0" class="rank-empty">暂无数据</div>
        <!-- 心率 / 血氧 / 步数排行 -->
        <template v-if="activeRank !== 'warnings'">
          <div class="rank-row" v-for="(row, i) in rankData" :key="i">
            <span class="rank-no" :class="i < 3 ? 'rank-top' : ''">{{ i + 1 }}</span>
            <span class="rank-name">{{ row.empName }}</span>
            <span class="rank-dept">{{ row.deptName }}</span>
            <span class="rank-val" :class="rankValClass(row)">
              <template v-if="activeRank === 'heartRate'">{{ row.avgHeartRate }} <small>bpm</small></template>
              <template v-else-if="activeRank === 'bloodOxygen'">{{ row.avgBloodOxygen }} <small>%</small></template>
              <template v-else>{{ row.avgSteps }} <small>步</small></template>
            </span>
          </div>
        </template>
        <!-- 预警列表 -->
        <template v-else>
          <div class="rank-row warn-row" v-for="(row, i) in rankData" :key="i">
            <span class="rank-no" :class="i < 3 ? 'rank-top' : ''">{{ i + 1 }}</span>
            <span class="rank-name">{{ row.empName }}</span>
            <span class="rank-dept">{{ row.deptName }}</span>
            <span class="warn-type">{{ row.warningType || row.indicatorName }}</span>
            <span class="warn-val">{{ row.warningValue }}</span>
            <span class="warn-level" :class="warnLevelClass(row.warningLevel)">{{ warnLevelLabel(row.warningLevel) }}</span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, Close } from '@element-plus/icons-vue'
import { getCalendarData, getDayHeartRateRank, getDayBloodOxygenRank, getDayStepsRank, getDayWarnings } from '@/api/workbench'

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// ── 当前月份 ────────────────────────────────────────────────────────
const today    = new Date()
const curYear  = ref(today.getFullYear())
const curMonth = ref(today.getMonth() + 1)   // 1-based

const yearLabel  = computed(() => curYear.value)
const monthLabel = computed(() => String(curMonth.value).padStart(2, '0'))
const isCurrentMonth = computed(() =>
  curYear.value === today.getFullYear() && curMonth.value === today.getMonth() + 1
)

function prevMonth() {
  if (curMonth.value === 1) { curYear.value--; curMonth.value = 12 }
  else curMonth.value--
}
function nextMonth() {
  if (isCurrentMonth.value) return
  if (curMonth.value === 12) { curYear.value++; curMonth.value = 1 }
  else curMonth.value++
}

// ── 数据加载 ────────────────────────────────────────────────────────
const loading  = ref(false)
const dayData  = ref({})   // { "2026-03-05": { avgHeartRate, avgBloodOxygen, avgSteps, warningCount } }

async function loadData() {
  loading.value = true
  try {
    const res = await getCalendarData(curYear.value, curMonth.value)
    const map = {}
    if (res.code === 200 && Array.isArray(res.data)) {
      for (const row of res.data) {
        map[row.date] = row
      }
    }
    dayData.value = map
  } catch (e) {
    console.error('[workbench] loadData error', e)
  } finally {
    loading.value = false
  }
}

watch([curYear, curMonth], loadData)
onMounted(loadData)

// ── 日历格子生成 ─────────────────────────────────────────────────────
const calCells = computed(() => {
  const cells = []
  const firstDay = new Date(curYear.value, curMonth.value - 1, 1).getDay()  // 0=Sun
  const daysInMonth = new Date(curYear.value, curMonth.value, 0).getDate()

  // 补充前空白
  for (let i = 0; i < firstDay; i++) cells.push({ day: 0 })
  // 实际日期
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${curYear.value}-${String(curMonth.value).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    const data = dayData.value[dateStr] || null
    const isToday = dateStr === today.toISOString().slice(0, 10)
    cells.push({ day: d, dateStr, data, isToday })
  }
  // 补充后空白到 7 的倍数
  while (cells.length % 7 !== 0) cells.push({ day: 0 })
  return cells
})

// ── 统计摘要 ─────────────────────────────────────────────────────────
const summary = computed(() => {
  const rows  = Object.values(dayData.value)
  const totalDays    = rows.length
  const warnDays     = rows.filter(r => r.warningCount > 0).length
  const goodDays     = rows.filter(r => r.warningCount === 0 && r.avgHeartRate).length
  const totalWarnings = rows.reduce((s, r) => s + (r.warningCount || 0), 0)
  const hrRows = rows.filter(r => r.avgHeartRate)
  const boRows = rows.filter(r => r.avgBloodOxygen)
  const avgHR  = hrRows.length ? Math.round(hrRows.reduce((s, r) => s + r.avgHeartRate, 0) / hrRows.length) : 0
  const avgBO  = boRows.length ? (boRows.reduce((s, r) => s + r.avgBloodOxygen, 0) / boRows.length).toFixed(1) : 0
  return { totalDays, goodDays, warnDays, totalWarnings, avgHR: avgHR || 0, avgBO: avgBO || 0 }
})

// ── 格子样式 ─────────────────────────────────────────────────────────
function cellClass(cell) {
  if (!cell.day) return 'cal-cell-empty'
  const cls = []
  if (cell.isToday) cls.push('cal-cell-today')
  if (cell.data) {
    cls.push(cell.data.warningCount > 0 ? 'cal-cell-warn' : 'cal-cell-good')
  } else {
    cls.push('cal-cell-nodata')
  }
  if (selected.value?.dateStr === cell.dateStr) cls.push('cal-cell-selected')
  return cls
}

// ── 选中详情 ─────────────────────────────────────────────────────────
const selected   = ref(null)
const activeRank = ref(null)   // 'heartRate' | 'bloodOxygen' | 'steps' | 'warnings' | null
const rankData   = ref([])
const rankLoading = ref(false)

function selectDay(cell) {
  if (!cell.data) { selected.value = null; activeRank.value = null; return }
  if (selected.value?.dateStr !== cell.dateStr) activeRank.value = null
  selected.value = cell
}

const rankTitle = computed(() => {
  const map = { heartRate: '心率排行（偏差最大）', bloodOxygen: '血氧排行（最低）', steps: '步数排行（最少）', warnings: '当日预警列表（严重优先）' }
  return map[activeRank.value] || ''
})

async function toggleRank(type) {
  if (activeRank.value === type) { activeRank.value = null; return }
  activeRank.value = type
  rankData.value = []
  rankLoading.value = true
  try {
    const date = selected.value.dateStr
    const apiFn = { heartRate: getDayHeartRateRank, bloodOxygen: getDayBloodOxygenRank, steps: getDayStepsRank, warnings: getDayWarnings }[type]
    const res = await apiFn(date)
    rankData.value = res.code === 200 ? res.data : []
  } finally {
    rankLoading.value = false
  }
}

function rankValClass(row) {
  if (activeRank.value === 'heartRate') {
    const v = row.avgHeartRate
    return (v < 60 || v > 100) ? 'text-red' : 'text-green'
  }
  if (activeRank.value === 'bloodOxygen') return row.avgBloodOxygen < 95 ? 'text-red' : 'text-green'
  if (activeRank.value === 'steps') return row.avgSteps < 6000 ? 'text-red' : ''
  return ''
}

function warnLevelLabel(level) {
  return level || '—'
}

function warnLevelClass(level) {
  if (!level) return ''
  if (['危急','高危','高','危险'].includes(level)) return 'wl-high'
  if (['中','警告'].includes(level)) return 'wl-mid'
  if (['低'].includes(level)) return 'wl-low'
  return ''
}
</script>

<style scoped lang="scss">
.wb-page {
  padding: 20px;
  color: #e0e6f0;
}

/* 头部 */
.wb-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.wb-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}
.wb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.wb-month-label {
  font-size: 16px;
  font-weight: 600;
  min-width: 110px;
  text-align: center;
  color: #93c5fd;
}
.wb-legend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  margin-left: auto;
}
.leg-dot {
  width: 10px; height: 10px; border-radius: 50%; display: inline-block;
}
.leg-good  { background: #22c55e; }
.leg-warn  { background: #f97316; }
.leg-empty { background: #374151; }

/* 统计卡片 */
.wb-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1;
  min-width: 100px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  padding: 12px 16px;
  text-align: center;
}
.sc-val {
  font-size: 24px;
  font-weight: 700;
  color: #93c5fd;
}
.sc-lbl {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
.text-green  { color: #22c55e !important; }
.text-orange { color: #f97316 !important; }
.text-red    { color: #ef4444 !important; }

/* 日历 */
.wb-calendar {
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px;
  overflow: hidden;
}
.cal-week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: rgba(255,255,255,0.07);
}
.cal-week-cell {
  padding: 8px 0;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #9ca3af;
}
.cal-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.cal-cell {
  min-height: 90px;
  padding: 8px;
  border: 1px solid rgba(255,255,255,0.05);
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
  &:hover { background: rgba(255,255,255,0.06); }
}
.cal-cell-empty {
  background: rgba(0,0,0,0.15);
  cursor: default;
  &:hover { background: rgba(0,0,0,0.15); }
}
.cal-cell-today {
  border-color: #3b82f6 !important;
  .cell-day { color: #60a5fa; font-weight: 700; }
}
.cal-cell-good   { background: rgba(34,197,94,0.08); }
.cal-cell-warn   { background: rgba(249,115,22,0.1); }
.cal-cell-nodata { background: rgba(255,255,255,0.02); }
.cal-cell-selected { outline: 2px solid #3b82f6; }

.cell-day {
  font-size: 14px;
  font-weight: 600;
  color: #d1d5db;
  margin-bottom: 4px;
}
.cell-metrics {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cm-item {
  font-size: 11px;
  color: #9ca3af;
}
.cm-hr { color: #f87171; }
.cm-bo { color: #60a5fa; }
.cell-no-data {
  font-size: 12px;
  color: #4b5563;
  margin-top: 8px;
}
.cell-warn {
  position: absolute;
  top: 6px;
  right: 6px;
}
:deep(.warn-badge .el-badge__content) {
  font-size: 10px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
}

/* 详情面板 */
.wb-detail {
  margin-top: 16px;
  background: rgba(59,130,246,0.1);
  border: 1px solid rgba(59,130,246,0.3);
  border-radius: 8px;
  padding: 12px 16px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.detail-date {
  font-weight: 600;
  color: #93c5fd;
  font-size: 15px;
}
.detail-body {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  border-radius: 6px;
}
.di-clickable {
  cursor: pointer;
  border: 1px solid rgba(255,255,255,0.08);
  transition: background 0.15s, border-color 0.15s;
  &:hover { background: rgba(59,130,246,0.12); border-color: rgba(59,130,246,0.3); }
}
.di-active {
  background: rgba(59,130,246,0.2) !important;
  border-color: #3b82f6 !important;
}
.di-hint {
  font-size: 10px;
  color: #6b7280;
  margin-top: 2px;
}
.di-label {
  font-size: 12px;
  color: #9ca3af;
}
.di-val {
  font-size: 18px;
  font-weight: 600;
  color: #e2e8f0;
  small { font-size: 11px; color: #9ca3af; }
}

/* 排行榜面板 */
.rank-panel {
  margin-top: 12px;
  border-top: 1px solid rgba(255,255,255,0.1);
  padding-top: 10px;
  max-height: 280px;
  overflow-y: auto;
}
.rank-title {
  font-size: 12px;
  color: #60a5fa;
  font-weight: 600;
  margin-bottom: 8px;
}
.rank-empty {
  font-size: 12px;
  color: #6b7280;
  padding: 12px 0;
  text-align: center;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 4px;
  border-radius: 4px;
  font-size: 13px;
  &:hover { background: rgba(255,255,255,0.04); }
}
.rank-no {
  width: 20px;
  text-align: center;
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
}
.rank-top { color: #f59e0b; font-weight: 700; }
.rank-name { width: 64px; color: #e2e8f0; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-dept { flex: 1; color: #9ca3af; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-val { width: 80px; text-align: right; font-weight: 600; color: #93c5fd; small { font-size: 10px; color: #6b7280; } }
.warn-type { flex: 1; color: #fbbf24; font-size: 12px; }
.warn-val  { width: 50px; text-align: right; color: #e2e8f0; font-size: 12px; }
.warn-level { width: 24px; text-align: center; font-size: 11px; font-weight: 700; border-radius: 3px; padding: 1px 4px; flex-shrink: 0; }
.wl-low  { background: rgba(234,179,8,0.2);  color: #fbbf24; }
.wl-mid  { background: rgba(249,115,22,0.2); color: #fb923c; }
.wl-high { background: rgba(239,68,68,0.2);  color: #f87171; }
</style>
