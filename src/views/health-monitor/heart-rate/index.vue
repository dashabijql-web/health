<template>
  <div class="hr-root">

    <!-- ══ 顶部 Header ══ -->
    <header class="hr-hd">
      <div class="hr-hd-left">
        <span class="hr-live-dot"></span>
        <h1 class="hr-hd-title">心率分析</h1>
      </div>

      <div class="hr-hd-kpis">
        <div class="hr-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="hr-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="hr-kpi-l">{{ k.label }}</span>
        </div>
      </div>

      <div class="hr-period-tabs">
        <span v-for="p in periodOptions" :key="p.value"
          :class="['hr-period-tab', activePeriod === p.value ? 'is-active' : '']"
          @click="switchPeriod(p.value)">{{ p.label }}</span>
      </div>

      <div class="hr-hd-time">{{ currentTime }}</div>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="hr-bd">

      <!-- ─ 左侧：TOP5(小) + 部门统计(大) ─ -->
      <aside class="hr-aside">
        <!-- TOP5：紧凑列表替代大图表 -->
        <div class="hr-panel hr-aside-top">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">异常频次 TOP5</span>
          </div>
          <div class="hr-top5-list">
            <div v-if="!top5Data.length" class="hr-top5-empty">暂无异常频次数据</div>
            <div class="hr-top5-row" v-for="(item, i) in top5Data" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="hr-top5-rank" :class="'rank-'+(i+1)">{{ i+1 }}</span>
              <span class="hr-top5-name">{{ item.userName }}</span>
              <div class="hr-top5-bar-wrap">
                <div class="hr-top5-bar" :style="{width: (item.count / top5Max * 100) + '%'}"></div>
              </div>
              <span class="hr-top5-val">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <!-- 部门统计：占剩余全部空间 -->
        <div class="hr-panel hr-aside-bot">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">部门心率异常统计</span>
            <div class="hr-ph-legend">
              <span class="hr-leg-dot" style="background:#4FC3F7"></span><span class="hr-leg-txt">偏低</span>
              <span class="hr-leg-dot" style="background:#FFB84D"></span><span class="hr-leg-txt">偏高</span>
            </div>
          </div>
          <div class="hr-pc">
            <div ref="deptRef" style="width:100%;height:100%"></div>
          </div>
        </div>
      </aside>

      <!-- ─ 中间：概况(紧凑) + 年龄/分布(中) + 趋势(大) ─ -->
      <main class="hr-main">

        <!-- 概况：单行水平布局，8个指标卡 + 仪表盘 -->
        <div class="hr-panel hr-overview-panel">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">{{ overviewTitle }}</span>
          </div>
          <div class="hr-overview-body">
            <!-- 仪表盘 -->
            <div class="hr-gauge-wrap">
              <div ref="gaugeRef" class="hr-gauge-chart"></div>
              <div class="hr-gauge-center">
                <div class="hr-gauge-val">{{ overview.avgHeartRate || '--' }}</div>
                <div class="hr-gauge-sub">bpm · 平均</div>
              </div>
            </div>
            <!-- 所有指标小卡片 -->
            <div class="hr-kpi-cards">
              <div class="hr-kpi-card" v-for="c in ovAllCards" :key="c.label">
                <div class="hr-kpi-card-val" :style="{color: c.color}">{{ c.val }}<span class="hr-kpi-card-unit">{{ c.unit }}</span></div>
                <div class="hr-kpi-card-label">{{ c.label }}</div>
              </div>
            </div>
            <!-- 心率区间说明 -->
            <div class="hr-range-info">
              <div class="hr-range-title">心率健康区间</div>
              <div class="hr-range-item" v-for="r in hrRanges" :key="r.label">
                <span class="hr-range-dot" :style="{background: r.color}"></span>
                <span class="hr-range-name" :style="{color: r.color}">{{ r.label }}</span>
                <span class="hr-range-val">{{ r.range }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 年龄段 + 每小时波动 + 分布（三列）-->
        <div class="hr-mid-row">
          <div class="hr-panel hr-panel-age">
            <div class="hr-ph">
              <span class="hr-ph-bar"></span>
              <span class="hr-ph-title">各年龄段平均心率</span>
            </div>
            <div class="hr-pc">
              <div ref="ageRef" style="width:100%;height:100%"></div>
            </div>
          </div>

          <div class="hr-panel hr-panel-hourly">
            <div class="hr-ph">
              <span class="hr-ph-bar"></span>
              <span class="hr-ph-title">{{ hourlyTitle }}</span>
            </div>
            <div class="hr-pc">
              <div ref="hourlyRef" style="width:100%;height:100%"></div>
            </div>
          </div>

          <div class="hr-panel hr-panel-dist">
            <div class="hr-ph">
              <span class="hr-ph-bar"></span>
              <span class="hr-ph-title">心率区间分布</span>
            </div>
            <div class="hr-dist-body">
              <div ref="distRef" class="hr-dist-chart"></div>
              <div class="hr-dist-legend">
                <div class="hr-dist-row" v-for="d in distLegend" :key="d.name">
                  <div class="hr-dist-dot" :style="{background: d.color}"></div>
                  <span class="hr-dist-name">{{ d.name }}</span>
                  <div class="hr-dist-bar-wrap">
                    <div class="hr-dist-bar" :style="{width: d.value + '%', background: d.color}"></div>
                  </div>
                  <span class="hr-dist-pct" :style="{color: d.color}">{{ d.value }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 趋势：固定高度 -->
        <div class="hr-panel hr-panel-trend">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">{{ trendTitle }}</span>
            <div class="hr-trend-tags">
              <span class="hr-tag" style="color:#00d4ff;border-color:rgba(0,212,255,0.3)">── 平均心率</span>
              <span class="hr-tag" style="color:#FFB84D;border-color:rgba(255,184,77,0.3)">- - 偏高(120)</span>
              <span class="hr-tag" style="color:#4FC3F7;border-color:rgba(79,195,247,0.3)">- - 偏低(55)</span>
            </div>
          </div>
          <div class="hr-pc">
            <div ref="trendRef" style="width:100%;height:100%"></div>
          </div>
        </div>

        <!-- 心率区间分布统计 -->
        <div class="hr-panel hr-panel-dist-stat">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">当前在线人员心率分布</span>
            <span class="hr-ds-total">共 <em>{{ realtimeList.length }}</em> 人在线</span>
          </div>
          <div class="hr-ds-body">
            <div class="hr-ds-zone" :class="z.cls" v-for="z in hrZones" :key="z.key">
              <div class="hr-ds-icon" :style="{color: z.color}">{{ z.icon }}</div>
              <div class="hr-ds-count" :style="{color: z.color}">{{ z.count }}</div>
              <div class="hr-ds-pct" :style="{color: z.color}">{{ z.pct }}%</div>
              <div class="hr-ds-label">{{ z.label }}</div>
              <div class="hr-ds-range">{{ z.range }}</div>
            </div>
          </div>
          <div class="hr-ds-bar-row">
            <div class="hr-ds-seg" v-for="z in hrZones" :key="z.key"
              :style="{width: z.pct + '%', background: z.color}"
              :title="z.label + ': ' + z.count + '人'"></div>
          </div>
        </div>

        <!-- 当前异常心率明细：高度跟内容走，不拉伸 -->
        <div class="hr-panel hr-panel-anomaly">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">当前异常心率明细</span>
            <span class="hr-anomaly-count" v-if="anomalyList.length">
              共 <em>{{ anomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!anomalyList.length" class="hr-anomaly-empty">
            <span class="hr-anomaly-ok">✓</span> 当前无异常心率人员
          </div>
          <div v-else class="hr-anomaly-body">
            <div class="hr-anomaly-hd">
              <span>姓名</span><span>部门</span><span>心率</span><span>类型</span><span>时间</span>
            </div>
            <div class="hr-anomaly-list">
              <div
                class="hr-anomaly-row"
                v-for="(item, i) in anomalyList"
                :key="i"
                :class="item.heartRate > 120 ? 'anom-high' : 'anom-low'"
                @click="showDetail(item)"
                style="cursor:pointer"
              >
                <span class="ha-name">{{ item.userName }}</span>
                <span class="ha-dept">{{ item.deptName || item.dept_name || '--' }}</span>
                <span class="ha-val">{{ item.heartRate }} bpm</span>
                <span class="ha-type">{{ item.heartRate > 120 ? '偏高↑' : '偏低↓' }}</span>
                <span class="ha-time">{{ fmtTime(item.recordTime) }}</span>
              </div>
            </div>
          </div>
        </div>

      </main>

      <!-- ─ 右侧：实时列表 ─ -->
      <div class="hr-rtlist">
        <div class="hr-panel hm-panel-flex">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">实时心率数据</span>
            <span class="hr-rt-total">{{ realtimeList.length }} 条</span>
          </div>

          <div class="hr-rt-hd">
            <span>姓名</span><span>心率</span><span>状态</span><span>时间</span>
          </div>

          <div class="hr-rt-body" ref="listRef">
            <div
              class="hr-rt-row"
              v-for="(item, i) in pagedList"
              :key="i"
              :class="hrLevel(item.heartRate)"
              @click="showDetail(item)"
              style="cursor:pointer"
            >
              <span class="hr-rt-name">{{ item.userName }}</span>
              <span class="hr-rt-val">
                {{ item.heartRate }}
                <em v-if="item.heartRate > 120" class="hr-rt-arrow">↑</em>
                <em v-else-if="item.heartRate < 55" class="hr-rt-arrow">↓</em>
              </span>
              <span class="hr-rt-badge" :class="hrLevel(item.heartRate)">
                {{ item.heartRate > 120 ? '偏高' : item.heartRate < 55 ? '偏低' : '正常' }}
              </span>
              <span class="hr-rt-time">{{ fmtTime(item.recordTime) }}</span>
            </div>
          </div>

          <div class="hr-rt-pg">
            <button class="hr-pg-btn" :disabled="currentPage===1" @click="currentPage=1">首页</button>
            <button class="hr-pg-btn" :disabled="currentPage===1" @click="currentPage--">‹</button>
            <span class="hr-pg-info">{{ currentPage }} / {{ totalPages }}</span>
            <button class="hr-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage++">›</button>
            <button class="hr-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage=totalPages">末页</button>
          </div>
        </div>
      </div>

    </section>

    <el-dialog v-model="detailVisible" :title="`${detailItem?.userName || ''} 心率详情`" width="400px" :append-to-body="true">
      <div v-if="detailItem" style="padding:8px 0">
        <div style="text-align:center;margin-bottom:20px">
          <span class="hm-detail-value">{{ detailItem.heartRate }}</span>
          <span class="hm-detail-unit">bpm</span>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">
            <el-tag :type="detailItem.heartRate > 120 ? 'warning' : detailItem.heartRate < 55 ? 'info' : 'success'" size="small" effect="dark">
              {{ detailItem.heartRate > 120 ? '偏高' : detailItem.heartRate < 55 ? '偏低' : '正常' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="记录时间">{{ fmtTime(detailItem.recordTime) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px;text-align:right">
          <el-button type="primary" size="small" @click="goToPortrait(detailItem);detailVisible=false">查看健康画像</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import {
  getHeartRateOverview,
  getHeartRateTrend,
  getHeartRateDistribution,
  getAgeHeartRate,
  getRealtimeHeartRate,
  getHeartRateTopUsers,
  getHeartRateDeptStats,
  getHourlyHeartRate
} from '@/api/heart-rate'
import { hrLevel, HR } from '@/constants/health-thresholds'
import chartPageMixin from '@/mixins/chartPage'
import { PERIOD_OPTIONS } from '@/constants/periods'
import { emptyOption, chartTooltip, categoryAxis, valueAxis, deptGrid, trendGrid, hourlyGrid, ageGrid, barLabel } from '@/utils/echarts-config'

export default {
  name: 'HeartRateAnalysis',
  mixins: [chartPageMixin],
  data() {
    return {
      currentTime: '',
      overview: {
        avgHeartRate: 0, minHeartRate: 0, maxHeartRate: 0,
        detectionRate: 0, abnormalCount: 0, totalCount: 0
      },
      distLegend: [],
      top5Data: [],
      hrRanges: [
        { label: '偏低 (心动过缓)', range: '< 55 次/分',     color: '#4FC3F7' },
        { label: '正常 (健康范围)', range: '55–120 次/分',   color: '#52c41a' },
        { label: '偏高 (心动过速)', range: '> 120 次/分',    color: '#FFB84D' },
        { label: '危险 (需立即处理)', range: '> 150 次/分',  color: '#ff5252' }
      ],
      realtimeList: [],
      currentPage: 1,
      pageSize: 20,
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      charts: {},
      detailItem: null,
      detailVisible: false,
      filterDept: ''
    }
  },
  computed: {
    headerKpis() {
      const o = this.overview
      return [
        { label: '平均心率',   val: (o.avgHeartRate || '--') + ' bpm', cls: 'kpi-cyan'  },
        { label: '异常人次',   val: o.abnormalCount  || 0,             cls: 'kpi-orange' },
        { label: '检测率',     val: (o.detectionRate || 0) + '%',       cls: 'kpi-green'  },
        { label: '总记录数',   val: (o.totalCount    || 0).toLocaleString(), cls: 'kpi-blue' }
      ]
    },
    ovAllCards() {
      const o = this.overview
      return [
        { label: '最低心率',   val: o.minHeartRate   || '--', unit: ' bpm', color: '#4FC3F7' },
        { label: '最高心率',   val: o.maxHeartRate   || '--', unit: ' bpm', color: '#FFB84D' },
        { label: '心率检测率', val: o.detectionRate  || '--', unit: '%',    color: '#52c41a' },
        { label: '异常记录',   val: o.abnormalCount  || '--', unit: ' 人',  color: '#ff5252' },
        { label: '总记录数',   val: (o.totalCount || 0).toLocaleString(), unit: ' 条', color: '#7eb8f7' },
        { label: '心率范围',   val: o.minHeartRate && o.maxHeartRate ? `${o.minHeartRate}~${o.maxHeartRate}` : '--', unit: '', color: '#a78bfa' }
      ]
    },
    overviewTitle() {
      return { day: '今日心率概况', week: '近7日心率概况', month: '近30日心率概况' }[this.activePeriod]
    },
    hourlyTitle() {
      return { day: '今日24小时波动', week: '近7日每日均值', month: '近30日每日均值' }[this.activePeriod]
    },
    trendTitle() {
      return { day: '今日心率趋势', week: '近7天心率趋势', month: '近30天心率趋势' }[this.activePeriod]
    },
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.count)) : 1
    },
    pagedList() {
      let list = this.realtimeList
      if (this.filterDept) {
        list = list.filter(x => (x.deptName || x.dept_name) === this.filterDept)
      }
      const s = (this.currentPage - 1) * this.pageSize
      return list.slice(s, s + this.pageSize)
    },
    totalPages() {
      let list = this.realtimeList
      if (this.filterDept) list = list.filter(x => (x.deptName || x.dept_name) === this.filterDept)
      return Math.max(1, Math.ceil(list.length / this.pageSize))
    },
    anomalyList() {
      return this.realtimeList.filter(x => x.heartRate > 120 || x.heartRate < 55)
    },
    anomalyPanelH() {
      const PH = 36, HD = 26, ROW = 27, PAD = 10
      if (!this.anomalyList.length) return PH + 46
      return PH + HD + Math.min(this.anomalyList.length, 12) * ROW + PAD
    },
    hrZones() {
      const list = this.realtimeList
      const total = list.length || 1
      const low      = list.filter(x => x.heartRate < 55).length
      const danger   = list.filter(x => x.heartRate > 150).length
      const elevated = list.filter(x => x.heartRate > 120 && x.heartRate <= 150).length
      const normal   = total - low - danger - elevated
      const pct = n => list.length > 0 ? Math.round(n / total * 100) : 0
      return [
        { key: 'low',      label: '偏低', range: '< 55 bpm',     count: low,      pct: pct(low),      color: '#4FC3F7', icon: '↓', cls: 'zone-low'      },
        { key: 'normal',   label: '正常', range: '55–120 bpm',   count: normal,   pct: pct(normal),   color: '#52c41a', icon: '✓', cls: 'zone-normal'   },
        { key: 'elevated', label: '偏高', range: '121–150 bpm',  count: elevated, pct: pct(elevated), color: '#FFB84D', icon: '↑', cls: 'zone-elevated' },
        { key: 'danger',   label: '危险', range: '> 150 bpm',    count: danger,   pct: pct(danger),   color: '#ff5252', icon: '⚠', cls: 'zone-danger'   }
      ]
    }
  },
  mounted() { this.initPage() },
  methods: {

    async fetchData() {
      await Promise.allSettled([
        this.loadOverview(),
        this.loadTopUsers(),
        this.loadDept(),
        this.loadAge(),
        this.loadDist(),
        this.loadTrend(),
        this.loadHourly(),
        this.loadRealtime()
      ])
    },

    async loadOverview() {
      const { startDate, endDate } = this.periodRange
      try { const r = await getHeartRateOverview(startDate, endDate); if (r.code === 200) this.overview = r.data } catch {}
      this.$nextTick(() => this.initGauge())
    },
    async loadTopUsers() {
      const { startDate, endDate } = this.periodRange
      let d = []; try { const r = await getHeartRateTopUsers(5, startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
      this.top5Data = d
    },
    async loadDept() {
      const { startDate, endDate } = this.periodRange
      let d = []; try { const r = await getHeartRateDeptStats(startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
      this.$nextTick(() => this.initDept(d))
    },
    async loadAge() {
      let d = []; try { const r = await getAgeHeartRate(); if (r.code === 200) d = r.data || [] } catch {}
      this.$nextTick(() => this.initAge(d))
    },
    async loadDist() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getHeartRateDistribution(startDate, endDate)
        if (r.code === 200) {
          d = (r.data || []).filter(x => x.name && x.value > 0)
          this.distLegend = d
        }
      } catch {}
      this.$nextTick(() => this.initDist(d))
    },
    async loadHourly() {
      if (this.activePeriod === 'day') {
        const today = new Date().toISOString().slice(0, 10)
        const vals = new Array(24).fill(null)
        try {
          const r = await getHourlyHeartRate(today, today)
          if (r.code === 200 && Array.isArray(r.data)) {
            r.data.forEach(({ hour, avgHeartRate }) => {
              if (hour >= 0 && hour < 24) vals[hour] = avgHeartRate
            })
          }
        } catch {}
        this.$nextTick(() => this.renderHourly(vals))
      } else {
        const days = this.activePeriod === 'week' ? 7 : 30
        let dates = [], vals = []
        try {
          const r = await getHeartRateTrend(days)
          if (r.code === 200 && r.data) {
            dates = r.data.dates || []
            vals  = r.data.values || []
          }
        } catch {}
        this.$nextTick(() => this.renderHourlyDaily(dates, vals))
      }
    },
    async loadTrend() {
      if (this.activePeriod === 'day') {
        const today = new Date().toISOString().slice(0, 10)
        const vals = new Array(24).fill(null)
        try {
          const r = await getHourlyHeartRate(today, today)
          if (r.code === 200 && Array.isArray(r.data)) {
            r.data.forEach(({ hour, avgHeartRate }) => {
              if (hour >= 0 && hour < 24) vals[hour] = avgHeartRate
            })
          }
        } catch {}
        this.$nextTick(() => this.initTrendDay(vals))
      } else {
        const days = this.activePeriod === 'week' ? 7 : 30
        let d = {}
        try { const r = await getHeartRateTrend(days); if (r.code === 200) d = r.data || {} } catch {}
        this.$nextTick(() => this.initTrend(d))
      }
    },
    async loadRealtime() {
      try { const r = await getRealtimeHeartRate(200); if (r.code === 200) this.realtimeList = r.data || [] } catch {}
    },

    // ── ECharts 初始化 ──
    initGauge() {
      const el = this.$refs.gaugeRef; if (!el) return
      if (this.charts.gauge) this.charts.gauge.dispose()
      const c = echarts.init(el); this.charts.gauge = c
      const v = this.overview.avgHeartRate || 0
      c.setOption({
        series: [{
          type: 'gauge', startAngle: 225, endAngle: -45,
          radius: '90%', center: ['50%', '58%'],
          min: 0, max: 160,
          axisLine: { lineStyle: { width: 16, color: [[0.34,'#4FC3F7'],[0.75,'#52c41a'],[1,'#FFB84D']] } },
          pointer: { length: '60%', width: 6, itemStyle: { color: '#00d4ff', shadowBlur: 14, shadowColor: 'rgba(0,212,255,0.8)' } },
          axisTick: { length: 5, distance: -22, lineStyle: { color: 'rgba(0,212,255,0.25)', width: 1 } },
          splitLine: { length: 10, distance: -22, lineStyle: { color: 'rgba(0,212,255,0.45)', width: 2 } },
          axisLabel: { color: '#8ba6c8', fontSize: 10, distance: -28 },
          detail: { show: false },
          data: [{ value: v }]
        }]
      })
    },

    initDept(data) {
      const el = this.$refs.deptRef; if (!el) return
      if (this.charts.dept) this.charts.dept.dispose()
      const c = echarts.init(el); this.charts.dept = c
      if (!data.length) { c.setOption(emptyOption()); return }
      const d = data.map(x => ({
        deptName:  x.deptName  || x.name,
        lowCount:  x.lowCount  || x.lowHeartRateCount  || 0,
        highCount: x.highCount || x.highHeartRateCount || x.abnormalCount || 0
      }))
      c.setOption({
        backgroundColor: 'transparent',
        legend: { data: ['偏低','偏高'], right: 10, top: 6, textStyle: { color: '#8ba6c8', fontSize: 11 }, itemWidth: 10, itemHeight: 10, icon: 'rect' },
        grid: deptGrid(),
        xAxis: valueAxis(),
        yAxis: { ...categoryAxis(d.map(x => x.deptName), { show: false }), inverse: true },
        series: [
          { name:'偏低', type:'bar', stack:'total', barWidth:'46%', data: d.map(x => x.lowCount),
            itemStyle: { color: new echarts.graphic.LinearGradient(1,0,0,0,[{offset:0,color:'#4FC3F7'},{offset:1,color:'#29B6F6'}]) },
            label: barLabel()
          },
          { name:'偏高', type:'bar', stack:'total', barWidth:'46%', data: d.map(x => x.highCount),
            itemStyle: { color: new echarts.graphic.LinearGradient(1,0,0,0,[{offset:0,color:'#FFB84D'},{offset:1,color:'#FFA726'}]), borderRadius:[0,4,4,0] },
            label: barLabel()
          }
        ]
      })
      c.off('click')
      c.on('click', (params) => {
        this.filterDept = this.filterDept === params.name ? '' : params.name
        this.currentPage = 1
      })
    },

    initAge(data) {
      const el = this.$refs.ageRef; if (!el) return
      if (this.charts.age) this.charts.age.dispose()
      const c = echarts.init(el); this.charts.age = c
      const fb = [
        { ageRange: '20-30', avgHeartRate: 75 }, { ageRange: '30-40', avgHeartRate: 78 },
        { ageRange: '40-50', avgHeartRate: 80 }, { ageRange: '50+',   avgHeartRate: 82 }
      ]
      const d = data.length ? data : fb
      c.setOption({
        backgroundColor: 'transparent',
        grid: ageGrid(),
        xAxis: categoryAxis(d.map(x => x.ageRange)),
        yAxis: valueAxis({ name: 'bpm', min: v => Math.max(0, v.min - 5), max: v => v.max + 5 }),
        series: [{
          type: 'bar', data: d.map(x => x.avgHeartRate), barWidth: '46%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0,0,0,1,
              [{ offset: 0, color: '#00d4ff' }, { offset: 1, color: 'rgba(0,100,220,0.35)' }]),
            borderRadius: [6, 6, 0, 0]
          },
          label: { show: true, position: 'top', color: '#00d4ff', fontSize: 11, fontWeight: 'bold' }
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
          data: data.map(x => ({
            value: x.value, name: x.name,
            itemStyle: { color: x.color, borderRadius: 4, shadowColor: x.color + '66', shadowBlur: 10 }
          }))
        }]
      })
    },

    initTrend(data) {
      const el = this.$refs.trendRef; if (!el) return
      if (this.charts.trend) this.charts.trend.dispose()
      const c = echarts.init(el); this.charts.trend = c
      const fbDates = Array.from({ length: 30 }, (_, i) => dayjs().subtract(29 - i, 'day').format('MM/DD'))
      const dates = data.dates  || fbDates
      const vals  = data.values || new Array(dates.length).fill(0)
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => `${p[0].name}<br/>平均心率：<b style="color:#00d4ff">${p[0].value}</b> 次/分`),
        grid: trendGrid(),
        xAxis: { ...categoryAxis(dates, { fontSize: 10, interval: 4 }), boundaryGap: false },
        yAxis: valueAxis({ min: v => Math.max(0, v.min - 3), max: v => v.max + 3 }),
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none',
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
            [{ offset: 0, color: 'rgba(0,212,255,0.28)' }, { offset: 1, color: 'rgba(0,212,255,0.02)' }]) },
          markPoint: {
            symbol: 'circle', symbolSize: 6,
            label: { fontSize: 10, fontWeight: 'bold', fontFamily: 'Consolas', offset: [0, -14] },
            data: [
              { type: 'max', name: '最高', itemStyle: { color: '#FFB84D' }, label: { color: '#FFB84D', formatter: p => '▲' + p.value } },
              { type: 'min', name: '最低', itemStyle: { color: '#4FC3F7' }, label: { color: '#4FC3F7', formatter: p => '▼' + p.value } }
            ]
          },
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: HR.HIGH, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '偏高 ' + HR.HIGH } },
              { yAxis: HR.LOW,  lineStyle: { color: '#4FC3F7', type: 'dashed', width: 1 }, label: { color: '#4FC3F7', fontSize: 10, formatter: '偏低 ' + HR.LOW } }
            ]
          }
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
            ? `${p[0].name}<br/>心率：<b style="color:#00d4ff">${p[0].value}</b> bpm`
            : `${p[0].name}<br/>暂无数据`),
        grid: hourlyGrid(),
        xAxis: { ...categoryAxis(hours, { fontSize: 9, interval: 3, lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: false },
        yAxis: valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)', min: v => v.min > 0 ? v.min - 4 : 50, max: v => v.max > 0 ? v.max + 4 : 120 }),
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#a78bfa', width: 1.5 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
            [{ offset: 0, color: 'rgba(167,139,250,0.22)' }, { offset: 1, color: 'rgba(167,139,250,0.02)' }]) }
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
        tooltip: chartTooltip(p => `${p[0].name}<br/>心率：<b style="color:#a78bfa">${p[0].value}</b> bpm`),
        grid: hourlyGrid(),
        xAxis: { ...categoryAxis(dates, { fontSize: 9, interval: Math.floor(dates.length / 5), lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: true },
        yAxis: valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)', min: v => v.min > 0 ? v.min - 4 : 50, max: v => v.max > 0 ? v.max + 4 : 120 }),
        series: [{
          type: 'bar', data: vals, barMaxWidth: 14,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1,
              [{ offset: 0, color: '#a78bfa' }, { offset: 1, color: 'rgba(167,139,250,0.2)' }]),
            borderRadius: [3, 3, 0, 0]
          }
        }]
      })
    },

    initTrendDay(vals) {
      const el = this.$refs.trendRef; if (!el) return
      if (this.charts.trend) this.charts.trend.dispose()
      const c = echarts.init(el); this.charts.trend = c
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => p[0].value != null
            ? `${p[0].name}<br/>心率：<b style="color:#00d4ff">${p[0].value}</b> bpm`
            : `${p[0].name}<br/>暂无数据`),
        grid: trendGrid(),
        xAxis: { ...categoryAxis(hours, { fontSize: 10, interval: 3 }), boundaryGap: false },
        yAxis: valueAxis({ min: v => v.min > 0 ? v.min - 4 : 50, max: v => v.max > 0 ? v.max + 4 : 120 }),
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
            [{ offset: 0, color: 'rgba(0,212,255,0.28)' }, { offset: 1, color: 'rgba(0,212,255,0.02)' }]) },
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: HR.HIGH, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '偏高 ' + HR.HIGH } },
              { yAxis: HR.LOW,  lineStyle: { color: '#4FC3F7', type: 'dashed', width: 1 }, label: { color: '#4FC3F7', fontSize: 10, formatter: '偏低 ' + HR.LOW } }
            ]
          }
        }]
      })
    },

    hrLevel,

    // setPageSize → chartPageMixin
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/hm-vars';
@import '@/styles/hm-layout';

@include hm-body('hr');
@include hm-main('hr');
@include hm-panel('hr');
@include hm-overview('hr');
@include hm-kpi-cards('hr');
@include hm-range-info('hr');
@include hm-pagination('hr');

// ── Root：自适应视口高度 ──
.hr-root {
  width: 100%;
  height: calc(100vh - 50px) !important; /* 视口高度 - 顶部导航栏 */
  min-height: 600px; /* 最小高度防止过小 */
  background: $bg;
  background-image:
    radial-gradient(circle at 18% 28%, rgba(0,212,255,0.06) 0%, transparent 48%),
    radial-gradient(circle at 82% 72%, rgba(42,82,152,0.08) 0%, transparent 48%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
  color: $text;
}

// ── Header ──
.hr-hd {
  height: 58px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 22px;
  gap: 20px;
  background: rgba(0, 6, 24, 0.65);
  border-bottom: 1px solid $border;
}
.hr-hd-left { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

.hr-live-dot {
  width: 9px; height: 9px;
  border-radius: 50%;
  background: $accent;
  box-shadow: 0 0 8px $accent;
  animation: hmPulse 2s ease-in-out infinite;
}

.hr-hd-title {
  font-size: 20px; font-weight: 700; color: $white; margin: 0;
  letter-spacing: 2px; text-shadow: 0 0 14px rgba(0,212,255,0.45);
}

.hr-hd-kpis {
  flex: 1; display: flex; justify-content: center;
}
.hr-kpi {
  display: flex; flex-direction: column; align-items: center;
  padding: 0 32px;
  border-right: 1px solid $border;
  &:first-child { border-left: 1px solid $border; }
}
.hr-kpi-n {
  font-size: 20px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1;
  &.kpi-cyan   { color: $accent; text-shadow: 0 0 10px rgba(0,212,255,0.5); }
  &.kpi-orange { color: #FFB84D; text-shadow: 0 0 10px rgba(255,184,77,0.4); }
  &.kpi-green  { color: #52c41a; text-shadow: 0 0 10px rgba(82,196,26,0.35); }
  &.kpi-blue   { color: #7eb8f7; }
}
.hr-kpi-l { font-size: 11px; color: $dim; margin-top: 2px; white-space: nowrap; }
.hr-hd-time { flex-shrink: 0; font-family: 'Consolas', monospace; font-size: 13px; color: $dim; }

// ── Body（hm-body mixin） ──

// ── Aside（左侧：TOP5紧凑列表 + 部门图）──
.hr-aside {
  width: 300px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 10px;
}
.hr-aside-top { height: 190px; flex-shrink: 0; }
.hr-aside-bot { flex: 1; }

// TOP5 紧凑列表
.hr-top5-empty { padding: 20px 0; text-align: center; color: rgba(126,184,247,0.5); font-size: 12px; }
.hr-top5-list {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hr-top5-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hr-top5-rank {
  width: 18px; height: 18px;
  border-radius: 4px;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  &.rank-1 { background: rgba(255,184,77,0.2); color: #FFB84D; border: 1px solid rgba(255,184,77,0.4); }
  &.rank-2 { background: rgba(0,212,255,0.12); color: #00d4ff; border: 1px solid rgba(0,212,255,0.3); }
  &.rank-3 { background: rgba(82,196,26,0.12); color: #52c41a; border: 1px solid rgba(82,196,26,0.3); }
  &.rank-4, &.rank-5 { background: rgba(168,196,230,0.08); color: #8ba6c8; border: 1px solid rgba(168,196,230,0.2); }
}
.hr-top5-name { font-size: 12px; color: $white; width: 64px; flex-shrink: 0; }
.hr-top5-bar-wrap { flex: 1; height: 6px; background: rgba(0,212,255,0.08); border-radius: 3px; overflow: hidden; }
.hr-top5-bar { height: 100%; border-radius: 3px; background: linear-gradient(90deg, #00d4ff, #0066cc); transition: width 0.8s ease; }
.hr-top5-val { font-size: 13px; font-weight: 700; color: #00d4ff; font-family: 'Consolas', monospace; width: 22px; text-align: right; flex-shrink: 0; }

// panel header legend → hm-panel mixin

// ── Main（hm-main mixin） ──
.hr-overview-panel { height: 162px; flex-shrink: 0; }
.hr-mid-row        { height: 190px; flex-shrink: 0; display: flex; gap: 10px; }
.hr-panel-age      { flex: 0 0 340px; }
.hr-panel-hourly   { flex: 1; }
.hr-panel-dist     { flex: 0 0 258px; }
.hr-panel-trend    { flex: 1; min-height: 160px; max-height: 300px; }

// ── Right list ──
.hr-rtlist { width: 272px; flex-shrink: 0; }

// ── Panel（hm-panel mixin + 页面特有） ──
.hr-ph {
  height: 38px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  border-bottom: 1px solid rgba(0,212,255,0.09);
  background: rgba(0,212,255,0.035);
}
.hr-ph-bar {
  width: 3px; height: 14px;
  background: linear-gradient(180deg, $accent, rgba(0,212,255,0.3));
  border-radius: 2px;
  box-shadow: 0 0 6px rgba(0,212,255,0.7);
}
.hr-period-tabs {
  display: flex;
  background: rgba(0,212,255,0.06);
  border: 1px solid rgba(0,212,255,0.2);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.hr-period-tab {
  padding: 4px 14px;
  font-size: 12px;
  color: $dim;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: $white; background: rgba(0,212,255,0.1); }
  &.is-active { color: $bg; background: $accent; font-weight: 700; }
}
// overview/kpi-cards/range-info → hm-overview + hm-kpi-cards + hm-range-info mixins
.hr-range-name { width: 78px; } // override mixin default 38px

// ── 分布图 ──
.hr-dist-body { flex: 1; min-height: 0; display: flex; align-items: center; gap: 10px; padding: 8px 12px; }
.hr-dist-chart { width: 120px; height: 120px; flex-shrink: 0; }
.hr-dist-legend { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.hr-dist-row { display: flex; align-items: center; gap: 7px; }
.hr-dist-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.hr-dist-name { font-size: 11px; color: $text; flex-shrink: 0; width: 56px; }
.hr-dist-bar-wrap { flex: 1; height: 5px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
.hr-dist-bar { height: 100%; border-radius: 3px; transition: width 0.8s ease; opacity: 0.85; }
.hr-dist-pct { font-size: 14px; font-weight: 700; font-family: 'Consolas', monospace; width: 34px; text-align: right; flex-shrink: 0; }

// ── 实时列表 ──
.hr-rt-hd {
  display: grid; grid-template-columns: 64px 46px 40px 1fr;
  gap: 6px; padding: 6px 10px; flex-shrink: 0;
  background: rgba(0,212,255,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.hr-rt-body {
  flex: 1; overflow-y: auto; padding: 3px 6px; min-height: 0;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.18); border-radius: 2px; }
}
.hr-rt-row {
  display: grid; grid-template-columns: 64px 46px 40px 1fr;
  gap: 6px; padding: 6px 4px; margin-bottom: 1px;
  border-radius: 5px; align-items: center;
  border-left: 2px solid transparent;
  transition: background 0.2s;
  &:hover { background: rgba(0,212,255,0.055); }
  &.normal { border-left-color: rgba(82,196,26,0.45); }
  &.high   { border-left-color: rgba(255,184,77,0.55); }
  &.low    { border-left-color: rgba(79,195,247,0.55); }
}
.hr-rt-name { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hr-rt-val {
  font-size: 14px; font-weight: 700; font-family: 'Consolas', monospace; color: #52c41a;
  .hr-rt-row.high & { color: #FFB84D; }
  .hr-rt-row.low  & { color: #4FC3F7; }
}
.hr-rt-arrow { font-style: normal; font-size: 10px; animation: blink 1.2s infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0.25} }
.hr-rt-badge {
  font-size: 10px; padding: 1px 4px; border-radius: 3px; text-align: center;
  &.normal { background: rgba(82,196,26,0.13); color: #52c41a; border: 1px solid rgba(82,196,26,0.28); }
  &.high   { background: rgba(255,184,77,0.13); color: #FFB84D; border: 1px solid rgba(255,184,77,0.28); }
  &.low    { background: rgba(79,195,247,0.13); color: #4FC3F7; border: 1px solid rgba(79,195,247,0.28); }
}
.hr-rt-time { font-size: 10px; color: $dim; }

// pagination → hm-pagination mixin
.hr-pg-info { font-size: 12px; color: $accent; min-width: 44px; text-align: center; } // override mixin

// ── 异常明细面板 ──
.hr-panel-anomaly { flex-shrink: 0; display: flex; flex-direction: column; overflow: hidden; }
.hr-anomaly-count {
  margin-left: auto; font-size: 12px; color: #FFB84D;
  em { font-style: normal; font-weight: 700; }
}
.hr-anomaly-empty {
  flex: 1; display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: rgba(82,196,26,0.8);
  .hr-anomaly-ok { font-size: 16px; margin-right: 6px; }
}
.hr-anomaly-body { flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.hr-anomaly-hd {
  display: grid; grid-template-columns: 64px 1fr 72px 52px 88px;
  gap: 6px; padding: 4px 10px; flex-shrink: 0;
  background: rgba(255,184,77,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.hr-anomaly-list {
  flex: 1; overflow-y: auto; padding: 3px 6px;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,184,77,0.2); border-radius: 2px; }
}
.hr-anomaly-row {
  display: grid; grid-template-columns: 64px 1fr 72px 52px 88px;
  gap: 6px; padding: 5px 4px; margin-bottom: 1px;
  border-radius: 4px; align-items: center;
  border-left: 2px solid transparent;
  transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.04); }
  &.anom-high { border-left-color: rgba(255,184,77,0.6); background: rgba(255,184,77,0.04); }
  &.anom-low  { border-left-color: rgba(79,195,247,0.6); background: rgba(79,195,247,0.04); }
}
.ha-name { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ha-dept { font-size: 11px; color: $dim; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ha-val  {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace;
  .anom-high & { color: #FFB84D; }
  .anom-low  & { color: #4FC3F7; }
}
.ha-type {
  font-size: 11px; padding: 1px 5px; border-radius: 3px; text-align: center;
  .anom-high & { color: #FFB84D; background: rgba(255,184,77,0.12); border: 1px solid rgba(255,184,77,0.25); }
  .anom-low  & { color: #4FC3F7; background: rgba(79,195,247,0.12); border: 1px solid rgba(79,195,247,0.25); }
}
.ha-time { font-size: 10px; color: $dim; }

// ── 心率分布统计面板 ──
.hr-panel-dist-stat { flex-shrink: 0; }
.hr-ds-total {
  margin-left: auto; font-size: 12px; color: $dim;
  em { color: #93c5fd; font-style: normal; font-weight: 700; }
}
.hr-ds-body {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 8px; padding: 6px 0 8px;
}
.hr-ds-zone {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px; padding: 10px 8px 8px;
  text-align: center; transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.07); }
  &.zone-low     { border-color: rgba(79,195,247,0.2);  }
  &.zone-normal  { border-color: rgba(82,196,26,0.2);   }
  &.zone-elevated{ border-color: rgba(255,184,77,0.2);  }
  &.zone-danger  { border-color: rgba(255,82,82,0.2);   }
}
.hr-ds-icon  { font-size: 16px; margin-bottom: 4px; }
.hr-ds-count { font-size: 24px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1; }
.hr-ds-pct   { font-size: 11px; margin-top: 1px; }
.hr-ds-label { font-size: 13px; font-weight: 600; color: $white; margin-top: 4px; }
.hr-ds-range { font-size: 10px; color: $dim; margin-top: 2px; }
.hr-ds-bar-row {
  display: flex; height: 6px; border-radius: 3px; overflow: hidden;
  background: rgba(255,255,255,0.05); margin-bottom: 2px;
}
.hr-ds-seg { transition: width 0.4s ease; min-width: 0; }
</style>