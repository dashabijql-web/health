<template>
  <div class="dm-outer" @transitionend.stop @animationend.stop>
  <div class="dm-root" ref="dmScale">

    <!-- ══════════ HEADER ══════════ -->
    <header class="dm-hd">
      <PageHeroHeader
        class="dm-hd-hero"
        variant="cockpit"
        eyebrow="Cockpit Dashboard"
        title="信智科技职业健康监测管理系统"
        :description="dashboardHeroDescription"
      >
        <template #meta>
          <div class="dm-hd-meta">
            <span class="dm-live-dot"></span>
            <span class="dm-hd-meta-label">实时运行</span>
            <span class="dm-hd-time">{{ currentTime }}</span>
            <div class="dm-refresh-info" @click="fetchData(true)" title="点击立即刷新">
              <span class="dm-refresh-icon" :class="{ 'is-spinning': isRefreshing }">↻</span>
              <span class="dm-refresh-time">{{ lastRefreshText }}</span>
            </div>
          </div>
        </template>
        <template #actions>
          <div class="dm-period-tabs">
            <span
              v-for="p in periodOptions"
              :key="p.value"
              :class="['dm-period-tab', activePeriod === p.value ? 'is-active' : '']"
              @click="switchPeriod(p.value)"
            >{{ p.label }}</span>
          </div>
          <div class="dm-fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏展示'">
            <span>{{ isFullscreen ? '⊡' : '⛶' }}</span>
          </div>
        </template>
      </PageHeroHeader>

      <MetricStrip
        class="dm-hd-kpis"
        :items="headerMetricStripItems"
        dense
        @select="onHeaderMetricSelect"
      />
    </header>

    <!-- ══════════ BODY ══════════ -->
    <div class="dm-bd">

      <!-- ─── 左栏 ─── -->
      <aside class="dm-left">

        <!-- 体征健康评估 -->
        <div class="dm-panel dm-left-assess">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">体征健康评估</span>
            <span class="dm-ph-sub">实时均值分析</span>
          </div>
          <!-- 6个体征指标卡 -->
          <div class="dm-vitals-grid">
            <div class="dm-vital-card" v-for="v in vitalCards" :key="v.label"
                 :style="v.route ? 'cursor:pointer' : ''"
                 @click="v.route && $router.push(v.route)">
              <div class="dm-vital-icon" :style="{color: v.color, borderColor: v.color + '33', background: v.color + '12'}">
                <el-icon :size="16"><component :is="v.icon" /></el-icon>
              </div>
              <div class="dm-vital-body">
                <div class="dm-vital-val" :style="{color: v.color}">{{ v.val }}<span class="dm-vital-unit">{{ v.unit }}</span></div>
                <div class="dm-vital-label">{{ v.label }}</div>
              </div>
              <div class="dm-vital-tag" :class="v.tagCls">{{ v.tag }}</div>
            </div>
          </div>
        </div>

        <!-- 部门综合看板（数据量 + 预警量双柱对比） -->
        <div class="dm-panel dm-left-dept">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">部门综合看板</span>
            <span class="dm-ph-sub">检测人数 vs 异常人数</span>
          </div>
          <div class="dm-pc">
            <div id="deptDataChart" style="width:100%;height:100%"></div>
          </div>
        </div>

      </aside>

      <!-- ─── 中栏 ─── -->
      <main class="dm-main">

        <!-- 6指标概况卡片行 -->
        <div class="dm-panel dm-main-metrics" style="cursor:pointer" @click="openDeptPersonModal">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">{{ periodLabel }}检测人数</span>
            <span class="dm-ph-sub">共 {{ totalPersons !== null ? totalPersons.toLocaleString() : '--' }} 人次 <span style="font-size:10px;color:#00b4ff;margin-left:6px">▶ 点击查看部门详情</span></span>
          </div>
          <div class="dm-metrics-row">
            <div class="dm-metric-card" v-for="m in metricCards" :key="m.label"
                 @click.stop="onMetricCardClick(m)">
              <div class="dm-metric-val" :style="{color: m.color}">
                {{ m.val.toLocaleString() }}
              </div>
              <div class="dm-metric-label">{{ m.label }}</div>
              <div class="dm-metric-bar-wrap">
                <div class="dm-metric-bar" :style="{width: m.pct+'%', background: m.color}"></div>
              </div>
              <div class="dm-metric-records" :title="m.records.toLocaleString()+'条记录'">
                {{ m.records >= 10000 ? (m.records/10000).toFixed(1)+'万次' : m.records.toLocaleString()+'次' }}
              </div>
              <div class="dm-metric-rec-bar-wrap">
                <div class="dm-metric-rec-bar" :style="{width: m.recPct+'%', background: m.color+'66'}"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="dm-panel dm-main-dispatch">
          <div class="dm-ph">
            <span class="dm-ph-bar" style="background:#ffd200"></span>
            <span class="dm-ph-title">值班决策面板</span>
            <span class="dm-ph-sub">先处理异常，再看趋势</span>
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

        <div class="dm-panel dm-main-model">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">健康监测中心</span>
            <span class="dm-ph-sub">实时体征综合分析</span>
          </div>
          <div class="dm-model-body">

            <DashboardWarningStream
              :warning-events="warningEvents"
              :latest-danger-event="latestDangerEvent"
              :format-time-ago="formatTimeAgo"
              :open-warn-curve="openWarnCurve"
              :open-handle-dialog="openHandleDialog"
            />

            <!-- ── 右：两区数据 ── -->
            <div class="dm-model-data-col">

              <!-- 区1：各指标每日异常率趋势折线图 -->
              <div class="dm-data-block dm-data-block-trend">
                <div class="dm-block-hd">
                  <span class="dm-ph-bar"></span>
                  <span class="dm-block-title">{{ trendBlockTitle }}</span>
                  <span class="dm-block-sub">异常率变化</span>
                </div>
                <div ref="unifiedTrendChart" style="width:100%;flex:1;min-height:0;"></div>
              </div>

              <!-- 区3：预警时段分布（已删除部门风险排行，已合并到左侧栏） -->
              <div class="dm-data-block">
                <div class="dm-block-hd">
                  <span class="dm-ph-bar"></span>
                  <span class="dm-block-title">{{ hourDistTitle }}</span>
                </div>
                <div id="hourDistChart" style="width:100%;height:100%;flex:1;"></div>
              </div>

            </div><!-- /dm-model-data-col -->
          </div><!-- /dm-model-body -->

          <!-- 底部信息条（业务统计） -->
          <div class="dm-model-footer">
            <div class="dm-mf-dot" style="background:#ffd200"></div>
            <span class="dm-mf-label">{{ periodLabel }}已处理</span>
            <span class="dm-mf-val" style="color:#ffd200">{{ warningEvents.filter(e=>e.handled).length }}</span>
            <span style="color:#8ba6c8;font-size:12px">件</span>
            <div class="dm-mf-sep"></div>
            <div class="dm-mf-dot" style="background:#ff5252"></div>
            <span class="dm-mf-label">待处理</span>
            <span class="dm-mf-val" style="color:#ff5252">{{ warningEvents.filter(e=>!e.handled).length }}</span>
            <span style="color:#8ba6c8;font-size:12px">件</span>
          </div>
        </div>

        <DashboardDevicePanel
          :device-cards="deviceCards"
          @go-device="goToDeviceList"
        />

        <!-- 环境健康关联 -->
        <div class="dm-panel dm-main-env">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">环境健康关联</span>
            <span class="dm-ph-sub">CO浓度/粉尘 vs 血氧趋势（模拟）</span>
          </div>
          <div ref="envChartRef" class="dm-env-chart"></div>
        </div>

      </main>

      <!-- ─── 右栏 ─── -->
      <DashboardRightSidebar
        :period-label="periodLabel"
        :top5-display-data="top5DisplayData"
        :top5-max="top5Max"
        :warning-rate-list="warningRateList"
        :pre-shift-data="preShiftData"
        :mine-ai-report="mineAiReport"
        :mine-ai-loading="mineAiLoading"
        @open-employee="openEmployeeDrawer"
        @toggle-ai="toggleMineAiPanel"
        @show-ai="mineAiDialogVisible = true"
      />

    </div><!-- /dm-bd -->
  </div><!-- /dm-root -->

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

  </div><!-- /dm-outer -->

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
import PageHeroHeader from '@/components/health-shell/PageHeroHeader.vue'
import MetricStrip from '@/components/health-shell/MetricStrip.vue'

export default {
  name: 'HealthDashboard',
  components: { DashboardDispatchPanel, DashboardRightSidebar, DashboardDevicePanel, DashboardDialogs, DashboardWarningStream, PageHeroHeader, MetricStrip },
  data() {
    return createDashboardPageState()
  },

  computed: {
    ...dashboardComputed,
    dashboardHeroDescription() {
      const total = this.totalPersons !== null ? this.totalPersons.toLocaleString() : '--'
      const pending = (this.warningEvents || []).filter((item) => !item.handled).length
      return `${this.periodLabel}覆盖 ${total} 人次，当前待处理 ${pending} 条，${this.lastRefreshText}`
    },
    headerMetricStripItems() {
      return (this.headerKpis || []).map((item, index) => ({
        key: `${item.label}-${index}`,
        label: item.label,
        value: item.valHtml ? String(item.valHtml).replace(/<[^>]+>/g, ' ') : String(item.val ?? '--'),
        note: item.sub || '',
        tone: this.resolveHeaderMetricTone(item.cls),
        clickable: Boolean(item.clickable),
        route: item.route
      }))
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
    onHeaderMetricSelect(item) {
      if (item?.route) {
        this.$router.push(item.route)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import './dashboard.scss';
</style>

