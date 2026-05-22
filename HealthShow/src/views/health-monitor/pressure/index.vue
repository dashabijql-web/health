<template>
  <div class="ps-root">

    <!-- ══ 顶部 Header ══ -->
    <header class="ps-hd">
      <div class="ps-hd-left">
        <span class="ps-live-dot"></span>
        <h1 class="ps-hd-title">压力指数分析</h1>
      </div>

      <div class="ps-hd-kpis">
        <div class="ps-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="ps-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="ps-kpi-l">{{ k.label }}</span>
        </div>
      </div>

      <div class="ps-period-tabs">
        <span v-for="p in periodOptions" :key="p.value"
          :class="['ps-period-tab', activePeriod === p.value ? 'is-active' : '']"
          @click="switchPeriod(p.value)">{{ p.label }}</span>
      </div>

      <div class="ps-hd-time">{{ currentTime }}</div>
      <button class="hm-export-btn" @click="exportExcel" title="导出当前数据">导出</button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="ps-bd" v-loading="pageLoading" element-loading-text="数据加载中..." element-loading-background="rgba(10,20,40,0.7)">

      <!-- ─ 左侧：TOP5 紧凑列表 + 部门柱状图 ─ -->
      <aside class="ps-aside">
        <!-- 高压力排行 -->
        <div class="ps-panel ps-aside-top">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">{{ top5Title }}</span>
          </div>
          <div class="ps-top5-list" ref="top5ScrollRef">
            <div v-if="!top5Data.length" class="ps-top5-empty">暂无高压力数据</div>
            <div class="ps-top5-row" v-for="(item, i) in displayedTop5" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="ps-top5-rank" :class="i < 3 ? 'rank-'+(i+1) : 'rank-n'">{{ i+1 }}</span>
              <span class="ps-top5-name">{{ item.userName }}</span>
              <div class="ps-top5-bar-wrap">
                <div class="ps-top5-bar"
                  :style="{
                    width: (top5Max > 0 ? (item.avgPressure / top5Max * 100) : 0) + '%',
                    background: top5BarColor(item.avgPressure)
                  }"></div>
              </div>
              <span class="ps-top5-val" :style="{ color: top5ValColor(item.avgPressure) }">{{ item.avgPressure }}</span>
            </div>
            <div v-if="top5Data.length > 20" class="ps-top5-more" @click="top5Expanded = !top5Expanded">
              {{ top5Expanded ? '▲ 收起' : `▼ 展开全部 (${top5Data.length} 条)` }}
            </div>
          </div>
        </div>

        <!-- 部门平均压力柱状图 -->
        <div class="ps-panel ps-aside-bot">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">部门平均压力指数</span>
            <span v-if="filterDept" class="ps-dept-tag" @click="filterDept=''" title="点击取消筛选">{{ filterDept }} ×</span>
          </div>
          <div class="ps-pc">
            <div ref="deptRef" style="width:100%;height:100%"></div>
          </div>
        </div>
      </aside>

      <!-- ─ 中间 ─ -->
      <main class="ps-main">

        <!-- 概况面板：仪表盘 + KPI cards + 压力等级说明 -->
        <div class="ps-panel ps-overview-panel">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">{{ overviewTitle }}</span>
          </div>
          <div class="ps-overview-body">
            <!-- 仪表盘 -->
            <div class="ps-gauge-wrap">
              <div ref="gaugeRef" class="ps-gauge-chart"></div>
              <div class="ps-gauge-center">
                <div class="ps-gauge-val">{{ overview.avgPressure != null ? overview.avgPressure : '--' }}</div>
                <div class="ps-gauge-sub">平均压力指数</div>
              </div>
            </div>
            <!-- KPI 小卡片 -->
            <div class="ps-kpi-cards">
              <div class="ps-kpi-card" v-for="c in ovAllCards" :key="c.label">
                <div class="ps-kpi-card-val" :style="{color: c.color}">{{ c.val }}<span class="ps-kpi-card-unit">{{ c.unit }}</span></div>
                <div class="ps-kpi-card-label">{{ c.label }}</div>
              </div>
            </div>
            <!-- 压力等级说明 -->
            <div class="ps-range-info">
              <div class="ps-range-title">压力等级说明</div>
              <div class="ps-range-item" v-for="r in psRanges" :key="r.label">
                <span class="ps-range-dot" :style="{background: r.color}"></span>
                <span class="ps-range-name" :style="{color: r.color}">{{ r.label }}</span>
                <span class="ps-range-val">{{ r.range }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间行：趋势折线 + 分布饼图 -->
        <div class="ps-mid-row">
          <div class="ps-panel ps-panel-hourly">
            <div class="ps-ph">
              <span class="ps-ph-bar"></span>
              <span class="ps-ph-title">{{ hourlyTitle }}</span>
              <div class="ps-trend-tags">
                <span class="ps-tag" style="color:#fb923c;border-color:rgba(251,146,60,0.3)">── 压力指数</span>
                <span class="ps-tag" style="color:#FFB84D;border-color:rgba(255,184,77,0.3)">- - 偏高(70)</span>
                <span class="ps-tag" style="color:#ff5252;border-color:rgba(255,82,82,0.3)">- - 高压(85)</span>
              </div>
            </div>
            <div class="ps-pc">
              <div ref="hourlyRef" style="width:100%;height:100%"></div>
            </div>
          </div>

          <div class="ps-panel ps-panel-dist">
            <div class="ps-ph">
              <span class="ps-ph-bar"></span>
              <span class="ps-ph-title">压力区间分布</span>
            </div>
            <div class="ps-dist-body">
              <div ref="distRef" class="ps-dist-chart"></div>
              <div class="ps-dist-legend">
                <div class="ps-dist-row" v-for="d in distLegend" :key="d.name">
                  <div class="ps-dist-dot" :style="{background: d.color}"></div>
                  <span class="ps-dist-name">{{ d.name }}</span>
                  <div class="ps-dist-bar-wrap">
                    <div class="ps-dist-bar" :style="{width: d.value + '%', background: d.color}"></div>
                  </div>
                  <span class="ps-dist-pct" :style="{color: d.color}">{{ d.value }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 实时区间分布面板 -->
        <div class="ps-panel ps-panel-dist-stat">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">当前在线人员压力分布</span>
            <span class="ps-ds-total">共 <em>{{ realtimeList.length }}</em> 人在线</span>
          </div>
          <div class="ps-ds-body">
            <div class="ps-ds-zone" :class="z.cls" v-for="z in psZones" :key="z.key">
              <div class="ps-ds-icon" :style="{color: z.color}">{{ z.icon }}</div>
              <div class="ps-ds-count" :style="{color: z.color}">{{ z.count }}</div>
              <div class="ps-ds-pct" :style="{color: z.color}">{{ z.pct }}%</div>
              <div class="ps-ds-label">{{ z.label }}</div>
              <div class="ps-ds-range">{{ z.range }}</div>
            </div>
          </div>
          <div class="ps-ds-bar-row">
            <div class="ps-ds-seg" v-for="z in psZones" :key="z.key"
              :style="{width: z.pct + '%', background: z.color}"
              :title="z.label + ': ' + z.count + '人'"></div>
          </div>
        </div>

        <!-- 当前异常压力明细 -->
        <div class="ps-panel ps-panel-anomaly">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">当前异常压力明细{{ filterDept ? ' — ' + filterDept : '' }}</span>
            <span class="ps-anomaly-count" v-if="psAnomalyList.length">
              共 <em>{{ psAnomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!psAnomalyList.length" class="ps-anomaly-empty">
            当前无异常压力人员
          </div>
          <div v-else class="ps-anomaly-body">
            <div class="ps-anomaly-hd">
              <span>姓名</span><span>部门</span><span>压力指数</span><span>等级</span><span>时间</span>
            </div>
            <div class="ps-anomaly-list">
              <div
                class="ps-anomaly-row"
                v-for="(item, i) in displayedAnomalyList"
                :key="i"
                :class="item.pressure >= 85 ? 'anom-high' : 'anom-elevated'"
                @click="goToPortrait(item)"
                style="cursor:pointer"
              >
                <span class="pa-name">{{ item.userName }}</span>
                <span class="pa-dept">{{ item.deptName || '--' }}</span>
                <span class="pa-val">{{ item.pressure }}</span>
                <span class="pa-type">{{ item.pressure >= 85 ? '高压需关注' : '偏高' }}</span>
                <span class="pa-time">{{ fmtTime(item.recordTime) }}</span>
              </div>
              <div v-if="psAnomalyList.length > 20" class="ps-anomaly-more" @click="anomalyExpanded = !anomalyExpanded">
                {{ anomalyExpanded ? '▲ 收起' : `▼ 展开全部 (${psAnomalyList.length} 人)` }}
              </div>
            </div>
          </div>
        </div>

      </main>

      <!-- ─ 右侧：实时压力列表 ─ -->
      <div class="ps-rtlist">
        <div class="ps-panel hm-panel-flex">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">实时压力数据</span>
            <span class="ps-rt-total">{{ realtimeList.length }} 条</span>
          </div>

          <div class="ps-rt-hd">
            <span>#</span><span>姓名</span><span>压力</span><span>状态</span><span>时间</span>
          </div>

          <div class="ps-rt-body" ref="listRef">
            <div
              class="ps-rt-row"
              v-for="(item, i) in filteredRealtimeList"
              :key="i"
              :class="psLevel(item.pressure)"
              @click="goToPortrait(item)"
              style="cursor:pointer"
            >
              <span class="ps-rt-idx">{{ i + 1 }}</span>
              <span class="ps-rt-name">{{ item.userName }}</span>
              <span class="ps-rt-val">{{ item.pressure }}</span>
              <span class="ps-rt-badge" :class="psLevel(item.pressure)">
                {{ item.pressure >= 85 ? '高压' : item.pressure >= 70 ? '偏高' : item.pressure >= 50 ? '正常' : '放松' }}
              </span>
              <span class="ps-rt-time">{{ fmtRtTime(item.recordTime) }}</span>
            </div>
          </div>
        </div>
      </div>

    </section>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import {
  getPressureOverview,
  getPressureTrend,
  getPressureDistribution,
  getPressureTopUsers,
  getPressureDeptStats,
  getPressureRealtime,
  getPressureHourly
} from '@/api/pressure'
import chartPageMixin from '@/mixins/chartPage'
import { PERIOD_OPTIONS } from '@/constants/periods'
import {
  createHourlySeries,
  fetchMetricData,
  getMetricToday,
  loadMetricDistribution,
  loadMetricOverview,
  loadMetricRangeChart,
  loadMetricRealtime,
  loadMetricTopUsers,
  metricDaysForPeriod
} from '@/views/health-monitor/metric-page/metric-data-loader'
import { exportMetricRows } from '@/views/health-monitor/metric-page/metric-export'
import metricPageMixin from '@/views/health-monitor/metric-page/metric-page-mixin'
import { pressureChartMethods } from './pressure-chart'

export default {
  name: 'PressureAnalysis',
  mixins: [chartPageMixin, metricPageMixin],
  data() {
    return {
      pageLoading: false,
      currentTime: '',
      overview: {},
      distLegend: [],
      top5Data: [],
      top5Expanded: false,
      anomalyExpanded: false,
      deptData: [],
      realtimeList: [],
      filterDept: '',
      currentPage: 1,
      pageSize: 20,
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      _top5ScrollLoop: null,
      psRanges: [
        { label: '放松 (低压力)', range: '< 50',      color: '#4FC3F7' },
        { label: '正常 (健康)',   range: '50 – 69',   color: '#52c41a' },
        { label: '偏高 (注意)',   range: '70 – 84',   color: '#FFB84D' },
        { label: '高压 (危险)',   range: '≥ 85',      color: '#ff5252' }
      ],
      charts: {},
    }
  },
  computed: {
    headerKpis() {
      const o = this.overview
      return [
        { label: '平均压力指数', val: o.avgPressure   != null ? o.avgPressure : '--',                     cls: 'kpi-orange' },
        { label: '正常率',       val: o.normalRate    != null ? o.normalRate + '%' : '--',                cls: 'kpi-green'  },
        { label: '偏高次数',     val: o.abnormalCount != null ? o.abnormalCount : 0,                      cls: 'kpi-yellow' },
        { label: '高压次数',     val: o.highCount     != null ? o.highCount : 0,                          cls: 'kpi-red'    }
      ]
    },
    ovAllCards() {
      const o = this.overview
      return [
        { label: '平均压力指数', val: o.avgPressure    != null ? o.avgPressure    : '--', unit: '',    color: '#fb923c' },
        { label: '正常率',       val: o.normalRate     != null ? o.normalRate     : '--', unit: '%',   color: '#52c41a' },
        { label: '偏高次数',     val: o.abnormalCount  != null ? o.abnormalCount  : '--', unit: ' 次', color: '#FFB84D' },
        { label: '高压次数',     val: o.highCount      != null ? o.highCount      : '--', unit: ' 次', color: '#ff5252' },
        { label: '检测人数',     val: o.detectionCount != null ? o.detectionCount : '--', unit: ' 人', color: '#4FC3F7' },
        { label: '记录总数',     val: o.totalCount     != null ? (o.totalCount).toLocaleString() : '--', unit: ' 条', color: '#7eb8f7' }
      ]
    },
    overviewTitle() {
      return { day: '今日压力概况', week: '近7日压力概况', month: '近30日压力概况' }[this.activePeriod]
    },
    hourlyTitle() {
      return { day: '今日24小时压力波动', week: '近7日每日均值', month: '近30日每日均值' }[this.activePeriod]
    },
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.avgPressure || 0)) : 1
    },
    displayedTop5() {
      return this.top5Data.slice(0, this.top5Expanded ? this.top5Data.length : 20)
    },
    top5Title() {
      const p = { day: '今日', week: '近7日', month: '近30日' }[this.activePeriod]
      return p + '高压力排行'
    },
    filteredRealtimeList() {
      if (!this.filterDept) return this.realtimeList
      return this.realtimeList.filter(x => x.deptName === this.filterDept)
    },
    /* pagedList / totalPages from chartPageMixin */
    psAnomalyList() {
      return this.filteredRealtimeList.filter(x => x.pressure >= 70)
    },
    displayedAnomalyList() {
      return this.anomalyExpanded ? this.psAnomalyList : this.psAnomalyList.slice(0, 20)
    },
    psZones() {
      const list = this.realtimeList
      const total = list.length || 1
      const relaxed  = list.filter(x => x.pressure < 50).length
      const normal   = list.filter(x => x.pressure >= 50 && x.pressure < 70).length
      const elevated = list.filter(x => x.pressure >= 70 && x.pressure < 85).length
      const high     = list.filter(x => x.pressure >= 85).length
      const pct = n => list.length > 0 ? Math.round(n / total * 100) : 0
      return [
        { key: 'relaxed',  label: '放松', range: '< 50',    count: relaxed,  pct: pct(relaxed),  color: '#4FC3F7', icon: '○', cls: 'zone-relaxed'  },
        { key: 'normal',   label: '正常', range: '50–69',   count: normal,   pct: pct(normal),   color: '#52c41a', icon: 'OK', cls: 'zone-normal'   },
        { key: 'elevated', label: '偏高', range: '70–84',   count: elevated, pct: pct(elevated), color: '#FFB84D', icon: '!', cls: 'zone-elevated' },
        { key: 'high',     label: '高压', range: '≥ 85',    count: high,     pct: pct(high),     color: '#ff5252', icon: 'ALERT', cls: 'zone-high'     }
      ]
    }
  },
  mounted() {
    this.initPage(() => this.loadRealtime())
    this.initAutoPageSize(27)
  },
  methods: {
    ...pressureChartMethods,

    async exportExcel() {
      const list = this.realtimeList
      const cols = [
        { label: '姓名', key: 'userName' },
        { label: '部门', key: 'deptName' },
        { label: '工号', key: 'empCode' },
        { label: '压力指数', key: 'pressure' },
        { label: '状态', key: 'status' },
        { label: '记录时间', key: 'recordTime' }
      ]
      await exportMetricRows({
        rows: list,
        columns: cols,
        filenamePrefix: '压力分析',
        mapRow: r => ({
          userName: r.userName || '--',
          deptName: r.deptName || '--',
          empCode: r.empCode || '--',
          pressure: r.pressure ?? '--',
          status: (r.pressure >= 85) ? '高危' : (r.pressure >= 70) ? '偏高' : (r.pressure >= 50) ? '正常' : '放松',
          recordTime: r.recordTime ? dayjs(r.recordTime).format('YYYY-MM-DD HH:mm') : '--'
        })
      })
    },

    async fetchData() {
      await Promise.allSettled([
        this.loadOverview(),
        this.loadTopUsers(),
        this.loadDept(),
        this.loadDist(),
        this.loadHourly(),
        this.loadRealtime()
      ])
    },

    async loadOverview() {
      await loadMetricOverview(this, getPressureOverview, {})
      this.$nextTick(() => this.initGauge())
    },

    async loadTopUsers() {
      await loadMetricTopUsers(this, getPressureTopUsers)
    },

    async loadDept() {
      await loadMetricRangeChart(this, getPressureDeptStats, 'initDept', { assignTo: 'deptData' })
    },

    async loadDist() {
      await loadMetricDistribution(this, getPressureDistribution, 'initDist')
    },

    async loadHourly() {
      if (this.activePeriod === 'day') {
        const today = getMetricToday()
        const rows = await fetchMetricData(() => getPressureHourly(today), [])
        const vals = createHourlySeries(rows, 'avgPressure')
        this.$nextTick(() => this.renderHourly(vals))
      } else {
        const days = metricDaysForPeriod(this.activePeriod)
        const data = await fetchMetricData(() => getPressureTrend(days), {})
        const dates = data.dates || []
        const vals = data.values || []
        this.$nextTick(() => this.renderHourlyDaily(dates, vals))
      }
    },

    async loadRealtime() {
      await loadMetricRealtime(this, getPressureRealtime)
    },
    fmtRtTime(ts) {
      return ts ? dayjs(ts).format('HH:mm:ss') : ''
    },

    psLevel(v) {
      if (v >= 85) return 'high'
      if (v >= 70) return 'elevated'
      if (v >= 50) return 'normal'
      return 'relaxed'
    },

    top5BarColor(val) {
      if (val >= 85) return 'linear-gradient(90deg, #ff5252, #b91c1c)'
      if (val >= 70) return 'linear-gradient(90deg, #FFB84D, #d97706)'
      if (val >= 50) return 'linear-gradient(90deg, #52c41a, #166534)'
      return 'linear-gradient(90deg, #4FC3F7, #0284c7)'
    },

    top5ValColor(val) {
      if (val >= 85) return '#ff5252'
      if (val >= 70) return '#FFB84D'
      if (val >= 50) return '#52c41a'
      return '#4FC3F7'
    },

    // setPageSize → chartPageMixin

  }
}
</script>

<style lang="scss" scoped src="./pressure.scss"></style>
