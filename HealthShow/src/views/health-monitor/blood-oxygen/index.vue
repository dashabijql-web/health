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
      <button class="hm-export-btn" @click="exportExcel" title="导出当前数据">导出</button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="bo-bd" v-loading="pageLoading" element-loading-text="数据加载中..." element-loading-background="rgba(10,20,40,0.7)">

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

        <!-- Hero 区：大仪表盘 + 4 区间卡 -->
        <div class="bo-hero">
          <div class="bo-gauge-wrap">
            <div ref="gaugeRef" class="bo-gauge-chart"></div>
            <div class="bo-gauge-center">
              <div class="bo-gauge-val">{{ overview.avgBloodOxygen || '--' }}</div>
              <div class="bo-gauge-sub">% · 平均血氧</div>
            </div>
          </div>
          <div class="bo-zone-cards">
            <div v-for="z in boZones" :key="z.key" :class="['bo-zone-card', z.cls]">
              <span class="bo-zone-icon" :style="{color: z.color}">{{ z.icon }}</span>
              <span class="bo-zone-count" :style="{color: z.color}">{{ z.count }}<em>人</em></span>
              <span class="bo-zone-label">{{ z.label }}</span>
              <span class="bo-zone-range">{{ z.range }}</span>
              <div class="bo-zone-pct-bar">
                <div class="bo-zone-pct-fill" :style="{ width: z.pct + '%', background: z.color }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 图表行：趋势 + 小时波动 -->
        <div class="bo-charts-row">
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
          <div class="bo-panel bo-panel-hourly">
            <div class="bo-ph">
              <span class="bo-ph-bar"></span>
              <span class="bo-ph-title">{{ hourlyTitle }}</span>
            </div>
            <div class="bo-pc">
              <div ref="hourlyRef" style="width:100%;height:100%"></div>
            </div>
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
            当前无异常血氧人员
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
import {
  getBloodOxygenOverview,
  getBloodOxygenTrend,
  getRealtimeBloodOxygen,
  getBloodOxygenTopUsers,
  getBloodOxygenDeptStats,
  getHourlyBloodOxygen
} from '@/api/blood-oxygen'
import { spo2Level } from '@/constants/health-thresholds'
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
import { bloodOxygenChartMethods } from './blood-oxygen-chart'

export default {
  name: 'BloodOxygenAnalysis',
  mixins: [chartPageMixin, metricPageMixin],
  data() {
    return {
      pageLoading: false,
      currentTime: '',
      overview: {
        avgBloodOxygen: 0, minBloodOxygen: 0, maxBloodOxygen: 0,
        detectionRate: 0, abnormalCount: 0, totalCount: 0
      },
      top5Data: [],
      top5Expanded: false,
      realtimeList: [],
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      charts: {},
      detailItem: null,
      detailVisible: false,
      _top5ScrollLoop: null
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
        { key: 'normal',    label: '正常', range: '95–98%',   count: normal,    pct: pct(normal),    color: '#52c41a', icon: 'OK', cls: 'zone-normal'    },
        { key: 'excellent', label: '优秀', range: '≥ 99%',    count: excellent, pct: pct(excellent), color: '#4FC3F7', icon: '↑', cls: 'zone-excellent' }
      ]
    },
    boAnomalyList() {
      return this.filteredRealtimeList.filter(x => x.bloodOxygen < 95)
    }
  },
  mounted() {
    this.initPage()
  },
  methods: {
    ...bloodOxygenChartMethods,

    async exportExcel() {
      const list = this.realtimeList
      const cols = [
        { label: '姓名', key: 'userName' },
        { label: '部门', key: 'deptName' },
        { label: '工号', key: 'empCode' },
        { label: '血氧饱和度(%)', key: 'bloodOxygen' },
        { label: '状态', key: 'status' },
        { label: '记录时间', key: 'recordTime' }
      ]
      await exportMetricRows({
        rows: list,
        columns: cols,
        filenamePrefix: '血氧分析',
        mapRow: r => ({
          userName: r.userName || '--',
          deptName: r.deptName || '--',
          empCode: r.empCode || '--',
          bloodOxygen: r.bloodOxygen ?? '--',
          status: (r.bloodOxygen && r.bloodOxygen < 95) ? '异常' : '正常',
          recordTime: r.recordTime ? dayjs(r.recordTime).format('YYYY-MM-DD HH:mm') : '--'
        })
      })
    },

    async fetchData() {
      await Promise.allSettled([
        this.loadOverview(), this.loadTopUsers(), this.loadDept(),
        this.loadTrendAndHourly(), this.loadRealtime()
      ])
    },

    async loadOverview() {
      await loadMetricOverview(this, getBloodOxygenOverview, this.overview)
      this.$nextTick(() => this.initGauge())
    },
    async loadTopUsers() {
      await loadMetricTopUsers(this, getBloodOxygenTopUsers)
    },
    async loadDept() {
      await loadMetricRangeChart(this, getBloodOxygenDeptStats, 'initDept')
    },
    /** 合并 loadTrend + loadHourly，避免在 week/month 模式下发出两次相同的 trend 请求 */
    async loadTrendAndHourly() {
      if (this.activePeriod === 'day') {
        const today = getMetricToday()
        const rows = await fetchMetricData(() => getHourlyBloodOxygen(today, today), [])
        const vals = createHourlySeries(rows, 'avgBloodOxygen')
        this.$nextTick(() => { this.initTrendDay(vals); this.renderHourly(vals) })
      } else {
        const days = metricDaysForPeriod(this.activePeriod)
        const d = await fetchMetricData(() => getBloodOxygenTrend(days), {})
        this.$nextTick(() => {
          this.initTrend(d)
          this.renderHourlyDaily(d.dates || [], d.values || [])
        })
      }
    },
    // 保留单独方法供 switchPeriod 等按需调用
    async loadHourly() {
      if (this.activePeriod === 'day') {
        const today = getMetricToday()
        const rows = await fetchMetricData(() => getHourlyBloodOxygen(today, today), [])
        const vals = createHourlySeries(rows, 'avgBloodOxygen')
        this.$nextTick(() => this.renderHourly(vals))
      } else {
        const days = metricDaysForPeriod(this.activePeriod)
        const d = await fetchMetricData(() => getBloodOxygenTrend(days), {})
        const dates = d.dates || []
        const vals = d.values || []
        this.$nextTick(() => this.renderHourlyDaily(dates, vals))
      }
    },
    async loadTrend() {
      if (this.activePeriod === 'day') {
        const today = getMetricToday()
        const rows = await fetchMetricData(() => getHourlyBloodOxygen(today, today), [])
        const vals = createHourlySeries(rows, 'avgBloodOxygen')
        this.$nextTick(() => this.initTrendDay(vals))
      } else {
        const days = metricDaysForPeriod(this.activePeriod)
        const d = await fetchMetricData(() => getBloodOxygenTrend(days), {})
        this.$nextTick(() => this.initTrend(d))
      }
    },
    async loadRealtime() {
      await loadMetricRealtime(this, getRealtimeBloodOxygen, 200)
    },
    boLevel: spo2Level,

    // setPageSize → chartPageMixin
  }
}
</script>

<style lang="scss" scoped src="./blood-oxygen.scss"></style>
