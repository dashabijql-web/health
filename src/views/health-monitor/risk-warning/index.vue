<template>
  <div class="rw-root">

    <!-- ══ Header ══ -->
    <header class="rw-hd">
      <div class="rw-hd-left">
        <span class="rw-live-dot"></span>
        <h1 class="rw-hd-title">健康风险预警</h1>
      </div>

      <!-- FIX ②: 5个KPI 含压力预警 -->
      <div class="rw-hd-kpis">
        <div class="rw-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="rw-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="rw-kpi-l">{{ k.label }}</span>
        </div>
      </div>

      <div class="rw-period-tabs">
        <span v-for="p in periodOptions" :key="p.value"
          :class="['rw-period-tab', activePeriod === p.value ? 'is-active' : '']"
          @click="switchPeriod(p.value)">{{ p.label }}</span>
      </div>
      <div class="rw-hd-time">{{ currentTime }}</div>
    </header>

    <!-- ══ Body ══ -->
    <section class="rw-bd">

      <!-- 左侧 -->
      <aside class="rw-aside">
        <div class="rw-panel rw-aside-top">
          <div class="rw-ph">
            <span class="rw-ph-bar"></span>
            <span class="rw-ph-title">{{ statTitle }}</span>
          </div>
          <div class="rw-stat-list">
            <div class="rw-stat-row" v-for="(item, i) in warningStats" :key="i">
              <span class="rw-stat-dot" :style="{background: item.color}"></span>
              <span class="rw-stat-name">{{ item.label }}</span>
              <div class="rw-stat-bar-wrap">
                <div class="rw-stat-bar" :style="{width: statBarWidth(item.value) + '%', background: item.color}"></div>
              </div>
              <span class="rw-stat-val" :style="{color: item.color}">{{ item.value }}</span>
            </div>
          </div>
        </div>

        <div class="rw-panel rw-aside-bot">
          <div class="rw-ph">
            <span class="rw-ph-bar"></span>
            <span class="rw-ph-title">部门风险预警分布</span>
          </div>
          <div class="rw-pc">
            <div ref="deptRef" style="width:100%;height:100%"></div>
          </div>
        </div>
      </aside>

      <!-- 主区域 -->
      <main class="rw-main">
        <div class="rw-panel rw-panel-trend">
          <div class="rw-ph">
            <span class="rw-ph-bar"></span>
            <span class="rw-ph-title">{{ trendTitle }}</span>
            <div class="rw-trend-tags">
              <span class="rw-tag" style="color:#ef4444;border-color:rgba(239,68,68,0.3)">── 心率</span>
              <span class="rw-tag" style="color:#f97316;border-color:rgba(249,115,22,0.3)">── 血氧</span>
              <span class="rw-tag" style="color:#22c55e;border-color:rgba(34,197,94,0.3)">── 体温</span>
              <span class="rw-tag" style="color:#00d4ff;border-color:rgba(0,212,255,0.3)">── 压力</span>
            </div>
          </div>
          <div class="rw-pc">
            <div ref="trendRef" style="width:100%;height:100%"></div>
          </div>
        </div>

        <!-- 预警列表 + 右侧面板 -->
        <div class="rw-panel rw-panel-list">

          <!-- 列表区域 -->
          <div class="rw-list-wrap">
            <div class="rw-ph">
              <span class="rw-ph-bar"></span>
              <span class="rw-ph-title">当天预警数据</span>
              <span class="rw-rt-total">{{ filteredList.length }} 条</span>
              <div class="rw-filter-bar">
                <input v-model="filterName" class="rw-filter-input" placeholder="搜索姓名…" @input="currentPage=1" />
                <select v-model="filterLevel" class="rw-filter-select" @change="currentPage=1">
                  <option value="">全部级别</option>
                  <option value="高">高</option><option value="中">中</option><option value="低">低</option>
                  <option value="危险">危险</option><option value="警告">警告</option><option value="提醒">提醒</option>
                </select>
                <select v-model="filterType" class="rw-filter-select" @change="currentPage=1">
                  <option value="">全部类型</option>
                  <option v-for="t in warningTypes" :key="t" :value="t">{{ t }}</option>
                </select>
                <button class="rw-scroll-btn" @click="toggleAutoScroll">
                  {{ autoScrollPaused ? '▶ 继续' : '⏸ 暂停' }}
                </button>
              </div>
            </div>

            <div class="rw-list-hd">
              <span>序号</span><span>姓名</span><span>部门</span><span>预警类型</span><span>级别</span><span>预警值</span><span>状态</span><span>时间</span>
            </div>

            <div class="rw-list-body" ref="listRef"
              @mouseenter="pauseAutoScroll"
              @mouseleave="resumeAutoScroll">
              <div class="rw-list-row" v-for="(item, i) in pagedList" :key="i"
                :class="warnClass(item.warningLevel)" @click="openDetail(item)">
                <span class="rw-list-idx">{{ (currentPage-1)*pageSize+i+1 }}</span>
                <span class="rw-list-name">{{ item.userName || '--' }}</span>
                <span class="rw-list-dept">{{ item.deptName || '--' }}</span>
                <span class="rw-list-type">{{ item.warningType || '--' }}</span>
                <span class="rw-list-badge" :class="warnClass(item.warningLevel)">{{ item.warningLevel || '--' }}</span>
                <span class="rw-list-val">{{ item.warningValue || '--' }}</span>
                <span class="rw-list-handled" :class="item.handled ? 'handled' : 'pending'">{{ item.handled ? '已处理' : '待处理' }}</span>
                <span class="rw-list-time">{{ fmtTime(item.createTime) }}</span>
              </div>
              <div v-if="filteredList.length===0" class="rw-list-empty">暂无匹配数据</div>
            </div>

            <div class="rw-list-pg">
              <button class="rw-pg-btn" :disabled="currentPage===1" @click="jumpPage(1)">首页</button>
              <button class="rw-pg-btn" :disabled="currentPage===1" @click="jumpPage(currentPage-1)">‹</button>
              <span class="rw-pg-info">{{ currentPage }} / {{ totalPages }}</span>
              <button class="rw-pg-btn" :disabled="currentPage>=totalPages" @click="jumpPage(currentPage+1)">›</button>
              <button class="rw-pg-btn" :disabled="currentPage>=totalPages" @click="jumpPage(totalPages)">末页</button>
            </div>
          </div>

          <!-- 右侧面板 320px -->
          <div class="rw-right-col">

            <!-- 3.1 体征均值卡 -->
            <div class="rw-vc-panel">
              <div class="rw-sub-ph">
                <span class="rw-ph-bar"></span>
                <span class="rw-ph-title">体征均值</span>
              </div>
              <div class="rw-vc-row">
                <div class="rw-vc" v-for="v in vitalAvg" :key="v.label">
                  <div class="rw-vc-label">{{ v.label }}</div>
                  <div class="rw-vc-val" :style="{color:v.color}">{{ v.val }}</div>
                  <div class="rw-vc-sub">异常 {{ v.abnormal }} 人</div>
                  <div class="rw-vc-bar-track">
                    <div class="rw-vc-bar" :style="{width:v.rate+'%',background:v.color}"></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 3.2 预警类型分布 -->
            <div class="rw-panel rw-donut-panel">
              <div class="rw-sub-ph">
                <span class="rw-ph-bar"></span>
                <span class="rw-ph-title">预警类型分布</span>
              </div>
              <div class="rw-donut-body">
                <div ref="donutRef" class="rw-donut-chart"></div>
                <div class="rw-donut-legend">
                  <div class="rw-donut-leg-row" v-for="s in warningStats" :key="s.label">
                    <span class="rw-donut-dot" :style="{color:s.color}">●</span>
                    <span class="rw-donut-lname">{{ s.label }}</span>
                    <span class="rw-donut-lval" :style="{color:s.color}">{{ s.value }}</span>
                    <span class="rw-donut-lpct">{{ donutPct(s.value) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 3.3 处理进度 -->
            <div class="rw-panel rw-prog-panel">
              <div class="rw-sub-ph">
                <span class="rw-ph-bar"></span>
                <span class="rw-ph-title">处理进度</span>
              </div>
              <div class="rw-prog-body">
                <div class="rw-prog-row" v-for="p in handleProgress" :key="p.label">
                  <div class="rw-prog-hd">
                    <span class="rw-prog-name">{{ p.label }}</span>
                    <span class="rw-prog-cnt">
                      <span style="color:#22c55e">{{ p.handled }}</span>
                      <span style="color:#4a5578">/{{ p.total }}</span>
                    </span>
                  </div>
                  <div class="rw-prog-track">
                    <div class="rw-prog-fill" :style="{width:p.rate+'%'}"></div>
                  </div>
                </div>
                <div class="rw-prog-total">
                  总处理率：<span style="color:#22c55e;font-weight:700">{{ handleProgressTotal }}%</span>
                </div>
              </div>
            </div>

            <!-- 3.4 高危人员 TOP5 -->
            <div class="rw-panel rw-top5-panel">
              <div class="rw-sub-ph">
                <span class="rw-ph-bar"></span>
                <span class="rw-ph-title">高危人员 TOP5</span>
              </div>
              <div class="rw-top5-body">
                <div class="rw-top5-row" v-for="(u, i) in top5Users" :key="u.empCode"
                  @click="openDetailByUser(u)">
                  <span class="rw-top5-rank" :class="'rank-'+(i+1)">{{ i+1 }}</span>
                  <div class="rw-top5-info">
                    <div class="rw-top5-name">{{ u.name }}</div>
                    <div class="rw-top5-dept">{{ u.dept }}</div>
                  </div>
                  <span class="rw-top5-count">{{ u.count }}</span>
                  <span class="rw-top5-badge" :class="'lvbadge-'+levelClass(u.maxLevel)">{{ levelLabel(u.maxLevel) }}</span>
                </div>
                <div v-if="!top5Users.length" class="rw-list-empty">暂无数据</div>
              </div>
            </div>

          </div>
        </div>
      </main>
    </section>

    <!-- ══ 预警详情 + 体征曲线 Drawer ══ -->
    <el-drawer
      v-model="detailVisible"
      :title="detailRow ? detailRow.userName + ' · 预警详情' : '预警详情'"
      width="580px"
      direction="rtl"
      class="rw-detail-drawer"
      @close="onDrawerClose"
    >
      <div v-if="detailRow" class="rw-dw-wrap">

        <!-- 顶部信息卡 -->
        <div class="rw-dw-card">
          <div class="rw-dw-avatar">{{ (detailRow.userName||'?').charAt(0) }}</div>
          <div class="rw-dw-main">
            <div class="rw-dw-name">{{ detailRow.userName||'--' }}</div>
            <div class="rw-dw-sub">{{ detailRow.deptName||'--' }} · {{ fmtTimeFull(detailRow.createTime) }}</div>
          </div>
          <div class="rw-dw-tags">
            <span :class="'badge-'+warnClass(detailRow.warningLevel)">{{ detailRow.warningLevel||'--' }}</span>
            <span :class="detailRow.handled?'badge-handled':'badge-pending'">{{ detailRow.handled?'已处理':'待处理' }}</span>
          </div>
        </div>

        <!-- KV 行 -->
        <div class="rw-dw-kvrow">
          <div class="rw-dw-kv"><span class="rw-dw-k">预警类型</span><span class="rw-dw-v">{{ detailRow.warningType||'--' }}</span></div>
          <div class="rw-dw-kv"><span class="rw-dw-k">预警值</span><span class="rw-dw-v rw-dw-num">{{ detailRow.warningValue||'--' }}</span></div>
          <div class="rw-dw-kv" v-if="detailRow.handleNote"><span class="rw-dw-k">备注</span><span class="rw-dw-v">{{ detailRow.handleNote }}</span></div>
        </div>

        <!-- 体征曲线 -->
        <div class="rw-dw-chart-section">
          <div class="rw-dw-chart-hd">
            <span class="rw-dw-chart-title">预警时刻前后 1 小时体征曲线</span>
            <span class="rw-dw-chart-range">{{ vitalChartRange }}</span>
          </div>

          <div v-if="vitalLoading" class="rw-dw-loading">
            <div class="rw-dw-spinner"></div><span>加载体征数据…</span>
          </div>

          <div v-else-if="vitalEmpty" class="rw-dw-empty">
            <div>📭</div><p>该时段暂无体征历史记录</p>
          </div>

          <template v-else>
            <div ref="vitalChartRef" class="rw-dw-chart"></div>
            <!-- 峰值摘要 -->
            <div class="rw-dw-summary">
              <div v-for="s in vitalSummary" :key="s.label" class="rw-dw-si">
                <div class="rw-dw-si-label">{{ s.label }}</div>
                <div class="rw-dw-si-val" :style="{color:s.color}">{{ s.val }}</div>
                <div class="rw-dw-si-sub">{{ s.sub }}</div>
              </div>
            </div>
          </template>
        </div>

      </div>
    </el-drawer>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getRiskWarningOverview, getRiskWarningList, getRiskWarningTrend, getDeptWarningStats } from '@/api/risk-warning'
import { getHealthRecords } from '@/api/health'

export default {
  name: 'RiskWarning',
  data() {
    return {
      currentTime: '',
      warningStats: [
        { label: '心率预警', value: 0, color: '#ef4444' },
        { label: '血氧预警', value: 0, color: '#f97316' },
        { label: '体温预警', value: 0, color: '#22c55e' },
        { label: '压力预警', value: 0, color: '#00d4ff' }
      ],
      warningList: [],
      currentPage: 1,
      pageSize: 20,
      filterName: '',
      filterLevel: '',
      filterType: '',
      trendData: { dates: [], series: { heartRate: [], bloodOxygen: [], temperature: [], pressure: [] } },
      deptData: [],
      activePeriod: 'month',
      periodOptions: [{ label: '当日', value: 'day' }, { label: '近7日', value: 'week' }, { label: '近30日', value: 'month' }],
      charts: {},
      clockTimer: null, refreshTimer: null, scrollTimer: null, resizeTimer: null,
      detailVisible: false, detailRow: null,
      autoScrollPaused: false,
      scrollTop: 0,
      // 体征曲线
      vitalLoading: false,
      vitalEmpty: false,
      vitalChartRange: '',
      vitalSummary: [],
      vitalChartInst: null
    }
  },
  computed: {
    // FIX ②: 5个KPI含压力
    headerKpis() {
      const total = this.warningStats.reduce((s, x) => s + x.value, 0)
      return [
        { label: '今日总预警', val: total,                       cls: 'kpi-red'    },
        { label: '心率预警',   val: this.warningStats[0].value, cls: 'kpi-red'    },
        { label: '血氧预警',   val: this.warningStats[1].value, cls: 'kpi-orange' },
        { label: '体温预警',   val: this.warningStats[2].value, cls: 'kpi-cyan'   },
        { label: '压力预警',   val: this.warningStats[3].value, cls: 'kpi-purple' }
      ]
    },
    statTitle()  { return { day:'今日预警统计', week:'近7日预警统计', month:'近30日预警统计' }[this.activePeriod] },
    trendTitle() { return { day:'今日预警分布', week:'近7天预警趋势', month:'近30天预警趋势' }[this.activePeriod] },
    periodRange() {
      const today = dayjs().format('YYYY-MM-DD')
      if (this.activePeriod === 'day')  return { startDate: today, endDate: today }
      if (this.activePeriod === 'week') return { startDate: dayjs().subtract(6,'day').format('YYYY-MM-DD'), endDate: today }
      return { startDate: dayjs().subtract(29,'day').format('YYYY-MM-DD'), endDate: today }
    },
    statMax() { return Math.max(1, ...this.warningStats.map(x => x.value)) },
    // FIX ⑤: 筛选
    filteredList() {
      return this.warningList.filter(item => {
        const nameOk  = !this.filterName  || (item.userName||'').includes(this.filterName)
        const levelOk = !this.filterLevel || item.warningLevel === this.filterLevel
        const typeOk  = !this.filterType  || item.warningType  === this.filterType
        return nameOk && levelOk && typeOk
      })
    },
    pagedList()   { const s=(this.currentPage-1)*this.pageSize; return this.filteredList.slice(s,s+this.pageSize) },
    totalPages()  { return Math.max(1, Math.ceil(this.filteredList.length/this.pageSize)) },
    warningTypes(){ return [...new Set(this.warningList.map(x=>x.warningType).filter(Boolean))] },

    // 右侧面板：体征均值卡
    vitalAvg() {
      const total = this.warningList.length || 1
      const parse = v => parseFloat(v) || null
      const hrVals   = this.warningList.filter(x=>(x.warningType||'').includes('心率')).map(x=>parse(x.warningValue)).filter(v=>v!=null)
      const spo2Vals = this.warningList.filter(x=>(x.warningType||'').includes('血氧')).map(x=>parse(x.warningValue)).filter(v=>v!=null)
      const tempVals = this.warningList.filter(x=>(x.warningType||'').includes('体温')).map(x=>parse(x.warningValue)).filter(v=>v!=null)
      const avg = arr => arr.length ? (arr.reduce((s,v)=>s+v,0)/arr.length).toFixed(0) : '--'
      return [
        { label:'平均心率', val:hrVals.length   ? avg(hrVals)+'bpm'  : '--', color:'#ef4444', abnormal:hrVals.length,   rate:Math.round(hrVals.length/total*100)   },
        { label:'平均血氧', val:spo2Vals.length  ? avg(spo2Vals)+'%'  : '--', color:'#f97316', abnormal:spo2Vals.length, rate:Math.round(spo2Vals.length/total*100) },
        { label:'平均体温', val:tempVals.length  ? avg(tempVals)+'°C' : '--', color:'#22c55e', abnormal:tempVals.length, rate:Math.round(tempVals.length/total*100) }
      ]
    },

    // 右侧面板：处理进度
    handleProgress() {
      return ['心率','血氧','体温','压力'].map(key => {
        const matched = this.warningList.filter(x=>(x.warningType||'').includes(key))
        const handled = matched.filter(x=>x.handled||x.isHandled===1||x.isHandled===true)
        return { label:key+'预警', total:matched.length, handled:handled.length, rate:matched.length?Math.round(handled.length/matched.length*100):0 }
      })
    },
    handleProgressTotal() {
      const all = this.warningList.length; if(!all) return 0
      return Math.round(this.warningList.filter(x=>x.handled||x.isHandled===1||x.isHandled===true).length/all*100)
    },

    // 右侧面板：TOP5 高危人员
    top5Users() {
      const map = {}
      this.warningList.forEach(x => {
        const k = x.empCode || x.userCode; if(!k) return
        if(!map[k]) map[k] = { name:x.userName||'--', dept:x.deptName||'--', empCode:k, count:0, maxLevel:'低', _first:x }
        map[k].count++
        if(this.levelRank(x.warningLevel) > this.levelRank(map[k].maxLevel)) map[k].maxLevel = x.warningLevel
      })
      return Object.values(map).sort((a,b)=>b.count-a.count).slice(0,5)
    }
  },
  watch: {
    filteredList() {
      this.$nextTick(() => { const el=this.$refs.listRef; if(el){ el.scrollTop=0; this.scrollTop=0 } })
    }
  },
  mounted() {
    this.initClock(); this.fetchData(); this.setScale()
    window.addEventListener('resize', this.handleResize)
    this.$nextTick(() => this.startAutoScroll())
    this.refreshTimer = setInterval(() => this.fetchData(), 30000)
  },
  beforeUnmount() {
    clearInterval(this.clockTimer); clearInterval(this.refreshTimer)
    clearInterval(this.scrollTimer); clearTimeout(this.resizeTimer)
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    initClock() {
      const tick = () => { this.currentTime = dayjs().format('YYYY年MM月DD日 HH:mm:ss') }
      tick(); this.clockTimer = setInterval(tick, 1000)
    },
    async fetchData() {
      await Promise.allSettled([this.loadStats(), this.loadTrend(), this.loadDept(), this.loadList()])
      this.$nextTick(() => this.initDonutChart())
    },
    async loadStats() {
      const { startDate, endDate } = this.periodRange
      try {
        const r = await getRiskWarningOverview(startDate, endDate)
        if (r.code===200 && r.data) {
          const d=r.data
          this.warningStats[0].value=d.heartRateCount||0; this.warningStats[1].value=d.bloodOxygenCount||0
          this.warningStats[2].value=d.temperatureCount||0; this.warningStats[3].value=d.pressureCount||0
        }
      } catch {}
    },
    async loadTrend() {
      if (this.activePeriod==='day') { this.$nextTick(()=>this.initTrendDay()); return }
      const days = this.activePeriod==='week'?7:30
      try {
        const r = await getRiskWarningTrend(days)
        if (r.code===200 && r.data) {
          if (r.data.dates?.length) {
            this.trendData=r.data
          } else if (Array.isArray(r.data) && r.data.length) {
            const dates=[],heartRate=[],bloodOxygen=[],temperature=[],pressure=[]
            r.data.forEach(x=>{dates.push(x.date);heartRate.push(x.heartRate||0);bloodOxygen.push(x.bloodOxygen||0);temperature.push(x.temperature||0);pressure.push(x.pressure||0)})
            this.trendData={dates,series:{heartRate,bloodOxygen,temperature,pressure}}
          }
        }
      } catch {}
      this.$nextTick(()=>this.initTrendChart())
    },
    async loadDept() {
      const { startDate, endDate } = this.periodRange
      try {
        const r = await getDeptWarningStats(startDate, endDate)
        if (r.code===200 && r.data?.length) this.deptData=r.data
      } catch {}
      this.$nextTick(()=>this.initDeptChart())
    },
    async loadList() {
      try {
        const r = await getRiskWarningList({page:1,size:10000,level:'',handled:null})
        if (r.code===200 && r.data) this.warningList=r.data.list||r.data||[]
      } catch {}
    },

    initTrendChart() {
      const el=this.$refs.trendRef; if(!el) return
      if(this.charts.trend) this.charts.trend.dispose()
      const c=echarts.init(el); this.charts.trend=c
      const {dates,series}=this.trendData
      if(!dates.length){ c.setOption({backgroundColor:'transparent',graphic:[{type:'text',left:'center',top:'middle',style:{text:'暂无趋势数据',fill:'#8ba6c8',fontSize:14}}]}); return }
      c.setOption({
        backgroundColor:'transparent',
        tooltip:{trigger:'axis',backgroundColor:'rgba(8,13,35,0.92)',borderColor:'rgba(0,212,255,0.25)',textStyle:{color:'#e0f0ff',fontSize:12}},
        legend:{data:['心率','血氧','体温','压力'],right:10,top:4,textStyle:{color:'#8ba6c8',fontSize:11},itemWidth:16,itemHeight:8},
        grid:{left:'4%',right:'4%',top:'12%',bottom:'10%',containLabel:true},
        xAxis:{type:'category',data:dates,boundaryGap:false,axisLine:{lineStyle:{color:'rgba(0,212,255,0.18)'}},axisTick:{show:false},axisLabel:{color:'#8ba6c8',fontSize:10,interval:4}},
        yAxis:{type:'value',axisLine:{show:false},axisTick:{show:false},splitLine:{lineStyle:{color:'rgba(0,212,255,0.07)',type:'dashed'}},axisLabel:{color:'#8ba6c8',fontSize:10}},
        series:[
          {name:'心率',type:'line',data:series.heartRate,smooth:true,symbol:'none',lineStyle:{color:'#ef4444',width:1.5},itemStyle:{color:'#ef4444'}},
          {name:'血氧',type:'line',data:series.bloodOxygen,smooth:true,symbol:'none',lineStyle:{color:'#f97316',width:1.5},itemStyle:{color:'#f97316'}},
          {name:'体温',type:'line',data:series.temperature,smooth:true,symbol:'none',lineStyle:{color:'#22c55e',width:1.5},itemStyle:{color:'#22c55e'}},
          {name:'压力',type:'line',data:series.pressure,smooth:true,symbol:'none',lineStyle:{color:'#00d4ff',width:1.5},itemStyle:{color:'#00d4ff'}}
        ]
      })
    },

    initDeptChart() {
      const el=this.$refs.deptRef; if(!el) return
      if(this.charts.dept) this.charts.dept.dispose()
      const c=echarts.init(el); this.charts.dept=c
      if(!this.deptData.length){ c.setOption({backgroundColor:'transparent',graphic:[{type:'text',left:'center',top:'middle',style:{text:'暂无数据',fill:'#8ba6c8',fontSize:14}}]}); return }
      const names=this.deptData.map(d=>d.deptName)
      // FIX ④: 动态左侧留白防截断
      const maxLen=Math.max(...names.map(n=>n.length))
      const leftPct=Math.min(42,Math.max(24,maxLen*2.8))+'%'
      c.setOption({
        backgroundColor:'transparent',
        tooltip:{trigger:'axis',axisPointer:{type:'shadow'},backgroundColor:'rgba(8,13,35,0.92)',borderColor:'rgba(0,212,255,0.25)',textStyle:{color:'#e0f0ff',fontSize:11}},
        legend:{data:['心率','血氧','体温','压力'],right:6,top:4,textStyle:{color:'#8ba6c8',fontSize:10},itemWidth:10,itemHeight:8,icon:'rect'},
        grid:{left:leftPct,right:'8%',top:'14%',bottom:'6%'},
        xAxis:{type:'value',axisLine:{show:false},axisTick:{show:false},splitLine:{lineStyle:{color:'rgba(0,212,255,0.07)',type:'dashed'}},axisLabel:{color:'#8ba6c8',fontSize:10}},
        yAxis:{type:'category',data:names,inverse:true,axisLine:{show:false},axisTick:{show:false},axisLabel:{color:'#a8c5e6',fontSize:11,overflow:'truncate',width:80}},
        series:[
          {name:'心率',type:'bar',stack:'total',barWidth:'50%',data:this.deptData.map(d=>d.heartRate||0),itemStyle:{color:'#ef4444'},label:{show:true,position:'inside',color:'#fff',fontSize:9,formatter:p=>p.value>0?p.value:''}},
          {name:'血氧',type:'bar',stack:'total',data:this.deptData.map(d=>d.bloodOxygen||0),itemStyle:{color:'#f97316'},label:{show:true,position:'inside',color:'#fff',fontSize:9,formatter:p=>p.value>0?p.value:''}},
          {name:'体温',type:'bar',stack:'total',data:this.deptData.map(d=>d.temperature||0),itemStyle:{color:'#22c55e'},label:{show:true,position:'inside',color:'#fff',fontSize:9,formatter:p=>p.value>0?p.value:''}},
          {name:'压力',type:'bar',stack:'total',data:this.deptData.map(d=>d.pressure||0),itemStyle:{color:'#00d4ff',borderRadius:[0,4,4,0]},label:{show:true,position:'inside',color:'#fff',fontSize:9,formatter:p=>p.value>0?p.value:''}}
        ]
      })
    },

    initTrendDay() {
      const el=this.$refs.trendRef; if(!el) return
      if(this.charts.trend) this.charts.trend.dispose()
      const c=echarts.init(el); this.charts.trend=c
      const total=this.warningStats.reduce((s,x)=>s+x.value,0)
      if(!total){ c.setOption({backgroundColor:'transparent',graphic:[{type:'text',left:'center',top:'middle',style:{text:'今日暂无预警数据',fill:'#8ba6c8',fontSize:14}}]}); return }
      c.setOption({
        backgroundColor:'transparent',
        tooltip:{trigger:'axis',axisPointer:{type:'shadow'},backgroundColor:'rgba(8,13,35,0.92)',borderColor:'rgba(0,212,255,0.25)',textStyle:{color:'#e0f0ff',fontSize:12},formatter:p=>`${p[0].name}：<b style="color:${this.warningStats[p[0].dataIndex]?.color||'#00d4ff'}">${p[0].value}</b> 次`},
        grid:{left:'5%',right:'5%',top:'12%',bottom:'12%',containLabel:true},
        xAxis:{type:'category',data:this.warningStats.map(x=>x.label),axisLine:{lineStyle:{color:'rgba(0,212,255,0.18)'}},axisTick:{show:false},axisLabel:{color:'#a8c5e6',fontSize:12}},
        yAxis:{type:'value',axisLine:{show:false},axisTick:{show:false},splitLine:{lineStyle:{color:'rgba(0,212,255,0.07)',type:'dashed'}},axisLabel:{color:'#8ba6c8',fontSize:10}},
        series:[{type:'bar',barWidth:'40%',data:this.warningStats.map(x=>({value:x.value,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:x.color},{offset:1,color:x.color+'55'}]),borderRadius:[6,6,0,0]}})),label:{show:true,position:'top',color:'#e0f0ff',fontSize:13,fontWeight:'bold',fontFamily:'Consolas'}}]
      })
    },

    // 右侧面板：环形图
    initDonutChart() {
      const el = this.$refs.donutRef; if(!el) return
      if(this.charts.donut) this.charts.donut.dispose()
      const c = echarts.init(el); this.charts.donut = c
      const total = this.warningStats.reduce((s,x)=>s+x.value,0)
      if(!total) {
        c.setOption({backgroundColor:'transparent',graphic:[{type:'text',left:'center',top:'middle',style:{text:'暂无数据',fill:'#8ba6c8',fontSize:12}}]})
        return
      }
      c.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger:'item', backgroundColor:'rgba(8,13,35,.95)', borderColor:'rgba(0,212,255,.3)', textStyle:{color:'#e0f0ff',fontSize:11}, formatter:p=>`${p.name}：${p.value} (${p.percent}%)` },
        series: [{ type:'pie', radius:['50%','75%'], center:['50%','50%'], label:{show:false}, emphasis:{scale:false},
          data: this.warningStats.map(x=>({ value:x.value, name:x.label, itemStyle:{color:x.color} })) }]
      })
    },
    donutPct(val) {
      const total = this.warningStats.reduce((s,x)=>s+x.value,0)
      return total ? Math.round(val/total*100)+'%' : '0%'
    },

    // 右侧面板：TOP5 点击打开详情
    openDetailByUser(u) {
      const first = this.warningList.find(x=>(x.empCode||x.userCode)===u.empCode)
      if(first) this.openDetail(first)
    },
    levelRank(lv) { return {高:3,危险:3,中:2,警告:2,低:1,提醒:1}[lv]||0 },
    levelLabel(lv) { return lv==='高'||lv==='危险'?'高危':lv==='中'||lv==='警告'?'中危':'注意' },
    levelClass(lv) { return lv==='高'||lv==='危险'?'danger':lv==='中'||lv==='警告'?'warn':'info' },

    switchPeriod(val) { if(this.activePeriod===val)return; this.activePeriod=val; this.fetchData() },
    async openDetail(item) {
      this.detailRow = item
      this.detailVisible = true
      this.vitalLoading = true
      this.vitalEmpty = false
      this.vitalSummary = []
      await this.$nextTick()
      await this.loadVitalChart(item)
    },

    async loadVitalChart(item) {
      try {
        const warnTime = dayjs(item.createTime)
        const empCode  = item.empCode || item.userCode || item.empId || ''
        let list = []
        let windowHours = 0  // 记录实际用的时间窗口
        let isDaily = false   // 是否降级为按天

        // ── 第一层：自动扩窗，±1h → ±6h，直到数据点 ≥ 3 ──
        for (const hours of [1, 3, 6]) {
          const start = warnTime.subtract(hours, 'hour')
          const end   = warnTime.add(hours, 'hour')
          try {
            const r = await getHealthRecords({
              pageNum: 1, pageSize: 200,
              empCode, userCode: empCode,
              startTime: start.format('YYYY-MM-DD HH:mm:ss'),
              endTime:   end.format('YYYY-MM-DD HH:mm:ss')
            })
            const rows = r?.data?.list || r?.data?.records || (Array.isArray(r?.data) ? r.data : [])
            if (rows.length >= 3) {
              list = rows
              windowHours = hours
              this.vitalChartRange = `${start.format('HH:mm')} – ${end.format('HH:mm')}（±${hours}h）`
              break
            } else if (rows.length > 0 && list.length === 0) {
              // 暂存最好结果，继续尝试更大窗口
              list = rows
              windowHours = hours
              this.vitalChartRange = `${start.format('HH:mm')} – ${end.format('HH:mm')}（±${hours}h）`
            }
          } catch(e) {
            console.warn('[VitalChart] records failed hours=' + hours, e)
          }
        }

        // ── 第二层兜底：数据点仍 < 3，改用 health-portrait 近7天日均趋势 ──
        if (list.length < 3 && empCode) {
          try {
            const { getHealthPortrait } = await import('@/api/health-portrait')
            const pr = await getHealthPortrait(empCode)
            const td = pr?.data?.trendData || pr?.data?.trend || pr?.data
            const dates = td?.dates || []
            const hArr  = td?.heartRates    || td?.heartRate    || []
            const sArr  = td?.bloodOxygens  || td?.bloodOxygen  || []
            const tArr  = td?.temperatures  || td?.temperature  || []
            if (dates.length >= 2) {
              list = dates.map((d, i) => ({
                recordTime: d + ' 00:00:00',
                heartRate:   hArr[i] || null,
                bloodOxygen: sArr[i] || null,
                temperature: tArr[i] || null
              }))
              isDaily = true
              this.vitalChartRange = '近7天日均趋势（采样间隔稀疏）'
            }
          } catch(e2) {
            console.warn('[VitalChart] portrait fallback failed:', e2)
          }
        }

        if (!list.length) { this.vitalEmpty = true; this.vitalLoading = false; return }

        list.sort((a, b) => new Date(a.time||a.recordTime||a.createTime) - new Date(b.time||b.recordTime||b.createTime))

        const times = list.map(d => {
          const t = d.time || d.recordTime || d.createTime
          return isDaily ? dayjs(t).format('MM/DD') : dayjs(t).format('HH:mm')
        })
        const hrs   = list.map(d => d.heartRate   ? +d.heartRate   : null)
        const spo2  = list.map(d => d.bloodOxygen ? +d.bloodOxygen : null)
        const temps = list.map(d => {
          if (!d.temperature) return null
          const t = +d.temperature
          return t > 100 ? +(t / 10).toFixed(1) : +t.toFixed(1)
        })

        const validHr   = hrs.filter(v => v != null)
        const validSpo2 = spo2.filter(v => v != null)
        const validTemp = temps.filter(v => v != null)
        this.vitalSummary = [
          { label:'最高心率', val: validHr.length   ? Math.max(...validHr)+'bpm'  : '--', sub:'正常 60-100', color:'#ef4444' },
          { label:'最低血氧', val: validSpo2.length  ? Math.min(...validSpo2)+'%'  : '--', sub:'正常 ≥95%',   color:'#f97316' },
          { label:'峰值体温', val: validTemp.length  ? Math.max(...validTemp)+'°C' : '--', sub:'正常 36-37.5',color:'#22c55e' },
          { label:'数据点数', val: list.length+'条',                                       sub:'采样点',      color:'#00d4ff' }
        ]

        this.vitalLoading = false
        await this.$nextTick()
        this.renderVitalChart(times, hrs, spo2, temps, isDaily ? '' : warnTime.format('HH:mm'))
      } catch(e) {
        console.error('[VitalChart] error:', e)
        this.vitalEmpty = true
        this.vitalLoading = false
      }
    },

    renderVitalChart(times, hrs, spo2, temps, warnTimeStr) {
      const el = this.$refs.vitalChartRef
      if (!el) return
      if (this.vitalChartInst) this.vitalChartInst.dispose()
      const c = echarts.init(el)
      this.vitalChartInst = c

      // 找预警时刻在 x 轴的索引
      const warnIdx = times.findIndex(t => t >= warnTimeStr)

      c.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(8,13,35,.95)',
          borderColor: 'rgba(0,212,255,.3)',
          textStyle: { color: '#e0f0ff', fontSize: 12 }
        },
        legend: {
          data: ['心率','血氧','体温'],
          top: 4, right: 8,
          textStyle: { color: '#8ba6c8', fontSize: 11 },
          itemWidth: 14, itemHeight: 8
        },
        grid: { left: 14, right: 14, top: 36, bottom: 32, containLabel: true },
        xAxis: {
          type: 'category', data: times, boundaryGap: false,
          axisLine: { lineStyle: { color: 'rgba(0,212,255,.2)' } },
          axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 10, interval: Math.floor(times.length/6) }
        },
        yAxis: [
          // 左轴：心率 40-160，血氧映射到同轴但用右轴刻度显示
          { type:'value', name:'bpm / %',
            nameTextStyle:{color:'#8ba6c8',fontSize:9},
            min: 40, max: 160, interval: 20,
            axisLine:{show:false}, axisTick:{show:false},
            splitLine:{lineStyle:{color:'rgba(0,212,255,.07)',type:'dashed'}},
            axisLabel:{color:'#8ba6c8',fontSize:10} },
          // 右轴：体温 35.0-39.0°C
          { type:'value', name:'°C',
            nameTextStyle:{color:'#22c55e',fontSize:9},
            min: 35, max: 39, interval: 1,
            axisLine:{show:false}, axisTick:{show:false}, splitLine:{show:false},
            axisLabel:{color:'#22c55e',fontSize:10, formatter:v => v.toFixed(1)} },
        ],
        series: [
          {
            name:'心率', type:'line', color:'#ef4444', data: hrs, smooth: true, symbol:'none',
            lineStyle:{color:'#ef4444',width:2},
            areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(239,68,68,.25)'},{offset:1,color:'rgba(239,68,68,.02)'}])},
            markLine: warnIdx >= 0 ? {
              silent: true,
              symbol: ['none','none'],
              lineStyle: { color: '#ff3b3b', width: 2.5, type: 'solid', shadowColor: 'rgba(255,59,59,.6)', shadowBlur: 8 },
              data: [{ xAxis: warnTimeStr, label: {
                show: true, position: 'insideStartTop',
                formatter: '⚠ 预警时刻',
                color: '#fff', fontSize: 11, fontWeight: 700,
                backgroundColor: '#ef4444',
                padding: [3,7,3,7], borderRadius: 4,
                shadowColor: 'rgba(239,68,68,.5)', shadowBlur: 6
              }}]
            } : {},
            markPoint: warnIdx >= 0 && hrs[warnIdx] != null ? {
              data: [{ coord:[warnTimeStr, hrs[warnIdx]], symbol:'circle', symbolSize:14,
                itemStyle:{color:'transparent',borderColor:'#ef4444',borderWidth:3}, label:{show:false} }]
            } : {}
          },
          { name:'血氧', type:'line', color:'#f97316', data: spo2, smooth:true, symbol:'none',
            yAxisIndex: 0,
            lineStyle:{color:'#f97316',width:2},
            areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(249,115,22,.2)'},{offset:1,color:'rgba(249,115,22,.01)'}])},
            markPoint: warnIdx >= 0 && spo2[warnIdx] != null ? {
              data: [{ coord:[warnTimeStr, spo2[warnIdx]], symbol:'circle', symbolSize:14,
                itemStyle:{color:'transparent',borderColor:'#f97316',borderWidth:3}, label:{show:false} }]
            } : {}
          },
          { name:'体温', type:'line', color:'#22c55e', data: temps, smooth:true, symbol:'none',
            yAxisIndex:1, lineStyle:{color:'#22c55e',width:2},
            markPoint: warnIdx >= 0 && temps[warnIdx] != null ? {
              data: [{ coord:[warnTimeStr, temps[warnIdx]], symbol:'circle', symbolSize:14,
                itemStyle:{color:'transparent',borderColor:'#22c55e',borderWidth:3}, label:{show:false} }]
            } : {}
          }
        ]
      })
    },

    onDrawerClose() {
      if (this.vitalChartInst) { this.vitalChartInst.dispose(); this.vitalChartInst = null }
    },

    // FIX ①③: 翻页同步重置滚动，不打架
    jumpPage(page) {
      this.currentPage=page
      this.$nextTick(()=>{ const el=this.$refs.listRef; if(el){el.scrollTop=0;this.scrollTop=0} })
    },

    // FIX ③: 精细化自动滚动控制
    startAutoScroll() {
      if(this.scrollTimer) clearInterval(this.scrollTimer)
      this.scrollTimer=setInterval(()=>{
        if(this.autoScrollPaused) return
        const el=this.$refs.listRef; if(!el) return
        const max=el.scrollHeight-el.clientHeight
        if(max<=0) return
        if(this.scrollTop>=max){
          this.autoScrollPaused=true
          setTimeout(()=>{ this.scrollTop=0; if(el)el.scrollTop=0; this.autoScrollPaused=false },2000)
        } else { this.scrollTop+=1; el.scrollTop=this.scrollTop }
      },40)
    },
    pauseAutoScroll()  { this.autoScrollPaused=true },
    resumeAutoScroll() { if(!this._manualPause) this.autoScrollPaused=false },
    toggleAutoScroll() {
      this._manualPause=!this._manualPause
      this.autoScrollPaused=this._manualPause
      if(!this._manualPause){ const el=this.$refs.listRef; if(el) this.scrollTop=el.scrollTop }
    },

    warnClass(level) {
      if(!level) return 'normal'
      if(['高','危险'].includes(level)) return 'danger'
      if(['中','警告'].includes(level)) return 'warn'
      if(['低','提醒'].includes(level)) return 'info'
      return 'normal'
    },
    statBarWidth(val) { return val/this.statMax*100 },
    fmtTime(ts)     { return ts?dayjs(ts).format('MM-DD HH:mm'):'--' },
    fmtTimeFull(ts) { return ts?dayjs(ts).format('YYYY-MM-DD HH:mm:ss'):'--' },
    setScale() {
      const el=this.$el; if(!el) return
      const vw=(el.parentElement?el.parentElement.clientWidth:window.innerWidth)-160
      const vh=window.innerHeight-48
      const scale=Math.max(0.4,Math.min(2,Math.min(vw/1920,vh/1030)))
      el.style.transformOrigin='top left'; el.style.transform=`scale(${scale})`
      el.style.width=`${(1/scale)*100}%`; el.style.height=`${(1/scale)*vh}px`
    },
    handleResize() {
      clearTimeout(this.resizeTimer)
      this.resizeTimer=setTimeout(()=>{ this.setScale(); this.$nextTick(()=>Object.values(this.charts).forEach(c=>c&&c.resize&&c.resize())) },200)
    }
  }
}
</script>

<style lang="scss" scoped>
$bg:     #080d1e;
$panel:  rgba(8,16,42,0.88);
$border: rgba(0,212,255,0.14);
$accent: #00d4ff;
$text:   #a8c5e6;
$dim:    #6a88ab;
$white:  #e8f4ff;

.rw-root {
  width:1920px; height:1032px; background:$bg;
  background-image: radial-gradient(circle at 18% 28%,rgba(239,68,68,.05) 0%,transparent 48%),radial-gradient(circle at 82% 72%,rgba(42,82,152,.08) 0%,transparent 48%);
  overflow:hidden; display:flex; flex-direction:column;
  font-family:'Microsoft YaHei',sans-serif; color:$text;
}

/* Header */
.rw-hd { height:52px; flex-shrink:0; display:flex; align-items:center; padding:0 20px; gap:16px; background:rgba(0,6,24,.65); border-bottom:1px solid $border; }
.rw-hd-left { display:flex; align-items:center; gap:10px; flex-shrink:0; }
.rw-live-dot { width:9px; height:9px; border-radius:50%; background:#ef4444; box-shadow:0 0 8px #ef4444; animation:rwPulse 2s ease-in-out infinite; }
@keyframes rwPulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:.45;transform:scale(.75)} }
.rw-hd-title { font-size:20px; font-weight:700; color:$white; margin:0; letter-spacing:2px; text-shadow:0 0 14px rgba(239,68,68,.45); }
.rw-hd-kpis { flex:1; display:flex; justify-content:center; }
.rw-kpi { display:flex; flex-direction:column; align-items:center; padding:0 20px; border-right:1px solid $border; &:first-child{border-left:1px solid $border;} }
.rw-kpi-n { font-size:18px; font-weight:800; font-family:'Consolas',monospace; line-height:1.1;
  &.kpi-red    { color:#ef4444; text-shadow:0 0 10px rgba(239,68,68,.5); }
  &.kpi-orange { color:#f97316; text-shadow:0 0 10px rgba(249,115,22,.4); }
  &.kpi-purple { color:#a855f7; text-shadow:0 0 10px rgba(168,85,247,.35); }
  &.kpi-cyan   { color:$accent; text-shadow:0 0 10px rgba(0,212,255,.4); }
}
.rw-kpi-l { font-size:11px; color:$dim; margin-top:2px; white-space:nowrap; }
.rw-hd-time { flex-shrink:0; font-family:'Consolas',monospace; font-size:13px; color:$dim; }

/* Body */
.rw-bd { flex:1; display:flex; gap:8px; padding:8px 12px 8px 12px; overflow:hidden; min-height:0; }

/* Aside */
.rw-aside { width:320px; flex-shrink:0; display:flex; flex-direction:column; gap:8px; }
.rw-aside-top { height:190px; flex-shrink:0; }
.rw-aside-bot { flex:1; min-height:0; }
.rw-stat-list { padding:8px 14px; display:flex; flex-direction:column; gap:8px; }
.rw-stat-row { display:flex; align-items:center; gap:8px; }
.rw-stat-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.rw-stat-name { font-size:12px; color:$white; width:56px; flex-shrink:0; }
.rw-stat-bar-wrap { flex:1; height:6px; background:rgba(255,255,255,.06); border-radius:3px; overflow:hidden; }
.rw-stat-bar { height:100%; border-radius:3px; transition:width .8s ease; opacity:.85; }
.rw-stat-val { font-size:13px; font-weight:700; font-family:'Consolas',monospace; width:32px; text-align:right; flex-shrink:0; }

/* Main */
.rw-main { flex:1; display:flex; flex-direction:column; gap:8px; min-width:0; }
.rw-panel-trend { height:260px; flex-shrink:0; }
.rw-panel-list  { flex:1; min-height:0; }

/* Panel */
.rw-panel { background:$panel; border:1px solid $border; border-radius:10px; display:flex; flex-direction:column; overflow:hidden; backdrop-filter:blur(8px); }
.rw-ph { height:36px; flex-shrink:0; display:flex; align-items:center; gap:8px; padding:0 12px; border-bottom:1px solid rgba(0,212,255,.09); background:rgba(0,212,255,.035); }
.rw-ph-bar { width:3px; height:14px; background:linear-gradient(180deg,#ef4444,rgba(239,68,68,.3)); border-radius:2px; box-shadow:0 0 6px rgba(239,68,68,.7); flex-shrink:0; }
.rw-ph-title { font-size:13px; font-weight:600; color:$white; letter-spacing:1px; flex-shrink:0; }
.rw-pc { flex:1; min-height:0; padding:6px; }
.rw-rt-total { font-size:11px; color:$dim; flex-shrink:0; }

/* FIX ⑤: 筛选栏 */
.rw-filter-bar { margin-left:auto; display:flex; align-items:center; gap:6px; }
.rw-filter-input {
  height:26px; padding:0 8px; width:110px;
  background:rgba(0,212,255,.06); border:1px solid rgba(0,212,255,.2);
  border-radius:4px; color:$white; font-size:11px; outline:none; transition:border-color .2s;
  &::placeholder { color:$dim; }
  &:focus { border-color:rgba(0,212,255,.5); }
}
.rw-filter-select {
  height:26px; padding:0 6px;
  background:rgba(0,212,255,.06); border:1px solid rgba(0,212,255,.2);
  border-radius:4px; color:$white; font-size:11px; outline:none; cursor:pointer;
  option { background:#0d1228; color:$white; }
}
.rw-scroll-btn {
  height:26px; padding:0 10px;
  background:rgba(0,212,255,.07); border:1px solid rgba(0,212,255,.2);
  border-radius:4px; color:$accent; font-size:11px; cursor:pointer; white-space:nowrap; transition:background .2s;
  &:hover { background:rgba(0,212,255,.16); }
}

/* FIX ①: 7列 含部门 */
.rw-list-hd {
  display:grid;
  grid-template-columns: 40px 60px 80px 96px 54px 96px 70px 84px;
  gap:0; padding:5px 14px; flex-shrink:0; background:rgba(0,212,255,.055);
  border-bottom:1px solid rgba(0,212,255,.1);
  span { font-size:11px; color:$dim; font-weight:600; padding:0 4px; }
}
.rw-list-body {
  flex:1; overflow-y:auto; padding:4px 8px; min-height:0;
  &::-webkit-scrollbar { width:3px; }
  &::-webkit-scrollbar-thumb { background:rgba(0,212,255,.18); border-radius:2px; }
}
.rw-list-row {
  display:grid;
  grid-template-columns: 40px 60px 80px 96px 54px 96px 70px 84px;
  gap:0; padding:7px 6px; margin-bottom:1px;
  border-radius:6px; align-items:center; border-left:3px solid transparent;
  transition:background .15s; cursor:pointer;
  &:hover { background:rgba(0,212,255,.055); }
  &:nth-child(even) { background:rgba(255,255,255,.015); }
  &:nth-child(even):hover { background:rgba(0,212,255,.055); }
  &.danger { border-left-color:rgba(239,68,68,.75);  background:rgba(239,68,68,.03) !important; }
  &.warn   { border-left-color:rgba(249,115,22,.75); background:rgba(249,115,22,.03) !important; }
  &.info   { border-left-color:rgba(59,130,246,.6); }
  &.normal { border-left-color:rgba(82,196,26,.5);  }
}
.rw-list-idx  { font-size:11px; color:$dim; text-align:center; }
.rw-list-name { font-size:13px; color:$white; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-weight:500; }
.rw-list-dept { font-size:12px; color:$text; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.rw-list-type { font-size:12px; color:$text; }
.rw-list-badge {
  font-size:11px; padding:2px 8px; border-radius:4px; text-align:center; white-space:nowrap; display:inline-block;
  &.danger { background:rgba(239,68,68,.14); color:#ef4444; border:1px solid rgba(239,68,68,.3); }
  &.warn   { background:rgba(249,115,22,.14); color:#f97316; border:1px solid rgba(249,115,22,.3); }
  &.info   { background:rgba(59,130,246,.14); color:#3b82f6; border:1px solid rgba(59,130,246,.3); }
  &.normal { background:rgba(82,196,26,.12);  color:#52c41a; border:1px solid rgba(82,196,26,.25); }
}
.rw-list-val  { font-size:13px; font-weight:700; font-family:'Consolas',monospace; color:#f97316; }
.rw-list-handled {
  font-size:11px; padding:2px 8px; border-radius:4px; text-align:center; white-space:nowrap; display:inline-block;
  &.handled { background:rgba(56,239,125,.1); color:#38ef7d; border:1px solid rgba(56,239,125,.25); }
  &.pending { background:rgba(255,210,0,.1);  color:#ffd200; border:1px solid rgba(255,210,0,.25); }
}
.rw-list-time { font-size:11px; color:$dim; }
.rw-list-empty { text-align:center; padding:40px 0; color:$dim; font-size:13px; }

/* 分页 */
.rw-list-pg { height:36px; flex-shrink:0; display:flex; align-items:center; justify-content:center; gap:5px; border-top:1px solid rgba(0,212,255,.1); }
.rw-pg-btn { height:22px; padding:0 7px; background:rgba(0,212,255,.07); border:1px solid rgba(0,212,255,.18); border-radius:3px; color:$accent; font-size:12px; cursor:pointer; transition:background .2s; &:hover:not(:disabled){background:rgba(0,212,255,.16);} &:disabled{opacity:.28;cursor:not-allowed;} }
.rw-pg-info { font-size:12px; color:$accent; min-width:44px; text-align:center; }

/* 其他 */
.rw-period-tabs { display:flex; background:rgba(0,212,255,.06); border:1px solid rgba(0,212,255,.2); border-radius:6px; overflow:hidden; flex-shrink:0; }
.rw-period-tab { padding:4px 14px; font-size:12px; color:$dim; cursor:pointer; transition:all .2s; &:hover{color:$white;background:rgba(0,212,255,.1);} &.is-active{color:$bg;background:$accent;font-weight:700;} }
.rw-trend-tags { margin-left:12px; display:flex; gap:8px; }
.rw-tag { font-size:10px; padding:2px 6px; border-radius:3px; border:1px solid; }

/* Badges (shared) */
.badge-danger  { color:#ef4444; background:rgba(239,68,68,.14); border:1px solid rgba(239,68,68,.3); padding:2px 8px; border-radius:4px; font-size:12px; }
.badge-warn    { color:#f97316; background:rgba(249,115,22,.14); border:1px solid rgba(249,115,22,.3); padding:2px 8px; border-radius:4px; font-size:12px; }
.badge-info    { color:#3b82f6; background:rgba(59,130,246,.14); border:1px solid rgba(59,130,246,.3); padding:2px 8px; border-radius:4px; font-size:12px; }
.badge-normal  { color:#52c41a; background:rgba(82,196,26,.12);  border:1px solid rgba(82,196,26,.25); padding:2px 8px; border-radius:4px; font-size:12px; }
.badge-handled { color:#38ef7d; background:rgba(56,239,125,.12); border:1px solid rgba(56,239,125,.3); padding:2px 8px; border-radius:4px; font-size:12px; }
.badge-pending { color:#ffd200; background:rgba(255,210,0,.12);  border:1px solid rgba(255,210,0,.28); padding:2px 8px; border-radius:4px; font-size:12px; }

/* Drawer body */
.rw-dw-wrap { padding:16px 20px; display:flex; flex-direction:column; gap:14px; height:100%; overflow-y:auto;
  &::-webkit-scrollbar{width:3px;} &::-webkit-scrollbar-thumb{background:#232b4d;border-radius:2px;} }

/* Info card */
.rw-dw-card { display:flex; align-items:center; gap:12px; background:rgba(0,212,255,.04); border:1px solid rgba(0,212,255,.15); border-radius:10px; padding:12px 16px; }
.rw-dw-avatar { width:44px; height:44px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2); display:flex; align-items:center; justify-content:center; font-size:18px; font-weight:700; color:#fff; flex-shrink:0; }
.rw-dw-main { flex:1; }
.rw-dw-name { font-size:16px; font-weight:700; color:#e8f4ff; }
.rw-dw-sub  { font-size:11px; color:#6a88ab; margin-top:3px; }
.rw-dw-tags { display:flex; flex-direction:column; gap:5px; align-items:flex-end; flex-shrink:0; }

/* KV row */
.rw-dw-kvrow { display:flex; gap:10px; flex-wrap:wrap; }
.rw-dw-kv    { background:rgba(255,255,255,.03); border:1px solid rgba(0,212,255,.1); border-radius:7px; padding:8px 12px; flex:1; min-width:100px; }
.rw-dw-k     { font-size:10px; color:#6a88ab; display:block; margin-bottom:3px; }
.rw-dw-v     { font-size:13px; color:#c8d8e8; font-weight:500; }
.rw-dw-num   { font-family:'Consolas',monospace; font-size:15px; color:#f97316; font-weight:700; }

/* Chart section */
.rw-dw-chart-section { background:rgba(0,0,0,.2); border:1px solid rgba(0,212,255,.12); border-radius:10px; padding:12px; display:flex; flex-direction:column; gap:8px; flex:1; min-height:280px; }
.rw-dw-chart-hd { display:flex; justify-content:space-between; align-items:center; }
.rw-dw-chart-title { font-size:12px; font-weight:600; color:#c8d8e8; }
.rw-dw-chart-range { font-size:11px; color:#6a88ab; font-family:'Consolas',monospace; }
.rw-dw-chart { flex:1; min-height:220px; }

/* Loading / empty */
.rw-dw-loading { display:flex; align-items:center; justify-content:center; gap:10px; flex:1; color:#6a88ab; font-size:13px; }
.rw-dw-spinner { width:18px; height:18px; border-radius:50%; border:2px solid rgba(0,212,255,.2); border-top-color:#00d4ff; animation:rwSpin .8s linear infinite; }
@keyframes rwSpin { to{transform:rotate(360deg)} }
.rw-dw-empty { display:flex; flex-direction:column; align-items:center; justify-content:center; flex:1; gap:8px; color:#6a88ab; font-size:20px;
  p { font-size:12px; margin:0; } }

/* Summary */
.rw-dw-summary { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; }
.rw-dw-si { background:rgba(255,255,255,.03); border-radius:6px; padding:8px 10px; text-align:center; }
.rw-dw-si-label { font-size:10px; color:#6a88ab; margin-bottom:3px; }
.rw-dw-si-val   { font-size:15px; font-weight:700; font-family:'Consolas',monospace; }
.rw-dw-si-sub   { font-size:9px; color:#4a5578; margin-top:2px; }

/* ══ 列表+右侧面板布局 ══ */
.rw-panel-list { flex-direction: row !important; }
.rw-list-wrap  { flex:1; min-width:0; display:flex; flex-direction:column; overflow:hidden; }
.rw-right-col  {
  width:320px; flex-shrink:0; display:flex; flex-direction:column; gap:8px;
  padding:8px 8px 8px 8px; overflow-y:auto;
  &::-webkit-scrollbar { width:2px; }
  &::-webkit-scrollbar-thumb { background:rgba(0,212,255,.15); border-radius:2px; }
}

/* 右侧子面板 header */
.rw-sub-ph {
  height:32px; flex-shrink:0; display:flex; align-items:center; gap:8px;
  padding:0 12px; border-bottom:1px solid rgba(0,212,255,.09); background:rgba(0,212,255,.03);
}

/* 体征均值卡 */
.rw-vc-panel { background:$panel; border:1px solid $border; border-radius:10px; overflow:hidden; flex-shrink:0; }
.rw-vc-row   { display:grid; grid-template-columns:1fr 1fr 1fr; gap:6px; padding:8px; }
.rw-vc       { background:rgba(0,212,255,.04); border-radius:8px; padding:10px 10px 8px; }
.rw-vc-label { font-size:9px; color:$dim; letter-spacing:.5px; margin-bottom:3px; }
.rw-vc-val   { font-size:19px; font-weight:800; font-family:'Consolas',monospace; line-height:1.1; }
.rw-vc-sub   { font-size:9px; color:$dim; margin-top:2px; }
.rw-vc-bar-track { height:3px; background:rgba(255,255,255,.06); border-radius:2px; margin-top:5px; overflow:hidden; }
.rw-vc-bar   { height:100%; border-radius:2px; transition:width .8s; opacity:.8; }

/* 环形图面板 */
.rw-donut-panel { flex-shrink:0; }
.rw-donut-body  { display:flex; align-items:center; padding:8px; gap:8px; }
.rw-donut-chart { width:80px; height:80px; flex-shrink:0; }
.rw-donut-legend { flex:1; display:flex; flex-direction:column; gap:6px; }
.rw-donut-leg-row { display:flex; align-items:center; gap:5px; }
.rw-donut-dot  { font-size:10px; flex-shrink:0; }
.rw-donut-lname { font-size:11px; color:$text; flex:1; }
.rw-donut-lval  { font-size:12px; font-weight:700; font-family:'Consolas',monospace; min-width:28px; text-align:right; }
.rw-donut-lpct  { font-size:10px; color:$dim; min-width:32px; text-align:right; }

/* 处理进度 */
.rw-prog-panel  { flex-shrink:0; }
.rw-prog-body   { padding:8px 12px; display:flex; flex-direction:column; gap:8px; }
.rw-prog-hd     { display:flex; justify-content:space-between; margin-bottom:4px; }
.rw-prog-name   { font-size:11px; color:$text; }
.rw-prog-cnt    { font-size:11px; font-family:'Consolas',monospace; }
.rw-prog-track  { height:10px; background:rgba(255,255,255,.05); border-radius:5px; overflow:hidden; position:relative; }
.rw-prog-fill   { height:100%; border-radius:5px; background:linear-gradient(90deg,#22c55e,rgba(34,197,94,.6)); position:absolute; left:0; top:0; transition:width .8s; }
.rw-prog-total  { font-size:11px; color:$dim; text-align:center; padding-top:6px; border-top:1px solid rgba(0,212,255,.08); }

/* TOP5 */
.rw-top5-panel { flex:1; min-height:0; }
.rw-top5-body  {
  flex:1; overflow-y:auto; padding:6px 8px;
  &::-webkit-scrollbar { width:2px; }
  &::-webkit-scrollbar-thumb { background:rgba(0,212,255,.15); border-radius:2px; }
}
.rw-top5-row {
  display:flex; align-items:center; gap:8px; padding:7px 6px; border-radius:5px; cursor:pointer; transition:background .12s;
  &:hover { background:rgba(0,212,255,.05); }
}
.rw-top5-rank {
  width:18px; height:18px; border-radius:50%; display:flex; align-items:center; justify-content:center;
  font-size:10px; font-weight:700; flex-shrink:0;
  &.rank-1,&.rank-2 { background:rgba(239,68,68,.2);  color:#ef4444; border:1px solid rgba(239,68,68,.4); }
  &.rank-3           { background:rgba(249,115,22,.2); color:#f97316; border:1px solid rgba(249,115,22,.4); }
  &.rank-4,&.rank-5  { background:rgba(255,255,255,.06); color:$dim;  border:1px solid rgba(255,255,255,.1); }
}
.rw-top5-info  { flex:1; min-width:0; }
.rw-top5-name  { font-size:12px; color:$white; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.rw-top5-dept  { font-size:10px; color:$dim; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.rw-top5-count { font-size:14px; font-weight:700; font-family:'Consolas',monospace; color:#ef4444; min-width:24px; text-align:right; flex-shrink:0; }
.rw-top5-badge {
  font-size:9px; padding:1px 5px; border-radius:3px; white-space:nowrap; flex-shrink:0; margin-left:4px;
  &.lvbadge-danger { color:#ef4444; background:rgba(239,68,68,.14);  border:1px solid rgba(239,68,68,.3); }
  &.lvbadge-warn   { color:#f97316; background:rgba(249,115,22,.14); border:1px solid rgba(249,115,22,.3); }
  &.lvbadge-info   { color:#3b82f6; background:rgba(59,130,246,.14); border:1px solid rgba(59,130,246,.3); }
}
</style>

<style lang="scss">
.rw-detail-drawer {
  background:#0d1228!important; border-left:1px solid rgba(0,212,255,.2)!important;
  .el-drawer__header { background:rgba(0,6,24,.8)!important; border-bottom:1px solid rgba(0,212,255,.15)!important; margin-bottom:0!important; padding:16px 20px!important; .el-drawer__title{color:#e8f4ff!important;font-size:15px!important;font-weight:600!important;} }
  .el-drawer__close-btn { color:#6a88ab!important; &:hover{color:#00d4ff!important;} }
  .el-drawer__body { padding:0!important; background:#0d1228!important; }
}
</style>
