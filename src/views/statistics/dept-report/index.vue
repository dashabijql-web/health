<template>
  <div class="page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><DataBoard /></el-icon>
        <div>
          <h1 class="main-title">部门健康报表</h1>
          <p class="sub-title">各部门健康指标综合分析</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap primary"><el-icon size="26"><OfficeBuilding /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.totalDepts }}</div>
            <div class="stat-label">部门总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap success"><el-icon size="26"><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.healthiestDept || '--' }}</div>
            <div class="stat-label">最健康部门</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap danger"><el-icon size="26"><WarnTriangleFilled /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.mostWarningsDept || '--' }}</div>
            <div class="stat-label">预警最多部门</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap info"><el-icon size="26"><TrendCharts /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.avgHealthScore }}<span class="unit">分</span></div>
            <div class="stat-label">平均健康评分</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Radar Chart -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="24">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title"><span class="title-bar"></span>部门健康雷达图（Top 5）</div>
          </div>
          <div ref="radarChartRef" class="chart-container" style="height: 240px;"></div>
        </div>
      </el-col>
    </el-row>

    <!-- Data Table -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>部门健康数据</div>
        <span class="total-badge">共 {{ tableData.length }} 个部门</span>
      </div>
      <div class="table-body">
      <el-table :data="tableData" v-loading="loading" stripe height="100%" style="width: 100%"
        :header-cell-style="{ background: '#141830', color: '#7eb8d4', fontWeight: '600', fontSize: '13px' }"
        :row-style="{ background: '#1a1f3a', cursor: 'pointer' }"
        @row-click="onTableRowClick">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="dept_name" label="部门名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="employee_count" label="员工数" width="100" align="center" />
        <el-table-column prop="warning_count" label="预警数" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.warning_count > 10 ? '#ff5252' : row.warning_count > 5 ? '#ffd200' : '#38ef7d', fontWeight: '600' }">
              {{ row.warning_count }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="健康评分" min-width="200">
          <template #default="{ row }">
            <div class="score-cell">
              <el-progress :percentage="calcHealthScore(row.warning_count, row.employee_count)" :color="getScoreColor(calcHealthScore(row.warning_count, row.employee_count))"
                :stroke-width="14" :text-inside="true" :format="() => calcHealthScore(row.warning_count, row.employee_count) + '分'" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="showDeptDetail(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" :title="detailDept?.dept_name + ' - 部门详情'" width="640px"
      @open="onDetailDialogOpen" @closed="onDetailDialogClose">
      <div v-if="detailDept" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="部门名称">{{ detailDept.dept_name }}</el-descriptions-item>
          <el-descriptions-item label="员工数">{{ detailDept.employee_count }} 人</el-descriptions-item>
          <el-descriptions-item label="预警次数">
            <span :style="{ color: detailDept.warning_count > 10 ? '#ff5252' : '#ffd200', fontWeight: '600' }">{{ detailDept.warning_count }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="健康评分">
            <span style="color: #00d4ff; font-weight: 700; font-size: 18px;">{{ calcHealthScore(detailDept.warning_count, detailDept.employee_count) }} 分</span>
          </el-descriptions-item>
          <el-descriptions-item label="人均预警次数">
            <span style="color: #ffd200; font-weight: 600;">
              {{ detailDept.employee_count ? ((detailDept.warning_count || 0) / detailDept.employee_count).toFixed(2) : '--' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <span :style="{ color: getRiskLevel(calcHealthScore(detailDept.warning_count, detailDept.employee_count)).color, fontWeight: '700' }">
              {{ getRiskLevel(calcHealthScore(detailDept.warning_count, detailDept.employee_count)).label }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-chart-title">各部门预警次数对比（当前部门高亮）</div>
        <div ref="detailChartRef" class="detail-chart"></div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { Timer, OfficeBuilding, CircleCheck, WarnTriangleFilled, TrendCharts, View, DataBoard } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDeptHealthSummary } from '@/api/statistics'
import * as echarts from 'echarts'

const currentTime = ref('')
const updateTime = () => { currentTime.value = new Date().toLocaleString('zh-CN') }
updateTime()
const timer = setInterval(updateTime, 1000)

const loading = ref(false)
const tableData = ref([])
const radarChartRef = ref(null)
let radarChart = null

const stats = reactive({ totalDepts: 0, healthiestDept: '--', mostWarningsDept: '--', avgHealthScore: 0 })

// 用人均预警率计算健康分：每人每轮次 3 条预警扣 1 分，最低 0
const calcHealthScore = (wc, ec) => Math.max(0, Math.min(100, Math.round(100 - (wc || 0) / Math.max(ec || 1, 1) * 3)))
const getScoreColor = (s) => s >= 80 ? '#38ef7d' : s >= 60 ? '#ffd200' : '#ff5252'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDeptHealthSummary()
    if (res.code === 200 && res.data) {
      const data = Array.isArray(res.data) ? res.data : []
      tableData.value = data
      stats.totalDepts = data.length
      if (data.length > 0) {
        const sorted = [...data].sort((a, b) => (a.warning_count || 0) - (b.warning_count || 0))
        stats.healthiestDept = sorted[0]?.dept_name || '--'
        stats.mostWarningsDept = sorted[sorted.length - 1]?.dept_name || '--'
        stats.avgHealthScore = Math.round(data.reduce((s, d) => s + calcHealthScore(d.warning_count, d.employee_count), 0) / data.length)
      }
      await nextTick()
      initRadarChart()
    }
  } catch (e) {
    ElMessage.error('加载部门健康数据失败')
  } finally { loading.value = false }
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const top5 = [...tableData.value].sort((a, b) => (b.employee_count || 0) - (a.employee_count || 0)).slice(0, 5)
  if (!top5.length) { radarChart.setOption({ backgroundColor: 'transparent', title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#7eb8d4' } } }); return }
  const maxW = Math.max(...top5.map(d => d.warning_count || 1))
  const maxE = Math.max(...top5.map(d => d.employee_count || 1))
  const radarIndicators = [
    { name: '员工规模', max: 100 },
    { name: '综合健康分', max: 100 },
    { name: '预警控制分', max: 100 }
  ]
  const radarSeriesData = top5.map((d) => {
    const hs = calcHealthScore(d.warning_count, d.employee_count)
    const warnScore = Math.round(Math.max(0, 100 - (d.warning_count || 0) / maxW * 100))
    return { value: [Math.round((d.employee_count || 0) / maxE * 100), hs, warnScore], name: d.dept_name }
  })
  radarChart.setOption({
    backgroundColor: 'transparent',
    legend: { data: top5.map(d => d.dept_name), bottom: 10, textStyle: { color: '#7eb8d4', fontSize: 12 } },
    tooltip: { backgroundColor: 'rgba(20,24,48,.95)', borderColor: '#2d3561', textStyle: { color: '#c8d8e8' }, trigger: 'item' },
    radar: {
      center: ['50%', '45%'], radius: '60%',
      indicator: radarIndicators,
      name: { textStyle: { color: '#7eb8d4' } },
      axisLine: { lineStyle: { color: '#232b4d' } },
      splitLine: { lineStyle: { color: '#232b4d' } },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,.02)', 'rgba(0,212,255,.05)'] } }
    },
    series: [{ type: 'radar', areaStyle: { opacity: 0.15 }, lineStyle: { width: 2 }, symbol: 'circle', symbolSize: 5,
      data: radarSeriesData,
      color: ['#00d4ff', '#38ef7d', '#ffd200', '#ff5252', '#764ba2']
    }]
  })

  // Radar dimension label click handler
  radarChart.on('click', (params) => {
    if (params.componentType === 'radar' && params.targetType === 'axisName') {
      const dimName = params.name
      const dimIndex = radarIndicators.findIndex(ind => ind.name === dimName)
      if (dimIndex === -1) return
      // Find the currently selected dept or the top1 dept
      const activeDept = detailDept.value || top5[0]
      if (!activeDept) return
      const deptSeries = radarSeriesData.find(s => s.name === activeDept.dept_name) || radarSeriesData[0]
      if (!deptSeries) return
      const dimValue = deptSeries.value[dimIndex]
      ElMessage.info(`${activeDept.dept_name} · ${dimName}：${dimValue} 分`)
    }
    // Series data point click
    if (params.componentType === 'series' && params.seriesType === 'radar') {
      const deptName = params.name
      const deptRow = top5.find(d => d.dept_name === deptName)
      if (deptRow) {
        const score = calcHealthScore(deptRow.warning_count, deptRow.employee_count)
        ElMessage.info(`${deptName}：综合评分 ${score} 分`)
      }
    }
  })
}

const detailDialogVisible = ref(false)
const detailDept = ref(null)
const detailChartRef = ref(null)
let detailChart = null

const getRiskLevel = (score) => {
  if (score >= 80) return { label: '低风险', color: '#38ef7d' }
  if (score >= 60) return { label: '中风险', color: '#ffd200' }
  if (score >= 40) return { label: '高风险', color: '#ff9800' }
  return { label: '极高风险', color: '#ff5252' }
}

const initDetailChart = () => {
  if (!detailChartRef.value || !detailDept.value) return
  if (detailChart) detailChart.dispose()
  detailChart = echarts.init(detailChartRef.value)
  const currentName = detailDept.value.dept_name
  const sorted = [...tableData.value]
    .sort((a, b) => (b.warning_count || 0) - (a.warning_count || 0))
    .slice(0, 10)
    .reverse()
  detailChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(20,24,48,.95)', borderColor: '#2d3561', textStyle: { color: '#c8d8e8' } },
    grid: { left: '32%', right: 50, top: 8, bottom: 8, containLabel: false },
    xAxis: { type: 'value', axisLabel: { color: '#7eb8d4', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e2545' } } },
    yAxis: { type: 'category', data: sorted.map(d => d.dept_name), axisLabel: { color: '#7eb8d4', fontSize: 11, width: 90, overflow: 'truncate' } },
    series: [{
      type: 'bar',
      data: sorted.map(d => ({
        value: d.warning_count || 0,
        itemStyle: { color: d.dept_name === currentName ? '#ff5252' : '#00d4ff', borderRadius: [0, 4, 4, 0] }
      })),
      label: { show: true, position: 'right', color: '#7eb8d4', fontSize: 10 }
    }]
  })
}

const onDetailDialogOpen = () => nextTick(() => initDetailChart())
const onDetailDialogClose = () => { detailChart?.dispose(); detailChart = null }
const showDeptDetail = (row) => { detailDept.value = row; detailDialogVisible.value = true }

const onTableRowClick = (row) => {
  const score = calcHealthScore(row.warning_count, row.employee_count)
  ElMessage.info(`${row.dept_name}：综合评分 ${score} 分`)
  showDeptDetail(row)
}

const handleResize = () => { radarChart?.resize(); detailChart?.resize() }

onMounted(() => { fetchData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { clearInterval(timer); window.removeEventListener('resize', handleResize); radarChart?.dispose(); detailChart?.dispose() })
</script>

<style scoped lang="scss">
@import '@/styles/dark-admin.scss';

.page-container {
  height: calc(100vh - 50px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  background: $da-bg;
  color: $da-text;
}
.page-header { @include da-page-header; flex-shrink: 0; }
.page-header-left { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 36px; color: $da-accent; background: rgba(0,212,255,.1); border-radius: 10px; padding: 8px; }
.main-title { font-size: 22px; font-weight: 700; color: #fff; margin: 0 0 2px; letter-spacing: 1px; }
.sub-title { font-size: 12px; color: $da-text-dim; margin: 0; }
.header-time { display: flex; align-items: center; gap: 6px; font-size: 13px; color: $da-text-dim; background: $da-accent-dim; padding: 6px 14px; border-radius: 20px; border: 1px solid $da-accent-hover; }
.mb-16 { margin-bottom: 16px; flex-shrink: 0; }
.stat-card { @include da-stat-card; }
.stat-icon-wrap { @include da-icon-wrap; &.primary { background: linear-gradient(135deg,#667eea,#764ba2); } &.success { background: linear-gradient(135deg,#11998e,#38ef7d); } &.warning { background: linear-gradient(135deg,#f7971e,#ffd200); } &.info { background: linear-gradient(135deg,#4facfe,#00f2fe); } &.danger { background: linear-gradient(135deg,#ff5252,#f48fb1); } }
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: $da-text-bright; line-height: 1.1; .unit { font-size: 14px; font-weight: 400; color: $da-text-dim; margin-left: 2px; } }
.stat-label { font-size: 12px; color: $da-text-dim; margin-top: 4px; }
.panel { @include da-panel; }
.panel.table-panel { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-body { flex: 1; min-height: 0; overflow: hidden; }
.panel-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid $da-border; }
.panel-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: $da-text; }
.title-bar { display: inline-block; width: 3px; height: 16px; background: $da-accent; border-radius: 2px; }
.total-badge { font-size: 12px; color: $da-text-dim; background: $da-accent-dim; border: 1px solid $da-accent-border; padding: 3px 12px; border-radius: 12px; }
.chart-container { width: 100%; padding: 16px; }
.score-cell { padding: 2px 0; :deep(.el-progress-bar__outer) { background: rgba(255,255,255,.08); border-radius: 7px; } :deep(.el-progress-bar__innerText) { font-size: 11px; font-weight: 600; } }
.detail-content {
  .detail-chart-title { margin: 16px 0 8px; font-size: 12px; color: $da-text-dim; font-weight: 600; }
  .detail-chart { width: 100%; height: 260px; }
}
@include da-el-overrides;
</style>
