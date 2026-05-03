<template>
  <div class="rc-page">
    <!-- ══ 头部 ══ -->
    <div class="rc-header">
      <div class="rc-header-left">
        <span class="rc-title">报表中心</span>
        <span class="rc-sub">月度统计 · 部门对比 · 健康趋势</span>
      </div>
      <div class="rc-header-right">
        <!-- 月份选择 -->
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          size="small"
          format="YYYY-MM"
          value-format="YYYY-MM"
          style="width:130px"
          @change="onMonthChange"
        />
        <el-button size="small" type="primary" plain @click="exportExcel" :loading="exporting">
          导出 Excel
        </el-button>
        <el-button size="small" type="warning" plain @click="exportPdf" :loading="exportingPdf">
          导出 PDF
        </el-button>
      </div>
    </div>

    <!-- ══ Tab ══ -->
    <el-tabs v-model="activeTab" class="rc-tabs" @tab-change="onTabChange">
      <el-tab-pane label="月度报表" name="monthly" />
      <el-tab-pane label="部门对比" name="dept" />
      <el-tab-pane label="健康趋势" name="trend" />
    </el-tabs>

    <!-- ══ 内容区（可导出区域）══ -->
    <div class="rc-content" ref="reportArea" v-loading="loading">
      <div class="rc-overview-row">
        <div
          v-for="card in reportOverviewCards"
          :key="card.label"
          :class="['rc-overview-card', `tone-${card.tone}`]"
        >
          <div class="rc-overview-label">{{ card.label }}</div>
          <div class="rc-overview-value">{{ card.value }}</div>
          <div class="rc-overview-sub">{{ card.sub }}</div>
        </div>
      </div>

      <div class="rc-ai-card">
        <div class="rc-ai-head">
          <span class="rc-ai-title">AI 报表摘要</span>
          <span class="rc-ai-tag">{{ activeTab === 'monthly' ? '月度结论' : activeTab === 'dept' ? '部门结论' : '趋势结论' }}</span>
        </div>
        <div class="rc-ai-lines">
          <div v-for="(line, idx) in insightLines" :key="idx" class="rc-ai-line">
            <span class="rc-ai-dot"></span>
            <span>{{ line }}</span>
          </div>
        </div>
      </div>

      <!-- ── Tab 1: 月度报表 ── -->
      <template v-if="activeTab === 'monthly'">
        <div class="rc-kpi-row">
          <div class="rc-kpi" v-for="k in monthlyKpis" :key="k.label">
            <div class="rc-kpi-val" :style="{ color: k.color }">{{ k.val }}</div>
            <div class="rc-kpi-label">{{ k.label }}</div>
          </div>
        </div>

        <div class="rc-charts-row">
          <div class="rc-chart-card rc-chart-card--half">
            <div class="rc-ch-title">每日记录量</div>
            <div id="rcDailyCountChart" style="height:220px"></div>
          </div>
          <div class="rc-chart-card rc-chart-card--half">
            <div class="rc-ch-title">预警类型分布</div>
            <div id="rcWarnTypeChart" style="height:220px"></div>
          </div>
        </div>

        <div class="rc-table-card">
          <div class="rc-ch-title">月度员工健康明细（共 {{ monthlySummary.length }} 人）</div>
          <el-table :data="monthlySummary" size="small" class="rc-table" stripe max-height="360">
            <el-table-column label="姓名"   min-width="80">
              <template #default="{ row }">{{ row.empName || row.emp_name || '--' }}</template>
            </el-table-column>
            <el-table-column label="部门"   min-width="100">
              <template #default="{ row }">{{ row.deptName || row.dept_name || '--' }}</template>
            </el-table-column>
            <el-table-column label="记录条数" min-width="80" align="center">
              <template #default="{ row }">{{ row.recordCount || row.record_count || 0 }}</template>
            </el-table-column>
            <el-table-column label="心率均值" min-width="80" align="center">
              <template #default="{ row }">{{ fmt1(row.avgHeartRate || row.avg_heart_rate) }}</template>
            </el-table-column>
            <el-table-column label="血氧均值%" min-width="90" align="center">
              <template #default="{ row }">{{ fmt1(row.avgBloodOxygen || row.avg_blood_oxygen) }}</template>
            </el-table-column>
            <el-table-column label="体温均值" min-width="80" align="center">
              <template #default="{ row }">{{ fmt1(row.avgTemperature || row.avg_temperature) }}</template>
            </el-table-column>
            <el-table-column label="健康评分" min-width="80" align="center">
              <template #default="{ row }">
                <span :style="{ color: (row.healthScore || row.health_score||0) >= 90 ? '#4CAF50' : (row.healthScore || row.health_score||0) >= 70 ? '#ffd200' : '#ff5252' }">
                  {{ row.healthScore ?? row.health_score ?? '--' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- ── Tab 2: 部门对比 ── -->
      <template v-if="activeTab === 'dept'">
        <div class="rc-chart-card">
          <div class="rc-ch-title">部门健康汇总对比（当前月）</div>
          <div id="rcDeptBarChart" style="height:300px"></div>
        </div>
        <div class="rc-chart-card">
          <div class="rc-ch-title">部门预警率排行</div>
          <div id="rcDeptWarnChart" style="height:260px"></div>
        </div>
        <div class="rc-table-card">
          <div class="rc-ch-title">部门健康详情</div>
          <el-table :data="deptSummary" size="small" class="rc-table" stripe>
            <el-table-column label="部门"     min-width="120">
              <template #default="{ row }">{{ row.deptName || row.dept_name || '--' }}</template>
            </el-table-column>
            <el-table-column label="在册人数" align="center">
              <template #default="{ row }">{{ row.employeeCount || row.employee_count || 0 }}</template>
            </el-table-column>
            <el-table-column label="近30天预警次数" align="center">
              <template #default="{ row }">
                <span :style="{ color: (row.warningCount||row.warning_count||0) > 0 ? '#ffd200' : '#4CAF50' }">{{ row.warningCount || row.warning_count || 0 }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- ── Tab 3: 健康趋势 ── -->
      <template v-if="activeTab === 'trend'">
        <div class="rc-trend-controls">
          <span class="rc-trend-label">时间范围：</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :label="7">近7天</el-radio-button>
            <el-radio-button :label="30">近30天</el-radio-button>
            <el-radio-button :label="90">近90天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="rc-chart-card">
          <div class="rc-ch-title">心率 / 血氧 / 体温异常率趋势</div>
          <div id="rcTrendChart" style="height:320px"></div>
        </div>
        <div class="rc-chart-card">
          <div class="rc-ch-title">每日检测人次趋势</div>
          <div id="rcPersonTrendChart" style="height:240px"></div>
        </div>
      </template>

    </div>
  </div>
</template>

<script>
import * as echarts from '@/utils/echarts-setup'
import dayjs from 'dayjs'
import request from '@/utils/request'
import { getHtml2Canvas, getJsPDF, getXLSX } from '@/utils/lazy-vendors'

export default {
  name: 'ReportCenter',
  data() {
    return {
      loading: false,
      exporting: false,
      exportingPdf: false,
      activeTab: 'monthly',
      selectedMonth: dayjs().format('YYYY-MM'),
      trendDays: 30,
      // data
      monthlySummary: [],
      deptSummary: [],
      dailyCounts: [],
      warnTypes: [],
      trendData: [],
      charts: {}
    }
  },
  computed: {
    monthlyKpis() {
      if (!this.monthlySummary.length) return []
      const persons = this.monthlySummary.length
      const records = this.monthlySummary.reduce((s, r) => s + (r.recordCount || r.record_count || 0), 0)
      const totalScore = this.monthlySummary.reduce((s, r) => s + (r.healthScore || r.health_score || 0), 0)
      const avgScore = persons > 0 ? (totalScore / persons).toFixed(1) : '--'
      const warnCnt = this.deptSummary.reduce((s, d) => s + (d.warningCount || d.warning_count || 0), 0)
      return [
        { label: '检测人员数', val: persons.toLocaleString(),  color: '#00d4ff' },
        { label: '检测记录条数', val: records.toLocaleString(), color: '#67C23A' },
        { label: '部门预警合计', val: warnCnt.toLocaleString(), color: '#ffd200' },
        { label: '人均健康评分', val: avgScore,                 color: '#a78bfa' }
      ]
    },
    reportOverviewCards() {
      if (this.activeTab === 'monthly') {
        const persons = this.monthlySummary.length
        const records = this.monthlySummary.reduce((s, r) => s + (r.recordCount || r.record_count || 0), 0)
        const topDept = [...this.deptSummary].sort((a, b) => ((b.warningCount || b.warning_count || 0) - (a.warningCount || a.warning_count || 0)))[0]
        return [
          {
            label: '当前月份',
            value: this.selectedMonth,
            sub: '月度统计和导出都基于当前月份',
            tone: 'accent'
          },
          {
            label: '覆盖对象',
            value: `${persons} 人 / ${records} 条`,
            sub: persons ? '已形成月度健康汇总' : '当前月份暂无有效记录',
            tone: persons ? 'info' : 'muted'
          },
          {
            label: '重点部门',
            value: topDept ? (topDept.deptName || topDept.dept_name || '--') : '暂无',
            sub: topDept ? `近30天预警 ${(topDept.warningCount || topDept.warning_count || 0)} 次` : '建议先检查部门统计来源',
            tone: topDept ? 'warn' : 'muted'
          }
        ]
      }

      if (this.activeTab === 'dept') {
        const sorted = [...this.deptSummary].sort((a, b) => ((b.warningCount || b.warning_count || 0) - (a.warningCount || a.warning_count || 0)))
        const topDept = sorted[0]
        const totalEmployees = this.deptSummary.reduce((s, d) => s + (d.employeeCount || d.employee_count || 0), 0)
        return [
          {
            label: '部门数量',
            value: `${this.deptSummary.length} 个`,
            sub: totalEmployees ? `覆盖在册人数 ${totalEmployees}` : '暂无有效部门样本',
            tone: this.deptSummary.length ? 'info' : 'muted'
          },
          {
            label: '最高风险部门',
            value: topDept ? (topDept.deptName || topDept.dept_name || '--') : '暂无',
            sub: topDept ? `预警 ${(topDept.warningCount || topDept.warning_count || 0)} 次` : '当前没有明显高风险部门',
            tone: topDept ? 'danger' : 'muted'
          },
          {
            label: '使用建议',
            value: '先看排行',
            sub: '先锁定高风险部门，再回人员画像与预警中心复核',
            tone: 'accent'
          }
        ]
      }

      const latest = this.trendData[this.trendData.length - 1]
      const dateLabel = latest?.date || '--'
      const activeSignals = [
        ['心率', latest?.heartRateRate ?? latest?.heartRateAbnormalRate ?? 0],
        ['血氧', latest?.bloodOxygenRate ?? latest?.bloodOxygenAbnormalRate ?? 0],
        ['体温', latest?.temperatureRate ?? latest?.temperatureAbnormalRate ?? 0],
        ['压力', latest?.pressureRate ?? latest?.pressureAbnormalRate ?? 0]
      ].sort((a, b) => Number(b[1]) - Number(a[1]))

      return [
        {
          label: '时间范围',
          value: `近 ${this.trendDays} 天`,
          sub: '趋势页适合发现连续性偏移',
          tone: 'accent'
        },
        {
          label: '最新采样日',
          value: dateLabel,
          sub: latest ? `当日检测人次 ${latest.personCount || latest.checkCount || 0}` : '暂无趋势样本',
          tone: latest ? 'info' : 'muted'
        },
        {
          label: '当前主信号',
          value: activeSignals[0] ? activeSignals[0][0] : '暂无',
          sub: activeSignals[0] ? `异常率 ${activeSignals[0][1]}%` : '建议先补齐趋势数据',
          tone: activeSignals[0] && Number(activeSignals[0][1]) >= 10 ? 'danger' : 'warn'
        }
      ]
    },
    insightLines() {
      if (this.activeTab === 'monthly') {
        const persons = this.monthlySummary.length
        const records = this.monthlySummary.reduce((s, r) => s + (r.recordCount || r.record_count || 0), 0)
        const totalScore = this.monthlySummary.reduce((s, r) => s + (r.healthScore || r.health_score || 0), 0)
        const avgScore = persons > 0 ? +(totalScore / persons).toFixed(1) : null
        const topDept = [...this.deptSummary].sort((a, b) => ((b.warningCount || b.warning_count || 0) - (a.warningCount || a.warning_count || 0)))[0]
        return [
          persons > 0
            ? `本月共覆盖 ${persons} 名员工，累计形成 ${records} 条健康记录，人均健康评分 ${avgScore ?? '--'}。`
            : '本月尚未形成有效健康记录，建议先核查统计数据来源和月份选择。',
          topDept
            ? `${topDept.deptName || topDept.dept_name || '--'} 当前为重点关注部门，近30天预警次数最多。`
            : '当前没有明显集中的高风险部门，建议继续关注跨部门波动。',
          avgScore !== null && avgScore < 80
            ? '整体评分偏低，建议优先复盘高频预警类型和班前准入失败原因。'
            : '整体健康水平相对稳定，建议重点关注异常率突增和连续预警人群。'
        ]
      }

      if (this.activeTab === 'dept') {
        const sorted = [...this.deptSummary].sort((a, b) => ((b.warningCount || b.warning_count || 0) - (a.warningCount || a.warning_count || 0)))
        const topDept = sorted[0]
        const quietDept = [...sorted].reverse()[0]
        return [
          topDept
            ? `${topDept.deptName || topDept.dept_name || '--'} 的预警压力最高，应优先作为部门治理对象。`
            : '暂无部门对比数据，无法输出部门级风险结论。',
          quietDept
            ? `${quietDept.deptName || quietDept.dept_name || '--'} 当前预警次数最低，可作为稳定样本部门参考。`
            : '暂无稳定部门样本。',
          '建议将高预警部门与在册人数、岗位风险等级和设备在线率结合分析，避免只看单一次数。'
        ]
      }

      const first = this.trendData[0]
      const last = this.trendData[this.trendData.length - 1]
      const fmtDelta = (key) => {
        const start = Number(first?.[key] ?? first?.[key.replace('Rate', 'AbnormalRate')] ?? 0)
        const end = Number(last?.[key] ?? last?.[key.replace('Rate', 'AbnormalRate')] ?? 0)
        const diff = +(end - start).toFixed(1)
        return `${diff > 0 ? '上升' : diff < 0 ? '下降' : '持平'} ${Math.abs(diff)}%`
      }
      return [
        this.trendData.length
          ? `近 ${this.trendDays} 天心率异常率 ${fmtDelta('heartRateRate')}，血氧异常率 ${fmtDelta('bloodOxygenRate')}。`
          : '暂无趋势数据，无法输出趋势结论。',
        this.trendData.length
          ? `体温异常率 ${fmtDelta('temperatureRate')}，压力异常率 ${fmtDelta('pressureRate')}，建议重点关注拐点日期。`
          : '建议补齐每日趋势数据后再做趋势判断。',
        '趋势页适合发现“连续偏移”，发现问题后建议回到预警中心和人员画像做进一步处置。'
      ]
    }
  },
  mounted() {
    this.loadAll()
  },
  beforeUnmount() {
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    async loadAll() {
      this.loading = true
      try {
        await Promise.allSettled([
          this.loadMonthlySummary(),
          this.loadDailyCounts(),
          this.loadWarnTypes(),
          this.loadDeptSummary(),
          this.loadTrend()
        ])
        this.$nextTick(() => this.initCharts())
      } finally {
        this.loading = false
      }
    },
    async loadMonthlySummary() {
      try {
        const res = await request({ url: '/statistics/monthly-summary', method: 'get', params: { month: this.selectedMonth } })
        this.monthlySummary = (res.code === 200 && Array.isArray(res.data)) ? res.data : []
      } catch (_) { this.monthlySummary = [] }
    },
    async loadDailyCounts() {
      try {
        const res = await request({ url: '/statistics/daily-counts', method: 'get', params: { month: this.selectedMonth } })
        this.dailyCounts = (res.code === 200 && Array.isArray(res.data)) ? res.data : []
      } catch (_) { this.dailyCounts = [] }
    },
    async loadWarnTypes() {
      try {
        const res = await request({ url: '/statistics/warning-types', method: 'get', params: { month: this.selectedMonth } })
        this.warnTypes = (res.code === 200 && Array.isArray(res.data)) ? res.data : []
      } catch (_) { this.warnTypes = [] }
    },
    async loadDeptSummary() {
      try {
        const res = await request({ url: '/statistics/dept-summary', method: 'get' })
        this.deptSummary = (res.code === 200 && Array.isArray(res.data)) ? res.data : []
      } catch (_) { this.deptSummary = [] }
    },
    async loadTrend() {
      try {
        const res = await request({ url: '/dashboard/daily-trend', method: 'get', params: { days: this.trendDays } })
        this.trendData = (res.code === 200 && Array.isArray(res.data)) ? res.data : []
        if (this.activeTab === 'trend') this.$nextTick(() => this.initTrendCharts())
      } catch (_) { this.trendData = [] }
    },
    onMonthChange() {
      this.loadAll()
    },
    onTabChange() {
      this.$nextTick(() => this.initCharts())
    },
    initCharts() {
      if (this.activeTab === 'monthly') {
        this.initDailyCountChart()
        this.initWarnTypeChart()
      } else if (this.activeTab === 'dept') {
        this.initDeptBarChart()
        this.initDeptWarnChart()
      } else if (this.activeTab === 'trend') {
        this.initTrendCharts()
      }
    },
    initDailyCountChart() {
      const dom = document.getElementById('rcDailyCountChart')
      if (!dom) return
      if (this.charts.daily) this.charts.daily.dispose()
      const chart = echarts.init(dom)
      this.charts.daily = chart
      const data = this.dailyCounts
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,20,50,0.9)', borderColor: '#00d4ff', textStyle: { color: '#fff', fontSize: 11 } },
        grid: { left: 40, right: 20, top: 20, bottom: 40 },
        xAxis: {
          type: 'category',
          data: data.map(d => String(d.day || d.date || '')),
          axisLabel: { color: '#6a7a9a', fontSize: 10 },
          axisLine: { lineStyle: { color: '#1e3a5f' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#6a7a9a', fontSize: 10 },
          splitLine: { lineStyle: { color: 'rgba(100,160,255,0.08)' } }
        },
        series: [{
          type: 'bar',
          data: data.map(d => d.count || d.recordCount || 0),
          barMaxWidth: 16,
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#00d4ff' }, { offset: 1, color: '#00d4ff33' }]), borderRadius: [3, 3, 0, 0] }
        }]
      })
    },
    initWarnTypeChart() {
      const dom = document.getElementById('rcWarnTypeChart')
      if (!dom) return
      if (this.charts.warnType) this.charts.warnType.dispose()
      const chart = echarts.init(dom)
      this.charts.warnType = chart
      const palette = ['#ff5252', '#ffd200', '#00d4ff', '#67C23A', '#a78bfa', '#ff9800']
      const data = this.warnTypes.map((d, i) => ({ name: d.typeName || d.name || d.indicatorName || ('类型' + i), value: d.count || d.value || 0, itemStyle: { color: palette[i % palette.length] } }))
      if (!data.length) {
        chart.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }] })
        return
      }
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item', backgroundColor: 'rgba(10,20,50,0.9)', borderColor: '#00d4ff', textStyle: { color: '#fff', fontSize: 12 }, formatter: '{b}: {c}次 ({d}%)' },
        legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#8ba6c8', fontSize: 11 }, icon: 'circle', itemWidth: 10, itemHeight: 10 },
        series: [{ type: 'pie', radius: ['45%', '70%'], center: ['38%', '50%'], data, label: { show: false } }]
      })
    },
    initDeptBarChart() {
      const dom = document.getElementById('rcDeptBarChart')
      if (!dom) return
      if (this.charts.deptBar) this.charts.deptBar.dispose()
      const chart = echarts.init(dom)
      this.charts.deptBar = chart
      const data = this.deptSummary
      if (!data.length) {
        chart.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }] })
        return
      }
      const getName = d => d.deptName || d.dept_name || '--'
      const getEmp  = d => d.employeeCount || d.employee_count || 0
      const getWarn = d => d.warningCount  || d.warning_count  || 0
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(10,20,50,0.9)', borderColor: '#00d4ff', textStyle: { color: '#fff', fontSize: 11 } },
        legend: { top: 0, textStyle: { color: '#8ba6c8', fontSize: 11 } },
        grid: { left: 90, right: 20, top: 30, bottom: 20 },
        xAxis: { type: 'value', axisLabel: { color: '#6a7a9a', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(100,160,255,0.08)' } } },
        yAxis: { type: 'category', data: data.map(getName), axisLabel: { color: '#8ba6c8', fontSize: 11, width: 80, overflow: 'truncate' }, axisLine: { show: false } },
        series: [
          { name: '在册人数', type: 'bar', data: data.map(getEmp), barMaxWidth: 12, itemStyle: { color: '#00d4ff', borderRadius: [0, 3, 3, 0] } },
          { name: '近30天预警次数', type: 'bar', data: data.map(getWarn), barMaxWidth: 12, itemStyle: { color: '#ff9800', borderRadius: [0, 3, 3, 0] } }
        ]
      })
    },
    initDeptWarnChart() {
      const dom = document.getElementById('rcDeptWarnChart')
      if (!dom) return
      if (this.charts.deptWarn) this.charts.deptWarn.dispose()
      const chart = echarts.init(dom)
      this.charts.deptWarn = chart
      // 按预警次数排序
      const getW = d => d.warningCount || d.warning_count || 0
      const getN = d => d.deptName || d.dept_name || '--'
      const sorted = [...this.deptSummary].sort((a, b) => getW(b) - getW(a))
      if (!sorted.length) {
        chart.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }] })
        return
      }
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', formatter: p => `${p[0].name}：${p[0].value} 次`, backgroundColor: 'rgba(10,20,50,0.9)', borderColor: '#00d4ff', textStyle: { color: '#fff', fontSize: 11 } },
        grid: { left: 90, right: 40, top: 10, bottom: 20 },
        xAxis: { type: 'value', axisLabel: { color: '#6a7a9a', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(100,160,255,0.08)' } } },
        yAxis: { type: 'category', data: sorted.map(getN), axisLabel: { color: '#8ba6c8', fontSize: 11, width: 80, overflow: 'truncate' }, axisLine: { show: false } },
        series: [{
          type: 'bar',
          data: sorted.map(d => { const v = getW(d); return { value: v, itemStyle: { color: v > 10 ? '#ff5252' : v > 3 ? '#ffd200' : '#4CAF50', borderRadius: [0, 4, 4, 0] } } }),
          barMaxWidth: 14,
          label: { show: true, position: 'right', color: '#8ba6c8', fontSize: 10 }
        }]
      })
    },
    initTrendCharts() {
      this.initRcTrendChart()
      this.initPersonTrendChart()
    },
    initRcTrendChart() {
      const dom = document.getElementById('rcTrendChart')
      if (!dom) return
      if (this.charts.trend) this.charts.trend.dispose()
      const chart = echarts.init(dom)
      this.charts.trend = chart
      const data = this.trendData
      if (!data.length) {
        chart.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }] })
        return
      }
      const dates = data.map(d => d.date)
      const series = [
        { name: '心率异常率', key: 'heartRateRate',   color: '#00d4ff' },
        { name: '血氧异常率', key: 'bloodOxygenRate', color: '#67C23A' },
        { name: '体温异常率', key: 'temperatureRate', color: '#ffd200' },
        { name: '压力异常率', key: 'pressureRate',    color: '#a78bfa' }
      ].map(s => ({
        name: s.name, type: 'line', smooth: true,
        data: data.map(d => d[s.key] ?? d[s.key.replace('Rate', 'AbnormalRate')] ?? null),
        lineStyle: { color: s.color, width: 2 },
        itemStyle: { color: s.color }, symbol: 'circle', symbolSize: 4
      }))
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,20,50,0.92)', borderColor: '#00d4ff33', textStyle: { color: '#fff', fontSize: 11 }, formatter: p => `<div style="color:#8ba0bb;margin-bottom:3px">${p[0].axisValue}</div>` + p.map(s => `<div><span style="color:${s.color}">●</span> ${s.seriesName}：<b>${s.value ?? '--'}%</b></div>`).join('') },
        legend: { top: 2, textStyle: { color: '#8ba0bb', fontSize: 11 }, itemWidth: 14, itemHeight: 3 },
        grid: { left: 40, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: dates, axisLabel: { color: '#6a7a9a', fontSize: 10, formatter: v => v.slice(5) }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
        yAxis: { type: 'value', axisLabel: { color: '#6a7a9a', fontSize: 10, formatter: v => v + '%' }, splitLine: { lineStyle: { color: 'rgba(100,160,255,0.08)' } } },
        series
      })
    },
    initPersonTrendChart() {
      const dom = document.getElementById('rcPersonTrendChart')
      if (!dom) return
      if (this.charts.personTrend) this.charts.personTrend.dispose()
      const chart = echarts.init(dom)
      this.charts.personTrend = chart
      const data = this.trendData
      if (!data.length) {
        chart.setOption({ backgroundColor: 'transparent', graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }] })
        return
      }
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,20,50,0.9)', borderColor: '#00d4ff', textStyle: { color: '#fff', fontSize: 11 } },
        grid: { left: 40, right: 20, top: 10, bottom: 30 },
        xAxis: { type: 'category', data: data.map(d => d.date ? d.date.slice(5) : ''), axisLabel: { color: '#6a7a9a', fontSize: 10 }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
        yAxis: { type: 'value', axisLabel: { color: '#6a7a9a', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(100,160,255,0.08)' } } },
        series: [{
          type: 'line', smooth: true,
          data: data.map(d => d.personCount || d.checkCount || 0),
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0,212,255,0.25)' }, { offset: 1, color: 'rgba(0,212,255,0.02)' }]) },
          lineStyle: { color: '#00d4ff', width: 2 },
          itemStyle: { color: '#00d4ff' }
        }]
      })
    },
    // ── 导出 ──
    async exportExcel() {
      this.exporting = true
      try {
        const XLSX = await getXLSX()
        const wb = XLSX.utils.book_new()
        const month = this.selectedMonth

        if (this.monthlySummary.length) {
          const msData = this.monthlySummary.map(r => ({
            '姓名': r.empName || '--',
            '部门': r.deptName || '--',
            '记录条数': r.recordCount || 0,
            '心率均值': this.fmt1(r.avgHeartRate),
            '血氧均值%': this.fmt1(r.avgBloodOxygen),
            '体温均值': this.fmt1(r.avgTemperature),
            '健康评分': r.healthScore ?? '--'
          }))
          XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(msData), `${month}员工月报`)
        }

        if (this.deptSummary.length) {
          const dsData = this.deptSummary.map(r => ({
            '部门': r.deptName || '--',
            '在册人数': r.employeeCount || 0,
            '近30天预警次数': r.warningCount || 0
          }))
          XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(dsData), '部门对比')
        }

        if (this.trendData.length) {
          const tdData = this.trendData.map(r => ({
            '日期': r.date,
            '心率异常率%': r.heartRateRate ?? '',
            '血氧异常率%': r.bloodOxygenRate ?? '',
            '体温异常率%': r.temperatureRate ?? '',
            '压力异常率%': r.pressureRate ?? ''
          }))
          XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(tdData), '健康趋势')
        }

        XLSX.writeFile(wb, `健康报表_${month}.xlsx`)
        this.$message.success('Excel 导出成功')
      } catch (e) {
        this.$message.error('导出失败：' + e.message)
      } finally {
        this.exporting = false
      }
    },
    async exportPdf() {
      this.exportingPdf = true
      try {
        const html2canvas = await getHtml2Canvas()
        const JsPDF = await getJsPDF()
        const el = this.$refs.reportArea
        const canvas = await html2canvas(el, { scale: 1.5, useCORS: true, backgroundColor: '#0a1628', logging: false })
        const imgW = canvas.width / 1.5, imgH = canvas.height / 1.5
        const pdf = new JsPDF({ orientation: imgH > imgW ? 'p' : 'l', unit: 'mm', format: [imgW * 0.264583, imgH * 0.264583] })
        pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 0, 0, imgW * 0.264583, imgH * 0.264583)
        pdf.save(`健康报表_${this.selectedMonth}.pdf`)
        this.$message.success('PDF 导出成功')
      } catch (e) {
        this.$message.error('PDF 导出失败：' + e.message)
      } finally {
        this.exportingPdf = false
      }
    },
    fmt1(val) {
      if (val === null || val === undefined || val === '') return '--'
      return parseFloat(val).toFixed(1)
    },
    warnRateColor(rate) {
      const r = parseFloat(rate) || 0
      if (r >= 15) return '#ff5252'
      if (r >= 8)  return '#ffd200'
      return '#4CAF50'
    }
  }
}
</script>

<style lang="scss" scoped>
.rc-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a1628;
  padding: 16px;
  gap: 12px;
  overflow: hidden;
}

.rc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;

  .rc-header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    .rc-title { font-size: 18px; font-weight: 700; color: #00d4ff; letter-spacing: 1px; }
    .rc-sub   { font-size: 12px; color: #4a6080; }
  }

  .rc-header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  :deep(.el-input__wrapper) { background: #0d2847; border-color: #1a4d8f; }
  :deep(.el-input__inner)   { color: #c8d8e8; }
}

.rc-tabs {
  flex-shrink: 0;

  :deep(.el-tabs__header) { margin-bottom: 0; border-bottom: 1px solid #1a4d8f; }
  :deep(.el-tabs__item)   { color: #8ba6c8; font-size: 13px; }
  :deep(.el-tabs__item.is-active) { color: #00d4ff; }
  :deep(.el-tabs__active-bar)     { background: #00d4ff; }
  :deep(.el-tabs__nav-wrap::after) { background: #1a4d8f; }
}

.rc-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 12px;
}

.rc-overview-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.rc-overview-card {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 212, 255, 0.14);
  background: linear-gradient(135deg, rgba(10, 28, 50, 0.98), rgba(8, 18, 33, 0.95));
}

.rc-overview-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: -18%;
  width: 42%;
  height: 100%;
  background: radial-gradient(circle, rgba(255,255,255,0.12), transparent 68%);
  opacity: 0.55;
}

.rc-overview-card.tone-accent {
  border-color: rgba(0, 212, 255, 0.22);
}

.rc-overview-card.tone-info {
  border-color: rgba(103, 194, 58, 0.2);
}

.rc-overview-card.tone-warn {
  border-color: rgba(255, 210, 0, 0.18);
}

.rc-overview-card.tone-danger {
  border-color: rgba(255, 82, 82, 0.22);
}

.rc-overview-card.tone-muted {
  border-color: rgba(139, 166, 200, 0.16);
}

.rc-overview-label {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: #7fa0be;
}

.rc-overview-value {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  font-size: 24px;
  line-height: 1.1;
  font-weight: 800;
  color: #e7f5ff;
}

.rc-overview-sub {
  position: relative;
  z-index: 1;
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #9fbad5;
}

.rc-ai-card {
  background: linear-gradient(135deg, rgba(11, 38, 68, 0.96), rgba(8, 24, 45, 0.96));
  border: 1px solid rgba(0, 212, 255, 0.18);
  border-radius: 10px;
  padding: 14px 16px;
}

.rc-ai-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.rc-ai-title {
  font-size: 14px;
  font-weight: 700;
  color: #dff2ff;
}

.rc-ai-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(0, 212, 255, 0.08);
  color: #7fe9ff;
  font-size: 11px;
}

.rc-ai-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rc-ai-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #bdd8f2;
  font-size: 12px;
  line-height: 1.7;
}

.rc-ai-dot {
  width: 7px;
  height: 7px;
  margin-top: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #00d4ff;
  box-shadow: 0 0 6px rgba(0, 212, 255, 0.45);
}

.rc-kpi-row {
  display: flex;
  gap: 12px;

  .rc-kpi {
    flex: 1;
    background: #0d2847;
    border-radius: 8px;
    border: 1px solid #1a4d8f;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 14px 8px;

    .rc-kpi-val   { font-size: 26px; font-weight: 800; }
    .rc-kpi-label { font-size: 11px; color: #6a7a9a; margin-top: 4px; }
  }
}

.rc-charts-row {
  display: flex;
  gap: 12px;
}

.rc-chart-card {
  background: #0d2847;
  border-radius: 8px;
  border: 1px solid #1a4d8f;
  padding: 14px;
  flex: 1;

  &--half { flex: 1; }

  .rc-ch-title {
    font-size: 13px;
    font-weight: 600;
    color: #c8d8e8;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    &::before {
      content: '';
      width: 3px; height: 14px;
      background: #00d4ff;
      border-radius: 2px;
      margin-right: 8px;
    }
  }
}

.rc-table-card {
  background: #0d2847;
  border-radius: 8px;
  border: 1px solid #1a4d8f;
  padding: 14px;

  .rc-ch-title {
    font-size: 13px;
    font-weight: 600;
    color: #c8d8e8;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    &::before {
      content: '';
      width: 3px; height: 14px;
      background: #00d4ff;
      border-radius: 2px;
      margin-right: 8px;
    }
  }
}

.rc-table {
  :deep(.el-table__header-wrapper th) {
    background: #0a1e3c !important;
    color: #8ba6c8 !important;
    font-size: 12px;
  }
  :deep(.el-table__row) {
    background: #0d2847 !important;
    color: #c8d8e8;
    font-size: 12px;
  }
  :deep(.el-table__row:hover td) {
    background: #102d52 !important;
  }
  :deep(.el-table__row--striped td) {
    background: #0a1e3c !important;
  }
  :deep(.el-table__inner-wrapper::before) {
    background: #1a4d8f;
  }
  :deep(td.el-table__cell) {
    border-bottom-color: #1a4d8f;
  }
}

.rc-trend-controls {
  display: flex;
  align-items: center;
  gap: 10px;

  .rc-trend-label { font-size: 12px; color: #8ba6c8; }

  :deep(.el-radio-button__inner) {
    background: #0d2847;
    border-color: #1a4d8f;
    color: #8ba6c8;
    font-size: 12px;
  }
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: #00d4ff22;
    border-color: #00d4ff;
    color: #00d4ff;
    box-shadow: -1px 0 0 0 #00d4ff;
  }
}

@media (max-width: 768px) {
  .rc-page {
    height: auto;
    min-height: calc(100vh - 50px);
    overflow: visible;
    padding: 12px;
  }

  .rc-header,
  .rc-header .rc-header-right,
  .rc-kpi-row,
  .rc-charts-row,
  .rc-overview-row {
    display: grid;
    grid-template-columns: 1fr;
  }

  .rc-header .rc-header-right {
    width: 100%;
  }

  .rc-content {
    overflow: visible;
  }
}
</style>
