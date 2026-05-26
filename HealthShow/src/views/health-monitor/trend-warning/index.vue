<template>
  <div class="tw-page hm-page-shell">
    <PageHeroHeader
      class="tw-hero"
      variant="cockpit"
      eyebrow="Trend Forecast"
      title="健康趋势预警"
      description="基于近 14 天历史数据预测未来 7 天风险，先锁定高风险人群，再按部门和姓名继续收窄。"
    >
      <template #meta>
        <div class="tw-hero-meta">
          <span class="hm-status-chip hm-status-chip--danger">高风险 {{ summary.highRisk || 0 }}</span>
          <span class="hm-status-chip hm-status-chip--warning">中风险 {{ summary.mediumRisk || 0 }}</span>
          <span class="hm-status-chip">最近更新 {{ lastUpdate || '--:--' }}</span>
        </div>
      </template>
      <template #actions>
        <button type="button" class="hm-action-btn" :disabled="loading" @click="fetchData">
          <el-icon><Refresh /></el-icon>
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </template>
    </PageHeroHeader>

    <MetricStrip class="tw-summary-strip" :items="summaryStripItems" dense />

    <!-- 筛选栏 -->
    <div class="tw-filter hm-filter-toolbar">
      <el-select v-model="filterLevel" placeholder="风险等级" clearable size="small" style="width:120px">
        <el-option label="高风险" :value="3" />
        <el-option label="中风险" :value="2" />
        <el-option label="低风险" :value="1" />
      </el-select>
      <el-select v-model="filterDept" placeholder="所属部门" clearable size="small" style="width:140px">
        <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
      </el-select>
      <el-input v-model="filterName" placeholder="姓名搜索" clearable size="small" style="width:130px" />
      <span class="filter-count">共 {{ filteredList.length }} 人次风险</span>
    </div>

    <!-- 风险列表 -->
    <div class="tw-body" v-loading="loading">
      <div v-if="!loading && filteredList.length === 0" class="empty-tip">
        <PageEmptyState
          eyebrow="Trend Queue"
          title="暂无趋势风险人员"
          description="可以切换部门、风险等级或姓名条件，继续排查预测队列。"
        />
      </div>

      <div v-for="emp in pagedList" :key="emp.empCode" class="emp-card"
           :class="'risk-' + emp.riskLevel">
        <!-- 员工基本信息 -->
        <div class="emp-header">
          <div class="emp-info">
            <span class="risk-badge" :class="'badge-' + emp.riskLevel">
              {{ riskLabel(emp.riskLevel) }}
            </span>
            <span class="emp-name">{{ emp.empName }}</span>
            <span class="emp-dept">{{ emp.deptName }}</span>
            <span class="emp-code">{{ emp.empCode }}</span>
          </div>
          <div class="emp-btns">
            <el-button size="small" text @click="goProfile(emp.empCode)">
              查看画像 →
            </el-button>
          </div>
        </div>

        <!-- 风险指标列表 -->
        <div class="metric-row" v-for="m in emp.riskMetrics" :key="m.metric">
          <div class="metric-info">
            <span class="metric-name">{{ m.metricName }}</span>
            <span class="metric-val">当前 {{ m.currentValue }}{{ m.unit }}</span>
            <span class="metric-proj" :class="projClass(m)">
              → 预测7日后 {{ m.projectedValue }}{{ m.unit }}
            </span>
            <span class="metric-thresh">阈值 {{ m.threshold }}{{ m.unit }}</span>
            <el-tag size="small" :type="tagType(m.riskLevel)" style="margin-left:6px">
              {{ m.riskLevel === 3 ? '即将突破' : m.riskLevel === 2 ? '7日内风险' : '趋势预警' }}
            </el-tag>
          </div>
          <!-- 迷你趋势图 -->
          <div class="sparkline-wrap">
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
  </div>
</template>

<script>
import { getTrendWarningPrediction } from '@/api/trend-warning'
import MetricStrip from '@/components/health-shell/MetricStrip.vue'
import PageEmptyState from '@/components/health-shell/PageEmptyState.vue'
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import { Refresh } from '@element-plus/icons-vue'
import SparkLine from './TrendSparkLine.js'

export default {
  name: 'TrendWarning',
  components: { Refresh, SparkLine, PageHeroHeader, MetricStrip, PageEmptyState },
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
    deptOptions() {
      const s = new Set(this.list.map(e => e.deptName).filter(Boolean))
      return [...s].sort()
    },
    summaryStripItems() {
      return [
        { key: 'high', label: '高风险人员', value: this.summary.highRisk || 0, note: '≤3天可能超标', tone: 'danger' },
        { key: 'medium', label: '中风险人员', value: this.summary.mediumRisk || 0, note: '4~7天可能超标', tone: 'warning' },
        { key: 'low', label: '低风险人员', value: this.summary.lowRisk || 0, note: '趋势正在逼近阈值', tone: 'primary' },
        { key: 'normal', label: '正常人员', value: this.summary.normal || 0, note: '暂无异常趋势', tone: 'success' }
      ]
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
    filterLevel() {
      this.currentPage = 1
    },
    filterDept() {
      this.currentPage = 1
    },
    filterName() {
      this.currentPage = 1
    }
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
    tagType(level) {
      return { 3: 'danger', 2: 'warning', 1: '' }[level] || 'info'
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
