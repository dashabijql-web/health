<template>
  <div class="tw-root">

    <!-- ══ 顶部 Header ══ -->
    <header class="tw-hd">
      <div class="tw-hd-left">
        <span class="tw-live-dot"></span>
        <h1 class="tw-hd-title">趋势预警</h1>
      </div>

      <div class="tw-hd-kpis">
        <div class="tw-kpi" v-for="k in headerKpis" :key="k.label">
          <span class="tw-kpi-n" :class="k.cls">{{ k.val }}</span>
          <span class="tw-kpi-l">{{ k.label }}</span>
        </div>
      </div>

      <div class="tw-hd-time">{{ lastUpdate || '--:--' }}</div>
      <button class="hm-export-btn" :disabled="loading" @click="fetchData">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </header>

    <!-- ══ 主体 ══ -->
    <section class="tw-bd" v-loading="loading" element-loading-text="数据加载中..." element-loading-background="rgba(10,20,40,0.7)">

      <!-- 风险摘要卡片 -->
      <div class="tw-summary">
        <div v-for="s in riskSummaryCards" :key="s.key"
             :class="['tw-sum-card', 'sum-' + s.key, filterLevel === s.level ? 'is-active' : '']"
             @click="toggleFilter(s.level)">
          <span class="tw-sum-icon" :style="{ color: s.color }">{{ s.icon }}</span>
          <span class="tw-sum-count" :style="{ color: s.color }">{{ s.count }}</span>
          <span class="tw-sum-label">{{ s.label }}</span>
          <span class="tw-sum-note">{{ s.note }}</span>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="tw-filter">
        <el-select v-model="filterLevel" placeholder="风险等级" clearable size="small" style="width:120px">
          <el-option label="高风险" :value="3" />
          <el-option label="中风险" :value="2" />
          <el-option label="低风险" :value="1" />
        </el-select>
        <el-select v-model="filterDept" placeholder="所属部门" clearable size="small" style="width:140px">
          <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
        </el-select>
        <el-input v-model="filterName" placeholder="姓名搜索" clearable size="small" style="width:130px" />
        <span class="tw-filter-count">共 <em>{{ filteredList.length }}</em> 人次风险</span>
      </div>

      <!-- 风险列表 -->
      <div class="tw-list">
        <div v-if="!loading && filteredList.length === 0" class="tw-empty">
          <span class="tw-empty-icon">✓</span>
          <span class="tw-empty-title">暂无趋势风险人员</span>
          <span class="tw-empty-desc">可切换部门、风险等级或姓名条件继续排查</span>
        </div>

        <div v-for="emp in pagedList" :key="emp.empCode"
             :class="['tw-card', 'risk-' + emp.riskLevel]">
          <div class="tw-card-hd">
            <span class="tw-risk-badge" :class="'badge-' + emp.riskLevel">
              {{ riskLabel(emp.riskLevel) }}
            </span>
            <span class="tw-emp-name">{{ emp.empName }}</span>
            <span class="tw-emp-dept">{{ emp.deptName }}</span>
            <span class="tw-emp-code">{{ emp.empCode }}</span>
            <span class="tw-card-link" @click="goProfile(emp.empCode)">查看画像 →</span>
          </div>

          <div class="tw-metric" v-for="m in emp.riskMetrics" :key="m.metric">
            <span class="tw-m-name">{{ m.metricName }}</span>
            <span class="tw-m-cur">当前 {{ m.currentValue }}{{ m.unit }}</span>
            <span class="tw-m-proj" :class="projClass(m)">
              → 预测 {{ m.projectedValue }}{{ m.unit }}
            </span>
            <span class="tw-m-thresh">阈值 {{ m.threshold }}{{ m.unit }}</span>
            <span class="tw-m-tag" :class="'tag-' + m.riskLevel">
              {{ m.riskLevel === 3 ? '即将突破' : m.riskLevel === 2 ? '7日内风险' : '趋势预警' }}
            </span>
            <div class="tw-sparkline">
              <SparkLine :data="m.history" :threshold="m.threshold"
                         :risky="m.riskDir" :metric="m.metric" />
            </div>
          </div>
        </div>

        <div v-if="!loading && filteredList.length > pageSize" class="tw-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredList.length"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            size="small"
          />
        </div>
      </div>

    </section>
  </div>
</template>

<script>
import { getTrendWarningPrediction } from '@/api/trend-warning'
import SparkLine from './TrendSparkLine.js'

export default {
  name: 'TrendWarning',
  components: { SparkLine },
  data() {
    return {
      loading: false,
      summary: {},
      list: [],
      filterLevel: null,
      filterDept: '',
      filterName: '',
      currentPage: 1,
      pageSize: 50,
      lastUpdate: ''
    }
  },
  computed: {
    headerKpis() {
      const s = this.summary
      return [
        { label: '高风险', val: s.highRisk || 0, cls: 'kpi-red' },
        { label: '中风险', val: s.mediumRisk || 0, cls: 'kpi-orange' },
        { label: '低风险', val: s.lowRisk || 0, cls: 'kpi-yellow' },
        { label: '正常', val: s.normal || 0, cls: 'kpi-green' }
      ]
    },
    riskSummaryCards() {
      const s = this.summary
      return [
        { key: 'high',   level: 3,    label: '高风险', count: s.highRisk || 0,   note: '≤3天可能超标',  color: '#ff5252', icon: 'ALERT' },
        { key: 'medium', level: 2,    label: '中风险', count: s.mediumRisk || 0, note: '4~7天可能超标', color: '#ffa726', icon: 'WARN' },
        { key: 'low',    level: 1,    label: '低风险', count: s.lowRisk || 0,    note: '趋势逼近阈值',  color: '#ffe040', icon: '↑' },
        { key: 'normal', level: null, label: '正常',   count: s.normal || 0,     note: '暂无异常趋势',  color: '#52c41a', icon: 'OK' }
      ]
    },
    deptOptions() {
      const s = new Set(this.list.map(e => e.deptName).filter(Boolean))
      return [...s].sort()
    },
    filteredList() {
      return this.list.filter(e => {
        if (this.filterLevel && e.riskLevel !== this.filterLevel) return false
        if (this.filterDept && e.deptName !== this.filterDept) return false
        if (this.filterName && !String(e.empName || '').includes(this.filterName)) return false
        return true
      })
    },
    pagedList() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredList.slice(start, start + this.pageSize)
    }
  },
  watch: {
    filterLevel() { this.currentPage = 1 },
    filterDept() { this.currentPage = 1 },
    filterName() { this.currentPage = 1 }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getTrendWarningPrediction()
        if (res.code === 200) {
          this.summary = res.data.summary || {}
          this.list    = res.data.list    || []
          const now = new Date()
          this.lastUpdate = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')}`
        }
      } catch (e) {
        this.$message.error('获取趋势预警数据失败')
      } finally {
        this.loading = false
      }
    },
    riskLabel(level) {
      return { 3: '高风险', 2: '中风险', 1: '低风险' }[level] || '未知'
    },
    projClass(m) {
      return m.riskLevel >= 3 ? 'proj-danger' : m.riskLevel >= 2 ? 'proj-warn' : 'proj-low'
    },
    toggleFilter(level) {
      this.filterLevel = this.filterLevel === level ? null : level
    },
    goProfile(empCode) {
      this.$router.push({ path: '/health-monitor/employee-profile', query: { empCode } })
    }
  }
}
</script>

<style scoped lang="scss">
@import './trend-warning.scss';
</style>
