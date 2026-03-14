<template>
  <div class="dm-outer" @transitionend.stop @animationend.stop>
  <div class="dm-root" ref="dmScale">

    <!-- ══════════ HEADER ══════════ -->
    <header class="dm-hd">
      <div class="dm-hd-left">
        <span class="dm-live-dot"></span>
        <h1 class="dm-hd-title">信智科技职业健康监测管理系统</h1>
      </div>

      <div class="dm-hd-kpis">
        <div class="dm-hd-kpi" v-for="k in headerKpis" :key="k.label"
             :style="k.clickable ? 'cursor:pointer' : ''"
             :title="k.clickable ? '点击查看详情' : ''"
             @click="k.clickable && onKpiClick(k)">
          <span v-if="k.valHtml" class="dm-hd-kpi-val" :class="k.cls" v-html="k.valHtml"></span>
          <span v-else class="dm-hd-kpi-val" :class="k.cls">{{ k.val }}</span>
          <span class="dm-hd-kpi-label">{{ k.label }}</span>
          <span v-if="k.sub" class="dm-hd-kpi-sub" :class="k.subCls">{{ k.sub }}</span>
        </div>
      </div>

      <div class="dm-hd-right">
        <!-- 时间维度切换 -->
        <div class="dm-period-tabs">
          <span
            v-for="p in periodOptions"
            :key="p.value"
            :class="['dm-period-tab', activePeriod === p.value ? 'is-active' : '']"
            @click="switchPeriod(p.value)"
          >{{ p.label }}</span>
        </div>
        <span class="dm-hd-time">{{ currentTime }}</span>
        <div class="dm-refresh-info" @click="fetchData" title="点击立即刷新">
          <span class="dm-refresh-icon" :class="{ 'is-spinning': isRefreshing }">↻</span>
          <span class="dm-refresh-time">{{ lastRefreshText }}</span>
        </div>
      </div>
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
            <div class="dm-vital-card" v-for="v in vitalCards" :key="v.label">
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
            <span class="dm-ph-sub">数据量 vs 预警量</span>
          </div>
          <div class="dm-pc">
            <div id="deptDataChart" style="width:100%;height:100%"></div>
          </div>
        </div>

      </aside>

      <!-- ─── 中栏 ─── -->
      <main class="dm-main">

        <!-- 6指标概况卡片行 -->
        <div class="dm-panel dm-main-metrics">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">{{ periodLabel }}检测数据量</span>
            <span class="dm-ph-sub">共 {{ totalRecords !== null ? totalRecords.toLocaleString() : '--' }} 条记录</span>
          </div>
          <div class="dm-metrics-row">
            <div class="dm-metric-card" v-for="m in metricCards" :key="m.label"
                 style="cursor:pointer"
                 @click="onMetricCardClick(m)">
              <div class="dm-metric-val" :style="{color: m.color}">
                {{ m.val.toLocaleString() }}
              </div>
              <div class="dm-metric-bar-wrap">
                <div class="dm-metric-bar" :style="{width: m.pct+'%', background: m.color}"></div>
              </div>
              <div class="dm-metric-label">{{ m.label }}</div>
            </div>
          </div>
        </div>

        <div class="dm-panel dm-main-model">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">健康监测中心</span>
            <span class="dm-ph-sub">实时体征综合分析</span>
          </div>
          <div class="dm-model-body">

            <!-- ── 左：视频 ── -->
            <div class="dm-model-video-col">
              <!-- 上方：实时在岗概况（始终为当前实时数据，不随时间段切换） -->
              <div class="dm-duty-bar">
                <div class="dm-duty-item">
                  <span class="dm-duty-val" style="color:#38ef7d">{{ (onDutyStats.onDuty > 0 || onDutyStats.offDuty > 0) ? onDutyStats.onDuty.toLocaleString() : '--' }}</span>
                  <span class="dm-duty-lbl">当前在岗</span>
                </div>
                <div class="dm-duty-sep"></div>
                <div class="dm-duty-item">
                  <span class="dm-duty-val" style="color:#8ba6c8">{{ (onDutyStats.onDuty > 0 || onDutyStats.offDuty > 0) ? onDutyStats.offDuty.toLocaleString() : '--' }}</span>
                  <span class="dm-duty-lbl">当前离岗</span>
                </div>
                <div class="dm-duty-sep"></div>
                <div class="dm-duty-item">
                  <span class="dm-duty-val" style="color:#ff5252">{{ onDutyStats.abnormal }}</span>
                  <span class="dm-duty-lbl">当前异常</span>
                </div>
              </div>

              <!-- 实时预警动态（从右栏移过来，已优化高度） -->
              <div class="dm-event-list-wrap">
                <!-- 页码指示器（固定在顶部，始终可见） -->
                <div class="dm-event-header" v-if="warningTotalPages > 1">
                  <span class="dm-event-title">实时预警</span>
                  <span class="dm-event-page-info">第 {{ warningCurrentPage }} / {{ warningTotalPages }} 页</span>
                  <div class="dm-event-page-btns">
                    <button class="dm-page-btn-sm" :disabled="warningCurrentPage === 1" @click="goToWarningPage('prev')">‹</button>
                    <button class="dm-page-btn-sm" :disabled="warningCurrentPage === warningTotalPages" @click="goToWarningPage('next')">›</button>
                  </div>
                </div>
                <div class="dm-event-list" ref="warningListMid" style="max-height:680px;overflow-y:auto;scrollbar-width:none;-ms-overflow-style:none">
                  <div
                    v-for="(ev, i) in paginatedWarningEvents" :key="i"
                    :class="['dm-event', ev.level === 'danger' ? 'ev-danger' : 'ev-warn', ev.level === 'danger' ? 'alert-item--critical' : '', ev.handled ? 'ev-handled' : '']"
                  >
                    <div class="dm-ev-row1">
                      <span :class="['dm-ev-badge', ev.level === 'danger' ? 'badge-danger' : 'badge-warn']">
                        {{ ev.level === 'danger' ? '危险' : '预警' }}
                      </span>
                      <span class="dm-ev-type">{{ ev.type }}</span>
                      <span class="dm-ev-time">{{ formatTimeAgo(ev.time) }}</span>
                    </div>
                    <div class="dm-ev-row2">
                      <span class="dm-ev-user">{{ ev.userName }}</span>
                      <span class="dm-ev-val">{{ ev.indicator }}: <em>{{ ev.value }}</em></span>
                      <span v-if="ev.handled" class="dm-ev-done">✓处理</span>
                      <span v-else>
                        <span class="dm-ev-pending">待处理</span>
                        <span class="dm-ev-handle-btn" @click.stop="openHandleDialog(ev)">处理</span>
                      </span>
                    </div>
                  </div>
                  <div v-if="!warningEvents.length" class="dm-empty">暂无预警事件</div>
                </div>
                <div class="dm-event-page" v-if="warningTotalPages > 1" style="margin-top:8px">
                  <button class="dm-page-btn" :disabled="warningCurrentPage === 1" @click="goToWarningPage('prev')">‹</button>
                  <span class="dm-page-info">{{ warningCurrentPage }} / {{ warningTotalPages }}</span>
                  <button class="dm-page-btn" :disabled="warningCurrentPage === warningTotalPages" @click="goToWarningPage('next')">›</button>
                </div>
              </div>

              <!-- 下方：最新预警横幅 -->
              <div class="dm-latest-warn" v-if="latestDangerEvent">
                <span class="dm-lw-dot"></span>
                <span class="dm-lw-name">{{ latestDangerEvent.userName }}</span>
                <span class="dm-lw-sep">·</span>
                <span class="dm-lw-type">{{ latestDangerEvent.type }}</span>
                <span class="dm-lw-sep">·</span>
                <em class="dm-lw-val">{{ latestDangerEvent.value }}</em>
                <span class="dm-lw-pending">待处理</span>
              </div>
              <div class="dm-latest-warn dm-lw-empty" v-else>
                <span class="dm-lw-dot" style="background:#38ef7d;box-shadow:0 0 6px #38ef7d"></span>
                <span style="color:#38ef7d;font-size:12px">当前无危险预警</span>
              </div>

            </div>

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

        <!-- 设备状态 -->
        <div class="dm-panel dm-main-device">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">设备状态</span>
            <span class="dm-ph-sub">激活 / 使用 / 预警</span>
          </div>
          <div class="dm-device-body">
            <!-- 左：数字卡片 -->
            <div class="dm-device-cards">
              <div class="dm-dcard" v-for="c in deviceCards" :key="c.label" :class="c.cls">
                <div class="dm-dcard-val">{{ c.val }}</div>
                <div class="dm-dcard-label">{{ c.label }}</div>
              </div>
            </div>
            <!-- 右：仪表盘 -->
            <div class="dm-device-gauges">
              <div class="dm-gauge-item">
                <div id="onlineRateChart" class="dm-gauge-chart"></div>
                <div class="dm-gauge-label">在线率</div>
              </div>
              <div class="dm-gauge-item">
                <div id="activeRateChart" class="dm-gauge-chart"></div>
                <div class="dm-gauge-label">激活率</div>
              </div>
              <div class="dm-gauge-item">
                <div id="usageRateChart" class="dm-gauge-chart"></div>
                <div class="dm-gauge-label">使用率</div>
              </div>
              <div class="dm-gauge-item">
                <div id="warningRateChart" class="dm-gauge-chart"></div>
                <div class="dm-gauge-label">预警率</div>
              </div>
            </div>
          </div>
        </div>

      </main>

      <!-- ─── 右栏 ─── -->
      <aside class="dm-right">

        <!-- 异常人员排行（与健康小贴士各占一半高度） -->
        <div class="dm-panel dm-right-rank">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">异常人员排行</span>
            <span class="dm-ph-sub">{{ periodLabel }}累计</span>
          </div>
          <div class="dm-top5-list" ref="top5List">
            <div class="dm-top5-row" v-for="(item,i) in top5DisplayData" :key="i"
                 @click="openEmployeeDrawer(item)" style="cursor:pointer">
              <span class="dm-top5-rank" :class="'rk-'+(i+1)">{{i+1}}</span>
              <span class="dm-top5-name">{{item.userName||item.name}}</span>
              <div class="dm-top5-bar-wrap">
                <div class="dm-top5-bar" :style="{width:(item.count/top5Max*100)+'%'}"></div>
              </div>
              <span class="dm-top5-val">{{item.count}}</span>
            </div>
            <div v-if="!top5DisplayData.length" class="dm-empty">暂无数据</div>
          </div>
        </div>

        <!-- 健康小贴士（与异常人员排行各占一半高度） -->
        <health-tips :count="15" class="dm-right-tips" />

        <!-- 指标预警率分析 -->
        <div class="dm-panel dm-right-warnrate">
          <div class="dm-ph">
            <span class="dm-ph-bar"></span>
            <span class="dm-ph-title">指标预警率分析</span>
            <span class="dm-ph-sub">{{ periodLabel }}触发预警人员占比</span>
          </div>
          <div class="dm-warn-stats">
            <div v-if="!warningRateList.length" class="dm-empty" style="padding:40px 20px;text-align:center;color:#4a6080;font-size:12px">
              暂无数据
            </div>
            <div v-for="item in warningRateList" :key="item.name" class="dm-warn-item">
              <div class="dm-warn-icon-wrap">
                <el-icon :size="15"><component :is="getWarningIcon(item.name)" /></el-icon>
              </div>
              <div class="dm-warn-body">
                <div class="dm-warn-top">
                  <span class="dm-warn-name">{{ item.name }}</span>
                  <span class="dm-warn-pct" :style="{color: getWarnColor(item.rate)}">{{ item.rate }}%</span>
                </div>
                <div class="dm-warn-bar-bg">
                  <div class="dm-warn-bar-fill" :style="{width: item.rate+'%', background: getWarnGradient(item.rate)}"></div>
                </div>
              </div>
              <div class="dm-warn-tag" :class="getWarnTagClass(item.rate)">{{ getWarnTagLabel(item.rate) }}</div>
            </div>
          </div>
        </div>

      </aside>

    </div><!-- /dm-bd -->
  </div><!-- /dm-root -->
  </div><!-- /dm-outer -->

  <!-- ══ 员工健康档案 Drawer ══ -->
  <el-drawer
    v-model="empDrawer.visible"
    title=""
    direction="rtl"
    size="520px"
    :append-to-body="true"
    :destroy-on-close="true"
    class="dm-emp-drawer"
  >
    <template #header>
      <div class="dm-drawer-hd">
        <div class="dm-drawer-avatar">{{ (empDrawer.data.empName || empDrawer.userName || '?').charAt(0) }}</div>
        <div>
          <div class="dm-drawer-name">{{ empDrawer.data.empName || empDrawer.userName || '--' }}</div>
          <div class="dm-drawer-meta">
            {{ empDrawer.data.deptName || '--' }} · {{ empDrawer.data.jobTypeName || '--' }}
          </div>
        </div>
        <div class="dm-drawer-warn-count">
          <span class="dm-dwc-num">{{ empDrawer.abnormalCount }}</span>
          <span class="dm-dwc-label">次异常</span>
        </div>
      </div>
    </template>

    <div v-loading="empDrawer.loading" class="dm-drawer-body">

      <!-- 基础体征卡片 -->
      <div class="dm-drawer-vitals">
        <div class="dm-dv-card" v-for="v in empDrawer.vitals" :key="v.label">
          <div class="dm-dv-val" :style="{color: v.color}">
            {{ v.val }}<span class="dm-dv-unit">{{ v.unit }}</span>
          </div>
          <div class="dm-dv-label">{{ v.label }}</div>
          <div class="dm-dv-status" :class="v.statusCls">{{ v.statusText }}</div>
        </div>
      </div>

      <!-- 近7日趋势图 -->
      <div class="dm-drawer-section">
        <div class="dm-ds-title">{{ trendBlockTitle }}</div>
        <div id="empTrendChart" style="width:100%;height:200px;"></div>
      </div>

      <!-- 健康评分雷达 -->
      <div class="dm-drawer-section">
        <div class="dm-ds-title">健康评分</div>
        <div id="empRadarChart" style="width:100%;height:180px;"></div>
      </div>

      <!-- 近期预警记录 -->
      <div class="dm-drawer-section">
        <div class="dm-ds-title">
          近期预警记录
          <span class="dm-ds-badge">{{ empDrawer.warnings.length }} 条</span>
        </div>
        <div class="dm-warn-table">
          <div class="dm-wt-row dm-wt-head">
            <span style="width:120px">时间</span>
            <span style="width:90px">类型</span>
            <span style="width:60px">数值</span>
            <span style="flex:1">描述</span>
            <span style="width:50px">级别</span>
          </div>
          <div class="dm-wt-row" v-for="(w, i) in empDrawer.warnings.slice(0,15)" :key="i">
            <span style="width:120px;font-size:11px">{{ formatWarnTime(w.createTime) }}</span>
            <span style="width:90px">{{ alertTypeLabel(w.warningType) }}</span>
            <span style="width:60px;font-family:Consolas;color:#FFB84D">{{ w.warningValue }}</span>
            <span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#8ba6c8">
              {{ w.warningMessage || '--' }}
            </span>
            <span style="width:50px">
              <span :class="['dm-wt-level', w.warningLevel === 3 ? 'lv-danger' : w.warningLevel === 2 ? 'lv-warn' : 'lv-info']">
                {{ w.warningLevel === 3 ? '严重' : w.warningLevel === 2 ? '预警' : '提示' }}
              </span>
            </span>
          </div>
          <div v-if="!empDrawer.warnings.length" class="dm-empty" style="padding:20px">暂无预警记录</div>
        </div>
      </div>

    </div>
  </el-drawer>

  <!-- 预警处理弹窗 -->
  <el-dialog
    v-model="handleDialog.visible"
    title="处理预警"
    width="400px"
    :append-to-body="true"
    class="dm-handle-dialog"
  >
    <div class="dm-hd-info">
      <div class="dm-hd-row"><span class="dm-hd-key">人员</span><span class="dm-hd-val">{{ handleDialog.event?.userName }}</span></div>
      <div class="dm-hd-row"><span class="dm-hd-key">类型</span><span class="dm-hd-val">{{ handleDialog.event?.type }}</span></div>
      <div class="dm-hd-row"><span class="dm-hd-key">数值</span><span class="dm-hd-val" style="color:#FFB84D">{{ handleDialog.event?.value }}</span></div>
    </div>
    <el-input
      v-model="handleDialog.remark"
      type="textarea"
      :rows="3"
      placeholder="请输入处理备注（可选）"
      style="margin-top:12px"
    />
    <template #footer>
      <el-button @click="handleDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="handleDialog.submitting" @click="submitHandle">确认处理</el-button>
    </template>
  </el-dialog>

</template>

<script>
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import {
  getDashboardOverview,
  getBodyIndicators,
  getDataTop5,
  getDeviceActivation,
  getWarningEvents,
  getDeptHealthCounts,
  getDailyTrend,
  getWarningCounts
} from '@/api/health'
import { getWarningTypes } from '@/api/statistics'
import { getHealthPortrait } from '@/api/health-portrait'
import { handleRiskWarning } from '@/api/risk-warning'
import { getRealtimeStatistics } from '@/api/realtime'
import HealthTips from '@/components/HealthTips.vue'

export default {
  name: 'HealthDashboard',
  components: { HealthTips },
  data() {
    return {
      currentTime: '',
      timeInterval: null,
      refreshTimer: null,
      resizeTimer: null,
      pageScrollInterval: null,
      warningPageTimer: null,
      top5ScrollInterval: null,
      riskScrollInterval: null,
      onDutyStats: { onDuty: 0, offDuty: 0, abnormal: 0 },
      checkData: {},
      bodyIndicators: {},
      top5Data: [],
      deptDataList: [],
      warningRates: [],
      deviceStats: { total: 0, activeRate: 0, usageRate: 0, warningRate: 0 },
      warningEvents: [],
      warningTypesData: [],
      warningCurrentPage: 1,
      warningPageSize: 15,
      charts: {},
      metricList: [
        { key: 'heartRate',   label: '心率',  color: '#00d4ff', icon: 'Monitor' },
        { key: 'bloodOxygen', label: '血氧',  color: '#67C23A', icon: 'FirstAidKit' },
        { key: 'steps',       label: '步数',  color: '#F56C6C', icon: 'Promotion' },
        { key: 'temperature', label: '体温',  color: '#00c8c8', icon: 'Sunny' },
        { key: 'pressure',    label: '压力',  color: '#a78bfa', icon: 'MagicStick' }
      ],
      // 功能一：时间维度
      activePeriod: 'month',
      periodOptions: [
        { label: '当日',  value: 'day'   },
        { label: '近7日', value: 'week'  },
        { label: '近30日', value: 'month' }
      ],
      // 功能二：员工档案 Drawer
      empDrawer: {
        visible: false,
        loading: false,
        userName: '',
        abnormalCount: 0,
        data: {},
        vitals: [],
        warnings: [],
        trendChart: null,
        radarChart: null,
      },
      // 功能三：预警处理弹窗
      handleDialog: {
        visible: false,
        event: null,
        remark: '',
        submitting: false,
      },
      // 功能四：刷新状态
      isRefreshing: false,
      lastRefreshTime: null,
      lastRefreshText: '加载中...',
      refreshTextTimer: null,
      // 改动1：决策型 KPI 数据字段
      kpiRealtimeOnline: 0,
      kpiRealtimeTotal: 0,
      kpiTodayWarnings: 0,
      kpiYesterdayWarnings: 0,
      kpiUnhandledHigh: 0,
      kpiUnhandledMid: 0,
      kpiRefreshTimer: null,
      // 改动3：体征异常率正常参考阈值（单位：%，超过视为异常偏高）
      VITAL_NORMAL_RANGES: {
        heartRate:   { max: 15 },
        bloodOxygen: { max: 10 },
        temperature: { max: 10 },
        pressure:    { max: 20 },
      },
      // 每日异常率趋势数据（来自后端真实数据）
      trendDailyData: [],
      // 预警分布数据（柱状图专用，按日/时统计）
      warningDistData: { labels: [], counts: [] },
    }
  },

  computed: {
    periodRange() {
      const today = dayjs()
      if (this.activePeriod === 'day') {
        return { startTime: today.format('YYYY-MM-DD'), endTime: today.format('YYYY-MM-DD') }
      }
      if (this.activePeriod === 'week') {
        return {
          startTime: today.subtract(6, 'day').format('YYYY-MM-DD'),
          endTime:   today.format('YYYY-MM-DD')
        }
      }
      return {
        startTime: today.subtract(29, 'day').format('YYYY-MM-DD'),
        endTime:   today.format('YYYY-MM-DD')
      }
    },
    periodLabel() {
      return { day: '当日', week: '近7日', month: '近30日' }[this.activePeriod]
    },
    trendDayCount() {
      return { day: 24, week: 7, month: 30 }[this.activePeriod]
    },
    trendXLabels() {
      if (this.activePeriod === 'day') {
        return Array.from({ length: 24 }, (_, i) => `${i}时`)
      }
      if (this.activePeriod === 'week') {
        return ['7天前', '6天前', '5天前', '4天前', '3天前', '昨天', '今天']
      }
      return Array.from({ length: 30 }, (_, i) => {
        const d = dayjs().subtract(29 - i, 'day')
        return (i % 7 === 0 || i === 29) ? d.format('MM/DD') : ''
      })
    },
    trendBlockTitle() {
      return { day: '今日体征趋势', week: '近7日体征趋势', month: '近30日体征趋势' }[this.activePeriod]
    },
    hourDistTitle() {
      return { day: '今日预警时段', week: '近7日预警时段', month: '近30日预警时段' }[this.activePeriod]
    },
    totalRecords() {
      const d = this.checkData
      // 如果 checkData 为空对象（API未返回），返回 null 而非 0
      if (!d || Object.keys(d).length === 0) return null
      return (d.heartRate||0)+(d.bloodOxygen||0)+(d.steps||0)+(d.temperature||0)+(d.pressure||0)
    },
    kpiOnline() {
      return Math.round(this.deviceStats.total * (this.deviceStats.usageRate || 0) / 100)
    },
    deviceOnline() {
      return Math.round((this.deviceStats.total||0) * (this.deviceStats.activeRate||0) / 100)
    },
    deviceOffline() {
      return Math.max(0, (this.deviceStats.total||0) - this.deviceOnline)
    },
    deviceWarningCount() {
      return Math.round((this.deviceStats.total||0) * (this.deviceStats.warningRate||0) / 100)
    },
    lowBatteryCount() {
      // 优先使用后端返回的 lowBattery 字段
      if (this.deviceStats.lowBattery !== undefined && this.deviceStats.lowBattery !== null) {
        return this.deviceStats.lowBattery
      }
      // 如果后端有低电量率，计算估算值
      if (this.deviceStats.lowBatteryRate && this.deviceStats.total) {
        return Math.round((this.deviceStats.total || 0) * (this.deviceStats.lowBatteryRate || 0) / 100)
      }
      // 否则返回 0
      return 0
    },
    healthPassRate() {
      const rates = this.warningRates
      // 数据未加载时返回 null，前端显示"--"
      if (!rates || !rates.length) return null
      const avgWarn = rates.reduce((s, r) => s + (r.rate||0), 0) / rates.length
      return Math.max(0, Math.min(100, Math.round(100 - avgWarn)))
    },
    warningRateList() {
      return this.warningRates || []
    },
    kpiWarningDelta() {
      if (!this.kpiYesterdayWarnings) return null
      return Math.round((this.kpiTodayWarnings - this.kpiYesterdayWarnings) / this.kpiYesterdayWarnings * 100)
    },
    unhandledHighCount() {
      return (this.warningEvents || []).filter(e => !e.handled && e.level === 'danger').length
    },
    headerKpis() {
      const onlineRate = this.kpiRealtimeTotal
        ? Math.round(this.kpiRealtimeOnline / this.kpiRealtimeTotal * 100) : 0
      const delta = this.kpiWarningDelta
      const deltaText = delta !== null
        ? `昨日${this.kpiYesterdayWarnings} ${delta > 0 ? '↑' : '↓'}${Math.abs(delta)}%`
        : `昨日 ${this.kpiYesterdayWarnings}`
      // 计算异常人员数（从 warningEvents 中去重统计，优先使用唯一标识）
      const abnormalUsers = new Set(
        this.warningEvents
          .map(e => e.userCode || e.empCode || e.id)
          .filter(Boolean)
      ).size

      // KPI 加载状态判断：kpiRealtimeTotal === 0 表示未加载（初始值），应显示 "--"
      const hasKpiData = this.kpiRealtimeTotal > 0 || this.kpiRealtimeOnline > 0

      return [
        {
          label: '当前在线 / 在岗总数',
          val: hasKpiData ? `${this.kpiRealtimeOnline} / ${this.kpiRealtimeTotal}` : '--',
          cls: 'kpi-cyan', clickable: true, route: '/health-monitor/employee-archive',
          sub: hasKpiData ? `上报率 ${onlineRate}%` : '数据加载中...'
        },
        {
          label: '今日新增预警',
          val: this.kpiTodayWarnings,
          cls: 'kpi-red', clickable: true, route: '/health-monitor/risk-warning',
          sub: deltaText,
          subCls: delta !== null && delta > 0 ? 'sub-up' : 'sub-down'
        },
        {
          label: '未处理告警',
          valHtml: `<span style="color:#ff3b3b">${this.kpiUnhandledHigh}</span><span style="font-size:11px;color:#8ba6c8"> 高危 / </span><span style="color:#ffaa00">${this.kpiUnhandledMid}</span><span style="font-size:11px;color:#8ba6c8"> 中危</span>`,
          cls: 'kpi-red', clickable: true, route: '/health-monitor/risk-warning'
        },
        {
          label: '异常人员数',
          val: abnormalUsers,
          cls: 'kpi-orange', clickable: true, route: '/health-monitor/risk-warning',
          sub: `${this.activePeriod === 'day' ? '今日' : this.periodLabel}累计`
        },
        {
          label: '健康达标率',
          val: this.healthPassRate !== null ? this.healthPassRate + '%' : '--',
          cls: 'kpi-teal', clickable: false
        }
      ]
    },
    metricCards() {
      const maxVal = Math.max(...this.metricList.map(m => this.checkData[m.key]||0), 1)
      return this.metricList.map(m => ({
        label: m.label,
        color: m.color,
        val: this.checkData[m.key] || 0,
        pct: Math.round((this.checkData[m.key]||0) / maxVal * 100)
      }))
    },
    vitalCards() {
      const b = this.bodyIndicators
      return [
        {
          label: '人均心率', val: b.avgHeartRate || '--', unit: 'bpm', color: '#00d4ff', icon: 'Monitor',
          tag: !b.avgHeartRate ? '-' : b.avgHeartRate > 100 ? '偏快' : b.avgHeartRate < 55 ? '偏慢' : '正常',
          tagCls: !b.avgHeartRate ? '' : (b.avgHeartRate > 100 || b.avgHeartRate < 55) ? 'vtag-warn' : 'vtag-ok'
        },
        {
          label: '人均血氧', val: b.avgBloodOxygen || '--', unit: '%', color: '#67C23A', icon: 'FirstAidKit',
          tag: !b.avgBloodOxygen ? '-' : b.avgBloodOxygen < 90 ? '过低' : b.avgBloodOxygen < 95 ? '偏低' : '良好',
          tagCls: !b.avgBloodOxygen ? '' : b.avgBloodOxygen < 90 ? 'vtag-danger' : b.avgBloodOxygen < 95 ? 'vtag-warn' : 'vtag-ok'
        },
        {
          label: '压力均值', val: b.avgPressure || '--', unit: '', color: '#a78bfa', icon: 'MagicStick',
          tag: !b.avgPressure ? '-' : b.avgPressure > 80 ? '过高' : b.avgPressure > 60 ? '偏高' : '适中',
          tagCls: !b.avgPressure ? '' : b.avgPressure > 80 ? 'vtag-danger' : b.avgPressure > 60 ? 'vtag-warn' : 'vtag-ok'
        },
        {
          label: '人均体温', val: b.avgTemperature || '--', unit: '°C', color: '#00c8c8', icon: 'Sunny',
          tag: !b.avgTemperature ? '-' : b.avgTemperature > 37.5 ? '偏高' : b.avgTemperature < 36 ? '偏低' : '正常',
          tagCls: !b.avgTemperature ? '' : (b.avgTemperature > 37.5 || b.avgTemperature < 36) ? 'vtag-warn' : 'vtag-ok'
        },
        {
          label: '人均步数', val: b.avgSteps ? Math.round(b.avgSteps/1000*10)/10+'k' : '--', unit: '', color: '#F56C6C', icon: 'Promotion',
          tag: !b.avgSteps ? '-' : b.avgSteps < 5000 ? '偏少' : b.avgSteps > 12000 ? '充足' : '达标',
          tagCls: !b.avgSteps ? '' : b.avgSteps < 5000 ? 'vtag-warn' : 'vtag-ok'
        },
        {
          label: '收缩压(高压)',
          val: b.avgBloodPressureHigh ? Math.round(b.avgBloodPressureHigh) : '--',
          unit: 'mmHg',
          color: '#ff6b9d',
          icon: 'Top',
          tag: !b.avgBloodPressureHigh ? '-'
            : b.avgBloodPressureHigh <= 120 ? '正常'
            : b.avgBloodPressureHigh <= 140 ? '偏高'
            : '高血压',
          tagCls: !b.avgBloodPressureHigh ? ''
            : b.avgBloodPressureHigh <= 120 ? 'vtag-ok'
            : 'vtag-warn'
        },
        {
          label: '舒张压(低压)',
          val: b.avgBloodPressureLow ? Math.round(b.avgBloodPressureLow) : '--',
          unit: 'mmHg',
          color: '#a78bfa',
          icon: 'Bottom',
          tag: !b.avgBloodPressureLow ? '-'
            : b.avgBloodPressureLow <= 80 ? '正常'
            : b.avgBloodPressureLow <= 90 ? '偏高'
            : '高血压',
          tagCls: !b.avgBloodPressureLow ? ''
            : b.avgBloodPressureLow <= 80 ? 'vtag-ok'
            : 'vtag-warn'
        },
        {
          label: '人均卡路里',
          val: b.avgCalories ? Math.round(b.avgCalories) : '--',
          unit: 'kcal',
          color: '#FFB84D',
          icon: 'Odometer',
          tag: !b.avgCalories ? '-'
            : b.avgCalories < 300 ? '偏低'
            : b.avgCalories > 800 ? '充足'
            : '适中',
          tagCls: !b.avgCalories ? ''
            : b.avgCalories < 300 ? 'vtag-warn'
            : 'vtag-ok'
        }
      ]
    },
    healthAssess() {
      const b = this.bodyIndicators
      return [
        {
          label: '心率',
          pct: b.avgHeartRate ? Math.min(100, Math.round((b.avgHeartRate-40)/(120-40)*100)) : 0,
          color: (b.avgHeartRate > 100 || b.avgHeartRate < 55) ? '#ff5252' : '#38ef7d',
          tag: !b.avgHeartRate ? '--' : (b.avgHeartRate > 100) ? '偏快' : (b.avgHeartRate < 55) ? '偏慢' : '正常'
        },
        {
          label: '血氧',
          pct: b.avgBloodOxygen || 0,
          color: (b.avgBloodOxygen < 90) ? '#ff5252' : (b.avgBloodOxygen < 95) ? '#ffd200' : '#38ef7d',
          tag: !b.avgBloodOxygen ? '--' : (b.avgBloodOxygen < 90) ? '过低' : (b.avgBloodOxygen < 95) ? '偏低' : '良好'
        },
        {
          label: '压力',
          pct: Math.min(100, b.avgPressure || 0),
          color: (b.avgPressure > 80) ? '#ff5252' : (b.avgPressure > 60) ? '#ffd200' : '#38ef7d',
          tag: !b.avgPressure ? '--' : (b.avgPressure > 80) ? '过高' : (b.avgPressure > 60) ? '偏高' : '适中'
        }
      ]
    },
    latestDangerEvent() {
      return this.warningEvents.find(e => e.level === 'danger' && !e.handled) ||
             this.warningEvents.find(e => !e.handled) ||
             null
    },
    top5DisplayData() {
      // 过滤掉 userName 为 "--" 或空值的记录，避免显示无效数据
      return this.top5Data
        .filter(d => d.userName && d.userName !== '--')
        .slice(0, 15)
    },
    top5Max() {
      const data = this.top5DisplayData
      return Math.max(...data.map(d => d.count||0), 1)
    },
    trendItems() {
      const base = [
        { key:'heartRate',   label:'心率', color:'#00d4ff' },
        { key:'bloodOxygen', label:'血氧', color:'#67C23A' },
        { key:'pressure',    label:'压力', color:'#a78bfa' }
      ]
      return base.map(b => {
        const found = this.warningRates.find(r => r.name && r.name.includes(b.label))
        const latest = found ? found.rate : 0
        return { ...b, latest }
      })
    },
    warnTypeData() {
      const colors = ['#ff5252','#00d4ff','#a78bfa','#ffd200','#38ef7d','#FFB84D']
      const source = this.warningTypesData.length ? this.warningTypesData : (() => {
        const typeMap = {}
        this.warningEvents.forEach(e => { const k = e.type || '其他'; typeMap[k] = (typeMap[k] || 0) + 1 })
        return Object.entries(typeMap).map(([name, value]) => ({ name, value }))
      })()
      const total = source.reduce((s, d) => s + (d.value || d.count || 0), 0) || 1
      return source.slice(0, 6).map((d, i) => ({
        name:  d.name,
        value: d.value || d.count || 0,
        color: colors[i],
        pct:   Math.round((d.value || d.count || 0) / total * 100)
      }))
    },
    riskDeptList() {
      if (!this.deptDataList.length) return []
      const sorted = [...this.deptDataList].sort((a,b) => b.count - a.count)
      const max = sorted[0].count || 1
      const colors = ['#ff5252','#ffd200','#FFB84D','#00d4ff','#38ef7d','#00c8c8','#a78bfa','#67C23A','#E6A23C','#F56C6C']
      return sorted.map((d, i) => {
        const pct = Math.round(d.count / max * 100)
        const prev = d.prevCount || 0
        const delta = (prev > 0 && Math.abs(d.count - prev) / prev <= 2)
          ? Math.round((d.count - prev) / prev * 100)
          : null
        return { name: d.name, pct, count: d.count, color: colors[i % colors.length], delta }
      })
    },
    deviceCards() {
      // 判断设备数据是否已加载：total > 0 或 boundDevices > 0
      const hasDeviceData = (this.deviceStats.total > 0) || (this.deviceStats.boundDevices > 0)
      const showVal = (val) => hasDeviceData ? val : '--'

      return [
        { label: '设备总数', val: showVal(this.deviceStats.boundDevices ?? this.deviceStats.total),  cls: 'dc-blue'   },
        { label: '在线设备', val: showVal(this.deviceOnline),        cls: 'dc-green'  },
        { label: '离线设备', val: showVal(this.deviceOffline),       cls: 'dc-gray'   },
        { label: '预警设备', val: showVal(this.deviceWarningCount),  cls: 'dc-red'    },
        { label: '电量不足', val: showVal(this.lowBatteryCount),     cls: 'dc-orange' }
      ]
    },
    paginatedWarningEvents() {
      const start = (this.warningCurrentPage - 1) * this.warningPageSize
      return this.warningEvents.slice(start, start + this.warningPageSize)
    },
    warningTotalPages() {
      return Math.ceil(this.warningEvents.length / this.warningPageSize)
    }
  },

  mounted() {
    this.initTime()
    this.fetchData().then(() => {
      this.$nextTick(() => {
        this.initHourDistChart()
        this.initUnifiedTrendChart()
        this.initWarnTypeChart()
        this.startListScroll('top5List', 'top5ScrollInterval', 45)
        this.startListScroll('riskList', 'riskScrollInterval', 35)
      })
      this.fetchKpiData()
    })
    this.kpiRefreshTimer = setInterval(() => this.fetchKpiData(), 30000)
    this.startAutoRefresh()
    
    this._resizeHandler = () => this.handleResize()
    window.addEventListener('resize', this._resizeHandler)
    document.addEventListener('visibilitychange', this._onVisibilityChange = () => this.onVisibilityChange())
    this.refreshTextTimer = setInterval(() => this.updateRefreshText(), 5000)
  },
  activated() {
    this.fetchData()
    
    this.$nextTick(() => Object.values(this.charts).forEach(c => c && c.resize()))
  },
  beforeUnmount() {
    clearInterval(this.timeInterval)
    clearInterval(this.refreshTimer)
    clearInterval(this.pageScrollInterval)
    clearInterval(this.warningPageTimer)
    clearInterval(this.top5ScrollInterval)
    clearInterval(this.riskScrollInterval)
    clearInterval(this.refreshTextTimer)
    clearInterval(this.kpiRefreshTimer)
    clearTimeout(this.resizeTimer)
    window.removeEventListener('resize', this._resizeHandler)
    document.removeEventListener('visibilitychange', this._onVisibilityChange)
    Object.values(this.charts).forEach(c => c && c.dispose())
    if (this.empDrawer.trendChart) this.empDrawer.trendChart.dispose()
    if (this.empDrawer.radarChart) this.empDrawer.radarChart.dispose()
    const el = this.$refs.dmScale
    if (el) { el.style.transform = ''; el.style.width = ''; el.style.height = ''; el.style.transformOrigin = '' }
  },

  methods: {
    initTime() {
      this.updateTime()
      this.timeInterval = setInterval(this.updateTime, 1000)
    },
    updateTime() {
      this.currentTime = dayjs().format('YYYY年MM月DD日 HH:mm:ss')
    },

    async fetchData() {
      if (this.isRefreshing) return
      this.isRefreshing = true
      await Promise.allSettled([
        this.fetchDashboardData(),
        this.fetchBodyIndicators(),
        this.fetchDeviceData(),
        this.fetchWarningEvents(),
        this.fetchTop5Data(),
        this.loadDeptData(),
        this.fetchTrendDaily(),
        this.fetchWarningDist(),
        this.fetchWarningTypes()
      ])
      this.isRefreshing    = false
      this.lastRefreshTime = Date.now()
      this.updateRefreshText()
      // 在所有接口都返回后，用 usageRate 计算在岗/离岗
      const total      = this.deviceStats.total || 0
      const onDutyCount = Math.round(total * (this.deviceStats.usageRate || 0) / 100)
      this.onDutyStats.onDuty  = onDutyCount
      this.onDutyStats.offDuty = total - onDutyCount
      this.$nextTick(() => {
        this.initHourDistChart()
        this.initWarnTypeChart()
        this.initUnifiedTrendChart()
        this.initDeptChart()
      })
    },
    async fetchTop5Data() {
      try {
        const res = await getDataTop5(this.periodRange)
        if (res.code === 200 && res.data) {
          const raw = Array.isArray(res.data) ? res.data : (res.data.list || res.data.records || [])
          this.top5Data = raw.map(d => ({
            userName: d.userName || d.real_name || d.empName || d.userCode || '--',
            empCode:  d.userCode || d.empCode   || d.userId  || '',
            count:    d.count    || 0
          }))
        } else {
          this.top5Data = []
        }
      } catch(e) {
        this.top5Data = []
      }
    },
    async fetchDashboardData() {
      try {
        const res = await getDashboardOverview(this.periodRange)
        if (res.code === 200) this.checkData = res.data
      } catch(e) { this.checkData = {} }
    },
    async fetchBodyIndicators() {
      try {
        const res = await getBodyIndicators(this.periodRange)
        if (res.code === 200) this.bodyIndicators = res.data
      } catch(e) { this.bodyIndicators = {} }
    },
    async fetchDeviceData() {
      try {
        const res = await getDeviceActivation(this.periodRange)
        if (res.code === 200) {
          const d = res.data
          this.deviceStats  = d.stats       || { total: 0, activeRate: 0, usageRate: 0, warningRate: 0 }
          this.warningRates = d.warningRates || []
          this.$nextTick(() => { this.initDeviceCharts() })
        }
      } catch(e) {
        this.deviceStats  = { total: 0, activeRate: 0, usageRate: 0, warningRate: 0 }
        this.warningRates = []
        this.$nextTick(() => { this.initDeviceCharts() })
      }
    },
    async fetchWarningEvents() {
      try {
        const res = await getWarningEvents(this.periodRange)
        if (res.code === 200) {
          this.warningEvents = (res.data || []).map(e => ({
            id:        e.id,
            type:      e.warningType || e.type || e.indicatorName || '--',
            indicator: e.indicatorName || e.indicator || e.warningType || '--',
            value:     e.warningValue || e.value || e.actualValue || '--',
            time:      e.createTime || e.recordTime || e.warningTime || e.time,
            userName:  e.empName || e.userName || e.name || '--',
            userCode:  e.empCode || e.userCode || e.code,
            deptName:  e.deptName || e.department || '--',
            level:     (e.warningLevel === 3 || e.level === 3 || e.level === '3') ? 'danger'
                     : (e.warningLevel === 2 || e.level === 2 || e.level === '2') ? 'warn'
                     : 'info',
            handled:   e.handled === true || e.handled === 1 || e.status === 1,
          }))
          // 计算异常人员数（去重后的唯一用户数，而非预警总数）
          const abnormalUsers = new Set(
            this.warningEvents
              .filter(e => !e.handled)
              .map(e => e.userCode)
              .filter(Boolean)
          )
          this.onDutyStats.abnormal = abnormalUsers.size
          this.$nextTick(() => { this.startAutoScroll(); this.startListScroll('top5List', 'top5ScrollInterval', 45) })
        }
      } catch(e) { this.warningEvents = [] }
    },
    async loadDeptData() {
      try {
        const r = await getDeptHealthCounts(this.periodRange)
        if (r.code === 200 && Array.isArray(r.data) && r.data.length) {
          this.deptDataList = r.data
            .map(d => ({
              name: d.name || d.deptName || '',
              count: d.count || d.dataCount || 0,
              prevCount: d.prevCount || d.previousCount || 0
            }))
            .filter(d => d.name && d.name !== '')
        } else {
          this.deptDataList = []
        }
      } catch (error) {
        this.deptDataList = []
      }
      this.$nextTick(() => { this.initDeptChart(); this.startListScroll('riskList','riskScrollInterval',35) })
    },

    // ─── 改动1：决策型 KPI 数据加载 ───
    async fetchKpiData() {
      try {
        // KPI-1：在线/在岗总数（来自 /realtime/statistics）
        const statsRes = await getRealtimeStatistics()
        if (statsRes.code === 200 && statsRes.data) {
          const d = statsRes.data
          this.kpiRealtimeOnline = d.onlineCount ?? d.onlineUsers ?? d.onlineDevices ?? 0
          this.kpiRealtimeTotal  = d.totalCount  ?? d.totalUsers ?? d.totalEmployees ?? d.totalDevices ?? 0
        }
        // KPI-2：今日/昨日预警（各请求一次 warning-events）
        const today     = dayjs().format('YYYY-MM-DD')
        const yesterday = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
        const [todayRes, yesRes] = await Promise.all([
          getWarningEvents({ startTime: today,     endTime: today     }),
          getWarningEvents({ startTime: yesterday, endTime: yesterday })
        ])
        const countData = r => {
          if (!r || r.code !== 200 || !r.data) return 0
          if (Array.isArray(r.data)) return r.data.length
          return r.data.total ?? r.data.totalElements ?? 0
        }
        this.kpiTodayWarnings     = countData(todayRes)
        this.kpiYesterdayWarnings = countData(yesRes)
        // KPI-3：未处理告警（复用已加载的 warningEvents，按 level 区分高危/中危）
        const unhandled = this.warningEvents.filter(e => !e.handled)
        this.kpiUnhandledHigh = unhandled.filter(e => e.level === 'danger').length
        this.kpiUnhandledMid  = unhandled.filter(e => e.level === 'warn').length
      } catch(e) {
        // fail silently，不影响主看板其他数据
      }
    },

    // ─── 功能一：时间维度 ───
    switchPeriod(val) {
      if (this.activePeriod === val) return
      this.activePeriod = val
      this.fetchData().then(() => this.fetchKpiData())
    },

    // ─── 功能四：刷新状态 ───
    updateRefreshText() {
      if (!this.lastRefreshTime) { this.lastRefreshText = '加载中...'; return }
      const secs = Math.round((Date.now() - this.lastRefreshTime) / 1000)
      if (secs < 5)        this.lastRefreshText = '刚刚更新'
      else if (secs < 60)  this.lastRefreshText = `${secs}秒前`
      else                 this.lastRefreshText = `${Math.floor(secs/60)}分钟前`
    },

    // ─── 功能二：员工档案 Drawer ───
    async openEmployeeDrawer(item) {
      const userName = item.userName || item.name || ''
      const empCode  = item.empCode || item.userCode || userName

      this.empDrawer.visible       = true
      this.empDrawer.loading       = true
      this.empDrawer.userName      = userName
      this.empDrawer.abnormalCount = item.count || 0
      this.empDrawer.data          = {}
      this.empDrawer.vitals        = []
      this.empDrawer.warnings      = []

      try {
        const res = await getHealthPortrait(empCode)
        if (res.code === 200 && res.data) {
          const d = res.data
          this.empDrawer.data = {
            empName:        d.empName        || d.employee?.empName        || userName,
            deptName:       d.deptName       || d.employee?.deptName       || '--',
            jobTypeName:    d.jobTypeName    || d.employee?.jobTypeName    || '--',
          }
          const v = d.vitals || d.realtime || {}
          this.empDrawer.vitals = [
            {
              label: '心率', val: v.heartRate || '--', unit: 'bpm', color: '#ff5252',
              statusCls:  (v.heartRate >= 60 && v.heartRate <= 100) ? 'dv-ok' : 'dv-warn',
              statusText: (v.heartRate >= 60 && v.heartRate <= 100) ? '正常' : '异常'
            },
            {
              label: '血氧', val: v.bloodOxygen || '--', unit: '%', color: '#00d4ff',
              statusCls:  (v.bloodOxygen >= 95) ? 'dv-ok' : 'dv-warn',
              statusText: (v.bloodOxygen >= 95) ? '正常' : '偏低'
            },
            {
              label: '体温', val: v.temperature || '--', unit: '°C', color: '#ffd200',
              statusCls:  (v.temperature >= 36 && v.temperature <= 37.3) ? 'dv-ok' : 'dv-warn',
              statusText: (v.temperature >= 36 && v.temperature <= 37.3) ? '正常' : '异常'
            },
            {
              label: '血压',
              val: (v.systolic && v.diastolic) ? `${v.systolic}/${v.diastolic}` : '--',
              unit: 'mmHg',
              color: '#38ef7d',
              statusCls: (v.systolic && v.diastolic && v.systolic >= 90 && v.systolic <= 140 && v.diastolic >= 60 && v.diastolic <= 90) ? 'dv-ok' : (v.systolic || v.diastolic) ? 'dv-warn' : '',
              statusText: (v.systolic && v.diastolic && v.systolic >= 90 && v.systolic <= 140 && v.diastolic >= 60 && v.diastolic <= 90) ? '正常' : (v.systolic || v.diastolic) ? '异常' : '--'
            },
          ]
          this.empDrawer.warnings = d.warnings || d.recentWarnings || []
          this.$nextTick(() => {
            this.initEmpTrendChart(d.trend)
            this.initEmpRadarChart(d.healthScores)
          })
        }
      } catch(e) {
        // API 失败时 drawer 仍然打开，显示"暂无数据"
      } finally {
        this.empDrawer.loading = false
      }
    },

    initEmpTrendChart(trend) {
      const dom = document.getElementById('empTrendChart')
      if (!dom) return
      if (this.empDrawer.trendChart) this.empDrawer.trendChart.dispose()
      const chart = echarts.init(dom)
      this.empDrawer.trendChart = chart

      // 如果没有趋势数据，显示"暂无数据"
      if (!trend || !trend.dates || trend.dates.length === 0) {
        chart.setOption({
          backgroundColor: 'transparent',
          graphic: [{ type: 'text', left: 'center', top: 'middle',
            style: { text: '暂无趋势数据', fill: '#4a6080', fontSize: 12 } }]
        })
        return
      }

      const dates = trend.dates
      const hrs   = trend.heartRates   || []
      const bos   = trend.bloodOxygens || []
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,20,50,0.92)', borderColor: '#00d4ff', textStyle: { color: '#fff', fontSize: 12 } },
        legend: { data: ['心率', '血氧'], bottom: 0, textStyle: { color: '#8ba6c8', fontSize: 11 } },
        grid: { top: 20, right: 50, bottom: 36, left: 46 },
        xAxis: { type: 'category', data: dates, axisLabel: { color: '#8ba6c8', fontSize: 10 }, axisLine: { lineStyle: { color: '#1a4d8f44' } }, axisTick: { show: false } },
        yAxis: [
          { type: 'value', name: 'bpm', min: 40, max: 140, nameTextStyle: { color: '#8ba6c8', fontSize: 10 }, axisLabel: { color: '#8ba6c8', fontSize: 10 }, axisLine: { show: false }, splitLine: { lineStyle: { color: '#1a4d8f33' } } },
          { type: 'value', name: '%',   min: 85, max: 100, nameTextStyle: { color: '#8ba6c8', fontSize: 10 }, axisLabel: { color: '#8ba6c8', fontSize: 10 }, axisLine: { show: false }, splitLine: { show: false } }
        ],
        series: [
          { name: '心率', type: 'line', data: hrs, smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { color: '#ff5252', width: 2 }, itemStyle: { color: '#ff5252' }, areaStyle: { color: { type:'linear',x:0,y:0,x2:0,y2:1, colorStops:[{offset:0,color:'rgba(255,82,82,0.25)'},{offset:1,color:'rgba(255,82,82,0)'}] } } },
          { name: '血氧', type: 'line', yAxisIndex: 1, data: bos, smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { color: '#00d4ff', width: 2 }, itemStyle: { color: '#00d4ff' }, areaStyle: { color: { type:'linear',x:0,y:0,x2:0,y2:1, colorStops:[{offset:0,color:'rgba(0,212,255,0.2)'},{offset:1,color:'rgba(0,212,255,0)'}] } } }
        ]
      })
    },

    initEmpRadarChart(scores) {
      const dom = document.getElementById('empRadarChart')
      if (!dom) return
      if (this.empDrawer.radarChart) this.empDrawer.radarChart.dispose()
      const chart = echarts.init(dom)
      this.empDrawer.radarChart = chart

      // 如果没有评分数据，显示"暂无数据"
      if (!scores || (scores.heartRate === 0 && scores.bloodOxygen === 0 && scores.temperature === 0 && scores.bloodPressure === 0 && scores.activity === 0)) {
        chart.setOption({
          backgroundColor: 'transparent',
          graphic: [{ type: 'text', left: 'center', top: 'middle',
            style: { text: '暂无评分数据', fill: '#4a6080', fontSize: 12 } }]
        })
        return
      }

      const s = scores
      chart.setOption({
        backgroundColor: 'transparent',
        radar: {
          center: ['50%', '50%'], radius: '60%',
          indicator: [
            { name: '心率',   max: 100 },
            { name: '血氧',   max: 100 },
            { name: '体温',   max: 100 },
            { name: '血压',   max: 100 },
            { name: '活动量', max: 100 }
          ],
          name: { textStyle: { color: '#8ba6c8', fontSize: 11 } },
          axisLine:  { lineStyle: { color: 'rgba(0,212,255,0.15)' } },
          splitLine: { lineStyle: { color: 'rgba(0,212,255,0.12)' } },
          splitArea: { areaStyle: { color: ['rgba(0,212,255,0.02)', 'rgba(0,212,255,0.05)'] } }
        },
        series: [{
          type: 'radar',
          data: [{
            value: [s.heartRate||0, s.bloodOxygen||0, s.temperature||0, s.bloodPressure||0, s.activity||0],
            areaStyle: { color: 'rgba(0,212,255,0.15)' },
            lineStyle: { color: '#00d4ff', width: 2 },
            itemStyle: { color: '#00d4ff' }
          }]
        }]
      })
    },

    alertTypeLabel(type) {
      const map = { 1: '心率异常', 2: '血氧异常', 3: '体温异常', 4: '血压异常', 5: '综合异常' }
      return map[type] || '其他'
    },
    formatWarnTime(time) {
      if (!time) return '--'
      return dayjs(time).format('MM-DD HH:mm')
    },

    // ─── 功能三：预警快速处理 ───
    openHandleDialog(ev) {
      this.handleDialog.event      = ev
      this.handleDialog.remark     = ''
      this.handleDialog.visible    = true
      this.handleDialog.submitting = false
    },
    async submitHandle() {
      const ev = this.handleDialog.event
      if (!ev) return
      this.handleDialog.submitting = true
      try {
        await handleRiskWarning(ev.id, {
          handleBy:     this.$store.getters.name || '管理员',
          handleRemark: this.handleDialog.remark
        })
        ev.handled      = true
        ev.handleRemark = this.handleDialog.remark
        this.handleDialog.visible = false
        this.$message?.success('预警已处理')
      } catch(e) {
        this.$message?.error('处理失败，请重试')
      } finally {
        this.handleDialog.submitting = false
      }
    },

    // ─── ECharts ───
    initDeptChart() {
      const dom = document.getElementById('deptDataChart')
      if (!dom) return
      if (this.charts.dept) this.charts.dept.dispose()
      const chart = echarts.init(dom)
      this.charts.dept = chart

      // 合并数据量和预警量，按数据量排序
      const deptMap = {}
      this.deptDataList.forEach(d => {
        deptMap[d.name] = { name: d.name, dataCount: d.count, warningCount: 0 }
      })
      this.riskDeptList.forEach(d => {
        if (deptMap[d.name]) {
          deptMap[d.name].warningCount = d.count
        } else {
          deptMap[d.name] = { name: d.name, dataCount: 0, warningCount: d.count }
        }
      })
      const sorted = Object.values(deptMap).sort((a, b) => b.dataCount - a.dataCount).slice(0, 10)

      // 如果没有部门数据，显示"暂无数据"
      if (sorted.length === 0) {
        chart.setOption({
          backgroundColor: 'transparent',
          graphic: [{ type: 'text', left: 'center', top: 'middle',
            style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }]
        })
        return
      }

      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis', axisPointer: { type: 'shadow' },
          backgroundColor: 'rgba(10,20,50,0.9)', borderColor: '#00d4ff',
          textStyle: { color: '#fff', fontSize: 11 },
          formatter: p => {
            return `${p[0].name}<br/>` +
              `<span style="color:#00d4ff">●</span> 数据量: <b style="color:#00d4ff">${p[0].value.toLocaleString()}</b><br/>` +
              `<span style="color:#ff9800">●</span> 预警量: <b style="color:#ff9800">${p[1].value.toLocaleString()}</b>`
          }
        },
        legend: {
          data: ['数据量', '预警量'],
          top: 0, right: 10,
          textStyle: { color: '#8ba6c8', fontSize: 10 },
          itemWidth: 12, itemHeight: 8
        },
        grid: { left: 90, right: 20, top: 26, bottom: 4, containLabel: false },
        xAxis: {
          type: 'value',
          axisLine: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(0,200,255,0.07)', type: 'dashed' } },
          axisLabel: { color: '#8ba6c8', fontSize: 9, formatter: v => v >= 1000 ? (v/1000).toFixed(1)+'k' : v }
        },
        yAxis: {
          type: 'category',
          data: sorted.map(d => d.name),
          inverse: true,
          axisLine: { show: false }, axisTick: { show: false },
          axisLabel: { color: '#8ba6c8', fontSize: 9, width: 82, overflow: 'truncate', interval: 0 }
        },
        series: [
          {
            name: '数据量',
            type: 'bar', barMaxWidth: 10, barGap: '20%',
            data: sorted.map(d => d.dataCount),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                { offset: 0, color: '#00d4ff' },
                { offset: 1, color: '#00d4ff33' }
              ]),
              borderRadius: [0, 3, 3, 0]
            },
            label: { show: true, position: 'right', color: '#00d4ff', fontSize: 8, formatter: p => p.value >= 1000 ? (p.value/1000).toFixed(1)+'k' : p.value }
          },
          {
            name: '预警量',
            type: 'bar', barMaxWidth: 10,
            data: sorted.map(d => d.warningCount),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                { offset: 0, color: '#ff9800' },
                { offset: 1, color: '#ffcc0244' }
              ]),
              borderRadius: [0, 3, 3, 0]
            },
            label: { show: true, position: 'right', color: '#ff9800', fontSize: 8, formatter: p => p.value || '' }
          }
        ]
      })
    },

    initDeviceCharts() {
      const totalDevices = this.deviceStats.boundDevices ?? this.deviceStats.total ?? 1
      const onlineRate = totalDevices > 0 ? Math.round((this.deviceOnline / totalDevices) * 100) : 0
      this.initGauge('onlineRateChart',  onlineRate, '#00d4ff', '#F56C6C')
      this.initGauge('activeRateChart',  this.deviceStats.activeRate  || 0, '#67C23A', '#F56C6C')
      this.initGauge('usageRateChart',   this.deviceStats.usageRate   || 0, '#a78bfa', '#E6A23C')
      this.initGauge('warningRateChart', this.deviceStats.warningRate || 0, '#F56C6C', '#67C23A')
    },
    async fetchTrendDaily() {
      const days = { day: 7, week: 7, month: 30 }[this.activePeriod] || 30
      try {
        const res = await getDailyTrend(days)
        this.trendDailyData = (res.code === 200 && Array.isArray(res.data)) ? res.data : []
      } catch {
        this.trendDailyData = []
      }
    },
    async fetchWarningDist() {
      const groupBy = this.activePeriod === 'day' ? 'hour' : 'day'
      try {
        const res = await getWarningCounts({ ...this.periodRange, groupBy })
        this.warningDistData = (res.code === 200 && res.data)
          ? res.data
          : { labels: [], counts: [] }
      } catch {
        this.warningDistData = { labels: [], counts: [] }
      }
    },
    async fetchWarningTypes() {
      try {
        const month = this.periodRange.startTime.slice(0, 7)  // "YYYY-MM"
        const res = await getWarningTypes({ month })
        if (res.code === 200 && Array.isArray(res.data)) {
          this.warningTypesData = res.data
        }
      } catch(_) {}
    },
    initUnifiedTrendChart() {
      const dom = this.$refs.unifiedTrendChart
      if (!dom) return
      if (this.charts.unifiedTrend) this.charts.unifiedTrend.dispose()
      const chart = echarts.init(dom)
      this.charts.unifiedTrend = chart

      const rawData = this.trendDailyData
      if (!rawData.length) {
        chart.setOption({
          backgroundColor: 'transparent',
          graphic: [{ type: 'text', left: 'center', top: 'middle',
            style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }]
        })
        return
      }

      const dates = rawData.map(d => d.date)

      // 字段名兼容性检查：支持多种后端字段命名
      const sampleRecord = rawData[0] || {}
      const getFieldKey = (possibleKeys) => {
        return possibleKeys.find(key => sampleRecord.hasOwnProperty(key)) || possibleKeys[0]
      }

      // 左轴：心率/血氧/体温异常率；右轴：压力异常率（量纲可能偏高）
      const seriesLeft = [
        { name: '心率', key: getFieldKey(['heartRateRate', 'hrRate', 'heartRateAbnormalRate']), color: '#00d4ff', threshold: (this.VITAL_NORMAL_RANGES.heartRate.max)   },
        { name: '血氧', key: getFieldKey(['bloodOxygenRate', 'boRate', 'bloodOxygenAbnormalRate']), color: '#67C23A', threshold: (this.VITAL_NORMAL_RANGES.bloodOxygen.max) },
        { name: '体温', key: getFieldKey(['temperatureRate', 'tempRate', 'temperatureAbnormalRate']), color: '#ffd200', threshold: (this.VITAL_NORMAL_RANGES.temperature.max) },
      ]
      const seriesRight = [
        { name: '压力', key: getFieldKey(['pressureRate', 'stressRate', 'pressureAbnormalRate']), color: '#a78bfa', threshold: (this.VITAL_NORMAL_RANGES.pressure.max), yAxisIndex: 1 },
      ]
      const allSeries = [...seriesLeft, ...seriesRight]

      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(10,20,50,0.92)',
          borderColor: '#00d4ff33',
          borderWidth: 1,
          textStyle: { color: '#fff', fontSize: 11 },
          formatter(params) {
            const header = `<div style="color:#8ba0bb;margin-bottom:3px">${params[0].axisValue}</div>`
            const rows = params.map(p =>
              `<div><span style="color:${p.color}">● </span>${p.seriesName}异常率：<b style="color:${p.color}">${p.value ?? '--'}%</b></div>`
            ).join('')
            return header + rows
          }
        },
        legend: {
          top: 2, right: 4,
          textStyle: { color: '#8ba0bb', fontSize: 10 },
          itemWidth: 14, itemHeight: 3,
          data: allSeries.map(s => ({ name: s.name, itemStyle: { color: s.color } }))
        },
        grid: { left: 32, right: 36, top: 22, bottom: 20 },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: { color: '#6a7a9a', fontSize: 10, formatter: v => v.slice(5) },
          axisLine: { lineStyle: { color: '#1e3a5f' } },
          splitLine: { show: false }
        },
        yAxis: [
          {
            type: 'value', min: 0,
            axisLabel: { color: '#6a7a9a', fontSize: 10, formatter: v => v + '%' },
            splitLine: { lineStyle: { color: 'rgba(100,160,255,0.08)' } }
          },
          {
            type: 'value', min: 0,
            position: 'right',
            axisLabel: { color: '#a78bfa', fontSize: 10, formatter: v => v + '%' },
            splitLine: { show: false }
          }
        ],
        series: allSeries.map(s => ({
          name: s.name,
          type: 'line',
          smooth: true,
          yAxisIndex: s.yAxisIndex || 0,
          symbol: 'circle', symbolSize: 4, showSymbol: false,
          data: rawData.map(d => d[s.key] ?? null),
          lineStyle: { color: s.color, width: 2 },
          itemStyle: { color: s.color },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: s.color + '33' },
                { offset: 1, color: s.color + '05' }
              ]
            }
          },
          markLine: {
            silent: true, symbol: 'none',
            lineStyle: { color: 'rgba(74,222,128,0.45)', type: 'dashed', width: 1 },
            data: [{ yAxis: s.threshold, name: '安全阈值' }]
          },
          markArea: {
            silent: true,
            itemStyle: { color: 'rgba(74,222,128,0.04)' },
            data: [[{ yAxis: 0 }, { yAxis: s.threshold }]]
          }
        }))
      })
    },

    initWarnTypeChart() {
      const dom = document.getElementById('warnTypeChart')
      if (!dom) return
      if (this.charts.warnType) this.charts.warnType.dispose()
      const chart = echarts.init(dom)
      this.charts.warnType = chart
      const data = this.warnTypeData
      if (!data.length) {
        chart.setOption({
          backgroundColor: 'transparent',
          graphic: [{ type: 'text', left: 'center', top: 'middle',
            style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }]
        })
        return
      }
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(10,20,50,0.9)',
          borderColor: '#00d4ff',
          textStyle: { color: '#fff', fontSize: 12 },
          formatter: '{b}: {c}次 ({d}%)'
        },
        series: [{
          type: 'pie',
          radius: ['52%', '78%'],
          center: ['50%', '50%'],
          data: data.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } })),
          label: { show: false },
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,212,255,0.3)' }
          }
        }]
      })
    },

    initHourDistChart() {
      const dom = document.getElementById('hourDistChart')
      if (!dom) return
      if (this.charts.hourDist) this.charts.hourDist.dispose()
      const chart = echarts.init(dom)
      this.charts.hourDist = chart

      let labels = []
      let vals   = []

      const dist = this.warningDistData || { labels: [], counts: [] }

      if (!dist.labels || !dist.counts || !Array.isArray(dist.labels) || !Array.isArray(dist.counts) || dist.counts.length === 0) {
        // 数据无效或全为空时显示"暂无数据"
        chart.setOption({
          backgroundColor: 'transparent',
          graphic: [{ type: 'text', left: 'center', top: 'middle',
            style: { text: '暂无数据', fill: '#4a6080', fontSize: 13 } }]
        })
        return
      }

      if (this.activePeriod === 'day') {
        // 后端按小时返回 labels=[0..23], counts=[...]
        const hourCounts = new Array(24).fill(0)
        dist.labels.forEach((h, i) => {
          // 更鲁棒的小时解析：支持 "3", "03", "03:00", "2026-03-01 03:00:00" 等格式
          const hourStr = String(h).split(':')[0].split(' ').pop()
          const hour = parseInt(hourStr, 10)
          if (!isNaN(hour) && hour >= 0 && hour < 24) {
            hourCounts[hour] = dist.counts[i] || 0
          }
        })
        labels = Array.from({ length: 24 }, (_, i) => i % 3 === 0 ? i + 'h' : '')
        vals = hourCounts
      } else {
        // 后端按日返回 labels=['2026-03-01',...], counts=[...]
        labels = (dist.labels || []).map(d => String(d).slice(5))
        vals = dist.counts || []
      }

      const maxVal = Math.max(...vals, 1)
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(10,20,50,0.9)',
          borderColor: '#00d4ff',
          textStyle: { color: '#fff', fontSize: 10 },
          formatter: p => `${p[0].axisValue}：${p[0].value}次`
        },
        grid: { left: 4, right: 4, top: 4, bottom: 20 },
        xAxis: {
          type: 'category',
          data: labels,
          axisLabel: { color: '#8ba6c8', fontSize: 9, margin: 4 },
          axisLine: { show: false },
          axisTick: { show: false }
        },
        yAxis: { type: 'value', show: false },
        series: [{
          type: 'bar',
          data: vals,
          barMaxWidth: this.activePeriod === 'day' ? 10 : 8,
          itemStyle: {
            color: params => {
              const v = params.value
              if (v >= maxVal * 0.7) return '#ff5252'
              if (v >= maxVal * 0.4) return '#ffd200'
              return '#00d4ff'
            },
            borderRadius: [2, 2, 0, 0]
          }
        }]
      })
    },
    initGauge(id, value, highColor, lowColor) {
      const dom = document.getElementById(id)
      if (!dom) return
      if (this.charts[id]) this.charts[id].dispose()
      const chart = echarts.init(dom)
      this.charts[id] = chart
      const color = value >= 50 ? highColor : value >= 20 ? '#E6A23C' : lowColor
      chart.setOption({
        backgroundColor: 'transparent',
        series: [{
          type: 'gauge',
          startAngle: 200, endAngle: -20,
          radius: '90%', center: ['50%', '65%'],
          min: 0, max: 100,
          axisLine: {
            lineStyle: { width: 8, color: [[value/100, color], [1, 'rgba(26,77,143,0.25)']] }
          },
          pointer: { show: false },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          detail: {
            valueAnimation: true,
            formatter: '{value}%',
            color, fontSize: 13, fontWeight: 'bold', fontFamily: 'Consolas',
            offsetCenter: [0, '15%']
          },
          data: [{ value }]
        }]
      })
    },

    // ─── KPI / Metric 点击 ───
    onKpiClick(k) {
      if (k.route) this.$router.push(k.route)
    },
    onMetricCardClick(m) {
      // 所有指标卡片统一跳转到风险预警页
      this.$router.push('/health-monitor/risk-warning')
    },

    // ─── 辅助 ───
    getWarningIcon(name) {
      const map = { '压力预警率':'MagicStick', '体温预警率':'Sunny', '心率预警率':'Monitor', '血氧预警率':'FirstAidKit' }
      return map[name] || 'Warning'
    },
    getWarnColor(rate) {
      return rate >= 20 ? '#ff5252' : rate >= 10 ? '#ffd200' : '#38ef7d'
    },
    getWarnGradient(rate) {
      return rate >= 20 ? 'linear-gradient(90deg,#ff5252,#ff1744)' : rate >= 10 ? 'linear-gradient(90deg,#ffd200,#ff9800)' : 'linear-gradient(90deg,#00d4ff,#38ef7d)'
    },
    getWarnTagClass(rate) {
      return rate >= 20 ? 'tag-danger' : rate >= 10 ? 'tag-warn' : 'tag-ok'
    },
    getWarnTagLabel(rate) {
      return rate >= 20 ? '偏高' : rate >= 10 ? '注意' : '正常'
    },
    formatTimeAgo(timestamp) {
      if (!timestamp) return ''
      const diff = Date.now() - new Date(timestamp).getTime()
      const mins = Math.floor(diff / 60000)
      if (mins < 1) return '刚刚'
      if (mins < 60) return `${mins}分钟前`
      const h = Math.floor(mins / 60)
      if (h < 24) return `${h}小时前`
      return dayjs(timestamp).format('MM-DD HH:mm')
    },

    // 手动翻页时滚动到顶部
    goToWarningPage(direction) {
      if (direction === 'prev' && this.warningCurrentPage > 1) {
        this.warningCurrentPage--
      } else if (direction === 'next' && this.warningCurrentPage < this.warningTotalPages) {
        this.warningCurrentPage++
      }
      this.$nextTick(() => {
        const list = this.$refs.warningListMid
        console.log('[手动翻页]', direction, '→ 第', this.warningCurrentPage, '页, scrollHeight=', list?.scrollHeight, 'clientHeight=', list?.clientHeight, '闭包 _warnScrollTop=', this._warnScrollTop)
        if (list) list.scrollTop = 0
        this._warnScrollTop = 0  // 同步重置闭包变量
      })
    },

    startAutoScroll() {
      if (this.pageScrollInterval) clearInterval(this.pageScrollInterval)
      if (this.warningPageTimer)   clearInterval(this.warningPageTimer)

      // 独立翻页定时器：每 8 秒切换到下一页（无论内容是否溢出）
      this.warningPageTimer = setInterval(() => {
        if (this.warningTotalPages > 1) {
          this.warningCurrentPage = this.warningCurrentPage < this.warningTotalPages
            ? this.warningCurrentPage + 1 : 1
          this.$nextTick(() => {
            const list = this.$refs.warningListMid
            console.log('[翻页定时器] 切到第', this.warningCurrentPage, '页, list.scrollHeight=', list?.scrollHeight, 'list.clientHeight=', list?.clientHeight, '重置 scrollTop=0, 闭包 scrollTop before reset=', this._warnScrollTop)
            if (list) list.scrollTop = 0
            this._warnScrollTop = 0  // 同步重置闭包变量
          })
        }
      }, 8000)

      // 平滑滚动（内容溢出时）
      this._warnScrollTop = 0
      this.$nextTick(() => {
        const list = this.$refs.warningListMid
        if (!list) return
        this.pageScrollInterval = setInterval(() => {
          const max = list.scrollHeight - list.clientHeight
          if (max <= 0) {
            if (this._warnScrollTop !== 0) console.log('[滚动interval] max<=0, 重置')
            this._warnScrollTop = 0
            return
          }
          if (this._warnScrollTop >= max) {
            console.log('[滚动interval] 到底了, scrollTop=', this._warnScrollTop, 'max=', max, '等待翻页')
            this._warnScrollTop = max
          } else {
            this._warnScrollTop += 1
            list.scrollTop = this._warnScrollTop
          }
        }, 40)
      })
    },

    startListScroll(refName, intervalKey, speed = 30) {
      if (this[intervalKey]) clearInterval(this[intervalKey])
      this.$nextTick(() => {
        const el = this.$refs[refName]
        if (!el) return
        let st = 0
        this[intervalKey] = setInterval(() => {
          const max = el.scrollHeight - el.clientHeight
          if (max <= 0) return
          st += 1
          if (st >= max) {
            // 滚到底后停顿1.5s回顶
            clearInterval(this[intervalKey])
            setTimeout(() => {
              el.scrollTop = 0
              st = 0
              this.startListScroll(refName, intervalKey, speed)
            }, 1500)
          } else {
            el.scrollTop = st
          }
        }, speed)
      })
    },
    startAutoRefresh() {
      this.refreshTimer = setInterval(() => this.fetchData(), 30000)
    },
    onVisibilityChange() {
      if (document.hidden) {
        clearInterval(this.refreshTimer)
        clearInterval(this.kpiRefreshTimer)
        clearInterval(this.refreshTextTimer)
      } else {
        this.fetchData()
        this.fetchKpiData()
        this.startAutoRefresh()
        this.kpiRefreshTimer = setInterval(() => this.fetchKpiData(), 30000)
        this.refreshTextTimer = setInterval(() => this.updateRefreshText(), 5000)
      }
    },
    handleResize() {
      clearTimeout(this.resizeTimer)
      this.resizeTimer = setTimeout(() => {
        
        this.$nextTick(() => Object.values(this.charts).forEach(c => c && c.resize && c.resize()))
      }, 200)
    }
  }
}
</script>

<style lang="scss" scoped>
// ══════════════════════════════════════════════════════════════
// 字体层级规范：
//   大数值  20-24px  Header KPI、模型核心数字
//   中数值  16-18px  卡片数值、当月检测概览
//   小数值  14-15px  列表数值、体征值
//   正文    13px     面板标题、名称、描述
//   次要    12px     标签、辅助说明、单位
//   最小    11px     仅用于极度受限场景
//   禁止    ≤10px
// ══════════════════════════════════════════════════════════════

$bg:     #060d1f;
$panel:  rgba(8,18,48,0.85);
$border: rgba(0,212,255,0.13);
$accent: #00d4ff;
$text:   #c8d8e8;
$dim:    #8ba6c8;
$white:  #e8f4ff;

.dm-outer { width:100%; height:calc(100vh - 50px) !important; overflow:hidden; background:$bg; }
.dm-root {
  width:100%; height:100%; /* 填满 dm-outer */
  min-height: 600px; /* 最小高度防止过小 */
  background:$bg;
  background-image:
    radial-gradient(circle at 15% 25%, rgba(0,212,255,0.05) 0%, transparent 45%),
    radial-gradient(circle at 85% 75%, rgba(42,82,152,0.06) 0%, transparent 45%);
  overflow:hidden; display:flex; flex-direction:column;
  font-family:'Microsoft YaHei', sans-serif; color:$text;
}

// ── Header 54px ──
.dm-hd {
  height:54px; flex-shrink:0;
  display:flex; align-items:center;
  padding:0 28px; gap:28px;
  background:rgba(0,6,24,0.8); border-bottom:1px solid $border;
  position:relative;
  &::after { content:''; position:absolute; bottom:0; left:0; right:0; height:1px;
    background:linear-gradient(90deg, transparent, rgba(0,212,255,0.5), transparent); }
}
.dm-hd-left { display:flex; align-items:center; gap:10px; flex-shrink:0; }
.dm-live-dot {
  width:10px; height:10px; border-radius:50%;
  background:$accent; box-shadow:0 0 10px $accent;
  animation:dmPulse 2s ease-in-out infinite;
}
@keyframes dmPulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.4;transform:scale(0.7)} }
.dm-hd-title { font-size:26px; font-weight:700; color:$white; margin:0; letter-spacing:3px; text-shadow:0 0 16px rgba(0,212,255,0.4); }
.dm-hd-kpis  { flex:1; display:flex; justify-content:center; }
.dm-hd-kpi {
  display:flex; flex-direction:column; align-items:center;
  padding:0 32px; border-right:1px solid $border;
  transition: background 0.2s;
  &:first-child { border-left:1px solid $border; }
  &[style*="cursor:pointer"]:hover { background: rgba(255,82,82,0.08); border-radius:6px; }
}
.dm-hd-kpi-val {
  font-size:20px; font-weight:700; font-family:'Consolas',monospace; line-height:1.1;
  &.kpi-cyan   { color:$accent;  text-shadow:0 0 10px rgba(0,212,255,0.6); }
  &.kpi-red    { color:#ff5252;  text-shadow:0 0 10px rgba(255,82,82,0.5); }
  &.kpi-green  { color:#52c41a;  text-shadow:0 0 10px rgba(82,196,26,0.4); }
  &.kpi-orange { color:#FFB84D;  text-shadow:0 0 10px rgba(255,184,77,0.5); }
  &.kpi-teal   { color:#00ffc8;  text-shadow:0 0 10px rgba(0,255,200,0.5); }
}
.dm-hd-kpi-label { font-size:12px; color:$dim; white-space:nowrap; margin-top:1px; }
.dm-hd-right { flex-shrink:0; }
.dm-hd-time  { font-family:'Consolas',monospace; font-size:14px; color:$dim; }

// ── Body ──
.dm-bd { flex:1; min-height:0; display:flex; gap:10px; padding:10px; overflow-y:auto; overflow-x:hidden; }

// ── Panel 通用 ──
.dm-panel {
  background:$panel; border:1px solid $border;
  border-radius:10px; display:flex; flex-direction:column; overflow:hidden;
}
.dm-ph {
  height:36px; flex-shrink:0;
  display:flex; align-items:center; gap:8px; padding:0 14px;
  border-bottom:1px solid $border;
}
.dm-ph-bar   { width:3px; height:14px; border-radius:2px; background:linear-gradient(180deg,$accent,#0066cc); flex-shrink:0; }
.dm-ph-title { font-size:13px; font-weight:700; color:$white; }
.dm-ph-sub   { margin-left:auto; font-size:12px; color:$dim; }
.dm-pc       { flex:1; min-height:0; }
.dm-empty    { text-align:center; color:$dim; font-size:13px; padding:16px; }

// ══ LEFT 290px ══
.dm-left { width:290px; flex-shrink:0; display:flex; flex-direction:column; gap:10px; }
// assess = 36(ph) + 6卡(3行×30px=90px) + 4bars(4×22px+16px=104px) = 230px
.dm-left-assess { flex:0 0 230px; overflow:hidden; }
.dm-left-dept   { flex:1; min-height:0; }

// 体征卡 2×3
.dm-vitals-grid { display:grid; grid-template-columns:1fr 1fr; gap:5px; padding:6px 12px 4px; }
.dm-vital-card {
  display:flex; align-items:center; gap:6px;
  background:rgba(0,212,255,0.04); border:1px solid rgba(0,212,255,0.12);
  border-radius:7px; padding:5px 7px;
}
.dm-vital-icon {
  width:26px; height:26px; border-radius:6px; border:1px solid;
  display:flex; align-items:center; justify-content:center; flex-shrink:0;
}
.dm-vital-body { flex:1; min-width:0; overflow:visible; }
.dm-vital-val  { font-size:15px; font-weight:700; font-family:'Consolas',monospace; line-height:1.1; }
.dm-vital-unit { font-size:10px; margin-left:2px; opacity:0.8; }
.dm-vital-label{ font-size:10px; color:$dim; margin-top:1px; white-space:nowrap; }
.dm-vital-tag  {
  font-size:11px; font-weight:600; padding:2px 5px; border-radius:3px; flex-shrink:0;
  &.vtag-ok     { color:#38ef7d; background:rgba(56,239,125,0.12);  border:1px solid rgba(56,239,125,0.3); }
  &.vtag-warn   { color:#ffd200; background:rgba(255,210,0,0.12);   border:1px solid rgba(255,210,0,0.3); }
  &.vtag-danger { color:#ff5252; background:rgba(255,82,82,0.12);   border:1px solid rgba(255,82,82,0.3); }
}
// 健康评估进度条
.dm-assess-bars { display:flex; flex-direction:column; gap:6px; padding:6px 12px 8px; }
.dm-assess-row  { display:flex; align-items:center; gap:8px; }
.dm-assess-label{ font-size:12px; color:$dim; width:28px; flex-shrink:0; }
.dm-assess-track{ flex:1; height:6px; background:rgba(26,77,143,0.3); border-radius:3px; overflow:hidden; }
.dm-assess-fill { height:100%; border-radius:3px; transition:width 0.8s ease; min-width:2px; }
.dm-assess-tag  { font-size:12px; font-weight:600; width:28px; text-align:right; flex-shrink:0; }

// ══ MAIN flex:1 ══
.dm-main {
  flex:1; min-width:0; display:flex; flex-direction:column; gap:10px;
  overflow-y: auto; overflow-x: hidden;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.3); border-radius: 2px; }
  &::-webkit-scrollbar-track { background: rgba(0,212,255,0.05); }
}
// metrics 36+50=86, device 36+64=100, gaps 20 → model = 966-54-86-100-20=706px
.dm-main-metrics { flex:0 0 96px; overflow:hidden; }
.dm-main-model   { flex:1; min-height:0; overflow:hidden; }
.dm-main-device  { flex:0 0 100px; overflow:hidden; }

// 当月检测概览指标行
.dm-metrics-row { display:flex; gap:8px; padding:5px 14px 6px; align-items:stretch; }
.dm-metric-card {
  flex:1; display:flex; flex-direction:column; align-items:center; gap:4px;
  background:rgba(0,212,255,0.04); border:1px solid rgba(0,212,255,0.12);
  border-radius:8px; padding:8px 8px 6px;
  transition:border-color 0.2s, background 0.2s, transform 0.15s;
  &:hover { border-color:rgba(0,212,255,0.5); background:rgba(0,212,255,0.09); transform:translateY(-1px); }
  &:active { transform:translateY(0); }
}
.dm-metric-val   { font-size:18px; font-weight:700; font-family:'Consolas',monospace; line-height:1; }
.dm-metric-bar-wrap { width:100%; height:4px; background:rgba(0,212,255,0.1); border-radius:2px; overflow:hidden; }
.dm-metric-bar   { height:100%; border-radius:2px; transition:width 1s ease; min-width:2px; }
.dm-metric-label { font-size:12px; color:$dim; }

// ── 健康监测中心 ──
.dm-model-body { flex:1; min-height:0; display:flex; overflow:hidden; }

// ── 视频区新布局 ──
.dm-model-video-col {
  flex:0 0 280px; position:relative;
  display:flex; flex-direction:column; align-items:stretch;
  overflow:hidden; padding:0;
}
// 上方在岗概况条
.dm-duty-bar {
  flex-shrink:0; height:52px;
  display:flex; align-items:center; justify-content:space-around;
  padding:0 12px;
  background:rgba(0,212,255,0.04);
  border-bottom:1px solid rgba(0,212,255,0.1);
  z-index:4; position:relative; overflow:hidden;
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: -40%;
    width: 40%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.9), transparent);
    animation: dmDutyFlow 3s linear infinite;
  }
}
.dm-duty-item { display:flex; flex-direction:column; align-items:center; gap:2px; }
.dm-duty-val  { font-size:20px; font-weight:700; font-family:'Consolas',monospace; line-height:1; }
.dm-duty-lbl  { font-size:11px; color:$dim; }
.dm-duty-sep  { width:1px; height:28px; background:rgba(0,212,255,0.15); }

// 视频贴边撑满
.dm-video-wrap {
  position:relative; flex:1; min-height:0; z-index:2; overflow:hidden;
}
.dm-model-video {
  width:100%; height:100%;
  object-fit:contain;
  filter:drop-shadow(0 0 20px rgba(0,212,255,0.3));
}
.dm-video-mask {
  position:absolute; bottom:0; left:0;
  width:130px; height:30px;
  background:linear-gradient(to right, rgba(6,13,31,1) 60%, transparent);
  z-index:3; pointer-events:none;
}

// 下方最新预警横幅
.dm-latest-warn {
  flex-shrink:0; height:36px;
  display:flex; align-items:center; gap:6px; padding:0 12px;
  background:rgba(255,82,82,0.06);
  border-top:1px solid rgba(255,82,82,0.2);
  z-index:4; position:relative; overflow:hidden;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -40%;
    width: 40%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(255, 82, 82, 0.9), transparent);
    animation: dmWarnFlow 2.5s linear infinite;
  }
  &.dm-lw-empty {
    background:rgba(56,239,125,0.04);
    border-top-color:rgba(56,239,125,0.15);
    &::before {
      background: linear-gradient(90deg, transparent, rgba(56, 239, 125, 0.9), transparent);
    }
  }
}
.dm-lw-dot {
  width:7px; height:7px; border-radius:50%;
  background:#ff5252; box-shadow:0 0 6px #ff5252;
  flex-shrink:0; animation:dmPulse 1.5s ease-in-out infinite;
}
.dm-lw-name    { font-size:12px; font-weight:700; color:#ff5252; flex-shrink:0; }
.dm-lw-sep     { font-size:11px; color:$dim; }
.dm-lw-type    { font-size:12px; color:$text; flex-shrink:0; }
.dm-lw-val     {
  font-size:12px; font-style:normal; color:#ffd200; font-weight:700;
  font-family:'Consolas',monospace;
  flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
}
.dm-lw-pending {
  font-size:11px; color:#ffd200;
  background:rgba(255,210,0,0.1); border:1px solid rgba(255,210,0,0.3);
  border-radius:3px; padding:1px 5px; flex-shrink:0;
}

// 右侧数据区
.dm-model-data-col {
  flex:1; min-width:0;
  display:flex; flex-direction:column;
  padding:10px 14px 8px 10px; gap:8px; overflow:hidden;
}

// 数据块通用
.dm-data-block {
  flex:1; min-height:0; display:flex; flex-direction:column;
  background:rgba(0,212,255,0.05); border:none;
  border-radius:6px; overflow:hidden;
}
.dm-block-hd {
  height:32px; flex-shrink:0;
  display:flex; align-items:center; gap:7px; padding:0 12px;
  border-bottom:1px solid rgba(0,212,255,0.1);
}
.dm-block-title { font-size:13px; font-weight:600; color:$white; }
.dm-block-sub   { margin-left:auto; font-size:12px; color:$dim; }

// TOP 异常频次列表
.dm-top5-list {
  display:flex; flex-direction:column; gap:5px;
  padding:6px 12px; flex:1; overflow:hidden;
}
.dm-top5-row { display:flex; align-items:center; gap:9px; }
.dm-top5-rank {
  width:20px; height:20px; border-radius:4px; flex-shrink:0;
  font-size:12px; font-weight:700;
  display:flex; align-items:center; justify-content:center;
  &.rk-1 { background:rgba(255,184,77,0.2);  color:#FFB84D; border:1px solid rgba(255,184,77,0.4); }
  &.rk-2 { background:rgba(0,212,255,0.12);  color:#00d4ff; border:1px solid rgba(0,212,255,0.3); }
  &.rk-3 { background:rgba(82,196,26,0.12);  color:#52c41a; border:1px solid rgba(82,196,26,0.3); }
  &.rk-4,&.rk-5,&.rk-6,&.rk-7,&.rk-8,&.rk-9,
  &.rk-10,&.rk-11,&.rk-12,&.rk-13,&.rk-14,&.rk-15 {
    background:rgba(139,166,200,0.08); color:$dim; border:1px solid rgba(139,166,200,0.2);
  }
}
.dm-top5-name { font-size:13px; color:$white; width:72px; flex-shrink:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.dm-top5-bar-wrap { flex:1; height:6px; background:rgba(0,212,255,0.08); border-radius:3px; overflow:hidden; }
.dm-top5-bar { height:100%; border-radius:3px; background:linear-gradient(90deg,#00d4ff,#0066cc); transition:width 0.8s ease; }
.dm-top5-val { font-size:14px; font-weight:700; color:#00d4ff; font-family:'Consolas',monospace; width:28px; text-align:right; flex-shrink:0; }

// 趋势和预警时段各占一半
.dm-data-block-trend { flex:1; }

// 底行：左右分割布局
.dm-data-row2 {
  flex:1; min-height:0; display:flex; gap:8px;
  background:transparent; border:none; padding:0;
}
// 通用左右分割列
.dm-split-left {
  flex:0 0 50%; display:flex; flex-direction:column; overflow:hidden;
}
.dm-split-right {
  flex:1; min-width:0; display:flex; flex-direction:column; overflow:hidden;
}
.dm-split-divider {
  width:1px; flex-shrink:0;
  background:rgba(0,212,255,0.13); margin:8px 0;
}
// 排行列表（TOP/风险）复用
.dm-rank-col {
  flex:1; min-width:0; display:flex; flex-direction:column;
  background:rgba(0,212,255,0.03); border:1px solid rgba(0,212,255,0.12);
  border-radius:8px; overflow:hidden;
}
.dm-risk-list {
  display:flex; flex-direction:column; gap:5px;
  padding:5px 12px; flex:1; overflow:hidden;
}
.dm-risk-row  { display:flex; align-items:center; gap:7px; }
.dm-risk-no   { font-size:12px; color:$dim; width:18px; flex-shrink:0; }
.dm-risk-name { font-size:12px; color:$text; width:66px; flex-shrink:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.dm-risk-bar-wrap { flex:0 0 38%; height:5px; background:rgba(26,77,143,0.3); border-radius:3px; overflow:hidden; }
.dm-risk-bar  { height:100%; border-radius:3px; transition:width 0.8s ease; min-width:2px; }
.dm-risk-pct  { font-size:13px; font-weight:700; font-family:'Consolas',monospace; width:32px; text-align:right; flex-shrink:0; margin-left:auto; }
.dm-risk-count { font-size:11px; font-weight:700; font-family:'Consolas',monospace; width:24px; text-align:right; flex-shrink:0; }

// 预警类型分布
.dm-warn-type-col {
  flex:1; min-width:0; display:flex; flex-direction:column;
  background:rgba(0,212,255,0.03); border:1px solid rgba(0,212,255,0.12);
  border-radius:8px; overflow:hidden;
}
.dm-warn-type-body {
  flex:1; min-height:0; display:flex; align-items:center; gap:10px; padding:8px 14px;
}
.dm-warn-type-legend { flex:1; min-width:0; display:flex; flex-direction:column; gap:6px; justify-content:center; }
.dm-wtl-item  { display:flex; align-items:center; gap:8px; }
.dm-wtl-dot   { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.dm-wtl-name  { font-size:12px; color:$text; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.dm-wtl-val   { font-size:14px; font-weight:700; font-family:'Consolas',monospace; flex-shrink:0; }
.dm-wtl-pct   { font-size:12px; color:$dim; width:36px; text-align:right; flex-shrink:0; }

.dm-hour-dist {
  flex:1; display:flex; flex-direction:column;
  background:rgba(0,212,255,0.03); border:1px solid rgba(0,212,255,0.12);
  border-radius:8px; overflow:hidden;
}

// 底部信息条
.dm-model-footer {
  display:flex; align-items:center; gap:10px;
  padding:7px 16px; flex-shrink:0;
  background:rgba(0,212,255,0.04); border-top:1px solid $border;
}
.dm-mf-dot   { width:7px; height:7px; border-radius:50%; flex-shrink:0; }
.dm-mf-label { font-size:12px; color:$dim; }
.dm-mf-val   { font-size:14px; font-weight:700; font-family:'Consolas',monospace; }
.dm-mf-unit  { font-size:12px; color:$dim; }
.dm-mf-sep   { width:1px; height:14px; background:$border; margin:0 2px; }

// 设备状态
.dm-device-body {
  flex:1; min-height:0; display:flex; align-items:center;
  padding:0 14px 8px; gap:12px;
}
.dm-device-cards { flex:1; display:grid; grid-template-columns:repeat(6,1fr); gap:6px; }
.dm-dcard {
  border-radius:8px; padding:6px 10px;
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  border:1px solid transparent;
  &.dc-blue   { background:rgba(0,212,255,0.08);   border-color:rgba(0,212,255,0.2);   .dm-dcard-val{color:#00d4ff} }
  &.dc-green  { background:rgba(56,239,125,0.08);  border-color:rgba(56,239,125,0.2);  .dm-dcard-val{color:#38ef7d} }
  &.dc-gray   { background:rgba(139,166,200,0.07); border-color:rgba(139,166,200,0.18);.dm-dcard-val{color:#8ba6c8} }
  &.dc-red    { background:rgba(255,82,82,0.08);   border-color:rgba(255,82,82,0.2);   .dm-dcard-val{color:#ff5252} }
  &.dc-orange { background:rgba(255,210,0,0.08);   border-color:rgba(255,210,0,0.2);   .dm-dcard-val{color:#ffd200} }
  &.dc-teal   { background:rgba(0,255,200,0.08);   border-color:rgba(0,255,200,0.2);   .dm-dcard-val{color:#00ffc8} }
}
.dm-dcard-val   { font-size:18px; font-weight:700; font-family:'Consolas',monospace; line-height:1.2; }
.dm-dcard-label { font-size:12px; color:$dim; margin-top:2px; }
.dm-device-gauges { display:flex; gap:8px; align-items:center; flex-shrink:0; }
.dm-gauge-item  { display:flex; flex-direction:column; align-items:center; }
.dm-gauge-chart { width:64px; height:64px; }
.dm-gauge-label { font-size:12px; color:$dim; margin-top:-2px; }

// ══ RIGHT 300px ══
.dm-right { width:300px; flex-shrink:0; display:flex; flex-direction:column; gap:10px; }
.dm-right-events  { flex:1; min-height:0; }
.dm-right-warnrate{ flex:0 0 220px; }
.dm-right-rank    { flex:1; min-height:0; overflow:hidden; }
.dm-right-tips    { flex:1; min-height:0; }

// 实时预警动态
.dm-badge-count {
  margin-left:auto;
  background:rgba(255,82,82,0.15); color:#ff5252;
  border:1px solid rgba(255,82,82,0.3);
  font-size:12px; font-weight:700; font-family:'Consolas',monospace;
  padding:2px 8px; border-radius:10px;
}
// 预警列表页码标题栏（固定在顶部，始终可见）
.dm-event-header {
  display:flex; align-items:center; justify-content:space-between;
  padding:8px 12px; margin-bottom:6px;
  background:rgba(0,40,90,0.45); border:1px solid rgba(0,212,255,0.15);
  border-radius:6px; flex-shrink:0;
}
.dm-event-title {
  font-size:13px; font-weight:700; color:$accent;
  border-left:3px solid $accent; padding-left:8px;
}
.dm-event-page-info {
  font-size:12px; color:$text; margin-left:auto; margin-right:8px;
}
.dm-event-page-btns {
  display:flex; gap:4px;
}
.dm-page-btn-sm {
  background:rgba(0,212,255,0.1); border:1px solid rgba(0,212,255,0.25);
  color:$accent; border-radius:3px; width:24px; height:22px;
  cursor:pointer; font-size:14px; display:flex; align-items:center; justify-content:center;
  transition: all 0.2s;
  &:disabled { opacity:0.3; cursor:not-allowed; }
  &:not(:disabled):hover { background:rgba(0,212,255,0.25); transform:scale(1.05); }
}

.dm-event-list {
  flex:1; overflow-y:auto; padding:2px 0;
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar { display:none; width:0; }
}
.dm-event {
  padding:8px 14px; border-bottom:1px solid rgba(0,212,255,0.07);
  transition: opacity 0.3s, background 0.3s;
  &.ev-danger { background:rgba(255,82,82,0.05); }
  &.ev-warn   { background:rgba(255,210,0,0.03); }
  &.ev-handled {
    opacity: 0.45;
    border-left-color: #38ef7d !important;
    background: rgba(56,239,125,0.03) !important;
    animation: none !important;
  }
}
.dm-ev-row1 { display:flex; align-items:center; gap:7px; margin-bottom:4px; }
.dm-ev-badge {
  font-size:11px; font-weight:700; padding:2px 6px; border-radius:3px; flex-shrink:0;
  &.badge-danger { background:rgba(255,82,82,0.2);  color:#ff5252; border:1px solid rgba(255,82,82,0.4); }
  &.badge-warn   { background:rgba(255,210,0,0.15); color:#ffd200; border:1px solid rgba(255,210,0,0.4); }
}
.dm-ev-type    { font-size:13px; color:$white; font-weight:600; flex:1; }
.dm-ev-time    { font-size:12px; color:$dim; flex-shrink:0; }
.dm-ev-row2    { display:flex; align-items:center; gap:8px; font-size:12px; }
.dm-ev-user    { color:$accent; font-weight:600; }
.dm-ev-val     { flex:1; color:$dim; em { color:#FFB84D; font-style:normal; } }
.dm-ev-pending { color:#ffd200; font-size:12px; }
.dm-ev-done    { color:#38ef7d; font-size:12px; }
.dm-event-page {
  display:flex; align-items:center; justify-content:center; gap:12px;
  padding:6px; border-top:1px solid $border; flex-shrink:0;
}
.dm-page-btn {
  background:rgba(0,212,255,0.1); border:1px solid rgba(0,212,255,0.25);
  color:$accent; border-radius:4px; width:30px; height:26px;
  cursor:pointer; font-size:16px; display:flex; align-items:center; justify-content:center;
  &:disabled { opacity:0.3; cursor:not-allowed; }
  &:not(:disabled):hover { background:rgba(0,212,255,0.2); }
}
.dm-page-info { font-size:13px; color:$text; }

// 指标预警率分析
.dm-warn-stats {
  flex:1; display:flex; flex-direction:column;
  padding:6px 14px 8px; justify-content:space-evenly;
}
.dm-warn-item { display:flex; align-items:center; gap:9px; }
.dm-warn-icon-wrap {
  width:28px; height:28px; border-radius:7px;
  background:rgba(0,212,255,0.08); border:1px solid rgba(0,212,255,0.2);
  display:flex; align-items:center; justify-content:center;
  flex-shrink:0; color:$accent;
}
.dm-warn-body { flex:1; min-width:0; }
.dm-warn-top  { display:flex; align-items:center; justify-content:space-between; margin-bottom:4px; }
.dm-warn-name { font-size:13px; color:$text; }
.dm-warn-pct  { font-size:15px; font-weight:700; font-family:'Consolas',monospace; }
.dm-warn-bar-bg { height:5px; background:rgba(26,77,143,0.3); border-radius:3px; overflow:hidden; }
.dm-warn-bar-fill { height:100%; border-radius:3px; transition:width 0.8s ease; min-width:2px; }
.dm-warn-tag {
  font-size:11px; font-weight:600; padding:2px 6px; border-radius:3px; flex-shrink:0;
  &.tag-ok     { color:#38ef7d; background:rgba(56,239,125,0.12); border:1px solid rgba(56,239,125,0.3); }
  &.tag-warn   { color:#ffd200; background:rgba(255,210,0,0.12);  border:1px solid rgba(255,210,0,0.3); }
  &.tag-danger { color:#ff5252; background:rgba(255,82,82,0.12);  border:1px solid rgba(255,82,82,0.3); }
}

// ── video fallback 人形占位 ──
.dm-model-fallback {
  position:relative; z-index:2;
  width:120px; height:280px;
  display:flex; flex-direction:column; align-items:center;
  filter:drop-shadow(0 0 18px rgba(0,212,255,0.35));
}
.dm-fallback-head {
  width:46px; height:46px; border-radius:50%;
  border:2px solid rgba(0,212,255,0.6);
  background:rgba(0,212,255,0.08);
  flex-shrink:0;
}
.dm-fallback-body {
  margin-top:8px;
  width:80px; flex:1;
  border:2px solid rgba(0,212,255,0.5);
  border-radius:14px 14px 6px 6px;
  background:rgba(0,212,255,0.06);
  position:relative;
  &::before, &::after {
    content:''; position:absolute; top:20px;
    width:22px; height:70px;
    border:2px solid rgba(0,212,255,0.4);
    border-radius:6px;
  }
  &::before { left:-26px; }
  &::after  { right:-26px; }
}
.dm-fallback-scan {
  position:absolute; left:50%; transform:translateX(-50%);
  width:110px; height:2px;
  background:linear-gradient(90deg, transparent, rgba(0,212,255,0.9), transparent);
  animation:dmFallbackScan 2s linear infinite;
  top:0;
}
@keyframes dmFallbackScan {
  0%   { top:0%;   opacity:0; }
  10%  { opacity:1; }
  90%  { opacity:1; }
  100% { top:100%; opacity:0; }
}

// ── 四角 Corner Bracket 装饰 ──
.dm-corner {
  position: absolute;
  width: 18px;
  height: 18px;
  z-index: 5;
  pointer-events: none;
  animation: dmCornerGlow 2.5s ease-in-out infinite;
  &.dm-corner-tl {
    top: 6px; left: 6px;
    border-top: 2px solid rgba(0, 212, 255, 0.9);
    border-left: 2px solid rgba(0, 212, 255, 0.9);
    box-shadow: -2px -2px 6px rgba(0, 212, 255, 0.3);
  }
  &.dm-corner-tr {
    top: 6px; right: 6px;
    border-top: 2px solid rgba(0, 212, 255, 0.9);
    border-right: 2px solid rgba(0, 212, 255, 0.9);
    animation-delay: 0.6s;
    box-shadow: 2px -2px 6px rgba(0, 212, 255, 0.3);
  }
  &.dm-corner-bl {
    bottom: 6px; left: 6px;
    border-bottom: 2px solid rgba(0, 212, 255, 0.9);
    border-left: 2px solid rgba(0, 212, 255, 0.9);
    animation-delay: 1.2s;
    box-shadow: -2px 2px 6px rgba(0, 212, 255, 0.3);
  }
  &.dm-corner-br {
    bottom: 6px; right: 6px;
    border-bottom: 2px solid rgba(0, 212, 255, 0.9);
    border-right: 2px solid rgba(0, 212, 255, 0.9);
    animation-delay: 1.8s;
    box-shadow: 2px 2px 6px rgba(0, 212, 255, 0.3);
  }
}
@keyframes dmCornerGlow {
  0%, 100% { opacity: 0.5; }
  50%       { opacity: 1; box-shadow: 0 0 10px rgba(0, 212, 255, 0.7); }
}
@keyframes dmDutyFlow {
  0%   { left: -40%; }
  100% { left: 140%; }
}
@keyframes dmWarnFlow {
  0%   { left: -40%; }
  100% { left: 140%; }
}
@keyframes dmDataStreamL {
  0%   { background-position: 0 0; }
  100% { background-position: 0 32px; }
}
@keyframes dmDataStreamR {
  0%   { background-position: 0 0; }
  100% { background-position: 0 32px; }
}

// ── 时间维度切换 ──
.dm-period-tabs {
  display: flex;
  background: rgba(0, 212, 255, 0.06);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.dm-period-tab {
  padding: 5px 14px;
  font-size: 12px;
  color: $dim;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  user-select: none;
  &:hover:not(.is-active) {
    color: $text;
    background: rgba(0, 212, 255, 0.08);
  }
  &.is-active {
    color: $bg;
    background: $accent;
    font-weight: 700;
  }
}

// ── 刷新状态指示器 ──
.dm-refresh-info {
  display: flex; align-items: center; gap: 5px;
  cursor: pointer; padding: 4px 10px;
  border-radius: 5px;
  transition: background 0.2s;
  &:hover { background: rgba(0, 212, 255, 0.08); }
}
.dm-refresh-icon {
  font-size: 15px; color: $accent; line-height: 1;
  display: inline-block;
  transition: transform 0.3s;
  &.is-spinning { animation: dmSpin 0.8s linear infinite; }
}
@keyframes dmSpin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.dm-refresh-time { font-size: 11px; color: $dim; }

// ══ 员工健康档案 Drawer ══
.dm-emp-drawer {
  :deep(.el-drawer) {
    background: #080c20;
    border-left: 1px solid rgba(0,212,255,0.2);
  }
  :deep(.el-drawer__header) {
    padding: 16px 20px 12px;
    border-bottom: 1px solid rgba(0,212,255,0.12);
    margin-bottom: 0;
  }
  :deep(.el-drawer__body) {
    padding: 0;
    overflow-y: auto;
    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-thumb { background: rgba(0,212,255,0.2); border-radius: 2px; }
  }
}
.dm-drawer-hd {
  display: flex; align-items: center; gap: 14px; width: 100%;
}
.dm-drawer-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700; color: #fff;
  border: 2px solid rgba(0,212,255,0.3);
  flex-shrink: 0;
}
.dm-drawer-name { font-size: 16px; font-weight: 700; color: #e8f4ff; }
.dm-drawer-meta { font-size: 12px; color: #8ba6c8; margin-top: 2px; }
.dm-drawer-warn-count {
  margin-left: auto;
  display: flex; flex-direction: column; align-items: center;
  background: rgba(255,82,82,0.1);
  border: 1px solid rgba(255,82,82,0.25);
  border-radius: 8px; padding: 6px 14px;
}
.dm-dwc-num   { font-size: 22px; font-weight: 700; color: #ff5252; font-family: 'Consolas', monospace; line-height: 1; }
.dm-dwc-label { font-size: 11px; color: #8ba6c8; }
.dm-drawer-body { padding: 14px 18px; display: flex; flex-direction: column; gap: 14px; }
.dm-drawer-vitals {
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
}
.dm-dv-card {
  background: rgba(0,212,255,0.05);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 8px; padding: 10px 14px;
  display: flex; flex-direction: column; gap: 3px;
}
.dm-dv-val    { font-size: 20px; font-weight: 700; font-family: 'Consolas', monospace; line-height: 1.1; }
.dm-dv-unit   { font-size: 11px; margin-left: 2px; opacity: 0.7; font-weight: 400; }
.dm-dv-label  { font-size: 12px; color: #8ba6c8; }
.dm-dv-status {
  font-size: 11px; font-weight: 600; padding: 2px 7px; border-radius: 3px;
  align-self: flex-start; margin-top: 2px;
  &.dv-ok   { color: #38ef7d; background: rgba(56,239,125,0.12); border: 1px solid rgba(56,239,125,0.3); }
  &.dv-warn { color: #ff5252; background: rgba(255,82,82,0.12);  border: 1px solid rgba(255,82,82,0.3); }
}
.dm-drawer-section {
  background: rgba(0,212,255,0.03);
  border: 1px solid rgba(0,212,255,0.1);
  border-radius: 8px; padding: 10px 14px;
}
.dm-ds-title {
  font-size: 13px; font-weight: 600; color: #c8d8e8;
  margin-bottom: 10px;
  display: flex; align-items: center; gap: 8px;
}
.dm-ds-badge {
  font-size: 11px; color: #8ba6c8;
  background: rgba(0,212,255,0.08);
  border: 1px solid rgba(0,212,255,0.2);
  padding: 2px 8px; border-radius: 10px;
}
.dm-warn-table { display: flex; flex-direction: column; gap: 0; }
.dm-wt-row {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: #c8d8e8;
  padding: 7px 6px;
  border-bottom: 1px solid rgba(0,212,255,0.06);
  &:last-child { border-bottom: none; }
}
.dm-wt-head {
  font-size: 11px; color: #8ba6c8; font-weight: 600;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0,212,255,0.15) !important;
}
.dm-wt-level {
  font-size: 11px; font-weight: 600; padding: 2px 5px; border-radius: 3px;
  &.lv-danger { color: #ff5252; background: rgba(255,82,82,0.15); }
  &.lv-warn   { color: #ffd200; background: rgba(255,210,0,0.12); }
  &.lv-info   { color: #8ba6c8; background: rgba(139,166,200,0.1); }
}

// ── 预警处理按钮 & 弹窗 ──
.dm-ev-handle-btn {
  display: inline-block;
  font-size: 11px; color: #00d4ff;
  background: rgba(0,212,255,0.1);
  border: 1px solid rgba(0,212,255,0.3);
  border-radius: 3px; padding: 1px 7px;
  cursor: pointer; margin-left: 6px;
  transition: all 0.2s;
  &:hover { background: rgba(0,212,255,0.2); }
}
.dm-handle-dialog {
  :deep(.el-dialog) { background: #080c20; border: 1px solid rgba(0,212,255,0.2); border-radius: 10px; }
  :deep(.el-dialog__title) { color: #e8f4ff; }
  :deep(.el-textarea__inner) { background: rgba(0,212,255,0.05); border-color: rgba(0,212,255,0.2); color: #c8d8e8; }
}
.dm-hd-info { background: rgba(0,212,255,0.04); border-radius: 6px; padding: 10px 14px; display: flex; flex-direction: column; gap: 7px; }
.dm-hd-row  { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.dm-hd-key  { color: #8ba6c8; width: 36px; flex-shrink: 0; }
.dm-hd-val  { color: #c8d8e8; font-weight: 600; }

// ── 改动1：KPI 副文字 & 环比颜色 ──
.dm-hd-kpi-sub { font-size: 11px; color: $dim; margin-top: 1px; white-space: nowrap; }
.sub-up   { color: #ff5252 !important; }
.sub-down { color: #38ef7d !important; }

// ── 改动1：实时预警高危角标 ──
.dm-hd-critical-badge {
  font-size: 11px; color: #ff3b3b; margin-left: 8px;
  background: rgba(255,59,59,0.1);
  border: 1px solid rgba(255,59,59,0.3);
  border-radius: 3px; padding: 1px 6px;
  animation: criticalBlink 2s infinite;
}

// ── 改动2：高危告警条目高亮 ──
.alert-item--critical {
  border-left: 3px solid #ff3b3b !important;
  background: rgba(255,59,59,0.06) !important;
  animation: criticalBlink 2s infinite;
}
@keyframes criticalBlink {
  0%, 100% { border-left-color: rgba(255,59,59,0.5); }
  50%       { border-left-color: #ff3b3b; box-shadow: inset 0 0 8px rgba(255,59,59,0.15); }
}
</style>