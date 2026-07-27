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
      <button class="hm-export-btn" @click="exportExcel" title="导出当前数据">导出</button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="hr-bd" v-loading="pageLoading" element-loading-text="数据加载中..." element-loading-background="rgba(10,20,40,0.7)">

      <!-- ─ 左侧：TOP5(小) + 部门统计(大) ─ -->
      <aside class="hr-aside">
        <!-- TOP5：紧凑列表替代大图表 -->
        <div class="hr-panel hr-aside-top">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">{{ metricPeriodLabel }}异常频次 Top 10</span>
          </div>
          <div class="hr-top5-list" ref="top5ScrollRef"
               @mouseenter="_top5Paused=true" @mouseleave="_top5Paused=false">
            <div v-if="!top5Data.length" class="hr-top5-empty">暂无异常频次数据</div>
            <div class="hr-top5-row" v-for="(item, i) in displayedTop5" :key="i" role="button" tabindex="0" @click="goToPortrait(item)" @keydown.enter="goToPortrait(item)" style="cursor:pointer">
              <span class="hr-top5-rank" :class="i < 3 ? 'rank-'+(i+1) : 'rank-n'">{{ i+1 }}</span>
              <span class="hr-top5-name">{{ item.userName }}</span>
              <div class="hr-top5-bar-wrap">
                <div class="hr-top5-bar" :style="{width: (item.count / top5Max * 100) + '%'}"></div>
              </div>
              <span class="hr-top5-val">{{ item.count }}</span>
              <span class="hr-top5-days" v-if="item.anomalyDays">{{ item.anomalyDays }}天</span>
            </div>
          </div>
        </div>

        <!-- 部门统计：占剩余全部空间 -->
        <div class="hr-panel hr-aside-bot">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">{{ metricPeriodLabel }}部门异常记录数</span>
            <span v-if="filterDept" class="hr-dept-tag" @click="filterDept=''" title="点击取消筛选">{{ filterDept }} ×</span>
          </div>
          <div class="hr-pc">
            <div ref="deptRef" style="width:100%;height:100%"></div>
          </div>
        </div>
      </aside>

      <!-- ─ 主区域 ─ -->
      <main class="hr-main">

        <!-- 当前人员口径 + 4 区间卡 -->
        <div class="hr-hero">
          <div class="hr-scope-card">
            <span class="hr-scope-label">当前覆盖人员</span>
            <strong class="hr-scope-value">{{ realtimeList.length }}<em>人</em></strong>
            <span class="hr-scope-note">近2小时每人最新一条</span>
            <span class="hr-scope-time">更新于 {{ latestRealtimeText }}</span>
          </div>
          <div class="hr-zone-cards">
            <div v-for="z in hrZones" :key="z.key" :class="['hr-zone-card', z.cls]">
              <span class="hr-zone-icon" :style="{color: z.color}">{{ z.icon }}</span>
              <span class="hr-zone-count" :style="{color: z.color}">{{ z.count }}<em>人</em></span>
              <span class="hr-zone-label">{{ z.label }}</span>
              <span class="hr-zone-range">{{ z.range }}</span>
              <div class="hr-zone-pct-bar">
                <div class="hr-zone-pct-fill" :style="{ width: z.pct + '%', background: z.color }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 图表行：趋势 + 小时波动 -->
        <div class="hr-charts-row">
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
          <div v-if="activePeriod !== 'day'" class="hr-panel hr-panel-hourly">
            <div class="hr-ph">
              <span class="hr-ph-bar"></span>
              <span class="hr-ph-title">{{ hourlyTitle }}</span>
            </div>
            <div class="hr-pc">
              <div ref="hourlyRef" style="width:100%;height:100%"></div>
            </div>
          </div>
        </div>

        <!-- 当前异常心率明细 -->
        <div class="hr-panel hr-panel-anomaly">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">当前异常心率人员</span>
            <span class="hr-anomaly-count" v-if="anomalyList.length">
              共 <em>{{ anomalyList.length }}</em> 人异常
            </span>
          </div>
          <div v-if="!anomalyList.length" class="hr-anomaly-empty">
            当前无异常心率人员
          </div>
          <div v-else class="hr-anomaly-body">
            <div class="hr-anomaly-hd">
              <span>姓名</span><span>性别/年龄</span><span>部门</span><span>工种</span><span>心率</span><span>类型</span><span>时间</span>
            </div>
            <div class="hr-anomaly-list">
              <div
                class="hr-anomaly-row"
                v-for="(item, i) in displayedAnomalyList"
                :key="i"
                role="button"
                tabindex="0"
                :class="item.heartRate > 120 ? 'anom-high' : 'anom-low'"
                @click="showDetail(item)"
                @keydown.enter="showDetail(item)"
                style="cursor:pointer"
              >
                <span class="ha-name">{{ item.userName }}</span>
                <span class="ha-gender">
                  <em :class="item.gender === '男' ? 'g-m' : 'g-f'">{{ item.gender || '--' }}</em>
                  <i v-if="item.age">{{ item.age }}岁</i>
                </span>
                <span class="ha-dept">{{ item.deptName || item.dept_name || '--' }}</span>
                <span class="ha-job">{{ item.jobType || '--' }}</span>
                <span class="ha-val">{{ item.heartRate }} bpm</span>
                <span class="ha-type">{{ item.heartRate > 120 ? '偏高↑' : '偏低↓' }}</span>
                <span class="ha-time">{{ fmtTime(item.recordTime) }}</span>
              </div>
              <button v-if="anomalyList.length > 20" class="hr-anomaly-more" @click="anomalyExpanded = !anomalyExpanded">
                {{ anomalyExpanded ? '收起' : `展开全部 (${anomalyList.length} 人)` }}
              </button>
            </div>
          </div>
        </div>

      </main>

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
import dayjs from 'dayjs'
import {
  getHeartRateOverview,
  getHeartRateTrend,
  getRealtimeHeartRate,
  getHeartRateTopUsers,
  getHeartRateDeptStats,
  getHourlyHeartRate,
  getDailyAnomalyHeartRate
} from '@/api/heart-rate'
import { hrLevel } from '@/constants/health-thresholds'
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
import { heartRateChartMethods } from './heart-rate-chart'

export default {
  name: 'HeartRateAnalysis',
  mixins: [chartPageMixin, metricPageMixin],
  data() {
    return {
      pageLoading: false,
      currentTime: '',
      overview: {
        avgHeartRate: 0, minHeartRate: 0, maxHeartRate: 0,
        detectionRate: 0, abnormalCount: 0, totalCount: 0
      },
      top5Data: [],
      anomalyExpanded: false,
      realtimeList: [],
      activePeriod: 'month',
      periodOptions: PERIOD_OPTIONS,
      charts: {},
      detailItem: null,
      detailVisible: false,
      filterDept: '',
      _top5Paused: false,
      _top5ScrollLoop: null
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
    hourlyTitle() {
      return { day: '今日24小时波动', week: '近7日异常人数', month: '近30日异常人数' }[this.activePeriod]
    },
    trendTitle() {
      return { day: '今日心率趋势', week: '近7天心率趋势', month: '近30天心率趋势' }[this.activePeriod]
    },
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.count)) : 1
    },
    displayedTop5() {
      return this.top5Data.slice(0, 10)
    },
    anomalyList() {
      return this.filteredRealtimeList.filter(x => x.heartRate > 120 || x.heartRate < 55)
    },
    displayedAnomalyList() {
      return this.anomalyExpanded ? this.anomalyList : this.anomalyList.slice(0, 20)
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
        { key: 'normal',   label: '正常', range: '55–120 bpm',   count: normal,   pct: pct(normal),   color: '#52c41a', icon: 'OK', cls: 'zone-normal'   },
        { key: 'elevated', label: '偏高', range: '121–150 bpm',  count: elevated, pct: pct(elevated), color: '#FFB84D', icon: '↑', cls: 'zone-elevated' },
        { key: 'danger',   label: '危险', range: '> 150 bpm',    count: danger,   pct: pct(danger),   color: '#ff5252', icon: 'ALERT', cls: 'zone-danger'   }
      ]
    }
  },
  mounted() {
    this.initPage()
    this.startTop5Scroll()
  },
  methods: {
    ...heartRateChartMethods,

    getTop5ScrollOptions() {
      return {
        intervalMs: 80,
        shouldScroll: () => !this._top5Paused
      }
    },

    async exportExcel() {
      const list = this.realtimeList
      const cols = [
        { label: '姓名', key: 'userName' },
        { label: '部门', key: 'deptName' },
        { label: '工号', key: 'empCode' },
        { label: '心率(bpm)', key: 'heartRate' },
        { label: '状态', key: 'status' },
        { label: '记录时间', key: 'recordTime' }
      ]
      await exportMetricRows({
        rows: list,
        columns: cols,
        filenamePrefix: '心率分析',
        mapRow: r => ({
          userName: r.userName || '--',
          deptName: r.deptName || '--',
          empCode: r.empCode || '--',
          heartRate: r.heartRate ?? '--',
          status: (r.heartRate && (r.heartRate < 55 || r.heartRate > 120)) ? '异常' : '正常',
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
        this.activePeriod === 'day' ? Promise.resolve() : this.loadHourly(),
        this.loadRealtime()
      ])
    },

    async loadOverview() {
      await loadMetricOverview(this, getHeartRateOverview, this.overview)
    },
    async loadTopUsers() {
      await loadMetricTopUsers(this, getHeartRateTopUsers)
    },
    async loadDept() {
      await loadMetricRangeChart(this, getHeartRateDeptStats, 'initDept')
    },
    async loadHourly() {
      const { startDate, endDate } = this.periodRange
      const rows = await fetchMetricData(() => getDailyAnomalyHeartRate(startDate, endDate), [])
      const dates = rows.map(x => x.date)
      const counts = rows.map(x => x.anomalyCount)
      this.$nextTick(() => this.renderDailyAnomaly(dates, counts))
    },
    async loadTrend() {
      if (this.activePeriod === 'day') {
        const today = getMetricToday()
        const rows = await fetchMetricData(() => getHourlyHeartRate(today, today), [])
        const vals = createHourlySeries(rows, 'avgHeartRate')
        this.$nextTick(() => this.initTrendDay(vals))
      } else {
        const days = metricDaysForPeriod(this.activePeriod)
        const d = await fetchMetricData(() => getHeartRateTrend(days), {})
        this.$nextTick(() => this.initTrend(d))
      }
    },
    async loadRealtime() {
      await loadMetricRealtime(this, getRealtimeHeartRate)
    },
    fmtRtTime(ts) {
      return ts ? dayjs(ts).format('HH:mm:ss') : ''
    },

    hrLevel
  }
}
</script>

<style lang="scss" scoped src="./heart-rate.scss"></style>
