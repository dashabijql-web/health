<template>
  <div class="nf-root">
    <header class="nf-hd">
      <div class="nf-hd-left">
        <span class="nf-live-dot"></span>
        <h1 class="nf-hd-title">消息通知中心</h1>
      </div>
      <div class="nf-hd-kpis">
        <span class="nf-kpi">待处理 <em class="nf-kpi-val danger">{{ unhandledCount }}</em></span>
      </div>
      <div class="nf-hd-time">{{ lastFetchedAt || '--' }}</div>
      <div class="nf-hd-actions">
        <el-select v-model="filter.level" placeholder="全部级别" size="small" clearable style="width:110px" @change="fetchList">
          <el-option label="全部级别" value="" />
          <el-option label="危险" value="3" />
          <el-option label="预警" value="2" />
          <el-option label="提示" value="1" />
        </el-select>
        <el-select v-model="filter.handled" placeholder="全部状态" size="small" clearable style="width:110px" @change="fetchList">
          <el-option label="全部状态" value="" />
          <el-option :label="warningHandledStatusLabel({ handled: false })" :value="false" />
          <el-option :label="warningHandledStatusLabel({ handled: true })" :value="true" />
        </el-select>
        <el-input v-model="filter.userCode" placeholder="搜索员工工号" size="small" clearable style="width:150px" @change="fetchList" />
        <el-button size="small" type="primary" plain @click="handleMarkAllRead" :disabled="unhandledCount === 0">全部已读</el-button>
        <el-button size="small" @click="fetchList" :loading="loading">刷新</el-button>
      </div>
    </header>

    <WarningCenterNav />

    <!-- ── 统计行 ── -->
    <div class="nf-stats-row">
      <div class="notif-stat notif-stat--danger" @click="setLevelFilter('3')">
        <span class="ns-val">{{ levelCounts[3] || 0 }}</span>
        <span class="ns-label">危险</span>
      </div>
      <div class="notif-stat notif-stat--warn" @click="setLevelFilter('2')">
        <span class="ns-val">{{ levelCounts[2] || 0 }}</span>
        <span class="ns-label">预警</span>
      </div>
      <div class="notif-stat notif-stat--info" @click="setLevelFilter('1')">
        <span class="ns-val">{{ levelCounts[1] || 0 }}</span>
        <span class="ns-label">提示</span>
      </div>
      <div class="notif-stat notif-stat--ok" @click="setHandledFilter(false)">
        <span class="ns-val">{{ unhandledCount }}</span>
        <span class="ns-label">待处理</span>
      </div>
    </div>

    <!-- ── 通知列表 ── -->
    <div class="nf-list" v-loading="loading">
      <div
        v-for="item in list"
        :key="item.id"
        :class="['nf-item', `lv-${item.warningLevel}`, item.handled ? 'is-handled' : '']"
        @click="openDetail(item)"
      >
        <div class="ni-left">
          <span :class="['ni-badge', item.warningLevelClass]">{{ item.warningLevelLabel }}</span>
          <div class="ni-info">
            <span class="ni-user">{{ item.empName || item.userName || '--' }}</span>
            <span class="ni-dept">{{ item.deptName || '--' }}</span>
          </div>
        </div>
        <div class="ni-mid">
          <span class="ni-type">{{ item.indicatorName || item.warningType || '--' }}</span>
          <span class="ni-val">{{ item.warningValue || item.indicatorValue || '--' }}</span>
          <span class="ni-sla-tag" :class="`is-${item.slaStatus || 'unknown'}`">{{ item.slaStatusText || '未知' }}</span>
          <span class="ni-sla-clock" :class="`is-${item.slaStatus || 'unknown'}`">
            <span>{{ item.slaClockLabel || '未知' }}</span>
            <strong>{{ item.slaClockText || '--' }}</strong>
          </span>
          <span class="ni-deadline">到期 {{ formatDeadline(item.slaDeadline) }}</span>
        </div>
        <div class="ni-time">{{ formatTime(item.createTime) }}</div>
        <div class="ni-actions" @click.stop>
          <el-button
            v-if="!item.handled"
            size="small"
            type="success"
            plain
            @click="handleSingle(item)"
            :loading="item._loading"
          >处理</el-button>
          <span v-else class="ni-done-tag">{{ warningHandledStatusLabel(item) }}</span>
          <el-button size="small" plain @click="goToUser(item)">画像</el-button>
        </div>
      </div>
      <div v-if="!loading && list.length === 0" class="nf-empty">
        <span>暂无通知消息</span>
      </div>
    </div>

    <!-- ── 分页 ── -->
    <div class="nf-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        background
        size="small"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script>
import WarningCenterNav from '@/components/WarningCenterNav.vue'
import { getRiskWarningList, handleRiskWarning, handleBatchRiskWarning } from '@/api/risk-warning'
import dayjs from 'dayjs'
import { createIntervalTask } from '@/utils/task-timer'
import { buildWarningLifecycleItem, levelLabel, markWarningHandled, warningHandledStatusLabel } from '../common/warning-lifecycle'

export default {
  name: 'NotificationCenter',
  components: { WarningCenterNav },
  data() {
    return {
      loading: false,
      list: [],
      filter: { level: '', handled: false, userCode: '' },
      pagination: { page: 1, size: 20, total: 0 },
      levelCounts: { 1: 0, 2: 0, 3: 0 },
      unhandledCount: 0,
      lastFetchedAt: ''
    }
  },
  mounted() {
    this.fetchList()
    this.fetchSummary()
    this._pollTask = createIntervalTask(() => {
      this.fetchList()
      this.fetchSummary()
    }, 30000)
    this._pollTask.start()
  },
  beforeUnmount() {
    this._pollTask?.stop()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const params = {
          page: this.pagination.page,
          size: this.pagination.size,
          level: this.filter.level || undefined,
          handled: this.filter.handled === '' ? undefined : this.filter.handled,
          userCode: this.filter.userCode || undefined
        }
        const res = await getRiskWarningList(params)
        if (res.code === 200) {
          this.list = (res.data?.list || res.data?.records || []).map(r => buildWarningLifecycleItem({ ...r, _loading: false }))
          this.pagination.total = res.data?.total || 0
          this.lastFetchedAt = dayjs().format('HH:mm:ss')
        }
      } finally {
        this.loading = false
      }
    },
    async fetchSummary() {
      try {
        // 各级别未处理数量：分别查 level=1/2/3 & handled=false
        const [r1, r2, r3, rAll] = await Promise.allSettled([
          getRiskWarningList({ page: 1, size: 1, level: '1', handled: false }),
          getRiskWarningList({ page: 1, size: 1, level: '2', handled: false }),
          getRiskWarningList({ page: 1, size: 1, level: '3', handled: false }),
          getRiskWarningList({ page: 1, size: 1, handled: false })
        ])
        this.levelCounts[1] = r1.status === 'fulfilled' && r1.value.code === 200 ? (r1.value.data?.total || 0) : 0
        this.levelCounts[2] = r2.status === 'fulfilled' && r2.value.code === 200 ? (r2.value.data?.total || 0) : 0
        this.levelCounts[3] = r3.status === 'fulfilled' && r3.value.code === 200 ? (r3.value.data?.total || 0) : 0
        this.unhandledCount = rAll.status === 'fulfilled' && rAll.value.code === 200 ? (rAll.value.data?.total || 0) : 0
      } catch (_) {}
    },
    async handleSingle(item) {
      item._loading = true
      try {
        await handleRiskWarning(item.id)
        markWarningHandled(item)
        this.unhandledCount = Math.max(0, this.unhandledCount - 1)
        this.$message.success('已标记处理')
      } catch (_) {
        this.$message.error('操作失败')
      } finally {
        item._loading = false
      }
    },
    async handleMarkAllRead() {
      const ids = this.list.filter(r => !r.handled).map(r => r.id)
      if (!ids.length) return
      try {
        await handleBatchRiskWarning(ids)
        this.list.forEach(r => { if (!r.handled) markWarningHandled(r) })
        await this.fetchSummary()
        this.$message.success(`已处理 ${ids.length} 条`)
      } catch (_) {
        this.$message.error('批量处理失败')
      }
    },
    goToUser(item) {
      this.$router.push({ path: '/health-monitor/employee-profile', query: { userCode: item.userCode || item.empCode } })
    },
    openDetail(item) {
      if (item.userCode || item.empCode) this.goToUser(item)
    },
    setLevelFilter(lv) {
      this.filter.level = this.filter.level === lv ? '' : lv
      this.pagination.page = 1
      this.fetchList()
    },
    setHandledFilter(val) {
      this.filter.handled = val
      this.pagination.page = 1
      this.fetchList()
    },
    levelLabel(lv) {
      return levelLabel(lv)
    },
    warningHandledStatusLabel,
    formatTime(t) {
      if (!t) return '--'
      return dayjs(t).format('MM-DD HH:mm')
    },
    formatDeadline(t) {
      if (!t || t === '--') return '--'
      return dayjs(t).format('HH:mm')
    }
  }
}
</script>

<style scoped lang="scss">
@import './notifications.scss';
</style>
