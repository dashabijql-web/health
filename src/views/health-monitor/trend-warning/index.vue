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
import * as echarts from '@/utils/echarts-setup'
import { defineComponent, h, onMounted, onBeforeUnmount, ref, watch } from 'vue'

// ─── 迷你趋势图子组件（内联）────────────────────────────────────────────
const SparkLine = defineComponent({
  name: 'SparkLine',
  props: {
    data:      { type: Array,  default: () => [] },
    threshold: { type: Number, default: null },
    risky:     { type: String, default: 'high' },
    metric:    { type: String, default: '' }
  },
  setup(props) {
    const el = ref(null)
    let chart = null
    let disposed = false

    function buildChart() {
      if (disposed) return
      if (!el.value || !props.data || props.data.length === 0) return
      if (!chart) chart = echarts.init(el.value)

      const validData = props.data.map((v, i) => [i, v])
      const yVals = props.data.filter(v => v !== null && v !== undefined)
      const minY = yVals.length ? Math.min(...yVals) : 0
      const maxY = yVals.length ? Math.max(...yVals) : 100

      const lineColor = props.risky === 'high'
        ? (props.metric === 'avg_blood_oxygen' ? '#00d4ff' : '#ff6b6b')
        : '#00d4ff'

      const option = {
        backgroundColor: 'transparent',
        grid: { top: 4, right: 4, bottom: 4, left: 4, containLabel: false },
        xAxis: { type: 'category', show: false },
        yAxis: {
          type: 'value', show: false,
          min: Math.floor(Math.min(minY, props.threshold ?? minY) * 0.97),
          max: Math.ceil(Math.max(maxY, props.threshold ?? maxY) * 1.03)
        },
        series: [
          {
            type: 'line', smooth: true, symbol: 'none',
            data: validData,
            lineStyle: { color: lineColor, width: 2 },
            areaStyle: {
              color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: lineColor.replace(')', ',0.35)').replace('rgb', 'rgba') },
                  { offset: 1, color: lineColor.replace(')', ',0.02)').replace('rgb', 'rgba') }
                ]}
            }
          },
          props.threshold !== null ? {
            type: 'line', symbol: 'none', silent: true,
            markLine: {
              silent: true, symbol: 'none',
              data: [{ yAxis: props.threshold }],
              lineStyle: { color: '#ff4444', type: 'dashed', width: 1 }
            }
          } : null
        ].filter(Boolean),
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(13,40,71,0.9)',
          textStyle: { color: '#e8f4fd', fontSize: 11 },
          formatter: p => `第${p[0].dataIndex + 1}天: ${p[0].value[1] ?? '--'}`
        }
      }
      chart.setOption(option)
    }

    onMounted(() => buildChart())
    onBeforeUnmount(() => {
      disposed = true
      if (chart) {
        chart.dispose()
        chart = null
      }
    })
    watch(() => props.data, () => buildChart(), { deep: true })

    return () => h('div', { ref: el, style: 'width:200px;height:54px' })
  }
})

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

<style lang="scss" scoped>
.tw-page {
  padding: 20px;
  min-height: 100%;
  background: #061828;
  color: #e8f4fd;
  font-family: 'Microsoft YaHei', sans-serif;
}

// ─── 头部 ───────────────────────────────────────────────────────
.tw-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .tw-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    font-weight: 700;
    color: #00d4ff;
    .tw-icon { font-size: 24px; }
    .tw-subtitle {
      font-size: 13px;
      color: #6b8fae;
      font-weight: 400;
      margin-left: 8px;
    }
  }

  .tw-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    .last-update { font-size: 12px; color: #4a7090; }
    .refresh-btn { background: transparent; border-color: #1a4d8f; color: #00d4ff; }
  }
}

// ─── 汇总卡片 ────────────────────────────────────────────────────
.tw-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;

  .sum-card {
    border-radius: 10px;
    padding: 18px 20px;
    border: 1px solid;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 3px;
    }

    &.high   { background: rgba(255, 80, 80, 0.08); border-color: rgba(255, 80, 80, 0.3); &::before { background: #ff5050; } }
    &.medium { background: rgba(255, 167, 38, 0.08); border-color: rgba(255, 167, 38, 0.3); &::before { background: #ffa726; } }
    &.low    { background: rgba(255, 236, 64, 0.08); border-color: rgba(255, 236, 64, 0.3); &::before { background: #ffec40; } }
    &.normal { background: rgba(0, 212, 255, 0.06); border-color: rgba(0, 212, 255, 0.2); &::before { background: #00d4ff; } }

    .sum-num {
      font-size: 36px;
      font-weight: 700;
      line-height: 1;
      margin-bottom: 6px;
    }
    .sum-label { font-size: 14px; color: #c0d8ee; margin-bottom: 4px; }
    .sum-desc  { font-size: 12px; color: #5a7590; }

    &.high   .sum-num { color: #ff6b6b; }
    &.medium .sum-num { color: #ffa726; }
    &.low    .sum-num { color: #ffec40; }
    &.normal .sum-num { color: #00d4ff; }
  }
}

// ─── 筛选栏 ────────────────────────────────────────────────────
.tw-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(26, 77, 143, 0.15);
  border-radius: 8px;
  border: 1px solid rgba(26, 77, 143, 0.3);

  .filter-count {
    margin-left: auto;
    font-size: 13px;
    color: #6b8fae;
  }

  :deep(.el-select .el-input__wrapper),
  :deep(.el-input__wrapper) {
    background: rgba(6, 24, 40, 0.8);
    border-color: #1a4d8f;
    box-shadow: none;
    .el-input__inner { color: #e8f4fd; }
  }
}

// ─── 主体列表 ────────────────────────────────────────────────────
.tw-body {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .empty-tip {
    padding: 60px 0;
    :deep(.el-empty__description) { color: #4a6f8c; }
  }
}

.tw-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 6px 0 2px;

  :deep(.el-pagination) {
    --el-pagination-bg-color: #0d2847;
    --el-pagination-button-bg-color: #0d2847;
    --el-pagination-text-color: #8ba6c8;
    --el-pagination-hover-color: #00d4ff;
    --el-pagination-button-disabled-bg-color: rgba(13, 40, 71, 0.45);
  }
}

// ─── 员工风险卡片 ─────────────────────────────────────────────────
.emp-card {
  border-radius: 10px;
  padding: 14px 18px;
  border: 1px solid;
  transition: all 0.2s;

  &.risk-3 { background: rgba(255, 50, 50, 0.06); border-color: rgba(255, 80, 80, 0.35); }
  &.risk-2 { background: rgba(255, 167, 38, 0.06); border-color: rgba(255, 167, 38, 0.3); }
  &.risk-1 { background: rgba(255, 236, 64, 0.05); border-color: rgba(255, 236, 64, 0.25); }

  &:hover { transform: translateX(2px); }

  .emp-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .emp-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .risk-badge {
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 600;
        &.badge-3 { background: rgba(255,80,80,0.2); color: #ff6b6b; border: 1px solid rgba(255,80,80,0.4); }
        &.badge-2 { background: rgba(255,167,38,0.2); color: #ffa726; border: 1px solid rgba(255,167,38,0.4); }
        &.badge-1 { background: rgba(255,236,64,0.15); color: #ffe040; border: 1px solid rgba(255,236,64,0.35); }
      }

      .emp-name  { font-size: 16px; font-weight: 600; color: #e8f4fd; }
      .emp-dept  { font-size: 13px; color: #8ba6c8; }
      .emp-code  { font-size: 12px; color: #4a7090; }
    }
  }

  // ─── 指标行 ──────────────────────────────────────────────────
  .metric-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
    border-top: 1px solid rgba(255,255,255,0.05);

    .metric-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .metric-name  { font-size: 13px; color: #8ba6c8; width: 50px; }
      .metric-val   { font-size: 14px; color: #e8f4fd; }
      .metric-proj  {
        font-size: 14px; font-weight: 600;
        &.proj-danger { color: #ff6b6b; }
        &.proj-warn   { color: #ffa726; }
        &.proj-low    { color: #ffe040; }
      }
      .metric-thresh { font-size: 12px; color: #4a7090; }
    }

    .sparkline-wrap {
      flex-shrink: 0;
      margin-left: 12px;
      border-radius: 6px;
      overflow: hidden;
      background: rgba(0,0,0,0.2);
    }
  }
}
</style>
