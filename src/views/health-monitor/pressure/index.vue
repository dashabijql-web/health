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
    </header>

    <!-- ══ 主体 ══ -->
    <section class="ps-bd">

      <!-- ─ 左侧：TOP5 紧凑列表 + 部门柱状图 ─ -->
      <aside class="ps-aside">
        <!-- TOP5：高压力人员 -->
        <div class="ps-panel ps-aside-top">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">高压力 TOP5</span>
          </div>
          <div class="ps-top5-list">
            <div v-if="!top5Data.length" class="ps-top5-empty">暂无高压力数据</div>
            <div class="ps-top5-row" v-for="(item, i) in top5Data" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="ps-top5-rank" :class="'rank-'+(i+1)">{{ i+1 }}</span>
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
          </div>
        </div>

        <!-- 部门平均压力柱状图 -->
        <div class="ps-panel ps-aside-bot">
          <div class="ps-ph">
            <span class="ps-ph-bar"></span>
            <span class="ps-ph-title">部门平均压力指数</span>
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
            <span class="ps-ph-title">当前异常压力明细</span>
            <span class="ps-anomaly-count" v-if="psAnomalyList.length">
              共 <em>{{ psAnomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!psAnomalyList.length" class="ps-anomaly-empty">
            <span class="ps-anomaly-ok">✓</span> 当前无异常压力人员
          </div>
          <div v-else class="ps-anomaly-body">
            <div class="ps-anomaly-hd">
              <span>姓名</span><span>部门</span><span>压力指数</span><span>等级</span><span>时间</span>
            </div>
            <div class="ps-anomaly-list">
              <div
                class="ps-anomaly-row"
                v-for="(item, i) in psAnomalyList"
                :key="i"
                :class="item.pressure >= 85 ? 'anom-high' : 'anom-elevated'"
                @click="goToPortrait(item)"
                style="cursor:pointer"
              >
                <span class="pa-name">{{ item.userName }}</span>
                <span class="pa-dept">{{ item.deptName || '--' }}</span>
                <span class="pa-val">{{ item.pressure }}</span>
                <span class="pa-type">{{ item.pressure >= 85 ? '高压⚠' : '偏高!' }}</span>
                <span class="pa-time">{{ fmtTime(item.recordTime) }}</span>
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
            <span>姓名</span><span>压力</span><span>状态</span><span>时间</span>
          </div>

          <div class="ps-rt-body" ref="listRef">
            <div
              class="ps-rt-row"
              v-for="(item, i) in pagedList"
              :key="i"
              :class="psLevel(item.pressure)"
              @click="goToPortrait(item)"
              style="cursor:pointer"
            >
              <span class="ps-rt-name">{{ item.userName }}</span>
              <span class="ps-rt-val">{{ item.pressure }}</span>
              <span class="ps-rt-badge" :class="psLevel(item.pressure)">
                {{ item.pressure >= 85 ? '高压' : item.pressure >= 70 ? '偏高' : item.pressure >= 50 ? '正常' : '放松' }}
              </span>
              <span class="ps-rt-time">{{ fmtTime(item.recordTime) }}</span>
            </div>
          </div>

          <div class="ps-rt-pg">
            <button class="ps-pg-btn" :disabled="currentPage===1" @click="currentPage=1">首页</button>
            <button class="ps-pg-btn" :disabled="currentPage===1" @click="currentPage--">‹</button>
            <span class="ps-pg-info">{{ currentPage }} / {{ totalPages }}</span>
            <button class="ps-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage++">›</button>
            <button class="ps-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage=totalPages">末页</button>
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
  getPressureOverview,
  getPressureTrend,
  getPressureDistribution,
  getPressureTopUsers,
  getPressureDeptStats,
  getPressureRealtime,
  getPressureHourly
} from '@/api/pressure'
import { emptyOption, chartTooltip, categoryAxis, valueAxis, deptGrid, trendGrid, hourlyGrid, barLabel } from '@/utils/echarts-config'
import chartPageMixin from '@/mixins/chartPage'
import { PERIOD_OPTIONS } from '@/constants/periods'

export default {
  name: 'PressureAnalysis',
  mixins: [chartPageMixin],
  data() {
    return {
      currentTime: '',
      overview: {},
      distLegend: [],
      top5Data: [],
      deptData: [],
      realtimeList: [],
      currentPage: 1,
      pageSize: 20,
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
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
    /* pagedList / totalPages from chartPageMixin */
    psAnomalyList() {
      return this.realtimeList.filter(x => x.pressure >= 70)
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
        { key: 'normal',   label: '正常', range: '50–69',   count: normal,   pct: pct(normal),   color: '#52c41a', icon: '✓', cls: 'zone-normal'   },
        { key: 'elevated', label: '偏高', range: '70–84',   count: elevated, pct: pct(elevated), color: '#FFB84D', icon: '!', cls: 'zone-elevated' },
        { key: 'high',     label: '高压', range: '≥ 85',    count: high,     pct: pct(high),     color: '#ff5252', icon: '⚠', cls: 'zone-high'     }
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
        this.loadHourly(),
        this.loadRealtime()
      ])
    },

    async loadOverview() {
      const { startDate, endDate } = this.periodRange
      try {
        const r = await getPressureOverview(startDate, endDate)
        if (r.code === 200) this.overview = r.data || {}
      } catch { this.overview = {} }
      this.$nextTick(() => this.initGauge())
    },

    async loadTopUsers() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getPressureTopUsers(5, startDate, endDate)
        if (r.code === 200) d = r.data || []
      } catch {}
      this.top5Data = d
    },

    async loadDept() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getPressureDeptStats(startDate, endDate)
        if (r.code === 200) d = r.data || []
      } catch {}
      this.deptData = d
      this.$nextTick(() => this.initDept(d))
    },

    async loadDist() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getPressureDistribution(startDate, endDate)
        if (r.code === 200) {
          d = (r.data || []).filter(x => x.name && x.value > 0)
          this.distLegend = d
        }
      } catch {}
      this.$nextTick(() => this.initDist(d))
    },

    async loadHourly() {
      if (this.activePeriod === 'day') {
        const today = dayjs().format('YYYY-MM-DD')
        const vals = new Array(24).fill(null)
        try {
          const r = await getPressureHourly(today)
          if (r.code === 200 && Array.isArray(r.data)) {
            r.data.forEach(({ hour, avgPressure }) => {
              if (hour >= 0 && hour < 24) vals[hour] = avgPressure
            })
          }
        } catch {}
        this.$nextTick(() => this.renderHourly(vals))
      } else {
        const days = this.activePeriod === 'week' ? 7 : 30
        let dates = [], vals = []
        try {
          const r = await getPressureTrend(days)
          if (r.code === 200 && r.data) {
            dates = r.data.dates  || []
            vals  = r.data.values || []
          }
        } catch {}
        this.$nextTick(() => this.renderHourlyDaily(dates, vals))
      }
    },

    async loadRealtime() {
      try {
        const r = await getPressureRealtime(1000)
        if (r.code === 200) this.realtimeList = r.data || []
      } catch {}
    },

    // ── ECharts ──

    initGauge() {
      const el = this.$refs.gaugeRef; if (!el) return
      if (this.charts.gauge) this.charts.gauge.dispose()
      const c = echarts.init(el); this.charts.gauge = c
      const v = this.overview.avgPressure || 0
      c.setOption({
        series: [{
          type: 'gauge',
          startAngle: 225, endAngle: -45,
          radius: '90%', center: ['50%', '58%'],
          min: 0, max: 100,
          axisLine: {
            lineStyle: {
              width: 16,
              color: [
                [0.50, '#4FC3F7'],
                [0.70, '#52c41a'],
                [0.85, '#FFB84D'],
                [1.00, '#ff5252']
              ]
            }
          },
          pointer: {
            length: '60%', width: 6,
            itemStyle: { color: '#fb923c', shadowBlur: 14, shadowColor: 'rgba(251,146,60,0.8)' }
          },
          axisTick:  { length: 5,  distance: -22, lineStyle: { color: 'rgba(251,146,60,0.25)', width: 1 } },
          splitLine: { length: 10, distance: -22, lineStyle: { color: 'rgba(251,146,60,0.45)', width: 2 } },
          axisLabel: { color: '#8ba6c8', fontSize: 10, distance: -28 },
          detail:    { show: false },
          data:      [{ value: v }]
        }]
      })
    },

    initDept(data) {
      const el = this.$refs.deptRef; if (!el) return
      if (this.charts.dept) this.charts.dept.dispose()
      const c = echarts.init(el); this.charts.dept = c
      if (!data.length) { c.setOption(emptyOption()); return }
      const d = data.slice(0, 10)
      c.setOption({
        backgroundColor: 'transparent',
        grid: { ...deptGrid(), top: '8%' },
        xAxis: { ...valueAxis(), min: 0, max: 100 },
        yAxis: { ...categoryAxis(d.map(x => x.deptName), { show: false }), inverse: true },
        series: [{
          name: '平均压力', type: 'bar', barWidth: '46%',
          data: d.map(x => ({
            value: x.avgPressure,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                { offset: 0, color: x.avgPressure >= 85 ? '#ff5252' : x.avgPressure >= 70 ? '#FFB84D' : '#fb923c' },
                { offset: 1, color: x.avgPressure >= 85 ? '#b91c1c' : x.avgPressure >= 70 ? '#d97706' : '#c2410c' }
              ]),
              borderRadius: [0, 4, 4, 0]
            }
          })),
          label: barLabel(),
          markLine: {
            silent: true, lineStyle: { color: '#FFB84D55', type: 'dashed' },
            data: [{ xAxis: 70, name: '偏高线' }]
          }
        }]
      })
    },

    initDist(data) {
      const el = this.$refs.distRef; if (!el) return
      if (this.charts.dist) this.charts.dist.dispose()
      const c = echarts.init(el); this.charts.dist = c
      c.setOption({
        backgroundColor: 'transparent',
        series: [{
          type: 'pie', radius: ['52%', '80%'], center: ['50%', '50%'],
          label: { show: false }, labelLine: { show: false },
          data: data.length
            ? data.map(x => ({
                value: x.value, name: x.name,
                itemStyle: { color: x.color, borderRadius: 4, shadowColor: x.color + '66', shadowBlur: 10 }
              }))
            : [{ name: '暂无数据', value: 1, itemStyle: { color: '#1e3a5f' } }]
        }]
      })
    },

    renderHourly(vals) {
      const el = this.$refs.hourlyRef; if (!el) return
      if (this.charts.hourly) this.charts.hourly.dispose()
      const c = echarts.init(el); this.charts.hourly = c
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => p[0].value != null
            ? `${p[0].name}<br/>压力指数：<b style="color:#fb923c">${p[0].value}</b>`
            : `${p[0].name}<br/>暂无数据`),
        grid: hourlyGrid(),
        xAxis: { ...categoryAxis(hours, { fontSize: 9, interval: 3, lineColor: 'rgba(251,146,60,0.15)' }), boundaryGap: false },
        yAxis: { ...valueAxis({ fontSize: 9, splitColor: 'rgba(251,146,60,0.06)' }), min: 0, max: 100 },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#fb923c', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(251,146,60,0.28)' },
            { offset: 1, color: 'rgba(251,146,60,0.02)' }
          ])},
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: 70, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '偏高 70' } },
              { yAxis: 85, lineStyle: { color: '#ff5252', type: 'dashed', width: 1 }, label: { color: '#ff5252', fontSize: 10, formatter: '高压 85' } }
            ]
          }
        }]
      })
    },

    renderHourlyDaily(dates, vals) {
      const el = this.$refs.hourlyRef; if (!el) return
      if (this.charts.hourly) this.charts.hourly.dispose()
      const c = echarts.init(el); this.charts.hourly = c
      if (!dates.length) { c.setOption(emptyOption('暂无数据', 13)); return }
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => `${p[0].name}<br/>压力指数：<b style="color:#fb923c">${p[0].value}</b>`),
        grid: hourlyGrid(),
        xAxis: { ...categoryAxis(dates, { fontSize: 9, interval: Math.floor(dates.length / 5), lineColor: 'rgba(251,146,60,0.15)' }), boundaryGap: true },
        yAxis: { ...valueAxis({ fontSize: 9, splitColor: 'rgba(251,146,60,0.06)' }), min: 0, max: 100 },
        series: [{
          type: 'bar', data: vals, barMaxWidth: 14,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#fb923c' },
              { offset: 1, color: 'rgba(251,146,60,0.2)' }
            ]),
            borderRadius: [3, 3, 0, 0]
          },
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: 70, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '偏高 70' } },
              { yAxis: 85, lineStyle: { color: '#ff5252', type: 'dashed', width: 1 }, label: { color: '#ff5252', fontSize: 10, formatter: '高压 85' } }
            ]
          }
        }]
      })
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

<style lang="scss" scoped>
$accent: #fb923c;
@import '@/styles/hm-vars';
@import '@/styles/hm-layout';
$cyan:   #00d4ff;

@include hm-body('ps');
@include hm-main('ps');
@include hm-panel('ps');
@include hm-overview('ps');
@include hm-kpi-cards('ps');
@include hm-range-info('ps');
@include hm-pagination('ps');

// ── Root ──
.ps-root {
  width: 100%;
  height: calc(100vh - 50px) !important; /* 视口高度 - 顶部导航栏 */
  min-height: 600px; /* 最小高度防止过小 */
  background: $bg;
  background-image:
    radial-gradient(circle at 18% 28%, rgba(251,146,60,0.05) 0%, transparent 48%),
    radial-gradient(circle at 82% 72%, rgba(42,82,152,0.08) 0%, transparent 48%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
  color: $text;
}

// ── Header ──
.ps-hd {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 22px;
  gap: 20px;
  background: rgba(0, 6, 24, 0.65);
  border-bottom: 1px solid $border;
}
.ps-hd-left { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.ps-live-dot {
  width: 9px; height: 9px;
  border-radius: 50%;
  background: $accent;
  box-shadow: 0 0 8px $accent;
  animation: hmPulse 2s ease-in-out infinite;
}

.ps-hd-title {
  font-size: 20px; font-weight: 700; color: $white; margin: 0;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #fb923c, #fde68a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: none;
  filter: drop-shadow(0 0 10px rgba(251,146,60,0.5));
}

.ps-hd-kpis {
  flex: 1; display: flex; justify-content: center;
}
.ps-kpi {
  display: flex; flex-direction: column; align-items: center;
  padding: 0 32px;
  border-right: 1px solid $border;
  &:first-child { border-left: 1px solid $border; }
}
.ps-kpi-n {
  font-size: 20px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1;
  &.kpi-orange { color: $accent;  text-shadow: 0 0 10px rgba(251,146,60,0.5); }
  &.kpi-green  { color: #52c41a; text-shadow: 0 0 10px rgba(82,196,26,0.35);  }
  &.kpi-yellow { color: #FFB84D; text-shadow: 0 0 10px rgba(255,184,77,0.4);  }
  &.kpi-red    { color: #ff5252; text-shadow: 0 0 10px rgba(255,82,82,0.4);   }
}
.ps-kpi-l { font-size: 11px; color: $dim; margin-top: 2px; white-space: nowrap; }
.ps-hd-time { flex-shrink: 0; font-family: 'Consolas', monospace; font-size: 13px; color: $dim; }

.ps-period-tabs {
  display: flex;
  background: rgba(251,146,60,0.06);
  border: 1px solid rgba(251,146,60,0.2);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.ps-period-tab {
  padding: 4px 14px;
  font-size: 12px;
  color: $dim;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: $white; background: rgba(251,146,60,0.1); }
  &.is-active { color: $bg; background: $accent; font-weight: 700; }
}

// ── Body（hm-body mixin） ──

// ── Aside（左侧）──
.ps-aside {
  width: 260px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 10px;
}
.ps-aside-top { height: 200px; flex-shrink: 0; }
.ps-aside-bot { flex: 1; }

// TOP5 紧凑列表
.ps-top5-empty { padding: 20px 0; text-align: center; color: rgba(251,146,60,0.5); font-size: 12px; }
.ps-top5-list {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.ps-top5-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ps-top5-rank {
  width: 18px; height: 18px;
  border-radius: 4px;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  &.rank-1 { background: rgba(255,184,77,0.2); color: #FFB84D; border: 1px solid rgba(255,184,77,0.4); }
  &.rank-2 { background: rgba(251,146,60,0.12); color: #fb923c; border: 1px solid rgba(251,146,60,0.3); }
  &.rank-3 { background: rgba(82,196,26,0.12); color: #52c41a; border: 1px solid rgba(82,196,26,0.3); }
  &.rank-4, &.rank-5 { background: rgba(168,196,230,0.08); color: #8ba6c8; border: 1px solid rgba(168,196,230,0.2); }
}
.ps-top5-name { font-size: 12px; color: $white; width: 60px; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ps-top5-bar-wrap { flex: 1; height: 6px; background: rgba(251,146,60,0.08); border-radius: 3px; overflow: hidden; }
.ps-top5-bar { height: 100%; border-radius: 3px; transition: width 0.8s ease; }
.ps-top5-val { font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace; width: 26px; text-align: right; flex-shrink: 0; }

// ── Main（hm-main mixin） ──
.ps-overview-panel { height: 162px; flex-shrink: 0; }
.ps-mid-row        { height: 190px; flex-shrink: 0; display: flex; gap: 10px; }
.ps-panel-hourly   { flex: 1; }
.ps-panel-dist     { flex: 0 0 258px; }

// ── Right list ──
.ps-rtlist { width: 272px; flex-shrink: 0; }

// ── Panel（hm-panel mixin + 页面特有） ──
.ps-ph {
  height: 38px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  border-bottom: 1px solid rgba(0,212,255,0.09);
  background: rgba(251,146,60,0.03);
}
.ps-ph-bar {
  width: 3px; height: 14px;
  background: linear-gradient(180deg, $accent, rgba(251,146,60,0.3));
  border-radius: 2px;
  box-shadow: 0 0 6px rgba(251,146,60,0.7);
}
// ph-title, rt-total, trend-tags, tag, pc → hm-panel mixin

// overview + kpi-cards → hm-overview + hm-kpi-cards mixins
// KPI 小卡片 orange overrides
.ps-kpi-cards {
}
.ps-kpi-card {
  background: rgba(251,146,60,0.04);
  border: 1px solid rgba(251,146,60,0.1);
  border-radius: 7px;
  padding: 7px 10px;
  display: flex; flex-direction: column; justify-content: center;
}
// kpi-card-val, kpi-card-unit, kpi-card-label → hm-kpi-cards mixin
// 压力等级说明 - ps uses orange colors + wider name, override mixin defaults
.ps-range-info { background: rgba(251,146,60,0.03); border-color: rgba(251,146,60,0.1); }
.ps-range-name { width: 78px; }

// ── 分布图 ──
.ps-dist-body { flex: 1; min-height: 0; display: flex; align-items: center; gap: 10px; padding: 8px 12px; }
.ps-dist-chart { width: 120px; height: 120px; flex-shrink: 0; }
.ps-dist-legend { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.ps-dist-row { display: flex; align-items: center; gap: 7px; }
.ps-dist-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ps-dist-name { font-size: 11px; color: $text; flex-shrink: 0; width: 56px; }
.ps-dist-bar-wrap { flex: 1; height: 5px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
.ps-dist-bar { height: 100%; border-radius: 3px; transition: width 0.8s ease; opacity: 0.85; }
.ps-dist-pct { font-size: 14px; font-weight: 700; font-family: 'Consolas', monospace; width: 34px; text-align: right; flex-shrink: 0; }

// ── 实时列表 ──
.ps-rt-hd {
  display: grid; grid-template-columns: 64px 44px 42px 1fr;
  gap: 6px; padding: 6px 10px; flex-shrink: 0;
  background: rgba(251,146,60,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.ps-rt-body {
  flex: 1; overflow-y: auto; padding: 3px 6px; min-height: 0;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(251,146,60,0.18); border-radius: 2px; }
}
.ps-rt-row {
  display: grid; grid-template-columns: 64px 44px 42px 1fr;
  gap: 6px; padding: 6px 4px; margin-bottom: 1px;
  border-radius: 5px; align-items: center;
  border-left: 2px solid transparent;
  transition: background 0.2s;
  &:hover { background: rgba(251,146,60,0.055); }
  &.relaxed  { border-left-color: rgba(79,195,247,0.55);  }
  &.normal   { border-left-color: rgba(82,196,26,0.45);   }
  &.elevated { border-left-color: rgba(255,184,77,0.55);  }
  &.high     { border-left-color: rgba(255,82,82,0.65);   }
}
.ps-rt-name { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ps-rt-val {
  font-size: 14px; font-weight: 700; font-family: 'Consolas', monospace; color: #52c41a;
  .ps-rt-row.elevated & { color: #FFB84D; }
  .ps-rt-row.high     & { color: #ff5252; }
  .ps-rt-row.relaxed  & { color: #4FC3F7; }
}
.ps-rt-badge {
  font-size: 10px; padding: 1px 4px; border-radius: 3px; text-align: center;
  &.relaxed  { background: rgba(79,195,247,0.13);  color: #4FC3F7; border: 1px solid rgba(79,195,247,0.28);  }
  &.normal   { background: rgba(82,196,26,0.13);   color: #52c41a; border: 1px solid rgba(82,196,26,0.28);   }
  &.elevated { background: rgba(255,184,77,0.13);  color: #FFB84D; border: 1px solid rgba(255,184,77,0.28);  }
  &.high     { background: rgba(255,82,82,0.13);   color: #ff5252; border: 1px solid rgba(255,82,82,0.28);   }
}
.ps-rt-time { font-size: 10px; color: $dim; }

// pagination → hm-pagination mixin + orange overrides
.ps-rt-pg { border-top-color: rgba(251,146,60,0.1); }
.ps-pg-btn {
  background: rgba(251,146,60,0.07); border-color: rgba(251,146,60,0.18);
  color: $accent;
  &:hover:not(:disabled) { background: rgba(251,146,60,0.16); }
}
.ps-pg-info { font-size: 12px; color: $accent; min-width: 44px; text-align: center; }

// ── 异常明细面板 ──
.ps-panel-anomaly { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-height: 0; }
.ps-anomaly-count {
  margin-left: auto; font-size: 12px; color: #FFB84D;
  em { font-style: normal; font-weight: 700; }
}
.ps-anomaly-empty {
  flex: 1; display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: rgba(82,196,26,0.8);
  .ps-anomaly-ok { font-size: 16px; margin-right: 6px; }
}
.ps-anomaly-body { flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.ps-anomaly-hd {
  display: grid; grid-template-columns: 64px 1fr 80px 52px 88px;
  gap: 6px; padding: 4px 10px; flex-shrink: 0;
  background: rgba(255,184,77,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.ps-anomaly-list {
  flex: 1; overflow-y: auto; padding: 3px 6px;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,184,77,0.2); border-radius: 2px; }
}
.ps-anomaly-row {
  display: grid; grid-template-columns: 64px 1fr 80px 52px 88px;
  gap: 6px; padding: 5px 4px; margin-bottom: 1px;
  border-radius: 4px; align-items: center;
  border-left: 2px solid transparent;
  transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.04); }
  &.anom-high     { border-left-color: rgba(255,82,82,0.6);   background: rgba(255,82,82,0.04); }
  &.anom-elevated { border-left-color: rgba(255,184,77,0.6);  background: rgba(255,184,77,0.04); }
}
.pa-name { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pa-dept { font-size: 11px; color: $dim; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pa-val  {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace;
  .anom-high     & { color: #ff5252; }
  .anom-elevated & { color: #FFB84D; }
}
.pa-type {
  font-size: 11px; padding: 1px 5px; border-radius: 3px; text-align: center;
  .anom-high     & { color: #ff5252; background: rgba(255,82,82,0.12);   border: 1px solid rgba(255,82,82,0.25);   }
  .anom-elevated & { color: #FFB84D; background: rgba(255,184,77,0.12);  border: 1px solid rgba(255,184,77,0.25);  }
}
.pa-time { font-size: 10px; color: $dim; }

// ── 压力分布统计面板 ──
.ps-panel-dist-stat { flex-shrink: 0; height: 140px; }
.ps-ds-total {
  margin-left: auto; font-size: 12px; color: $dim;
  em { color: #93c5fd; font-style: normal; font-weight: 700; }
}
.ps-ds-body {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 8px; padding: 6px 0 8px;
}
.ps-ds-zone {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px; padding: 10px 8px 8px;
  text-align: center; transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.07); }
  &.zone-relaxed  { border-color: rgba(79,195,247,0.2);  }
  &.zone-normal   { border-color: rgba(82,196,26,0.2);   }
  &.zone-elevated { border-color: rgba(255,184,77,0.2);  }
  &.zone-high     { border-color: rgba(255,82,82,0.2);   }
}
.ps-ds-icon  { font-size: 16px; margin-bottom: 4px; }
.ps-ds-count { font-size: 24px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1; }
.ps-ds-pct   { font-size: 11px; margin-top: 1px; }
.ps-ds-label { font-size: 13px; font-weight: 600; color: $white; margin-top: 4px; }
.ps-ds-range { font-size: 10px; color: $dim; margin-top: 2px; }
.ps-ds-bar-row {
  display: flex; height: 6px; border-radius: 3px; overflow: hidden;
  background: rgba(255,255,255,0.05); margin-bottom: 2px;
}
.ps-ds-seg { transition: width 0.4s ease; min-width: 0; }
</style>
