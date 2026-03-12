<template>
  <div class="bo-root">

    <!-- ══ Header ══ -->
    <header class="bo-hd">
      <div class="bo-hd-left">
        <span class="bo-live-dot"></span>
        <h1 class="bo-hd-title">血氧分析</h1>
      </div>
      <div class="bo-hd-kpis">
        <div class="bo-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="bo-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="bo-kpi-l">{{ k.label }}</span>
        </div>
      </div>
      <div class="bo-period-tabs">
        <span v-for="p in periodOptions" :key="p.value"
          :class="['bo-period-tab', activePeriod === p.value ? 'is-active' : '']"
          @click="switchPeriod(p.value)">{{ p.label }}</span>
      </div>

      <div class="bo-hd-time">{{ currentTime }}</div>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="bo-bd">

      <!-- ─ 左侧：TOP5 紧凑列表 + 部门统计 ─ -->
      <aside class="bo-aside">
        <div class="bo-panel bo-aside-top">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">异常频次 TOP5</span>
          </div>
          <div class="bo-top5-list">
            <div v-if="!top5Data.length" class="bo-top5-empty">暂无异常频次数据</div>
            <div class="bo-top5-row" v-for="(item, i) in top5Data" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="bo-top5-rank" :class="'rank-'+(i+1)">{{ i+1 }}</span>
              <span class="bo-top5-name">{{ item.userName }}</span>
              <div class="bo-top5-bar-wrap">
                <div class="bo-top5-bar" :style="{width: (item.count / top5Max * 100) + '%'}"></div>
              </div>
              <span class="bo-top5-val">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <div class="bo-panel bo-aside-bot">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">部门血氧异常统计</span>
            <div class="bo-ph-legend">
              <span class="bo-leg-dot" style="background:#FFB84D"></span><span class="bo-leg-txt">偏低</span>
              <span class="bo-leg-dot" style="background:#4FC3F7"></span><span class="bo-leg-txt">偏高</span>
            </div>
          </div>
          <div class="bo-pc">
            <div ref="deptRef" style="width:100%;height:100%"></div>
          </div>
        </div>
      </aside>

      <!-- ─ 中间 ─ -->
      <main class="bo-main">

        <!-- 概况：仪表盘 + KPI卡 + 血氧区间说明 -->
        <div class="bo-panel bo-overview-panel">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">{{ overviewTitle }}</span>
          </div>
          <div class="bo-overview-body">
            <div class="bo-gauge-wrap">
              <div ref="gaugeRef" class="bo-gauge-chart"></div>
              <div class="bo-gauge-center">
                <div class="bo-gauge-val">{{ overview.avgBloodOxygen || '--' }}</div>
                <div class="bo-gauge-sub">% · 平均血氧</div>
              </div>
            </div>
            <div class="bo-kpi-cards">
              <div class="bo-kpi-card" v-for="c in ovAllCards" :key="c.label">
                <div class="bo-kpi-card-val" :style="{color: c.color}">{{ c.val }}<span class="bo-kpi-card-unit">{{ c.unit }}</span></div>
                <div class="bo-kpi-card-label">{{ c.label }}</div>
              </div>
            </div>
            <div class="bo-range-info">
              <div class="bo-range-title">血氧健康区间</div>
              <div class="bo-range-item" v-for="r in boRanges" :key="r.label">
                <span class="bo-range-dot" :style="{background: r.color}"></span>
                <span class="bo-range-name" :style="{color: r.color}">{{ r.label }}</span>
                <span class="bo-range-val">{{ r.range }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 年龄段 + 今日24h波动 + 分布 -->
        <div class="bo-mid-row">
          <div class="bo-panel bo-panel-age">
            <div class="bo-ph">
              <span class="bo-ph-bar"></span>
              <span class="bo-ph-title">各年龄段平均血氧</span>
            </div>
            <div class="bo-pc">
              <div ref="ageRef" style="width:100%;height:100%"></div>
            </div>
          </div>

          <div class="bo-panel bo-panel-hourly">
            <div class="bo-ph">
              <span class="bo-ph-bar"></span>
              <span class="bo-ph-title">{{ hourlyTitle }}</span>
            </div>
            <div class="bo-pc">
              <div ref="hourlyRef" style="width:100%;height:100%"></div>
            </div>
          </div>

          <div class="bo-panel bo-panel-dist">
            <div class="bo-ph">
              <span class="bo-ph-bar"></span>
              <span class="bo-ph-title">血氧区间分布</span>
            </div>
            <div class="bo-dist-body">
              <div ref="distRef" class="bo-dist-chart"></div>
              <div class="bo-dist-legend">
                <div class="bo-dist-row" v-for="d in distLegend" :key="d.name">
                  <div class="bo-dist-dot" :style="{background: d.color}"></div>
                  <span class="bo-dist-name">{{ d.name }}</span>
                  <div class="bo-dist-bar-wrap">
                    <div class="bo-dist-bar" :style="{width: d.value + '%', background: d.color}"></div>
                  </div>
                  <span class="bo-dist-pct" :style="{color: d.color}">{{ d.value }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 趋势 -->
        <div class="bo-panel bo-panel-trend">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">{{ trendTitle }}</span>
            <div class="bo-trend-tags">
              <span class="bo-tag" style="color:#00d4ff;border-color:rgba(0,212,255,0.3)">── 平均血氧</span>
              <span class="bo-tag" style="color:#FFB84D;border-color:rgba(255,184,77,0.3)">- - 偏低预警(90%)</span>
              <span class="bo-tag" style="color:#4FC3F7;border-color:rgba(79,195,247,0.3)">- - 正常下限(95%)</span>
            </div>
          </div>
          <div class="bo-pc">
            <div ref="trendRef" style="width:100%;height:100%"></div>
          </div>
        </div>

        <!-- 血氧区间分布统计 -->
        <div class="bo-panel bo-panel-dist-stat">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">当前在线人员血氧分布</span>
            <span class="bo-ds-total">共 <em>{{ realtimeList.length }}</em> 人在线</span>
          </div>
          <div class="bo-ds-body">
            <div class="bo-ds-zone" :class="z.cls" v-for="z in boZones" :key="z.key">
              <div class="bo-ds-icon" :style="{color: z.color}">{{ z.icon }}</div>
              <div class="bo-ds-count" :style="{color: z.color}">{{ z.count }}</div>
              <div class="bo-ds-pct" :style="{color: z.color}">{{ z.pct }}%</div>
              <div class="bo-ds-label">{{ z.label }}</div>
              <div class="bo-ds-range">{{ z.range }}</div>
            </div>
          </div>
          <div class="bo-ds-bar-row">
            <div class="bo-ds-seg" v-for="z in boZones" :key="z.key"
              :style="{width: z.pct + '%', background: z.color}"
              :title="z.label + ': ' + z.count + '人'"></div>
          </div>
        </div>

        <!-- 当前异常血氧明细 -->
        <div class="bo-panel bo-panel-anomaly">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">当前异常血氧明细</span>
            <span class="bo-anomaly-count" v-if="boAnomalyList.length">
              共 <em>{{ boAnomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!boAnomalyList.length" class="bo-anomaly-empty">
            <span class="bo-anomaly-ok">✓</span> 当前无异常血氧人员
          </div>
          <div v-else class="bo-anomaly-body">
            <div class="bo-anomaly-hd">
              <span>姓名</span><span>部门</span><span>血氧</span><span>类型</span><span>时间</span>
            </div>
            <div class="bo-anomaly-list">
              <div
                class="bo-anomaly-row"
                v-for="(item, i) in boAnomalyList"
                :key="i"
                :class="item.bloodOxygen < 90 ? 'anom-danger' : 'anom-low'"
                @click="showDetail(item)"
                style="cursor:pointer"
              >
                <span class="ba-name">{{ item.userName }}</span>
                <span class="ba-dept">{{ item.deptName || item.dept_name || '--' }}</span>
                <span class="ba-val">{{ item.bloodOxygen }}%</span>
                <span class="ba-type">{{ item.bloodOxygen < 90 ? '危险↓↓' : '偏低↓' }}</span>
                <span class="ba-time">{{ fmtTime(item.recordTime) }}</span>
              </div>
            </div>
          </div>
        </div>

      </main>

      <!-- ─ 右侧：实时列表 ─ -->
      <div class="bo-rtlist">
        <div class="bo-panel" style="height:100%;display:flex;flex-direction:column;overflow:hidden">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">实时血氧数据</span>
            <span class="bo-rt-total">{{ realtimeList.length }} 条</span>
          </div>
          <div class="bo-rt-hd">
            <span>姓名</span><span>血氧</span><span>状态</span><span>时间</span>
          </div>
          <div class="bo-rt-body" ref="listRef">
            <div
              class="bo-rt-row"
              v-for="(item, i) in pagedList"
              :key="i"
              :class="boLevel(item.bloodOxygen)"
              @click="showDetail(item)"
              style="cursor:pointer"
            >
              <span class="bo-rt-name">{{ item.userName }}</span>
              <span class="bo-rt-val">
                {{ item.bloodOxygen }}%
                <em v-if="item.bloodOxygen < 90" class="bo-rt-arrow">↓</em>
              </span>
              <span class="bo-rt-badge" :class="boLevel(item.bloodOxygen)">
                {{ item.bloodOxygen < 90 ? '危险' : item.bloodOxygen < 95 ? '偏低' : item.bloodOxygen >= 99 ? '优秀' : '正常' }}
              </span>
              <span class="bo-rt-time">{{ fmtTime(item.recordTime) }}</span>
            </div>
          </div>
          <div class="bo-rt-pg">
            <button class="bo-pg-btn" :disabled="currentPage===1" @click="currentPage=1">首页</button>
            <button class="bo-pg-btn" :disabled="currentPage===1" @click="currentPage--">‹</button>
            <span class="bo-pg-info">{{ currentPage }} / {{ totalPages }}</span>
            <button class="bo-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage++">›</button>
            <button class="bo-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage=totalPages">末页</button>
          </div>
        </div>
      </div>

    </section>

    <el-dialog v-model="detailVisible" :title="`${detailItem?.userName || ''} 血氧详情`" width="400px" :append-to-body="true">
      <div v-if="detailItem" style="padding:8px 0">
        <div style="text-align:center;margin-bottom:20px">
          <span style="font-size:48px;font-weight:700;font-family:Consolas;color:#00d4ff">{{ detailItem.bloodOxygen }}</span>
          <span style="font-size:16px;color:#8ba6c8;margin-left:4px">%</span>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">
            <el-tag :class="boLevel(detailItem.bloodOxygen)" size="small" effect="dark"
              :type="detailItem.bloodOxygen < 90 ? 'danger' : detailItem.bloodOxygen < 95 ? 'warning' : detailItem.bloodOxygen >= 99 ? 'info' : 'success'">
              {{ detailItem.bloodOxygen < 90 ? '危险' : detailItem.bloodOxygen < 95 ? '偏低' : detailItem.bloodOxygen >= 99 ? '优秀' : '正常' }}
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
  getBloodOxygenOverview,
  getBloodOxygenTrend,
  getBloodOxygenDistribution,
  getAgeBloodOxygen,
  getRealtimeBloodOxygen,
  getTopUsers,
  getDeptAbnormalStats,
  getHourlyBloodOxygen
} from '@/api/blood-oxygen'

export default {
  name: 'BloodOxygenAnalysis',
  data() {
    return {
      currentTime: '',
      overview: {
        avgBloodOxygen: 0, minBloodOxygen: 0, maxBloodOxygen: 0,
        detectionRate: 0, abnormalCount: 0, totalCount: 0
      },
      distLegend: [],
      boRanges: [
        { label: '危险',   range: '< 90%',    color: '#ff5252' },
        { label: '偏低',   range: '90 – 94%', color: '#FFB84D' },
        { label: '正常',   range: '95 – 98%', color: '#52c41a' },
        { label: '优秀',   range: '≥ 99%',    color: '#4FC3F7' }
      ],
      top5Data: [],
      realtimeList: [],
      currentPage: 1,
      pageSize: 20,
      activePeriod: 'month',
      periodOptions: [
        { label: '当日', value: 'day' },
        { label: '近7日', value: 'week' },
        { label: '近30日', value: 'month' }
      ],
      charts: {},
      clockTimer: null, refreshTimer: null, scrollTimer: null, resizeTimer: null,
      detailItem: null,
      detailVisible: false,
      filterDept: ''
    }
  },
  computed: {
    headerKpis() {
      const o = this.overview
      return [
        { label: '平均血氧',   val: (o.avgBloodOxygen || '--') + '%', cls: 'kpi-cyan'   },
        { label: '异常人次',   val: o.abnormalCount || 0,              cls: 'kpi-orange'  },
        { label: '检测率',     val: (o.detectionRate || 0) + '%',      cls: 'kpi-green'   },
        { label: '总记录数',   val: (o.totalCount || 0).toLocaleString(), cls: 'kpi-blue' }
      ]
    },
    ovAllCards() {
      const o = this.overview
      return [
        { label: '最低血氧',   val: (o.minBloodOxygen || '--') + '', unit: '%',   color: '#FFB84D' },
        { label: '最高血氧',   val: (o.maxBloodOxygen || '--') + '', unit: '%',   color: '#4FC3F7' },
        { label: '血氧检测率', val: o.detectionRate || '--',          unit: '%',   color: '#52c41a' },
        { label: '异常记录',   val: o.abnormalCount  || '--',         unit: ' 人', color: '#ff5252' },
        { label: '总记录数',   val: (o.totalCount || 0).toLocaleString(), unit: ' 条', color: '#7eb8f7' },
        { label: '血氧范围',   val: o.minBloodOxygen != null && o.maxBloodOxygen != null ? `${o.minBloodOxygen}~${o.maxBloodOxygen}` : '--', unit: '%', color: '#a78bfa' }
      ]
    },
    overviewTitle() {
      return { day: '今日血氧概况', week: '近7日血氧概况', month: '近30日血氧概况' }[this.activePeriod]
    },
    hourlyTitle() {
      return { day: '今日24小时波动', week: '近7日每日均值', month: '近30日每日均值' }[this.activePeriod]
    },
    trendTitle() {
      return { day: '今日血氧趋势', week: '近7天血氧趋势', month: '近30天血氧趋势' }[this.activePeriod]
    },
    periodRange() {
      const today = dayjs().format('YYYY-MM-DD')
      if (this.activePeriod === 'day') return { startDate: today, endDate: today }
      if (this.activePeriod === 'week') return { startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'), endDate: today }
      return { startDate: dayjs().subtract(29, 'day').format('YYYY-MM-DD'), endDate: today }
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
    boZones() {
      const list = this.realtimeList
      const total = list.length || 1
      const danger   = list.filter(x => x.bloodOxygen < 90).length
      const low      = list.filter(x => x.bloodOxygen >= 90 && x.bloodOxygen < 95).length
      const normal   = list.filter(x => x.bloodOxygen >= 95 && x.bloodOxygen < 99).length
      const excellent = list.filter(x => x.bloodOxygen >= 99).length
      const pct = n => list.length > 0 ? Math.round(n / total * 100) : 0
      return [
        { key: 'danger',    label: '危险', range: '< 90%',    count: danger,    pct: pct(danger),    color: '#ff5252', icon: '↓', cls: 'zone-danger'    },
        { key: 'low',       label: '偏低', range: '90–94%',   count: low,       pct: pct(low),       color: '#FFB84D', icon: '↓', cls: 'zone-low'       },
        { key: 'normal',    label: '正常', range: '95–98%',   count: normal,    pct: pct(normal),    color: '#52c41a', icon: '✓', cls: 'zone-normal'    },
        { key: 'excellent', label: '优秀', range: '≥ 99%',    count: excellent, pct: pct(excellent), color: '#4FC3F7', icon: '↑', cls: 'zone-excellent' }
      ]
    },
    boAnomalyList() {
      return this.realtimeList.filter(x => x.bloodOxygen < 95)
    }
  },
  mounted() {
    this.initClock()
    this.fetchData()
    this.setScale()
    window.addEventListener('resize', this.handleResize)
    this.$nextTick(() => {
      this.startAutoScroll()
    })
    this.refreshTimer = setInterval(() => this.fetchData(), 30000)
  },
  beforeUnmount() {
    clearInterval(this.clockTimer)
    clearInterval(this.refreshTimer)
    clearInterval(this.scrollTimer)
    clearTimeout(this.resizeTimer)
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    initClock() {
      const tick = () => { this.currentTime = dayjs().format('YYYY年MM月DD日 HH:mm:ss') }
      tick(); this.clockTimer = setInterval(tick, 1000)
    },

    async fetchData() {
      await Promise.allSettled([
        this.loadOverview(), this.loadTopUsers(), this.loadDept(),
        this.loadAge(), this.loadDist(), this.loadTrend(), this.loadHourly(), this.loadRealtime()
      ])
    },

    async loadOverview() {
      const { startDate, endDate } = this.periodRange
      try { const r = await getBloodOxygenOverview(startDate, endDate); if (r.code === 200) this.overview = r.data } catch {}
      this.$nextTick(() => this.initGauge())
    },
    async loadTopUsers() {
      const { startDate, endDate } = this.periodRange
      let d = []; try { const r = await getTopUsers(5, startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
      this.top5Data = d
    },
    async loadDept() {
      const { startDate, endDate } = this.periodRange
      let d = []; try { const r = await getDeptAbnormalStats(startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
      this.$nextTick(() => this.initDept(d))
    },
    async loadAge() {
      let d = []; try { const r = await getAgeBloodOxygen(); if (r.code === 200) d = r.data || [] } catch {}
      this.$nextTick(() => this.initAge(d))
    },
    async loadDist() {
      const { startDate, endDate } = this.periodRange
      let d = []
      try {
        const r = await getBloodOxygenDistribution(startDate, endDate)
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
          const r = await getHourlyBloodOxygen(today, today)
          if (r.code === 200 && Array.isArray(r.data)) {
            r.data.forEach(({ hour, avgBloodOxygen }) => {
              if (hour >= 0 && hour < 24) vals[hour] = avgBloodOxygen
            })
          }
        } catch {}
        this.$nextTick(() => this.renderHourly(vals))
      } else {
        const days = this.activePeriod === 'week' ? 7 : 30
        let dates = [], vals = []
        try {
          const r = await getBloodOxygenTrend(days)
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
          const r = await getHourlyBloodOxygen(today, today)
          if (r.code === 200 && Array.isArray(r.data)) {
            r.data.forEach(({ hour, avgBloodOxygen }) => {
              if (hour >= 0 && hour < 24) vals[hour] = avgBloodOxygen
            })
          }
        } catch {}
        this.$nextTick(() => this.initTrendDay(vals))
      } else {
        const days = this.activePeriod === 'week' ? 7 : 30
        let d = {}
        try { const r = await getBloodOxygenTrend(days); if (r.code === 200) d = r.data || {} } catch {}
        this.$nextTick(() => this.initTrend(d))
      }
    },
    async loadRealtime() {
      try { const r = await getRealtimeBloodOxygen(200); if (r.code === 200) this.realtimeList = r.data || [] } catch {}
    },

    // ── 仪表盘（80-100%）──
    initGauge() {
      const el = this.$refs.gaugeRef; if (!el) return
      if (this.charts.gauge) this.charts.gauge.dispose()
      const c = echarts.init(el); this.charts.gauge = c
      const v = this.overview.avgBloodOxygen || 0
      c.setOption({
        series: [{
          type: 'gauge', startAngle: 225, endAngle: -45,
          radius: '90%', center: ['50%', '58%'],
          min: 80, max: 100,
          axisLine: { lineStyle: { width: 14, color: [[0.5,'#ff5252'],[0.75,'#FFB84D'],[0.95,'#52c41a'],[1,'#4FC3F7']] } },
          pointer: { length: '60%', width: 6, itemStyle: { color: '#00d4ff', shadowBlur: 14, shadowColor: 'rgba(0,212,255,0.8)' } },
          axisTick: { length: 5, distance: -20, lineStyle: { color: 'rgba(0,212,255,0.25)', width: 1 } },
          splitLine: { length: 10, distance: -20, lineStyle: { color: 'rgba(0,212,255,0.45)', width: 2 } },
          axisLabel: { color: '#8ba6c8', fontSize: 9, distance: -28 },
          detail: { show: false },
          data: [{ value: v }]
        }]
      })
    },

    // ── 部门统计（血氧：偏低+偏高）──
    initDept(data) {
      const el = this.$refs.deptRef; if (!el) return
      if (this.charts.dept) this.charts.dept.dispose()
      const c = echarts.init(el); this.charts.dept = c
      if (!data.length) {
        c.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#8ba6c8', fontSize: 14 } }] })
        return
      }
      const d = data.map(x => ({
        deptName: x.deptName || x.name,
        lowCount: x.lowCount || 0,
        highCount: x.highCount || 0
      }))
      c.setOption({
        backgroundColor: 'transparent',
        grid: { left: '26%', right: '8%', top: '10%', bottom: '6%' },
        xAxis: {
          type: 'value', axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 10 }
        },
        yAxis: {
          type: 'category', data: d.map(x => x.deptName), inverse: true,
          axisLine: { show: false }, axisTick: { show: false },
          axisLabel: { color: '#a8c5e6', fontSize: 11 }
        },
        series: [
          { name:'偏低', type:'bar', stack:'total', barWidth:'46%', data: d.map(x => x.lowCount),
            itemStyle: { color: new echarts.graphic.LinearGradient(1,0,0,0,[{offset:0,color:'#FFB84D'},{offset:1,color:'#FFA726'}]) },
            label: { show: true, position:'inside', color:'#fff', fontSize:10, formatter: p => p.value > 0 ? p.value : '' }
          },
          { name:'偏高', type:'bar', stack:'total', barWidth:'46%', data: d.map(x => x.highCount),
            itemStyle: { color: new echarts.graphic.LinearGradient(1,0,0,0,[{offset:0,color:'#4FC3F7'},{offset:1,color:'#29B6F6'}]), borderRadius:[0,4,4,0] },
            label: { show: true, position:'inside', color:'#fff', fontSize:10, formatter: p => p.value > 0 ? p.value : '' }
          }
        ]
      })
      c.off('click')
      c.on('click', (params) => {
        this.filterDept = this.filterDept === params.name ? '' : params.name
        this.currentPage = 1
      })
    },

    // ── 年龄段（血氧 94-99%）──
    initAge(data) {
      const el = this.$refs.ageRef; if (!el) return
      if (this.charts.age) this.charts.age.dispose()
      const c = echarts.init(el); this.charts.age = c
      const fb = [
        { ageRange: '20-30', avgBloodOxygen: 98 }, { ageRange: '30-40', avgBloodOxygen: 97 },
        { ageRange: '40-50', avgBloodOxygen: 96 }, { ageRange: '50+',   avgBloodOxygen: 95 }
      ]
      const d = data.length ? data : fb
      c.setOption({
        backgroundColor: 'transparent',
        grid: { left: '10%', right: '4%', top: '16%', bottom: '16%' },
        xAxis: {
          type: 'category', data: d.map(x => x.ageRange),
          axisLine: { lineStyle: { color: 'rgba(0,212,255,0.18)' } }, axisTick: { show: false },
          axisLabel: { color: '#a8c5e6', fontSize: 11 }
        },
        yAxis: {
          type: 'value', name: '%', nameTextStyle: { color: '#8ba6c8', fontSize: 10 },
          axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 10 },
          min: v => Math.max(80, v.min - 2), max: v => Math.min(100, v.max + 1)
        },
        series: [{
          type: 'bar', data: d.map(x => x.avgBloodOxygen), barWidth: '46%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0,0,0,1,
              [{ offset: 0, color: '#00d4ff' }, { offset: 1, color: 'rgba(0,100,220,0.35)' }]),
            borderRadius: [6, 6, 0, 0]
          },
          label: { show: true, position: 'top', color: '#00d4ff', fontSize: 11, fontWeight: 'bold',
            formatter: p => p.value + '%' }
        }]
      })
    },

    // ── 逐小时波动图渲染（数据由 loadHourly 提供）──
    renderHourly(vals) {
      const el = this.$refs.hourlyRef; if (!el) return
      if (this.charts.hourly) this.charts.hourly.dispose()
      const c = echarts.init(el); this.charts.hourly = c
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,0.9)', borderColor: 'rgba(0,212,255,0.25)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: p => p[0].value != null
            ? `${p[0].name}<br/>血氧：<b style="color:#00d4ff">${p[0].value}%</b>`
            : `${p[0].name}<br/>暂无数据`
        },
        grid: { left: '8%', right: '2%', top: '14%', bottom: '16%', containLabel: true },
        xAxis: {
          type: 'category', data: hours, boundaryGap: false,
          axisLine: { lineStyle: { color: 'rgba(0,212,255,0.15)' } }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 9, interval: 3 }
        },
        yAxis: {
          type: 'value', axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.06)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 9, formatter: v => v + '%' },
          min: v => v.min > 0 ? Math.max(80, v.min - 2) : 90,
          max: v => v.max > 0 ? Math.min(100, v.max + 1) : 100
        },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#00d4ff', width: 1.5 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
            [{ offset: 0, color: 'rgba(0,212,255,0.22)' }, { offset: 1, color: 'rgba(0,212,255,0.02)' }]) },
          markLine: {
            silent: true, symbol: 'none',
            data: [{ yAxis: 95, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 },
              label: { color: '#FFB84D', fontSize: 9, formatter: '正常下限95%' } }]
          }
        }]
      })
    },

    // ── 分布环形图 ──
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

    // ── 趋势折线图 ──
    initTrend(data) {
      const el = this.$refs.trendRef; if (!el) return
      if (this.charts.trend) this.charts.trend.dispose()
      const c = echarts.init(el); this.charts.trend = c
      const fbDates = Array.from({ length: 30 }, (_, i) => dayjs().subtract(29 - i, 'day').format('MM/DD'))
      const dates = data.dates  || fbDates
      const vals  = data.values || new Array(dates.length).fill(0)
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,0.92)', borderColor: 'rgba(0,212,255,0.25)',
          textStyle: { color: '#e0f0ff', fontSize: 12 },
          formatter: p => `${p[0].name}<br/>平均血氧：<b style="color:#00d4ff">${p[0].value}%</b>`
        },
        grid: { left: '5%', right: '3%', top: '12%', bottom: '12%', containLabel: true },
        xAxis: {
          type: 'category', data: dates, boundaryGap: false,
          axisLine: { lineStyle: { color: 'rgba(0,212,255,0.18)' } }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 10, interval: 4 }
        },
        yAxis: {
          type: 'value', axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 10, formatter: v => v + '%' },
          min: v => Math.max(80, v.min - 2), max: v => Math.min(100, v.max + 2)
        },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none',
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
            [{ offset: 0, color: 'rgba(0,212,255,0.28)' }, { offset: 1, color: 'rgba(0,212,255,0.02)' }]) },
          markPoint: {
            symbol: 'circle', symbolSize: 6,
            label: { fontSize: 10, fontWeight: 'bold', fontFamily: 'Consolas', offset: [0, -14] },
            data: [
              { type: 'min', name: '最低', itemStyle: { color: '#FFB84D' }, label: { color: '#FFB84D', formatter: p => '▼' + p.value + '%' } },
              { type: 'max', name: '最高', itemStyle: { color: '#4FC3F7' }, label: { color: '#4FC3F7', formatter: p => '▲' + p.value + '%' } }
            ]
          },
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: 90, lineStyle: { color: '#ff5252', type: 'dashed', width: 1 }, label: { color: '#ff5252', fontSize: 10, formatter: '危险 90%' } },
              { yAxis: 95, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '正常 95%' } }
            ]
          }
        }]
      })
    },

    renderHourlyDaily(dates, vals) {
      const el = this.$refs.hourlyRef; if (!el) return
      if (this.charts.hourly) this.charts.hourly.dispose()
      const c = echarts.init(el); this.charts.hourly = c
      if (!dates.length) {
        c.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#8ba6c8', fontSize: 13 } }] })
        return
      }
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,0.9)', borderColor: 'rgba(0,212,255,0.25)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: p => `${p[0].name}<br/>血氧：<b style="color:#a78bfa">${p[0].value}%</b>`
        },
        grid: { left: '8%', right: '2%', top: '14%', bottom: '16%', containLabel: true },
        xAxis: {
          type: 'category', data: dates, boundaryGap: true,
          axisLine: { lineStyle: { color: 'rgba(0,212,255,0.15)' } }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 9, interval: Math.floor(dates.length / 5) }
        },
        yAxis: {
          type: 'value', axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.06)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 9, formatter: v => v + '%' },
          min: v => v.min > 0 ? Math.max(80, v.min - 2) : 90,
          max: v => v.max > 0 ? Math.min(100, v.max + 1) : 100
        },
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

    switchPeriod(val) {
      if (this.activePeriod === val) return
      this.activePeriod = val
      this.fetchData()
    },

    initTrendDay(vals) {
      const el = this.$refs.trendRef; if (!el) return
      if (this.charts.trend) this.charts.trend.dispose()
      const c = echarts.init(el); this.charts.trend = c
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,0.92)', borderColor: 'rgba(0,212,255,0.25)',
          textStyle: { color: '#e0f0ff', fontSize: 12 },
          formatter: p => p[0].value != null
            ? `${p[0].name}<br/>血氧：<b style="color:#00d4ff">${p[0].value}%</b>`
            : `${p[0].name}<br/>暂无数据`
        },
        grid: { left: '5%', right: '3%', top: '12%', bottom: '12%', containLabel: true },
        xAxis: {
          type: 'category', data: hours, boundaryGap: false,
          axisLine: { lineStyle: { color: 'rgba(0,212,255,0.18)' } }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 10, interval: 3 }
        },
        yAxis: {
          type: 'value', axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 10, formatter: v => v + '%' },
          min: 90, max: 100
        },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
            [{ offset: 0, color: 'rgba(0,212,255,0.28)' }, { offset: 1, color: 'rgba(0,212,255,0.02)' }]) },
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: 95, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '正常下限 95%' } },
              { yAxis: 90, lineStyle: { color: '#ff5252', type: 'dashed', width: 1 }, label: { color: '#ff5252', fontSize: 10, formatter: '危险 90%' } }
            ]
          }
        }]
      })
    },

    boLevel(v) { return v < 90 ? 'danger' : v < 95 ? 'low' : v >= 99 ? 'excellent' : 'normal' },
    fmtTime(ts) { return ts ? dayjs(ts).format('MM-DD HH:mm') : '' },

    goToPortrait(item) {
      if (item.userCode || item.empCode) {
        this.$router.push({ path: '/personnel-management/health-portrait', query: { empCode: item.userCode || item.empCode } })
      } else {
        this.$router.push({ path: '/personnel-management/health-portrait', query: { name: item.userName } })
      }
    },
    showDetail(item) {
      this.detailItem = item
      this.detailVisible = true
    },

    setScale() {
      const el = this.$el; if (!el) return
      const bcr = el.getBoundingClientRect()
      const vw = window.innerWidth - bcr.left
      const vh = window.innerHeight - bcr.top
      const scale = Math.max(0.4, Math.min(1, Math.min(vw / 1920, vh / 1030)))
      el.style.transformOrigin = 'top left'
      el.style.transform = `scale(${scale})`
      if (scale < 1) {
        el.style.width = `${(1/scale)*100}%`; el.style.height = `${(1/scale)*vh}px`
        el.style.position = 'absolute'; el.style.top = bcr.top + 'px'; el.style.left = '0'
      } else {
        el.style.width = '1920px'; el.style.height = '1030px'
        el.style.position = ''; el.style.top = ''; el.style.left = ''
      }
      this.$nextTick(() => this.setPageSize())
    },
    setPageSize() {
      const el = this.$refs.listRef; if (!el) return
      const ROW_H = 27  // bo-rt-row: 6+6 padding + ~14px line + 1px margin
      const n = Math.max(10, Math.floor(el.clientHeight / ROW_H))
      if (n !== this.pageSize) {
        this.pageSize = n
        this.currentPage = 1
      }
    },
    handleResize() {
      clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        this.setScale()
        this.$nextTick(() => Object.values(this.charts).forEach(c => c && c.resize && c.resize()))
      }, 200)
    },
    startAutoScroll() {
      const el = this.$refs.listRef; if (!el) return
      let top = 0
      this.scrollTimer = setInterval(() => {
        const max = el.scrollHeight - el.clientHeight
        if (max <= 0) return
        if (top >= max) { setTimeout(() => { top = 0; el.scrollTop = 0 }, 1500) }
        else { top += 1; el.scrollTop = top }
      }, 40)
    }
  }
}
</script>

<style lang="scss" scoped>
$bg:     #080d1e;
$panel:  rgba(8, 16, 42, 0.88);
$border: rgba(0, 212, 255, 0.14);
$accent: #00d4ff;
$text:   #a8c5e6;
$dim:    #6a88ab;
$white:  #e8f4ff;

.bo-root {
  width: 100%;
  height: calc(100vh - 50px); /* 视口高度 - 顶部导航栏 */
  min-height: 600px; /* 最小高度防止过小 */
  background: $bg;
  background-image:
    radial-gradient(circle at 18% 28%, rgba(0,212,255,0.05) 0%, transparent 48%),
    radial-gradient(circle at 82% 72%, rgba(0,100,180,0.08) 0%, transparent 48%);
  overflow: hidden;
  display: flex; flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif; color: $text;
}

/* ── Header ── */
.bo-hd {
  height: 58px; flex-shrink: 0;
  display: flex; align-items: center; padding: 0 22px; gap: 20px;
  background: rgba(0,6,24,0.65);
  border-bottom: 1px solid $border;
}
.bo-hd-left  { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.bo-live-dot {
  width: 9px; height: 9px; border-radius: 50%;
  background: $accent; box-shadow: 0 0 8px $accent;
  animation: boPulse 2s ease-in-out infinite;
}
@keyframes boPulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.45;transform:scale(0.75)} }
.bo-hd-title { font-size: 20px; font-weight: 700; color: $white; margin: 0; letter-spacing: 2px; text-shadow: 0 0 14px rgba(0,212,255,0.45); }
.bo-hd-kpis  { flex: 1; display: flex; justify-content: center; }
.bo-kpi {
  display: flex; flex-direction: column; align-items: center;
  padding: 0 32px; border-right: 1px solid $border;
  &:first-child { border-left: 1px solid $border; }
}
.bo-kpi-n {
  font-size: 20px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1;
  &.kpi-cyan   { color: $accent; text-shadow: 0 0 10px rgba(0,212,255,0.5); }
  &.kpi-orange { color: #FFB84D; text-shadow: 0 0 10px rgba(255,184,77,0.4); }
  &.kpi-green  { color: #52c41a; text-shadow: 0 0 10px rgba(82,196,26,0.35); }
  &.kpi-blue   { color: #7eb8f7; }
}
.bo-kpi-l    { font-size: 11px; color: $dim; margin-top: 2px; white-space: nowrap; }
.bo-hd-time  { flex-shrink: 0; font-family: 'Consolas', monospace; font-size: 13px; color: $dim; }

/* ── Body ── */
.bo-bd { flex: 1; display: flex; gap: 10px; padding: 10px; overflow: hidden; min-height: 0; }

/* ── Aside ── */
.bo-aside       { width: 300px; flex-shrink: 0; display: flex; flex-direction: column; gap: 10px; }
.bo-aside-top   { height: 190px; flex-shrink: 0; }
.bo-aside-bot   { flex: 1; }

/* ── Main ── */
.bo-main        { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.bo-overview-panel { height: 162px; flex-shrink: 0; }
.bo-mid-row     { height: 190px; flex-shrink: 0; display: flex; gap: 10px; }
.bo-panel-age   { flex: 0 0 340px; }
.bo-panel-hourly{ flex: 1; }
.bo-panel-dist  { flex: 0 0 258px; }
.bo-panel-trend { flex: 1; min-height: 0; }

/* ── Rtlist ── */
.bo-rtlist { width: 272px; flex-shrink: 0; }

/* ── Panel ── */
.bo-panel {
  background: $panel; border: 1px solid $border; border-radius: 10px;
  display: flex; flex-direction: column; overflow: hidden; backdrop-filter: blur(8px);
}
.bo-ph {
  height: 38px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  border-bottom: 1px solid rgba(0,212,255,0.09);
  background: rgba(0,212,255,0.035);
}
.bo-ph-bar { width: 3px; height: 14px; background: linear-gradient(180deg,$accent,rgba(0,212,255,0.3)); border-radius: 2px; box-shadow: 0 0 6px rgba(0,212,255,0.7); }
.bo-ph-title { font-size: 13px; font-weight: 600; color: $white; letter-spacing: 1px; }
.bo-rt-total { margin-left: auto; font-size: 11px; color: $dim; }
.bo-ph-legend { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.bo-leg-dot   { width: 8px; height: 8px; border-radius: 2px; }
.bo-leg-txt   { font-size: 11px; color: $dim; }
.bo-period-tabs {
  display: flex;
  background: rgba(0,212,255,0.06);
  border: 1px solid rgba(0,212,255,0.2);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.bo-period-tab {
  padding: 4px 14px;
  font-size: 12px;
  color: $dim;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: $white; background: rgba(0,212,255,0.1); }
  &.is-active { color: $bg; background: $accent; font-weight: 700; }
}
.bo-trend-tags { margin-left: 12px; display: flex; gap: 10px; }
.bo-tag { font-size: 10px; padding: 2px 6px; border-radius: 3px; border: 1px solid; }
.bo-pc  { flex: 1; min-height: 0; padding: 6px; }

/* ── TOP5 ── */
.bo-top5-empty { padding: 20px 0; text-align: center; color: rgba(126,184,247,0.5); font-size: 12px; }
.bo-top5-list { padding: 8px 12px; display: flex; flex-direction: column; gap: 8px; }
.bo-top5-row  { display: flex; align-items: center; gap: 8px; }
.bo-top5-rank {
  width: 18px; height: 18px; border-radius: 4px; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  &.rank-1 { background: rgba(255,184,77,0.2); color: #FFB84D; border: 1px solid rgba(255,184,77,0.4); }
  &.rank-2 { background: rgba(0,212,255,0.12); color: $accent;  border: 1px solid rgba(0,212,255,0.3); }
  &.rank-3 { background: rgba(82,196,26,0.12); color: #52c41a;  border: 1px solid rgba(82,196,26,0.3); }
  &.rank-4, &.rank-5 { background: rgba(168,196,230,0.08); color: #8ba6c8; border: 1px solid rgba(168,196,230,0.2); }
}
.bo-top5-name    { font-size: 12px; color: $white; width: 64px; flex-shrink: 0; }
.bo-top5-bar-wrap{ flex: 1; height: 6px; background: rgba(0,212,255,0.08); border-radius: 3px; overflow: hidden; }
.bo-top5-bar     { height: 100%; border-radius: 3px; background: linear-gradient(90deg,$accent,#0066cc); transition: width 0.8s ease; }
.bo-top5-val     { font-size: 13px; font-weight: 700; color: $accent; font-family: 'Consolas', monospace; width: 22px; text-align: right; flex-shrink: 0; }

/* ── 概况 ── */
.bo-overview-body { flex: 1; min-height: 0; display: flex; align-items: center; padding: 8px 14px; gap: 14px; }
.bo-gauge-wrap  { width: 120px; height: 108px; flex-shrink: 0; position: relative; }
.bo-gauge-chart { width: 100%; height: 100%; }
.bo-gauge-center{ position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%); text-align: center; pointer-events: none; }
.bo-gauge-val   { font-size: 22px; font-weight: 700; color: $accent; font-family: 'Consolas', monospace; line-height: 1; }
.bo-gauge-sub   { font-size: 10px; color: $dim; margin-top: 1px; white-space: nowrap; }

.bo-kpi-cards {
  flex: 1; display: grid;
  grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(2, 1fr); gap: 7px;
}
.bo-kpi-card { background: rgba(0,212,255,0.04); border: 1px solid rgba(0,212,255,0.1); border-radius: 7px; padding: 7px 10px; display: flex; flex-direction: column; justify-content: center; }
.bo-kpi-card-val   { font-size: 18px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1; }
.bo-kpi-card-unit  { font-size: 10px; color: $dim; font-weight: normal; font-family: sans-serif; margin-left: 1px; }
.bo-kpi-card-label { font-size: 10px; color: $dim; margin-top: 2px; }

.bo-range-info  { width: 168px; flex-shrink: 0; background: rgba(0,212,255,0.03); border: 1px solid rgba(0,212,255,0.1); border-radius: 8px; padding: 8px 10px; display: flex; flex-direction: column; gap: 4px; }
.bo-range-title { font-size: 11px; color: $dim; font-weight: 600; margin-bottom: 3px; letter-spacing: 0.5px; }
.bo-range-item  { display: flex; align-items: center; gap: 6px; }
.bo-range-dot   { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.bo-range-name  { font-size: 10px; width: 38px; flex-shrink: 0; }
.bo-range-val   { font-size: 10px; color: $dim; font-family: 'Consolas', monospace; }

/* ── 分布 ── */
.bo-dist-body   { flex: 1; min-height: 0; display: flex; align-items: center; gap: 10px; padding: 8px 12px; }
.bo-dist-chart  { width: 110px; height: 110px; flex-shrink: 0; }
.bo-dist-legend { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.bo-dist-row    { display: flex; align-items: center; gap: 6px; }
.bo-dist-dot    { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.bo-dist-name   { font-size: 10px; color: $text; flex-shrink: 0; width: 72px; }
.bo-dist-bar-wrap { flex: 1; height: 4px; background: rgba(255,255,255,0.06); border-radius: 2px; overflow: hidden; }
.bo-dist-bar    { height: 100%; border-radius: 2px; transition: width 0.8s ease; opacity: 0.85; }
.bo-dist-pct    { font-size: 12px; font-weight: 700; font-family: 'Consolas', monospace; width: 32px; text-align: right; flex-shrink: 0; }

/* ── 实时列表 ── */
.bo-rt-hd {
  display: grid; grid-template-columns: 64px 50px 42px 1fr;
  gap: 6px; padding: 6px 10px; flex-shrink: 0;
  background: rgba(0,212,255,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.bo-rt-body {
  flex: 1; overflow-y: auto; padding: 3px 6px; min-height: 0;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.18); border-radius: 2px; }
}
.bo-rt-row {
  display: grid; grid-template-columns: 64px 50px 42px 1fr;
  gap: 6px; padding: 6px 4px; margin-bottom: 1px;
  border-radius: 5px; align-items: center;
  border-left: 2px solid transparent; transition: background 0.2s;
  &:hover { background: rgba(0,212,255,0.05); }
  &.normal    { border-left-color: rgba(82,196,26,0.45); }
  &.excellent { border-left-color: rgba(79,195,247,0.45); }
  &.low       { border-left-color: rgba(255,184,77,0.55); }
  &.danger    { border-left-color: rgba(255,82,82,0.65); }
}
.bo-rt-name { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bo-rt-val  {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace; color: #52c41a;
  .bo-rt-row.excellent & { color: #4FC3F7; }
  .bo-rt-row.low       & { color: #FFB84D; }
  .bo-rt-row.danger    & { color: #ff5252; }
}
.bo-rt-arrow { font-style: normal; font-size: 10px; animation: boArrow 1s infinite; }
@keyframes boArrow { 0%,100%{opacity:1} 50%{opacity:0.2} }
.bo-rt-badge {
  font-size: 10px; padding: 1px 3px; border-radius: 3px; text-align: center;
  &.normal    { background: rgba(82,196,26,0.13); color: #52c41a; border: 1px solid rgba(82,196,26,0.28); }
  &.excellent { background: rgba(79,195,247,0.13); color: #4FC3F7; border: 1px solid rgba(79,195,247,0.28); }
  &.low       { background: rgba(255,184,77,0.13); color: #FFB84D; border: 1px solid rgba(255,184,77,0.28); }
  &.danger    { background: rgba(255,82,82,0.13);  color: #ff5252; border: 1px solid rgba(255,82,82,0.28); }
}
.bo-rt-time { font-size: 10px; color: $dim; }

.bo-rt-pg {
  height: 36px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; gap: 5px;
  border-top: 1px solid rgba(0,212,255,0.1);
}
.bo-pg-btn {
  height: 22px; padding: 0 7px;
  background: rgba(0,212,255,0.07); border: 1px solid rgba(0,212,255,0.18);
  border-radius: 3px; color: $accent; font-size: 12px; cursor: pointer; transition: background 0.2s;
  &:hover:not(:disabled) { background: rgba(0,212,255,0.16); }
  &:disabled { opacity: 0.28; cursor: not-allowed; }
}
.bo-pg-info { font-size: 12px; color: $accent; min-width: 44px; text-align: center; }

// ── 血氧分布统计面板 ──
.bo-panel-dist-stat { flex-shrink: 0; }
.bo-ds-total {
  margin-left: auto; font-size: 12px; color: $dim;
  em { color: #93c5fd; font-style: normal; font-weight: 700; }
}
.bo-ds-body {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 8px; padding: 6px 0 8px;
}
.bo-ds-zone {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px; padding: 10px 8px 8px;
  text-align: center; transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.07); }
  &.zone-danger    { border-color: rgba(255,82,82,0.2);   }
  &.zone-low       { border-color: rgba(255,184,77,0.2);  }
  &.zone-normal    { border-color: rgba(82,196,26,0.2);   }
  &.zone-excellent { border-color: rgba(79,195,247,0.2);  }
}
.bo-ds-icon  { font-size: 16px; margin-bottom: 4px; }
.bo-ds-count { font-size: 24px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1; }
.bo-ds-pct   { font-size: 11px; margin-top: 1px; }
.bo-ds-label { font-size: 13px; font-weight: 600; color: $white; margin-top: 4px; }
.bo-ds-range { font-size: 10px; color: $dim; margin-top: 2px; }
.bo-ds-bar-row {
  display: flex; height: 6px; border-radius: 3px; overflow: hidden;
  background: rgba(255,255,255,0.05); margin-bottom: 2px;
}
.bo-ds-seg { transition: width 0.4s ease; min-width: 0; }

// ── 异常血氧明细面板 ──
.bo-panel-anomaly { flex-shrink: 0; }
.bo-anomaly-count {
  margin-left: auto; font-size: 12px; color: $dim;
  em { color: #ff8a80; font-style: normal; font-weight: 700; }
}
.bo-anomaly-empty {
  text-align: center; padding: 14px 0; font-size: 13px; color: $dim;
}
.bo-anomaly-ok { color: #52c41a; font-size: 15px; margin-right: 4px; }
.bo-anomaly-body { display: flex; flex-direction: column; }
.bo-anomaly-hd {
  display: grid; grid-template-columns: 1.2fr 1.5fr 0.9fr 0.9fr 1.4fr;
  padding: 4px 8px; font-size: 11px; color: $dim;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.bo-anomaly-list { max-height: 120px; overflow-y: auto; }
.bo-anomaly-row {
  display: grid; grid-template-columns: 1.2fr 1.5fr 0.9fr 0.9fr 1.4fr;
  padding: 5px 8px; font-size: 12px; border-radius: 4px;
  transition: background 0.15s;
  &:hover { background: rgba(255,255,255,0.05); }
  &.anom-danger { background: rgba(255,82,82,0.06); }
  &.anom-low    { background: rgba(255,184,77,0.06); }
}
.ba-name { color: $white; font-weight: 500; }
.ba-dept { color: $dim; }
.ba-val  { color: #00d4ff; font-family: Consolas; font-weight: 700; }
.ba-type { }
.anom-danger .ba-type { color: #ff5252; font-weight: 600; }
.anom-low    .ba-type { color: #FFB84D; font-weight: 600; }
.ba-time { font-size: 10px; color: $dim; }
</style>