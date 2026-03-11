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
        <el-button text :icon="Close" @click="selected = null" />
      </div>
      <div class="detail-body">
        <div class="detail-item">
          <span class="di-label">平均心率</span>
          <span class="di-val">{{ selected.data.avgHeartRate || '--' }} <small>bpm</small></span>
        </div>
        <div class="detail-item">
          <span class="di-label">平均血氧</span>
          <span class="di-val">{{ selected.data.avgBloodOxygen || '--' }} <small>%</small></span>
        </div>
        <div class="detail-item">
          <span class="di-label">平均步数</span>
          <span class="di-val">{{ selected.data.avgSteps || '--' }} <small>步</small></span>
        </div>
        <div class="detail-item">
          <span class="di-label">预警次数</span>
          <span class="di-val" :class="selected.data.warningCount > 0 ? 'text-red' : 'text-green'">
            {{ selected.data.warningCount }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, Close } from '@element-plus/icons-vue'
import { getCalendarData } from '@/api/workbench'

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
const selected = ref(null)
function selectDay(cell) {
  if (!cell.data) { selected.value = null; return }
  selected.value = cell
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
  gap: 24px;
  flex-wrap: wrap;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
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
</style>
