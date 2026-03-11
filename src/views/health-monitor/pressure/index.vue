<template>
  <div class="ps-page">
    <!-- 顶部筛选栏 -->
    <div class="ps-toolbar">
      <span class="ps-toolbar-title">压力指数分析</span>
      <div class="ps-toolbar-right">
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
    <div class="ps-overview">
      <div class="ps-ov-card" v-for="card in overviewCards" :key="card.key">
        <div class="ps-ov-icon" :style="{ background: card.bg }">
          <el-icon :color="card.color" :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="ps-ov-info">
          <div class="ps-ov-val" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="ps-ov-label">{{ card.label }}</div>
        </div>
        <div class="ps-ov-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 中部图表区 -->
    <div class="ps-charts-row">
      <!-- 趋势折线图 -->
      <div class="ps-panel ps-panel-trend">
        <div class="ps-panel-hd">
          <span class="ps-panel-dot"></span>
          <span class="ps-panel-title">压力趋势</span>
          <span class="ps-panel-sub">日均压力指数</span>
        </div>
        <div ref="trendChart" class="ps-chart-area"></div>
      </div>

      <!-- 分布饼图 -->
      <div class="ps-panel ps-panel-dist">
        <div class="ps-panel-hd">
          <span class="ps-panel-dot"></span>
          <span class="ps-panel-title">压力分布</span>
        </div>
        <div ref="distChart" class="ps-chart-area"></div>
        <ul class="ps-dist-legend" v-if="distData.length">
          <li v-for="d in distData" :key="d.name">
            <span class="ps-legend-dot" :style="{ background: d.color }"></span>
            <span class="ps-legend-name">{{ d.name }}</span>
            <span class="ps-legend-val">{{ d.value }}%</span>
          </li>
        </ul>
        <div v-else class="ps-empty">暂无数据</div>
      </div>
    </div>

    <!-- 下部：TOP5 + 部门统计 -->
    <div class="ps-charts-row">
      <!-- TOP5 高压力人员 -->
      <div class="ps-panel ps-panel-top5">
        <div class="ps-panel-hd">
          <span class="ps-panel-dot"></span>
          <span class="ps-panel-title">高压力 TOP 5</span>
          <span class="ps-panel-sub">近{{ trendDays }}天均值</span>
        </div>
        <div v-if="top5Data.length" class="ps-top5-list">
          <div v-for="(item, i) in top5Data" :key="i" class="ps-top5-item">
            <span class="ps-top5-rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
            <div class="ps-top5-info">
              <span class="ps-top5-name">{{ item.userName }}</span>
              <span class="ps-top5-dept">{{ item.deptName }}</span>
            </div>
            <div class="ps-top5-values">
              <span class="ps-top5-avg">均值 <em>{{ item.avgPressure }}</em></span>
              <span class="ps-top5-max">峰值 <em>{{ item.maxPressure }}</em></span>
            </div>
            <div class="ps-top5-bar-wrap">
              <div class="ps-top5-bar" :style="{ width: barWidth(item.avgPressure) + '%', background: barColor(item.avgPressure) }"></div>
            </div>
          </div>
        </div>
        <div v-else class="ps-empty">暂无异常人员</div>
      </div>

      <!-- 部门统计柱状图 -->
      <div class="ps-panel ps-panel-dept">
        <div class="ps-panel-hd">
          <span class="ps-panel-dot"></span>
          <span class="ps-panel-title">部门平均压力指数</span>
        </div>
        <div ref="deptChart" class="ps-chart-area"></div>
      </div>
    </div>

    <!-- 异常记录表 -->
    <div class="ps-panel ps-panel-table">
      <div class="ps-panel-hd">
        <span class="ps-panel-dot"></span>
        <span class="ps-panel-title">异常压力记录</span>
        <span class="ps-panel-sub">近30天，压力指数 ≥ 70</span>
      </div>
      <el-table :data="abnormal.list" size="small" stripe class="ps-table">
        <el-table-column prop="userName"  label="姓名"   width="100" />
        <el-table-column prop="deptName"  label="部门"   width="130" />
        <el-table-column prop="pressure"  label="压力指数" width="110" align="center">
          <template #default="{ row }">
            <span :class="levelCls(row.level)">{{ row.pressure }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 'danger' ? 'danger' : 'warning'" size="small">
              {{ row.level === 'danger' ? '高压' : '偏高' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordTime" label="记录时间" min-width="160">
          <template #default="{ row }">{{ fmtTime(row.recordTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="ps-pagination">
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
  getPressureOverview, getPressureTrend, getPressureDistribution,
  getPressureTopUsers, getPressureDeptStats, getPressureAbnormal
} from '@/api/pressure'

export default {
  name: 'PressureAnalysis',
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
          key: 'avg', label: '平均压力指数', icon: 'Cpu', color: '#fb923c', bg: 'rgba(251,146,60,0.15)',
          value: o.avgPressure != null ? o.avgPressure : '--',
          sub: '正常 < 70'
        },
        {
          key: 'normal', label: '正常率', icon: 'CircleCheck', color: '#4ade80', bg: 'rgba(74,222,128,0.15)',
          value: o.normalRate != null ? o.normalRate + '%' : '--',
          sub: '压力指数 < 70'
        },
        {
          key: 'count', label: '检测人数', icon: 'User', color: '#38bdf8', bg: 'rgba(56,189,248,0.15)',
          value: o.detectionCount != null ? o.detectionCount + ' 人' : '--',
          sub: '区间内有记录'
        },
        {
          key: 'abnormal', label: '偏高次数', icon: 'Warning', color: '#fbbf24', bg: 'rgba(251,191,36,0.15)',
          value: o.abnormalCount != null ? o.abnormalCount + ' 次' : '--',
          sub: '压力指数 ≥ 70'
        },
        {
          key: 'high', label: '高压次数', icon: 'AlarmClock', color: '#f87171', bg: 'rgba(248,113,113,0.15)',
          value: o.highCount != null ? o.highCount + ' 次' : '--',
          sub: '压力指数 ≥ 85'
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
        this.loadOverview(), this.loadTrend(), this.loadDist(),
        this.loadTop5(), this.loadDept(), this.loadAbnormal(),
      ])
    },
    onDateChange() {
      this.loadOverview(); this.loadDist(); this.loadTop5(); this.loadDept()
    },
    async loadOverview() {
      try { const res = await getPressureOverview(this.startDate, this.endDate); this.overview = res.data || {} }
      catch { this.overview = {} }
    },
    async loadTrend() {
      try {
        const res = await getPressureTrend(this.trendDays)
        const d = res.data || {}
        this.initTrendChart(d.dates || [], d.values || [])
      } catch { this.initTrendChart([], []) }
    },
    async loadDist() {
      try { const res = await getPressureDistribution(this.startDate, this.endDate); this.distData = res.data || []; this.initDistChart() }
      catch { this.distData = [] }
    },
    async loadTop5() {
      try { const res = await getPressureTopUsers(5, this.startDate, this.endDate); this.top5Data = res.data || [] }
      catch { this.top5Data = [] }
    },
    async loadDept() {
      try { const res = await getPressureDeptStats(this.startDate, this.endDate); this.deptData = res.data || []; this.initDeptChart() }
      catch { this.deptData = [] }
    },
    async loadAbnormal(page) {
      if (page) this.abnormal.page = page
      try {
        const res = await getPressureAbnormal(this.abnormal.page, this.abnormal.size)
        const d = res.data || {}
        this.abnormal.list  = d.list  || []
        this.abnormal.total = d.total || 0
      } catch { this.abnormal.list = [] }
    },

    initTrendChart(dates, values) {
      if (!this.$refs.trendChart) return
      if (!this.trendChart) this.trendChart = echarts.init(this.$refs.trendChart)
      const isEmpty = !dates.length
      this.trendChart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,18,48,0.9)', borderColor: '#00d4ff33', textStyle: { color: '#e0f0ff' } },
        grid: { top: 24, bottom: 24, left: 48, right: 16 },
        xAxis: {
          type: 'category', data: isEmpty ? ['暂无数据'] : dates,
          axisLine: { lineStyle: { color: '#1e3a5f' } },
          axisLabel: { color: '#8ba6c8', fontSize: 11 },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value', min: 0, max: 100,
          axisLabel: { color: '#8ba6c8', fontSize: 11 },
          splitLine: { lineStyle: { color: '#1e3a5f' } }
        },
        series: [{
          name: '压力指数', type: 'line', data: isEmpty ? [] : values,
          smooth: true, connectNulls: false,
          lineStyle: { color: '#fb923c', width: 2 },
          itemStyle: { color: '#fb923c' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(251,146,60,0.35)' },
            { offset: 1, color: 'rgba(251,146,60,0.02)' }
          ])},
          markLine: { silent: true, lineStyle: { color: '#fbbf2455', type: 'dashed' },
            data: [{ yAxis: 70, name: '偏高' }, { yAxis: 85, name: '高压' }] }
        }]
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
          label: { show: data.length, color: '#8ba6c8', fontSize: 11, formatter: '{b}\n{c}%' },
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
          textStyle: { color: '#e0f0ff' } },
        grid: { top: 16, bottom: 60, left: 48, right: 16 },
        xAxis: {
          type: 'category',
          data: data.length ? data.map(d => d.deptName) : ['暂无数据'],
          axisLabel: { color: '#8ba6c8', fontSize: 10, rotate: 30 },
          axisLine: { lineStyle: { color: '#1e3a5f' } }
        },
        yAxis: {
          type: 'value', min: 0, max: 100,
          axisLabel: { color: '#8ba6c8', fontSize: 11 },
          splitLine: { lineStyle: { color: '#1e3a5f' } }
        },
        series: [{
          name: '平均压力', type: 'bar', barMaxWidth: 22,
          data: data.map(d => ({
            value: d.avgPressure,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: d.avgPressure >= 85 ? '#f87171' : d.avgPressure >= 70 ? '#fbbf24' : '#fb923c' },
                { offset: 1, color: d.avgPressure >= 85 ? '#b91c1c' : d.avgPressure >= 70 ? '#d97706' : '#c2410c' }
              ]),
              borderRadius: [3, 3, 0, 0]
            }
          })),
          markLine: { silent: true, lineStyle: { color: '#fbbf2455', type: 'dashed' },
            data: [{ yAxis: 70, name: '偏高线' }] }
        }]
      })
    },

    barWidth(val) {
      return Math.min(Math.round((val / 100) * 100), 100)
    },
    barColor(val) {
      if (val >= 85) return 'linear-gradient(90deg, #f87171, #b91c1c)'
      if (val >= 70) return 'linear-gradient(90deg, #fbbf24, #d97706)'
      return 'linear-gradient(90deg, #fb923c, #c2410c)'
    },
    levelCls(level) {
      return level === 'danger' ? 'ps-danger' : 'ps-warn'
    },
    fmtTime(t) {
      return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '--'
    },
    resizeCharts() {
      ;[this.trendChart, this.distChart, this.deptChart].forEach(c => c && c.resize())
    }
  }
}
</script>

<style lang="scss" scoped>
.ps-page {
  padding: 16px;
  min-height: 100%;
  background: #060e24;
  color: #e0f0ff;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ps-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px;
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 10px;
}
.ps-toolbar-title {
  font-size: 16px; font-weight: bold; color: #e0f0ff;
  border-left: 3px solid #00d4ff; padding-left: 10px;
}
.ps-toolbar-right { display: flex; align-items: center; }

.ps-overview {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px;
}
.ps-ov-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.12);
  border-radius: 10px;
  position: relative; overflow: hidden;
}
.ps-ov-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ps-ov-info { flex: 1; }
.ps-ov-val  { font-size: 20px; font-weight: bold; line-height: 1.2; }
.ps-ov-label{ font-size: 12px; color: #8ba6c8; margin-top: 2px; }
.ps-ov-sub  { position: absolute; bottom: 6px; right: 10px; font-size: 10px; color: #4a6a8a; }

.ps-panel {
  background: rgba(10,18,48,0.65);
  border: 1px solid rgba(0,212,255,0.12);
  border-radius: 10px;
  padding: 14px 16px;
  display: flex; flex-direction: column; gap: 10px;
}
.ps-panel-hd { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.ps-panel-dot { width: 6px; height: 6px; background: #00d4ff; border-radius: 50%; box-shadow: 0 0 6px #00d4ff; }
.ps-panel-title { font-size: 14px; font-weight: bold; color: #e0f0ff; }
.ps-panel-sub   { font-size: 11px; color: #4a6a8a; margin-left: 4px; }
.ps-chart-area  { flex: 1; min-height: 200px; }

.ps-charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.ps-panel-dist  { min-height: 280px; }
.ps-panel-top5  { min-height: 260px; }
.ps-panel-table { min-height: 200px; }

.ps-dist-legend {
  list-style: none; margin: 0; padding: 0;
  display: flex; gap: 16px; flex-wrap: wrap; justify-content: center;
}
.ps-dist-legend li { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #8ba6c8; }
.ps-legend-dot { width: 10px; height: 10px; border-radius: 50%; }
.ps-legend-val { color: #e0f0ff; font-weight: bold; }

.ps-top5-list { display: flex; flex-direction: column; gap: 10px; }
.ps-top5-item { display: grid; grid-template-columns: 28px 1fr auto 120px; align-items: center; gap: 10px; }
.ps-top5-rank {
  width: 24px; height: 24px; border-radius: 50%;
  background: rgba(0,212,255,0.15); border: 1px solid rgba(0,212,255,0.3);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: bold; color: #8ba6c8;
  &.rank-1 { background: rgba(251,191,36,0.2); border-color: #fbbf24; color: #fbbf24; }
  &.rank-2 { background: rgba(156,163,175,0.2); border-color: #9ca3af; color: #9ca3af; }
  &.rank-3 { background: rgba(180,83,9,0.2); border-color: #b45309; color: #b45309; }
}
.ps-top5-info { display: flex; flex-direction: column; }
.ps-top5-name { font-size: 13px; color: #e0f0ff; }
.ps-top5-dept { font-size: 11px; color: #4a6a8a; }
.ps-top5-values { display: flex; gap: 8px; white-space: nowrap; font-size: 11px; color: #4a6a8a; }
.ps-top5-values em { font-style: normal; color: #fb923c; font-weight: bold; }
.ps-top5-bar-wrap { height: 6px; background: rgba(251,146,60,0.1); border-radius: 3px; overflow: hidden; }
.ps-top5-bar { height: 100%; border-radius: 3px; transition: width 0.6s; }

.ps-table { background: transparent; }
:deep(.ps-table .el-table__header th) { background: rgba(0,30,70,0.6); color: #8ba6c8; font-size: 12px; }
:deep(.ps-table .el-table__body tr) { background: transparent; }
:deep(.ps-table .el-table__body tr:hover td) { background: rgba(0,212,255,0.05); }
:deep(.ps-table .el-table__body td) { border-bottom: 1px solid rgba(0,212,255,0.06); color: #a8c5e6; font-size: 12px; }
:deep(.ps-table .el-table__body tr.el-table__row--striped td) { background: rgba(0,20,55,0.4); }

.ps-danger { color: #f87171; font-weight: bold; }
.ps-warn   { color: #fbbf24; font-weight: bold; }

.ps-pagination { display: flex; justify-content: flex-end; margin-top: 6px; }
:deep(.ps-pagination .el-pagination) {
  --el-pagination-text-color: #8ba6c8;
  --el-pagination-button-color: #8ba6c8;
  --el-pagination-hover-color: #00d4ff;
}
.ps-empty { text-align: center; color: #4a6a8a; font-size: 13px; padding: 30px 0; }

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
