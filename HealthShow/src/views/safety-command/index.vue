<template>
  <div class="sc-war-room">
    <header :class="['sc-hd', !isSafe && 'is-danger']">
      <div class="sc-hd-left">
        <span :class="['sc-hd-beacon', isSafe ? 'is-safe' : 'is-danger']"></span>
        <h1 class="sc-hd-title">安全指挥中心</h1>
        <span :class="['sc-hd-badge', isSafe ? 'tone-safe' : 'tone-danger']">
          {{ isSafe ? '现场平稳' : '高危介入' }}
        </span>
      </div>
      <div class="sc-hd-kpis">
        <div v-for="m in safetyMetricItems" :key="m.key"
          :class="['sc-hd-kpi', `tone-${m.tone}`]"
          :title="m.note">
          <span class="sc-hd-kpi-v">{{ m.value }}</span>
          <span class="sc-hd-kpi-l">{{ m.label }}</span>
        </div>
      </div>
      <div class="sc-hd-right">
        <span class="sc-hd-clock">数据更新 {{ dataAsOfLabel }}</span>
        <div class="sc-hd-actions">
          <button class="sc-hd-btn sc-hd-btn--outline" @click="openPriorityIncident">最高危事件</button>
          <button class="sc-hd-btn sc-hd-btn--outline" @click="goToDashboard">统一管控</button>
        </div>
      </div>
    </header>

    <section class="sc-war-grid">
      <div class="sc-situation-stage sc-panel panel-enter" style="--delay:.05s">
        <div class="sc-panel-head">
          <div>
            <span class="sc-panel-kicker">重点态势</span>
            <h2>{{ incidentTitle }}</h2>
          </div>
          <span :class="['sc-stage-severity', incidentTone]">{{ incidentStatus }}</span>
        </div>

        <div class="sc-stage-canvas">
          <div class="sc-stage-section-head">
            <strong>重点部门</strong>
            <span>按今日预警量排序</span>
          </div>

          <div class="sc-stage-operational-grid" aria-label="重点部门风险">
            <button
              v-for="node in stageNodes"
              :key="node.key"
              type="button"
              :class="['sc-stage-zone', `tone-${node.tone}`]"
              @click="node.dept ? showDeptDetail(node.dept) : showAreaDetail(node.area)"
            >
              <span class="sc-stage-zone-head">
                <span class="sc-node-pulse"></span>
                <span class="sc-node-name">{{ node.label }}</span>
                <strong>{{ node.value }}</strong>
              </span>
              <span class="sc-stage-node-meta">{{ node.meta }}</span>
            </button>
          </div>

          <article v-if="priorityEvent" :class="['sc-priority-incident', `tone-${priorityEvent.tone}`]">
            <div class="sc-priority-incident__heading">
              <span>当前优先事件</span>
              <strong>{{ priorityEvent.statusLabel }}</strong>
            </div>
            <div class="sc-priority-incident__main">
              <strong>{{ priorityEvent.user || '未知人员' }} · {{ priorityEvent.type || '风险预警' }}</strong>
              <span>{{ priorityEvent.location || priorityEvent.dept || '未接入定位' }} / {{ priorityEvent.time || '刚刚' }}</span>
            </div>
            <div class="sc-priority-incident__meta">
              <span>责任：{{ priorityEvent.owner || '未分派' }}</span>
              <span>SLA：{{ priorityEvent.sla || '未配置' }}</span>
            </div>
            <button type="button" @click="showEventDetail(priorityEvent)">进入处置</button>
          </article>
          <div v-else class="sc-priority-incident sc-priority-incident--safe">
            <strong>当前无待处置事件</strong>
            <span>继续关注部门预警变化和设备例外状态。</span>
          </div>
        </div>
      </div>

      <aside class="sc-response-queue sc-panel panel-enter" style="--delay:.1s">
        <div class="sc-panel-head">
          <div>
            <h2>现场处置队列</h2>
            <span class="sc-panel-subtitle">展示前50条开放事件，按风险优先</span>
          </div>
          <div class="sc-queue-head-meta">
            <div class="sc-queue-filters" aria-label="事件筛选">
              <button
                v-for="filter in queueFilterItems"
                :key="filter.key"
                type="button"
                :class="['sc-queue-filter', { active: queueFilter === filter.key }]"
                @click="queueFilter = filter.key"
              >{{ filter.label }} {{ filter.count }}</button>
            </div>
            <span class="sc-queue-count">展示 {{ loadedQueueCount }} / 待办 {{ pendingCount }}</span>
          </div>
        </div>

        <div class="sc-queue-list">
          <div v-if="commandQueue.length === 0" class="sc-queue-empty">
            <strong>{{ queueFilter === 'all' ? '当前无开放事件' : '当前筛选无事件' }}</strong>
            <button v-if="queueFilter !== 'all'" type="button" @click="queueFilter = 'all'">查看全部</button>
          </div>
          <article
            v-for="(item, index) in commandQueue"
            :key="item.id"
            :class="['sc-queue-card', `tone-${item.tone}`]"
            @click="item.event ? showEventDetail(item.event) : showInfoDialog('值守状态', item.meta)"
          >
            <span class="sc-queue-order">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="sc-queue-main">
              <strong>{{ item.title }}</strong>
              <span>{{ item.meta }}</span>
              <em>{{ item.context }}</em>
            </div>
            <button
              type="button"
              class="sc-queue-action"
              @click.stop="item.event ? showEventDetail(item.event) : showInfoDialog('当前无事件上下文', '请选择具体事件后再执行呼叫、广播或撤离动作。')"
            >{{ item.action }}</button>
          </article>
        </div>
      </aside>
    </section>

    <SafetyCommandSupportGrid
      :handled-count="handledCount"
      :pending-count="pendingCount"
      :trend7day-total="trend7dayTotal"
      :trend-change="trendChange"
      :trend-path="trendPath"
      :warning-handled-rate="warningHandledRate"
      :warning-trend="warningTrend"
    />

    <SafetyCommandDialogs
      v-model:areaDialogVisible="areaDialogVisible"
      v-model:deptDialogVisible="deptDialogVisible"
      v-model:broadcastDialogVisible="broadcastDialogVisible"
      v-model:contactDialogVisible="contactDialogVisible"
      v-model:infoDialogVisible="infoDialogVisible"
      v-model:broadcastContent="broadcastContent"
      :currentArea="currentArea"
      :currentDept="currentDept"
      :deptAiReport="deptAiReport"
      :deptAiRendered="deptAiRendered"
      :deptAiLoading="deptAiLoading"
      :deptAiTime="deptAiTime"
      :infoDialogTitle="infoDialogTitle"
      :infoDialogContent="infoDialogContent"
      @showInfo="showInfoDialog"
      @deptDialogOpen="onDeptDialogOpen"
      @handleDeptAi="handleDeptAi"
      @confirmBroadcast="confirmBroadcast"
    />

    <IncidentCommandDrawer
      v-model:visible="incidentDrawerVisible"
      :event="currentEvent"
      source-page="safety-command"
      :return-available="route.query.from === 'dashboard'"
      @updated="fetchAllData"
      @return-to-origin="returnToDashboard"
    />
    <PersonDetailDrawer
      v-model:visible="personDrawerVisible"
      :userCode="personDrawerUserCode"
      :userName="personDrawerUserName"
      @emergency="showEventDetail"
    />

  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCommandCenterIncident } from '@/api/command-center'
import SafetyCommandSupportGrid from './components/SafetyCommandSupportGrid.vue'
import SafetyCommandDialogs from './components/SafetyCommandDialogs.vue'
import IncidentCommandDrawer from './components/IncidentCommandDrawer.vue'
import PersonDetailDrawer from './components/PersonDetailDrawer.vue'
import {
  buildDeptRankData,
  buildStageNodes,
  buildTrendChange,
  buildTrendPath,
  buildTrend7dayTotal,
  mapWarningToEvent,
} from './safety-command-view-model'
import { useSafetyCommandInteractions } from './safety-command-interactions'
import { useSafetyCommandPageData } from './use-safety-command-page-data'
import {
  buildDashboardReturnQuery,
  buildSafetyCommandQueue,
  buildSafetyPriorityEvent
} from './safety-command-workflow'

const {
  commandSummary,
  dataAsOf,
  departments,
  events,
  fetchAllData,
  handledCount,
  pendingWarnings,
  criticalWarnings,
  warningTrend
} = useSafetyCommandPageData()

const route = useRoute()
const router = useRouter()

const summaryWarning = computed(() => commandSummary.value?.warning || {})
const pendingCount = computed(() => Number(summaryWarning.value.pendingTotal ?? pendingWarnings.value ?? events.value.length))
const highRiskCount = computed(() => Number(summaryWarning.value.criticalPending ?? criticalWarnings.value ?? 0))
const overdueCount = computed(() => Number(summaryWarning.value.overdueTotal || 0))
const isSafe = computed(() => highRiskCount.value === 0 && overdueCount.value === 0)
const dataAsOfLabel = computed(() => {
  const value = dataAsOf.value || commandSummary.value?.dataAsOf || ''
  return value ? String(value).replace('T', ' ').slice(11, 19) : '--:--:--'
})
const safetyMetricItems = computed(() => [
  {
    key: 'today',
    label: '今日新增',
    value: Number(summaryWarning.value.todayNew || 0),
    note: '今日产生的全部预警',
    tone: 'primary'
  },
  {
    key: 'critical',
    label: '高危待办',
    value: highRiskCount.value,
    note: '今日尚未处置的高危预警',
    tone: highRiskCount.value > 0 ? 'danger' : 'success'
  },
  {
    key: 'pending',
    label: '待办总数',
    value: pendingCount.value,
    note: '今日全部开放事件',
    tone: pendingCount.value > 0 ? 'warning' : 'success'
  },
  {
    key: 'unassigned',
    label: '未分派',
    value: Number(summaryWarning.value.unassignedTotal || 0),
    note: '尚未指定责任人的开放事件',
    tone: summaryWarning.value.unassignedTotal > 0 ? 'warning' : 'success'
  },
  {
    key: 'overdue',
    label: '已超时',
    value: overdueCount.value,
    note: '已超过已配置 SLA 的开放事件',
    tone: overdueCount.value > 0 ? 'danger' : 'success'
  }
])
const warningHandledRate = computed(() => {
  const total = pendingCount.value + handledCount.value
  return total > 0 ? Math.round(handledCount.value / total * 100) : 0
})

const deptsSorted = computed(() => buildDeptRankData(departments.value))
const priorityEvent = computed(() => buildSafetyPriorityEvent(events.value))

const incidentTone = computed(() => {
  if (highRiskCount.value > 0 || overdueCount.value > 0) return 'danger'
  if (pendingCount.value > 0) return 'warning'
  return 'safe'
})

const incidentStatus = computed(() => ({
  danger: '一级响应',
  warning: '处置跟进',
  safe: '值守巡查'
}[incidentTone.value]))

const incidentTitle = computed(() => {
  const event = priorityEvent.value
  if (!event) return '全矿态势稳定，保持值守'
  return `${event.location || event.dept || '现场'} · ${event.type || '风险预警'}`
})

const stageNodes = computed(() => buildStageNodes({
  deptsSorted: deptsSorted.value,
  areas: [],
  departments: departments.value
}))

const queueFilter = ref('all')
const queueFilterItems = computed(() => [
  { key: 'all', label: '全部', count: events.value.length },
  { key: 'sos', label: 'SOS', count: events.value.filter((event) => event.eventType === 'sos').length },
  { key: 'fall', label: '跌倒', count: events.value.filter((event) => event.eventType === 'fall').length },
  { key: 'other', label: '其他', count: events.value.filter((event) => !['sos', 'fall'].includes(event.eventType)).length }
])
const filteredQueueEvents = computed(() => {
  if (queueFilter.value === 'all') return events.value
  if (queueFilter.value === 'other') return events.value.filter((event) => !['sos', 'fall'].includes(event.eventType))
  return events.value.filter((event) => event.eventType === queueFilter.value)
})
const commandQueue = computed(() => filteredQueueEvents.value.length
  ? buildSafetyCommandQueue(filteredQueueEvents.value)
  : [])
const loadedQueueCount = computed(() => events.value.length)

const trendPath = computed(() => buildTrendPath(warningTrend.value))
const trend7dayTotal = computed(() => buildTrend7dayTotal(warningTrend.value))
const trendChange = computed(() => buildTrendChange(warningTrend.value))

const {
  areaDialogVisible,
  broadcastContent,
  broadcastDialogVisible,
  confirmBroadcast,
  contactDialogVisible,
  currentArea,
  currentDept,
  currentEvent,
  deptAiLoading,
  deptAiRendered,
  deptAiReport,
  deptAiTime,
  deptDialogVisible,
  handleDeptAi,
  incidentDrawerVisible,
  infoDialogContent,
  infoDialogTitle,
  infoDialogVisible,
  onDeptDialogOpen,
  onShowPerson,
  onShowPersonFromEvent,
  personDrawerUserCode,
  personDrawerUserName,
  personDrawerVisible,
  showAreaDetail,
  showDeptDetail,
  showEventDetail,
  showInfoDialog
} = useSafetyCommandInteractions()

function openPriorityIncident() {
  if (priorityEvent.value) {
    showEventDetail(priorityEvent.value)
    return
  }
  showInfoDialog('当前无待处置事件', '请继续关注部门预警变化和设备例外状态。')
}

function goToDashboard() {
  router.push('/health-monitor/dashboard')
}

function returnToDashboard() {
  const event = currentEvent.value || priorityEvent.value
  router.push({
    path: '/health-monitor/dashboard',
    query: buildDashboardReturnQuery(event)
  })
}

let routeIncidentKey = ''
watch(
  () => [route.query.warningId, route.query.occurredAt],
  async ([warningId, occurredAt]) => {
    const nextKey = `${warningId || ''}:${occurredAt || ''}`
    if (!warningId || !occurredAt || nextKey === routeIncidentKey) return
    routeIncidentKey = nextKey
    try {
      const response = await getCommandCenterIncident(warningId, occurredAt)
      if (response.code === 200 && response.data) {
        showEventDetail(mapWarningToEvent(response.data))
      }
    } catch {
      // The target incident can disappear after a data-source switch or retention cleanup.
    }
  },
  { immediate: true }
)

</script>

<style scoped lang="scss">
@import './safety-command.scss';
</style>
