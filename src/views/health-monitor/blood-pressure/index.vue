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
    </header>

    <!-- ══ 主体 ══ -->
    <section class="bp-bd">

      <!-- ─ 左侧：TOP5 + 部门统计 ─ -->
      <aside class="bp-aside">
        <!-- TOP5 紧凑列表 -->
        <div class="bp-panel bp-aside-top">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">高收缩压 TOP5</span>
          </div>
          <div class="bp-top5-list">
            <div v-if="!top5Data.length" class="bp-top5-empty">暂无高收缩压人员数据</div>
            <div class="bp-top5-row" v-for="(item, i) in top5Data" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="bp-top5-rank" :class="'rank-'+(i+1)">{{ i+1 }}</span>
              <span class="bp-top5-name">{{ item.userName }}</span>
              <div class="bp-top5-bar-wrap">
                <div class="bp-top5-bar" :style="{width: (item.avgSystolic / top5Max * 100) + '%'}"></div>
              </div>
              <span class="bp-top5-val">{{ item.avgSystolic }}</span>
            </div>
          </div>
        </div>

        <!-- 部门统计柱状图 -->
        <div class="bp-panel bp-aside-bot">
          <div class="bp-ph">
            <span class="bp-ph-bar"></span>
            <span class="bp-ph-title">部门平均收缩压</span>
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
            <span class="bp-ph-title">当前异常血压明细</span>
            <span class="bp-anomaly-count" v-if="bpAnomalyList.length">
              共 <em>{{ bpAnomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!bpAnomalyList.length" class="bp-anomaly-empty">
            <span class="bp-anomaly-ok">✓</span> 当前无异常血压人员
          </div>
          <div v-else class="bp-anomaly-body">
            <div class="bp-anomaly-hd">
              <span>姓名</span><span>部门</span><span>收缩压</span><span>舒张压</span><span>等级</span><span>时间</span>
            </div>
            <div class="bp-anomaly-list">
              <div
                class="bp-anomaly-row"
                v-for="(item, i) in bpAnomalyList"
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
            <span>姓名</span><span>收缩</span><span>舒张</span><span>状态</span>
          </div>

          <div class="bp-rt-body" ref="listRef">
            <div
              class="bp-rt-row"
              v-for="(item, i) in pagedList"
              :key="i"
              :class="bpLevel(item)"
              @click="goToPortrait(item)"
              style="cursor:pointer"
            >
              <span class="bp-rt-name">{{ item.userName }}</span>
              <span class="bp-rt-sys">{{ item.systolic }}</span>
              <span class="bp-rt-dia">{{ item.diastolic }}</span>
              <span class="bp-rt-badge" :class="bpLevel(item)">
                {{ bpLevelLabel(item) }}
              </span>
            </div>
          </div>

          <div class="bp-rt-pg">
            <button class="bp-pg-btn" :disabled="currentPage===1" @click="currentPage=1">首页</button>
            <button class="bp-pg-btn" :disabled="currentPage===1" @click="currentPage--">‹</button>
            <span class="bp-pg-info">{{ currentPage }} / {{ totalPages }}</span>
            <button class="bp-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage++">›</button>
            <button class="bp-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage=totalPages">末页</button>
          </div>
        </div>
      </div>

    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts'
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
import { emptyOption, chartTooltip, categoryAxis, valueAxis, deptGrid, trendGrid, hourlyGrid, barLabel } from '@/utils/echarts-config'
import chartPageMixin from '@/mixins/chartPage'
import { PERIOD_OPTIONS } from '@/constants/periods'

export default {
  name: 'BloodPressureAnalysis',
  mixins: [chartPageMixin],
  data() {
    return {
      currentTime: '',
      overview: {
        avgSystolic: 0, avgDiastolic: 0,
        normalRate: 0, abnormalCount: 0,
        detectionCount: 0, elevatedRate: 0, hypertensionRate: 0
      },
      distLegend: [],
      top5Data: [],
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
    pagedList() {
      const s = (this.currentPage - 1) * this.pageSize
      return this.realtimeList.slice(s, s + this.pageSize)
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.realtimeList.length / this.pageSize))
    },
    bpAnomalyList() {
      return this.realtimeList.filter(x => x.systolic >= 140 || x.diastolic >= 90)
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
        { key: 'normal', label: '正常',      range: '< 120 / < 80',    count: normal, pct: pct(normal), color: '#52c41a', icon: '✓', cls: 'zone-normal'  },
        { key: 'pre',    label: '偏高',      range: '120~139 / 80~89', count: pre,    pct: pct(pre),    color: '#FFB84D', icon: '↑', cls: 'zone-pre'     },
        { key: 'stage1', label: '1级高血压', range: '140~159 / 90~99', count: stage1, pct: pct(stage1), color: '#ff7043', icon: '⚠', cls: 'zone-stage1'  },
        { key: 'danger', label: '2级高血压', range: '≥ 160 / ≥ 100',  count: danger, pct: pct(danger), color: '#ff5252', icon: '🚨', cls: 'zone-danger'  }
      ]
    }
  },
  mounted() { this.initPage(() => this.loadRealtime()) },
  methods: {
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
      const { startDate, endDate } = this.periodRange
      try {
        const r = await getBPOverview(startDate, endDate)
        if (r.code === 200) this.overview = r.data || {}
      } catch { this.overview = {} }
    },

    async loadTopUsers() {
      const { startDate, endDate } = this.periodRange
      try {
        const r = await getBPTopUsers(5, startDate, endDate)
        if (r.code === 200) this.top5Data = r.data || []
      } catch { this.top5Data = [] }
    },

    async loadDept() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getBPDeptStats(startDate, endDate)
        if (r.code === 200) d = r.data || []
      } catch {}
      this.$nextTick(() => this.initDeptChart(d))
    },

    async loadDist() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getBPDistribution(startDate, endDate)
        if (r.code === 200) {
          d = (r.data || []).filter(x => x.name && x.value > 0)
          this.distLegend = d
        }
      } catch {}
      this.$nextTick(() => this.initDistChart(d))
    },

    async loadTrend() {
      const days = this.activePeriod === 'day' ? 1 : this.activePeriod === 'week' ? 7 : 30
      let dates = [], sysVals = [], diaVals = []
      try {
        const r = await getBPTrend(days)
        if (r.code === 200 && r.data) {
          dates   = r.data.dates          || []
          sysVals = r.data.systolicValues  || []
          diaVals = r.data.diastolicValues || []
        }
      } catch {}
      this.$nextTick(() => this.initTrendChart(dates, sysVals, diaVals))
    },

    async loadHourly() {
      const today = dayjs().format('YYYY-MM-DD')
      const sysVals = new Array(24).fill(null)
      const diaVals = new Array(24).fill(null)
      try {
        const r = await getBPHourly(today)
        if (r.code === 200 && Array.isArray(r.data)) {
          r.data.forEach(({ hour, avgSystolic, avgDiastolic }) => {
            if (hour >= 0 && hour < 24) {
              sysVals[hour] = avgSystolic
              diaVals[hour] = avgDiastolic
            }
          })
        }
      } catch {}
      this.$nextTick(() => this.initHourlyChart(sysVals, diaVals))
    },

    async loadRealtime() {
      try {
        const r = await getBPRealtime(1000)
        if (r.code === 200) this.realtimeList = r.data || []
      } catch { this.realtimeList = [] }
    },

    // ── ECharts ──
    initDeptChart(data) {
      const el = this.$refs.deptRef; if (!el) return
      if (this.charts.dept) this.charts.dept.dispose()
      const c = echarts.init(el); this.charts.dept = c
      if (!data.length) { c.setOption(emptyOption()); return }
      const d = data.slice(0, 12)
      c.setOption({
        backgroundColor: 'transparent',
        legend: { data: ['收缩压','舒张压'], right: 10, top: 6, textStyle: { color: '#8ba6c8', fontSize: 11 }, itemWidth: 10, itemHeight: 10, icon: 'rect' },
        grid: { ...deptGrid(), top: '14%' },
        xAxis: valueAxis(),
        yAxis: { ...categoryAxis(d.map(x => x.deptName), { show: false }), inverse: true },
        series: [
          {
            name: '收缩压', type: 'bar', stack: 'none', barWidth: '35%',
            data: d.map(x => x.avgSystolic || 0),
            itemStyle: { color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [{ offset: 0, color: '#a78bfa' }, { offset: 1, color: '#7c3aed' }]) },
            label: barLabel()
          },
          {
            name: '舒张压', type: 'bar', stack: 'none', barWidth: '35%',
            data: d.map(x => x.avgDiastolic || 0),
            itemStyle: { color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [{ offset: 0, color: '#38bdf8' }, { offset: 1, color: '#0284c7' }]), borderRadius: [0, 4, 4, 0] },
            label: barLabel()
          }
        ]
      })
    },

    initDistChart(data) {
      const el = this.$refs.distRef; if (!el) return
      if (this.charts.dist) this.charts.dist.dispose()
      const c = echarts.init(el); this.charts.dist = c
      c.setOption({
        backgroundColor: 'transparent',
        series: [{
          type: 'pie', radius: ['52%', '80%'], center: ['50%', '50%'],
          label: { show: false }, labelLine: { show: false },
          data: data.length
            ? data.map(x => ({ value: x.value, name: x.name, itemStyle: { color: x.color, borderRadius: 4, shadowColor: x.color + '66', shadowBlur: 10 } }))
            : [{ name: '暂无数据', value: 1, itemStyle: { color: '#1e3a5f' } }]
        }]
      })
    },

    initTrendChart(dates, sysVals, diaVals) {
      const el = this.$refs.trendRef; if (!el) return
      if (this.charts.trend) this.charts.trend.dispose()
      const c = echarts.init(el); this.charts.trend = c
      const isEmpty = !dates.length
      const fbDates = Array.from({ length: 30 }, (_, i) => dayjs().subtract(29 - i, 'day').format('MM/DD'))
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => {
            const sys = p.find(x => x.seriesName === '收缩压')
            const dia = p.find(x => x.seriesName === '舒张压')
            return `${p[0].name}<br/>` +
              (sys ? `收缩压：<b style="color:#a78bfa">${sys.value || '--'}</b> mmHg<br/>` : '') +
              (dia ? `舒张压：<b style="color:#38bdf8">${dia.value || '--'}</b> mmHg` : '')
          }),
        legend: {
          data: ['收缩压', '舒张压'], right: 10, top: 4,
          textStyle: { color: '#8ba6c8', fontSize: 11 }, itemWidth: 14, itemHeight: 3
        },
        grid: { ...trendGrid(), right: '5%', top: '16%' },
        xAxis: { ...categoryAxis(isEmpty ? fbDates : dates, { fontSize: 10, interval: Math.floor((isEmpty ? fbDates : dates).length / 6) }), boundaryGap: false },
        yAxis: valueAxis({ name: 'mmHg', min: v => Math.max(0, Math.floor(v.min - 8)), max: v => Math.ceil(v.max + 8) }),
        series: [
          {
            name: '收缩压', type: 'line', data: isEmpty ? [] : sysVals,
            smooth: true, symbol: 'none', connectNulls: false,
            lineStyle: { color: '#a78bfa', width: 2 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(167,139,250,0.28)' }, { offset: 1, color: 'rgba(167,139,250,0.02)' }
            ])},
            markLine: { silent: true, symbol: 'none', lineStyle: { color: '#a78bfa55', type: 'dashed' },
              data: [{ yAxis: 139, label: { color: '#a78bfa', fontSize: 10, formatter: '偏高 139' } }] }
          },
          {
            name: '舒张压', type: 'line', data: isEmpty ? [] : diaVals,
            smooth: true, symbol: 'none', connectNulls: false,
            lineStyle: { color: '#38bdf8', width: 2 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(56,189,248,0.2)' }, { offset: 1, color: 'rgba(56,189,248,0.02)' }
            ])},
            markLine: { silent: true, symbol: 'none', lineStyle: { color: '#38bdf855', type: 'dashed' },
              data: [{ yAxis: 89, label: { color: '#38bdf8', fontSize: 10, formatter: '偏高 89' } }] }
          }
        ]
      })
    },

    initHourlyChart(sysVals, diaVals) {
      const el = this.$refs.hourlyRef; if (!el) return
      if (this.charts.hourly) this.charts.hourly.dispose()
      const c = echarts.init(el); this.charts.hourly = c
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => {
            const sys = p.find(x => x.seriesName === '收缩压')
            const dia = p.find(x => x.seriesName === '舒张压')
            return `${p[0].name}<br/>` +
              `收缩压：<b style="color:#a78bfa">${sys?.value ?? '--'}</b> mmHg<br/>` +
              `舒张压：<b style="color:#38bdf8">${dia?.value ?? '--'}</b> mmHg`
          }),
        legend: {
          data: ['收缩压', '舒张压'], right: 4, top: 2,
          textStyle: { color: '#8ba6c8', fontSize: 10 }, itemWidth: 12, itemHeight: 3
        },
        grid: { ...hourlyGrid(), top: '18%' },
        xAxis: { ...categoryAxis(hours, { fontSize: 9, interval: 3, lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: false },
        yAxis: valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)', min: v => v.min > 0 ? v.min - 8 : 50, max: v => v.max > 0 ? v.max + 8 : 160 }),
        series: [
          {
            name: '收缩压', type: 'line', data: sysVals,
            smooth: true, symbol: 'none', connectNulls: false,
            lineStyle: { color: '#a78bfa', width: 1.5 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(167,139,250,0.22)' }, { offset: 1, color: 'rgba(167,139,250,0.02)' }
            ])}
          },
          {
            name: '舒张压', type: 'line', data: diaVals,
            smooth: true, symbol: 'none', connectNulls: false,
            lineStyle: { color: '#38bdf8', width: 1.5 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(56,189,248,0.18)' }, { offset: 1, color: 'rgba(56,189,248,0.02)' }
            ])}
          }
        ]
      })
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


    setPageSize() {
      const el = this.$refs.listRef; if (!el) return
      const ROW_H = 27
      const n = Math.max(10, Math.floor(el.clientHeight / ROW_H))
      if (n !== this.pageSize) {
        this.pageSize = n
        this.currentPage = 1
      }
    },
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/hm-vars';
@import '@/styles/hm-layout';
$purple: #a78bfa;

@include hm-body('bp');
@include hm-main('bp');
@include hm-panel('bp');
@include hm-kpi-cards('bp');
@include hm-pagination('bp');
$sky:    #38bdf8;

// ── Root：自适应视口高度 ──
.bp-root {
  width: 100%;
  height: calc(100vh - 50px) !important; /* 视口高度 - 顶部导航栏 */
  min-height: 600px; /* 最小高度防止过小 */
  background: $bg;
  background-image:
    radial-gradient(circle at 18% 28%, rgba(167,139,250,0.06) 0%, transparent 48%),
    radial-gradient(circle at 82% 72%,  rgba(56,189,248,0.06) 0%, transparent 48%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
  color: $text;
}

// ── Header ──
.bp-hd {
  height: 58px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 22px;
  gap: 20px;
  background: rgba(0, 6, 24, 0.65);
  border-bottom: 1px solid $border;
}
.bp-hd-left { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.bp-live-dot {
  width: 9px; height: 9px;
  border-radius: 50%;
  background: $purple;
  box-shadow: 0 0 8px $purple;
  animation: hmPulse 2s ease-in-out infinite;
}

.bp-hd-title {
  font-size: 20px; font-weight: 700; color: $white; margin: 0;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #a78bfa, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 8px rgba(167,139,250,0.5));
}

.bp-hd-kpis {
  flex: 1; display: flex; justify-content: center;
}
.bp-kpi {
  display: flex; flex-direction: column; align-items: center;
  padding: 0 32px;
  border-right: 1px solid $border;
  &:first-child { border-left: 1px solid $border; }
}
.bp-kpi-n {
  font-size: 20px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1;
  &.kpi-purple { color: $purple; text-shadow: 0 0 10px rgba(167,139,250,0.5); }
  &.kpi-sky    { color: $sky;    text-shadow: 0 0 10px rgba(56,189,248,0.4);  }
  &.kpi-green  { color: #52c41a; text-shadow: 0 0 10px rgba(82,196,26,0.35);  }
  &.kpi-red    { color: #ff5252; text-shadow: 0 0 10px rgba(255,82,82,0.4);   }
}
.bp-kpi-l { font-size: 11px; color: $dim; margin-top: 2px; white-space: nowrap; }
.bp-hd-time { flex-shrink: 0; font-family: 'Consolas', monospace; font-size: 13px; color: $dim; }

// ── Period tabs ──
.bp-period-tabs {
  display: flex;
  background: rgba(167,139,250,0.06);
  border: 1px solid rgba(167,139,250,0.2);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.bp-period-tab {
  padding: 4px 14px;
  font-size: 12px;
  color: $dim;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: $white; background: rgba(167,139,250,0.1); }
  &.is-active { color: $bg; background: $purple; font-weight: 700; }
}

// ── Body（hm-body mixin） ──

// ── Aside（左侧）──
.bp-aside {
  width: 260px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 10px;
}
.bp-aside-top { height: 200px; flex-shrink: 0; }
.bp-aside-bot { flex: 1; }

// ── Main（hm-main mixin） ──
.bp-overview-panel { height: 162px; flex-shrink: 0; }
.bp-mid-row {
  height: 185px; flex-shrink: 0; display: flex; gap: 10px;
}
.bp-panel-trend   { flex: 1; min-width: 0; }
.bp-panel-dist    { flex: 0 0 250px; }
.bp-panel-hourly  { flex: 0 0 280px; }
.bp-panel-zones   { flex-shrink: 0; }
.bp-panel-anomaly { flex-shrink: 0; }

// ── Right list ──
.bp-rtlist { width: 272px; flex-shrink: 0; }

// ── Panel（hm-panel mixin + 页面特有） ──
.bp-ph {
  height: 38px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  border-bottom: 1px solid rgba(0,212,255,0.09);
  background: rgba(167,139,250,0.03);
}
.bp-ph-bar {
  width: 3px; height: 14px;
  background: linear-gradient(180deg, $purple, rgba(167,139,250,0.3));
  border-radius: 2px;
  box-shadow: 0 0 6px rgba(167,139,250,0.7);
}
// ph-title, rt-total, ph-legend, leg-dot, leg-txt, trend-tags, tag, pc → hm-panel mixin

// ── TOP5 紧凑列表 ──
.bp-top5-empty { padding: 20px 0; text-align: center; color: rgba(167,139,250,0.5); font-size: 12px; }
.bp-top5-list {
  padding: 8px 12px;
  display: flex; flex-direction: column; gap: 8px;
}
.bp-top5-row {
  display: flex; align-items: center; gap: 8px;
}
.bp-top5-rank {
  width: 18px; height: 18px; border-radius: 4px;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  &.rank-1 { background: rgba(255,184,77,0.2); color: #FFB84D; border: 1px solid rgba(255,184,77,0.4); }
  &.rank-2 { background: rgba(167,139,250,0.12); color: $purple; border: 1px solid rgba(167,139,250,0.3); }
  &.rank-3 { background: rgba(56,189,248,0.12); color: $sky; border: 1px solid rgba(56,189,248,0.3); }
  &.rank-4, &.rank-5 { background: rgba(168,196,230,0.08); color: #8ba6c8; border: 1px solid rgba(168,196,230,0.2); }
}
.bp-top5-name { font-size: 12px; color: $white; width: 56px; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bp-top5-bar-wrap { flex: 1; height: 6px; background: rgba(167,139,250,0.08); border-radius: 3px; overflow: hidden; }
.bp-top5-bar { height: 100%; border-radius: 3px; background: linear-gradient(90deg, $purple, #7c3aed); transition: width 0.8s ease; }
.bp-top5-val { font-size: 13px; font-weight: 700; color: $purple; font-family: 'Consolas', monospace; width: 28px; text-align: right; flex-shrink: 0; }

// ── 概况面板内容 ──
.bp-overview-body {
  flex: 1; min-height: 0;
  display: flex; align-items: center; padding: 8px 14px; gap: 16px;
}
.bp-dual-wrap {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  background: rgba(167,139,250,0.04);
  border: 1px solid rgba(167,139,250,0.12);
  border-radius: 10px; padding: 10px 16px;
}
.bp-dual-item { text-align: center; }
.bp-dual-val {
  font-size: 38px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1;
}
.bp-dual-label { font-size: 12px; color: $text; margin-top: 4px; }
.bp-dual-unit  { font-size: 10px; color: $dim; font-weight: normal; }
.bp-dual-sub   { font-size: 10px; color: $dim; margin-top: 2px; }
.bp-dual-sep   { font-size: 28px; color: $dim; align-self: center; padding-bottom: 6px; }

// kpi-cards → hm-kpi-cards mixin + purple overrides
.bp-kpi-card {
  background: rgba(167,139,250,0.04);
  border: 1px solid rgba(167,139,250,0.1);
}

// 血压等级说明
.bp-grade-info {
  width: 168px; flex-shrink: 0;
  background: rgba(167,139,250,0.03);
  border: 1px solid rgba(167,139,250,0.1);
  border-radius: 8px;
  padding: 8px 10px;
  display: flex; flex-direction: column; gap: 5px;
}
.bp-grade-title { font-size: 11px; color: $dim; font-weight: 600; margin-bottom: 2px; letter-spacing: 0.5px; }
.bp-grade-item  { display: flex; align-items: center; gap: 6px; }
.bp-grade-dot   { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.bp-grade-name  { font-size: 10px; width: 62px; flex-shrink: 0; }
.bp-grade-val   { font-size: 10px; color: $dim; font-family: 'Consolas', monospace; }

// ── 分布图 ──
.bp-dist-body { flex: 1; min-height: 0; display: flex; align-items: center; gap: 10px; padding: 8px 12px; }
.bp-dist-chart { width: 110px; height: 110px; flex-shrink: 0; }
.bp-dist-legend { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.bp-dist-row { display: flex; align-items: center; gap: 7px; }
.bp-dist-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.bp-dist-name { font-size: 11px; color: $text; flex-shrink: 0; width: 64px; }
.bp-dist-bar-wrap { flex: 1; height: 5px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
.bp-dist-bar { height: 100%; border-radius: 3px; transition: width 0.8s ease; opacity: 0.85; }
.bp-dist-pct { font-size: 14px; font-weight: 700; font-family: 'Consolas', monospace; width: 34px; text-align: right; flex-shrink: 0; }

// ── 血压区间分布统计 ──
.bp-ds-total {
  margin-left: auto; font-size: 12px; color: $dim;
  em { color: #93c5fd; font-style: normal; font-weight: 700; }
}
.bp-ds-body {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 8px; padding: 6px 0 8px;
}
.bp-ds-zone {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px; padding: 10px 8px 8px;
  text-align: center; transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.07); }
  &.zone-normal { border-color: rgba(82,196,26,0.2);   }
  &.zone-pre    { border-color: rgba(255,184,77,0.2);  }
  &.zone-stage1 { border-color: rgba(255,112,67,0.2);  }
  &.zone-danger { border-color: rgba(255,82,82,0.2);   }
}
.bp-ds-icon  { font-size: 16px; margin-bottom: 4px; }
.bp-ds-count { font-size: 24px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1; }
.bp-ds-pct   { font-size: 11px; margin-top: 1px; }
.bp-ds-label { font-size: 13px; font-weight: 600; color: $white; margin-top: 4px; }
.bp-ds-range { font-size: 10px; color: $dim; margin-top: 2px; }
.bp-ds-bar-row {
  display: flex; height: 6px; border-radius: 3px; overflow: hidden;
  background: rgba(255,255,255,0.05); margin-bottom: 2px;
}
.bp-ds-seg { transition: width 0.4s ease; min-width: 0; }

// ── 异常明细面板 ──
.bp-panel-anomaly { flex-shrink: 0; display: flex; flex-direction: column; overflow: hidden; }
.bp-anomaly-count {
  margin-left: auto; font-size: 12px; color: #ff7043;
  em { font-style: normal; font-weight: 700; }
}
.bp-anomaly-empty {
  flex: 1; display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: rgba(82,196,26,0.8); padding: 10px;
}
.bp-anomaly-ok { font-size: 16px; margin-right: 6px; }
.bp-anomaly-body { flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.bp-anomaly-hd {
  display: grid; grid-template-columns: 64px 1fr 72px 72px 80px 90px;
  gap: 6px; padding: 4px 10px; flex-shrink: 0;
  background: rgba(255,112,67,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.bp-anomaly-list {
  flex: 1; overflow-y: auto; padding: 3px 6px; max-height: 110px;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,112,67,0.2); border-radius: 2px; }
}
.bp-anomaly-row {
  display: grid; grid-template-columns: 64px 1fr 72px 72px 80px 90px;
  gap: 6px; padding: 5px 4px; margin-bottom: 1px;
  border-radius: 4px; align-items: center;
  border-left: 2px solid transparent;
  transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.04); }
  &.anom-danger { border-left-color: rgba(255,82,82,0.6);  background: rgba(255,82,82,0.04);  }
  &.anom-stage1 { border-left-color: rgba(255,112,67,0.6); background: rgba(255,112,67,0.04); }
}
.ba-name  { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ba-dept  { font-size: 11px; color: $dim; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ba-sys   {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace; color: $purple;
  .anom-danger & { color: #ff5252; }
}
.ba-dia   {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace; color: $sky;
  .anom-danger & { color: #ff7043; }
}
.ba-level {
  font-size: 11px; padding: 1px 5px; border-radius: 3px; text-align: center;
  .anom-danger & { color: #ff5252; background: rgba(255,82,82,0.12);  border: 1px solid rgba(255,82,82,0.25);  }
  .anom-stage1 & { color: #ff7043; background: rgba(255,112,67,0.12); border: 1px solid rgba(255,112,67,0.25); }
}
.ba-time  { font-size: 10px; color: $dim; }

// ── 实时列表 ──
.bp-rt-hd {
  display: grid; grid-template-columns: 64px 40px 40px 1fr;
  gap: 6px; padding: 6px 10px; flex-shrink: 0;
  background: rgba(167,139,250,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.bp-rt-body {
  flex: 1; overflow-y: auto; padding: 3px 6px; min-height: 0;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(167,139,250,0.18); border-radius: 2px; }
}
.bp-rt-row {
  display: grid; grid-template-columns: 64px 40px 40px 1fr;
  gap: 6px; padding: 6px 4px; margin-bottom: 1px;
  border-radius: 5px; align-items: center;
  border-left: 2px solid transparent;
  transition: background 0.2s;
  &:hover { background: rgba(167,139,250,0.055); }
  &.normal { border-left-color: rgba(82,196,26,0.45); }
  &.pre    { border-left-color: rgba(255,184,77,0.55); }
  &.stage1 { border-left-color: rgba(255,112,67,0.6); }
  &.danger { border-left-color: rgba(255,82,82,0.7); }
}
.bp-rt-name { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bp-rt-sys  {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace; color: $purple;
  .bp-rt-row.danger & { color: #ff5252; }
  .bp-rt-row.stage1 & { color: #ff7043; }
  .bp-rt-row.pre    & { color: #FFB84D; }
}
.bp-rt-dia  {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace; color: $sky;
  .bp-rt-row.danger & { color: #ff7043; }
}
.bp-rt-badge {
  font-size: 10px; padding: 1px 4px; border-radius: 3px; text-align: center;
  &.normal { background: rgba(82,196,26,0.13);  color: #52c41a; border: 1px solid rgba(82,196,26,0.28); }
  &.pre    { background: rgba(255,184,77,0.13); color: #FFB84D; border: 1px solid rgba(255,184,77,0.28); }
  &.stage1 { background: rgba(255,112,67,0.13); color: #ff7043; border: 1px solid rgba(255,112,67,0.28); }
  &.danger { background: rgba(255,82,82,0.13);  color: #ff5252; border: 1px solid rgba(255,82,82,0.28); }
}

// pagination → hm-pagination mixin + purple overrides
.bp-pg-btn {
  background: rgba(167,139,250,0.07); border-color: rgba(167,139,250,0.18);
  color: $purple;
  &:hover:not(:disabled) { background: rgba(167,139,250,0.16); }
}
.bp-pg-info { font-size: 12px; color: $purple; min-width: 44px; text-align: center; }
</style>
