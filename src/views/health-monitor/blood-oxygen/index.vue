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
      <button class="hm-export-btn" @click="exportExcel" title="导出当前数据">⬇ 导出</button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="bo-bd">

      <!-- ─ 左侧：TOP5 紧凑列表 + 部门统计 ─ -->
      <aside class="bo-aside">
        <div class="bo-panel bo-aside-top">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">{{ top5Title }}</span>
          </div>
          <div class="bo-top5-list" ref="top5ScrollRef">
            <div v-if="!top5Data.length" class="bo-top5-empty">暂无异常频次数据</div>
            <div class="bo-top5-row" v-for="(item, i) in displayedTop5" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="bo-top5-rank" :class="i < 3 ? 'rank-'+(i+1) : 'rank-n'">{{ i+1 }}</span>
              <span class="bo-top5-name">{{ item.userName }}</span>
              <div class="bo-top5-bar-wrap">
                <div class="bo-top5-bar" :style="{width: (item.count / top5Max * 100) + '%'}"></div>
              </div>
              <span class="bo-top5-val">{{ item.count }}</span>
            </div>
            <div v-if="top5Data.length > 20" class="bo-top5-more" @click="top5Expanded = !top5Expanded">
              {{ top5Expanded ? '▲ 收起' : '▼ 展开全部 (' + top5Data.length + '条)' }}
            </div>
          </div>
        </div>

        <div class="bo-panel bo-aside-bot">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">部门血氧异常统计</span>
            <span v-if="filterDept" class="bo-dept-tag" @click="filterDept=''" title="点击取消筛选">{{ filterDept }} ×</span>
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
            <span class="bo-ph-title">当前异常血氧明细{{ filterDept ? ' — ' + filterDept : '' }}</span>
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
        <div class="bo-panel hm-panel-flex">
          <div class="bo-ph">
            <span class="bo-ph-bar"></span>
            <span class="bo-ph-title">实时血氧数据</span>
            <span class="bo-rt-total">{{ realtimeList.length }} 条</span>
          </div>
          <div class="bo-rt-hd">
            <span>#</span><span>姓名</span><span>血氧</span><span>状态</span><span>时间</span>
          </div>
          <div class="bo-rt-body" ref="listRef">
            <div
              class="bo-rt-row"
              v-for="(item, i) in filteredRealtimeList"
              :key="i"
              :class="boLevel(item.bloodOxygen)"
              @click="showDetail(item)"
              style="cursor:pointer"
            >
              <span class="bo-rt-idx">{{ i + 1 }}</span>
              <span class="bo-rt-name">{{ item.userName }}</span>
              <span class="bo-rt-val">
                {{ item.bloodOxygen }}%
                <em v-if="item.bloodOxygen < 90" class="bo-rt-arrow">↓</em>
              </span>
              <span class="bo-rt-badge" :class="boLevel(item.bloodOxygen)">
                {{ item.bloodOxygen < 90 ? '危险' : item.bloodOxygen < 95 ? '偏低' : item.bloodOxygen >= 99 ? '优秀' : '正常' }}
              </span>
              <span class="bo-rt-time">{{ fmtRtTime(item.recordTime) }}</span>
            </div>
          </div>
        </div>
      </div>

    </section>

    <el-dialog v-model="detailVisible" :title="`${detailItem?.userName || ''} 血氧详情`" width="400px" :append-to-body="true">
      <div v-if="detailItem" style="padding:8px 0">
        <div style="text-align:center;margin-bottom:20px">
          <span class="hm-detail-value">{{ detailItem.bloodOxygen }}</span>
          <span class="hm-detail-unit">%</span>
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
import dayjs from 'dayjs'
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'
import {
  getBloodOxygenOverview,
  getBloodOxygenTrend,
  getBloodOxygenDistribution,
  getAgeBloodOxygen,
  getRealtimeBloodOxygen,
  getBloodOxygenTopUsers,
  getBloodOxygenDeptStats,
  getHourlyBloodOxygen
} from '@/api/blood-oxygen'
import { spo2Level, SPO2 } from '@/constants/health-thresholds'
import { initChart, distOption, gaugeOption, gradH, gradV } from '@/utils/chart-helpers'
import { emptyOption, chartTooltip, categoryAxis, valueAxis, deptGrid, trendGrid, hourlyGrid, ageGrid, barLabel } from '@/utils/echarts-config'
import chartPageMixin from '@/mixins/chartPage'
import { PERIOD_OPTIONS } from '@/constants/periods'

export default {
  name: 'BloodOxygenAnalysis',
  mixins: [chartPageMixin],
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
      top5Expanded: false,
      realtimeList: [],
      currentPage: 1,
      pageSize: 20,
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      charts: {},
      detailItem: null,
      detailVisible: false,
      _top5ScrollTimer: null
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
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.count)) : 1
    },
    displayedTop5() {
      return this.top5Expanded ? this.top5Data : this.top5Data.slice(0, 20)
    },
    top5Title() {
      const p = { day: '今日', week: '近7日', month: '近30日' }[this.activePeriod]
      return p + '异常频次排行'
    },
    /* pagedList / totalPages / filteredRealtimeList from chartPageMixin */
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
      return this.filteredRealtimeList.filter(x => x.bloodOxygen < 95)
    }
  },
  mounted() {
    this.initPage()
    this.$nextTick(() => {
      this._ro = new ResizeObserver(() => this.setPageSize(27))
      const el = this.$refs.listRef
      if (el) { this._ro.observe(el); this.setPageSize(27) }
    })
  },
  beforeUnmount() {
    if (this._ro) this._ro.disconnect()
    if (this._top5ScrollTimer) clearInterval(this._top5ScrollTimer)
  },
  methods: {

    exportExcel() {
      const list = this.realtimeList
      if (!list.length) { ElMessage.warning('暂无数据可导出'); return }
      const data = list.map(r => ({
        '姓名': r.userName || '--', '部门': r.deptName || '--', '工号': r.empCode || '--',
        '血氧饱和度(%)': r.bloodOxygen ?? '--',
        '状态': (r.bloodOxygen && r.bloodOxygen < 95) ? '异常' : '正常',
        '记录时间': r.recordTime ? dayjs(r.recordTime).format('YYYY-MM-DD HH:mm') : '--'
      }))
      const ws = XLSX.utils.json_to_sheet(data)
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, '血氧数据')
      XLSX.writeFile(wb, `血氧分析_${dayjs().format('YYYYMMDD')}.xlsx`)
      ElMessage.success(`已导出 ${list.length} 条记录`)
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
      let d = []; try { const r = await getBloodOxygenTopUsers(1000, startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
      this.top5Data = d
      this.$nextTick(() => this.startTop5Scroll())
    },
    async loadDept() {
      const { startDate, endDate } = this.periodRange
      let d = []; try { const r = await getBloodOxygenDeptStats(startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
      this.$nextTick(() => this.initDept(d))
    },
    async loadAge() {
      const { startDate, endDate } = this.periodRange
      let d = []; try { const r = await getAgeBloodOxygen(startDate, endDate); if (r.code === 200) d = r.data || [] } catch {}
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
    fmtRtTime(ts) {
      return ts ? dayjs(ts).format('HH:mm:ss') : ''
    },

    // ── 仪表盘（80-100%）──
    initGauge() {
      const c = initChart(this.charts, 'gauge', this.$refs.gaugeRef)
      if (c) c.setOption(gaugeOption(this.overview.avgBloodOxygen || 0, {
        min: 80, max: 100, colors: [[0.5,'#ff5252'],[0.75,'#FFB84D'],[0.95,'#52c41a'],[1,'#4FC3F7']]
      }))
    },

    // ── 部门统计（血氧：偏低+偏高）──
    initDept(data) {
      const c = initChart(this.charts, 'dept', this.$refs.deptRef); if (!c) return
      if (!data.length) { c.setOption(emptyOption()); return }
      const d = data.map(x => ({
        deptName: x.deptName || x.name,
        lowCount: x.lowCount || 0,
        highCount: x.highCount || 0
      }))
      c.setOption({
        backgroundColor: 'transparent',
        grid: deptGrid(),
        xAxis: valueAxis(),
        yAxis: { ...categoryAxis(d.map(x => x.deptName), { show: false }), inverse: true },
        series: [
          { name:'偏低', type:'bar', stack:'total', barWidth:'46%', data: d.map(x => x.lowCount),
            itemStyle: { color: gradH('#FFB84D','#FFA726') },
            label: barLabel()
          },
          { name:'偏高', type:'bar', stack:'total', barWidth:'46%', data: d.map(x => x.highCount),
            itemStyle: { color: gradH('#4FC3F7','#29B6F6'), borderRadius:[0,4,4,0] },
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

    // ── 年龄段（血氧 94-99%）──
    initAge(data) {
      const c = initChart(this.charts, 'age', this.$refs.ageRef); if (!c) return
      const d = data
      c.setOption({
        backgroundColor: 'transparent',
        grid: ageGrid(),
        xAxis: categoryAxis(d.map(x => x.ageRange)),
        yAxis: valueAxis({ name: '%', min: v => Math.max(80, v.min - 2), max: v => Math.min(100, v.max + 1) }),
        series: [{
          type: 'bar', data: d.map(x => x.avgBloodOxygen), barWidth: '46%',
          itemStyle: {
            color: gradV('#00d4ff', 'rgba(0,100,220,0.35)'),
            borderRadius: [6, 6, 0, 0]
          },
          label: { show: true, position: 'top', color: '#00d4ff', fontSize: 11, fontWeight: 'bold',
            formatter: p => p.value + '%' }
        }]
      })
    },

    // ── 逐小时波动图渲染（数据由 loadHourly 提供）──
    renderHourly(vals) {
      const c = initChart(this.charts, 'hourly', this.$refs.hourlyRef); if (!c) return
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => p[0].value != null
            ? `${p[0].name}<br/>血氧：<b style="color:#00d4ff">${p[0].value}%</b>`
            : `${p[0].name}<br/>暂无数据`),
        grid: hourlyGrid(),
        xAxis: { ...categoryAxis(hours, { fontSize: 9, interval: 3, lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: false },
        yAxis: { ...valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)',
          min: v => v.min > 0 ? Math.max(80, v.min - 2) : 90,
          max: v => v.max > 0 ? Math.min(100, v.max + 1) : 100
        }), axisLabel: { color: '#8ba6c8', fontSize: 9, formatter: v => v + '%' } },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#00d4ff', width: 1.5 },
          areaStyle: { color: gradV('rgba(0,212,255,0.22)', 'rgba(0,212,255,0.02)') },
          markLine: {
            silent: true, symbol: 'none',
            data: [{ yAxis: 95, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 },
              label: { color: '#FFB84D', fontSize: 9, formatter: '正常下限95%' } }]
          }
        }]
      })
    },

    initDist(data) {
      const c = initChart(this.charts, 'dist', this.$refs.distRef)
      if (c) c.setOption(distOption(data))
    },

    // ── 趋势折线图 ──
    initTrend(data) {
      const c = initChart(this.charts, 'trend', this.$refs.trendRef); if (!c) return
      const dates = data.dates  || []
      const vals  = data.values || []
      if (!dates.length) { c.setOption(emptyOption('暂无数据', 13)); return }
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => `${p[0].name}<br/>平均血氧：<b style="color:#00d4ff">${p[0].value}%</b>`),
        grid: trendGrid(),
        xAxis: { ...categoryAxis(dates, { fontSize: 10, interval: 4 }), boundaryGap: false },
        yAxis: { ...valueAxis({ min: v => Math.max(80, v.min - 2), max: v => Math.min(100, v.max + 2) }),
          axisLabel: { color: '#8ba6c8', fontSize: 10, formatter: v => v + '%' } },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none',
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: gradV('rgba(0,212,255,0.28)', 'rgba(0,212,255,0.02)') },
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
              { yAxis: SPO2.DANGER, lineStyle: { color: '#ff5252', type: 'dashed', width: 1 }, label: { color: '#ff5252', fontSize: 10, formatter: '危险 ' + SPO2.DANGER + '%' } },
              { yAxis: SPO2.LOW, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '正常 ' + SPO2.LOW + '%' } }
            ]
          }
        }]
      })
    },

    renderHourlyDaily(dates, vals) {
      const c = initChart(this.charts, 'hourly', this.$refs.hourlyRef); if (!c) return
      if (!dates.length) { c.setOption(emptyOption('暂无数据', 13)); return }
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => `${p[0].name}<br/>血氧：<b style="color:#a78bfa">${p[0].value}%</b>`),
        grid: hourlyGrid(),
        xAxis: { ...categoryAxis(dates, { fontSize: 9, interval: Math.floor(dates.length / 5), lineColor: 'rgba(0,212,255,0.15)' }), boundaryGap: true },
        yAxis: { ...valueAxis({ fontSize: 9, splitColor: 'rgba(0,212,255,0.06)',
          min: v => v.min > 0 ? Math.max(80, v.min - 2) : 90,
          max: v => v.max > 0 ? Math.min(100, v.max + 1) : 100
        }), axisLabel: { color: '#8ba6c8', fontSize: 9, formatter: v => v + '%' } },
        series: [{
          type: 'bar', data: vals, barMaxWidth: 14,
          itemStyle: {
            color: gradV('#a78bfa', 'rgba(167,139,250,0.2)'),
            borderRadius: [3, 3, 0, 0]
          }
        }]
      })
    },

    initTrendDay(vals) {
      const c = initChart(this.charts, 'trend', this.$refs.trendRef); if (!c) return
      const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: chartTooltip(p => p[0].value != null
            ? `${p[0].name}<br/>血氧：<b style="color:#00d4ff">${p[0].value}%</b>`
            : `${p[0].name}<br/>暂无数据`),
        grid: trendGrid(),
        xAxis: { ...categoryAxis(hours, { fontSize: 10, interval: 3 }), boundaryGap: false },
        yAxis: { ...valueAxis({ min: 90, max: 100 }),
          axisLabel: { color: '#8ba6c8', fontSize: 10, formatter: v => v + '%' } },
        series: [{
          type: 'line', data: vals, smooth: true, symbol: 'none', connectNulls: false,
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: gradV('rgba(0,212,255,0.28)', 'rgba(0,212,255,0.02)') },
          markLine: {
            silent: true, symbol: 'none',
            data: [
              { yAxis: SPO2.LOW, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 }, label: { color: '#FFB84D', fontSize: 10, formatter: '正常下限 ' + SPO2.LOW + '%' } },
              { yAxis: SPO2.DANGER, lineStyle: { color: '#ff5252', type: 'dashed', width: 1 }, label: { color: '#ff5252', fontSize: 10, formatter: '危险 ' + SPO2.DANGER + '%' } }
            ]
          }
        }]
      })
    },

    boLevel: spo2Level,

    startTop5Scroll() {
      if (this._top5ScrollTimer) { clearInterval(this._top5ScrollTimer); this._top5ScrollTimer = null }
      const el = this.$refs.top5ScrollRef
      if (!el || el.scrollHeight <= el.clientHeight) return
      let paused = false
      this._top5ScrollTimer = setInterval(() => {
        if (paused) return
        el.scrollTop += 1
        if (el.scrollTop + el.clientHeight >= el.scrollHeight - 2) {
          paused = true
          setTimeout(() => { el.scrollTop = 0; paused = false }, 1500)
        }
      }, 40)
    },

    // setPageSize → chartPageMixin
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/hm-vars';
@import '@/styles/hm-layout';

@include hm-body('bo');
@include hm-main('bo');
@include hm-panel('bo');
@include hm-overview('bo');
@include hm-kpi-cards('bo');
@include hm-range-info('bo');
@include hm-pagination('bo');

.bo-root {
  width: 100%;
  height: calc(100vh - 50px) !important; /* 视口高度 - 顶部导航栏 */
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
  animation: hmPulse 2s ease-in-out infinite;
}
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

/* ── Body（hm-body mixin） ── */

/* ── Aside ── */
.bo-aside       { width: 300px; flex-shrink: 0; display: flex; flex-direction: column; gap: 10px; }
.bo-aside-top   { height: 190px; flex-shrink: 0; display: flex; flex-direction: column; }
.bo-aside-bot   { flex: 1; }

/* ── Main（hm-main mixin） ── */
.bo-overview-panel { height: 162px; flex-shrink: 0; }
.bo-mid-row     { height: 190px; flex-shrink: 0; display: flex; gap: 10px; }
.bo-panel-age   { flex: 0 0 340px; }
.bo-panel-hourly{ flex: 1; }
.bo-panel-dist  { flex: 0 0 258px; }
.bo-panel-trend { flex: 0 0 165px; }

/* ── Rtlist ── */
.bo-rtlist {
  width: 272px; flex-shrink: 0;
  display: flex; flex-direction: column; min-height: 0;
  .bo-panel { flex: 1; min-height: 0; }
}

/* ── Panel（hm-panel mixin + 页面特有） ── */
.bo-ph {
  height: 38px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  border-bottom: 1px solid rgba(0,212,255,0.09);
  background: rgba(0,212,255,0.035);
}
.bo-ph-bar { width: 3px; height: 14px; background: linear-gradient(180deg,$accent,rgba(0,212,255,0.3)); border-radius: 2px; box-shadow: 0 0 6px rgba(0,212,255,0.7); }
// ph-title, rt-total, ph-legend, leg-dot, leg-txt → hm-panel mixin
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
// trend-tags, tag, pc → hm-panel mixin

/* ── TOP5 ── */
.bo-top5-empty { padding: 20px 0; text-align: center; color: rgba(126,184,247,0.5); font-size: 12px; }
.bo-top5-list {
  flex: 1; overflow-y: auto;
  padding: 8px 12px; display: flex; flex-direction: column; gap: 8px;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.18); border-radius: 2px; }
}
.bo-top5-row  { display: flex; align-items: center; gap: 8px; }
.bo-top5-rank {
  width: 18px; height: 18px; border-radius: 4px; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  &.rank-1 { background: rgba(255,184,77,0.2); color: #FFB84D; border: 1px solid rgba(255,184,77,0.4); }
  &.rank-2 { background: rgba(0,212,255,0.12); color: $accent;  border: 1px solid rgba(0,212,255,0.3); }
  &.rank-3 { background: rgba(82,196,26,0.12); color: #52c41a;  border: 1px solid rgba(82,196,26,0.3); }
  &.rank-4, &.rank-5, &.rank-n { background: rgba(168,196,230,0.08); color: #8ba6c8; border: 1px solid rgba(168,196,230,0.2); }
}
.bo-top5-name    { font-size: 12px; color: $white; width: 64px; flex-shrink: 0; }
.bo-top5-bar-wrap{ flex: 1; height: 6px; background: rgba(0,212,255,0.08); border-radius: 3px; overflow: hidden; }
.bo-top5-bar     { height: 100%; border-radius: 3px; background: linear-gradient(90deg,$accent,#0066cc); transition: width 0.8s ease; }
.bo-top5-val     { font-size: 13px; font-weight: 700; color: $accent; font-family: 'Consolas', monospace; width: 22px; text-align: right; flex-shrink: 0; }
.bo-top5-more    { text-align: center; font-size: 11px; color: $accent; padding: 6px 0; cursor: pointer; opacity: 0.7; &:hover { opacity: 1; } }

// 概况 / kpi-cards / range-info → hm-overview + hm-kpi-cards + hm-range-info mixins

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
  display: grid; grid-template-columns: 28px 1fr 50px 42px 44px;
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
  display: grid; grid-template-columns: 28px 1fr 50px 42px 44px;
  gap: 6px; padding: 6px 4px; margin-bottom: 1px;
  border-radius: 5px; align-items: center;
  border-left: 2px solid transparent; transition: background 0.2s;
  &:hover { background: rgba(0,212,255,0.05); }
  &.normal    { border-left-color: rgba(82,196,26,0.45); }
  &.excellent { border-left-color: rgba(79,195,247,0.45); }
  &.low       { border-left-color: rgba(255,184,77,0.55); }
  &.danger    { border-left-color: rgba(255,82,82,0.65); }
}
.bo-rt-idx  { font-size: 11px; color: $dim; font-family: 'Consolas', monospace; text-align: center; }
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

// pagination → hm-pagination mixin
.bo-pg-info { font-size: 12px; color: $accent; min-width: 44px; text-align: center; } // override mixin

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

// 部门筛选标签
.bo-dept-tag {
  font-size: 11px; padding: 1px 6px; border-radius: 3px;
  background: rgba(0,212,255,0.15); color: $accent; border: 1px solid rgba(0,212,255,0.35);
  cursor: pointer;
  &:hover { background: rgba(0,212,255,0.25); }
}

// ── 异常血氧明细面板 ──
.bo-panel-anomaly { flex: 1; min-height: 200px; display: flex; flex-direction: column; overflow: hidden; }
.bo-anomaly-count {
  margin-left: auto; font-size: 12px; color: $dim;
  em { color: #ff8a80; font-style: normal; font-weight: 700; }
}
.bo-anomaly-empty {
  text-align: center; padding: 14px 0; font-size: 13px; color: $dim;
}
.bo-anomaly-ok { color: #52c41a; font-size: 15px; margin-right: 4px; }
.bo-anomaly-body { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.bo-anomaly-hd {
  display: grid; grid-template-columns: 1.2fr 1.5fr 0.9fr 0.9fr 1.4fr;
  padding: 4px 8px; font-size: 11px; color: $dim; flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.bo-anomaly-list {
  flex: 1; overflow-y: auto;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,184,77,0.2); border-radius: 2px; }
}
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

@include hm-mobile('bo');

@media (max-width: 768px) {
  /* mid-row 三个 panel 竖向堆叠 */
  .bo-mid-row {
    flex-direction: column !important;
    height: auto !important;
  }
  .bo-panel-age,
  .bo-panel-hourly {
    flex: none !important;
    height: 200px;
    .bo-pc { height: 160px; }
  }
  .bo-panel-dist {
    flex: none !important;
    height: auto;
    .bo-dist-body { padding: 6px 10px; }
  }
  /* 趋势 panel 标题不折行 */
  .bo-ph-title { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 55vw; }
  /* trend legend 在手机上折行显示 */
  .bo-ph-legend { flex-wrap: wrap; gap: 6px 10px; }
}
</style>