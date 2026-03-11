<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <el-icon class="header-icon"><Calendar /></el-icon>
        <div>
          <h1 class="main-title">月度统计</h1>
          <p class="sub-title">按月查看健康数据汇总</p>
        </div>
      </div>
      <div class="header-time"><el-icon><Timer /></el-icon>{{ currentTime }}</div>
    </div>

    <!-- Filter Bar -->
    <div class="panel mb-16">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>筛选条件</div>
      </div>
      <div class="search-form">
        <el-date-picker v-model="filterMonth" type="month" placeholder="选择月份" format="YYYY-MM" value-format="YYYY-MM" :clearable="false" style="width:180px;" />
        <el-select v-model="filterDept" placeholder="全部部门" clearable style="width:200px;">
          <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName||d.dept_name" :value="d.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
        <div style="flex:1;"></div>
        <el-button type="success" :icon="Download" @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap primary"><el-icon size="26"><Document /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ stats.totalRecords }}</div><div class="stat-label">总记录数</div></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap success"><el-icon size="26"><User /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ stats.activeEmployees }}</div><div class="stat-label">活跃员工</div></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon-wrap info"><el-icon size="26"><TrendCharts /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ stats.avgHealthScore }}<span class="unit">分</span></div><div class="stat-label">平均健康评分</div></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-clickable" @click="showWarningRateDetail">
          <div class="stat-icon-wrap warning"><el-icon size="26"><WarnTriangleFilled /></el-icon></div>
          <div class="stat-body"><div class="stat-value">{{ stats.warningRate }}<span class="unit">%</span></div><div class="stat-label">预警率 <el-icon style="font-size:11px;opacity:.6;margin-left:2px;"><QuestionFilled /></el-icon></div></div>
        </div>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="14">
        <div class="panel"><div class="panel-header"><div class="panel-title"><span class="title-bar"></span>每日记录趋势<span class="chart-hint">（点击查看当日详情）</span></div></div>
          <div ref="lineChartRef" class="chart-container" style="height:240px;"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="panel"><div class="panel-header"><div class="panel-title"><span class="title-bar"></span>预警类型分布<span class="chart-hint">（点击查看类型详情）</span></div></div>
          <div ref="pieChartRef" class="chart-container" style="height:240px;"></div>
        </div>
      </el-col>
    </el-row>

    <!-- Data Table -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="title-bar"></span>月度员工健康汇总<span class="table-hint">（点击行查看健康画像）</span></div>
        <span class="total-badge">共 {{ filteredData.length }} 条记录</span>
      </div>
      <div class="table-body">
      <el-table :data="pagedData" v-loading="loading" element-loading-background="rgba(20,24,48,0.85)" stripe height="100%" style="width:100%"
        :header-cell-style="{ background:'#141830', color:'#7eb8d4', fontWeight:'600', fontSize:'13px' }"
        :row-style="rowStyle"
        @row-click="goToPortrait">
        <el-table-column type="index" label="#" width="50" align="center" :index="(i) => (page.cur-1)*page.size+i+1" />
        <el-table-column prop="emp_name" label="姓名" min-width="110" />
        <el-table-column prop="dept_name" label="部门" min-width="130" />
        <el-table-column prop="record_count" label="记录数" width="90" align="center" sortable />
        <el-table-column label="平均心率" width="110" align="center">
          <template #default="{row}"><span :style="{color:hrColor(row.avg_heart_rate)}">{{ row.avg_heart_rate!=null?Number(row.avg_heart_rate).toFixed(1):'--' }}</span></template>
        </el-table-column>
        <el-table-column label="平均血氧" width="110" align="center">
          <template #default="{row}"><span :style="{color:spoColor(row.avg_blood_oxygen)}">{{ row.avg_blood_oxygen!=null?Number(row.avg_blood_oxygen).toFixed(1):'--' }}</span></template>
        </el-table-column>
        <el-table-column label="平均体温" width="110" align="center">
          <template #default="{row}"><span :style="{color:tempColor(row.avg_temperature)}">{{ row.avg_temperature!=null?Number(row.avg_temperature).toFixed(1):'--' }}</span></template>
        </el-table-column>
        <el-table-column label="健康评分" min-width="180">
          <template #default="{row}">
            <el-progress :percentage="row._score||0" :color="scoreColor(row._score||0)" :stroke-width="14" :text-inside="true" :format="()=>(row._score||0)+'分'" />
          </template>
        </el-table-column>
      </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination background layout="total,sizes,prev,pager,next,jumper" :current-page="page.cur" :page-sizes="[10,20,50]" :page-size="page.size" :total="filteredData.length" @size-change="s=>{page.size=s;page.cur=1}" @current-change="p=>{page.cur=p}" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Timer, Calendar, Search, RefreshRight, Download, Document, User, TrendCharts, WarnTriangleFilled, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMonthlySummary, getDailyCounts, getWarningTypes } from '@/api/statistics'
import { getDepartmentList } from '@/api/department'
import * as echarts from 'echarts'

const router = useRouter()

const currentTime = ref('')
const updateTime = () => { currentTime.value = new Date().toLocaleString('zh-CN') }
updateTime()
const tmr = setInterval(updateTime, 1000)

const now = new Date()
const defaultMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`
const filterMonth = ref(defaultMonth)
const filterDept = ref('')
const deptOptions = ref([])
const loading = ref(false)
const allData = ref([])
const dailyCounts = ref([])
const warningTypes = ref([])
const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null, pieChart = null
const page = reactive({ cur: 1, size: 20 })

const filteredData = computed(() => {
  if (!filterDept.value) return allData.value
  // dept_id 从后端返回为 Number，filterDept 来自 el-option :value="d.id"，用 == 兼容类型
  return allData.value.filter(r => r.dept_id == filterDept.value)
})
const pagedData = computed(() => {
  const s = (page.cur-1)*page.size
  return filteredData.value.slice(s, s+page.size)
})

const stats = reactive({ totalRecords: 0, activeEmployees: 0, avgHealthScore: '--', warningRate: '--' })
const computeStats = () => {
  const d = filteredData.value
  stats.totalRecords = d.reduce((s,r) => s+(r.record_count||0), 0)
  stats.activeEmployees = d.length
  if (d.length) {
    stats.avgHealthScore = Math.round(d.reduce((s,r) => s+(r._score||0), 0)/d.length)
    const totalWarnings = warningTypes.value.reduce((s, r) => s + Number(r.value), 0)
    stats.warningRate = stats.totalRecords > 0 ? ((totalWarnings / stats.totalRecords) * 100).toFixed(1) : '--'
  } else { stats.avgHealthScore = '--'; stats.warningRate = '--' }
}

const hrColor = v => v==null?'#7eb8d4':v>=60&&v<=100?'#38ef7d':v>100||v<50?'#ff5252':'#ffd200'
const spoColor = v => v==null?'#7eb8d4':v>=95?'#38ef7d':v>=90?'#ffd200':'#ff5252'
const tempColor = v => v==null?'#7eb8d4':v>=36&&v<=37.3?'#38ef7d':v>37.3&&v<=38?'#ffd200':'#ff5252'
const scoreColor = s => s>=80?'#38ef7d':s>=60?'#ffd200':'#ff5252'

// Table row style — includes cursor:pointer for click affordance
const rowStyle = () => ({ background: '#1a1f3a', cursor: 'pointer' })

// Navigate to health portrait on row click
const goToPortrait = (row) => {
  if (row.emp_code || row.empCode) {
    router.push({ path: '/personnel-management/health-portrait', query: { empCode: row.emp_code || row.empCode } })
  } else {
    ElMessage.info('该员工暂无画像数据')
  }
}

// Stat card — warning rate explanation
const showWarningRateDetail = () => {
  const totalWarnings = warningTypes.value.reduce((s, r) => s + Number(r.value), 0)
  ElMessage.info(`预警率 = 预警总数 / 记录总数 × 100%（${totalWarnings} / ${stats.totalRecords} × 100%）`)
}

const fetchDepts = async () => {
  try { const r = await getDepartmentList(); if (r.code===200) deptOptions.value = Array.isArray(r.data)?r.data:(r.data?.list||[]) } catch(e) {}
}

const fetchData = async () => {
  loading.value = true
  try {
    const [sumRes, dayRes, typeRes] = await Promise.all([
      getMonthlySummary({ month: filterMonth.value }),
      getDailyCounts({ month: filterMonth.value }),
      getWarningTypes({ month: filterMonth.value })
    ])
    if (sumRes.code === 200 && sumRes.data) {
      const list = Array.isArray(sumRes.data) ? sumRes.data : []
      list.forEach(r => { r._score = r.health_score != null ? r.health_score : 75 })
      allData.value = list
    }
    dailyCounts.value = (dayRes.code === 200 && Array.isArray(dayRes.data)) ? dayRes.data : []
    warningTypes.value = (typeRes.code === 200 && Array.isArray(typeRes.data)) ? typeRes.data : []
  } catch (e) { ElMessage.error('加载月度数据失败') }
  finally { loading.value = false; computeStats(); await nextTick(); initLineChart(); initPieChart() }
}

const initLineChart = () => {
  if (!lineChartRef.value) return
  if (lineChart) lineChart.dispose()
  lineChart = echarts.init(lineChartRef.value)
  const [y, m] = filterMonth.value.split('-').map(Number)
  const days = new Date(y, m, 0).getDate()
  const xData = Array.from({length: days}, (_,i) => i+1)
  // 将后端返回的 [{day, count}] 转换为按天索引的数组
  const countMap = {}
  dailyCounts.value.forEach(r => { countMap[Number(r.day)] = Number(r.count) })
  const yData = xData.map(d => countMap[d] || 0)
  lineChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger:'axis', backgroundColor:'rgba(20,24,48,.95)', borderColor:'#2d3561', textStyle:{color:'#c8d8e8'} },
    grid: { left:50, right:20, top:30, bottom:40 },
    xAxis: { type:'category', data:xData, axisLine:{lineStyle:{color:'#232b4d'}}, axisLabel:{color:'#7eb8d4'}, splitLine:{lineStyle:{color:'#1e2545'}} },
    yAxis: { type:'value', axisLine:{lineStyle:{color:'#232b4d'}}, axisLabel:{color:'#7eb8d4'}, splitLine:{lineStyle:{color:'#1e2545'}} },
    series: [{ type:'line', data:yData, smooth:true, showSymbol:true, symbolSize:6, lineStyle:{color:'#00d4ff',width:2}, itemStyle:{color:'#00d4ff'},
      areaStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,212,255,.35)'},{offset:1,color:'rgba(0,212,255,0)'}]) }
    }]
  })
  // Line chart click — show day summary
  lineChart.off('click')
  lineChart.on('click', (params) => {
    const day = params.dataIndex + 1
    const [, mon] = filterMonth.value.split('-')
    ElMessage.info(`${filterMonth.value.replace('-', '年')}月${day}日 共 ${params.value} 条记录`)
  })
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)
  const colors = ['#1890ff','#52c41a','#faad14','#ff5252','#ff9800','#722ed1','#00d4ff','#38ef7d']
  const hasData = warningTypes.value.length > 0
  const data = hasData
    ? warningTypes.value.map((r, i) => ({ value: Number(r.value), name: r.name, itemStyle: { color: colors[i % colors.length] } }))
    : [{ value: 1, name: '暂无数据', itemStyle: { color: '#2d3561' } }]
  pieChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger:'item', backgroundColor:'rgba(20,24,48,.95)', borderColor:'#2d3561', textStyle:{color:'#c8d8e8'}, formatter:'{b}: {c} ({d}%)' },
    legend: { orient:'vertical', right:10, top:'center', textStyle:{color:'#7eb8d4',fontSize:12} },
    series: [{ type:'pie', radius:['40%','65%'], center:['40%','50%'],
      itemStyle:{borderColor:'#141830',borderWidth:2,borderRadius:4},
      label:{show:true,color:'#c8d8e8',fontSize:11,formatter:'{b}\n{d}%'}, labelLine:{lineStyle:{color:'#2d3561'}},
      data
    }]
  })
  // Pie chart click — show type count and highlight slice
  pieChart.off('click')
  pieChart.on('click', (params) => {
    if (params.name === '暂无数据') return
    ElMessage.info(`${params.name}：${params.value} 条`)
    // Highlight the clicked slice with emphasis; clicking again cancels
    pieChart.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex: params.dataIndex })
    setTimeout(() => {
      pieChart.dispatchAction({ type: 'downplay', seriesIndex: 0, dataIndex: params.dataIndex })
    }, 1500)
  })
}

const handleSearch = () => { page.cur=1; fetchData() }
const handleReset = () => { filterMonth.value=defaultMonth; filterDept.value=''; page.cur=1; fetchData() }
const handleExport = () => {
  if (!filteredData.value.length) { ElMessage.warning('暂无数据可导出'); return }
  const headers = ['序号', '姓名', '部门', '记录数', '平均心率(bpm)', '平均血氧(%)', '平均体温(℃)', '健康评分']
  const rows = filteredData.value.map((r, i) => [
    i + 1,
    r.emp_name || '--',
    r.dept_name || '--',
    r.record_count || 0,
    r.avg_heart_rate != null ? Number(r.avg_heart_rate).toFixed(1) : '--',
    r.avg_blood_oxygen != null ? Number(r.avg_blood_oxygen).toFixed(1) : '--',
    r.avg_temperature != null ? Number(r.avg_temperature).toFixed(1) : '--',
    r._score || 0
  ])
  const csv = [headers, ...rows].map(r => r.join(',')).join('\r\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `月度健康统计_${filterMonth.value}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}
watch(filteredData, computeStats)
watch(filterDept, () => { page.cur = 1 })
const onResize = () => { lineChart?.resize(); pieChart?.resize() }
onMounted(async () => { await fetchDepts(); await fetchData(); window.addEventListener('resize', onResize) })
onUnmounted(() => { clearInterval(tmr); window.removeEventListener('resize', onResize); lineChart?.dispose(); pieChart?.dispose() })
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
.stat-card-clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  &:hover {
    border-color: rgba(0, 212, 255, 0.5);
    box-shadow: 0 0 12px rgba(0, 212, 255, 0.18);
  }
}
.stat-icon-wrap { @include da-icon-wrap; &.primary { background: linear-gradient(135deg,#667eea,#764ba2); } &.success { background: linear-gradient(135deg,#11998e,#38ef7d); } &.warning { background: linear-gradient(135deg,#f7971e,#ffd200); } &.info { background: linear-gradient(135deg,#4facfe,#00f2fe); } &.danger { background: linear-gradient(135deg,#ff5252,#f48fb1); } }
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: $da-text-bright; line-height: 1.1; .unit { font-size: 14px; font-weight: 400; color: $da-text-dim; margin-left: 2px; } }
.stat-label { font-size: 12px; color: $da-text-dim; margin-top: 4px; display: flex; align-items: center; }
.panel { @include da-panel; }
.panel-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid $da-border; flex-shrink: 0; }
.panel-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: $da-text; }
.title-bar { display: inline-block; width: 3px; height: 16px; background: $da-accent; border-radius: 2px; }
.chart-hint { font-size: 11px; font-weight: 400; color: $da-text-dim; margin-left: 4px; opacity: 0.7; }
.table-hint { font-size: 11px; font-weight: 400; color: $da-text-dim; margin-left: 4px; opacity: 0.7; }
.total-badge { font-size: 12px; color: $da-text-dim; background: $da-accent-dim; border: 1px solid $da-accent-border; padding: 3px 12px; border-radius: 12px; }
.search-form { display: flex; align-items: center; gap: 12px; padding: 14px 20px; flex-wrap: wrap; }
.chart-container { width: 100%; padding: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 14px 20px; border-top: 1px solid $da-border; flex-shrink: 0; }
.table-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.table-body { flex: 1; min-height: 0; overflow: hidden; }
@include da-el-overrides;
</style>
