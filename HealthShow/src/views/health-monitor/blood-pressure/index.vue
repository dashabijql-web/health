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
            <div class="bp-top5-row" v-for="(item, i) in displayedTop5" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="bp-top5-rank" :class="i < 3 ? 'rank-'+(i+1) : 'rank-n'">{{ i+1 }}</span>
              <span class="bp-top5-name">{{ item.userName }}</span>
              <div class="bp-top5-bar-wrap">
                <div class="bp-top5-bar" :style="{width: (item.avgSystolic / top5Max * 100) + '%'}"></div>
              </div>
              <span class="bp-top5-val">{{ item.avgSystolic }}</span>
            </div>
            <div v-if="top5Data.length > 20" class="bp-top5-more" @click="top5Expanded = !top5Expanded">
              {{ top5Expanded ? '▲ 收起' : '▼ 展开全部 (' + top5Data.length + '条)' }}
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

        <!-- 概况：双值 + KPI cards + 血压等级说明 -->
        <div class="bp-panel bp-overview-panel">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">{{ overviewTitle }}</span>
          </div>
          <div class="bp-overview-body">
            <!-- 双值显示区 -->
            <div class="bp-dual-wrap">
              <div class="bp-dual-item">
                <div class="bp-dual-val" style="color:#a78bfa">{{ overview.avgSystolic || '--' }}</div>
                <div class="bp-dual-label">收缩压 <span class="bp-dual-unit">mmHg</span></div>
                <div class="bp-dual-sub">正常 90~139</div>
              </div>
              <div class="bp-dual-sep">/</div>
              <div class="bp-dual-item">
                <div class="bp-dual-val" style="color:#38bdf8">{{ overview.avgDiastolic || '--' }}</div>
                <div class="bp-dual-label">舒张压 <span class="bp-dual-unit">mmHg</span></div>
                <div class="bp-dual-sub">正常 60~89</div>
              </div>
            </div>
            <!-- KPI cards -->
            <div class="bp-kpi-cards">
              <div class="bp-kpi-card" v-for="c in ovCards" :key="c.label">
                <div class="bp-kpi-card-val" :style="{color: c.color}">{{ c.val }}<span class="bp-kpi-card-unit">{{ c.unit }}</span></div>
                <div class="bp-kpi-card-label">{{ c.label }}</div>
              </div>
            </div>
            <!-- 血压等级说明 -->
            <div class="bp-grade-info">
              <div class="bp-grade-title">血压等级参考</div>
              <div class="bp-grade-item" v-for="g in bpGrades" :key="g.label">
                <span class="bp-grade-dot" :style="{background: g.color}"></span>
                <span class="bp-grade-name" :style="{color: g.color}">{{ g.label }}</span>
                <span class="bp-grade-val">{{ g.range }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- mid-row：趋势 + 分布 + 小时均值 -->
        <div class="bp-mid-row">
          <!-- 每日趋势折线 -->
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

          <!-- 分布饼图 -->
          <div class="bp-panel bp-panel-dist">
            <div class="bp-ph">
              <span class="bp-ph-bar"></span>
              <span class="bp-ph-title">血压等级分布</span>
            </div>
            <div class="bp-dist-body">
              <div ref="distRef" class="bp-dist-chart"></div>
              <div class="bp-dist-legend">
                <div class="bp-dist-row" v-for="d in distLegend" :key="d.name">
                  <div class="bp-dist-dot" :style="{background: d.color}"></div>
                  <span class="bp-dist-name">{{ d.name }}</span>
                  <div class="bp-dist-bar-wrap">
                    <div class="bp-dist-bar" :style="{width: d.value + '%', background: d.color}"></div>
                  </div>
                  <span class="bp-dist-pct" :style="{color: d.color}">{{ d.value }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 小时均值图 -->
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

        <!-- 血压区间分布统计 -->
        <div class="bp-panel bp-panel-zones">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">当前在线人员血压分布</span>
            <span class="bp-ds-total">共 <em>{{ realtimeList.length }}</em> 人在线</span>
          </div>
          <div class="bp-ds-body">
            <div class="bp-ds-zone" :class="z.cls" v-for="z in bpZones" :key="z.key">
              <div class="bp-ds-icon" :style="{color: z.color}">{{ z.icon }}</div>
              <div class="bp-ds-count" :style="{color: z.color}">{{ z.count }}</div>
              <div class="bp-ds-pct" :style="{color: z.color}">{{ z.pct }}%</div>
              <div class="bp-ds-label">{{ z.label }}</div>
              <div class="bp-ds-range">{{ z.range }}</div>
            </div>
          </div>
          <div class="bp-ds-bar-row">
            <div class="bp-ds-seg" v-for="z in bpZones" :key="z.key"
              :style="{width: z.pct + '%', background: z.color}"
              :title="z.label + ': ' + z.count + '人'"></div>
          </div>
        </div>

        <!-- 当前异常血压明细 -->
        <div class="bp-panel bp-panel-anomaly">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">当前异常血压明细{{ filterDept ? ' — ' + filterDept : '' }}</span>
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
                :class="item.systolic >= 160 || item.diastolic >= 100 ? 'anom-danger' : 'anom-stage1'"
                @click="goToPortrait(item)"
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

      <!-- ─ 右侧：实时血压列表 ─ -->
      <div class="bp-rtlist">
        <div class="bp-panel hm-panel-flex">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">实时血压数据</span>
            <span class="bp-rt-total">{{ realtimeList.length }} 条</span>
          </div>

          <div class="bp-rt-hd">
            <span>#</span><span>姓名</span><span>收缩</span><span>舒张</span><span>状态</span>
          </div>

          <div class="bp-rt-body" ref="listRef">
            <div
              class="bp-rt-row"
              v-for="(item, i) in filteredRealtimeList"
              :key="i"
              :class="bpLevel(item)"
              @click="goToPortrait(item)"
              style="cursor:pointer"
            >
              <span class="bp-rt-idx">{{ i + 1 }}</span>
              <span class="bp-rt-name">{{ item.userName }}</span>
              <span class="bp-rt-sys">{{ item.systolic }}</span>
              <span class="bp-rt-dia">{{ item.diastolic }}</span>
              <span class="bp-rt-badge" :class="bpLevel(item)">
                {{ bpLevelLabel(item) }}
              </span>
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
  getBPOverview,
  getBPTrend,
  getBPDistribution,
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
  loadMetricDistribution,
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
      distLegend: [],
      top5Data: [],
      top5Expanded: false,
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
      currentPage: 1,
      pageSize: 20,
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
    ovCards() {
      const o = this.overview
      return [
        { label: '正常率',    val: o.normalRate      || '--', unit: '%',  color: '#52c41a' },
        { label: '偏高率',    val: o.elevatedRate    || '--', unit: '%',  color: '#FFB84D' },
        { label: '高血压率',  val: o.hypertensionRate|| '--', unit: '%',  color: '#ff5252' },
        { label: '检测人数',  val: o.detectionCount  || '--', unit: ' 人',color: '#7eb8f7' },
        { label: '异常次数',  val: (o.abnormalCount  || 0).toLocaleString(), unit: ' 次', color: '#ff7043' },
        { label: '血压范围',  val: o.avgSystolic && o.avgDiastolic ? `${o.avgSystolic}/${o.avgDiastolic}` : '--', unit: '', color: '#a78bfa' }
      ]
    },
    overviewTitle() {
      return { day: '今日血压概况', week: '近7日血压概况', month: '近30日血压概况' }[this.activePeriod]
    },
    trendTitle() {
      return { day: '今日血压趋势', week: '近7天血压趋势', month: '近30天血压趋势' }[this.activePeriod]
    },
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.avgSystolic || 0), 160) : 160
    },
    displayedTop5() {
      return this.top5Expanded ? this.top5Data : this.top5Data.slice(0, 20)
    },
    top5Title() {
      const p = { day: '今日', week: '近7日', month: '近30日' }[this.activePeriod]
      return p + '高收缩压排行'
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
    this.initAutoPageSize(27)
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
        this.loadDist(),
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

    async loadDist() {
      await loadMetricDistribution(this, getBPDistribution, 'initDistChart')
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

    bpLevel(item) {
      if (item.systolic >= 160 || item.diastolic >= 100) return 'danger'
      if (item.systolic >= 140 || item.diastolic >= 90)  return 'stage1'
      if (item.systolic >= 120 || item.diastolic >= 80)  return 'pre'
      return 'normal'
    },
    bpLevelLabel(item) {
      const lv = this.bpLevel(item)
      return { normal: '正常', pre: '偏高', stage1: '1级', danger: '2级' }[lv]
    },


    // setPageSize → chartPageMixin
  }
}
</script>

<style lang="scss" scoped src="./blood-pressure.scss"></style>
