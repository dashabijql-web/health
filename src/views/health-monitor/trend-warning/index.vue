<template>
  <div class="tw-page">
    <!-- 顶部标题 -->
    <div class="tw-header">
      <div class="tw-title">
        <span class="tw-icon">📈</span>
        <span>健康趋势预警</span>
        <span class="tw-subtitle">基于近 14 天历史数据预测未来 7 天风险</span>
      </div>
      <div class="tw-actions">
        <span class="last-update" v-if="lastUpdate">{{ lastUpdate }} 更新</span>
        <el-button size="small" :loading="loading" @click="fetchData" class="refresh-btn">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="tw-summary" v-loading="loading">
      <div class="sum-card high">
        <div class="sum-num">{{ summary.highRisk || 0 }}</div>
        <div class="sum-label">高风险人员</div>
        <div class="sum-desc">≤3天可能超标</div>
      </div>
      <div class="sum-card medium">
        <div class="sum-num">{{ summary.mediumRisk || 0 }}</div>
        <div class="sum-label">中风险人员</div>
        <div class="sum-desc">4~7天可能超标</div>
      </div>
      <div class="sum-card low">
        <div class="sum-num">{{ summary.lowRisk || 0 }}</div>
        <div class="sum-label">低风险人员</div>
        <div class="sum-desc">趋势向危险方向</div>
      </div>
      <div class="sum-card normal">
        <div class="sum-num">{{ summary.normal || 0 }}</div>
        <div class="sum-label">正常人员</div>
        <div class="sum-desc">暂无异常趋势</div>
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
      <span class="filter-count">共 {{ filteredList.length }} 人次风险</span>
    </div>

    <!-- 风险列表 -->
    <div class="tw-body" v-loading="loading">
      <div v-if="!loading && filteredList.length === 0" class="empty-tip">
        <el-empty description="暂无趋势风险人员" />
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
import { Refresh } from '@element-plus/icons-vue'
import SparkLine from './TrendSparkLine.js'

export default {
  name: 'TrendWarning',
  components: { Refresh, SparkLine },
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
