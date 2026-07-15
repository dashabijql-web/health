<template>
  <section class="ep-panel ep-history" v-loading="loading">
    <div class="ep-history-head">
      <div>
        <div class="ep-ph ep-history-title"><span class="ep-ph-bar"></span>历史健康数据</div>
        <p>{{ employeeName || '--' }} · {{ employeeCode || '--' }} · {{ rangeLabel }}</p>
      </div>
      <div class="ep-history-filters">
        <el-button-group>
          <el-button v-for="item in quickRanges" :key="item.days" :type="activeDays === item.days ? 'primary' : ''" @click="applyQuickRange(item.days)">{{ item.label }}</el-button>
        </el-button-group>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :clearable="false"
          :disabled-date="disableFuture"
          @change="applyCustomRange"
        />
      </div>
    </div>

    <div class="ep-history-summary">
      <div><span>完整样本</span><strong>{{ trend.totalSamples || 0 }} 条</strong></div>
      <div><span>曲线粒度</span><strong>{{ trend.granularity === 'hour' ? '小时均值' : '日均值' }}</strong></div>
      <div><span>明细总数</span><strong>{{ recordTotal }} 条</strong></div>
    </div>

    <el-tabs v-model="activeTab" class="ep-history-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="历史曲线" name="chart">
        <div class="ep-metric-switches">
          <el-checkbox-group v-model="activeMetrics" @change="renderChart">
            <el-checkbox-button v-for="metric in metrics" :key="metric.key" :value="metric.key">{{ metric.label }}</el-checkbox-button>
          </el-checkbox-group>
        </div>
        <div class="ep-history-chart-wrap">
          <div ref="chartRef" class="ep-history-chart"></div>
          <div v-if="!trend.points?.length" class="ep-history-empty">所选时间段暂无健康数据</div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="明细记录" name="records">
        <el-table :data="records" class="ep-history-table" height="420" empty-text="所选时间段暂无健康记录">
          <el-table-column prop="recordTime" label="采集时间" min-width="168">
            <template #default="{ row }">{{ row.recordTime || row.record_time || row.time || '--' }}</template>
          </el-table-column>
          <el-table-column prop="heartRate" label="心率" width="86"><template #default="{ row }">{{ value(row.heartRate) }}</template></el-table-column>
          <el-table-column prop="bloodOxygen" label="血氧" width="86"><template #default="{ row }">{{ unitValue(row.bloodOxygen, '%') }}</template></el-table-column>
          <el-table-column prop="temperature" label="体温" width="96"><template #default="{ row }">{{ formatHistoryTemperature(row.temperature) }}</template></el-table-column>
          <el-table-column label="血压" width="100"><template #default="{ row }">{{ formatHistoryBloodPressure(row) }}</template></el-table-column>
          <el-table-column prop="pressure" label="压力" width="78"><template #default="{ row }">{{ value(row.pressure) }}</template></el-table-column>
          <el-table-column prop="steps" label="步数" width="100"><template #default="{ row }">{{ value(row.steps) }}</template></el-table-column>
          <el-table-column prop="calories" label="热量" width="100"><template #default="{ row }">{{ unitValue(row.calories, 'kcal') }}</template></el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="recordPage"
          v-model:page-size="recordSize"
          :page-sizes="[20, 50, 100]"
          :total="recordTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="loadRecords"
          @size-change="handleSizeChange"
        />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from '@/utils/echarts-setup'
import { getEmployeeHealthHistory, getHealthRecords } from '@/api/health'
import {
  HISTORY_METRICS,
  buildHistoryChartOption,
  defaultHistoryRange,
  formatHistoryBloodPressure,
  formatHistoryTemperature,
  normalizeHistoryRecords
} from '../employee-profile-history'

const props = defineProps({
  employeeCode: { type: String, default: '' },
  employeeName: { type: String, default: '' }
})

const quickRanges = [{ label: '今日', days: 1 }, { label: '近7日', days: 7 }, { label: '近30日', days: 30 }]
const metrics = HISTORY_METRICS
const activeDays = ref(7)
const dateRange = ref(defaultHistoryRange(7))
const activeTab = ref('chart')
const activeMetrics = ref(['heartRate', 'bloodOxygen'])
const loading = ref(false)
const trend = ref({ points: [], totalSamples: 0, granularity: 'hour' })
const records = ref([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordSize = ref(20)
const chartRef = ref(null)
let chart = null

const rangeLabel = computed(() => dateRange.value?.length === 2 ? `${dateRange.value[0]} 至 ${dateRange.value[1]}` : '--')
const disableFuture = (date) => date.getTime() > Date.now()
const value = (input) => input === null || input === undefined || input === '' ? '--' : input
const unitValue = (input, unit) => value(input) === '--' ? '--' : `${input} ${unit}`

async function loadHistory() {
  if (!props.employeeCode || dateRange.value?.length !== 2) return
  if (!validateRange()) return
  loading.value = true
  try {
    const params = { userCode: props.employeeCode, startDate: dateRange.value[0], endDate: dateRange.value[1] }
    const [trendResult, recordResult] = await Promise.allSettled([
      getEmployeeHealthHistory(params),
      getHealthRecords({ userCode: props.employeeCode, startTime: dateRange.value[0], endTime: dateRange.value[1], current: 1, size: recordSize.value })
    ])
    trend.value = trendResult.status === 'fulfilled' && trendResult.value?.data
      ? trendResult.value.data
      : { points: [], totalSamples: 0, granularity: 'hour' }
    if (recordResult.status === 'fulfilled') applyRecords(recordResult.value?.data)
    else applyRecords(null)
    recordPage.value = 1
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  if (!props.employeeCode || dateRange.value?.length !== 2) return
  const response = await getHealthRecords({
    userCode: props.employeeCode,
    startTime: dateRange.value[0],
    endTime: dateRange.value[1],
    current: recordPage.value,
    size: recordSize.value
  })
  applyRecords(response?.data)
}

function applyRecords(payload) {
  const page = normalizeHistoryRecords(payload)
  records.value = page.records
  recordTotal.value = page.total
}

function applyQuickRange(days) {
  activeDays.value = days
  dateRange.value = defaultHistoryRange(days)
  void loadHistory()
}

function applyCustomRange() {
  activeDays.value = 0
  void loadHistory()
}

function validateRange() {
  const start = new Date(`${dateRange.value[0]}T00:00:00`)
  const end = new Date(`${dateRange.value[1]}T00:00:00`)
  const days = Math.floor((end.getTime() - start.getTime()) / 86400000) + 1
  if (!Number.isFinite(days) || days < 1) {
    ElMessage.warning('结束日期不能早于开始日期')
    return false
  }
  if (days > 365) {
    ElMessage.warning('历史数据查询范围不能超过365天')
    return false
  }
  return true
}

function handleSizeChange() {
  recordPage.value = 1
  void loadRecords()
}

function handleTabChange(name) {
  if (name === 'chart') nextTick(renderChart)
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(buildHistoryChartOption(trend.value, activeMetrics.value), true)
  chart.resize()
}

watch(() => props.employeeCode, () => void loadHistory())
onMounted(() => void loadHistory())
onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped lang="scss">
.ep-history {
  margin: 0 14px;
  padding: 16px;
  background: #0a1428;
  border: 1px solid #1a3153;
  border-radius: 8px;
  color: #c8d8f0;
}
.ep-history-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.ep-history-title { display: flex; align-items: center; gap: 8px; min-height: 28px; color: #d4e7fa; font-size: 14px; font-weight: 700; }
.ep-ph-bar { width: 3px; height: 15px; background: #00b4ff; border-radius: 2px; }
.ep-history-head p { margin: 7px 0 0; color: #6f91ad; font-size: 11px; }
.ep-history-filters { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.ep-history-filters :deep(.el-date-editor) { width: 260px; }
.ep-history-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin: 14px 0 6px; }
.ep-history-summary div { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; padding: 10px 12px; background: #0d192d; border: 1px solid #213653; border-radius: 6px; color: #7191ad; font-size: 11px; }
.ep-history-summary strong { color: #dceeff; font-family: var(--font-mono); }
.ep-history-tabs :deep(.el-tabs__header) { margin: 0 0 12px; }
.ep-history-tabs :deep(.el-tabs__item) { color: #7897b2; }
.ep-history-tabs :deep(.el-tabs__item.is-active) { color: #40c4ff; }
.ep-metric-switches { margin-bottom: 10px; }
.ep-history-chart-wrap { position: relative; }
.ep-history-chart { width: 100%; height: 390px; background: #081224; border: 1px solid #192d48; border-radius: 6px; }
.ep-history-empty { position: absolute; inset: 0; display: grid; place-items: center; color: #56718d; font-size: 12px; pointer-events: none; }
.ep-history-table { width: 100%; }
.ep-history-tabs :deep(.el-pagination) { justify-content: flex-end; margin-top: 12px; }

@media (max-width: 767px) {
  .ep-history { margin: 0 12px; padding: 14px; }
  .ep-history-head { align-items: stretch; flex-direction: column; }
  .ep-history-filters { justify-content: flex-start; }
  .ep-history-filters :deep(.el-date-editor) { width: 100%; }
  .ep-history-summary { grid-template-columns: 1fr; gap: 6px; }
  .ep-history-chart { height: 330px; }
  .ep-metric-switches { overflow-x: auto; padding-bottom: 4px; }
  .ep-metric-switches :deep(.el-checkbox-group) { display: flex; width: max-content; }
  .ep-history-tabs :deep(.el-pagination) { justify-content: flex-start; overflow-x: auto; }
}
</style>
