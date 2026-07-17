<template>
  <div class="db-control-system" ref="dmScale" @transitionend.stop @animationend.stop>

    <header class="db-hd">
      <div class="db-hd-left">
        <span class="db-hd-beacon"></span>
        <h1 class="db-hd-title">统一管控</h1>
      </div>
      <div class="db-hd-kpis">
        <button v-for="item in headerMetricStripItems" :key="item.key" type="button"
          :class="['db-hd-kpi', `tone-${item.tone}`]"
          @click="onHeaderMetricSelect(item)">
          <span class="db-hd-kpi-v">{{ item.value }}</span>
          <span class="db-hd-kpi-l">{{ item.label }}</span>
        </button>
      </div>
      <div class="db-hd-right">
        <div class="db-hd-time">
          <span class="db-hd-clock">{{ currentTime }}</span>
          <button type="button" class="db-hd-refresh" @click="fetchData(true)" title="点击立即刷新">
            <span :class="['db-hd-refresh-icon', isRefreshing && 'is-spinning']">&#x21BB;</span>
            <span class="db-hd-refresh-text">{{ lastRefreshText }}</span>
          </button>
        </div>
        <div class="db-hd-period">
          <button v-for="p in periodOptions" :key="p.value" type="button"
            :class="['db-hd-period-tab', activePeriod === p.value && 'is-active']"
            @click="switchPeriod(p.value)">{{ p.label }}</button>
        </div>
        <button type="button" class="db-hd-fs" @click="toggleFullscreen"
          :title="isFullscreen ? '退出全屏' : '全屏展示'">
          {{ isFullscreen ? '⊑' : '⛶' }}
        </button>
      </div>
    </header>

    <DashboardPersonSearch @select="goToEmployeeProfile" />

    <div class="db-control-body dm-bd" ref="dmBody">
      <section class="db-main-grid">
        <aside class="db-col-health">
          <div class="db-panel db-duty-snapshot db-panel--interactive">
            <div class="db-track">
              <span class="db-track-title">监测覆盖与数据质量</span>
              <button type="button" class="dm-inline-action" @click="openDeptPersonModal">查看部门详情</button>
            </div>
            <div class="dm-metrics-row">
              <div
                v-for="m in coverageCards"
                :key="m.key"
                :class="['dm-metric-card', 'is-operational-card', `tone-${m.tone}`, m.route ? 'is-clickable' : '']"
                :style="{ '--metric-tone': m.color }"
                @click="m.route && $router.push(m.route)"
              >
                <div class="dm-metric-val">
                  <span>{{ m.value }}</span>
                </div>
                <div class="dm-metric-label">{{ m.label }}</div>
                <div class="dm-metric-note">{{ m.note }}</div>
                <div v-if="m.progress !== null && m.progress !== undefined" class="dm-metric-bar-wrap">
                  <div class="dm-metric-bar" :style="{ width: `${m.progress}%` }"></div>
                </div>
              </div>
            </div>
          </div>

          <div class="db-panel db-health-snapshot db-health-exception-snapshot">
            <div class="db-track">
              <span class="db-track-title">健康异常快照</span>
              <button type="button" class="dm-inline-action" @click="$router.push('/health-monitor/risk-warning')">查看全部异常</button>
            </div>
            <div class="dm-vitals-grid">
              <div
                v-for="v in healthExceptionCards"
                :key="v.label"
                :class="['dm-vital-card', v.route ? 'is-clickable' : '']"
                :style="{ '--vital-tone': v.color, '--vital-tone-soft': `${v.color}12`, '--vital-tone-border': `${v.color}33` }"
                @click="v.route && $router.push(v.route)"
              >
                <div class="dm-vital-head">
                  <div class="dm-vital-icon">
                    <el-icon :size="15"><component :is="v.icon" /></el-icon>
                  </div>
                  <div class="dm-vital-label">{{ v.label }}</div>
                </div>
                <div class="dm-vital-reading">
                  <span class="dm-vital-val">{{ v.val }}</span>
                  <span v-if="v.unit" class="dm-vital-unit">{{ v.unit }}</span>
                </div>
                <div class="dm-vital-foot">
                  <div class="dm-vital-foot__status">
                    <div class="dm-vital-tag" :class="v.tagCls">{{ v.tag }}</div>
                    <span :class="['dm-vital-exception', `tone-${v.tone}`]">{{ v.exceptionText }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="dm-health-snapshot-note">
              实时窗口 {{ healthSnapshot?.onlineWindowMinutes || '--' }} 分钟 · 新鲜度 {{ healthSnapshot?.freshnessMinutes || '--' }} 分钟 ·
              覆盖 {{ healthSnapshot?.freshUsers ?? '--' }}/{{ healthSnapshot?.onlineUsers ?? '--' }} 人 ·
              数据状态 {{ healthSnapshotStatusText }}
            </div>
          </div>

        </aside>

        <main class="db-col-decision">
          <div class="db-panel db-ops-preshift">
            <div class="db-track">
              <span class="db-track-title">班前健康准入</span>
              <span class="db-track-sub">人员名单和健康异常均可直接进入处置</span>
            </div>
            <div class="dm-preshift-queues">
              <button
                v-for="item in admissionQueueItems"
                :key="item.key"
                type="button"
                :class="['dm-preshift-queue', `tone-${item.tone}`, item.unavailable && 'is-unavailable']"
                :disabled="item.unavailable"
                @click="openAdmissionQueue(item)"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <em>{{ item.note }}</em>
              </button>
            </div>
          </div>

          <section class="db-panel db-closure-lane">
            <div class="db-track">
              <span class="db-track-title">闭环指挥线</span>
              <span class="db-track-sub">事件处置 / 值班研判 / 重点人员</span>
            </div>
            <div class="db-closure-workspace">
              <div class="db-closure-primary">
                <div class="db-closure-items">
                  <button
                    v-for="item in closureLaneItems"
                    :key="item.key"
                    type="button"
                    :class="['db-closure-item', `tone-${item.tone}`]"
                    @click="item.route && $router.push(item.route)"
                  >
                    <span class="db-closure-label">{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                    <em>{{ item.note }}</em>
                  </button>
                </div>
                <DashboardWarningStream
                  class="db-closure-stream"
                  :warning-events="warningEvents"
                  :latest-danger-event="latestDangerEvent"
                  :format-time-ago="formatTimeAgo"
                  :open-warn-curve="openWarnCurve"
                  :open-handle-dialog="openHandleDialog"
                  :open-command-incident="openCommandIncident"
                />
              </div>
              <DashboardDispatchPanel
                class="db-closure-assist"
                :dispatch-priority="dispatchPriority"
                :dispatch-action-items="dispatchActionItems"
                :focus-warning-events="focusWarningEvents"
                :mine-ai-report="mineAiReport"
                :mine-ai-loading="mineAiLoading"
                :dashboard-ai-summary="dashboardAiSummary"
                :risk-dept-list="riskDeptList"
                :latest-danger-event="latestDangerEvent"
                @navigate="$router.push($event)"
                @person-click="goToEmployeeProfile"
                @toggle-ai="toggleMineAiPanel"
              />
            </div>
          </section>

        </main>

      </section>

      <section class="db-device-band">
        <DashboardDevicePanel
          class="db-panel db-col-device"
          :device-cards="deviceCards"
          @go-device="goToDeviceList"
        />
      </section>

      <section class="db-analysis-band">
        <div class="db-panel db-analysis-trends dm-main-model db-trend-command">
          <div class="db-track">
            <span class="db-track-title">趋势研判</span>
            <span class="db-track-sub">{{ trendBlockTitle }} / {{ hourDistTitle }}</span>
          </div>
          <div class="db-trend-grid">
            <div class="dm-data-block dm-data-block-trend">
              <div class="dm-block-hd">
                <span class="dm-block-title">{{ trendBlockTitle }}</span>
                <span class="dm-block-sub">异常率变化</span>
              </div>
              <div ref="unifiedTrendChart" class="dm-chart-flex"></div>
            </div>
            <div class="dm-data-block">
              <div class="dm-block-hd">
                <span class="dm-block-title">{{ hourDistTitle }}</span>
              </div>
              <div id="hourDistChart" class="dm-chart-flex"></div>
            </div>
            <div class="dm-data-block">
              <div class="dm-block-hd">
                <span class="dm-block-title">预警类型</span>
                <span class="dm-block-sub">结构占比</span>
              </div>
              <div id="warnTypeChart" class="dm-chart-flex"></div>
            </div>
          </div>
          <div class="dm-model-footer">
            <div class="dm-mf-group is-handled">
              <div class="dm-mf-dot"></div>
              <span class="dm-mf-label">{{ periodLabel }}已处理</span>
              <span class="dm-mf-val">{{ warningEvents.filter(e=>e.handled).length }}</span>
              <span class="dm-mf-unit">件</span>
            </div>
            <div class="dm-mf-sep"></div>
            <div class="dm-mf-group is-pending">
              <div class="dm-mf-dot"></div>
              <span class="dm-mf-label">待处理</span>
              <span class="dm-mf-val">{{ warningEvents.filter(e=>!e.handled).length }}</span>
              <span class="dm-mf-unit">件</span>
            </div>
          </div>
        </div>

        <DashboardRightSidebar
          class="db-analysis-sidebar"
          mode="full"
          :period-label="periodLabel"
          :top5-display-data="top5DisplayData"
          :top5-max="top5Max"
          :warning-rate-list="warningRateList"
          :pre-shift-data="preShiftData"
          :mine-ai-report="mineAiReport"
          :mine-ai-loading="mineAiLoading"
          :latest-danger-event="latestDangerEvent"
          :kpi-unhandled-high="kpiUnhandledHigh"
          :focus-warning-count="focusWarningEvents.length"
          @open-employee="openEmployeeDrawer"
          @toggle-ai="toggleMineAiPanel"
          @show-ai="mineAiDialogVisible = true"
        />
      </section>

    </div>

  <DashboardDialogs
    :emp-drawer="empDrawer"
    :handle-dialog="handleDialog"
    :dept-person-modal="deptPersonModal"
    :dept-detail-modal="deptDetailModal"
    :metric-detail-modal="metricDetailModal"
    :warn-curve-modal="warnCurveModal"
    :trend-block-title="trendBlockTitle"
    :alert-type-label="alertTypeLabel"
    :format-warn-time="formatWarnTime"
    :load-dept-person-chart="loadDeptPersonChart"
    :load-dept-detail-chart="loadDeptDetailChart"
    :load-metric-detail-chart="loadMetricDetailChart"
    :init-warn-curve-chart="initWarnCurveChart"
    :submit-handle="submitHandle"
  />

  <IncidentCommandDrawer
    v-model:visible="incidentDrawerVisible"
    :event="currentIncidentEvent"
    source-page="dashboard"
    @updated="handleIncidentUpdated"
    @open-command-center="goToCommandIncident(currentIncidentEvent)"
  />

  </div>

</template>

<script>
import { dashboardComputed } from './dashboard-computed'
import { createDashboardPageState } from './dashboard-page-state'
import { dashboardViewActions } from './dashboard-view-actions'
import { dashboardChartMethods } from './dashboard-chart-methods'
import { dashboardDetailMethods } from './dashboard-detail-methods'
import { activateDashboardPage, mountDashboardPage, unmountDashboardPage } from './dashboard-lifecycle'
import { dashboardRuntimeMethods } from './dashboard-runtime'
import {
  buildDashboardAdmissionQueueItems,
  buildDashboardClosureLaneItems,
  dashboardCommandWorkflowMethods
} from './dashboard-command-workflow'
import DashboardDispatchPanel from './components/DashboardDispatchPanel.vue'
import DashboardPersonSearch from './components/DashboardPersonSearch.vue'
import DashboardRightSidebar from './components/DashboardRightSidebar.vue'
import DashboardDevicePanel from './components/DashboardDevicePanel.vue'
import DashboardDialogs from './components/DashboardDialogs.vue'
import DashboardWarningStream from './components/DashboardWarningStream.vue'
import IncidentCommandDrawer from '../../safety-command/components/IncidentCommandDrawer.vue'

export default {
  name: 'HealthDashboard',
  components: { DashboardDispatchPanel, DashboardPersonSearch, DashboardRightSidebar, DashboardDevicePanel, DashboardDialogs, DashboardWarningStream, IncidentCommandDrawer },
  data() {
    return createDashboardPageState()
  },

  computed: {
    ...dashboardComputed,
    headerMetricStripItems() {
        return (this.headerKpis || []).map((item, index) => ({
          key: `${item.label}-${index}`,
          label: item.label,
          value: item.valHtml ? String(item.valHtml).replace(/<[^>]+>/g, ' ') : String(item.val ?? '--'),
        note: this.normalizeHeaderMetricNote(item.sub),
        tone: this.resolveHeaderMetricTone(item.cls),
          clickable: Boolean(item.clickable),
          route: item.route
        }))
      },
      closureLaneItems() {
        return buildDashboardClosureLaneItems({
          warningSummary: this.commandSummary?.warning,
          preShiftData: this.preShiftData
        })
      },
      admissionQueueItems() {
        return buildDashboardAdmissionQueueItems({
          preShiftData: this.preShiftData,
          admissionSummary: this.commandSummary?.admission
        })
      },
      primaryVitalCards() {
        return (this.vitalCards || []).slice(0, 6)
      },
      supplementalVitalCards() {
        return (this.vitalCards || []).slice(6)
      }
    },

  mounted() {
    mountDashboardPage(this)
  },
  activated() {
    activateDashboardPage(this)
  },
  beforeUnmount() {
    unmountDashboardPage(this)
  },
  watch: {
    '$route.query': {
      immediate: true,
      deep: true,
      handler(query) {
        this.openIncidentFromRoute(query)
      }
    }
  },

  methods: {
    ...dashboardRuntimeMethods,
    ...dashboardCommandWorkflowMethods,
    ...dashboardViewActions,
    ...dashboardChartMethods,
    ...dashboardDetailMethods,
    resolveHeaderMetricTone(cls) {
      if (cls === 'kpi-red') return 'danger'
      if (cls === 'kpi-orange') return 'warning'
      if (cls === 'kpi-green' || cls === 'kpi-teal') return 'success'
      return 'primary'
    },
    normalizeHeaderMetricNote(note) {
      if (!note) return ''
      const normalized = String(note).replace(/\s+/g, ' ').trim()
      if (normalized === '数据加载中...') return '等待刷新'
      return normalized.length > 18 ? `${normalized.slice(0, 18)}…` : normalized
    },
    onHeaderMetricSelect(item) {
      if (item?.route) {
        this.$router.push(item.route)
      }
    },
  }
}
</script>

<style lang="scss">
@import './dashboard.scss';
</style>
