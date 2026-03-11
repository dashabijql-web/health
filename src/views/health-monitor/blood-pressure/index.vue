<template>
  <div class="bp-page">
    <!-- 顶部筛选栏 -->
    <div class="bp-toolbar">
      <span class="bp-toolbar-title">血压分析</span>
      <div class="bp-toolbar-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          :disabled-date="d => d > new Date()"
          value-format="YYYY-MM-DD"
          style="width:240px"
          @change="onDateChange"
        />
        <el-select v-model="trendDays" size="small" style="width:100px;margin-left:8px" @change="loadTrend">
          <el-option label="近7天" :value="7" />
          <el-option label="近30天" :value="30" />
          <el-option label="近90天" :value="90" />
        </el-select>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="bp-overview">
      <div class="bp-ov-card" v-for="card in overviewCards" :key="card.key">
        <div class="bp-ov-icon" :style="{ background: card.bg }">
          <el-icon :color="card.color" :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="bp-ov-info">
          <div class="bp-ov-val" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="bp-ov-label">{{ card.label }}</div>
        </div>
        <div class="bp-ov-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 中部图表区 -->
    <div class="bp-charts-row">
      <!-- 趋势折线图 -->
      <div class="bp-panel bp-panel-trend">
        <div class="bp-panel-hd">
          <span class="bp-panel-dot"></span>
          <span class="bp-panel-title">血压趋势</span>
          <span class="bp-panel-sub">收缩压 / 舒张压 日均值</span>
        </div>
        <div ref="trendChart" class="bp-chart-area"></div>
      </div>

      <!-- 分布饼图 -->
      <div class="bp-panel bp-panel-dist">
        <div class="bp-panel-hd">
          <span class="bp-panel-dot"></span>
          <span class="bp-panel-title">血压分布</span>
        </div>
        <div ref="distChart" class="bp-chart-area"></div>
        <ul class="bp-dist-legend" v-if="distData.length">
          <li v-for="d in distData" :key="d.name">
            <span class="bp-legend-dot" :style="{ background: d.color }"></span>
            <span class="bp-legend-name">{{ d.name }}</span>
            <span class="bp-legend-val">{{ d.value }}%</span>
          </li>
        </ul>
        <div v-else class="bp-empty">暂无数据</div>
      </div>
    </div>

    <!-- 下部：TOP5 + 部门统计 -->
    <div class="bp-charts-row">
      <!-- TOP5 偏高人员 -->
      <div class="bp-panel bp-panel-top5">
        <div class="bp-panel-hd">
          <span class="bp-panel-dot"></span>
          <span class="bp-panel-title">偏高收缩压 TOP 5</span>
          <span class="bp-panel-sub">近{{ trendDays }}天均值</span>
        </div>
        <div v-if="top5Data.length" class="bp-top5-list">
          <div v-for="(item, i) in top5Data" :key="i" class="bp-top5-item">
            <span class="bp-top5-rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
            <div class="bp-top5-info">
              <span class="bp-top5-name">{{ item.userName }}</span>
              <span class="bp-top5-dept">{{ item.deptName }}</span>
            </div>
            <div class="bp-top5-values">
              <span class="bp-top5-sys">{{ item.avgSystolic }} <em>收</em></span>
              <span class="bp-top5-dia">{{ item.avgDiastolic }} <em>舒</em></span>
            </div>
            <div class="bp-top5-bar-wrap">
              <div class="bp-top5-bar" :style="{ width: barWidth(item.avgSystolic) + '%' }"></div>
            </div>
          </div>
        </div>
        <div v-else class="bp-empty">暂无异常人员</div>
      </div>

      <!-- 部门统计柱状图 -->
      <div class="bp-panel bp-panel-dept">
        <div class="bp-panel-hd">
          <span class="bp-panel-dot"></span>
          <span class="bp-panel-title">部门平均收缩压</span>
        </div>
        <div ref="deptChart" class="bp-chart-area"></div>
      </div>
    </div>

    <!-- 异常记录表 -->
    <div class="bp-panel bp-panel-table">
      <div class="bp-panel-hd">
        <span class="bp-panel-dot"></span>
        <span class="bp-panel-title">异常血压记录</span>
        <span class="bp-panel-sub">近30天，收缩压 &gt;139 或 舒张压 &gt;89</span>
      </div>
      <el-table :data="abnormal.list" size="small" stripe class="bp-table">
        <el-table-column prop="userName"  label="姓名"   width="100" />
        <el-table-column prop="deptName"  label="部门"   width="130" />
        <el-table-column prop="systolic"  label="收缩压(mmHg)" width="130" align="center">
          <template #default="{ row }">
            <span :class="levelCls(row.level)">{{ row.systolic }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diastolic" label="舒张压(mmHg)" width="130" align="center">
          <template #default="{ row }">
            <span :class="levelCls(row.level)">{{ row.diastolic }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 'danger' ? 'danger' : 'warning'" size="small">
              {{ row.level === 'danger' ? '高血压' : '偏高' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordTime" label="记录时间" min-width="160" />
      </el-table>
      <div class="bp-pagination">
        <el-pagination
          v-model:current-page="abnormal.page"
          :page-size="abnormal.size"
          :total="abnormal.total"
          layout="total, prev, pager, next"
          small
          @current-change="loadAbnormal"
        />
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import {
  getBPOverview, getBPTrend, getBPDistribution,
  getBPTopUsers, getBPDeptStats, getBPAbnormal
} from '@/api/blood-pressure'

export default {
  name: 'BloodPressureAnalysis',
  data() {
    const end = dayjs().format('YYYY-MM-DD')
    const start = dayjs().subtract(29, 'day').format('YYYY-MM-DD')
    return {
      dateRange: [start, end],
      trendDays: 30,
      overview: {},
      distData: [],
      top5Data: [],
      deptData: [],
      abnormal: { list: [], total: 0, page: 1, size: 10 },
      trendChart: null,
      distChart: null,
      deptChart: null,
    }
  },
  computed: {
    startDate() { return this.dateRange ? this.dateRange[0] : dayjs().subtract(29, 'day').format('YYYY-MM-DD') },
    endDate()   { return this.dateRange ? this.dateRange[1] : dayjs().format('YYYY-MM-DD') },
    overviewCards() {
      const o = this.overview
      return [
        {
          key: 'sys', label: '平均收缩压', icon: 'Pointer', color: '#a78bfa', bg: 'rgba(167,139,250,0.15)',
          value: o.avgSystolic ? o.avgSystolic + ' mmHg' : '--',
          sub: '正常 90~139 mmHg'
        },
        {
          key: 'dia', label: '平均舒张压', icon: 'Pointer', color: '#38bdf8', bg: 'rgba(56,189,248,0.15)',
          value: o.avgDiastolic ? o.avgDiastolic + ' mmHg' : '--',
          sub: '正常 60~89 mmHg'
        },
        {
          key: 'normal', label: '正常率', icon: 'CircleCheck', color: '#4ade80', bg: 'rgba(74,222,128,0.15)',
          value: o.normalRate != null ? o.normalRate + '%' : '--',
          sub: '收缩压90~139 & 舒张压60~89'
        },
        {
          key: 'count', label: '检测人数', icon: 'User', color: '#fb923c', bg: 'rgba(251,146,60,0.15)',
          value: o.detectionCount != null ? o.detectionCount + ' 人' : '--',
          sub: '区间内有血压记录'
        },
        {
          key: 'abnormal', label: '异常次数', icon: 'Warning', color: '#f87171', bg: 'rgba(248,113,113,0.15)',
          value: o.abnormalCount != null ? o.abnormalCount + ' 次' : '--',
          sub: '收缩压>139 或 舒张压>89'
        },
      ]
    }
  },
  mounted() {
    this.loadAll()
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeCharts)
    ;[this.trendChart, this.distChart, this.deptChart].forEach(c => c && c.dispose())
  },
  methods: {
    async loadAll() {
      await Promise.all([
        this.loadOverview(),
        this.loadTrend(),
        this.loadDist(),
        this.loadTop5(),
        this.loadDept(),
        this.loadAbnormal(),
      ])
    },
    onDateChange() {
      this.loadOverview()
      this.loadDist()
      this.loadTop5()
      this.loadDept()
    },
    async loadOverview() {
      try {
        const res = await getBPOverview(this.startDate, this.endDate)
        this.overview = res.data || {}
      } catch { this.overview = {} }
    },
    async loadTrend() {
      try {
        const res = await getBPTrend(this.trendDays)
        const d = res.data || {}
        this.initTrendChart(d.dates || [], d.systolicValues || [], d.diastolicValues || [])
      } catch { this.initTrendChart([], [], []) }
    },
    async loadDist() {
      try {
        const res = await getBPDistribution(this.startDate, this.endDate)
        this.distData = res.data || []
        this.initDistChart()
      } catch { this.distData = [] }
    },
    async loadTop5() {
      try {
        const res = await getBPTopUsers(5, this.startDate, this.endDate)
        this.top5Data = res.data || []
      } catch { this.top5Data = [] }
    },
    async loadDept() {
      try {
        const res = await getBPDeptStats(this.startDate, this.endDate)
        this.deptData = res.data || []
        this.initDeptChart()
      } catch { this.deptData = [] }
    },
    async loadAbnormal(page) {
      if (page) this.abnormal.page = page
      try {
        const res = await getBPAbnormal(this.abnormal.page, this.abnormal.size)
        const d = res.data || {}
        this.abnormal.list  = d.list  || []
        this.abnormal.total = d.total || 0
      } catch { this.abnormal.list = [] }
    },

    initTrendChart(dates, sysVals, diaVals) {
      if (!this.$refs.trendChart) return
      if (!this.trendChart) this.trendChart = echarts.init(this.$refs.trendChart)
      const isEmpty = !dates.length
      this.trendChart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,18,48,0.9)', borderColor: '#00d4ff33', textStyle: { color: '#e0f0ff' } },
        legend: { data: ['收缩压', '舒张压'], top: 4, textStyle: { color: '#8ba6c8' } },
        grid: { top: 36, bottom: 24, left: 48, right: 16 },
        xAxis: {
          type: 'category', data: isEmpty ? ['暂无数据'] : dates,
          axisLine: { lineStyle: { color: '#1e3a5f' } },
          axisLabel: { color: '#8ba6c8', fontSize: 11 },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value', name: 'mmHg',
          min: val => Math.max(0, Math.floor(val.min - 10)),
          axisLabel: { color: '#8ba6c8', fontSize: 11 },
          splitLine: { lineStyle: { color: '#1e3a5f' } }
        },
        series: [
          {
            name: '收缩压', type: 'line', data: isEmpty ? [] : sysVals,
            smooth: true, connectNulls: false,
            lineStyle: { color: '#a78bfa', width: 2 },
            itemStyle: { color: '#a78bfa' },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(167,139,250,0.3)' },
              { offset: 1, color: 'rgba(167,139,250,0.02)' }
            ])},
            markLine: { silent: true, lineStyle: { color: '#a78bfa55', type: 'dashed' },
              data: [{ yAxis: 139, name: '正常上限' }] }
          },
          {
            name: '舒张压', type: 'line', data: isEmpty ? [] : diaVals,
            smooth: true, connectNulls: false,
            lineStyle: { color: '#38bdf8', width: 2 },
            itemStyle: { color: '#38bdf8' },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(56,189,248,0.2)' },
              { offset: 1, color: 'rgba(56,189,248,0.02)' }
            ])},
            markLine: { silent: true, lineStyle: { color: '#38bdf855', type: 'dashed' },
              data: [{ yAxis: 89, name: '正常上限' }] }
          }
        ]
      })
    },

    initDistChart() {
      if (!this.$refs.distChart) return
      if (!this.distChart) this.distChart = echarts.init(this.$refs.distChart)
      const data = this.distData
      this.distChart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item', backgroundColor: 'rgba(10,18,48,0.9)', borderColor: '#00d4ff33', textStyle: { color: '#e0f0ff' },
          formatter: '{b}: {c}%' },
        series: [{
          type: 'pie', radius: ['45%', '70%'], center: ['50%', '50%'],
          data: data.length ? data.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } }))
                            : [{ name: '暂无数据', value: 1, itemStyle: { color: '#1e3a5f' } }],
          label: { show: data.length, color: '#8ba6c8', fontSize: 11,
            formatter: '{b}\n{c}%' },
          labelLine: { lineStyle: { color: '#1e3a5f' } },
          emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,212,255,0.3)' } }
        }]
      })
    },

    initDeptChart() {
      if (!this.$refs.deptChart) return
      if (!this.deptChart) this.deptChart = echarts.init(this.$refs.deptChart)
      const data = this.deptData.slice(0, 10)
      this.deptChart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,18,48,0.9)', borderColor: '#00d4ff33',
          textStyle: { color: '#e0f0ff' },
          formatter: params => params.map(p => `${p.seriesName}: ${p.value} mmHg`).join('<br>') },
        legend: { data: ['收缩压', '舒张压'], top: 4, textStyle: { color: '#8ba6c8' } },
        grid: { top: 36, bottom: 60, left: 60, right: 16 },
        xAxis: {
          type: 'category',
          data: data.length ? data.map(d => d.deptName) : ['暂无数据'],
          axisLabel: { color: '#8ba6c8', fontSize: 10, rotate: 30 },
          axisLine: { lineStyle: { color: '#1e3a5f' } }
        },
        yAxis: {
          type: 'value', name: 'mmHg',
          axisLabel: { color: '#8ba6c8', fontSize: 11 },
          splitLine: { lineStyle: { color: '#1e3a5f' } }
        },
        series: [
          {
            name: '收缩压', type: 'bar', barMaxWidth: 18,
            data: data.map(d => d.avgSystolic),
            itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#a78bfa' }, { offset: 1, color: '#7c3aed' }
            ]), borderRadius: [3, 3, 0, 0] }
          },
          {
            name: '舒张压', type: 'bar', barMaxWidth: 18,
            data: data.map(d => d.avgDiastolic),
            itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#38bdf8' }, { offset: 1, color: '#0284c7' }
            ]), borderRadius: [3, 3, 0, 0] }
          }
        ]
      })
    },

    barWidth(val) {
      const maxSys = Math.max(...this.top5Data.map(d => d.avgSystolic), 160)
      return Math.round((val / maxSys) * 100)
    },

    levelCls(level) {
      return level === 'danger' ? 'bp-danger' : 'bp-warn'
    },

    resizeCharts() {
      ;[this.trendChart, this.distChart, this.deptChart].forEach(c => c && c.resize())
    }
  }
}
</script>

<style lang="scss" scoped>
.bp-page {
  padding: 16px;
  min-height: 100%;
  background: #060e24;
  color: #e0f0ff;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

// 顶部筛选栏
.bp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 10px;
}
.bp-toolbar-title {
  font-size: 16px;
  font-weight: bold;
  color: #e0f0ff;
  border-left: 3px solid #00d4ff;
  padding-left: 10px;
}
.bp-toolbar-right {
  display: flex;
  align-items: center;
}

// 概览卡片
.bp-overview {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.bp-ov-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.12);
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.bp-ov-icon {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.bp-ov-info { flex: 1; }
.bp-ov-val  { font-size: 20px; font-weight: bold; line-height: 1.2; }
.bp-ov-label{ font-size: 12px; color: #8ba6c8; margin-top: 2px; }
.bp-ov-sub  {
  position: absolute; bottom: 6px; right: 10px;
  font-size: 10px; color: #4a6a8a;
}

// 面板
.bp-panel {
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.12);
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bp-panel-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.bp-panel-dot {
  width: 6px; height: 6px;
  background: #00d4ff;
  border-radius: 50%;
  box-shadow: 0 0 6px #00d4ff;
}
.bp-panel-title { font-size: 14px; font-weight: bold; color: #e0f0ff; }
.bp-panel-sub   { font-size: 11px; color: #4a6a8a; margin-left: 4px; }

.bp-chart-area { flex: 1; min-height: 200px; }

.bp-charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.bp-panel-trend { grid-column: span 1; }
.bp-panel-dist  { min-height: 280px; }
.bp-panel-top5  { min-height: 260px; }
.bp-panel-dept  { grid-column: span 1; }
.bp-panel-table { min-height: 200px; }

// 分布图例
.bp-dist-legend {
  list-style: none; margin: 0; padding: 0;
  display: flex; gap: 16px; flex-wrap: wrap;
  justify-content: center;
}
.bp-dist-legend li { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #8ba6c8; }
.bp-legend-dot { width: 10px; height: 10px; border-radius: 50%; }
.bp-legend-val { color: #e0f0ff; font-weight: bold; }

// TOP5
.bp-top5-list { display: flex; flex-direction: column; gap: 10px; }
.bp-top5-item {
  display: grid;
  grid-template-columns: 28px 1fr auto 120px;
  align-items: center;
  gap: 10px;
}
.bp-top5-rank {
  width: 24px; height: 24px;
  border-radius: 50%;
  background: rgba(0,212,255,0.15);
  border: 1px solid rgba(0,212,255,0.3);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: bold; color: #8ba6c8;
  &.rank-1 { background: rgba(251,191,36,0.2); border-color: #fbbf24; color: #fbbf24; }
  &.rank-2 { background: rgba(156,163,175,0.2); border-color: #9ca3af; color: #9ca3af; }
  &.rank-3 { background: rgba(180,83,9,0.2); border-color: #b45309; color: #b45309; }
}
.bp-top5-info { display: flex; flex-direction: column; }
.bp-top5-name { font-size: 13px; color: #e0f0ff; }
.bp-top5-dept { font-size: 11px; color: #4a6a8a; }
.bp-top5-values { display: flex; gap: 8px; white-space: nowrap; }
.bp-top5-sys { color: #a78bfa; font-size: 13px; font-weight: bold; em { font-style: normal; font-size: 10px; color: #4a6a8a; } }
.bp-top5-dia { color: #38bdf8; font-size: 13px; font-weight: bold; em { font-style: normal; font-size: 10px; color: #4a6a8a; } }
.bp-top5-bar-wrap { height: 6px; background: rgba(0,212,255,0.1); border-radius: 3px; overflow: hidden; }
.bp-top5-bar { height: 100%; background: linear-gradient(90deg, #a78bfa, #7c3aed); border-radius: 3px; transition: width 0.6s; }

// 表格
.bp-table { background: transparent; }
:deep(.bp-table .el-table__header th) { background: rgba(0,30,70,0.6); color: #8ba6c8; font-size: 12px; }
:deep(.bp-table .el-table__body tr) { background: transparent; }
:deep(.bp-table .el-table__body tr:hover td) { background: rgba(0,212,255,0.05); }
:deep(.bp-table .el-table__body td) { border-bottom: 1px solid rgba(0,212,255,0.06); color: #a8c5e6; font-size: 12px; }
:deep(.bp-table .el-table__body tr.el-table__row--striped td) { background: rgba(0,20,55,0.4); }

.bp-danger { color: #f87171; font-weight: bold; }
.bp-warn   { color: #fbbf24; font-weight: bold; }

.bp-pagination {
  display: flex; justify-content: flex-end; margin-top: 6px;
}
:deep(.bp-pagination .el-pagination) {
  --el-pagination-text-color: #8ba6c8;
  --el-pagination-button-color: #8ba6c8;
  --el-pagination-hover-color: #00d4ff;
}

.bp-empty {
  text-align: center; color: #4a6a8a; font-size: 13px; padding: 30px 0;
}

// Element Plus 深色覆盖
:deep(.el-date-editor), :deep(.el-select .el-input__wrapper) {
  background: rgba(0,20,55,0.6) !important;
  box-shadow: 0 0 0 1px rgba(0,212,255,0.2) !important;
  .el-input__inner, .el-range-input { color: #e0f0ff !important; background: transparent !important; }
  .el-range-separator { color: #4a6a8a !important; }
  .el-input__icon { color: #4a6a8a !important; }
}
:deep(.el-select-dropdown) {
  background: rgba(6,14,36,0.98) !important;
  border-color: rgba(0,212,255,0.2) !important;
  .el-select-dropdown__item { color: #8ba6c8; &.is-selected, &:hover { color: #00d4ff; background: rgba(0,212,255,0.08); } }
}
</style>
