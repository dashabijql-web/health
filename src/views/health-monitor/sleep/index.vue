<template>
  <div class="sl-root">

    <!-- ══ Header ══ -->
    <header class="sl-hd">
      <div class="sl-hd-left">
        <span class="sl-live-dot"></span>
        <h1 class="sl-hd-title">睡眠分析</h1>
        <span class="sl-hd-date">{{ yesterdayDate }} 昨夜汇总</span>
      </div>
      <div class="sl-hd-kpis">
        <div class="sl-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="sl-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="sl-kpi-l">{{ k.label }}</span>
        </div>
      </div>
      <div class="sl-hd-time">{{ currentTime }}</div>
    </header>

    <!-- ══ Body ══ -->
    <section class="sl-bd">

      <!-- ─ 左侧：4个概况卡 + 睡眠阶段环图 + 评分分布柱图 ─ -->
      <aside class="sl-aside">

        <!-- 2×2 概况指标卡 -->
        <div class="sl-stat-grid">
          <div class="sl-stat-card" v-for="c in statCards" :key="c.label" :style="{'--c': c.color}">
            <div class="sl-stat-icon" :style="{background: c.bg}">
              <span :style="{color: c.color}">{{ c.icon }}</span>
            </div>
            <div class="sl-stat-body">
              <div class="sl-stat-val" :style="{color: c.color}">{{ c.val }}</div>
              <div class="sl-stat-label">{{ c.label }}</div>
            </div>
          </div>
        </div>

        <!-- 睡眠阶段占比 -->
        <div class="sl-panel sl-stage-panel">
          <div class="sl-ph">
            <span class="sl-ph-bar"></span>
            <span class="sl-ph-title">昨夜睡眠阶段分布</span>
          </div>
          <div class="sl-stage-body">
            <div ref="stageRef" class="sl-stage-chart"></div>
            <div class="sl-stage-legend">
              <div class="sl-stage-row" v-for="s in stageLegend" :key="s.name">
                <div class="sl-stage-dot" :style="{background: s.color}"></div>
                <span class="sl-stage-name">{{ s.name }}</span>
                <div class="sl-stage-bar-wrap">
                  <div class="sl-stage-bar" :style="{width: s.value+'%', background: s.color}"></div>
                </div>
                <span class="sl-stage-pct" :style="{color: s.color}">{{ s.value }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 质量评分分布 -->
        <div class="sl-panel sl-score-panel">
          <div class="sl-ph">
            <span class="sl-ph-bar"></span>
            <span class="sl-ph-title">睡眠质量评分分布</span>
          </div>
          <div class="sl-pc">
            <div ref="scoreRef" style="width:100%;height:100%"></div>
          </div>
        </div>

        <!-- 昨日睡眠异常预警 -->
        <div class="sl-panel sl-alert-panel">
          <div class="sl-ph">
            <span class="sl-ph-bar" style="background:linear-gradient(180deg,#ff5252,rgba(255,82,82,0.3))"></span>
            <span class="sl-ph-title">昨日睡眠异常预警</span>
            <span class="sl-alert-count">{{ alertList.length }} 人</span>
          </div>
          <div class="sl-alert-body">
            <div v-if="!alertList.length" class="sl-alert-empty">昨夜无睡眠异常预警</div>
            <div class="sl-alert-row" v-for="(a, i) in alertList" :key="i" :class="a.level">
              <span class="sl-alert-name">{{ a.name }}</span>
              <span class="sl-alert-tag" :class="a.level">{{ a.tag }}</span>
              <span class="sl-alert-val">{{ a.val }}</span>
              <span class="sl-alert-desc">{{ a.desc }}</span>
            </div>
          </div>
        </div>

      </aside>

      <!-- ─ 中间：趋势(大) + 三列小图 ─ -->
      <main class="sl-main">

        <!-- 近30天趋势：睡眠时长柱 + 评分折线 双轴 -->
        <div class="sl-panel sl-trend-panel">
          <div class="sl-ph">
            <span class="sl-ph-bar"></span>
            <span class="sl-ph-title">近30天睡眠趋势</span>
            <div class="sl-trend-tags">
              <span class="sl-tag" style="color:#a78bfa;border-color:rgba(167,139,250,0.3)">▌ 睡眠时长(h)</span>
              <span class="sl-tag" style="color:#52c41a;border-color:rgba(82,196,26,0.3)">── 质量评分</span>
              <span class="sl-tag" style="color:#FFB84D;border-color:rgba(255,184,77,0.3)">- - 建议时长(7h)</span>
            </div>
          </div>
          <div class="sl-pc">
            <div ref="trendRef" style="width:100%;height:100%"></div>
          </div>
        </div>

        <!-- 上下两层布局 -->
        <div class="sl-mid-row">

          <!-- 上层：睡眠时长 + 入睡时间（高度约半） -->
          <div class="sl-mid-top">

            <!-- 睡眠时长分布 -->
            <div class="sl-panel sl-duration-panel">
              <div class="sl-ph">
                <span class="sl-ph-bar"></span>
                <span class="sl-ph-title">睡眠时长分布</span>
                <span class="sl-ph-sub">{{ overview.totalCount || 0 }} 人</span>
              </div>
              <div class="sl-dur-segbar">
                <div class="sl-dur-seg" v-for="(d,i) in durationLegend" :key="d.name"
                  :style="{flex:d.value, background:d.color,
                    borderRadius: i===0?'5px 0 0 5px': i===durationLegend.length-1?'0 5px 5px 0':'0'}">
                </div>
              </div>
              <div class="sl-dur-seglabels">
                <span class="sl-dur-seglabel" v-for="d in durationLegend" :key="d.name" :style="{flex:d.value}">
                  <em :style="{background:d.color}"></em>{{ d.name }} {{ d.value }}%
                </span>
              </div>
              <div class="sl-dur-body">
                <div ref="durationRef" class="sl-dur-chart"></div>
                <div class="sl-dur-tips">
                  <div class="sl-dur-tip-title">时长健康参考</div>
                  <div class="sl-dur-tip-row" v-for="t in durationTips" :key="t.label">
                    <span class="sl-dur-tip-dot" :style="{background: t.color}"></span>
                    <span class="sl-dur-tip-name" :style="{color: t.color}">{{ t.label }}</span>
                    <span class="sl-dur-tip-desc">{{ t.desc }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 入睡时间分布 -->
            <div class="sl-panel sl-bedtime-panel">
              <div class="sl-ph">
                <span class="sl-ph-bar"></span>
                <span class="sl-ph-title">入睡时间分布</span>
                <span class="sl-ph-sub" style="color:#4a6080">暂无入睡时刻数据</span>
              </div>
              <div class="sl-bed-body">
                <div ref="bedtimeRef" class="sl-bed-chart"></div>
                <div class="sl-bed-tips">
                  <div class="sl-bed-tip-title">入睡时间与健康关联</div>
                  <div class="sl-bed-tip-row" v-for="b in bedtimeTips" :key="b.time">
                    <span class="sl-bed-tip-icon" :style="{color: b.color}">{{ b.icon }}</span>
                    <span class="sl-bed-tip-time" :style="{color: b.color}">{{ b.time }}</span>
                    <span class="sl-bed-tip-desc">{{ b.desc }}</span>
                  </div>
                  <div class="sl-bed-warn" style="color:#4a6080">
                    <span class="sl-bed-warn-dot" style="background:#4a6080"></span>
                    <span>设备暂未采集入睡时刻，分布图不可用</span>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <!-- 下层：各部门数据上传率（全宽横向，动态从API获取所有部门）-->
          <div class="sl-panel sl-dept-panel">
            <div class="sl-ph">
              <span class="sl-ph-bar"></span>
              <span class="sl-ph-title">各部门数据上传率</span>
              <span class="sl-ph-sub">共 {{ deptUploadList.length }} 个部门</span>
            </div>
            <div class="sl-pc">
              <div ref="deptRef" style="width:100%;height:100%"></div>
            </div>
          </div>

        </div>

      </main>

      <!-- ─ 右侧：数据明细 ─ -->
      <div class="sl-rtlist">
        <div class="sl-panel" style="height:100%;display:flex;flex-direction:column;overflow:hidden">
          <div class="sl-ph">
            <span class="sl-ph-bar"></span>
            <span class="sl-ph-title">数据明细</span>
            <span class="sl-rt-total">{{ detailList.length }} 条</span>
          </div>
          <div class="sl-rt-hd">
            <span>姓名</span><span>时长</span><span>评分</span><span>评级</span><span>时间</span>
          </div>
          <div class="sl-rt-body" ref="listRef">
            <div
              class="sl-rt-row"
              v-for="(item, i) in pagedList"
              :key="i"
              :class="item.level"
              style="cursor:pointer"
              @click="openRecordDialog(item)"
            >
              <span class="sl-rt-name">{{ item.userName }}</span>
              <span class="sl-rt-dur">{{ item.sleepHours }}</span>
              <span class="sl-rt-score" :class="scoreClass(item.score)">{{ item.score }}</span>
              <span class="sl-rt-badge" :class="item.level">{{ item.levelText }}</span>
              <span class="sl-rt-time">{{ fmtTime(item.recordTime) }}</span>
            </div>
          </div>
          <div class="sl-rt-pg">
            <button class="sl-pg-btn" :disabled="currentPage===1" @click="currentPage=1">首页</button>
            <button class="sl-pg-btn" :disabled="currentPage===1" @click="currentPage--">‹</button>
            <span class="sl-pg-info">{{ currentPage }} / {{ totalPages }}</span>
            <button class="sl-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage++">›</button>
            <button class="sl-pg-btn" :disabled="currentPage>=totalPages" @click="currentPage=totalPages">末页</button>
          </div>
        </div>
      </div>

    </section>
  </div>

  <!-- ══ 睡眠记录详情弹窗 ══ -->
  <el-dialog
    v-model="recordDialog.visible"
    title="睡眠记录详情"
    width="420px"
    :append-to-body="true"
    class="sl-record-dialog"
  >
    <div v-if="recordDialog.item" class="sl-rd-body">
      <div class="sl-rd-row"><span class="sl-rd-key">姓名</span><span class="sl-rd-val">{{ recordDialog.item.userName }}</span></div>
      <div class="sl-rd-row"><span class="sl-rd-key">睡眠时长</span><span class="sl-rd-val" style="color:#a78bfa">{{ recordDialog.item.sleepHours }}</span></div>
      <div class="sl-rd-row"><span class="sl-rd-key">质量评分</span>
        <span class="sl-rd-val" :class="scoreClass(recordDialog.item.score)" style="font-weight:700">{{ recordDialog.item.score }} 分</span>
      </div>
      <div class="sl-rd-row"><span class="sl-rd-key">评级</span>
        <span class="sl-rt-badge" :class="recordDialog.item.level" style="font-size:12px;padding:2px 8px">{{ recordDialog.item.levelText }}</span>
      </div>
      <div class="sl-rd-row"><span class="sl-rd-key">记录时间</span><span class="sl-rd-val">{{ fmtTime(recordDialog.item.recordTime) }}</span></div>
      <div class="sl-rd-row" v-if="recordDialog.item.deepSleep != null">
        <span class="sl-rd-key">深睡占比</span><span class="sl-rd-val" style="color:#4FC3F7">{{ recordDialog.item.deepSleep }}%</span>
      </div>
      <div class="sl-rd-row" v-if="recordDialog.item.remSleep != null">
        <span class="sl-rd-key">REM 占比</span><span class="sl-rd-val" style="color:#a78bfa">{{ recordDialog.item.remSleep }}%</span>
      </div>
      <div class="sl-rd-row" v-if="recordDialog.item.lightSleep != null">
        <span class="sl-rd-key">浅睡占比</span><span class="sl-rd-val" style="color:#52c41a">{{ recordDialog.item.lightSleep }}%</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="recordDialog.visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getSleepPageData, getSleepTrend, getSleepQualityDistribution } from '@/api/sleep'
import { getDepartmentList } from '@/api/department'

export default {
  name: 'SleepAnalysis',
  data() {
    return {
      currentTime: '',
      yesterdayDate: '',
      overview: {
        uploadRate: 0, greenLineRate: 0,
        avgSleepTime: '--', avgScore: 0, totalCount: 0
      },
      stageLegend: [],
      durationLegend: [],
      durationTips: [
        { label: '<4小时',  color: '#ff5252', desc: '严重不足，影响认知' },
        { label: '4-6小时', color: '#FFB84D', desc: '偏少，易疲劳' },
        { label: '6-8小时', color: '#4FC3F7', desc: '建议范围' },
        { label: '>8小时',  color: '#52c41a', desc: '充足，状态最佳' }
      ],
      bedtimeTips: [
        { time: '21-22时', icon: '★', color: '#52c41a', desc: '最佳入睡时间' },
        { time: '22-23时', icon: '✓', color: '#4FC3F7', desc: '良好，顺应生物钟' },
        { time: '23-24时', icon: '!', color: '#FFB84D', desc: '偏晚，影响深睡' },
        { time: '0时以后', icon: '✕', color: '#ff5252', desc: '过晚，损害健康' }
      ],
      lateBedPct: 5,
      deptUploadList: [],
      alertList: [],
      detailList: [],
      currentPage: 1,
      pageSize: 20,
      charts: {},
      clockTimer: null, refreshTimer: null, scrollTimer: null, resizeTimer: null,
      recordDialog: { visible: false, item: null }
    }
  },
  computed: {
    headerKpis() {
      const o = this.overview
      return [
        { label: '数据上传率',   val: (o.uploadRate    || 0) + '%', cls: 'kpi-cyan'   },
        { label: '绿线达标率',   val: (o.greenLineRate || 0) + '%', cls: 'kpi-green'  },
        { label: '平均睡眠时长', val: o.avgSleepTime   || '--',     cls: 'kpi-purple' },
        { label: '平均质量评分', val: (o.avgScore      || 0) + '分',cls: 'kpi-amber'  }
      ]
    },
    statCards() {
      const o = this.overview
      return [
        { label: '数据上传率',   val: (o.uploadRate    || 0) + '%', icon: '↑', color: '#00d4ff', bg: 'rgba(0,212,255,0.12)'   },
        { label: '绿线达标率',   val: (o.greenLineRate || 0) + '%', icon: '✓', color: '#52c41a', bg: 'rgba(82,196,26,0.12)'   },
        { label: '平均睡眠时长', val: o.avgSleepTime   || '--',     icon: '⏱', color: '#a78bfa', bg: 'rgba(167,139,250,0.12)' },
        { label: '平均质量评分', val: (o.avgScore      || 0) + '分',icon: '★', color: '#FFB84D', bg: 'rgba(255,184,77,0.12)'  }
      ]
    },
    pagedList() {
      const s = (this.currentPage - 1) * this.pageSize
      return this.detailList.slice(s, s + this.pageSize)
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.detailList.length / this.pageSize))
    }
  },
  mounted() {
    this.initClock()
    this.yesterdayDate = dayjs().subtract(1, 'day').format('MM月DD日')
    this.fetchData()
    
    window.addEventListener('resize', this.handleResize)
    this.$nextTick(() => {
      this.startAutoScroll()
      this.initBedtime()
    })
    this.refreshTimer = setInterval(() => this.fetchData(), 60000)
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
        this.loadPageData(),
        this.loadTrend(),
        this.loadQualityDist(),
        this.loadDept()
      ])
    },

    // 从 /department/list 拿全量部门，再和 page-data 的 deptUpload 合并显示上传率
    async loadDept() {
      let depts = []
      let uploadMap = {}
      try {
        const r = await getDepartmentList()
        if (r.code === 200 && r.data) {
          // 兼容 list/rows/records 等不同返回格式
          depts = r.data.list || r.data.rows || r.data.records || r.data || []
        }
      } catch {}
      // 同时尝试从 page-data 的 deptUpload 取上传率
      try {
        const r2 = await getSleepPageData()
        if (r2.code === 200 && r2.data?.deptUpload?.length) {
          r2.data.deptUpload.forEach(d => { uploadMap[d.deptName] = d.count })
        }
      } catch {}
      // 组合：用部门列表作为基础，有上传率就用真实值，没有就用随机模拟值
      const list = depts.length > 0
        ? depts.map(d => {
            const name = d.deptName || d.name || d.label || ''
            const realCount = uploadMap[name]
            return { deptName: name, count: realCount != null ? realCount : 0 }
          }).filter(d => d.deptName)
        : null  // 无部门数据时用 fallback
      this.$nextTick(() => this.initDept(list))
    },

    async loadPageData() {
      try {
        const r = await getSleepPageData()
        if (r.code === 200 && r.data) {
          const d = r.data
          if (d.overview)       this.overview      = { ...this.overview, ...d.overview }
          if (d.durationLegend?.length) this.durationLegend = d.durationLegend
          if (d.categoryLegend?.length) this.stageLegend    = d.categoryLegend
          if (d.detailList?.length)     this.detailList     = d.detailList
          if (d.deptUpload?.length)     this.deptUploadList = d.deptUpload
          this.$nextTick(() => {
            this.initStage()
            this.initDuration()
            this.initDept(d.deptUpload || [])
          })
          return
        }
      } catch {}
      // fallback: render with default data
      this.$nextTick(() => {
        this.initStage()
        this.initDuration()
        this.initDept([])
      })
    },

    async loadTrend() {
      let d = {}
      try { const r = await getSleepTrend(30); if (r.code === 200) d = r.data || {} } catch {}
      this.$nextTick(() => this.initTrend(d))
    },

    async loadQualityDist() {
      let d = []
      try { const r = await getSleepQualityDistribution(); if (r.code === 200) d = r.data || [] } catch {}
      this.$nextTick(() => this.initScore(d))
    },

    // ── 睡眠阶段环形图 ──
    initStage() {
      const el = this.$refs.stageRef; if (!el) return
      if (this.charts.stage) this.charts.stage.dispose()
      const c = echarts.init(el); this.charts.stage = c
      c.setOption({
        backgroundColor: 'transparent',
        series: [{
          type: 'pie', radius: ['50%', '78%'], center: ['50%', '50%'],
          label: { show: false }, labelLine: { show: false },
          cursor: 'pointer',
          data: this.stageLegend.map(x => ({
            value: x.value, name: x.name,
            itemStyle: { color: x.color, borderRadius: 3, shadowColor: x.color + '55', shadowBlur: 8 }
          }))
        }]
      })
      c.on('click', params => {
        this.$message && this.$message.info(`${params.name}：${params.value}%`)
      })
    },

    // ── 质量评分分布柱图 ──
    initScore(data) {
      const el = this.$refs.scoreRef; if (!el) return
      if (this.charts.score) this.charts.score.dispose()
      const c = echarts.init(el); this.charts.score = c
      const d = data.length ? data : [
        { label: '差(0-40)',    count: 0, color: '#ff5252' },
        { label: '较差(40-60)', count: 0, color: '#FFB84D' },
        { label: '良好(60-80)', count: 0, color: '#4FC3F7' },
        { label: '优秀(80+)',   count: 0, color: '#52c41a' }
      ]
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,0.9)', borderColor: 'rgba(167,139,250,0.3)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: p => `${p[0].name}<br/>人数：<b style="color:#a78bfa">${p[0].value}</b> 人`
        },
        grid: { left: '2%', right: '4%', top: '8%', bottom: '16%', containLabel: true },
        xAxis: {
          type: 'category', data: d.map(x => x.label),
          axisLine: { lineStyle: { color: 'rgba(167,139,250,0.15)' } }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 9 }
        },
        yAxis: {
          type: 'value', axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(167,139,250,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 9 }
        },
        series: [{
          type: 'bar', barWidth: '55%',
          cursor: 'pointer',
          data: d.map(x => ({
            value: x.count,
            itemStyle: { color: x.color, borderRadius: [4,4,0,0], shadowColor: x.color+'44', shadowBlur: 6 }
          })),
          label: { show: true, position: 'top', color: '#a8c5e6', fontSize: 9 }
        }]
      })
      c.on('click', params => {
        this.$message && this.$message.info(`${params.name}：${params.value} 人`)
      })
    },

    // ── 近30天趋势（时长柱 + 评分折线）──
    initTrend(data) {
      const el = this.$refs.trendRef; if (!el) return
      if (this.charts.trend) this.charts.trend.dispose()
      const c = echarts.init(el); this.charts.trend = c
      const fbDates = Array.from({length:30}, (_,i) => dayjs().subtract(29-i,'day').format('MM/DD'))
      const dates  = data.dates || fbDates
      const hours  = data.avgData || data.hours || new Array(dates.length).fill(0)
      const scores = data.scores || hours.map(h => h >= 8 ? 90 : h >= 7 ? 75 : h >= 6 ? 60 : h > 0 ? 40 : 0)
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,0.92)', borderColor: 'rgba(167,139,250,0.3)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: p => `${p[0].name}<br/>
            <span style="color:#a78bfa">睡眠时长：${p[0].value}h</span><br/>
            <span style="color:#52c41a">质量评分：${p[1]?.value ?? '--'}分</span>`
        },
        grid: { left: '5%', right: '5%', top: '10%', bottom: '12%', containLabel: true },
        xAxis: {
          type: 'category', data: dates, boundaryGap: true,
          axisLine: { lineStyle: { color: 'rgba(167,139,250,0.18)' } }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 10, interval: 4 }
        },
        yAxis: [
          {
            type: 'value', name: '时长(h)', nameTextStyle: { color: '#a78bfa', fontSize: 10 },
            axisLine: { show: false }, axisTick: { show: false },
            splitLine: { lineStyle: { color: 'rgba(167,139,250,0.07)', type: 'dashed' } },
            axisLabel: { color: '#8ba6c8', fontSize: 9 },
            min: 0, max: 12
          },
          {
            type: 'value', name: '评分', nameTextStyle: { color: '#52c41a', fontSize: 10 },
            axisLine: { show: false }, axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: { color: '#8ba6c8', fontSize: 9 },
            min: 0, max: 100
          }
        ],
        series: [
          {
            name: '睡眠时长', type: 'bar', yAxisIndex: 0, data: hours, barWidth: '55%',
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0,0,0,1,
                [{offset:0,color:'rgba(167,139,250,0.9)'},{offset:1,color:'rgba(167,139,250,0.18)'}]),
              borderRadius: [3,3,0,0]
            },
            markLine: {
              silent: true, symbol: 'none',
              data: [{ yAxis: 7, lineStyle: { color: '#FFB84D', type: 'dashed', width: 1 },
                label: { color: '#FFB84D', fontSize: 10, formatter: '建议7h' } }]
            }
          },
          {
            name: '质量评分', type: 'line', yAxisIndex: 1, data: scores, smooth: true, symbol: 'none',
            lineStyle: { color: '#52c41a', width: 2 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,
              [{offset:0,color:'rgba(82,196,26,0.18)'},{offset:1,color:'rgba(82,196,26,0.02)'}]) },
            markPoint: {
              symbol: 'circle', symbolSize: 5,
              label: { fontSize: 9, fontFamily: 'Consolas', offset: [0,-12] },
              data: [
                { type:'max', itemStyle:{color:'#52c41a'}, label:{color:'#52c41a', formatter: p=>'▲'+p.value} },
                { type:'min', itemStyle:{color:'#ff5252'}, label:{color:'#ff5252', formatter: p=>'▼'+p.value} }
              ]
            }
          }
        ]
      })
      c.on('click', params => {
        if (params.seriesName === '睡眠时长') {
          this.$message && this.$message.info(`${params.name} 睡眠时长：${params.value}h`)
        } else if (params.seriesName === '质量评分') {
          this.$message && this.$message.info(`${params.name} 质量评分：${params.value}分`)
        }
      })
    },

    // ── 睡眠时长分布（小环形饼图）──
    initDuration() {
      const el = this.$refs.durationRef; if (!el) return
      if (this.charts.duration) this.charts.duration.dispose()
      const c = echarts.init(el); this.charts.duration = c
      const total = this.overview.totalCount || 0
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(8,13,35,0.9)', borderColor: 'rgba(167,139,250,0.25)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: p => `${p.name}<br/>占比：<b style="color:${p.color}">${p.value}%</b><br/>约 ${Math.round(p.value*total/100)} 人`
        },
        title: {
          text: '检测人数', subtext: total + '人',
          left: 'center', top: '36%',
          textStyle: { color: '#6a88ab', fontSize: 10 },
          subtextStyle: { color: '#a78bfa', fontSize: 16, fontWeight: 'bold', fontFamily: 'Consolas' }
        },
        series: [{
          type: 'pie', radius: ['42%', '64%'], center: ['50%', '52%'],
          label: { show: false }, labelLine: { show: false },
          cursor: 'pointer',
          data: this.durationLegend.map(x => ({
            value: x.value, name: x.name,
            itemStyle: { color: x.color, borderRadius: 3, shadowColor: x.color+'44', shadowBlur: 6 }
          }))
        }]
      })
      c.on('click', params => {
        const count = Math.round(params.value * (this.overview.totalCount || 0) / 100)
        this.$message && this.$message.info(`${params.name}：${params.value}%（约 ${count} 人）`)
      })
    },

    // ── 入睡时间分布（数据库无入睡时刻字段，显示暂无数据）──
    initBedtime() {
      const el = this.$refs.bedtimeRef; if (!el) return
      if (this.charts.bedtime) this.charts.bedtime.dispose()
      const c = echarts.init(el); this.charts.bedtime = c
      this.lateBedPct = 0
      c.setOption({
        backgroundColor: 'transparent',
        graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }]
      })
    },

    // ── 各部门睡眠数据上传率（竖向柱状图，部门数据来自 /department/list 接口）──
    initDept(data) {
      const el = this.$refs.deptRef; if (!el) return
      if (this.charts.dept) this.charts.dept.dispose()
      const c = echarts.init(el); this.charts.dept = c
      if (!data || !data.length) {
        c.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#8ba6c8', fontSize: 14 } }] })
        return
      }
      const list = data
      this.deptUploadList = list
      const sorted = [...list].sort((a, b) => b.count - a.count)
      const getColor = v => v >= 90 ? '#52c41a' : v >= 75 ? '#4FC3F7' : v >= 60 ? '#FFB84D' : '#ff5252'
      // 根据部门数量动态算柱宽，保证22个部门也能全部显示
      const barMaxW = Math.max(10, Math.min(28, Math.floor(900 / sorted.length) - 4))
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis', axisPointer: { type: 'none' },
          backgroundColor: 'rgba(8,13,35,0.9)', borderColor: 'rgba(167,139,250,0.25)',
          textStyle: { color: '#e0f0ff', fontSize: 11 },
          formatter: p => `${p[0].name}：<b style="color:${p[0].color}">${p[0].value}%</b>`
        },
        grid: { left: 32, right: 12, top: 24, bottom: 56 },
        xAxis: {
          type: 'category',
          data: sorted.map(x => x.deptName),
          axisLine: { lineStyle: { color: 'rgba(167,139,250,0.15)' } },
          axisTick: { show: false },
          axisLabel: {
            color: '#8ba6c8',
            fontSize: sorted.length > 15 ? 8 : 9,
            rotate: sorted.length > 12 ? 35 : 0,
            interval: 0,
            overflow: 'truncate',
            width: 52
          }
        },
        yAxis: {
          type: 'value', min: 0, max: 100,
          axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(167,139,250,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 9, formatter: v => v + '%' }
        },
        series: [{
          type: 'bar',
          barMaxWidth: barMaxW,
          data: sorted.map(x => ({
            value: x.count,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: getColor(x.count) },
                { offset: 1, color: getColor(x.count) + '55' }
              ]),
              borderRadius: [4, 4, 0, 0]
            }
          })),
          label: {
            show: true, position: 'top',
            color: '#a8c5e6', fontSize: sorted.length > 15 ? 8 : 9,
            formatter: p => p.value + '%'
          },
          showBackground: true,
          backgroundStyle: { color: 'rgba(167,139,250,0.05)', borderRadius: [4,4,0,0] }
        }]
      })
    },

    scoreClass(s) {
      if (s >= 80) return 'sc-excellent'
      if (s >= 60) return 'sc-good'
      if (s >= 40) return 'sc-fair'
      return 'sc-poor'
    },
    fmtTime(ts) { return ts ? dayjs(ts).format('MM-DD HH:mm') : '' },

    // ── 记录详情弹窗 ──
    openRecordDialog(item) {
      this.recordDialog.item    = item
      this.recordDialog.visible = true
    },

    handleResize() {
      clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        
        this.$nextTick(() => Object.values(this.charts).forEach(c => c?.resize?.()))
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
$panel:  rgba(8,16,42,0.88);
$border: rgba(167,139,250,0.14);
$accent: #a78bfa;
$cyan:   #00d4ff;
$text:   #a8c5e6;
$dim:    #6a88ab;
$white:  #e8f4ff;

/* ── Root ── */
.sl-root {
  width: 100%;
  height: calc(100vh - 50px) !important; /* 视口高度 - 顶部导航栏 */
  min-height: 600px; /* 最小高度防止过小 */
  background: $bg;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(167,139,250,0.07) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(0,100,180,0.06) 0%, transparent 50%);
  overflow: hidden; display: flex; flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif; color: $text;
}

/* ── Header ── */
.sl-hd {
  height: 58px; flex-shrink: 0;
  display: flex; align-items: center; padding: 0 22px; gap: 16px;
  background: rgba(0,4,20,0.65); border-bottom: 1px solid $border;
}
.sl-hd-left { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.sl-live-dot {
  width: 9px; height: 9px; border-radius: 50%;
  background: $accent; box-shadow: 0 0 8px $accent;
  animation: slPulse 2.5s ease-in-out infinite;
}
@keyframes slPulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.4;transform:scale(0.75)} }
.sl-hd-title { font-size: 20px; font-weight: 700; color: $white; margin: 0; letter-spacing: 2px; text-shadow: 0 0 14px rgba(167,139,250,0.5); }
.sl-hd-date  { font-size: 12px; color: $accent; background: rgba(167,139,250,0.1); border: 1px solid rgba(167,139,250,0.25); border-radius: 4px; padding: 2px 8px; white-space: nowrap; }
.sl-hd-kpis  { flex: 1; display: flex; justify-content: center; }
.sl-kpi {
  display: flex; flex-direction: column; align-items: center;
  padding: 0 28px; border-right: 1px solid $border;
  &:first-child { border-left: 1px solid $border; }
}
.sl-kpi-n {
  font-size: 20px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1;
  &.kpi-cyan   { color: $cyan;   text-shadow: 0 0 10px rgba(0,212,255,0.5); }
  &.kpi-green  { color: #52c41a; text-shadow: 0 0 10px rgba(82,196,26,0.4); }
  &.kpi-purple { color: $accent; text-shadow: 0 0 10px rgba(167,139,250,0.5); }
  &.kpi-amber  { color: #FFB84D; text-shadow: 0 0 10px rgba(255,184,77,0.4); }
}
.sl-kpi-l   { font-size: 11px; color: $dim; margin-top: 2px; white-space: nowrap; }
.sl-hd-time { flex-shrink: 0; font-family: 'Consolas', monospace; font-size: 13px; color: $dim; }

/* ── Body ── */
.sl-bd { flex: 1; display: flex; gap: 8px; padding: 8px; overflow: hidden; min-height: 0; }

/* ── Aside ── */
.sl-aside { width: 304px; flex-shrink: 0; display: flex; flex-direction: column; gap: 8px; }

/* 2×2 卡片网格 */
.sl-stat-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 7px;
  flex-shrink: 0; height: 148px;
}
.sl-stat-card {
  background: $panel; border: 1px solid $border; border-radius: 10px;
  display: flex; align-items: center; gap: 10px; padding: 0 14px;
  position: relative; overflow: hidden;
  &::after {
    content: ''; position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
    width: 32px; height: 32px; border-radius: 50%;
    border: 2px solid rgba(255,255,255,0.05); border-top-color: var(--c); opacity: 0.5;
  }
}
.sl-stat-icon {
  width: 36px; height: 36px; border-radius: 9px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 16px;
}
.sl-stat-body { flex: 1; min-width: 0; }
.sl-stat-val   { font-size: 17px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.2; }
.sl-stat-label { font-size: 10px; color: $dim; margin-top: 2px; }

/* 阶段面板 */
.sl-stage-panel { flex: 0 0 178px; }
/* 评分面板 */
.sl-score-panel { flex: 0 0 170px; }
/* 异常预警 */
.sl-alert-panel { flex: 1; min-height: 0; }

/* ── Panel shared ── */
.sl-panel {
  background: $panel; border: 1px solid $border; border-radius: 10px;
  display: flex; flex-direction: column; overflow: hidden; backdrop-filter: blur(8px);
}
.sl-ph {
  height: 38px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 12px;
  border-bottom: 1px solid rgba(167,139,250,0.08);
  background: rgba(167,139,250,0.03);
}
.sl-ph-bar   { width: 3px; height: 14px; background: linear-gradient(180deg,$accent,rgba(167,139,250,0.25)); border-radius: 2px; box-shadow: 0 0 7px rgba(167,139,250,0.8); }
.sl-ph-title { font-size: 13px; font-weight: 600; color: $white; letter-spacing: 1px; }
.sl-ph-sub   { margin-left: auto; font-size: 11px; color: $dim; }
.sl-rt-total { margin-left: auto; font-size: 11px; color: $dim; }
.sl-trend-tags { margin-left: auto; display: flex; gap: 8px; }
.sl-tag { font-size: 10px; padding: 2px 6px; border-radius: 3px; border: 1px solid; }
.sl-pc  { flex: 1; min-height: 0; padding: 6px; }

/* ── 睡眠阶段 ── */
.sl-stage-body   { flex: 1; display: flex; align-items: center; gap: 8px; padding: 6px 10px; min-height: 0; }
.sl-stage-chart  { width: 110px; height: 110px; flex-shrink: 0; }
.sl-stage-legend { flex: 1; display: flex; flex-direction: column; gap: 7px; }
.sl-stage-row    { display: flex; align-items: center; gap: 6px; }
.sl-stage-dot    { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.sl-stage-name   { font-size: 11px; color: $text; width: 60px; flex-shrink: 0; }
.sl-stage-bar-wrap { flex: 1; height: 5px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
.sl-stage-bar    { height: 100%; border-radius: 3px; transition: width 0.8s ease; opacity: 0.85; }
.sl-stage-pct    { font-size: 12px; font-weight: 700; font-family: 'Consolas', monospace; width: 30px; text-align: right; }

/* ── Main ── */
.sl-main      {
  flex: 1; display: flex; flex-direction: column; gap: 8px; min-width: 0;
  overflow-y: auto; overflow-x: hidden;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 2px; }
  &::-webkit-scrollbar-track { background: rgba(0,212,255,0.05); }
}
.sl-trend-panel { flex: 0 0 390px; }
.sl-mid-row   { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 8px; }
.sl-mid-top   { flex: 0 0 48%; display: flex; gap: 8px; min-height: 0; }
.sl-duration-panel { flex: 0 0 310px; }
.sl-bedtime-panel  { flex: 1; }
.sl-dept-panel     { flex: 1; min-height: 0; }

/* 睡眠时长分布 */
.sl-mid-top   { flex: 0 0 48%; display: flex; gap: 8px; min-height: 0; overflow: hidden; }
.sl-dur-segbar { height: 14px; display: flex; margin: 6px 10px 0; border-radius: 5px; overflow: hidden; flex-shrink: 0; }
.sl-dur-seg    { transition: flex 0.8s; }
.sl-dur-seglabels { display: flex; margin: 4px 10px 0; flex-shrink: 0; }
.sl-dur-seglabel  {
  font-size: 9px; color: $dim; text-align: center; overflow: hidden;
  em { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 2px; vertical-align: middle; }
}
.sl-dur-body  { flex: 1; min-height: 0; display: flex; align-items: stretch; margin-top: 4px; }
.sl-dur-chart { width: 120px; flex-shrink: 0; }
.sl-dur-tips  { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 6px; padding: 6px 10px 6px 2px; }
.sl-dur-tip-title { font-size: 10px; color: $dim; font-weight: 600; letter-spacing: 0.5px; margin-bottom: 2px; }
.sl-dur-tip-row { display: flex; align-items: center; gap: 5px; }
.sl-dur-tip-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.sl-dur-tip-name { font-size: 10px; font-weight: 600; width: 44px; flex-shrink: 0; }
.sl-dur-tip-desc { font-size: 10px; color: $dim; }

/* 入睡时间分布：左图右文 */
.sl-bed-body  { flex: 1; min-height: 0; display: flex; align-items: stretch; }
.sl-bed-chart { flex: 0 0 55%; min-width: 0; }
.sl-bed-tips  { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 6px; padding: 8px 12px 8px 4px; }
.sl-bed-tip-title { font-size: 10px; color: $dim; font-weight: 600; letter-spacing: 0.5px; margin-bottom: 2px; }
.sl-bed-tip-row { display: flex; align-items: center; gap: 5px; }
.sl-bed-tip-icon { font-size: 11px; font-style: normal; width: 13px; flex-shrink: 0; }
.sl-bed-tip-time { font-size: 10px; font-weight: 600; width: 46px; flex-shrink: 0; }
.sl-bed-tip-desc { font-size: 10px; color: $dim; }
.sl-bed-warn {
  margin-top: 3px; display: flex; align-items: center; gap: 5px;
  padding: 4px 7px; border-radius: 5px;
  background: rgba(255,82,82,0.08); border: 1px solid rgba(255,82,82,0.2);
  font-size: 10px; color: $dim;
}
.sl-bed-warn-dot { width: 5px; height: 5px; border-radius: 50%; background: #ff5252; flex-shrink: 0; animation: slPulse 1.5s infinite; }

/* 异常预警面板 */
.sl-alert-empty { padding: 16px 0; text-align: center; color: rgba(126,184,247,0.5); font-size: 12px; }
.sl-alert-count { margin-left: auto; font-size: 11px; color: #ff5252; font-weight: 600; }
.sl-alert-body  { flex: 1; min-height: 0; overflow-y: auto; padding: 4px 8px;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,82,82,0.2); border-radius: 2px; }
}
.sl-alert-row {
  display: grid; grid-template-columns: 64px 52px 38px 1fr;
  gap: 4px; padding: 5px 4px; margin-bottom: 2px;
  border-radius: 5px; border-left: 2px solid transparent; align-items: center;
  background: rgba(255,82,82,0.04);
  &.danger { border-left-color: rgba(255,82,82,0.6); background: rgba(255,82,82,0.06); }
  &.warn   { border-left-color: rgba(255,184,77,0.5); background: rgba(255,184,77,0.04); }
  &.miss   { border-left-color: rgba(106,136,171,0.4); }
}
.sl-alert-name { font-size: 11px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sl-alert-tag {
  font-size: 9px; padding: 1px 4px; border-radius: 3px; text-align: center; white-space: nowrap;
  &.danger { background: rgba(255,82,82,0.15);  color: #ff5252; border: 1px solid rgba(255,82,82,0.3); }
  &.warn   { background: rgba(255,184,77,0.12); color: #FFB84D; border: 1px solid rgba(255,184,77,0.3); }
  &.miss   { background: rgba(106,136,171,0.1); color: $dim;    border: 1px solid rgba(106,136,171,0.2); }
}
.sl-alert-val  { font-size: 11px; font-weight: 700; color: #ff8c42; font-family: 'Consolas', monospace; }
.sl-alert-desc { font-size: 10px; color: $dim; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── Rtlist ── */
.sl-rtlist { width: 262px; flex-shrink: 0; }

.sl-rt-hd {
  display: grid; grid-template-columns: 56px 46px 36px 38px 1fr;
  gap: 4px; padding: 6px 10px; flex-shrink: 0;
  background: rgba(167,139,250,0.06);
  span { font-size: 11px; color: $dim; font-weight: 600; }
}
.sl-rt-body {
  flex: 1; overflow-y: auto; padding: 3px 6px; min-height: 0;
  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(167,139,250,0.18); border-radius: 2px; }
}
.sl-rt-row {
  display: grid; grid-template-columns: 56px 46px 36px 38px 1fr;
  gap: 4px; padding: 6px 4px; margin-bottom: 1px;
  border-radius: 5px; align-items: center;
  border-left: 2px solid transparent; transition: background 0.2s;
  &:hover { background: rgba(167,139,250,0.05); }
  &.excellent { border-left-color: rgba(82,196,26,0.5); }
  &.good      { border-left-color: rgba(79,195,247,0.5); }
  &.poor      { border-left-color: rgba(255,184,77,0.5); }
}
.sl-rt-name  { font-size: 12px; color: $white; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sl-rt-dur   { font-size: 12px; color: $accent; font-family: 'Consolas', monospace; }
.sl-rt-score {
  font-size: 13px; font-weight: 700; font-family: 'Consolas', monospace;
  &.sc-excellent { color: #52c41a; }
  &.sc-good      { color: #4FC3F7; }
  &.sc-fair      { color: #FFB84D; }
  &.sc-poor      { color: #ff5252; }
}
.sl-rt-badge {
  font-size: 10px; padding: 1px 3px; border-radius: 3px; text-align: center;
  &.excellent { background: rgba(82,196,26,0.12);  color: #52c41a; border: 1px solid rgba(82,196,26,0.28); }
  &.good      { background: rgba(79,195,247,0.12); color: #4FC3F7; border: 1px solid rgba(79,195,247,0.28); }
  &.poor      { background: rgba(255,184,77,0.12); color: #FFB84D; border: 1px solid rgba(255,184,77,0.28); }
}
.sl-rt-time  { font-size: 10px; color: $dim; }

.sl-rt-pg {
  height: 36px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; gap: 5px;
  border-top: 1px solid rgba(167,139,250,0.1);
}
.sl-pg-btn {
  height: 22px; padding: 0 7px;
  background: rgba(167,139,250,0.07); border: 1px solid rgba(167,139,250,0.2);
  border-radius: 3px; color: $accent; font-size: 12px; cursor: pointer; transition: background 0.2s;
  &:hover:not(:disabled) { background: rgba(167,139,250,0.16); }
  &:disabled { opacity: 0.28; cursor: not-allowed; }
}
.sl-pg-info { font-size: 12px; color: $accent; min-width: 44px; text-align: center; }

/* ── 睡眠记录详情弹窗（不受 scoped 缩放影响，采用 :deep 穿透） */
.sl-rt-row { cursor: pointer; }
</style>

<style>
.sl-record-dialog .el-dialog {
  background: rgba(8,13,35,0.97);
  border: 1px solid rgba(167,139,250,0.22);
  border-radius: 12px;
  color: #a8c5e6;
}
.sl-record-dialog .el-dialog__title { color: #e8f4ff; font-weight: 700; letter-spacing: 1px; }
.sl-record-dialog .el-dialog__header { border-bottom: 1px solid rgba(167,139,250,0.12); }
.sl-rd-body { display: flex; flex-direction: column; gap: 12px; padding: 4px 0; }
.sl-rd-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 12px; border-radius: 6px;
  background: rgba(167,139,250,0.04);
  border: 1px solid rgba(167,139,250,0.08);
}
.sl-rd-key { font-size: 13px; color: #6a88ab; width: 72px; flex-shrink: 0; }
.sl-rd-val { font-size: 14px; color: #e8f4ff; font-family: 'Consolas', monospace; font-weight: 600; }
</style>