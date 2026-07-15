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
      <div class="sc-hd-right">
        <span class="sc-hd-clock">数据截至 {{ dataAsOf || `${currentDate} ${currentTime}` }}</span>
        <div class="sc-hd-actions">
          <button class="sc-hd-btn sc-hd-btn--outline" @click="emergencyCall">呼叫</button>
          <button class="sc-hd-btn sc-hd-btn--outline" @click="emergencyBroadcast">广播</button>
          <button class="sc-hd-btn sc-hd-btn--danger" @click="emergencyEvacuate">撤离</button>
        </div>
      </div>
    </header>

    <section class="sc-war-grid">
      <div class="sc-situation-stage sc-panel panel-enter" style="--delay:.05s">
        <div class="sc-panel-head">
          <div>
            <span class="sc-panel-kicker">SITUATION STAGE</span>
            <h2>{{ incidentTitle }}</h2>
          </div>
          <span :class="['sc-stage-severity', incidentTone]">{{ incidentStatus }}</span>
        </div>

        <div class="sc-stage-canvas">
          <div class="sc-stage-intel">
            <button
              v-for="item in stageIntelItems"
              :key="item.key"
              type="button"
              :class="['sc-stage-intel-card', `tone-${item.tone}`]"
              @click="showInfoDialog(item.label, item.note)"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.note }}</em>
            </button>
          </div>

          <div class="sc-stage-radar">
            <span class="sc-radar-ring ring-1"></span>
            <span class="sc-radar-ring ring-2"></span>
            <span class="sc-radar-ring ring-3"></span>
            <div class="sc-stage-core">
              <span>开放事件</span>
              <strong>{{ authoritativePendingCount }}</strong>
              <em>条待处置</em>
            </div>
          </div>
          <button
            v-for="node in stageNodes"
            :key="node.key"
            type="button"
            :class="['sc-stage-node', `tone-${node.tone}`]"
            :style="{ '--node-x': node.x + '%', '--node-y': node.y + '%' }"
            @click="node.dept ? showDeptDetail(node.dept) : showAreaDetail(node.area)"
          >
            <span class="sc-node-pulse"></span>
            <span class="sc-node-main">
              <span class="sc-node-name">{{ node.label }}</span>
              <span class="sc-stage-node-meta">{{ node.meta }}</span>
            </span>
            <span class="sc-node-score">
              <strong>{{ node.value }}</strong>
              <em>{{ node.status }}</em>
            </span>
          </button>

          <div class="sc-stage-action-strip">
            <button
              v-for="action in stageActionItems"
              :key="action.key"
              type="button"
              :class="['sc-stage-action-cell', `tone-${action.tone}`]"
              @click="action.key === 'broadcast' ? emergencyBroadcast() : showInfoDialog(action.label, action.note)"
            >
              <span>{{ action.label }}</span>
              <strong>{{ action.value }}</strong>
              <em>{{ action.note }}</em>
            </button>
          </div>
        </div>

      </div>

      <aside class="sc-response-queue sc-panel panel-enter" style="--delay:.1s">
        <div class="sc-panel-head">
          <div>
            <span class="sc-panel-kicker">RESPONSE QUEUE</span>
            <h2>现场处置队列</h2>
          </div>
          <span class="sc-queue-count">{{ authoritativePendingCount }}</span>
        </div>

        <div class="sc-queue-list">
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
            </div>
            <button
              type="button"
              class="sc-queue-action"
              @click.stop="item.event ? onHandleEvent(item.event) : emergencyBroadcast()"
            >{{ item.action }}</button>
          </article>
        </div>

        <RiskPersonPanel
          class="sc-risk-lane"
          :persons="top5Persons"
          @callAll="emergencyCall"
          @showPerson="onShowPerson"
        />
      </aside>
    </section>

    <SafetyCommandSupportGrid
      :donut-segments="donutSegments"
      :events="events"
      :handled-count="handledCount"
      :pending-count="authoritativePendingCount"
      :trend7day-total="trend7dayTotal"
      :trend-change="trendChange"
      :trend-path="trendPath"
      :warning-handled-rate="warningHandledRate"
      :warning-trend="warningTrend"
      @show-dept="showDeptDetail"
      @show-event="showEventDetail"
      @show-person-from-event="onShowPersonFromEvent"
      @handle-event="onHandleEvent"
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
      @call="(code) => showInfoDialog('呼叫', `正在呼叫 ${code}...`)"
      @notify="(code) => showInfoDialog('通知', `正在通知 ${code}...`)"
      @viewRecord="(code) => showInfoDialog('完整档案', `加载 ${code} 健康档案...`)"
    />

  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCommandCenterIncident } from '@/api/command-center'
import RiskPersonPanel from './components/RiskPersonPanel.vue'
import SafetyCommandSupportGrid from './components/SafetyCommandSupportGrid.vue'
import SafetyCommandDialogs from './components/SafetyCommandDialogs.vue'
import IncidentCommandDrawer from './components/IncidentCommandDrawer.vue'
import PersonDetailDrawer from './components/PersonDetailDrawer.vue'
import {
  buildDeptRankData,
  buildDonutSegments,
  buildStageActionItems,
  buildStageIntelItems,
  buildStageNodes,
  buildTop5RiskPersons,
  buildTrendChange,
  buildTrendPath,
  buildTrend7dayTotal,
  mapWarningToEvent,
} from './safety-command-view-model'
import { useSafetyCommandInteractions } from './safety-command-interactions'
import { useSafetyCommandPageData } from './use-safety-command-page-data'
import { buildDashboardReturnQuery } from './safety-command-workflow'

const {
  areas,
  commandSummary,
  currentDate,
  currentTime,
  dataAsOf,
  departments,
  events,
  fetchAllData,
  handledCount,
  pendingWarnings,
  processWarningData,
  riskPersons,
  stats,
  warningTrend
} = useSafetyCommandPageData()

const route = useRoute()
const router = useRouter()

// ── Derived KPI ───────────────────────────────────────────────────────────────
const isSafe = computed(() => Number(commandSummary.value.warning?.criticalPending || 0) === 0)
const authoritativePendingCount = computed(() => Number(pendingWarnings.value ?? events.value.length))
const warningHandledRate = computed(() => {
  const total = authoritativePendingCount.value + handledCount.value
  return total > 0 ? Math.round(handledCount.value / total * 100) : 0
})

// ── Dept ranking ──────────────────────────────────────────────────────────────
const deptsSorted = computed(() => buildDeptRankData(departments.value))

const incidentTone = computed(() => {
  if (Number(commandSummary.value.warning?.criticalPending || 0) > 0) return 'danger'
  if (authoritativePendingCount.value > 0) return 'warning'
  return 'safe'
})

const incidentStatus = computed(() => ({
  danger: '一级响应',
  warning: '处置跟进',
  safe: '值守巡查'
}[incidentTone.value]))

const incidentTitle = computed(() => {
  const first = events.value[0]
  if (!first) return '全矿态势稳定，保持值守'
  return `${first.location || first.dept || '现场'} · ${first.type || '风险预警'}`
})

const stageIntelItems = computed(() => buildStageIntelItems({
  pendingCount: authoritativePendingCount.value,
  handledCount: handledCount.value,
  criticalCount: commandSummary.value.warning?.criticalPending || 0,
  unassignedCount: commandSummary.value.warning?.unassignedTotal || 0,
  overdueCount: commandSummary.value.warning?.overdueTotal || 0,
  dataAsOf: dataAsOf.value,
  warningHandledRate: warningHandledRate.value,
  deptsSorted: deptsSorted.value
}))
const stageActionItems = computed(() => buildStageActionItems({
  todayNew: commandSummary.value.warning?.todayNew || 0,
  pendingCount: authoritativePendingCount.value,
  handledCount: handledCount.value,
  criticalCount: commandSummary.value.warning?.criticalPending || 0,
  warningHandledRate: warningHandledRate.value
}))
const stageNodes = computed(() => buildStageNodes({
  deptsSorted: deptsSorted.value,
  areas: areas.value,
  departments: departments.value
}))

const commandQueue = computed(() => {
  const queue = (events.value || []).slice(0, 5).map((event) => ({
    id: event.id || `${event.user}-${event.time}-${event.type}`,
    title: `${event.user || '未知人员'} · ${event.type || '预警'}`,
    meta: `${event.dept || event.location || '未知区域'} / ${event.time || '刚刚'}`,
    action: '处理',
    tone: event.eventType === 'sos' || event.eventType === 'fall' ? 'danger' : 'warning',
    event
  }))

  if (queue.length) return queue

  return [{
    id: 'safe-duty',
    title: '当前无紧急事件',
    meta: '保持在线巡查，关注设备离线和低电量变化',
    action: '广播',
    tone: 'safe',
    event: null
  }]
})

// ── TOP5 ──────────────────────────────────────────────────────────────────────
const top5Persons = computed(() => buildTop5RiskPersons(riskPersons.value))

// ── Donut ─────────────────────────────────────────────────────────────────────
const donutSegments = computed(() => buildDonutSegments(events.value))

// ── Handling progress per type ────────────────────────────────────────────────

// ── 7-day trend ───────────────────────────────────────────────────────────────
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
  emergencyBroadcast,
  emergencyCall,
  emergencyEvacuate,
  handleDeptAi,
  infoDialogContent,
  infoDialogTitle,
  infoDialogVisible,
  onDeptDialogOpen,
  onHandleEvent,
  onShowPerson,
  onShowPersonFromEvent,
  personDrawerUserCode,
  personDrawerUserName,
  personDrawerVisible,
  showAreaDetail,
  showDeptDetail,
  showEventDetail,
  showInfoDialog
} = useSafetyCommandInteractions({ statsRef: stats })

function returnToDashboard() {
  router.push({
    path: '/health-monitor/dashboard',
    query: buildDashboardReturnQuery(currentEvent.value || events.value[0])
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
      if (response.code === 200 && response.data) showEventDetail(mapWarningToEvent(response.data))
    } catch {
      // The incident may no longer exist after switching data sources.
    }
  },
  { immediate: true }
)

</script>

<style scoped lang="scss">
@import './safety-command.scss';
</style>
