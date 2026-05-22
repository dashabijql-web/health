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
import { spo2Level } from '@/constants/health-thresholds'
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
    this.initAutoPageSize(27)
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
      // loadTrendAndHourly 合并两个原本各自调用 trend 接口的方法，消除重复请求
      await Promise.allSettled([
        this.loadOverview(), this.loadTopUsers(), this.loadDept(),
        this.loadAge(), this.loadDist(), this.loadTrendAndHourly(), this.loadRealtime()
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
    async loadAge() {
      await loadMetricRangeChart(this, getAgeBloodOxygen, 'initAge')
    },
    async loadDist() {
      await loadMetricDistribution(this, getBloodOxygenDistribution, 'initDist')
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
    fmtRtTime(ts) {
      return ts ? dayjs(ts).format('HH:mm:ss') : ''
    },

    boLevel: spo2Level,

    // setPageSize → chartPageMixin
  }
}
</script>

<style lang="scss" scoped src="./blood-oxygen.scss"></style>
