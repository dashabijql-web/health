<template>
  <div class="sos-page">
    <WarningCenterNav />

    <!-- ══ 顶部大警报栏 ══ -->
    <div :class="['sos-alarm-bar', hasCritical ? 'sos-alarm-bar--active' : '']">
      <span class="sos-alarm-icon">🚨</span>
      <span class="sos-alarm-text">
        {{ hasCritical
          ? `当前有 ${criticalCount} 条危险级预警未处理！`
          : '当前无危险级预警，系统正常' }}
      </span>
      <span v-if="hasCritical" class="sos-alarm-blink">紧急处置</span>
      <div class="sos-last-update">最后更新：{{ lastUpdateTime }}</div>
    </div>

    <!-- ══ 统计卡片 ══ -->
    <div class="sos-kpi-row">
      <div class="sos-kpi sos-kpi--red">
        <div class="sos-kpi-val">{{ criticalCount }}</div>
        <div class="sos-kpi-label">危险未处理</div>
      </div>
      <div class="sos-kpi sos-kpi--yellow">
        <div class="sos-kpi-val">{{ todayTotal }}</div>
        <div class="sos-kpi-label">今日触发总计</div>
      </div>
      <div class="sos-kpi sos-kpi--green">
        <div class="sos-kpi-val">{{ todayHandled }}</div>
        <div class="sos-kpi-label">今日已处置</div>
      </div>
      <div class="sos-kpi sos-kpi--blue">
        <div class="sos-kpi-val">{{ affectedPersons }}</div>
        <div class="sos-kpi-label">涉及人员数</div>
      </div>
    </div>

    <!-- ══ 危险预警列表 ══ -->
    <div class="sos-section-title">
      <span class="sos-st-bar"></span>
      危险级预警事件
      <span class="sos-st-sub">（{{ criticalCount }} 条待处理，按时间倒序）</span>
    </div>

    <div class="sos-list" v-loading="loading">
      <div
        v-for="item in criticalList"
        :key="item.id"
        :class="['sos-item', item.handled ? 'sos-item--done' : 'sos-item--active']"
      >
        <!-- 左：等级指示 -->
        <div class="sos-item-level">
          <span class="sos-level-icon">{{ item.handled ? '✓' : '⚠' }}</span>
        </div>

        <!-- 中：事件信息 -->
        <div class="sos-item-body">
          <div class="sos-item-row1">
            <span class="sos-name">{{ item.empName || item.userName || '--' }}</span>
            <span class="sos-dept">{{ item.deptName || '--' }}</span>
            <span class="sos-code">工号 {{ item.userCode || item.empCode || '--' }}</span>
          </div>
          <div class="sos-item-row2">
            <span class="sos-metric">{{ item.indicatorName || item.warningType || '--' }}</span>
            <span class="sos-value">{{ item.indicatorValue || '--' }}</span>
            <span class="sos-time">{{ formatTime(item.createTime) }}</span>
          </div>
        </div>

        <!-- 右：操作按钮 -->
        <div class="sos-item-actions">
          <el-button
            v-if="!item.handled"
            type="danger"
            size="small"
            :loading="item._loading"
            @click="handleItem(item)"
          >标记处置</el-button>
          <el-button
            size="small"
            plain
            @click="goToProfile(item)"
          >查看画像</el-button>
          <span v-if="item.handled" class="sos-handled-tag">已处置</span>
        </div>
      </div>

      <div v-if="!loading && criticalList.length === 0" class="sos-empty">
        <span class="sos-empty-icon">✓</span>
        <span>当前没有危险级预警，所有人员状态正常</span>
      </div>
    </div>

    <!-- ══ 分页 ══ -->
    <div class="sos-pagination" v-if="pagination.total > pagination.size">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next, total"
        background
        small
        @current-change="fetchCritical"
      />
    </div>
  </div>
</template>

<script>
import WarningCenterNav from '@/components/WarningCenterNav.vue'
import { getRiskWarningList, handleRiskWarning } from '@/api/risk-warning'
import dayjs from 'dayjs'
import { createIntervalTask } from '@/utils/task-timer'

export default {
  name: 'SosPage',
  components: { WarningCenterNav },
  data() {
    return {
      loading: false,
      criticalList: [],
      criticalCount: 0,
      todayTotal: 0,
      todayHandled: 0,
      affectedPersons: 0,
      lastUpdateTime: '--',
      pagination: { page: 1, size: 30, total: 0 }
    }
  },
  computed: {
    hasCritical() {
      return this.criticalCount > 0
    }
  },
  mounted() {
    this.fetchAll()
    this._pollTask = createIntervalTask(() => this.fetchAll(), 15000)
    this._pollTask.start()
  },
  beforeUnmount() {
    this._pollTask?.stop()
  },
  methods: {
    async fetchAll() {
      await Promise.allSettled([this.fetchCritical(), this.fetchTodayStats()])
      this.lastUpdateTime = dayjs().format('HH:mm:ss')
    },
    async fetchCritical() {
      this.loading = true
      try {
        const res = await getRiskWarningList({
          page: this.pagination.page,
          size: this.pagination.size,
          level: '3'
        })
        if (res.code === 200) {
          this.criticalList = (res.data?.list || res.data?.records || []).map(r => ({ ...r, _loading: false }))
          this.pagination.total = res.data?.total || 0
          this.criticalCount = this.criticalList.filter(r => !r.handled).length
        }
      } finally {
        this.loading = false
      }
    },
    async fetchTodayStats() {
      try {
        const today = dayjs().format('YYYY-MM-DD')
        const [allRes, handledRes] = await Promise.allSettled([
          getRiskWarningList({ page: 1, size: 1, level: '3', startDate: today, endDate: today }),
          getRiskWarningList({ page: 1, size: 1, level: '3', handled: true, startDate: today, endDate: today })
        ])
        this.todayTotal = allRes.status === 'fulfilled' && allRes.value.code === 200 ? (allRes.value.data?.total || 0) : 0
        this.todayHandled = handledRes.status === 'fulfilled' && handledRes.value.code === 200 ? (handledRes.value.data?.total || 0) : 0
        // 涉及人员：去重
        const persons = new Set(this.criticalList.map(r => r.userCode || r.empCode).filter(Boolean))
        this.affectedPersons = persons.size
      } catch (_) {}
    },
    async handleItem(item) {
      item._loading = true
      try {
        await handleRiskWarning(item.id)
        item.handled = true
        this.criticalCount = Math.max(0, this.criticalCount - 1)
        this.$message.success('已标记处置')
      } catch (_) {
        this.$message.error('操作失败')
      } finally {
        item._loading = false
      }
    },
    goToProfile(item) {
      this.$router.push({ path: '/health-monitor/employee-profile', query: { userCode: item.userCode || item.empCode } })
    },
    formatTime(t) {
      if (!t) return '--'
      return dayjs(t).format('MM-DD HH:mm:ss')
    }
  }
}
</script>

<style lang="scss" scoped>
.sos-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0a1628;
  padding: 16px;
  gap: 12px;
  overflow-y: auto;
}

/* ── 顶部警报栏 ── */
.sos-alarm-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  border-radius: 10px;
  background: rgba(0, 212, 255, 0.06);
  border: 1px solid rgba(0, 212, 255, 0.15);
  position: relative;
  transition: all .4s;

  &--active {
    background: rgba(255, 40, 40, 0.12);
    border-color: rgba(255, 40, 40, 0.4);
    animation: sos-pulse 2s infinite;
  }

  .sos-alarm-icon { font-size: 28px; }

  .sos-alarm-text {
    font-size: 18px;
    font-weight: 700;
    color: #c8d8e8;
    flex: 1;
  }

  &--active .sos-alarm-text { color: #ff5252; }

  .sos-alarm-blink {
    font-size: 13px;
    font-weight: 700;
    color: #fff;
    background: #ff2828;
    padding: 4px 14px;
    border-radius: 20px;
    animation: blink-bg 1s infinite;
  }

  .sos-last-update {
    font-size: 11px;
    color: #4a6080;
    position: absolute;
    right: 20px;
    bottom: 6px;
  }
}

/* ── KPI 卡片 ── */
.sos-kpi-row {
  display: flex;
  gap: 12px;

  .sos-kpi {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 16px 8px;
    border-radius: 10px;
    border: 1px solid transparent;

    .sos-kpi-val   { font-size: 32px; font-weight: 800; }
    .sos-kpi-label { font-size: 12px; color: rgba(255,255,255,0.55); margin-top: 4px; }

    &--red    { background: rgba(255,82,82,0.12);   border-color: rgba(255,82,82,0.2);  .sos-kpi-val { color: #ff5252; } }
    &--yellow { background: rgba(255,210,0,0.10);   border-color: rgba(255,210,0,0.2);  .sos-kpi-val { color: #ffd200; } }
    &--green  { background: rgba(76,175,80,0.10);   border-color: rgba(76,175,80,0.2);  .sos-kpi-val { color: #4CAF50; } }
    &--blue   { background: rgba(0,212,255,0.08);   border-color: rgba(0,212,255,0.15); .sos-kpi-val { color: #00d4ff; } }
  }
}

/* ── 章节标题 ── */
.sos-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #c8d8e8;
  display: flex;
  align-items: center;
  gap: 8px;

  .sos-st-bar {
    width: 3px; height: 16px;
    background: #ff5252;
    border-radius: 2px;
  }

  .sos-st-sub { font-size: 12px; color: #4a6080; font-weight: 400; }
}

/* ── 列表 ── */
.sos-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .sos-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 18px;
    border-radius: 10px;
    border: 1px solid transparent;
    transition: all .2s;

    &--active {
      background: rgba(255, 40, 40, 0.08);
      border-color: rgba(255, 40, 40, 0.25);

      &:hover { background: rgba(255, 40, 40, 0.12); }
    }

    &--done {
      background: rgba(76, 175, 80, 0.06);
      border-color: rgba(76, 175, 80, 0.15);
      opacity: 0.7;
    }
  }

  .sos-item-level {
    .sos-level-icon {
      font-size: 24px;
      line-height: 1;
    }
  }

  .sos-item-body {
    flex: 1;

    .sos-item-row1, .sos-item-row2 {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .sos-item-row1 { margin-bottom: 6px; }

    .sos-name   { font-size: 16px; font-weight: 700; color: #fff; }
    .sos-dept   { font-size: 12px; color: #8ba6c8; background: rgba(0,212,255,0.08); padding: 1px 8px; border-radius: 10px; }
    .sos-code   { font-size: 11px; color: #4a6080; }
    .sos-metric { font-size: 13px; color: #8ba6c8; }
    .sos-value  { font-size: 18px; font-weight: 800; color: #ff5252; }
    .sos-time   { font-size: 12px; color: #4a6080; }
  }

  .sos-item-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .sos-handled-tag {
    font-size: 12px;
    color: #4CAF50;
    padding: 3px 10px;
    border: 1px solid #4CAF5030;
    border-radius: 4px;
  }
}

.sos-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: #4a6080;

  .sos-empty-icon { font-size: 48px; }
}

/* ── 分页 ── */
.sos-pagination {
  display: flex;
  justify-content: center;
}

/* ── 动画 ── */
@keyframes sos-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255,40,40,0.3); }
  50%       { box-shadow: 0 0 20px 4px rgba(255,40,40,0.15); }
}

@keyframes blink-bg {
  0%, 100% { background: #ff2828; }
  50%       { background: #cc0000; }
}
</style>
