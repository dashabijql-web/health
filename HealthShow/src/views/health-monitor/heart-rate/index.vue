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
      <button class="hm-export-btn" @click="exportExcel" title="导出当前数据">⬇ 导出</button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="hr-bd" v-loading="pageLoading" element-loading-text="数据加载中..." element-loading-background="rgba(10,20,40,0.7)">

      <!-- ─ 左侧：TOP5(小) + 部门统计(大) ─ -->
      <aside class="hr-aside">
        <!-- TOP5：紧凑列表替代大图表 -->
        <div class="hr-panel hr-aside-top">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">异常频次排行</span>
          </div>
          <div class="hr-top5-list" ref="top5ScrollRef"
               @mouseenter="_top5Paused=true" @mouseleave="_top5Paused=false">
            <div v-if="!top5Data.length" class="hr-top5-empty">暂无异常频次数据</div>
            <div class="hr-top5-row" v-for="(item, i) in displayedTop5" :key="i" @click="goToPortrait(item)" style="cursor:pointer">
              <span class="hr-top5-rank" :class="i < 3 ? 'rank-'+(i+1) : 'rank-n'">{{ i+1 }}</span>
              <span class="hr-top5-name">{{ item.userName }}</span>
              <div class="hr-top5-bar-wrap">
                <div class="hr-top5-bar" :style="{width: (item.count / top5Max * 100) + '%'}"></div>
              </div>
              <span class="hr-top5-val">{{ item.count }}</span>
              <span class="hr-top5-days" v-if="item.anomalyDays">{{ item.anomalyDays }}天</span>
            </div>
            <div v-if="top5Data.length > 20" class="hr-top5-more" @click="top5Expanded = !top5Expanded">
              {{ top5Expanded ? '▲ 收起' : `▼ 展开全部 (${top5Data.length} 条)` }}
            </div>
          </div>
        </div>

        <!-- 部门统计：占剩余全部空间 -->
        <div class="hr-panel hr-aside-bot">
          <div class="hr-ph">
            <span class="hr-ph-bar"></span>
            <span class="hr-ph-title">部门心率异常统计</span>
            <span v-if="filterDept" class="hr-dept-tag" @click="filterDept=''" title="点击取消筛选">{{ filterDept }} ×</span>
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


        <!-- 心率区间统计 -->
        <div class="hr-zone-row">
          <div v-for="z in hrZones" :key="z.key" :class="['hr-zone-card', z.cls]">
            <div class="hr-zone-top">
              <span class="hr-zone-label">{{ z.icon }} {{ z.label }}</span>
              <span class="hr-zone-range">{{ z.range }}</span>
            </div>
            <span class="hr-zone-count">{{ z.count }}<em>人</em></span>
            <div class="hr-zone-bar">
              <div class="hr-zone-fill" :style="{ width: z.pct + '%', background: z.color }"></div>
            </div>
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
              <span>姓名</span><span>性别/年龄</span><span>部门</span><span>工种</span><span>心率</span><span>类型</span><span>时间</span>
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
            <button class="hr-export-btn" @click="exportExcel" title="导出Excel">⬇ 导出</button>
          </div>

          <div class="hr-rt-hd">
            <span>#</span><span>姓名</span><span>心率</span><span>状态</span><span>时间</span>
          </div>

          <div class="hr-rt-body" ref="listRef">
            <div
              class="hr-rt-row"
              v-for="(item, i) in sortedRealtimeList"
              :key="i"
              :class="hrLevel(item.heartRate)"
              @click="showDetail(item)"
              style="cursor:pointer"
            >
              <span class="hr-rt-idx">{{ i + 1 }}</span>
              <span class="hr-rt-name">{{ item.userName }}</span>
              <span class="hr-rt-val">
                {{ item.heartRate }}
                <em v-if="item.heartRate > 120" class="hr-rt-arrow">↑</em>
                <em v-else-if="item.heartRate < 55" class="hr-rt-arrow">↓</em>
              </span>
              <span class="hr-rt-badge" :class="hrLevel(item.heartRate)">
                {{ item.heartRate > 120 ? '偏高' : item.heartRate < 55 ? '偏低' : '正常' }}
              </span>
              <span class="hr-rt-time">{{ fmtRtTime(item.recordTime) }}</span>
            </div>
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
import dayjs from 'dayjs'
import {
  getHeartRateOverview,
  getHeartRateTrend,
  getAgeHeartRate,
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
      top5Expanded: false,

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
      return { day: '今日24小时波动', week: '近7日异常人数', month: '近30日异常人数' }[this.activePeriod]
    },
    trendTitle() {
      return { day: '今日心率趋势', week: '近7天心率趋势', month: '近30天心率趋势' }[this.activePeriod]
    },
    top5Max() {
      return this.top5Data.length ? Math.max(...this.top5Data.map(x => x.count)) : 1
    },
    displayedTop5() {
      const limit = this.top5Expanded ? this.top5Data.length : 20
      return this.top5Data.slice(0, limit)
    },
    sortedRealtimeList() {
      return [...this.filteredRealtimeList].sort((a, b) => {
        const aAbnormal = a.heartRate && (a.heartRate > 120 || a.heartRate < 55) ? 1 : 0
        const bAbnormal = b.heartRate && (b.heartRate > 120 || b.heartRate < 55) ? 1 : 0
        return bAbnormal - aAbnormal
      })
    },
    anomalyList() {
      return this.filteredRealtimeList.filter(x => x.heartRate > 120 || x.heartRate < 55)
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
  mounted() {
    this.initPage()
    this.startTop5Scroll()
    this.initAutoPageSize(27)
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
        this.loadAge(),
        this.loadTrend(),
        this.loadHourly(),
        this.loadRealtime()
      ])
    },

    async loadOverview() {
      await loadMetricOverview(this, getHeartRateOverview, this.overview)
      this.$nextTick(() => this.initGauge())
    },
    async loadTopUsers() {
      await loadMetricTopUsers(this, getHeartRateTopUsers)
    },
    async loadDept() {
      await loadMetricRangeChart(this, getHeartRateDeptStats, 'initDept')
    },
    async loadAge() {
      await loadMetricRangeChart(this, getAgeHeartRate, 'initAge')
    },
    async loadHourly() {
      if (this.activePeriod === 'day') {
        const today = getMetricToday()
        const rows = await fetchMetricData(() => getHourlyHeartRate(today, today), [])
        const vals = createHourlySeries(rows, 'avgHeartRate')
        this.$nextTick(() => this.renderHourly(vals))
      } else {
        const { startDate, endDate } = this.periodRange
        const rows = await fetchMetricData(() => getDailyAnomalyHeartRate(startDate, endDate), [])
        const dates = rows.map(x => x.date)
        const counts = rows.map(x => x.anomalyCount)
        this.$nextTick(() => this.renderDailyAnomaly(dates, counts))
      }
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

    hrLevel,

    // setPageSize(27) → chartPageMixin（公式：floor(clientHeight / 27), min 10）
  }
}
</script>

<style lang="scss" scoped src="./heart-rate.scss"></style>
