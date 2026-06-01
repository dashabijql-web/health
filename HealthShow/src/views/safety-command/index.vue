<template>
  <div class="hm-page-shell sc-war-room">
    <PageHeroHeader
      class="sc-incident-hero"
      variant="cockpit"
      eyebrow="Field Command"
      title="安全指挥中心"
      :description="safetyHeroDescription"
    >
      <template #meta>
        <div class="sc-hero-meta">
          <span :class="['sc-live-dot', isSafe ? 'is-safe' : 'is-danger']"></span>
          <span :class="['hm-status-chip', isSafe ? 'hm-status-chip--success' : 'hm-status-chip--danger']">
            {{ isSafe ? '现场平稳' : '高危介入' }}
          </span>
          <span class="sc-hero-date">{{ currentDate }}</span>
          <span class="sc-hero-time">{{ currentTime }}</span>
        </div>
      </template>
      <template #actions>
        <div class="sc-emergency-actions">
          <button class="eb eb-o" @click="emergencyCall">呼叫</button>
          <button class="eb eb-o" @click="emergencyBroadcast">广播</button>
          <button class="eb eb-r" @click="emergencyEvacuate">撤离</button>
        </div>
      </template>
    </PageHeroHeader>

    <MetricStrip
      class="sc-command-ribbon"
      :items="safetyMetricItems"
      dense
      clickable
      @select="handleSafetyMetricSelect"
    />

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
          <div class="sc-stage-radar">
            <span class="sc-radar-ring ring-1"></span>
            <span class="sc-radar-ring ring-2"></span>
            <span class="sc-radar-ring ring-3"></span>
            <div class="sc-stage-core">
              <span>井下态势</span>
              <strong>{{ stats.underground || 0 }}</strong>
              <em>人在线作业</em>
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
            <span class="sc-node-name">{{ node.label }}</span>
            <strong>{{ node.value }}</strong>
          </button>
        </div>

        <div class="sc-stage-telemetry">
          <button
            v-for="item in commandTelemetry"
            :key="item.key"
            type="button"
            :class="['sc-telemetry-item', `tone-${item.tone}`]"
            @click="item.key === 'watch' ? handleKpiDetail('watch') : showInfoDialog(item.label, item.note)"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.note }}</em>
          </button>
        </div>
      </div>

      <aside class="sc-response-queue sc-panel panel-enter" style="--delay:.1s">
        <div class="sc-panel-head">
          <div>
            <span class="sc-panel-kicker">RESPONSE QUEUE</span>
            <h2>现场处置队列</h2>
          </div>
          <span class="sc-queue-count">{{ pendingCount }}</span>
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
      :areas="areas"
      :depts-sorted="deptsSorted"
      :donut-segments="donutSegments"
      :events="events"
      :handled-count="handledCount"
      :pending-count="pendingCount"
      :trend7day-total="trend7dayTotal"
      :trend-change="trendChange"
      :trend-path="trendPath"
      :type-handle-progress="typeHandleProgress"
      :vitals-rows="vitalsRows"
      :warning-handled-rate="warningHandledRate"
      :warning-total="warningTotal"
      :warning-trend="warningTrend"
      @show-dept="showDeptDetail"
      @show-area="showAreaDetail"
      @show-event="showEventDetail"
      @show-person-from-event="onShowPersonFromEvent"
      @handle-event="onHandleEvent"
    />

    <SafetyCommandDialogs
      v-model:eventDialogVisible="eventDialogVisible"
      v-model:areaDialogVisible="areaDialogVisible"
      v-model:deptDialogVisible="deptDialogVisible"
      v-model:broadcastDialogVisible="broadcastDialogVisible"
      v-model:contactDialogVisible="contactDialogVisible"
      v-model:infoDialogVisible="infoDialogVisible"
      v-model:broadcastContent="broadcastContent"
      :currentEvent="currentEvent"
      :currentArea="currentArea"
      :currentDept="currentDept"
      :deptAiReport="deptAiReport"
      :deptAiRendered="deptAiRendered"
      :deptAiLoading="deptAiLoading"
      :deptAiTime="deptAiTime"
      :infoDialogTitle="infoDialogTitle"
      :infoDialogContent="infoDialogContent"
      @handleEvent="closeEventDialogAfterHandle"
      @showPersonFromEvent="closeEventDialogAfterShowPerson"
      @showInfo="showInfoDialog"
      @deptDialogOpen="onDeptDialogOpen"
      @handleDeptAi="handleDeptAi"
      @confirmBroadcast="confirmBroadcast"
    />

    <EventHandleDialog
      v-model:visible="handleDialogVisible"
      :event="handleEvent"
      @handled="onEventHandled"
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
import { computed } from 'vue'
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import MetricStrip from '@/components/health-shell/MetricStrip.vue'
import RiskPersonPanel from './components/RiskPersonPanel.vue'
import SafetyCommandSupportGrid from './components/SafetyCommandSupportGrid.vue'
import SafetyCommandDialogs from './components/SafetyCommandDialogs.vue'
import EventHandleDialog from './components/EventHandleDialog.vue'
import PersonDetailDrawer from './components/PersonDetailDrawer.vue'
import {
  buildAreasFromDepartments,
  buildDeptRankData,
  buildDonutSegments,
  buildTop5RiskPersons,
  buildTrendChange,
  buildTrendPath,
  buildTrend7dayTotal,
  buildTypeHandleProgress,
  buildVitalsRows,
} from './safety-command-view-model'
import { useSafetyCommandInteractions } from './safety-command-interactions'
import { useSafetyCommandPageData } from './use-safety-command-page-data'

const {
  areas,
  currentDate,
  currentTime,
  departments,
  events,
  handledCount,
  processWarningData,
  riskPersons,
  stats,
  vitalAvg,
  vitalsHistory,
  watchStatus,
  warningTrend
} = useSafetyCommandPageData()

// ── Derived KPI ───────────────────────────────────────────────────────────────
const isSafe = computed(() => stats.value.sos === 0 && stats.value.fall === 0)
const pendingCount = computed(() => events.value.length)
const safetyHeroDescription = computed(() =>
  `当前待处置 ${pendingCount.value} 条，已闭环 ${handledCount.value} 条，手表在线 ${watchStatus.value.online}/${watchStatus.value.total || '--'}。`
)
const safetyMetricItems = computed(() => [
  {
    key: 'underground',
    label: '井下人数',
    value: stats.value.underground,
    note: `正常 ${stats.value.normal} / 总数 ${stats.value.total}`,
    tone: 'primary'
  },
  {
    key: 'sos',
    label: 'SOS 求救',
    value: stats.value.sos,
    note: stats.value.sos > 0 ? '立即处置' : '无紧急求救',
    tone: stats.value.sos > 0 ? 'danger' : 'success'
  },
  {
    key: 'fall',
    label: '跌倒检测',
    value: stats.value.fall,
    note: stats.value.fall > 0 ? '需要复核' : '无跌倒事件',
    tone: stats.value.fall > 0 ? 'danger' : 'success'
  },
  {
    key: 'alerts',
    label: '其他预警',
    value: stats.value.static + stats.value.abnormal,
    note: `静止 ${stats.value.static} / 异常 ${stats.value.abnormal}`,
    tone: stats.value.static + stats.value.abnormal > 0 ? 'warning' : 'primary'
  },
  {
    key: 'watch',
    label: '手表状态',
    value: `${watchStatus.value.online}/${watchStatus.value.total || '--'}`,
    note: `离线 ${watchStatus.value.offline} / 低电 ${watchStatus.value.lowBattery}`,
    tone: watchStatus.value.offline > 20 || watchStatus.value.lowBattery > 30 ? 'warning' : 'success'
  }
])
// 优先用 watchStatus.online（来自 getStatistics().onlineUsers）
const warningHandledRate = computed(() => {
  const total = pendingCount.value + handledCount.value
  return total > 0 ? Math.round(handledCount.value / total * 100) : 0
})
const warningTotal = computed(() => pendingCount.value + handledCount.value)
const handleSafetyMetricSelect = (item) => handleKpiDetail(item.key)

// ── Dept ranking ──────────────────────────────────────────────────────────────
const deptsSorted = computed(() => buildDeptRankData(departments.value))

const incidentTone = computed(() => {
  if (stats.value.sos > 0 || stats.value.fall > 0) return 'danger'
  if (pendingCount.value > 0) return 'warning'
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

const commandTelemetry = computed(() => [
  {
    key: 'online',
    label: '井下在线',
    value: `${stats.value.underground || 0}/${stats.value.total || 0}`,
    note: `正常 ${stats.value.normal || 0}`,
    tone: 'primary'
  },
  {
    key: 'closure',
    label: '闭环率',
    value: `${warningHandledRate.value}%`,
    note: `已闭环 ${handledCount.value}`,
    tone: warningHandledRate.value >= 80 ? 'success' : 'warning'
  },
  {
    key: 'watch',
    label: '手表在线',
    value: `${watchStatus.value.online || 0}/${watchStatus.value.total || 0}`,
    note: `离线 ${watchStatus.value.offline || 0}`,
    tone: watchStatus.value.offline > 0 ? 'warning' : 'success'
  },
  {
    key: 'temperature',
    label: '平均体温',
    value: vitalAvg.value.temperature > 0 ? vitalAvg.value.temperature.toFixed(1) : '--',
    note: vitalAvg.value.bloodOxygen > 0 ? `血氧 ${Math.round(vitalAvg.value.bloodOxygen)}%` : '等待体征',
    tone: 'primary'
  }
])

const stageNodes = computed(() => {
  const positions = [
    [16, 22], [72, 18], [24, 68], [82, 62], [50, 12], [52, 78]
  ]
  const deptNodes = deptsSorted.value.slice(0, 6).map((dept, index) => ({
    key: `dept-${dept.id || dept.name || index}`,
    label: dept.name || `部门${index + 1}`,
    value: dept.warnings || 0,
    tone: dept.level === 'H' ? 'danger' : dept.level === 'M' ? 'warning' : dept.level === 'L' ? 'primary' : 'safe',
    dept,
    x: positions[index][0],
    y: positions[index][1]
  }))

  if (deptNodes.length) return deptNodes

  const fallbackAreas = (areas.value?.length ? areas.value : buildAreasFromDepartments(departments.value)).slice(0, 6)
  return fallbackAreas.map((area, index) => ({
    key: `area-${area.id || area.name || index}`,
    label: area.name || `区域${index + 1}`,
    value: area.warning || area.count || 0,
    tone: area.level === 'danger' ? 'danger' : area.level === 'warning' ? 'warning' : 'safe',
    area,
    x: positions[index][0],
    y: positions[index][1]
  }))
})

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
const typeHandleProgress = computed(() => buildTypeHandleProgress(events.value, handledCount.value))
const vitalsRows = computed(() => buildVitalsRows(vitalAvg.value, vitalsHistory))

// ── 7-day trend ───────────────────────────────────────────────────────────────
const trendPath = computed(() => buildTrendPath(warningTrend.value))
const trend7dayTotal = computed(() => buildTrend7dayTotal(warningTrend.value))
const trendChange = computed(() => buildTrendChange(warningTrend.value))

const {
  areaDialogVisible,
  broadcastContent,
  broadcastDialogVisible,
  closeEventDialogAfterHandle,
  closeEventDialogAfterShowPerson,
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
  eventDialogVisible,
  handleDeptAi,
  handleDialogVisible,
  handleEvent,
  handleKpiDetail,
  infoDialogContent,
  infoDialogTitle,
  infoDialogVisible,
  onDeptDialogOpen,
  onEventHandled,
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
} = useSafetyCommandInteractions({
  statsRef: stats,
  watchStatusRef: watchStatus,
  handledCountRef: handledCount,
  eventsRef: events,
  riskPersonsRef: riskPersons
})

</script>

<style scoped lang="scss">
@import './safety-command.scss';
</style>
