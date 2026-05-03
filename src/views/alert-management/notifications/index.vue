<template>
  <div class="notif-page">
    <WarningCenterNav />

    <!-- ── 头部 ── -->
    <div class="notif-header">
      <div class="notif-header-left">
        <span class="notif-title">消息通知中心</span>
        <el-badge :value="unhandledCount" :hidden="unhandledCount === 0" :max="99" type="danger" class="notif-badge">
          <span class="notif-sub">待处理预警</span>
        </el-badge>
      </div>
      <div class="notif-header-right">
        <!-- 筛选 -->
        <el-select v-model="filter.level" placeholder="全部级别" size="small" clearable style="width:110px" @change="fetchList">
          <el-option label="全部级别" value="" />
          <el-option label="危险" value="3" />
          <el-option label="预警" value="2" />
          <el-option label="提示" value="1" />
        </el-select>
        <el-select v-model="filter.handled" placeholder="全部状态" size="small" clearable style="width:110px" @change="fetchList">
          <el-option label="全部状态" value="" />
          <el-option label="未处理" :value="false" />
          <el-option label="已处理" :value="true" />
        </el-select>
        <el-input v-model="filter.userCode" placeholder="搜索员工工号" size="small" clearable style="width:150px" @change="fetchList" />
        <el-button size="small" type="primary" plain @click="handleMarkAllRead" :disabled="unhandledCount === 0">
          全部标记已读
        </el-button>
        <el-button size="small" @click="fetchList" :loading="loading">刷新</el-button>
      </div>
    </div>

    <div class="notif-focus-board">
      <div class="notif-focus-main">
        <div class="notif-focus-label">当前处置优先级</div>
        <div class="notif-focus-headline">{{ boardHeadline }}</div>
        <div class="notif-focus-sub">{{ boardSubline }}</div>
        <div class="notif-focus-tags">
          <span
            v-for="tag in activeFilterTags"
            :key="tag"
            class="notif-focus-tag"
          >{{ tag }}</span>
          <span v-if="activeFilterTags.length === 0" class="notif-focus-tag is-muted">当前查看默认待处理队列</span>
        </div>
      </div>
      <div class="notif-focus-actions">
        <button class="notif-focus-btn tone-accent" @click="goOverview">风险总览</button>
        <button class="notif-focus-btn tone-warn" @click="goRecords">处置记录</button>
        <button class="notif-focus-btn" @click="clearFilters" :disabled="!hasActiveFilters">清空筛选</button>
      </div>
    </div>

    <!-- ── 统计行 ── -->
    <div class="notif-stats-row">
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
    <div class="notif-list" v-loading="loading">
      <div
        v-for="item in list"
        :key="item.id"
        :class="['notif-item', `lv-${item.warningLevel}`, item.handled ? 'is-handled' : '']"
        @click="openDetail(item)"
      >
        <div class="ni-left">
          <span :class="['ni-badge', levelClass(item.warningLevel)]">{{ levelLabel(item.warningLevel) }}</span>
          <div class="ni-info">
            <span class="ni-user">{{ item.empName || item.userName || '--' }}</span>
            <span class="ni-dept">{{ item.deptName || '--' }}</span>
          </div>
        </div>
        <div class="ni-mid">
          <span class="ni-type">{{ item.indicatorName || item.warningType || '--' }}</span>
          <span class="ni-val">{{ item.warningValue || item.indicatorValue || '--' }}</span>
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
          <span v-else class="ni-done-tag">已处理</span>
          <el-button size="small" plain @click="goToUser(item)">画像</el-button>
        </div>
      </div>
      <div v-if="!loading && list.length === 0" class="notif-empty">
        <span>暂无通知消息</span>
      </div>
    </div>

    <!-- ── 分页 ── -->
    <div class="notif-pagination">
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
      lastFetchedAt: '',
      pollTimer: null
    }
  },
  computed: {
    hasActiveFilters() {
      return Boolean(this.filter.level || this.filter.userCode || this.filter.handled === true || this.filter.handled === '')
    },
    activeFilterTags() {
      const tags = []
      if (this.filter.level) tags.push(`级别：${this.levelLabel(this.filter.level)}`)
      if (this.filter.handled === true) tags.push('状态：已处理')
      if (this.filter.handled === '') tags.push('状态：全部')
      if (this.filter.userCode) tags.push(`员工：${this.filter.userCode}`)
      return tags
    },
    boardHeadline() {
      if (this.levelCounts[3] > 0) return `先处理 ${this.levelCounts[3]} 条危险预警`
      if (this.unhandledCount > 0) return `当前仍有 ${this.unhandledCount} 条待处理预警`
      return '当前没有待处理预警'
    },
    boardSubline() {
      const parts = []
      if (this.filter.userCode) parts.push(`当前聚焦员工 ${this.filter.userCode}`)
      if (this.filter.level) parts.push(`已按 ${this.levelLabel(this.filter.level)} 级别筛选`)
      if (this.filter.handled === true) parts.push('当前查看已处理记录')
      if (this.filter.handled === '') parts.push('当前显示全部处理状态')
      if (!parts.length) parts.push('默认展示待处理预警队列，适合做值班闭环处置')
      if (this.lastFetchedAt) parts.push(`最近刷新 ${this.lastFetchedAt}`)
      return parts.join('，')
    }
  },
  mounted() {
    this.fetchList()
    this.fetchSummary()
    this.pollTimer = setInterval(() => { this.fetchList(); this.fetchSummary() }, 30000)
  },
  beforeUnmount() {
    if (this.pollTimer) clearInterval(this.pollTimer)
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
          this.list = (res.data?.list || res.data?.records || []).map(r => ({ ...r, _loading: false }))
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
        item.handled = true
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
        this.list.forEach(r => { if (!r.handled) r.handled = true })
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
    clearFilters() {
      this.filter = { level: '', handled: false, userCode: '' }
      this.pagination.page = 1
      this.fetchList()
    },
    goOverview() {
      this.$router.push('/health-monitor/risk-warning')
    },
    goRecords() {
      this.$router.push('/alert-management/records')
    },
    levelLabel(lv) {
      return { '3': '危险', '2': '预警', '1': '提示', 3: '危险', 2: '预警', 1: '提示' }[lv] || '提示'
    },
    levelClass(lv) {
      return { '3': 'badge-danger', '2': 'badge-warn', '1': 'badge-info', 3: 'badge-danger', 2: 'badge-warn', 1: 'badge-info' }[lv] || 'badge-info'
    },
    formatTime(t) {
      if (!t) return '--'
      return dayjs(t).format('MM-DD HH:mm')
    }
  }
}
</script>

<style lang="scss" scoped>
.notif-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a1628;
  padding: 16px;
  gap: 12px;
  overflow: hidden;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;

  .notif-header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .notif-title {
    font-size: 18px;
    font-weight: 700;
    color: #00d4ff;
    letter-spacing: 1px;
  }

  .notif-sub {
    font-size: 12px;
    color: #8ba6c8;
  }

  .notif-header-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  :deep(.el-select .el-input__wrapper),
  :deep(.el-input .el-input__wrapper) {
    background: #0d2847;
    border-color: #1a4d8f;
  }
  :deep(.el-select .el-input__inner),
  :deep(.el-input .el-input__inner) {
    color: #c8d8e8;
  }
}

.notif-focus-board {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid rgba(0, 212, 255, 0.14);
  background: linear-gradient(135deg, rgba(10, 26, 46, 0.96), rgba(8, 18, 34, 0.96));
}

.notif-focus-label {
  font-size: 12px;
  color: #7fa0be;
}

.notif-focus-headline {
  margin-top: 8px;
  font-size: 24px;
  line-height: 1.1;
  font-weight: 800;
  color: #eef7ff;
}

.notif-focus-sub {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #9eb8d4;
}

.notif-focus-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.notif-focus-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(0, 212, 255, 0.08);
  color: #9fe2ff;
  font-size: 11px;
}

.notif-focus-tag.is-muted {
  background: rgba(139, 166, 200, 0.1);
  color: #8ba6c8;
}

.notif-focus-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.notif-focus-btn {
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(0, 212, 255, 0.16);
  background: rgba(0, 212, 255, 0.06);
  color: #a9e6ff;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s ease;

  &:hover:not(:disabled) {
    background: rgba(0, 212, 255, 0.14);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  &.tone-accent {
    border-color: rgba(0, 212, 255, 0.24);
  }

  &.tone-warn {
    border-color: rgba(255, 210, 0, 0.24);
    color: #ffe37d;
  }
}

.notif-stats-row {
  display: flex;
  gap: 12px;

  .notif-stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px 0;
    border-radius: 8px;
    cursor: pointer;
    transition: opacity .2s;
    border: 1px solid transparent;

    &:hover { opacity: 0.85; border-color: rgba(255,255,255,0.12); }

    .ns-val { font-size: 22px; font-weight: 700; }
    .ns-label { font-size: 11px; margin-top: 2px; color: rgba(255,255,255,0.65); }

    &--danger { background: rgba(255, 82, 82, 0.12); .ns-val { color: #ff5252; } }
    &--warn   { background: rgba(255, 210, 0, 0.10); .ns-val { color: #ffd200; } }
    &--info   { background: rgba(0, 212, 255, 0.08); .ns-val { color: #00d4ff; } }
    &--ok     { background: rgba(76, 175, 80, 0.10);  .ns-val { color: #4CAF50; } }
  }
}

.notif-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 0;

  .notif-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 14px;
    border-radius: 8px;
    background: #0d2847;
    border-left: 3px solid #1a4d8f;
    cursor: pointer;
    transition: background .2s;

    &:hover { background: #102d52; }

    &.lv-3, &.lv-danger { border-left-color: #ff5252; background: rgba(255, 82, 82, 0.06); }
    &.lv-2, &.lv-warn   { border-left-color: #ffd200; background: rgba(255, 210, 0, 0.05); }
    &.is-handled { opacity: 0.55; border-left-color: #4CAF50; }
  }

  .ni-left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 140px;
  }

  .ni-badge {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 700;
    white-space: nowrap;

    &.badge-danger { background: rgba(255,82,82,0.2); color: #ff5252; border: 1px solid #ff525230; }
    &.badge-warn   { background: rgba(255,210,0,0.15); color: #ffd200; border: 1px solid #ffd20030; }
    &.badge-info   { background: rgba(0,212,255,0.12); color: #00d4ff; border: 1px solid #00d4ff30; }
  }

  .ni-info {
    display: flex;
    flex-direction: column;
    .ni-user  { font-size: 13px; color: #c8d8e8; font-weight: 600; }
    .ni-dept  { font-size: 11px; color: #6a7a9a; }
  }

  .ni-mid {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 10px;
    .ni-type { font-size: 13px; color: #8ba6c8; }
    .ni-val  { font-size: 14px; color: #ffd200; font-weight: 600; }
  }

  .ni-time {
    font-size: 12px;
    color: #4a6080;
    min-width: 80px;
    text-align: right;
  }

  .ni-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 120px;
    justify-content: flex-end;
  }

  .ni-done-tag {
    font-size: 11px;
    color: #4CAF50;
    padding: 2px 6px;
    border: 1px solid #4CAF5030;
    border-radius: 4px;
  }
}

.notif-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a6080;
  font-size: 14px;
}

.notif-pagination {
  padding-top: 8px;
  display: flex;
  justify-content: flex-end;

  :deep(.el-pagination) {
    --el-pagination-bg-color: #0d2847;
    --el-pagination-text-color: #8ba6c8;
    --el-pagination-border-radius: 4px;
    .el-pager li { background: #0d2847; color: #8ba6c8; border-color: #1a4d8f; }
    .el-pager li.is-active { background: #00d4ff; color: #0a1628; }
  }
}

@media (max-width: 768px) {
  .notif-page {
    padding: 12px;
    overflow: visible;
  }

  .notif-focus-board,
  .notif-stats-row {
    grid-template-columns: 1fr;
  }

  .notif-stats-row {
    display: grid;
  }

  .notif-list .notif-item {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .notif-list .ni-left,
  .notif-list .ni-mid,
  .notif-list .ni-actions {
    width: 100%;
    min-width: 0;
  }

  .notif-list .ni-time {
    min-width: 0;
    text-align: left;
  }

  .notif-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
