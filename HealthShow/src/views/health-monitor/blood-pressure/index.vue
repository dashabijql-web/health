<template>
  <div class="bp-root">

    <!-- ══ 顶部 Header ══ -->
    <header class="bp-hd">
      <div class="bp-hd-left">
        <span class="bp-live-dot"></span>
        <h1 class="bp-hd-title">血压分析</h1>
      </div>

      <div class="bp-hd-kpis">
        <div class="bp-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="bp-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="bp-kpi-l">{{ k.label }}</span>
        </div>
      </div>

      <div class="bp-period-tabs">
        <span v-for="p in periodOptions" :key="p.value"
          :class="['bp-period-tab', activePeriod === p.value ? 'is-active' : '']"
          @click="switchPeriod(p.value)">{{ p.label }}</span>
      </div>

      <div class="bp-hd-time">{{ currentTime }}</div>
      <button class="hm-export-btn" @click="exportExcel" title="导出当前数据">导出</button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="bp-bd" v-loading="pageLoading" element-loading-text="数据加载中..." element-loading-background="rgba(10,20,40,0.7)">

      <!-- ─ 左侧：TOP5 + 部门统计 ─ -->
      <aside class="bp-aside">
        <!-- 高收缩压排行 -->
        <div class="bp-panel bp-aside-top">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">{{ top5Title }}</span>
          </div>
          <div class="bp-top5-list" ref="top5ScrollRef">
            <div v-if="!top5Data.length" class="bp-top5-empty">暂无高收缩压人员数据</div>
            <div class="bp-top5-row" v-for="(item, i) in displayedTop5" :key="i" role="button" tabindex="0" @click="goToPortrait(item)" @keydown.enter="goToPortrait(item)" style="cursor:pointer">
              <span class="bp-top5-rank" :class="i < 3 ? 'rank-'+(i+1) : 'rank-n'">{{ i+1 }}</span>
              <span class="bp-top5-name">{{ item.userName }}</span>
              <div class="bp-top5-bar-wrap">
                <div class="bp-top5-bar" :style="{width: (item.avgSystolic / top5Max * 100) + '%'}"></div>
              </div>
              <span class="bp-top5-val">{{ item.avgSystolic }}/{{ item.avgDiastolic }}</span>
            </div>
          </div>
        </div>

        <!-- 部门统计柱状图 -->
        <div class="bp-panel bp-aside-bot">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">部门平均收缩压</span>
            <span v-if="filterDept" class="bp-dept-tag" @click="filterDept=''" title="点击取消筛选">{{ filterDept }} ×</span>
            <div class="bp-ph-legend">
              <span class="bp-leg-dot" style="background:#a78bfa"></span><span class="bp-leg-txt">收缩压</span>
              <span class="bp-leg-dot" style="background:#38bdf8"></span><span class="bp-leg-txt">舒张压</span>
            </div>
          </div>
          <div class="bp-pc">
            <div ref="deptRef" style="width:100%;height:100%"></div>
          </div>
        </div>
      </aside>

      <!-- ─ 中间主体 ─ -->
      <main class="bp-main">

        <!-- 当前人员口径 + 4 区间卡 -->
        <div class="bp-hero">
          <div class="bp-scope-card">
            <span class="bp-scope-label">当前覆盖人员</span>
            <strong class="bp-scope-value">{{ realtimeList.length }}<em>人</em></strong>
            <span class="bp-scope-note">近2小时每人最新一条</span>
            <span class="bp-scope-time">更新于 {{ latestRealtimeText }}</span>
          </div>
          <div class="bp-zone-cards">
            <div v-for="z in bpZones" :key="z.key" :class="['bp-zone-card', z.cls]">
              <span class="bp-zone-icon" :style="{color: z.color}">{{ z.icon }}</span>
              <span class="bp-zone-count" :style="{color: z.color}">{{ z.count }}<em>人</em></span>
              <span class="bp-zone-label">{{ z.label }}</span>
              <span class="bp-zone-range">{{ z.range }}</span>
              <div class="bp-zone-pct-bar">
                <div class="bp-zone-pct-fill" :style="{ width: z.pct + '%', background: z.color }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 图表行：趋势 + 24h小时波动 -->
        <div class="bp-charts-row">
          <div class="bp-panel bp-panel-trend">
            <div class="bp-ph">
              <span class="bp-ph-bar"></span>
              <span class="bp-ph-title">{{ trendTitle }}</span>
              <div class="bp-trend-tags">
                <span class="bp-tag" style="color:#a78bfa;border-color:rgba(167,139,250,0.3)">── 收缩压</span>
                <span class="bp-tag" style="color:#38bdf8;border-color:rgba(56,189,248,0.3)">── 舒张压</span>
              </div>
            </div>
            <div class="bp-pc">
              <div ref="trendRef" style="width:100%;height:100%"></div>
            </div>
          </div>
          <div class="bp-panel bp-panel-hourly">
            <div class="bp-ph">
              <span class="bp-ph-bar"></span>
              <span class="bp-ph-title">今日24小时波动</span>
            </div>
            <div class="bp-pc">
              <div ref="hourlyRef" style="width:100%;height:100%"></div>
            </div>
          </div>
        </div>

        <!-- 当前异常血压明细 -->
        <div class="bp-panel bp-panel-anomaly">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">当前异常血压人员{{ filterDept ? ' — ' + filterDept : '' }}</span>
            <span class="bp-anomaly-count" v-if="bpAnomalyList.length">
              共 <em>{{ bpAnomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!bpAnomalyList.length" class="bp-anomaly-empty">
            当前无异常血压人员
          </div>
          <div v-else class="bp-anomaly-body">
            <div class="bp-anomaly-hd">
              <span>姓名</span><span>部门</span><span>收缩压</span><span>舒张压</span><span>等级</span><span>时间</span>
            </div>
            <div class="bp-anomaly-list">
              <div
                class="bp-anomaly-row"
                v-for="(item, i) in displayedBpAnomalyList"
                :key="i"
                role="button"
                tabindex="0"
                :class="item.systolic >= 160 || item.diastolic >= 100 ? 'anom-danger' : 'anom-stage1'"
                @click="goToPortrait(item)"
                @keydown.enter="goToPortrait(item)"
                style="cursor:pointer"
              >
                <span class="ba-name">{{ item.userName }}</span>
                <span class="ba-dept">{{ item.deptName || '--' }}</span>
                <span class="ba-sys">{{ item.systolic }}</span>
                <span class="ba-dia">{{ item.diastolic }}</span>
                <span class="ba-level">
                  {{ item.systolic >= 160 || item.diastolic >= 100 ? '2级高血压' :
                     item.systolic >= 140 || item.diastolic >= 90  ? '1级高血压' : '偏高' }}
                </span>
                <span class="ba-time">{{ fmtTime(item.recordTime) }}</span>
              </div>
              <div v-if="bpAnomalyList.length > 20" class="bp-anomaly-more" @click="anomalyExpanded = !anomalyExpanded">
                {{ anomalyExpanded ? '▲ 收起' : `▼ 展开全部 (${bpAnomalyList.length} 人)` }}
              </div>
            </div>
          </div>
        </div>

      </main>

    </section>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import {
  getBPOverview,
  getBPTrend,
  getBPTopUsers,
  getBPDeptStats,
  getBPRealtime,
  getBPHourly
} from '@/api/blood-pressure'
import chartPageMixin from '@/mixins/chartPage'
import { PERIOD_OPTIONS } from '@/constants/periods'
import {
  createHourlySeries,
  fetchMetricData,
  getMetricToday,
  loadMetricOverview,
  loadMetricRangeChart,
  loadMetricRealtime,
  loadMetricTopUsers,
  metricDaysForPeriod
} from '@/views/health-monitor/metric-page/metric-data-loader'
import { exportMetricRows } from '@/views/health-monitor/metric-page/metric-export'
import metricPageMixin from '@/views/health-monitor/metric-page/metric-page-mixin'
import { bloodPressureChartMethods } from './blood-pressure-chart'

export default {
  name: 'BloodPressureAnalysis',
  mixins: [chartPageMixin, metricPageMixin],
  data() {
    return {
      pageLoading: false,
      currentTime: '',
      overview: {
        avgSystolic: 0, avgDiastolic: 0,
        normalRate: 0, abnormalCount: 0,
        detectionCount: 0, elevatedRate: 0, hypertensionRate: 0
      },
      top5Data: [],
      anomalyExpanded: false,
      filterDept: '',
      _top5ScrollLoop: null,
      bpGrades: [
        { label: '正常',       range: '< 120 / < 80 mmHg',  color: '#52c41a' },
        { label: '偏高',       range: '120~139 / 80~89',     color: '#FFB84D' },
        { label: '1级高血压',  range: '140~159 / 90~99',     color: '#ff7043' },
        { label: '2级高血压',  range: '≥ 160 / ≥ 100',      color: '#ff5252' }
      ],
      realtimeList: [],
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      charts: {}
    }
  },
  computed: {
    headerKpis() {
      const o = this.overview
      return [
        { label: '平均收缩压', val: (o.avgSystolic  || '--') + ' mmHg', cls: 'kpi-purple' },
        { label: '平均舒张压', val: (o.avgDiastolic || '--') + ' mmHg', cls: 'kpi-sky'    },
        { label: '正常率',     val: (o.normalRate   || 0)   + '%',       cls: 'kpi-green'  },
        { label: '异常次数',   val: (o.abnormalCount || 0).toLocaleString(), cls: 'kpi-red' }
      ]
    },
    trendTitle() {
      return { day: '今日血压趋势', week: '近7天血压趋势', month: '近30天血压趋势' }[this.activePeriod]
    },
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.avgSystolic || 0), 160) : 160
    },
    displayedTop5() {
      return this.top5Data.slice(0, 10)
    },
    top5Title() {
      const p = { day: '今日', week: '近7日', month: '近30日' }[this.activePeriod]
      return p + '高血压风险 Top 10'
    },
    filteredRealtimeList() {
      if (!this.filterDept) return this.realtimeList
      return this.realtimeList.filter(x => x.deptName === this.filterDept)
    },
    /* pagedList / totalPages from chartPageMixin */
    bpAnomalyList() {
      return this.filteredRealtimeList.filter(x => x.systolic >= 140 || x.diastolic >= 90)
    },
    displayedBpAnomalyList() {
      return this.anomalyExpanded ? this.bpAnomalyList : this.bpAnomalyList.slice(0, 20)
    },
    bpZones() {
      const list = this.realtimeList
      const total = list.length || 1
      const danger  = list.filter(x => x.systolic >= 160 || x.diastolic >= 100).length
      const stage1  = list.filter(x => (x.systolic >= 140 || x.diastolic >= 90) && !(x.systolic >= 160 || x.diastolic >= 100)).length
      const pre     = list.filter(x => (x.systolic >= 120 || x.diastolic >= 80) && !(x.systolic >= 140 || x.diastolic >= 90)).length
      const normal  = list.filter(x => x.systolic < 120 && x.diastolic < 80).length
      const pct = n => list.length > 0 ? Math.round(n / total * 100) : 0
      return [
        { key: 'normal', label: '正常',      range: '< 120 / < 80',    count: normal, pct: pct(normal), color: '#52c41a', icon: 'OK', cls: 'zone-normal'  },
        { key: 'pre',    label: '偏高',      range: '120~139 / 80~89', count: pre,    pct: pct(pre),    color: '#FFB84D', icon: '↑', cls: 'zone-pre'     },
        { key: 'stage1', label: '1级高血压', range: '140~159 / 90~99', count: stage1, pct: pct(stage1), color: '#ff7043', icon: 'WARN', cls: 'zone-stage1'  },
        { key: 'danger', label: '2级高血压', range: '≥ 160 / ≥ 100',  count: danger, pct: pct(danger), color: '#ff5252', icon: 'ALERT', cls: 'zone-danger'  }
      ]
    }
  },
  mounted() {
    this.initPage(() => this.loadRealtime())
  },
  methods: {
    ...bloodPressureChartMethods,

    async exportExcel() {
      const list = this.realtimeList
      const cols = [
        { label: '姓名', key: 'userName' },
        { label: '部门', key: 'deptName' },
        { label: '工号', key: 'empCode' },
        { label: '收缩压(mmHg)', key: 'systolic' },
        { label: '舒张压(mmHg)', key: 'diastolic' },
        { label: '状态', key: 'status' },
        { label: '记录时间', key: 'recordTime' }
      ]
      await exportMetricRows({
        rows: list,
        columns: cols,
        filenamePrefix: '血压分析',
        mapRow: r => ({
          userName: r.userName || '--',
          deptName: r.deptName || '--',
          empCode: r.empCode || '--',
          systolic: r.systolic ?? '--',
          diastolic: r.diastolic ?? '--',
          status: (r.systolic >= 140 || r.diastolic >= 90) ? '偏高' : r.systolic < 90 ? '偏低' : '正常',
          recordTime: r.recordTime ? dayjs(r.recordTime).format('YYYY-MM-DD HH:mm') : '--'
        })
      })
    },

    async fetchData() {
      await Promise.allSettled([
        this.loadOverview(),
        this.loadTopUsers(),
        this.loadDept(),
        this.loadTrend(),
        this.loadHourly(),
        this.loadRealtime()
      ])
    },

    async loadOverview() {
      await loadMetricOverview(this, getBPOverview, {})
    },

    async loadTopUsers() {
      await loadMetricTopUsers(this, getBPTopUsers)
    },

    async loadDept() {
      await loadMetricRangeChart(this, getBPDeptStats, 'initDeptChart')
    },

    async loadTrend() {
      const data = await fetchMetricData(() => getBPTrend(metricDaysForPeriod(this.activePeriod)), {})
      const dates = data.dates || []
      const sysVals = data.systolicValues || []
      const diaVals = data.diastolicValues || []
      this.$nextTick(() => this.initTrendChart(dates, sysVals, diaVals))
    },

    async loadHourly() {
      const rows = await fetchMetricData(() => getBPHourly(getMetricToday()), [])
      const sysVals = createHourlySeries(rows, 'avgSystolic')
      const diaVals = createHourlySeries(rows, 'avgDiastolic')
      this.$nextTick(() => this.initHourlyChart(sysVals, diaVals))
    },

    async loadRealtime() {
      await loadMetricRealtime(this, getBPRealtime)
    },

    // setPageSize → chartPageMixin
  }
}
</script>

<style lang="scss" scoped src="./blood-pressure.scss"></style>
