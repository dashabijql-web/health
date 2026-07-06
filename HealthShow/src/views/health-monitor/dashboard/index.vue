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

    <div class="db-control-body dm-bd" ref="dmBody">
      <section class="db-main-grid">
        <aside class="db-col-health">
          <div class="db-panel db-duty-snapshot db-panel--interactive" @click="openDeptPersonModal">
            <div class="db-track">
              <span class="db-track-title">{{ periodLabel }}检测人数</span>
              <span class="db-track-sub">
                <span class="dm-main-metrics-total">共 {{ totalPersons !== null ? totalPersons.toLocaleString() : '--' }} 人次</span>
                <span class="dm-inline-action">查看部门详情</span>
              </span>
            </div>
            <div class="dm-metrics-row">
              <div
                v-for="m in metricCards"
                :key="m.key"
                :class="['dm-metric-card', m.summary ? 'is-summary' : '']"
                :style="{ '--metric-tone': m.color }"
                @click.stop="onMetricCardClick(m)"
              >
                <div class="dm-metric-val">
                  <span>{{ m.val.toLocaleString() }}</span>
                  <em v-if="m.unit">{{ m.unit }}</em>
                </div>
                <div class="dm-metric-label">{{ m.label }}</div>
                <div class="dm-metric-bar-wrap">
                  <div class="dm-metric-bar" :style="{ width: `${m.pct}%` }"></div>
                </div>
              </div>
            </div>
          </div>

          <div class="db-panel db-health-snapshot">
            <div class="db-track">
              <span class="db-track-title">健康快照</span>
              <span class="db-track-sub">{{ periodLabel }}体征均值</span>
            </div>
            <div class="dm-vitals-grid">
              <div
                v-for="v in primaryVitalCards"
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
                  <div class="dm-vital-tag" :class="v.tagCls">{{ v.tag }}</div>
                </div>
              </div>
            </div>
            <div class="dm-vitals-supplemental">
              <button
                v-for="v in supplementalVitalCards"
                :key="`${v.label}-supplemental`"
                type="button"
                :class="['dm-vitals-supplemental__item', v.route ? 'is-clickable' : '']"
                :style="{ '--vital-tone': v.color, '--vital-tone-soft': `${v.color}14`, '--vital-tone-border': `${v.color}2b` }"
                @click="v.route && $router.push(v.route)"
              >
                <span class="dm-vitals-supplemental__label">{{ v.label }}</span>
                <span class="dm-vitals-supplemental__value">{{ v.val }}<em v-if="v.unit">{{ v.unit }}</em></span>
                <span :class="['dm-vitals-supplemental__tag', v.tagCls]">{{ v.tag }}</span>
              </button>
            </div>
            <div class="dm-assess-bars">
              <div
                v-for="item in healthAssess"
                :key="`health-assess-${item.label}`"
                class="dm-assess-row"
              >
                <span class="dm-assess-label">{{ item.label }}</span>
                <span class="dm-assess-track">
                  <span class="dm-assess-fill" :style="{ width: `${item.pct}%`, background: item.color }"></span>
                </span>
                <span class="dm-assess-tag" :style="{ color: item.color }">{{ item.tag }}</span>
              </div>
            </div>
          </div>

          <DashboardDevicePanel
            class="db-panel db-col-device"
            :device-cards="deviceCards"
            @go-device="goToDeviceList"
          />
        </aside>

        <main class="db-col-decision">
          <div class="db-panel db-ops-preshift db-panel--interactive" @click="$router.push('/health-monitor/mine-entry')">
            <div class="db-track">
              <span class="db-track-title">班前健康准入</span>
              <span class="db-track-sub">{{ (preShiftData?.failedCount || 0) > 0 ? `${preShiftData.failedCount} 人需复核` : '当前准入平稳' }}</span>
            </div>
            <div class="dm-preshift-body">
              <div class="dm-ps-ring-wrap">
                <svg viewBox="0 0 96 96" class="dm-ps-ring">
                  <circle cx="48" cy="48" r="38" fill="none" stroke="#1a2a4d" stroke-width="7"/>
                  <circle cx="48" cy="48" r="38" fill="none"
                    :stroke="(preShiftData?.preShiftRate || 0) >= 90 ? '#38ef7d' : (preShiftData?.preShiftRate || 0) >= 70 ? '#ffd200' : '#ff5252'"
                    stroke-width="7" stroke-linecap="round"
                    :stroke-dasharray="`${(preShiftData?.preShiftRate || 0) * 2.388} 239`"
                    stroke-dashoffset="60"
                  />
                </svg>
                <div class="dm-ps-ring-inner">
                  <div :class="['dm-ps-rate', (preShiftData?.preShiftRate || 0) >= 90 ? 'tone-success' : (preShiftData?.preShiftRate || 0) >= 70 ? 'tone-warning' : 'tone-danger']">
                    {{ preShiftData?.preShiftRate != null ? preShiftData.preShiftRate + '%' : '--' }}
                  </div>
                  <div class="dm-ps-rate-label">达标率</div>
                </div>
              </div>
              <div class="dm-ps-stats">
                <div class="dm-ps-stat">
                  <span class="dm-ps-stat-val">{{ preShiftData?.totalToday || 0 }}</span>
                  <span class="dm-ps-stat-label">今日检测</span>
                </div>
                <div class="dm-ps-stat dm-ps-ok">
                  <span class="dm-ps-stat-val">{{ preShiftData?.qualifiedCount || 0 }}</span>
                  <span class="dm-ps-stat-label">准入通过</span>
                </div>
                <div class="dm-ps-stat dm-ps-fail">
                  <span class="dm-ps-stat-val">{{ preShiftData?.failedCount || 0 }}</span>
                  <span class="dm-ps-stat-label">禁止入井</span>
                </div>
              </div>
            </div>
          </div>

          <section class="db-panel db-closure-lane">
            <div class="db-track">
              <span class="db-track-title">闭环指挥线</span>
              <span class="db-track-sub">先清待办，再看趋势</span>
            </div>
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
            />
          </section>

          <div class="db-panel db-duty-console dm-main-dispatch dm-command-dispatch-shell">
            <div class="db-track">
              <span class="db-track-title">值班决策面板</span>
              <span class="db-track-sub">任务闭环 / 班前准入 / 重点人员</span>
            </div>
            <DashboardDispatchPanel
              :dispatch-priority="dispatchPriority"
              :dispatch-action-items="dispatchActionItems"
              :kpi-unhandled-high="kpiUnhandledHigh"
              :pre-shift-data="preShiftData"
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
        </main>

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
import DashboardDispatchPanel from './components/DashboardDispatchPanel.vue'
import DashboardRightSidebar from './components/DashboardRightSidebar.vue'
import DashboardDevicePanel from './components/DashboardDevicePanel.vue'
import DashboardDialogs from './components/DashboardDialogs.vue'
import DashboardWarningStream from './components/DashboardWarningStream.vue'

export default {
  name: 'HealthDashboard',
  components: { DashboardDispatchPanel, DashboardRightSidebar, DashboardDevicePanel, DashboardDialogs, DashboardWarningStream },
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
        const pending = (this.warningEvents || []).filter((item) => !item.handled).length
        return [
          {
            key: 'high',
            label: '高危闭环',
            value: this.kpiUnhandledHigh || 0,
            note: '通知页优先处置',
            tone: (this.kpiUnhandledHigh || 0) > 0 ? 'danger' : 'success',
            route: '/alert-management/notifications'
          },
          {
            key: 'pending',
            label: '全量待办',
            value: pending,
            note: `${this.periodLabel}预警流`,
            tone: pending > 0 ? 'warning' : 'success',
            route: '/alert-management/records'
          },
          {
            key: 'entry',
            label: '准入复核',
            value: this.preShiftData?.failedCount || 0,
            note: '班前未通过',
            tone: (this.preShiftData?.failedCount || 0) > 0 ? 'warning' : 'success',
            route: '/health-monitor/mine-entry'
          },
          {
            key: 'device',
            label: '设备干预',
            value: this.deviceWarningCount || 0,
            note: `离线 ${this.deviceOffline || 0}`,
            tone: (this.deviceWarningCount || this.deviceOffline) ? 'warning' : 'success',
            route: { path: '/admin/device-list', query: { filter: 'warning' } }
          }
        ]
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

  methods: {
    ...dashboardRuntimeMethods,
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
    }
  }
}
</script>

<style lang="scss">
@import './dashboard.scss';
</style>

